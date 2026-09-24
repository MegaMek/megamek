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

package megamek.common.equipment;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.stream.Stream;

import megamek.common.units.BipedMek;
import megamek.common.units.Entity;
import megamek.common.units.EntityMovementMode;
import megamek.common.units.FixedWingSupport;
import megamek.common.units.LargeSupportTank;
import megamek.common.units.SupportTank;
import megamek.common.units.SupportVTOL;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class SupportEngineRatingTest {
    @BeforeAll
    static void initializeTypes() {
        EquipmentType.initializeTypes();
    }

    static Stream<Arguments> supportRatings() {
        return Stream.of(
              Arguments.of(new SupportTank(), EntityMovementMode.HOVER, 2.5, 15, 37.5),
              Arguments.of(new SupportTank(), EntityMovementMode.TRACKED, 3.0, 8, 24.0),
              Arguments.of(new LargeSupportTank(), EntityMovementMode.WHEELED, 150.0, 2, 300.0),
              Arguments.of(new SupportVTOL(), EntityMovementMode.VTOL, 2.5, 15, 37.5),
              Arguments.of(new FixedWingSupport(), EntityMovementMode.AERODYNE, 4.999, 4, 19.996),
              Arguments.of(new FixedWingSupport(), EntityMovementMode.AERODYNE, 100.0, 6, 500.0),
              Arguments.of(new SupportTank(), EntityMovementMode.WHEELED, 25.0, 0, 0.0));
    }

    @ParameterizedTest
    @MethodSource("supportRatings")
    void ratingPreservesFractionalMassWithoutSuspensionOrRounding(Entity unit, EntityMovementMode mode,
          double tons, int cruiseMp, double expected) {
        unit.setMovementMode(mode);
        unit.setWeight(tons);
        unit.setOriginalWalkMP(cruiseMp);
        Engine engine = new Engine(0, Engine.COMBUSTION_ENGINE, Engine.SUPPORT_VEE_ENGINE);
        unit.setEngine(engine);
        assertEquals(expected, engine.getRating(unit), 0.000001);
    }

    @Test
    void ratingTracksDesignChangesAndIgnoresLegacyStoredSupportValues() {
        SupportTank unit = new SupportTank();
        unit.setWeight(2.5);
        unit.setOriginalWalkMP(15);
        Engine engine = new Engine(10, Engine.FUEL_CELL, Engine.SUPPORT_VEE_ENGINE);
        unit.setEngine(engine);
        assertEquals(37.5, engine.getRating(unit));
        unit.setWeight(3.0);
        assertEquals(45.0, engine.getRating(unit));
        unit.setOriginalWalkMP(8);
        assertEquals(24.0, engine.getRating(unit));
    }

    @Test
    void ordinaryEngineRatingsRemainFixed() {
        BipedMek unit = new BipedMek();
        unit.setWeight(50);
        unit.setOriginalWalkMP(1);
        Engine engine = new Engine(250, Engine.NORMAL_ENGINE, 0);
        unit.setEngine(engine);
        assertEquals(250, engine.getRating());
        assertEquals(250.0, engine.getRating(unit));
    }

    @Test
    void boobyTrapRoundsFinalFractionalDamageUp() {
        SupportTank unit = new SupportTank() {
            @Override
            public boolean hasBoobyTrap() {
                return true;
            }
        };
        unit.setWeight(2.5);
        unit.setOriginalWalkMP(15);
        unit.setEngine(new Engine(0, Engine.FUEL_CELL, Engine.SUPPORT_VEE_ENGINE));
        assertEquals(38, unit.getBoobyTrapDamage());
        unit.setWeight(100);
        assertEquals(500, unit.getBoobyTrapDamage());
    }

    @Test
    void unitsWithoutBoobyTrapsDoNoBoobyTrapDamage() {
        SupportTank unit = new SupportTank();
        unit.setWeight(2.5);
        unit.setOriginalWalkMP(15);
        assertEquals(0, unit.getBoobyTrapDamage());
    }
}
