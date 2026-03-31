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

    // 基础类型映射表
    private static final Map<String, String> TYPE_MAPPING = Map.ofEntries(
        Map.entry("java.util.List", "list"),
        Map.entry("java.util.Set", "set"),
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
            addError(null, null, null, null, null, 
                ValidationError.ErrorType.OTHER, "接口类不能为空");
            return result;
        }
        if (!interfaceClass.isInterface()) {
            addError(interfaceClass.getName(), null, null, null, null,
                ValidationError.ErrorType.OTHER, "必须是接口类型");
            return result;
        }

        String interfaceName = interfaceClass.getName();

        // 检查 @ApiGroup
        ApiGroup apiGroup = interfaceClass.getAnnotation(ApiGroup.class);
        if (apiGroup == null) {
            // 没有@ApiGroup的接口不处理
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
            addError(interfaceName, null, null, null, null,
                ValidationError.ErrorType.MISSING_DESCRIPTION, "接口必须标记 @Description 注解");
        }

        // 提取错误码
        Set<Integer> allErrorCodes = new HashSet<>();
        List<ErrorCodeInfo> errorCodes = extractErrorCodes(apiGroup.codeDefine(), interfaceName, 
            apiGroup.minCode(), apiGroup.maxCode(), allErrorCodes);

        // 收集实体类型
        Set<Class<?>> entityTypes = new HashSet<>();
        List<MethodDefinition> methods = new ArrayList<>();

        for (Method method : interfaceClass.getDeclaredMethods()) {
            MethodDefinition methodDef = extractMethodMetadata(method, entityTypes, interfaceName, allErrorCodes, apiGroup.codeDefine());
            if (methodDef != null) {
                methods.add(methodDef);
            }
        }

        // 收集实体定义
        Map<String, EntityDefinition> entities = collectEntityDefinitions(entityTypes, interfaceName);

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
     * 添加错误（自动去重）
     */
    private void addError(String interfaceName, String methodName, String parameterName,
                         String entityName, String fieldName,
                         ValidationError.ErrorType errorType, String errorMessage) {
        // 生成错误指纹用于去重
        String fingerprint = String.join("|", 
            String.valueOf(interfaceName),
            String.valueOf(methodName),
            String.valueOf(parameterName),
            String.valueOf(entityName),
            String.valueOf(fieldName),
            errorType.name(),
            errorMessage
        );
        
        if (reportedErrors.contains(fingerprint)) {
            return; // 已报告过，跳过
        }
        reportedErrors.add(fingerprint);
        
        result.addError(interfaceName, methodName, parameterName, entityName, fieldName, errorType, errorMessage);
    }

    /**
     * 提取方法元数据
     */
    private MethodDefinition extractMethodMetadata(Method method, Set<Class<?>> entityTypes,
                                                    String interfaceName, Set<Integer> allErrorCodes,
                                                    Class<? extends AbstractReturnCode> codeDefineClass) {
        String methodName = method.getName();
        Class<?> returnType = method.getReturnType();

        // 检查返回类型
        java.lang.reflect.Type genericReturnType = method.getGenericReturnType();
        if (genericReturnType instanceof java.lang.reflect.ParameterizedType parameterizedType) {
            Class<?> rawType = (Class<?>) parameterizedType.getRawType();
            if (!isWhitelistedContainer(rawType)) {
                addError(interfaceName, methodName, null, null, null,
                    ValidationError.ErrorType.UNSUPPORTED_CONTAINER,
                    "不支持的容器类型: " + rawType.getName() + "，请使用白名单中的容器类型: " + CONTAINER_WHITELIST);
            } else {
                // 检查泛型参数
                java.lang.reflect.Type[] typeArguments = parameterizedType.getActualTypeArguments();
                if (typeArguments.length > 0) {
                    checkTypeArgument(typeArguments[0], entityTypes, interfaceName, methodName, null);
                }
            }
        } else {
            checkType(returnType, entityTypes, interfaceName, methodName, null);
        }

        // 检查 @Description
        String description = null;
        Description methodDesc = method.getAnnotation(Description.class);
        if (methodDesc != null) {
            description = methodDesc.value();
        } else {
            addError(interfaceName, methodName, null, null, null,
                ValidationError.ErrorType.MISSING_DESCRIPTION, "方法必须标记 @Description 注解");
        }

        // 检查错误码
        int[] errorCodes = null;
        DesignedErrorCode errorCode = method.getAnnotation(DesignedErrorCode.class);
        if (errorCode != null) {
            errorCodes = errorCode.value();
            if (allErrorCodes != null) {
                for (int code : errorCodes) {
                    if (!allErrorCodes.contains(code)) {
                        addError(interfaceName, methodName, null, null, null,
                            ValidationError.ErrorType.INVALID_ERROR_CODE,
                            "@DesignedErrorCode 中的错误码 " + code + " 不存在于 @ApiGroup 声明的错误码列表中（" + codeDefineClass.getName() + "）");
                    }
                }
            }
        }

        // 提取参数
        List<ParameterDefinition> params = new ArrayList<>();
        for (Parameter param : method.getParameters()) {
            ParameterDefinition paramDef = extractParameterMetadata(param, entityTypes, interfaceName, methodName);
            if (paramDef != null) {
                params.add(paramDef);
            }
        }

        return new MethodDefinition(methodName, returnType.getName(), description, errorCodes, params);
    }

    /**
     * 提取参数元数据
     */
    private ParameterDefinition extractParameterMetadata(Parameter param, Set<Class<?>> entityTypes,
                                                          String interfaceName, String methodName) {
        String paramName = param.getName();
        Class<?> paramType = param.getType();
        String type = paramType.getName();
        String containerType = null;

        // 检查 @ApiParameter
        ApiParameter apiParam = param.getAnnotation(ApiParameter.class);
        if (apiParam == null) {
            addError(interfaceName, methodName, paramName, null, null,
                ValidationError.ErrorType.MISSING_API_PARAMETER, "参数必须标记 @ApiParameter 注解");
            return null;
        }

        // 处理 StreamObserver
        if ("org.apache.dubbo.common.stream.StreamObserver".equals(paramType.getName())) {
            containerType = "StreamObserver";
            java.lang.reflect.Type genericType = param.getParameterizedType();
            if (genericType instanceof java.lang.reflect.ParameterizedType parameterizedType) {
                java.lang.reflect.Type[] typeArguments = parameterizedType.getActualTypeArguments();
                if (typeArguments.length > 0) {
                    checkTypeArgument(typeArguments[0], entityTypes, interfaceName, methodName, paramName);
                    type = typeArguments[0].getTypeName();
                }
            }
        }
        // 处理数组
        else if (paramType.isArray()) {
            containerType = "array";
            Class<?> compType = paramType.getComponentType();
            type = compType.getName();
            checkType(compType, entityTypes, interfaceName, methodName, paramName);
        }
        // 处理泛型容器
        else if (param.getParameterizedType() instanceof java.lang.reflect.ParameterizedType parameterizedType) {
            Class<?> rawType = (Class<?>) parameterizedType.getRawType();
            if (!isWhitelistedContainer(rawType)) {
                addError(interfaceName, methodName, paramName, null, null,
                    ValidationError.ErrorType.UNSUPPORTED_CONTAINER,
                    "不支持的容器类型: " + rawType.getName() + "，请使用白名单中的容器类型: " + CONTAINER_WHITELIST);
            } else {
                containerType = rawType.getSimpleName();
                java.lang.reflect.Type[] typeArguments = parameterizedType.getActualTypeArguments();
                if (typeArguments.length > 0) {
                    checkTypeArgument(typeArguments[0], entityTypes, interfaceName, methodName, paramName);
                    type = typeArguments[0].getTypeName();
                }
            }
        }
        // 普通类型
        else {
            checkType(paramType, entityTypes, interfaceName, methodName, paramName);
        }

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
     * 检查类型
     */
    private void checkType(Class<?> type, Set<Class<?>> entityTypes, String interfaceName, 
                          String methodName, String paramName) {
        if (type == null || type.isPrimitive()) {
            return;
        }

        String typeName = type.getName();

        // 基础类型白名单
        if (TYPE_WHITELIST.contains(typeName)) {
            return;
        }

        // 容器类型白名单
        if (CONTAINER_WHITELIST.contains(typeName)) {
            return;
        }

        // 数组类型
        if (type.isArray()) {
            checkType(type.getComponentType(), entityTypes, interfaceName, methodName, paramName);
            return;
        }

        // JDK 内部类型（不支持）
        if (typeName.startsWith("java.lang.") || typeName.startsWith("java.io.") || typeName.startsWith("java.time.")) {
            addError(interfaceName, methodName, paramName, null, null,
                ValidationError.ErrorType.UNSUPPORTED_TYPE,
                "不支持的 JDK 类型: " + typeName + "，请使用白名单中的类型或自定义实体类");
            return;
        }

        // 其他类型视为实体
        entityTypes.add(type);
    }

    /**
     * 检查泛型参数类型
     */
    private void checkTypeArgument(java.lang.reflect.Type typeArg, Set<Class<?>> entityTypes,
                                    String interfaceName, String methodName, String paramName) {
        String typeName = typeArg.getTypeName();
        
        // 基础类型
        if (TYPE_WHITELIST.contains(typeName)) {
            return;
        }

        try {
            Class<?> clazz = Class.forName(typeName);
            checkType(clazz, entityTypes, interfaceName, methodName, paramName);
        } catch (ClassNotFoundException e) {
            // 可能是基本类型或数组类型，忽略
        }
    }

    /**
     * 收集实体定义
     */
    private Map<String, EntityDefinition> collectEntityDefinitions(Set<Class<?>> entityTypes, String interfaceName) {
        Map<String, EntityDefinition> entities = new LinkedHashMap<>();
        Set<Class<?>> processedTypes = new HashSet<>();
        Set<Class<?>> pendingTypes = new HashSet<>(entityTypes);
        Map<String, String> simpleNameToFullName = new HashMap<>();

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
                    addError(interfaceName, null, null, fullName, null,
                        ValidationError.ErrorType.ENTITY_NAME_CONFLICT,
                        "实体类名冲突：" + simpleName + " 被多个类使用：" + 
                        simpleNameToFullName.get(simpleName) + " 和 " + fullName);
                    continue;
                }
                simpleNameToFullName.put(simpleName, fullName);

                EntityDefinition entityDef = extractEntityMetadata(entityType, newTypes, processedTypes, interfaceName);
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
                                                    Set<Class<?>> processedTypes, String interfaceName) {
        String entityName = entityType.getName();

        // 检查 @Description
        Description classDesc = entityType.getAnnotation(Description.class);
        if (classDesc == null) {
            addError(interfaceName, null, null, entityName, null,
                ValidationError.ErrorType.MISSING_DESCRIPTION, "实体类必须标记 @Description 注解");
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

            // 处理数组
            if (fieldType.isArray()) {
                containerType = "array";
                Class<?> compType = fieldType.getComponentType();
                type = compType.getName();
                if (isEntityType(compType) && !processedTypes.contains(compType)) {
                    nestedEntityTypes.add(compType);
                }
            } else if (isEntityType(fieldType) && !processedTypes.contains(fieldType)) {
                nestedEntityTypes.add(fieldType);
            }

            // 检查字段 @Description
            Description fieldDesc = field.getAnnotation(Description.class);
            if (fieldDesc == null) {
                addError(interfaceName, null, null, entityName, field.getName(),
                    ValidationError.ErrorType.MISSING_DESCRIPTION, "字段必须标记 @Description 注解");
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
                                                   String interfaceName, int minCode, int maxCode,
                                                   Set<Integer> allErrorCodes) {
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
                            addError(interfaceName, null, null, null, field.getName(),
                                ValidationError.ErrorType.INVALID_ERROR_CODE,
                                "错误码 " + code + " 超出范围 [" + minCode + ", " + maxCode + "]");
                        } else if (allErrorCodes.contains(code)) {
                            addError(interfaceName, null, null, null, field.getName(),
                                ValidationError.ErrorType.INVALID_ERROR_CODE,
                                "错误码 " + code + " 与之前声明的错误码冲突");
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
            addError(interfaceName, null, null, null, null,
                ValidationError.ErrorType.OTHER, "提取错误码失败: " + e.getMessage());
        }
        return null;
    }

    /**
     * 判断是否为实体类型
     */
    private boolean isEntityType(Class<?> type) {
        if (type == null || type.isPrimitive()) {
            return false;
        }
        String typeName = type.getName();
        if (TYPE_WHITELIST.contains(typeName) || CONTAINER_WHITELIST.contains(typeName)) {
            return false;
        }
        if (type.isArray()) {
            return isEntityType(type.getComponentType());
        }
        return !typeName.startsWith("java.");
    }

    /**
     * 检查是否在容器白名单
     */
    private boolean isWhitelistedContainer(Class<?> type) {
        return type != null && CONTAINER_WHITELIST.contains(type.getName());
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
            return typeName;
        }
        if (TYPE_MAPPING.containsKey(typeName)) {
            return TYPE_MAPPING.get(typeName);
        }
        if ("StreamObserver".equals(typeName) || "array".equals(typeName)) {
            return typeName;
        }
        String simpleName = typeName.substring(typeName.lastIndexOf('.') + 1);
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
            System.err.println("用法: java ApiMetadataValidator <api.jar路径>");
            System.err.println("示例: java ApiMetadataValidator /path/to/api-publish-service-api-1.0.0.jar");
            System.err.println("\n注意: 作为独立应用程序运行时，需要设置环境变量 API_PUBLISH_SERVICE_NAME");
            System.err.println("      例如: export API_PUBLISH_SERVICE_NAME=api-publish-service");
            System.exit(1);
        }
        
        // 检查环境变量
        String envServiceName = System.getenv(ENV_SERVICE_NAME);
        if (envServiceName == null || envServiceName.isEmpty()) {
            System.err.println("[警告] 环境变量 " + ENV_SERVICE_NAME + " 未设置");
            System.err.println("       将尝试从 Dubbo 配置获取服务名称");
            System.err.println("       建议设置环境变量: export " + ENV_SERVICE_NAME + "=<服务名>");
        } else {
            System.out.println("[INFO] 使用环境变量 " + ENV_SERVICE_NAME + "=" + envServiceName);
        }
        
        String jarPath = args[0];
        System.out.println("[API-CHECK] 检查 API 子工程: " + jarPath);
        
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
                            System.out.println("\n[API-CHECK] 发现 API 接口: " + className);
                            
                            // 为每个接口创建新的验证器实例，避免错误累积
                            ApiMetadataValidator validator = new ApiMetadataValidator();
                            
                            // 验证接口
                            ValidationResult result = validator.validate(clazz);
                            
                            if (result.hasErrors()) {
                                totalErrorCount += result.getErrorCount();
                                System.out.println("  ✗ 发现 " + result.getErrorCount() + " 个错误:");
                                for (var error : result.getErrors()) {
                                    System.err.println("    - " + error.format());
                                }
                            } else {
                                ServiceDefinition serviceDef = result.getServiceDefinition();
                                if (serviceDef != null) {
                                    // 打印摘要信息
                                    System.out.println("  ✓ 接口名: " + serviceDef.interfaceName());
                                    if (serviceDef.apiGroup() != null) {
                                        System.out.println("  ✓ API组: " + serviceDef.apiGroup().name());
                                        System.out.println("  ✓ 错误码范围: [" + serviceDef.apiGroup().minCode() + ", " + serviceDef.apiGroup().maxCode() + "]");
                                    }
                                    System.out.println("  ✓ 方法数: " + (serviceDef.methods() != null ? serviceDef.methods().size() : 0));
                                    if (serviceDef.entities() != null && !serviceDef.entities().isEmpty()) {
                                        System.out.println("  ✓ 实体类型数: " + serviceDef.entities().size());
                                    }
                                    if (serviceDef.errorCodes() != null && !serviceDef.errorCodes().isEmpty()) {
                                        int totalCodes = serviceDef.errorCodes().stream()
                                            .mapToInt(ec -> ec.codes() != null ? ec.codes().size() : 0)
                                            .sum();
                                        System.out.println("  ✓ 错误码数: " + totalCodes);
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
            System.out.println("\n" + "=".repeat(60));
            System.out.println("[API-CHECK] 检查完成");
            System.out.println("  扫描类数: " + checkedCount);
            System.out.println("  API接口数: " + apiGroupCount);
            System.out.println("  错误数: " + totalErrorCount);
            System.out.println("=".repeat(60));
            
            // 关闭类加载器
            classLoader.close();
            
            // 如果有错误，退出码非零
            if (totalErrorCount > 0) {
                System.exit(2);
            }
            
        } catch (Exception e) {
            System.err.println("[API-CHECK] 检查失败: " + e.getMessage());
            e.printStackTrace();
            System.exit(3);
        }
    }
}
