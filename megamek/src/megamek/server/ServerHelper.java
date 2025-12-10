/*
 * Copyright (C) 2018-2025 The MegaMek Team. All Rights Reserved.
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

package megamek.server;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import megamek.common.Hex;
import megamek.common.LosEffects;
import megamek.common.Report;
import megamek.common.battleArmor.BattleArmor;
import megamek.common.board.Coords;
import megamek.common.compute.Compute;
import megamek.common.equipment.Minefield;
import megamek.common.equipment.MiscType;
import megamek.common.game.Game;
import megamek.common.options.OptionsConstants;
import megamek.common.rolls.Roll;
import megamek.common.rolls.TargetRoll;
import megamek.common.units.Entity;
import megamek.common.units.EntityMovementMode;
import megamek.common.units.Infantry;
import megamek.common.units.Mek;
import megamek.common.units.Terrain;
import megamek.common.units.Terrains;
import megamek.server.totalWarfare.TWGameManager;

/**
 * This class contains computations carried out by the Server class. Methods put in here should be static and
 * self-contained.
 *
 * @author NickAragua
 */
public class ServerHelper {
    private ServerHelper() {
    }

    /**
     * Determines if the given entity is an infantry unit in the given hex is "in the open" (and thus subject to double
     * damage from attacks)
     *
     * @param te                         Target entity.
     * @param te_hex                     Hex where target entity is located.
     * @param game                       The current {@link Game}
     * @param isPlatoon                  Whether the target unit is a platoon.
     * @param ammoExplosion              Whether we're considering a "big boom" ammo explosion from TacOps.
     * @param ignoreInfantryDoubleDamage Whether we should ignore double damage to infantry.
     *
     * @return Whether the infantry unit can be considered to be "in the open"
     */
    public static boolean infantryInOpen(Entity te, Hex te_hex, Game game,
          boolean isPlatoon, boolean ammoExplosion, boolean ignoreInfantryDoubleDamage) {
        if (isPlatoon && !te.isDestroyed() && !te.isDoomed() && !ignoreInfantryDoubleDamage
              && (((Infantry) te).getDugIn() != Infantry.DUG_IN_COMPLETE)) {

            if (te_hex == null) {
                te_hex = game.getBoard().getHex(te.getPosition());
            }

            return (te_hex != null)
                  && !te_hex.containsTerrain(Terrains.WOODS)
                  && !te_hex.containsTerrain(Terrains.JUNGLE)
                  && !te_hex.containsTerrain(Terrains.ROUGH)
                  && !te_hex.containsTerrain(Terrains.RUBBLE)
                  && !te_hex.containsTerrain(Terrains.SWAMP)
                  && !te_hex.containsTerrain(Terrains.BUILDING)
                  && !te_hex.containsTerrain(Terrains.FUEL_TANK)
                  && !te_hex.containsTerrain(Terrains.FORTIFIED)
                  && (!te.hasAbility(OptionsConstants.INFANTRY_URBAN_GUERRILLA))
                  && (!te_hex.containsTerrain(Terrains.PAVEMENT) || !te_hex.containsTerrain(Terrains.ROAD))
                  && !ammoExplosion;
        }

        return false;
    }

    /**
     * Helper function that causes an entity to sink to the bottom of the water hex it's currently in.
     */
    public static void sinkToBottom(Entity entity) {
        if ((entity == null) || !entity.getGame().getBoard().contains(entity.getPosition())) {
            return;
        }

        Hex fallHex = entity.getGame().getBoard().getHex(entity.getPosition());
        int waterDepth;

        // we're going hull down, we still sink to the bottom if appropriate
        if (fallHex.containsTerrain(Terrains.WATER)) {
            boolean hexHasBridge = fallHex.containsTerrain(Terrains.BRIDGE_CF);
            boolean entityOnTopOfBridge = hexHasBridge && (entity.getElevation() == fallHex.ceiling());

            if (!entityOnTopOfBridge) {
                // *Only* use this if there actually is water in the hex, otherwise
                // we get Terrain.LEVEL_NONE, i.e. Integer.minValue...
                waterDepth = fallHex.terrainLevel(Terrains.WATER);
                entity.setElevation(-waterDepth);
            }
        }
    }

    public static void checkAndApplyMagmaCrust(Hex hex, int elevation, Entity entity, Coords curPos,
          boolean jumpLanding, Vector<Report> vPhaseReport, TWGameManager gameManager) {
        if ((hex.terrainLevel(Terrains.MAGMA) == 1) && (elevation == 0)
              && (entity.getMovementMode() != EntityMovementMode.HOVER)) {
            int reportID = jumpLanding ? 2396 : 2395;

            Roll diceRoll = Compute.rollD6(1);
            Report r = new Report(reportID);
            r.addDesc(entity);
            r.add(diceRoll);
            r.subject = entity.getId();
            vPhaseReport.add(r);

            int rollTarget = jumpLanding ? 4 : 6;

            if (diceRoll.getIntValue() >= rollTarget) {
                hex.removeTerrain(Terrains.MAGMA);
                hex.addTerrain(new Terrain(Terrains.MAGMA, 2));
                gameManager.sendChangedHex(curPos);
                for (Entity en : entity.getGame().getEntitiesVector(curPos)) {
                    if (en != entity) {
                        gameManager.doMagmaDamage(en, false);
                    }
                }
            }
        }
    }

