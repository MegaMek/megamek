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
package megamek.client.bot.princess;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import megamek.common.HexTarget;
import megamek.common.Player;
import megamek.common.board.Coords;
import megamek.common.game.Game;
import megamek.common.units.BuildingTarget;
import megamek.common.units.Entity;
import megamek.common.units.Targetable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Princess picks the buildings that hold enemy turrets before movement. A turret that died after that (fire damage
 * is rolled at the end of movement) used to leave its building on the target list, so the bot wasted its shots on
 * an empty building (issue #9611).
 */
class PrincessTurretBuildingTargetsTest {

    private static final int BOARD_ID = 0;
    private static final Coords TURRET_HEX = new Coords(22, 22);

    private Princess princess;
    private Game game;
    private Player localPlayer;
    private FireControlState fireControlState;
    private Set<Coords> strategicTargets;
    private Targetable turretBuilding;

    @BeforeEach
    void setUp() {
        princess = mock(Princess.class);
        game = mock(Game.class);
        localPlayer = mock(Player.class);
        fireControlState = new FireControlState();
        strategicTargets = new HashSet<>();
        BehaviorSettings behaviorSettings = mock(BehaviorSettings.class);

        doCallRealMethod().when(princess).dropEmptyTurretBuildingTargets();
        when(princess.getFireControlState()).thenReturn(fireControlState);
        when(princess.getGame()).thenReturn(game);
        when(princess.getLocalPlayer()).thenReturn(localPlayer);
        when(princess.getBehaviorSettings()).thenReturn(behaviorSettings);
        when(princess.getStrategicBuildingTargets()).thenReturn(strategicTargets);
        when(behaviorSettings.getIgnoredUnitTargets()).thenReturn(new HashSet<>());

        turretBuilding = mock(BuildingTarget.class);
        when(turretBuilding.getPosition()).thenReturn(TURRET_HEX);
        when(turretBuilding.getBoardId()).thenReturn(BOARD_ID);
        when(turretBuilding.getDisplayName()).thenReturn("Medium Command Tower");
        fireControlState.addAdditionalTarget(turretBuilding);
    }

    private Entity enemyTurret(boolean destroyed) {
        Entity turret = mock(Entity.class);
        Player enemy = mock(Player.class);
        when(enemy.isEnemyOf(localPlayer)).thenReturn(true);
        when(turret.getOwner()).thenReturn(enemy);
        when(turret.hasETypeFlag(Entity.ETYPE_GUN_EMPLACEMENT)).thenReturn(true);
        when(turret.isDestroyed()).thenReturn(destroyed);
        return turret;
    }

    private void placeInTurretHex(Entity... entities) {
        when(game.getEntitiesVector(TURRET_HEX, BOARD_ID, true)).thenReturn(List.of(entities));
    }

    @Test
    void buildingWithALiveTurretStaysATarget() {
        placeInTurretHex(enemyTurret(false));

        princess.dropEmptyTurretBuildingTargets();

        assertEquals(List.of(turretBuilding), fireControlState.getAdditionalTargets());
    }

    @Test
    void buildingWhoseTurretWasDestroyedIsDropped() {
        placeInTurretHex(enemyTurret(true));

        princess.dropEmptyTurretBuildingTargets();

        assertTrue(fireControlState.getAdditionalTargets().isEmpty(),
              "a building with only a destroyed turret in it is not worth shooting");
    }

    @Test
    void buildingWhoseTurretIsGoneFromTheGameIsDropped() {
        placeInTurretHex();

        princess.dropEmptyTurretBuildingTargets();

        assertTrue(fireControlState.getAdditionalTargets().isEmpty());
    }

    @Test
    void strategicBuildingStaysATargetWithoutATurret() {
        strategicTargets.add(TURRET_HEX);
        placeInTurretHex(enemyTurret(true));

        princess.dropEmptyTurretBuildingTargets();

        assertEquals(List.of(turretBuilding), fireControlState.getAdditionalTargets());
    }

    @Test
    void hexTargetsAreLeftAlone() {
        Targetable clearingHex = new HexTarget(new Coords(3, 3), BOARD_ID, Targetable.TYPE_HEX_CLEAR);
        fireControlState.setAdditionalTargets(new ArrayList<>(List.of(clearingHex)));

        princess.dropEmptyTurretBuildingTargets();

        assertEquals(List.of(clearingHex), fireControlState.getAdditionalTargets());
    }
}
