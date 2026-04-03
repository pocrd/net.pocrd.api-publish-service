/**
 * Greet API Test - 参考 Java 的 HttpsGreet2Test
 * 
 * 测试 greet 和 greet2 API 调用
 */

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <assert.h>
#include "apg_http_client.h"
#include "apg_test_config.h"
#include "apg_return_code.h"
#include "greeterservice_greet.h"
#include "greeterservice_greet2.h"

/* 测试结果宏 */
#define TEST_ASSERT(cond, msg) do { \
    if (!(cond)) { \
        printf("  [FAIL] %s (line %d)\n", msg, __LINE__); \
        return 1; \
    } else { \
        printf("  [PASS] %s\n", msg); \
    } \
} while(0)

/**
 * 测试 greet API
 */
int test_greet(apg_http_client_t *client, const char *gateway_url) {
    printf("\n[Test] Greet API\n");
    printf("----------------\n");
    
    // 创建 API 方法对象
    GreeterService_Greet_t *api = greeterservice_greet_create();
    TEST_ASSERT(api != NULL, "API method created");
    
    // 创建请求
    GreeterService_Greet_request_t *req = greeterservice_greet_request_create();
    TEST_ASSERT(req != NULL, "Request created");
    
    req->name = strdup("World");
    
    // 构建请求体（使用生成的函数）
    char *body = greeterservice_greet_build_request_body(req);
    TEST_ASSERT(body != NULL, "Request body built");
    printf("  Request body: %s\n", body);
    
    // 构建完整 URL（使用生成的函数）
    char *url = greeterservice_greet_build_url(gateway_url);
    TEST_ASSERT(url != NULL, "URL built");
    printf("  URL: %s\n", url);
    
    // 发送请求（使用完整 URL）
    if (client) {
        apg_http_response_t *resp = apg_http_request(client, url, api->http_method, 
                                                      body, "application/json");
        
        if (resp) {
            printf("  Response status: %d\n", resp->status_code);
            if (resp->body) {
                printf("  Response body: %s\n", resp->body);
            }
            
            TEST_ASSERT(apg_http_response_is_success(resp), "Request successful");
            apg_http_response_free(resp);
        }
    }
    
    // 清理
    free(body);
    free(url);
    greeterservice_greet_request_destroy(req);
    greeterservice_greet_destroy(api);
    
    return 0;
}

/**
 * 测试 greet2 API
 */
int test_greet2(apg_http_client_t *client, const char *gateway_url) {
    printf("\n[Test] Greet2 API\n");
    printf("-----------------\n");
    
    // 创建 API 方法对象
    GreeterService_Greet2_t *api = greeterservice_greet2_create();
    TEST_ASSERT(api != NULL, "API method created");
    
    // 创建请求
    GreeterService_Greet2_request_t *req = greeterservice_greet2_request_create();
    TEST_ASSERT(req != NULL, "Request created");
    
    req->name1 = strdup("Hello");
    req->name2 = strdup("World");
    
    // 构建请求体（使用生成的函数）
    char *body = greeterservice_greet2_build_request_body(req);
    TEST_ASSERT(body != NULL, "Request body built");
    printf("  Request body: %s\n", body);
    
    // 构建完整 URL（使用生成的函数）
    char *url = greeterservice_greet2_build_url(gateway_url);
    TEST_ASSERT(url != NULL, "URL built");
    printf("  URL: %s\n", url);
    
    // 发送请求（使用完整 URL）
    if (client) {
        apg_http_response_t *resp = apg_http_request(client, url, api->http_method,
                                                      body, "application/json");
        
        if (resp) {
            printf("  Response status: %d\n", resp->status_code);
            if (resp->body) {
                printf("  Response body: %s\n", resp->body);
            }
            
            TEST_ASSERT(apg_http_response_is_success(resp), "Request successful");
            apg_http_response_free(resp);
        }
    }
    
    // 清理
    free(body);
    free(url);
    greeterservice_greet2_request_destroy(req);
    greeterservice_greet2_destroy(api);
    
    return 0;
}

/**
 * 测试错误码
 */
int test_error_codes() {
    printf("\n[Test] Error Codes\n");
    printf("------------------\n");
    
    // 测试错误码结构
    apg_return_code_t code = GREET_NAME_TOO_LONG;
    TEST_ASSERT(code.code == 1001, "Error code value correct");
    TEST_ASSERT(strcmp(code.name, "GREET_NAME_TOO_LONG") == 0, "Error code name correct");
    
    // 测试辅助函数
    const char *name = apg_return_code_name(1001);
    TEST_ASSERT(strcmp(name, "GREET_NAME_TOO_LONG") == 0, "apg_return_code_name works");
    
    const char *msg = apg_return_code_message(1001);
    TEST_ASSERT(msg != NULL, "apg_return_code_message works");
    
    TEST_ASSERT(apg_return_code_is_error(1001) == true, "is_error works");
    TEST_ASSERT(apg_return_code_is_success(0) == true, "is_success works");
    
    return 0;
}

