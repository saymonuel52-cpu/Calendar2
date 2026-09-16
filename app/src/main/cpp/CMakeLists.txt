cmake_minimum_required(VERSION 3.22.1)
project("jarvis-llama")

set(LLAMA_BUILD_SHARED_LIBS ON CACHE BOOL "llama: build shared libs" FORCE)
set(LLAMA_NATIVE OFF CACHE BOOL "llama: disable native optimizations" FORCE)

add_subdirectory(llama.cpp)

add_library(jarvis-ai SHARED
    jarvis_ai.cpp
)

target_link_libraries(jarvis-ai
    llama
    android
    log
)
