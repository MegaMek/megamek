/*
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

package megamek;

import java.awt.*;
import java.awt.event.*;
import java.io.*;

import megamek.common.*;
import megamek.client.*;
import megamek.client.bot.*;
import megamek.server.*;

public class MegaMek
    implements WindowListener, ActionListener
{
    public static String    VERSION = "0.26.2";
    public static long      TIMESTAMP = new File("timestamp").lastModified();
    
    public Frame            frame;
    
    public Client client;
    public Server server;
    
    /**
     * Contruct a MegaMek, and display the main menu in the
     * specified frame.
     */
    public MegaMek(Frame frame) {
        this.frame = frame;
        frame.addWindowListener(this);
    
        if(Settings.windowSizeHeight != 0) {
            frame.setLocation(Settings.windowPosX, Settings.windowPosY);
            frame.setSize(Settings.windowSizeWidth, Settings.windowSizeHeight);
        } else {
            frame.setSize(800, 600);
        }
        
        frame.setBackground(SystemColor.menu);
        frame.setForeground(SystemColor.menuText);
        
        showMainMenu();
        
        frame.setVisible(true);
    }
    
    /**
     * Display the main menu.
     */
    public void showMainMenu() {
        Button hostB, connectB, botB, editB;
        Label labVersion = new Label();
            
        frame.removeAll();
        
        labVersion.setText("MegaMek version " + VERSION);
        
        hostB = new Button("Host a New Game...");
        hostB.setActionCommand("game_host");
        hostB.addActionListener(this);
        
        connectB = new Button("Connect to a Game...");
        connectB.setActionCommand("game_connect");
        connectB.addActionListener(this);
        
        botB = new Button("Connect as a Bot...");
        botB.setActionCommand("game_botconnect");
        botB.addActionListener(this);

        editB = new Button("Map Editor");
        editB.setActionCommand("editor");
        editB.addActionListener(this);

        GridBagLayout gridbag = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();
        frame.setLayout(gridbag);
        
        c.fill = GridBagConstraints.NONE;
        c.weightx = 0.0;    c.weighty = 0.0;
        c.insets = new Insets(4, 4, 1, 1);
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.ipadx = 20;    c.ipady = 5;

        addBag(labVersion, gridbag, c);
        addBag(hostB, gridbag, c);
        addBag(connectB, gridbag, c);
        addBag(botB, gridbag, c);
        addBag(editB, gridbag, c);

        frame.validate();
    }
    
    /**
     * Display the board editor.
     */
    public void showEditor() {
        Game            game = new Game();
        BoardView1        bv;
        BoardEditor        be;
        
        frame.removeAll();
        
        bv = new BoardView1(game, frame);
        
        be = new BoardEditor(frame, game.board, bv);
        game.board.addBoardListener(be);
        be.setSize(120, 120);
        
        be.addKeyListener(bv);
        bv.addKeyListener(be);
        
        GridBagLayout gridbag = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();
        frame.setLayout(gridbag);
        
        c.fill = GridBagConstraints.NONE;
        c.weightx = 1.0;    c.weighty = 1.0;
        
        c.fill = GridBagConstraints.BOTH;
        addBag(bv, gridbag, c);
        c.fill = GridBagConstraints.VERTICAL;
        c.weightx = 0.0;
        c.gridwidth = GridBagConstraints.REMAINDER; 
        addBag(be, gridbag, c);
        
        frame.validate();
    }
    
    /**
     * Host a new game, connect to it, and then launch the 
     * chat lounge.
     */
    public void host() {
        HostDialog hd;
        
        hd = new HostDialog(frame);
        hd.show();
        // verify dialog data
        if(hd.name == null || hd.serverPass == null || hd.port == 0) {
            return;
        }
        // start server
        server = new Server(hd.serverPass, hd.port);
        // initialize game
        client = new Client(frame, hd.name);
        // verify connection
        if(!client.connect("localhost", hd.port)) {
            server = null;
            client = null;
            new AlertDialog(frame, "Host a Game", "Error: could not connect to local server.").show();
            return;
        }
        // wait for full connection
        client.retrieveServerInfo();
    }
    
    /**
     * Connect to to a game and then launch the chat lounge.
     */
    public void connect() {
        ConnectDialog cd;
        
        cd = new ConnectDialog(frame);
        cd.show();
        // verify dialog data
        if(cd.name == null || cd.serverAddr == null || cd.port == 0) {
            return;
        }
        // initialize game
        client = new Client(frame, cd.name);
        // verify connection
        if(!client.connect(cd.serverAddr, cd.port)) {
            server = null;
            client = null;
            new AlertDialog(frame, "Connect to a Game", "Error: could not connect.").show();
            return;
        }
        // wait for full connection
        client.retrieveServerInfo();
    }
    
    public void connectBot() {
        ConnectDialog cd;	

        cd = new ConnectDialog(frame);
        cd.show();
        // verify dialog data
        if(cd.name == null || cd.serverAddr == null || cd.port == 0) {
            return;
        }
        // initialize game
        client = BotFactory.getBot(BotFactory.TEST, frame, cd.name);
        //client = new BotClient(frame, cd.name);
         
	// verify connection
        if(!client.connect(cd.serverAddr, cd.port)) {
            server = null;
            client = null;
            new AlertDialog(frame, "Connect to a Game", "Error: could not connect.").show();
            return;
        }
        // wait for full connection
        client.retrieveServerInfo();
    }
  
    private void addBag(Component comp, GridBagLayout gridbag, GridBagConstraints c) {
        gridbag.setConstraints(comp, c);
        frame.add(comp);
    }
    
    private static void testDice() {
        // test RNG
        long rolls = 1000000;
        int sides = 14;
        long[] hits = new long[sides];
        
        System.out.println("testing dice, " + rolls + " rolls...");

        long start = System.currentTimeMillis();
        for (long i = 0; i < rolls; i++) {
            hits[megamek.common.Compute.d6(2)]++;
        }
        long end = System.currentTimeMillis();
        
        System.out.println("done testing dice in " + (end - start) + " ms.");
        for (int i = 0; i < sides; i++) {
            System.out.println("hits on " + i + " : " + hits[i] + "; probability = " + ((double)hits[i] / (double)rolls));
        }
    }
    
    public static void main(String[] args) {
        Settings.load();
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-testdice")) {
                testDice();
                return;
            }
            if (args[i].equals("-dedicated")) {
                new Server(Settings.lastServerPass, Settings.lastServerPort);
                return;
            }
        }
        
        Frame frame = new Frame("MegaMek");
        MegaMek mm = new MegaMek(frame);
    }
    
    //
    // ActionListener
    //
    public void actionPerformed(ActionEvent ev) {
        if(ev.getActionCommand().equalsIgnoreCase("editor")) {
            showEditor();
        }
        if(ev.getActionCommand().equalsIgnoreCase("game_host")) {
            host();
        }
        if(ev.getActionCommand().equalsIgnoreCase("game_connect")) {
            connect();
        }
        if(ev.getActionCommand().equalsIgnoreCase("game_botconnect")) {
            connectBot();
        }
    }
    
    //
    // WindowListener
    //    
    public void windowClosing(WindowEvent ev) {
        // feed last window position to settings
        Settings.windowPosX = frame.getLocation().x;
        Settings.windowPosY = frame.getLocation().y;
        Settings.windowSizeWidth = frame.getSize().width;
        Settings.windowSizeHeight = frame.getSize().height;
        
        // also minimap
        if (client != null && client.minimapW != null 
        && client.minimapW.getSize().width != 0 && client.minimapW.getSize().height != 0) {
            Settings.minimapPosX = client.minimapW.getLocation().x;
            Settings.minimapPosY = client.minimapW.getLocation().y;
            Settings.minimapSizeWidth = client.minimapW.getSize().width;
            Settings.minimapSizeHeight = client.minimapW.getSize().height;
        }
        
        // save settings
        Settings.save();
        
        // okay, exit program
        if (client != null) {
            client.die();
        } else {
            System.exit(0);
        }
    }
    public void windowOpened(WindowEvent ev) {
    }
    public void windowClosed(WindowEvent ev) {
    }
    public void windowDeiconified(WindowEvent ev) {
    }
    public void windowActivated(WindowEvent ev) {
    }
    public void windowIconified(WindowEvent ev) {
    }
    public void windowDeactivated(WindowEvent ev) {
    }
}

