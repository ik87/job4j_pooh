package ru.job4j;

import java.io.BufferedWriter;
import java.util.Map;

/**
 * Main interface for JSM handling GET or POST methods
 *
 * @author Kosolapov Ilya (d_dexter@mail.ru)
 * @version 1.0
 * @since 27.10.2020
 */
public interface RequestResponseHandler {
    /**
     * @param in  request header
     * @param out stream response
     */
    void doGet(Map<String, String> in, BufferedWriter out);

    /**
     * @param in  request header
     * @param out stream response
     */
    void doPost(Map<String, String> in, BufferedWriter out);
}
