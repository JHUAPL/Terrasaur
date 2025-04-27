#!/bin/bash

# Configure these for the package
packageName=Terrasaur
appPackage="terrasaur"
scriptPath=$(
    cd "$(dirname "$0")" || exit 1
    pwd -P
)
srcPath="${scriptPath}/src/main/java"
srcFile="${srcPath}/${appPackage}/utils/AppVersion.java"
appSrcDir="${appPackage}/apps"

function build_jar() {
    rev=$1

    cd "${scriptPath}" || exit 1

    # store the version number in pom.xml
    cp -p pom.xml pom.bak
    sed "s,<version>0.0.1-SNAPSHOT</version>,<version>$rev</version>,g" pom.bak >pom.xml

    # install dependencies to the local maven repository
    if [ -d dependency ]; then
        cd dependency || exit 1

        for pom in *.pom; do
            base=$(basename "$pom" .pom)
            jar="${base}.jar"

            # Extract groupId, artifactId, and version from the POM file
            groupId=$(grep -m1 '<groupId>' "$pom" | sed -E 's|.*<groupId>(.*)</groupId>.*|\1|' | tr '.' '/')
            artifactId=$(grep -m1 '<artifactId>' "$pom" | sed -E 's|.*<artifactId>(.*)</artifactId>.*|\1|')
            version=$(grep -m1 '<version>' "$pom" | sed -E 's|.*<version>(.*)</version>.*|\1|')

            if [ -z "$groupId" ] || [ -z "$artifactId" ] || [ -z "$version" ]; then
                echo "Skipping ${base}: Unable to extract Maven coordinates."
                continue
            fi

            # Construct the expected JAR and POM paths in the local Maven repository
            repo_path="$HOME/.m2/repository/$groupId/$artifactId/$version"
            jar_path="$repo_path/$artifactId-$version.jar"
            pom_path="$repo_path/$artifactId-$version.pom"

            if [ -f "$jar_path" ] || [ -f "$pom_path" ]; then
                echo "${base} is already installed in local repository."
                continue
            fi

            # Install the artifact
            if [ -e "$jar" ]; then
                mvn -q install:install-file -Dfile="$jar" -DpomFile="$pom"
            else
                mvn -q install:install-file -Dfile="$pom" -DpomFile="$pom"
            fi

            echo "Installed ${base} in local repository"
        done

        cd ..
    else
        # install the third party jar files
        mvn -q install:install-file -Dfile=3rd-party/"${ARCH}"/spice/spice.jar -DgroupId=gov.nasa.jpl.naif -DartifactId=spice -Dversion=N0067 -Dpackaging=jar
        mvn -q install:install-file -Dfile=3rd-party/"${ARCH}"/vtk/lib/java/vtk.jar -DgroupId=com.kitware -DartifactId=vtk-apl -Dversion=9.2.6-apl -Dpackaging=jar

        # Deploy to surfshop

#        echo mvn deploy:deploy-file -Dfile=3rd-party/"${ARCH}"/spice/spice.jar -DgroupId=gov.nasa.jpl.naif -DartifactId=spice -Dversion=N0067 -Dpackaging=jar -DrepositoryId=third-party -Durl=http://surfshop:8082/artifactory/libs-3rdparty-local/
#        echo mvn deploy:deploy-file -Dfile=3rd-party/"${ARCH}"/vtk/lib/java/vtk.jar -DgroupId=com.kitware -DartifactId=vtk-apl -Dversion=9.2.6-apl -Dpackaging=jar -DrepositoryId=third-party -Durl=http://surfshop:8082/artifactory/libs-3rdparty-local/

    fi

    # ARCH needed for maven-surefire-plugin
    export ARCH
    mvn clean install

    # restore the old pom file
    mv pom.bak pom.xml

    # install the maven products
    rsync -a "${scriptPath}"/target/${packageName}.jar "${libDir}"
    rsync -a "${scriptPath}"/target/${packageName}_lib "${libDir}"

    # shellcheck disable=SC2086
    rsync -a "${scriptPath}"/3rd-party/ ${libDir}

}

