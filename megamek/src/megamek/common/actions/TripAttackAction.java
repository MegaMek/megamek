/*
 * Copyright (C) 2000-2004 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2006-2025 The MegaMek Team. All Rights Reserved.
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

import megamek.common.Hex;
import megamek.common.Player;
import megamek.common.ToHitData;
import megamek.common.compute.Compute;
import megamek.common.compute.ComputeArc;
import megamek.common.equipment.Mounted;
import megamek.common.game.Game;
import megamek.common.options.OptionsConstants;
import megamek.common.rolls.TargetRoll;
import megamek.common.units.Entity;
import megamek.common.units.Mek;
import megamek.common.units.QuadMek;
import megamek.common.units.Targetable;

/**
 * The attacker kicks the target.
 */
public class TripAttackAction extends PhysicalAttackAction {
    @Serial
    private static final long serialVersionUID = -8639566786588420601L;

    public TripAttackAction(int entityId, int targetId) {
        super(entityId, targetId);
    }

    public TripAttackAction(int entityId, int targetType, int targetId) {
        super(entityId, targetType, targetId);
    }

    public ToHitData toHit(Game game) {
        return TripAttackAction.toHit(game, getEntityId(), game.getTarget(getTargetType(), getTargetId()));
    }

    /**
     * To-hit number for the specified leg to kick
     */
    public static ToHitData toHit(Game game, int attackerId, Targetable target) {
        final Entity ae = game.getEntity(attackerId);
        if (ae == null) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "You can't attack from a null entity!");
        }

        if (!game.getOptions().booleanOption(OptionsConstants.ADVANCED_COMBAT_TAC_OPS_TRIP_ATTACK)) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "no Trip attack");
        }

        String impossible = PhysicalAttackAction.toHitIsImpossible(game, ae, target);
        if (impossible != null) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "impossible");
        }

        if (ae.getGrappled() != Entity.NONE) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "impossible");
        }

        if (!game.getOptions().booleanOption(OptionsConstants.BASE_FRIENDLY_FIRE)) {
            // a friendly unit can never be the target of a direct attack.
            if ((target.getTargetType() == Targetable.TYPE_ENTITY)
                  && ((target.getOwnerId() == ae.getOwnerId())
                  || ((((Entity) target).getOwner().getTeam() != Player.TEAM_NONE)
                  && (ae.getOwner().getTeam() != Player.TEAM_NONE)
                  && (ae.getOwner().getTeam() == ((Entity) target).getOwner().getTeam())))) {
                return new ToHitData(TargetRoll.IMPOSSIBLE,
                      "A friendly unit can never be the target of a direct attack.");
            }
        }

        ToHitData toHit;

        // non-meks can't trip or be tripped
        if (!(ae instanceof Mek) || !(target instanceof Mek)) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "Only meks can trip other meks");
        }

        // LAM AirMeks can only trip when grounded.
        if (ae.isAirborneVTOLorWIGE()) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "Cannot trip while airborne");
        }

        // described as a leg hook
        // needs 2 legs present
        if (ae.isLocationBad(Mek.LOC_LEFT_LEG) || ae.isLocationBad(Mek.LOC_RIGHT_LEG)) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "Leg missing");
        }

        // check range
        final int range = ae.getPosition().distance(target.getPosition());
        if (range > 1) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "Target out of range");
        }

        int limb1 = Entity.LOC_NONE;

        // check facing
        if (!ComputeArc.isInArc(ae.getPosition(), ae.getFacing(), target, Compute.ARC_FORWARD)) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "Target not in arc");
        }

        // can't trip while prone
        if (ae.isProne()) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "Attacker is prone");
        }
        if (((Entity) target).isProne()) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "Target is prone");
        }

        Hex attHex = game.getBoard().getHex(ae.getPosition());
        Hex targHex = game.getBoard().getHex(target.getPosition());
        final int attackerElevation = ae.getElevation() + attHex.getLevel();
        final int targetElevation = target.getElevation() + targHex.getLevel();

        if (attackerElevation != targetElevation) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "Attacker and Target must be at the same elevation");
        }

        // check if attacker has fired leg-mounted weapons
        boolean[] usedWeapons = new boolean[ae.locations()];
        for (int i = 0; i < ae.locations(); i++) {
            usedWeapons[i] = false;
        }

        for (Mounted<?> mounted : ae.getWeaponList()) {
            if (mounted.isUsedThisRound()) {
                int loc = mounted.getLocation();
                if (loc != Entity.LOC_NONE) {
                    usedWeapons[loc] = true;
                }
            }
        }

        // check for good hips / shoulders
        if (!ae.hasWorkingSystem(Mek.ACTUATOR_HIP, Mek.LOC_RIGHT_LEG)) {
            usedWeapons[Mek.LOC_RIGHT_LEG] = true;
        }
        if (!ae.hasWorkingSystem(Mek.ACTUATOR_HIP, Mek.LOC_LEFT_LEG)) {
            usedWeapons[Mek.LOC_LEFT_LEG] = true;
        }
        if (!ae.hasWorkingSystem(Mek.ACTUATOR_HIP, Mek.LOC_RIGHT_ARM)) {
            usedWeapons[Mek.LOC_RIGHT_ARM] = true;
        }
        if (!ae.hasWorkingSystem(Mek.ACTUATOR_HIP, Mek.LOC_LEFT_ARM)) {
            usedWeapons[Mek.LOC_LEFT_ARM] = true;
        }

        if (ae instanceof QuadMek) {
            if (usedWeapons[Mek.LOC_RIGHT_ARM]) {
                if (usedWeapons[Mek.LOC_LEFT_ARM]) {
                    return new ToHitData(TargetRoll.IMPOSSIBLE, "both legs unusable");
                }
                limb1 = Mek.LOC_LEFT_ARM;
            }
        } else if (usedWeapons[Mek.LOC_RIGHT_LEG]) { // normal attack uses both legs
            if (usedWeapons[Mek.LOC_LEFT_LEG]) {
                return new ToHitData(TargetRoll.IMPOSSIBLE, "both legs unusable");
            }
            limb1 = Mek.LOC_LEFT_LEG;
        }

        // Set the base BTH
        int base = ae.getCrew().getPiloting();

        // Start the To-Hit
        toHit = new ToHitData(base, "base");
        toHit.addModifier(-1, "Trip");

        PhysicalAttackAction.setCommonModifiers(toHit, game, ae, target);

        // Get best leg
        if (ae instanceof QuadMek) {
            if (limb1 == Entity.LOC_NONE) {
                ToHitData left = TripAttackAction.getLimbModifier(Mek.LOC_LEFT_ARM, ae);
                ToHitData right = TripAttackAction.getLimbModifier(Mek.LOC_RIGHT_ARM, ae);
                if (left.getValue() < right.getValue()) {
                    toHit.append(left);
                } else {
                    toHit.append(right);
                }
            } else {
                toHit.append(TripAttackAction.getLimbModifier(limb1, ae));
            }
        } else if (limb1 == Entity.LOC_NONE) {
            ToHitData left = TripAttackAction.getLimbModifier(Mek.LOC_LEFT_LEG, ae);
            ToHitData right = TripAttackAction.getLimbModifier(Mek.LOC_RIGHT_LEG, ae);
            if (left.getValue() < right.getValue()) {
                toHit.append(left);
            } else {
                toHit.append(right);
            }
        } else {
            toHit.append(TripAttackAction.getLimbModifier(limb1, ae));
        }

        if (ae.hasFunctionalLegAES()) {
            toHit.addModifier(-1, "AES modifier");
        }

        // done!
        return toHit;
    }

    private static ToHitData getLimbModifier(int loc, Entity ae) {
        ToHitData toHit = new ToHitData();
        // damaged or missing actuators
        if (!ae.hasWorkingSystem(Mek.ACTUATOR_UPPER_LEG, loc)) {
            toHit.addModifier(2, "Upper leg actuator destroyed");
        }

        if (!ae.hasWorkingSystem(Mek.ACTUATOR_LOWER_LEG, loc)) {
            toHit.addModifier(2, "Lower leg actuator destroyed");
        }

        if (!ae.hasWorkingSystem(Mek.ACTUATOR_FOOT, loc)) {
            toHit.addModifier(1, "Foot actuator destroyed");
        }
        return toHit;
    }
}
