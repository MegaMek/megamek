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

import java.util.List;
import java.util.Vector;

import megamek.common.Hex;
import megamek.common.HitData;
import megamek.common.Report;
import megamek.common.ToHitData;
import megamek.common.actions.ChargeAttackAction;
import megamek.common.annotations.Nullable;
import megamek.common.board.Coords;
import megamek.common.compute.Compute;
import megamek.common.enums.HitDamageType;
import megamek.common.game.Game;
import megamek.common.game.IGame;
import megamek.common.options.OptionsConstants;
import megamek.common.rolls.PilotingRollData;
import megamek.common.rolls.Roll;
import megamek.common.units.Dropship;
import megamek.common.units.Entity;
import megamek.common.units.EntityMovementMode;
import megamek.common.units.IBuilding;
import megamek.common.units.Infantry;
import megamek.common.units.LargeSupportTank;
import megamek.common.units.PilotSPAHelper;
import megamek.common.units.Tank;
import megamek.common.units.Terrains;
import megamek.common.units.VTOL;

/**
 * Resolves crashes, forced landings and crash explosions of airborne VTOL and WiGE vehicles (TW pp.54-56, 68, 196-199).
 * Moved out of {@link TWGameManager} unchanged; the methods return their reports rather than adding them, so callers
 * keep control of report order.
 */
class AirborneVehicleCrashHandler extends AbstractTWRuleHandler {

    /** The extra MP a WiGE pays per hex to hold its elevation over lower terrain (TW p.55). */
    static final int KEEP_ELEVATION_MP = 2;

    AirborneVehicleCrashHandler(TWGameManager gameManager) {
        super(gameManager);
    }

    /**
     * TW p.67: a failed sideslip moves the unit a number of hexes equal to the Margin of Failure, but never more than
     * one less than the number of hexes it entered this turn before the sideslip. Example: a unit that moved 3 hexes and
     * fails by 4 or more sideslips 2 hexes.
     *
     * @param marginOfFailure        the Driving Skill Roll's margin of failure
     * @param hexesEnteredBeforeSlip the hexes the unit entered this turn before the sideslip
     *
     * @return the number of hexes to sideslip, never negative
     */
    static int sideslipDistanceCap(int marginOfFailure, int hexesEnteredBeforeSlip) {
        return Math.max(0, Math.min(marginOfFailure, hexesEnteredBeforeSlip - 1));
    }

    /**
     * TW p.68: a VTOL or WiGE that crashes while sideslipping takes damage equal to the number of hexes it moved that
     * turn times its tonnage, divided by 10 (rounded up). The hexes moved include the sideslipped hexes and the hex it
     * crashed in.
     *
     * @param tonnage    the vehicle's tonnage
     * @param hexesMoved the hexes the vehicle moved this turn, including sideslipped hexes and the crash hex
     *
     * @return the crash damage, never negative
     */
    static int sideslipCrashDamage(double tonnage, int hexesMoved) {
        return (int) Math.ceil((Math.max(0, hexesMoved) * tonnage) / 10.0);
    }

    /**
     * The level of the top of a unit standing in a hex, as TW counts it (TW p.68 "the level of the underlying terrain,
     * plus the level of the unit"; unit heights from TW p.99). MegaMek heights are one lower than TW's: a vehicle is 0
     * levels tall in MegaMek and 1 in TW.
     *
     * @param hex  the hex the unit stands in
     * @param unit the unit
     *
     * @return the unit's top level for sideslip collisions
     */
    static int sideslipCollisionLevel(Hex hex, Entity unit) {
        return hex.getLevel() + unit.relHeight() + 1;
    }

    /**
     * TW p.68: a sideslipping VTOL or WiGE crashes into a building whose level is at or higher than its own, and charges
     * it.
     *
     * @param nextHex      the hex the vehicle is sideslipping into
     * @param slipAltitude the vehicle's altitude (absolute level) while sideslipping
     *
     * @return {@code true} if a building in the hex reaches the vehicle
     */
    static boolean isBuildingInSideslipPath(Hex nextHex, int slipAltitude) {
        return nextHex.containsTerrain(Terrains.BLDG_ELEV)
              && ((nextHex.getLevel() + nextHex.terrainLevel(Terrains.BLDG_ELEV)) >= slipAltitude);
    }

