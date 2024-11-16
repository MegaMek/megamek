package megamek.server.commands.arguments;

import megamek.client.ui.Messages;

import java.util.Arrays;

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
            value = enumType.getEnumConstants()[Integer.parseInt(input)];
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
        var sb = new StringBuilder();
        for (int i = 0; i < enumType.getEnumConstants().length; i++) {
            sb.append(i).append(": ").append(enumType.getEnumConstants()[i]);
            if (i < enumType.getEnumConstants().length - 1) {
                sb.append(", ");
            }
        }
        return sb.toString();
    }

    @Override
    public String getHelp() {
        return getDescription() +
            " [" + getEnumConstantsString() + "] " +
            (defaultValue != null ? " [default: " + defaultValue + "]. " : ". ") +
            Messages.getString("Gamemaster.cmd.params.optional");
    }

}
