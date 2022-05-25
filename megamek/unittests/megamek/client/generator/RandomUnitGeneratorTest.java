/*
 * Copyright (c) 2000-2011 - Ben Mazur (bmazur@sev.org)
 * Copyright (c) 2022 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */
package megamek.client.generator;

import megamek.client.generator.RandomUnitGenerator.RatEntry;
import megamek.common.MechSummary;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Vector;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

/**
 * @author Deric Page <deric dot page at gmail dot com>
 * @since 5/15/14 2:04 PM
 */
public class RandomUnitGeneratorTest {

    @Test
    public void testGenerate() {
        final String M1 = "Mech 1";
        final String M2 = "Mech 2";
        final String M3 = "Mech 3";
        final String M4 = "Mech 4";
        final String M5 = "Mech 5";
        final String M6 = "Mech 6";
        final String M7 = "Mech 7";
        final String M8 = "Mech 8";

        RandomUnitGenerator testRug = spy(new RandomUnitGenerator());
        testRug.initRats();

        Vector<String> testUnits = new Vector<>(4);
        Vector<Float> testWeights = new Vector<>(4);
        testUnits.add(M1);
        testWeights.add(0.2F);
        testUnits.add(M2);
        testWeights.add(0.3F);
        testUnits.add(M3);
        testWeights.add(0.4F);
        testUnits.add("@Other RAT");
        testWeights.add(0.1F);
        RatEntry mockRatEntry = mock(RatEntry.class);
        when(mockRatEntry.getUnits()).thenReturn(testUnits);
        when(mockRatEntry.getWeights()).thenReturn(testWeights);
        testRug.addRat("mockRat", mockRatEntry);

        Vector<String> otherUnits = new Vector<>(5);
        Vector<Float> otherWeights = new Vector<>(5);
        otherUnits.add(M4);
        otherWeights.add(0.2F);
        otherUnits.add(M5);
        otherWeights.add(0.2F);
        otherUnits.add(M6);
        otherWeights.add(0.2F);
        otherUnits.add(M7);
        otherWeights.add(0.2F);
        otherUnits.add(M8);
        otherWeights.add(0.2F);
        RatEntry mockOtherRat = mock(RatEntry.class);
        when(mockOtherRat.getUnits()).thenReturn(otherUnits);
        when(mockOtherRat.getWeights()).thenReturn(otherWeights);
        testRug.addRat("Other RAT", mockOtherRat);

        MechSummary mockMech1 = mock(MechSummary.class);
        doReturn(mockMech1).when(testRug).getMechByName(M1);
        MechSummary mockMech2 = mock(MechSummary.class);
        doReturn(mockMech2).when(testRug).getMechByName(M2);
        MechSummary mockMech3 = mock(MechSummary.class);
        doReturn(mockMech3).when(testRug).getMechByName(M3);
        MechSummary mockMech4 = mock(MechSummary.class);
        doReturn(mockMech4).when(testRug).getMechByName(M4);
        MechSummary mockMech5 = mock(MechSummary.class);
        doReturn(mockMech5).when(testRug).getMechByName(M5);
        MechSummary mockMech6 = mock(MechSummary.class);
        doReturn(mockMech6).when(testRug).getMechByName(M6);
        MechSummary mockMech7 = mock(MechSummary.class);
        doReturn(mockMech7).when(testRug).getMechByName(M7);
        MechSummary mockMech8 = mock(MechSummary.class);
        doReturn(mockMech8).when(testRug).getMechByName(M8);

        ArrayList<MechSummary> expected = new ArrayList<>(1);
        expected.add(mockMech1);
        doReturn(0.1).when(testRug).getRandom();
        assertEquals(expected, testRug.generate(1, "mockRat"));

        expected = new ArrayList<>(1);
        expected.add(mockMech2);
        doReturn(0.5).when(testRug).getRandom();
        assertEquals(expected, testRug.generate(1, "mockRat"));

        expected = new ArrayList<>(1);
        expected.add(mockMech3);
        doReturn(0.9).when(testRug).getRandom();
        assertEquals(expected, testRug.generate(1, "mockRat"));

        expected = new ArrayList<>(1);
        expected.add(mockMech8);
        doReturn(0.99).when(testRug).getRandom();
        assertEquals(expected, testRug.generate(1, "mockRat"));
    }
}
