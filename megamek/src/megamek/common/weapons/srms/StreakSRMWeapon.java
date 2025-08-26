/*
  Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
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

package megamek.common.weapons.srms;

import static megamek.common.game.IGame.LOGGER;

import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.annotations.Nullable;
import megamek.common.equipment.AmmoType;
import megamek.common.equipment.Mounted;
import megamek.common.game.Game;
import megamek.common.loaders.EntityLoadingException;
import megamek.common.units.Entity;
import megamek.common.weapons.handlers.AttackHandler;
import megamek.common.weapons.handlers.StreakHandler;
import megamek.server.totalwarfare.TWGameManager;

/**
 * @author Sebastian Brocks
 */
public abstract class StreakSRMWeapon extends SRMWeapon {
    private static final long serialVersionUID = 9157660680598071296L;

    public StreakSRMWeapon() {
        super();
        this.ammoType = AmmoType.AmmoTypeEnum.SRM_STREAK;
        flags = flags.or(F_PROTO_WEAPON).andNot(F_ARTEMIS_COMPATIBLE);
    }

    @Override
    public double getTonnage(Entity entity, int location, double size) {
        if ((null != entity) && entity.hasETypeFlag(Entity.ETYPE_PROTOMEK)) {
            return getRackSize() * 0.5;
        } else {
            return super.getTonnage(entity, location, size);
        }
    }

    @Override
    @Nullable
    public AttackHandler getCorrectHandler(ToHitData toHit, WeaponAttackAction waa, Game game, TWGameManager manager) {
        try {
            return new StreakHandler(toHit, waa, game, manager);
        } catch (EntityLoadingException ignored) {
            LOGGER.warn("Get Correct Handler - Attach Handler Received Null Entity.");
        }
        return null;
    }

    @Override
    public double getBattleForceDamage(int range, Mounted<?> fcs) {
        double damage = 0;
        if (range <= getLongRange()) {
            damage = getRackSize() * 2;
        }
        return damage / 10.0;
    }

    @Override
    public int getBattleForceClass() {
        return BF_CLASS_STANDARD;
    }

    @Override
    public String getSortingName() {
        String oneShotTag = hasFlag(F_ONE_SHOT) ? "OS " : "";
        if (name.contains("I-OS")) {
            oneShotTag = "XIOS ";
        }
        return "SRM Streak " + oneShotTag + rackSize;
    }
}
