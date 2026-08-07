package megamek.common.rules.core;
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

import megamek.client.ui.Messages;
import megamek.common.CriticalSlot;
import megamek.common.LosEffects;
import megamek.common.ToHitData;
import megamek.common.compute.Compute;
import megamek.common.enums.AimingMode;
import megamek.common.equipment.AmmoType;
import megamek.common.equipment.WeaponMounted;
import megamek.common.equipment.WeaponType;
import megamek.common.rules.RulesTarget;
import megamek.common.units.Dropship;
import megamek.common.units.Entity;
import megamek.common.units.EntityWeightClass;
import megamek.common.units.Mek;
import megamek.common.units.Targetable;
import megamek.common.weapons.artillery.ArtilleryCannonWeapon;
import megamek.common.weapons.artillery.ArtilleryWeapon;
import megamek.common.weapons.bayWeapons.ArtilleryBayWeapon;

import java.util.EnumSet;

public class CoreRulesTarget extends RulesTarget {
    /**
     * {@inheritDoc}
     * Large targets get a -1 modifier to hit them. Superheavy meks are large targets Core rules page 64, 240
     */
    @Override
    public int largeTargetModifier(int weightclass, boolean markedLarge) {
        if (weightclass == EntityWeightClass.WEIGHT_SUPER_HEAVY
              || weightclass == EntityWeightClass.WEIGHT_LARGE_SUPPORT
              || markedLarge) {
            return -1;
        }
        return 0;
    }

    /**
     * {@inheritDoc}
     * Aimed shots hit on d6 4+. Core p.70
     */
    @Override
    public boolean checkAimedLocation() {
        int roll = Compute.d6(1);
        if (roll >= 4) {
            return true;
        }
        return false;
    }

    /**
     * {@inheritDoc}
     * Secondary arcs are +1. Core p.64
     */
    @Override
    public int getSecondaryArcModifier() {
        return 1;
    }

    /**
     * {@inheritDoc}
     * Can shoot with one arm while prone. Core p.67
     */
    @Override
    public boolean proneFireWithOneArm(final boolean toProneFire) {
        return true;
    }

    /**
     * {@inheritDoc}
     * Only upper arm actuators increase the to hit for shooting. Core p.97
     */
    @Override
    public int getArmActuatorHitMod(Entity attacker, int location) {
        if (attacker.getBadCriticalSlots(CriticalSlot.TYPE_SYSTEM, Mek.ACTUATOR_UPPER_ARM, location) > 0) {
            return 1;
        }
        return 0;
    }
    
    /**
     * {@inheritDoc}
     * BAP reduces smoke within its range. It is blocked by ECM (Handled prior to this call) Core p.197, 124
     */
    @Override
    public int getBAPSmokeReduction(LosEffects los) {
        return los.getBAPReduceSmoke();
    }

    /**
     * {@inheritDoc}
     *
     * Note: all Ranged attack roll calls _must_ go through addImmobileMod to get to getImmobileMod or they may
     * illegally gain the -4 Immobile Target mod.
     *
     * Note: modifies the passed-in ToHitData toHit.
     */
    @Override
    public void addImmobileMod(Targetable target, ToHitData toHit, int aimingAt, WeaponType weaponType,
          WeaponMounted weapon, AmmoType ammoType, EnumSet<AmmoType.Munitions> munition, Entity entityTarget,
          AimingMode aimingMode) {

        if (weaponType != null) {
            // Bombs, Arrow IV, Artillery, Artillery Cannons, all AE in fact, do not gain
            // Likewise, MRM Saturation attacks never get the target immobile mod
            // Finally, Mek Mortar airburst munitions also ignore it.
            boolean mekMortarMunitionsIgnoreImmobile = weaponType.hasFlag(WeaponType.F_MEK_MORTAR)
                  && (ammoType != null) && munition.contains(AmmoType.Munitions.M_AIRBURST);

            if ((weaponType instanceof ArtilleryCannonWeapon)
                  || (weaponType instanceof ArtilleryWeapon)
                  || (weaponType instanceof ArtilleryBayWeapon)
                  || mekMortarMunitionsIgnoreImmobile
                  || (target.getTargetType() == Targetable.TYPE_HEX_BOMB)
                  || (target.getTargetType() == Targetable.TYPE_HEX_AERO_BOMB)
                  || (target.getTargetType() == Targetable.TYPE_SATURATION)
            ) {
                return;
            }

            ToHitData immobileMod;
            // grounded dropships are treated as immobile as well for purpose of the mods
            if (entityTarget instanceof Dropship && !entityTarget.isAirborne() && !entityTarget.isSpaceborne()) {
                immobileMod = new ToHitData(-4, Messages.getString("WeaponAttackAction.ImmobileDs"));
            } else {
                if (Compute.allowAimedShotWith(weapon, aimingMode)) {
                    immobileMod = Compute.getImmobileMod(target, aimingAt, aimingMode);
                } else {
                    immobileMod = Compute.getImmobileMod(target, aimingAt, AimingMode.NONE);
                }
            }

            if (immobileMod != null) {
                toHit.append(immobileMod);
            }
        }
    }
}
