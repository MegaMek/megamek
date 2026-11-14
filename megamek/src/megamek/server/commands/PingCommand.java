/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MegaMek was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */
package megamek.server.commands;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;

import jakarta.annotation.Nullable;
import megamek.client.ui.Messages;
import megamek.common.Player;
import megamek.common.net.enums.PacketCommand;
import megamek.common.net.packets.Packet;
import megamek.logging.MMLogger;
import megamek.server.Server;

/**
 * Pings one player, or everyone, to get their attention. The pinged client plays its ping sound and shows who pinged
 * it; the sender gets a chat note. Anyone may ping, observers included, but only once per {@link #COOLDOWN_MILLIS}.
 *
 * <p>Examples: {@code /ping} pings everyone but the sender; {@code /ping Clan Wolf} pings the player named "Clan Wolf".
 * Names may contain spaces and are matched ignoring case.</p>
 */
public class PingCommand extends ServerCommand {

    private static final MMLogger LOGGER = MMLogger.create(PingCommand.class);

    public static final String COMMAND_NAME = "ping";

    /** How long a sender must wait between pings, shared by pinging one player and pinging everyone. */
    static final long COOLDOWN_MILLIS = 10_000;

    private final LongSupplier clock;
    private final Map<Integer, Long> lastPingMillisBySender = new ConcurrentHashMap<>();

    /**
     * @param server the server that relays the pings
     */
    public PingCommand(Server server) {
        this(server, System::currentTimeMillis);
    }

    /**
     * @param server the server that relays the pings
     * @param clock  the time source in milliseconds, replaceable so tests can step past the cooldown
     */
    PingCommand(Server server, LongSupplier clock) {
        super(server, COMMAND_NAME, Messages.getString("PingCommand.help"));
        this.clock = clock;
    }

    /**
     * Builds the chat text that pings the given player.
     *
     * @param player the player to ping, or {@code null} to ping everyone
     *
     * @return the command, e.g. {@code /ping Clan Wolf}
     */
    public static String commandText(@Nullable Player player) {
        return (player == null) ? "/" + COMMAND_NAME : "/" + COMMAND_NAME + ' ' + player.getName();
    }

    @Override
    public void run(int connId, String[] args) {
        Player sender = server.getGame().getPlayer(connId);
        if (sender == null) {
            LOGGER.info("[PlayerPing] ping refused from connection {}: no player for that connection", connId);
            return;
        }
        Player restrictingGameMaster = findRestrictingGameMaster(server.getGame().getPlayersList());
        if ((restrictingGameMaster != null) && (restrictingGameMaster.getId() != sender.getId())) {
            LOGGER.info("[PlayerPing] {} blocked: Game Master {} has limited pings to themselves", sender.getName(),
                  restrictingGameMaster.getName());
            server.sendServerChat(connId,
                  Messages.getString("PingCommand.gameMasterOnly", restrictingGameMaster.getName()));
            return;
        }
        boolean toEveryone = args.length < 2;
        Player target = null;
        if (!toEveryone) {
            String targetName = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
            target = findPlayer(targetName);
            if (target == null) {
                LOGGER.info("[PlayerPing] {} pinged \"{}\": no such player", sender.getName(), targetName);
                server.sendServerChat(connId, Messages.getString("PingCommand.noSuchPlayer", targetName));
                return;
            }
            if (target.getId() == sender.getId()) {
                LOGGER.info("[PlayerPing] {} tried to ping themselves", sender.getName());
                server.sendServerChat(connId, Messages.getString("PingCommand.self"));
                return;
            }
        }

        long now = clock.getAsLong();
        Long lastPing = lastPingMillisBySender.get(sender.getId());
        if ((lastPing != null) && ((now - lastPing) < COOLDOWN_MILLIS)) {
            long secondsLeft = (long) Math.ceil((COOLDOWN_MILLIS - (now - lastPing)) / 1000.0);
            LOGGER.info("[PlayerPing] {} blocked by the cooldown, {} s left", sender.getName(), secondsLeft);
            server.sendServerChat(connId, Messages.getString("PingCommand.cooldown", secondsLeft));
            return;
        }
        lastPingMillisBySender.put(sender.getId(), now);

        Packet ping = new Packet(PacketCommand.PLAYER_PING, sender.getId(), toEveryone);
        if (toEveryone) {
            int pinged = 0;
            for (Player player : server.getGame().getPlayersList()) {
                if ((player.getId() != sender.getId()) && !player.isGhost()) {
                    server.send(player.getId(), ping);
                    pinged++;
                }
            }
            LOGGER.info("[PlayerPing] {} pinged everyone ({} player(s))", sender.getName(), pinged);
            server.sendServerChat(connId, Messages.getString("PingCommand.sentEveryone"));
        } else {
            server.send(target.getId(), ping);
            LOGGER.info("[PlayerPing] {} pinged {}", sender.getName(), target.getName());
            server.sendServerChat(connId, Messages.getString("PingCommand.sent", target.getName()));
        }
    }

    /**
     * Finds the connected Game Master who has limited pings to themselves. A disconnected Game Master no longer holds
     * the limit, so the table is not left unable to ping.
     *
     * @param players the players in the game
     *
     * @return that Game Master, or {@code null} when anyone may ping
     */
    public static @Nullable Player findRestrictingGameMaster(List<Player> players) {
        for (Player player : players) {
            if (player.isPingGameMasterOnly() && !player.isGhost()) {
                return player;
            }
        }
        return null;
    }

    private @Nullable Player findPlayer(String name) {
        for (Player player : server.getGame().getPlayersList()) {
            if (player.getName().equalsIgnoreCase(name)) {
                return player;
            }
        }
        return null;
    }
}
