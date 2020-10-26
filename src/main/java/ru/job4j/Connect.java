package ru.job4j;


import javax.annotation.concurrent.ThreadSafe;
import java.io.*;
import java.net.Socket;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Class where happens filtering and mapping with suitable JMS handler (queue or topic)
 *
 * @author Kosolapov Ilya (d_dexter@mail.ru)
 * @version 1.0
 * @since 27.10.2020
 */
@ThreadSafe
class Connect {
    private static final Connect INSTANCE = new Connect();
    private final Map<String, RequestResponseHandler> hendlers;
    private final RequestResponseHandler queue = new JMSQueue();
    private final RequestResponseHandler topic = new JMSTopic();

    private Connect() {
        hendlers = new HashMap<>();
        hendlers.put("/queue", queue);
        hendlers.put("/topic", topic);
    }

    static Connect getInstance() {
     return INSTANCE;
    }

    /**
     * This is main method where processed any new connection
     * get IO streams, filtering, mapping with suitable JMS handler
     * @param socket new connection
     */
    void accept(Socket socket) {
        // Get input and output streams
        try (BufferedReader in =
                     new BufferedReader(
                             new InputStreamReader(socket.getInputStream()));
             BufferedWriter out =
                     new BufferedWriter(
                             new OutputStreamWriter(socket.getOutputStream()))) {


            Map<String, String> req = parseHeader(in);
            String method = req.get("Method");
            String path = req.get("Path");

            //filtering and mapping
            for (String key : hendlers.keySet()) {
                if (path.startsWith(key)) {
                    RequestResponseHandler handler = hendlers.get(key);
                    if ("GET".equals(method) && path.matches("/.+/.+/*")) {
                        handler.doGet(req, out);
                        break;
                    } else if ("POST".equals(method)) {
                        handler.doPost(req, out);
                        break;
                    } else {
                        Service.badRequest(out);
                        break;
                    }
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("close connect");
    }

    /**
     * Parse the header
     * @param in input stream
     * @return map with name header and his value
     * @throws IOException in case if stream has problem
     */
    Map<String, String> parseHeader(BufferedReader in) throws IOException {
        Map<String, String> req = new LinkedHashMap<>();
        String userInput = in.readLine();
        req.put("Method", userInput.split(" ")[0]);
        req.put("Path", userInput.split(" ")[1]);
        while ((userInput = in.readLine()) != null && (userInput.length() != 0)) {
            String[] arrReq = userInput.split(":");
            req.put(arrReq[0], arrReq[1].trim());
        }
        String contentLength = req.get("Content-Length");
        if (contentLength != null && contentLength.length() != 0) {
            int postDataI = Integer.valueOf(contentLength);
            char[] charArray = new char[postDataI];
            in.read(charArray, 0, postDataI);
            req.put("Body", String.valueOf(charArray));
        }
        return req;
    }

}