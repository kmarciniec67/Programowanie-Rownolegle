import java.util.Random;

class Obraz {

    private int size_n;
    private int size_m;
    private int liczba_symboli;
    private char[][] tab;
    private char[] tab_symb;
    private int[] histogram;
    private int[] hist_parallel;
    private int[][] hist_czesciowe;

    public Obraz(int n, int m) {
        this(n, m, 94, System.nanoTime());
    }

    public Obraz(int n, int m, int liczba_symboli, long ziarno) {
        if (n < 1 || m < 1) {
            throw new IllegalArgumentException("Rozmiar obrazu musi byc dodatni.");
        }
        if (liczba_symboli < 1 || liczba_symboli > 94) {
            throw new IllegalArgumentException("Liczba symboli musi byc z zakresu 1..94.");
        }

        this.size_n = n;
        this.size_m = m;
        this.liczba_symboli = liczba_symboli;
        tab = new char[n][m];
        tab_symb = new char[liczba_symboli];

        final Random random = new Random(ziarno);

        for (int k = 0; k < liczba_symboli; k++) {
            tab_symb[k] = (char) (k + 33);
        }

        for (int i = 0; i < n; i++) {
            for (int j = 0; j < m; j++) {
                tab[i][j] = tab_symb[random.nextInt(liczba_symboli)];
                System.out.print(tab[i][j] + " ");
            }
            System.out.print("\n");
        }
        System.out.print("\n\n");

        histogram = new int[liczba_symboli];
        hist_parallel = new int[liczba_symboli];
        clear_histogram();
        clear_histogram_parallel();
    }

    public int getLiczbaSymboli() {
        return liczba_symboli;
    }

    public int getWiersze() {
        return size_n;
    }

    public int getKolumny() {
        return size_m;
    }

    public char getSymbol(int indeks) {
        return tab_symb[indeks];
    }

    public int poczatek_bloku(int id, int nWatkow, int rozmiar) {
        int base = rozmiar / nWatkow;
        int rem = rozmiar % nWatkow;
        return id * base + Math.min(id, rem);
    }

    public int koniec_bloku(int id, int nWatkow, int rozmiar) {
        int base = rozmiar / nWatkow;
        int rem = rozmiar % nWatkow;
        int start = id * base + Math.min(id, rem);
        int len = base + (id < rem ? 1 : 0);
        return start + len;
    }

    public void clear_histogram() {
        for (int i = 0; i < liczba_symboli; i++) histogram[i] = 0;
    }

    public void clear_histogram_parallel() {
        for (int i = 0; i < liczba_symboli; i++) hist_parallel[i] = 0;
    }

    public void calculate_histogram() {
        for (int i = 0; i < size_n; i++) {
            for (int j = 0; j < size_m; j++) {
                for (int k = 0; k < liczba_symboli; k++) {
                    if (tab[i][j] == tab_symb[k]) histogram[k]++;
                }
            }
        }
    }

    public void print_histogram() {
        for (int i = 0; i < liczba_symboli; i++) {
            System.out.print(tab_symb[i] + " " + histogram[i] + "\n");
        }
    }

    // Wspolny wzorzec dla dekompozycji po znakach. Kazdy watek zapisuje inne
    // indeksy hist_parallel, wiec sam przyrost nie jest synchronizowany.
    public void calculate_histogram_wzorzec(int start_wiersz, int end_wiersz, int skok_wiersz,
                                            int start_kol, int end_kol, int skok_kol,
                                            int start_znak, int end_znak, int skok_znak) {
        for (int i = start_wiersz; i < end_wiersz; i += skok_wiersz) {
            for (int j = start_kol; j < end_kol; j += skok_kol) {
                for (int k = start_znak; k < end_znak; k += skok_znak) {
                    if (tab[i][j] == tab_symb[k]) hist_parallel[k]++;
                }
            }
        }
    }

    public void calculate_histogram_znak(int znak) {
        calculate_histogram_wzorzec(0, size_n, 1, 0, size_m, 1, znak, znak + 1, 1);
    }

    public void calculate_histogram_blok_znakow(int start, int end) {
        calculate_histogram_wzorzec(0, size_n, 1, 0, size_m, 1, start, end, 1);
    }

