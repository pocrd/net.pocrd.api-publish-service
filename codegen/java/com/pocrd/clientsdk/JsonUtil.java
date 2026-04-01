package com.pocrd.clientsdk;

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
     * @param json   JSON 数组字符串，如 ["a","b","c"] 或 [1,2,3]
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

        // 简单分割（注意：不支持嵌套数组或包含逗号的字符串）
        String[] items = content.split(",");
        for (String item : items) {
            result.add(parser.apply(item.trim()));
        }
        return result;
    }
}
