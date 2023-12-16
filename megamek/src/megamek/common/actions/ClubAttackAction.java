/*
 * MegaMek - Copyright (C) 2001-2004 Ben Mazur (bmazur@sev.org)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 */
package megamek.common.actions;

import megamek.client.ui.Messages;
import megamek.common.Compute;
import megamek.common.CriticalSlot;
import megamek.common.Entity;
import megamek.common.GunEmplacement;
import megamek.common.Game;
import megamek.common.Hex;
import megamek.common.ILocationExposureStatus;
import megamek.common.Mech;
import megamek.common.MiscType;
import megamek.common.Mounted;
import megamek.common.TargetRoll;
import megamek.common.Targetable;
import megamek.common.ToHitData;
import megamek.common.options.OptionsConstants;

/**
 * The attacker makes a club attack on the target. This also covers mech melee
 * weapons like hatchets.
 *
 * @author Ben
 * @since April 3, 2002, 2:37 PM
 */
public class ClubAttackAction extends PhysicalAttackAction {
    private static final long serialVersionUID = -8744665286254604559L;
    private Mounted club;
    private int aiming;
    private boolean zweihandering;

    /**
     * Creates new ClubAttackAction
     */
    public ClubAttackAction(int entityId, int targetId, Mounted club,
                            int aimTable) {
        super(entityId, targetId);
        this.club = club;
        aiming = aimTable;
    }

    /**
     * Creates a new club attack
     * @param entityId - id of entity performing the attack
     * @param targetType - type of target
     * @param targetId - id of target
     * @param club - The <code>Mounted</code> of the weapon doing the attack
     * @param aimTable
     * @param zweihandering - a boolean indicating whether the attacker is zweihandering (using both hands)
     */
    public ClubAttackAction(int entityId, int targetType, int targetId,
                            Mounted club, int aimTable, boolean zweihandering) {
        super(entityId, targetType, targetId);
        this.club = club;
        aiming = aimTable;
        this.zweihandering = zweihandering;

    }

