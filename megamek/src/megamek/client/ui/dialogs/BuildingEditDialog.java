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

package megamek.client.ui.dialogs;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.KeyEvent;
import java.io.Serial;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.KeyStroke;
import javax.swing.SpinnerNumberModel;
import javax.swing.WindowConstants;

import megamek.MegaMek;
import megamek.client.ui.Messages;
import megamek.client.ui.preferences.JWindowPreference;
import megamek.client.ui.preferences.PreferencesNode;
import megamek.client.ui.clientGUI.ClientGUI;
import megamek.client.ui.util.UIUtil;
import megamek.common.Hex;
import megamek.common.board.BuildingEditSpec;
import megamek.common.board.Coords;
import megamek.common.enums.BasementType;
import megamek.common.enums.BuildingType;
import megamek.common.equipment.FuelTank;
import megamek.common.units.IBuilding;
import megamek.common.units.Terrains;
import megamek.logging.MMLogger;

/**
 * Lets a Game Master put a building in a hex, change the one that is there, or take it away.
 *
 * <p>The dialog describes the building that should be standing in the hex when it is done, rather than offering an
 * add mode and an edit mode. It opens showing whatever is there now - or sensible defaults for an empty hex - and the
 * gamemaster changes what they want and applies it. The server works out from the hex whether that means putting a
 * building up, adjusting the one there, or rebuilding it.</p>
 *
 * <p>What a building is made of and what class it belongs to are fixed when the board makes it, so changing either
 * takes the building down and puts a new one up. That is worth knowing for a building that has already been shot at:
 * it comes back whole.</p>
 *
 * <p>This is for the ordinary map buildings, the kind that live in the hex as terrain. The advanced buildings are
 * units in their own right and belong with the tools that place units, not here.</p>
 *
 * <h2>Status and known limits</h2>
 *
 * <p>Note for anyone picking this up: like the terrain tools this is a first, simple pass and has had little testing.
 * It handles one hex. Real map buildings often span several hexes joined by exits, and the board checks that those
 * exits only join buildings of matching type and class; none of that is done here, so a gamemaster cannot yet build
 * or properly edit a multi-hex building.</p>
 *
 * <p>TODO: multi-hex buildings, and a proper playtest of raising, rebuilding and removing.</p>
 */
public class BuildingEditDialog extends JDialog {

    @Serial
    private static final long serialVersionUID = 1L;

    private static final MMLogger LOGGER = MMLogger.create(BuildingEditDialog.class);

    /** Comfortably above the sturdiest building in the rules, so a gamemaster is not boxed in by the dialog. */
    private static final int MAX_CONSTRUCTION_FACTOR = 500;

    /** Above the heaviest building armor in the rules. */
    private static final int MAX_ARMOR = 200;

    /** Taller than any building on a standard map. */
    private static final int MAX_HEIGHT = 20;

    /** Above any fluff image number the shipped boards use, so a gamemaster is not boxed in by the dialog. */
    private static final int MAX_FLUFF_IMAGE = 999;

    /** Above the largest fuel tank explosion the shipped boards use. */
    private static final int MAX_MAGNITUDE = 100;

    /**
     * How much less damage each hex further from the tank takes. The explosion is dealt as the magnitude in the
     * tank's own hex, less this much in each ring outward, until there is nothing left to deal.
     */
    private static final int MAGNITUDE_DEGRADATION = 10;

    /**
     * The smallest magnitude that does anything at all. The explosion works out how many rings it reaches by
     * dividing the magnitude by the degradation, so anything under one full ring produces no explosion whatsoever -
     * the tank is destroyed and nobody near it notices.
     */
    private static final int SMALLEST_USEFUL_MAGNITUDE = MAGNITUDE_DEGRADATION;

    /** What a new tank starts at; the shipped boards use 10 to 100, most often 20 to 50. */
    private static final int DEFAULT_MAGNITUDE = 20;