    // Jedna linia na raz. Synchronizacja chroni wydruk, nie licznik:
    // dwa watki nie moga rozerwac paska '=' w polowie linii.
    public synchronized void print_histogram_znak_watku(int numerWatku, int znak) {
        StringBuilder sb = new StringBuilder();
        sb.append("Watek ");
        sb.append(numerWatku + 1);
        sb.append(":   ");
        sb.append(tab_symb[znak]);
        sb.append(' ');
        for (int c = 0; c < hist_parallel[znak]; c++) {
            sb.append('=');
        }
        System.out.println(sb.toString());
    }

    public synchronized void print_pusty_przydzial(int numerWatku) {
        System.out.println("Watek " + (numerWatku + 1) + ":   (pusty przydzial)");
    }

    public void przygotuj_histogramy_czesciowe(int nWatkow) {
        hist_czesciowe = new int[nWatkow][liczba_symboli];
        clear_histogram_parallel();
    }

    // Dekompozycja obrazu. Watek pisze wylacznie do wlasnego wiersza
    // hist_czesciowe[id], wiec liczniki nie wymagaja blokady.
    // Indeks liczony bezposrednio z kodu znaku, bo alfabet jest ciagly od 33.
    public void calculate_fragment_obrazu(int id,
                                          int startW, int endW, int skokW,
                                          int startK, int endK, int skokK) {
        for (int i = startW; i < endW; i += skokW) {
            for (int j = startK; j < endK; j += skokK) {
                hist_czesciowe[id][(int) tab[i][j] - 33]++;
            }
        }
    }

    public void calculate_wiersze_cykliczne(int id, int nWatkow) {
        calculate_fragment_obrazu(id, id, size_n, nWatkow, 0, size_m, 1);
    }

    public void calculate_wiersze_blokowo(int id, int nWatkow) {
        int start = poczatek_bloku(id, nWatkow, size_n);
        int end = koniec_bloku(id, nWatkow, size_n);
        calculate_fragment_obrazu(id, start, end, 1, 0, size_m, 1);
    }

    public void calculate_kolumny_blokowo(int id, int nWatkow) {
        int start = poczatek_bloku(id, nWatkow, size_m);
        int end = koniec_bloku(id, nWatkow, size_m);
        calculate_fragment_obrazu(id, 0, size_n, 1, start, end, 1);
    }

    // Redukcja: histogram calego obrazu to suma histogramow kawalkow.
    // Wolana z watku glownego po join, wiec bez synchronizacji.
    public void scal_histogramy_czesciowe() {
        for (int t = 0; t < hist_czesciowe.length; t++) {
            for (int k = 0; k < liczba_symboli; k++) {
                hist_parallel[k] += hist_czesciowe[t][k];
            }
        }
    }

    public void print_histogramy_czesciowe() {
        for (int t = 0; t < hist_czesciowe.length; t++) {
            System.out.print("Czesciowy watek " + (t + 1) + ":");
            boolean cokolwiek = false;
            for (int k = 0; k < liczba_symboli; k++) {
                if (hist_czesciowe[t][k] == 0) continue;
                cokolwiek = true;
                System.out.print(" " + tab_symb[k] + "=" + hist_czesciowe[t][k]);
            }
            if (!cokolwiek) System.out.print(" (pusto)");
            System.out.println();
        }
    }

    public void print_histogram_parallel() {
        for (int i = 0; i < liczba_symboli; i++) {
            System.out.print(tab_symb[i] + " " + hist_parallel[i] + " ");
            for (int c = 0; c < hist_parallel[i]; c++) System.out.print("=");
            System.out.println();
        }
    }

    public void wypisz_porownanie() {
        int sumaSek = 0;
        int sumaPar = 0;
        boolean ok = true;
        for (int i = 0; i < liczba_symboli; i++) {
            sumaSek += histogram[i];
            sumaPar += hist_parallel[i];
            if (histogram[i] != hist_parallel[i]) {
                ok = false;
                System.out.println("Roznica przy " + tab_symb[i]
                        + ": sekw=" + histogram[i] + " par=" + hist_parallel[i]);
            }
        }
        System.out.println("Suma sekwencyjna = " + sumaSek
                + ", suma rownolegla = " + sumaPar
                + ", pikseli = " + (size_n * size_m));
        if (ok && sumaSek == size_n * size_m) {
            System.out.println("Wynik rownolegly ZGODNY z sekwencyjnym.");
        } else {
            System.out.println("BLAD: histogramy sie roznia.");
        }
    }
}
