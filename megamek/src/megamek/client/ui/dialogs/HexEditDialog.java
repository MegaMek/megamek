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
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.io.Serial;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.JToggleButton;
import javax.swing.KeyStroke;
import javax.swing.WindowConstants;

import megamek.client.event.BoardViewEvent;
import megamek.client.event.BoardViewListenerAdapter;
import megamek.client.ui.Messages;
import megamek.client.ui.clientGUI.ClientGUI;
import megamek.client.ui.clientGUI.boardview.BoardView;
import megamek.client.ui.clientGUI.boardview.sprite.FieldOfFireSprite;
import megamek.client.ui.util.FlatLafStyleBuilder;
import megamek.client.ui.util.UIUtil;
import megamek.common.Hex;
import megamek.common.RangeType;
import megamek.common.util.Distractable;
import megamek.common.board.Coords;
import megamek.common.board.HexEditSpec;
import megamek.common.board.HexEditValidator;
import megamek.common.units.Terrain;
import megamek.common.units.Terrains;
import megamek.logging.MMLogger;

/**
 * Lets a Game Master rebuild the ground in one or more hexes, and shows them as they go whether what they have built
 * is a legal hex.
 *
 * <p>The point of the dialog is that a gamemaster should not be able to assemble something the rules do not allow.
 * Terrain levels are offered in the rules' own words - Light, Heavy and Ultra Woods rather than one, two and three -
 * and only the levels a terrain actually has are offered at all. Every change re-checks the hex that would result and
 * puts the reason on screen while there is still something to change, instead of letting the gamemaster press the
 * button and be refused.</p>
 *
 * <p>Structures are not shown or edited here. A building is not part of the ground, it is built on it, and taking one
 * down is Modify Building's job so that it goes through the collapse rules.</p>
 */
public class HexEditDialog extends JDialog {

    @Serial
    private static final long serialVersionUID = 1L;

    private static final MMLogger LOGGER = MMLogger.create(HexEditDialog.class);

    /** The terrains a gamemaster may lay down, in the order they are offered. */
    private static final int[] EDITABLE_TERRAINS = {
          Terrains.WATER, Terrains.RAPIDS, Terrains.WOODS, Terrains.JUNGLE, Terrains.ROUGH, Terrains.RUBBLE,
          Terrains.PAVEMENT, Terrains.ROAD, Terrains.SWAMP, Terrains.MUD, Terrains.SNOW, Terrains.ICE,
          Terrains.TUNDRA, Terrains.SAND, Terrains.MAGMA };

    /** The highest level worth offering for a terrain whose levels are open-ended, such as water depth. */
    private static final int HIGHEST_LEVEL_OFFERED = 10;

    /** Water is the one terrain whose level starts at zero, because depth 0 water is a legal thing to have. */
    private static final int LOWEST_WATER_DEPTH = 0;

    /** Draws the highlight around a picked hex on every side of it. */
    private static final int ALL_HEX_BORDERS = 63;

    private final ClientGUI clientGUI;
    private final int boardId;

    /** The hexes the edit applies to, in the order they were picked. */
    private final List<Coords> selectedHexes = new ArrayList<>();

    /** The terrain the edited hexes should end up holding, as terrain type to level, in the order it was added. */
    private final Map<Integer, Integer> terrainLevels = new LinkedHashMap<>();

    /** The level the edited hexes should end up at. Separate from water depth, which is the water terrain's level. */
    private final JSpinner hexLevelSpinner = new JSpinner(new SpinnerNumberModel(0, -30, 30, 1));

    /** Explains how the hex level and any water depth in it add up, which is not obvious from two numbers. */
    private final JLabel groundLabel = new JLabel();

    private final DefaultListModel<String> hexListModel = new DefaultListModel<>();
    private final DefaultListModel<String> terrainListModel = new DefaultListModel<>();
    private final JList<String> terrainList = new JList<>(terrainListModel);
    private final JComboBox<TerrainChoice> terrainChooser = new JComboBox<>();
    private final JComboBox<LevelChoice> levelChooser = new JComboBox<>();
    private final JLabel legalityLabel = new JLabel();
    private final JButton executeButton = new JButton(Messages.getString("HexEditDialog.execute"));
    private final JButton undoButton = new JButton(Messages.getString("HexEditDialog.undo"));

