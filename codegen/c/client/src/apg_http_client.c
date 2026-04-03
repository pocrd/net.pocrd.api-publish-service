#include "apg_http_client.h"
#include <stdlib.h>
#include <string.h>
#include <stdio.h>
#include <curl/curl.h>

/* 内部客户端结构 */
struct apg_http_client {
    char *base_url;
    char *resolve_host;
    char *resolve_ip;
    int resolve_port;
    char *client_cert_path;
    char *client_key_path;
    bool debug;
    int timeout_ms;
    CURL *curl;
};

/* 响应写入回调 */
typedef struct {
    char *data;
    size_t size;
} response_buffer_t;

static size_t write_callback(void *contents, size_t size, size_t nmemb, void *userp) {
    size_t realsize = size * nmemb;
    response_buffer_t *mem = (response_buffer_t *)userp;
    
    char *ptr = realloc(mem->data, mem->size + realsize + 1);
    if (!ptr) return 0;
    
    mem->data = ptr;
    memcpy(&(mem->data[mem->size]), contents, realsize);
    mem->size += realsize;
    mem->data[mem->size] = 0;
    
    return realsize;
}

/* 创建基础客户端 */
apg_http_client_t* apg_http_client_create(const char *base_url) {
    return apg_http_client_create_with_debug(base_url, false);
}

apg_http_client_t* apg_http_client_create_with_debug(const char *base_url, bool debug) {
    if (!base_url) return NULL;
    
    apg_http_client_t *client = (apg_http_client_t*)calloc(1, sizeof(apg_http_client_t));
    if (!client) return NULL;
    
    client->base_url = strdup(base_url);
    client->debug = debug;
    client->timeout_ms = 30000; // 默认 30 秒
    
    // 初始化 curl
    curl_global_init(CURL_GLOBAL_DEFAULT);
    client->curl = curl_easy_init();
    if (!client->curl) {
        free(client->base_url);
        free(client);
        return NULL;
    }
    
    if (debug) {
        curl_easy_setopt(client->curl, CURLOPT_VERBOSE, 1L);
    }
    
    return client;
}

apg_http_client_t* apg_http_client_create_with_mtls(const char *base_url,
                                                      const char *client_cert_path,
                                                      const char *client_key_path) {
    apg_http_client_t *client = apg_http_client_create(base_url);
    if (!client) return NULL;
    
    if (client_cert_path) {
        client->client_cert_path = strdup(client_cert_path);
    }
    if (client_key_path) {
        client->client_key_path = strdup(client_key_path);
    }
    
    return client;
}

apg_http_client_t* apg_http_client_create_full(const apg_http_client_config_t *config) {
    if (!config || !config->base_url) return NULL;
    
    apg_http_client_t *client = apg_http_client_create_with_debug(config->base_url, config->debug);
    if (!client) return NULL;
    
    client->timeout_ms = config->timeout_ms > 0 ? config->timeout_ms : 30000;
    
    if (config->resolve_host && config->resolve_ip) {
        client->resolve_host = strdup(config->resolve_host);
        client->resolve_ip = strdup(config->resolve_ip);
        client->resolve_port = config->resolve_port;
    }
    
    if (config->client_cert_path) {
        client->client_cert_path = strdup(config->client_cert_path);
    }
    if (config->client_key_path) {
        client->client_key_path = strdup(config->client_key_path);
    }
    
    return client;
}

void apg_http_client_destroy(apg_http_client_t *client) {
    if (!client) return;
    
    if (client->curl) {
        curl_easy_cleanup(client->curl);
    }
    
    free(client->base_url);
    free(client->resolve_host);
    free(client->resolve_ip);
    free(client->client_cert_path);
    free(client->client_key_path);
    free(client);
    
    curl_global_cleanup();
}

void apg_http_client_set_debug(apg_http_client_t *client, bool debug) {
    if (!client) return;
    client->debug = debug;
    curl_easy_setopt(client->curl, CURLOPT_VERBOSE, debug ? 1L : 0L);
}

void apg_http_client_set_timeout(apg_http_client_t *client, int timeout_ms) {
    if (!client) return;
    client->timeout_ms = timeout_ms;
}

