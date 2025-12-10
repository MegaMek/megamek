/*
 * Copyright (c) 2000-2003 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2002-2025 The MegaMek Team. All Rights Reserved.
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
package megamek.client.ui.boardeditor;

import static megamek.common.units.Terrains.*;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.Stack;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.filechooser.FileFilter;

import megamek.client.event.BoardViewEvent;
import megamek.client.event.BoardViewListenerAdapter;
import megamek.client.ui.Messages;
import megamek.client.ui.clientGUI.BoardFileFilter;
import megamek.client.ui.clientGUI.ClientGUI;
import megamek.client.ui.clientGUI.CommonMenuBar;
import megamek.client.ui.clientGUI.GUIPreferences;
import megamek.client.ui.clientGUI.IMapSettingsObserver;
import megamek.client.ui.clientGUI.RecentBoardList;
import megamek.client.ui.clientGUI.boardview.BoardView;
import megamek.client.ui.clientGUI.boardview.overlay.KeyBindingsOverlay;
import megamek.client.ui.clientGUI.boardview.overlay.TraceOverlay;
import megamek.client.ui.clientGUI.boardview.toolTip.BoardEditorTooltip;
import megamek.client.ui.dialogs.CommonAboutDialog;
import megamek.client.ui.dialogs.ConfirmDialog;
import megamek.client.ui.dialogs.ExitsDialog;
import megamek.client.ui.dialogs.MMDialogs.MMConfirmDialog;
import megamek.client.ui.dialogs.buttonDialogs.CommonSettingsDialog;
import megamek.client.ui.dialogs.buttonDialogs.MultiIntSelectorDialog;
import megamek.client.ui.dialogs.helpDialogs.AbstractHelpDialog;
import megamek.client.ui.dialogs.helpDialogs.BoardEditorHelpDialog;
import megamek.client.ui.dialogs.minimap.MinimapDialog;
import megamek.client.ui.dialogs.minimap.MinimapPanel;
import megamek.client.ui.dialogs.randomMap.RandomMapDialog;
import megamek.client.ui.dialogs.randomMap.ResizeMapDialog;
import megamek.client.ui.enums.DialogResult;
import megamek.client.ui.tileset.TilesetManager;
import megamek.client.ui.util.MegaMekController;
import megamek.client.ui.util.UIUtil;
import megamek.client.ui.util.UIUtil.FixedYPanel;
import megamek.common.Configuration;
import megamek.common.Hex;
import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.game.Game;
import megamek.common.loaders.MapSettings;
import megamek.common.units.IBuilding;
import megamek.common.units.Terrain;
import megamek.common.units.Terrains;
import megamek.common.util.BoardUtilities;
import megamek.common.util.ImageUtil;
import megamek.common.util.fileUtils.MegaMekFile;
import megamek.logging.MMLogger;

// TODO: center map
// TODO: background on the whole screen
// TODO: restrict terrains to those with images?
// TODO: Allow drawing of invalid terrain as an override?
// TODO: Allow adding/changing board background images
// TODO: sluggish hex drawing?
// TODO: the board validation after a board load seems to be influenced by the former board...
// TODO: copy/paste hexes
public class BoardEditorPanel extends JPanel
      implements ItemListener, ListSelectionListener, ActionListener, DocumentListener, IMapSettingsObserver {
    private final static MMLogger LOGGER = MMLogger.create(BoardEditorPanel.class);

    private final GUIPreferences guip = GUIPreferences.getInstance();

    private static final int BASE_TERRAINBUTTON_ICON_WIDTH = 70;
    private static final int BASE_ARROWBUTTON_ICON_WIDTH = 25;
    private static final String CMD_EDIT_DEPLOYMENT_ZONES = "CMD_EDIT_DEPLOYMENT_ZONES";

    // Components
    private final JFrame frame = new JFrame();
    private final Game game = new Game();
    private Board board = game.getBoard();
    BoardView bv;
    boolean isDragging = false;
    private Component bvc;
    private final CommonMenuBar menuBar = CommonMenuBar.getMenuBarForBoardEditor();
    private AbstractHelpDialog help;
    private CommonSettingsDialog settingsDialog;
    private MinimapDialog minimapW;
    private final MegaMekController controller;

    // The current files
    private File curFileImage;
    private File curBoardFile;

    // The active hex "brush"
    private HexCanvas canHex;
    Hex curHex = new Hex();

    // Easy terrain access buttons
    private final List<ScalingIconButton> terrainButtons = new ArrayList<>();
    private ScalingIconButton buttonLW, buttonLJ;
    private ScalingIconButton buttonOW, buttonOJ;
    private ScalingIconButton buttonWa, buttonSw, buttonRo;
    private ScalingIconButton buttonRd, buttonCl, buttonBu;
    private ScalingIconButton buttonMd, buttonPv, buttonSn;
    private ScalingIconButton buttonIc, buttonTu, buttonMg;
    private ScalingIconButton buttonBr, buttonFT;
    private final List<ScalingIconToggleButton> brushButtons = new ArrayList<>();
    private ScalingIconToggleButton buttonBrush1, buttonBrush2, buttonBrush3;
    private ScalingIconToggleButton buttonOOC;
    private final List<ScalingIconToggleButton> paintModeButtons = new ArrayList<>();
    private ScalingIconToggleButton buttonRaiseLower;
    private ScalingIconToggleButton buttonDeployZone;

    private final SpinnerNumberModel deploymentZoneSpnModel = new SpinnerNumberModel(1, 1, 31, 1);
    private final JSpinner deploymentZoneChooser = new JSpinner(deploymentZoneSpnModel);

    // The brush size: 1 = 1 hex, 2 = radius 1, 3 = radius 2
    private int brushSize = 1;
    private int hexLevelToDraw = -1000;
    private EditorTextField texElev;
    private ScalingIconButton butElevUp;
    private ScalingIconButton butElevDown;
    private JList<TerrainTypeHelper> lisTerrain;
    private TerrainListRenderer lisTerrainRenderer;
    private ScalingIconButton butDelTerrain;
    private JComboBox<TerrainHelper> choTerrainType;
    private EditorTextField texTerrainLevel;
    private JCheckBox cheTerrExitSpecified;
    private EditorTextField texTerrExits;
    private ScalingIconButton butTerrExits;
    private JCheckBox cheRoadsAutoExit;
    private final JButton copyButton = new JButton(Messages.getString("BoardEditor.copyButton"));
    private final JButton pasteButton = new JButton(Messages.getString("BoardEditor.pasteButton"));
    private ScalingIconButton butExitUp, butExitDown;
    private JComboBox<String> choTheme;
    private ScalingIconButton butTerrDown, butTerrUp;
    private JButton butAddTerrain;
    private JButton butSourceFile;
    private MapSettings mapSettings = MapSettings.getInstance();
    private Coords lastClicked;
    private final JLabel labTheme = new JLabel(Messages.getString("BoardEditor.labTheme"), SwingConstants.LEFT);

    private final FixedYPanel panelHexSettings = new FixedYPanel();
    private final FixedYPanel panelTerrSettings = new FixedYPanel(new GridLayout(0, 2, 4, 4));
    private final FixedYPanel panelBoardSettings = new FixedYPanel();

    // Help Texts
    private final JLabel labHelp1 = new JLabel(Messages.getString("BoardEditor.helpText"), SwingConstants.LEFT);
    private final JLabel labHelp2 = new JLabel(Messages.getString("BoardEditor.helpText2"), SwingConstants.LEFT);

    // Undo / Redo
    private final List<ScalingIconButton> undoButtons = new ArrayList<>();
    private ScalingIconButton buttonUndo, buttonRedo;
    private final Stack<HashSet<Hex>> undoStack = new Stack<>();
    private final Stack<HashSet<Hex>> redoStack = new Stack<>();
    private HashSet<Hex> currentUndoSet;
    private HashSet<Coords> currentUndoCoords;

    // Tracker for board changes; unfortunately this is not equal to
    // undoStack == empty because saving the board doesn't empty the
    // undo stack but makes the board unchanged.
    /** Tracks if the board has changes over the last saved version. */
    private boolean hasChanges = false;
    /** Tracks if the board can return to the last saved version. */
    private boolean canReturnToSaved = true;
    /**
     * The undo stack size at the last save. Used to track saved status of the board.
     */
    private int savedUndoStackSize = 0;

    // Misc
    private File loadPath = Configuration.boardsDir();

    /**
     * Special purpose indicator, keeps terrain list from de-selecting when clicking it
     */
    private boolean terrListBlocker = false;

    /**
     * Special purpose indicator, prevents an update loop when the terrain level or exits field is changed
     */
    private boolean noTextFieldUpdate = false;

    /**
     * A MouseAdapter that closes a JLabel when clicked
     */
    private final MouseAdapter clickToHide = new MouseAdapter() {
        @Override
        public void mouseReleased(MouseEvent e) {
            if (e.getSource() instanceof JLabel) {
                ((JLabel) e.getSource()).setVisible(false);
            }
        }
    };

    /**
     * Flag that indicates whether hotkeys should be ignored or not. This is used for disabling hot keys when various
     * dialogs are displayed.
     */
    private boolean ignoreHotKeys = false;

    private final DeploymentZoneDrawPlugin deploymentZoneDrawer = new DeploymentZoneDrawPlugin();

    /**
     * Creates and lays out a new Board Editor frame.
     */
    public BoardEditorPanel(MegaMekController c) {
        controller = c;
        try {
            bv = new BoardView(game, controller, null, 0);
            bv.addOverlay(new KeyBindingsOverlay(bv));
            bv.addOverlay(new TraceOverlay(bv));
            bv.setUseLosTool(false);
            bv.setDisplayInvalidFields(true);
            bv.setTooltipProvider(new BoardEditorTooltip(bv));
            bvc = bv.getComponent(true);
            bv.addHexDrawPlugin(deploymentZoneDrawer);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(frame,
                  Messages.getString("BoardEditor.CouldNotInitialize") + e,
                  Messages.getString("BoardEditor.FatalError"),
                  JOptionPane.ERROR_MESSAGE);
            frame.dispose();
        }

        // Add a mouse listener for mouse button release
        // to handle Undo
        bv.getPanel().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON1) {
                    // Act only if the user actually drew something
                    if ((currentUndoSet != null) && !currentUndoSet.isEmpty()) {
                        // Since this draw action is finished, push the
                        // drawn hexes onto the Undo Stack and get ready
                        // for a new draw action
                        undoStack.push(currentUndoSet);
                        currentUndoSet = null;
                        buttonUndo.setEnabled(true);
                        // Drawing something disables any redo actions
                        redoStack.clear();
                        buttonRedo.setEnabled(false);
                        // When Undo (without Redo) has been used after saving and the user draws on the board, then
                        // it can no longer know if it's been returned to the saved state, and it will always be
                        // treated as changed.
                        if (savedUndoStackSize > undoStack.size()) {
                            canReturnToSaved = false;
                        }
                        hasChanges = !canReturnToSaved || (undoStack.size() != savedUndoStackSize);
                    }
                    // Mark the title when the board has changes
                    setFrameTitle();
                }
            }
        });
        bv.addBoardViewListener(new BoardViewListenerAdapter() {
            @Override
            public void hexMoused(BoardViewEvent b) {
                Coords c = b.getCoords();
                // return if there are no or no valid coords or if we click the same hex again unless Raise/Lower
                // Terrain is active which should let us click the same hex
                if ((c == null) || (c.equals(lastClicked) && (paintMode() != PaintMode.LOWER_RAISE_HEX_LEVEL))
                      || !board.contains(c)) {
                    return;
                }
                lastClicked = c;
                bv.cursor(c);
                boolean isALT = (b.getModifiers() & InputEvent.ALT_DOWN_MASK) != 0;
                boolean isSHIFT = (b.getModifiers() & InputEvent.SHIFT_DOWN_MASK) != 0;
                boolean isCTRL = (b.getModifiers() & InputEvent.CTRL_DOWN_MASK) != 0;
                boolean isLMB = (b.getButton() == MouseEvent.BUTTON1);

                // Raise/Lower Terrain is selected
                if (paintMode() == PaintMode.LOWER_RAISE_HEX_LEVEL) {
                    // Mouse Button released
                    if (b.getType() == BoardViewEvent.BOARD_HEX_CLICKED) {
                        hexLevelToDraw = -1000;
                        isDragging = false;
                    }

                    // Mouse Button clicked or dragged
                    if ((b.getType() == BoardViewEvent.BOARD_HEX_DRAGGED) && isLMB) {
                        if (!isDragging) {
                            hexLevelToDraw = board.getHex(c).getLevel();
                            if (isALT) {
                                hexLevelToDraw--;
                            } else if (isSHIFT) {
                                hexLevelToDraw++;
                            }
                            isDragging = true;
                        }
                    }

                    // CORRECTION, click outside the board then drag inside???
                    if (hexLevelToDraw != -1000) {
                        LinkedList<Coords> allBrushHexes = getBrushCoords(c);
                        for (Coords h : allBrushHexes) {
                            if (!buttonOOC.isSelected() || board.getHex(h).isClearHex()) {
                                saveToUndo(h);
                                relevelHex(h);
                            }
                        }
                    }
                    // ------- End Raise/Lower Terrain

                } else if (paintMode() == PaintMode.DEPLOYMENT_ZONE) {
                    if (isLMB || (b.getModifiers() & InputEvent.BUTTON1_DOWN_MASK) != 0) {
                        for (Coords h : getBrushCoords(c)) {
                            saveToUndo(h);
                            if (isCTRL) {
                                removeDeploymentZone(h, (int) deploymentZoneChooser.getValue());
                            } else {
                                addDeploymentZone(h, (int) deploymentZoneChooser.getValue());
                            }
                        }
                    }

                } else if (isLMB || (b.getModifiers() & InputEvent.BUTTON1_DOWN_MASK) != 0) {
                    // 'isLMB' is true if a button 1 is associated to a click or release but not
                    // while dragging.
                    // The left button down mask is checked because we could be dragging.

                    // Normal texture paint
                    if (isALT) { // ALT-Click
                        setCurrentHex(board.getHex(b.getCoords()));
                    } else {
                        for (Coords h : getBrushCoords(c)) {
                            // test if texture overwriting is active
                            if ((!buttonOOC.isSelected() || board.getHex(h).isClearHex()) && curHex.isValid(null)) {
                                saveToUndo(h);
                                if (isCTRL) { // CTRL-Click
                                    paintHex(h);
                                } else if (isSHIFT) { // SHIFT-Click
                                    addToHex(h);
                                } else { // Normal click
                                    retextureHex(h);
                                }
                            }
                        }
                    }
                }
            }
        });

        setupEditorPanel();
        setupFrame();
        frame.setVisible(true);
        if (GUIPreferences.getInstance().getNagForMapEdReadme()) {
            String title = Messages.getString("BoardEditor.readme.title");
            String body = Messages.getString("BoardEditor.readme.message");
            ConfirmDialog confirm = new ConfirmDialog(frame, title, body, true);
            confirm.setVisible(true);
            if (!confirm.getShowAgain()) {
                GUIPreferences.getInstance().setNagForMapEdReadme(false);
            }
            if (confirm.getAnswer()) {
                showHelp();
            }
        }
    }

    /**
     * Sets up the frame that will display the editor.
     */
    private void setupFrame() {
        setFrameTitle();
        frame.add(bvc, BorderLayout.CENTER);
        frame.add(this, BorderLayout.EAST);
        menuBar.addActionListener(this);
        frame.setJMenuBar(menuBar);
        if (GUIPreferences.getInstance().getWindowSizeHeight() != 0) {
            frame.setLocation(GUIPreferences.getInstance().getWindowPosX(),
                  GUIPreferences.getInstance().getWindowPosY());
            frame.setSize(GUIPreferences.getInstance().getWindowSizeWidth(),
                  GUIPreferences.getInstance().getWindowSizeHeight());
        } else {
            frame.setSize(800, 600);
        }

        frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                // When the board has changes, ask the user
                if (hasChanges && (showSavePrompt() == DialogResult.CANCELLED)) {
                    return;
                }
                // otherwise: exit the Map Editor
                minimapW.setVisible(false);
                if (controller != null) {
                    controller.removeAllActions();
                    controller.boardEditor = null;
                }
                bv.dispose();
                frame.dispose();
            }
        });
    }

    /**
     * Shows a prompt to save the current board. When the board is actually saved or the user presses "No" (don't want
     * to save), returns DialogResult.CONFIRMED. In this case, the action (loading a board or leaving the board editor)
     * that led to this prompt may be continued. In all other cases, returns DialogResult.CANCELLED, meaning the action
     * should not be continued.
     *
     * @return DialogResult.CANCELLED (cancel action) or CONFIRMED (continue action)
     */
    private DialogResult showSavePrompt() {
        ignoreHotKeys = true;
        int savePrompt = JOptionPane.showConfirmDialog(null,
              Messages.getString("BoardEditor.exitprompt"),
              Messages.getString("BoardEditor.exittitle"),
              JOptionPane.YES_NO_CANCEL_OPTION,
              JOptionPane.WARNING_MESSAGE);
        ignoreHotKeys = false;
        // When the user cancels or did not actually save the board, don't load anything
        if (((savePrompt == JOptionPane.YES_OPTION) && !boardSave(false)) ||
              (savePrompt == JOptionPane.CANCEL_OPTION) ||
              (savePrompt == JOptionPane.CLOSED_OPTION)) {
            return DialogResult.CANCELLED;
        } else {
            return DialogResult.CONFIRMED;
        }
    }

    /**
     * Sets up Scaling Icon Buttons
     */
    private ScalingIconButton prepareButton(String iconName, String buttonName, List<ScalingIconButton> bList,
          int width) {
        // Get the normal icon
        File file = new MegaMekFile(Configuration.widgetsDir(), "/MapEditor/" + iconName + ".png").getFile();
        Image imageButton = ImageUtil.loadImageFromFile(file.getAbsolutePath());
        if (imageButton == null) {
            imageButton = ImageUtil.failStandardImage();
        }
        ScalingIconButton button = new ScalingIconButton(imageButton, width);

        // Get the hover icon
        file = new MegaMekFile(Configuration.widgetsDir(), "/MapEditor/" + iconName + "_H.png").getFile();
        imageButton = ImageUtil.loadImageFromFile(file.getAbsolutePath());
        button.setRolloverImage(imageButton);

        // Get the disabled icon, if any
        file = new MegaMekFile(Configuration.widgetsDir(), "/MapEditor/" + iconName + "_G.png").getFile();
        imageButton = ImageUtil.loadImageFromFile(file.getAbsolutePath());
        button.setDisabledImage(imageButton);

        String tt = Messages.getString("BoardEditor." + iconName + "TT");
        if (!tt.isBlank()) {
            button.setToolTipText(tt);
        }
        button.setMargin(new Insets(0, 0, 0, 0));
        if (bList != null) {
            bList.add(button);
        }
        button.addActionListener(this);
        return button;
    }

    /**
     * Sets up Scaling Icon ToggleButtons
     */
    private ScalingIconToggleButton prepareToggleButton(String iconName,
          List<ScalingIconToggleButton> bList, int width) {
        // Get the normal icon
        File file = new MegaMekFile(Configuration.widgetsDir(), "/MapEditor/" + iconName + ".png").getFile();
        Image imageButton = ImageUtil.loadImageFromFile(file.getAbsolutePath());
        if (imageButton == null) {
            imageButton = ImageUtil.failStandardImage();
        }
        ScalingIconToggleButton button = new ScalingIconToggleButton(imageButton, width);

        // Get the hover icon
        file = new MegaMekFile(Configuration.widgetsDir(), "/MapEditor/" + iconName + "_H.png").getFile();
        imageButton = ImageUtil.loadImageFromFile(file.getAbsolutePath());
        button.setRolloverImage(imageButton);

        // Get the selected icon, if any
        file = new MegaMekFile(Configuration.widgetsDir(), "/MapEditor/" + iconName + "_S.png").getFile();
        imageButton = ImageUtil.loadImageFromFile(file.getAbsolutePath());
        button.setSelectedImage(imageButton);

        button.setToolTipText(Messages.getString("BoardEditor." + iconName + "TT"));
        if (bList != null) {
            bList.add(button);
        }
        button.addActionListener(this);
        return button;
    }

    /**
     * Sets up the editor panel, which goes on the right of the map and has controls for editing the current square.
     */
    private void setupEditorPanel() {
        // Help Texts
        labHelp1.addMouseListener(clickToHide);
        labHelp2.addMouseListener(clickToHide);
        labHelp1.setAlignmentX(Component.CENTER_ALIGNMENT);
        labHelp2.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Buttons to ease setting common terrain types
        buttonLW = prepareButton("ButtonLW", "Woods", terrainButtons, BASE_TERRAINBUTTON_ICON_WIDTH);
        buttonLJ = prepareButton("ButtonLJ", "Jungle", terrainButtons, BASE_TERRAINBUTTON_ICON_WIDTH);
        buttonOW = prepareButton("ButtonLLW", "Low Woods", terrainButtons, BASE_TERRAINBUTTON_ICON_WIDTH);
        buttonOJ = prepareButton("ButtonLLJ", "Low Jungle", terrainButtons, BASE_TERRAINBUTTON_ICON_WIDTH);
        buttonWa = prepareButton("ButtonWa", "Water", terrainButtons, BASE_TERRAINBUTTON_ICON_WIDTH);
        buttonSw = prepareButton("ButtonSw", "Swamp", terrainButtons, BASE_TERRAINBUTTON_ICON_WIDTH);
        buttonRo = prepareButton("ButtonRo", "Rough", terrainButtons, BASE_TERRAINBUTTON_ICON_WIDTH);
        buttonMd = prepareButton("ButtonMd", "Mud", terrainButtons, BASE_TERRAINBUTTON_ICON_WIDTH);
        buttonPv = prepareButton("ButtonPv", "Pavement", terrainButtons, BASE_TERRAINBUTTON_ICON_WIDTH);
        buttonSn = prepareButton("ButtonSn", "Snow", terrainButtons, BASE_TERRAINBUTTON_ICON_WIDTH);
        buttonBu = prepareButton("ButtonBu", "Buildings", terrainButtons, BASE_TERRAINBUTTON_ICON_WIDTH);
        buttonRd = prepareButton("ButtonRd", "Roads", terrainButtons, BASE_TERRAINBUTTON_ICON_WIDTH);
        buttonBr = prepareButton("ButtonBr", "Bridges", terrainButtons, BASE_TERRAINBUTTON_ICON_WIDTH);
        buttonFT = prepareButton("ButtonFT", "Fuel Tanks", terrainButtons, BASE_TERRAINBUTTON_ICON_WIDTH);
        buttonIc = prepareButton("ButtonIc", "Ice", terrainButtons, BASE_TERRAINBUTTON_ICON_WIDTH);
        buttonTu = prepareButton("ButtonTu", "Tundra", terrainButtons, BASE_TERRAINBUTTON_ICON_WIDTH);
        buttonMg = prepareButton("ButtonMg", "Magma", terrainButtons, BASE_TERRAINBUTTON_ICON_WIDTH);
        buttonCl = prepareButton("ButtonCl", "Clear", terrainButtons, BASE_TERRAINBUTTON_ICON_WIDTH);

        buttonBrush1 = prepareToggleButton("ButtonHex1", brushButtons, BASE_ARROWBUTTON_ICON_WIDTH);
        buttonBrush2 = prepareToggleButton("ButtonHex7", brushButtons, BASE_ARROWBUTTON_ICON_WIDTH);
        buttonBrush3 = prepareToggleButton("ButtonHex19", brushButtons, BASE_ARROWBUTTON_ICON_WIDTH);
        ButtonGroup brushGroup = new ButtonGroup();
        brushGroup.add(buttonBrush1);
        brushGroup.add(buttonBrush2);
        brushGroup.add(buttonBrush3);
        buttonOOC = prepareToggleButton("ButtonOOC", brushButtons, BASE_ARROWBUTTON_ICON_WIDTH);

        ScalingIconToggleButton buttonPaintTerrain = prepareToggleButton("ButtonPaintTerrain", paintModeButtons,
              BASE_ARROWBUTTON_ICON_WIDTH);
        buttonRaiseLower = prepareToggleButton("ButtonUpDn", paintModeButtons, BASE_ARROWBUTTON_ICON_WIDTH);
        buttonDeployZone = prepareToggleButton("ButtonDepl", paintModeButtons, BASE_ARROWBUTTON_ICON_WIDTH);
        ButtonGroup paintModeGroup = new ButtonGroup();
        paintModeGroup.add(buttonPaintTerrain);
        paintModeGroup.add(buttonRaiseLower);
        paintModeGroup.add(buttonDeployZone);
        buttonPaintTerrain.setSelected(true);

        buttonUndo = prepareButton("ButtonUndo", "Undo", undoButtons, BASE_ARROWBUTTON_ICON_WIDTH);
        buttonRedo = prepareButton("ButtonRedo", "Redo", undoButtons, BASE_ARROWBUTTON_ICON_WIDTH);
        buttonUndo.setEnabled(false);
        buttonRedo.setEnabled(false);
        buttonUndo.setActionCommand(ClientGUI.BOARD_UNDO);
        buttonRedo.setActionCommand(ClientGUI.BOARD_REDO);

        MouseWheelListener wheelListener = e -> {
            int terrain;
            if (e.getSource() == buttonRo) {
                terrain = Terrains.ROUGH;
            } else if (e.getSource() == buttonSw) {
                terrain = Terrains.SWAMP;
            } else if (e.getSource() == buttonWa) {
                terrain = Terrains.WATER;
            } else if (e.getSource() == buttonLW) {
                terrain = Terrains.WOODS;
            } else if (e.getSource() == buttonLJ) {
                terrain = Terrains.JUNGLE;
            } else if (e.getSource() == buttonOW) {
                terrain = Terrains.WOODS;
            } else if (e.getSource() == buttonOJ) {
                terrain = Terrains.JUNGLE;
            } else if (e.getSource() == buttonMd) {
                terrain = Terrains.MUD;
            } else if (e.getSource() == buttonPv) {
                terrain = Terrains.PAVEMENT;
            } else if (e.getSource() == buttonIc) {
                terrain = Terrains.ICE;
            } else if (e.getSource() == buttonSn) {
                terrain = Terrains.SNOW;
            } else if (e.getSource() == buttonTu) {
                terrain = Terrains.TUNDRA;
            } else if (e.getSource() == buttonMg) {
                terrain = Terrains.MAGMA;
            } else {
                return;
            }

            Hex saveHex = curHex.duplicate();
            // change the terrain level by wheel direction if present,
            // or set to 1 if not present
            int newLevel = 1;
            if (curHex.containsTerrain(terrain)) {
                newLevel = curHex.terrainLevel(terrain) + (e.getWheelRotation() < 0 ? 1 : -1);
            } else if (!e.isShiftDown()) {
                curHex.removeAllTerrains();
            }
            addSetTerrainEasy(terrain, newLevel);
            // Add or adapt elevation helper terrain for foliage
            // When the elevation was 1, it stays 1 (L1 Foliage, TO p.36)
            // Otherwise, it is set to 3 for Ultra W/J and 2 otherwise (TW foliage)
            if ((terrain == Terrains.WOODS) || (terrain == Terrains.JUNGLE)) {
                int elev = curHex.terrainLevel(Terrains.FOLIAGE_ELEV);
                if ((elev != 1) && (newLevel == 3)) {
                    elev = 3;
                } else if (elev != 1) {
                    elev = 2;
                }
                curHex.addTerrain(new Terrain(Terrains.FOLIAGE_ELEV, elev));
            }
            // Reset the terrain to the former state
            // if the new would be invalid.
            if (!curHex.isValid(null)) {
                curHex = saveHex;
            }

            refreshTerrainList();
            repaintWorkingHex();
        };

        buttonSw.addMouseWheelListener(wheelListener);
        buttonWa.addMouseWheelListener(wheelListener);
        buttonRo.addMouseWheelListener(wheelListener);
        buttonLJ.addMouseWheelListener(wheelListener);
        buttonLW.addMouseWheelListener(wheelListener);
        buttonOJ.addMouseWheelListener(wheelListener);
        buttonOW.addMouseWheelListener(wheelListener);
        buttonMd.addMouseWheelListener(wheelListener);
        buttonPv.addMouseWheelListener(wheelListener);
        buttonSn.addMouseWheelListener(wheelListener);
        buttonIc.addMouseWheelListener(wheelListener);
        buttonTu.addMouseWheelListener(wheelListener);
        buttonMg.addMouseWheelListener(wheelListener);

        // Mouse wheel behaviour for the BUILDINGS button
        // Always ADDS the building.
        buttonBu.addMouseWheelListener(e -> {
            // If we don't have at least one of the building values, overwrite the current
            // hex
            if (!curHex.containsTerrain(Terrains.BLDG_CF) &&
                  !curHex.containsTerrain(Terrains.BLDG_ELEV) &&
                  !curHex.containsTerrain(Terrains.BUILDING)) {
                curHex.removeAllTerrains();
            }
            // Restore mandatory building parts if some are missing
            setBasicBuilding(false);
            int wheelDir = (e.getWheelRotation() < 0) ? 1 : -1;

            if (e.isShiftDown()) {
                int oldLevel = curHex.getTerrain(Terrains.BLDG_CF).getLevel();
                int newLevel = Math.max(10, oldLevel + (wheelDir * 5));
                curHex.addTerrain(new Terrain(Terrains.BLDG_CF, newLevel));
            } else if (e.isControlDown()) {
                int oldLevel = curHex.getTerrain(Terrains.BUILDING).getLevel();
                int newLevel = Math.max(1, Math.min(4, oldLevel + wheelDir)); // keep between 1 and 4

                if (newLevel != oldLevel) {
                    Terrain curTerr = curHex.getTerrain(Terrains.BUILDING);
                    curHex.addTerrain(new Terrain(Terrains.BUILDING,
                          newLevel,
                          curTerr.hasExitsSpecified(),
                          curTerr.getExits()));

                    // Set the CF to the appropriate standard value *IF* it is the appropriate value
                    // now,
                    // i.e. if the user has not manually set it to something else
                    int curCF = curHex.getTerrain(Terrains.BLDG_CF).getLevel();
                    if (curCF == IBuilding.getDefaultCF(oldLevel)) {
                        curHex.addTerrain(new Terrain(Terrains.BLDG_CF, IBuilding.getDefaultCF(newLevel)));
                    }
                }
            } else {
                int oldLevel = curHex.getTerrain(Terrains.BLDG_ELEV).getLevel();
                int newLevel = Math.max(1, oldLevel + wheelDir);
                curHex.addTerrain(new Terrain(Terrains.BLDG_ELEV, newLevel));
            }

            refreshTerrainList();
            repaintWorkingHex();
        });

        // Mouse wheel behaviour for the BRIDGE button
        buttonBr.addMouseWheelListener(e -> {
            // If we don't have at least one of the bridge values, overwrite the current hex
            if (!curHex.containsTerrain(Terrains.BRIDGE_CF) &&
                  !curHex.containsTerrain(Terrains.BRIDGE_ELEV) &&
                  !curHex.containsTerrain(Terrains.BRIDGE)) {
                curHex.removeAllTerrains();
            }
            setBasicBridge();
            int wheelDir = (e.getWheelRotation() < 0) ? 1 : -1;
            int terrainType;
            int newLevel;

            if (e.isShiftDown()) {
                terrainType = Terrains.BRIDGE_CF;
                int oldLevel = curHex.getTerrain(terrainType).getLevel();
                newLevel = Math.max(10, oldLevel + wheelDir * 10);
                curHex.addTerrain(new Terrain(terrainType, newLevel));
            } else if (e.isControlDown()) {
                Terrain terrain = curHex.getTerrain(Terrains.BRIDGE);
                boolean hasExits = terrain.hasExitsSpecified();
                int exits = terrain.getExits();
                newLevel = Math.max(1, terrain.getLevel() + wheelDir);
                curHex.addTerrain(new Terrain(Terrains.BRIDGE, newLevel, hasExits, exits));
            } else {
                terrainType = Terrains.BRIDGE_ELEV;
                int oldLevel = curHex.getTerrain(terrainType).getLevel();
                newLevel = Math.max(0, oldLevel + wheelDir);
                curHex.addTerrain(new Terrain(terrainType, newLevel));
            }

            refreshTerrainList();
            repaintWorkingHex();
        });

        // Mouse wheel behaviour for the FUELTANKS button
        buttonFT.addMouseWheelListener(e -> {
            // If we don't have at least one of the fuel tank values, overwrite the current
            // hex
            if (!curHex.containsTerrain(Terrains.FUEL_TANK) &&
                  !curHex.containsTerrain(Terrains.FUEL_TANK_CF) &&
                  !curHex.containsTerrain(Terrains.FUEL_TANK_ELEV) &&
                  !curHex.containsTerrain(Terrains.FUEL_TANK_MAGN)) {
                curHex.removeAllTerrains();
            }
            setBasicFuelTank();
            int wheelDir = (e.getWheelRotation() < 0) ? 1 : -1;
            int terrainType;
            int newLevel;

            if (e.isShiftDown()) {
                terrainType = Terrains.FUEL_TANK_CF;
                int oldLevel = curHex.getTerrain(terrainType).getLevel();
                newLevel = Math.max(10, oldLevel + wheelDir * 10);
            } else if (e.isControlDown()) {
                terrainType = Terrains.FUEL_TANK_MAGN;
                int oldLevel = curHex.getTerrain(terrainType).getLevel();
                newLevel = Math.max(10, oldLevel + wheelDir * 10);
            } else {
                terrainType = Terrains.FUEL_TANK_ELEV;
                int oldLevel = curHex.getTerrain(terrainType).getLevel();
                newLevel = Math.max(1, oldLevel + wheelDir);
            }

            curHex.addTerrain(new Terrain(terrainType, newLevel));
            refreshTerrainList();
            repaintWorkingHex();
        });

        FixedYPanel terrainButtonPanel = new FixedYPanel(new GridLayout(0, 4, 2, 2));
        addManyButtons(terrainButtonPanel, terrainButtons);

        FixedYPanel brushButtonPanel = new FixedYPanel(new GridLayout(0, 3, 2, 2));
        addManyButtons(brushButtonPanel, brushButtons);
        buttonBrush1.setSelected(true);

        FixedYPanel paintModeButtonPanel = new FixedYPanel(new GridLayout(0, 3, 2, 2));
        addManyButtons(paintModeButtonPanel, paintModeButtons);
        deploymentZoneChooser.addChangeListener(e -> changeSelectedDeploymentZone());
        deploymentZoneChooser.setEnabled(false);
        buttonDeployZone.addActionListener(e -> deployZoneToggled());
        buttonPaintTerrain.addActionListener(e -> deployZoneToggled());
        buttonRaiseLower.addActionListener(e -> deployZoneToggled());
        buttonBrush1.setSelected(true);

        FixedYPanel undoButtonPanel = new FixedYPanel(new GridLayout(1, 2, 2, 2));
        addManyButtons(undoButtonPanel, List.of(buttonUndo, buttonRedo));

        // Hex Elevation Control
        texElev = new EditorTextField("0", 3);
        texElev.addActionListener(this);
        texElev.getDocument().addDocumentListener(this);

        butElevUp = prepareButton("ButtonHexUP", "Raise Hex Elevation", null, BASE_ARROWBUTTON_ICON_WIDTH);
        butElevUp.setName("butElevUp");
        butElevUp.setToolTipText(Messages.getString("BoardEditor.butElevUp.toolTipText"));

        butElevDown = prepareButton("ButtonHexDN", "Lower Hex Elevation", null, BASE_ARROWBUTTON_ICON_WIDTH);
        butElevDown.setName("butElevDown");
        butElevDown.setToolTipText(Messages.getString("BoardEditor.butElevDown.toolTipText"));

        // Terrain List
        lisTerrainRenderer = new TerrainListRenderer();
        lisTerrain = new JList<>(new DefaultListModel<>());
        lisTerrain.addListSelectionListener(this);
        lisTerrain.setCellRenderer(lisTerrainRenderer);
        lisTerrain.setVisibleRowCount(6);
        lisTerrain.setPrototypeCellValue(new TerrainTypeHelper(new Terrain(WATER, 2)));
        lisTerrain.setFixedCellWidth(180);
        refreshTerrainList();

        // Terrain List, Preview, Delete
        FixedYPanel panlisHex = new FixedYPanel(new FlowLayout(FlowLayout.LEFT, 4, 4));
        butDelTerrain = prepareButton("buttonRemT", "Delete Terrain", null, BASE_ARROWBUTTON_ICON_WIDTH);
        butDelTerrain.setEnabled(false);
        canHex = new HexCanvas(this);
        panlisHex.add(butDelTerrain);
        panlisHex.add(new JScrollPane(lisTerrain));
        panlisHex.add(canHex);

        // Build the terrain list for the chooser ComboBox,
        // excluding terrains that are handled internally
        ArrayList<TerrainHelper> tList = new ArrayList<>();
        for (int i = 1; i < Terrains.SIZE; i++) {
            if (!Terrains.AUTOMATIC.contains(i) && (i != DEPLOYMENT_ZONE)) {
                tList.add(new TerrainHelper(i));
            }
        }
        TerrainHelper[] terrains = new TerrainHelper[tList.size()];
        tList.toArray(terrains);
        Arrays.sort(terrains);
        texTerrainLevel = new EditorTextField("0", 2, 0);
        texTerrainLevel.addActionListener(this);
        texTerrainLevel.getDocument().addDocumentListener(this);
        choTerrainType = new JComboBox<>(terrains);
        TerrainListRenderer renderer = new TerrainListRenderer();
        renderer.setTerrains(terrains);
        choTerrainType.setRenderer(renderer);
        // Selecting a terrain type in the Dropdown should deselect
        // all in the terrain overview list except when selected from there
        choTerrainType.addActionListener(e -> {
            if (!terrListBlocker) {
                lisTerrain.clearSelection();

                // if we've selected DEPLOYMENT ZONE, disable the "exits" buttons and make the "exits" popup point to
                // a multi-select list that lets the user choose which deployment zones will be flagged here
                // otherwise, re-enable all the buttons and reset the "exits" popup to its normal behavior
                if (((TerrainHelper) Objects.requireNonNull(choTerrainType.getSelectedItem())).terrainType() ==
                      Terrains.DEPLOYMENT_ZONE) {
                    butExitUp.setEnabled(false);
                    butExitDown.setEnabled(false);
                    texTerrExits.setEnabled(false);
                    cheTerrExitSpecified.setEnabled(false);
                    cheTerrExitSpecified.setText("Zones");// Messages.getString("BoardEditor.deploymentZoneIDs"));
                    butTerrExits.setActionCommand(CMD_EDIT_DEPLOYMENT_ZONES);
                } else {
                    butExitUp.setEnabled(true);
                    butExitDown.setEnabled(true);
                    texTerrExits.setEnabled(true);
                    cheTerrExitSpecified.setEnabled(true);
                    cheTerrExitSpecified.setText(Messages.getString("BoardEditor.cheTerrExitSpecified"));
                    butTerrExits.setActionCommand("");
                }
            }
        });
        butAddTerrain = new JButton(Messages.getString("BoardEditor.butAddTerrain"));
        butTerrUp = prepareButton("ButtonTLUP", "Increase Terrain Level", null, BASE_ARROWBUTTON_ICON_WIDTH);
        butTerrDown = prepareButton("ButtonTLDN", "Decrease Terrain Level", null, BASE_ARROWBUTTON_ICON_WIDTH);

        // Exits
        cheTerrExitSpecified = new JCheckBox(Messages.getString("BoardEditor.cheTerrExitSpecified"));
        cheTerrExitSpecified.addActionListener(this);
        butTerrExits = prepareButton("ButtonExitA",
              Messages.getString("BoardEditor.butTerrExits"),
              null,
              BASE_ARROWBUTTON_ICON_WIDTH);
        texTerrExits = new EditorTextField("0", 2, 0);
        texTerrExits.addActionListener(this);
        texTerrExits.getDocument().addDocumentListener(this);
        butExitUp = prepareButton("ButtonEXUP", "Increase Exit / Gfx", null, BASE_ARROWBUTTON_ICON_WIDTH);
        butExitDown = prepareButton("ButtonEXDN", "Decrease Exit / Gfx", null, BASE_ARROWBUTTON_ICON_WIDTH);

        // Copy and Paste
        FixedYPanel panCopyPaste = new FixedYPanel(new FlowLayout(FlowLayout.RIGHT, 4, 4));
        panCopyPaste.add(pasteButton);
        panCopyPaste.add(copyButton);
        copyButton.addActionListener(e -> copyWorkingHexToClipboard());
        pasteButton.addActionListener(e -> pasteFromClipboard());

        // Arrows and text fields for type and exits
        JPanel panUP = new JPanel(new GridLayout(1, 0, 4, 4));
        panUP.add(butTerrUp);
        panUP.add(butExitUp);
        panUP.add(butTerrExits);
        JPanel panTex = new JPanel(new GridLayout(1, 0, 4, 4));
        panTex.add(texTerrainLevel);
        panTex.add(texTerrExits);
        panTex.add(cheTerrExitSpecified);
        JPanel panDN = new JPanel(new GridLayout(1, 0, 4, 4));
        panDN.add(butTerrDown);
        panDN.add(butExitDown);
        panDN.add(Box.createHorizontalStrut(5));

        // Auto Exits to Pavement
        cheRoadsAutoExit = new JCheckBox(Messages.getString("BoardEditor.cheRoadsAutoExit"));
        cheRoadsAutoExit.addItemListener(this);
        cheRoadsAutoExit.setSelected(true);

        // Theme
        JPanel panTheme = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 4));
        choTheme = new JComboBox<>();
        TilesetManager tileMan = bv.getTilesetManager();
        Set<String> themes = tileMan.getThemes();
        for (String s : themes) {
            choTheme.addItem(s);
        }
        choTheme.addActionListener(this);
        panTheme.add(labTheme);
        panTheme.add(choTheme);

        // The hex settings panel (elevation, theme)
        panelHexSettings.setBorder(new TitledBorder("Hex Settings"));
        panelHexSettings.add(butElevUp);
        panelHexSettings.add(texElev);
        panelHexSettings.add(butElevDown);
        panelHexSettings.add(panTheme);

        // The terrain settings panel (type, level, exits)
        panelTerrSettings.setBorder(new TitledBorder("Terrain Settings"));
        panelTerrSettings.add(Box.createVerticalStrut(5));
        panelTerrSettings.add(panUP);

        panelTerrSettings.add(choTerrainType);
        panelTerrSettings.add(panTex);

        panelTerrSettings.add(butAddTerrain);
        panelTerrSettings.add(panDN);

        // The board settings panel (Auto exit roads to pavement)
        panelBoardSettings.setBorder(new TitledBorder("Board Settings"));
        panelBoardSettings.add(cheRoadsAutoExit);

        // Board Buttons (Save, Load...)
        JButton butBoardNew = new JButton(Messages.getString("BoardEditor.butBoardNew"));
        butBoardNew.setActionCommand(ClientGUI.BOARD_NEW);

        JButton butExpandMap = new JButton(Messages.getString("BoardEditor.butExpandMap"));
        butExpandMap.setActionCommand(ClientGUI.BOARD_RESIZE);

        JButton butBoardOpen = new JButton(Messages.getString("BoardEditor.butBoardOpen"));
        butBoardOpen.setActionCommand(ClientGUI.BOARD_OPEN);

        JButton butBoardSave = new JButton(Messages.getString("BoardEditor.butBoardSave"));
        butBoardSave.setActionCommand(ClientGUI.BOARD_SAVE);

        JButton butBoardSaveAs = new JButton(Messages.getString("BoardEditor.butBoardSaveAs"));
        butBoardSaveAs.setActionCommand(ClientGUI.BOARD_SAVE_AS);

        JButton butBoardSaveAsImage = new JButton(Messages.getString("BoardEditor.butBoardSaveAsImage"));
        butBoardSaveAsImage.setActionCommand(ClientGUI.BOARD_SAVE_AS_IMAGE);

        JButton butBoardValidate = new JButton(Messages.getString("BoardEditor.butBoardValidate"));
        butBoardValidate.setActionCommand(ClientGUI.BOARD_VALIDATE);

        butSourceFile = new JButton(Messages.getString("BoardEditor.butSourceFile"));
        butSourceFile.setActionCommand(ClientGUI.BOARD_SOURCE_FILE);

        addManyActionListeners(butBoardValidate, butBoardSaveAsImage, butBoardSaveAs, butBoardSave);
        addManyActionListeners(butBoardOpen, butExpandMap, butBoardNew);
        addManyActionListeners(butDelTerrain, butAddTerrain, butSourceFile);

        JPanel panButtons = new JPanel(new GridLayout(3, 3, 2, 2));
        addManyButtons(panButtons,
              List.of(butBoardNew,
                    butBoardSave,
                    butBoardOpen,
                    butExpandMap,
                    butBoardSaveAs,
                    butBoardSaveAsImage,
                    butBoardValidate));
        if (Desktop.isDesktopSupported()) {
            panButtons.add(butSourceFile);
        }

        var deploymentZoneChooserPanel = new FixedYPanel();
        deploymentZoneChooserPanel.add(new JLabel("Deployment Zone: "));
        deploymentZoneChooserPanel.add(deploymentZoneChooser);

        // Arrange everything
        setLayout(new BorderLayout());
        Box centerPanel = Box.createVerticalBox();
        centerPanel.add(labHelp1);
        centerPanel.add(labHelp2);
        centerPanel.add(Box.createVerticalStrut(10));
        centerPanel.add(terrainButtonPanel);
        centerPanel.add(Box.createVerticalStrut(10));
        centerPanel.add(brushButtonPanel);
        centerPanel.add(Box.createVerticalStrut(10));
        centerPanel.add(paintModeButtonPanel);
        centerPanel.add(deploymentZoneChooserPanel);
        centerPanel.add(Box.createVerticalStrut(10));
        centerPanel.add(undoButtonPanel);
        centerPanel.add(Box.createVerticalGlue());
        centerPanel.add(panelBoardSettings);
        centerPanel.add(panelHexSettings);
        centerPanel.add(panelTerrSettings);
        centerPanel.add(panlisHex);
        centerPanel.add(panCopyPaste);
        var scrCenterPanel = new JScrollPane(centerPanel);
        scrCenterPanel.getVerticalScrollBar().setUnitIncrement(16);
        add(scrCenterPanel, BorderLayout.CENTER);
        add(panButtons, BorderLayout.PAGE_END);

        minimapW = new MinimapDialog(frame);
        minimapW.add(new MinimapPanel(minimapW, game, bv, null, null, 0));
        minimapW.setVisible(guip.getMinimapEnabled());
    }

    /**
     * Returns coords that the active brush will paint on; returns only coords that are valid, i.e. on the board
     */
    private LinkedList<Coords> getBrushCoords(Coords center) {
        var result = new LinkedList<Coords>();
        // The center hex itself is always part of the brush
        result.add(center);
        // Add surrounding hexes for the big brush
        if (brushSize >= 2) {
            result.addAll(center.allAdjacent());
        }
        if (brushSize == 3) {
            result.addAll(center.allAtDistance(2));
        }
        // Remove coords that are not on the board
        result.removeIf(c -> !board.contains(c));
        return result;
    }

    // Helper to shorten the code
    private void addManyActionListeners(JButton... buttons) {
        for (JButton button : buttons) {
            button.addActionListener(this);
        }
    }

    // Helper to shorten the code
    private void addManyButtons(JPanel panel, List<? extends AbstractButton> terrainButtons) {
        terrainButtons.forEach(panel::add);
    }

    /**
     * Save the hex at c into the current undo Set
     */
    private void saveToUndo(Coords c) {
        // Create a new set of hexes to save for undoing
        // This will be filled as long as the mouse is dragged
        if (currentUndoSet == null) {
            currentUndoSet = new HashSet<>();
            currentUndoCoords = new HashSet<>();
        }
        if (!currentUndoCoords.contains(c)) {
            Hex hex = board.getHex(c).duplicate();
            // Newly drawn board hexes do not know their Coords
            hex.setCoords(c);
            currentUndoSet.add(hex);
            currentUndoCoords.add(c);
        }
    }

    private void resetUndo() {
        currentUndoSet = null;
        currentUndoCoords = null;
        undoStack.clear();
        redoStack.clear();
        buttonUndo.setEnabled(false);
        buttonRedo.setEnabled(false);
    }

    /**
     * Changes the hex level at Coords c. Expects c to be on the board.
     */
    private void relevelHex(Coords c) {
        Hex newHex = board.getHex(c).duplicate();
        newHex.setLevel(hexLevelToDraw);
        board.resetStoredElevation();
        board.setHex(c, newHex);

    }

    /**
     * Apply the current Hex to the Board at the specified location.
     */
    void paintHex(Coords c) {
        board.resetStoredElevation();
        Hex newHex = curHex.duplicate();
        // preserve deployment zones; these are painted in their own mode
        Terrain deploymentZone = board.getHex(c).getTerrain(DEPLOYMENT_ZONE);
        if (deploymentZone != null) {
            newHex.addTerrain(deploymentZone);
        }
        board.setHex(c, newHex);
    }

    /**
     * Apply the current Hex to the Board at the specified location.
     */
    public void retextureHex(Coords c) {
        if (board.contains(c)) {
            Hex newHex = curHex.duplicate();
            newHex.setLevel(board.getHex(c).getLevel());
            // preserve deployment zones; these are painted in their own mode
            Terrain deploymentZone = board.getHex(c).getTerrain(DEPLOYMENT_ZONE);
            if (deploymentZone != null) {
                newHex.addTerrain(deploymentZone);
            }
            board.resetStoredElevation();
            board.setHex(c, newHex);
        }
    }

    /**
     * Apply the current Hex to the Board at the specified location.
     */
    public void addToHex(Coords c) {
        if (board.contains(c)) {
            Hex newHex = curHex.duplicate();
            Hex oldHex = board.getHex(c);
            newHex.setLevel(oldHex.getLevel());
            int[] terrainTypes = oldHex.getTerrainTypes();
            for (int terrainID : terrainTypes) {
                if (!newHex.containsTerrain(terrainID) && oldHex.containsTerrain(terrainID)) {
                    newHex.addTerrain(oldHex.getTerrain(terrainID));
                }
            }
            newHex.setTheme(oldHex.getTheme());
            board.resetStoredElevation();
            board.setHex(c, newHex);
        }
    }

    /**
     * Apply the current Hex to the Board at the specified location.
     */
    public void addDeploymentZone(Coords c, int deploymentZoneToAdd) {
        if (board.contains(c)) {
            Hex hex = board.getHex(c);
            if (hex.containsTerrain(DEPLOYMENT_ZONE)) {
                var deploymentZones = hex.getTerrain(DEPLOYMENT_ZONE);
                int currentZones = deploymentZones.getExits();
                deploymentZones.setExits(currentZones | (1 << (deploymentZoneToAdd - 1)));
            } else {
                hex.addTerrain(new Terrain(DEPLOYMENT_ZONE, 0, true, 1 << (deploymentZoneToAdd - 1)));
            }
            board.setHex(c, hex);
        }
    }

    /**
     * Removes the given deployment zone from the Hex at Coord c. Does nothing if c is not on the board or the hex does
     * not have that deployment zone. Removes the deployment zone terrain entirely if no zones are left in that hex.
     */
    public void removeDeploymentZone(Coords c, int deploymentZoneToRemove) {
        if (board.contains(c) && board.getHex(c).containsTerrain(DEPLOYMENT_ZONE)) {
            var hex = board.getHex(c);
            var deploymentZones = hex.getTerrain(DEPLOYMENT_ZONE);
            int currentZones = deploymentZones.getExits();
            int newZones = currentZones & ~(1 << (deploymentZoneToRemove - 1));

            if (newZones == 0) {
                hex.removeTerrain(DEPLOYMENT_ZONE);
            } else {
                deploymentZones.setExits(newZones);
            }
            board.setHex(c, hex);
        }
    }

    /**
     * Sets the working hex to <code>hex</code>; used for mouse ALT-click (eyedropper function).
     *
     * @param hex hex to set.
     */
    void setCurrentHex(Hex hex) {
        curHex = hex.duplicate();
        curHex.removeTerrain(DEPLOYMENT_ZONE);
        texElev.setText(Integer.toString(curHex.getLevel()));
        refreshTerrainList();
        if (lisTerrain.getModel().getSize() > 0) {
            lisTerrain.setSelectedIndex(0);
            refreshTerrainFromList();
        }
        choTheme.setSelectedItem(curHex.getTheme());
        repaint();
        repaintWorkingHex();
    }

    private void repaintWorkingHex() {
        if (curHex != null) {
            TilesetManager tm = bv.getTilesetManager();
            tm.clearHex(curHex);
        }
        canHex.repaint();
        lastClicked = null;
    }

    /**
     * Refreshes the terrain list to match the current hex
     */
    private void refreshTerrainList() {
        TerrainTypeHelper selectedEntry = lisTerrain.getSelectedValue();
        ((DefaultListModel<TerrainTypeHelper>) lisTerrain.getModel()).removeAllElements();
        lisTerrainRenderer.setTerrainTypes(null);
        int[] terrainTypes = curHex.getTerrainTypes();
        List<TerrainTypeHelper> types = new ArrayList<>();
        for (final int terrainType : terrainTypes) {
            final Terrain terrain = curHex.getTerrain(terrainType);
            if ((terrain != null) && !Terrains.AUTOMATIC.contains(terrainType) && (terrainType != DEPLOYMENT_ZONE)) {
                final TerrainTypeHelper tth = new TerrainTypeHelper(terrain);
                types.add(tth);
            }
        }
        Collections.sort(types);
        for (final TerrainTypeHelper tth : types) {
            ((DefaultListModel<TerrainTypeHelper>) lisTerrain.getModel()).addElement(tth);
        }
        lisTerrainRenderer.setTerrainTypes(types);
        // Reselect the formerly selected terrain if possible
        if (selectedEntry != null) {
            selectTerrain(selectedEntry.getTerrain());
        }
    }

    /**
     * Returns a new instance of the terrain that is currently entered in the terrain input fields
     */
    private Terrain enteredTerrain() {
        int type = ((TerrainHelper) Objects.requireNonNull(choTerrainType.getSelectedItem())).terrainType();
        int level = texTerrainLevel.getNumber();
        // For the terrain subtypes that only add to a main terrain type exits make no
        // sense at all. Therefore, simply do not add them
        if ((type == Terrains.BLDG_ARMOR) ||
              (type == Terrains.BLDG_CF) ||
              (type == Terrains.BLDG_ELEV) ||
              (type == Terrains.BLDG_CLASS) ||
              (type == Terrains.BLDG_BASE_COLLAPSED) ||
              (type == Terrains.BLDG_BASEMENT_TYPE) ||
              (type == Terrains.BRIDGE_CF) ||
              (type == Terrains.BRIDGE_ELEV) ||
              (type == Terrains.FUEL_TANK_CF) ||
              (type == Terrains.FUEL_TANK_ELEV) ||
              (type == Terrains.FUEL_TANK_MAGN)) {
            return new Terrain(type, level, false, 0);
        } else {
            boolean exitsSpecified = cheTerrExitSpecified.isSelected();
            int exits = texTerrExits.getNumber();
            return new Terrain(type, level, exitsSpecified, exits);
        }
    }

    /**
     * Add or set the terrain to the list based on the fields.
     */
    private void addSetTerrain() {
        Terrain toAdd = enteredTerrain();
        if (((toAdd.getType() == Terrains.BLDG_ELEV) || (toAdd.getType() == Terrains.BRIDGE_ELEV)) &&
              (toAdd.getLevel() < 0)) {
            texTerrainLevel.setNumber(0);
            JOptionPane.showMessageDialog(frame,
                  Messages.getString("BoardEditor.BridgeBuildingElevError"),
                  Messages.getString("BoardEditor.invalidTerrainTitle"),
                  JOptionPane.ERROR_MESSAGE);
            return;
        }

        curHex.addTerrain(toAdd);

        noTextFieldUpdate = true;
        refreshTerrainList();
        repaintWorkingHex();
        noTextFieldUpdate = false;
    }

    /**
     * Cycle the terrain level (mouse wheel behavior) from the easy access buttons
     */
    private void addSetTerrainEasy(int type, int level) {
        boolean exitsSpecified = false;
        int exits = 0;
        Terrain present = curHex.getTerrain(type);
        if (present != null) {
            exitsSpecified = present.hasExitsSpecified();
            exits = present.getExits();
        }
        Terrain toAdd = new Terrain(type, level, exitsSpecified, exits);
        curHex.addTerrain(toAdd);
        refreshTerrainList();
        repaintWorkingHex();
    }

    /**
     * Sets valid basic Fuel Tank values as far as they are missing
     */
    private void setBasicFuelTank() {
        // There is only fuel_tank:1, so this can be set
        curHex.addTerrain(new Terrain(Terrains.FUEL_TANK, 1, true, 0));

        if (!curHex.containsTerrain(Terrains.FUEL_TANK_CF)) {
            curHex.addTerrain(new Terrain(Terrains.FUEL_TANK_CF, 40, false, 0));
        }

        if (!curHex.containsTerrain(Terrains.FUEL_TANK_ELEV)) {
            curHex.addTerrain(new Terrain(Terrains.FUEL_TANK_ELEV, 1, false, 0));
        }

        if (!curHex.containsTerrain(Terrains.FUEL_TANK_MAGN)) {
            curHex.addTerrain(new Terrain(Terrains.FUEL_TANK_MAGN, 100, false, 0));
        }

        refreshTerrainList();
        selectTerrain(new Terrain(Terrains.FUEL_TANK_ELEV, 1));
        repaintWorkingHex();
    }

    /**
     * Sets valid basic bridge values as far as they are missing
     */
    private void setBasicBridge() {
        if (!curHex.containsTerrain(Terrains.BRIDGE_CF)) {
            curHex.addTerrain(new Terrain(Terrains.BRIDGE_CF, 40, false, 0));
        }

        if (!curHex.containsTerrain(Terrains.BRIDGE_ELEV)) {
            curHex.addTerrain(new Terrain(Terrains.BRIDGE_ELEV, 1, false, 0));
        }

        if (!curHex.containsTerrain(Terrains.BRIDGE)) {
            curHex.addTerrain(new Terrain(Terrains.BRIDGE, 1, false, 0));
        }

        refreshTerrainList();
        selectTerrain(new Terrain(Terrains.BRIDGE_ELEV, 1));
        repaintWorkingHex();
    }

    /**
     * Sets valid basic Building values as far as they are missing
     */
    private void setBasicBuilding(boolean ALT_Held) {
        if (!curHex.containsTerrain(Terrains.BLDG_CF)) {
            curHex.addTerrain(new Terrain(Terrains.BLDG_CF, 15, false, 0));
        }

        if (!curHex.containsTerrain(Terrains.BLDG_ELEV)) {
            curHex.addTerrain(new Terrain(Terrains.BLDG_ELEV, 1, false, 0));
        }

        if (!curHex.containsTerrain(Terrains.BUILDING)) {
            curHex.addTerrain(new Terrain(Terrains.BUILDING, 1, ALT_Held, 0));
        }

        // When clicked with ALT, only toggle the exits
        if (ALT_Held) {
            Terrain curTerr = curHex.getTerrain(Terrains.BUILDING);
            curHex.addTerrain(new Terrain(Terrains.BUILDING,
                  curTerr.getLevel(),
                  !curTerr.hasExitsSpecified(),
                  curTerr.getExits()));
        }

        refreshTerrainList();
        selectTerrain(new Terrain(Terrains.BLDG_ELEV, 1));
        repaintWorkingHex();
    }

    /**
     * Set all the appropriate terrain fields to match the currently selected terrain in the list.
     */
    private void refreshTerrainFromList() {
        if (lisTerrain.isSelectionEmpty()) {
            butDelTerrain.setEnabled(false);
        } else {
            butDelTerrain.setEnabled(true);
            Terrain terrain = new Terrain(lisTerrain.getSelectedValue().getTerrain());
            terrain = curHex.getTerrain(terrain.getType());
            TerrainHelper terrainHelper = new TerrainHelper(terrain.getType());
            terrListBlocker = true;
            choTerrainType.setSelectedItem(terrainHelper);
            texTerrainLevel.setText(Integer.toString(terrain.getLevel()));
            setExitsState(terrain.hasExitsSpecified());
            texTerrExits.setNumber(terrain.getExits());
            terrListBlocker = false;
        }
    }

    /**
     * Updates the selected terrain in the terrain list if a terrain is actually selected
     */
    private void updateWhenSelected() {
        if (!lisTerrain.isSelectionEmpty()) {
            addSetTerrain();
        }
    }

    public void boardNew(boolean showDialog) {
        boolean userCancel = false;
        if (showDialog) {
            RandomMapDialog rmd = new RandomMapDialog(frame, this, null, mapSettings);
            userCancel = rmd.activateDialog(bv.getTilesetManager().getThemes());
        }
        if (!userCancel) {
            board = BoardUtilities.generateRandom(mapSettings);
            // "Initialize" all hexes to add internally handled terrains
            correctExits();
            game.setBoard(board);
            curBoardFile = null;
            choTheme.setSelectedItem(mapSettings.getTheme());
            setupUiFreshBoard();
        }
    }

    public void boardResize() {
        ResizeMapDialog emd = new ResizeMapDialog(frame, this, null, mapSettings);
        boolean userCancel = emd.activateDialog(bv.getTilesetManager().getThemes());
        if (!userCancel) {
            board = BoardUtilities.generateRandom(mapSettings);

            // Implant the old board
            int west = emd.getExpandWest();
            int north = emd.getExpandNorth();
            int east = emd.getExpandEast();
            int south = emd.getExpandSouth();
            board = implantOldBoard(game, west, north, east, south);

            game.setBoard(board);
            curBoardFile = null;
            setupUiFreshBoard();
        }
    }

    // When we resize a board, implant the old board's hexes where they should be in
    // the new board
    public Board implantOldBoard(Game game, int west, int north, int east, int south) {
        Board oldBoard = game.getBoard();
        for (int x = 0; x < oldBoard.getWidth(); x++) {
            for (int y = 0; y < oldBoard.getHeight(); y++) {
                int newX = x + west;
                int odd = x & 1 & west;
                int newY = y + north + odd;
                if (oldBoard.contains(x, y) && board.contains(newX, newY)) {
                    Hex oldHex = oldBoard.getHex(x, y);
                    Hex hex = board.getHex(newX, newY);
                    hex.removeAllTerrains();
                    hex.setLevel(oldHex.getLevel());
                    int[] terrainTypes = oldHex.getTerrainTypes();
                    for (int terrainID : terrainTypes) {
                        if (!hex.containsTerrain(terrainID) && oldHex.containsTerrain(terrainID)) {
                            hex.addTerrain(oldHex.getTerrain(terrainID));
                        }
                    }
                    hex.setTheme(oldHex.getTheme());
                    board.setHex(newX, newY, hex);
                    board.resetStoredElevation();
                }
            }
        }
        return board;
    }

    @Override
    public void updateMapSettings(MapSettings newSettings) {
        mapSettings = newSettings;
    }

    public void loadBoard() {
        JFileChooser fc = new JFileChooser(loadPath);
        setDialogSize(fc);
        fc.setDialogTitle(Messages.getString("BoardEditor.loadBoard"));
        fc.setFileFilter(new BoardFileFilter());
        int returnVal = fc.showOpenDialog(frame);
        saveDialogSize(fc);
        if ((returnVal != JFileChooser.APPROVE_OPTION) || (fc.getSelectedFile() == null)) {
            // I want a file, y'know!
            return;
        }
        loadBoard(fc.getSelectedFile());
    }

    public void loadBoard(File file) {
        try (InputStream is = new FileInputStream(file)) {
            // tell the board to load!
            board.load(is, null, true);
            Set<String> boardTags = board.getTags();
            // Board generation in a game always calls BoardUtilities.combine
            // This serves no purpose here, but is necessary to create
            // flipBGVert/flipBGHoriz lists for the board, which is necessary
            // for the background image to work in the BoardEditor
            board = BoardUtilities.combine(board.getWidth(),
                  board.getHeight(),
                  1,
                  1,
                  new Board[] { board },
                  MapSettings.MEDIUM_GROUND);
            game.setBoard(board);
            // BoardUtilities.combine does not preserve tags, so add them back
            for (String tag : boardTags) {
                board.addTag(tag);
            }
            cheRoadsAutoExit.setSelected(board.getRoadsAutoExit());
            mapSettings.setBoardSize(board.getWidth(), board.getHeight());
            curBoardFile = file;
            RecentBoardList.addBoard(curBoardFile);
            loadPath = curBoardFile.getParentFile();

            // Now, *after* initialization of the board which will correct some errors,
            // do a board validation
            validateBoard(false);
            refreshTerrainList();
            setupUiFreshBoard();
        } catch (IOException ex) {
            LOGGER.error(ex, "loadBoard");
            showBoardLoadError(ex);
            initializeBoardIfEmpty();
        }
    }

    private void showBoardLoadError(Exception ex) {
        String message = Messages.getString("BoardEditor.loadBoardError") + System.lineSeparator() + ex.getMessage();
        String title = Messages.getString("Error");
        JOptionPane.showMessageDialog(frame, message, title, JOptionPane.ERROR_MESSAGE);
    }

    private void initializeBoardIfEmpty() {
        if ((board == null) || (board.getWidth() == 0) || (board.getHeight() == 0)) {
            boardNew(false);
        }
    }

    /**
     * Will do board.initializeHex() for all hexes, correcting building and road connection issues for those hexes that
     * do not have the exits check set.
     */
    private void correctExits() {
        for (int x = 0; x < board.getWidth(); x++) {
            for (int y = 0; y < board.getHeight(); y++) {
                board.initializeHex(x, y);
            }
        }
    }

    /**
     * Saves the board in PNG image format.
     */
    private void boardSaveImage(boolean ignoreUnits) {
        if (curFileImage == null) {
            boardSaveAsImage(ignoreUnits);
            return;
        }
        JDialog waitD = new JDialog(frame, Messages.getString("BoardEditor.waitDialog.title"));
        waitD.add(new JLabel(Messages.getString("BoardEditor.waitDialog.message")));
        waitD.setSize(250, 130);
        // move to middle of screen
        waitD.setLocation((frame.getSize().width / 2) - (waitD.getSize().width / 2),
              (frame.getSize().height / 2) - (waitD.getSize().height / 2));
        waitD.setVisible(true);
        frame.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        waitD.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        // save!
        try {
            ImageIO.write(bv.getEntireBoardImage(ignoreUnits, false), "png", curFileImage);
        } catch (IOException e) {
            LOGGER.error(e, "boardSaveImage");
        }
        waitD.setVisible(false);
        frame.setCursor(Cursor.getDefaultCursor());
    }

    /**
     * Saves the board to a .board file. When saveAs is true, acts as a Save As... by opening a file chooser dialog.
     * When saveAs is false, it will directly save to the current board file name, if it is known and otherwise act as
     * Save As...
     */
    private boolean boardSave(boolean saveAs) {
        // Correct connection issues and do a validation.
        correctExits();
        validateBoard(false);

        // Choose a board file to save to if this was
        // called as "Save As..." or there is no current filename
        if ((curBoardFile == null) || saveAs) {
            if (!chooseSaveBoardFile()) {
                return false;
            }
        }

        // write the board
        try (OutputStream os = new FileOutputStream(curBoardFile)) {
            board.save(os);

            // Adapt to successful save
            butSourceFile.setEnabled(true);
            savedUndoStackSize = undoStack.size();
            hasChanges = false;
            RecentBoardList.addBoard(curBoardFile);
            setFrameTitle();
            return true;
        } catch (IOException e) {
            LOGGER.error(e, "boardSave");
            return false;
        }
    }

    /**
     * Shows a dialog for choosing a .board file to save to. Sets curBoardFile and returns true when a valid file was
     * chosen. Returns false otherwise.
     */
    private boolean chooseSaveBoardFile() {
        JFileChooser fc = new JFileChooser(loadPath);
        setDialogSize(fc);
        fc.setLocation(frame.getLocation().x + 150, frame.getLocation().y + 100);
        fc.setDialogTitle(Messages.getString("BoardEditor.saveBoardAs"));
        fc.setFileFilter(new BoardFileFilter());
        int returnVal = fc.showSaveDialog(frame);
        saveDialogSize(fc);
        if ((returnVal != JFileChooser.APPROVE_OPTION) || (fc.getSelectedFile() == null)) {
            return false;
        }
        File choice = fc.getSelectedFile();
        // make sure the file ends in board
        if (!choice.getName().toLowerCase().endsWith(".board")) {
            try {
                choice = new File(choice.getCanonicalPath() + ".board");
            } catch (IOException ignored) {
                return false;
            }
        }
        curBoardFile = choice;
        return true;
    }

    /**
     * Opens a file dialog box to select a file to save as; saves the board to the file as an image. Useful for printing
     * boards.
     */
    private void boardSaveAsImage(boolean ignoreUnits) {
        JFileChooser fc = new JFileChooser(".");
        setDialogSize(fc);
        fc.setLocation(frame.getLocation().x + 150, frame.getLocation().y + 100);
        fc.setDialogTitle(Messages.getString("BoardEditor.saveAsImage"));
        fc.setFileFilter(new FileFilter() {
            @Override
            public boolean accept(File dir) {
                return (dir.getName().endsWith(".png") || dir.isDirectory());
            }

            @Override
            public String getDescription() {
                return ".png";
            }
        });
        int returnVal = fc.showSaveDialog(frame);
        saveDialogSize(fc);
        if ((returnVal != JFileChooser.APPROVE_OPTION) || (fc.getSelectedFile() == null)) {
            // I want a file, y'know!
            return;
        }
        curFileImage = fc.getSelectedFile();

        // make sure the file ends in png
        if (!curFileImage.getName().toLowerCase().endsWith(".png")) {
            try {
                curFileImage = new File(curFileImage.getCanonicalPath() + ".png");
            } catch (IOException ignored) {
                // failure!
                return;
            }
        }
        boardSaveImage(ignoreUnits);
    }

    //
    // ItemListener
    //
    @Override
    public void itemStateChanged(ItemEvent ie) {
        if (ie.getSource().equals(cheRoadsAutoExit)) {
            // Set the new value for the option, and refresh the board.
            board.setRoadsAutoExit(cheRoadsAutoExit.isSelected());
            bv.updateBoard();
            repaintWorkingHex();
        }
    }

    //
    // TextListener
    //
    @Override
    public void changedUpdate(DocumentEvent te) {
        if (te.getDocument().equals(texElev.getDocument())) {
            int value;
            try {
                value = Integer.parseInt(texElev.getText());
            } catch (NumberFormatException ex) {
                return;
            }
            if (value != curHex.getLevel()) {
                curHex.setLevel(value);
                repaintWorkingHex();
            }
        } else if (te.getDocument().equals(texTerrainLevel.getDocument())) {
            // prevent updating the terrain from looping back to
            // update the text fields that have just been edited
            if (!terrListBlocker) {
                noTextFieldUpdate = true;
                updateWhenSelected();
                noTextFieldUpdate = false;
            }
        } else if (te.getDocument().equals(texTerrExits.getDocument())) {
            // prevent updating the terrain from looping back to
            // update the text fields that have just been edited
            if (!terrListBlocker) {
                noTextFieldUpdate = true;
                setExitsState(true);
                updateWhenSelected();
                noTextFieldUpdate = false;
            }
        }
    }

    @Override
    public void insertUpdate(DocumentEvent event) {
        changedUpdate(event);
    }

    @Override
    public void removeUpdate(DocumentEvent event) {
        changedUpdate(event);
    }

    /** Called when the user selects the "Help->About" menu item. */
    private void showAbout() {
        new CommonAboutDialog(frame).setVisible(true);
    }

    /** Called when the user selects the "Help->Contents" menu item. */
    private void showHelp() {
        if (help == null) {
            help = new BoardEditorHelpDialog(frame);
        }
        help.setVisible(true); // Show the help dialog.
    }

    /** Called when the user selects the "View->Client Settings" menu item. */
    private void showSettings() {
        if (settingsDialog == null) {
            settingsDialog = new CommonSettingsDialog(frame);
        }
        settingsDialog.setVisible(true);
    }

    /**
     * Adjusts some UI and internal settings for a freshly loaded or freshly generated board.
     */
    private void setupUiFreshBoard() {
        // Reset the Undo stack and the board has no changes
        savedUndoStackSize = 0;
        canReturnToSaved = true;
        resetUndo();
        hasChanges = false;
        // When a board was loaded, we have a file, otherwise not
        butSourceFile.setEnabled(curBoardFile != null);
        // Adjust the UI
        bvc.doLayout();
        setFrameTitle();
    }

    /**
     * Performs board validation. When showPositiveResult is true, the result of the validation will be shown in a
     * dialog. Otherwise, only a negative result (the board has errors) will be shown.
     */
    private void validateBoard(boolean showPositiveResult) {
        List<String> errors = new ArrayList<>();
        board.isValid(errors);
        if ((!errors.isEmpty()) || showPositiveResult) {
            showBoardValidationReport(errors);
        }
    }

    /**
     * Shows a board validation report dialog, reporting either the contents of errBuff or that the board has no
     * errors.
     */
    private void showBoardValidationReport(List<String> errors) {
        ignoreHotKeys = true;
        if ((errors != null) && !errors.isEmpty()) {
            String title = Messages.getString("BoardEditor.invalidBoard.title");
            String msg = Messages.getString("BoardEditor.invalidBoard.report");
            msg += String.join("\n", errors);
            JTextArea textArea = new JTextArea(msg);
            JScrollPane scrollPane = new JScrollPane(textArea);
            textArea.setLineWrap(true);
            textArea.setWrapStyleWord(true);
            scrollPane.setPreferredSize(new Dimension(getWidth(), getHeight() / 2));
            JOptionPane.showMessageDialog(frame, scrollPane, title, JOptionPane.ERROR_MESSAGE);
        } else {
            String title = Messages.getString("BoardEditor.validBoard.title");
            String msg = Messages.getString("BoardEditor.validBoard.report");
            JOptionPane.showMessageDialog(frame, msg, title, JOptionPane.INFORMATION_MESSAGE);
        }
        ignoreHotKeys = false;
    }

    //
    // ActionListener
    //
    @Override
    public void actionPerformed(ActionEvent ae) {
        if (ae.getActionCommand().startsWith(ClientGUI.BOARD_RECENT)) {
            if (hasChanges && (showSavePrompt() == DialogResult.CANCELLED)) {
                return;
            }
            String recentBoard = ae.getActionCommand().substring(ClientGUI.BOARD_RECENT.length() + 1);
            loadBoard(new File(recentBoard));
        } else if (ae.getActionCommand().equals(ClientGUI.BOARD_NEW)) {
            ignoreHotKeys = true;
            boardNew(true);
            ignoreHotKeys = false;
        } else if (ae.getActionCommand().equals(ClientGUI.BOARD_RESIZE)) {
            ignoreHotKeys = true;
            boardResize();
            ignoreHotKeys = false;
        } else if (ae.getActionCommand().equals(ClientGUI.BOARD_OPEN)) {
            ignoreHotKeys = true;
            loadBoard();
            ignoreHotKeys = false;
        } else if (ae.getActionCommand().equals(ClientGUI.BOARD_SAVE)) {
            ignoreHotKeys = true;
            boardSave(false);
            ignoreHotKeys = false;
        } else if (ae.getActionCommand().equals(ClientGUI.BOARD_SAVE_AS)) {
            ignoreHotKeys = true;
            boardSave(true);
            ignoreHotKeys = false;
        } else if (ae.getActionCommand().equals(ClientGUI.BOARD_SAVE_AS_IMAGE)) {
            ignoreHotKeys = true;
            boardSaveAsImage(false);
            ignoreHotKeys = false;
        } else if (ae.getActionCommand().equals(ClientGUI.BOARD_SOURCE_FILE)) {
            if (curBoardFile != null) {
                try {
                    Desktop.getDesktop().open(curBoardFile);
                } catch (IOException e) {
                    ignoreHotKeys = true;
                    JOptionPane.showMessageDialog(frame,
                          Messages.getString("BoardEditor.OpenFileError", curBoardFile.toString()) + e.getMessage());
                    LOGGER.error(e, "actionPerformed");
                    ignoreHotKeys = false;
                }
            }
        } else if (ae.getActionCommand().equals(ClientGUI.BOARD_VALIDATE)) {
            correctExits();
            validateBoard(true);
        } else if (ae.getSource().equals(butDelTerrain) && !lisTerrain.isSelectionEmpty()) {
            Terrain toRemove = new Terrain(lisTerrain.getSelectedValue().getTerrain());
            curHex.removeTerrain(toRemove.getType());
            refreshTerrainList();
            repaintWorkingHex();
        } else if (ae.getSource().equals(butAddTerrain)) {
            addSetTerrain();
        } else if (ae.getSource().equals(butElevUp) && (curHex.getLevel() < 9)) {
            curHex.setLevel(curHex.getLevel() + 1);
            texElev.incValue();
            repaintWorkingHex();
        } else if (ae.getSource().equals(butElevDown) && (curHex.getLevel() > -5)) {
            curHex.setLevel(curHex.getLevel() - 1);
            texElev.decValue();
            repaintWorkingHex();
        } else if (ae.getSource().equals(butTerrUp)) {
            texTerrainLevel.incValue();
            updateWhenSelected();
        } else if (ae.getSource().equals(butTerrDown)) {
            texTerrainLevel.decValue();
            updateWhenSelected();
        } else if (ae.getSource().equals(texTerrainLevel)) {
            updateWhenSelected();
        } else if (ae.getSource().equals(texTerrExits)) {
            int exitsVal = texTerrExits.getNumber();
            if (exitsVal == 0) {
                setExitsState(false);
            } else if (exitsVal > 63) {
                texTerrExits.setNumber(63);
            }
            updateWhenSelected();
        } else if (ae.getSource().equals(butTerrExits)) {
            int exitsVal;

            if (ae.getActionCommand().equals(CMD_EDIT_DEPLOYMENT_ZONES)) {
                var dlg = new MultiIntSelectorDialog(frame,
                      "BoardEditor.deploymentZoneSelectorName",
                      "BoardEditor.deploymentZoneSelectorTitle",
                      "BoardEditor.deploymentZoneSelectorDescription",
                      Board.MAX_DEPLOYMENT_ZONE_NUMBER,
                      Board.exitsAsIntList(texTerrExits.getNumber()));
                dlg.setVisible(true);
                exitsVal = Board.IntListAsExits(dlg.getSelectedItems());
                texTerrExits.setNumber(exitsVal);
            } else {
                ExitsDialog ed = new ExitsDialog(frame);
                exitsVal = texTerrExits.getNumber();
                ed.setExits(exitsVal);
                ed.setVisible(true);
                exitsVal = ed.getExits();
                texTerrExits.setNumber(exitsVal);
            }
            setExitsState(exitsVal != 0);
            updateWhenSelected();
        } else if (ae.getSource().equals(cheTerrExitSpecified)) {
            noTextFieldUpdate = true;
            updateWhenSelected();
            noTextFieldUpdate = false;
            setExitsState(cheTerrExitSpecified.isSelected());
        } else if (ae.getSource().equals(butExitUp)) {
            setExitsState(true);
            texTerrExits.incValue();
            updateWhenSelected();
        } else if (ae.getSource().equals(butExitDown)) {
            texTerrExits.decValue();
            setExitsState(texTerrExits.getNumber() != 0);
            updateWhenSelected();
        } else if (ae.getActionCommand().equals(ClientGUI.VIEW_MINI_MAP)) {
            guip.toggleMinimapEnabled();
            minimapW.setVisible(guip.getMinimapEnabled());
        } else if (ae.getActionCommand().equals(ClientGUI.HELP_ABOUT)) {
            showAbout();
        } else if (ae.getActionCommand().equals(ClientGUI.HELP_CONTENTS)) {
            showHelp();
        } else if (ae.getActionCommand().equals(ClientGUI.VIEW_CLIENT_SETTINGS)) {
            showSettings();
        } else if (ae.getActionCommand().equals(ClientGUI.VIEW_ZOOM_IN)) {
            bv.zoomIn();
        } else if (ae.getActionCommand().equals(ClientGUI.VIEW_ZOOM_OUT)) {
            bv.zoomOut();
        } else if (ae.getActionCommand().equals(ClientGUI.VIEW_TOGGLE_ISOMETRIC)) {
            GUIPreferences.getInstance().setIsometricEnabled(!GUIPreferences.getInstance().getIsometricEnabled());
        } else if (ae.getActionCommand().equals(ClientGUI.VIEW_CHANGE_THEME)) {
            String newTheme = bv.changeTheme();
            if (newTheme != null) {
                choTheme.setSelectedItem(newTheme);
            }
        } else if (ae.getSource().equals(choTheme)) {
            curHex.setTheme((String) choTheme.getSelectedItem());
            repaintWorkingHex();
        } else if (ae.getSource().equals(buttonLW)) {
            setConvenientTerrain(ae, new Terrain(Terrains.WOODS, 1), new Terrain(Terrains.FOLIAGE_ELEV, 2));
        } else if (ae.getSource().equals(buttonOW)) {
            setConvenientTerrain(ae, new Terrain(Terrains.WOODS, 1), new Terrain(Terrains.FOLIAGE_ELEV, 1));
        } else if (ae.getSource().equals(buttonMg)) {
            setConvenientTerrain(ae, new Terrain(Terrains.MAGMA, 1));
        } else if (ae.getSource().equals(buttonLJ)) {
            setConvenientTerrain(ae, new Terrain(Terrains.JUNGLE, 1), new Terrain(Terrains.FOLIAGE_ELEV, 2));
        } else if (ae.getSource().equals(buttonOJ)) {
            setConvenientTerrain(ae, new Terrain(Terrains.JUNGLE, 1), new Terrain(Terrains.FOLIAGE_ELEV, 1));
        } else if (ae.getSource().equals(buttonWa)) {
            buttonRaiseLower.setSelected(false);
            if ((ae.getModifiers() & ActionEvent.CTRL_MASK) != 0) {
                int rapidsLevel = curHex.containsTerrain(Terrains.RAPIDS, 1) ? 2 : 1;
                if (!curHex.containsTerrain(Terrains.WATER) || (curHex.getTerrain(Terrains.WATER).getLevel() == 0)) {
                    setConvenientTerrain(ae, new Terrain(Terrains.RAPIDS, rapidsLevel), new Terrain(Terrains.WATER, 1));
                } else {
                    setConvenientTerrain(ae,
                          new Terrain(Terrains.RAPIDS, rapidsLevel),
                          curHex.getTerrain(Terrains.WATER));
                }
            } else {
                if ((ae.getModifiers() & ActionEvent.SHIFT_MASK) == 0) {
                    curHex.removeAllTerrains();
                }
                setConvenientTerrain(ae, new Terrain(Terrains.WATER, 1));
            }
        } else if (ae.getSource().equals(buttonSw)) {
            setConvenientTerrain(ae, new Terrain(Terrains.SWAMP, 1));
        } else if (ae.getSource().equals(buttonRo)) {
            setConvenientTerrain(ae, new Terrain(Terrains.ROUGH, 1));
        } else if (ae.getSource().equals(buttonPv)) {
            setConvenientTerrain(ae, new Terrain(Terrains.PAVEMENT, 1));
        } else if (ae.getSource().equals(buttonMd)) {
            setConvenientTerrain(ae, new Terrain(Terrains.MUD, 1));
        } else if (ae.getSource().equals(buttonTu)) {
            setConvenientTerrain(ae, new Terrain(Terrains.TUNDRA, 1));
        } else if (ae.getSource().equals(buttonIc)) {
            setConvenientTerrain(ae, new Terrain(Terrains.ICE, 1));
        } else if (ae.getSource().equals(buttonSn)) {
            setConvenientTerrain(ae, new Terrain(Terrains.SNOW, 1));
        } else if (ae.getSource().equals(buttonCl)) {
            curHex.removeAllTerrains();
            buttonRaiseLower.setSelected(false);
            refreshTerrainList();
            repaintWorkingHex();
        } else if (ae.getSource().equals(buttonBrush1)) {
            brushSize = 1;
            lastClicked = null;
        } else if (ae.getSource().equals(buttonBrush2)) {
            brushSize = 2;
            lastClicked = null;
        } else if (ae.getSource().equals(buttonBrush3)) {
            brushSize = 3;
            lastClicked = null;
        } else if (ae.getSource().equals(buttonBu)) {
            buttonRaiseLower.setSelected(false);
            if (((ae.getModifiers() & ActionEvent.SHIFT_MASK) == 0) && ((ae.getModifiers() & ActionEvent.ALT_MASK) ==
                  0)) {
                curHex.removeAllTerrains();
            }
            setBasicBuilding((ae.getModifiers() & ActionEvent.ALT_MASK) != 0);
        } else if (ae.getSource().equals(buttonBr)) {
            if ((ae.getModifiers() & ActionEvent.SHIFT_MASK) == 0) {
                curHex.removeAllTerrains();
            }
            buttonRaiseLower.setSelected(false);
            setBasicBridge();
        } else if (ae.getSource().equals(buttonFT)) {
            if ((ae.getModifiers() & ActionEvent.SHIFT_MASK) == 0) {
                curHex.removeAllTerrains();
            }
            buttonRaiseLower.setSelected(false);
            setBasicFuelTank();
        } else if (ae.getSource().equals(buttonRd)) {
            setConvenientTerrain(ae, new Terrain(Terrains.ROAD, 1));
        } else if (ae.getSource().equals(buttonRaiseLower)) {
            // Not so useful to only do on clear terrain
            buttonOOC.setSelected(false);
        } else if (ae.getActionCommand().equals(ClientGUI.BOARD_UNDO)) {
            // The button should not be active when the stack is empty, but
            // let's check nevertheless
            if (undoStack.isEmpty()) {
                buttonUndo.setEnabled(false);
            } else {
                HashSet<Hex> recentHexes = undoStack.pop();
                HashSet<Hex> redoHexes = new HashSet<>();
                for (Hex hex : recentHexes) {
                    // Retrieve the board hex for Redo
                    Hex rHex = board.getHex(hex.getCoords()).duplicate();
                    rHex.setCoords(hex.getCoords());
                    redoHexes.add(rHex);
                    // and undo the board hex
                    board.setHex(hex.getCoords(), hex);
                }
                redoStack.push(redoHexes);
                if (undoStack.isEmpty()) {
                    buttonUndo.setEnabled(false);
                }
                hasChanges = !canReturnToSaved || (undoStack.size() != savedUndoStackSize);
                buttonRedo.setEnabled(true);
                currentUndoSet = null; // should be anyway
                correctExits();
            }
            setFrameTitle();
        } else if (ae.getActionCommand().equals(ClientGUI.BOARD_REDO)) {
            // The button should not be active when the stack is empty, but
            // let's check nevertheless
            if (redoStack.isEmpty()) {
                buttonRedo.setEnabled(false);
            } else {
                HashSet<Hex> recentHexes = redoStack.pop();
                HashSet<Hex> undoHexes = new HashSet<>();
                for (Hex hex : recentHexes) {
                    Hex rHex = board.getHex(hex.getCoords()).duplicate();
                    rHex.setCoords(hex.getCoords());
                    undoHexes.add(rHex);
                    board.setHex(hex.getCoords(), hex);
                }
                undoStack.push(undoHexes);
                if (redoStack.isEmpty()) {
                    buttonRedo.setEnabled(false);
                }
                buttonUndo.setEnabled(true);
                hasChanges = !canReturnToSaved || (undoStack.size() != savedUndoStackSize);
                currentUndoSet = null; // should be anyway
                correctExits();
            }
            setFrameTitle();
        } else if (ae.getActionCommand().equals(ClientGUI.BOARD_RAISE)) {
            boardChangeLevel();
        } else if (ae.getActionCommand().equals(ClientGUI.BOARD_CLEAR)) {
            boardClear();
        } else if (ae.getActionCommand().equals(ClientGUI.BOARD_FLOOD)) {
            boardFlood();
        } else if (ae.getActionCommand().equals(ClientGUI.BOARD_REMOVE_WATER)) {
            boardRemoveTerrain(WATER, WATER_FLUFF);
        } else if (ae.getActionCommand().equals(ClientGUI.BOARD_REMOVE_ROADS)) {
            boardRemoveTerrain(ROAD, ROAD_FLUFF, BRIDGE, BRIDGE_CF, BRIDGE_ELEV);
        } else if (ae.getActionCommand().equals(ClientGUI.BOARD_REMOVE_FORESTS)) {
            boardRemoveTerrain(WOODS, JUNGLE, FOLIAGE_ELEV);
        } else if (ae.getActionCommand().equals(ClientGUI.BOARD_REMOVE_BUILDINGS)) {
            boardRemoveTerrain(BUILDING,
                  BLDG_ARMOR,
                  BLDG_CF,
                  BLDG_CLASS,
                  BLDG_FLUFF,
                  BLDG_BASE_COLLAPSED,
                  BLDG_BASEMENT_TYPE,
                  BLDG_ELEV,
                  FUEL_TANK,
                  FUEL_TANK_CF,
                  FUEL_TANK_ELEV, FUEL_TANK_MAGN);
        } else if (ae.getActionCommand().equals(ClientGUI.BOARD_FLATTEN)) {
            boardFlatten();
        } else if (ae.getActionCommand().equals(ClientGUI.VIEW_RESET_WINDOW_POSITIONS)) {
            minimapW.setBounds(0, 0, minimapW.getWidth(), minimapW.getHeight());
        }
    }

    /** Flattens the board, setting all hexes to level 0. */
    private void boardFlatten() {
        for (int x = 0; x < board.getWidth(); x++) {
            for (int y = 0; y < board.getHeight(); y++) {
                Coords c = new Coords(x, y);
                if (board.getHex(c).getLevel() != 0) {
                    saveToUndo(c);
                    Hex newHex = board.getHex(c).duplicate();
                    newHex.setLevel(0);
                    board.setHex(c, newHex);
                }
            }
        }
        correctExits();
        endCurrentUndoSet();
    }

    /** Removes the given terrain type(s) from the board. */
    private void boardRemoveTerrain(int type, int... types) {
        for (int x = 0; x < board.getWidth(); x++) {
            for (int y = 0; y < board.getHeight(); y++) {
                Coords c = new Coords(x, y);
                if (board.getHex(c).containsTerrain(type) || board.getHex(c).containsAnyTerrainOf(types)) {
                    saveToUndo(c);
                    Hex newHex = board.getHex(c).duplicate();
                    newHex.removeTerrain(type);
                    for (int additional : types) {
                        newHex.removeTerrain(additional);
                    }
                    board.setHex(c, newHex);
                }
            }
        }
        correctExits();
        endCurrentUndoSet();
    }

    /**
     * Asks for confirmation and clears the whole board (sets all hexes to clear level 0).
     */
    private void boardClear() {
        if (!MMConfirmDialog.confirm(frame,
              Messages.getString("BoardEditor.clearTitle"),
              Messages.getString("BoardEditor.clearMsg"))) {
            return;
        }
        board.resetStoredElevation();
        for (int x = 0; x < board.getWidth(); x++) {
            for (int y = 0; y < board.getHeight(); y++) {
                Coords c = new Coords(x, y);
                saveToUndo(c);
                board.setHex(c, new Hex(0));
            }
        }
        correctExits();
        endCurrentUndoSet();
    }

    /**
     * "Pushes" the current set of undoable hexes as a package to the stack, meaning that a paint or other action is
     * finished.
     */
    private void endCurrentUndoSet() {
        if ((currentUndoSet != null) && !currentUndoSet.isEmpty()) {
            undoStack.push(currentUndoSet);
            currentUndoSet = null;
            buttonUndo.setEnabled(true);
            // Drawing something disables any redo actions
            redoStack.clear();
            buttonRedo.setEnabled(false);

            // When Undo (without Redo) has been used after saving and the user draws on the board, then it can no
            // longer know if it's been returned to the saved state, and it will always be treated as changed.
            if (savedUndoStackSize > undoStack.size()) {
                canReturnToSaved = false;
            }
            hasChanges = !canReturnToSaved || (undoStack.size() != savedUndoStackSize);
        }
    }

    /**
     * Asks for a level delta and changes the level of all the board's hexes by that delta.
     */
    private void boardChangeLevel() {
        var dlg = new LevelChangeDialog(frame);
        dlg.setVisible(true);
        if (!dlg.getResult().isConfirmed() || (dlg.getLevelChange() == 0)) {
            return;
        }

        board.resetStoredElevation();
        for (int x = 0; x < board.getWidth(); x++) {
            for (int y = 0; y < board.getHeight(); y++) {
                Coords c = new Coords(x, y);
                saveToUndo(c);
                Hex newHex = board.getHex(c).duplicate();
                newHex.setLevel(newHex.getLevel() + dlg.getLevelChange());
                board.setHex(c, newHex);
            }
        }
        correctExits();
        endCurrentUndoSet();
    }

    /**
     * Asks for flooding info and then floods the whole board with water up to a level.
     */
    private void boardFlood() {
        var dlg = new FloodDialog(frame);
        dlg.setVisible(true);
        if (!dlg.getResult().isConfirmed()) {
            return;
        }

        int surface = dlg.getLevelChange();
        board.resetStoredElevation();
        for (int x = 0; x < board.getWidth(); x++) {
            for (int y = 0; y < board.getHeight(); y++) {
                Coords c = new Coords(x, y);
                Hex hex = board.getHex(c);
                if (hex.getLevel() < surface) {
                    saveToUndo(c);
                    Hex newHex = hex.duplicate();
                    int presentDepth = hex.containsTerrain(Terrains.WATER) ? hex.terrainLevel(Terrains.WATER) : 0;
                    if (dlg.getRemoveTerrain()) {
                        newHex.removeAllTerrains();
                        // Restore bridges if they're above the water
                        if (hex.containsTerrain(BRIDGE) &&
                              (hex.getLevel() + hex.getTerrain(BRIDGE_ELEV).getLevel() >= surface)) {
                            newHex.addTerrain(hex.getTerrain(BRIDGE));
                            newHex.addTerrain(new Terrain(BRIDGE_ELEV,
                                  hex.getLevel() + hex.getTerrain(BRIDGE_ELEV).getLevel() - surface));
                            newHex.addTerrain(hex.getTerrain(BRIDGE_CF));
                        }
                    }
                    int addedWater = surface - hex.getLevel();
                    newHex.addTerrain(new Terrain(Terrains.WATER, addedWater + presentDepth));
                    newHex.setLevel(newHex.getLevel() + addedWater);
                    board.setHex(c, newHex);
                }
            }
        }
        correctExits();
        endCurrentUndoSet();
    }

    private void setConvenientTerrain(ActionEvent event, Terrain... terrains) {
        if (terrains.length == 0) {
            return;
        }
        if ((event.getModifiers() & ActionEvent.SHIFT_MASK) == 0) {
            curHex.removeAllTerrains();
        }
        buttonRaiseLower.setSelected(false);
        for (var terrain : terrains) {
            curHex.addTerrain(terrain);
        }
        refreshTerrainList();
        repaintWorkingHex();
        selectTerrain(terrains[0]);
    }

    /**
     * Selects the given terrain in the terrain list, if possible. All but terrain type is ignored.
     */
    private void selectTerrain(Terrain terrain) {
        for (int i = 0; i < lisTerrain.getModel().getSize(); i++) {
            Terrain listEntry = lisTerrain.getModel().getElementAt(i).getTerrain();
            if (listEntry.getType() == terrain.getType()) {
                lisTerrain.setSelectedIndex(i);
                return;
            }
        }
    }

    /**
     * Sets the "Use Exits" checkbox to newState and adapts the coloring of the textfield accordingly. Use this instead
     * of setting the checkbox state directly.
     */
    private void setExitsState(boolean newState) {
        cheTerrExitSpecified.setSelected(newState);
        if (cheTerrExitSpecified.isSelected()) {
            texTerrExits.setForeground(null);
        } else {
            texTerrExits.setForeground(UIUtil.uiGray());
        }
    }

    @Override
    public void valueChanged(ListSelectionEvent event) {
        if (event.getValueIsAdjusting()) {
            return;
        }
        if (event.getSource().equals(lisTerrain) && !noTextFieldUpdate) {
            refreshTerrainFromList();
        }
    }

    /**
     * @return the frame this is displayed in
     */
    public JFrame getFrame() {
        return frame;
    }

    /**
     * Returns true if a dialog is visible on top of the <code>ClientGUI</code>. For example, the
     * <code>MegaMekController</code> should ignore hotkeys if there is a dialog, like the
     * <code>CommonSettingsDialog</code>, open.
     *
     * @return whether hot keys should be ignored or not
     */
    public boolean shouldIgnoreHotKeys() {
        return ignoreHotKeys ||
              UIUtil.isModalDialogDisplayed() ||
              ((help != null) && help.isVisible()) ||
              ((settingsDialog != null) && settingsDialog.isVisible()) ||
              texElev.hasFocus() ||
              texTerrainLevel.hasFocus() ||
              texTerrExits.hasFocus();
    }

    private void setDialogSize(JFileChooser dialog) {
        int width = guip.getBoardEditLoadWidth();
        int height = guip.getBoardEditLoadHeight();
        dialog.setPreferredSize(new Dimension(width, height));
    }

    private void saveDialogSize(JComponent dialog) {
        guip.setBoardEditLoadHeight(dialog.getHeight());
        guip.setBoardEditLoadWidth(dialog.getWidth());
    }

    /**
     * Sets the Board Editor frame title, adding the current file name if any and a "*" if the board has unsaved
     * changes.
     */
    private void setFrameTitle() {
        String title = (curBoardFile == null) ?
              Messages.getString("BoardEditor.title") :
              Messages.getString("BoardEditor.title0", curBoardFile);
        frame.setTitle(title + (hasChanges ? "*" : ""));
    }

    private void copyWorkingHexToClipboard() {
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(new StringSelection(curHex.getClipboardString()), null);
    }

    private void pasteFromClipboard() {
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        Transferable contents = clipboard.getContents(null);
        if ((contents != null) && contents.isDataFlavorSupported(DataFlavor.stringFlavor)) {
            try {
                String clipboardString = (String) contents.getTransferData(DataFlavor.stringFlavor);
                Hex pastedHex = Hex.parseClipboardString(clipboardString);
                if (pastedHex != null) {
                    setCurrentHex(pastedHex);
                }
            } catch (Exception ex) {
                LOGGER.error(ex, "pasteFromClipboard");
            }
        }
    }

    private PaintMode paintMode() {
        if (buttonRaiseLower.isSelected()) {
            return PaintMode.LOWER_RAISE_HEX_LEVEL;
        } else if (buttonDeployZone.isSelected()) {
            return PaintMode.DEPLOYMENT_ZONE;
        } else {
            return PaintMode.NORMAL;
        }
    }

    private void changeSelectedDeploymentZone() {
        deploymentZoneDrawer.setSelectedDeploymentZone((Integer) deploymentZoneChooser.getValue());
        bv.clearHexImageCache();
        bv.repaint();
    }

    private void deployZoneToggled() {
        deploymentZoneChooser.setEnabled(buttonDeployZone.isSelected());
        deploymentZoneDrawer.setDeploymentZoneMode(buttonDeployZone.isSelected());
        bv.clearHexImageCache();
        bv.repaint();
    }
}
