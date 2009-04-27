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

import megamek.common.BipedMech;
import megamek.common.Compute;
import megamek.common.CriticalSlot;
import megamek.common.Entity;
import megamek.common.GunEmplacement;
import megamek.common.IGame;
import megamek.common.IHex;
import megamek.common.ILocationExposureStatus;
import megamek.common.Mech;
import megamek.common.MiscType;
import megamek.common.Mounted;
import megamek.common.TargetRoll;
import megamek.common.Targetable;
import megamek.common.ToHitData;
import megamek.common.VTOL;

/**
 * The attacker makes a club attack on the target. This also covers mech melee
 * weapons like hatchets.
 *
 * @author Ben
 * @version
 */
public class ClubAttackAction extends PhysicalAttackAction {

    /**
     *
     */
    private static final long serialVersionUID = -8744665286254604559L;
    private Mounted club;
    private int aiming;

    /** Creates new ClubAttackAction */
    public ClubAttackAction(int entityId, int targetId, Mounted club,
            int aimTable) {
        super(entityId, targetId);
        this.club = club;
        aiming = aimTable;
    }

    public ClubAttackAction(int entityId, int targetType, int targetId,
            Mounted club, int aimTable) {
        super(entityId, targetType, targetId);
        this.club = club;
        aiming = aimTable;
    }

    /**
     * Damage that the specified mech does with a club attack
     */
    public static int getDamageFor(Entity entity, Mounted club,
            boolean targetInfantry) {
        MiscType mType = (MiscType) (club.getType());
        int nDamage = (int) Math.floor(entity.getWeight() / 5.0);
        if (mType.hasSubType(MiscType.S_SWORD)) {
            nDamage = (int) (Math.ceil(entity.getWeight() / 10.0) + 1.0);
        } else if (mType.hasSubType(MiscType.S_MACE_THB)) {
            nDamage *= 2;
        } else if (mType.hasSubType(MiscType.S_RETRACTABLE_BLADE)) {
            nDamage = (int) Math.ceil(entity.getWeight() / 10.0);
        } else if (mType.hasSubType(MiscType.S_MACE)) {
            nDamage = (int) Math.floor(entity.getWeight() / 4.0);
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
            nDamage = club.getDamageAbsorption(entity, club.getLocation());
        } else if (mType.hasSubType(MiscType.S_WRECKING_BALL)) {
            // Wrecking Balls have constant damage, not variable like most.
            nDamage = 8;
        } else if (mType.hasSubType(MiscType.S_BUZZSAW)) {
            // buzzsaw does 2d6 damage, not weight dependant
            nDamage = Compute.d6(2);
        } else if (mType.isVibroblade()) {
            if (club.curMode().equals("Active")) {
                if (mType.hasSubType(MiscType.S_VIBRO_LARGE)) {
                    nDamage = 14;
                } else if (mType.hasSubType(MiscType.S_VIBRO_MEDIUM)) {
                    nDamage = 10;
                } else {
                    nDamage = 7;
                }
            } else {
                // when not active a vibro blade does normal sword damage
                nDamage = (int) (Math.ceil(entity.getWeight() / 10.0) + 1.0);
            }
        }else if (mType.hasSubType(MiscType.S_CHAIN_WHIP)){
            nDamage = 3;
        } else if (mType.hasSubType(MiscType.S_ROCK_CUTTER)) {
            nDamage = 5;
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
                        || mType.hasSubType(MiscType.S_BUZZSAW)
                        || mType.hasSubType(MiscType.S_CHAIN_WHIP))
                && ((Mech) entity).hasTSM()) {
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
        if (targetInfantry) {
            nDamage = Math.max(1, nDamage / 10);
        }

        return nDamage
                + entity.getCrew().modifyPhysicalDamagaForMeleeSpecialist();
    }

    public ToHitData toHit(IGame game) {
        return ClubAttackAction.toHit(game, getEntityId(), game.getTarget(getTargetType(),
                getTargetId()), getClub(), aiming);
    }

