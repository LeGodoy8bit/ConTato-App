# IA do ConTato: llama.cpp compilado junto com o app (sem internet, roda no próprio celular).
cmake_minimum_required(VERSION 3.22.1)
project("contatollm" LANGUAGES C CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# llama.cpp fica em ConTatoApp/third_party/llama.cpp (submódulo Git, versão fixa b11860).
set(LLAMA_DIR ${CMAKE_CURRENT_SOURCE_DIR}/../../../../third_party/llama.cpp)
if(NOT EXISTS ${LLAMA_DIR}/CMakeLists.txt)
    message(FATAL_ERROR "llama.cpp não encontrado em ${LLAMA_DIR}. Veja o passo a passo da IA.")
endif()

# A IA fica MUITO lenta sem otimização. O Android Studio compila o modo debug como "Debug",
# então forçamos Release (-O3) aqui, para o app de teste também ficar rápido.
set(CMAKE_BUILD_TYPE Release)

# Só a biblioteca: nada de exemplos, servidor, ferramentas ou HTTPS.
# As bibliotecas estáticas do llama.cpp entram numa .so: precisam ser PIC.
set(CMAKE_POSITION_INDEPENDENT_CODE ON)
set(BUILD_SHARED_LIBS     OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_COMMON    OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_TESTS     OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_TOOLS     OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_EXAMPLES  OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_SERVER    OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_APP       OFF CACHE BOOL "" FORCE)
set(LLAMA_OPENSSL         OFF CACHE BOOL "" FORCE)
# Sem otimizações do PC que compila (o app roda em vários celulares) e sem OpenMP.
set(GGML_NATIVE           OFF CACHE BOOL "" FORCE)
set(GGML_OPENMP           OFF CACHE BOOL "" FORCE)
set(GGML_LLAMAFILE        OFF CACHE BOOL "" FORCE)
set(GGML_CCACHE          OFF CACHE BOOL "" FORCE)

add_subdirectory(${LLAMA_DIR} llama_build EXCLUDE_FROM_ALL)

add_library(contatollm SHARED
    contato_llm.cpp
    contato_jni.cpp
)

target_link_libraries(contatollm PRIVATE llama)
if(ANDROID)
    target_link_libraries(contatollm PRIVATE log android)
endif()
