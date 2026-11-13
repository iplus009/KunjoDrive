cmake_minimum_required(VERSION 3.22.1)
project("novo_core" LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

add_library(
    novo_core
    SHARED
    novo_security_core.cpp
)

target_compile_options(
    novo_core
    PRIVATE
    -fvisibility=hidden
    -O2
)

find_library(log-lib log)

target_link_libraries(
    novo_core
    ${log-lib}
)

