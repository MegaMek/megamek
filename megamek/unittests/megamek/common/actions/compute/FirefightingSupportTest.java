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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import megamek.common.HexTarget;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.board.Coords;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.WeaponType;
import megamek.common.game.Game;
import megamek.common.units.ConvInfantry;
import megamek.common.units.EntityMovementMode;
import megamek.common.units.Targetable;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Tests the multi-platoon firefighting choice from TO:AuE p.153 (see {@link FirefightingSupport}): platoons in
 * {@link FirefightingSupport#MODE_FIREFIGHT} mode roll separately, while platoons in
 * {@link FirefightingSupport#MODE_SUPPORT} mode yield their roll and lend -1 to the lead platoon on the same hex.
 */
class FirefightingSupportTest {

    private static final Coords HEX = new Coords(3, 4);

    @BeforeAll
    static void initializeEquipment() {
        EquipmentType.initializeTypes();
    }

    private ConvInfantry firefighter(int id) {
        ConvInfantry infantry = new ConvInfantry();
        infantry.setId(id);
        infantry.setMovementMode(EntityMovementMode.INF_LEG);
        infantry.setSquadSize(7);
        infantry.setSquadCount(4);
        infantry.autoSetInternal();
        infantry.setSpecializations(ConvInfantry.FIRE_ENGINEERS);
        return infantry;
    }

    private void setSupportMode(ConvInfantry platoon) {
        platoon.getWeaponList().stream()
              .filter(weapon -> weapon.getType().hasFlag(WeaponType.F_EXTINGUISHER))
              .findFirst()
              .orElseThrow()
              .setMode(FirefightingSupport.MODE_SUPPORT);
    }

    /**
     * Builds a mocked game where every given platoon has declared an extinguish attack against {@link #HEX}.
     */
    private Game gameFightingOneHex(Game game, HexTarget hex, ConvInfantry... platoons) {
        List<WeaponAttackAction> actions = new ArrayList<>();
        for (ConvInfantry platoon : platoons) {
            when(game.getEntity(platoon.getId())).thenReturn(platoon);
            WeaponAttackAction action = mock(WeaponAttackAction.class);
            when(action.getEntityId()).thenReturn(platoon.getId());
            when(action.getTargetType()).thenReturn(Targetable.TYPE_HEX_EXTINGUISH);
            when(action.getTarget(game)).thenReturn(hex);
            actions.add(action);
        }
        // A fresh enumeration each call: the helper iterates the actions more than once.
        when(game.getActions()).thenAnswer(invocation -> Collections.enumeration(actions));
        return game;
    }

    @Test
    void newFirefighterDefaultsToFirefightMode() {
        String mode = firefighter(1).getWeaponList().stream()
              .filter(weapon -> weapon.getType().hasFlag(WeaponType.F_EXTINGUISHER))
              .findFirst()
              .orElseThrow()
              .curMode()
              .getName();
        assertEquals(FirefightingSupport.MODE_FIREFIGHT, mode,
              "A firefighting platoon should roll on its own by default (RAW option a)");
    }

    @Test
    void lonePlatoonHasNoSupport() {
        Game game = mock(Game.class);
        HexTarget hex = new HexTarget(HEX, 0, Targetable.TYPE_HEX_EXTINGUISH);
        ConvInfantry solo = firefighter(1);
        gameFightingOneHex(game, hex, solo);

        assertEquals(0, FirefightingSupport.supportingPlatoons(game, solo, hex));
        assertFalse(FirefightingSupport.isYieldingSupporter(game, solo, hex));
    }

    @Test
    void allFirefightModePlatoonsRollSeparately() {
        Game game = mock(Game.class);
        HexTarget hex = new HexTarget(HEX, 0, Targetable.TYPE_HEX_EXTINGUISH);
        ConvInfantry first = firefighter(1);
        ConvInfantry second = firefighter(2);
        gameFightingOneHex(game, hex, first, second);

        // RAW option (a): independent rolls, no -1 and no yielding.
        assertEquals(0, FirefightingSupport.supportingPlatoons(game, first, hex));
        assertEquals(0, FirefightingSupport.supportingPlatoons(game, second, hex));
        assertFalse(FirefightingSupport.isYieldingSupporter(game, first, hex));
        assertFalse(FirefightingSupport.isYieldingSupporter(game, second, hex));
    }

    @Test
    void supportPlatoonLendsMinusOneToTheLead() {
        Game game = mock(Game.class);
        HexTarget hex = new HexTarget(HEX, 0, Targetable.TYPE_HEX_EXTINGUISH);
        ConvInfantry lead = firefighter(1);
        ConvInfantry support = firefighter(2);
        setSupportMode(support);
        gameFightingOneHex(game, hex, lead, support);

        // RAW option (b): the single rolling platoon gets -1; the supporter makes no roll.
        assertEquals(1, FirefightingSupport.supportingPlatoons(game, lead, hex));
        assertFalse(FirefightingSupport.isYieldingSupporter(game, lead, hex));
        assertEquals(0, FirefightingSupport.supportingPlatoons(game, support, hex));
        assertTrue(FirefightingSupport.isYieldingSupporter(game, support, hex));
    }

    @Test
    void twoSupportersGiveTheLeadMinusTwo() {
        Game game = mock(Game.class);
        HexTarget hex = new HexTarget(HEX, 0, Targetable.TYPE_HEX_EXTINGUISH);
        ConvInfantry lead = firefighter(1);
        ConvInfantry supportA = firefighter(2);
        ConvInfantry supportB = firefighter(3);
        setSupportMode(supportA);
        setSupportMode(supportB);
        gameFightingOneHex(game, hex, lead, supportA, supportB);

        assertEquals(2, FirefightingSupport.supportingPlatoons(game, lead, hex));
        assertTrue(FirefightingSupport.isYieldingSupporter(game, supportA, hex));
        assertTrue(FirefightingSupport.isYieldingSupporter(game, supportB, hex));
    }

    @Test
    void allSupportModePromotesLowestIdToLead() {
        Game game = mock(Game.class);
        HexTarget hex = new HexTarget(HEX, 0, Targetable.TYPE_HEX_EXTINGUISH);
        ConvInfantry promoted = firefighter(1);
        ConvInfantry support = firefighter(2);
        setSupportMode(promoted);
        setSupportMode(support);
        gameFightingOneHex(game, hex, promoted, support);

        // With nobody set to roll, the lowest-id platoon is promoted so the attack is not wasted, and the
        // remaining supporter still lends -1.
        assertFalse(FirefightingSupport.isYieldingSupporter(game, promoted, hex));
        assertEquals(1, FirefightingSupport.supportingPlatoons(game, promoted, hex));
        assertTrue(FirefightingSupport.isYieldingSupporter(game, support, hex));
        assertEquals(0, FirefightingSupport.supportingPlatoons(game, support, hex));
    }

    @Test
    void nonFirefighterIsNeverSupportOrSupported() {
        Game game = mock(Game.class);
        HexTarget hex = new HexTarget(HEX, 0, Targetable.TYPE_HEX_EXTINGUISH);
        ConvInfantry plain = new ConvInfantry();
        plain.setId(4);
        gameFightingOneHex(game, hex, plain);

        assertEquals(0, FirefightingSupport.supportingPlatoons(game, plain, hex));
        assertFalse(FirefightingSupport.isYieldingSupporter(game, plain, hex));
    }
}
