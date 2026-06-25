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
package megamek.common.units;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import megamek.common.equipment.EquipmentType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Verifies the {@link HeatBreakdown} that drives the Heat Phase report's "gains N heat" / "sinks N heat" breakdown
 * tooltips: contributions add to the running {@link Entity#heatBuildup} total and are recorded per source (with a
 * count, so several of the same weapon read "PPC x2"), the tooltips format correctly, and the breakdown survives entity
 * serialization.
 */
class HeatBuildupBreakdownTest {

    @BeforeAll
    static void initializeEquipment() {
        EquipmentType.initializeTypes();
    }

    @Test
    void tracksTotalCountAndItemizedContributions() {
        BipedMek mek = new BipedMek();
        mek.changeHeatBuildup(2, "Movement (Running)");
        mek.changeHeatBuildup(10, "PPC");
        mek.changeHeatBuildup(10, "PPC");
        mek.changeHeatBuildup(-9, "Cooling (water/coolant/etc.)");

        assertEquals(13, mek.heatBuildup, "The running total is the signed sum of every contribution");

        Map<String, HeatBreakdown.HeatContribution> breakdown = mek.getHeatBreakdown().buildup();
        assertEquals(2, breakdown.get("Movement (Running)").totalHeat());
        assertEquals(1, breakdown.get("Movement (Running)").count());
        // Two PPCs at 10 each must read as count 2 / total 20, not a single 20-heat weapon.
        assertEquals(20, breakdown.get("PPC").totalHeat(), "Same-source contributions are summed");
        assertEquals(2, breakdown.get("PPC").count(), "Each firing increments the source count");
        assertEquals(-9, breakdown.get("Cooling (water/coolant/etc.)").totalHeat(),
              "Cooling is recorded as negative heat");
        assertEquals(List.of("Movement (Running)", "PPC", "Cooling (water/coolant/etc.)"),
              new ArrayList<>(breakdown.keySet()), "Order reflects when each source first applied heat");
    }

    @Test
    void buildupTooltipShowsCountsSignsAndReconcilesRemainder() {
        HeatBreakdown breakdown = new HeatBreakdown();
        breakdown.addBuildup(2, "Movement (Running)");
        breakdown.addBuildup(10, "PPC");
        breakdown.addBuildup(10, "PPC");
        breakdown.addBuildup(-9, "Cooling (water/coolant/etc.)");

        // Reported total 16 = 13 itemized + 3 un-itemized, so a trailing "Other: +3" reconciles it.
        assertEquals(
              "Movement (Running): +2, PPC x2: +20 (10 each), Cooling (water/coolant/etc.): -9, Other: +3",
              breakdown.buildupTooltip(16));
    }

    @Test
    void dissipationTooltipListsEachSource() {
        HeatBreakdown breakdown = new HeatBreakdown();
        breakdown.addDissipation(10, "Heat sinks");
        breakdown.addDissipation(2, "Submerged");
        breakdown.addDissipation(2, "Submerged");

        assertEquals("Heat sinks: 10, Submerged: 4", breakdown.dissipationTooltip());
    }

    @Test
    void emptyBreakdownYieldsNoTooltip() {
        HeatBreakdown breakdown = new HeatBreakdown();
        assertEquals("", breakdown.buildupTooltip(0), "No tracked buildup means an empty tooltip");
        assertEquals("", breakdown.dissipationTooltip(), "No tracked dissipation means an empty tooltip");
    }

    @Test
    void clearEmptiesBothBreakdowns() {
        BipedMek mek = new BipedMek();
        mek.changeHeatBuildup(5, "Engine hits");
        mek.getHeatBreakdown().addDissipation(10, "Heat sinks");

        mek.clearHeatBreakdown();
        assertTrue(mek.getHeatBreakdown().buildup().isEmpty(), "Clearing resets the buildup breakdown");
        assertTrue(mek.getHeatBreakdown().dissipation().isEmpty(), "Clearing also resets the dissipation breakdown");
    }

    @Test
    void zeroOrUnlabeledHeatChangesTheTotalButIsNotItemized() {
        BipedMek mek = new BipedMek();
        mek.changeHeatBuildup(0, "No-op");
        mek.changeHeatBuildup(5, null);
        mek.changeHeatBuildup(4, "   ");

        assertEquals(9, mek.heatBuildup, "Heat still accrues even when no usable source label is given");
        assertTrue(mek.getHeatBreakdown().buildup().isEmpty(),
              "Zero or unlabeled contributions are not added to the itemized breakdown");
    }

    @Test
    void nullBreakdownIsLazilyRecreatedWhenHeatIsApplied() throws Exception {
        BipedMek mek = new BipedMek();
        // Simulate a unit restored from a stream written before the heatBreakdown field existed: Java
        // deserialization skips field initializers, so the field comes back null. Force that state directly.
        Field heatBreakdownField = Entity.class.getDeclaredField("heatBreakdown");
        heatBreakdownField.setAccessible(true);
        heatBreakdownField.set(mek, null);

        // Applying heat must not throw an NPE; the breakdown is lazily recreated and records the contribution.
        mek.changeHeatBuildup(10, "PPC");

        assertNotNull(mek.getHeatBreakdown(), "A null breakdown is lazily recreated to honor the never-null contract");
        assertEquals(10, mek.heatBuildup, "Heat still accrues after the breakdown is recreated");
        assertEquals(10, mek.getHeatBreakdown().buildup().get("PPC").totalHeat(),
              "The contribution is recorded in the recreated breakdown");
    }

    @Test
    void breakdownSurvivesSerialization() throws Exception {
        BipedMek mek = new BipedMek();
        mek.changeHeatBuildup(2, "Movement (Walking)");
        mek.changeHeatBuildup(8, "PPC");
        mek.getHeatBreakdown().addDissipation(10, "Heat sinks");

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {
            out.writeObject(mek);
        }
        BipedMek restored;
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
            restored = (BipedMek) in.readObject();
        }

        assertEquals(2, restored.getHeatBreakdown().buildup().get("Movement (Walking)").totalHeat(),
              "The heat breakdown must survive entity serialization");
        assertEquals(8, restored.getHeatBreakdown().buildup().get("PPC").totalHeat());
        assertEquals(10, restored.getHeatBreakdown().dissipation().get("Heat sinks"));
    }
}
