package megamek.server.commands.arguments;

import megamek.client.ui.Messages;

/**
 * Argument for an Integer type.
 * @author Luana Coppio
 */
public class CoordXArgument extends Argument<Integer> {

    public CoordXArgument(String name, String description) {
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
                throw new IllegalArgumentException(getName() + " must be an integer of an X hex coordinate.");
            }
            value = parsedValue;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(getName() + " must be an integer of an X hex coordinate.");
        }
    }

    @Override
    public String getHelp() {
        return getDescription() + " " + Messages.getString("Gamemaster.cmd.params.required");
    }
}
