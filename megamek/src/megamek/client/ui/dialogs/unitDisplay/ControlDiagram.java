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
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Rectangle;
import java.io.Serial;
import java.util.List;
import java.util.Map;
import java.util.function.IntConsumer;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import megamek.client.ui.Messages;
import megamek.client.ui.buttons.StateToggleButton;
import megamek.client.ui.clientGUI.ClientGUI;
import megamek.client.ui.clientGUI.GUIPreferences;
import megamek.client.ui.dialogs.unitEditor.UnitDamageControls;
import megamek.client.ui.dialogs.unitEditor.UnitDamagePanelBuilder;
import megamek.client.ui.util.UIUtil;
import megamek.common.annotations.Nullable;
import megamek.common.equipment.EquipmentMode;
import megamek.common.equipment.Mounted;
import megamek.common.game.Game;
import megamek.common.units.Entity;

/**
 * The Control tab's diagram: the unit on the left; on the right, the GM damage editor's panels for whichever
 * location is chosen - built by the same {@link UnitDamagePanelBuilder}, in its live mode - with the General panel
 * and the Extras under them, and the Pilot panel when the crew is chosen.
 * <p>
 * Live mode means the armor, structure and critical-hit boxes only show the unit's state, while the mode choosers,
 * on/off switches and ammo dump buttons act at once, through {@link EquipmentActions}, exactly as the classic
 * Systems tab did. The panels are rebuilt from the unit on every refresh, since the server sends a fresh copy of
 * the unit after every change; the chosen location survives the rebuild.
 * <p>
 * The Pilot and Extras panels belong to the unit display and are borrowed with {@link #attachPanels()}; the
 * display's six-panel view can take them back, so the tab attaches them again each time it shows a unit.
 */
class ControlDiagram extends AbstractLocationDiagram {

    @Serial
    private static final long serialVersionUID = 1L;

    /** The chooser entry for the crew. */
    static final String CREW_KEY = "crew";

    private static final double DIAGRAM_SHARE = 0.45;
    private static final GUIPreferences GUIP = GUIPreferences.getInstance();

    private final UnitDisplayPanel unitDisplayPanel;
    private final PilotPanel crew;
    private final ExtraPanel extras;
    private final boolean gameMaster;
    private final IntConsumer editDamage;
    private final JPanel panGeneral = new JPanel();
    private UnitDamageControls controls;
    private boolean acting = false;

    /**
     * @param entity           the unit shown
     * @param unitDisplayPanel the display, whose client carries out the live actions
     * @param crew             the unit display's Pilot panel, shown when the crew is chosen
     * @param extras           the unit display's Extras panel, pinned under the General panel
     * @param gameMaster       whether to offer the GM's damage editor on each location
     * @param editDamage       opens the damage editor at a location; only called when {@code gameMaster}
     */
    ControlDiagram(Entity entity, UnitDisplayPanel unitDisplayPanel, PilotPanel crew, ExtraPanel extras,
          boolean gameMaster, IntConsumer editDamage) {
        super(entity, gameOf(unitDisplayPanel));
        this.unitDisplayPanel = unitDisplayPanel;
        this.crew = crew;
        this.extras = extras;
        this.gameMaster = gameMaster;
        this.editDamage = editDamage;
        getPaperdoll().setFitToWindow(true);
        setResizeWeight(DIAGRAM_SHARE);
        panGeneral.setLayout(new BoxLayout(panGeneral, BoxLayout.PAGE_AXIS));
        buildControls();
        buildCards();
        attachPanels();
    }

    private static @Nullable Game gameOf(UnitDisplayPanel unitDisplayPanel) {
        ClientGUI clientGui = unitDisplayPanel.getClientGUI();
        return (clientGui == null) ? null : clientGui.getClient().getGame();
    }

