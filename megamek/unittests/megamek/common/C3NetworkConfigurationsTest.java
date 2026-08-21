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
package megamek.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.ArrayList;
import java.util.List;

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
 * Tests that all four C3 network configurations from the Core Rulebook (CR p.199, C3 Configuration Diagram) can be
 * built through {@link C3Util}, including the consolidated multi-master company nodes of Configurations 3 and 4 and
 * the All-C3-Master lance variation, and that the capacity limits reject illegal networks.
 *
 * <ul>
 *     <li>Configuration 1: dedicated single-C3M company master, three lance masters, 12 units</li>
 *     <li>Configuration 2: dual-C3M company node (company link + own lance), two lance masters, 12 units</li>
 *     <li>Configuration 3: triple-C3M company node (company link + two lances of slaves), one lance master, 11
 *     units</li>
 *     <li>Configuration 4: quad-C3M company node (company link + three lances of slaves), 10 units</li>
 * </ul>
 */
public class C3NetworkConfigurationsTest {

    private static final int[] MASTER_LOCATIONS =
          { Mek.LOC_HEAD, Mek.LOC_CENTER_TORSO, Mek.LOC_LEFT_TORSO, Mek.LOC_RIGHT_TORSO };

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

    /** Creates a Mek carrying the given number (1-4) of C3 Master computers. */
    private Entity createMasterUnit(int masterCount) {
        Entity entity = createBaseUnit("C3M x" + masterCount);
        try {
            EquipmentType c3m = EquipmentType.get("ISC3MasterComputer");
            for (int mountIndex = 0; mountIndex < masterCount; mountIndex++) {
                entity.addEquipment(c3m, MASTER_LOCATIONS[mountIndex]);
            }
        } catch (Exception ex) {
            fail("Failed to add C3 Master equipment: " + ex.getMessage());
        }
        game.addEntity(entity);
        return entity;
    }

    /** Creates a Mek carrying one C3 Slave computer. */
    private Entity createSlaveUnit() {
        Entity entity = createBaseUnit("C3S");
        try {
            EquipmentType c3s = EquipmentType.get("ISC3SlaveUnit");
            entity.addEquipment(c3s, Mek.LOC_HEAD);
        } catch (Exception ex) {
            fail("Failed to add C3 Slave equipment: " + ex.getMessage());
        }
        game.addEntity(entity);
        return entity;
    }

    /** Creates a Mek carrying a C3i computer. */
    private Entity createC3iUnit() {
        Entity entity = createBaseUnit("C3i");
        try {
            EquipmentType c3iComputer = EquipmentType.get("ISC3iUnit");
            entity.addEquipment(c3iComputer, Mek.LOC_HEAD);
        } catch (Exception ex) {
            fail("Failed to add C3i equipment: " + ex.getMessage());
        }
        game.addEntity(entity);
        return entity;
    }

    private Entity createBaseUnit(String model) {
        Entity entity = new BipedMek();
        entity.setGame(game);
        entity.setId(nextEntityId++);
        entity.setChassis("Test Mek");
        entity.setModel(model);
        entity.setCrew(new Crew(CrewType.SINGLE));
        entity.setOwner(game.getPlayer(0));
        entity.setWeight(50.0);
        entity.setOriginalWalkMP(5);
        return entity;
    }

    private List<Entity> createSlaveUnits(int count) {
        List<Entity> slaves = new ArrayList<>();
        for (int slaveIndex = 0; slaveIndex < count; slaveIndex++) {
            slaves.add(createSlaveUnit());
        }
        return slaves;
    }

    /** C3Util.connect mutates the passed collection, so always hand it a fresh mutable list. */
    private void connect(List<Entity> joiners, Entity master) throws Exception {
        C3Util.connect(game, new ArrayList<>(joiners), master.getId(), false);
    }

    private void assertAllNetworked(List<Entity> members) {
        Entity first = members.get(0);
        for (Entity member : members) {
            assertTrue(first.onSameC3NetworkAs(member) || first.equals(member),
                  member.getModel() + " #" + member.getId() + " should be on the same network as "
                        + first.getModel() + " #" + first.getId());
        }
    }

