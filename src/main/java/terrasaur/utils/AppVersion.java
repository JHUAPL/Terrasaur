
package terrasaur.utils;

public class AppVersion {
    public final static String lastCommit = "24.12.10";
    // an M at the end of gitRevision means this was built from a "dirty" git repository
    public final static String gitRevision = "4c87dac";
    public final static String applicationName = "Terrasaur";
    public final static String dateString = "2024-Dec-20 18:07:16 UTC";

	private AppVersion() {}

    /**
     * Terrasaur version 24.12.10-4c87dacM built 2024-Dec-20 18:07:16 UTC
     */
    public static String getFullString() {
      return String.format("%s version %s-%s built %s", applicationName, lastCommit, gitRevision, dateString);
    }

    /**
     * Terrasaur version 24.12.10-4c87dacM
     */
    public static String getVersionString() {
      return String.format("%s version %s-%s", applicationName, lastCommit, gitRevision);
    }
}

