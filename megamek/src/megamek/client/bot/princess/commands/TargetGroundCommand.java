package megamek.client.bot.princess.commands;

import megamek.client.bot.princess.ChatCommands;
import megamek.client.bot.princess.Princess;
import megamek.common.Coords;
import megamek.common.util.StringUtil;
import megamek.server.commands.arguments.Argument;
import megamek.server.commands.arguments.Arguments;
import megamek.server.commands.arguments.BooleanArgument;
import megamek.server.commands.arguments.HexNumberArgument;

import java.util.List;

public class TargetGroundCommand implements ChatCommand {
    private static final String HEX = "hexNumber";
    @Override
    public List<Argument<?>> defineArguments() {
        return List.of(
            new HexNumberArgument(HEX, "Hex number to target."),
            quietArgument()
        );
    }

    @Override
    public void execute(Princess princess, Arguments arguments) {
        HexNumberArgument hexArg = arguments.get(HEX, HexNumberArgument.class);
        BooleanArgument quietArg = arguments.get(quietArgument().getName(), BooleanArgument.class);
        if (!princess.getGame().getBoard().contains(hexArg.getValue())) {
            if (!quietArg.getValue()) {
                princess.sendChat("Board does not have hex " + hexArg.getValue().toFriendlyString());
            }
            return;
        }

        princess.addStrategicBuildingTarget(hexArg.getValue());
        if (!quietArg.getValue()) {
            princess.sendChat("Hex " + hexArg.getValue().toFriendlyString() + " added to strategic targets list.");
        }
    }
}
