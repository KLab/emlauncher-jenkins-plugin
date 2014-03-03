package emlauncher;

import org.kohsuke.stapler.DataBoundConstructor;

public class TestflightTeam {

    private String hostTokenPairName;
    private String filePath;
    private String dsymPath;

    @DataBoundConstructor
    public TestflightTeam(String hostTokenPairName, String filePath, String dsymPath) {
        super();
        this.hostTokenPairName = hostTokenPairName;
        this.filePath = filePath;
        this.dsymPath = dsymPath;
    }

    public String getHostTokenPairName() {
        return hostTokenPairName;
    }

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
