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

import megamek.common.Hex;
import megamek.common.HitData;
import megamek.common.Report;
import megamek.common.ToHitData;
import megamek.common.annotations.Nullable;
import megamek.common.board.Coords;
import megamek.common.compute.Compute;
import megamek.common.enums.HitDamageType;
import megamek.common.game.Game;
import megamek.common.game.IGame;
import megamek.common.options.OptionsConstants;
import megamek.common.rolls.PilotingRollData;
import megamek.common.rolls.Roll;
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
     * Resolves the forced landing of one airborne {@code VTOL} or {@code WiGE} in its current hex. As this method is
     * only for internal use and not part of the exported public API, it simply relies on its client code to only ever
     * hand it a valid airborne vehicle and does not run any further checks of its own.
     *
     * @param en The {@code VTOL} or {@code WiGE} in question.
     *
     * @return The resulting {@code Vector} of {@code Report}s.
     */
    Vector<Report> forceLandVTOLorWiGE(Tank en) {
        Vector<Report> vDesc = new Vector<>();
        PilotingRollData psr = en.getBasePilotingRoll();
        Hex hex = getGame().getBoard().getHex(en.getPosition());

        if (en instanceof VTOL) {
            psr.addModifier(4, "VTOL making forced landing");
        } else {
            psr.addModifier(0, "WiGE making forced landing");
        }

        if (en.hasAbility(OptionsConstants.PILOT_WIND_WALKER) && PilotSPAHelper.isWindWalkerValid(en)) {
            psr.addModifier(-1, "Wind Walker SPA");
        }

        int elevation = Math.max(hex.terrainLevel(Terrains.BLDG_ELEV), hex.terrainLevel(Terrains.BRIDGE_ELEV));
        elevation = Math.max(elevation, 0);
        elevation = Math.min(elevation, en.getElevation());
        if (en.getElevation() > elevation) {
            if (!hex.containsTerrain(Terrains.FUEL_TANK) &&
                  !hex.containsTerrain(Terrains.JUNGLE) &&
                  !hex.containsTerrain(Terrains.MAGMA) &&
                  !hex.containsTerrain(Terrains.MUD) &&
                  !hex.containsTerrain(Terrains.RUBBLE) &&
                  !hex.containsTerrain(Terrains.WATER) &&
                  !hex.containsTerrain(Terrains.WOODS)) {
                Report r = new Report(2180);
                r.subject = en.getId();
                r.addDesc(en);
                r.add(psr.getLastPlainDesc(), true);
                vDesc.add(r);

                // roll
                final Roll diceRoll = Compute.rollD6(2);
                r = new Report(2185);
                r.subject = en.getId();
                r.add(psr.getValueAsString());
                r.add(psr.getDesc());
                r.add(diceRoll);

                if (diceRoll.getIntValue() < psr.getValue()) {
                    r.choose(false);
                    vDesc.add(r);
                    vDesc.addAll(crashVTOLorWiGE(en, true));
                } else {
                    r.choose(true);
                    vDesc.add(r);
                    en.setElevation(elevation);
                }
            } else {
                vDesc.addAll(crashVTOLorWiGE(en, true));
            }
        }
        return vDesc;
    }

    /**
     * Crash a VTOL
     *
     * @param en the <code>VTOL</code> to be crashed
     *
     * @return the <code>Vector<Report></code> containing phase reports
     */
    Vector<Report> crashVTOLorWiGE(Tank en) {
        return crashVTOLorWiGE(en, false, false, 0, en.getPosition(), en.getElevation(), 0);
    }

    /**
     * Crash a VTOL or WiGE.
     *
     * @param en              The {@code VTOL} or {@code WiGE} to crash.
     * @param rerollRotorHits Whether any rotor hits from the crash should be rerolled, typically after a "rotor
     *                        destroyed" critical hit.
     *
     * @return The {@code Vector<Report>} of resulting reports.
     */
    Vector<Report> crashVTOLorWiGE(Tank en, boolean rerollRotorHits) {
        return crashVTOLorWiGE(en, rerollRotorHits, false, 0, en.getPosition(), en.getElevation(), 0);
    }

    /**
     * Crash a VTOL or WiGE.
     *
     * @param en              The {@code VTOL} or {@code WiGE} to crash.
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

    Vector<Report> crashVTOLorWiGE(Tank en, boolean rerollRotorHits, boolean sideSlipCrash, int hexesMoved,
          Coords crashPos, int crashElevation, int impactSide) {
        Vector<Report> vDesc = new Vector<>();
        Report r;

        // we might be off the board after a DFA, so return then
        if (!getGame().getBoard().contains(crashPos)) {
            return vDesc;
        }

        if (!sideSlipCrash) {
            // report lost movement and crashing
            r = new Report(6260);
            r.subject = en.getId();
            r.newlines = 0;
            r.addDesc(en);
            vDesc.addElement(r);
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
                r = new Report(6265);
                r.subject = en.getId();
                vDesc.addElement(r);
                return vDesc;
            }
            // set elevation 1st to avoid multiple crashes
            en.setElevation(newElevation);

            // plummets to ground
            r = new Report(6270);
            r.subject = en.getId();
            r.add(fall);
            vDesc.addElement(r);

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
                    r = new Report(2119);
                    r.subject = en.getId();
                    r.addDesc(en);
                    r.add(diceRoll);
                    r.subject = en.getId();
                    vDesc.add(r);
                    if (diceRoll.getIntValue() > 3) {
                        vDesc.addAll(gameManager.resolveIceBroken(crashPos));
                    } else {
                        waterFall = false; // saved by ice
                    }
                }
                if (waterFall) {
                    // falls into water and is destroyed
                    r = new Report(6275);
                    r.subject = en.getId();
                    vDesc.addElement(r);
                    vDesc.addAll(gameManager.destroyEntity(en, "Fell into water", false, false));
                    // not sure, is this salvageable?
                }
            }

            // calculate damage for hitting the surface
            int damage = (int) Math.round(en.getWeight() / 10.0) * (fall + 1);

            // adjust damage for gravity
            damage = Math.round(damage * getGame().getPlanetaryConditions().getGravity());
            // report falling
            r = new Report(6280);
            r.subject = en.getId();
            r.indent();
            r.addDesc(en);
            r.add(side);
            r.add(damage);
            // r.newlines = 0;
            vDesc.addElement(r);

            en.setFacing((en.getFacing() + (facing)) % 6);

            boolean exploded = false;

            // standard damage loop
            while (damage > 0) {
                int cluster = Math.min(5, damage);
                HitData hit = en.rollHitLocation(ToHitData.HIT_NORMAL, table);
                if ((en instanceof VTOL) && (hit.getLocation() == VTOL.LOC_ROTOR) && rerollRotorHits) {
                    continue;
                }
                hit.setGeneralDamageType(HitDamageType.DAMAGE_PHYSICAL);
                int[] isBefore = { en.getInternal(Tank.LOC_FRONT), en.getInternal(Tank.LOC_RIGHT),
                                   en.getInternal(Tank.LOC_LEFT), en.getInternal(Tank.LOC_REAR) };
                vDesc.addAll(gameManager.damageEntity(en, hit, cluster));
                int[] isAfter = { en.getInternal(Tank.LOC_FRONT), en.getInternal(Tank.LOC_RIGHT),
                                  en.getInternal(Tank.LOC_LEFT), en.getInternal(Tank.LOC_REAR) };
                for (int x = 0; x <= 3; x++) {
                    if (isBefore[x] != isAfter[x]) {
                        exploded = true;
                        break;
                    }
                }
                damage -= cluster;
            }
            if (exploded) {
                r = new Report(6285);
                r.subject = en.getId();
                r.addDesc(en);
                vDesc.addElement(r);
                vDesc.addAll(explodeVTOLorWiGE(en));
            }

            // check for location exposure
            vDesc.addAll(gameManager.doSetLocationsExposure(en, fallHex, false, newElevation));

        } else {
            en.setElevation(0);// considered landed in the hex.
            // crashes into ground thanks to sideslip
            r = new Report(6290);
            r.subject = en.getId();
            r.addDesc(en);
            vDesc.addElement(r);
            int damage = sideslipCrashDamage(en.getWeight(), hexesMoved);
            boolean exploded = false;

            // standard damage loop
            while (damage > 0) {
                int cluster = Math.min(5, damage);
                HitData hit = en.rollHitLocation(ToHitData.HIT_NORMAL, impactSide);
                hit.setGeneralDamageType(HitDamageType.DAMAGE_PHYSICAL);
                int[] isBefore = { en.getInternal(Tank.LOC_FRONT), en.getInternal(Tank.LOC_RIGHT),
                                   en.getInternal(Tank.LOC_LEFT), en.getInternal(Tank.LOC_REAR) };
                vDesc.addAll(gameManager.damageEntity(en, hit, cluster));
                int[] isAfter = { en.getInternal(Tank.LOC_FRONT), en.getInternal(Tank.LOC_RIGHT),
                                  en.getInternal(Tank.LOC_LEFT), en.getInternal(Tank.LOC_REAR) };
                for (int x = 0; x <= 3; x++) {
                    if (isBefore[x] != isAfter[x]) {
                        exploded = true;
                        break;
                    }
                }
                damage -= cluster;
            }
            if (exploded) {
                r = new Report(6295);
                r.subject = en.getId();
                r.addDesc(en);
                vDesc.addElement(r);
                vDesc.addAll(explodeVTOLorWiGE(en));
            }

        }

        if (getGame().containsMinefield(crashPos)) {
            // may set off any minefields in the hex
            gameManager.enterMinefield(en, crashPos, 0, true, vDesc, 7);
            // it may also clear any minefields that it detonated
            gameManager.clearDetonatedMines(crashPos, 5);
            gameManager.resetMines();
        }

        return vDesc;

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