    /** Whether the edit on screen has been sent, so it can be taken back rather than sent again. */
    private boolean editHasBeenSent;
    private final JToggleButton pickHexesButton =
          new JToggleButton(Messages.getString("HexEditDialog.pickHexes"));

    /** Listens for clicks on the board while hex picking is on, so the gamemaster can select an area to change. */
    private final BoardViewListenerAdapter hexPicker = new BoardViewListenerAdapter() {
        @Override
        public void hexMoused(BoardViewEvent event) {
            boolean isLeftClickOnAHex = (event.getType() == BoardViewEvent.BOARD_HEX_CLICKED)
                  && (event.getButton() == MouseEvent.BUTTON1)
                  && (event.getCoords() != null);
            if (isLeftClickOnAHex) {
                toggleHex(event.getCoords());
            }
        }
    };

    /**
     * The phase display that is being kept from acting on board clicks while hexes are being picked. Without this a
     * click meant for the hex list is also read by the movement phase as a move order, so picking a hex to flood also
     * walks a unit into it.
     */
    private Distractable suppressedDisplay;

    /** The highlight drawn on each picked hex, so the selection is visible on the board and not only in the list. */
    private final Map<Coords, FieldOfFireSprite> hexHighlights = new LinkedHashMap<>();

    /** One terrain offered in the chooser, named the way the map editor names it. */
    private record TerrainChoice(int terrainType) {
        @Override
        public String toString() {
            return Terrains.getEditorName(terrainType);
        }
    }

    /**
     * One level offered for the chosen terrain, in the words the rules use for it. The terrain's own name is dropped
     * from the front, because the terrain is already named in the chooser beside this one and reading "Water" twice
     * across two dropdowns is what made them look like duplicates of each other.
     */
    private record LevelChoice(int terrainType, int level) {
        @Override
        public String toString() {
            String fullName = Terrains.getDisplayName(terrainType, level);
            if (fullName == null) {
                return Messages.getString("HexEditDialog.levelNumber", level);
            }
            String terrainName = Terrains.getEditorName(terrainType);
            String withoutTerrainName = fullName.replace(terrainName, "").replace("()", "").trim();
            return withoutTerrainName.isBlank() ? fullName : withoutTerrainName;
        }
    }

    /**
     * Opens the dialog on one hex, which the gamemaster may then add more hexes to.
     *
     * @param parent    The frame to open over
     * @param clientGUI The client the edit is sent through
     * @param coords    The hex that was right-clicked
     */
    public HexEditDialog(JFrame parent, ClientGUI clientGUI, Coords coords) {
        super(parent, Messages.getString("HexEditDialog.title"), false);
        this.clientGUI = clientGUI;
        this.boardId = clientGUI.getClient().getGame().getBoard().getBoardId();

        selectedHexes.add(coords);
        readTerrainFrom(coords);
        readLevelFrom(coords);
        buildUI(parent);
        refreshEverything();
    }

    /** Starts the edit from what the clicked hex already holds, so the gamemaster changes it rather than replacing it. */
    private void readTerrainFrom(Coords coords) {
        Hex hex = clientGUI.getClient().getGame().getBoard().getHex(coords);
        if (hex == null) {
            return;
        }
        for (int terrainType : EDITABLE_TERRAINS) {
            if (hex.containsTerrain(terrainType)) {
                terrainLevels.put(terrainType, hex.terrainLevel(terrainType));
            }
        }
    }

    /** Starts the level control at the clicked hex's own level, so leaving it alone leaves the ground where it is. */
    private void readLevelFrom(Coords coords) {
        Hex hex = clientGUI.getClient().getGame().getBoard().getHex(coords);
        if (hex != null) {
            hexLevelSpinner.setValue(hex.getLevel());
        }
    }

