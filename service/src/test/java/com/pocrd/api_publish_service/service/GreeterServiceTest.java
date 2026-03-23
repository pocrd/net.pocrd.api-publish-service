package com.pocrd.api_publish_service.service;

import com.pocrd.api_publish_service.service.impl.GreeterServiceHttpExportImpl;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class GreeterServiceTest {

    private GreeterServiceHttpExportImpl greeterServiceImpl = new GreeterServiceHttpExportImpl();

    @Test
    public void testGreet() {
        // Test direct implementation
        String result = greeterServiceImpl.greet("World2");
        assertNotNull(result);
        assertTrue(result.contains("Hello World2"));
        System.out.println("Unary call result: " + result);
    }

    @Test
    public void testGreetWithEmptyName() {
        String result = greeterServiceImpl.greet("");
        assertNotNull(result);
        assertTrue(result.contains("Hello"));
        System.out.println("Empty name result: " + result);
    }
}