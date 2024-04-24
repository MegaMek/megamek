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
package megamek.client;

import megamek.MMConstants;
import megamek.client.bot.princess.BehaviorSettings;
import megamek.client.bot.princess.Princess;
import megamek.common.*;
import megamek.common.actions.ArtilleryAttackAction;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.enums.GamePhase;
import megamek.common.event.*;
import megamek.common.net.enums.PacketCommand;
import megamek.common.net.packets.Packet;
import megamek.common.options.GameOptions;
import megamek.common.planetaryconditions.PlanetaryConditions;
import megamek.common.strategicBattleSystems.SBFGame;
import megamek.server.SmokeCloud;
import org.apache.logging.log4j.LogManager;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.*;

/**
 * This is the game client for Strategic BattleForce games, as one would think.
 */
public class SBFClient extends AbstractClient {

    /**
     * The game object that holds all game information. This object is persistent, i.e. it is never replaced
     * by another game object sent from the server. Instead, the info in this game object is added to
     * or replaced as necessary. Therefore, other objects may keep a reference to this game object. Other
     * objects however, like the players or units, *will* be replaced by objects sent from the server.
     */
    private final SBFGame game = new SBFGame();

    /**
     * Construct a client which will try to connect. If the connection fails, it
     * will alert the player, free resources and hide the frame.
     *
     * @param name the player name for this client
     * @param host the hostname
     * @param port the host port
     */
    public SBFClient(String name, String host, int port) {
        super(name, host, port);
    }

    @Override
    public IGame getIGame() {
        return game;
    }

    @Override
    protected boolean handleGameSpecificPacket(Packet packet) throws Exception {
        return false;
    }
}
