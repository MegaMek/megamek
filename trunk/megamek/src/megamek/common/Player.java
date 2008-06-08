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
import java.util.List;
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

    public static final String colorNames[] = { "Blue", "Yellow", "Red", "Green", "White", "Cyan",
            "Pink", "Orange", "Gray", "Brown", "Purple" };

    public static final String teamNames[] = { "No Team", "Team 1", "Team 2", "Team 3", "Team 4",
            "Team 5" };

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

    private final Vector<Minefield> visibleMinefields = new Vector<Minefield>();

    private boolean admitsDefeat = false;

    private final Vector<Report> turnReports = new Vector<Report>();

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
        return num_mf_cmd > 0 || num_mf_conv > 0 || num_mf_vibra > 0;
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

    public int getNbrMFConventional() {
        return num_mf_conv;
    }

    public int getNbrMFCommand() {
        return num_mf_cmd;
    }

    public int getNbrMFVibra() {
        return num_mf_vibra;
    }

    public void setCamoCategory(String name) {
        camoCategory = name;
    }

    public String getCamoCategory() {
        return camoCategory;
    }

    public void setCamoFileName(String name) {
        camoFileName = name;
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
        if (game != null && game.getPhase() == IGame.Phase.PHASE_VICTORY) {
            return false;
        }
        return observer;
    }

    public void setSeeAll(boolean see_all) {
        see_entire_board = see_all;
    }

    // This simply returns the value, without checking the observer flag
    public boolean getSeeAll() {
        return see_entire_board;
    }

    // If observer is false, see_entire_board does nothing
    public boolean canSeeAll() {
        return observer && see_entire_board;
    }

    public void setObserver(boolean observer) {
        this.observer = observer;
        // If not an observer, clear the set see all flag
        if (!observer) {
            setSeeAll(false);
        }
    }

    public int getColorIndex() {
        return colorIndex;
    }

    public void setColorIndex(int index) {
        colorIndex = index;
    }

    public int getStartingPos() {
        return startingPos;
    }

    public void setStartingPos(int startingPos) {
        this.startingPos = startingPos;
    }

    /** Set deployment zone to edge of board for reinforcements */
    public void adjustStartingPosForReinforcements() {
        if (startingPos > 10) {
            startingPos -= 10; // deep deploy change to standard
        }
        if (startingPos == 0 || startingPos == 10) {
            startingPos = 9; // any or centre change to edge
        }
    }

    public boolean isEnemyOf(Player other) {
        return id != other.getId() && (team == TEAM_NONE || team != other.getTeam());
    }

    /**
     * Two players are equal if their ids are equal
     */
    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        } else if (object == null || getClass() != object.getClass()) {
            return false;
        }
        final Player other = (Player) object;
        return other.getId() == id;
    }

    @Override
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
        for (final Entity m : game.getSelectedEntities(new EntitySelector() {
            private final int ownerId = getId();

            public boolean accept(Entity entity) {
                if (entity.getOwner() == null) {
                    return false;
                }
                if (ownerId == entity.getOwner().getId()) {
                    return true;
                }
                return false;
            }
        })) {
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
        int bv = 0;

        final List<Entity> survivors = game.getEntities();
        for (final Entity entity : survivors) {
            if (entity.getOwner() == this && !entity.isDestroyed()) {
                bv += entity.calculateBattleValue();
            }
        }
        return (int) (bv * getForceSizeBVMod());
    }

    public void setInitialBV() {
        initialBV = getBV();
    }

    public int getInitialBV() {
        return initialBV;
    }

    public float getForceSizeBVMod() {
        if (game.getOptions().booleanOption("no_force_size_mod")) {
            return 1;
        }
        float ourUnitCount = 0;
        final List<Entity> force = game.getEntities();
        for (final Entity entity : force) {
            if (entity.getOwner().equals(this) && !entity.isDestroyed()) {
                ourUnitCount++;
            }
        }
        float enemyUnitCount = 0;
        if (getTeam() == TEAM_NONE) {
            for (final Enumeration<Player> e = game.getPlayers(); e.hasMoreElements();) {
                final Player p = e.nextElement();
                if (!p.equals(this)) {
                    enemyUnitCount += game.getEntitiesOwnedBy(p);
                }
            }
        } else {
            final Team team = game.getTeamForPlayer(this);
            if (team != null) {
                for (final Enumeration<Player> e = team.getPlayers(); e.hasMoreElements();) {
                    final Player p = e.nextElement();
                    if (!p.equals(this)) {
                        ourUnitCount += game.getEntitiesOwnedBy(p);
                    }
                }
            }
            for (final Enumeration<Team> e = game.getTeams(); e.hasMoreElements();) {
                final Team t = e.nextElement();
                if (t.getId() != getTeam()) {
                    for (final Enumeration<Player> players = t.getPlayers(); players.hasMoreElements();) {
                        final Player p = players.nextElement();
                        enemyUnitCount += game.getEntitiesOwnedBy(p);
                    }
                }
            }
        }
        if (ourUnitCount <= enemyUnitCount || enemyUnitCount == 0 || ourUnitCount == 0) {
            return 1;
        }

        return enemyUnitCount / ourUnitCount + ourUnitCount / enemyUnitCount - 1;
    }

    public void setConstantInitBonus(int b) {
        constantInitBonus = b;
    }

    public int getConstantInitBonus() {
        return constantInitBonus;
    }
}