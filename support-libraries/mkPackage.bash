#!/bin/bash

root=SBCLT_support-$(date -u +"%Y.%m.%d")
mkdir -p dist/${root}
rsync -a README.md altwg buildAll.bash spice vtk dist/${root}
cd dist
tar cfz ${root}.tar.gz ./${root}
/bin/rm -fr ./${root}

echo -e "Created ./dist/${root}.tar.gz"
