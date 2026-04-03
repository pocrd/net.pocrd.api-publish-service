/**
 * All API Methods Test - C 版本
 * 参考 Java 的 AllApiMethodsTest.java
 * 
 * 使用生成的实体结构和 SDK 进行测试
 */

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <time.h>
#include "apg_http_client.h"
#include "apg_test_config.h"

/* Include all autogen headers */
#include "apg_return_code.h"
#include "apg_apipublish_entities.h"
#include "apg_dubbodemo_entities.h"

/* GreeterService API methods */
#include "greeterservice_greet.h"
#include "greeterservice_greet2.h"
#include "greeterservice_greet_stream.h"
#include "greeterservice_test_complex_nested_entity_input.h"
#include "greeterservice_test_list_nested_entity_input.h"
#include "greeterservice_test_complex_param_combination.h"
#include "greeterservice_test_batch_entity_array_input.h"
#include "greeterservice_test_deep_nested_entity_return.h"
#include "greeterservice_test_paged_nested_entity_return.h"
#include "greeterservice_test_complex_set_entity_return.h"
#include "greeterservice_test_grouped_items_return.h"
#include "greeterservice_test_entity_array_return.h"
#include "greeterservice_test_complex_scenario.h"

/* CRUDService API methods */
#include "crudservice_create_user.h"
#include "crudservice_get_user_by_id.h"
#include "crudservice_update_user.h"
#include "crudservice_delete_user.h"
#include "crudservice_get_all_users.h"
#include "crudservice_create_product.h"
#include "crudservice_get_product_by_id.h"
#include "crudservice_update_product.h"
#include "crudservice_delete_product.h"
#include "crudservice_get_all_products.h"
#include "crudservice_create_order.h"
#include "crudservice_get_order_by_id.h"
#include "crudservice_update_order.h"
#include "crudservice_delete_order.h"
#include "crudservice_get_all_orders.h"

/* ValidApiTestService API methods */
#include "validapitestservice_test_basic_types.h"
#include "validapitestservice_test_no_params.h"
#include "validapitestservice_test_single_entity.h"
#include "validapitestservice_test_nested_entity.h"
#include "validapitestservice_test_deep_nested_entity.h"
#include "validapitestservice_test_array_param.h"
#include "validapitestservice_test_list_param.h"
#include "validapitestservice_test_set_param.h"
#include "validapitestservice_test_complex_params.h"
#include "validapitestservice_test_enum_param.h"
#include "validapitestservice_test_enum_field_entity.h"
#include "validapitestservice_test_optional_params.h"
#include "validapitestservice_test_regex_validation.h"
#include "validapitestservice_test_multiple_validation.h"
#include "validapitestservice_test_return_array.h"
#include "validapitestservice_test_return_list.h"
#include "validapitestservice_test_void_return.h"
#include "validapitestservice_test_wrapper_return.h"
#include "validapitestservice_test_page_query.h"
#include "validapitestservice_test_page_with_nested_entity.h"
#include "validapitestservice_test_server_stream.h"
#include "validapitestservice_test_circular_entity.h"
#include "validapitestservice_test_set_entity_return.h"
#include "validapitestservice_test_stream_observer_complex.h"

/* Test counters */
static int tests_passed = 0;
static int tests_failed = 0;
static int tests_total = 0;

/* Test macros */
#define TEST_START(name) do { \
    tests_total++; \
    printf("\n[Test %d] %s\n", tests_total, name); \
    printf("----------------\n"); \
} while(0)

#define TEST_ASSERT(cond, msg) do { \
    if (!(cond)) { \
        printf("  [FAIL] %s (line %d)\n", msg, __LINE__); \
        tests_failed++; \
        return 1; \
    } else { \
        printf("  [PASS] %s\n", msg); \
    } \
} while(0)

/* Helper: Execute API and return response (caller must free) */
static apg_http_response_t* execute_api(apg_http_client_t *client, const char *url, 
                                         apg_http_method_t method, const char *body) {
    apg_http_response_t *resp = apg_http_request(client, url, method, body, "application/json");
    return resp;
}

/* ==================== GreeterService Tests ==================== */

static int test_greeterservice_greet(apg_http_client_t *client, const char *gateway_url) {
    TEST_START("GreeterService.Greet");
    
    GreeterService_Greet_t *api = greeterservice_greet_create();
    TEST_ASSERT(api != NULL, "API method created");
    
    GreeterService_Greet_request_t *req = greeterservice_greet_request_create();
    TEST_ASSERT(req != NULL, "Request created");
    req->name = strdup("World");
    
    char *body = greeterservice_greet_build_request_body(req);
    TEST_ASSERT(body != NULL, "Request body built");
    printf("  Request: %s\n", body);
    
    char *url = greeterservice_greet_build_url(gateway_url);
    TEST_ASSERT(url != NULL, "URL built");
    
    apg_http_response_t *resp = execute_api(client, url, api->http_method, body);
    TEST_ASSERT(resp != NULL, "Response received");
    TEST_ASSERT(apg_http_response_is_success(resp), "HTTP success");
    
    printf("  Response: %s\n", resp->body);
    TEST_ASSERT(resp->body != NULL && strlen(resp->body) > 0, "Response not empty");
    
    apg_http_response_free(resp);
    free(body);
    free(url);
    greeterservice_greet_request_destroy(req);
    greeterservice_greet_destroy(api);
    
    tests_passed++;
    return 0;
}

static int test_greeterservice_greet2(apg_http_client_t *client, const char *gateway_url) {
    TEST_START("GreeterService.Greet2");
    
    GreeterService_Greet2_t *api = greeterservice_greet2_create();
    GreeterService_Greet2_request_t *req = greeterservice_greet2_request_create();
    req->name1 = strdup("张三");
    req->name2 = strdup("李四");
    
    char *body = greeterservice_greet2_build_request_body(req);
    char *url = greeterservice_greet2_build_url(gateway_url);
    
    apg_http_response_t *resp = execute_api(client, url, api->http_method, body);
    TEST_ASSERT(resp != NULL && apg_http_response_is_success(resp), "Request successful");
    
    printf("  Response: %s\n", resp->body);
    TEST_ASSERT(strstr(resp->body, "张三") != NULL && strstr(resp->body, "李四") != NULL, 
                "Response contains names");
    
    apg_http_response_free(resp);
    free(body);
    free(url);
    greeterservice_greet2_request_destroy(req);
    greeterservice_greet2_destroy(api);
    
    tests_passed++;
    return 0;
}

static int test_greeterservice_greet_stream(apg_http_client_t *client, const char *gateway_url) {
    TEST_START("GreeterService.GreetStream");
    
    GreeterService_GreetStream_t *api = greeterservice_greet_stream_create();
    GreeterService_GreetStream_request_t *req = greeterservice_greet_stream_request_create();
    req->name = strdup("StreamUser");
    
    char *body = greeterservice_greet_stream_build_request_body(req);
    char *url = greeterservice_greet_stream_build_url(gateway_url);
    
    apg_http_response_t *resp = execute_api(client, url, api->http_method, body);
    TEST_ASSERT(resp != NULL, "Response received");
    printf("  Response: %s\n", resp->body ? resp->body : "(null)");
    
    apg_http_response_free(resp);
    free(body);
    free(url);
    greeterservice_greet_stream_request_destroy(req);
    greeterservice_greet_stream_destroy(api);
    
    tests_passed++;
    return 0;
}

static int test_greeterservice_test_complex_nested_entity_input(apg_http_client_t *client, const char *gateway_url) {
    TEST_START("GreeterService.TestComplexNestedEntityInput");
    
    // 构造复杂嵌套实体
    ApiPublish_ServiceInfo_t *serviceInfo = apipublish_service_info_create();
    serviceInfo->serviceName = strdup("TestService");
    serviceInfo->version = strdup("1.0.0");
    serviceInfo->uptime = 1234567890L;
    serviceInfo->requestCount = 100;
    
    // 添加 API 信息列表
    serviceInfo->apiInfos = apg_list_create();
    ApiPublish_ApiInfo_t *apiInfo1 = apipublish_api_info_create();
    apiInfo1->apiName = strdup("testMethod1");
    apiInfo1->path = strdup("/api/v1/test1");
    apiInfo1->httpMethod = strdup("GET");
    apiInfo1->description = strdup("Test API with \"quotes\"");
    apg_list_append(serviceInfo->apiInfos, apiInfo1);
    
    // 添加错误码列表
    serviceInfo->errorCodes = apg_list_create();
    apg_list_append(serviceInfo->errorCodes, strdup("E0001"));
    apg_list_append(serviceInfo->errorCodes, strdup("E0002:错误\"信息\""));
    
    char *body_json = apipublish_service_info_to_json(serviceInfo);
    printf("  ServiceInfo JSON: %s\n", body_json);
    
    GreeterService_TestComplexNestedEntityInput_t *api = greeterservice_test_complex_nested_entity_input_create();
    char *url = greeterservice_test_complex_nested_entity_input_build_url(gateway_url);
    
    apg_http_response_t *resp = execute_api(client, url, api->http_method, body_json);
    TEST_ASSERT(resp != NULL && apg_http_response_is_success(resp), "Request successful");
    printf("  Response: %s\n", resp->body);
    
    // 反序列化响应
    if (resp->body && resp->body[0] == '{') {
        ApiPublish_ServiceInfo_t *respService = apipublish_service_info_from_json(resp->body);
        if (respService) {
            printf("  Response service: name=%s\n", respService->serviceName ? respService->serviceName : "(null)");
            apipublish_service_info_destroy(respService);
        }
    }
    
    apg_http_response_free(resp);
    free(body_json);
    free(url);
    apipublish_service_info_destroy(serviceInfo);
    greeterservice_test_complex_nested_entity_input_destroy(api);
    
    tests_passed++;
    return 0;
}

