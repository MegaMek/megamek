/*
 * Copyright (C) 2017-2025 The MegaMek Team. All Rights Reserved.
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
import megamek.common.units.Dropship;
import megamek.common.units.Entity;
import megamek.common.units.EntityMovementType;
import megamek.common.units.IBuilding;
import megamek.common.units.Infantry;
import megamek.common.units.LandAirMek;
import megamek.common.units.ProtoMek;
import megamek.common.units.Targetable;
import megamek.common.units.Terrains;

/**
 * Ram attack by an airborne LAM in air mek mode. This is treated like a charge in the movement path, but has
 * significant difference in the way damage is calculated and in the final locations.
 *
 * @author Neoancient
 */
public class AirMekRamAttackAction extends DisplacementAttackAction {
    @Serial
    private static final long serialVersionUID = 5110608317218688433L;

    public AirMekRamAttackAction(Entity attacker, Targetable target) {
        this(attacker.getId(), target.getTargetType(), target.getId(), target.getPosition());
    }

    public AirMekRamAttackAction(int entityId, int targetType, int targetId, Coords targetPos) {
        super(entityId, targetType, targetId, targetPos);
    }

    /**
     * To-hit number for a ram, assuming that movement has been handled
     *
     * @param game The current {@link Game}
     */
    public ToHitData toHit(Game game) {
        final Entity entity = game.getEntity(getEntityId());
        if (entity != null) {
            return toHit(game,
                  game.getTarget(getTargetType(), getTargetId()),
                  entity.getPosition(),
                  entity.getElevation(),
                  entity.moved);
        }

        return new ToHitData();
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

        if (!(ae instanceof LandAirMek) || !ae.isAirborneVTOLorWIGE()) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "Attacker is not airborne AirMek");
        }

        int targetId;
        Entity targetEntity;
        if (target.getTargetType() == Targetable.TYPE_ENTITY) {
            targetEntity = (Entity) target;
            targetId = target.getId();
        } else {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "Invalid Target");
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

        Hex targHex = game.getBoard().getHex(target.getPosition());
        // we should not be using the attacker's hex here since the attacker
        // will end up in
        // the target's hex
        final int attackerElevation = elevation + targHex.getLevel();
        final int attackerHeight = attackerElevation + ae.height();
        final int targetElevation = target.getElevation()
              + targHex.getLevel();
        final int targetHeight = targetElevation + target.getHeight();
        IBuilding bldg = game.getBoard().getBuildingAt(getTargetPos());
        ToHitData toHit;
        boolean targIsBuilding = ((getTargetType() == Targetable.TYPE_FUEL_TANK)
              || (getTargetType() == Targetable.TYPE_BUILDING));

        boolean inSameBuilding = Compute.isInSameBuilding(game, ae, targetEntity);

        // can't target yourself
        if (ae.equals(targetEntity)) {
            return new ToHitData(TargetRoll.IMPOSSIBLE,
                  "You can't target yourself");
        }

        // Can't target a transported entity.
        if (Entity.NONE != targetEntity.getTransportId()) {
            return new ToHitData(TargetRoll.IMPOSSIBLE,
                  "Target is a passenger.");
        }

        // Can't target an entity conducting a swarm attack.
        if (Entity.NONE != targetEntity.getSwarmTargetId()) {
            return new ToHitData(TargetRoll.IMPOSSIBLE,
                  "Target is swarming a Mek.");
        }

        // Cannot target infantry
        if (targetEntity instanceof Infantry) {
            return new ToHitData(TargetRoll.IMPOSSIBLE,
                  "Target is infantry.");
        }

        // Cannot target protomek
        if (targetEntity instanceof ProtoMek) {
            return new ToHitData(TargetRoll.IMPOSSIBLE,
                  "Target is protomek.");
        }

        // check range
        if (src.distance(target.getPosition()) > 1) {
            if (null != targetEntity.getSecondaryPositions()) {
                boolean inSecondaryRange = false;
                for (int i : targetEntity.getSecondaryPositions().keySet()) {
                    if (null != targetEntity.getSecondaryPositions().get(i)) {
                        if (src.distance(targetEntity.getSecondaryPositions().get(i)) < 2) {
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

        // can't attack mek making a different displacement attack
        if (targetEntity.hasDisplacementAttack()) {
            return new ToHitData(TargetRoll.IMPOSSIBLE,
                  "Target is already making a charge/DFA attack");
        }

        // target must have moved already
        // errata: immobile units can be targeted, even when they haven't moved
        // yet
        if (!targetEntity.isDone() && !targetEntity.isImmobile()) {
            return new ToHitData(TargetRoll.IMPOSSIBLE,
                  "Target must be done with movement");
        }

        // can't attack the target of another displacement attack
        if (targetEntity.isTargetOfDisplacementAttack()
              && (targetEntity.findTargetedDisplacement().getEntityId() != ae.getId())) {
            return new ToHitData(TargetRoll.IMPOSSIBLE,
                  "Target is the target of another charge/DFA");
        }

        // Can't target units in buildings (from the outside).
        if ((null != bldg) && (!targIsBuilding)
              && Compute.isInBuilding(game, targetEntity)) {
            if (!Compute.isInBuilding(game, ae)) {
                return new ToHitData(TargetRoll.IMPOSSIBLE,
                      "Target is inside building");
            } else if (!game.getBoard().getBuildingAt(ae.getPosition())
                  .equals(bldg)) {
                return new ToHitData(TargetRoll.IMPOSSIBLE,
                      "Target is inside different building");
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
        toHit.append(Compute.getTargetTerrainModifier(game, targetEntity, 0,
              inSameBuilding));

        // attacker is spotting
        if (ae.isSpotting() && !ae.getCrew().hasActiveCommandConsole()) {
            toHit.addModifier(+1, "attacker is spotting");
        }
        // piloting skill differential
        if (ae.getCrew().getPiloting() != targetEntity.getCrew().getPiloting()) {
            toHit.addModifier(ae.getCrew().getPiloting()
                  - targetEntity.getCrew().getPiloting(), "piloting skill differential");
        }

        // target prone
        if (targetEntity.isProne()) {
            toHit.addModifier(-2, "target prone and adjacent");
        }

        // water partial cover?
        if ((targetEntity.height() > 0) && (targetEntity.getElevation() == -1)
              && (targHex.terrainLevel(Terrains.WATER) == targetEntity.height())) {
            toHit.addModifier(1, "target has partial cover");
        }

        // target immobile
        toHit.append(Compute.getImmobileMod(targetEntity));

        Compute.modifyPhysicalBTHForAdvantages(ae, targetEntity, toHit, game);

        // evading bonuses (
        if (targetEntity.isEvading()) {
            toHit.addModifier(targetEntity.getEvasionBonus(), "target is evading");
        }

        // determine hit direction
        toHit.setSideTable(targetEntity.sideTable(src));

        // all rams resolved against full-body table, against meks in water partial cover
        if ((targHex.terrainLevel(Terrains.WATER) == targetEntity.height())
              && (targetEntity.getElevation() == -1) && (targetEntity.height() > 0)) {
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
    public ToHitData toHit(Game game, MovePath movePath) {
        final Entity attackingEntity = game.getEntity(getEntityId());
        final Targetable target = getTarget(game);
        Coords ramSrc = null;
        int ramEl = 0;

        if (attackingEntity != null) {
            ramSrc = attackingEntity.getPosition();
            ramEl = attackingEntity.getElevation();
        }

        MoveStep ramStep = null;

        // let's just check this
        if (!movePath.contains(MoveStepType.CHARGE)) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "Ram action not found in movement path");
        }

        // no evading
        if (movePath.contains(MoveStepType.EVADE)) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "No evading while charging");
        }

        // determine last valid step
        movePath.compile(game, attackingEntity);
        for (final ListIterator<MoveStep> i = movePath.getSteps(); i.hasNext(); ) {
            final MoveStep step = i.next();
            if (step.getMovementType(movePath.isEndStep(step)) == EntityMovementType.MOVE_ILLEGAL) {
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
                  && (null != target.getSecondaryPositions())) {
                for (int i : target.getSecondaryPositions().keySet()) {
                    if (null != target.getSecondaryPositions().get(i)) {
                        isReachable = target.getSecondaryPositions().get(i).equals(ramStep.getPosition());
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

        if (!movePath.getSecondLastStep().isLegalEndPos()) {
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
     * Damage that an AirMek does with a successful ram. Assumes that delta_distance is correct.
     */
    public static int getDamageFor(Entity entity) {
        return AirMekRamAttackAction.getDamageFor(entity, entity.delta_distance);
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
        return AirMekRamAttackAction.getDamageTakenBy(entity, target, entity.delta_distance);
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
