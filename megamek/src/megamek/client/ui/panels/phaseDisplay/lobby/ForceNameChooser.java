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
package megamek.client.ui.panels.phaseDisplay.lobby;

import java.util.List;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JOptionPane;

import megamek.client.ui.Messages;
import megamek.common.Player;
import megamek.common.annotations.Nullable;
import megamek.common.force.ForceNames;
import megamek.common.game.Game;

/**
 * Asks for a force's name, offering names that suit its side and read well on the radio ("Charlie Lance", "Gamma
 * Star"), while still taking any name typed in.
 */
final class ForceNameChooser {

    private ForceNameChooser() {}

    /**
     * @param frame       the frame to center on
     * @param game        the game, for the owner's units
     * @param owner       the player the force belongs to
     * @param currentName the force's name now, or {@code null} for a new force
     *
     * @return the chosen name, trimmed, or {@code null} when cancelled or left blank
     */
    static @Nullable String choose(JFrame frame, Game game, @Nullable Player owner, @Nullable String currentName) {
        ForceNames.Style style = ForceNames.styleFor((owner == null) ? null : owner.getName(),
              (owner == null) ? List.of() : game.getPlayerEntities(owner, false));
        JComboBox<String> names = new JComboBox<>(ForceNames.suggestions(style).toArray(new String[0]));
        names.setEditable(true);
        names.setSelectedItem((currentName == null) ? "" : currentName);
        int result = JOptionPane.showConfirmDialog(frame, names, Messages.getString("ChatLounge.forceName.title"),
              JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
        Object chosen = names.getEditor().getItem();
        if ((result != JOptionPane.OK_OPTION) || (chosen == null) || chosen.toString().isBlank()) {
            return null;
        }
        return chosen.toString().trim();
    }
}
