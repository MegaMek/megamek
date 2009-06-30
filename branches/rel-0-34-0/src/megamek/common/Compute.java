/*
 * MegaMek -
 * Copyright (C) 2000,2001,2002,2003,2004,2005 Ben Mazur (bmazur@sev.org)
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License as published by the Free
 *  Software Foundation; either version 2 of the License, or (at your option)
 *  any later version.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *  or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 *  for more details.
 */

package megamek.common;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import megamek.common.actions.BAVibroClawAttackAction;
import megamek.common.actions.BreakGrappleAttackAction;
import megamek.common.actions.BrushOffAttackAction;
import megamek.common.actions.ClubAttackAction;
import megamek.common.actions.EntityAction;
import megamek.common.actions.GrappleAttackAction;
import megamek.common.actions.JumpJetAttackAction;
import megamek.common.actions.KickAttackAction;
import megamek.common.actions.LayExplosivesAttackAction;
import megamek.common.actions.ProtomechPhysicalAttackAction;
import megamek.common.actions.PunchAttackAction;
import megamek.common.actions.PushAttackAction;
import megamek.common.actions.ThrashAttackAction;
import megamek.common.actions.TripAttackAction;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.weapons.BayWeapon;

/**
 * The compute class is designed to provide static methods for mechs and other
 * entities moving, firing, etc.
 */
public class Compute {

    public static final int ARC_360 = 0;
    public static final int ARC_FORWARD = 1;
    public static final int ARC_LEFTARM = 2;
    public static final int ARC_RIGHTARM = 3;
    public static final int ARC_REAR = 4;
    public static final int ARC_LEFTSIDE = 5;
    public static final int ARC_RIGHTSIDE = 6;
    public static final int ARC_MAINGUN = 7;
    public static final int ARC_NORTH = 8;
    public static final int ARC_EAST = 9;
    public static final int ARC_WEST = 10;
    public static final int ARC_NOSE = 11;
    public static final int ARC_LWING = 12;
    public static final int ARC_RWING = 13;
    public static final int ARC_LWINGA = 14;
    public static final int ARC_RWINGA = 15;
    public static final int ARC_LEFTSIDE_SPHERE = 16;
    public static final int ARC_RIGHTSIDE_SPHERE = 17;
    public static final int ARC_LEFTSIDEA_SPHERE = 18;
    public static final int ARC_RIGHTSIDEA_SPHERE= 19;
    public static final int ARC_LEFT_BROADSIDE = 20;
    public static final int ARC_RIGHT_BROADSIDE =21;
    public static final int ARC_AFT = 22;
    public static final int ARC_LEFT_SPHERE_GROUND = 23;
    public static final int ARC_RIGHT_SPHERE_GROUND =24;
    public static final int ARC_TURRET = 25;

    public static final int TYPE_IS = 0;
    public static final int TYPE_CLAN = 1;
    public static final int TYPE_MD = 2;

    public static final int LEVEL_GREEN = 0;
    public static final int LEVEL_REGULAR = 1;
    public static final int LEVEL_VETERAN = 2;
    public static final int LEVEL_ELITE = 3;

    public static final int METHOD_TW = 0;
    public static final int METHOD_TAHARQA = 1;
    public static final int METHOD_CONSTANT = 2;

    public static final int WEAPON_DIRECT_FIRE = 0;
    public static final int WEAPON_CLUSTER_BALLISTIC = 1;
    public static final int WEAPON_PULSE = 2;
    public static final int WEAPON_CLUSTER_MISSILE = 3;
    public static final int WEAPON_CLUSTER_MISSILE_1D6 = 4;
    public static final int WEAPON_CLUSTER_MISSILE_2D6 = 5;
    public static final int WEAPON_CLUSTER_MISSILE_3D6 = 6;

    private static final int[][] skillLevels = new int[][] {
        { 7, 6, 5, 4, 4, 3, 2, 1, 0 },
        { 7, 7, 6, 6, 5, 4, 3, 2, 1 } };

    private static MMRandom random = MMRandom.generate(MMRandom.R_DEFAULT);

    private static final int[][] clusterHitsTable = new int[][] {
        { 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1 },
        { 2, 1, 1, 1, 1, 1, 1, 2, 2, 2, 2, 2 },
        { 3, 1, 1, 1, 2, 2, 2, 2, 2, 3, 3, 3 },
        { 4, 1, 2, 2, 2, 2, 3, 3, 3, 3, 4, 4 },
        { 5, 1, 2, 2, 3, 3, 3, 3, 4, 4, 5, 5 },
        { 6, 2, 2, 3, 3, 4, 4, 4, 5, 5, 6, 6 },
        { 7, 2, 2, 3, 4, 4, 4, 4, 6, 6, 7, 7 },
        { 8, 2, 3, 3, 4, 4, 5, 5, 6, 7, 8, 8 },
        { 9, 3, 3, 4, 5, 5, 5, 5, 7, 7, 9, 9 },
        { 10, 3, 3, 4, 6, 6, 6, 6, 8, 8, 10, 10 },
        { 11, 4, 4, 5, 7, 7, 7, 7, 9, 9, 11, 11 },
        { 12, 4, 4, 5, 8, 8, 8, 8, 10, 10, 12, 12 },
        { 13, 4, 4, 5, 8, 8, 8, 8, 11, 11, 13, 13 },
        { 14, 5, 5, 6, 9, 9, 9, 9, 11, 11, 14, 14 },
        { 15, 5, 5, 6, 9, 9, 9, 9, 12, 12, 15, 15 },
        { 16, 5, 5, 7, 10, 10, 10, 10, 13, 13, 16, 16 },
        { 17, 5, 5, 7, 10, 10, 10, 10, 14, 14, 17, 17 },
        { 18, 6, 6, 8, 11, 11, 11, 11, 14, 14, 18, 18 },
        { 19, 6, 6, 8, 11, 11, 11, 11, 15, 15, 19, 19 },
        { 20, 6, 6, 9, 12, 12, 12, 12, 16, 16, 20, 20 },
        { 21, 7, 7, 9, 13, 13, 13, 13, 17, 17, 21, 21 },
        { 22, 7, 7, 9, 14, 14, 14, 14, 18, 18, 22, 22 },
        { 23, 7, 7, 10, 15, 15, 15, 15, 19, 19, 23, 23 },
        { 24, 8, 8, 10, 16, 16, 16, 16, 20, 20, 24, 24 },
        { 25, 8, 8, 10, 16, 16, 16, 16, 21, 21, 25, 25 },
        { 26, 9, 9, 11, 17, 17, 17, 17, 21, 21, 26, 26 },
        { 27, 9, 9, 11, 17, 17, 17, 17, 22, 22, 27, 27 },
        { 28, 9, 9, 11, 17, 17, 17, 17, 23, 23, 28, 28 },
        { 29, 10, 10, 12, 18, 18, 18, 18, 23, 23, 29, 29 },
        { 30, 10, 10, 12, 18, 18, 18, 18, 24, 24, 30, 30 },
        { 40, 12, 12, 18, 24, 24, 24, 24, 32, 32, 40, 40 } };

    /** Wrapper to random#d6(n) */
    public static int d6(int dice) {
        Roll roll = random.d6(dice);
        return roll.getIntValue();
    }

    /** Wrapper to random#d6() */
    public static int d6() {
        Roll roll = random.d6();
        return roll.getIntValue();
    }

    /** Wrapper to random#randomInt(n) */
    public static int randomInt(int maxValue) {
        Roll roll = new MMRoll(random, maxValue);
        return roll.getIntValue();
    }

    /**
     * Sets the RNG to the desired type
     */
    public static void setRNG(int type) {
        random = MMRandom.generate(type);
    }

    /**
     * Returns the odds that a certain number or above will be rolled on 2d6.
     */
    public static double oddsAbove(int n) {
        if (n <= 2) {
            return 100.0;
        } else if (n > 12) {
            return 0;
        }
        final double[] odds = { 100.0, 100.0, 100.0, 97.2, 91.6, 83.3, 72.2, 58.3, 41.6, 27.7, 16.6, 8.3, 2.78, 0 };
        return odds[n];
    }

    /**
     * Returns an entity if the specified entity would cause a stacking
     * violation entering a hex, or returns null if it would not. The returned
     * entity is the entity causing the violation.
     */
    public static Entity stackingViolation(IGame game, int enteringId, Coords coords) {
        Entity entering = game.getEntity(enteringId);
        return Compute.stackingViolation(game, entering, coords, null);
    }

    /**
     * When compiling an unloading step, both the transporter and the unloaded
     * unit probably occupy some other position on the board.
     */
    public static Entity stackingViolation(IGame game, Entity entering, Coords coords, Entity transport) {
        //no stacking violations on the low-atmosphere and space maps
        if(!game.getBoard().onGround()) {
            return null;
        }

        boolean isMech = entering instanceof Mech;
        Entity firstEntity = transport;
        int totalUnits = 1;
        int thisLowStackingLevel = entering.getElevation();
        if ((coords != null) && (entering.getPosition() != null)) {
            thisLowStackingLevel = entering.calcElevation(game.getBoard()
                    .getHex(entering.getPosition()), game.getBoard().getHex(
                    coords));
        }
        int thisHighStackingLevel = thisLowStackingLevel + entering.height();

        // Walk through the entities in the given hex.
        for (Enumeration<Entity> i = game.getEntities(coords); i.hasMoreElements();) {
            final Entity inHex = i.nextElement();

            int lowStackinglevel = inHex.getElevation();
            int highStackingLevel = lowStackinglevel + inHex.height();

            // Only do all this jazz if they're close enough together on level
            // to interfere.
            if ((thisLowStackingLevel <= highStackingLevel) && (thisHighStackingLevel >= lowStackinglevel)) {
                // Don't compare the entering entity to itself.
                if (inHex.equals(entering)) {
                    continue;
                }

                // Ignore the transport of the entering entity.
                if (inHex.equals(transport)) {
                    continue;
                }

                // DFAing units don't count towards stacking
                if (inHex.isMakingDfa()) {
                    continue;
                }

                // If the entering entity is a mech,
                // then any other mech in the hex is a violation.
                // Unless grappled
                if (isMech && (inHex instanceof Mech) && (((Mech) inHex).getGrappled() != entering.getId())) {
                    return inHex;
                }

                totalUnits++;
                // If the new one is the most
                if (totalUnits > 4) {
                    // Arbitrarily return this one, because we can, and it's
                    // simpler.
                    return inHex;
                }

                // Otherwise, if there are two present entities controlled
                // by this player, returns a random one of the two.
                // Somewhat arbitrary, but how else should we resolve it?
                if (!inHex.getOwner().isEnemyOf(entering.getOwner())) {
                    if (firstEntity == null) {
                        firstEntity = inHex;
                    } else {
                        return Compute.d6() > 3 ? firstEntity : inHex;
                    }
                }
            }
        }

        // okay, all clear
        return null;
    }

    /**
     * Returns true if there is any unit that is an enemy of the specified unit
     * in the specified hex. This is only called for stacking purposes, and so
     * does not return true if the enemy unit is currenly making a DFA.
     */
    public static boolean isEnemyIn(IGame game, Entity entity, Coords coords,
            boolean onlyMechs, boolean ignoreInfantry, int enLowEl) {
        int enHighEl = enLowEl + entity.getHeight();
        for (Enumeration<Entity> i = game.getEntities(coords); i.hasMoreElements();) {
            final Entity inHex = i.nextElement();
            int inHexEnLowEl = inHex.getElevation();
            int inHexEnHighEl = inHexEnLowEl + inHex.getHeight();
            if ((!onlyMechs || (inHex instanceof Mech))
                    && !(ignoreInfantry && (inHex instanceof Infantry))
                    && inHex.isEnemyOf(entity) && !inHex.isMakingDfa()
                    && (enLowEl <= inHexEnHighEl) && (enHighEl >= inHexEnLowEl)) {
                return true;
            }
        }
        return false;
    }

    /**
     * @return true if a piloting skill roll is needed to traverse the terrain
     */
    public static boolean isPilotingSkillNeeded(IGame game, int entityId,
            Coords src, Coords dest, int movementType, boolean isTurning,
            boolean prevStepIsOnPavement, int srcElevation, int destElevation,
            MovePath path) {
        final Entity entity = game.getEntity(entityId);
        final IHex srcHex = game.getBoard().getHex(src);
        final IHex destHex = game.getBoard().getHex(dest);
        final boolean isInfantry = (entity instanceof Infantry);
        final boolean isPavementStep = Compute.canMoveOnPavement(game, src, dest, path);

        // arguments valid?
        if (entity == null) {
            throw new IllegalArgumentException("Entity invalid.");
        }
        if (src.distance(dest) > 1) {
            throw new IllegalArgumentException("Coordinates must be adjacent.");
        }

        // let's only worry about actual movement, please
        if (src.equals(dest)) {
            return false;
        }

        // check for rubble
        if ((movementType != IEntityMovementType.MOVE_JUMP)
                && (destHex.terrainLevel(Terrains.RUBBLE) > 0)
                && (entity.getMovementMode() != IEntityMovementMode.VTOL)
                && !isInfantry) {
            return true;
        }

        // check for swamp
        if (destHex.containsTerrain(Terrains.SWAMP)
                && !(entity.getElevation() > destHex.getElevation())
                && (entity.getMovementMode() != IEntityMovementMode.HOVER)
                && (entity.getMovementMode() != IEntityMovementMode.VTOL)
                && (movementType != IEntityMovementType.MOVE_JUMP)
                && (entity.getMovementMode() != IEntityMovementMode.WIGE)) {
            return true;
        }

        // check for thin ice
        if (destHex.containsTerrain(Terrains.ICE)
                && destHex.containsTerrain(Terrains.WATER)
                && !(entity.getElevation() > destHex.getElevation())
                && !isPavementStep
                && (movementType != IEntityMovementType.MOVE_JUMP)) {
            return true;
        }

        // Check for water unless we're a hovercraft or naval or using a bridge
        // or flying.
        if ((movementType != IEntityMovementType.MOVE_JUMP)
                && !(entity.getElevation() > destHex.surface())
                && !((entity.getMovementMode() == IEntityMovementMode.HOVER)
                        || (entity.getMovementMode() == IEntityMovementMode.NAVAL)
                        || (entity.getMovementMode() == IEntityMovementMode.HYDROFOIL)
                        || (entity.getMovementMode() == IEntityMovementMode.SUBMARINE)
                        || (entity.getMovementMode() == IEntityMovementMode.INF_UMU)
                        || (entity.getMovementMode() == IEntityMovementMode.BIPED_SWIM)
                        || (entity.getMovementMode() == IEntityMovementMode.QUAD_SWIM) || (entity
                        .getMovementMode() == IEntityMovementMode.WIGE))
                && (destHex.terrainLevel(Terrains.WATER) > 0) && !isPavementStep) {
            return true;
        }

        // Check for skid. Please note, the skid will be rolled on the
        // current step, but starts from the previous step's location.
        // TODO: add check for elevation of pavement, road,
        // or bridge matches entity elevation.
        /*
         * Bug 754610: Revert fix for bug 702735. if ( (
         * srcHex.contains(Terrain.PAVEMENT) || srcHex.contains(Terrain.ROAD) ||
         * srcHex.contains(Terrain.BRIDGE) )
         */
        if (((prevStepIsOnPavement && (movementType == IEntityMovementType.MOVE_RUN))
                || ((srcHex.containsTerrain(Terrains.ICE))
                && (movementType != IEntityMovementType.MOVE_JUMP)))
                && (entity.getMovementMode() != IEntityMovementMode.HOVER)
                && (entity.getMovementMode() != IEntityMovementMode.WIGE)
                && isTurning && !isInfantry) {
            return true;
        }

        // If we entering a building, all non-infantry
        // need to make a piloting check to avoid damage.
        if ((destElevation < destHex.terrainLevel(Terrains.BLDG_ELEV))
                && !(entity instanceof Infantry)) {
            return true;
        }

        // check sideslips
        if ((entity instanceof VTOL)
                || (entity.getMovementMode() == IEntityMovementMode.HOVER)
                || (entity.getMovementMode() == IEntityMovementMode.WIGE)) {
            if (isTurning
                    && ((movementType == IEntityMovementType.MOVE_RUN)
                            || (movementType == IEntityMovementType.MOVE_VTOL_RUN))) {
                return true;
            }
        }

        return false;
    }

    /**
     * Can the defending unit be displaced from the source to the destination?
     */
    public static boolean isValidDisplacement(IGame game, int entityId, Coords src, int direction) {
        return Compute.isValidDisplacement(game, entityId, src, src.translated(direction));
    }

    /**
     * Can the defending unit be displaced from the source to the destination?
     */
    public static boolean isValidDisplacement(IGame game, int entityId, Coords src, Coords dest) {
        final Entity entity = game.getEntity(entityId);
        final IHex srcHex = game.getBoard().getHex(src);
        final IHex destHex = game.getBoard().getHex(dest);
        final ArrayList<Coords> intervening = Coords.intervening(src, dest);
        final int direction = src.direction(dest);

        // arguments valid?
        if (entity == null) {
            throw new IllegalArgumentException("Entity invalid.");
        }

        // an easy check
        if (!game.getBoard().contains(dest)) {
            if (game.getOptions().booleanOption("push_off_board")) {
                return true;
            }
            return false;
        }

        // can't be displaced into prohibited terrain
        // unless we're displacing a tracked or wheeled vee into water
        if (entity.isHexProhibited(destHex)
                && !((entity instanceof Tank)
                        && destHex.containsTerrain(Terrains.WATER)
                        && ((entity.movementMode == IEntityMovementMode.TRACKED)
                                || (entity.movementMode == IEntityMovementMode.WHEELED)))) {
            return false;
        }

        // can't go up more levels than normally possible
        for (Coords c : intervening) {
            // ignore off-board hexes
            if (!game.getBoard().contains(c)) {
                continue;
            }
            final IHex hex = game.getBoard().getHex(c);
            int change = entity.elevationOccupied(hex) - entity.elevationOccupied(srcHex);
            if (change > entity.getMaxElevationChange()) {
                return false;
            }
        }

        // if there's an entity in the way, can they be displaced in that
        // direction?
        Entity inTheWay = Compute.stackingViolation(game, entityId, dest);
        if (inTheWay != null) {
            return Compute.isValidDisplacement(game, inTheWay.getId(), inTheWay.getPosition(), direction);
        }

        // okay, that's about all the checks
        return true;
    }

    /**
     * Gets a valid displacement, from the hexes around src, as close to the
     * original direction as is possible.
     *
     * @return valid displacement coords, or null if none
     */
    public static Coords getValidDisplacement(IGame game, int entityId, Coords src, int direction) {
        // check the surrounding hexes, nearest to the original direction first
        int[] offsets = { 0, 1, 5, 2, 4, 3 };
        for (int offset : offsets) {
            Coords dest = src.translated((direction + offset) % 6);
            if (Compute.isValidDisplacement(game, entityId, src, dest)) {
                return dest;
            }
        }
        // have fun being insta-killed!
        return null;
    }

    /**
     * Gets a preferred displacement. Right now this picks the surrounding hex
     * with the same elevation as original hex, if not available it picks the
     * highest elevation that is a valid displacement.
     * This will preferably not displace into friendly units
     *
     * @return valid displacement coords, or null if none
     */
    public static Coords getPreferredDisplacement(IGame game, int entityId, Coords src, int direction) {
        final Entity entity = game.getEntity(entityId);
        int highestElev = Integer.MIN_VALUE;
        Coords highest = null;

        // check the surrounding hexes, nearest to the original direction first
        int[] offsets = { 0, 1, 5, 2, 4, 3 };
        // first, try not to displace into friendly units
        for (int offset : offsets) {
            Coords dest = src.translated((direction + offset) % 6);
            if (Compute.isValidDisplacement(game, entityId, src, dest)
                    && game.getBoard().contains(dest)) {
                Enumeration<Entity> entities = game.getFriendlyEntities(dest, game.getEntity(entityId));
                if (entities.hasMoreElements()) {
                    // friendly unit here, try next hex
                    continue;
                }
                IHex hex = game.getBoard().getHex(dest);
                int elevation = entity.elevationOccupied(hex);
                if (elevation > highestElev) {
                    highestElev = elevation;
                    highest = dest;
                }
                // preferably, go to same elevation
                if (elevation == entity.getElevation()) {
                    return dest;
                }
            }
        }
        if (highest != null) {
            return highest;
        }
        // ok, all hexes occupied, now displace preferably to same elevation,
        // else highest
        for (int offset : offsets) {
            Coords dest = src.translated((direction + offset) % 6);
            if (Compute.isValidDisplacement(game, entityId, src, dest)
                    && game.getBoard().contains(dest)) {
                IHex hex = game.getBoard().getHex(dest);
                int elevation = entity.elevationOccupied(hex);
                if (elevation > highestElev) {
                    highestElev = elevation;
                    highest = dest;
                }
                // preferably, go to same elevation
                if (elevation == entity.getElevation()) {
                    return dest;
                }
            }
        }
        return highest;
    }

