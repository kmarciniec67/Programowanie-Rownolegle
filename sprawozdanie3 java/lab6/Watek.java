// Wariant 1. Klasa watku dziedziczy po Thread. Jeden watek liczy jeden znak.
class Watek extends Thread {

    private final int znak;
    private final Obraz obraz;

    public Watek(int znak, Obraz obraz) {
        this.znak = znak;
        this.obraz = obraz;
    }

    @Override
    public void run() {
        obraz.calculate_histogram_znak(znak);
        obraz.print_histogram_znak_watku(znak, znak);
    }
}
