/*
 * MegaMek - Copyright (C) 2000,2001,2002,2003,2004 Ben Mazur (bmazur@sev.org)
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

import java.util.*;

import megamek.common.actions.*;
import megamek.client.*;

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
    public static Entity stackingViolation(Game game, int enteringId, Coords coords) {
        Entity entering = game.getEntity(enteringId);
        return stackingViolation( game, entering, coords, null );
    }

    /**
     * When compiling an unloading step, both the transporter and the unloaded
     * unit probably occupy some other position on the board.
     */
    public static Entity stackingViolation( Game game,
                                             Entity entering,
                                             Coords coords,
                                             Entity transport ) {
        boolean isMech = entering instanceof Mech;
        Entity firstEntity = transport;

        // Walk through the entities in the given hex.
        for (Enumeration i = game.getEntities(coords); i.hasMoreElements();) {
            final Entity inHex = (Entity)i.nextElement();

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

        // okay, all clear
        return null;
    }

    /**
     * Returns true if there is any unit that is an enemy of the specified unit
     * in the specified hex.  This is only called for stacking purposes, and
     * so does not return true if the enemy unit is currenly making a DFA.
     */
    public static boolean isEnemyIn(Game game, int entityId, Coords coords, boolean isMech) {
        Entity entity = game.getEntity(entityId);
        for (Enumeration i = game.getEntities(coords); i.hasMoreElements();) {
            final Entity inHex = (Entity)i.nextElement();
            if ((!isMech || inHex instanceof Mech) && inHex.isEnemyOf(entity) && !inHex.isMakingDfa()) {
                return true;
            }
        }
        return false;
    }

    /**
     * @return true if a piloting skill roll is needed to traverse the terrain
     */
    public static boolean isPilotingSkillNeeded(Game game, int entityId,
                                                Coords src, Coords dest,
                                                int movementType,
                                                boolean isTurning,
                                                boolean prevStepIsOnPavement) {
        final Entity entity = game.getEntity(entityId);
        final Hex srcHex = game.board.getHex(src);
        final Hex destHex = game.board.getHex(dest);
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
        if (movementType != Entity.MOVE_JUMP
            && destHex.levelOf(Terrain.RUBBLE) > 0
            && !isInfantry) {
            return true;
        }

        // Check for water unless we're a hovercraft or using a bridge.
        if (movementType != Entity.MOVE_JUMP
            && entity.getMovementType() != Entity.MovementType.HOVER
            && destHex.levelOf(Terrain.WATER) > 0
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
        //   && overallMoveType == Entity.MOVE_RUN
             && movementType == Entity.MOVE_RUN
             && isTurning
             && !isInfantry ) {
            return true;
        }

        // If we entering or leaving a building, all non-infantry
        // need to make a piloting check to avoid damage.
        // TODO: allow entities to occupy different levels of buildings.
        int nSrcEl = entity.elevationOccupied(srcHex);
        int nDestEl = entity.elevationOccupied(destHex);
        if ( ( nSrcEl < srcHex.levelOf( Terrain.BLDG_ELEV ) ||
               nDestEl < destHex.levelOf( Terrain.BLDG_ELEV ) ) &&
             !(entity instanceof Infantry) ) {
            return true;
        }

        return false;
    }

    /**
     * Can the defending unit be displaced from the source to the destination?
     */
    public static boolean isValidDisplacement(Game game, int entityId,
                                              Coords src, int direction) {
        return isValidDisplacement(game, entityId, src,
                                   src.translated(direction));
    }
    /**
     * Can the defending unit be displaced from the source to the destination?
     */
    public static boolean isValidDisplacement(Game game, int entityId,
                                              Coords src, Coords dest) {
        final Entity entity = game.getEntity(entityId);
        final Hex srcHex = game.board.getHex(src);
        final Hex destHex = game.board.getHex(dest);
        final Coords[] intervening = Coords.intervening(src, dest);
        final int direction = src.direction(dest);

        // arguments valid?
        if (entity == null) {
            throw new IllegalArgumentException("Entity invalid.");
        }

        // an easy check
        if (!game.board.contains(dest)) {
            return false;
        }

        // can't be displaced into prohibited terrain
        if (entity.isHexProhibited(destHex)) {
            return false;
        }

        // can't go up more levels than normally possible
        for (int i = 0; i < intervening.length; i++) {
            final Hex hex = game.board.getHex(intervening[i]);
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
    public static Coords getValidDisplacement(Game game, int entityId,
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
    public static Coords getPreferredDisplacement(Game game, int entityId,
                                              Coords src, int direction) {
        final Entity entity = game.getEntity(entityId);
        int highestElev = Integer.MIN_VALUE;
        Coords highest = null;

        // check the surrounding hexes, nearest to the original direction first
        int[] offsets = {0, 1, 5, 2, 4, 3};
        for (int i = 0; i < offsets.length; i++) {
            Coords dest = src.translated((direction + offsets[i]) % 6);
            if (isValidDisplacement(game, entityId, src, dest)) {
                 // assume that if the displacement's valid, hex is !null
                Hex hex = game.board.getHex(dest);
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
    public static Coords getMissedChargeDisplacement(Game game, int entityId, Coords src, int direction) {
        Coords first = src.translated((direction + 1) % 6);
        Coords second = src.translated((direction + 5) % 6);
        Hex firstHex = game.board.getHex(first);
        Hex secondHex = game.board.getHex(second);
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

        if (isValidDisplacement(game, entityId, src, src.direction(first))) {
            return first;
        } else if (isValidDisplacement(game, entityId, src, src.direction(second))) {
            return second;
        } else {
            return src;
        }
    }

    public static ToHitData toHitWeapon(Game game, WeaponAttackAction waa) {
        return toHitWeapon(game, waa.getEntityId(),
                           game.getTarget(waa.getTargetType(), waa.getTargetId()),
                           waa.getWeaponId(),
                           waa.getAimedLocation(),
                           waa.getAimingMode());
    }


    public static ToHitData toHitWeapon(Game game, int attackerId, Targetable target, int weaponId) {
    return toHitWeapon(game, attackerId, target, weaponId, Mech.LOC_NONE, FiringDisplay.AIM_MODE_NONE);
  }
    /**
     * To-hit number for attacker firing a weapon at the target.
     */
    public static ToHitData toHitWeapon(Game game, int attackerId, Targetable target, int weaponId, int aimingAt, int aimingMode) {
        final Entity ae = game.getEntity(attackerId);
        Entity te = null;
        if (target.getTargetType() == Targetable.TYPE_ENTITY) {
            te = (Entity) target;
        }
        final Mounted weapon = ae.getEquipment(weaponId);
        final WeaponType wtype = (WeaponType)weapon.getType();
        boolean isAttackerInfantry = (ae instanceof Infantry);
        boolean isWeaponInfantry = wtype.hasFlag(WeaponType.F_INFANTRY);
        // 2003-01-02 BattleArmor MG and Small Lasers have unlimited ammo.
        // 2002-09-16 Infantry weapons have unlimited ammo.
        final boolean usesAmmo = wtype.getAmmoType() != AmmoType.T_NA &&
            wtype.getAmmoType() != AmmoType.T_BA_MG &&
            wtype.getAmmoType() != AmmoType.T_BA_SMALL_LASER &&
            !isWeaponInfantry;
        final Mounted ammo = usesAmmo ? weapon.getLinked() : null;
        final AmmoType atype = ammo == null ? null : (AmmoType)ammo.getType();
        final boolean targetInBuilding = isInBuilding( game, te );
        boolean isIndirect = !(wtype.hasFlag(WeaponType.F_ONESHOT)) && wtype.getAmmoType() == AmmoType.T_LRM //For now, oneshot LRM launchers won't be able to indirect.  Sue me, until I can figure out a better fix.
            && weapon.curMode().equals("Indirect");
        boolean isInferno =
            ( atype != null &&
              atype.getMunitionType() == AmmoType.M_INFERNO ) ||
            ( isWeaponInfantry &&
              wtype.hasFlag(WeaponType.F_INFERNO) );
        boolean isArtilleryDirect= wtype.hasFlag(WeaponType.F_ARTILLERY) && game.getPhase() == Game.PHASE_FIRING;
        boolean isArtilleryIndirect = wtype.hasFlag(WeaponType.F_ARTILLERY) && (game.getPhase() == Game.PHASE_TARGETING || game.getPhase() == Game.PHASE_OFFBOARD);//hack, otherwise when actually resolves shot labeled impossible.


        ToHitData toHit = null;

        // make sure weapon can deliver minefield
        if (target.getTargetType() == Targetable.TYPE_MINEFIELD_DELIVER &&
        	!AmmoType.canDeliverMinefield(atype)) {
			return new ToHitData(ToHitData.IMPOSSIBLE, "Weapon can't deliver minefields");
        }
        if((game.getPhase() == Game.PHASE_TARGETING) && !isArtilleryIndirect) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Only indirect artillery can be fired in the targeting phase");
        }


        if ( atype != null &&
             atype.getAmmoType() == AmmoType.T_LRM &&
             ( atype.getMunitionType() == AmmoType.M_THUNDER ||
               atype.getMunitionType() == AmmoType.M_THUNDER_ACTIVE ||
               atype.getMunitionType() == AmmoType.M_THUNDER_INFERNO ||
               atype.getMunitionType() == AmmoType.M_THUNDER_VIBRABOMB||
               atype.getMunitionType() == AmmoType.M_THUNDER_AUGMENTED ) &&
             target.getTargetType() != Targetable.TYPE_MINEFIELD_DELIVER) {
            return new ToHitData( ToHitData.IMPOSSIBLE,
                                  "Weapon can only deliver minefields" );
        }

        // make sure weapon can clear minefield
		if (target instanceof MinefieldTarget &&
			!AmmoType.canClearMinefield(atype)) {
			return new ToHitData(ToHitData.IMPOSSIBLE, "Weapon can't clear minefields");
		}
		
        // Arty shots have to be with arty, non arty shots with non arty.
        if (target.getTargetType() == Targetable.TYPE_HEX_ARTILLERY && !wtype.hasFlag(WeaponType.F_ARTILLERY)) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Weapon can't make artillery attacks.");
        }
        
        // Arrow IV submunition TargetTypes must use appropriate ammo
        if (!((target.getTargetType() == Targetable.TYPE_HEX_ARTILLERY) ||
              (target.getTargetType() == Targetable.TYPE_HEX_FASCAM) ||
              (target.getTargetType() == Targetable.TYPE_HEX_INFERNO_IV) ||
              (target.getTargetType() == Targetable.TYPE_HEX_VIBRABOMB_IV)) && 
              wtype.hasFlag(WeaponType.F_ARTILLERY)) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Weapon must make artillery attacks.");
        }
        if (target.getTargetType() == Targetable.TYPE_HEX_FASCAM && !(atype.getMunitionType() == AmmoType.M_FASCAM)) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Must use appropriate ammo to make this attack.");
        }
        if (target.getTargetType() == Targetable.TYPE_HEX_VIBRABOMB_IV && !(atype.getMunitionType() == AmmoType.M_VIBRABOMB_IV)) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Must use appropriate ammo to make this attack.");
        }
        if (target.getTargetType() == Targetable.TYPE_HEX_INFERNO_IV && !(atype.getMunitionType() == AmmoType.M_INFERNO_IV)) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Must use appropriate ammo to make this attack.");
        }
        
        // can't target yourself
        if (ae.equals(te)) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "You can't target yourself");
        }

        // weapon operational?
        if (weapon.isDestroyed() || weapon.isBreached()) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Weapon not operational.");
        }

        // got ammo?
        if ( usesAmmo && (ammo == null || ammo.getShotsLeft() == 0) ) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Weapon out of ammo.");
        }

        // Are we dumping that ammo?
        if ( usesAmmo && ammo.isDumping() ) {
            ae.loadWeapon( weapon );
            if ( ammo.getShotsLeft() == 0 || ammo.isDumping() ) {
                return new ToHitData( ToHitData.IMPOSSIBLE,
                                      "Dumping remaining ammo." );
            }
        }

        // is the attacker even active?
        if (ae.isShutDown() || !ae.getCrew().isActive()) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Attacker is in no condition to fire weapons.");
        }

        // sensors operational?
        final int sensorHits = ae.getBadCriticals(CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_SENSORS, Mech.LOC_HEAD);
        if (sensorHits > 1) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Attacker sensors destroyed.");
        }

        // Is the weapon blocked by a passenger?
        if ( ae.isWeaponBlockedAt(weapon.getLocation(), weapon.isRearMounted()) ) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Weapon blocked by passenger.");
        }

        // Can't target a entity conducting a swarm attack.
        if ( te != null && Entity.NONE != te.getSwarmTargetId() ) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Target is swarming a Mek.");
        }

        // Infantry can't clear woods.
        if ( isAttackerInfantry &&
             Targetable.TYPE_HEX_CLEAR == target.getTargetType() ) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Infantry can not clear woods.");
        }

        // Some weapons can't cause fires, but Infernos always can.
        if ( wtype.hasFlag(WeaponType.F_NO_FIRES) && !isInferno &&
             Targetable.TYPE_HEX_IGNITE == target.getTargetType() ) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Weapon can not cause fires.");
        }

        // Can't target infantry with Inferno rounds (BMRr, pg. 141).
        //  Also, enforce options for keeping vehicles and protos safe
        //  if those options are checked.
        if ( isInferno &&
             (te instanceof Infantry
              || (te instanceof Tank &&
                  game.getOptions().booleanOption("vehicles_safe_from_infernos"))
              || (te instanceof Protomech &&
                  game.getOptions().booleanOption("protos_safe_from_infernos")))
              ) {
            return new ToHitData( ToHitData.IMPOSSIBLE,
                                  "Can not target that unit type with Inferno rounds." );
        }

        // Can't raise the heat of infantry or tanks.
        if ( wtype.hasFlag(WeaponType.F_FLAMER) &&
             wtype.hasModes() &&
             weapon.curMode().equals("Heat") &&
             !(te instanceof Mech) ) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Can only raise the heat level of Meks.");
        }

        // Handle solo attack weapons.
        if ( wtype.hasFlag(WeaponType.F_SOLO_ATTACK) ) {
            for ( Enumeration i = game.getActions();
                  i.hasMoreElements(); ) {
                Object o = i.nextElement();
                if (!(o instanceof WeaponAttackAction)) {
                    continue;
                }
                WeaponAttackAction prevAttack = (WeaponAttackAction)o;
                if (prevAttack.getEntityId() == attackerId) {

                    // If the attacker fires another weapon, this attack fails.
                    if ( weaponId != prevAttack.getWeaponId() ) {
                        return new ToHitData(ToHitData.IMPOSSIBLE,
                                             "Other weapon attacks declared.");
                    }
                }
            }
        } // End current-weapon-is-solo

        int attEl = ae.absHeight();
        int targEl;

        if (te == null) {
            targEl = game.board.getHex(target.getPosition()).floor();
        } else {
            targEl = te.absHeight();
        }

        //TODO: mech making DFA could be higher if DFA target hex is higher
        //      BMRr pg. 43, "attacking unit is considered to be in the air
        //      above the hex, standing on an elevation 1 level higher than
        //      the target hex or the elevation of the hex the attacker is
        //      in, whichever is higher."

        // check if indirect fire is valid
        if (isIndirect && !game.getOptions().booleanOption("indirect_fire")) {
      return new ToHitData(ToHitData.IMPOSSIBLE, "Indirect fire option not enabled");
        }

        // if we're doing indirect fire, find a spotter
        Entity spotter = null;
        if (isIndirect) {
            spotter = findSpotter(game, ae, target);
            if (spotter == null) {
                return new ToHitData( ToHitData.IMPOSSIBLE, "No available spotter");
            }
        }

        // check LOS (indirect LOS is from the spotter)
        LosEffects los;
        ToHitData losMods;
        if (!isIndirect) {
            los = LosEffects.calculateLos(game, attackerId, target);
            losMods = los.losModifiers(game);
        } else {
            los = LosEffects.calculateLos(game, spotter.getId(), target);
            // do not count attacker partial cover in indirect fire
            los.setAttackerCover(false);
            losMods = los.losModifiers(game);
        }

        // if LOS is blocked, block the shot
        if (losMods.getValue() == ToHitData.IMPOSSIBLE && !isArtilleryIndirect) {
            return losMods;
        }

        // Must target infantry in buildings from the inside.
        if ( targetInBuilding &&
             te instanceof Infantry &&
             null == los.getThruBldg() ) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Attack on infantry crosses building exterior wall.");
        }

        // attacker partial cover means no leg weapons
        if (los.attackerCover && ae.locationIsLeg(weapon.getLocation())) {
            return new ToHitData(ToHitData.IMPOSSIBLE,
                                 "Nearby terrain blocks leg weapons.");
        }

        // Weapon in arc?
        if (!isInArc(game, attackerId, weaponId, target)) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Target not in arc.");
        }

        // Leg attacks, Swarm attacks, and
        // Mine Launchers don't use gunnery.
        if ( Infantry.LEG_ATTACK.equals( wtype.getInternalName() ) ) {
            toHit = Compute.getLegAttackBaseToHit( ae, te );

            // Return if the attack is impossible.
            if ( ToHitData.IMPOSSIBLE == toHit.getValue() ) {
                return toHit;
            }

            // If the attacker has Assault claws, give a -1 modifier.
            // We can stop looking when we find our first match.
            for ( Enumeration iter = ae.getMisc(); iter.hasMoreElements(); ) {
                Mounted mount = (Mounted) iter.nextElement();
                EquipmentType equip = mount.getType();
                if ( BattleArmor.ASSAULT_CLAW.equals
                     (equip.getInternalName()) ) {
                    toHit.addModifier( -1, "attacker has assault claws" );
                    break;
                }
            }
        }
        else if ( Infantry.SWARM_MEK.equals( wtype.getInternalName() ) ) {
            toHit = Compute.getSwarmMekBaseToHit( ae, te );

            // Return if the attack is impossible.
            if ( ToHitData.IMPOSSIBLE == toHit.getValue() ) {
                return toHit;
            }

            // If the attacker has Assault claws, give a -1 modifier.
            // We can stop looking when we find our first match.
            for ( Enumeration iter = ae.getMisc(); iter.hasMoreElements(); ) {
                Mounted mount = (Mounted) iter.nextElement();
                EquipmentType equip = mount.getType();
                if ( BattleArmor.ASSAULT_CLAW.equals
                     (equip.getInternalName()) ) {
                    toHit.addModifier( -1, "attacker has assault claws" );
                    break;
                }
            }
        }
        else if ( Infantry.STOP_SWARM.equals( wtype.getInternalName() ) ) {
            // Can't stop if we're not swarming, otherwise automatic.
            if ( Entity.NONE == ae.getSwarmTargetId() ) {
                return new ToHitData( ToHitData.IMPOSSIBLE,
                                      "Not swarming a Mek." );
            } else {
                return new ToHitData( ToHitData.AUTOMATIC_SUCCESS,
                                      "End swarm attack." );
            }
        }
        else if ( BattleArmor.MINE_LAUNCHER.equals(wtype.getInternalName()) ) {
            // Mine launchers can not hit infantry.
            if ( te instanceof Infantry ) {
                return new ToHitData( ToHitData.IMPOSSIBLE,
                                      "Can not attack infantry." );
            } else {
                toHit = new ToHitData(8, "magnetic mine attack");
            }
        }
        // Swarming infantry always hit their target, but
        // they can only target the Mek they're swarming.
        else if ( te != null && ae.getSwarmTargetId() == te.getId() ) {
            // Only certain weapons can be used in a swarm attack.
            if ( wtype.getDamage() == 0 ) {
                return new ToHitData( ToHitData.IMPOSSIBLE,
                                      "Weapon causes no damage." );
            } else {
                return new ToHitData( ToHitData.AUTOMATIC_SUCCESS,
                                      "Attack during swarm.",
                                      ToHitData.HIT_SWARM,
                                      ToHitData.SIDE_FRONT );
            }
        }
        else if ( Entity.NONE != ae.getSwarmTargetId() ) {
            return new ToHitData( ToHitData.IMPOSSIBLE,
                                  "Must target the Mek being swarmed." );
        }
        else {
            toHit = new ToHitData(ae.crew.getGunnery(), "gunnery skill");
        }

        // Is the pilot a weapon specialist?
        if (ae.crew.getOptions().stringOption("weapon_specialist").equals(wtype.getName())) {
            toHit.addModifier( -2, "weapon specialist" );
        }
        
        // Has the pilot the appropriate gunnery skill?
        if (ae.crew.getOptions().booleanOption("gunnery_laser") == true && wtype.hasFlag(WeaponType.F_ENERGY) ) {
            toHit.addModifier ( -1, "Gunnery/Laser" );
        }
              
        if (ae.crew.getOptions().booleanOption("gunnery_ballistic") == true && wtype.hasFlag(WeaponType.F_BALLISTIC) ) {
            toHit.addModifier ( -1, "Gunnery/Ballistic" );
        }
 
        if (ae.crew.getOptions().booleanOption("gunnery_missile") == true && wtype.hasFlag(WeaponType.F_MISSILE) ) {
            toHit.addModifier ( -1, "Gunnery/Missile" );
        }

        // determine some more variables
        int aElev = ae.getElevation();
        int tElev = target.getElevation();
        int distance = effectiveDistance(game, ae, target);


        // Handle direct artillery attacks.
        if(isArtilleryDirect) {
          toHit.addModifier(5, "direct artillery modifer");
          toHit.append(getAttackerMovementModifier(game, attackerId));
          toHit.append(losMods);
          toHit.append(getSecondaryTargetMod(game, ae, target));
          // actuator & sensor damage to attacker
          toHit.append(getDamageWeaponMods(ae, weapon));
          // heat
          if (ae.getHeatFiringModifier() != 0) {
            toHit.addModifier(ae.getHeatFiringModifier(), "heat");
          }
          // weapon to-hit modifier
          if (wtype.getToHitModifier() != 0) {
            toHit.addModifier(wtype.getToHitModifier(), "weapon to-hit modifier");
          }

          // ammo to-hit modifier
          if (usesAmmo && atype.getToHitModifier() != 0) {
            toHit.addModifier(atype.getToHitModifier(),
                              "ammunition to-hit modifier");
          }
          if (distance >17) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Direct artillery attack at range >17 hexes.");
          }
          return toHit;

        }
        if(isArtilleryIndirect) {
            int boardRange=(int)Math.ceil(((float)distance)/17f);
            if(boardRange>wtype.getLongRange()) {
                return new ToHitData(ToHitData.IMPOSSIBLE, "Indirect artillery attack out of range");
            }
            if(distance<=17  && !(losMods.getValue()==ToHitData.IMPOSSIBLE)) {
                return new ToHitData(ToHitData.IMPOSSIBLE, "Cannot fire indirectly at range <=17 hexes unless no LOS.");
            }
            toHit.addModifier(7, "indirect artillery modifier");
            int adjust = ae.aTracker.getModifier(weapon,target.getPosition());
            if(adjust==ToHitData.AUTOMATIC_SUCCESS) {
                return new ToHitData(ToHitData.AUTOMATIC_SUCCESS, "Artillery firing at target that's been hit before.");
            } else if(adjust!=0) {
                toHit.addModifier(adjust, "adjusted fire");
            }
            return toHit;

        }


        // Attacks against adjacent buildings automatically hit.
        if ( distance == 1 &&
             ( target.getTargetType() == Targetable.TYPE_BUILDING ||
               target.getTargetType() == Targetable.TYPE_BLDG_IGNITE ) ) {
            return new ToHitData( ToHitData.AUTOMATIC_SUCCESS,
                                  "Targeting adjacent building." );
        }

        // Attacks against buildings from inside automatically hit.
        if ( null != los.getThruBldg() &&
             ( target.getTargetType() == Targetable.TYPE_BUILDING ||
               target.getTargetType() == Targetable.TYPE_BLDG_IGNITE ) ) {
            return new ToHitData( ToHitData.AUTOMATIC_SUCCESS,
                                  "Targeting building from inside (are you SURE this is a good idea?)." );
        }

        // add range mods
        toHit.append(getRangeMods(game, ae, weaponId, target));

        // Battle Armor targets are hard for Meks and Tanks to hit.
        if ( !isAttackerInfantry && te != null && te instanceof BattleArmor ) {
            toHit.addModifier( 1, "battle armor target" );
        }

        // Indirect fire has a +1 mod
        if (isIndirect) {
            toHit.addModifier( 1, "indirect fire" );
        }

        // attacker movement
        toHit.append(getAttackerMovementModifier(game, attackerId));

        // target movement
        if (te != null) {
            ToHitData thTemp = getTargetMovementModifier(game, target.getTargetId());
            toHit.append(thTemp);

            // precision ammo reduces this modifier
            if (atype != null && atype.getAmmoType() == AmmoType.T_AC &&
                atype.getMunitionType() == AmmoType.M_PRECISION) {
                int nAdjust = Math.min(2, thTemp.getValue());
                if (nAdjust > 0) {
                    toHit.append(new ToHitData(-nAdjust, "Precision Ammo"));
                }
            }
        }

        // Armor Piercing ammo is a flat +1
        if ( (atype != null) &&
             (atype.getMunitionType() == AmmoType.M_ARMOR_PIERCING) ) {
            toHit.addModifier( 1, "Armor-Piercing Ammo" );
        }

        // spotter movement, if applicable
        if (isIndirect) {
            toHit.append(getSpotterMovementModifier(game, spotter.getId()));
        }

        // attacker terrain
        toHit.append(getAttackerTerrainModifier(game, attackerId));

        // target terrain, not applicable when delivering minefields
        if (target.getTargetType() != Targetable.TYPE_MINEFIELD_DELIVER) {
            toHit.append(getTargetTerrainModifier(game, target));
        }

        // target in water?
        Hex attHex = game.board.getHex(ae.getPosition());
        Hex targHex = game.board.getHex(target.getPosition());
        if (targHex.contains(Terrain.WATER) && targHex.surface() == targEl && te.height() > 0) { //target in partial water
            los.targetCover = true;
            losMods = los.losModifiers(game);
        }

        // add in LOS mods that we've been keeping
        toHit.append(losMods);

        // secondary targets modifier...
        toHit.append(getSecondaryTargetMod(game, ae, target));

        // heat
        if (ae.getHeatFiringModifier() != 0) {
            toHit.addModifier(ae.getHeatFiringModifier(), "heat");
        }

        // actuator & sensor damage to attacker
        toHit.append(getDamageWeaponMods(ae, weapon));

        // target immobile
        toHit.append(getImmobileMod(target, aimingAt, aimingMode));

        // attacker prone
        toHit.append(getProneMods(game, ae, weaponId));

        // target prone
        if (te != null && te.isProne()) {
            // easier when point-blank
            if (distance <= 1) {
                // BMRr, pg. 72: Swarm Mek attacks get "an additional -4
                // if the BattleMech is prone or immoble."  I interpret
                // this to mean that the bonus gets applied *ONCE*.
                if ( Infantry.SWARM_MEK.equals( wtype.getInternalName() ) ) {
                    // If the target is immoble, don't give a bonus for prone.
                    if ( !te.isImmobile() ) {
                        toHit.addModifier(-4, "swarm prone target");
                    }
                } else {
                    toHit.addModifier(-2, "target prone and adjacent");
                }
            }
            // harder at range
            else {
                toHit.addModifier(1, "target prone and at range");
            }
        }

        // weapon to-hit modifier
        if (wtype.getToHitModifier() != 0) {
            toHit.addModifier(wtype.getToHitModifier(), "weapon to-hit modifier");
        }

        // ammo to-hit modifier
        if (usesAmmo && atype.getToHitModifier() != 0) {
            toHit.addModifier(atype.getToHitModifier(), "ammunition to-hit modifier");
        }

        // add targeting computer (except with LBX cluster ammo)
        if (aimingMode == FiringDisplay.AIM_MODE_TARG_COMP &&
          aimingAt != Mech.LOC_NONE) {
          toHit.addModifier(3, "aiming with targeting computer");
        } else {
          if ( ae.hasTargComp() && wtype.hasFlag(WeaponType.F_DIRECT_FIRE) &&
               (!usesAmmo || atype.getMunitionType() != AmmoType.M_CLUSTER) ) {
              toHit.addModifier(-1, "targeting computer");
          }
      }

        // Change hit table for elevation differences inside building.
        if ( null != los.getThruBldg() && aElev != tElev ) {

            // Tanks get hit in a random side.
            if ( target instanceof Tank ) {
                toHit.setSideTable( ToHitData.SIDE_RANDOM );
            }

            // Meks have special tables for shots from above and below.
            else if ( target instanceof Mech ) {
                if ( aElev > tElev ) {
                    toHit.setHitTable( ToHitData.HIT_ABOVE );
                } else {
                    toHit.setHitTable( ToHitData.HIT_BELOW );
                }
            }

        }

        // Change hit table for partial cover, accomodate for partial underwater(legs)
        if (los.targetCover) {
            if ( ae.getLocationStatus(weapon.getLocation()) == Entity.LOC_WET && (targHex.contains(Terrain.WATER) && targHex.surface() == targEl && te.height() > 0) ) {
                //weapon underwater, target in partial water
                toHit.setHitTable(ToHitData.HIT_KICK);
            } else {
                toHit.setHitTable(ToHitData.HIT_PUNCH);
            }
        }

        // factor in target side
        if ( isAttackerInfantry && 0 == distance ) {
            // Infantry attacks from the same hex are resolved against the front.
            toHit.setSideTable( ToHitData.SIDE_FRONT );
        } else {
            toHit.setSideTable( targetSideTable(ae, target) );
        }

        // okay!
        return toHit;
    }

    /**
     * Finds the best spotter for the attacker.  The best spotter is the one
     * with the lowest attack modifiers, of course.  LOS modifiers and
     * movement are considered.
     */
    public static Entity findSpotter(Game game, Entity attacker, Targetable target) {
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
            if (true || mods.getValue() < bestMods.getValue()) {
                spotter = other;
                bestMods = mods;
            }
        }

        return spotter;
    }

	public static ToHitData getImmobileMod(Targetable target) {
		return getImmobileMod(target, Mech.LOC_NONE, FiringDisplay.AIM_MODE_NONE);
	}

	public static ToHitData getImmobileMod(Targetable target, int aimingAt, int aimingMode) {
		if (target.isImmobile()) {
			if ((aimingAt == Mech.LOC_HEAD) &&
				(aimingMode == FiringDisplay.AIM_MODE_IMMOBILE)) {
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
    private static ToHitData getRangeMods(Game game, Entity ae, int weaponId, Targetable target) {
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

        //is water involved?
        Hex attHex = game.board.getHex(ae.getPosition());
        Hex targHex = game.board.getHex(target.getPosition());
        int targEl;
        if (target == null) {
            targEl = game.board.getHex(target.getPosition()).floor();
        } else {

            targEl = target.absHeight();
        }

        if (ae.getLocationStatus(weapon.getLocation()) == Entity.LOC_WET) {
            weaponRanges = wtype.getWRanges();
            //HACK on ranges: for those without underwater range,
            // long == medium; iteration in rangeBracket() allows this
            if (weaponRanges[RangeType.RANGE_SHORT] == 0) {
                return new ToHitData(ToHitData.IMPOSSIBLE,
                                     "Weapon cannot fire underwater.");
            }
            if (!(targHex.contains(Terrain.WATER)) ||
                targHex.surface() <= target.getElevation()) {
                //target on land or over water
                return new ToHitData(ToHitData.IMPOSSIBLE,
                                     "Weapon underwater, but not target.");
            }
        } else if (targHex.contains(Terrain.WATER) &&
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

        // add range modifier
        if (usingRange == range) {
            // no c3 adjustment
            if (range == RangeType.RANGE_MEDIUM) {
                mods.addModifier(2, "medium range");
            }
            else if (range == RangeType.RANGE_LONG) {
                // Protos that loose head sensors can't shoot long range.
                if( (ae instanceof Protomech) &&
                    (2 == ((Protomech)ae).getCritsHit(Protomech.LOC_HEAD)) ) {
                    mods.addModifier
                        ( ToHitData.IMPOSSIBLE,
                          "No long range attacks with destroyed head sensors." );
                } else {
                    mods.addModifier(4, "long range");
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
                    mods.addModifier(8, "extreme range");
                }
            }
        }

        else {
            // report c3 adjustment
            if (c3range == RangeType.RANGE_SHORT) {
                mods.addModifier(0, "short range due to C3 spotter");
            }
            else if (c3range == RangeType.RANGE_MEDIUM) {
                mods.addModifier(2, "medium range due to C3 spotter");
            }
            else if (c3range == RangeType.RANGE_LONG) {
                mods.addModifier(4, "long range due to C3 spotter");
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

        // add minumum range modifier
        int minRange = weaponRanges[RangeType.RANGE_MINIMUM];
        if (minRange > 0 && distance <= minRange) {
            int minPenalty = (minRange - distance) + 1;
            // Infantry LRMs suffer double minimum range penalties.
            if (isLRMInfantry) {
                mods.addModifier(minPenalty * 2, "infantry LRM minumum range");
            } else {
                mods.addModifier(minPenalty, "minumum range");
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
    public static int effectiveDistance(Game game, Entity attacker, Targetable target) {
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
    private static Entity findC3Spotter(Game game, Entity attacker, Targetable target) {
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
    private static ToHitData getProneMods(Game game, Entity attacker, int weaponId) {
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
  private static boolean isFiringFromArmAlready(Game game, int weaponId, final Entity attacker, int armLoc) {
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
  private static ToHitData getDamageWeaponMods(Entity attacker, Mounted weapon)
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
    private static ToHitData getSecondaryTargetMod(Game game, Entity attacker, Targetable target) {
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

  public static ToHitData toHitPunch(Game game, PunchAttackAction paa) {
        return toHitPunch(game, paa.getEntityId(),
                          game.getTarget(paa.getTargetType(), paa.getTargetId() ),
                          paa.getArm());
    }

    /**
     * To-hit number for the specified arm to punch
     */
    public static ToHitData toHitPunch(Game game, int attackerId,
                                       Targetable target, int arm) {
        final Entity ae = game.getEntity(attackerId);
        int targetId = Entity.NONE;
        Entity te = null;
        if ( target.getTargetType() == Targetable.TYPE_ENTITY ) {
            te = (Entity) target;
            targetId = target.getTargetId();
        }
        final int attackerHeight = ae.absHeight();
        final int targetHeight = target.absHeight();
        final int targetElevation = target.getElevation();
        final int armLoc = (arm == PunchAttackAction.RIGHT)
                           ? Mech.LOC_RARM : Mech.LOC_LARM;
        final int armArc = (arm == PunchAttackAction.RIGHT)
                           ? Compute.ARC_RIGHTARM : Compute.ARC_LEFTARM;
        final boolean targetInBuilding = isInBuilding( game, te );
        Building bldg = null;
        if ( targetInBuilding ) {
            bldg = game.board.getBuildingAt( te.getPosition() );
        }
        ToHitData toHit;

        // arguments legal?
        if (arm != PunchAttackAction.RIGHT && arm != PunchAttackAction.LEFT) {
            throw new IllegalArgumentException("Arm must be LEFT or RIGHT");
        }
        if (ae == null || target == null) {
            throw new IllegalArgumentException("Attacker or target not valid");
        }

        // can't target yourself
        if (ae.equals(te)) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "You can't target yourself");
        }

        // non-mechs can't punch
        if (!(ae instanceof Mech)) {
        	return new ToHitData(ToHitData.IMPOSSIBLE, "Non-mechs can't punch");
        }

        // Can't target a transported entity.
        if ( te != null && Entity.NONE != te.getTransportId() ) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Target is a passenger.");
        }

        // Can't target a entity conducting a swarm attack.
        if ( te != null && Entity.NONE != te.getSwarmTargetId() ) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Target is swarming a Mek.");
        }

        // can't make physical attacks while spotting
        if (ae.isSpotting()) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Attacker is spotting this turn");
        }

        //Quads can't punch
        if ( ae.entityIsQuad() ) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Attacker is a quad");
        }

        //Can't punch with flipped arms
        if (ae.getArmsFlipped()) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Arms are flipped to the rear. Can not punch.");
        }

        // check if arm is present
        if (ae.isLocationBad(armLoc)) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Arm missing");
        }

        // check if shoulder is functional
        if (!ae.hasWorkingSystem(Mech.ACTUATOR_SHOULDER, armLoc)) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Shoulder destroyed");
        }

        // check if attacker has fired arm-mounted weapons
        if (ae.weaponFiredFrom(armLoc)) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Weapons fired from arm this turn");
        }

        // check range
        if (ae.getPosition().distance(target.getPosition()) > 1 ) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Target not in range");
        }

        // check elevation
        if (attackerHeight < targetElevation || attackerHeight > targetHeight) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Target elevation not in range");
        }

        // can't physically attack mechs making dfa attacks
        if ( te != null && te.isMakingDfa() ) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Target is making a DFA attack");
        }

        // Can't target units in buildings (from the outside).
        if ( targetInBuilding ) {
            if ( !isInBuilding(game, ae) ) {
                return new ToHitData(ToHitData.IMPOSSIBLE, "Target is inside building" );
            }
            else if ( !game.board.getBuildingAt( ae.getPosition() )
                      .equals( bldg ) ) {
                return new ToHitData(ToHitData.IMPOSSIBLE, "Target is inside differnt building" );
            }
        }


        //Set the base BTH
          int base = 4;

          if ( game.getOptions().booleanOption("maxtech_physical_BTH") ) {
            base = ae.getCrew().getPiloting() - 1;
          }

          toHit = new ToHitData(base, "base");

        // Prone Meks can only punch vehicles in the same hex.
        if (ae.isProne()) {
            // The Mek must have both arms, the target must
            // be a tank, and both must be in the same hex.
            if ( !ae.isLocationBad(Mech.LOC_RARM) &&
                 !ae.isLocationBad(Mech.LOC_LARM) &&
                 te instanceof Tank &&
                 ae.getPosition().distance( target.getPosition() ) == 0 ) {
                toHit.addModifier( 2, "attacker is prone" );
            } else {
                return new ToHitData(ToHitData.IMPOSSIBLE,"Attacker is prone");
            }
        }

        // Check facing if the Mek is not prone.
        else if ( !isInArc(ae.getPosition(), ae.getSecondaryFacing(),
                           target.getPosition(), armArc) ) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Target not in arc");
        }

        // Attacks against adjacent buildings automatically hit.
        if ( target.getTargetType() == Targetable.TYPE_BUILDING ) {
            return new ToHitData( ToHitData.AUTOMATIC_SUCCESS,
                                  "Targeting adjacent building." );
        }

        // Can't target woods or ignite a building with a physical.
        if ( target.getTargetType() == Targetable.TYPE_BLDG_IGNITE ||
             target.getTargetType() == Targetable.TYPE_HEX_CLEAR ||
             target.getTargetType() == Targetable.TYPE_HEX_IGNITE ) {
            return new ToHitData( ToHitData.IMPOSSIBLE, "Invalid attack");
        }

        // Battle Armor targets are hard for Meks and Tanks to hit.
        if ( te instanceof BattleArmor ) {
            toHit.addModifier( 1, "battle armor target" );
        }


        // attacker movement
        toHit.append(getAttackerMovementModifier(game, attackerId));

        // target movement
        toHit.append(getTargetMovementModifier(game, targetId));

        // attacker terrain
        toHit.append(getAttackerTerrainModifier(game, attackerId));

        // target terrain
        toHit.append(getTargetTerrainModifier(game, te));

        // damaged or missing actuators
        if (!ae.hasWorkingSystem(Mech.ACTUATOR_UPPER_ARM, armLoc)) {
            toHit.addModifier(2, "Upper arm actuator destroyed");
        }
        if (!ae.hasWorkingSystem(Mech.ACTUATOR_LOWER_ARM, armLoc)) {
            toHit.addModifier(2, "Lower arm actuator missing or destroyed");
        }
        if (!ae.hasWorkingSystem(Mech.ACTUATOR_HAND, armLoc)) {
            toHit.addModifier(1, "Hand actuator missing or destroyed");
        }

        // target prone
        if (te.isProne()) {
            toHit.addModifier(-2, "target prone and adjacent");
        }

        // water partial cover?
        Hex targHex = game.board.getHex(te.getPosition());
        if (te.height() > 0 && targHex.levelOf(Terrain.WATER) == te.height()) {
            toHit.addModifier(3, "target has partial cover");
        }

    // target immobile
    toHit.append(getImmobileMod(te));

        modifyPhysicalBTHForAdvantages(ae, te, toHit, game);

        // elevation
        if (attackerHeight == targetElevation) {
            if (te.height() == 0) {
                toHit.setHitTable(ToHitData.HIT_NORMAL);
            } else {
                toHit.setHitTable(ToHitData.HIT_KICK);
            }
        } else {
            toHit.setHitTable(ToHitData.HIT_PUNCH);
        }

        // factor in target side
        toHit.setSideTable(targetSideTable(ae,te));

        // done!
        return toHit;
    }

    /**
     * Damage that the specified mech does with a punch.
     */
    public static int getPunchDamageFor(Entity entity, int arm) {
        final int armLoc = (arm == PunchAttackAction.RIGHT)
                           ? Mech.LOC_RARM : Mech.LOC_LARM;
        int damage = (int)Math.ceil(entity.getWeight() / 10.0);
        float multiplier = 1.0f;

        if (!entity.hasWorkingSystem(Mech.ACTUATOR_UPPER_ARM, armLoc)) {
            multiplier /= 2.0f;
        }
        if (!entity.hasWorkingSystem(Mech.ACTUATOR_LOWER_ARM, armLoc)) {
            multiplier /= 2.0f;
        }
        if (!entity.hasWorkingSystem(Mech.ACTUATOR_SHOULDER, armLoc)) {
            damage = 0;
        }
        if (entity.heat >= 9 && ((Mech)entity).hasTSM()) {
            multiplier *= 2.0f;
        }
        if (entity.getLocationStatus(armLoc) == Entity.LOC_WET) {
            multiplier /= 2.0f;
        }
        return (int)Math.floor(damage * multiplier) + entity.getCrew().modifyPhysicalDamagaForMeleeSpecialist();
    }

    public static ToHitData toHitKick(Game game, KickAttackAction kaa) {
        return toHitKick(game, kaa.getEntityId(),
                         game.getTarget(kaa.getTargetType(), kaa.getTargetId()),
                         kaa.getLeg());
    }
    
    /**
     * To-hit number for the specified leg to kick
     */
    public static ToHitData toHitKick(Game game, int attackerId,
                                      Targetable target, int leg) {
        final Entity ae = game.getEntity(attackerId);
        int targetId = Entity.NONE;
        Entity te = null;
        if ( target.getTargetType() == Targetable.TYPE_ENTITY ) {
            te = (Entity) target;
            targetId = target.getTargetId();
        }
        final int attackerElevation = ae.getElevation();
        final int targetHeight = target.absHeight();
        final int targetElevation = target.getElevation();
        final boolean targetInBuilding = isInBuilding( game, te );
        Building bldg = null;
        if ( targetInBuilding ) {
            bldg = game.board.getBuildingAt( te.getPosition() );
        }

        int[] kickLegs = new int[2];
        if ( ae.entityIsQuad() ) {
          kickLegs[0] = Mech.LOC_RARM;
          kickLegs[1] = Mech.LOC_LARM;
        } else {
          kickLegs[0] = Mech.LOC_RLEG;
          kickLegs[1] = Mech.LOC_LLEG;
        }
        final int legLoc =
            (leg == KickAttackAction.RIGHT) ? kickLegs[0] : kickLegs[1];

        ToHitData toHit;

        // arguments legal?
        if (leg != KickAttackAction.RIGHT && leg != KickAttackAction.LEFT) {
            throw new IllegalArgumentException("Leg must be LEFT or RIGHT");
        }
        if (ae == null || target == null) {
            throw new IllegalArgumentException("Attacker or target not valid");
        }

        // can't target yourself
        if (ae.equals(te)) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "You can't target yourself");
        }

        // non-mechs can't kick
        if (!(ae instanceof Mech)) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Non-mechs can't kick");
        }

        // can't make physical attacks while spotting
        if (ae.isSpotting()) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Attacker is spotting this turn");
        }

        // Can't target a transported entity.
        if ( te != null && Entity.NONE != te.getTransportId() ) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Target is a passenger.");
        }

        // Can't target a entity conducting a swarm attack.
        if ( te != null && Entity.NONE != te.getSwarmTargetId() ) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Target is swarming a Mek.");
        }

        // check if both legs are present & working
        if (ae.isLocationBad(kickLegs[0])
            || ae.isLocationBad(kickLegs[1])) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Leg missing");
        }

        // check if both hips are operational
        if (!ae.hasWorkingSystem(Mech.ACTUATOR_HIP, kickLegs[0])
            || !ae.hasWorkingSystem(Mech.ACTUATOR_HIP, kickLegs[1])) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Hip destroyed");
        }

        // check if attacker has fired leg-mounted weapons
        for (Enumeration i = ae.getWeapons(); i.hasMoreElements();) {
            Mounted mounted = (Mounted)i.nextElement();
            if (mounted.isUsedThisRound() && mounted.getLocation() == legLoc) {
                return new ToHitData(ToHitData.IMPOSSIBLE, "Weapons fired from leg this turn");
            }
        }

        // check range
        final int range = ae.getPosition().distance(target.getPosition());
        if ( range > 1 ) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Target not in range");
        }
        else if ( te instanceof Infantry && 1 == range ) {
            // As per Randall in the post, http://www.classicbattletech.com/w3t/showflat.php?Cat=&Board=ask&Number=626894&page=1&view=collapsed&sb=5&o=0&fpart=
            return new ToHitData(ToHitData.IMPOSSIBLE,
                                 "Can only stomp Infantry in same hex");
        }

        // check elevation
        if (attackerElevation < targetElevation || attackerElevation > targetHeight) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Target elevation not in range");
        }

        // can't physically attack mechs making dfa attacks
        if ( te != null && te.isMakingDfa() ) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Target is making a DFA attack");
        }

        // check facing
        // Don't check arc for stomping infantry or tanks.
        if (0 != range &&
            !isInArc(ae.getPosition(), ae.getFacing(),
                     target.getPosition(), Compute.ARC_FORWARD)) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Target not in arc");
        }

        // can't kick while prone
        if (ae.isProne()) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Attacker is prone");
        }

        // Can't target units in buildings (from the outside).
        if ( 0 != range && targetInBuilding ) {
            if ( !isInBuilding(game, ae) ) {
                return new ToHitData(ToHitData.IMPOSSIBLE, "Target is inside building" );
            }
            else if ( !game.board.getBuildingAt( ae.getPosition() )
                      .equals( bldg ) ) {
                return new ToHitData(ToHitData.IMPOSSIBLE, "Target is inside differnt building" );
            }
        }

        // Attacks against adjacent buildings automatically hit.
        if ( target.getTargetType() == Targetable.TYPE_BUILDING ) {
            return new ToHitData( ToHitData.AUTOMATIC_SUCCESS,
                                  "Targeting adjacent building." );
        }

        // Can't target woods or ignite a building with a physical.
        if ( target.getTargetType() == Targetable.TYPE_BLDG_IGNITE ||
             target.getTargetType() == Targetable.TYPE_HEX_CLEAR ||
             target.getTargetType() == Targetable.TYPE_HEX_IGNITE ) {
            return new ToHitData( ToHitData.IMPOSSIBLE, "Invalid attack");
        }

        //Set the base BTH
        int base = 3;

        // Level 3 rule: the BTH is PSR - 2
        if ( game.getOptions().booleanOption("maxtech_physical_BTH") ) {
            base = ae.getCrew().getPiloting() - 2;
        }

        // Start the To-Hit
        toHit = new ToHitData(base, "base");

        // BMR(r), page 33. +3 modifier for kicking infantry.
        if ( te instanceof Infantry ) {
            toHit.addModifier( 3, "Stomping Infantry" );
        }

        // Battle Armor targets are hard for Meks and Tanks to hit.
        if ( te instanceof BattleArmor ) {
            toHit.addModifier( 1, "battle armor target" );
        }

        // attacker movement
        toHit.append(getAttackerMovementModifier(game, attackerId));

        // target movement
        toHit.append(getTargetMovementModifier(game, targetId));

        // attacker terrain
        toHit.append(getAttackerTerrainModifier(game, attackerId));

        // target terrain
        toHit.append(getTargetTerrainModifier(game, te));

        // damaged or missing actuators
        if (!ae.hasWorkingSystem(Mech.ACTUATOR_UPPER_LEG, legLoc)) {
            toHit.addModifier(2, "Upper leg actuator destroyed");
        }
        if (!ae.hasWorkingSystem(Mech.ACTUATOR_LOWER_LEG, legLoc)) {
            toHit.addModifier(2, "Lower leg actuator destroyed");
        }
        if (!ae.hasWorkingSystem(Mech.ACTUATOR_FOOT, legLoc)) {
            toHit.addModifier(1, "Foot actuator destroyed");
        }

        // target prone
        if (te.isProne()) {
            toHit.addModifier(-2, "target prone and adjacent");
        }

        // water partial cover?
        Hex targHex = game.board.getHex(te.getPosition());
        if (te.height() > 0 && targHex.levelOf(Terrain.WATER) == te.height()) {
            toHit.addModifier(3, "target has partial cover");
        }

        // target immobile
        toHit.append(getImmobileMod(te));

        modifyPhysicalBTHForAdvantages(ae, te, toHit, game);

        // elevation
        if (attackerElevation < targetHeight) {
            toHit.setHitTable(ToHitData.HIT_KICK);
        } else if (te.height() > 0) {
            toHit.setHitTable(ToHitData.HIT_PUNCH);
        } else {
            toHit.setHitTable(ToHitData.HIT_NORMAL);
        }

        // factor in target side
        toHit.setSideTable(targetSideTable(ae,te));

        // BMRr pg. 42, "The side on which a vehicle takes damage is determined
        // randomly if the BattleMech is attacking from the same hex."
        if ( 0 == range && te instanceof Tank ) {
            toHit.setSideTable( ToHitData.SIDE_RANDOM );
        }

        // done!
        return toHit;
    }


    /**
     * Damage that the specified mech does with a kick
     */
    public static int getKickDamageFor(Entity entity, int leg) {
        int[] kickLegs = new int[2];
        if ( entity.entityIsQuad() ) {
          kickLegs[0] = Mech.LOC_RARM;
          kickLegs[1] = Mech.LOC_LARM;
        } else {
          kickLegs[0] = Mech.LOC_RLEG;
          kickLegs[1] = Mech.LOC_LLEG;
        }

        final int legLoc = (leg == KickAttackAction.RIGHT) ? kickLegs[0] : kickLegs[1];
        int damage = (int)Math.floor(entity.getWeight() / 5.0);
        float multiplier = 1.0f;

        if (!entity.hasWorkingSystem(Mech.ACTUATOR_UPPER_LEG, legLoc)) {
            multiplier /= 2.0f;
        }
        if (!entity.hasWorkingSystem(Mech.ACTUATOR_LOWER_LEG, legLoc)) {
            multiplier /= 2.0f;
        }
        if (!entity.hasWorkingSystem(Mech.ACTUATOR_HIP, legLoc)) {
            damage = 0;
        }
        if (entity.heat >= 9 && ((Mech)entity).hasTSM()) {
            multiplier *= 2.0f;
        }
        if (entity.getLocationStatus(legLoc) == Entity.LOC_WET) {
            multiplier /= 2.0f;
        }
        return (int)Math.floor(damage * multiplier) + entity.getCrew().modifyPhysicalDamagaForMeleeSpecialist();
    }

    public static ToHitData toHitClub(Game game, ClubAttackAction caa) {
        return toHitClub(game, caa.getEntityId(),
                         game.getTarget(caa.getTargetType(), caa.getTargetId()),
                         caa.getClub());
    }

    /**
     * To-hit number for the specified club to hit
     */
    public static ToHitData toHitClub(Game game, int attackerId, Targetable target, Mounted club) {
        final Entity ae = game.getEntity(attackerId);
        int targetId = Entity.NONE;

        // arguments legal?
        if (ae == null || target == null) {
            throw new IllegalArgumentException("Attacker or target not valid");
        }
        if (club == null) {
            throw new IllegalArgumentException("Club is null");
        }
        if (club.getType() == null) {
            throw new IllegalArgumentException("Club type is null");
        }

        Entity te = null;
        if ( target.getTargetType() == Targetable.TYPE_ENTITY ) {
            te = (Entity) target;
            targetId = target.getTargetId();
        }
        final int attackerElevation = ae.getElevation();
        final int attackerHeight = ae.absHeight();
        final int targetHeight = target.absHeight();
        final int targetElevation = target.getElevation();
        final boolean bothArms = club.getType().hasFlag(MiscType.F_CLUB);
        final boolean targetInBuilding = isInBuilding( game, te );
        Building bldg = null;
        if ( targetInBuilding ) {
            bldg = game.board.getBuildingAt( te.getPosition() );
        }
        ToHitData toHit;

        // can't target yourself
        if (ae.equals(te)) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "You can't target yourself");
        }

        // non-mechs can't club
        if (!(ae instanceof Mech)) {
        	return new ToHitData(ToHitData.IMPOSSIBLE, "Non-mechs can't club");
        }

        //Quads can't club
        if ( ae.entityIsQuad() ) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Attacker is a quad");
        }

        // can't make physical attacks while spotting
        if (ae.isSpotting()) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Attacker is spotting this turn");
        }

        // Can't target a transported entity.
        if ( te != null && Entity.NONE != te.getTransportId() ) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Target is a passenger.");
        }

        // Can't target a entity conducting a swarm attack.
        if ( te != null && Entity.NONE != te.getSwarmTargetId() ) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Target is swarming a Mek.");
        }

        // Can't target units in buildings (from the outside).
        // TODO: you can target units from within the *same* building.
        if ( targetInBuilding ) {
            if ( !isInBuilding(game, ae) ) {
                return new ToHitData(ToHitData.IMPOSSIBLE, "Target is inside building" );
            }
            else if ( !game.board.getBuildingAt( ae.getPosition() )
                      .equals( bldg ) ) {
                return new ToHitData(ToHitData.IMPOSSIBLE, "Target is inside differnt building" );
            }
        }

        if (bothArms) {
            // check if both arms are present & operational
            if (ae.isLocationBad(Mech.LOC_RARM)
                || ae.isLocationBad(Mech.LOC_LARM)) {
                return new ToHitData(ToHitData.IMPOSSIBLE, "Arm missing");
            }
            // check if attacker has fired arm-mounted weapons
            if (ae.weaponFiredFrom(Mech.LOC_RARM)
            || ae.weaponFiredFrom(Mech.LOC_LARM)) {
                return new ToHitData(ToHitData.IMPOSSIBLE, "Weapons fired from arm this turn");
            }
            // need shoulder and hand actuators
            if (!ae.hasWorkingSystem(Mech.ACTUATOR_SHOULDER, Mech.LOC_RARM)
            || !ae.hasWorkingSystem(Mech.ACTUATOR_SHOULDER, Mech.LOC_LARM)) {
                return new ToHitData(ToHitData.IMPOSSIBLE, "Shoulder actuator destroyed");
            }
            if (!ae.hasWorkingSystem(Mech.ACTUATOR_HAND, Mech.LOC_RARM)
            || !ae.hasWorkingSystem(Mech.ACTUATOR_HAND, Mech.LOC_LARM)) {
                return new ToHitData(ToHitData.IMPOSSIBLE, "Hand actuator destroyed");
            }
        } else {
            // check if arm is present
            if (ae.isLocationBad(club.getLocation())) {
                return new ToHitData(ToHitData.IMPOSSIBLE, "Arm missing");
            }
            // check if attacker has fired arm-mounted weapons
            if (ae.weaponFiredFrom(club.getLocation())) {
                return new ToHitData(ToHitData.IMPOSSIBLE, "Weapons fired from arm this turn");
            }
            // need shoulder and hand actuators
            if (!ae.hasWorkingSystem(Mech.ACTUATOR_SHOULDER, club.getLocation())) {
                return new ToHitData(ToHitData.IMPOSSIBLE, "Shoulder actuator destroyed");
            }
            if (!ae.hasWorkingSystem(Mech.ACTUATOR_HAND, club.getLocation())) {
                return new ToHitData(ToHitData.IMPOSSIBLE, "Hand actuator destroyed");
            }
        }

        // club must not be damaged
        if (ae.getBadCriticals(CriticalSlot.TYPE_EQUIPMENT, ae.getEquipmentNum(club), club.getLocation()) > 0) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Club is damaged");
        }

        // check range
        if (ae.getPosition().distance(target.getPosition()) > 1 ) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Target not in range");
        }

        // check elevation (target must be within one level)
        if (targetHeight < attackerElevation || targetElevation > attackerHeight) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Target elevation not in range");
        }

        // can't physically attack mechs making dfa attacks
        if ( te != null && te.isMakingDfa() ) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Target is making a DFA attack");
        }

        // check facing
        int clubArc = bothArms ? Compute.ARC_FORWARD : (club.getLocation() == Mech.LOC_LARM ? Compute.ARC_LEFTARM : Compute.ARC_RIGHTARM);
        if ( !isInArc( ae.getPosition(), ae.getSecondaryFacing(),
                       target.getPosition(), clubArc ) ) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Target not in arc");
        }

        // can't club while prone
        if (ae.isProne()) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Attacker is prone");
        }

        // Attacks against adjacent buildings automatically hit.
        if ( target.getTargetType() == Targetable.TYPE_BUILDING ) {
            return new ToHitData( ToHitData.AUTOMATIC_SUCCESS,
                                  "Targeting adjacent building." );
        }

        // Can't target woods or ignite a building with a physical.
        if ( target.getTargetType() == Targetable.TYPE_BLDG_IGNITE ||
             target.getTargetType() == Targetable.TYPE_HEX_CLEAR ||
             target.getTargetType() == Targetable.TYPE_HEX_IGNITE ) {
            return new ToHitData( ToHitData.IMPOSSIBLE, "Invalid attack");
        }

        //Set the base BTH
        int base = 4;

        if ( game.getOptions().booleanOption("maxtech_physical_BTH") ) {
        	base = ae.getCrew().getPiloting() - 1;
        }

        if (club.getType().hasFlag(MiscType.F_SWORD)) {
            base--;
        }

        toHit = new ToHitData(base, "base");

        // Battle Armor targets are hard for Meks and Tanks to hit.
        if ( te instanceof BattleArmor ) {
            toHit.addModifier( 1, "battle armor target" );
        }

        // attacker movement
        toHit.append(getAttackerMovementModifier(game, attackerId));

        // target movement
        toHit.append(getTargetMovementModifier(game, targetId));

        // attacker terrain
        toHit.append(getAttackerTerrainModifier(game, attackerId));

        // target terrain
        toHit.append(getTargetTerrainModifier(game, te));

        // damaged or missing actuators
        if (bothArms) {
            if (!ae.hasWorkingSystem(Mech.ACTUATOR_UPPER_ARM, Mech.LOC_RARM)) {
                toHit.addModifier(2, "Upper arm actuator destroyed");
            }
            if (!ae.hasWorkingSystem(Mech.ACTUATOR_UPPER_ARM, Mech.LOC_LARM)) {
                toHit.addModifier(2, "Upper arm actuator destroyed");
            }
            if (!ae.hasWorkingSystem(Mech.ACTUATOR_LOWER_ARM, Mech.LOC_RARM)) {
                toHit.addModifier(2, "Lower arm actuator missing or destroyed");
            }
            if (!ae.hasWorkingSystem(Mech.ACTUATOR_LOWER_ARM, Mech.LOC_LARM)) {
                toHit.addModifier(2, "Lower arm actuator missing or destroyed");
            }
        } else {
            if (!ae.hasWorkingSystem(Mech.ACTUATOR_UPPER_ARM, club.getLocation())) {
                toHit.addModifier(2, "Upper arm actuator destroyed");
            }
            if (!ae.hasWorkingSystem(Mech.ACTUATOR_LOWER_ARM, club.getLocation())) {
                toHit.addModifier(2, "Lower arm actuator missing or destroyed");
            }
        }

        // target prone
        if (te.isProne()) {
            toHit.addModifier(-2, "target prone and adjacent");
        }

        // water partial cover?
        Hex targHex = game.board.getHex(te.getPosition());
        if (te.height() > 0 && targHex.levelOf(Terrain.WATER) == te.height()) {
            toHit.addModifier(3, "target has partial cover");
        }

        // target immobile
        toHit.append(getImmobileMod(te));

        modifyPhysicalBTHForAdvantages(ae, te, toHit, game);

        // elevation
        if (attackerElevation == targetElevation) {
            toHit.setHitTable(ToHitData.HIT_NORMAL);
        } else if (attackerElevation < targetElevation) {
            if (te.height() == 0) {
                toHit.setHitTable(ToHitData.HIT_NORMAL);
            } else {
                toHit.setHitTable(ToHitData.HIT_KICK);
            }
        } else {
            toHit.setHitTable(ToHitData.HIT_PUNCH);
        }

        // factor in target side
        toHit.setSideTable(targetSideTable(ae,te));

        // done!
        return toHit;
    }

    /**
     * Damage that the specified mech does with a club attack
     */
    public static int getClubDamageFor(Entity entity, Mounted club) {

        int nDamage = (int)Math.floor(entity.getWeight() / 5.0);
        if (club.getType().hasFlag(MiscType.F_SWORD)) {
            nDamage = (int)(Math.ceil(entity.getWeight() / 10.0) + 1.0);
        }
        if (entity.heat >= 9 && ((Mech)entity).hasTSM()) {
            nDamage *= 2;
        }
        int clubLocation = club.getLocation();
        // tree clubs don't have a location--use right arm (is this okay?)
        if (clubLocation == Entity.LOC_NONE) {
            clubLocation = Mech.LOC_RARM;
        }
        if (entity.getLocationStatus(clubLocation) == Entity.LOC_WET) {
            nDamage /= 2.0f;
        }

        return nDamage + entity.getCrew().modifyPhysicalDamagaForMeleeSpecialist();
    }

    public static ToHitData toHitPush(Game game, PushAttackAction paa) {
        return toHitPush(game, paa.getEntityId(),
                         game.getTarget(paa.getTargetType(), paa.getTargetId()));
    }

    /**
     * To-hit number for the mech to push another mech
     */
    public static ToHitData toHitPush(Game game,
                                      int attackerId,
                                      Targetable target) {
        final Entity ae = game.getEntity(attackerId);
        int targetId = Entity.NONE;
        Entity te = null;
        if ( target.getTargetType() == Targetable.TYPE_ENTITY ) {
            te = (Entity) target;
            targetId = target.getTargetId();
        }
        final int attackerElevation = ae.getElevation();
        final int targetElevation = target.getElevation();
        final boolean targetInBuilding = Compute.isInBuilding( game, te );
        Building bldg = null;
        if ( targetInBuilding ) {
            bldg = game.board.getBuildingAt( te.getPosition() );
        }
        ToHitData toHit = null;

        // arguments legal?
        if (ae == null || target == null) {
            throw new IllegalArgumentException("Attacker or target not valid");
        }

        // can't target yourself
        if (ae.equals(te)) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "You can't target yourself");
        }

        // non-mechs can't push
        if (!(ae instanceof Mech)) {
        	return new ToHitData(ToHitData.IMPOSSIBLE, "Non-mechs can't push");
        }

        //Quads can't push
        if ( ae.entityIsQuad() ) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Attacker is a quad");
        }

        // can't make physical attacks while spotting
        if (ae.isSpotting()) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Attacker is spotting this turn");
        }

        //Can only push mechs
        if ( te !=null && !(te instanceof Mech) ) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Target is not a mech");
        }

        //Can't push with flipped arms
        if (ae.getArmsFlipped()) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Arms are flipped to the rear. Can not push.");
        }

        // Can't target a transported entity.
        if ( te != null && Entity.NONE != te.getTransportId() ) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Target is a passenger.");
        }

        // Can't target a entity conducting a swarm attack.
        if ( te != null && Entity.NONE != te.getSwarmTargetId() ) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Target is swarming a Mek.");
        }

        // check if both arms are present
        if (ae.isLocationBad(Mech.LOC_RARM)
            || ae.isLocationBad(Mech.LOC_LARM)) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Arm missing");
        }

        // check if attacker has fired arm-mounted weapons
        if (ae.weaponFiredFrom(Mech.LOC_RARM)
        || ae.weaponFiredFrom(Mech.LOC_LARM)) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Weapons fired from arm this turn");
        }

        // check range
        if (ae.getPosition().distance(target.getPosition()) > 1 ) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Target not in range");
        }

        // target must be at same elevation
        if (attackerElevation != targetElevation) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Target not at same elevation");
        }

        // can't push mech making non-pushing displacement attack
        if ( te != null && te.hasDisplacementAttack() && !te.isPushing() ) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Target is making a charge/DFA attack");
        }

        // can't push mech pushing another, different mech
        if ( te != null && te.isPushing() &&
             te.getDisplacementAttack().getTargetId() != ae.getId() ) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Target is pushing another mech");
        }

        // can't do anything but counter-push if the target of another attack
        if (ae.isTargetOfDisplacementAttack() && ae.findTargetedDisplacement().getEntityId() != target.getTargetId()) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Attacker is the target of another push/charge/DFA");
        }

        // can't attack the target of another displacement attack
        if ( te != null && te.isTargetOfDisplacementAttack() &&
             te.findTargetedDisplacement().getEntityId() != ae.getId() ) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Target is the target of another push/charge/DFA");
        }

        // check facing
        if (!target.getPosition().equals(ae.getPosition().translated(ae.getFacing()))) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Target not directly ahead of feet");
        }

        // can't push while prone
        if (ae.isProne()) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Attacker is prone");
        }

        // can't push prone mechs
        if ( te != null && te.isProne()) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Target is prone");
        }

        // Can't target units in buildings (from the outside).
        if ( targetInBuilding ) {
            if ( !isInBuilding(game, ae) ) {
                return new ToHitData(ToHitData.IMPOSSIBLE, "Target is inside building" );
            }
            else if ( !game.board.getBuildingAt( ae.getPosition() )
                      .equals( bldg ) ) {
                return new ToHitData(ToHitData.IMPOSSIBLE, "Target is inside differnt building" );
            }
        }

        // Attacks against adjacent buildings automatically hit.
        if ( target.getTargetType() == Targetable.TYPE_BUILDING ) {
            return new ToHitData( ToHitData.IMPOSSIBLE,
                                  "You can not push a building (well, you can, but it won't do anything)." );
        }

        // Can't target woods or ignite a building with a physical.
        if ( target.getTargetType() == Targetable.TYPE_BLDG_IGNITE ||
             target.getTargetType() == Targetable.TYPE_HEX_CLEAR ||
             target.getTargetType() == Targetable.TYPE_HEX_IGNITE ) {
            return new ToHitData( ToHitData.IMPOSSIBLE, "Invalid attack");
        }

        //Set the base BTH
          int base = 4;

          if ( game.getOptions().booleanOption("maxtech_physical_BTH") ) {
            base = ae.getCrew().getPiloting() - 1;
          }

          toHit = new ToHitData(base, "base");

        // attacker movement
        toHit.append(getAttackerMovementModifier(game, attackerId));

        // target movement
        toHit.append(getTargetMovementModifier(game, targetId));

        // attacker terrain
        toHit.append(getAttackerTerrainModifier(game, attackerId));

        // target terrain
        toHit.append(getTargetTerrainModifier(game, te));

        // damaged or missing actuators
        if (!ae.hasWorkingSystem(Mech.ACTUATOR_SHOULDER, Mech.LOC_RARM)) {
            toHit.addModifier(2, "Right Shoulder destroyed");
        }
        if (!ae.hasWorkingSystem(Mech.ACTUATOR_SHOULDER, Mech.LOC_LARM)) {
            toHit.addModifier(2, "Left Shoulder destroyed");
        }

        // water partial cover?
        Hex targHex = game.board.getHex(target.getPosition());
        if (te.height() > 0 && targHex.levelOf(Terrain.WATER) == te.height()) {
            toHit.addModifier(3, "target has partial cover");
        }

        // target immobile
        toHit.append(getImmobileMod(te));

        modifyPhysicalBTHForAdvantages(ae, te, toHit, game);

        // side and elevation shouldn't matter

        // done!
        return toHit;
    }
    
    /**
     * Damage a Protomech does with it's Combo-physicalattack.
     */
    public static int getProtoPhysicalDamageFor(Entity entity) {
    	if (entity.getWeight() >= 2 && entity.getWeight() < 6) {
    		return 1;
    	} else return 2;
    }
    
    public static ToHitData toHitProto(Game game, ProtomechPhysicalAttackAction ppaa) {
        return toHitProto(game, ppaa.getEntityId(),
                         game.getTarget(ppaa.getTargetType(), ppaa.getTargetId()));
    }
    
    public static ToHitData toHitProto(Game game, int attackerId, Targetable target) {
    	final Entity ae = game.getEntity(attackerId);
        int targetId = Entity.NONE;
        Entity te = null;
        if ( target.getTargetType() == Targetable.TYPE_ENTITY ) {
            te = (Entity) target;
            targetId = target.getTargetId();
        }
        final int attackerElevation = ae.getElevation();
        final int targetHeight = target.absHeight();
        final int targetElevation = target.getElevation();
        final boolean targetInBuilding = isInBuilding( game, te );
        Building bldg = null;
        if ( targetInBuilding ) {
            bldg = game.board.getBuildingAt( te.getPosition() );
        }
        
        ToHitData toHit;

        // arguments legal?
        if (ae == null || target == null) {
            throw new IllegalArgumentException("Attacker or target not valid");
        }

        // can't target yourself
        if (ae.equals(te)) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "You can't target yourself");
        }

        // non-protos can't make protomech-physicalattacks
        if (!(ae instanceof Protomech)) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Non-protos can't make proto-physicalattacks");
        }

        // can't make physical attacks while spotting
        if (ae.isSpotting()) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Attacker is spotting this turn");
        }

        // Can't target a transported entity.
        if ( te != null && Entity.NONE != te.getTransportId() ) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Target is a passenger.");
        }

        // Can't target a entity conducting a swarm attack.
        if ( te != null && Entity.NONE != te.getSwarmTargetId() ) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Target is swarming a Mek.");
        }

        // check range
        final int range = ae.getPosition().distance(target.getPosition());
        if ( range > 1 ) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Target not in range");
        }

        // check elevation
        if (attackerElevation < targetElevation || attackerElevation > targetHeight) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Target elevation not in range");
        }

        // can't physically attack mechs making dfa attacks
        if ( te != null && te.isMakingDfa() ) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Target is making a DFA attack");
        }
        
        //can only target targets in adjacent hexes, not in same hex
        if (range == 0) {
        	return new ToHitData(ToHitData.IMPOSSIBLE, "Target not in adjacent hex");
        }
        
        // check facing
        // Don't check arc for stomping infantry or tanks.
        if (0 != range &&
            !isInArc(ae.getPosition(), ae.getFacing(),
                     target.getPosition(), Compute.ARC_FORWARD)) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Target not in arc");
        }

        // Can't target units in buildings (from the outside).
        if ( 0 != range && targetInBuilding ) {
            if ( !isInBuilding(game, ae) ) {
                return new ToHitData(ToHitData.IMPOSSIBLE, "Target is inside building" );
            }
            else if ( !game.board.getBuildingAt( ae.getPosition() )
                      .equals( bldg ) ) {
                return new ToHitData(ToHitData.IMPOSSIBLE, "Target is inside differnt building" );
            }
        }

        // Attacks against adjacent buildings automatically hit.
        if ( target.getTargetType() == Targetable.TYPE_BUILDING ) {
            return new ToHitData( ToHitData.AUTOMATIC_SUCCESS,
                                  "Targeting adjacent building." );
        }

        // Can't target woods or ignite a building with a physical.
        if ( target.getTargetType() == Targetable.TYPE_BLDG_IGNITE ||
             target.getTargetType() == Targetable.TYPE_HEX_CLEAR ||
             target.getTargetType() == Targetable.TYPE_HEX_IGNITE ) {
            return new ToHitData( ToHitData.IMPOSSIBLE, "Invalid attack");
        }

        //Set the base BTH
        int base = 4;

        // Start the To-Hit
        toHit = new ToHitData(base, "base");

        // attacker movement
        toHit.append(getAttackerMovementModifier(game, attackerId));

        // target movement
        toHit.append(getTargetMovementModifier(game, targetId));

        // attacker terrain
        toHit.append(getAttackerTerrainModifier(game, attackerId));

        // target terrain
        toHit.append(getTargetTerrainModifier(game, te));

        // target prone
        if (te.isProne()) {
            toHit.addModifier(-2, "target prone and adjacent");
        }

        // water partial cover?
        Hex targHex = game.board.getHex(te.getPosition());
        if (te.height() > 0 && targHex.levelOf(Terrain.WATER) == te.height()) {
            toHit.addModifier(3, "target has partial cover");
        }

        // target immobile
        toHit.append(getImmobileMod(te));

        modifyPhysicalBTHForAdvantages(ae, te, toHit, game);

        // elevation
        if (attackerElevation < targetHeight) {
            toHit.setHitTable(ToHitData.HIT_KICK);
        } else if (te.height() > 0) {
            toHit.setHitTable(ToHitData.HIT_PUNCH);
        } else {
            toHit.setHitTable(ToHitData.HIT_NORMAL);
        }

        // factor in target side
        toHit.setSideTable(targetSideTable(ae,te));

        // done!
        return toHit;
    }

    /**
     * Checks if a death from above attack can hit the target, including movement
     */
    public static ToHitData toHitDfa(Game game, int attackerId, Targetable target, MovePath md) {
        final Entity ae = game.getEntity(attackerId);
        Entity te = null;
        if ( target.getTargetType() == Targetable.TYPE_ENTITY ) {
            te = (Entity) target;
        }
        Coords chargeSrc = ae.getPosition();
        MoveStep chargeStep = null;

        // Infantry CAN'T dfa!!!
        if ( ae instanceof Infantry ) {
        	return new ToHitData(ToHitData.IMPOSSIBLE, "Infantry can't D.F.A.");
        }

        // let's just check this
        if (!md.contains(MovePath.STEP_DFA)) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "D.F.A. action not found in movment path");
        }

        // have to jump
        if (!md.contains(MovePath.STEP_START_JUMP)) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "D.F.A. must involve jumping");
        }

        // Can't target a transported entity.
        if ( te != null && Entity.NONE != te.getTransportId() ) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Target is a passenger.");
        }

        // can't make physical attacks while spotting
        if (ae.isSpotting()) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Attacker is spotting this turn");
        }

        // Can't target a entity conducting a swarm attack.
        if ( te != null && Entity.NONE != te.getSwarmTargetId() ) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Target is swarming a Mek.");
        }

        // determine last valid step
        md.compile(game, ae);
        for (final Enumeration i = md.getSteps(); i.hasMoreElements();) {
            final MoveStep step = (MoveStep)i.nextElement();
            if (step.getMovementType() == Entity.MOVE_ILLEGAL) {
                break;
            } else {
                if (step.getType() == MovePath.STEP_DFA) {
                    chargeStep = step;
                } else {
                    chargeSrc = step.getPosition();
                }
            }
        }

        // need to reach target
        if (chargeStep == null || !target.getPosition().equals(chargeStep.getPosition())) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Could not reach target with movement");
        }

        // target must have moved already
        if ( te != null && !te.isDone() ) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Target must be done with movement");
        }

        return toHitDfa(game, attackerId, target, chargeSrc);
    }

    public static ToHitData toHitDfa(Game game, DfaAttackAction daa) {
        final Entity entity = game.getEntity(daa.getEntityId());
        return toHitDfa(game, daa.getEntityId(),
                        game.getTarget(daa.getTargetType(), daa.getTargetId()),
                        entity.getPosition());
    }

    /**
     * To-hit number for a death from above attack, assuming that movement has
     * been handled
     */
    public static ToHitData toHitDfa(Game game, int attackerId, Targetable target, Coords src) {
        final Entity ae = game.getEntity(attackerId);
        int targetId = Entity.NONE;
        Entity te = null;
        if ( target.getTargetType() == Targetable.TYPE_ENTITY ) {
            te = (Entity) target;
            targetId = target.getTargetId();
        }
        final boolean targetInBuilding = isInBuilding( game, te );
        ToHitData toHit = null;

        // arguments legal?
        if ( ae == null || target == null ) {
            throw new IllegalArgumentException("Attacker or target not valid");
        }

        // can't target yourself
        if (ae.equals(te)) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "You can't target yourself");
        }

        // Infantry CAN'T dfa!!!
        if ( ae instanceof Infantry ) {
        	return new ToHitData(ToHitData.IMPOSSIBLE, "Infantry can't dfa");
        }

        // Can't target a transported entity.
        if ( te != null && Entity.NONE != te.getTransportId() ) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Target is a passenger.");
        }

        // Can't target a entity conducting a swarm attack.
        if ( te != null && Entity.NONE != te.getSwarmTargetId() ) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Target is swarming a Mek.");
        }

        // check range
        if (src.distance(target.getPosition()) > 1 ) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Target not in range");
        }

        // can't dfa while prone, even if you somehow did manage to jump
        if (ae.isProne()) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Attacker is prone");
        }

        // can't attack mech making a different displacement attack
        if ( te != null && te.hasDisplacementAttack() ) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Target is already making a charge/DFA attack");
        }

        // can't attack the target of another displacement attack
        if ( te != null && te.isTargetOfDisplacementAttack() &&
             te.findTargetedDisplacement().getEntityId() != ae.getId() ) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Target is the target of another charge/DFA");
        }

        // Can't target units in buildings (from the outside).
        if ( targetInBuilding ) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Target is inside building" );
        }

        // Attacks against adjacent buildings automatically hit.
        if ( target.getTargetType() == Targetable.TYPE_BUILDING ) {
            return new ToHitData( ToHitData.AUTOMATIC_SUCCESS,
                                  "Targeting adjacent building." );
        }

        // Can't target woods or ignite a building with a physical.
        if ( target.getTargetType() == Targetable.TYPE_BLDG_IGNITE ||
             target.getTargetType() == Targetable.TYPE_HEX_CLEAR ||
             target.getTargetType() == Targetable.TYPE_HEX_IGNITE ) {
            return new ToHitData( ToHitData.IMPOSSIBLE, "Invalid attack");
        }

        //Set the base BTH
        int base = 5;

        if ( game.getOptions().booleanOption("maxtech_physical_BTH") ) {
            base = ae.getCrew().getPiloting();
        }

        toHit = new ToHitData(base, "base");

          // BMR(r), page 33. +3 modifier for DFA on infantry.
        if ( te instanceof Infantry ) {
        	toHit.addModifier( 3, "Infantry target" );
        }

        // Battle Armor targets are hard for Meks and Tanks to hit.
        if ( te instanceof BattleArmor ) {
            toHit.addModifier( 1, "battle armor target" );
        }

        // attacker movement
        toHit.append(getAttackerMovementModifier(game, attackerId, Entity.MOVE_JUMP));

        // target movement
        toHit.append(getTargetMovementModifier(game, targetId));

        // piloting skill differential
        if (ae.getCrew().getPiloting() != te.getCrew().getPiloting()) {
            toHit.addModifier(ae.getCrew().getPiloting() - te.getCrew().getPiloting(), "piloting skill differential");
        }

        // target prone
        if (te.isProne()) {
            toHit.addModifier(-2, "target prone and adjacent");
        }

        // water partial cover?
        Hex targHex = game.board.getHex(te.getPosition());
        if (te.height() > 0 && targHex.levelOf(Terrain.WATER) == te.height()) {
            toHit.addModifier(3, "target has partial cover");
        }

        // target immobile
        toHit.append(getImmobileMod(te));

        modifyPhysicalBTHForAdvantages(ae, te, toHit, game);

        if (te instanceof Tank) {
            toHit.setSideTable(ToHitData.SIDE_FRONT);
            toHit.setHitTable(ToHitData.HIT_NORMAL);
        }
        else if (te.isProne()) {
            toHit.setSideTable(ToHitData.SIDE_REAR);
            toHit.setHitTable(ToHitData.HIT_NORMAL);
        }
        else {
            toHit.setSideTable(targetSideTable(src, te.getPosition(),
                                            te.getFacing(), te instanceof Tank));
            toHit.setHitTable(ToHitData.HIT_PUNCH);
        }

        // done!
        return toHit;
    }

    /**
     * Damage that a mech does with a successful DFA.
     */
    public static int getDfaDamageFor(Entity entity) {
        return (int)Math.ceil((entity.getWeight() / 10.0) * 3.0);
    }
    
    /**
     * Damage that a mech does with a accidental fall from above.
     */
    
    public static int getAffaDamageFor(Entity entity) {
    	return (int)entity.getWeight() / 10;
    }

    /**
     * Damage done to a mech after a successful DFA.
     */
    public static int getDfaDamageTakenBy(Entity entity) {
        return (int)Math.ceil(entity.getWeight() / 5.0);
    }


    /**
     * Modifier to attacks due to attacker movement
     */
    public static ToHitData getAttackerMovementModifier(Game game, int entityId) {
        return getAttackerMovementModifier(game, entityId, game.getEntity(entityId).moved);
    }

    /**
     * Modifier to attacks due to attacker movement
     */
    public static ToHitData getAttackerMovementModifier(Game game, int entityId, int movement) {
        final Entity entity = game.getEntity(entityId);
        ToHitData toHit = new ToHitData();

        // infantry aren't affected by their own movement
        if (entity instanceof Infantry) {
            return toHit;
        }

        if (movement == Entity.MOVE_WALK) {
            toHit.addModifier(1, "attacker walked");
        } else if (movement == Entity.MOVE_RUN ||
                   movement == Entity.MOVE_SKID) {
            toHit.addModifier(2, "attacker ran");
        } else if (movement == Entity.MOVE_JUMP) {
            toHit.addModifier(3, "attacker jumped");
        }

        return toHit;
    }


    /**
     * Modifier to attacks due to spotter movement
     */
    public static ToHitData getSpotterMovementModifier(Game game, int entityId) {
        return getSpotterMovementModifier(game, entityId, game.getEntity(entityId).moved);
    }

    /**
     * Modifier to attacks due to spotter movement
     */
    public static ToHitData getSpotterMovementModifier(Game game, int entityId, int movement) {
        final Entity entity = game.getEntity(entityId);
        ToHitData toHit = new ToHitData();

        // infantry aren't affected by their own movement
        if (entity instanceof Infantry) {
            return toHit;
        }

        if (movement == Entity.MOVE_WALK) {
            toHit.addModifier(1, "spotter walked");
        } else if (movement == Entity.MOVE_RUN ||
                   movement == Entity.MOVE_SKID) {
            toHit.addModifier(2, "spotter ran");
        } else if (movement == Entity.MOVE_JUMP) {
            toHit.addModifier(3, "spotter jumped");
        }

        return toHit;
    }

    /**
     * Modifier to physical attack BTH due to pilot advantages
     */
    public static void modifyPhysicalBTHForAdvantages(Entity attacker, Entity target, ToHitData toHit, Game game) {

        if (attacker.getCrew().getOptions().booleanOption("melee_specialist")
            && attacker instanceof Mech
            && getTargetMovementModifier(game, target.getId()).getValue() > 0) {
            toHit.addModifier(-1, "melee specialist");
        }

        if (target instanceof Mech &&
            target.getCrew().getOptions().booleanOption("dodge_maneuver") &&
            (target.dodging || !target.isEligibleForPhysical())) {
            toHit.addModifier(2, "target is dodging");
        }
    }

    /**
     * Modifier to attacks due to target movement
     */
    public static ToHitData getTargetMovementModifier(Game game, int entityId) {
        Entity entity = game.getEntity(entityId);
        ToHitData toHit = getTargetMovementModifier
            ( entity.delta_distance, entity.moved == Entity.MOVE_JUMP, game.getOptions().booleanOption("maxtech_target_modifiers") );

        // Did the target skid this turn?
        if ( entity.moved == Entity.MOVE_SKID ) {
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
    public static ToHitData getAttackerTerrainModifier(Game game, int entityId) {
        final Entity attacker = game.getEntity(entityId);
        final Hex hex = game.board.getHex(attacker.getPosition());
        ToHitData toHit = new ToHitData();

        if (hex.levelOf(Terrain.WATER) > 0
        && attacker.getMovementType() != Entity.MovementType.HOVER) {
            toHit.addModifier(1, "attacker in water");
        }

        return toHit;
    }

    /**
     * Modifier to attacks due to target terrain
     */
    public static ToHitData getTargetTerrainModifier(Game game, Targetable t) {
        Entity entityTarget = null;
        if (t.getTargetType() == Targetable.TYPE_ENTITY) {
            entityTarget = (Entity) t;
        }
        final Hex hex = game.board.getHex(t.getPosition());

        // you don't get terrain modifiers in midair
        // should be abstracted more into a 'not on the ground' flag for vtols and such
        if (entityTarget != null && entityTarget.isMakingDfa()) {
            return null;
        }

        ToHitData toHit = new ToHitData();

        // only entities get terrain bonuses
        // TODO: should this be changed for buildings???
        if (entityTarget == null) {
            return toHit;
        }

        if (hex.levelOf(Terrain.WATER) > 0
        && entityTarget.getMovementType() != Entity.MovementType.HOVER) {
            toHit.addModifier(-1, "target in water");
        }

        // Smoke and woods. With L3, the effects STACK.
        if (!game.getOptions().booleanOption("maxtech_fire")) { // L2
            if (hex.contains(Terrain.SMOKE)) {
                toHit.addModifier(2, "target in smoke");
            } else if (hex.levelOf(Terrain.WOODS) == 1) {
                toHit.addModifier(1, "target in light woods");
            } else if (hex.levelOf(Terrain.WOODS) > 1) {
                toHit.addModifier(2, "target in heavy woods");
            }
            return toHit;
        } else { // L3
            if (hex.levelOf(Terrain.SMOKE) == 1) {
                toHit.addModifier(1, "target in light smoke");
            } else if (hex.levelOf(Terrain.SMOKE) > 1) {
                toHit.addModifier(2, "target in heavy smoke");
            }
            
            if (hex.levelOf(Terrain.WOODS) == 1) {
                toHit.addModifier(1, "target in light woods");
            } else if (hex.levelOf(Terrain.WOODS) > 1) {
                toHit.addModifier(2, "target in heavy woods");
            }
            return toHit;
        }
    }

    /**
     * Returns the weapon attack out of a list that has the highest expected damage
     */
    public static WeaponAttackAction getHighestExpectedDamage(Game g, Vector vAttacks)
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
            4.0f, 0.0f, 0.0f, 5.47f, 6.31f, 0.0f, 8.14f, 0.0f, 0.0f, 9.5f, 0.0f, 0.0f, 0.0f,
            0.0f, 12.7f };

    /**
     * Determines the expected damage of a weapon attack, based on to-hit, salvo sizes, etc.
     */
    public static float getExpectedDamage(Game g, WeaponAttackAction waa)
    {
        Entity attacker = g.getEntity(waa.getEntityId());
        Mounted weapon = attacker.getEquipment(waa.getWeaponId());
        System.out.println("Computing expected damage for " + attacker.getShortName() + " " +
                weapon.getName());
        ToHitData hitData = Compute.toHitWeapon(g, waa);
        if (hitData.getValue() == ToHitData.IMPOSSIBLE || hitData.getValue() == ToHitData.AUTOMATIC_FAIL) {
            return 0.0f;
        }

        float fChance = 0.0f;
        if (hitData.getValue() == ToHitData.AUTOMATIC_SUCCESS) {
            fChance = 1.0f;
        }
        else {
            fChance = (float)oddsAbove(hitData.getValue()) / 100.0f;
        }
        System.out.println("\tHit Chance: " + fChance);

        // TODO : update for BattleArmor.

        float fDamage = 0.0f;
        WeaponType wt = (WeaponType)weapon.getType();
        if (wt.getDamage() == WeaponType.DAMAGE_MISSILE) {
            if (weapon.getLinked() == null) return 0.0f;
            AmmoType at = (AmmoType)weapon.getLinked().getType();

            float fHits = 0.0f;
            if (wt.getAmmoType() == AmmoType.T_SRM_STREAK) {
                fHits = wt.getRackSize();
            }
            else if (wt.getRackSize() == 40 || wt.getRackSize() == 30) {
                fHits = 2.0f * expectedHitsByRackSize[wt.getRackSize() / 2];
            }
            else {
                fHits = expectedHitsByRackSize[wt.getRackSize()];
            }
            // adjust for previous AMS
            Vector vCounters = waa.getCounterEquipment();
            if (vCounters != null) {
                for (int x = 0; x < vCounters.size(); x++) {
                    Mounted counter = (Mounted)vCounters.elementAt(x);
                    if (counter.getType() instanceof WeaponType &&
                            ((WeaponType)counter.getType()).getAmmoType() == AmmoType.T_AMS) {
                        float fAMS = 3.5f * ((WeaponType)counter.getType()).getDamage();
                        fHits = Math.max(0.0f, fHits - fAMS);
                    }
                }
            }
            System.out.println("\tExpected Hits: " + fHits);
            // damage is expected missiles * damage per missile
            fDamage = fHits * (float)at.getDamagePerShot();
        }
        else {
            fDamage = (float)wt.getDamage();
        }

        fDamage *= fChance;
        System.out.println("\tExpected Damage: " + fDamage);
        return fDamage;
    }

    /**
     * Checks to see if a target is in arc of the specified
     * weapon, on the specified entity
     */
    public static boolean isInArc(Game game, int attackerId, int weaponId, Targetable t) {
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
    public static boolean canSee(Game game, Entity ae, Targetable target)
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

    public static boolean canPhysicalTarget(Game game, int entityId, Targetable target) {
        boolean canHit = false;

        canHit |= Compute.toHitPunch
            ( game, entityId, target,
             PunchAttackAction.LEFT ).getValue()
            != ToHitData.IMPOSSIBLE;

        canHit |= Compute.toHitPunch
            ( game, entityId, target,
             PunchAttackAction.RIGHT ).getValue()
            != ToHitData.IMPOSSIBLE;

        canHit |= Compute.toHitKick
            ( game, entityId, target,
             KickAttackAction.LEFT ).getValue()
            != ToHitData.IMPOSSIBLE;

        canHit |= Compute.toHitKick
            ( game, entityId, target,
             KickAttackAction.RIGHT ).getValue()
            != ToHitData.IMPOSSIBLE;

        canHit |= BrushOffAttackAction.toHitBrushOff
            ( game, entityId, target,
              BrushOffAttackAction.LEFT ).getValue()
            != ToHitData.IMPOSSIBLE;

        canHit |= BrushOffAttackAction.toHitBrushOff
            ( game, entityId, target,
              BrushOffAttackAction.RIGHT ).getValue()
            != ToHitData.IMPOSSIBLE;

        canHit |= new ThrashAttackAction(entityId, target).toHit(game).getValue()
            != ToHitData.IMPOSSIBLE;

        canHit |= Compute.toHitProto(game, entityId, target).getValue()
            != ToHitData.IMPOSSIBLE;

        Mounted club = Compute.clubMechHas( game.getEntity(entityId) );
        if ( null != club ) {
            canHit |= Compute.toHitClub
                ( game, entityId, target,
                  club ).getValue()
                != ToHitData.IMPOSSIBLE;
        }

        canHit |= Compute.toHitPush
            ( game, entityId, target ).getValue()
            != ToHitData.IMPOSSIBLE;

        return canHit;
    }

    /**
     * Can movement between the two coordinates be on pavement (which includes
     * roads and bridges)?  If so it will override prohibited terrain, it may
     * change movement costs, and it may lead to skids.
     *
     * @param   game - the <code>Game</code> object.
     * @param   src - the <code>Coords</code> being left.
     * @param   dest - the <code>Coords</code> being entered.
     * @return  <code>true</code> if movement between <code>src</code> and
     *          <code>dest</code> can be on pavement; <code>false</code>
     *          otherwise.
     */
    public static boolean canMoveOnPavement( Game game,
                                             Coords src, Coords dest ) {
        final Hex srcHex = game.board.getHex(src);
        final Hex destHex = game.board.getHex(dest);
        final int src2destDir = src.direction(dest);
        final int dest2srcDir = (src2destDir + 3) % 6;
        boolean result = false;

        // We may be moving in the same hex.
        if ( src.equals(dest) &&
             ( srcHex.contains(Terrain.PAVEMENT) ||
               srcHex.contains(Terrain.ROAD) ||
               srcHex.contains(Terrain.BRIDGE) ) ) {
            result = true;
        }

        // If the source is a pavement hex, then see if the destination
        // hex is also a pavement hex or has a road or bridge that exits
        // into the source hex.
        else if ( srcHex.contains(Terrain.PAVEMENT) &&
             ( destHex.contains(Terrain.PAVEMENT) ||
               destHex.containsTerrainExit(Terrain.ROAD, dest2srcDir) ||
               destHex.containsTerrainExit(Terrain.BRIDGE, dest2srcDir) ) ) {
            result = true;
        }

        // See if the source hex has a road or bridge that exits into the
        // destination hex.
        else if ( srcHex.containsTerrainExit(Terrain.ROAD, src2destDir) ||
                  srcHex.containsTerrainExit(Terrain.BRIDGE, src2destDir) ) {
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
  public static boolean isInSameBuilding(Game game, Entity attacker, Targetable target) {
    if (!(target instanceof Entity)) {
      return false;
    }
    Entity targetEntity = (Entity)target;
    if (!isInBuilding(game, attacker) || !isInBuilding(game, targetEntity)) {
      return false;
    }

    Building attkBldg = game.board.getBuildingAt(attacker.getPosition());
    Building targBldg = game.board.getBuildingAt(target.getPosition());

    return attkBldg.equals(targBldg);
  }

    /**
     * Determine if the given unit is inside of a building at the given
     * coordinates.
     *
     * @param   game - the <code>Game</code> object.
     *          This value may be <code>null</code>.
     * @param   entity - the <code>Entity</code> to be checked.
     *          This value may be <code>null</code>.
     * @return  <code>true</code> if the entity is inside of the building
     *          at those coordinates.  <code>false</code> if there is no
     *          building at those coordinates or if the entity is on the
     *          roof or in the air above the building, or if any input
     *          argument is <code>null</code>.
     */
    public static boolean isInBuilding( Game game,
                                        Entity entity ) {

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
     * @param   game - the <code>Game</code> object.
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
    public static boolean isInBuilding( Game game,
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
        final Hex curHex = game.board.getHex( coords );

        return isInBuilding(game, entity.elevationOccupied(curHex), coords);
    }

    static boolean isInBuilding(Game game, int entityElev, Coords coords ) {

		// Get the Hex at those coordinates.
		final Hex curHex = game.board.getHex( coords );

		// The entity can't be inside of a building that isn't there.
		if ( !curHex.contains( Terrain.BLDG_ELEV ) ) {
			return false;
		}

		// Get the elevations occupied by the building.
		int surface = curHex.surface();
		int bldgHeight = curHex.levelOf( Terrain.BLDG_ELEV );
		int basement = 0;
		if ( curHex.contains( Terrain.BLDG_BASEMENT ) ) {
			basement = curHex.levelOf( Terrain.BLDG_BASEMENT );
		}

		// Return true if the entity is in the range of building elevations.
		if ( entityElev >= (surface - basement) &&
			 entityElev < (surface + bldgHeight) ) {
			return true;
		}

		// Entity is not *inside* of the building.
		return false;
	}

    public static Coords scatter(Coords coords) {
    	int scatterDirection = d6(1) - 1;
    	int scatterDistance = d6(1);

    	for (int i = 0; i < scatterDistance; i++) {
    		coords = coords.translated(scatterDirection);
    	}
    	return coords;
    }
    
} // End public class Compute
