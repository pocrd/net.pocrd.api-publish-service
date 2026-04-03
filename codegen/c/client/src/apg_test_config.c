#include "apg_test_config.h"
#include <stdlib.h>
#include <string.h>
#include <stdio.h>

apg_test_config_t* apg_test_config_create(void) {
    apg_test_config_t *config = (apg_test_config_t*)calloc(1, sizeof(apg_test_config_t));
    if (!config) return NULL;
    
    // 设置默认值
    config->gateway_url = strdup(APG_DEFAULT_GATEWAY_URL);
    config->timeout_ms = APG_DEFAULT_TIMEOUT_MS;
    config->debug = false;
    config->resolve_port = 443;
    
    // 从环境变量加载
    apg_test_config_load_from_env(config);
    
    return config;
}

void apg_test_config_destroy(apg_test_config_t *config) {
    if (!config) return;
    
    free(config->gateway_url);
    free(config->client_cert_path);
    free(config->client_key_path);
    free(config->resolve_ip);
    free(config);
}

void apg_test_config_load_from_env(apg_test_config_t *config) {
    if (!config) return;
    
    const char *env;
    
    // GATEWAY_URL
    env = getenv("GATEWAY_URL");
    if (env && strlen(env) > 0) {
        free(config->gateway_url);
        config->gateway_url = strdup(env);
    }
    
    // CLIENT_CERT_PATH
    env = getenv("CLIENT_CERT_PATH");
    if (env && strlen(env) > 0) {
        free(config->client_cert_path);
        config->client_cert_path = strdup(env);
    }
    
    // CLIENT_KEY_PATH
    env = getenv("CLIENT_KEY_PATH");
    if (env && strlen(env) > 0) {
        free(config->client_key_path);
        config->client_key_path = strdup(env);
    }
    
    // RESOLVE_IP
    env = getenv("RESOLVE_IP");
    if (env && strlen(env) > 0) {
        free(config->resolve_ip);
        config->resolve_ip = strdup(env);
    }
    
    // RESOLVE_PORT
    env = getenv("RESOLVE_PORT");
    if (env && strlen(env) > 0) {
        config->resolve_port = atoi(env);
    }
    
    // DEBUG
    env = getenv("DEBUG");
    if (env && (strcmp(env, "1") == 0 || strcmp(env, "true") == 0 || 
                strcmp(env, "TRUE") == 0)) {
        config->debug = true;
    }
    
    // TIMEOUT_MS
    env = getenv("TIMEOUT_MS");
    if (env && strlen(env) > 0) {
        config->timeout_ms = atoi(env);
    }
}

void apg_test_config_set_gateway_url(apg_test_config_t *config, const char *url) {
    if (!config || !url) return;
    free(config->gateway_url);
    config->gateway_url = strdup(url);
}

void apg_test_config_set_client_cert(apg_test_config_t *config, const char *path) {
    if (!config) return;
    free(config->client_cert_path);
    config->client_cert_path = path ? strdup(path) : NULL;
}

void apg_test_config_set_client_key(apg_test_config_t *config, const char *path) {
    if (!config) return;
    free(config->client_key_path);
    config->client_key_path = path ? strdup(path) : NULL;
}

void apg_test_config_set_resolve(apg_test_config_t *config, const char *ip, int port) {
    if (!config) return;
    free(config->resolve_ip);
    config->resolve_ip = ip ? strdup(ip) : NULL;
    config->resolve_port = port > 0 ? port : 443;
}

void apg_test_config_set_debug(apg_test_config_t *config, bool debug) {
    if (!config) return;
    config->debug = debug;
}

void apg_test_config_set_timeout(apg_test_config_t *config, int timeout_ms) {
    if (!config) return;
    config->timeout_ms = timeout_ms > 0 ? timeout_ms : APG_DEFAULT_TIMEOUT_MS;
}

const char* apg_test_config_get_gateway_url(const apg_test_config_t *config) {
    return config ? config->gateway_url : NULL;
}

const char* apg_test_config_get_client_cert(const apg_test_config_t *config) {
    return config ? config->client_cert_path : NULL;
}

const char* apg_test_config_get_client_key(const apg_test_config_t *config) {
    return config ? config->client_key_path : NULL;
}

void apg_test_config_print(const apg_test_config_t *config) {
    if (!config) return;
    
    printf("========================================\n");
    printf("APG Test Configuration\n");
    printf("========================================\n");
    printf("Gateway URL:    %s\n", config->gateway_url ? config->gateway_url : "(not set)");
    printf("Client Cert:    %s\n", config->client_cert_path ? config->client_cert_path : "(not set)");
    printf("Client Key:     %s\n", config->client_key_path ? config->client_key_path : "(not set)");
    printf("Resolve IP:     %s\n", config->resolve_ip ? config->resolve_ip : "(not set)");
    printf("Resolve Port:   %d\n", config->resolve_port);
    printf("Debug Mode:     %s\n", config->debug ? "enabled" : "disabled");
    printf("Timeout:        %d ms\n", config->timeout_ms);
    printf("========================================\n");
}
