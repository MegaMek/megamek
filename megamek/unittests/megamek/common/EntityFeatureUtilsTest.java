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

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import megamek.ai.utility.EntityFeatureUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Test class for EntityFeatureUtils.
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
        warship = (Warship) mekSummaryCache.getMek("Aegis Heavy Cruiser (2372)").loadEntity();
        spaceStation = (SpaceStation) mekSummaryCache.getMek("Crucible Station").loadEntity();
        jumpship = (Jumpship) mekSummaryCache.getMek("Explorer JumpShip").loadEntity();
        superHeavyTank = (SuperHeavyTank) mekSummaryCache.getMek("Devastator II Superheavy Tank").loadEntity();
        supportTank = (SupportTank) mekSummaryCache.getMek("Air Car").loadEntity();

        vtol = (VTOL) mekSummaryCache.getMek("Cobra Transport VTOL").loadEntity();
        supportVtol = (SupportVTOL) mekSummaryCache.getMek("SOAR VTOL").loadEntity();
        tank = (Tank) mekSummaryCache.getMek("Bulldog Medium Tank").loadEntity();
        mek = (Mek) mekSummaryCache.getMek("Sun Cobra").loadEntity();
        protoMek = (ProtoMek) mekSummaryCache.getMek("Centaur").loadEntity();

        infantry = (Infantry) mekSummaryCache.getMek("Fast Recon Cavalry Point").loadEntity();
        battleArmor = (BattleArmor) mekSummaryCache.getMek("Hantu AIX-210(Sqd5)").loadEntity();
        aeroSpaceFighter = (AeroSpaceFighter) mekSummaryCache.getMek("Cheetah F-11").loadEntity();
        convFighter = (ConvFighter) mekSummaryCache.getMek("Boeing Jump Bomber").loadEntity();
        tripodMek = (TripodMek) mekSummaryCache.getMek("Triskelion TRK-4V").loadEntity();

        quadVee = (QuadVee) mekSummaryCache.getMek("Boreas C").loadEntity();
        quadMek = (QuadMek) mekSummaryCache.getMek("Great Turtle GTR-1").loadEntity();
        largeSupportTank = (LargeSupportTank) mekSummaryCache.getMek("Dromedary Water Transport").loadEntity();
        fixedWingSupport = (FixedWingSupport) mekSummaryCache.getMek("Mosquito Light Fighter").loadEntity();
        dropship = (Dropship) mekSummaryCache.getMek("Princess Luxury Liner").loadEntity();

        gunEmplacement = (GunEmplacement) mekSummaryCache.getMek("Medium Blaze Turret (3025)").loadEntity();
        smallCraft = (SmallCraft) mekSummaryCache.getMek("Mowang Courier (Clandestine)").loadEntity();
        landAirMek = (LandAirMek) mekSummaryCache.getMek("Shadow Hawk LAM SHD-X2").loadEntity();
    }

    /**
     * Sanity test for healthy units. It evaluates 1.0f for all units.
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
