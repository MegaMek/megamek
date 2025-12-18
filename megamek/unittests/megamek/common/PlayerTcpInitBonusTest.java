/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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

import megamek.common.board.Coords;
import megamek.common.equipment.EquipmentType;
import megamek.common.game.Game;
import megamek.common.options.OptionsConstants;
import megamek.common.planetaryConditions.EMI;
import megamek.common.units.BipedMek;
import megamek.common.units.Crew;
import megamek.common.units.CrewType;
import megamek.common.units.Mek;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link Player#getTCPInitBonus()} verifying TCP initiative bonus calculation per IO pg 81:
 * <ul>
 *   <li>Base +2 bonus for TCP+VDNI/BVDNI</li>
 *   <li>+1 for Cockpit Command Module, C3/C3i, or >3 tons comms</li>
 *   <li>-1 if shutdown</li>
 *   <li>-1 if ECM-affected (unless unit has own ECM)</li>
 *   <li>-1 if EMI conditions active</li>
 * </ul>
 */
class PlayerTcpInitBonusTest {

    private Game game;
    private Player player;

    @BeforeAll
    static void initializeEquipment() {
        EquipmentType.initializeTypes();
    }

    @BeforeEach
    void setUp() {
        game = new Game();
        player = new Player(0, "Test Player");
        game.addPlayer(0, player);
    }

    /**
     * Creates a BipedMek with optional TCP and VDNI implants.
     */
    private BipedMek createMek(boolean withTcp, boolean withVdni, boolean deployed) {
        BipedMek mek = new BipedMek();
        mek.setGame(game);
        mek.setId(game.getNextEntityId());
        mek.setChassis("Test Mek");
        mek.setModel("Standard");
        mek.setWeight(50);

        Crew crew = new Crew(CrewType.SINGLE);
        mek.setCrew(crew);

        if (withTcp) {
            crew.getOptions().getOption(OptionsConstants.MD_TRIPLE_CORE_PROCESSOR).setValue(true);
        }
        if (withVdni) {
            crew.getOptions().getOption(OptionsConstants.MD_VDNI).setValue(true);
        }

        mek.setOwner(player);
        mek.autoSetInternal();

        if (deployed) {
            mek.setDeployed(true);
            mek.setPosition(new Coords(5, 5));
        }

        game.addEntity(mek);
        return mek;
    }

    @Nested
    @DisplayName("Basic TCP Bonus Calculation")
    class BasicBonusTests {

        @Test
        @DisplayName("Returns 0 when no entities exist")
        void returnsZeroWhenNoEntities() {
            assertEquals(0, player.getTCPInitBonus());
        }

        @Test
        @DisplayName("Returns 0 when entity lacks TCP implant")
        void returnsZeroWhenNoTcp() {
            createMek(false, true, true);
            assertEquals(0, player.getTCPInitBonus());
        }

        @Test
        @DisplayName("Returns 0 when entity lacks VDNI")
        void returnsZeroWhenNoVdni() {
            createMek(true, false, true);
            assertEquals(0, player.getTCPInitBonus());
        }

        @Test
        @DisplayName("Returns +2 base bonus for TCP+VDNI entity")
        void returnsBaseBonusForTcpVdni() {
            createMek(true, true, true);
            assertEquals(2, player.getTCPInitBonus());
        }

        @Test
        @DisplayName("Returns +2 base bonus for TCP+BVDNI entity")
        void returnsBaseBonusForTcpBvdni() {
            BipedMek mek = createMek(true, false, true);
            mek.getCrew().getOptions().getOption(OptionsConstants.MD_BVDNI).setValue(true);
            assertEquals(2, player.getTCPInitBonus());
        }
    }

    @Nested
    @DisplayName("Command Equipment Bonus (+1)")
    class CommandEquipmentTests {

        @Test
        @DisplayName("Returns +3 with C3 Master system")
        void returnsThreeWithC3Master() throws Exception {
            BipedMek mek = createMek(true, true, true);
            // Add C3 Master
            EquipmentType c3m = EquipmentType.get("ISC3MasterComputer");
            if (c3m != null) {
                mek.addEquipment(c3m, Mek.LOC_CENTER_TORSO);
            }
            assertEquals(3, player.getTCPInitBonus());
        }
    }