void apg_http_client_set_resolve(apg_http_client_t *client, const char *host,
                                  const char *ip, int port) {
    if (!client) return;
    
    free(client->resolve_host);
    free(client->resolve_ip);
    
    client->resolve_host = host ? strdup(host) : NULL;
    client->resolve_ip = ip ? strdup(ip) : NULL;
    client->resolve_port = port;
}

void apg_http_client_set_mtls(apg_http_client_t *client, const char *cert_path,
                               const char *key_path) {
    if (!client) return;
    
    free(client->client_cert_path);
    free(client->client_key_path);
    
    client->client_cert_path = cert_path ? strdup(cert_path) : NULL;
    client->client_key_path = key_path ? strdup(key_path) : NULL;
}

/* 执行 HTTP 请求 */
static apg_http_response_t* perform_request(apg_http_client_t *client, const char *url,
                                             const char *method, const char *body,
                                             const char *content_type) {
    if (!client || !url) return NULL;
    
    apg_http_response_t *response = (apg_http_response_t*)calloc(1, sizeof(apg_http_response_t));
    if (!response) return NULL;
    
    response_buffer_t buffer = {0};
    
    // 重置 curl 选项
    curl_easy_reset(client->curl);
    
    // 设置 URL
    curl_easy_setopt(client->curl, CURLOPT_URL, url);
    
    // 设置回调
    curl_easy_setopt(client->curl, CURLOPT_WRITEFUNCTION, write_callback);
    curl_easy_setopt(client->curl, CURLOPT_WRITEDATA, (void *)&buffer);
    
    // 设置超时
    curl_easy_setopt(client->curl, CURLOPT_TIMEOUT_MS, (long)client->timeout_ms);
    curl_easy_setopt(client->curl, CURLOPT_CONNECTTIMEOUT_MS, (long)(client->timeout_ms / 2));
    
    // 设置请求方法
    if (strcmp(method, "POST") == 0) {
        curl_easy_setopt(client->curl, CURLOPT_POST, 1L);
        if (body) {
            curl_easy_setopt(client->curl, CURLOPT_POSTFIELDS, body);
        }
    }
    
    // 设置 headers
    struct curl_slist *headers = NULL;
    if (content_type) {
        char ct_header[256];
        snprintf(ct_header, sizeof(ct_header), "Content-Type: %s", content_type);
        headers = curl_slist_append(headers, ct_header);
    }
    headers = curl_slist_append(headers, "Accept: application/json");
    curl_easy_setopt(client->curl, CURLOPT_HTTPHEADER, headers);
    
    // 设置 SSL/TLS 选项
    if (strncmp(url, "https://", 8) == 0) {
        // 设置客户端证书
        if (client->client_cert_path) {
            curl_easy_setopt(client->curl, CURLOPT_SSLCERT, client->client_cert_path);
        }
        if (client->client_key_path) {
            curl_easy_setopt(client->curl, CURLOPT_SSLKEY, client->client_key_path);
        }
        
        // 允许自签名证书（测试用）
        curl_easy_setopt(client->curl, CURLOPT_SSL_VERIFYPEER, 0L);
        curl_easy_setopt(client->curl, CURLOPT_SSL_VERIFYHOST, 0L);
    }
    
    // 设置 resolve（模拟 curl --resolve）
    if (client->resolve_host && client->resolve_ip) {
        struct curl_slist *host = NULL;
        char resolve_str[512];
        int port = client->resolve_port > 0 ? client->resolve_port : 443;
        snprintf(resolve_str, sizeof(resolve_str), "%s:%d:%s", 
                 client->resolve_host, port, client->resolve_ip);
        host = curl_slist_append(host, resolve_str);
        curl_easy_setopt(client->curl, CURLOPT_RESOLVE, host);
    }
    
    // 执行请求
    CURLcode res = curl_easy_perform(client->curl);
    
    if (res != CURLE_OK) {
        fprintf(stderr, "curl_easy_perform() failed: %s\n", curl_easy_strerror(res));
        response->status_code = -1;
    } else {
        long http_code = 0;
        curl_easy_getinfo(client->curl, CURLINFO_RESPONSE_CODE, &http_code);
        response->status_code = (int)http_code;
    }
    
    // 清理
    curl_slist_free_all(headers);
    
    // 设置响应体
    if (buffer.data) {
        response->body = buffer.data;
        response->body_len = buffer.size;
    }
    
    return response;
}

