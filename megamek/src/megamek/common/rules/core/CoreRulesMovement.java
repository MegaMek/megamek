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


import java.util.ArrayList;
import java.util.List;

import jakarta.annotation.Nullable;
import megamek.common.board.Board;
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

public class CoreRulesMovement extends RulesMovement {
    /**
     * {@inheritDoc}
     * Core gives Meks no skid on pavement (Core p.122) or ice (Core p.124), and terrain never forces Assets to make
     * Piloting Skill Rolls (Core p.142). Other units, such as combat vehicles, are not in Core and keep the Total
     * Warfare skid (TW p.62).
     */
    @Override
    public boolean skidEnabled(Entity entity) {
        return !(entity instanceof Mek) && !entity.isBattlefieldSupportAsset();
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
     * A walking Mek can make a backward level change (Core p.46), unless hip damage or a destroyed leg prevents it
     * (Core pp.47, 90, 238). "An Asset cannot change levels on a turn that it moves backward" (Core p.141). Other units
     * are not in Core and keep the Tactical Operations option (TO:AR p.20).
     */
    @Override
    public boolean enableBackwardsElevationChange(final boolean toBackwardsElevation, Entity entity) {
        if (entity instanceof Mek mek) {
            return !CoreRulesUnits.hasHipOrLegLevelRestriction(mek);
        }
        if (entity.isBattlefieldSupportAsset()) {
            return false;
        }
        return toBackwardsElevation;
    }

    /**
     * {@inheritDoc}
     * Do we add leg damage together, yes. Core p.99
     */
    @Override
    public boolean cumulativeLegDamage(boolean bTOLegDamage) {
        return true;
    }

    /**
     * {@inheritDoc}
     * A Mek with hip damage or a destroyed leg can only change one level per hex moving forward (Core pp.47, 90, 238).
     */
    @Override
    public boolean reduceMaxElevation(Mek mek) {
        return CoreRulesUnits.hasHipOrLegLevelRestriction(mek);
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
     * The target Mek dodges "one hex directly forward or backward", but the hex the displacing Mek is coming from "is
     * presumed to be occupied", and it "cannot move backward if it ran that turn" (Core p.114).
     */
    @Override
    public boolean isDominoMoveLegal(final int direction, final Entity entity, final MovePath step,
          boolean forwards) {
        // The intruder comes from the hex opposite its direction of travel
        int towardsIntruder = (direction + 3) % 6;
        if (forwards) {
            return step.isMoveLegal() && (entity.getFacing() != towardsIntruder);
        }
        boolean ran = (entity.moved == EntityMovementType.MOVE_RUN) || (entity.moved == EntityMovementType.MOVE_SPRINT);
        return step.isMoveLegal() && (((entity.getFacing() + 3) % 6) != towardsIntruder) && !ran;
    }

    /**
     * {@inheritDoc}
     * No MP cost for domino displacement. Core p.114
     */
    @Override
    public boolean getDominoDisplacementCostsMP() { return false; }

    /**
     * {@inheritDoc}
     * The damage multiplier is "the number of levels fallen, minus the target's height; minimum result of 1" (Core
     * p.116). A standing Mek is 2 levels tall and a prone one 1 (Core p.57), one more than MegaMek's height.
     */
    @Override
    public int getAccidentalFallElevation(final int fallElevation, final int hitHeight) {
        return Math.max(1, fallElevation - (hitHeight + 1));
    }

    /**
     * {@inheritDoc}
     * A faller that misses "lands in an adjacent hex as close as possible to the hex from which it fell"; the unit
     * fallen on is "displaced to a random neighboring legal hex". Ties are random, "but empty hexes must be prioritized
     * over occupied hexes" (Core p.116).
     */
    @Nullable
    @Override
    public Coords getAccidentalFallDisplacement(Game game, int entityId, Coords src, int direction,
          int range, boolean fallerMissed) {
        Entity entity = game.getEntity(entityId);
        if (entity == null) {
            return null;
        }
        Coords fellFrom = src.translated((direction + 3) % 6, range);
        List<Coords> legalHexes = new ArrayList<>();
        for (int checkDirection = 0; checkDirection < 6; checkDirection++) {
            Coords dest = src.translated(checkDirection, range);
            if (Compute.isValidDisplacement(game, entityId, src, dest) && !(fallerMissed && dest.equals(fellFrom))) {
                legalHexes.add(dest);
            }
        }
        List<Coords> candidates = legalHexes;
        if (fallerMissed && !legalHexes.isEmpty()) {
            int closest = legalHexes.stream().mapToInt(hex -> hex.distance(fellFrom)).min().getAsInt();
            legalHexes.removeIf(hex -> hex.distance(fellFrom) > closest);
            List<Coords> emptyHexes = legalHexes.stream()
                  .filter(hex -> game.getEntitiesVector(hex, entity.getBoardId()).isEmpty())
                  .toList();
            candidates = emptyHexes.isEmpty() ? legalHexes : emptyHexes;
        }
        return candidates.isEmpty() ? null : candidates.get(Compute.randomInt(candidates.size()));
    }

    /**
     * {@inheritDoc}
     * Until the DFA resolves, "the attacker's level is the level of the target's hex or the attacker's hex (whichever is
     * higher), plus the target's unit height", 2 levels for a standing Mek and 1 for a prone one (Core pp.57, 79), one
     * more than MegaMek's height. Without a target the attacker lands as an ordinary jump (Core p.80).
     */
    @Override
    public int getDFAElevation(Game game, int attackerId, int targetId, MoveStep step, Coords attackHex) {
        Entity attacker = game.getEntity(attackerId);
        Entity target = game.getEntity(targetId);
        if ((attacker == null) || (target == null)) {
            return step.getElevation();
        }
        Board board = game.getBoard(attacker.getBoardId());
        int attackerHexLevel = board.getHex(attackHex).getLevel();
        // A target standing on a roof or bridge stands at that level
        int targetHexLevel = board.getHex(target.getPosition()).getLevel() + Math.max(0, target.getElevation());
        int attackerLevel = Math.max(attackerHexLevel, targetHexLevel) + target.getHeight() + 1;
        return attackerLevel - attackerHexLevel;
    }

    /**
     * Core rules max mp reduction by shield is to 1 movement.
     *
     * @param reduceMPbyShield what the shield reduction is
     * @param baseMP   what the original walk mp was
     *
     * @return the reduction amount.
     */
    @Override
    public int reduceMovementWithShields(int reduceMPbyShield, int baseMP) {
        return Math.min(reduceMPbyShield, Math.max(0, baseMP - 1));
    }
}
