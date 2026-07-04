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
import java.util.List;
import java.util.Optional;

import megamek.common.QuirkEntry;
import megamek.common.Report;
import megamek.common.compute.Compute;
import megamek.common.enums.GamePhase;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.Mounted;
import megamek.common.equipment.WeaponType;
import megamek.common.game.Game;
import megamek.common.options.IOption;
import megamek.common.options.OptionsConstants;
import megamek.common.options.Quirks;
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

    private static WeaponType hyperAssaultGaussRifle30() {
        return (WeaponType) EquipmentType.get("CLHAG30");
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
        @DisplayName("2-point mount uses a forward arc with a rear facing offset when flipped")
        void twoPointFiresRearWhenFlipped() {
            BipedMek mek = new BipedMek();
            Mounted<?> weapon = mountWithDirectionalQuirk(mek,
                  OptionsConstants.QUIRK_WEAPON_POS_DIRECT_TORSO_MOUNT);

            weapon.setDirectionalMountRear(true);

            assertTrue(weapon.isDirectionalMountRear());
            assertEquals(3, weapon.getDirectionalMountFacing(), "Rear is facing offset 3");
            // The mount aims like a turret: the arc shape is always forward; the rear-ness is the offset.
            assertEquals(Compute.ARC_FORWARD, mek.getWeaponArc(mek.getEquipmentNum(weapon)));
        }

        @Test
        @DisplayName("3-point quad mount is a turret that can rotate to any of the six facings")
        void threePointQuadRotates() {
            QuadMek mek = new QuadMek();
            Mounted<?> weapon = mountWithDirectionalQuirk(mek,
                  OptionsConstants.QUIRK_WEAPON_POS_DIRECT_TORSO_MOUNT_QUAD);

            assertTrue(weapon.hasDirectional360TorsoMount());
            // A turret always returns a forward arc; the direction is the rotatable facing offset (0-5).
            assertEquals(Compute.ARC_FORWARD, mek.getWeaponArc(mek.getEquipmentNum(weapon)));

            weapon.setDirectionalMountFacing(2);
            assertEquals(2, weapon.getDirectionalMountFacing(), "The 360 turret can rotate to any facing");
            assertEquals(Compute.ARC_FORWARD, mek.getWeaponArc(mek.getEquipmentNum(weapon)));
        }

        @Test
        @DisplayName("The 3-point 360 quad quirk is inert on a biped (quad-only)")
        void threePointIsQuadOnlyInEngine() {
            BipedMek mek = new BipedMek();
            Mounted<?> weapon = mountWithDirectionalQuirk(mek,
                  OptionsConstants.QUIRK_WEAPON_POS_DIRECT_TORSO_MOUNT_QUAD);

            assertFalse(weapon.hasDirectional360TorsoMount(),
                  "A biped must never get the 360-degree turret, even with the quad quirk set");
            // The quad-only quirk simply does not apply to a biped: the weapon is not a directional mount
            // at all and keeps its normal forward arc, unaffected by the rear flag.
            assertFalse(weapon.hasDirectionalTorsoMount());
            assertEquals(Compute.ARC_FORWARD, mek.getWeaponArc(mek.getEquipmentNum(weapon)));
            weapon.setDirectionalMountRear(true);
            assertEquals(Compute.ARC_FORWARD, mek.getWeaponArc(mek.getEquipmentNum(weapon)));
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
                  "The mount facing must survive the End Phase / new round reset");
            assertEquals(3, weapon.getDirectionalMountFacing());
            assertEquals(Compute.ARC_FORWARD, mek.getWeaponArc(mek.getEquipmentNum(weapon)));
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
        @DisplayName("Chassis torso-set quirk value survives a serialization round-trip")
        void chassisTorsoSetSurvivesSerialization() throws Exception {
            QuadMek mek = new QuadMek();
            unitLevelMek(mek, erLargeLaser(), Mek.LOC_RIGHT_TORSO,
                  OptionsConstants.QUIRK_POS_DIRECTIONAL_TORSO_MOUNT_360, "LT RT");

            ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
            try (ObjectOutputStream objectOut = new ObjectOutputStream(byteOut)) {
                objectOut.writeObject(mek);
            }
            Mek deserializedMek;
            try (ObjectInputStream objectIn = new ObjectInputStream(
                  new ByteArrayInputStream(byteOut.toByteArray()))) {
                deserializedMek = (Mek) objectIn.readObject();
            }

            assertEquals("LT RT", deserializedMek.getQuirks()
                        .getOption(OptionsConstants.QUIRK_POS_DIRECTIONAL_TORSO_MOUNT_360).stringValue(),
                  "The chassis torso-set value must survive transmission");
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
        return unitLevelMek(mek, weaponType, location,
              OptionsConstants.QUIRK_POS_DIRECTIONAL_TORSO_MOUNT, "H LT RT CT");
    }

    private static Mounted<?> unitLevelMek(Mek mek, WeaponType weaponType, int location,
          String chassisQuirk, String torsoValue) {
        Game game = new Game();
        game.getOptions().getOption(OptionsConstants.ADVANCED_STRATOPS_QUIRKS).setValue(true);
        mek.setGame(game);
        mek.getQuirks().getOption(chassisQuirk).setValue(torsoValue);
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
            assertEquals(3, weapon.getDirectionalMountFacing());
            assertEquals(Compute.ARC_FORWARD, mek.getWeaponArc(mek.getEquipmentNum(weapon)));
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
        @DisplayName("Covers an unsplit torso HAG/30 (the canon OmniMarauder Prime dorsal mount)")
        void unitLevelCoversUnsplitHyperAssaultGauss() {
            BipedMek mek = new BipedMek();
            Mounted<?> weapon = unitLevelMek(mek, hyperAssaultGaussRifle30(), Mek.LOC_RIGHT_TORSO);

            assertTrue(weapon.hasDirectionalTorsoMount(),
                  "A HAG/30 mounted whole in one torso has no location placement restriction");
        }

        @Test
        @DisplayName("Never covers a weapon that is physically split across two locations")
        void doesNotCoverSplitWeapon() {
            BipedMek mek = new BipedMek();
            Mounted<?> weapon = unitLevelMek(mek, hyperAssaultGaussRifle30(), Mek.LOC_RIGHT_TORSO);
            weapon.setSplit(true);

            assertFalse(weapon.hasDirectionalTorsoMount(),
                  "A split weapon cannot ride a single-location Directional Torso Mount");
        }

        @Test
        @DisplayName("A split weapon is not a directional mount even with the weapon-level quirk")
        void splitWeaponIgnoresWeaponLevelQuirk() {
            BipedMek mek = new BipedMek();
            Mounted<?> weapon = mountWithDirectionalQuirk(mek,
                  OptionsConstants.QUIRK_WEAPON_POS_DIRECT_TORSO_MOUNT);
            weapon.setSplit(true);

            assertFalse(weapon.hasDirectionalTorsoMount(),
                  "The split guard applies regardless of the quirk source");
        }

        @Test
        @DisplayName("Unit-level 2-pt quirk is front/rear even on a quad (not 360)")
        void unitLevelIsTwoPointOnQuad() {
            QuadMek mek = new QuadMek();
            Mounted<?> weapon = unitLevelMek(mek, erLargeLaser(), Mek.LOC_RIGHT_TORSO);

            assertFalse(weapon.hasDirectional360TorsoMount());
            assertEquals(Compute.ARC_FORWARD, mek.getWeaponArc(mek.getEquipmentNum(weapon)));
            weapon.setDirectionalMountRear(true);
            assertEquals(3, weapon.getDirectionalMountFacing());
            assertEquals(Compute.ARC_FORWARD, mek.getWeaponArc(mek.getEquipmentNum(weapon)));
        }

        @Test
        @DisplayName("The chassis quirk only covers the torsos listed in its value")
        void unitLevelOnlyCoversListedTorsos() {
            BipedMek inSet = new BipedMek();
            Mounted<?> leftWeapon = unitLevelMek(inSet, erLargeLaser(), Mek.LOC_LEFT_TORSO,
                  OptionsConstants.QUIRK_POS_DIRECTIONAL_TORSO_MOUNT, "LT");
            assertTrue(leftWeapon.hasDirectionalTorsoMount(), "LT is in the set, so its weapon is covered");

            BipedMek outOfSet = new BipedMek();
            Mounted<?> rightWeapon = unitLevelMek(outOfSet, erLargeLaser(), Mek.LOC_RIGHT_TORSO,
                  OptionsConstants.QUIRK_POS_DIRECTIONAL_TORSO_MOUNT, "LT");
            assertFalse(rightWeapon.hasDirectionalTorsoMount(), "RT is not in the set, so its weapon is not covered");
        }

        @Test
        @DisplayName("The 360 chassis quirk makes a quad's listed-torso weapons a rotatable turret")
        void unitLevel360OnQuad() {
            QuadMek mek = new QuadMek();
            Mounted<?> weapon = unitLevelMek(mek, erLargeLaser(), Mek.LOC_RIGHT_TORSO,
                  OptionsConstants.QUIRK_POS_DIRECTIONAL_TORSO_MOUNT_360, "LT RT");

            assertTrue(weapon.hasDirectional360TorsoMount());
            assertEquals(Compute.ARC_FORWARD, mek.getWeaponArc(mek.getEquipmentNum(weapon)));
            weapon.setDirectionalMountFacing(4);
            assertEquals(4, weapon.getDirectionalMountFacing(), "The 360 turret can rotate to any facing");
        }

        @Test
        @DisplayName("The 360 chassis quirk has no effect on a biped (quad-only)")
        void unitLevel360IgnoredOnBiped() {
            BipedMek mek = new BipedMek();
            Mounted<?> weapon = unitLevelMek(mek, erLargeLaser(), Mek.LOC_RIGHT_TORSO,
                  OptionsConstants.QUIRK_POS_DIRECTIONAL_TORSO_MOUNT_360, "LT RT");

            assertFalse(weapon.hasDirectional360TorsoMount(), "A biped never gets the 360 turret");
            assertFalse(weapon.hasDirectionalTorsoMount(),
                  "The 360 chassis quirk alone does not make a biped weapon directional");
            assertEquals(Compute.ARC_FORWARD, mek.getWeaponArc(mek.getEquipmentNum(weapon)));
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

                // The check is always reported (so the player sees the roll), but an 8 does not lock the mount.
                assertTrue(report.isPresent(), "The surviving check should still be reported");
            }

            assertFalse(weapon.isDirectionalMountLocked(), "An 8 result should not lock the mount");
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
        @DisplayName("Applying a facing change sets the mount to the requested facing")
        void applyMountFacingSetsArc() {
            BipedMek mek = new BipedMek();
            Mounted<?> weapon = mountWithDirectionalQuirk(mek,
                  OptionsConstants.QUIRK_WEAPON_POS_DIRECT_TORSO_MOUNT);

            DirectionalTorsoMountRules.applyMountFacing(mek, mek.getEquipmentNum(weapon), 3);
            assertEquals(3, weapon.getDirectionalMountFacing());

            DirectionalTorsoMountRules.applyMountFacing(mek, mek.getEquipmentNum(weapon), 0);
            assertEquals(0, weapon.getDirectionalMountFacing());
        }

        @Test
        @DisplayName("A facing change rotates every weapon in the mount's location together")
        void applyMountFacingRotatesWholeMount() {
            BipedMek mek = new BipedMek();
            Mounted<?> firstWeapon = mountWithDirectionalQuirk(mek,
                  OptionsConstants.QUIRK_WEAPON_POS_DIRECT_TORSO_MOUNT);
            Mounted<?> secondWeapon;
            try {
                secondWeapon = mek.addEquipment(erLargeLaser(), Mek.LOC_RIGHT_TORSO);
                secondWeapon.getQuirks().getOption(OptionsConstants.QUIRK_WEAPON_POS_DIRECT_TORSO_MOUNT)
                      .setValue(true);
            } catch (Exception exception) {
                throw new RuntimeException(exception);
            }

            // Setting the facing via one weapon must flip the whole mount (both weapons in the location).
            DirectionalTorsoMountRules.applyMountFacing(mek, mek.getEquipmentNum(firstWeapon), 3);
            assertEquals(3, firstWeapon.getDirectionalMountFacing());
            assertEquals(3, secondWeapon.getDirectionalMountFacing(),
                  "Both weapons in the mount share one facing and rotate together");
        }

        @Test
        @DisplayName("A 2-point mount rejects a non front/rear facing; a 360 turret accepts any")
        void applyMountFacingValidatesByType() {
            BipedMek bipedMek = new BipedMek();
            Mounted<?> twoPoint = mountWithDirectionalQuirk(bipedMek,
                  OptionsConstants.QUIRK_WEAPON_POS_DIRECT_TORSO_MOUNT);
            DirectionalTorsoMountRules.applyMountFacing(bipedMek, bipedMek.getEquipmentNum(twoPoint), 2);
            assertEquals(0, twoPoint.getDirectionalMountFacing(), "2-point mount rejects facing 2 (front/rear only)");

            QuadMek quadMek = new QuadMek();
            Mounted<?> turret = mountWithDirectionalQuirk(quadMek,
                  OptionsConstants.QUIRK_WEAPON_POS_DIRECT_TORSO_MOUNT_QUAD);
            DirectionalTorsoMountRules.applyMountFacing(quadMek, quadMek.getEquipmentNum(turret), 2);
            assertEquals(2, turret.getDirectionalMountFacing(), "360 turret accepts any facing");
        }

        @Test
        @DisplayName("A locked mount ignores a facing-change action")
        void applyMountFacingIgnoredWhenLocked() {
            BipedMek mek = new BipedMek();
            Mounted<?> weapon = mountWithDirectionalQuirk(mek,
                  OptionsConstants.QUIRK_WEAPON_POS_DIRECT_TORSO_MOUNT);
            weapon.setDirectionalMountLocked(true);

            DirectionalTorsoMountRules.applyMountFacing(mek, mek.getEquipmentNum(weapon), 3);
            assertEquals(0, weapon.getDirectionalMountFacing(), "A locked mount must not change facing");
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

            DirectionalTorsoMountRules.applyMountFacing(mek, mek.getEquipmentNum(weapon), 3);
            assertEquals(0, weapon.getDirectionalMountFacing());
        }
    }

    @Nested
    @DisplayName("Once-per-turn facing change (modeled on torso twists)")
    class OncePerTurnTests {

        @Test
        @DisplayName("A mount flipped in the targeting phase cannot flip again in the firing phase")
        void flipInEarlierPhaseLocksLaterPhases() {
            BipedMek mek = new BipedMek();
            Mounted<?> weapon = mountWithDirectionalQuirk(mek,
                  OptionsConstants.QUIRK_WEAPON_POS_DIRECT_TORSO_MOUNT);
            mek.getGame().setPhase(GamePhase.TARGETING);

            weapon.setDirectionalMountRear(true);
            assertEquals(3, weapon.getDirectionalMountFacing());
            assertFalse(weapon.isDirectionalMountAlreadyFlipped(),
                  "The facing stays adjustable within the phase it was changed in");

            mek.getGame().setPhase(GamePhase.FIRING);
            assertTrue(weapon.isDirectionalMountAlreadyFlipped(),
                  "A mount refaced in an earlier phase is locked for the rest of the turn");
            weapon.setDirectionalMountRear(false);
            assertEquals(3, weapon.getDirectionalMountFacing(),
                  "The facing must not change again in a later phase of the same turn");
        }

        @Test
        @DisplayName("Within the same phase the facing stays freely adjustable")
        void sameFlipPhaseAllowsAdjustment() {
            BipedMek mek = new BipedMek();
            Mounted<?> weapon = mountWithDirectionalQuirk(mek,
                  OptionsConstants.QUIRK_WEAPON_POS_DIRECT_TORSO_MOUNT);
            mek.getGame().setPhase(GamePhase.FIRING);

            weapon.setDirectionalMountRear(true);
            weapon.setDirectionalMountRear(false);
            assertEquals(0, weapon.getDirectionalMountFacing(),
                  "Re-adjusting within the same phase (before committing) must remain possible");
        }

        @Test
        @DisplayName("The once-per-turn tracker resets at the start of a new round; the facing persists")
        void newRoundResetsOncePerTurnTracker() {
            BipedMek mek = new BipedMek();
            Mounted<?> weapon = mountWithDirectionalQuirk(mek,
                  OptionsConstants.QUIRK_WEAPON_POS_DIRECT_TORSO_MOUNT);
            mek.getGame().setPhase(GamePhase.TARGETING);
            weapon.setDirectionalMountRear(true);
            mek.getGame().setPhase(GamePhase.FIRING);
            assertTrue(weapon.isDirectionalMountAlreadyFlipped());

            mek.newRound(2);

            assertTrue(weapon.isDirectionalMountRear(), "The chosen facing itself persists across rounds");
            assertFalse(weapon.isDirectionalMountAlreadyFlipped(), "The once-per-turn tracker resets each round");
            weapon.setDirectionalMountRear(false);
            assertEquals(0, weapon.getDirectionalMountFacing(), "The mount can be refaced again next turn");
        }

        @Test
        @DisplayName("The server-side facing action is refused after an earlier-phase flip")
        void applyMountFacingRefusedWhenAlreadyFlipped() {
            BipedMek mek = new BipedMek();
            Mounted<?> weapon = mountWithDirectionalQuirk(mek,
                  OptionsConstants.QUIRK_WEAPON_POS_DIRECT_TORSO_MOUNT);
            mek.getGame().setPhase(GamePhase.TARGETING);
            DirectionalTorsoMountRules.applyMountFacing(mek, mek.getEquipmentNum(weapon), 3);
            assertEquals(3, weapon.getDirectionalMountFacing());

            mek.getGame().setPhase(GamePhase.FIRING);
            DirectionalTorsoMountRules.applyMountFacing(mek, mek.getEquipmentNum(weapon), 0);
            assertEquals(3, weapon.getDirectionalMountFacing(),
                  "The server must reject a second facing change in a later phase of the same turn");
        }
    }

    @Nested
    @DisplayName("Legacy bare quirk migration (pre-torso-set unit files)")
    class LegacyQuirkTests {

        @Test
        @DisplayName("A valueless directional_torso_mount line loads as all three torso locations")
        void bareChassisQuirkDefaultsToAllTorsos() {
            BipedMek mek = new BipedMek();
            Game game = new Game();
            game.getOptions().getOption(OptionsConstants.ADVANCED_STRATOPS_QUIRKS).setValue(true);
            mek.setGame(game);
            Mounted<?> weapon;
            try {
                weapon = mek.addEquipment(erLargeLaser(), Mek.LOC_LEFT_TORSO);
            } catch (Exception exception) {
                throw new RuntimeException(exception);
            }

            // The Omni-Marauder, Barghest and others carry the legacy bare form "quirk:directional_torso_mount".
            mek.loadQuirks(List.of(new QuirkEntry("directional_torso_mount")));

            assertEquals("LT RT CT", mek.getQuirks()
                        .getOption(OptionsConstants.QUIRK_POS_DIRECTIONAL_TORSO_MOUNT).stringValue(),
                  "The legacy bare quirk must migrate to the all-torsos set, not load as inactive");
            assertTrue(mek.hasQuirk(OptionsConstants.QUIRK_POS_DIRECTIONAL_TORSO_MOUNT));
            assertTrue(weapon.hasDirectionalTorsoMount(),
                  "A torso weapon on a legacy-quirk unit must be a directional mount");
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
        @DisplayName("A HAG/30 may take a Directional Torso Mount (splittable-by-size is not a restriction)")
        void hyperAssaultGaussAllowed() {
            // The canon OmniMarauder Prime mounts its DTM HAG/30 whole in one torso (XTR: Caveat Emptor);
            // being large enough that construction ALLOWS a split is not a location placement restriction.
            assertFalse(WeaponQuirks.isQuirkDisallowed(
                  quirkOption(OptionsConstants.QUIRK_WEAPON_POS_DIRECT_TORSO_MOUNT),
                  new BipedMek(), hyperAssaultGaussRifle30()));
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

        @Test
        @DisplayName("The 360 chassis quirk is offered only on quads; the 2-pt chassis quirk on any Mek")
        void chassisQuirkEligibility() {
            Quirks quirks = new Quirks();
            IOption mount360 = quirks.getOption(OptionsConstants.QUIRK_POS_DIRECTIONAL_TORSO_MOUNT_360);
            assertTrue(Quirks.isQuirkDisallowed(mount360, new BipedMek()), "360 chassis quirk is quad-only");
            assertFalse(Quirks.isQuirkDisallowed(mount360, new QuadMek()), "360 chassis quirk allowed on quads");

            IOption mount2pt = quirks.getOption(OptionsConstants.QUIRK_POS_DIRECTIONAL_TORSO_MOUNT);
            assertFalse(Quirks.isQuirkDisallowed(mount2pt, new BipedMek()), "2-pt chassis quirk allowed on bipeds");
            assertFalse(Quirks.isQuirkDisallowed(mount2pt, new QuadMek()), "2-pt chassis quirk allowed on quads");
        }
    }
}