function make_scripts() {

    classes=$(jar tf "${scriptPath}"/target/${packageName}.jar | grep $appSrcDir | grep -v '\$' | grep -v "package-info" | grep class)

    for class in $classes; do
        base=$(basename "$class" ".class")
        tool=${scriptDir}/${base}
        path=$(dirname "$class" | sed 's,/,.,g').${base}
        echo "#!/bin/bash" >${tool}
        echo 'script_dir=$(dirname $(which $0))' >>${tool}
        echo 'script_dir=$(cd $script_dir; pwd -P)' >>${tool}
        echo 'root=$(dirname $script_dir)' >>${tool}
        echo 'MEMSIZE=""' >>${tool}
        echo 'ARCH=$(uname -s)_$(uname -m)' >>${tool}
        echo 'export PATH="${root}/lib/${ARCH}/altwg:${root}/lib/${ARCH}/spice/JNISpice/exe:${PATH}"' >>${tool}
        echo 'JAVA_LIBRARY_PATH=""' >>${tool}
        echo 'if [ ! -z $JAVA_HOME ]; then' >>${tool}
        echo '  JAVA_LIBRARY_PATH="${JAVA_HOME}/lib:${JAVA_LIBRARY_PATH}"' >>${tool}
        echo 'fi' >>${tool}
        echo 'JAVA_LIBRARY_PATH="${root}/lib/${ARCH}/spice/JNISpice/lib:${JAVA_LIBRARY_PATH}"' >>${tool}
        echo 'JAVA_LIBRARY_PATH="${root}/lib/${ARCH}/vtk/lib:${JAVA_LIBRARY_PATH}"' >>${tool}
        echo 'JAVA_LIBRARY_PATH="${root}/lib/${ARCH}/vtk/lib/java/vtk-$(uname -s)-$(uname -m):${JAVA_LIBRARY_PATH}"' >>${tool}
        echo 'if [ "$(uname -s)" == "Darwin" ]; then' >>${tool}
        echo '    MEMSIZE=$(sysctl hw.memsize | awk '\''{print int($2/1024)}'\'')' >>${tool}
        echo '    export DYLD_LIBRARY_PATH=$JAVA_LIBRARY_PATH' >>${tool}
        echo 'elif [ "$(uname -s)" == "Linux" ]; then' >>${tool}
        echo '    MEMSIZE=$(grep MemTotal /proc/meminfo | awk '\''{print $2}'\'')' >>${tool}
        echo '    export LD_LIBRARY_PATH=$JAVA_LIBRARY_PATH' >>${tool}
        echo 'fi' >>${tool}
        echo 'java=$(which java)' >>${tool}
        echo 'if [ -z "$java" ]; then' >>${tool}
        echo '    echo "Java executable not found in your PATH"' >>${tool}
        echo '    exit 1' >>${tool}
        echo 'fi' >>${tool}
        echo 'fullVersion=$($java -version 2>&1 | head -1 |awk -F\" '\''{print $2}'\'')' >>${tool}
        echo 'version=$(echo $fullVersion | awk -F\. '\''{print $1}'\'')' >>${tool}
        echo 'if [ "$version" -lt "'$REQUIRED_JAVA_VERSION'" ];then' >>${tool}
        echo '    echo "minimum Java version required is '$REQUIRED_JAVA_VERSION'.  Version found is $fullVersion."' >>${tool}
        echo '    exit 1' >>${tool}
        echo 'fi' >>${tool}
        echo '$java' "-Djava.library.path=\${JAVA_LIBRARY_PATH} -Xmx\${MEMSIZE}K -cp \${root}/lib/*:\${root}/lib/${packageName}_lib/* $path \"\$@\"" >>${tool}

        chmod +x ${tool}
    done

}

