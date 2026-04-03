#ifndef APG_TEST_CONFIG_H
#define APG_TEST_CONFIG_H

#include <stdbool.h>

#ifdef __cplusplus
extern "C" {
#endif

/**
 * @file apg_test_config.h
 * @brief 测试配置管理器
 * 
 * 管理测试配置，包括网关 URL、证书路径等
 */

/* 默认配置 */
#define APG_DEFAULT_GATEWAY_URL "https://api.caringfamily.cn"
#define APG_DEFAULT_TIMEOUT_MS  30000

/* 测试配置结构 */
typedef struct {
    char *gateway_url;
    char *client_cert_path;
    char *client_key_path;
    char *resolve_ip;
    int resolve_port;
    bool debug;
    int timeout_ms;
} apg_test_config_t;

/* 配置管理 */
apg_test_config_t* apg_test_config_create(void);
void apg_test_config_destroy(apg_test_config_t *config);

/* 从环境变量加载配置 */
void apg_test_config_load_from_env(apg_test_config_t *config);

/* 配置设置 */
void apg_test_config_set_gateway_url(apg_test_config_t *config, const char *url);
void apg_test_config_set_client_cert(apg_test_config_t *config, const char *path);
void apg_test_config_set_client_key(apg_test_config_t *config, const char *path);
void apg_test_config_set_resolve(apg_test_config_t *config, const char *ip, int port);
void apg_test_config_set_debug(apg_test_config_t *config, bool debug);
void apg_test_config_set_timeout(apg_test_config_t *config, int timeout_ms);

/* 获取配置值 */
const char* apg_test_config_get_gateway_url(const apg_test_config_t *config);
const char* apg_test_config_get_client_cert(const apg_test_config_t *config);
const char* apg_test_config_get_client_key(const apg_test_config_t *config);

/* 打印配置信息 */
void apg_test_config_print(const apg_test_config_t *config);

#ifdef __cplusplus
}
#endif

#endif /* APG_TEST_CONFIG_H */
