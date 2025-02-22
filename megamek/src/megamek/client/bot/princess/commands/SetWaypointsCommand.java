package megamek.client.bot.princess.commands;

import megamek.client.bot.princess.Princess;
import megamek.common.Entity;
import megamek.server.commands.arguments.*;

import java.util.List;
import java.util.Optional;

public class SetWaypointsCommand implements ChatCommand {
    private static final String UNIT_ID = "unitID";
    private static final String HEX_NUMBER = "hexNumber";

    @Override
    public List<Argument<?>> defineArguments() {
        return List.of(
            new UnitArgument(UNIT_ID, "The ID of the unit to add a waypoint to."),
            new MultiHexNumberArgument(HEX_NUMBER, "The hexes to set as waypoints."),
            quietArgument()
        );
    }

    @Override
    public void execute(Princess princess, Arguments arguments) {
        UnitArgument unitArgument = arguments.get(UNIT_ID, UnitArgument.class);
        MultiHexNumberArgument multiHexNumberArgument = arguments.get(HEX_NUMBER, MultiHexNumberArgument.class);
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

        for (var coords : multiHexNumberArgument.getValue()) {
            if (!princess.getGame().getBoard().contains(coords) && !quietArgument.getValue()) {
                princess.sendChat("Board does not have hex " + coords.toFriendlyString());
                return;
            }
        }
        princess.getUnitBehaviorTracker().setEntityWaypoints(unitOpt.get(), multiHexNumberArgument.getValue(), princess);
    }
}
