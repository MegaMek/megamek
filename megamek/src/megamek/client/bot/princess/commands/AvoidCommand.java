package megamek.client.bot.princess.commands;

import megamek.client.bot.princess.Princess;
import megamek.server.commands.arguments.Argument;
import megamek.server.commands.arguments.Arguments;
import megamek.server.commands.arguments.IncDecSetIntegerArgument;

import java.util.List;

public class AvoidCommand implements ChatCommand {

    private static final String AVOID = "avoid";

    @Override
    public List<Argument<?>> defineArguments() {
        return List.of(
            new IncDecSetIntegerArgument(
                AVOID,
                "Adjustment to self preservation index, this also accepts +/- to increase and decrease the current value",
                0,
                10)
        );
    }

    @Override
    public void execute(Princess princess, Arguments arguments) {

        IncDecSetIntegerArgument value = arguments.get(AVOID, IncDecSetIntegerArgument.class);
        int currentSelfPreservation = princess.getBehaviorSettings().getSelfPreservationIndex();

        if (value.getOperation().equals(IncDecSetIntegerArgument.Operation.SET)) {
            princess.getBehaviorSettings().setSelfPreservationIndex(value.getValue());
        } else {
            princess.getBehaviorSettings().setSelfPreservationIndex(
                princess.getBehaviorSettings().getSelfPreservationIndex() + value.getValue());
        }

        String msg = "Self Preservation changed from " + currentSelfPreservation + " to " +
            princess.getBehaviorSettings().getSelfPreservationIndex();
        princess.sendChat(msg);
    }
}
