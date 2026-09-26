# Install script for directory: /home/aneesh/phonevisionstuff/FRC-Phonevision-main/app/.cxx/Debug/1k658vk3/arm64-v8a/_deps/apriltag-src

# Set the install prefix
if(NOT DEFINED CMAKE_INSTALL_PREFIX)
  set(CMAKE_INSTALL_PREFIX "/usr/local")
endif()
string(REGEX REPLACE "/$" "" CMAKE_INSTALL_PREFIX "${CMAKE_INSTALL_PREFIX}")

# Set the install configuration name.
if(NOT DEFINED CMAKE_INSTALL_CONFIG_NAME)
  if(BUILD_TYPE)
    string(REGEX REPLACE "^[^A-Za-z0-9_]+" ""
           CMAKE_INSTALL_CONFIG_NAME "${BUILD_TYPE}")
  else()
    set(CMAKE_INSTALL_CONFIG_NAME "Debug")
  endif()
  message(STATUS "Install configuration: \"${CMAKE_INSTALL_CONFIG_NAME}\"")
endif()

# Set the component getting installed.
if(NOT CMAKE_INSTALL_COMPONENT)
  if(COMPONENT)
    message(STATUS "Install component: \"${COMPONENT}\"")
    set(CMAKE_INSTALL_COMPONENT "${COMPONENT}")
  else()
    set(CMAKE_INSTALL_COMPONENT)
  endif()
endif()

# Install shared libraries without execute permission?
if(NOT DEFINED CMAKE_INSTALL_SO_NO_EXE)
  set(CMAKE_INSTALL_SO_NO_EXE "1")
endif()

# Is this installation the result of a crosscompile?
if(NOT DEFINED CMAKE_CROSSCOMPILING)
  set(CMAKE_CROSSCOMPILING "TRUE")
endif()

# Set default install directory permissions.
if(NOT DEFINED CMAKE_OBJDUMP)
  set(CMAKE_OBJDUMP "/home/aneesh/Android/Sdk/ndk/28.2.13676358/toolchains/llvm/prebuilt/linux-x86_64/bin/llvm-objdump")
endif()

if("x${CMAKE_INSTALL_COMPONENT}x" STREQUAL "xUnspecifiedx" OR NOT CMAKE_INSTALL_COMPONENT)
  file(INSTALL DESTINATION "${CMAKE_INSTALL_PREFIX}/include/apriltag" TYPE FILE FILES "/home/aneesh/phonevisionstuff/FRC-Phonevision-main/app/.cxx/Debug/1k658vk3/arm64-v8a/_deps/apriltag-src/apriltag.h")
endif()

if("x${CMAKE_INSTALL_COMPONENT}x" STREQUAL "xUnspecifiedx" OR NOT CMAKE_INSTALL_COMPONENT)
  file(INSTALL DESTINATION "${CMAKE_INSTALL_PREFIX}/include/apriltag" TYPE FILE FILES "/home/aneesh/phonevisionstuff/FRC-Phonevision-main/app/.cxx/Debug/1k658vk3/arm64-v8a/_deps/apriltag-src/apriltag_math.h")
endif()

if("x${CMAKE_INSTALL_COMPONENT}x" STREQUAL "xUnspecifiedx" OR NOT CMAKE_INSTALL_COMPONENT)
  file(INSTALL DESTINATION "${CMAKE_INSTALL_PREFIX}/include/apriltag" TYPE FILE FILES "/home/aneesh/phonevisionstuff/FRC-Phonevision-main/app/.cxx/Debug/1k658vk3/arm64-v8a/_deps/apriltag-src/apriltag_pose.h")
endif()

if("x${CMAKE_INSTALL_COMPONENT}x" STREQUAL "xUnspecifiedx" OR NOT CMAKE_INSTALL_COMPONENT)
  file(INSTALL DESTINATION "${CMAKE_INSTALL_PREFIX}/include/apriltag/common" TYPE FILE FILES "/home/aneesh/phonevisionstuff/FRC-Phonevision-main/app/.cxx/Debug/1k658vk3/arm64-v8a/_deps/apriltag-src/common/debug_print.h")
endif()