static int test_greeterservice_test_list_nested_entity_input(apg_http_client_t *client, const char *gateway_url) {
    TEST_START("GreeterService.TestListNestedEntityInput");
    
    // 构造嵌套订单实体
    ApiPublish_AddressInfo_t *address = apipublish_address_info_create();
    address->province = strdup("Province");
    address->city = strdup("City");
    address->district = strdup("District");
    address->detail = strdup("Street 123");
    address->zipCode = strdup("100000");
    
    ApiPublish_UserInfo_t *buyer = apipublish_user_info_create();
    buyer->userId = 1L;
    buyer->username = strdup("buyer_test");
    buyer->email = strdup("buyer@example.com");
    buyer->age = 30;
    buyer->active = true;
    buyer->address = address;
    
    ApiPublish_OrderInfo_t *order = apipublish_order_info_create();
    order->orderId = 1L;
    order->orderNo = strdup("ORDER001_VERIFIED");
    order->buyer = buyer;
    order->status = strdup("PAID_VERIFIED");
    order->totalAmount = strdup("349.98");
    
    char *order_json = apipublish_order_info_to_json(order);
    printf("  Order JSON: %s\n", order_json);
    
    GreeterService_TestListNestedEntityInput_t *api = greeterservice_test_list_nested_entity_input_create();
    GreeterService_TestListNestedEntityInput_request_t *req = greeterservice_test_list_nested_entity_input_request_create();
    req->orders = apg_list_create();
    apg_list_append(req->orders, order);
    
    char *body = greeterservice_test_list_nested_entity_input_build_request_body(req);
    char *url = greeterservice_test_list_nested_entity_input_build_url(gateway_url);
    
    apg_http_response_t *resp = execute_api(client, url, api->http_method, body);
    TEST_ASSERT(resp != NULL && apg_http_response_is_success(resp), "Request successful");
    printf("  Response: %s\n", resp->body);
    
    apg_http_response_free(resp);
    free(body);
    free(url);
    free(order_json);
    greeterservice_test_list_nested_entity_input_request_destroy(req);
    greeterservice_test_list_nested_entity_input_destroy(api);
    
    tests_passed++;
    return 0;
}

static int test_greeterservice_test_complex_param_combination(apg_http_client_t *client, const char *gateway_url) {
    TEST_START("GreeterService.TestComplexParamCombination");
    
    // 构造复杂参数
    ApiPublish_AddressInfo_t *address = apipublish_address_info_create();
    address->province = strdup("Province_测试");
    address->city = strdup("City_VERIFIED");
    address->district = strdup("District");
    address->detail = strdup("Street 123");
    address->zipCode = strdup("100000");
    
    ApiPublish_UserInfo_t *user = apipublish_user_info_create();
    user->userId = 1L;
    user->username = strdup("user_verified");
    user->email = strdup("user@example.com");
    user->age = 25;
    user->active = true;
    user->address = address;
    
    GreeterService_TestComplexParamCombination_t *api = greeterservice_test_complex_param_combination_create();
    GreeterService_TestComplexParamCombination_request_t *req = greeterservice_test_complex_param_combination_request_create();
    req->userId = 42L;
    req->user = user;
    req->tags = apg_set_create();
    apg_set_add(req->tags, strdup("tag1"), apg_set_cmp_str);
    apg_set_add(req->tags, strdup("tag2"), apg_set_cmp_str);
    
    char *body = greeterservice_test_complex_param_combination_build_request_body(req);
    char *url = greeterservice_test_complex_param_combination_build_url(gateway_url);
    printf("  Request: %s\n", body);
    
    apg_http_response_t *resp = execute_api(client, url, api->http_method, body);
    TEST_ASSERT(resp != NULL && apg_http_response_is_success(resp), "Request successful");
    printf("  Response: %s\n", resp->body);
    
    apg_http_response_free(resp);
    free(body);
    free(url);
    greeterservice_test_complex_param_combination_request_destroy(req);
    greeterservice_test_complex_param_combination_destroy(api);
    
    tests_passed++;
    return 0;
}

static int test_greeterservice_test_batch_entity_array_input(apg_http_client_t *client, const char *gateway_url) {
    TEST_START("GreeterService.TestBatchEntityArrayInput");
    
    GreeterService_TestBatchEntityArrayInput_t *api = greeterservice_test_batch_entity_array_input_create();
    GreeterService_TestBatchEntityArrayInput_request_t *req = greeterservice_test_batch_entity_array_input_request_create();
    req->items = apg_list_create();
    
    ApiPublish_OrderItem_t *item1 = apipublish_order_item_create();
    item1->productId = 1L;
    item1->productName = strdup("Product1_\"quoted\"");
    item1->quantity = 2;
    item1->unitPrice = strdup("99.99");
    item1->totalPrice = strdup("199.98");
    apg_list_append(req->items, item1);
    
    ApiPublish_OrderItem_t *item2 = apipublish_order_item_create();
    item2->productId = 2L;
    item2->productName = strdup("Product2_测试");
    item2->quantity = 1;
    item2->unitPrice = strdup("49.99");
    item2->totalPrice = strdup("49.99");
    apg_list_append(req->items, item2);
    
    char *body = greeterservice_test_batch_entity_array_input_build_request_body(req);
    char *url = greeterservice_test_batch_entity_array_input_build_url(gateway_url);
    
    apg_http_response_t *resp = execute_api(client, url, api->http_method, body);
    TEST_ASSERT(resp != NULL && apg_http_response_is_success(resp), "Request successful");
    printf("  Response: %s\n", resp->body);
    
    apg_http_response_free(resp);
    free(body);
    free(url);
    greeterservice_test_batch_entity_array_input_request_destroy(req);
    greeterservice_test_batch_entity_array_input_destroy(api);
    
    tests_passed++;
    return 0;
}

static int test_greeterservice_test_deep_nested_entity_return(apg_http_client_t *client, const char *gateway_url) {
    TEST_START("GreeterService.TestDeepNestedEntityReturn");
    
    GreeterService_TestDeepNestedEntityReturn_t *api = greeterservice_test_deep_nested_entity_return_create();
    GreeterService_TestDeepNestedEntityReturn_request_t *req = greeterservice_test_deep_nested_entity_return_request_create();
    req->serviceName = strdup("mock-service");
    
    char *body = greeterservice_test_deep_nested_entity_return_build_request_body(req);
    char *url = greeterservice_test_deep_nested_entity_return_build_url(gateway_url);
    
    apg_http_response_t *resp = execute_api(client, url, api->http_method, body);
    TEST_ASSERT(resp != NULL && apg_http_response_is_success(resp), "Request successful");
    
    // 反序列化响应
    if (resp->body && resp->body[0] == '{') {
        ApiPublish_ServiceInfo_t *serviceInfo = apipublish_service_info_from_json(resp->body);
        if (serviceInfo) {
            printf("  Service: name=%s, version=%s, uptime=%lld\n", 
                   serviceInfo->serviceName ? serviceInfo->serviceName : "(null)",
                   serviceInfo->version ? serviceInfo->version : "(null)",
                   serviceInfo->uptime);
            apipublish_service_info_destroy(serviceInfo);
        }
    }
    
    apg_http_response_free(resp);
    free(body);
    free(url);
    greeterservice_test_deep_nested_entity_return_request_destroy(req);
    greeterservice_test_deep_nested_entity_return_destroy(api);
    
    tests_passed++;
    return 0;
}

static int test_greeterservice_test_paged_nested_entity_return(apg_http_client_t *client, const char *gateway_url) {
    TEST_START("GreeterService.TestPagedNestedEntityReturn");
    
    GreeterService_TestPagedNestedEntityReturn_t *api = greeterservice_test_paged_nested_entity_return_create();
    GreeterService_TestPagedNestedEntityReturn_request_t *req = greeterservice_test_paged_nested_entity_return_request_create();
    req->pageNum = 1;
    req->pageSize = 10;
    
    char *body = greeterservice_test_paged_nested_entity_return_build_request_body(req);
    char *url = greeterservice_test_paged_nested_entity_return_build_url(gateway_url);
    
    apg_http_response_t *resp = execute_api(client, url, api->http_method, body);
    TEST_ASSERT(resp != NULL && apg_http_response_is_success(resp), "Request successful");
    printf("  Response: %s\n", resp->body);
    
    apg_http_response_free(resp);
    free(body);
    free(url);
    greeterservice_test_paged_nested_entity_return_request_destroy(req);
    greeterservice_test_paged_nested_entity_return_destroy(api);
    
    tests_passed++;
    return 0;
}

