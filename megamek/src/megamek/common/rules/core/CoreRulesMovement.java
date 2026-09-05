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
import megamek.common.units.QuadMek;

import java.util.ArrayList;

public class CoreRulesMovement extends RulesMovement {
    /**
     * {@inheritDoc}
     * No skidding in Core Rules
     */
     public boolean skidEnabled() {
        return false;
    }

    /**
     * {@inheritDoc}
     * Can you run / flank in water? Core p.51
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
              (movementMode != EntityMovementMode.BIPED) &&
              (movementMode != EntityMovementMode.QUAD) &&
              (movementMode != EntityMovementMode.TRIPOD) &&
              !amphibious) {
            return true;
        }
        return false;
    }

    /**
     * {@inheritDoc}
     * Even with a destroyed leg, we can run. Core p.90
     */
    @Override
    public int getMekRunMP(int badLegs, int walkMP, int runMP, boolean isQuad) {
        return runMP;
    }

    /**
     * {@inheritDoc}
     * Fully underwater hexes cost 2MP. Core p.51
     */
    @Override
    public int getUnderwaterMPCost() {
        return 2;
    }

    /**
     * {@inheritDoc}
     * Backwards elevation changes are enabled. Core p.46
     */
    @Override
    public boolean enableBackwardsElevationChange(final boolean toBackwardsElevation, Entity entity) {
        if (entity instanceof Mek) {
            int legsDestroyed = ((Mek) entity).countBadLegs();
            if (legsDestroyed >= 1 && !(entity instanceof QuadMek)) {
                // No backwards elevation with a bad leg
                return false;
            } else if (legsDestroyed >= 3) {
                // Quads run into this with 3 legs gone
                return false;
            }
            return true;
        }
        // Not a Mek
        return false;
    }

    /**
     * {@inheritDoc}    
     * Do we add leg damage together, yes. Core p.98
     */
    @Override
    public boolean cumulativeLegDamage(boolean bTOLegDamage) {
        return true;
    }

    /**
     * {@inheritDoc}
     * meks can only change 1 elevation level when leg destroyed. Core p.90, p.238
     */
    @Override
    public boolean reduceMaxElevation(Mek mek) {
        if (mek.atLeastOneBadLeg()) {
            if (mek instanceof QuadMek) { 
                 if (mek.countBadLegs() >= 3) {
                    return true;
                 } else {
                     return false;
                 }
            } 
            return false;
        }
        if (mek.hasHipCrit()) {
            return true;
        }
        return false;
    }

    /**
     * {@inheritDoc}
     * Moving into water only triggers danger when running and you can run in water. Core p.45
     */
    @Override
    public boolean isMoveIntoWaterDangerous(EntityMovementType movementType, EntityMovementMode movementMode) {
        if ((movementType == EntityMovementType.MOVE_RUN || movementType == EntityMovementType.MOVE_SPRINT) 
              && !cannotRunInWater(movementMode,false)) {
            return true;
        }
        return false;
    }

    /**
     * {@inheritDoc}
     * Core rules p.114
     */
    @Override
    public boolean dominoEffectMovementCriteria(final int direction, final Entity entity, final MovePath stepForward, final MovePath stepBackwards,
          final Entity violation) {
        if (isDominoMoveLegal(direction, violation, stepForward, true) || isDominoMoveLegal(direction, violation,
              stepBackwards, false)) {
            return true;
        }
        return false;
    }

    /**
     * {@inheritDoc}
     * 
     * Core rules p.114
     */
    @Override
    public boolean isDominoMoveLegal(final int direction, final Entity entity, final MovePath step,
          boolean forwards) {
        if (forwards) {
            if ((step.isMoveLegal() && direction != entity.getFacing())) {
                return true;
            }
        } else {
            if ((step.isMoveLegal() && direction != ((entity.getFacing()+3) % 6) && entity.moved == EntityMovementType.MOVE_WALK)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * {@inheritDoc}
     * No MP cost for domino displacement. Core p.114
     */
    @Override
    public boolean getDominoDisplacementCostsMP() { return false; }

    /**
     * {@inheritDoc}
     * Height is minus the height of the unit landed on, Core rules p.116
     */
    @Override
    public int getAccidentalFallElevation(final int fallElevation, final int hitHeight) { return fallElevation - hitHeight; }

    /**
     * {@inheritDoc}
     *
     * Clear hexes first, then randomly choose. Core p.116, 114
     */
    @Nullable
    @Override
    public Coords getAccidentalFallDisplacement(Game game, int entityId, Coords src, int direction,
          int range) {
        ArrayList<Coords> hexCoordsArray = new ArrayList<>();
        ArrayList<Coords> hexAlternateArray = new ArrayList<>();
        for (int checkDirection = 0; checkDirection < 6; checkDirection++) {
            Coords dest = src.translated(checkDirection, range);
            if (Compute.isValidDisplacement(game, entityId, src, dest) && (Compute.stackingViolation(game, game.getEntity(entityId), dest, null,
                  game.getEntity(entityId).climbMode(), false) == null)) {
                // No other units there and a valid displacement
                hexCoordsArray.add(dest);
            }
            if (Compute.isValidDisplacement(game, entityId, src, dest)
                  && (Compute.stackingViolation(game, game.getEntity(entityId), dest, null,
                  game.getEntity(entityId).climbMode(), false) != null)) {
                // Valid displacement, but other units present
                hexAlternateArray.add(dest);
            }
        }
        // If we have a hex without something in it, randomly pick one
        if (!hexCoordsArray.isEmpty()) {
            int randomHex = Compute.randomInt(hexCoordsArray.size());
            return hexCoordsArray.get(randomHex);
        }
        // I guess no valid hexes without units in them. Ok, pick one with something in it
        if (!hexAlternateArray.isEmpty()) {
            int randomHex = Compute.randomInt(hexAlternateArray.size());
            return hexAlternateArray.get(randomHex);
        }
        // Uh oh, no valid hexes to go into, this is bad
        return null;
    }

    /**
     * {@inheritDoc}
     *
     * Returns the higher of the current hex the attacker is in, or the defender hex
     * Core p.79
     */
    @Override
    public int getDFAElevation(Game game, int attackerId, int targetId, MoveStep step) {
        int elevation;

        int dfaTargetElevation = game.getEntity(targetId).getElevation();
        int dfaTargetHexElevation =
              game.getBoard(game.getEntity(attackerId).getBoardId()).getHex(game.getEntity(targetId).getPosition()).getLevel();
        dfaTargetElevation += dfaTargetHexElevation + game.getEntity(targetId).getHeight();
        // step includes +1 elevation for jumping
        int dfaEntityElevation = step.getElevation();
        int dfaEntityHexElevation =
              game.getBoard(game.getEntity(attackerId).getBoardId()).getHex(game.getEntity(attackerId).getPosition()).getLevel();
        dfaEntityElevation += dfaEntityHexElevation;

        if (dfaEntityElevation <= dfaTargetElevation) {
            // The target elevation total is higher than the attacker. Use the higher value, modified for terrain
            return (dfaTargetElevation - dfaEntityHexElevation);
        } else {
            // The attacking entity elevation is higher than the target needs to be
            return (dfaEntityElevation - dfaEntityHexElevation);
        }
    }
}