    /**
     * Check for movement into magma hex and apply damage.
     */
    public static void checkEnteringMagma(Hex hex, int elevation, Entity entity, TWGameManager gameManager) {

        if ((hex.terrainLevel(Terrains.MAGMA) == 2) && (elevation == 0)
              && (entity.getMovementMode() != EntityMovementMode.HOVER)) {
            gameManager.doMagmaDamage(entity, false);
        }
    }

    /**
     * Check for movement into hazardous liquid and apply damage.
     */
    public static void checkEnteringHazardousLiquid(Hex hex, int elevation, Entity entity, TWGameManager gameManager) {

        if (hex.containsTerrain(Terrains.HAZARDOUS_LIQUID) && (elevation <= 0)) {
            int depth = hex.containsTerrain(Terrains.WATER) ? hex.terrainLevel(Terrains.WATER) : 0;
            gameManager.doHazardousLiquidDamage(entity, false, depth);
        }
    }

    public static void checkEnteringUltraSublevel(Hex hex, int elevation, Entity entity, TWGameManager gameManager) {
        if (hex.containsTerrain(Terrains.ULTRA_SUBLEVEL) && (elevation <= 0)) {
            gameManager.doUltraSublevelDamage(entity);
        }
    }

    /**
     * Check for black ice when moving into pavement hex.
     */
    public static boolean checkEnteringBlackIce(TWGameManager gameManager, Coords curPos, Hex curHex,
          boolean useBlackIce, boolean goodTemp, boolean isIceStorm) {
        boolean isPavement = curHex.hasPavement();
        if (isPavement && ((useBlackIce && goodTemp) || isIceStorm)) {
            if (!curHex.containsTerrain(Terrains.BLACK_ICE)) {
                int blackIceChance = Compute.d6(1);
                if (blackIceChance > 4) {
                    curHex.addTerrain(new Terrain(Terrains.BLACK_ICE, 1));
                    gameManager.sendChangedHex(curPos);
                    return true;
                }
            } else {
                return true;
            }
        }
        return false;
    }

    /**
     * Loops through all active entities in the game and performs mine detection
     */
    public static void detectMinefields(Game game, Vector<Report> vPhaseReport, TWGameManager gameManager) {
        boolean tacOpsBap = game.getOptions().booleanOption(OptionsConstants.ADVANCED_TAC_OPS_BAP);

        // if the entity is on the board
        // and it either a) hasn't moved or b) we're not using TacOps BAP rules
        // if we are not using the TacOps BAP rules, that means we only check the
        // entity's final hex
        // if we are using TacOps BAP rules, all moved entities have made all their
        // checks already
        // so we just need to do the unmoved entities
        for (Entity entity : game.getEntitiesVector()) {
            if (!entity.isOffBoard() && entity.isDeployed() &&
                  ((entity.delta_distance == 0) || !tacOpsBap)) {
                detectMinefields(game, entity, entity.getPosition(), vPhaseReport, gameManager);
            }
        }
    }

    /**
     * Checks for minefields within the entity's active probe range.
     *
     * @return True if any minefields have been detected.
     */
    public static boolean detectMinefields(Game game, Entity entity, Coords coords,
          Vector<Report> vPhaseReport, TWGameManager gameManager) {
        if (!game.getOptions().booleanOption(OptionsConstants.ADVANCED_MINEFIELDS)) {
            return false;
        }

        // can't detect minefields if the coordinates are invalid
        if (coords == null) {
            return false;
        }

        // can't detect minefields if there aren't any to detect
        if (!game.getMinedCoords().hasMoreElements()) {
            return false;
        }

        // can't detect minefields if we have no probe
        int probeRange = entity.getBAPRange();
        if (probeRange <= 0) {
            return false;
        }

        boolean minefieldDetected = false;

        for (int distance = 1; distance <= probeRange; distance++) {
            for (Coords potentialMineCoords : coords.allAtDistance(distance)) {
                if (!game.getBoard().contains(potentialMineCoords)) {
                    continue;
                }

                for (Minefield minefield : game.getMinefields(potentialMineCoords)) {
                    // no need to roll for already revealed minefields
                    if (entity.getOwner().containsMinefield(minefield)) {
                        continue;
                    }

                    int roll = Compute.d6(2);

                    if (roll >= minefield.getBAPDetectionTarget()) {
                        minefieldDetected = true;

                        Report r = new Report(2163);
                        r.subject = entity.getId();
                        r.add(entity.getShortName(), true);
                        r.add(potentialMineCoords.toFriendlyString());
                        vPhaseReport.add(r);

                        gameManager.revealMinefield(entity.getOwner(), minefield);
                    }
                }
            }
        }

        return minefieldDetected;
    }

