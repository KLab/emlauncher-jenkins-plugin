package emlauncher;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.json.simple.parser.JSONParser;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.Scanner;

import org.apache.commons.lang.builder.ToStringBuilder;

/**
 * A testflight uploader
 */
public class TestflightUploader implements Serializable {
    static interface Logger {
        void logDebug(String message);
    }

    static class UploadRequest implements Serializable {
        String filePaths;
        String dsymPath;
        String apiHost;
        String apiToken;
        boolean sslEnable;
        //String teamToken;
        Boolean notifyTeam;
        String title;
        String description;
        String tags;
        //String buildNotes;
        File file;
        File dsymFile;
        //String lists;
        //Boolean replace;
        String proxyHost;
        String proxyUser;
        String proxyPass;
        int proxyPort;
        Boolean debug;
        int timeout;

        public String toString() {
            return new ToStringBuilder(this)
                    .append("filePaths", filePaths)
                    .append("dsymPath", dsymPath)
                    .append("apiHost", apiHost)
                    .append("apiToken", "********")
                    .append("ssl", sslEnable)
                    .append("notifyTeam", notifyTeam)
                    .append("title", title)
                    .append("description", description)
                    .append("tags", tags)
                    .append("file", file)
                    .append("dsymFile", dsymFile)
                    .append("proxyHost", proxyHost)
                    .append("proxyUser", proxyUser)
                    .append("proxyPass", "********")
                    .append("proxyPort", proxyPort)
                    .append("timeout", timeout)
                    .append("debug", debug)
                    .toString();
        }

        static UploadRequest copy(UploadRequest r) {
            UploadRequest r2 = new UploadRequest();
            r2.filePaths = r.filePaths;
            r2.dsymPath = r.dsymPath;
            r2.apiHost = r.apiHost;
            r2.apiToken = r.apiToken;
            r2.sslEnable = r.sslEnable;
            r2.notifyTeam = r.notifyTeam;
            r2.title = r.title;
            r2.description = r.description;
            r2.tags = r.tags;
            r2.file = r.file;
            r2.dsymFile = r.dsymFile;
            r2.proxyHost = r.proxyHost;
            r2.proxyUser = r.proxyUser;
            r2.proxyPort = r.proxyPort;
            r2.proxyPass = r.proxyPass;
            r2.debug = r.debug;
            r2.timeout = r.timeout;

            return r2;
        }
    }

    private Logger logger = null;

    public void setLogger(Logger logger) {
        this.logger = logger;
    }

    public Map upload(UploadRequest ur) throws IOException, org.json.simple.parser.ParseException {
        DefaultHttpClient httpClient = new DefaultHttpClient();

        HttpParams httpParams = httpClient.getParams();
        
        HttpConnectionParams.setConnectionTimeout(httpParams, ur.timeout);
        HttpConnectionParams.setSoTimeout(httpParams, ur.timeout);

        // Configure the proxy if necessary
        if (ur.proxyHost != null && !ur.proxyHost.isEmpty() && ur.proxyPort > 0) {
            Credentials cred = null;
            if (ur.proxyUser != null && !ur.proxyUser.isEmpty())
                cred = new UsernamePasswordCredentials(ur.proxyUser, ur.proxyPass);

            httpClient.getCredentialsProvider().setCredentials(new AuthScope(ur.proxyHost, ur.proxyPort), cred);
            HttpHost proxy = new HttpHost(ur.proxyHost, ur.proxyPort);
            httpClient.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY, proxy);
        }
        
        HttpHost targetHost;
        if(ur.sslEnable){
        	targetHost = new HttpHost(ur.apiHost, 443, "https");
        } else {
        	targetHost = new HttpHost(ur.apiHost);
        }
        HttpPost httpPost = new HttpPost("/api/upload");
        FileBody fileBody = new FileBody(ur.file);

        MultipartEntityBuilder builder = MultipartEntityBuilder.create();
        builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
        builder.addTextBody("api_key", ur.apiToken, ContentType.TEXT_PLAIN);
        builder.addTextBody("title", ur.title, ContentType.TEXT_PLAIN);
        
        if ( ur.description != null ) {
            builder.addTextBody("description", ur.description, ContentType.TEXT_PLAIN.withCharset("UTF-8"));
        }
        
        if ( ur.tags != null ) {
            builder.addTextBody("tags", ur.tags, ContentType.TEXT_PLAIN.withCharset("UTF-8"));
        }

        builder.addPart("file", fileBody);

        if ( ur.dsymFile != null ) {
            FileBody dsymFileBody = new FileBody(ur.dsymFile);
            builder.addPart("dsym", dsymFileBody);
        }

        builder.addPart("notify", new StringBody(ur.notifyTeam ? "True" : "False"));

        HttpEntity postEntity = builder.build();
        httpPost.setEntity(postEntity);

        logDebug("POST Request: " + ur);

        HttpResponse response = httpClient.execute(targetHost, httpPost);
        HttpEntity resEntity = response.getEntity();

        InputStream is = resEntity.getContent();

        // Improved error handling.
        int statusCode = response.getStatusLine().getStatusCode();
        if (statusCode != 200) {
            String responseBody = new Scanner(is).useDelimiter("\\A").next();
            throw new UploadException(statusCode, responseBody, response);
        }

        StringWriter writer = new StringWriter();
        IOUtils.copy(is, writer, "UTF-8");
        String json = writer.toString();

        logDebug("POST Answer: " + json);

        JSONParser parser = new JSONParser();

        return (Map) parser.parse(json);
    }

    private void logDebug(String message) {
        if (logger != null) {
            logger.logDebug(message);
        }
    }
}
