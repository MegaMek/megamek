/*

 * Copyright (C) 2000-2005 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2014-2025 The MegaMek Team. All Rights Reserved.
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

package megamek.common.compute;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;

import megamek.common.ECCMComparator;
import megamek.common.ECMComparator;
import megamek.common.ECMInfo;
import megamek.common.annotations.Nullable;
import megamek.common.battleArmor.BattleArmor;
import megamek.common.board.Coords;
import megamek.common.equipment.INarcPod;
import megamek.common.equipment.MiscMounted;
import megamek.common.equipment.MiscType;
import megamek.common.game.Game;
import megamek.common.options.OptionsConstants;
import megamek.common.units.Entity;
import megamek.server.SmokeCloud;

/**
 * Similar to the Compute class, this class contains various static methods for common computations related to ECM.
 *
 * @author arlith
 */
public class ComputeECM {

    /**
     * This method checks to see if a line from a to b is affected by any ECM field (including Angel) of the enemy of
     * attackingEntity
     *
     */
    public static boolean isAffectedByECM(Entity ae, Coords a, Coords b) {
        return isAffectedByECM(ae, a, b, null);
    }

    /**
     * This method checks to see if a line from a to b is affected by any ECM field (including Angel) of the enemy of
     * attackingEntity
     *
     * @param allECMInfo A collection of ECMInfo for each Entity in the Game.
     *
     */
    public static boolean isAffectedByECM(Entity ae, Coords a, Coords b,
          List<ECMInfo> allECMInfo) {
        ECMInfo ecmInfo = getECMEffects(ae, a, b, true, allECMInfo);
        return (ecmInfo != null) && ecmInfo.isECM();
    }

    /**
     * This method checks to see if a line from a to b is affected by an ECCM field of the enemy of attackingEntity.
     *
     */
    public static boolean isAffectedByECCM(Entity ae, Coords a, Coords b) {
        ECMInfo ecmInfo = getECMEffects(ae, a, b, false, null);
        return (ecmInfo != null) && ecmInfo.isECCM();
    }

    /**
     * This method checks to see if a line from a to b is affected by an Angel ECM field of the enemy of attackingEntity
     * (ignoring other kinds of ECM).
     *
     * @return shows if you are in a friendly ECCM field positive number means you are in a friendly ECCM field Negative
     *       number means you are in an enemy ECM field 0 means you are not affect by enemy or friendly fields.
     */
    public static boolean isAffectedByAngelECM(Entity ae, Coords a, Coords b) {
        return isAffectedByAngelECM(ae, a, b, null);
    }

    /**
     * This method checks to see if a line from a to b is affected by an Angel ECM field of the enemy of attackingEntity
     * (ignoring other kinds of ECM).
     *
     * @param allECMInfo A collection of ECMInfo for each Entity in the Game.
     *
     * @return shows if you are in a friendly ECCM field positive number means you are in a friendly ECCM field Negative
     *       number means you are in an enemy ECM field 0 means you are not affect by enemy or friendly fields.
     */
    public static boolean isAffectedByAngelECM(Entity ae, Coords a, Coords b,
          List<ECMInfo> allECMInfo) {
        ECMInfo ecmInfo = getECMEffects(ae, a, b, true, allECMInfo);
        return (ecmInfo != null) && ecmInfo.isAngelECM();
    }


