/*
 * MegaMek - Copyright (C) 2001,2002,2003,2004 Ben Mazur (bmazur@sev.org)
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

/*
 * ClubAttackAction.java
 *
 * Created on April 3, 2002, 2:37 PM
 */

package megamek.common.actions;

import megamek.common.*;

/**
 * The attacker makes a club attack on the target.  This also covers mech
 * melee weapons like hatchets.
 *
 * @author  Ben
 * @version 
 */
public class ClubAttackAction extends AbstractAttackAction {
    
    private Mounted club;

    /** Creates new ClubAttackAction */
    public ClubAttackAction(int entityId, int targetId, Mounted club) {
        super(entityId, targetId);
        this.club = club;
    }

    public ClubAttackAction(int entityId, int targetType, int targetId, Mounted club) {
        super(entityId, targetType, targetId);
        this.club = club;
    }
    
    /**
     * Damage that the specified mech does with a club attack
     */
    public static int getDamageFor(Entity entity, Mounted club) {

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
    
    public ToHitData toHit(Game game) {
        return toHit(game, getEntityId(),
                game.getTarget(getTargetType(), getTargetId()), getClub());
    }


    /**
     * To-hit number for the specified club to hit
     */
    public static ToHitData toHit(Game game, int attackerId, Targetable target, Mounted club) {
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
        final boolean targetInBuilding = Compute.isInBuilding( game, te );
        Building bldg = null;
        if ( targetInBuilding ) {
            bldg = game.board.getBuildingAt( te.getPosition() );
        }
        final int nightModifier = (game.getOptions().booleanOption("night_battle")) ? +2 : 0;
        
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
            if ( !Compute.isInBuilding(game, ae) ) {
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
        if ( !Compute.isInArc( ae.getPosition(), ae.getSecondaryFacing(),
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
        toHit.append(Compute.getAttackerMovementModifier(game, attackerId));

        // target movement
        toHit.append(Compute.getTargetMovementModifier(game, targetId));

        // attacker terrain
        toHit.append(Compute.getAttackerTerrainModifier(game, attackerId));

        // target terrain
        toHit.append(Compute.getTargetTerrainModifier(game, te));

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
        toHit.append(Compute.getImmobileMod(te));

        if (nightModifier>0) {
            toHit.addModifier(nightModifier, "Night Battle, no Spotlight");
        }

        Compute.modifyPhysicalBTHForAdvantages(ae, te, toHit, game);

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
        toHit.setSideTable(Compute.targetSideTable(ae,te));

        // done!
        return toHit;
    }
    public Mounted getClub() {
        return club;
    }
    
    public void setClub(Mounted club) {
        this.club = club;
    }
}
