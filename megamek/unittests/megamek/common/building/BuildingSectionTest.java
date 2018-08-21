/*
 * Copyright (c) 2018 The MegaMek Team. All rights reserved.
 *
 * This file is part of MegaMek.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ.  If not, see <http://www.gnu.org/licenses/>.
 */
package megamek.common.building;

import java.util.Arrays;
import java.util.Collections;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Test;

import megamek.common.Coords;
import megamek.test.TestUtilities;

public class BuildingSectionTest {

    @Test
    public void testStaticCtor() {

        Runnable[] npe = { // these throw NullPointerException
            () -> BuildingSection.of(null, BasementType.NONE, 0, 0, 0, false, false), // no coords
            () -> BuildingSection.of(new Coords(0,0), null, 0, 0, 0, false, false), // no basement type
        };
        for (int i = 0; i < npe.length; i++) {
            try { npe[i].run(); Assert.fail("npe index " + i); } catch (@SuppressWarnings("unused") NullPointerException expected) { /* ok */ } //$NON-NLS-1$
        }

        Runnable[] iae = { // these throw IllegalArgumentException
                () -> BuildingSection.of(new Coords(0,0), BasementType.NONE, -1, 0, 0, false, false), // negative current CF
                () -> BuildingSection.of(new Coords(0,0), BasementType.NONE, 0, -1, 0, false, false), // negative phase CF
                () -> BuildingSection.of(new Coords(0,0), BasementType.NONE, 0, 0, -1, false, false), // negative armor
            };
        for (int i = 0; i < iae.length; i++) {
            try { iae[i].run(); Assert.fail("iae index " + i); } catch (@SuppressWarnings("unused") IllegalArgumentException expected) { /* ok */ } //$NON-NLS-1$
        }

    }


    @Test
    public void testSerializable() {
        TestUtilities.assertEqualsASerializedClone(BuildingSection.of(new Coords(0,0), BasementType.NONE, 0, 0, 0, false, false));
    }

    @Test
    public void testEquals() {
        BuildingSection bs1 = BuildingSection.of(new Coords(0,0), BasementType.NONE, 0, 0, 0, false, false);
        BuildingSection bs2 = BuildingSection.of(new Coords(0,0), BasementType.NONE, 0, 0, 0, false, false);
        Assert.assertEquals(bs1, bs2);
    }

    @Test
    public void testDemolitionCharges() {
        BuildingSection  bs = BuildingSection.of(new Coords(0,0), BasementType.NONE, 0, 0, 0, false, false);
        DemolitionCharge dc = new DemolitionCharge(10, 10, bs.getCoordinates());

        // must add 2 charges
        Assert.assertEquals(0, bs.streamDemolitionCharges().count());
        bs.addDemolitionCharge(10, 10);
        bs.addDemolitionCharge(10, 10);
        Assert.assertEquals(2, bs.streamDemolitionCharges().count());

        // must reset to 1 charge
        Assert.assertEquals(2, bs.streamDemolitionCharges().count());
        bs.setDemolitionCharges(Collections.singleton(dc));
        Assert.assertEquals(1, bs.streamDemolitionCharges().count());

        // must also reset to 1 charge
        bs.addDemolitionCharge(10, 10);
        Assert.assertEquals(2, bs.streamDemolitionCharges().count());
        bs.setDemolitionCharges(Arrays.asList(dc, dc, dc, dc));
        Assert.assertEquals(1, bs.streamDemolitionCharges().count());

        // must clear charges
        Assert.assertEquals(1, bs.streamDemolitionCharges().count());
        bs.setDemolitionCharges(Collections.emptyList());
        Assert.assertEquals(0, bs.streamDemolitionCharges().count());

        // must also clear charges
        bs.addDemolitionCharge(10, 10);
        bs.addDemolitionCharge(10, 10);
        bs.addDemolitionCharge(10, 10);
        Assert.assertEquals(3, bs.streamDemolitionCharges().count());
        bs.streamDemolitionCharges().collect(Collectors.toList()).forEach(bs::removeDemolitionCharge);
        Assert.assertEquals(0, bs.streamDemolitionCharges().count());

        // must also clear charges
        bs.addDemolitionCharge(10, 10);
        bs.addDemolitionCharge(10, 10);
        bs.addDemolitionCharge(10, 10);
        Assert.assertEquals(3, bs.streamDemolitionCharges().count());
        bs.streamDemolitionCharges().collect(Collectors.toList()).stream().map(TestUtilities::cloneViaSerialization).forEach(bs::removeDemolitionCharge);
        Assert.assertEquals(0, bs.streamDemolitionCharges().count());

    }
}
