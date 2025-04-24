#!/bin/bash

# This script builds the gravity and lidar-optimize tools from the
# ALTWG distribution.

ARCH=$(uname -s)_$(uname -m)
if [ -z $1 ]; then
    echo "Usage: $0 install_dir"
    echo "e.g.   $0 3rd-party/${ARCH}/altwg"
    exit 0
fi

install_dir=$1

# Assume VTK and SPICE are installed
base=$(
    cd $(dirname $install_dir)
    pwd -P
)
export VTK_HOME=${base}/vtk
if [ -d ${VTK_HOME} ]; then
    echo "VTK found in ${VTK_HOME}"
else
    echo "VTK should be installed in ${VTK_HOME}.  Please run install-vtk.sh before this script."
    exit 0
fi

export SPICE_HOME=${base}/spice/JNISpice
if [ -d ${SPICE_HOME} ]; then
    echo "SPICE found in ${SPICE_HOME}"
else
    echo "SPICE should be installed in ${SPICE_HOME}.  Run the install-spice.sh script first."
    exit 0
fi

GSL_VERSION=2.8
export GSL_HOME=${base}/gsl
if [ ! -d $GSL_HOME ]; then
    echo "Installing GSL"
    if [ ! -e gsl-${GSL_VERSION}.tar.gz ]; then
        curl -RO https://ftp.gnu.org/gnu/gsl/gsl-${GSL_VERSION}.tar.gz
    fi
    tar xfz gsl-${GSL_VERSION}.tar.gz
    (
        cd gsl-${GSL_VERSION}
        ./configure --prefix ${GSL_HOME}
        # make install cannot find certain header files?
        make -j4
        make install
    )
fi

mkdir -p $install_dir

if [ $? -ne 0 ]; then
    echo "cannot create directory $install_dir"
    exit 1
fi

if [ ! -w $install_dir ]; then
    echo "cannot write to directory $install_dir"
    exit 1
fi

pushd $install_dir >/dev/null
install_dir=$(pwd -P)
popd >/dev/null
echo "will install ALTWG tools to $install_dir"

set -e

# location of this script
DIR=$(
    cd $(dirname $0)
    pwd -P
)

BUILD_DIR=buildaltwg
rm -rf $BUILD_DIR
mkdir -p $BUILD_DIR
cd $BUILD_DIR

export VTK_DIR=${VTK_HOME}
cmake -DCMAKE_BUILD_TYPE:String=Release "-DCMAKE_CXX_FLAGS_RELEASE:String=-O2 -DNDEBUG" ${DIR}
cmake --build . --config Release

for file in gravity lidar-optimize; do
    if [ "$(uname -s)" == "Darwin" ]; then
        # set @rpath for macOS executables
        install_name_tool -add_rpath @executable_path/../vtk/lib $file
    fi
    cp -p $file $install_dir
done

echo "installed ALTWG tools. You may remove $(pwd)."
