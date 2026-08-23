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
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
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
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.JSpinner;
import javax.swing.JToggleButton;
import javax.swing.KeyStroke;
import javax.swing.SpinnerNumberModel;
import javax.swing.WindowConstants;

import megamek.client.event.BoardViewEvent;
import megamek.client.event.BoardViewListenerAdapter;
import megamek.MegaMek;
import megamek.client.ui.Messages;
import megamek.client.ui.preferences.JWindowPreference;
import megamek.client.ui.preferences.PreferencesNode;
import megamek.client.ui.clientGUI.ClientGUI;
import megamek.client.ui.clientGUI.boardview.BoardView;
import megamek.client.ui.clientGUI.boardview.sprite.FieldOfFireSprite;
import megamek.client.ui.util.UIUtil;
import megamek.common.Hex;
import megamek.common.RangeType;
import megamek.common.board.Coords;
import megamek.common.board.HexEditSpec;
import megamek.common.board.HexEditValidator;
import megamek.common.units.Terrain;
import megamek.common.units.Terrains;
import megamek.common.util.Distractable;
import megamek.logging.MMLogger;

/**
 * Lets a Game Master repaint the ground, hex by hex, and shows them as they go whether what they have built is legal.
 *
 * <p>The dialog works as a brush. Set what the brush holds - a terrain, its level, a ground level - then click hexes
 * on the map to lay it down. Change the brush and keep clicking, and different hexes get different terrain, so a
 * river can have deep water in the channel, shallows at its edge and rough ground on the bank in one action. This is
 * how the map editor already works, so it needs no explaining to anyone who has drawn a map.</p>
 *
 * <p>A gamemaster should not be able to assemble something the rules do not allow. Levels are offered in the rules
 * own words - Light, Heavy and Ultra Woods rather than one, two and three - and only the levels a terrain actually
 * has are offered at all. Every painted hex is re-checked and the reason put on screen while there is still something
 * to change, rather than the server refusing the edit afterwards.</p>
 *
 * <p>Structures are not painted here. A building is not part of the ground, it is built on it, and taking one down is
 * Modify Building's job so that it goes through the collapse rules.</p>
 *
 * <h2>Status and known limits</h2>
 *
 * <p>Note for anyone picking this up: the gamemaster terrain tools are a first, deliberately simple pass. They work,
 * but they have had little testing and the validation is thin - see {@link megamek.common.board.HexEditValidator} for
 * what is and is not checked, and for the TODO that goes with it. Both modifying terrain and changing it want more
 * testing than they have had.</p>
 */
public class HexEditDialog extends JDialog {

    @Serial
    private static final long serialVersionUID = 2L;

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

    /** Draws the highlight around a painted hex on every side of it. */
    private static final int ALL_HEX_BORDERS = 63;

    /** Stands for the brush holding nothing, which strips a hex to bare ground. */
    private static final int BARE_GROUND = -1;

    private final ClientGUI clientGUI;
    private final int boardId;

    /** Each hex painted so far and what it will end up holding, in the order it was painted. */
    private final Map<Coords, HexEditSpec.HexPaint> paintedHexes = new LinkedHashMap<>();

    private final DefaultListModel<String> paintedListModel = new DefaultListModel<>();
    private final JComboBox<TerrainChoice> terrainChooser = new JComboBox<>();
    private final JComboBox<LevelChoice> levelChooser = new JComboBox<>();
    private final JSpinner hexLevelSpinner = new JSpinner(new SpinnerNumberModel(0, -30, 30, 1));
    private final JCheckBox changeGroundLevelBox = new JCheckBox(Messages.getString("HexEditDialog.changeGround"));
    private final JLabel groundLabel = new JLabel();
    private final JLabel legalityLabel = new JLabel();
    private final JButton applyButton = new JButton(Messages.getString("HexEditDialog.execute"));
    private final JButton undoButton = new JButton(Messages.getString("HexEditDialog.undo"));
    private final JToggleButton paintOnMapButton = new JToggleButton(Messages.getString("HexEditDialog.paintOnMap"));
    private final JToggleButton eraseButton = new JToggleButton(Messages.getString("HexEditDialog.erase"));

