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

package megamek.server.commands;

import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

import megamek.client.ui.Messages;
import megamek.common.planetaryConditions.Atmosphere;
import megamek.common.planetaryConditions.BlowingSand;
import megamek.common.planetaryConditions.EMI;
import megamek.common.planetaryConditions.Fog;
import megamek.common.planetaryConditions.Light;
import megamek.common.planetaryConditions.PlanetaryConditions;
import megamek.common.planetaryConditions.Weather;
import megamek.common.planetaryConditions.Wind;
import megamek.common.planetaryConditions.WindDirection;
import megamek.server.Server;
import megamek.server.commands.arguments.Argument;
import megamek.server.commands.arguments.Arguments;
import megamek.server.commands.arguments.OptionalEnumArgument;
import megamek.server.totalwarfare.TWGameManager;

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
        super(server, gameManager, WEATHER, Messages.getString("Gamemaster.cmd.changeweather.help"),
              Messages.getString("Gamemaster.cmd.changeweather.longName"));
    }

    @Override
    public List<Argument<?>> defineArguments() {
        return List.of(new OptionalEnumArgument<>(FOG,
                    Messages.getString("Gamemaster.cmd.changeweather.fog"),
                    Fog.class),
              new OptionalEnumArgument<>(LIGHT, Messages.getString("Gamemaster.cmd.changeweather.light"), Light.class),
              new OptionalEnumArgument<>(WIND, Messages.getString("Gamemaster.cmd.changeweather.wind"), Wind.class),
              new OptionalEnumArgument<>(WIND_DIR,
                    Messages.getString("Gamemaster.cmd.changeweather.winddir"),
                    WindDirection.class),
              new OptionalEnumArgument<>(ATMO,
                    Messages.getString("Gamemaster.cmd.changeweather.atmo"),
                    Atmosphere.class),
              new OptionalEnumArgument<>(BLOWSAND,
                    Messages.getString("Gamemaster.cmd.changeweather.blowsand"),
                    BlowingSand.class),
              new OptionalEnumArgument<>(EMIS, Messages.getString("Gamemaster.cmd.changeweather.emi"), EMI.class),
              new OptionalEnumArgument<>(WEATHER,
                    Messages.getString("Gamemaster.cmd.changeweather.weather"),
                    Weather.class));
    }

    private record Condition<E extends Enum<E>>(Consumer<E> setter, Function<E, String> successMessage) {
        @SuppressWarnings("unchecked")
        public void updatePlanetaryCondition(Enum<?> value, int connId, Server server) {
            setter.accept((E) value);
            server.sendServerChat(connId, successMessage.apply((E) value));
        }
    }

    /**
     * Run this command with the arguments supplied
     */
    @Override
    protected void runCommand(int connId, Arguments args) {
        if (getGameManager().getGame().getBoard().isSpace()) {
            server.sendServerChat(connId, "There is no planetary conditions to change outside of a planet");
            return;
        }
        var planetaryConditions = getGameManager().getGame().getPlanetaryConditions();
        var conditions = getStringConditionMap(planetaryConditions);
        conditions.forEach(updatePlanetaryConditions(connId, args));
        getGameManager().getGame().setPlanetaryConditions(planetaryConditions);
    }

    private BiConsumer<String, Condition<?>> updatePlanetaryConditions(int connId, Arguments args) {
        return (prefix, condition) -> {
            if (args.hasArg(prefix) && ((OptionalEnumArgument<?>) args.get(prefix)).isPresent()) {
                var value = ((OptionalEnumArgument<?>) args.get(prefix)).getValue();
                condition.updatePlanetaryCondition(value, connId, server);
            }
        };
    }

    private static Map<String, Condition<?>> getStringConditionMap(PlanetaryConditions planetaryConditions) {
        return Map.of(
              FOG,
              new Condition<>(planetaryConditions::setFog,
                    value -> Messages.getString("Gamemaster.cmd.changeweather.fog.success")),
              WIND,
              new Condition<>(planetaryConditions::setWind,
                    value -> Messages.getString("Gamemaster.cmd.changeweather.wind.success")),
              WIND_DIR,
              new Condition<>(planetaryConditions::setWindDirection,
                    value -> Messages.getString("Gamemaster.cmd.changeweather.winddir.success")),
              LIGHT,
              new Condition<>(planetaryConditions::setLight,
                    value -> Messages.getString("Gamemaster.cmd.changeweather.light.success")),
              ATMO,
              new Condition<>(planetaryConditions::setAtmosphere,
                    value -> value.equals(Atmosphere.VACUUM) ?
                          Messages.getString("Gamemaster.cmd.changeweather.atmo.success0") :
                          Messages.getString("Gamemaster.cmd.changeweather.atmo.success")),
              BLOWSAND,
              new Condition<>(planetaryConditions::setBlowingSand,
                    value -> value.equals(BlowingSand.BLOWING_SAND) ?
                          Messages.getString("Gamemaster.cmd.changeweather.blowsand.success1") :
                          Messages.getString("Gamemaster.cmd.changeweather.blowsand.success")),
              WEATHER,
              new Condition<>(planetaryConditions::setWeather,
                    value -> Messages.getString("Gamemaster.cmd.changeweather.weather.success")),
              EMIS,
              new Condition<>(planetaryConditions::setEMI,
                    value -> value.equals(EMI.EMI) ? Messages.getString("Gamemaster.cmd.changeweather.emi.success1") :
                          Messages.getString("Gamemaster.cmd.changeweather.emi.success"))
        );
    }
}
