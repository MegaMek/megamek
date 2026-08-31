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
import java.awt.Component;
import java.awt.FlowLayout;
import java.io.Serial;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;

import megamek.client.ui.Messages;
import megamek.client.ui.clientGUI.ClientGUI;
import megamek.client.ui.dialogs.UnitEditorDialog;
import megamek.client.ui.dialogs.unitDisplay.UnitDisplayPanel.ControlFocus;
import megamek.client.ui.util.UIUtil;
import megamek.common.Player;
import megamek.common.annotations.Nullable;
import megamek.common.units.Entity;

/**
 * The Control tab of the unit display: the unit diagram with, for whichever location is clicked, its equipment, plus
 * the crew and the extras. It folds the classic Pilot, Armor, Systems and Extras tabs into one.
 * <p>
 * A unit that carries others gets a Unit chooser above the diagram, as the Systems tab's unit list did: choosing a
 * carried unit shows that unit's diagram, panels, crew and status.
 * <p>
 * The tab keeps one {@link ControlDiagram} for the unit shown and rebuilds it when another unit is displayed. Its
 * crew and status panels are its own, so the display's six-panel view keeps its own untouched.
 */
public class ControlTabPanel extends JPanel {

    @Serial
    private static final long serialVersionUID = 1L;

    private final UnitDisplayPanel unitDisplayPanel;
    private final PilotPanel crew;
    private final ExtraPanel extras;
    private final JComboBox<Entity> unitChooser = new JComboBox<>();
    private final JPanel panChooser;
    private ControlDiagram diagram;
    /** The unit the display is showing, which carries whatever else the chooser offers. */
    private Entity carrier;
    /** The id of the unit whose panels are shown, so a refresh does not drop back to the carrier. */
    private int shownId = Entity.NONE;
    private boolean choosing = false;

    ControlTabPanel(UnitDisplayPanel unitDisplayPanel) {
        super(new BorderLayout());
        this.unitDisplayPanel = unitDisplayPanel;
        // The tab has its own crew and status panels rather than borrowing the display's, so the six-panel view
        // keeps its own in its own look and neither view has to hand components back to the other.
        this.crew = new PilotPanel(unitDisplayPanel);
        this.extras = new ExtraPanel(unitDisplayPanel, true);

        unitChooser.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
                  boolean cellHasFocus) {
                Component renderer = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof Entity unit) {
                    setText(unit.equals(carrier) ? Messages.getString("MekDisplay.Ego") : unit.getShortName());
                }
                return renderer;
            }
        });
        unitChooser.addActionListener(event -> {
            if (!choosing && (unitChooser.getSelectedItem() instanceof Entity chosen)) {
                showEntity(chosen);
            }
        });
        panChooser = new JPanel(new FlowLayout(FlowLayout.LEFT, UIUtil.scaleForGUI(5), UIUtil.scaleForGUI(2)));
        panChooser.add(new JLabel(Messages.getString("MekDisplay.Unit")));
        panChooser.add(unitChooser);
        panChooser.setVisible(false);
        add(panChooser, BorderLayout.PAGE_START);
    }

    /**
     * Shows the given unit. A unit already shown is refreshed in place; another unit gets a new diagram.
     *
     * @param entity the unit to show, or {@code null} for none yet
     */
    void displayMek(@Nullable Entity entity) {
        if (entity == null) {
            return;
        }
        carrier = entity;
        Entity toShow = entity;
        choosing = true;
        try {
            unitChooser.removeAllItems();
            unitChooser.addItem(entity);
            for (Entity loaded : entity.getLoadedUnits()) {
                unitChooser.addItem(loaded);
                // a refresh while a carried unit is shown keeps showing it
                if (loaded.getId() == shownId) {
                    toShow = loaded;
                }
            }
            unitChooser.setSelectedItem(toShow);
        } finally {
            choosing = false;
        }
        // the chooser is only worth its row when the unit actually carries something
        panChooser.setVisible(unitChooser.getItemCount() > 1);
        showEntity(toShow);
    }

    /** Shows the unit chosen: its diagram, its panels, its crew and its status. */
    private void showEntity(Entity entity) {
        shownId = entity.getId();
        crew.displayMek(entity);
        extras.displayMek(entity);
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
