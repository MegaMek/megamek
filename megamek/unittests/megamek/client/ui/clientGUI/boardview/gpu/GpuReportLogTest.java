/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import megamek.common.Player;
import megamek.common.Report;
import megamek.common.enums.GamePhase;
import megamek.common.units.Crew;
import megamek.common.units.Entity;
import megamek.common.units.Mek;
import org.junit.jupiter.api.Test;

class GpuReportLogTest {
    @Test
    void informationLinksRetainTheirExactTextAndExplanationsAfterHtmlCleanup() {
        var text = GpuReportLog.linkedText("<b>Laser</b> at <a href='#entity:42'>Atlas &amp; Co</a> needs "
              + "<a href=\"#tooltip:4 (pilot's skill)<br>+ 3 (target movement)\">7</a>, rolls "
              + "<a href='#tooltip:Dice: &lt;4 + 5&gt;'>9</a> : hits.<br>"
              + "<a href='#entity:99'>????</a>");
        assertEquals("Laser at Atlas & Co needs 7, rolls 9 : hits.\n????", text.text());
        assertEquals(3, text.links().size(), "Obscured identities must not become active links");
        var unit = text.links().getFirst();
        assertEquals(42, unit.unitId());
        assertEquals("Atlas & Co", text.text().substring(unit.start(), unit.end()));
        var need = text.links().get(1);
        assertEquals("7", text.text().substring(need.start(), need.end()));
        assertEquals("4 (pilot's skill)\n+ 3 (target movement)", need.detail());
        assertEquals("Dice: <4 + 5>", text.links().getLast().detail());
        var heat = GpuReportLog.linkedText(new Report(3150)
              .addDataWithTooltip("12", "Weapon's heat: 10<br>Movement: 2").text());
        assertEquals("Weapon's heat: 10\nMovement: 2", heat.links().getFirst().detail(),
              "Existing reports put unescaped apostrophes and HTML inside single-quoted tooltip attributes");
    }

