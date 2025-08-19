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

package megamek.common.weapons.srms;

import static megamek.common.equipment.MountedHelper.isArtemisIV;
import static megamek.common.equipment.MountedHelper.isArtemisProto;
import static megamek.common.equipment.MountedHelper.isArtemisV;

import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.alphaStrike.AlphaStrikeElement;
import megamek.common.equipment.AmmoType;
import megamek.common.equipment.Mounted;
import megamek.common.game.Game;
import megamek.common.units.Entity;
import megamek.common.weapons.handlers.AttackHandler;
import megamek.common.weapons.handlers.srm.SRMHandler;
import megamek.common.weapons.missiles.MissileWeapon;
import megamek.server.totalwarfare.TWGameManager;

/**
 * @author Sebastian Brocks
 */
public abstract class SRTWeapon extends MissileWeapon {
    private static final long serialVersionUID = 2209880229033489588L;

    public SRTWeapon() {
        super();
        ammoType = AmmoType.AmmoTypeEnum.SRM_TORPEDO;
        flags = flags.or(F_ARTEMIS_COMPATIBLE);
    }

    @Override
    public double getTonnage(Entity entity, int location, double size) {
        if ((null != entity) && entity.hasETypeFlag(Entity.ETYPE_PROTOMEK)) {
            return getRackSize() * 0.25;
        } else {
            return super.getTonnage(entity, location, size);
        }
    }

    @Override
    protected AttackHandler getCorrectHandler(ToHitData toHit,
          WeaponAttackAction waa, Game game, TWGameManager manager) {
        return new SRMHandler(toHit, waa, game, manager);
    }

    @Override
    public double getBattleForceDamage(int range, Mounted<?> fcs) {
        if (range >= AlphaStrikeElement.LONG_RANGE) {
            return 0;
        }
        if (isClan()) {
            if (rackSize == 2) {
                if (isArtemisIV(fcs) || isArtemisProto(fcs)) {
                    return 0.4;
                } else if (isArtemisV(fcs)) {
                    return 0.42;
                } else {
                    return 0.2;
                }
            } else if (rackSize == 4) {
                if (isArtemisV(fcs)) {
                    return 0.63;
                } else {
                    return 0.6;
                }
            } else {
                if (isArtemisIV(fcs) || isArtemisProto(fcs)) {
                    return 0.1;
                } else if (isArtemisV(fcs)) {
                    return 1.05;
                } else {
                    return 0.8;
                }
            }
        } else {
            if (rackSize == 2) {
                return (isArtemisIV(fcs) || isArtemisProto(fcs)) ? 0.4 : 0.2;
            } else if (rackSize == 4) {
                return 0.6;
            } else {
                return (isArtemisIV(fcs) || isArtemisProto(fcs)) ? 1 : 0.8;
            }
        }
    }

    @Override
    public double getBattleForceDamage(int range, int baSquadSize) {
        return super.getBattleForceDamage(range, baSquadSize) * 2;
    }

    @Override
    public int getBattleForceClass() {
        return BF_CLASS_TORPEDO;
    }

    @Override
    public String getSortingName() {
        String oneShotTag = hasFlag(F_ONE_SHOT) ? "OS " : "";
        if (name.contains("I-OS")) {
            oneShotTag = "XIOS ";
        }
        return "SRT " + oneShotTag + rackSize;
    }
}
