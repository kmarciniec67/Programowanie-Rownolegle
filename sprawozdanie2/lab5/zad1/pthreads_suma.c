#include<stdlib.h>
#include<stdio.h>
#include<pthread.h>

#include"pomiar_czasu.h"

/* Rozmiar tablicy i liczba watkow: z linii polecen albo wartosci domyslne.
   Laboratorium prosi o porownanie m.in. 1000 i 100 000 000 elementow. */
int ROZMIAR = 100000000;
int LICZBA_W = 2;

pthread_mutex_t muteks;
pthread_t *watki;
double *global_array_of_local_sums;

void *suma_w( void *arg_wsk);
void *suma_w_no_mutex( void *arg_wsk);

double *tab;
double suma=0;

/* Podzial blokowy [start, koniec). Reszta z dzielenia trafia do pierwszych watkow,
   wiec przy ROZMIAR niepodzielnym przez LICZBA_W nikt nie wychodzi poza tablice
   i kazdy element jest dodany dokladnie raz. */
static void zakres_bloku(int moj_id, int *start, int *koniec)
{
  int bazowy = ROZMIAR / LICZBA_W;
  int reszta = ROZMIAR % LICZBA_W;
  if (moj_id < reszta) {
    *start = moj_id * (bazowy + 1);
    *koniec = *start + bazowy + 1;
  } else {
    *start = reszta * (bazowy + 1) + (moj_id - reszta) * bazowy;
    *koniec = *start + bazowy;
  }
}

int main( int argc, char *argv[] ){

  int i;

  double t_sekw, t_mutex, t_tablica;

  if (argc >= 2) ROZMIAR = atoi(argv[1]);
  if (argc >= 3) LICZBA_W = atoi(argv[2]);
  if (ROZMIAR < 1 || LICZBA_W < 1) {
    fprintf(stderr, "Uzycie: %s [rozmiar] [liczba_watkow]\n", argv[0]);
    return 1;
  }

  printf("ROZMIAR = %d, LICZBA_W = %d\n", ROZMIAR, LICZBA_W);

  int *indeksy = (int *) malloc(LICZBA_W * sizeof(int));
  watki = (pthread_t *) malloc(LICZBA_W * sizeof(pthread_t));
  global_array_of_local_sums = (double *) malloc(LICZBA_W * sizeof(double));
  if (indeksy == NULL || watki == NULL || global_array_of_local_sums == NULL) {
    fprintf(stderr, "Brak pamieci na struktury watkow\n");
    return 1;
  }
  for(i=0;i<LICZBA_W;i++) indeksy[i]=i;

  tab = (double *) malloc((size_t)ROZMIAR * sizeof(double));
  if (tab == NULL) {
    fprintf(stderr, "Brak pamieci na tablice (rozmiar %d)\n", ROZMIAR);
    return 1;
  }
  for(i=0; i<ROZMIAR; i++ ) tab[i] = ((double) i+1) / ROZMIAR;

  inicjuj_czas();

  printf("Obliczenia sekwencyjne\n");
  suma = 0;
  t_sekw = czas_zegara();
  for( i = 0; i < ROZMIAR; i++){
    suma += tab[i];
  }
  t_sekw = czas_zegara() - t_sekw;
  printf("suma = %.15lf\n", suma);
  printf("Czas obliczen sekwencyjnych = %lf\n", t_sekw);


  pthread_mutex_init( &muteks, NULL);

  printf("Poczatek tworzenia watkow\n");
  t_mutex = czas_zegara();

  suma =0;
  for(i=0; i<LICZBA_W; i++ )
    pthread_create( &watki[i], NULL, suma_w, (void *) &indeksy[i] );
  /* Przekazanie &i zamiast &indeksy[i] jest bledem synchronizacji:
     watki czytaja jedna wspolna zmienna sterujaca petli. */

  for(i=0; i<LICZBA_W; i++ ) pthread_join( watki[i], NULL );

  t_mutex = czas_zegara() - t_mutex;
  printf("suma = %.15lf\n", suma);
  printf("Czas obliczen %d watkow (mutex) = %lf\n", LICZBA_W, t_mutex);

  /* Wersja z tablica sum czastkowych: brak sekcji krytycznej.
     Watek glowny sam sklada wynik po join. */
  suma =0;
  printf("Poczatek tworzenia watkow\n");
  t_tablica = czas_zegara();

  for(i=0; i<LICZBA_W; i++ ) {
    global_array_of_local_sums[i]=0.0;
    pthread_create( &watki[i], NULL, suma_w_no_mutex, (void *) &indeksy[i] );
  }

  for(i=0; i<LICZBA_W; i++ ) {
    pthread_join( watki[i], NULL );
    suma += global_array_of_local_sums[i];
  }

  t_tablica = czas_zegara() - t_tablica;
  printf("suma = %.15lf\n", suma);
  printf("Czas obliczen %d watkow (tablica zamiast mutexa) = %lf\n",
	 LICZBA_W, t_tablica);

  if (t_mutex > 0.0)
    printf("Przyspieszenie mutex = %lf\n", t_sekw / t_mutex);
  if (t_tablica > 0.0)
    printf("Przyspieszenie tablica = %lf\n", t_sekw / t_tablica);

  pthread_mutex_destroy(&muteks);
  free(tab);
  free(indeksy);
  free(watki);
  free(global_array_of_local_sums);
  return 0;
}

void *suma_w( void *arg_wsk){

  int i, moj_id;
  int start, koniec;
  double moja_suma=0;

  moj_id = *( (int *) arg_wsk );

  zakres_bloku(moj_id, &start, &koniec);

  /* Dekompozycja blokowa petli: watek moj_id dostaje spojny zakres indeksow.
     Suma czastkowa jest prywatna. Do zmiennej wspolnej wchodzimy raz, pod mutexem. */
  for( i = start; i < koniec; i++){
    moja_suma += tab[i];
  }

  pthread_mutex_lock( &muteks );
  suma += moja_suma;
  pthread_mutex_unlock( &muteks );

  return NULL;
}

void *suma_w_no_mutex( void *arg_wsk){

  int i, moj_id;
  int start, koniec;

  moj_id = *( (int *) arg_wsk );

  double tmp=0.0;
  zakres_bloku(moj_id, &start, &koniec);
  for( i = start; i < koniec; i++){
    /* tmp jest prywatne. Zapis do tablicy globalnej dopiero na koncu,
       zeby watki nie przepychaly jednej linii cache przy kazdym dodaniu. */
    tmp+=tab[i];
  }

  global_array_of_local_sums[moj_id] = tmp;

  return NULL;
}
