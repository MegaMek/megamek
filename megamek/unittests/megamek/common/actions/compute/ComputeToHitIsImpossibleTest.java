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

package megamek.common.actions.compute;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import megamek.common.Player;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.Mounted;
import megamek.common.equipment.WeaponMounted;
import megamek.common.game.Game;
import megamek.common.units.ConvInfantry;
import megamek.common.weapons.infantry.InfantryWeapon;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Regression tests for the order-independent gating of Disposable Weapon attacks (TO:AuE p.116, Corrected Sixth
 * Printing). A Disposable Weapon attack replaces the platoon's standard weapon attack, so a unit may fire EITHER its
 * disposable OR its other weapons in a turn, never both - regardless of which is declared first. The bug that prompted
 * these tests: firing the disposable first did not block a following standard weapon attack (only the reverse order was
 * gated).
 *
 * @see ComputeToHitIsImpossible#hasDeclaredDisposableAttack(Game, megamek.common.units.Entity)
 */
class ComputeToHitIsImpossibleTest {

    private Game game;

    @BeforeAll
    static void initializeEquipment() {
        EquipmentType.initializeTypes();
    }

    @BeforeEach
    void setUp() {
        game = new Game();
        game.addPlayer(0, new Player(0, "Test Player"));
    }

    private ConvInfantry createInfantry() {
        ConvInfantry infantry = new ConvInfantry();
        infantry.setGame(game);
        infantry.setId(game.getNextEntityId());
        infantry.setChassis("Test Platoon");
        infantry.setOwner(game.getPlayer(0));
        infantry.setPrimaryWeapon((InfantryWeapon) EquipmentType.get("InfantryAssaultRifle"));
        infantry.autoSetInternal();
        infantry.initializeInternal(28, ConvInfantry.LOC_INFANTRY);
        game.addEntity(infantry);
        return infantry;
    }

    private WeaponMounted disposableMount(ConvInfantry infantry) {
        return infantry.getWeaponList().stream()
              .filter(WeaponMounted::isDisposableWeapon)
              .findFirst()
              .orElseThrow();
    }

    private WeaponMounted addStandardWeapon(ConvInfantry infantry) throws Exception {
        WeaponMounted rifle = (WeaponMounted) Mounted.createMounted(infantry,
              EquipmentType.get("InfantryAssaultRifle"));
        infantry.addEquipment(rifle, ConvInfantry.LOC_INFANTRY, false);
        return rifle;
    }

    private void declareAttack(ConvInfantry attacker, WeaponMounted weapon) {
        game.addAction(new WeaponAttackAction(attacker.getId(), attacker.getId(),
              attacker.getEquipmentNum(weapon)));
    }

    @Test
    @DisplayName("no declared attacks: the unit has not committed its Disposable Weapon")
    void noDeclaredAttack() {
        ConvInfantry infantry = createInfantry();
        infantry.equipDisposableWeapon((InfantryWeapon) EquipmentType.get("Rocket Launcher (LAW)"));

        assertFalse(ComputeToHitIsImpossible.hasDeclaredDisposableAttack(game, infantry));
    }

    @Test
    @DisplayName("a declared Disposable Weapon attack is detected (blocks a later standard weapon attack)")
    void declaredDisposableAttackIsDetected() {
        ConvInfantry infantry = createInfantry();
        infantry.equipDisposableWeapon((InfantryWeapon) EquipmentType.get("Rocket Launcher (LAW)"));

        declareAttack(infantry, disposableMount(infantry));

        assertTrue(ComputeToHitIsImpossible.hasDeclaredDisposableAttack(game, infantry));
    }

    @Test
    @DisplayName("a declared standard weapon attack is NOT counted as a Disposable Weapon attack")
    void declaredStandardWeaponIsNotDisposable() throws Exception {
        ConvInfantry infantry = createInfantry();
        infantry.equipDisposableWeapon((InfantryWeapon) EquipmentType.get("Rocket Launcher (LAW)"));
        WeaponMounted rifle = addStandardWeapon(infantry);

        declareAttack(infantry, rifle);

        assertFalse(ComputeToHitIsImpossible.hasDeclaredDisposableAttack(game, infantry));
    }

    @Test
    @DisplayName("a Disposable Weapon attack by another unit does not block this unit's standard weapons")
    void disposableAttackByOtherUnitIsIgnored() {
        ConvInfantry otherPlatoon = createInfantry();
        otherPlatoon.equipDisposableWeapon((InfantryWeapon) EquipmentType.get("Rocket Launcher (LAW)"));
        declareAttack(otherPlatoon, disposableMount(otherPlatoon));

        ConvInfantry infantry = createInfantry();

        assertFalse(ComputeToHitIsImpossible.hasDeclaredDisposableAttack(game, infantry));
    }
}
