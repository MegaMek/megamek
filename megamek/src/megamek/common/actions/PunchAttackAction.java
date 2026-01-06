/*
 * Copyright (C) 2000-2004 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2002-2025 The MegaMek Team. All Rights Reserved.
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

package megamek.common.actions;

import java.io.Serial;

import megamek.client.ui.Messages;
import megamek.common.CriticalSlot;
import megamek.common.Hex;
import megamek.common.ToHitData;
import megamek.common.compute.Compute;
import megamek.common.compute.ComputeArc;
import megamek.common.compute.ComputeSideTable;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.MiscMounted;
import megamek.common.equipment.MiscType;
import megamek.common.equipment.Mounted;
import megamek.common.equipment.enums.MiscTypeFlag;
import megamek.common.game.Game;
import megamek.common.interfaces.ILocationExposureStatus;
import megamek.common.options.OptionsConstants;
import megamek.common.rolls.TargetRoll;
import megamek.common.units.Dropship;
import megamek.common.units.Entity;
import megamek.common.units.Mek;
import megamek.common.units.Tank;
import megamek.common.units.Targetable;
import megamek.logging.MMLogger;

/**
 * The attacker punches the target.
 */
public class PunchAttackAction extends PhysicalAttackAction {
    private static final MMLogger logger = MMLogger.create(PunchAttackAction.class);

    @Serial
    private static final long serialVersionUID = 3684646558944678180L;
    public static final int BOTH = 0;
    public static final int LEFT = 1;
    public static final int RIGHT = 2;

    private int arm;
    // booleans for retractable blade extension
    private boolean leftBlade = false;
    private boolean rightBlade = false;
    private boolean zweihandering = false;

    public PunchAttackAction(int entityId, int targetId, int arm) {
        super(entityId, targetId);
        this.arm = arm;
    }

    /**
     * Punch attack vs an entity or other type of target (e.g. building)
     */
    public PunchAttackAction(int entityId, int targetType, int targetId, int arm) {
        super(entityId, targetType, targetId);
        this.arm = arm;
    }

    public PunchAttackAction(int entityId, int targetType, int targetId, int arm, boolean leftBlade,
          boolean rightBlade, boolean zweihandering) {
        super(entityId, targetType, targetId);
        this.arm = arm;
        this.leftBlade = leftBlade;
        this.rightBlade = rightBlade;
        this.zweihandering = zweihandering;
    }

    public int getArm() {
        return arm;
    }

    public void setArm(int arm) {
        this.arm = arm;
    }

    /**
     * @return true if the entity is zweihandering (attacking with both hands)
     */
    public boolean isZweihandering() {
        return zweihandering;
    }

    public boolean isBladeExtended(int arm) {
        if (arm == LEFT) {
            return leftBlade;
        }
        if (arm == RIGHT) {
            return rightBlade;
        }
        return false;
    }

    public ToHitData toHit(Game game) {
        return PunchAttackAction.toHit(game, getEntityId(), game.getTarget(getTargetType(),
              getTargetId()), getArm(), isZweihandering());
    }

