/*
 * MegaMek - Copyright (C) 2000,2001,2002,2003,2004 Ben Mazur (bmazur@sev.org)
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

package megamek.common;

import java.io.Serializable;
import java.util.Enumeration;
import java.util.Vector;

import megamek.common.event.GamePlayerChangeEvent;

/**
 * Represents a player in the game.
 */
public final class Player extends TurnOrdered implements Serializable {
    /**
     * 
     */
    private static final long serialVersionUID = 6828849559007455760L;
    public static final int PLAYER_NONE = -1;
    public static final int TEAM_NONE = 0;

    public static final String colorNames[] = { "Blue", "Yellow", "Red",
            "Green", "White", "Cyan", "Pink", "Orange", "Gray", "Brown",
            "Purple" };

    public static final String teamNames[] = { "No Team", "Team 1", "Team 2",
            "Team 3", "Team 4", "Team 5" };
    public static final int MAX_TEAMS = teamNames.length;

    private transient IGame game;

    private String name = "unnamed";
    private int id;

    private int team = TEAM_NONE;

    private boolean done = false; // done with phase
    private boolean ghost = false; // disconnected player
    private boolean observer = false;

    private boolean see_entire_board = false; // Player can observe
    // double blind games

    private int colorIndex = 0;

    // these are game-specific, and maybe should be seperate from the player
    // object
    private int startingPos = 0;

    // number of minefields
    private int num_mf_conv = 0;
    private int num_mf_cmd = 0;
    private int num_mf_vibra = 0;
    private int num_mf_active = 0;
    private int num_mf_inferno = 0;
    
    //now I need to actually keep a vector of minefields because more information is needed than just the number

    // hexes that are automatically hit by artillery
    private Vector<Coords> artyAutoHitHexes = new Vector<Coords>();

    private int initialBV;

    // initiative bonuses go here because we don't know if teams are rolling
    // initiative collectively
    // if they are then we pick the best non-zero bonuses
    private int constantInitBonus = 0;

    /**
     * The "no camo" category.
     */
    public static final String NO_CAMO = "-- No Camo --";

    /**
     * The category for camos in the root directory.
     */
    public static final String ROOT_CAMO = "-- General --";

    private String camoCategory = Player.NO_CAMO;

    private String camoFileName = null;

    private Vector<Minefield> visibleMinefields = new Vector<Minefield>();

    private boolean admitsDefeat = false;

    private Vector<Report> turnReports = new Vector<Report>();

    public Vector<Minefield> getMinefields() {
        return visibleMinefields;
    }

    public void addMinefield(Minefield mf) {
        visibleMinefields.addElement(mf);
    }

    public void addMinefields(Vector<Minefield> minefields) {
        for (int i = 0; i < minefields.size(); i++) {
            visibleMinefields.addElement(minefields.elementAt(i));
        }
    }

    public void removeMinefield(Minefield mf) {
        visibleMinefields.removeElement(mf);
    }

    public void removeMinefields() {
        visibleMinefields.removeAllElements();
    }

    public void removeArtyAutoHitHexes() {
        artyAutoHitHexes.removeAllElements();
    }

    public boolean containsMinefield(Minefield mf) {
        return visibleMinefields.contains(mf);
    }

    public boolean hasMinefields() {
        return (num_mf_cmd > 0) || (num_mf_conv > 0) || (num_mf_vibra > 0) || (num_mf_active > 0) || (num_mf_inferno > 0);
    }

    public void setNbrMFConventional(int nbrMF) {
        num_mf_conv = nbrMF;
    }

    public void setNbrMFCommand(int nbrMF) {
        num_mf_cmd = nbrMF;
    }

    public void setNbrMFVibra(int nbrMF) {
        num_mf_vibra = nbrMF;
    }
    
    public void setNbrMFActive(int nbrMF) {
        num_mf_active = nbrMF;
    }
    
    public void setNbrMFInferno(int nbrMF) {
        num_mf_inferno = nbrMF;
    }

    public int getNbrMFConventional() {
        return num_mf_conv;
    }

    public int getNbrMFCommand() {
        return num_mf_cmd;
    }

    public int getNbrMFVibra() {
        return num_mf_vibra;
    }
    
