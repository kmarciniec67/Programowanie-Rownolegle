import java.util.Arrays;
import java.util.concurrent.RecursiveTask;

// Sortowanie przez scalanie jako zadanie ForkJoinPool.
// Rekurencja konczy sie, gdy fragment ma co najwyzej `prog` elementow.
class DivideTask extends RecursiveTask<int[]> {

    private final int[] arrayToDivide;
    private final int prog;

    public DivideTask(int[] arrayToDivide, int prog) {
        this.arrayToDivide = arrayToDivide;
        this.prog = prog < 1 ? 1 : prog;
    }

    @Override
    protected int[] compute() {
        if (arrayToDivide.length <= prog) {
            return sortujSekwencyjnie(arrayToDivide);
        }

        int mid = arrayToDivide.length / 2;
        int[] lewa = Arrays.copyOfRange(arrayToDivide, 0, mid);
        int[] prawa = Arrays.copyOfRange(arrayToDivide, mid, arrayToDivide.length);

        DivideTask task1 = new DivideTask(lewa, prog);
        DivideTask task2 = new DivideTask(prawa, prog);

        // Biezacy watek liczy prawa polowke, lewa wykonuje inny watek puli.
        // Galezie schodza do progu, wiec wywolanie rekurencyjne sie konczy.
        task1.fork();
        int[] tab2 = task2.compute();
        int[] tab1 = task1.join();

        int[] wynik = new int[tab1.length + tab2.length];
        scal_tab(tab1, tab2, wynik);
        return wynik;
    }

    static int[] sortujSekwencyjnie(int[] tab) {
        if (tab.length <= 1) {
            return tab;
        }
        int mid = tab.length / 2;
        int[] tab1 = sortujSekwencyjnie(Arrays.copyOfRange(tab, 0, mid));
        int[] tab2 = sortujSekwencyjnie(Arrays.copyOfRange(tab, mid, tab.length));
        int[] wynik = new int[tab.length];
        scal_tab(tab1, tab2, wynik);
        return wynik;
    }

    private static void scal_tab(
            int[] tab1,
            int[] tab2,
            int[] scal_tab) {

        int i = 0, j = 0, k = 0;

        while ((i < tab1.length) && (j < tab2.length)) {

            if (tab1[i] < tab2[j]) {
                scal_tab[k] = tab1[i++];
            } else {
                scal_tab[k] = tab2[j++];
            }

            k++;
        }

        if (i == tab1.length) {

            for (int a = j; a < tab2.length; a++) {
                scal_tab[k++] = tab2[a];
            }

        } else {

            for (int a = i; a < tab1.length; a++) {
                scal_tab[k++] = tab1[a];
            }

        }
    }
}
