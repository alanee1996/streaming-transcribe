package org.streaming.transcribe;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.google.gson.Gson;

public class JsonConvert {
    /**
     * This method will convert any object to json string. It will not throw any exception, but it will print error in console
     * @param obj object that want to convert to json string
     * @param <T> object type
     * @return json string
     */
    public static <T> String JsonSerialize(T obj) {
        try {
            ObjectWriter mapper = new ObjectMapper().writer().withDefaultPrettyPrinter();
            if (obj == null) throw new IllegalArgumentException(obj.getClass().getSimpleName() + " cannot be null");
            String json = mapper.writeValueAsString(obj);
            if (json.isEmpty()) throw new Exception("Error occur during conversion from object to json");
            return json;
        } catch (Exception ex) {
            ex.printStackTrace();
            return "";
        }
    }


    public static <T> String GsonSerialize(T obj) {
        try {
            return new Gson().toJson(obj);
        } catch (Exception ex) {
            ex.printStackTrace();
            return "";
        }
    }
}
