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

package megamek.common.units;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.StringWriter;
import java.util.ArrayList;

import megamek.common.Player;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.EquipmentTypeLookup;
import megamek.common.game.Game;
import megamek.common.loaders.MULParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Covers writing a tractor-and-trailer train to a MUL.
 *
 * <p>The file records the train on the tractor only, as an ordered list of the ids the saving game used. Order
 * matters: it fixes the hitch chain and therefore where each trailer sits. Those ids mean nothing until the units
 * reach a game, which is where {@code TWGameManager.restoreTrains} translates them.</p>
 *
 * <p>These tests assert on the written XML rather than reading the file back, because the parser rebuilds each unit
 * from the unit cache and the vehicles built here are not cached designs. The read side is covered by
 * {@code TrainMulRestoreTest}.</p>
 */
class TrainMulRoundTripTest {

    private static final double TRACTOR_TONS = 75.0;
    private static final double CARRIAGE_TONS = 10.0;

    private Game game;
    private Player owner;

    @BeforeEach
    void setUp() {
        EquipmentType.initializeTypes();
        owner = new Player(0, "Owner");
        owner.setTeam(1);
        game = new Game();
        game.addPlayer(0, owner);
    }

    private Tank buildVehicle(double tonnage, boolean isTrailer) throws Exception {
        Tank vehicle = new Tank();
        vehicle.setId(game.getNextEntityId());
        vehicle.setOwner(owner);
        vehicle.setChassis(isTrailer ? "Test Carriage" : "Test Tractor");
        vehicle.setModel("TT-1");
        vehicle.setWeight(tonnage);
        vehicle.setMovementMode(EntityMovementMode.TRACKED);
        vehicle.setTrailer(isTrailer);
        vehicle.addEquipment(EquipmentType.get(EquipmentTypeLookup.HITCH), Tank.LOC_BODY);
        vehicle.setTrailerHitches();
        game.addEntity(vehicle);
        return vehicle;
    }

    private Tank buildTrain(int trailerCount) throws Exception {
        Tank tractor = buildVehicle(TRACTOR_TONS, false);
        for (int index = 0; index < trailerCount; index++) {
            Tank carriage = buildVehicle(CARRIAGE_TONS, true);
            tractor.towUnit(carriage.getId());
        }
        return tractor;
    }

    private String toMul(Entity entity) throws Exception {
        StringWriter writer = new StringWriter();
        ArrayList<Entity> list = new ArrayList<>();
        list.add(entity);
        EntityListFile.writeEntityList(writer, list);
        return writer.toString();
    }

    private static String towedUnitTag(int id) {
        return '<' + MULParser.ELE_TOWED_UNIT + ' ' + MULParser.ATTR_ID + "=\"" + id + "\"/>";
    }

    @Test
    void aTractorWritesTheTrailersItTows() throws Exception {
        Tank tractor = buildTrain(2);
        int leadTrailerId = tractor.getAllTowedUnits().get(0);
        int secondTrailerId = tractor.getAllTowedUnits().get(1);

        String xml = toMul(tractor);

        assertTrue(xml.contains('<' + MULParser.ELE_TOWED_UNITS + '>'), "The train element is written: " + xml);
        assertTrue(xml.contains(towedUnitTag(leadTrailerId)), "The lead trailer is listed: " + xml);
        assertTrue(xml.contains(towedUnitTag(secondTrailerId)), "The second trailer is listed: " + xml);
    }

    @Test
    void trailersAreWrittenFrontToBack() throws Exception {
        Tank tractor = buildTrain(3);
        String xml = toMul(tractor);

        int previousIndex = -1;
        for (int towedId : tractor.getAllTowedUnits()) {
            int index = xml.indexOf(towedUnitTag(towedId));
            assertTrue(index > previousIndex, "Trailer " + towedId + " is out of order in: " + xml);
            previousIndex = index;
        }
    }

    @Test
    void aUnitTowingNothingWritesNoTrainElement() throws Exception {
        Tank tractor = buildTrain(0);

        String xml = toMul(tractor);

        assertFalse(xml.contains(MULParser.ELE_TOWED_UNITS),
              "A unit towing nothing writes no TowedUnits element: " + xml);
    }

    @Test
    void aTrailerDoesNotRecordTheTrainItself() throws Exception {
        Tank tractor = buildTrain(2);
        Entity leadTrailer = game.getEntity(tractor.getAllTowedUnits().get(0));

        String xml = toMul(leadTrailer);

        assertFalse(xml.contains(MULParser.ELE_TOWED_UNITS),
              "Only the tractor records the train: " + xml);
    }

    @Test
    void everyTrailerAppearsExactlyOnce() throws Exception {
        Tank tractor = buildTrain(3);
        String xml = toMul(tractor);

        assertEquals(3, xml.split(MULParser.ELE_TOWED_UNIT + ' ', -1).length - 1,
              "Three trailers produce three entries: " + xml);
    }
}