if("x${CMAKE_INSTALL_COMPONENT}x" STREQUAL "xUnspecifiedx" OR NOT CMAKE_INSTALL_COMPONENT)
  file(INSTALL DESTINATION "${CMAKE_INSTALL_PREFIX}/include/apriltag/common" TYPE FILE FILES "/home/aneesh/phonevisionstuff/FRC-Phonevision-main/app/.cxx/Debug/1k658vk3/arm64-v8a/_deps/apriltag-src/common/doubles.h")
endif()

if("x${CMAKE_INSTALL_COMPONENT}x" STREQUAL "xUnspecifiedx" OR NOT CMAKE_INSTALL_COMPONENT)
  file(INSTALL DESTINATION "${CMAKE_INSTALL_PREFIX}/include/apriltag/common" TYPE FILE FILES "/home/aneesh/phonevisionstuff/FRC-Phonevision-main/app/.cxx/Debug/1k658vk3/arm64-v8a/_deps/apriltag-src/common/doubles_floats_impl.h")
endif()

if("x${CMAKE_INSTALL_COMPONENT}x" STREQUAL "xUnspecifiedx" OR NOT CMAKE_INSTALL_COMPONENT)
  file(INSTALL DESTINATION "${CMAKE_INSTALL_PREFIX}/include/apriltag/common" TYPE FILE FILES "/home/aneesh/phonevisionstuff/FRC-Phonevision-main/app/.cxx/Debug/1k658vk3/arm64-v8a/_deps/apriltag-src/common/floats.h")
endif()

if("x${CMAKE_INSTALL_COMPONENT}x" STREQUAL "xUnspecifiedx" OR NOT CMAKE_INSTALL_COMPONENT)
  file(INSTALL DESTINATION "${CMAKE_INSTALL_PREFIX}/include/apriltag/common" TYPE FILE FILES "/home/aneesh/phonevisionstuff/FRC-Phonevision-main/app/.cxx/Debug/1k658vk3/arm64-v8a/_deps/apriltag-src/common/g2d.h")
endif()

if("x${CMAKE_INSTALL_COMPONENT}x" STREQUAL "xUnspecifiedx" OR NOT CMAKE_INSTALL_COMPONENT)
  file(INSTALL DESTINATION "${CMAKE_INSTALL_PREFIX}/include/apriltag/common" TYPE FILE FILES "/home/aneesh/phonevisionstuff/FRC-Phonevision-main/app/.cxx/Debug/1k658vk3/arm64-v8a/_deps/apriltag-src/common/getopt.h")
endif()

if("x${CMAKE_INSTALL_COMPONENT}x" STREQUAL "xUnspecifiedx" OR NOT CMAKE_INSTALL_COMPONENT)
  file(INSTALL DESTINATION "${CMAKE_INSTALL_PREFIX}/include/apriltag/common" TYPE FILE FILES "/home/aneesh/phonevisionstuff/FRC-Phonevision-main/app/.cxx/Debug/1k658vk3/arm64-v8a/_deps/apriltag-src/common/homography.h")
endif()

if("x${CMAKE_INSTALL_COMPONENT}x" STREQUAL "xUnspecifiedx" OR NOT CMAKE_INSTALL_COMPONENT)
  file(INSTALL DESTINATION "${CMAKE_INSTALL_PREFIX}/include/apriltag/common" TYPE FILE FILES "/home/aneesh/phonevisionstuff/FRC-Phonevision-main/app/.cxx/Debug/1k658vk3/arm64-v8a/_deps/apriltag-src/common/image_types.h")
endif()

if("x${CMAKE_INSTALL_COMPONENT}x" STREQUAL "xUnspecifiedx" OR NOT CMAKE_INSTALL_COMPONENT)
  file(INSTALL DESTINATION "${CMAKE_INSTALL_PREFIX}/include/apriltag/common" TYPE FILE FILES "/home/aneesh/phonevisionstuff/FRC-Phonevision-main/app/.cxx/Debug/1k658vk3/arm64-v8a/_deps/apriltag-src/common/image_u8.h")
endif()

if("x${CMAKE_INSTALL_COMPONENT}x" STREQUAL "xUnspecifiedx" OR NOT CMAKE_INSTALL_COMPONENT)
  file(INSTALL DESTINATION "${CMAKE_INSTALL_PREFIX}/include/apriltag/common" TYPE FILE FILES "/home/aneesh/phonevisionstuff/FRC-Phonevision-main/app/.cxx/Debug/1k658vk3/arm64-v8a/_deps/apriltag-src/common/image_u8x3.h")
endif()