    /**
     * Check for the total number of fighter/small craft ECM bubbles in space along the path from a to b
     */
    public static int getSmallCraftECM(Entity ae, Coords a, Coords b) {
        if (!ae.isSpaceborne()) {
            // only matters in space
            return 0;
        }
        // Only grab enemies with active ECM
        Vector<Coords> vEnemyECMCoords = new Vector<>(16);
        Vector<Integer> vEnemyECMRanges = new Vector<>(16);
        Vector<Coords> vFriendlyECCMCoords = new Vector<>(16);
        Vector<Integer> vFriendlyECCMRanges = new Vector<>(16);
        Vector<Coords> vFriendlyBAPCoords = new Vector<>(16);
        Vector<Integer> vFriendlyBAPRanges = new Vector<>(16);
        Vector<Integer> vFriendlyBAPFacings = new Vector<>(16);
        for (Entity ent : ae.getGame().getEntitiesVector()) {
            Coords entPos = ent.getPosition();
            if ((entPos == null) && (ent.getTransportId() != Entity.NONE)) {
                Entity carrier = ae.getGame().getEntity(ent.getTransportId());
                if ((null != carrier) && carrier.loadedUnitsHaveActiveECM()) {
                    entPos = carrier.getPosition();
                }
            }
            if (ent.isEnemyOf(ae) && ent.hasActiveECM() && (entPos != null)
                  && !ent.isLargeCraft()) {
                vEnemyECMCoords.addElement(entPos);
                vEnemyECMRanges.addElement(ent.getECMRange());
            }
            if (!ent.isEnemyOf(ae) && ent.hasActiveECCM() && (entPos != null)
                  && !ent.isLargeCraft()) {
                vFriendlyECCMCoords.addElement(entPos);
                vFriendlyECCMRanges.addElement(ent.getECMRange());
            }
            if (!ent.isEnemyOf(ae) && ent.hasBAP(false) && (entPos != null)) {
                vFriendlyBAPCoords.addElement(entPos);
                vFriendlyBAPRanges.addElement(ent.getBAPRange());
                vFriendlyBAPFacings.addElement(ent.getFacing());
            }

            // TODO: do docked DropShips give ECM benefit?
        }

        // none? get out of here
        if (vEnemyECMCoords.isEmpty()) {
            return 0;
        }

        // get intervening Coords.
        ArrayList<Coords> coords = Coords.intervening(a, b);
        // loop through all intervening coords, check each if they are ECM affected
        int totalECM = 0;
        // check for split hexes
        boolean bDivided = ((a.degree(b) % 60) == 30);
        int x = 0;
        int prevEcmStatus = 0;
        boolean prevEccmPresent = false;
        for (Coords c : coords) {
            int ecmStatus = 0;
            boolean eccmPresent = false;
            // first, subtract 1 for each enemy ECM that affects us
            Enumeration<Integer> ranges = vEnemyECMRanges.elements();
            for (Coords enemyECMCoords : vEnemyECMCoords) {
                int range = ranges.nextElement();
                int nDist = c.distance(enemyECMCoords);
                if (nDist <= range) {
                    ecmStatus++;
                }
            }
            // now check for friendly eccm
            ranges = vFriendlyECCMRanges.elements();
            for (Coords friendlyECCMCoords : vFriendlyECCMCoords) {
                int range = ranges.nextElement();
                int nDist = c.distance(friendlyECCMCoords);
                if (nDist <= range) {
                    eccmPresent = true;
                    break;
                }
            }
            // if eccm still not present, check for BAP
            if (!eccmPresent) {
                ranges = vFriendlyBAPRanges.elements();
                Enumeration<Integer> facings = vFriendlyBAPFacings.elements();
                for (Coords friendlyBAPCoords : vFriendlyBAPCoords) {
                    int range = ranges.nextElement();
                    int nDist = c.distance(friendlyBAPCoords);
                    int facing = facings.nextElement();
                    if (nDist <= range) {
                        // still might need to check for right arc if using medium range
                        if ((range < 7)
                              || ComputeArc.isInArc(friendlyBAPCoords, facing, c, Compute.ARC_NOSE)) {
                            eccmPresent = true;
                            break;
                        }
                    }
                }
            }
            // if any coords in the line are affected, the whole line is
            if (!bDivided || ((x % 3) == 0)) {
                if ((ecmStatus > 0) && !eccmPresent) {
                    totalECM++;
                }
            } else if (((x % 3) == 2)) {
                // if we are looking at the second split hex then both this one
                // and the prior need to have ECM
                // because the advantage should go to the defender
                if ((ecmStatus > 0) && !eccmPresent && (prevEcmStatus > 0)
                      && !prevEccmPresent) {
                    totalECM++;
                }
            }
            x++;
            prevEccmPresent = eccmPresent;
            prevEcmStatus = ecmStatus;

        }
        return totalECM;
    }

