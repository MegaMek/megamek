/*
 * Copyright (C) 2000-2004 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2002-2026 The MegaMek Team. All Rights Reserved.
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
import java.util.ArrayList;
import java.util.List;

import megamek.client.ui.Messages;
import megamek.common.Hex;
import megamek.common.ToHitData;
import megamek.common.compute.Compute;
import megamek.common.compute.ComputeArc;
import megamek.common.compute.ComputeSideTable;
import megamek.common.equipment.MiscType;
import megamek.common.equipment.Mounted;
import megamek.common.game.Game;
import megamek.common.interfaces.ILocationExposureStatus;
import megamek.common.rolls.TargetRoll;
import megamek.common.rules.totalwarfare.TWRulesManager;
import megamek.common.units.Dropship;
import megamek.common.units.Entity;
import megamek.common.units.Infantry;
import megamek.common.units.Mek;
import megamek.common.units.Tank;
import megamek.common.units.Targetable;

/**
 * The attacker kicks the target.
 */
public class KickAttackAction extends PhysicalAttackAction {
    @Serial
    private static final long serialVersionUID = 1697321306815235635L;
    public static final int BOTH = 0;
    public static final int LEFT = 1;
    public static final int RIGHT = 2;
    public static final int LEFT_MULE = 3;
    public static final int RIGHT_MULE = 4;

    private int leg;

    public KickAttackAction(int entityId, int targetId, int leg) {
        super(entityId, targetId);
        this.leg = leg;
    }

    public KickAttackAction(int entityId, int targetType, int targetId, int leg) {
        super(entityId, targetType, targetId);
        this.leg = leg;
    }

    public int getLeg() {
        return leg;
    }

    public void setLeg(int leg) {
        this.leg = leg;
    }

    private static List<Integer> getLegLocs(Entity entity,
                                            int leg) {
        List<Integer> legLocs = new ArrayList<>();

        if (entity.entityIsQuad() && (leg != LEFT_MULE) && (leg != RIGHT_MULE)) {
            legLocs.add(Mek.LOC_RIGHT_ARM);
            legLocs.add(Mek.LOC_LEFT_ARM);
        } else {
            legLocs.add(Mek.LOC_RIGHT_LEG);
            legLocs.add(Mek.LOC_LEFT_LEG);
        }
        if (entity.isTripodMek()) {
            legLocs.add(Mek.LOC_CENTER_LEG);
        }

        return legLocs;
    }

    /**
     * Damage that the specified mek does with a kick
     *
     * @return The kick damage for the 'Mek, or 0 for non-'Mek entities.
     */
    public static int getDamageFor(Entity entity, int leg,
          boolean targetInfantry) {
        if (!(entity instanceof Mek)) {
            return 0; // Non-'Meks can't kick, so can't deal damage this way.
        }
        List<Integer> legLocs = getLegLocs(entity, leg);
        int kickingLeg;

        if (entity.entityIsQuad() && (leg != LEFT_MULE) && (leg != RIGHT_MULE)) {
            kickingLeg = (leg == LEFT) ? Mek.LOC_LEFT_ARM : Mek.LOC_RIGHT_ARM;
        } else {
            kickingLeg = (leg == LEFT) ? Mek.LOC_LEFT_LEG : Mek.LOC_RIGHT_LEG;
        }

        /* Ordering is very important here. As per Core p.87/88.
         * The Order is:
         * 1) Base Value
         * 2) Equipment modified (eg: TSM, Talons, Shield)
         * 3) Actuator modifiers (eg: Destroyed actuators)
         *
         * Rounding applies for each step which divides unless otherwise noted.
         */
        int damage = (int) Math.floor(entity.getWeight() / 5.0);

        // Talons first. Confirmed this is rounding normally (0.5 goes up) as per TO:AUE p.103
        // Core p.196, "Rounding normally"
        if (entity.hasWorkingMisc(MiscType.F_TALON, null, kickingLeg) && entity.hasWorkingSystem(Mek.ACTUATOR_FOOT,
                                                                                                 kickingLeg)) {
            damage = Math.round(damage * 1.5f);
        }

        // TSM next (Core p.196, "Talons can receive TSM damage bonus")
        if (((Mek) entity).hasActiveTSM()) {
            damage *= 2;
        }

        // Check all applicable leg locations for actuator damage. Applies with both legs
        for (int legLoc : legLocs) {
            // Actuator damage
            if (!entity.hasWorkingSystem(Mek.ACTUATOR_UPPER_LEG, legLoc)) {
                damage = (int) Math.floor(damage / 2.0f);
            }
            if (!entity.hasWorkingSystem(Mek.ACTUATOR_LOWER_LEG, legLoc)) {
                damage = (int) Math.floor(damage / 2.0f);
            }
            // Hip should never occur, but just in case, 0.
            if (!entity.hasWorkingSystem(Mek.ACTUATOR_HIP, legLoc)) {
                return 0;
            }
        }

        // underwater damage is half, round up (see bug 1110692)
        // Core p.77 confirms this is floor, not ceil
        if (entity.getLocationStatus(kickingLeg) == ILocationExposureStatus.WET) {
            damage = (int) Math.floor(damage * 0.5f);
        }
        if (targetInfantry) {
            damage = (int) Math.ceil(damage / 10);
        }
        damage = Math.max(1, damage);
        // SPA adding after TSM, but before actuators.
        damage += entity.modifyPhysicalDamageForMeleeSpecialist();

        return damage;
    }

