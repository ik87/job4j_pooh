package ru.job4j;

import com.google.gson.Gson;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Simple implementation async point-to-point (p2p) model.
 * Handling queue JMS
 *
 * @author Kosolapov Ilya (d_dexter@mail.ru)
 * @version 1.0
 * @since 27.10.2020
 */
public class JMSQueue implements RequestResponseHandler {
    private Map<String, Queue<EntityQueue>> queue = new ConcurrentHashMap<>();

    @Override
    public void doGet(Map<String, String> in, BufferedWriter out) {
        try {

            String json = getJsonFromMessage(in);

            if (json != null) {
                Service.transferJson(json, out);
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
            if (isQueueJson(json)) {
                EntityQueue entityQueue = new Gson().fromJson(json, EntityQueue.class);
                putMessageInQueue(entityQueue.getQueue(), entityQueue);
                Service.transferJson(json, out);
                return;
            }
            Service.messageNotFound(out);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Put new json in queue
     *
     * @param key         theme new queue message
     * @param entityQueue entity that will mapping with theme and put in queue theme
     */
    private void putMessageInQueue(String key, EntityQueue entityQueue) {
        if (!queue.containsKey(key)) {
            Queue<EntityQueue> messages = new LinkedList<>();
            messages.add(entityQueue);
            queue.put(key, messages);
        } else {
            queue.get(key).offer(entityQueue);
        }
    }

    /**
     * Check suitable json before parse
     *
     * @param json checked string
     * @return true/false
     */
    private boolean isQueueJson(String json) {
        boolean result = false;
        if (json != null && !json.isEmpty()) {
            Pattern r = Pattern.compile("queue.*:(.*),");
            Matcher m = r.matcher(json);
            result = m.find() && m.groupCount() > 0;
        }
        return result;
    }

    /**
     * Get entity from queue after that the entity transforms to json
     *
     * @param in header
     * @return json
     */
    private String getJsonFromMessage(Map<String, String> in) {
        String path = in.get("Path").split("/")[2];
        String json = null;
        EntityQueue msg = queue.containsKey(path) ? queue.get(path).remove() : null;
        if (msg != null) {
            json = new Gson().toJson(msg);
        }
        return json;
    }
}
