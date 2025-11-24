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
package megamek.common.weapons.handlers;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import megamek.common.equipment.EquipmentMode;
import megamek.common.equipment.INarcPod;
import megamek.common.equipment.MiscType;
import megamek.common.equipment.Mounted;
import megamek.common.equipment.enums.MiscTypeFlag;
import megamek.common.units.Entity;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * Unit tests for ARADEquipmentDetector.
 * <p>
 * Tests verify that equipment detection works correctly for all qualifying electronic systems, including edge cases:
 * <ul>
 *   <li>Stealth Armor blocking</li>
 *   <li>Friendly vs enemy Narc pods</li>
 *   <li>Destroyed equipment</li>
 * </ul>
 *
 * @author Hammer - Built with Claude Code
 * @since 2025-01-16
 */
public class ARADEquipmentDetectorTest {

    private static final int FRIENDLY_TEAM = 1;
    private static final int ENEMY_TEAM = 2;

    @Test
    void testNullTargetReturnsFalse() {
        assertFalse(ARADEquipmentDetector.targetHasQualifyingElectronics(null, FRIENDLY_TEAM),
              "Null target should return false");
    }

    @Test
    void testActiveStealthArmorDetection() {
        Entity target = mock(Entity.class);
        when(target.isStealthActive()).thenReturn(true);

        assertTrue(ARADEquipmentDetector.hasActiveStealthArmor(target),
              "Should detect active Stealth Armor");
    }

    @Test
    void testInactiveStealthArmorNotDetected() {
        Entity target = mock(Entity.class);
        when(target.isStealthActive()).thenReturn(false);

        assertFalse(ARADEquipmentDetector.hasActiveStealthArmor(target),
              "Inactive Stealth Armor should not be detected");
    }

    @Test
    void testStealthBlocksInternalSystems() {
        Entity target = mock(Entity.class);
        when(target.isStealthActive()).thenReturn(true);
        when(target.hasC3()).thenReturn(true);
        when(target.hasECM()).thenReturn(true);
        when(target.isNarcedBy(FRIENDLY_TEAM)).thenReturn(false);
        when(target.getINarcPodsAttached()).thenReturn(Collections.emptyIterator());

        // Stealth blocks C3 and ECM, so should return false
        assertFalse(ARADEquipmentDetector.targetHasQualifyingElectronics(target, FRIENDLY_TEAM),
              "Active Stealth should block internal systems");
    }

    @Test
    void testNarcOverridesStealthArmor() {
        Entity target = mock(Entity.class);
        when(target.isStealthActive()).thenReturn(true);
        when(target.isNarcedBy(FRIENDLY_TEAM)).thenReturn(true);

        // Stealth blocks internal systems but NOT external Narc
        assertTrue(ARADEquipmentDetector.targetHasQualifyingElectronics(target, FRIENDLY_TEAM),
              "Narc should override Stealth Armor blocking");
    }

    @Test
    void testActiveTAGDetection() {
        Entity target = mock(Entity.class);
        when(target.getTaggedBy()).thenReturn(5);  // Some entity ID

        assertTrue(ARADEquipmentDetector.hasActiveTAG(target),
              "Should detect TAG'd target");
    }

    @Test
    void testNoTAGDetection() {
        Entity target = mock(Entity.class);
        when(target.getTaggedBy()).thenReturn(-1);  // Not TAG'd

        assertFalse(ARADEquipmentDetector.hasActiveTAG(target),
              "Should not detect non-TAG'd target");
    }

    @Test
    void testGhostTargetsDetection() {
        Entity target = mock(Entity.class);
        when(target.hasGhostTargets(true)).thenReturn(true);

        assertTrue(ARADEquipmentDetector.hasGhostTargets(target),
              "Should detect Ghost Target generation");
    }

