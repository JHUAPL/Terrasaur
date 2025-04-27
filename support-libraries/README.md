# Supporting libraries for Terrasaur

The Terrasaur code is available at [GitHub](https://github.com/JHUAPL/Terrasaur.git).  This directory contains these supporting libraries needed to build Terrasaur:

* [GSL](https://www.gnu.org/software/gsl/)
* [SPICE](https://naif.jpl.nasa.gov)
* [VTK](https://vtk.org)
* [OpenCV](https://opencv.org/)

# Shortcut

The support code may take a long time to compile (several hours in some cases).  You can instead download a binary package for your architecture from [GitHub](https://github.com/JHUAPL/Terrasaur/releases).  The compiled 3rd party products are found in the `lib` directory (e.g. `Terrasaur-YYYY.MM.DD/lib/Darwin_arm64`).  Copy or move this directory to your source directory under `3rd-party`.  For example:

```
    mkdir 3rd-party
    tar xfz Terrasaur-YYYY.MM.DD.tar.gz
    ln -s Terrasaur-YYYY.MM.DD/lib/Darwin_arm64 3rd-party/
```

You can now compile the Terrasaur code using the released support files.

# Building the code

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

The `buildAll.bash` script will build and install the libraries and executables to the desired location.  For example, this would install everything in a directory named for your system architecture (e.g. 3rd-party/Darwin_x86_64 on an intel macOS system):

```
./buildAll.bash ../3rd-party/$(uname -s)_$(uname -m)
```

### Note for macOS users

We have found that the VTK library will not compile with the XCode command line tools version 16.3 with clang-1700.0.13.3.  We could successfully compile after downgrading to the Xcode 16 command line tools with clang-1600.026.3.