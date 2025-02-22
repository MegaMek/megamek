package megamek.client.bot.princess.commands;

import megamek.client.bot.princess.Princess;
import megamek.common.Entity;
import megamek.server.commands.arguments.*;

import java.util.List;
import java.util.Optional;

public class RemoveWaypointCommand implements ChatCommand {
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
        // tailWaypoint is the last waypoint in the list, the last is what was added recently
        // so this works as an undo
        princess.getUnitBehaviorTracker().removeTailWaypoint(unitOpt.get());
    }
}
