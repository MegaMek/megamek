package megamek.server.commands.arguments;

import megamek.client.ui.Messages;
import org.apache.commons.lang3.math.NumberUtils;

import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Nullable Argument for an Enum type.
 * @param <E>
 * @author Luana Coppio
 */
public class OptionalEnumArgument<E extends Enum<E>> extends EnumArgument<E> {

    public OptionalEnumArgument(String name, String description, Class<E> enumType) {
        super(name, description, enumType, null);
    }

    @Override
    public void parse(String input) throws IllegalArgumentException {
        if (input == null) {
            return;
        }
        try {
            if (NumberUtils.isCreatable(input)) {
                value = enumType.getEnumConstants()[Integer.parseInt(input)];
            } else {
                value = Enum.valueOf(enumType, input.toUpperCase());
            }
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(getName() + " must be one of: " + getEnumConstantsString());
        }
    }

    public boolean isPresent() {
        return value != null;
    }

    public boolean isEmpty() {
        return value == null;
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
            (defaultValue != null ? " [default: " + defaultValue + "]. " : ". ") +
            Messages.getString("Gamemaster.cmd.params.optional");
    }

}
