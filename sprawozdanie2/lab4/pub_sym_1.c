#include <stdlib.h>
#include <stdio.h>
#include <time.h>
#include <pthread.h>
#include <unistd.h>

#define ILE_MUSZE_WYPIC 3

// Zmienne globalne reprezentujące zasoby pubu
int wolne_kufle;
int poczatkowe_kufle;

// Muteks chroniący dostęp do zmiennej wolne_kufle
pthread_mutex_t kufle_mutex = PTHREAD_MUTEX_INITIALIZER;

void * watek_klient (void * arg);

int main( void ){

  pthread_t *tab_klient;
  int *tab_klient_id;

  int l_kl, l_kf, i;

  printf("\nLiczba klientow: "); 
  if(scanf("%d", &l_kl) != 1) return 1;

  printf("Liczba kufli: "); 
  if(scanf("%d", &l_kf) != 1) return 1;

  wolne_kufle = l_kf;
  poczatkowe_kufle = l_kf;

  tab_klient = (pthread_t *) malloc(l_kl * sizeof(pthread_t));
  tab_klient_id = (int *) malloc(l_kl * sizeof(int));
  for(i = 0; i < l_kl; i++) {
      tab_klient_id[i] = i;
  }

  printf("\nOtwieramy pub (aktywne czekanie - 5c)!\n");
  printf("Liczba wolnych kufli na starcie: %d\n\n", wolne_kufle); 

  // Tworzenie wątków klientów
  for(i = 0; i < l_kl; i++){
    pthread_create(&tab_klient[i], NULL, watek_klient, &tab_klient_id[i]); 
  }

  // Oczekiwanie na wyjście wszystkich klientów
  for(i = 0; i < l_kl; i++){
    pthread_join(tab_klient[i], NULL);
  }

  // Sprawdzenie warunku 4a przed zamknięciem pubu
  printf("\n--- WERYFIKACJA KONCOWA ---");
  printf("\nKoncowa liczba kufli: %d\n", wolne_kufle);
  if (wolne_kufle != poczatkowe_kufle) {
      printf("BLAD (4a): Calkowita liczba kufli w pubie zmienila sie! (Start: %d, Koniec: %d)\n", poczatkowe_kufle, wolne_kufle);
  } else {
      printf("OK (4a): Liczba kufli zgadza sie z poczatkowa.\n");
  }

  printf("\nZamykamy pub!\n");

  free(tab_klient);
  free(tab_klient_id);
  
  return 0;
}

void * watek_klient (void * arg_wsk){

  int moj_id = * ((int *)arg_wsk);
  int i;
  int ile_musze_wypic = ILE_MUSZE_WYPIC;
  long int wykonana_praca = 0;

  printf("Klient %d, wchodzę do pubu\n", moj_id); 
    
  for(i = 0; i < ile_musze_wypic; i++){

    int success = 0;
    do {
        pthread_mutex_lock(&kufle_mutex); // Wejście do sekcji krytycznej
        
        if (wolne_kufle > 0) {
            wolne_kufle--;
            success = 1;
            
            
            if (wolne_kufle < 0) {
                printf("BLAD (4b): Klient %d pobral kufel, mimo braku wolnych! (wolne_kufle = %d)\n", moj_id, wolne_kufle);
            }
            
            printf("Klient %d, wybieram kufel (pozostalo wolnych: %d)\n", moj_id, wolne_kufle); 
        }
        
        pthread_mutex_unlock(&kufle_mutex);

        if (success == 0) {
            wykonana_praca++;
            usleep(200000);
        }
    } while (success == 0);

    //NALEWANIE
    printf("Klient %d, nalewam z kranu\n", moj_id); 
    sleep(1);
    
    //Picie
    printf("Klient %d, pije %d. kufel\n", moj_id, i+1); 
    sleep(3);
    
    //ODDAWANIE KUFLA
    pthread_mutex_lock(&kufle_mutex);
    wolne_kufle++;
    printf("Klient %d, odkladam kufel (dostepnych wolnych: %d)\n", moj_id, wolne_kufle); 
    pthread_mutex_unlock(&kufle_mutex);
    
  }

  printf("Klient %d, wychodzę z pubu; wykonana praca (cykle oczekiwania): %ld\n", moj_id, wykonana_praca); 
    
  return(NULL);
}
