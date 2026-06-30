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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Optional;

import megamek.common.Report;
import megamek.common.compute.Compute;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.Mounted;
import megamek.common.equipment.WeaponType;
import megamek.common.game.Game;
import megamek.common.options.IOption;
import megamek.common.options.OptionsConstants;
import megamek.common.options.WeaponQuirks;
import megamek.common.rolls.Roll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

/**
 * Tests for the Directional Torso Mount weapon quirk (BMM p.83): the 2-point front/rear version and the 3-point
 * quad-only 360-degree version.
 */
class DirectionalTorsoMountTest {

    @BeforeAll
    static void beforeAll() {
        EquipmentType.initializeTypes();
    }

    private static WeaponType erLargeLaser() {
        return (WeaponType) EquipmentType.get("ISERLargeLaser");
    }

    private static WeaponType heavyGaussRifle() {
        return (WeaponType) EquipmentType.get("ISHeavyGaussRifle");
    }

    /**
     * Builds a Mek with a single torso-mounted ER Large Laser carrying the given Directional Torso Mount quirk,
     * attached to a game that has the StratOps quirks option enabled.
     */
    private static Mounted<?> mountWithDirectionalQuirk(Mek mek, String quirkName) {
        Game game = new Game();
        game.getOptions().getOption(OptionsConstants.ADVANCED_STRATOPS_QUIRKS).setValue(true);
        mek.setGame(game);
        try {
            Mounted<?> weapon = mek.addEquipment(erLargeLaser(), Mek.LOC_RIGHT_TORSO);
            weapon.getQuirks().getOption(quirkName).setValue(true);
            return weapon;
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }
    }

    @Nested
    @DisplayName("Firing-arc behavior")
    class ArcTests {

        @Test
        @DisplayName("2-point mount defaults to the front arc")
        void twoPointDefaultsToFrontArc() {
            BipedMek mek = new BipedMek();
            Mounted<?> weapon = mountWithDirectionalQuirk(mek,
                  OptionsConstants.QUIRK_WEAPON_POS_DIRECT_TORSO_MOUNT);

            assertTrue(weapon.hasDirectionalTorsoMount());
            assertFalse(weapon.isDirectionalMountRear());
            assertEquals(Compute.ARC_FORWARD, mek.getWeaponArc(mek.getEquipmentNum(weapon)));
        }

        @Test
        @DisplayName("2-point mount fires into the rear arc when flipped")
        void twoPointFiresRearWhenFlipped() {
            BipedMek mek = new BipedMek();
            Mounted<?> weapon = mountWithDirectionalQuirk(mek,
                  OptionsConstants.QUIRK_WEAPON_POS_DIRECT_TORSO_MOUNT);

            weapon.setDirectionalMountRear(true);

            assertTrue(weapon.isDirectionalMountRear());
            assertEquals(Compute.ARC_REAR, mek.getWeaponArc(mek.getEquipmentNum(weapon)));
        }

        @Test
        @DisplayName("3-point quad mount always fires into a full 360-degree arc")
        void threePointQuadIs360() {
            QuadMek mek = new QuadMek();
            Mounted<?> weapon = mountWithDirectionalQuirk(mek,
                  OptionsConstants.QUIRK_WEAPON_POS_DIRECT_TORSO_MOUNT_QUAD);

            assertTrue(weapon.hasDirectional360TorsoMount());
            assertEquals(Compute.ARC_360, mek.getWeaponArc(mek.getEquipmentNum(weapon)));

            // The front/rear flag is irrelevant for the 360-degree version.
            weapon.setDirectionalMountRear(true);
            assertEquals(Compute.ARC_360, mek.getWeaponArc(mek.getEquipmentNum(weapon)));
        }

