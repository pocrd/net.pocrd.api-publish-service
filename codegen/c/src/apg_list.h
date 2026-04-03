#ifndef APG_LIST_H
#define APG_LIST_H

#include <stddef.h>

#ifdef __cplusplus
extern "C" {
#endif

/**
 * @file apg_list.h
 * @brief Generic linked list for C SDK
 */

typedef struct apg_list_node {
    void *data;
    struct apg_list_node *next;
} apg_list_node_t;

typedef struct apg_list {
    apg_list_node_t *head;
    apg_list_node_t *tail;
    size_t size;
} apg_list_t;

/* List operations */
apg_list_t* apg_list_create(void);
void apg_list_destroy(apg_list_t *list, void (*free_fn)(void*));
void apg_list_append(apg_list_t *list, void *data);

#ifdef __cplusplus
}
#endif

#endif /* APG_LIST_H */
