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
package megamek.client.ui.panels.phaseDisplay.lobby;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.ArrayList;
import java.util.List;

import megamek.client.ui.panels.phaseDisplay.lobby.C3FormationPlanner.ForcePlan;
import megamek.client.ui.panels.phaseDisplay.lobby.C3FormationPlanner.LeftOutLance;
import megamek.client.ui.panels.phaseDisplay.lobby.C3FormationPlanner.LeftOutReason;
import megamek.client.ui.panels.phaseDisplay.lobby.C3FormationPlanner.TemplateAssignment;
import megamek.common.Player;
import megamek.common.equipment.EquipmentType;
import megamek.common.force.Force;
import megamek.common.force.Forces;
import megamek.common.game.Game;
import megamek.common.units.BipedMek;
import megamek.common.units.Crew;
import megamek.common.units.CrewType;
import megamek.common.units.Entity;
import megamek.common.units.Mek;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests that C3 Network Manager auto-assign keeps the player's lobby lances whole when filling the four configuration
 * templates (CR p.199), names the lances it leaves out with the reason, and still plans units in no force.
 */
class C3FormationPlannerTest {

    private static final int[] MASTER_LOCATIONS =
          { Mek.LOC_HEAD, Mek.LOC_CENTER_TORSO, Mek.LOC_LEFT_TORSO, Mek.LOC_RIGHT_TORSO };

    private Game game;
    private Player player;
    private int nextEntityId = 1;

    @BeforeAll
    static void initializeEquipment() {
        EquipmentType.initializeTypes();
    }

    @BeforeEach
    void setUp() {
        game = new Game();
        player = new Player(0, "Test Player");
        game.addPlayer(0, player);
        nextEntityId = 1;
    }

    /** Configuration 2 from a command lance and two lances: each lance lands in its own block, nothing split. */
    @Test
    void testConfiguration2FromThreeLances() {
        int company = addCompany("Gamma Company");
        Entity head = createMasterUnit(2);
        List<Entity> commandSlaves = createSlaveUnits(3);
        addLance("Command Lance", company, head, commandSlaves);
        Entity alphaMaster = createMasterUnit(1);
        List<Entity> alphaSlaves = createSlaveUnits(3);
        addLance("Alpha Lance", company, alphaMaster, alphaSlaves);
        Entity bravoMaster = createMasterUnit(1);
        List<Entity> bravoSlaves = createSlaveUnits(3);
        addLance("Bravo Lance", company, bravoMaster, bravoSlaves);

        TemplateAssignment assignment = planner().planTemplate(2);

        assertTrue(assignment.isComplete(), "All 12 slots filled");
        assertSame(head, assignment.head());
        assertArrayEquals(commandSlaves.toArray(), assignment.directSlaves(), "The head runs its own lance");
        assertLanceBlock(assignment, 0, alphaMaster, alphaSlaves);
        assertLanceBlock(assignment, 1, bravoMaster, bravoSlaves);
        assertTrue(assignment.leftOut().isEmpty());
    }

    /** Configuration 1's company lance is the head, a lance master and two slaves - the diagram's short lance. */
    @Test
    void testConfiguration1CommandLanceFillsTheCompanyLance() {
        int company = addCompany("Gamma Company");
        Entity head = createMasterUnit(1);
        Entity commandLanceMaster = createMasterUnit(1);
        List<Entity> commandSlaves = createSlaveUnits(2);
        List<Entity> commandMembers = new ArrayList<>(commandSlaves);
        commandMembers.add(commandLanceMaster);
        addLance("Command Lance", company, head, commandMembers);
        Entity alphaMaster = createMasterUnit(1);
        List<Entity> alphaSlaves = createSlaveUnits(3);
        addLance("Alpha Lance", company, alphaMaster, alphaSlaves);
        Entity bravoMaster = createMasterUnit(1);
        List<Entity> bravoSlaves = createSlaveUnits(3);
        addLance("Bravo Lance", company, bravoMaster, bravoSlaves);

        TemplateAssignment assignment = planner().planTemplate(1);

        assertTrue(assignment.isComplete(), "All 12 slots filled");
        assertSame(head, assignment.head());
        assertLanceBlock(assignment, 2, commandLanceMaster, commandSlaves);
        assertLanceBlock(assignment, 0, alphaMaster, alphaSlaves);
        assertLanceBlock(assignment, 1, bravoMaster, bravoSlaves);
    }

    /** Configuration 3: a lance of slaves with no master becomes the second lance the head runs itself. */
    @Test
    void testConfiguration3HeadRunsALanceOfSlaves() {
        int company = addCompany("Gamma Company");
        Entity head = createMasterUnit(3);
        List<Entity> commandSlaves = createSlaveUnits(3);
        addLance("Command Lance", company, head, commandSlaves);
        Entity alphaMaster = createMasterUnit(1);
        List<Entity> alphaSlaves = createSlaveUnits(3);
        addLance("Alpha Lance", company, alphaMaster, alphaSlaves);
        List<Entity> charlieSlaves = createSlaveUnits(3);
        addLance("Charlie Lance", company, null, charlieSlaves);

        TemplateAssignment assignment = planner().planTemplate(3);

        assertTrue(assignment.isComplete(), "All 11 slots filled");
        assertEquals(commandSlaves, List.of(assignment.directSlaves()).subList(0, 3));
        assertEquals(charlieSlaves, List.of(assignment.directSlaves()).subList(3, 6));
        assertLanceBlock(assignment, 0, alphaMaster, alphaSlaves);
    }

