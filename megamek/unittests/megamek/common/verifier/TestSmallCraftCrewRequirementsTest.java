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
 */
package megamek.common.verifier;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import megamek.common.units.SmallCraft;
import org.junit.jupiter.api.Test;

class TestSmallCraftCrewRequirementsTest {

    @Test
    void smallCraftAtThresholdDoesNotRequireStandardCrewOrQuarters() {
        SmallCraft craft = smallCraftWithNoQuarters(5.0);
        TestSmallCraft verifier = new TestSmallCraft(craft, EntityVerifier.getInstance(null).aeroOption, null);

        assertFalse(TestSmallCraft.requiresMinimumCrewAndQuarters(craft));
        assertTrue(verifier.correctCrew(new StringBuffer()));
    }

    @Test
    void smallCraftAboveThresholdRequiresStandardCrewAndQuarters() {
        SmallCraft craft = smallCraftWithNoQuarters(30);
        TestSmallCraft verifier = new TestSmallCraft(craft, EntityVerifier.getInstance(null).aeroOption, null);

        assertTrue(TestSmallCraft.requiresMinimumCrewAndQuarters(craft));
        assertFalse(verifier.correctCrew(new StringBuffer()));
    }

    private SmallCraft smallCraftWithNoQuarters(double tonnage) {
        SmallCraft craft = new SmallCraft();
        craft.setWeight(tonnage);
        craft.setNCrew(1);
        craft.setNOfficers(0);
        craft.setNGunners(0);
        return craft;
    }
}