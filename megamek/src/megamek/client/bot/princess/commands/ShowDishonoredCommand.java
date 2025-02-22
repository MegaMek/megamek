package megamek.client.bot.princess.commands;

import megamek.client.bot.princess.Princess;
import megamek.server.commands.arguments.Argument;
import megamek.server.commands.arguments.Arguments;

import java.util.List;
import java.util.stream.Collectors;

public class ShowDishonoredCommand implements ChatCommand {
    @Override
    public void execute(Princess princess, Arguments arguments) {
        String msg = "Dishonored Player ids: " + princess.getHonorUtil().getDishonoredEnemies().stream()
            .map(Object::toString).collect(Collectors.joining(", "));
        princess.sendChat(msg);
    }
}