    public ToHitData toHit(Game game) {
        return KickAttackAction.toHit(game, getEntityId(), game.getTarget(getTargetType(),
              getTargetId()), getLeg());
    }

    /**
     * To-hit number for the specified leg to kick
     */
    public static ToHitData toHit(Game game, int attackerId,
          Targetable target, int leg) {
        final Entity attackingEntity = game.getEntity(attackerId);
        if (attackingEntity == null) {
            return new ToHitData(TargetRoll.IMPOSSIBLE,
                  "You can't attack from a null entity!");
        }

        if (!(attackingEntity instanceof Mek)) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "Non-'Meks can't kick.");
        }

        if (attackingEntity.isStuck()) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "Bogged-down units can't kick.");
        }

        String impossible = PhysicalAttackAction.toHitIsImpossible(game, attackingEntity, target);
        if (impossible != null) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "impossible");
        }

        Hex attHex = game.getHexOf(attackingEntity);
        Hex targHex = game.getHexOf(target);
        final int attackerElevation = attackingEntity.getElevation() + attHex.getLevel();
        final int targetElevation = target.getElevation()
              + targHex.getLevel();
        final int targetHeight = targetElevation + target.getHeight();

        boolean muleKick = false;
        List<Integer> legLocs = getLegLocs(attackingEntity, leg);
        int kickingLeg;

        if (attackingEntity.entityIsQuad() && (leg != LEFT_MULE) && (leg != RIGHT_MULE)) {
            kickingLeg = (leg == LEFT) ? Mek.LOC_LEFT_ARM : Mek.LOC_RIGHT_ARM;
            muleKick = true;
        } else {
            kickingLeg = (leg == LEFT) ? Mek.LOC_LEFT_LEG : Mek.LOC_RIGHT_LEG;
        }

        ToHitData toHit;

        // arguments legal?
        // By allowing mule kicks, this gets a little more complicated :(
        if ((leg != KickAttackAction.RIGHT) && (leg != KickAttackAction.LEFT)
              && (leg != KickAttackAction.RIGHT_MULE)
              && (leg != KickAttackAction.LEFT_MULE)) {
            throw new IllegalArgumentException(
                  "Leg must be one of LEFT, RIGHT, LEFT_MULE, or RIGHT_MULE");
        }

        // if either front leg is bad, no kick at all. No mule, no front.
        if (attackingEntity.isLocationBad(Mek.LOC_LEFT_LEG) || attackingEntity.isLocationBad(Mek.LOC_RIGHT_LEG) ||
            (attackingEntity.isTripodMek() && attackingEntity.isLocationBad(Mek.LOC_CENTER_LEG))) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "Leg missing");
        }
        if (attackingEntity.entityIsQuad()
                && (attackingEntity.isLocationBad(Mek.LOC_LEFT_ARM)
                    || attackingEntity.isLocationBad(Mek.LOC_RIGHT_ARM)) && muleKick) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "Leg missing");
        }

        // check if all hips are operational
        if (!attackingEntity.entityIsQuad() && (!attackingEntity.hasWorkingSystem(Mek.ACTUATOR_HIP, Mek.LOC_LEFT_LEG)
                                                || !attackingEntity.hasWorkingSystem(Mek.ACTUATOR_HIP,
                                                                                     Mek.LOC_RIGHT_LEG))) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "Hip destroyed");
        }
        // Core p.238. TW p. 147
        if (attackingEntity.entityIsQuad()) {
            if ((leg == KickAttackAction.LEFT_MULE || leg == KickAttackAction.RIGHT_MULE) &&
                (!attackingEntity.hasWorkingSystem(Mek.ACTUATOR_HIP, Mek.LOC_LEFT_LEG)
                 || !attackingEntity.hasWorkingSystem(Mek.ACTUATOR_HIP, Mek.LOC_RIGHT_LEG))) {
                return new ToHitData(TargetRoll.IMPOSSIBLE, "Hip destroyed");
            } else if ((leg == KickAttackAction.LEFT || leg == KickAttackAction.RIGHT) &&
                       (!attackingEntity.hasWorkingSystem(Mek.ACTUATOR_HIP, Mek.LOC_LEFT_ARM)
                        || !attackingEntity.hasWorkingSystem(Mek.ACTUATOR_HIP, Mek.LOC_RIGHT_ARM))) {
                return new ToHitData(TargetRoll.IMPOSSIBLE, "Hip destroyed");
            }
        }

        // check if attacker has fired leg-mounted weapons
        for (Mounted<?> mounted : attackingEntity.getWeaponList()) {
            if (mounted.isUsedThisRound() && (mounted.getLocation() == kickingLeg)) {
                return new ToHitData(TargetRoll.IMPOSSIBLE,
                      "Weapons fired from leg this turn");
            }
            // TW requires both legs to be free of weapons fire for a mule kick, not just the kicking leg.
            if (attackingEntity.isQuadMek() &&
                mounted.isUsedThisRound() &&
                legLocs.contains(mounted.getLocation()) &&
                muleKick &&
                Game.rulesManager instanceof TWRulesManager) {
                return new ToHitData(TargetRoll.IMPOSSIBLE,
                                     "Weapons fired from rear legs this turn");
            }
        }

        // check range
        final int range = attackingEntity.getPosition().distance(target.getPosition());

        // check elevation
        if (target.isAirborneVTOLorWIGE()) {
            if (targetElevation - attackerElevation != 0) {
                return new ToHitData(TargetRoll.IMPOSSIBLE, "Target elevation not in range");
            }
        } else if ((attackerElevation < targetElevation)
              || (attackerElevation > targetHeight)) {
            return new ToHitData(TargetRoll.IMPOSSIBLE,
                  "Target elevation not in range");
        }

        // check facing
        // Don't check arc for stomping infantry or tanks.
        // Tripods use torso facing (secondary) for physical attacks per IO:AE p.158
        if ((0 != range) && (!muleKick)) {
            int facing = attackingEntity.isTripodMek() ? attackingEntity.getSecondaryFacing() : attackingEntity.getFacing();
            if (!ComputeArc.isInArc(attackingEntity.getPosition(), facing, target, Compute.ARC_FORWARD)) {
                return new ToHitData(TargetRoll.IMPOSSIBLE, "Target not in arc");
            }
        }

        // check facing, part 2: Mule kick
        // Tripods cannot perform mule kicks per IO:AE p.158
        if (muleKick) {
            if (attackingEntity.isTripodMek()) {
                return new ToHitData(TargetRoll.IMPOSSIBLE, "Tripods cannot perform mule kicks");
            }
            if ((0 != range)
                && !ComputeArc.isInArc(attackingEntity.getPosition(),
                                       attackingEntity.getFacing(),
                                       target,
                                       Compute.ARC_REAR)) {
                return new ToHitData(TargetRoll.IMPOSSIBLE, "Target not in arc");
            }
        }

        // can't kick while prone
        if (attackingEntity.isProne()) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "Attacker is prone");
        }

        // Prone 'Meks can only be kicked if they are at the same level as the attacker
        // See BMM 7th Printing, Physical Attacks and Prone 'Meks
        if ((target instanceof Entity) && ((Entity) target).isProne()) {
            if (targetElevation != attackerElevation) {
                return new ToHitData(TargetRoll.IMPOSSIBLE,
                      Messages.getString("PhysicalAttackAction.ProneMekKick"));
            }
        }

        if (attackingEntity.isHullDown()) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "Attacker is hull down");
        }

        // Attacks against adjacent buildings automatically hit.
        if ((target.getTargetType() == Targetable.TYPE_BUILDING)
              || (target.getTargetType() == Targetable.TYPE_FUEL_TANK)
              || (target.isBuildingEntityOrGunEmplacement())) {
            return new ToHitData(TargetRoll.AUTOMATIC_SUCCESS,
                  "Targeting adjacent building.");
        }

        // Set the base BTH
        int base = attackingEntity.getCrew().getPiloting();

        // Start the To-Hit
        toHit = new ToHitData(base, "base");

        toHit.addModifier(Game.rulesManager.getRulesPhysical().getKickModifier(), "Kick");

        PhysicalAttackAction.setCommonModifiers(toHit, game, attackingEntity, target);

        // +3 modifier for kicking infantry in same hex
        // see bug 1749177
        if ((target instanceof Infantry) && (range == 0)) {
            toHit.addModifier(3, "Stomping Infantry");
        }

        // Mule kick?
        if (muleKick) {
            int muleModifier = Game.rulesManager.getRulesUnits().getMuleKickModifier();
            if (muleModifier > 0) {
                toHit.addModifier(muleModifier, "Quad Mek making a mule kick");
            }
        }

        for (int legLoc : legLocs) {
            // damaged or missing actuators
            if (!attackingEntity.hasWorkingSystem(Mek.ACTUATOR_UPPER_LEG, legLoc)) {
                toHit.addModifier(2, "Upper leg actuator destroyed");
            }
            if (!attackingEntity.hasWorkingSystem(Mek.ACTUATOR_LOWER_LEG, legLoc)) {
                toHit.addModifier(2, "Lower leg actuator destroyed");
            }
            if (!attackingEntity.hasWorkingSystem(Mek.ACTUATOR_FOOT, legLoc)) {
                toHit.addModifier(1, "Foot actuator destroyed");
            }
        }

        if (attackingEntity.hasFunctionalLegAES()) {
            toHit.addModifier(-1, "AES bonus");
        }

        // elevation
        if (isConvertedQuadVee(target, game)) {
            toHit.setHitTable(ToHitData.HIT_PUNCH);
        } else {
            if (attackerElevation < targetHeight) {
                toHit.setHitTable(ToHitData.HIT_KICK);
            } else if (target.getHeight() > 0) {
                toHit.setHitTable(ToHitData.HIT_PUNCH);
            } else {
                toHit.setHitTable(ToHitData.HIT_NORMAL);
            }
        }

        // What to do with grounded dropships? Awaiting rules clarification, but
        // until then, we will assume that if the attacker height is less than half
        // the target elevation, then use HIT_KICK, otherwise HIT_NORMAL
        // See Dropship.rollHitLocation to see how HIT_KICK is handled
        if (target instanceof Dropship) {
            if ((attackerElevation - targetElevation) > (target.getHeight() / 2)) {
                toHit.setHitTable(ToHitData.HIT_NORMAL);
            } else {
                toHit.setHitTable(ToHitData.HIT_KICK);
            }
        }

        // factor in target side
        toHit.setSideTable(ComputeSideTable.sideTable(attackingEntity, target));

        // BMRr pg. 42, "The side on which a vehicle takes damage is determined
        // randomly if the BattleMek is attacking from the same hex."
        if ((0 == range) && (target instanceof Tank)) {
            toHit.setSideTable(ToHitData.SIDE_RANDOM);
        }

        // done!
        return toHit;
    }

    @Override
    public String toSummaryString(final Game game) {
        String rollLeft;
        String rollRight;
        String buffer;
        final int leg = this.getLeg();
        switch (leg) {
            case KickAttackAction.BOTH:
                rollLeft = KickAttackAction.toHit(game, this.getEntityId(),
                            game.getTarget(this.getTargetType(), this.getTargetId()), KickAttackAction.LEFT)
                      .getValueAsString();
                rollRight = KickAttackAction.toHit(game, this.getEntityId(),
                            game.getTarget(this.getTargetType(), this.getTargetId()), KickAttackAction.RIGHT)
                      .getValueAsString();
                buffer = Messages.getString("BoardView1.kickBoth", rollLeft, rollRight);
                break;
            case KickAttackAction.LEFT:
                rollLeft = KickAttackAction.toHit(game, this.getEntityId(),
                            game.getTarget(this.getTargetType(), this.getTargetId()), KickAttackAction.LEFT)
                      .getValueAsString();
                buffer = Messages.getString("BoardView1.kickLeft", rollLeft);
                break;
            case KickAttackAction.RIGHT:
                rollRight = KickAttackAction.toHit(game, this.getEntityId(),
                            game.getTarget(this.getTargetType(), this.getTargetId()), KickAttackAction.RIGHT)
                      .getValueAsString();
                buffer = Messages.getString("BoardView1.kickRight", rollRight);
                break;
            default:
                buffer = "Error on kick action";
        }
        return buffer;
    }
}
