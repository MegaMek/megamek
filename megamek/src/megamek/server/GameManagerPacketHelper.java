/*
 * Copyright (C) 2024-2025 The MegaMek Team. All Rights Reserved.
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

package megamek.server;

import static megamek.common.net.enums.PacketCommand.PHASE_CHANGE;
import static megamek.common.net.enums.PacketCommand.ROUND_UPDATE;
import static megamek.common.net.enums.PacketCommand.SENDING_BOARD;
import static megamek.common.net.enums.PacketCommand.SENDING_GAME_SETTINGS;
import static megamek.common.net.enums.PacketCommand.SENDING_PLANETARY_CONDITIONS;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import megamek.common.game.IGame;
import megamek.common.interfaces.PlanetaryConditionsUsing;
import megamek.common.actions.EntityAction;
import megamek.common.net.enums.PacketCommand;
import megamek.common.net.packets.Packet;
import megamek.common.planetaryConditions.PlanetaryConditions;

/**
 * This is a helper class used by GameManagers (not Clients) to create packets to send to the Clients.
 */
public class GameManagerPacketHelper {

    private final AbstractGameManager gameManager;

    GameManagerPacketHelper(AbstractGameManager gameManager) {
        this.gameManager = gameManager;
    }

    /** @return A Packet containing information about a list of actions (not limited to Entity!). */
    public Packet createAttackPacket(List<? extends EntityAction> actions, boolean isChargeAttacks) {
        return new Packet(PacketCommand.ENTITY_ATTACK, actions, isChargeAttacks);
    }

    /** @return A Packet containing information about a single unit action (not limited to Entity!). */
    public Packet createChargeAttackPacket(EntityAction action) {
        // Redundant list construction because serialization doesn't like unmodifiable lists
        return createAttackPacket(new ArrayList<>(List.of(action)), true);
    }

    /** @return A Packet containing the player's current done status. */
    public Packet createPlayerDonePacket(int playerId) {
        return new Packet(PacketCommand.PLAYER_READY, playerId, game().getPlayer(playerId).isDone());
    }

    /** @return A Packet instructing the Client to set the round number to the GameManager's game's current round. */
    public Packet createCurrentRoundNumberPacket() {
        return new Packet(ROUND_UPDATE, game().getCurrentRound());
    }

    /** @return A Packet informing the Client of a phase change to the GameManager's game's current phase. */
    public Packet createPhaseChangePacket() {
        return new Packet(PHASE_CHANGE, game().getPhase());
    }

    /**
     * @return A Packet containing the game's planetary conditions, if it uses them, a newly created PlC otherwise.
     *       <p>
     *       This method avoids throwing an IllegalArgumentException if the game doesn't use PlC as, in that case, the
     *       sent packet is likely going to be ignored anyway and not cause the game to break.
     */
    public Packet createPlanetaryConditionsPacket() {
        return new Packet(SENDING_PLANETARY_CONDITIONS, game() instanceof PlanetaryConditionsUsing
              ? ((PlanetaryConditionsUsing) game()).getPlanetaryConditions() : new PlanetaryConditions());
    }

    /** @return A Packet containing the complete Map of boards and IDs to send from Server to Client. */
    public Packet createBoardsPacket() {
        // The new HashMap is created because getBoards() returns an unmodifiable view that
        // XStream cannot deserialize properly
        return new Packet(SENDING_BOARD, new HashMap<>(game().getBoards()));
    }

    /**
     * @return A packet containing the game settings. Note that this packet differs from the one sent by the Client in
     *       that the Client's packet will also contain a password
     */
    public Packet createGameSettingsPacket() {
        return new Packet(SENDING_GAME_SETTINGS, game().getOptions());
    }

    /**
     * @return A packet containing the current list of player turns.
     */
    public Packet createTurnListPacket() {
        return new Packet(PacketCommand.SENDING_TURNS, game().getTurnsList());
    }

    /**
     * @param previousPlayerId The ID of the player who triggered the turn change
     *
     * @return A packet containing the current player turn index. The ID of the previous player may be
     *       {@link megamek.common.Player#PLAYER_NONE}.
     */
    public Packet createTurnIndexPacket(int previousPlayerId) {
        return new Packet(PacketCommand.TURN, game().getTurnIndex(), previousPlayerId);
    }

    private IGame game() {
        return gameManager.getGame();
    }
}
