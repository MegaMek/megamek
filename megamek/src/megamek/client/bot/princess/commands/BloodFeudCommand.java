package megamek.client.bot.princess.commands;

import megamek.client.bot.princess.ChatCommands;
import megamek.client.bot.princess.Princess;
import megamek.common.Player;
import megamek.common.util.StringUtil;
import megamek.server.commands.arguments.Argument;
import megamek.server.commands.arguments.Arguments;
import megamek.server.commands.arguments.PlayerArgument;

import java.util.List;

public class BloodFeudCommand implements ChatCommand {
    private static final String PLAYER_ID = "playerId";
    @Override
    public List<Argument<?>> defineArguments() {
        return List.of(
            new PlayerArgument(PLAYER_ID, "Player ID to add to the dishonored list.")
        );
    }

    @Override
    public void execute(Princess princess, Arguments arguments) {
        PlayerArgument playerArg = arguments.get(PLAYER_ID, PlayerArgument.class);
        Player player = princess.getGame().getPlayer(playerArg.getValue());
        if (player != null) {
            princess.getHonorUtil().setEnemyDishonored(playerArg.getValue());
            princess.sendChat("Player " + player.getName() + " added to the dishonored list.");
        } else {
            princess.sendChat("Player with id " + playerArg.getValue() + " not found.");
        }
    }
}
