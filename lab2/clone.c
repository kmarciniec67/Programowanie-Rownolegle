#define _GNU_SOURCE
#include<stdlib.h>
#include<stdio.h>
#include<unistd.h>

#include <sys/types.h>
#include <sys/wait.h>
#include <sched.h>
#include <linux/sched.h>
#include "pomiar_czasu.h"
int zmienna_globalna=0;

#define ROZMIAR_STOSU 1024*64

int funkcja_watku(void* argument) {
    // Alokacja 1 MB pamięci na stosie wątku
    char duza_tablica[1024 * 1024]; 
    duza_tablica[0] = 'A'; // Próba zapisu
    printf("Watek dziala\n");
    return 0;

}

int main()
{

  void *stos;
  pid_t pid;
  int i; 
  
  stos = malloc( ROZMIAR_STOSU );
  if (stos == 0) {
    printf("Proces nadrzędny - blad alokacji stosu\n");
    exit( 1 );
  }
  inicjuj_czas();
  for(i=0;i<1000;i++){

    pid = clone(&funkcja_watku, (void *)stos+ROZMIAR_STOSU, SIGCHLD, NULL);

    waitpid(pid, NULL, 0);

  }
  drukuj_czas();

  printf("zmienna globalna: %d\n",zmienna_globalna);
  free( stos );
}