static int test_greeterservice_test_complex_set_entity_return(apg_http_client_t *client, const char *gateway_url) {
    TEST_START("GreeterService.TestComplexSetEntityReturn");
    
    GreeterService_TestComplexSetEntityReturn_t *api = greeterservice_test_complex_set_entity_return_create();
    GreeterService_TestComplexSetEntityReturn_request_t *req = greeterservice_test_complex_set_entity_return_request_create();
    req->userIds = apg_list_create();
    apg_list_append(req->userIds, strdup("1"));
    apg_list_append(req->userIds, strdup("2"));
    apg_list_append(req->userIds, strdup("3"));
    
    char *body = greeterservice_test_complex_set_entity_return_build_request_body(req);
    char *url = greeterservice_test_complex_set_entity_return_build_url(gateway_url);
    
    apg_http_response_t *resp = execute_api(client, url, api->http_method, body);
    TEST_ASSERT(resp != NULL && apg_http_response_is_success(resp), "Request successful");
    printf("  Response: %s\n", resp->body);
    
    apg_http_response_free(resp);
    free(body);
    free(url);
    greeterservice_test_complex_set_entity_return_request_destroy(req);
    greeterservice_test_complex_set_entity_return_destroy(api);
    
    tests_passed++;
    return 0;
}

static int test_greeterservice_test_grouped_items_return(apg_http_client_t *client, const char *gateway_url) {
    TEST_START("GreeterService.TestGroupedItemsReturn");
    
    GreeterService_TestGroupedItemsReturn_t *api = greeterservice_test_grouped_items_return_create();
    GreeterService_TestGroupedItemsReturn_request_t *req = greeterservice_test_grouped_items_return_request_create();
    req->categoryId = 3392L;
    
    char *body = greeterservice_test_grouped_items_return_build_request_body(req);
    char *url = greeterservice_test_grouped_items_return_build_url(gateway_url);
    
    apg_http_response_t *resp = execute_api(client, url, api->http_method, body);
    TEST_ASSERT(resp != NULL && apg_http_response_is_success(resp), "Request successful");
    
    if (resp->body && resp->body[0] == '{') {
        ApiPublish_OrderInfo_t *order = apipublish_order_info_from_json(resp->body);
        if (order) {
            printf("  Order: orderNo=%s, status=%s\n", 
                   order->orderNo ? order->orderNo : "(null)",
                   order->status ? order->status : "(null)");
            apipublish_order_info_destroy(order);
        }
    }
    
    apg_http_response_free(resp);
    free(body);
    free(url);
    greeterservice_test_grouped_items_return_request_destroy(req);
    greeterservice_test_grouped_items_return_destroy(api);
    
    tests_passed++;
    return 0;
}

static int test_greeterservice_test_entity_array_return(apg_http_client_t *client, const char *gateway_url) {
    TEST_START("GreeterService.TestEntityArrayReturn");
    
    GreeterService_TestEntityArrayReturn_t *api = greeterservice_test_entity_array_return_create();
    GreeterService_TestEntityArrayReturn_request_t *req = greeterservice_test_entity_array_return_request_create();
    req->departmentId = 1L;
    
    char *body = greeterservice_test_entity_array_return_build_request_body(req);
    char *url = greeterservice_test_entity_array_return_build_url(gateway_url);
    
    apg_http_response_t *resp = execute_api(client, url, api->http_method, body);
    TEST_ASSERT(resp != NULL && apg_http_response_is_success(resp), "Request successful");
    
    // 反序列化为用户数组
    if (resp->body && resp->body[0] == '[') {
        cJSON *json = cJSON_Parse(resp->body);
        if (json && cJSON_IsArray(json)) {
            int count = cJSON_GetArraySize(json);
            printf("  Received %d users\n", count);
            for (int i = 0; i < count && i < 3; i++) {
                cJSON *item = cJSON_GetArrayItem(json, i);
                ApiPublish_UserInfo_t *user = apipublish_user_info_from_cjson(item);
                if (user) {
                    printf("    User %d: id=%lld, username=%s\n", 
                           i + 1, user->userId, user->username ? user->username : "(null)");
                    apipublish_user_info_destroy(user);
                }
            }
        }
        cJSON_Delete(json);
    }
    
    apg_http_response_free(resp);
    free(body);
    free(url);
    greeterservice_test_entity_array_return_request_destroy(req);
    greeterservice_test_entity_array_return_destroy(api);
    
    tests_passed++;
    return 0;
}

static int test_greeterservice_test_complex_scenario(apg_http_client_t *client, const char *gateway_url) {
    TEST_START("GreeterService.TestComplexScenario");
    
    GreeterService_TestComplexScenario_t *api = greeterservice_test_complex_scenario_create();
    GreeterService_TestComplexScenario_request_t *req = greeterservice_test_complex_scenario_request_create();
    
    req->apiInfos = apg_list_create();
    ApiPublish_ApiInfo_t *apiInfo = apipublish_api_info_create();
    apiInfo->apiName = strdup("testMethod1");
    apiInfo->path = strdup("/api/v1/test1");
    apiInfo->httpMethod = strdup("GET");
    apiInfo->description = strdup("API with \"quotes\"");
    apg_list_append(req->apiInfos, apiInfo);
    
    req->config = apipublish_address_info_create();
    req->config->province = strdup("Province_测试");
    req->config->city = strdup("City_VERIFIED");
    req->config->district = strdup("District");
    req->config->detail = strdup("Street 123");
    req->config->zipCode = strdup("100000");
    
    char *body = greeterservice_test_complex_scenario_build_request_body(req);
    char *url = greeterservice_test_complex_scenario_build_url(gateway_url);
    
    apg_http_response_t *resp = execute_api(client, url, api->http_method, body);
    TEST_ASSERT(resp != NULL && apg_http_response_is_success(resp), "Request successful");
    printf("  Response: %s\n", resp->body);
    
    apg_http_response_free(resp);
    free(body);
    free(url);
    greeterservice_test_complex_scenario_request_destroy(req);
    greeterservice_test_complex_scenario_destroy(api);
    
    tests_passed++;
    return 0;
}

/* ==================== CRUDService - User Tests ==================== */

static int test_crudservice_create_user(apg_http_client_t *client, const char *gateway_url) {
    TEST_START("CRUDService.CreateUser");
    
    DubboDemo_User_t *user = dubbodemo_user_create();
    char uniq[32];
    snprintf(uniq, sizeof(uniq), "%ld", (long)time(NULL));
    user->username = strdup(uniq);
    user->email = strdup("test@example.com");
    user->phone = strdup("13800138000");
    
    char *user_json = dubbodemo_user_to_json(user);
    printf("  User JSON: %s\n", user_json);
    
    CRUDService_CreateUser_t *api = crudservice_create_user_create();
    char *url = crudservice_create_user_build_url(gateway_url);
    
    apg_http_response_t *resp = execute_api(client, url, api->http_method, user_json);
    TEST_ASSERT(resp != NULL, "Response received");
    printf("  Response: %s\n", resp->body ? resp->body : "(null)");
    
    apg_http_response_free(resp);
    free(user_json);
    free(url);
    dubbodemo_user_destroy(user);
    crudservice_create_user_destroy(api);
    
    tests_passed++;
    return 0;
}

static int test_crudservice_get_user_by_id(apg_http_client_t *client, const char *gateway_url) {
    TEST_START("CRUDService.GetUserById");
    
    CRUDService_GetUserById_t *api = crudservice_get_user_by_id_create();
    CRUDService_GetUserById_request_t *req = crudservice_get_user_by_id_request_create();
    req->id = 1;
    
    char *body = crudservice_get_user_by_id_build_request_body(req);
    char *url = crudservice_get_user_by_id_build_url(gateway_url);
    
    apg_http_response_t *resp = execute_api(client, url, api->http_method, body);
    TEST_ASSERT(resp != NULL && apg_http_response_is_success(resp), "Request successful");
    printf("  Response: %s\n", resp->body);
    
    apg_http_response_free(resp);
    free(body);
    free(url);
    crudservice_get_user_by_id_request_destroy(req);
    crudservice_get_user_by_id_destroy(api);
    
    tests_passed++;
    return 0;
}

static int test_crudservice_update_user(apg_http_client_t *client, const char *gateway_url) {
    TEST_START("CRUDService.UpdateUser");
    
    DubboDemo_User_t *user = dubbodemo_user_create();
    user->id = 1;
    user->username = strdup("updateduser");
    user->email = strdup("updated@example.com");
    user->phone = strdup("13900139000");
    
    char *user_json = dubbodemo_user_to_json(user);
    
    CRUDService_UpdateUser_t *api = crudservice_update_user_create();
    char *url = crudservice_update_user_build_url(gateway_url);
    
    apg_http_response_t *resp = execute_api(client, url, api->http_method, user_json);
    TEST_ASSERT(resp != NULL, "Response received");
    printf("  Response: %s\n", resp->body ? resp->body : "(null)");
    
    apg_http_response_free(resp);
    free(user_json);
    free(url);
    dubbodemo_user_destroy(user);
    crudservice_update_user_destroy(api);
    
    tests_passed++;
    return 0;
}

static int test_crudservice_delete_user(apg_http_client_t *client, const char *gateway_url) {
    TEST_START("CRUDService.DeleteUser");
    
    CRUDService_DeleteUser_t *api = crudservice_delete_user_create();
    CRUDService_DeleteUser_request_t *req = crudservice_delete_user_request_create();
    req->id = 1;
    
    char *body = crudservice_delete_user_build_request_body(req);
    char *url = crudservice_delete_user_build_url(gateway_url);
    
    apg_http_response_t *resp = execute_api(client, url, api->http_method, body);
    TEST_ASSERT(resp != NULL, "Response received");
    printf("  Response: %s\n", resp->body ? resp->body : "(null)");
    
    apg_http_response_free(resp);
    free(body);
    free(url);
    crudservice_delete_user_request_destroy(req);
    crudservice_delete_user_destroy(api);
    
    tests_passed++;
    return 0;
}

