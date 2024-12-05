package megamek.server.commands.arguments;

import megamek.client.ui.Messages;

/**
 * Argument for an Integer type.
 * @author Luana Coppio
 */
public class UnitArgument extends Argument<Integer> {

    public UnitArgument(String name, String description) {
        super(name, description);
    }

    @Override
    public Integer getValue() {
        return value;
    }

    @Override
    public void parse(String input) throws IllegalArgumentException {
        if (input == null) {
            throw new IllegalArgumentException(getName() + " is required.");
        }
        try {
            int parsedValue = Integer.parseInt(input);
            if (parsedValue < 0) {
                throw new IllegalArgumentException(getName() + " must be an integer ID of a unit.");
            }
            value = parsedValue;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(getName() + " must be an integer ID of a unit.");
        }
    }

    @Override
    public String getHelp() {
        return getDescription() + " " + Messages.getString("Gamemaster.cmd.params.required");
    }
}
