/**
 * MegaMek - Copyright (C) 2000-2002 Ben Mazur (bmazur@sev.org)
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

package megamek.client;

import java.awt.*;
import java.awt.event.*;
import java.util.Vector;
import java.util.Enumeration;
import java.io.*;

import megamek.common.*;

public class BoardEditor extends Container
    implements BoardListener, ItemListener, ActionListener,
    KeyListener
{
    public Frame                frame;
    
    public Board                board;
    private BoardView1          bv;
    public Hex                    curHex;
    
    public String                curpath, curfile;
    
    public boolean                ctrlheld, altheld;
    
    // buttons and labels and such:
    public Label                nameL, typeL;
    public HexCanvas            hexC;
    public Label                elevL;
    public Choice                elevC;
    public Label                loadedL;
    public List                    loadedList;
    public Label                blankL;
    public Button                loadHexesB, clearUnusedB;
    
    public Label                boardL;
    public Button                boardNewB, boardLoadB;
    public Button                boardSaveB, boardSaveAsB;
    
    /**
     * Contruct a new board editor panel.
     * 
     * @param frame            parent frame, for dialogs & such.
     * @param board            the board to edit.
     */
    public BoardEditor(Frame frame, Board board, BoardView1 bv) {
        this.frame = frame;
        this.board = board;
        this.bv = bv;
        
        board.newData(0, 0, new Hex[0]);
        
        frame.setTitle("MegaMek Editor : Unnamed");
        
        nameL = new Label("Name (no tile selected)", Label.CENTER);
        typeL = new Label("Type (no tile selected)", Label.CENTER);
        hexC = new HexCanvas();
        elevL = new Label("Elevation:", Label.RIGHT);
        elevC = new Choice();
        for(int i = 9; i > -6; i--) {
            elevC.add("" + i);
        }
        elevC.select("0");
        elevC.addItemListener(this);
        
        loadedL = new Label("Hexes Loaded:", Label.LEFT);
        loadedList = new List(10);
        loadedList.addItemListener(this);
        refreshTerrainList();
        loadHexesB = new Button("Load More Hexes...");
        loadHexesB.setActionCommand("hexes_load");
        loadHexesB.addActionListener(this);
        clearUnusedB = new Button("Clear Unused Hexes");
        clearUnusedB.setActionCommand("hexes_clear");
        clearUnusedB.addActionListener(this);
        
        boardL = new Label("Board:", Label.LEFT);
        boardNewB = new Button("New...");
        boardNewB.setActionCommand("board_new");
        boardNewB.addActionListener(this);
        boardLoadB = new Button("Load...");
        boardLoadB.setActionCommand("board_load");
        boardLoadB.addActionListener(this);
        boardSaveB = new Button("Save");
        boardSaveB.setActionCommand("board_save");
        boardSaveB.addActionListener(this);
        boardSaveAsB = new Button("Save As...");
        boardSaveAsB.setActionCommand("board_saveas");
        boardSaveAsB.addActionListener(this);
        
        blankL = new Label("", Label.CENTER);
        
        GridBagLayout gridbag = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();
        setLayout(gridbag);
        
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 0.0;    c.weighty = 0.0;
        c.insets = new Insets(4, 4, 1, 1);
        
        c.gridwidth = GridBagConstraints.REMAINDER; 
        addBag(nameL, gridbag, c);
        addBag(hexC, gridbag, c);
        addBag(typeL, gridbag, c);
        c.gridwidth = 1; 
        addBag(elevL, gridbag, c);
        c.gridwidth = GridBagConstraints.REMAINDER; 
        addBag(elevC, gridbag, c);
        
        addBag(loadedL, gridbag, c);
        addBag(loadedList, gridbag, c);
        addBag(loadHexesB, gridbag, c);
        addBag(clearUnusedB, gridbag, c);
    
        c.weightx = 1.0;    c.weighty = 1.0;
        addBag(blankL, gridbag, c);
        
        c.weightx = 0.0;    c.weighty = 0.0;
        addBag(boardL, gridbag, c);
        c.gridwidth = 1; 
        addBag(boardNewB, gridbag, c);
        c.gridwidth = GridBagConstraints.REMAINDER; 
        addBag(boardLoadB, gridbag, c);
        c.gridwidth = 1; 
        addBag(boardSaveB, gridbag, c);
        c.gridwidth = GridBagConstraints.REMAINDER; 
        addBag(boardSaveAsB, gridbag, c);

    }
    
    private void addBag(Component comp, GridBagLayout gridbag, GridBagConstraints c) {
        gridbag.setConstraints(comp, c);
        add(comp);
        comp.addKeyListener(this);
    }
    
    /**
     * Apply the current Hex to the Board at the specified
     * location.
     */
    public void paintHex(Coords c) {
        board.setHex(c, new Hex(curHex));
    }
    
    /**
     * Sets the current hex
     * 
     * @param hex            hex to set.
     */
    public void setCurrentHex(Hex hex) {
        curHex = new Hex(hex);
        
        nameL.setText(curHex.getTerrain().getName());
        typeL.setText(Terrain.TERRAIN_NAMES[curHex.getTerrainType()]);
        elevC.select(0 - (curHex.getElevation() - 9));
            
        selectCurrentHex();
        
        repaint();
        hexC.repaint();
    }
    
    /**
     * Refreshes the hex list from the hexesLoaded Vector.
     */
    public void refreshTerrainList() {
        loadedList.removeAll();
    for (int i = 0; i < board.terrains.length; i++) {
            Terrain terrain = board.terrains[i];
            loadedList.add(terrain.getName());
        }
        selectCurrentHex();
    }
    
    /**
     * Goes through the loadedList and if a name matches the
     * current hex name, selects that name.
     */
    public void selectCurrentHex() {
        for(int i = 0; i < loadedList.getItemCount(); i++) {
            if(curHex != null && loadedList.getItem(i).equalsIgnoreCase(curHex.getTerrain().getName())) {
                loadedList.select(i);
            }
        }
    }
    
    /**
     * Displays a file selection box; loads the selected file
     * as a text file and parses it for hex information.
     */
    public void loadTerrainFile() {
        FileDialog fd = new FileDialog(frame, "Load Hexes From...", FileDialog.LOAD);
        fd.setDirectory("data" + File.separator + "hexes");
        fd.setLocation(frame.getLocation().x + 150, frame.getLocation().y + 100);
        fd.show();
        
        if(fd.getFile() == null) {
            // I want a file, y'know!
            return;
        }
        
        parseTerrainFile(fd.getDirectory(), fd.getFile());
    
    refreshTerrainList();
    }
    
    /**
     * Parses a text file for terrain information; loads
     * all parsed terrains into the terrain thing, if new.
     * 
     * Hexes should appear in the text file like this:
     *        hex "name" type_string "picfile"
     * 
     * @param path            the path of the text file and the 
     *                        hex images.
     * @param filename        the text file name.
     */
    public void parseTerrainFile(String path, String filename) {
    Vector terrainLoaded = new Vector();
    for(int i = 0; i < board.terrains.length; i++) {
      terrainLoaded.addElement(board.terrains[i]);
    }
    
        String remove = System.getProperty("user.dir") + File.separator;
        if(path.startsWith(remove)) {
            path = path.substring(remove.length()).replace(File.separatorChar, '/');
        }
        try {
            Reader r = new BufferedReader(new FileReader(new File(path, filename)));
            StreamTokenizer st = new StreamTokenizer(r);
            st.eolIsSignificant(true);
            st.commentChar('#');
            st.quoteChar('"');
            st.wordChars('_', '_');
            while(st.nextToken() != StreamTokenizer.TT_EOF) {
                if(st.ttype == StreamTokenizer.TT_WORD && st.sval.equalsIgnoreCase("hex")) {
                    // read rest of line
                    String[] args = {"", "", ""};
                    int i = 0;
                    while(st.nextToken() == StreamTokenizer.TT_WORD || st.ttype == '"') {
                        args[i++] = st.sval;
                    }
                    // check terrainB type
                    int terrainType = Hex.parse(args[1]);
                    // if valid, add to list
                    if(terrainType != -1) {
            Terrain newTerrain = new Terrain(args[0], terrainType, path + args[2]);
            if (terrainLoaded.indexOf(newTerrain) == -1) {
                          terrainLoaded.addElement(newTerrain);
            }
                    }
                }
            }
            r.close();
        } catch(IOException ex) {
            System.err.println("I/O error reading terrain definition file");
            System.err.println(ex);
        }
    
    board.terrains = new Terrain[terrainLoaded.size()];
    terrainLoaded.copyInto(board.terrains);
    }
    
    /**
     * Initialize a new data set in the current board.
     * 
     * First, checks to see if any hexes are loaded.  If not, 
     * tries to make the user load some.  If still no hexes,
     * cancels out.
     * 
     * If hexes are loaded, brings up a dialog box requesting
     * width and height and default hex.  If height and width
     * are valid, creates new board data and fills it with the
     * selected hex.
     */
    public void boardNew() {
        if(board.terrains.length == 0) {
            // if there's no hexes loaded, give them a chance to load some...
            loadTerrainFile();
            if(board.terrains.length == 0) {
                // if there's still no hexes, then cancel
                return;
            }
            refreshTerrainList();
        }
        // display new board dialog
        BoardNewDialog bnd = new BoardNewDialog(frame, loadedList.getItems(), loadedList.getSelectedIndex());
        bnd.setLocation(frame.getLocation().x + 150, frame.getLocation().y + 100);
        bnd.show();
        
        if(bnd.getX() > 0 || bnd.getY() > 0) {
            board.newData(bnd.getX(), bnd.getY());
            for(int i = 0; i < board.data.length; i++) {
                board.data[i] = new Hex(board.terrains[bnd.getSelected()], 0);
            }
            loadedList.select(bnd.getSelected());
      curpath = null;
          curfile = null;
      frame.setTitle("MegaMek Editor : Unnamed");
        }
    }
    
    public void boardLoad() {
        FileDialog fd = new FileDialog(frame, "Load Board...", FileDialog.LOAD);
        fd.setDirectory("data" + File.separator + "boards");
        fd.setLocation(frame.getLocation().x + 150, frame.getLocation().y + 100);
        fd.show();
        
        if(fd.getFile() == null) {
            // I want a file, y'know!
            return;
        }
        curpath = fd.getDirectory();
        curfile = fd.getFile();
        // load!
        try {
            InputStream is = new FileInputStream(new File(curpath, curfile));
            // tell the board to load!
            board.load(is);
            // okay, done!
            is.close();
        } catch(IOException ex) {
            System.err.println("error opening file to save!");
            System.err.println(ex);
        }

    frame.setTitle("MegaMek Editor : " + curfile);
    
    refreshTerrainList();
    }
    
    /**
     * Checks to see if there is already a path and name
     * stored; if not, calls "save as"; otherwise, saves 
     * the board to the specified file.
     */
    public void boardSave() {
        if(curfile == null) {
            boardSaveAs();
            return;
        }
        // save!
        try {
            OutputStream os = new FileOutputStream(new File(curpath, curfile));
            // tell the board to save!
            board.save(os);
            // okay, done!
            os.close();
        } catch(IOException ex) {
            System.err.println("error opening file to save!");
            System.err.println(ex);
        }
    }
    
    /**
     * Opens a file dialog box to select a file to save as;
     * saves the board to the file.
     */
    public void boardSaveAs() {
        FileDialog fd = new FileDialog(frame, "Save Board As...", FileDialog.SAVE);
        fd.setDirectory("data" + File.separator + "boards");
        fd.setLocation(frame.getLocation().x + 150, frame.getLocation().y + 100);
        fd.show();
        
        if(fd.getFile() == null) {
            // I want a file, y'know!
            return;
        }
        curpath = fd.getDirectory();
        curfile = fd.getFile();
        
    frame.setTitle("MegaMek Editor : " + curfile);

    boardSave();
    }
    
    
    //
    // BoardListener
    //
    public void boardHexMoused(BoardEvent b) {
        board.cursor(b.getCoords());
        if(altheld) {
            setCurrentHex(board.getHex(b.getCoords()));
            board.highlight(b.getCoords());
        }
        if(ctrlheld) {
            if(!board.getHex(b.getCoords()).equals(curHex)) {
                paintHex(b.getCoords());
            }
        }
    }
    public void boardHexSelected(BoardEvent b) {
        ;
    }
    public void boardHexCursor(BoardEvent b) {
        ;
    }
    public void boardHexHighlighted(BoardEvent b) {
        ;
    }
    public void boardChangedHex(BoardEvent b) {
        ;
    }
    public void boardChangedEntity(BoardEvent b) {
        ;
    }
    public void boardNewEntities(BoardEvent b) {
        ;
    }
    public void boardNewVis(BoardEvent b) {
        ;
    }
    public void boardNewBoard(BoardEvent b) {
        refreshTerrainList();
    
    }
    
    //
    // ItemListener
    //
    public void itemStateChanged(ItemEvent ie) {
        if(ie.getSource().equals(elevC)) {
            curHex.setElevation(9 - elevC.getSelectedIndex());
            hexC.repaint();
        }
        if(ie.getSource().equals(loadedList)) {
      int elevation = curHex == null ? 0 : curHex.getElevation();
            setCurrentHex(new Hex(board.terrains[loadedList.getSelectedIndex()], elevation));
        }
    }
    
    //
    // KeyListener
    //
    public void keyPressed(KeyEvent ke) {
        switch(ke.getKeyCode()) {
        case KeyEvent.VK_CONTROL : 
            if(!ctrlheld) {
                paintHex(board.lastCursor);
                ctrlheld = true;
            }
            break;
        case KeyEvent.VK_ALT : 
            if(!altheld) {
                setCurrentHex(board.getHex(board.lastCursor));
                altheld = true;
                //board.highlight(board.lastCursor);
            }
            break;
        }
    }
    public void keyReleased(KeyEvent ke) {
        switch(ke.getKeyCode()) {
        case KeyEvent.VK_CONTROL : 
            ctrlheld = false; 
            break;
        case KeyEvent.VK_ALT : 
            altheld = false; 
            break;
        }
    }
    public void keyTyped(KeyEvent ke) {
        ;
    }
    
    //
    // ActionListener
    //
    public void actionPerformed(ActionEvent ae) {
        if(ae.getActionCommand().equalsIgnoreCase("hexes_load")) {
            loadTerrainFile();
            refreshTerrainList();
        }
        if(ae.getActionCommand().equalsIgnoreCase("hexes_clear")) {
            //getHexesFromBoard(false);
            refreshTerrainList();
        }
        if(ae.getActionCommand().equalsIgnoreCase("board_new")) {
            boardNew();
        }
        if(ae.getActionCommand().equalsIgnoreCase("board_load")) {
            boardLoad();
        }
        if(ae.getActionCommand().equalsIgnoreCase("board_save")) {
            boardSave();
        }
        if(ae.getActionCommand().equalsIgnoreCase("board_saveas")) {
            boardSaveAs();
        }
    }

    
    
    /**
     * Displays the currently selected hex picture, in 
     * component form
     */
    private class HexCanvas extends Canvas {
        public HexCanvas() {
            super();
            setSize(72, 72);
        }
        
        public void paint(Graphics g) {
            update(g);
        }
        
        public void update(Graphics g) {
            if(curHex != null) {
                g.drawImage(curHex.getImage(this), 0, 0, this);
                g.setFont(new Font("SansSerif", Font.PLAIN, 9));
                g.drawString("LEVEL " + curHex.getElevation(), 24, 70);
            } else {
                g.clearRect(0, 0, 72, 72);
            }
        }
    }
}