    @Test
    void testMultiMasterCapacityPools() throws Exception {
        Entity singleMaster = createMasterUnit(1);
        Entity dualMaster = createMasterUnit(2);
        Entity tripleMaster = createMasterUnit(3);
        Entity quadMaster = createMasterUnit(4);
        C3Util.setCompanyMaster(List.of(singleMaster, dualMaster, tripleMaster, quadMaster));

        assertEquals(1, singleMaster.getOperableC3MCount());
        assertEquals(2, dualMaster.getOperableC3MCount());
        assertEquals(3, tripleMaster.getOperableC3MCount());
        assertEquals(4, quadMaster.getOperableC3MCount());

        // The company computer controls up to 3 masters; sibling computers occupy company links only while
        // actually running a lance, so an empty company node of any size offers all 3 (idle computers allowed -
        // a multi-master can head the smaller configurations too)
        assertEquals(3, singleMaster.calculateFreeC3MNodes(), "Empty single-C3M company master: 3 master links");
        assertEquals(3, dualMaster.calculateFreeC3MNodes(), "Empty dual-C3M company node: 3 master links");
        assertEquals(3, tripleMaster.calculateFreeC3MNodes(), "Empty triple-C3M company node: 3 master links");
        assertEquals(3, quadMaster.calculateFreeC3MNodes(), "Empty quad-C3M company node: 3 master links");

        // Each C3 Master beyond the company computer runs a lance of 3 slaves
        assertEquals(0, singleMaster.calculateFreeC3Nodes(), "Single-C3M company master controls no slaves");
        assertEquals(3, dualMaster.calculateFreeC3Nodes(), "Dual-C3M company node: 3 slave links");
        assertEquals(6, tripleMaster.calculateFreeC3Nodes(), "Triple-C3M company node: 6 slave links");
        assertEquals(9, quadMaster.calculateFreeC3Nodes(), "Quad-C3M company node: 9 slave links");
    }

    /** Configuration 1: dedicated company master (1 C3M), three lance masters, 8 slaves - 12 units. */
    @Test
    void testConfiguration1() throws Exception {
        Entity companyMaster = createMasterUnit(1);
        Entity lanceMaster1 = createMasterUnit(1);
        Entity lanceMaster2 = createMasterUnit(1);
        Entity lanceMaster3 = createMasterUnit(1);
        C3Util.setCompanyMaster(List.of(companyMaster));

        connect(List.of(lanceMaster1, lanceMaster2, lanceMaster3), companyMaster);

        List<Entity> lance1Slaves = createSlaveUnits(3);
        List<Entity> lance2Slaves = createSlaveUnits(3);
        List<Entity> lance3Slaves = createSlaveUnits(2);
        connect(lance1Slaves, lanceMaster1);
        connect(lance2Slaves, lanceMaster2);
        connect(lance3Slaves, lanceMaster3);

        List<Entity> allMembers = new ArrayList<>(
              List.of(companyMaster, lanceMaster1, lanceMaster2, lanceMaster3));
        allMembers.addAll(lance1Slaves);
        allMembers.addAll(lance2Slaves);
        allMembers.addAll(lance3Slaves);
        assertAllNetworked(allMembers);
        assertEquals(12, game.getC3NetworkMembers(companyMaster).size());

        // Lance master 3 has a free slave link, but a 13th unit busts the 12-unit network cap (CR p.198)
        Entity thirteenthUnit = createSlaveUnit();
        assertThrows(C3Util.C3CapacityException.class,
              () -> connect(List.of(thirteenthUnit), lanceMaster3),
              "A 13th network member must be rejected");

        // Lance master 1 already controls 3 slaves - its links are full
        Entity extraSlave = createSlaveUnit();
        assertThrows(C3Util.C3CapacityException.class,
              () -> connect(List.of(extraSlave), lanceMaster1),
              "A fourth slave on one lance master must be rejected");
    }

    /** Configuration 2: dual-C3M company node with its own lance of 3, two lance masters - 12 units. */
    @Test
    void testConfiguration2() throws Exception {
        Entity companyNode = createMasterUnit(2);
        C3Util.setCompanyMaster(List.of(companyNode));

        List<Entity> directSlaves = createSlaveUnits(3);
        connect(directSlaves, companyNode);

        Entity lanceMaster1 = createMasterUnit(1);
        Entity lanceMaster2 = createMasterUnit(1);
        connect(List.of(lanceMaster1, lanceMaster2), companyNode);

        List<Entity> lance1Slaves = createSlaveUnits(3);
        List<Entity> lance2Slaves = createSlaveUnits(3);
        connect(lance1Slaves, lanceMaster1);
        connect(lance2Slaves, lanceMaster2);

        assertEquals(12, game.getC3NetworkMembers(companyNode).size());

        Entity thirdMaster = createMasterUnit(1);
        assertThrows(C3Util.C3CapacityException.class,
              () -> connect(List.of(thirdMaster), companyNode),
              "A dual-C3M company node has only 2 external master links");

        Entity fourthDirectSlave = createSlaveUnit();
        assertThrows(C3Util.C3CapacityException.class,
              () -> connect(List.of(fourthDirectSlave), companyNode),
              "A dual-C3M company node has only 3 slave links");
    }

