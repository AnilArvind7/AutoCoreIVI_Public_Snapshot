cmake_minimum_required(VERSION 3.22.1)

project("autocore_vhal")

add_library(
        autocore_vhal
        SHARED
        native-lib.cpp
)
