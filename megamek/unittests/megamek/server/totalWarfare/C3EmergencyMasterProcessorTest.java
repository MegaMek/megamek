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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.ArrayList;
import java.util.List;

import megamek.common.Player;
import megamek.common.equipment.EquipmentType;
import megamek.common.game.Game;
import megamek.common.options.OptionsConstants;
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
 * Tests the C3 Emergency Master End Phase rules (TO:AUE p.110 and the Xotl rulings in forum topic 40600):
 * lance-level takeover when the master dies, company-link restoration, the operating-turn overload, and that the
 * whole feature stays inert without its game option.
 */
public class C3EmergencyMasterProcessorTest {

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
        game.getOptions().getOption(OptionsConstants.ADVANCED_TAC_OPS_C3_EMERGENCY_MASTER).setValue(true);
        nextEntityId = 1;
    }

    private Entity createUnit(String... equipmentNames) {
        Entity entity = new BipedMek();
        entity.setGame(game);
        entity.setId(nextEntityId++);
        entity.setChassis("Test Mek");
        entity.setModel("#" + (nextEntityId - 1));
        entity.setCrew(new Crew(CrewType.SINGLE));
        entity.setOwner(game.getPlayer(0));
        entity.setWeight(50.0);
        entity.setOriginalWalkMP(5);
        int location = 0;
        int[] locations = { Mek.LOC_HEAD, Mek.LOC_CENTER_TORSO, Mek.LOC_LEFT_TORSO };
        for (String equipmentName : equipmentNames) {
            try {
                entity.addEquipment(EquipmentType.get(equipmentName), locations[location++]);
            } catch (Exception ex) {
                fail("Failed to add equipment: " + ex.getMessage());
            }
        }
        game.addEntity(entity);
        return entity;
    }

    /** A simple lance: master + C3EM carrier + two plain slaves, connected. */
    private Entity[] buildLanceWithEmergencyMaster() throws Exception {
        Entity lanceMaster = createUnit("ISC3MasterComputer");
        Entity emergencyCarrier = createUnit("ISC3EmergencyMaster");
        Entity firstSlave = createUnit("ISC3SlaveUnit");
        Entity secondSlave = createUnit("ISC3SlaveUnit");
        C3Util.connect(game, new ArrayList<>(List.of(emergencyCarrier, firstSlave, secondSlave)),
              lanceMaster.getId(), false);
        return new Entity[] { lanceMaster, emergencyCarrier, firstSlave, secondSlave };
    }

    @Test
    void testC3EmCarrierActsAsPlainSlaveUntilActivated() throws Exception {
        Entity[] lance = buildLanceWithEmergencyMaster();
        Entity emergencyCarrier = lance[1];

        assertTrue(emergencyCarrier.hasC3S(), "A C3EM duplicates a C3 Slave while its master lives");
        assertFalse(emergencyCarrier.hasC3M(), "Not a master while inactive");
        assertTrue(emergencyCarrier.hasC3EmergencyMaster());
        assertEquals(4, game.getC3NetworkMembers(lance[0]).size());
    }

    @Test
    void testTakeoverOnMasterDestruction() throws Exception {
        Entity[] lance = buildLanceWithEmergencyMaster();
        Entity lanceMaster = lance[0];
        Entity emergencyCarrier = lance[1];

        lanceMaster.setDestroyed(true);
        C3EmergencyMasterProcessor.processEndPhase(game);

        assertTrue(emergencyCarrier.isC3EmergencyMasterActive(), "C3EM activates when the master dies");
        assertTrue(emergencyCarrier.hasC3M(), "An active C3EM functions as a C3 Master");
        assertFalse(emergencyCarrier.hasC3S(), "An active C3EM is not a slave");
        assertTrue(lance[2].C3MasterIs(emergencyCarrier), "First slave reattaches to the C3EM");
        assertTrue(lance[3].C3MasterIs(emergencyCarrier), "Second slave reattaches to the C3EM");
        assertTrue(lance[2].onSameC3NetworkAs(emergencyCarrier), "The lance network works again");
    }

    @Test
    void testTakeoverSurvivesLazyPointerCleanup() throws Exception {
        Entity[] lance = buildLanceWithEmergencyMaster();
        Entity lanceMaster = lance[0];
        Entity emergencyCarrier = lance[1];

        // The master's C3M computer is critted out; something then queries the network before the End Phase,
        // and the lazy cleanup in getC3Master() clears the live pointers
        for (megamek.common.equipment.WeaponMounted weapon : lanceMaster.getWeaponList()) {
            weapon.setDestroyed(true);
        }
        for (Entity member : lance) {
            member.getC3Master();
        }
        assertEquals(Entity.NONE, emergencyCarrier.getC3MasterId(), "Pointer already cleared pre-End Phase");

        C3EmergencyMasterProcessor.processEndPhase(game);

        assertTrue(emergencyCarrier.isC3EmergencyMasterActive(),
              "The recorded lost-master id lets the takeover find the dead master's network");
        assertTrue(lance[2].C3MasterIs(emergencyCarrier));
        assertTrue(lance[3].C3MasterIs(emergencyCarrier));
    }

    @Test
    void testCompanyLinkRestoredOnTakeover() throws Exception {
        Entity companyMaster = createUnit("ISC3MasterComputer");
        C3Util.setCompanyMaster(List.of(companyMaster));
        Entity lanceMaster = createUnit("ISC3MasterComputer");
        C3Util.connect(game, new ArrayList<>(List.of(lanceMaster)), companyMaster.getId(), false);
        Entity emergencyCarrier = createUnit("ISC3EmergencyMaster");
        Entity plainSlave = createUnit("ISC3SlaveUnit");
        C3Util.connect(game, new ArrayList<>(List.of(emergencyCarrier, plainSlave)), lanceMaster.getId(), false);

        lanceMaster.setDestroyed(true);
        C3EmergencyMasterProcessor.processEndPhase(game);

        assertTrue(emergencyCarrier.isC3EmergencyMasterActive());
        assertTrue(emergencyCarrier.C3MasterIs(companyMaster),
              "Ruling 1: the C3EM connects upward to the company master");
        assertTrue(plainSlave.C3MasterIs(emergencyCarrier));
        assertTrue(plainSlave.onSameC3NetworkAs(companyMaster), "The company network is whole again");
    }

    @Test
    void testOverloadAfterSixOperatingTurns() throws Exception {
        Entity[] lance = buildLanceWithEmergencyMaster();
        Entity lanceMaster = lance[0];
        Entity emergencyCarrier = lance[1];

        lanceMaster.setDestroyed(true);
        C3EmergencyMasterProcessor.processEndPhase(game);
        assertTrue(emergencyCarrier.isC3EmergencyMasterActive());

        for (int endPhase = 1; endPhase <= Entity.C3EM_MAX_OPERATING_TURNS; endPhase++) {
            C3EmergencyMasterProcessor.processEndPhase(game);
        }

        assertFalse(emergencyCarrier.isC3EmergencyMasterActive(), "Overloaded after 6 operating turns");
        assertTrue(emergencyCarrier.isC3EmergencyMasterOverloaded());
        assertFalse(emergencyCarrier.hasC3M(), "Ruling 3: dead as Master after overload");
        assertFalse(emergencyCarrier.hasC3S(), "Ruling 3: dead as Slave after overload");
        assertEquals(Entity.NONE, lance[2].getC3MasterId(), "The lance dissolved");
        assertFalse(emergencyCarrier.isC3EmergencyMasterActive());

        // And it never reactivates
        C3EmergencyMasterProcessor.processEndPhase(game);
        assertFalse(emergencyCarrier.isC3EmergencyMasterActive(), "An overloaded C3EM may not be reused");
    }

    @Test
    void testInertWithoutGameOption() throws Exception {
        game.getOptions().getOption(OptionsConstants.ADVANCED_TAC_OPS_C3_EMERGENCY_MASTER).setValue(false);
        Entity[] lance = buildLanceWithEmergencyMaster();
        Entity lanceMaster = lance[0];
        Entity emergencyCarrier = lance[1];

        lanceMaster.setDestroyed(true);
        C3EmergencyMasterProcessor.processEndPhase(game);

        assertFalse(emergencyCarrier.isC3EmergencyMasterActive(),
              "With the option off, the C3EM stays a plain slave");
        assertTrue(emergencyCarrier.hasC3S());
    }

    @Test
    void testAdoptsAtMostThreeSlavesLowestIdsFirst() throws Exception {
        // A dead triple-master company node with 6 direct slaves, one of them the C3EM carrier
        Entity companyNode = createUnit("ISC3MasterComputer", "ISC3MasterComputer", "ISC3MasterComputer");
        C3Util.setCompanyMaster(List.of(companyNode));
        Entity emergencyCarrier = createUnit("ISC3EmergencyMaster");
        List<Entity> fellowSlaves = new ArrayList<>();
        for (int slaveIndex = 0; slaveIndex < 5; slaveIndex++) {
            fellowSlaves.add(createUnit("ISC3SlaveUnit"));
        }
        List<Entity> joiners = new ArrayList<>(fellowSlaves);
        joiners.add(emergencyCarrier);
        C3Util.connect(game, joiners, companyNode.getId(), false);

        companyNode.setDestroyed(true);
        C3EmergencyMasterProcessor.processEndPhase(game);

        assertTrue(emergencyCarrier.isC3EmergencyMasterActive());
        int adopted = 0;
        for (Entity fellowSlave : fellowSlaves) {
            if (fellowSlave.C3MasterIs(emergencyCarrier)) {
                adopted++;
            }
        }
        assertEquals(Entity.MAX_C3M_SUBORDINATES, adopted,
              "Ruling 2: strictly lance-level - a single emergency master holds at most 3 slaves");
        assertTrue(fellowSlaves.get(0).C3MasterIs(emergencyCarrier), "Lowest ids adopted first");
    }
}
