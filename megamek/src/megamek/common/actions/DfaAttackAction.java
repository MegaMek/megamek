/*
 * MegaMek - Copyright (C) 2000,2001,2002,2003,2004 Ben Mazur (bmazur@sev.org)
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

/*
 * DfaAttackAction.java
 *
 * Created on March 16, 2002, 11:43 AM
 */

package megamek.common.actions;

import java.util.Enumeration;

import megamek.common.BattleArmor;
import megamek.common.BipedMech;
import megamek.common.Compute;
import megamek.common.Coords;
import megamek.common.CriticalSlot;
import megamek.common.Dropship;
import megamek.common.Entity;
import megamek.common.EntityMovementType;
import megamek.common.EntityWeightClass;
import megamek.common.GunEmplacement;
import megamek.common.IGame;
import megamek.common.IPlayer;
import megamek.common.Infantry;
import megamek.common.Mech;
import megamek.common.MiscType;
import megamek.common.MovePath;
import megamek.common.MovePath.MoveStepType;
import megamek.common.MoveStep;
import megamek.common.Tank;
import megamek.common.TargetRoll;
import megamek.common.Targetable;
import megamek.common.ToHitData;
import megamek.common.options.OptionsConstants;

/**
 * @author Ben
 */
public class DfaAttackAction extends DisplacementAttackAction {

    /**
     *
     */
    private static final long serialVersionUID = 3953889779582616903L;

    /**
     * Creates new DfaAttackAction
     */
    public DfaAttackAction(int entityId, int targetId, Coords targetPos) {
        super(entityId, targetId, targetPos);
    }

    public DfaAttackAction(int entityId, int targetType, int targetId,
                           Coords targetPos) {
        super(entityId, targetType, targetId, targetPos);
    }

    /**
     * Damage done to a mech after a successful DFA.
     */
    public static int getDamageTakenBy(Entity entity) {
        return (int) Math.ceil(entity.getWeight() / 5.0);
    }

    /**
     * Damage that a mech does with a successful DFA.
     */
    public static int getDamageFor(Entity entity, boolean targetInfantry) {
        int toReturn = (int) Math.ceil((entity.getWeight() / 10.0) * 3.0);

        if (DfaAttackAction.hasTalons(entity)) {
            toReturn *= 1.5;
        }

        if (targetInfantry) {
            toReturn = Math.max(1, toReturn / 10);
        }
        return toReturn;

    }

