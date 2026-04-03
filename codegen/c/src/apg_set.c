#include "apg_set.h"
#include <stdlib.h>
#include <string.h>

apg_set_t* apg_set_create(void) {
    apg_set_t *set = (apg_set_t*)malloc(sizeof(apg_set_t));
    if (set) {
        set->head = NULL;
        set->tail = NULL;
        set->size = 0;
    }
    return set;
}

void apg_set_destroy(apg_set_t *set, void (*free_fn)(void*)) {
    if (!set) return;
    apg_set_node_t *current = set->head;
    while (current) {
        apg_set_node_t *next = current->next;
        if (free_fn && current->data) {
            free_fn(current->data);
        }
        free(current);
        current = next;
    }
    free(set);
}

int apg_set_add(apg_set_t *set, void *data, apg_set_cmp_fn cmp_fn) {
    if (!set) return 0;
    
    /* Check if already exists */
    if (cmp_fn && apg_set_contains(set, data, cmp_fn)) {
        return 0;
    }
    
    apg_set_node_t *node = (apg_set_node_t*)malloc(sizeof(apg_set_node_t));
    if (!node) return 0;
    node->data = data;
    node->next = NULL;
    
    if (set->tail) {
        set->tail->next = node;
    } else {
        set->head = node;
    }
    set->tail = node;
    set->size++;
    return 1;
}

int apg_set_contains(apg_set_t *set, const void *data, apg_set_cmp_fn cmp_fn) {
    if (!set || !cmp_fn) return 0;
    apg_set_node_t *current = set->head;
    while (current) {
        if (cmp_fn(current->data, data) == 0) {
            return 1;
        }
        current = current->next;
    }
    return 0;
}

int apg_set_cmp_str(const void *a, const void *b) {
    if (!a && !b) return 0;
    if (!a || !b) return (a ? 1 : -1);
    return strcmp((const char*)a, (const char*)b);
}

int apg_set_cmp_int(const void *a, const void *b) {
    if (!a && !b) return 0;
    if (!a || !b) return (a ? 1 : -1);
    return *(const int*)a - *(const int*)b;
}
