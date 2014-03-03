package emlauncher;

import hudson.EnvVars;
import hudson.Extension;
import hudson.Launcher;
import hudson.ProxyConfiguration;
import hudson.model.*;
import hudson.model.AbstractBuild;
import hudson.scm.ChangeLogSet;
import hudson.scm.ChangeLogSet.Entry;
import hudson.tasks.*;
import hudson.util.CopyOnWriteList;
import hudson.util.RunList;
import hudson.util.Secret;
import org.apache.commons.collections.Predicate;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import hudson.model.Hudson;

import java.util.*;

import net.sf.json.JSON;
import net.sf.json.JSONObject;
import org.apache.commons.collections.CollectionUtils;
import org.kohsuke.stapler.StaplerRequest;

public class TestflightRecorder extends Recorder {
    private String hostTokenPairName;

    public String getHostTokenPairName() {
        return this.hostTokenPairName;
    }
    
    private String apiHost;
    
    /**
		 * @return the apiHost
		 */
		public String getApiHost() {
			return apiHost;
		}

		private Secret apiToken;

    @Deprecated
    public Secret getApiToken() {
        return this.apiToken;
    }
    
    private boolean sslEnable;
    public boolean getSslEnable() {
    	return sslEnable;
    }

    private Boolean notifyTeam;

    public Boolean getNotifyTeam() {
        return this.notifyTeam;
    }

    private String title;

    /**
		 * @return the title
		 */
		public String getTitle() {
			return title;
		}
		
    private String description;

    /**
		 * @return the description
		 */
		public String getDescription() {
			return description;
		}

		private String tags;

		/**
		 * @return the tags
		 */
		public String getTags() {
			return tags;
		}

    private boolean appendChangelog;

    public boolean getAppendChangelog() {
        return this.appendChangelog;
    }

    /**
     * Comma- or space-separated list of patterns of files/directories to be archived.
     * The variable hasn't been renamed yet for compatibility reasons
     */
    private String filePath;

    public String getFilePath() {
        return this.filePath;
    }

    private String dsymPath;

    public String getDsymPath() {
        return this.dsymPath;
    }

    private String proxyHost;

    @Deprecated
    public String getProxyHost() {
        return proxyHost;
    }

    private String proxyUser;

    @Deprecated
    public String getProxyUser() {
        return proxyUser;
    }

    private String proxyPass;

    @Deprecated
    public String getProxyPass() {
        return proxyPass;
    }

    private int proxyPort;

    @Deprecated
    public int getProxyPort() {
        return proxyPort;
    }

    private Boolean debug;

    public Boolean getDebug() {
        return this.debug;
    }

    private TestflightTeam [] additionalTeams;
    
    public TestflightTeam [] getAdditionalTeams() {
        return this.additionalTeams;
    }
    
