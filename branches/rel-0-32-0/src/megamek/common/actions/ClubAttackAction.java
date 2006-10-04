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
public class ClubAttackAction extends PhysicalAttackAction {
    
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
        MiscType mType = (MiscType)(club.getType());
        int nDamage = (int)Math.floor(entity.getWeight() / 5.0);
        if (mType.hasSubType(MiscType.S_SWORD)) {
            nDamage = (int)(Math.ceil(entity.getWeight() / 10.0) + 1.0);
        } else if (mType.hasSubType(MiscType.S_MACE_THB)) {
            nDamage *= 2;
        } else if (mType.hasSubType(MiscType.S_MACE)) {
            nDamage = (int)Math.floor(entity.getWeight() / 4.0);
        } else if (mType.hasSubType(MiscType.S_PILE_DRIVER)) {
            // Pile Drivers have constant damage, not variable like most.
            nDamage = 10;
        } else if (mType.hasSubType(MiscType.S_FLAIL)) {
            // Flails have constant damage, not variable like most.
            nDamage = 9;
        } else if (mType.hasSubType(MiscType.S_DUAL_SAW)) {
            // Saws have constant damage, not variable like most.
            nDamage = 7;
        } else if (mType.hasSubType(MiscType.S_CHAINSAW)) {
            // Saws have constant damage, not variable like most.
            nDamage = 5;
        } else if (mType.hasSubType(MiscType.S_BACKHOE)) {
            // Backhoes have constant damage, not variable like most.
            nDamage = 6;
        } else if (mType.isShield()) {
            nDamage = club.getDamageAbsorption(entity,club.getLocation());
        } else if (mType.hasSubType(MiscType.S_WRECKING_BALL)) {
            // Wrecking Balls have constant damage, not variable like most.
            nDamage = 8;
        } else if (mType.hasSubType(MiscType.S_BUZZSAW)) {
            // buzzsaw does 2d6 damage, not weight dependant
            nDamage = Compute.d6(2);
        } else if ( mType.isVibroblade() ){
            if ( club.curMode().equals("Active") ){
                if ( mType.hasSubType(MiscType.S_VIBRO_LARGE) )
                    nDamage = 14;
                else if ( mType.hasSubType(MiscType.S_VIBRO_MEDIUM) )
                    nDamage = 10;
                else 
                    nDamage = 7;
            } else //when not active a vibro blade does normal sword damage
                 nDamage = (int)(Math.ceil(entity.getWeight() / 10.0) + 1.0);
        }

        // TSM doesn't apply to some weapons, including Saws.
        if (entity.heat >= 9
                && !(mType.hasSubType(MiscType.S_DUAL_SAW)
                || mType.hasSubType(MiscType.S_CHAINSAW)
                || mType.hasSubType(MiscType.S_PILE_DRIVER)
                || mType.isShield()
                || mType.hasSubType(MiscType.S_WRECKING_BALL)
                || mType.hasSubType(MiscType.S_FLAIL)
                || (mType.isVibroblade() && club.curMode().equals("Active"))
                || mType.hasSubType(MiscType.S_BUZZSAW))
                && ((Mech)entity).hasTSM()) {
            nDamage *= 2;
        }
        int clubLocation = club.getLocation();
        // tree clubs don't have a location--use right arm (is this okay?)
        if (clubLocation == Entity.LOC_NONE) {
            clubLocation = Mech.LOC_RARM;
        }
        if (entity.getLocationStatus(clubLocation) == ILocationExposureStatus.WET) {
            nDamage /= 2.0f;
        }

