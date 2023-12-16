/*
 * MegaMek - Copyright (C) 2004 Ben Mazur (bmazur@sev.org)
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

import megamek.client.ui.Messages;
import megamek.common.*;
import megamek.common.options.OptionsConstants;

/**
 * The attacking ProtoMech makes its combo physical attack action.
 */
public class ProtomechPhysicalAttackAction extends AbstractAttackAction {
    private static final long serialVersionUID = 1432011536091665084L;

    public ProtomechPhysicalAttackAction(int entityId, int targetId) {
        super(entityId, targetId);
    }

    public ProtomechPhysicalAttackAction(int entityId, int targetType,
            int targetId) {
        super(entityId, targetType, targetId);
    }

    /**
     * Damage a ProtoMech does with its Combo-physicalattack.
     */
    public static int getDamageFor(Entity entity, Targetable target) {
        int toReturn;
        if ((entity.getWeight() >= 2) && (entity.getWeight() < 6)) {
            toReturn = 1;
        } else if (entity.getWeight() <= 9) {
            toReturn = 2;
        } else {
            toReturn = 3;
        }

        // ProtoMech weapon (TacOps, p. 337) or quad melee system (IO, p. 67)
        if (entity.hasWorkingMisc(MiscType.F_PROTOMECH_MELEE, MiscType.S_PROTO_QMS)) {
            toReturn += (int) Math.ceil(entity.getWeight() / 5.0) * 2;
        } else if (entity.hasWorkingMisc(MiscType.F_PROTOMECH_MELEE)) {
            toReturn += (int) Math.ceil(entity.getWeight() / 5.0);
        }

        if (((Protomech) entity).isEDPCharged() && target.isConventionalInfantry()) {
            toReturn++;
            // TODO: add another +1 to damage if target is cybernetically enhanced
        }

        // underwater damage is half, round up (see bug 1110692)
        if (entity.getLocationStatus(Protomech.LOC_TORSO) == ILocationExposureStatus.WET) {
            toReturn = (int) Math.ceil(toReturn * 0.5f);
        }

        if ((null != entity.getCrew())
                && entity.hasAbility(OptionsConstants.PILOT_MELEE_MASTER)) {
            toReturn *= 2;
        }
        return toReturn;
    }

    public ToHitData toHit(Game game) {
        return toHit(game, getEntityId(), game.getTarget(getTargetType(),
                getTargetId()));
    }

    public static ToHitData toHit(Game game, int attackerId, Targetable target) {
        final Entity ae = game.getEntity(attackerId);
        int targetId = Entity.NONE;
        Entity te = null;
        // arguments legal?
        if ((ae == null) || (target == null)) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "Attacker or target not valid");
        }

        if (target.getTargetType() == Targetable.TYPE_ENTITY) {
            te = (Entity) target;
            targetId = target.getId();
        }

        if (!game.getOptions().booleanOption(OptionsConstants.BASE_FRIENDLY_FIRE)) {
            // a friendly unit can never be the target of a direct attack.
            if ((target.getTargetType() == Targetable.TYPE_ENTITY)
                    && ((((Entity) target).getOwnerId() == ae.getOwnerId())
                            || ((((Entity) target).getOwner().getTeam() != Player.TEAM_NONE)
                                    && (ae.getOwner().getTeam() != Player.TEAM_NONE)
                                    && (ae.getOwner().getTeam() == ((Entity) target).getOwner().getTeam())))) {
                return new ToHitData(TargetRoll.IMPOSSIBLE, "A friendly unit "
                        + "can never be the target of a direct attack.");
            }
        }

        final Hex attHex = game.getBoard().getHex(ae.getPosition());
        final Hex targHex = game.getBoard().getHex(target.getPosition());
        if ((attHex == null) || (targHex == null)) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "off board");
        }
        final int attackerElevation = ae.getElevation() + attHex.getLevel();
        final int targetHeight = target.relHeight() + targHex.getLevel();
        final int targetElevation = target.getElevation() + targHex.getLevel();

        boolean inSameBuilding = Compute.isInSameBuilding(game, ae, te);

        ToHitData toHit;

        // can't target yourself
        if (ae.equals(te)) {
            return new ToHitData(TargetRoll.IMPOSSIBLE,
                    "You can't target yourself");
        }

        // non-protos can't make protomech-physicalattacks
        if (!(ae instanceof Protomech)) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "Non-protos can't make proto-physicalattacks");
        }

        // Can't target a transported entity.
        if ((te != null) && (Entity.NONE != te.getTransportId())) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "Target is a passenger.");
        }

        // Can't target a entity conducting a swarm attack.
        if ((te != null) && (Entity.NONE != te.getSwarmTargetId())) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "Target is swarming a Mek.");
        }

        // check range
        final int range = ae.getPosition().distance(target.getPosition());
        if (range != 0) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "Target must be in same hex");
        }

        // check elevation
        if ((attackerElevation < targetElevation) || (attackerElevation > targetHeight)) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "Target elevation not in range");
        }

        // can't physically attack mechs making dfa attacks
        if ((te != null) && te.isMakingDfa()) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "Target is making a DFA attack");
        }

        // Can't target woods or ignite a building with a physical.
        if ((target.getTargetType() == Targetable.TYPE_BLDG_IGNITE)
                || (target.getTargetType() == Targetable.TYPE_HEX_CLEAR)
                || (target.getTargetType() == Targetable.TYPE_HEX_IGNITE)) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "Invalid attack");
        }

        // Set the base BTH
        int base = 4;

        // Start the To-Hit
        toHit = new ToHitData(base, "base");

        // attacker movement
        toHit.append(Compute.getAttackerMovementModifier(game, attackerId));

        // target movement
        if (targetId != Entity.NONE) {
            toHit.append(Compute.getTargetMovementModifier(game, targetId));
        }

        // attacker terrain
        toHit.append(Compute.getAttackerTerrainModifier(game, attackerId));

        // target terrain
        if (te != null) {
            toHit.append(Compute.getTargetTerrainModifier(game, te, 0, inSameBuilding));
        }

        // target prone
        if ((te != null) && te.isProne()) {
            toHit.addModifier(-2, "target prone and adjacent");
        }

        // target immobile
        if (te != null) {
            toHit.append(Compute.getImmobileMod(te));
        }

        toHit.append(nightModifiers(game, target, null, ae, false));

        // te can be null for this
        Compute.modifyPhysicalBTHForAdvantages(ae, te, toHit, game);

        // Standing 'mechs use kick table
        if ((te instanceof Mech) && !te.isProne()) {
            toHit.setHitTable(ToHitData.HIT_KICK);
        } // Everything else uses the standard table, which is default

        // done!
        return toHit;
    }

    @Override
    public String toSummaryString(final Game game) {
        final String roll = this.toHit(game).getValueAsString();
        return Messages.getString("BoardView1.ProtomechPhysicalAttackAction", roll);
    }
}
