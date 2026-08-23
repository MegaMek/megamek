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
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.util.List;
import java.awt.FlowLayout;
import java.awt.event.KeyEvent;
import java.io.Serial;
import javax.swing.JButton;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.ScrollPaneConstants;
import javax.swing.KeyStroke;
import javax.swing.SpinnerNumberModel;
import javax.swing.WindowConstants;

import megamek.MegaMek;
import megamek.client.ui.Messages;
import megamek.client.ui.preferences.JWindowPreference;
import megamek.client.ui.preferences.PreferencesNode;
import megamek.client.ui.clientGUI.ClientGUI;
import megamek.client.ui.tileset.HexTileset;
import megamek.client.ui.tileset.TilesetManager;
import megamek.client.ui.util.UIUtil;
import megamek.common.Hex;
import megamek.common.annotations.Nullable;
import megamek.common.board.BuildingEditSpec;
import megamek.common.board.Coords;
import megamek.common.enums.BasementType;
import megamek.common.enums.BuildingType;
import megamek.common.equipment.FuelTank;
import megamek.common.units.IBuilding;
import megamek.common.units.Terrain;
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
    private final SpinnerNumberModel constructionFactorModel =
          new SpinnerNumberModel(0, 0, MAX_CONSTRUCTION_FACTOR, 5);
    private final JSpinner constructionFactorSpinner = new JSpinner(constructionFactorModel);

    /** Says what band of construction factors the chosen building type covers, so the limit is not a surprise. */
    private final JLabel constructionFactorBandLabel = new JLabel();
    private final JSpinner armorSpinner = new JSpinner(new SpinnerNumberModel(0, 0, MAX_ARMOR, 1));
    private final JSpinner heightSpinner = new JSpinner(new SpinnerNumberModel(1, 1, MAX_HEIGHT, 1));
    private final JComboBox<FluffChoice> fluffImageChooser = new JComboBox<>();
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

    /** Draws the building as the chosen fluff image will actually look, since the number alone says nothing. */
    private final JLabel fluffPreviewLabel = new JLabel();

    /**
     * The dialog's sections. A fuel tank and a building have little in common beyond a construction factor, so the
     * parts that do not apply are taken away rather than greyed out - a dialog that only shows what can be set is
     * shorter and says what it is for.
     */
    private final JPanel structureSection;
    private final JPanel conditionSection;
    private final JPanel appearanceSection;
    private final JPanel explosionSection;

    /**
     * One fluff image offered in the chooser. Zero is the ordinary artwork for the building type; the rest are the
     * levels the tileset actually defines for it.
     */
    private record FluffChoice(int level) {
        @Override
        public String toString() {
            return (level == 0)
                  ? Messages.getString("BuildingEditDialog.fluffImage.default")
                  : Messages.getString("BuildingEditDialog.fluffImage.numbered", level);
        }
    }

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

        structureSection = section("BuildingEditDialog.section.structure",
              "BuildingEditDialog.structure", structureChooser,
              "BuildingEditDialog.type", typeChooser,
              "BuildingEditDialog.buildingClass", classChooser);
        conditionSection = section("BuildingEditDialog.section.condition",
              "BuildingEditDialog.constructionFactor", constructionFactorSpinner,
              "BuildingEditDialog.armor", armorSpinner,
              "BuildingEditDialog.height", heightSpinner,
              "BuildingEditDialog.basement", basementChooser);
        conditionSection.add(constructionFactorBandLabel);
        appearanceSection = section("BuildingEditDialog.section.appearance",
              "BuildingEditDialog.fluffImage", fluffImageChooser,
              "BuildingEditDialog.fluffPreview", fluffPreviewLabel);
        explosionSection = section("BuildingEditDialog.section.explosion",
              "BuildingEditDialog.magnitude", magnitudeSpinner,
              "BuildingEditDialog.magnitudeEffect", magnitudeEffectLabel);

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
        refreshConstructionFactorBand(existing.getCurrentCF(coords));
        constructionFactorSpinner.setValue(existing.getCurrentCF(coords));
        armorSpinner.setValue(existing.getArmor(coords));
        heightSpinner.setValue(Math.max(1, existing.getHeight(coords)));
        basementChooser.setSelectedItem(existing.getBasement(coords));
        Hex hex = clientGUI.getClient().getGame().getBoard().getHex(coords);
        if (hex != null) {
            refreshFluffChoices();
            fluffImageChooser.setSelectedItem(
                  new FluffChoice(Math.max(0, hex.terrainLevel(Terrains.BLDG_FLUFF))));
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
                refreshConstructionFactorBand(type.getDefaultCF());
                constructionFactorSpinner.setValue(type.getDefaultCF());
            }
        });

        // which images exist depends on the whole building, not only its type, so anything the tileset matches on
        // works the list out again
        fluffImageChooser.addActionListener(event -> refreshFluffPreview());
        typeChooser.addActionListener(event -> refreshFluffChoices());
        classChooser.addActionListener(event -> refreshFluffChoices());
        heightSpinner.addChangeListener(event -> refreshFluffChoices());
        armorSpinner.addChangeListener(event -> refreshFluffChoices());
        basementChooser.addActionListener(event -> refreshFluffChoices());

        JPanel sections = new JPanel();
        sections.setLayout(new BoxLayout(sections, BoxLayout.Y_AXIS));
        sections.add(originalSection());
        sections.add(structureSection);
        sections.add(conditionSection);
        sections.add(appearanceSection);
        sections.add(explosionSection);
        sections.add(Box.createVerticalGlue());

        JScrollPane scroller = new JScrollPane(sections);
        scroller.getVerticalScrollBar().setUnitIncrement(16);
        scroller.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroller.setBorder(null);

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(scroller, BorderLayout.CENTER);
        getContentPane().add(buttonPanel(), BorderLayout.PAGE_END);

        getRootPane().registerKeyboardAction(event -> dispose(),
              KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
              JComponent.WHEN_IN_FOCUSED_WINDOW);
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        refreshFieldsForStructure();
        refreshMagnitudeEffect();
        refreshFluffChoices();

        pack();
        setMinimumSize(UIUtil.scaleForGUI(380, 300));
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

    /** @return the fluff image level currently chosen, or zero for the ordinary artwork */
    private int chosenFluffImage() {
        FluffChoice chosen = (FluffChoice) fluffImageChooser.getSelectedItem();
        return (chosen == null) ? 0 : chosen.level();
    }

    /**
     * Fills the fluff chooser with the images the tileset actually has for the building as it is currently set up.
     *
     * <p>The numbers are not a range. The tileset defines particular ones and ties each to a particular building, so
     * a plain number field offered a thousand values of which nearly all drew nothing and most of the rest belonged
     * to a different kind of building - a light building has four images where a hardened one has fifty-four. What
     * is offered here is what exists, and it is worked out again whenever the building changes.</p>
     */
    private void refreshFluffChoices() {
        int previous = chosenFluffImage();
        fluffImageChooser.removeAllItems();
        fluffImageChooser.addItem(new FluffChoice(0));
        for (int level : clientGUI.getTilesetManager().definedFluffLevels(buildingAsHex(0), Terrains.BLDG_FLUFF)) {
            fluffImageChooser.addItem(new FluffChoice(level));
        }
        fluffImageChooser.setSelectedItem(new FluffChoice(previous));
        if (fluffImageChooser.getSelectedIndex() < 0) {
            // the image that was chosen does not exist for this building type, so fall back to its ordinary artwork
            fluffImageChooser.setSelectedIndex(0);
        }
        refreshFluffPreview();
    }

    /**
     * Draws the building as the tileset will actually draw it, for the fluff image currently chosen.
     *
     * <p>A fluff image is picked by number, and the number says nothing about what the building looks like. Boards
     * use them to give a district its character, so a gamemaster placing a building in one wants to see whether it
     * matches its neighbours rather than counting through numbers and looking at the map after each try.</p>
     *
     * <p>The preview is built by asking the tileset for the images it would use for a hex holding exactly this
     * building, which is the same question the board asks when it draws one - so what is shown here is what will
     * appear.</p>
     */
    private void refreshFluffPreview() {
        if (structureChooser.getSelectedItem() == StructureKind.FUEL_TANK) {
            fluffPreviewLabel.setIcon(null);
            fluffPreviewLabel.setText(null);
            return;
        }
        try {
            Image preview = drawBuildingAsTilesetWould();
            fluffPreviewLabel.setIcon((preview == null) ? null : new ImageIcon(preview));
            fluffPreviewLabel.setText((preview == null)
                  ? Messages.getString("BuildingEditDialog.fluffPreview.none")
                  : null);
        } catch (RuntimeException previewFailure) {
            // a preview that cannot be drawn must not stop the dialog being used to place a building
            LOGGER.debug("[GMBuilding] could not draw the fluff preview: {}", previewFailure.getMessage());
            fluffPreviewLabel.setIcon(null);
            fluffPreviewLabel.setText(Messages.getString("BuildingEditDialog.fluffPreview.none"));
        }
    }

    /**
     * Writes the building the dialog currently describes into a hex, the way the board would hold it.
     *
     * <p>Both the picture and the list of available pictures come from this, because the tileset answers questions
     * about hexes rather than about buildings - so asking it with the same hex the board would have is what makes
     * the preview show what will actually appear.</p>
     *
     * @param fluffImage The fluff image to include, or zero for none
     *
     * @return The hex, which belongs to no board and is only ever handed to the tileset
     */
    private Hex buildingAsHex(int fluffImage) {
        Hex hex = new Hex();
        BuildingType type = (BuildingType) typeChooser.getSelectedItem();
        hex.addTerrain(new Terrain(Terrains.BUILDING,
              (type == null) ? BuildingType.MEDIUM.getTypeValue() : type.getTypeValue()));
        hex.addTerrain(new Terrain(Terrains.BLDG_ELEV, (int) heightSpinner.getValue()));
        hex.addTerrain(new Terrain(Terrains.BLDG_CF, (int) constructionFactorSpinner.getValue()));
        BuildingClassChoice buildingClass = (BuildingClassChoice) classChooser.getSelectedItem();
        if (buildingClass != null) {
            hex.addTerrain(new Terrain(Terrains.BLDG_CLASS, buildingClass.buildingClass()));
        }
        int armor = (int) armorSpinner.getValue();
        if (armor > 0) {
            hex.addTerrain(new Terrain(Terrains.BLDG_ARMOR, armor));
        }
        BasementType basement = (BasementType) basementChooser.getSelectedItem();
        hex.addTerrain(new Terrain(Terrains.BLDG_BASEMENT_TYPE,
              ((basement == null) ? BasementType.NONE : basement).ordinal()));
        if (fluffImage > 0) {
            hex.addTerrain(new Terrain(Terrains.BLDG_FLUFF, fluffImage));
        }
        return hex;
    }

    /**
     * @return the building drawn the way the board would draw it, or {@code null} when the tileset offers nothing for
     *       this combination
     */
    private @Nullable Image drawBuildingAsTilesetWould() {
        Hex preview = buildingAsHex(chosenFluffImage());
        TilesetManager tilesetManager = clientGUI.getTilesetManager();
        Image base = tilesetManager.baseFor(preview);
        List<Image> supers = tilesetManager.supersFor(preview);
        if ((base == null) && ((supers == null) || supers.isEmpty())) {
            return null;
        }

        BufferedImage canvas = new BufferedImage(HexTileset.HEX_W, HexTileset.HEX_H,
              BufferedImage.TYPE_INT_ARGB);
        Graphics graphics = canvas.getGraphics();
        try {
            if (base != null) {
                graphics.drawImage(base, 0, 0, null);
            }
            if (supers != null) {
                for (Image layer : supers) {
                    graphics.drawImage(layer, 0, 0, null);
                }
            }
        } finally {
            graphics.dispose();
        }
        return canvas;
    }

    /**
     * Holds the construction factor to the band the chosen building type covers, and says what that band is.
     *
     * <p>Each type is a band rather than a single value - light 1-15, medium 16-40, heavy 41-90, hardened 91-150
     * (Total Warfare, p. 168). A building stronger than its band is not that type of building, it is the next type
     * up, so raising a light building to 40 is really asking for a medium one.</p>
     *
     * <p>Only the top of the band is held to. Damage takes a building below its band without changing what it is
     * made of, so a medium building standing at 3 is simply a medium building nearly flattened.</p>
     *
     * @param currentFactor What the building stands at now, which is allowed even above the band: a board may say
     *                      what it likes, and a gamemaster should be able to lower it rather than be stuck
     */
    private void refreshConstructionFactorBand(int currentFactor) {
        BuildingType type = (BuildingType) typeChooser.getSelectedItem();
        if ((type == null) || (type.getMaximumCF() < 0)) {
            constructionFactorModel.setMaximum(MAX_CONSTRUCTION_FACTOR);
            constructionFactorBandLabel.setText(null);
            return;
        }
        constructionFactorModel.setMaximum(Math.max(type.getMaximumCF(), currentFactor));
        constructionFactorBandLabel.setText(Messages.getString("BuildingEditDialog.constructionFactor.band",
              type.getMinimumCF(), type.getMaximumCF()));
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
        appearanceSection.setVisible(!isFuelTank);
        explosionSection.setVisible(isFuelTank);
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

    /** The line saying what the hex held before a gamemaster changed it. */
    private JPanel originalSection() {
        JPanel panel = new UIUtil.FixedYPanel(new FlowLayout(FlowLayout.LEFT));
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(originalLabel);
        return panel;
    }

    /**
     * Builds one titled section holding label-and-control rows.
     *
     * <p>The rows are laid out so that each control keeps the height it asks for. Giving every row an equal share of
     * the dialog, as a plain grid does, stretches a spinner to the height of a picture and turns ten fields into a
     * wall of controls taller than the screen.</p>
     *
     * @param headerKey The message key for the section's title
     * @param rows      Alternating message keys and the controls they label
     *
     * @return The finished section
     */
    private static JPanel section(String headerKey, Object... rows) {
        JPanel panel = new UIUtil.OptionPanel(headerKey);
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        UIUtil.Content content = new UIUtil.Content(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = new Insets(2, 2, 2, 8);
        constraints.anchor = GridBagConstraints.WEST;

        for (int row = 0; row < rows.length; row += 2) {
            constraints.gridy = row / 2;
            constraints.gridx = 0;
            constraints.weightx = 0;
            constraints.fill = GridBagConstraints.NONE;
            content.add(new JLabel(Messages.getString((String) rows[row])), constraints);

            constraints.gridx = 1;
            constraints.weightx = 1;
            constraints.fill = GridBagConstraints.HORIZONTAL;
            content.add((JComponent) rows[row + 1], constraints);
        }
        panel.add(content);
        return panel;
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
        spec.setFluffImage(chosenFluffImage());
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
