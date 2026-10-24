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
package megamek.common.weapons.handlers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Vector;
import java.util.stream.Stream;

import jakarta.annotation.Nullable;
import megamek.common.Hex;
import megamek.common.Player;
import megamek.common.Report;
import megamek.common.board.Board;
import megamek.common.equipment.AmmoType;
import megamek.common.equipment.AmmoType.AmmoTypeEnum;
import megamek.common.equipment.EquipmentType;
import megamek.common.game.Game;
import megamek.common.planetaryConditions.Atmosphere;
import megamek.common.units.BipedMek;
import megamek.common.units.Crew;
import megamek.common.units.CrewType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Fuel-air artillery ammo (issue #9317): the atmosphere rule shared by every fuel-air attack, the Clan Arrow IV
 * fuel-air round, and the prototype Arrow IV round's radius.
 */
class FuelAirArtilleryTest {

    private static final int REPORT_NO_EFFECT = 9986;
    private static final int REPORT_THIN_AIR = 9990;

    private Game game;
    private BipedMek attacker;

    @BeforeAll
    static void beforeAll() {
        EquipmentType.initializeTypes();
    }

    @BeforeEach
    void beforeEach() {
        game = new Game();
        Board board = new Board(4, 4, Stream.generate(Hex::new).limit(16).toArray(Hex[]::new));
        game.setBoard(0, board);
        Player owner = new Player(0, "Gunner");
        game.addPlayer(0, owner);
        attacker = new BipedMek();
        attacker.setGame(game);
        attacker.setId(game.getNextEntityId());
        attacker.setCrew(new Crew(CrewType.SINGLE));
        attacker.setOwner(owner);
    }

    private static @Nullable AmmoType fuelAirRound(AmmoTypeEnum launcher, boolean clan) {
        for (AmmoType ammo : AmmoType.getMunitionsFor(launcher)) {
            if ((ammo.isClan() == clan) && ammo.getMunitionType().contains(AmmoType.Munitions.M_FAE)) {
                return ammo;
            }
        }
        return null;
    }

    private static int count(Vector<Report> reports, int messageId) {
        int found = 0;
        for (Report report : reports) {
            if (report.messageId == messageId) {
                found++;
            }
        }
        return found;
    }

    @Test
    void fuelAirDoesNothingInVacuumOrTrace() {
        for (Atmosphere atmosphere : new Atmosphere[] { Atmosphere.VACUUM, Atmosphere.TRACE }) {
            game.getPlanetaryConditions().setAtmosphere(atmosphere);
            Vector<Report> reports = new Vector<>();

            assertTrue(AreaEffectHelper.fuelAirHasNoEffect(game, 0, attacker, reports), atmosphere + ": no effect");
            assertEquals(1, count(reports, REPORT_NO_EFFECT), atmosphere + ": the report says why");
        }
    }

    @Test
    void fuelAirWorksInThinAtHalfDamageRoundedUp() {
        game.getPlanetaryConditions().setAtmosphere(Atmosphere.THIN);
        Vector<Report> reports = new Vector<>();

        assertFalse(AreaEffectHelper.fuelAirHasNoEffect(game, 0, attacker, reports), "thin air still burns");
        assertEquals(3, AreaEffectHelper.fuelAirDamageInAtmosphere(game, 5, attacker, reports), "5 halved rounds up to 3");
        assertEquals(1, count(reports, REPORT_THIN_AIR), "the halving is reported");
    }

    @Test
    void fuelAirDoesFullDamageInStandardAtmosphere() {
        game.getPlanetaryConditions().setAtmosphere(Atmosphere.STANDARD);
        Vector<Report> reports = new Vector<>();

        assertFalse(AreaEffectHelper.fuelAirHasNoEffect(game, 0, attacker, reports));
        assertEquals(20, AreaEffectHelper.fuelAirDamageInAtmosphere(game, 20, attacker, reports));
        assertTrue(reports.isEmpty(), "nothing to report");
    }

    /** IO:AE p.160: all fuel-air values are identical for Clan and Inner Sphere versions. */
    @Test
    void clanArrowIvHasAFuelAirRoundWithTheInnerSphereRadius() {
        AmmoType clanRound = fuelAirRound(AmmoTypeEnum.ARROW_IV, true);

        assertNotNull(clanRound, "the Clan Arrow IV can load fuel-air");
        assertEquals(2, AreaEffectHelper.getFuelAirBlastRadiusIndex(clanRound.getInternalName()), "radius 2");
    }

    @Test
    void prototypeArrowIvFuelAirRoundHasRadiusTwo() {
        AmmoType prototypeRound = fuelAirRound(AmmoTypeEnum.ARROWIV_PROTO, false);

        assertNotNull(prototypeRound, "the prototype Arrow IV can load fuel-air");
        assertEquals(2, AreaEffectHelper.getFuelAirBlastRadiusIndex(prototypeRound.getInternalName()), "radius 2");
    }
}
