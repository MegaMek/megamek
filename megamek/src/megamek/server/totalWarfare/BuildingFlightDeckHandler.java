/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
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

package megamek.server.totalWarfare;

import java.util.Vector;

import megamek.common.HitData;
import megamek.common.Report;
import megamek.common.ToHitData;
import megamek.common.actions.RamAttackAction;
import megamek.common.board.Coords;
import megamek.common.compute.Compute;
import megamek.common.enums.MoveStepType;
import megamek.common.moves.MobileStructureAirMovement;
import megamek.common.moves.MovePath;
import megamek.common.turns.SpecificEntityTurn;
import megamek.common.units.AbstractBuildingEntity;
import megamek.common.units.Aero;
import megamek.common.units.BuildingFlightDeckRules;
import megamek.common.units.Entity;
import megamek.common.units.IAero;
import megamek.common.units.LandAirMek;
import megamek.common.units.Mek;
import megamek.common.units.VTOL;

/** Flight-deck operations use normal aircraft controls and leave landed aircraft exposed on their actual roof hex. */
final class BuildingFlightDeckHandler extends AbstractTWRuleHandler {
    BuildingFlightDeckHandler(TWGameManager manager) { super(manager); }

    boolean handleAircraft(Entity unit, MovePath path, boolean launchMapAvailable, Runnable changeLaunchBoard) {
        if (!(unit instanceof IAero aero)) { return false; }
        boolean takeoff = path.contains(MoveStepType.TAKEOFF) || path.contains(MoveStepType.VERTICAL_TAKE_OFF);
        var occupied = BuildingFlightDeckRules.onDeck(unit);
        if (takeoff && occupied != null) {
            boolean vertical = path.contains(MoveStepType.VERTICAL_TAKE_OFF);
            if (path.length() == 1 && launchMapAvailable && BuildingFlightDeckRules.canTakeOff(unit, vertical)) {
                launch(occupied, unit, vertical, changeLaunchBoard);
            } else {
                unit.setDone(true);
                gameManager.entityUpdate(unit.getId());
            }
            return true;
        }
        boolean vertical = path.contains(MoveStepType.VERTICAL_LAND);
        if (!vertical && !path.contains(MoveStepType.LAND)) { return false; }
        if (BuildingFlightDeckRules.decksAt(getGame(), path.getFinalBoardId(), path.getFinalCoords()).isEmpty()) {
            return false;
        }
        var deck = BuildingFlightDeckRules.landingDeck(unit, path.getFinalBoardId(), path.getFinalCoords(),
              path.getFinalFacing(), vertical);
        int approachAltitude = path.length() < 2 ? unit.getAltitude() : path.getStep(path.length() - 2).getAltitude();
        if (unit.isAirborne() && deck != null && approachAltitude == MobileStructureAirMovement.aerospaceAltitude(
              deck.carrier(), path.getFinalCoords(), deck.carrier().getHeight(path.getFinalCoords()))) {
            int velocity = path.getFinalVelocity();
            unit.setBoardId(path.getFinalBoardId());
            var roll = aero.getLandingControlRoll(velocity, path.getFinalCoords(), path.getFinalFacing(), vertical);
            boolean success = gameManager.attemptLandingCheck(unit, roll, gameManager.getMainPhaseReport());
            int length = deck.landingDeck() ? aero.getLandingLength() : 3;
            Coords position = vertical ? path.getFinalCoords() : path.getFinalCoords().translated(path.getFinalFacing(), length - 1);
            if (!success) {
                int damage = vertical ? (int) Math.ceil(unit.getWeight() / 2)
                      : RamAttackAction.getDamageFor(aero, deck.carrier(), path.getFinalCoords().translated(
                            Math.floorMod(path.getFinalFacing() + 3, 6)), velocity, deck.carrier().delta_distance / 16);
                addReport(gameManager.damageBuilding(deck.carrier(), damage, "flight deck landing collision", position,
                      Math.max(0, deck.carrier().getHeight(position) - 1), null, false));
            }
            aero.land();
            unit.setPosition(position);
            unit.setFacing(path.getFinalFacing());
            unit.setSecondaryFacing(path.getFinalFacing());
            unit.setElevation(deck.elevation(position));
            BuildingFlightDeckRules.landed(deck, unit, position);
            new TaintedAtmosphereHandler(gameManager).checkExhaustWashIgnition(unit, path.getFinalCoords(),
                  unit.getBoardId(), path.getFinalFacing(), TaintedAtmosphereHandler.ExhaustWashMoment.LANDING);
            gameManager.entityUpdate(deck.carrier().getId());
        }
        unit.setDone(true);
        gameManager.entityUpdate(unit.getId());
        return true;
    }