    /** Shipped boards step magnitudes in fives, so the spinner does too. */
    private static final int MAGNITUDE_STEP = 5;

    /** What kind of structure stands in the hex, which decides which of the fields below mean anything. */
    private enum StructureKind {
        BUILDING("BuildingEditDialog.structure.building"),
        FUEL_TANK("BuildingEditDialog.structure.fuelTank");

        private final String messageKey;

        StructureKind(String messageKey) {
            this.messageKey = messageKey;
        }

        @Override
        public String toString() {
            return Messages.getString(messageKey);
        }
    }

    private final ClientGUI clientGUI;
    private final Coords coords;
    private final int boardId;

    private final JComboBox<BuildingType> typeChooser = new JComboBox<>();
    private final JComboBox<BuildingClassChoice> classChooser = new JComboBox<>();
    private final JComboBox<BasementType> basementChooser = new JComboBox<>();
    private final JSpinner constructionFactorSpinner =
          new JSpinner(new SpinnerNumberModel(0, 0, MAX_CONSTRUCTION_FACTOR, 5));
    private final JSpinner armorSpinner = new JSpinner(new SpinnerNumberModel(0, 0, MAX_ARMOR, 1));
    private final JSpinner heightSpinner = new JSpinner(new SpinnerNumberModel(1, 1, MAX_HEIGHT, 1));
    private final JSpinner fluffImageSpinner = new JSpinner(new SpinnerNumberModel(0, 0, MAX_FLUFF_IMAGE, 1));
    private final JSpinner magnitudeSpinner =
          new JSpinner(new SpinnerNumberModel(DEFAULT_MAGNITUDE, SMALLEST_USEFUL_MAGNITUDE, MAX_MAGNITUDE,
                MAGNITUDE_STEP));

    /** Says in plain terms what the chosen magnitude will actually do when the tank goes up. */
    private final JLabel magnitudeEffectLabel = new JLabel();
    private final JComboBox<StructureKind> structureChooser = new JComboBox<>();
    private final JButton removeButton = new JButton(Messages.getString("BuildingEditDialog.remove"));
    private final JButton restoreButton = new JButton(Messages.getString("BuildingEditDialog.restore"));

    /** Says what the hex held before this gamemaster first changed it, so a change can be seen and taken back. */
    private final JLabel originalLabel = new JLabel();

    /** One building class offered in the chooser, named rather than numbered. */
    private record BuildingClassChoice(int buildingClass, String messageKey) {
        @Override
        public String toString() {
            return Messages.getString(messageKey);
        }
    }

    /** The classes a building may belong to, in the order they are offered. */
    private static final BuildingClassChoice[] BUILDING_CLASSES = {
          new BuildingClassChoice(IBuilding.STANDARD, "BuildingEditDialog.class.standard"),
          new BuildingClassChoice(IBuilding.HANGAR, "BuildingEditDialog.class.hangar"),
          new BuildingClassChoice(IBuilding.FORTRESS, "BuildingEditDialog.class.fortress"),
          new BuildingClassChoice(IBuilding.GUN_EMPLACEMENT, "BuildingEditDialog.class.gunEmplacement") };

    /**
     * Opens the dialog on one hex, showing the building there or sensible defaults for an empty one.
     *
     * @param parent    The frame to open over
     * @param clientGUI The client the edit is sent through
     * @param coords    The hex that was right-clicked
     */
    public BuildingEditDialog(JFrame parent, ClientGUI clientGUI, Coords coords) {
        super(parent, Messages.getString("BuildingEditDialog.title", coords.getBoardNum()), false);
        this.clientGUI = clientGUI;
        this.coords = coords;
        this.boardId = clientGUI.getClient().getGame().getBoard().getBoardId();

        buildUI(parent);
        loadFromHex();
    }

    /** @return the building standing in the hex, or {@code null} when the hex has none */
    private IBuilding buildingInHex() {
        return clientGUI.getClient().getGame().getBoard().getBuildingAt(coords);
    }

