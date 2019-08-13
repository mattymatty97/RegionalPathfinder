# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html

# Sets the minimum version of CMake required to build the native library.

cmake_minimum_required(VERSION 3.4.1)

# Creates and names a library, sets it as either STATIC
# or SHARED, and provides the relative paths to its source code.
# You can define multiple libraries, and CMake builds them for you.
# Gradle automatically packages shared libraries with your APK.

include_directories(/usr/lib/jvm/java-8-openjdk-amd64/include)
include_directories(/usr/lib/jvm/java-8-openjdk-amd64/include/linux)

add_library(c-graph
        SHARED
        libs/Edge.h
        libs/Edge.cpp
        libs/Node.h
        libs/Node.cpp
        libs/PQ.h
        libs/PQ.cpp
        libs/Dijkstra.h
        libs/Dijkstra.cpp
        graph.h
        graph.cpp
        graph_adapter.cpp)


# Specifies libraries CMake should link to your target library. You
# can link multiple libraries, such as libraries you define in this
# build script, prebuilt third-party libraries, or system libraries.

target_link_libraries( c-graph )