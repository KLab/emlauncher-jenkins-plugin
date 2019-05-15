/*
 * The MIT License
 *
 * Copyright (c) 2011-2016, CloudBees, Inc., Stephen Connolly.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package emlauncher;

import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.Util;
import hudson.security.ACL;
import hudson.util.Secret;
import org.kohsuke.stapler.DataBoundConstructor;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.impl.BaseStandardCredentials;

import java.util.Collections;
import java.util.List;

/**
 *
 */
@SuppressWarnings("unused") // read resolved by extension plugins
public class EMLauncherCredentialsImpl extends BaseStandardCredentials implements EMLauncherCredentials {

    /**
     * The EMLauncher hostname or IP address.
     */
    @NonNull
    private final String apiHost;

    /**
     * The EMLauncher API key for App.
     */
    @NonNull
    private final Secret apiToken;

    /**
     * The flag for keychain in search path.
     */
    @NonNull
    private final boolean sslEnable;

    /**
     * Constructor.
     *
     * @param scope           the credentials scope
     * @param id              the ID or {@code null} to generate a new one.
     * @param description     the description.
     * @param apiHost         the EMLauncher hostname or IP address.
     * @param apiToken        the EMLauncher API key for App.
     * @param sslEnable       flag for use SSL/TLS for connection.
     */
    @DataBoundConstructor
    @SuppressWarnings("unused") // by stapler
    public EMLauncherCredentialsImpl(@CheckForNull CredentialsScope scope,
                                     @CheckForNull String id,
                                     @CheckForNull String description,
                                     @CheckForNull String apiHost,
                                     @CheckForNull String apiToken,
                                     boolean sslEnable) {
        super(scope, id, description);
        this.apiHost = Util.fixNull(apiHost);
        this.apiToken = Secret.fromString(apiToken);
        this.sslEnable = sslEnable;
    }

    /**
     * EMLauncher hostname or IP address.
     * @return EMLauncher hostname or IP address.
     */
    @NonNull
    public String getApiHost() {
        return apiHost;
    }

    /**
     * EMLauncher API Key.
     * @return EMLauncher API Key.
     */
    @NonNull
    public Secret getApiToken() {
        return apiToken;
    }

    /**
     * flag for use SSL/TLS for connection..
     * @return flag for use SSL/TLS for connection..
     */
    public boolean getSslEnable() {
        return sslEnable;
    }

    /**
     * {@inheritDoc}
     */
    @Extension
    public static class DescriptorImpl extends BaseStandardCredentialsDescriptor {

        /**
         * {@inheritDoc}
         */
        @Override
        public String getDisplayName() {
            return Messages.EMLauncherCredentials_DisplayName();
        }

        /*
         * {@inheritDoc}
         *
        @Override
        public String getIconClassName() {
            return "icon-credentials";
        }
        */
    }

    public static List<EMLauncherCredentialsImpl> getAllCredentials() {
        return CredentialsProvider.lookupCredentials(EMLauncherCredentialsImpl.class, (hudson.model.Item)null, ACL.SYSTEM, Collections.<DomainRequirement>emptyList());
    }

}
