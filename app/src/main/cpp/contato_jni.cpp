// Ponte JNI: liga contato.app.core.ai.LlamaNative (Kotlin) ao núcleo em contato_llm.cpp.
#include <jni.h>
#include <string>

#include "contato_llm.h"

namespace {
// Texto em UTF-8 de verdade (vem do Kotlin como bytes; GetStringUTFChars estraga emojis).
std::string bytes_to_std(JNIEnv * env, jbyteArray b) {
    if (!b) return "";
    jsize len = env->GetArrayLength(b);
    std::string out((size_t) len, '\0');
    if (len > 0) env->GetByteArrayRegion(b, 0, len, reinterpret_cast<jbyte *>(&out[0]));
    return out;
}

std::string to_std(JNIEnv * env, jstring s) {
    if (!s) return "";
    const char * chars = env->GetStringUTFChars(s, nullptr);
    std::string out(chars ? chars : "");
    if (chars) env->ReleaseStringUTFChars(s, chars);
    return out;
}
} // namespace

extern "C" {

JNIEXPORT jboolean JNICALL
Java_contato_app_core_ai_LlamaNative_init(JNIEnv * env, jobject, jstring modelPath, jint threads) {
    return contato::llm_init(to_std(env, modelPath), threads) ? JNI_TRUE : JNI_FALSE;
}

// Devolve os bytes UTF-8 da resposta: o Kotlin decodifica com segurança
// (NewStringUTF derruba o app se o texto tiver UTF-8 incompleto).
JNIEXPORT jbyteArray JNICALL
Java_contato_app_core_ai_LlamaNative_generate(JNIEnv * env, jobject, jbyteArray prompt, jint maxTokens,
                                              jfloat temperature, jfloat topP, jfloat repeatPenalty,
                                              jfloat frequencyPenalty, jfloat presencePenalty) {
    contato::GenerationParams p;
    p.max_tokens        = maxTokens;
    p.temperature       = temperature;
    p.top_p             = topP;
    p.repeat_penalty    = repeatPenalty;
    p.frequency_penalty = frequencyPenalty;
    p.presence_penalty  = presencePenalty;
    std::string result = contato::llm_generate(bytes_to_std(env, prompt), p);

    jbyteArray bytes = env->NewByteArray((jsize) result.size());
    if (bytes && !result.empty()) {
        env->SetByteArrayRegion(bytes, 0, (jsize) result.size(), reinterpret_cast<const jbyte *>(result.data()));
    }
    return bytes;
}

JNIEXPORT jboolean JNICALL
Java_contato_app_core_ai_LlamaNative_isLoaded(JNIEnv *, jobject) {
    return contato::llm_is_loaded() ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT void JNICALL
Java_contato_app_core_ai_LlamaNative_abort(JNIEnv *, jobject) {
    contato::llm_abort();
}

JNIEXPORT void JNICALL
Java_contato_app_core_ai_LlamaNative_release(JNIEnv *, jobject) {
    contato::llm_free();
}

} // extern "C"
