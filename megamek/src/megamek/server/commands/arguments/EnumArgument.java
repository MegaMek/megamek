package megamek.server.commands.arguments;

import megamek.client.ui.Messages;

import java.util.Arrays;

public class EnumArgument<E extends Enum<E>> extends Argument<E> {
    protected final Class<E> enumType;
    protected final E defaultValue;

    public EnumArgument(String name, String description, Class<E> enumType, E defaultValue) {
        super(name, description);
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

    @Override
    public String getHelp() {
        return getDescription() +
            " (" + String.join(", ", Arrays.toString(enumType.getEnumConstants())) + ")" +
            (defaultValue != null ?
                " [default: " + defaultValue + "]. " + Messages.getString("Gamemaster.cmd.params.optional") :
                " " + Messages.getString("Gamemaster.cmd.params.required"));
    }

}