/**
 * 打印使用说明
 */
void print_usage(const char *prog) {
    printf("Usage: %s [options]\n", prog);
    printf("\nOptions:\n");
    printf("  -h, --help          Show this help\n");
    printf("  -r, --remote        Test remote server (default: local via --resolve)\n");
    printf("  --no-http           Skip HTTP tests (only test request building)\n");
    printf("  --cert PATH         Client certificate path\n");
    printf("  --key PATH          Client private key path\n");
    printf("  --resolve IP:PORT   Resolve hostname to IP:PORT\n");
    printf("  --debug             Enable debug output\n");
    printf("\nEnvironment Variables:\n");
    printf("  GATEWAY_URL         Gateway URL (default: https://api.caringfamily.cn)\n");
    printf("  CLIENT_CERT_PATH    Client certificate path\n");
    printf("  CLIENT_KEY_PATH     Client private key path\n");
    printf("  RESOLVE_IP          Resolve IP address\n");
    printf("  RESOLVE_PORT        Resolve port\n");
    printf("  DEBUG               Enable debug mode (1/true)\n");
}

int main(int argc, char *argv[]) {
    printf("========================================\n");
    printf("APG C SDK Greet Test\n");
    printf("Reference: HttpsGreet2Test.java\n");
    printf("========================================\n");
    
    // 解析参数
    bool no_http = false;
    bool is_remote = false;
    char *cert_path = NULL;
    char *key_path = NULL;
    char *resolve_ip = "127.0.0.1";  // 默认本地
    int resolve_port = 30443;         // 默认本地端口
    bool debug = false;
    
    for (int i = 1; i < argc; i++) {
        if (strcmp(argv[i], "-h") == 0 || strcmp(argv[i], "--help") == 0) {
            print_usage(argv[0]);
            return 0;
        } else if (strcmp(argv[i], "-r") == 0 || strcmp(argv[i], "--remote") == 0) {
            is_remote = true;
        } else if (strcmp(argv[i], "--no-http") == 0) {
            no_http = true;
        } else if (strcmp(argv[i], "--cert") == 0 && i + 1 < argc) {
            cert_path = argv[++i];
        } else if (strcmp(argv[i], "--key") == 0 && i + 1 < argc) {
            key_path = argv[++i];
        } else if (strcmp(argv[i], "--resolve") == 0 && i + 1 < argc) {
            char *resolve = argv[++i];
            char *colon = strrchr(resolve, ':');
            if (colon) {
                *colon = '\0';
                resolve_ip = resolve;
                resolve_port = atoi(colon + 1);
            } else {
                resolve_ip = resolve;
            }
        } else if (strcmp(argv[i], "--debug") == 0) {
            debug = true;
        }
    }
    
    // 远程模式禁用 resolve
    if (is_remote) {
        resolve_ip = NULL;
    }
    
    // 加载配置
    apg_test_config_t *config = apg_test_config_create();
    if (cert_path) apg_test_config_set_client_cert(config, cert_path);
    if (key_path) apg_test_config_set_client_key(config, key_path);
    if (resolve_ip) apg_test_config_set_resolve(config, resolve_ip, resolve_port);
    if (debug) apg_test_config_set_debug(config, debug);
    
    // 本地测试时修改 gateway_url 包含端口
    char *gateway_url = config->gateway_url;
    char local_url[256] = {0};
    if (!is_remote && resolve_ip) {
        // 本地测试：URL 需要带端口号
        snprintf(local_url, sizeof(local_url), "%s:%d", 
                 config->gateway_url, resolve_port);
        gateway_url = local_url;
    }
    
    apg_test_config_print(config);
    
    // 创建 HTTP 客户端
    apg_http_client_t *client = NULL;
    if (!no_http) {
        client = apg_http_client_create_full(&(apg_http_client_config_t){
            .base_url = gateway_url,
            .client_cert_path = config->client_cert_path,
            .client_key_path = config->client_key_path,
            .resolve_host = "api.caringfamily.cn",
            .resolve_ip = config->resolve_ip,
            .resolve_port = config->resolve_port,
            .debug = config->debug,
            .timeout_ms = config->timeout_ms
        });
        
        if (!client) {
            printf("Warning: Failed to create HTTP client, skipping HTTP tests\n");
        }
    }
    
    // 运行测试
    int failures = 0;
    
    failures += test_error_codes();
    failures += test_greet(client, gateway_url);
    failures += test_greet2(client, gateway_url);
    
    // 清理
    apg_http_client_destroy(client);
    apg_test_config_destroy(config);
    
    // 结果
    printf("\n========================================\n");
    if (failures == 0) {
        printf("All tests PASSED!\n");
    } else {
        printf("%d test(s) FAILED!\n", failures);
    }
    printf("========================================\n");
    
    return failures;
}