if("x${CMAKE_INSTALL_COMPONENT}x" STREQUAL "xUnspecifiedx" OR NOT CMAKE_INSTALL_COMPONENT)
  file(INSTALL DESTINATION "${CMAKE_INSTALL_PREFIX}/include/apriltag/common" TYPE FILE FILES "/home/aneesh/phonevisionstuff/FRC-Phonevision-main/app/.cxx/Debug/1k658vk3/arm64-v8a/_deps/apriltag-src/common/image_u8x4.h")
endif()

if("x${CMAKE_INSTALL_COMPONENT}x" STREQUAL "xUnspecifiedx" OR NOT CMAKE_INSTALL_COMPONENT)
  file(INSTALL DESTINATION "${CMAKE_INSTALL_PREFIX}/include/apriltag/common" TYPE FILE FILES "/home/aneesh/phonevisionstuff/FRC-Phonevision-main/app/.cxx/Debug/1k658vk3/arm64-v8a/_deps/apriltag-src/common/matd.h")
endif()

if("x${CMAKE_INSTALL_COMPONENT}x" STREQUAL "xUnspecifiedx" OR NOT CMAKE_INSTALL_COMPONENT)
  file(INSTALL DESTINATION "${CMAKE_INSTALL_PREFIX}/include/apriltag/common" TYPE FILE FILES "/home/aneesh/phonevisionstuff/FRC-Phonevision-main/app/.cxx/Debug/1k658vk3/arm64-v8a/_deps/apriltag-src/common/math_util.h")
endif()

if("x${CMAKE_INSTALL_COMPONENT}x" STREQUAL "xUnspecifiedx" OR NOT CMAKE_INSTALL_COMPONENT)
  file(INSTALL DESTINATION "${CMAKE_INSTALL_PREFIX}/include/apriltag/common" TYPE FILE FILES "/home/aneesh/phonevisionstuff/FRC-Phonevision-main/app/.cxx/Debug/1k658vk3/arm64-v8a/_deps/apriltag-src/common/pam.h")
endif()

if("x${CMAKE_INSTALL_COMPONENT}x" STREQUAL "xUnspecifiedx" OR NOT CMAKE_INSTALL_COMPONENT)
  file(INSTALL DESTINATION "${CMAKE_INSTALL_PREFIX}/include/apriltag/common" TYPE FILE FILES "/home/aneesh/phonevisionstuff/FRC-Phonevision-main/app/.cxx/Debug/1k658vk3/arm64-v8a/_deps/apriltag-src/common/pjpeg.h")
endif()

if("x${CMAKE_INSTALL_COMPONENT}x" STREQUAL "xUnspecifiedx" OR NOT CMAKE_INSTALL_COMPONENT)
  file(INSTALL DESTINATION "${CMAKE_INSTALL_PREFIX}/include/apriltag/common" TYPE FILE FILES "/home/aneesh/phonevisionstuff/FRC-Phonevision-main/app/.cxx/Debug/1k658vk3/arm64-v8a/_deps/apriltag-src/common/pnm.h")
endif()

if("x${CMAKE_INSTALL_COMPONENT}x" STREQUAL "xUnspecifiedx" OR NOT CMAKE_INSTALL_COMPONENT)
  file(INSTALL DESTINATION "${CMAKE_INSTALL_PREFIX}/include/apriltag/common" TYPE FILE FILES "/home/aneesh/phonevisionstuff/FRC-Phonevision-main/app/.cxx/Debug/1k658vk3/arm64-v8a/_deps/apriltag-src/common/postscript_utils.h")
endif()

if("x${CMAKE_INSTALL_COMPONENT}x" STREQUAL "xUnspecifiedx" OR NOT CMAKE_INSTALL_COMPONENT)
  file(INSTALL DESTINATION "${CMAKE_INSTALL_PREFIX}/include/apriltag/common" TYPE FILE FILES "/home/aneesh/phonevisionstuff/FRC-Phonevision-main/app/.cxx/Debug/1k658vk3/arm64-v8a/_deps/apriltag-src/common/pthreads_cross.h")
endif()

if("x${CMAKE_INSTALL_COMPONENT}x" STREQUAL "xUnspecifiedx" OR NOT CMAKE_INSTALL_COMPONENT)
  file(INSTALL DESTINATION "${CMAKE_INSTALL_PREFIX}/include/apriltag/common" TYPE FILE FILES "/home/aneesh/phonevisionstuff/FRC-Phonevision-main/app/.cxx/Debug/1k658vk3/arm64-v8a/_deps/apriltag-src/common/string_util.h")