    /**
     * Fills the controls from the building already in the hex, or leaves them at defaults suited to putting a new one
     * up. Either way the gamemaster is changing something concrete rather than describing a building from nothing.
     */
    private void loadFromHex() {
        refreshOriginalLabel();
        IBuilding existing = buildingInHex();
        removeButton.setEnabled(existing != null);
        if (existing == null) {
            typeChooser.setSelectedItem(BuildingType.MEDIUM);
            constructionFactorSpinner.setValue(BuildingType.MEDIUM.getDefaultCF());
            return;
        }
        if (existing instanceof FuelTank fuelTank) {
            structureChooser.setSelectedItem(StructureKind.FUEL_TANK);
            magnitudeSpinner.setValue(Math.max(SMALLEST_USEFUL_MAGNITUDE, fuelTank.getMagnitude()));
            refreshMagnitudeEffect();
            constructionFactorSpinner.setValue(existing.getCurrentCF(coords));
            heightSpinner.setValue(Math.max(1, existing.getHeight(coords)));
            refreshFieldsForStructure();
            return;
        }
        structureChooser.setSelectedItem(StructureKind.BUILDING);
        refreshFieldsForStructure();
        typeChooser.setSelectedItem(existing.getBuildingType());
        constructionFactorSpinner.setValue(existing.getCurrentCF(coords));
        armorSpinner.setValue(existing.getArmor(coords));
        heightSpinner.setValue(Math.max(1, existing.getHeight(coords)));
        basementChooser.setSelectedItem(existing.getBasement(coords));
        Hex hex = clientGUI.getClient().getGame().getBoard().getHex(coords);
        if (hex != null) {
            fluffImageSpinner.setValue(Math.max(0, hex.terrainLevel(Terrains.BLDG_FLUFF)));
        }
        for (BuildingClassChoice choice : BUILDING_CLASSES) {
            if (choice.buildingClass() == existing.getBldgClass()) {
                classChooser.setSelectedItem(choice);
                break;
            }
        }
    }

    private void buildUI(JFrame parent) {
        for (StructureKind kind : StructureKind.values()) {
            structureChooser.addItem(kind);
        }
        structureChooser.addActionListener(event -> refreshFieldsForStructure());
        magnitudeSpinner.setToolTipText(Messages.getString("BuildingEditDialog.magnitude.tooltip"));
        magnitudeSpinner.addChangeListener(event -> refreshMagnitudeEffect());
        for (BuildingType type : BuildingType.values()) {
            // UNKNOWN is what the code uses for "no building here", not something to build
            if (type != BuildingType.UNKNOWN) {
                typeChooser.addItem(type);
            }
        }
        for (BuildingClassChoice choice : BUILDING_CLASSES) {
            classChooser.addItem(choice);
        }
        for (BasementType basement : BasementType.values()) {
            basementChooser.addItem(basement);
        }
        // picking a type moves the construction factor to that type's own, which is what a gamemaster raising a
        // building almost always wants and can still be overridden
        typeChooser.addActionListener(event -> {
            BuildingType type = (BuildingType) typeChooser.getSelectedItem();
            if (type != null) {
                constructionFactorSpinner.setValue(type.getDefaultCF());
            }
        });

        refreshFieldsForStructure();
        refreshMagnitudeEffect();

        JPanel fields = new JPanel(new GridLayout(0, 2, 6, 6));
        fields.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        addField(fields, "BuildingEditDialog.structure", structureChooser);
        addField(fields, "BuildingEditDialog.type", typeChooser);
        addField(fields, "BuildingEditDialog.buildingClass", classChooser);
        addField(fields, "BuildingEditDialog.constructionFactor", constructionFactorSpinner);
        addField(fields, "BuildingEditDialog.armor", armorSpinner);
        addField(fields, "BuildingEditDialog.height", heightSpinner);
        addField(fields, "BuildingEditDialog.basement", basementChooser);
        addField(fields, "BuildingEditDialog.fluffImage", fluffImageSpinner);
        addField(fields, "BuildingEditDialog.magnitude", magnitudeSpinner);
        addField(fields, "BuildingEditDialog.magnitudeEffect", magnitudeEffectLabel);

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(fields, BorderLayout.CENTER);
        getContentPane().add(buttonPanel(), BorderLayout.PAGE_END);

        getRootPane().registerKeyboardAction(event -> dispose(),
              KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
              JComponent.WHEN_IN_FOCUSED_WINDOW);
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        getContentPane().add(originalLabel, BorderLayout.PAGE_START);
        pack();
        setMinimumSize(UIUtil.scaleForGUI(360, 260));
        setLocationRelativeTo(parent);
        setPreferences("BuildingEditDialog");
    }

