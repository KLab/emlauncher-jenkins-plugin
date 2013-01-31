package testflight;

import hudson.model.BuildListener;
import hudson.remoting.Callable;

import java.io.File;
import java.io.Serializable;

/**
 * Code for sending a build to TestFlight which can run on a master or slave.
 * 
 * When the ipa file or optional dsym file are not specified, this class first tries to resolve their paths, searching them inside the workspace.
 */
public class TestflightRemoteRecorder implements Callable<Object, Throwable>, Serializable {
    final private String remoteWorkspace;
    final private TestflightUploader.UploadRequest uploadRequest;
    final private BuildListener listener;

    public TestflightRemoteRecorder(String remoteWorkspace, TestflightUploader.UploadRequest uploadRequest, BuildListener listener) {
        this.remoteWorkspace = remoteWorkspace;
        this.uploadRequest = uploadRequest;
        this.listener = listener;
    }

    public Object call() throws Throwable {
        uploadRequest.file = identifyIpa();
        uploadRequest.dsymFile = identifyDsym();

        listener.getLogger().println("IPA: " + uploadRequest.file);
        listener.getLogger().println("DSYM: " + uploadRequest.dsymFile);

        TestflightUploader uploader = new TestflightUploader();
        return uploader.upload(uploadRequest);
    }

    private File identifyIpa() {
        return identifyFile(uploadRequest.filePath, ".ipa");
    }

    private File identifyDsym() {
        return identifyFile(uploadRequest.dsymPath, "-dSYM.zip");
    }

    private File identifyFile(String filePath, String suffix) {
        if (filePath != null && !filePath.trim().isEmpty()) {
            return new File(filePath);
        } else {
            return findFirstFile(new File(remoteWorkspace), suffix);
        }
    }

    /* Finds the first file ending with the specified suffix searching recursively inside the specified root, or null otherwise */
    static File findFirstFile(File root, String suffix) {
        for (File file : root.listFiles()) {

            if (file.isDirectory())
            {
                File result = findFirstFile(file, suffix);
                if(result != null)
                    return result;
            }
            else if (file.getName().endsWith(suffix))
            {
                return file;
            }
        }
        return null;
    }

}
