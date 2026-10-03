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
package megamek.client.ui.dialogs.BotCommands;

import java.awt.Container;
import java.awt.event.ActionEvent;
import java.util.List;
import java.util.Optional;
import javax.swing.JFrame;

import megamek.client.ui.Messages;
import megamek.client.ui.dialogs.buttonDialogs.AbstractButtonDialog;
import megamek.common.OffBoardDirection;
import megamek.common.annotations.Nullable;
import megamek.common.orders.LanceRole;
import megamek.common.orders.LanceRoles;

/**
 * The lobby's Role dialog for one bot lance: the same panel as the Move Order editor's Role tab, so a lance can start
 * the game as a convoy or an escort.
 */
public class LanceRoleDialog extends AbstractButtonDialog {

    private final String lanceName;
    private final List<LanceRoles.ConvoyChoice> convoys;
    private final OffBoardDirection defaultExitEdge;
    private LanceRolePanel rolePanel;

    /**
     * @param frame           the owner
     * @param lanceName       the lance's name
     * @param convoys         the convoy lances on the side the lance can escort
     * @param defaultExitEdge the exit edge a new convoy starts with
     * @param shown           the role to show first, or {@code null} for none
     */
    public LanceRoleDialog(JFrame frame, String lanceName, List<LanceRoles.ConvoyChoice> convoys,
          OffBoardDirection defaultExitEdge, @Nullable LanceRole shown) {
        super(frame, true, "LanceRoleDialog", "BotCommandPanel.Role.dialogTitle");
        this.lanceName = lanceName;
        this.convoys = convoys;
        this.defaultExitEdge = defaultExitEdge;
        initialize();
        setTitle(Messages.getString("BotCommandPanel.Role.dialogTitleFor", lanceName));
        rolePanel.setRole(shown);
    }

    @Override
    protected Container createCenterPane() {
        rolePanel = new LanceRolePanel(lanceName, convoys, defaultExitEdge);
        return rolePanel;
    }

    @Override
    protected void okButtonActionPerformed(ActionEvent event) {
        // an escort with no convoy or no place round it cannot be set; the panel's summary says what is missing
        if (rolePanel.isComplete()) {
            super.okButtonActionPerformed(event);
        }
    }

    /**
     * @return the lance the escort is given when it is not a convoy yet, so it becomes one; empty otherwise
     */
    public Optional<LanceRoles.ConvoyChoice> lanceToMakeConvoy() {
        return rolePanel.lanceToMakeConvoy();
    }

    /**
     * @return the role chosen, or {@code null} for none
     */
    public @Nullable LanceRole getRole() {
        return rolePanel.getRole();
    }
}