    /**
     * Restores the size and position the dialog was last left at, and keeps them up to date as it is moved and
     * resized. A gamemaster works out of these dialogs for a whole session, so having one open where they put it
     * last is worth more than opening tidily in the middle of the screen.
     */
    private void setPreferences(String dialogName) {
        try {
            setName(dialogName);
            PreferencesNode preferences = MegaMek.getMMPreferences().forClass(getClass());
            preferences.manage(new JWindowPreference(this));
        } catch (Exception ex) {
            // a dialog that cannot remember where it was is still perfectly usable
            LOGGER.error(ex, "Could not set the preferences of {}", dialogName);
        }
    }


    /**
     * Greys out the fields that mean nothing for the kind of structure chosen. A fuel tank has no building type,
     * class, basement or artwork of its own, and only a fuel tank has an explosion to size.
     */
    private void refreshFieldsForStructure() {
        boolean isFuelTank = structureChooser.getSelectedItem() == StructureKind.FUEL_TANK;
        typeChooser.setEnabled(!isFuelTank);
        classChooser.setEnabled(!isFuelTank);
        basementChooser.setEnabled(!isFuelTank);
        fluffImageSpinner.setEnabled(!isFuelTank);
        magnitudeSpinner.setEnabled(isFuelTank);
        magnitudeEffectLabel.setEnabled(isFuelTank);
    }

    /**
     * Spells out what the chosen magnitude does, because the number on its own says nothing about how far the
     * explosion reaches or how much anything takes.
     */
    private void refreshMagnitudeEffect() {
        int magnitude = (int) magnitudeSpinner.getValue();
        int hexesReached = magnitude / MAGNITUDE_DEGRADATION;
        if (hexesReached < 1) {
            magnitudeEffectLabel.setText(Messages.getString("BuildingEditDialog.magnitudeEffect.none"));
            return;
        }
        int damageAtEdge = magnitude - ((hexesReached - 1) * MAGNITUDE_DEGRADATION);
        magnitudeEffectLabel.setText(Messages.getString("BuildingEditDialog.magnitudeEffect.text",
              magnitude, hexesReached - 1, damageAtEdge));
    }

    private static void addField(JPanel fields, String labelKey, JComponent control) {
        fields.add(new JLabel(Messages.getString(labelKey)));
        fields.add(control);
    }

    private JPanel buttonPanel() {
        JButton applyButton = new JButton(Messages.getString("BuildingEditDialog.apply"));
        applyButton.addActionListener(event -> apply());
        removeButton.setToolTipText(Messages.getString("BuildingEditDialog.remove.tooltip"));
        removeButton.addActionListener(event -> removeBuilding());
        restoreButton.setToolTipText(Messages.getString("BuildingEditDialog.restore.tooltip"));
        restoreButton.addActionListener(event -> restoreOriginal());
        JButton closeButton = new JButton(Messages.getString("HexEditDialog.close"));
        closeButton.addActionListener(event -> dispose());

        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        panel.add(applyButton);
        panel.add(removeButton);
        panel.add(restoreButton);
        panel.add(closeButton);
        getRootPane().setDefaultButton(applyButton);
        return panel;
    }

