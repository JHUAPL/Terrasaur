cmake_minimum_required(VERSION 3.17)

PROJECT(SBMT)

find_package(SQLite3 REQUIRED)

# built from install-altwg.sh
# find_package(GSL REQUIRED)

set(GSL_HOME $ENV{GSL_HOME})
set(VTK_DIR $ENV{VTK_HOME}/lib/cmake/vtk-9.0/)
find_package(VTK COMPONENTS vtkFiltersCore vtkIOLegacy vtkIOGeometry NO_MODULE)

set(SPICE_DIR $ENV{SPICE_HOME} CACHE PATH "Path to Spice")

add_definitions(-Wall)

# Print all environment variables
#get_cmake_property(_variableNames VARIABLES)
#list (SORT _variableNames)
#foreach (_variableName ${_variableNames})
#    message(STATUS "${_variableName}=${${_variableName}}")
#endforeach()

include_directories(
${GSL_HOME}/include
${SPICE_DIR}/include
${CMAKE_SOURCE_DIR}
${CMAKE_SOURCE_DIR}/gravity
${CMAKE_SOURCE_DIR}/common
)

add_library(lidar
gravity/gravity-werner.cpp
gravity/gravity-cheng.cpp
gravity/gravity-point.h
gravity/platemodel.cpp
common/util.h
common/mathutil.h
strip-adjustment/optimizer.cpp
strip-adjustment/lidardata.cpp
strip-adjustment/closest-point-vtk.cpp
strip-adjustment/icp-gsl.cpp
strip-adjustment/optimize-gsl.cpp
)
target_link_libraries(lidar ${GSL_HOME}/lib/libgsl.a ${GSL_HOME}/lib/libgslcblas.a ${VTK_LIBRARIES} ${SPICE_DIR}/lib/cspice.a sqlite3)

add_executable(gravity gravity/gravity.cpp)
target_link_libraries(gravity lidar)

add_executable(lidar-optimize strip-adjustment/lidar-optimize.cpp)
target_link_libraries(lidar-optimize lidar)
