#!/bin/bash

if [ -z $1 ]; then
    ARCH=$(uname -s)_$(uname -m)
    echo "Usage: $0 install_dir"
    echo "e.g.   $0 3rd-party/${ARCH}/opencv"
    exit 0
fi

TAG=4.7.0

install_dir=$1
mkdir -p $install_dir
install_dir=$(cd "$install_dir"; pwd -P)

if [ $? -ne 0 ]; then
   echo "cannot create directory $install_dir"
   exit 1
fi

if [ ! -w $install_dir ]; then
   echo "cannot write to directory $install_dir"
   exit 1
fi
echo "will install OpenCV to $install_dir"

set -e

BUILD_DIR=buildopencv
rm -rf $BUILD_DIR
mkdir -p $BUILD_DIR
cd $BUILD_DIR

curl -L -o opencv-${TAG}.zip https://github.com/opencv/opencv/archive/refs/tags/${TAG}.zip 
curl -L -o opencv_contrib-${TAG}.zip https://github.com/opencv/opencv_contrib/archive/refs/tags/${TAG}.zip 

unzip -q opencv-${TAG}.zip
unzip -q opencv_contrib-${TAG}.zip

mkdir build
cd build

VTK_DIR=$(dirname $install_dir)/vtk/lib/cmake/vtk-9.2
if [ ! -d ${VTK_DIR} ]; then
   unset VTK_DIR
fi

cmake -DCMAKE_BUILD_TYPE=Release -DVTK_DIR=${VTK_DIR} -DCMAKE_INSTALL_PREFIX=$install_dir -DOPENCV_EXTRA_MODULES_PATH=../opencv_contrib-${TAG}/modules -DOPENCV_ENABLE_NONFREE=ON -DBUILD_ZLIB=OFF ../opencv-${TAG}

make -j4
make install
