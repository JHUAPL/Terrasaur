#!/bin/bash

ARCH=$(uname -s)_$(uname -m)
if [ -z $1 ]; then
    echo "Usage: $0 install_dir"
    echo "e.g.   $0 3rd-party/"'$(uname -s)_$(uname -m)'
    echo -e "\nThis would build everything in 3rd-party/$(uname -s)_$(uname -m) on this machine."
    exit 0
fi

install_dir=$1
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
echo "will install to $install_dir"

set -e

# location of this script
DIR=$(
    cd $(dirname $0)
    pwd -P
)

build_dir=build
mkdir -p $build_dir
if [ $? -ne 0 ]; then
    echo "cannot create directory $build_dir"
    exit 1
fi

if [ ! -w $build_dir ]; then
    echo "cannot write to directory $build_dir"
    exit 1
fi

cd $build_dir

VTK_INSTALL=${install_dir}/vtk
if [ -d ${VTK_INSTALL} ]; then
    echo "${VTK_INSTALL} exists.  If you want to build VTK, remove this directory and call this script again."
else
    ${DIR}/vtk/install-vtk.sh $VTK_INSTALL
fi

SPICE_INSTALL=${install_dir}/spice
if [ -d ${SPICE_INSTALL} ]; then
    echo "${SPICE_INSTALL} exists.  If you want to build SPICE, remove this directory and call this script again."
else
    ${DIR}/spice/install-spice.sh $SPICE_INSTALL
fi

ALTWG_INSTALL=${install_dir}/altwg
if [ -d ${ALTWG_INSTALL} ]; then
    echo "${ALTWG_INSTALL} exists.  If you want to build the ALTWG tools, remove this directory and call this script again."
else
    ${DIR}/altwg/install-altwg.sh $ALTWG_INSTALL
fi

echo -n "looking for ant "
if hash ant 2>/dev/null; then
    ant=$(which ant)
    echo "... found at $ant."
    OPENCV_INSTALL=${install_dir}/opencv
    if [ -d ${OPENCV_INSTALL} ]; then
        echo "${OPENCV_INSTALL} exists.  If you want to build OpenCV, remove this directory and call this script again."
    else
        ${DIR}/opencv/install-opencv.sh $OPENCV_INSTALL
    fi
else
    echo "... not found.  Skipping OpenCV compilation."
fi

${DIR}/util/createInfoFiles/install-createInfoFiles.sh ${install_dir}
