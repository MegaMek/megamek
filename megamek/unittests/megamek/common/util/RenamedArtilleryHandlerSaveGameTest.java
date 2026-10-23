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

package megamek.common.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.stream.Stream;

import com.thoughtworks.xstream.core.JVM;
import megamek.common.weapons.handlers.artillery.ArtilleryBayWeaponCloseFireHandler;
import megamek.common.weapons.handlers.artillery.ArtilleryBayWeaponCloseHomingHandler;
import megamek.common.weapons.handlers.artillery.ArtilleryBayWeaponDistantFireHandler;
import megamek.common.weapons.handlers.artillery.ArtilleryBayWeaponDistantHomingHandler;
import megamek.common.weapons.handlers.artillery.ArtilleryWeaponCloseFireHandler;
import megamek.common.weapons.handlers.artillery.ArtilleryWeaponCloseHomingHandler;
import megamek.common.weapons.handlers.artillery.ArtilleryWeaponDistantFireHandler;
import megamek.common.weapons.handlers.artillery.ArtilleryWeaponDistantHomingHandler;
import megamek.server.Server;
import megamek.server.totalWarfare.TWGameManager;
import megamek.utils.ServerFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Tests that a game saved before the artillery handlers were renamed from Direct / Indirect to Close / Distant
 * (0.51.01) still loads. Artillery in flight is saved as its handler, so such a save names the old handler class.
 */
class RenamedArtilleryHandlerSaveGameTest {

    private static final String HANDLER_PACKAGE = "megamek.common.weapons.handlers.artillery.";

    private static Server server;

    @BeforeAll
    static void startServer() throws IOException {
        // WeaponHandler.readObject takes its game manager from the running server
        server = ServerFactory.createServer(new TWGameManager());
    }

    @AfterAll
    static void stopServer() {
        server.die();
    }

    static Stream<Arguments> renamedHandlers() {
        return Stream.of(
              Arguments.of("ArtilleryWeaponDirectFireHandler", ArtilleryWeaponCloseFireHandler.class),
              Arguments.of("ArtilleryWeaponDirectHomingHandler", ArtilleryWeaponCloseHomingHandler.class),
              Arguments.of("ArtilleryWeaponIndirectFireHandler", ArtilleryWeaponDistantFireHandler.class),
              Arguments.of("ArtilleryWeaponIndirectHomingHandler", ArtilleryWeaponDistantHomingHandler.class),
              Arguments.of("ArtilleryBayWeaponDirectFireHandler", ArtilleryBayWeaponCloseFireHandler.class),
              Arguments.of("ArtilleryBayWeaponDirectHomingHandler", ArtilleryBayWeaponCloseHomingHandler.class),
              Arguments.of("ArtilleryBayWeaponIndirectFireHandler", ArtilleryBayWeaponDistantFireHandler.class),
              Arguments.of("ArtilleryBayWeaponIndirectHomingHandler", ArtilleryBayWeaponDistantHomingHandler.class));
    }

    @ParameterizedTest(name = "{0} loads as {1}")
    @MethodSource("renamedHandlers")
    void saveWithOldHandlerNameLoadsRenamedHandler(String oldSimpleName, Class<?> renamedClass) {
        // The handler's state does not matter here, only its class name, so it is built without its constructor
        Object handler = JVM.newReflectionProvider().newInstance(renamedClass);
        String currentSave = SerializationHelper.getSaveGameXStream().toXML(handler);
        assertFalse(currentSave.contains(HANDLER_PACKAGE + oldSimpleName),
              "A current save should not write the old class name: " + currentSave);

        // Rewrite the save as 0.51.00 wrote it: the handler under its old class name
        String olderSave = currentSave.replace(renamedClass.getName(), HANDLER_PACKAGE + oldSimpleName);
        assertTrue(olderSave.contains(HANDLER_PACKAGE + oldSimpleName),
              "The test save should name the old class: " + olderSave);

        Object loaded = SerializationHelper.getLoadSaveGameXStream().fromXML(olderSave);

        assertSame(renamedClass, loaded.getClass(), "The old handler name should load as the renamed handler");
    }
}
