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


import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * @author Luana Scoppio
 */
public class ChangeWeatherCommand extends ServerCommand {

    private final TWGameManager gameManager;

    private static final String HELP_TEXT = "GM changes (weather) planetary conditions. The parameters are optional and unordered " +
        "and the effects are applied at the beginning of the next turn. The square brackets means that argument is optional. " +
        "Usage format: /weather [fog=0-2] [wind=0-6] [winddir=0-6] [light=0-6] [atmo=0-5] [blowsand=0-1] [weather=0-14]  " +
        "light= 0: daylight, 1: dusk, 2: full moon, 3: glare, 4: moonless night, 5: solar flare, 6: pitch black  " +
        "fog= 0: none, 1: light, 2: heavy  " +
        "wind= 0: calm, 1: light gale, 2: moderate gale, 3: strong gale, 4: storm, 5: tornado F1-F3, 6: tornado F4  " +
        "winddir= 0: south, 1: southwest, 2: northwest, 3: north, 4: northeast, 5: southeast, 6: random  " +
        "atmo= 0: vacuum, 1: trace, 2: thin, 3: standard, 4: high, 5: very high  " +
        "blowsand= 0: no, 1: yes  " +
        "weather= 0: clear, 1: light rain, 2: moderate rain, 3: heavy rain, 4: gusting rain, 5: downpour, 6: light snow " +
        "7: moderate snow, 8: snow flurries, 9: heavy snow, 10: sleet, 11: ice storm, 12: light hail, 13: heavy hail " +
        "14: lightning storm";

    /** Creates new ChangeWeatherCommand */
    public ChangeWeatherCommand(Server server, TWGameManager gameManager) {
        super(server, "weather", HELP_TEXT);
        this.gameManager = gameManager;
    }

    private void updatePlanetaryCondition(String arg, String prefix, int connId, int maxLength, Consumer<Integer> setter,
                                          Function<Integer, String> successMessage, Function<Integer, String> errorMessage) {
        var value = Integer.parseInt(arg.substring(prefix.length()));
        if (value >= 0 && value < maxLength) {
            setter.accept(value);
            server.sendServerChat(connId, successMessage.apply(value));
        } else {
            server.sendServerChat(connId, errorMessage.apply(maxLength));
        }
    }

    private record Condition(int maxLength, Consumer<Integer> setter, Function<Integer, String> successMessage, Function<Integer, String> errorMessage) {}

    /**
     * Run this command with the arguments supplied
     */
    @Override
    public void run(int connId, String[] args) {
        if (!server.getPlayer(connId).getGameMaster()) {
            server.sendServerChat(connId, "You are not a Game Master.");
            return;
        }

        var planetaryConditions = gameManager.getGame().getPlanetaryConditions();

        if (args.length > 1) {

            Map<String, Condition> conditions = Map.of(
                "fog=", new Condition(Fog.values().length, value -> planetaryConditions.setFog(Fog.values()[value]),
                    value -> "The fog has changed.", maxLength -> "Invalid fog value. Must be between 0 and " + (maxLength - 1)),
                "wind=", new Condition(Wind.values().length, value -> planetaryConditions.setWind(Wind.values()[value]),
                    value -> "The wind strength has changed.", maxLength -> "Invalid wind value. Must be between 0 and " + (maxLength - 1)),
                "winddir=", new Condition(WindDirection.values().length, value -> planetaryConditions.setWindDirection(WindDirection.values()[value]),
                    value -> "The wind direction has changed.", maxLength -> "Invalid wind direction value. Must be between 0 and " + (maxLength - 1)),
                "light=", new Condition(Light.values().length, value -> planetaryConditions.setLight(Light.values()[value]),
                    value -> "The light has changed.", maxLength -> "Invalid light value. Must be between 0 and " + (maxLength - 1)),
                "atmo=", new Condition(Atmosphere.values().length, value -> planetaryConditions.setAtmosphere(Atmosphere.values()[value]),
                    value -> value == 0 ? "The air has vanished, put your vac suits!" : "The air is changing.", maxLength -> "Invalid atmosphere value. Must be between 0 and " + (maxLength - 1)),
                "blowsand=", new Condition(BlowingSand.values().length, value -> planetaryConditions.setBlowingSand(BlowingSand.values()[value]),
                    value -> value == 1 ? "Sand started blowing." : "The sand has settled.", maxLength -> "Invalid blowsand value. Must be between 0 and " + (maxLength - 1)),
                "weather=", new Condition(Weather.values().length, value -> planetaryConditions.setWeather(Weather.values()[value]),
                    value -> "The weather has changed.", maxLength -> "Invalid weather value. Must be between 0 and " + (maxLength - 1))
            );

            Stream.of(args)
                .forEach(arg -> conditions.forEach((prefix, condition) -> {
                    if (arg.startsWith(prefix)) {
                        updatePlanetaryCondition(arg, prefix, connId, condition.maxLength, condition.setter, condition.successMessage, condition.errorMessage);
                    }
                }));

            gameManager.getGame().setPlanetaryConditions(planetaryConditions);
        } else {
            // Error out; it's not a valid call.
            server.sendServerChat(connId, "weather command failed. " + HELP_TEXT);
        }
    }
}
