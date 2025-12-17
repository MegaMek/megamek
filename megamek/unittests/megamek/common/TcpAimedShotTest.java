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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import megamek.common.battleArmor.BattleArmor;
import megamek.common.equipment.EquipmentType;
import megamek.common.game.Game;
import megamek.common.options.OptionsConstants;
import megamek.common.units.BipedMek;
import megamek.common.units.Crew;
import megamek.common.units.CrewType;
import megamek.common.units.EntityWeightClass;
import megamek.common.units.Infantry;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for Triple-Core Processor (TCP) aimed shot capability.
 * <p>
 * Per IO pg 81, only MekWarriors, vehicle commanders, and fighter pilots with TCP and VDNI/BVDNI may execute aimed
 * shots as if equipped with a Targeting Computer. Infantry and Battle Armor do NOT qualify for this capability.
 */
public class TcpAimedShotTest {

    private Game game;

    @BeforeAll
    static void initializeEquipment() {
        EquipmentType.initializeTypes();
    }

    @BeforeEach
    void setUp() {
        game = new Game();
        game.addPlayer(0, new Player(0, "Test Player"));
    }

    /**
     * Creates a BipedMek with optional TCP and VDNI implants.
     */
    private BipedMek createMek(boolean withTcp, boolean withVdni) {
        BipedMek mek = new BipedMek();
        mek.setGame(game);
        mek.setId(1);
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

        mek.setOwner(game.getPlayer(0));
        mek.autoSetInternal();

        return mek;
    }

    /**
     * Creates a conventional infantry unit with optional TCP and VDNI implants.
     */
    private Infantry createInfantry(boolean withTcp, boolean withVdni) {
        Infantry infantry = new Infantry();
        infantry.setGame(game);
        infantry.setId(1);
        infantry.setChassis("Test Platoon");
        infantry.setModel("Standard");

        Crew crew = new Crew(CrewType.INFANTRY_CREW);
        infantry.setCrew(crew);

        if (withTcp) {
            crew.getOptions().getOption(OptionsConstants.MD_TRIPLE_CORE_PROCESSOR).setValue(true);
        }
        if (withVdni) {
            crew.getOptions().getOption(OptionsConstants.MD_VDNI).setValue(true);
        }

        infantry.setOwner(game.getPlayer(0));
        infantry.autoSetInternal();
        infantry.initializeInternal(21, Infantry.LOC_INFANTRY);

        return infantry;
    }

    /**
     * Creates a Battle Armor unit with optional TCP and VDNI implants.
     */
    private BattleArmor createBattleArmor(boolean withTcp, boolean withVdni) {
        BattleArmor ba = new BattleArmor();
        ba.setGame(game);
        ba.setId(1);
        ba.setChassis("Test BA");
        ba.setModel("Standard");
        ba.setTroopers(4);
        ba.setWeightClass(EntityWeightClass.WEIGHT_MEDIUM);

        Crew crew = new Crew(CrewType.INFANTRY_CREW);
        ba.setCrew(crew);

        if (withTcp) {
            crew.getOptions().getOption(OptionsConstants.MD_TRIPLE_CORE_PROCESSOR).setValue(true);
        }
        if (withVdni) {
            crew.getOptions().getOption(OptionsConstants.MD_VDNI).setValue(true);
        }

        ba.setOwner(game.getPlayer(0));

        for (int i = 1; i <= 4; i++) {
            ba.initializeArmor(4, i);
        }
        ba.autoSetInternal();

        return ba;
    }

    @Nested
    @DisplayName("Mek TCP Aimed Shot Capability")
    class MekTcpAimedShotTests {

        @Test
        @DisplayName("Mek with TCP and VDNI has aimed shot capability")
        void mekWithTcpAndVdniHasAimedShotCapability() {
            BipedMek mek = createMek(true, true);

            assertTrue(mek.hasTCPAimedShotCapability(),
                  "Mek with TCP+VDNI should have aimed shot capability");
        }

        @Test
        @DisplayName("Mek with TCP but no VDNI does not have aimed shot capability")
        void mekWithTcpButNoVdniDoesNotHaveCapability() {
            BipedMek mek = createMek(true, false);

            assertFalse(mek.hasTCPAimedShotCapability(),
                  "Mek with TCP but no VDNI should not have aimed shot capability");
        }

        @Test
        @DisplayName("Mek with VDNI but no TCP does not have aimed shot capability")
        void mekWithVdniButNoTcpDoesNotHaveCapability() {
            BipedMek mek = createMek(false, true);

            assertFalse(mek.hasTCPAimedShotCapability(),
                  "Mek with VDNI but no TCP should not have aimed shot capability");
        }

        @Test
        @DisplayName("Mek without TCP or VDNI does not have aimed shot capability")
        void mekWithoutTcpOrVdniDoesNotHaveCapability() {
            BipedMek mek = createMek(false, false);

            assertFalse(mek.hasTCPAimedShotCapability(),
                  "Mek without TCP or VDNI should not have aimed shot capability");
        }
    }

    @Nested
    @DisplayName("Infantry TCP Aimed Shot Exclusion")
    class InfantryTcpAimedShotTests {

        @Test
        @DisplayName("Infantry with TCP and VDNI does NOT have aimed shot capability")
        void infantryWithTcpAndVdniDoesNotHaveCapability() {
            Infantry infantry = createInfantry(true, true);

            // Per IO pg 81: Only MekWarriors, vehicle commanders, and fighter pilots qualify
            assertFalse(infantry.hasTCPAimedShotCapability(),
                  "Infantry with TCP+VDNI should NOT have aimed shot capability per IO pg 81");
        }

        @Test
        @DisplayName("Infantry without implants does not have aimed shot capability")
        void infantryWithoutImplantsDoesNotHaveCapability() {
            Infantry infantry = createInfantry(false, false);

            assertFalse(infantry.hasTCPAimedShotCapability(),
                  "Infantry without implants should not have aimed shot capability");
        }
    }

    @Nested
    @DisplayName("Battle Armor TCP Aimed Shot Exclusion")
    class BattleArmorTcpAimedShotTests {

        @Test
        @DisplayName("Battle Armor with TCP and VDNI does NOT have aimed shot capability")
        void battleArmorWithTcpAndVdniDoesNotHaveCapability() {
            BattleArmor ba = createBattleArmor(true, true);

            // Per IO pg 81: Only MekWarriors, vehicle commanders, and fighter pilots qualify
            assertFalse(ba.hasTCPAimedShotCapability(),
                  "Battle Armor with TCP+VDNI should NOT have aimed shot capability per IO pg 81");
        }

        @Test
        @DisplayName("Battle Armor without implants does not have aimed shot capability")
        void battleArmorWithoutImplantsDoesNotHaveCapability() {
            BattleArmor ba = createBattleArmor(false, false);

            assertFalse(ba.hasTCPAimedShotCapability(),
                  "Battle Armor without implants should not have aimed shot capability");
        }
    }
}
