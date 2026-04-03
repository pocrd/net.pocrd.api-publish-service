#include "apg_list.h"
#include <stdlib.h>

apg_list_t* apg_list_create(void) {
    apg_list_t *list = (apg_list_t*)malloc(sizeof(apg_list_t));
    if (list) {
        list->head = NULL;
        list->tail = NULL;
        list->size = 0;
    }
    return list;
}

void apg_list_destroy(apg_list_t *list, void (*free_fn)(void*)) {
    if (!list) return;
    apg_list_node_t *current = list->head;
    while (current) {
        apg_list_node_t *next = current->next;
        if (free_fn && current->data) {
            free_fn(current->data);
        }
        free(current);
        current = next;
    }
    free(list);
}

void apg_list_append(apg_list_t *list, void *data) {
    if (!list) return;
    apg_list_node_t *node = (apg_list_node_t*)malloc(sizeof(apg_list_node_t));
    if (!node) return;
    node->data = data;
    node->next = NULL;
    if (list->tail) {
        list->tail->next = node;
    } else {
        list->head = node;
    }
    list->tail = node;
    list->size++;
}
