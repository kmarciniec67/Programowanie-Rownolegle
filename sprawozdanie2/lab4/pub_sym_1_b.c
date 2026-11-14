#include <stdlib.h>
#include <stdio.h>
#include <time.h>
#include <pthread.h>
#include <unistd.h>

// Zwiększamy liczbę iteracji zgodnie z instrukcją, np. do 10000
#define ILE_MUSZE_WYPIC 10000 

int wolne_kufle;
int poczatkowe_kufle;

void * watek_klient (void * arg);

int main( void ){
  pthread_t *tab_klient;
  int *tab_klient_id;
  int l_kl, l_kf, i;

  printf("\nLiczba klientow: "); scanf("%d", &l_kl);
  printf("Liczba kufli: "); scanf("%d", &l_kf);

  wolne_kufle = l_kf;
  poczatkowe_kufle = l_kf;

  tab_klient = (pthread_t *) malloc(l_kl*sizeof(pthread_t));
  tab_klient_id = (int *) malloc(l_kl*sizeof(int));
  for(i=0; i<l_kl; i++) tab_klient_id[i] = i;

  printf("\nOtwieramy pub!\n");
  printf("Poczatkowa liczba kufli: %d\n", wolne_kufle);

  for(i=0; i<l_kl; i++){
    pthread_create(&tab_klient[i], NULL, watek_klient, &tab_klient_id[i]); 
  }
  for(i=0; i<l_kl; i++){
    pthread_join(tab_klient[i], NULL);
  }

  // Weryfikacja błędu 4a
  printf("\nKoncowa liczba kufli: %d\n", wolne_kufle);
  if (wolne_kufle != poczatkowe_kufle) {
      printf("BLAD (4a): Liczba kufli w pubie zmienila sie! (Start: %d, Koniec: %d)\n", poczatkowe_kufle, wolne_kufle);
  } else {
      printf("OK (4a): Liczba kufli zgadza sie.\n");
  }

  free(tab_klient);
  free(tab_klient_id);
  return 0;
}

void * watek_klient (void * arg_wsk){
  int moj_id = * ((int *)arg_wsk);
  int i;

  for(i=0; i<ILE_MUSZE_WYPIC; i++){
    // POBRANIE KUFLA (niebezpieczne - bez muteksu)
    wolne_kufle--;
    
    // Weryfikacja błędu 4b
    if (wolne_kufle < 0) {
        // Wypisujemy tylko przy błędzie, żeby nie zwalniać pętli
        printf("BLAD (4b): Klient %d pobral kufel, mimo braku wolnych! (wolne_kufle = %d)\n", moj_id, wolne_kufle);
    }

    // Minimalne opóźnienie zgodnie ze wskazówką
    usleep(1); 

    // ODDANIE KUFLA (niebezpieczne - bez muteksu)
    wolne_kufle++;
  }
  return(NULL);
}
