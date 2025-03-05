package me.gosdev.chatpointsttv.twitch.auth;

import me.gosdev.chatpointsttv.utils.Scopes;

import java.io.*;
import java.net.Socket;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.bukkit.Bukkit.getLogger;

public class AuthenticationCallbackRequest implements Runnable {

    private static final String EOL = "\r\n";
    private static final Logger LOG = getLogger();

    private final Socket socket;
    private final URL authPage;
    private final URL failurePage;
    private final URL successPage;

    private AuthenticationListener authenticationListener;

    /**
     * Construct the request and specify which HTML files to server.
     *
     * @param socket      Connection socket of the request
     * @param authPage    HTML page that twitch.tv will send the access_token to
     * @param failurePage HTML page that shows auth error to user
     * @param successPage HTML page that shows auth success to user
     */
    public AuthenticationCallbackRequest(Socket socket, URL authPage, URL failurePage, URL successPage) {
        this.socket = socket;
        this.authPage = authPage;
        this.failurePage = failurePage;
        this.successPage = successPage;
    }

    /**
     * Send bytes from file input stream to the socket output stream.
     *
     * @param fis InputStream of the file contents.
     * @param os  OutputStream of the socket output stream.
     * @throws IOException if an I/O exception occurs.
     */
    private static void sendFileBytes(InputStream fis, OutputStream os) throws IOException {
        byte[] buffer = new byte[1024];
        int bytes;
        while ((bytes = fis.read(buffer)) != -1) {
            os.write(buffer, 0, bytes);
        }
    }

    /**
     * Extract the GET parameters from the HTTP request string.
     *
     * @param request HTTP request string
     * @return Map of all GET parameter key value pairs
     */
    private static Map<String, String> extractQueryParams(String request) {
        Map<String, String> params = new HashMap<>();

        String[] parts = request.split("\\?", 2);
        if (parts.length < 2) {
            return params;
        }

        String query = parts[1];
        for (String param : query.split("&")) {
            String[] pair = param.split("=");

            String key = URLDecoder.decode(pair[0], StandardCharsets.UTF_8);
            String value = "";
            if (pair.length > 1) {
                value = URLDecoder.decode(pair[1], StandardCharsets.UTF_8);
            }
            params.put(key, value);
        }

        return params;
    }

    public void setAuthenticationListener(AuthenticationListener receiver) {
        this.authenticationListener = receiver;
    }

    @Override
    public void run() {
        try {
            processRequest();
        } catch (Exception exception) {
            LOG.log(Level.WARNING, "error while processing request", exception);
        }
    }

    /**
     * Process the HTTP request and send out correct page.
     */
    private void processRequest() throws IOException {


        try (
                DataOutputStream os = new DataOutputStream(socket.getOutputStream());
                BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {


            String requestLine = br.readLine();

            Map<String, String> queryParams = getParams(requestLine);

            String scope = queryParams.get("scope").split(" ")[0];

            String accessToken = queryParams.get("access_token");
            String error = queryParams.get("error");
            String errorDescription = queryParams.get("error_description");

            write(accessToken, error, os);

            socket.close();


            if (authenticationListener != null) {
                if (accessToken != null) {
                    Scopes accessScopes = Scopes.fromString(scope);
                    authenticationListener.onAccessTokenReceived(accessToken, accessScopes);
                }
                if (error != null) {
                    authenticationListener.onAuthenticationError(error, errorDescription);
                }
            }
        }

    }

    private void write(String accessToken, String error, DataOutputStream os) throws IOException {
        InputStream inputStream = getInputStream(accessToken, error);

        os.writeBytes("HTTP/1.1 200 OK" + EOL);

        os.writeBytes("Content-type: text/html" + EOL);

        os.writeBytes(EOL);

        sendFileBytes(inputStream, os);
        inputStream.close();
    }

    private InputStream getInputStream(String accessToken, String error) throws IOException {
        if (accessToken != null) {
            return successPage.openStream();
        } else if (error != null) {
            return failurePage.openStream();
        }
        return authPage.openStream();
    }

    private Map<String, String> getParams(String requestLine) {
        StringTokenizer tokens = new StringTokenizer(requestLine);
        String requestFilename = tokens.nextToken();
        return extractQueryParams(requestFilename);
    }
}