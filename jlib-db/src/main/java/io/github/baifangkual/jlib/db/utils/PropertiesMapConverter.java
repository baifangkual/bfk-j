package io.github.baifangkual.jlib.db.utils;

import lombok.NonNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * @author baifangkual
 * create time 2024/7/11
 * Map[String, String] 与 Properties 相互转换的工具
 */
public class PropertiesMapConverter {

    private PropertiesMapConverter() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * 将prop转为map[str, str]
     *
     * @param prop {@link Properties}
     * @return {@link Map}
     */
    public static Map<String, String> convert(@NonNull Properties prop) {
        Map<String, String> map = new HashMap<>();
        for (String k : prop.stringPropertyNames()) {
            map.put(k, prop.getProperty(k));
        }
        return map;
    }

    /**
     * 将map[str, str]转为prop
     *
     * @param map {@link Map}
     * @return {@link Properties}
     */
    public static Properties convert(@NonNull Map<String, String> map) {
        Properties prop = new Properties();
        for (String k : map.keySet()) {
            prop.setProperty(k, map.get(k));
        }
        return prop;
    }

}
