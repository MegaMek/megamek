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
package megamek.client.bot;

import java.util.Objects;

import megamek.client.bot.caspar.Caspar;
import megamek.client.bot.princess.BehaviorSettings;
import megamek.client.bot.princess.Princess;
import megamek.logging.MMLogger;

/**
 * The single construction point for bot clients. Instead of scattering {@code new Princess(...)} calls across the
 * lobby, scenario loaders and headless runners, callers ask this factory for a bot by {@link AIType}. This keeps the
 * choice of AI in one place (so a new AI type only has to be wired here and offered in the selection surfaces) and
 * returns the abstract {@link BotClient} type, so callers do not depend on a concrete bot class.
 *
 * <p>The returned bot has had its background processing started (equivalent to the previous
 * {@code new Princess(...); startPrecognition();} pairing), so callers only need to connect it and, if not using the
 * behavior-settings overload, apply its behavior.</p>
 */
public final class BotFactory {
    private static final MMLogger LOGGER = MMLogger.create(BotFactory.class);

    private BotFactory() {
    }

    /**
     * Creates and starts a bot of the given type, leaving its behavior settings at the default.
     *
     * @param aiType the AI implementation to build
     * @param name   the bot player's display name
     * @param host   the server host to connect to
     * @param port   the server port to connect to
     *
     * @return a started {@link BotClient} of the requested type, not yet connected
     */
    public static BotClient createBot(AIType aiType, String name, String host, int port) {
        Objects.requireNonNull(aiType, "aiType");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(host, "host");
        LOGGER.debug("Creating {} bot '{}' for {}:{}", aiType, name, host, port);
        return switch (aiType) {
            case PRINCESS -> {
                Princess princess = new Princess(name, host, port);
                princess.startPrecognition();
                yield princess;
            }
            case CASPAR -> {
                Caspar caspar = new Caspar(name, host, port);
                caspar.startPrecognition();
                yield caspar;
            }
        };
    }

    /**
     * Creates and starts a bot of the given type and applies the given behavior settings.
     *
     * @param aiType           the AI implementation to build
     * @param name             the bot player's display name
     * @param host             the server host to connect to
     * @param port             the server port to connect to
     * @param behaviorSettings the behavior settings to apply to the new bot
     *
     * @return a started {@link BotClient} of the requested type, not yet connected
     */
    public static BotClient createBot(AIType aiType, String name, String host, int port,
          BehaviorSettings behaviorSettings) {
        Objects.requireNonNull(behaviorSettings, "behaviorSettings");
        BotClient bot = createBot(aiType, name, host, port);
        bot.setBehaviorSettings(behaviorSettings);
        return bot;
    }
}
