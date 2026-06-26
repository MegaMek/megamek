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

package megamek.common.weapons.infantry.support.mortar;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import megamek.common.LosEffects;
import megamek.common.compute.Compute;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.EquipmentTypeLookup;
import megamek.common.equipment.InfantryWeaponMounted;
import megamek.common.equipment.WeaponType;
import megamek.common.game.Game;
import megamek.common.options.GameOptions;
import megamek.common.options.OptionsConstants;
import megamek.common.units.ConvInfantry;
import megamek.common.units.Targetable;
import megamek.common.weapons.infantry.InfantryWeapon;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

/**
 * Verifies the TO:AUE rule that conventional infantry whose Light or Heavy Mortar defines their final range value may
 * use indirect fire like Mek Mortars (which Battle Armor mortars already do).
 *
 * <p>The capability is granted by the {@link WeaponType#F_MORTAR_TYPE_INDIRECT} flag plus
 * {@link WeaponType#hasIndirectFire()}. The to-hit logic only consults these on the weapon returned by
 * {@code weapon.getType()}, which for a conventional platoon is the range-defining weapon (the first constructor
 * argument of {@link InfantryWeaponMounted}). That dispatch is what restricts the capability to platoons "for whom
 * Light or Heavy Mortars define the final range value".
 */
class InfantryMortarIndirectFireTest {

    @BeforeAll
    static void setup() {
        EquipmentType.initializeTypes();
    }

    private static InfantryWeapon infantryWeapon(String internalName) {
        return (InfantryWeapon) EquipmentType.get(internalName);
    }

    private static void assertSupportsMortarIndirectFire(String internalName) {
        InfantryWeapon mortar = infantryWeapon(internalName);
        assertTrue(mortar.hasIndirectFire(),
              internalName + " should report indirect fire capability");
        assertTrue(mortar.hasFlag(WeaponType.F_MORTAR_TYPE_INDIRECT),
              internalName + " should carry the mortar indirect flag");
        // Conventional infantry must not gain the AlphaStrike IF ability from the mortar.
        assertFalse(mortar.isAlphaStrikeIndirectFire(),
              internalName + " should not contribute the AlphaStrike IF ability");
    }

    @Test
    void lightMortarSupportsMortarIndirectFire() {
        assertSupportsMortarIndirectFire(EquipmentTypeLookup.INFANTRY_MORTAR_LIGHT);
    }

    @Test
    void heavyMortarSupportsMortarIndirectFire() {
        assertSupportsMortarIndirectFire(EquipmentTypeLookup.INFANTRY_MORTAR_HEAVY);
    }

    @Test
    void lightInfernoMortarSupportsMortarIndirectFire() {
        assertSupportsMortarIndirectFire(EquipmentTypeLookup.INFANTRY_MORTAR_LIGHT_INFERNO);
    }

    @Test
    void heavyInfernoMortarSupportsMortarIndirectFire() {
        assertSupportsMortarIndirectFire(EquipmentTypeLookup.INFANTRY_MORTAR_HEAVY_INFERNO);
    }

    @Test
    void standardInfantryWeaponDoesNotSupportMortarIndirectFire() {
        InfantryWeapon rifle = infantryWeapon(EquipmentTypeLookup.INFANTRY_ASSAULT_RIFLE);
        assertFalse(rifle.hasIndirectFire(),
              "A non-mortar infantry weapon should not report indirect fire capability");
        assertFalse(rifle.hasFlag(WeaponType.F_MORTAR_TYPE_INDIRECT),
              "A non-mortar infantry weapon should not carry the mortar indirect flag");
    }

    /**
     * The indirect-fire gates check the flag on the platoon's range-defining weapon (the InfantryWeaponMounted's type).
     * The mortar therefore enables mortar-style indirect fire only when it is the range-defining weapon.
     */
    @Test
    void mortarEnablesIndirectOnlyWhenItDefinesRange() {
        ConvInfantry platoon = new ConvInfantry();
        InfantryWeapon mortar = infantryWeapon(EquipmentTypeLookup.INFANTRY_MORTAR_LIGHT);
        InfantryWeapon rifle = infantryWeapon(EquipmentTypeLookup.INFANTRY_ASSAULT_RIFLE);

        // Mortar is the range-defining weapon (first constructor argument): capability is honored.
        InfantryWeaponMounted mortarDefinesRange = new InfantryWeaponMounted(platoon, mortar, rifle);
        assertTrue(mortarDefinesRange.getType().hasFlag(WeaponType.F_MORTAR_TYPE_INDIRECT),
              "Mortar defining the platoon range should expose the indirect flag to the to-hit logic");

        // Mortar is the secondary, non-range-defining weapon: capability is not honored.
        InfantryWeaponMounted rifleDefinesRange = new InfantryWeaponMounted(platoon, rifle, mortar);
        assertFalse(rifleDefinesRange.getType().hasFlag(WeaponType.F_MORTAR_TYPE_INDIRECT),
              "A mortar that does not define the platoon range should not expose the indirect flag");
    }

    /**
     * The "do not require a completely blocked line of sight" clause: with the Indirect Fire option on and a clear LOS
     * to the target, {@link Compute#indirectAttackImpossible} must return {@code false} for a mortar (attack is still
     * allowed) but {@code true} for an ordinary infantry weapon (blocked, like an indirect LRM with LOS). This
     * exercises the real gate that consumes {@link WeaponType#F_MORTAR_TYPE_INDIRECT}, so it catches a regression in
     * either the flag or the gate.
     */
    @Test
    void mortarMayFireIndirectlyEvenWithLineOfSight() {
        assertFalse(indirectImpossibleWithLineOfSight(EquipmentTypeLookup.INFANTRY_MORTAR_LIGHT),
              "An infantry mortar should be allowed to fire indirectly even with line of sight to the target");
    }

    @Test
    void standardInfantryWeaponBlockedFromIndirectWithLineOfSight() {
        assertTrue(indirectImpossibleWithLineOfSight(EquipmentTypeLookup.INFANTRY_ASSAULT_RIFLE),
              "A non-mortar infantry weapon firing indirectly should be blocked when it has line of sight");
    }

    /**
     * Runs {@link Compute#indirectAttackImpossible} for the named weapon under: Indirect Fire option on, double-blind
     * off, "indirect always possible" off, and a clear line of sight to the target. Under these conditions the only
     * thing that lets the attack proceed is the mortar indirect flag.
     *
     * @param weaponInternalName the internal name of the firing weapon
     *
     * @return whether the indirect attack is impossible
     */
    private static boolean indirectImpossibleWithLineOfSight(String weaponInternalName) {
        WeaponType weaponType = (WeaponType) EquipmentType.get(weaponInternalName);
        ConvInfantry attacker = new ConvInfantry();
        Targetable target = mock(Targetable.class);

        GameOptions options = mock(GameOptions.class);
        when(options.booleanOption(anyString())).thenReturn(false);
        when(options.booleanOption(OptionsConstants.BASE_INDIRECT_FIRE)).thenReturn(true);

        Game game = mock(Game.class);
        when(game.getOptions()).thenReturn(options);

        LosEffects los = mock(LosEffects.class);
        when(los.canSee()).thenReturn(true);

        try (MockedStatic<LosEffects> mockedLos = mockStatic(LosEffects.class)) {
            mockedLos.when(() -> LosEffects.calculateLOS(any(), any(), any())).thenReturn(los);
            return Compute.indirectAttackImpossible(game, attacker, target, weaponType, null);
        }
    }
}
