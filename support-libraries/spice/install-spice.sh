#!/bin/bash

ARCH=$(uname -s)_$(uname -m)
if [ -z $1 ]; then
    echo "Usage: $0 install_dir"
    echo "e.g.   $0 3rd-party/${ARCH}/spice"
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

SRCDIR=${DIR}/src/${ARCH}

# Install the JNISpice library

if [ ! -e ${SRCDIR}/JNISpice.tar.Z ]; then
    mkdir -p ${SRCDIR}
    pushd ${SRCDIR} >/dev/null
    if [ "$ARCH" == "Darwin_x86_64" ]; then
        curl -RO https://naif.jpl.nasa.gov/pub/naif/misc/JNISpice/MacIntel_OSX_AppleC_Java1.8_64bit/packages/JNISpice.tar.Z
    elif [ "$ARCH" == "Darwin_arm64" ]; then
        if [ -e /project/spice/toolkit/latest/JNISpice.tar.Z ]; then
            # preinstalled on srn-devmac1
            cp -p /project/spice/toolkit/latest/JNISpice.tar.Z .
        else
            echo "NAIF SPICE package not found for $ARCH"
        fi
    elif [ "$ARCH" == "Linux_x86_64" ]; then
        curl -RO -N https://naif.jpl.nasa.gov/pub/naif/misc/JNISpice/PC_Linux_GCC_Java1.8_64bit/packages/JNISpice.tar.Z
    else
        echo "NAIF SPICE package not found for $ARCH"
        exit 0
    fi
    popd >/dev/null
fi

# Get the FORTRAN package

if [ ! -e ${SRCDIR}/toolkit.tar.Z ]; then
    mkdir -p ${SRCDIR}
    pushd ${SRCDIR} >/dev/null
    if [ "$ARCH" == "Darwin_x86_64" ]; then
        curl -RO https://naif.jpl.nasa.gov/pub/naif/toolkit/FORTRAN/MacIntel_OSX_gfortran_64bit/packages/toolkit.tar.Z
    elif [ "$ARCH" == "Darwin_arm64" ]; then
        if [ -e /project/spice/toolkit/latest/toolkit.tar.Z ]; then
            # preinstalled on srn-devmac1
            cp -p /project/spice/toolkit/latest/toolkit.tar.Z .
        else
            echo "NAIF SPICE package not found for $ARCH"
        fi
    elif [ "$ARCH" == "Linux_x86_64" ]; then
        curl -RO -N https://naif.jpl.nasa.gov/pub/naif/toolkit//FORTRAN/PC_Linux_gfortran_64bit/packages/toolkit.tar.Z
    else
        echo "NAIF SPICE package not found for $ARCH"
        exit 0
    fi
    popd >/dev/null
fi

cd $install_dir

if [ -e ${SRCDIR}/toolkit.tar.Z ]; then
    rm -rf toolkit
    tar xf ${SRCDIR}/toolkit.tar.Z
else
    echo "${SRCDIR}/toolkit.tar.Z does not exist"
fi

if [ -e ${SRCDIR}/JNISpice.tar.Z ]; then
    rm -rf JNISpice
    tar xf ${SRCDIR}/JNISpice.tar.Z
    (
        cd JNISpice/src/JNISpice
        jar cfv ${install_dir}/spice.jar spice
    )
else
    echo "${SRCDIR}/JNISpice.tar.Z does not exist"
fi
