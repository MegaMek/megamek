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

import java.awt.BorderLayout;
import java.io.Serial;
import javax.swing.JPanel;

import megamek.client.ui.clientGUI.ClientGUI;
import megamek.client.ui.dialogs.UnitEditorDialog;
import megamek.client.ui.dialogs.unitDisplay.UnitDisplayPanel.ControlFocus;
import megamek.common.Player;
import megamek.common.annotations.Nullable;
import megamek.common.units.Entity;

/**
 * The Control tab of the unit display: the unit diagram with, for whichever location is clicked, its equipment, plus
 * the crew and the extras. It folds the classic Pilot, Armor, Systems and Extras tabs into one.
 * <p>
 * The tab keeps one {@link ControlDiagram} per unit shown and rebuilds it when a different unit is displayed. The
 * Pilot and Extras panels are the unit display's own, borrowed while the tabbed view shows this tab; the six-panel
 * view takes them back.
 */
public class ControlTabPanel extends JPanel {

    @Serial
    private static final long serialVersionUID = 1L;

    private final UnitDisplayPanel unitDisplayPanel;
    private final PilotPanel crew;
    private final ExtraPanel extras;
    private ControlDiagram diagram;

    ControlTabPanel(UnitDisplayPanel unitDisplayPanel, PilotPanel crew, ExtraPanel extras) {
        super(new BorderLayout());
        this.unitDisplayPanel = unitDisplayPanel;
        this.crew = crew;
        this.extras = extras;
    }

    /**
     * Shows the given unit. A unit already shown is refreshed in place; another unit gets a new diagram. Call after
     * the Pilot and Extras panels have been given the unit, since this borrows them.
     *
     * @param entity the unit to show, or {@code null} for none yet
     */
    void displayMek(@Nullable Entity entity) {
        if (entity == null) {
            return;
        }
        if ((diagram == null) || !diagram.showsSameUnit(entity)) {
            if (diagram != null) {
                remove(diagram);
            }
            diagram = new ControlDiagram(entity, unitDisplayPanel, crew, extras, isGameMaster(), this::editDamage);
            add(diagram, BorderLayout.CENTER);
        } else {
            diagram.setEntity(entity);
            diagram.refresh();
        }
        revalidate();
        repaint();
    }

    /**
     * Brings the given part of the tab forward.
     *
     * @param focus the part a key asked for
     */
    void focus(ControlFocus focus) {
        if (diagram == null) {
            return;
        }
        switch (focus) {
            case DIAGRAM -> diagram.showLocations();
            case CREW -> diagram.selectCard(ControlDiagram.CREW_KEY);
            case EXTRAS -> diagram.scrollToExtras();
        }
    }

    /**
     * Brings the given location's equipment forward, as a click on the diagram would.
     *
     * @param location the location
     */
    void selectLocation(int location) {
        if (diagram != null) {
            diagram.selectLocation(location);
        }
    }

    private boolean isGameMaster() {
        ClientGUI clientGui = unitDisplayPanel.getClientGUI();
        if (clientGui == null) {
            return false;
        }
        Player localPlayer = clientGui.getClient().getLocalPlayer();
        return (localPlayer != null) && localPlayer.isGameMaster();
    }

    /** Opens the GM damage editor on the unit shown, at the given location. */
    private void editDamage(int location) {
        ClientGUI clientGui = unitDisplayPanel.getClientGUI();
        if ((clientGui == null) || (diagram == null)) {
            return;
        }
        // The editor commits its edits to the server itself, so there is nothing to send when it closes.
        UnitEditorDialog editor = new UnitEditorDialog(clientGui.getFrame(),
              diagram.getEntity(),
              true,
              clientGui.getClient());
        editor.showLocation(location);
        clientGui.getBoardView().setShouldIgnoreKeys(true);
        editor.setVisible(true);
        editor.dispose();
        clientGui.getBoardView().setShouldIgnoreKeys(false);
    }
}
