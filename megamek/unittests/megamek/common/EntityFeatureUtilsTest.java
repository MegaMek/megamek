/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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
package megamek.common;

import static megamek.testUtilities.MMTestUtilities.getEntityForUnitTesting;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;

import megamek.ai.utility.EntityFeatureUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Test class for EntityFeatureUtils.
 *
 * @author Luana Coppio
 */
public class EntityFeatureUtilsTest {

    private static final MekSummaryCache mekSummaryCache = MekSummaryCache.getInstance(true);
    private static Warship warship;
    private static SpaceStation spaceStation;
    private static Jumpship jumpship;
    private static LandAirMek landAirMek;
    private static SuperHeavyTank superHeavyTank;
    private static SupportTank supportTank;
    private static VTOL vtol;
    private static SupportVTOL supportVtol;
    private static Tank tank;
    private static Mek mek;
    private static ProtoMek protoMek;
    private static Infantry infantry;
    private static SmallCraft smallCraft;
    private static BattleArmor battleArmor;
    private static AeroSpaceFighter aeroSpaceFighter;
    private static ConvFighter convFighter;
    private static TripodMek tripodMek;
    private static QuadVee quadVee;
    private static QuadMek quadMek;
    private static GunEmplacement gunEmplacement;
    private static LargeSupportTank largeSupportTank;
    private static FixedWingSupport fixedWingSupport;
    private static Dropship dropship;

    @BeforeAll
    static void setUp() {
        // Initialize any necessary data or configurations here
        warship = (Warship) getEntityForUnitTesting("Aegis Heavy Cruiser (2372)", true);
        assertNotNull(warship, "Aegis Heavy Cruiser (2372) not found");
        spaceStation = (SpaceStation) getEntityForUnitTesting("Crucible Station", true);
        assertNotNull(spaceStation, "Crucible Station not found");
        jumpship = (Jumpship) getEntityForUnitTesting("Explorer JumpShip", true);
        assertNotNull(jumpship, "Explorer JumpShip not found");
        superHeavyTank = (SuperHeavyTank) getEntityForUnitTesting("Devastator II Superheavy Tank ", true);
        assertNotNull(superHeavyTank, "Devastator II Superheavy Tank not found");
        supportTank = (SupportTank) getEntityForUnitTesting("Air Car", true);
        assertNotNull(supportTank, "Air Car not found");

        vtol = (VTOL) getEntityForUnitTesting("Cobra Transport VTOL", true);
        assertNotNull(vtol, "Cobra Transport VTOL not found");
        supportVtol = (SupportVTOL) getEntityForUnitTesting("SOAR VTOL", true);
        assertNotNull(supportVtol, "SOAR VTOL not found");
        tank = (Tank) getEntityForUnitTesting("Bulldog Medium Tank", true);
        assertNotNull(tank, "Bulldog Medium Tank not found");
        mek = (Mek) getEntityForUnitTesting("Sun Cobra", false);
        assertNotNull(mek, "Sun Cobra not found");
        protoMek = (ProtoMek) getEntityForUnitTesting("Centaur", true);
        assertNotNull(protoMek, "Centaur not found");

        infantry = (Infantry) getEntityForUnitTesting("Fast Recon Cavalry Point", true);
        assertNotNull(infantry, "Fast Recon Cavalry Point not found");
        battleArmor = (BattleArmor) getEntityForUnitTesting("Hantu AIX-210(Sqd5)", true);
        assertNotNull(battleArmor, "Hantu AIX-210(Sqd5) not found");
        aeroSpaceFighter = (AeroSpaceFighter) getEntityForUnitTesting("Cheetah F-11", true);
        assertNotNull(aeroSpaceFighter, "Cheetah F-11 not found");
        convFighter = (ConvFighter) getEntityForUnitTesting("Boeing Jump Bomber", true);
        assertNotNull(convFighter, "Boeing Jump Bomber not found");
        tripodMek = (TripodMek) getEntityForUnitTesting("Triskelion TRK-4V", false);
        assertNotNull(tripodMek, "Triskelion TRK-4V not found");

        quadVee = (QuadVee) getEntityForUnitTesting("Boreas C", false);
        assertNotNull(quadVee, "Boreas C not found");
        quadMek = (QuadMek) getEntityForUnitTesting("Great Turtle GTR-1", false);
        assertNotNull(quadMek, "Great Turtle GTR-1 not found");
        largeSupportTank = (LargeSupportTank) getEntityForUnitTesting("Dromedary Water Transport", true);
        assertNotNull(largeSupportTank, "Dromedary Water Transport not found");
        fixedWingSupport = (FixedWingSupport) getEntityForUnitTesting("Mosquito Light Fighter", true);
        assertNotNull(fixedWingSupport, "Mosquito Light Fighter not found");
        dropship = (Dropship) getEntityForUnitTesting("Princess Luxury Liner", true);
        assertNotNull(dropship, "Princess Luxury Liner not found");

        gunEmplacement = (GunEmplacement) getEntityForUnitTesting("Medium Blaze Turret 3025", true);
        assertNotNull(gunEmplacement, "Medium Blaze Turret 3025 not found");
        smallCraft = (SmallCraft) getEntityForUnitTesting("Mowang Courier (Clandestine)", true);
        assertNotNull(smallCraft, "Mowang Courier (Clandestine) not found");
        landAirMek = (LandAirMek) getEntityForUnitTesting("Shadow Hawk LAM SHD-X2", false);
        assertNotNull(landAirMek, "Shadow Hawk LAM SHD-X2 not found");
    }

    /**
     * Sanity test for healthy units. It evaluates 1.0f for all units.
     *
     * @param unit the unit to test
     */
    @ParameterizedTest
    @MethodSource(value = "getHealthyUnits")
    void testHealthyUnits(Entity unit) {
        assertUnitValues(unit);
    }

    static List<Entity> getHealthyUnits() {
        return List.of(
              warship,
              spaceStation,
              jumpship,
              superHeavyTank,
              supportTank,
              vtol,
              supportVtol,
              tank,
              mek,
              protoMek,
              infantry,
              battleArmor,
              aeroSpaceFighter,
              convFighter,
              tripodMek,
              quadVee,
              quadMek,
              largeSupportTank,
              fixedWingSupport,
              dropship,
              landAirMek,
              smallCraft,
              gunEmplacement
        );
    }

    private void assertUnitValues(Entity unit) {
        float[] values = {
              EntityFeatureUtils.getTargetFrontHealthStats(unit),
              EntityFeatureUtils.getTargetBackHealthStats(unit),
              EntityFeatureUtils.getTargetOverallHealthStats(unit),
              EntityFeatureUtils.getTargetLeftSideHealthStats(unit),
              EntityFeatureUtils.getTargetRightSideHealthStats(unit)
        };
        assertAll(() -> assertEquals(1.0f, values[0],
                    "Failed front health for unit " + unit.getClass().getSimpleName()),
              () -> assertEquals(1.0f, values[1],
                    "Failed back health for unit " + unit.getClass().getSimpleName()),
              () -> assertEquals(1.0f, values[2],
                    "Failed overall health for unit " + unit.getClass().getSimpleName()),
              () -> assertEquals(1.0f, values[3],
                    "Failed left health for unit " + unit.getClass().getSimpleName()),
              () -> assertEquals(1.0f, values[4],
                    "Failed right health for unit " + unit.getClass().getSimpleName()));
    }
}
