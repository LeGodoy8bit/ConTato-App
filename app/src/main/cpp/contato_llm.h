// Núcleo da IA do ConTato (llama.cpp), sem nada de Android/JNI: dá para testar no PC.
#pragma once

#include <string>

namespace contato {

struct GenerationParams {
    int   max_tokens        = 64;
    float temperature       = 0.1f;
    float top_p             = 0.9f;
    float repeat_penalty    = 1.2f;
    float frequency_penalty = 0.5f;
    float presence_penalty  = 0.1f;
};

// Carrega o modelo (.gguf). Pode ser chamada de novo: libera o anterior antes.
bool llm_init(const std::string & model_path, int n_threads);

// Gera a continuação do prompt. Thread-safe (uma geração por vez).
// Devolve "" se o modelo não estiver carregado ou der erro.
std::string llm_generate(const std::string & prompt, const GenerationParams & params);

bool llm_is_loaded();

// Pede para a geração em andamento parar no próximo token (pode ser chamada de outra thread).
void llm_abort();

void llm_free();

} // namespace contato
