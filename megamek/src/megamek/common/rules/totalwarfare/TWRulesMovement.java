package megamek.common.rules.totalwarfare;

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
import megamek.common.compute.Compute;
import megamek.common.game.Game;
import megamek.common.moves.MovePath;
import megamek.common.moves.MoveStep;
import megamek.common.rules.RulesMovement;
import megamek.common.units.Entity;
import megamek.common.units.EntityMovementMode;
import megamek.common.units.EntityMovementType;
import megamek.common.units.Mek;

public class TWRulesMovement extends RulesMovement {

    /**
     * Units can skid
     *
     * @return true if skidding is enabled
     */
    @Override
    public boolean skidEnabled() {
        return true;
    }

    /**
     * Can a unit use run / flank MP in water. Vehicles and mechs cannot
     *
     * @param movementMode the movement mode of the unit
     * @param amphibious   true if the unit is amphibious
     *
     * @return true if the unit cannot run in water
     */
    @Override
    public boolean cannotRunInWater(EntityMovementMode movementMode,
          boolean amphibious) {
        if ((movementMode != EntityMovementMode.HOVER) &&
              (movementMode != EntityMovementMode.NAVAL) &&
              (movementMode != EntityMovementMode.HYDROFOIL) &&
              (movementMode != EntityMovementMode.INF_UMU) &&
              (movementMode != EntityMovementMode.SUBMARINE) &&
              (movementMode != EntityMovementMode.VTOL) &&
              (movementMode != EntityMovementMode.WIGE) &&
              !amphibious) {
            return true;
        }
        return false;
    }

    /**
     * What is the MP cost of moving into a water hex that is fully submerged. The cost is 3MP
     *
     * @return the MP cost for underwater movement
     */
    @Override
    public int getUnderwaterMPCost() {
        return 3;
    }

    /**
     * Can you move backwards up elevation? Backwards elevation changes only if tacops rule
     *
     * @param toBackwardsElevation true if moving backwards up elevation
     * @param entity               the entity attempting to move
     *
     * @return true if backwards elevation change is allowed
     */
    @Override
    public boolean enableBackwardsElevationChange(final boolean toBackwardsElevation, Entity entity) {
        return toBackwardsElevation;
    }

    /**
     * Do we add leg damage together, no unless TO.
     *
     * @param bTOLegDamage true if TO leg damage is enabled
     *
     * @return true if leg damage is cumulative
     */
    @Override
    public boolean cumulativeLegDamage(boolean bTOLegDamage) {
        return bTOLegDamage;
    }

    /**
     * Does 0 MP cause immobile? Only unconscious, shut down, or leg destruction causes immobile
     *
     * @param walkMP the walking movement points
     *
     * @return true if 0 MP causes immobile status
     */
    @Override
    public boolean checkMPZeroCauseImmobile(int walkMP) {return false;}

    /**
     * {@inheritDoc}
     * Lost a leg? No running
     */
    @Override
    public int getMekRunMP(int badLegs, int walkMP, int runMP, boolean isQuad) {
        if ((badLegs == 0 && !isQuad) || (badLegs <= 1 && isQuad)) {
            return runMP;
        } else {
            return walkMP;
        }
    }

    /**
     * Moving into water always triggers PSR danger
     *
     * @param movementType the type of movement
     * @param movementMode the movement mode of the unit
     *
     * @return true if moving into water is dangerous
     */
    @Override
    public boolean isMoveIntoWaterDangerous(EntityMovementType movementType, EntityMovementMode movementMode) {
        return true;
    }

    /**
     * Can it change more than 1 level when missing a leg? TW doesn't care
     *
     * @param mek the Mek to check
     *
     * @return true if maximum elevation can be reduced
     */
    @Override
    public boolean reduceMaxElevation(Mek mek) {
        return false;
    }

    /**
     * What are the criteria to allow for domino effect changes BMM p 56
     *
     * @param direction     The direction the violation is coming from
     * @param entity        the entity causing the domino effect
     * @param stepForward   potential step forwards
     * @param stepBackwards potential step backwards
     * @param violation     entity being displaced
     *
     * @return True if it can possibly step out of the way
     */
    @Override
    public boolean dominoEffectMovementCriteria(final int direction, final Entity entity, final MovePath stepForward,
          final MovePath stepBackwards,
          final Entity violation) {
        // if the direction comes from a side, Entity didn't jump, and it
        // has MP left to use, it can try to move.
        if (direction != violation.getFacing() &&
              (direction != ((violation.getFacing() + 3) % 6)) &&
              !entity.getIsJumpingNow() &&
              (stepForward.isMoveLegal() || stepBackwards.isMoveLegal())) {
            return true;
        }
        return false;
    }

    /**
     * Domino displacement costs MP, so return true
     *
     * @return false if there is no cost, true if there is
     */
    @Override
    public boolean getDominoDisplacementCostsMP() {return true;}

    /**
     * {@inheritDoc}
     * Dominos
     */
    @Override
    public boolean isDominoMoveLegal(final int direction, final Entity entity, final MovePath movePath,
          boolean forwards) {
        if (movePath.isMoveLegal()) {
            return true;
        }
        return false;
    }

    /**
     * Do we change the accidental fall elevation? No.
     *
     * @param fallElevation height of the fall
     * @param hitHeight     height of the unit hit
     *
     * @return the modified height for damage
     */
    @Override
    public int getAccidentalFallElevation(final int fallElevation, final int hitHeight) {return fallElevation;}


    /**
     * How should we displace a unit that had another fall on it accidentally? Use a distribution and find the first
     * valid hex we can displace into
     *
     * @param game      the game
     * @param entityId  the entity being displaced
     * @param src       the source hex
     * @param direction the direction that it is coming from
     * @param range     how far to displace (used for Dropships)
     *
     * @return the destination Coords
     */
    @Nullable
    @Override
    public Coords getAccidentalFallDisplacement(Game game, int entityId, Coords src, int direction,
          int range) {
        int[] offsets = { 0, 1, 5, 2, 4, 3 };
        for (int offset : offsets) {
            Coords dest = src.translated((direction + offset) % 6, range);
            if (Compute.isValidDisplacement(game, entityId, src, dest)) {
                return dest;
            }
            // code here borrowed from Compute.coordsAtRange
            for (int count = 1; count < range; count++) {
                dest = dest.translated((direction + offset + 2) % 6);
                if (Compute.isValidDisplacement(game, entityId, src, dest)) {
                    return dest;
                }
            }
        }
        return null;
    }

    /**
     * When performing a DFA, and we move closer to the target, what elevation is the attacker at?
     *
     * @param game       the game object
     * @param attackerId entityId of the attacker
     * @param targetId   entityId of the defender
     * @param step       the movement step as accepted.
     *
     * @return int, the elevation above the underlying terrain the attacker should be at
     */
    @Override
    public int getDFAElevation(Game game, int attackerId, int targetId, MoveStep step) {
        return step.getElevation();
    }
}
