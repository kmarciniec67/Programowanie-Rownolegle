#define _GNU_SOURCE
#include <stdlib.h>
#include <stdio.h>
#include <unistd.h>
#include <sys/types.h>
#include <sys/wait.h>
#include <sched.h>
#include <linux/sched.h>

#define ROZMIAR_STOSU 1024*64
#define ITERACJE 100000

// Zmienna globalna (współdzielona)
int zmienna_globalna = 0;

int funkcja_watku(void* argument)
{
    // Rzutowanie przekazanego argumentu na wskaźnik do int
    int* zmienna_lokalna = (int*)argument;
    int i;
    
    // Zwiększanie zmiennych w pętli
    for (i = 0; i < ITERACJE; i++) {
        zmienna_globalna++;
        (*zmienna_lokalna)++;
    }
    
    // Wypisanie wartości po zakończeniu pętli w wątku
    printf("Watek - zmienna globalna: %d\n", zmienna_globalna);
    printf("Watek - zmienna lokalna: %d\n\n", *zmienna_lokalna);
    
    return 0;
}

int main()
{
    void *stos1, *stos2;
    pid_t pid1, pid2;
    
    // Zmienne lokalne w funkcji main, których adresy przekażemy do wątków
    int lokalna1 = 0;
    int lokalna2 = 0;

    stos1 = malloc(ROZMIAR_STOSU);
    stos2 = malloc(ROZMIAR_STOSU);

    if (stos1 == 0 || stos2 == 0) {
        printf("Blad alokacji stosu\n");
        exit(1);
    }

    // Tworzenie pierwszego wątku
    pid1 = clone(&funkcja_watku, (void *)stos1 + ROZMIAR_STOSU,
                 CLONE_FS | CLONE_FILES | CLONE_SIGHAND | CLONE_VM, &lokalna1);
    
    // Tworzenie drugiego wątku BEZ czekania na pierwszy
    pid2 = clone(&funkcja_watku, (void *)stos2 + ROZMIAR_STOSU,
                 CLONE_FS | CLONE_FILES | CLONE_SIGHAND | CLONE_VM, &lokalna2);

    // Oczekiwanie na zakończenie obu wątków DOPIERO po ich utworzeniu
    waitpid(pid1, NULL, __WCLONE);
    waitpid(pid2, NULL, __WCLONE);

    // Wypisanie wartości w funkcji main po zakończeniu obu wątków
    printf("Funkcja main - po zakonczeniu watkow:\n");
    printf("Main - zmienna globalna (oczekiwane %d): %d\n", 2 * ITERACJE, zmienna_globalna);
    printf("Main - zmienna lokalna 1 (oczekiwane %d): %d\n", ITERACJE, lokalna1);
    printf("Main - zmienna lokalna 2 (oczekiwane %d): %d\n", ITERACJE, lokalna2);

    free(stos1);
    free(stos2);
    
    return 0;
}
