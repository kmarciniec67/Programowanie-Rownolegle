import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

// javac -encoding UTF-8 Calka_callable.java Calka_executor.java
// java Calka_executor [dx] [liczba_watkow] [liczba_zadan]
//
// Trzy niezalezne parametry: dx, liczba watkow, liczba zadan.
// Zadania sa wysylane do puli w jednej petli, wyniki Future odbierane w drugiej.
public class Calka_executor {

    public static void main(String[] args) throws InterruptedException, ExecutionException {
        double dx = 1e-7;
        int watki = Runtime.getRuntime().availableProcessors();
        int zadania = watki * 4;

        if (args.length >= 1) {
            dx = Double.parseDouble(args[0]);
        }
        if (args.length >= 2) {
            watki = Integer.parseInt(args[1]);
        }
        if (args.length >= 3) {
            zadania = Integer.parseInt(args[2]);
        }
        if (args.length > 3) {
            System.out.println("Uzycie: java Calka_executor [dx] [liczba_watkow] [liczba_zadan]");
            return;
        }
        if (watki < 1 || zadania < 1) {
            throw new IllegalArgumentException("Liczba watkow i zadan musi byc dodatnia.");
        }

        double xp = 0.0;
        double xk = Math.PI;
        double dokladny = 2.0;

        System.out.println("Calka sin(x) na [0, PI], wartosc dokladna = 2");
        System.out.println("Dostepne procesory: " + Runtime.getRuntime().availableProcessors());

        System.out.println();
        System.out.println("--- Przyklad podzialu (dx = 0.5, watki = 2, zadania = 4) ---");
        double przyklad = obliczRownolegle(xp, xk, 0.5, 2, 4, true);
        System.out.println("Suma calek czastkowych = " + przyklad);
        System.out.println("Blad przykladu = " + Math.abs(przyklad - dokladny));

        System.out.println();
        System.out.println("--- Pomiar (dx = " + dx + ", watki = " + watki
                + ", zadania = " + zadania + ") ---");
        System.out.println("Rozgrzewanie JVM...");
        new Calka_callable(xp, xk, dx, false).compute_integral();
        obliczRownolegle(xp, xk, dx, watki, zadania, false);

        long t0 = System.nanoTime();
        double sekw = new Calka_callable(xp, xk, dx, false).compute_integral();
        long t1 = System.nanoTime();

        long t2 = System.nanoTime();
        double rown = obliczRownolegle(xp, xk, dx, watki, zadania, false);
        long t3 = System.nanoTime();

        double czasSek = (t1 - t0) / 1e6;
        double czasRown = (t3 - t2) / 1e6;

        System.out.println("Sekwencyjnie: wynik = " + sekw
                + ", blad = " + Math.abs(sekw - dokladny)
                + ", czas = " + czasSek + " ms");
        System.out.println("Rownolegle:   wynik = " + rown
                + ", blad = " + Math.abs(rown - dokladny)
                + ", czas = " + czasRown + " ms");
        System.out.println("Przyspieszenie (sekw / rown) = " + (czasSek / czasRown));

        if (Math.abs(sekw - dokladny) > 1e-4 || Math.abs(rown - dokladny) > 1e-4) {
            System.out.println("BLAD: wynik zbyt daleko od 2.");
            System.exit(1);
        }
    }

    private static double obliczRownolegle(double xp, double xk, double dx,
                                            int watki, int zadania, boolean verbose)
            throws InterruptedException, ExecutionException {

        ExecutorService executor = Executors.newFixedThreadPool(watki);
        List<Future<Double>> wyniki = new ArrayList<Future<Double>>();

        try {
            // Jedna petla: tworzenie zadan i wstawienie ich do puli (bez czekania na wynik).
            for (int i = 0; i < zadania; i++) {
                double a = xp + (xk - xp) * i / zadania;
                double b = xp + (xk - xp) * (i + 1) / zadania;
                Calka_callable zadanie = new Calka_callable(a, b, dx, verbose);
                wyniki.add(executor.submit(zadanie));
            }

            // Druga petla: odbiór wyników. get() dopiero tutaj, wiec zadania leca rownolegle.
            double suma = 0.0;
            for (Future<Double> future : wyniki) {
                suma += future.get();
            }
            return suma;
        } finally {
            executor.shutdown();
        }
    }
}
