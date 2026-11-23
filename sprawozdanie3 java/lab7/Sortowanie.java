import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.ForkJoinPool;

// javac -encoding UTF-8 scal_tab.java Sortowanie.java
// java Sortowanie [n] [prog] [liczba_watkow]
//
// Sortowanie przez scalanie na ForkJoinPool.
// prog = maksymalny rozmiar fragmentu sortowanego bez dalszego podzialu.
public class Sortowanie {

    public static void main(String[] args) {
        int n = 1_000_000;
        int prog = 10_000;
        int watki = Runtime.getRuntime().availableProcessors();

        if (args.length >= 1) {
            n = Integer.parseInt(args[0]);
        }
        if (args.length >= 2) {
            prog = Integer.parseInt(args[1]);
        }
        if (args.length >= 3) {
            watki = Integer.parseInt(args[2]);
        }
        if (args.length > 3) {
            System.out.println("Uzycie: java Sortowanie [n] [prog] [liczba_watkow]");
            return;
        }
        if (n < 0 || prog < 1 || watki < 1) {
            throw new IllegalArgumentException("n >= 0, prog >= 1, liczba watkow >= 1.");
        }

        System.out.println("Sortowanie przez scalanie, ForkJoinPool");
        System.out.println("n = " + n + ", prog = " + prog + ", watki = " + watki);
        System.out.println("Dostepne procesory: " + Runtime.getRuntime().availableProcessors());

        System.out.println();
        System.out.println("--- Testy malych tablic ---");
        sprawdz(new int[] {8, 3, 5, 1, 9, 2, 7, 4}, 3, 2);
        sprawdz(new int[] {5, 1, 4, 2, 3, 3, 0, -1}, 1, 2);
        sprawdz(new int[] {}, 1, 2);
        sprawdz(new int[] {7}, 1, 2);
        sprawdz(new int[] {2, 1}, 1, 2);
        sprawdz(new int[] {1, 1, 1, 1}, 2, 2);
        System.out.println("Male tablice: OK");

        int[] dane = new int[n];
        Random random = new Random(42);
        for (int i = 0; i < n; i++) {
            dane[i] = random.nextInt();
        }

        int rozgrzewka = Math.min(n, 200_000);
        System.out.println();
        System.out.println("Rozgrzewanie JVM (n = " + rozgrzewka + ")...");
        int[] krotkie = Arrays.copyOf(dane, rozgrzewka);
        DivideTask.sortujSekwencyjnie(krotkie);
        ForkJoinPool rozgrzewkaPula = new ForkJoinPool(watki);
        try {
            rozgrzewkaPula.invoke(new DivideTask(krotkie, prog));
        } finally {
            rozgrzewkaPula.shutdown();
        }

        System.out.println();
        System.out.println("--- Pomiar n = " + n + " ---");

        long t0 = System.nanoTime();
        int[] sekw = DivideTask.sortujSekwencyjnie(dane);
        long t1 = System.nanoTime();

        long t2 = System.nanoTime();
        ForkJoinPool pool = new ForkJoinPool(watki);
        int[] rown;
        try {
            rown = pool.invoke(new DivideTask(dane, prog));
        } finally {
            pool.shutdown();
        }
        long t3 = System.nanoTime();

        double czasSek = (t1 - t0) / 1e6;
        double czasRown = (t3 - t2) / 1e6;

        boolean sekwOk = zgodneZArraysSort(dane, sekw);
        boolean rownOk = zgodneZArraysSort(dane, rown);

        System.out.println("Sekwencyjnie: czas = " + czasSek + " ms, poprawne = " + sekwOk);
        System.out.println("ForkJoin:     czas = " + czasRown + " ms, poprawne = " + rownOk);
        System.out.println("Przyspieszenie (sekw / ForkJoin) = " + (czasSek / czasRown));

        if (!sekwOk || !rownOk) {
            System.out.println("BLAD: wynik sortowania niezgodny z Arrays.sort.");
            System.exit(1);
        }
    }

    private static void sprawdz(int[] tab, int prog, int watki) {
        System.out.println("Wejscie: " + Arrays.toString(tab));
        ForkJoinPool pool = new ForkJoinPool(watki);
        int[] wynik;
        try {
            wynik = pool.invoke(new DivideTask(tab, prog));
        } finally {
            pool.shutdown();
        }
        System.out.println("Wynik:   " + Arrays.toString(wynik));
        if (!zgodneZArraysSort(tab, wynik)) {
            throw new IllegalStateException("Bledny wynik dla " + Arrays.toString(tab));
        }
    }

    private static boolean zgodneZArraysSort(int[] oryginal, int[] wynik) {
        int[] oczekiwane = Arrays.copyOf(oryginal, oryginal.length);
        Arrays.sort(oczekiwane);
        return Arrays.equals(oczekiwane, wynik);
    }
}
