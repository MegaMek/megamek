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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Vector;

import megamek.common.Player;
import megamek.common.actions.ArtilleryAttackAction;
import megamek.common.game.Game;
import megamek.common.net.packets.Packet;
import megamek.common.options.OptionsConstants;
import megamek.common.weapons.handlers.WeaponHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Regression tests for issue #8685: a game could not be loaded once an artillery round outlived the player who fired
 * it.
 *
 * <p>A round already in the air lands whether or not the firing unit survives, so it stays in the attack list. When
 * that unit was its owner's last one, the player can be dropped from the game while the round is still in flight,
 * leaving an attack stamped with a player id that no longer resolves. Every connecting client asked the server which
 * rounds were friendly, that lookup returned {@code null}, and the connection was closed - so every player was kicked
 * off and the game could not advance.</p>
 */
class TWGameManagerArtilleryPacketTest {

    private static final int VIEWING_PLAYER_ID = 0;
    private static final int DEPARTED_PLAYER_ID = 2;
    private static final int ALLY_PLAYER_ID = 1;
    private static final int SHARED_TEAM = 1;

    private TWGameManager gameManager;
    private Game game;
    private Player viewingPlayer;

    @BeforeEach
    void setUp() {
        gameManager = new TWGameManager();
        game = gameManager.getGame();
        viewingPlayer = new Player(VIEWING_PLAYER_ID, "Dust Devils");
        viewingPlayer.setTeam(SHARED_TEAM);
        game.addPlayer(VIEWING_PLAYER_ID, viewingPlayer);
        game.initializeRulesManager(OptionsConstants.RULES_CORE);
    }

    /** Adds an in-flight artillery attack fired by the given player id, as a loaded save would hold it. */
    private void addInFlightArtilleryAttack(int firingPlayerId) {
        ArtilleryAttackAction attack = mock(ArtilleryAttackAction.class);
        when(attack.getPlayerId()).thenReturn(firingPlayerId);
        WeaponHandler handler = mock(WeaponHandler.class);
        handler.weaponAttackAction = attack;
        game.addAttack(handler);
    }

    @Test
    void artilleryPacketIsBuiltWhenTheFiringPlayerHasLeftTheGame() {
        addInFlightArtilleryAttack(DEPARTED_PLAYER_ID);

        Packet packet = assertDoesNotThrow(() -> gameManager.createArtilleryPacket(viewingPlayer));

        assertNotNull(packet);
    }

    @Test
    void aRoundFromADepartedPlayerIsNotTreatedAsFriendly() {
        addInFlightArtilleryAttack(DEPARTED_PLAYER_ID);

        Packet packet = gameManager.createArtilleryPacket(viewingPlayer);

        // A firer that cannot be resolved has no team, so the round must not be sent as one of the viewer's own.
        assertTrue(friendlyAttacksIn(packet).isEmpty());
    }

    @Test
    void aRoundFromASurvivingTeammateIsStillSentInFull() {
        Player ally = new Player(ALLY_PLAYER_ID, "Federated Commonwealth Alliance Additional Force");
        ally.setTeam(SHARED_TEAM);
        game.addPlayer(ALLY_PLAYER_ID, ally);
        addInFlightArtilleryAttack(ALLY_PLAYER_ID);

        Packet packet = gameManager.createArtilleryPacket(viewingPlayer);

        assertEquals(1, friendlyAttacksIn(packet).size());
    }

    @SuppressWarnings("unchecked")
    private Vector<ArtilleryAttackAction> friendlyAttacksIn(Packet packet) {
        return (Vector<ArtilleryAttackAction>) packet.getObject(0);
    }
}
