package me.gosdev.chatpointsttv.twitch.auth;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

import lombok.Getter;
import me.gosdev.chatpointsttv.utils.Scopes;

import static org.bukkit.Bukkit.getLogger;

public class AuthenticationCallbackServer implements AuthenticationListener {

    /**
     * Default HTML page that twitch.tv will send the access_token to
     */
    public static final String DEFAULT_AUTH_PAGE = "/authorize.html";

    /**
     * Default HTML page that shows auth error's to user
     */
    public static final String DEFAULT_FAILURE_PAGE = "/authorize-failure.html";

    /**
     * Default HTML page that shows auth success to
     */
    public static final String DEFAULT_SUCCESS_PAGE = "/authorize-success.html";
    private static final Logger LOG = getLogger();
    private final URL authPage;
    private final URL failurePage;
    private final URL successPage;
    private final int port;
    private ServerSocket serverSocket;
    @Getter
    private String accessToken;
    Thread thread;

    /**
     * Constructor that will use default HTML views for output.
     *
     * @param port Network port to receive requests on
     */
    public AuthenticationCallbackServer(int port) {
        this.port = port;
        authPage = getClass().getResource(DEFAULT_AUTH_PAGE);
        failurePage = getClass().getResource(DEFAULT_FAILURE_PAGE);
        successPage = getClass().getResource(DEFAULT_SUCCESS_PAGE);
    }

    /**
     * Start the server and listen for auth callbacks from twitch.
     *
     * @throws IOException if an I/O error occurs while waiting for a connection.
     */
    public void start() throws IOException {
        serverSocket = new ServerSocket(port, 0, InetAddress.getByName("127.0.0.1"));
        run();
    }

    private void run() throws IOException {
        while (!serverSocket.isClosed()) {
            try {
                Socket connectionSocket = serverSocket.accept();
                AuthenticationCallbackRequest request = new AuthenticationCallbackRequest(connectionSocket, authPage, failurePage, successPage);
                request.setAuthenticationListener(this);
                thread = new Thread(request);
                thread.start();
            } catch (SocketException e) {
                break;
            }
        }
    }

    /**
     * Stops the server.
     */
    public void stop() {
        if (serverSocket != null && !serverSocket.isClosed()) {
            try {
                serverSocket.close();
                if (thread != null) {
                    thread.interrupt();
                }
            } catch (IOException exception) {
                LOG.log(Level.WARNING, "error while stopping callback server", exception);
            } finally {
                serverSocket = null;
            }
        }
    }

    /**
     * Check to see if server is running.
     *
     * @return <code>true</code> if server is running. <code>false</code> otherwise
     */
    public boolean isNotRunning() {
        return serverSocket == null;
    }

    @Override
    public void onAccessTokenReceived(String token, Scopes... scopes) {
        accessToken = token;
        stop();
    }

    @Override
    public void onAuthenticationError(String error, String description) {
        stop();
    }

}