        @Test
        @DisplayName("Without the quirk a torso weapon keeps its normal forward arc")
        void noQuirkKeepsForwardArc() {
            BipedMek mek = new BipedMek();
            Game game = new Game();
            game.getOptions().getOption(OptionsConstants.ADVANCED_STRATOPS_QUIRKS).setValue(true);
            mek.setGame(game);
            Mounted<?> weapon;
            try {
                weapon = mek.addEquipment(erLargeLaser(), Mek.LOC_RIGHT_TORSO);
            } catch (Exception exception) {
                throw new RuntimeException(exception);
            }

            assertFalse(weapon.hasDirectionalTorsoMount());
            assertEquals(Compute.ARC_FORWARD, mek.getWeaponArc(mek.getEquipmentNum(weapon)));
        }
    }

    @Nested
    @DisplayName("Persistence and locking")
    class StateTests {

        @Test
        @DisplayName("Chosen arc persists across a new round (does not reset like a torso twist)")
        void arcPersistsAcrossNewRound() {
            BipedMek mek = new BipedMek();
            Mounted<?> weapon = mountWithDirectionalQuirk(mek,
                  OptionsConstants.QUIRK_WEAPON_POS_DIRECT_TORSO_MOUNT);
            weapon.setDirectionalMountRear(true);

            mek.newRound(2);

            assertTrue(weapon.isDirectionalMountRear(),
                  "The mount arc must survive the End Phase / new round reset");
            assertEquals(Compute.ARC_REAR, mek.getWeaponArc(mek.getEquipmentNum(weapon)));
        }

        @Test
        @DisplayName("A locked mount can no longer change arc but the weapon is not destroyed")
        void lockedMountCannotChangeArcButStillFires() {
            BipedMek mek = new BipedMek();
            Mounted<?> weapon = mountWithDirectionalQuirk(mek,
                  OptionsConstants.QUIRK_WEAPON_POS_DIRECT_TORSO_MOUNT);

            // Lock it while pointing forward, then try to flip to rear.
            weapon.setDirectionalMountLocked(true);
            weapon.setDirectionalMountRear(true);

            assertTrue(weapon.isDirectionalMountLocked());
            assertFalse(weapon.isDirectionalMountRear(), "A locked mount must ignore arc changes");
            assertFalse(weapon.isDestroyed(), "Locking the mount must not destroy the weapon");
            assertEquals(Compute.ARC_FORWARD, mek.getWeaponArc(mek.getEquipmentNum(weapon)));
        }

        @Test
        @DisplayName("Weapon description shows a rear indicator when the mount is flipped, and locked")
        void descriptionShowsFlipAndLockIndicators() {
            BipedMek mek = new BipedMek();
            Mounted<?> weapon = mountWithDirectionalQuirk(mek,
                  OptionsConstants.QUIRK_WEAPON_POS_DIRECT_TORSO_MOUNT);

            assertFalse(weapon.getDesc().contains("(R)"), "Front-facing mount shows no rear indicator");

            weapon.setDirectionalMountRear(true);
            assertTrue(weapon.getDesc().contains("(R)"), "Rear-facing mount shows the (R) indicator");

            weapon.setDirectionalMountLocked(true);
            assertTrue(weapon.getDesc().contains("(Locked)"), "A locked mount shows the (Locked) indicator");
        }

        @Test
        @DisplayName("Mount state survives a serialization round-trip (lobby/save transmission)")
        void mountStateSurvivesSerialization() throws Exception {
            BipedMek mek = new BipedMek();
            Mounted<?> weapon = mountWithDirectionalQuirk(mek,
                  OptionsConstants.QUIRK_WEAPON_POS_DIRECT_TORSO_MOUNT);
            weapon.setDirectionalMountRear(true);
            weapon.setDirectionalMountLocked(true);
            int weaponNumber = mek.getEquipmentNum(weapon);

            ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
            try (ObjectOutputStream objectOut = new ObjectOutputStream(byteOut)) {
                objectOut.writeObject(mek);
            }
            Mek deserializedMek;
            try (ObjectInputStream objectIn = new ObjectInputStream(
                  new ByteArrayInputStream(byteOut.toByteArray()))) {
                deserializedMek = (Mek) objectIn.readObject();
            }

            Mounted<?> deserializedWeapon = deserializedMek.getEquipment(weaponNumber);
            assertTrue(deserializedWeapon.isDirectionalMountRear(), "Rear arc flag must survive transmission");
            assertTrue(deserializedWeapon.isDirectionalMountLocked(), "Locked flag must survive transmission");
            // The quirk option value itself is part of the transmitted state (game gate aside).
            assertTrue(deserializedWeapon.getQuirks()
                  .booleanOption(OptionsConstants.QUIRK_WEAPON_POS_DIRECT_TORSO_MOUNT));
        }
    }

