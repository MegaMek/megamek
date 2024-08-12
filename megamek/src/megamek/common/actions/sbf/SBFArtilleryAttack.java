/*
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */
package megamek.common.actions.sbf;

import megamek.common.alphaStrike.BattleForceSUA;
import megamek.common.strategicBattleSystems.SBFGame;

import static org.apache.logging.log4j.LogManager.getLogger;

/**
 * This is a base class for SBF artillery attacks, indirect, against hexes, units or fortifications or direct.
 * IO:BF p.203
 */
public abstract class SBFArtilleryAttack extends AbstractSBFAttackAction {

    private final BattleForceSUA artilleryType;

    public SBFArtilleryAttack(int entityId, int targetId, BattleForceSUA artilleryType) {
        super(entityId, targetId);
        this.artilleryType = artilleryType;
    }

    @Override
    public boolean isDataValid(SBFGame game) {
        if (artilleryType == null) {
            getLogger().error("Missing artillery type! {}", this);
            return false;
        } else if (!artilleryType.isArtillery() || artilleryType.isArtilleryCannon()) {
            getLogger().error("Invalid artillery type! {}", this);
            return false;
        }
        return true;
    }
}