    /**
     * Checks if a death from above attack can hit the target, including
     * movement
     */
    public static ToHitData toHit(IGame game, int attackerId,
                                  Targetable target, MovePath md) {
        final Entity ae = game.getEntity(attackerId);

        // Do to pretreatment of physical attacks, the target may be null.
        if (target == null) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "Target is null");
        }

        Entity te = null;
        if (target.getTargetType() == Targetable.TYPE_ENTITY) {
            te = (Entity) target;
        }
        Coords chargeSrc = ae.getPosition();
        MoveStep chargeStep = null;

        // Infantry CAN'T dfa!!!
        if (ae instanceof Infantry) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "Infantry can't D.F.A.");
        }

        if (ae.getJumpType() == Mech.JUMP_BOOSTER) {
            return new ToHitData(TargetRoll.IMPOSSIBLE,
                                 "Can't D.F.A. using mechanical jump boosters.");
        }

        // let's just check this
        if (!md.contains(MoveStepType.DFA)) {
            return new ToHitData(TargetRoll.IMPOSSIBLE,
                                 "D.F.A. action not found in movement path");
        }

        // have to jump
        if (!md.contains(MoveStepType.START_JUMP)) {
            return new ToHitData(TargetRoll.IMPOSSIBLE,
                                 "D.F.A. must involve jumping");
        }

        // can't target airborne units
        if ((te != null) && te.isAirborne()) {
            return new ToHitData(TargetRoll.IMPOSSIBLE,
                                 "Cannot D.F.A. an airborne target.");
        }

        // can't target dropships
        if ((te != null) && (te instanceof Dropship)) {
            return new ToHitData(TargetRoll.IMPOSSIBLE,
                                 "Cannot D.F.A. a dropship.");
        }

        // Can't target a transported entity.
        if ((te != null) && (Entity.NONE != te.getTransportId())) {
            return new ToHitData(TargetRoll.IMPOSSIBLE,
                                 "Target is a passenger.");
        }

        // no evading
        if (md.contains(MoveStepType.EVADE)) {
            return new ToHitData(TargetRoll.IMPOSSIBLE,
                                 "No evading while charging");
        }

        // Can't target a entity conducting a swarm attack.
        if ((te != null) && (Entity.NONE != te.getSwarmTargetId())) {
            return new ToHitData(TargetRoll.IMPOSSIBLE,
                                 "Target is swarming a Mek.");
        }

        // determine last valid step
        md.compile(game, ae);
        for (final Enumeration<MoveStep> i = md.getSteps(); i.hasMoreElements(); ) {
            final MoveStep step = i.nextElement();
            if (!step.isLegal(md)) {
                break;
            }
            if (step.getType() == MoveStepType.DFA) {
                chargeStep = step;
            } else {
                chargeSrc = step.getPosition();
            }
        }

        // need to reach target
        if ((chargeStep == null)
            || !target.getPosition().equals(chargeStep.getPosition())) {
            return new ToHitData(TargetRoll.IMPOSSIBLE,
                                 "Could not reach target with movement");
        }

        // target must have moved already, unless it's immobile
        if ((te != null) && (!te.isDone() && !te.isImmobile())) {
            return new ToHitData(TargetRoll.IMPOSSIBLE,
                                 "Target must be done with movement");
        }

        return DfaAttackAction.toHit(game, attackerId, target, chargeSrc);
    }

    public ToHitData toHit(IGame game) {
        final Entity entity = game.getEntity(getEntityId());
        return DfaAttackAction.toHit(game, getEntityId(),
                                     game.getTarget(getTargetType(), getTargetId()),
                                     entity.getPosition());
    }

    /**
     * To-hit number for a death from above attack, assuming that movement has
     * been handled
     */
    public static ToHitData toHit(IGame game, int attackerId,
                                  Targetable target, Coords src) {
        final Entity ae = game.getEntity(attackerId);

        // arguments legal?
        if (ae == null) {
            throw new IllegalArgumentException("Attacker is null");
        }

        // Do to pretreatment of physical attacks, the target may be null.
        if (target == null) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "Target is null");
        }

        int targetId = Entity.NONE;
        Entity te = null;
        if (target.getTargetType() == Targetable.TYPE_ENTITY) {
            te = (Entity) target;
            targetId = target.getTargetId();
        } else {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "Invalid Target");
        }

        if (!game.getOptions().booleanOption(OptionsConstants.BASE_FRIENDLY_FIRE)) {
            // a friendly unit can never be the target of a direct attack.
            if ((target.getTargetType() == Targetable.TYPE_ENTITY)
                && ((((Entity) target).getOwnerId() == ae.getOwnerId()) || ((((Entity) target)
                                                                                     .getOwner().getTeam() != IPlayer
                                                                                     .TEAM_NONE)
                                                                            && (ae.getOwner().getTeam() != IPlayer
                    .TEAM_NONE) && (ae
                                                                                                                                          .getOwner().getTeam() == ((Entity) target)
                                                                                                                                          .getOwner().getTeam())))) {
                return new ToHitData(TargetRoll.IMPOSSIBLE,
                                     "A friendly unit can never be the target of a direct attack.");
            }
        }

        final boolean targetInBuilding = Compute.isInBuilding(game, te);
        ToHitData toHit = null;

        final int attackerElevation = ae.getElevation()
                                      + game.getBoard().getHex(ae.getPosition()).getLevel();
        final int targetElevation = target.getElevation()
                                    + game.getBoard().getHex(target.getPosition()).getLevel();
        final int attackerHeight = attackerElevation + ae.getHeight();

        // check elevation of target flying VTOL
        if (target.isAirborneVTOLorWIGE()) {
            if ((targetElevation - attackerHeight) > ae.getJumpMP()) {
                return new ToHitData(TargetRoll.IMPOSSIBLE,
                                     "Elevation difference to high");
            }
        }

        // can't target yourself
        if (ae.equals(te)) {
            return new ToHitData(TargetRoll.IMPOSSIBLE,
                                 "You can't target yourself");
        }

        // Infantry CAN'T dfa!!!
        if (ae instanceof Infantry) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "Infantry can't dfa");
        }

        // Can't target a transported entity.
        if ((Entity.NONE != te.getTransportId())) {
            return new ToHitData(TargetRoll.IMPOSSIBLE,
                                 "Target is a passenger.");
        }

        // Can't target a entity conducting a swarm attack.
        if ((Entity.NONE != te.getSwarmTargetId())) {
            return new ToHitData(TargetRoll.IMPOSSIBLE,
                                 "Target is swarming a Mek.");
        }

        // check range
        if (src.distance(target.getPosition()) > 1) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "Target not in range");
        }

        // can't dfa while prone, even if you somehow did manage to jump
        if (ae.isProne()) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "Attacker is prone");
        }

        // can't attack mech making a different displacement attack
        if (te.hasDisplacementAttack()) {
            return new ToHitData(TargetRoll.IMPOSSIBLE,
                                 "Target is already making a charge/DFA attack");
        }

        // can't attack the target of another displacement attack
        if (te.isTargetOfDisplacementAttack()
            && (te.findTargetedDisplacement().getEntityId() != ae.getId())) {
            return new ToHitData(TargetRoll.IMPOSSIBLE,
                                 "Target is the target of another charge/DFA");
        }

        // Can't target units in buildings (from the outside).
        if (targetInBuilding) {
            return new ToHitData(TargetRoll.IMPOSSIBLE,
                                 "Target is inside building");
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

        // Set the base BTH
        int base = ae.getCrew().getPiloting();

        toHit = new ToHitData(base, "base");

        // BMR(r), page 33. +3 modifier for DFA on infantry.
        if (te instanceof Infantry) {
            toHit.addModifier(3, "Infantry target");
        }

        // Battle Armor targets are hard for Meks and Tanks to hit.
        if (te instanceof BattleArmor) {
            toHit.addModifier(1, "battle armor target");
        }

        if ((ae instanceof Mech) && ((Mech) ae).isSuperHeavy()) {
            toHit.addModifier(1, "attacker is superheavy mech");
        }

        // attacker movement
        toHit.append(Compute.getAttackerMovementModifier(game, attackerId,
                                                         EntityMovementType.MOVE_JUMP));

        // target movement
        toHit.append(Compute.getTargetMovementModifier(game, targetId));

        // piloting skill differential
        if ((ae.getCrew().getPiloting() != te.getCrew().getPiloting())) {
            toHit.addModifier(ae.getCrew().getPiloting()
                              - te.getCrew().getPiloting(), "piloting skill differential");
        }

        // attacker is spotting
        if (ae.isSpotting()) {
            toHit.addModifier(+1, "attacker is spotting");
        }

        // target prone
        if (te.isProne()) {
            toHit.addModifier(-2, "target prone and adjacent");
        }

        // If it has a torso-mounted cockpit and two head sensor hits or three
        // sensor hits...
        // It gets a =4 penalty for being blind!
        if ((ae instanceof Mech)
            && (((Mech) ae).getCockpitType() == Mech.COCKPIT_TORSO_MOUNTED)) {
            int sensorHits = ae.getBadCriticals(CriticalSlot.TYPE_SYSTEM,
                                                Mech.SYSTEM_SENSORS, Mech.LOC_HEAD);
            int sensorHits2 = ae.getBadCriticals(CriticalSlot.TYPE_SYSTEM,
                                                 Mech.SYSTEM_SENSORS, Mech.LOC_CT);
            if ((sensorHits + sensorHits2) == 3) {
                return new ToHitData(TargetRoll.IMPOSSIBLE,
                                     "Sensors Completely Destroyed for Torso-Mounted Cockpit");
            } else if (sensorHits == 2) {
                toHit.addModifier(4,
                                  "Head Sensors Destroyed for Torso-Mounted Cockpit");
            }
        }

        // target immobile
        toHit.append(Compute.getImmobileMod(te));

        toHit.append(AbstractAttackAction.nightModifiers(game, target, null, ae, false));

        Compute.modifyPhysicalBTHForAdvantages(ae, te, toHit, game);

        // evading bonuses (
        if (te.isEvading()) {
            toHit.addModifier(te.getEvasionBonus(), "target is evading");
        }

        if (te instanceof Tank) {
            toHit.setSideTable(ToHitData.SIDE_FRONT);
            toHit.setHitTable(ToHitData.HIT_NORMAL);
        } else if (te.isProne()) {
            toHit.setSideTable(ToHitData.SIDE_REAR);
            toHit.setHitTable(ToHitData.HIT_NORMAL);
        } else {
            toHit.setSideTable(te.sideTable(src));
            toHit.setHitTable(ToHitData.HIT_PUNCH);
        }
        // Attacking Weight Class Modifier.
        if (game.getOptions().booleanOption(OptionsConstants.ADVGRNDMOV_TACOPS_PHYSICAL_ATTACK_PSR)) {
            if (ae.getWeightClass() == EntityWeightClass.WEIGHT_LIGHT) {
                toHit.addModifier(-2, "Weight Class Attack Modifier");
            } else if (ae.getWeightClass() == EntityWeightClass.WEIGHT_MEDIUM) {
                toHit.addModifier(-1, "Weight Class Attack Modifier");
            }
        }

        if ((ae instanceof Mech) && ((Mech) ae).hasIndustrialTSM()) {
            toHit.addModifier(2, "industrial TSM");
        }

        // done!
        return toHit;
    }

    public static boolean hasTalons(Entity entity) {

        if (entity instanceof Mech) {

            if (entity instanceof BipedMech) {

                return (entity.hasWorkingMisc(MiscType.F_TALON, -1,
                                              Mech.LOC_RLEG) && entity.hasWorkingSystem(
                        Mech.ACTUATOR_FOOT, Mech.LOC_RLEG))
                       || (entity.hasWorkingMisc(MiscType.F_TALON, -1,
                                                 Mech.LOC_LLEG) && entity.hasWorkingSystem(
                        Mech.ACTUATOR_FOOT, Mech.LOC_LLEG));
            }
            return (entity.hasWorkingMisc(MiscType.F_TALON, -1, Mech.LOC_RLEG) && entity
                    .hasWorkingSystem(Mech.ACTUATOR_FOOT, Mech.LOC_RLEG))
                   || (entity.hasWorkingMisc(MiscType.F_TALON, -1,
                                             Mech.LOC_LLEG) && entity.hasWorkingSystem(
                    Mech.ACTUATOR_FOOT, Mech.LOC_LLEG))
                   || ((entity.hasWorkingMisc(MiscType.F_TALON, -1,
                                              Mech.LOC_RARM)) && (entity.hasWorkingSystem(
                    Mech.ACTUATOR_FOOT, Mech.LOC_RARM) || (entity
                                                                   .hasWorkingMisc(MiscType.F_TALON, -1, Mech.LOC_LARM) && entity
                                                                   .hasWorkingSystem(Mech.ACTUATOR_FOOT, Mech.LOC_LARM))));
        }

        return false;
    }
}
