package com.pocrd.api_publish_service.sdk.util;

import com.pocrd.api_publish_service.sdk.annotation.ApiGroup;
import com.pocrd.api_publish_service.sdk.annotation.ApiParameter;
import com.pocrd.api_publish_service.sdk.annotation.Description;
import com.pocrd.api_publish_service.sdk.annotation.DesignedErrorCode;
import com.pocrd.api_publish_service.sdk.annotation.EnumDef;
import com.pocrd.api_publish_service.sdk.apidefine.ApiGroupInfo;
import com.pocrd.api_publish_service.sdk.apidefine.EntityDefinition;
import com.pocrd.api_publish_service.sdk.apidefine.ErrorCodeInfo;
import com.pocrd.api_publish_service.sdk.apidefine.FieldDefinition;
import com.pocrd.api_publish_service.sdk.apidefine.MethodDefinition;
import com.pocrd.api_publish_service.sdk.apidefine.ParameterDefinition;
import com.pocrd.api_publish_service.sdk.apidefine.ServiceDefinition;
import com.pocrd.api_publish_service.sdk.apidefine.ValidationError;
import com.pocrd.api_publish_service.sdk.apidefine.ValidationResult;
import com.pocrd.api_publish_service.sdk.entity.AbstractReturnCode;
import com.pocrd.api_publish_service.sdk.entity.ApiMetadataExtractException;
import com.pocrd.api_publish_service.sdk.entity.EnumNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * API 元数据验证器
 * 负责验证接口定义的正确性，收集所有错误信息而不是抛出异常
 */
public class ApiMetadataValidator {
    private static final Logger logger = LoggerFactory.getLogger(ApiMetadataValidator.class);

    // 基础类型映射表
    private static final Map<String, String> TYPE_MAPPING = Map.ofEntries(
        Map.entry("java.util.List", "list"),
        Map.entry("java.util.Set", "set"),
        Map.entry("org.apache.dubbo.common.stream.StreamObserver", "StreamObserver"),
        Map.entry("java.lang.String", "string"),
        Map.entry("java.lang.Integer", "int"),
        Map.entry("java.lang.Long", "long"),
        Map.entry("java.lang.Boolean", "bool"),
        Map.entry("int", "int"),
        Map.entry("long", "long"),
        Map.entry("boolean", "bool"),
        Map.entry("void", "void")
    );

    // 基础类型白名单
    private static final Set<String> TYPE_WHITELIST = Set.of(
        "java.lang.String",
        "java.lang.Integer", "int",
        "java.lang.Long", "long",
        "java.lang.Boolean", "boolean",
        "void"
    );

    // 容器类型白名单
    private static final Set<String> CONTAINER_WHITELIST = Set.of(
        "java.util.List",
        "java.util.Set",
        "org.apache.dubbo.common.stream.StreamObserver"
    );

    // 环境变量名称
    private static final String ENV_SERVICE_NAME = "API_PUBLISH_SERVICE_NAME";

    private final String serviceIdPrefix;
    private final ValidationResult result;
    private final Set<String> reportedErrors; // 用于去重
    
    // 扫描路径栈，用于记录当前扫描位置
    private final java.util.Deque<ScanFrame> scanStack = new java.util.ArrayDeque<>();

    /**
     * 扫描帧，记录扫描路径中的一个位置
     */
    private static class ScanFrame {
        final String interfaceName;
        final String methodName;
        final String parameterName;
        final String entityName;
        final String fieldName;
        final LocationType locationType;

        enum LocationType {
            INTERFACE, METHOD, METHOD_RETURN, PARAMETER, ENTITY, FIELD, ERROR_CODE
        }

        ScanFrame(String interfaceName, String methodName, String parameterName, 
                  String entityName, String fieldName, LocationType locationType) {
            this.interfaceName = interfaceName;
            this.methodName = methodName;
            this.parameterName = parameterName;
            this.entityName = entityName;
            this.fieldName = fieldName;
            this.locationType = locationType;
        }
        
        static ScanFrame ofInterface(String interfaceName) {
            return new ScanFrame(interfaceName, null, null, null, null, LocationType.INTERFACE);
        }
        
        static ScanFrame ofMethod(String interfaceName, String methodName) {
            return new ScanFrame(interfaceName, methodName, null, null, null, LocationType.METHOD);
        }
        
        static ScanFrame ofMethodReturn(String interfaceName, String methodName) {
            return new ScanFrame(interfaceName, methodName, null, null, null, LocationType.METHOD_RETURN);
        }
        
        static ScanFrame ofParameter(String interfaceName, String methodName, String parameterName) {
            return new ScanFrame(interfaceName, methodName, parameterName, null, null, LocationType.PARAMETER);
        }
        