endif()

if("x${CMAKE_INSTALL_COMPONENT}x" STREQUAL "xUnspecifiedx" OR NOT CMAKE_INSTALL_COMPONENT)
  file(INSTALL DESTINATION "${CMAKE_INSTALL_PREFIX}/include/apriltag/common" TYPE FILE FILES "/home/aneesh/phonevisionstuff/FRC-Phonevision-main/app/.cxx/Debug/1k658vk3/arm64-v8a/_deps/apriltag-src/common/svd22.h")
endif()

if("x${CMAKE_INSTALL_COMPONENT}x" STREQUAL "xUnspecifiedx" OR NOT CMAKE_INSTALL_COMPONENT)
  file(INSTALL DESTINATION "${CMAKE_INSTALL_PREFIX}/include/apriltag/common" TYPE FILE FILES "/home/aneesh/phonevisionstuff/FRC-Phonevision-main/app/.cxx/Debug/1k658vk3/arm64-v8a/_deps/apriltag-src/common/time_util.h")
endif()

if("x${CMAKE_INSTALL_COMPONENT}x" STREQUAL "xUnspecifiedx" OR NOT CMAKE_INSTALL_COMPONENT)
  file(INSTALL DESTINATION "${CMAKE_INSTALL_PREFIX}/include/apriltag/common" TYPE FILE FILES "/home/aneesh/phonevisionstuff/FRC-Phonevision-main/app/.cxx/Debug/1k658vk3/arm64-v8a/_deps/apriltag-src/common/timeprofile.h")
endif()

if("x${CMAKE_INSTALL_COMPONENT}x" STREQUAL "xUnspecifiedx" OR NOT CMAKE_INSTALL_COMPONENT)
  file(INSTALL DESTINATION "${CMAKE_INSTALL_PREFIX}/include/apriltag/common" TYPE FILE FILES "/home/aneesh/phonevisionstuff/FRC-Phonevision-main/app/.cxx/Debug/1k658vk3/arm64-v8a/_deps/apriltag-src/common/unionfind.h")
endif()

if("x${CMAKE_INSTALL_COMPONENT}x" STREQUAL "xUnspecifiedx" OR NOT CMAKE_INSTALL_COMPONENT)
  file(INSTALL DESTINATION "${CMAKE_INSTALL_PREFIX}/include/apriltag/common" TYPE FILE FILES "/home/aneesh/phonevisionstuff/FRC-Phonevision-main/app/.cxx/Debug/1k658vk3/arm64-v8a/_deps/apriltag-src/common/workerpool.h")
endif()

if("x${CMAKE_INSTALL_COMPONENT}x" STREQUAL "xUnspecifiedx" OR NOT CMAKE_INSTALL_COMPONENT)
  file(INSTALL DESTINATION "${CMAKE_INSTALL_PREFIX}/include/apriltag/common" TYPE FILE FILES "/home/aneesh/phonevisionstuff/FRC-Phonevision-main/app/.cxx/Debug/1k658vk3/arm64-v8a/_deps/apriltag-src/common/zarray.h")
endif()

if("x${CMAKE_INSTALL_COMPONENT}x" STREQUAL "xUnspecifiedx" OR NOT CMAKE_INSTALL_COMPONENT)
  file(INSTALL DESTINATION "${CMAKE_INSTALL_PREFIX}/include/apriltag/common" TYPE FILE FILES "/home/aneesh/phonevisionstuff/FRC-Phonevision-main/app/.cxx/Debug/1k658vk3/arm64-v8a/_deps/apriltag-src/common/zhash.h")
endif()

if("x${CMAKE_INSTALL_COMPONENT}x" STREQUAL "xUnspecifiedx" OR NOT CMAKE_INSTALL_COMPONENT)
  file(INSTALL DESTINATION "${CMAKE_INSTALL_PREFIX}/include/apriltag/common" TYPE FILE FILES "/home/aneesh/phonevisionstuff/FRC-Phonevision-main/app/.cxx/Debug/1k658vk3/arm64-v8a/_deps/apriltag-src/common/zmaxheap.h")
endif()

