#!/bin/bash

ARCH=$(uname -s)_$(uname -m)
if [ -z $1 ]; then
    echo "Usage: $0 install_dir"
    echo "e.g.   $0 arch/${ARCH}/cfitsio"
    exit 0
fi

install_dir=$1

# location of this script
DIR=$(cd $(dirname $0); pwd -P)

SRCDIR=${DIR}

cd ${SRCDIR}

make all SPICE_DIR="${install_dir}/spice/JNISpice" > make-all.txt 2>&1
if test $? -ne 0; then
  echo "Problem building createInfoFiles; see file ${SRCDIR}/make-all.txt"
  exit 1
fi
make clean > /dev/null 2>&1
rm -f make-all.txt

mkdir -p ${install_dir}/bin

mv createInfoFiles ${install_dir}/bin
if test $? -ne 0; then
  echo "Problem installing createInfoFiles"
  exit 1
fi

mv computePointing ${install_dir}/bin
if test $? -ne 0; then
  echo "Problem installing computePointing"
  exit 1
fi

make distclean > /dev/null 2>&1
