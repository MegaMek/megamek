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
package megamek.common.battleArmor;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.List;

import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.EquipmentTypeLookup;
import megamek.common.equipment.MiscMounted;
import megamek.common.equipment.MiscType;
import megamek.common.equipment.Mounted;
import megamek.common.units.BaConstructionUtil;
import megamek.common.verifier.EntityVerifier;
import megamek.common.verifier.TestBattleArmor;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests Battle Armor eligibility to carry Disposable Weapons (TO:AuE p.116, Corrected Sixth Printing): only suits with
 * an anti-personnel weapon mount or two armored gloves may carry them. Armored gloves themselves carry the
 * {@code F_AP_MOUNT} flag, so the "AP mount" path must exclude them.
 * <p>
 * Also tests the construction validation of the actual mounting: the Disposable Weapon must be attached to the
 * anti-personnel weapon mount or to an armored glove (on a suit with two armored gloves) - a suit whose AP mount is
 * occupied by a different weapon may not carry a Disposable Weapon on the side.
 * </p>
 */
class BattleArmorDisposableWeaponTest {

    private static final String DISPOSABLE_WEAPON = "InfantryGrenade";
    private static final String OTHER_AP_WEAPON = "Auto-Pistol";
    // Internal name from MiscType#createBAArmoredGlove (mirrored in BattleArmor.MANIPULATOR_TYPE_STRINGS, which we
    // cannot reference here: it would class-load BattleArmor before EquipmentType.initializeTypes() has run)
    private static final String ARMORED_GLOVE = "BAArmoredGlove";
    private static final String DISPOSABLE_ERROR_MARKER = "is a Disposable Weapon";

    @BeforeAll
    static void initialize() {
        EquipmentType.initializeTypes();
    }

    private static MiscMounted miscWithFlags(boolean apMount, boolean armoredGlove) {
        MiscMounted mount = mock(MiscMounted.class);
        MiscType type = mock(MiscType.class);
        when(mount.getType()).thenReturn(type);
        when(type.hasFlag(MiscType.F_AP_MOUNT)).thenReturn(apMount);
        when(type.hasFlag(MiscType.F_ARMORED_GLOVE)).thenReturn(armoredGlove);
        return mount;
    }

    private static BattleArmor battleArmorWith(List<MiscMounted> misc, int armoredGloveCount) {
        BattleArmor battleArmor = mock(BattleArmor.class);
        when(battleArmor.canCarryDisposableWeapons()).thenCallRealMethod();
        doReturn(misc).when(battleArmor).getMisc();
        doReturn(armoredGloveCount).when(battleArmor).countWorkingMisc(MiscType.F_ARMORED_GLOVE);
        return battleArmor;
    }

    @Test
    @DisplayName("a dedicated anti-personnel weapon mount makes the suit eligible")
    void apMountIsEligible() {
        BattleArmor battleArmor = battleArmorWith(List.of(miscWithFlags(true, false)), 0);
        assertTrue(battleArmor.canCarryDisposableWeapons());
    }

    @Test
    @DisplayName("two armored gloves make the suit eligible")
    void twoArmoredGlovesAreEligible() {
        List<MiscMounted> gloves = List.of(miscWithFlags(true, true), miscWithFlags(true, true));
        BattleArmor battleArmor = battleArmorWith(gloves, 2);
        assertTrue(battleArmor.canCarryDisposableWeapons());
    }

    @Test
    @DisplayName("a single armored glove is NOT enough (and does not count as an AP mount)")
    void singleArmoredGloveIsNotEligible() {
        BattleArmor battleArmor = battleArmorWith(List.of(miscWithFlags(true, true)), 1);
        assertFalse(battleArmor.canCarryDisposableWeapons());
    }

    @Test
    @DisplayName("a suit with neither an AP mount nor armored gloves is not eligible")
    void noMountIsNotEligible() {
        BattleArmor battleArmor = battleArmorWith(List.of(), 0);
        assertFalse(battleArmor.canCarryDisposableWeapons());
    }

