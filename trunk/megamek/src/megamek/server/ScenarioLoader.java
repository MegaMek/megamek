/*
 * MegaMek - Copyright (C) 2000-2002 Ben Mazur (bmazur@sev.org)
 * ScenarioLoader - Copyright (C) 2002 Josh Yockey
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

package megamek.server;

import java.io.File;
import java.io.FileInputStream;
import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.Vector;
import megamek.common.*;

public class ScenarioLoader 
{
    private File m_scenFile;
    private static final String[] FACING = 
            { "N", "NE", "SE", "S", "SW", "NW" };
    private static final String[] DIRS = 
            { "NW", "N", "NE", "E", "SE", "S", "SW", "W", "C" };
    private boolean[] m_baTakenDirs = new boolean[DIRS.length];
    
            
    
    public ScenarioLoader(File f)
    {
        m_scenFile = f;
    }
    
    public Game createGame()
        throws Exception
    {
        System.out.println("Loading scenario from " + m_scenFile);
        Properties p = loadProperties();
        
        String sCheck = p.getProperty("MMSVersion");
        if (sCheck == null) {
            throw new Exception("Not a valid MMS file.  No MMSVersion.");
        }
        
        Game g = new Game();
        
        // build the board
        g.board = createBoard(p);
        
        // build the faction players
        Player[] players = createPlayers(p);
        for (int x = 0; x < players.length; x++) {
            g.addPlayer(x, players[x]);
        }
        
        // build the entities
        int nIndex = 0;
        Coords center = new Coords(g.board.width / 2, g.board.height / 2);
        for (int x = 0; x < players.length; x++) {
            Entity[] entities = buildFactionEntities(p, players[x].getName());
            for (int y = 0; y < entities.length; y++) {
                entities[y].setOwner(players[x]);
                entities[y].setId(nIndex++);
                g.addEntity(entities[y].getId(), entities[y]);
                // place entity if not specified
                if (entities[y].getPosition() == null || entities[y].getFacing() == -1) {
                    placeEntity(entities[y], g);
                }
            }
        }
        
        // game's ready
        g.getOptions().initialize();
        g.phase = Game.PHASE_INITIATIVE;
        return g;
    }
    
    private void placeEntity(Entity e, Game g)
    {
        if (e.getPosition() == null) {
            // find a position based on the faction's starting position
            Coords c = getStartingCoords(g, e.getOwner().getStartingPos());
            e.setPosition(getCoordsAround(c, g));
        }
        
        if (e.getFacing() == -1) {
            // face towards the middle
            int nDir = e.getPosition().direction(getStartingCoords(g, 8));
            e.setFacing(nDir);
            e.setSecondaryFacing(nDir);
        }
    }
    
    private Coords getCoordsAround(Coords c, Game g)
    {
        // check the requested coords
        if (g.board.contains(c) && g.getEntity(c) == null) {
            return c;
        }
        
        // check the surrounding coords
        for (int x = 0; x < 6; x++) {
            Coords c2 = c.translated(x);
            if (g.board.contains(c2) && g.getEntity(c2) == null) {
                return c2;
            }
        }
        
        // recurse in a random direction
        return getCoordsAround(c.translated(Compute.random.nextInt(6)), g);
    }

    // taken from Server.java, with Center added    
    private Coords getStartingCoords(Game game, int nDir)
    {
        switch (nDir) {
            default :
            case 0 :
                return new Coords(1, 1);
            case 1 :
                return new Coords(game.board.width / 2, 1);
            case 2 :
                return new Coords(game.board.width - 2, 1);
            case 3 :
                return new Coords(game.board.width - 2, game.board.height / 2);
            case 4 :
                return new Coords(game.board.width - 2, game.board.height - 2);
            case 5 :
                return new Coords(game.board.width / 2, game.board.height - 2);
            case 6 :
                return new Coords(1, game.board.height - 2);
            case 7 :
                return new Coords(1, game.board.height / 2);
            case 8 :
                return new Coords(game.board.width / 2, game.board.height / 2);
        }
    }
    
    private Entity[] buildFactionEntities(Properties p, String sFaction)
        throws Exception
    {
        Vector vEntities = new Vector();
        for (int i = 1; true; i++) {
            String s = p.getProperty("Unit_" + sFaction + "_" + i);
            if (s == null) {
                // prepare and return array
                Entity[] out = new Entity[vEntities.size()];
                vEntities.copyInto(out);
                return out;
            }
            else {
                vEntities.addElement(parseEntityLine(s));
            }
        }        
    }
    
    private Entity parseEntityLine(String s)
        throws Exception
    {
        try {
            StringTokenizer st = new StringTokenizer(s, ",");
            String sRef = st.nextToken();
            MechSummary ms = MechSummaryCache.getInstance().getMech(sRef);
            if (ms == null) {
                throw new Exception("Scenario requires missing entity: " + sRef);
            }
            System.out.println("Loading " + ms.getRef());
            Entity e = new MechFileParser(ms.getSourceFile(), ms.getEntryName()).getEntity();
            e.crew = new Pilot(st.nextToken(), Integer.parseInt(st.nextToken()), 
                    Integer.parseInt(st.nextToken()));
            int nFacing = -1;
            if (st.hasMoreTokens()) {
                // facing specified
                nFacing = findIndex(FACING, st.nextToken());
            }
            e.setFacing(nFacing);
            e.setSecondaryFacing(nFacing);
            if (st.hasMoreTokens()) {
                // coords specified
                e.setPosition(new Coords(Integer.parseInt(st.nextToken()), 
                        Integer.parseInt(st.nextToken())));
            }
            else {
                // explicitly set to null so a later process can detect it
                e.setPosition(null);
            }
            return e;
        } catch (NumberFormatException e) {
            e.printStackTrace();
            throw new Exception("Unparseable entity line: " + s);
        } catch (NoSuchElementException e) {
            e.printStackTrace();
            throw new Exception("Unparseable entity line: " + s);
        }
    }
    
    private int findIndex(String[] sa, String s)
    {
        for (int x = 0; x < sa.length; x++) {
            if (sa[x].equals(s)) {
                return x;
            }
        }
        return -1;
    }
        
    
    
    private Player[] createPlayers(Properties p)
        throws Exception
    {
        String sFactions = p.getProperty("Factions");
        if (sFactions == null) {
            throw new Exception("Not a valid MMS file.  No Factions");
        }
        
        StringTokenizer st = new StringTokenizer(sFactions, ",");
        Player[] out = new Player[st.countTokens()];
        for (int x = 0; x < out.length; x++) {
            out[x] = new Player(x, st.nextToken());
               
            // scenario players start out as ghosts to be logged into
            out[x].setGhost(true);
            
            // check for initial placement
            String s = p.getProperty("Location_" + out[x].getName());
            
            // default to random
            if (s == null) {
                s = "R";
            }
            
            int nDir = -1;
            
            if (!s.equals("R")) {
                nDir = findIndex(DIRS, s);
            }
            
            // if it's not set by now, make it random
            if (nDir == -1) {
                nDir = getRandomStartingPos();
            }
            
            out[x].setStartingPos(nDir);
            m_baTakenDirs[nDir] = true;
        }
        
        return out;
    }
    
    private int getRandomStartingPos()
    {
        // check to see if all directions are taken
        boolean bAllTaken = true;
        
        for (int x = 0; x < m_baTakenDirs.length; x++) {
            if (!m_baTakenDirs[x]) {
                bAllTaken = false;
                break;
            }
        }
        
        // essentially loop forever, but fall out in case of weirdness
        for (int x = 0; x < 200; x++) {
            int nRand = Compute.random.nextInt(DIRS.length);
            if (bAllTaken || !m_baTakenDirs[nRand]) {
                m_baTakenDirs[nRand] = true;
                return nRand;
            }
        }
        
        // should never get here
        System.out.println("Warning! getRandomStartingPos looped 200 times, but not all taken");
        return Compute.random.nextInt(DIRS.length);
    }

    
    /**
     * Load board files and create the megaboard.
     * For now I have to make the huge assumption that all boards are the default
     * size (16x17), because there's currently no way to specify the desired boardheight
     * in the scenario file
     */
    private Board createBoard(Properties p)
        throws Exception
    {
        int nWidth = 1, nHeight = 1;
        if (p.getProperty("BoardWidth") == null) {
            System.out.println("No board width specified.  Using 1");
        }
        else {
            nWidth = Integer.parseInt(p.getProperty("BoardWidth"));
        }
        
        if (p.getProperty("BoardHeight") == null) {
            System.out.println("No board height specified.  Using 1");
        }
        else {
            nHeight = Integer.parseInt(p.getProperty("BoardHeight"));
        }
        
        System.out.println("Constructing " + nWidth + " by " + nHeight + " board.");
        
        // load available boards
        // basically copied from Server.java.  Should get moved somewhere neutral
        Vector vBoards = new Vector();
        File boardDir = new File("data/boards");

        String[] fileList = boardDir.list();
        for (int i = 0; i < fileList.length; i++) {
            if (fileList[i].endsWith(".board")) {
                vBoards.addElement(fileList[i].substring(0, fileList[i].lastIndexOf(".board")));
            }
        }
        
        Board[] ba = new Board[nWidth * nHeight];
        StringTokenizer st = new StringTokenizer(p.getProperty("Maps"), ",");
        for (int x = 0; x < nWidth; x++) {
            for (int y = 0; y < nHeight; y++) {
                int n = y * nWidth + x;
                String sBoard = "RANDOM";
                if (st.hasMoreTokens()) {
                    sBoard = st.nextToken();
                }
                System.out.println("(" + x + "," + y + ")" + sBoard);
                
                String sBoardFile;
                if (sBoard.equals("RANDOM")) {
                    sBoardFile = (String)(vBoards.elementAt(Compute.random.nextInt(vBoards.size()))) + ".board";
                }
                else {
                    sBoardFile = sBoard + ".board";
                }
                File fBoard = new File(boardDir, sBoardFile);
                if (!fBoard.exists()) {
                    throw new Exception("Scenario requires nonexistant board: " + sBoard);
                }
                ba[n] = new Board();
                ba[n].load(sBoardFile);
            }
        }
        
        // construct the big board
        Board out = new Board();
        out.combine(16, 17, nWidth, nHeight, ba);
        return out;        
    }
    
    private Properties loadProperties()
        throws Exception
    {
        Properties p = new Properties();
        FileInputStream fis = new FileInputStream(m_scenFile);
        p.load(fis);
        fis.close();
        return p;
    }
    
    public static void main(String[] saArgs)
        throws Exception
    {
        ScenarioLoader sl = new ScenarioLoader(new File(saArgs[0]));
        Game g = sl.createGame();
        System.out.println("Successfully loaded.");
    }
}
