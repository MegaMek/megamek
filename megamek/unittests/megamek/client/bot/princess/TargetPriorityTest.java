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
package megamek.client.bot.princess;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import megamek.client.bot.AIType;
import megamek.common.Player;
import megamek.common.game.Game;
import megamek.common.net.enums.PacketCommand;
import megamek.common.net.marshalling.SanityInputFilter;
import megamek.common.net.packets.Packet;
import megamek.common.util.SerializationHelper;
import megamek.server.totalWarfare.TWGameManager;
import org.junit.jupiter.api.Test;

/**
 * Covers target priority on a bot's priority targets (#9174): how it is stored, that settings and saves made before it
 * existed still load and play as before, and that it survives the trip between bot, server and save file.
 */
class TargetPriorityTest {

    private static final int BOT_PLAYER_ID = 0;
    private static final String BOT_NAME = "Princess";
    private static final int PHOENIX_HAWK = 9;
    private static final int LOCUST = 11;

    private static BehaviorSettings settingsWith(int unitId, int priority) {
        BehaviorSettings settings = new BehaviorSettings();
        settings.setPriorityUnit(unitId, priority);
        return settings;
    }

    @Test
    void aUnitAddedWithoutAPriorityPlaysAtTheDefault() {
        BehaviorSettings settings = new BehaviorSettings();
        settings.addPriorityUnit(LOCUST);

        assertEquals(BehaviorSettings.DEFAULT_TARGET_PRIORITY, settings.getTargetPriority(LOCUST));
    }

    @Test
    void aChosenPriorityIsKept() {
        BehaviorSettings settings = settingsWith(PHOENIX_HAWK, 1);

        assertTrue(settings.getPriorityUnitTargets().contains(PHOENIX_HAWK));
        assertEquals(1, settings.getTargetPriority(PHOENIX_HAWK));
    }

    @Test
    void aPriorityOutsideOneToFiveIsPulledBackIntoRange() {
        BehaviorSettings settings = new BehaviorSettings();
        settings.setPriorityUnit(PHOENIX_HAWK, 0);
        settings.setPriorityUnit(LOCUST, 9);

        assertEquals(BehaviorSettings.HIGHEST_TARGET_PRIORITY, settings.getTargetPriority(PHOENIX_HAWK));
        assertEquals(BehaviorSettings.LOWEST_TARGET_PRIORITY, settings.getTargetPriority(LOCUST));
    }

    @Test
    void removingATargetForgetsItsPriority() {
        BehaviorSettings settings = settingsWith(PHOENIX_HAWK, 1);

        settings.removePriorityUnit(PHOENIX_HAWK);
        settings.addPriorityUnit(PHOENIX_HAWK);

        assertEquals(BehaviorSettings.DEFAULT_TARGET_PRIORITY, settings.getTargetPriority(PHOENIX_HAWK));
    }

    @Test
    void aCopyKeepsThePriorities() throws PrincessException {
        BehaviorSettings settings = settingsWith(PHOENIX_HAWK, 2);

        BehaviorSettings copy = settings.getCopy();

        assertEquals(2, copy.getTargetPriority(PHOENIX_HAWK));
        assertEquals(settings, copy);
    }

    @Test
    void settingsThatDifferOnlyInPriorityAreNotEqual() {
        assertNotEquals(settingsWith(PHOENIX_HAWK, 1), settingsWith(PHOENIX_HAWK, 2));
    }

    @Test
    void choosingTheDefaultEqualsNotChoosingAtAll() {
        BehaviorSettings chosen = settingsWith(PHOENIX_HAWK, BehaviorSettings.DEFAULT_TARGET_PRIORITY);
        BehaviorSettings added = new BehaviorSettings();
        added.addPriorityUnit(PHOENIX_HAWK);

        assertEquals(added, chosen);
        assertEquals(added.hashCode(), chosen.hashCode());
    }

