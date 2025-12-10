/*
 * Copyright (C) 2000-2004 Ben Mazur (bmazur@sev.org)
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
import java.util.ListIterator;
import java.util.Objects;

import megamek.common.Hex;
import megamek.common.Player;
import megamek.common.ToHitData;
import megamek.common.board.Coords;
import megamek.common.compute.Compute;
import megamek.common.enums.MoveStepType;
import megamek.common.game.Game;
import megamek.common.moves.MovePath;
import megamek.common.moves.MoveStep;
import megamek.common.options.OptionsConstants;
import megamek.common.rolls.TargetRoll;
import megamek.common.units.Dropship;
import megamek.common.units.Entity;
import megamek.common.units.EntityMovementType;
import megamek.common.units.FighterSquadron;
import megamek.common.units.IAero;
import megamek.common.units.Jumpship;
import megamek.common.units.SpaceStation;
import megamek.common.units.Targetable;
import megamek.common.units.Warship;

/**
 * Represents one unit charging another. Stores information about where the target is supposed to be for the charge to
 * be successful, as well as normal attack info.
 *
 * @author Ben Mazur
 * @since May 28, 2008
 */
public class RamAttackAction extends AbstractAttackAction {
    @Serial
    private static final long serialVersionUID = -3549351664290057785L;

    public RamAttackAction(Entity attacker, Targetable target) {
        this(attacker.getId(), target.getTargetType(), target.getId(), target.getPosition());
    }

    public RamAttackAction(int entityId, int targetType, int targetId, Coords targetPos) {
        super(entityId, targetType, targetId);
    }

    /**
     * To-hit number for a ram, assuming that movement has been handled
     */
    public ToHitData toHit(Game game) {
        final Entity entity = game.getEntity(getEntityId());

        if (entity != null) {
            return toHit(game,
                  game.getTarget(getTargetType(), getTargetId()),
                  entity.getPosition(),
                  entity.getElevation(),
                  entity.getPriorPosition(),
                  entity.moved);
        }

        return null;
    }

