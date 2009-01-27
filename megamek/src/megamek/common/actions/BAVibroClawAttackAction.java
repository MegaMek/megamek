/*
 * MegaMek - Copyright (C) 2008 Ben Mazur (bmazur@sev.org)
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

package megamek.common.actions;

import megamek.common.BattleArmor;
import megamek.common.Compute;
import megamek.common.Entity;
import megamek.common.IGame;
import megamek.common.IHex;
import megamek.common.Infantry;
import megamek.common.Player;
import megamek.common.TargetRoll;
import megamek.common.Targetable;
import megamek.common.ToHitData;

/**
 * A BattleArmor uses its vibroclaws
 */
public class BAVibroClawAttackAction extends AbstractAttackAction {

    /**
     *
     */
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
        return Compute.missilesHit(((BattleArmor)entity).getShootingStrength()) * entity.getVibroClaws();
    }

    public ToHitData toHit(IGame game) {
        return toHit(game, getEntityId(), game.getTarget(getTargetType(),
                getTargetId()));
    }

    public static ToHitData toHit(IGame game, int attackerId, Targetable target) {
        final Entity ae = game.getEntity(attackerId);
        int targetId = Entity.NONE;
        Entity te = null;
        // arguments legal?
        if ((ae == null) || (target == null)) {
            throw new IllegalArgumentException("Attacker or target not valid");
        }

        if (target.getTargetType() == Targetable.TYPE_ENTITY) {
            te = (Entity) target;
            targetId = target.getTargetId();
        }

        if (!game.getOptions().booleanOption("friendly_fire")) {
            // a friendly unit can never be the target of a direct attack.
            if ((target.getTargetType() == Targetable.TYPE_ENTITY)
                    && ((((Entity)target).getOwnerId() == ae.getOwnerId())
                            || ((((Entity)target).getOwner().getTeam() != Player.TEAM_NONE)
                                    && (ae.getOwner().getTeam() != Player.TEAM_NONE)
                                    && (ae.getOwner().getTeam() == ((Entity)target).getOwner().getTeam())))) {
                return new ToHitData(TargetRoll.IMPOSSIBLE, "A friendly unit can never be the target of a direct attack.");
            }
        }

        final IHex attHex = game.getBoard().getHex(ae.getPosition());
        final IHex targHex = game.getBoard().getHex(target.getPosition());
        if ((attHex == null) || (targHex == null)) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "off board");
        }
        boolean inSameBuilding = (te != null) && (game.getBoard().getBuildingAt(ae.getPosition()) != null)
            && game.getBoard().getBuildingAt(ae.getPosition()).equals(game.getBoard().getBuildingAt(te.getPosition()));

        ToHitData toHit;

        // can't target yourself
        if ((te != null) && ae.equals(te)) {
            return new ToHitData(TargetRoll.IMPOSSIBLE,
                    "You can't target yourself");
        }

        // only BA can make this attack
        if (!(ae instanceof BattleArmor)) {
            return new ToHitData(TargetRoll.IMPOSSIBLE,
                    "Non-BA can't make vibroclaw-physicalattacks");
        }

        if ((te != null) && !((te instanceof Infantry))) {
            return new ToHitData(TargetRoll.IMPOSSIBLE,
                    "can't target non-infantry");
        }

        // need to have vibroclaws to make this attack
        if (ae.getVibroClaws() == 0) {
            return new ToHitData(TargetRoll.IMPOSSIBLE,
                    "no vibro claws mounted");
        }

        // Can't target a transported entity.
        if ((te != null) && (Entity.NONE != te.getTransportId())) {
            return new ToHitData(TargetRoll.IMPOSSIBLE,
                    "Target is a passenger.");
        }

        // Can't target a entity conducting a swarm attack.
        if ((te != null) && (Entity.NONE != te.getSwarmTargetId())) {
            return new ToHitData(TargetRoll.IMPOSSIBLE,
                    "Target is swarming a Mek.");
        }

        // check range
        final int range = ae.getPosition().distance(target.getPosition());
        if (range > 0) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "Target not in range");
        }

        // check elevation
        if ((te != null) && (te.getElevation() > 0)) {
            return new ToHitData(TargetRoll.IMPOSSIBLE,
                    "Target elevation not in range");
        }

        // can't physically attack mechs making dfa attacks
        if ((te != null) && te.isMakingDfa()) {
            return new ToHitData(TargetRoll.IMPOSSIBLE,
                    "Target is making a DFA attack");
        }

        // Can't target woods or ignite a building with a physical.
        if ((target.getTargetType() == Targetable.TYPE_BLDG_IGNITE)
                || (target.getTargetType() == Targetable.TYPE_HEX_CLEAR)
                || (target.getTargetType() == Targetable.TYPE_HEX_IGNITE)) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "Invalid attack");
        }

        // Set the base BTH
        int base = ae.getCrew().getGunnery();

        // Start the To-Hit
        toHit = new ToHitData(base, "base");

        // attacker movement
        toHit.append(Compute.getAttackerMovementModifier(game, attackerId));

        // target movement
        toHit.append(Compute.getTargetMovementModifier(game, targetId));

        // attacker terrain
        toHit.append(Compute.getAttackerTerrainModifier(game, attackerId));

        // target terrain
        toHit.append(Compute.getTargetTerrainModifier(game, te, 0, inSameBuilding));

        // attacker is spotting
        if (ae.isSpotting()) {
            toHit.addModifier(+1, "attacker is spotting");
        }

        // taser feedback
        if (ae.getTaserFeedBackRounds() > 0) {
            toHit.addModifier(1, "Taser feedback");
        }

        // target immobile
        toHit.append(Compute.getImmobileMod(te));

        toHit.append(nightModifiers(game, target, null, ae, false));

        Compute.modifyPhysicalBTHForAdvantages(ae, te, toHit, game);

        // factor in target side
        toHit.setSideTable(Compute.targetSideTable(ae, te));

        // done!
        return toHit;
    }
}