static int test_crudservice_get_all_users(apg_http_client_t *client, const char *gateway_url) {
    TEST_START("CRUDService.GetAllUsers");
    
    CRUDService_GetAllUsers_t *api = crudservice_get_all_users_create();
    char *url = crudservice_get_all_users_build_url(gateway_url);
    
    apg_http_response_t *resp = execute_api(client, url, api->http_method, NULL);
    TEST_ASSERT(resp != NULL && apg_http_response_is_success(resp), "Request successful");
    printf("  Response: %s\n", resp->body);
    
    apg_http_response_free(resp);
    free(url);
    crudservice_get_all_users_destroy(api);
    
    tests_passed++;
    return 0;
}

/* ==================== CRUDService - Product Tests ==================== */

static int test_crudservice_create_product(apg_http_client_t *client, const char *gateway_url) {
    TEST_START("CRUDService.CreateProduct");
    
    DubboDemo_Product_t *product = dubbodemo_product_create();
    char code[32];
    snprintf(code, sizeof(code), "PROD%ld", (long)time(NULL));
    product->productCode = strdup(code);
    product->productName = strdup("Test Product");
    product->price = 9999;
    product->stock = 100;
    product->category = strdup("Electronics");
    
    char *product_json = dubbodemo_product_to_json(product);
    printf("  Product JSON: %s\n", product_json);
    
    CRUDService_CreateProduct_t *api = crudservice_create_product_create();
    char *url = crudservice_create_product_build_url(gateway_url);
    
    apg_http_response_t *resp = execute_api(client, url, api->http_method, product_json);
    TEST_ASSERT(resp != NULL, "Response received");
    printf("  Response: %s\n", resp->body ? resp->body : "(null)");
    
    apg_http_response_free(resp);
    free(product_json);
    free(url);
    dubbodemo_product_destroy(product);
    crudservice_create_product_destroy(api);
    
    tests_passed++;
    return 0;
}

static int test_crudservice_get_product_by_id(apg_http_client_t *client, const char *gateway_url) {
    TEST_START("CRUDService.GetProductById");
    
    CRUDService_GetProductById_t *api = crudservice_get_product_by_id_create();
    CRUDService_GetProductById_request_t *req = crudservice_get_product_by_id_request_create();
    req->id = 1;
    
    char *body = crudservice_get_product_by_id_build_request_body(req);
    char *url = crudservice_get_product_by_id_build_url(gateway_url);
    
    apg_http_response_t *resp = execute_api(client, url, api->http_method, body);
    TEST_ASSERT(resp != NULL && apg_http_response_is_success(resp), "Request successful");
    printf("  Response: %s\n", resp->body);
    
    apg_http_response_free(resp);
    free(body);
    free(url);
    crudservice_get_product_by_id_request_destroy(req);
    crudservice_get_product_by_id_destroy(api);
    
    tests_passed++;
    return 0;
}

static int test_crudservice_update_product(apg_http_client_t *client, const char *gateway_url) {
    TEST_START("CRUDService.UpdateProduct");
    
    DubboDemo_Product_t *product = dubbodemo_product_create();
    product->id = 1;
    product->productCode = strdup("PROD001");
    product->productName = strdup("UpdatedProduct");
    product->price = 19999;
    product->stock = 50;
    product->category = strdup("Electronics");
    
    char *product_json = dubbodemo_product_to_json(product);
    
    CRUDService_UpdateProduct_t *api = crudservice_update_product_create();
    char *url = crudservice_update_product_build_url(gateway_url);
    
    apg_http_response_t *resp = execute_api(client, url, api->http_method, product_json);
    TEST_ASSERT(resp != NULL, "Response received");
    printf("  Response: %s\n", resp->body ? resp->body : "(null)");
    
    apg_http_response_free(resp);
    free(product_json);
    free(url);
    dubbodemo_product_destroy(product);
    crudservice_update_product_destroy(api);
    
    tests_passed++;
    return 0;
}

static int test_crudservice_delete_product(apg_http_client_t *client, const char *gateway_url) {
    TEST_START("CRUDService.DeleteProduct");
    
    CRUDService_DeleteProduct_t *api = crudservice_delete_product_create();
    CRUDService_DeleteProduct_request_t *req = crudservice_delete_product_request_create();
    req->id = 1;
    
    char *body = crudservice_delete_product_build_request_body(req);
    char *url = crudservice_delete_product_build_url(gateway_url);
    
    apg_http_response_t *resp = execute_api(client, url, api->http_method, body);
    TEST_ASSERT(resp != NULL, "Response received");
    printf("  Response: %s\n", resp->body ? resp->body : "(null)");
    
    apg_http_response_free(resp);
    free(body);
    free(url);
    crudservice_delete_product_request_destroy(req);
    crudservice_delete_product_destroy(api);
    
    tests_passed++;
    return 0;
}

static int test_crudservice_get_all_products(apg_http_client_t *client, const char *gateway_url) {
    TEST_START("CRUDService.GetAllProducts");
    
    CRUDService_GetAllProducts_t *api = crudservice_get_all_products_create();
    char *url = crudservice_get_all_products_build_url(gateway_url);
    
    apg_http_response_t *resp = execute_api(client, url, api->http_method, NULL);
    TEST_ASSERT(resp != NULL && apg_http_response_is_success(resp), "Request successful");
    printf("  Response: %s\n", resp->body);
    
    apg_http_response_free(resp);
    free(url);
    crudservice_get_all_products_destroy(api);
    
    tests_passed++;
    return 0;
}

/* ==================== CRUDService - Order Tests ==================== */

static int test_crudservice_create_order(apg_http_client_t *client, const char *gateway_url) {
    TEST_START("CRUDService.CreateOrder");
    
    DubboDemo_Order_t *order = dubbodemo_order_create();
    char order_no[32];
    snprintf(order_no, sizeof(order_no), "ORD%ld", (long)time(NULL));
    order->orderNo = strdup(order_no);
    order->userId = 1L;
    order->amount = 29999;
    order->status = 0;
    order->remark = strdup("Test order from C SDK");
    
    char *order_json = dubbodemo_order_to_json(order);
    printf("  Order JSON: %s\n", order_json);
    
    CRUDService_CreateOrder_t *api = crudservice_create_order_create();
    char *url = crudservice_create_order_build_url(gateway_url);
    
    apg_http_response_t *resp = execute_api(client, url, api->http_method, order_json);
    TEST_ASSERT(resp != NULL, "Response received");
    printf("  Response: %s\n", resp->body ? resp->body : "(null)");
    
    apg_http_response_free(resp);
    free(order_json);
    free(url);
    dubbodemo_order_destroy(order);
    crudservice_create_order_destroy(api);
    
    tests_passed++;
    return 0;
}

static int test_crudservice_get_order_by_id(apg_http_client_t *client, const char *gateway_url) {
    TEST_START("CRUDService.GetOrderById");
    
    CRUDService_GetOrderById_t *api = crudservice_get_order_by_id_create();
    CRUDService_GetOrderById_request_t *req = crudservice_get_order_by_id_request_create();
    req->id = 1;
    
    char *body = crudservice_get_order_by_id_build_request_body(req);
    char *url = crudservice_get_order_by_id_build_url(gateway_url);
    
    apg_http_response_t *resp = execute_api(client, url, api->http_method, body);
    TEST_ASSERT(resp != NULL && apg_http_response_is_success(resp), "Request successful");
    printf("  Response: %s\n", resp->body);
    
    apg_http_response_free(resp);
    free(body);
    free(url);
    crudservice_get_order_by_id_request_destroy(req);
    crudservice_get_order_by_id_destroy(api);
    
    tests_passed++;
    return 0;
}

static int test_crudservice_update_order(apg_http_client_t *client, const char *gateway_url) {
    TEST_START("CRUDService.UpdateOrder");
    
    DubboDemo_Order_t *order = dubbodemo_order_create();
    order->id = 1;
    order->orderNo = strdup("ORDER001");
    order->userId = 1L;
    order->amount = 39999;
    order->status = 2;
    order->remark = strdup("Updated order");
    
    char *order_json = dubbodemo_order_to_json(order);
    
    CRUDService_UpdateOrder_t *api = crudservice_update_order_create();
    char *url = crudservice_update_order_build_url(gateway_url);
    
    apg_http_response_t *resp = execute_api(client, url, api->http_method, order_json);
    TEST_ASSERT(resp != NULL, "Response received");
    printf("  Response: %s\n", resp->body ? resp->body : "(null)");
    
    apg_http_response_free(resp);
    free(order_json);
    free(url);
    dubbodemo_order_destroy(order);
    crudservice_update_order_destroy(api);
    
    tests_passed++;
    return 0;
}

