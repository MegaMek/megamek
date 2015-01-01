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

package megamek.client.ui.AWT;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Canvas;
import java.awt.Checkbox;
import java.awt.Choice;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dialog;
import java.awt.FileDialog;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Label;
import java.awt.Panel;
import java.awt.SystemColor;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.TextEvent;
import java.awt.event.TextListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;

import keypoint.PngEncoder;
import megamek.client.event.BoardViewEvent;
import megamek.client.event.BoardViewListenerAdapter;
import megamek.client.ui.Messages;
import megamek.common.Coords;
import megamek.common.Game;
import megamek.common.Hex;
import megamek.common.IBoard;
import megamek.common.IHex;
import megamek.common.ITerrain;
import megamek.common.MapSettings;
import megamek.common.Terrains;
import megamek.common.util.BoardUtilities;

/**
 * The BoardEditor class is a
 */
public class BoardEditor extends Container implements ItemListener,
        ActionListener, TextListener, IMapSettingsObserver {

    /**
     * Serialization ID.
     */
    private static final long serialVersionUID = -8281854232284355981L;

    private final Frame frame = new Frame();

    private final Game game = new Game();
    private IBoard board = game.getBoard();
    private BoardView1 bv;
    private final CommonMenuBar menuBar = new CommonMenuBar();
    private CommonAboutDialog about = null;
    private CommonHelpDialog help = null;
    private CommonSettingsDialog setdlg = null;

    /**
     * The current working hex.
     */
    private IHex curHex = new Hex();

    private String curpath, curfile, curfileImage;

    // buttons and labels and such:
    private HexCanvas canHex;

    private Label labElev;
    private TextField texElev;
    private Button butElevUp;
    private Button butElevDown;

    private Label labTerrain;
    private java.awt.List lisTerrain;

    private Button butDelTerrain;

    private Panel panTerrainType;
    private Choice choTerrainType;
    private TextField texTerrainLevel;

    private Panel panTerrExits;
    private Checkbox cheTerrExitSpecified;
    private TextField texTerrExits;
    private Button butTerrExits;

    private Panel panRoads;
    private Checkbox cheRoadsAutoExit;

    private Label labTheme;
    private TextField texTheme;

    private Button butAddTerrain;

    private Label blankL;

    private Label labBoard;
    private Panel panButtons;
    private Button butBoardNew, butBoardLoad;
    private Button butBoardSave, butBoardSaveAs;
    private Button butBoardSaveAsImage;
    private Button butMiniMap;

    private Dialog minimapW;
    private MiniMap minimap;

    private Coords lastClicked;

    private MapSettings mapSettings = new MapSettings();

    /**
     * Creates and lays out a new Board Editor frame.
     */
    public BoardEditor() {
        try {
            bv = new BoardView1(game);
        } catch (final IOException e) {
            new AlertDialog(
                    frame,
                    Messages.getString("BoardEditor.FatalError"), Messages.getString("BoardEditor.CouldntInitialize") + e); //$NON-NLS-1$ //$NON-NLS-2$
            frame.dispose();
        }

        bv.addBoardViewListener(new BoardViewListenerAdapter() {

            public void hexMoused(final BoardViewEvent b) {
                final Coords c = b.getCoords();
                if (c.equals(lastClicked))
                    return;
                lastClicked = c;
                bv.cursor(c);
                if ((b.getModifiers() & InputEvent.ALT_MASK) != 0) {
                    setCurrentHex(board.getHex(b.getCoords()));
                } else if ((b.getModifiers() & InputEvent.CTRL_MASK) != 0) {
                    if (!board.getHex(b.getCoords()).equals(curHex)) {
                        paintHex(b.getCoords());
                    }
                } else if ((b.getModifiers() & InputEvent.SHIFT_MASK) != 0) {
                    addToHex(b.getCoords());
                } else if ((b.getModifiers() & InputEvent.BUTTON1_MASK) != 0) {
                    resurfaceHex(b.getCoords());
                }
            }
        });

        bv.setUseLOSTool(false);

        setupEditorPanel();
        setupFrame();

        frame.setVisible(true);

        if (true == GUIPreferences.getInstance().getNagForMapEdReadme()) {
            final String title = Messages.getString("BoardEditor.readme.title"); //$NON-NLS-1$
            final String body = Messages
                    .getString("BoardEditor.readme.message"); //$NON-NLS-1$
            final ConfirmDialog confirm = new ConfirmDialog(frame, title, body,
                    true);
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
        frame.setLayout(new BorderLayout());

        frame.add(bv.getComponent(), BorderLayout.CENTER);

        frame.add(this, BorderLayout.EAST);

        menuBar.addActionListener(this);
        frame.setMenuBar(menuBar);

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
            public void windowClosing(final WindowEvent e) {
                frame.setVisible(false);
                setMapVisible(false);
            }
        });
    }

    /**
     * Sets up the editor panel, which goes on the right of the map and has
     * controls for editing the current square.
     */
    private void setupEditorPanel() {
        canHex = new HexCanvas();
        labElev = new Label(
                Messages.getString("BoardEditor.labElev"), Label.RIGHT); //$NON-NLS-1$
        texElev = new TextField("0", 1); //$NON-NLS-1$
        texElev.addActionListener(this);
        texElev.addTextListener(this);
        butElevUp = new Button(Messages.getString("BoardEditor.butElevUp")); //$NON-NLS-1$
        butElevUp.addActionListener(this);
        butElevDown = new Button(Messages.getString("BoardEditor.butElevDown")); //$NON-NLS-1$
        butElevDown.addActionListener(this);

        labTerrain = new Label(
                Messages.getString("BoardEditor.labTerrain"), Label.LEFT); //$NON-NLS-1$
        lisTerrain = new java.awt.List(6);
        lisTerrain.addItemListener(this);
        refreshTerrainList();

        butDelTerrain = new Button(Messages
                .getString("BoardEditor.butDelTerrain")); //$NON-NLS-1$
        butDelTerrain.addActionListener(this);

        choTerrainType = new Choice();
        for (int i = 1; i < Terrains.SIZE; i++) {
            choTerrainType.add(Terrains.getName(i));
        }

        texTerrainLevel = new TextField("1", 1); //$NON-NLS-1$

        butAddTerrain = new Button(Messages
                .getString("BoardEditor.butAddTerrain")); //$NON-NLS-1$
        butAddTerrain.addActionListener(this);

        butMiniMap = new Button(Messages.getString("BoardEditor.butMiniMap")); //$NON-NLS-1$
        butMiniMap.setActionCommand("viewMiniMap"); //$NON-NLS-1$
        butMiniMap.addActionListener(this);

        panTerrainType = new Panel(new BorderLayout());
        panTerrainType.add(choTerrainType, BorderLayout.WEST);
        panTerrainType.add(texTerrainLevel, BorderLayout.CENTER);

        cheTerrExitSpecified = new Checkbox(Messages
                .getString("BoardEditor.cheTerrExitSpecified")); //$NON-NLS-1$
        butTerrExits = new Button(Messages
                .getString("BoardEditor.butTerrExits")); //$NON-NLS-1$
        texTerrExits = new TextField("0", 1); //$NON-NLS-1$
        butTerrExits.addActionListener(this);

        panTerrExits = new Panel(new FlowLayout());
        panTerrExits.add(cheTerrExitSpecified);
        panTerrExits.add(butTerrExits);
        panTerrExits.add(texTerrExits);

        panRoads = new Panel(new FlowLayout());
        cheRoadsAutoExit = new Checkbox(Messages
                .getString("BoardEditor.cheRoadsAutoExit")); //$NON-NLS-1$
        cheRoadsAutoExit.addItemListener(this);
        panRoads.add(cheRoadsAutoExit);

        labTheme = new Label(
                Messages.getString("BoardEditor.labTheme"), Label.LEFT); //$NON-NLS-1$
        texTheme = new TextField("", 15); //$NON-NLS-1$
        texTheme.addTextListener(this);

        labBoard = new Label(
                Messages.getString("BoardEditor.labBoard"), Label.LEFT); //$NON-NLS-1$
        butBoardNew = new Button(Messages.getString("BoardEditor.butBoardNew")); //$NON-NLS-1$
        butBoardNew.setActionCommand("fileBoardNew"); //$NON-NLS-1$
        butBoardNew.addActionListener(this);

        butBoardLoad = new Button(Messages
                .getString("BoardEditor.butBoardLoad")); //$NON-NLS-1$
        butBoardLoad.setActionCommand("fileBoardOpen"); //$NON-NLS-1$
        butBoardLoad.addActionListener(this);

        butBoardSave = new Button(Messages
                .getString("BoardEditor.butBoardSave")); //$NON-NLS-1$
        butBoardSave.setActionCommand("fileBoardSave"); //$NON-NLS-1$
        butBoardSave.addActionListener(this);

        butBoardSaveAs = new Button(Messages
                .getString("BoardEditor.butBoardSaveAs")); //$NON-NLS-1$
        butBoardSaveAs.setActionCommand("fileBoardSaveAs"); //$NON-NLS-1$
        butBoardSaveAs.addActionListener(this);

        butBoardSaveAsImage = new Button(Messages
                .getString("BoardEditor.butBoardSaveAsImage")); //$NON-NLS-1$
        butBoardSaveAsImage.setActionCommand("fileBoardSaveAsImage"); //$NON-NLS-1$
        butBoardSaveAsImage.addActionListener(this);

        panButtons = new Panel(new GridLayout(3, 2, 2, 2));
        panButtons.add(labBoard);
        panButtons.add(butBoardNew);
        panButtons.add(butBoardLoad);
        panButtons.add(butBoardSave);
        panButtons.add(butBoardSaveAs);
        panButtons.add(butBoardSaveAsImage);

        blankL = new Label("", Label.CENTER); //$NON-NLS-1$

        final GridBagLayout gridbag = new GridBagLayout();
        final GridBagConstraints c = new GridBagConstraints();
        setLayout(gridbag);

        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 0.0;
        c.weighty = 0.0;
        c.insets = new Insets(4, 4, 1, 1);

        c.gridwidth = GridBagConstraints.REMAINDER;
        addBag(canHex, gridbag, c);
        c.gridwidth = 1;
        addBag(labElev, gridbag, c);
        addBag(butElevUp, gridbag, c);
        addBag(butElevDown, gridbag, c);
        c.gridwidth = GridBagConstraints.REMAINDER;
        addBag(texElev, gridbag, c);

        addBag(labTerrain, gridbag, c);
        addBag(lisTerrain, gridbag, c);
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

        minimapW = new Dialog(frame,
                Messages.getString("BoardEditor.minimapW"), false); //$NON-NLS-1$
        minimapW.setLocation(GUIPreferences.getInstance().getMinimapPosX(),
                GUIPreferences.getInstance().getMinimapPosY());
        try {
            minimap = new MiniMap(minimapW, game, bv);
        } catch (final IOException e) {
            new AlertDialog(
                    frame,
                    Messages.getString("BoardEditor.FatalError"), Messages.getString("BoardEditor.CouldNotInitialiseMinimap") + e); //$NON-NLS-1$ //$NON-NLS-2$
            frame.dispose();
        }
        minimapW.add(minimap);

        setMapVisible(true);
    }

    private void addBag(final Component comp, final GridBagLayout gridbag,
            final GridBagConstraints c) {
        gridbag.setConstraints(comp, c);
        add(comp);
    }

    /**
     * Apply the current Hex to the Board at the specified location.
     * 
     * @param c the location.
     */
    public void paintHex(final Coords c) {
        board.setHex(c, curHex.duplicate());
    }

    /**
     * Apply the current Hex to the Board at the specified location.
     * 
     * @param c the location in {@link megamek.common.Coords , Coords }.
     */
    public void resurfaceHex(final Coords c) {
        if (board.contains(c)) {
            final IHex newHex = curHex.duplicate();
            newHex.setElevation(board.getHex(c).getElevation());
            board.setHex(c, newHex);
        }
    }

    /**
     * Apply the current Hex to the Board at the specified location.
     * 
     * @param c the location in {@link megamek.common.Coords , Coords }.
     */
    public void addToHex(final Coords c) {
        if (board.contains(c)) {
            final IHex newHex = curHex.duplicate();
            final IHex oldHex = board.getHex(c);
            newHex.setElevation(oldHex.getElevation());
            for (int i = 0; i < Terrains.SIZE; i++) {
                if (!newHex.containsTerrain(i) && oldHex.containsTerrain(i)) {
                    newHex.addTerrain(oldHex.getTerrain(i));
                }
            }
            board.setHex(c, newHex);
        }
    }

    /**
     * Sets the current hex
     * 
     * @param hex hex to set.
     */
    public void setCurrentHex(final IHex hex) {
        curHex = hex.duplicate();

        texElev.setText(Integer.toString(curHex.getElevation()));

        refreshTerrainList();

        if (lisTerrain.getItemCount() > 0) {
            lisTerrain.select(0);
            refreshTerrainFromList();
        }

        texTheme.setText(curHex.getTheme());
        repaint();
        repaintWorkingHex();
    }

    /**
     * Repaints the current working hex.
     */
    private void repaintWorkingHex() {
        if (curHex != null) {
            final TilesetManager tm = bv.getTilesetManager();
            tm.clearHex(curHex);
        }
        canHex.repaint();
        lastClicked = null;
    }

    /**
     * Refreshes the terrain list to match the current hex
     */
    public void refreshTerrainList() {
        lisTerrain.removeAll();
        for (int i = 0; i < Terrains.SIZE; i++) {
            final ITerrain terrain = curHex.getTerrain(i);
            if (terrain != null) {
                lisTerrain.add(terrain.toString());
            }
        }
    }

    /**
     * Returns a new instance of the terrain that is currently entered in the
     * terrain input fields
     */
    private ITerrain enteredTerrain() {
        final int type = Terrains.getType(choTerrainType.getSelectedItem());
        final int level = Integer.parseInt(texTerrainLevel.getText());
        final boolean exitsSpecified = cheTerrExitSpecified.getState();
        final int exits = Integer.parseInt(texTerrExits.getText());
        return Terrains.getTerrainFactory().createTerrain(type, level,
                exitsSpecified, exits);
    }

    /**
     * Add or set the terrain to the list based on the fields.
     */
    private void addSetTerrain() {
        final ITerrain toAdd = enteredTerrain();
        curHex.addTerrain(toAdd);
        refreshTerrainList();
        repaintWorkingHex();
    }

    /**
     * Set all the appropriate terrain field to match the currently selected
     * terrain in the list.
     */
    private void refreshTerrainFromList() {
        ITerrain terrain = Terrains.getTerrainFactory().createTerrain(
                lisTerrain.getSelectedItem());
        terrain = curHex.getTerrain(terrain.getType());

        choTerrainType.select(Terrains.getName(terrain.getType()));
        texTerrainLevel.setText(Integer.toString(terrain.getLevel()));
        cheTerrExitSpecified.setState(terrain.hasExitsSpecified());
        texTerrExits.setText(Integer.toString(terrain.getExits()));
    }

    /**
     * Initialize a new data set in the current board. If hexes are loaded,
     * brings up a dialog box requesting width and height and default hex. If
     * height and width are valid, creates new board data and fills it with the
     * selected hex.
     */
    public void boardNewXX() {
        // display new board dialog
        final BoardNewDialog bnd = new BoardNewDialog(frame, lisTerrain
                .getItems(), lisTerrain.getSelectedIndex());
        bnd.setLocation(frame.getLocation().x + 150,
                frame.getLocation().y + 100);
        bnd.setVisible(true);

        if (bnd.getX() > 0 || bnd.getY() > 0) {
            final IHex[] newHexes = new IHex[bnd.getX() * bnd.getY()];
            for (int i = 0; i < newHexes.length; i++) {
                newHexes[i] = new Hex();
            }
            board.newData(bnd.getX(), bnd.getY(), newHexes);
            curpath = null;
            curfile = null;
            frame.setTitle(Messages.getString("BoardEditor.title")); //$NON-NLS-1$
            menuBar.setBoard(true);
        }
    }

    public void boardNew() {
        final RandomMapDialog rmd = new RandomMapDialog(frame, this,
                mapSettings);
        rmd.setVisible(true);

        board = BoardUtilities.generateRandom(mapSettings);
        game.setBoard(board);
        curpath = null;
        curfile = null;
        frame.setTitle(Messages.getString("BoardEditor.title")); //$NON-NLS-1$
        menuBar.setBoard(true);
    }

    /**
     * Updates the map settings.
     * 
     * @param newSettings the new settings.
     */
    public void updateMapSettings(final MapSettings newSettings) {
        mapSettings = newSettings;
    }

    /**
     * Selects and loads a file into the BoardEditor. Called when the user
     * selects load file.
     */
    public void boardLoad() {
        final FileDialog fd = new FileDialog(frame, Messages
                .getString("BoardEditor.loadBoard"), FileDialog.LOAD); //$NON-NLS-1$
        fd.setDirectory("data" + File.separator + "boards"); //$NON-NLS-1$ //$NON-NLS-2$
        fd
                .setLocation(frame.getLocation().x + 150,
                        frame.getLocation().y + 100);
        fd.setVisible(true);

        if (fd.getFile() == null) {
            // I want a file, y'know!
            return;
        }
        curpath = fd.getDirectory();
        curfile = fd.getFile();
        // load!
        try {
            final InputStream is = new FileInputStream(new File(curpath,
                    curfile));
            // tell the board to load!
            board.load(is);
            // okay, done!
            is.close();
            menuBar.setBoard(true);
        } catch (final IOException ex) {
            System.err.println("error opening file to load!"); //$NON-NLS-1$
            System.err.println(ex);
        }

        frame.setTitle(Messages.getString("BoardEditor.title0") + curfile); //$NON-NLS-1$

        cheRoadsAutoExit.setState(board.getRoadsAutoExit());

        refreshTerrainList();
    }

    /**
     * Checks to see if there is already a path and name stored; if not, calls
     * "save as"; otherwise, saves the board to the specified file.
     */
    public void boardSave() {
        if (curfile == null) {
            boardSaveAs();
            return;
        }
        // save!
        try {
            final OutputStream os = new FileOutputStream(new File(curpath,
                    curfile));
            // tell the board to save!
            board.save(os);
            // okay, done!
            os.close();
        } catch (final IOException ex) {
            System.err.println("error opening file to save!"); //$NON-NLS-1$
            System.err.println(ex);
        }
    }

    /**
     * Saves the board in PNG image format.
     */
    public void boardSaveImage() {
        if (curfileImage == null) {
            boardSaveAsImage();
            return;
        }
        final Dialog waitD = new Dialog(this.frame, Messages
                .getString("BoardEditor.waitDialog.title")); //$NON-NLS-1$
        waitD.add(new Label(Messages
                .getString("BoardEditor.waitDialog.message"))); //$NON-NLS-1$
        waitD.setSize(250, 130);
        // move to middle of screen
        waitD.setLocation(
                frame.getSize().width / 2 - waitD.getSize().width / 2, frame
                        .getSize().height
                        / 2 - waitD.getSize().height / 2);
        waitD.setVisible(true);
        frame.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        waitD.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        // save!
        final int filter = 0; // 0 - no filter; 1 - sub; 2 - up
        final int compressionLevel = 9; // 0 to 9 with 0 being no compression
        final PngEncoder png = new PngEncoder(bv.getEntireBoardImage(),
                PngEncoder.NO_ALPHA, filter, compressionLevel);
        try {
            final FileOutputStream outfile = new FileOutputStream(curfileImage);
            byte[] pngbytes;
            pngbytes = png.pngEncode();
            if (pngbytes == null) {
                System.out.println("Failed to save board as image:Null image"); //$NON-NLS-1$
            } else {
                outfile.write(pngbytes);
            }
            outfile.flush();
            outfile.close();
        } catch (final IOException e) {
            e.printStackTrace();
        }
        waitD.setVisible(false);
        frame.setCursor(Cursor.getDefaultCursor());
    }

    /**
     * Opens a file dialog box to select a file to save as; saves the board to
     * the file.
     */
    public void boardSaveAs() {
        final FileDialog fd = new FileDialog(frame, Messages
                .getString("BoardEditor.saveBoardAs"), FileDialog.SAVE); //$NON-NLS-1$
        fd.setDirectory("data" + File.separator + "boards"); //$NON-NLS-1$ //$NON-NLS-2$
        fd
                .setLocation(frame.getLocation().x + 150,
                        frame.getLocation().y + 100);
        fd.setVisible(true);

        if (fd.getFile() == null) {
            // I want a file, y'know!
            return;
        }
        curpath = fd.getDirectory();
        curfile = fd.getFile();

        // make sure the file ends in board
        if (!curfile.toLowerCase().endsWith(".board")) { //$NON-NLS-1$
            curfile += ".board"; //$NON-NLS-1$
        }

        frame.setTitle(Messages.getString("BoardEditor.title0") + curfile); //$NON-NLS-1$

        boardSave();
    }

    /**
     * Opens a file dialog box to select a file to save as; saves the board to
     * the file as an image. Useful for printing boards.
     */
    public void boardSaveAsImage() {
        final FileDialog fd = new FileDialog(frame, Messages
                .getString("BoardEditor.saveAsImage"), FileDialog.SAVE); //$NON-NLS-1$
        // fd.setDirectory("data" + File.separator + "boards");
        fd
                .setLocation(frame.getLocation().x + 150,
                        frame.getLocation().y + 100);

        // Add a filter for PNG files
        fd.setFilenameFilter(new FilenameFilter() {
            public boolean accept(final File dir, final String name) {
                return (null != name && name.endsWith(".png")); //$NON-NLS-1$
            }
        });

        // use base directory by default
        fd.setDirectory("."); //$NON-NLS-1$

        // Default to the board's name (if it has one).
        String fileName = null;
        if (null != curfile && curfile.length() > 0) {
            fileName = curfile.toUpperCase();
            if (fileName.endsWith(".BOARD")) { //$NON-NLS-1$
                final int length = fileName.length();
                fileName = fileName.substring(0, length - 6);
            }
            fileName = fileName.toLowerCase() + ".png"; //$NON-NLS-1$
            fd.setFile(fileName);
        }

        // Open the dialog and wait for it's return.
        fd.setVisible(true);

        if (fd.getFile() == null) {
            // I want a file, y'know!
            return;
        }
        curpath = fd.getDirectory();
        curfileImage = fd.getFile();

        // make sure the file ends in board
        if (!curfileImage.toLowerCase().endsWith(".png")) { //$NON-NLS-1$
            curfileImage += ".png"; //$NON-NLS-1$
        }

        frame.setTitle(Messages.getString("BoardEditor.title0") + curfileImage); //$NON-NLS-1$

        boardSaveImage();
    }

    //
    // ItemListener
    //
    /**
     * @see java.awt.event.ItemListener
     * @param ie the ItemEvent
     */
    public void itemStateChanged(final ItemEvent ie) {
        if (ie.getSource() == lisTerrain) {
            refreshTerrainFromList();
        } else if (ie.getSource() == cheRoadsAutoExit) {
            // Set the new value for the option, and refresh the board.
            board.setRoadsAutoExit(cheRoadsAutoExit.getState());
            bv.updateBoard();
            repaintWorkingHex();
        }
    }

    //
    // TextListener
    //
    /**
     * @see java.awt.event.TextListener
     * @param te the TextEvent
     */
    public void textValueChanged(final TextEvent te) {
        if (te.getSource() == texElev) {
            int value;
            try {
                value = Integer.parseInt(texElev.getText());
            } catch (final NumberFormatException ex) {
                return;
            }
            if (value != curHex.getElevation()) {
                curHex.setElevation(value);
                repaintWorkingHex();
            }
        } else if (te.getSource() == texTheme) {
            curHex.setTheme(texTheme.getText());
            repaintWorkingHex();
        }
    }

    /**
     * Called when the user selects the "Help->About" menu item.
     */
    private void showAbout() {
        // Do we need to create the "about" dialog?
        if (this.about == null) {
            this.about = new CommonAboutDialog(this.frame);
        }

        // Show the about dialog.
        this.about.setVisible(true);
    }

    /**
     * Called when the user selects the "Help->Contents" menu item.
     */
    private void showHelp() {
        // Do we need to create the "help" dialog?
        if (this.help == null) {
            final File helpfile = new File("docs", "editor-readme.txt"); //$NON-NLS-1$
            this.help = new CommonHelpDialog(this.frame, helpfile);
        }

        // Show the help dialog.
        this.help.setVisible(true);
    }

    /**
     * Called when the user selects the "View->Client Settings" menu item.
     */
    private void showSettings() {
        // Do we need to create the "settings" dialog?
        if (this.setdlg == null) {
            this.setdlg = new CommonSettingsDialog(this.frame);
        }

        // Show the settings dialog.
        this.setdlg.setVisible(true);
    }

    //
    // ActionListener
    //
    public void actionPerformed(final ActionEvent ae) {
        if (ae.getActionCommand().equalsIgnoreCase("fileBoardNew")) { //$NON-NLS-1$
            boardNew();
        } else if (ae.getActionCommand().equalsIgnoreCase("fileBoardOpen")) { //$NON-NLS-1$
            boardLoad();
        } else if (ae.getActionCommand().equalsIgnoreCase("fileBoardSave")) { //$NON-NLS-1$
            boardSave();
        } else if (ae.getActionCommand().equalsIgnoreCase("fileBoardSaveAs")) { //$NON-NLS-1$
            boardSaveAs();
        } else if (ae.getActionCommand().equalsIgnoreCase(
                "fileBoardSaveAsImage")) { //$NON-NLS-1$
            boardSaveAsImage();
        } else if (ae.getSource() == butDelTerrain
                && lisTerrain.getSelectedItem() != null) {
            final ITerrain toRemove = Terrains.getTerrainFactory()
                    .createTerrain(lisTerrain.getSelectedItem());
            curHex.removeTerrain(toRemove.getType());
            refreshTerrainList();
            repaintWorkingHex();
        } else if (ae.getSource() == butAddTerrain) {
            addSetTerrain();
        } else if (ae.getSource() == butElevUp && curHex.getElevation() < 9) {
            curHex.setElevation(curHex.getElevation() + 1);
            texElev.setText(Integer.toString(curHex.getElevation()));
            repaintWorkingHex();
        } else if (ae.getSource() == butElevDown && curHex.getElevation() > -5) {
            curHex.setElevation(curHex.getElevation() - 1);
            texElev.setText(Integer.toString(curHex.getElevation()));
            repaintWorkingHex();
        } else if (ae.getSource() == butTerrExits) {
            final ExitsDialog ed = new ExitsDialog(frame);
            cheTerrExitSpecified.setState(true);
            ed.setExits(Integer.parseInt(texTerrExits.getText()));
            ed.setVisible(true);
            texTerrExits.setText(Integer.toString(ed.getExits()));
            addSetTerrain();
        } else if (ae.getActionCommand().equalsIgnoreCase("viewMiniMap")) { //$NON-NLS-1$
            toggleMap();
        } else if (ae.getActionCommand().equals(ClientGUI.VIEW_ZOOM_IN)) {
            bv.zoomIn();
        } else if (ae.getActionCommand().equals(ClientGUI.VIEW_ZOOM_OUT)) {
            bv.zoomOut();
        } else if (ae.getActionCommand().equalsIgnoreCase("helpAbout")) { //$NON-NLS-1$
            showAbout();
        } else if (ae.getActionCommand().equalsIgnoreCase("helpContents")) { //$NON-NLS-1$
            showHelp();
        } else if (ae.getActionCommand().equalsIgnoreCase("viewClientSettings")) { //$NON-NLS-1$
            showSettings();
        }
    }

    /**
     * Displays the currently selected hex picture, in component form
     */
    private class HexCanvas extends Canvas {

        private static final long serialVersionUID = 537547390238897105L;

        public HexCanvas() {
            super();
            setSize(72, 72);
        }

        public void paint(final Graphics g) {
            update(g);
        }

        public void update(final Graphics g) {
            if (curHex != null) {
                final TilesetManager tm = bv.getTilesetManager();
                g.drawImage(tm.baseFor(curHex), 0, 0, this);
                if (tm.supersFor(curHex) != null) {
                    for (final Iterator<Image> i = tm.supersFor(curHex)
                            .iterator(); i.hasNext();) {
                        g.drawImage(i.next(), 0, 0, this);
                        g.drawString(
                                Messages.getString("BoardEditor.SUPER"), 0, 10); //$NON-NLS-1$
                    }
                }
                g.setFont(new Font("SansSerif", Font.PLAIN, 9)); //$NON-NLS-1$
                g
                        .drawString(
                                Messages.getString("BoardEditor.LEVEL") + curHex.getElevation(), 24, 70); //$NON-NLS-1$
            } else {
                g.clearRect(0, 0, 72, 72);
            }
        }
    }

    /**
     * @return the frame this is displayed in
     */
    public Frame getFrame() {
        return frame;
    }

    /**
     * Toggles the minimap window Also, toggles the minimap enabled setting
     */
    public void toggleMap() {
        setMapVisible(!minimapW.isVisible());
    }

    /**
     * Sets if the map is visible or not.
     * 
     * @param visible true if the map should be visible, false if not.
     */
    public void setMapVisible(final boolean visible) {
        minimapW.setVisible(visible);
    }
}
