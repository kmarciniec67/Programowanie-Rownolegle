#include<stdio.h>
#include<stdlib.h>
#include<math.h>
#include"pomiar_czasu.h"

#ifndef M_PI // standardy C99 i C11 nie wymagają definiowania stałej M_PI
#define M_PI (3.14159265358979323846)
#endif


double funkcja ( double x );

double funkcja ( double x ){ return( sin(x) ); }

double calka_sekw(double a, double b, double dx);

double calka_zrownoleglenie_petli(double a, double b, double dx, int l_w);

double calka_dekompozycja_obszaru(double a, double b, double dx, int l_w);

/* wariant: 0 cykliczna, 1 blokowa; redukcja: 0 mutex, 1 tablica sum */
void ustaw_parametry_petli(int wariant, int redukcja);


int main( int argc, char *argv[] ){

  double t1, t2, t3;

  double a, b, dx, calka_s, calka_p, calka_o;

  int l_w;
  int wariant = 0;
  int redukcja = 0;

  printf("\nProgram obliczania calki z funkcji (sinus) metoda trapezow.\n");
  printf("Przedzial [0, pi]. Wartosc dokladna = 2.\n");

  a = 0.0;
  b = M_PI;

  if (argc >= 3) {
    dx = atof(argv[1]);
    l_w = atoi(argv[2]);
    if (argc >= 4) wariant = atoi(argv[3]);
    if (argc >= 5) redukcja = atoi(argv[4]);
  } else {
    printf("\nPodaj wysokosc pojedynczego trapezu:  ");
    if (scanf("%lf", &dx) != 1) return 1;
    printf("\nPodaj liczbe watkow:  ");
    if (scanf("%d", &l_w) != 1) return 1;
  }

  if (dx <= 0.0 || l_w < 1 || (wariant != 0 && wariant != 1) ||
      (redukcja != 0 && redukcja != 1)) {
    fprintf(stderr,
            "Uzycie: %s <dx> <liczba_watkow> [0=cykliczna|1=blokowa] [0=mutex|1=tablica]\n",
            argv[0]);
    return 1;
  }

  ustaw_parametry_petli(wariant, redukcja);

  printf("Parametry: dx = %.12g, watki = %d, petla = %s, redukcja = %s\n",
         dx, l_w,
         wariant == 0 ? "cykliczna" : "blokowa",
         redukcja == 0 ? "mutex" : "tablica");

  inicjuj_czas();

  printf("\nPoczatek obliczen sekwencyjnych\n");
  t1 = czas_zegara();

  calka_s = calka_sekw(a, b, dx);

  t1 = czas_zegara() - t1;
  printf("\nKoniec obliczen sekwencyjnych\n");
  printf("\tCzas wykonania %lf. \tObliczona calka = %.15lf\n", t1, calka_s);
  printf("\t|calka - 2| = %.3e\n", fabs(calka_s - 2.0));

  printf("\nPoczatek obliczen rownoleglych (zrownoleglenie petli)\n");
  t2 = czas_zegara();

  calka_p = calka_zrownoleglenie_petli(a, b, dx, l_w);

  t2 = czas_zegara() - t2;
  printf("\nKoniec obliczen rownoleglych (zrownoleglenie petli) \n");
  printf("\tCzas wykonania %lf. \tObliczona calka = %.15lf\n", t2, calka_p);
  printf("\t|petla - sekw| = %.3e\n", fabs(calka_p - calka_s));
  printf("\t|petla - 2| = %.3e\n", fabs(calka_p - 2.0));

  printf("\nPoczatek obliczen rownoleglych (dekompozycja obszaru)\n");
  t3 = czas_zegara();

  calka_o = calka_dekompozycja_obszaru(a, b, dx, l_w);

  t3 = czas_zegara() - t3;
  printf("\nKoniec obliczen rownoleglych (dekompozycja obszaru) \n");
  printf("\tCzas wykonania %lf. \tObliczona calka = %.15lf\n", t3, calka_o);
  printf("\t|obszar - sekw| = %.3e\n", fabs(calka_o - calka_s));
  printf("\t|obszar - 2| = %.3e\n", fabs(calka_o - 2.0));

  printf("\nPODSUMOWANIE dx=%.12g watki=%d wariant=%d redukcja=%d\n",
         dx, l_w, wariant, redukcja);
  printf("PODSUMOWANIE sekw %.15lf %lf | petla %.15lf %lf | obszar %.15lf %lf\n",
         calka_s, t1, calka_p, t2, calka_o, t3);

  return 0;
}

double calka_sekw(double a, double b, double dx){

  int N = ceil((b-a)/dx);
  double dx_adjust = (b-a)/N;

  printf("Obliczona liczba trapezow: N = %d, dx_adjust = %.15lf\n", N, dx_adjust);
  int i;
  double calka = 0.0;
  for(i=0; i<N; i++){

    double x1 = a + i*dx_adjust;
    calka += 0.5*dx_adjust*(funkcja(x1)+funkcja(x1+dx_adjust));

  }

  return(calka);
}
