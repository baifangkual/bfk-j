package io.github.baifangkual.jlib.db.util;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;

/**
 * Properties Map Converter
 * Map[String, String] 与 Properties 相互转换的工具
 *
 * @author baifangkual
 * @since 2024/7/11 v0.0.7
 */
public final class PropMapc {

    private PropMapc() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * 将prop转为map[str, str]
     *
     * @param prop {@link Properties}
     * @return {@link Map}
     * @throws NullPointerException null prop
     */
    public static Map<String, String> convert(Properties prop) {
        Objects.requireNonNull(prop);
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
     * @throws NullPointerException null map
     */
    public static Properties convert(Map<String, String> map) {
        Objects.requireNonNull(map);
        Properties prop = new Properties();
        for (String k : map.keySet()) {
            prop.setProperty(k, map.get(k));
        }
        return prop;
    }

}
