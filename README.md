# Terrasaur

Terrasaur is a suite of programs written in Java and C++. These standalone
command line analysis and shape model manipulation programs complement the
[Small Body Mapping Tool](https://sbmt.jhuapl.edu/) and create data products that
are well-suited to being visualized in the SBMT GUI. Among other functions,
these programs facilitate the building of digital terrain models (DTMs) of
small bodies, permit assessing the quality of these DTMs, and construct a
broad suite of DTM products that characterize the geophysical and surface
properties of small bodies.

## Quick Start

### Prerequisites

The Terrasaur package requires Java 21 or later.  Some freely available versions are

* [Amazon Corretto](https://aws.amazon.com/corretto/)
* [Azul Zulu](https://www.azul.com/downloads/?package=jdk)
* [Eclipse Temurin](https://adoptium.net/)
* [OpenJDK](https://jdk.java.net/).  Most Linux distributions and HomeBrew have OpenJDK packages.

### Download the latest release

Precompiled release packages can be found on [GitHub](https://github.com/JHUAPL/Terrasaur/releases)

### Build the package

If you don't want to use the prebuilt package,  check out the code:

    git clone https://github.com/JHUAPL/Terrasaur.git

The 3rd party executables and libraries are assumed to exist in
`3rd-party/$(uname -s)_$(uname -m)` (e.g. `3rd-party/Darwin_x86_64` on an
Intel macOS machine).  The script to build the 3rd party products is in the
`support-libraries` directory.  

    cd support-libraries
    ./buildAll.bash ../3rd-party/$(uname -s)_$(uname -m)

You can instead use the precompiled support libraries from a release package to save a lot of time.  See the `support-libraries` [README](./support-libraries/README.md).

Maven must be installed to build the software.  Once the `3rd-party`
directory has been built, compile the Terrasaur package using the
`mkPackage.bash` script:

    ./mkPackage.bash

This will create executable and source packages in the dist directory,
named SBCLT-YYYY.MM.DD.tar.gz and SBCLT-YYYY.MM.DD-src.tar.gz

[Sphinx](https://www.sphinx-doc.org/en/master/) with the
[PD](https://sphinx-themes.org/sample-sites/sphinx-theme-pd/)
theme is used to create the documentation in the `doc/` folder.

### Install the executable package

    cd (your destination directory)
    tar xfz (path to command-line-tools)/dist/Terrasaur-YYYY.MM.DD.tar.gz

The `scripts/` directory contains all the applications in the
package.  Running without any arguments will display a usage message.

The `doc/` directory contains documentation including a javadoc
directory.