        return nDamage + entity.getCrew().modifyPhysicalDamagaForMeleeSpecialist();
    }

    public ToHitData toHit(IGame game) {
        return toHit(game, getEntityId(),
                game.getTarget(getTargetType(), getTargetId()), getClub());
    }


    /**
     * To-hit number for the specified club to hit
     */
    public static ToHitData toHit(IGame game, int attackerId, Targetable target, Mounted club) {
        final Entity ae = game.getEntity(attackerId);

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

        String impossible = toHitIsImpossible(game, ae, target);
        if (impossible != null) {
            return new ToHitData(ToHitData.IMPOSSIBLE, impossible);
        }

        // non-mechs can't club
        if (!(ae instanceof Mech)) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Non-mechs can't club");
        }

        //Quads can't club
        if (ae.entityIsQuad()) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Attacker is a quad");
        }

        IHex attHex = game.getBoard().getHex(ae.getPosition());
        IHex targHex = game.getBoard().getHex(target.getPosition());
        final int attackerElevation = ae.getElevation() + attHex.getElevation();
        final int attackerHeight = attackerElevation + ae.height();
        final int targetElevation = target.getElevation() + targHex.getElevation();
        final int targetHeight = targetElevation + target.getHeight();
        final boolean bothArms = (club.getType().hasFlag(MiscType.F_CLUB)
                && ((MiscType)club.getType()).hasSubType(MiscType.S_CLUB));
        final boolean hasClaws = ( ((BipedMech)ae).hasClaw(Mech.LOC_RARM) || ((BipedMech)ae).hasClaw(Mech.LOC_LARM) );
        final boolean shield = ((MiscType)club.getType()).isShield();
        boolean needsHand = true;
        
        if (hasClaws
                || (((MiscType)club.getType()).hasSubType(MiscType.S_FLAIL))
                || (((MiscType)club.getType()).hasSubType(MiscType.S_WRECKING_BALL))
                || (((MiscType)club.getType()).hasSubType(MiscType.S_LANCE))
                || (((MiscType)club.getType()).hasSubType(MiscType.S_BUZZSAW))
                || (((MiscType)club.getType()).hasSubType(MiscType.S_DUAL_SAW))) {
            needsHand = false;
        }
        
        ToHitData toHit;


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
            if ( (!ae.hasWorkingSystem(Mech.ACTUATOR_HAND, Mech.LOC_RARM)
                    || !ae.hasWorkingSystem(Mech.ACTUATOR_HAND, Mech.LOC_LARM))  && needsHand) {
                return new ToHitData(ToHitData.IMPOSSIBLE, "Hand actuator destroyed");
            }
        } else if (shield) {
            if (!ae.hasPassiveShield(club.getLocation())) {
                return new ToHitData(ToHitData.IMPOSSIBLE, "Shield not in passive mode");
            }
        } else if (((MiscType)club.getType()).hasSubType(MiscType.S_FLAIL)){
            if (!ae.hasWorkingSystem(Mech.ACTUATOR_UPPER_ARM, club.getLocation())) {
                return new ToHitData(ToHitData.IMPOSSIBLE, "Upper actuator destroyed");
            }
            if (!ae.hasWorkingSystem(Mech.ACTUATOR_LOWER_ARM, club.getLocation())) {
                return new ToHitData(ToHitData.IMPOSSIBLE, "Lower actuator destroyed");
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
            if (!ae.hasWorkingSystem(Mech.ACTUATOR_HAND, club.getLocation()) && needsHand) {
                return new ToHitData(ToHitData.IMPOSSIBLE, "Hand actuator destroyed");
            }
        }

        // club must not be damaged
        if (!shield && ae.getBadCriticals(CriticalSlot.TYPE_EQUIPMENT,
                ae.getEquipmentNum(club), club.getLocation()) > 0) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Club is damaged");
        }

        // check elevation (target must be within one level, except for VTOL)
        int targetMaxElevation = attackerHeight;
        if(target instanceof VTOL) {
            targetMaxElevation ++;
        }
        if (targetHeight < attackerElevation || targetElevation > targetMaxElevation) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Target elevation not in range");
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
        if (target.getTargetType() == Targetable.TYPE_BUILDING
                || target.getTargetType() == Targetable.TYPE_FUEL_TANK
                || target instanceof GunEmplacement) {
            return new ToHitData( ToHitData.AUTOMATIC_SUCCESS,
                                  "Targeting adjacent building." );
        }

        //Set the base BTH
        int base = 4;

        if ( game.getOptions().booleanOption("maxtech_physical_BTH") ) {
            base = ae.getCrew().getPiloting() - 1;
        }

        // Various versions of physical weapons have different base bonuses and penalties.
        if (((MiscType)club.getType()).hasSubType(MiscType.S_SWORD)
                || ((MiscType)club.getType()).isVibroblade()) {
            base -= 1;
        } else if ((((MiscType)club.getType()).hasSubType(MiscType.S_DUAL_SAW))
                || (((MiscType)club.getType()).hasSubType(MiscType.S_CHAINSAW))
                || (((MiscType)club.getType()).hasSubType(MiscType.S_FLAIL))) {
            base += 1;
        } else if ((((MiscType)club.getType()).hasSubType(MiscType.S_MACE_THB)) 
                || (((MiscType)club.getType()).hasSubType(MiscType.S_MACE))
                || (((MiscType)club.getType()).hasSubType(MiscType.S_BACKHOE))
                || (((MiscType)club.getType()).hasSubType(MiscType.S_LANCE))
                || (((MiscType)club.getType()).hasSubType(MiscType.S_BACKHOE))
                || (((MiscType)club.getType()).hasSubType(MiscType.S_WRECKING_BALL))) {
            base += 2;
        } else if (((MiscType)club.getType()).hasSubType(MiscType.S_PILE_DRIVER)) {
            base += 3;
        } else if (((MiscType)club.getType()).hasSubType(MiscType.S_SHIELD_LARGE)) {
            base -= 3;
        } else if (((MiscType)club.getType()).hasSubType(MiscType.S_SHIELD_MEDIUM)) {
            base -= 2;
        } else if (((MiscType)club.getType()).hasSubType(MiscType.S_SHIELD_SMALL)) {
            base -= 1;
        }


        toHit = new ToHitData(base, "base");

        setCommonModifiers(toHit, game, ae, target);

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
            if (hasClaws) {
                toHit.addModifier(2, "Mek has claws");
            }
        } else {
            if (!ae.hasWorkingSystem(Mech.ACTUATOR_UPPER_ARM, club.getLocation())) {
                toHit.addModifier(2, "Upper arm actuator destroyed");
                if ( (((MiscType)club.getType()).hasSubType(MiscType.S_LANCE)) )
                    return new ToHitData(ToHitData.IMPOSSIBLE, "Unable to use lance with upper arm actuator missing or destroyed");
            }
            if (!ae.hasWorkingSystem(Mech.ACTUATOR_LOWER_ARM, club.getLocation())) {
                toHit.addModifier(2, "Lower arm actuator missing or destroyed");
                if ( (((MiscType)club.getType()).hasSubType(MiscType.S_LANCE)) )
                    return new ToHitData(ToHitData.IMPOSSIBLE, "Unable to use lance with lower arm actuator missing or destroyed");
            }
            //Rules state +2 bth if your using a club with claws.
            if (hasClaws) {
                toHit.addModifier(2, "Mek has claws");
            }
            if ( (((MiscType)club.getType()).hasSubType(MiscType.S_LANCE))
                    && (!ae.hasWorkingSystem(Mech.ACTUATOR_LOWER_ARM, club.getLocation()) 
                            || !ae.hasWorkingSystem(Mech.ACTUATOR_UPPER_ARM, club.getLocation()))){
            }
        }

        // elevation
        if (attackerElevation == targetElevation) {
            if (shield)
                toHit.setHitTable(ToHitData.HIT_PUNCH);
            else
                toHit.setHitTable(ToHitData.HIT_NORMAL);
        } else if (attackerElevation < targetElevation) {
            if (target.getHeight() == 0) {
                if (shield )
                    toHit.setHitTable(ToHitData.HIT_PUNCH);
                else
                    toHit.setHitTable(ToHitData.HIT_NORMAL);
            } else {
                toHit.setHitTable(ToHitData.HIT_KICK);
            }
        } else {
            toHit.setHitTable(ToHitData.HIT_PUNCH);
        }

        // factor in target side
        toHit.setSideTable(Compute.targetSideTable(ae,target));

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
