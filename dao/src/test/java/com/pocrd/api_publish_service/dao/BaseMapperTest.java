package com.pocrd.api_publish_service.dao;

import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import java.io.IOException;
import java.io.Reader;

/**
 * Mapper 测试基类
 * 提供纯 MyBatis 的测试环境，不依赖 Spring
 */
public abstract class BaseMapperTest {

    protected SqlSessionFactory sqlSessionFactory;
    protected SqlSession sqlSession;

    @BeforeEach
    public void setUp() throws IOException {
        // 读取 MyBatis 配置文件
        String resource = "mybatis-test-config.xml";
        Reader reader = Resources.getResourceAsReader(resource);
        sqlSessionFactory = new SqlSessionFactoryBuilder().build(reader);
        reader.close();
        
        // 打开会话，自动提交关闭（由 @AfterEach 回滚）
        sqlSession = sqlSessionFactory.openSession(false); // false = 不自动提交
    }

    @AfterEach
    public void tearDown() {
        // 回滚事务，不提交任何更改
        if (sqlSession != null) {
            sqlSession.rollback();
            sqlSession.close();
        }
    }

    /**
     * 获取 Mapper 实例
     */
    protected <T> T getMapper(Class<T> mapperClass) {
        return sqlSession.getMapper(mapperClass);
    }
}
