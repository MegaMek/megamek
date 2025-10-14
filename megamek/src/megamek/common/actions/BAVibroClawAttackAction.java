/*
 * Copyright (C) 2008 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2008-2025 The MegaMek Team. All Rights Reserved.
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
import megamek.common.battleArmor.BattleArmor;
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
 * A BattleArmor uses its vibroclaws
 */
public class BAVibroClawAttackAction extends AbstractAttackAction {
    private static final MMLogger LOGGER = MMLogger.create(BAVibroClawAttackAction.class);

    @Serial
    private static final long serialVersionUID = 1432011536091665084L;

    public BAVibroClawAttackAction(int entityId, int targetId) {
        super(entityId, targetId);
    }

    public BAVibroClawAttackAction(int entityId, int targetType,
          int targetId) {
        super(entityId, targetType, targetId);
    }

    /**
     * Damage a BA does with its vibroclaws.
     */
    public static int getDamageFor(Entity entity) {
        return Compute.missilesHit(((BattleArmor) entity).getShootingStrength()) * entity.getVibroClaws();
    }

    public ToHitData toHit(Game game) {
        return toHit(game, getEntityId(), game.getTarget(getTargetType(),
              getTargetId()));
    }

    public static ToHitData toHit(Game game, int attackerId, Targetable target) {
        final Entity attackingEntity = game.getEntity(attackerId);
        int targetId = Entity.NONE;
        Entity targetEntity = null;
        // arguments legal?
        if (attackingEntity == null) {
            LOGGER.error("Attacker not valid");
            return new ToHitData(TargetRoll.IMPOSSIBLE, "Attacker not valid");
        }

        if (target == null) {
            LOGGER.error("target not valid");
            return new ToHitData(TargetRoll.IMPOSSIBLE, "target not valid");
        }

        if (target.getTargetType() == Targetable.TYPE_ENTITY) {
            targetEntity = (Entity) target;
            targetId = target.getId();
        }

        if (!game.getOptions().booleanOption(OptionsConstants.BASE_FRIENDLY_FIRE)) {
            // a friendly unit can never be the target of a direct attack.
            if ((target.getTargetType() == Targetable.TYPE_ENTITY)
                  && ((target.getOwnerId() == attackingEntity.getOwnerId())
                  || ((((Entity) target).getOwner().getTeam() != Player.TEAM_NONE)
                  && (attackingEntity.getOwner().getTeam() != Player.TEAM_NONE)
                  && (attackingEntity.getOwner().getTeam() == ((Entity) target).getOwner().getTeam())))) {
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

        ToHitData toHit;

        // can't target yourself
        if (attackingEntity.equals(targetEntity)) {
            return new ToHitData(TargetRoll.IMPOSSIBLE,
                  "You can't target yourself");
        }

        // only BA can make this attack
        if (!(attackingEntity instanceof BattleArmor)) {
            return new ToHitData(TargetRoll.IMPOSSIBLE,
                  "Non-BA can't make vibroclaw-physicalAttacks");
        }

        if ((targetEntity != null) && !((targetEntity instanceof Infantry))) {
            return new ToHitData(TargetRoll.IMPOSSIBLE,
                  "can't target non-infantry");
        }

        // need to have vibroclaws to make this attack
        if (attackingEntity.getVibroClaws() == 0) {
            return new ToHitData(TargetRoll.IMPOSSIBLE,
                  "no vibro claws mounted");
        }

        // Can't target a transported entity.
        if ((targetEntity != null) && (Entity.NONE != targetEntity.getTransportId())) {
            return new ToHitData(TargetRoll.IMPOSSIBLE,
                  "Target is a passenger.");
        }

        // Can't target an entity conducting a swarm attack.
        if ((targetEntity != null) && (Entity.NONE != targetEntity.getSwarmTargetId())) {
            return new ToHitData(TargetRoll.IMPOSSIBLE,
                  "Target is swarming a Mek.");
        }

        // check range
        final int range = attackingEntity.getPosition().distance(target.getPosition());
        if (range > 0) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "Target not in range");
        }

        // check elevation
        if ((targetEntity != null) && (targetEntity.getElevation() > 0)) {
            return new ToHitData(TargetRoll.IMPOSSIBLE,
                  "Target elevation not in range");
        }

        // can't physically attack meks making dfa attacks
        if ((targetEntity != null) && targetEntity.isMakingDfa()) {
            return new ToHitData(TargetRoll.IMPOSSIBLE,
                  "Target is making a DFA attack");
        }

        // Can only attack other entities
        if (target.getTargetType() != Targetable.TYPE_ENTITY) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "Invalid attack");
        }

        // Set the base BTH
        int base = attackingEntity.getCrew().getGunnery();

        // Start the To-Hit
        toHit = new ToHitData(base, "base");

        // attacker movement
        toHit.append(Compute.getAttackerMovementModifier(game, attackerId));

        // target movement
        toHit.append(Compute.getTargetMovementModifier(game, targetId));

        // attacker terrain
        toHit.append(Compute.getAttackerTerrainModifier(game, attackerId));

        // target terrain
        toHit.append(Compute.getTargetTerrainModifier(game, targetEntity, 0, inSameBuilding));

        // attacker is spotting
        if (attackingEntity.isSpotting()) {
            toHit.addModifier(+1, "attacker is spotting");
        }

        // taser feedback
        if (attackingEntity.getTaserFeedBackRounds() > 0) {
            toHit.addModifier(1, "Taser feedback");
        }

        // target immobile
        toHit.append(Compute.getImmobileMod(targetEntity));

        toHit.append(nightModifiers(game, target, null, attackingEntity, false));

        Compute.modifyPhysicalBTHForAdvantages(attackingEntity, targetEntity, toHit, game);

        // factor in target side
        toHit.setSideTable(ComputeSideTable.sideTable(attackingEntity, targetEntity));

        // done!
        return toHit;
    }
}