    /**
     * Gets a hex to displace a missed charge to. Picks left or right, first
     * preferring higher hexes, then randomly, or returns the base hex if
     * they're impassible.
     */
    public static Coords getMissedChargeDisplacement(IGame game, int entityId, Coords src, int direction) {
        Coords first = src.translated((direction + 1) % 6);
        Coords second = src.translated((direction + 5) % 6);
        IHex firstHex = game.getBoard().getHex(first);
        IHex secondHex = game.getBoard().getHex(second);
        Entity entity = game.getEntity(entityId);

        if ((firstHex == null) || (secondHex == null)) {
            // leave it, will be handled
        } else if (entity.elevationOccupied(firstHex) > entity.elevationOccupied(secondHex)) {
            // leave it
        } else if (entity.elevationOccupied(firstHex) < entity.elevationOccupied(secondHex)) {
            // switch
            Coords temp = first;
            first = second;
            second = temp;
        } else if (Compute.d6() > 3) {
            // switch randomly
            Coords temp = first;
            first = second;
            second = temp;
        }

        if (Compute.isValidDisplacement(game, entityId, src, src.direction(first)) && game.getBoard().contains(first)) {
            return first;
        } else if (Compute.isValidDisplacement(game, entityId, src, src.direction(second)) && game.getBoard().contains(second)) {
            return second;
        } else {
            return src;
        }
    }

    /**
     * Finds the best spotter for the attacker. The best spotter is the one with
     * the lowest attack modifiers, of course. LOS modifiers and movement are
     * considered.
     */
    public static Entity findSpotter(IGame game, Entity attacker, Targetable target) {
        Entity spotter = null;
        int taggedBy = -1;
        if (target instanceof Entity) {
            taggedBy = ((Entity) target).getTaggedBy();
        }
        ToHitData bestMods = new ToHitData(TargetRoll.IMPOSSIBLE, "");

        for (java.util.Enumeration<Entity> i = game.getEntities(); i.hasMoreElements();) {
            Entity other = i.nextElement();
            if (((other.isSpotting() && (other.getSpotTargetId() == target.getTargetId()))
                    || (taggedBy == other.getId())) && !attacker.isEnemyOf(other)) {
                // what are this guy's mods to the attack?
                LosEffects los = LosEffects.calculateLos(game, other.getId(), target);
                ToHitData mods = los.losModifiers(game);
                los.setTargetCover(LosEffects.COVER_NONE);
                mods.append(Compute.getAttackerMovementModifier(game, other.getId()));
                if (other.isAttackingThisTurn()) {
                    mods.addModifier(1, "spotter is making an attack this turn");
                }
                // is this guy a better spotter?
                if ((spotter == null) || (mods.getValue() < bestMods.getValue())) {
                    spotter = other;
                    bestMods = mods;
                }
            }
        }

        return spotter;
    }

    public static ToHitData getImmobileMod(Targetable target) {
        return Compute.getImmobileMod(target, Entity.LOC_NONE, IAimingModes.AIM_MODE_NONE);
    }

    public static ToHitData getImmobileMod(Targetable target, int aimingAt, int aimingMode) {
        if (target.isImmobile()) {
            if ((target instanceof Mech) && (aimingAt == Mech.LOC_HEAD) && (aimingMode == IAimingModes.AIM_MODE_IMMOBILE)) {
                return new ToHitData(3, "aiming at head");
            }
            return new ToHitData(-4, "target immobile");
        }
        return null;
    }

