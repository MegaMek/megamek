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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

import megamek.common.Player;
import megamek.common.Report;
import megamek.common.rolls.Roll;
import megamek.common.units.Crew;
import megamek.common.units.Entity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Renders the Tainted and Toxic Atmospheres reports and checks that what a player reads is what the code meant.
 * <p>
 * These reports pair a message template with a matching number of {@code add} calls, and nothing in the compiler
 * checks that the two agree. When they disagree every field after the mismatch slides along by one: a playtest saw
 * "needs Hammershome+ to set the hex alight" - the owning player's name in the target number's place - because
 * {@link Report#addDesc} contributes two fields, the unit and its owner, rather than one. Worse, the {@code msg}
 * selector reads a field too, so the shifted report announced "no ignition" while starting a fire. Rendering each
 * report here is what catches that.
 */
class TaintedAtmosphereReportTest {

    private static final String OWNER_NAME = "Hammershome";
    private static final String UNIT_NAME = "Phoenix Hawk PXH-1b";

    private Entity reportingUnit() {
        Entity entity = mock(Entity.class);
        Crew crew = mock(Crew.class);
        lenient().when(crew.getSize()).thenReturn(1);
        lenient().when(crew.getNickname()).thenReturn("");
        lenient().when(entity.getCrew()).thenReturn(crew);
        lenient().when(entity.getId()).thenReturn(7);
        lenient().when(entity.getShortName()).thenReturn(UNIT_NAME);
        lenient().when(entity.getOwner()).thenReturn(new Player(1, OWNER_NAME));
        return entity;
    }

    /**
     * The report text as a player reads it, with the HTML stripped out. Rolls in particular are wrapped in a tooltip
     * link, so "rolled 9" is only contiguous once the markup is gone.
     *
     * @param report the report to render
     *
     * @return the rendered text without any markup
     */
    private String plainTextOf(Report report) {
        return report.text().replaceAll("<[^>]*>", "");
    }

    private Roll rollOf(int value) {
        Roll roll = mock(Roll.class);
        lenient().when(roll.getIntValue()).thenReturn(value);
        lenient().when(roll.toString()).thenReturn(String.valueOf(value));
        lenient().when(roll.getReport()).thenReturn(String.valueOf(value));
        return roll;
    }

    /**
     * Asserts that a rendered report used every field it was given and asked for no more than it was given. A left
     * over {@code <data>} means the template wants more fields than the code supplies; the reporting-error text means
     * it wants more than exist at all.
     *
     * @param renderedText the text the player would see
     */
    private void assertRendersCleanly(String renderedText) {
        assertFalse(renderedText.contains("<data>"),
              "an unfilled <data> placeholder means the code did not add enough fields: " + renderedText);
        assertFalse(renderedText.contains("Reporting Error"),
              "the template asked for more fields than the code added: " + renderedText);
        assertFalse(renderedText.contains(OWNER_NAME + "+"),
              "the owner's name landed in the target number's place: " + renderedText);
    }

    @Test
    @DisplayName("A successful jump ignition names the target, the hex and the fire it started")
    void jumpIgnitionSuccessReadsCorrectly() {
        Report report = new Report(7712);
        report.subject = 7;
        report.addDesc(reportingUnit());
        report.add(7);
        report.add("0811");
        report.add(rollOf(9));
        report.choose(true);

        String renderedText = plainTextOf(report);

        assertRendersCleanly(renderedText);
        assertTrue(renderedText.contains(UNIT_NAME), "the unit should be named: " + renderedText);
        assertTrue(renderedText.contains(OWNER_NAME), "the owner should be named: " + renderedText);
        assertTrue(renderedText.contains("needs 7+"), "the target number should be 7: " + renderedText);
        assertTrue(renderedText.contains("hex 0811"), "the hex should be named: " + renderedText);
        assertTrue(renderedText.contains("rolled 9"), "the roll should be 9: " + renderedText);
        assertTrue(renderedText.contains("catches fire"),
              "a successful roll must not read as 'no ignition': " + renderedText);
    }

    @Test
    @DisplayName("A failed jump ignition reads as no ignition")
    void jumpIgnitionFailureReadsCorrectly() {
        Report report = new Report(7712);
        report.subject = 7;
        report.addDesc(reportingUnit());
        report.add(7);
        report.add("0811");
        report.add(rollOf(4));
        report.choose(false);

        String renderedText = plainTextOf(report);

        assertRendersCleanly(renderedText);
        assertTrue(renderedText.contains("no ignition"), "a failed roll should say so: " + renderedText);
        assertFalse(renderedText.contains("catches fire"),
              "a failed roll must not announce a fire: " + renderedText);
    }

    @Test
    @DisplayName("A hot unit igniting its own hex names the heat, the target, the hex and the roll")
    void spontaneousIgnitionReadsCorrectly() {
        Report report = new Report(7707);
        report.subject = 7;
        report.addDesc(reportingUnit());
        report.add(18);
        report.add(10);
        report.add("0811");
        report.add(rollOf(11));
        report.choose(true);

        String renderedText = plainTextOf(report);

        assertRendersCleanly(renderedText);
        assertTrue(renderedText.contains("18 heat"), "the heat should be 18: " + renderedText);
        assertTrue(renderedText.contains("needs 10+"), "the target number should be 10: " + renderedText);
        assertTrue(renderedText.contains("hex 0811"), "the hex should be named: " + renderedText);
        assertTrue(renderedText.contains("catches fire"), "the roll succeeded: " + renderedText);
    }

    @Test
    @DisplayName("An exhaust wash names the target, the hex, the roll and which end of the flight it was")
    void exhaustWashReadsCorrectly() {
        Report takeoffReport = new Report(TaintedAtmosphereHandler.ExhaustWashMoment.TAKEOFF.reportId());
        takeoffReport.subject = 7;
        takeoffReport.addDesc(reportingUnit());
        takeoffReport.add(6);
        takeoffReport.add("0811");
        takeoffReport.add(rollOf(8));
        takeoffReport.choose(true);

        String takeoffText = plainTextOf(takeoffReport);
        assertRendersCleanly(takeoffText);
        assertTrue(takeoffText.contains("needs 6+"), "the target number should be 6: " + takeoffText);
        assertTrue(takeoffText.contains("hex 0811"), "the hex should be named: " + takeoffText);
        assertTrue(takeoffText.contains("takes off"), "a takeoff should say so: " + takeoffText);
        assertTrue(takeoffText.contains("catch fire"), "the roll succeeded: " + takeoffText);

        Report landingReport = new Report(TaintedAtmosphereHandler.ExhaustWashMoment.LANDING.reportId());
        landingReport.subject = 7;
        landingReport.addDesc(reportingUnit());
        landingReport.add(6);
        landingReport.add("0811");
        landingReport.add(rollOf(4));
        landingReport.choose(false);

        String landingText = plainTextOf(landingReport);
        assertRendersCleanly(landingText);
        assertTrue(landingText.contains("lands"), "a landing should say so: " + landingText);
        assertTrue(landingText.contains("no ignition"), "the roll failed: " + landingText);
    }

    @Test
    @DisplayName("A battle armor suit breach names the trooper, the target and the roll")
    void battleArmorSuitBreachReadsCorrectly() {
        Report report = new Report(7702);
        report.subject = 7;
        report.add("TR1");
        report.add(9);
        report.add(rollOf(10));
        report.choose(true);

        String renderedText = plainTextOf(report);

        assertRendersCleanly(renderedText);
        assertTrue(renderedText.contains("TR1"), "the trooper should be named: " + renderedText);
        assertTrue(renderedText.contains("needs 9+"), "the target number should be 9: " + renderedText);
        assertTrue(renderedText.contains("killed by the atmosphere"), "the roll succeeded: " + renderedText);
    }

    @Test
    @DisplayName("The reports that only name a unit render both the unit and its owner")
    void unitOnlyReportsRenderCleanly() {
        for (int messageId : new int[] { 7700, 7701, 7710, 7716 }) {
            Report report = new Report(messageId);
            report.subject = 7;
            report.addDesc(reportingUnit());

            String renderedText = plainTextOf(report);

            assertRendersCleanly(renderedText);
            assertTrue(renderedText.contains(UNIT_NAME), messageId + " should name the unit: " + renderedText);
            assertTrue(renderedText.contains(OWNER_NAME), messageId + " should name the owner: " + renderedText);
        }
    }

    @Test
    @DisplayName("The exposure reports name the unit, its owner and how long it has been out there")
    void exposureReportsRenderCleanly() {
        Report infantryReport = new Report(7705);
        infantryReport.subject = 7;
        infantryReport.addDesc(reportingUnit());
        infantryReport.add(31);
        infantryReport.add(4);
        assertRendersCleanly(plainTextOf(infantryReport));
        assertTrue(plainTextOf(infantryReport).contains("31 turns"), plainTextOf(infantryReport));

        Report vehicleReport = new Report(7706);
        vehicleReport.subject = 7;
        vehicleReport.addDesc(reportingUnit());
        vehicleReport.add(91);
        assertRendersCleanly(plainTextOf(vehicleReport));
        assertTrue(plainTextOf(vehicleReport).contains("91 turns"), plainTextOf(vehicleReport));
    }

    @Test
    @DisplayName("The jump exhaust auto-ignition report names the unit, its owner and the hex")
    void automaticJumpIgnitionRendersCleanly() {
        Report report = new Report(7711);
        report.subject = 7;
        report.addDesc(reportingUnit());
        report.add("0811");

        String renderedText = plainTextOf(report);

        assertRendersCleanly(renderedText);
        assertTrue(renderedText.contains("hex 0811"), "the hex should be named: " + renderedText);
    }

    @Test
    @DisplayName("The remaining atmosphere reports render every field they are given")
    void remainingReportsRenderCleanly() {
        Report fireSpreadReport = new Report(7715);
        fireSpreadReport.subject = 7;
        fireSpreadReport.add("0811");
        assertRendersCleanly(plainTextOf(fireSpreadReport));

        Report doubledDamageReport = new Report(7717);
        doubledDamageReport.subject = 7;
        assertRendersCleanly(plainTextOf(doubledDamageReport));

        Report causticDamageReport = new Report(7718);
        causticDamageReport.subject = 7;
        causticDamageReport.add(4);
        assertRendersCleanly(plainTextOf(causticDamageReport));

        Report infantryOriginReport = new Report(7719);
        infantryOriginReport.subject = 7;
        infantryOriginReport.add(12);
        assertRendersCleanly(plainTextOf(infantryOriginReport));
    }
}
