package megamek.client.bot.princess.commands;

import megamek.client.bot.princess.Princess;
import megamek.server.commands.arguments.Argument;
import megamek.server.commands.arguments.Arguments;
import megamek.server.commands.arguments.IncDecSetIntegerArgument;

import java.util.List;

public class CautionCommand implements ChatCommand {

    private static final String CAUTION = "caution";

    @Override
    public List<Argument<?>> defineArguments() {
        return List.of(
            new IncDecSetIntegerArgument(
                CAUTION,
                "Adjustment to caution index, this also accepts +/- to increase and decrease the current value",
                0,
                10)
        );
    }

    @Override
    public void execute(Princess princess, Arguments arguments) {

        IncDecSetIntegerArgument value = arguments.get(CAUTION, IncDecSetIntegerArgument.class);
        int currentIndex = princess.getBehaviorSettings().getFallShameIndex();

        if (value.getOperation().equals(IncDecSetIntegerArgument.Operation.SET)) {
            princess.getBehaviorSettings().setFallShameIndex(value.getValue());
        } else {
            princess.getBehaviorSettings().setFallShameIndex(
                princess.getBehaviorSettings().getFallShameIndex() + value.getValue());
        }

        String msg = "Piloting caution changed from " + currentIndex + " to " +
            princess.getBehaviorSettings().getFallShameIndex();
        princess.sendChat(msg);
    }
}
