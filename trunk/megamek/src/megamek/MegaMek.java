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

package megamek;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;

import megamek.common.*;
import megamek.client.*;
import megamek.client.util.widget.*;
import megamek.client.bot.*;
import megamek.server.*;

public class MegaMek
    implements ActionListener
{
    public static String    VERSION = "0.29.7";
    public static long      TIMESTAMP = new File("timestamp").lastModified();

    public Frame            frame;

    public Client			client;
    public Server			server;

    /**
     * Contruct a MegaMek, and display the main menu in the
     * specified frame.
     */
    public MegaMek() {
        this.frame = new Frame("MegaMek");
		frame.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				quit();
			}
		});

        frame.setBackground(SystemColor.menu);
        frame.setForeground(SystemColor.menuText);
        
        frame.setIconImage(frame.getToolkit().getImage("data/images/megamek-icon.gif"));

        showMainMenu();

        // echo some useful stuff
        System.out.println("Starting MegaMek v" + VERSION + " ...");
        System.out.println("Timestamp " + new Date(TIMESTAMP).toString());
        System.out.println("Java vendor " + System.getProperty("java.vendor"));
        System.out.println("Java version " + System.getProperty("java.version"));

		// set visible on middle of screen
		Dimension screenSize = frame.getToolkit().getScreenSize();
		frame.pack();
        frame.setLocation(
            screenSize.width / 2 - frame.getSize().width / 2,
            screenSize.height / 2 - frame.getSize().height / 2);
        frame.setVisible(true);
    }

    /**
     * Display the main menu.
     */
    public void showMainMenu() {
        Button hostB, connectB, botB, editB, scenB, loadB, quitB;
        Label labVersion = new Label();

        labVersion.setText("MegaMek version " + VERSION);

        hostB = new Button("Host a New Game...");
        hostB.setActionCommand("game_host");
        hostB.addActionListener(this);

        scenB = new Button("Host a Scenario...");
        scenB.setActionCommand("game_scen");
        scenB.addActionListener(this);

        loadB = new Button("Host a Saved Game...");
        loadB.setActionCommand("game_load");
        loadB.addActionListener(this);

        connectB = new Button("Connect to a Game...");
        connectB.setActionCommand("game_connect");
        connectB.addActionListener(this);

        botB = new Button("Connect as a Bot...");
        botB.setActionCommand("game_botconnect");
        botB.addActionListener(this);

		editB = new Button("Map Editor");
		editB.setActionCommand("editor");
		editB.addActionListener(this);

		quitB = new Button("Quit");
		quitB.setActionCommand("quit");
		quitB.addActionListener(this);
		
		// initialize splash image panel
		BufferedPanel panTitle = new BufferedPanel();
		Image imgSplash = panTitle.getToolkit().getImage("data/images/megamek-splash.gif");
		BackGroundDrawer bgdTitle = new BackGroundDrawer(imgSplash);
		panTitle.addBgDrawer(bgdTitle);
		panTitle.setPreferredSize(180, 250);
		
		// layout
        GridBagLayout gridbag = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();
        frame.setLayout(gridbag);

        c.fill = GridBagConstraints.BOTH;
        c.anchor = GridBagConstraints.WEST;
        c.weightx = 0.0;    c.weighty = 0.0;
        c.insets = new Insets(4, 4, 1, 1);
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.ipadx = 10;    c.ipady = 5;
        c.gridx = 0;

		c.gridwidth = 1;
		c.gridheight = 8;
		addBag(panTitle, gridbag, c);
		
		c.gridwidth = GridBagConstraints.REMAINDER;
		c.gridx = 1;
		c.gridheight = 1;
		c.fill = GridBagConstraints.HORIZONTAL;

        c.gridy = 0;
		addBag(labVersion, gridbag, c);
		c.gridy++;
        addBag(hostB, gridbag, c);
		c.gridy++;
		addBag(loadB, gridbag, c);
        c.gridy++;
        addBag(scenB, gridbag, c);
        c.gridy++;
        addBag(connectB, gridbag, c);
        c.gridy++;
        addBag(botB, gridbag, c);
		c.gridy++;
		addBag(editB, gridbag, c);
		c.gridy++;
		addBag(quitB, gridbag, c);

		// wait for splash image to load completely before returning
		MediaTracker tracker = new MediaTracker(frame);
		tracker.addImage(imgSplash, 0);
		try {
			tracker.waitForID(0);
		} catch (InterruptedException e) {
			;
		}
		
		frame.validate();
    }

    /**
     * Display the board editor.
     */
    public void showEditor() {
    	BoardEditor editor = new BoardEditor();
    	launch(editor.getFrame());
    }

    /**
     * Start instances of both the client and the server.
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
        // initialize client
        client = new Client(hd.name, "localhost", hd.port);
		launch(client.getFrame());

        server.getGame().getOptions().loadOptions(client, hd.serverPass);
    }

    public void loadGame() {
        FileDialog fd =
            new FileDialog(frame, "Select saved game...", FileDialog.LOAD);
        fd.setDirectory(".");
        fd.show();
        if (fd.getFile() == null) {
            return;
        }

        HostDialog hd = new HostDialog(frame);
        hd.show();
        if (hd.name == null || hd.serverPass == null || hd.port == 0) {
            return;
        }
        server = new Server(hd.serverPass, hd.port);
        if (!server.loadGame(new File(fd.getDirectory(), fd.getFile()))) {
            new AlertDialog(frame, "Load a Game", "Error: unable to load game file.").show();
            server = null;
            return;
        }
        client = new Client(hd.name, "localhost", hd.port);
		launch(client.getFrame());
    }

    /**
     * Host a game constructed from a scenario file
     */
    public void scenario() {
        FileDialog fd = new FileDialog(frame,
                "Select a scenario file...",
                FileDialog.LOAD);
        fd.setDirectory("data" + File.separatorChar + "scenarios");

        // the filter doesn't seem to do anything in windows.  oh well
        FilenameFilter ff = new FilenameFilter() {
            public boolean accept(File f, String s) {
                return s.endsWith(".mms");
            }
        };
        fd.setFilenameFilter(ff);
        fd.show();
        if (fd.getFile() == null) {
            return;
        }
        ScenarioLoader sl = new ScenarioLoader(new File(fd.getDirectory(), fd.getFile()));
        Game g = null;
        try {
            g = sl.createGame();
        } catch (Exception e) {
            new AlertDialog(frame, "Host Scenario", "Error: " + e.getMessage()).show();
            return;
        }

        // get player types and colors set
        Player[] pa = new Player[g.getPlayersVector().size()];
        g.getPlayersVector().copyInto(pa);

        ScenarioDialog sd = new ScenarioDialog(frame, pa);
        sd.show();
        if (!sd.bSet) {
            return;
        }

        // host with the scenario.  essentially copied from host()
        HostDialog hd = new HostDialog(frame);
        hd.yourNameF.setText(sd.localName);
        hd.show();
        // verify dialog data
        if(hd.name == null || hd.serverPass == null || hd.port == 0) {
            return;
        }

        // start server
        server = new Server(hd.serverPass, hd.port);
        server.setGame(g);

        // apply any scenario damage
        sl.applyDamage(server);

        if (sd.localName != "") {
            // initialize game
            client = new Client(hd.name, "localhost", hd.port);
			launch(client.getFrame());

            server.getGame().getOptions().loadOptions(client, hd.serverPass);
            
            // popup options dialog
            client.getGameOptionsDialog().update(client.game.getOptions());
            client.getGameOptionsDialog().show();
        }



        // setup any bots
        for (int x = 0; x < pa.length; x++) {
            if (sd.playerTypes[x] == ScenarioDialog.T_BOT) {
                Client c = BotFactory.getBot(BotFactory.TEST, pa[x].getName());
                c.connect("localhost", hd.port);
                c.retrieveServerInfo();
                ((BotClientWrapper)c).initialize();
                //f.hide();
            }
        }
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
        client = new Client(cd.name, cd.serverAddr, cd.port);
		launch(client.getFrame());
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
        client = BotFactory.getBot(BotFactory.TEST, cd.name);
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
        
        launch(client.getFrame());
    }

    private void addBag(Component comp, GridBagLayout gridbag, GridBagConstraints c) {
        gridbag.setConstraints(comp, c);
        frame.add(comp);
    }
    
    /**
     * Called when the quit buttons is pressed or the main menu is closed.
     */
    private void quit() {
    	System.exit(0);
    }
    
    /**
     * Hides this window for later.  Listens to the frame until it closes,
     * then calls unlaunch().
     */
    private void launch(Frame launched) {
    	// listen to new frame
        launched.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                unlaunch();
            }
        });
        // hide menu frame
        frame.setVisible(false);
    }
    
    /**
     * Un-hides the main menu and tries to clean up the client or server.
     */
    private void unlaunch() {
		// clean up server, if we have one
		if (server != null) {
			server.die();
			server = null;
		}
		// show menu frame
		frame.setVisible(true);
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

        int[][] pairs = new int[7][7];
        System.out.println("testing streaks, " + rolls + " rolls...");

        int nLastLastRoll = 0, nRoll = 0;
        for (long i = 0; i < rolls; i++) {
            nRoll = megamek.common.Compute.d6();
            pairs[nLastLastRoll][nRoll]++;
            nLastLastRoll = nRoll;
        }
        for (int x = 0; x < pairs.length; x++) {
            for (int y = 0; y < pairs[x].length; y++) {
                System.out.println(x + "," + y + ": " + pairs[x][y]);
            }
        }
        // odd, but necessary
        System.out.flush();

    }

    public static void main(String[] args) {

        String logFileName = "MegaMek.log";
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-testdice")) {
                testDice();
                return;
            }
            else if (args[i].equals("-dedicated")) {
                Settings.load();
                new Server(Settings.lastServerPass, Settings.lastServerPort);
                return;
            }
            else if (args[i].equals("-log")) {
                // Next argument is the log file's name.
                i++;
                if ( i >= args.length || args[i].equals("none")
                     || args[i].equals("off") ) {
                    logFileName = null;
                } else {
                    logFileName = args[i];
                }
            }
            else if ( args[i].equals("-testxml") ) {
                // Next argument is the log file's name.
                i++;
                if ( i >= args.length ) {
                    System.err.println( "The '-testxml' flag requires a file name." );
                } else {
                    new TinyXMLTest( "xml", args[i] );
                }
                return;
            }
        }

        // Redirect output to logfiles, unless turned off.
        if ( logFileName != null ) {
            try {
                System.out.println("Redirecting output to " + logFileName);
                PrintStream ps = new PrintStream(new BufferedOutputStream(
                                 new FileOutputStream(logFileName), 64));
                System.setOut(ps);
                System.setErr(ps);
            } catch (Exception e) {
                System.err.println("Unable to redirect output to " +
                                   logFileName);
                e.printStackTrace();
            }
        } // End log-to-file

        // kick off a RNG check as quick as possible, since init can take a while
        megamek.common.Compute.d6();
        Settings.load();
        new MegaMek();
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
        if(ev.getActionCommand().equalsIgnoreCase("game_scen")) {
            scenario();
        }
        if(ev.getActionCommand().equalsIgnoreCase("game_connect")) {
            connect();
        }
        if(ev.getActionCommand().equalsIgnoreCase("game_botconnect")) {
            connectBot();
        }
		if(ev.getActionCommand().equalsIgnoreCase("game_load")) {
			loadGame();
		}
		if(ev.getActionCommand().equalsIgnoreCase("quit")) {
			quit();
		}
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
        yourNameF.addActionListener(this);
        serverPassF = new TextField(Settings.lastServerPass, 16);
        serverPassF.addActionListener(this);
        portF = new TextField(Settings.lastServerPort + "", 4);
        portF.addActionListener(this);

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
        if(!e.getActionCommand().equals("cancel")) {
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
        }
        setVisible(false);
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
        yourNameF.addActionListener(this);
        serverAddrF = new TextField(Settings.lastConnectAddr, 16);
        serverAddrF.addActionListener(this);
        portF = new TextField(Settings.lastConnectPort + "", 4);
        portF.addActionListener(this);

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
        if(!e.getActionCommand().equals("cancel")) {
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
        }
        setVisible(false);
    }
}