static int test_crudservice_delete_order(apg_http_client_t *client, const char *gateway_url) {
    TEST_START("CRUDService.DeleteOrder");
    
    CRUDService_DeleteOrder_t *api = crudservice_delete_order_create();
    CRUDService_DeleteOrder_request_t *req = crudservice_delete_order_request_create();
    req->id = 1;
    
    char *body = crudservice_delete_order_build_request_body(req);
    char *url = crudservice_delete_order_build_url(gateway_url);
    
    apg_http_response_t *resp = execute_api(client, url, api->http_method, body);
    TEST_ASSERT(resp != NULL, "Response received");
    printf("  Response: %s\n", resp->body ? resp->body : "(null)");
    
    apg_http_response_free(resp);
    free(body);
    free(url);
    crudservice_delete_order_request_destroy(req);
    crudservice_delete_order_destroy(api);
    
    tests_passed++;
    return 0;
}

static int test_crudservice_get_all_orders(apg_http_client_t *client, const char *gateway_url) {
    TEST_START("CRUDService.GetAllOrders");
    
    CRUDService_GetAllOrders_t *api = crudservice_get_all_orders_create();
    char *url = crudservice_get_all_orders_build_url(gateway_url);
    
    apg_http_response_t *resp = execute_api(client, url, api->http_method, NULL);
    TEST_ASSERT(resp != NULL && apg_http_response_is_success(resp), "Request successful");
    printf("  Response: %s\n", resp->body);
    
    apg_http_response_free(resp);
    free(url);
    crudservice_get_all_orders_destroy(api);
    
    tests_passed++;
    return 0;
}

/* ==================== ValidApiTestService - Basic Tests ==================== */

static int test_validapitestservice_test_basic_types(apg_http_client_t *client, const char *gateway_url) {
    TEST_START("ValidApiTestService.TestBasicTypes");
    
    ValidApiTestService_TestBasicTypes_t *api = validapitestservice_test_basic_types_create();
    ValidApiTestService_TestBasicTypes_request_t *req = validapitestservice_test_basic_types_request_create();
    req->strParam = strdup("test_string");
    req->intParam = 42;
    req->longParam = 123456789L;
    req->boolParam = true;
    
    char *body = validapitestservice_test_basic_types_build_request_body(req);
    char *url = validapitestservice_test_basic_types_build_url(gateway_url);
    
    apg_http_response_t *resp = execute_api(client, url, api->http_method, body);
    TEST_ASSERT(resp != NULL && apg_http_response_is_success(resp), "Request successful");
    printf("  Response: %s\n", resp->body);
    
    apg_http_response_free(resp);
    free(body);
    free(url);
    validapitestservice_test_basic_types_request_destroy(req);
    validapitestservice_test_basic_types_destroy(api);
    
    tests_passed++;
    return 0;
}

static int test_validapitestservice_test_no_params(apg_http_client_t *client, const char *gateway_url) {
    TEST_START("ValidApiTestService.TestNoParams");
    
    ValidApiTestService_TestNoParams_t *api = validapitestservice_test_no_params_create();
    char *url = validapitestservice_test_no_params_build_url(gateway_url);
    
    apg_http_response_t *resp = execute_api(client, url, api->http_method, NULL);
    TEST_ASSERT(resp != NULL && apg_http_response_is_success(resp), "Request successful");
    printf("  Response: %s\n", resp->body);
    
    apg_http_response_free(resp);
    free(url);
    validapitestservice_test_no_params_destroy(api);
    
    tests_passed++;
    return 0;
}

static int test_validapitestservice_test_single_entity(apg_http_client_t *client, const char *gateway_url) {
    TEST_START("ValidApiTestService.TestSingleEntity");
    
    ApiPublish_AddressInfo_t *address = apipublish_address_info_create();
    address->province = strdup("广东省");
    address->city = strdup("深圳市");
    address->district = strdup("南山区");
    address->detail = strdup("科技园路123号");
    address->zipCode = strdup("518000");
    
    char *address_json = apipublish_address_info_to_json(address);
    printf("  Address JSON: %s\n", address_json);
    
    ValidApiTestService_TestSingleEntity_t *api = validapitestservice_test_single_entity_create();
    char *url = validapitestservice_test_single_entity_build_url(gateway_url);
    
    apg_http_response_t *resp = execute_api(client, url, api->http_method, address_json);
    TEST_ASSERT(resp != NULL && apg_http_response_is_success(resp), "Request successful");
    printf("  Response: %s\n", resp->body);
    
    apg_http_response_free(resp);
    free(address_json);
    free(url);
    apipublish_address_info_destroy(address);
    validapitestservice_test_single_entity_destroy(api);
    
    tests_passed++;
    return 0;
}

static int test_validapitestservice_test_nested_entity(apg_http_client_t *client, const char *gateway_url) {
    TEST_START("ValidApiTestService.TestNestedEntity");
    
    ApiPublish_AddressInfo_t *address = apipublish_address_info_create();
    address->province = strdup("北京市");
    address->city = strdup("北京市");
    address->district = strdup("朝阳区");
    address->detail = strdup("建国路88号");
    address->zipCode = strdup("100020");
    
    ApiPublish_UserInfo_t *user = apipublish_user_info_create();
    user->userId = 1L;
    user->username = strdup("testuser");
    user->email = strdup("test@example.com");
    user->age = 25;
    user->active = true;
    user->address = address;
    
    char *user_json = apipublish_user_info_to_json(user);
    printf("  User JSON: %s\n", user_json);
    
    ValidApiTestService_TestNestedEntity_t *api = validapitestservice_test_nested_entity_create();
    char *url = validapitestservice_test_nested_entity_build_url(gateway_url);
    
    apg_http_response_t *resp = execute_api(client, url, api->http_method, user_json);
    TEST_ASSERT(resp != NULL && apg_http_response_is_success(resp), "Request successful");
    printf("  Response: %s\n", resp->body);
    
    apg_http_response_free(resp);
    free(user_json);
    free(url);
    apipublish_user_info_destroy(user);
    validapitestservice_test_nested_entity_destroy(api);
    
    tests_passed++;
    return 0;
}

static int test_validapitestservice_test_deep_nested_entity(apg_http_client_t *client, const char *gateway_url) {
    TEST_START("ValidApiTestService.TestDeepNestedEntity");
    
    ApiPublish_OrderItem_t *item = apipublish_order_item_create();
    item->productId = 1L;
    item->productName = strdup("Product");
    item->quantity = 2;
    item->unitPrice = strdup("99.99");
    item->totalPrice = strdup("199.98");
    
    ApiPublish_UserInfo_t *buyer = apipublish_user_info_create();
    buyer->userId = 1L;
    buyer->username = strdup("buyer");
    buyer->email = strdup("buyer@example.com");
    buyer->age = 30;
    buyer->active = true;
    
    ApiPublish_OrderInfo_t *order = apipublish_order_info_create();
    order->orderId = 1L;
    order->orderNo = strdup("ORDER001");
    order->buyer = buyer;
    order->items = apg_list_create();
    apg_list_append(order->items, item);
    order->status = strdup("PAID");
    order->totalAmount = strdup("199.98");
    
    char *order_json = apipublish_order_info_to_json(order);
    printf("  Order JSON: %s\n", order_json);
    
    ValidApiTestService_TestDeepNestedEntity_t *api = validapitestservice_test_deep_nested_entity_create();
    char *url = validapitestservice_test_deep_nested_entity_build_url(gateway_url);
    
    apg_http_response_t *resp = execute_api(client, url, api->http_method, order_json);
    TEST_ASSERT(resp != NULL && apg_http_response_is_success(resp), "Request successful");
    printf("  Response: %s\n", resp->body);
    
    apg_http_response_free(resp);
    free(order_json);
    free(url);
    apipublish_order_info_destroy(order);
    validapitestservice_test_deep_nested_entity_destroy(api);
    
    tests_passed++;
    return 0;
}

/* ==================== ValidApiTestService - Collection Tests ==================== */

static int test_validapitestservice_test_array_param(apg_http_client_t *client, const char *gateway_url) {
    TEST_START("ValidApiTestService.TestArrayParam");
    
    ValidApiTestService_TestArrayParam_t *api = validapitestservice_test_array_param_create();
    ValidApiTestService_TestArrayParam_request_t *req = validapitestservice_test_array_param_request_create();
    req->ids = apg_list_create();
    apg_list_append(req->ids, strdup("1"));
    apg_list_append(req->ids, strdup("2"));
    apg_list_append(req->ids, strdup("3"));
    
    char *body = validapitestservice_test_array_param_build_request_body(req);
    char *url = validapitestservice_test_array_param_build_url(gateway_url);
    printf("  Request: %s\n", body);
    
    apg_http_response_t *resp = execute_api(client, url, api->http_method, body);
    TEST_ASSERT(resp != NULL && apg_http_response_is_success(resp), "Request successful");
    printf("  Response: %s\n", resp->body);
    
    apg_http_response_free(resp);
    free(body);
    free(url);
    validapitestservice_test_array_param_request_destroy(req);
    validapitestservice_test_array_param_destroy(api);
    
    tests_passed++;
    return 0;
}

