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

import javax.imageio.ImageIO;
import javax.swing.Box;
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
import megamek.common.MapSettings;
import megamek.common.Terrains;
import megamek.common.util.BoardUtilities;
import megamek.common.util.ImageUtil;
import megamek.common.util.MegaMekFile;

public class BoardEditor extends JComponent implements ItemListener,
                                                       ListSelectionListener, ActionListener, DocumentListener,
                                                       IMapSettingsObserver {
    
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

    }
    

    private static class ComboboxToolTipRenderer extends DefaultListCellRenderer {
        /**
         * 
         */
        private static final long serialVersionUID = 7428395938750335593L;

        TerrainHelper[] terrains;

        @SuppressWarnings("rawtypes")
        @Override
        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected,
                boolean cellHasFocus) {

            JComponent comp = (JComponent) super.getListCellRendererComponent(list, value, index, isSelected,
                    cellHasFocus);

            if (-1 < index && null != value && null != terrains) {
                list.setToolTipText(terrains[index].getTerrainTooltip());
            }
            return comp;
        }

        public void setTerrains(TerrainHelper[] terrains) {
            this.terrains = terrains;
        }
    }

    /**
     *
     */
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
    IHex curHex = new Hex();
    private File curfileImage;
    private File curfile;
    // buttons and labels and such:
    private HexCanvas canHex;
    // Easy terrain access buttons
    private JPanel terrainButtonPanel;
    private JPanel brushButtonPanel;
    private ArrayList<JButton> terrainButtons;
    private ArrayList<JToggleButton> brushButtons;
    private JButton buttonLW, buttonHW, buttonUW;
    private JButton buttonLJ, buttonHJ, buttonUJ;
    private JButton buttonWa, buttonSw, buttonRo;
    private JButton buttonRd, buttonCl, buttonBu;
    private JToggleButton buttonBrush1, buttonBrush2, buttonBrush3;
    private JToggleButton buttonUpDn, buttonOOC;
    JLabel terrainButtonHelp;
    // the brush size: 1 = 1 hex, 2 = 1+6 surrounding hexes  
    int brushSize = 1;
    int hexLeveltoDraw = -1000;
    private JLabel labElev;
    private JTextField texElev;
    private JButton butElevUp;
    private JButton butElevDown;
    private JLabel labTerrain;
    private JList<String> lisTerrain;
    private JButton butDelTerrain;
    private JPanel panTerrainType;
    private JComboBox<TerrainHelper> choTerrainType;
    private JTextField texTerrainLevel;
    private JPanel panTerrExits;
    private JCheckBox cheTerrExitSpecified;
    private JTextField texTerrExits;
    private JButton butTerrExits;
    private JPanel panRoads;
    private JCheckBox cheRoadsAutoExit;
    private JLabel labTheme;
    private JTextField texTheme;
    private JButton butAddTerrain;
    private JLabel blankL;
    private JLabel labBoard;
    private JPanel panButtons;
    private JButton butBoardNew;
    private JButton butBoardLoad;
    private JButton butBoardSave;
    private JButton butBoardSaveAs;
    private JButton butBoardSaveAsImage;
    private JButton butMiniMap;
    private JButton butBoardValidate;
    private JDialog minimapW;
    private MiniMap minimap;
    private MapSettings mapSettings = MapSettings.getInstance();
    private JButton butExpandMap;

    Coords lastClicked;

    MegaMekController controller;

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
        bv.addBoardViewListener(new BoardViewListenerAdapter() {
            @Override
            public void hexMoused(BoardViewEvent b) {
                Coords c = b.getCoords();
                // return if there are no or no valid coords or if we click the same hex again
                // unless Raise/Lower Terrain is active which should let us click the same hex 
                if ((c == null) || (c.equals(lastClicked) & !buttonUpDn.isSelected())
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
                        ArrayList<Coords> allBrushHexes = getBrushCoords(c) ;
                        for (Coords h: allBrushHexes) {
                            if (!buttonOOC.isSelected() ||
                                    (buttonOOC.isSelected() && board.getHex(h).isClearHex()))
                            {
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
                        ArrayList<Coords> allBrushHexes = getBrushCoords(c) ;
                        for (Coords h: allBrushHexes) {
                            // test if texture overwriting is active
                            if (!buttonOOC.isSelected() ||
                                    (buttonOOC.isSelected() && board.getHex(h).isClearHex() )  )
                            {
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
     * Adds and sets up the easy access terrain buttons
     */
    JButton addTerrainButton(String iconName, String buttonName, String bTooltip, ArrayList<JButton> bList) {
        JButton button = new JButton(buttonName);
        button.addActionListener(this);
        // Get the normal icon
        File file = new MegaMekFile(Configuration.widgetsDir(), "/MapEditor/"+iconName+".png").getFile();
        Image imageButton = ImageUtil.loadImageFromFile(file.getAbsolutePath());
        if (imageButton != null) {
            button.setIcon(new ImageIcon(imageButton));
            // When there is an icon, then the text can be removed
            button.setText("");
        }
        // Get the hover icon
        file = new MegaMekFile(Configuration.widgetsDir(), "/MapEditor/"+iconName+"_H.png").getFile();
        imageButton = ImageUtil.loadImageFromFile(file.getAbsolutePath());
        if (imageButton != null)
            button.setRolloverIcon(new ImageIcon(imageButton));
        
        button.setToolTipText(bTooltip);
        bList.add(button);
        return button;
    }
    
    /**
     * Adds and sets up the easy access terrain buttons
     */
    JToggleButton addTerrainTButton(String iconName, String buttonName, String bTooltip, ArrayList<JToggleButton> bList) {
        JToggleButton button = new JToggleButton(buttonName);
        button.addActionListener(this);
        
        // Get the normal icon
        File file = new MegaMekFile(Configuration.widgetsDir(), "/MapEditor/"+iconName+".png").getFile();
        Image imageButton = ImageUtil.loadImageFromFile(file.getAbsolutePath());
        if (imageButton != null) {
            button.setIcon(new ImageIcon(imageButton));
            // When there is an icon, then the text can be removed
            button.setText("");
        }
        
        // Get the hover icon
        file = new MegaMekFile(Configuration.widgetsDir(), "/MapEditor/"+iconName+"_H.png").getFile();
        imageButton = ImageUtil.loadImageFromFile(file.getAbsolutePath());
        if (imageButton != null)
            button.setRolloverIcon(new ImageIcon(imageButton));
        
        // Get the selected icon
        file = new MegaMekFile(Configuration.widgetsDir(), "/MapEditor/"+iconName+"_S.png").getFile();
        imageButton = ImageUtil.loadImageFromFile(file.getAbsolutePath());
        if (imageButton != null)
            button.setSelectedIcon(new ImageIcon(imageButton));
        
        button.setToolTipText(bTooltip);
        bList.add(button);
        return button;
    }

    /**
     * Sets up the editor panel, which goes on the right of the map and has
     * controls for editing the current square.
     */
    private void setupEditorPanel() {
        canHex = new HexCanvas();

        // Buttons to ease setting common terrain types
        //
        String waTooltip = "<HTML>Water<BR>Mouse Wheel on the button to change depth</HTML>";
        String buTooltip = "<HTML>Buildings<BR>Mouse Wheel on the button to change height"
                + "<BR>CTRL-Mouse Wheel to change building type"
                + "<BR>SHIFT-Mouse Wheel to change CF</HTML>";
        String buUpDn = "<HTML>Level/Lower/Raise Terrain"
                + "<BR>Click and drag on the map to even the level"
                + "<BR>SHIFT-Click on the map to raise elevation"
                + "<BR>ALT-Click on the map to lower elevation</HTML>";
        
        terrainButtons = new ArrayList<JButton>();
        buttonLW = addTerrainButton("ButtonLW", "Light Woods", "Light Woods", terrainButtons);
        buttonHW = addTerrainButton("ButtonHW", "Heavy Woods", "Heavy Woods", terrainButtons);
        buttonUW = addTerrainButton("ButtonUW", "Ultra Woods", "Ultra Woods", terrainButtons);
        buttonLJ = addTerrainButton("ButtonLJ", "Light Jungle", "Light Jungle", terrainButtons);
        buttonHJ = addTerrainButton("ButtonHJ", "Heavy Jungle", "Heavy Jungle", terrainButtons);
        buttonUJ = addTerrainButton("ButtonUJ", "Ultra Jungle", "Ultra Jungle", terrainButtons);
        buttonWa = addTerrainButton("ButtonWa", "Water", waTooltip, terrainButtons);
        buttonSw = addTerrainButton("ButtonSw", "Swamp", "Swamp", terrainButtons);
        buttonRo = addTerrainButton("ButtonRo", "Rough", "Rough", terrainButtons);
        buttonBu = addTerrainButton("ButtonBu", "Buildings", buTooltip, terrainButtons);
        buttonRd = addTerrainButton("ButtonRd", "Roads", "Roads", terrainButtons);
        buttonCl = addTerrainButton("ButtonCl", "Clear", "Clear Terrain", terrainButtons);
        
        brushButtons = new ArrayList<JToggleButton>();
        buttonBrush1 = addTerrainTButton("ButtonHex1", "Brush1", "Small Brush", brushButtons);
        buttonBrush2 = addTerrainTButton("ButtonHex7", "Brush2", "Medium Brush", brushButtons);
        buttonBrush3 = addTerrainTButton("ButtonHex19", "Brush3", "Large Brush", brushButtons);
        buttonOOC = addTerrainTButton("ButtonOOC", "OOC", "Only overwrite clear hexes", brushButtons);
        buttonUpDn = addTerrainTButton("ButtonUpDn", "UpDown", buUpDn, brushButtons);
        
        // Mouse wheel behaviour for the WATER button to change the water depth
        buttonWa.addMouseWheelListener(new MouseWheelListener() {
            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                int dir = (e.getWheelRotation() < 0) ? 1 : -1;
                ITerrain curWater = curHex.getTerrain(Terrains.WATER);
                if (curWater == null) { // <- No water yet
                    if ((e.getModifiers() & InputEvent.SHIFT_MASK) == 0) curHex.removeAllTerrains();
                    addSetTerrainEasy(Terrains.WATER, 1);
                } else {
                    addSetTerrainEasy(Terrains.WATER, Math.max(curWater.getLevel()+dir, 0));
                }
            }
        });

        // Mouse wheel behaviour for the BUILDINGS button
        // This always ADDS the building because clearing all terrin except
        // buildings is too complicated. User can click the X button to clear terrain.
        buttonBu.addMouseWheelListener(new MouseWheelListener() {
            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                ITerrain curBldg = curHex.getTerrain(Terrains.BUILDING);
                ITerrain curCF = curHex.getTerrain(Terrains.BLDG_CF);
                ITerrain curElev = curHex.getTerrain(Terrains.BLDG_ELEV);
                int wheelDir = (e.getWheelRotation() < 0) ? 1 : -1; 

                // Set valid base building values when they're missing
                if (curBldg == null) addSetTerrainEasy(Terrains.BUILDING, 1);
                if (curCF == null) addSetTerrainEasy(Terrains.BLDG_CF, 40);
                if (curElev == null) addSetTerrainEasy(Terrains.BLDG_ELEV, 1);

                if ((e.getModifiers() & InputEvent.CTRL_MASK) != 0) { 
                    // CTRL: Change Building type
                    int newLevel = curBldg.getLevel()+wheelDir;
                    if (newLevel > 5) newLevel = 5;
                    if (newLevel < 1) newLevel = 1;
                    addSetTerrainEasy(Terrains.BUILDING, newLevel);
                } else if ((e.getModifiers() & InputEvent.SHIFT_MASK) != 0) {
                    // SHIFT: Change Building CF
                    int newLevel = curCF.getLevel()+wheelDir*10;
                    if (newLevel < 10) newLevel = 10;
                    addSetTerrainEasy(Terrains.BLDG_CF, newLevel);
                } else {
                    // Just Wheel: Change Building elevation
                    addSetTerrainEasy(Terrains.BLDG_ELEV, Math.max(curElev.getLevel()+wheelDir, 1));
                }

            }
        }); 

        terrainButtonHelp = new JLabel("<HTML><U>General Info:</U>"
                + "<BR>Click the map to paint the terrain <I>without</I> elevation"
                + "<BR>CTRL-Click the map to paint the terrain <I>with</I> elevation"
                + "<BR>SHIFT-Click the map to <I>add</I> terrain"
                + "<BR>ALT-Click the map to retrieve its terrain (eyedropper tool)"
                + "<BR><U>Info for the terrain buttons:</U>"                
                + "<BR>Click to select a terrain type"
                + "<BR>SHIFT-click to add terrain type"
                + "<BR><FONT COLOR=RED>Additional info in the button tooltips!"
                + "</FONT></FONT></HTML>", 
                SwingConstants.LEFT); //$NON-NLS-1$
        
        terrainButtonPanel = new JPanel(new GridLayout(0, 3, 2, 2));
        addManyButtons(terrainButtonPanel, terrainButtons);
        
        brushButtonPanel = new JPanel(new GridLayout(0, 3, 2, 2));
        addManyTButtons(brushButtonPanel, brushButtons);
        buttonBrush1.setSelected(true);
        buttonBrush2.setSelected(false);
        buttonBrush3.setSelected(false);
        
        labElev = new JLabel(
                Messages.getString("BoardEditor.labElev"), SwingConstants.RIGHT); //$NON-NLS-1$
        texElev = new JTextField("0", 1); //$NON-NLS-1$
        texElev.addActionListener(this);
        texElev.getDocument().addDocumentListener(this);
        butElevUp = new JButton(Messages.getString("BoardEditor.butElevUp")); //$NON-NLS-1$
        butElevDown = new JButton(Messages.getString("BoardEditor.butElevDown")); //$NON-NLS-1$
        labTerrain = new JLabel(
                Messages.getString("BoardEditor.labTerrain"), SwingConstants.LEFT); //$NON-NLS-1$
        lisTerrain = new JList<String>(new DefaultListModel<String>());
        lisTerrain.addListSelectionListener(this);
        lisTerrain.setVisibleRowCount(6);
        refreshTerrainList();
        butDelTerrain = new JButton(Messages.getString("BoardEditor.butDelTerrain")); //$NON-NLS-1$
        
        TerrainHelper[] terrains = new TerrainHelper[Terrains.SIZE - 1];
        for (int i = 1; i < Terrains.SIZE; i++) {
            terrains[i - 1] = new TerrainHelper(i);
        }
        Arrays.sort(terrains);
        
        texTerrainLevel = new JTextField("0", 1); //$NON-NLS-1$
        choTerrainType = new JComboBox<>(terrains);
        ComboboxToolTipRenderer renderer = new ComboboxToolTipRenderer();
        renderer.setTerrains(terrains);
        choTerrainType.setRenderer(renderer);
        texTerrainLevel = new JTextField("0", 1); //$NON-NLS-1$
        butAddTerrain = new JButton(Messages.getString("BoardEditor.butAddTerrain")); //$NON-NLS-1$
        butMiniMap = new JButton(Messages.getString("BoardEditor.butMiniMap")); //$NON-NLS-1$
        butMiniMap.setActionCommand("viewMiniMap"); //$NON-NLS-1$

        panTerrainType = new JPanel(new BorderLayout());
        panTerrainType.add(choTerrainType, BorderLayout.WEST);
        panTerrainType.add(texTerrainLevel, BorderLayout.CENTER);
        cheTerrExitSpecified = new JCheckBox(Messages
                                                     .getString("BoardEditor.cheTerrExitSpecified")); //$NON-NLS-1$
        butTerrExits = new JButton(Messages
                                           .getString("BoardEditor.butTerrExits")); //$NON-NLS-1$
        texTerrExits = new JTextField("0", 1); //$NON-NLS-1$
        panTerrExits = new JPanel(new FlowLayout());
        panTerrExits.add(cheTerrExitSpecified);
        panTerrExits.add(butTerrExits);
        panTerrExits.add(texTerrExits);
        panRoads = new JPanel(new FlowLayout());
        cheRoadsAutoExit = new JCheckBox(Messages
                                                 .getString("BoardEditor.cheRoadsAutoExit")); //$NON-NLS-1$
        cheRoadsAutoExit.addItemListener(this);
        panRoads.add(cheRoadsAutoExit);
        labTheme = new JLabel(Messages.getString("BoardEditor.labTheme"), SwingConstants.LEFT); //$NON-NLS-1$
        texTheme = new JTextField("", 15); //$NON-NLS-1$
        texTheme.getDocument().addDocumentListener(this);
        labBoard = new JLabel(Messages.getString("BoardEditor.labBoard"), SwingConstants.LEFT); //$NON-NLS-1$
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
        addManyActionListeners(butBoardLoad, butExpandMap, butBoardNew, butTerrExits, butMiniMap); 
        addManyActionListeners(butDelTerrain, butAddTerrain, butElevUp, butElevDown);

        panButtons = new JPanel(new GridLayout(4, 2, 2, 2));
        panButtons.add(labBoard);
        panButtons.add(new JLabel("")); // Spacer Label
        panButtons.add(new JLabel("")); // Spacer Label
        addManyButtons(panButtons, butBoardNew, butBoardSave, butBoardLoad, 
                butExpandMap, butBoardSaveAs, butBoardSaveAsImage);
        
        panButtons.add(Box.createHorizontalStrut(5));
        panButtons.add(butBoardValidate);
        blankL = new JLabel("", SwingConstants.CENTER); //$NON-NLS-1$
        GridBagLayout gridbag = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();
        setLayout(gridbag);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 0.0;
        c.insets = new Insets(4, 4, 1, 1);
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.weighty = 1.0;
        addBag(terrainButtonHelp, gridbag, c);
        addBag(terrainButtonPanel, gridbag, c);
        addBag(brushButtonPanel, gridbag, c);
        c.weighty = 0;
        c.weightx = 1;
        c.gridwidth = 1;
        addBag(labElev, gridbag, c);
        addBag(butElevUp, gridbag, c);
        addBag(butElevDown, gridbag, c);
        c.gridwidth = GridBagConstraints.REMAINDER;
        addBag(texElev, gridbag, c);
        c.weighty = 1;
        c.gridwidth = 1;
        c.gridx = 1;
        addBag(labTerrain, gridbag, c);
        c.weighty = 0;
        addBag(new JScrollPane(lisTerrain), gridbag, c);
        c.gridx = GridBagConstraints.RELATIVE;
        c.gridwidth = GridBagConstraints.REMAINDER;
        addBag(canHex, gridbag, c);
        c.weightx = 1.0;
        c.weighty = 1.0;
        addBag(butDelTerrain, gridbag, c);
        addBag(panTerrainType, gridbag, c);
        addBag(panTerrExits, gridbag, c);
        addBag(panRoads, gridbag, c);
        addBag(labTheme, gridbag, c);
        addBag(texTheme, gridbag, c);
        addBag(butAddTerrain, gridbag, c);
        addBag(butMiniMap, gridbag, c);
        c.weightx = 1.0;
        c.weighty = 1.0;
        addBag(blankL, gridbag, c);
        c.weightx = 1.0;
        c.weighty = 0.0;
        // addBag(labBoard, gridbag, c);
        addBag(panButtons, gridbag, c);
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

    private void addBag(JComponent comp, GridBagLayout gridbag,
                        GridBagConstraints c) {
        gridbag.setConstraints(comp, c);
        add(comp);
    }

    /**
     * Returns coords that the active brush will paint on;
     * returns only coords that are valid, i.e. on the board
     */
    private ArrayList<Coords> getBrushCoords(Coords center) {
        ArrayList<Coords> coords = new ArrayList<>();
        // The center hex itself is always part of the brush
        if (board.contains(center)) 
            coords.add(center);
        // Add surrounding hexes for the big brush
        if (brushSize > 1) {
            for (int dir: allDirections) {
                Coords candC = center.translated(dir);
                if (board.contains(candC)) 
                    coords.add(candC);
            }
        } 
        // Add the surrounding hexes, radius 2 for the very big brush
        if (brushSize > 2) {
            for (int dir: allDirections) {
                Coords candC = center.translated(dir).translated(dir);
                if (board.contains(candC)) 
                    coords.add(candC);
                candC = candC.translated((dir+2)%6);
                if (board.contains(candC)) 
                    coords.add(candC);
            }
        } 
        return coords;
    }
    
    /**
     * Raises hex at Coords c by one level
     */
    private void relevelHex(Coords c) {
        IHex newHex = board.getHex(c).duplicate(); 
        newHex.setLevel(hexLeveltoDraw);
        board.resetStoredElevation();
        board.setHex(c, newHex);
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
     * Sets the current hex
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
        texTheme.setText(curHex.getTheme());
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
        ((DefaultListModel<String>) lisTerrain.getModel()).removeAllElements();
        int terrainTypes[] = curHex.getTerrainTypes();
        for (int i = 0; i < terrainTypes.length; i++) {
            ITerrain terrain = curHex.getTerrain(terrainTypes[i]);
            if (terrain != null) {
                ((DefaultListModel<String>) lisTerrain.getModel()).addElement(terrain
                                                                                      .toString());
            }
        }
    }

    /**
     * Returns a new instance of the terrain that is currently entered in the
     * terrain input fields
     */
    private ITerrain enteredTerrain() {
        int type = ((TerrainHelper)choTerrainType.getSelectedItem()).getTerrainType();
        int level = Integer.parseInt(texTerrainLevel.getText());
        boolean exitsSpecified = cheTerrExitSpecified.isSelected();
        int exits = Integer.parseInt(texTerrExits.getText());
        return Terrains.getTerrainFactory().createTerrain(type, level, exitsSpecified, exits);
    }

    /**
     * Add or set the terrain to the list based on the fields.
     */
    private void addSetTerrain() {
        ITerrain toAdd = enteredTerrain();
        if (((toAdd.getType() == Terrains.BLDG_ELEV) 
                || (toAdd.getType() == Terrains.BRIDGE_ELEV))
                && toAdd.getLevel() < 0) {
            JOptionPane.showMessageDialog(frame,
                    Messages.getString("BoardEditor.BridgeBuildingElevError"), //$NON-NLS-1$
                    Messages.getString("BoardEditor.invalidTerrainTitle"), //$NON-NLS-1$ 
                    JOptionPane.ERROR_MESSAGE);
            return;
        }
        curHex.addTerrain(toAdd);
        refreshTerrainList();
        repaintWorkingHex();
    }
    
    /**
     * Add to the terrain from one of the easy access buttons
     */
    private void addSetTerrainEasy(int type, int level) {
        boolean exitsSpecified = cheTerrExitSpecified.isSelected();
        int exits = Integer.parseInt(texTerrExits.getText());
        ITerrain toAdd = Terrains.getTerrainFactory().createTerrain(type, level, exitsSpecified, exits);
        if (((toAdd.getType() == Terrains.BLDG_ELEV) 
                || (toAdd.getType() == Terrains.BRIDGE_ELEV))
                && toAdd.getLevel() < 0) {
            JOptionPane.showMessageDialog(frame,
                    Messages.getString("BoardEditor.BridgeBuildingElevError"), //$NON-NLS-1$
                    Messages.getString("BoardEditor.invalidTerrainTitle"), //$NON-NLS-1$ 
                    JOptionPane.ERROR_MESSAGE);
            return;
        }
        curHex.addTerrain(toAdd);
        refreshTerrainList();
        repaintWorkingHex();
        // Choosing a terrain deselects the Lower/Raise terrain button
        buttonUpDn.setSelected(false);
    }

    /**
     * Set all the appropriate terrain field to match the currently selected
     * terrain in the list.
     */
    private void refreshTerrainFromList() {
        if (lisTerrain.getSelectedIndex() == -1) {
            return;
        }
        ITerrain terrain = Terrains.getTerrainFactory().createTerrain(
                lisTerrain.getSelectedValue());
        terrain = curHex.getTerrain(terrain.getType());
        choTerrainType.setSelectedItem(Terrains.getName(terrain.getType()));
        texTerrainLevel.setText(Integer.toString(terrain.getLevel()));
        cheTerrExitSpecified.setSelected(terrain.hasExitsSpecified());
        texTerrExits.setText(Integer.toString(terrain.getExits()));
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
        } else if (te.getDocument().equals(texTheme.getDocument())) {
            curHex.setTheme(texTheme.getText());
            repaintWorkingHex();
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
        } else if ("fileBoardExpand".equalsIgnoreCase(ae.getActionCommand())) { //$NON-NLS-1$
            ignoreHotKeys = true;
            boardResize();
            ignoreHotKeys = false;
        } else if ("fileBoardOpen".equalsIgnoreCase(ae.getActionCommand())) { //$NON-NLS-1$
            ignoreHotKeys = true;
            boardLoad();
            ignoreHotKeys = false;
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
                   && (lisTerrain.getSelectedValue() != null)) {
            ITerrain toRemove = Terrains.getTerrainFactory().createTerrain(
                    lisTerrain.getSelectedValue());
            curHex.removeTerrain(toRemove.getType());
            refreshTerrainList();
            repaintWorkingHex();
        } else if (ae.getSource().equals(butAddTerrain)) {
            addSetTerrain();
        } else if (ae.getSource().equals(butElevUp)
                   && (curHex.getLevel() < 9)) {
            curHex.setLevel(curHex.getLevel() + 1);
            texElev.setText(Integer.toString(curHex.getLevel()));
            repaintWorkingHex();
        } else if (ae.getSource().equals(butElevDown)
                   && (curHex.getLevel() > -5)) {
            curHex.setLevel(curHex.getLevel() - 1);
            texElev.setText(Integer.toString(curHex.getLevel()));
            repaintWorkingHex();
        } else if (ae.getSource().equals(butTerrExits)) {
            ExitsDialog ed = new ExitsDialog(frame);
            cheTerrExitSpecified.setSelected(true);
            ed.setExits(Integer.parseInt(texTerrExits.getText()));
            ed.setVisible(true);
            texTerrExits.setText(Integer.toString(ed.getExits()));
            addSetTerrain();
        } else if ("viewMiniMap".equalsIgnoreCase(ae.getActionCommand())) { //$NON-NLS-1$
            toggleMap();
        } else if (ae.getActionCommand().equals(ClientGUI.VIEW_ZOOM_IN)) {
            bv.zoomIn();
        } else if (ae.getActionCommand().equals(ClientGUI.VIEW_ZOOM_OUT)) {
            bv.zoomOut();
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
        } else if (ae.getSource().equals(buttonLW)) {
            if ((ae.getModifiers() & InputEvent.SHIFT_MASK) == 0) curHex.removeAllTerrains();  
            addSetTerrainEasy(Terrains.WOODS, 1);
        } else if (ae.getSource().equals(buttonHW)) {
            if ((ae.getModifiers() & InputEvent.SHIFT_MASK) == 0) curHex.removeAllTerrains();
            addSetTerrainEasy(Terrains.WOODS, 2);
        } else if (ae.getSource().equals(buttonUW)) {
            if ((ae.getModifiers() & InputEvent.SHIFT_MASK) == 0) curHex.removeAllTerrains();
            addSetTerrainEasy(Terrains.WOODS, 3);
        } else if (ae.getSource().equals(buttonLJ)) {
            if ((ae.getModifiers() & InputEvent.SHIFT_MASK) == 0) curHex.removeAllTerrains();  
            addSetTerrainEasy(Terrains.JUNGLE, 1);
        } else if (ae.getSource().equals(buttonHJ)) {
            if ((ae.getModifiers() & InputEvent.SHIFT_MASK) == 0) curHex.removeAllTerrains();
            addSetTerrainEasy(Terrains.JUNGLE, 2);
        } else if (ae.getSource().equals(buttonUJ)) {
            if ((ae.getModifiers() & InputEvent.SHIFT_MASK) == 0) curHex.removeAllTerrains();
            addSetTerrainEasy(Terrains.JUNGLE, 3);
        } else if (ae.getSource().equals(buttonWa)) {
            if ((ae.getModifiers() & InputEvent.SHIFT_MASK) == 0) curHex.removeAllTerrains();
            addSetTerrainEasy(Terrains.WATER, 1);
        } else if (ae.getSource().equals(buttonSw)) {
            if ((ae.getModifiers() & InputEvent.SHIFT_MASK) == 0) curHex.removeAllTerrains();
            addSetTerrainEasy(Terrains.SWAMP, 1);
        } else if (ae.getSource().equals(buttonRo)) {
            if ((ae.getModifiers() & InputEvent.SHIFT_MASK) == 0) curHex.removeAllTerrains();
            addSetTerrainEasy(Terrains.ROUGH, 1);
        } else if (ae.getSource().equals(buttonCl)) {
            curHex.removeAllTerrains();
            refreshTerrainList();
            repaintWorkingHex();
            buttonUpDn.setSelected(false);
        } else if (ae.getSource().equals(buttonBrush1)) {
            brushSize = 1;
            buttonBrush1.setSelected(true);
            buttonBrush2.setSelected(false);
            buttonBrush3.setSelected(false);
        } else if (ae.getSource().equals(buttonBrush2)) {
            brushSize = 2;
            buttonBrush1.setSelected(false);
            buttonBrush2.setSelected(true);
            buttonBrush3.setSelected(false);
        } else if (ae.getSource().equals(buttonBrush3)) {
            brushSize = 3;
            buttonBrush1.setSelected(false);
            buttonBrush2.setSelected(false);
            buttonBrush3.setSelected(true);
        } else if (ae.getSource().equals(buttonBu)) { 
            if ((ae.getModifiers() & InputEvent.SHIFT_MASK) == 0) curHex.removeAllTerrains();  
            addSetTerrainEasy(Terrains.BUILDING, 1);
            addSetTerrainEasy(Terrains.BLDG_ELEV, 1);
            addSetTerrainEasy(Terrains.BLDG_CF, 40);
        } else if (ae.getSource().equals(buttonRd)) {
            if ((ae.getModifiers() & InputEvent.SHIFT_MASK) == 0) curHex.removeAllTerrains();  
            addSetTerrainEasy(Terrains.ROAD, 1);
        } else if (ae.getSource().equals(buttonUpDn)) {
            // Not so useful to only do on clear terrain
            buttonOOC.setSelected(false);
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
                || texTerrainLevel.hasFocus() || texTerrExits.hasFocus()
                || texTheme.hasFocus();
    }
}
