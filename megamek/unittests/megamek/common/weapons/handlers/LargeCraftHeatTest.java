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
package megamek.common.weapons.handlers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;

import megamek.common.actions.WeaponAttackAction;
import megamek.common.equipment.WeaponMounted;
import megamek.common.game.Game;
import megamek.common.options.GameOptions;
import megamek.common.options.OptionsConstants;
import megamek.common.units.Entity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Regression tests for issue #8966: {@link WeaponHandler#getLargeCraftHeat(Entity)} read the Heat by bay option
 * backwards, so point defence heat accounting disagreed with the firing validation in ComputeToHitIsImpossible.
 *
 * <p>The large craft has fired two weapons from the same front arc. Each weapon's bay generates 10 heat, and the arc
 * as a whole generates 15.</p>
 */
class LargeCraftHeatTest {

    private static final int SHIP_ID = 1;
    private static final int BAY_HEAT = 10;
    private static final int ARC_HEAT = 15;

    private Game game;
    private GameOptions options;
    private TestHandler handler;
    private Entity ship;

    /** Exposes the protected method under test. */
    private static class TestHandler extends WeaponHandler {
        TestHandler(Game game) {
            this.game = game;
        }
    }

    @BeforeEach
    void setUp() {
        game = mock(Game.class);
        options = mock(GameOptions.class);
        when(game.getOptions()).thenReturn(options);

        ship = mock(Entity.class);
        when(ship.getId()).thenReturn(SHIP_ID);
        when(ship.isLargeCraft()).thenReturn(true);
        when(ship.locations()).thenReturn(4);
        when(ship.getHeatInArc(0, false)).thenReturn(ARC_HEAT);

        AttackHandler first = declareFrontArcAttack(0);
        AttackHandler second = declareFrontArcAttack(1);
        when(game.getAttacks()).thenAnswer(invocation -> Collections.enumeration(List.of(first, second)));

        handler = new TestHandler(game);
    }

    private AttackHandler declareFrontArcAttack(int weaponId) {
        WeaponMounted weapon = mock(WeaponMounted.class);
        when(weapon.getHeatByBay()).thenReturn(BAY_HEAT);
        when(weapon.getLocation()).thenReturn(0);
        when(weapon.isRearMounted()).thenReturn(false);
        doReturn(weapon).when(ship).getEquipment(weaponId);

        WeaponAttackAction action = mock(WeaponAttackAction.class);
        when(action.getEntityId()).thenReturn(SHIP_ID);
        when(action.getWeaponId()).thenReturn(weaponId);

        AttackHandler attackHandler = mock(AttackHandler.class);
        when(attackHandler.getWeaponAttackAction()).thenReturn(action);
        return attackHandler;
    }

    @Test
    void heatByBayOffCountsEachArcOnce() {
        when(options.booleanOption(OptionsConstants.ADVANCED_AERO_RULES_HEAT_BY_BAY)).thenReturn(false);

        assertEquals(ARC_HEAT, handler.getLargeCraftHeat(ship));
    }

    @Test
    void heatByBayOnSumsEveryBay() {
        when(options.booleanOption(OptionsConstants.ADVANCED_AERO_RULES_HEAT_BY_BAY)).thenReturn(true);

        assertEquals(2 * BAY_HEAT, handler.getLargeCraftHeat(ship));
    }
}
