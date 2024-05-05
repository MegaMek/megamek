/*
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */
package megamek.server;

import megamek.common.IGame;
import megamek.common.PlanetaryConditionsUsing;
import megamek.common.actions.EntityAction;
import megamek.common.net.enums.PacketCommand;
import megamek.common.net.packets.Packet;
import megamek.common.planetaryconditions.PlanetaryConditions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static megamek.common.net.enums.PacketCommand.*;

/**
 * This is a helper class used by GameManagers (not Clients) to create packets to send to the Clients.
 */
class GameManagerPacketHelper {

    private final AbstractGameManager gameManager;

    GameManagerPacketHelper(AbstractGameManager gameManager) {
        this.gameManager = gameManager;
    }

    /** @return A Packet containing information about a list of actions (not limited to Entity!). */
    Packet createAttackPacket(List<? extends EntityAction> actions, boolean isChargeAttacks) {
        return new Packet(PacketCommand.ENTITY_ATTACK, actions, isChargeAttacks);
    }

    /** @return A Packet containing information about a single unit action (not limited to Entity!). */
    Packet createChargeAttackPacket(EntityAction action) {
        // Redundant list construction because serialization doesn't like unmodifiable lists
        return createAttackPacket(new ArrayList<>(List.of(action)), true);
    }

    /** @return A Packet containing the player's current done status. */
    Packet createPlayerDonePacket(int playerId) {
        return new Packet(PacketCommand.PLAYER_READY, playerId, game().getPlayer(playerId).isDone());
    }

    /** @return A Packet instructing the Client to set the round number to the GameManager's game's current round. */
    Packet createCurrentRoundNumberPacket() {
        return new Packet(ROUND_UPDATE, game().getCurrentRound());
    }

    /** @return A Packet informing the Client of a phase change to the GameManager's game's current phase. */
    Packet createPhaseChangePacket() {
        return new Packet(PHASE_CHANGE, game().getPhase());
    }

    /**
     * @return A Packet containing the game's planetary conditions, if it uses them, a newly created PlC otherwise.
     *
     * This method avoids throwing an IllegalArgumentException if the game doesn't use PlC as, in that case, the
     * sent packet is likely going to be ignored anyway and not cause the game to break.
     */
    Packet createPlanetaryConditionsPacket() {
        return new Packet(SENDING_PLANETARY_CONDITIONS, game() instanceof PlanetaryConditionsUsing
                ? ((PlanetaryConditionsUsing) game()).getPlanetaryConditions() : new PlanetaryConditions());
    }

    /** @return A Packet containing the complete Map of boards and IDs to send from Server to Client. */
    Packet createBoardsPacket() {
        // The new HashMap is created because getBoards() returns an unmodifiable view that
        // XStream cannot deserialize properly
        return new Packet(SENDING_BOARD, new HashMap<>(game().getBoards()));
    }

    /**
     * @return A packet containing the game settings. Note that this packet differs from the one sent by the
     * Client in that the Client's packet will also contain a password
     */
    Packet createGameSettingsPacket() {
        return new Packet(SENDING_GAME_SETTINGS, game().getOptions());
    }

    /**
     * @return A packet containing the current list of player turns.
     */
    Packet createTurnVectorPacket() {
        return new Packet(PacketCommand.SENDING_TURNS, game().getTurnsList());
    }

    /**
     * @return A packet containing the current player turn index. The ID of the previous player may be
     * {@link megamek.common.Player#PLAYER_NONE}.
     *
     * @param previousPlayerId The ID of the player who triggered the turn change
     */
    Packet createTurnIndexPacket(int previousPlayerId) {
        return new Packet(PacketCommand.TURN, game().getTurnIndex(), previousPlayerId);
    }

    private IGame game() {
        return gameManager.getGame();
    }
}
