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

import megamek.client.ui.Messages;
import megamek.client.ui.clientGUI.ClientGUI;
import megamek.client.ui.util.UIUtil;
import megamek.common.Hex;
import megamek.common.board.BuildingEditSpec;
import megamek.common.board.Coords;
import megamek.common.enums.BasementType;
import megamek.common.enums.BuildingType;
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
    private final JButton removeButton = new JButton(Messages.getString("BuildingEditDialog.remove"));

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
        IBuilding existing = buildingInHex();
        removeButton.setEnabled(existing != null);
        if (existing == null) {
            typeChooser.setSelectedItem(BuildingType.MEDIUM);
            constructionFactorSpinner.setValue(BuildingType.MEDIUM.getDefaultCF());
            return;
        }
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

        JPanel fields = new JPanel(new GridLayout(0, 2, 6, 6));
        fields.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        addField(fields, "BuildingEditDialog.type", typeChooser);
        addField(fields, "BuildingEditDialog.buildingClass", classChooser);
        addField(fields, "BuildingEditDialog.constructionFactor", constructionFactorSpinner);
        addField(fields, "BuildingEditDialog.armor", armorSpinner);
        addField(fields, "BuildingEditDialog.height", heightSpinner);
        addField(fields, "BuildingEditDialog.basement", basementChooser);
        addField(fields, "BuildingEditDialog.fluffImage", fluffImageSpinner);

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(fields, BorderLayout.CENTER);
        getContentPane().add(buttonPanel(), BorderLayout.PAGE_END);

        getRootPane().registerKeyboardAction(event -> dispose(),
              KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
              JComponent.WHEN_IN_FOCUSED_WINDOW);
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        pack();
        setMinimumSize(UIUtil.scaleForGUI(360, 260));
        setLocationRelativeTo(parent);
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
        JButton closeButton = new JButton(Messages.getString("HexEditDialog.close"));
        closeButton.addActionListener(event -> dispose());

        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        panel.add(applyButton);
        panel.add(removeButton);
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
        return spec;
    }

    private void apply() {
        Hex hex = clientGUI.getClient().getGame().getBoard().getHex(coords);
        if ((hex != null) && (hex.depth() > 0) && !hex.containsTerrain(Terrains.BUILDING)) {
            // said here as well as on the server, so a gamemaster is told before the trip rather than after it
            clientGUI.doAlertDialog(Messages.getString("BuildingEditDialog.cannotBuild.title"),
                  Messages.getString("BuildingEditDialog.cannotBuild.inWater"));
            return;
        }
        LOGGER.info("[GMBuilding] sending a building edit for hex {}", coords.getBoardNum());
        clientGUI.getClient().sendBuildingEdit(describedBuilding());
        dispose();
    }

    private void removeBuilding() {
        BuildingEditSpec spec = new BuildingEditSpec(coords, boardId);
        spec.setRemovingBuilding(true);
        LOGGER.info("[GMBuilding] removing the building in hex {}", coords.getBoardNum());
        clientGUI.getClient().sendBuildingEdit(spec);
        dispose();
    }
}