    @Test
    void testNoGhostTargetsDetection() {
        Entity target = mock(Entity.class);
        when(target.hasGhostTargets(true)).thenReturn(false);

        assertFalse(ARADEquipmentDetector.hasGhostTargets(target),
              "Should not detect when no Ghost Targets");
    }

    @Test
    void testStandardNarcDetection() {
        Entity target = mock(Entity.class);
        when(target.isStealthActive()).thenReturn(false);
        when(target.isNarcedBy(FRIENDLY_TEAM)).thenReturn(true);
        when(target.getINarcPodsAttached()).thenReturn(Collections.emptyIterator());

        assertTrue(ARADEquipmentDetector.isNarcTagged(target, FRIENDLY_TEAM),
              "Should detect friendly standard Narc");
    }

    @Test
    void testEnemyNarcNotDetected() {
        Entity target = mock(Entity.class);
        when(target.isNarcedBy(FRIENDLY_TEAM)).thenReturn(false);
        when(target.isNarcedBy(ENEMY_TEAM)).thenReturn(true);
        when(target.getINarcPodsAttached()).thenReturn(Collections.emptyIterator());

        assertFalse(ARADEquipmentDetector.isNarcTagged(target, FRIENDLY_TEAM),
              "Enemy Narc should not trigger ARAD");
    }

    @Test
    void testFriendlyINarcHomingDetection() {
        Entity target = mock(Entity.class);
        when(target.isNarcedBy(FRIENDLY_TEAM)).thenReturn(false);

        List<INarcPod> pods = new ArrayList<>();
        pods.add(new INarcPod(FRIENDLY_TEAM, INarcPod.HOMING, 0));
        when(target.getINarcPodsAttached()).thenReturn(pods.iterator());

        assertTrue(ARADEquipmentDetector.isNarcTagged(target, FRIENDLY_TEAM),
              "Friendly iNarc Homing pod should trigger ARAD");
    }

    @Test
    void testFriendlyINarcNemesisDetection() {
        Entity target = mock(Entity.class);
        when(target.isNarcedBy(FRIENDLY_TEAM)).thenReturn(false);

        List<INarcPod> pods = new ArrayList<>();
        pods.add(new INarcPod(FRIENDLY_TEAM, INarcPod.NEMESIS, 0));
        when(target.getINarcPodsAttached()).thenReturn(pods.iterator());

        assertTrue(ARADEquipmentDetector.isNarcTagged(target, FRIENDLY_TEAM),
              "Friendly iNarc Nemesis pod should trigger ARAD");
    }

    @Test
    void testEnemyINarcNemesisNotDetected() {
        Entity target = mock(Entity.class);
        when(target.isNarcedBy(FRIENDLY_TEAM)).thenReturn(false);

        List<INarcPod> pods = new ArrayList<>();
        pods.add(new INarcPod(ENEMY_TEAM, INarcPod.NEMESIS, 0));
        when(target.getINarcPodsAttached()).thenReturn(pods.iterator());

        assertFalse(ARADEquipmentDetector.isNarcTagged(target, FRIENDLY_TEAM),
              "Enemy iNarc Nemesis pod should NOT trigger ARAD");
    }

    @Test
    void testINarcHaywireNotDetected() {
        Entity target = mock(Entity.class);
        when(target.isNarcedBy(FRIENDLY_TEAM)).thenReturn(false);

        List<INarcPod> pods = new ArrayList<>();
        pods.add(new INarcPod(FRIENDLY_TEAM, INarcPod.HAYWIRE, 0));
        when(target.getINarcPodsAttached()).thenReturn(pods.iterator());

        assertFalse(ARADEquipmentDetector.isNarcTagged(target, FRIENDLY_TEAM),
              "iNarc Haywire pod should NOT trigger ARAD");
    }