/**
 * here's a quick class for the new map diaglogue box
 */
class BoardNewDialog extends Dialog implements ActionListener {
    public int            xvalue, yvalue;
    public int            selected;
    
    protected Label        xL, yL;
    protected Label        defaultL;
    protected Choice    defaultC;
    protected TextField    xT, yT;
    protected Button        okayB, cancelB;
    
    public BoardNewDialog(Frame frame, String[] hexList, int hexSelected) {
        super(frame, "Set Dimensions", true);
        
        xvalue = 0;
        yvalue = 0;
        
        xL = new Label("Width:", Label.RIGHT);
        yL = new Label("Height:", Label.RIGHT);
        
        xT = new TextField("16", 2);
        yT = new TextField("17", 2);
        
        defaultL = new Label("Default:", Label.RIGHT);
        defaultC = new Choice();
        for(int i = 0; i < hexList.length; i++) {
            defaultC.add(hexList[i]);
        }
        if(hexSelected != -1) {
            defaultC.select(hexSelected);
        }
        
        okayB = new Button("Okay");
        okayB.setActionCommand("done");
        okayB.addActionListener(this);
        okayB.setSize(80, 24);

        cancelB = new Button("Cancel");
        cancelB.setActionCommand("cancel");
        cancelB.addActionListener(this);
        cancelB.setSize(80, 24);

        GridBagLayout gridbag = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();
        setLayout(gridbag);

        c.fill = GridBagConstraints.NONE;
        c.weightx = 0.0;    c.weighty = 0.0;
        c.insets = new Insets(5, 5, 1, 1);
        
        gridbag.setConstraints(xL, c);
        add(xL);
        
        c.gridwidth = GridBagConstraints.REMAINDER;
        gridbag.setConstraints(xT, c);
        add(xT);
        
        c.gridwidth = GridBagConstraints.RELATIVE;
        gridbag.setConstraints(yL, c);
        add(yL);
        
        c.gridwidth = GridBagConstraints.REMAINDER;
        gridbag.setConstraints(yT, c);
        add(yT);
        
        c.gridwidth = GridBagConstraints.RELATIVE;
        gridbag.setConstraints(defaultL, c);
        add(defaultL);
        
        c.gridwidth = GridBagConstraints.REMAINDER;
        gridbag.setConstraints(defaultC, c);
        add(defaultC);
        
        c.ipadx = 20;    c.ipady = 5;
        c.gridwidth = GridBagConstraints.RELATIVE;
        gridbag.setConstraints(okayB, c);
        add(okayB);
        
        c.gridwidth = GridBagConstraints.REMAINDER;
        gridbag.setConstraints(cancelB, c);
        add(cancelB);
        
        pack();
        setResizable(false);
        setLocation(frame.getLocation().x + frame.getSize().width/2 - getSize().width/2,
                    frame.getLocation().y + frame.getSize().height/2 - getSize().height/2);
    }
    
    public void actionPerformed(ActionEvent e) {
        if(e.getActionCommand().equals("done")) {
            try {
                xvalue = Integer.decode(xT.getText()).intValue();
                yvalue = Integer.decode(yT.getText()).intValue();
                selected = defaultC.getSelectedIndex();
            } catch(NumberFormatException ex) {
                System.err.println(ex.getMessage());
            }
            setVisible(false);
        }
        if(e.getActionCommand().equals("cancel")) {
            setVisible(false);
        }
    }
    
    public int getX() {
        return xvalue;
    }
    
    public int getY() {
        return yvalue;
    }
    
    public int getSelected() {
        return selected;
    }
}
