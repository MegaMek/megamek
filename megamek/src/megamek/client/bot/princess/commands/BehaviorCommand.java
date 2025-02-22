package megamek.client.bot.princess.commands;

import megamek.client.bot.princess.BehaviorSettings;
import megamek.client.bot.princess.BehaviorSettingsFactory;
import megamek.client.bot.princess.Princess;
import megamek.server.commands.arguments.Argument;
import megamek.server.commands.arguments.Arguments;
import megamek.server.commands.arguments.StringArgument;

import java.util.List;

public class BehaviorCommand implements ChatCommand {
    private static final String BEHAVIOR = "behavior";

    @Override
    public List<Argument<?>> defineArguments() {
        return List.of(
            new StringArgument(BEHAVIOR, "Name of the behavior to switch to.")
        );
    }

    @Override
    public void execute(Princess princess, Arguments arguments) {
       String behavior = arguments.getString(BEHAVIOR);

        BehaviorSettings newBehavior = BehaviorSettingsFactory.getInstance().getBehavior(behavior);

        if (newBehavior == null) {
            princess.sendChat("Behavior '" + behavior + "' does not exist.");
            return;
        }

        princess.setBehaviorSettings(newBehavior);
        princess.sendChat("Behavior changed to " + princess.getBehaviorSettings().getDescription());
    }
}