    /**
     * @param other a unit
     *
     * @return whether the given unit is the one shown - the same unit id in the same shape, allowing for the fresh
     *       copy the server sends after each change
     */
    boolean showsSameUnit(Entity other) {
        Entity shown = getEntity();
        return (other.getId() == shown.getId())
              && (other.getClass() == shown.getClass())
              && (other.locations() == shown.locations());
    }

    /**
     * Borrows the Pilot and Extras panels into this diagram. Safe to call again; a panel already here stays where
     * it is.
     */
    void attachPanels() {
        if (getEntity().getCrew() != null) {
            getCardsPanel().add(crew, CREW_KEY);
        }
        extras.setAlignmentX(Component.LEFT_ALIGNMENT);
        panGeneral.add(extras);
    }

    /**
     * Rebuilds the panels from the unit as it now is and redraws the diagram. The chosen location stays chosen.
     */
    void refresh() {
        buildControls();
        rebuildCards();
        attachPanels();
        refreshDiagram();
    }

    /** Brings the locations forward if the crew is showing instead. */
    void showLocations() {
        DiagramChoice shown = getShownChoice();
        if ((shown == null) || (shown.location() != Entity.LOC_NONE)) {
            return;
        }
        for (int location = 0; location < getEntity().locations(); location++) {
            if (controls.locationPanels[location] != null) {
                selectLocation(location);
                return;
            }
        }
    }

    /** Scrolls the pinned Extras panel into view. */
    void scrollToExtras() {
        extras.scrollRectToVisible(new Rectangle(0, 0, extras.getWidth(), extras.getHeight()));
    }

    UnitDamageControls getControls() {
        return controls;
    }

    // ---- building ----

    /** Builds the GM editor's panels for the unit in live mode, and wires the live controls. */
    private void buildControls() {
        Entity entity = getEntity();
        controls = new UnitDamageControls();
        UnitDamagePanelBuilder builder = new UnitDamagePanelBuilder(entity, controls, false, true);
        if (entity.isConventionalInfantry()) {
            controls.locationPanels = new JPanel[entity.locations()];
            controls.locationLabels = new JLabel[entity.locations()];
            controls.panGeneral = builder.initInfantryPanel();
        } else {
            builder.build();
        }
        colorLocationLabels();
        wireLiveControls();
    }

