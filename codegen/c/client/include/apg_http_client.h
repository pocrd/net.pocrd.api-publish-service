#ifndef APG_HTTP_CLIENT_H
#define APG_HTTP_CLIENT_H

#include <stdbool.h>
#include <stddef.h>

#ifdef __cplusplus
extern "C" {
#endif

/**
 * @file apg_http_client.h
 * @brief APG HTTP Client - 基于 libcurl 的 HTTP/HTTPS 客户端
 * 
 * 支持 HTTP/HTTPS、mTLS 客户端证书认证、自定义域名解析
 */

/* HTTP 响应结构 */
typedef struct {
    int status_code;
    char *body;
    size_t body_len;
    char *content_type;
} apg_http_response_t;

/* HTTP 客户端配置 */
typedef struct {
    char *base_url;
    char *resolve_host;
    char *resolve_ip;
    int resolve_port;
    char *client_cert_path;
    char *client_key_path;
    bool debug;
    int timeout_ms;
} apg_http_client_config_t;

/* HTTP 客户端句柄 */
typedef struct apg_http_client apg_http_client_t;

/* 客户端生命周期 */
apg_http_client_t* apg_http_client_create(const char *base_url);
apg_http_client_t* apg_http_client_create_with_debug(const char *base_url, bool debug);
apg_http_client_t* apg_http_client_create_with_mtls(const char *base_url, 
                                                      const char *client_cert_path,
                                                      const char *client_key_path);
apg_http_client_t* apg_http_client_create_full(const apg_http_client_config_t *config);
void apg_http_client_destroy(apg_http_client_t *client);

/* 配置设置 */
void apg_http_client_set_debug(apg_http_client_t *client, bool debug);
void apg_http_client_set_timeout(apg_http_client_t *client, int timeout_ms);
void apg_http_client_set_resolve(apg_http_client_t *client, const char *host, 
                                  const char *ip, int port);
void apg_http_client_set_mtls(apg_http_client_t *client, const char *cert_path,
                               const char *key_path);

/* HTTP 请求方法 */
apg_http_response_t* apg_http_get(apg_http_client_t *client, const char *path);
apg_http_response_t* apg_http_post(apg_http_client_t *client, const char *path, 
                                    const char *body, const char *content_type);

/* 使用完整 URL 的请求方法 */
#include "apg_http_method.h"
apg_http_response_t* apg_http_request(apg_http_client_t *client, const char *full_url,
                                       apg_http_method_t method, const char *body,
                                       const char *content_type);

/* 使用 API 方法对象的便捷方法（需要包含生成的 API 头文件） */
struct apg_api_method;
apg_http_response_t* apg_http_call_method(apg_http_client_t *client, 
                                           struct apg_api_method *method,
                                           const char *request_body);

/* 响应处理 */
void apg_http_response_free(apg_http_response_t *response);
bool apg_http_response_is_success(const apg_http_response_t *response);
bool apg_http_response_is_error(const apg_http_response_t *response);

/* 工具函数 */
char* apg_http_build_url(const char *base_url, const char *interface_name, 
                         const char *method_name);
char* apg_http_escape_json_string(const char *str);

#ifdef __cplusplus
}
#endif

#endif /* APG_HTTP_CLIENT_H */
