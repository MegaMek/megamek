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
package megamek.client.ui.util;

import java.util.HashSet;
import java.util.Set;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import megamek.client.ui.Messages;
import megamek.server.RefusedConnection;

/**
 * Tells the host, with a dialog, that the server turned a player away for running a different version.
 *
 * <p>Install {@link #showHostDialog(RefusedConnection)} as the server's refused connection handler wherever a game
 * is hosted from a user interface, such as MegaMek's own Host Game and MekHQ's Host MegaMek Game.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
public final class RefusedConnectionNotifier {

    /**
     * The players whose refusal dialog is open right now. A player who keeps pressing Connect would otherwise stack
     * a fresh dialog on the host for every attempt; while one is showing for them, further attempts are only
     * logged. Touched only on the event dispatch thread, so it needs no locking.
     */
    private static final Set<String> PLAYERS_WITH_OPEN_DIALOGS = new HashSet<>();

    private RefusedConnectionNotifier() {
    }

    /**
     * Shows the host a dialog saying who was refused and why. Safe to call from the server's network thread: the
     * dialog is put up on the event dispatch thread.
     *
     * @param refusedConnection the connection that was refused
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static void showHostDialog(RefusedConnection refusedConnection) {
        SwingUtilities.invokeLater(() -> showHostDialogOnEventThread(refusedConnection));
    }

    private static void showHostDialogOnEventThread(RefusedConnection refusedConnection) {
        final String playerDescription = describePlayer(refusedConnection);
        if (!PLAYERS_WITH_OPEN_DIALOGS.add(playerDescription)) {
            return;
        }

        try {
            JOptionPane.showMessageDialog(null,
                  buildHostMessage(refusedConnection),
                  Messages.getString("RefusedConnectionNotifier.title"),
                  JOptionPane.WARNING_MESSAGE);
        } finally {
            PLAYERS_WITH_OPEN_DIALOGS.remove(playerDescription);
        }
    }

    /**
     * Builds the text of the host's dialog.
     *
     * <p>The text deliberately opens with fixed wording rather than the player's name: Swing renders any dialog
     * text that begins with {@code <html>} as HTML, and the name is chosen by the joining player.</p>
     *
     * @param refusedConnection the connection that was refused
     *
     * @return the message to show the host
     *
     * @author Illiani
     * @since 0.51.01
     */
    static String buildHostMessage(RefusedConnection refusedConnection) {
        return Messages.getString("RefusedConnectionNotifier.message",
              describePlayer(refusedConnection),
              refusedConnection.clientVersion(),
              refusedConnection.serverVersion());
    }

    /**
     * @param refusedConnection the connection that was refused
     *
     * @return the player's name and address, or just the address for a client too old to send its name
     *
     * @author Illiani
     * @since 0.51.01
     */
    static String describePlayer(RefusedConnection refusedConnection) {
        if (refusedConnection.hasPlayerName()) {
            return Messages.getString("RefusedConnectionNotifier.namedPlayer",
                  refusedConnection.playerName(),
                  refusedConnection.address());
        }

        return Messages.getString("RefusedConnectionNotifier.unnamedPlayer", refusedConnection.address());
    }
}