/**
 * here's a quick class for the host new game diaglogue box
 */
class HostDialog extends Dialog implements ActionListener {
    public String            name;
    public String            serverPass;
    public int                port;
    
    protected Label            yourNameL, serverPassL, portL;
    protected TextField        yourNameF, serverPassF, portF;
    protected Button        okayB, cancelB;
    
    public HostDialog(Frame frame) {
        super(frame, "Host New Game...", true);
        
        yourNameL = new Label("Your Name:", Label.RIGHT);
        serverPassL = new Label("Server Password:", Label.RIGHT);
        portL = new Label("Port:", Label.RIGHT);
        
        yourNameF = new TextField(Settings.lastPlayerName, 16);
        serverPassF = new TextField(Settings.lastServerPass, 16);
        portF = new TextField(Settings.lastServerPort + "", 4);
    
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
        c.insets = new Insets(5, 5, 5, 5);
        
        c.gridwidth = 1;
        c.anchor = GridBagConstraints.EAST;
        gridbag.setConstraints(yourNameL, c);
        add(yourNameL);
        
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.anchor = GridBagConstraints.WEST;
        gridbag.setConstraints(yourNameF, c);
        add(yourNameF);
        
        c.gridwidth = 1;
        c.anchor = GridBagConstraints.EAST;
        gridbag.setConstraints(serverPassL, c);
        add(serverPassL);
        
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.anchor = GridBagConstraints.WEST;
        gridbag.setConstraints(serverPassF, c);
        add(serverPassF);
        
        c.gridwidth = 1;
        c.anchor = GridBagConstraints.EAST;
        gridbag.setConstraints(portL, c);
        add(portL);
        
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.anchor = GridBagConstraints.WEST;
        gridbag.setConstraints(portF, c);
        add(portF);
        
        
        c.ipadx = 20;    c.ipady = 5;
        c.gridwidth = 1;
        c.anchor = GridBagConstraints.CENTER;
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
                name = yourNameF.getText();
                serverPass = serverPassF.getText();
                port = Integer.decode(portF.getText()).intValue();
            } catch(NumberFormatException ex) {
                System.err.println(ex.getMessage());
            }
            
