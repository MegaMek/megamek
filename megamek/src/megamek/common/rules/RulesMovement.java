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


import megamek.common.units.EntityMovementMode;
import megamek.common.units.Entity;
import megamek.common.units.EntityMovementType;
import megamek.common.units.Mek;

public abstract class RulesMovement {
    /**
     * Can units skid?
     *
     * @return true if skidding is enabled
     */
    public abstract boolean skidEnabled();

    /**
     * Can a unit use run / flank MP in water.
     *
     * @param movementMode the movement mode of the unit
     * @param amphibious true if the unit is amphibious
     * @return true if the unit cannot run in water
     */
    public abstract boolean cannotRunInWater(EntityMovementMode movementMode,
                                    boolean amphibious);

    /**
     * What is the MP cost of moving into a water hex that is fully submerged.
     *
     * @return the MP cost for underwater movement
     */
    public abstract int getUnderwaterMPCost();

    /**
     * Can you move backwards up elevation?
     *
     * @param toBackwardsElevation true if moving backwards up elevation
     * @param entity the entity attempting to move
     * @return true if backwards elevation change is allowed
     */
    public abstract boolean enableBackwardsElevationChange(boolean toBackwardsElevation, Entity entity);

    /**
     * Do we add leg damage together, or do hips override.
     *
     * @param b true if using cumulative leg damage
     * @return true if leg damage is cumulative
     */
    public abstract boolean cumulativeLegDamage(boolean b);

    /**
     * Does 0 MP cause immobile? 
     *
     * @param walkMP the walking movement points
     * @return true if 0 MP causes immobile status
     */
    public boolean checkMPZeroCauseImmobile(int walkMP) {
        return (walkMP == 0); 
    }

    /**
     * What is our Running MP?
     *
     * @param badLegs the number of damaged legs
     * @param walkMP the walking movement points
     * @param runMP the running movement points
     * @return the effective running movement points
     */
    public abstract int getMekRunMP(int badLegs, int walkMP, int runMP);

    /**
     * Can it change more than 1 level when missing a leg?
     *
     * @param mek the MEK to check
     * @return true if maximum elevation can be reduced by more than 1 level
     */
    public abstract boolean reduceMaxElevation(Mek mek);

    /**
     * Is running into water considered dangerous.
     *
     * @param movementType the type of movement
     * @param movementMode the movement mode of the unit
     * @return true if moving into water is dangerous
     */
    public abstract boolean isMoveIntoWaterDangerous(EntityMovementType movementType, EntityMovementMode movementMode);
}