    /**
     * Determines the to-hit modifier due to range for an attack with the
     * specified parameters. Includes minimum range, infantry 0-range mods, and
     * target stealth mods. Accounts for friendly C3 units.
     *
     * @return the modifiers
     */
    public static ToHitData getRangeMods(IGame game, Entity ae, int weaponId, Targetable target) {
        Mounted weapon = ae.getEquipment(weaponId);
        WeaponType wtype = (WeaponType) weapon.getType();
        int[] weaponRanges = wtype.getRanges(weapon);
        boolean isAttackerInfantry = (ae instanceof Infantry);
        boolean isWeaponInfantry = wtype.hasFlag(WeaponType.F_INFANTRY);
        boolean isIndirect = ((wtype.getAmmoType() == AmmoType.T_LRM) || (wtype.getAmmoType() == AmmoType.T_MML) || (wtype.getAmmoType() == AmmoType.T_EXLRM) || (wtype.getAmmoType() == AmmoType.T_TBOLT_5) || (wtype.getAmmoType() == AmmoType.T_TBOLT_10) || (wtype.getAmmoType() == AmmoType.T_TBOLT_15) || (wtype.getAmmoType() == AmmoType.T_TBOLT_20) || (wtype.getAmmoType() == AmmoType.T_LRM_TORPEDO)) && weapon.curMode().equals("Indirect");
        boolean useExtremeRange = game.getOptions().booleanOption("tacops_range");

        if(ae instanceof Aero) {
            useExtremeRange = true;
        }

        ToHitData mods = new ToHitData();

        //
        // modifiy the ranges for PPCs when field inhibitors are turned off
        // TODO: See above, it should be coded elsewhere...
        //
        if (wtype.hasFlag(WeaponType.F_PPC)) {
            if (game.getOptions().booleanOption("tacops_ppc_inhibitors")) {
                if ((weapon.curMode() != null) && weapon.curMode().equals("Field Inhibitor OFF")) {
                    weaponRanges[RangeType.RANGE_MINIMUM] = 0;
                }
            }
        }

        // Hotloaded weapons
        if (weapon.isHotLoaded() && game.getOptions().booleanOption("tacops_hotload")) {
            weaponRanges[RangeType.RANGE_MINIMUM] = 0;
        }

        // is water involved?
        IHex targHex = game.getBoard().getHex(target.getPosition());
        int targTop = target.absHeight();
        int targBottom = target.getElevation();

        boolean targetInPartialWater = false;
        boolean targetUnderwater = false;
        boolean weaponUnderwater = (ae.getLocationStatus(weapon.getLocation()) == ILocationExposureStatus.WET);
        if ((target.getTargetType() == Targetable.TYPE_ENTITY) && targHex.containsTerrain(Terrains.WATER) && (targBottom < 0)) {
            if (targTop >= 0) {
                targetInPartialWater = true;
            } else {
                targetUnderwater = true;
            }
        }

        // allow naval units on surface to be attacked from above or below
        Entity te = null;
        if (target instanceof Entity) {
            te = (Entity) target;
            if ((targBottom == 0) && (UnitType.determineUnitTypeCode(te) == UnitType.NAVAL)) {
                targetInPartialWater = true;
            }
        }
        // allow naval units to target underwater units,
        // torpedo tubes are mounted underwater
        if ((targetUnderwater || (wtype.getAmmoType() == AmmoType.T_LRM_TORPEDO)
                || (wtype.getAmmoType() == AmmoType.T_SRM_TORPEDO))
                && (UnitType.determineUnitTypeCode(ae) == UnitType.NAVAL)) {
            weaponUnderwater = true;
            weaponRanges = wtype.getWRanges();
        }
        // allow ice to be cleared from below
        if (targHex.containsTerrain(Terrains.WATER) && (target.getTargetType() == Targetable.TYPE_HEX_CLEAR)) {
            targetInPartialWater = true;
        }

        if (weaponUnderwater) {
            weaponRanges = wtype.getWRanges();
            boolean MPM = false;
            if ((wtype.getAmmoType() == AmmoType.T_SRM)
                    || (wtype.getAmmoType() == AmmoType.T_MRM)
                    || (wtype.getAmmoType() == AmmoType.T_LRM)
                    || (wtype.getAmmoType() == AmmoType.T_MML)) {
                AmmoType atype = (AmmoType) weapon.getLinked().getType();
                if (atype.getMunitionType() == AmmoType.M_TORPEDO) {
                    weaponRanges = wtype.getRanges(weapon);
                } else if (atype.getMunitionType() == AmmoType.M_MULTI_PURPOSE) {
                    weaponRanges = wtype.getRanges(weapon);
                    MPM = true;
                }
            }

            // HACK on ranges: for those without underwater range,
            // long == medium; iteration in rangeBracket() allows this
            if (weaponRanges[RangeType.RANGE_SHORT] == 0) {
                return new ToHitData(TargetRoll.IMPOSSIBLE, "Weapon cannot fire underwater.");
            }
            if (!targetUnderwater && !targetInPartialWater && !MPM) {
                // target on land or over water
                return new ToHitData(TargetRoll.IMPOSSIBLE, "Weapon underwater, but not target.");
            }
            // special case: mechs can only fire upper body weapons at surface
            // naval
            if ((te != null) && (UnitType.determineUnitTypeCode(te) == UnitType.NAVAL)
                    && (ae instanceof Mech) && (ae.height() > 0)
                    && (ae.getElevation() == -1)) {
                return new ToHitData(TargetRoll.IMPOSSIBLE, "Partially submerged mech cannot fire leg weapons at surface naval vessels.");
            }
        } else if (targetUnderwater) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "Target underwater, but not weapon.");
        } else if ((wtype.getAmmoType() == AmmoType.T_LRM_TORPEDO) || (wtype.getAmmoType() == AmmoType.T_SRM_TORPEDO)) {
            // Torpedos only fire underwater.
            return new ToHitData(TargetRoll.IMPOSSIBLE, "Weapon can only fire underwater.");
        }

        //if Aero then adjust to stanard ranges
        if(ae instanceof Aero) {
            weaponRanges = wtype.getATRanges();
        }

        // determine base distance & range bracket
        int distance = Compute.effectiveDistance(game, ae, target);
        int range = RangeType.rangeBracket(distance, weaponRanges, useExtremeRange);

        int maxRange = wtype.getMaxRange();
        //if this is a weapon bay I need to cycle through weapons
        if(wtype instanceof BayWeapon) {
            for(int wId : weapon.getBayWeapons()) {
                Mounted bayW = ae.getEquipment(wId);
                WeaponType bayWType = (WeaponType)bayW.getType();
                if(bayWType.getMaxRange() > maxRange) {
                    maxRange = bayWType.getMaxRange();
                }
            }
        }

        //if aero and greater than max range then swith to range_out
        if((ae instanceof Aero) && (range > maxRange)) {
            range = RangeType.RANGE_OUT;
        }

        // short circuit if at zero range or out of range
        if (range == RangeType.RANGE_OUT) {
            return new ToHitData(TargetRoll.AUTOMATIC_FAIL, "Target out of range");
        }
        if ((distance == 0) && !isAttackerInfantry && !(ae instanceof Aero)
                && !((ae instanceof Mech) && (((Mech) ae).getGrappled() == target.getTargetId()))) {
            return new ToHitData(TargetRoll.AUTOMATIC_FAIL, "Only infantry shoot at zero range");
        }

        //Account for "dead zones" between Aeros at different altitudes
        if(game.getBoard().inAtmosphere() && (ae instanceof Aero) && (target instanceof Aero)) {
            int altDiff = Math.abs(ae.getElevation() - target.getElevation());
            int realDistance = distance - altDiff;
            if(altDiff >= realDistance) {
                return new ToHitData(TargetRoll.IMPOSSIBLE,
                "Target in dead zone");
            }
        }

        // find any c3 spotters that could help
        Entity c3spotter = Compute.findC3Spotter(game, ae, target);
        if (isIndirect) {
            c3spotter = ae; // no c3 when using indirect fire
        }
        if (isIndirect && game.getOptions().booleanOption("indirect_fire")
                && !game.getOptions().booleanOption("indirect_always_possible")
                && LosEffects.calculateLos(game, ae.getId(), target).canSee()) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "Indirect fire impossible with direct LOS");
        }

        int c3dist = Compute.effectiveDistance(game, c3spotter, target);
        int c3range = RangeType.rangeBracket(c3dist, weaponRanges, useExtremeRange);

        /*
         * Tac Ops Extreme Range Rule p. 85 if the weapons normal range is Extreme then C3
         * uses the next highest range bracket, i.e. medium instead of short.
         */
        if ( range == RangeType.RANGE_EXTREME ) {
            c3range++;
        }

        // determine which range we're using
        int usingRange = Math.min(range, c3range);

        String targSysType = "";
        // Get the targeting system type string ready, if necessary
        if ((ae.getTargSysType() == MiscType.T_TARGSYS_LONGRANGE) && (ae.getTargSysType() == MiscType.T_TARGSYS_SHORTRANGE)) {
            targSysType = " (w/" + MiscType.getTargetSysName(ae.getTargSysType()) + ")";
        }

        // add range modifier
        if (usingRange == range) {
            // no c3 adjustment
            if (((range == RangeType.RANGE_SHORT) || (range == RangeType.RANGE_MINIMUM)) && (ae.getShortRangeModifier() != 0)) {
                mods.addModifier(ae.getShortRangeModifier(), "short range" + targSysType);
            } else if (range == RangeType.RANGE_MEDIUM) {
                // Right now, the range-mod affecting targeting systems DON'T
                // affect medium range, so we won't add that here ever.
                mods.addModifier(ae.getMediumRangeModifier(), "medium range");
            } else if (range == RangeType.RANGE_LONG) {
                // Protos that loose head sensors can't shoot long range.
                if ((ae instanceof Protomech) && (2 == ((Protomech) ae).getCritsHit(Protomech.LOC_HEAD))) {
                    mods.addModifier(TargetRoll.IMPOSSIBLE, "No long range attacks with destroyed head sensors.");
                } else {
                    mods.addModifier(ae.getLongRangeModifier(), "long range" + targSysType);
                }
            } else if (range == RangeType.RANGE_EXTREME) {
                // Protos that loose head sensors can't shoot extreme range.
                if ((ae instanceof Protomech) && (2 == ((Protomech) ae).getCritsHit(Protomech.LOC_HEAD))) {
                    mods.addModifier(TargetRoll.IMPOSSIBLE, "No extreme range attacks with destroyed head sensors.");
                } else {
                    mods.addModifier(ae.getExtremeRangeModifier(), "extreme range" + targSysType);
                }
            }
        } else {
            // report c3 adjustment
            if ((c3range == RangeType.RANGE_SHORT) || (c3range == RangeType.RANGE_MINIMUM)) {
                mods.addModifier(ae.getShortRangeModifier(), "short range due to C3 spotter" + targSysType);
            } else if (c3range == RangeType.RANGE_MEDIUM) {
                mods.addModifier(ae.getMediumRangeModifier(), "medium range due to C3 spotter" + targSysType);
            } else if (c3range == RangeType.RANGE_LONG) {
                mods.addModifier(ae.getLongRangeModifier(), "long range due to C3 spotter" + targSysType);
            }
        }

        // add infantry zero-range modifier
        // TODO: this is not the right place to hardcode these
        if (isWeaponInfantry && (distance == 0)) {
            // Infantry platoons attacking with infantry weapons can attack
            // in the same hex with a base of 2, except for flamers and
            // SRMs/LRMs, which have a base of 3.
            if (wtype.hasFlag(WeaponType.F_FLAMER)) {
                mods.addModifier(-1, "infantry flamer assault");
            } else if ((wtype.getAmmoType() == AmmoType.T_SRM) || (wtype.getAmmoType() == AmmoType.T_LRM)) {
                mods.addModifier(-1, "infantry missile assault");
            } else {
                mods.addModifier(-2, "infantry assault");
            }
        }

        // add minimum range modifier
        int minRange = weaponRanges[RangeType.RANGE_MINIMUM];
        if ((minRange > 0) && (distance <= minRange)) {
            int minPenalty = (minRange - distance) + 1;
            mods.addModifier(minPenalty, "minimum range");
        }

        // add any target stealth modifier
        if (target instanceof Entity) {
            TargetRoll tmpTR = ((Entity) target).getStealthModifier(usingRange, ae);
            if (tmpTR.getValue() != 0) {
                mods.append(((Entity) target).getStealthModifier(usingRange, ae));
            }
        }

        return mods;
    }

    /**
     * Finds the effective distance between an attacker and a target. Includes
     * the distance bonus if the attacker and target are in the same building
     * and on different levels.
     *
     * @return the effective distance
     */
    public static int effectiveDistance(IGame game, Entity attacker, Targetable target) {
        int distance = attacker.getPosition().distance(target.getPosition());

        // If the attack is completely inside a building, add the difference
        // in elevations between the attacker and target to the range.
        // TODO: should the player be explcitly notified?
        //also for Aeros in atmosphere
        if (Compute.isInSameBuilding(game, attacker, target)  ||
                ((attacker instanceof Aero) && (target instanceof Aero) && game.getBoard().inAtmosphere())) {
            int aElev = attacker.getElevation();
            int tElev = target.getElevation();
            distance += Math.abs(aElev - tElev);
        }

        return distance;
    }

    /**
     * Attempts to find a C3 spotter that is closer to the target than the
     * attacker.
     *
     * @return A closer C3 spotter, or the attack if no spotters are found
     */
    private static Entity findC3Spotter(IGame game, Entity attacker, Targetable target) {
        //TODO: underwater units can't spot for overwater units and vice versa
        if (!attacker.hasC3() && !attacker.hasC3i()) {
            return attacker;
        }

        if (attacker.hasC3i()) {
            return Compute.findC3iSpotter(game, attacker, target);
        }

        Entity c3spotter = attacker;
        int c3range = attacker.getPosition().distance(target.getPosition());

        for (Enumeration<Entity> i = game.getEntities(); i.hasMoreElements();) {
            Entity friend = i.nextElement();

            // TODO : can units being transported be used for C3 spotting?
            if (attacker.equals(friend) || !friend.isActive() || !attacker.onSameC3NetworkAs(friend) || !friend.isDeployed()) {
                continue; // useless to us...
            }

            int buddyRange = Compute.effectiveDistance(game, friend, target);
            if (buddyRange < c3range) {
                c3range = buddyRange;
                c3spotter = friend;
            }
        }
        return c3spotter;
    }

    /**
     * find a c3i spotter that is closer to the target than the
     * attacker.
     * @param game
     * @param attacker
     * @param target
     * @return
     */
    private static Entity findC3iSpotter(IGame game, Entity attacker, Targetable target) {
        if (!attacker.hasC3() && !attacker.hasC3i()) {
            return attacker;
        }
        Entity c3spotter = attacker;

        ArrayList<Entity> network = new ArrayList<Entity>();

        for (Enumeration<Entity> i = game.getEntities(); i.hasMoreElements();) {
            Entity friend = i.nextElement();

            if (attacker.equals(friend) || !attacker.onSameC3NetworkAs(friend, true) || !friend.isDeployed()) {
                continue; // useless to us...
            }

            int buddyRange = Compute.effectiveDistance(game, friend, target);

            boolean added = false;
            //but everyone in the C3i network into a list and sort it by range.
            for (int pos = 0; pos < network.size(); pos++) {
                if (Compute.effectiveDistance(game, network.get(pos), target) >= buddyRange) {
                    network.add(pos, friend);
                    added = true;
                    break;
                }
            }

            if ( !added ) {
                network.add(friend);
            }
        }

        int position = 0;
        for (Entity spotter : network) {

            for (int count = position++; count < network.size(); count++) {
                if (Compute.canCompleteNodePath(spotter, attacker, network, count)) {
                    return spotter;
                }
            }
        }
        return c3spotter;
    }

    private static boolean canCompleteNodePath(Entity start, Entity end, ArrayList<Entity>network, int startPosition) {

        Entity spotter = network.get(startPosition);

        //Last position cannot get to this one. go to the next person
        if ( Compute.isAffectedByECM(spotter, start.getPosition(), spotter.getPosition()) ) {
            return false;
        }

        if ( !Compute.isAffectedByECM(spotter, spotter.getPosition(), end.getPosition()) ) {
            return true;
        }

        for (++startPosition ; startPosition < network.size(); startPosition++) {
            if ( Compute.canCompleteNodePath(spotter,end, network, startPosition) ) {
                return true;
            }
        }

        return false;
    }

    /**
     * Gets the modifiers, if any, that the mech receives from being prone.
     *
     * @return any applicable modifiers due to being prone
     */
    public static ToHitData getProneMods(IGame game, Entity attacker, int weaponId) {
        if (!attacker.isProne()) {
            return null; // no modifier
        }
        ToHitData mods = new ToHitData();
        Mounted weapon = attacker.getEquipment(weaponId);
        if (attacker.entityIsQuad()) {
            int legsDead = ((Mech) attacker).countBadLegs();
            if (legsDead == 0) {
                // No legs destroyed: no penalty and can fire all weapons
                return null; // no modifier
            } else if (legsDead >= 3) {
                return new ToHitData(TargetRoll.IMPOSSIBLE, "Prone with three or more legs destroyed.");
            }
            // we have one or two dead legs...

            // Need an intact front leg
            if (attacker.isLocationBad(Mech.LOC_RARM) && attacker.isLocationBad(Mech.LOC_LARM)) {
                return new ToHitData(TargetRoll.IMPOSSIBLE, "Prone with both front legs destroyed.");
            }

            // front leg-mounted weapons have addidional trouble
            if ((weapon.getLocation() == Mech.LOC_RARM) || (weapon.getLocation() == Mech.LOC_LARM)) {
                int otherArm = weapon.getLocation() == Mech.LOC_RARM ? Mech.LOC_LARM : Mech.LOC_RARM;
                // check previous attacks for weapons fire from the other arm
                if (Compute.isFiringFromArmAlready(game, weaponId, attacker, otherArm)) {
                    return new ToHitData(TargetRoll.IMPOSSIBLE, "Prone and firing from other front leg already.");
                }
            }
            // can't fire rear leg weapons
            if ((weapon.getLocation() == Mech.LOC_LLEG) || (weapon.getLocation() == Mech.LOC_RLEG)) {
                return new ToHitData(TargetRoll.IMPOSSIBLE, "Can't fire rear leg-mounted weapons while prone with destroyed legs.");
            }
            mods.addModifier(2, "attacker prone");
        } else {
            int l3ProneFiringArm = Entity.LOC_NONE;

            if (attacker.isLocationBad(Mech.LOC_RARM) || attacker.isLocationBad(Mech.LOC_LARM)) {
                if (game.getOptions().booleanOption("tacops_prone_fire")) {
                    // Can fire with only one arm
                    if (attacker.isLocationBad(Mech.LOC_RARM) && attacker.isLocationBad(Mech.LOC_LARM)) {
                        return new ToHitData(TargetRoll.IMPOSSIBLE, "Prone with both arms destroyed.");
                    }

                    l3ProneFiringArm = attacker.isLocationBad(Mech.LOC_RARM) ? Mech.LOC_LARM : Mech.LOC_RARM;
                } else {
                    // must have an arm intact
                    return new ToHitData(TargetRoll.IMPOSSIBLE, "Prone with one or both arms destroyed.");
                }
            }

            // arm-mounted weapons have addidional trouble
            if ((weapon.getLocation() == Mech.LOC_RARM) || (weapon.getLocation() == Mech.LOC_LARM)) {
                if (l3ProneFiringArm == weapon.getLocation()) {
                    return new ToHitData(TargetRoll.IMPOSSIBLE, "Prone and propping up with this arm.");
                }

                int otherArm = weapon.getLocation() == Mech.LOC_RARM ? Mech.LOC_LARM : Mech.LOC_RARM;
                // check previous attacks for weapons fire from the other arm
                if (Compute.isFiringFromArmAlready(game, weaponId, attacker, otherArm)) {
                    return new ToHitData(TargetRoll.IMPOSSIBLE, "Prone and firing from other arm already.");
                }
            }
            // can't fire leg weapons
            if ((weapon.getLocation() == Mech.LOC_LLEG) || (weapon.getLocation() == Mech.LOC_RLEG)) {
                return new ToHitData(TargetRoll.IMPOSSIBLE, "Can't fire leg-mounted weapons while prone.");
            }
            mods.addModifier(2, "attacker prone");

            if (l3ProneFiringArm != Entity.LOC_NONE) {
                mods.addModifier(1, "attacker propping on single arm");
            }
        }
        return mods;
    }

    /**
     * Checks to see if there is an attack previous to the one with this weapon
     * from the specified arm.
     *
     * @return true if there is a previous attack from this arm
     */
    private static boolean isFiringFromArmAlready(IGame game, int weaponId, final Entity attacker, int armLoc) {
        int torsoLoc = Mech.getInnerLocation(armLoc);
        for (Enumeration<EntityAction> i = game.getActions(); i.hasMoreElements();) {
            EntityAction ea = i.nextElement();
            if (!(ea instanceof WeaponAttackAction)) {
                continue;
            }
            WeaponAttackAction prevAttack = (WeaponAttackAction) ea;
            // stop when we get to this weaponattack (does this always work?)
            if ((prevAttack.getEntityId() == attacker.getId()) && (prevAttack.getWeaponId() == weaponId)) {
                break;
            }
            if (((prevAttack.getEntityId() == attacker.getId()) && (attacker.getEquipment(prevAttack.getWeaponId()).getLocation() == armLoc)) || ((prevAttack.getEntityId() == attacker.getId()) && (attacker.getEquipment(prevAttack.getWeaponId()).getLocation() == torsoLoc) && attacker.getEquipment(prevAttack.getWeaponId()).isSplit())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Adds any damage modifiers from arm critical hits or sensor damage.
     *
     * @return Any applicable damage modifiers
     */
    public static ToHitData getDamageWeaponMods(Entity attacker, Mounted weapon) {
        ToHitData mods = new ToHitData();
        if (attacker instanceof Protomech) {
            // Head criticals add to target number of all weapons.
            int hits = ((Protomech) attacker).getCritsHit(Protomech.LOC_HEAD);
            if (hits > 0) {
                mods.addModifier(hits, hits + " head critical(s)");
            }

            // Arm mounted (and main gun) weapons get DRMs from arm crits.
            switch (weapon.getLocation()) {
            case Protomech.LOC_LARM:
            case Protomech.LOC_RARM:
                hits = ((Protomech) attacker).getCritsHit(weapon.getLocation());
                if (hits > 0) {
                    mods.addModifier(hits, hits + " arm critical(s)");
                }
                break;
            case Protomech.LOC_MAINGUN:
                // Main gun is affected by crits in *both* arms.
                hits = ((Protomech) attacker).getCritsHit(Protomech.LOC_LARM);
                hits += ((Protomech) attacker).getCritsHit(Protomech.LOC_RARM);
                if (4 == hits) {
                    mods.addModifier(TargetRoll.IMPOSSIBLE, "Cannot fire main gun with no arms.");
                } else if (hits > 0) {
                    mods.addModifier(hits, hits + " arm critical(s)");
                }
                break;
            }

        } // End attacker-is-Protomech

        // Is the shoulder destroyed?
        else {
            // split weapons need to account for arm actuator hits, too
            // see bug 1363690
            // we don't need to specifically check for weapons split between
            // torso and leg, because for those, the location stored in the
            // Mounted is the leg.
            int location = weapon.getLocation();
            if (weapon.isSplit()) {
                switch (location) {
                case Mech.LOC_LT:
                    location = Mech.LOC_LARM;
                    break;
                case Mech.LOC_RT:
                    location = Mech.LOC_RARM;
                    break;
                default:
                }
            }
            if (attacker.getBadCriticals(CriticalSlot.TYPE_SYSTEM, Mech.ACTUATOR_SHOULDER, location) > 0) {
                mods.addModifier(4, "shoulder actuator destroyed");
            } else {
                // no shoulder hits, add other arm hits
                int actuatorHits = 0;
                if (attacker.getBadCriticals(CriticalSlot.TYPE_SYSTEM, Mech.ACTUATOR_UPPER_ARM, location) > 0) {
                    actuatorHits++;
                }
                if (attacker.getBadCriticals(CriticalSlot.TYPE_SYSTEM, Mech.ACTUATOR_LOWER_ARM, location) > 0) {
                    actuatorHits++;
                }
                if (actuatorHits > 0) {
                    mods.addModifier(actuatorHits, actuatorHits + " destroyed arm actuators");
                }
            }
        }

        // sensors critical hit to attacker
        int sensorHits = attacker.getBadCriticals(CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_SENSORS, Mech.LOC_HEAD);
        if ((attacker instanceof Mech) && (((Mech) attacker).getCockpitType() == Mech.COCKPIT_TORSO_MOUNTED)) {
            sensorHits += attacker.getBadCriticals(CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_SENSORS, Mech.LOC_CT);
            if (sensorHits > 1) {
                mods.addModifier(4, "attacker sensors badly damaged");
            } else if (sensorHits > 0) {
                mods.addModifier(2, "attacker sensors damaged");
            }
        } else if (sensorHits > 0) {
            mods.addModifier(2, "attacker sensors damaged");
        }

        return mods;
    }

    /**
     * Determines if the current target is a secondary target, and if so,
     * returns the appropriate modifier.
     *
     * @return The secondary target modifier.
     * @author Ben
     */
    public static ToHitData getSecondaryTargetMod(IGame game, Entity attacker, Targetable target) {
        return Compute.getSecondaryTargetMod(game, attacker, target, false);
    }

    public static ToHitData getSecondaryTargetMod(IGame game, Entity attacker, Targetable target, boolean isSwarm) {

        //large craft do not get secondary target mod
        //http://www.classicbattletech.com/forums/index.php/topic,37661.0.html
        if((attacker instanceof Dropship) || (attacker instanceof Jumpship)) {
            return null;
        }

        boolean curInFrontArc = Compute.isInArc(attacker.getPosition(), attacker.getSecondaryFacing(), target.getPosition(), ARC_FORWARD);

        int primaryTarget = Entity.NONE;
        for (Enumeration<EntityAction> i = game.getActions(); i.hasMoreElements();) {
            Object o = i.nextElement();
            if (!(o instanceof WeaponAttackAction)) {
                continue;
            }
            WeaponAttackAction prevAttack = (WeaponAttackAction) o;
            if (prevAttack.getEntityId() == attacker.getId()) {
                // first front arc target is our primary.
                // if first target is non-front, and either a later target or
                // the current one is in front, use that instead.
                Targetable pte = game.getTarget(prevAttack.getTargetType(), prevAttack.getTargetId());
                // in double blind play, we might not have the target in our
                // local copy of the game. In that case, the sprite won't
                // have the correct to-hit number, but at least we don't crash
                if (pte == null) {
                    continue;
                }
                // When targeting a stealthed Mech, you can _only_ target it,
                // not anything else (BMRr, pg. 147)
                if ((pte instanceof Mech) && ((Entity) pte).isStealthActive() && (pte != target) && !isSwarm) {
                    return new ToHitData(TargetRoll.IMPOSSIBLE, "When targeting a stealthed Mech, can not attack secondary targets");
                }
                if (Compute.isInArc(attacker.getPosition(), attacker.getSecondaryFacing(), pte.getPosition(), ARC_FORWARD)) {
                    primaryTarget = prevAttack.getTargetId();
                    break;
                } else if ((primaryTarget == Entity.NONE) && !curInFrontArc) {
                    primaryTarget = prevAttack.getTargetId();
                }
            }
        }

        if ((primaryTarget == Entity.NONE) || (primaryTarget == target.getTargetId())) {
            // current target is primary target
            return null; // no modifier
        }

        // current target is secondary

        // Infantry can't attack secondary targets, but BA can (TW, page 109).
        if ((attacker instanceof Infantry) && !(attacker instanceof BattleArmor)) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "Can't have multiple targets.");
        }

        // Stealthed Mechs can't be secondary targets (BMRr, pg. 147)
        if ((target instanceof Mech) && ((Entity) target).isStealthActive()) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "Can't target Mech with active stealth armor as secondary target");
        }

        if (curInFrontArc || (attacker instanceof BattleArmor)) {
            return new ToHitData(1, "secondary target modifier");
        }
        return new ToHitData(2, "secondary target modifier");
    }

    /**
     * Damage that a mech does with a accidental fall from above.
     */

    public static int getAffaDamageFor(Entity entity) {
        return (int) entity.getWeight() / 10;
    }

    /**
     * Modifier to attacks due to attacker movement
     */
    public static ToHitData getAttackerMovementModifier(IGame game, int entityId) {
        return Compute.getAttackerMovementModifier(game, entityId, game.getEntity(entityId).moved);
    }

    /**
     * Modifier to attacks due to attacker movement
     */
    public static ToHitData getAttackerMovementModifier(IGame game, int entityId, int movement) {
        final Entity entity = game.getEntity(entityId);
        ToHitData toHit = new ToHitData();

        // infantry aren't affected by their own movement.
        if (entity instanceof Infantry) {
            return toHit;
        }

        if ((entity.getMovementMode() == IEntityMovementMode.BIPED_SWIM) || (entity.getMovementMode() == IEntityMovementMode.QUAD_SWIM)) {
            return toHit;
        }

        if ((movement == IEntityMovementType.MOVE_WALK) || (movement == IEntityMovementType.MOVE_VTOL_WALK) || (movement == IEntityMovementType.MOVE_CAREFUL_STAND)) {
            toHit.addModifier(1, "attacker walked");
        } else if ((movement == IEntityMovementType.MOVE_RUN) || (movement == IEntityMovementType.MOVE_VTOL_RUN)) {
            toHit.addModifier(2, "attacker ran");
        } else if (movement == IEntityMovementType.MOVE_SKID) {
            toHit.addModifier(3, "attacker ran and skidded");
        } else if (movement == IEntityMovementType.MOVE_JUMP) {
            toHit.addModifier(3, "attacker jumped");
        } else if (movement == IEntityMovementType.MOVE_OVER_THRUST) {
            toHit.addModifier(2, "over thrust used");
        }

        return toHit;
    }

    /**
     * Modifier to attacks due to spotter movement
     */
    public static ToHitData getSpotterMovementModifier(IGame game, int entityId) {
        return Compute.getSpotterMovementModifier(game, entityId, game.getEntity(entityId).moved);
    }

    /**
     * Modifier to attacks due to spotter movement
     */
    public static ToHitData getSpotterMovementModifier(IGame game, int entityId, int movement) {
        ToHitData toHit = new ToHitData();

        Entity e = game.getEntity(entityId);
        if ((e != null) && (e instanceof Infantry)) {
            return toHit;
        }

        if ((movement == IEntityMovementType.MOVE_WALK) || (movement == IEntityMovementType.MOVE_VTOL_WALK)) {
            toHit.addModifier(1, "spotter walked");
        } else if ((movement == IEntityMovementType.MOVE_RUN) || (movement == IEntityMovementType.MOVE_VTOL_RUN) || (movement == IEntityMovementType.MOVE_SKID)) {
            toHit.addModifier(2, "spotter ran");
        } else if (movement == IEntityMovementType.MOVE_JUMP) {
            toHit.addModifier(3, "spotter jumped");
        }

        return toHit;
    }

    /**
     * Modifier to physical attack BTH due to pilot advantages
     */
    public static void modifyPhysicalBTHForAdvantages(Entity attacker, Entity target, ToHitData toHit, IGame game) {

        if (attacker.getCrew().getOptions().booleanOption("melee_specialist") && (attacker instanceof Mech) && (Compute.getAttackerMovementModifier(game, attacker.getId()).getValue() > 0)) {
            toHit.addModifier(-1, "melee specialist");
        }

        if (attacker.getCrew().getOptions().booleanOption("clan_pilot_training")) {
            toHit.addModifier(1, "clan pilot training");
        }

        // Mek targets that are dodging are harder to hit.

        if ((target != null) && (target instanceof Mech) && target.getCrew().getOptions().booleanOption("dodge_maneuver") && (target.dodging )) {
            toHit.addModifier(2, "target is dodging");
        }
    }

    /**
     * Modifier to attacks due to target movement
     */
    public static ToHitData getTargetMovementModifier(IGame game, int entityId) {
        Entity entity = game.getEntity(entityId);

        if(entity instanceof Aero) {
            return new ToHitData();
        }

    if (game.getOptions().booleanOption("tacops_standing_still") &&
            (entity.moved==IEntityMovementType.MOVE_NONE) &&
            !entity.isImmobile() &&
            !((entity instanceof Infantry) || (entity instanceof VTOL) ||
              (entity instanceof GunEmplacement))) {
            ToHitData toHit = new ToHitData();
            toHit.addModifier(-1, "target didn't move");
            return toHit;
        }

        ToHitData toHit = Compute.getTargetMovementModifier(entity.delta_distance, ((entity.moved == IEntityMovementType.MOVE_JUMP) || (entity.moved == IEntityMovementType.MOVE_VTOL_RUN) || (entity.moved == IEntityMovementType.MOVE_VTOL_WALK)), (entity.moved == IEntityMovementType.MOVE_VTOL_RUN) || (entity.moved == IEntityMovementType.MOVE_VTOL_WALK) || (entity.getMovementMode() == IEntityMovementMode.VTOL));

        // Did the target skid this turn?
        if (entity.moved == IEntityMovementType.MOVE_SKID) {
            toHit.addModifier(2, "target skidded");
        }
        if ((entity.getElevation() > 0) && (entity.getMovementMode() == IEntityMovementMode.WIGE)) {
            toHit.addModifier(1, "target is a flying WiGE");
        }

        return toHit;
    }

    /**
     * Target movement modifer for the specified delta_distance
     */

    public static ToHitData getTargetMovementModifier(int distance, boolean jumped, boolean isVTOL) {
        ToHitData toHit = new ToHitData();
        if (distance == 0) {
            return toHit;
        }

        if ((distance >= 3) && (distance <= 4)) {
            toHit.addModifier(1, "target moved 3-4 hexes");
        } else if ((distance >= 5) && (distance <= 6)) {
            toHit.addModifier(2, "target moved 5-6 hexes");
        } else if ((distance >= 7) && (distance <= 9)) {
            toHit.addModifier(3, "target moved 7-9 hexes");
        } else if ((distance >= 10) && (distance <= 17)) {
            toHit.addModifier(4, "target moved 10-17 hexes");
        } else if ((distance >= 18) && (distance <= 24)) {
            toHit.addModifier(5, "target moved 18-24 hexes");
        } else if (distance >= 25) {
            toHit.addModifier(6, "target moved 25+ hexes");
        }

        if (jumped) {
            if (isVTOL) {
                toHit.addModifier(1, "target VTOL used MPs");
            } else {
                toHit.addModifier(1, "target jumped");
            }
        }

        return toHit;
    }

    /**
     * Modifier to attacks due to attacker terrain
     */
    public static ToHitData getAttackerTerrainModifier(IGame game, int entityId) {
        final Entity attacker = game.getEntity(entityId);
        final IHex hex = game.getBoard().getHex(attacker.getPosition());

        ToHitData toHit = new ToHitData();

        //space screens; bonus depends on number (level)
        if(hex.terrainLevel(Terrains.SCREEN) > 0) {
            toHit.addModifier(hex.terrainLevel(Terrains.SCREEN) + 1, "attacker in screen(s)");
        }

        return toHit;
    }

    /**
     * Modifier to attacks due to target terrain TODO:um....should VTOLs get
     * modifiers for smoke, etc.
     */
    public static ToHitData getTargetTerrainModifier(IGame game, Targetable t) {
        return Compute.getTargetTerrainModifier(game, t, 0);
    }

    public static ToHitData getTargetTerrainModifier(IGame game, Targetable t, int eistatus) {
        return Compute.getTargetTerrainModifier(game, t, eistatus, false);
    }

    public static ToHitData getTargetTerrainModifier(IGame game, Targetable t, int eistatus, boolean attackerInSameBuilding) {
        Entity entityTarget = null;
        IHex hex = game.getBoard().getHex(t.getPosition());
        if (t.getTargetType() == Targetable.TYPE_ENTITY) {
            entityTarget = (Entity) t;
            if (hex == null) {
                entityTarget.setPosition(game.getEntity(entityTarget.getId()).getPosition());
                hex = game.getBoard().getHex(game.getEntity(entityTarget.getId()).getPosition());
            }
        }

        boolean isAboveWoods = ((entityTarget != null) && (hex != null)) && (entityTarget.absHeight() >= 2);
        boolean isAboveSmoke = ((entityTarget != null) && (hex != null)) && (entityTarget.absHeight() >= 3);
        ToHitData toHit = new ToHitData();

        // if we have in-building combat, it's a +1
        if (attackerInSameBuilding) {
            toHit.addModifier(1, "target in a building hex");
        }

        // Smoke and woods. With L3, the effects STACK.
        int woodsLevel = hex.terrainLevel(Terrains.WOODS);
        int jungleLevel = hex.terrainLevel(Terrains.JUNGLE);
        String woodsText = "woods";
        if (woodsLevel < jungleLevel) {
            woodsLevel = jungleLevel;
            woodsText = "jungle";
        }
        if (woodsLevel == 1) {
            woodsText = "target in light " + woodsText;
        } else if (woodsLevel == 2) {
            woodsText = "target in heavy " + woodsText;
        } else if (woodsLevel == 3) {
            woodsText = "target in ultra heavy " + woodsText;
        }

        if (!game.getOptions().booleanOption("tacops_woods_cover") && !isAboveWoods && !((t.getTargetType() == Targetable.TYPE_HEX_CLEAR) || (t.getTargetType() == Targetable.TYPE_HEX_IGNITE) || (t.getTargetType() == Targetable.TYPE_HEX_BOMB) || (t.getTargetType() == Targetable.TYPE_HEX_ARTILLERY) || (t.getTargetType() == Targetable.TYPE_MINEFIELD_DELIVER))) {
            if ((woodsLevel == 1) && (eistatus != 2)) {
                toHit.addModifier(1, woodsText);
            } else if (woodsLevel > 1) {
                if (eistatus > 0) {
                    toHit.addModifier(woodsLevel - 1, woodsText);
                } else {
                    toHit.addModifier(woodsLevel, woodsText);
                }
            }
        }
        if (!isAboveSmoke) {
            if (hex.terrainLevel(Terrains.SMOKE) == 1) {
                toHit.addModifier(1, "target in light smoke");
            } else if (hex.terrainLevel(Terrains.SMOKE) > 1) {
                if (eistatus > 0) {
                    toHit.addModifier(1, "target in heavy smoke");
                } else {
                    toHit.addModifier(2, "target in heavy smoke");
                }
            }

        }
        if (hex.terrainLevel(Terrains.GEYSER) == 2) {
            if (eistatus > 0) {
                toHit.addModifier(1, "target in erupting geyser");
            } else {
                toHit.addModifier(2, "target in erupting geyser");
            }
        }

        if(hex.containsTerrain(Terrains.INDUSTRIAL)) {
            toHit.addModifier(+1, "target in heavy industrial zone");
        }
        //space screens; bonus depends on number (level)
        if(hex.terrainLevel(Terrains.SCREEN) > 0) {
            toHit.addModifier(hex.terrainLevel(Terrains.SCREEN) + 1, "target in screen(s)");
        }

        // only entities get remaining terrain bonuses
        // TODO: should this be changed for buildings???
        if (entityTarget == null) {
            return toHit;
        } else if (entityTarget.isMakingDfa()) {
            // you don't get terrain modifiers in midair
            // should be abstracted more into a 'not on the ground'
            // flag for vtols and such
            return toHit;
        }

        if (entityTarget.isStuck()) {
            toHit.addModifier(-2, "target stuck in swamp");
        }
        if((entityTarget instanceof Infantry) && hex.containsTerrain(Terrains.FIELDS)) {
            toHit.addModifier(+1, "target in planted fields");
        }
        return toHit;
    }

    /**
     * Returns the weapon attack out of a list that has the highest expected
     * damage
     */
    public static WeaponAttackAction getHighestExpectedDamage(IGame g, Vector<WeaponAttackAction> vAttacks, boolean assumeHit) {
        float fHighest = -1.0f;
        WeaponAttackAction waaHighest = null;
        for (int x = 0, n = vAttacks.size(); x < n; x++) {
            WeaponAttackAction waa = vAttacks.elementAt(x);
            float fDanger = Compute.getExpectedDamage(g, waa, assumeHit);
            if (fDanger > fHighest) {
                fHighest = fDanger;
                waaHighest = waa;
            }
        }
        return waaHighest;
    }

    // store these as constants since the tables will never change
    private static float[] expectedHitsByRackSize = { 0.0f, 1.0f, 1.58f, 2.0f, 2.63f, 3.17f, 4.0f, 4.49f, 4.98f, 5.47f, 6.31f, 7.23f, 8.14f, 8.59f, 9.04f, 9.5f, 10.1f, 10.8f, 11.42f, 12.1f, 12.7f };

    /*
     * | No Modifier | +2 (Artemis, Narc) | -2 (HAG, AMS v Art)| -4 (AMS) | |
     * Avg | Avg | Avg | Avg | | Hits Pct | Hits Pct | Hits Pct | Hits Pct | |
     * Avg Per vs | Avg Per vs | Avg Per vs | Avg Per vs | Size| Hits Size Avg |
     * Hits Size Avg | Hits Size Avg | Hits Size Avg |
     * ----+--------------------+--------------------+--------------------+--------------------+
     * 2 | 1.42 0.708 9.1 | 1.72 0.861 10.3 | 1.17 0.583 10.7 | 1.03 0.514 21.9 |
     * 3 | 2.00 0.667 2.7 | 2.39 0.796 2.0 | 1.61 0.537 2.0 | 1.28 0.426 1.0 | 4 |
     * 2.64 0.660 1.6 | 3.11 0.778 -0.4 | 2.11 0.528 0.2 | 1.67 0.417 -1.2 | 5 |
     * 3.17 0.633 -2.5 | 3.83 0.767 -1.8 | 2.50 0.500 -5.1 | 1.86 0.372 -11.7 |
     * 6 | 4.00 0.667 2.7 | 4.78 0.796 2.0 | 3.22 0.537 2.0 | 2.58 0.431 2.1 | 7 |
     * 4.39 0.627 -3.4 | 5.42 0.774 -0.9 | 3.47 0.496 -5.8 | 2.69 0.385 -8.7 | 8 |
     * 5.08 0.635 -2.1 | 6.06 0.757 -3.0 | 4.22 0.528 0.2 | 3.58 0.448 6.2 | 9 |
     * 5.47 0.608 -6.4 | 6.69 0.744 -4.7 | 4.47 0.497 -5.7 | 3.69 0.410 -2.7 |
     * 10 | 6.31 0.631 -2.9 | 7.67 0.767 -1.8 | 5.06 0.506 -4.0 | 3.97 0.397
     * -5.8 | 11 | 7.31 0.664 2.3 | 8.67 0.788 0.9 | 6.06 0.551 4.5 | 4.97 0.452
     * 7.2 | 12 | 8.14 0.678 4.5 | 9.64 0.803 2.9 | 6.64 0.553 5.0 | 5.25 0.438
     * 3.7 | 13 | 8.42 0.647 -0.3 | 10.22 0.786 0.7 | 6.72 0.517 -1.8 | 5.25
     * 0.404 -4.2 | 14 | 9.22 0.659 1.5 | 10.92 0.780 -0.1 | 7.64 0.546 3.6 |
     * 6.25 0.446 5.9 | 15 | 9.50 0.633 -2.5 | 11.50 0.767 -1.8 | 7.72 0.515
     * -2.3 | 6.25 0.417 -1.2 | 16 | 10.42 0.651 0.3 | 12.50 0.781 0.1 | 8.44
     * 0.528 0.2 | 6.67 0.417 -1.2 | 17 | 10.69 0.629 -3.1 | 13.08 0.770 -1.4 |
     * 8.53 0.502 -4.8 | 6.67 0.392 -7.0 | 18 | 11.50 0.639 -1.6 | 13.78 0.765
     * -1.9 | 9.44 0.525 -0.4 | 7.67 0.426 1.0 | 19 | 11.78 0.620 -4.5 | 14.36
     * 0.756 -3.2 | 9.53 0.501 -4.8 | 7.67 0.404 -4.3 | 20 | 12.69 0.635 -2.2 |
     * 15.36 0.768 -1.6 | 10.25 0.512 -2.7 | 8.08 0.404 -4.2 | 21 | 13.61 0.648
     * -0.2 | 16.33 0.778 -0.4 | 11.11 0.529 0.4 | 8.94 0.426 1.0 | 22 | 14.44
     * 0.657 1.1 | 17.31 0.787 0.8 | 11.69 0.532 0.9 | 9.22 0.419 -0.6 | 23 |
     * 15.36 0.668 2.9 | 18.31 0.796 2.0 | 12.42 0.540 2.5 | 9.64 0.419 -0.6 |
     * 24 | 16.28 0.678 4.5 | 19.28 0.803 2.9 | 13.28 0.553 5.0 | 10.50 0.438
     * 3.7 | 25 | 16.56 0.662 2.0 | 19.86 0.794 1.8 | 13.36 0.534 1.5 | 10.50
     * 0.420 -0.4 | 26 | 17.36 0.668 2.8 | 20.56 0.791 1.3 | 14.28 0.549 4.2 |
     * 11.50 0.442 4.9 | 27 | 17.64 0.653 0.6 | 21.14 0.783 0.3 | 14.36 0.532
     * 1.0 | 11.50 0.426 1.0 | 28 | 17.92 0.640 -1.4 | 21.72 0.776 -0.6 | 14.44
     * 0.516 -2.1 | 11.50 0.411 -2.6 | 29 | 18.72 0.646 -0.6 | 22.42 0.773 -1.0 |
     * 15.36 0.530 0.6 | 12.50 0.431 2.2 | 30 | 19.00 0.633 -2.5 | 23.00 0.767
     * -1.8 | 15.44 0.515 -2.3 | 12.50 0.417 -1.2 | 40 | 25.39 0.635 -2.2 |
     * 30.72 0.768 -1.6 | 20.50 0.512 -2.7 | 16.17 0.404 -4.2 | ----- -----
     * ----- ----- Average: 0.649 0.781 0.527 0.422 1.202 0.811 0.649
     */

    /**
     * Determines the expected damage of a weapon attack, based on to-hit, salvo
     * sizes, etc.
     */
    public static float getExpectedDamage(IGame g, WeaponAttackAction waa, boolean assumeHit) {
        boolean use_table = false;

        AmmoType loaded_ammo = new AmmoType();

        Entity attacker = g.getEntity(waa.getEntityId());
        Infantry inf_attacker = new Infantry();
        BattleArmor ba_attacker = new BattleArmor();
        Mounted weapon = attacker.getEquipment(waa.getWeaponId());
        Mounted lnk_guide;

        ToHitData hitData = waa.toHit(g);

        if (attacker instanceof BattleArmor) {
            ba_attacker = (BattleArmor) g.getEntity(waa.getEntityId());
        }
        if ((attacker instanceof Infantry) && !(attacker instanceof BattleArmor)) {
            inf_attacker = (Infantry) g.getEntity(waa.getEntityId());
        }

        WeaponType wt = (WeaponType) weapon.getType();

        float fDamage = 0.0f;
        float fChance = 0.0f;
        if (assumeHit) {
            fChance = 1.0f;
        } else {
            if ((hitData.getValue() == TargetRoll.IMPOSSIBLE) || (hitData.getValue() == TargetRoll.AUTOMATIC_FAIL)) {
                return 0.0f;
            }

            if (hitData.getValue() == TargetRoll.AUTOMATIC_SUCCESS) {
                fChance = 1.0f;
            } else {
                fChance = (float) Compute.oddsAbove(hitData.getValue()) / 100.0f;
            }
        }

        // Missiles, LBX cluster rounds, and ultra/rotary cannons (when spun up)
        // use the missile hits table
        if (wt.getDamage() == WeaponType.DAMAGE_MISSILE) {
            use_table = true;
        }
        if ((wt.getAmmoType() == AmmoType.T_AC_LBX) || (wt.getAmmoType() == AmmoType.T_AC_LBX_THB) || (wt.getAmmoType() == AmmoType.T_AC_LBX_THB)) {
            loaded_ammo = (AmmoType) weapon.getLinked().getType();
            if (((loaded_ammo.getAmmoType() == AmmoType.T_AC_LBX) || (loaded_ammo.getAmmoType() == AmmoType.T_AC_LBX_THB)) && (loaded_ammo.getMunitionType() == AmmoType.M_CLUSTER)) {
                use_table = true;
            }
        }

        if ((wt.getAmmoType() == AmmoType.T_AC_ULTRA) || (wt.getAmmoType() == AmmoType.T_AC_ULTRA_THB) || (wt.getAmmoType() == AmmoType.T_AC_ROTARY)) {
            if ((weapon.curMode().getName() == "Ultra") || (weapon.curMode().getName() == "2-shot") || (weapon.curMode().getName() == "3-shot") || (weapon.curMode().getName() == "4-shot") || (weapon.curMode().getName() == "5-shot") || (weapon.curMode().getName() == "6-shot")) {
                use_table = true;
            }
        }

        // Kinda cheap, but lets use the missile hits table for Battle armor
        // weapons too

        if (attacker instanceof BattleArmor) {
            if ((wt.getInternalName() != Infantry.SWARM_MEK) && (wt.getInternalName() != Infantry.LEG_ATTACK)) {
                use_table = true;
            }
        }

        if (use_table == true) {
            if (!(attacker instanceof BattleArmor)) {
                if (weapon.getLinked() == null) {
                    return 0.0f;
                }
            }
            AmmoType at = null;
            if (weapon.getLinked() != null) {
                at = (AmmoType) weapon.getLinked().getType();
                fDamage = at.getDamagePerShot();
            }

            float fHits = 0.0f;
            if ((wt.getRackSize() != 40) && (wt.getRackSize() != 30)) {
                fHits = expectedHitsByRackSize[wt.getRackSize()];
            } else {
                fHits = 2.0f * expectedHitsByRackSize[wt.getRackSize() / 2];
            }
            if (((wt.getAmmoType() == AmmoType.T_SRM_STREAK) || (wt.getAmmoType() == AmmoType.T_MRM_STREAK) || (wt.getAmmoType() == AmmoType.T_LRM_STREAK)) && !Compute.isAffectedByAngelECM(attacker, attacker.getPosition(), waa.getTarget(g).getPosition())) {
                fHits = wt.getRackSize();
            }
            if ((wt.getAmmoType() == AmmoType.T_AC_ULTRA) || (wt.getAmmoType() == AmmoType.T_AC_ULTRA_THB) || (wt.getAmmoType() == AmmoType.T_AC_ROTARY)) {
                if ((weapon.curMode().getName() == "Ultra") || (weapon.curMode().getName() == "2-shot")) {
                    fHits = expectedHitsByRackSize[2];
                }
                if (weapon.curMode().getName() == "3-shot") {
                    fHits = expectedHitsByRackSize[3];
                }
                if (weapon.curMode().getName() == "4-shot") {
                    fHits = expectedHitsByRackSize[4];
                }
                if (weapon.curMode().getName() == "5-shot") {
                    fHits = expectedHitsByRackSize[5];
                }
                if (weapon.curMode().getName() == "6-shot") {
                    fHits = expectedHitsByRackSize[6];
                }
            }

            // Most Battle Armor units have a weapon per trooper, plus their
            // weapons do odd things when mounting multiples
            if (attacker instanceof BattleArmor) {
                // The number of troopers hitting
                fHits = expectedHitsByRackSize[ba_attacker.getShootingStrength()];
                if (wt.getDamage() == WeaponType.DAMAGE_MISSILE) {
                    fHits *= expectedHitsByRackSize[wt.getRackSize()];
                }
                if (wt.getDamage() != WeaponType.DAMAGE_MISSILE) {
                    if (wt.getDamage() != WeaponType.DAMAGE_VARIABLE) {
                        fDamage = wt.getDamage();
                    } else {
                        fDamage = wt.getRackSize();
                    }
                }
                if (wt.hasFlag(WeaponType.F_MISSILE_HITS)) {
                    fHits *= expectedHitsByRackSize[wt.getRackSize()];
                }
            }

            // If there is no ECM coverage to the target, guidance systems are
            // good for another 1.20x damage on missile weapons
            if ((!Compute.isAffectedByECM(attacker, attacker.getPosition(), g.getEntity(waa.getTargetId()).getPosition())) && (wt.getDamage() == WeaponType.DAMAGE_MISSILE)) {
                // Check for linked artemis guidance system
                if ((wt.getAmmoType() == AmmoType.T_LRM) || (wt.getAmmoType() == AmmoType.T_MML) || (wt.getAmmoType() == AmmoType.T_SRM)) {
                    lnk_guide = weapon.getLinkedBy();
                    if ((lnk_guide != null) && (lnk_guide.getType() instanceof MiscType) && !lnk_guide.isDestroyed() && !lnk_guide.isMissing() && !lnk_guide.isBreached() && lnk_guide.getType().hasFlag(MiscType.F_ARTEMIS)) {

                        // Don't use artemis if this is indirect fire
                        // -> Hook for Artemis V Level 3 Clan tech here; use
                        // 1.30f multiplier when implemented
                        if (((weapon.curMode() == null) || !weapon.curMode().equals("Indirect")) && (at.getMunitionType() == AmmoType.M_ARTEMIS_CAPABLE)) {
                            fHits *= 1.2f;
                        }
                    }
                }

                // Check for ATMs, which have built in Artemis
                if (wt.getAmmoType() == AmmoType.T_ATM) {
                    fHits *= 1.2f;
                }

                // Check for target with attached Narc or iNarc homing pod from
                // friendly unit
                if (g.getEntity(waa.getTargetId()).isNarcedBy(attacker.getOwner().getTeam()) || g.getEntity(waa.getTargetId()).isINarcedBy(attacker.getOwner().getTeam())) {
                    if (((at.getAmmoType() == AmmoType.T_LRM) || (at.getAmmoType() == AmmoType.T_MML) || (at.getAmmoType() == AmmoType.T_SRM)) && (at.getMunitionType() == AmmoType.M_NARC_CAPABLE)) {
                        fHits *= 1.2f;
                    }
                }
            }

            if (wt.getAmmoType() == AmmoType.T_MRM) {
                lnk_guide = weapon.getLinkedBy();
                if ((lnk_guide != null) && (lnk_guide.getType() instanceof MiscType) && !lnk_guide.isDestroyed() && !lnk_guide.isMissing() && !lnk_guide.isBreached() && lnk_guide.getType().hasFlag(MiscType.F_APOLLO)) {
                    fHits *= .9f;
                }
            }


            // adjust for previous AMS
            if (wt.getDamage() == WeaponType.DAMAGE_MISSILE) {
                ArrayList<Mounted> vCounters = waa.getCounterEquipment();
                if (vCounters != null) {
                    for (int x = 0; x < vCounters.size(); x++) {
                        EquipmentType type = vCounters.get(x).getType();
                        if ((type instanceof WeaponType) && type.hasFlag(WeaponType.F_AMS)) {
                            fHits *= 0.6;
                        }
                    }
                }
            }

            fDamage *= fHits;

            if ((wt.getAmmoType() == AmmoType.T_AC_ULTRA) || (wt.getAmmoType() == AmmoType.T_AC_ULTRA_THB) || (wt.getAmmoType() == AmmoType.T_AC_ROTARY)) {
                fDamage = fHits * wt.getDamage();
            }

        } else {
            // Direct fire weapons (and LBX slug rounds) just do a single shot
            // so they don't use the missile hits table
            fDamage = wt.getDamage();
            if ((attacker.getPosition() != null) && (g.getEntity(waa.getTargetId()).getPosition() != null)) {
                if (wt.getAmmoType() == AmmoType.T_GAUSS_HEAVY) {
                    fDamage = 25.0f;
                    int rtt = attacker.getPosition().distance(g.getEntity(waa.getTargetId()).getPosition());
                    if (rtt > 13) {
                        fDamage = 10.0f;
                    } else if (rtt > 6) {
                        fDamage = 20.0f;
                    }
                }
            }

            // Infantry follow some special rules, but do fixed amounts of
            // damage
            // Anti-mek attacks are weapon-like in nature, so include them here
            // as well
            if (attacker instanceof Infantry) {
                if (wt.getInternalName() == Infantry.LEG_ATTACK) {
                    fDamage = 10.0f; // Actually 5, but the chance of crits
                    // deserves a boost
                }

                if (inf_attacker.isPlatoon()) {
                    if (wt.getInternalName() == Infantry.SWARM_MEK) {
                        // If the target is a Mek that is not swarmed, this is a
                        // good thing
                        if ((g.getEntity(waa.getTargetId()).getSwarmAttackerId() == Entity.NONE) && (g.getEntity(waa.getTargetId()) instanceof Mech)) {
                            /*
                             * fDamage = 1.5f * inf_attacker
                             * .getDamage(inf_attacker .getShootingStrength());
                             */
                            // TODO: Fix me
                            fDamage = 4;
                        }
                        // Otherwise, call it 0 damage
                        else {
                            fDamage = 0.0f;
                        }
                    }

                    else {
                        // conventional weapons; field guns should be handled
                        // under the standard weapons section
                        /*
                         * fDamage = 0.6f * inf_attacker.getDamage(inf_attacker
                         * .getShootingStrength());
                         */
                        // TODO: Fix me
                        fDamage = 2;
                    }

                } else {
                    // Battle armor units conducting swarm attack
                    if (wt.getInternalName() == Infantry.SWARM_MEK) {
                        // If the target is a Mek that is not swarmed, this is a
                        // good thing
                        if ((g.getEntity(waa.getTargetId()).getSwarmAttackerId() == Entity.NONE) && (g.getEntity(waa.getTargetId()) instanceof Mech)) {
                            // Overestimated, but the chance at crits and head
                            // shots deserves a boost
                            fDamage = 5.0f * ba_attacker.getShootingStrength();
                        }
                        // Otherwise, call it 0 damage
                        else {
                            fDamage = 0.0f;
                        }
                    }

                }
            }

        }
        fDamage *= fChance;

        // Conventional infantry take double damage in the open
        if ((g.getEntity(waa.getTargetId()) instanceof Infantry) && !(g.getEntity(waa.getTargetId()) instanceof BattleArmor)) {
            IHex e_hex = g.getBoard().getHex(g.getEntity(waa.getTargetId()).getPosition().x, g.getEntity(waa.getTargetId()).getPosition().y);
            if (!e_hex.containsTerrain(Terrains.WOODS) && !e_hex.containsTerrain(Terrains.JUNGLE) && !e_hex.containsTerrain(Terrains.BUILDING)) {
                fDamage *= 2.0f;
            }

            // Cap damage to prevent run-away values
            fDamage = Math.min(inf_attacker.getShootingStrength(), fDamage);
        }
        return fDamage;
    }

    /**
     * If the unit is carrying multiple types of ammo for the specified weapon,
     * cycle through them and choose the type best suited to engage the
     * specified target Value returned is expected damage Note that some ammo
     * types, such as infernos, do no damage or have special properties and so
     * the damage is an estimation of effectiveness
     */

    public static double getAmmoAdjDamage(IGame cgame, WeaponAttackAction atk) {
        boolean no_bin = true;
        boolean multi_bin = false;

        double ammo_multiple, ex_damage, max_damage;

        Entity shooter, target;

        Mounted fabin, best_bin;
        AmmoType abin_type = new AmmoType();
        AmmoType fabin_type = new AmmoType();
        WeaponType wtype = new WeaponType();
        WeaponType target_weapon = new WeaponType();

        // Get shooter entity, target entity, and weapon being fired
        target = cgame.getEntity(atk.getTargetId());
        shooter = atk.getEntity(cgame);
        wtype = (WeaponType) shooter.getEquipment(atk.getWeaponId()).getType();

        max_damage = 0.0;

        // If the weapon doesn't require ammo, just get the estimated damage
        if (wtype.hasFlag(WeaponType.F_ENERGY) || wtype.hasFlag(WeaponType.F_ONESHOT) || wtype.hasFlag(WeaponType.F_INFANTRY) || (wtype.getAmmoType() == AmmoType.T_NA)) {
            return Compute.getExpectedDamage(cgame, atk, false);
        }

        // Get a list of ammo bins and the first valid bin
        fabin = null;
        best_bin = null;

        for (Mounted abin : shooter.getAmmo()) {
            if (shooter.loadWeapon(shooter.getEquipment(atk.getWeaponId()), abin)) {
                if (abin.getShotsLeft() > 0) {
                    abin_type = (AmmoType) abin.getType();
                    if (!AmmoType.canDeliverMinefield(abin_type)) {
                        fabin = abin;
                        fabin_type = (AmmoType) fabin.getType();
                        break;
                    }
                }
            }
        }

        // To save processing time, lets see if we have more than one type of
        // bin
        // Thunder-type ammos and empty bins are excluded from the list
        for (Mounted abin : shooter.getAmmo()) {
            if (shooter.loadWeapon(shooter.getEquipment(atk.getWeaponId()), abin)) {
                if (abin.getShotsLeft() > 0) {
                    abin_type = (AmmoType) abin.getType();
                    if (!AmmoType.canDeliverMinefield(abin_type)) {
                        no_bin = false;
                        if (abin_type.getMunitionType() != fabin_type.getMunitionType()) {
                            multi_bin = true;
                            break;
                        }
                    }
                }
            }
        }

        // If no_bin is true, then either all bins are empty or contain
        // Thunder-type rounds and
        // we can safely say that the expected damage is 0.0
        // If no_bin is false, then we have at least one good bin
        if (no_bin) {
            return 0.0;
        }
        // If multi_bin is true, then multiple ammo types are present and an
        // appropriate type must be selected
        // If multi_bin is false, then all bin types are the same; skip down
        // to getting the expected damage
        if (!multi_bin) {
            return Compute.getExpectedDamage(cgame, atk, false);
        }
        if (multi_bin) {

            // Set default max damage as 0, and the best bin as the first
            // bin
            max_damage = 0.0;
            best_bin = fabin;

            // For each valid ammo bin
            for (Mounted abin : shooter.getAmmo()) {
                if (shooter.loadWeapon(shooter.getEquipment(atk.getWeaponId()), abin)) {
                    if (abin.getShotsLeft() > 0) {
                        abin_type = (AmmoType) abin.getType();
                        if (!AmmoType.canDeliverMinefield(abin_type)) {

                            // Load weapon with specified bin
                            shooter.loadWeapon(shooter.getEquipment(atk.getWeaponId()), abin);
                            atk.setAmmoId(shooter.getEquipmentNum(abin));

                            // Get expected damage
                            ex_damage = Compute.getExpectedDamage(cgame, atk, false);

                            // Calculate any modifiers due to ammo type
                            ammo_multiple = 1.0;

                            // Frag missiles, flechette AC rounds do double
                            // damage against conventional infantry
                            // and 0 damage against everything else
                            // Any further anti-personnel specialized rounds
                            // should be tested for here
                            if (((((abin_type.getAmmoType() == AmmoType.T_LRM) || (abin_type.getAmmoType() == AmmoType.T_MML) || (abin_type.getAmmoType() == AmmoType.T_SRM))) && (abin_type.getMunitionType() == AmmoType.M_FRAGMENTATION)) || (((abin_type.getAmmoType() == AmmoType.T_AC) || (abin_type.getAmmoType() == AmmoType.T_LAC)) && (abin_type.getMunitionType() == AmmoType.M_FLECHETTE))) {
                                ammo_multiple = 0.0;
                                if (target instanceof Infantry) {
                                    if (!(target instanceof BattleArmor)) {
                                        ammo_multiple = 2.0;
                                    }
                                }
                            }

                            // LBX cluster rounds work better against units
                            // with little armor, vehicles, and Meks in
                            // partial cover
                            // Other ammo that deliver lots of small
                            // submunitions should be tested for here too
                            if (((abin_type.getAmmoType() == AmmoType.T_AC_LBX) || (abin_type.getAmmoType() == AmmoType.T_AC_LBX_THB) || (abin_type.getAmmoType() == AmmoType.T_SBGAUSS)) && (abin_type.getMunitionType() == AmmoType.M_CLUSTER)) {
                                if (target.getArmorRemainingPercent() <= 0.25) {
                                    ammo_multiple = 1.0 + (wtype.getRackSize() / 10);
                                }
                                if (target instanceof Tank) {
                                    ammo_multiple += 1.0;
                                }
                            }

                            // AP autocannon rounds work much better against
                            // Meks and vehicles than infantry,
                            // give a damage boost in proportion to calibre
                            // to reflect scaled crit chance
                            // Other armor-penetrating ammo types should be
                            // tested here, such as Tandem-charge SRMs
                            if (((abin_type.getAmmoType() == AmmoType.T_AC) || (abin_type.getAmmoType() == AmmoType.T_LAC)) && (abin_type.getMunitionType() == AmmoType.M_ARMOR_PIERCING)) {
                                if ((target instanceof Mech) || (target instanceof Tank)) {
                                    ammo_multiple = 1.0 + (wtype.getRackSize() / 10);
                                }
                                if (target instanceof Infantry) {
                                    ammo_multiple = 0.6;
                                }
                            }

                            // Inferno SRMs work better against overheating
                            // Meks that are not/almost not on fire,
                            // and against vehicles and protos if allowed by
                            // game option
                            if (((abin_type.getAmmoType() == AmmoType.T_SRM) || (abin_type.getAmmoType() == AmmoType.T_MML)) && (abin_type.getMunitionType() == AmmoType.M_INFERNO)) {
                                ammo_multiple = 0.5;
                                if (target instanceof Mech) {
                                    if ((target.infernos.getTurnsLeftToBurn() < 4) && (target.heat >= 5)) {
                                        ammo_multiple = 1.1;
                                    }
                                }
                                if ((target instanceof Tank) && !(cgame.getOptions().booleanOption("vehicles_safe_from_infernos"))) {
                                    ammo_multiple = 1.1;
                                }
                                if ((target instanceof Protomech) && !(cgame.getOptions().booleanOption("protos_safe_from_infernos"))) {
                                    ammo_multiple = 1.1;
                                }
                            }

                            // Narc beacon doesn't really do damage but if
                            // the target is not infantry and doesn't have
                            // one, give 'em one by making it an attractive
                            // option
                            if ((wtype.getAmmoType() == AmmoType.T_NARC) && (abin_type.getMunitionType() == AmmoType.M_STANDARD)) {
                                if (!(target.isNarcedBy(shooter.getOwner().getTeam())) && !(target instanceof Infantry)) {
                                    ex_damage = 5.0;
                                } else {
                                    ex_damage = 0.5;
                                }
                            }

                            // iNarc beacon doesn't really do damage, but if
                            // the target is not infantry and doesn't have
                            // one, give 'em one by making it an attractive
                            // option
                            if (wtype.getAmmoType() == AmmoType.T_INARC) {
                                if ((abin_type.getMunitionType() == AmmoType.M_STANDARD) && !(target instanceof Infantry)) {
                                    if (!(target.isINarcedBy(shooter.getOwner().getTeam()))) {
                                        ex_damage = 7.0;
                                    } else {
                                        ex_damage = 1.0;
                                    }
                                }

                                // iNarc ECM doesn't really do damage, but
                                // if the target has a C3 link or missile
                                // launchers
                                // make it a priority
                                // Checking for actual ammo types carried
                                // would be nice, but can't be sure of exact
                                // loads
                                // when "true" double blind is implemented
                                if ((abin_type.getAmmoType() == AmmoType.T_INARC) && (abin_type.getMunitionType() == AmmoType.M_ECM) && !(target instanceof Infantry)) {
                                    if (!target.isINarcedWith(AmmoType.M_ECM)) {
                                        if (!(target.getC3MasterId() == Entity.NONE) || target.hasC3M() || target.hasC3MM() || target.hasC3i()) {
                                            ex_damage = 8.0;
                                        } else {
                                            ex_damage = 0.5;
                                        }
                                        for (Mounted weapon : shooter.getWeaponList()) {
                                            target_weapon = (WeaponType) weapon.getType();
                                            if ((target_weapon.getAmmoType() == AmmoType.T_LRM) || (target_weapon.getAmmoType() == AmmoType.T_MML) || (target_weapon.getAmmoType() == AmmoType.T_SRM)) {
                                                ex_damage = ex_damage + (target_weapon.getRackSize() / 2);
                                            }
                                        }
                                    }
                                }

                                // iNarc Nemesis doesn't really do damage,
                                // but if the target is not infantry and
                                // doesn't have
                                // one give it a try; make fast units a
                                // priority because they are usually out
                                // front
                                if ((abin_type.getAmmoType() == AmmoType.T_INARC) && (abin_type.getMunitionType() == AmmoType.M_NEMESIS) && !(target instanceof Infantry)) {
                                    if (!target.isINarcedWith(AmmoType.M_NEMESIS)) {
                                        ex_damage = (double) (target.getWalkMP() + target.getJumpMP()) / 2;
                                    } else {
                                        ex_damage = 0.5;
                                    }
                                }
                            }

                            // If the adjusted damage is highest, store the
                            // damage and bin
                            if ((ex_damage * ammo_multiple) > max_damage) {
                                max_damage = ex_damage * ammo_multiple;
                                best_bin = abin;
                            }
                        }
                    }
                }
            }

            // Now that the best bin has been found, reload the weapon with
            // it
            shooter.loadWeapon(shooter.getEquipment(atk.getWeaponId()), best_bin);
            atk.setAmmoId(shooter.getEquipmentNum(best_bin));
        }
        return max_damage;
    }

    /**
     * If this is an ultra or rotary cannon, lets see about 'spinning it up' for
     * extra damage
     *
     * @return the <code>int</code> ID of weapon mode
     */

    public static int spinUpCannon(IGame cgame, WeaponAttackAction atk) {

        int threshold = 12;
        int test, final_spin;
        Entity shooter;
        Mounted weapon;
        WeaponType wtype = new WeaponType();

        // Double check this is an Ultra or Rotary cannon
        shooter = atk.getEntity(cgame);
        weapon = shooter.getEquipment(atk.getWeaponId());
        wtype = (WeaponType) shooter.getEquipment(atk.getWeaponId()).getType();

        if (!((wtype.getAmmoType() == AmmoType.T_AC_ULTRA) || (wtype.getAmmoType() == AmmoType.T_AC_ULTRA_THB) || (wtype.getAmmoType() == AmmoType.T_AC_ROTARY))) {
            return 0;
        }

        // Get the to-hit number
        threshold = atk.toHit(cgame).getValue();

        // Set the weapon to single shot mode
        weapon.setMode("Single");
        final_spin = 0;

        // If weapon can't hit target, exit the function with the weapon on
        // single shot
        if ((threshold == TargetRoll.IMPOSSIBLE) || (threshold == TargetRoll.AUTOMATIC_FAIL)) {
            return final_spin;
        }

        // Set a random 2d6 roll
        test = Compute.d6(2);

        // If random roll is >= to-hit + 1, then set double-spin
        if (test >= threshold + 1) {
            final_spin = 1;
            if ((wtype.getAmmoType() == AmmoType.T_AC_ULTRA) || (wtype.getAmmoType() == AmmoType.T_AC_ULTRA_THB)) {
                weapon.setMode("Ultra");
            }
            if (wtype.getAmmoType() == AmmoType.T_AC_ROTARY) {
                weapon.setMode("2-shot");
            }
        }

        // If this is a Rotary cannon
        if (wtype.getAmmoType() == AmmoType.T_AC_ROTARY) {

            // If random roll is >= to-hit + 2 then set to quad-spin
            if (test >= threshold + 2) {
                final_spin = 2;
                weapon.setMode("4-shot");
            }

            // If random roll is >= to-hit + 3 then set to six-spin
            if (test >= threshold + 3) {
                final_spin = 3;
                weapon.setMode("6-shot");
            }
        }
        return final_spin;
    }

    /**
     * Checks to see if a target is in arc of the specified weapon, on the
     * specified entity
     */
    public static boolean isInArc(IGame game, int attackerId, int weaponId, Targetable t) {
        Entity ae = game.getEntity(attackerId);
        if ((ae instanceof Mech) && (((Mech) ae).getGrappled() == t.getTargetId())) {
            return true;
        }
        int facing = ae.isSecondaryArcWeapon(weaponId) ? ae.getSecondaryFacing() : ae.getFacing();
        Coords aPos = ae.getPosition();
        Coords tPos = t.getPosition();

        //aeros in the same hex in space may still be able to fire at one another. First I need to translate
        //their positions to see who was further back
        if(game.getBoard().inSpace() && ae.getPosition().equals(t.getPosition())
                && (ae instanceof Aero) && (t instanceof Aero)) {
            if(((Aero)ae).shouldMoveBackHex((Aero)t)) {
                aPos = ae.getPriorPosition();
            }
            if(((Aero)t).shouldMoveBackHex((Aero)ae)) {
                tPos = ((Entity)t).getPosition();
            }
        }
        return Compute.isInArc(aPos, facing, tPos, ae.getWeaponArc(weaponId));
    }

    /**
     * Returns true if the line between source Coords and target goes through
     * the hex in front of the attacker
     */
    public static boolean isThroughFrontHex(IGame game, Coords src, Entity t) {
        Coords dest = t.getPosition();
        int fa = dest.degree(src) - t.getFacing() * 60;
        if (fa < 0) {
            fa += 360;
        }
        return (fa > 330) || (fa < 30);
    }

    /**
     * Returns true if the target is in the specified arc.
     *
     * @param src
     *            the attacker coordinate
     * @param facing
     *            the appropriate attacker sfacing
     * @param dest
     *            the target coordinate
     * @param arc
     *            the arc
     */
    public static boolean isInArc(Coords src, int facing, Coords dest, int arc) {
        if ((src == null) || (dest == null)) {
            return true;
        }
        // calculate firing angle
        int fa = src.degree(dest) - facing * 60;
        if (fa < 0) {
            fa += 360;
        }
        // is it in the specifed arc?
        switch (arc) {
        case ARC_FORWARD:
            return (fa >= 300) || (fa <= 60);
        case Compute.ARC_RIGHTARM:
            return (fa >= 300) || (fa <= 120);
        case Compute.ARC_LEFTARM:
            return (fa >= 240) || (fa <= 60);
        case ARC_REAR:
            return (fa > 120) && (fa < 240);
        case ARC_RIGHTSIDE:
            return (fa > 60) && (fa <= 120);
        case ARC_LEFTSIDE:
            return (fa < 300) && (fa >= 240);
        case ARC_MAINGUN:
            return (fa >= 240) || (fa <= 120);
        case ARC_360:
            return true;
        case ARC_NORTH:
            return (fa >= 270) || (fa <= 30);
        case ARC_EAST:
            return (fa >= 30) && (fa <= 150);
        case ARC_WEST:
            return (fa >= 150) && (fa <= 270);
        case ARC_NOSE:
            return (fa > 300) || (fa < 60);
        case ARC_LWING:
            return (fa > 300) || (fa <= 0);
        case ARC_RWING:
            return (fa >= 0) && (fa < 60);
        case ARC_LWINGA:
            return (fa >= 180) && (fa < 240);
        case ARC_RWINGA:
            return (fa > 120) && (fa <= 180);
        case ARC_AFT:
            return (fa > 120) && (fa < 240);
        case ARC_LEFTSIDE_SPHERE:
            return (fa > 240) || (fa < 0);
        case ARC_RIGHTSIDE_SPHERE:
            return (fa > 0) && (fa < 120);
        case ARC_LEFTSIDEA_SPHERE:
            return (fa > 180) && (fa < 300);
        case ARC_RIGHTSIDEA_SPHERE:
            return (fa > 60) && (fa < 180);
        case ARC_LEFT_BROADSIDE:
            return (fa >= 240) && (fa <= 300);
        case ARC_RIGHT_BROADSIDE:
            return (fa >= 60) && (fa <= 120);
        case ARC_LEFT_SPHERE_GROUND:
            return (fa >= 180) && (fa < 360);
        case ARC_RIGHT_SPHERE_GROUND:
            return (fa >= 0) && (fa < 180);
        case ARC_TURRET:
            return (fa >= 330) || (fa <= 30);
        default:
            return false;
        }
    }

    /**
     * checks to see whether the target is within visual range of the entity, but not necessarily LoS
     */
    public static boolean inVisualRange(IGame game, Entity ae, Targetable target) {
        boolean teSpotlight = false;
        if (target.getTargetType() == Targetable.TYPE_ENTITY) {
            Entity te = (Entity) target;
            teSpotlight = te.usedSearchlight();
            if (te.isOffBoard()) {
                return false;
            }
        }

        //if either does not have a position then return false
        if((ae.getPosition() == null) || (target.getPosition() == null)) {
            return false;
        }

        //check visual range based on planetary conditions
        int visualRange = game.getPlanetaryConditions().getVisualRange(ae, teSpotlight);

        //smoke in los
        visualRange -= LosEffects.calculateLos(game, ae.getId(), target).getLightSmoke();
        visualRange -= (2 * LosEffects.calculateLos(game, ae.getId(), target).getHeavySmoke());

        //check for camo and null sig on the target
        if(target.getTargetType() == Targetable.TYPE_ENTITY) {
            Entity te = (Entity) target;

            if ( game.getTeamForPlayer(ae.getOwner()).equals(game.getTeamForPlayer(te.getOwner())) ){
                return true;
            }

            if(te.isVoidSigActive()) {
                visualRange = visualRange / 4;
            } else if(te.hasWorkingMisc(MiscType.F_VISUAL_CAMO, -1)) {
                visualRange = visualRange / 2;
            } else if(te.isChameleonShieldActive()) {
                visualRange = visualRange / 2;
            }
        }

        visualRange = Math.max(visualRange, 1);

        return ae.getPosition().distance(target.getPosition()) <= visualRange;

    }

    /**
     * Checks to see whether the target is within sensor range (but not necessarily LoS or visual range)
     */
    public static boolean inSensorRange(IGame game, Entity ae, Targetable target) {

        //if either does not have a position then return false
        if((ae.getPosition() == null) || (target.getPosition() == null)) {
            return false;
        }

        int bracket = Compute.getSensorRangeBracket(ae, target);
        int range = Compute.getSensorRangeByBracket(game, ae, target);

        int maxSensorRange = bracket*range;
        int minSensorRange = Math.max((bracket-1)*range,0);
        if(game.getOptions().booleanOption("inclusive_sensor_range")) {
            minSensorRange = 0;
        }

        int distance = ae.getPosition().distance(target.getPosition());

        return (distance > minSensorRange) && (distance <= maxSensorRange);
    }

    /**
     * Slightly misnamed. Checks to see if the target is visible to the unit, either visually or through sensors
     */
    public static boolean canSee(IGame game, Entity ae, Targetable target) {

        if(!ae.getCrew().isActive()) {
            return false;
        }
        if (target.isOffBoard()) {
            return false;
        }

        return (LosEffects.calculateLos(game, ae.getId(), target).canSee() && Compute.inVisualRange(game, ae, target)) || Compute.inSensorRange(game, ae, target);
    }

    private static int getSensorRangeBracket(Entity ae, Targetable target) {

        Sensor sensor = ae.getActiveSensor();
        if(null == sensor) {
            return 0;
        }
        //only works for entities
        if(target.getTargetType() != Targetable.TYPE_ENTITY) {
            return 0;
        }
        Entity te = (Entity)target;

        //if this sensor is an active probe and it is critted, then no can see
        if(sensor.isBAP() && !ae.hasBAP(false)) {
            return 0;
        }

        int check = ae.getSensorCheck();
        check += sensor.getModsForStealth(te);
        //ECM bubbles
        check += sensor.getModForECM(ae);

        //get the range bracket (0 - none; 1 - short; 2 - medium; 3 - long)
        int bracket = 0;
        if((check == 7) || (check == 8)) {
            bracket = 1;
        }
        if((check == 5) || (check == 6)) {
            bracket = 2;
        }
        if(check < 5) {
            bracket = 3;
        }

        return bracket;
    }
    /**
     * Checks whether the target is within sensor range of the current entity
     */
    private static int getSensorRangeByBracket(IGame game, Entity ae, Targetable target) {

        Sensor sensor = ae.getActiveSensor();
        if(null == sensor) {
            return 0;
        }
        //only works for entities
        if(target.getTargetType() != Targetable.TYPE_ENTITY) {
            return 0;
        }
        Entity te = (Entity)target;

        //if this sensor is an active probe and it is critted, then no can see
        if(sensor.isBAP() && !ae.hasBAP(false)) {
            return 0;
        }

        //if we are crossing water then only magscan will work unless we are a naval vessel
        if(LosEffects.calculateLos(game, ae.getId(), target).isBlockedByWater()
                && (sensor.getType() != Sensor.TYPE_MEK_MAGSCAN) && (sensor.getType() != Sensor.TYPE_VEE_MAGSCAN)
                && (ae.getMovementMode() != IEntityMovementMode.HYDROFOIL) && (ae.getMovementMode() != IEntityMovementMode.NAVAL)) {
            return 0;
        }

        //now get the range
        int range = sensor.getRangeByBracket();

        //adjust the range based on LOS and planetary conditions
        range = sensor.adjustRange(range, game, LosEffects.calculateLos(game, ae.getId(), target));

        //now adjust for anything about the target entity (size, heat, etc)
        range = sensor.entityAdjustments(range, te, game);

        if(range < 0) {
            range = 0;
        }

        return range;

    }

    public static int targetSideTable(Coords inPosition, Targetable target) {
        return target.sideTable(inPosition);
    }

    public static int targetSideTable(Entity attacker, Targetable target) {
        Coords attackPos = attacker.getPosition();

        boolean usePrior = false;
        //aeros in the same hex in space need to adjust position to get side table
        if(attacker.game.getBoard().inSpace() && attacker.getPosition().equals(target.getPosition())
                && (attacker instanceof Aero) && (target instanceof Aero)) {
            if(((Aero)attacker).shouldMoveBackHex((Aero)target)) {
                attackPos = attacker.getPriorPosition();
            }
            usePrior = ((Aero)target).shouldMoveBackHex((Aero)attacker);
        }
        if((target instanceof Aero) && (attacker instanceof Aero)) {
            return ((Entity)target).sideTable(attackPos, usePrior);
        }
        return target.sideTable(attackPos);
    }

    /**
     * Maintain backwards compatability.
     *
     * @param missiles -
     *            the <code>int</code> number of missiles in the pack.
     */
    public static int missilesHit(int missiles) {
        return Compute.missilesHit(missiles, 0);
    }

    /**
     * Maintain backwards compatability.
     * @param missiles
     * @param nMod
     * @return
     */
    public static int missilesHit(int missiles, int nMod) {
        return Compute.missilesHit(missiles, nMod, false);
    }

    /**
     * Maintain backwards compatability.
     * @param missiles
     * @param nMod
     * @param hotloaded
     * @return
     */
    public static int missilesHit(int missiles, int nMod, boolean hotloaded) {
        return Compute.missilesHit(missiles, nMod, hotloaded, false, false);
    }

    /**
     * Roll the number of missiles (or whatever) on the missile hit table, with
     * the specified mod to the roll.
     *
     * @param missiles -
     *            the <code>int</code> number of missiles in the pack.
     * @param nMod -
     *            the <code>int</code> modifier to the roll for number of
     *            missiles that hit.
     * @param hotloaded -
     *            roll 3d6 take worst 2
     * @param streak -
     *            force a roll of 11 on the cluster table
     * @param advancedAMS -
     *            the roll can now go below 2, indicating no damage
     */
    public static int missilesHit(int missiles, int nMod, boolean hotloaded, boolean streak, boolean advancedAMS) {
        int nRoll = Compute.d6(2);

        if (hotloaded) {
            int roll1 = Compute.d6();
            int roll2 = Compute.d6();
            int roll3 = Compute.d6();
            int lowRoll1 = 0;
            int lowRoll2 = 0;

            if ((roll1 <= roll2) && (roll1 <= roll3)) {
                lowRoll1 = roll1;
                lowRoll2 = Math.min(roll2, roll3);
            } else if ((roll2 <= roll1) && (roll2 <= roll3)) {
                lowRoll1 = roll2;
                lowRoll2 = Math.min(roll1, roll3);
            } else if ((roll3 <= roll1) && (roll3 <= roll2)) {
                lowRoll1 = roll3;
                lowRoll2 = Math.min(roll2, roll1);
            }
            nRoll = lowRoll1 + lowRoll2;
        }
        if (streak) {
            nRoll = 11;
        }
        nRoll += nMod;
        if (!advancedAMS) {
            nRoll = Math.min(Math.max(nRoll, 2), 12);
        } else {
            nRoll=Math.min(nRoll, 12);
        }
        if (nRoll<2) {
            return 0;
        }

        for (int[] element : clusterHitsTable) {
            if (element[0] == missiles) {
                return element[nRoll - 1];
            }
        }
        // BA missiles may have larger number of missiles than max entry on the
        // table
        // if so, take largest, subtract value and try again
        for (int i = clusterHitsTable.length - 1; i >= 0; i--) {
            if (missiles > clusterHitsTable[i][0]) {
                return clusterHitsTable[i][nRoll - 1] + Compute.missilesHit(missiles - clusterHitsTable[i][0], nMod, hotloaded, streak, advancedAMS);
            }
        }
        throw new RuntimeException("Could not find number of missiles in hit table");
    }

    /**
     * Returns the consciousness roll number
     *
     * @param hit -
     *            the <code>int</code> number of the crew hit currently being
     *            rolled.
     * @return The <code>int</code> number that must be rolled on 2d6 for the
     *         crew to stay conscious.
     */
    public static int getConsciousnessNumber(int hit) {
        switch (hit) {
        case 0:
            return 2;
        case 1:
            return 3;
        case 2:
            return 5;
        case 3:
            return 7;
        case 4:
            return 10;
        case 5:
            return 11;
        default:
            return Integer.MAX_VALUE;
        }
    }

    /**
     * This method checks to see if a line from a to b is affected by an ECM
     * field of the enemy of ae
     *
     * @param ae
     * @param a
     * @param b
     * @return
     */
    public static boolean isAffectedByECM(Entity ae, Coords a, Coords b) {
        return Compute.getECMFieldSize(ae, a, b) > 0;
    }

    /**
     * This method returns the highest number of enemy ECM fields of ae between points a and b
     *
     * @param ae
     * @param a
     * @param b
     * @return
     */
    public static double getECMFieldSize(Entity ae, Coords a, Coords b) {
        if(ae.getGame().getBoard().inSpace()) {
            //normal ECM effects don't apply in space
            return 0;
        }
        if ((a == null) || (b == null)) {
            return 0;
        }

        // Only grab enemies with active ECM
        Vector<Coords> vEnemyECMCoords = new Vector<Coords>(16);
        Vector<Integer> vEnemyECMRanges = new Vector<Integer>(16);
        Vector<Double> vEnemyECMStrengths = new Vector<Double>(16);
        Vector<Coords> vFriendlyECCMCoords = new Vector<Coords>(16);
        Vector<Integer> vFriendlyECCMRanges = new Vector<Integer>(16);
        Vector<Double> vFriendlyECCMStrengths = new Vector<Double>(16);
        for (Enumeration<Entity> e = ae.game.getEntities(); e.hasMoreElements();) {
            Entity ent = e.nextElement();
            Coords entPos = ent.getPosition();
            if (ent.isEnemyOf(ae) && ent.hasActiveECM() && (entPos != null)) {
                vEnemyECMCoords.addElement(entPos);
                vEnemyECMRanges.addElement(new Integer(ent.getECMRange()));
                vEnemyECMStrengths.addElement(new Double(ent.getECMStrength()));
            }
            if (!ent.isEnemyOf(ae) && ent.hasActiveECCM() && (entPos != null)) {
                vFriendlyECCMCoords.addElement(entPos);
                vFriendlyECCMRanges.addElement(new Integer(ent.getECMRange()));
                vFriendlyECCMStrengths.addElement(new Double(ent.getECCMStrength()));
            }

            // Check the ECM effects of the entity's passengers.
            for (Entity other : ent.getLoadedUnits()) {
                if (other.isEnemyOf(ae) && other.hasActiveECM() && (entPos != null)) {
                    vEnemyECMCoords.addElement(entPos);
                    vEnemyECMRanges.addElement(new Integer(other.getECMRange()));
                    vEnemyECMStrengths.addElement(new Double(other.getECMStrength()));
                }
                if (!other.isEnemyOf(ae) && ent.hasActiveECCM() && (entPos != null)) {
                    vFriendlyECCMCoords.addElement(entPos);
                    vFriendlyECCMRanges.addElement(new Integer(ent.getECMRange()));
                    vFriendlyECCMStrengths.addElement(new Double(ent.getECCMStrength()));
                }
            }
        }

        // none? get out of here
        if (vEnemyECMCoords.size() == 0) {
            return 0;
        }

        // get intervening Coords.
        ArrayList<Coords> coords = Coords.intervening(a, b);
        // loop through all intervening coords, check each if they are ECM
        // affected
        double worstECM = 0;
        for (Coords c : coords) {
            // > 0: in friendly ECCM
            // 0: unaffected by enemy ECM
            // <0: affected by enemy ECM
            double ecmStatus = 0;
            // if we're at ae's Position, figure in a possible
            // iNarc ECM pod
            if (c.equals(ae.getPosition()) && ae.isINarcedWith(INarcPod.ECM)) {
                ecmStatus++;
            }
            // first, subtract 1 for each enemy ECM that affects us
            Enumeration<Integer> ranges = vEnemyECMRanges.elements();
            Enumeration<Double> strengths = vEnemyECMStrengths.elements();
            for (Coords enemyECMCoords : vEnemyECMCoords) {
                int range = ranges.nextElement().intValue();
                int nDist = c.distance(enemyECMCoords);
                double strength = strengths.nextElement().doubleValue();
                if (nDist <= range) {
                    ecmStatus += strength;
                }
            }
            // now, add one for each friendly ECCM
            ranges = vFriendlyECCMRanges.elements();
            strengths = vFriendlyECCMStrengths.elements();
            for (Coords friendlyECCMCoords : vFriendlyECCMCoords) {
                int range = ranges.nextElement().intValue();
                int nDist = c.distance(friendlyECCMCoords);
                double strength = strengths.nextElement().doubleValue();
                if (nDist <= range) {
                    ecmStatus -= strength;
                }
            }
            // if any coords in the line are affected, the whole line is
            if (ecmStatus > worstECM) {
                worstECM = ecmStatus;
            }
        }
        return worstECM;
    }

    /**
     *  This method checks to see if a line from a to b is affected by an Angel
     *  ECM field of the enemy of ae
     *
     * @param ae
     * @param a
     * @param b
     * @return count that shows if you are in an friendly ECCM field positive
     *         number means you are in an friendly ECCM field Negative number
     *         means you are in a enemy ECM field 0 means you are not effect by
     *         enemy or friendly fields.
     */
    public static boolean isAffectedByAngelECM(Entity ae, Coords a, Coords b) {
        return Compute.getAngelECMFieldSize(ae, a, b) > 0;
    }

    public static double getAngelECMFieldSize(Entity ae, Coords a, Coords b) {
        if(ae.getGame().getBoard().inSpace()) {
            //normal Angel ECM effects don't apply in space
            return 0;
        }
        if ((a == null) || (b == null)) {
            return 0;
        }

        // Only grab enemies with active angel ECM
        Vector<Coords> vEnemyAngelECMCoords = new Vector<Coords>(16);
        Vector<Integer> vEnemyAngelECMRanges = new Vector<Integer>(16);
        Vector<Double> vEnemyAngelECMStrengths = new Vector<Double>(16);
        Vector<Coords> vFriendlyECCMCoords = new Vector<Coords>(16);
        Vector<Integer> vFriendlyECCMRanges = new Vector<Integer>(16);
        Vector<Double> vFriendlyECCMStrengths = new Vector<Double>(16);
        for (Enumeration<Entity> e = ae.game.getEntities(); e.hasMoreElements();) {
            Entity ent = e.nextElement();
            Coords entPos = ent.getPosition();
            // add each angel ECM at its ECM strength
            if (ent.isEnemyOf(ae) && ent.hasActiveAngelECM() && (entPos != null)) {
                vEnemyAngelECMCoords.addElement(entPos);
                vEnemyAngelECMRanges.addElement(new Integer(ent.getECMRange()));
                vEnemyAngelECMStrengths.add(ent.getECMStrength());
            }
            if (!ent.isEnemyOf(ae) && ent.hasActiveECCM() && (entPos != null)) {
                vFriendlyECCMCoords.addElement(entPos);
                vFriendlyECCMRanges.addElement(new Integer(ent.getECMRange()));
                vFriendlyECCMStrengths.add(ent.getECCMStrength());
            }

            // Check the angel ECM effects of the entity's passengers.
            for (Entity other : ent.getLoadedUnits()) {
                if (other.isEnemyOf(ae) && other.hasActiveAngelECM() && (entPos != null)) {
                    vEnemyAngelECMCoords.addElement(entPos);
                    vEnemyAngelECMRanges.addElement(new Integer(other.getECMRange()));
                    vEnemyAngelECMStrengths.add(ent.getECMStrength());
                }
                if (!other.isEnemyOf(ae) && ent.hasActiveECCM() && (entPos != null)) {
                    vFriendlyECCMCoords.addElement(entPos);
                    vFriendlyECCMRanges.addElement(new Integer(ent.getECMRange()));
                    vFriendlyECCMStrengths.add(ent.getECMStrength());
                }
            }
        }

        // none? get out of here
        if (vEnemyAngelECMCoords.size() == 0) {
            return 0;
        }

        // get intervening Coords.
        ArrayList<Coords> coords = Coords.intervening(a, b);
        // loop through all intervening coords, check each if they are ECM
        // affected
        double worstECM = 0;
        for (Coords c : coords) {
            // > 0: in friendly ECCM
            // 0: unaffected by enemy ECM
            // <0: affected by enemy angel ECM
            double ecmStatus = 0;
            // first, subtract 2 for each enemy angel ECM that affects us
            Enumeration<Integer> ranges = vEnemyAngelECMRanges.elements();
            Enumeration<Double> strengths = vEnemyAngelECMStrengths.elements();
            for (Coords enemyECMCoords : vEnemyAngelECMCoords) {
                int range = ranges.nextElement().intValue();
                int nDist = c.distance(enemyECMCoords);
                double strength = strengths.nextElement().doubleValue();
                if (nDist <= range) {
                    ecmStatus += strength;
                }
            }
            // now, add one for each friendly ECCM
            ranges = vFriendlyECCMRanges.elements();
            strengths = vFriendlyECCMStrengths.elements();
            for (Coords friendlyECCMCoords : vFriendlyECCMCoords) {
                int range = ranges.nextElement().intValue();
                int nDist = c.distance(friendlyECCMCoords);
                double strength = strengths.nextElement().doubleValue();

                if (nDist <= range) {
                    ecmStatus -= strength;
                }
            }
            // if any coords in the line are affected, the whole line is
            if (ecmStatus > worstECM) {
                worstECM = ecmStatus;
            }
        }
        return worstECM;
    }

    /**
     * Check for ECM bubbles in Ghost Target mode along the path from a to b and return the highest
     * target roll. -1 if no Ghost Targets
     */
    public static int getGhostTargetNumber(Entity ae, Coords a, Coords b) {
        if(ae.getGame().getBoard().inSpace()) {
            //ghost targets don't work in space
            return 0;
        }
        if ((a == null) || (b == null)) {
            return 0;
        }

        // Only grab enemies with active ECM
        //need to create two hashtables for ghost targeting, one with mods
        //and one with booleans indicating that this ghost target was intersected
        //the keys will be the entity id
        Hashtable<Integer, Boolean> hEnemyGTCrossed = new Hashtable<Integer, Boolean>();
        Hashtable<Integer, Integer> hEnemyGTMods = new Hashtable<Integer, Integer>();
        Vector<Coords> vEnemyECMCoords = new Vector<Coords>(16);
        Vector<Integer> vEnemyECMRanges = new Vector<Integer>(16);
        Vector<Double> vEnemyECMStrengths = new Vector<Double>(16);
        Vector<Coords> vEnemyGTCoords = new Vector<Coords>(16);
        Vector<Integer> vEnemyGTRanges = new Vector<Integer>(16);
        Vector<Integer> vEnemyGTId = new Vector<Integer>(16);
        Vector<Coords> vFriendlyECCMCoords = new Vector<Coords>(16);
        Vector<Integer> vFriendlyECCMRanges = new Vector<Integer>(16);
        Vector<Double> vFriendlyECCMStrengths = new Vector<Double>(16);
        for (Enumeration<Entity> e = ae.game.getEntities(); e.hasMoreElements();) {
            Entity ent = e.nextElement();
            Coords entPos = ent.getPosition();
            if (ent.isEnemyOf(ae) && ent.hasGhostTargets(true) && (entPos != null)) {
                vEnemyGTCoords.addElement(entPos);
                vEnemyGTRanges.addElement(new Integer(ent.getECMRange()));
                vEnemyGTId.addElement(new Integer(ent.getId()));
                hEnemyGTCrossed.put(ent.getId(), false);
                hEnemyGTMods.put(ent.getId(), ent.getGhostTargetRollMoS());
            }
            if (ent.isEnemyOf(ae) && ent.hasActiveECM() && (entPos != null)) {
                vEnemyECMCoords.addElement(entPos);
                vEnemyECMRanges.addElement(new Integer(ent.getECMRange()));
                vEnemyECMStrengths.add(ent.getECMStrength());
            }
            if (!ent.isEnemyOf(ae) && ent.hasActiveECCM() && (entPos != null)) {
                vFriendlyECCMCoords.addElement(entPos);
                vFriendlyECCMRanges.addElement(new Integer(ent.getECMRange()));
                vFriendlyECCMStrengths.add(ent.getECCMStrength());
            }

            // Check the ECM effects of the entity's passengers.
            for (Entity other : ent.getLoadedUnits()) {
                if (other.isEnemyOf(ae) && other.hasGhostTargets(true) && (entPos != null)) {
                    vEnemyGTCoords.addElement(entPos);
                    vEnemyGTRanges.addElement(new Integer(other.getECMRange()));
                    vEnemyGTId.addElement(new Integer(ent.getId()));
                    hEnemyGTCrossed.put(ent.getId(), false);
                    hEnemyGTMods.put(ent.getId(), ent.getGhostTargetRollMoS());
                }
                if (other.isEnemyOf(ae) && other.hasActiveECM() && (entPos != null)) {
                    vEnemyECMCoords.addElement(entPos);
                    vEnemyECMRanges.addElement(new Integer(other.getECMRange()));
                    vEnemyECMStrengths.add(ent.getECMStrength());
                }
                if (!other.isEnemyOf(ae) && ent.hasActiveECCM() && (entPos != null)) {
                    vFriendlyECCMCoords.addElement(entPos);
                    vFriendlyECCMRanges.addElement(new Integer(ent.getECMRange()));
                    vFriendlyECCMStrengths.add(ent.getECCMStrength());
                }
            }
        }

        // none? get out of here
        if (vEnemyGTCoords.size() == 0) {
            return -1;
        }

        // get intervening Coords.
        ArrayList<Coords> coords = Coords.intervening(a, b);
        // loop through all intervening coords, if they are not eccm'ed by the enemy then add any Ghost Targets
        //to the hashlist
        for (Coords c : coords) {
            // < 0: in friendly ECCM
            // 0: unaffected by enemy ECM
            // >0: affected by enemy ECM
            int ecmStatus = 0;
            // first, subtract 1 for each enemy ECM that affects us
            Enumeration<Integer> ranges = vEnemyECMRanges.elements();
            Enumeration<Double> strengths = vEnemyECMStrengths.elements();
            for (Coords enemyECMCoords : vEnemyECMCoords) {
                int range = ranges.nextElement().intValue();
                int nDist = c.distance(enemyECMCoords);
                double strength = strengths.nextElement().doubleValue();
                if (nDist <= range) {
                    ecmStatus += strength;
                }
            }
            // now, add one for each friendly ECCM
            ranges = vFriendlyECCMRanges.elements();
            strengths = vFriendlyECCMStrengths.elements();
            for (Coords friendlyECCMCoords : vFriendlyECCMCoords) {
                int range = ranges.nextElement().intValue();
                int nDist = c.distance(friendlyECCMCoords);
                double strength = strengths.nextElement().doubleValue();
                if (nDist <= range) {
                    ecmStatus -= strength;
                }
            }

            if(ecmStatus >= 0) {
                //find any new Ghost Targets that we have crossed
                ranges = vEnemyGTRanges.elements();
                Enumeration<Integer> ids = vEnemyGTId.elements();
                for (Coords enemyGTCoords : vEnemyGTCoords) {
                    int range = ranges.nextElement().intValue();
                    int id = ids.nextElement().intValue();
                    int nDist = c.distance(enemyGTCoords);
                    if ((nDist <= range) && !hEnemyGTCrossed.get(id)) {
                        hEnemyGTCrossed.put(id, true);
                    }
                }
            }
        }

        //ok so now we have a hashtable that tells us which Ghost Targets have been crossed
        //lets loop through that and identify the highest bonus and count the total number crossed
        int totalGT = 0;
        int highestMod = -1;
        Enumeration<Integer> ids = hEnemyGTCrossed.keys();
        while(ids.hasMoreElements()) {
            int id = ids.nextElement();
            if(hEnemyGTCrossed.get(id)) {
                if(hEnemyGTMods.get(id) > highestMod) {
                    highestMod = hEnemyGTMods.get(id);
                } else {
                    totalGT++;
                }
            }
        }
        return highestMod + totalGT;
    }

    /**
     * Check for the total number of fighter/small craft ECM bubbles in space along the path from a to b
     */
    public static int getSmallCraftECM(Entity ae, Coords a, Coords b) {
        if(!ae.getGame().getBoard().inSpace()) {
            //only matters in space
            return 0;
        }
        //Only grab enemies with active ECM
        Vector<Coords> vEnemyECMCoords = new Vector<Coords>(16);
        Vector<Integer> vEnemyECMRanges = new Vector<Integer>(16);
        Vector<Coords> vFriendlyECCMCoords = new Vector<Coords>(16);
        Vector<Integer> vFriendlyECCMRanges = new Vector<Integer>(16);
        Vector<Coords> vFriendlyBAPCoords = new Vector<Coords>(16);
        Vector<Integer> vFriendlyBAPRanges = new Vector<Integer>(16);
        Vector<Integer> vFriendlyBAPFacings = new Vector<Integer>(16);
        for (Enumeration<Entity> e = ae.game.getEntities(); e.hasMoreElements();) {
            Entity ent = e.nextElement();
            Coords entPos = ent.getPosition();
            if((entPos == null) && (ent.getTransportId() != Entity.NONE)) {
                Entity carrier = ae.game.getEntity(ent.getTransportId());
                if((null != carrier) && carrier.loadedUnitsHaveActiveECM()) {
                    entPos = carrier.getPosition();
                }
            }
            if (ent.isEnemyOf(ae) && ent.hasActiveECM() && (entPos != null) && !ent.isLargeCraft()) {
                vEnemyECMCoords.addElement(entPos);
                vEnemyECMRanges.addElement(new Integer(ent.getECMRange()));
            }
            if (!ent.isEnemyOf(ae) && ent.hasActiveECCM() && (entPos != null) && !ent.isLargeCraft()) {
                vFriendlyECCMCoords.addElement(entPos);
                vFriendlyECCMRanges.addElement(new Integer(ent.getECMRange()));
            }
            if(!ent.isEnemyOf(ae) && ent.hasBAP(false) && (entPos != null)) {
                vFriendlyBAPCoords.addElement(entPos);
                vFriendlyBAPRanges.addElement(new Integer(ent.getBAPRange()));
                vFriendlyBAPFacings.addElement(new Integer(ent.getFacing()));
            }

            //TODO: do docked dropships give ECM benefit?
        }

        // none? get out of here
        if (vEnemyECMCoords.size() == 0) {
            return 0;
        }

        // get intervening Coords.
        ArrayList<Coords> coords = Coords.intervening(a, b);
        // loop through all intervening coords, check each if they are ECM
        // affected
        int totalECM = 0;
        //check for split hexes
        boolean bDivided = (a.degree(b) % 60 == 30);
        int x = 0;
        int prevEcmStatus = 0;
        boolean prevEccmPresent = false;
        for (Coords c : coords) {
            int ecmStatus = 0;
            boolean eccmPresent = false;
            // first, subtract 1 for each enemy ECM that affects us
            Enumeration<Integer> ranges = vEnemyECMRanges.elements();
            for (Coords enemyECMCoords : vEnemyECMCoords) {
                int range = ranges.nextElement().intValue();
                int nDist = c.distance(enemyECMCoords);
                if (nDist <= range) {
                    ecmStatus++;
                }
            }
            //now check for friendly eccm
            ranges = vFriendlyECCMRanges.elements();
            for (Coords friendlyECCMCoords : vFriendlyECCMCoords) {
                int range = ranges.nextElement().intValue();
                int nDist = c.distance(friendlyECCMCoords);
                if (nDist <= range) {
                    eccmPresent = true;
                    break;
                }
            }
            //if eccm still not present, check for BAP
            if(!eccmPresent) {
                ranges = vFriendlyBAPRanges.elements();
                Enumeration<Integer> facings = vFriendlyBAPFacings.elements();
                for (Coords friendlyBAPCoords : vFriendlyBAPCoords) {
                    int range = ranges.nextElement().intValue();
                    int nDist = c.distance(friendlyBAPCoords);
                    int facing = facings.nextElement().intValue();
                    if (nDist <= range) {
                        //still might need to check for right arc if using medium range
                        if((range < 7) || Compute.isInArc(friendlyBAPCoords, facing, c, ARC_NOSE)) {
                            eccmPresent = true;
                            break;
                        }
                    }
                }
            }
            // if any coords in the line are affected, the whole line is
            if(!bDivided || (x % 3 == 0)) {
                if ((ecmStatus > 0) && !eccmPresent) {
                    totalECM++;
                }
            } else if ((x % 3 == 2)) {
                //if we are looking at the second split hex then both this one and the prior need to have ECM
                //becaue the advantage should go to the defender
                if ((ecmStatus > 0) && !eccmPresent && (prevEcmStatus > 0) && !prevEccmPresent) {
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
        if(!ae.getGame().getBoard().inSpace()) {
            //only matters in space
            return 0;
        }
        //Only grab enemies with active ECM
        Vector<Coords> vEnemyECMCoords = new Vector<Coords>(16);
        Vector<Integer> vEnemyECMRanges = new Vector<Integer>(16);
        Vector<Coords> vFriendlyECCMCoords = new Vector<Coords>(16);
        Vector<Integer> vFriendlyECCMRanges = new Vector<Integer>(16);
        Vector<Coords> vFriendlyBAPCoords = new Vector<Coords>(16);
        Vector<Integer> vFriendlyBAPRanges = new Vector<Integer>(16);
        Vector<Integer> vFriendlyBAPFacings = new Vector<Integer>(16);
        for (Enumeration<Entity> e = ae.game.getEntities(); e.hasMoreElements();) {
            Entity ent = e.nextElement();
            Coords entPos = ent.getPosition();
            if((entPos == null) && (ent.getTransportId() != Entity.NONE)) {
                Entity carrier = ae.game.getEntity(ent.getTransportId());
                if((null != carrier) && carrier.loadedUnitsHaveActiveECM()) {
                    entPos = carrier.getPosition();
                }
            }
            if (ent.isEnemyOf(ae) && ent.hasActiveECM() && (entPos != null) && ent.isLargeCraft()) {
                vEnemyECMCoords.addElement(entPos);
                vEnemyECMRanges.addElement(new Integer(ent.getECMRange()));
            }
            if (!ent.isEnemyOf(ae) && ent.hasActiveECCM() && (entPos != null) && !ent.isLargeCraft()) {
                vFriendlyECCMCoords.addElement(entPos);
                vFriendlyECCMRanges.addElement(new Integer(ent.getECMRange()));
            }
            if(!ent.isEnemyOf(ae) && ent.hasBAP(false) && (entPos != null)) {
                vFriendlyBAPCoords.addElement(entPos);
                vFriendlyBAPRanges.addElement(new Integer(ent.getBAPRange()));
                vFriendlyBAPFacings.addElement(new Integer(ent.getFacing()));

            }
            //TODO: do docked dropships give ECM benefit?
        }

        // none? get out of here
        if (vEnemyECMCoords.size() == 0) {
            return 0;
        }

        // get intervening Coords.
        ArrayList<Coords> coords = Coords.intervening(a, b);
        // loop through all intervening coords, check each if they are ECM
        // affected
        int totalECM = 0;
        boolean bDivided = (a.degree(b) % 60 == 30);
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
                int range = ranges.nextElement().intValue();
                int nDist = c.distance(enemyECMCoords);
                if (nDist <= range) {
                    ecmStatus++;
                }
            }
            //now check for friendly small craft eccm
            ranges = vFriendlyECCMRanges.elements();
            for (Coords friendlyECCMCoords : vFriendlyECCMCoords) {
                int range = ranges.nextElement().intValue();
                int nDist = c.distance(friendlyECCMCoords);
                if (nDist <= range) {
                    ecmStatus--;
                }
            }
            //now check BAP
            ranges = vFriendlyBAPRanges.elements();
            Enumeration<Integer> facings = vFriendlyBAPFacings.elements();
            for (Coords friendlyBAPCoords : vFriendlyBAPCoords) {
                int range = ranges.nextElement().intValue();
                int nDist = c.distance(friendlyBAPCoords);
                int facing = facings.nextElement().intValue();
                if (nDist <= range) {
                    //still might need to check for right arc if using medium range
                    if((range < 7) || Compute.isInArc(friendlyBAPCoords, facing, c, ARC_NOSE)) {
                        ecmStatus = ecmStatus - 2;
                    }
                }
            }
            // if any coords in the line are affected, the whole line is
            if(!bDivided || (x % 3 == 0)) {
                if (ecmStatus > 0) {
                    totalECM++;
                }
            } else if(x % 3 == 2) {
                //if we are looking at the second split hex then both this one and the prior need to have ECM
                //becaue the advantage should go to the defender
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
     * Get the base to-hit number of a space bomb attack by the given attacker upon the
     * given defender
     *
     * @param attacker -
     *            the <code>Entity</code> conducting the leg attack.
     * @param defender -
     *            the <code>Entity</code> being attacked.
     * @return The base <code>ToHitData</code> of the attack.
     */
    public static ToHitData getSpaceBombBaseToHit(Entity attacker,
            Entity defender, IGame game) {
        int base = TargetRoll.IMPOSSIBLE;
        StringBuffer reason = new StringBuffer();

        if(!(attacker instanceof Aero)) {
            return new ToHitData(base, "attacker is not an Aero");
        }

        Aero a = (Aero)attacker;

        //the fighters nose must be aligned with its direction of travel
        boolean rightFacing = false;
        //using normal movement, I think this means that the last move can't be a turn
        if(!game.useVectorMove()) {
            rightFacing = true;
        }
        //for advanced movement, it must be aligned with largest vector
        if(game.useVectorMove()) {
            for(int h:attacker.getHeading()) {
                if(h == attacker.facing) {
                    rightFacing = true;
                    break;
                }
            }
        }

        boolean canTarget = false;
        Coords attackCoords = null;
        for(Coords c:attacker.getPassedThrough()) {
            for (Enumeration<Entity> e = game.getEntities(c); e.hasMoreElements(); ) {
                Entity target = e.nextElement();
                if(target.getId() == defender.getId()) {
                    canTarget = true;
                }
            }
            if(canTarget) {
                break;
            } else {
                attackCoords = c;
            }
        }
        if(null == attackCoords) {
            attackCoords = attacker.getPosition();
        }

        //must be in control
        if(a.isOutControlTotal()) {
            reason.append("the attacker is out of control");
        }
        else if(a.getSpaceBombs().size() < 1) {
            reason.append("the attacker has no useable bombs");
        } else if(!rightFacing) {
            reason.append("the attacker is not facing the direction of travel");
        }
        //attacker and defender must both be in space hex
        else if(!game.getBoard().getHex(attacker.getPosition()).containsTerrain(Terrains.SPACE)) {
            reason.append("attacker not in space hex");
        } else if(!game.getBoard().getHex(defender.getPosition()).containsTerrain(Terrains.SPACE)) {
            reason.append("defender not in space hex");
        } else if(!canTarget) {
            reason.append("defender is not in hex passed through by attacker this turn");
        }
        //the defender must weight 10000+ tons
        else if(defender.weight < 10000) {
            reason.append("the defender weighs less than 10,000 tons");
        }

        //ok if we are still alive then lets calculate the tohit
        else {
            base = attacker.getCrew().getGunnery();
            reason.append("base");
        }

        ToHitData toHit = new ToHitData(base, reason.toString(), ToHitData.HIT_NORMAL,
                    defender.sideTable(attackCoords));

        toHit.addModifier(+4,"space bomb attack");
        if(attacker.mpUsed > 0) {
            toHit.addModifier(attacker.mpUsed, "attacker thrust");
        }
        if(defender.mpUsed > 0) {
            toHit.addModifier(defender.mpUsed, "defender thrust");
        }
        if((defender instanceof SpaceStation) || (defender.getWalkMP() == 0)) {
            toHit.addModifier(-4, "immobile");
        }
        if(defender.weight < 100000) {
            int penalty = (int)Math.ceil((100000 - defender.weight)/10000);
            toHit.addModifier(penalty, "defender weight");
        }

        return toHit;
    }


    /**
     * Get the base to-hit number of a Leg Attack by the given attacker upon the
     * given defender
     *
     * @param attacker -
     *            the <code>Entity</code> conducting the leg attack.
     * @param defender -
     *            the <code>Entity</code> being attacked.
     * @return The base <code>ToHitData</code> of the attack.
     */
    public static ToHitData getLegAttackBaseToHit(Entity attacker, Entity defender) {
        int men = 0;
        int base = TargetRoll.IMPOSSIBLE;
        StringBuffer reason = new StringBuffer();

        // Can only attack a Mek's legs.
        if (!(defender instanceof Mech)) {
            reason.append("Defender is not a Mek.");
        }

        // Can't attack if flying
        else if (attacker.getElevation()>defender.getElevation()) {
            reason.append("Cannot do leg attack while flying.");
        }
        // Can't target a transported entity.
        else if (Entity.NONE != defender.getTransportId()) {
            reason.append("Target is a passenger.");
        }

        // Can't target a entity conducting a swarm attack.
        else if (Entity.NONE != defender.getSwarmTargetId()) {
            reason.append("Target is swarming a Mek.");
        }

        // Attacker can't be swarming.
        else if (Entity.NONE != attacker.getSwarmTargetId()) {
            reason.append("Attacker is swarming.");
        }

        // Handle BattleArmor attackers.
        else if (attacker instanceof BattleArmor) {
            BattleArmor inf = (BattleArmor) attacker;

            // Battle Armor units can't Leg Attack if they're burdened.
            if (inf.isBurdened()) {
                reason.append("Launcher not jettisoned.");
            } else {
                men = inf.getShootingStrength();
                if (men >= 4) {
                    base = inf.getCrew().getPiloting();
                } else if (men >= 3) {
                    base = inf.getCrew().getPiloting() + 2;
                } else if (men >= 2) {
                    base = inf.getCrew().getPiloting() + 5;
                } else if (men >= 1) {
                    base = inf.getCrew().getPiloting() + 7;
                }
                reason.append(men);
                reason.append(" trooper(s) active");
            }
        } else if (attacker instanceof Infantry) {
            // Non-BattleArmor infantry need many more men.
            Infantry inf = (Infantry) attacker;
            men = inf.getShootingStrength();
            if (men >= 22) {
                base = inf.getCrew().getPiloting();
            } else if (men >= 16) {
                base = inf.getCrew().getPiloting() + 2;
            } else if (men >= 10) {
                base = inf.getCrew().getPiloting() + 5;
            } else if (men >= 5) {
                base = inf.getCrew().getPiloting() + 7;
            }
            reason.append(men);
            reason.append(" men alive");
        } else {
            // No one else can conduct leg attacks.
            reason.append("Attacker is not infantry.");
        }
        ToHitData toReturn = new ToHitData(base, reason.toString(), ToHitData.HIT_KICK, ToHitData.SIDE_FRONT);
        if ((defender instanceof Mech) && ((Mech)defender).isIndustrial()) {
           toReturn.addModifier(-1, "targeting industrial mech");
        }
        // Return the ToHitData for this attack.
        // N.B. we attack the legs.
        return toReturn;
    }

    /**
     * Get the base to-hit number of a Swarm Mek by the given attacker upon the
     * given defender.
     *
     * @param attacker -
     *            the <code>Entity</code> swarming.
     * @param defender -
     *            the <code>Entity</code> being swarmed.
     * @return The base <code>ToHitData</code> of the mek.
     */
    public static ToHitData getSwarmMekBaseToHit(Entity attacker, Entity defender) {
        int men = 0;
        int base = TargetRoll.IMPOSSIBLE;
        StringBuffer reason = new StringBuffer();

        // Can only swarm a Mek.
        if (!(defender instanceof Mech) && !(defender instanceof Tank)) {
            reason.append("Defender is not a Mek or vehicle.");
        }

        // Can't target a transported entity.
        else if (Entity.NONE != defender.getTransportId()) {
            reason.append("Target is a passenger.");
        }

        // Attacker can't be swarming.
        else if (Entity.NONE != attacker.getSwarmTargetId()) {
            reason.append("Attacker is swarming.");
        }

        // Can't target a entity invloved in a swarm attack.
        else if (Entity.NONE != defender.getSwarmAttackerId()) {
            reason.append("Target is already being swarmed.");
        }

        // Can't target a entity conducting a swarm attack.
        else if (Entity.NONE != defender.getSwarmTargetId()) {
            reason.append("Target is swarming a Mek.");
        }

        // Can't swarm a friendly Mek.
        // See http://www.classicbattletech.com/w3t/showflat.php?Cat=&Board=ask&Number=632321&page=0&view=collapsed&sb=5&o=0&fpart=
        else if (!attacker.isEnemyOf(defender) && !attacker.getGame().getOptions().booleanOption("friendly_fire")) {
            reason.append("Can only swarm an enemy.");
        }

        // Handle BattleArmor attackers.
        else if (attacker instanceof BattleArmor) {
            BattleArmor inf = (BattleArmor) attacker;

            // Battle Armor units can't Leg Attack if they're burdened.
            if (inf.isBurdened()) {
                reason.append("Launcher not jettisoned.");
            } else {
                men = inf.getShootingStrength();
                if (men >= 4) {
                    base = inf.getCrew().getPiloting() + 2;
                } else if (men >= 1) {
                    base = inf.getCrew().getPiloting() + 5;
                }
                reason.append(men);
                reason.append(" trooper(s) active");
            }
        }

        // Non-BattleArmor infantry need many more men.
        else if (attacker instanceof Infantry) {
            Infantry inf = (Infantry) attacker;
            men = inf.getShootingStrength();
            if (men >= 22) {
                base = inf.getCrew().getPiloting() + 2;
            } else if (men >= 16) {
                base = inf.getCrew().getPiloting() + 5;
            }
            reason.append(men);
            reason.append(" men alive");
        }

        // No one else can conduct leg attacks.
        else {
            reason.append("Attacker is not infantry.");
        }

        ToHitData toReturn = new ToHitData(base, reason.toString());
        if ((defender instanceof Mech) && ((Mech)defender).isIndustrial()) {
            toReturn.addModifier(-1, "targeting industrial mech");
        }
        // Return the ToHitData for this attack.
        return toReturn;
    }

    public static boolean canPhysicalTarget(IGame game, int entityId, Targetable target) {

        if (PunchAttackAction.toHit(game, entityId, target, PunchAttackAction.LEFT).getValue() != TargetRoll.IMPOSSIBLE) {
            return true;
        }

        if (PunchAttackAction.toHit(game, entityId, target, PunchAttackAction.RIGHT).getValue() != TargetRoll.IMPOSSIBLE) {
            return true;
        }

        if (KickAttackAction.toHit(game, entityId, target, KickAttackAction.LEFT).getValue() != TargetRoll.IMPOSSIBLE) {
            return true;
        }

        if (KickAttackAction.toHit(game, entityId, target, KickAttackAction.RIGHT).getValue() != TargetRoll.IMPOSSIBLE) {
            return true;
        }

        if ((game.getEntity(entityId) instanceof QuadMech) && ((KickAttackAction.toHit(game, entityId, target, KickAttackAction.LEFTMULE).getValue() != TargetRoll.IMPOSSIBLE) || (KickAttackAction.toHit(game, entityId, target, KickAttackAction.RIGHTMULE).getValue() != TargetRoll.IMPOSSIBLE))) {
            return true;
        }

        if (BrushOffAttackAction.toHit(game, entityId, target, BrushOffAttackAction.LEFT).getValue() != TargetRoll.IMPOSSIBLE) {
            return true;
        }

        if (BrushOffAttackAction.toHit(game, entityId, target, BrushOffAttackAction.RIGHT).getValue() != TargetRoll.IMPOSSIBLE) {
            return true;
        }

        if (new ThrashAttackAction(entityId, target).toHit(game).getValue() != TargetRoll.IMPOSSIBLE) {
            return true;
        }

        if (ProtomechPhysicalAttackAction.toHit(game, entityId, target).getValue() != TargetRoll.IMPOSSIBLE) {
            return true;
        }

        if (PushAttackAction.toHit(game, entityId, target).getValue() != TargetRoll.IMPOSSIBLE) {
            return true;
        }

        if (LayExplosivesAttackAction.toHit(game, entityId, target).getValue() != TargetRoll.IMPOSSIBLE) {
            return true;
        }

        if (TripAttackAction.toHit(game, entityId, target).getValue() != TargetRoll.IMPOSSIBLE) {
            return true;
        }

        if (GrappleAttackAction.toHit(game, entityId, target).getValue() != TargetRoll.IMPOSSIBLE) {
            return true;
        }

        if (BreakGrappleAttackAction.toHit(game, entityId, target).getValue() != TargetRoll.IMPOSSIBLE) {
            return true;
        }

        for (Mounted club : game.getEntity(entityId).getClubs()) {
            if (null != club) {
                if (ClubAttackAction.toHit(game, entityId, target, club, ToHitData.HIT_NORMAL).getValue() != TargetRoll.IMPOSSIBLE) {
                    return true;
                }
            }
        }

        if (JumpJetAttackAction.toHit(game, entityId, target, JumpJetAttackAction.BOTH).getValue() != TargetRoll.IMPOSSIBLE) {
            return true;
        }
        if (JumpJetAttackAction.toHit(game, entityId, target, JumpJetAttackAction.LEFT).getValue() != TargetRoll.IMPOSSIBLE) {
            return true;
        }
        if (JumpJetAttackAction.toHit(game, entityId, target, JumpJetAttackAction.RIGHT).getValue() != TargetRoll.IMPOSSIBLE) {
            return true;
        }

        if (BAVibroClawAttackAction.toHit(game, entityId, target).getValue() != TargetRoll.IMPOSSIBLE) {
            return true;
        }

        return false;
    }

    /**
     * Can movement between the two coordinates be on pavement (which includes
     * roads and bridges)? If so it will override prohibited terrain, it may
     * change movement costs, and it may lead to skids.
     *
     * @param game -
     *            the <code>IGame</code> object.
     * @param src -
     *            the <code>Coords</code> being left.
     * @param dest -
     *            the <code>Coords</code> being entered.
     * @param entity -
     *            the <code>Entity</code> that is moving
     * @return <code>true</code> if movement between <code>src</code> and
     *         <code>dest</code> can be on pavement; <code>false</code>
     *         otherwise.
     */
    public static boolean canMoveOnPavement(IGame game, Coords src, Coords dest, MovePath movePath) {
        final IHex srcHex = game.getBoard().getHex(src);
        final IHex destHex = game.getBoard().getHex(dest);
        final int src2destDir = src.direction(dest);
        final int dest2srcDir = (src2destDir + 3) % 6;
        boolean result = false;

        // We may be moving in the same hex.
        if (src.equals(dest) && (srcHex.containsTerrain(Terrains.PAVEMENT) || srcHex.containsTerrain(Terrains.ROAD) || srcHex.containsTerrain(Terrains.BRIDGE))) {
            result = true;
        }

        // If the source is a pavement hex, then see if the destination
        // hex is also a pavement hex or has a road or bridge that exits
        // into the source hex and the entity is climbing onto the bridge.
        else if (srcHex.containsTerrain(Terrains.PAVEMENT)
                && (destHex.containsTerrain(Terrains.PAVEMENT)
                   || destHex.containsTerrainExit(Terrains.ROAD,dest2srcDir)
                   || (destHex.containsTerrainExit(Terrains.BRIDGE, dest2srcDir)
                      && movePath.getFinalClimbMode()))) {
            result = true;
        }

        // See if the source hex has a road or bridge (and the entity is on the
        // bridge) that exits into the destination hex, and the dest hex has
        // pavement or a corresponding exit to the src hex
        else if ((srcHex.containsTerrainExit(Terrains.ROAD, src2destDir)
                 || (srcHex.containsTerrainExit(Terrains.BRIDGE, src2destDir)
                    && (movePath.getLastStep().getElevation() == srcHex.terrainLevel(Terrains.BRIDGE_ELEV))))
                && (destHex.containsTerrainExit(Terrains.ROAD, dest2srcDir)
                   || (destHex.containsTerrainExit(Terrains.BRIDGE, dest2srcDir)
                      && movePath.getFinalClimbMode())
                   || destHex.containsTerrain(Terrains.PAVEMENT))) {
            result = true;
        }

        return result;
    }

    /**
     * Determines whether the attacker and the target are in the same building.
     *
     * @return true if the target can and does occupy the same building, false
     *         otherwise.
     */
    public static boolean isInSameBuilding(IGame game, Entity attacker, Targetable target) {
        if (!(target instanceof Entity)) {
            return false;
        }
        Entity targetEntity = (Entity) target;
        if (!Compute.isInBuilding(game, attacker) || !Compute.isInBuilding(game, targetEntity)) {
            return false;
        }

        Building attkBldg = game.getBoard().getBuildingAt(attacker.getPosition());
        Building targBldg = game.getBoard().getBuildingAt(target.getPosition());

        return attkBldg.equals(targBldg);
    }

    /**
     * Determine if the given unit is inside of a building at the given
     * coordinates.
     *
     * @param game -
     *            the <code>IGame</code> object. This value may be
     *            <code>null</code>.
     * @param entity -
     *            the <code>Entity</code> to be checked. This value may be
     *            <code>null</code>.
     * @return <code>true</code> if the entity is inside of the building at
     *         those coordinates. <code>false</code> if there is no building
     *         at those coordinates or if the entity is on the roof or in the
     *         air above the building, or if any input argument is
     *         <code>null</code>.
     */
    public static boolean isInBuilding(IGame game, Entity entity) {

        // No game, no building.
        if (game == null) {
            return false;
        }

        // Null entities can't be in a building.
        if (entity == null) {
            return false;
        }

        // Call the version of the function that requires coordinates.
        return Compute.isInBuilding(game, entity, entity.getPosition());
    }

    /**
     * Determine if the given unit is inside of a building at the given
     * coordinates.
     *
     * @param game -
     *            the <code>IGame</code> object. This value may be
     *            <code>null</code>.
     * @param entity -
     *            the <code>Entity</code> to be checked. This value may be
     *            <code>null</code>.
     * @param coords -
     *            the <code>Coords</code> of the building hex. This value may
     *            be <code>null</code>.
     * @return <code>true</code> if the entity is inside of the building at
     *         those coordinates. <code>false</code> if there is no building
     *         at those coordinates or if the entity is on the roof or in the
     *         air above the building, or if any input argument is
     *         <code>null</code>.
     */
    public static boolean isInBuilding(IGame game, Entity entity, Coords coords) {

        // No game, no building.
        if (game == null) {
            return false;
        }

        // Null entities can't be in a building.
        if (entity == null) {
            return false;
        }

        // Null coordinates can't have buildings.
        if (coords == null) {
            return false;
        }

        // Get the Hex at those coordinates.

        return Compute.isInBuilding(game, entity.getElevation(), coords);
    }

    static boolean isInBuilding(IGame game, int entityElev, Coords coords) {

        // Get the Hex at those coordinates.
        final IHex curHex = game.getBoard().getHex(coords);

        if (curHex == null) {
            // probably off board artillery or reinforcement
            return false;
        }

        // The entity can't be inside of a building that isn't there.
        if (!curHex.containsTerrain(Terrains.BLDG_ELEV)) {
            return false;
        }

        // Get the elevations occupied by the building.
        int bldgHeight = curHex.terrainLevel(Terrains.BLDG_ELEV);
        int basement = 0;
        if (curHex.containsTerrain(Terrains.BLDG_BASEMENT)) {
            basement = curHex.terrainLevel(Terrains.BLDG_BASEMENT);
        }

        // Return true if the entity is in the range of building elevations.
        if ((entityElev >= (-basement)) && (entityElev < (bldgHeight))) {
            return true;
        }

        // Entity is not *inside* of the building.
        return false;
    }

    /**
     * scatter from hex according to dive bombing rules
     * (1d6 of scatter distance)
     * @param coords The <code>Coords</code> to scatter from
     * @return the <code>Coords</code> scattered to
     */
    public static Coords scatterDiveBombs(Coords coords) {
        return Compute.scatter(coords, Compute.d6());
    }

    /**
     * scatter from hex according to direct fire artillery rules
     * (1d6 of scatter distance)
     * @param coords The <code>Coords</code> to scatter from
     * @return the <code>Coords</code> scattered to
     */
    public static Coords scatterDirectArty(Coords coords) {
        return Compute.scatter(coords, Compute.d6());
    }

    /**
     * scatter from a hex according, roll d6 to choose scatter direction
     * @param coords The <code>Coords</code> to scatter from
     * @param margin the <code>int</code> margin of failure,
     * scatter distance will be the margin of failure
     * @return the <code>Coords</code> scattered to
     */
    public static Coords scatter(Coords coords, int margin) {
        int scatterDirection = Compute.d6(1) - 1;
        return coords.translated(scatterDirection, margin);
    }

    /**
     * scatter from hex according to atmospheric drop rules
     * d6 for direction, 1d6 per point of MOF
     * @param coords The <code>Coords</code> to scatter from
     * @param margin the <code>int</code> margin of failure
     * @return the <code>Coords</code> scattered to
     */
    public static Coords scatterAssaultDrop(Coords coords, int margin) {
        int scatterDirection = Compute.d6(1) - 1;
        int distance = Compute.d6(margin);
        return coords.translated(scatterDirection, distance);
    }

    /**
     * Gets a ring of hexes at a specified distance from the centre
     *
     * @param centre
     *            The centre point of the ring
     * @param range
     *            The radius of the ring
     */
    public static ArrayList<Coords> coordsAtRange(Coords centre, int range) {
        ArrayList<Coords> result = new ArrayList<Coords>(range * 6);
        if (range < 1) {
            result.add(centre);
            return result;
        }
        for (int dir = 0; dir < 6; dir++) {
            Coords corner = centre.translated(dir, range);
            for (int count = 0; count < range; count++) {
                result.add(corner);
                corner = corner.translated((dir + 2) % 6);
            }
        }
        return result;
    }

    /**
     * Gets a new target for a flight of swarm missiles that was just shot at an
     * entity and has missiles left
     *
     * @param game
     * @param aeId
     *            The attacking <code>Entity</code>
     * @param te
     *            The <code>Entity</code> that was shot at.
     * @param weaponId
     *            The <code>int</code> ID of the launcher used to fire this
     *            volley
     * @return the new target <code>Entity</code>. May return null if no new
     *         target available
     */
    public static Entity getSwarmMissileTarget(IGame game, int aeId, Entity te, int weaponId) {
        Coords coords = te.getPosition();
        Entity tempEntity = null;
        // first, check the hex of the original target
        Enumeration<Entity> entities = game.getEntities(coords);
        Vector<Entity> possibleTargets = new Vector<Entity>();
        while (entities.hasMoreElements()) {
            tempEntity = entities.nextElement();
            if (!tempEntity.getTargetedBySwarm(aeId, weaponId)) {
                // we found a target
                possibleTargets.add(tempEntity);
            }
        }
        // if there is at least one target, get a random one of them
        if (!possibleTargets.isEmpty()) {
            return possibleTargets.get(Compute.randomInt(possibleTargets.size()));
        }
        // loop through adjacent hexes
        for (int dir = 0; dir <= 5; dir++) {
            Coords tempcoords = coords.translated(dir);
            if (!game.getBoard().contains(tempcoords)) {
                continue;
            }
            if (coords.equals(tempcoords)) {
                continue;
            }
            entities = game.getEntities(tempcoords);
            if (entities.hasMoreElements()) {
                tempEntity = entities.nextElement();
                if (!tempEntity.getTargetedBySwarm(aeId, weaponId)) {
                    // we found a target
                    possibleTargets.add(tempEntity);
                }
            }
        }
        // if there is at least one target, get a random one of them
        if (!possibleTargets.isEmpty()) {
            return possibleTargets.get(Compute.randomInt(possibleTargets.size()));
        }
        return null;
    }

    public static int[] getRandomSkills(int method, int type, int level, boolean isVee) {

        int[] skills = { 4, 5 };

        // constant is the easy one
        if (method == METHOD_CONSTANT) {
            if (level == LEVEL_GREEN) {
                skills[0] = 5;
                skills[1] = 6;
            }
            if (level == LEVEL_VETERAN) {
                skills[0] = 3;
                skills[1] = 4;
            }
            if (level == LEVEL_ELITE) {
                skills[0] = 2;
                skills[1] = 3;
            }
            if ((type == TYPE_CLAN) || (type == TYPE_MD)) {
                skills[0]--;
                skills[1]--;
            }
            return skills;
        }

        // if using Taharqa's method, then the base skill level for each entity
        // is determined
        // separately
        if (method == METHOD_TAHARQA) {
            int lbonus = 0;
            if (level == LEVEL_GREEN) {
                lbonus -= 2;
            }
            if (level == LEVEL_VETERAN) {
                lbonus += 2;
            }
            if (level == LEVEL_ELITE) {
                lbonus += 4;
            }

            int lvlroll = Compute.d6(2) + lbonus;

            // restate level based on roll
            if (lvlroll < 6) {
                level = LEVEL_GREEN;
            } else if (lvlroll < 10) {
                level = LEVEL_REGULAR;
            } else if (lvlroll < 12) {
                level = LEVEL_VETERAN;
            } else {
                level = LEVEL_ELITE;
            }
        }

        // first get the bonus
        int bonus = 0;
        if (type == TYPE_CLAN) {
            if (isVee) {
                bonus--;
            } else {
                bonus++;
            }
        }
        if (type == TYPE_MD) {
            bonus++;
        }

        int gunroll = Compute.d6(1) + bonus;
        int pilotroll = Compute.d6(1) + bonus;

        int glevel = 0;
        int plevel = 0;

        switch (level) {
        case LEVEL_REGULAR:
            glevel = (int) Math.ceil(gunroll / 2.0) + 2;
            plevel = (int) Math.ceil(pilotroll / 2.0) + 2;
            break;
        case LEVEL_VETERAN:
            glevel = (int) Math.ceil(gunroll / 2.0) + 3;
            plevel = (int) Math.ceil(pilotroll / 2.0) + 3;
            break;
        case LEVEL_ELITE:
            glevel = (int) Math.ceil(gunroll / 2.0) + 4;
            plevel = (int) Math.ceil(pilotroll / 2.0) + 4;
            break;
        default:
            glevel = (int) Math.ceil((gunroll + 0.5) / 2.0);
            plevel = (int) Math.ceil((pilotroll + 0.5) / 2.0);
            if (gunroll <= 0) {
                glevel = 0;
            }
            if (pilotroll <= 0) {
                plevel = 0;
            }
        }

        skills[0] = skillLevels[0][glevel];
        skills[1] = skillLevels[1][plevel];

        return skills;
    }
    /*
    public static FighterSquadron compileSquadron(Vector<Entity> squadron) {

        //cycle through the entity vector and create a fighter squadron
        FighterSquadron fs = new FighterSquadron();
        /*
        String chassis = squadron.elementAt(0).getChassis();
        int si = 99;
        boolean alike = true;
        int armor = 0;
        int heat = 0;
        int safeThrust = 99;
        int n = 0;
        float weight = 0.0f;
        int fuel = 0;
        int bv = 0;
        double cost = 0.0;
        int nTC = 0;
        for(Entity e : squadron) {
            if(!chassis.equals(e.getChassis())) {
                alike = false;
            }
            n++;
            //names
            fs.fighters.add(e.getChassis() + " " + e.getModel());
            //armor
            armor += e.getTotalArmor();
            //heat
            heat += e.getHeatCapacity();
            //weight
            weight += e.getWeight();
            bv += e.calculateBattleValue();
            cost += e.getCost();
            //safe thrust
            if(e.getWalkMP() < safeThrust)
                safeThrust = e.getWalkMP();

            Aero a = (Aero)e;
            //si
            if(a.getSI() < si) {
                si = a.getSI();
            }

            //fuel - give the minimum fuel
            if(a.getFuel() < fuel || fuel == 0) {
                fuel = a.getFuel();
            }


            //weapons
            Mounted newmount;
            for(Mounted m : e.getEquipment() ) {

                if(m.getType() instanceof WeaponType) {
                    //first load the weapon onto the squadron
                    WeaponType wtype = (WeaponType)m.getType();
                    try{
                        newmount = fs.addEquipment(wtype, m.getLocation());
                    } catch (LocationFullException ex) {
                        System.out.println("Unable to compile weapons"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                        ex.printStackTrace();
                        return fs;
                    }
                    //skip to the next if it has no AT class
                    if(wtype.getAtClass() == WeaponType.CLASS_NONE) {
                        continue;
                    }

                    //now find the right bay
                    Mounted bay = fs.getFirstBay(wtype, newmount.getLocation(), newmount.isRearMounted());
                    //if this is null, then I should create a new bay
                    if(bay == null) {
                        EquipmentType newBay = WeaponType.getBayType(wtype.getAtClass());
                        try{
                            bay = fs.addEquipment(newBay, newmount.getLocation());
                        } catch (LocationFullException ex) {
                            System.out.println("Unable to compile weapons"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                            ex.printStackTrace();
                            return fs;
                        }
                    }
                    //now add the weapon to the bay
                    bay.addWeaponToBay(fs.getEquipmentNum(newmount));
                } else {
                    //just add the equipment normally
                    try{
                        //check if this is a TC
                        if (m.getType() instanceof MiscType && m.getType().hasFlag(MiscType.F_TARGCOMP)) {
                            nTC++;
                        }
                        fs.addEquipment(m.getType(), m.getLocation());
                    } catch (LocationFullException ex) {
                        System.out.println("Unable to add equipment"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                        ex.printStackTrace();
                        return fs;
                    }
                }
            }
        }

        armor = (int)Math.round(armor / 10.0);

        fs.setArmor(armor);
        fs.set0Armor(armor);
        fs.setHeatSinks(heat);
        fs.setOriginalWalkMP(safeThrust);
        fs.setN0Fighters(n);
        fs.setNFighters(n);
        fs.autoSetThresh();
        fs.setWeight(weight);
        fs.set0SI(si);
        fs.setCost(cost);
        fs.setFuel(fuel);

        if(nTC >= n) {
            fs.setHasTC(true);
        }

        //if all the same chassis, name by chassis
        //otherwise name by weight
        if(alike) {
            fs.setChassis(chassis + " Squadron");
        } else {
            int aveWeight = Math.round(weight/n);
            if(aveWeight <= 45) {
                fs.setChassis("Mixed Light Squadron");
            } else if(aveWeight <= 70) {
                fs.setChassis("Mixed Medium Squadron");
            } else {
                fs.setChassis("Mixed Heavy Squadron");
            }
        }
        fs.setModel("");
        fs.loadAllWeapons();
        fs.setRapidFire();

        return fs;
    }
    */



    /****STUFF FOR VECTOR MOVEMENT CALCULATIONS***/
    public static Coords getFinalPosition(Coords curpos, int[] v) {

        if((v == null) || (v.length != 6)) {
            return curpos;
        }

        //step through each vector and move the direction indicated
        int thrust = 0;
        Coords endpos = curpos;
        for(int dir = 0; dir < 6; dir++) {
            thrust = v[dir];
            while(thrust > 0) {
                endpos = endpos.translated(dir);
                thrust--;
            }
        }

        return endpos;
    }

    /**
     * method to change a set of active vectors for a one-point thrust
     * expenditure in the giving facing
     * @param v
     * @param facing
     * @return
     */
    public static int[] changeVectors(int[] v, int facing) {

        if((v == null) || (v.length != 6)) {
            return v;
        }

        //first look at opposing vectors
        int oppv = facing + 3;
        if(oppv > 5) {
            oppv -= 6;
        }
        //is this vector active
        if(v[oppv] > 0) {
            //then decrement it by one and return
            v[oppv]--;
            return v;
        }

        //now check oblique vectors
        int oblv1 = facing + 2;
        if(oblv1 > 5) {
            oblv1 -= 6;
        }
        int oblv2 = facing - 2;
        if(oblv2 < 0) {
            oblv2 += 6;
        }

        //check both of these and if either is active
        //deal with it and then return
        if((v[oblv1] > 0) || (v[oblv2] > 0)) {

            int newface = facing + 1;
            if(newface > 5) {
                newface = 0;
            }
            if(v[oblv1] > 0) {
                v[oblv1]--;
                v[newface]++;
            }

            newface = facing - 1;
            if(newface < 0) {
                newface = 0;
            }
            if(v[oblv2] > 0) {
                v[oblv2]--;
                v[newface]++;
            }
            return v;
        }

        //if nothing was found, then just increase velocity in this vector
        v[facing]++;
        return v;
    }

    /**
     * compare two vectors and determine if they are the same
     * @param v1
     * @param v2
     * @return
     */
    public static boolean sameVectors(int[] v1, int[] v2) {

        for(int i = 0; i<6; i++) {
            if(v1[i] != v2[i]) {
                return false;
            }
        }

        return true;
    }

    /**
     * Get the net velocity of two aeros for ramming attacks
     */
    public static int getNetVelocity(Coords src, Aero te, int avel, int tvel) {
        int angle = te.sideTableRam(src);

        switch(angle) {
        case Aero.RAM_TOWARD_DIR:
            return Math.max(avel+tvel,1);
        case Aero.RAM_TOWARD_OBL:
            return Math.max(avel+(tvel/2),1);
        case Aero.RAM_AWAY_OBL:
            return Math.max(avel-(tvel/2),1);
        case Aero.RAM_AWAY_DIR:
            return Math.max(avel-tvel,1);
        }
        return 0;
    }

    /**
     * Method replicates the Non-Conventional Damage against Infantry damage table
     * as well as shifting for direct blows.
     * also adjust for non-infantry damaging mechanized infantry
     * @param damage
     * @param mos
     * @param damageType
     * @return
     */
    public static int directBlowInfantryDamage(double damage, int mos, int damageType, boolean isNonInfantryAgainstMechanized){

        damageType += mos;

        switch(damageType){
        case Compute.WEAPON_DIRECT_FIRE:
            damage /= 10;
            break;
        case Compute.WEAPON_CLUSTER_BALLISTIC:
            damage /= 10;
            damage++;
            break;
        case Compute.WEAPON_PULSE:
            damage /= 10;
            damage += 2;
            break;
        case Compute.WEAPON_CLUSTER_MISSILE:
            damage /= 5;
            break;
        case Compute.WEAPON_CLUSTER_MISSILE_1D6:
            damage /=5;
            damage += Compute.d6();
            break;
        case Compute.WEAPON_CLUSTER_MISSILE_2D6:
            damage /=5;
            damage += Compute.d6(2);
            break;
        case Compute.WEAPON_CLUSTER_MISSILE_3D6:
            damage /=5;
            damage += Compute.d6(3);
            break;
        }
        damage = Math.ceil(damage);
        if (isNonInfantryAgainstMechanized) {
            damage *= 2;
        }
        return (int)damage;
    }

    /**
     * Method computes how much damage a dial down weapon has done
     * @param weapon
     * @param wtype
     * @returnnew damage
     */
    public static int dialDownDamage(Mounted weapon, WeaponType wtype){
        return Compute.dialDownDamage(weapon, wtype, 1);
    }

    /**
     * Method computes how much damage a dial down weapon has done
     * @param weapon
     * @param wtype
     * @param range
     * @return new damage
     */
    public static int dialDownDamage(Mounted weapon, WeaponType wtype, int range){
        int toReturn = wtype.getDamage(range);

        if ( !wtype.hasModes() ) {
            return toReturn;
        }

        String damage = weapon.curMode().getName();

        //Vehicle flamers have damage and heat modes so lets make sure this is an actual dial down Damage.
        if ( (damage.trim().toLowerCase().indexOf("damage") == 0) && (damage.trim().length() > 6)){
            toReturn = Integer.parseInt(damage.substring(6).trim());
        }

        return Math.min(wtype.getDamage(range), toReturn);

    }

    /**
     * Method computes how much heat a dial down weapon generates
     * @param weapon
     * @param wtype
     * @return Heat, minimum of 1;
     */
    public static int dialDownHeat(Mounted weapon, WeaponType wtype){
        return Compute.dialDownHeat(weapon, wtype,1);
    }

    /**
     * Method computes how much heat a dial down weapon generates
     * @param weapon
     * @param wtype
     * @param range
     * @return Heat, minimum of 1;
     */
    public static int dialDownHeat(Mounted weapon, WeaponType wtype, int range){
        int toReturn = wtype.getHeat();

        if ( !wtype.hasModes() ) {
            return toReturn;
        }

        int damage = wtype.getDamage(range);
        int newDamage = Compute.dialDownDamage(weapon, wtype, range);


        toReturn = Math.max(1, wtype.getHeat()-Math.max(0,damage-newDamage));
        return toReturn;

    }

    /**
     * @param ae - attacking entity
     * @param te - targeted entity
     * @return a vector of all the entities that are adjacent to the targeted entity and would fall along the angle
     *         of attack
     */
    public static ArrayList<Entity> getAdjacentEntitiesAlongAttack(Coords aPos, Coords tPos, IGame game) {
        ArrayList<Entity> entities = new ArrayList<Entity>();
        ArrayList<Coords> coords = Coords.intervening(aPos, tPos);
        // loop through all intervening coords
        for (Coords c : coords) {
            //must be adjacent to the target
            if((c.distance(tPos) > 1) || c.equals(tPos)) {
                continue;
            }
            //now lets add all the entities here
            for (Enumeration<Entity> i = game.getEntities(c); i.hasMoreElements();) {
                Entity en = i.nextElement();
                entities.add(en);
            }
        }
        return entities;
    }
} // End public class Compute