    /**
     * Checks to see if any units can detected hidden units.
     */
    public static boolean detectHiddenUnits(Game game, Entity detector, Coords detectorCoords,
          Vector<Report> vPhaseReport, TWGameManager gameManager) {
        // If hidden units aren't on, nothing to do
        if (!game.getOptions().booleanOption(OptionsConstants.ADVANCED_HIDDEN_UNITS)) {
            return false;
        }

        // Units without a position won't be able to detect
        // check for this before calculating BAP range, as that's expensive
        if ((detector.getPosition() == null) || (detectorCoords == null)) {
            return false;
        }

        int probeRange = detector.getBAPRange();
        boolean isAero = detector.isAero();

        // if no probe, save ourselves a few loops
        if (probeRange < 0) {
            return false;
        }

        if (isAero && game.isOnGroundMap(detector)) {
            // Aerospace with BAP on the ground map detect hidden units to 1 hex on either
            // side of their flight path; see https://bg.battletech.com/forums/index.php?topic=84054.0
            // Otherwise, range is 0 (fly-over only)
            probeRange = (probeRange > 0) ? 1 : 0;
        }

        // Get all hidden units in probe range
        List<Entity> hiddenUnits = new ArrayList<>();
        for (Coords coords : detectorCoords.allAtDistanceOrLess(probeRange)) {
            for (Entity entity : game.getEntitiesVector(coords, detector.getBoardId(), true)) {
                if (entity.isHidden() && entity.isEnemyOf(detector)) {
                    hiddenUnits.add(entity);
                }
            }
        }

        // If no one is hidden, there's nothing to do
        if (hiddenUnits.isEmpty()) {
            return false;
        }

        Set<Integer> reportPlayers = new HashSet<>();

        boolean detectorHasBloodhound = detector.hasWorkingMisc(MiscType.F_BLOODHOUND);
        boolean hiddenUnitFound = false;

        for (Entity detected : hiddenUnits) {
            // Can only detect units within the probes range
            int dist = detector.getPosition().distance(detected.getPosition());
            boolean beyondPointBlankRange = dist > 1;

            // Check for Void/Null Sig - only detected by Bloodhound probes
            if (beyondPointBlankRange && (detected instanceof Mek m)) {
                if ((m.isVoidSigActive() || m.isNullSigActive()) && !detectorHasBloodhound) {
                    continue;
                }
            }

            // Check for Infantry stealth armor
            if (beyondPointBlankRange && (detected instanceof BattleArmor ba)) {
                // Need Bloodhound to detect BA stealth armor
                if (ba.isStealthy() && !detectorHasBloodhound) {
                    continue;
                }
            } else if (beyondPointBlankRange && (detected instanceof Infantry inf)) {
                // Can't detect sneaky infantry
                if (inf.isStealthy()) {
                    continue;
                }
                // Need bloodhound to detect non-sneaky inf
                if (!detectorHasBloodhound) {
                    continue;
                }
            }

            LosEffects los = LosEffects.calculateLOS(game, detector, detected, detectorCoords, detected.getPosition(),
                  false);
            if (los.canSee() || !beyondPointBlankRange) {
                detected.setHidden(false);
                gameManager.entityUpdate(detected.getId());
                Report r = new Report(9960);
                r.addDesc(detector);
                r.subject = detector.getId();
                r.add(detected.getPosition().getBoardNum());
                vPhaseReport.addElement(r);
                Report.addNewline(vPhaseReport);
                reportPlayers.add(detector.getOwnerId());
                reportPlayers.add(detected.getOwnerId());

                hiddenUnitFound = true;
            }
        }

        if (!vPhaseReport.isEmpty() && game.getPhase().isMovement()
              && ((game.getTurnIndex() + 1) < game.getTurnsList().size())) {
            for (Integer playerId : reportPlayers) {
                gameManager.send(playerId, gameManager.createSpecialReportPacket());
            }
        }

        return hiddenUnitFound;
    }

    /**
     * Loop through the game and clear 'blood stalker' flag for any entities that have the given unit as the blood
     * stalker target.
     */
    public static void clearBloodStalkers(Game game, int stalkeeID, TWGameManager gameManager) {
        for (Entity entity : game.getEntitiesVector()) {
            if (entity.getBloodStalkerTarget() == stalkeeID) {
                entity.setBloodStalkerTarget(Entity.BLOOD_STALKER_TARGET_CLEARED);
                gameManager.entityUpdate(entity.getId());
            }
        }
    }

    /**
     * Returns the target number to avoid Radical Heat Sink Failure for the given number of rounds of consecutive use,
     * IO p.89. The first round of use means consecutiveRounds = 1; this is the minimum as 0 rounds of use would not
     * trigger a roll.
     *
     * @param consecutiveRounds The rounds the RHS has been used
     *
     * @return The roll target number to avoid failure
     */
    public static int radicalHeatSinkSuccessTarget(int consecutiveRounds) {
        return switch (consecutiveRounds) {
            case 1 -> 3;
            case 2 -> 5;
            case 3 -> 7;
            case 4 -> 10;
            case 5 -> 11;
            default -> TargetRoll.AUTOMATIC_FAIL;
        };
    }
}
