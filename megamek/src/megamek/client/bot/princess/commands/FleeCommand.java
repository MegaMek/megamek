package megamek.client.bot.princess.commands;

import megamek.client.bot.princess.CardinalEdge;
import megamek.client.bot.princess.Princess;
import megamek.server.commands.arguments.Argument;
import megamek.server.commands.arguments.Arguments;
import megamek.server.commands.arguments.EnumArgument;

import java.util.List;

public class FleeCommand implements ChatCommand {
    private static final String EDGE = "edge";

    @Override
    public List<Argument<?>> defineArguments() {
        return List.of(
            new EnumArgument<>(EDGE, "Retreat edge to flee to.", CardinalEdge.class)
        );
    }

    @Override
    public void execute(Princess princess, Arguments arguments) {
        CardinalEdge edge = arguments.getEnum(EDGE, CardinalEdge.class);
        princess.sendChat("Received flee order - " + edge.name());
        princess.getBehaviorSettings().setDestinationEdge(edge);
        princess.setFallBack(true, "Received flee order - " + edge.name());
    }
}
