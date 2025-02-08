package megamek.common;


public class CubeCoords {

    public final double q, r, s;

    public CubeCoords(double q, double r, double s) {
        this.q = q;
        this.r = r;
        this.s = s;
    }

    public Coords toOffset() {
        // Implement your hex grid's conversion logic
        int x = (int) Math.round(q);
        int y = (int) Math.round(r + (q - Math.round(q)) / 2);
        return new Coords(x, y);
    }

    public CubeCoords roundToNearestHex() {
        // Implement cube coordinate rounding
        double rx = Math.round(q);
        double ry = Math.round(r);
        double rz = Math.round(s);

        double q_diff = Math.abs(rx - q);
        double r_diff = Math.abs(ry - r);
        double s_diff = Math.abs(rz - s);

        if ((q_diff > r_diff) && (q_diff > s_diff)) {
            rx = -ry - rz;
        } else if (r_diff > s_diff) {
            ry = -rx - rz;
        } else {
            rz = -rx - ry;
        }

        return new CubeCoords(rx, ry, rz);
    }

    /**
     * Linearly interpolates between two cube coordinates.
     *
     * @param a the start cube coordinate
     * @param b the end cube coordinate
     * @param t the interpolation parameter, where {@code 0 <= t <= 1}
     * @return a new CubeCoords representing the interpolated coordinate
     */
    public static CubeCoords lerp(CubeCoords a, CubeCoords b, double t) {
        double q = a.q * (1 - t) + b.q * t;
        double r = a.r * (1 - t) + b.r * t;
        double s = a.s * (1 - t) + b.s * t;
        return new CubeCoords(q, r, s);
    }
}
