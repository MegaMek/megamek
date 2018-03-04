/*
 * MegaMek - Copyright (C) 2000-2003 Ben Mazur (bmazur@sev.org)
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License as published by the Free
 *  Software Foundation; either version 2 of the License, or (at your option)
 *  any later version.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *  or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 *  for more details.
 */
package megamek.client.ui.swing;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Point;
import java.awt.SystemColor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
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
import java.util.Set;
import java.util.Stack;

import javax.imageio.ImageIO;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.filechooser.FileFilter;

import megamek.client.event.BoardViewEvent;
import megamek.client.event.BoardViewListenerAdapter;
import megamek.client.ui.Messages;
import megamek.client.ui.swing.boardview.BoardView1;
import megamek.client.ui.swing.util.MegaMekController;
import megamek.common.Configuration;
import megamek.common.Coords;
import megamek.common.Game;
import megamek.common.Hex;
import megamek.common.IBoard;
import megamek.common.IGame;
import megamek.common.IHex;
import megamek.common.ITerrain;
import megamek.common.ITerrainFactory;
import megamek.common.MapSettings;
import megamek.common.Terrains;
import megamek.common.util.BoardUtilities;
import megamek.common.util.ImageUtil;
import megamek.common.util.MegaMekFile;


// TODO: center map
// TODO: background on the whole screen
// TODO: vertical size of editor pane :(
// TODO: restrict terrains to those with images
// TODO: Allow drawing of invalid terrain?