    /**
     * Check for the total number of fighter/small craft ECM bubbles in space along the path from a to b
     */
    public static int getLargeCraftECM(Entity ae, Coords a, Coords b) {
        if (!ae.isSpaceborne()) {
            // only matters in space
            return 0;
        }
        // Only grab enemies with active ECM
        Vector<Coords> vEnemyECMCoords = new Vector<>(16);
        Vector<Integer> vEnemyECMRanges = new Vector<>(16);
        Vector<Coords> vFriendlyECCMCoords = new Vector<>(16);
        Vector<Integer> vFriendlyECCMRanges = new Vector<>(16);
        Vector<Coords> vFriendlyBAPCoords = new Vector<>(16);
        Vector<Integer> vFriendlyBAPRanges = new Vector<>(16);
        Vector<Integer> vFriendlyBAPFacings = new Vector<>(16);
        for (Entity ent : ae.getGame().getEntitiesVector()) {
            Coords entPos = ent.getPosition();
            if ((entPos == null) && (ent.getTransportId() != Entity.NONE)) {
                Entity carrier = ae.getGame().getEntity(ent.getTransportId());
                if ((null != carrier) && carrier.loadedUnitsHaveActiveECM()) {
                    entPos = carrier.getPosition();
                }
            }
            if (ent.isEnemyOf(ae) && ent.hasActiveECM() && (entPos != null)
                  && ent.isLargeCraft()) {
                vEnemyECMCoords.addElement(entPos);
                vEnemyECMRanges.addElement(ent.getECMRange());
            }
            if (!ent.isEnemyOf(ae) && ent.hasActiveECCM() && (entPos != null)
                  && !ent.isLargeCraft()) {
                vFriendlyECCMCoords.addElement(entPos);
                vFriendlyECCMRanges.addElement(ent.getECMRange());
            }
            if (!ent.isEnemyOf(ae) && ent.hasBAP(false) && (entPos != null)) {
                vFriendlyBAPCoords.addElement(entPos);
                vFriendlyBAPRanges.addElement(ent.getBAPRange());
                vFriendlyBAPFacings.addElement(ent.getFacing());

            }
            // TODO: do docked DropShips give ECM benefit?
        }

        // none? get out of here
        if (vEnemyECMCoords.isEmpty()) {
            return 0;
        }

        // get intervening Coords.
        ArrayList<Coords> coords = Coords.intervening(a, b);
        // loop through all intervening coords, check each if they are ECM
        // affected
        int totalECM = 0;
        boolean bDivided = ((a.degree(b) % 60) == 30);
        int x = 0;
        int prevEcmStatus = 0;
        for (Coords c : coords) {
            // > 0: in friendly ECCM
            // 0: unaffected by enemy ECM
            // <0: affected by enemy ECM
            int ecmStatus = 0;
            // first, subtract 1 for each enemy ECM that affects us
            Enumeration<Integer> ranges = vEnemyECMRanges.elements();
            for (Coords enemyECMCoords : vEnemyECMCoords) {
                int range = ranges.nextElement();
                int nDist = c.distance(enemyECMCoords);
                if (nDist <= range) {
                    ecmStatus++;
                }
            }
            // now check for friendly small craft eccm
            ranges = vFriendlyECCMRanges.elements();
            for (Coords friendlyECCMCoords : vFriendlyECCMCoords) {
                int range = ranges.nextElement();
                int nDist = c.distance(friendlyECCMCoords);
                if (nDist <= range) {
                    ecmStatus--;
                }
            }
            // now check BAP
            ranges = vFriendlyBAPRanges.elements();
            Enumeration<Integer> facings = vFriendlyBAPFacings.elements();
            for (Coords friendlyBAPCoords : vFriendlyBAPCoords) {
                int range = ranges.nextElement();
                int nDist = c.distance(friendlyBAPCoords);
                int facing = facings.nextElement();
                if (nDist <= range) {
                    // still might need to check for right arc if using medium range
                    if ((range < 7)
                          || ComputeArc.isInArc(friendlyBAPCoords, facing, c,
                          Compute.ARC_NOSE)) {
                        ecmStatus = ecmStatus - 2;
                    }
                }
            }
            // if any coords in the line are affected, the whole line is
            if (!bDivided || ((x % 3) == 0)) {
                if (ecmStatus > 0) {
                    totalECM++;
                }
            } else if ((x % 3) == 2) {
                // if we are looking at the second split hex then both this one
                // and the prior need to have ECM
                // because the advantage should go to the defender
                if ((ecmStatus > 0) && (prevEcmStatus > 0)) {
                    totalECM++;
                }
            }
            x++;
            prevEcmStatus = ecmStatus;
        }
        return totalECM;
    }

