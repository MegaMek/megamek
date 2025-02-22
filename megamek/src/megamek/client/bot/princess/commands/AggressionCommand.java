package megamek.client.bot.princess.commands;

import megamek.client.bot.princess.Princess;
import megamek.server.commands.arguments.Argument;
import megamek.server.commands.arguments.Arguments;
import megamek.server.commands.arguments.IncDecSetIntegerArgument;

import java.util.List;

public class AggressionCommand implements ChatCommand {

    private static final String AGGRESSION = "aggression";

    @Override
    public List<Argument<?>> defineArguments() {
        return List.of(
            new IncDecSetIntegerArgument(
                AGGRESSION,
                "Adjustment to hyper aggression index, this also accepts +/- to increase and decrease the current value",
                0,
                10)
        );
    }

    @Override
    public void execute(Princess princess, Arguments arguments) {

        IncDecSetIntegerArgument value = arguments.get(AGGRESSION, IncDecSetIntegerArgument.class);
        int currentIndex = princess.getBehaviorSettings().getHyperAggressionIndex();

        if (value.getOperation().equals(IncDecSetIntegerArgument.Operation.SET)) {
            princess.getBehaviorSettings().setHyperAggressionIndex(value.getValue());
        } else {
            princess.getBehaviorSettings().setHyperAggressionIndex(
                princess.getBehaviorSettings().getHyperAggressionIndex() + value.getValue());
        }

        String msg = "Hyper aggression changed from " + currentIndex + " to " +
            princess.getBehaviorSettings().getHyperAggressionIndex();
        princess.sendChat(msg);
    }
}