    @DataBoundConstructor
    public TestflightRecorder(String hostTokenPairName, String apiHost, Secret apiToken, boolean sslEnable, Boolean notifyTeam, String title, String description, String tags, Boolean appendChangelog, String filePath, String dsymPath, String proxyHost, String proxyUser, String proxyPass, int proxyPort, Boolean debug, TestflightTeam [] additionalTeams) {
        this.hostTokenPairName = hostTokenPairName;
        this.apiHost = apiHost;
        this.apiToken = apiToken;
        this.sslEnable = sslEnable;
        this.notifyTeam = notifyTeam;
        this.title = title;
        this.description = description;
        this.tags = tags;
        this.appendChangelog = appendChangelog;
        this.filePath = filePath;
        this.dsymPath = dsymPath;
        this.proxyHost = proxyHost;
        this.proxyUser = proxyUser;
        this.proxyPass = proxyPass;
        this.proxyPort = proxyPort;
        this.debug = debug;
        this.additionalTeams = additionalTeams;
    }

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }

    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.NONE;
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, final BuildListener listener) {
        if (build.getResult().isWorseOrEqualTo(Result.FAILURE))
            return false;

        listener.getLogger().println(Messages.TestflightRecorder_InfoUploading());

        try {
            EnvVars vars = build.getEnvironment(listener);

            String workspace = vars.expand("$WORKSPACE");

            List<TestflightUploader.UploadRequest> urList = new ArrayList<TestflightUploader.UploadRequest>();

            for(TestflightTeam team : createDefaultPlusAdditionalTeams()) {
                try {
                    TestflightUploader.UploadRequest ur = createPartialUploadRequest(team, vars, build);
                    urList.add(ur);
                } catch (MisconfiguredJobException mje) {
                    listener.getLogger().println(mje.getConfigurationMessage());
                    return false;
                }
            }

            for(TestflightUploader.UploadRequest ur : urList) {
                TestflightRemoteRecorder remoteRecorder = new TestflightRemoteRecorder(workspace, ur, listener);
    
                final List<Map> parsedMaps;
    
                try {
                    Object result = launcher.getChannel().call(remoteRecorder);
                    parsedMaps = (List<Map>) result;
                } catch (UploadException ue) {
                    listener.getLogger().println(Messages.TestflightRecorder_IncorrectResponseCode(ue.getStatusCode()));
                    listener.getLogger().println(ue.getResponseBody());
                    return false;
                }
    
                if (parsedMaps.size() == 0) {
                    listener.getLogger().println(Messages.TestflightRecorder_NoUploadedFile(ur.filePaths));
                    return false;
                }
                for (Map parsedMap: parsedMaps) {
                    addTestflightLinks(build, listener, parsedMap);
                }
            }
        } catch (Throwable e) {
            listener.getLogger().println(e);
            e.printStackTrace(listener.getLogger());
            return false;
        }

        return true;
    }

    private List<TestflightTeam> createDefaultPlusAdditionalTeams() {
        List<TestflightTeam> allTeams = new ArrayList<TestflightTeam>();
        // first team is default
        allTeams.add(new TestflightTeam(getHostTokenPairName(), getFilePath(), getDsymPath()));
        if(additionalTeams != null) {
            allTeams.addAll(Arrays.asList(additionalTeams));
        }
        return allTeams;
    }

    private void addTestflightLinks(AbstractBuild<?, ?> build, BuildListener listener, Map parsedMap) {
        TestflightBuildAction installAction = new TestflightBuildAction();
        String installUrl = (String) parsedMap.get("package_url");
        installAction.displayName = Messages.TestflightRecorder_InstallLinkText();
        installAction.iconFileName = "package.gif";
        installAction.urlName = installUrl;
        build.addAction(installAction);
        listener.getLogger().println(Messages.TestflightRecorder_InfoInstallLink(installUrl));

        TestflightBuildAction configureAction = new TestflightBuildAction();
        String configUrl = (String) parsedMap.get("application_url");
        configureAction.displayName = Messages.TestflightRecorder_ConfigurationLinkText();
        configureAction.iconFileName = "gear2.gif";
        configureAction.urlName = configUrl;
        build.addAction(configureAction);
        listener.getLogger().println(Messages.TestflightRecorder_InfoConfigurationLink(configUrl));

        build.addAction(new EnvAction());

        // Add info about the selected build into the environment
        EnvAction envData = build.getAction(EnvAction.class);
        if (envData != null) {
            envData.add("EMLAUNCHER_INSTALL_URL", installUrl);
            envData.add("EMLAUNCHER_CONFIG_URL", configUrl);
        }
    }

    private TestflightUploader.UploadRequest createPartialUploadRequest(TestflightTeam team, EnvVars vars, AbstractBuild<?, ?> build) {
        TestflightUploader.UploadRequest ur = new TestflightUploader.UploadRequest();
        HostTokenPair hostTokenPair = getHostTokenPair(team.getHostTokenPairName());
        ur.filePaths = vars.expand(StringUtils.trim(team.getFilePath()));
        ur.dsymPath = vars.expand(StringUtils.trim(team.getDsymPath()));
        ur.apiHost = vars.expand(hostTokenPair.getApiHost());
        ur.apiToken = vars.expand(Secret.toString(hostTokenPair.getApiToken()));
        ur.sslEnable = hostTokenPair.getSslEnable();
        ur.title = vars.expand(title);
        ur.description = vars.expand(description);
        ur.tags = vars.expand(tags);
        ur.notifyTeam = notifyTeam;
        ProxyConfiguration proxy = getProxy();
        ur.proxyHost = proxy.name;
        ur.proxyPass = proxy.getPassword();
        ur.proxyPort = proxy.port;
        ur.proxyUser = proxy.getUserName();
        ur.debug = debug;
        return ur;
    }

    private ProxyConfiguration getProxy() {
        ProxyConfiguration proxy;
        if (Hudson.getInstance() != null && Hudson.getInstance().proxy != null) {
            proxy = Hudson.getInstance().proxy;
        } else if (proxyHost != null && proxyPort > 0) {
            // backward compatibility for pre-1.3.7 configurations
            proxy = new ProxyConfiguration(proxyHost, proxyPort, proxyUser, proxyPass);
        } else {
            proxy = new ProxyConfiguration("", 0, "", "");
        }
        return proxy;
    }

    // Append the changelog if we should and can
    private String createBuildNotes(String buildNotes, final ChangeLogSet<?> changeSet) {
        if (appendChangelog) {
            StringBuilder stringBuilder = new StringBuilder();

            // Show the build notes first
            stringBuilder.append(buildNotes);

            // Then append the changelog
            stringBuilder.append("\n\n")
                    .append(changeSet.isEmptySet() ? Messages.TestflightRecorder_EmptyChangeSet() : Messages.TestflightRecorder_Changelog())
                    .append("\n");

            int entryNumber = 1;

            for (Entry entry : changeSet) {
                stringBuilder.append("\n").append(entryNumber).append(". ");
                stringBuilder.append(entry.getMsg()).append(" \u2014 ").append(entry.getAuthor());

                entryNumber++;
            }
            buildNotes = stringBuilder.toString();
        }
        return buildNotes;
    }

    @Override
    public Collection<? extends Action> getProjectActions(AbstractProject<?, ?> project) {
        ArrayList<TestflightBuildAction> actions = new ArrayList<TestflightBuildAction>();
        RunList<? extends AbstractBuild<?, ?>> builds = project.getBuilds();

        Collection predicated = CollectionUtils.select(builds, new Predicate() {
            public boolean evaluate(Object o) {
                Result result = ((AbstractBuild<?, ?>) o).getResult();
                if (result == null) return false; // currently running builds can have a null result
                return result.isBetterOrEqualTo(Result.SUCCESS);
            }
        });

        ArrayList<AbstractBuild<?, ?>> filteredList = new ArrayList<AbstractBuild<?, ?>>(predicated);


        Collections.reverse(filteredList);
        for (AbstractBuild<?, ?> build : filteredList) {
            List<TestflightBuildAction> testflightActions = build.getActions(TestflightBuildAction.class);
            if (testflightActions != null && testflightActions.size() > 0) {
                for (TestflightBuildAction action : testflightActions) {
                    actions.add(new TestflightBuildAction(action));
                }
                break;
            }
        }

        return actions;
    }

    private HostTokenPair getHostTokenPair(String hostTokenPairName) {
        for (HostTokenPair hostTokenPair : getDescriptor().getHostTokenPairs()) {
            if (hostTokenPair.getHostTokenPairName().equals(hostTokenPairName))
                return hostTokenPair;
        }

        if (getApiToken() != null && getApiHost() != null)
            return new HostTokenPair("", getApiHost(), getApiToken(), getSslEnable());

        String hostTokenPairNameForMessage = hostTokenPairName != null ? hostTokenPairName : "(null)";
        throw new MisconfiguredJobException(Messages._TestflightRecorder_HostTokenPairNotFound(hostTokenPairNameForMessage));
    }

    @Extension // This indicates to Jenkins that this is an implementation of an extension point.
    public static final class DescriptorImpl extends BuildStepDescriptor<Publisher> {
        private final CopyOnWriteList<HostTokenPair> hostTokenPairs = new CopyOnWriteList<HostTokenPair>();

        public DescriptorImpl() {
            super(TestflightRecorder.class);
            load();
        }

        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            // Indicates that this builder can be used with all kinds of project types
            return true;
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject json) throws FormException {
        		if(((JSON)(json.get("hostTokenPair"))).isArray()){
          		hostTokenPairs.replaceBy(req.bindJSONToList(HostTokenPair.class, json.getJSONArray("hostTokenPair")));
            } else {
          		hostTokenPairs.replaceBy(req.bindJSONToList(HostTokenPair.class, json.getJSONObject("hostTokenPair")));
            }
            
            save();
            return true;
        }

        /**
         * This human readable name is used in the configuration screen.
         */
        public String getDisplayName() {
            return Messages.TestflightRecorder_UploadLinkText();
        }

        public Iterable<HostTokenPair> getHostTokenPairs() {
            return hostTokenPairs;
        }
    }

    private static class EnvAction implements EnvironmentContributingAction {
        private transient Map<String, String> data = new HashMap<String, String>();

        private void add(String key, String value) {
            if (data == null) return;
            data.put(key, value);
        }

        public void buildEnvVars(AbstractBuild<?, ?> build, EnvVars env) {
            if (data != null) env.putAll(data);
        }

        public String getIconFileName() {
            return null;
        }

        public String getDisplayName() {
            return null;
        }

        public String getUrlName() {
            return null;
        }
    }
}
