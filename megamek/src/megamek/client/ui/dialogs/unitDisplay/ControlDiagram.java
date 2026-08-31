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

import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.io.Serial;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import megamek.client.ui.Messages;
import megamek.client.ui.util.UIUtil;
import megamek.common.annotations.Nullable;
import megamek.common.game.Game;
import megamek.common.units.Entity;

/**
 * The Control tab's diagram: the unit on the left; on the right, a card per location with its armor and structure,
 * the Systems panel showing that location's equipment, the Pilot panel in place of it when the crew is chosen, and
 * the Extras panel pinned under whichever of those is showing.
 * <p>
 * The Systems, Pilot and Extras panels belong to the unit display and are borrowed with {@link #attachPanels()};
 * the display's six-panel view can take them back, so the tab attaches them again each time it shows a unit.
 */
class ControlDiagram extends AbstractLocationDiagram {

    @Serial
    private static final long serialVersionUID = 1L;

    /** The chooser entry for the crew. */
    static final String CREW_KEY = "crew";
    /** The chooser entry that lists every piece of equipment on the unit. */
    static final String ALL_EQUIPMENT_KEY = "all-equipment";

    private static final String SYSTEMS_CARD = "systems";
    private static final String CREW_CARD = "crew";
    private static final double DIAGRAM_SHARE = 0.45;

    private final SystemPanel systems;
    private final PilotPanel crew;
    private final ExtraPanel extras;
    private final boolean gameMaster;
    private final IntConsumer editDamage;
    private final CardLayout detailLayout = new CardLayout();
    private final JPanel panDetail = new JPanel(detailLayout);
    private final JPanel panGeneral = new JPanel();
    private final List<LocationCard> locationCards = new ArrayList<>();

