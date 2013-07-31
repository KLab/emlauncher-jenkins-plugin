package testflight;

import hudson.model.BuildListener;

import java.io.IOException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;

import hudson.Util;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

import org.mockito.*;
import org.hamcrest.Description;


public class TestflightRemoteRecorderTest {
    static File tmpDir;
    static File ipa1;
    static File dSYM1;
    static File ipa2;
    static File dSYM2;
    static File ipa3;
    static File apk1;
    TestflightUploader uploader;
    BuildListener listener;

    @BeforeClass
    public static void SetUpDirectoryLayout() throws IOException {
        tmpDir = Util.createTempDir();
        new File(tmpDir, "a/b").mkdirs();
        new File(tmpDir, "a/c").mkdirs();
        new File(tmpDir, "a/d").mkdirs();
        ipa1 = new File(tmpDir, "a/b/test.ipa");
        dSYM1 = new File(tmpDir, "a/b/test-dSYM.zip");
        ipa2 = new File(tmpDir, "a/c/test.ipa");
        dSYM2 = new File(tmpDir, "a/c/test.dSYM.zip");
        ipa3 = new File(tmpDir, "a/d/test.ipa");
        apk1 = new File(tmpDir, "a/d/test.apk");
        touch(ipa1, 0);
        touch(dSYM1, 0);
        touch(ipa2, 0);
        touch(dSYM2, 0);
        touch(ipa3, 0);
        touch(apk1, 0);
    }

    @Before
    public void setUpMocks() {
        listener = mock(BuildListener.class);
        uploader = mock(TestflightUploader.class);

        PrintStream mockLogger = mock(PrintStream.class);
        when(listener.getLogger()).thenReturn(mockLogger);
    }

    @Test
    public void testPrettySpeed() {
        assertEquals("NaN bps", TestflightRemoteRecorder.prettySpeed(Float.NaN));
    }

    @Test
    public void findMultipleFiles() throws Throwable {
        TestflightUploader.UploadRequest ur = createTestUploadRequest(null);

        TestflightRemoteRecorder remoteRecorder = new TestflightRemoteRecorder(tmpDir.toString(), ur, listener);

        remoteRecorder.uploadWith(uploader);

        verify(uploader).upload(argThat(new IsUploadRequestForRightFiles(ipa1, dSYM1)));
        verify(uploader).upload(argThat(new IsUploadRequestForRightFiles(ipa2, dSYM2)));
        verify(uploader).upload(argThat(new IsUploadRequestForRightFiles(ipa3, null)));
        verify(uploader).upload(argThat(new IsUploadRequestForRightFiles(apk1, null)));

        verifyNoMoreInteractions(uploader);
    }

    @Test
    public void findFilesWithAbsolutePaths() throws Throwable {
        TestflightUploader.UploadRequest ur = createTestUploadRequest(apk1.getPath());

        TestflightRemoteRecorder remoteRecorder = new TestflightRemoteRecorder(tmpDir.toString(), ur, listener);

        remoteRecorder.uploadWith(uploader);

        verify(uploader).upload(argThat(new IsUploadRequestForRightFiles(apk1, null)));

        verifyNoMoreInteractions(uploader);
    }

    @Test
    public void findFilesWithRelativePaths() throws Throwable {
        TestflightUploader.UploadRequest ur = createTestUploadRequest("a/d/test.apk");

        TestflightRemoteRecorder remoteRecorder = new TestflightRemoteRecorder(tmpDir.toString(), ur, listener);

        remoteRecorder.uploadWith(uploader);

        verify(uploader).upload(argThat(new IsUploadRequestForRightFiles(apk1, null)));

        verifyNoMoreInteractions(uploader);
    }

    static class IsUploadRequestForRightFiles extends ArgumentMatcher<TestflightUploader.UploadRequest> {
        File file;
        File dsymFile;

        IsUploadRequestForRightFiles(File f, File d) {
            file = f;
            dsymFile = d;
        }

        public void describeTo(Description description) {
            super.describeTo(description);
            description.appendText("file=").appendText(notNull(file));
            description.appendText(",dsymFile=").appendText(notNull(dsymFile));
        }

        public boolean matches(Object request) {
            TestflightUploader.UploadRequest r = (TestflightUploader.UploadRequest) request;
            return objectEquals(r.file, file) && objectEquals(r.dsymFile, dsymFile);
        }

        private static boolean objectEquals(Object o1, Object o2) {
            return (o1 == null ? o2 == null : o1.equals(o2));
        }

        private static String notNull(Object o) {
            return o == null ? "<null>" : o.toString();
        }
    }


    private TestflightUploader.UploadRequest createTestUploadRequest(String paths) {
        TestflightUploader.UploadRequest r = new TestflightUploader.UploadRequest();
        r.filePaths = paths;
        return r;
    }


    // copied from FilePath.touch()
    private static void touch(File f, long timestamp) throws IOException {
        if (!f.exists())
            new FileOutputStream(f).close();
        if (!f.setLastModified(timestamp))
            throw new IOException("Failed to set the timestamp of " + f + " to " + timestamp);
    }
}