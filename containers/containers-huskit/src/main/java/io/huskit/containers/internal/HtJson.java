package io.huskit.containers.internal;

import lombok.experimental.UtilityClass;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.Reader;
import java.io.StringReader;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@UtilityClass
public class HtJson {

    public static Map<String, Object> toMap(String json) {
        return json.equals("{}") ? Map.of() : toMap(new StringReader(json));
    }

    public static Map<String, Object> toMap(Reader json) {
        return new JSONObject(new JSONTokener(json)).toMap();
    }

    public static Stream<Map<String, Object>> toMapStream(String json) {
        return json.equals("[]") ? Stream.of() : toMapStream(new StringReader(json));
    }

    public static Stream<Map<String, Object>> toMapStream(Reader json) {
        return toMapList(json).stream();
    }

    public static List<Map<String, Object>> toMapList(String json) {
        return json.equals("[]") ? List.of() : toMapList(new StringReader(json));
    }

    public static List<Map<String, Object>> toMapList(Reader json) {
        return hide(new JSONArray(new JSONTokener(json)).toList());
    }

    public static String toJson(Map<String, Object> map) {
        return new JSONObject(map).toString();
    }

    @SuppressWarnings("unchecked")
    private static <T> T hide(Object o) {
        return (T) o;
    }
}
