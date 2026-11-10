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

import java.awt.GraphicsEnvironment;
import java.util.HashSet;
import java.util.Set;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import jakarta.annotation.Nullable;
import megamek.Version;
import megamek.client.ui.Messages;
import megamek.server.RefusedConnection;

/**
 * Tells a player already in the game, with a dialog, that the server turned someone away for running a different
 * version.
 *
 * <p>The server sends every connected player a {@code PLAYER_REFUSED_DIFFERENT_VERSION} packet, and each human client
 * passes it to {@link #showPlayerDialog(Object, Version, Version)}. Bots and headless clients do not.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
public final class RefusedConnectionNotifier {

    /**
     * The refused players whose dialog is open right now. Someone who keeps pressing Connect would otherwise stack a
     * fresh dialog on every player for each attempt; while one is showing for them, further attempts are skipped.
     * Touched only on the event dispatch thread, so it needs no locking.
     */
    private static final Set<String> PLAYERS_WITH_OPEN_DIALOGS = new HashSet<>();

    private RefusedConnectionNotifier() {
    }

    /**
     * Shows this player a dialog saying who was refused and why. Safe to call from the network thread: the dialog is
     * put up on the event dispatch thread. Does nothing when there is no screen to show it on.
     *
     * @param rawPlayerName  the refused player's name as it arrived, or {@code null} when their client is too old to
     *                       send one
     * @param refusedVersion the version the refused player is running
     * @param gameVersion    the version this game is running
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static void showPlayerDialog(@Nullable Object rawPlayerName, Version refusedVersion, Version gameVersion) {
        if (GraphicsEnvironment.isHeadless()) {
            return;
        }

        final String playerName = RefusedConnection.cleanPlayerName(rawPlayerName);
        SwingUtilities.invokeLater(() -> showPlayerDialogOnEventThread(playerName, refusedVersion, gameVersion));
    }

    private static void showPlayerDialogOnEventThread(@Nullable String playerName, Version refusedVersion,
          Version gameVersion) {
        final String dialogKey = (playerName == null) ? "" : playerName;
        if (!PLAYERS_WITH_OPEN_DIALOGS.add(dialogKey)) {
            return;
        }

        try {
            JOptionPane.showMessageDialog(null,
                  buildPlayerMessage(playerName, refusedVersion, gameVersion),
                  Messages.getString("RefusedConnectionNotifier.title"),
                  JOptionPane.WARNING_MESSAGE);
        } finally {
            PLAYERS_WITH_OPEN_DIALOGS.remove(dialogKey);
        }
    }

    /**
     * Builds the text of the dialog.
     *
     * <p>The text deliberately opens with fixed wording rather than the refused player's name: Swing renders any
     * dialog text that begins with {@code <html>} as HTML, and the name is chosen by the refused player.</p>
     *
     * @param playerName     the refused player's cleaned name, or {@code null} when it is not known
     * @param refusedVersion the version the refused player is running
     * @param gameVersion    the version this game is running
     *
     * @return the message to show
     *
     * @author Illiani
     * @since 0.51.01
     */
    static String buildPlayerMessage(@Nullable String playerName, Version refusedVersion, Version gameVersion) {
        final String playerDescription = (playerName == null)
              ? Messages.getString("RefusedConnectionNotifier.unnamedPlayer")
              : playerName;
        return Messages.getString("RefusedConnectionNotifier.message",
              playerDescription,
              refusedVersion,
              gameVersion);
    }
}