static int test_validapitestservice_test_list_param(apg_http_client_t *client, const char *gateway_url) {
    TEST_START("ValidApiTestService.TestListParam");
    
    ValidApiTestService_TestListParam_t *api = validapitestservice_test_list_param_create();
    ValidApiTestService_TestListParam_request_t *req = validapitestservice_test_list_param_request_create();
    req->items = apg_list_create();
    apg_list_append(req->items, strdup("item1"));
    apg_list_append(req->items, strdup("item2"));
    apg_list_append(req->items, strdup("item3"));
    
    char *body = validapitestservice_test_list_param_build_request_body(req);
    char *url = validapitestservice_test_list_param_build_url(gateway_url);
    
    apg_http_response_t *resp = execute_api(client, url, api->http_method, body);
    TEST_ASSERT(resp != NULL && apg_http_response_is_success(resp), "Request successful");
    printf("  Response: %s\n", resp->body);
    
    apg_http_response_free(resp);
    free(body);
    free(url);
    validapitestservice_test_list_param_request_destroy(req);
    validapitestservice_test_list_param_destroy(api);
    
    tests_passed++;
    return 0;
}

static int test_validapitestservice_test_set_param(apg_http_client_t *client, const char *gateway_url) {
    TEST_START("ValidApiTestService.TestSetParam");
    
    ValidApiTestService_TestSetParam_t *api = validapitestservice_test_set_param_create();
    ValidApiTestService_TestSetParam_request_t *req = validapitestservice_test_set_param_request_create();
    req->tags = apg_set_create();
    apg_set_add(req->tags, strdup("item1"), apg_set_cmp_str);
    apg_set_add(req->tags, strdup("item2"), apg_set_cmp_str);
    
    char *body = validapitestservice_test_set_param_build_request_body(req);
    char *url = validapitestservice_test_set_param_build_url(gateway_url);
    
    apg_http_response_t *resp = execute_api(client, url, api->http_method, body);
    TEST_ASSERT(resp != NULL && apg_http_response_is_success(resp), "Request successful");
    printf("  Response: %s\n", resp->body);
    
    apg_http_response_free(resp);
    free(body);
    free(url);
    validapitestservice_test_set_param_request_destroy(req);
    validapitestservice_test_set_param_destroy(api);
    
    tests_passed++;
    return 0;
}

static int test_validapitestservice_test_complex_params(apg_http_client_t *client, const char *gateway_url) {
    TEST_START("ValidApiTestService.TestComplexParams");
    
    ApiPublish_OrderItem_t *item = apipublish_order_item_create();
    item->productId = 1L;
    item->productName = strdup("Product");
    item->quantity = 2;
    item->unitPrice = strdup("99.99");
    item->totalPrice = strdup("199.98");
    
    ApiPublish_AddressInfo_t *address = apipublish_address_info_create();
    address->province = strdup("Province");
    address->city = strdup("City");
    address->district = strdup("District");
    address->detail = strdup("Street 123");
    address->zipCode = strdup("100000");
    
    ValidApiTestService_TestComplexParams_t *api = validapitestservice_test_complex_params_create();
    ValidApiTestService_TestComplexParams_request_t *req = validapitestservice_test_complex_params_request_create();
    req->userId = 1L;
    req->items = apg_list_create();
    apg_list_append(req->items, item);
    req->address = address;
    
    char *body = validapitestservice_test_complex_params_build_request_body(req);
    char *url = validapitestservice_test_complex_params_build_url(gateway_url);
    
    apg_http_response_t *resp = execute_api(client, url, api->http_method, body);
    TEST_ASSERT(resp != NULL && apg_http_response_is_success(resp), "Request successful");
    printf("  Response: %s\n", resp->body);
    
    apg_http_response_free(resp);
    free(body);
    free(url);
    validapitestservice_test_complex_params_request_destroy(req);
    validapitestservice_test_complex_params_destroy(api);
    
    tests_passed++;
    return 0;
}

/* ==================== ValidApiTestService - Enum and Special Tests ==================== */

static int test_validapitestservice_test_enum_param(apg_http_client_t *client, const char *gateway_url) {
    TEST_START("ValidApiTestService.TestEnumParam");
    
    ValidApiTestService_TestEnumParam_t *api = validapitestservice_test_enum_param_create();
    ValidApiTestService_TestEnumParam_request_t *req = validapitestservice_test_enum_param_request_create();
    req->status = strdup("PENDING");
    
    char *body = validapitestservice_test_enum_param_build_request_body(req);
    char *url = validapitestservice_test_enum_param_build_url(gateway_url);
    
    apg_http_response_t *resp = execute_api(client, url, api->http_method, body);
    TEST_ASSERT(resp != NULL && apg_http_response_is_success(resp), "Request successful");
    printf("  Response: %s\n", resp->body);
    
    apg_http_response_free(resp);
    free(body);
    free(url);
    validapitestservice_test_enum_param_request_destroy(req);
    validapitestservice_test_enum_param_destroy(api);
    
    tests_passed++;
    return 0;
}

static int test_validapitestservice_test_enum_field_entity(apg_http_client_t *client, const char *gateway_url) {
    TEST_START("ValidApiTestService.TestEnumFieldEntity");
    
    ApiPublish_EnumFieldEntity_t *entity = apipublish_enum_field_entity_create();
    entity->orderId = 1L;
    entity->status = strdup("PENDING");
    entity->amount = strdup("Description");
    
    char *entity_json = apipublish_enum_field_entity_to_json(entity);
    
    ValidApiTestService_TestEnumFieldEntity_t *api = validapitestservice_test_enum_field_entity_create();
    char *url = validapitestservice_test_enum_field_entity_build_url(gateway_url);
    
    apg_http_response_t *resp = execute_api(client, url, api->http_method, entity_json);
    TEST_ASSERT(resp != NULL && apg_http_response_is_success(resp), "Request successful");
    printf("  Response: %s\n", resp->body);
    
    apg_http_response_free(resp);
    free(entity_json);
    free(url);
    apipublish_enum_field_entity_destroy(entity);
    validapitestservice_test_enum_field_entity_destroy(api);
    
    tests_passed++;
    return 0;
}

static int test_validapitestservice_test_optional_params(apg_http_client_t *client, const char *gateway_url) {
    TEST_START("ValidApiTestService.TestOptionalParams");
    
    ValidApiTestService_TestOptionalParams_t *api = validapitestservice_test_optional_params_create();
    ValidApiTestService_TestOptionalParams_request_t *req = validapitestservice_test_optional_params_request_create();
    req->requiredParam = strdup("required");
    req->optionalParam = strdup("optional");
    
    char *body = validapitestservice_test_optional_params_build_request_body(req);
    char *url = validapitestservice_test_optional_params_build_url(gateway_url);
    
    apg_http_response_t *resp = execute_api(client, url, api->http_method, body);
    TEST_ASSERT(resp != NULL && apg_http_response_is_success(resp), "Request successful");
    printf("  Response: %s\n", resp->body);
    
    apg_http_response_free(resp);
    free(body);
    free(url);
    validapitestservice_test_optional_params_request_destroy(req);
    validapitestservice_test_optional_params_destroy(api);
    
    tests_passed++;
    return 0;
}

static int test_validapitestservice_test_regex_validation(apg_http_client_t *client, const char *gateway_url) {
    TEST_START("ValidApiTestService.TestRegexValidation");
    
    ValidApiTestService_TestRegexValidation_t *api = validapitestservice_test_regex_validation_create();
    ValidApiTestService_TestRegexValidation_request_t *req = validapitestservice_test_regex_validation_request_create();
    req->email = strdup("test@example.com");
    
    char *body = validapitestservice_test_regex_validation_build_request_body(req);
    char *url = validapitestservice_test_regex_validation_build_url(gateway_url);
    
    apg_http_response_t *resp = execute_api(client, url, api->http_method, body);
    TEST_ASSERT(resp != NULL && apg_http_response_is_success(resp), "Request successful");
    printf("  Response: %s\n", resp->body);
    
    apg_http_response_free(resp);
    free(body);
    free(url);
    validapitestservice_test_regex_validation_request_destroy(req);
    validapitestservice_test_regex_validation_destroy(api);
    
    tests_passed++;
    return 0;
}

static int test_validapitestservice_test_multiple_validation(apg_http_client_t *client, const char *gateway_url) {
    TEST_START("ValidApiTestService.TestMultipleValidation");
    
    ValidApiTestService_TestMultipleValidation_t *api = validapitestservice_test_multiple_validation_create();
    ValidApiTestService_TestMultipleValidation_request_t *req = validapitestservice_test_multiple_validation_request_create();
    req->phone = strdup("13800138000");
    req->idCard = strdup("110101199001011234");
    
    char *body = validapitestservice_test_multiple_validation_build_request_body(req);
    char *url = validapitestservice_test_multiple_validation_build_url(gateway_url);
    
    apg_http_response_t *resp = execute_api(client, url, api->http_method, body);
    TEST_ASSERT(resp != NULL && apg_http_response_is_success(resp), "Request successful");
    printf("  Response: %s\n", resp->body);
    
    apg_http_response_free(resp);
    free(body);
    free(url);
    validapitestservice_test_multiple_validation_request_destroy(req);
    validapitestservice_test_multiple_validation_destroy(api);
    
    tests_passed++;
    return 0;
}

/* ==================== ValidApiTestService - Return Type Tests ==================== */

