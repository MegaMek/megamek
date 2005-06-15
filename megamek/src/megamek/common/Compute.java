/*
 * MegaMek - Copyright (C) 2000,2001,2002,2003,2004,2005 Ben Mazur (bmazur@sev.org)
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

import java.util.Enumeration;
import java.util.Vector;

import megamek.common.actions.BrushOffAttackAction;
import megamek.common.actions.ClubAttackAction;
import megamek.common.actions.KickAttackAction;
import megamek.common.actions.ProtomechPhysicalAttackAction;
import megamek.common.actions.PunchAttackAction;
import megamek.common.actions.PushAttackAction;
import megamek.common.actions.ThrashAttackAction;
import megamek.common.actions.WeaponAttackAction;

/**
 * The compute class is designed to provide static methods for mechs
 * and other entities moving, firing, etc.
 */
public class Compute
{

    public static final int        ARC_360          = 0;
    public static final int        ARC_FORWARD      = 1;
    public static final int        ARC_LEFTARM      = 2;
    public static final int        ARC_RIGHTARM     = 3;
    public static final int        ARC_REAR         = 4;
    public static final int        ARC_LEFTSIDE     = 5;
    public static final int        ARC_RIGHTSIDE    = 6;
    public static final int        ARC_MAINGUN      = 7;

    private static MMRandom random = MMRandom.generate(MMRandom.R_DEFAULT);

    /** Wrapper to random#d6(n) */
    public static int d6(int dice) {
        Roll roll = random.d6 (dice);
        return roll.getIntValue();
    }

    /** Wrapper to random#d6() */
    public static int d6() {
        Roll roll = random.d6();
        return roll.getIntValue();
    }

    /** Wrapper to random#randomInt(n) */
    public static int randomInt( int maxValue ) {
        Roll roll = new MMRoll (random, maxValue);
        return roll.getIntValue();
    }

    /**
     * Sets the RNG to the desired type
     */
    public static void setRNG(int type) {
        random = MMRandom.generate(type);
    }

    /**
     * Returns the odds that a certain number or above
     * will be rolled on 2d6.
     */
    public static double oddsAbove(int n) {
        if (n <= 2) {
            return 100.0;
        } else if (n > 12) {
            return 0;
        }
        final double[] odds = {100.0, 100.0,
                100.0, 97.2, 91.6, 83.3, 72.2, 58.3,
                41.6, 27.7, 16.6, 8.3, 2.78, 0};
        return odds[n];
    }

    /**
     * Returns an entity if the specified entity would cause a stacking
     * violation entering a hex, or returns null if it would not.
     *
     * The returned entity is the entity causing the violation.
     */
    public static Entity stackingViolation(IGame game, int enteringId, Coords coords) {
        Entity entering = game.getEntity(enteringId);
        return stackingViolation( game, entering, coords, null );
    }

    /**
     * When compiling an unloading step, both the transporter and the unloaded
     * unit probably occupy some other position on the board.
     */
    public static Entity stackingViolation(IGame game,
                                             Entity entering,
                                             Coords coords,
                                             Entity transport ) {
        boolean isMech = entering instanceof Mech;
        Entity firstEntity = transport;
        int totalUnits = 1;
        int thisLowStackingLevel = entering.getElevation();
        int thisHighStackingLevel = thisLowStackingLevel+entering.height();

        // Walk through the entities in the given hex.
        for (Enumeration i = game.getEntities(coords); i.hasMoreElements();) {
            final Entity inHex = (Entity)i.nextElement();

            int lowStackinglevel = inHex.getElevation();
            int highStackingLevel = lowStackinglevel+inHex.height();

            // Only do all this jazz if they're close enough together on level to interfere.
            if ((thisLowStackingLevel <= highStackingLevel) && (thisHighStackingLevel >= lowStackinglevel)) {
                // Don't compare the entering entity to itself.
                if (inHex.equals(entering)) {
                    continue;
                }
    
                // Ignore the transport of the entering entity.
                if ( inHex.equals(transport) ) {
                    continue;
                }
    
                // DFAing units don't count towards stacking
                if (inHex.isMakingDfa()) {
                    continue;
                }
    
                // If the entering entity is a mech,
                // then any other mech in the hex is a violation.
                if (isMech && (inHex instanceof Mech)) {
                    return inHex;
                }
    
                totalUnits++;
                // If the new one is the most 
                if (totalUnits > 4) {
                    // Arbitrarily return this one, because we can, and it's simpler.
                    return inHex;
                }
    
                // Otherwise, if there are two present entities controlled
                // by this player, returns a random one of the two.
                // Somewhat arbitrary, but how else should we resolve it?
                if ( !inHex.getOwner().isEnemyOf(entering.getOwner()) ) {
                    if (firstEntity == null) {
                        firstEntity = inHex;
                    } else {
                        return d6() > 3 ? firstEntity : inHex;
                    }
                }
            }
        }

        // okay, all clear
        return null;
    }

    public static boolean isEnemyIn(IGame game, int entityId, Coords coords, boolean isMech) {
        return isEnemyIn(game, entityId, coords, isMech, game.getEntity(entityId).getElevation());
    }

    /**
     * Returns true if there is any unit that is an enemy of the specified unit
     * in the specified hex.  This is only called for stacking purposes, and
     * so does not return true if the enemy unit is currenly making a DFA.
     */
    public static boolean isEnemyIn(IGame game, int entityId, Coords coords, boolean isMech, int enLowEl) {
        Entity entity = game.getEntity(entityId);
        int enHighEl = enLowEl+entity.getHeight();
        for (Enumeration i = game.getEntities(coords); i.hasMoreElements();) {
            final Entity inHex = (Entity)i.nextElement();
            int inHexEnLowEl = inHex.getElevation();
            int inHexEnHighEl = inHexEnLowEl+inHex.getHeight();
            if ((!isMech || inHex instanceof Mech) && inHex.isEnemyOf(entity) && !inHex.isMakingDfa() && (enLowEl <= inHexEnHighEl) && (enHighEl >= inHexEnLowEl)) {
                return true;
            }
        }
        return false;
    }

    /**
     * @return true if a piloting skill roll is needed to traverse the terrain
     * TODO: VTOL sideslipping
     */
    public static boolean isPilotingSkillNeeded(IGame game, int entityId,
                                                Coords src, Coords dest,
                                                int movementType,
                                                boolean isTurning,
                                                boolean prevStepIsOnPavement) {
        final Entity entity = game.getEntity(entityId);
        final IHex srcHex = game.getBoard().getHex(src);
        final IHex destHex = game.getBoard().getHex(dest);
        final boolean isInfantry = ( entity instanceof Infantry );
        final boolean isPavementStep = canMoveOnPavement( game, src, dest );

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
        if (movementType != IEntityMovementType.MOVE_JUMP
            && destHex.terrainLevel(Terrains.RUBBLE) > 0
            && !isInfantry) {
            return true;
        }
        
        // check for swamp
        if (destHex.containsTerrain(Terrains.SWAMP)
            && !(entity instanceof VTOL)
            && entity.getMovementMode() != IEntityMovementMode.HOVER
            && movementType != IEntityMovementType.MOVE_JUMP) {
            return true;
        }

        // Check for water unless we're a hovercraft or naval or using a bridge.
        if (movementType != IEntityMovementType.MOVE_JUMP
            && !(entity instanceof VTOL)
            && ((entity.getMovementMode() != IEntityMovementMode.HOVER)
                || (entity.getMovementMode() != IEntityMovementMode.NAVAL)
                || (entity.getMovementMode() != IEntityMovementMode.HYDROFOIL)
                || (entity.getMovementMode() != IEntityMovementMode.SUBMARINE))
            && destHex.terrainLevel(Terrains.WATER) > 0
            && !isPavementStep) {
            return true;
        }

        // Check for skid.  Please note, the skid will be rolled on the
        // current step, but starts from the previous step's location.
        // TODO: add check for elevation of pavement, road,
        //       or bridge matches entity elevation.
        /* Bug 754610: Revert fix for bug 702735.
        if ( ( srcHex.contains(Terrain.PAVEMENT) ||
               srcHex.contains(Terrain.ROAD) ||
               srcHex.contains(Terrain.BRIDGE) )
        */
        if ( prevStepIsOnPavement
        //   && overallMoveType == IMoveType.MOVE_RUN
             && movementType == IEntityMovementType.MOVE_RUN
             && isTurning
             && !isInfantry ) {
            return true;
        }

        // If we entering or leaving a building, all non-infantry
        // need to make a piloting check to avoid damage.
        // TODO: allow entities to occupy different levels of buildings.
        int nSrcEl = entity.elevationOccupied(srcHex);
        int nDestEl = entity.elevationOccupied(destHex);
        if ( ( nSrcEl < srcHex.terrainLevel( Terrains.BLDG_ELEV ) ||
               nDestEl < destHex.terrainLevel( Terrains.BLDG_ELEV ) ) &&
             !(entity instanceof Infantry) ) {
            return true;
        }
        
        //check sideslips
        if (entity instanceof VTOL) {
            if(isTurning && movementType == IEntityMovementType.MOVE_RUN)
            return true;
        }

        return false;
    }

