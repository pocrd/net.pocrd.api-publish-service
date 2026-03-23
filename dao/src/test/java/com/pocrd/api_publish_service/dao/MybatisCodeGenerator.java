package com.pocrd.api_publish_service.dao;

import org.mybatis.generator.api.MyBatisGenerator;
import org.mybatis.generator.config.Configuration;
import org.mybatis.generator.config.Context;
import org.mybatis.generator.config.JDBCConnectionConfiguration;
import org.mybatis.generator.config.JavaClientGeneratorConfiguration;
import org.mybatis.generator.config.JavaModelGeneratorConfiguration;
import org.mybatis.generator.config.ModelType;
import org.mybatis.generator.config.SqlMapGeneratorConfiguration;
import org.mybatis.generator.config.TableConfiguration;
import org.mybatis.generator.internal.DefaultShellCallback;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * MyBatis 代码生成器
 * 根据数据库表结构自动生成 Entity、Mapper 接口和 XML 文件
 * 配置信息从 generator-config.properties 文件读取
 */
public class MybatisCodeGenerator {

    private static final String CONFIG_FILE = "generator-config.xml";
    private static final String PROJECT_ROOT = System.getProperty("user.dir");
    private static final String JAVA_SOURCE_DIR = PROJECT_ROOT + "/dao/src/main/java";
    private static final String RESOURCE_DIR = PROJECT_ROOT + "/dao/src/main/resources";

    private String dbUrl;
    private String dbUsername;
    private String dbPassword;
    private String dbDriver;
    private String basePackage;
    private List<TableMapping> tableMappings;

    public static void main(String[] args) throws Exception {
        MybatisCodeGenerator generator = new MybatisCodeGenerator();
        generator.loadConfig();
        generator.generate();
    }

    /**
     * 从 XML 配置文件加载配置
     */
    private void loadConfig() throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();