    /**
     * To-hit number for a ram, assuming that movement has been handled
     */
    public ToHitData toHit(Game game, Targetable target, Coords src, int elevation, Coords priorSrc,
          EntityMovementType movement) {
        final Entity ae = getEntity(game);

        // arguments legal?
        Objects.requireNonNull(ae, "Attacker is null");

        // Do to pretreatment of physical attacks, the target may be null.
        if (target == null) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "Target is null");
        }

        if (!ae.isAero()) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "Attacker is not Aero");
        }

        if (!target.isAero()) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "Target is not Aero");
        }

        if (ae instanceof FighterSquadron || target instanceof FighterSquadron) {
            return new ToHitData(TargetRoll.IMPOSSIBLE,
                  "fighter squadrons may not ram nor be the target of a ramming attack.");
        }

        Entity te;
        if (target.getTargetType() == Targetable.TYPE_ENTITY) {
            te = (Entity) target;
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
        Hex attHex = game.getBoard().getHex(src);
        Hex targHex = game.getBoard().getHex(target.getPosition());
        final int attackerElevation = elevation + attHex.getLevel();
        final int targetElevation = target.getElevation() + targHex.getLevel();
        ToHitData toHit;

        // can't target yourself
        if (ae.equals(te)) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "You can't target yourself");
        }

        // Can't target a transported entity.
        if (Entity.NONE != te.getTransportId()) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "Target is a passenger.");
        }

        // check range
        if (src.distance(target.getPosition()) > 0) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "Target not in range");
        }

        // target must be at same elevation level
        if (attackerElevation != targetElevation) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "Target must be at the same elevation level");
        }

        // can't attack Aero making a different ramming attack
        if (te.isRamming()) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "Target is already making a ramming attack");
        }

        // attacker

        // target must have moved already
        if (!te.isDone()) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "Target must be done with movement");
        }

        // Set the base BTH
        int base = 6 + te.getCrew().getPiloting() - ae.getCrew().getPiloting();

        toHit = new ToHitData(base, "base");

        IAero a = (IAero) ae;

        // target type
        if (target instanceof SpaceStation) {
            toHit.addModifier(-1, "target is a space station");
        } else if (target instanceof Warship) {
            toHit.addModifier(+1, "target is a WarShip");
        } else if (target instanceof Jumpship) {
            toHit.addModifier(+0, "target is a JumpShip");
        } else if (target instanceof Dropship) {
            toHit.addModifier(+2, "target is a DropShip");
        } else {
            toHit.addModifier(+4, "target is a fighter/small craft");
        }

        // attacker type
        if (a instanceof SpaceStation) {
            toHit.addModifier(+0, "attacker is a space station");
        } else if (a instanceof Warship) {
            toHit.addModifier(+1, "attacker is a WarShip");
        } else if (a instanceof Jumpship) {
            toHit.addModifier(+0, "attacker is a JumpShip");
        } else if (a instanceof Dropship) {
            toHit.addModifier(-1, "attacker is a DropShip");
        } else {
            toHit.addModifier(-2, "attacker is a fighter/small craft");
        }

        // can the target unit move
        if (target.isImmobile() || (te.getWalkMP() == 0)) {
            toHit.addModifier(-2, "target cannot spend thrust");
        }

        // sensor damage
        if (a.getSensorHits() > 0) {
            toHit.addModifier(+1, "sensor damage");
        }

        // avionics damage
        int avionics = Math.min(a.getAvionicsHits(), 3);
        if (avionics > 0) {
            toHit.addModifier(avionics, "avionics damage");
        }

        // evading bonuses
        if (target.getTargetType() == Targetable.TYPE_ENTITY && te.isEvading()) {
            toHit.addModifier(te.getEvasionBonus(), "target is evading");
        }

        // determine hit direction
        toHit.setSideTable(te.sideTable(priorSrc));

        toHit.setHitTable(ToHitData.HIT_NORMAL);

        // done!
        return toHit;
    }

    /**
     * Checks if a ram can hit the target, taking account of movement
     */
    public ToHitData toHit(Game game, MovePath md) {
        final Entity attackingEntity = game.getEntity(getEntityId());
        final Targetable target = getTarget(game);

        Coords ramSrc = null;
        Coords priorSrc = null;
        int ramEl = 0;

        if (attackingEntity != null) {
            ramSrc = attackingEntity.getPosition();
            ramEl = attackingEntity.getElevation();
            priorSrc = md.getSecondFinalPosition(attackingEntity.getPosition());
        }

        MoveStep ramStep = null;

        // let's just check this
        if (!md.contains(MoveStepType.RAM)) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "Ram action not found in movement path");
        }

        // determine last valid step
        md.compile(game, attackingEntity);
        for (final ListIterator<MoveStep> i = md.getSteps(); i.hasNext(); ) {
            final MoveStep step = i.next();
            if (step.getMovementType(md.isEndStep(step)) == EntityMovementType.MOVE_ILLEGAL) {
                break;
            }
            if (step.getType() == MoveStepType.RAM) {
                ramStep = step;
                ramSrc = step.getPosition();
                ramEl = step.getElevation();
            }
        }

        // need to reach target
        if (ramStep == null || !target.getPosition().equals(ramStep.getPosition())) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "Could not reach target with movement");
        }

        return toHit(game, target, ramSrc, ramEl, priorSrc, ramStep.getMovementType(true));
    }

    /**
     * Damage that an Aero does on a successful ramming attack
     */
    public static int getDamageFor(IAero attacker, Entity target) {
        int attackerVelocity = attacker.getCurrentVelocity();
        int targetVelocity = 0;
        if (target.isAero()) {
            targetVelocity = ((IAero) target).getCurrentVelocity();
        }
        return getDamageFor(attacker, target, ((Entity) attacker).getPriorPosition(), attackerVelocity, targetVelocity);
    }

    public static int getDamageFor(IAero attacker, Entity target, Coords attackHex, int attackVelocity,
          int targetVelocity) {
        int netVelocity = Compute.getNetVelocity(attackHex, target, attackVelocity, targetVelocity);
        return (int) Math.ceil((((Entity) attacker).getWeight() / 10.0) * netVelocity);
    }

    /**
     * Damage that an Aero suffers after a successful charge.
     */
    public static int getDamageTakenBy(IAero attacker, Entity target) {
        int attackVelocity = attacker.getCurrentVelocity();
        int targetVelocity = 0;
        if (target.isAero()) {
            targetVelocity = ((IAero) target).getCurrentVelocity();
        }
        return getDamageTakenBy(attacker,
              target,
              ((Entity) attacker).getPriorPosition(),
              attackVelocity,
              targetVelocity);
    }

    public static int getDamageTakenBy(IAero attacker, Entity target, Coords attackHex, int attackVelocity,
          int targetVelocity) {
        int netVelocity = Compute.getNetVelocity(attackHex, target, attackVelocity, targetVelocity);
        return (int) Math.ceil((target.getWeight() / 10.0) * netVelocity);
    }
}
