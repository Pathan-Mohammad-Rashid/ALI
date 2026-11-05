cmake_minimum_required(VERSION 3.22.1)
project("ali_llama" LANGUAGES C CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Optional full llama.cpp submodule integration:
# If the user clones https://github.com/ggerganov/llama.cpp into app/src/main/cpp/llama.cpp,
# CMake automatically links the full GGUF tensor runtime with ARM64 NEON acceleration.
if(EXISTS "${CMAKE_CURRENT_SOURCE_DIR}/llama.cpp/CMakeLists.txt")
    message(STATUS "ALI: Found llama.cpp submodule, enabling full GGUF native inference")
    set(LLAMA_BUILD_COMMON OFF CACHE BOOL "" FORCE)
    set(LLAMA_BUILD_TESTS OFF CACHE BOOL "" FORCE)
    set(LLAMA_BUILD_EXAMPLES OFF CACHE BOOL "" FORCE)
    set(LLAMA_BUILD_SERVER OFF CACHE BOOL "" FORCE)
    add_subdirectory(llama.cpp)
    add_definitions(-DALI_HAS_LLAMA_CPP=1)
endif()

add_library(
    ali_llama
    SHARED
    llama_jni.cpp
)

find_library(
    log-lib
    log
)

if(EXISTS "${CMAKE_CURRENT_SOURCE_DIR}/llama.cpp/CMakeLists.txt")
    target_link_libraries(
        ali_llama
        llama
        ggml
        ${log-lib}
    )
else()
    target_link_libraries(
        ali_llama
        ${log-lib}
    )
endif()