    /** Configuration 3: triple-C3M company node running two lances of slaves itself, one lance master - 11 units. */
    @Test
    void testConfiguration3() throws Exception {
        Entity companyNode = createMasterUnit(3);
        C3Util.setCompanyMaster(List.of(companyNode));

        List<Entity> directSlaves = createSlaveUnits(6);
        connect(directSlaves, companyNode);

        Entity lanceMaster = createMasterUnit(1);
        connect(List.of(lanceMaster), companyNode);

        List<Entity> lanceSlaves = createSlaveUnits(3);
        connect(lanceSlaves, lanceMaster);

        assertEquals(11, game.getC3NetworkMembers(companyNode).size());

        Entity seventhDirectSlave = createSlaveUnit();
        assertThrows(C3Util.C3CapacityException.class,
              () -> connect(List.of(seventhDirectSlave), companyNode),
              "A triple-C3M company node has only 6 slave links");

        Entity secondMaster = createMasterUnit(1);
        assertThrows(C3Util.C3CapacityException.class,
              () -> connect(List.of(secondMaster), companyNode),
              "A triple-C3M company node has only 1 external master link");
    }

    /** Configuration 4: quad-C3M company node running all three lances of slaves itself - 10 units. */
    @Test
    void testConfiguration4() throws Exception {
        Entity companyNode = createMasterUnit(4);
        C3Util.setCompanyMaster(List.of(companyNode));

        List<Entity> directSlaves = createSlaveUnits(9);
        connect(directSlaves, companyNode);

        assertEquals(10, game.getC3NetworkMembers(companyNode).size());
        assertAllNetworked(game.getC3NetworkMembers(companyNode));

        Entity tenthSlave = createSlaveUnit();
        assertThrows(C3Util.C3CapacityException.class,
              () -> connect(List.of(tenthSlave), companyNode),
              "A quad-C3M company node has only 9 slave links");

        Entity anyMaster = createMasterUnit(1);
        assertThrows(C3Util.C3CapacityException.class,
              () -> connect(List.of(anyMaster), companyNode),
              "A quad-C3M company node has no external master links");
    }

    /**
     * Idle computers are allowed (CR p.198 designation rules), so a multi-master unit can head a configuration
     * smaller than its computer count: a triple-master heading three lance masters is Configuration 1. Slaves and
     * masters compete for the same three company links, so once all three carry masters, no direct slaves fit.
     */
    @Test
    void testMultiMasterCanHeadSmallerConfigurations() throws Exception {
        Entity companyNode = createMasterUnit(3);
        C3Util.setCompanyMaster(List.of(companyNode));

        connect(List.of(createMasterUnit(1), createMasterUnit(1), createMasterUnit(1)), companyNode);
        assertEquals(0, companyNode.calculateFreeC3MNodes(), "All three company links carry masters");
        assertEquals(0, companyNode.calculateFreeC3Nodes(),
              "No company links left for the node's own lance computers - no direct slaves");

        Entity directSlave = createSlaveUnit();
        assertThrows(C3Util.C3CapacityException.class,
              () -> connect(List.of(directSlave), companyNode),
              "A Configuration 1 head cannot also take direct slaves");

        // Configuration 2 usage of a triple-master: one lance of slaves plus two subordinate masters
        Entity secondCompanyNode = createMasterUnit(3);
        C3Util.setCompanyMaster(List.of(secondCompanyNode));
        connect(createSlaveUnits(3), secondCompanyNode);
        connect(List.of(createMasterUnit(1), createMasterUnit(1)), secondCompanyNode);
        assertEquals(0, secondCompanyNode.calculateFreeC3MNodes(),
              "One lance computer in use plus two masters fills the three company links");
    }

