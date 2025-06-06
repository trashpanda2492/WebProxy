/*
 * Author: Andrew Hsu
 * Project 2
 * Description: This program is a special webproxy that fixes malformed
 * HTTP requests by reformatting the request with correct capitalization.
 * The server response is forwarded back to the client.
 */

import java.io.*;
import java.net.*;
import java.lang.Thread;
import static java.lang.System.out;

public class WebProxy {
	public static void main(String[] args) {
        try {
            if (args.length != 2)
                throw new IllegalArgumentException("Insuficient arguments");
            // and the local port that we listen for connections on
            String host = "localhost";
            int remoteport = 80;
            int localport = Integer.parseInt(args[1]);
            // Print a start-up message
            out.println("Listening on port " + localport);
            ServerSocket server = new ServerSocket(localport);
            while (true) {
            	Socket client = server.accept();
            	new ThreadProxy(client, host, remoteport);
            }
        } catch (Exception e) {
            System.err.println(e);
            System.err.println("Usage: java WebProxy "
                    + "-p <localport>");
        }
    }
}
/**
 * Handles a socket connection to the proxy server from the client and uses 2
 * threads to proxy between server and client
 *
 * @author jcgonzalez.com
 *
 */
class ThreadProxy extends Thread implements Runnable{
    private Socket sClient;
    private final String SERVER_URL;
    private final int SERVER_PORT;
    ThreadProxy(Socket sClient, String ServerUrl, int ServerPort) {
        this.SERVER_URL = ServerUrl;
        this.SERVER_PORT = ServerPort;
        this.sClient = sClient;
        this.start();
    }
    @Override
    public void run() {
        try {
            final byte[] request = new byte[1024];
            byte[] reply = new byte[4096];
            final InputStream inFromClient = sClient.getInputStream();
            final OutputStream outToClient = sClient.getOutputStream();
            Socket server = null;
            // connects a socket to the server
            try {
                server = new Socket(SERVER_URL, SERVER_PORT);
                out.println("Socket successfully connected to server!");
            } catch (IOException e) {
                PrintWriter out = new PrintWriter(new OutputStreamWriter(
                        outToClient));
                out.flush();
                throw new RuntimeException(e);
            }
            // a new thread to manage streams from server to client (DOWNLOAD)
            final InputStream inFromServer = server.getInputStream();
            final OutputStream outToServer = server.getOutputStream();
            // a new thread for uploading to the server
            new Thread() {
                public void run() {
                    int bytes_read;
                    try {
                        while ((bytes_read = inFromClient.read(request)) != -1) {
                            outToServer.write(request, 0, bytes_read);
                            outToServer.flush();
                        }
                    } catch (IOException e) {
                    }
                    try {
                        outToServer.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }.start();
            // current thread manages streams from server to client (DOWNLOAD)
            int bytes_read;
            try {
                while ((bytes_read = inFromServer.read(reply)) != -1) {
                    outToClient.write(reply, 0, bytes_read);
                    outToClient.flush();
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    if (server != null)
                        server.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            outToClient.close();
            sClient.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
} // WebProxy
