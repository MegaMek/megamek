package megamek.client.bot.caspar.axis;


/**
 * Base class for axis calculators.
 */
public abstract class BaseAxisCalculator implements AxisCalculator {
    /**
     * Normalizes a value to the range [0, 1].
     *
     * @param value The value to normalize
     * @param min   The minimum expected value
     * @param max   The maximum expected value
     * @return A normalized value between 0 and 1
     */
    protected double normalize(double value, double min, double max) {
        if (max == min) {
            return 0.5; // Avoid division by zero
        }
        double normalized = (value - min) / (max - min);
        return Math.max(0, Math.min(1, normalized));
    }
}
