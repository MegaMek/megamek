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
import java.util.ListIterator;

import megamek.client.ui.Messages;
import megamek.common.CriticalSlot;
import megamek.common.Player;
import megamek.common.ToHitData;
import megamek.common.annotations.Nullable;
import megamek.common.battleArmor.BattleArmor;
import megamek.common.board.Coords;
import megamek.common.compute.Compute;
import megamek.common.enums.MoveStepType;
import megamek.common.equipment.GunEmplacement;
import megamek.common.equipment.MiscType;
import megamek.common.game.Game;
import megamek.common.moves.MovePath;
import megamek.common.moves.MoveStep;
import megamek.common.options.OptionsConstants;
import megamek.common.rolls.TargetRoll;
import megamek.common.units.BipedMek;
import megamek.common.units.Dropship;
import megamek.common.units.Entity;
import megamek.common.units.EntityMovementType;
import megamek.common.units.EntityWeightClass;
import megamek.common.units.Infantry;
import megamek.common.units.Mek;
import megamek.common.units.Tank;
import megamek.common.units.Targetable;

/**
 * @author Ben
 * @since March 16, 2002, 11:43 AM
 */
public class DfaAttackAction extends DisplacementAttackAction {
    @Serial
    private static final long serialVersionUID = 3953889779582616903L;

    /**
     * Creates new DfaAttackAction
     */
    public DfaAttackAction(int entityId, int targetId, Coords targetPos) {
        super(entityId, targetId, targetPos);
    }

    public DfaAttackAction(int entityId, int targetType, int targetId, Coords targetPos) {
        super(entityId, targetType, targetId, targetPos);
    }

    /**
     * Damage done to a mek after a successful DFA.
     */
    public static int getDamageTakenBy(Entity entity) {
        return (int) Math.ceil(entity.getWeight() / 5.0);
    }

    /**
     * Damage that a mek does with a successful DFA.
     */
    public static int getDamageFor(Entity entity, boolean targetInfantry) {
        int toReturn = (int) Math.ceil((entity.getWeight() / 10.0) * 3.0);

        if (DfaAttackAction.hasTalons(entity)) {
            toReturn = (int) (toReturn * 1.5);
        }

        if (targetInfantry) {
            toReturn = Math.max(1, toReturn / 10);
        }

        return toReturn;

    }