    public int getNbrMFActive() {
        return num_mf_active;
    }
    
    public int getNbrMFInferno() {
        return num_mf_inferno;
    }

    public void setCamoCategory(String name) {
        this.camoCategory = name;
    }

    public String getCamoCategory() {
        return camoCategory;
    }

    public void setCamoFileName(String name) {
        this.camoFileName = name;
    }

    public String getCamoFileName() {
        return camoFileName;
    }

    public Player(int id, String name) {
        this.name = name;
        this.id = id;
    }

    public void setGame(IGame game) {
        this.game = game;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getId() {
        return id;
    }

    public int getTeam() {
        return team;
    }

    public Vector<Report> getTurnReport() {
        return turnReports;
    }

    public void setTeam(int team) {
        this.team = team;
    }

    public boolean isDone() {
        return done;
    }

    public void setDone(boolean done) {
        this.done = done;
        game.processGameEvent(new GamePlayerChangeEvent(this, this));
    }

    public boolean isGhost() {
        return ghost;
    }

    public void setGhost(boolean ghost) {
        this.ghost = ghost;
    }

    public boolean isObserver() {
        if (game != null && game.getPhase() == IGame.Phase.PHASE_VICTORY)
            return false;
        return observer;
    }

    public void setSeeAll(boolean see_all) {
        this.see_entire_board = see_all;
    }

    // This simply returns the value, without checking the observer flag
    public boolean getSeeAll() {
        return see_entire_board;
    }

    // If observer is false, see_entire_board does nothing
    public boolean canSeeAll() {
        return (observer && see_entire_board);
    }

    public void setObserver(boolean observer) {
        this.observer = observer;
        // If not an observer, clear the set see all flag
        if (!observer)
            this.setSeeAll(false);
    }

    public int getColorIndex() {
        return colorIndex;
    }

    public void setColorIndex(int index) {
        this.colorIndex = index;
    }

    public int getStartingPos() {
        return startingPos;
    }

    public void setStartingPos(int startingPos) {
        this.startingPos = startingPos;
    }

    /** Set deployment zone to edge of board for reinforcements */
    public void adjustStartingPosForReinforcements() {
        if (startingPos > 10)
            startingPos -= 10; // deep deploy change to standard
        if (startingPos == 0 || startingPos == 10)
            startingPos = 9; // any or centre change to edge
    }

    public boolean isEnemyOf(Player other) {
        return (id != other.getId() && (team == TEAM_NONE || team != other
                .getTeam()));
    }

    /**
     * Two players are equal if their ids are equal
     */
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        } else if (object == null || getClass() != object.getClass()) {
            return false;
        }
        Player other = (Player) object;
        return other.getId() == this.id;
    }

    public int hashCode() {
        return getId();
    }

    public void setAdmitsDefeat(boolean admitsDefeat) {
        this.admitsDefeat = admitsDefeat;
    }

    public boolean admitsDefeat() {
        return admitsDefeat;
    }

    public void setArtyAutoHitHexes(Vector<Coords> artyAutoHitHexes) {
        this.artyAutoHitHexes = artyAutoHitHexes;
    }

    public Vector<Coords> getArtyAutoHitHexes() {
        return artyAutoHitHexes;
    }

    public boolean hasTAG() {
        for (Enumeration<Entity> e = game
                .getSelectedEntities(new EntitySelector() {
                    private final int ownerId = getId();

                    public boolean accept(Entity entity) {
                        if (entity.getOwner() == null)
                            return false;
                        if (ownerId == entity.getOwner().getId())
                            return true;
                        return false;
                    }
                }); e.hasMoreElements();) {
            Entity m = e.nextElement();
            if (m.hasTAG()) {
                return true;
            }
            // A player can't be on two teams.
        }
        return false;
    }

    /**
     * @return The combined Battle Value of all the player's current assets.
     */
    public int getBV() {
        Enumeration<Entity> survivors = game.getEntities();
        int bv = 0;

        while (survivors.hasMoreElements()) {
            Entity entity = survivors.nextElement();
            if (entity.getOwner() == this && !entity.isDestroyed())
                bv += entity.calculateBattleValue();
        }
        return (int) (bv * this.getForceSizeBVMod());
    }

    public void setInitialBV() {
        this.initialBV = getBV();
    }

    public int getInitialBV() {
        return initialBV;
    }

    public float getForceSizeBVMod() {
        if (game.getOptions().booleanOption("no_force_size_mod"))
            return 1;
        Enumeration<Entity> entities = game.getEntities();
        float ourUnitCount = 0;
        while (entities.hasMoreElements()) {
            final Entity entity = entities.nextElement();
            if (entity.getOwner().equals(this) && !entity.isDestroyed()) {
                ourUnitCount++;
            }
        }
        float enemyUnitCount = 0;
        if (this.getTeam() == TEAM_NONE) {
            for (Enumeration<Player> e = game.getPlayers(); e.hasMoreElements();) {
                Player p = e.nextElement();
                if (!p.equals(this)) {
                    enemyUnitCount += game.getEntitiesOwnedBy(p);
                }
            }
        } else {
            Team team = game.getTeamForPlayer(this);
            if (team != null) {
                for (Enumeration<Player> e = team.getPlayers(); e
                        .hasMoreElements();) {
                    Player p = e.nextElement();
                    if (!p.equals(this)) {
                        ourUnitCount += game.getEntitiesOwnedBy(p);
                    }
                }
            }
            for (Enumeration<Team> e = game.getTeams(); e.hasMoreElements();) {
                Team t = e.nextElement();
                if (t.getId() != this.getTeam()) {
                    for (Enumeration<Player> players = t.getPlayers(); players
                            .hasMoreElements();) {
                        Player p = players.nextElement();
                        enemyUnitCount += game.getEntitiesOwnedBy(p);
                    }
                }
            }
        }
        if (ourUnitCount <= enemyUnitCount || enemyUnitCount == 0
                || ourUnitCount == 0) {
            return 1;
        }

        return (enemyUnitCount / ourUnitCount)
                + (ourUnitCount / enemyUnitCount) - 1;
    }

    public void setConstantInitBonus(int b) {
        this.constantInitBonus = b;
    }

    public int getConstantInitBonus() {
        return constantInitBonus;
    }
    
    /**
     * @return the bonus to this player's initiative rolls granted by his units
     */
    public int getTurnInitBonus() {
        int bonusHQ = 0;
        int bonusMD = 0;      
        for (Entity entity : game.getEntitiesVector()) {
            if (entity.getOwner().equals(this)) {
                if (game.getOptions().booleanOption("tacops_mobile_hqs") 
                        && bonusHQ == 0 && entity.getHQIniBonus() > 0) {
                            bonusHQ = entity.getHQIniBonus();
                }
                if (game.getOptions().booleanOption("manei_domini") 
                        && bonusMD == 0 && entity.getMDIniBonus() > 0) {
                            bonusMD = entity.getMDIniBonus();
                }
            }
        }     
        return bonusHQ + bonusMD;
    }
    
    /**
     * @return the bonus to this player's initiative rolls for 
     * the highest value initiative (i.e. the 'commander')
     */
    public int getCommandBonus() {
        int commandb = 0;
        if (game.getOptions().booleanOption("command_init")) {
            for (Entity entity : game.getEntitiesVector()) {
                if (entity.getOwner().equals(this) 
                        && !entity.isDestroyed()
                        && entity.isDeployed() 
                        && !entity.isOffBoard()
                        && entity.getCrew().isActive()
                        && !entity.isCaptured()) {
                    if (entity.getCrew().getCommandBonus() > commandb)
                        commandb = entity.getCrew().getCommandBonus();
                }
            }
        }
        return commandb;
    }
    
    /**
     * cycle through entities on team and collect all the airborne VTOL/WIGE
     * @return a vector of relevant entity ids
     */
    public Vector<Integer> getAirborneVTOL() {
    
        //a vector of unit ids
        Vector<Integer> units = new Vector<Integer>();           
        for(Entity entity : game.getEntitiesVector()) {
            if (entity.getOwner().equals(this) ) {
                if(entity.getElevation() > 0 &&
                        (entity instanceof VTOL 
                                || entity.getMovementMode() == IEntityMovementMode.WIGE)) {
                    units.add(entity.getId());
                }             
            }
        }
        return units;
    }
}