#!/bin/bash

# This script is run from maven.  See the exec-maven-plugin block in the pom.xml file.

# these should be consistent with the root-level mkPackage.bash script
package=Terrasaur
srcFile="../java/terrasaur/utils/AppVersion.java"

cd $(dirname $0)

date=$(date -u +"%Y-%b-%d %H:%M:%S %Z")

rev=$(git rev-parse --verify --short HEAD)
if [ $? -gt 0 ]; then
    lastCommit=$(date -u +"%y.%m.%d")
    rev="UNVERSIONED"
else
    lastCommit=$(git log -1 --format=%cd --date=format:%y.%m.%d)
    rev=$(git rev-parse --verify --short HEAD)

    if [[ $(git diff --stat) != '' ]]; then
        if [[ $(git status -s --untracked=no | grep -v pom.xml | grep -v pom.bak | grep -v .m2 | grep -v $srcFile) != '' ]]; then
            rev=${rev}M
        fi
    fi
fi

mkdir -p $(dirname $srcFile)

touch $srcFile

    cat <<EOF > $srcFile

package terrasaur.utils;

public class AppVersion {
    public final static String lastCommit = "$lastCommit";
    // an M at the end of gitRevision means this was built from a "dirty" git repository
    public final static String gitRevision = "$rev";
    public final static String applicationName = "$package";
    public final static String dateString = "$date";

	private AppVersion() {}

    /**
     * $package version $lastCommit-$rev built $date
     */
    public static String getFullString() {
      return String.format("%s version %s-%s built %s", applicationName, lastCommit, gitRevision, dateString);
    }

    /**
     * $package version $lastCommit-$rev
     */
    public static String getVersionString() {
      return String.format("%s version %s-%s", applicationName, lastCommit, gitRevision);
    }
}

EOF