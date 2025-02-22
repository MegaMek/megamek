package megamek.client.bot.princess.commands;

import megamek.client.bot.princess.Princess;
import megamek.server.commands.arguments.Argument;
import megamek.server.commands.arguments.Arguments;
import megamek.server.commands.arguments.IncDecSetIntegerArgument;

import java.util.List;

public class HerdingCommand implements ChatCommand {

    private static final String HERDING = "herding";

    @Override
    public List<Argument<?>> defineArguments() {
        return List.of(
            new IncDecSetIntegerArgument(
                HERDING,
                "Adjustment to herding index, this also accepts +/- to increase and decrease the current value",
                0,
                10)
        );
    }

    @Override
    public void execute(Princess princess, Arguments arguments) {

        IncDecSetIntegerArgument value = arguments.get(HERDING, IncDecSetIntegerArgument.class);
        int currentIndex = princess.getBehaviorSettings().getHerdMentalityIndex();

        if (value.getOperation().equals(IncDecSetIntegerArgument.Operation.SET)) {
            princess.getBehaviorSettings().setHerdMentalityIndex(value.getValue());
        } else {
            princess.getBehaviorSettings().setHerdMentalityIndex(
                princess.getBehaviorSettings().getHerdMentalityIndex() + value.getValue());
        }

        String msg = "Herding changed from " + currentIndex + " to " +
            princess.getBehaviorSettings().getHerdMentalityIndex();
        princess.sendChat(msg);
    }
}