    @Test
    void thumbnailsAreCapturedOnlyForDisclosedUnitsAndRetainedWithTheirHistory() {
        var log = new GpuReportLog();
        var atlas = unit(42, "Atlas");
        var pixels = new BoardScene.Pixels(new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB));
        var requests = new AtomicInteger();
        List<List<Report>> history = new ArrayList<>(List.of(new ArrayList<>(List.of(
              new Report(3000), new Report(6065).addDesc(atlas).add(10).add("Left Torso")))));
        var first = log.capture(history, 1, GamePhase.FIRING_REPORT, id -> {
            assertEquals(42, id);
            requests.incrementAndGet();
            return pixels;
        });
        assertSame(pixels, first.icons().get(42));
        var next = log.capture(history, 1, GamePhase.PHYSICAL, id -> {
            requests.incrementAndGet();
            return null;
        });
        assertSame(pixels, next.icons().get(42));
        assertEquals(1, requests.get());
        history.clear();
        assertTrue(log.capture(history, 0, GamePhase.LOUNGE).icons().isEmpty());
    }

    @Test
    void receivedReportsKeepShooterTargetRollsAndDamageTogetherWithoutMixingTargets() throws Exception {
        Entity shooter = unit(7, "Timber Wolf Prime");
        Entity target = unit(42, "Atlas AS7-D");
        Entity other = unit(43, "Locust LCT-1V");
        List<Report> reports = new ArrayList<>(List.of(new Report(3000), new Report(3100).addDesc(shooter)));
        reports.addAll(attack(target, "ER Large Laser", true));
        reports.addAll(attack(other, "LRM 20", false));
        // Exercise the actual wire boundary: subject is transient and must not be used as a client-side identity.
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {
            out.writeObject(reports);
        }
        List<Report> received;
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
            @SuppressWarnings("unchecked") List<Report> restored = (List<Report>) in.readObject();
            received = restored;
        }
        var entries = GpuReportLog.format(3, received);
        assertEquals(2, entries.size());
        var hit = entries.getFirst();
        assertTrue(hit.matches(3, GamePhase.FIRING.localizedName(), 42, "laser"));
        assertTrue(hit.matches(0, "", 7, ""));
        assertFalse(hit.matches(3, "", 43, ""));
        assertFalse(entries.getLast().matches(0, "", 42, ""));
        assertTrue(hit.text().contains("needs 7, rolls 9 : hits"), hit.text());
        assertTrue(hit.text().contains("takes 10 damage to Left Torso"));
        assertTrue(hit.rolls().contains("gunnery"));
        assertTrue(hit.rolls().contains("target movement"));
        assertTrue(hit.matches(0, "", -1, "target movement"));
        assertFalse(hit.text().contains("<"));
    }

    @Test
    void phaseBoundariesResetTheAttackerAndRepeatedEventsAreRetained() {
        Entity shooter = unit(7, "Timber Wolf Prime");
        Entity target = unit(42, "Atlas AS7-D");
        List<Report> reports = new ArrayList<>(List.of(new Report(3000), new Report(3100).addDesc(shooter)));
        reports.addAll(attack(target, "ER Large Laser", false));
        reports.addAll(attack(target, "ER Large Laser", false));
        reports.add(new Report(5000));
        reports.add(new Report(6065).addDesc(target).add(5).add("Right Arm"));
        var entries = GpuReportLog.format(2, reports);
        assertEquals(3, entries.size());
        assertEquals(entries.get(0).text(), entries.get(1).text());
        assertEquals("", entries.getLast().heading());
        assertEquals(List.of(42), entries.getLast().units().stream().map(GpuReportLog.Unit::id).toList());
        assertFalse(entries.getLast().phase().equals(entries.getFirst().phase()));
    }

    @Test
    void plainPhysicalTargetsUseDisclosedNamesWithoutConfusingDuplicateChassis() {
        Entity shooter = unit(7, "Timber Wolf Prime");
        Entity first = unit(42, "Atlas AS7-D");
        Entity second = unit(43, "Atlas AS7-D #2");
        Report punch = new Report(4010).add("Left arm").add("Atlas AS7-D #2 (OpFor)");
        punch.newlines = 0;
        var entries = GpuReportLog.format(1, List.of(new Report(4000), new Report(4005).addDesc(shooter), punch,
              new Report(4035), new Report(5000), new Report(6065).addDesc(first).add(5).add("Right Arm"),
              new Report(6065).addDesc(second).add(5).add("Right Arm")));
        assertTrue(entries.getFirst().matches(0, "", 43, ""));
        assertFalse(entries.getFirst().matches(0, "", 42, ""));
    }

    @Test
    void obscuredReportsDoNotInventIdentitiesFromTransientSubjectsOrSpriteMarkers() {
        Report hidden = new Report(3115).add("Laser").add("????").add("????");
        hidden.subject = 123;
        var entry = GpuReportLog.format(1, List.of(new Report(3000), hidden)).getFirst();
        assertTrue(entry.units().isEmpty());
        assertTrue(entry.text().contains("????"));
        assertFalse(entry.matches(0, "", 123, ""));
    }

    @Test
    void cacheRefreshesAppendedAndReplacedHistoryAndRetiresProvisionalReports() {
        GpuReportLog log = new GpuReportLog();
        List<Report> round = new ArrayList<>(List.of(new Report(2000), new Report(2165).add("Locust")));
        List<List<Report>> history = new ArrayList<>(List.of(round));
        var first = log.capture(history, 1, GamePhase.MOVEMENT);
        assertSame(first, log.capture(history, 1, GamePhase.MOVEMENT));
        log.live(new Report(2165).add("Atlas").text(), 1, GamePhase.MOVEMENT);
        var live = log.capture(history, 1, GamePhase.MOVEMENT);
        assertEquals(2, live.entries().size());
        log.live(new Report(2165).add("Atlas").text(), 1, GamePhase.MOVEMENT);
        assertSame(live, log.capture(history, 1, GamePhase.MOVEMENT));
        round.add(new Report(2165).add("Atlas"));
        var finalReport = log.capture(history, 1, GamePhase.MOVEMENT_REPORT);
        assertTrue(finalReport.entries().stream().noneMatch(entry -> entry.heading().equals("Live resolution")));
        assertTrue(finalReport.entries().stream().anyMatch(entry -> entry.text().contains("Atlas")));
        history.set(0, new ArrayList<>(List.of(new Report(3000), new Report(3220))));
        var replacement = log.capture(history, 1, GamePhase.FIRING_REPORT);
        assertNotSame(finalReport, replacement);
        assertFalse(replacement.entries().stream().anyMatch(entry -> entry.text().contains("Atlas")));
        history.clear();
        assertTrue(log.capture(history, 0, GamePhase.LOUNGE).entries().isEmpty());
    }

    static Entity unit(int id, String name) {
        Entity unit = mock(Mek.class);
        when(unit.getId()).thenReturn(id);
        when(unit.getShortName()).thenReturn(name);
        when(unit.getOwner()).thenReturn(new Player(id, id == 7 ? "Raven's Nest" : "OpFor"));
        Crew crew = mock(Crew.class);
        when(unit.getCrew()).thenReturn(crew);
        when(crew.getNickname()).thenReturn("");
        return unit;
    }

    static List<Report> attack(Entity target, String weapon, boolean hit) {
        Report fire = new Report(3115).add(weapon).addDesc(target);
        fire.newlines = 0;
        Report need = new Report(3150).addDataWithTooltip("7", "4 (gunnery)<br>+ 3 (target movement)");
        need.newlines = 0;
        Report roll = new Report(3155).add(9);
        roll.newlines = 0;
        List<Report> result = new ArrayList<>(List.of(fire, need, roll, new Report(hit ? 3390 : 3220)));
        if (hit) {
            result.add(new Report(6065).addDesc(target).add(10).add("Left Torso"));
            result.add(new Report(6085).add(12));
        }
        result.getLast().newlines = 2;
        return result;
    }
}
