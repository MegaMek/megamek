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

package megamek.common.equipment;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import megamek.common.battleArmor.BattleArmor;
import megamek.common.game.Game;
import megamek.common.units.Entity;
import megamek.common.units.EntityMovementMode;
import megamek.common.units.Tank;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.ThrowingSupplier;

/**
 * Regression tests for issue #8638: adding a WiGE vehicle carrying a {@link ClampMountTank} transporter crashed the
 * server with a {@link NullPointerException}, because the design verifier computes walk MP (WiGE minimum 5 Cruise MP
 * check) before the entity - and therefore its transporters - has received a {@link Game} reference. The transient
 * {@code game} field in {@code BattleArmorHandles} must be null-guarded so a transporter without a game simply reports
 * nothing loaded.
 *
 * @author Claude Code (Fable 5)
 */
class ClampMountTankTest {

    private static final int CARRIED_BATTLE_ARMOR_ID = 5;

    private BattleArmor mockMagneticClampBattleArmor() {
        BattleArmor battleArmor = mock(BattleArmor.class);
        when(battleArmor.hasMagneticClamps()).thenReturn(true);
        when(battleArmor.getId()).thenReturn(CARRIED_BATTLE_ARMOR_ID);
        return battleArmor;
    }

    @Test
    @DisplayName("Issue #8638: a detached WiGE tank with a clamp mount computes walk MP without an NPE")
    void wigeTankWithClampMountAndNoGameComputesWalkMP() {
        Tank wigeTank = new Tank();
        wigeTank.setMovementMode(EntityMovementMode.WIGE);
        wigeTank.setOriginalWalkMP(5);
        wigeTank.addTransporter(new ClampMountTank());

        ThrowingSupplier<Integer> computeWalkMP = wigeTank::getWalkMP;
        int walkMP = assertDoesNotThrow(computeWalkMP,
              "Walk MP of a unit not yet added to a game must not throw (server verifies designs pre-add)");
        assertEquals(5, walkMP, "With no game, no carried unit can be resolved, so no cargo MP reduction applies");
    }

    @Test
    @DisplayName("with no game reference, a clamp mount reports nothing loaded even if a unit ID is stored")
    void noGameReferenceMeansNothingLoaded() {
        ClampMountTank clampMount = new ClampMountTank();
        clampMount.load(mockMagneticClampBattleArmor());

        assertTrue(clampMount.getLoadedUnits().isEmpty(), "Without a game the carried unit ID cannot be resolved");
        assertEquals(0, clampMount.getCargoMpReduction(mock(Tank.class)));
        assertFalse(clampMount.unload(mockMagneticClampBattleArmor()));
        assertFalse(clampMount.isWeaponBlockedAt(Tank.LOC_REAR, false));
        assertNull(clampMount.getExteriorUnitAt(Tank.LOC_REAR, false));
    }

    @Test
    @DisplayName("with a game reference, a loaded clamp mount still resolves its carried unit and reduces MP")
    void gameReferenceResolvesCarriedUnit() {
        BattleArmor battleArmor = mockMagneticClampBattleArmor();
        Game game = mock(Game.class);
        when(game.getEntity(CARRIED_BATTLE_ARMOR_ID)).thenReturn(battleArmor);

        ClampMountTank clampMount = new ClampMountTank();
        clampMount.load(battleArmor);
        clampMount.setGame(game);

        List<Entity> loadedUnits = clampMount.getLoadedUnits();
        assertEquals(1, loadedUnits.size());
        assertEquals(battleArmor, loadedUnits.getFirst());
        assertEquals(1, clampMount.getCargoMpReduction(mock(Tank.class)),
              "One loaded magnetic-clamp squad reduces cruise MP by 1");
    }
}