    /**
     * Can the defending unit be displaced from the source to the destination?
     */
    public static boolean isValidDisplacement(IGame game, int entityId,
                                              Coords src, int direction) {
        return isValidDisplacement(game, entityId, src,
                                   src.translated(direction));
    }
    /**
     * Can the defending unit be displaced from the source to the destination?
     */
    public static boolean isValidDisplacement(IGame game, int entityId,
                                              Coords src, Coords dest) {
        final Entity entity = game.getEntity(entityId);
        final IHex srcHex = game.getBoard().getHex(src);
        final IHex destHex = game.getBoard().getHex(dest);
        final Coords[] intervening = Coords.intervening(src, dest);
        final int direction = src.direction(dest);

        // arguments valid?
        if (entity == null) {
            throw new IllegalArgumentException("Entity invalid.");
        }

        // an easy check
        if (!game.getBoard().contains(dest)) {
            if (game.getOptions().booleanOption("push_off_board")) {
                return true;
            } else {
                return false;
            }
        }

        // can't be displaced into prohibited terrain
        // unless we're displacing a tracked or wheeled vee into water
        if (entity.isHexProhibited(destHex) && 
            !(entity instanceof Tank && destHex.containsTerrain(Terrains.WATER) && 
              (entity.movementMode == IEntityMovementMode.TRACKED 
               || entity.movementMode == IEntityMovementMode.WHEELED))) {
            return false;
        }

        // can't go up more levels than normally possible
        for (int i = 0; i < intervening.length; i++) {
            final IHex hex = game.getBoard().getHex(intervening[i]);
            int change = entity.elevationOccupied(hex) - entity.elevationOccupied(srcHex);
            if (change > entity.getMaxElevationChange()) {
                return false;
            }
        }

        // if there's an entity in the way, can they be displaced in that direction?
        Entity inTheWay = stackingViolation(game, entityId, dest);
        if (inTheWay != null) {
            return isValidDisplacement(game, inTheWay.getId(), inTheWay.getPosition(), direction);
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
    public static Coords getValidDisplacement(IGame game, int entityId,
                                              Coords src, int direction) {
        // check the surrounding hexes, nearest to the original direction first
        int[] offsets = {0, 1, 5, 2, 4, 3};
        for (int i = 0; i < offsets.length; i++) {
            Coords dest = src.translated((direction + offsets[i]) % 6);
            if (isValidDisplacement(game, entityId, src, dest)) {
                return dest;
            }
        }
        // have fun being insta-killed!
        return null;
    }

    /**
     * Gets a preferred displacement.  Right now this picks the surrounding hex
     * with the highest elevation that is a valid displacement.
     *
     * @return valid displacement coords, or null if none
     */
    public static Coords getPreferredDisplacement(IGame game, int entityId,
                                              Coords src, int direction) {
        final Entity entity = game.getEntity(entityId);
        int highestElev = Integer.MIN_VALUE;
        Coords highest = null;

        // check the surrounding hexes, nearest to the original direction first
        int[] offsets = {0, 1, 5, 2, 4, 3};
        for (int i = 0; i < offsets.length; i++) {
            Coords dest = src.translated((direction + offsets[i]) % 6);
            if (isValidDisplacement(game, entityId, src, dest) 
                && game.getBoard().contains(dest)) {
                IHex hex = game.getBoard().getHex(dest);
                int elevation = entity.elevationOccupied(hex);
                if (elevation > highestElev) {
                    highestElev = elevation;
                    highest = dest;
                }
            }
        }
        return highest;
    }

    /**
     * Gets a hex to displace a missed charge to.  Picks left or right, first
     * preferring higher hexes, then randomly, or returns the base hex if
     * they're impassible.
     */
    public static Coords getMissedChargeDisplacement(IGame game, int entityId, Coords src, int direction) {
        Coords first = src.translated((direction + 1) % 6);
        Coords second = src.translated((direction + 5) % 6);
        IHex firstHex = game.getBoard().getHex(first);
        IHex secondHex = game.getBoard().getHex(second);
        Entity entity = game.getEntity(entityId);

        if (firstHex == null || secondHex == null) {
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

        if (isValidDisplacement(game, entityId, src, src.direction(first)) 
            && game.getBoard().contains(first)) {
            return first;
        } else if (isValidDisplacement(game, entityId, src, src.direction(second))
            && game.getBoard().contains(second)) {
            return second;
        } else {
            return src;
        }
    }

    /**
     * Finds the best spotter for the attacker.  The best spotter is the one
     * with the lowest attack modifiers, of course.  LOS modifiers and
     * movement are considered.
     */
    public static Entity findSpotter(IGame game, Entity attacker, Targetable target) {
      Entity spotter = null;
      ToHitData bestMods = new ToHitData(ToHitData.IMPOSSIBLE, "");

        for (java.util.Enumeration i = game.getEntities(); i.hasMoreElements();) {
            Entity other = (Entity)i.nextElement();
            if (!other.isSpotting() || attacker.isEnemyOf(other)) {
                continue; // useless to us...
            }
            // what are this guy's mods to the attack?
            LosEffects los = LosEffects.calculateLos(game, other.getId(), target);
            ToHitData mods = los.losModifiers(game);
            los.setTargetCover(false);
            mods.append(getAttackerMovementModifier(game, other.getId()));
            // is this guy a better spotter?
            if (spotter == null || mods.getValue() < bestMods.getValue()) {
                spotter = other;
                bestMods = mods;
            }
        }

        return spotter;
    }

    public static ToHitData getImmobileMod(Targetable target) {
        return getImmobileMod(target, Mech.LOC_NONE, IAimingModes.AIM_MODE_NONE);
    }

    public static ToHitData getImmobileMod(Targetable target, int aimingAt, int aimingMode) {
        if (target.isImmobile()) {
            if ((aimingAt == Mech.LOC_HEAD) &&
                (aimingMode == IAimingModes.AIM_MODE_IMMOBILE)) {
                return new ToHitData(3, "aiming at head");
            } else {
                return new ToHitData(-4, "target immobile");
            }
        } else {
            return null;
        }
    }

    /**
     * Determines the to-hit modifier due to range for an attack with the
     * specified parameters. Includes minimum range, infantry 0-range
     * mods, and target stealth mods.  Accounts for friendly C3 units.
     *
     * @return the modifiers
     */
    public static ToHitData getRangeMods(IGame game, Entity ae, int weaponId, Targetable target) {
        Mounted weapon = ae.getEquipment(weaponId);
        WeaponType wtype = (WeaponType)weapon.getType();
        int[] weaponRanges = wtype.getRanges();
        boolean isAttackerInfantry = (ae instanceof Infantry);
        boolean isWeaponInfantry = wtype.hasFlag(WeaponType.F_INFANTRY);
        boolean isLRMInfantry = isWeaponInfantry && wtype.getAmmoType() == AmmoType.T_LRM;
        boolean isIndirect = !(wtype.hasFlag(WeaponType.F_ONESHOT)) && wtype.getAmmoType() == AmmoType.T_LRM //For now, oneshot LRM launchers won't be able to indirect.  Sue me, until I can figure out a better fix.
            && weapon.curMode().equals("Indirect");
        boolean useExtremeRange = game.getOptions().booleanOption("maxtech_range");

        ToHitData mods = new ToHitData();

        // modify the ranges for ATM missile systems based on the ammo selected
        // TODO: this is not the right place to hardcode these
        if (wtype.getAmmoType() == AmmoType.T_ATM) {
            AmmoType atype = (AmmoType)weapon.getLinked().getType();
            if (atype.getMunitionType() == AmmoType.M_EXTENDED_RANGE) {
                weaponRanges = new int[] {4, 9, 18, 27, 36};
            }
            else if (atype.getMunitionType() == AmmoType.M_HIGH_EXPLOSIVE) {
                weaponRanges = new int[] {0, 3, 6, 9, 12};
            }
        }
        //
        // modifiy the ranges for PPCs when field inhibitors are turned off
        // TODO: See above, it should be coded elsewhere...
        //
        if (wtype.getName().equals("Particle Cannon")) {
            if (game.getOptions().booleanOption("maxtech_ppc_inhibitors")) {
                if (weapon.curMode().equals("Field Inhibitor OFF")) {
                    weaponRanges[0] = 0;
                }
            }
        }
        //is water involved?
        IHex attHex = game.getBoard().getHex(ae.getPosition());
        IHex targHex = game.getBoard().getHex(target.getPosition());
        int targEl;
        if (target == null) {
            targEl = game.getBoard().getHex(target.getPosition()).floor();
        } else {

            targEl = target.absHeight();
        }

        if (ae.getLocationStatus(weapon.getLocation()) == ILocationExposureStatus.WET) {
            weaponRanges = wtype.getWRanges();
            //HACK on ranges: for those without underwater range,
            // long == medium; iteration in rangeBracket() allows this
            if (weaponRanges[RangeType.RANGE_SHORT] == 0) {
                return new ToHitData(ToHitData.IMPOSSIBLE,
                                     "Weapon cannot fire underwater.");
            }
            if (!(targHex.containsTerrain(Terrains.WATER)) ||
                targHex.surface() <= target.getElevation()) {
                //target on land or over water
                return new ToHitData(ToHitData.IMPOSSIBLE,
                                     "Weapon underwater, but not target.");
            }
        } else if (targHex.containsTerrain(Terrains.WATER) &&
                   targHex.surface() > targEl) {
            //target completely underwater, weapon not
            return new ToHitData(ToHitData.IMPOSSIBLE,
                                 "Target underwater, but not weapon.");
        }

        // determine base distance & range bracket
        int distance = effectiveDistance(game, ae, target);
        int range = RangeType.rangeBracket(distance, weaponRanges,
                                           useExtremeRange);

        // short circuit if at zero range or out of range
        if (range == RangeType.RANGE_OUT) {
            return new ToHitData(ToHitData.AUTOMATIC_FAIL, "Target out of range");
        }
        if (distance == 0 && !isAttackerInfantry) {
            return new ToHitData(ToHitData.AUTOMATIC_FAIL, "Only infantry shoot at zero range");
        }

        // find any c3 spotters that could help
        Entity c3spotter = findC3Spotter(game, ae, target);
        if (isIndirect) {
            c3spotter = ae; // no c3 when using indirect fire
        }
        int c3dist = effectiveDistance(game, c3spotter, target);
        int c3range = RangeType.rangeBracket(c3dist, weaponRanges,
                                             useExtremeRange);

        // determine which range we're using
        int usingRange = Math.min(range, c3range);

        String targSysType = "";
        // Get the targetting system type string ready, if necessary
        if ((ae.getTargSysType() == MiscType.T_TARGSYS_LONGRANGE) && (ae.getTargSysType() == MiscType.T_TARGSYS_SHORTRANGE)){
            targSysType = " (w/"+MiscType.getTargetSysName(ae.getTargSysType())+")";
        }

        // add range modifier
        if (usingRange == range) {
            // no c3 adjustment
            if ((range == RangeType.RANGE_SHORT) && (ae.getShortRangeModifier() != 0)) {
                mods.addModifier(ae.getShortRangeModifier(), "short range"+targSysType);
            } else if (range == RangeType.RANGE_MEDIUM) {
                // Right now, the range-mod affecting targetting systems DON'T affect medium range, so we won't add that here ever.
                mods.addModifier(ae.getMediumRangeModifier(), "medium range");
            }
            else if (range == RangeType.RANGE_LONG) {
                // Protos that loose head sensors can't shoot long range.
                if( (ae instanceof Protomech) &&
                    (2 == ((Protomech)ae).getCritsHit(Protomech.LOC_HEAD)) ) {
                    mods.addModifier
                        ( ToHitData.IMPOSSIBLE,
                          "No long range attacks with destroyed head sensors." );
                } else {
                    mods.addModifier(ae.getLongRangeModifier(), "long range"+targSysType);
                }
            }
            else if (range == RangeType.RANGE_EXTREME) {
                // Protos that loose head sensors can't shoot extreme range.
                if( (ae instanceof Protomech) &&
                    (2 == ((Protomech)ae).getCritsHit(Protomech.LOC_HEAD)) ) {
                    mods.addModifier
                        ( ToHitData.IMPOSSIBLE,
                          "No extreme range attacks with destroyed head sensors." );
                } else {
                    mods.addModifier(ae.getExtremeRangeModifier(), "extreme range"+targSysType);
                }
            }
        } else {
            // report c3 adjustment
            if (c3range == RangeType.RANGE_SHORT) {
                mods.addModifier(ae.getShortRangeModifier(), "short range due to C3 spotter"+targSysType);
            }
            else if (c3range == RangeType.RANGE_MEDIUM) {
                mods.addModifier(ae.getMediumRangeModifier(), "medium range due to C3 spotter"+targSysType);
            }
            else if (c3range == RangeType.RANGE_LONG) {
                mods.addModifier(ae.getLongRangeModifier(), "long range due to C3 spotter"+targSysType);
            }
        }

        // add infantry LRM maximum range penalty
        if (isLRMInfantry && distance == weaponRanges[RangeType.RANGE_LONG]) {
            mods.addModifier(1, "infantry LRM maximum range");
        }

        // add infantry zero-range modifier
        // TODO: this is not the right place to hardcode these
        if (isWeaponInfantry && distance == 0) {
            // Infantry platoons attacking with infantry weapons can attack
            // in the same hex with a base of 2, except for flamers and
            // SRMs, which have a base of 3 and LRMs, which suffer badly.
            if (wtype.hasFlag(WeaponType.F_FLAMER)) {
                mods.addModifier(-1, "infantry flamer assault");
            } else if (wtype.getAmmoType() == AmmoType.T_SRM ) {
                mods.addModifier(-1, "infantry SRM assault");
            } else if (wtype.getAmmoType() != AmmoType.T_LRM) {
                mods.addModifier(-2, "infantry assault");
            }
        }

        // add minimum range modifier
        int minRange = weaponRanges[RangeType.RANGE_MINIMUM];
        if (minRange > 0 && distance <= minRange) {
            int minPenalty = (minRange - distance) + 1;
            // Infantry LRMs suffer double minimum range penalties.
            if (isLRMInfantry) {
                mods.addModifier(minPenalty * 2, "infantry LRM minimum range");
            } else {
                mods.addModifier(minPenalty, "minimum range");
            }
        }

        // add any target stealth modifier
        if ((target instanceof Entity) && ((Entity)target).isStealthActive()) {
            mods.append(((Entity)target).getStealthModifier(usingRange));
        }

        return mods;
    }

  /**
   * Finds the effective distance between an attacker and a target.
   * Includes the distance bonus if the attacker and target are in the
   * same building and on different levels.
   *
   * @return the effective distance
   */
    public static int effectiveDistance(IGame game, Entity attacker, Targetable target) {
        int distance = attacker.getPosition().distance(target.getPosition());

        // If the attack is completely inside a building, add the difference
        // in elevations between the attacker and target to the range.
        // TODO: should the player be explcitly notified?
        if ( isInSameBuilding(game, attacker, target) ) {
            int aElev = attacker.getElevation();
            int tElev = target.getElevation();
            distance += Math.abs(aElev - tElev);
        }

        return distance;
    }

    /**
     * Attempts to find a C3 spotter that is closer to the target than the
     * attacker.
     * @return A closer C3 spotter, or the attack if no spotters are found
     */
    private static Entity findC3Spotter(IGame game, Entity attacker, Targetable target) {
    if (!attacker.hasC3() && !attacker.hasC3i()) {
      return attacker;
    }
    Entity c3spotter = attacker;
    int c3range = attacker.getPosition().distance(target.getPosition());

    for (java.util.Enumeration i = game.getEntities(); i.hasMoreElements();) {
      Entity friend = (Entity)i.nextElement();

      // TODO : can units being transported be used for C3 spotting?
      if ( attacker.equals(friend) ||
           !friend.isActive() ||
           !attacker.onSameC3NetworkAs(friend) ||
           !canSee(game, friend, target) ) {
        continue; // useless to us...
      }

      int buddyRange = effectiveDistance(game, friend, target);
      if(buddyRange < c3range) {
        c3range = buddyRange;
        c3spotter = friend;
      }

    }
    return c3spotter;
  }

  /**
     * Gets the modifiers, if any, that the mech receives from being prone.
     * @return any applicable modifiers due to being prone
     */
    public static ToHitData getProneMods(IGame game, Entity attacker, int weaponId) {
    if (!attacker.isProne()) {
      return null; // no modifier
    }

    ToHitData mods = new ToHitData();
    Mounted weapon = attacker.getEquipment(weaponId);
        if ( attacker.entityIsQuad() ) {
            int legsDead = ((Mech)attacker).countBadLegs();
            if (legsDead == 0) {
        // No legs destroyed: no penalty and can fire all weapons
              return null; // no modifier
            } else if ( legsDead >= 3 ) {
        return new ToHitData(ToHitData.IMPOSSIBLE, "Prone with three or more legs destroyed.");
      }
      // we have one or two dead legs...

            // Need an intact front leg
            if (attacker.isLocationBad(Mech.LOC_RARM) && attacker.isLocationBad(Mech.LOC_LARM)) {
                return new ToHitData(ToHitData.IMPOSSIBLE, "Prone with both front legs destroyed.");
            }

            // front leg-mounted weapons have addidional trouble
            if (weapon.getLocation() == Mech.LOC_RARM || weapon.getLocation() == Mech.LOC_LARM) {
                int otherArm = weapon.getLocation() == Mech.LOC_RARM ? Mech.LOC_LARM : Mech.LOC_RARM;
                // check previous attacks for weapons fire from the other arm
                if (isFiringFromArmAlready(game, weaponId, attacker, otherArm)) {
          return new ToHitData(ToHitData.IMPOSSIBLE, "Prone and firing from other front leg already.");
                }
            }
            // can't fire rear leg weapons
            if (weapon.getLocation() == Mech.LOC_LLEG || weapon.getLocation() == Mech.LOC_RLEG) {
                return new ToHitData(ToHitData.IMPOSSIBLE, "Can't fire rear leg-mounted weapons while prone with destroyed legs.");
            }
            mods.addModifier(2, "attacker prone");
        } else {
            int l3ProneFiringArm = Entity.LOC_NONE;

            if (attacker.isLocationBad(Mech.LOC_RARM) || attacker.isLocationBad(Mech.LOC_LARM)) {
              if ( game.getOptions().booleanOption("maxtech_prone_fire") ) {
                //Can fire with only one arm
                if (attacker.isLocationBad(Mech.LOC_RARM) && attacker.isLocationBad(Mech.LOC_LARM)) {
                    return new ToHitData(ToHitData.IMPOSSIBLE, "Prone with both arms destroyed.");
                }

                l3ProneFiringArm = attacker.isLocationBad(Mech.LOC_RARM) ? Mech.LOC_LARM : Mech.LOC_RARM;
              } else {
                // must have an arm intact
                return new ToHitData(ToHitData.IMPOSSIBLE, "Prone with one or both arms destroyed.");
              }
            }

            // arm-mounted weapons have addidional trouble
            if (weapon.getLocation() == Mech.LOC_RARM || weapon.getLocation() == Mech.LOC_LARM) {
              if ( l3ProneFiringArm == weapon.getLocation() ) {
                return new ToHitData(ToHitData.IMPOSSIBLE, "Prone and propping up with this arm.");
              }

                int otherArm = weapon.getLocation() == Mech.LOC_RARM ? Mech.LOC_LARM : Mech.LOC_RARM;
        // check previous attacks for weapons fire from the other arm
        if (isFiringFromArmAlready(game, weaponId, attacker, otherArm)) {
          return new ToHitData(ToHitData.IMPOSSIBLE, "Prone and firing from other arm already.");
        }
            }
            // can't fire leg weapons
            if (weapon.getLocation() == Mech.LOC_LLEG || weapon.getLocation() == Mech.LOC_RLEG) {
                return new ToHitData(ToHitData.IMPOSSIBLE, "Can't fire leg-mounted weapons while prone.");
            }
            mods.addModifier(2, "attacker prone");

            if ( l3ProneFiringArm != Entity.LOC_NONE ) {
              mods.addModifier(1, "attacker propping on single arm");
            }
        }
        return mods;
  }

  /**
   * Checks to see if there is an attack previous to the one with this
   * weapon from the specified arm.
   * @return true if there is a previous attack from this arm
   */
  private static boolean isFiringFromArmAlready(IGame game, int weaponId, final Entity attacker, int armLoc) {
    for (Enumeration i = game.getActions(); i.hasMoreElements();) {
        Object o = i.nextElement();
        if (!(o instanceof WeaponAttackAction)) {
            continue;
        }
        WeaponAttackAction prevAttack = (WeaponAttackAction)o;
        // stop when we get to this weaponattack (does this always work?)
        if (prevAttack.getEntityId() == attacker.getId() && prevAttack.getWeaponId() == weaponId) {
            break;
        }
        if (prevAttack.getEntityId() == attacker.getId() && attacker.getEquipment(prevAttack.getWeaponId()).getLocation() == armLoc) {
          return true;
        }
    }
    return false;
  }

  /**
   * Adds any damage modifiers from arm critical hits or sensor damage.
   * @return Any applicable damage modifiers
   */
  public static ToHitData getDamageWeaponMods(Entity attacker, Mounted weapon)
  {
      ToHitData mods = new ToHitData();

      if ( attacker instanceof Protomech ) {

          // Head criticals add to target number of all weapons.
          int hits = ((Protomech)attacker).getCritsHit(Protomech.LOC_HEAD);
          if( hits > 0 ) {
              mods.addModifier( hits, hits + " head critical(s)" );
          }

          // Arm mounted (and main gun) weapons get DRMs from arm crits.
          switch ( weapon.getLocation() ) {
          case Protomech.LOC_LARM:
          case Protomech.LOC_RARM:
              hits = ((Protomech)attacker).getCritsHit( weapon.getLocation() );
              if ( hits > 0 ) {
                  mods.addModifier( hits, hits + " arm critical(s)" );
              }
              break;
          case Protomech.LOC_MAINGUN:
              // Main gun is affected by crits in *both* arms.
              hits = ((Protomech)attacker).getCritsHit( Protomech.LOC_LARM );
              hits += ((Protomech)attacker).getCritsHit( Protomech.LOC_RARM );
              if ( 4 == hits ) {
                  mods.addModifier( ToHitData.IMPOSSIBLE,
                                    "Cannot fire main gun with no arms." );
              }
              else if ( hits > 0 ) {
                  mods.addModifier( hits, hits + " arm critical(s)" );
              }
              break;
          }

      } // End attacker-is-Protomech

      // Is the shoulder destroyed?
      else if ( attacker.getBadCriticals( CriticalSlot.TYPE_SYSTEM,
                                              Mech.ACTUATOR_SHOULDER,
                                              weapon.getLocation() ) > 0 ) {
          mods.addModifier(4, "shoulder actuator destroyed");
      }
      else {
          // no shoulder hits, add other arm hits
          int actuatorHits = 0;
          if ( attacker.getBadCriticals( CriticalSlot.TYPE_SYSTEM,
                                               Mech.ACTUATOR_UPPER_ARM,
                                               weapon.getLocation() ) > 0 ) {
              actuatorHits++;
          }
          if ( attacker.getBadCriticals(CriticalSlot.TYPE_SYSTEM,
                                              Mech.ACTUATOR_LOWER_ARM,
                                              weapon.getLocation() ) > 0 ) {
              actuatorHits++;
          }
          if (actuatorHits > 0) {
              mods.addModifier( actuatorHits,
                                actuatorHits + " destroyed arm actuators" );
          }
    }

    // sensors critical hit to attacker
    int sensorHits = attacker.getBadCriticals( CriticalSlot.TYPE_SYSTEM,
                                                     Mech.SYSTEM_SENSORS,
                                                     Mech.LOC_HEAD );
    if (sensorHits > 0) {
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
    boolean curInFrontArc = isInArc(attacker.getPosition(), attacker.getSecondaryFacing(), target.getPosition(), ARC_FORWARD);

    int primaryTarget = Entity.NONE;
        for (Enumeration i = game.getActions(); i.hasMoreElements();) {
            Object o = i.nextElement();
            if (!(o instanceof WeaponAttackAction)) {
                continue;
            }
            WeaponAttackAction prevAttack = (WeaponAttackAction)o;
            if (prevAttack.getEntityId() == attacker.getId()) {
                // first front arc target is our primary.
                // if first target is non-front, and either a later target or
                // the current one is in front, use that instead.
                Targetable pte = game.getTarget(prevAttack.getTargetType(), prevAttack.getTargetId());
                // in double blind play, we might not have the target in our 
                // local copy of the game. In that case, the sprite won't
                // have the correct to-hit number, but at least we don't crash
                if ( pte == null ) {
                    continue;
                }
                // When targeting a stealthed Mech, you can _only_ target it, not anything else (BMRr, pg. 147)
                if (pte instanceof Entity && ((Entity)pte).isStealthActive() && pte != target ) {
                    return new ToHitData(ToHitData.IMPOSSIBLE, "When targeting a stealthed Mech, can not attack secondary targets");
                }
                if (isInArc(attacker.getPosition(), attacker.getSecondaryFacing(), pte.getPosition(), ARC_FORWARD)) {
                    primaryTarget = prevAttack.getTargetId();
                    break;
                } else if (primaryTarget == Entity.NONE && !curInFrontArc) {
                    primaryTarget = prevAttack.getTargetId();
                }
            }
        }

        if (primaryTarget == Entity.NONE || primaryTarget == target.getTargetId()) {
          // current target is primary target
          return null; // no modifier
        }

        // current target is secondary

        // Infantry can't attack secondary targets (BMRr, pg. 32).
        if (attacker instanceof Infantry) {
          return new ToHitData(ToHitData.IMPOSSIBLE, "Can't have multiple targets.");
        }
        
        // Stealthed Mechs can't be secondary targets (BMRr, pg. 147)
        if ((target instanceof Entity) && ((Entity)target).isStealthActive() ) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Can't target Mech with active stealth armor as secondary target");
        }
        
        if (curInFrontArc) {
      return new ToHitData(1, "secondary target modifier");
        } else {
      return new ToHitData(2, "secondary target modifier");
        }
    }
    
    /**
     * Damage that a mech does with a accidental fall from above.
     */
    
    public static int getAffaDamageFor(Entity entity) {
        return (int)entity.getWeight() / 10;
    }

    /**
     * Modifier to attacks due to attacker movement
     */
    public static ToHitData getAttackerMovementModifier(IGame game, int entityId) {
        return getAttackerMovementModifier(game, entityId, game.getEntity(entityId).moved);
    }

    /**
     * Modifier to attacks due to attacker movement
     */
    public static ToHitData getAttackerMovementModifier(IGame game, int entityId, int movement) {
        final Entity entity = game.getEntity(entityId);
        ToHitData toHit = new ToHitData();

        // infantry aren't affected by their own movement
        if (entity instanceof Infantry) {
            return toHit;
        }

        if (movement == IEntityMovementType.MOVE_WALK) {
            toHit.addModifier(1, "attacker walked");
        } else if (movement == IEntityMovementType.MOVE_RUN ||
                   movement == IEntityMovementType.MOVE_SKID) {
            toHit.addModifier(2, "attacker ran");
        } else if (movement == IEntityMovementType.MOVE_JUMP) {
            toHit.addModifier(3, "attacker jumped");
        }

        return toHit;
    }


    /**
     * Modifier to attacks due to spotter movement
     */
    public static ToHitData getSpotterMovementModifier(IGame game, int entityId) {
        return getSpotterMovementModifier(game, entityId, game.getEntity(entityId).moved);
    }

    /**
     * Modifier to attacks due to spotter movement
     */
    public static ToHitData getSpotterMovementModifier(IGame game, int entityId, int movement) {
        final Entity entity = game.getEntity(entityId);
        ToHitData toHit = new ToHitData();

        if (movement == IEntityMovementType.MOVE_WALK) {
            toHit.addModifier(1, "spotter walked");
        } else if (movement == IEntityMovementType.MOVE_RUN ||
                   movement == IEntityMovementType.MOVE_SKID) {
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

        if (attacker.getCrew().getOptions().booleanOption("melee_specialist")
            && attacker instanceof Mech
            && getTargetMovementModifier(game, target.getId()).getValue() > 0) {
            toHit.addModifier(-1, "melee specialist");
        }

        // Mek targets that are dodging are harder to hit.

        if ( target != null && target instanceof Mech &&
             target.getCrew().getOptions().booleanOption("dodge_maneuver") &&
             ( target.dodging )) {
            toHit.addModifier(2, "target is dodging");
        }
    }

    /**
     * Modifier to attacks due to target movement
     */
    public static ToHitData getTargetMovementModifier(IGame game, int entityId) {
        Entity entity = game.getEntity(entityId);
        ToHitData toHit = getTargetMovementModifier
            ( entity.delta_distance, ((entity.moved == IEntityMovementType.MOVE_JUMP) || (entity instanceof VTOL)), game.getOptions().booleanOption("maxtech_target_modifiers") );

        // Did the target skid this turn?
        if ( entity.moved == IEntityMovementType.MOVE_SKID ) {
            toHit.addModifier( 2, "target skidded" );
        }

        return toHit;
    }

    /**
     * Target movement modifer for the specified delta_distance
     */
    public static ToHitData getTargetMovementModifier(int distance, boolean jumped) {
        return getTargetMovementModifier(distance, jumped, false);
    }

    public static ToHitData getTargetMovementModifier(int distance, boolean jumped, boolean maxtech) {
        ToHitData toHit = new ToHitData();

        if (!maxtech) {
            if (distance >= 3 && distance <= 4) {
                toHit.addModifier(1, "target moved 3-4 hexes");
            } else if (distance >= 5 && distance <= 6) {
                toHit.addModifier(2, "target moved 5-6 hexes");
            } else if (distance >= 7 && distance <= 9) {
                toHit.addModifier(3, "target moved 7-9 hexes");
            } else if (distance >= 10) {
                toHit.addModifier(4, "target moved 10+ hexes");
            }
        } else {
            if (distance >= 3 && distance <= 4) {
               toHit.addModifier(1, "target moved 3-4 hexes");
            } else if (distance >= 5 && distance <= 6) {
               toHit.addModifier(2, "target moved 5-6 hexes");
            } else if (distance >= 7 && distance <= 9) {
               toHit.addModifier(3, "target moved 7-9 hexes");
            } else if (distance >= 10 && distance <= 13) {
               toHit.addModifier(4, "target moved 10-13 hexes");
            } else if (distance >= 14 && distance <=18) {
               toHit.addModifier(5, "target moved 14-18 hexes");
            } else if (distance >= 19 && distance <=24) {
               toHit.addModifier(6, "target moved 19-24 hexes");
            } else if (distance >= 25) {
               toHit.addModifier(7, "target moved 25+ hexes");
            }
        }

        if (jumped) {
            toHit.addModifier(1, "target jumped");
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

        // Only BattleMechs in water get the terrain penalty for firing!
        if (hex.terrainLevel(Terrains.WATER) > 0
                && (attacker instanceof Mech)) {
            toHit.addModifier(1, "attacker in water");
        }

        return toHit;
    }

    /**
     * Modifier to attacks due to target terrain
     * TODO:um....should VTOLs get modifiers for smoke, etc.
     */
    public static ToHitData getTargetTerrainModifier(IGame game, Targetable t) {
        Entity entityTarget = null;
        if (t.getTargetType() == Targetable.TYPE_ENTITY) {
            entityTarget = (Entity) t;
        }
        final IHex hex = game.getBoard().getHex(t.getPosition());
        
        boolean isVTOL = entityTarget==null? false : (entityTarget instanceof VTOL);

        ToHitData toHit = new ToHitData();
        
        // only entities get terrain bonuses
        // TODO: should this be changed for buildings???
        if (entityTarget == null) {
            return toHit;
        } else if (entityTarget.isMakingDfa()) {
            // you don't get terrain modifiers in midair
            // should be abstracted more into a 'not on the ground' 
            // flag for vtols and such
            return toHit;
        }

        // -1 bonus only against BattleMechs in water!
        if (hex.terrainLevel(Terrains.WATER) > 0
                && (entityTarget instanceof Mech)) {
            toHit.addModifier(-1, "target in water");
        }

        if (entityTarget.isStuck()) {
            toHit.addModifier(-2, "target stuck in swamp");
        }

        // Smoke and woods. With L3, the effects STACK.
        if (!game.getOptions().booleanOption("maxtech_fire")) { // L2
            if (hex.containsTerrain(Terrains.SMOKE)) {
                toHit.addModifier(2, "target in smoke");
            } else if ((hex.terrainLevel(Terrains.WOODS) == 1) && !isVTOL) {
                toHit.addModifier(1, "target in light woods");
            } else if ((hex.terrainLevel(Terrains.WOODS) > 1) && !isVTOL) {
                toHit.addModifier(2, "target in heavy woods");
            }
            return toHit;
        } else { // L3
            if (hex.terrainLevel(Terrains.SMOKE) == 1) {
                toHit.addModifier(1, "target in light smoke");
            } else if (hex.terrainLevel(Terrains.SMOKE) > 1) {
                toHit.addModifier(2, "target in heavy smoke");
            }
            
            if(!isVTOL) {
            
            if (hex.terrainLevel(Terrains.WOODS) == 1) {
                toHit.addModifier(1, "target in light woods");
            } else if (hex.terrainLevel(Terrains.WOODS) > 1) {
                toHit.addModifier(2, "target in heavy woods");
            }
            }
            return toHit;
        }
    }

    /**
     * Returns the weapon attack out of a list that has the highest expected damage
     */
    public static WeaponAttackAction getHighestExpectedDamage(IGame g, Vector vAttacks)
    {
    float fHighest = -1.0f;
        WeaponAttackAction waaHighest = null;
        for (int x = 0, n = vAttacks.size(); x < n; x++) {
            WeaponAttackAction waa = (WeaponAttackAction)vAttacks.elementAt(x);
            float fDanger = getExpectedDamage(g, waa);
            if (fDanger > fHighest) {
                fHighest = fDanger;
                waaHighest = waa;
            }
        }
        return waaHighest;
    }

    // store these as constants since the tables will never change
    private static float[] expectedHitsByRackSize = { 0.0f, 1.0f, 1.58f, 2.0f, 2.63f, 3.17f,
            4.0f, 4.49f, 4.98f, 5.47f, 6.31f, 7.23f, 8.14f, 8.59f, 9.04f, 9.5f, 10.1f, 10.8f, 11.42f,
            12.1f, 12.7f };

    /**
     * Determines the expected damage of a weapon attack, based on to-hit, salvo sizes, etc.
     */
    public static float getExpectedDamage(IGame g, WeaponAttackAction waa)
    {

        boolean use_table = false;

        AmmoType loaded_ammo = new AmmoType();

        Entity attacker = g.getEntity(waa.getEntityId());
        Infantry inf_attacker = new Infantry();
        BattleArmor ba_attacker = new BattleArmor();
        Mounted weapon = attacker.getEquipment(waa.getWeaponId());
        Mounted lnk_guide;

        ToHitData hitData = waa.toHit(g);

        if (attacker instanceof BattleArmor){
            ba_attacker = (BattleArmor) g.getEntity(waa.getEntityId());
        }
        if ((attacker instanceof Infantry) && !(attacker instanceof BattleArmor)){
            inf_attacker = (Infantry) g.getEntity(waa.getEntityId());
        }

        float fDamage = 0.0f;
        WeaponType wt = (WeaponType) weapon.getType();

        if (hitData.getValue() == ToHitData.IMPOSSIBLE || hitData.getValue() == ToHitData.AUTOMATIC_FAIL) {
            return 0.0f;
        }

        float fChance = 0.0f;
        if (hitData.getValue() == ToHitData.AUTOMATIC_SUCCESS) {
            fChance = 1.0f;
        } else {
            fChance = (float)oddsAbove(hitData.getValue()) / 100.0f;
        }

        // Missiles, LBX cluster rounds, and ultra/rotary cannons (when spun up) use the missile hits table
        if (wt.getDamage() == WeaponType.DAMAGE_MISSILE){
            use_table = true;
        }
        if (wt.getAmmoType() == AmmoType.T_AC_LBX){
            loaded_ammo = (AmmoType) weapon.getLinked().getType();
            if (loaded_ammo.getMunitionType() == AmmoType.M_CLUSTER){
                use_table = true;
            }
        }
        
        if ((wt.getAmmoType() == AmmoType.T_AC_ULTRA) || (wt.getAmmoType() == AmmoType.T_AC_ROTARY)){
            if ((weapon.curMode().getName() == "Ultra") || (weapon.curMode().getName() == "2-shot") ||
                    (weapon.curMode().getName() == "4-shot") || (weapon.curMode().getName() == "6-shot")) {
                use_table = true;
            }
        }

// Kinda cheap, but lets use the missile hits table for Battle armor weapons too

        if (wt.hasFlag(WeaponType.F_BATTLEARMOR)){
            use_table = true;
        }

        if (use_table == true) {
            if (weapon.getLinked() == null) return 0.0f;
            AmmoType at = (AmmoType)weapon.getLinked().getType();

            float fHits = 0.0f;
            fHits = expectedHitsByRackSize[wt.getRackSize()];
            if ((wt.getAmmoType() == AmmoType.T_SRM_STREAK) || (wt.getAmmoType() == AmmoType.T_LRM_STREAK)) {
                fHits = wt.getRackSize();
            }
            if (wt.getRackSize() == 40 || wt.getRackSize() == 30) {
                fHits = 2.0f * expectedHitsByRackSize[wt.getRackSize() / 2];
            }
            if ((wt.getAmmoType() == AmmoType.T_AC_ULTRA) || (wt.getAmmoType() == AmmoType.T_AC_ROTARY)){
                if ((weapon.curMode().getName() == "Ultra") || (weapon.curMode().getName() == "2-shot")){
                    fHits = expectedHitsByRackSize[2];
                }
                if (weapon.curMode().getName() == "4-shot"){
                    fHits = expectedHitsByRackSize[4];
                }
                if (weapon.curMode().getName() == "6-shot"){
                    fHits = expectedHitsByRackSize[6];
                }
            }
            // Most Battle Armor units have a weapon per trooper
            if (attacker instanceof BattleArmor){
                if (wt.getDamage() == WeaponType.DAMAGE_MISSILE){
                    fHits *= ba_attacker.getShootingStrength();
                }
                else {
                fHits = expectedHitsByRackSize[ba_attacker.getShootingStrength()];
                }
            }

            // If there is no ECM coverage to the target, guidance systems are good for another 1.20x damage
            if (!isAffectedByECM(attacker, attacker.getPosition(), g.getEntity(waa.getTargetId()).getPosition())){

                // Check for linked artemis guidance system
                if (wt.getAmmoType() == AmmoType.T_LRM ||
                    wt.getAmmoType() == AmmoType.T_SRM) {

                    lnk_guide = weapon.getLinkedBy();
                    if (lnk_guide != null && lnk_guide.getType() instanceof MiscType && !lnk_guide.isDestroyed() &&
                            !lnk_guide.isMissing() && !lnk_guide.isBreached() &&
                            lnk_guide.getType().hasFlag(MiscType.F_ARTEMIS) ) {

                            // Don't use artemis if this is indirect fire
                            //-> HACK! Artemis-specific ammo should be used for this, NOT standard ammo!
                            //-> Hook for Artemis V Level 3 Clan tech here; use 1.30f multiplier when implemented
                            if ((!weapon.curMode().equals("Indirect")) &&
                                    at.getMunitionType() == AmmoType.M_STANDARD){
                                fHits *= 1.2f;
                            }
                    }
                }

                // Check for ATMs, which have built in Artemis
                if (wt.getAmmoType() == AmmoType.T_ATM){
                    fHits *= 1.2f;
                }

                // Check for target with attached Narc or iNarc homing pod from friendly unit
                if (g.getEntity(waa.getTargetId()).isNarcedBy(attacker.getOwner().getTeam()) || 
                         g.getEntity(waa.getTargetId()).isINarcedBy(attacker.getOwner().getTeam())) {
                    if (at.getMunitionType() == AmmoType.M_NARC_CAPABLE){
                        fHits *= 1.2f;
                    }
                }
            }

            // adjust for previous AMS
            if (wt.getDamage() == WeaponType.DAMAGE_MISSILE){
                Vector vCounters = waa.getCounterEquipment();
                if (vCounters != null) {
                    for (int x = 0; x < vCounters.size(); x++) {
                        Mounted counter = (Mounted)vCounters.elementAt(x);
                        if (counter.getType() instanceof WeaponType &&
                                counter.getType().hasFlag(WeaponType.F_AMS)) {
                            float fAMS = 3.5f * ((WeaponType)counter.getType()).getDamage();
                            fHits = Math.max(0.0f, fHits - fAMS);
                        }
                    }
                }
            }

            fDamage = fHits * (float) at.getDamagePerShot();
            if ((wt.getAmmoType() == AmmoType.T_AC_ULTRA) || (wt.getAmmoType() == AmmoType.T_AC_ROTARY)){
                fDamage = fHits * (float) wt.getDamage();
            }
        } else {

            // Direct fire weapons (and LBX slug rounds) just do a single shot so they don't use the missile hits table
            fDamage = (float) wt.getDamage();

            // Infantry follow some special rules, but do fixed amounts of damage                   
            // Anti-mek attacks are weapon-like in nature, so include them here as well
            if (attacker instanceof Infantry){
                if (wt.getInternalName() == Infantry.LEG_ATTACK){
                    fDamage = 10.0f; // Actually 5, but the chance of crits deserves a boost
                }

                if (inf_attacker.isPlatoon()){
                    if (wt.getInternalName() == Infantry.SWARM_MEK){
                        // If the target is a Mek that is not swarmed, this is a good thing
                        if ( (g.getEntity(waa.getTargetId()).getSwarmAttackerId() == Entity.NONE) && 
                         (g.getEntity(waa.getTargetId()) instanceof Mech)){
                            fDamage = 1.5f * (float) inf_attacker.getDamage(inf_attacker.getShootingStrength());
                        }
                        // Otherwise, call it 0 damage
                        else {
                            fDamage = 0.0f;
                        }
                    }

                    else {
                        // conventional weapons; field guns should be handled under the standard weapons section
                        fDamage = (float) inf_attacker.getDamage(inf_attacker.getShootingStrength());
                    }

                }
                else {
                    // Battle armor units conducting swarm attack
                    if (wt.getInternalName() == Infantry.SWARM_MEK){
                        // If the target is a Mek that is not swarmed, this is a good thing
                        if ( (g.getEntity(waa.getTargetId()).getSwarmAttackerId() == Entity.NONE) &&
                         (g.getEntity(waa.getTargetId()) instanceof Mech)){
                            // Overestimated, but the chance at crits and head shots deserves a boost
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
        if ((g.getEntity(waa.getTargetId()) instanceof Infantry) && 
                !(g.getEntity(waa.getTargetId()) instanceof BattleArmor)){
            IHex e_hex = g.getBoard().getHex(g.getEntity(waa.getTargetId()).getPosition().x,
                g.getEntity(waa.getTargetId()).getPosition().y);
            if (!e_hex.containsTerrain(Terrains.WOODS) && !e_hex.containsTerrain(Terrains.BUILDING)) {
                fDamage *= 2.0f;
            }
        } 
        return fDamage;
    }

    /**
     * If the unit is carrying multiple types of ammo for the specified weapon,
     * cycle through them and choose the type best suited to engage the specified target
     * Value returned is expected damage
     * Note that some ammo types, such as infernos, do no damage or have special properties and so the damage
     * is an estimation of effectiveness
     */

    public static double getAmmoAdjDamage(IGame cgame, WeaponAttackAction atk){

        boolean no_bin = true;
        boolean multi_bin = false;

        int bin_count = 0;
        int weapon_count = 0;

        double ammo_multiple, ex_damage, max_damage;
    
        Enumeration ammo_bin_list, target_weapons;
    
        Entity shooter, target;
    
        Mounted abin, fabin, best_bin;
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
        if (wtype.hasFlag(WeaponType.F_ENERGY) || wtype.hasFlag(WeaponType.F_ONESHOT) || 
                wtype.hasFlag(WeaponType.F_INFANTRY)){
            return getExpectedDamage(cgame, atk);
        }
    
        // Get a list of ammo bins and the first valid bin
        fabin = null;
        best_bin = null;
        ammo_bin_list = shooter.getAmmo();

        while (ammo_bin_list.hasMoreElements()){
            abin = (Mounted) ammo_bin_list.nextElement();
            if (shooter.loadWeapon(shooter.getEquipment(atk.getWeaponId()), abin)){
                if (abin.getShotsLeft() > 0){
                    abin_type = (AmmoType) abin.getType();
                    if (!AmmoType.canDeliverMinefield(abin_type)){
                        fabin = abin;
                        fabin_type = (AmmoType) fabin.getType();
                        break;
                    }
                }
            }
        }
    
        // To save processing time, lets see if we have more than one type of bin
        // Thunder-type ammos and empty bins are excluded from the list
        ammo_bin_list = shooter.getAmmo();
        while (ammo_bin_list.hasMoreElements()){
            abin = (Mounted) ammo_bin_list.nextElement();
            if (shooter.loadWeapon(shooter.getEquipment(atk.getWeaponId()), abin)){
                if (abin.getShotsLeft() > 0){
                    abin_type = (AmmoType) abin.getType();
                    if (!AmmoType.canDeliverMinefield(abin_type)){
                        bin_count++;
                        no_bin = false;
                        if (abin_type.getMunitionType() != fabin_type.getMunitionType()){
                            multi_bin = true;
                            break;
                        }
                    }
                }
            }
        }

        // If no_bin is true, then either all bins are empty or contain Thunder-type rounds and
        // we can safely say that the expected damage is 0.0
        // If no_bin is false, then we have at least one good bin
        if (no_bin){
            return 0.0;
        } else {
            // If multi_bin is true, then multiple ammo types are present and an appropriate type must be selected
            // If multi_bin is false, then all bin types are the same; skip down to getting the expected damage
            if (!multi_bin){
                return getExpectedDamage(cgame, atk);
            }
            if (multi_bin){

                // Set default max damage as 0, and the best bin as the first bin
                max_damage = 0.0;
                best_bin = fabin;

                // Reload list of ammo bins
                ammo_bin_list = shooter.getAmmo();

                // For each valid ammo bin
                while (ammo_bin_list.hasMoreElements()){
                    abin = (Mounted) ammo_bin_list.nextElement();
                    if (shooter.loadWeapon(shooter.getEquipment(atk.getWeaponId()), abin)){
                        if (abin.getShotsLeft() > 0){
                            abin_type = (AmmoType) abin.getType();
                            if (!AmmoType.canDeliverMinefield(abin_type)){

                                // Load weapon with specified bin
                                shooter.loadWeapon(shooter.getEquipment(atk.getWeaponId()), abin);
                                atk.setAmmoId(shooter.getEquipmentNum(abin));

                                // Get expected damage
                                ex_damage = getExpectedDamage(cgame, atk);

                                // Calculate any modifiers due to ammo type
                                ammo_multiple = 1.0;

                                // Frag missiles, flechette AC rounds do double damage against conventional infantry
                                // and 0 damage against everything else
                                // Any further anti-personnel specialized rounds should be tested for here
                                if ((abin_type.getMunitionType() == AmmoType.M_FRAGMENTATION) ||
                                    (abin_type.getMunitionType() == AmmoType.M_FLECHETTE)){
                                    ammo_multiple = 0.0;
                                    if (target instanceof Infantry){
                                        if (!(target instanceof BattleArmor)) {
                                            ammo_multiple = 2.0;
                                        }
                                    }
                                }

                                // LBX cluster rounds work better against units with little armor, vehicles, and Meks in partial cover
                                // Other ammo that deliver lots of small submunitions should be tested for here too
                                if (abin_type.getMunitionType() == AmmoType.M_CLUSTER){
                                    if (target.getArmorRemainingPercent() <= 0.25) {
                                        ammo_multiple = 1.0 + (wtype.getRackSize()/10);
                                    }
                                    if (target instanceof Tank){
                                        ammo_multiple += 1.0;
                                    }
                                }

                                // AP autocannon rounds work much better against Meks and vehicles than infantry,
                                // give a damage boost in proportion to calibre to reflect scaled crit chance
                                // Other armor-penetrating ammo types should be tested here, such as Tandem-charge SRMs
                                if (abin_type.getMunitionType() == AmmoType.M_ARMOR_PIERCING){
                                    if ((target instanceof Mech) || (target instanceof Tank)){
                                        ammo_multiple = 1.0 + (wtype.getRackSize()/10);
                                    }
                                    if (target instanceof Infantry){
                                        ammo_multiple = 0.6;
                                    }
                                }

                                // Inferno SRMs work better against overheating Meks that are not/almost not on fire,
                                // and against vehicles and protos if allowed by game option
                                if (abin_type.getMunitionType() == AmmoType.M_INFERNO){
                                    ammo_multiple = 0.5;
                                    if (target instanceof Mech){
                                        if ((target.infernos.getTurnsLeftToBurn() < 4) && (target.heat >= 5)){
                                            ammo_multiple = 1.1;
                                        }
                                    }
                                    if ((target instanceof Tank) &&
                                        !(cgame.getOptions().booleanOption("vehicles_safe_from_infernos"))){
                                            ammo_multiple = 1.1;
                                    }
                                    if ((target instanceof Protomech) &&
                                        !(cgame.getOptions().booleanOption("protos_safe_from_infernos"))){
                                            ammo_multiple = 1.1;
                                    }
                                }

                                // Narc beacon doesn't really do damage but if the target is not infantry and doesn't have
                                // one, give 'em one by making it an attractive option
                                if ((wtype.getAmmoType() == AmmoType.T_NARC) && (abin_type.getMunitionType() == AmmoType.M_STANDARD)){
                                    if (!(target.isNarcedBy(shooter.getOwner().getTeam())) && 
                                        !(target instanceof Infantry)){
                                        ex_damage = 5.0;
                                    } else {
                                        ex_damage = 0.5;
                                    }
                                }

                                // iNarc beacon doesn't really do damage, but if the target is not infantry and doesn't have
                                // one, give 'em one by making it an attractive option
                                if (wtype.getAmmoType() == AmmoType.T_INARC){
                                    if ((abin_type.getMunitionType() == AmmoType.M_STANDARD) && 
                                            !(target instanceof Infantry)){
                                        if (!(target.isINarcedBy(shooter.getOwner().getTeam()))) {
                                            ex_damage = 7.0;
                                        } else {
                                            ex_damage = 1.0;
                                        }
                                    }

                                    // iNarc ECM doesn't really do damage, but if the target has a C3 link or missile launchers
                                    // make it a priority
                                    // Checking for actual ammo types carried would be nice, but can't be sure of exact loads
                                    // when "true" double blind is implemented
                                    if ((abin_type.getMunitionType() == AmmoType.M_ECM) &&
                                        !(target instanceof Infantry)) {
                                        if (!target.isINarcedWith(AmmoType.M_ECM)) {
                                            if (!(target.getC3MasterId() == Entity.NONE) ||
                                                target.hasC3M() || target.hasC3MM() ||
                                                target.hasC3i()) {
                                                ex_damage = 8.0;
                                            } else {
                                                ex_damage = 0.5;
                                            }
                                            target_weapons = target.getWeapons();
                                            while (target_weapons.hasMoreElements()){
                                                target_weapon = (WeaponType) ((Mounted)target_weapons.nextElement()).getType();
                                                if ((target_weapon.getAmmoType() == AmmoType.T_LRM) ||
                                                    (target_weapon.getAmmoType() == AmmoType.T_SRM)){
                                                    ex_damage = ex_damage + (target_weapon.getRackSize()/2);
                                                }
                                            }
                                        }
                                    }

                                    // iNarc Nemesis doesn't really do damage, but if the target is not infantry and doesn't have
                                    // one give it a try; make fast units a priority because they are usually out front
                                    if ((abin_type.getMunitionType() == AmmoType.M_NEMESIS) && 
                                            !(target instanceof Infantry)){
                                        if (!target.isINarcedWith(AmmoType.M_NEMESIS)) {
                                            ex_damage = (double) (target.getOriginalWalkMP() + target.getOriginalJumpMP())/2;
                                        } else {
                                            ex_damage = 0.5;
                                        }
                                    }
                                }

                                // If the adjusted damage is highest, store the damage and bin
                                if ((ex_damage * ammo_multiple) > max_damage){
                                    max_damage = ex_damage * ammo_multiple;
                                    best_bin = abin;
                                }
                            }
                        }
                    }
                }

                // Now that the best bin has been found, reload the weapon with it
                shooter.loadWeapon(shooter.getEquipment(atk.getWeaponId()), best_bin);
                atk.setAmmoId(shooter.getEquipmentNum(best_bin));
            }
        }
        return max_damage;
    }



    /**
     * If this is an ultra or rotary cannon, lets see about
     *   'spinning it up' for extra damage
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

        if (!((wtype.getAmmoType() == AmmoType.T_AC_ULTRA) || (wtype.getAmmoType() == AmmoType.T_AC_ROTARY))){
            return 0;
        }

        // Get the to-hit number
        threshold = atk.toHit(cgame).getValue();

        // Set the weapon to single shot mode
        weapon.setMode("Single");
        final_spin = 0;

        // If weapon can't hit target, exit the function with the weapon on single shot
        if ((threshold == ToHitData.IMPOSSIBLE) || (threshold == ToHitData.AUTOMATIC_FAIL)) {
            return final_spin;
        }

        // Set a random 2d6 roll
        test = d6(2);

        // If random roll is >= to-hit + 1, then set double-spin
        if (test >= threshold + 1){
            final_spin = 1;
            if (wtype.getAmmoType() == AmmoType.T_AC_ULTRA){
                weapon.setMode("Ultra");
            }
            if (wtype.getAmmoType() == AmmoType.T_AC_ROTARY){
                weapon.setMode("2-shot");
            }
        }

        // If this is a Rotary cannon
        if (wtype.getAmmoType() == AmmoType.T_AC_ROTARY){

            // If random roll is >= to-hit + 2 then set to quad-spin
            if (test >= threshold + 2){
                final_spin = 2;
                weapon.setMode("4-shot");
            }

            // If random roll is >= to-hit + 3 then set to six-spin
            if (test >= threshold + 3){
                final_spin = 3;
                weapon.setMode("6-shot");
            }
        }
        return final_spin;
    }




    /**
     * Checks to see if a target is in arc of the specified
     * weapon, on the specified entity
     */
    public static boolean isInArc(IGame game, int attackerId, int weaponId, Targetable t) {
        Entity ae = game.getEntity(attackerId);
        int facing = ae.isSecondaryArcWeapon(weaponId) ? ae.getSecondaryFacing() : ae.getFacing();
        return isInArc(ae.getPosition(), facing, t.getPosition(), ae.getWeaponArc(weaponId));
    }

    /**
     * Returns true if the target is in the specified arc.
     * @param src the attacker coordinate
     * @param facing the appropriate attacker sfacing
     * @param dest the target coordinate
     * @param arc the arc
     */
    public static boolean isInArc(Coords src, int facing, Coords dest, int arc) {
        // calculate firing angle
        int fa = src.degree(dest) - facing * 60;
        if (fa < 0) {
            fa += 360;
        }
        // is it in the specifed arc?
        switch(arc) {
        case ARC_FORWARD :
            return fa >= 300 || fa <= 60;
        case Compute.ARC_RIGHTARM :
            return fa >= 300 || fa <= 120;
        case Compute.ARC_LEFTARM :
            return fa >= 240 || fa <= 60;
        case ARC_REAR :
            return fa > 120 && fa < 240;
        case ARC_RIGHTSIDE :
            return fa > 60 && fa <= 120;
        case ARC_LEFTSIDE :
            return fa < 300 && fa >= 240;
        case ARC_MAINGUN:
            return fa >= 240 || fa <= 120;
        case ARC_360 :
            return true;
        default:
            return false;
        }
    }

    /**
     * LOS check from ae to te.
     */
    public static boolean canSee(IGame game, Entity ae, Targetable target)
    {
        return LosEffects.calculateLos(game, ae.getId(), target).canSee()
            && ae.getCrew().isActive();
    }

    public static int targetSideTable(Entity attacker, Targetable target) {
        if (target.getTargetType() != Targetable.TYPE_ENTITY) {
            return ToHitData.SIDE_FRONT;
        }
        Entity te = (Entity)target;
        return targetSideTable(attacker.getPosition(), te.getPosition(), te.getFacing(), te instanceof Tank);
    }

    /**
     * Returns the side location table that you should be using
     */
    public static int targetSideTable(Coords src, Coords dest, int targetFacing, boolean targetIsTank) {
        // calculate firing angle
        int fa = (dest.degree(src) + (6 - targetFacing) * 60) % 360;

        if (targetIsTank) {
            if (fa > 30 && fa <= 150) {
                return ToHitData.SIDE_RIGHT;
            } else if (fa > 150 && fa < 210) {
                return ToHitData.SIDE_REAR;
            } else if (fa >= 210 && fa < 330) {
                return ToHitData.SIDE_LEFT;
            } else {
                return ToHitData.SIDE_FRONT;
            }
        } else {
            if (fa > 90 && fa <= 150) {
                return ToHitData.SIDE_RIGHT;
            } else if (fa > 150 && fa < 210) {
                return ToHitData.SIDE_REAR;
            } else if (fa >= 210 && fa < 270) {
                return ToHitData.SIDE_LEFT;
            } else {
                return ToHitData.SIDE_FRONT;
            }
        }
    }

    /**
     * Returns the club a mech possesses, or null if none.
     */
    public static Mounted clubMechHas(Entity entity) {
        for (Enumeration i = entity.getMisc(); i.hasMoreElements();) {
            Mounted mounted = (Mounted)i.nextElement();
            if (mounted.getType().hasFlag(MiscType.F_CLUB) || mounted.getType().hasFlag(MiscType.F_HATCHET) || mounted.getType().hasFlag(MiscType.F_SWORD)) {
                return mounted;
            }
        }
        return null;
    }

    /**
     * Maintain backwards compatability.
     *
     * @param   missiles - the <code>int</code> number of missiles in the pack.
     */
    public static int missilesHit(int missiles) {
        return missilesHit(missiles, 0);
    }

    /**
     * Roll the number of missiles (or whatever) on the missile
     * hit table, with the specified mod to the roll.
     *
     * @param   missiles - the <code>int</code> number of missiles in the pack.
     * @param   nMod - the <code>int</code> modifier to the roll for number
     *          of missiles that hit.
     */
    public static int missilesHit(int missiles, int nMod, boolean maxtech) {
        int nRoll = d6(2) + nMod;
        int minimum = maxtech ? 1 : 2;
        nRoll = Math.min(Math.max(nRoll, minimum), 12);

        if (maxtech && nRoll == 1) {
            return 1;
        }
        if (nRoll<2) {
            nRoll = 2;
        }

        final int[][] hits = new int[][] {
        {2,1,1,1,1,1,1,2,2,2,2,2},
        {3,1,1,1,2,2,2,2,2,3,3,3},
        {4,1,2,2,2,2,3,3,3,3,4,4},
        {5,1,2,2,3,3,3,3,4,4,5,5},
        {6,2,2,3,3,4,4,4,5,5,6,6},
        {8,2,3,3,4,4,5,5,6,7,8,8},
        {9,3,3,4,5,5,5,5,7,7,9,9},
        {10,3,3,4,6,6,6,6,8,8,10,10},
        {12,4,4,5,8,8,8,8,10,10,12,12},
        {15,5,5,6,9,9,9,9,12,12,15,15},
        {20,6,6,9,12,12,12,12,16,16,20,20}};

        for (int i = 0; i < hits.length; i++) {
            if (hits[i][0] >= missiles) {
                return Math.min(missiles, hits[i][nRoll-1]);
            }
        }
        throw new RuntimeException("Could not find number of missles in hit table");
    }

    public static int missilesHit (int missiles, int nMod) {
        return missilesHit (missiles, nMod, false);
    }


    /**
     * Returns the consciousness roll number
     *
     * @param hit - the <code>int</code> number of the crew hit currently
     *    being rolled.
     * @return  The <code>int</code> number that must be rolled on 2d6
     *    for the crew to stay conscious.
     */
    public static int getConsciousnessNumber(int hit) {
        switch(hit) {
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
     * Checks to see if a line passing from Coords A to Coords B is intercepted
     * by an ECM field generated by an enemy of Entity AE.
     */
    public static boolean isAffectedByECM(Entity ae, Coords a, Coords b) {

        if (a == null || b == null) return false;
        
        if (ae.isINarcedWith( INarcPod.ECM )) {
            return true;
        }

        // Only grab enemies with active ECM
        Vector vEnemyCoords = new Vector(16);
        Vector vECMRanges = new Vector(16);
        for (Enumeration e = ae.game.getEntities(); e.hasMoreElements(); ) {
            Entity ent = (Entity)e.nextElement();
            Coords entPos = ent.getPosition();
            if (ent.isEnemyOf(ae) && ent.hasActiveECM() && entPos != null) {
                // TODO : only use the best ECM range in a given Coords.
                vEnemyCoords.addElement( entPos );
                vECMRanges.addElement( new Integer(ent.getECMRange()) );
            }

            // Check the ECM effects of the entity's passengers.
            Vector passengers = ent.getLoadedUnits();
            Enumeration iter = passengers.elements();
            while ( iter.hasMoreElements() ) {
                Entity other = (Entity) iter.nextElement();
                if (other.isEnemyOf(ae) && other.hasActiveECM() && entPos != null) {
                    // TODO : only use the best ECM range in a given Coords.
                    vEnemyCoords.addElement( entPos );
                    vECMRanges.addElement( new Integer(other.getECMRange()) );
                }
            }

        }

        // none?  get out of here
        if (vEnemyCoords.size() == 0) return false;

        // get intervening Coords.  See the comments for intervening() and losDivided()
        Coords[] coords = Coords.intervening(a, b);
        boolean bDivided = (a.degree(b) % 60 == 30);
        Enumeration ranges = vECMRanges.elements();
        for (Enumeration e = vEnemyCoords.elements(); e.hasMoreElements(); ) {
            Coords c = (Coords)e.nextElement();
            int range = ( (Integer) ranges.nextElement() ).intValue();
            int nLastDist = -1;

            // loop through intervening hexes and see if any of them are within range
            for (int x = 0; x < coords.length; x++) {
                int nDist = c.distance(coords[x]);

                if ( nDist <= range ) return true;

                // optimization.  if we're getting farther away, forget it
                // but ignore the doubled-up hexes intervening() adds along hexlines
                if (!bDivided || (x % 3 == 0)) {
                    if (nLastDist == -1) {
                        nLastDist = nDist;
                    }
                    else if (nDist > nLastDist) {
                        break;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Get the base to-hit number of a Leg Attack by the given attacker upon
     * the given defender
     *
     * @param   attacker - the <code>Entity</code> conducting the leg attack.
     * @param   defender - the <code>Entity</code> being attacked.
     * @return  The base <code>ToHitData</code> of the attack.
     */
    public static ToHitData getLegAttackBaseToHit( Entity attacker,
                                                   Entity defender ) {
        int men = 0;
        int base = ToHitData.IMPOSSIBLE;
        StringBuffer reason = new StringBuffer();
        final int nightModifier = (attacker.game.getOptions().booleanOption("night_battle")) ? +2 : 0;

        // Can only attack a Mek's legs.
        if ( !(defender instanceof Mech) ) {
            reason.append( "Defender is not a Mek." );
        }

        // Can't target a transported entity.
        else if ( Entity.NONE != defender.getTransportId() ) {
            reason.append( "Target is a passenger." );
        }

        // Can't target a entity conducting a swarm attack.
        else if ( Entity.NONE != defender.getSwarmTargetId() ) {
            reason.append("Target is swarming a Mek.");
        }

        // Attacker can't be swarming.
        else if ( Entity.NONE != attacker.getSwarmTargetId() ) {
            reason.append( "Attacker is swarming." );
        }

        // Handle BattleArmor attackers.
        else if ( attacker instanceof BattleArmor ) {
            BattleArmor inf = (BattleArmor) attacker;

            // Battle Armor units can't Leg Attack if they're burdened.
            if ( inf.isBurdened() ) {
                reason.append( "Launcher not jettisoned." );
            } else {
                men = inf.getShootingStrength();
                if ( men >= 4 ) base = 4;
                else if ( men >= 3 ) base = 7;
                else if ( men >= 2 ) base = 10;
                else if ( men >= 1 ) base = 12;
                reason.append( men );
                reason.append( " trooper(s) active" );
            }
        }

        // Non-BattleArmor infantry need many more men.
        else if ( attacker instanceof Infantry ) {
            Infantry inf = (Infantry) attacker;
            men = inf.getShootingStrength();
            if ( men >= 22 ) base = 4;
            else if ( men >= 16 ) base = 7;
            else if ( men >= 10 ) base = 10;
            else if ( men >= 5 ) base = 12;
            reason.append( men );
            reason.append( " men alive" );
        }

        if (nightModifier>0) {
            base += nightModifier;
            reason.append ("Night Battle, no Spotlights");
        }
        
        // No one else can conduct leg attacks.
        else {
            reason.append( "Attacker is not infantry." );
        }

        // Return the ToHitData for this attack.
        // N.B. we attack the legs.
        return new ToHitData( base, reason.toString(),
                              ToHitData.HIT_KICK, ToHitData.SIDE_FRONT );
    }

    /**
     * Get the base to-hit number of a Swarm Mek by the given attacker upon
     * the given defender.
     *
     * @param   attacker - the <code>Entity</code> swarming.
     * @param   defender - the <code>Entity</code> being swarmed.
     * @return  The base <code>ToHitData</code> of the mek.
     */
    public static ToHitData getSwarmMekBaseToHit( Entity attacker,
                                                  Entity defender ) {
        int men = 0;
        int base = ToHitData.IMPOSSIBLE;
        StringBuffer reason = new StringBuffer();
        final int nightModifier = (attacker.game.getOptions().booleanOption("night_battle")) ? +2 : 0;

        // Can only swarm a Mek.
        if ( !(defender instanceof Mech) ) {
            reason.append( "Defender is not a Mek." );
        }

        // Can't target a transported entity.
        else if ( Entity.NONE != defender.getTransportId() ) {
            reason.append("Target is a passenger.");
        }

        // Attacker can't be swarming.
        else if ( Entity.NONE != attacker.getSwarmTargetId() ) {
            reason.append( "Attacker is swarming." );
        }

        // Can't target a entity invloved in a swarm attack.
        else if ( Entity.NONE != defender.getSwarmAttackerId() ) {
            reason.append("Target is already being swarmed.");
        }

        // Can't target a entity conducting a swarm attack.
        else if ( Entity.NONE != defender.getSwarmTargetId() ) {
            reason.append("Target is swarming a Mek.");
        }

        // Can't swarm a friendly Mek.
        // Seehttp://www.classicbattletech.com/w3t/showflat.php?Cat=&Board=ask&Number=632321&page=0&view=collapsed&sb=5&o=0&fpart=
        else if ( !attacker.isEnemyOf( defender ) ) {
            reason.append( "Can only swarm an enemy." );
        }

        // Handle BattleArmor attackers.
        else if ( attacker instanceof BattleArmor ) {
            BattleArmor inf = (BattleArmor) attacker;

            // Battle Armor units can't Leg Attack if they're burdened.
            if ( inf.isBurdened() ) {
                reason.append( "Launcher not jettisoned." );
            } else {
                men = inf.getShootingStrength();
                if ( men >= 4 ) base = 7;
                else if ( men >= 1 ) base = 10;
                reason.append( men );
                reason.append( " trooper(s) active" );
            }
        }

        // Non-BattleArmor infantry need many more men.
        else if ( attacker instanceof Infantry ) {
            Infantry inf = (Infantry) attacker;
            men = inf.getShootingStrength();
            if ( men >= 22 ) base = 7;
            else if ( men >= 16 ) base = 10;
            reason.append( men );
            reason.append( " men alive" );
        }

        // No one else can conduct leg attacks.
        else {
            reason.append( "Attacker is not infantry." );
        }

        if (nightModifier>0) {
            base += nightModifier;
            reason.append ("Night Battle, no Spotlights");
        }

        // Return the ToHitData for this attack.
        return new ToHitData( base, reason.toString() );
    }

    /**
     * Determine the number of shots from a Battle Armor unit's attack hit.
     *
     * @param   shots - the <code>int</code> number of shots from the unit.
     * @return  the <code>int</code> number of shots that hit the target.
     */
    public static int getBattleArmorHits( int shots ) {
        int nRoll = d6(2);
        
        if (shots == 1) {
            return 1;
        }
        
        if (shots > 5) {
            throw new IllegalArgumentException("shots were greater than 5");
        }
        
        final int[][] hit_table = 
        {{1,1,1,1,1,2,2,2,2,2,2},
         {1,1,2,2,2,2,2,3,3,3,3},
         {1,2,2,2,2,3,3,3,4,4,4},
         {1,2,2,3,3,3,4,4,4,5,5}};
        
        return hit_table[shots - 2][nRoll - 2];
    }

    public static boolean canPhysicalTarget(IGame game, int entityId, Targetable target) {
        boolean canHit = false;

        canHit |= PunchAttackAction.toHit
            ( game, entityId, target,
             PunchAttackAction.LEFT ).getValue()
            != ToHitData.IMPOSSIBLE;

        canHit |= PunchAttackAction.toHit
            ( game, entityId, target,
             PunchAttackAction.RIGHT ).getValue()
            != ToHitData.IMPOSSIBLE;

        canHit |= KickAttackAction.toHit
            ( game, entityId, target,
             KickAttackAction.LEFT ).getValue()
            != ToHitData.IMPOSSIBLE;

        canHit |= KickAttackAction.toHit
            ( game, entityId, target,
             KickAttackAction.RIGHT ).getValue()
            != ToHitData.IMPOSSIBLE;

        if (game.getOptions().booleanOption("maxtech_mulekicks") &&
            game.getEntity(entityId) instanceof QuadMech) {
            canHit |= KickAttackAction.toHit
            ( game, entityId, target,
              KickAttackAction.LEFTMULE ).getValue()
            != ToHitData.IMPOSSIBLE;
            
            canHit |= KickAttackAction.toHit
            ( game, entityId, target,
              KickAttackAction.RIGHTMULE ).getValue()
            != ToHitData.IMPOSSIBLE;
        }
        
        canHit |= BrushOffAttackAction.toHit
            ( game, entityId, target,
              BrushOffAttackAction.LEFT ).getValue()
            != ToHitData.IMPOSSIBLE;

        canHit |= BrushOffAttackAction.toHit
            ( game, entityId, target,
              BrushOffAttackAction.RIGHT ).getValue()
            != ToHitData.IMPOSSIBLE;

        canHit |= new ThrashAttackAction(entityId, target).toHit(game).getValue()
            != ToHitData.IMPOSSIBLE;

        canHit |= ProtomechPhysicalAttackAction.toHit(game, entityId, target).getValue()
            != ToHitData.IMPOSSIBLE;

        Mounted club = Compute.clubMechHas( game.getEntity(entityId) );
        if ( null != club ) {
            canHit |= ClubAttackAction.toHit
                ( game, entityId, target,
                  club ).getValue()
                != ToHitData.IMPOSSIBLE;
        }

        canHit |= PushAttackAction.toHit
            ( game, entityId, target ).getValue()
            != ToHitData.IMPOSSIBLE;

        return canHit;
    }

    /**
     * Can movement between the two coordinates be on pavement (which includes
     * roads and bridges)?  If so it will override prohibited terrain, it may
     * change movement costs, and it may lead to skids.
     *
     * @param   game - the <code>IGame</code> object.
     * @param   src - the <code>Coords</code> being left.
     * @param   dest - the <code>Coords</code> being entered.
     * @return  <code>true</code> if movement between <code>src</code> and
     *          <code>dest</code> can be on pavement; <code>false</code>
     *          otherwise.
     */
    public static boolean canMoveOnPavement(IGame game,
                                             Coords src, Coords dest ) {
        final IHex srcHex = game.getBoard().getHex(src);
        final IHex destHex = game.getBoard().getHex(dest);
        final int src2destDir = src.direction(dest);
        final int dest2srcDir = (src2destDir + 3) % 6;
        boolean result = false;

        // We may be moving in the same hex.
        if ( src.equals(dest) &&
             ( srcHex.containsTerrain(Terrains.PAVEMENT) ||
               srcHex.containsTerrain(Terrains.ROAD) ||
               srcHex.containsTerrain(Terrains.BRIDGE) ) ) {
            result = true;
        }

        // If the source is a pavement hex, then see if the destination
        // hex is also a pavement hex or has a road or bridge that exits
        // into the source hex.
        else if ( srcHex.containsTerrain(Terrains.PAVEMENT) &&
             ( destHex.containsTerrain(Terrains.PAVEMENT) ||
               destHex.containsTerrainExit(Terrains.ROAD, dest2srcDir) ||
               destHex.containsTerrainExit(Terrains.BRIDGE, dest2srcDir) ) ) {
            result = true;
        }

        // See if the source hex has a road or bridge that exits into the
        // destination hex.
        else if ( srcHex.containsTerrainExit(Terrains.ROAD, src2destDir) ||
                  srcHex.containsTerrainExit(Terrains.BRIDGE, src2destDir) ) {
            result = true;
        }

        return result;
    }

  /**
   * Determines whether the attacker and the target are in the same
   * building.
   * @return true if the target can and does occupy the same building,
   * false otherwise.
   */
  public static boolean isInSameBuilding(IGame game, Entity attacker, Targetable target) {
    if (!(target instanceof Entity)) {
      return false;
    }
    Entity targetEntity = (Entity)target;
    if (!isInBuilding(game, attacker) || !isInBuilding(game, targetEntity)) {
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
     * @param   game - the <code>IGame</code> object.
     *          This value may be <code>null</code>.
     * @param   entity - the <code>Entity</code> to be checked.
     *          This value may be <code>null</code>.
     * @return  <code>true</code> if the entity is inside of the building
     *          at those coordinates.  <code>false</code> if there is no
     *          building at those coordinates or if the entity is on the
     *          roof or in the air above the building, or if any input
     *          argument is <code>null</code>.
     */
    public static boolean isInBuilding(IGame game, Entity entity ) {

        // No game, no building.
        if ( game == null ) {
            return false;
        }

        // Null entities can't be in a building.
        if ( entity == null ) {
            return false;
        }

        // Call the version of the function that requires coordinates.
        return isInBuilding( game, entity, entity.getPosition() );
    }

    /**
     * Determine if the given unit is inside of a building at the given
     * coordinates.
     *
     * @param   game - the <code>IGame</code> object.
     *          This value may be <code>null</code>.
     * @param   entity - the <code>Entity</code> to be checked.
     *          This value may be <code>null</code>.
     * @param   coords - the <code>Coords</code> of the building hex.
     *          This value may be <code>null</code>.
     * @return  <code>true</code> if the entity is inside of the building
     *          at those coordinates.  <code>false</code> if there is no
     *          building at those coordinates or if the entity is on the
     *          roof or in the air above the building, or if any input
     *          argument is <code>null</code>.
     */
    public static boolean isInBuilding(IGame game,
                                        Entity entity,
                                        Coords coords ) {

        // No game, no building.
        if ( game == null ) {
            return false;
        }

        // Null entities can't be in a building.
        if ( entity == null ) {
            return false;
        }

        // Null coordinates can't have buildings.
        if ( coords == null ) {
            return false;
        }

        // Get the Hex at those coordinates.
        final IHex curHex = game.getBoard().getHex( coords );

        return isInBuilding(game, entity.elevationOccupied(curHex), coords);
    }

    static boolean isInBuilding(IGame game, int entityElev, Coords coords ) {

        // Get the Hex at those coordinates.
        final IHex curHex = game.getBoard().getHex( coords );

        // The entity can't be inside of a building that isn't there.
        if ( !curHex.containsTerrain( Terrains.BLDG_ELEV ) ) {
            return false;
        }

        // Get the elevations occupied by the building.
        int surface = curHex.surface();
        int bldgHeight = curHex.terrainLevel( Terrains.BLDG_ELEV );
        int basement = 0;
        if ( curHex.containsTerrain( Terrains.BLDG_BASEMENT ) ) {
            basement = curHex.terrainLevel( Terrains.BLDG_BASEMENT );
        }

        // Return true if the entity is in the range of building elevations.
        if ( entityElev >= (surface - basement) &&
             entityElev < (surface + bldgHeight) ) {
            return true;
        }

        // Entity is not *inside* of the building.
        return false;
    }

    public static Coords scatter(Coords coords, int margin) {
        int scatterDirection = d6(1) - 1;
        int scatterDistance = 0;
        if (margin > 0) {
            scatterDistance = margin;
        } else {
            scatterDistance = d6(1);
        }

        for (int i = 0; i < scatterDistance; i++) {
            coords = coords.translated(scatterDirection);
        }
        return coords;
    }
    
    /**
     * Gets a new target for a flight of swarm missiles that was just shot at
     * an entity and has missiles left
     * @param game
     * @param ae The attacking <code>Entity</code>
     * @param te The <code>Entity</code> that was shot at.
     * @param weaponId The <code>int</code> ID of the launcher
     *        used to fire this volley
     * 
     * @return the new target <code>Entity</code>. May return null if no
     *         new target available
     */
    public static Entity getSwarmTarget(IGame game, int aeId, Entity te, int weaponId) {
        Coords coords = te.getPosition();
        Entity newTarget = null;
        Entity tempEntity = null;
        // first, check the hex of the original target
        Enumeration entities = game.getEnemyEntities(coords, te);
        while (entities.hasMoreElements()) {
            tempEntity = (Entity)entities.nextElement();
            if (!tempEntity.getTargetedBySwarm(aeId, weaponId)) {
                // we found a target
                return tempEntity;
            }
        }
        // loop through adjacent hexes
        for(int dir=0;dir<=5;dir++) {
            Coords tempcoords=coords.translated(dir);
            if(!game.getBoard().contains(tempcoords)) {
                continue;
            }
            if(coords.equals(tempcoords)) {
                continue;
            }
            entities = game.getEnemyEntities(tempcoords, te);
            if (entities.hasMoreElements()) {
                tempEntity = (Entity)entities.nextElement();
                if (!tempEntity.getTargetedBySwarm(aeId, weaponId)) {
                    // we found a target
                    return tempEntity;
                }
            }
            entities = game.getFriendlyEntities(tempcoords, te);
            if (entities.hasMoreElements()) {
                tempEntity = (Entity)entities.nextElement();
                if (!tempEntity.getTargetedBySwarm(aeId, weaponId)) {
                    // we found a target
                    return tempEntity;
                }
            }
        }
        return newTarget;
    }
    
} // End public class Compute


