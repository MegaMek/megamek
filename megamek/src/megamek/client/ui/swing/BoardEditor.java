/*
 * MegaMek - Copyright (C) 2000-2003 Ben Mazur (bmazur@sev.org)
 * Copyright (c) 2021 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */
package megamek.client.ui.swing;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.*;
import javax.swing.filechooser.FileFilter;

import megamek.MegaMek;
import megamek.client.event.BoardViewEvent;
import megamek.client.event.BoardViewListenerAdapter;
import megamek.client.ui.Messages;
import megamek.client.ui.dialogs.helpDialogs.AbstractHelpDialog;
import megamek.client.ui.dialogs.helpDialogs.BoardEditorHelpDialog;
import megamek.client.ui.swing.boardview.BoardView1;
import megamek.client.ui.swing.tileset.TilesetManager;
import megamek.client.ui.swing.util.MegaMekController;
import megamek.client.ui.swing.util.UIUtil;
import megamek.client.ui.swing.util.UIUtil.FixedYPanel;
import megamek.common.*;
import megamek.common.annotations.Nullable;
import megamek.common.preference.IPreferenceChangeListener;
import megamek.common.preference.PreferenceChangeEvent;
import megamek.common.util.BoardUtilities;
import megamek.common.util.ImageUtil;
import megamek.common.util.fileUtils.MegaMekFile;