apg_http_response_t* apg_http_get(apg_http_client_t *client, const char *path) {
    if (!client || !path) return NULL;
    
    char *url = apg_http_build_url(client->base_url, NULL, path);
    if (!url) return NULL;
    
    apg_http_response_t *response = perform_request(client, url, "GET", NULL, NULL);
    free(url);
    
    return response;
}

apg_http_response_t* apg_http_post(apg_http_client_t *client, const char *path,
                                    const char *body, const char *content_type) {
    if (!client || !path) return NULL;
    
    char *url = apg_http_build_url(client->base_url, NULL, path);
    if (!url) return NULL;
    
    apg_http_response_t *response = perform_request(client, url, "POST", body, 
                                                     content_type ? content_type : "application/json");
    free(url);
    
    return response;
}

apg_http_response_t* apg_http_request(apg_http_client_t *client, const char *full_url,
                                       apg_http_method_t method, const char *body,
                                       const char *content_type) {
    if (!client || !full_url) return NULL;
    
    const char *method_str = (method == APG_HTTP_POST) ? "POST" : "GET";
    apg_http_response_t *response = perform_request(client, full_url, method_str, body,
                                                     content_type ? content_type : "application/json");
    return response;
}

void apg_http_response_free(apg_http_response_t *response) {
    if (!response) return;
    free(response->body);
    free(response->content_type);
    free(response);
}

bool apg_http_response_is_success(const apg_http_response_t *response) {
    return response && response->status_code >= 200 && response->status_code < 300;
}

bool apg_http_response_is_error(const apg_http_response_t *response) {
    return !apg_http_response_is_success(response);
}

char* apg_http_build_url(const char *base_url, const char *interface_name,
                         const char *method_name) {
    if (!base_url) return NULL;
    
    size_t len = strlen(base_url) + 1;
    if (interface_name) len += strlen(interface_name) + 1;
    if (method_name) len += strlen(method_name) + 1;
    
    char *url = (char*)malloc(len);
    if (!url) return NULL;
    
    strcpy(url, base_url);
    
    // 移除末尾的斜杠
    size_t base_len = strlen(url);
    while (base_len > 0 && url[base_len - 1] == '/') {
        url[base_len - 1] = '\0';
        base_len--;
    }
    
    if (interface_name) {
        strcat(url, "/");
        strcat(url, interface_name);
    }
    
    if (method_name) {
        strcat(url, "/");
        strcat(url, method_name);
    }
    
    return url;
}

char* apg_http_escape_json_string(const char *str) {
    if (!str) return NULL;
    
    size_t len = strlen(str);
    size_t escaped_len = 0;
    
    // 计算转义后的长度
    for (size_t i = 0; i < len; i++) {
        switch (str[i]) {
            case '"':
            case '\\':
            case '\b':
            case '\f':
            case '\n':
            case '\r':
            case '\t':
                escaped_len += 2;
                break;
            default:
                escaped_len += 1;
        }
    }
    
    char *escaped = (char*)malloc(escaped_len + 1);
    if (!escaped) return NULL;
    
    size_t j = 0;
    for (size_t i = 0; i < len; i++) {
        switch (str[i]) {
            case '"': escaped[j++] = '\\'; escaped[j++] = '"'; break;
            case '\\': escaped[j++] = '\\'; escaped[j++] = '\\'; break;
            case '\b': escaped[j++] = '\\'; escaped[j++] = 'b'; break;
            case '\f': escaped[j++] = '\\'; escaped[j++] = 'f'; break;
            case '\n': escaped[j++] = '\\'; escaped[j++] = 'n'; break;
            case '\r': escaped[j++] = '\\'; escaped[j++] = 'r'; break;
            case '\t': escaped[j++] = '\\'; escaped[j++] = 't'; break;
            default: escaped[j++] = str[i];
        }
    }
    escaped[j] = '\0';
    
    return escaped;
}
