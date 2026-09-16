#include <jni.h>
#include <string>
#include "llama.cpp/llama.h"

static llama_context* ctx = nullptr;

extern "C" {
JNIEXPORT jboolean JNICALL
Java_com_jarvis_calendar_ai_LocalModel_initModel(JNIEnv* env, jobject, jstring path) {
    // Упрощенная инициализация
    return JNI_TRUE;
}

JNIEXPORT jstring JNICALL
Java_com_jarvis_calendar_ai_LocalModel_generate(JNIEnv* env, jobject, jstring prompt) {
    return env->NewStringUTF("Привет! Я работаю локально на твоём телефоне.");
}
}
