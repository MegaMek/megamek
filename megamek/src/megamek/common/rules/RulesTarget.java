package megamek.common.rules;
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
import megamek.common.LosEffects;
import megamek.common.ToHitData;
import megamek.common.board.Coords;
import megamek.common.compute.Compute;
import megamek.common.enums.AimingMode;
import megamek.common.equipment.AmmoType;
import megamek.common.equipment.WeaponMounted;
import megamek.common.equipment.WeaponType;
import megamek.common.units.Dropship;
import megamek.common.units.Entity;
import megamek.common.units.Targetable;
import megamek.common.weapons.artillery.ArtilleryCannonWeapon;

import java.util.ArrayList;
import java.util.EnumSet;

public abstract class RulesTarget {
    /**
     * Check if the target is large and if there is a modifier.
     *
     * @param weightclass the weight class of the target
     * @param markedLarge true if the target is marked as large
     * @return the large target modifier
     */
    public abstract int largeTargetModifier(int weightclass, boolean markedLarge);

    /**
     * Alternate call for largeTargetModifier(int weightclass, boolean markedLarge) with default markedLarge = false.
     * @param weightclass the weight class of the target
     * @return the large target modifier
     */
    public int largeTargetModifier(int weightclass) { return largeTargetModifier(weightclass, false); };

    /**
     * Alternate call for largeTargetModifier(int weightclass, boolean markedLarge) with default weightclass = 0.
     * @param markedLarge the large target flag
     * @return the large target modifier
     */
    public int largeTargetModifier(boolean markedLarge) {return largeTargetModifier(0,markedLarge);};

    /**
     * Do we hit the aimed location?
     *
     * @return true if the aimed location is hit
     */
    public abstract boolean checkAimedLocation();

    /**
     * What is the secondary arc modifier.
     *
     * @return the secondary arc modifier
     */
    public abstract int getSecondaryArcModifier();

    /**
     * Can you shoot with one arm while prone.
     *
     * @param toProneFire true if checking prone fire capability
     * @return true if you can shoot with one arm while prone
     */
    public abstract boolean proneFireWithOneArm(boolean toProneFire);

    /**
     * What is the arm actuator hit mod for shooting.
     *
     * @param attacker the attacking entity
     * @param location the arm location being used
     * @return the arm actuator hit modifier
     */
    public abstract int getArmActuatorHitMod(Entity attacker, int location);

    /**
     * Do we reduce smoke?
     *
     * @param los the line of sight effects
     * @return the BAP smoke reduction amount
     */
    public abstract int getBAPSmokeReduction(LosEffects los);

    /**
     * Compute whether this specific target will get an immobile mod, and applies the mod if necessary
     *
     * @param target        Targetable being attacked
     * @param toHit         Existing ToHitData
     * @param aimingAt      Aimed-at location, if applicable
     * @param weaponType    Type of attacking weapon
     * @param weapon        The weapon itself
     * @param ammoType      Type of ammo
     * @param munition      Collection of munition information
     * @param entityTarget  Entity version of the target, if it's an entity, else null
     * @param aimingMode    Aiming mode data
     *
     * Note: modifies the passed-in ToHitData toHit.
     */
    public abstract void addImmobileMod(Targetable target, ToHitData toHit, int aimingAt, WeaponType weaponType,
          WeaponMounted weapon, AmmoType ammoType, EnumSet<AmmoType.Munitions> munition, Entity entityTarget,
         AimingMode aimingMode);
}
