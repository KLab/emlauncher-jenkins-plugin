package emlauncher;

import hudson.util.Secret;
import org.kohsuke.stapler.DataBoundConstructor;

public class HostTokenPair {
    private String hostTokenPairName;
    private String apiHost;
    private Secret apiToken;
    private boolean sslEnable;

    public HostTokenPair() {
    }

    @DataBoundConstructor
    public HostTokenPair(String hostTokenPairName, String apiHost, Secret apiToken, boolean sslEnable) {
        this.hostTokenPairName = hostTokenPairName;
        this.apiHost = apiHost;
        this.apiToken = apiToken;
        this.sslEnable = sslEnable;
    }

    public String getHostTokenPairName() {
        return hostTokenPairName;
    }

    public void setHostTokenPairName(String hostTokenPairName) {
        this.hostTokenPairName = hostTokenPairName;
    }
    
    /**
		 * @return the apiHost
		 */
		public String getApiHost() {
			return apiHost;
		}

		/**
		 * @param apiHost the apiHost to set
		 */
		public void setApiHost(String apiHost) {
			this.apiHost = apiHost;
		}

		public Secret getApiToken() {
        return apiToken;
    }

    public void setApiToken(Secret apiToken) {
        this.apiToken = apiToken;
    }

		/**
		 * @return the ssl
		 */
		public boolean getSslEnable() {
			return sslEnable;
		}

		/**
		 * @param ssl the ssl to set
		 */
		public void setSslEnable(boolean sslEnable) {
			this.sslEnable = sslEnable;
		}

}