    /**
     * Builds a Mek carrying the chassis-wide (unit-level) directional_torso_mount quirk and a single weapon of the
     * given type in the given location, attached to a game with quirks enabled. The weapon itself carries no
     * weapon-level quirk.
     */
    private static Mounted<?> unitLevelMek(Mek mek, WeaponType weaponType, int location) {
        Game game = new Game();
        game.getOptions().getOption(OptionsConstants.ADVANCED_STRATOPS_QUIRKS).setValue(true);
        mek.setGame(game);
        mek.getQuirks().getOption(OptionsConstants.QUIRK_POS_DIRECTIONAL_TORSO_MOUNT).setValue(true);
        try {
            return mek.addEquipment(weaponType, location);
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }
    }

    @Nested
    @DisplayName("Unit-level (chassis-wide) directional_torso_mount quirk")
    class UnitLevelQuirkTests {

        @Test
        @DisplayName("Covers an eligible torso weapon that has no weapon-level quirk")
        void unitLevelCoversTorsoWeapon() {
            BipedMek mek = new BipedMek();
            Mounted<?> weapon = unitLevelMek(mek, erLargeLaser(), Mek.LOC_RIGHT_TORSO);

            assertTrue(weapon.hasDirectionalTorsoMount(),
                  "A torso weapon on a unit with the chassis-wide quirk is a directional mount");
            assertEquals(Compute.ARC_FORWARD, mek.getWeaponArc(mek.getEquipmentNum(weapon)));
            weapon.setDirectionalMountRear(true);
            assertEquals(Compute.ARC_REAR, mek.getWeaponArc(mek.getEquipmentNum(weapon)));
        }

        @Test
        @DisplayName("Does not cover an arm weapon (torso feature only)")
        void unitLevelDoesNotCoverArmWeapon() {
            BipedMek mek = new BipedMek();
            Mounted<?> weapon = unitLevelMek(mek, erLargeLaser(), Mek.LOC_RIGHT_ARM);

            assertFalse(weapon.hasDirectionalTorsoMount(), "Arm weapons are outside the torso mount");
            assertEquals(Compute.ARC_RIGHT_ARM, mek.getWeaponArc(mek.getEquipmentNum(weapon)));
        }

        @Test
        @DisplayName("Does not cover a turret-mounted torso weapon")
        void unitLevelDoesNotCoverTurretWeapon() {
            BipedMek mek = new BipedMek();
            Mounted<?> weapon = unitLevelMek(mek, erLargeLaser(), Mek.LOC_RIGHT_TORSO);
            weapon.setMekTurretMounted(true);

            assertFalse(weapon.hasDirectionalTorsoMount(),
                  "A turret-mounted weapon already has a turret arc and is not a directional mount");
        }

        @Test
        @DisplayName("Does not cover a location-restricted weapon (Heavy Gauss)")
        void unitLevelDoesNotCoverHeavyGauss() {
            BipedMek mek = new BipedMek();
            Mounted<?> weapon = unitLevelMek(mek, heavyGaussRifle(), Mek.LOC_RIGHT_TORSO);

            assertFalse(weapon.hasDirectionalTorsoMount(),
                  "Heavy Gauss may not be placed in a Directional Torso Mount");
        }

        @Test
        @DisplayName("Unit-level quirk is the 2-point version even on a quad (front/rear, not 360)")
        void unitLevelIsTwoPointOnQuad() {
            QuadMek mek = new QuadMek();
            Mounted<?> weapon = unitLevelMek(mek, erLargeLaser(), Mek.LOC_RIGHT_TORSO);

            assertFalse(weapon.hasDirectional360TorsoMount());
            assertEquals(Compute.ARC_FORWARD, mek.getWeaponArc(mek.getEquipmentNum(weapon)));
            weapon.setDirectionalMountRear(true);
            assertEquals(Compute.ARC_REAR, mek.getWeaponArc(mek.getEquipmentNum(weapon)));
        }
    }