    /**
     * Damage for the club attack
     * @param entity - the entity performing the attack
     * @param club - The <code>Mounted</code> of the weapon doing the attack
     * @param targetInfantry - whether this attack targets infantry
     * @param zweihandering - a boolean indicating whether the attacker is zweihandering (using both hands)
     * @return an integer of the damage dealt
     */
    public static int getDamageFor(Entity entity, Mounted club,
            boolean targetInfantry, boolean zweihandering) {
        MiscType mType = (MiscType) (club.getType());
        int nDamage = (int) Math.floor(entity.getWeight() / 5.0);
        if (mType.hasSubType(MiscType.S_SWORD)) {
            nDamage = (int) (Math.ceil(entity.getWeight() / 10.0) + 1.0);
        } else if (mType.hasSubType(MiscType.S_RETRACTABLE_BLADE)) {
            nDamage = (int) Math.ceil(entity.getWeight() / 10.0);
        } else if (mType.hasSubType(MiscType.S_MACE)) {
            nDamage = (int) Math.ceil(entity.getWeight() / 4.0);
        } else if (mType.hasSubType(MiscType.S_PILE_DRIVER)) {
            // Pile Drivers have constant damage, not variable like most.
            nDamage = 10;
        } else if (mType.hasSubType(MiscType.S_FLAIL)) {
            // Flails have constant damage, not variable like most.
            nDamage = 9;
        } else if (mType.hasSubType(MiscType.S_DUAL_SAW)) {
            if (targetInfantry) {
                nDamage = Compute.d6();
            } else {
                // Saws have constant damage, not variable like most.
                nDamage = 7;
            }
        } else if (mType.hasSubType(MiscType.S_CHAINSAW)) {
            if (targetInfantry) {
                nDamage = Compute.d6();
            } else {
                // Saws have constant damage, not variable like most.
                nDamage = 5;
            }
        } else if (mType.hasSubType(MiscType.S_BACKHOE)) {
            // Backhoes have constant damage, not variable like most.
            nDamage = 6;
        } else if (mType.hasSubType(MiscType.S_MINING_DRILL)) {
            // Mining drills have constant damage, not variable like most.
            nDamage = 4;
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
        } else if (mType.hasSubType(MiscType.S_CHAIN_WHIP)) {
            nDamage = 3;
        } else if (mType.hasSubType(MiscType.S_COMBINE)) {
            if (targetInfantry) {
                nDamage = Compute.d6();
            } else {
                nDamage = 3;
            }
        } else if (mType.hasSubType(MiscType.S_ROCK_CUTTER)) {
            nDamage = 5;
        } else if (mType.hasSubType(MiscType.S_SPOT_WELDER)) {
            nDamage = 5;
        }

        //SMASH! CamOps, pg. 82
        if (zweihandering) {
            nDamage += (int) Math.floor(entity.getWeight() / 10.0);
        }

        // TSM doesn't apply to some weapons, including Saws.
        if ((entity instanceof Mech) && ((Mech) entity).hasActiveTSM()
            && !(mType.hasSubType(MiscType.S_DUAL_SAW)
                 || mType.hasSubType(MiscType.S_CHAINSAW)
                 || mType.hasSubType(MiscType.S_PILE_DRIVER)
                 || mType.isShield()
                 || mType.hasSubType(MiscType.S_WRECKING_BALL)
                 || mType.hasSubType(MiscType.S_FLAIL)
                 || (mType.isVibroblade() && club.curMode().equals(
                "Active"))
                 || mType.hasSubType(MiscType.S_BUZZSAW)
                 || mType.hasSubType(MiscType.S_MINING_DRILL)
                 || mType.hasSubType(MiscType.S_ROCK_CUTTER)
                 || mType.hasSubType(MiscType.S_SPOT_WELDER)
                 || mType.hasSubType(MiscType.S_CHAIN_WHIP) || mType
                .hasSubType(MiscType.S_COMBINE))) {
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
               + entity.modifyPhysicalDamageForMeleeSpecialist();
    }

    /**
     * Modifiers to the to-hit roll for specific weapons
     *
     * @param clubType A physical weapon
     * @return         The modifier to hit with the weapon
     */
    public static int getHitModFor(MiscType clubType) {
        if (clubType.hasSubType(MiscType.S_PILE_DRIVER)) {
            return 2;
        } else if (clubType.hasSubType(MiscType.S_BACKHOE)
                || clubType.hasSubType(MiscType.S_ROCK_CUTTER)
                || clubType.hasSubType(MiscType.S_WRECKING_BALL)
                || clubType.hasSubType(MiscType.S_LANCE)
                || clubType.hasSubType(MiscType.S_MACE)) {
            return 1;
        } else if (clubType.hasSubType(MiscType.S_CHAINSAW)
                || clubType.hasSubType(MiscType.S_DUAL_SAW)
                || clubType.hasSubType(MiscType.S_FLAIL)) {
            return 0;
        } else if (clubType.hasSubType(MiscType.S_HATCHET)
                || clubType.hasSubType(MiscType.S_MINING_DRILL)) {
            return -1;
        } else if (clubType.hasSubType(MiscType.S_COMBINE)
                || clubType.hasSubType(MiscType.S_RETRACTABLE_BLADE)
                || clubType.hasSubType(MiscType.S_SWORD)
                || clubType.hasSubType(MiscType.S_CHAIN_WHIP)
                || clubType.hasSubType(MiscType.S_SHIELD_SMALL)
                || clubType.isVibroblade()
                || clubType.hasSubType(MiscType.S_COMBINE)) {
            return -2;
        } else if (clubType.hasSubType(MiscType.S_SHIELD_MEDIUM)) {
            return -3;
        } else if (clubType.hasSubType(MiscType.S_SHIELD_LARGE)) {
            return -4;
        } else {
            return -1;
        }
    }

    /**
     *
     * @return true if the entity is zweihandering (attacking with both hands)
     */
    public boolean isZweihandering() {
        return zweihandering;
    }

    public ToHitData toHit(Game game) {
        return ClubAttackAction.toHit(game, getEntityId(),
                                      game.getTarget(getTargetType(), getTargetId()), getClub(),
                                      aiming, zweihandering);
    }

    /**
     * To-hit number for the specified club to hit
     * @param game The current {@link Game}
     * @param attackerId - attacker id
     * @param target <code>Targetable</code> of the target
     * @param club - <code>Mounted</code> of the weapon
     * @param aimTable
     * @param zweihandering - a boolean indicating whether the attacker is zweihandering (using both hands)
     * @return
     */
    public static ToHitData toHit(Game game, int attackerId,
                                  Targetable target, Mounted club, int aimTable, boolean zweihandering) {
        final Entity ae = game.getEntity(attackerId);
        MiscType clubType;
        // arguments legal?
        if ((ae == null) || (target == null)) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "Attacker or target not valid");
        }
        if (club == null) {
            throw new IllegalArgumentException("Club is null");
        }
        if (club.getType() == null) {
            throw new IllegalArgumentException("Club type is null");
        } else {
            clubType = (MiscType) club.getType();
        }