    @Override
    protected @Nullable JComponent createLocationCard(int location) {
        JPanel panel = controls.locationPanels[location];
        if (panel == null) {
            return null;
        }
        if (!gameMaster) {
            return panel;
        }
        JPanel card = new JPanel(new BorderLayout());
        card.add(panel, BorderLayout.CENTER);
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, UIUtil.scaleForGUI(5), UIUtil.scaleForGUI(2)));
        JButton edit = new JButton(Messages.getString("UnitDisplay.controlTab.editDamage"));
        edit.setToolTipText(Messages.getString("UnitDisplay.controlTab.editDamage.tooltip"));
        edit.addActionListener(event -> editDamage.accept(location));
        row.add(edit);
        card.add(row, BorderLayout.PAGE_END);
        return card;
    }

    @Override
    protected List<DiagramChoice> createExtraChoices() {
        if (getEntity().getCrew() == null) {
            return List.of();
        }
        return List.of(DiagramChoice.extra(CREW_KEY, Messages.getString("UnitDisplay.controlTab.crew")));
    }

    @Override
    protected @Nullable JComponent createExtraCard(DiagramChoice choice) {
        return CREW_KEY.equals(choice.key()) ? crew : null;
    }

    @Override
    protected @Nullable JComponent createGeneralCard() {
        panGeneral.removeAll();
        if (controls.panGeneral != null) {
            controls.panGeneral.setAlignmentX(Component.LEFT_ALIGNMENT);
            panGeneral.add(controls.panGeneral);
        }
        return panGeneral;
    }

    /** Colors each location's title by how damaged the location is, as the GM editor does. */
    private void colorLocationLabels() {
        Entity entity = getEntity();
        for (int location = 0; location < entity.locations(); location++) {
            JLabel label = controls.locationLabels[location];
            if (label == null) {
                continue;
            }
            Color worst = damageColor(entity.getArmor(location, false), entity.getOArmor(location, false), null);
            if (entity.hasRearArmor(location)) {
                worst = damageColor(entity.getArmor(location, true), entity.getOArmor(location, true), worst);
            }
            worst = damageColor(entity.getInternal(location), entity.getOInternal(location), worst);
            if (worst != null) {
                label.setForeground(worst);
            }
        }
    }

    private static @Nullable Color damageColor(int current, int original, @Nullable Color worstSoFar) {
        if (original <= 0) {
            return worstSoFar;
        }
        Color color;
        if (current <= 0) {
            color = GUIP.getUnitTooltipArmorMiniColorDamaged();
        } else if (current < original) {
            color = GUIP.getUnitTooltipArmorMiniColorPartialDamage();
        } else {
            color = GUIP.getUnitTooltipArmorMiniColorIntact();
        }
        if (worstSoFar == null) {
            return color;
        }
        Color damaged = GUIP.getUnitTooltipArmorMiniColorDamaged();
        Color partial = GUIP.getUnitTooltipArmorMiniColorPartialDamage();
        if (damaged.equals(color) || damaged.equals(worstSoFar)) {
            return damaged;
        }
        if (partial.equals(color) || partial.equals(worstSoFar)) {
            return partial;
        }
        return color;
    }

    // ---- live controls ----

    /** Makes the mode choosers, on/off switches and dump buttons act on the unit at once. */
    private void wireLiveControls() {
        Entity entity = getEntity();
        for (Map.Entry<Integer, JComboBox<EquipmentMode>> entry : controls.equipmentModes.entrySet()) {
            Mounted<?> mounted = entity.getEquipment(entry.getKey());
            JComboBox<EquipmentMode> chooser = entry.getValue();
            chooser.addActionListener(event -> {
                if (acting) {
                    return;
                }
                int modeIndex = chooser.getSelectedIndex();
                if (!EquipmentActions.changeMode(unitDisplayPanel, entity, mounted, modeIndex)) {
                    acting = true;
                    try {
                        chooser.setSelectedItem(mounted.curMode());
                    } finally {
                        acting = false;
                    }
                    return;
                }
                refreshLater();
            });
        }
        for (Map.Entry<Integer, UnitDamageControls.ModeSwitch> entry : controls.equipmentOnOff.entrySet()) {
            Mounted<?> mounted = entity.getEquipment(entry.getKey());
            UnitDamageControls.ModeSwitch onOff = entry.getValue();
            StateToggleButton toggle = onOff.toggle();
            toggle.addItemListener(event -> {
                if (acting) {
                    return;
                }
                int modeIndex = modeIndex(mounted, onOff.chosenMode());
                if ((modeIndex < 0) || !EquipmentActions.changeMode(unitDisplayPanel, entity, mounted, modeIndex)) {
                    acting = true;
                    try {
                        toggle.setSelected(!toggle.isSelected());
                    } finally {
                        acting = false;
                    }
                    return;
                }
                refreshLater();
            });
        }
        for (Map.Entry<Integer, JButton> entry : controls.ammoDump.entrySet()) {
            Mounted<?> mounted = entity.getEquipment(entry.getKey());
            entry.getValue().addActionListener(event -> {
                if (EquipmentActions.toggleDump(unitDisplayPanel, entity, mounted)) {
                    refreshLater();
                }
            });
        }
    }

    private static int modeIndex(Mounted<?> mounted, String modeName) {
        for (int index = 0; index < mounted.getType().getModesCount(mounted); index++) {
            if (mounted.getType().getMode(index).getName().equals(modeName)) {
                return index;
            }
        }
        return -1;
    }

    /** Rebuilds after the control that asked for the change has finished with its event. */
    private void refreshLater() {
        SwingUtilities.invokeLater(this::refresh);
    }
}