    /** Three standard lances cannot head Configuration 1; two fill lance blocks, the third is named, none split. */
    @Test
    void testLanceWithoutRoomIsNamedNotSplit() {
        int company = addCompany("Gamma Company");
        Entity alphaMaster = createMasterUnit(1);
        List<Entity> alphaSlaves = createSlaveUnits(3);
        addLance("Alpha Lance", company, alphaMaster, alphaSlaves);
        Entity bravoMaster = createMasterUnit(1);
        List<Entity> bravoSlaves = createSlaveUnits(3);
        addLance("Bravo Lance", company, bravoMaster, bravoSlaves);
        addLance("Charlie Lance", company, createMasterUnit(1), createSlaveUnits(3));

        TemplateAssignment assignment = planner().planTemplate(1);

        assertNull(assignment.head(), "No lance or loose unit can be the dedicated company master");
        assertLanceBlock(assignment, 0, alphaMaster, alphaSlaves);
        assertLanceBlock(assignment, 1, bravoMaster, bravoSlaves);
        assertLeftOut(assignment.leftOut(), "Charlie Lance", LeftOutReason.NO_FREE_BLOCK);
    }

    /** Lances that fit no block are left out whole and named with the reason. */
    @Test
    void testMisfitLancesAreNamedWithTheirReason() {
        int company = addCompany("Gamma Company");
        addLance("Command Lance", company, createMasterUnit(2), createSlaveUnits(3));
        addLance("Big Lance", company, createMasterUnit(1), createSlaveUnits(4));
        List<Entity> twoMasters = new ArrayList<>(createSlaveUnits(2));
        twoMasters.add(createMasterUnit(1));
        addLance("Twin Lance", company, createMasterUnit(1), twoMasters);
        addLance("Orphan Lance", company, null, createSlaveUnits(3));

        TemplateAssignment assignment = planner().planTemplate(2);

        assertLeftOut(assignment.leftOut(), "Big Lance", LeftOutReason.TOO_MANY_SLAVES);
        assertLeftOut(assignment.leftOut(), "Twin Lance", LeftOutReason.TOO_MANY_MASTERS);
        assertLeftOut(assignment.leftOut(), "Orphan Lance", LeftOutReason.NO_MASTER);
        assertEquals(4, assignment.filledCount(), "Only the command lance is placed");
    }

    /** Configuration 4 has no lance master slot, so a lance with its own master cannot join it. */
    @Test
    void testConfiguration4HasNoPlaceForALanceMaster() {
        int company = addCompany("Gamma Company");
        addLance("Command Lance", company, createMasterUnit(4), createSlaveUnits(3));
        addLance("Alpha Lance", company, createMasterUnit(1), createSlaveUnits(3));

        TemplateAssignment assignment = planner().planTemplate(4);

        assertLeftOut(assignment.leftOut(), "Alpha Lance", LeftOutReason.NO_LANCE_MASTER_PLACE);
    }

    /** The head's own company fills the template before lances of another company with lower force ids. */
    @Test
    void testHeadCompanyIsPreferred() {
        int otherCompany = addCompany("Delta Company");
        addLance("Delta One", otherCompany, createMasterUnit(1), createSlaveUnits(3));
        addLance("Delta Two", otherCompany, createMasterUnit(1), createSlaveUnits(3));
        int company = addCompany("Gamma Company");
        addLance("Command Lance", company, createMasterUnit(2), createSlaveUnits(3));
        Entity alphaMaster = createMasterUnit(1);
        List<Entity> alphaSlaves = createSlaveUnits(3);
        addLance("Alpha Lance", company, alphaMaster, alphaSlaves);
        Entity bravoMaster = createMasterUnit(1);
        List<Entity> bravoSlaves = createSlaveUnits(3);
        addLance("Bravo Lance", company, bravoMaster, bravoSlaves);

        TemplateAssignment assignment = planner().planTemplate(2);

        assertLanceBlock(assignment, 0, alphaMaster, alphaSlaves);
        assertLanceBlock(assignment, 1, bravoMaster, bravoSlaves);
        assertTrue(assignment.leftOut().isEmpty(), "Delta's lances had no room, which is not news for Gamma");
    }