    @Nested
    @DisplayName("Destruction roll on a location hit (BMM p.83)")
    class DestructionRollTests {

        private static Roll mockRollOf(int value) {
            Roll roll = mock(Roll.class);
            when(roll.getIntValue()).thenReturn(value);
            return roll;
        }

        @Test
        @DisplayName("A 9+ roll locks the mount but leaves the weapon able to fire")
        void rollOfNineLocksMount() {
            BipedMek mek = new BipedMek();
            Mounted<?> weapon = mountWithDirectionalQuirk(mek,
                  OptionsConstants.QUIRK_WEAPON_POS_DIRECT_TORSO_MOUNT);

            Roll roll = mockRollOf(9);
            try (MockedStatic<Compute> mockedCompute = mockStatic(Compute.class)) {
                mockedCompute.when(() -> Compute.rollD6(2)).thenReturn(roll);
                Optional<Report> report = DirectionalTorsoMountRules.rollLockFromLocationDamage(
                      mek, Mek.LOC_RIGHT_TORSO);

                assertTrue(report.isPresent(), "A 9+ result should produce a lock report");
            }

            assertTrue(weapon.isDirectionalMountLocked(), "A 9+ result must lock the mount");
            assertFalse(weapon.isDestroyed(), "Locking the mount must not destroy the weapon");
        }

        @Test
        @DisplayName("An 8 roll leaves the mount rotatable")
        void rollOfEightDoesNotLock() {
            BipedMek mek = new BipedMek();
            Mounted<?> weapon = mountWithDirectionalQuirk(mek,
                  OptionsConstants.QUIRK_WEAPON_POS_DIRECT_TORSO_MOUNT);

            Roll roll = mockRollOf(8);
            try (MockedStatic<Compute> mockedCompute = mockStatic(Compute.class)) {
                mockedCompute.when(() -> Compute.rollD6(2)).thenReturn(roll);
                Optional<Report> report = DirectionalTorsoMountRules.rollLockFromLocationDamage(
                      mek, Mek.LOC_RIGHT_TORSO);

                assertFalse(report.isPresent(), "An 8 result should not lock the mount");
            }

            assertFalse(weapon.isDirectionalMountLocked());
        }

        @Test
        @DisplayName("A hit to a location with no directional mount does nothing (no roll)")
        void noMountInLocationDoesNothing() {
            BipedMek mek = new BipedMek();
            Mounted<?> weapon = mountWithDirectionalQuirk(mek,
                  OptionsConstants.QUIRK_WEAPON_POS_DIRECT_TORSO_MOUNT);

            // Hit a different location than the one holding the mount.
            Optional<Report> report = DirectionalTorsoMountRules.rollLockFromLocationDamage(
                  mek, Mek.LOC_LEFT_TORSO);

            assertFalse(report.isPresent());
            assertFalse(weapon.isDirectionalMountLocked());
        }

        @Test
        @DisplayName("An already-locked mount is not rolled again")
        void alreadyLockedMountIsNotRerolled() {
            BipedMek mek = new BipedMek();
            Mounted<?> weapon = mountWithDirectionalQuirk(mek,
                  OptionsConstants.QUIRK_WEAPON_POS_DIRECT_TORSO_MOUNT);
            weapon.setDirectionalMountLocked(true);

            // No Compute mock: if a roll were attempted the real RNG would run, but the method must
            // short-circuit to empty because the only mount in the location is already locked.
            Optional<Report> report = DirectionalTorsoMountRules.rollLockFromLocationDamage(
                  mek, Mek.LOC_RIGHT_TORSO);

            assertFalse(report.isPresent());
        }
    }

    @Nested
    @DisplayName("Facing-change action application (BMM p.83)")
    class FacingActionTests {