    /**
     * @param entity     the unit shown
     * @param game       the game, or {@code null} outside one
     * @param systems    the unit display's Systems panel, shown for the chosen location
     * @param crew       the unit display's Pilot panel, shown when the crew is chosen
     * @param extras     the unit display's Extras panel, pinned under the other two
     * @param gameMaster whether to offer the GM's damage editor on each location
     * @param editDamage opens the damage editor at a location; only called when {@code gameMaster}
     */
    ControlDiagram(Entity entity, @Nullable Game game, SystemPanel systems, PilotPanel crew, ExtraPanel extras,
          boolean gameMaster, IntConsumer editDamage) {
        super(entity, game);
        this.systems = systems;
        this.crew = crew;
        this.extras = extras;
        this.gameMaster = gameMaster;
        this.editDamage = editDamage;
        getPaperdoll().setFitToWindow(true);
        setResizeWeight(DIAGRAM_SHARE);
        buildCards();
        attachPanels();
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
     * Borrows the Systems, Pilot and Extras panels into this diagram. Safe to call again; a panel already here stays
     * where it is.
     */
    void attachPanels() {
        panDetail.add(systems, SYSTEMS_CARD);
        panDetail.add(crew, CREW_CARD);
        panGeneral.add(extras);
        // the diagram picks the location, so the Systems panel's own location list would only repeat it
        systems.setLocationListVisible(false);
    }

    /** Redraws the diagram and the location cards from the unit, and re-applies the chosen card to the panels. */
    void refresh() {
        for (LocationCard card : locationCards) {
            card.refresh();
        }
        refreshDiagram();
        DiagramChoice shown = getShownChoice();
        if (shown != null) {
            onCardShown(shown);
        }
    }

    /** Brings the locations forward if the crew or the equipment list is showing instead. */
    void showLocations() {
        DiagramChoice shown = getShownChoice();
        if ((shown == null) || (shown.location() != Entity.LOC_NONE) || locationCards.isEmpty()) {
            return;
        }
        selectLocation(locationCards.getFirst().location);
    }

    /** Scrolls the pinned Extras panel into view. */
    void scrollToExtras() {
        extras.scrollRectToVisible(new Rectangle(0, 0, extras.getWidth(), extras.getHeight()));
    }

    @Override
    protected @Nullable JComponent createLocationCard(int location) {
        Entity entity = getEntity();
        boolean hasArmor = entity.getOArmor(location) > 0;
        boolean hasStructure = entity.getOInternal(location) > 0;
        boolean hasSlots = entity.getNumberOfCriticalSlots(location) > 0;
        if (!hasArmor && !hasStructure && !hasSlots) {
            return null;
        }
        LocationCard card = new LocationCard(location);
        locationCards.add(card);
        return card;
    }

    @Override
    protected List<DiagramChoice> createExtraChoices() {
        return List.of(DiagramChoice.extra(CREW_KEY, Messages.getString("UnitDisplay.controlTab.crew")),
              DiagramChoice.extra(ALL_EQUIPMENT_KEY, Messages.getString("UnitDisplay.controlTab.allEquipment")));
    }

    @Override
    protected @Nullable JComponent createExtraCard(DiagramChoice choice) {
        // the crew and the equipment list are shown by the borrowed panels below; the card itself is empty
        return new JPanel();
    }

    @Override
    protected @Nullable JComponent createGeneralCard() {
        panGeneral.setLayout(new BoxLayout(panGeneral, BoxLayout.PAGE_AXIS));
        panDetail.setAlignmentX(Component.LEFT_ALIGNMENT);
        extras.setAlignmentX(Component.LEFT_ALIGNMENT);
        panGeneral.add(panDetail);
        return panGeneral;
    }

    @Override
    protected void onCardShown(DiagramChoice choice) {
        if (choice.location() != Entity.LOC_NONE) {
            detailLayout.show(panDetail, SYSTEMS_CARD);
            // a location with no critical slots (a vehicle's) has nothing of its own to list
            if (!systems.selectLocation(choice.location())) {
                systems.showAllEquipment();
            }
        } else if (CREW_KEY.equals(choice.key())) {
            detailLayout.show(panDetail, CREW_CARD);
        } else if (ALL_EQUIPMENT_KEY.equals(choice.key())) {
            detailLayout.show(panDetail, SYSTEMS_CARD);
            systems.showAllEquipment();
        }
    }

    /** A location's armor and structure, and for a GM the way into the damage editor for it. */
    private final class LocationCard extends JPanel {

        @Serial
        private static final long serialVersionUID = 1L;

        private final int location;
        private final JLabel armor = new JLabel();
        private final JLabel rear = new JLabel();
        private final JLabel structure = new JLabel();

        LocationCard(int location) {
            super(new GridBagLayout());
            this.location = location;
            int inset = UIUtil.scaleForGUI(4);
            GridBagConstraints constraints = new GridBagConstraints();
            constraints.anchor = GridBagConstraints.LINE_START;
            constraints.insets = new Insets(inset, inset * 2, inset, inset * 2);
            constraints.gridx = 0;
            constraints.gridy = 0;
            constraints.gridwidth = 2;

            JLabel title = new JLabel(getEntity().getLocationName(location));
            title.setFont(title.getFont().deriveFont(Font.BOLD));
            add(title, constraints);

            constraints.gridwidth = 1;
            constraints.gridy++;
            add(new JLabel(Messages.getString("UnitDisplay.controlTab.armor")), constraints);
            constraints.gridx = 1;
            add(armor, constraints);

            if (getEntity().hasRearArmor(location)) {
                constraints.gridx = 0;
                constraints.gridy++;
                add(new JLabel(Messages.getString("UnitDisplay.controlTab.rear")), constraints);
                constraints.gridx = 1;
                add(rear, constraints);
            }

            constraints.gridx = 0;
            constraints.gridy++;
            add(new JLabel(Messages.getString("UnitDisplay.controlTab.structure")), constraints);
            constraints.gridx = 1;
            add(structure, constraints);

            if (gameMaster) {
                constraints.gridx = 0;
                constraints.gridy++;
                constraints.gridwidth = 2;
                JButton edit = new JButton(Messages.getString("UnitDisplay.controlTab.editDamage"));
                edit.setToolTipText(Messages.getString("UnitDisplay.controlTab.editDamage.tooltip"));
                edit.addActionListener(event -> editDamage.accept(location));
                add(edit, constraints);
            }

            // take only the height the rows need, so the panels below sit right under the card
            constraints.gridx = 0;
            constraints.gridy++;
            constraints.weightx = 1.0;
            constraints.weighty = 1.0;
            add(new JLabel(), constraints);
            refresh();
        }

        void refresh() {
            Entity entity = getEntity();
            armor.setText(valueText(entity.getArmor(location, false), entity.getOArmor(location, false)));
            rear.setText(valueText(entity.getArmor(location, true), entity.getOArmor(location, true)));
            structure.setText(valueText(entity.getInternal(location), entity.getOInternal(location)));
        }

        private String valueText(int current, int original) {
            if (original <= 0) {
                return Messages.getString("UnitDisplay.controlTab.none");
            }
            return Math.max(current, 0) + " / " + original;
        }
    }
}