    @Test
    void testC3Detection() {
        Entity target = mock(Entity.class);
        when(target.isStealthActive()).thenReturn(false);
        when(target.hasC3()).thenReturn(true);

        // Create mock C3 equipment
        Mounted<?> c3Equipment = createMockEquipment(MiscType.F_C3S, false, false, false);
        List<Mounted<?>> equipment = new ArrayList<>();
        equipment.add(c3Equipment);
        when(target.getEquipment()).thenReturn(equipment);

        assertTrue(ARADEquipmentDetector.hasC3(target),
              "Should detect functional C3");
    }

    @Test
    void testDestroyedC3NotDetected() {
        Entity target = mock(Entity.class);
        when(target.hasC3()).thenReturn(true);

        // Create mock destroyed C3 equipment
        Mounted<?> c3Equipment = createMockEquipment(MiscType.F_C3S, true, false, false);
        List<Mounted<?>> equipment = new ArrayList<>();
        equipment.add(c3Equipment);
        when(target.getEquipment()).thenReturn(equipment);

        assertFalse(ARADEquipmentDetector.hasC3(target),
              "Destroyed C3 should not trigger ARAD");
    }

    @Test
    void testHeavyCommsDetection() {
        Entity target = mock(Entity.class);
        when(target.isStealthActive()).thenReturn(false);

        // Create mock heavy comms equipment (4 tons)
        Mounted<?> comms = createMockCommsEquipment(4.0, false, false, false);
        List<Mounted<?>> equipment = new ArrayList<>();
        equipment.add(comms);
        when(target.getEquipment()).thenReturn(equipment);

        assertTrue(ARADEquipmentDetector.hasHeavyComms(target),
              "Should detect 4-ton communications equipment");
    }

    @Test
    void testLightCommsNotDetected() {
        Entity target = mock(Entity.class);

        // Create mock light comms equipment (2 tons - below 3.5 threshold)
        Mounted<?> comms = createMockCommsEquipment(2.0, false, false, false);
        List<Mounted<?>> equipment = new ArrayList<>();
        equipment.add(comms);
        when(target.getEquipment()).thenReturn(equipment);

        assertFalse(ARADEquipmentDetector.hasHeavyComms(target),
              "2-ton comms should not trigger ARAD (below 3.5 ton threshold)");
    }

    @Test
    void testExactly35TonCommsDetected() {
        Entity target = mock(Entity.class);
        when(target.isStealthActive()).thenReturn(false);

        // Create mock comms equipment exactly at threshold
        Mounted<?> comms = createMockCommsEquipment(3.5, false, false, false);
        List<Mounted<?>> equipment = new ArrayList<>();
        equipment.add(comms);
        when(target.getEquipment()).thenReturn(equipment);

        assertTrue(ARADEquipmentDetector.hasHeavyComms(target),
              "Exactly 3.5-ton comms should trigger ARAD (inclusive threshold)");
    }

    @Test
    void testECMDetection() {
        Entity target = mock(Entity.class);
        when(target.isStealthActive()).thenReturn(false);
        when(target.hasECM()).thenReturn(true);

        Mounted<?> ecm = createMockEquipment(MiscType.F_ECM, false, false, false);
        List<Mounted<?>> equipment = new ArrayList<>();
        equipment.add(ecm);
        when(target.getEquipment()).thenReturn(equipment);

        assertTrue(ARADEquipmentDetector.hasECM(target),
              "Should detect functional ECM");
    }

    @Test
    void testActiveProbeDetection() {
        Entity target = mock(Entity.class);
        when(target.isStealthActive()).thenReturn(false);
        when(target.hasBAP()).thenReturn(true);

        Mounted<?> probe = createMockEquipment(MiscType.F_BAP, false, false, false);
        List<Mounted<?>> equipment = new ArrayList<>();
        equipment.add(probe);
        when(target.getEquipment()).thenReturn(equipment);

        assertTrue(ARADEquipmentDetector.hasActiveProbe(target),
              "Should detect functional Active Probe");
    }

