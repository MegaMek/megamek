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
package megamek.server.totalWarfare;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import megamek.common.Player;
import megamek.common.enums.GamePhase;
import megamek.common.equipment.EquipmentType;
import megamek.common.game.Game;
import megamek.common.net.enums.PacketCommand;
import megamek.common.net.packets.Packet;
import megamek.common.units.DamageEditSpec;
import megamek.common.units.Entity;
import megamek.common.units.Mek;
import megamek.testUtilities.MMTestUtilities;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for the server's handling of a damage edit packet: only a gamemaster's edits are applied, they land on the
 * server's own copy of the unit, a fatal edit destroys the unit, and server-side state the edit does not carry -
 * such as a pending traitor switch - survives the edit.
 */
class TWGameManagerDamageEditTest {

    private static final int UNIT_ID = 5;
    private static final int OWNER_CONNECTION_ID = 0;
    private static final int GAME_MASTER_CONNECTION_ID = 1;
    private static final int TRAITOR_TARGET_PLAYER_ID = 7;

    private TWGameManager gameManager;
    private Game game;
    private Entity mek;

    @BeforeAll
    static void initializeEquipment() {
        EquipmentType.initializeTypes();
    }

    @BeforeEach
    void setUp() {
        game = new Game();

        Player owner = new Player(OWNER_CONNECTION_ID, "Owner");
        owner.setTeam(1);
        game.addPlayer(OWNER_CONNECTION_ID, owner);

        Player gameMaster = new Player(GAME_MASTER_CONNECTION_ID, "GameMaster");
        gameMaster.setTeam(2);
        gameMaster.setGameMaster(true);
        game.addPlayer(GAME_MASTER_CONNECTION_ID, gameMaster);

        mek = MMTestUtilities.getEntityForUnitTesting("Enforcer III ENF-6M", false);
        assertNotNull(mek, "Test unit could not be loaded");
        mek.setId(UNIT_ID);
        mek.setOwner(owner);
        game.addEntity(mek);

        game.setPhase(GamePhase.MOVEMENT);

        gameManager = mock(TWGameManager.class);
        doCallRealMethod().when(gameManager).setGame(any());
        doCallRealMethod().when(gameManager).handlePacket(anyInt(), any());
        gameManager.setGame(game);
    }

    /** A spec for the test unit with location arrays sized and everything else absent. */
    private DamageEditSpec emptySpec() {
        DamageEditSpec spec = new DamageEditSpec();
        spec.entityId = UNIT_ID;
        spec.internal = new Integer[mek.locations()];
        spec.armor = new Integer[mek.locations()];
        spec.rearArmor = new Integer[mek.locations()];
        return spec;
    }

    private void sendDamageEdit(int connectionId, DamageEditSpec spec) {
        gameManager.handlePacket(connectionId, new Packet(PacketCommand.ENTITY_DAMAGE_EDIT, spec));
    }

    @Test
    void gameMasterEditLandsOnTheServerUnit() {
        DamageEditSpec spec = emptySpec();
        spec.heat = 10;

        sendDamageEdit(GAME_MASTER_CONNECTION_ID, spec);

        assertEquals(10, mek.heat);
    }

    @Test
    void nonGameMasterEditIsDropped() {
        DamageEditSpec spec = emptySpec();
        spec.heat = 10;

        sendDamageEdit(OWNER_CONNECTION_ID, spec);

        assertEquals(0, mek.heat, "An edit from a player who is not the gamemaster must not be applied");
    }

    @Test
    void fatalEditDestroysTheUnit() {
        DamageEditSpec spec = emptySpec();
        spec.internal[Mek.LOC_CENTER_TORSO] = 0;

        sendDamageEdit(GAME_MASTER_CONNECTION_ID, spec);

        verify(gameManager).destroyEntity(mek, "damage", true);
    }

    @Test
    void pendingTraitorSwitchSurvivesADamageEdit() {
        mek.setTraitorId(TRAITOR_TARGET_PLAYER_ID);
        DamageEditSpec spec = emptySpec();
        spec.heat = 5;

        sendDamageEdit(GAME_MASTER_CONNECTION_ID, spec);

        assertEquals(TRAITOR_TARGET_PLAYER_ID, mek.getTraitorId(),
              "The edit carries no ownership change, so the pending switch must survive it");
        assertEquals(5, mek.heat);
    }
}