// TODO: center map
// TODO: background on the whole screen
// TODO: restrict terrains to those with images?
// TODO: Allow drawing of invalid terrain as an override?
// TODO: Allow adding/changing board background images
// TODO: sluggish hex drawing?
// TODO: the board validation after a board load seems to be influenced by the former board...
// TODO: copy/paste hexes
public class BoardEditor extends JPanel
        implements ItemListener, ListSelectionListener, ActionListener, 
        DocumentListener, IMapSettingsObserver, IPreferenceChangeListener {
    
    /**
     * Class to make terrains in JComboBoxes easier.  This enables keeping the terrain type int separate from the name
     * that gets displayed and also provides a way to get tooltips.
     * 
     * @author arlith
     */
    private static class TerrainHelper implements Comparable<TerrainHelper> {
        private int terrainType;

        TerrainHelper (int terrain) {
            terrainType = terrain;
        }

        public int getTerrainType() {
            return terrainType;
        }

        @Override
        public String toString() {
            return Terrains.getEditorName(terrainType);
        }

        public String getTerrainTooltip() {
            return Terrains.getEditorTooltip(terrainType);
        }

        @Override
        public int compareTo(TerrainHelper o) {
            return toString().compareTo(o.toString());
        }
        
        @Override
        public boolean equals(Object other) {
            if (other instanceof Integer) {
                return getTerrainType() == (Integer) other;
            }
            if (!(other instanceof TerrainHelper)) {
                return false;
            }
            return getTerrainType() == ((TerrainHelper)other).getTerrainType();
        }
    }

    /**
     * Class to make it easier to display a <code>Terrain</code> in a JList or JComboBox.
     *
     * @author arlith
     */
    private static class TerrainTypeHelper implements Comparable<TerrainTypeHelper> {

        ITerrain terrain;

        TerrainTypeHelper(ITerrain terrain) {
            this.terrain = terrain;
        }

        public ITerrain getTerrain() {
            return terrain;
        }

        @Override
        public String toString() {
            String baseString = Terrains.getDisplayName(terrain.getType(), terrain.getLevel());
            if (baseString == null) {
                baseString = Terrains.getEditorName(terrain.getType());
                baseString += " " + terrain.getLevel();
            }
            if (terrain.hasExitsSpecified()) {
                baseString += " (Exits: " + terrain.getExits() + ")";
            }
            return baseString; 
        }

        public String getTooltip() {
            return terrain.toString();
        }

        @Override
        public int compareTo(TerrainTypeHelper o) {
            return toString().compareTo(o.toString());
        }
    }
    
    /**
     *  ListCellRenderer for rendering tooltips for each item in a list or combobox.  Code from SourceForge:
     *  https://stackoverflow.com/questions/480261/java-swing-mouseover-text-on-jcombobox-items 
     */
    private static class ComboboxToolTipRenderer extends DefaultListCellRenderer {
        private static final long serialVersionUID = -6776114675645434769L;
        
        private TerrainHelper[] terrains;
        private List<TerrainTypeHelper> terrainTypes;

        @Override
        public Component getListCellRendererComponent(final JList<?> list, final Object value,
                                                      final int index, final boolean isSelected,
                                                      final boolean cellHasFocus) {
            JComponent comp = (JComponent) super.getListCellRendererComponent(list, value, index,
                    isSelected, cellHasFocus);
            if ((-1 < index) && (value != null)) {
                if (terrainTypes != null) {
                    list.setToolTipText(terrainTypes.get(index).getTooltip());
                } else if (terrains != null) {
                    list.setToolTipText(terrains[index].getTerrainTooltip());
                }
            }
            return comp;
        }

        public void setTerrains(TerrainHelper... terrains) {
            this.terrains = terrains;
        }
        
        public void setTerrainTypes(List<TerrainTypeHelper> terrainTypes) {
            this.terrainTypes = terrainTypes;
        }
    }
 
    private static final long serialVersionUID = 4689863639249616192L;
    
    private GUIPreferences guip = GUIPreferences.getInstance();

    //region action commands
    private static final String FILE_BOARD_EDITOR_EXPAND = "fileBoardExpand";
    private static final String FILE_BOARD_EDITOR_VALIDATE = "fileBoardValidate";
    private static final String FILE_SOURCEFILE = "fileSource";
    //endregion action commands
    
    private static final int BASE_TERRAINBUTTON_ICON_WIDTH = 70;
    private static final int BASE_ARROWBUTTON_ICON_WIDTH = 25;

    // Components
    private JFrame frame = new JFrame();
    private Game game = new Game();
    private IBoard board = game.getBoard();
    private BoardView1 bv;
    public static final int [] allDirections = {0,1,2,3,4,5};
    boolean isDragging = false;
    private Component bvc;
    private CommonMenuBar menuBar = new CommonMenuBar();
    private CommonAboutDialog about;
    private AbstractHelpDialog help;
    private CommonSettingsDialog setdlg;
    private ITerrainFactory TF = Terrains.getTerrainFactory();
    private JDialog minimapW;
    private MiniMap minimap;
    private MegaMekController controller;
    
    // The current files
    private File curfileImage;
    private File curBoardFile;

    // The active hex "brush"
    private HexCanvas canHex;
    private IHex curHex = new Hex();
    
    // Easy terrain access buttons
    private List<ScalingIconButton> terrainButtons = new ArrayList<>();
    private ScalingIconButton buttonLW, buttonLJ;
    private ScalingIconButton buttonOW, buttonOJ;
    private ScalingIconButton buttonWa, buttonSw, buttonRo;
    private ScalingIconButton buttonRd, buttonCl, buttonBu;
    private ScalingIconButton buttonMd, buttonPv, buttonSn;
    private ScalingIconButton buttonIc, buttonTu, buttonMg;
    private ScalingIconButton buttonBr, buttonFT;
    private List<ScalingIconToggleButton> brushButtons = new ArrayList<>();
    private ScalingIconToggleButton buttonBrush1, buttonBrush2, buttonBrush3;
    private ScalingIconToggleButton buttonUpDn, buttonOOC;

    // The brush size: 1 = 1 hex, 2 = radius 1, 3 = radius 2  
    private int brushSize = 1;
    private int hexLeveltoDraw = -1000;
    private Font fontElev = new Font("SansSerif", Font.BOLD, 20);
    private Font fontComboTerr = new Font("SansSerif", Font.BOLD, 12);
    private EditorTextField texElev;
    private ScalingIconButton butElevUp;
    private ScalingIconButton butElevDown;
    private JList<TerrainTypeHelper> lisTerrain;
    private ComboboxToolTipRenderer lisTerrainRenderer;
    private ScalingIconButton butDelTerrain;
    private JComboBox<TerrainHelper> choTerrainType;
    private EditorTextField texTerrainLevel;
    private JCheckBox cheTerrExitSpecified;
    private EditorTextField texTerrExits;
    private ScalingIconButton butTerrExits;
    private JCheckBox cheRoadsAutoExit;
    private ScalingIconButton butExitUp, butExitDown;
    private JComboBox<String> choTheme;
    private ScalingIconButton butTerrDown, butTerrUp;
    private JButton butAddTerrain;
    private JButton butBoardNew;
    private JButton butBoardOpen;
    private JButton butBoardSave;
    private JButton butBoardSaveAs;
    private JButton butBoardSaveAsImage;
    private JButton butMiniMap;
    private JButton butBoardValidate;
    private JButton butSourceFile;
    private MapSettings mapSettings = MapSettings.getInstance();
    private JButton butExpandMap;
    private Coords lastClicked;
    private JLabel labTheme = new JLabel(Messages.getString("BoardEditor.labTheme"), SwingConstants.LEFT);
    
    private FixedYPanel panelHexSettings = new FixedYPanel();
    private FixedYPanel panelTerrSettings = new FixedYPanel(new GridLayout(0, 2, 4, 4));
    private FixedYPanel panelBoardSettings = new FixedYPanel();
    
    // Help Texts
    private JLabel labHelp1 = new JLabel(Messages.getString("BoardEditor.helpText"), SwingConstants.LEFT);
    private JLabel labHelp2 = new JLabel(Messages.getString("BoardEditor.helpText2"), SwingConstants.LEFT);
    
    // Undo / Redo
    private List<ScalingIconButton> undoButtons = new ArrayList<>();
    private ScalingIconButton buttonUndo, buttonRedo;
    private Stack<HashSet<IHex>> undoStack = new Stack<>();
    private Stack<HashSet<IHex>> redoStack = new Stack<>();
    private HashSet<IHex> currentUndoSet;
    private HashSet<Coords> currentUndoCoords;
    
    // Tracker for board changes; unfortunately this is not equal to 
    // undoStack == empty because saving the board doesn't empty the 
    // undo stack but makes the board unchanged.
    /** Tracks if the board has changes over the last saved version. */
    private boolean hasChanges = false;
    /** Tracks if the board can return to the last saved version. */
    private boolean canReturnToSaved = true;
    /** The undo stack size at the last save. Used to track saved status of the board. */
    private int savedUndoStackSize = 0;
    
    // Misc
    private File loadPath = Configuration.boardsDir();
    
    /**
     * Special purpose indicator, keeps terrain list 
     * from de-selecting when clicking it
     */
    private boolean terrListBlocker = false;
    
    /**
     * Special purpose indicator, prevents an update
     * loop when the terrain level or exits field is changed
     */
    private boolean noTextFieldUpdate = false;
    
    /**
     * A MouseAdapter that closes a JLabel when clicked 
     */
    private MouseAdapter clickToHide = new MouseAdapter() {
        @Override
        public void mouseReleased(MouseEvent e) {
            if (e.getSource() instanceof JLabel) {
                ((JLabel) e.getSource()).setVisible(false);
            }
        }
    };

    /**
     * Flag that indicates whether hotkeys should be ignored or not.  This is
     * used for disabling hot keys when various dialogs are displayed.
     */
    private boolean ignoreHotKeys = false;

    /**
     * Creates and lays out a new Board Editor frame.
     */
    public BoardEditor(MegaMekController c) {
        controller = c;
        try {
            bv = new BoardView1(game, controller, null);
            bvc = bv.getComponent(true);
            bv.setDisplayInvalidHexInfo(true);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(frame,
                    Messages.getString("BoardEditor.CouldntInitialize") + e,
                    Messages.getString("BoardEditor.FatalError"), JOptionPane.ERROR_MESSAGE);
            frame.dispose();
        }

        // Add a mouse listener for mouse button release 
        // to handle Undo
        bv.addMouseListener(new MouseAdapter() {
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
                        // When Undo (without Redo) has been used after saving
                        // and the user draws on the board, then it can
                        // no longer know if it's been returned to the saved state
                        // and it will always be treated as changed.
                        if (savedUndoStackSize > undoStack.size()) {
                            canReturnToSaved = false;
                        }
                        hasChanges = !canReturnToSaved | (undoStack.size() != savedUndoStackSize);
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
                // return if there are no or no valid coords or if we click the same hex again
                // unless Raise/Lower Terrain is active which should let us click the same hex 
                if ((c == null) || (c.equals(lastClicked) && !buttonUpDn.isSelected())
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
                if (buttonUpDn.isSelected()) {
                    // Mouse Button released
                    if (b.getType() == BoardViewEvent.BOARD_HEX_CLICKED) {
                        hexLeveltoDraw = -1000;
                        isDragging = false;
                    }

                    // Mouse Button clicked or dragged
                    if ((b.getType() == BoardViewEvent.BOARD_HEX_DRAGGED) && isLMB) {
                        if (!isDragging) {
                            hexLeveltoDraw = board.getHex(c).getLevel();
                            if (isALT) {
                                hexLeveltoDraw--;
                            } else if (isSHIFT) {
                                hexLeveltoDraw++;
                            }
                            isDragging = true;
                        }
                    }

                    // CORRECTION, click outside the board then drag inside???
                    if (hexLeveltoDraw != -1000) {
                        LinkedList<Coords> allBrushHexes = getBrushCoords(c) ;
                        for (Coords h: allBrushHexes) {
                            if (!buttonOOC.isSelected() || board.getHex(h).isClearHex()) {
                                saveToUndo(h);
                                relevelHex(h);
                            }   
                        }
                    }
                    // ------- End Raise/Lower Terrain
                } else if (isLMB || (b.getModifiers() & InputEvent.BUTTON1_DOWN_MASK) != 0) {
                    // 'isLMB' is true if a button 1 is associated to a click or release but not while dragging.
                    // The left button down mask is checked because we could be dragging.
                    
                    // Normal texture paint
                    if (isALT) { // ALT-Click
                        setCurrentHex(board.getHex(b.getCoords()));
                    } else {
                        LinkedList<Coords> allBrushHexes = getBrushCoords(c);
                        for (Coords h: allBrushHexes) {
                            // test if texture overwriting is active
                            if ((!buttonOOC.isSelected() || board.getHex(h).isClearHex())
                                    && curHex.isValid(null)) {
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
        
        bv.setUseLOSTool(false);
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
        
        adaptToGUIScale();
        GUIPreferences.getInstance().addPreferenceChangeListener(this);
    }

    /**
     * Sets up the frame that will display the editor.
     */
    private void setupFrame() {
        setFrameTitle();
        frame.getContentPane().add(bvc, BorderLayout.CENTER);
        frame.getContentPane().add(this, BorderLayout.EAST);
        
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
                if (hasChanges) {
                    ignoreHotKeys = true;
                    int savePrompt = JOptionPane.showConfirmDialog(null,
                            Messages.getString("BoardEditor.exitprompt"),
                            Messages.getString("BoardEditor.exittitle"),
                            JOptionPane.YES_NO_CANCEL_OPTION,
                            JOptionPane.WARNING_MESSAGE);
                    ignoreHotKeys = false;

                    // When the user cancels or did not actually save the board, don't close 
                    if (((savePrompt == JOptionPane.YES_OPTION) && !boardSave(false)) || 
                            (savePrompt == JOptionPane.CANCEL_OPTION)) {
                        return;
                    } 
                }

                // otherwise: exit the Map Editor
                minimapW.setVisible(false);
                if (controller != null) {
                    controller.removeAllActions();
                    controller.boardEditor = null;
                }
                frame.dispose();
            }

            @Override
            public void windowClosed(WindowEvent e) {
                GUIPreferences.getInstance().removePreferenceChangeListener(BoardEditor.this);
            }
        });
    }

    /**
     * Sets up Scaling Icon Buttons
     */
    private ScalingIconButton prepareButton(String iconName, String buttonName, 
            List<ScalingIconButton> bList, int width) {
        // Get the normal icon
        File file = new MegaMekFile(Configuration.widgetsDir(), "/MapEditor/"+iconName+".png").getFile();
        Image imageButton = ImageUtil.loadImageFromFile(file.getAbsolutePath());
        if (imageButton == null) {
            imageButton = ImageUtil.failStandardImage();
        }
        ScalingIconButton button = new ScalingIconButton(imageButton, width);

        // Get the hover icon
        file = new MegaMekFile(Configuration.widgetsDir(), "/MapEditor/"+iconName+"_H.png").getFile();
        imageButton = ImageUtil.loadImageFromFile(file.getAbsolutePath());
        button.setRolloverImage(imageButton);
        
        // Get the disabled icon, if any
        file = new MegaMekFile(Configuration.widgetsDir(), "/MapEditor/"+iconName+"_G.png").getFile();
        imageButton = ImageUtil.loadImageFromFile(file.getAbsolutePath());
        button.setDisabledImage(imageButton);

        String tt = Messages.getString("BoardEditor."+iconName+"TT");
        if (tt.length() != 0) {
            button.setToolTipText(tt);
        }
        button.setMargin(new Insets(0,0,0,0));
        if (bList != null) {
            bList.add(button);
        }
        button.addActionListener(this);
        return button;
    }
    
    /**
     * Sets up Scaling Icon ToggleButtons
     */
    private ScalingIconToggleButton prepareToggleButton(String iconName, String buttonName, 
            List<ScalingIconToggleButton> bList, int width) {
        // Get the normal icon
        File file = new MegaMekFile(Configuration.widgetsDir(), "/MapEditor/"+iconName+".png").getFile();
        Image imageButton = ImageUtil.loadImageFromFile(file.getAbsolutePath());
        if (imageButton == null) {
            imageButton = ImageUtil.failStandardImage();
        }
        ScalingIconToggleButton button = new ScalingIconToggleButton(imageButton, width);
        
        // Get the hover icon
        file = new MegaMekFile(Configuration.widgetsDir(), "/MapEditor/"+iconName+"_H.png").getFile();
        imageButton = ImageUtil.loadImageFromFile(file.getAbsolutePath());
        button.setRolloverImage(imageButton);
        
        // Get the selected icon, if any
        file = new MegaMekFile(Configuration.widgetsDir(), "/MapEditor/"+iconName+"_S.png").getFile();
        imageButton = ImageUtil.loadImageFromFile(file.getAbsolutePath());
        button.setSelectedImage(imageButton);
        
        button.setToolTipText(Messages.getString("BoardEditor."+iconName+"TT"));
        if (bList != null) {
            bList.add(button);
        }
        button.addActionListener(this);
        return button;
    }
    
    /**
     * Sets up the editor panel, which goes on the right of the map and has
     * controls for editing the current square.
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

        buttonBrush1 = prepareToggleButton("ButtonHex1", "Brush1", brushButtons, BASE_ARROWBUTTON_ICON_WIDTH);
        buttonBrush2 = prepareToggleButton("ButtonHex7", "Brush2", brushButtons, BASE_ARROWBUTTON_ICON_WIDTH);
        buttonBrush3 = prepareToggleButton("ButtonHex19", "Brush3", brushButtons, BASE_ARROWBUTTON_ICON_WIDTH);
        ButtonGroup brushGroup = new ButtonGroup();
        brushGroup.add(buttonBrush1);
        brushGroup.add(buttonBrush2);
        brushGroup.add(buttonBrush3);
        buttonOOC = prepareToggleButton("ButtonOOC", "OOC", brushButtons, BASE_ARROWBUTTON_ICON_WIDTH);
        buttonUpDn = prepareToggleButton("ButtonUpDn", "UpDown", brushButtons, BASE_ARROWBUTTON_ICON_WIDTH);

        buttonUndo = prepareButton("ButtonUndo", "Undo", undoButtons, BASE_ARROWBUTTON_ICON_WIDTH);
        buttonRedo = prepareButton("ButtonRedo", "Redo", undoButtons, BASE_ARROWBUTTON_ICON_WIDTH);
        buttonUndo.setEnabled(false);
        buttonRedo.setEnabled(false);

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

            IHex saveHex = curHex.duplicate();
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
                curHex.addTerrain(Terrains.getTerrainFactory().createTerrain(Terrains.FOLIAGE_ELEV, elev));
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
            // If we don't have at least one of the building values, overwrite the current hex
            if (!curHex.containsTerrain(Terrains.BLDG_CF)
                    && !curHex.containsTerrain(Terrains.BLDG_ELEV)
                    && !curHex.containsTerrain(Terrains.BUILDING)) {
                curHex.removeAllTerrains();
            }
            // Restore mandatory building parts if some are missing
            setBasicBuilding(false);
            int wheelDir = (e.getWheelRotation() < 0) ? 1 : -1;

            if (e.isShiftDown()) {
                int oldLevel = curHex.getTerrain(Terrains.BLDG_CF).getLevel();
                int newLevel = Math.max(10, oldLevel + (wheelDir * 5));
                curHex.addTerrain(TF.createTerrain(Terrains.BLDG_CF, newLevel));
            } else if (e.isControlDown()) {
                int oldLevel = curHex.getTerrain(Terrains.BUILDING).getLevel();
                int newLevel = Math.max(1, Math.min(4, oldLevel + wheelDir)); // keep between 1 and 4

                if (newLevel != oldLevel) {
                    ITerrain curTerr = curHex.getTerrain(Terrains.BUILDING);
                    curHex.addTerrain(TF.createTerrain(Terrains.BUILDING, 
                            newLevel, curTerr.hasExitsSpecified(), curTerr.getExits()));

                    // Set the CF to the appropriate standard value *IF* it is the appropriate value now,
                    // i.e. if the user has not manually set it to something else
                    int curCF = curHex.getTerrain(Terrains.BLDG_CF).getLevel();
                    if (curCF == Building.getDefaultCF(oldLevel)) {
                        curHex.addTerrain(TF.createTerrain(Terrains.BLDG_CF, Building.getDefaultCF(newLevel)));
                    }
                }
            } else {
                int oldLevel = curHex.getTerrain(Terrains.BLDG_ELEV).getLevel();
                int newLevel = Math.max(1, oldLevel + wheelDir);
                curHex.addTerrain(TF.createTerrain(Terrains.BLDG_ELEV, newLevel));
            }

            refreshTerrainList();
            repaintWorkingHex();
        });

        // Mouse wheel behaviour for the BRIDGE button
        buttonBr.addMouseWheelListener(e -> {
            // If we don't have at least one of the bridge values, overwrite the current hex
            if (!curHex.containsTerrain(Terrains.BRIDGE_CF)
                    && !curHex.containsTerrain(Terrains.BRIDGE_ELEV)
                    && !curHex.containsTerrain(Terrains.BRIDGE)) {
                curHex.removeAllTerrains();
            }
            setBasicBridge();
            int wheelDir = (e.getWheelRotation() < 0) ? 1 : -1;
            int terrainType;
            int newLevel;

            if (e.isShiftDown()) {
                terrainType = Terrains.BRIDGE_CF;
                int oldLevel = curHex.getTerrain(terrainType).getLevel();
                newLevel = Math.max(10, oldLevel + wheelDir*10);
                curHex.addTerrain(TF.createTerrain(terrainType, newLevel));
            } else if (e.isControlDown()) {
                ITerrain terrain = curHex.getTerrain(Terrains.BRIDGE);
                boolean hasExits = terrain.hasExitsSpecified();
                int exits = terrain.getExits();
                newLevel = Math.max(1, terrain.getLevel() + wheelDir);
                curHex.addTerrain(TF.createTerrain(Terrains.BRIDGE, newLevel, hasExits, exits));
            } else {
                terrainType = Terrains.BRIDGE_ELEV;
                int oldLevel = curHex.getTerrain(terrainType).getLevel();
                newLevel = Math.max(0, oldLevel + wheelDir);
                curHex.addTerrain(TF.createTerrain(terrainType, newLevel));
            }

            refreshTerrainList();
            repaintWorkingHex();
        });

        // Mouse wheel behaviour for the FUELTANKS button
        buttonFT.addMouseWheelListener(e -> {
            // If we don't have at least one of the fuel tank values, overwrite the current hex
            if (!curHex.containsTerrain(Terrains.FUEL_TANK)
                    && !curHex.containsTerrain(Terrains.FUEL_TANK_CF)
                    && !curHex.containsTerrain(Terrains.FUEL_TANK_ELEV)
                    && !curHex.containsTerrain(Terrains.FUEL_TANK_MAGN)) {
                curHex.removeAllTerrains();
            }
            setBasicFuelTank();
            int wheelDir = (e.getWheelRotation() < 0) ? 1 : -1;
            int terrainType;
            int newLevel;

            if (e.isShiftDown()) {
                terrainType = Terrains.FUEL_TANK_CF;
                int oldLevel = curHex.getTerrain(terrainType).getLevel();
                newLevel = Math.max(10, oldLevel + wheelDir*10);
            } else if (e.isControlDown()) {
                terrainType = Terrains.FUEL_TANK_MAGN;
                int oldLevel = curHex.getTerrain(terrainType).getLevel();
                newLevel = Math.max(10, oldLevel + wheelDir*10);
            } else {
                terrainType = Terrains.FUEL_TANK_ELEV;
                int oldLevel = curHex.getTerrain(terrainType).getLevel();
                newLevel = Math.max(1, oldLevel + wheelDir);
            }

            curHex.addTerrain(TF.createTerrain(terrainType, newLevel));
            refreshTerrainList();
            repaintWorkingHex();
        });

        FixedYPanel terrainButtonPanel = new FixedYPanel(new GridLayout(0, 4, 2, 2));
        addManyButtons(terrainButtonPanel, terrainButtons);

        FixedYPanel brushButtonPanel = new FixedYPanel(new GridLayout(0, 3, 2, 2));
        addManyButtons(brushButtonPanel, brushButtons);
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
        lisTerrainRenderer = new ComboboxToolTipRenderer();
        lisTerrain = new JList<>(new DefaultListModel<>());
        lisTerrain.addListSelectionListener(this);
        lisTerrain.setCellRenderer(lisTerrainRenderer);
        lisTerrain.setVisibleRowCount(6);
        lisTerrain.setFixedCellWidth(180);
        refreshTerrainList();

        // Terrain List, Preview, Delete
        FixedYPanel panlisHex = new FixedYPanel(new FlowLayout(FlowLayout.LEFT, 4, 4));
        butDelTerrain = prepareButton("buttonRemT", "Delete Terrain", null, BASE_ARROWBUTTON_ICON_WIDTH);
        butDelTerrain.setEnabled(false);
        canHex = new HexCanvas();
        panlisHex.add(butDelTerrain);
        panlisHex.add(new JScrollPane(lisTerrain));
        panlisHex.add(canHex);

        // Build the terrain list for the chooser ComboBox,
        // excluding terrains that are handled internally
        ArrayList<TerrainHelper> tList = new ArrayList<>();
        for (int i = 1; i < Terrains.SIZE; i++) {
            if (!Terrains.AUTOMATIC.contains(i)) {
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
        ComboboxToolTipRenderer renderer = new ComboboxToolTipRenderer();
        renderer.setTerrains(terrains);
        choTerrainType.setRenderer(renderer);
        // Selecting a terrain type in the Dropdown should deselect
        // all in the terrain overview list except when selected from there
        choTerrainType.addActionListener(e -> { if (!terrListBlocker) lisTerrain.clearSelection(); });
        choTerrainType.setFont(fontComboTerr);
        butAddTerrain = new JButton(Messages.getString("BoardEditor.butAddTerrain"));
        butTerrUp = prepareButton("ButtonTLUP", "Increase Terrain Level", null, BASE_ARROWBUTTON_ICON_WIDTH);
        butTerrDown = prepareButton("ButtonTLDN", "Decrease Terrain Level", null, BASE_ARROWBUTTON_ICON_WIDTH);

        // Minimap Toggle
        butMiniMap = new JButton(Messages.getString("BoardEditor.butMiniMap"));
        butMiniMap.setActionCommand(ClientGUI.VIEW_MINI_MAP);

        // Exits
        cheTerrExitSpecified = new JCheckBox(Messages.getString("BoardEditor.cheTerrExitSpecified"));
        cheTerrExitSpecified.addActionListener(e -> {
            noTextFieldUpdate = true;
            updateWhenSelected();
            noTextFieldUpdate = false;
        });
        butTerrExits = prepareButton("ButtonExitA", Messages.getString("BoardEditor.butTerrExits"), null, BASE_ARROWBUTTON_ICON_WIDTH);
        texTerrExits = new EditorTextField("0", 2, 0); //$NON-NLS-1$
        texTerrExits.addActionListener(this);
        texTerrExits.getDocument().addDocumentListener(this);
        butExitUp = prepareButton("ButtonEXUP", "Increase Exit / Gfx", null, BASE_ARROWBUTTON_ICON_WIDTH);
        butExitDown = prepareButton("ButtonEXDN", "Decrease Exit / Gfx", null, BASE_ARROWBUTTON_ICON_WIDTH);

        // Arrows and text fields for type and exits
        JPanel panUP = new JPanel(new GridLayout(1,0,4,4));
        panUP.add(butTerrUp);
        panUP.add(butExitUp);
        panUP.add(butTerrExits);
        JPanel panTex = new JPanel(new GridLayout(1,0,4,4));
        panTex.add(texTerrainLevel);
        panTex.add(texTerrExits);
        panTex.add(cheTerrExitSpecified);
        JPanel panDN = new JPanel(new GridLayout(1,0,4,4));
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
        for (String s: themes) choTheme.addItem(s);
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
        butBoardNew = new JButton(Messages.getString("BoardEditor.butBoardNew"));
        butBoardNew.setActionCommand(ClientGUI.FILE_BOARD_NEW);

        butExpandMap = new JButton(Messages.getString("BoardEditor.butExpandMap"));
        butExpandMap.setActionCommand(FILE_BOARD_EDITOR_EXPAND);

        butBoardOpen = new JButton(Messages.getString("BoardEditor.butBoardOpen"));
        butBoardOpen.setActionCommand(ClientGUI.FILE_BOARD_OPEN);

        butBoardSave = new JButton(Messages.getString("BoardEditor.butBoardSave"));
        butBoardSave.setActionCommand(ClientGUI.FILE_BOARD_SAVE);

        butBoardSaveAs = new JButton(Messages.getString("BoardEditor.butBoardSaveAs"));
        butBoardSaveAs.setActionCommand(ClientGUI.FILE_BOARD_SAVE_AS);

        butBoardSaveAsImage = new JButton(Messages.getString("BoardEditor.butBoardSaveAsImage"));
        butBoardSaveAsImage.setActionCommand(ClientGUI.FILE_BOARD_SAVE_AS_IMAGE);

        butBoardValidate = new JButton(Messages.getString("BoardEditor.butBoardValidate"));
        butBoardValidate.setActionCommand(FILE_BOARD_EDITOR_VALIDATE);
        
        butSourceFile = new JButton(Messages.getString("BoardEditor.butSourceFile"));
        butSourceFile.setActionCommand(FILE_SOURCEFILE);

        addManyActionListeners(butBoardValidate, butBoardSaveAsImage, butBoardSaveAs, butBoardSave);
        addManyActionListeners(butBoardOpen, butExpandMap, butBoardNew, butMiniMap);
        addManyActionListeners(butDelTerrain, butAddTerrain, butSourceFile);
        
        JPanel panButtons = new JPanel(new GridLayout(3, 2, 2, 2));
        addManyButtons(panButtons, List.of(butBoardNew, butBoardSave, butBoardOpen,
                butExpandMap, butBoardSaveAs, butBoardSaveAsImage));
        panButtons.add(butBoardValidate);
        panButtons.add(butMiniMap);
        if (Desktop.isDesktopSupported()) {
            panButtons.add(butSourceFile);
        }

        // Arrange everything
        setLayout(new BorderLayout());
        var centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.PAGE_AXIS));
        centerPanel.add(labHelp1);
        centerPanel.add(labHelp2);
        centerPanel.add(terrainButtonPanel);
        centerPanel.add(brushButtonPanel);
        centerPanel.add(Box.createVerticalGlue());
        centerPanel.add(undoButtonPanel);
        centerPanel.add(Box.createVerticalGlue());
        centerPanel.add(panelBoardSettings);
        centerPanel.add(panelHexSettings);
        centerPanel.add(panelTerrSettings);
        centerPanel.add(panlisHex);
        var scrCenterPanel = new JScrollPane(centerPanel);
        scrCenterPanel.getVerticalScrollBar().setUnitIncrement(16);
        add(scrCenterPanel, BorderLayout.CENTER);
        add(panButtons, BorderLayout.PAGE_END);

        // Set up the minimap 
        minimapW = new JDialog(frame, Messages.getString("BoardEditor.minimapW"), false);
        minimapW.setLocation(guip.getMinimapPosX(), guip.getMinimapPosY());
        try {
            minimap = new MiniMap(minimapW, game, bv);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(frame,
                    Messages.getString("BoardEditor.CouldNotInitialiseMinimap") + e,
                    Messages.getString("BoardEditor.FatalError"), JOptionPane.ERROR_MESSAGE);
            frame.dispose();
        }
        minimapW.add(minimap);
        minimapW.setVisible(true);
    }
    
    /**
     * Returns coords that the active brush will paint on;
     * returns only coords that are valid, i.e. on the board
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
        for (JButton button: buttons) {
            button.addActionListener(this);
        }
    }
    
    // Helper to shorten the code
    private void addManyButtons(JPanel panel, List<? extends AbstractButton> terrainButtons) {
        terrainButtons.stream().forEach(panel::add);
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
            IHex hex = board.getHex(c).duplicate();
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
     * Changes the hex level at Coords c. Expects c 
     * to be on the board.
     */
    private void relevelHex(Coords c) {
        IHex newHex = board.getHex(c).duplicate(); 
        newHex.setLevel(hexLeveltoDraw);
        board.resetStoredElevation();
        board.setHex(c, newHex);
        
    }

    /**
     * Apply the current Hex to the Board at the specified location.
     */
    void paintHex(Coords c) {
        board.resetStoredElevation();
        board.setHex(c, curHex.duplicate());
    } 
    
    /**
     * Apply the current Hex to the Board at the specified location.
     */
    public void retextureHex(Coords c) {
        if (board.contains(c)) {
            IHex newHex = curHex.duplicate();
            newHex.setLevel(board.getHex(c).getLevel());
            board.resetStoredElevation();
            board.setHex(c, newHex);
        }
    }

    /**
     * Apply the current Hex to the Board at the specified location.
     */
    public void addToHex(Coords c) {
        if (board.contains(c)) {
            IHex newHex = curHex.duplicate();
            IHex oldHex = board.getHex(c);
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
     * Sets the working hex to <code>hex</code>;
     * used for mouse ALT-click (eyedropper function).
     *
     * @param hex hex to set.
     */
    void setCurrentHex(IHex hex) {
        curHex = hex.duplicate();
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
        ((DefaultListModel<TerrainTypeHelper>) lisTerrain.getModel()).removeAllElements();
        lisTerrainRenderer.setTerrainTypes(null);
        int[] terrainTypes = curHex.getTerrainTypes();
        List<TerrainTypeHelper> types = new ArrayList<>();
        for (int terrainType : terrainTypes) {
            ITerrain terrain = curHex.getTerrain(terrainType);
            if ((terrain != null) && !Terrains.AUTOMATIC.contains(terrainType)) {
                TerrainTypeHelper tth = new TerrainTypeHelper(terrain);
                types.add(tth);
            }
        }
        Collections.sort(types);
        for (TerrainTypeHelper tth : types) {
            ((DefaultListModel<TerrainTypeHelper>) lisTerrain.getModel()).addElement(tth);
        }
        lisTerrainRenderer.setTerrainTypes(types);
    }

    /**
     * Returns a new instance of the terrain that is currently entered in the
     * terrain input fields
     */
    private ITerrain enteredTerrain() {
        int type = ((TerrainHelper) Objects.requireNonNull(choTerrainType.getSelectedItem())).getTerrainType();
        int level = texTerrainLevel.getNumber();  
        // For the terrain subtypes that only add to a main terrain type exits make no
        // sense at all. Therefore simply do not add them
        if ((type == Terrains.BLDG_ARMOR) || (type == Terrains.BLDG_CF) 
                || (type == Terrains.BLDG_ELEV) || (type == Terrains.BLDG_CLASS)  
                || (type == Terrains.BLDG_BASE_COLLAPSED) || (type == Terrains.BLDG_BASEMENT_TYPE)
                || (type == Terrains.BRIDGE_CF) || (type == Terrains.BRIDGE_ELEV)
                || (type == Terrains.FUEL_TANK_CF) || (type == Terrains.FUEL_TANK_ELEV)
                || (type == Terrains.FUEL_TANK_MAGN)) {
            return Terrains.getTerrainFactory().createTerrain(type, level, false, 0);
        } else {
            boolean exitsSpecified = cheTerrExitSpecified.isSelected();
            int exits = texTerrExits.getNumber();
            return Terrains.getTerrainFactory().createTerrain(type, level, exitsSpecified, exits);
        }
    }

    /**
     * Add or set the terrain to the list based on the fields.
     */
    private void addSetTerrain() {
        ITerrain toAdd = enteredTerrain();
        if (((toAdd.getType() == Terrains.BLDG_ELEV) || (toAdd.getType() == Terrains.BRIDGE_ELEV))
                && (toAdd.getLevel() < 0)) {
            texTerrainLevel.setNumber(0);
            JOptionPane.showMessageDialog(frame,
                    Messages.getString("BoardEditor.BridgeBuildingElevError"),
                    Messages.getString("BoardEditor.invalidTerrainTitle"),
                    JOptionPane.ERROR_MESSAGE);
            return;
        }
        curHex.addTerrain(toAdd);
        int formerSelection = lisTerrain.getSelectedIndex();
        noTextFieldUpdate = true;
        refreshTerrainList();
        lisTerrain.setSelectedIndex(formerSelection);
        lisTerrain.ensureIndexIsVisible(formerSelection);
        repaintWorkingHex();
        noTextFieldUpdate = false;
    }
    
    /**
     * Add to the terrain from one of the easy access buttons
     */
    private void addSetTerrainEasy(int type, int level) {
        boolean exitsSpecified = cheTerrExitSpecified.isSelected();
        int exits = texTerrExits.getNumber();
        ITerrain toAdd = Terrains.getTerrainFactory().createTerrain(type, level, exitsSpecified, exits);
        curHex.addTerrain(toAdd);
        TerrainTypeHelper toSelect = new TerrainTypeHelper(toAdd);
        refreshTerrainList();
        lisTerrain.setSelectedValue(toSelect, true);
        repaintWorkingHex();
    }
    
    /**
     * Sets valid basic Fuel Tank values as far as they are missing
     */
    private void setBasicFuelTank() {
        // There is only fuel_tank:1, so this can be set
        curHex.addTerrain(TF.createTerrain(Terrains.FUEL_TANK, 1, true, 0));

        if (!curHex.containsTerrain(Terrains.FUEL_TANK_CF)) {
            curHex.addTerrain(TF.createTerrain(Terrains.FUEL_TANK_CF, 40, false, 0));
        }
        
        if (!curHex.containsTerrain(Terrains.FUEL_TANK_ELEV)) {
            curHex.addTerrain(TF.createTerrain(Terrains.FUEL_TANK_ELEV, 1, false, 0));
        }
        
        if (!curHex.containsTerrain(Terrains.FUEL_TANK_MAGN)) {
            curHex.addTerrain(TF.createTerrain(Terrains.FUEL_TANK_MAGN, 100, false, 0));
        }

        refreshTerrainList();
        repaintWorkingHex();
    }
    
    /**
     * Sets valid basic bridge values as far as they are missing
     */
    private void setBasicBridge() {
        if (!curHex.containsTerrain(Terrains.BRIDGE_CF)) {
            curHex.addTerrain(TF.createTerrain(Terrains.BRIDGE_CF, 40, false, 0));
        }
        
        if (!curHex.containsTerrain(Terrains.BRIDGE_ELEV)) {
            curHex.addTerrain(TF.createTerrain(Terrains.BRIDGE_ELEV, 1, false, 0));
        }
        
        if (!curHex.containsTerrain(Terrains.BRIDGE)) {
            curHex.addTerrain(TF.createTerrain(Terrains.BRIDGE, 1, false, 0));
        }
        
        refreshTerrainList();
        repaintWorkingHex();
    }
    
    /**
     * Sets valid basic Building values as far as they are missing
     */
    private void setBasicBuilding(boolean ALT_Held) {
        if (!curHex.containsTerrain(Terrains.BLDG_CF)) {
            curHex.addTerrain(TF.createTerrain(Terrains.BLDG_CF, 15, false, 0));
        }

        if (!curHex.containsTerrain(Terrains.BLDG_ELEV)) {
            curHex.addTerrain(TF.createTerrain(Terrains.BLDG_ELEV, 1, false, 0));
        }

        if (!curHex.containsTerrain(Terrains.BUILDING)) {
            curHex.addTerrain(TF.createTerrain(Terrains.BUILDING, 1, ALT_Held, 0));
        }

        // When clicked with ALT, only toggle the exits
        if (ALT_Held) {
            ITerrain curTerr = curHex.getTerrain(Terrains.BUILDING);
            curHex.addTerrain(TF.createTerrain(Terrains.BUILDING, 
                    curTerr.getLevel(), !curTerr.hasExitsSpecified(), curTerr.getExits()));
        }

        refreshTerrainList();
        repaintWorkingHex();
    }

    /**
     * Set all the appropriate terrain fields to match the currently selected
     * terrain in the list.
     */
    private void refreshTerrainFromList() {
        if (lisTerrain.isSelectionEmpty()) {
            butDelTerrain.setEnabled(false);
        } else {
            butDelTerrain.setEnabled(true);
            ITerrain terrain = Terrains.getTerrainFactory().createTerrain(
                    lisTerrain.getSelectedValue().getTerrain());
            terrain = curHex.getTerrain(terrain.getType());
            TerrainHelper terrainHelper = new TerrainHelper(terrain.getType());
            terrListBlocker = true;
            choTerrainType.setSelectedItem(terrainHelper);
            texTerrainLevel.setText(Integer.toString(terrain.getLevel()));
            cheTerrExitSpecified.setSelected(terrain.hasExitsSpecified());
            texTerrExits.setNumber(terrain.getExits());
            terrListBlocker = false;
        }
    }

    /**
     * Updates the selected terrain in the terrain list if
     * a terrain is actually selected
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

    // When we resize a board, implant the old board's hexes where they should be in the new board
    public IBoard implantOldBoard(IGame game, int west, int north, int east, int south) {
        IBoard oldBoard = game.getBoard();
        for (int x = 0; x < oldBoard.getWidth(); x++) {
            for (int y = 0; y < oldBoard.getHeight(); y++) {
                int newX = x + west;
                int odd = x & 1 & west;
                int newY = y + north + odd;
                if (oldBoard.contains(x, y) && board.contains(newX, newY)) {
                    IHex oldHex = oldBoard.getHex(x, y);
                    IHex hex = board.getHex(newX, newY);
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

    public void boardLoad() {
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
        curBoardFile = fc.getSelectedFile();
        loadPath = curBoardFile.getParentFile();
        // load!
        try (InputStream is = new FileInputStream(fc.getSelectedFile())) {            
            // tell the board to load!
            board.load(is, null, true);
            // Board generation in a game always calls BoardUtilities.combine
            // This serves no purpose here, but is necessary to create 
            // flipBGVert/flipBGHoriz lists for the board, which is necessary 
            // for the background image to work in the BoardEditor
            board = BoardUtilities.combine(board.getWidth(), board.getHeight(), 1, 1, 
                    new IBoard[]{board}, Collections.singletonList(false), MapSettings.MEDIUM_GROUND);
            game.setBoard(board);
            cheRoadsAutoExit.setSelected(board.getRoadsAutoExit());
            mapSettings.setBoardSize(board.getWidth(), board.getHeight());
            
            // Now, *after* initialization of the board which will correct some errors,
            // do a board validation
            validateBoard(false);
            
            refreshTerrainList();
            setupUiFreshBoard();
        } catch (IOException ex) {
            MegaMek.getLogger().error(ex);
        }
    }
    
    /**
     * Will do board.initializeHex() for all hexes, correcting 
     * building and road connection issues for those hexes that do not have
     * the exits check set.
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
        if (curfileImage == null) {
            boardSaveAsImage(ignoreUnits);
            return;
        }
        JDialog waitD = new JDialog(frame, Messages.getString("BoardEditor.waitDialog.title"));
        waitD.add(new JLabel(Messages.getString("BoardEditor.waitDialog.message")));
        waitD.setSize(250, 130);
        // move to middle of screen
        waitD.setLocation(
                (frame.getSize().width / 2) - (waitD.getSize().width / 2),
                (frame.getSize().height / 2) - (waitD.getSize().height / 2));
        waitD.setVisible(true);
        frame.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        waitD.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        // save!
        try {
            ImageIO.write(bv.getEntireBoardImage(ignoreUnits), "png", curfileImage);
        } catch (IOException e) {
            MegaMek.getLogger().error(e);
        }
        waitD.setVisible(false);
        frame.setCursor(Cursor.getDefaultCursor());
    }

    /**
     * Saves the board to a .board file. 
     * When saveAs is true, acts as a Save As... by opening a file chooser dialog.
     * When saveAs is false, it will directly save to the current board file name,
     * if it is known and otherwise act as Save As... 
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
            setFrameTitle();
            return true;
        } catch (IOException e) {
            MegaMek.getLogger().error(e);
            return false;
        }
    }
    
    /** 
     * Shows a dialog for choosing a .board file to save to. 
     * Sets curBoardFile and returns true when a valid file was chosen.
     * Returns false otherwise.
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
     * Opens a file dialog box to select a file to save as; saves the board to
     * the file as an image. Useful for printing boards.
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
        if ((returnVal != JFileChooser.APPROVE_OPTION)
            || (fc.getSelectedFile() == null)) {
            // I want a file, y'know!
            return;
        }
        curfileImage = fc.getSelectedFile();

        // make sure the file ends in png
        if (!curfileImage.getName().toLowerCase().endsWith(".png")) {
            try {
                curfileImage = new File(curfileImage.getCanonicalPath() + ".png");
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
                cheTerrExitSpecified.setSelected(true);
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
        if (about == null) {
            about = new CommonAboutDialog(frame);
        }
        about.setVisible(true);
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
        if (setdlg == null) {
            setdlg = new CommonSettingsDialog(frame);
        }
        setdlg.setVisible(true);
    }
    
    /** 
     * Adjusts some UI and internal settings for a freshly 
     * loaded or freshly generated board.
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
        menuBar.setBoard(true);
        bvc.doLayout();
        setFrameTitle();
    }
    
    /** 
     * Performs board validation. When showPositiveResult is true,
     * the result of the validation will be shown in a dialog. 
     * Otherwise, only a negative result (the board has errors) will 
     * be shown.
     */
    private void validateBoard(boolean showPositiveResult) {
        StringBuffer errBuff = new StringBuffer();
        board.isValid(errBuff);
        if ((errBuff.length() > 0) || showPositiveResult) {
            showBoardValidationReport(errBuff);
        }
    }

    /**
     * Shows a board validation report dialog, reporting either
     * the contents of errBuff or that the board has no errors.
     */
    private void showBoardValidationReport(StringBuffer errBuff) {
        ignoreHotKeys = true;
        if ((errBuff != null) && errBuff.length() > 0) {
            String title = Messages.getString("BoardEditor.invalidBoard.title");
            String msg = Messages.getString("BoardEditor.invalidBoard.report");
            msg += errBuff;
            JTextArea textArea = new JTextArea(msg);
            JScrollPane scrollPane = new JScrollPane(textArea);
            textArea.setLineWrap(true);
            textArea.setWrapStyleWord(true);
            scrollPane.setPreferredSize(new Dimension(getWidth(), getHeight() / 2));
            JOptionPane.showMessageDialog(frame, scrollPane, title, JOptionPane.ERROR_MESSAGE);
        } else {
            String title =  Messages.getString("BoardEditor.validBoard.title");
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
        if (ae.getActionCommand().equals(ClientGUI.FILE_BOARD_NEW)) {
            ignoreHotKeys = true;
            boardNew(true);
            ignoreHotKeys = false;
        } else if (ae.getActionCommand().equals(FILE_BOARD_EDITOR_EXPAND)) {
            ignoreHotKeys = true;
            boardResize();
            ignoreHotKeys = false;
        } else if (ae.getActionCommand().equals(ClientGUI.FILE_BOARD_OPEN)) {
            ignoreHotKeys = true;
            boardLoad();
            ignoreHotKeys = false;
        } else if (ae.getActionCommand().equals(ClientGUI.FILE_BOARD_SAVE)) {
            ignoreHotKeys = true;
            boardSave(false);
            ignoreHotKeys = false;
        } else if (ae.getActionCommand().equals(ClientGUI.FILE_BOARD_SAVE_AS)) {
            ignoreHotKeys = true;
            boardSave(true);
            ignoreHotKeys = false;
        } else if (ae.getActionCommand().equals(ClientGUI.FILE_BOARD_SAVE_AS_IMAGE)) {
            ignoreHotKeys = true;
            boardSaveAsImage(false);
            ignoreHotKeys = false;
        } else if (ae.getActionCommand().equals(FILE_SOURCEFILE)) {
            if (curBoardFile != null) {
                try {
                    Desktop.getDesktop().open(curBoardFile);
                } catch (IOException e) {
                    ignoreHotKeys = true;
                    JOptionPane.showMessageDialog(frame,
                            Messages.getString("BoardEditor.OpenFileError", curBoardFile.toString())
                            + e.getMessage());
                    MegaMek.getLogger().error(e);
                    ignoreHotKeys = false;
                }
            }
        } else if (ae.getActionCommand().equals(FILE_BOARD_EDITOR_VALIDATE)) {
            correctExits();
            validateBoard(true);
        } else if (ae.getSource().equals(butDelTerrain) && !lisTerrain.isSelectionEmpty()) {
            ITerrain toRemove = Terrains.getTerrainFactory().createTerrain(
                    lisTerrain.getSelectedValue().getTerrain());
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
                cheTerrExitSpecified.setSelected(false);
            } else if (exitsVal > 63) {
                texTerrExits.setNumber(63);
            }
            updateWhenSelected();
        } else if (ae.getSource().equals(butTerrExits)) {
            ExitsDialog ed = new ExitsDialog(frame);
            int exitsVal = texTerrExits.getNumber();
            ed.setExits(exitsVal);
            ed.setVisible(true);
            exitsVal = ed.getExits();
            texTerrExits.setNumber(exitsVal);
            cheTerrExitSpecified.setSelected(exitsVal != 0);
            updateWhenSelected();
        } else if (ae.getSource().equals(butExitUp)) {
            cheTerrExitSpecified.setSelected(true);
            if (texTerrExits.getNumber() < 63) {
                texTerrExits.incValue();
            }
            updateWhenSelected();
        } else if (ae.getSource().equals(butExitDown)) {
            texTerrExits.decValue();
            cheTerrExitSpecified.setSelected(texTerrExits.getNumber() != 0);
            updateWhenSelected();
        } else if (ae.getActionCommand().equals(ClientGUI.VIEW_MINI_MAP)) {
            minimapW.setVisible(!minimapW.isVisible());
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
            bv.toggleIsometric();
        } else if (ae.getActionCommand().equals(ClientGUI.VIEW_CHANGE_THEME)) {
            bv.changeTheme();
        } else if (ae.getSource().equals(choTheme) ) { 
            curHex.setTheme((String)choTheme.getSelectedItem());
            repaintWorkingHex();
        } else if (ae.getSource().equals(buttonLW)) {
            setConvenientTerrain(ae, TF.createTerrain(Terrains.WOODS, 1), TF.createTerrain(Terrains.FOLIAGE_ELEV, 2));
        } else if (ae.getSource().equals(buttonOW)) {
            setConvenientTerrain(ae, TF.createTerrain(Terrains.WOODS, 1), TF.createTerrain(Terrains.FOLIAGE_ELEV, 1));
        } else if (ae.getSource().equals(buttonMg)) {
            setConvenientTerrain(ae, TF.createTerrain(Terrains.MAGMA, 1));
        } else if (ae.getSource().equals(buttonLJ)) {
            setConvenientTerrain(ae, TF.createTerrain(Terrains.JUNGLE, 1), TF.createTerrain(Terrains.FOLIAGE_ELEV, 2));
        } else if (ae.getSource().equals(buttonOJ)) {
            setConvenientTerrain(ae, TF.createTerrain(Terrains.JUNGLE, 1), TF.createTerrain(Terrains.FOLIAGE_ELEV, 1));
        } else if (ae.getSource().equals(buttonWa)) {
            buttonUpDn.setSelected(false);
            if ((ae.getModifiers() & ActionEvent.CTRL_MASK) != 0) {
                addSetTerrainEasy(Terrains.RAPIDS, curHex.containsTerrain(Terrains.RAPIDS, 1) ? 2 : 1);
                if (!curHex.containsTerrain(Terrains.WATER)
                        || (curHex.getTerrain(Terrains.WATER).getLevel() == 0)) {
                    addSetTerrainEasy(Terrains.WATER, 1);
                }
            } else {
                if ((ae.getModifiers() & ActionEvent.SHIFT_MASK) == 0) {
                    curHex.removeAllTerrains();
                }
                addSetTerrainEasy(Terrains.WATER, 1);
            }
        } else if (ae.getSource().equals(buttonSw)) {
            setConvenientTerrain(ae, TF.createTerrain(Terrains.SWAMP, 1));
        } else if (ae.getSource().equals(buttonRo)) {
            setConvenientTerrain(ae, TF.createTerrain(Terrains.ROUGH, 1));
        } else if (ae.getSource().equals(buttonPv)) {
            setConvenientTerrain(ae, TF.createTerrain(Terrains.PAVEMENT, 1));
        } else if (ae.getSource().equals(buttonMd)) {
            setConvenientTerrain(ae, TF.createTerrain(Terrains.MUD, 1));
        } else if (ae.getSource().equals(buttonTu)) {
            setConvenientTerrain(ae, TF.createTerrain(Terrains.TUNDRA, 1));
        } else if (ae.getSource().equals(buttonIc)) {
            setConvenientTerrain(ae, TF.createTerrain(Terrains.ICE, 1));
        } else if (ae.getSource().equals(buttonSn)) {
            setConvenientTerrain(ae, TF.createTerrain(Terrains.SNOW, 1));
        } else if (ae.getSource().equals(buttonCl)) {
            curHex.removeAllTerrains();
            buttonUpDn.setSelected(false);
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
            buttonUpDn.setSelected(false);
            if ((ae.getModifiers() & ActionEvent.SHIFT_MASK) == 0) {
                curHex.removeAllTerrains();
            }
            setBasicBuilding((ae.getModifiers() & ActionEvent.ALT_MASK) != 0);
        } else if (ae.getSource().equals(buttonBr)) {
            if ((ae.getModifiers() & ActionEvent.SHIFT_MASK) == 0) {
                curHex.removeAllTerrains();
            }
            buttonUpDn.setSelected(false);
            setBasicBridge();
        } else if (ae.getSource().equals(buttonFT)) {
            if ((ae.getModifiers() & ActionEvent.SHIFT_MASK) == 0) {
                curHex.removeAllTerrains();
            }
            buttonUpDn.setSelected(false);
            setBasicFuelTank();
        } else if (ae.getSource().equals(buttonRd)) {
            if ((ae.getModifiers() & ActionEvent.SHIFT_MASK) == 0) {
                curHex.removeAllTerrains();
            }
            buttonUpDn.setSelected(false);
            curHex.addTerrain(TF.createTerrain(Terrains.ROAD, 1));
            refreshTerrainList();
            repaintWorkingHex();
        } else if (ae.getSource().equals(buttonUpDn)) {
            // Not so useful to only do on clear terrain
            buttonOOC.setSelected(false);
        } else if (ae.getSource().equals(buttonUndo)) {
            // The button should not be active when the stack is empty, but
            // let's check nevertheless
            if (undoStack.isEmpty()) { 
                buttonUndo.setEnabled(false);
            } else {
                HashSet<IHex> recentHexes = undoStack.pop();
                HashSet<IHex> redoHexes = new HashSet<>(); 
                for (IHex hex: recentHexes) {
                    // Retrieve the board hex for Redo
                    IHex rHex = board.getHex(hex.getCoords()).duplicate();
                    rHex.setCoords(hex.getCoords());
                    redoHexes.add(rHex);
                    // and undo the board hex
                    board.setHex(hex.getCoords(), hex);
                }
                redoStack.push(redoHexes);
                if (undoStack.isEmpty()) {
                    buttonUndo.setEnabled(false);
                }
                hasChanges = !canReturnToSaved | (undoStack.size() != savedUndoStackSize);
                buttonRedo.setEnabled(true);
                currentUndoSet = null; // should be anyway
            }
            setFrameTitle();
        } else if (ae.getSource().equals(buttonRedo)) {
            // The button should not be active when the stack is empty, but
            // let's check nevertheless
            if (redoStack.isEmpty()) { 
                buttonRedo.setEnabled(false); 
            } else {
                HashSet<IHex> recentHexes = redoStack.pop();
                HashSet<IHex> undoHexes = new HashSet<>(); 
                for (IHex hex: recentHexes) {
                    IHex rHex = board.getHex(hex.getCoords()).duplicate();
                    rHex.setCoords(hex.getCoords());
                    undoHexes.add(rHex);
                    board.setHex(hex.getCoords(), hex);
                }
                undoStack.push(undoHexes);
                if (redoStack.isEmpty()) {
                    buttonRedo.setEnabled(false);
                }
                buttonUndo.setEnabled(true);
                hasChanges = !canReturnToSaved | (undoStack.size() != savedUndoStackSize);
                currentUndoSet = null; // should be anyway
            }
            setFrameTitle();
        }
    }
    
    private void setConvenientTerrain(ActionEvent event, ITerrain... terrains) {
        if ((event.getModifiers() & ActionEvent.SHIFT_MASK) == 0) {
            curHex.removeAllTerrains();
        }
        buttonUpDn.setSelected(false);
        for (var terrain: terrains) {
            curHex.addTerrain(terrain);
        }
        refreshTerrainList();
        repaintWorkingHex();
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
    
    @Override
    public void preferenceChange(PreferenceChangeEvent e) {
        if (e.getName().equals(GUIPreferences.GUI_SCALE)) {
            adaptToGUIScale();
        } 
    }
    
    /** Adapts the whole Board Editor UI to the current guiScale. */
    private void adaptToGUIScale() {
        Font scaledFont = new Font("Dialog", Font.PLAIN, UIUtil.scaleForGUI(UIUtil.FONT_SCALE1));

        butAddTerrain.setFont(scaledFont);
        butBoardNew.setFont(scaledFont);
        butBoardOpen.setFont(scaledFont);
        butBoardSave.setFont(scaledFont);
        butBoardSaveAs.setFont(scaledFont);
        butBoardSaveAsImage.setFont(scaledFont);
        butBoardValidate.setFont(scaledFont);
        butMiniMap.setFont(scaledFont);
        butExpandMap.setFont(scaledFont);
        butSourceFile.setFont(scaledFont);
        
        choTerrainType.setFont(scaledFont);
        choTheme.setFont(scaledFont);
        cheRoadsAutoExit.setFont(scaledFont);
        cheTerrExitSpecified.setFont(scaledFont);
        lisTerrain.setFont(scaledFont);
        texTerrExits.setFont(scaledFont);
        texElev.setFont(scaledFont);
        texTerrainLevel.setFont(scaledFont);
        
        labHelp1.setFont(scaledFont);
        labHelp2.setFont(scaledFont);
        labTheme.setFont(scaledFont);
        
        ((TitledBorder) panelBoardSettings.getBorder()).setTitleFont(scaledFont);
        ((TitledBorder) panelHexSettings.getBorder()).setTitleFont(scaledFont);
        ((TitledBorder) panelTerrSettings.getBorder()).setTitleFont(scaledFont);
        
        terrainButtons.stream().forEach(ScalingIconButton::rescale);
        undoButtons.stream().forEach(ScalingIconButton::rescale);
        brushButtons.stream().forEach(ScalingIconToggleButton::rescale);
        butTerrDown.rescale();
        butTerrUp.rescale();
        butElevDown.rescale();
        butElevUp.rescale();
        butExitDown.rescale();
        butExitUp.rescale();
        butTerrExits.rescale();
        butDelTerrain.rescale();
    }

    /**
     * Displays the currently selected hex picture, in component form
     */
    private class HexCanvas extends JPanel {
        private static final long serialVersionUID = 3201928357525361191L;

        HexCanvas() {
            setPreferredSize(new Dimension(90, 90));
        }
        
        /** Returns list or an empty list when list is null. */
        private List<Image> safeList(List<Image> list) {
            return list == null ? Collections.emptyList() : list;
        }

        @Override
        public void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (curHex != null) {
                // draw the terrain images
                TilesetManager tm = bv.getTilesetManager();
                g.drawImage(tm.baseFor(curHex), 0, 0, BoardView1.HEX_W, BoardView1.HEX_H, this);
                for (final Image newVar : safeList(tm.supersFor(curHex))) {
                    g.drawImage(newVar, 0, 0, this);
                }
                for (final Image newVar : safeList(tm.orthoFor(curHex))) {
                    g.drawImage(newVar, 0, 0, this);
                }
                // add level and INVALID if necessary
                if (guip.getAntiAliasing()) {
                    ((Graphics2D) g).setRenderingHint(
                            RenderingHints.KEY_ANTIALIASING,
                            RenderingHints.VALUE_ANTIALIAS_ON);
                }
                g.setColor(getForeground());
                g.setFont(new Font("SansSerif", Font.PLAIN, 9));
                g.drawString(Messages.getString("BoardEditor.LEVEL") + curHex.getLevel(), 24, 70);
                StringBuffer errBuf = new StringBuffer();
                if (!curHex.isValid(errBuf)) {
                    g.setFont(new Font("SansSerif", Font.BOLD, 14));
                    Point hexCenter = new Point(BoardView1.HEX_W / 2, BoardView1.HEX_H / 2);
                    BoardView1.drawCenteredText((Graphics2D) g, Messages.getString("BoardEditor.INVALID"),
                            hexCenter, guip.getWarningColor(), false);
                    String tooltip = Messages.getString("BoardEditor.invalidHex") + errBuf;
                    tooltip = tooltip.replace("\n", "<br>");
                    setToolTipText(tooltip);
                } else {
                    setToolTipText(null);
                }
            } else {
                g.clearRect(0, 0, 72, 72);
            }
        }

        // Make the hex stubborn when resizing the frame
        @Override
        public Dimension getPreferredSize() {
            return new Dimension(90, 90);
        }
        
        @Override
        public Dimension getMinimumSize() {
            return new Dimension(90, 90);
        }
    }

    /**
     * @return the frame this is displayed in
     */
    public JFrame getFrame() {
        return frame;
    }

    /**
     * Returns true if a dialog is visible on top of the <code>ClientGUI</code>.
     * For example, the <code>MegaMekController</code> should ignore hotkeys
     * if there is a dialog, like the <code>CommonSettingsDialog</code>, open.
     *
     * @return whether hot keys should be ignored or not
     */
    public boolean shouldIgnoreHotKeys() {
        return ignoreHotKeys
                || ((about != null) && about.isVisible())
                || ((help != null) && help.isVisible())
                || ((setdlg != null) && setdlg.isVisible())
                || texElev.hasFocus() || texTerrainLevel.hasFocus() || texTerrExits.hasFocus();
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
     *  Sets the Board Editor frame title, adding the current file name if any
     *  and a "*" if the board has unsaved changes.
     */
    private void setFrameTitle() {
        String title = (curBoardFile == null) ? Messages.getString("BoardEditor.title")
                : Messages.getString("BoardEditor.title0", curBoardFile);
        frame.setTitle(title + (hasChanges ? "*" : ""));
    }
    
    
    /**
     * Specialized field for the BoardEditor that supports 
     * MouseWheel changes.
     * 
     * @author Simon
     */
    private class EditorTextField extends JTextField {
        private static final long serialVersionUID = 1137300303131688344L;
        
        private int minValue = Integer.MIN_VALUE;
        
        /**
         * Creates an EditorTextField based on JTextField. This is a 
         * specialized field for the BoardEditor that supports 
         * MouseWheel changes.
         * 
         * @param text the initial text
         * @param columns as in JTextField
         * 
         * @see javax.swing.JTextField#JTextField(String, int)
         */
        public EditorTextField(String text, int columns) {
            super(text, columns);
            // Automatically select all text when clicking the text field
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseReleased(MouseEvent e) {
                    selectAll();
                }
            });
            addMouseWheelListener(e -> {
                if (e.getWheelRotation() < 0) {
                    incValue();
                } else {
                    decValue();
                }
            });
            setMargin(new Insets(1,1,1,1));
            setHorizontalAlignment(JTextField.CENTER);
            setFont(fontElev);
            setCursor(Cursor.getDefaultCursor());
        }
        
        /**
         * Creates an EditorTextField based on JTextField. This is a 
         * specialized field for the BoardEditor that supports 
         * MouseWheel changes.
         * 
         * @param text the initial text
         * @param columns as in JTextField
         * @param minimum a minimum value that the EditorTextField
         * will generally adhere to when its own methods are used
         * to change its value.
         * 
         * @see javax.swing.JTextField#JTextField(String, int)
         * 
         * @author Simon/Juliez
         */
        public EditorTextField(String text, int columns, int minimum) {
            this(text, columns);
            minValue = minimum;
        }
        
        /**
         * Increases the EditorTextField's number by one, if a number
         * is present.
         */
        public void incValue() {
            int newValue = getNumber() + 1;
            setNumber(newValue);
        }

        /**
         * Lowers the EditorTextField's number by one, if a number
         * is present and if that number is higher than the minimum
         * value.
         */
        public void decValue() {
            setNumber(getNumber() - 1);
        }

        /**
         * Sets the text to <code>newValue</code>. If <code>newValue</code> is lower
         * than the EditorTextField's minimum value, the minimum value will
         * be set instead.
         * 
         * @param newValue the value to be set
         */
        public void setNumber(int newValue) {
            int value = Math.max(newValue, minValue);
            setText(Integer.toString(value));
        }
        
        /**
         * Returns the text in the EditorTextField's as an int. 
         * Returns 0 when no parsable number (only letters) are present. 
         */
        public int getNumber() {
            try {
                return Integer.parseInt(getText());
            } catch (NumberFormatException ex) {
                return 0;
            }
        }
    }
    
    /** 
     * A specialized JButton that only shows an icon but scales that icon according
     * to the current GUI scaling when its rescale() method is called.
     */
    private class ScalingIconButton extends JButton {
        private static final long serialVersionUID = 4351623243707863737L;
        
        private Image baseImage;
        private Image baseRolloverImage;
        private Image baseDisabledImage;
        private int baseWidth;
        
        ScalingIconButton(Image image, int width) {
            super();
            Objects.requireNonNull(image);
            baseImage = image;
            baseWidth = width;
            rescale();
        }
        
        /** Adapts all images of this button to the current gui scale. */
        void rescale() {
            int realWidth = UIUtil.scaleForGUI(baseWidth);
            int realHeight = baseImage.getHeight(null) * realWidth / baseImage.getWidth(null);
            setIcon(new ImageIcon(ImageUtil.getScaledImage(baseImage, realWidth, realHeight)));
            
            if (baseRolloverImage != null) {
                realHeight = baseRolloverImage.getHeight(null) * realWidth / baseRolloverImage.getWidth(null);
                setRolloverIcon(new ImageIcon(ImageUtil.getScaledImage(baseRolloverImage, realWidth, realHeight)));
            } else {
                setRolloverIcon(null);
            }
                
            
            if (baseDisabledImage != null) {
                realHeight = baseDisabledImage.getHeight(null) * realWidth / baseDisabledImage.getWidth(null);
                setDisabledIcon(new ImageIcon(ImageUtil.getScaledImage(baseDisabledImage, realWidth, realHeight)));
            } else {
                setDisabledIcon(null);
            }
        }
        
        /** 
         * Sets the unscaled base image to use as a mouse hover image for the button. 
         * image may be null. Passing null disables the hover image. 
         */
        void setRolloverImage(@Nullable Image image) {
            baseRolloverImage = image;
        }
        
        /** 
         * Sets the unscaled base image to use as a button disabled image for the button. 
         * image may be null. Passing null disables the button disabled image. 
         */
        void setDisabledImage(@Nullable Image image) {
            baseDisabledImage = image;
        }
    }
    
    /** 
     * A specialized JToggleButton that only shows an icon but scales that icon according
     * to the current GUI scaling when its rescale() method is called.
     */
    private class ScalingIconToggleButton extends JToggleButton {
        private static final long serialVersionUID = 4351623243707863737L;
        
        private Image baseImage;
        private Image baseRolloverImage;
        private Image baseSelectedImage;
        private int baseWidth;
        
        ScalingIconToggleButton(Image image, int width) {
            super();
            Objects.requireNonNull(image);
            baseImage = image;
            baseWidth = width;
            rescale();
        }
        
        /** Adapts all images of this button to the current gui scale. */ 
        void rescale() {
            int realWidth = UIUtil.scaleForGUI(baseWidth);
            int realHeight = baseImage.getHeight(null) * realWidth / baseImage.getWidth(null);
            setIcon(new ImageIcon(ImageUtil.getScaledImage(baseImage, realWidth, realHeight)));
            
            if (baseRolloverImage != null) {
                realHeight = baseRolloverImage.getHeight(null) * realWidth / baseRolloverImage.getWidth(null);
                setRolloverIcon(new ImageIcon(ImageUtil.getScaledImage(baseRolloverImage, realWidth, realHeight)));
            } else {
                setRolloverIcon(null);
            }
            
            if (baseSelectedImage != null) {
                realHeight = baseSelectedImage.getHeight(null) * realWidth / baseSelectedImage.getWidth(null);
                setSelectedIcon(new ImageIcon(ImageUtil.getScaledImage(baseSelectedImage, realWidth, realHeight)));
            } else {
                setSelectedIcon(null);
            }
        }
        
        /** 
         * Sets the unscaled base image to use as a mouse hover image for the button. 
         * image may be null. Passing null disables the hover image. 
         */
        void setRolloverImage(@Nullable Image image) {
            baseRolloverImage = image;
        }
        
        /** 
         * Sets the unscaled base image to use as a "toggle button is selected" image for the button. 
         * image may be null. Passing null disables the "is selected" image. 
         */
        void setSelectedImage(@Nullable Image image) {
            baseSelectedImage = image;
        }
    }
}
