package megamek.server.entityControler;

import megamek.common.Minefield;
import megamek.common.Player;
import megamek.common.Team;
import megamek.common.net.Packet;
import megamek.server.Server;

import java.util.Enumeration;

import static megamek.common.net.Packet.COMMAND_REMOVE_MINEFIELD;
import static megamek.common.net.Packet.COMMAND_REVEAL_MINEFIELD;

public class MineFieldController {
    /**
     * Removes the minefield from the game.
     *
     * @param server
     * @param mf The <code>Minefield</code> to remove
     */
    public static void removeMinefield(Server server, Minefield mf) {
        if (server.getGame().containsVibrabomb(mf)) {
            server.getGame().removeVibrabomb(mf);
        }
        server.getGame().removeMinefield(mf);

        Enumeration<Player> players = server.getGame().getPlayers();
        while (players.hasMoreElements()) {
            Player player = players.nextElement();
            removeMinefield(server, player, mf);
        }
    }

    /**
     * Removes the minefield from a player.
     *
     * @param server
     * @param player The <code>Player</code> whose minefield should be removed
     * @param mf     The <code>Minefield</code> to be removed
     */
    private static void removeMinefield(Server server, Player player, Minefield mf) {
        if (player.containsMinefield(mf)) {
            player.removeMinefield(mf);
            server.send(player.getId(), new Packet(COMMAND_REMOVE_MINEFIELD, mf));
        }
    }

    /**
     * Reveals a minefield for all players.
     *
     * @param server
     * @param mf The <code>Minefield</code> to be revealed
     */
    public static void revealMinefield(Server server, Minefield mf) {
        Enumeration<Team> teams = server.getGame().getTeams();
        while (teams.hasMoreElements()) {
            Team team = teams.nextElement();
            revealMinefield(server, team, mf);
        }
    }

    /**
     * Reveals a minefield for all players on a team.
     *
     * @param server
     * @param team The <code>team</code> whose minefield should be revealed
     * @param mf   The <code>Minefield</code> to be revealed
     */
    public static void revealMinefield(Server server, Team team, Minefield mf) {
        Enumeration<Player> players = team.getPlayers();
        while (players.hasMoreElements()) {
            Player player = players.nextElement();
            if (!player.containsMinefield(mf)) {
                player.addMinefield(mf);
                server.send(player.getId(), new Packet(COMMAND_REVEAL_MINEFIELD, mf));
            }
        }
    }

    /**
     * Reveals a minefield for a specific player
     * If on a team, does it for the whole team. Otherwise, just the player.
     */
    public static void revealMinefield(Server server, Player player, Minefield mf) {
        Team team = server.getGame().getTeamForPlayer(player);

        if (team != null) {
            revealMinefield(server, team, mf);
        } else {
            if (!player.containsMinefield(mf)) {
                player.addMinefield(mf);
                server.send(player.getId(), new Packet(COMMAND_REVEAL_MINEFIELD, mf));
            }
        }
    }
}
