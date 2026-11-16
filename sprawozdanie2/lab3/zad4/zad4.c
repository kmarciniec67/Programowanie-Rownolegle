#define _GNU_SOURCE
#include <stdio.h>
#include <stdlib.h>
#include <pthread.h>
#include <unistd.h>

void *zadanie_watku(void *arg) {
    pthread_attr_t attr;
    size_t stack_size;
    void *stack_addr;

    // Odczyt atrybutów działającego wątku (funkcja GNU)
    pthread_getattr_np(pthread_self(), &attr);
    pthread_attr_getstack(&attr, &stack_addr, &stack_size);

    printf("Watek potomny odczytal swoje atrybuty:\n");
    printf(" - Adres stosu: %p\n", stack_addr);
    printf(" - Rozmiar stosu: %zu bajtów (ok. %zu MB)\n", stack_size, stack_size / (1024 * 1024));

    pthread_attr_destroy(&attr);

    // Test alokacji dużej tablicy na stosie (wymaga kompilacji z -g)
    // int tab[5000000] = {0}; 
    // printf("Alokacja dużej tablicy powiodła się!\n");

    return NULL;
}

int main() {
    pthread_t tid;
    pthread_attr_t attr;
    size_t nowy_rozmiar_stosu = 1024 * 1024 * 4; // 4 MB

    // 1. Inicjalizacja atrybutów
    pthread_attr_init(&attr);

    // 2. Ustawienie niestandardowego rozmiaru stosu
    pthread_attr_setstacksize(&attr, nowy_rozmiar_stosu);

    printf("Wątek główny: tworzenie wątku z niestandardowym stosem (4 MB)...\n");

    // 3. Utworzenie wątku z przygotowanymi atrybutami
    pthread_create(&tid, &attr, zadanie_watku, NULL);

    // 4. Oczekiwanie na zakończenie wątku
    pthread_join(tid, NULL);

    // 5. Usunięcie struktury atrybutów
    pthread_attr_destroy(&attr);

    return 0;
}
