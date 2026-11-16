import java.net.InetAddress;
import java.util.Scanner;

// javac -encoding UTF-8 *.java
// java Histogram_test n m watki [symbole 1..94] [ziarno]
class Histogram_test {

    public static void main(String[] args) {

        int n;
        int m;
        int numThreads;
        int nSym;
        long ziarno;

        if (args.length >= 3) {
            n = Integer.parseInt(args[0]);
            m = Integer.parseInt(args[1]);
            numThreads = Integer.parseInt(args[2]);
            nSym = args.length >= 4 ? Integer.parseInt(args[3]) : 94;
            ziarno = args.length >= 5 ? Long.parseLong(args[4]) : System.nanoTime();
        } else {
            Scanner scanner = new Scanner(System.in);
            System.out.println("Rozmiar obrazu: n (wiersze) m (kolumny)");
            n = scanner.nextInt();
            m = scanner.nextInt();
            System.out.println("Liczba watkow dla dekompozycji blokowej i obrazu");
            numThreads = scanner.nextInt();
            System.out.println("Liczba symboli (1..94)");
            nSym = scanner.nextInt();
            System.out.println("Ziarno generatora (liczba calkowita)");
            ziarno = scanner.nextLong();
            scanner.close();
        }

        if (numThreads < 1) {
            throw new IllegalArgumentException("Liczba watkow musi byc dodatnia.");
        }

        String host = "localhost";
        try {
            host = InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            host = "localhost";
        }
        System.out.println(System.getProperty("user.name") + "@" + host);
        System.out.println("n = " + n + ", m = " + m
                + ", watki = " + numThreads
                + ", symbole = " + nSym
                + ", ziarno = " + ziarno);
        System.out.println();

        Obraz obraz = new Obraz(n, m, nSym, ziarno);

        System.out.println("========== 1. Sekwencyjnie ==========");
        obraz.calculate_histogram();
        obraz.print_histogram();
        System.out.println();

        System.out.println("========== 2. Wariant 1: Thread, jeden znak na watek ==========");
        obraz.clear_histogram_parallel();
        Watek[] watkiZnak = new Watek[nSym];
        for (int i = 0; i < nSym; i++) {
            watkiZnak[i] = new Watek(i, obraz);
            watkiZnak[i].start();
        }
        for (int i = 0; i < nSym; i++) {
            try {
                watkiZnak[i].join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        obraz.wypisz_porownanie();
        System.out.println();

        System.out.println("========== 3. Wariant 2: Runnable, blok znakow ==========");
        obraz.clear_histogram_parallel();
        wypisz_przydzial_znakow(obraz, numThreads);
        WatekZnakow[] zadaniaZnakow = new WatekZnakow[numThreads];
        for (int i = 0; i < numThreads; i++) {
            zadaniaZnakow[i] = new WatekZnakow(i, numThreads, obraz);
        }
        uruchom(zadaniaZnakow);
        obraz.wypisz_porownanie();
        System.out.println();

        uruchom_dekompozycje_obrazu(obraz, numThreads, "wiersze cykliczne");
        uruchom_dekompozycje_obrazu(obraz, numThreads, "wiersze blokowo");
        uruchom_dekompozycje_obrazu(obraz, numThreads, "kolumny blokowo");
    }

    private static void wypisz_przydzial_znakow(Obraz obraz, int numThreads) {
        for (int i = 0; i < numThreads; i++) {
            int start = obraz.poczatek_bloku(i, numThreads, obraz.getLiczbaSymboli());
            int end = obraz.koniec_bloku(i, numThreads, obraz.getLiczbaSymboli());
            System.out.print("Przydzial watku " + (i + 1) + ": ");
            if (start == end) {
                System.out.println("(brak)");
                continue;
            }
            for (int k = start; k < end; k++) {
                System.out.print(obraz.getSymbol(k) + " ");
            }
            System.out.println();
        }
    }

    private static void uruchom_dekompozycje_obrazu(Obraz obraz, int numThreads, String nazwa) {
        System.out.println("========== Dekompozycja obrazu 1D: " + nazwa + " ==========");
        obraz.przygotuj_histogramy_czesciowe(numThreads);
        wypisz_przydzial_obrazu(obraz, numThreads, nazwa);

        Runnable[] zadania = new Runnable[numThreads];
        for (int i = 0; i < numThreads; i++) {
            if (nazwa.equals("wiersze cykliczne")) {
                zadania[i] = new WatekWierszeCykliczne(i, numThreads, obraz);
            } else if (nazwa.equals("wiersze blokowo")) {
                zadania[i] = new WatekWierszeBlok(i, numThreads, obraz);
            } else {
                zadania[i] = new WatekKolumnyBlok(i, numThreads, obraz);
            }
        }
        uruchom(zadania);

        System.out.println("Histogramy czesciowe (przed redukcja):");
        obraz.print_histogramy_czesciowe();
        obraz.scal_histogramy_czesciowe();
        System.out.println("Histogram po zsumowaniu, wypisany przez watek glowny:");
        obraz.print_histogram_parallel();
        obraz.wypisz_porownanie();
        System.out.println();
    }

    private static void wypisz_przydzial_obrazu(Obraz obraz, int numThreads, String nazwa) {
        for (int i = 0; i < numThreads; i++) {
            System.out.print("Przydzial watku " + (i + 1) + ": ");
            if (nazwa.equals("wiersze cykliczne")) {
                StringBuilder sb = new StringBuilder("wiersze ");
                boolean pierwszy = true;
                for (int r = i; r < obraz.getWiersze(); r += numThreads) {
                    if (!pierwszy) sb.append(", ");
                    pierwszy = false;
                    sb.append(r);
                }
                if (pierwszy) sb.append("(brak)");
                System.out.println(sb.toString());
            } else if (nazwa.equals("wiersze blokowo")) {
                int start = obraz.poczatek_bloku(i, numThreads, obraz.getWiersze());
                int end = obraz.koniec_bloku(i, numThreads, obraz.getWiersze());
                if (start == end) System.out.println("wiersze (brak)");
                else System.out.println("wiersze [" + start + ", " + end + ")");
            } else {
                int start = obraz.poczatek_bloku(i, numThreads, obraz.getKolumny());
                int end = obraz.koniec_bloku(i, numThreads, obraz.getKolumny());
                if (start == end) System.out.println("kolumny (brak)");
                else System.out.println("kolumny [" + start + ", " + end + ")");
            }
        }
    }

    private static void uruchom(Runnable[] zadania) {
        Thread[] watki = new Thread[zadania.length];
        for (int i = 0; i < zadania.length; i++) {
            watki[i] = new Thread(zadania[i]);
            watki[i].start();
        }
        for (int i = 0; i < watki.length; i++) {
            try {
                watki[i].join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
