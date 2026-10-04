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
package megamek.common.actions;

import java.io.Serial;

import megamek.client.ui.Messages;
import megamek.common.game.Game;
import megamek.common.units.Entity;

/**
 * A unit's order to point its Recon Camera at a hostile unit this turn (TO:AUE p.150). Declared in the Off-Board phase,
 * where TAG is fired, and rolled at the end of that phase so the result is known before indirect fire is declared.
 */
public class ReconCameraSpotAction extends AbstractEntityAction {

    @Serial
    private static final long serialVersionUID = 1L;

    private final int targetId;

    /**
     * @param entityId the unit with the camera
     * @param targetId the unit to spot
     */
    public ReconCameraSpotAction(int entityId, int targetId) {
        super(entityId);
        this.targetId = targetId;
    }

    /** @return the id of the unit to spot */
    public int getTargetId() {
        return targetId;
    }

    @Override
    public String toSummaryString(final Game game) {
        Entity target = game.getEntity(targetId);
        String targetName = (target == null) ? Messages.getString("ReconCamera.unknownTarget") : target.getShortName();
        return Messages.getString("ReconCamera.summary", targetName);
    }
}
