#----------------------------------------------------------------
# Generated CMake target import file for configuration "Debug".
#----------------------------------------------------------------

# Commands may need to know the format version.
set(CMAKE_IMPORT_FILE_VERSION 1)

# Import target "apriltag::apriltag" for configuration "Debug"
set_property(TARGET apriltag::apriltag APPEND PROPERTY IMPORTED_CONFIGURATIONS DEBUG)
set_target_properties(apriltag::apriltag PROPERTIES
  IMPORTED_LINK_INTERFACE_LANGUAGES_DEBUG "C"
  IMPORTED_LOCATION_DEBUG "${_IMPORT_PREFIX}/lib/libapriltagd.a"
  )

list(APPEND _IMPORT_CHECK_TARGETS apriltag::apriltag )
list(APPEND _IMPORT_CHECK_FILES_FOR_apriltag::apriltag "${_IMPORT_PREFIX}/lib/libapriltagd.a" )

# Commands beyond this point should not need to know the version.
set(CMAKE_IMPORT_FILE_VERSION)
