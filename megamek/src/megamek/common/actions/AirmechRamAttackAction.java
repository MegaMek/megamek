/*
 * MegaMek - Copyright (C) 2017 - The MegaMek Team
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 */
package megamek.common.actions;

import megamek.common.*;
import megamek.common.MovePath.MoveStepType;
import megamek.common.options.OptionsConstants;

import java.util.Enumeration;

/**
 * Ram attack by an airborne LAM in airmech mode. This is treated like a charge in the movement path,
 * but has significant difference in the way damage is calculated and in the final locations.
 * 
 * @author Neoancient
 */
public class AirmechRamAttackAction extends DisplacementAttackAction {
    private static final long serialVersionUID = 5110608317218688433L;

    public AirmechRamAttackAction(Entity attacker, Targetable target) {
        this(attacker.getId(), target.getTargetType(), target.getId(), target.getPosition());
    }

    public AirmechRamAttackAction(int entityId, int targetType, int targetId, Coords targetPos) {
        super(entityId, targetType, targetId, targetPos);
    }

    /**
     * To-hit number for a ram, assuming that movement has been handled
     *
     * @param game The current {@link Game}
     */
    public ToHitData toHit(Game game) {
        final Entity entity = game.getEntity(getEntityId());
        return toHit(game, game.getTarget(getTargetType(), getTargetId()),
                     entity.getPosition(), entity.getElevation(), entity.moved);
    }

