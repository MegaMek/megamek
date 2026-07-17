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
        String staticReport = CompositeTechLevelReport.toPlainText(entity, Faction.NONE, entity.getYear(), false);

        assertTrue(variableReport.contains("Variable Tech Level"),
              "Variable report does not report the rule in use");
        assertTrue(staticReport.contains("Static Tech Level"),
              "Static report does not report the rule in use");
    }

    @Test
    public void extinctionAndReintroductionAreTrackedInTheirOwnColumns() {
        // The Devastator DVS-2-EC carries Star League tech (a 300 XL engine) that goes extinct in the
        // Succession Wars and returns later, so the whole unit goes extinct and comes back.
        Entity devastator = getEntityForUnitTesting("Devastator DVS-2-EC", false);
        assertNotNull(devastator, "Devastator DVS-2-EC not found");

        String reportText = CompositeTechLevelReport.toPlainText(devastator, Faction.NONE, devastator.getYear(), true);

        assertTrue(reportText.contains("Equipment extinct") && reportText.contains("Equipment returns"),
              "Build-up table is missing the extinction columns:\n" + reportText);
        assertTrue(reportText.contains("Extinct:"), "Report has no extinction result line:\n" + reportText);

        // The 300 XL is the component that first drives the unit extinct, so its row carries the extinction and
        // reintroduction dates.
        String xlEngineBuildUpRow = reportText.lines()
              .filter(line -> line.contains("300 XL") && line.contains("Becomes Common"))
              .findFirst()
              .orElse("");
        assertTrue(xlEngineBuildUpRow.contains("2865") && xlEngineBuildUpRow.contains("3035"),
              "The XL engine row does not show the unit's extinction and return dates:\n" + reportText);
    }

    @Test
    public void unitEvaluatedBeforeItExistsReportsWhenItBecomesAvailable() {
        // The DVS-X10 MUSE EARTH is introduced in 3075, so in 3031 it cannot be built at all. A bare level
        // would read as "you can build this in 3031"; the report says when that actually becomes true.
        Entity devastator = getEntityForUnitTesting("Devastator DVS-X10 MUSE EARTH", false);
        assertNotNull(devastator, "Devastator DVS-X10 MUSE EARTH not found");

        String reportText = CompositeTechLevelReport.toPlainText(devastator, Faction.NONE, 3031, true);

        assertTrue(reportText.contains("Becomes Experimental (3075)"),
              "Report does not say when the unit becomes available:\n" + reportText);
    }

    @Test
    public void unitEvaluatedInsideItsExtinctionWindowReportsWhenItReturns() {
        // The Devastator DVS-2-EC carries Star League tech that is extinct through 3080, so in 2900 the unit
        // is not makeable; it becomes available again when the last lost component returns.
        Entity devastator = getEntityForUnitTesting("Devastator DVS-2-EC", false);
        assertNotNull(devastator, "Devastator DVS-2-EC not found");

        String reportText = CompositeTechLevelReport.toPlainText(devastator, Faction.NONE, 2900, true);

        assertTrue(reportText.contains("Becomes Advanced (3081)"),
              "Report does not say when the extinct unit returns:\n" + reportText);
    }

    @Test
    public void progressionNoteCarriesTheYearItSets() {
        Entity entity = elemental();

        String reportText = CompositeTechLevelReport.toPlainText(entity, Faction.NONE, entity.getYear(), true);

        // The note reads as plain English with the year in brackets rather than a bare label.
        assertTrue(reportText.contains("Becomes Common (3054)"),
              "Progression note does not carry the year it sets:\n" + reportText);
    }

    @Test
    public void aBackfilledProductionDateIsNotReportedAsAProgression() {
        // The 300 XL pushes the Devastator's common date out; the algorithm then backfills the production
        // column with the old common date. That backfill must not be reported as "Becomes Production" -- the
        // engine only makes the unit take longer to become common.
        Entity devastator = getEntityForUnitTesting("Devastator DVS-2-EC", false);
        assertNotNull(devastator, "Devastator DVS-2-EC not found");

        String reportText = CompositeTechLevelReport.toPlainText(devastator, Faction.NONE, devastator.getYear(), true);

        // The engine appears in both the components table and the build-up table; only the build-up row carries
        // a progression note, so check the note across every row that mentions the engine.
        List<String> xlEngineRows = reportText.lines()
              .filter(line -> line.contains("300 XL"))
              .toList();
        assertTrue(xlEngineRows.stream().anyMatch(row -> row.contains("Becomes Common")),
              "The XL engine should be flagged as driving the common date:\n" + reportText);
        assertTrue(xlEngineRows.stream().noneMatch(row -> row.contains("Becomes Production")),
              "A backfilled production date was wrongly reported as a progression:\n" + reportText);
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
