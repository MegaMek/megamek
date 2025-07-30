/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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
package megamek.client.ui.entityreadout;

import megamek.client.ui.dialogs.abstractDialogs.AbstractDialog;
import megamek.common.Game;

import javax.swing.*;
import java.awt.*;

/**
 * A dialog showing the unit readout, TRO, AS Card and Faction info for a given unit. The Readout is kept
 * updated until the window is closed. This dialog is always non-modal.
 */
public class LiveReadoutDialog extends AbstractDialog {

    private final LiveEntityViewPane entityView;

    /** Constructs a non-modal dialog showing and updating the EntityReadout of the given entity. */
    public LiveReadoutDialog(JFrame frame, Game game, int entityId) {
        super(frame, false, "EntityReadoutDialog", "EntityReadoutDialog.title");
        entityView = new LiveEntityViewPane(frame, game, entityId);
        initialize();
    }

    @Override
    protected Container createCenterPane() {
        entityView.initialize();
        return entityView;
    }

    @Override
    protected void cancelAction() {
        entityView.dispose();
        dispose();
    }
}
