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
package megamek.server.commands;

import megamek.common.planetaryconditions.*;
import megamek.server.Server;
import megamek.server.totalwarfare.TWGameManager;

/**
 * @author Luana Scoppio
 */
public class ChangeWeatherCommand extends ServerCommand {

    private final TWGameManager gameManager;

    /** Creates new ChangeWeatherCommand */
    public ChangeWeatherCommand(Server server, TWGameManager gameManager) {
        super(server, "weather", "GM changes (weather) planetary conditions. The parameters are optional and unordered " +
            "and the effects are applied at the beggining of the next turn. The square brackets means that argument is optional. " +
            "Usage format: /weather [fog=0-2] [wind=0-6] [winddir=0-6] [light=0-6] [atmo=0-5] [blowsand=0-1] [weather=0-14]  " +
            "light= 0: daylight, 1: dusk, 2: full moon, 3: glare, 4: moonless night, 5: solar flare, 6: pitch black  " +
            "fog= 0: none, 1: light, 2: heavy  " +
            "wind= 0: calm, 1: light gale, 2: moderate gale, 3: strong gale, 4: storm, 5: tornado F1-F3, 6: tornado F4  " +
            "winddir= 0: south, 1: southwest, 2: northwest, 3: north, 4: northeast, 5: southeast, 6: random  " +
            "atmo= 0: vacuum, 1: trace, 2: thin, 3: standard, 4: high, 5: very high  " +
            "blowsand= 0: no, 1: yes  " +
            "weather= 0: clear, 1: light rain, 2: moderate rain, 3: heavy rain, 4: gusting rain, 5: downpour, 6: light snow " +
            "7: moderate snow, 8: snow flurries, 9: heavy snow, 10: sleet, 11: ice storm, 12: light hail, 13: heavy hail " +
            "14: lightning storm");
        this.gameManager = gameManager;
    }

    /**
     * Run this command with the arguments supplied
     */
    @Override
    public void run(int connId, String[] args) {
        if (!server.getPlayer(connId).getGameMaster()) {
            server.sendServerChat(connId, "You are not a Game Master.");
            return;
        }

        // Check argument integrity.
        if (args.length > 1) {
            // Check command
            var planetaryConditions = gameManager.getGame().getPlanetaryConditions();

            for (var arg : args) {
                if (arg.startsWith("fog=")) {
                    var fog = Integer.parseInt(arg.substring(4));
                    if (fog >= 0 && fog < Fog.values().length) {
                        planetaryConditions.setFog(Fog.values()[fog]);
                        server.sendServerChat(connId, "The fog has changed.");
                    } else {
                        server.sendServerChat(connId, "Invalid fog value. Must be between 0 and " + Fog.values().length);
                    }
                } else if (arg.startsWith("wind=")) {
                    var wind = Integer.parseInt(arg.substring(5));
                    if (wind >= 0 && wind < Wind.values().length) {
                        planetaryConditions.setWind(Wind.values()[wind]);
                        server.sendServerChat(connId, "The wind strength has changed.");
                    } else {
                        server.sendServerChat(connId, "Invalid wind value. Must be between 0 and " + Wind.values().length);
                    }
                } else if (arg.startsWith("winddir=")) {
                    var windDir = Integer.parseInt(arg.substring(8));
                    if (windDir >= 0 && windDir < WindDirection.values().length) {
                        planetaryConditions.setWindDirection(WindDirection.values()[windDir]);
                        server.sendServerChat(connId, "The wind direction has changed.");
                    } else {
                        server.sendServerChat(connId, "Invalid wind direction value. Must be between 0 and " + WindDirection.values().length);
                    }
                } else if (arg.startsWith("light=")) {
                    var light = Integer.parseInt(arg.substring(6));
                    if (light >= 0 && light < Light.values().length) {
                        planetaryConditions.setLight(Light.values()[light]);
                        server.sendServerChat(connId, "The light has changed.");
                    } else {
                        server.sendServerChat(connId, "Invalid light value. Must be between 0 and " + Light.values().length);
                    }
                } else if (arg.startsWith("atmo=")) {
                    var atmo = Integer.parseInt(arg.substring(5));
                    if (atmo >= 0 && atmo < Atmosphere.values().length) {
                        planetaryConditions.setAtmosphere(Atmosphere.values()[atmo]);
                        server.sendServerChat(connId, atmo == 0 ? "The air has vanished, put your vac suits!" : "The air is changing.");
                    } else {
                        server.sendServerChat(connId, "Invalid atmosphere value. Must be between 0 and " + Atmosphere.values().length);
                    }
                } else if (arg.startsWith("blowsand=")) {
                    var blowSand = Integer.parseInt(arg.substring(9));
                    if (blowSand >= 0 && blowSand < BlowingSand.values().length) {
                        planetaryConditions.setBlowingSand(BlowingSand.values()[blowSand]);
                        server.sendServerChat(connId, blowSand == 1 ? "Sand started blowing." : "The sand has settled.");
                    } else {
                        server.sendServerChat(connId, "Invalid blowsand value. Must be between 0 and " + BlowingSand.values().length);
                    }
                } else if (arg.startsWith("weather=")) {
                    var weather = Integer.parseInt(arg.substring(8));
                    if (weather >= 0 && weather < Weather.values().length) {
                        planetaryConditions.setWeather(Weather.values()[weather]);
                        server.sendServerChat(connId, "The weather has changed.");
                    } else {
                        server.sendServerChat(connId, "Invalid weather value. Must be between 0 and " + Weather.values().length);
                    }
                }
            }

            gameManager.getGame().setPlanetaryConditions(planetaryConditions);
        } else {
            // Error out; it's not a valid call.
            server.sendServerChat(connId, "weather command failed.");
        }
    }
}
