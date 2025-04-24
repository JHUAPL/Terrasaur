#!/bin/bash

# Use this script to build VTK on a Mac, Linux, or Windows 64-bit
# system. On all platforms, the JAVA_HOME environmental variable must
# be defined to equal the path where the JDK is installed.
#
# Platform specific notes:
#
# Linux:
#   - Install gcc.
#
# Mac:
#   - Install the latest XCode.
#
# Windows:
#   - You must run this script from Cygwin. Install Cygwin's curl.
#   - Do NOT install Cygwin's CMake. Instead download and install the Windows
#     version from www.cmake.org.
#   - Install Visual Studio 2019 

# This is the tag to download from https://gitlab.kitware.com/hococoder/vtk
BUILD_VERSION=9.2.6
TAG=v${BUILD_VERSION}-apl
VTK_VERSION=vtk-${TAG}

ARCH=$(uname -s)_$(uname -m)
if [ -z $1 ]; then
    echo "Usage: $0 install_dir"
    echo "e.g.   $0 3rd-party/${ARCH}/vtk"
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

pushd $install_dir > /dev/null
install_dir=$(pwd -P)
popd > /dev/null
echo "will install VTK to $install_dir"

set -e

# location of this script
SCRIPTDIR=$(cd $(dirname $0); pwd -P)
echo "SCRIPTDIR IS $SCRIPTDIR"

# current working directory
DIR=$(pwd -P)
echo "DIR is $DIR"

: ${JAVA_HOME:?"Error: JAVA_HOME environmental variable not set"}

BUILD_DIR=buildvtk
rm -rf $BUILD_DIR
mkdir -p $BUILD_DIR
cd $BUILD_DIR

echo git clone --branch ${TAG} https://gitlab.kitware.com/hococoder/vtk.git  ${VTK_VERSION}
git clone --branch ${TAG} https://gitlab.kitware.com/hococoder/vtk.git  ${VTK_VERSION}

platform=$(uname)
nativejar="-natives-windows-amd64"
if [ $platform == "Darwin" ];then
    nativejar="-natives-macosx-universal"
elif [ $platform == "Linux" ];then
    nativejar="-natives-linux-amd64"
fi

jogljars=""
if [ $VTK_VERSION != "vtk-v${BUILD_VERSION}-apl" ]; then 
	for jar in "" $nativejar; do
    		for prefix in jogl-all gluegen-rt; do
			filename=${SCRIPTDIR}/src/${prefix}${jar}.jar
			jogljars="$filename $jogljars"
			if [ ! -e $filename ]; then
	    			pushd ${SCRIPTDIR}/src
	    			wget -N https://jogamp.org/deployment/v2.3.2/jar/$(basename ${filename})
	    			popd
			fi
			ln -s $filename $(basename $filename)
    		done
	done
else
    for jar in "-v2.4.0-rc4"; do
            echo "jar is ${jar}"
            for prefix in jogl-all gluegen-rt; do
                    filename=${SCRIPTDIR}/resources/${prefix}${jar}.jar
                    jogljars="$filename $jogljars"
                    if [ ! -e $filename ]; then
                            echo "${filename} is not in the resources folder, please fix and rerun"
                    fi
                    ln -sfn $filename ${DIR}/${BUILD_DIR}/${prefix}.jar
            done
    done
    for jar in $nativejar; do
            echo "jar is ${jar}"
            for prefix in jogl-all gluegen-rt; do
                    filename=${SCRIPTDIR}/resources/${prefix}${jar}.jar
                    jogljars="$filename $jogljars"
                    if [ ! -e $filename ]; then
                            echo "${filename} is not in the resources folder, please fix and rerun"
                    fi
                    ln -s $filename $(basename $filename)
            done
    done
fi


mkdir -p build
mkdir -p install

# Required when building with modern Java versions - note this still gives a module warning
JAVA_VERSION=17
BUILD_OPTIONS="-DVTK_JAVA_SOURCE_VERSION=${JAVA_VERSION} -DVTK_JAVA_TARGET_VERSION=${JAVA_VERSION} -DCMAKE_BUILD_TYPE=Release"

INSTALL_DIR=$(pwd -P)/install
GLUE_PATH=${DIR}/${BUILD_DIR}/gluegen-rt.jar
JOGL_PATH=${DIR}/${BUILD_DIR}/jogl-all.jar
CXX_FLAGS=
C_FLAGS=

