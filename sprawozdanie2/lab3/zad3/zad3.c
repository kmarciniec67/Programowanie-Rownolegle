#include <stdio.h>
#include <stdlib.h>
#include <pthread.h>

#define LICZBA_WATKOW 4

typedef struct {
    double a;
    double b;
    double wynik;
} dane_watku_t;

void* zadanie_watku(void* arg)
{
    dane_watku_t* dane = (dane_watku_t*)arg;
    dane->wynik = (dane->a + dane->b) / 2.0;
    printf("Watek: fragment (%lf, %lf), srodek = %lf\n", dane->a, dane->b, dane->wynik);
    return NULL;
}

int main()
{
    pthread_t tid[LICZBA_WATKOW];
    dane_watku_t dane[LICZBA_WATKOW];
    double a = 0.0, b = 1.0;
    double dlugosc = (b - a) / LICZBA_WATKOW;
    double suma = 0.0;
    int i;

    for (i = 0; i < LICZBA_WATKOW; i++) {
        dane[i].a = a + i * dlugosc;
        dane[i].b = a + (i + 1) * dlugosc;
        dane[i].wynik = 0.0;
        pthread_create(&tid[i], NULL, zadanie_watku, &dane[i]);
    }

    for (i = 0; i < LICZBA_WATKOW; i++) {
        pthread_join(tid[i], NULL);
        suma += dane[i].wynik;
    }

    printf("Suma wynikow = %lf\n", suma);
    return 0;
}
