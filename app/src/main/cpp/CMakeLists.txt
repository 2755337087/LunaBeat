cmake_minimum_required(VERSION 3.22.1)

project(lyricbox)

set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -O3 -fvisibility=hidden -flto -fdata-sections -ffunction-sections -fomit-frame-pointer")
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -O3 -fvisibility=hidden -flto -fdata-sections -ffunction-sections -fomit-frame-pointer")
set(CMAKE_SHARED_LINKER_FLAGS "${CMAKE_SHARED_LINKER_FLAGS} -flto -Wl,--exclude-libs,ALL -Wl,--gc-sections -s -Wl,--pack-dyn-relocs=android -Wl,--build-id=none")

set(CMAKE_C_VISIBILITY_PRESET hidden)
set(CMAKE_VISIBILITY_INLINES_HIDDEN YES)

add_library(${CMAKE_PROJECT_NAME} SHARED
        native-lib.cpp)

target_link_libraries(${CMAKE_PROJECT_NAME}
        android
        log)

add_custom_command(TARGET ${CMAKE_PROJECT_NAME} POST_BUILD
        COMMAND ${CMAKE_OBJCOPY}
        --remove-section .comment
        --remove-section .note
        --strip-debug $<TARGET_FILE:${CMAKE_PROJECT_NAME}>)
