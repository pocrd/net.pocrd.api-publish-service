cmake_minimum_required(VERSION 3.10)
project(apg_sdk C)

set(CMAKE_C_STANDARD 11)
set(CMAKE_C_STANDARD_REQUIRED ON)

# 查找依赖
find_package(CURL REQUIRED)
find_package(cJSON REQUIRED)

# 收集源文件
file(GLOB_RECURSE SOURCES "*.c")
file(GLOB_RECURSE HEADERS "*.h")

# 创建静态库
add_library(apg_sdk STATIC ${SOURCES})

target_include_directories(apg_sdk PUBLIC
    ${CMAKE_CURRENT_SOURCE_DIR}
    ${CURL_INCLUDE_DIRS}
    ${CJSON_INCLUDE_DIRS}
)

target_link_libraries(apg_sdk
    ${CURL_LIBRARIES}
    ${CJSON_LIBRARIES}
)

# 安装目标
install(TARGETS apg_sdk
    ARCHIVE DESTINATION lib
    LIBRARY DESTINATION lib
)

install(FILES ${HEADERS} DESTINATION include/apg_sdk)
