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

import megamek.common.ToHitData;
import megamek.common.game.Game;
import megamek.common.options.OptionsConstants;
import megamek.common.rolls.TargetRoll;
import megamek.common.units.Entity;
import megamek.common.units.Mek;
import megamek.common.units.ProtoMek;
import megamek.common.units.Targetable;

/**
 * The attacker grapples the target.
 */
public class BreakGrappleAttackAction extends PhysicalAttackAction {
    @Serial
    private static final long serialVersionUID = 5615694825997720537L;

    public BreakGrappleAttackAction(int entityId, int targetId) {
        super(entityId, targetId);
    }

    public BreakGrappleAttackAction(int entityId, int targetType, int targetId) {
        super(entityId, targetType, targetId);
    }

    /**
     * Generates the to hit data for this action.
     *
     * @param game The current {@link Game}
     *
     * @return the to hit data object for this action.
     *
     * @see #toHit(Game, int, Targetable)
     */
    public ToHitData toHit(Game game) {
        return toHit(game, getEntityId(), game.getTarget(getTargetType(), getTargetId()));
    }

    /**
     * To-hit number
     *
     * @param game The current {@link Game}
     */
    public static ToHitData toHit(Game game, int attackerId, Targetable target) {
        final Entity attackingEntity = game.getEntity(attackerId);
        if (attackingEntity == null) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "You can't attack from a null entity!");
        }

        if (!game.getOptions().booleanOption(OptionsConstants.ADVANCED_COMBAT_TAC_OPS_GRAPPLING)) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "grappling attack not allowed");
        }

        // LAM AirMeks can only grapple when grounded.
        if (attackingEntity.isAirborneVTOLorWIGE()) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "Cannot grapple while airborne");
        }

        String impossible = toHitIsImpossible(game, attackingEntity, target);
        if ((impossible != null) && !impossible.equals("Locked in Grapple")) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "impossible");
        }

        if ((attackingEntity.getGrappled() != Entity.NONE) && attackingEntity.isChainWhipGrappled()) {
            return new ToHitData(TargetRoll.IMPOSSIBLE,
                  "cannot break free from a chain whip grapple");
        }

        ToHitData toHit;

        // non-meks can't grapple or be grappled
        if (!(attackingEntity instanceof Mek) && !(attackingEntity instanceof ProtoMek)) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "Only Meks and ProtoMeks can be grappled");
        }

        if (attackingEntity.getGrappled() != target.getId()) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "Not grappled");
        }

        // Set the base BTH
        int base = attackingEntity.getCrew().getPiloting();

        // Start the To-Hit
        toHit = new ToHitData(base, "base");

        PhysicalAttackAction.setCommonModifiers(toHit, game, attackingEntity, target);

        if (attackingEntity.isGrappleAttacker()) {
            toHit.addModifier(TargetRoll.AUTOMATIC_SUCCESS, "original attacker");
            return toHit;
        }

        setCommonModifiers(toHit, game, attackingEntity, target);

        if (attackingEntity instanceof Mek) {
            // damaged or missing actuators
            if (!attackingEntity.hasWorkingSystem(Mek.ACTUATOR_SHOULDER, Mek.LOC_LEFT_ARM)) {
                toHit.addModifier(2, "Left shoulder actuator destroyed");
            }
            if (!attackingEntity.hasWorkingSystem(Mek.ACTUATOR_UPPER_ARM, Mek.LOC_LEFT_ARM)) {
                toHit.addModifier(2, "Left upper arm actuator destroyed");
            }
            if (!attackingEntity.hasWorkingSystem(Mek.ACTUATOR_LOWER_ARM, Mek.LOC_LEFT_ARM)) {
                toHit.addModifier(2, "Left lower arm actuator destroyed");
            }
            if (!attackingEntity.hasWorkingSystem(Mek.ACTUATOR_HAND, Mek.LOC_LEFT_ARM)) {
                toHit.addModifier(1, "Left hand actuator destroyed");
            }

            if (!attackingEntity.hasWorkingSystem(Mek.ACTUATOR_SHOULDER, Mek.LOC_RIGHT_ARM)) {
                toHit.addModifier(2, "Right shoulder actuator destroyed");
            }
            if (!attackingEntity.hasWorkingSystem(Mek.ACTUATOR_UPPER_ARM, Mek.LOC_RIGHT_ARM)) {
                toHit.addModifier(2, "Right upper arm actuator destroyed");
            }
            if (!attackingEntity.hasWorkingSystem(Mek.ACTUATOR_LOWER_ARM, Mek.LOC_RIGHT_ARM)) {
                toHit.addModifier(2, "Right lower arm actuator destroyed");
            }
            if (!attackingEntity.hasWorkingSystem(Mek.ACTUATOR_HAND, Mek.LOC_RIGHT_ARM)) {
                toHit.addModifier(1, "Right hand actuator destroyed");
            }
            if (attackingEntity.hasFunctionalArmAES(Mek.LOC_RIGHT_ARM)
                  && attackingEntity.hasFunctionalArmAES(Mek.LOC_LEFT_ARM)) {
                toHit.addModifier(-1, "AES modifier");
            }
        }
        int weightMod = getWeightMod((Entity) target, attackingEntity);

        if (weightMod != 0) {
            toHit.addModifier(weightMod, "Weight class difference");
        }
        // done!
        return toHit;
    }

    private static int getWeightMod(Entity target, Entity attackingEntity) {
        // Weight class difference
        GrappleAttackAction.getWeaponMod(attackingEntity, target);
        return weightMod;
    }
}
