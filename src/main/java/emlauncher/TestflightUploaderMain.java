package emlauncher;

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
        r.apiHost = args[0];
        r.apiToken = args[1];
        r.title = args[2];
        r.description = args[3];
        r.tags = args[4];
        File file = new File(args[5]);
        r.file = file;
        r.dsymFile = null;
        r.notifyTeam = true;
        r.timeout = Integer.parseInt(args[6]);

        uploader.upload(r);
    }
}
