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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Vector;

import org.junit.jupiter.api.Test;

import megamek.client.generator.RandomUnitGenerator.RatEntry;
import megamek.common.MekSummary;

/**
 * @author Deric Page <deric dot page at gmail dot com>
 * @since 5/15/14 2:04 PM
 */
class RandomUnitGeneratorTest {

    @Test
    void testGenerate() {
        final String M1 = "Mek 1";
        final String M2 = "Mek 2";
        final String M3 = "Mek 3";
        final String M4 = "Mek 4";
        final String M5 = "Mek 5";
        final String M6 = "Mek 6";
        final String M7 = "Mek 7";
        final String M8 = "Mek 8";

        RandomUnitGenerator testRug = spy(new RandomUnitGenerator());

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

        MekSummary mockMek1 = mock(MekSummary.class);
        doReturn(mockMek1).when(testRug).getMekByName(M1);
        MekSummary mockMek2 = mock(MekSummary.class);
        doReturn(mockMek2).when(testRug).getMekByName(M2);
        MekSummary mockMek3 = mock(MekSummary.class);
        doReturn(mockMek3).when(testRug).getMekByName(M3);
        MekSummary mockMek4 = mock(MekSummary.class);
        doReturn(mockMek4).when(testRug).getMekByName(M4);
        MekSummary mockMek5 = mock(MekSummary.class);
        doReturn(mockMek5).when(testRug).getMekByName(M5);
        MekSummary mockMek6 = mock(MekSummary.class);
        doReturn(mockMek6).when(testRug).getMekByName(M6);
        MekSummary mockMek7 = mock(MekSummary.class);
        doReturn(mockMek7).when(testRug).getMekByName(M7);
        MekSummary mockMek8 = mock(MekSummary.class);
        doReturn(mockMek8).when(testRug).getMekByName(M8);

        ArrayList<MekSummary> expected = new ArrayList<>(1);
        expected.add(mockMek1);
        doReturn(0.1).when(testRug).getRandom();
        assertEquals(expected, testRug.generate(1, "mockRat"));

        expected = new ArrayList<>(1);
        expected.add(mockMek2);
        doReturn(0.5).when(testRug).getRandom();
        assertEquals(expected, testRug.generate(1, "mockRat"));

        expected = new ArrayList<>(1);
        expected.add(mockMek3);
        doReturn(0.9).when(testRug).getRandom();
        assertEquals(expected, testRug.generate(1, "mockRat"));

        expected = new ArrayList<>(1);
        expected.add(mockMek8);
        doReturn(0.99).when(testRug).getRandom();
        assertEquals(expected, testRug.generate(1, "mockRat"));
    }
}
