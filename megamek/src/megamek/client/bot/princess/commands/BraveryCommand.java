package megamek.client.bot.princess.commands;

import megamek.client.bot.princess.Princess;
import megamek.server.commands.arguments.Argument;
import megamek.server.commands.arguments.Arguments;
import megamek.server.commands.arguments.IncDecSetIntegerArgument;

import java.util.List;

public class BraveryCommand implements ChatCommand {

    private static final String BRAVERY = "bravery";

    @Override
    public List<Argument<?>> defineArguments() {
        return List.of(
            new IncDecSetIntegerArgument(
                BRAVERY,
                "Adjustment to bravery index, this also accepts +/- to increase and decrease the current value",
                0,
                10)
        );
    }

    @Override
    public void execute(Princess princess, Arguments arguments) {

        IncDecSetIntegerArgument value = arguments.get(BRAVERY, IncDecSetIntegerArgument.class);
        int currentIndex = princess.getBehaviorSettings().getBraveryIndex();

        if (value.getOperation().equals(IncDecSetIntegerArgument.Operation.SET)) {
            princess.getBehaviorSettings().setBraveryIndex(value.getValue());
        } else {
            princess.getBehaviorSettings().setBraveryIndex(
                princess.getBehaviorSettings().getBraveryIndex() + value.getValue());
        }

        String msg = "Bravery changed from " + currentIndex + " to " +
            princess.getBehaviorSettings().getBraveryIndex();
        princess.sendChat(msg);
    }
}