function make_doc {
    cwd=$(pwd)

    # build javadoc
    javadoc -quiet -Xdoclint:none -cp ${libDir}/*:${libDir}/${packageName}_lib/* -d ${docDir}/javadoc -sourcepath ${srcPath} -subpackages ${appPackage} -overview ${docDir}/src/overview.html
    /bin/rm -fr "${docDir}"/src

    # sphinx
    cd ${scriptPath}/doc || exit 1

    python3 -m venv "${scriptPath}"/venv
    source "${scriptPath}"/venv/bin/activate
    site_package_path=$(python3 -c 'import sysconfig; print(sysconfig.get_paths()["purelib"])')
    if [ -z "$PYTHONPATH" ]; then
        export PYTHONPATH=$site_package_path
    else
        export PYTHONPATH=$site_package_path:$PYTHONPATH
    fi

    python3 -m pip --default-timeout=1000 install -U sphinx
    python3 -m pip --default-timeout=1000 install sphinx-theme-pd

    ./make_doc.bash ${scriptDir}
    sphinx-build -b html . _build
    rsync -a _build/ ${docDir}
    /bin/rm -fr toolDescriptions tools/shortDescriptions.rst tools/index.rst _build

    cd "$cwd" || exit 1
}

### Don't need to modify anything below this line

# update maven-compiler-plugin block in pom if this version changes
REQUIRED_JAVA_VERSION=21

java=$(which java)
if [ -z $java ]; then
    echo "Java executable not found in your PATH"
    exit 0
fi
fullVersion=$(java -version 2>&1 | head -1 | awk -F\" '{print $2}')
version=$(echo $fullVersion | awk -F\. '{print $1}')
if [ "$version" -lt "$REQUIRED_JAVA_VERSION" ]; then
    echo "minimum Java version required is $REQUIRED_JAVA_VERSION.  Version found is $fullVersion."
    exit 0
fi

if [ -d .git ]; then

    date=$(git log -1 --format=%cd --date=format:%Y.%m.%d)
    rev=$(git rev-parse --verify --short HEAD)

    if [[ $(git diff --stat) != '' ]]; then
        echo 'WARNING: the following files have not been checked in:'
        git status --short
        echo "waiting for 5 seconds ..."
        sleep 5
        rev=${rev}M
    fi

else
    date=$(date -u +"%Y.%m.%d")
    rev="UNVERSIONED"
fi

ARCH=$(uname -s)_$(uname -m)
pkgBase=${packageName}-${date}

scriptDir=${pkgBase}/scripts
scriptDir=$(
    mkdir -p "${scriptDir}"
    cd "${scriptDir}" || exit 1
    pwd -P
)
libDir=${pkgBase}/lib
libDir=$(
    mkdir -p "${libDir}"
    cd "${libDir}" || exit 1
    pwd -P
)
docDir=${pkgBase}/doc
docDir=$(
    mkdir -p "${docDir}"
    cd "${docDir}" || exit 1
    pwd -P
)

if [ ! -d ${scriptPath}/3rd-party/${ARCH} ]; then
    echo "third party libraries should be installed in 3rd-party/${ARCH}.  Please install them and run this script again."
    exit 0
fi

# Build the jar file
build_jar ${rev}

# create the executable scripts
make_scripts

# create documentation
make_doc

# Create distribution files for each architecture
mkdir -p dist
for arch in 3rd-party/*; do
    this_arch=$(basename "$arch")
    tarfile=./dist/${pkgBase}-${rev}_${this_arch}.tar
    tar --exclude='lib' -cf "$tarfile" "${pkgBase}"
    tar rf "$tarfile" "${pkgBase}"/lib/"${this_arch}" "${pkgBase}"/lib/${packageName}.jar "${pkgBase}"/lib/${packageName}_lib
    gzip "${tarfile}"
    echo "Created ${tarfile}.gz"
done

mvn -q -Dmdep.copyPom=true dependency:copy-dependencies
rsync -a README.md CHANGELOG.md mkPackage.bash pom.xml doc src target/dependency "${pkgBase}"-src/
tar cfz ./dist/"${pkgBase}"-${rev}-src.tar.gz ./"${pkgBase}"-src
echo -e "\nCreated ./dist/${pkgBase}-${rev}-src.tar.gz"
/bin/rm -fr ./"${pkgBase}" ./"${pkgBase}"-src

if [ -d .git ]; then
    git restore "$srcFile"
fi