    /**
     * To-hit number for a ram, assuming that movement has been handled
     *
     * @param game The current {@link Game}
     */
    public ToHitData toHit(Game game, Targetable target, Coords src,
                           int elevation, EntityMovementType movement) {
        final Entity ae = getEntity(game);

        // arguments legal?
        if (ae == null) {
            throw new IllegalStateException("Attacker is null");
        }

        // Due to pretreatment of physical attacks, the target may be null.
        if (target == null) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "Target is null");
        }
        
        if (!(ae instanceof LandAirMech) || !ae.isAirborneVTOLorWIGE()) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "Attacker is not airborne airmech");
        }

        int targetId = Entity.NONE;
        Entity te = null;
        if (target.getTargetType() == Targetable.TYPE_ENTITY) {
            te = (Entity) target;
            targetId = target.getId();
        } else {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "Invalid Target");
        }
        
        if (!game.getOptions().booleanOption(OptionsConstants.BASE_FRIENDLY_FIRE)) {
            // a friendly unit can never be the target of a direct attack.
            if ((target.getTargetType() == Targetable.TYPE_ENTITY)
                && ((((Entity) target).getOwnerId() == ae.getOwnerId())
                        || ((((Entity) target).getOwner().getTeam() != Player.TEAM_NONE)
                                && (ae.getOwner().getTeam() != Player.TEAM_NONE)
                                && (ae.getOwner().getTeam() == ((Entity) target).getOwner().getTeam())))) {
                return new ToHitData(TargetRoll.IMPOSSIBLE,
                                     "A friendly unit can never be the target of a direct attack.");
            }
        }

        Hex targHex = game.getBoard().getHex(target.getPosition());
        // we should not be using the attacker's hex here since the attacker
        // will end up in
        // the target's hex
        final int attackerElevation = elevation + targHex.getLevel();
        final int attackerHeight = attackerElevation + ae.height();
        final int targetElevation = target.getElevation()
                                    + targHex.getLevel();
        final int targetHeight = targetElevation + target.getHeight();
        Building bldg = game.getBoard().getBuildingAt(getTargetPos());
        ToHitData toHit = null;
        boolean targIsBuilding = ((getTargetType() == Targetable.TYPE_FUEL_TANK)
                || (getTargetType() == Targetable.TYPE_BUILDING));

        boolean inSameBuilding = Compute.isInSameBuilding(game, ae, te);

        // can't target yourself
        if (ae.equals(te)) {
            return new ToHitData(TargetRoll.IMPOSSIBLE,
                                 "You can't target yourself");
        }

        // Can't target a transported entity.
        if (Entity.NONE != te.getTransportId()) {
            return new ToHitData(TargetRoll.IMPOSSIBLE,
                                 "Target is a passenger.");
        }

        // Can't target a entity conducting a swarm attack.
        if (Entity.NONE != te.getSwarmTargetId()) {
            return new ToHitData(TargetRoll.IMPOSSIBLE,
                                 "Target is swarming a Mek.");
        }
        
        // Cannot target infantry
        if (te instanceof Infantry) {
            return new ToHitData(TargetRoll.IMPOSSIBLE,
                    "Target is infantry.");
        }

        // Cannot target protomech
        if (te instanceof Protomech) {
            return new ToHitData(TargetRoll.IMPOSSIBLE,
                    "Target is protomech.");
        }

        // check range
        if (src.distance(target.getPosition()) > 1) {
            if (null != te.getSecondaryPositions()) {
                boolean inSecondaryRange = false;
                for (int i : te.getSecondaryPositions().keySet()) {
                    if (null != te.getSecondaryPositions().get(i)) {
                        if (src.distance(te.getSecondaryPositions().get(i)) < 2) {
                            inSecondaryRange = true;
                            break;
                        }
                    }
                }
                if (!inSecondaryRange) {
                    return new ToHitData(TargetRoll.IMPOSSIBLE,
                                         "Target not in range");
                }
            } else {
                return new ToHitData(TargetRoll.IMPOSSIBLE,
                                     "Target not in range");
            }
        }

        // target must be within 1 elevation level
        if ((attackerElevation > targetHeight)
            || (attackerHeight < targetElevation)) {
            return new ToHitData(TargetRoll.IMPOSSIBLE,
                                 "Target must be within 1 elevation level");
        }

        // can't attack mech making a different displacement attack
        if (te.hasDisplacementAttack()) {
            return new ToHitData(TargetRoll.IMPOSSIBLE,
                                 "Target is already making a charge/DFA attack");
        }

        // target must have moved already
        // errata: immobile units can be targeted, even when they haven't moved
        // yet
        if (!te.isDone() && !te.isImmobile()) {
            return new ToHitData(TargetRoll.IMPOSSIBLE,
                                 "Target must be done with movement");
        }

        // can't attack the target of another displacement attack
        if (te.isTargetOfDisplacementAttack()
            && (te.findTargetedDisplacement().getEntityId() != ae.getId())) {
            return new ToHitData(TargetRoll.IMPOSSIBLE,
                                 "Target is the target of another charge/DFA");
        }

        // Can't target units in buildings (from the outside).
        if ((null != bldg) && (!targIsBuilding)
            && Compute.isInBuilding(game, te)) {
            if (!Compute.isInBuilding(game, ae)) {
                return new ToHitData(TargetRoll.IMPOSSIBLE,
                                     "Target is inside building");
            } else if (!game.getBoard().getBuildingAt(ae.getPosition())
                            .equals(bldg)) {
                return new ToHitData(TargetRoll.IMPOSSIBLE,
                                     "Target is inside differnt building");
            }
        }

        // Attacks against adjacent buildings automatically hit.
        if ((target.getTargetType() == Targetable.TYPE_BUILDING)
            || (target.getTargetType() == Targetable.TYPE_FUEL_TANK)
            || (target instanceof GunEmplacement)) {
            return new ToHitData(TargetRoll.AUTOMATIC_SUCCESS,
                                 "Targeting adjacent building.");
        }

        // Can't target woods or ignite a building with a physical.
        if ((target.getTargetType() == Targetable.TYPE_BLDG_IGNITE)
            || (target.getTargetType() == Targetable.TYPE_HEX_CLEAR)
            || (target.getTargetType() == Targetable.TYPE_HEX_IGNITE)) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "Invalid attack");
        }

        toHit = new ToHitData(5, "base");

        // attacker movement
        toHit.append(Compute.getAttackerMovementModifier(game, ae.getId(),
                                                         movement));

        // target movement
        toHit.append(Compute.getTargetMovementModifier(game, targetId));

        // attacker terrain
        toHit.append(Compute.getAttackerTerrainModifier(game, ae.getId()));

        // target terrain
        toHit.append(Compute.getTargetTerrainModifier(game, te, 0,
                                                      inSameBuilding));

        // attacker is spotting
        if (ae.isSpotting() && !ae.getCrew().hasActiveCommandConsole()) {
            toHit.addModifier(+1, "attacker is spotting");
        }
        // piloting skill differential
        if (ae.getCrew().getPiloting() != te.getCrew().getPiloting()) {
            toHit.addModifier(ae.getCrew().getPiloting()
                              - te.getCrew().getPiloting(), "piloting skill differential");
        }

        // target prone
        if (te.isProne()) {
            toHit.addModifier(-2, "target prone and adjacent");
        }

        // water partial cover?
        if ((te.height() > 0) && (te.getElevation() == -1)
            && (targHex.terrainLevel(Terrains.WATER) == te.height())) {
            toHit.addModifier(1, "target has partial cover");
        }

        // target immobile
        toHit.append(Compute.getImmobileMod(te));

        Compute.modifyPhysicalBTHForAdvantages(ae, te, toHit, game);

        // evading bonuses (
        if (te.isEvading()) {
            toHit.addModifier(te.getEvasionBonus(), "target is evading");
        }

        // determine hit direction
        toHit.setSideTable(te.sideTable(src));

        // all rams resolved against full-body table, against mechs in water partial cover
        if ((targHex.terrainLevel(Terrains.WATER) == te.height())
            && (te.getElevation() == -1) && (te.height() > 0)) {
            toHit.setHitTable(ToHitData.HIT_PUNCH);
        } else {
            toHit.setHitTable(ToHitData.HIT_NORMAL);
        }

        // What to do with grounded dropships? Awaiting rules clarification, but
        // until then, we will assume that if the attacker height is less than
        // half
        // the target elevation, then use HIT_PUNCH, otherwise HIT_NORMAL
        // See Dropship.rollHitLocation to see how HIT_PUNCH is handled
        if (target instanceof Dropship) {
            if ((attackerHeight - targetElevation) > (target.getHeight() / 2)) {
                toHit.setHitTable(ToHitData.HIT_NORMAL);
            } else {
                toHit.setHitTable(ToHitData.HIT_PUNCH);
            }
        }

        // done!
        return toHit;
    }

    /**
     * Checks if a ram can hit the target, taking account of movement
     */
    public ToHitData toHit(Game game, MovePath md) {
        final Entity ae = game.getEntity(getEntityId());
        final Targetable target = getTarget(game);
        Coords ramSrc = ae.getPosition();
        int ramEl = ae.getElevation();
        MoveStep ramStep = null;

        // let's just check this
        if (!md.contains(MoveStepType.CHARGE)) {
            return new ToHitData(TargetRoll.IMPOSSIBLE,
                                 "Ram action not found in movement path");
        }

        // no evading
        if (md.contains(MoveStepType.EVADE)) {
            return new ToHitData(TargetRoll.IMPOSSIBLE,
                                 "No evading while charging");
        }

        // determine last valid step
        md.compile(game, ae);
        for (final Enumeration<MoveStep> i = md.getSteps(); i.hasMoreElements(); ) {
            final MoveStep step = i.nextElement();
            if (step.getMovementType(md.isEndStep(step)) == EntityMovementType.MOVE_ILLEGAL) {
                break;
            }
            if (step.getType() == MoveStepType.CHARGE) {
                ramStep = step;
            } else {
                ramSrc = step.getPosition();
                ramEl = step.getElevation();
            }
        }

        // need to reach target
        boolean isReachable = false;
        if ((ramStep != null)) {
            isReachable = target.getPosition().equals(ramStep.getPosition());
            if (!isReachable && (target instanceof Entity)
                && (null != ((Entity) target).getSecondaryPositions())) {
                for (int i : ((Entity) target).getSecondaryPositions().keySet()) {
                    if (null != ((Entity) target).getSecondaryPositions()
                                                 .get(i)) {
                        isReachable = ((Entity) target).getSecondaryPositions()
                                                       .get(i).equals(ramStep.getPosition());
                        if (isReachable) {
                            break;
                        }
                    }
                }
            }
        }
        if (!isReachable) {
            return new ToHitData(TargetRoll.IMPOSSIBLE,
                                 "Could not reach target with movement");
        }

        if (!md.getSecondLastStep().isLegalEndPos()) {
            return new ToHitData(TargetRoll.IMPOSSIBLE,
                                 "Violation of stacking limit in second last step");
        }

        return toHit(
                game,
                target,
                ramSrc,
                ramEl,
                ramStep.getMovementType(true));
    }

    /**
     * Damage that an airmech does with a successful ram. Assumes that
     * delta_distance is correct.
     */
    public static int getDamageFor(Entity entity) {
        return AirmechRamAttackAction.getDamageFor(entity, entity.delta_distance);
    }

    public static int getDamageFor(Entity entity, int hexesMoved) {
        if (hexesMoved == 0) {
            hexesMoved = 1;
        }
        return (int) Math
                .ceil((entity.getWeight() / 5.0)
                        * (hexesMoved - 1)
                        * (entity.getLocationStatus(1) == ILocationExposureStatus.WET ? 0.5
                                : 1));
    }

    public static int getDamageTakenBy(Entity entity, Targetable target) {
        return AirmechRamAttackAction.getDamageTakenBy(entity, target, entity.delta_distance);
    }

    public static int getDamageTakenBy(Entity entity, Targetable target, int distance) {
        if (distance == 0) {
            distance = 1;
        }
        double weight = entity.getWeight();
        if (target.getTargetType() == Targetable.TYPE_ENTITY) {
            weight = ((Entity) target).getWeight();
        }
        return (int) Math
                .ceil((weight / 10.0) * (distance - 1)
                      * (entity.getLocationStatus(1) == ILocationExposureStatus.WET ? 0.5 : 1));
    }

}
