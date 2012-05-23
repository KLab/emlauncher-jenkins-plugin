package testflight;

import hudson.model.BuildListener;
import hudson.remoting.Callable;
import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

/**
 * Code for sending a build to TestFlight which can run on a master or slave.
 */
public class TestflightRemoteRecorder implements Callable<Object, Throwable>, Serializable {
    final private String filePath;
    final private String dsymPath;
    final private boolean pathSpecified;
    final private TestflightUploader.UploadRequest uploadRequest;
    final private BuildListener listener;

    public TestflightRemoteRecorder(String filePath, String dsymPath, boolean pathSpecified, TestflightUploader.UploadRequest uploadRequest, BuildListener listener) {
        this.filePath = filePath;
        this.dsymPath = dsymPath;
        this.pathSpecified = pathSpecified;
        this.uploadRequest = uploadRequest;
        this.listener = listener;
    }

    public Object call() throws Throwable {
        if (!StringUtils.isEmpty(dsymPath))
            uploadRequest.dsymFile = new File(dsymPath);

        if (pathSpecified) {
            uploadRequest.file = new File(filePath);
        } else {
            File workspaceDir = new File(filePath);
            List<File> ipas = new LinkedList<File>();
            TestflightRemoteRecorder.findIpas(workspaceDir, ipas);
            if (ipas.isEmpty())
                uploadRequest.file = workspaceDir;
            else
                uploadRequest.file = ipas.get(0);
        }

        listener.getLogger().println(uploadRequest.file);

        TestflightUploader uploader = new TestflightUploader();
        return uploader.upload(uploadRequest);
    }

    public static void findIpas(File root, List<File> ipas) {
        for (File file : root.listFiles()) {
            if (file.isDirectory())
                findIpas(file, ipas);
            else if (file.getName().endsWith(".ipa"))
                ipas.add(file);
        }
    }
}
