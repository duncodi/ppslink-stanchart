package com.duncodi.ppslink.stanchart.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;

public class JsonUtil {

    private static final ObjectMapper objectMapper = new ObjectMapper()
            .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
            .enable(SerializationFeature.INDENT_OUTPUT);

    public static <T> JsonNode convertToJsonNode(T object) {
        try {
            if (object == null) {
                return objectMapper.createObjectNode();
            }
            return objectMapper.valueToTree(object);
        } catch (IllegalArgumentException e) {
            return objectMapper.createObjectNode();
        }
    }

    public static String convertToJsonString(Object object) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }

    public static String convertToPrettyJsonString(Object object) {
        try {
            ObjectWriter writer = objectMapper.writerWithDefaultPrettyPrinter();
            return writer.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }

}
