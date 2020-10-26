package ru.job4j;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Simple implementation async point-to-point (queue)  and provider-subscriber (topic) models
 * Entry point class. It catch new connection  and put it in thread pool for further processing,
 * also it has some static REST headers for response
 *
 * @author Kosolapov Ilya (d_dexter@mail.ru)
 * @version 1.0
 * @since 27.10.2020
 */
public class Service {
    private static SimpleDateFormat format = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:Ss z");
    private static final int PORT = 80;

    public static void main(String[] args) throws IOException {

        ServerSocket serverSocket = new ServerSocket(PORT);
        int numCors = Runtime.getRuntime().availableProcessors();
        Connect connect = Connect.getInstance();
        ExecutorService pool = Executors.newFixedThreadPool(numCors);


        while (true) {
            Socket socket = serverSocket.accept();
            System.out.println("connected");
            pool.submit(() -> {
                connect.accept(socket);
            });
        }
    }

    //REST response...

    static void badRequest(BufferedWriter out) throws IOException {
        String msg = "<HTML><HEAD>\n"
                + "<TITLE>400 Bad Request</TITLE>\n"
                + "</HEAD><BODY>\n"
                + "<H1>Bad Request</H1>\n"
                + "</BODY></HTML>";
        String res = "HTTP/1.1 400 Bad Request\n"
                + "Server: HTTP server/0.1\n"
                + "Date: " + format.format(new java.util.Date())
                + "\n"
                + "Content-type: text/html; charset=UTF-8\n"
                + "Content-Length: " + msg.length() + "\n\n" + msg;
        out.write(res);
        out.flush();
    }

    static void ok(BufferedWriter out) throws IOException {
        String res = "HTTP/1.1 200 OK\n"
                + "Server: HTTP server/0.1\n"
                + "Date: "
                + format.format(new java.util.Date())
                + "\n";
        out.write(res);
    }

    static void messageNotFound(BufferedWriter out) throws IOException {
        String msg = "<HTML><HEAD>\n"
                + "<TITLE>Message not found</TITLE>\n"
                + "</HEAD><BODY>\n"
                + "<H1>Message not found</H1>\n"
                + "</BODY></HTML>";
        transferMsg(msg, out);
    }

    static void transferMsg(String msg, BufferedWriter out) throws IOException {
        Service.ok(out);
        String res = "Content-type: text/html; charset=UTF-8\n"
                + "Content-Length: " + msg.length() + "\n\n"
                + msg;
        out.write(res);
        out.flush();
    }

    static void transferJson(String msg, BufferedWriter out) throws IOException {
        Service.ok(out);
        String res = "Content-type: application/json; charset=UTF-8\n"
                + "Content-Length: "
                + msg.length()
                + "\n\n"
                + msg;
        out.write(res);
        out.flush();
    }
}

