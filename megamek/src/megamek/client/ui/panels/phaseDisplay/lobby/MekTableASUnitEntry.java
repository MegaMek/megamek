/*
 * Copyright (C) 2023-2025 The MegaMek Team. All Rights Reserved.
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

import static megamek.client.ui.panels.phaseDisplay.lobby.MekTableModel.DOT_SPACER;
import static megamek.client.ui.util.UIUtil.ECM_SIGN;
import static megamek.client.ui.util.UIUtil.uiC3Color;
import static megamek.client.ui.util.UIUtil.uiGray;
import static megamek.client.ui.util.UIUtil.uiLightGreen;
import static megamek.common.alphaStrike.BattleForceSUA.AECM;
import static megamek.common.alphaStrike.BattleForceSUA.ECM;
import static megamek.common.alphaStrike.BattleForceSUA.LECM;

import megamek.client.Client;
import megamek.client.ui.Messages;
import megamek.client.ui.util.UIUtil;
import megamek.common.game.Game;
import megamek.common.Player;
import megamek.common.alphaStrike.AlphaStrikeElement;
import megamek.common.options.GameOptions;
import megamek.common.options.OptionsConstants;
import megamek.common.preference.PreferenceManager;

public class MekTableASUnitEntry {

    /**
     * Creates and returns the display content of the Unit column for the given AlphaStrikeElement and for the
     * non-compact display mode. When blindDrop is true, the unit details are not given.
     */
    static String fullEntry(AlphaStrikeElement element, ChatLounge lobby, boolean forceView, boolean compactView) {
        StringBuilder result = new StringBuilder("<HTML><NOBR>");

        Client client = lobby.getClientGUI().getClient();
        Game game = client.getGame();
        GameOptions options = game.getOptions();
        Player localPlayer = client.getLocalPlayer();
        Player owner = game.getPlayer(element.getOwnerId());
        boolean hideEntity = owner.isEnemyOf(localPlayer)
              && options.booleanOption(OptionsConstants.BASE_BLIND_DROP);

        if (hideEntity) {
            result.append(DOT_SPACER);
            if (element.isInfantry()) {
                result.append(Messages.getString("ChatLounge.0"));
            } else if (element.isProtoMek()) {
                result.append(Messages.getString("ChatLounge.1"));
            } else if (element.isFighter()) {
                result.append(Messages.getString("ChatLounge.4"));
            } else if (element.isMek()) {
                result.append(Messages.getString("ChatLounge.3"));
            } else if (element.isVehicle()) {
                result.append(Messages.getString("ChatLounge.6"));
            } else {
                result.append("SZ ").append(element.getSize()).append(" ").append(element.getASUnitType());
            }
            result.append(DOT_SPACER);
            return result.toString();
        }

        // First line

        // Unit Name
        result.append(UIUtil.fontHTML(uiLightGreen()));
        result.append("<B>").append(element.getName()).append("</B></FONT>");

        // ID
        if (PreferenceManager.getClientPreferences().getShowUnitId()) {
            result.append(UIUtil.fontHTML(uiGray()));
            result.append(" [ID: ").append(element.getId()).append("]</FONT>");
        }
        if (!forceView && !compactView) {
            result.append("<BR>");
        } else {
            result.append(DOT_SPACER);
        }

        // Tonnage
        if (forceView) {
            result.append(DOT_SPACER);
        }
        result.append(element.getASUnitType());
        result.append(DOT_SPACER);
        result.append("SZ ").append(element.getSize());
        result.append("</FONT>");
        result.append(DOT_SPACER);
        result.append("MV ").append(element.getMovementAsString());
        if (element.usesTMM()) {
            result.append(DOT_SPACER);
            result.append("TMM ").append(element.getTMM());
        }
        if (!element.usesArcs()) {
            result.append(DOT_SPACER);
            result.append("DMG ").append(element.getStandardDamage());
        }
        if (element.hasOV()) {
            result.append(DOT_SPACER);
            result.append("OV ").append(element.getOV());
        }
        if (element.getRole().hasRole()) {
            result.append(DOT_SPACER);
            result.append(element.getRole());
        }

        // ECM
        if (element.hasAnySUAOf(ECM, LECM, AECM)) {
            result.append(DOT_SPACER).append(UIUtil.fontHTML(uiC3Color()));
            result.append(ECM_SIGN + " ");
            result.append(Messages.getString("BoardView1.ecmSource"));
            result.append("</FONT>");
        }

        // SECOND OR THIRD LINE in Force View / Table
        if (!compactView) {
            result.append("<BR>");
        } else {
            result.append(DOT_SPACER);
        }

        if (element.usesArcs() || compactView) {
            result.append("\u25cf ").append(element.getCurrentStructure());
            result.append(" \u25cb ").append(element.getCurrentArmor());
        } else {
            result.append(UIUtil.repeat("\u25cf", element.getCurrentStructure()));
            result.append(UIUtil.repeat("\u25cb", element.getCurrentArmor()));
        }

        if (element.usesThreshold()) {
            result.append(DOT_SPACER);
            result.append(" TH ");
            result.append(element.getThreshold());
        }

        String specials = element.getSpecialsDisplayString(element);
        if (!specials.isBlank()) {
            result.append(DOT_SPACER);
            result.append(" ").append(specials);
        }

        return result.toString();
    }

    private MekTableASUnitEntry() {}
}