    /**
     * To-hit number for the specified club to hit
     */
    public static ToHitData toHit(IGame game, int attackerId,
            Targetable target, Mounted club, int aimTable) {
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

        String impossible = PhysicalAttackAction.toHitIsImpossible(game, ae, target);
        if (impossible != null) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, impossible);
        }

        // non-mechs can't club
        if (!(ae instanceof Mech)) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "Non-mechs can't club");
        }

        // Quads can't club
        if (ae.entityIsQuad()) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "Attacker is a quad");
        }

        if (((MiscType) club.getType())
                .hasSubType(MiscType.S_RETRACTABLE_BLADE)
                && !((Mech) ae).hasExtendedRetractableBlade()) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "Blade is Retracted.");
        }

        if ( ae.getGrappled() != Entity.NONE &&
                ae.getGrappleSide() == Entity.GRAPPLE_LEFT
                && club.getLocation() == Mech.LOC_LARM ) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "impossible");
        }

        if ( ae.getGrappled() != Entity.NONE &&
                ae.getGrappleSide() == Entity.GRAPPLE_RIGHT
                && club.getLocation() == Mech.LOC_RARM ) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "impossible");
        }


        IHex attHex = game.getBoard().getHex(ae.getPosition());
        IHex targHex = game.getBoard().getHex(target.getPosition());
        final int attackerElevation = ae.getElevation() + attHex.getElevation();
        final int attackerHeight = attackerElevation + ae.height();
        final int targetElevation = target.getElevation()
                + targHex.getElevation();
        final int targetHeight = targetElevation + target.getHeight();
        final boolean bothArms = (club.getType().hasFlag(MiscType.F_CLUB)
                && ((MiscType) club.getType()).hasSubType(MiscType.S_CLUB));
        final boolean hasClaws = (((BipedMech) ae).hasClaw(Mech.LOC_RARM)
                || ((BipedMech) ae).hasClaw(Mech.LOC_LARM));
        final boolean shield = ((MiscType) club.getType()).isShield();
        boolean needsHand = true;

        if (hasClaws
                || (((MiscType) club.getType()).hasSubType(MiscType.S_FLAIL))
                || (((MiscType) club.getType()).hasSubType(MiscType.S_WRECKING_BALL))
                || (((MiscType) club.getType()).hasSubType(MiscType.S_LANCE))
                || (((MiscType) club.getType()).hasSubType(MiscType.S_BUZZSAW))
                || (((MiscType) club.getType()).hasSubType(MiscType.S_DUAL_SAW))) {
            needsHand = false;
        }

        ToHitData toHit;

        if (bothArms) {
            // check if both arms are present & operational
            if (ae.isLocationBad(Mech.LOC_RARM)
                    || ae.isLocationBad(Mech.LOC_LARM)) {
                return new ToHitData(TargetRoll.IMPOSSIBLE, "Arm missing");
            }
            // check if attacker has fired arm-mounted weapons
            if (ae.weaponFiredFrom(Mech.LOC_RARM)
                    || ae.weaponFiredFrom(Mech.LOC_LARM)) {
                return new ToHitData(TargetRoll.IMPOSSIBLE,
                        "Weapons fired from arm this turn");
            }
            // need shoulder and hand actuators
            if (!ae.hasWorkingSystem(Mech.ACTUATOR_SHOULDER, Mech.LOC_RARM)
                    || !ae.hasWorkingSystem(Mech.ACTUATOR_SHOULDER,
                            Mech.LOC_LARM)) {
                return new ToHitData(TargetRoll.IMPOSSIBLE,
                        "Shoulder actuator destroyed");
            }
            if ((!ae.hasWorkingSystem(Mech.ACTUATOR_HAND, Mech.LOC_RARM) || !ae
                    .hasWorkingSystem(Mech.ACTUATOR_HAND, Mech.LOC_LARM))
                    && needsHand) {
                return new ToHitData(TargetRoll.IMPOSSIBLE,
                        "Hand actuator destroyed");
            }
        } else if (shield) {
            if (!ae.hasPassiveShield(club.getLocation())) {
                return new ToHitData(TargetRoll.IMPOSSIBLE,
                        "Shield not in passive mode");
            }
        } else if (((MiscType) club.getType()).hasSubType(MiscType.S_FLAIL)) {
            if (!ae.hasWorkingSystem(Mech.ACTUATOR_UPPER_ARM, club
                    .getLocation())) {
                return new ToHitData(TargetRoll.IMPOSSIBLE,
                        "Upper actuator destroyed");
            }
            if (!ae.hasWorkingSystem(Mech.ACTUATOR_LOWER_ARM, club
                    .getLocation())) {
                return new ToHitData(TargetRoll.IMPOSSIBLE,
                        "Lower actuator destroyed");
            }
        } else {
            // check if arm is present
            if (ae.isLocationBad(club.getLocation())) {
                return new ToHitData(TargetRoll.IMPOSSIBLE, "Arm missing");
            }
            // check if attacker has fired arm-mounted weapons
            if (ae.weaponFiredFrom(club.getLocation())) {
                return new ToHitData(TargetRoll.IMPOSSIBLE,
                        "Weapons fired from arm this turn");
            }
            // need shoulder and hand actuators
            if (!ae
                    .hasWorkingSystem(Mech.ACTUATOR_SHOULDER, club
                            .getLocation())) {
                return new ToHitData(TargetRoll.IMPOSSIBLE,
                        "Shoulder actuator destroyed");
            }
            if (!ae.hasWorkingSystem(Mech.ACTUATOR_HAND, club.getLocation())
                    && needsHand) {
                return new ToHitData(TargetRoll.IMPOSSIBLE,
                        "Hand actuator destroyed");
            }
        }

        // club must not be damaged
        if (!shield
                && ae.getBadCriticals(CriticalSlot.TYPE_EQUIPMENT, ae
                        .getEquipmentNum(club), club.getLocation()) > 0) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "Club is damaged");
        }

        // check elevation (target must be within one level, except for VTOL)
        if (target instanceof VTOL && ((VTOL)target).isFlying()) {
            if (targetElevation - attackerElevation > 3 || targetElevation - attackerElevation < 0) {
                return new ToHitData(TargetRoll.IMPOSSIBLE, "Target elevation not in range");
            }
        } else if (targetHeight < attackerElevation
                || targetElevation > attackerHeight) {
            return new ToHitData(TargetRoll.IMPOSSIBLE,
                    "Target elevation not in range");
        }

        // check facing
        int clubArc = bothArms ? Compute.ARC_FORWARD
                : (club.getLocation() == Mech.LOC_LARM ? Compute.ARC_LEFTARM
                        : Compute.ARC_RIGHTARM);
        if (!Compute.isInArc(ae.getPosition(), ae.getSecondaryFacing(), target
                .getPosition(), clubArc)) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "Target not in arc");
        }

        // can't club while prone
        if (ae.isProne()) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "Attacker is prone");
        }

        // Attacks against adjacent buildings automatically hit.
        if (target.getTargetType() == Targetable.TYPE_BUILDING
                || target.getTargetType() == Targetable.TYPE_FUEL_TANK
                || target instanceof GunEmplacement) {
            return new ToHitData(TargetRoll.AUTOMATIC_SUCCESS,
                    "Targeting adjacent building.");
        }

        // Set the base BTH
        int base = ae.getCrew().getPiloting();

        // Various versions of physical weapons have different base bonuses and
        // penalties.
        if (((MiscType) club.getType()).hasSubType(MiscType.S_PILE_DRIVER)) {
            base += 2;
        } else if (((MiscType) club.getType()).hasSubType(MiscType.S_BACKHOE)
                || ((MiscType) club.getType()).hasSubType(MiscType.S_ROCK_CUTTER)
                || ((MiscType) club.getType()).hasSubType(MiscType.S_WRECKING_BALL)
                || ((MiscType) club.getType()).hasSubType(MiscType.S_LANCE)
                || ((MiscType) club.getType()).hasSubType(MiscType.S_FLAIL)
                || ((MiscType) club.getType()).hasSubType(MiscType.S_MACE)
                || ((MiscType) club.getType()).hasSubType(MiscType.S_MACE_THB)) {
            base += 1;
        } else if (((MiscType) club.getType()).hasSubType(MiscType.S_CHAINSAW)
                || ((MiscType) club.getType()).hasSubType(MiscType.S_DUAL_SAW)) {
            base += 0;
        } else if (((MiscType) club.getType()).hasSubType(MiscType.S_HATCHET)
                || ((MiscType) club.getType()).hasSubType(MiscType.S_MINING_DRILL)) {
            base -= 1;
        } else if (((MiscType) club.getType()).hasSubType(MiscType.S_COMBINE)
                || ((MiscType) club.getType()).hasSubType(MiscType.S_RETRACTABLE_BLADE)
                || ((MiscType) club.getType()).hasSubType(MiscType.S_SWORD)
                || ((MiscType) club.getType()).hasSubType(MiscType.S_CHAIN_WHIP)
                || ((MiscType) club.getType()).hasSubType(MiscType.S_SHIELD_SMALL)
                || ((MiscType) club.getType()).isVibroblade()) {
            base -= 2;
        } else if (((MiscType) club.getType()).hasSubType(MiscType.S_SHIELD_MEDIUM)) {
            base -= 3;
        } else if (((MiscType) club.getType()).hasSubType(MiscType.S_SHIELD_LARGE)) {
            base -= 4;
        } else {
            base -= 1;
        }

        toHit = new ToHitData(base, "base");

        PhysicalAttackAction.setCommonModifiers(toHit, game, ae, target);

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
            if ( ae.hasFunctionalArmAES(Mech.LOC_RARM) && ae.hasFunctionalArmAES(Mech.LOC_LARM) ) {
                toHit.addModifier(-1,"AES modifer");
            }
        } else {
            if (!ae.hasWorkingSystem(Mech.ACTUATOR_UPPER_ARM, club
                    .getLocation())) {
                toHit.addModifier(2, "Upper arm actuator destroyed");
                if ((((MiscType) club.getType()).hasSubType(MiscType.S_LANCE))) {
                    return new ToHitData(TargetRoll.IMPOSSIBLE,
                            "Unable to use lance with upper arm actuator missing or destroyed");
                }
            }
            if (!ae.hasWorkingSystem(Mech.ACTUATOR_LOWER_ARM, club
                    .getLocation())) {
                toHit.addModifier(2, "Lower arm actuator missing or destroyed");
                if ((((MiscType) club.getType()).hasSubType(MiscType.S_LANCE))) {
                    return new ToHitData(TargetRoll.IMPOSSIBLE,
                            "Unable to use lance with lower arm actuator missing or destroyed");
                }
            }
            // Rules state +2 bth if your using a club with claws.
            if (hasClaws) {
                toHit.addModifier(2, "Mek has claws");
            }
            if ((((MiscType) club.getType()).hasSubType(MiscType.S_LANCE))
                    && (!ae.hasWorkingSystem(Mech.ACTUATOR_LOWER_ARM, club
                            .getLocation()) || !ae.hasWorkingSystem(
                            Mech.ACTUATOR_UPPER_ARM, club.getLocation()))) {
            }
            if ( ae.hasFunctionalArmAES(club.getLocation()) ) {
                toHit.addModifier(-1,"AES modifer");
            }
        }

        // elevation
        if (attackerElevation == targetElevation) {
            if (shield) {
                toHit.setHitTable(ToHitData.HIT_PUNCH);
            } else {
                toHit.setHitTable(aimTable);
                if (aimTable != ToHitData.HIT_NORMAL) {
                    toHit.addModifier(4, "called shot");
                }
            }
        } else if (attackerElevation < targetElevation) {
            if (target.getHeight() == 0) {
                if (shield) {
                    toHit.setHitTable(ToHitData.HIT_PUNCH);
                } else {
                    toHit.setHitTable(ToHitData.HIT_NORMAL);
                }
            } else {
                toHit.setHitTable(ToHitData.HIT_KICK);
            }
        } else {
            toHit.setHitTable(ToHitData.HIT_PUNCH);
        }

        // factor in target side
        toHit.setSideTable(Compute.targetSideTable(ae, target));

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
