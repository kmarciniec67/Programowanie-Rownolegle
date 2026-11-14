#include <stdio.h>
#include <stdlib.h>
#include <pthread.h>
#include <unistd.h>

#define LICZBA_WATKOW 5

void *zadanie_watku(void *arg) {
    int id = *((int *)arg);
    usleep(100);
    printf("Watek systemowy ID: %lu, Przeslany identyfikator: %d\n", (unsigned long)pthread_self(), id);
    return NULL;
}

int main() {
    pthread_t watki[LICZBA_WATKOW];
    int i;

    printf("--- WERSJA Z BLEDEM SYNCHRONIZACJI ---\n");
    for (i = 0; i < LICZBA_WATKOW; i++) {
        pthread_create(&watki[i], NULL, zadanie_watku, &i);
    }

    for (i = 0; i < LICZBA_WATKOW; i++) {
        pthread_join(watki[i], NULL);
    }

    return 0;
}
