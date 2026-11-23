import java.util.concurrent.Callable;

// Zadanie dla puli watkow: call() zwraca calke na przedziale [xp, xk].
public class Calka_callable implements Callable<Double> {

    private double dx;
    private double xp;
    private double xk;
    private int N;
    private boolean verbose;

    public Calka_callable(double xp, double xk, double dx) {
        this(xp, xk, dx, true);
    }

    public Calka_callable(double xp, double xk, double dx, boolean verbose) {
        if (!(xk > xp) || Double.isNaN(xp) || Double.isNaN(xk)) {
            throw new IllegalArgumentException("Przedzial musi spelniac xp < xk.");
        }
        if (!(dx > 0.0) || Double.isNaN(dx) || Double.isInfinite(dx)) {
            throw new IllegalArgumentException("dx musi byc skonczone i dodatnie.");
        }

        this.xp = xp;
        this.xk = xk;
        this.verbose = verbose;

        double nReal = Math.ceil((xk - xp) / dx);
        if (nReal > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("dx jest za male, liczba trapezow nie miesci sie w int.");
        }
        this.N = (int) nReal;
        if (this.N < 1) {
            this.N = 1;
        }
        // dx finalne dopasowane tak, aby N trapezow pokrylo [xp, xk] dokladnie.
        // Nie zalezy od liczby zadan ani watkow.
        this.dx = (xk - xp) / N;

        if (verbose) {
            System.out.println("Creating an instance of Calka_callable");
            System.out.println("xp = " + xp + ", xk = " + xk + ", N = " + N);
            System.out.println("dx requested = " + dx + ", dx final = " + this.dx);
        }
    }

    private double getFunction(double x) {
        return Math.sin(x);
    }

    public double compute_integral() {
        double calka = 0;
        for (int i = 0; i < N; i++) {
            double x1 = xp + i * dx;
            double x2 = x1 + dx;
            calka += ((getFunction(x1) + getFunction(x2)) / 2.0) * dx;
        }
        if (verbose) {
            System.out.println("Calka czastkowa " + calka);
        }
        return calka;
    }

    @Override
    public Double call() {
        return compute_integral();
    }
}