            // update settings
            Settings.lastPlayerName = name;
            Settings.lastServerPass = serverPass;
            Settings.lastServerPort = port;

            setVisible(false);
        }
        if(e.getActionCommand().equals("cancel")) {
            setVisible(false);
        }
    }
}

/**
 * here's a quick class for the connect to game diaglogue box
 */
class ConnectDialog extends Dialog implements ActionListener {
    public String            name, serverAddr;
    public int                port;
    
    protected Label            yourNameL, serverAddrL, portL;
    protected TextField        yourNameF, serverAddrF, portF;
    protected Button        okayB, cancelB;
    
    public ConnectDialog(Frame frame) {
        super(frame, "Connect To Game...", true);
        
        yourNameL = new Label("Your Name:", Label.RIGHT);
        serverAddrL = new Label("Server Address:", Label.RIGHT);
        portL = new Label("Port:", Label.RIGHT);
        
        yourNameF = new TextField(Settings.lastPlayerName, 16);
        serverAddrF = new TextField(Settings.lastConnectAddr, 16);
        portF = new TextField(Settings.lastConnectPort + "", 4);
    
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
        c.insets = new Insets(5, 5, 5, 5);
        
        c.gridwidth = 1;
        c.anchor = GridBagConstraints.EAST;
        gridbag.setConstraints(yourNameL, c);
        add(yourNameL);
        
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.anchor = GridBagConstraints.WEST;
        gridbag.setConstraints(yourNameF, c);
        add(yourNameF);
        
        c.gridwidth = 1;
        c.anchor = GridBagConstraints.EAST;
        gridbag.setConstraints(serverAddrL, c);
        add(serverAddrL);
        
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.anchor = GridBagConstraints.WEST;
        gridbag.setConstraints(serverAddrF, c);
        add(serverAddrF);
        
        c.gridwidth = 1;
        c.anchor = GridBagConstraints.EAST;
        gridbag.setConstraints(portL, c);
        add(portL);
        
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.anchor = GridBagConstraints.WEST;
        gridbag.setConstraints(portF, c);
        add(portF);
        
        
        c.ipadx = 20;    c.ipady = 5;
        c.gridwidth = 1;
        c.anchor = GridBagConstraints.CENTER;
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
                name = yourNameF.getText();
                serverAddr = serverAddrF.getText();
                port = Integer.decode(portF.getText()).intValue();
                
            } catch(NumberFormatException ex) {
                System.err.println(ex.getMessage());
            }
            
            // update settings
            Settings.lastPlayerName = name;
            Settings.lastConnectAddr = serverAddr;
            Settings.lastConnectPort = port;
                
            setVisible(false);
        }
        if(e.getActionCommand().equals("cancel")) {
            setVisible(false);
        }
    }
}