    @Test
    void testArtemisDetection() {
        Entity target = mock(Entity.class);
        when(target.isStealthActive()).thenReturn(false);

        Mounted<?> artemis = createMockEquipment(MiscType.F_ARTEMIS, false, false, false);
        List<Mounted<?>> equipment = new ArrayList<>();
        equipment.add(artemis);
        when(target.getEquipment()).thenReturn(equipment);

        assertTrue(ARADEquipmentDetector.hasArtemis(target),
              "Should detect functional Artemis IV");
    }

    @Test
    void testBlueShieldDetection() {
        Entity target = mock(Entity.class);
        when(target.isStealthActive()).thenReturn(false);

        Mounted<?> blueShield = createMockEquipment(MiscType.F_BLUE_SHIELD, false, false, false);

        // Mock Blue Shield in "On" mode
        EquipmentMode onMode = mock(EquipmentMode.class);
        when(onMode.equals("On")).thenReturn(true);
        doReturn(onMode).when(blueShield).curMode();

        List<Mounted<?>> equipment = new ArrayList<>();
        equipment.add(blueShield);
        when(target.getEquipment()).thenReturn(equipment);

        assertTrue(ARADEquipmentDetector.hasBlueShield(target),
              "Should detect Blue Shield in On mode");
    }

    @Test
    void testBlueShieldOffMode() {
        Entity target = mock(Entity.class);
        when(target.isStealthActive()).thenReturn(false);

        Mounted<?> blueShield = createMockEquipment(MiscType.F_BLUE_SHIELD, false, false, false);

        // Mock Blue Shield in "Off" mode
        EquipmentMode offMode = mock(EquipmentMode.class);
        when(offMode.equals("On")).thenReturn(false);
        when(offMode.equals("Off")).thenReturn(true);
        doReturn(offMode).when(blueShield).curMode();

        List<Mounted<?>> equipment = new ArrayList<>();
        equipment.add(blueShield);
        when(target.getEquipment()).thenReturn(equipment);

        assertFalse(ARADEquipmentDetector.hasBlueShield(target),
              "Should NOT detect Blue Shield in Off mode");
    }

    @Test
    void testBlueShieldModeAffectsElectronicsDetection() {
        Entity target = mock(Entity.class);
        when(target.isStealthActive()).thenReturn(false);
        // Mock all other equipment detection methods to isolate Blue Shield testing
        when(target.hasC3()).thenReturn(false);
        when(target.hasC3i()).thenReturn(false);
        when(target.hasC3M()).thenReturn(false);
        when(target.hasC3MM()).thenReturn(false);
        when(target.hasECM()).thenReturn(false);
        when(target.hasBAP()).thenReturn(false);
        when(target.getTaggedBy()).thenReturn(-1);
        when(target.hasGhostTargets(true)).thenReturn(false);
        when(target.isNarcedBy(FRIENDLY_TEAM)).thenReturn(false);
        when(target.getINarcPodsAttached()).thenReturn(Collections.emptyIterator());

        Mounted<?> blueShield = createMockEquipment(MiscType.F_BLUE_SHIELD, false, false, false);

        // Scenario 1: Blue Shield ON - should be detected
        EquipmentMode onMode = mock(EquipmentMode.class);
        when(onMode.equals("On")).thenReturn(true);
        doReturn(onMode).when(blueShield).curMode();

        List<Mounted<?>> equipment = new ArrayList<>();
        equipment.add(blueShield);
        when(target.getEquipment()).thenReturn(equipment);

        assertTrue(ARADEquipmentDetector.targetHasQualifyingElectronics(target, FRIENDLY_TEAM),
              "Blue Shield in On mode should qualify as electronics");

        // Scenario 2: Blue Shield OFF - should NOT be detected
        EquipmentMode offMode = mock(EquipmentMode.class);
        when(offMode.equals("On")).thenReturn(false);
        doReturn(offMode).when(blueShield).curMode();

        assertFalse(ARADEquipmentDetector.targetHasQualifyingElectronics(target, FRIENDLY_TEAM),
              "Blue Shield in Off mode should NOT qualify as electronics");
    }

