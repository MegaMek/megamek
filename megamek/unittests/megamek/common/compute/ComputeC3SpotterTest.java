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
package megamek.common.compute;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import megamek.common.ECMInfo;
import megamek.common.Player;
import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.equipment.EquipmentType;
import megamek.common.exceptions.LocationFullException;
import megamek.common.game.Game;
import megamek.common.options.GameOptions;
import megamek.common.planetaryConditions.PlanetaryConditions;
import megamek.common.units.BipedMek;
import megamek.common.units.Entity;
import megamek.common.units.Mek;
import megamek.common.units.Targetable;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for the C3 spotter search, guarding the closest-connected-spotter selection and the precomputed-ECM overload
 * added for performance (see issue #8441).
 */
class ComputeC3SpotterTest {

    private Game mockGame;
    private List<Entity> gameEntities;

    @BeforeAll
    static void beforeAll() {
        EquipmentType.initializeTypes();
    }

    @BeforeEach
    void setUp() {
        Player mockPlayer = mock(Player.class);

        Board mockBoard = mock(Board.class);
        when(mockBoard.isSpace()).thenReturn(false);
        when(mockBoard.contains(any(Coords.class))).thenReturn(true);
        when(mockBoard.contains(anyInt(), anyInt())).thenReturn(true);

        GameOptions mockOptions = mock(GameOptions.class);
        when(mockOptions.booleanOption(anyString())).thenReturn(false);

        gameEntities = new ArrayList<>();

        mockGame = mock(Game.class);
        when(mockGame.getBoard()).thenReturn(mockBoard);
        when(mockGame.getBoard(anyInt())).thenReturn(mockBoard);
        when(mockGame.getBoard(any(Targetable.class))).thenReturn(mockBoard);
        when(mockGame.getOptions()).thenReturn(mockOptions);
        when(mockGame.getPlayer(anyInt())).thenReturn(mockPlayer);
        when(mockGame.getPlanetaryConditions()).thenReturn(new PlanetaryConditions());
        when(mockGame.getSmokeCloudList()).thenReturn(new ArrayList<>());
        when(mockGame.getEntitiesVector()).thenReturn(gameEntities);
        when(mockGame.isOnGroundMap(any(Entity.class))).thenReturn(true);
        when(mockGame.onConnectedBoards(any(Entity.class), any(Targetable.class))).thenReturn(true);
        when(mockGame.onTheSameBoard(any(Entity.class), any(Entity.class))).thenReturn(true);
        when(mockGame.hasBoardLocationOf(any(Targetable.class))).thenReturn(true);
    }

    private Mek createMek(int id, Coords position) {
        Mek mek = new BipedMek();
        mek.setGame(mockGame);
        mek.setId(id);
        mek.setPosition(position);
        mek.setDeployed(true);
        gameEntities.add(mek);
        return mek;
    }

    private Mek createC3iMek(int id, Coords position) throws LocationFullException {
        Mek mek = createMek(id, position);
        mek.addEquipment(EquipmentType.get("ISC3iUnit"), Mek.LOC_LEFT_TORSO);
        return mek;
    }

    private Mek createNovaMek(int id, Coords position) throws LocationFullException {
        Mek mek = createMek(id, position);
        mek.addEquipment(EquipmentType.get("NovaCEWS"), Mek.LOC_LEFT_TORSO);
        return mek;
    }

    @Test
    void returnsAttackerWhenNotOnAC3Network() {
        Mek attacker = createMek(1, new Coords(0, 0));
        Mek target = createMek(2, new Coords(0, 20));

        Entity spotter = ComputeC3Spotter.findC3Spotter(mockGame, attacker, target);

        assertSame(attacker, spotter);
    }

    @Test
    void picksTheNetworkMemberClosestToTheTarget() throws LocationFullException {
        Mek attacker = createC3iMek(1, new Coords(0, 0));
        Mek farSpotter = createC3iMek(2, new Coords(0, 10));
        Mek nearSpotter = createC3iMek(3, new Coords(0, 18));
        Mek target = createMek(4, new Coords(0, 20));
        attacker.setC3NetIdSelf();
        farSpotter.setC3NetId(attacker);
        nearSpotter.setC3NetId(attacker);

        Entity spotter = ComputeC3Spotter.findC3Spotter(mockGame, attacker, target);

        assertSame(nearSpotter, spotter);
    }

    @Test
    void picksTheNovaNetworkMemberClosestToTheTarget() throws LocationFullException {
        Mek attacker = createNovaMek(1, new Coords(0, 0));
        Mek farSpotter = createNovaMek(2, new Coords(0, 10));
        Mek nearSpotter = createNovaMek(3, new Coords(0, 18));
        Mek target = createMek(4, new Coords(0, 20));
        attacker.setC3NetIdSelf();
        farSpotter.setC3NetId(attacker);
        nearSpotter.setC3NetId(attacker);

        Entity spotter = ComputeC3Spotter.findC3Spotter(mockGame, attacker, target);

        assertSame(nearSpotter, spotter);
    }

    @Test
    void precomputedEcmOverloadMatchesOnDemandResult() throws LocationFullException {
        Mek attacker = createC3iMek(1, new Coords(0, 0));
        Mek farSpotter = createC3iMek(2, new Coords(0, 10));
        Mek nearSpotter = createC3iMek(3, new Coords(0, 18));
        Mek target = createMek(4, new Coords(0, 20));
        attacker.setC3NetIdSelf();
        farSpotter.setC3NetId(attacker);
        nearSpotter.setC3NetId(attacker);

        Entity onDemandResult = ComputeC3Spotter.findC3Spotter(mockGame, attacker, target);
        List<ECMInfo> precomputedEcmInfo = ComputeECM.computeAllEntitiesECMInfo(gameEntities);
        Entity cachedResult = ComputeC3Spotter.findC3Spotter(mockGame, attacker, target, precomputedEcmInfo);

        assertSame(onDemandResult, cachedResult);
    }
}
