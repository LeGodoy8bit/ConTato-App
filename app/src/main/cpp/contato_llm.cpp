// Núcleo da IA do ConTato, baseado no llama-android.cpp do colega, com estas correções:
//  1. Limpa a memória (KV cache) antes de cada frase: sem isso, da 2ª frase em diante a IA
//     "lembrava" da anterior e podia misturar textos ou falhar no decode.
//  2. Trava (mutex): duas gerações ao mesmo tempo corrompiam o contexto global.
//  3. Para em qualquer token de fim (llama_vocab_is_eog), não só no EOS.
//  4. Limita o prompt ao tamanho do contexto, para não estourar.
//  5. Devolve bytes (o JNI converte para String em Kotlin), evitando crash com UTF-8 quebrado.
//  6. Pode ser interrompida (llm_abort): se demorar demais ou a pessoa sair da tela, para na hora.
#include "contato_llm.h"

#include <algorithm>
#include <atomic>
#include <mutex>
#include <vector>

#include "llama.h"

#if defined(__ANDROID__)
#include <android/log.h>
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, "ConTatoLLM", __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, "ConTatoLLM", __VA_ARGS__)
#else
#include <cstdio>
#define LOGI(...) (std::fprintf(stderr, "[I] " __VA_ARGS__), std::fprintf(stderr, "\n"))
#define LOGE(...) (std::fprintf(stderr, "[E] " __VA_ARGS__), std::fprintf(stderr, "\n"))
#endif

namespace contato {

namespace {
std::mutex      g_mutex;
llama_model   * g_model   = nullptr;
llama_context * g_ctx     = nullptr;
bool            g_backend = false;
std::atomic<bool> g_abort{false};

constexpr int kContext = 512;
constexpr int kBatch   = 256;

void free_locked() {
    if (g_ctx)   { llama_free(g_ctx);         g_ctx = nullptr; }
    if (g_model) { llama_model_free(g_model); g_model = nullptr; }
}

// Corta o texto em qualquer marcador de fim usado no treino.
bool cut_at_stop(std::string & text) {
    static const char * stops[] = { "\n\n", "<|user|>", "<|end|>", "<|assistant|>" };
    size_t best = std::string::npos;
    for (const char * s : stops) {
        size_t pos = text.find(s);
        if (pos != std::string::npos) best = std::min(best, pos);
    }
    if (best == std::string::npos) return false;
    text.resize(best);
    return true;
}
} // namespace

bool llm_init(const std::string & model_path, int n_threads) {
    std::lock_guard<std::mutex> lock(g_mutex);
    free_locked();
    if (!g_backend) { llama_backend_init(); g_backend = true; }

    LOGI("Carregando modelo: %s", model_path.c_str());
    llama_model_params mparams = llama_model_default_params();
    mparams.n_gpu_layers = 0; // só CPU: funciona em qualquer celular
    g_model = llama_model_load_from_file(model_path.c_str(), mparams);
    if (!g_model) { LOGE("Falha ao carregar o modelo"); return false; }

    llama_context_params cparams = llama_context_default_params();
    cparams.n_ctx           = kContext;
    cparams.n_batch         = kBatch;
    cparams.n_threads       = std::max(1, n_threads);
    cparams.n_threads_batch = std::max(1, n_threads);
    g_ctx = llama_init_from_model(g_model, cparams);
    if (!g_ctx) { LOGE("Falha ao criar o contexto"); free_locked(); return false; }

    LOGI("Modelo pronto");
    return true;
}

bool llm_is_loaded() {
    std::lock_guard<std::mutex> lock(g_mutex);
    return g_model != nullptr && g_ctx != nullptr;
}

std::string llm_generate(const std::string & prompt, const GenerationParams & p) {
    std::lock_guard<std::mutex> lock(g_mutex);
    g_abort = false;
    if (!g_model || !g_ctx) return "";

    // (1) Começa sempre do zero.
    llama_memory_clear(llama_get_memory(g_ctx), true);

    const llama_vocab * vocab = llama_model_get_vocab(g_model);

    // Tokeniza (o modelo foi treinado com os marcadores <|user|>/<|assistant|> como texto especial).
    std::vector<llama_token> tokens(prompt.size() + 8);
    int n = llama_tokenize(vocab, prompt.data(), (int32_t) prompt.size(), tokens.data(), (int32_t) tokens.size(), true, true);
    if (n < 0) {
        tokens.resize(-n);
        n = llama_tokenize(vocab, prompt.data(), (int32_t) prompt.size(), tokens.data(), (int32_t) tokens.size(), true, true);
    }
    if (n <= 0) { LOGE("Falha ao tokenizar"); return ""; }
    tokens.resize(n);

    const int max_new = (p.max_tokens > 0 && p.max_tokens <= 256) ? p.max_tokens : 64;
    // (4) Prompt + resposta precisam caber no contexto.
    if ((int) tokens.size() + max_new >= kContext) {
        LOGE("Prompt grande demais (%d tokens)", (int) tokens.size());
        return "";
    }

    llama_sampler * smpl = llama_sampler_chain_init(llama_sampler_chain_default_params());
    if (p.repeat_penalty != 1.0f || p.frequency_penalty != 0.0f || p.presence_penalty != 0.0f) {
        llama_sampler_chain_add(smpl, llama_sampler_init_penalties(
                llama_vocab_n_tokens(vocab), 64, p.repeat_penalty, p.frequency_penalty, p.presence_penalty));
    }
    llama_sampler_chain_add(smpl, llama_sampler_init_top_p(p.top_p, 1));
    if (p.temperature > 0.0f) {
        llama_sampler_chain_add(smpl, llama_sampler_init_temp(p.temperature));
        llama_sampler_chain_add(smpl, llama_sampler_init_dist(LLAMA_DEFAULT_SEED));
    } else {
        llama_sampler_chain_add(smpl, llama_sampler_init_greedy());
    }

    std::string out;
    // Primeiro o prompt inteiro (em blocos do tamanho do batch), depois um token por vez.
    for (size_t i = 0; i < tokens.size(); i += kBatch) {
        if (g_abort) { llama_sampler_free(smpl); return ""; }
        int count = (int) std::min<size_t>(kBatch, tokens.size() - i);
        if (llama_decode(g_ctx, llama_batch_get_one(tokens.data() + i, count)) != 0) {
            LOGE("Erro no decode do prompt");
            llama_sampler_free(smpl);
            return "";
        }
    }

    char piece[256];
    for (int i = 0; i < max_new; i++) {
        if (g_abort) { LOGI("Geração interrompida"); out.clear(); break; }
        llama_token tok = llama_sampler_sample(smpl, g_ctx, -1);
        if (llama_vocab_is_eog(vocab, tok)) break; // (3)

        int len = llama_token_to_piece(vocab, tok, piece, (int32_t) sizeof(piece), 0, true);
        if (len > 0) {
            out.append(piece, (size_t) len);
            if (cut_at_stop(out)) break;
        }
        if (llama_decode(g_ctx, llama_batch_get_one(&tok, 1)) != 0) {
            LOGE("Erro no decode da resposta");
            break;
        }
    }

    llama_sampler_free(smpl);
    return out;
}

void llm_abort() {
    g_abort = true;
}

void llm_free() {
    std::lock_guard<std::mutex> lock(g_mutex);
    free_locked();
    if (g_backend) { llama_backend_free(); g_backend = false; }
}

} // namespace contato
