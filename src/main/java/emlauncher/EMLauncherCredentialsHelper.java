package emlauncher;

import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import hudson.model.ItemGroup;
import hudson.security.ACL;
import hudson.util.ListBoxModel;
import org.apache.commons.lang.StringUtils;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import java.util.Collections;

/**
 * @author <a href="mailto:kazuhide.t@linux-powered.com">Kazuhide Takahashi</a>
 */
public class EMLauncherCredentialsHelper {

    private EMLauncherCredentialsHelper() {}

    @CheckForNull
    public static EMLauncherCredentials getCredentials(@Nullable String credentialsId, ItemGroup context) {
        if (StringUtils.isBlank(credentialsId)) {
            return null;
        }
        return (EMLauncherCredentials) CredentialsMatchers.firstOrNull(
                CredentialsProvider.lookupCredentials(EMLauncherCredentials.class, context,
                        ACL.SYSTEM, Collections.EMPTY_LIST),
                CredentialsMatchers.withId(credentialsId));
    }

    public static ListBoxModel doFillCredentialsIdItems(ItemGroup context) {
        return new StandardListBoxModel()
                .withEmptySelection()
                .withMatching(
                        CredentialsMatchers.always(),
                        CredentialsProvider.lookupCredentials(EMLauncherCredentials.class,
                                context,
                                ACL.SYSTEM,
                                Collections.EMPTY_LIST));
    }
}