    /**
     * Go through each entity in the supplied list and calculate the information for any ECM and ECCM it has and return
     * the collection of ECMInfos.
     *
     * @param entities The list of entities to compute information for
     *
     * @return An ECMInfo entry for each active ECM and ECCM fielded.
     */
    public static ArrayList<ECMInfo> computeAllEntitiesECMInfo(
          List<Entity> entities) {
        Comparator<ECMInfo> ecmComparator;
        ecmComparator = new ECCMComparator();

        ArrayList<ECMInfo> allEcmInfo = new ArrayList<>(entities.size());
        ArrayList<ECMInfo> allEccmInfo = new ArrayList<>(entities.size());
        // ECCM that counter an ECM need to get removed from allEcmInfo later
        LinkedList<ECMInfo> eccmToRemove = new LinkedList<>();

        Game game = null;
        for (Entity e : entities) {
            ECMInfo ecmInfo = e.getECMInfo();
            if (ecmInfo != null) {
                allEcmInfo.add(ecmInfo);
            }
            ECMInfo eccmInfo = e.getECCMInfo();
            if (eccmInfo != null) {
                allEcmInfo.add(eccmInfo);
                allEccmInfo.add(eccmInfo);
            }
            if (game == null) {
                game = e.getGame();
            }
        }

        // If either case is true, the rest is meaningless
        if (entities.isEmpty() || (game == null)) {
            return allEcmInfo;
        }

        // Add ECMInfo for chaff
        for (SmokeCloud cloud : game.getSmokeCloudList()) {
            if (cloud.getSmokeLevel() == SmokeCloud.SMOKE_CHAFF_LIGHT) {
                for (Coords c : cloud.getCoordsList()) {
                    ECMInfo ecmInfo = new ECMInfo(1, c, null, 1, 0);
                    allEcmInfo.add(ecmInfo);
                }
            }
        }

        // Sort the ECM, as we need to take care of the stronger ECM/ECCM first
        // ie; Angel ECCM can counter any number of ECM, however if an angel
        //  ECM counters it first...
        allEcmInfo.sort(ecmComparator);
        Collections.reverse(allEcmInfo);

        // If ECCM is on, we may have to remove some ECM that is negated
        if (game.getOptions().booleanOption(OptionsConstants.ADVANCED_TAC_OPS_ECCM)
              && !allEccmInfo.isEmpty()) {
            Iterator<ECMInfo> ecmIterator = allEcmInfo.iterator();
            Iterator<ECMInfo> eccmIterator;
            while (ecmIterator.hasNext()) {
                ECMInfo ecmInfo = ecmIterator.next();
                // Ignore ECCM
                if (ecmInfo.isECCM()) {
                    continue;
                }
                eccmIterator = allEccmInfo.iterator();
                boolean ecmNegated = false;
                // ECCM that covers source of an ECM field, negates the field
                while (eccmIterator.hasNext() && !ecmNegated) {
                    ECMInfo eccmInfo = eccmIterator.next();
                    // ECCM only effects enemy ECM
                    if (!eccmInfo.isOpposed(ecmInfo)) {
                        continue;
                    }
                    int dist = eccmInfo.getPos().distance(ecmInfo.getPos());
                    // Is the origin of the ECM within the ECCM's range?
                    if (dist <= eccmInfo.getRange()) {
                        // Angel ECCM vs non-Angel ECM
                        if (!ecmInfo.isAngelECM() && eccmInfo.isAngelECCM()) {
                            // Remove ECM, but ECCM is unaffected
                            ecmIterator.remove();
                            ecmNegated = true;
                            // Angel vs Angel
                        } else if (eccmInfo.getAngelECCMStrength() >= ecmInfo.getAngelECMStrength()) {
                            // Remove the ECM and ECCM
                            ecmIterator.remove();
                            eccmIterator.remove();
                            ecmNegated = true;
                            // Keep track of this eccm to remove it again later
                            eccmToRemove.add(eccmInfo);
                        } else if (!ecmInfo.isAngelECM()
                              && (eccmInfo.getECCMStrength() >= ecmInfo.getECMStrength())) {
                            // Remove the ECM and ECCM
                            ecmIterator.remove();
                            eccmIterator.remove();
                            ecmNegated = true;
                            // Keep track of this eccm to remove it again later
                            eccmToRemove.add(eccmInfo);
                        }
                    }
                }
            }
            allEcmInfo.removeAll(eccmToRemove);
        }

        return allEcmInfo;
    }