    private static BattleArmor buildSuit() {
        BattleArmor battleArmor = new BattleArmor();
        battleArmor.setChassisType(BattleArmor.CHASSIS_TYPE_BIPED);
        battleArmor.setSquadSize(4);
        return battleArmor;
    }

    private static Mounted<?> addToSquad(BattleArmor battleArmor, String internalName) throws Exception {
        Mounted<?> mounted = Mounted.createMounted(battleArmor, EquipmentType.get(internalName));
        battleArmor.addEquipment(mounted, BattleArmor.LOC_SQUAD, false);
        return mounted;
    }

    private static String verifierErrors(BattleArmor battleArmor) {
        EntityVerifier entityVerifier = EntityVerifier.getInstance(
              new File("testresources/data/mekfiles/UnitVerifierOptions.xml"));
        TestBattleArmor testBattleArmor = new TestBattleArmor(battleArmor, entityVerifier.baOption, null);
        StringBuffer errors = new StringBuffer();
        testBattleArmor.hasIllegalEquipmentCombinations(errors);
        return errors.toString();
    }

    @Test
    @DisplayName("a Disposable Weapon mounted in the AP weapon mount passes verification")
    void disposableInApMountIsLegal() throws Exception {
        BattleArmor battleArmor = buildSuit();
        Mounted<?> apMount = addToSquad(battleArmor, EquipmentTypeLookup.BA_APM);
        Mounted<?> disposable = addToSquad(battleArmor, DISPOSABLE_WEAPON);
        BaConstructionUtil.mountOnApm(disposable, apMount);

        String errors = verifierErrors(battleArmor);
        assertFalse(errors.contains(DISPOSABLE_ERROR_MARKER), errors);
    }

    @Test
    @DisplayName("a Disposable Weapon carried by an armored glove on a two-glove suit passes verification")
    void disposableOnGloveWithTwoGlovesIsLegal() throws Exception {
        BattleArmor battleArmor = buildSuit();
        Mounted<?> leftGlove = addToSquad(battleArmor, ARMORED_GLOVE);
        addToSquad(battleArmor, ARMORED_GLOVE);
        Mounted<?> disposable = addToSquad(battleArmor, DISPOSABLE_WEAPON);
        BaConstructionUtil.mountOnApm(disposable, leftGlove);

        String errors = verifierErrors(battleArmor);
        assertFalse(errors.contains(DISPOSABLE_ERROR_MARKER), errors);
    }

    @Test
    @DisplayName("a Disposable Weapon on a single glove fails even when the suit's AP mount holds another weapon")
    void disposableOnSingleGloveWithOccupiedApMountIsIllegal() throws Exception {
        BattleArmor battleArmor = buildSuit();
        Mounted<?> apMount = addToSquad(battleArmor, EquipmentTypeLookup.BA_APM);
        Mounted<?> otherWeapon = addToSquad(battleArmor, OTHER_AP_WEAPON);
        BaConstructionUtil.mountOnApm(otherWeapon, apMount);
        Mounted<?> glove = addToSquad(battleArmor, ARMORED_GLOVE);
        Mounted<?> disposable = addToSquad(battleArmor, DISPOSABLE_WEAPON);
        BaConstructionUtil.mountOnApm(disposable, glove);

        String errors = verifierErrors(battleArmor);
        assertTrue(errors.contains(DISPOSABLE_ERROR_MARKER), errors);
    }

    @Test
    @DisplayName("a Disposable Weapon attached to nothing fails verification even when an AP mount exists")
    void unattachedDisposableIsIllegal() throws Exception {
        BattleArmor battleArmor = buildSuit();
        addToSquad(battleArmor, EquipmentTypeLookup.BA_APM);
        addToSquad(battleArmor, DISPOSABLE_WEAPON);

        String errors = verifierErrors(battleArmor);
        assertTrue(errors.contains(DISPOSABLE_ERROR_MARKER), errors);
    }
}