if("x${CMAKE_INSTALL_COMPONENT}x" STREQUAL "xUnspecifiedx" OR NOT CMAKE_INSTALL_COMPONENT)
  file(INSTALL DESTINATION "${CMAKE_INSTALL_PREFIX}/include/apriltag" TYPE FILE FILES "/home/aneesh/phonevisionstuff/FRC-Phonevision-main/app/.cxx/Debug/1k658vk3/arm64-v8a/_deps/apriltag-src/tag16h5.h")
endif()

if("x${CMAKE_INSTALL_COMPONENT}x" STREQUAL "xUnspecifiedx" OR NOT CMAKE_INSTALL_COMPONENT)
  file(INSTALL DESTINATION "${CMAKE_INSTALL_PREFIX}/include/apriltag" TYPE FILE FILES "/home/aneesh/phonevisionstuff/FRC-Phonevision-main/app/.cxx/Debug/1k658vk3/arm64-v8a/_deps/apriltag-src/tag25h9.h")
endif()

if("x${CMAKE_INSTALL_COMPONENT}x" STREQUAL "xUnspecifiedx" OR NOT CMAKE_INSTALL_COMPONENT)
  file(INSTALL DESTINATION "${CMAKE_INSTALL_PREFIX}/include/apriltag" TYPE FILE FILES "/home/aneesh/phonevisionstuff/FRC-Phonevision-main/app/.cxx/Debug/1k658vk3/arm64-v8a/_deps/apriltag-src/tag36h10.h")
endif()

if("x${CMAKE_INSTALL_COMPONENT}x" STREQUAL "xUnspecifiedx" OR NOT CMAKE_INSTALL_COMPONENT)
  file(INSTALL DESTINATION "${CMAKE_INSTALL_PREFIX}/include/apriltag" TYPE FILE FILES "/home/aneesh/phonevisionstuff/FRC-Phonevision-main/app/.cxx/Debug/1k658vk3/arm64-v8a/_deps/apriltag-src/tag36h11.h")
endif()

if("x${CMAKE_INSTALL_COMPONENT}x" STREQUAL "xUnspecifiedx" OR NOT CMAKE_INSTALL_COMPONENT)
  file(INSTALL DESTINATION "${CMAKE_INSTALL_PREFIX}/include/apriltag" TYPE FILE FILES "/home/aneesh/phonevisionstuff/FRC-Phonevision-main/app/.cxx/Debug/1k658vk3/arm64-v8a/_deps/apriltag-src/tagCircle21h7.h")
endif()

if("x${CMAKE_INSTALL_COMPONENT}x" STREQUAL "xUnspecifiedx" OR NOT CMAKE_INSTALL_COMPONENT)
  file(INSTALL DESTINATION "${CMAKE_INSTALL_PREFIX}/include/apriltag" TYPE FILE FILES "/home/aneesh/phonevisionstuff/FRC-Phonevision-main/app/.cxx/Debug/1k658vk3/arm64-v8a/_deps/apriltag-src/tagCircle49h12.h")
endif()

if("x${CMAKE_INSTALL_COMPONENT}x" STREQUAL "xUnspecifiedx" OR NOT CMAKE_INSTALL_COMPONENT)
  file(INSTALL DESTINATION "${CMAKE_INSTALL_PREFIX}/include/apriltag" TYPE FILE FILES "/home/aneesh/phonevisionstuff/FRC-Phonevision-main/app/.cxx/Debug/1k658vk3/arm64-v8a/_deps/apriltag-src/tagCustom48h12.h")
endif()

if("x${CMAKE_INSTALL_COMPONENT}x" STREQUAL "xUnspecifiedx" OR NOT CMAKE_INSTALL_COMPONENT)
  file(INSTALL DESTINATION "${CMAKE_INSTALL_PREFIX}/include/apriltag" TYPE FILE FILES "/home/aneesh/phonevisionstuff/FRC-Phonevision-main/app/.cxx/Debug/1k658vk3/arm64-v8a/_deps/apriltag-src/tagStandard41h12.h")
endif()

if("x${CMAKE_INSTALL_COMPONENT}x" STREQUAL "xUnspecifiedx" OR NOT CMAKE_INSTALL_COMPONENT)
  file(INSTALL DESTINATION "${CMAKE_INSTALL_PREFIX}/include/apriltag" TYPE FILE FILES "/home/aneesh/phonevisionstuff/FRC-Phonevision-main/app/.cxx/Debug/1k658vk3/arm64-v8a/_deps/apriltag-src/tagStandard52h13.h")
