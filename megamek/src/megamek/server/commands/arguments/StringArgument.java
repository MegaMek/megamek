package megamek.server.commands.arguments;

import megamek.client.ui.Messages;

import java.util.List;

/**
 * Argument for a String type.
 * @author Luana Coppio
 */
public class StringArgument extends Argument<String> {
    private final String defaultValue;

    public StringArgument(String name, String description, String defaultValue) {
        super(name, description);
        this.defaultValue = defaultValue;
    }

    public StringArgument(String name, String description) {
        this(name, description, null);
    }

    @Override
    public String getValue() {
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
        value = input;
    }

    public boolean hasDefaultValue() {
        return defaultValue != null;
    }

    @Override
    public String getHelp() {
        return getDescription() +
            (defaultValue != null ?
                " [default: " + defaultValue + "]. " + Messages.getString("Gamemaster.cmd.params.optional") :
                " " + Messages.getString("Gamemaster.cmd.params.required"));
    }

}