    @Test
    void testMultipleSystemsDetected() {
        Entity target = mock(Entity.class);
        when(target.isStealthActive()).thenReturn(false);
        when(target.hasC3()).thenReturn(true);
        when(target.hasECM()).thenReturn(true);

        Mounted<?> c3 = createMockEquipment(MiscType.F_C3S, false, false, false);
        Mounted<?> ecm = createMockEquipment(MiscType.F_ECM, false, false, false);
        List<Mounted<?>> equipment = new ArrayList<>();
        equipment.add(c3);
        equipment.add(ecm);
        when(target.getEquipment()).thenReturn(equipment);

        assertTrue(ARADEquipmentDetector.targetHasQualifyingElectronics(target, FRIENDLY_TEAM),
              "Target with C3 + ECM should have qualifying electronics");
    }

    @Test
    void testAnyOneSystemSufficient() {
        Entity target = mock(Entity.class);
        when(target.isStealthActive()).thenReturn(false);
        when(target.hasECM()).thenReturn(true);
        when(target.hasC3()).thenReturn(false);

        Mounted<?> ecm = createMockEquipment(MiscType.F_ECM, false, false, false);
        List<Mounted<?>> equipment = new ArrayList<>();
        equipment.add(ecm);
        when(target.getEquipment()).thenReturn(equipment);

        assertTrue(ARADEquipmentDetector.targetHasQualifyingElectronics(target, FRIENDLY_TEAM),
              "Single ECM should be sufficient for ARAD bonus");
    }

    @Test
    void testNoElectronicsDetected() {
        Entity target = mock(Entity.class);
        when(target.isStealthActive()).thenReturn(false);
        when(target.hasC3()).thenReturn(false);
        when(target.hasECM()).thenReturn(false);
        when(target.hasBAP()).thenReturn(false);
        when(target.getTaggedBy()).thenReturn(-1);
        when(target.hasGhostTargets(true)).thenReturn(false);
        when(target.isNarcedBy(FRIENDLY_TEAM)).thenReturn(false);
        when(target.getINarcPodsAttached()).thenReturn(Collections.emptyIterator());
        when(target.getEquipment()).thenReturn(new ArrayList<>());

        assertFalse(ARADEquipmentDetector.targetHasQualifyingElectronics(target, FRIENDLY_TEAM),
              "Target with no electronics should return false");
    }

    /**
     * Helper method to create mock equipment with specific flags and states.
     */
    private Mounted<?> createMockEquipment(MiscTypeFlag flag, boolean destroyed, boolean missing, boolean breached) {
        Mounted<?> equipment = mock(Mounted.class);
        MiscType type = mock(MiscType.class);

        doReturn(false).when(type).hasFlag(Mockito.any(MiscTypeFlag.class));
        doReturn(true).when(type).hasFlag(flag);
        doReturn(type).when(equipment).getType();
        doReturn(destroyed).when(equipment).isDestroyed();
        doReturn(missing).when(equipment).isMissing();
        doReturn(breached).when(equipment).isBreached();

        return equipment;
    }

    /**
     * Helper method to create mock communications equipment with specific tonnage.
     */
    private Mounted<?> createMockCommsEquipment(double tonnage, boolean destroyed, boolean missing, boolean breached) {
        Mounted<?> equipment = mock(Mounted.class);
        MiscType type = mock(MiscType.class);

        doReturn(false).when(type).hasFlag(Mockito.any(MiscTypeFlag.class));
        doReturn(true).when(type).hasFlag(MiscType.F_COMMUNICATIONS);
        doReturn(tonnage).when(equipment).getTonnage();
        doReturn(type).when(equipment).getType();
        doReturn(destroyed).when(equipment).isDestroyed();
        doReturn(missing).when(equipment).isMissing();
        doReturn(breached).when(equipment).isBreached();

        return equipment;
    }
}
