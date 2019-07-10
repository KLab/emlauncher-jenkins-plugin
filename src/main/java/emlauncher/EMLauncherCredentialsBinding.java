/*
 * The MIT License
 *
 *  Copyright (c) 2016, CloudBees, Inc.
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in
 *  all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *  THE SOFTWARE.
 *
 */

package emlauncher;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Run;
import hudson.model.TaskListener;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.credentialsbinding.BindingDescriptor;
import org.jenkinsci.plugins.credentialsbinding.MultiBinding;
import org.kohsuke.stapler.DataBoundConstructor;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author <a href="mailto:kazuhide.t@linux-powered.com">Kazuhide Takahashi</a>
 */
public class EMLauncherCredentialsBinding extends MultiBinding<EMLauncherCredentials> {

    public final static String DEFAULT_API_HOST_VARIABLE_NAME = "API_HOST";
    private final static String DEFAULT_API_TOKEN_VARIABLE_NAME = "API_TOKEN";
    private final static String DEFAULT_SSL_ENABLE_VARIABLE_NAME = "SSL_ENABLE";

    @NonNull
    private final String apiHostVariable;
    @NonNull
    private final String apiTokenVariable;
    @NonNull
    private final String sslEnableVariable;

    /**
     *
     * @param apiHostVariable if {@code null}, {@value DEFAULT_API_HOST_VARIABLE_NAME} will be used.
     * @param apiTokenVariable if {@code null}, {@value DEFAULT_API_TOKEN_VARIABLE_NAME} will be used.
     * @param sslEnableVariable if {@code null}, {@value DEFAULT_SSL_ENABLE_VARIABLE_NAME} will be used.
     * @param credentialsId identifier which should be referenced when accessing the credentials from a job/pipeline.
     */
    @DataBoundConstructor
    public EMLauncherCredentialsBinding(@Nullable String apiHostVariable, @Nullable String apiTokenVariable, @Nullable String sslEnableVariable, String credentialsId) {
        super(credentialsId);
        this.apiHostVariable = StringUtils.defaultIfBlank(apiHostVariable, DEFAULT_API_HOST_VARIABLE_NAME);
        this.apiTokenVariable = StringUtils.defaultIfBlank(apiTokenVariable, DEFAULT_API_TOKEN_VARIABLE_NAME);
        this.sslEnableVariable = StringUtils.defaultIfBlank(sslEnableVariable, DEFAULT_SSL_ENABLE_VARIABLE_NAME);
    }

    @NonNull
    public String getApiHostVariable() {
        return apiHostVariable;
    }

    @NonNull
    public String getApiTokenVariable() {
        return apiTokenVariable;
    }

    @NonNull
    public String getSslEnableVariable() {
        return sslEnableVariable;
    }

    @Override
    protected Class<EMLauncherCredentials> type() {
        return EMLauncherCredentials.class;
    }

    @Override
    public MultiEnvironment bind(@Nonnull Run<?, ?> build, FilePath workspace, Launcher launcher, TaskListener listener) throws IOException, InterruptedException {
        EMLauncherCredentials credential = getCredentials(build);
        Map<String,String> m = new HashMap<String,String>();
        m.put(apiHostVariable, credential.getApiHost());
        m.put(apiTokenVariable, credential.getApiToken().getPlainText());
        m.put(sslEnableVariable, credential.getSslEnable() ? "true": "false");
        return new MultiEnvironment(m);
    }

    @Override
    public Set<String> variables() {
        return new HashSet<String>(Arrays.asList(apiHostVariable, apiTokenVariable, sslEnableVariable));
    }

    @Extension
    public static class DescriptorImpl extends BindingDescriptor<EMLauncherCredentials> {

        @Override protected Class<EMLauncherCredentials> type() {
            return EMLauncherCredentials.class;
        }

        @Override public String getDisplayName() {
            return Messages.EMLauncherCredentials_DisplayName();
        }
    }

}
