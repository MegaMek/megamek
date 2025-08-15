/*
 * Copyright (C) 2024-2025 The MegaMek Team. All Rights Reserved.
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

package megamek.common.actions.sbf;

import megamek.common.alphaStrike.BattleForceSUA;
import megamek.common.strategicBattleSystems.SBFGame;
import megamek.logging.MMLogger;

/**
 * This is a base class for SBF artillery attacks, indirect, against hexes, units or fortifications or direct. IO:BF
 * p.203
 */
public abstract class SBFArtilleryAttack extends AbstractSBFAttackAction {
    private static final MMLogger logger = MMLogger.create(SBFArtilleryAttack.class);

    private final BattleForceSUA artilleryType;

    public SBFArtilleryAttack(int entityId, int targetId, BattleForceSUA artilleryType) {
        super(entityId, targetId);
        this.artilleryType = artilleryType;
    }

    @Override
    public boolean isDataValid(SBFGame game) {
        if (artilleryType == null) {
            String message = String.format("Missing artillery type! %h", this);
            logger.error(message);
            return false;
        } else if (!artilleryType.isArtillery() || artilleryType.isArtilleryCannon()) {
            String message = String.format("Invalid artillery type! %h", this);
            logger.error(message);
            return false;
        }
        return true;
    }
}
