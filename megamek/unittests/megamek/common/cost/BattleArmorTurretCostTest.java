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
package megamek.common.cost;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import megamek.client.ui.clientGUI.calculationReport.CalculationReport;
import megamek.client.ui.clientGUI.calculationReport.DummyCalculationReport;
import megamek.common.Player;
import megamek.common.battleArmor.BattleArmor;
import megamek.common.equipment.EquipmentType;
import megamek.common.game.Game;
import megamek.common.units.Crew;
import megamek.common.units.CrewType;
import megamek.common.units.EntityWeightClass;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Regression tests for MegaMek/megamek#9552: the cost of a battle armor unit with a turret mount threw an
 * IllegalArgumentException because the slot-count message held an invalid choice pattern. The unit cache then dropped
 * every turret BA, and MegaMekLab crashed when opening one.
 */
class BattleArmorTurretCostTest {

    @BeforeAll
    static void initializeEquipment() {
        EquipmentType.initializeTypes();
    }

    private static BattleArmor createTurretBattleArmor(int turretSize, boolean modularTurret) {
        Game game = new Game();
        game.addPlayer(0, new Player(0, "Test Player"));

        BattleArmor battleArmor = new BattleArmor();
        battleArmor.setGame(game);
        battleArmor.setId(1);
        battleArmor.setChassis("Test BA");
        battleArmor.setModel("Turret");
        battleArmor.setSquadSize(4);
        battleArmor.setWeightClass(EntityWeightClass.WEIGHT_HEAVY);
        battleArmor.setCrew(new Crew(CrewType.INFANTRY_CREW));
        battleArmor.setOwner(game.getPlayer(0));
        battleArmor.setTurretSize(turretSize);
        battleArmor.setModularTurret(modularTurret);
        for (int trooper = 1; trooper <= 4; trooper++) {
            battleArmor.initializeArmor(4, trooper);
        }
        battleArmor.autoSetInternal();
        return battleArmor;
    }

    /** Records the type column of every line added to the report. */
    private static final class RecordingReport extends DummyCalculationReport {
        private final List<String> lineTypes = new ArrayList<>();

        @Override
        public CalculationReport addLine(String type, String calculation, String result) {
            lineTypes.add(type);
            return this;
        }

        boolean hasLineEndingWith(String suffix) {
            for (String lineType : lineTypes) {
                if ((lineType != null) && lineType.endsWith(suffix)) {
                    return true;
                }
            }
            return false;
        }
    }

    @ParameterizedTest
    @ValueSource(ints = { 1, 2, 3, 6 })
    void costOfTurretBattleArmorDoesNotThrow(int turretSize) {
        BattleArmor modularTurret = createTurretBattleArmor(turretSize, false);
        BattleArmor configurableTurret = createTurretBattleArmor(turretSize, true);

        assertDoesNotThrow(() -> modularTurret.getCost(false));
        assertDoesNotThrow(() -> configurableTurret.getCost(true));
    }

    @Test
    void turretLineUsesSingularForOneSlot() {
        RecordingReport report = new RecordingReport();

        CostCalculator.getWeaponsAndEquipmentCost(createTurretBattleArmor(1, false), report, false);

        assertTrue(report.hasLineEndingWith("(1 Slot)"),
              "Expected a turret line ending in (1 Slot), got " + report.lineTypes);
    }

    @Test
    void turretLineUsesPluralForSeveralSlots() {
        RecordingReport report = new RecordingReport();

        CostCalculator.getWeaponsAndEquipmentCost(createTurretBattleArmor(3, false), report, false);

        assertTrue(report.hasLineEndingWith("(3 Slots)"),
              "Expected a turret line ending in (3 Slots), got " + report.lineTypes);
    }
}
