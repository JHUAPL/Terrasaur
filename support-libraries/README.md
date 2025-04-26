# Supporting libraries for Terrasaur

The Terrasaur code is available at [GitHub](https://github.com/JHUAPL/Terrasaur.git).  This directory contains supporting libraries needed to build Terrasaur.

* [GSL](https://www.gnu.org/software/gsl/)
* [SPICE](https://naif.jpl.nasa.gov)
* [VTK](https://vtk.org)
* [OpenCV](https://opencv.org/)

## Prerequisites

You may need to install the following packages with 
your favorite package manager:
* [Apache Ant](https://ant.apache.org/) - needed for OpenCV Java support
* [CMake](https://cmake.org/) - used to build VTK and OpenCV
* [SQLite](https://www.sqlite.org/index.html) - needed for lidar-optimize

## Build the packages

Each subdirectory has a build script which takes a single argument,
which is the installation location for the compiled libraries and
executables.  

The `buildAll.bash` script will build and install the libraries and executables
to the desired location.  For example, this would install everything in a 
directory named for your system architecture (e.g. 3rd-party/Darwin_x86_64 on 
an intel macOS system):

```
./buildAll.bash ../3rd-party/$(uname -s)_$(uname -m)
```

### Note for macOS users

We have found that the VTK library will not compile with the XCode command line tools version 16.3 with clang-1700.0.13.3.  We could successfully compile after downgrading to the Xcode 16 command line tools with clang-1600.026.3.