#include <jni.h>
#include <string>
#include "llama.h"

static llama_context* ctx = nullptr;
static llama_model* model = nullptr;

extern "C" {

JNIEXPORT jboolean JNICALL
Java_com_jarvis_calendar_ai_LocalModel_initModel(JNIEnv* env, jobject thiz, jstring modelPath) {
    const char* path = env->GetStringUTFChars(modelPath, 0);
    
    llama_model_params model_params = llama_model_default_params();
    model = llama_load_model_from_file(path, model_params);
    
    env->ReleaseStringUTFChars(modelPath, path);
    
    if (model == nullptr) {
        return JNI_FALSE;
    }
    
    llama_context_params ctx_params = llama_context_params_from_gpt_params(llama_model_params(model));
    ctx = llama_new_context_with_model(model, ctx_params);
    
    return ctx != nullptr ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jstring JNICALL
Java_com_jarvis_calendar_ai_LocalModel_generate(JNIEnv* env, jobject thiz, jstring prompt) {
    if (ctx == nullptr) {
        return env->NewStringUTF("Ошибка: модель не загружена");
    }
    
    const char* promptText = env->GetStringUTFChars(prompt, 0);
    
    // Упрощенная генерация
    llama_batch batch = llama_batch_get_one(promptText, -1, 0, 0);
    
    std::string result;
    int n_ctx = llama_n_ctx(ctx);
    
    for (int i = 0; i < 256; ++i) {
        llama_decode(ctx, batch);
        
        llama_token new_token = llama_sampler_sample(llama_get_sampler(ctx, 0), -1, nullptr);
        
        if (new_token == llama_token_eos(llama_get_model(ctx))) {
            break;
        }
        
        result += llama_token_to_piece(ctx, new_token);
        
        batch = llama_batch_get_one(&new_token, 1, 0, 0);
    }
    
    env->ReleaseStringUTFChars(prompt, promptText);
    
    return env->NewStringUTF(result.c_str());
}

JNIEXPORT void JNICALL
Java_com_jarvis_calendar_ai_LocalModel_freeModel(JNIEnv* env, jobject thiz) {
    if (ctx != nullptr) {
        llama_free(ctx);
        ctx = nullptr;
    }
    if (model != nullptr) {
        llama_free_model(model);
        model = nullptr;
    }
}

}
