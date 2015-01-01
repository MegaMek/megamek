/*
 * RandomUnitGeneratorTest.java
 *
 * MegaMek - Copyright (C) 2000-2011 Ben Mazur (bmazur@sev.org)
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License as published by the Free
 *  Software Foundation; either version 2 of the License, or (at your option)
 *  any later version.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *  or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 *  for more details.
 */

package megamek.client;

import megamek.common.MechSummary;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.Vector;

/**
 * @author Deric Page <deric dot page at gmail dot com>
 * @since: 5/15/14 2:04 PM
 * @version: %Id%
 */
@RunWith(JUnit4.class)
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

        RandomUnitGenerator testRug = Mockito.spy(new RandomUnitGenerator());
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
        RandomUnitGenerator.RatEntry mockRatEntry = Mockito.mock(RandomUnitGenerator.RatEntry.class);
        Mockito.when(mockRatEntry.getUnits()).thenReturn(testUnits);
        Mockito.when(mockRatEntry.getWeights()).thenReturn(testWeights);
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
        RandomUnitGenerator.RatEntry mockOtherRat = Mockito.mock(RandomUnitGenerator.RatEntry.class);
        Mockito.when(mockOtherRat.getUnits()).thenReturn(otherUnits);
        Mockito.when(mockOtherRat.getWeights()).thenReturn(otherWeights);
        testRug.addRat("Other RAT", mockOtherRat);

        MechSummary mockMech1 = Mockito.mock(MechSummary.class);
        Mockito.doReturn(mockMech1).when(testRug).getMechByName(M1);
        MechSummary mockMech2 = Mockito.mock(MechSummary.class);
        Mockito.doReturn(mockMech2).when(testRug).getMechByName(M2);
        MechSummary mockMech3 = Mockito.mock(MechSummary.class);
        Mockito.doReturn(mockMech3).when(testRug).getMechByName(M3);
        MechSummary mockMech4 = Mockito.mock(MechSummary.class);
        Mockito.doReturn(mockMech4).when(testRug).getMechByName(M4);
        MechSummary mockMech5 = Mockito.mock(MechSummary.class);
        Mockito.doReturn(mockMech5).when(testRug).getMechByName(M5);
        MechSummary mockMech6 = Mockito.mock(MechSummary.class);
        Mockito.doReturn(mockMech6).when(testRug).getMechByName(M6);
        MechSummary mockMech7 = Mockito.mock(MechSummary.class);
        Mockito.doReturn(mockMech7).when(testRug).getMechByName(M7);
        MechSummary mockMech8 = Mockito.mock(MechSummary.class);
        Mockito.doReturn(mockMech8).when(testRug).getMechByName(M8);

        ArrayList<MechSummary> expected = new ArrayList<>(1);
        expected.add(mockMech1);
        Mockito.doReturn(0.1).when(testRug).getRandom();
        Assert.assertEquals(expected, testRug.generate(1, "mockRat"));

        expected = new ArrayList<>(1);
        expected.add(mockMech2);
        Mockito.doReturn(0.5).when(testRug).getRandom();
        Assert.assertEquals(expected, testRug.generate(1, "mockRat"));

        expected = new ArrayList<>(1);
        expected.add(mockMech3);
        Mockito.doReturn(0.9).when(testRug).getRandom();
        Assert.assertEquals(expected, testRug.generate(1, "mockRat"));

        expected = new ArrayList<>(1);
        expected.add(mockMech8);
        Mockito.doReturn(0.99).when(testRug).getRandom();
        Assert.assertEquals(expected, testRug.generate(1, "mockRat"));
    }

}
