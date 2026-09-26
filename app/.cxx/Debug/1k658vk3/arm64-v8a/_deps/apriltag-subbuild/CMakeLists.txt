# Distributed under the OSI-approved BSD 3-Clause License.  See accompanying
# file Copyright.txt or https://cmake.org/licensing for details.

cmake_minimum_required(VERSION 3.22.1-g37088a8)

# We name the project and the target for the ExternalProject_Add() call
# to something that will highlight to the user what we are working on if
# something goes wrong and an error message is produced.

project(apriltag-populate NONE)


# Pass through things we've already detected in the main project to avoid
# paying the cost of redetecting them again in ExternalProject_Add()
set(GIT_EXECUTABLE [==[/usr/bin/git]==])
set(GIT_VERSION_STRING [==[2.53.0]==])
set_property(GLOBAL PROPERTY _CMAKE_FindGit_GIT_EXECUTABLE_VERSION
  [==[/usr/bin/git;2.53.0]==]
)


include(ExternalProject)
ExternalProject_Add(apriltag-populate
                     "UPDATE_DISCONNECTED" "False" "GIT_REPOSITORY" "https://github.com/AprilRobotics/apriltag.git" "GIT_TAG" "v3.4.2"
                    SOURCE_DIR          "/home/aneesh/phonevisionstuff/FRC-Phonevision-main/app/.cxx/Debug/1k658vk3/arm64-v8a/_deps/apriltag-src"
                    BINARY_DIR          "/home/aneesh/phonevisionstuff/FRC-Phonevision-main/app/.cxx/Debug/1k658vk3/arm64-v8a/_deps/apriltag-build"
                    CONFIGURE_COMMAND   ""
                    BUILD_COMMAND       ""
                    INSTALL_COMMAND     ""
                    TEST_COMMAND        ""
                    USES_TERMINAL_DOWNLOAD  YES
                    USES_TERMINAL_UPDATE    YES
)