    private void buildUI(JFrame parent) {
        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        content.add(hexPanel());
        content.add(Box.createVerticalStrut(8));
        content.add(groundPanel());
        content.add(Box.createVerticalStrut(8));
        content.add(terrainPanel());
        content.add(Box.createVerticalStrut(8));
        content.add(legalityPanel());

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(content, BorderLayout.CENTER);
        getContentPane().add(buttonPanel(), BorderLayout.PAGE_END);

        getRootPane().registerKeyboardAction(event -> closeDialog(),
              KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
              JComponent.WHEN_IN_FOCUSED_WINDOW);
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent event) {
                closeDialog();
            }
        });

        setSize(UIUtil.scaleForGUI(460, 520));
        setMinimumSize(UIUtil.scaleForGUI(400, 420));
        setLocationRelativeTo(parent);
    }

    /** The hexes being changed, and the button that lets more be picked off the board. */
    private JPanel hexPanel() {
        JPanel panel = new JPanel(new BorderLayout(4, 4));
        panel.setBorder(BorderFactory.createTitledBorder(Messages.getString("HexEditDialog.hexes")));

        JList<String> hexList = new JList<>(hexListModel);
        hexList.setVisibleRowCount(3);
        panel.add(new JScrollPane(hexList), BorderLayout.CENTER);

        pickHexesButton.setToolTipText(Messages.getString("HexEditDialog.pickHexes.tooltip"));
        pickHexesButton.addActionListener(event -> setHexPicking(pickHexesButton.isSelected()));
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buttons.add(pickHexesButton);
        panel.add(buttons, BorderLayout.PAGE_END);
        return panel;
    }

    /** The terrain the hexes will end up holding, with the controls that add to and take from it. */
    private JPanel terrainPanel() {
        JPanel panel = new JPanel(new BorderLayout(4, 4));
        panel.setBorder(BorderFactory.createTitledBorder(Messages.getString("HexEditDialog.terrain")));

        terrainList.setVisibleRowCount(5);
        panel.add(new JScrollPane(terrainList), BorderLayout.CENTER);

        for (int terrainType : EDITABLE_TERRAINS) {
            terrainChooser.addItem(new TerrainChoice(terrainType));
        }
        terrainChooser.addActionListener(event -> refreshLevelChooser());

        JButton setButton = new JButton(Messages.getString("HexEditDialog.set"));
        setButton.addActionListener(event -> setChosenTerrain());
        JButton removeButton = new JButton(Messages.getString("HexEditDialog.remove"));
        removeButton.addActionListener(event -> removeSelectedTerrain());
        JButton clearButton = new JButton(Messages.getString("HexEditDialog.clear"));
        clearButton.setToolTipText(Messages.getString("HexEditDialog.clear.tooltip"));
        clearButton.addActionListener(event -> clearTerrain());

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT));
        controls.add(new JLabel(Messages.getString("HexEditDialog.terrainLabel")));
        controls.add(terrainChooser);
        controls.add(new JLabel(Messages.getString("HexEditDialog.levelLabel")));
        controls.add(levelChooser);
        controls.add(setButton);
        controls.add(removeButton);
        controls.add(clearButton);
        panel.add(controls, BorderLayout.PAGE_END);
        return panel;
    }

    /**
     * The hex's own level, and what it works out to once any water in it is taken into account.
     *
     * <p>A hex's level and the depth of water in it are two different numbers, and a gamemaster reading them side by
     * side has to do the arithmetic themselves. On a dam board a reservoir hex sits at level 5 holding water 8 deep,
     * which puts its surface five levels up and its bottom three levels down. The line under the spinner says that
     * out loud so the gamemaster can see what they are building.</p>
     */
    private JPanel groundPanel() {
        JPanel panel = new JPanel(new BorderLayout(4, 4));
        panel.setBorder(BorderFactory.createTitledBorder(Messages.getString("HexEditDialog.ground")));

        hexLevelSpinner.addChangeListener(event -> refreshEverything());
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT));
        row.add(new JLabel(Messages.getString("HexEditDialog.hexLevel")));
        row.add(hexLevelSpinner);
        panel.add(row, BorderLayout.CENTER);
        panel.add(groundLabel, BorderLayout.PAGE_END);
        return panel;
    }

    /** Says where the ground and any water surface and bottom end up, in levels. */
    private void refreshGroundLabel() {
        int hexLevel = (int) hexLevelSpinner.getValue();
        Integer waterDepth = terrainLevels.get(Terrains.WATER);
        if ((waterDepth == null) || (waterDepth == 0)) {
            groundLabel.setText(Messages.getString("HexEditDialog.groundDry", hexLevel));
            return;
        }
        groundLabel.setText(Messages.getString("HexEditDialog.groundFlooded",
              hexLevel, waterDepth, hexLevel - waterDepth));
    }

    /** Where the hex is reported legal or not, with the reason. */
    private JPanel legalityPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        legalityLabel.setVerticalAlignment(JLabel.TOP);
        legalityLabel.setPreferredSize(new Dimension(UIUtil.scaleForGUI(400), UIUtil.scaleForGUI(60)));
        panel.add(legalityLabel, BorderLayout.CENTER);
        return panel;
    }

    private JPanel buttonPanel() {
        executeButton.addActionListener(event -> execute());
        undoButton.addActionListener(event -> undoEdit());
        undoButton.setToolTipText(Messages.getString("HexEditDialog.undo.tooltip"));
        undoButton.setEnabled(false);
        JButton closeButton = new JButton(Messages.getString("HexEditDialog.close"));
        closeButton.addActionListener(event -> closeDialog());

        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        panel.add(executeButton);
        panel.add(undoButton);
        panel.add(closeButton);
        getRootPane().setDefaultButton(executeButton);
        return panel;
    }

    /**
     * Turns board hex picking on or off.
     *
     * <p>While it is on the current phase display is kept from acting on board clicks. A click is delivered to
     * everything listening to the board, so without this a click meant to add a hex to the list is also read by the
     * movement phase as a move order, and picking a hex to flood walks a unit into it as well.</p>
     */
    private void setHexPicking(boolean picking) {
        clientGUI.boardViews().forEach(boardView -> {
            boardView.removeBoardViewListener(hexPicker);
            if (picking) {
                boardView.addBoardViewListener(hexPicker);
            }
        });

        if (picking) {
            if ((suppressedDisplay == null) && (clientGUI.getCurrentPanel() instanceof Distractable distractable)) {
                suppressedDisplay = distractable;
                suppressedDisplay.setIgnoringEvents(true);
            }
        } else if (suppressedDisplay != null) {
            suppressedDisplay.setIgnoringEvents(false);
            suppressedDisplay = null;
        }
        LOGGER.debug("[GMHexEdit] hex picking {}", picking ? "on" : "off");
    }

    /** Draws a highlight on every picked hex and takes away the ones no longer picked. */
    private void refreshHexHighlights() {
        clientGUI.boardViews().stream().findFirst().ifPresent(boardView -> {
            boardView.removeSprites(hexHighlights.values());
            hexHighlights.clear();
            for (Coords coords : selectedHexes) {
                hexHighlights.put(coords,
                      new FieldOfFireSprite((BoardView) boardView, RangeType.RANGE_SHORT, coords, ALL_HEX_BORDERS));
            }
            boardView.addSprites(hexHighlights.values());
        });
    }

    /** Adds a hex to the edit, or takes it out again when it was already in. */
    private void toggleHex(Coords coords) {
        if (coords == null) {
            return;
        }
        if (selectedHexes.contains(coords)) {
            selectedHexes.remove(coords);
        } else {
            selectedHexes.add(coords);
        }
        refreshEverything();
    }

    /** Adds the chosen terrain at the chosen level, replacing that terrain if the hexes already had it. */
    private void setChosenTerrain() {
        TerrainChoice terrain = (TerrainChoice) terrainChooser.getSelectedItem();
        LevelChoice level = (LevelChoice) levelChooser.getSelectedItem();
        if ((terrain == null) || (level == null)) {
            return;
        }
        terrainLevels.put(terrain.terrainType(), level.level());
        refreshEverything();
    }

    private void removeSelectedTerrain() {
        int selectedRow = terrainList.getSelectedIndex();
        if (selectedRow < 0) {
            return;
        }
        List<Integer> terrainTypes = new ArrayList<>(terrainLevels.keySet());
        terrainLevels.remove(terrainTypes.get(selectedRow));
        refreshEverything();
    }

    private void clearTerrain() {
        terrainLevels.clear();
        refreshEverything();
    }

    /** Offers only the levels the chosen terrain actually has, named the way the rules name them. */
    private void refreshLevelChooser() {
        TerrainChoice terrain = (TerrainChoice) terrainChooser.getSelectedItem();
        levelChooser.removeAllItems();
        if (terrain == null) {
            return;
        }
        if (terrain.terrainType() == Terrains.WATER) {
            // depth 0 water is a legal hex, so water is the one terrain offered from zero
            levelChooser.addItem(new LevelChoice(Terrains.WATER, LOWEST_WATER_DEPTH));
        }
        for (int level : HexEditValidator.legalLevelsFor(terrain.terrainType(), HIGHEST_LEVEL_OFFERED)) {
            levelChooser.addItem(new LevelChoice(terrain.terrainType(), level));
        }
    }

    /** Redraws the hex list, the terrain list and the legality report from the edit as it now stands. */
    private void refreshEverything() {
        editHasBeenSent = false;
        refreshHexHighlights();
        refreshGroundLabel();
        hexListModel.clear();
        for (Coords coords : selectedHexes) {
            hexListModel.addElement(Messages.getString("HexEditDialog.hexEntry", coords.getBoardNum()));
        }

        terrainListModel.clear();
        for (Map.Entry<Integer, Integer> terrain : terrainLevels.entrySet()) {
            terrainListModel.addElement(Terrains.getDisplayName(terrain.getKey(), terrain.getValue()));
        }
        if (terrainLevels.isEmpty()) {
            terrainListModel.addElement(Messages.getString("HexEditDialog.bareGround"));
        }

        if (levelChooser.getItemCount() == 0) {
            refreshLevelChooser();
        }
        refreshLegality();
    }

    /**
     * Re-checks every selected hex as it would be after the edit and reports the first thing wrong, turning the
     * execute button off while anything is. This is the part that keeps a gamemaster from building an illegal hex:
     * the reason is on screen while there is still something to change.
     */
    private void refreshLegality() {
        List<String> problems = firstProblems();
        boolean isLegal = problems.isEmpty() && !selectedHexes.isEmpty();
        executeButton.setEnabled(isLegal);
        undoButton.setEnabled(editHasBeenSent);

        if (editHasBeenSent) {
            legalityLabel.setText(Messages.getString("HexEditDialog.applied", selectedHexes.size()));
            return;
        }
        if (selectedHexes.isEmpty()) {
            legalityLabel.setText(Messages.getString("HexEditDialog.noHexes"));
            return;
        }
        legalityLabel.setText(isLegal
              ? Messages.getString("HexEditDialog.legal", selectedHexes.size())
              : Messages.getString("HexEditDialog.illegal", String.join(" ", problems)));
    }

    /** @return what is wrong with the first selected hex that the edit would break, or an empty list when none would */
    private List<String> firstProblems() {
        for (Coords coords : selectedHexes) {
            Hex hex = clientGUI.getClient().getGame().getBoard().getHex(coords);
            if (hex == null) {
                return List.of(Messages.getString("HexEditDialog.offBoard", coords.getBoardNum()));
            }
            // the same check the server will make, including the rules that depend on what is standing in the hex.
            // Asking a different question here than the server asks is how a dialog comes to call an edit legal that
            // is then refused, leaving a gamemaster with no visible reason why nothing happened.
            boolean isOccupied = !clientGUI.getClient().getGame().getEntitiesVector(coords, boardId).isEmpty();
            List<String> problems = HexEditValidator.problemsWithChange(hex, editedCopyOf(hex), isOccupied);
            if (!problems.isEmpty()) {
                problems.add(0, Messages.getString("HexEditDialog.inHex", coords.getBoardNum()));
                return problems;
            }
        }
        return List.of();
    }

    /** @return the hex as it would be after the edit, with any structure standing in it carried through */
    private Hex editedCopyOf(Hex hex) {
        Hex edited = hex.duplicate();
        edited.setLevel((int) hexLevelSpinner.getValue());
        List<Terrain> structures = new ArrayList<>();
        for (int structureTerrain : HexEditValidator.structureTerrains()) {
            Terrain existing = edited.getTerrain(structureTerrain);
            if (existing != null) {
                structures.add(existing);
            }
        }
        edited.removeAllTerrains();
        for (Map.Entry<Integer, Integer> terrain : terrainLevels.entrySet()) {
            edited.addTerrain(new Terrain(terrain.getKey(), terrain.getValue()));
        }
        for (Terrain structure : structures) {
            edited.addTerrain(structure);
        }
        return edited;
    }

    /**
     * Sends the edit and closes, because the gamemaster wants to see the board it changed.
     *
     * <p>An edit that names no terrain strips the hexes to bare ground, which is a real thing to want and also what a
     * gamemaster ends up with if they choose a terrain but never add it to the list. On hexes that are already clear
     * it looks like nothing happened at all, so it is worth asking first.</p>
     */
    private void execute() {
        if (terrainLevels.isEmpty() && !confirmStrippingHexesBare()) {
            return;
        }
        sendEdit();
    }

    /** @return {@code true} when the gamemaster confirms they meant to strip the hexes rather than lay something down */
    private boolean confirmStrippingHexesBare() {
        int choice = JOptionPane.showConfirmDialog(this,
              Messages.getString("HexEditDialog.confirmClear.message"),
              Messages.getString("HexEditDialog.confirmClear.title"),
              JOptionPane.YES_NO_OPTION,
              JOptionPane.QUESTION_MESSAGE);
        return choice == JOptionPane.YES_OPTION;
    }

    /**
     * Sends the edit and stays open, offering to take it back.
     *
     * <p>The change is made on the real board rather than drawn as a sketch over it, because the only preview worth
     * having is the one that shows what the players will actually see. The dialog stays up so that a gamemaster who
     * looks at the result and thinks better of it can put the hexes back without hunting for a way to.</p>
     */
    private void sendEdit() {
        HexEditSpec spec = new HexEditSpec(boardId);
        selectedHexes.forEach(spec::addCoords);
        terrainLevels.forEach(spec::setTerrain);
        spec.setLevel((Integer) hexLevelSpinner.getValue());
        LOGGER.info("[GMHexEdit] sending an edit of {} hex(es)", selectedHexes.size());
        clientGUI.getClient().sendHexEdit(spec);
        editHasBeenSent = true;
        setHexPicking(false);
        pickHexesButton.setSelected(false);
        refreshLegality();
    }

    /** Asks the server to put the hexes back the way they were before the edit that was just sent. */
    private void undoEdit() {
        HexEditSpec undo = new HexEditSpec(boardId);
        undo.setUndoingLastEdit(true);
        LOGGER.info("[GMHexEdit] taking back the last edit");
        clientGUI.getClient().sendHexEdit(undo);
        editHasBeenSent = false;
        refreshLegality();
    }

    /** Closes, making sure the board is not left listening for hex picks that no longer have a dialog to go to. */
    private void closeDialog() {
        setHexPicking(false);
        clientGUI.boardViews().stream().findFirst().ifPresent(boardView -> {
            boardView.removeSprites(hexHighlights.values());
            hexHighlights.clear();
        });
        dispose();
    }
}
