package emlauncher;

import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import hudson.AbortException;
import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.ProxyConfiguration;
import hudson.model.*;
import hudson.model.AbstractBuild;
import hudson.model.TaskListener;
import hudson.scm.ChangeLogSet;
import hudson.scm.ChangeLogSet.Entry;
import hudson.security.ACL;
import hudson.tasks.*;
import hudson.util.CopyOnWriteList;
import hudson.util.RunList;
import hudson.util.Secret;
import org.apache.commons.collections.Predicate;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import hudson.model.Hudson;
import org.jenkinsci.plugins.tokenmacro.MacroEvaluationException;
import org.jenkinsci.plugins.tokenmacro.TokenMacro;
import org.jenkinsci.Symbol;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import jenkins.tasks.SimpleBuildStep;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.UUID;
import javax.annotation.Nonnull;
import java.io.IOException;

import net.sf.json.JSON;
import net.sf.json.JSONObject;
import org.apache.commons.collections.CollectionUtils;
import org.kohsuke.stapler.StaplerRequest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;

public class TestflightRecorder extends Recorder implements SimpleBuildStep {
    private String emlauncherCredentialId = null;
    @Deprecated
    private String hostTokenPairName = null;

    public String getEmlauncherCredentialId() {
        return this.emlauncherCredentialId;
    }
    @Deprecated
    public String getHostTokenPairName() {
        return this.hostTokenPairName;
    }

    @DataBoundSetter
    public void setEmlauncherCredentialId(String emlauncherCredentialId) {
        this.emlauncherCredentialId = emlauncherCredentialId;
    }
    @Deprecated
    @DataBoundSetter
    public void setHostTokenPairName(String hostTokenPairName) {
	this.hostTokenPairName = hostTokenPairName;
    }
    
    private String apiHost = null;
    
    /**
     * @return the apiHost
     */
    public String getApiHost() {
        return apiHost;
    }

    @DataBoundSetter
    public void setApiHost(String apiHost) {
	this.apiHost = apiHost;
    }

    private Secret apiToken = null;

    @Deprecated
    public Secret getApiToken() {
        return this.apiToken;
    }

    @DataBoundSetter
    public void setApiToken(Secret apiToken) {
	this.apiToken = apiToken;
    }
    
    private boolean sslEnable = false;

    public boolean getSslEnable() {
        return sslEnable;
    }

    @DataBoundSetter
    public void setSslEnable(boolean sslEnable) {
	this.sslEnable = sslEnable;
    }

    private Boolean notifyTeam = false;

    public Boolean getNotifyTeam() {
        return this.notifyTeam;
    }

    @DataBoundSetter
    public void setNotifyTeam(boolean notifyTeam) {
	this.notifyTeam = notifyTeam;
    }

    private String title = null;

    /**
     * @return the title
     */
    public String getTitle() {
        return title;
    }

    @DataBoundSetter
    public void setTitle(String title) {
	this.title = title;
    }
        
    private String description = null;

    /**
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    @DataBoundSetter
    public void setDescription(String description) {
	this.description = description;
    }

    private String tags = null;

    /**
     * @return the tags
     */
    public String getTags() {
        return tags;
    }

    @DataBoundSetter
    public void setTags(String tags) {
	this.tags = tags;
    }

    private boolean appendChangelog = false;

    public boolean getAppendChangelog() {
        return this.appendChangelog;
    }

    @DataBoundSetter
    public void setAppendChangelog(boolean appendChangelog) {
	this.appendChangelog = appendChangelog;
    }

    /**
     * Comma- or space-separated list of patterns of files/directories to be archived.
     * The variable hasn't been renamed yet for compatibility reasons
     */
    private String filePath = null;

    public String getFilePath() {
        return this.filePath;
    }

    @DataBoundSetter
    public void setFilePath(String filePath) {
	this.filePath = filePath;
    }

    private String dsymPath = null;

    public String getDsymPath() {
        return this.dsymPath;
    }

    @DataBoundSetter
    public void setDsymPath(String dsymPath) {
	this.dsymPath = dsymPath;
    }

    private String proxyHost = null;

    @Deprecated
    public String getProxyHost() {
        return proxyHost;
    }

    private String proxyUser = null;

    @Deprecated
    public String getProxyUser() {
        return proxyUser;
    }

    private String proxyPass = null;

    @Deprecated
    public String getProxyPass() {
        return proxyPass;
    }

    private int proxyPort = 0;

    @Deprecated
    public int getProxyPort() {
        return proxyPort;
    }

    private Boolean debug = false;

    public Boolean getDebug() {
        return this.debug;
    }

    @DataBoundSetter
    public void setDebug(boolean debug) {
	this.debug = debug;
    }

    private int timeout = 0;
    
    public int getTimeout() {
        return timeout;
    }

    @DataBoundSetter
    public void setTimeout(int timeout) {
	this.timeout = timeout;
    }

    private TestflightTeam [] additionalTeams = null;
    
