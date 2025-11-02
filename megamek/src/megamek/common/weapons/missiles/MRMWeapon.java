/*
 * Copyright (c) 2005 - Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2007-2025 The MegaMek Team. All Rights Reserved.
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

package megamek.common.weapons.missiles;

import static megamek.common.game.IGame.LOGGER;

import java.io.Serial;

import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.annotations.Nullable;
import megamek.common.compute.Compute;
import megamek.common.equipment.AmmoType;
import megamek.common.equipment.MiscType;
import megamek.common.equipment.Mounted;
import megamek.common.game.Game;
import megamek.common.loaders.EntityLoadingException;
import megamek.common.options.OptionsConstants;
import megamek.common.weapons.handlers.AttackHandler;
import megamek.common.weapons.handlers.MRMHandler;
import megamek.server.totalWarfare.TWGameManager;

/**
 * @author Sebastian Brocks
 */
public abstract class MRMWeapon extends MissileWeapon {

    @Serial
    private static final long serialVersionUID = 274817921444431878L;

    public MRMWeapon() {
        super();
        ammoType = AmmoType.AmmoTypeEnum.MRM;
        toHitModifier = 1;
        atClass = CLASS_MRM;
    }

    @Override
    // PLAYTEST3 MRMS no longer have a +1 to hit.
    public int getToHitModifier(@Nullable Mounted<?> mounted) {
        if (mounted != null && mounted.getEntity() != null && mounted.getEntity().getGame() != null && mounted.getEntity().getGame().getOptions().booleanOption(OptionsConstants.PLAYTEST_3)) {
            return 0;
        } else {
            return toHitModifier;
        }
    }
    
    @Override
    @Nullable
    public AttackHandler getCorrectHandler(ToHitData toHit, WeaponAttackAction waa, Game game, TWGameManager manager) {
        try {
            return new MRMHandler(toHit, waa, game, manager);
        } catch (EntityLoadingException ignored) {
            LOGGER.warn("Get Correct Handler - Attach Handler Received Null Entity.");
        }
        return null;
    }

    @Override
    public double getBattleForceDamage(int range, Mounted<?> fcs) {
        double damage = 0;
        if (range > getLongRange()) {
            return damage;
        }
        if (fcs != null && fcs.getType() instanceof MiscType
              && fcs.getType().hasFlag(MiscType.F_APOLLO)) {
            damage = Compute.calculateClusterHitTableAmount(6, getRackSize());
        } else {
            damage = Compute.calculateClusterHitTableAmount(7, getRackSize());
            damage *= 0.95; // +1 to hit
        }
        if (range == 0 && getMinimumRange() > 0) {
            damage = adjustBattleForceDamageForMinRange(damage);
        }
        return damage / 10.0;
    }

    @Override
    public String getSortingName() {
        String oneShotTag = hasFlag(F_ONE_SHOT) ? "OS " : "";
        if (name.contains("I-OS")) {
            oneShotTag = "XIOS ";
        }
        return "MRM " + oneShotTag + rackSize;
    }
}