    /**
     * Returns the total ECM effects on the supplied unit.
     *
     */
    public static @Nullable ECMInfo getECMEffects(Entity ae, @Nullable Coords a, @Nullable Coords b,
          boolean compareECM,
          @Nullable List<ECMInfo> allEcmInfo) {
        Comparator<ECMInfo> ecmComparator;
        if (compareECM) {
            ecmComparator = new ECMComparator();
        } else {
            ecmComparator = new ECCMComparator();
        }

        if (ae.isSpaceborne()) {
            // normal ECM effects don't apply in space
            return null;
        }
        if ((a == null) || (b == null)) {
            return null;
        }

        if (allEcmInfo == null) {
            allEcmInfo = computeAllEntitiesECMInfo(ae.getGame().getEntitiesVector());
        }

        // Get intervening Coords
        ArrayList<Coords> coords = Coords.intervening(a, b);

        // PLAYTEST3 only if the two coordinates are affected by ECM, not intervening
        if (ae.getGame().getOptions().booleanOption(OptionsConstants.PLAYTEST_3)) {
            coords.clear();
            coords.add(a);
            coords.add(b);
        }
        
        ECMInfo worstECMEffects = null;
        // Loop through intervening coords, and find the worst effects
        for (Coords c : coords) {
            ECMInfo affectedInfo = null;
            if (c.equals(ae.getPosition()) && ae.isINarcedWith(INarcPod.ECM)) {
                affectedInfo = new ECMInfo(0, 1, ae.getOwner(), c);
            }
            for (ECMInfo ecmInfo : allEcmInfo) {
                // Is the ECMInfo in range of this position?
                int dist = c.distance(ecmInfo.getPos());
                if (dist <= ecmInfo.getRange()) {
                    if (affectedInfo == null) {
                        affectedInfo = new ECMInfo(0, 0, ae.getOwner(), c);
                    }
                    affectedInfo.addOpposingECMEffects(ecmInfo);
                }
            }
            if ((worstECMEffects == null && affectedInfo != null)
                  || (affectedInfo != null && ecmComparator.compare(
                  affectedInfo, worstECMEffects) > 0)) {
                worstECMEffects = affectedInfo;
            }
        }
        return worstECMEffects;
    }

