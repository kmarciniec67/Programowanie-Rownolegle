// Dekompozycja 1D obrazu: wiersze cyklicznie.
// Watek o numerze id bierze wiersze id, id+nWatkow, id+2*nWatkow, ...
class WatekWierszeCykliczne implements Runnable {

    private final int id;
    private final int nWatkow;
    private final Obraz obraz;

    public WatekWierszeCykliczne(int id, int nWatkow, Obraz obraz) {
        this.id = id;
        this.nWatkow = nWatkow;
        this.obraz = obraz;
    }

    @Override
    public void run() {
        obraz.calculate_wiersze_cykliczne(id, nWatkow);
    }
}