static int test_validapitestservice_test_return_array(apg_http_client_t *client, const char *gateway_url) {
    TEST_START("ValidApiTestService.TestReturnArray");
    
    ValidApiTestService_TestReturnArray_t *api = validapitestservice_test_return_array_create();
    ValidApiTestService_TestReturnArray_request_t *req = validapitestservice_test_return_array_request_create();
    req->size = 10;
    
    char *body = validapitestservice_test_return_array_build_request_body(req);
    char *url = validapitestservice_test_return_array_build_url(gateway_url);
    
    apg_http_response_t *resp = execute_api(client, url, api->http_method, body);
    TEST_ASSERT(resp != NULL && apg_http_response_is_success(resp), "Request successful");
    printf("  Response: %s\n", resp->body);
    
    apg_http_response_free(resp);
    free(body);
    free(url);
    validapitestservice_test_return_array_request_destroy(req);
    validapitestservice_test_return_array_destroy(api);
    
    tests_passed++;
    return 0;
}

static int test_validapitestservice_test_return_list(apg_http_client_t *client, const char *gateway_url) {
    TEST_START("ValidApiTestService.TestReturnList");
    
    ValidApiTestService_TestReturnList_t *api = validapitestservice_test_return_list_create();
    ValidApiTestService_TestReturnList_request_t *req = validapitestservice_test_return_list_request_create();
    req->count = 10;
    
    char *body = validapitestservice_test_return_list_build_request_body(req);
    char *url = validapitestservice_test_return_list_build_url(gateway_url);
    
    apg_http_response_t *resp = execute_api(client, url, api->http_method, body);
    TEST_ASSERT(resp != NULL && apg_http_response_is_success(resp), "Request successful");
    printf("  Response: %s\n", resp->body);
    
    apg_http_response_free(resp);
    free(body);
    free(url);
    validapitestservice_test_return_list_request_destroy(req);
    validapitestservice_test_return_list_destroy(api);
    
    tests_passed++;
    return 0;
}

static int test_validapitestservice_test_void_return(apg_http_client_t *client, const char *gateway_url) {
    TEST_START("ValidApiTestService.TestVoidReturn");
    
    ValidApiTestService_TestVoidReturn_t *api = validapitestservice_test_void_return_create();
    ValidApiTestService_TestVoidReturn_request_t *req = validapitestservice_test_void_return_request_create();
    req->action = strdup("test");
    
    char *body = validapitestservice_test_void_return_build_request_body(req);
    char *url = validapitestservice_test_void_return_build_url(gateway_url);
    
    apg_http_response_t *resp = execute_api(client, url, api->http_method, body);
    TEST_ASSERT(resp != NULL, "Response received");
    printf("  Response: %s\n", resp->body ? resp->body : "(null)");
    
    apg_http_response_free(resp);
    free(body);
    free(url);
    validapitestservice_test_void_return_request_destroy(req);
    validapitestservice_test_void_return_destroy(api);
    
    tests_passed++;
    return 0;
}

static int test_validapitestservice_test_wrapper_return(apg_http_client_t *client, const char *gateway_url) {
    TEST_START("ValidApiTestService.TestWrapperReturn");
    
    ValidApiTestService_TestWrapperReturn_t *api = validapitestservice_test_wrapper_return_create();
    ValidApiTestService_TestWrapperReturn_request_t *req = validapitestservice_test_wrapper_return_request_create();
    req->value = 42;
    
    char *body = validapitestservice_test_wrapper_return_build_request_body(req);
    char *url = validapitestservice_test_wrapper_return_build_url(gateway_url);
    
    apg_http_response_t *resp = execute_api(client, url, api->http_method, body);
    TEST_ASSERT(resp != NULL && apg_http_response_is_success(resp), "Request successful");
    printf("  Response: %s\n", resp->body);
    
    apg_http_response_free(resp);
    free(body);
    free(url);
    validapitestservice_test_wrapper_return_request_destroy(req);
    validapitestservice_test_wrapper_return_destroy(api);
    
    tests_passed++;
    return 0;
}

/* ==================== ValidApiTestService - Page and Stream Tests ==================== */

static int test_validapitestservice_test_page_query(apg_http_client_t *client, const char *gateway_url) {
    TEST_START("ValidApiTestService.TestPageQuery");
    
    ValidApiTestService_TestPageQuery_t *api = validapitestservice_test_page_query_create();
    ValidApiTestService_TestPageQuery_request_t *req = validapitestservice_test_page_query_request_create();
    req->pageNum = 1;
    req->pageSize = 5;
    
    char *body = validapitestservice_test_page_query_build_request_body(req);
    char *url = validapitestservice_test_page_query_build_url(gateway_url);
    
    apg_http_response_t *resp = execute_api(client, url, api->http_method, body);
    TEST_ASSERT(resp != NULL && apg_http_response_is_success(resp), "Request successful");
    
    // 反序列化为用户列表
    if (resp->body && resp->body[0] == '[') {
        cJSON *json = cJSON_Parse(resp->body);
        if (json && cJSON_IsArray(json)) {
            int count = cJSON_GetArraySize(json);
            printf("  Received %d users\n", count);
            for (int i = 0; i < count; i++) {
                cJSON *item = cJSON_GetArrayItem(json, i);
                ApiPublish_UserInfo_t *user = apipublish_user_info_from_cjson(item);
                if (user) {
                    printf("    User %d: id=%lld, username=%s, age=%d\n",
                           i + 1, user->userId, user->username ? user->username : "(null)", user->age);
                    apipublish_user_info_destroy(user);
                }
            }
        }
        cJSON_Delete(json);
    }
    
    apg_http_response_free(resp);
    free(body);
    free(url);
    validapitestservice_test_page_query_request_destroy(req);
    validapitestservice_test_page_query_destroy(api);
    
    tests_passed++;
    return 0;
}

static int test_validapitestservice_test_page_with_nested_entity(apg_http_client_t *client, const char *gateway_url) {
    TEST_START("ValidApiTestService.TestPageWithNestedEntity");
    
    ValidApiTestService_TestPageWithNestedEntity_t *api = validapitestservice_test_page_with_nested_entity_create();
    ValidApiTestService_TestPageWithNestedEntity_request_t *req = validapitestservice_test_page_with_nested_entity_request_create();
    req->status = strdup("PENDING");
    req->pageNum = 1;
    req->pageSize = 10;
    
    char *body = validapitestservice_test_page_with_nested_entity_build_request_body(req);
    char *url = validapitestservice_test_page_with_nested_entity_build_url(gateway_url);
    
    apg_http_response_t *resp = execute_api(client, url, api->http_method, body);
    TEST_ASSERT(resp != NULL && apg_http_response_is_success(resp), "Request successful");
    printf("  Response: %s\n", resp->body);
    
    apg_http_response_free(resp);
    free(body);
    free(url);
    validapitestservice_test_page_with_nested_entity_request_destroy(req);
    validapitestservice_test_page_with_nested_entity_destroy(api);
    
    tests_passed++;
    return 0;
}

static int test_validapitestservice_test_server_stream(apg_http_client_t *client, const char *gateway_url) {
    TEST_START("ValidApiTestService.TestServerStream");
    
    ValidApiTestService_TestServerStream_t *api = validapitestservice_test_server_stream_create();
    ValidApiTestService_TestServerStream_request_t *req = validapitestservice_test_server_stream_request_create();
    req->userId = 100L;
    
    char *body = validapitestservice_test_server_stream_build_request_body(req);
    char *url = validapitestservice_test_server_stream_build_url(gateway_url);
    
    apg_http_response_t *resp = execute_api(client, url, api->http_method, body);
    TEST_ASSERT(resp != NULL, "Response received");
    printf("  Response: %s\n", resp->body ? resp->body : "(null)");
    
    apg_http_response_free(resp);
    free(body);
    free(url);
    validapitestservice_test_server_stream_request_destroy(req);
    validapitestservice_test_server_stream_destroy(api);
    
    tests_passed++;
    return 0;
}

/* ==================== ValidApiTestService - Circular and Set Tests ==================== */

static int test_validapitestservice_test_circular_entity(apg_http_client_t *client, const char *gateway_url) {
    TEST_START("ValidApiTestService.TestCircularEntity");
    
    ApiPublish_CircularEntityA_t *entityA = apipublish_circular_entity_a_create();
    entityA->id = 1L;
    entityA->name = strdup("A");
    
    ApiPublish_CircularEntityB_t *entityB = apipublish_circular_entity_b_create();
    entityB->id = 1L;
    entityB->name = strdup("B");
    entityB->entityA = entityA;
    
    char *entity_json = apipublish_circular_entity_b_to_json(entityB);
    
    ValidApiTestService_TestCircularEntity_t *api = validapitestservice_test_circular_entity_create();
    char *url = validapitestservice_test_circular_entity_build_url(gateway_url);
    
    apg_http_response_t *resp = execute_api(client, url, api->http_method, entity_json);
    TEST_ASSERT(resp != NULL && apg_http_response_is_success(resp), "Request successful");
    printf("  Response: %s\n", resp->body);
    
    apg_http_response_free(resp);
    free(entity_json);
    free(url);
    apipublish_circular_entity_b_destroy(entityB);
    validapitestservice_test_circular_entity_destroy(api);
    
    tests_passed++;
    return 0;
}