    /** @return the building the controls describe */
    private BuildingEditSpec describedBuilding() {
        BuildingEditSpec spec = new BuildingEditSpec(coords, boardId);
        BuildingType type = (BuildingType) typeChooser.getSelectedItem();
        if (type != null) {
            spec.setBuildingType(type);
        }
        BuildingClassChoice buildingClass = (BuildingClassChoice) classChooser.getSelectedItem();
        if (buildingClass != null) {
            spec.setBuildingClass(buildingClass.buildingClass());
        }
        BasementType basement = (BasementType) basementChooser.getSelectedItem();
        if (basement != null) {
            spec.setBasement(basement);
        }
        spec.setConstructionFactor((int) constructionFactorSpinner.getValue());
        spec.setArmor((int) armorSpinner.getValue());
        spec.setHeight((int) heightSpinner.getValue());
        spec.setFluffImage((int) fluffImageSpinner.getValue());
        spec.setFuelTank(structureChooser.getSelectedItem() == StructureKind.FUEL_TANK);
        spec.setMagnitude((int) magnitudeSpinner.getValue());
        return spec;
    }

    /** Shows what the hex held before it was first changed, and offers to put it back when there is something to. */
    private void refreshOriginalLabel() {
        String before = GameMasterEditMemory.describeBeforeFirstEdit(coords);
        restoreButton.setEnabled(before != null);
        originalLabel.setText((before == null)
              ? Messages.getString("BuildingEditDialog.original.unchanged")
              : Messages.getString("BuildingEditDialog.original.was", before));
    }

    /** @return what the hex holds now, in words, for remembering before a change is made */
    private String describeHexNow() {
        IBuilding existing = buildingInHex();
        if (existing == null) {
            return Messages.getString("BuildingEditDialog.original.nothing");
        }
        if (existing instanceof FuelTank fuelTank) {
            return Messages.getString("BuildingEditDialog.original.fuelTank",
                  existing.getCurrentCF(coords), fuelTank.getMagnitude());
        }
        return Messages.getString("BuildingEditDialog.original.building",
              existing.getBuildingType().toString(), existing.getCurrentCF(coords), existing.getHeight(coords));
    }

    /** Asks the server to put the hex back the way it was before any gamemaster changed it. */
    private void restoreOriginal() {
        BuildingEditSpec spec = new BuildingEditSpec(coords, boardId);
        spec.setRestoringOriginal(true);
        LOGGER.info("[GMBuilding] restoring hex {} to how it was before it was edited", coords.getBoardNum());
        clientGUI.getClient().sendBuildingEdit(spec);
        GameMasterEditMemory.forget(coords);
        dispose();
    }

    private void apply() {
        Hex hex = clientGUI.getClient().getGame().getBoard().getHex(coords);
        if ((hex != null) && (hex.depth() > 0) && !hex.containsTerrain(Terrains.BUILDING)) {
            // said here as well as on the server, so a gamemaster is told before the trip rather than after it
            clientGUI.doAlertDialog(Messages.getString("BuildingEditDialog.cannotBuild.title"),
                  Messages.getString("BuildingEditDialog.cannotBuild.inWater"));
            return;
        }
        GameMasterEditMemory.rememberBeforeFirstEdit(coords, describeHexNow());
        LOGGER.info("[GMBuilding] sending a building edit for hex {}", coords.getBoardNum());
        clientGUI.getClient().sendBuildingEdit(describedBuilding());
        dispose();
    }

    private void removeBuilding() {
        BuildingEditSpec spec = new BuildingEditSpec(coords, boardId);
        spec.setRemovingBuilding(true);
        GameMasterEditMemory.rememberBeforeFirstEdit(coords, describeHexNow());
        LOGGER.info("[GMBuilding] removing the building in hex {}", coords.getBoardNum());
        clientGUI.getClient().sendBuildingEdit(spec);
        dispose();
    }
}
