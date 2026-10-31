package tools.jackson;

import tools.jackson.databind.ObjectMapper;

public class JacksonUtils {

    public static ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public static <T> T convertJsonToObject(String jsonString, Class<T> clazz) {
        return OBJECT_MAPPER.readValue(jsonString, clazz);
    }

}
