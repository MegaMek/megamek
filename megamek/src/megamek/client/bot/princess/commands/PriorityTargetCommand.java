package megamek.client.bot.princess.commands;

import megamek.client.bot.princess.ChatCommands;
import megamek.client.bot.princess.Princess;
import megamek.common.util.StringUtil;
import megamek.server.commands.arguments.Argument;
import megamek.server.commands.arguments.Arguments;
import megamek.server.commands.arguments.UnitArgument;

import java.util.List;

public class PriorityTargetCommand implements ChatCommand {
    private static final String UNIT_ID = "unitID";
    @Override
    public List<Argument<?>> defineArguments() {
        return List.of(
            new UnitArgument(UNIT_ID, "Unit to prioritize fire.")
        );
    }

    @Override
    public void execute(Princess princess, Arguments arguments) {
        UnitArgument unitArg = arguments.get(UNIT_ID, UnitArgument.class);
        princess.getBehaviorSettings().addPriorityUnit(unitArg.getValue());
        princess.sendChat("Unit " + unitArg.getValue() + " added to priority target units list.");
    }
}
