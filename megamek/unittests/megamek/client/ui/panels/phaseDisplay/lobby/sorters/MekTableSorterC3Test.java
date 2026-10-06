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
package megamek.client.ui.panels.phaseDisplay.lobby.sorters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import megamek.common.Player;
import megamek.common.equipment.EquipmentType;
import megamek.common.game.Game;
import megamek.common.units.BipedMek;
import megamek.common.units.Crew;
import megamek.common.units.CrewType;
import megamek.common.units.Entity;
import megamek.common.units.Mek;
import megamek.common.util.C3Util;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests that {@link MekTableSorter#keepingC3NetworksTogether(List, Comparator)} keeps a C3 network as one
 * hierarchy-ordered block under an arbitrary base sorter, which the lobby's C3 branch glyphs rely on.
 */
public class MekTableSorterC3Test {

    private Game game;
    private int nextEntityId = 1;

    @BeforeAll
    static void initializeEquipment() {
        EquipmentType.initializeTypes();
    }

    @BeforeEach
    void setUp() {
        game = new Game();
        game.addPlayer(0, new Player(0, "Test Player"));
        nextEntityId = 1;
    }

    private Entity createUnit(String equipmentName) {
        Entity entity = new BipedMek();
        entity.setGame(game);
        entity.setId(nextEntityId++);
        entity.setChassis("Test Mek");
        entity.setModel((equipmentName == null) ? "Plain" : equipmentName);
        entity.setCrew(new Crew(CrewType.SINGLE));
        entity.setOwner(game.getPlayer(0));
        entity.setWeight(50.0);
        entity.setOriginalWalkMP(5);
        if (equipmentName != null) {
            try {
                entity.addEquipment(EquipmentType.get(equipmentName), Mek.LOC_HEAD);
            } catch (Exception ex) {
                fail("Failed to add equipment: " + ex.getMessage());
            }
        }
        game.addEntity(entity);
        return entity;
    }

    @Test
    void testNetworkStaysTogetherInHierarchyOrderUnderAdversarialSort() throws Exception {
        Entity companyMaster = createUnit("ISC3MasterComputer");
        C3Util.setCompanyMaster(List.of(companyMaster));
        Entity lanceMaster = createUnit("ISC3MasterComputer");
        C3Util.connect(game, new ArrayList<>(List.of(lanceMaster)), companyMaster.getId(), false);
        Entity firstSlave = createUnit("ISC3SlaveUnit");
        Entity secondSlave = createUnit("ISC3SlaveUnit");
        C3Util.connect(game, new ArrayList<>(List.of(firstSlave, secondSlave)), lanceMaster.getId(), false);

        Entity plainEarly = createUnit(null);
        Entity plainLate = createUnit(null);

        // Adversarial base sort: descending id would normally scatter the network completely
        List<Entity> tableRows = new ArrayList<>(
              List.of(firstSlave, plainEarly, companyMaster, plainLate, secondSlave, lanceMaster));
        Comparator<Entity> descendingById = Comparator.comparingInt(Entity::getId).reversed();
        tableRows.sort(MekTableSorter.keepingC3NetworksTogether(tableRows, descendingById));

        int companyIndex = tableRows.indexOf(companyMaster);
        int lanceMasterIndex = tableRows.indexOf(lanceMaster);
        int firstSlaveIndex = tableRows.indexOf(firstSlave);
        int secondSlaveIndex = tableRows.indexOf(secondSlave);

        // The network is one contiguous block of four rows
        int blockStart = companyIndex;
        assertEquals(blockStart + 1, lanceMasterIndex, "Lance master sits directly under the company master");
        assertTrue((firstSlaveIndex > lanceMasterIndex) && (firstSlaveIndex <= blockStart + 3),
              "First slave sits inside the network block, below its lance master");
        assertTrue((secondSlaveIndex > lanceMasterIndex) && (secondSlaveIndex <= blockStart + 3),
              "Second slave sits inside the network block, below its lance master");
        assertTrue(firstSlaveIndex < secondSlaveIndex, "Sibling slaves keep ascending id order");

        // The un-networked units still follow the base sort relative to each other
        assertTrue(tableRows.indexOf(plainLate) < tableRows.indexOf(plainEarly),
              "Plain units keep the descending-id base order");
    }

    @Test
    void testSoloC3UnitsSortAsOrdinaryUnits() {
        Entity soloMaster = createUnit("ISC3MasterComputer");
        Entity plainUnit = createUnit(null);

        List<Entity> tableRows = new ArrayList<>(List.of(soloMaster, plainUnit));
        Comparator<Entity> descendingById = Comparator.comparingInt(Entity::getId).reversed();
        tableRows.sort(MekTableSorter.keepingC3NetworksTogether(tableRows, descendingById));

        assertEquals(plainUnit, tableRows.get(0), "An un-networked C3 carrier gets no special placement");
        assertEquals(soloMaster, tableRows.get(1));
    }
}