    private void launch(BuildingFlightDeckRules.Deck deck, Entity unit, boolean vertical, Runnable changeLaunchBoard) {
        IAero aero = (IAero) unit;
        boolean success;
        if (vertical) {
            success = gameManager.doVerticalTakeOffCheck(unit, aero.checkVerticalTakeOff());
        } else if (deck.landingDeck()) {
            success = true;
        } else {
            var target = unit.getBasePilotingRoll();
            var dice = Compute.rollD6(2);
            var report = new Report(9375);
            report.subject = unit.getId();
            report.add(unit.getDisplayName());
            report.add(target);
            report.add(dice);
            success = dice.getIntValue() >= target.getValue();
            report.choose(success);
            addReport(report);
            if (!success) {
                int damage = 10 * (target.getValue() - dice.getIntValue());
                // The catapult strains the aft; the normal bay-launch damage table strikes the nose (TO:AUE p.125).
                HitData hit = new HitData(unit instanceof LandAirMek
                      ? (Compute.d6() <= 3 ? Mek.LOC_LEFT_LEG : Mek.LOC_RIGHT_LEG) : Aero.LOC_AFT);
                addReport(gameManager.damageEntity(unit, hit, damage));
            }
        }
        deck.state().operated(getGame().getRoundCount());
        if (!unit.isDestroyed() && !unit.isDoomed() && (!vertical || success)) {
            int altitude = MobileStructureAirMovement.aerospaceAltitude(deck.carrier(), unit.getPosition(),
                  deck.carrier().getHeight(unit.getPosition()) + 1);
            BuildingFlightDeckRules.departed(deck, unit);
            new TaintedAtmosphereHandler(gameManager).checkExhaustWashIgnition(unit, unit.getPosition(),
                  unit.getBoardId(), unit.getFacing(), TaintedAtmosphereHandler.ExhaustWashMoment.TAKEOFF);
            changeLaunchBoard.run();
            aero.setCurrentVelocity(1);
            aero.setNextVelocity(1);
            aero.liftOff(altitude);
            unit.setDone(deck.landingDeck());
            gameManager.checkForTakeoffDamage(aero);
            if (!deck.landingDeck()) {
                // A catapult launch uses fighter-launch timing, allowing movement during this same turn.
                getGame().insertTurnAfter(new SpecificEntityTurn(unit.getOwnerId(), unit.getId()), getGame().getTurnIndex());
                gameManager.send(gameManager.getPacketHelper().createTurnListPacket());
            }
        } else {
            unit.setDone(true);
        }
        gameManager.entityUpdate(unit.getId());
        gameManager.entityUpdate(deck.carrier().getId());
    }

    /** VTOLs use ordinary UP/DOWN movement; completion records the operation after ordinary crash checks. */
    void completedVtolMovement(Entity unit, BuildingFlightDeckRules.Deck previous, boolean crashed) {
        if (!(unit instanceof VTOL)) { return; }
        var deck = BuildingFlightDeckRules.onDeck(unit);
        if (previous != null && (deck == null || previous.carrier() != deck.carrier() || previous.id() != deck.id())) {
            BuildingFlightDeckRules.departed(previous, unit);
            gameManager.entityUpdate(previous.carrier().getId());
        }
        if (previous == null && deck != null) {
            BuildingFlightDeckRules.landed(deck, unit, unit.getPosition());
            if (crashed) {
                addReport(gameManager.damageBuilding(deck.carrier(), (int) Math.ceil(unit.getWeight() / 2),
                      "VTOL helipad landing collision", unit.getPosition(),
                      Math.max(0, deck.carrier().getHeight(unit.getPosition()) - 1), null, false));
            }
            gameManager.entityUpdate(deck.carrier().getId());
        }
    }
    void damageOnDeck(AbstractBuildingEntity carrier, Coords coords, int damage, Entity attacker, Vector<Report> reports) {
        if (damage <= 0 || attacker == null) { return; }
        var victims = BuildingFlightDeckRules.decksAt(getGame(), carrier.getBoardId(), coords).stream()
              .filter(deck -> deck.carrier() == carrier).flatMap(deck -> deck.occupants().stream())
              .map(occupant -> getGame().getEntity(occupant.entityId())).distinct()
              .filter(unit -> unit != null && coords.equals(unit.getPosition())).toList();
        for (Entity unit : victims) {
            int side = attacker.getPosition() == null ? ToHitData.SIDE_FRONT : unit.sideTable(attacker.getPosition());
            HitData hit = unit.rollHitLocation(ToHitData.HIT_NORMAL, side);
            reports.addAll(gameManager.damageEntity(unit, hit, damage));
            gameManager.entityUpdate(unit.getId());
        }
    }
}
