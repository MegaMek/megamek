/*
 * MegaMek - Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 */
package megamek.server.commands;

import megamek.client.ui.Messages;
import megamek.common.Coords;
import megamek.server.Server;
import megamek.server.commands.arguments.Argument;
import megamek.server.commands.arguments.EnumArgument;
import megamek.server.totalwarfare.TWGameManager;

import java.util.List;
import java.util.Map;

/**
 * @author Luana Coppio
 */
public class DisasterCommand extends GamemasterServerCommand {

    public static final String TYPE = "type";

    enum Disaster {
        RANDOM,
        HURRICANE,
        LIGHTNING_STORM,
        ORBITAL_BOMBARDMENT,
        ORBITAL_BOMBARDMENT_2,
        ORBITAL_BOMBARDMENT_3,
        SANDSTORM,
        HAILSTORM,
        ICESTORM,
        ECLIPSE,
        SOLAR_FLARE,
        SUPERNOVA,
        SMOG,
        FIRESTORM,
        TRAITOR;

        public static Disaster getRandomDisaster() {
            return values()[(int) (Math.random() * values().length)];
        }
    }

    public DisasterCommand(Server server, TWGameManager gameManager) {
        super(server, gameManager, "disaster", Messages.getString("Gamemaster.cmd.disaster.help"),
            Messages.getString("Gamemaster.cmd.disaster.longName"));
    }

    @Override
    public List<Argument<?>> defineArguments() {
        return List.of(new EnumArgument<>(TYPE, Messages.getString("Gamemaster.cmd.disaster.type"), Disaster.class, Disaster.RANDOM));
    }

    private void runDisasterCommand(int connId, Disaster disaster) {
        switch (disaster) {
            case HURRICANE:
                new ChangeWeatherCommand(server, gameManager).run(connId, new String[]{"weather", "wind=6", "winddir=6"});
                server.sendServerChat("Hurricane incoming!");
                break;
            case LIGHTNING_STORM:
                new ChangeWeatherCommand(server, gameManager).run(connId, new String[]{"weather", "weather=14"});
                server.sendServerChat("Lightning storm incoming!");
                break;
            case ECLIPSE:
                new ChangeWeatherCommand(server, gameManager).run(connId, new String[]{"weather", "light=4"});
                server.sendServerChat("The sun is being eclipsed...");
                break;
            case SOLAR_FLARE:
                new ChangeWeatherCommand(server, gameManager).run(connId, new String[]{"weather", "light=5", "emi=1"});
                new FirestormCommand(server, gameManager).run(connId, new String[]{"firestorm", "1", "5"});
                server.sendServerChat("Sensors warn of an imminent solar flare incoming! Expect some fires.");
                break;
            case SUPERNOVA:
                new ChangeWeatherCommand(server, gameManager).run(connId, new String[]{"weather", "light=5", "emi=1", "atmo=2", "wind=0", "weather=0"});
                new FirestormCommand(server, gameManager).run(connId, new String[]{"firestorm", "2", "75"});
                server.sendServerChat("The star is going supernova!");
                server.sendServerChat("Everything is on fire! We are doomed!");
                break;
            case ORBITAL_BOMBARDMENT_3:
                orbitalBombardment(connId);
            case ORBITAL_BOMBARDMENT_2:
                orbitalBombardment(connId);
            case ORBITAL_BOMBARDMENT:
                orbitalBombardment(connId);
                break;
            case SANDSTORM:
                new ChangeWeatherCommand(server, gameManager).run(connId, new String[]{"weather", "blowsand=1", "wind=4", "winddir=6"});
                server.sendServerChat("A sandstorm is approaching!");
                break;
            case HAILSTORM:
                new ChangeWeatherCommand(server, gameManager).run(connId, new String[]{"weather", "weather=13", "wind=4", "winddir=6"});
                server.sendServerChat("A hailstorm is incoming!");
                break;
            case ICESTORM:
                new ChangeWeatherCommand(server, gameManager).run(connId, new String[]{"weather", "fog=1", "weather=11", "wind=6", "winddir=6"});
                server.sendServerChat("A hailstorm is incoming!");
                break;
            case FIRESTORM:
                new FirestormCommand(server, gameManager).run(connId, new String[]{"firestorm", "2", "50"});
                server.sendServerChat("A firestorm is consuming the battlefield!");
                break;
            case SMOG:
                new ChangeWeatherCommand(server, gameManager).run(connId, new String[]{"weather", "atmo=5", "fog=2", "light=1"});
                server.sendServerChat("A thick smog is covering the battlefield!");
                break;
            case TRAITOR:
            default:
                {
                    var players = gameManager.getGame().getPlayersList();
                    var randomPlayer = players.get((int) (Math.random() * players.size()));

                    var units = gameManager.getGame().getPlayerEntities(randomPlayer, true);
                    var randomUnit = units.get((int) (Math.random() * units.size()));

                    var otherPlayers = players.stream().filter(p -> p != randomPlayer).toList();
                    var newOwner = otherPlayers.get((int) (Math.random() * otherPlayers.size()));

                    new ChangeOwnershipCommand(server, gameManager).run(connId,
                        new String[]{"traitor", "" + randomUnit.getId(), "" + newOwner.getId()});
                    server.sendServerChat("A traitor has been revealed!");
                }
        }
    }

    private Coords getRandomHexCoords() {
        var board = gameManager.getGame().getBoard();
        var x = (int) (Math.random() * board.getWidth());
        var y = (int) (Math.random() * board.getHeight());
        return new Coords(x, y);
    }

    private void orbitalBombardment(int connId) {
        var coords = getRandomHexCoords();
        new OrbitalBombardmentCommand(server, gameManager).run(connId, new String[]{"ob",
            coords.getX() + 1 + "",
            coords.getY() + 1 + ""
        });
    }

    /**
     * Run this command with the arguments supplied
     */
    @Override
    protected void runAsGM(int connId, Map<String, Argument<?>> args) {
        if (args.get(TYPE).getValue().equals(Disaster.RANDOM)) {
            runDisasterCommand(connId, Disaster.getRandomDisaster());
        } else {
            runDisasterCommand(connId, (Disaster) args.get(TYPE).getValue());
        }
    }
}