    /**
     * punches are impossible when physical attacks are impossible, or a retractable blade is extended
     *
     * @param game The current {@link Game}
     */
    protected static String toHitIsImpossible(Game game, Entity ae, Targetable target, int arm) {
        String physicalImpossible = PhysicalAttackAction.toHitIsImpossible(game, ae, target);
        if (physicalImpossible != null) {
            return physicalImpossible;
        }
        Hex attHex = game.getHexOf(ae);
        Hex targHex = game.getHexOf(target);
        int attackerHeight = ae.relHeight() + attHex.getLevel(); // The absolute level of the attacker's arms
        if (ae.isHullDown()) {
            attackerHeight--;
        }
        final int targetElevation = target.getElevation()
              + targHex.getLevel(); // The absolute level of the target's base
        final int targetHeight = targetElevation + target.getHeight(); // The absolute level of the target's top
        final int armLoc = (arm == PunchAttackAction.RIGHT) ? Mek.LOC_RIGHT_ARM
              : Mek.LOC_LEFT_ARM;
        if (((ae.getGrappled() != Entity.NONE)
              && (((ae.getGrappleSide() == Entity.GRAPPLE_LEFT) && (arm == Mek.LOC_LEFT_ARM))))
              || ((ae.getGrappleSide() == Entity.GRAPPLE_RIGHT) && (arm == Mek.LOC_RIGHT_ARM))) {
            return "grappled with punching arm";
        }
        if ((ae instanceof Mek) && ((Mek) ae).hasExtendedRetractableBlade()) {
            return "Extended retractable blade";
        }
        // non-meks can't punch
        if (!(ae instanceof Mek)) {
            return "Non-meks can't punch";
        }

        // Quads can't punch
        if (ae.entityIsQuad()) {
            return "Attacker is a quad";
        }

        // Can't punch with flipped arms
        if (ae.getArmsFlipped()) {
            return "Arms are flipped to the rear. Can not punch.";
        }

        // check if arm is present
        if (ae.isLocationBad(armLoc)) {
            return "Arm missing";
        }

        // check for no/minimal arms quirk
        if (ae.hasQuirk(OptionsConstants.QUIRK_NEG_NO_ARMS)) {
            return "No/minimal arms";
        }

        // check if shoulder is functional
        if (!ae.hasWorkingSystem(Mek.ACTUATOR_SHOULDER, armLoc)) {
            return "Shoulder destroyed";
        }

        // check if attacker has fired arm-mounted weapons
        if (ae.weaponFiredFrom(armLoc)) {
            return "Weapons fired from arm this turn";
        }

        // check elevation; if target base is above attacker's arms or target top is
        // below, cannot punch
        if ((targetElevation > attackerHeight) || (targetHeight < attackerHeight)) {
            return "Target elevation not in range";
        }

        // Prone 'Mechs can only be punched if they are one level higher than the attacker
        // See BMM 7th Printing, Physical Attacks and Prone 'Mechs
        if ((target instanceof Entity) && ((Entity) target).isProne()) {
            int attackerLevel = attHex.getLevel() + ae.getElevation();
            if (targetElevation != attackerLevel + 1) {
                return Messages.getString("PhysicalAttackAction.ProneMekPunch");
            }
        }

        // Cannot punch with an arm that has an active shield on it.
        if (ae.hasActiveShield(armLoc)) {
            return "Cannot punch with shield in active mode";
        }

        if (!ae.canFireWeapon(armLoc)) {
            return Messages.getString("WeaponAttackAction.CantFireWhileCarryingCargo");
        }

        return null;
    }