        static ScanFrame ofEntity(String interfaceName, String methodName, String entityName) {
            return new ScanFrame(interfaceName, methodName, null, entityName, null, LocationType.ENTITY);
        }
        
        static ScanFrame ofField(String interfaceName, String methodName, String entityName, String fieldName) {
            return new ScanFrame(interfaceName, methodName, null, entityName, fieldName, LocationType.FIELD);
        }
        
        static ScanFrame ofErrorCode(String interfaceName, String fieldName) {
            return new ScanFrame(interfaceName, null, null, null, fieldName, LocationType.ERROR_CODE);
        }
    }

    /**
     * 创建验证器（自动从 Dubbo 配置或环境变量获取服务ID前缀）
     */
    public ApiMetadataValidator() {
        this(getServiceIdPrefixFromEnv());
    }

    /**
     * 创建验证器（指定服务ID前缀）
     */
    public ApiMetadataValidator(String serviceIdPrefix) {
        this.serviceIdPrefix = serviceIdPrefix;
        this.result = new ValidationResult();
        this.reportedErrors = new HashSet<>();
    }

    /**
     * 从环境变量或 Dubbo 配置获取服务ID前缀
     */
    private static String getServiceIdPrefixFromEnv() {
        String serviceName = System.getenv(ENV_SERVICE_NAME);
        if (serviceName == null || serviceName.isEmpty()) {
            serviceName = getDubboAppName();
        }
        if (serviceName == null || serviceName.isEmpty()) {
            throw new ApiMetadataExtractException(
                "无法从 Dubbo 配置中获取服务名称，请确保已正确配置 Dubbo ApplicationConfig"
            );
        }
        return formatServiceIdPrefix(serviceName);
    }

