#include <stdio.h>

int main(int argc, char *argv[]) {
    if (argc > 1) {
        printf("Kacper Marciniec - uruchomiono przez: %s\n", argv[1]);
    } else {
        printf("Kacper Marciniec - uruchomiono bez argumentow\n");
    }
    return 0;
}
