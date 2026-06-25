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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import megamek.client.ui.Messages;
import megamek.common.Player;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.Mounted;
import megamek.common.equipment.WeaponMounted;
import megamek.common.equipment.WeaponType;
import megamek.common.game.Game;
import megamek.common.options.OptionsConstants;
import megamek.common.units.ConvInfantry;
import megamek.common.units.Infantry;
import megamek.common.weapons.infantry.InfantryWeapon;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Regression tests for the order-independent gating of Disposable Weapon attacks (TO:AuE p.116, Corrected Sixth
 * Printing). A Disposable Weapon attack replaces the platoon's standard weapon attack, so a unit may declare EITHER its
 * Disposable Weapon OR its other weapons in a turn, never both - regardless of which is declared first. The bug that
 * prompted these tests: declaring the disposable first did not block a following standard weapon attack (only the
 * reverse order was gated).
 *
 * @see ComputeToHitIsImpossible#disposableWeaponGateReason(Game, megamek.common.units.Entity, WeaponMounted,
 *       WeaponType)
 */
class ComputeToHitIsImpossibleTest {

    private static final String DISPOSABLE_WEAPON = "Rocket Launcher (LAW)";
    private static final String STANDARD_WEAPON = "InfantryAssaultRifle";

    private Game game;

    @BeforeAll
    static void initializeEquipment() {
        EquipmentType.initializeTypes();
    }

    @BeforeEach
    void setUp() {
        game = new Game();
        game.addPlayer(0, new Player(0, "Test Player"));
        setDisposableRule(true);
    }

    private void setDisposableRule(boolean enabled) {
        game.getOptions().getOption(OptionsConstants.ADVANCED_COMBAT_DISPOSABLE_INFANTRY_WEAPONS).setValue(enabled);
    }

    private ConvInfantry createInfantry() {
        ConvInfantry infantry = new ConvInfantry();
        infantry.setGame(game);
        infantry.setId(game.getNextEntityId());
        infantry.setChassis("Test Platoon");
        infantry.setOwner(game.getPlayer(0));
        infantry.setPrimaryWeapon((InfantryWeapon) EquipmentType.get(STANDARD_WEAPON));
        infantry.autoSetInternal();
        infantry.initializeInternal(28, ConvInfantry.LOC_INFANTRY);
        game.addEntity(infantry);
        return infantry;
    }

    private ConvInfantry createInfantryWithDisposable() {
        ConvInfantry infantry = createInfantry();
        infantry.equipDisposableWeapon((InfantryWeapon) EquipmentType.get(DISPOSABLE_WEAPON));
        return infantry;
    }

    private WeaponMounted disposableMount(ConvInfantry infantry) {
        return infantry.getWeaponList().stream()
              .filter(WeaponMounted::isDisposableWeapon)
              .findFirst()
              .orElseThrow();
    }

    private WeaponMounted addStandardWeapon(ConvInfantry infantry) throws Exception {
        WeaponMounted rifle = (WeaponMounted) Mounted.createMounted(infantry, EquipmentType.get(STANDARD_WEAPON));
        infantry.addEquipment(rifle, ConvInfantry.LOC_INFANTRY, false);
        return rifle;
    }

    private void declareAttack(ConvInfantry attacker, WeaponMounted weapon) {
        game.addAction(new WeaponAttackAction(attacker.getId(), attacker.getId(),
              attacker.getEquipmentNum(weapon)));
    }

    private String gateReason(ConvInfantry attacker, WeaponMounted weapon) {
        return ComputeToHitIsImpossible.disposableWeaponGateReason(game, attacker, weapon,
              (WeaponType) weapon.getType());
    }

    @Test
    @DisplayName("declaring a standard weapon after the Disposable Weapon is impossible (the reported bug)")
    void standardWeaponAfterDisposableIsBlocked() throws Exception {
        ConvInfantry infantry = createInfantryWithDisposable();
        WeaponMounted rifle = addStandardWeapon(infantry);
        declareAttack(infantry, disposableMount(infantry));

        assertEquals(Messages.getString("WeaponAttackAction.DisposableReplacesStandard"), gateReason(infantry, rifle));
    }

    @Test
    @DisplayName("declaring the Disposable Weapon after a standard weapon is impossible (the already-working order)")
    void disposableAfterStandardWeaponIsBlocked() throws Exception {
        ConvInfantry infantry = createInfantryWithDisposable();
        WeaponMounted rifle = addStandardWeapon(infantry);
        declareAttack(infantry, rifle);

        assertEquals(Messages.getString("WeaponAttackAction.DisposableOnly"),
              gateReason(infantry, disposableMount(infantry)));
    }

    @Test
    @DisplayName("a standard weapon is allowed when no Disposable Weapon attack has been declared")
    void standardWeaponAloneIsAllowed() throws Exception {
        ConvInfantry infantry = createInfantryWithDisposable();
        WeaponMounted rifle = addStandardWeapon(infantry);

        assertNull(gateReason(infantry, rifle));
    }

    @Test
    @DisplayName("the Disposable Weapon is allowed when it is the unit's only declared attack")
    void disposableAloneIsAllowed() {
        ConvInfantry infantry = createInfantryWithDisposable();

        assertNull(gateReason(infantry, disposableMount(infantry)));
    }

    @Test
    @DisplayName("with the Disposable Weapon rule disabled, the gate never blocks an attack")
    void ruleDisabledNeverBlocks() throws Exception {
        setDisposableRule(false);
        ConvInfantry infantry = createInfantryWithDisposable();
        WeaponMounted rifle = addStandardWeapon(infantry);
        declareAttack(infantry, disposableMount(infantry));

        assertNull(gateReason(infantry, rifle));
    }

    @Test
    @DisplayName("another unit's Disposable Weapon attack does not block this unit's standard weapon")
    void otherUnitsDisposableDoesNotBlock() throws Exception {
        ConvInfantry otherPlatoon = createInfantryWithDisposable();
        declareAttack(otherPlatoon, disposableMount(otherPlatoon));

        ConvInfantry infantry = createInfantry();
        WeaponMounted rifle = addStandardWeapon(infantry);

        assertNull(gateReason(infantry, rifle));
    }

    @Test
    @DisplayName("an anti-Mek leg attack is not gated here, so its own dedicated rules produce the reason")
    void antiMekAttackIsNotGatedByDisposableRule() {
        ConvInfantry infantry = createInfantryWithDisposable();
        declareAttack(infantry, disposableMount(infantry));
        WeaponType legAttack = (WeaponType) EquipmentType.get(Infantry.LEG_ATTACK);

        assertNull(ComputeToHitIsImpossible.disposableWeaponGateReason(game, infantry, null, legAttack));
    }
}
