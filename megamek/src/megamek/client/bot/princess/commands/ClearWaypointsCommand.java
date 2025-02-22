package megamek.client.bot.princess.commands;

import megamek.client.bot.princess.Princess;
import megamek.common.Entity;
import megamek.server.commands.arguments.Argument;
import megamek.server.commands.arguments.Arguments;
import megamek.server.commands.arguments.BooleanArgument;
import megamek.server.commands.arguments.UnitArgument;

import java.util.List;
import java.util.Optional;

public class ClearWaypointsCommand implements ChatCommand {
    private static final String UNIT_ID = "unitID";

    @Override
    public List<Argument<?>> defineArguments() {
        return List.of(
            new UnitArgument(UNIT_ID, "The ID of the unit to add a waypoint to."),
            quietArgument()
        );
    }

    @Override
    public void execute(Princess princess, Arguments arguments) {
        UnitArgument unitArgument = arguments.get(UNIT_ID, UnitArgument.class);
        BooleanArgument quietArgument = arguments.get(QUIET, BooleanArgument.class);

        Optional<Entity> unitOpt = princess.getEntitiesOwned().stream()
            .filter(entity -> entity.getId() == unitArgument.getValue())
            .findFirst();

        if (unitOpt.isEmpty()) {
            if (!quietArgument.getValue()) {
                princess.sendChat("Unit " + unitArgument.getValue() + " not found.");
            }
            return;
        }

        princess.getUnitBehaviorTracker().clearWaypoints(unitOpt.get());
    }
}