    /** Whether the edit on screen has been applied, so it can be taken back rather than applied again. */
    private boolean editHasBeenApplied;

    /**
     * The phase display kept from acting on board clicks while painting. A click is delivered to everything listening
     * to the board, so without this a click meant for the brush is also read by the movement phase as a move order.
     */
    private Distractable suppressedDisplay;

    /** The highlight drawn on each painted hex, so the work is visible on the board and not only in the list. */
    private final Map<Coords, FieldOfFireSprite> hexHighlights = new LinkedHashMap<>();

    /** Listens for clicks on the board while painting is on. */
    private final BoardViewListenerAdapter brushListener = new BoardViewListenerAdapter() {
        @Override
        public void hexMoused(BoardViewEvent event) {
            boolean isLeftClickOnAHex = (event.getType() == BoardViewEvent.BOARD_HEX_CLICKED)
                  && (event.getButton() == MouseEvent.BUTTON1)
                  && (event.getCoords() != null);
            if (isLeftClickOnAHex) {
                brushHex(event.getCoords());
            }
        }
    };

    /** One terrain offered in the brush, named the way the map editor names it. */
    private record TerrainChoice(int terrainType) {
        @Override
        public String toString() {
            return (terrainType == BARE_GROUND)
                  ? Messages.getString("HexEditDialog.bareGroundChoice")
                  : Terrains.getEditorName(terrainType);
        }
    }

    /**
     * One level offered for the brush's terrain, in the words the rules use for it. The terrain's own name is dropped
     * from the front, because the terrain is named in the chooser beside this one.
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
     * Opens the dialog with the brush set to what the clicked hex already holds, and that hex painted, so a
     * gamemaster who only wants to change one hex has it in front of them.
     *
     * @param parent    The frame to open over
     * @param clientGUI The client the edit is sent through
     * @param coords    The hex that was right-clicked
     */
    public HexEditDialog(JFrame parent, ClientGUI clientGUI, Coords coords) {
        super(parent, Messages.getString("HexEditDialog.title"), false);
        this.clientGUI = clientGUI;
        this.boardId = clientGUI.getClient().getGame().getBoard().getBoardId();

        buildUI(parent);
        loadBrushFrom(coords);
        brushHex(coords);
    }

    /** Sets the brush to what a hex already holds, so the gamemaster starts from that hex rather than from nothing. */
    private void loadBrushFrom(Coords coords) {
        Hex hex = clientGUI.getClient().getGame().getBoard().getHex(coords);
        if (hex == null) {
            return;
        }
        // shown as a starting point only; the level is not written to a hex unless the gamemaster ticks the box
        hexLevelSpinner.setValue(hex.getLevel());
        for (int terrainType : EDITABLE_TERRAINS) {
            if (hex.containsTerrain(terrainType)) {
                terrainChooser.setSelectedItem(new TerrainChoice(terrainType));
                refreshLevelChooser();
                levelChooser.setSelectedItem(new LevelChoice(terrainType, hex.terrainLevel(terrainType)));
                return;
            }
        }
    }