    /**
     * Checks if a death from above attack can hit the target, including movement
     *
     * @param game The current {@link Game}
     */
    public static ToHitData toHit(Game game, int attackerId, Targetable target, MovePath md) {
        final Entity ae = game.getEntity(attackerId);

        if (ae == null) {
            return null;
        }

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

        if (md.contains(MoveStepType.JUMP_MEK_MECHANICAL_BOOSTER)) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "Can't D.F.A. using mechanical jump boosters.");
        }

        // let's just check this
        if (!md.contains(MoveStepType.DFA)) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "D.F.A. action not found in movement path");
        }

        // have to jump
        if (!md.contains(MoveStepType.START_JUMP)) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "D.F.A. must involve jumping");
        }

        // can't target airborne units
        if ((te != null) && te.isAirborne()) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "Cannot D.F.A. an airborne target.");
        }

        // can't target dropships
        if ((te instanceof Dropship)) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "Cannot D.F.A. a dropship.");
        }

        // Can't target a transported entity.
        if ((te != null) && (Entity.NONE != te.getTransportId())) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "Target is a passenger.");
        }

        // no evading
        if (md.contains(MoveStepType.EVADE)) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "No evading while charging");
        }

        // Can't target an entity conducting a swarm attack.
        if ((te != null) && (Entity.NONE != te.getSwarmTargetId())) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "Target is swarming a Mek.");
        }

        // determine last valid step
        md.compile(game, ae);
        for (final ListIterator<MoveStep> i = md.getSteps(); i.hasNext(); ) {
            final MoveStep step = i.next();
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
        if ((chargeStep == null) || !target.getPosition().equals(chargeStep.getPosition())) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "Could not reach target with movement");
        }

        // target must have moved already, unless it's immobile
        if ((te != null) && (!te.isDone() && !te.isImmobile())) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "Target must be done with movement");
        }

        return DfaAttackAction.toHit(game, attackerId, target, chargeSrc);
    }

    public ToHitData toHit(Game game) {
        final Entity entity = game.getEntity(getEntityId());

        if (entity == null) {
            return null;
        }

        return DfaAttackAction.toHit(game,
              getEntityId(),
              game.getTarget(getTargetType(), getTargetId()),
              entity.getPosition());
    }

    /**
     * To-hit number for a death from above attack, assuming that movement has been handled
     *
     * @param game The current {@link Game}
     */
    public static ToHitData toHit(Game game, int attackerId, @Nullable Targetable target, Coords src) {
        final Entity ae = game.getEntity(attackerId);

        // arguments legal?
        if (ae == null) {
            throw new IllegalArgumentException("Attacker is null");
        }

        // Do to pretreatment of physical attacks, the target may be null.
        if (target == null) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "Target is null");
        }

        int targetId;
        Entity te;

        if (target.getTargetType() == Targetable.TYPE_ENTITY) {
            te = (Entity) target;
            targetId = target.getId();
        } else {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "Invalid Target");
        }

        if (!game.getOptions().booleanOption(OptionsConstants.BASE_FRIENDLY_FIRE)) {
            // a friendly unit can never be the target of a direct attack.
            if ((target.getTargetType() == Targetable.TYPE_ENTITY) &&
                  ((target.getOwnerId() == ae.getOwnerId()) ||
                        ((((Entity) target).getOwner().getTeam() != Player.TEAM_NONE) &&
                              (ae.getOwner().getTeam() != Player.TEAM_NONE) &&
                              (ae.getOwner().getTeam() == ((Entity) target).getOwner().getTeam())))) {
                return new ToHitData(TargetRoll.IMPOSSIBLE,
                      "A friendly unit can never be the target of a direct attack.");
            }
        }

        final boolean targetInBuilding = Compute.isInBuilding(game, te);
        ToHitData toHit;

        final int attackerElevation = ae.getElevation() + game.getBoard().getHex(ae.getPosition()).getLevel();
        final int targetElevation = target.getElevation() + game.getBoard().getHex(target.getPosition()).getLevel();
        final int attackerHeight = attackerElevation + ae.getHeight();

        // check elevation of target flying VTOL
        if (target.isAirborneVTOLorWIGE()) {
            if ((targetElevation - attackerHeight) > ae.getJumpMP()) {
                return new ToHitData(TargetRoll.IMPOSSIBLE, "Elevation difference to high");
            }
        }

        // can't target yourself
        if (ae.equals(te)) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "You can't target yourself");
        }

        // Infantry CAN'T dfa!!!
        if (ae instanceof Infantry) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "Infantry can't dfa");
        }

        // Can't target a transported entity.
        if ((Entity.NONE != te.getTransportId())) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "Target is a passenger.");
        }

        // Can't target an entity conducting a swarm attack.
        if ((Entity.NONE != te.getSwarmTargetId())) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "Target is swarming a Mek.");
        }

        // check range
        if (src.distance(target.getPosition()) > 1) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "Target not in range");
        }

        // can't dfa while prone, even if you somehow did manage to jump
        if (ae.isProne()) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "Attacker is prone");
        }

        // can't attack mek making a different displacement attack
        if (te.hasDisplacementAttack()) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "Target is already making a charge/DFA attack");
        }

        // can't attack the target of another displacement attack
        if (te.isTargetOfDisplacementAttack() && (te.findTargetedDisplacement().getEntityId() != ae.getId())) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "Target is the target of another charge/DFA");
        }

        // Can't target units in buildings (from the outside).
        if (targetInBuilding) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "Target is inside building");
        }

        // Attacks against adjacent buildings automatically hit.
        if ((target.getTargetType() == Targetable.TYPE_BUILDING) ||
              (target.getTargetType() == Targetable.TYPE_FUEL_TANK) ||
              (target instanceof GunEmplacement)) {
            return new ToHitData(TargetRoll.AUTOMATIC_SUCCESS, "Targeting adjacent building.");
        }

        // Can't target woods or ignite a building with a physical.
        if ((target.getTargetType() == Targetable.TYPE_BLDG_IGNITE) ||
              (target.getTargetType() == Targetable.TYPE_HEX_CLEAR) ||
              (target.getTargetType() == Targetable.TYPE_HEX_IGNITE)) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "Invalid attack");
        }

        // Set the base BTH
        int base = ae.getCrew().getPiloting();

        toHit = new ToHitData(base, "base");
        toHit.addModifier(0, "DFA");

        // BMR(r), page 33. +3 modifier for DFA on infantry.
        if (te instanceof Infantry) {
            toHit.addModifier(3, "Infantry target");
        }

        // Battle Armor targets are hard for Meks and Tanks to hit.
        if (te instanceof BattleArmor) {
            toHit.addModifier(1, "battle armor target");
        }

        if ((ae instanceof Mek) && ae.isSuperHeavy()) {
            toHit.addModifier(1, "attacker is superheavy mek");
        }

        // attacker movement
        toHit.append(Compute.getAttackerMovementModifier(game, attackerId, EntityMovementType.MOVE_JUMP));

        // target movement
        toHit.append(Compute.getTargetMovementModifier(game, targetId));

        // piloting skill differential
        if ((ae.getCrew().getPiloting() != te.getCrew().getPiloting())) {
            toHit.addModifier(ae.getCrew().getPiloting() - te.getCrew().getPiloting(), "piloting skill differential");
        }

        // attacker is spotting (no penalty with second pilot in command console)
        if (ae.isSpotting() && !ae.getCrew().hasActiveCommandConsole()) {
            toHit.addModifier(+1, "attacker is spotting");
        }

        // target prone
        if (te.isProne()) {
            toHit.addModifier(-2, "target prone and adjacent");
        }

        // If it has a torso-mounted cockpit and two head sensor hits or three
        // sensor hits...
        // It gets a =4 penalty for being blind!
        if ((ae instanceof Mek) && (((Mek) ae).getCockpitType() == Mek.COCKPIT_TORSO_MOUNTED)) {
            int sensorHits = ae.getBadCriticalSlots(CriticalSlot.TYPE_SYSTEM, Mek.SYSTEM_SENSORS, Mek.LOC_HEAD);
            int sensorHits2 = ae.getBadCriticalSlots(CriticalSlot.TYPE_SYSTEM,
                  Mek.SYSTEM_SENSORS,
                  Mek.LOC_CENTER_TORSO);
            if ((sensorHits + sensorHits2) == 3) {
                return new ToHitData(TargetRoll.IMPOSSIBLE, "Sensors Completely Destroyed for Torso-Mounted Cockpit");
            } else if (sensorHits == 2) {
                toHit.addModifier(4, "Head Sensors Destroyed for Torso-Mounted Cockpit");
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
        if (game.getOptions().booleanOption(OptionsConstants.ADVANCED_GROUND_MOVEMENT_TAC_OPS_PHYSICAL_ATTACK_PSR)) {
            if (ae.getWeightClass() == EntityWeightClass.WEIGHT_LIGHT) {
                toHit.addModifier(-2, "Weight Class Attack Modifier");
            } else if (ae.getWeightClass() == EntityWeightClass.WEIGHT_MEDIUM) {
                toHit.addModifier(-1, "Weight Class Attack Modifier");
            }
        }

        if ((ae instanceof Mek) && ((Mek) ae).hasIndustrialTSM()) {
            toHit.addModifier(2, "industrial TSM");
        }

        // done!
        return toHit;
    }

    public static boolean hasTalons(Entity entity) {

        if (entity instanceof Mek) {

            if (entity instanceof BipedMek) {

                return (entity.hasWorkingMisc(MiscType.F_TALON, -1, Mek.LOC_RIGHT_LEG) &&
                      entity.hasWorkingSystem(Mek.ACTUATOR_FOOT, Mek.LOC_RIGHT_LEG)) ||
                      (entity.hasWorkingMisc(MiscType.F_TALON, -1, Mek.LOC_LEFT_LEG) &&
                            entity.hasWorkingSystem(Mek.ACTUATOR_FOOT, Mek.LOC_LEFT_LEG));
            }
            return (entity.hasWorkingMisc(MiscType.F_TALON, -1, Mek.LOC_RIGHT_LEG) &&
                  entity.hasWorkingSystem(Mek.ACTUATOR_FOOT, Mek.LOC_RIGHT_LEG)) ||
                  (entity.hasWorkingMisc(MiscType.F_TALON, -1, Mek.LOC_LEFT_LEG) &&
                        entity.hasWorkingSystem(Mek.ACTUATOR_FOOT, Mek.LOC_LEFT_LEG)) ||
                  ((entity.hasWorkingMisc(MiscType.F_TALON, -1, Mek.LOC_RIGHT_ARM)) &&
                        (entity.hasWorkingSystem(Mek.ACTUATOR_FOOT, Mek.LOC_RIGHT_ARM) ||
                              (entity.hasWorkingMisc(MiscType.F_TALON, -1, Mek.LOC_LEFT_ARM) &&
                                    entity.hasWorkingSystem(Mek.ACTUATOR_FOOT, Mek.LOC_LEFT_ARM))));
        }

        return false;
    }

    @Override
    public String toSummaryString(final Game game) {
        final String roll = this.toHit(game).getValueAsString();
        return Messages.getString("BoardView1.DfaAttackAction", roll);
    }
}
