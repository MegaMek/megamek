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


import megamek.common.annotations.Nullable;
import megamek.common.board.Coords;
import megamek.common.game.Game;
import megamek.common.moves.MovePath;
import megamek.common.moves.MoveStep;
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
     * @param isQuad is the unit a quad             
     * @return the effective running movement points
     */
    public abstract int getMekRunMP(int badLegs, int walkMP, int runMP, boolean isQuad);

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

    /**
     * What are the criteria to allow for domino effect changes
     * 
     * @param direction The direction the violation is coming from
     * @param entity the entity causing the domino effect
     * @param stepForward potential step forwards
     * @param stepBackwards potential step backwards
     * @param violation entity being displaced
     * @return True if it can possibly step out of the way
     */
    public abstract boolean dominoEffectMovementCriteria(final int direction, final Entity entity, final MovePath stepForward, final MovePath stepBackwards,
          final Entity violation);

    /**
     * This returns if a domino displacement should cost MP
     * @return true if there is a cost, false if no cost
     */
    public abstract boolean getDominoDisplacementCostsMP();

    /**
     * This is a helper for dominoEffectMovementCriteria. It returns true if you can move in that direction
     * 
     * @param direction direction of source of domino 
     * @param entity entity being displaced
     * @param movePath the movePath of the step
     * @param forwards is it forwards or backwards
     * @return
     */
    public abstract boolean isDominoMoveLegal(final int direction, final Entity entity, final MovePath movePath,
          boolean forwards);

    /**
     * Do we change the accidental fall elevation?
     * 
     * @param fallElevation height of the fall
     * @param hitHeight height of the unit hit
     * @return the modified height for damage
     */
    public abstract int getAccidentalFallElevation(final int fallElevation, final int hitHeight);

    /**
     * How should we displace a unit that had another fall on it accidentally?
     * If null is returned, no valid displacement could be found
     *
     * @param game the game
     * @param entityId the entity being displaced
     * @param src the source hex
     * @param direction the direction that it is coming from
     * @param range how far to displace (used for Dropships)
     * @return the destination Coords.
     */
    @Nullable
    public abstract Coords getAccidentalFallDisplacement(Game game, int entityId, Coords src, int direction,
          int range);

    /**
     * When performing a DFA, and we move closer to the target, what elevation is the attacker at?
     *
     * @param game the game object
     * @param attackerId entityId of the attacker
     * @param targetId entityId of the defender
     * @param step the movement step as accepted.
     * @return int, the elevation above the underlying terrain the attacker should be at
     */
    public abstract int getDFAElevation(Game game, int attackerId, int targetId, MoveStep step);
}