if [ "$(uname)" == "Darwin" ]; then

    echo "building VTK for macOS"

    BUILD_SUFFIX=""
    if [ "$(uname -m)" == "arm64" ]; then
       BUILD_SUFFIX="arm64-${BUILD_VERSION}"
    fi
    echo "The build suffix is ${BUILD_SUFFIX}"
    BUILD_OPTIONS="${BUILD_OPTIONS}
        -DCMAKE_POLICY_VERSION_MINIMUM=3.5
        -Wno-dev
        -DVTK_CUSTOM_LIBRARY_SUFFIX=${BUILD_SUFFIX}
        -DVTK_REQUIRED_OBJCXX_FLAGS:STRING=
        -DVTK_USE_COCOA=ON
        -DJAVA_AWT_INCLUDE_PATH:PATH=${JAVA_HOME}/include
        -DJAVA_AWT_LIBRARY:FILEPATH=${JAVA_HOME}/lib/libjawt.dylib
        -DJAVA_INCLUDE_PATH:PATH=${JAVA_HOME}/include
        -DJAVA_INCLUDE_PATH2:PATH=${JAVA_HOME}/include/darwin
        -DJAVA_JVM_LIBRARY:FILEPATH=${JAVA_HOME}/lib/server/libjvm.dylib
        -DJava_IDLJ_EXECUTABLE:FILEPATH=${JAVA_HOME}/bin/idlj
        -DJava_JARSIGNER_EXECUTABLE:FILEPATH=${JAVA_HOME}/bin/jarsigner
        -DJava_JAR_EXECUTABLE:FILEPATH=${JAVA_HOME}/bin/jar
        -DJava_JAVAC_EXECUTABLE:FILEPATH=${JAVA_HOME}/bin/javac
        -DJava_JAVADOC_EXECUTABLE:FILEPATH=${JAVA_HOME}/bin/javadoc
        -DJava_JAVAH_EXECUTABLE:FILEPATH=${JAVA_HOME}/bin/javah
        -DJava_JAVA_EXECUTABLE:FILEPATH=${JAVA_HOME}/bin/java"

elif [ "$(uname)" == "Linux" ]; then

    echo "building VTK for Linux"
    CXX_FLAGS="-DCMAKE_CXX_FLAGS_RELEASE:String=-O2 -DNDEBUG $CXX_FLAGS"
    C_FLAGS="-DCMAKE_C_FLAGS_RELEASE:String=-O2 -DNDEBUG $CFLAGS"

else
    
    echo "building VTK for Cygwin"
    BUILD_OPTIONS="-GVisual Studio 16 2019"
    INSTALL_DIR=`cygpath -w $INSTALL_DIR`
    GLUE_PATH=`cygpath -w $GLUE_PATH`
    JOGL_PATH=`cygpath -w $JOGL_PATH`

fi

cd build

cmake $BUILD_OPTIONS                           \
    -DCMAKE_INSTALL_PREFIX:PATH=${INSTALL_DIR} \
    -DBUILD_SHARED_LIBS:BOOL=ON                \
    -DBUILD_TESTING:BOOL=OFF                   \
    -DVTK_DEBUG_LEAKS:BOOL=OFF                 \
    -DVTK_WRAP_JAVA:BOOL=ON                    \
    -DVTK_JAVA_JOGL_COMPONENT:BOOL=ON          \
    -DJOGL_GLUE:FILEPATH=${GLUE_PATH}          \
    -DJOGL_LIB:FILEPATH=${JOGL_PATH}           \
    -DVTK_DATA_EXCLUDE_FROM_ALL=ON             \
    -DVTK_WRAP_PYTHON=OFF                      \
    -DVTK_MODULE_ENABLE_VTK_hdf5=NO            \
    ../${VTK_VERSION}

cmake --build . --config Release --target install -j4

BUILD_DIR=$(pwd -P)

cd ../install

if [ -d lib64 ]; then
    ln -sf lib64 lib
fi

for jogljar in $jogljars; do
    for dir in lib lib64; do 
	if [ -d $dir/java ]; then
	    cp -p $jogljar $dir/java
	fi
    done
done

rsync -avP . $install_dir/

echo "installed VTK. You may remove ${BUILD_DIR}."