        Document doc;
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(CONFIG_FILE)) {
            if (is == null) {
                throw new RuntimeException("找不到配置文件: " + CONFIG_FILE);
            }
            doc = builder.parse(is);
        }

        doc.getDocumentElement().normalize();

        // 解析数据库配置
        Element databaseElem = (Element) doc.getElementsByTagName("database").item(0);
        dbUrl = getElementText(databaseElem, "url");
        dbUsername = getElementText(databaseElem, "username");
        dbPassword = getElementText(databaseElem, "password");
        dbDriver = getElementText(databaseElem, "driver");

        // 解析代码生成配置
        Element generatorElem = (Element) doc.getElementsByTagName("generator").item(0);
        basePackage = getElementText(generatorElem, "basePackage");

        // 解析表映射配置
        tableMappings = new ArrayList<>();
        Element tablesElem = (Element) doc.getElementsByTagName("tables").item(0);
        NodeList tableNodes = tablesElem.getElementsByTagName("table");
        for (int i = 0; i < tableNodes.getLength(); i++) {
            Element tableElem = (Element) tableNodes.item(i);
            String tableName = tableElem.getAttribute("tableName");
            String domainObjectName = tableElem.getAttribute("domainObjectName");
            tableMappings.add(new TableMapping(tableName, domainObjectName));
            System.out.println("  加载映射: " + tableName + " -> " + domainObjectName);
        }

        System.out.println("配置加载完成:");
        System.out.println("  数据库: " + dbUrl);
        System.out.println("  用户名: " + dbUsername);
        System.out.println("  基础包名: " + basePackage);
        System.out.println("  表映射数: " + tableMappings.size());
    }

    /**
     * 获取 XML 元素的文本内容
     */
    private String getElementText(Element parent, String tagName) {
        NodeList nodeList = parent.getElementsByTagName(tagName);
        if (nodeList.getLength() > 0) {
            return nodeList.item(0).getTextContent();
        }
        return "";
    }

    /**
     * 执行代码生成
     */
    public void generate() throws Exception {
        System.out.println("\n开始生成 MyBatis 代码...");
        System.out.println("项目根目录: " + PROJECT_ROOT);
        System.out.println("Java 源码目录: " + JAVA_SOURCE_DIR);
        System.out.println("资源目录: " + RESOURCE_DIR);

        // 清除旧的生成代码
        cleanAutogenDirectory();

        // 确保目录存在
        createDirectoryIfNotExists(JAVA_SOURCE_DIR);
        createDirectoryIfNotExists(RESOURCE_DIR + "/mapper");

        // 构建生成配置
        Configuration config = new Configuration();
        Context context = new Context(ModelType.FLAT);
        context.setId("mysql");
        context.setTargetRuntime("MyBatis3");

        // JDBC 连接配置
        JDBCConnectionConfiguration jdbcConfig = new JDBCConnectionConfiguration();
        jdbcConfig.setDriverClass(dbDriver);
        jdbcConfig.setConnectionURL(dbUrl);
        jdbcConfig.setUserId(dbUsername);
        jdbcConfig.setPassword(dbPassword);
        context.setJdbcConnectionConfiguration(jdbcConfig);

        // Java 模型生成配置 (Entity)
        JavaModelGeneratorConfiguration modelConfig = new JavaModelGeneratorConfiguration();
        modelConfig.setTargetProject(JAVA_SOURCE_DIR);
        modelConfig.setTargetPackage(basePackage + ".entity");
        modelConfig.addProperty("enableSubPackages", "true");
        modelConfig.addProperty("trimStrings", "true");
        context.setJavaModelGeneratorConfiguration(modelConfig);

        // SQL Map 生成配置 (XML)
        SqlMapGeneratorConfiguration sqlMapConfig = new SqlMapGeneratorConfiguration();
        sqlMapConfig.setTargetProject(RESOURCE_DIR);
        sqlMapConfig.setTargetPackage("autogen.mapper");
        sqlMapConfig.addProperty("enableSubPackages", "true");
        context.setSqlMapGeneratorConfiguration(sqlMapConfig);

        // Java Client 生成配置 (Mapper 接口)
        JavaClientGeneratorConfiguration clientConfig = new JavaClientGeneratorConfiguration();
        clientConfig.setTargetProject(JAVA_SOURCE_DIR);
        clientConfig.setTargetPackage(basePackage + ".mapper");
        clientConfig.setConfigurationType("XMLMAPPER");
        clientConfig.addProperty("enableSubPackages", "true");
        context.setJavaClientGeneratorConfiguration(clientConfig);

        // 添加表配置
        for (TableMapping mapping : tableMappings) {
            addTableConfiguration(context, mapping.tableName, mapping.domainObjectName);
        }

        config.addContext(context);

        // 执行代码生成
        DefaultShellCallback callback = new DefaultShellCallback(true);
        MyBatisGenerator generator = new MyBatisGenerator(config, callback, new ArrayList<>());
        generator.generate(null);

        System.out.println("\nMyBatis 代码生成完成！");
        System.out.println("生成文件位置:");
        System.out.println("  - Entity: " + JAVA_SOURCE_DIR + "/" + basePackage.replace(".", "/") + "/entity/");
        System.out.println("  - Mapper 接口: " + JAVA_SOURCE_DIR + "/" + basePackage.replace(".", "/") + "/mapper/");
        System.out.println("  - Mapper XML: " + RESOURCE_DIR + "/autogen/mapper/");
    }

    /**
     * 添加表配置
     */
    private static void addTableConfiguration(Context context, String tableName, String domainObjectName) {
        TableConfiguration tableConfig = new TableConfiguration(context);
        tableConfig.setTableName(tableName);
        tableConfig.setDomainObjectName(domainObjectName);
        tableConfig.setMapperName(domainObjectName + "Mapper");
        
        // 使用实际列名，不添加下划线转驼峰
        tableConfig.addProperty("useActualColumnNames", "false");
        
        // 生成所有 CRUD 方法
        tableConfig.setSelectByExampleStatementEnabled(true);
        tableConfig.setUpdateByExampleStatementEnabled(true);
        tableConfig.setDeleteByExampleStatementEnabled(true);
        tableConfig.setCountByExampleStatementEnabled(true);
        
        // 配置自增主键，使 insert 方法返回生成的主键值
        tableConfig.setGeneratedKey(new org.mybatis.generator.config.GeneratedKey("id", "mysql", true, null));
        
        context.addTableConfiguration(tableConfig);
        System.out.println("已添加表配置: " + tableName + " -> " + domainObjectName);
    }

    /**
     * 清除 autogen 目录下的旧代码
     */
    private void cleanAutogenDirectory() {
        // 清除 Java 源码中的 autogen 目录
        String autogenJavaPath = JAVA_SOURCE_DIR + "/" + basePackage.replace(".", "/");
        File autogenJavaDir = new File(autogenJavaPath);
        if (autogenJavaDir.exists()) {
            deleteDirectory(autogenJavaDir);
            System.out.println("已清除旧 Java 代码目录: " + autogenJavaPath);
        }
        
        // 清除 resources 中的 autogen 目录（包含 Mapper XML）
        String autogenResourcePath = RESOURCE_DIR + "/autogen";
        File autogenResourceDir = new File(autogenResourcePath);
        if (autogenResourceDir.exists()) {
            deleteDirectory(autogenResourceDir);
            System.out.println("已清除旧资源目录: " + autogenResourcePath);
        }
    }

    /**
     * 递归删除目录
     */
    private static void deleteDirectory(File dir) {
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteDirectory(file);
                } else {
                    file.delete();
                }
            }
        }
        dir.delete();
    }

    /**
     * 创建目录（如果不存在）
     */
    private static void createDirectoryIfNotExists(String path) {
        File dir = new File(path);
        if (!dir.exists()) {
            boolean created = dir.mkdirs();
            if (created) {
                System.out.println("创建目录: " + path);
            }
        }
    }

    /**
     * 表名与实体名映射关系
     */
    private static class TableMapping {
        final String tableName;
        final String domainObjectName;

        TableMapping(String tableName, String domainObjectName) {
            this.tableName = tableName;
            this.domainObjectName = domainObjectName;
        }
    }
}