/**
 * Allow a user to set types and colors for scenario players
 */
class ScenarioDialog extends Dialog implements ActionListener
{
    public static final int T_ME = 0;
    public static final int T_HUMAN = 1;
    public static final int T_BOT = 2;

    private Player[] m_players;
    private Label[] m_labels;
    private Choice[] m_typeChoices;
    private Choice[] m_colorChoices;
    private Choice[] m_camoChoices;
    private Frame m_frame;

    public boolean bSet = false;
    public int[] playerTypes;
    public String localName = "";

    public ScenarioDialog(Frame frame, Player[] pa)
    {
        super(frame, "Set Scenario Players...", true);
        m_frame = frame;
        m_players = pa;
        m_labels = new Label[pa.length];
        m_typeChoices = new Choice[pa.length];
        m_colorChoices = new Choice[pa.length];
        m_camoChoices = new Choice[pa.length];
        playerTypes = new int[pa.length];

        Vector camoList = ChatLounge.getCamoList();
        
        for (int x = 0; x < pa.length; x++) {
            m_labels[x] = new Label(pa[x].getName(), Label.LEFT);
            m_typeChoices[x] = new Choice();
            m_typeChoices[x].add("Me");
            m_typeChoices[x].add("Other Human");
            m_typeChoices[x].add("Bot");
            m_colorChoices[x] = new Choice();
            for (int i = 0; i < Player.colorNames.length; i++) {
                m_colorChoices[x].add(Player.colorNames[i]);
            }
            m_colorChoices[x].select(x);
            
            m_camoChoices[x] = new Choice();
            for ( int i = 0; i < camoList.size(); i++ ) {
              m_camoChoices[x].add((String)camoList.elementAt(i));
            }
            m_camoChoices[x].select(0);
        }

        setLayout(new BorderLayout());
        Panel choicePanel = new Panel();
        choicePanel.setLayout(new GridLayout(pa.length + 1, 3));
        choicePanel.add(new Label("Player"));
        choicePanel.add(new Label("Type"));
        choicePanel.add(new Label("Color"));
        choicePanel.add(new Label("Camo"));
        for (int x = 0; x < pa.length; x++) {
            choicePanel.add(m_labels[x]);
            choicePanel.add(m_typeChoices[x]);
            choicePanel.add(m_colorChoices[x]);
            choicePanel.add(m_camoChoices[x]);
        }
        add(choicePanel, BorderLayout.CENTER);

        Panel butPanel = new Panel();
        butPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
        Button bOkay = new Button("Okay");
        bOkay.setActionCommand("okay");
        bOkay.addActionListener(this);
        Button bCancel = new Button("Cancel");
        bCancel.setActionCommand("cancel");
        bCancel.addActionListener(this);
        butPanel.add(bOkay);
        butPanel.add(bCancel);
        add(butPanel, BorderLayout.SOUTH);
        pack();
        setResizable(false);
        setLocation(frame.getLocation().x + frame.getSize().width/2 - getSize().width/2,
                    frame.getLocation().y + frame.getSize().height/2 - getSize().height/2);
    }

    public void actionPerformed(ActionEvent e)
    {
        if (e.getActionCommand().equals("okay")) {
            boolean bMeSet = false;
            for (int x = 0; x < m_players.length; x++) {
                playerTypes[x] = m_typeChoices[x].getSelectedIndex();
                if (playerTypes[x] == T_ME) {
                    if (bMeSet) {
                        new AlertDialog(m_frame, "Scenario Error",
                                "Only one faction can be set to 'Me'.").show();
                        return;
                    }
                    bMeSet = true;
                    localName = m_players[x].getName();
                }
                m_players[x].setColorIndex(m_colorChoices[x].getSelectedIndex());
                m_players[x].setCamoFileName(m_camoChoices[x].getSelectedItem());
            }
            bSet = true;
            setVisible(false);
        }
        else if (e.getActionCommand().equals("cancel")) {
            setVisible(false);
        }
    }
}