endif()

if("x${CMAKE_INSTALL_COMPONENT}x" STREQUAL "xUnspecifiedx" OR NOT CMAKE_INSTALL_COMPONENT)
  file(INSTALL DESTINATION "${CMAKE_INSTALL_PREFIX}/lib" TYPE STATIC_LIBRARY FILES "/home/aneesh/phonevisionstuff/FRC-Phonevision-main/app/.cxx/Debug/1k658vk3/arm64-v8a/_deps/apriltag-build/libapriltagd.a")
endif()

if("x${CMAKE_INSTALL_COMPONENT}x" STREQUAL "xUnspecifiedx" OR NOT CMAKE_INSTALL_COMPONENT)
  if(EXISTS "$ENV{DESTDIR}${CMAKE_INSTALL_PREFIX}/share/apriltag/cmake/apriltagTargets.cmake")
    file(DIFFERENT EXPORT_FILE_CHANGED FILES
         "$ENV{DESTDIR}${CMAKE_INSTALL_PREFIX}/share/apriltag/cmake/apriltagTargets.cmake"
         "/home/aneesh/phonevisionstuff/FRC-Phonevision-main/app/.cxx/Debug/1k658vk3/arm64-v8a/_deps/apriltag-build/CMakeFiles/Export/share/apriltag/cmake/apriltagTargets.cmake")
    if(EXPORT_FILE_CHANGED)
      file(GLOB OLD_CONFIG_FILES "$ENV{DESTDIR}${CMAKE_INSTALL_PREFIX}/share/apriltag/cmake/apriltagTargets-*.cmake")
      if(OLD_CONFIG_FILES)
        message(STATUS "Old export file \"$ENV{DESTDIR}${CMAKE_INSTALL_PREFIX}/share/apriltag/cmake/apriltagTargets.cmake\" will be replaced.  Removing files [${OLD_CONFIG_FILES}].")
        file(REMOVE ${OLD_CONFIG_FILES})
      endif()
    endif()
  endif()
  file(INSTALL DESTINATION "${CMAKE_INSTALL_PREFIX}/share/apriltag/cmake" TYPE FILE FILES "/home/aneesh/phonevisionstuff/FRC-Phonevision-main/app/.cxx/Debug/1k658vk3/arm64-v8a/_deps/apriltag-build/CMakeFiles/Export/share/apriltag/cmake/apriltagTargets.cmake")
  if("${CMAKE_INSTALL_CONFIG_NAME}" MATCHES "^([Dd][Ee][Bb][Uu][Gg])$")
    file(INSTALL DESTINATION "${CMAKE_INSTALL_PREFIX}/share/apriltag/cmake" TYPE FILE FILES "/home/aneesh/phonevisionstuff/FRC-Phonevision-main/app/.cxx/Debug/1k658vk3/arm64-v8a/_deps/apriltag-build/CMakeFiles/Export/share/apriltag/cmake/apriltagTargets-debug.cmake")
  endif()
endif()

if("x${CMAKE_INSTALL_COMPONENT}x" STREQUAL "xUnspecifiedx" OR NOT CMAKE_INSTALL_COMPONENT)
  file(INSTALL DESTINATION "${CMAKE_INSTALL_PREFIX}/share/apriltag/cmake" TYPE FILE FILES
    "/home/aneesh/phonevisionstuff/FRC-Phonevision-main/app/.cxx/Debug/1k658vk3/arm64-v8a/_deps/apriltag-build/generated/apriltagConfig.cmake"
    "/home/aneesh/phonevisionstuff/FRC-Phonevision-main/app/.cxx/Debug/1k658vk3/arm64-v8a/_deps/apriltag-build/generated/apriltagConfigVersion.cmake"
    )
endif()

if("x${CMAKE_INSTALL_COMPONENT}x" STREQUAL "xUnspecifiedx" OR NOT CMAKE_INSTALL_COMPONENT)
  file(INSTALL DESTINATION "${CMAKE_INSTALL_PREFIX}/lib/pkgconfig" TYPE FILE FILES "/home/aneesh/phonevisionstuff/FRC-Phonevision-main/app/.cxx/Debug/1k658vk3/arm64-v8a/_deps/apriltag-build/apriltag.pc")
endif()

