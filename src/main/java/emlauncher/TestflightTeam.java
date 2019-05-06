package emlauncher;

import org.kohsuke.stapler.DataBoundConstructor;

public class TestflightTeam {

    private String emlauncherCredentialId;
    @Deprecated
    private String hostTokenPairName;
    private String filePath;
    private String dsymPath;

    @DataBoundConstructor
    public TestflightTeam(String hostTokenPairNameOrCredentialId, String filePath, String dsymPath, boolean isCredential) {
        super();
        if ( isCredential ) {
            this.emlauncherCredentialId = hostTokenPairNameOrCredentialId;
        }
        else {
            this.hostTokenPairName = hostTokenPairNameOrCredentialId;
        }
        this.filePath = filePath;
        this.dsymPath = dsymPath;
    }

    public String getEmlauncherCredentialId() {
        return emlauncherCredentialId;
    }
    @Deprecated
    public String getHostTokenPairName() {
        return hostTokenPairName;
    }

    public void setEmlauncherCredentialId(String emlauncherCredentialId) {
        this.emlauncherCredentialId = emlauncherCredentialId;
    }
    @Deprecated
    public void setHostTokenPairName(String hostTokenPairName) {
        this.hostTokenPairName = hostTokenPairName;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getDsymPath() {
        return dsymPath;
    }

    public void setDsymPath(String dsymPath) {
        this.dsymPath = dsymPath;
    }
}
