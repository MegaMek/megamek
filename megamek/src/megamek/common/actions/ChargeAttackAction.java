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
import megamek.common.Hex;
import megamek.common.Player;
import megamek.common.ToHitData;
import megamek.common.board.Coords;
import megamek.common.compute.Compute;
import megamek.common.enums.MoveStepType;
import megamek.common.equipment.GunEmplacement;
import megamek.common.game.Game;
import megamek.common.interfaces.ILocationExposureStatus;
import megamek.common.moves.MovePath;
import megamek.common.moves.MoveStep;
import megamek.common.options.OptionsConstants;
import megamek.common.rolls.TargetRoll;
import megamek.common.units.*;

/**
 * Represents one unit charging another. Stores information about where the target is supposed to be for the charge to
 * be successful, as well as normal attack info.
 *
 * @author Ben Mazur
 * @since March 12, 2002, 3:23 PM
 */
public class ChargeAttackAction extends DisplacementAttackAction {
    @Serial
    private static final long serialVersionUID = -3549351664290057785L;

    public ChargeAttackAction(Entity attacker, Targetable target) {
        this(attacker.getId(), target.getTargetType(), target.getId(),
              target.getPosition());
    }

    public ChargeAttackAction(int entityId, int targetType, int targetId,
          Coords targetPos) {
        super(entityId, targetType, targetId, targetPos);
    }

    /**
     * To-hit number for a charge, assuming that movement has been handled
     *
     * @param game The current {@link Game}
     */
    public ToHitData toHit(Game game) {
        return toHit(game, false);
    }

    public ToHitData toHit(Game game, boolean skid) {
        final Entity entity = game.getEntity(getEntityId());
        if (entity == null) {
            return new ToHitData();
        }

        return toHit(game,
              game.getTarget(getTargetType(), getTargetId()),
              entity.getPosition(),
              entity.getElevation(),
              entity.moved,
              skid,
              false);
    }

    /**
     * To-hit number for a charge, assuming that movement has been handled
     *
     * @param game The current {@link Game}
     */
    public ToHitData toHit(Game game, Targetable target, Coords src, int elevation, EntityMovementType movement,
          boolean skid, boolean gotUp) {
        final Entity attackingEntity = getEntity(game);

        // arguments legal?
        if (attackingEntity == null) {
            throw new IllegalStateException("Attacker is null");
        }

        // Due to pretreatment of physical attacks, the target may be null.
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
            if (!skid && (target.getTargetType() == Targetable.TYPE_ENTITY)
                  && ((target.getOwnerId() == attackingEntity.getOwnerId())
                  || ((((Entity) target).getOwner().getTeam() != Player.TEAM_NONE)
                  && (attackingEntity.getOwner().getTeam() != Player.TEAM_NONE)
                  && (attackingEntity.getOwner().getTeam() == ((Entity) target).getOwner().getTeam())))) {
                return new ToHitData(TargetRoll.IMPOSSIBLE,
                      "A friendly unit can never be the target of a direct attack.");
            }
        }

        Hex srcHex = game.getBoard().getHex(src);
        Hex targHex = game.getBoard().getHex(target.getPosition());
        // we should not be using the attacker's hex here since the attacker
        // will end up in
        // the target's hex

        // If the charge is coming across a bridge, we want the elevation above the
        // bridge rather
        // than the underlying terrain.
        final int attackerElevation;
        if (srcHex.containsTerrain(Terrains.BRIDGE)
              && (elevation >= srcHex.getTerrain(Terrains.BRIDGE_ELEV).getLevel())) {
            attackerElevation = elevation + targHex.getLevel() - srcHex.getTerrain(Terrains.BRIDGE_ELEV).getLevel();
        } else {
            attackerElevation = elevation + targHex.getLevel();
        }
        final int attackerHeight = attackerElevation + attackingEntity.height();
        final int targetElevation;
        if (targHex.containsTerrain(Terrains.BRIDGE)
              && (target.getElevation() >= targHex.getTerrain(Terrains.BRIDGE_ELEV).getLevel())) {
            targetElevation = target.getElevation() + targHex.getLevel()
                  - targHex.getTerrain(Terrains.BRIDGE_ELEV).getLevel();
        } else {
            targetElevation = target.getElevation() + targHex.getLevel();
        }
        final int targetHeight = targetElevation + target.getHeight();
        IBuilding bldg = game.getBoard().getBuildingAt(getTargetPos());
        ToHitData toHit;
        boolean targIsBuilding = ((getTargetType() == Targetable.TYPE_FUEL_TANK)
              || (getTargetType() == Targetable.TYPE_BUILDING));

