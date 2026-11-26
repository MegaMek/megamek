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
package megamek.client.ratgenerator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import megamek.common.units.Crew;
import megamek.common.units.CrewType;
import megamek.common.units.Entity;
import megamek.common.units.UnitType;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Covers rerolling one unit of a generated force. In the test data the Lyrans field the Archer ARC-2R, the Atlas
 * AS7-D, and two custom heavy Archers (ARC-8Z, ARC-9X) that reach their table through their unit files.
 */
class UnitRerollerTest {

    private static final int ERA = 3050;
    private static final String FACTION = "LA";
    private static final String ARCHER = "Archer ARC-2R";

    @BeforeAll
    static void loadForceGeneratorFromTestData() throws Exception {
        ForceGeneratorTestFixture.loadFromTestData(ERA);
        Ruleset.loadData();
    }

    @AfterAll
    static void clearSharedSingletons() throws Exception {
        ForceGeneratorTestFixture.reset();
    }

    @Test
    void rerollDrawsAnotherDesignAndKeepsThePilot() {
        ForceDescriptor slot = archerInLance();
        CrewDescriptor pilot = slot.getCo();
        int archerWeightClass = slot.getEntity().getWeightClass();

        UnitReroller.Outcome outcome = UnitReroller.reroll(slot);

        assertNotNull(outcome, "Other designs are there to take the Archer's place");
        assertEquals(ARCHER, outcome.before());
        assertNotEquals(ARCHER, outcome.after(), "The design being replaced is never drawn again");
        assertEquals(outcome.after(), slot.getModelName());
        assertEquals(outcome.after(), slot.getEntity().getShortName(), "The unit itself is rebuilt, not the record");
        assertEquals(archerWeightClass, slot.getEntity().getWeightClass(),
              "A heavy is there to draw, so the slot stays heavy rather than taking the Atlas");
        assertSame(pilot, slot.getCo(), "The same pilot moves into the new unit");
        assertTrue(slot.getRejectedModels().isEmpty(), "The ruled-out designs must not outlive the reroll");
    }

    @Test
    void aStruckOutUnitComesBackInWhenRerolled() {
        ForceDescriptor slot = archerInLance();
        slot.setIncludedRecursively(false);

        assertNotNull(UnitReroller.reroll(slot));

        assertTrue(slot.isIncluded());
    }

    @Test
    void aUnitThatWouldBreakItsFormationIsNotTaken() {
        ForceDescriptor slot = archerInLance();
        // A Light Battle lance must be mostly lights with a Scout, which no design here is. The first test is the same
        // lance without the formation, where a new unit is taken - so it is the formation that refuses them here.
        slot.getParent().setFormationType(FormationType.getFormationType("Light Battle"));
        Entity archer = slot.getEntity();

        assertNull(UnitReroller.reroll(slot));

        assertEquals(ARCHER, slot.getModelName(), "The slot keeps the unit it had");
        assertSame(archer, slot.getEntity());
        assertTrue(slot.getRejectedModels().isEmpty(), "Cleared on a failed reroll too");
    }

    @Test
    void aFormationIsNotRerolledAsOneUnit() {
        ForceDescriptor slot = archerInLance();

        assertEquals(UnitReroller.Refusal.NOT_A_UNIT, UnitReroller.whyNotRerollable(slot.getParent()));
        assertNull(UnitReroller.reroll(slot.getParent()));
    }

    @Test
    void aShipIsLeftToRerollShip() {
        ForceDescriptor ship = lance().createChild(0);
        ship.setElement(true);
        ship.setUnitType(UnitType.DROPSHIP);

        assertEquals(UnitReroller.Refusal.SHIP, UnitReroller.whyNotRerollable(ship));
    }

    @Test
    void aFormationThatFieldsOneDesignRefusesASingleReroll() {
        ForceDescriptor slot = archerInLance();
        slot.getParent().getModels().add(ARCHER);

        assertEquals(UnitReroller.Refusal.ONE_DESIGN_FORMATION, UnitReroller.whyNotRerollable(slot));
        assertNull(UnitReroller.reroll(slot));
        assertEquals(ARCHER, slot.getModelName());
    }

    @Test
    void aRerollableUnitHasNoRefusal() {
        assertNull(UnitReroller.whyNotRerollable(archerInLance()));
    }

    private static ForceDescriptor lance() {
        ForceDescriptor lance = new ForceDescriptor();
        lance.setFaction(FACTION);
        lance.setYear(ERA);
        lance.setRating("A");
        lance.setUnitType(UnitType.MEK);
        return lance;
    }

    /** A one-unit lance holding a loaded Archer with a pilot. */
    private static ForceDescriptor archerInLance() {
        ForceDescriptor lance = lance();
        ForceDescriptor slot = lance.createChild(0);
        lance.addSubForce(slot);
        slot.setUnit(RATGenerator.getInstance().getModelRecord(ARCHER));
        CrewDescriptor pilot = mock(CrewDescriptor.class);
        when(pilot.createCrew(any(CrewType.class))).thenAnswer(invocation -> new Crew(invocation.getArgument(0)));
        slot.setCo(pilot);
        slot.loadEntities(null, 0);
        return slot;
    }
}
