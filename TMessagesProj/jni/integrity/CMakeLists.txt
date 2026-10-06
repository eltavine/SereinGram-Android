set(SEREIN_SIGNING_SHA1 "" CACHE STRING "Uppercase SHA-1 of the APK signing certificate; empty skips the check")

add_library(integrity STATIC
        integrity/integrity.cpp
        integrity/meth.cpp
        integrity/openat.c
        integrity/read_cert.cpp
        integrity/SHA1.cpp)
target_compile_options(integrity PUBLIC
        -Oz -Wall -fvisibility=hidden)
target_compile_definitions(integrity PRIVATE
        SEREIN_SIGNING_SHA1="${SEREIN_SIGNING_SHA1}")
target_include_directories(integrity PUBLIC
        integrity)
