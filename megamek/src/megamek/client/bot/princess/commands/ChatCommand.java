package megamek.client.bot.princess.commands;

import megamek.client.bot.princess.Princess;
import megamek.server.commands.arguments.Argument;
import megamek.server.commands.arguments.Arguments;
import megamek.server.commands.arguments.BooleanArgument;

import java.util.List;

public interface ChatCommand {
    String QUIET = "quiet";
    default List<Argument<?>> defineArguments() {
        return List.of();
    }

    void execute(Princess princess, Arguments arguments);

    default BooleanArgument quietArgument() {
        return new BooleanArgument(QUIET, "When set to true, princess won't respond in the chat.", false);
    }

    default String getArgumentsRepr() {
        StringBuilder help = new StringBuilder();
        for (Argument<?> arg : defineArguments()) {
            help.append(arg.getRepr());
        }
        return help.toString();
    }

    default String getArgumentsDescription() {
        StringBuilder help = new StringBuilder();

        for (var arg : defineArguments()) {
            help.append(arg.getName())
                .append(": ")
                .append(arg.getHelp())
                .append("    ");
        }
        return help.toString();
    }

}