    public TestflightTeam [] getAdditionalTeams() {
        return this.additionalTeams;
    }

    @DataBoundSetter
    public void setAdditionalTeams(TestflightTeam [] additionalTeams) {
	this.additionalTeams = additionalTeams;
    }

    @DataBoundConstructor
    public TestflightRecorder(String emlauncherCredentialId) {
        this.emlauncherCredentialId = emlauncherCredentialId;
    }
    
    @Deprecated
    public TestflightRecorder(String emlauncherCredentialId, String apiHost, Secret apiToken, boolean sslEnable, Boolean notifyTeam, String title, String description, String tags, Boolean appendChangelog, String filePath, String dsymPath, String proxyHost, String proxyUser, String proxyPass, int proxyPort, int timeout, Boolean debug, TestflightTeam [] additionalTeams) {
	this(emlauncherCredentialId);
    }

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }

    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.NONE;
    }

    @Override
    public void perform(@Nonnull Run<?, ?> run, @Nonnull FilePath workspace, @Nonnull Launcher launcher, @Nonnull TaskListener listener) throws InterruptedException, IOException {
	_perform(run, workspace, launcher, listener);
    }

    public boolean _perform(Run<?, ?> build, FilePath workspace, Launcher launcher, final TaskListener listener) {
        listener.getLogger().println(Messages.TestflightRecorder_InfoUploading());

        try {
            EnvVars vars = build.getEnvironment(listener);

            List<TestflightUploader.UploadRequest> urList = new ArrayList<TestflightUploader.UploadRequest>();

            for(TestflightTeam team : createDefaultPlusAdditionalTeams()) {
                try {
                    TestflightUploader.UploadRequest ur = createPartialUploadRequest(team, vars, build, listener);
                    urList.add(ur);
                } catch (MisconfiguredJobException mje) {
                    listener.getLogger().println(mje.getConfigurationMessage());
                    return false;
                }
            }

            for(TestflightUploader.UploadRequest ur : urList) {
                TestflightRemoteRecorder remoteRecorder = new TestflightRemoteRecorder(workspace.absolutize().getRemote(), ur, listener);
    
                final List<Map> parsedMaps;
    
                try {
                    String result = launcher.getChannel().call(remoteRecorder);
		    ObjectMapper mapper = new ObjectMapper();
                    parsedMaps = mapper.readValue(result, new TypeReference<List<Map>>() {});
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
        if ( StringUtils.isNotEmpty(getHostTokenPairName()) ) {
            // for backward compatibility
            allTeams.add(new TestflightTeam(getHostTokenPairName(), getFilePath(), getDsymPath(), false));
        }
        else {
            allTeams.add(new TestflightTeam(getEmlauncherCredentialId(), getFilePath(), getDsymPath(), true));
        }
        if(additionalTeams != null) {
            allTeams.addAll(Arrays.asList(additionalTeams));
        }
        return allTeams;
    }

    private void addTestflightLinks(Run<?, ?> build, TaskListener listener, Map parsedMap) {
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

    private TestflightUploader.UploadRequest createPartialUploadRequest(TestflightTeam team, EnvVars vars, Run<?, ?> build, final TaskListener listener) {
        TestflightUploader.UploadRequest ur = new TestflightUploader.UploadRequest();
        if ( StringUtils.isNotEmpty(team.getHostTokenPairName()) ) {
            // for backward compatibility
            HostTokenPair hostTokenPair = getHostTokenPair(team.getHostTokenPairName());
            if ( hostTokenPair == null ) {
                throw new MisconfiguredJobException(Messages._TestflightRecorder_HostTokenPairNotFound(team.getHostTokenPairName()));
            }
            ur.apiHost = vars.expand(hostTokenPair.getApiHost());
            ur.apiToken = vars.expand(Secret.toString(hostTokenPair.getApiToken()));
            ur.sslEnable = hostTokenPair.getSslEnable();
        }
        else if ( StringUtils.isNotEmpty(team.getEmlauncherCredentialId()) ) {
            EMLauncherCredentials credential = getEmlauncherCredential(build.getParent(), team.getEmlauncherCredentialId());
            if ( credential == null ) {
                throw new MisconfiguredJobException(Messages._TestflightRecorder_HostTokenPairNotFound(team.getEmlauncherCredentialId()));
            }
            ur.apiHost = vars.expand(credential.getApiHost());
            ur.apiToken = vars.expand(credential.getApiToken().getPlainText());
            ur.sslEnable = credential.getSslEnable();
        }
        else {
            ur.apiHost = vars.expand(this.apiHost);
            ur.apiToken = vars.expand(Secret.toString(this.apiToken));
            ur.sslEnable = this.sslEnable;
        }
        ur.filePaths = vars.expand(StringUtils.trim(team.getFilePath()));
        ur.dsymPath = vars.expand(StringUtils.trim(team.getDsymPath()));
        ur.title = vars.expand(title);
	// Add SCM change log to description. (if needed)
	List<ChangeLogSet> changeSets = new ArrayList<ChangeLogSet>();
	try {
	    if ( build instanceof WorkflowRun ) {
		changeSets = (List<ChangeLogSet>)build.getClass().getMethod("getChangeSets").invoke(build);
	    }
	    else if ( build instanceof AbstractBuild ) {
		changeSets.add((ChangeLogSet)build.getClass().getMethod("getChangeSet").invoke(build));
	    }
	    else {
		listener.getLogger().println(Messages.TestflightRecorder_UnknownClassInstance(build.getClass().getName()));
	    }
	}
	catch (java.lang.reflect.InvocationTargetException ex) {
	    listener.getLogger().println("InvocationTargetException");
	}
	catch (java.lang.IllegalAccessException ex) {
	    listener.getLogger().println("IllegalAccessException");
	}
	catch (NoSuchMethodException ex) {
	    listener.getLogger().println("NoSuchMethodException");
	}
	ur.description = createDescription(build, listener, vars.expand(description), changeSets);
        ur.tags = vars.expand(tags);
        ur.notifyTeam = notifyTeam;
        ProxyConfiguration proxy = getProxy();
        ur.proxyHost = proxy.name;
        ur.proxyPass = proxy.getPassword();
        ur.proxyPort = proxy.port;
        ur.proxyUser = proxy.getUserName();
        ur.debug = debug;
        ur.timeout = getTimeout();
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
    private String createDescription(Run<?, ?> build, final TaskListener listener, String description, final List<ChangeLogSet> changeSets) {

        try {
            description = TokenMacro.expandAll((AbstractBuild)build, listener, description);
        }
        catch ( MacroEvaluationException e )  {
            listener.getLogger().println("Error evaluating token: " + e.getMessage());
        }
        catch ( Exception e ) {
            Logger.getLogger(TestflightRecorder.class.getName()).log(Level.SEVERE, null, e);
        }

        if ( appendChangelog ) {
            StringBuilder stringBuilder = new StringBuilder();

            // Show the build notes first
            stringBuilder.append(description);

            // Then append the changelog
            stringBuilder.append("\n\n");
	    if ( changeSets.size() < 1 ) {
		stringBuilder.append(Messages.TestflightRecorder_EmptyChangeSet()).append("\n");
	    }
	    else {
		stringBuilder.append(Messages.TestflightRecorder_Changelog()).append("\n");
		int entryNumber = 1;
		for ( ChangeLogSet<?> changeSet : changeSets ) {
		for ( Entry entry : changeSet ) {
		    stringBuilder.append("\n").append(entryNumber).append(". ");
		    stringBuilder.append(entry.getMsg()).append(" \u2014 ").append(entry.getAuthor());
		    entryNumber++;
		}
		}
	    }
            description = stringBuilder.toString();
        }
        return description;
    }

/*
    public Collection<? extends Action> getProjectActions(Run<?, ?> project) {
        ArrayList<TestflightBuildAction> actions = new ArrayList<TestflightBuildAction>();
        RunList<? extends Run<?, ?>> builds = (AbstractBuild)project.getBuilds();

        Collection predicated = CollectionUtils.select(builds, new Predicate() {
            public boolean evaluate(Object o) {
                Result result = ((Run<?, ?>) o).getResult();
                if (result == null) return false; // currently running builds can have a null result
                return result.isBetterOrEqualTo(Result.SUCCESS);
            }
        });

        ArrayList<Run<?, ?>> filteredList = new ArrayList<Run<?, ?>>(predicated);


        Collections.reverse(filteredList);
        for (Run<?, ?> build : filteredList) {
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
*/

    @Deprecated
    private HostTokenPair getHostTokenPair(String hostTokenPairName) {
        for (HostTokenPair hostTokenPair : getDescriptor().getHostTokenPairs()) {
            if (hostTokenPair.getHostTokenPairName().equals(hostTokenPairName))
                return hostTokenPair;
        }
        return null;
    }

    private EMLauncherCredentials getEmlauncherCredential(Item context, String emlauncherCerdentialId) {
        return (EMLauncherCredentials) CredentialsMatchers.firstOrNull(
                CredentialsProvider.lookupCredentials(EMLauncherCredentials.class, context,
                        ACL.SYSTEM, Collections.EMPTY_LIST),
                CredentialsMatchers.withId(emlauncherCerdentialId));
    }

    @Extension // This indicates to Jenkins that this is an implementation of an extension point.
    @Symbol("emlauncherUploader")
    public static final class DescriptorImpl extends BuildStepDescriptor<Publisher> {
        private final CopyOnWriteList<HostTokenPair> hostTokenPairs = new CopyOnWriteList<HostTokenPair>();

        public DescriptorImpl() {
            super(TestflightRecorder.class);
            load();
        }

        public boolean isApplicable(Class<? extends AbstractProject> c) {
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

        public String getUUID() {
            return "" + UUID.randomUUID().getMostSignificantBits();
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
