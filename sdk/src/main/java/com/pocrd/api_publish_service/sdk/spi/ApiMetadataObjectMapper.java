package com.pocrd.api_publish_service.sdk.spi;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.pocrd.api_publish_service.sdk.apidefine.EntityDefinition;
import com.pocrd.api_publish_service.sdk.apidefine.FieldDefinition;
import com.pocrd.api_publish_service.sdk.apidefine.MethodDefinition;
import com.pocrd.api_publish_service.sdk.apidefine.ParameterDefinition;
import com.pocrd.api_publish_service.sdk.entity.EnumNull;
import com.pocrd.api_publish_service.sdk.util.ApiMetadataValidator;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * API 元数据专用的 ObjectMapper 工厂
 * 
 * 序列化规则：
 * 1. 省略 null 字段
 * 2. 省略 boolean 类型的 false 字段
 * 3. 过滤静态字段（如 serialVersionUID）
 * 4. 类型名称映射（简化实体类名）
 * 5. 循环依赖检测
 * 6. EnumNull 视为 null，展开真实枚举值
 */
public class ApiMetadataObjectMapper {

    // EnumNull 的完整类名
    private static final String ENUM_NULL_CLASS = EnumNull.class.getName();
    
    // 枚举值缓存
    private static final Map<String, List<String>> ENUM_VALUES_CACHE = new ConcurrentHashMap<>();
    
