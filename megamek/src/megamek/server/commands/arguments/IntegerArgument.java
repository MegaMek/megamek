package megamek.server.commands.arguments;

public class IntegerArgument extends Argument<Integer> {
    private final int minValue;
    private final int maxValue;
    private final Integer defaultValue;

    public IntegerArgument(String name) {
        this(name, Integer.MIN_VALUE, Integer.MAX_VALUE, null);
    }

    public IntegerArgument(String name, int minValue, int maxValue) {
        this(name, minValue, maxValue, null);
    }

    public IntegerArgument(String name, int minValue, int maxValue, Integer defaultValue) {
        super(name);
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.defaultValue = defaultValue;
    }

    @Override
    public Integer getValue() {
        if (value == null && defaultValue != null) {
            return defaultValue;
        }
        return value;
    }

    @Override
    public void parse(String input) throws IllegalArgumentException {
        if (input == null && defaultValue != null) {
            value = defaultValue;
            return;
        } else {
            if (input == null) {
                throw new IllegalArgumentException(getName() + " is required.");
            }
        }
        try {
            int parsedValue = Integer.parseInt(input);
            if (parsedValue < minValue || parsedValue > maxValue) {
                throw new IllegalArgumentException(getName() + " must be between " + minValue + " and " + maxValue);
            }
            value = parsedValue;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(getName() + " must be an integer.");
        }
    }

    public boolean hasDefaultValue() {
        return defaultValue != null;
    }

    public int getMinValue() {
        return minValue;
    }

    public int getMaxValue() {
        return maxValue;
    }
}