public class BoardEditor extends JComponent
        implements ItemListener, ListSelectionListener, ActionListener, DocumentListener, IMapSettingsObserver {
    
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
                return getTerrainType() == ((Integer)other).intValue();
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
        /**
         * 
         */
        private static final long serialVersionUID = 7428395938750335593L;

        TerrainHelper[] terrains;
        
        List<TerrainTypeHelper> terrainTypes;

        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
                boolean cellHasFocus) {

            JComponent comp = (JComponent) super.getListCellRendererComponent(list, value, index, isSelected,
                    cellHasFocus);

            if (-1 < index && null != value && null != terrains) {
                list.setToolTipText(terrains[index].getTerrainTooltip());
            }
            if (-1 < index && null != value && null != terrainTypes) {
                list.setToolTipText(terrainTypes.get(index).getTooltip());
            }
            return comp;
        }

        public void setTerrains(TerrainHelper[] terrains) {
            this.terrains = terrains;
        }
        
        public void setTerrainTypess(List<TerrainTypeHelper> terrainTypes) {
            this.terrainTypes = terrainTypes;
        }
    }
 
    private static final long serialVersionUID = 4689863639249616192L;
    JFrame frame = new JFrame();
    private Game game = new Game();
    IBoard board = game.getBoard();
    BoardView1 bv;
    public static final int [] allDirections = {0,1,2,3,4,5};
    boolean isDragging = false;
    private Component bvc;
    private CommonMenuBar menuBar = new CommonMenuBar();
    private CommonAboutDialog about;
    private CommonHelpDialog help;
    private CommonSettingsDialog setdlg;
    private ITerrainFactory TF = Terrains.getTerrainFactory();
    private JDialog minimapW;
    private MiniMap minimap;
    MegaMekController controller;
    IHex curHex = new Hex();
    private File curfileImage;
    private File curfile;
    // buttons and labels and such:
    private HexCanvas canHex;
    // Easy terrain access buttons
    private JButton buttonLW, buttonLJ;
    private JButton buttonWa, buttonSw, buttonRo;
    private JButton buttonRd, buttonCl, buttonBu;
    private JButton buttonMd, buttonPv, buttonSn;
    private JButton buttonIc, buttonTu, buttonMg;
    private JButton buttonBr, buttonFT;
    private JToggleButton buttonBrush1, buttonBrush2, buttonBrush3;
    private JToggleButton buttonUpDn, buttonOOC;
    // the brush size: 1 = 1 hex, 2 = radius 1, 3 = radius 2  
    int brushSize = 1;
    int hexLeveltoDraw = -1000;
    private Font fontElev = new Font("SansSerif", Font.BOLD, 20); //$NON-NLS-1$
    private EditorTextField texElev;
    private JButton butElevUp;
    private JButton butElevDown;
    private JList<TerrainTypeHelper> lisTerrain;
    private ComboboxToolTipRenderer lisTerrainRenderer;
    private JButton butDelTerrain;
    private JComboBox<TerrainHelper> choTerrainType;
    private EditorTextField texTerrainLevel;
    private JCheckBox cheTerrExitSpecified;
    private EditorTextField texTerrExits;
    private JButton butTerrExits;
    private JCheckBox cheRoadsAutoExit;
    private JButton butExitUp, butExitDown;
    private JComboBox<String> choTheme;
    private JButton butTerrDown, butTerrUp;
    private JButton butAddTerrain;
    private JButton butBoardNew;
    private JButton butBoardLoad;
    private JButton butBoardSave;
    private JButton butBoardSaveAs;
    private JButton butBoardSaveAsImage;
    private JButton butMiniMap;
    private JButton butBoardValidate;
    private MapSettings mapSettings = MapSettings.getInstance();
    private JButton butExpandMap;
    private Coords lastClicked;
    // Undo / Redo
    JButton buttonUndo, buttonRedo;
    private Stack<HashSet<IHex>> undoStack = new Stack<>();
    private Stack<HashSet<IHex>> redoStack = new Stack<>();
    private HashSet<IHex> currentUndoSet;
    private HashSet<Coords> currentUndoCoords;
    
    /**
     * Special purpose indicator, keeps terrain list 
     * from de-selecting when clicking it
     */
    private boolean terrListBlocker = false;
    
    
    /**
     * A MouseAdapter that closes a JLabel when clicked 
     */
    private MouseAdapter clickToHide = new MouseAdapter() {
        @Override
        public void mouseReleased(MouseEvent e) {
            if (e.getSource() instanceof JLabel)
                ((JLabel) e.getSource()).setVisible(false);
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
            JOptionPane
                    .showMessageDialog(
                            frame,
                            Messages.getString("BoardEditor.CouldntInitialize") + e,
                            Messages.getString("BoardEditor.FatalError"), JOptionPane.ERROR_MESSAGE); //$NON-NLS-1$
            //$NON-NLS-2$
            frame.dispose();
        }
        // Add a mouse listener for mouse button release 
        // to handle Undo
        bv.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON1) {
                    // Act only if the user actually drew something
                    if ((currentUndoSet != null) &&
                            !currentUndoSet.isEmpty()) {
                        // Since this draw action is finished, push the
                        // drawn hexes onto the Undo Stack and get ready
                        // for a new draw action
                        undoStack.push(currentUndoSet);
                        currentUndoSet = null;
                        buttonUndo.setEnabled(true);
                        // Drawing something disables any redo actions
                        redoStack.clear();
                        buttonRedo.setEnabled(false);
                    }
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
                boolean isALT = (b.getModifiers() & InputEvent.ALT_MASK) != 0;
                boolean isSHIFT = (b.getModifiers() & InputEvent.SHIFT_MASK) != 0;
                boolean isCTRL = (b.getModifiers() & InputEvent.CTRL_MASK) != 0;
                boolean isLMB = (b.getModifiers() & InputEvent.BUTTON1_MASK) != 0;

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
                            if (isSHIFT) hexLeveltoDraw++;
                            else if (isALT) hexLeveltoDraw--;
                            isDragging = true;
                        }
                    }

                    // CORRECTION, click outside the board then drag inside???
                    if (hexLeveltoDraw != -1000) {
                        LinkedList<Coords> allBrushHexes = getBrushCoords(c) ;
                        for (Coords h: allBrushHexes) {
                            if (!buttonOOC.isSelected() ||
                                    (buttonOOC.isSelected() && board.getHex(h).isClearHex()))
                            {
                                saveToUndo(h);
                                relevelHex(h);
                            }   
                        }
                    }
                    // ------- End Raise/Lower Terrain
                } else {
                    // Normal texture paint
                    if (isALT) { // ALT-Click
                        setCurrentHex(board.getHex(b.getCoords()));
                    } else {
                        LinkedList<Coords> allBrushHexes = getBrushCoords(c);
                        for (Coords h: allBrushHexes) {
                            // test if texture overwriting is active
                            if ((!buttonOOC.isSelected() ||
                                    (buttonOOC.isSelected() && board.getHex(h).isClearHex() )  )
                                    && curHex.isValid(null))
                            {
                                saveToUndo(h);
                                if (isCTRL) { // CTRL-Click
                                    paintHex(h);
                                } else if (isSHIFT) { // SHIFT-Click
                                    addToHex(h);
                                } else if (isLMB) { // Normal click
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
            String title = Messages.getString("BoardEditor.readme.title"); //$NON-NLS-1$
            String body = Messages.getString("BoardEditor.readme.message"); //$NON-NLS-1$
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
        frame.setTitle(Messages.getString("BoardEditor.title")); //$NON-NLS-1$
        frame.getContentPane().setLayout(new BorderLayout());

        frame.getContentPane().add(bvc, BorderLayout.CENTER);
        frame.getContentPane().add(this, BorderLayout.EAST);
        menuBar.addActionListener(this);
        frame.setJMenuBar(menuBar);
        frame.setBackground(SystemColor.menu);
        frame.setForeground(SystemColor.menuText);
        if (GUIPreferences.getInstance().getWindowSizeHeight() != 0) {
            frame.setLocation(GUIPreferences.getInstance().getWindowPosX(),
                              GUIPreferences.getInstance().getWindowPosY());
            frame.setSize(GUIPreferences.getInstance().getWindowSizeWidth(),
                          GUIPreferences.getInstance().getWindowSizeHeight());
        } else {
            frame.setSize(800, 600);
        }

        // when frame is closing, just hide it
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                frame.setVisible(false);
                setMapVisible(false);
                if (controller != null) {
                    controller.removeAllActions();
                    controller.boardEditor = null;
                }
            }
        });
    }
    
    /**
     * Sets up JButtons
     */
    JButton prepareButton(String iconName, String buttonName, ArrayList<JButton> bList) {
        JButton button = new JButton(buttonName);
        button.addActionListener(this);
        // Get the normal icon
        File file = new MegaMekFile(Configuration.widgetsDir(), "/MapEditor/"+iconName+".png").getFile(); //$NON-NLS-1$ //$NON-NLS-2$
        Image imageButton = ImageUtil.loadImageFromFile(file.getAbsolutePath());
        if (imageButton != null) {
            button.setIcon(new ImageIcon(imageButton));
            // When there is an icon, then the text can be removed
            button.setText("");
        }
        // Get the hover icon
        file = new MegaMekFile(Configuration.widgetsDir(), "/MapEditor/"+iconName+"_H.png").getFile(); //$NON-NLS-1$ //$NON-NLS-2$
        imageButton = ImageUtil.loadImageFromFile(file.getAbsolutePath());
        if (imageButton != null)
            button.setRolloverIcon(new ImageIcon(imageButton));
        
        // Get the disabled icon, if any
        file = new MegaMekFile(Configuration.widgetsDir(), "/MapEditor/"+iconName+"_G.png").getFile(); //$NON-NLS-1$ //$NON-NLS-2$
        imageButton = ImageUtil.loadImageFromFile(file.getAbsolutePath());
        if (imageButton != null)
            button.setDisabledIcon(new ImageIcon(imageButton));
        
        button.setToolTipText(Messages.getString("BoardEditor."+iconName+"TT")); //$NON-NLS-1$ //$NON-NLS-2$
        button.setMargin(new Insets(0,0,0,0));
        if (bList != null) bList.add(button);
        return button;
    }
    
    /**
     * Sets up JToggleButtons
     */
    JToggleButton addTerrainTButton(String iconName, String buttonName, ArrayList<JToggleButton> bList) {
        JToggleButton button = new JToggleButton(buttonName);
        button.addActionListener(this);
        
        // Get the normal icon
        File file = new MegaMekFile(Configuration.widgetsDir(), "/MapEditor/"+iconName+".png").getFile(); //$NON-NLS-1$ //$NON-NLS-2$
        Image imageButton = ImageUtil.loadImageFromFile(file.getAbsolutePath());
        if (imageButton != null) {
            button.setIcon(new ImageIcon(imageButton));
            // When there is an icon, then the text can be removed
            button.setText("");
        }
        
        // Get the hover icon
        file = new MegaMekFile(Configuration.widgetsDir(), "/MapEditor/"+iconName+"_H.png").getFile(); //$NON-NLS-1$ //$NON-NLS-2$
        imageButton = ImageUtil.loadImageFromFile(file.getAbsolutePath());
        if (imageButton != null)
            button.setRolloverIcon(new ImageIcon(imageButton));
        
        // Get the selected icon
        file = new MegaMekFile(Configuration.widgetsDir(), "/MapEditor/"+iconName+"_S.png").getFile(); //$NON-NLS-1$ //$NON-NLS-2$
        imageButton = ImageUtil.loadImageFromFile(file.getAbsolutePath());
        if (imageButton != null)
            button.setSelectedIcon(new ImageIcon(imageButton));
        
        button.setToolTipText(Messages.getString("BoardEditor."+iconName+"TT")); //$NON-NLS-1$ //$NON-NLS-2$
        if (bList != null) bList.add(button);
        return button;
    }

    /**
     * Sets up the editor panel, which goes on the right of the map and has
     * controls for editing the current square.
     */
    private void setupEditorPanel() {
        // Help Texts
        JLabel genHelpText1 = new JLabel(Messages.getString("BoardEditor.helpText"),SwingConstants.LEFT); //$NON-NLS-1$
        JLabel terrainButtonHelp = new JLabel(Messages.getString("BoardEditor.helpText2"),SwingConstants.LEFT); //$NON-NLS-1$
        genHelpText1.addMouseListener(clickToHide);
        terrainButtonHelp.addMouseListener(clickToHide);

        // Buttons to ease setting common terrain types
        ArrayList<JButton> terrainButtons = new ArrayList<>();
        buttonLW = prepareButton("ButtonLW", "Woods", terrainButtons); //$NON-NLS-1$ //$NON-NLS-2$
        buttonLJ = prepareButton("ButtonLJ", "Jungle", terrainButtons); //$NON-NLS-1$ //$NON-NLS-2$
        buttonWa = prepareButton("ButtonWa", "Water", terrainButtons); //$NON-NLS-1$ //$NON-NLS-2$
        buttonSw = prepareButton("ButtonSw", "Swamp", terrainButtons); //$NON-NLS-1$ //$NON-NLS-2$
        buttonRo = prepareButton("ButtonRo", "Rough", terrainButtons); //$NON-NLS-1$ //$NON-NLS-2$
        buttonMd = prepareButton("ButtonMd", "Mud", terrainButtons); //$NON-NLS-1$ //$NON-NLS-2$
        buttonPv = prepareButton("ButtonPv", "Pavement", terrainButtons); //$NON-NLS-1$ //$NON-NLS-2$
        buttonBu = prepareButton("ButtonBu", "Buildings", terrainButtons); //$NON-NLS-1$ //$NON-NLS-2$
        buttonRd = prepareButton("ButtonRd", "Roads", terrainButtons); //$NON-NLS-1$ //$NON-NLS-2$
        buttonBr = prepareButton("ButtonBr", "Bridges", terrainButtons); //$NON-NLS-1$ //$NON-NLS-2$
        buttonFT = prepareButton("ButtonFT", "Fuel Tanks", terrainButtons); //$NON-NLS-1$ //$NON-NLS-2$
        buttonSn = prepareButton("ButtonSn", "Snow", terrainButtons); //$NON-NLS-1$ //$NON-NLS-2$
        buttonIc = prepareButton("ButtonIc", "Ice", terrainButtons); //$NON-NLS-1$ //$NON-NLS-2$
        buttonTu = prepareButton("ButtonTu", "Tundra", terrainButtons); //$NON-NLS-1$ //$NON-NLS-2$
        buttonMg = prepareButton("ButtonMg", "Magma", terrainButtons); //$NON-NLS-1$ //$NON-NLS-2$
        buttonCl = prepareButton("ButtonCl", "Clear", terrainButtons); //$NON-NLS-1$ //$NON-NLS-2$
        
        ArrayList<JToggleButton> brushButtons = new ArrayList<>();
        buttonBrush1 = addTerrainTButton("ButtonHex1", "Brush1", brushButtons); //$NON-NLS-1$ //$NON-NLS-2$
        buttonBrush2 = addTerrainTButton("ButtonHex7", "Brush2", brushButtons); //$NON-NLS-1$ //$NON-NLS-2$
        buttonBrush3 = addTerrainTButton("ButtonHex19", "Brush3", brushButtons); //$NON-NLS-1$ //$NON-NLS-2$
        ButtonGroup brushGroup = new ButtonGroup();
        brushGroup.add(buttonBrush1);
        brushGroup.add(buttonBrush2);
        brushGroup.add(buttonBrush3);
        buttonOOC = addTerrainTButton("ButtonOOC", "OOC", brushButtons); //$NON-NLS-1$ //$NON-NLS-2$
        buttonUpDn = addTerrainTButton("ButtonUpDn", "UpDown", brushButtons); //$NON-NLS-1$ //$NON-NLS-2$
        
        ArrayList<JButton> undoButtons = new ArrayList<JButton>();
        buttonUndo = prepareButton("ButtonUndo", "Undo", undoButtons); //$NON-NLS-1$ //$NON-NLS-2$
        buttonRedo = prepareButton("ButtonRedo", "Redo", undoButtons); //$NON-NLS-1$ //$NON-NLS-2$
        buttonUndo.setEnabled(false);
        buttonRedo.setEnabled(false);

        MouseWheelListener wheelListener = new MouseWheelListener() {
            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                int terrain = Integer.MIN_VALUE;
                if (e.getSource() == buttonRo) terrain = Terrains.ROUGH; 
                else if (e.getSource() == buttonSw) terrain = Terrains.SWAMP;
                else if (e.getSource() == buttonWa) terrain = Terrains.WATER;
                else if (e.getSource() == buttonLW) terrain = Terrains.WOODS;
                else if (e.getSource() == buttonLJ) terrain = Terrains.JUNGLE;
                else if (e.getSource() == buttonMd) terrain = Terrains.MUD;
                else if (e.getSource() == buttonPv) terrain = Terrains.PAVEMENT;
                else if (e.getSource() == buttonIc) terrain = Terrains.ICE;
                else if (e.getSource() == buttonSn) terrain = Terrains.SNOW;
                else if (e.getSource() == buttonTu) terrain = Terrains.TUNDRA;
                else if (e.getSource() == buttonMg) terrain = Terrains.MAGMA;
                
                if (terrain >= 0) {
                    IHex saveHex = curHex.duplicate();
                    // change the terrain level by wheel direction if present,
                    // or set to 1 if not present
                    if (curHex.containsTerrain(terrain)) {
                        addSetTerrainEasy(terrain, 
                                curHex.getTerrain(terrain).getLevel() +
                                ((e.getWheelRotation() < 0) ? 1 : -1));
                    } else {
                        if (!e.isShiftDown())
                            curHex.removeAllTerrains();
                        addSetTerrainEasy(terrain, 1);
                    }
                    // Reset the terrain to the former state
                    // if the new would be invalid. 
                    if (!curHex.isValid(null)) {
                        curHex = saveHex;
                        refreshTerrainList();
                        repaintWorkingHex();
                    }
                }
            }
        };

        buttonSw.addMouseWheelListener(wheelListener);
        buttonWa.addMouseWheelListener(wheelListener);
        buttonRo.addMouseWheelListener(wheelListener);
        buttonLJ.addMouseWheelListener(wheelListener);
        buttonLW.addMouseWheelListener(wheelListener);
        buttonMd.addMouseWheelListener(wheelListener);
        buttonPv.addMouseWheelListener(wheelListener);
        buttonSn.addMouseWheelListener(wheelListener);
        buttonIc.addMouseWheelListener(wheelListener);
        buttonTu.addMouseWheelListener(wheelListener);
        buttonMg.addMouseWheelListener(wheelListener);

        // Mouse wheel behaviour for the BUILDINGS button
        // This always ADDS the building because clearing all terrin except
        // buildings is too complicated. User can click the X button to clear terrain.
        buttonBu.addMouseWheelListener(new MouseWheelListener() {
            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                setBasicBuilding(false);
                int wheelDir = (e.getWheelRotation() < 0) ? 1 : -1;
                int terrainType;
                int newLevel;

                if (e.isShiftDown()) {
                    terrainType = Terrains.BLDG_CF;
                    int oldLevel = curHex.getTerrain(terrainType).getLevel();
                    newLevel = Math.max(10, oldLevel + wheelDir*10);
                } 
                else if (e.isControlDown()) {
                    terrainType = Terrains.BUILDING;
                    int oldLevel = curHex.getTerrain(terrainType).getLevel();
                    newLevel = Math.max(1, oldLevel + wheelDir);
                    newLevel = Math.min(5, oldLevel + wheelDir);
                } 
                else {
                    terrainType = Terrains.BLDG_ELEV;
                    int oldLevel = curHex.getTerrain(terrainType).getLevel();
                    newLevel = Math.max(1, oldLevel + wheelDir);
                }

                if (e.isAltDown()) {
                    curHex.addTerrain(TF.createTerrain(terrainType, newLevel, true, 0));
                } else {
                    curHex.addTerrain(TF.createTerrain(terrainType, newLevel));
                }
                refreshTerrainList();
                repaintWorkingHex();
            }
        });
        
        // Mouse wheel behaviour for the BRIDGE button
        buttonBr.addMouseWheelListener(new MouseWheelListener() {
            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                setBasicBridge();
                int wheelDir = (e.getWheelRotation() < 0) ? 1 : -1;
                int terrainType;
                int newLevel;

                if (e.isShiftDown()) {
                    terrainType = Terrains.BRIDGE_CF;
                    int oldLevel = curHex.getTerrain(terrainType).getLevel();
                    newLevel = Math.max(10, oldLevel + wheelDir*10);
                } 
                else if (e.isControlDown()) {
                    terrainType = Terrains.BRIDGE;
                    int oldLevel = curHex.getTerrain(terrainType).getLevel();
                    newLevel = Math.max(1, oldLevel + wheelDir);
                } 
                else {
                    terrainType = Terrains.BRIDGE_ELEV;
                    int oldLevel = curHex.getTerrain(terrainType).getLevel();
                    newLevel = Math.max(0, oldLevel + wheelDir);
                }
                
                curHex.addTerrain(TF.createTerrain(terrainType, newLevel));
                refreshTerrainList();
                repaintWorkingHex();
            }
        }); 
        
        // Mouse wheel behaviour for the FUELTANKS button
        buttonFT.addMouseWheelListener(new MouseWheelListener() {
            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                setBasicFuelTank();
                int wheelDir = (e.getWheelRotation() < 0) ? 1 : -1;
                int terrainType;
                int newLevel;

                if (e.isShiftDown()) {
                    terrainType = Terrains.FUEL_TANK_CF;
                    int oldLevel = curHex.getTerrain(terrainType).getLevel();
                    newLevel = Math.max(10, oldLevel + wheelDir*10);
                } 
                else if (e.isControlDown()) {
                    terrainType = Terrains.FUEL_TANK_MAGN;
                    int oldLevel = curHex.getTerrain(terrainType).getLevel();
                    newLevel = Math.max(10, oldLevel + wheelDir*10);
                } 
                else {
                    terrainType = Terrains.FUEL_TANK_ELEV;
                    int oldLevel = curHex.getTerrain(terrainType).getLevel();
                    newLevel = Math.max(1, oldLevel + wheelDir);
                }
                
                curHex.addTerrain(TF.createTerrain(terrainType, newLevel));
                refreshTerrainList();
                repaintWorkingHex();
            }
        }); 

        JPanel terrainButtonPanel = new JPanel(new GridLayout(0, 3, 2, 2));
        addManyButtons(terrainButtonPanel, terrainButtons);
        
        JPanel brushButtonPanel = new JPanel(new GridLayout(0, 3, 2, 2));
        addManyTButtons(brushButtonPanel, brushButtons);
        buttonBrush1.setSelected(true);
        
        JPanel undoButtonPanel = new JPanel(new GridLayout(1, 2, 2, 2));
        addManyButtons(undoButtonPanel, buttonUndo, buttonRedo);
        
        // Hex Elevation Control
        texElev = new EditorTextField("0", 3); //$NON-NLS-1$
        texElev.addActionListener(this);
        texElev.getDocument().addDocumentListener(this);
        butElevUp = prepareButton("ButtonExitUP", "Raise Hex Elevation", null); //$NON-NLS-1$ //$NON-NLS-2$
        butElevDown = prepareButton("ButtonExitDN", "Lower Hex Elevation", null); //$NON-NLS-1$ //$NON-NLS-2$
        
        // Terrain List
        lisTerrainRenderer = new ComboboxToolTipRenderer();
        lisTerrain = new JList<>(new DefaultListModel<>());
        lisTerrain.addListSelectionListener(this);
        lisTerrain.setCellRenderer(lisTerrainRenderer);
        lisTerrain.setVisibleRowCount(6);
        lisTerrain.setFixedCellWidth(180);
        refreshTerrainList();

        // Terrain List, Preview, Delete
        JPanel panlisHex = new JPanel(new FlowLayout(FlowLayout.LEFT,4,4));
        butDelTerrain = prepareButton("buttonRemT", "Delete Terrain", null);
        butDelTerrain.setEnabled(false);
        canHex = new HexCanvas();
        panlisHex.add(butDelTerrain);
        panlisHex.add(new JScrollPane(lisTerrain));
        panlisHex.add(canHex);
        
        // Terrain Type Chooser, Level 
        TerrainHelper[] terrains = new TerrainHelper[Terrains.SIZE - 1];
        for (int i = 1; i < Terrains.SIZE; i++) {
            terrains[i - 1] = new TerrainHelper(i);
        }
        Arrays.sort(terrains);
        texTerrainLevel = new EditorTextField("0", 2); //$NON-NLS-1$
        texTerrainLevel.addActionListener(this);
        texTerrainLevel.getDocument().addDocumentListener(this);
        choTerrainType = new JComboBox<>(terrains);
        ComboboxToolTipRenderer renderer = new ComboboxToolTipRenderer();
        renderer.setTerrains(terrains);
        choTerrainType.setRenderer(renderer);
        // Selecting a terrain type in the Dropdown should deselect
        // all in the terrain overview list except when selected from there
        choTerrainType.addActionListener(e -> { if (!terrListBlocker) lisTerrain.clearSelection(); });
        butAddTerrain = new JButton(Messages.getString("BoardEditor.butAddTerrain")); //$NON-NLS-1$
        butTerrUp = prepareButton("arrowsmallup", "Increase Terrain Level", null); //$NON-NLS-1$ //$NON-NLS-2$
        butTerrDown = prepareButton("arrowsmalldn", "Decrease Terrain Level", null); //$NON-NLS-1$ //$NON-NLS-2$
        
        // Minimap Toggle
        butMiniMap = new JButton(Messages.getString("BoardEditor.butMiniMap")); //$NON-NLS-1$
        butMiniMap.setActionCommand("viewMiniMap"); //$NON-NLS-1$

        // Exits
        cheTerrExitSpecified = new JCheckBox(Messages.getString("BoardEditor.cheTerrExitSpecified")); //$NON-NLS-1$
        cheTerrExitSpecified.addActionListener(e -> updateWhenSelected());
        butTerrExits = prepareButton("ButtonExitA", Messages.getString("BoardEditor.butTerrExits"), null); //$NON-NLS-1$ //$NON-NLS-2$
        texTerrExits = new EditorTextField("0", 2); //$NON-NLS-1$
        texTerrExits.addActionListener(this);
        texTerrExits.getDocument().addDocumentListener(this);
        butExitUp = prepareButton("arrowsmallup", "Increase Exit / Gfx", null); //$NON-NLS-1$ //$NON-NLS-2$
        butExitDown = prepareButton("arrowsmalldn", "Decrease Exit / Gfx", null); //$NON-NLS-1$ //$NON-NLS-2$

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
        cheRoadsAutoExit = new JCheckBox(Messages
                .getString("BoardEditor.cheRoadsAutoExit")); //$NON-NLS-1$
        cheRoadsAutoExit.addItemListener(this);
        cheRoadsAutoExit.setSelected(true);
        
        // Theme
        JPanel panTheme = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 4));
        JLabel labTheme = new JLabel(Messages.getString("BoardEditor.labTheme"), SwingConstants.LEFT); //$NON-NLS-1$
        choTheme = new JComboBox<String>();
        TilesetManager tileMan = bv.getTilesetManager();
        Set<String> themes = tileMan.getThemes();
        for (String s: themes) choTheme.addItem(s);
        choTheme.addActionListener(this);
        panTheme.add(labTheme);
        panTheme.add(choTheme);

        // The hex settings panel (elevation, theme)
        JPanel panelHexSettings = new JPanel();
        panelHexSettings.setBorder(new TitledBorder(new LineBorder(Color.BLUE, 1), "Hex Settings")); //$NON-NLS-1$
        panelHexSettings.add(butElevUp);
        panelHexSettings.add(texElev);
        panelHexSettings.add(butElevDown);
        panelHexSettings.add(panTheme);
        
        // The terrain settings panel (type, level, exits)
        JPanel panelTerrSettings = new JPanel(new GridLayout(0, 2, 4, 4));
        panelTerrSettings.setBorder(new TitledBorder(new LineBorder(Color.BLUE, 1), "Terrain Settings")); //$NON-NLS-1$
        panelTerrSettings.add(Box.createVerticalStrut(5));
        panelTerrSettings.add(panUP);
        
        panelTerrSettings.add(choTerrainType);
        panelTerrSettings.add(panTex);
        
        panelTerrSettings.add(butAddTerrain);
        panelTerrSettings.add(panDN);

        // The board settings panel (Auto exit roads to pavement)
        JPanel panelBoardSettings = new JPanel();
        panelBoardSettings.setBorder(new TitledBorder(new LineBorder(Color.BLUE, 1), "Board Settings")); //$NON-NLS-1$
        panelBoardSettings.add(cheRoadsAutoExit);
        
        // Board Buttons (Save, Load...)
        JLabel labBoard = new JLabel(Messages.getString("BoardEditor.labBoard"), SwingConstants.LEFT); //$NON-NLS-1$
        butBoardNew = new JButton(Messages.getString("BoardEditor.butBoardNew")); //$NON-NLS-1$
        butBoardNew.setActionCommand("fileBoardNew"); //$NON-NLS-1$

        butExpandMap = new JButton(Messages.getString("BoardEditor.butExpandMap")); //$NON-NLS-1$
        butExpandMap.setActionCommand("fileBoardExpand"); //$NON-NLS-1$

        butBoardLoad = new JButton(Messages.getString("BoardEditor.butBoardLoad")); //$NON-NLS-1$
        butBoardLoad.setActionCommand("fileBoardOpen"); //$NON-NLS-1$

        butBoardSave = new JButton(Messages.getString("BoardEditor.butBoardSave")); //$NON-NLS-1$
        butBoardSave.setActionCommand("fileBoardSave"); //$NON-NLS-1$

        butBoardSaveAs = new JButton(Messages.getString("BoardEditor.butBoardSaveAs")); //$NON-NLS-1$
        butBoardSaveAs.setActionCommand("fileBoardSaveAs"); //$NON-NLS-1$

        butBoardSaveAsImage = new JButton(Messages.getString("BoardEditor.butBoardSaveAsImage")); //$NON-NLS-1$
        butBoardSaveAsImage.setActionCommand("fileBoardSaveAsImage"); //$NON-NLS-1$

        butBoardValidate = new JButton(Messages.getString("BoardEditor.butBoardValidate")); //$NON-NLS-1$
        butBoardValidate.setActionCommand("fileBoardValidate"); //$NON-NLS-1$
        
        addManyActionListeners(butBoardValidate, butBoardSaveAsImage, butBoardSaveAs, butBoardSave);
        addManyActionListeners(butBoardLoad, butExpandMap, butBoardNew, butMiniMap); 
        addManyActionListeners(butDelTerrain, butAddTerrain);

        JPanel panButtons = new JPanel(new GridLayout(4, 2, 2, 2));
        panButtons.add(labBoard);
        panButtons.add(new JLabel("")); // Spacer Label
        panButtons.add(new JLabel("")); // Spacer Label
        addManyButtons(panButtons, butBoardNew, butBoardSave, butBoardLoad, 
                butExpandMap, butBoardSaveAs, butBoardSaveAsImage);
        panButtons.add(Box.createHorizontalStrut(5));
        panButtons.add(butBoardValidate);
        
        // ------------------
        // Arrange everything
        //
        GridBagLayout gridbag = new GridBagLayout();
        GridBagConstraints cfullLine = new GridBagConstraints();
        GridBagConstraints cYFiller = new GridBagConstraints();
        setLayout(gridbag);
        
        cfullLine.fill = GridBagConstraints.HORIZONTAL;
        cfullLine.gridwidth = GridBagConstraints.REMAINDER;
        cfullLine.gridx = 0;
        cfullLine.insets = new Insets(4, 4, 1, 1);
        
        cYFiller.fill = GridBagConstraints.HORIZONTAL;
        cYFiller.gridwidth = GridBagConstraints.REMAINDER;
        cYFiller.gridx = 0;
        cYFiller.weighty = 1;
        cYFiller.insets = new Insets(4, 4, 1, 1);
        
        // Easy Access Terrain Buttons
        add(genHelpText1, cfullLine);
        add(terrainButtonHelp, cfullLine);
        add(terrainButtonPanel, cfullLine);
        add(brushButtonPanel, cfullLine);
        add(new JLabel(""), cYFiller); //$NON-NLS-1$
        add(undoButtonPanel, cfullLine);
        add(new JLabel(""), cYFiller); //$NON-NLS-1$
        
        // Terrain and Hex Control
        add(panelBoardSettings, cfullLine);
        add(panelHexSettings, cfullLine);
        add(panelTerrSettings, cfullLine);
        
        // Terrain List and Preview Hex
        add(panlisHex, cfullLine);
        
        // Minimap Toggle
        add(new JLabel(""), cYFiller); //$NON-NLS-1$ 
        add(butMiniMap, cfullLine);
         
        // Board buttons
        add(panButtons, cfullLine);
        
        minimapW = new JDialog(frame, Messages
                .getString("BoardEditor.minimapW"), false); //$NON-NLS-1$
        minimapW.setLocation(GUIPreferences.getInstance().getMinimapPosX(),
                             GUIPreferences.getInstance().getMinimapPosY());
        try {
            minimap = new MiniMap(minimapW, game, bv);
        } catch (IOException e) {
            JOptionPane
                    .showMessageDialog(
                            frame,
                            Messages
                                    .getString("BoardEditor.CouldNotInitialiseMinimap") + e,
                            Messages.getString("BoardEditor.FatalError"), JOptionPane.ERROR_MESSAGE); //$NON-NLS-1$
            //$NON-NLS-2$
            frame.dispose();
        }
        minimapW.add(minimap);
        setMapVisible(true);
    }
    
    /**
     * Returns coords that the active brush will paint on;
     * returns only coords that are valid, i.e. on the board
     */
    private LinkedList<Coords> getBrushCoords(Coords center) {
        LinkedList<Coords> coords = new LinkedList<>();
        // The center hex itself is always part of the brush
        coords.add(center);
        // Add surrounding hexes for the big brush
        if (brushSize > 1) {
            for (int dir: allDirections) 
                coords.add(center.translated(dir));
        } 
        // Add the surrounding hexes, radius 2 for the very big brush
        if (brushSize > 2) {
            for (int dir: allDirections) {
                Coords candC = center.translated(dir, 2);
                coords.add(candC);
                coords.add(candC.translated((dir+2)%6));
            }
        } 
        // Remove coords that are not on the board
        LinkedList<Coords> finalCoords = new LinkedList<>();
        for (Coords c: coords) if (board.contains(c)) finalCoords.add(c);
        return finalCoords;
    }

    // Helper to shorten the code
    private void addManyActionListeners(JButton... buttons) {
        for (JButton button: buttons) button.addActionListener(this);
    }
    
    // Helper to shorten the code
    private void addManyButtons(JPanel panel, JButton... buttons) {
        for (JButton button: buttons) panel.add(button);
    }
    
    // Helper to shorten the code
    private void addManyButtons(JPanel panel, ArrayList<JButton> buttonList) {
        for (JButton button: buttonList) panel.add(button);
    }
    
    // Helper to shorten the code
    private void addManyTButtons(JPanel panel, ArrayList<JToggleButton> buttonList) {
        for (JToggleButton button: buttonList) panel.add(button);
    }
    
    /**
     * Save the hex at c into the current undo Set
     */
    private void saveToUndo(Coords c) {
        // Create a new set of hexes to save for undoing
        // This will be filled as long as the mouse is dragged
        if (currentUndoSet == null) {
            currentUndoSet = new HashSet<IHex>();
            currentUndoCoords = new HashSet<Coords>();
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
            int terrainTypes[] = oldHex.getTerrainTypes();
            for (int i = 0; i < terrainTypes.length; i++) {
                int terrainID = terrainTypes[i];
                if (!newHex.containsTerrain(terrainID) &&
                    oldHex.containsTerrain(terrainID)) {
                    newHex.addTerrain(oldHex.getTerrain(terrainID));
                }
            }
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
        
        ((DefaultListModel<TerrainTypeHelper>)lisTerrain.getModel()).removeAllElements();
        lisTerrainRenderer.setTerrainTypess(null);
        int terrainTypes[] = curHex.getTerrainTypes();
        List<TerrainTypeHelper> types = new ArrayList<>();
        for (int i = 0; i < terrainTypes.length; i++) {
            ITerrain terrain = curHex.getTerrain(terrainTypes[i]);
            if (terrain != null) {
                TerrainTypeHelper tth = new TerrainTypeHelper(terrain);
                types.add(tth);
            }
        }
        Collections.sort(types);
        for (TerrainTypeHelper tth : types) {
            ((DefaultListModel<TerrainTypeHelper>) lisTerrain.getModel()).addElement(tth);
        }
        lisTerrainRenderer.setTerrainTypess(types);
    }

    /**
     * Returns a new instance of the terrain that is currently entered in the
     * terrain input fields
     */
    private ITerrain enteredTerrain() {
        int type = ((TerrainHelper)choTerrainType.getSelectedItem()).getTerrainType();
        int level = Integer.parseInt(texTerrainLevel.getText());
        // For the terrain subtypes that only add to a main terrain type exits make no
        // sense at all. Therefore simply do not add them
        if ((type == Terrains.BLDG_ARMOR) || (type == Terrains.BLDG_CF) 
                || (type == Terrains.BLDG_ELEV) || (type == Terrains.BLDG_CLASS)  
                || (type == Terrains.BLDG_BASE_COLLAPSED) || (type == Terrains.BLDG_BASEMENT_TYPE)
                || (type == Terrains.BRIDGE_CF) || (type == Terrains.BRIDGE_ELEV)
                || (type == Terrains.FUEL_TANK_CF) || (type == Terrains.FUEL_TANK_ELEV)
                || (type == Terrains.FUEL_TANK_MAGN)) 
        {
            return Terrains.getTerrainFactory().createTerrain(type, level, false, 0);
        } else {
            boolean exitsSpecified = cheTerrExitSpecified.isSelected();
            int exits = Integer.parseInt(texTerrExits.getText());
            return Terrains.getTerrainFactory().createTerrain(type, level, exitsSpecified, exits);
        }
    }

    /**
     * Add or set the terrain to the list based on the fields.
     */
    private void addSetTerrain() {
        ITerrain toAdd = enteredTerrain();
        if (((toAdd.getType() == Terrains.BLDG_ELEV) 
                || (toAdd.getType() == Terrains.BRIDGE_ELEV))
                && toAdd.getLevel() < 0) {
            texTerrainLevel.setValue(0);
            JOptionPane.showMessageDialog(frame,
                    Messages.getString("BoardEditor.BridgeBuildingElevError"), //$NON-NLS-1$
                    Messages.getString("BoardEditor.invalidTerrainTitle"), //$NON-NLS-1$ 
                    JOptionPane.ERROR_MESSAGE);
            return;
        }
        curHex.addTerrain(toAdd);
        int formerSelection = lisTerrain.getSelectedIndex();
        refreshTerrainList();
        lisTerrain.setSelectedIndex(formerSelection);
        lisTerrain.ensureIndexIsVisible(formerSelection);
        repaintWorkingHex();
    }
    
    /**
     * Add to the terrain from one of the easy access buttons
     */
    private void addSetTerrainEasy(int type, int level) {
        boolean exitsSpecified = cheTerrExitSpecified.isSelected();
        int exits = Integer.parseInt(texTerrExits.getText());
        ITerrain toAdd = Terrains.getTerrainFactory().createTerrain(type, level, exitsSpecified, exits);
        curHex.addTerrain(toAdd);
        TerrainTypeHelper toSelect = new TerrainTypeHelper(toAdd);
        refreshTerrainList();
        lisTerrain.setSelectedValue(toSelect, true);
        repaintWorkingHex();
    }
    
    /**
     * Sets valid basic Fueltank values as far as they are missing
     */
    private void setBasicFuelTank() {
        // There is only fuel_tank:1, so this can be set
        curHex.addTerrain(TF.createTerrain(Terrains.FUEL_TANK, 1, true, 0));

        if (!curHex.containsTerrain(Terrains.FUEL_TANK_CF)) 
         curHex.addTerrain(TF.createTerrain(Terrains.FUEL_TANK_CF, 40, false, 0));
        
        if (!curHex.containsTerrain(Terrains.FUEL_TANK_ELEV)) 
            curHex.addTerrain(TF.createTerrain(Terrains.FUEL_TANK_ELEV, 1, false, 0));
        
        if (!curHex.containsTerrain(Terrains.FUEL_TANK_MAGN)) 
            curHex.addTerrain(TF.createTerrain(Terrains.FUEL_TANK_MAGN, 100, false, 0));
        
        refreshTerrainList();
        repaintWorkingHex();
    }
    
    /**
     * Sets valid basic bridge values as far as they are missing
     */
    private void setBasicBridge() {
        if (!curHex.containsTerrain(Terrains.BRIDGE_CF)) 
         curHex.addTerrain(TF.createTerrain(Terrains.BRIDGE_CF, 40, false, 0));
        
        if (!curHex.containsTerrain(Terrains.BRIDGE_ELEV)) 
            curHex.addTerrain(TF.createTerrain(Terrains.BRIDGE_ELEV, 1, false, 0));
        
        if (!curHex.containsTerrain(Terrains.BRIDGE)) 
            curHex.addTerrain(TF.createTerrain(Terrains.BRIDGE, 1, false, 0));
        
        refreshTerrainList();
        repaintWorkingHex();
    }
    
    /**
     * Sets valid basic bridge values as far as they are missing
     */
    private void setBasicBuilding(boolean singleHex) {
        if (!curHex.containsTerrain(Terrains.BLDG_CF)) 
            curHex.addTerrain(TF.createTerrain(Terrains.BLDG_CF, 40, false, 0));

        if (!curHex.containsTerrain(Terrains.BLDG_ELEV)) 
            curHex.addTerrain(TF.createTerrain(Terrains.BLDG_ELEV, 1, false, 0));

        if (!curHex.containsTerrain(Terrains.BUILDING))
        {
            if (singleHex) {
                curHex.addTerrain(TF.createTerrain(Terrains.BUILDING, 1, true, 0));
            } else {
                curHex.addTerrain(TF.createTerrain(Terrains.BUILDING, 1, false, 0));
            }
        }

        refreshTerrainList();
        repaintWorkingHex();
    }

    /**
     * Set all the appropriate terrain field to match the currently selected
     * terrain in the list.
     */
    private void refreshTerrainFromList() {
        if (lisTerrain.getSelectedIndex() == -1) {
            butDelTerrain.setEnabled(false);
        } else {
            butDelTerrain.setEnabled(true);
            ITerrain terrain = Terrains.getTerrainFactory().createTerrain(
                    lisTerrain.getSelectedValue().getTerrain());
            terrain = curHex.getTerrain(terrain.getType());
            TerrainHelper terrainHelper = new TerrainHelper(terrain.getType());
            terrListBlocker = true;
            choTerrainType.setSelectedItem(terrainHelper);
            terrListBlocker = false;
            texTerrainLevel.setText(Integer.toString(terrain.getLevel()));
            cheTerrExitSpecified.setSelected(terrain.hasExitsSpecified());
            texTerrExits.setValue(terrain.getExits());
        }
    }
    
    /**
     * Updates the selected terrain in the terrain list when 
     * a terrain is actually selected 
     */
    private void updateWhenSelected() {
        if (!lisTerrain.isSelectionEmpty()) 
            addSetTerrain(); 
    }

    public void boardNew() {
        RandomMapDialog rmd = new RandomMapDialog(frame, this, null, mapSettings);
        rmd.setVisible(true);
        board = BoardUtilities.generateRandom(mapSettings);
        game.setBoard(board);
        curfile = null;
        frame.setTitle(Messages.getString("BoardEditor.title")); //$NON-NLS-1$
        menuBar.setBoard(true);
        bvc.doLayout();
    }

    public void boardResize() {
        ResizeMapDialog emd = new ResizeMapDialog(frame, this, null, mapSettings);
        emd.setVisible(true);
        board = BoardUtilities.generateRandom(mapSettings);

        // Implant the old board
        int west = emd.getExpandWest();
        int north = emd.getExpandNorth();
        int east = emd.getExpandEast();
        int south = emd.getExpandSouth();
        board = implantOldBoard(game, west, north, east, south);

        game.setBoard(board);
        curfile = null;
        frame.setTitle(Messages.getString("BoardEditor.title")); //$NON-NLS-1$
        menuBar.setBoard(true);
        bvc.doLayout();
    }


    // When we resize a board, implant the old board's hexes where they should be in the new board
    public IBoard implantOldBoard(IGame game, int west, int north, int east, int south) {
        IBoard oldBoard = game.getBoard();
        for (int x = 0; x < oldBoard.getWidth(); x++) {
            for (int y = 0; y < oldBoard.getHeight(); y++) {
                int newX = x+west;
                int newY = y+north;
                if (oldBoard.contains(x, y) && board.contains(newX, newY)) {
                    IHex oldHex = oldBoard.getHex(x, y);
                    IHex hex = board.getHex(newX, newY);
                    hex.removeAllTerrains();
                        hex.setLevel(oldHex.getLevel());
                    int terrainTypes[] = oldHex.getTerrainTypes();
                    for (int i = 0; i < terrainTypes.length; i++) {
                        int terrainID = terrainTypes[i];
                        if (!hex.containsTerrain(terrainID) &&
                            oldHex.containsTerrain(terrainID)) {
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

    public void updateMapSettings(MapSettings newSettings) {
        mapSettings = newSettings;
    }

    public void boardLoad() {
        JFileChooser fc = new JFileChooser("data" + File.separator + "boards");
        fc
                .setLocation(frame.getLocation().x + 150,
                             frame.getLocation().y + 100);
        fc.setDialogTitle(Messages.getString("BoardEditor.loadBoard"));
        fc.setFileFilter(new FileFilter() {
            @Override
            public boolean accept(File dir) {
                return ((null != dir.getName())
                        && (dir.getName().endsWith(".board") || dir.isDirectory())); //$NON-NLS-1$
            }

            @Override
            public String getDescription() {
                return "*.board";
            }
        });
        int returnVal = fc.showOpenDialog(frame);
        if ((returnVal != JFileChooser.APPROVE_OPTION)
            || (fc.getSelectedFile() == null)) {
            // I want a file, y'know!
            return;
        }
        curfile = fc.getSelectedFile();
        // load!
        try (InputStream is = new FileInputStream(fc.getSelectedFile())) {            
            // tell the board to load!
            StringBuffer errBuff = new StringBuffer();
            board.load(is, errBuff, true);
            if (errBuff.length() > 0) {
                String msg = Messages.getString("BoardEditor.invalidBoard.message");
                String title =  Messages.getString("BoardEditor.invalidBoard.title");
                JOptionPane.showMessageDialog(this, msg, title, JOptionPane.ERROR_MESSAGE);
            }
            menuBar.setBoard(true);
        } catch (IOException ex) {
            System.err.println("error opening file to save!"); //$NON-NLS-1$
            System.err.println(ex);
        }
        frame.setTitle(Messages.getString("BoardEditor.title0") + curfile); //$NON-NLS-1$
        cheRoadsAutoExit.setSelected(board.getRoadsAutoExit());
        mapSettings.setBoardSize(board.getWidth(), board.getHeight());
        refreshTerrainList();
    }

    /**
     * Checks to see if there is already a path and name stored; if not, calls
     * "save as"; otherwise, saves the board to the specified file.
     */
    private void boardSave() {
        if (curfile == null) {
            boardSaveAs();
            return;
        }
        // save!
        try {
            OutputStream os = new FileOutputStream(curfile);
            // tell the board to save!
            board.save(os);
            // okay, done!
            os.close();
        } catch (IOException ex) {
            System.err.println("error opening file to save!"); //$NON-NLS-1$
            System.err.println(ex);
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
        JDialog waitD = new JDialog(frame, Messages
                .getString("BoardEditor.waitDialog.title")); //$NON-NLS-1$
        waitD.add(new JLabel(Messages
                                     .getString("BoardEditor.waitDialog.message"))); //$NON-NLS-1$
        waitD.setSize(250, 130);
        // move to middle of screen
        waitD.setLocation(
                (frame.getSize().width / 2) - (waitD.getSize().width / 2), (frame
                                                                                    .getSize().height
                                                                            / 2) - (waitD.getSize().height / 2));
        waitD.setVisible(true);
        frame.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        waitD.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        // save!
        try {
            ImageIO.write(bv.getEntireBoardImage(ignoreUnits), "png",
                    curfileImage);
        } catch (IOException e) {
            e.printStackTrace();
        }
        waitD.setVisible(false);
        frame.setCursor(Cursor.getDefaultCursor());
    }

    /**
     * Opens a file dialog box to select a file to save as; saves the board to
     * the file.
     */
    private void boardSaveAs() {
        JFileChooser fc = new JFileChooser("data" + File.separator + "boards");
        fc
                .setLocation(frame.getLocation().x + 150,
                             frame.getLocation().y + 100);
        fc.setDialogTitle(Messages.getString("BoardEditor.saveBoardAs"));
        fc.setFileFilter(new FileFilter() {
            @Override
            public boolean accept(File dir) {
                return ((null != dir.getName())
                        && (dir.getName().endsWith(".board") || dir.isDirectory())); //$NON-NLS-1$
            }

            @Override
            public String getDescription() {
                return "*.board";
            }
        });
        int returnVal = fc.showSaveDialog(frame);
        if ((returnVal != JFileChooser.APPROVE_OPTION)
            || (fc.getSelectedFile() == null)) {
            // I want a file, y'know!
            return;
        }
        curfile = fc.getSelectedFile();

        // make sure the file ends in board
        if (!curfile.getName().toLowerCase().endsWith(".board")) { //$NON-NLS-1$
            try {
                curfile = new File(curfile.getCanonicalPath() + ".board"); //$NON-NLS-1$
            } catch (IOException ie) {
                // failure!
                return;
            }
        }
        frame.setTitle(Messages.getString("BoardEditor.title0") + curfile); //$NON-NLS-1$
        boardSave();
    }

    /**
     * Opens a file dialog box to select a file to save as; saves the board to
     * the file as an image. Useful for printing boards.
     */
    private void boardSaveAsImage(boolean ignoreUnits) {
        JFileChooser fc = new JFileChooser(".");
        fc.setLocation(frame.getLocation().x + 150,
                       frame.getLocation().y + 100);
        fc.setDialogTitle(Messages.getString("BoardEditor.saveAsImage"));
        fc.setFileFilter(new FileFilter() {
            @Override
            public boolean accept(File dir) {
                return (null != dir.getName()) && (dir.getName().endsWith(".png") || dir.isDirectory()); //$NON-NLS-1$
            }

            @Override
            public String getDescription() {
                return ".png";
            }
        });
        int returnVal = fc.showSaveDialog(frame);
        if ((returnVal != JFileChooser.APPROVE_OPTION)
            || (fc.getSelectedFile() == null)) {
            // I want a file, y'know!
            return;
        }
        curfileImage = fc.getSelectedFile();

        // make sure the file ends in png
        if (!curfileImage.getName().toLowerCase().endsWith(".png")) { //$NON-NLS-1$
            try {
                curfileImage = new File(curfileImage.getCanonicalPath()
                                        + ".png"); //$NON-NLS-1$
            } catch (IOException ie) {
                // failure!
                return;
            }
        }
        frame.setTitle(Messages.getString("BoardEditor.title0") + curfileImage); //$NON-NLS-1$
        boardSaveImage(ignoreUnits);
    }

    //
    // ItemListener
    //
    public void itemStateChanged(ItemEvent ie) {
        if (ie.getSource().equals(cheRoadsAutoExit)) {
            // Set the new value for the option, and refrest the board.
            board.setRoadsAutoExit(cheRoadsAutoExit.isSelected());
            bv.updateBoard();
            repaintWorkingHex();
        }
    }

    //
    // TextListener
    //
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
        }
    }

    /**
     * Called when the user selects the "Help->About" menu item.
     */
    private void showAbout() {
        // Do we need to create the "about" dialog?
        if (about == null) {
            about = new CommonAboutDialog(frame);
        }

        // Show the about dialog.
        about.setVisible(true);
    }

    /**
     * Called when the user selects the "Help->Contents" menu item.
     */
    private void showHelp() {
        // Do we need to create the "help" dialog?
        if (help == null) {
            File helpfile = new File("docs", "editor-readme.txt"); //$NON-NLS-1$
            help = new CommonHelpDialog(frame, helpfile);
        }

        // Show the help dialog.
        help.setVisible(true);
    }

    /**
     * Called when the user selects the "View->Client Settings" menu item.
     */
    private void showSettings() {
        // Do we need to create the "settings" dialog?
        if (setdlg == null) {
            setdlg = new CommonSettingsDialog(frame);
        }

        // Show the settings dialog.
        setdlg.setVisible(true);
    }

    private void showBoardValidationReport(StringBuffer errBuff) {
        String title = Messages.getString("BoardEditor.invalidBoard.title");
        String msg = Messages.getString("BoardEditor.invalidBoard.report");
        msg += errBuff;
        JTextArea textArea = new JTextArea(msg);
        JScrollPane scrollPane = new JScrollPane(textArea);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        scrollPane.setPreferredSize(new Dimension(getWidth(), getHeight() / 2));
        JOptionPane.showMessageDialog(this, scrollPane, title, JOptionPane.ERROR_MESSAGE);
    }

    //
    // ActionListener
    //
    public void actionPerformed(ActionEvent ae) {
        if ("fileBoardNew".equalsIgnoreCase(ae.getActionCommand())) { //$NON-NLS-1$
            ignoreHotKeys = true;
            boardNew();
            ignoreHotKeys = false;
            resetUndo();
        } else if ("fileBoardExpand".equalsIgnoreCase(ae.getActionCommand())) { //$NON-NLS-1$
            ignoreHotKeys = true;
            boardResize();
            ignoreHotKeys = false;
            resetUndo();
        } else if ("fileBoardOpen".equalsIgnoreCase(ae.getActionCommand())) { //$NON-NLS-1$
            ignoreHotKeys = true;
            boardLoad();
            ignoreHotKeys = false;
            resetUndo();
        } else if ("fileBoardSave".equalsIgnoreCase(ae.getActionCommand())) { //$NON-NLS-1$
            ignoreHotKeys = true;
            boardSave();
            ignoreHotKeys = false;
        } else if ("fileBoardSaveAs".equalsIgnoreCase(ae.getActionCommand())) { //$NON-NLS-1$
            ignoreHotKeys = true;
            boardSaveAs();
            ignoreHotKeys = false;
        } else if ("fileBoardSaveAsImage".equalsIgnoreCase(ae.getActionCommand())) { //$NON-NLS-1$
            ignoreHotKeys = true;
            boardSaveAsImage(false);
            ignoreHotKeys = false;
        } else if ("fileBoardValidate".equalsIgnoreCase(ae.getActionCommand())) { //$NON-NLS-1$StringBuffer errBuff = new StringBuffer();
            StringBuffer errBuff = new StringBuffer();
            board.isValid(errBuff);
            if (errBuff.length() > 0) {
                showBoardValidationReport(errBuff);
            } else {
                String title =  Messages.getString("BoardEditor.validBoard.title");
                String msg = Messages.getString("BoardEditor.validBoard.report");
                JOptionPane.showMessageDialog(this, msg, title, JOptionPane.INFORMATION_MESSAGE);
            }
        } else if (ae.getSource().equals(butDelTerrain)
                   && (!lisTerrain.isSelectionEmpty())) {
            ITerrain toRemove = Terrains.getTerrainFactory().createTerrain(
                    lisTerrain.getSelectedValue().getTerrain());
            curHex.removeTerrain(toRemove.getType());
            refreshTerrainList();
            repaintWorkingHex();
        } else if (ae.getSource().equals(butAddTerrain)) {
            addSetTerrain();
        } else if (ae.getSource().equals(butElevUp)
                   && (curHex.getLevel() < 9)) {
            curHex.setLevel(curHex.getLevel() + 1);
            texElev.incValue();
            repaintWorkingHex();
        } else if (ae.getSource().equals(butElevDown)
                   && (curHex.getLevel() > -5)) {
            curHex.setLevel(curHex.getLevel() - 1);
            texElev.decValue();
            repaintWorkingHex();
        } else if (ae.getSource().equals(butTerrUp)) {
            texTerrainLevel.incValue();
            updateWhenSelected();
        } else if (ae.getSource().equals(butTerrDown)) {
            texTerrainLevel.decValue();
            updateWhenSelected();
        } else if (ae.getSource().equals(butTerrExits)) {
            ExitsDialog ed = new ExitsDialog(frame);
            cheTerrExitSpecified.setSelected(true);
            ed.setExits(Integer.parseInt(texTerrExits.getText()));
            ed.setVisible(true);
            texTerrExits.setValue(ed.getExits());
            updateWhenSelected();
        } else if (ae.getSource().equals(butExitUp)) {
            cheTerrExitSpecified.setSelected(true);
            texTerrExits.incValue();
            updateWhenSelected();
        } else if (ae.getSource().equals(butExitDown)) {
            cheTerrExitSpecified.setSelected(true);
            texTerrExits.decValue(0);
            updateWhenSelected();
        } else if ("viewMiniMap".equalsIgnoreCase(ae.getActionCommand())) { //$NON-NLS-1$
            toggleMap();
        } else if ("helpAbout".equalsIgnoreCase(ae.getActionCommand())) { //$NON-NLS-1$
            showAbout();
        } else if ("helpContents".equalsIgnoreCase(ae.getActionCommand())) { //$NON-NLS-1$
            showHelp();
        } else if ("viewClientSettings".equalsIgnoreCase(ae.getActionCommand())) { //$NON-NLS-1$
            showSettings();
        } else if (ae.getActionCommand().equals(ClientGUI.VIEW_TOGGLE_ISOMETRIC)) {
            bv.toggleIsometric();
        } else if (ae.getActionCommand().equals(ClientGUI.VIEW_CHANGE_THEME)) {
            bv.changeTheme();
        } else if (ae.getSource().equals(choTheme) ) { 
            curHex.setTheme((String)choTheme.getSelectedItem());
            repaintWorkingHex();
        } else if (ae.getSource().equals(buttonLW)) {
            if ((ae.getModifiers() & InputEvent.SHIFT_MASK) == 0) curHex.removeAllTerrains();  
            buttonUpDn.setSelected(false);
            addSetTerrainEasy(Terrains.WOODS, 1);
        } else if (ae.getSource().equals(buttonMg)) {
            if ((ae.getModifiers() & InputEvent.SHIFT_MASK) == 0) curHex.removeAllTerrains();
            buttonUpDn.setSelected(false);
            addSetTerrainEasy(Terrains.MAGMA, 1);
        } else if (ae.getSource().equals(buttonLJ)) {
            if ((ae.getModifiers() & InputEvent.SHIFT_MASK) == 0) curHex.removeAllTerrains();
            buttonUpDn.setSelected(false);
            addSetTerrainEasy(Terrains.JUNGLE, 1);
        } else if (ae.getSource().equals(buttonWa)) {
            buttonUpDn.setSelected(false);
            if ((ae.getModifiers() & InputEvent.CTRL_MASK) != 0) {
                if (curHex.containsTerrain(Terrains.RAPIDS, 1))
                    addSetTerrainEasy(Terrains.RAPIDS, 2);
                else
                    addSetTerrainEasy(Terrains.RAPIDS, 1);
                if (!curHex.containsTerrain(Terrains.WATER) ||
                        curHex.getTerrain(Terrains.WATER).getLevel() == 0)
                    addSetTerrainEasy(Terrains.WATER, 1);
            } else {
                if ((ae.getModifiers() & InputEvent.SHIFT_MASK) == 0) curHex.removeAllTerrains();
                addSetTerrainEasy(Terrains.WATER, 1);
            }
        } else if (ae.getSource().equals(buttonSw)) {
            if ((ae.getModifiers() & InputEvent.SHIFT_MASK) == 0) curHex.removeAllTerrains();
            buttonUpDn.setSelected(false);
            addSetTerrainEasy(Terrains.SWAMP, 1);
        } else if (ae.getSource().equals(buttonRo)) {
            if ((ae.getModifiers() & InputEvent.SHIFT_MASK) == 0) curHex.removeAllTerrains();
            buttonUpDn.setSelected(false);
            addSetTerrainEasy(Terrains.ROUGH, 1);
        } else if (ae.getSource().equals(buttonPv)) {
            if ((ae.getModifiers() & InputEvent.SHIFT_MASK) == 0) curHex.removeAllTerrains();
            buttonUpDn.setSelected(false);
            addSetTerrainEasy(Terrains.PAVEMENT, 1);
        } else if (ae.getSource().equals(buttonMd)) {
            if ((ae.getModifiers() & InputEvent.SHIFT_MASK) == 0) curHex.removeAllTerrains();
            buttonUpDn.setSelected(false);
            addSetTerrainEasy(Terrains.MUD, 1);
        } else if (ae.getSource().equals(buttonTu)) {
            if ((ae.getModifiers() & InputEvent.SHIFT_MASK) == 0) curHex.removeAllTerrains();
            buttonUpDn.setSelected(false);
            addSetTerrainEasy(Terrains.TUNDRA, 1);
        } else if (ae.getSource().equals(buttonIc)) {
            if ((ae.getModifiers() & InputEvent.SHIFT_MASK) == 0) curHex.removeAllTerrains();
            buttonUpDn.setSelected(false);
            addSetTerrainEasy(Terrains.ICE, 1);
        } else if (ae.getSource().equals(buttonSn)) {
            if ((ae.getModifiers() & InputEvent.SHIFT_MASK) == 0) curHex.removeAllTerrains();
            buttonUpDn.setSelected(false);
            addSetTerrainEasy(Terrains.SNOW, 1);
        } else if (ae.getSource().equals(buttonCl)) {
            curHex.removeAllTerrains();
            refreshTerrainList();
            repaintWorkingHex();
            buttonUpDn.setSelected(false);
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
            if ((ae.getModifiers() & InputEvent.SHIFT_MASK) == 0) curHex.removeAllTerrains();
            if ((ae.getModifiers() & InputEvent.ALT_MASK) != 0) {
                setBasicBuilding(true);
            } else {
                setBasicBuilding(false);
            }
        } else if (ae.getSource().equals(buttonBr)) {
            if ((ae.getModifiers() & InputEvent.SHIFT_MASK) == 0) curHex.removeAllTerrains();
            buttonUpDn.setSelected(false);
            setBasicBridge();
        } else if (ae.getSource().equals(buttonFT)) {
            if ((ae.getModifiers() & InputEvent.SHIFT_MASK) == 0) curHex.removeAllTerrains();
            buttonUpDn.setSelected(false);
            setBasicFuelTank();
        } else if (ae.getSource().equals(buttonRd)) {
            if ((ae.getModifiers() & InputEvent.SHIFT_MASK) == 0) curHex.removeAllTerrains();
            buttonUpDn.setSelected(false);
            addSetTerrainEasy(Terrains.ROAD, 1);
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
                if (undoStack.isEmpty()) buttonUndo.setEnabled(false);
                buttonRedo.setEnabled(true);
                currentUndoSet = null; // should be anyway
            }
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
                if (redoStack.isEmpty()) buttonRedo.setEnabled(false);
                buttonUndo.setEnabled(true);
                currentUndoSet = null; // should be anyway
            }
        }
    }

    public void insertUpdate(DocumentEvent event) {
        changedUpdate(event);
    }

    public void removeUpdate(DocumentEvent event) {
        changedUpdate(event);
    }

    public void valueChanged(ListSelectionEvent event) {
        if (event.getValueIsAdjusting()) {
            return;
        }
        if (event.getSource().equals(lisTerrain)) {
            refreshTerrainFromList();
        }
    }

    /**
     * Displays the currently selected hex picture, in component form
     */
    private class HexCanvas extends JPanel {
        /**
         *
         */
        private static final long serialVersionUID = 3201928357525361191L;

        HexCanvas() {
            setPreferredSize(new Dimension(90, 90));
        }

        @Override
        public void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (curHex != null) {
                TilesetManager tm = bv.getTilesetManager();
                g.drawImage(tm.baseFor(curHex), 0, 0, BoardView1.HEX_W, BoardView1.HEX_H, this);
                g.setColor(getForeground());
                if (tm.supersFor(curHex) != null) {
                    for (final Object newVar : tm.supersFor(curHex)) {
                        g.drawImage((Image) newVar, 0, 0, this);
                        g.drawString(
                                Messages.getString("BoardEditor.SUPER"), 0, 10); //$NON-NLS-1$
                    }
                }
                g.setFont(new Font("SansSerif", Font.PLAIN, 9)); //$NON-NLS-1$
                g.drawString(Messages.getString("BoardEditor.LEVEL") + curHex.getLevel(), 24, 70); //$NON-NLS-1$
                StringBuffer errBuf = new StringBuffer();
                if (!curHex.isValid(errBuf)) {
                    g.setFont(new Font("SansSerif", Font.BOLD, 14)); //$NON-NLS-1$
                    Point hexCenter = new Point(BoardView1.HEX_W / 2, BoardView1.HEX_H / 2);
                    bv.drawCenteredText((Graphics2D) g, Messages.getString("BoardEditor.INVALID"), hexCenter, Color.RED,
                            false);
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
            return new Dimension(80,80);
        }
        
        @Override
        public Dimension getMinimumSize() {
            return new Dimension(80,80);
        }
    }

    /**
     * @return the frame this is displayed in
     */
    public JFrame getFrame() {
        return frame;
    }

    /**
     * Toggles the minimap window Also, toggles the minimap enabled setting
     */
    private void toggleMap() {
        setMapVisible(!minimapW.isVisible());
    }

    void setMapVisible(boolean visible) {
        minimapW.setVisible(visible);
    }

    /**
     * Returns true if a dialog is visible on top of the <code>ClientGUI</code>.
     * For example, the <code>MegaMekController</code> should ignore hotkeys
     * if there is a dialog, like the <code>CommonSettingsDialog</code>, open.
     *
     * @return
     */
    public boolean shouldIgnoreHotKeys() {
        return ignoreHotKeys || (about != null && about.isVisible())
                || (help != null && help.isVisible())
                || (setdlg != null && setdlg.isVisible()) || texElev.hasFocus()
                || texTerrainLevel.hasFocus() || texTerrExits.hasFocus();
    }
    
    /**
     * Small Text Field Template with a standard format
     * <P>
     * @author Simon
     */
    private class EditorTextField extends JTextField {

        /**
         * 
         */
        private static final long serialVersionUID = 4706926692515844105L;

        /**
         * @see javax.swing.JTextField#JTextField(String, int)
         */
        public EditorTextField(String text, int columns) {
            super(text, columns);
            // Automatically select all text when clicking the text field
            addMouseListener(new MouseAdapter() {
                @Override public void mouseReleased(MouseEvent e) {
                    selectAll();
                }
            });
            setMargin(new Insets(1,1,1,1));
            setHorizontalAlignment(JTextField.CENTER);
            setFont(fontElev);
        }
        
//        public void incValue(int maximum) {
//            int newValue = Integer.parseInt(getText());
//            newValue = newValue + 1 > maximum ? newValue : newValue + 1;   
//            setValue(newValue);
//        }
        
        public void incValue() {
            int newValue = Integer.parseInt(getText()) + 1;
            setValue(newValue);
        }
        
        public void decValue(int minimum) {
            int newValue = Integer.parseInt(getText());
            newValue = newValue - 1 < minimum ? newValue : newValue - 1;   
            setValue(newValue);
        }
        
        public void decValue() {
            int newValue = Integer.parseInt(getText()) - 1;
            setValue(newValue);
        }
        
        public void setValue(int newValue) {
            setText(Integer.toString(newValue));
        }
    }
    
    
   

}
