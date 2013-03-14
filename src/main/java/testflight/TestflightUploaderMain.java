package testflight;

import java.io.File;

public class TestflightUploaderMain {
    /**
     * Useful for testing
     */
    public static void main(String[] args) {
        try {
            upload(args);
        } catch (Exception e) {
            System.err.println(e);
            e.printStackTrace(System.err);
        }
    }

    private static void upload(String[] args) throws Exception {
        TestflightUploader uploader = new TestflightUploader();
        uploader.setLogger(new TestflightUploader.Logger() {
            public void logDebug(String message) {
                System.out.println(message);
            }
        });

        TestflightUploader.UploadRequest r = new TestflightUploader.UploadRequest();
        r.apiToken = args[0];
        r.teamToken = args[1];
        r.buildNotes = args[2];
        File file = new File(args[3]);
        r.file = file;
        r.dsymFile = null;
        r.notifyTeam = true;
        r.replace = false;
        r.lists = args[4];

        uploader.upload(r);
    }
}
