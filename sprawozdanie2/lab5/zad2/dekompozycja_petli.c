#include<stdio.h>
#include<stdlib.h>
#include<pthread.h>
#include<math.h>

double funkcja ( double x );

double calka_zrownoleglenie_petli(double a, double b, double dx, int l_w);

/* 0 = dekompozycja cykliczna, 1 = dekompozycja blokowa */
static int wariant_global = 0;
/* 0 = redukcja pod mutexem, 1 = tablica sum czastkowych, skladana po join */
static int redukcja_global = 0;

static int l_w_global=0;

static double calka_global=0.0;
static double a_global;
static double dx_global;
static int N_global;

static pthread_mutex_t muteks_calka = PTHREAD_MUTEX_INITIALIZER;
static double *sumy_watkow = NULL;

void* calka_fragment_petli_w(void* arg_wsk);

void ustaw_parametry_petli(int wariant, int redukcja)
{
  wariant_global = wariant;
  redukcja_global = redukcja;
}

double calka_zrownoleglenie_petli(double a, double b, double dx, int l_w){

  int N = ceil((b-a)/dx);
  double dx_adjust = (b-a)/N;

  printf("Obliczona liczba trapezow: N = %d, dx_adjust = %.15lf\n", N, dx_adjust);
  printf("Dekompozycja petli: %s, redukcja: %s\n",
         wariant_global == 0 ? "cykliczna" : "blokowa",
         redukcja_global == 0 ? "mutex" : "tablica");

  a_global = a;
  dx_global = dx_adjust;
  N_global = N;
  l_w_global = l_w;
  calka_global = 0.0;

  pthread_t *watki = (pthread_t *) malloc((size_t)l_w * sizeof(pthread_t));
  int *indeksy = (int *) malloc((size_t)l_w * sizeof(int));
  if (watki == NULL || indeksy == NULL) {
    fprintf(stderr, "Brak pamieci na watki petli\n");
    exit(1);
  }

  if (redukcja_global == 1) {
    sumy_watkow = (double *) calloc((size_t)l_w, sizeof(double));
    if (sumy_watkow == NULL) {
      fprintf(stderr, "Brak pamieci na sumy czastkowe\n");
      exit(1);
    }
  }

  int i;
  for (i = 0; i < l_w; i++) {
    indeksy[i] = i;
    pthread_create(&watki[i], NULL, calka_fragment_petli_w, (void *) &indeksy[i]);
  }

  for (i = 0; i < l_w; i++) pthread_join(watki[i], NULL);

  double wynik;
  if (redukcja_global == 1) {
    wynik = 0.0;
    for (i = 0; i < l_w; i++) wynik += sumy_watkow[i];
    free(sumy_watkow);
    sumy_watkow = NULL;
  } else {
    wynik = calka_global;
  }

  free(watki);
  free(indeksy);
  return wynik;
}

void* calka_fragment_petli_w(void* arg_wsk){

  int my_id = *( (int *) arg_wsk );

  /* Wariant z samych identyfikatorem wymusza zmienne wspolne.
     Kazdy watek czyta te same a, dx, N i liczbe watkow. */
  double a = a_global;
  double dx = dx_global;
  int N = N_global;
  int l_w = l_w_global;

  int my_start;
  int my_end;
  int my_stride;

  if (wariant_global == 0) {
    /* Cyklicznie: watek k bierze iteracje k, k+l_w, k+2*l_w, ...
       Gdy N nie dzieli sie przez l_w, czesc watkow dostaje o jedna iteracje wiecej. */
    my_start = my_id;
    my_end = N;
    my_stride = l_w;
  } else {
    /* Blokowo: spojny przedzial indeksow. Reszta N % l_w rozchodzi sie po 1. */
    int chunk = N / l_w;
    int reszta = N % l_w;
    if (my_id < reszta) {
      my_start = my_id * (chunk + 1);
      my_end = my_start + chunk + 1;
    } else {
      my_start = reszta * (chunk + 1) + (my_id - reszta) * chunk;
      my_end = my_start + chunk;
    }
    my_stride = 1;
  }

  printf("Watek %d: my_start %d, my_end %d, my_stride %d\n",
	 my_id, my_start, my_end, my_stride);

  int i;
  double calka = 0.0;
  for(i=my_start; i<my_end; i+=my_stride){

    double x1 = a + i*dx;
    calka += 0.5*dx*(funkcja(x1)+funkcja(x1+dx));

  }

  if (redukcja_global == 0) {
    pthread_mutex_lock(&muteks_calka);
    calka_global += calka;
    pthread_mutex_unlock(&muteks_calka);
  } else {
    sumy_watkow[my_id] = calka;
  }

  return NULL;
}