    /**
     * Finds a grounded DropShip or Large Support Vehicle in the hex that reaches a sideslipping VTOL or WiGE (TW p.68).
     *
     * @param nextHex      the hex the vehicle is sideslipping into
     * @param occupants    the units in that hex
     * @param slipAltitude the vehicle's altitude (absolute level) while sideslipping
     *
     * @return the unit it collides with, or {@code null} if none
     */
    static @Nullable Entity findLargeUnitInSideslipPath(Hex nextHex, List<Entity> occupants, int slipAltitude) {
        for (Entity occupant : occupants) {
            boolean isLargeUnit = (occupant instanceof Dropship) || (occupant instanceof LargeSupportTank);
            if (isLargeUnit && !occupant.isAirborne()
                  && (sideslipCollisionLevel(nextHex, occupant) >= slipAltitude)) {
                return occupant;
            }
        }
        return null;
    }

    /**
     * TW p.68: a VTOL or WiGE that sideslips into a hex where it could charge an infantry unit drops to the level of
     * that hex's terrain and crashes.
     *
     * @param nextHex      the hex the vehicle is sideslipping into
     * @param occupants    the units in that hex
     * @param slipAltitude the vehicle's altitude (absolute level) while sideslipping
     *
     * @return {@code true} if an infantry unit in the hex reaches the vehicle
     */
    static boolean isInfantryInSideslipPath(Hex nextHex, List<Entity> occupants, int slipAltitude) {
        for (Entity occupant : occupants) {
            if ((occupant instanceof Infantry) && (sideslipCollisionLevel(nextHex, occupant) >= slipAltitude)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Resolves an airborne VTOL or WiGE sideslipping into a building, a DropShip or Large Support Vehicle, or an infantry
     * unit (TW p.68). The vehicle charges the building or large unit, then crashes in the hex it was sliding out of, as
     * a skidding unit stops in the hex before the obstacle.
     *
     * @param tank         the sideslipping VTOL or WiGE, currently at {@code curPos}
     * @param curPos       the hex the vehicle is sliding out of
     * @param nextPos      the hex it is sliding into
     * @param nextHex      the hex at {@code nextPos}
     * @param occupants    the units in {@code nextPos}
     * @param slipAltitude the vehicle's altitude (absolute level) while sideslipping
     * @param direction    the direction of the sideslip
     * @param impactSide   the side of the vehicle that hits the obstacle
     * @param hexesMoved   the hexes moved this turn, including sideslipped hexes and the crash hex
     *
     * @return the reports, or {@code null} if nothing in {@code nextPos} stops the vehicle
     */
    @Nullable
    Vector<Report> resolveSideslipCollision(Tank tank, Coords curPos, Coords nextPos, Hex nextHex,
          List<Entity> occupants, int slipAltitude, int direction, int impactSide, int hexesMoved) {
        Vector<Report> reports = new Vector<>();
        int fallElevation = tank.getElevation();
        if (isBuildingInSideslipPath(nextHex, slipAltitude)) {
            IBuilding building = getGame().getBoard(tank).getBuildingAt(nextPos);
            reports.add(sideslipObstacleReport(tank, building.getName(), nextPos));
            int chargeDamage = ChargeAttackAction.getDamageFor(tank,
                  getGame().getOptions().booleanOption(OptionsConstants.ADVANCED_COMBAT_TAC_OPS_CHARGE_DAMAGE),
                  tank.delta_distance);
            Vector<Report> buildingReports = gameManager.damageBuilding(building,
                  ChargeAttackAction.getBuildingChargeDamage(tank, chargeDamage), nextPos);
            for (Report report : buildingReports) {
                report.subject = tank.getId();
            }
            reports.addAll(buildingReports);
            HitData hit = tank.rollHitLocation(ToHitData.HIT_NORMAL, impactSide);
            hit.setGeneralDamageType(HitDamageType.DAMAGE_PHYSICAL_NONATTACK);
            reports.addAll(gameManager.damageEntity(tank, hit,
                  ChargeAttackAction.getDamageTakenBy(tank, building, nextPos)));
            if (building.getCurrentCF(nextPos) <= 0) {
                gameManager.checkForCollapse(building, nextPos, true, gameManager.getMainPhaseReport());
            }
        } else {
            Entity largeUnit = findLargeUnitInSideslipPath(nextHex, occupants, slipAltitude);
            if (largeUnit != null) {
                reports.add(sideslipObstacleReport(tank, largeUnit.getShortName(), nextPos));
                ChargeAttackAction charge = new ChargeAttackAction(tank.getId(), largeUnit.getTargetType(),
                      largeUnit.getId(), largeUnit.getPosition());
                gameManager.resolveChargeDamage(tank, largeUnit, charge.toHit(getGame(), true), direction);
            } else if (isInfantryInSideslipPath(nextHex, occupants, slipAltitude)) {
                Report report = new Report(2052);
                report.subject = tank.getId();
                report.indent();
                report.add(nextPos.getBoardNum(), true);
                reports.add(report);
            } else {
                return null;
            }
        }
        if (!tank.isDoomed()) {
            reports.addAll(crashVTOLorWiGE(tank, false, true, hexesMoved, curPos, tank.getElevation(), impactSide));
            reports.addAll(resolveSideslipCrashAftermath(tank, curPos, fallElevation, direction));
        }
        return reports;
    }

    /**
     * TW p.68: a VTOL or WiGE that survives a sideslip crash has landed if it can normally land in the crash hex, and
     * is destroyed otherwise. A WiGE lands in clear, paved or water hexes (TW p.55); a VTOL lands in clear or paved
     * hexes, or on a building roof (TW p.54).
     *
     * @param tank the crashed VTOL or WiGE
     * @param hex  the hex it crashed in
     *
     * @return {@code true} if the vehicle can land in the hex
     */
    static boolean canLandAfterSideslipCrash(Tank tank, Hex hex) {
        if (hex.isClearForTakeoff()) {
            return true;
        }
        if (tank.getMovementMode() == EntityMovementMode.WIGE) {
            return hex.containsTerrain(Terrains.WATER);
        }
        return hex.containsTerrain(Terrains.BLDG_ELEV);
    }

    /**
     * TW p.68: a WiGE that sideslips toward a drop that would make a ground vehicle fall can avoid the fall if it has
     * the MP to hold its elevation (+2 MP, TW p.55). A VTOL keeps its altitude and never falls.
     *
     * @param tank the sideslipping VTOL or WiGE; its {@code mpUsed} holds the MP spent so far this turn
     *
     * @return {@code true} if the vehicle does not fall
     */
    static boolean canAvoidSideslipFall(Tank tank) {
        if (tank.getMovementMode() != EntityMovementMode.WIGE) {
            return true;
        }
        return (tank.getRunMP() - tank.mpUsed) >= KEEP_ELEVATION_MP;
    }

    /**
     * Finishes a sideslip crash (TW p.68). A unit on the ground in the crash hex is hit as an accidental fall from above
     * (TW p.152); if that misses, the vehicle comes down in a valid adjacent hex instead. The vehicle is then destroyed
     * unless it can land in the hex it ended in.
     *
     * @param tank          the crashed VTOL or WiGE, already in {@code crashPos}
     * @param crashPos      the hex it crashed in
     * @param fallElevation the elevation it fell from
     * @param direction     the direction of the sideslip
     *
     * @return the reports
     */
    Vector<Report> resolveSideslipCrashAftermath(Tank tank, Coords crashPos, int fallElevation, int direction) {
        Vector<Report> reports = new Vector<>();
        Entity fallenOn = getGame().getAFFATarget(crashPos, tank);
        if ((fallenOn != null) && (fallElevation > 0) && !tank.isDoomed()) {
            Report report = new Report(2054);
            report.subject = tank.getId();
            report.indent();
            report.addDesc(fallenOn);
            report.add(crashPos.getBoardNum(), true);
            reports.add(report);
            if (gameManager.resolveAccidentalFallFromAboveHit(tank, fallenOn, fallElevation, reports)) {
                gameManager.displaceUnitFallenOn(tank, crashPos, direction, reports);
            } else {
                Coords landing = Compute.getValidDisplacement(getGame(), tank.getId(), crashPos, direction);
                if (landing != null) {
                    tank.setPosition(landing);
                } else {
                    reports.addAll(gameManager.destroyEntity(tank, "impossible displacement", false, false));
                }
            }
        }
        if (!tank.isDoomed()
              && !canLandAfterSideslipCrash(tank, getGame().getHex(tank.getPosition(), tank.getBoardId()))) {
            reports.addAll(gameManager.destroyEntity(tank, "could not land in crash site"));
        }
        return reports;
    }

    private static Report sideslipObstacleReport(Tank tank, String obstacle, Coords nextPos) {
        Report report = new Report(2051);
        report.subject = tank.getId();
        report.indent();
        report.add(obstacle, true);
        report.add(nextPos.getBoardNum(), true);
        return report;
    }

    /**
     * Resolves the forced landing of one airborne {@code VTOL} or {@code WiGE} in its current hex. As this method is
     * only for internal use and not part of the exported public API, it simply relies on its client code to only ever
     * hand it a valid airborne vehicle and does not run any further checks of its own.
     *
     * @param tank The {@code VTOL} or {@code WiGE} in question.
     *
     * @return The resulting {@code Vector} of {@code Report}s.
     */
    Vector<Report> forceLandVTOLorWiGE(Tank tank) {
        Vector<Report> reports = new Vector<>();
        PilotingRollData pilotingRoll = tank.getBasePilotingRoll();
        Hex hex = getGame().getBoard().getHex(tank.getPosition());

        if (tank instanceof VTOL) {
            pilotingRoll.addModifier(4, "VTOL making forced landing");
        } else {
            pilotingRoll.addModifier(0, "WiGE making forced landing");
        }

        if (tank.hasAbility(OptionsConstants.PILOT_WIND_WALKER) && PilotSPAHelper.isWindWalkerValid(tank)) {
            pilotingRoll.addModifier(-1, "Wind Walker SPA");
        }

        int elevation = Math.max(hex.terrainLevel(Terrains.BLDG_ELEV), hex.terrainLevel(Terrains.BRIDGE_ELEV));
        elevation = Math.max(elevation, 0);
        elevation = Math.min(elevation, tank.getElevation());
        if (tank.getElevation() > elevation) {
            if (!hex.containsTerrain(Terrains.FUEL_TANK) &&
                  !hex.containsTerrain(Terrains.JUNGLE) &&
                  !hex.containsTerrain(Terrains.MAGMA) &&
                  !hex.containsTerrain(Terrains.MUD) &&
                  !hex.containsTerrain(Terrains.RUBBLE) &&
                  !hex.containsTerrain(Terrains.WATER) &&
                  !hex.containsTerrain(Terrains.WOODS)) {
                Report report = new Report(2180);
                report.subject = tank.getId();
                report.addDesc(tank);
                report.add(pilotingRoll.getLastPlainDesc(), true);
                reports.add(report);

                // roll
                final Roll diceRoll = Compute.rollD6(2);
                report = new Report(2185);
                report.subject = tank.getId();
                report.add(pilotingRoll.getValueAsString());
                report.add(pilotingRoll.getDesc());
                report.add(diceRoll);

                if (diceRoll.getIntValue() < pilotingRoll.getValue()) {
                    report.choose(false);
                    reports.add(report);
                    reports.addAll(crashVTOLorWiGE(tank, true));
                } else {
                    report.choose(true);
                    reports.add(report);
                    tank.setElevation(elevation);
                }
            } else {
                reports.addAll(crashVTOLorWiGE(tank, true));
            }
        }
        return reports;
    }

    /**
     * Crash a VTOL
     *
     * @param tank the <code>VTOL</code> to be crashed
     *
     * @return the <code>Vector<Report></code> containing phase reports
     */
    Vector<Report> crashVTOLorWiGE(Tank tank) {
        return crashVTOLorWiGE(tank, false, false, 0, tank.getPosition(), tank.getElevation(), 0);
    }

    /**
     * Crash a VTOL or WiGE.
     *
     * @param tank              The {@code VTOL} or {@code WiGE} to crash.
     * @param rerollRotorHits Whether any rotor hits from the crash should be rerolled, typically after a "rotor
     *                        destroyed" critical hit.
     *
     * @return The {@code Vector<Report>} of resulting reports.
     */
    Vector<Report> crashVTOLorWiGE(Tank tank, boolean rerollRotorHits) {
        return crashVTOLorWiGE(tank, rerollRotorHits, false, 0, tank.getPosition(), tank.getElevation(), 0);
    }

    /**
     * Crash a VTOL or WiGE.
     *
     * @param tank              The {@code VTOL} or {@code WiGE} to crash.
     * @param rerollRotorHits Whether any rotor hits from the crash should be rerolled, typically after a "rotor
     *                        destroyed" critical hit.
     * @param sideSlipCrash   A <code>boolean</code> value indicating whether this is a sideslip crash or not.
     * @param hexesMoved      For a sideslip crash, the hexes moved this turn, including sideslipped hexes and the
     *                        crash hex.
     * @param crashPos        The <code>Coords</code> of the crash
     * @param crashElevation  The <code>int</code> elevation of the VTOL
     * @param impactSide      The <code>int</code> describing the side on which the VTOL falls
     *
     * @return a <code>Vector<Report></code> of Reports.
     */

    Vector<Report> crashVTOLorWiGE(Tank tank, boolean rerollRotorHits, boolean sideSlipCrash, int hexesMoved,
          Coords crashPos, int crashElevation, int impactSide) {
        Vector<Report> reports = new Vector<>();
        Report report;

        // we might be off the board after a DFA, so return then
        if (!getGame().getBoard().contains(crashPos)) {
            return reports;
        }

        if (!sideSlipCrash) {
            // report lost movement and crashing
            report = new Report(6260);
            report.subject = tank.getId();
            report.newlines = 0;
            report.addDesc(tank);
            reports.addElement(report);
            int newElevation = 0;
            Hex fallHex = getGame().getBoard().getHex(crashPos);

            // May land on roof of building or bridge
            if (fallHex.containsTerrain(Terrains.BLDG_ELEV)) {
                newElevation = fallHex.terrainLevel(Terrains.BLDG_ELEV);
            } else if (fallHex.containsTerrain(Terrains.BRIDGE_ELEV)) {
                newElevation = fallHex.terrainLevel(Terrains.BRIDGE_ELEV);
                if (newElevation > crashElevation) {
                    newElevation = 0; // vtol was under bridge already
                }
            }

            int fall = crashElevation - newElevation;
            if (fall == 0) {
                // already on ground, no harm done
                report = new Report(6265);
                report.subject = tank.getId();
                reports.addElement(report);
                return reports;
            }
            // set elevation 1st to avoid multiple crashes
            tank.setElevation(newElevation);

            // plummets to ground
            report = new Report(6270);
            report.subject = tank.getId();
            report.add(fall);
            reports.addElement(report);

            // facing after fall
            String side;
            int table;
            int facing = Game.rulesManager.getRulesCharts().getFacingForFall();
            table = switch (facing) {
                case 1, 2 -> {
                    side = "right side";
                    yield ToHitData.SIDE_RIGHT;
                }
                case 3 -> {
                    side = "rear";
                    yield ToHitData.SIDE_REAR;
                }
                case 4, 5 -> {
                    side = "left side";
                    yield ToHitData.SIDE_LEFT;
                }
                default -> {
                    side = "front";
                    yield ToHitData.SIDE_FRONT;
                }
            };

            if (newElevation <= 0) {
                boolean waterFall = fallHex.containsTerrain(Terrains.WATER);
                if (waterFall && fallHex.containsTerrain(Terrains.ICE)) {
                    Roll diceRoll = Compute.rollD6(1);
                    report = new Report(2119);
                    report.subject = tank.getId();
                    report.addDesc(tank);
                    report.add(diceRoll);
                    report.subject = tank.getId();
                    reports.add(report);
                    if (diceRoll.getIntValue() > 3) {
                        reports.addAll(gameManager.resolveIceBroken(crashPos));
                    } else {
                        waterFall = false; // saved by ice
                    }
                }
                if (waterFall) {
                    // falls into water and is destroyed
                    report = new Report(6275);
                    report.subject = tank.getId();
                    reports.addElement(report);
                    reports.addAll(gameManager.destroyEntity(tank, "Fell into water", false, false));
                    // not sure, is this salvageable?
                }
            }

            // calculate damage for hitting the surface
            int damage = (int) Math.round(tank.getWeight() / 10.0) * (fall + 1);

            // adjust damage for gravity
            damage = Math.round(damage * getGame().getPlanetaryConditions().getGravity());
            // report falling
            report = new Report(6280);
            report.subject = tank.getId();
            report.indent();
            report.addDesc(tank);
            report.add(side);
            report.add(damage);
            // report.newlines = 0;
            reports.addElement(report);

            tank.setFacing((tank.getFacing() + (facing)) % 6);

            boolean exploded = false;

            // standard damage loop
            while (damage > 0) {
                int cluster = Math.min(5, damage);
                HitData hit = tank.rollHitLocation(ToHitData.HIT_NORMAL, table);
                if ((tank instanceof VTOL) && (hit.getLocation() == VTOL.LOC_ROTOR) && rerollRotorHits) {
                    continue;
                }
                hit.setGeneralDamageType(HitDamageType.DAMAGE_PHYSICAL);
                int[] isBefore = { tank.getInternal(Tank.LOC_FRONT), tank.getInternal(Tank.LOC_RIGHT),
                                   tank.getInternal(Tank.LOC_LEFT), tank.getInternal(Tank.LOC_REAR) };
                reports.addAll(gameManager.damageEntity(tank, hit, cluster));
                int[] isAfter = { tank.getInternal(Tank.LOC_FRONT), tank.getInternal(Tank.LOC_RIGHT),
                                  tank.getInternal(Tank.LOC_LEFT), tank.getInternal(Tank.LOC_REAR) };
                for (int x = 0; x <= 3; x++) {
                    if (isBefore[x] != isAfter[x]) {
                        exploded = true;
                        break;
                    }
                }
                damage -= cluster;
            }
            if (exploded) {
                report = new Report(6285);
                report.subject = tank.getId();
                report.addDesc(tank);
                reports.addElement(report);
                reports.addAll(explodeVTOLorWiGE(tank));
            }

            // check for location exposure
            reports.addAll(gameManager.doSetLocationsExposure(tank, fallHex, false, newElevation));

        } else {
            tank.setElevation(0);// considered landed in the hex.
            // TW p.68: the vehicle may not attack in the turn it crashes
            tank.setCrashedThisTurn(true);
            // crashes into ground thanks to sideslip
            report = new Report(6290);
            report.subject = tank.getId();
            report.addDesc(tank);
            reports.addElement(report);
            int damage = sideslipCrashDamage(tank.getWeight(), hexesMoved);
            boolean exploded = false;

            // standard damage loop
            while (damage > 0) {
                int cluster = Math.min(5, damage);
                HitData hit = tank.rollHitLocation(ToHitData.HIT_NORMAL, impactSide);
                hit.setGeneralDamageType(HitDamageType.DAMAGE_PHYSICAL);
                int[] isBefore = { tank.getInternal(Tank.LOC_FRONT), tank.getInternal(Tank.LOC_RIGHT),
                                   tank.getInternal(Tank.LOC_LEFT), tank.getInternal(Tank.LOC_REAR) };
                reports.addAll(gameManager.damageEntity(tank, hit, cluster));
                int[] isAfter = { tank.getInternal(Tank.LOC_FRONT), tank.getInternal(Tank.LOC_RIGHT),
                                  tank.getInternal(Tank.LOC_LEFT), tank.getInternal(Tank.LOC_REAR) };
                for (int x = 0; x <= 3; x++) {
                    if (isBefore[x] != isAfter[x]) {
                        exploded = true;
                        break;
                    }
                }
                damage -= cluster;
            }
            if (exploded) {
                report = new Report(6295);
                report.subject = tank.getId();
                report.addDesc(tank);
                reports.addElement(report);
                reports.addAll(explodeVTOLorWiGE(tank));
            }

        }

        if (getGame().containsMinefield(crashPos)) {
            // may set off any minefields in the hex
            gameManager.enterMinefield(tank, crashPos, 0, true, reports, 7);
            // it may also clear any minefields that it detonated
            gameManager.clearDetonatedMines(crashPos, 5);
            gameManager.resetMines();
        }

        return reports;

    }

    /**
     * Explodes a VTOL or WiGE unit.
     *
     * @param entity The unit to explode.
     *
     * @return The new reports created by the events
     */
    private Vector<Report> explodeVTOLorWiGE(@Nullable Tank entity) {
        Vector<Report> newReports = new Vector<>();
        if (entity == null) {
            IGame.LOGGER.error("Tried to explode null entity");
            return newReports;

        } else if (entity.hasEngine() && entity.getEngine().isFusion()) {
            // fusion engine, no effect
            newReports.addElement(new Report(6300).subject(entity.getId()));

        } else {
            if (getGame().hasBoardLocationOf(entity)) {
                Hex hex = getGame().getHexOf(entity);
                int fireTerrain = hex.hasVegetation() ? Terrains.FIRE_LVL_NORMAL : Terrains.FIRE_LVL_INFERNO;
                gameManager.ignite(entity.getPosition(), entity.getBoardId(), fireTerrain, newReports);
            }
            newReports.addAll(gameManager.destroyEntity(entity, "crashed and burned", false, false));
        }
        return newReports;
    }
}
