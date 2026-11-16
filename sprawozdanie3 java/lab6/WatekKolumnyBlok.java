// Dekompozycja 1D obrazu: spojne bloki kolumn.
class WatekKolumnyBlok implements Runnable {

    private final int id;
    private final int nWatkow;
    private final Obraz obraz;

    public WatekKolumnyBlok(int id, int nWatkow, Obraz obraz) {
        this.id = id;
        this.nWatkow = nWatkow;
        this.obraz = obraz;
    }

    @Override
    public void run() {
        obraz.calculate_kolumny_blokowo(id, nWatkow);
    }
}
