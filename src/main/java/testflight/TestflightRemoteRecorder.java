package testflight;

import hudson.model.BuildListener;
import hudson.remoting.Callable;

import java.io.File;
import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;

/**
 * Code for sending a build to TestFlight which can run on a master or slave.
 * 
 * When the ipa/apk file or optional dsym file are not specified, this class first tries to resolve their paths, searching them inside the workspace.
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
        HashMap result = new HashMap();
        TestflightUploader uploader = new TestflightUploader();

        Collection<File> ipaOrApkFiles = findIpaOrApkFiles(uploadRequest.filePath);
        for (File ipaOrApkFile : ipaOrApkFiles) {
            boolean isIpa = ipaOrApkFile.getName().endsWith(".ipa");
            uploadRequest.file = ipaOrApkFile;
            if (isIpa) {
                uploadRequest.dsymFile = identifyDsym(uploadRequest.dsymPath, ipaOrApkFile.getName());
            }

            listener.getLogger().println("File: " + uploadRequest.file);
            if (isIpa) {
                listener.getLogger().println("DSYM: " + uploadRequest.dsymFile);
            }

            long startTime = System.currentTimeMillis();
            result.putAll(uploader.upload(uploadRequest));
            long time = System.currentTimeMillis() - startTime;

            float speed = computeSpeed(time);
            listener.getLogger().println(Messages.TestflightRemoteRecorder_UploadSpeed(prettySpeed(speed)));
        }

        return result;
    }

    // return the speed in bits per second
    private float computeSpeed(long uploadTimeMillis) {
        if (uploadTimeMillis == 0) {
            return Float.NaN;
        }
        long postSize = 0;
        if (uploadRequest.file != null) {
           postSize += uploadRequest.file.length();           
        }
        if (uploadRequest.dsymFile != null) {
           postSize += uploadRequest.dsymFile.length();           
        }
        return (postSize*8000.0f)/uploadTimeMillis;
    }

    private static String prettySpeed(float speed) {
        if (speed == Float.NaN) return "NaN bps";

        String[] units = { "bps", "Kbps", "Mbps", "Gbps" };
        int idx=0;
        while (speed > 1024 && idx <= units.length - 1) {
            speed /= 1024;
            idx+=1;
        }
        return String.format("%.2f", speed) + units[idx];
    }

    /* if a specified filePath is specified, return it, otherwise find in the workspace the DSYM matching the specified ipa file name */
    private File identifyDsym(String filePath, String ipaName) {
        File dsymFile;
        if (filePath != null && !filePath.trim().isEmpty()) {
            dsymFile = new File(filePath);
        } else {
            String fileName = FilenameUtils.removeExtension(ipaName);
            Collection<File> files = FileUtils.listFiles(new File(remoteWorkspace), FileFilterUtils.nameFileFilter(fileName + "-dSYM.zip"), TrueFileFilter.INSTANCE);
            if (!files.isEmpty()) {
                dsymFile = files.iterator().next();
            } else {
                dsymFile = null;
            }
        }
        return dsymFile;
    }

    /* if a specified filePath is specified, return it, otherwise find recursively all ipa/apk files in the remoteworkspace */
    private Collection<File> findIpaOrApkFiles(String filePath) {
        Collection<File> files;
        if (filePath != null && !filePath.trim().isEmpty()) {
            files = Collections.singleton(new File(filePath));
        } else {
            String[] extensions = {"ipa", "apk"};
            boolean recursive = true;
            files = FileUtils.listFiles(new File(remoteWorkspace), extensions, recursive);
        }
        return files;
    }
}
