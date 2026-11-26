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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.when;

import java.util.Vector;

import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.compute.Compute;
import megamek.common.equipment.WeaponMounted;
import megamek.common.equipment.WeaponType;
import megamek.common.game.Game;
import megamek.common.options.GameOptions;
import megamek.common.planetaryConditions.EMI;
import megamek.common.planetaryConditions.PlanetaryConditions;
import megamek.common.units.BipedMek;
import megamek.common.units.Targetable;
import megamek.server.totalWarfare.TWGameManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link LBXHandler} and {@link CLLBXPrototypeHandler} cluster hit resolution
 * ({@link LBXHandler#calcHits}), verifying that planetary EMI applies the {@code -2} Cluster Hits Table modifier
 * without using the hot-loaded missile roll.
 */
public class LBXHandlerTest {
    private final ToHitData mockToHit = mock(ToHitData.class);
    private final WeaponAttackAction mockAction = mock(WeaponAttackAction.class);
    private final Game mockGame = mock(Game.class);
    private final GameOptions mockOptions = mock(GameOptions.class);
    private final Board mockBoard = mock(Board.class);
    private final TWGameManager mockGameManager = mock(TWGameManager.class);
    private final BipedMek mockAttacker = mock(BipedMek.class);
    private final WeaponMounted mockWeapon = mock(WeaponMounted.class);
    private final WeaponType mockWeaponType = mock(WeaponType.class);
    private final BipedMek mockTarget = mock(BipedMek.class);

    @BeforeEach
    void setUp() {
        var mockConditions = new PlanetaryConditions();
        mockConditions.setEMI(EMI.EMI);
        when(mockGame.getOptions()).thenReturn(mockOptions);
        when(mockOptions.booleanOption(anyString())).thenReturn(false);
        when(mockGame.getBoard()).thenReturn(mockBoard);
        when(mockGame.getPlanetaryConditions()).thenReturn(mockConditions);


        when(mockAttacker.getId()).thenReturn(1);
        when(mockGame.getEntity(1)).thenReturn(mockAttacker);
        when(mockAttacker.getAttackingEntity()).thenReturn(mockAttacker);
        var attackerCoords = new Coords(0, 0);
        when(mockAttacker.getPosition()).thenReturn(attackerCoords);
        doReturn(mockWeapon).when(mockAttacker).getEquipment(0);

        when(mockTarget.getId()).thenReturn(2);
        var targetCoords = new Coords(0, 10);
        when(mockTarget.getPosition()).thenReturn(targetCoords);
        when(mockTarget.getTargetType()).thenReturn(Targetable.TYPE_ENTITY);
        when(mockGame.getTarget(Targetable.TYPE_ENTITY, 2)).thenReturn(mockTarget);

        when(mockWeapon.getType()).thenReturn(mockWeaponType);
        when(mockWeaponType.getInternalName()).thenReturn("ISLBXAC10");
        when(mockWeaponType.getRackSize()).thenReturn(10);
        when(mockWeaponType.getRanges(mockWeapon)).thenReturn(new int[]{0, 6, 12, 18, 24 });

        when(mockAction.getEntityId()).thenReturn(1);
        when(mockAction.getWeaponId()).thenReturn(0);
        when(mockAction.getTargetType()).thenReturn(Targetable.TYPE_ENTITY);
        when(mockAction.getTargetId()).thenReturn(2);

        when(mockToHit.getTableDesc()).thenReturn("");
    }

    @Test
    @DisplayName("LBXHandler rolls cluster hits under EMI without hot-loaded penalty")
    void lbxClusterUnderEmiDoesNotRollHotLoaded() throws Exception {
        var handler = new LBXHandler(mockToHit, mockAction, mockGame, mockGameManager);

        try (var compute = mockStatic(Compute.class)) {
            compute.when(() -> Compute.missilesHit(10, -2)).thenReturn(6);
            var hits = handler.calcHits(new Vector<>());
            compute.verify(() -> Compute.missilesHit(10, -2), only());
            assertEquals(6, hits);
        }
    }

    @Test
    @DisplayName("CLLBXPrototypeHandler rolls cluster hits under EMI without hot-loaded penalty")
    void clanLBXClusterUnderEmiDoesNotRollHotLoaded() throws Exception {
        var handler = new CLLBXPrototypeHandler(mockToHit, mockAction, mockGame, mockGameManager);

        try (var compute = mockStatic(Compute.class)) {
            compute.when(() -> Compute.missilesHit(10, -3)).thenReturn(5);
            var hits = handler.calcHits(new Vector<>());
            compute.verify(() -> Compute.missilesHit(10, -3), only());
            assertEquals(5, hits);
        }
    }
}