    @Test
    void settingsLoadedFromBeforePrioritiesExistedPlayAtTheDefault() throws Exception {
        // Java serialization and XStream both skip field initialisers, so an old packet or save leaves the map null.
        BehaviorSettings old = new BehaviorSettings();
        old.addPriorityUnit(LOCUST);
        Field priorities = BehaviorSettings.class.getDeclaredField("targetPriorities");
        priorities.setAccessible(true);
        priorities.set(old, null);

        assertEquals(BehaviorSettings.DEFAULT_TARGET_PRIORITY, old.getTargetPriority(LOCUST));
        BehaviorSettings fresh = new BehaviorSettings();
        fresh.addPriorityUnit(LOCUST);
        assertEquals(fresh, old);
        old.setPriorityUnit(LOCUST, 1);
        assertEquals(1, old.getTargetPriority(LOCUST));
    }

    @Test
    void aPrioritySurvivesASaveAndLoad() {
        BehaviorSettings settings = settingsWith(PHOENIX_HAWK, 1);

        String savedXml = SerializationHelper.getSaveGameXStream().toXML(settings);
        BehaviorSettings restored = (BehaviorSettings) SerializationHelper.getLoadSaveGameXStream().fromXML(savedXml);

        assertEquals(1, restored.getTargetPriority(PHOENIX_HAWK));
    }

    @Test
    void aSaveMadeBeforePrioritiesExistedLoadsWithTheDefault() {
        BehaviorSettings settings = settingsWith(PHOENIX_HAWK, 1);
        String legacyXml = SerializationHelper.getSaveGameXStream().toXML(settings)
              .replaceAll("(?s)<targetPriorities[^>]*>.*?</targetPriorities>", "")
              .replaceAll("<targetPriorities[^>]*/>", "");
        assertFalse(legacyXml.contains("targetPriorities"), "the test must really remove the field");

        BehaviorSettings restored = (BehaviorSettings) SerializationHelper.getLoadSaveGameXStream().fromXML(legacyXml);

        assertTrue(restored.getPriorityUnitTargets().contains(PHOENIX_HAWK));
        assertEquals(BehaviorSettings.DEFAULT_TARGET_PRIORITY, restored.getTargetPriority(PHOENIX_HAWK));
    }

    /**
     * The server drops the connection of anyone who sends a class not on its allow list, so the settings packet is read
     * back through that same filter, in both directions it travels.
     */
    private static Packet throughTheNetworkFilter(Packet packet) throws IOException, ClassNotFoundException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream output = new ObjectOutputStream(bytes)) {
            output.writeObject(packet);
        }
        try (ObjectInputStream input = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
            input.setObjectInputFilter(new SanityInputFilter());
            return (Packet) input.readObject();
        }
    }

    @Test
    void theServerStoresThePriorityTheBotSends() throws Exception {
        Game game = new Game();
        game.addPlayer(BOT_PLAYER_ID, new Player(BOT_PLAYER_ID, BOT_NAME));
        TWGameManager gameManager = new TWGameManager();
        gameManager.setGame(game);
        Packet sent = new Packet(PacketCommand.PRINCESS_SETTINGS, settingsWith(PHOENIX_HAWK, 1), AIType.PRINCESS);

        gameManager.handlePacket(BOT_PLAYER_ID, throughTheNetworkFilter(sent));

        BehaviorSettings stored = game.getBotSettings().get(BOT_NAME);
        assertEquals(1, stored.getTargetPriority(PHOENIX_HAWK));
    }

    @Test
    void thePriorityReachesClientsInTheServerSettingsPacket() throws Exception {
        Map<String, BehaviorSettings> botSettings = new HashMap<>();
        botSettings.put(BOT_NAME, settingsWith(PHOENIX_HAWK, 4));
        Packet sent = new Packet(PacketCommand.PRINCESS_SETTINGS, botSettings, new HashMap<String, AIType>());

        Packet received = throughTheNetworkFilter(sent);

        @SuppressWarnings("unchecked")
        Map<String, BehaviorSettings> receivedSettings = (Map<String, BehaviorSettings>) received.getObject(0);
        assertEquals(4, receivedSettings.get(BOT_NAME).getTargetPriority(PHOENIX_HAWK));
    }
}
