package megamek.common;

public class ECMInfo {
    public int range;
    public Coords pos;
    public double strength;

    public ECMInfo(int r, Coords p, double s) {
        range = r;
        pos = p;
        strength = s;
    }
}