    /**
     * Returns the total friendly ECM effects on the supplied unit.
     *
     * @param affectedEntity The entity to check.
     */
    public static @Nullable ECMInfo getFriendlyECMEffects(Entity affectedEntity, @Nullable List<ECMInfo> allEcmInfo) {
        if (allEcmInfo == null) {
            allEcmInfo = computeAllEntitiesECMInfo(affectedEntity.getGame().getEntitiesVector());
        }

        Coords entityPosition = affectedEntity.getPosition();

        ECMInfo affectedInfo = new ECMInfo(0, 0, affectedEntity.getOwner(), entityPosition);
        for (ECMInfo ecmInfo : allEcmInfo) {
            // Is the ECMInfo in range of this position?
            int dist = entityPosition.distance(ecmInfo.getPos());
            if (dist <= ecmInfo.getRange()) {
                affectedInfo.addAlliedECMEffects(ecmInfo);
            }
        }
        return affectedInfo;
    }

    /**
     * @return information (range, location, strength) about ECM if the unit has active ECM or null if it doesn't. In
     *       the case of multiple ECCM system, the best one takes precedence, as a unit can only have one active ECCM at
     *       a time.
     */
    @Nullable
    public static ECMInfo getECMInfo(@Nullable Entity entity) {
        if (entity == null || entity.getGame() == null) {
            return null;
        }
        Game game = entity.getGame();
        if ((game == null) || !game.hasBoardLocation(entity.getBoardLocation()) || entity.isShutDown()
              || entity.isStealthOn() || entity.isTransported()) {
            return null;
        }

        // E(C)CM operates differently in space (SO pg 110)
        if (entity.isSpaceborne()) {
            // No ECM in space unless SO rule is on
            if (!game.getOptions().booleanOption(OptionsConstants.ADVANCED_AERO_RULES_STRATOPS_ECM)) {
                return null;
            }
            int range = entity.getECMRange();
            if ((range >= 0) && entity.hasActiveECM()) {
                return new ECMInfo(range, 1, entity);
            } else {
                return null;
            }
        }

        // ASF ECM only has an effect if the unit is NOE
        if (entity.isAirborne() && !entity.isNOE()) {
            return null;
        }

        ECMInfo bestInfo = null;
        Comparator<ECMInfo> ecmComparator;
        ecmComparator = new ECCMComparator();
        for (MiscMounted m : entity.getMisc()) {
            // Ignore if inoperable
            if (m.isInoperable()) {
                continue;
            }
            ECMInfo newInfo = null;
            // Angel ECM
            if (m.getType().hasFlag(MiscType.F_ANGEL_ECM)) {
                if (m.curMode().equals("ECM")) {
                    newInfo = new ECMInfo(6, 0, entity);
                    newInfo.setAngelECMStrength(1);
                } else if (m.curMode().equals("ECM & ECCM") || m.curMode().equals("ECM & Ghost Targets")) {
                    // In dual mode, Angel ECM operates as two standard ECM suites,
                    // losing Angel abilities but gaining simultaneous ECM+ECCM (TO p.100)
                    newInfo = new ECMInfo(6, 1, entity);
                }
                // BA Angel ECM has a shorter range
                if ((newInfo != null) && (entity instanceof BattleArmor)) {
                    newInfo.setRange(2);
                }
                // Anything that's not Angel ECM
            } else if (m.getType().hasFlag(MiscType.F_ECM) && m.curMode().equals("ECM")) {
                int range = 6;
                if (m.getType().hasFlag(MiscType.F_SINGLE_HEX_ECM)) {
                    range = 0;
                } else if (m.getType().hasFlag(MiscType.F_EW_EQUIPMENT) ||
                      m.getType().hasFlag(MiscType.F_NOVA) ||
                      m.getType().hasFlag(MiscType.F_WATCHDOG)) {
                    range = 3;
                }
                newInfo = new ECMInfo(range, 1, entity);
                newInfo.setECMNova(m.getType().hasFlag(MiscType.F_NOVA));
            }
            // In some type of ECM mode...
            if (newInfo != null) {
                if ((bestInfo == null) || (ecmComparator.compare(newInfo, bestInfo) > 0)) {
                    bestInfo = newInfo;
                }
            }
        }
        return bestInfo;
    }

