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

import java.util.List;
import java.util.Locale;

import megamek.common.Player;
import megamek.common.net.connections.AbstractConnection;
import megamek.common.net.stats.NetStats;
import megamek.server.Server;

/**
 * Shows the server's network counters in chat. Read-only views are open to everyone; {@code reset} and
 * {@code timing} change server state and are restricted like the other game-altering commands.
 */
public class NetStatsCommand extends ServerCommand {

    static final int DEFAULT_TOP = 15;

    public NetStatsCommand(Server server) {
        super(server, "netstats",
              "Shows network traffic counters. Usage: /netstats [all | conn | reset | timing on|off]. "
                    + "No argument lists the busiest " + DEFAULT_TOP + " packet commands; 'all' lists every "
                    + "command seen; 'conn' lists per-connection totals; 'reset' zeroes the counters; "
                    + "'timing on' starts measuring marshal, write, flush and pump times (small cost).");
    }

    @Override
    public void run(int connId, String[] args) {
        String mode = (args.length > 1) ? args[1].toLowerCase(Locale.ROOT) : "";
        NetStats stats = server.getNetStats();
        switch (mode) {
            case "":
                send(connId, stats.format(DEFAULT_TOP));
                break;
            case "all":
                send(connId, stats.format(Integer.MAX_VALUE));
                break;
            case "conn":
                sendConnections(connId);
                break;
            case "reset":
                if (!canRunRestrictedCommand(connId)) {
                    server.sendServerChat(connId, "Observers are not allowed to reset the network counters.");
                    return;
                }
                stats.reset();
                server.sendServerChat(connId, "Network counters reset.");
                break;
            case "timing":
                if (!canRunRestrictedCommand(connId)) {
                    server.sendServerChat(connId, "Observers are not allowed to change network timing.");
                    return;
                }
                String setting = (args.length > 2) ? args[2].toLowerCase(Locale.ROOT) : "";
                if ("on".equals(setting) || "off".equals(setting)) {
                    NetStats.setTimingEnabled("on".equals(setting));
                    server.sendServerChat(connId, "Network timing is now " + setting + ".");
                } else {
                    server.sendServerChat(connId, "Network timing is " + (NetStats.isTimingEnabled() ? "on" : "off")
                          + ". Usage: /netstats timing on|off");
                }
                break;
            default:
                server.sendServerChat(connId, "Unknown option '" + args[1] + "'. " + getHelp());
                break;
        }
    }

    private void send(int connId, List<String> lines) {
        for (String line : lines) {
            server.sendServerChat(connId, line);
        }
    }

    private void sendConnections(int connId) {
        server.sendServerChat(connId, String.format("%-4s %-20s %12s %12s %s", "id", "player", "bytes-out",
              "bytes-in", "pending"));
        server.forEachConnection(connection -> server.sendServerChat(connId, describe(connection)));
    }

    private String describe(AbstractConnection connection) {
        Player player = server.getPlayer(connection.getId());
        return String.format("%-4d %-20s %12d %12d %s", connection.getId(),
              (player == null) ? "?" : player.getName(), connection.getBytesSent(), connection.getBytesReceived(),
              connection.hasPending() ? "yes" : "no");
    }
}
