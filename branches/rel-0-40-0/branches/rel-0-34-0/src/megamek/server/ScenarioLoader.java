/*
 * MegaMek - Copyright (C) 2003, 2004, 2005 Ben Mazur (bmazur@sev.org)
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
import java.util.Enumeration;
import java.util.HashMap;
import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.Vector;

import megamek.common.AmmoType;
import megamek.common.BattleArmor;
import megamek.common.Board;
import megamek.common.Compute;
import megamek.common.Coords;
import megamek.common.CriticalSlot;
import megamek.common.Entity;
import megamek.common.Game;
import megamek.common.HitData;
import megamek.common.IArmorState;
import megamek.common.IBoard;
import megamek.common.IGame;
import megamek.common.IStartingPositions;
import megamek.common.Infantry;
import megamek.common.MapSettings;
import megamek.common.Mech;
import megamek.common.MechFileParser;
import megamek.common.MechSummary;
import megamek.common.MechSummaryCache;
import megamek.common.Mounted;
import megamek.common.Pilot;
import megamek.common.Player;
import megamek.common.Protomech;
import megamek.common.Tank;
import megamek.common.ToHitData;
import megamek.common.loaders.EntityLoadingException;
import megamek.common.options.IOption;
import megamek.common.util.BoardUtilities;

public class ScenarioLoader {
    private File m_scenFile;
    // copied from ChatLounge.java
    private Vector<DamagePlan> m_vDamagePlans = new Vector<DamagePlan>();

    // Used to store Crit Hits
    private Vector<CritHitPlan> m_vCritHitPlans = new Vector<CritHitPlan>();

    // Used to set ammo Spec Ammounts
    private Vector<SetAmmoPlan> m_vSetAmmoTo = new Vector<SetAmmoPlan>();

    public ScenarioLoader(File f) {
        m_scenFile = f;
    }

    /**
     * The damage procedures are built into a server object, so we delay dealing
     * the random damage until a server is made available to us.
     */
    public void applyDamage(Server s) {
        for (int x = 0, n = m_vDamagePlans.size(); x < n; x++) {
            DamagePlan dp = m_vDamagePlans.elementAt(x);
            System.out
                    .println("Applying damage to " + dp.entity.getShortName());
            for (int y = 0; y < dp.nBlocks; y++) {
                HitData hit = dp.entity.rollHitLocation(ToHitData.HIT_NORMAL,
                        ToHitData.SIDE_FRONT);
                System.out.println("[s.damageEntity(dp.entity, hit, 5)]");
                s.damageEntity(dp.entity, hit, 5);
            }

            // Apply Spec Dammage
            for (int dpspot = 0, dpcount = dp.specificDammage.size(); dpspot < dpcount; dpspot++) {
                // Get the SpecDam
                SpecDam sd = dp.specificDammage.elementAt(dpspot);

                if (dp.entity.locations() <= sd.loc) // Make sure the the
                                                        // location is valid
                    System.out
                            .println("\tInvalid Location Specified " + sd.loc);
                else {
                    // Infantry only take dammage to "internal"
                    if (sd.internal
                            || ((dp.entity instanceof Infantry) && !(dp.entity instanceof BattleArmor))) {
                        if (dp.entity.getOInternal(sd.loc) > sd.setArmorTo) {
                            dp.entity.setInternal(sd.setArmorTo, sd.loc);
                            System.out
                                    .println("\tSet Armor Value for (Internal "
                                            + dp.entity.getLocationName(sd.loc)
                                            + ") To " + sd.setArmorTo);
                            if (sd.setArmorTo == 0) {
                                // Mark destroy if internal armor is set to zero
                                System.out.println("\tSection Destoyed "
                                        + dp.entity.getLocationName(sd.loc));
                                dp.entity.destroyLocation(sd.loc);
                            }
                        }
                    } else {
                        if (sd.rear && dp.entity.hasRearArmor(sd.loc)) {
                            if (dp.entity.getOArmor(sd.loc, true) > sd.setArmorTo) {
                                System.out
                                        .println("\tSet Armor Value for (Rear "
                                                + dp.entity
                                                        .getLocationName(sd.loc)
                                                + ") To " + sd.setArmorTo);
                                dp.entity.setArmor(sd.setArmorTo, sd.loc, true);
                            }
                        } else {
                            if (dp.entity.getOArmor(sd.loc, false) > sd.setArmorTo) {
                                System.out.println("\tSet Armor Value for ("
                                        + dp.entity.getLocationName(sd.loc)
                                        + ") To " + sd.setArmorTo);

                                // Battle Armor Handled Differently
                                // If armor set to Zero kill the Armor sport
                                // which represents
                                // one member of the squad
                                if (dp.entity instanceof BattleArmor) {
                                    if (sd.setArmorTo == 0) {
                                        dp.entity.setArmor(
                                                IArmorState.ARMOR_DOOMED,
                                                sd.loc, false);
                                        dp.entity.setInternal(
                                                IArmorState.ARMOR_DOOMED,
                                                sd.loc);
                                    } else {
                                        // For some reason setting armor to 1
                                        // will result in 2 armor points
                                        // left on the GUI Dont know why but
                                        // adjust here!
                                        dp.entity.setArmor(sd.setArmorTo - 1,
                                                sd.loc);
                                    }
                                } else {
                                    dp.entity.setArmor(sd.setArmorTo, sd.loc);
                                }
                            }
                        }
                    }
                }
            }
        }

        // Loop throught Crit Hits
        for (int chSpot = 0, chCount = m_vCritHitPlans.size(); chSpot < chCount; chSpot++) {
            CritHitPlan chp = m_vCritHitPlans.elementAt(chSpot);
            System.out.print("Applying Critical Hits to "
                    + chp.entity.getShortName());

            for (int chpspot = 0, chpcount = chp.critHits.size(); chpspot < chpcount; chpspot++) {
                // Get the ScritHit
                CritHit ch = chp.critHits.elementAt(chpspot);

                // Apply a critical hit to the indicated slot.
                if (chp.entity.locations() <= ch.loc)
                    System.out.println("\n\tInvalid Location Specified "
                            + ch.loc);
                else {
                    // Make sure that we have crit spot to hit
                    if (chp.entity instanceof Mech
                            || chp.entity instanceof Protomech) {

                        // Is this a torso weapon slot?
                        CriticalSlot cs = null;
                        if (chp.entity instanceof Protomech
                                && Protomech.LOC_TORSO == ch.loc
                                && (Protomech.SYSTEM_TORSO_WEAPON_A == ch.slot || Protomech.SYSTEM_TORSO_WEAPON_B == ch.slot)) {
                            cs = new CriticalSlot(CriticalSlot.TYPE_SYSTEM,
                                    ch.slot);
                        }
                        // Is this a valid slot number?
                        else if (ch.slot < 0
                                || ch.slot > chp.entity
                                        .getNumberOfCriticals(ch.loc)) {
                            System.out.println("\n\tInvalid Slot Specified "
                                    + ch.loc + ":" + (ch.slot + 1));
                        }
                        // Get the slot from the entity.
                        else {
                            cs = chp.entity.getCritical(ch.loc, ch.slot);
                        }

                        // Ignore invalid, unhittable, and damaged slots.
                        if (null == cs || !cs.isHittable()) {
                            System.out.println("\n\tSlot not hittable "
                                    + ch.loc + ":" + (ch.slot + 1));
                        } else {
                            System.out
                                    .print("[s.applyCriticalHit(chp.entity, ch.loc, cs, false)]");
                            s.applyCriticalHit(chp.entity, ch.loc, cs, false);
                        }
                    }
                    // Handle Tanks differently.
                    else if (chp.entity instanceof Tank) {
                        if (ch.slot < 0 || ch.slot >= 6) {
                            System.out.println("\n\tInvalid Slot Specified "
                                    + ch.loc + ":" + (ch.slot + 1));
                        } else {
                            CriticalSlot cs = new CriticalSlot(
                                    CriticalSlot.TYPE_SYSTEM, ch.slot + 1);
                            System.out
                                    .print("[s.applyCriticalHit(chp.entity, ch.loc, cs, false)]");
                            s.applyCriticalHit(chp.entity, Entity.NONE, cs,
                                    false);
                        }

                    } // End have-tank

                } // End have-valid-location

            } // Handle the next critical hit

            // Print a line between hit plans.
            System.out.println();

        } // Handle the next critical hit plan

        // Loop throught Set Ammo To
        for (int saSpot = 0, saCount = m_vSetAmmoTo.size(); saSpot < saCount; saSpot++) {
            SetAmmoPlan sap = m_vSetAmmoTo.elementAt(saSpot);
            System.out.println("Applying Ammo Adjustment to "
                    + sap.entity.getShortName());

            for (int sapSpot = 0, sapCount = sap.ammoSetTo.size(); sapSpot < sapCount; sapSpot++) {
                // Get the ScritHit
                SetAmmoTo sa = sap.ammoSetTo.elementAt(sapSpot);

                // Only can be done against Mechs
                if (sap.entity instanceof Mech) {
                    if (sa.slot < sap.entity.getNumberOfCriticals(sa.loc)) {
                        // Get the piece of Eqipment and Check to make sure it
                        // is
                        // a ammo item then set its amount!
                        CriticalSlot cs = sap.entity.getCritical(sa.loc,
                                sa.slot);
                        if (!(cs == null)) {
                            Mounted ammo = sap.entity.getEquipment(sap.entity
                                    .getCritical(sa.loc, sa.slot).getIndex());
                            if (ammo.getType() instanceof AmmoType) {
                                // Also make sure we dont exceed the max aloud
                                ammo.setShotsLeft(Math.min(sa.setAmmoTo, ammo
                                        .getShotsLeft()));
                            }
                        }
                    }
                }
            }
        }
    }

    public IGame createGame() throws Exception {
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
        for (int x = 0; x < players.length; x++) {
            Entity[] entities = buildFactionEntities(p, players[x]);
            for (int y = 0; y < entities.length; y++) {
                entities[y].setOwner(players[x]);
                entities[y].setId(nIndex++);
                g.addEntity(entities[y].getId(), entities[y]);
            }
        }
        // game's ready
        g.getOptions().initialize();
        String optionFile = p.getProperty("GameOptionsFile");
        if (optionFile == null) {
            g.getOptions().loadOptions();
        } else {
            g.getOptions().loadOptions(
                    new File(m_scenFile.getParentFile(), optionFile));
        }

        // set wind
        g.getPlanetaryConditions().determineWind();

        // Set up the teams (for initiative)
        g.setupTeams();

        g.setPhase(IGame.Phase.PHASE_STARTING_SCENARIO);

        g.setupRoundDeployment();

        // Read the external game id from the scenario file
        g.setExternalGameId(parseExternalGameId(p));
        
        g.setVictoryContext(new HashMap<String, Object>());
        g.createVictoryConditions();

        return g;
    }

    private Entity[] buildFactionEntities(Properties p, Player player)
            throws Exception {
        String sFaction = player.getName();

        Vector<Entity> vEntities = new Vector<Entity>();
        for (int i = 1; true; i++) {
            String s = p.getProperty("Unit_" + sFaction + "_" + i);
            if (s == null) {
                // prepare and return array
                Entity[] out = new Entity[vEntities.size()];
                vEntities.copyInto(out);
                return out;
            }
            Entity e = parseEntityLine(s);

            // Damage Plan Stuff
            boolean dpCreated = false;
            DamagePlan dp = new DamagePlan(e);
            s = p.getProperty("Unit_" + sFaction + "_" + i + "_Damage");
            if (s != null) {
                int nBlocks = Integer.parseInt(s);
                m_vDamagePlans.addElement(new DamagePlan(e, nBlocks));
            }

            // Add the Specif Dammage if it exists
            s = p.getProperty("Unit_" + sFaction + "_" + i + "_DamageSpecific");
            if (s != null) {
                StringTokenizer st = new StringTokenizer(s, ",");
                while (st.hasMoreTokens()) {
                    dp.AddSpecificDammage(st.nextToken());
                }
                dpCreated = true;
            }

            // Add Crit Hits if it exists
            boolean chpCreated = false;
            s = p.getProperty("Unit_" + sFaction + "_" + i + "_CritHit");
            CritHitPlan chp = new CritHitPlan(e);
            if (s != null) {
                StringTokenizer st = new StringTokenizer(s, ",");
                while (st.hasMoreTokens()) {
                    chp.AddCritHit(st.nextToken());
                }
                chpCreated = true;
            }

            // Add Set Ammo Locations
            boolean sapCreated = false;
            s = p.getProperty("Unit_" + sFaction + "_" + i + "_SetAmmoTo");
            SetAmmoPlan sap = new SetAmmoPlan(e);
            if (s != null) {
                StringTokenizer st = new StringTokenizer(s, ",");
                while (st.hasMoreTokens()) {
                    sap.AddSetAmmoTo(st.nextToken());
                }
                sapCreated = true;
            }

            if (chpCreated)
                m_vCritHitPlans.addElement(chp);
            if (dpCreated)
                m_vDamagePlans.addElement(dp);
            if (sapCreated)
                m_vSetAmmoTo.addElement(sap);

            // Check for pilot hits
            s = p.getProperty("Unit_" + sFaction + "_" + i + "_PilotHits");
            if (null != s) {
                int hits = Integer.parseInt(s);
                if (hits > 5) {
                    hits = 0;
                }
                e.crew.setHits(hits);
            }

            // Check for unit external ids
            s = p.getProperty("Unit_" + sFaction + "_" + i + "_ExternalID");
            if (null != s) {
                e.setExternalId(Integer.parseInt(s));
            }

            // Check for advantages
            s = p.getProperty("Unit_" + sFaction + "_" + i + "_Advantages");
            if (null != s) {
                parseAdvantages(e, s);
            }

            // Check for autoeject
            s = p.getProperty("Unit_" + sFaction + "_" + i + "_AutoEject");
            if (null != s) {
                parseAutoEject(e, s);
            }

            // Check for commander
            s = p.getProperty("Unit_" + sFaction + "_" + i + "_Commander");
            if (null != s) {
                parseCommander(e, s);
            }

            // Check for deployment
            s = p
                    .getProperty("Unit_" + sFaction + "_" + i
                            + "_DeploymentRound");
            if (null != s) {
                int round = 0;

                try {
                    round = Integer.parseInt(s);
                } catch (Exception ex) {
                    throw new Exception("Bad deployment round setting (" + s
                            + ") for unit " + sFaction + ":" + i);
                }

                if (round < 0) {
                    System.out.println("Deployment round setting of '" + round
                            + "' for " + sFaction + ":" + i
                            + " will be ignored and set to 0");
                    round = 0;
                }

                if (round > 0) {
                    if (player.getStartingPos() == 0) {
                        throw new Exception(
                                "Can not combine a starting position of 'any' with delayed deployment.");
                    }

                    System.out.println(e.getDisplayName()
                            + " will be deployed before round " + round);
                    e.setDeployRound(round);
                }
            }

            vEntities.addElement(e);
        }
    }

    private Entity parseEntityLine(String s) throws Exception {
        try {
            StringTokenizer st = new StringTokenizer(s, ",");
            String sRef = st.nextToken();
            MechSummary ms = MechSummaryCache.getInstance().getMech(sRef);
            if (ms == null) {
                throw new Exception("Scenario requires missing entity: " + sRef);
            }
            System.out.println("Loading " + ms.getName());
            Entity e = new MechFileParser(ms.getSourceFile(), ms.getEntryName())
                    .getEntity();
            e
                    .setCrew(new Pilot(st.nextToken(), Integer.parseInt(st
                            .nextToken()), Integer.parseInt(st.nextToken())));
            try {
                String direction = st.nextToken();
                if (direction.equalsIgnoreCase("N")) {
                    e.setFacing(0);
                } else if (direction.equalsIgnoreCase("NW")) {
                    e.setFacing(5);
                } else if (direction.equalsIgnoreCase("SW")) {
                    e.setFacing(4);
                } else if (direction.equalsIgnoreCase("S")) {
                    e.setFacing(3);
                } else if (direction.equalsIgnoreCase("SE")) {
                    e.setFacing(2);
                } else if (direction.equalsIgnoreCase("NE")) {
                    e.setFacing(1);
                }
                int x = Integer.parseInt(st.nextToken()) - 1;
                int y = Integer.parseInt(st.nextToken()) - 1;
                Coords coords = new Coords(x, y);
                e.setPosition(coords);
                e.setDeployed(true);
            } catch (NoSuchElementException ex) {
            }
            return e;
        } catch (NumberFormatException e) {
            e.printStackTrace();
            throw new Exception("Unparseable entity line: " + s);
        } catch (NoSuchElementException e) {
            e.printStackTrace();
            throw new Exception("Unparseable entity line: " + s);
        } catch (EntityLoadingException e) {
            e.printStackTrace();
            throw new Exception("Unparseable entity line: " + s
                    + "\n   Unable to load mech: " + e.getMessage());
        }
    }

    private void parseAdvantages(Entity entity, String adv) {
        StringTokenizer st = new StringTokenizer(adv);

        while (st.hasMoreTokens()) {
            String curAdv = st.nextToken();

            IOption option = entity.getCrew().getOptions().getOption(curAdv);

            if (null == option) {
                System.out.println("Ignoring invalid pilot advantage: "
                        + curAdv);
            } else {
                System.out.println("Adding pilot advantage '" + curAdv
                        + "' to " + entity.getDisplayName());
                option.setValue(true);
            }
        }
    }

    private void parseAutoEject(Entity entity, String eject) {
        if (entity instanceof Mech) {
            Mech mech = (Mech) entity;
            mech.setAutoEject(Boolean.valueOf(eject).booleanValue());
        }
    }

    private void parseCommander(Entity entity, String eject) {
        entity.setCommander(Boolean.valueOf(eject).booleanValue());
    }

    private int findIndex(String[] sa, String s) {
        for (int x = 0; x < sa.length; x++) {
            if (sa[x].equalsIgnoreCase(s)) {
                return x;
            }
        }
        return -1;
    }

    private Player[] createPlayers(Properties p) throws Exception {
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

            // default to any
            if (s == null) {
                s = "Any";
            }

            int nDir = findIndex(IStartingPositions.START_LOCATION_NAMES, s);

            // if it's not set by now, make it any
            if (nDir == -1) {
                nDir = 0;
            }

            out[x].setStartingPos(nDir);

            // Check for team setup
            int team = Player.TEAM_NONE;

            try {
                team = Integer.parseInt(p.getProperty("Team_"
                        + out[x].getName()));
            } catch (Exception e) {
                team = Player.TEAM_NONE;
            }

            out[x].setTeam(team);

            String minefields = p.getProperty("Minefields_" + out[x].getName());
            if (minefields != null) {
                try {
                    StringTokenizer mfs = new StringTokenizer(minefields, ",");
                    out[x].setNbrMFConventional(Integer.parseInt(mfs
                            .nextToken()));
                    out[x].setNbrMFCommand(Integer.parseInt(mfs.nextToken()));
                    out[x].setNbrMFVibra(Integer.parseInt(mfs.nextToken()));
                } catch (Exception e) {
                    out[x].setNbrMFConventional(0);
                    out[x].setNbrMFCommand(0);
                    out[x].setNbrMFVibra(0);
                    System.err.println("Something wrong with "
                            + out[x].getName() + "s minefields.");
                }
            }
        }

        return out;
    }

    /**
     * Load board files and create the megaboard.
     */
    private IBoard createBoard(Properties p) throws Exception {
        int mapWidth = 16, mapHeight = 17;
        if (p.getProperty("MapWidth") == null) {
            System.out.println("No map width specified.  Using " + mapWidth);
        } else {
            mapWidth = Integer.parseInt(p.getProperty("MapWidth"));
        }

        if (p.getProperty("MapHeight") == null) {
            System.out.println("No map height specified.  Using " + mapHeight);
        } else {
            mapHeight = Integer.parseInt(p.getProperty("MapHeight"));
        }

        int nWidth = 1, nHeight = 1;
        if (p.getProperty("BoardWidth") == null) {
            System.out.println("No board width specified.  Using " + nWidth);
        } else {
            nWidth = Integer.parseInt(p.getProperty("BoardWidth"));
        }

        if (p.getProperty("BoardHeight") == null) {
            System.out.println("No board height specified.  Using " + nHeight);
        } else {
            nHeight = Integer.parseInt(p.getProperty("BoardHeight"));
        }

        System.out.println("Mapsheets are " + mapWidth + " by " + mapHeight
                + " hexes.");
        System.out.println("Constructing " + nWidth + " by " + nHeight
                + " board.");
        int cf = 0;
        if (p.getProperty("BridgeCF") == null) {
            System.out
                    .println("No CF for bridges defined. Using map file defaults.");
        } else {
            cf = Integer.parseInt(p.getProperty("BridgeCF"));
            System.out.println("Overriding map-defined bridge CFs with " + cf
                    + ".");
        }
        // load available boards
        // basically copied from Server.java. Should get moved somewhere neutral
        Vector<String> vBoards = new Vector<String>();
        File boardDir = new File("data/boards");

        String[] fileList = boardDir.list();
        for (int i = 0; i < fileList.length; i++) {
            if (fileList[i].endsWith(".board")) {
                vBoards.addElement(fileList[i].substring(0, fileList[i]
                        .lastIndexOf(".board")));
            }
        }

        IBoard[] ba = new IBoard[nWidth * nHeight];
        StringTokenizer st = new StringTokenizer(p.getProperty("Maps"), ",");
        for (int x = 0; x < nWidth; x++) {
            for (int y = 0; y < nHeight; y++) {
                int n = y * nWidth + x;
                String sBoard = "RANDOM";
                if (st.hasMoreTokens()) {
                    sBoard = st.nextToken();
                }
                System.out.println("(" + x + "," + y + ")" + sBoard);

                boolean isRotated = false;
                if (sBoard.startsWith(Board.BOARD_REQUEST_ROTATION)) {
                    isRotated = true;
                    sBoard = sBoard.substring(Board.BOARD_REQUEST_ROTATION
                            .length());
                }

                String sBoardFile;
                if (sBoard.equals("RANDOM")) {
                    sBoardFile = (vBoards.elementAt(Compute.randomInt(vBoards
                            .size())))
                            + ".board";
                } else {
                    sBoardFile = sBoard + ".board";
                }
                File fBoard = new File(boardDir, sBoardFile);
                if (!fBoard.exists()) {
                    throw new Exception("Scenario requires nonexistant board: "
                            + sBoard);
                }
                ba[n] = new Board();
                ba[n].load(sBoardFile);
                if (cf > 0) {
                    ba[n].setBridgeCF(cf);
                }
                BoardUtilities.flip(ba[n], isRotated, isRotated);
            }
        }

        // if only one board just return it.
        if (ba.length == 1)
            return ba[0];
        // construct the big board
        return BoardUtilities.combine(mapWidth, mapHeight, nWidth, nHeight, ba, MapSettings.MEDIUM_GROUND);
    }

    private Properties loadProperties() throws Exception {
        Properties props = new Properties();
        FileInputStream fis = new FileInputStream(m_scenFile);
        props.load(fis);
        fis.close();

        // Strip trailing spaces
        int loop;
        String key;
        StringBuffer value;
        Properties fixed = new Properties();
        Enumeration<Object> keyIt = props.keys();
        while (keyIt.hasMoreElements()) {
            key = keyIt.nextElement().toString();
            value = new StringBuffer(props.getProperty(key));
            for (loop = value.length() - 1; loop >= 0; loop--) {
                if (!Character.isWhitespace(value.charAt(loop)))
                    break;
            }

            value.setLength(loop + 1);
            fixed.setProperty(key, value.toString());
        }
        return fixed;
    }

    public static void main(String[] saArgs) throws Exception {
        ScenarioLoader sl = new ScenarioLoader(new File(saArgs[0]));
        IGame g = sl.createGame();
        if (g != null)
            System.out.println("Successfully loaded.");
    }

    /*
     * This is used specify the critical hit location
     */
    public class CritHit {
        public int loc;
        public int slot;

        public CritHit(int l, int s) {
            loc = l;
            slot = s;
        }
    }

    /*
     * This class is used to store the critical hit plan for a entity it is
     * loaded from the scenario file. It contains a vector of CritHit.
     */
    class CritHitPlan {
        public Entity entity;
        Vector<CritHit> critHits = new Vector<CritHit>();

        public CritHitPlan(Entity e) {
            entity = e;
        }

        public void AddCritHit(String s) {
            int loc;
            int slot;

            // Get the pos of the ":"
            int ewSpot = s.indexOf(":");

            loc = Integer.parseInt(s.substring(0, ewSpot));
            slot = Integer.parseInt(s.substring(ewSpot + 1));
            critHits.addElement(new CritHit(loc, slot - 1));
        }
    }

    /*
     * This is used to store the ammour to change ammo at a given location
     */
    public class SetAmmoTo {
        public int loc;
        public int slot;
        public int setAmmoTo;

        public SetAmmoTo(int Location, int Slot, int SetAmmoTo) {
            loc = Location;
            slot = Slot;
            setAmmoTo = SetAmmoTo;
        }
    }

    /*
     * This class is used to store the ammo Adjustments it is loaded from the
     * scenario file. It contains a vector of SetAmmoTo.
     */
    class SetAmmoPlan {
        public Entity entity;
        Vector<SetAmmoTo> ammoSetTo = new Vector<SetAmmoTo>();

        public SetAmmoPlan(Entity e) {
            entity = e;
        }

        /*
         * Converts 2:1-34 to Location 2 Slot 1 set Ammo to 34
         */
        public void AddSetAmmoTo(String s) {
            int loc = 0;
            int slot = 0;
            int setTo = 0;

            // Get the pos of the ":"
            int ewSpot = s.indexOf(":");
            int amSpot = s.indexOf("-");

            loc = Integer.parseInt(s.substring(0, ewSpot));
            slot = Integer.parseInt(s.substring(ewSpot + 1, amSpot));
            setTo = Integer.parseInt(s.substring(amSpot + 1));

            ammoSetTo.addElement(new SetAmmoTo(loc, slot, setTo));

        }
    }

    /*
     * This is used specify the one damage location
     */
    public class SpecDam {
        public int loc;
        public int setArmorTo;
        public boolean rear;
        public boolean internal;

        public SpecDam(int Location, int SetArmorTo, boolean RearHit,
                boolean Internal) {
            loc = Location;
            setArmorTo = SetArmorTo;
            rear = RearHit;
            internal = Internal;
        }
    }

    /*
     * This class is used to store the damage plan for a entity it is loaded
     * from the scenario file. It contains a vector of SpecDam.
     */
    class DamagePlan {
        public Entity entity;
        public int nBlocks;
        Vector<SpecDam> specificDammage = new Vector<SpecDam>();
        Vector<SetAmmoTo> ammoSetTo = new Vector<SetAmmoTo>();

        public DamagePlan(Entity e, int n) {
            entity = e;
            nBlocks = n;
        }

        public DamagePlan(Entity e) {
            entity = e;
            nBlocks = 0;
        }

        /*
         * Converts N2:1 to Nornam hit to location 2 set armor to 1!
         */
        public void AddSpecificDammage(String s) {
            int loc = 0;
            int setTo = 0;
            boolean rear = false;
            boolean internal = false;

            // Get the type of set to make
            if (s.substring(0, 1).equals("R"))
                rear = true;

            if (s.substring(0, 1).equals("I"))
                internal = true;

            // Get the pos of the ":"
            int ewSpot = s.indexOf(":");

            // Get the Location this is the number starting at Character 2 in
            // the string
            loc = Integer.parseInt(s.substring(1, ewSpot));
            setTo = Integer.parseInt(s.substring(ewSpot + 1));
            specificDammage.addElement(new SpecDam(loc, setTo, rear, internal));
        }
    }

    /**
     * Parses out the external game id from the scenario file
     */
    private int parseExternalGameId(Properties p) {
        String sExternalId = p.getProperty("ExternalId");
        int ExternalGameId = 0;
        if (sExternalId != null) {
            ExternalGameId = Integer.parseInt(sExternalId);
        }
        return ExternalGameId;
    }
}