    /**
     * 从 Dubbo 配置中获取应用名称
     */
    private static String getDubboAppName() {
        try {
            java.util.Optional<org.apache.dubbo.config.ApplicationConfig> applicationOpt = 
                org.apache.dubbo.rpc.model.ApplicationModel.defaultModel().getApplicationConfigManager().getApplication();
            return applicationOpt.map(org.apache.dubbo.config.ApplicationConfig::getName).orElse(null);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 格式化服务 ID 前缀
     * 例如：api-publish-service -> ApiPublish
     */
    private static String formatServiceIdPrefix(String serviceName) {
        // 去掉 -service 后缀
        if (serviceName != null && serviceName.endsWith("-service")) {
            serviceName = serviceName.substring(0, serviceName.length() - 8);
        }
        
        // 将每个单词首字母大写，并移除连接符
        StringBuilder result = new StringBuilder();
        boolean capitalizeNext = true;
        for (char c : serviceName.toCharArray()) {
            if (c == '-') {
                capitalizeNext = true;
            } else if (capitalizeNext) {
                result.append(Character.toUpperCase(c));
                capitalizeNext = false;
            } else {
                result.append(c);
            }
        }
        
        return result.toString();
    }

    /**
     * 验证接口并返回结果
     */
    public ValidationResult validate(Class<?> interfaceClass) {
        if (interfaceClass == null) {
            addError(ValidationError.ErrorType.OTHER, "接口类不能为空");
            return result;
        }
        
        String interfaceName = interfaceClass.getName();
        scanStack.push(ScanFrame.ofInterface(interfaceName));
        
        if (!interfaceClass.isInterface()) {
            addError(ValidationError.ErrorType.OTHER, "必须是接口类型");
            scanStack.pop();
            return result;
        }

        // 检查 @ApiGroup
        ApiGroup apiGroup = interfaceClass.getAnnotation(ApiGroup.class);
        if (apiGroup == null) {
            // 没有@ApiGroup的接口不处理
            scanStack.pop();
            return result;
        }

        ApiGroupInfo apiGroupInfo = new ApiGroupInfo(
            apiGroup.name(),
            apiGroup.minCode(),
            apiGroup.maxCode(),
            apiGroup.codeDefine().getSimpleName()
        );

        // 检查 @Description
        String description = null;
        Description desc = interfaceClass.getAnnotation(Description.class);
        if (desc != null) {
            description = desc.value();
        } else {
            addError(ValidationError.ErrorType.MISSING_DESCRIPTION, "接口必须标记 @Description 注解");
        }

        // 提取错误码
        Set<Integer> allErrorCodes = new HashSet<>();
        List<ErrorCodeInfo> allErrorCodeInfos = extractErrorCodes(apiGroup.codeDefine(), allErrorCodes);

        // 收集实体类型
        Set<Class<?>> entityTypes = new HashSet<>();
        List<MethodDefinition> methods = new ArrayList<>();

        for (Method method : interfaceClass.getDeclaredMethods()) {
            MethodDefinition methodDef = extractMethodMetadata(method, entityTypes, allErrorCodes, apiGroup.codeDefine());
            if (methodDef != null) {
                methods.add(methodDef);
            }
        }

        // 收集实际使用的错误码
        Set<Integer> usedErrorCodes = new HashSet<>();
        for (MethodDefinition method : methods) {
            int[] methodErrorCodes = method.errorCodes();
            if (methodErrorCodes != null) {
                for (int code : methodErrorCodes) {
                    usedErrorCodes.add(code);
                }
            }
        }

        // 过滤错误码，只保留实际使用的
        List<ErrorCodeInfo> errorCodes = filterUsedErrorCodes(allErrorCodeInfos, usedErrorCodes);

        // 收集实体定义
        Map<String, EntityDefinition> entities = collectEntityDefinitions(entityTypes);
        
        scanStack.pop();

        ServiceDefinition serviceDef = new ServiceDefinition(
            interfaceName,
            apiGroupInfo,
            description,
            methods,
            entities,
            errorCodes,
            null,
            null
        );
        result.setServiceDefinition(serviceDef);

        return result;
    }

    /**
     * 添加错误（自动去重，从扫描栈获取路径信息）
     */
    private void addError(ValidationError.ErrorType errorType, String errorMessage) {
        // 从扫描栈顶获取当前路径信息
        ScanFrame frame = scanStack.peek();
        String interfaceName = frame != null ? frame.interfaceName : null;
        String methodName = frame != null ? frame.methodName : null;
        String parameterName = frame != null ? frame.parameterName : null;
        String entityName = frame != null ? frame.entityName : null;
        String fieldName = frame != null ? frame.fieldName : null;
        ScanFrame.LocationType locationType = frame != null ? frame.locationType : null;
        
        // 根据位置类型，在错误消息中添加明确的位置信息
        String locationPrefix = getLocationPrefix(locationType);
        String enhancedMessage = locationPrefix != null ? locationPrefix + errorMessage : errorMessage;
        
        // 生成错误指纹用于去重
        String fingerprint = String.join("|", 
            String.valueOf(interfaceName),
            String.valueOf(methodName),
            String.valueOf(parameterName),
            String.valueOf(entityName),
            String.valueOf(fieldName),
            errorType.name(),
            enhancedMessage
        );
        
        if (reportedErrors.contains(fingerprint)) {
            return; // 已报告过，跳过
        }
        reportedErrors.add(fingerprint);
        
        result.addError(interfaceName, methodName, parameterName, entityName, fieldName, errorType, enhancedMessage);
    }
    
    /**
     * 获取位置前缀，用于明确错误发生的位置
     */
    private String getLocationPrefix(ScanFrame.LocationType locationType) {
        if (locationType == null) {
            return null;
        }
        return switch (locationType) {
            case METHOD_RETURN -> "方法返回类型: ";
            case PARAMETER -> "参数: ";
            case FIELD -> "字段: ";
            case ERROR_CODE -> "错误码: ";
            default -> null;
        };
    }

    /**
     * 提取方法元数据
     */
    private MethodDefinition extractMethodMetadata(Method method, Set<Class<?>> entityTypes,
                                                    Set<Integer> allErrorCodes,
                                                    Class<? extends AbstractReturnCode> codeDefineClass) {
        String methodName = method.getName();
        scanStack.push(ScanFrame.ofMethod(scanStack.peek().interfaceName, methodName));

        Class<?> returnType = method.getReturnType();
        String returnContainerType = null;
        String returnTypeName;

        // 检查返回类型（包括数组，统一视为 List 处理）
        scanStack.push(ScanFrame.ofMethodReturn(scanStack.peek().interfaceName, methodName));
        java.lang.reflect.Type genericReturnType = method.getGenericReturnType();
        if (returnType.isArray()) {
            // 数组返回类型统一视为 List
            Class<?> compType = returnType.getComponentType();
            checkTypeArgument(compType, entityTypes);
            returnTypeName = compType.getName();
            returnContainerType = "java.util.List";
        } else if (genericReturnType instanceof java.lang.reflect.ParameterizedType parameterizedType) {
            Class<?> rawType = (Class<?>) parameterizedType.getRawType();
            if (!CONTAINER_WHITELIST.contains(rawType.getName())) {
                addError(ValidationError.ErrorType.UNSUPPORTED_CONTAINER,
                    "不支持的容器类型: " + rawType.getName());
                returnTypeName = returnType.getName();
            } else {
                // 检查泛型参数
                java.lang.reflect.Type[] typeArguments = parameterizedType.getActualTypeArguments();
                if (typeArguments.length == 1) {
                    checkTypeArgument(typeArguments[0], entityTypes);
                    returnTypeName = typeArguments[0].getTypeName();
                } else {
                    addError(ValidationError.ErrorType.UNSUPPORTED_TYPE,
                        "不支持的泛型返回类型: " + genericReturnType.getTypeName() + "，请使用具体类型");
                    returnTypeName = returnType.getName();
                }
                returnContainerType = rawType.getName();
            }
        } else if (genericReturnType instanceof java.lang.reflect.TypeVariable) {
            // 泛型类型变量（如 <T> T）
            addError(ValidationError.ErrorType.UNSUPPORTED_TYPE,
                "不支持的泛型返回类型: " + genericReturnType.getTypeName() + "，请使用具体类型");
            returnTypeName = returnType.getName();
        } else {
            checkType(returnType, entityTypes);
            returnTypeName = returnType.getName();
        }
        scanStack.pop();

        // 检查 @Description
        String description = null;
        Description methodDesc = method.getAnnotation(Description.class);
        if (methodDesc != null) {
            description = methodDesc.value();
        } else {
            addError(ValidationError.ErrorType.MISSING_DESCRIPTION, "方法必须标记 @Description 注解");
        }

        // 检查错误码
        int[] errorCodes = null;
        DesignedErrorCode errorCode = method.getAnnotation(DesignedErrorCode.class);
        if (errorCode != null) {
            errorCodes = errorCode.value();
            if (allErrorCodes != null) {
                for (int code : errorCodes) {
                    if (!allErrorCodes.contains(code)) {
                        addError(ValidationError.ErrorType.INVALID_ERROR_CODE,
                            "@DesignedErrorCode 中的错误码 " + code + " 不存在于 @ApiGroup 声明的错误码列表中（" + codeDefineClass.getName() + "）");
                    }
                }
            }
        }

        // 提取参数
        List<ParameterDefinition> params = new ArrayList<>();
        for (Parameter param : method.getParameters()) {
            ParameterDefinition paramDef = extractParameterMetadata(param, entityTypes);
            if (paramDef != null) {
                params.add(paramDef);
            }
        }
        
        scanStack.pop();
        return new MethodDefinition(methodName, returnTypeName, returnContainerType, description, errorCodes, params);
    }

    /**
     * 提取参数元数据
     */
    private ParameterDefinition extractParameterMetadata(Parameter param, Set<Class<?>> entityTypes) {
        String paramName = param.getName();
        ScanFrame parentFrame = scanStack.peek();
        scanStack.push(ScanFrame.ofParameter(parentFrame.interfaceName, parentFrame.methodName, paramName));
        
        Class<?> paramType = param.getType();
        String type = paramType.getName();
        String containerType = null;

        // 检查 @ApiParameter
        ApiParameter apiParam = param.getAnnotation(ApiParameter.class);
        if (apiParam == null) {
            addError(ValidationError.ErrorType.MISSING_API_PARAMETER, "参数必须标记 @ApiParameter 注解");
            scanStack.pop();
            return null;
        }

        // 处理泛型容器（包括数组，统一视为 List 处理）
        if (paramType.isArray() || param.getParameterizedType() instanceof java.lang.reflect.ParameterizedType) {
            Class<?> rawType;
            java.lang.reflect.Type[] typeArguments;
            
            if (paramType.isArray()) {
                // 数组类型统一视为 List
                rawType = java.util.List.class;
                typeArguments = new java.lang.reflect.Type[] { paramType.getComponentType() };
            } else {
                java.lang.reflect.ParameterizedType parameterizedType = 
                    (java.lang.reflect.ParameterizedType) param.getParameterizedType();
                rawType = (Class<?>) parameterizedType.getRawType();
                typeArguments = parameterizedType.getActualTypeArguments();
            }
            
            if (!CONTAINER_WHITELIST.contains(rawType.getName())) {
                addError(ValidationError.ErrorType.UNSUPPORTED_CONTAINER,
                    "不支持的容器类型: " + rawType.getName());
            } else {
                containerType = rawType.getName();
                if (typeArguments.length > 0) {
                    checkTypeArgument(typeArguments[0], entityTypes);
                    type = typeArguments[0].getTypeName();
                }
            }
        }
        // 普通类型
        else {
            checkType(paramType, entityTypes);
        }

        scanStack.pop();
        return new ParameterDefinition(
            paramName,
            simplifyType(type),
            containerType != null ? simplifyType(containerType) : null,
            apiParam.required(),
            isEmptyString(apiParam.verifyRegex()) ? null : apiParam.verifyRegex(),
            isEmptyString(apiParam.verifyMsg()) ? null : apiParam.verifyMsg(),
            apiParam.desc(),
            isEmptyString(apiParam.exampleValue()) ? null : apiParam.exampleValue(),
            apiParam.enumDef() != null && apiParam.enumDef() != EnumNull.class ? apiParam.enumDef().getName() : null,
            param.getAnnotation(Description.class) != null ? param.getAnnotation(Description.class).value() : null
        );
    }

    /**
     * 检查类型（基础类型和实体类型，不检查容器类型和数组类型）
     * 注意：数组类型应在调用方统一转为 List 处理，不应传入此方法
     */
    private void checkType(Class<?> type, Set<Class<?>> entityTypes) {
        if (type == null) {
            addError(ValidationError.ErrorType.UNSUPPORTED_TYPE, "类型为 null");
            return;
        }

        String typeName = type.getName();

        // 在白名单中 → 直接通过
        if (TYPE_WHITELIST.contains(typeName)) {
            return;
        }

        // 注意：容器类型和数组类型不在此处检查，应在调用方统一处理

        // 不在白名单中 → 检查是否有 @Description 注解
        if (type.getAnnotation(Description.class) == null) {
            addError(ValidationError.ErrorType.MISSING_DESCRIPTION, 
                "类型 " + typeName + " 缺少 @Description 注解，不是有效的自定义类型");
            return;
        }

        // 有 @Description 注解 → 添加到实体集合等待后续检查
        entityTypes.add(type);
    }

    /**
     * 检查泛型参数类型（递归处理嵌套泛型）
     */
    private void checkTypeArgument(java.lang.reflect.Type typeArg, Set<Class<?>> entityTypes) {
        // 处理嵌套泛型类型（如 List<List<String>> 中的 List<String>）
        if (typeArg instanceof java.lang.reflect.ParameterizedType nestedParamType) {
            Class<?> rawType = (Class<?>) nestedParamType.getRawType();
            // 禁止嵌套容器类型（即使是白名单中的容器类型也不允许嵌套）
            addError(ValidationError.ErrorType.UNSUPPORTED_CONTAINER,
                "不支持的嵌套容器类型: " + rawType.getName() + "，容器类型不支持嵌套使用");
            return;
        }
        
        // 处理泛型类型变量（如 <T>）
        if (typeArg instanceof java.lang.reflect.TypeVariable) {
            addError(ValidationError.ErrorType.UNSUPPORTED_TYPE,
                "不支持的泛型类型变量: " + typeArg.getTypeName() + "，请使用具体类型");
            return;
        }
        
        String typeName = typeArg.getTypeName();
        
        // 基础类型
        if (TYPE_WHITELIST.contains(typeName)) {
            return;
        }

        try {
            Class<?> clazz = Class.forName(typeName);
            checkType(clazz, entityTypes);
        } catch (ClassNotFoundException e) {
            // 可能是基本类型或数组类型，忽略
        }
    }

    /**
     * 收集实体定义
     */
    private Map<String, EntityDefinition> collectEntityDefinitions(Set<Class<?>> entityTypes) {
        Map<String, EntityDefinition> entities = new LinkedHashMap<>();
        Set<Class<?>> processedTypes = new HashSet<>();
        Set<Class<?>> pendingTypes = new HashSet<>(entityTypes);
        Map<String, String> simpleNameToFullName = new HashMap<>();
        String interfaceName = scanStack.peek().interfaceName;
        String methodName = scanStack.peek().methodName;

        while (!pendingTypes.isEmpty()) {
            Set<Class<?>> newTypes = new HashSet<>();
            for (Class<?> entityType : pendingTypes) {
                if (processedTypes.contains(entityType)) {
                    continue;
                }

                String simpleName = entityType.getSimpleName();
                String fullName = entityType.getName();

                // 检查类名冲突
                if (simpleNameToFullName.containsKey(simpleName)) {
                    scanStack.push(ScanFrame.ofEntity(interfaceName, methodName, fullName));
                    addError(ValidationError.ErrorType.ENTITY_NAME_CONFLICT,
                        "实体类名冲突：" + simpleName + " 被多个类使用：" + 
                        simpleNameToFullName.get(simpleName) + " 和 " + fullName);
                    scanStack.pop();
                    continue;
                }
                simpleNameToFullName.put(simpleName, fullName);

                EntityDefinition entityDef = extractEntityMetadata(entityType, newTypes, processedTypes);
                if (entityDef != null) {
                    entities.put(serviceIdPrefix + "_" + simpleName, entityDef);
                }
                processedTypes.add(entityType);
            }
            pendingTypes = newTypes;
        }

        return entities.isEmpty() ? null : entities;
    }

    /**
     * 提取实体元数据
     */
    private EntityDefinition extractEntityMetadata(Class<?> entityType, Set<Class<?>> nestedEntityTypes,
                                                    Set<Class<?>> processedTypes) {
        String entityName = entityType.getName();
        ScanFrame parentFrame = scanStack.peek();
        String interfaceName = parentFrame.interfaceName;
        String methodName = parentFrame.methodName;
        scanStack.push(ScanFrame.ofEntity(interfaceName, methodName, entityName));

        // 先检查 @Description - 如果没有 @Description，说明不是自定义实体类型
        Description classDesc = entityType.getAnnotation(Description.class);
        if (classDesc == null) {
            addError(ValidationError.ErrorType.MISSING_DESCRIPTION, "类型缺少 @Description 注解，不是有效的自定义类型");
            scanStack.pop();
            return null;
        }

        // 只有带 @Description 的自定义类型才需要是 record
        if (!entityType.isRecord()) {
            addError(ValidationError.ErrorType.NON_RECORD_ENTITY, "实体类必须使用record类型定义");
            scanStack.pop();
            return null;
        }

        List<FieldDefinition> fields = new ArrayList<>();
        for (Field field : entityType.getDeclaredFields()) {
            if (java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
                continue;
            }

            Class<?> fieldType = field.getType();
            String type = fieldType.getName();
            String containerType = null;

            // 处理数组和泛型容器（统一视为 List 处理）
            if (fieldType.isArray()) {
                containerType = "java.util.List";
                Class<?> compType = fieldType.getComponentType();
                type = compType.getName();
                // 检查数组元素类型
                if (!TYPE_WHITELIST.contains(type)) {
                    scanStack.push(ScanFrame.ofField(interfaceName, methodName, entityName, field.getName()));
                    checkType(compType, nestedEntityTypes);
                    scanStack.pop();
                }
            } else if (CONTAINER_WHITELIST.contains(fieldType.getName())) {
                // 处理泛型容器（如 List<T>, Set<T>）
                containerType = fieldType.getName();
                java.lang.reflect.Type genericType = field.getGenericType();
                if (genericType instanceof java.lang.reflect.ParameterizedType paramType) {
                    java.lang.reflect.Type[] typeArgs = paramType.getActualTypeArguments();
                    // 支持只包含一个类型参数的泛型容器
                    if (typeArgs.length == 1) {
                        java.lang.reflect.Type typeArg = typeArgs[0];
                        if (typeArg instanceof Class<?> compClass) {
                            type = compClass.getName();
                            if (!TYPE_WHITELIST.contains(type)) {
                                scanStack.push(ScanFrame.ofField(interfaceName, methodName, entityName, field.getName()));
                                checkType(compClass, nestedEntityTypes);
                                scanStack.pop();
                            }
                        }
                    }
                }
            } else if (!TYPE_WHITELIST.contains(type)) {
                // 检查普通字段类型
                scanStack.push(ScanFrame.ofField(interfaceName, methodName, entityName, field.getName()));
                checkType(fieldType, nestedEntityTypes);
                scanStack.pop();
            }

            // 检查字段 @Description
            Description fieldDesc = field.getAnnotation(Description.class);
            if (fieldDesc == null) {
                scanStack.push(ScanFrame.ofField(interfaceName, methodName, entityName, field.getName()));
                addError(ValidationError.ErrorType.MISSING_DESCRIPTION, "字段必须标记 @Description 注解");
                scanStack.pop();
            }

            String enumDef = null;
            EnumDef enumDefAnno = field.getAnnotation(EnumDef.class);
            if (enumDefAnno != null) {
                enumDef = enumDefAnno.value().getName();
            }

            fields.add(new FieldDefinition(
                field.getName(),
                simplifyType(type),
                containerType != null ? simplifyType(containerType) : null,
                fieldDesc != null ? fieldDesc.value() : null,
                enumDef
            ));
        }

        scanStack.pop();
        return new EntityDefinition(
            simplifyType(entityName),
            classDesc.value(),
            entityType.isRecord(),
            fields
        );
    }

    /**
     * 提取错误码
     */
    private List<ErrorCodeInfo> extractErrorCodes(Class<? extends AbstractReturnCode> codeDefineClass,
                                                   Set<Integer> allErrorCodes) {
        ApiGroup apiGroup = scanStack.peek().interfaceName != null ? 
            getInterfaceApiGroup(scanStack.peek().interfaceName) : null;
        if (apiGroup == null) {
            return null;
        }
        int minCode = apiGroup.minCode();
        int maxCode = apiGroup.maxCode();
        String interfaceName = scanStack.peek().interfaceName;
        
        try {
            Field[] fields = codeDefineClass.getDeclaredFields();
            Map<Integer, ErrorCodeInfo.ErrorCodeItem> codes = new HashMap<>();

            for (Field field : fields) {
                if (java.lang.reflect.Modifier.isStatic(field.getModifiers()) &&
                    java.lang.reflect.Modifier.isPublic(field.getModifiers()) &&
                    AbstractReturnCode.class.isAssignableFrom(field.getType())) {

                    field.setAccessible(true);
                    AbstractReturnCode returnCode = (AbstractReturnCode) field.get(null);
                    if (returnCode != null) {
                        int code = returnCode.getCode();

                        if (code < minCode || code > maxCode) {
                            scanStack.push(ScanFrame.ofErrorCode(interfaceName, field.getName()));
                            addError(ValidationError.ErrorType.INVALID_ERROR_CODE,
                                "错误码 " + code + " 超出范围 [" + minCode + ", " + maxCode + "]");
                            scanStack.pop();
                        } else if (allErrorCodes.contains(code)) {
                            scanStack.push(ScanFrame.ofErrorCode(interfaceName, field.getName()));
                            addError(ValidationError.ErrorType.INVALID_ERROR_CODE,
                                "错误码 " + code + " 与之前声明的错误码冲突");
                            scanStack.pop();
                        } else {
                            allErrorCodes.add(code);
                            codes.put(code, new ErrorCodeInfo.ErrorCodeItem(
                                field.getName(), code, returnCode.getDesc()));
                        }
                    }
                }
            }

            if (!codes.isEmpty()) {
                return List.of(new ErrorCodeInfo(new ArrayList<>(codes.values())));
            }
        } catch (IllegalAccessException e) {
            addError(ValidationError.ErrorType.OTHER, "提取错误码失败: " + e.getMessage());
        }
        return null;
    }
    
    private ApiGroup getInterfaceApiGroup(String interfaceName) {
        try {
            Class<?> clazz = Class.forName(interfaceName);
            return clazz.getAnnotation(ApiGroup.class);
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    /**
     * 过滤错误码，只保留实际使用的错误码
     */
    private List<ErrorCodeInfo> filterUsedErrorCodes(List<ErrorCodeInfo> allErrorCodeInfos,
                                                      Set<Integer> usedErrorCodes) {
        if (allErrorCodeInfos == null || allErrorCodeInfos.isEmpty()) {
            return null;
        }
        if (usedErrorCodes == null || usedErrorCodes.isEmpty()) {
            return null;
        }

        List<ErrorCodeInfo> result = new ArrayList<>();
        for (ErrorCodeInfo errorCodeInfo : allErrorCodeInfos) {
            if (errorCodeInfo.codes() == null || errorCodeInfo.codes().isEmpty()) {
                continue;
            }
            List<ErrorCodeInfo.ErrorCodeItem> filteredCodes = new ArrayList<>();
            for (ErrorCodeInfo.ErrorCodeItem codeItem : errorCodeInfo.codes()) {
                if (usedErrorCodes.contains(codeItem.code())) {
                    filteredCodes.add(codeItem);
                }
            }
            if (!filteredCodes.isEmpty()) {
                result.add(new ErrorCodeInfo(filteredCodes));
            }
        }

        return result.isEmpty() ? null : result;
    }

    /**
     * 简化类型名称（使用默认服务ID前缀）
     */
    public static String simplifyType(String typeName) {
        return simplifyType(typeName, getServiceIdPrefixFromEnv());
    }
    
    /**
     * 简化类型名称（指定服务ID前缀）
     */
    public static String simplifyType(String typeName, String serviceIdPrefix) {
        if (typeName == null || typeName.isEmpty()) {
            throw new IllegalArgumentException("类型名称不能为空");
        }
        if (TYPE_MAPPING.containsKey(typeName)) {
            return TYPE_MAPPING.get(typeName);
        }
        String simpleName = typeName.substring(typeName.lastIndexOf('.') + 1);
        // 检查简化后的名称是否也是基础类型（如 "String" -> "string"）
        String fullTypeName = "java.lang." + simpleName;
        if (TYPE_MAPPING.containsKey(fullTypeName)) {
            return TYPE_MAPPING.get(fullTypeName);
        }
        return serviceIdPrefix + "_" + simpleName;
    }

    private boolean isEmptyString(String str) {
        return str == null || str.isEmpty();
    }

    /**
     * 命令行入口点 - 用于部署时检查 API 子工程
     * 
     * 用法: java ApiMetadataValidator <api.jar路径>
     * 示例: java ApiMetadataValidator /path/to/api-publish-service-api-1.0.0.jar
     * 
     * 注意: 作为独立应用程序运行时，需要设置环境变量 API_PUBLISH_SERVICE_NAME
     *       例如: export API_PUBLISH_SERVICE_NAME=api-publish-service
     */
    public static void main(String[] args) {
        if (args.length < 1) {
            logger.error("用法: java ApiMetadataValidator <api.jar路径>");
            logger.error("示例: java ApiMetadataValidator /path/to/api-publish-service-api-1.0.0.jar");
            logger.error("\n注意: 作为独立应用程序运行时，需要设置环境变量 API_PUBLISH_SERVICE_NAME");
            logger.error("      例如: export API_PUBLISH_SERVICE_NAME=api-publish-service");
            System.exit(1);
        }

        // 检查环境变量
        String envServiceName = System.getenv(ENV_SERVICE_NAME);
        if (envServiceName == null || envServiceName.isEmpty()) {
            logger.warn("[警告] 环境变量 {} 未设置", ENV_SERVICE_NAME);
            logger.warn("       将尝试从 Dubbo 配置获取服务名称");
            logger.warn("       建议设置环境变量: export {}=<服务名>", ENV_SERVICE_NAME);
        } else {
            logger.info("[INFO] 使用环境变量 {}={}", ENV_SERVICE_NAME, envServiceName);
        }

        String jarPath = args[0];
        logger.info("[API-CHECK] 检查 API 子工程: {}", jarPath);
        
        try {
            // 创建类加载器加载 API jar
            URLClassLoader classLoader = new URLClassLoader(
                new URL[] { new File(jarPath).toURI().toURL() },
                ApiMetadataValidator.class.getClassLoader()
            );
            
            // 扫描 jar 中的所有类
            int checkedCount = 0;
            int apiGroupCount = 0;
            int totalErrorCount = 0;
            
            try (JarFile jarFile = new JarFile(jarPath)) {
                java.util.Enumeration<JarEntry> entries = jarFile.entries();
                
                while (entries.hasMoreElements()) {
                    JarEntry entry = entries.nextElement();
                    String entryName = entry.getName();
                    
                    // 只处理 .class 文件
                    if (!entryName.endsWith(".class")) {
                        continue;
                    }
                    
                    // 转换为类名
                    String className = entryName.replace('/', '.').substring(0, entryName.length() - 6);
                    
                    try {
                        Class<?> clazz = Class.forName(className, false, classLoader);
                        checkedCount++;
                        
                        // 检查是否是接口且声明了 @ApiGroup
                        if (clazz.isInterface() && clazz.isAnnotationPresent(ApiGroup.class)) {
                            apiGroupCount++;
                            logger.info("\n[API-CHECK] 发现 API 接口: {}", className);

                            // 为每个接口创建新的验证器实例，避免错误累积
                            ApiMetadataValidator validator = new ApiMetadataValidator();

                            // 验证接口
                            ValidationResult result = validator.validate(clazz);

                            if (result.hasErrors()) {
                                totalErrorCount += result.getErrorCount();
                                logger.error("  ✗ 发现 {} 个错误:", result.getErrorCount());
                                for (var error : result.getErrors()) {
                                    logger.error("    - {}", error.format());
                                }
                            } else {
                                ServiceDefinition serviceDef = result.getServiceDefinition();
                                if (serviceDef != null) {
                                    // 打印摘要信息
                                    logger.info("  ✓ 接口名: {}", serviceDef.interfaceName());
                                    if (serviceDef.apiGroup() != null) {
                                        logger.info("  ✓ API组: {}", serviceDef.apiGroup().name());
                                        logger.info("  ✓ 错误码范围: [{}, {}]", serviceDef.apiGroup().minCode(), serviceDef.apiGroup().maxCode());
                                    }
                                    logger.info("  ✓ 方法数: {}", (serviceDef.methods() != null ? serviceDef.methods().size() : 0));
                                    if (serviceDef.entities() != null && !serviceDef.entities().isEmpty()) {
                                        logger.info("  ✓ 实体类型数: {}", serviceDef.entities().size());
                                    }
                                    if (serviceDef.errorCodes() != null && !serviceDef.errorCodes().isEmpty()) {
                                        int totalCodes = serviceDef.errorCodes().stream()
                                            .mapToInt(ec -> ec.codes() != null ? ec.codes().size() : 0)
                                            .sum();
                                        logger.info("  ✓ 错误码数: {}", totalCodes);
                                    }
                                }
                            }
                        }
                    } catch (ClassNotFoundException | NoClassDefFoundError e) {
                        // 忽略无法加载的类
                    }
                }
            }
            
            // 打印总结
            logger.info("{}", "=".repeat(60));
            logger.info("[API-CHECK] 检查完成");
            logger.info("  扫描类数: {}", checkedCount);
            logger.info("  API接口数: {}", apiGroupCount);
            logger.info("  错误数: {}", totalErrorCount);
            logger.info("{}", "=".repeat(60));

            // 关闭类加载器
            classLoader.close();

            // 如果有错误，退出码非零
            if (totalErrorCount > 0) {
                System.exit(2);
            }

        } catch (Exception e) {
            logger.error("[API-CHECK] 检查失败: {}", e.getMessage(), e);
            System.exit(3);
        }
    }
}
