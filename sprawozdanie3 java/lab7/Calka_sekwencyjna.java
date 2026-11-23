// javac -encoding UTF-8 Calka_callable.java Calka_sekwencyjna.java
// java Calka_sekwencyjna [dx]
//
// Cala calka sin(x) na [0, PI] liczona w jednym watku.
// Wartosc dokladna: 2.
public class Calka_sekwencyjna {

    public static void main(String[] args) {
        double dx = 1e-6;
        if (args.length >= 1) {
            dx = Double.parseDouble(args[0]);
        }
        if (args.length > 1) {
            System.out.println("Uzycie: java Calka_sekwencyjna [dx]");
            return;
        }

        double xp = 0.0;
        double xk = Math.PI;
        double dokladny = 2.0;

        System.out.println("Sekwencyjna calka sin(x) na [0, PI]");
        System.out.println("dx = " + dx);

        long start = System.nanoTime();
        Calka_callable calka = new Calka_callable(xp, xk, dx);
        double wynik = calka.compute_integral();
        long stop = System.nanoTime();

        System.out.println("Wynik = " + wynik);
        System.out.println("Dokladny = " + dokladny);
        System.out.println("Blad = " + Math.abs(wynik - dokladny));
        System.out.println("Czas = " + ((stop - start) / 1e6) + " ms");
    }
}
