package megamek.server.commands.arguments;

import megamek.client.ui.Messages;
import org.apache.commons.lang3.math.NumberUtils;

import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Argument for an Enum type.
 * @param <E>
 * @author Luana Coppio
 */
public class EnumArgument<E extends Enum<E>> extends Argument<E> {
    protected final Class<E> enumType;
    protected final E defaultValue;

    public EnumArgument(String name, String description, Class<E> enumType, E defaultValue) {
        super(name, description);
        this.enumType = enumType;
        this.defaultValue = defaultValue;
    }

    public EnumArgument(String name, String description, Class<E> enumType) {
        this(name, description, enumType, null);
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
            if (NumberUtils.isCreatable(input)) {
                value = enumType.getEnumConstants()[Integer.parseInt(input)];
            } else {
                value = Enum.valueOf(enumType, input.toUpperCase());
            }
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

    private String getEnumConstantsString() {
        return IntStream.range(0, enumType.getEnumConstants().length)
            .mapToObj(i -> i + ": " + enumType.getEnumConstants()[i])
            .collect(Collectors.joining(", "));
    }

    @Override
    public String getHelp() {
        return getDescription() +
            " [" + getEnumConstantsString() + "] " +
            (defaultValue != null ?
                " [default: " + defaultValue + "]. " + Messages.getString("Gamemaster.cmd.params.optional") :
                " " + Messages.getString("Gamemaster.cmd.params.required"));
    }

}
