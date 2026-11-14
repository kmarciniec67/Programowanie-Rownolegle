#include<stdio.h>
#include<stdlib.h>
#include<pthread.h>

double calka_sekw(double a, double b, double dx);

double calka_dekompozycja_obszaru(double a, double b, double dx, int l_w);

void* calka_podobszar_w(void* arg_wsk);

/* Argument watku: granice podobszaru, zadane dx i pole na wynik.
   Watek nie potrzebuje mutexa. Wynik wraca w tej samej strukturze. */
typedef struct {
  int id;
  double a_local;
  double b_local;
  double dx;
  double wynik;
} zadanie_t;

double calka_dekompozycja_obszaru(double a, double b, double dx, int l_w){

  double calka_suma_local = 0.0;
  double szerokosc = (b - a) / (double) l_w;

  zadanie_t *zadania = (zadanie_t *) malloc((size_t)l_w * sizeof(zadanie_t));
  pthread_t *watki = (pthread_t *) malloc((size_t)l_w * sizeof(pthread_t));
  if (zadania == NULL || watki == NULL) {
    fprintf(stderr, "Brak pamieci na dekompozycje obszaru\n");
    exit(1);
  }

  int i;
  for (i = 0; i < l_w; i++) {
    zadania[i].id = i;
    zadania[i].a_local = a + i * szerokosc;
    /* Ostatni kraniec domykamy do b, zeby suma podobszarow pokryla [a, b]. */
    zadania[i].b_local = (i == l_w - 1) ? b : a + (i + 1) * szerokosc;
    zadania[i].dx = dx;
    zadania[i].wynik = 0.0;
    pthread_create(&watki[i], NULL, calka_podobszar_w, (void *) &zadania[i]);
  }

  for (i = 0; i < l_w; i++) {
    pthread_join(watki[i], NULL);
    calka_suma_local += zadania[i].wynik;
  }

  free(zadania);
  free(watki);
  return(calka_suma_local);
}



void* calka_podobszar_w(void* arg_wsk){

  zadanie_t *zadanie = (zadanie_t *) arg_wsk;

  printf("Watek %d: a_local %.15lf, b_local %.15lf, dx %.12lf\n",
	 zadanie->id, zadanie->a_local, zadanie->b_local, zadanie->dx);

  /* Ten sam algorytm co calka_sekw w main, tylko na krancach podobszaru.
     N i dx_adjust powstaja wewnatrz calka_sekw, osobno dla tego odcinka. */
  zadanie->wynik = calka_sekw(zadanie->a_local, zadanie->b_local, zadanie->dx);
  return NULL;
}
