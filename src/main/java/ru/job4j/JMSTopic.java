package ru.job4j;

import com.google.gson.Gson;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.*;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Simple implementation async provider-subscriber model.
 * Handling topic JMS, every new "GET" connection get new cookie (for identification).
 * Provider make publications and all subscribers can get them in order.
 * All subscribers have his self queue.
 * First request will subscribe new clients, after that, they can get messages.
 *
 * @author Kosolapov Ilya (d_dexter@mail.ru)
 * @version 1.0
 * @since 27.10.2020
 */
public class JMSTopic implements RequestResponseHandler {
    private final Map<String, Map<String, Queue<EntityTopic>>> subscribers = new ConcurrentHashMap<>();

    @Override
    public void doGet(Map<String, String> in, BufferedWriter out) {
        try {
            String cookie = in.get("Cookie");
            if (cookie == null || !subscribers.containsKey(cookie)) {
                subscribe(cookie, out);
                return;
            }

            String msg = getJsonFromMessage(in, cookie);
            if (msg != null) {
                Service.transferMsg(msg, out);
                return;
            }
            Service.messageNotFound(out);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void doPost(Map<String, String> in, BufferedWriter out) {
        String json = in.get("Body");
        try {
            if (isTopicJson(json)) {
                EntityTopic entityTopic = new Gson().fromJson(json, EntityTopic.class);
                putMessageInPoolSessions(entityTopic.getTopic(), entityTopic);
                Service.transferMsg(json, out);
                return;
            }
            Service.badRequest(out);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Check suitable json before parse
     *
     * @param json checked string
     * @return true/false
     */
    private boolean isTopicJson(String json) {
        boolean result = false;
        if (json != null && !json.isEmpty()) {
            Pattern r = Pattern.compile("topic.*:(.*),");
            Matcher m = r.matcher(json);
            result = m.find() && m.groupCount() > 0;
        }
        return result;
    }

    /**
     * Subscribing a new client by set the random number in his cooke
     * and response message about it
     *
     * @param cookie check cookie on exist
     * @param out    response stream
     * @throws IOException if something going wrong with out stream
     */
    private void subscribe(String cookie, BufferedWriter out) throws IOException {
        if (cookie == null) {
            cookie = String.valueOf(new Random().nextLong());
            out.write("Set-Cookie: " + cookie + "\n");
        }
        subscribers.put(cookie, new ConcurrentHashMap<>());
        String msg = "<HTML><HEAD>\n"
                + "<TITLE>YOU HAVE BEEN SUBSCRIBED</TITLE>\n"
                + "</HEAD><BODY>\n"
                + "<H1>YOU HAVE BEEN SUBSCRIBED</H1>\n"
                + "</BODY></HTML>";
        Service.transferMsg(msg, out);
    }

    /**
     * Check sub patch and get json from Pool Subscribers
     *
     * @param in     header
     * @param cookie for search and get subscriber
     * @return json
     */
    private String getJsonFromMessage(Map<String, String> in, String cookie) {
        String subpath = in.get("Path").split("/")[2];
        String json = null;
        EntityTopic msg = getMessageFromSubscribersPool(cookie, subpath);
        if (msg != null) {
            json = new Gson().toJson(msg);
        }
        return json;
    }

    /**
     * Get entity topic from subscribers pool
     *
     * @param cooke   identical subscriber cooke
     * @param subpath relate url, key, for example localhost/topic/weather, "weather" is subpath
     * @return entity topic
     */
    private EntityTopic getMessageFromSubscribersPool(String cooke, String subpath) {
        EntityTopic entityTopic = null;
        if (subscribers.get(cooke).containsKey(subpath)) {
            entityTopic = subscribers.get(cooke).get(subpath).remove();
            if (subscribers.get(cooke).get(subpath).isEmpty()) {
                subscribers.get(cooke).remove(subpath);
            }
        }
        return entityTopic;
    }

    /**
     * Put new json in queue for all subscribers
     *
     * @param key         theme new topic message
     * @param entityTopic entity that will mapping with theme and put in queue theme
     */
    private void putMessageInPoolSessions(String key, EntityTopic entityTopic) {
        for (String k : subscribers.keySet()) {
            if (!subscribers.get(k).containsKey(key)) {
                Queue<EntityTopic> queue = new LinkedList<>();
                queue.add(entityTopic);
                subscribers.get(k).put(entityTopic.getTopic(), queue);
            } else {
                subscribers.get(k).get(key).offer(entityTopic);
            }

        }
    }

}
