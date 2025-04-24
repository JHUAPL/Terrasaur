#!/bin/bash

# This program generates a file containing documentation of all
# programs in this software system by running all programs with no
# arguments and piping the usage output to a file.

if [ -z "$1" ]; then
    echo "usage: $0 scriptDir"
    echo "e.g.   $0 ../Terrasaur-2024.12.21/scripts"
    exit 0
fi

rootDir=$(
    cd "$(dirname "$0")"
    pwd -P
)
scriptDir=$1

docfile=${rootDir}/tools/shortDescriptions.rst
indexfile=${rootDir}/tools/index.rst
rm -f "$docfile" "$indexfile"

cat >>"$indexfile" <<EOF
===========
Tools Index
===========

EOF

cat >>"$docfile" <<EOF
==================
Terrasaur Programs
==================

EOF

programsToSkip=()

for f in $(find "${scriptDir}" -type f -maxdepth 1 | sort -f); do
    f=$(basename "$f")
    flink=$(echo "$f" | awk '{print tolower($0)}' | sed -e 's/[^[:alnum:]|-]/\-/g')

    skip=0
    # Ignore programs that begin with lowercase letter or the string "Immutable"
    if [[ "$f" =~ ^([a-z].*|Immutable.*) ]]; then
        skip=1
    fi

    for program in "${programsToSkip[@]}"; do
        if [[ "$f" == "$program" ]]; then
            skip=1
            break
        fi
    done

    if [ $skip -eq 1 ]; then
        echo "Skipping $f"
        continue
    fi

    echo "Generating documentation for $f"

    shortDescription=$("${scriptDir}"/"$f" -shortDescription)

    mkdir -p toolDescriptions
    "${scriptDir}"/"$f" >toolDescriptions/"${f}".txt

    if [ -e tools/"${f}".rst ]; then
        cat >>"$indexfile" <<EOF
:doc:\`$f\`: $shortDescription

EOF
    else
        cat >>"$indexfile" <<EOF
\`$f <shortDescriptions.html#${flink}>\`__: $shortDescription

EOF
        cat >>"$docfile" <<EOF
$f
$(printf '=%.0s' {1..100})

.. include:: ../toolDescriptions/${f}.txt
    :literal:


EOF
    fi
done