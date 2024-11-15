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
import megamek.common.planetaryconditions.*;
import megamek.server.Server;
import megamek.server.commands.arguments.Argument;
import megamek.server.commands.arguments.OptionalIntegerArgument;
import megamek.server.totalwarfare.TWGameManager;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * @author Luana Coppio
 */
public class ChangeWeatherCommand extends GamemasterServerCommand {

    private static final String FOG = "fog";
    private static final String LIGHT = "light";
    private static final String WIND = "wind";
    private static final String WIND_DIR = "winddir";
    private static final String ATMO = "atmo";
    private static final String BLOWSAND = "blowsand";
    private static final String EMIS = "emi";
    private static final String WEATHER = "weather";

    /** Creates new ChangeWeatherCommand */
    public ChangeWeatherCommand(Server server, TWGameManager gameManager) {
        super(server, gameManager, WEATHER, Messages.getString("Gamemaster.cmd.changeweather.help"));
    }

    @Override
    public List<Argument<?>> defineArguments() {
        return List.of(new OptionalIntegerArgument(FOG, Messages.getString("Gamemaster.cmd.changeweather.fog"), 0, 2),
            new OptionalIntegerArgument(LIGHT, Messages.getString("Gamemaster.cmd.changeweather.light"), 0, 6),
            new OptionalIntegerArgument(WIND, Messages.getString("Gamemaster.cmd.changeweather.wind"), 0, 6),
            new OptionalIntegerArgument(WIND_DIR, Messages.getString("Gamemaster.cmd.changeweather.winddir"), 0, 6),
            new OptionalIntegerArgument(ATMO, Messages.getString("Gamemaster.cmd.changeweather.atmo"), 0, 5),
            new OptionalIntegerArgument(BLOWSAND, Messages.getString("Gamemaster.cmd.changeweather.blowsand"), 0, 1),
            new OptionalIntegerArgument(EMIS, Messages.getString("Gamemaster.cmd.changeweather.emi"), 0, 1),
            new OptionalIntegerArgument(WEATHER, Messages.getString("Gamemaster.cmd.changeweather.weather"), 0, 14));
    }

    private void updatePlanetaryCondition(int value, int connId, int maxLength, Consumer<Integer> setter,
                                          Function<Integer, String> successMessage, Function<Integer, String> errorMessage) {
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
    public void runAsGM(int connId, Map<String, Argument<?>> args) {
        var planetaryConditions = getGameManager().getGame().getPlanetaryConditions();

        Map<String, Condition> conditions = Map.of(
            FOG, new Condition(Fog.values().length, value -> planetaryConditions.setFog(Fog.values()[value]),
                value -> Messages.getString("Gamemaster.cmd.changeweather.fog.success"),
                maxLength -> Messages.getString("Gamemaster.cmd.changeweather.fog.error", (maxLength - 1))),
            WIND, new Condition(Wind.values().length, value -> planetaryConditions.setWind(Wind.values()[value]),
                value -> Messages.getString("Gamemaster.cmd.changeweather.wind.success"),
                maxLength -> Messages.getString("Gamemaster.cmd.changeweather.wind.error", (maxLength - 1))),
            WIND_DIR, new Condition(WindDirection.values().length, value -> planetaryConditions.setWindDirection(WindDirection.values()[value]),
                value -> Messages.getString("Gamemaster.cmd.changeweather.winddir.success"),
                maxLength -> Messages.getString("Gamemaster.cmd.changeweather.winddir.error", (maxLength - 1))),
            LIGHT, new Condition(Light.values().length, value -> planetaryConditions.setLight(Light.values()[value]),
                value -> Messages.getString("Gamemaster.cmd.changeweather.light.success"),
                maxLength -> Messages.getString("Gamemaster.cmd.changeweather.light.error", (maxLength - 1))),
            ATMO, new Condition(Atmosphere.values().length, value -> planetaryConditions.setAtmosphere(Atmosphere.values()[value]),
                value -> value == 0 ? Messages.getString("Gamemaster.cmd.changeweather.atmo.success0") : Messages.getString("Gamemaster.cmd.changeweather.atmo.success"),
                maxLength -> Messages.getString("Gamemaster.cmd.changeweather.atmo.error", (maxLength - 1))),
            BLOWSAND, new Condition(BlowingSand.values().length, value -> planetaryConditions.setBlowingSand(BlowingSand.values()[value]),
                value -> value == 1 ? Messages.getString("Gamemaster.cmd.changeweather.blowsand.success1") : Messages.getString("Gamemaster.cmd.changeweather.blowsand.success"),
                maxLength -> Messages.getString("Gamemaster.cmd.changeweather.blowsand.error", (maxLength - 1))),
            WEATHER, new Condition(Weather.values().length, value -> planetaryConditions.setWeather(Weather.values()[value]),
                value -> Messages.getString("Gamemaster.cmd.changeweather.weather.success"),
                maxLength -> Messages.getString("Gamemaster.cmd.changeweather.weather.error", (maxLength - 1))),
            EMIS, new Condition(EMI.values().length, value -> planetaryConditions.setEMI(EMI.values()[value]),
                value -> value == 1 ? Messages.getString("Gamemaster.cmd.changeweather.emi.success1") : Messages.getString("Gamemaster.cmd.changeweather.emi.success"),
                maxLength -> Messages.getString("Gamemaster.cmd.changeweather.emi.error", (maxLength - 1)))
        );

        conditions.forEach((prefix, condition) -> {
            if (args.containsKey(prefix) && ((OptionalIntegerArgument) args.get(prefix)).getValue().isPresent()) {
                updatePlanetaryCondition(
                    ((OptionalIntegerArgument) args.get(prefix)).getValue().get(),
                    connId,
                    condition.maxLength,
                    condition.setter,
                    condition.successMessage,
                    condition.errorMessage);
            }
        });

        getGameManager().getGame().setPlanetaryConditions(planetaryConditions);
    }
}