    /**
     * To-hit number for the specified arm to punch
     */
    public static ToHitData toHit(Game game, int attackerId, Targetable target, int arm, boolean zweihandering) {
        final Entity ae = game.getEntity(attackerId);
        if (ae == null) {
            logger.error("Attacker not valid");
            return new ToHitData(TargetRoll.IMPOSSIBLE, "Attacker not valid");
        }
        if (target == null) {
            logger.error("target not valid");
            return new ToHitData(TargetRoll.IMPOSSIBLE, "target not valid");
        }
        String impossible = PunchAttackAction.toHitIsImpossible(game, ae, target, arm);
        if (impossible != null) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, impossible);
        }

        Hex attHex = game.getHexOf(ae);
        Hex targHex = game.getHexOf(target);
        final int attackerHeight = ae.relHeight() + attHex.getLevel(); // The absolute level of the attacker's arms
        final int targetElevation = target.getElevation()
              + targHex.getLevel(); // The absolute level of the target's arms
        // Tripods can only punch targets in front arc per IO:AE p.158
        final int armArc;
        if (ae.isTripodMek()) {
            armArc = Compute.ARC_FORWARD;
        } else {
            armArc = (arm == PunchAttackAction.RIGHT) ? Compute.ARC_RIGHT_ARM : Compute.ARC_LEFT_ARM;
        }

        ToHitData toHit;

        // arguments legal?
        if ((arm != PunchAttackAction.RIGHT) && (arm != PunchAttackAction.LEFT)) {
            throw new IllegalArgumentException("Arm must be LEFT or RIGHT");
        }

        // Set the base BTH
        int base = ae.getCrew().getPiloting();

        toHit = new ToHitData(base, "base");

        toHit.addModifier(0, "Punch");

        PhysicalAttackAction.setCommonModifiers(toHit, game, ae, target);

        // Prone Meks can only punch vehicles in the same hex.
        if (ae.isProne()) {
            // The Mek must have both arms, the target must
            // be a tank, and both must be in the same hex.
            if (!ae.isLocationBad(Mek.LOC_RIGHT_ARM)
                  && !ae.isLocationBad(Mek.LOC_LEFT_ARM)
                  && (target instanceof Tank)
                  && (ae.getPosition().distance(target.getPosition()) == 0)) {
                toHit.addModifier(2, "attacker is prone");
            } else {
                return new ToHitData(TargetRoll.IMPOSSIBLE, "Attacker is prone");
            }
        }

        // Check facing if the Mek is not prone.
        else if (!ComputeArc.isInArc(ae.getPosition(), ae.getSecondaryFacing(),
              target, armArc)) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "Target not in arc");
        }

        // Attacks against adjacent buildings automatically hit.
        if ((target.getTargetType() == Targetable.TYPE_BUILDING)
              || (target.getTargetType() == Targetable.TYPE_FUEL_TANK)
              || (target.isBuildingEntityOrGunEmplacement())) {
            return new ToHitData(TargetRoll.AUTOMATIC_SUCCESS,
                  "Targeting adjacent building.");
        }

        final int armLoc = (arm == PunchAttackAction.RIGHT) ? Mek.LOC_RIGHT_ARM
              : Mek.LOC_LEFT_ARM;
        final int otherArm = armLoc == Mek.LOC_RIGHT_ARM ? Mek.LOC_LEFT_ARM : Mek.LOC_RIGHT_ARM;

        // damaged or missing actuators
        if (!ae.hasWorkingSystem(Mek.ACTUATOR_UPPER_ARM, armLoc)) {
            toHit.addModifier(2, "Upper arm actuator destroyed");
        }
        if (!ae.hasWorkingSystem(Mek.ACTUATOR_LOWER_ARM, armLoc)) {
            toHit.addModifier(2, "Lower arm actuator missing or destroyed");
        }

        if (zweihandering) {
            if (!ae.hasWorkingSystem(Mek.ACTUATOR_UPPER_ARM, otherArm)) {
                toHit.addModifier(2, "Upper arm actuator destroyed");
            }
            if (!ae.hasWorkingSystem(Mek.ACTUATOR_LOWER_ARM, otherArm)) {
                toHit.addModifier(2, "Lower arm actuator missing or destroyed");
            }
        }

        if (ae.hasFunctionalArmAES(armLoc)) {
            toHit.addModifier(-1, "AES modifier");
        }

        // Claws replace Actuators, but they are Equipment vs System as they
        // take up multiple crits.
        // Rules state +1 bth with claws and if claws are critted then you get
        // the normal +1 bth for missing hand actuator.
        // Damned if you do damned if you dont. --Torren.
        final boolean hasClaws = ((Mek) ae).hasClaw(armLoc);
        final boolean hasLowerArmActuator = ae.hasSystem(Mek.ACTUATOR_LOWER_ARM, armLoc);
        final boolean hasHandActuator = ae.hasSystem(Mek.ACTUATOR_HAND, armLoc);
        // Missing hand actuator is not cumulative with missing actuator,
        // but critical damage is cumulative
        if (!hasClaws && !hasHandActuator && hasLowerArmActuator
              && (((arm == PunchAttackAction.RIGHT) && !ae.hasQuirk(OptionsConstants.QUIRK_POS_BARREL_FIST_RA))
              || (arm == PunchAttackAction.LEFT)
              && !ae.hasQuirk(OptionsConstants.QUIRK_POS_BARREL_FIST_LA))) {
            toHit.addModifier(1, "Hand actuator missing");
            // Check for present but damaged hand actuator
        } else if (hasHandActuator && !hasClaws &&
              !ae.hasWorkingSystem(Mek.ACTUATOR_HAND, armLoc)) {
            toHit.addModifier(1, "Hand actuator destroyed");
        } else if (hasClaws) {
            // PLAYTEST3 claw modifier removed
            if (!game.getOptions().booleanOption(OptionsConstants.PLAYTEST_3)) {
                toHit.addModifier(1, "Using Claws");
            } else {
                toHit.addModifier(0, "Using Claws");
            }
        }

        if (hasHandActuator
              && (((arm == PunchAttackAction.RIGHT) && ae.hasQuirk(OptionsConstants.QUIRK_POS_BATTLE_FIST_RA))
              || ((arm == PunchAttackAction.LEFT)
              && ae.hasQuirk(OptionsConstants.QUIRK_POS_BATTLE_FIST_LA)))) {
            toHit.addModifier(-1, "BattleFist");
        }

        // elevation
        if (isConvertedQuadVee(target, game)) {
            toHit.setHitTable(ToHitData.HIT_PUNCH);
        } else {
            if ((attackerHeight == targetElevation) && !ae.isHullDown()) {
                if (target.getHeight() == 0) {
                    toHit.setHitTable(ToHitData.HIT_NORMAL);
                } else {
                    toHit.setHitTable(ToHitData.HIT_KICK);
                }
            } else {
                if (ae.isHullDown()) {
                    // If we are above the target, use punch chart
                    if (attackerHeight > targetElevation) {
                        toHit.setHitTable(ToHitData.HIT_PUNCH);
                    } else { // If on the same level, it's a punch to the legs
                        toHit.setHitTable(ToHitData.HIT_KICK);
                    } // Target can't be above, as it wouldn't be legal
                } else {
                    toHit.setHitTable(ToHitData.HIT_PUNCH);
                }
            }
        }

        // What to do with grounded dropships? Awaiting rules clarification, but
        // until then, we will assume that if the attacker height is less than half
        // the target elevation, then use HIT_PUNCH, otherwise HIT_NORMAL
        // See Dropship.rollHitLocation to see how HIT_PUNCH is handled
        if (target instanceof Dropship) {
            if ((attackerHeight - targetElevation) > (target.getHeight() / 2)) {
                toHit.setHitTable(ToHitData.HIT_NORMAL);
            } else {
                toHit.setHitTable(ToHitData.HIT_PUNCH);
            }
        }

        // factor in target side
        toHit.setSideTable(ComputeSideTable.sideTable(ae, target));

        // done!
        return toHit;
    }

    /**
     * Damage that the specified mek does with a punch.
     */
    public static int getDamageFor(Entity entity, int arm,
          boolean targetInfantry, boolean zweihandering) {
        final int armLoc = (arm == PunchAttackAction.RIGHT) ? Mek.LOC_RIGHT_ARM
              : Mek.LOC_LEFT_ARM;
        int damage = (int) Math.ceil(entity.getWeight() / 10.0);

        // Rules state tonnage/7 for claws
        if (((Mek) entity).hasClaw(armLoc)) {
            damage = (int) Math.ceil(entity.getWeight() / 7.0);
        }

        // PLAYTEST3 shields boost punching power. We only need to find the first shield entry to figure it out.
        if (entity.hasShield() && entity.getGame().getOptions().booleanOption(OptionsConstants.PLAYTEST_3)) {
            for (int slot = 0; slot < entity.getNumberOfCriticalSlots(armLoc); slot++) {
                CriticalSlot cs = entity.getCritical(armLoc, slot);

                if (cs == null) {
                    continue;
                }

                if (cs.getType() != CriticalSlot.TYPE_EQUIPMENT) {
                    continue;
                }

                Mounted<?> m = cs.getMount();
                EquipmentType type = m.getType();
                if ((type instanceof MiscType) && ((MiscType) type).isShield()) {
                    if ((((MiscMounted) m).getDamageAbsorption(entity, armLoc) > 0) && (((MiscMounted) m).getCurrentDamageCapacity(entity, armLoc) > 0)) {
                        if (type.hasFlag(MiscTypeFlag.S_SHIELD_LARGE)) {
                            damage += 3;
                            break;
                        } else if (type.hasFlag(MiscTypeFlag.S_SHIELD_MEDIUM)) {
                            damage += 2;
                            break;
                        } else if (type.hasFlag(MiscTypeFlag.S_SHIELD_SMALL)) {
                            damage += 1;
                            break;
                        }
                    } else {
                        // Shield DA or DC is 0, so no bonus
                        break;
                    }
                }
            }
        }
        
        // CamOps, pg. 82
        if (zweihandering) {
            damage += (int) Math.floor(entity.getWeight() / 10.0);
        }

        float multiplier = 1.0f;

        if (!entity.hasWorkingSystem(Mek.ACTUATOR_UPPER_ARM, armLoc)) {
            multiplier /= 2.0f;
        }
        if (!entity.hasWorkingSystem(Mek.ACTUATOR_LOWER_ARM, armLoc)) {
            multiplier /= 2.0f;
        }
        if (!entity.hasWorkingSystem(Mek.ACTUATOR_SHOULDER, armLoc)) {
            damage = 0;
        }
        if (((Mek) entity).hasActiveTSM()) {
            multiplier *= 2.0f;
        }
        int toReturn = (int) Math.floor(damage * multiplier)
              + entity.modifyPhysicalDamageForMeleeSpecialist();
        // underwater damage is half, round up (see bug 1110692)
        if (entity.getLocationStatus(armLoc) == ILocationExposureStatus.WET) {
            toReturn = (int) Math.ceil(toReturn * 0.5f);
        }
        if (targetInfantry) {
            toReturn = Math.max(1, toReturn / 10);
        }
        return toReturn;
    }

    @Override
    public String toSummaryString(final Game game) {
        String buffer;
        String rollLeft;
        String rollRight;
        final int arm = this.getArm();
        switch (arm) {
            case PunchAttackAction.BOTH:
                rollLeft = PunchAttackAction.toHit(game, this.getEntityId(),
                            game.getTarget(this.getTargetType(), this.getTargetId()), PunchAttackAction.LEFT, false)
                      .getValueAsString();
                rollRight = PunchAttackAction.toHit(game, this.getEntityId(),
                            game.getTarget(this.getTargetType(), this.getTargetId()), PunchAttackAction.RIGHT, false)
                      .getValueAsString();
                buffer = Messages.getString("BoardView1.punchBoth", rollLeft, rollRight);
                break;
            case PunchAttackAction.LEFT:
                rollLeft = PunchAttackAction.toHit(game, this.getEntityId(),
                            game.getTarget(this.getTargetType(), this.getTargetId()), PunchAttackAction.LEFT, false)
                      .getValueAsString();
                buffer = Messages.getString("BoardView1.punchLeft", rollLeft);
                break;
            case PunchAttackAction.RIGHT:
                rollRight = PunchAttackAction.toHit(game, this.getEntityId(),
                            game.getTarget(this.getTargetType(), this.getTargetId()), PunchAttackAction.RIGHT, false)
                      .getValueAsString();
                buffer = Messages.getString("BoardView1.punchRight", rollRight);
                break;
            default:
                buffer = "Error on punch action";
        }
        return buffer;
    }
}
