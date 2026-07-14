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

import static megamek.testUtilities.MMTestUtilities.getEntityForUnitTesting;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import megamek.common.RecordingCompositeTechLevel.ComponentTechRecord;
import megamek.common.enums.Faction;
import megamek.common.equipment.EquipmentType;
import megamek.common.eras.Eras;
import megamek.common.units.Entity;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class CompositeTechLevelReportTest {

    private static final String ELEMENTAL = "Elemental Battle Armor [Laser](Sqd5)";

    @BeforeAll
    public static void setupClass() {
        EquipmentType.initializeTypes();
        Eras.getInstance();
    }

    private static Entity elemental() {
        Entity entity = getEntityForUnitTesting(ELEMENTAL, true);
        assertNotNull(entity, ELEMENTAL + " not found");
        return entity;
    }

    @Test
    public void recordedTechLevelAgreesWithTheUnitsRealTechLevel() {
        Entity entity = elemental();

        RecordingCompositeTechLevel recorded = entity.recordedTechLevel(Faction.NONE, entity.getYear());

        // The recording composite replays the same component list as the unit's own calculation, so the two
        // must always produce the same answer. This is what keeps the report honest.
        assertEquals(entity.getPrototypeRangeDate(), recorded.getPrototypeDateRange());
        assertEquals(entity.getProductionDateRange(), recorded.getProductionDateRange());
        assertEquals(entity.getCommonDateRange(), recorded.getCommonDateRange());
    }

    @Test
    public void everyComponentIsNamed() {
        Entity entity = elemental();

        List<ComponentTechRecord> components = entity.recordedTechLevel(Faction.NONE, entity.getYear())
              .getComponentRecords();

        assertFalse(components.isEmpty(), "No components were recorded");
        for (ComponentTechRecord component : components) {
            assertFalse(component.componentName().isBlank(), "A component was recorded with a blank name");
            assertNotEquals("Unnamed component", component.componentName(),
                  "A component was recorded without a name: " + component);
        }
    }

    @Test
    public void reportNamesTheArmorThatDrivesTheCommonDate() {
        Entity entity = elemental();

        String reportText = CompositeTechLevelReport.toPlainText(entity, Faction.NONE, entity.getYear(), true);

        // The suit's armor is what pushes the unit's common date out to 3054, so the report has to name it,
        // and has to flag it as the component that moves the unit.
        assertTrue(reportText.contains("Armor: BA Standard"),
              "Report does not name the BA Standard armor:\n" + reportText);
        assertTrue(reportText.contains("Small Laser"),
              "Report does not name the small laser:\n" + reportText);
        assertTrue(reportText.contains("3054"),
              "Report does not show the 3054 common date:\n" + reportText);
        // The armor pushes the unit's common date out to 3054, so it is the component that drives when the
        // unit becomes common.
        assertTrue(reportText.contains("Becomes Common"),
              "Report does not name the progression the armor drives:\n" + reportText);
    }

    @Test
    public void antiMekAttacksAreNotShownForBattleArmor() {
        Entity entity = elemental();

        String reportText = CompositeTechLevelReport.toPlainText(entity, Faction.NONE, entity.getYear(), true);

        // The anti-Mek attack pseudo-weapons every BA unit carries are noise and are dropped from the report.
        assertFalse(reportText.contains("Swarm Mek"), "Report still lists the Swarm Mek attack:\n" + reportText);
        assertFalse(reportText.contains("Leg Attack"), "Report still lists the Leg Attack:\n" + reportText);
        assertFalse(reportText.contains("Stop Swarm"), "Report still lists the Stop Swarm attack:\n" + reportText);
    }

    @Test
    public void reportShowsBothTechLevelRules() {
        Entity entity = elemental();

        String variableReport = CompositeTechLevelReport.toPlainText(entity, Faction.NONE, entity.getYear(), true);
        String basicReport = CompositeTechLevelReport.toPlainText(entity, Faction.NONE, entity.getYear(), false);

        assertTrue(variableReport.contains("Variable Tech Level"),
              "Variable report does not report the rule in use");
        assertTrue(basicReport.contains("Basic (static)"),
              "Basic report does not report the rule in use");
    }

    @Test
    public void identicalComponentsAreCollectedIntoOneCountedRow() {
        Entity atlas = getEntityForUnitTesting("Atlas AS7-D", false);
        assertNotNull(atlas, "Atlas AS7-D not found");

        String reportText = CompositeTechLevelReport.toPlainText(atlas, Faction.NONE, atlas.getYear(), true);

        // The Atlas carries 20 heat sinks and 4 medium lasers. Folding the same component in again cannot move
        // the unit's dates a second time, so they are counted on one row instead of repeated.
        assertTrue(reportText.contains("Heat Sink x20"),
              "Report does not collect the heat sinks into one row:\n" + reportText);
        assertTrue(reportText.contains("Medium Laser x4"),
              "Report does not collect the medium lasers into one row:\n" + reportText);
        assertFalse(reportText.contains("Heat Sink x1"), "A single component was given a count");
    }

    @Test
    public void htmlReportIsWellFormedAndEscaped() {
        Entity entity = elemental();

        String html = CompositeTechLevelReport.toHtml(entity, Faction.NONE, entity.getYear(), true);

        assertTrue(html.startsWith("<div class='report'>") && html.endsWith("</div>"),
              "HTML report is not wrapped in the themed report div");
        assertTrue(html.contains("<table") && html.contains("</table>"), "HTML report contains no table");
        assertTrue(html.contains("Armor: BA Standard"), "HTML report does not name the armor");
        // The marker is written as escaped text, so no stray unescaped "<--" may reach the pane.
        assertFalse(html.contains("<--"), "HTML report contains an unescaped '<--' marker");
    }
}
