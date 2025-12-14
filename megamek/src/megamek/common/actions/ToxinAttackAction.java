/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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
import megamek.common.compute.ComputeSideTable;
import megamek.common.game.Game;
import megamek.common.options.OptionsConstants;
import megamek.common.rolls.TargetRoll;
import megamek.common.units.Entity;
import megamek.common.units.Infantry;
import megamek.common.units.Targetable;
import megamek.logging.MMLogger;

/**
 * Toxin Gas Attack (IO pg 79) A Conventional Infantry unit with the Gas Effuser (Toxin) cybernetic implant releases
 * toxic gas to damage enemy conventional infantry in the same hex. On success, the target takes 0.25 points of damage
 * per attacking trooper.
 */
public class ToxinAttackAction extends AbstractAttackAction {
    private static final MMLogger LOGGER = MMLogger.create(ToxinAttackAction.class);

    @Serial
    private static final long serialVersionUID = -3958472619384756123L;

    /** Damage per attacking trooper */
    private static final double DAMAGE_PER_TROOPER = 0.25;

    public ToxinAttackAction(int entityId, int targetId) {
        super(entityId, targetId);
    }

    public ToxinAttackAction(int entityId, int targetType, int targetId) {
        super(entityId, targetType, targetId);
    }

    /**
     * Calculate the damage dealt by a toxin attack.
     *
     * @param attacker the attacking infantry unit
     *
     * @return the damage dealt (0.25 per trooper, rounded normally)
     */
    public static int getDamageFor(Infantry attacker) {
        int troopers = attacker.getShootingStrength();
        return (int) Math.round(troopers * DAMAGE_PER_TROOPER);
    }

    public ToHitData toHit(Game game) {
        return toHit(game, getEntityId(), game.getTarget(getTargetType(), getTargetId()));
    }

    public static ToHitData toHit(Game game, int attackerId, Targetable target) {
        final Entity attackingEntity = game.getEntity(attackerId);
        int targetId = Entity.NONE;
        Entity targetEntity = null;

        // Arguments legal?
        if (attackingEntity == null) {
            LOGGER.error("Attacker not valid");
            return new ToHitData(TargetRoll.IMPOSSIBLE, "Attacker not valid");
        }

        if (target == null) {
            LOGGER.error("Target not valid");
            return new ToHitData(TargetRoll.IMPOSSIBLE, "Target not valid");
        }

        // Can only attack entities
        if (target.getTargetType() != Targetable.TYPE_ENTITY) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "Target must be an entity");
        }

        targetEntity = (Entity) target;
        targetId = target.getId();

        // Check friendly fire
        if (!game.getOptions().booleanOption(OptionsConstants.BASE_FRIENDLY_FIRE)) {
            if ((target.getOwnerId() == attackingEntity.getOwnerId())
                  || ((targetEntity.getOwner().getTeam() != Player.TEAM_NONE)
                  && (attackingEntity.getOwner().getTeam() != Player.TEAM_NONE)
                  && (attackingEntity.getOwner().getTeam() == targetEntity.getOwner().getTeam()))) {
                return new ToHitData(TargetRoll.IMPOSSIBLE,
                      "A friendly unit can never be the target of a direct attack.");
            }
        }

        Hex attHex = game.getHexOf(attackingEntity);
        Hex targHex = game.getHexOf(target);
        if ((attHex == null) || (targHex == null)) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "off board");
        }

        boolean inSameBuilding = Compute.isInSameBuilding(game, attackingEntity, targetEntity);

        // Can't target yourself
        if (attackingEntity.equals(targetEntity)) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "You can't target yourself");
        }

        // Only conventional infantry can make this attack
        if (!(attackingEntity instanceof Infantry) || !attackingEntity.isConventionalInfantry()) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "Only conventional infantry can use toxin gas");
        }

        // Must have the Gas Effuser (Toxin) implant
        if (!attackingEntity.hasAbility(OptionsConstants.MD_GAS_EFFUSER_TOXIN)) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "Attacker lacks Gas Effuser (Toxin) implant");
        }

        // Target must be conventional infantry
        if (!targetEntity.isConventionalInfantry()) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "Target must be conventional infantry");
        }

        // Target must not be protected from gas attacks
        Infantry targetInfantry = (Infantry) targetEntity;
        if (targetInfantry.isProtectedFromGasAttacks()) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "Target is protected from gas attacks");
        }

        // NOTE: Unlike pheromone, toxin CAN be used repeatedly on the same target

        // Can't target a transported entity
        if (Entity.NONE != targetEntity.getTransportId()) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "Target is a passenger");
        }

        // Can't target an entity conducting a swarm attack
        if (Entity.NONE != targetEntity.getSwarmTargetId()) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "Target is swarming a Mek");
        }

        // Check range - must be same hex (range 0)
        final int range = attackingEntity.getPosition().distance(target.getPosition());
        if (range > 0) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "Target not in same hex");
        }

        // Can't physically attack units making DFA attacks
        if (targetEntity.isMakingDfa()) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "Target is making a DFA attack");
        }

        // Set the base BTH - gunnery skill
        int base = attackingEntity.getCrew().getGunnery();

        // Start the To-Hit
        ToHitData toHit = new ToHitData(base, "base");

        // Attacker movement modifier
        toHit.append(Compute.getAttackerMovementModifier(game, attackerId));

        // Target movement modifier
        toHit.append(Compute.getTargetMovementModifier(game, targetId));

        // Attacker terrain modifier
        toHit.append(Compute.getAttackerTerrainModifier(game, attackerId));

        // Target terrain modifier
        toHit.append(Compute.getTargetTerrainModifier(game, targetEntity, 0, inSameBuilding));

        // Attacker is spotting
        if (attackingEntity.isSpotting()) {
            toHit.addModifier(+1, "attacker is spotting");
        }

        // Taser feedback
        if (attackingEntity.getTaserFeedBackRounds() > 0) {
            toHit.addModifier(1, "Taser feedback");
        }

        // Target immobile
        toHit.append(Compute.getImmobileMod(targetEntity));

        // Night modifiers
        toHit.append(nightModifiers(game, target, null, attackingEntity, false));

        // Physical attack advantages
        Compute.modifyPhysicalBTHForAdvantages(attackingEntity, targetEntity, toHit, game);

        // Factor in target side
        toHit.setSideTable(ComputeSideTable.sideTable(attackingEntity, targetEntity));

        // NOTE: Per IO pg 79, area-effect weapons like toxin gas have NO range modifier
        // Since we're in same hex (range 0), this is already satisfied

        return toHit;
    }
}