    /** A combined join must not oversubscribe the company links that slaves and masters share. */
    @Test
    void testMixedJoinCannotOversubscribeCompanyLinks() throws Exception {
        Entity companyNode = createMasterUnit(3);
        C3Util.setCompanyMaster(List.of(companyNode));

        // 6 slaves need 2 lance computers; with 2 masters that is 4 company links - one over the limit
        List<Entity> joiners = new ArrayList<>(createSlaveUnits(6));
        joiners.add(createMasterUnit(1));
        joiners.add(createMasterUnit(1));
        assertThrows(C3Util.C3CapacityException.class,
              () -> connect(joiners, companyNode),
              "6 slaves + 2 masters needs 4 company links; only 3 exist");
    }

    /**
     * The configuration diagram shows "the only four ways" a network forms (CR p.198): multi-master consolidation
     * happens at the company node only. A subordinate or undesignated multi-master runs a single lance of 3 - its
     * spare computers stay idle - so a 12-unit pseudo-Configuration-3 with a subordinate triple-master carrying 4
     * slaves cannot be built.
     */
    @Test
    void testSubordinateMultiMasterRunsOneLance() throws Exception {
        Entity undesignated = createMasterUnit(3);
        assertEquals(3, undesignated.calculateFreeC3Nodes(),
              "An undesignated multi-master offers a single lance of 3");

        Entity companyNode = createMasterUnit(3);
        C3Util.setCompanyMaster(List.of(companyNode));
        connect(createSlaveUnits(6), companyNode);

        Entity subordinate = createMasterUnit(3);
        connect(List.of(subordinate), companyNode);
        assertEquals(3, subordinate.calculateFreeC3Nodes(),
              "A subordinate multi-master offers a single lance of 3");

        connect(createSlaveUnits(3), subordinate);
        assertEquals(11, game.getC3NetworkMembers(companyNode).size(),
              "Canonical Configuration 3: company node, 6 direct slaves, one lance master, its 3 slaves");

        Entity fourthLanceSlave = createSlaveUnit();
        assertThrows(C3Util.C3CapacityException.class,
              () -> connect(List.of(fourthLanceSlave), subordinate),
              "A subordinate multi-master must not use spare computers for a second lance");
    }

    /** All-C3-Master lance (CR p.199): masters may fill the slave positions, but the lance stays homogeneous. */
    @Test
    void testAllMasterLance() throws Exception {
        Entity lanceMaster = createMasterUnit(1);
        Entity masterAsSlave1 = createMasterUnit(1);
        Entity masterAsSlave2 = createMasterUnit(1);
        Entity masterAsSlave3 = createMasterUnit(1);

        connect(List.of(masterAsSlave1, masterAsSlave2, masterAsSlave3), lanceMaster);
        assertEquals(4, game.getC3NetworkMembers(lanceMaster).size());
        assertAllNetworked(game.getC3NetworkMembers(lanceMaster));

        // Capacity applies to masters in slave roles as it does to slaves
        Entity fourthMasterAsSlave = createMasterUnit(1);
        assertThrows(C3Util.C3CapacityException.class,
              () -> connect(List.of(fourthMasterAsSlave), lanceMaster));

        // A master cannot control masters and slaves simultaneously (CR p.199)
        Entity mixedInSlave = createSlaveUnit();
        assertThrows(C3Util.MismatchingC3MException.class,
              () -> connect(List.of(mixedInSlave), lanceMaster),
              "A slave must not join an all-master lance");

        Entity slaveLanceMaster = createMasterUnit(1);
        connect(createSlaveUnits(1), slaveLanceMaster);
        Entity mixedInMaster = createMasterUnit(1);
        assertThrows(C3Util.MismatchingC3MException.class,
              () -> connect(List.of(mixedInMaster), slaveLanceMaster),
              "A master must not join a lance of slaves");
    }

    /** A dual-C3M company node accepts masters and slaves in one connect call (existing SMM behavior). */
    @Test
    void testMixedConnectOnCompanyNode() throws Exception {
        Entity companyNode = createMasterUnit(2);
        C3Util.setCompanyMaster(List.of(companyNode));

        Entity lanceMaster = createMasterUnit(1);
        List<Entity> joiners = new ArrayList<>(createSlaveUnits(2));
        joiners.add(lanceMaster);
        connect(joiners, companyNode);

        assertEquals(4, game.getC3NetworkMembers(companyNode).size());
    }

