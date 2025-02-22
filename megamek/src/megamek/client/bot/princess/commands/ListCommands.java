package megamek.client.bot.princess.commands;

import megamek.client.bot.princess.ChatCommands;
import megamek.client.bot.princess.Princess;
import megamek.server.commands.arguments.Argument;
import megamek.server.commands.arguments.Arguments;

import java.util.List;

public class ListCommands implements ChatCommand {
    @Override
    public void execute(Princess princess, Arguments arguments) {
        princess.sendChat("Princess Chat Commands");
        for (ChatCommands cmd : ChatCommands.values()) {
            princess.sendChat("# " + cmd.getSyntax() + " :: " + cmd.getDescription());
        }
    }
}
