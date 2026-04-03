package com.pocrd.clientsdk;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * JSON 工具类
 * <p>
 * 提供基础类型的 JSON 序列化、列表解析和循环引用检测功能。
 * 实体类的序列化/反序列化由实体自身实现（toJson/fromJson）。
 */
public final class JsonUtil {

    private JsonUtil() {}

    /** ThreadLocal 用于检测循环引用（全局共享） */
    private static final ThreadLocal<java.util.Set<Object>> SERIALIZING_SET = ThreadLocal.withInitial(java.util.HashSet::new);

    /**
     * 检查是否正在序列化指定对象（用于循环引用检测）
     *
     * @param obj 要检查的对象
     * @return 如果该对象正在当前线程的序列化栈中，返回 true
     */
    public static boolean isSerializing(Object obj) {
        return SERIALIZING_SET.get().contains(obj);
    }

    /**
     * 将对象标记为正在序列化
     *
     * @param obj 要标记的对象
     */
    public static void beginSerialize(Object obj) {
        SERIALIZING_SET.get().add(obj);
    }

    /**
     * 移除对象的序列化标记
     *
     * @param obj 要移除标记的对象
     */
    public static void endSerialize(Object obj) {
        java.util.Set<Object> set = SERIALIZING_SET.get();
        set.remove(obj);
        // 清理 ThreadLocal 防止内存泄漏
        if (set.isEmpty()) {
            SERIALIZING_SET.remove();
        }
    }

    /**
     * 将对象序列化为 JSON 字符串
     * <p>
     * 支持基础类型、String、List/Set 集合。
     * 实体类请直接调用其 toJson() 方法，此方法会处理实体列表的序列化。
     */
    public static String toJson(Object obj) {
        if (obj == null) {
            return "null";
        }
        if (obj instanceof String) {
            return "\"" + escape((String) obj) + "\"";
        }
        if (obj instanceof Number || obj instanceof Boolean) {
            return obj.toString();
        }
        if (obj instanceof java.util.Collection<?>) {
            return collectionToJson((java.util.Collection<?>) obj);
        }
        // 其他类型（单个实体类）不应该走到这里
        throw new IllegalArgumentException("Unsupported type for JsonUtil.toJson: " + obj.getClass().getName() +
                ". Use entity.toJson() for entity objects.");
    }

    /**
     * 将集合序列化为 JSON 数组字符串
     */
    private static String collectionToJson(java.util.Collection<?> col) {
        StringBuilder sb = new StringBuilder("[");
        boolean first = true;
        for (Object item : col) {
            if (!first) sb.append(",");
            if (item == null) {
                sb.append("null");
            } else if (item instanceof String) {
                sb.append("\"").append(escape((String) item)).append("\"");
            } else if (item instanceof Number || item instanceof Boolean) {
                sb.append(item.toString());
            } else {
                // 实体类型，调用其 toJson() 方法
                try {
                    sb.append((String) item.getClass().getMethod("toJson").invoke(item));
                } catch (Exception e) {
                    throw new IllegalArgumentException("Failed to serialize item: " + item.getClass().getName(), e);
                }
            }
            first = false;
        }
        sb.append("]");
        return sb.toString();
    }

    /**
     * 转义字符串中的特殊字符
     */
    private static String escape(String s) {
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    /**
     * 解析 JSON 数组字符串为 List
     *
     * @param json   JSON 数组字符串，如 ["a","b","c"] 或 [{...},{...}]
     * @param parser 元素解析函数
     * @return 解析后的 List
     */
    public static <T> java.util.List<T> parseList(String json, java.util.function.Function<String, T> parser) {
        java.util.List<T> result = new java.util.ArrayList<>();
        if (json == null || json.isEmpty() || json.equals("null") || json.equals("[]")) {
            return result;
        }
        String content = json.trim();
        if (content.startsWith("[")) content = content.substring(1);
        if (content.endsWith("]")) content = content.substring(0, content.length() - 1);
        if (content.isEmpty()) return result;

        // 智能分割：处理嵌套的 JSON 对象和数组
        java.util.List<String> items = splitJsonArray(content);
        for (String item : items) {
            result.add(parser.apply(item.trim()));
        }
        return result;
    }

    /**
     * 从 JsonParser 直接解析 JSON 数组为 List（零拷贝方式）
     * <p>
     * 调用前 parser 应该已经定位到 START_ARRAY token
     * 
     * @param parser         JsonParser 实例
     * @param elementParser  元素解析函数，接收 JsonParser 并返回解析后的元素
     * @return 解析后的 List
     */
    public static <T> List<T> parseListFromJson(JsonParser parser, ThrowingFunction<JsonParser, T> elementParser) throws IOException {
        List<T> result = new ArrayList<>();
        JsonToken token = parser.currentToken();
        if (token != JsonToken.START_ARRAY) {
            throw new IllegalArgumentException("Expected START_ARRAY, got: " + token);
        }
        
        while ((token = parser.nextToken()) != JsonToken.END_ARRAY) {
            if (token == JsonToken.VALUE_NULL) {
                result.add(null);
            } else {
                result.add(elementParser.apply(parser));
            }
        }
        return result;
    }

    /**
     * 从 JsonParser 直接解析 JSON 数组中的字符串元素
     */
    public static List<String> parseStringListFromJson(JsonParser parser) throws IOException {
        List<String> result = new ArrayList<>();
        JsonToken token = parser.currentToken();
        if (token != JsonToken.START_ARRAY) {
            throw new IllegalArgumentException("Expected START_ARRAY, got: " + token);
        }
        
        while ((token = parser.nextToken()) != JsonToken.END_ARRAY) {
            if (token == JsonToken.VALUE_NULL) {
                result.add(null);
            } else {
                result.add(parser.getValueAsString());
            }
        }
        return result;
    }

    /**
     * 可抛出异常的 Function 接口
     */
    @FunctionalInterface
    public interface ThrowingFunction<T, R> {
        R apply(T t) throws IOException;
    }

    /**
     * 智能分割 JSON 数组元素，正确处理嵌套的对象和数组
     */
    private static java.util.List<String> splitJsonArray(String content) {
        java.util.List<String> result = new java.util.ArrayList<>();
        StringBuilder current = new StringBuilder();
        int braceDepth = 0;    // 大括号深度
        int bracketDepth = 0;  // 中括号深度
        boolean inString = false;
        char prevChar = 0;

        for (int i = 0; i < content.length(); i++) {
            char c = content.charAt(i);

            // 处理字符串转义
            if (c == '"' && prevChar != '\\') {
                inString = !inString;
            }

            if (!inString) {
                if (c == '{') braceDepth++;
                else if (c == '}') braceDepth--;
                else if (c == '[') bracketDepth++;
                else if (c == ']') bracketDepth--;
                else if (c == ',' && braceDepth == 0 && bracketDepth == 0) {
                    // 在顶层遇到逗号，分割元素
                    if (current.length() > 0) {
                        result.add(current.toString().trim());
                        current = new StringBuilder();
                    }
                    continue;
                }
            }

            current.append(c);
            prevChar = c;
        }

        // 添加最后一个元素
        if (current.length() > 0) {
            result.add(current.toString().trim());
        }

        return result;
    }
}