    /** The BV bonus gate must recognize consolidated company networks (it used free-node heuristics before). */
    @Test
    void testBVBonusForConfiguration4() throws Exception {
        Entity companyNode = createMasterUnit(4);
        C3Util.setCompanyMaster(List.of(companyNode));

        int soloBaseBV = companyNode.calculateBattleValue(true, true);
        assertEquals(0, companyNode.getExtraC3BV(soloBaseBV), "A solo quad-C3M unit gets no network bonus");

        List<Entity> directSlaves = createSlaveUnits(9);
        connect(directSlaves, companyNode);

        // 10 members caps the bonus at 8 x 5% = 40% (CR p.220)
        int baseBV = companyNode.calculateBattleValue(true, true);
        assertEquals((int) Math.round(baseBV * 0.40), companyNode.getExtraC3BV(baseBV),
              "Company node in a 10-unit network gets the capped 40% bonus");

        Entity slave = directSlaves.get(0);
        int slaveBaseBV = slave.calculateBattleValue(true, true);
        assertEquals((int) Math.round(slaveBaseBV * 0.40), slave.getExtraC3BV(slaveBaseBV),
              "Slave in a 10-unit network gets the capped 40% bonus");
    }

    /** The membership-based BV gate must also work for the masterless systems (C3i branch). */
    @Test
    void testBVBonusForC3iNetwork() throws Exception {
        Entity firstPeer = createC3iUnit();
        Entity secondPeer = createC3iUnit();

        int soloBaseBV = firstPeer.calculateBattleValue(true, true);
        assertEquals(0, firstPeer.getExtraC3BV(soloBaseBV), "A solo C3i unit gets no network bonus");

        C3Util.joinNh(game, new ArrayList<>(List.of(firstPeer, secondPeer)), firstPeer.getId(), true);

        int baseBV = firstPeer.calculateBattleValue(true, true);
        assertEquals((int) Math.round(baseBV * 0.10), firstPeer.getExtraC3BV(baseBV),
              "Each member of a 2-unit C3i network gets the 10% bonus");
        int secondBaseBV = secondPeer.calculateBattleValue(true, true);
        assertEquals((int) Math.round(secondBaseBV * 0.10), secondPeer.getExtraC3BV(secondBaseBV),
              "Both peers get the bonus");
    }

    /**
     * Save/load is the classic C3 breakage: a Configuration 3 network must survive the UUID round trip that MUL
     * files and lobby serialization use. Simulates the save (record each unit's master as a UUID reference, as
     * EntityListFile does), the load (live master ids are gone), and the client-side rewire.
     */
    @Test
    void testWireC3RestoresConfiguration3FromUUIDs() throws Exception {
        Entity companyNode = createMasterUnit(3);
        C3Util.setCompanyMaster(List.of(companyNode));
        List<Entity> directSlaves = createSlaveUnits(6);
        connect(directSlaves, companyNode);
        Entity lanceMaster = createMasterUnit(1);
        connect(List.of(lanceMaster), companyNode);
        List<Entity> lanceSlaves = createSlaveUnits(3);
        connect(lanceSlaves, lanceMaster);

        List<Entity> members = new ArrayList<>(game.getC3NetworkMembers(companyNode));
        assertEquals(11, members.size(), "Configuration 3 before the round trip");

        // Save: every unit records its master as a UUID reference (the company commander references itself)
        for (Entity member : members) {
            Entity master = member.getC3Master();
            if (master != null) {
                member.setC3MasterIsUUIDAsString(master.getC3UUIDAsString());
            }
        }
        // Load: live master ids are gone until wireC3 resolves the UUID references
        for (Entity member : members) {
            member.setC3Master(Entity.NONE, false);
        }
        for (Entity member : members) {
            assertEquals(1, game.getC3NetworkMembers(member).size(),
                  "Each unit stands alone after the simulated load");
        }

        for (Entity member : members) {
            C3Util.wireC3(game, member);
        }

        assertTrue(companyNode.isC3CompanyCommander(), "Company designation survives the round trip");
        assertEquals(11, game.getC3NetworkMembers(companyNode).size(), "Full network restored");
        for (Entity directSlave : directSlaves) {
            assertTrue(directSlave.C3MasterIs(companyNode), "Direct slaves reattach to the company node");
        }
        assertTrue(lanceMaster.C3MasterIs(companyNode), "The lance master reattaches to the company node");
        for (Entity lanceSlave : lanceSlaves) {
            assertTrue(lanceSlave.C3MasterIs(lanceMaster), "Lance slaves reattach to their lance master");
        }
        assertEquals(0, companyNode.calculateFreeC3MNodes(), "Restored network is at full master capacity");
        assertEquals(0, companyNode.calculateFreeC3Nodes(), "Restored network is at full slave capacity");
    }
}