    @Nested
    @DisplayName("Penalty Conditions (-1)")
    class PenaltyTests {

        @Test
        @DisplayName("Returns +1 when shutdown (base 2, -1 penalty)")
        void returnsOneWhenShutdown() {
            BipedMek mek = createMek(true, true, true);
            mek.setShutDown(true);
            assertEquals(1, player.getTCPInitBonus());
        }

        @Test
        @DisplayName("Returns +1 when EMI conditions active")
        void returnsOneWhenEmiActive() {
            createMek(true, true, true);
            game.getPlanetaryConditions().setEMI(EMI.EMI);
            assertEquals(1, player.getTCPInitBonus());
        }

        @Test
        @DisplayName("Returns 0 when shutdown AND EMI active (penalties stack per Xotl)")
        void returnsZeroWhenShutdownAndEmi() {
            BipedMek mek = createMek(true, true, true);
            mek.setShutDown(true);
            game.getPlanetaryConditions().setEMI(EMI.EMI);
            // Base +2, shutdown -1, EMI -1 = 0
            assertEquals(0, player.getTCPInitBonus());
        }
    }

    @Nested
    @DisplayName("Entity State Validation")
    class EntityStateTests {

        @Test
        @DisplayName("Returns 0 when entity is destroyed")
        void returnsZeroWhenDestroyed() {
            BipedMek mek = createMek(true, true, true);
            mek.setDestroyed(true);
            assertEquals(0, player.getTCPInitBonus());
        }

        @Test
        @DisplayName("Returns 0 when entity is off-board")
        void returnsZeroWhenOffBoard() {
            BipedMek mek = createMek(true, true, true);
            mek.setOffBoard(1, OffBoardDirection.NORTH);
            // Set deploy round to not match "deploying next round" condition
            mek.setDeployRound(99);
            assertEquals(0, player.getTCPInitBonus());
        }

        @Test
        @DisplayName("Returns 0 when crew is inactive")
        void returnsZeroWhenCrewInactive() {
            BipedMek mek = createMek(true, true, true);
            mek.getCrew().setDead(true);
            assertEquals(0, player.getTCPInitBonus());
        }

        @Test
        @DisplayName("Returns 0 when entity not deployed and not deploying this round")
        void returnsZeroWhenNotDeployed() {
            BipedMek mek = createMek(true, true, false); // Not deployed
            // Set deploy round to future round so entity doesn't count during current round
            mek.setDeployRound(5);
            assertEquals(0, player.getTCPInitBonus());
        }
    }

    @Nested
    @DisplayName("Multiple Entities (Best Bonus)")
    class MultipleEntitiesTests {

        @Test
        @DisplayName("Returns best bonus from multiple qualifying entities")
        void returnsBestBonusFromMultiple() throws Exception {
            // Entity 1: TCP+VDNI only (+2)
            createMek(true, true, true);

            // Entity 2: TCP+VDNI + C3 (+3)
            BipedMek mek2 = createMek(true, true, true);
            EquipmentType c3m = EquipmentType.get("ISC3MasterComputer");
            if (c3m != null) {
                mek2.addEquipment(c3m, Mek.LOC_CENTER_TORSO);
            }

            assertEquals(3, player.getTCPInitBonus());
        }

        @Test
        @DisplayName("Ignores non-qualifying entities when calculating best")
        void ignoresNonQualifyingEntities() {
            // Entity 1: No TCP (doesn't qualify)
            createMek(false, true, true);

            // Entity 2: TCP+VDNI (+2)
            createMek(true, true, true);

            assertEquals(2, player.getTCPInitBonus());
        }

        @Test
        @DisplayName("Returns best even when some entities have penalties")
        void returnsBestDespitePenalties() {
            // Entity 1: TCP+VDNI but shutdown (+2 - 1 = +1)
            BipedMek mek1 = createMek(true, true, true);
            mek1.setShutDown(true);

            // Entity 2: TCP+VDNI only, no penalties (+2)
            createMek(true, true, true);

            assertEquals(2, player.getTCPInitBonus());
        }
    }
}
