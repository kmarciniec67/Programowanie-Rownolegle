// Dekompozycja 1D obrazu: spojne bloki wierszy.
class WatekWierszeBlok implements Runnable {

    private final int id;
    private final int nWatkow;
    private final Obraz obraz;

    public WatekWierszeBlok(int id, int nWatkow, Obraz obraz) {
        this.id = id;
        this.nWatkow = nWatkow;
        this.obraz = obraz;
    }

    @Override
    public void run() {
        obraz.calculate_wiersze_blokowo(id, nWatkow);
    }
}