    /**
     * @return information (range, location, strength) about ECCM if the unit has active ECCM or null if it doesn't. In
     *       the case of multiple ECCM system, the best one takes precedence, as a unit can only have one active ECCM at
     *       a time.
     */
    @Nullable
    public static ECMInfo getECCMInfo(@Nullable Entity entity) {
        if (entity == null || entity.getGame() == null) {
            return null;
        }
        Game game = entity.getGame();
        if ((game == null) || !game.hasBoardLocation(entity.getBoardLocation()) || entity.isShutDown()
              || entity.isStealthOn() || entity.isTransported()) {
            return null;
        }
        // E(C)CM operates differently in space (SO pg 110)
        if (entity.isSpaceborne()) {
            // No ECCM in space unless SO rule is on
            if (!game.getOptions().booleanOption(OptionsConstants.ADVANCED_AERO_RULES_STRATOPS_ECM)) {
                return null;
            }
            int bapRange = entity.getBAPRange();
            int range = entity.getECMRange();
            ECMInfo eccmInfo = new ECMInfo(0, 0, entity);
            eccmInfo.setECCMStrength(1);
            if (bapRange > 0) {
                eccmInfo.setRange(bapRange);
                // Medium range band only effects the nose, so set direction
                if (bapRange > 6) {
                    eccmInfo.setDirection(entity.getFacing());
                }
            } else if ((range >= 0) && entity.hasActiveECCM()) {
                eccmInfo.setRange(range);
            } else {
                eccmInfo = null;
            }
            return eccmInfo;
        }

        ECMInfo bestInfo = null;
        Comparator<ECMInfo> ecmComparator;
        ecmComparator = new ECCMComparator();
        for (MiscMounted m : entity.getMisc()) {
            ECMInfo newInfo = null;
            if (m.getType().hasFlag(MiscType.F_COMMUNICATIONS) && m.curMode().equals("ECCM")) {
                if ((entity.getTotalCommGearTons() > 3)) {
                    newInfo = new ECMInfo(6, 0.5, entity);
                }
                if ((entity.getTotalCommGearTons() > 6)) {
                    newInfo = new ECMInfo(6, 1, entity);
                }
            }
            // Angel ECM
            if (m.getType().hasFlag(MiscType.F_ANGEL_ECM)) {
                if (m.curMode().equals("ECCM")) {
                    newInfo = new ECMInfo(6, 0, entity);
                    newInfo.setAngelECCMStrength(1);
                } else if (m.curMode().equals("ECM & ECCM") || m.curMode().equals("ECCM & Ghost Targets")) {
                    // In dual mode, ECCM operates at regular strength, not Angel strength
                    newInfo = new ECMInfo(6, 0, entity);
                    newInfo.setECCMStrength(1);
                }
                // BA Angel ECM has a shorter range
                if ((newInfo != null) && (entity instanceof BattleArmor)) {
                    newInfo.setRange(2);
                }
                // Anything that's not Angel ECM
            } else if (m.getType().hasFlag(MiscType.F_ECM) && m.curMode().equals("ECCM")) {
                int range = 6;
                if (m.getType().hasFlag(MiscType.F_SINGLE_HEX_ECM)) {
                    range = 0;
                } else if (m.getType().hasFlag(MiscType.F_EW_EQUIPMENT) ||
                      m.getType().hasFlag(MiscType.F_NOVA) ||
                      m.getType().hasFlag(MiscType.F_WATCHDOG)) {
                    range = 3;
                }
                newInfo = new ECMInfo(range, 0, entity);
                newInfo.setECCMStrength(1);
            }
            // In some type of ECCM mode...
            if (newInfo != null) {
                if ((bestInfo == null) || (ecmComparator.compare(newInfo, bestInfo) > 0)) {
                    bestInfo = newInfo;
                }
            }
        }
        return bestInfo;
    }

    private ComputeECM() {}
}
