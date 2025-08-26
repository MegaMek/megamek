/*
 * Copyright (c) 2005 - Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2022-2025 The MegaMek Team. All Rights Reserved.
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

package megamek.common.weapons.lrms;

import static megamek.common.equipment.MountedHelper.isArtemisIV;
import static megamek.common.equipment.MountedHelper.isArtemisProto;
import static megamek.common.equipment.MountedHelper.isArtemisV;

import megamek.common.alphaStrike.AlphaStrikeElement;
import megamek.common.equipment.AmmoType;
import megamek.common.equipment.Mounted;
import megamek.common.options.IGameOptions;
import megamek.common.options.OptionsConstants;
import megamek.common.units.Entity;
import megamek.common.weapons.missiles.MissileWeapon;

/**
 * @author Sebastian Brocks
 */
public abstract class LRTWeapon extends MissileWeapon {

    private static final long serialVersionUID = -7350712286691532142L;

    public LRTWeapon() {
        super();
        ammoType = AmmoType.AmmoTypeEnum.LRM_TORPEDO;
        flags = flags.or(F_ARTEMIS_COMPATIBLE);

    }

    @Override
    public boolean hasIndirectFire() {
        return true;
    }

    @Override
    public double getTonnage(Entity entity, int location, double size) {
        if ((entity != null) && entity.hasETypeFlag(Entity.ETYPE_PROTOMEK)) {
            return getRackSize() * 0.2;
        } else {
            return super.getTonnage(entity, location, size);
        }
    }

    @Override
    public int getBattleForceClass() {
        return BF_CLASS_TORPEDO;
    }

    @Override
    public boolean isAlphaStrikeIndirectFire() {
        return false;
    }

    @Override
    public double getBattleForceDamage(int range, Mounted<?> fcs) {
        if (isClan()) {
            if (isArtemisIV(fcs) || isArtemisProto(fcs)) {
                return (range <= AlphaStrikeElement.LONG_RANGE) ? 0.4 * rackSize / 5 : 0;
            } else if (isArtemisV(fcs)) {
                return (range <= AlphaStrikeElement.LONG_RANGE) ? 0.42 * rackSize / 5 : 0;
            } else {
                return (range <= AlphaStrikeElement.LONG_RANGE) ? 0.3 * rackSize / 5 : 0;
            }
        } else {
            if (isArtemisIV(fcs) || isArtemisProto(fcs)) {
                if (range == AlphaStrikeElement.SHORT_RANGE) {
                    return 0.2 * rackSize / 5;
                } else {
                    return (range <= AlphaStrikeElement.LONG_RANGE) ? 0.4 * rackSize / 5 : 0;
                }
            } else {
                if (range == AlphaStrikeElement.SHORT_RANGE) {
                    return 0.15 * rackSize / 5;
                } else {
                    return (range <= AlphaStrikeElement.LONG_RANGE) ? 0.3 * rackSize / 5 : 0;
                }
            }
        }
    }

    @Override
    public void adaptToGameOptions(IGameOptions gameOptions) {
        super.adaptToGameOptions(gameOptions);

        // Indirect Fire
        if (gameOptions.booleanOption(OptionsConstants.BASE_INDIRECT_FIRE)) {
            addMode("");
            addMode("Indirect");
        } else {
            removeMode("");
            removeMode("Indirect");
        }
    }

    @Override
    public String getSortingName() {
        String oneShotTag = hasFlag(F_ONE_SHOT) ? "OS " : "";
        if (name.contains("I-OS")) {
            oneShotTag = "XIOS ";
        }
        return "LRT " + oneShotTag + ((rackSize < 10) ? "0" + rackSize : rackSize);
    }
}