    /** Without lobby forces the planner fills the template from the whole pool, as before. */
    @Test
    void testUnitsInNoForceFillTheTemplate() {
        Entity head = createMasterUnit(1);
        createMasterUnit(1);
        createMasterUnit(1);
        createMasterUnit(1);
        createSlaveUnits(8);

        TemplateAssignment assignment = planner().planTemplate(1);

        assertTrue(assignment.isComplete(), "4 masters and 8 slaves fill Configuration 1");
        assertSame(head, assignment.head(), "The first exact computer-count fit heads it");
    }

    /** Plan Force builds the company from whole lances, keeps a spare lance as its own network, names misfits. */
    @Test
    void testForcePlanKeepsLancesWhole() {
        int company = addCompany("Gamma Company");
        addLance("Command Lance", company, createMasterUnit(2), createSlaveUnits(3));
        addLance("Alpha Lance", company, createMasterUnit(1), createSlaveUnits(3));
        addLance("Bravo Lance", company, createMasterUnit(1), createSlaveUnits(3));
        Entity reserveMaster = createMasterUnit(1);
        List<Entity> reserveSlaves = createSlaveUnits(2);
        addLance("Reserve Lance", company, reserveMaster, reserveSlaves);
        addLance("Orphan Lance", company, null, createSlaveUnits(2));

        ForcePlan plan = planner().planForce();

        assertEquals(1, plan.companies().size());
        assertEquals(2, plan.companies().getFirst().configurationNumber());
        assertEquals(1, plan.lances().size());
        assertSame(reserveMaster, plan.lances().getFirst().master());
        assertEquals(reserveSlaves, plan.lances().getFirst().members());
        assertLeftOut(plan.leftOut(), "Orphan Lance", LeftOutReason.NO_MASTER);
        assertEquals(2, plan.leftoverUnits());
    }

    private C3FormationPlanner planner() {
        return new C3FormationPlanner(game, player.getId());
    }

    /** Asserts that lance master slot {@code lanceIndex} holds exactly this lance: its master and its slaves. */
    private void assertLanceBlock(TemplateAssignment assignment, int lanceIndex, Entity master, List<Entity> slaves) {
        assertSame(master, assignment.lanceMasters()[lanceIndex], "Lance master slot " + lanceIndex);
        List<Entity> blockSlaves = new ArrayList<>();
        for (Entity slave : assignment.lanceSlaves()[lanceIndex]) {
            if (slave != null) {
                blockSlaves.add(slave);
            }
        }
        assertEquals(slaves, blockSlaves, "Slaves of lance master slot " + lanceIndex);
    }

    private void assertLeftOut(List<LeftOutLance> leftOut, String lanceName, LeftOutReason reason) {
        for (LeftOutLance lance : leftOut) {
            if (lance.lanceName().equals(lanceName)) {
                assertEquals(reason, lance.reason(), lanceName + " reason");
                return;
            }
        }
        fail(lanceName + " should be reported as left out; reported: " + leftOut);
    }

    private int addCompany(String name) {
        return game.getForces().addTopLevelForce(Force.createToplevelForce(name, player), player);
    }

    /** Adds a lance under the company holding the master (if any) and the other members. */
    private void addLance(String name, int companyId, Entity master, List<Entity> members) {
        Forces forces = game.getForces();
        Force company = forces.getForce(companyId);
        int lanceId = forces.addSubForce(Force.createSubforce(name, company), company);
        if (master != null) {
            forces.addEntity(master, lanceId);
        }
        for (Entity member : members) {
            forces.addEntity(member, lanceId);
        }
    }

    /** Creates a Mek carrying the given number (1-4) of C3 Master computers. */
    private Entity createMasterUnit(int masterCount) {
        Entity entity = createBaseUnit("C3M x" + masterCount);
        try {
            EquipmentType masterComputer = EquipmentType.get("ISC3MasterComputer");
            for (int mountIndex = 0; mountIndex < masterCount; mountIndex++) {
                entity.addEquipment(masterComputer, MASTER_LOCATIONS[mountIndex]);
            }
        } catch (Exception exception) {
            fail("Failed to add C3 Master equipment: " + exception.getMessage());
        }
        game.addEntity(entity);
        return entity;
    }

    private List<Entity> createSlaveUnits(int count) {
        List<Entity> slaves = new ArrayList<>();
        for (int slaveIndex = 0; slaveIndex < count; slaveIndex++) {
            Entity entity = createBaseUnit("C3S");
            try {
                entity.addEquipment(EquipmentType.get("ISC3SlaveUnit"), Mek.LOC_HEAD);
            } catch (Exception exception) {
                fail("Failed to add C3 Slave equipment: " + exception.getMessage());
            }
            game.addEntity(entity);
            slaves.add(entity);
        }
        return slaves;
    }

    private Entity createBaseUnit(String model) {
        Entity entity = new BipedMek();
        entity.setGame(game);
        entity.setId(nextEntityId++);
        entity.setChassis("Test Mek");
        entity.setModel(model);
        entity.setCrew(new Crew(CrewType.SINGLE));
        entity.setOwner(player);
        entity.setWeight(50.0);
        entity.setOriginalWalkMP(5);
        return entity;
    }
}
