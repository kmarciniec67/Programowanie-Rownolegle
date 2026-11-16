// Wariant 2. Runnable, blokowa dekompozycja alfabetu.
// Watek dostaje spojny zakres znakow i wypisuje je po kolei.
// Kazda linia jest osobnym wywolaniem synchronizowanej metody, wiec
// miedzy znakami tego samego watku moze wejsc linia innego watku,
// ale pojedynczy pasek nie jest rozrywany.
class WatekZnakow implements Runnable {

    private final int id;
    private final int nWatkow;
    private final Obraz obraz;

    public WatekZnakow(int id, int nWatkow, Obraz obraz) {
        this.id = id;
        this.nWatkow = nWatkow;
        this.obraz = obraz;
    }

    @Override
    public void run() {
        int start = obraz.poczatek_bloku(id, nWatkow, obraz.getLiczbaSymboli());
        int end = obraz.koniec_bloku(id, nWatkow, obraz.getLiczbaSymboli());
        if (start == end) {
            obraz.print_pusty_przydzial(id);
            return;
        }
        obraz.calculate_histogram_blok_znakow(start, end);
        for (int k = start; k < end; k++) {
            obraz.print_histogram_znak_watku(id, k);
        }
    }
}