    /**
     * 创建 API 元数据专用的 ObjectMapper
     */
    public static ObjectMapper create() {
        ObjectMapper mapper = new ObjectMapper();
        
        // 有序序列化
        mapper.configure(com.fasterxml.jackson.databind.MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true);
        mapper.configure(com.fasterxml.jackson.databind.SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
        
        // 省略 null 字段
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        
        // 注册自定义模块
        SimpleModule module = new SimpleModule("ApiMetadataModule");
        module.addSerializer(FieldDefinition.class, new FieldDefinitionSerializer());
        module.addSerializer(ParameterDefinition.class, new ParameterDefinitionSerializer());
        module.addSerializer(MethodDefinition.class, new MethodDefinitionSerializer());
        module.addSerializer(EntityDefinition.class, new EntityDefinitionSerializer());
        mapper.registerModule(module);
        
        return mapper;
    }

    /**
     * 检查 enumDef 是否有效（不为 null 且不是 EnumNull）
     */
    private static boolean isValidEnumDef(String enumDef) {
        return enumDef != null && !enumDef.isEmpty() && !enumDef.equals(ENUM_NULL_CLASS);
    }
    
    /**
     * 提取枚举类的所有值
     */
    private static List<String> extractEnumValues(String enumClassName) {
        return ENUM_VALUES_CACHE.computeIfAbsent(enumClassName, className -> {
            try {
                Class<?> clazz = Class.forName(className);
                if (clazz.isEnum()) {
                    return Arrays.stream(clazz.getEnumConstants())
                        .map(Object::toString)
                        .toList();
                }
            } catch (ClassNotFoundException e) {
                System.err.println("[METADATA] Enum class not found: " + className);
            }
            return null;
        });
    }

    /**
     * FieldDefinition 自定义序列化器
     * 
     * 规则：
     * - 省略 null 字段
     * - containerType 为空时省略
     */
    private static class FieldDefinitionSerializer extends JsonSerializer<FieldDefinition> {
        @Override
        public void serialize(FieldDefinition value, JsonGenerator gen, SerializerProvider serializers) 
                throws IOException {
            gen.writeStartObject();
            
            // name: 必须输出
            gen.writeStringField("name", value.name());
            
            // type: 已简化的类型名
            gen.writeStringField("type", value.type());
            
            // containerType: 非空时输出（已简化的类型名）
            if (value.containerType() != null && !value.containerType().isEmpty()) {
                gen.writeStringField("containerType", value.containerType());
            }
            
            // desc: 非 null 时输出
            if (value.desc() != null) {
                gen.writeStringField("desc", value.desc());
            }
            
            // enumDef: EnumNull 视为 null，否则展开枚举值
            writeEnumDef(value.enumDef(), gen);
            
            gen.writeEndObject();
        }
    }

    /**
     * EntityDefinition 自定义序列化器
     * 
     * 规则：
     * - 过滤掉 null 字段
     */
    private static class EntityDefinitionSerializer extends JsonSerializer<EntityDefinition> {
        @Override
        public void serialize(EntityDefinition value, JsonGenerator gen, SerializerProvider serializers) 
                throws IOException {
            gen.writeStartObject();
            
            // type: 已简化的类型名（{serviceIdPrefix}_{simpleName}）
            gen.writeStringField("type", value.type());
            
            // isRecord: 只有为 true 时才输出
            if (value.isRecord()) {
                gen.writeBooleanField("isRecord", true);
            }
            
            // fields: 输出
            if (value.fields() != null && !value.fields().isEmpty()) {
                gen.writeArrayFieldStart("fields");
                for (var field : value.fields()) {
                    serializers.defaultSerializeValue(field, gen);
                }
                gen.writeEndArray();
            }
            
            gen.writeEndObject();
        }
    }
    
    /**
     * ParameterDefinition 自定义序列化器
     * 
     * 规则：
     * - 省略 null 字段
     * - containerType 为空时省略
     * - EnumNull 视为 null，展开真实枚举值
     */
    private static class ParameterDefinitionSerializer extends JsonSerializer<ParameterDefinition> {
        @Override
        public void serialize(ParameterDefinition value, JsonGenerator gen, SerializerProvider serializers) 
                throws IOException {
            gen.writeStartObject();
            
            // name: 必须输出
            gen.writeStringField("name", value.name());
            
            // type: 已简化的类型名
            gen.writeStringField("type", value.type());
            
            // containerType: 非空时输出（已简化的类型名）
            if (value.containerType() != null && !value.containerType().isEmpty()) {
                gen.writeStringField("containerType", value.containerType());
            }
            
            // required: 只有为 true 时才输出
            if (value.required()) {
                gen.writeBooleanField("required", true);
            }
            
            // verifyRegex: 非 null 时输出
            if (value.verifyRegex() != null) {
                gen.writeStringField("verifyRegex", value.verifyRegex());
            }
            
            // verifyMsg: 非 null 时输出
            if (value.verifyMsg() != null) {
                gen.writeStringField("verifyMsg", value.verifyMsg());
            }
            
            // desc: 非 null 时输出
            if (value.desc() != null) {
                gen.writeStringField("desc", value.desc());
            }
            
            // exampleValue: 非 null 时输出
            if (value.exampleValue() != null) {
                gen.writeStringField("exampleValue", value.exampleValue());
            }
            
            // enumDef: EnumNull 视为 null，否则展开枚举值
            writeEnumDef(value.enumDef(), gen);
            
            // description: 非 null 时输出
            if (value.description() != null) {
                gen.writeStringField("description", value.description());
            }
            
            gen.writeEndObject();
        }
    }
    
    /**
     * MethodDefinition 自定义序列化器
     * 
     * 规则：
     * - 省略 null 字段
     * - returnType 简化类型名
     */
    private static class MethodDefinitionSerializer extends JsonSerializer<MethodDefinition> {
        @Override
        public void serialize(MethodDefinition value, JsonGenerator gen, SerializerProvider serializers) 
                throws IOException {
            gen.writeStartObject();
            
            // name: 必须输出
            gen.writeStringField("name", value.name());
            
            // returnType: 简化类型名
            gen.writeStringField("returnType", ApiMetadataValidator.simplifyType(value.returnType()));
            
            // description: 非 null 时输出
            if (value.description() != null) {
                gen.writeStringField("description", value.description());
            }
            
            // errorCodes: 非空时输出
            if (value.errorCodes() != null && value.errorCodes().length > 0) {
                gen.writeArrayFieldStart("errorCodes");
                for (int errorCode : value.errorCodes()) {
                    gen.writeNumber(errorCode);
                }
                gen.writeEndArray();
            }
            
            // parameters: 非空时输出
            if (value.parameters() != null && !value.parameters().isEmpty()) {
                gen.writeArrayFieldStart("parameters");
                for (var param : value.parameters()) {
                    serializers.defaultSerializeValue(param, gen);
                }
                gen.writeEndArray();
            }
            
            gen.writeEndObject();
        }
    }
    
    /**
     * 写入 enumDef 字段（EnumNull 视为 null，否则展开为 enumValues 数组）
     */
    private static void writeEnumDef(String enumDef, JsonGenerator gen) throws IOException {
        if (isValidEnumDef(enumDef)) {
            List<String> enumValues = extractEnumValues(enumDef);
            if (enumValues != null && !enumValues.isEmpty()) {
                gen.writeArrayFieldStart("enumValues");
                for (String enumValue : enumValues) {
                    gen.writeString(enumValue);
                }
                gen.writeEndArray();
            }
        }
    }
}
