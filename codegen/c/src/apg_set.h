#ifndef APG_SET_H
#define APG_SET_H

#include <stddef.h>

#ifdef __cplusplus
extern "C" {
#endif

/**
 * @file apg_set.h
 * @brief Generic set (unique elements) for C SDK
 */

typedef struct apg_set_node {
    void *data;
    struct apg_set_node *next;
} apg_set_node_t;

typedef struct apg_set {
    apg_set_node_t *head;
    apg_set_node_t *tail;
    size_t size;
} apg_set_t;

/**
 * Compare function for set elements, returns 0 if equal
 */
typedef int (*apg_set_cmp_fn)(const void *a, const void *b);

/* Set operations */
apg_set_t* apg_set_create(void);
void apg_set_destroy(apg_set_t *set, void (*free_fn)(void*));
/**
 * Add element to set, returns 1 if added, 0 if already exists
 */
int apg_set_add(apg_set_t *set, void *data, apg_set_cmp_fn cmp_fn);
/**
 * Check if element exists in set
 */
int apg_set_contains(apg_set_t *set, const void *data, apg_set_cmp_fn cmp_fn);

/* Helper compare functions */
int apg_set_cmp_str(const void *a, const void *b);
int apg_set_cmp_int(const void *a, const void *b);

#ifdef __cplusplus
}
#endif

#endif /* APG_SET_H */
