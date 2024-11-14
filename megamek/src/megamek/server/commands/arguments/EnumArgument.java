package megamek.server.commands.arguments;

import java.util.Arrays;

public class EnumArgument<E extends Enum<E>> extends Argument<E> {
    private final Class<E> enumType;
    private final E defaultValue;

    public EnumArgument(String name, Class<E> enumType, E defaultValue) {
        super(name);
        this.enumType = enumType;
        this.defaultValue = defaultValue;
    }

    public Class<E> getEnumType() {
        return enumType;
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
            value = Enum.valueOf(enumType, input.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(getName() + " must be one of: "
                + String.join(", ", Arrays.toString(enumType.getEnumConstants())));
        }
    }

    @Override
    public E getValue() {
        if (value == null && defaultValue != null) {
            return defaultValue;
        }
        return value;
    }
}
