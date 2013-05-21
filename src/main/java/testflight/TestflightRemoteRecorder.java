package testflight;

import hudson.model.BuildListener;
import hudson.remoting.Callable;
import hudson.Util;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.apache.tools.ant.types.FileSet;
import org.apache.commons.io.FilenameUtils;

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
        TestflightUploader uploader = new TestflightUploader();
        if (uploadRequest.debug != null && uploadRequest.debug) {
            uploader.setLogger(new TestflightUploader.Logger() {
                public void logDebug(String message) {
                    listener.getLogger().println(message);
                }
            });
        }
        return uploadWith(uploader);
    }

    HashMap uploadWith(TestflightUploader uploader) throws Throwable {
        HashMap result = new HashMap();

        Collection<File> ipaOrApkFiles = findIpaOrApkFiles(uploadRequest.filePaths);
        for (File ipaOrApkFile : ipaOrApkFiles) {
            TestflightUploader.UploadRequest ur = TestflightUploader.UploadRequest.copy(uploadRequest);
            boolean isIpa = ipaOrApkFile.getName().endsWith(".ipa");
            ur.file = ipaOrApkFile;
            if (isIpa) {
                ur.dsymFile = identifyDsym(ur.dsymPath, ipaOrApkFile.toString());
            }

            listener.getLogger().println("File: " + ur.file);
            if (isIpa) {
                listener.getLogger().println("DSYM: " + ur.dsymFile);
            }

            long startTime = System.currentTimeMillis();
            result.putAll(uploader.upload(ur));
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
            dsymFile = findRelativeFile(filePath);
        } else {
            String fileName = FilenameUtils.removeExtension(ipaName);
            File f = new File(fileName + "-dSYM.zip");
            if (f.exists()) {
                dsymFile = f;
            } else {
                dsymFile = null;
            }
        }
        return dsymFile;
    }

    /* if a specified filePath is specified, return it, otherwise find recursively all ipa/apk files in the remoteworkspace */
    private Collection<File> findIpaOrApkFiles(String filePaths) {
        if (filePaths == null || filePaths.trim().isEmpty()) {
            filePaths = "**/*.ipa, **/*.apk";
        }
        FileSet fileSet = Util.createFileSet(new File(remoteWorkspace), filePaths, null);
        List<File> files = new ArrayList<File>();
        Iterator it = fileSet.iterator();
        while(it.hasNext()) {
            files.add(new File(it.next().toString()));
        }
        return files;
    }

    private File findRelativeFile(String path) {
        File f = new File(path);
        if (f.exists())
            return f;
        f = new File(remoteWorkspace, path);
        if (f.exists())
            return f;
        throw new IllegalArgumentException("Couldn't find file " + path + " in workspace " + remoteWorkspace);
    }
}
