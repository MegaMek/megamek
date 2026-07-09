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

package megamek.ai.dataset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.game.Game;
import megamek.common.units.Entity;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * Unit tests for UnitAttack class and its serializer.
 */
class UnitAttackTest {

    @Test
    void testFromAttackAction() {
        WeaponAttackAction mockAction = Mockito.mock(WeaponAttackAction.class);
        Game mockGame = Mockito.mock(Game.class);
        Entity mockAttacker = Mockito.mock(Entity.class);
        Entity mockWeaponEntity = Mockito.mock(Entity.class);
        Entity mockTarget = Mockito.mock(Entity.class);
        ToHitData mockToHit = Mockito.mock(ToHitData.class);

        Mockito.when(mockAction.getEntity(mockGame)).thenReturn(mockWeaponEntity);
        Mockito.when(mockWeaponEntity.getAttackingEntity()).thenReturn(mockAttacker);
        Mockito.when(mockAttacker.getId()).thenReturn(1);
        Mockito.when(mockAttacker.getOwnerId()).thenReturn(2);
        Mockito.when(mockAttacker.getRole()).thenReturn(megamek.common.units.UnitRole.SCOUT);
        Mockito.when(mockAttacker.isDeployed()).thenReturn(true);
        Mockito.when(mockAttacker.getPosition()).thenReturn(new megamek.common.board.Coords(0, 0));
        Mockito.when(mockAttacker.getFacing()).thenReturn(0);

        Mockito.when(mockAction.getTarget(mockGame)).thenReturn(mockTarget);
        Mockito.when(mockTarget.getId()).thenReturn(10);
        Mockito.when(mockTarget.getPosition()).thenReturn(new megamek.common.board.Coords(1, 1));
        Mockito.when(mockTarget.getOwnerId()).thenReturn(3);
        Mockito.when(mockTarget.getRole()).thenReturn(megamek.common.units.UnitRole.BRAWLER);
        Mockito.when(mockTarget.getFacing()).thenReturn(3);

        Mockito.when(mockAction.toHit(mockGame)).thenReturn(mockToHit);
        Mockito.when(mockToHit.getValue()).thenReturn(7);
        Mockito.when(mockAction.getAimedLocation()).thenReturn(-1);
        Mockito.when(mockAction.getAimingMode()).thenReturn(megamek.common.enums.AimingMode.NONE);
        Mockito.when(mockAction.getAmmoId()).thenReturn(-1);
        Mockito.when(mockAction.getWeaponId()).thenReturn(0);

        UnitAttack attack = UnitAttack.fromAttackAction(mockAction, mockGame);

        assertEquals(1, attack.get(UnitAttack.Field.ENTITY_ID));
        assertEquals(10, attack.get(UnitAttack.Field.TARGET_ID));
        assertEquals(7.0, ((Number) attack.get(UnitAttack.Field.TO_HIT)).doubleValue(), 0.01);
    }

    @Test
    void testSerializer() {
        UnitAttack attack = new UnitAttack();
        attack.put(UnitAttack.Field.ENTITY_ID, 1)
              .put(UnitAttack.Field.TARGET_ID, 10)
              .put(UnitAttack.Field.TO_HIT, 7.0);

        UnitAttackSerializer serializer = new UnitAttackSerializer();
        String serialized = serializer.serialize(attack);

        assertNotNull(serialized);
        assertTrue(serialized.contains("1"));
        assertTrue(serialized.contains("10"));
        assertTrue(serialized.contains("7.00")); // format handler for double
    }
}
