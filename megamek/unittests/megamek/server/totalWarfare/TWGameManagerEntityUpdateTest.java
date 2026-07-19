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

import static megamek.testUtilities.MMTestUtilities.getEntityForUnitTesting;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import megamek.common.Player;
import megamek.common.enums.GamePhase;
import megamek.common.game.Game;
import megamek.common.net.enums.PacketCommand;
import megamek.common.net.packets.Packet;
import megamek.common.units.Entity;
import megamek.common.units.Mek;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Regression tests for {@link TWGameManager}'s handling of {@code ENTITY_UPDATE} packets (issue #8479).
 *
 * <p>The server must accept an entity update from the unit's owner or a teammate, reject it from an enemy player,
 * and accept it from any gamemaster regardless of team. Before the fix, gamemaster edits (Edit Damage / Configure) were
 * silently dropped and reverted on the next server broadcast.</p>
 */
class TWGameManagerEntityUpdateTest {

    private static final int UNIT_ID = 5;
    private static final int OWNER_CONNECTION_ID = 0;
    private static final int ENEMY_CONNECTION_ID = 1;
    private static final int GM_CONNECTION_ID = 2;
    private static final int ORIGINAL_ARMOR = 15;
    private static final int EDITED_ARMOR = 3;

    private Game game;
    private TWGameManager gameManager;

    @BeforeEach
    void setUp() {
        Player owner = new Player(OWNER_CONNECTION_ID, "Owner");
        owner.setTeam(1);
        Player enemy = new Player(ENEMY_CONNECTION_ID, "Enemy");
        enemy.setTeam(2);
        Player gamemaster = new Player(GM_CONNECTION_ID, "Referee");
        gamemaster.setTeam(Player.TEAM_NONE);
        gamemaster.setGameMaster(true);

        game = new Game();
        game.setPhase(GamePhase.MOVEMENT);
        game.addPlayer(OWNER_CONNECTION_ID, owner);
        game.addPlayer(ENEMY_CONNECTION_ID, enemy);
        game.addPlayer(GM_CONNECTION_ID, gamemaster);

        Entity atlas = loadAtlas();
        atlas.setOwner(owner);
        atlas.setArmor(ORIGINAL_ARMOR, Mek.LOC_LEFT_TORSO);
        game.addEntity(atlas);

        gameManager = mock(TWGameManager.class);
        doNothing().when(gameManager).entityUpdate(anyInt());
        when(gameManager.getGame()).thenReturn(game);
        doCallRealMethod().when(gameManager).setGame(any(Game.class));
        doCallRealMethod().when(gameManager).handlePacket(anyInt(), any(Packet.class));
        gameManager.setGame(game);
    }

    private Entity loadAtlas() {
        Entity entity = getEntityForUnitTesting("Atlas AS7-D", false);
        assertNotNull(entity, "Atlas AS7-D not found");
        entity.setId(UNIT_ID);
        return entity;
    }

    /** Builds a copy of the unit with edited armor, as a client-side damage edit would produce. */
    private Entity editedAtlas(Player owner) {
        Entity edited = loadAtlas();
        edited.setOwner(owner);
        edited.setArmor(EDITED_ARMOR, Mek.LOC_LEFT_TORSO);
        return edited;
    }

    private void sendUpdate(int connectionId, Entity edited) {
        gameManager.handlePacket(connectionId, new Packet(PacketCommand.ENTITY_UPDATE, edited));
    }

    private int armorOnServer() {
        Entity serverEntity = game.getEntity(UNIT_ID);
        assertNotNull(serverEntity);
        return serverEntity.getArmor(Mek.LOC_LEFT_TORSO);
    }

    @Test
    void ownerUpdateIsAccepted() {
        sendUpdate(OWNER_CONNECTION_ID, editedAtlas(game.getPlayer(OWNER_CONNECTION_ID)));
        assertEquals(EDITED_ARMOR, armorOnServer());
    }

    @Test
    void enemyNonGamemasterUpdateIsRejected() {
        sendUpdate(ENEMY_CONNECTION_ID, editedAtlas(game.getPlayer(OWNER_CONNECTION_ID)));
        assertEquals(ORIGINAL_ARMOR, armorOnServer());
    }

    @Test
    void noTeamGamemasterUpdateIsAccepted() {
        sendUpdate(GM_CONNECTION_ID, editedAtlas(game.getPlayer(OWNER_CONNECTION_ID)));
        assertEquals(EDITED_ARMOR, armorOnServer());
    }

    @Test
    void enemyTeamGamemasterUpdateIsAccepted() {
        Player gamemaster = game.getPlayer(GM_CONNECTION_ID);
        gamemaster.setTeam(2);
        sendUpdate(GM_CONNECTION_ID, editedAtlas(game.getPlayer(OWNER_CONNECTION_ID)));
        assertEquals(EDITED_ARMOR, armorOnServer());
    }
}