        boolean inSameBuilding = Compute.isInSameBuilding(game, attackingEntity, te);

        // can't target yourself
        if (attackingEntity.equals(te)) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "You can't target yourself");
        }

        // Can't target a transported entity.
        if (Entity.NONE != te.getTransportId()) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "Target is a passenger.");
        }

        // Can't target an entity conducting a swarm attack.
        if (Entity.NONE != te.getSwarmTargetId()) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "Target is swarming a Mek.");
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
                    return new ToHitData(TargetRoll.IMPOSSIBLE, "Target not in range");
                }
            } else {
                return new ToHitData(TargetRoll.IMPOSSIBLE, "Target not in range");
            }
        }

        // meks can only charge standing meks
        if ((attackingEntity instanceof Mek) && !skid) {
            if (!(te instanceof Mek)) {
                return new ToHitData(TargetRoll.IMPOSSIBLE, "Target is not a 'Mek");
            }

            if (te.isProne()) {
                return new ToHitData(TargetRoll.IMPOSSIBLE, "Target is prone");
            }
        } else if (te instanceof Infantry) {
            // Can't charge infantry.
            return new ToHitData(TargetRoll.IMPOSSIBLE, "Target is infantry");
        } else if (te instanceof ProtoMek) {
            // Can't charge ProtoMeks.
            return new ToHitData(TargetRoll.IMPOSSIBLE, "Target is ProtoM<ech");
        }

        // target must be within 1 elevation level
        if ((attackerElevation > targetHeight)
              || (attackerHeight < targetElevation)) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "Target must be within 1 elevation level");
        }

        // can't attack mek making a different displacement attack
        if (te.hasDisplacementAttack()) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "Target is already making a charge/DFA attack");
        }

        // target must have moved already, unless it's a skid charge
        // errata: immobile units can be targeted, even when they haven't moved
        // yet
        if (!te.isDone() && !skid && !te.isImmobile()) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "Target must be done with movement");
        }

        // can't attack the target of another displacement attack
        if (te.isTargetOfDisplacementAttack()
              && (te.findTargetedDisplacement().getEntityId() != attackingEntity.getId())) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "Target is the target of another charge/DFA");
        }

        // Can't target units in buildings (from the outside).
        if ((null != bldg) && (!targIsBuilding)
              && Compute.isInBuilding(game, te)) {
            if (!Compute.isInBuilding(game, attackingEntity)) {
                return new ToHitData(TargetRoll.IMPOSSIBLE, "Target is inside building");
            } else if (!game.getBoard().getBuildingAt(attackingEntity.getPosition()).equals(bldg)) {
                return new ToHitData(TargetRoll.IMPOSSIBLE, "Target is inside different building");
            }
        }

        // Attacks against adjacent buildings automatically hit.
        if ((target.getTargetType() == Targetable.TYPE_BUILDING)
              || (target.getTargetType() == Targetable.TYPE_FUEL_TANK)
              || (target instanceof GunEmplacement)) {
            return new ToHitData(TargetRoll.AUTOMATIC_SUCCESS, "Targeting adjacent building.");
        }

        // Can't target woods or ignite a building with a physical.
        if ((target.getTargetType() == Targetable.TYPE_BLDG_IGNITE)
              || (target.getTargetType() == Targetable.TYPE_HEX_CLEAR)
              || (target.getTargetType() == Targetable.TYPE_HEX_IGNITE)) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "Invalid attack");
        }

        // Set the base BTH
        int base = attackingEntity.getCrew().getPiloting();

        toHit = new ToHitData(base, "base");
        toHit.addModifier(0, "Charge");

        // attacker movement
        toHit.append(Compute.getAttackerMovementModifier(game, attackingEntity.getId(), movement));

        // target movement
        toHit.append(Compute.getTargetMovementModifier(game, targetId));

        // attacker terrain
        toHit.append(Compute.getAttackerTerrainModifier(game, attackingEntity.getId()));

        // target terrain
        toHit.append(Compute.getTargetTerrainModifier(game, te, 0, inSameBuilding));

        if ((attackingEntity instanceof Mek) && attackingEntity.isSuperHeavy()) {
            toHit.addModifier(+1, "attacker is superheavy mek");
        }

        // attacker is spotting
        if (attackingEntity.isSpotting() && !attackingEntity.getCrew().hasActiveCommandConsole()) {
            toHit.addModifier(+1, "attacker is spotting");
        }
        // piloting skill differential
        if (attackingEntity.getCrew().getPiloting() != te.getCrew().getPiloting()) {
            toHit.addModifier(attackingEntity.getCrew().getPiloting() - te.getCrew().getPiloting(),
                  "piloting skill differential");
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

        // If it has a torso-mounted cockpit and two head sensor hits or three
        // sensor hits...
        // It gets a =4 penalty for being blind!
        if ((attackingEntity instanceof Mek)
              && (((Mek) attackingEntity).getCockpitType() == Mek.COCKPIT_TORSO_MOUNTED)) {
            int sensorHits = attackingEntity.getBadCriticalSlots(CriticalSlot.TYPE_SYSTEM,
                  Mek.SYSTEM_SENSORS,
                  Mek.LOC_HEAD);
            int sensorHits2 = attackingEntity.getBadCriticalSlots(CriticalSlot.TYPE_SYSTEM,
                  Mek.SYSTEM_SENSORS,
                  Mek.LOC_CENTER_TORSO);
            if ((sensorHits + sensorHits2) == 3) {
                return new ToHitData(TargetRoll.IMPOSSIBLE,
                      "Sensors Completely Destroyed for Torso-Mounted Cockpit");
            } else if (sensorHits == 2) {
                toHit.addModifier(4, "Head Sensors Destroyed for Torso-Mounted Cockpit");
            }
        }

        // target immobile
        toHit.append(Compute.getImmobileMod(te));

        // skids have a penalty for unintentional charge
        if (skid) {
            toHit.addModifier(3, "unintentional charge");
        }

        Compute.modifyPhysicalBTHForAdvantages(attackingEntity, te, toHit, game);

        // evading bonuses (
        if (te.isEvading()) {
            toHit.addModifier(te.getEvasionBonus(), "target is evading");
        }

        // determine hit direction
        toHit.setSideTable(te.sideTable(src));

        // all charges resolved against full-body table, except vehicles
        // and charges against meks in water partial cover
        if ((targHex.terrainLevel(Terrains.WATER) == te.height())
              && (te.getElevation() == -1) && (te.height() > 0)) {
            toHit.setHitTable(ToHitData.HIT_PUNCH);
        } else if (attackingEntity.getHeight() < target.getHeight()) {
            toHit.setHitTable(ToHitData.HIT_KICK);
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

        // Attacking Weight Class Modifier.
        if (game.getOptions().booleanOption(OptionsConstants.ADVANCED_GROUND_MOVEMENT_TAC_OPS_PHYSICAL_ATTACK_PSR)) {
            if (attackingEntity.getWeightClass() == EntityWeightClass.WEIGHT_LIGHT) {
                toHit.addModifier(-2, "Weight Class Attack Modifier");
            } else if (attackingEntity.getWeightClass() == EntityWeightClass.WEIGHT_MEDIUM) {
                toHit.addModifier(-1, "Weight Class Attack Modifier");
            }
        }

        if ((attackingEntity instanceof Mek) && ((Mek) attackingEntity).hasIndustrialTSM()) {
            toHit.addModifier(2, "industrial TSM");
        }

        // done!
        return toHit;
    }

    /**
     * Checks if a charge can hit the target, taking account of movement
     *
     * @param game The current {@link Game}
     */
    public ToHitData toHit(Game game, MovePath md) {
        final Entity attackingEntity = game.getEntity(getEntityId());
        final Targetable target = getTarget(game);
        Coords chargeSrc = null;
        int chargeEl = 0;

        if (attackingEntity != null) {
            chargeSrc = attackingEntity.getPosition();
            chargeEl = attackingEntity.getElevation();
        }

        MoveStep chargeStep = null;

        // let's just check this
        if (!md.contains(MoveStepType.CHARGE)) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "Charge action not found in movement path");
        }

        // no jumping
        if (md.contains(MoveStepType.START_JUMP)) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "No jumping allowed while charging");
        }

        // no backwards
        if (md.contains(MoveStepType.BACKWARDS)
              || md.contains(MoveStepType.LATERAL_LEFT_BACKWARDS)
              || md.contains(MoveStepType.LATERAL_RIGHT_BACKWARDS)) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "No backwards movement allowed while charging");
        }

        // no prone
        if (md.getFinalProne()) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "You cannot charge if you end the movement phase prone");
        }

        // no evading
        if (md.contains(MoveStepType.EVADE)) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "No evading while charging");
        }

        // determine last valid step
        md.compile(game, attackingEntity);
        for (final ListIterator<MoveStep> i = md.getSteps(); i.hasNext(); ) {
            final MoveStep step = i.next();
            if (step.getMovementType(md.isEndStep(step)) == EntityMovementType.MOVE_ILLEGAL) {
                break;
            }

            if (step.getType() == MoveStepType.CHARGE) {
                chargeStep = step;
            } else {
                chargeSrc = step.getPosition();
                chargeEl = step.getElevation();
            }
        }

        // need to reach target
        boolean isReachable = false;
        if ((chargeStep != null)) {
            isReachable = target.getPosition().equals(chargeStep.getPosition());
            if (!isReachable && (target instanceof Entity) && (null != target.getSecondaryPositions())) {
                for (int i : target.getSecondaryPositions().keySet()) {
                    if (null != target.getSecondaryPositions().get(i)) {
                        isReachable = target.getSecondaryPositions().get(i).equals(chargeStep.getPosition());
                        if (isReachable) {
                            break;
                        }
                    }
                }
            }
        }
        if (!isReachable) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "Could not reach target with movement");
        }

        if (!md.getSecondLastStep().isLegalEndPos()) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "Violation of stacking limit in second last step");
        }

        return toHit(
              game,
              target,
              chargeSrc,
              chargeEl,
              chargeStep.getMovementType(true),
              false,
              md.contains(MoveStepType.GET_UP)
                    || md.contains(MoveStepType.CAREFUL_STAND));
    }

    /**
     * Damage that a mek does with a successful charge. Assumes that delta_distance is correct.
     */
    public static int getDamageFor(Entity entity) {
        return ChargeAttackAction.getDamageFor(entity, entity, false, 0, entity.delta_distance);
    }

    public static int getDamageFor(Entity entity, boolean tacOps, int hexesMoved) {
        return ChargeAttackAction.getDamageFor(entity, entity, tacOps, 0, hexesMoved);
    }

    public static int getDamageFor(Entity entity, Entity target, boolean tacOps, int mos) {
        return ChargeAttackAction.getDamageFor(entity, target, tacOps, mos, entity.delta_distance);
    }

    public static int getDamageFor(Entity entity, Entity target, boolean tacOps, int mos, int hexesMoved) {
        if (!tacOps) {
            if (hexesMoved == 0) {
                hexesMoved = 1;
            }
            return (int) Math
                  .ceil((entity.getWeight() / 10.0)
                        * (hexesMoved - 1)
                        * (entity.getLocationStatus(1) == ILocationExposureStatus.WET ? 0.5
                        : 1));
        }
        return (int) Math
              .floor(((((target.getWeight() * entity.getWeight()) * hexesMoved) / (target
                    .getWeight()
                    + entity
                    .getWeight()))
                    / 10) +
                    mos);
    }

    /**
     * Damage that a mek suffers after a successful charge.
     */
    public static int getDamageTakenBy(Entity entity, IBuilding bldg, Coords coords) {
        // Charges against targets that have no tonnage use the attacker's tonnage to
        // compute damage.
        return getDamageTakenBy(entity, entity, false, entity.delta_distance);
    }

    public static int getDamageTakenBy(Entity entity, Entity target) {
        return ChargeAttackAction.getDamageTakenBy(entity, target, false, 0);
    }

    public static int getDamageTakenBy(Entity entity, Entity target, boolean tacOps) {
        return ChargeAttackAction.getDamageTakenBy(entity, target, tacOps, entity.delta_distance);
    }

    public static int getDamageTakenBy(Entity entity, Entity target, boolean tacOps, int distance) {
        // Per TW p.148, DropShips are "unusual targets" - use attacker's weight for self-damage
        // (same as buildings which have no tonnage)
        double effectiveTargetWeight = (target instanceof Dropship) ? entity.getWeight() : target.getWeight();

        if (!tacOps) {
            return (int) Math
                  .ceil((effectiveTargetWeight / 10.0)
                        * (entity.getLocationStatus(1) == ILocationExposureStatus.WET ? 0.5 : 1));
        }
        return (int) Math
              .floor((((effectiveTargetWeight * entity.getWeight()) * distance)
                    / (effectiveTargetWeight + entity.getWeight())) / 10);
    }

    @Override
    public String toSummaryString(final Game game) {
        final String roll = this.toHit(game).getValueAsString();
        return Messages.getString("BoardView1.ChargeAttackAction", roll);
    }
}
