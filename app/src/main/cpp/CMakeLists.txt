cmake_minimum_required(VERSION 3.22.1)
project("jarvis-ai")

# Отключаем лишние бэкенды для стабильной сборки только под CPU Android
set(LLAMA_BUILD_SHARED_LIBS ON CACHE BOOL "llama: build shared libs" FORCE)
set(LLAMA_NATIVE OFF CACHE BOOL "llama: disable native optimizations" FORCE)
set(LLAMA_BLAS OFF CACHE BOOL "llama: disable BLAS" FORCE)
set(LLAMA_VULKAN OFF CACHE BOOL "llama: disable Vulkan" FORCE)
set(LLAMA_METAL OFF CACHE BOOL "llama: disable Metal" FORCE)
set(LLAMA_CUDA OFF CACHE BOOL "llama: disable CUDA" FORCE)
set(LLAMA_BUILD_TESTS OFF CACHE BOOL "llama: disable tests" FORCE)
set(LLAMA_BUILD_EXAMPLES OFF CACHE BOOL "llama: disable examples" FORCE)

add_subdirectory(llama.cpp)

add_library(jarvis-ai SHARED
    jarvis_ai.cpp
)

target_link_libraries(jarvis-ai
    llama
    android
    log
)