    private void buildUI(JFrame parent) {
        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        content.add(brushPanel());
        content.add(Box.createVerticalStrut(8));
        content.add(paintedPanel());
        content.add(Box.createVerticalStrut(8));
        content.add(legalityPanel());
        content.add(Box.createVerticalGlue());

        // scrolled, and each section held to the height it asks for, so the dialog stays usable on a small screen
        // and a long list of painted hexes does not push the buttons off the bottom
        JScrollPane scroller = new JScrollPane(content);
        scroller.getVerticalScrollBar().setUnitIncrement(16);
        scroller.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroller.setBorder(null);

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(scroller, BorderLayout.CENTER);
        getContentPane().add(buttonPanel(), BorderLayout.PAGE_END);

        getRootPane().registerKeyboardAction(event -> closeDialog(),
              KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
              JComponent.WHEN_IN_FOCUSED_WINDOW);
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent event) {
                closeDialog();
            }
        });

        setSize(UIUtil.scaleForGUI(480, 560));
        setMinimumSize(UIUtil.scaleForGUI(420, 460));
        setLocationRelativeTo(parent);
        setPreferences("HexEditDialog");
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


    /** What the brush holds, and the switch that turns board clicks into painting. */
    private JPanel brushPanel() {
        JPanel panel = new UIUtil.FixedYPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createTitledBorder(Messages.getString("HexEditDialog.brush")));

        terrainChooser.addItem(new TerrainChoice(BARE_GROUND));
        for (int terrainType : EDITABLE_TERRAINS) {
            terrainChooser.addItem(new TerrainChoice(terrainType));
        }
        terrainChooser.addActionListener(event -> {
            refreshLevelChooser();
            refreshGroundLabel();
        });
        levelChooser.addActionListener(event -> refreshGroundLabel());
        hexLevelSpinner.addChangeListener(event -> refreshGroundLabel());

        JPanel terrainRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
        terrainRow.add(new JLabel(Messages.getString("HexEditDialog.terrainLabel")));
        terrainRow.add(terrainChooser);
        terrainRow.add(new JLabel(Messages.getString("HexEditDialog.levelLabel")));
        terrainRow.add(levelChooser);
        panel.add(terrainRow);

        changeGroundLevelBox.setToolTipText(Messages.getString("HexEditDialog.changeGround.tooltip"));
        changeGroundLevelBox.addActionListener(event -> {
            hexLevelSpinner.setEnabled(changeGroundLevelBox.isSelected());
            refreshGroundLabel();
        });
        hexLevelSpinner.setEnabled(false);

        JPanel groundRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
        groundRow.add(changeGroundLevelBox);
        groundRow.add(new JLabel(Messages.getString("HexEditDialog.hexLevel")));
        groundRow.add(hexLevelSpinner);
        panel.add(groundRow);

        paintOnMapButton.setToolTipText(Messages.getString("HexEditDialog.paintOnMap.tooltip"));
        paintOnMapButton.addActionListener(event -> setPainting(paintOnMapButton.isSelected()));
        eraseButton.setToolTipText(Messages.getString("HexEditDialog.erase.tooltip"));

        JButton applyToAllButton = new JButton(Messages.getString("HexEditDialog.brushToAll"));
        applyToAllButton.setToolTipText(Messages.getString("HexEditDialog.brushToAll.tooltip"));
        applyToAllButton.addActionListener(event -> brushEveryPaintedHex());

        JPanel modeRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
        modeRow.add(paintOnMapButton);
        modeRow.add(eraseButton);
        modeRow.add(applyToAllButton);
        panel.add(modeRow);

        panel.add(groundLabel);
        return panel;
    }

    /** The hexes painted so far and what each will hold. */
    private JPanel paintedPanel() {
        JPanel panel = new JPanel(new BorderLayout(4, 4));
        panel.setBorder(BorderFactory.createTitledBorder(Messages.getString("HexEditDialog.painted")));

        JList<String> paintedList = new JList<>(paintedListModel);
        paintedList.setVisibleRowCount(6);
        panel.add(new JScrollPane(paintedList), BorderLayout.CENTER);

        JButton undoLastHexButton = new JButton(Messages.getString("HexEditDialog.undoLastHex"));
        undoLastHexButton.addActionListener(event -> unpaintLastHex());
        JButton clearAllButton = new JButton(Messages.getString("HexEditDialog.clearHexes"));
        clearAllButton.setToolTipText(Messages.getString("HexEditDialog.clearHexes.tooltip"));
        clearAllButton.addActionListener(event -> clearPaintedHexes());

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buttons.add(undoLastHexButton);
        buttons.add(clearAllButton);
        panel.add(buttons, BorderLayout.PAGE_END);
        return panel;
    }

    /** Where the edit is reported legal or not, with the reason. */
    private JPanel legalityPanel() {
        JPanel panel = new UIUtil.FixedYPanel(new BorderLayout());
        legalityLabel.setVerticalAlignment(JLabel.TOP);
        legalityLabel.setPreferredSize(new Dimension(UIUtil.scaleForGUI(420), UIUtil.scaleForGUI(52)));
        panel.add(legalityLabel, BorderLayout.CENTER);
        return panel;
    }

    private JPanel buttonPanel() {
        applyButton.addActionListener(event -> apply());
        undoButton.addActionListener(event -> undoEdit());
        undoButton.setToolTipText(Messages.getString("HexEditDialog.undo.tooltip"));
        undoButton.setEnabled(false);
        JButton closeButton = new JButton(Messages.getString("HexEditDialog.close"));
        closeButton.addActionListener(event -> closeDialog());

        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        panel.add(applyButton);
        panel.add(undoButton);
        panel.add(closeButton);
        getRootPane().setDefaultButton(applyButton);
        return panel;
    }

    /**
     * @return what the brush currently holds, ready to be laid on a hex. The ground level is left out unless the
     *       gamemaster asked to change it, so painting across a hillside follows the ground rather than flattening
     *       every hex to one level.
     */
    private HexEditSpec.HexPaint currentBrush() {
        HexEditSpec.HexPaint paint = new HexEditSpec.HexPaint();
        if (changeGroundLevelBox.isSelected()) {
            paint.setLevel((Integer) hexLevelSpinner.getValue());
        }
        TerrainChoice terrain = (TerrainChoice) terrainChooser.getSelectedItem();
        LevelChoice level = (LevelChoice) levelChooser.getSelectedItem();
        if ((terrain != null) && (terrain.terrainType() != BARE_GROUND) && (level != null)) {
            paint.setTerrain(terrain.terrainType(), level.level());
        }
        return paint;
    }

    /** Lays the brush on a hex, or lifts the hex out of the edit when erasing. */
    private void brushHex(Coords coords) {
        if (coords == null) {
            return;
        }
        if (eraseButton.isSelected()) {
            paintedHexes.remove(coords);
        } else {
            paintedHexes.put(coords, currentBrush());
        }
        refreshEverything();
    }

    /** Lays the brush on every hex painted so far, which is how a whole area is given one terrain. */
    private void brushEveryPaintedHex() {
        for (Coords coords : new ArrayList<>(paintedHexes.keySet())) {
            paintedHexes.put(coords, currentBrush());
        }
        refreshEverything();
    }

    /** Lifts the most recently painted hex back out of the edit. */
    private void unpaintLastHex() {
        List<Coords> painted = new ArrayList<>(paintedHexes.keySet());
        if (!painted.isEmpty()) {
            paintedHexes.remove(painted.getLast());
            refreshEverything();
        }
    }

    private void clearPaintedHexes() {
        paintedHexes.clear();
        refreshEverything();
    }

    /**
     * Turns painting on or off.
     *
     * <p>While it is on the current phase display is kept from acting on board clicks, because a click is delivered
     * to everything listening to the board; without this a click meant for the brush is also read by the movement
     * phase as a move order.</p>
     */
    private void setPainting(boolean painting) {
        clientGUI.boardViews().forEach(boardView -> {
            boardView.removeBoardViewListener(brushListener);
            if (painting) {
                boardView.addBoardViewListener(brushListener);
            }
        });

        if (painting) {
            if ((suppressedDisplay == null) && (clientGUI.getCurrentPanel() instanceof Distractable distractable)) {
                suppressedDisplay = distractable;
                suppressedDisplay.setIgnoringEvents(true);
            }
        } else if (suppressedDisplay != null) {
            suppressedDisplay.setIgnoringEvents(false);
            suppressedDisplay = null;
        }
        LOGGER.debug("[GMHexEdit] painting {}", painting ? "on" : "off");
    }

    /** Offers only the levels the brush's terrain actually has, named the way the rules name them. */
    private void refreshLevelChooser() {
        TerrainChoice terrain = (TerrainChoice) terrainChooser.getSelectedItem();
        levelChooser.removeAllItems();
        if ((terrain == null) || (terrain.terrainType() == BARE_GROUND)) {
            levelChooser.setEnabled(false);
            return;
        }
        levelChooser.setEnabled(true);
        if (terrain.terrainType() == Terrains.WATER) {
            // depth 0 water is a legal hex, so water is the one terrain offered from zero
            levelChooser.addItem(new LevelChoice(Terrains.WATER, LOWEST_WATER_DEPTH));
        }
        for (int level : HexEditValidator.legalLevelsFor(terrain.terrainType(), HIGHEST_LEVEL_OFFERED)) {
            levelChooser.addItem(new LevelChoice(terrain.terrainType(), level));
        }
        // start on the first real level rather than depth 0, which is legal but paints something a gamemaster
        // reaching for water would not see on the board
        if ((terrain.terrainType() == Terrains.WATER) && (levelChooser.getItemCount() > 1)) {
            levelChooser.setSelectedIndex(1);
        }
    }

    /**
     * Says where the ground and any water surface and bottom end up, because two numbers side by side leave the
     * gamemaster doing the arithmetic themselves.
     */
    private void refreshGroundLabel() {
        Integer waterDepth = currentBrush().getTerrainLevels().get(Terrains.WATER);
        boolean isFlooding = (waterDepth != null) && (waterDepth > 0);

        if (!changeGroundLevelBox.isSelected()) {
            groundLabel.setText(isFlooding
                  ? Messages.getString("HexEditDialog.groundKeptFlooded", waterDepth)
                  : Messages.getString("HexEditDialog.groundKept"));
            return;
        }
        int hexLevel = (int) hexLevelSpinner.getValue();
        groundLabel.setText(isFlooding
              ? Messages.getString("HexEditDialog.groundFlooded", hexLevel, waterDepth, hexLevel - waterDepth)
              : Messages.getString("HexEditDialog.groundDry", hexLevel));
    }

    /** Redraws the painted list, the board highlights and the legality report. */
    private void refreshEverything() {
        editHasBeenApplied = false;
        refreshHexHighlights();
        refreshGroundLabel();

        paintedListModel.clear();
        for (Map.Entry<Coords, HexEditSpec.HexPaint> painted : paintedHexes.entrySet()) {
            paintedListModel.addElement(Messages.getString("HexEditDialog.paintedEntry",
                  painted.getKey().getBoardNum(), describe(painted.getValue())));
        }
        refreshLegality();
    }

    /** @return what a painted hex will end up holding, in words */
    private static String describe(HexEditSpec.HexPaint paint) {
        if (paint.isBareGround()) {
            return Messages.getString("HexEditDialog.bareGroundChoice");
        }
        List<String> described = new ArrayList<>();
        for (Map.Entry<Integer, Integer> terrain : paint.getTerrainLevels().entrySet()) {
            described.add(Terrains.getDisplayName(terrain.getKey(), terrain.getValue()));
        }
        return String.join(", ", described);
    }

    /** Draws a highlight on every painted hex and takes away the ones no longer painted. */
    private void refreshHexHighlights() {
        clientGUI.boardViews().stream().findFirst().ifPresent(boardView -> {
            boardView.removeSprites(hexHighlights.values());
            hexHighlights.clear();
            for (Coords coords : paintedHexes.keySet()) {
                hexHighlights.put(coords,
                      new FieldOfFireSprite((BoardView) boardView, RangeType.RANGE_SHORT, coords, ALL_HEX_BORDERS));
            }
            boardView.addSprites(hexHighlights.values());
        });
    }

    /**
     * Re-checks every painted hex and reports the first thing wrong, turning Apply off while anything is. This is
     * what keeps a gamemaster from building an illegal hex: the reason is on screen while there is still something to
     * change.
     */
    private void refreshLegality() {
        List<String> problems = firstProblems();
        boolean isLegal = problems.isEmpty() && !paintedHexes.isEmpty();
        applyButton.setEnabled(isLegal);
        undoButton.setEnabled(editHasBeenApplied);

        if (editHasBeenApplied) {
            legalityLabel.setText(Messages.getString("HexEditDialog.applied", paintedHexes.size()));
            return;
        }
        if (paintedHexes.isEmpty()) {
            legalityLabel.setText(Messages.getString("HexEditDialog.noHexes"));
            return;
        }
        legalityLabel.setText(isLegal
              ? Messages.getString("HexEditDialog.legal", paintedHexes.size())
              : Messages.getString("HexEditDialog.illegal", String.join(" ", problems)));
    }

    /** @return what is wrong with the first painted hex that would break, or an empty list when none would */
    private List<String> firstProblems() {
        for (Map.Entry<Coords, HexEditSpec.HexPaint> painted : paintedHexes.entrySet()) {
            Coords coords = painted.getKey();
            Hex hex = clientGUI.getClient().getGame().getBoard().getHex(coords);
            if (hex == null) {
                return List.of(Messages.getString("HexEditDialog.offBoard", coords.getBoardNum()));
            }
            // the same check the server will make, including the rules that depend on what is standing in the hex.
            // Asking a different question here than the server asks is how a dialog comes to call an edit legal that
            // is then refused, leaving a gamemaster with no visible reason why nothing happened.
            boolean isOccupied = !clientGUI.getClient().getGame().getEntitiesVector(coords, boardId).isEmpty();
            List<String> problems = HexEditValidator.problemsWithChange(hex,
                  editedCopyOf(hex, painted.getValue()), isOccupied);
            if (!problems.isEmpty()) {
                problems.add(0, Messages.getString("HexEditDialog.inHex", coords.getBoardNum()));
                return problems;
            }
        }
        return List.of();
    }

    /** @return the hex as it would be after the paint, with any structure standing in it carried through */
    private static Hex editedCopyOf(Hex hex, HexEditSpec.HexPaint paint) {
        Hex edited = hex.duplicate();
        if (paint.getLevel() != null) {
            edited.setLevel(paint.getLevel());
        }
        List<Terrain> structures = new ArrayList<>();
        for (int structureTerrain : HexEditValidator.carriedThroughTerrains()) {
            Terrain existing = edited.getTerrain(structureTerrain);
            if (existing != null) {
                structures.add(existing);
            }
        }
        edited.removeAllTerrains();
        for (Map.Entry<Integer, Integer> terrain : paint.getTerrainLevels().entrySet()) {
            edited.addTerrain(new Terrain(terrain.getKey(), terrain.getValue()));
        }
        for (Terrain structure : structures) {
            edited.addTerrain(structure);
        }
        return edited;
    }

    /**
     * Applies the edit and stays open, offering to take it back.
     *
     * <p>The change is made on the real board rather than drawn as a sketch over it, because the only preview worth
     * having is the one that shows what the players will actually see.</p>
     */
    private void apply() {
        HexEditSpec spec = new HexEditSpec(boardId);
        paintedHexes.forEach(spec::paint);
        LOGGER.info("[GMHexEdit] applying an edit of {} painted hex(es)", paintedHexes.size());
        clientGUI.getClient().sendHexEdit(spec);
        editHasBeenApplied = true;
        setPainting(false);
        paintOnMapButton.setSelected(false);
        refreshLegality();
    }

    /** Asks the server to put the hexes back the way they were before the edit that was just applied. */
    private void undoEdit() {
        HexEditSpec undo = new HexEditSpec(boardId);
        undo.setUndoingLastEdit(true);
        LOGGER.info("[GMHexEdit] taking back the last edit");
        clientGUI.getClient().sendHexEdit(undo);
        editHasBeenApplied = false;
        refreshLegality();
    }

    /** Closes, making sure the board is not left listening for paint strokes that no longer have a dialog to go to. */
    private void closeDialog() {
        setPainting(false);
        clientGUI.boardViews().stream().findFirst().ifPresent(boardView -> {
            boardView.removeSprites(hexHighlights.values());
            hexHighlights.clear();
        });
        dispose();
    }
}
