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
package megamek.client.ui.dialogs.unitDisplay;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.awt.Component;
import java.awt.Container;
import java.awt.GraphicsEnvironment;

import megamek.client.Client;
import megamek.client.ui.clientGUI.ClientGUI;
import megamek.client.ui.clientGUI.GUIPreferences;
import megamek.client.ui.dialogs.unitDisplay.UnitDisplayPanel.ControlFocus;
import megamek.common.Player;
import megamek.common.game.Game;
import megamek.common.units.BipedMek;
import megamek.common.units.Crew;
import megamek.common.units.CrewType;
import megamek.common.units.Mek;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests which layout the unit display builds. The control layout (General, Weapon, Control) is a client setting, on by
 * default, read once when the display is built; a viewer with no client GUI always gets the classic six tabs.
 */
@DisplayName("Unit display layout setting")
class UnitDisplayLayoutTest {

    private static final GUIPreferences GUIP = GUIPreferences.getInstance();

    private boolean savedSetting;
    private boolean savedTabbed;

    @BeforeAll
    static void requireDisplay() {
        assumeFalse(GraphicsEnvironment.isHeadless(), "Skipping GUI tests - no display available");
    }

    @BeforeEach
    void rememberSetting() {
        savedSetting = GUIP.getUnitDisplayControlLayout();
        savedTabbed = GUIP.getUnitDisplayStartTabbed();
    }

    @AfterEach
    void restoreSetting() {
        GUIP.setUnitDisplayControlLayout(savedSetting);
        GUIP.setUnitDisplayStartTabbed(savedTabbed);
    }

    private static ClientGUI clientGuiOverNewGame() {
        return clientGuiOver(new Game(), null);
    }

    private static ClientGUI clientGuiOver(Game game, Player localPlayer) {
        Client client = mock(Client.class);
        when(client.getGame()).thenReturn(game);
        when(client.getLocalPlayer()).thenReturn(localPlayer);
        ClientGUI clientGui = mock(ClientGUI.class);
        when(clientGui.getClient()).thenReturn(client);
        when(clientGui.getUnitDisplayDialog()).thenReturn(mock(UnitDisplayDialog.class));
        return clientGui;
    }

    private static BipedMek createMek(Game game, Player owner) {
        BipedMek mek = new BipedMek();
        mek.setGame(game);
        mek.setId(1);
        mek.setOwner(owner);
        mek.setChassis("Test Mek");
        mek.setModel("Standard");
        mek.setWeight(50);
        mek.setCrew(new Crew(CrewType.SINGLE));
        mek.autoSetInternal();
        for (int location = 0; location < mek.locations(); location++) {
            mek.initializeArmor(16, location);
        }
        return mek;
    }

    private static ControlTabPanel findControlTab(Container root) {
        for (Component child : root.getComponents()) {
            if (child instanceof ControlTabPanel controlTab) {
                return controlTab;
            }
            if ((child instanceof Container container) && (findControlTab(container) != null)) {
                return findControlTab(container);
            }
        }
        return null;
    }

    @Test
    @DisplayName("The control layout is the default")
    void controlLayoutIsTheDefault() {
        assertTrue(GUIP.getDefaultBoolean(GUIPreferences.UNIT_DISPLAY_CONTROL_LAYOUT),
              "a fresh install gets the control layout");
    }

    @Test
    @DisplayName("With the setting on, a game display gets General, Weapon and Control")
    void settingOnBuildsTheControlLayout() {
        GUIP.setUnitDisplayControlLayout(true);

        UnitDisplayPanel display = new UnitDisplayPanel(clientGuiOverNewGame(), null);

        assertTrue(display.isControlLayout());
    }

    @Test
    @DisplayName("With the setting off, a game display gets the classic six tabs")
    void settingOffBuildsTheClassicLayout() {
        GUIP.setUnitDisplayControlLayout(false);

        UnitDisplayPanel display = new UnitDisplayPanel(clientGuiOverNewGame(), null);

        assertFalse(display.isControlLayout());
    }

    @Test
    @DisplayName("A viewer with no client GUI keeps the classic layout whatever the setting")
    void viewerAlwaysGetsTheClassicLayout() {
        GUIP.setUnitDisplayControlLayout(true);

        UnitDisplayPanel viewer = new UnitDisplayPanel(null, null);

        assertFalse(viewer.isControlLayout(), "the report viewer shows everything, so it keeps six tabs");
    }

    @Test
    @DisplayName("The setting is read once - changing it does not alter a built display")
    void settingIsReadOnce() {
        GUIP.setUnitDisplayControlLayout(true);
        UnitDisplayPanel display = new UnitDisplayPanel(clientGuiOverNewGame(), null);

        GUIP.setUnitDisplayControlLayout(false);

        assertTrue(display.isControlLayout(), "a change applies to the next game, not this display");
        assertEquals(false, GUIP.getUnitDisplayControlLayout());
    }

    @Test
    @DisplayName("Under the control layout, showing a unit fills the Control tab and a location click lands on it")
    void controlLayoutShowsTheUnitOnTheControlTab() {
        GUIP.setUnitDisplayControlLayout(true);
        GUIP.setUnitDisplayStartTabbed(true);
        Game game = new Game();
        Player owner = new Player(0, "Tester");
        game.addPlayer(0, owner);
        UnitDisplayPanel display = new UnitDisplayPanel(clientGuiOver(game, owner), null);

        display.displayEntity(createMek(game, owner));

        ControlTabPanel controlTab = findControlTab(display);
        assertTrue(controlTab != null, "the Control tab is a card of the display");
        display.showSpecificSystem(Mek.LOC_LEFT_ARM);
        display.showControlTab(ControlFocus.CREW);
        display.showControlTab(ControlFocus.EXTRAS);
        display.showControlTab(ControlFocus.DIAGRAM);
        display.setDisplayNonTabbed();
        display.displayEntity(createMek(game, owner));
    }
}