static int test_validapitestservice_test_set_entity_return(apg_http_client_t *client, const char *gateway_url) {
    TEST_START("ValidApiTestService.TestSetEntityReturn");
    
    ValidApiTestService_TestSetEntityReturn_t *api = validapitestservice_test_set_entity_return_create();
    ValidApiTestService_TestSetEntityReturn_request_t *req = validapitestservice_test_set_entity_return_request_create();
    req->userIds = apg_list_create();
    apg_list_append(req->userIds, strdup("1"));
    apg_list_append(req->userIds, strdup("2"));
    apg_list_append(req->userIds, strdup("3"));
    
    char *body = validapitestservice_test_set_entity_return_build_request_body(req);
    char *url = validapitestservice_test_set_entity_return_build_url(gateway_url);
    
    apg_http_response_t *resp = execute_api(client, url, api->http_method, body);
    TEST_ASSERT(resp != NULL && apg_http_response_is_success(resp), "Request successful");
    printf("  Response: %s\n", resp->body);
    
    apg_http_response_free(resp);
    free(body);
    free(url);
    validapitestservice_test_set_entity_return_request_destroy(req);
    validapitestservice_test_set_entity_return_destroy(api);
    
    tests_passed++;
    return 0;
}

static int test_validapitestservice_test_stream_observer_complex(apg_http_client_t *client, const char *gateway_url) {
    TEST_START("ValidApiTestService.TestStreamObserverComplex");
    
    ValidApiTestService_TestStreamObserverComplex_t *api = validapitestservice_test_stream_observer_complex_create();
    ValidApiTestService_TestStreamObserverComplex_request_t *req = validapitestservice_test_stream_observer_complex_request_create();
    req->orderId = 100L;
    
    char *body = validapitestservice_test_stream_observer_complex_build_request_body(req);
    char *url = validapitestservice_test_stream_observer_complex_build_url(gateway_url);
    
    apg_http_response_t *resp = execute_api(client, url, api->http_method, body);
    TEST_ASSERT(resp != NULL, "Response received");
    printf("  Response: %s\n", resp->body ? resp->body : "(null)");
    
    apg_http_response_free(resp);
    free(body);
    free(url);
    validapitestservice_test_stream_observer_complex_request_destroy(req);
    validapitestservice_test_stream_observer_complex_destroy(api);
    
    tests_passed++;
    return 0;
}

/* ==================== Main ==================== */

int main(int argc, char *argv[]) {
    printf("========================================\n");
    printf("C SDK All API Methods Test\n");
    printf("Using generated entities and SDK\n");
    printf("========================================\n");
    
    // Parse arguments
    bool is_remote = false;
    char *cert_path = NULL;
    char *key_path = NULL;
    bool debug = false;
    
    for (int i = 1; i < argc; i++) {
        if (strcmp(argv[i], "-r") == 0 || strcmp(argv[i], "--remote") == 0) {
            is_remote = true;
        } else if (strcmp(argv[i], "--cert") == 0 && i + 1 < argc) {
            cert_path = argv[++i];
        } else if (strcmp(argv[i], "--key") == 0 && i + 1 < argc) {
            key_path = argv[++i];
        } else if (strcmp(argv[i], "--debug") == 0) {
            debug = true;
        }
    }
    
    // Default resolve settings
    char *resolve_ip = "127.0.0.1";
    int resolve_port = 30443;
    
    if (is_remote) {
        resolve_ip = NULL;
    }
    
    // Load config
    apg_test_config_t *config = apg_test_config_create();
    if (cert_path) apg_test_config_set_client_cert(config, cert_path);
    if (key_path) apg_test_config_set_client_key(config, key_path);
    if (resolve_ip) apg_test_config_set_resolve(config, resolve_ip, resolve_port);
    if (debug) apg_test_config_set_debug(config, debug);
    
    // Build gateway URL
    char gateway_url[256];
    if (!is_remote && resolve_ip) {
        snprintf(gateway_url, sizeof(gateway_url), "%s:%d", config->gateway_url, resolve_port);
    } else {
        snprintf(gateway_url, sizeof(gateway_url), "%s", config->gateway_url);
    }
    
    apg_test_config_print(config);
    
    // Create HTTP client
    apg_http_client_t *client = apg_http_client_create_full(&(apg_http_client_config_t){
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
        printf("Error: Failed to create HTTP client\n");
        return 1;
    }
    
    srand((unsigned int)time(NULL));
    
    printf("\n========================================\n");
    printf("Running Tests...\n");
    printf("========================================\n");
    
    // ==================== GreeterService Tests (13) ====================
    printf("\n=== GreeterService Tests ===\n");
    test_greeterservice_greet(client, gateway_url);
    test_greeterservice_greet2(client, gateway_url);
    test_greeterservice_greet_stream(client, gateway_url);
    test_greeterservice_test_complex_nested_entity_input(client, gateway_url);
    test_greeterservice_test_list_nested_entity_input(client, gateway_url);
    test_greeterservice_test_complex_param_combination(client, gateway_url);
    test_greeterservice_test_batch_entity_array_input(client, gateway_url);
    test_greeterservice_test_deep_nested_entity_return(client, gateway_url);
    test_greeterservice_test_paged_nested_entity_return(client, gateway_url);
    test_greeterservice_test_complex_set_entity_return(client, gateway_url);
    test_greeterservice_test_grouped_items_return(client, gateway_url);
    test_greeterservice_test_entity_array_return(client, gateway_url);
    test_greeterservice_test_complex_scenario(client, gateway_url);
    
    // ==================== CRUDService - User Tests (5) ====================
    printf("\n=== CRUDService - User Tests ===\n");
    test_crudservice_create_user(client, gateway_url);
    test_crudservice_get_user_by_id(client, gateway_url);
    test_crudservice_update_user(client, gateway_url);
    test_crudservice_delete_user(client, gateway_url);
    test_crudservice_get_all_users(client, gateway_url);
    
    // ==================== CRUDService - Product Tests (5) ====================
    printf("\n=== CRUDService - Product Tests ===\n");
    test_crudservice_create_product(client, gateway_url);
    test_crudservice_get_product_by_id(client, gateway_url);
    test_crudservice_update_product(client, gateway_url);
    test_crudservice_delete_product(client, gateway_url);
    test_crudservice_get_all_products(client, gateway_url);
    
    // ==================== CRUDService - Order Tests (5) ====================
    printf("\n=== CRUDService - Order Tests ===\n");
    test_crudservice_create_order(client, gateway_url);
    test_crudservice_get_order_by_id(client, gateway_url);
    test_crudservice_update_order(client, gateway_url);
    test_crudservice_delete_order(client, gateway_url);
    test_crudservice_get_all_orders(client, gateway_url);
    
    // ==================== ValidApiTestService - Basic Tests (6) ====================
    printf("\n=== ValidApiTestService - Basic Tests ===\n");
    test_validapitestservice_test_basic_types(client, gateway_url);
    test_validapitestservice_test_no_params(client, gateway_url);
    test_validapitestservice_test_single_entity(client, gateway_url);
    test_validapitestservice_test_nested_entity(client, gateway_url);
    test_validapitestservice_test_deep_nested_entity(client, gateway_url);
    
    // ==================== ValidApiTestService - Collection Tests (4) ====================
    printf("\n=== ValidApiTestService - Collection Tests ===\n");
    test_validapitestservice_test_array_param(client, gateway_url);
    test_validapitestservice_test_list_param(client, gateway_url);
    test_validapitestservice_test_set_param(client, gateway_url);
    test_validapitestservice_test_complex_params(client, gateway_url);
    
    // ==================== ValidApiTestService - Enum Tests (4) ====================
    printf("\n=== ValidApiTestService - Enum Tests ===\n");
    test_validapitestservice_test_enum_param(client, gateway_url);
    test_validapitestservice_test_enum_field_entity(client, gateway_url);
    test_validapitestservice_test_optional_params(client, gateway_url);
    test_validapitestservice_test_regex_validation(client, gateway_url);
    test_validapitestservice_test_multiple_validation(client, gateway_url);
    
    // ==================== ValidApiTestService - Return Type Tests (4) ====================
    printf("\n=== ValidApiTestService - Return Type Tests ===\n");
    test_validapitestservice_test_return_array(client, gateway_url);
    test_validapitestservice_test_return_list(client, gateway_url);
    test_validapitestservice_test_void_return(client, gateway_url);
    test_validapitestservice_test_wrapper_return(client, gateway_url);
    
    // ==================== ValidApiTestService - Page Tests (3) ====================
    printf("\n=== ValidApiTestService - Page Tests ===\n");
    test_validapitestservice_test_page_query(client, gateway_url);
    test_validapitestservice_test_page_with_nested_entity(client, gateway_url);
    test_validapitestservice_test_server_stream(client, gateway_url);
    
    // ==================== ValidApiTestService - Circular Tests (3) ====================
    printf("\n=== ValidApiTestService - Circular Tests ===\n");
    test_validapitestservice_test_circular_entity(client, gateway_url);
    test_validapitestservice_test_set_entity_return(client, gateway_url);
    test_validapitestservice_test_stream_observer_complex(client, gateway_url);
    
    // Cleanup
    apg_http_client_destroy(client);
    apg_test_config_destroy(config);
    
    // Results
    printf("\n========================================\n");
    printf("Test Results\n");
    printf("========================================\n");
    printf("Total:  %d\n", tests_total);
    printf("Passed: %d\n", tests_passed);
    printf("Failed: %d\n", tests_failed);
    printf("========================================\n");
    
    if (tests_failed == 0) {
        printf("All tests PASSED!\n");
    } else {
        printf("Some tests FAILED!\n");
    }
    
    return tests_failed > 0 ? 1 : 0;
}