        @Test
        @DisplayName("Applying a facing change sets the mount to the requested arc")
        void applyMountFacingSetsArc() {
            BipedMek mek = new BipedMek();
            Mounted<?> weapon = mountWithDirectionalQuirk(mek,
                  OptionsConstants.QUIRK_WEAPON_POS_DIRECT_TORSO_MOUNT);

            DirectionalTorsoMountRules.applyMountFacing(mek, mek.getEquipmentNum(weapon), true);
            assertTrue(weapon.isDirectionalMountRear());

            DirectionalTorsoMountRules.applyMountFacing(mek, mek.getEquipmentNum(weapon), false);
            assertFalse(weapon.isDirectionalMountRear());
        }

        @Test
        @DisplayName("A locked mount ignores a facing-change action")
        void applyMountFacingIgnoredWhenLocked() {
            BipedMek mek = new BipedMek();
            Mounted<?> weapon = mountWithDirectionalQuirk(mek,
                  OptionsConstants.QUIRK_WEAPON_POS_DIRECT_TORSO_MOUNT);
            weapon.setDirectionalMountLocked(true);

            DirectionalTorsoMountRules.applyMountFacing(mek, mek.getEquipmentNum(weapon), true);
            assertFalse(weapon.isDirectionalMountRear(), "A locked mount must not change arc");
        }

        @Test
        @DisplayName("A non-directional weapon ignores a facing-change action")
        void applyMountFacingIgnoredForNonDirectionalWeapon() {
            BipedMek mek = new BipedMek();
            Game game = new Game();
            game.getOptions().getOption(OptionsConstants.ADVANCED_STRATOPS_QUIRKS).setValue(true);
            mek.setGame(game);
            Mounted<?> weapon;
            try {
                weapon = mek.addEquipment(erLargeLaser(), Mek.LOC_RIGHT_TORSO);
            } catch (Exception exception) {
                throw new RuntimeException(exception);
            }

            DirectionalTorsoMountRules.applyMountFacing(mek, mek.getEquipmentNum(weapon), true);
            assertFalse(weapon.isDirectionalMountRear());
        }
    }

    @Nested
    @DisplayName("Quirk eligibility (customizer validation)")
    class EligibilityTests {

        private static IOption quirkOption(String name) {
            return new WeaponQuirks().getOption(name);
        }

        @Test
        @DisplayName("2-point mount is allowed on a normal biped torso weapon")
        void twoPointAllowedOnBipedWeapon() {
            assertFalse(WeaponQuirks.isQuirkDisallowed(
                  quirkOption(OptionsConstants.QUIRK_WEAPON_POS_DIRECT_TORSO_MOUNT),
                  new BipedMek(), erLargeLaser()));
        }

        @Test
        @DisplayName("Location-restricted weapons (Heavy Gauss) may not take a Directional Torso Mount")
        void heavyGaussDisallowed() {
            assertTrue(WeaponQuirks.isQuirkDisallowed(
                  quirkOption(OptionsConstants.QUIRK_WEAPON_POS_DIRECT_TORSO_MOUNT),
                  new BipedMek(), heavyGaussRifle()));
        }

        @Test
        @DisplayName("3-point 360 mount is allowed only on quad Meks")
        void threePointQuadOnly() {
            assertTrue(WeaponQuirks.isQuirkDisallowed(
                  quirkOption(OptionsConstants.QUIRK_WEAPON_POS_DIRECT_TORSO_MOUNT_QUAD),
                  new BipedMek(), erLargeLaser()), "The 360 version must be disallowed on bipeds");
            assertFalse(WeaponQuirks.isQuirkDisallowed(
                  quirkOption(OptionsConstants.QUIRK_WEAPON_POS_DIRECT_TORSO_MOUNT_QUAD),
                  new QuadMek(), erLargeLaser()), "The 360 version must be allowed on quads");
        }

        @Test
        @DisplayName("Directional Torso Mount remains disallowed on non-Mek unit types")
        void disallowedOnTank() {
            assertTrue(WeaponQuirks.isQuirkDisallowed(
                  quirkOption(OptionsConstants.QUIRK_WEAPON_POS_DIRECT_TORSO_MOUNT),
                  new Tank(), erLargeLaser()));
        }
    }
}