        String impossible = PhysicalAttackAction.toHitIsImpossible(game, ae,
                                                                   target);
        if (impossible != null) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, impossible);
        }

        // non-mechs can't club
        if (!(ae instanceof Mech)) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "Non-mechs can't club");
        }

        // Quads can't club...
        // except for torso mounted industrial tools of course!
        if (ae.entityIsQuad()
            && !(clubType.hasSubType(MiscType.S_BACKHOE))
            && !(clubType
                .hasSubType(MiscType.S_WRECKING_BALL))
            // && !(clubType.hasSubType(MiscType.S_LANCE))
            // Not sure if Lance can be used on a quad, comment out for now.
            && !(clubType.hasSubType(MiscType.S_BUZZSAW))
            && !(clubType.hasSubType(MiscType.S_DUAL_SAW))
            && !(clubType.hasSubType(MiscType.S_COMBINE))
            && !(clubType.hasSubType(MiscType.S_CHAINSAW))) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "Attacker is a quad");
        }

        if (clubType.hasSubType(MiscType.S_RETRACTABLE_BLADE)
            && !((Mech) ae).hasExtendedRetractableBlade()) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "Blade is Retracted.");
        }

        if ((ae.getGrappled() != Entity.NONE)
            && (ae.getGrappleSide() == Entity.GRAPPLE_LEFT)
            && (club.getLocation() == Mech.LOC_LARM)) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "impossible");
        }

        if ((ae.getGrappled() != Entity.NONE)
            && (ae.getGrappleSide() == Entity.GRAPPLE_RIGHT)
            && (club.getLocation() == Mech.LOC_RARM)) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "impossible");
        }

        Hex attHex = game.getBoard().getHex(ae.getPosition());
        Hex targHex = game.getBoard().getHex(target.getPosition());
        final int attackerElevation = ae.getElevation() + attHex.getLevel();
        final int attackerHeight = attackerElevation + ae.height();
        final int targetElevation = target.getElevation()
                                    + targHex.getLevel();
        final int targetHeight = targetElevation + target.getHeight();
        final boolean bothArms = (club.getType().hasFlag(MiscType.F_CLUB)
                                  && ((MiscType) club.getType()).hasSubType(MiscType.S_CLUB))
                    || zweihandering;
        // Cast is safe because non-'Mechs never even get here.
        final boolean hasClaws = ((Mech) ae).hasClaw(Mech.LOC_RARM)
                                 || ((Mech) ae).hasClaw(Mech.LOC_LARM);
        final boolean shield = clubType.isShield();
        boolean needsHand = true;
        final boolean armMounted = (club.getLocation() == Mech.LOC_LARM
                                    || club.getLocation() == Mech.LOC_RARM);

        if (hasClaws
            || (clubType.hasSubType(MiscType.S_BACKHOE))
            || (clubType.hasSubType(MiscType.S_BUZZSAW))
            || (clubType.hasSubType(MiscType.S_CHAINSAW))
            || (clubType.hasSubType(MiscType.S_COMBINE))
            || (clubType.hasSubType(MiscType.S_DUAL_SAW))
            || (clubType.hasSubType(MiscType.S_FLAIL))
            || (clubType.hasSubType(MiscType.S_LANCE))
            || (clubType.hasSubType(MiscType.S_MINING_DRILL))
            || (clubType.hasSubType(MiscType.S_PILE_DRIVER))
            || (clubType.hasSubType(MiscType.S_ROCK_CUTTER))
            || (clubType.hasSubType(MiscType.S_SPOT_WELDER))
            || (clubType
                .hasSubType(MiscType.S_WRECKING_BALL))) {
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
        } else {
            // check if location is present
            if (ae.isLocationBad(club.getLocation())) {
                return new ToHitData(TargetRoll.IMPOSSIBLE, "Arm missing");
            }
            // check if attacker has fired arm-mounted weapons
            if (ae.weaponFiredFrom(club.getLocation()) && armMounted) {
                return new ToHitData(TargetRoll.IMPOSSIBLE,
                                     "Weapons fired from arm this turn");
            }
            // need shoulder and hand actuators
            if (armMounted
                && !ae.hasWorkingSystem(Mech.ACTUATOR_SHOULDER,
                                        club.getLocation())) {
                return new ToHitData(TargetRoll.IMPOSSIBLE,
                                     "Shoulder actuator destroyed");
            }
            if (armMounted
                && !ae.hasWorkingSystem(Mech.ACTUATOR_HAND,
                                        club.getLocation()) && needsHand) {
                return new ToHitData(TargetRoll.IMPOSSIBLE,
                                     "Hand actuator destroyed");
            }
        }

        // check for no/minimal arms quirk
        if (armMounted && ae.hasQuirk(OptionsConstants.QUIRK_NEG_NO_ARMS)) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "No/minimal arms");
        }

        // club must not be damaged
        if (!shield
            && (ae.getBadCriticals(CriticalSlot.TYPE_EQUIPMENT,
                                   ae.getEquipmentNum(club), club.getLocation()) > 0)) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "Club is damaged");
        }

        // check elevation (target must be within one level, except for VTOL)
        if (target.isAirborneVTOLorWIGE()) {
            if (((targetElevation - attackerElevation) > 3)
                || ((targetElevation - attackerElevation) < 0)) {
                return new ToHitData(TargetRoll.IMPOSSIBLE,
                                     "Target elevation not in range");
            }
        } else if ((targetHeight < attackerElevation)
                   || (targetElevation > attackerHeight)) {
            return new ToHitData(TargetRoll.IMPOSSIBLE,
                                 "Target elevation not in range");
        }

        // check facing
        int clubArc;
        if (bothArms) {
            clubArc = Compute.ARC_FORWARD;
        } else {
            if (club.getLocation() == Mech.LOC_LARM) {
                clubArc = Compute.ARC_LEFTARM;
            } else if (armMounted) {
                clubArc = Compute.ARC_RIGHTARM;
            } else if (club.isRearMounted()) {
                clubArc = Compute.ARC_REAR;
            } else {
                clubArc = Compute.ARC_FORWARD;
            }
        }
        if (!Compute.isInArc(ae.getPosition(), ae.getSecondaryFacing(), target,
                             clubArc)) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "Target not in arc");
        }

        // can't club while prone
        if (ae.isProne()) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "Attacker is prone");
        }

        // Attacks against adjacent buildings automatically hit.
        if ((target.getTargetType() == Targetable.TYPE_BUILDING)
            || (target.getTargetType() == Targetable.TYPE_FUEL_TANK)
            || (target instanceof GunEmplacement)) {
            return new ToHitData(TargetRoll.AUTOMATIC_SUCCESS,
                                 "Targeting adjacent building.");
        }

        // Set the base BTH
        int base = ae.getCrew().getPiloting();

        // Various versions of physical weapons have different base bonuses and
        // penalties.
        base += getHitModFor(clubType);
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
            if (ae.hasFunctionalArmAES(Mech.LOC_RARM)
                && ae.hasFunctionalArmAES(Mech.LOC_LARM)) {
                toHit.addModifier(-1, "AES modifer");
            }
        } else {
            if (armMounted && !ae.hasWorkingSystem(Mech.ACTUATOR_UPPER_ARM,
                                                   club.getLocation())) {
                toHit.addModifier(2, "Upper arm actuator destroyed");
            }
            if (armMounted && !ae.hasWorkingSystem(Mech.ACTUATOR_LOWER_ARM,
                                                   club.getLocation())) {
                toHit.addModifier(2, "Lower arm actuator missing or destroyed");
            }
            // Rules state +2 bth if your using a club with claws.
            if (hasClaws
                && (clubType.hasSubType(MiscType.S_CLUB))) {
                toHit.addModifier(2, "Mek has claws");
            }
            if (ae.hasFunctionalArmAES(club.getLocation())) {
                toHit.addModifier(-1, "AES modifer");
            }
        }

        // elevation
        if (game.getOptions().booleanOption(OptionsConstants.ADVCOMBAT_CLUBS_PUNCH)
            && (target instanceof Mech)) {
            toHit.setHitTable(ToHitData.HIT_PUNCH);
            if ((attackerHeight == targetElevation) && !ae.isHullDown()) {
                if (target.getHeight() == 0) {
                    toHit.setHitTable(ToHitData.HIT_NORMAL);
                } else {
                    toHit.setHitTable(ToHitData.HIT_KICK);
                }
            } else {
                if (ae.isHullDown()) {
                    toHit.setHitTable(ToHitData.HIT_KICK);
                } else {
                    toHit.setHitTable(ToHitData.HIT_PUNCH);
                }
            }
        } else {
            if (attackerElevation == targetElevation) {
                toHit.setHitTable(aimTable);
                if (aimTable != ToHitData.HIT_NORMAL) {
                    toHit.addModifier(4, "called shot");
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

    @Override
    public String toSummaryString(final Game game) {
        final String roll = this.toHit(game).getValueAsString();
        final String club = this.getClub().getName();
        return Messages.getString("BoardView1.ClubAttackAction", club, roll);
    }
}
