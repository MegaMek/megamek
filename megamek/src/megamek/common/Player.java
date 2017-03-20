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

import java.util.Enumeration;
import java.util.Iterator;
import java.util.Vector;

import megamek.common.event.GamePlayerChangeEvent;
import megamek.common.options.OptionsConstants;

/**
 * Represents a player in the game.
 */
public final class Player extends TurnOrdered implements IPlayer {
    /**
     *
     */
    private static final long serialVersionUID = 6828849559007455760L;

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
    private int startingPos = Board.START_ANY;

    // number of minefields
    private int num_mf_conv = 0;
    private int num_mf_cmd = 0;
    private int num_mf_vibra = 0;
    private int num_mf_active = 0;
    private int num_mf_inferno = 0;

    // hexes that are automatically hit by artillery
    private Vector<Coords> artyAutoHitHexes = new Vector<Coords>();

    private int initialBV;

    // initiative bonuses go here because we don't know if teams are rolling
    // initiative collectively
    // if they are then we pick the best non-zero bonuses
    private int constantInitBonus = 0;
    private int streakCompensationBonus = 0;

    private String camoCategory = IPlayer.NO_CAMO;

    private String camoFileName = null;

    private Vector<Minefield> visibleMinefields = new Vector<Minefield>();

    private boolean admitsDefeat = false;
    
    /**
     * Boolean that keeps track of whether a player has accepted another 
     * player's request to chang teams.
     */
    private boolean allowingTeamChange = false;

    @Override
    public Vector<Minefield> getMinefields() {
        return visibleMinefields;
    }

    @Override
    public void addMinefield(Minefield mf) {
        visibleMinefields.addElement(mf);
    }

    @Override
    public void addMinefields(Vector<Minefield> minefields) {
        for (int i = 0; i < minefields.size(); i++) {
            visibleMinefields.addElement(minefields.elementAt(i));
        }
    }

    @Override
    public void removeMinefield(Minefield mf) {
        visibleMinefields.removeElement(mf);
    }

    @Override
    public void removeMinefields() {
        visibleMinefields.removeAllElements();
    }

    @Override
    public void removeArtyAutoHitHexes() {
        artyAutoHitHexes.removeAllElements();
    }

    @Override
    public boolean containsMinefield(Minefield mf) {
        return visibleMinefields.contains(mf);
    }

    @Override
    public boolean hasMinefields() {
        return (num_mf_cmd > 0) || (num_mf_conv > 0) || (num_mf_vibra > 0) || (num_mf_active > 0) || (num_mf_inferno > 0);
    }

    @Override
    public void setNbrMFConventional(int nbrMF) {
        num_mf_conv = nbrMF;
    }

    @Override
    public void setNbrMFCommand(int nbrMF) {
        num_mf_cmd = nbrMF;
    }

    @Override
    public void setNbrMFVibra(int nbrMF) {
        num_mf_vibra = nbrMF;
    }

    @Override
    public void setNbrMFActive(int nbrMF) {
        num_mf_active = nbrMF;
    }

    @Override
    public void setNbrMFInferno(int nbrMF) {
        num_mf_inferno = nbrMF;
    }

    @Override
    public int getNbrMFConventional() {
        return num_mf_conv;
    }

    @Override
    public int getNbrMFCommand() {
        return num_mf_cmd;
    }

    @Override
    public int getNbrMFVibra() {
        return num_mf_vibra;
    }

    @Override
    public int getNbrMFActive() {
        return num_mf_active;
    }

    @Override
    public int getNbrMFInferno() {
        return num_mf_inferno;
    }

    @Override
    public void setCamoCategory(String name) {
        camoCategory = name;
    }

    @Override
    public String getCamoCategory() {
        return camoCategory;
    }

    @Override
    public void setCamoFileName(String name) {
        camoFileName = name;
    }

    @Override
    public String getCamoFileName() {
        return camoFileName;
    }

    public Player(int id, String name) {
        this.name = name;
        this.id = id;
    }

    @Override
    public void setGame(IGame game) {
        this.game = game;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public int getId() {
        return id;
    }

    @Override
    public int getTeam() {
        return team;
    }

    @Override
    public void setTeam(int team) {
        this.team = team;
    }

    @Override
    public boolean isDone() {
        return done;
    }

    @Override
    public void setDone(boolean done) {
        this.done = done;
        game.processGameEvent(new GamePlayerChangeEvent(this, this));
    }

    @Override
    public boolean isGhost() {
        return ghost;
    }

    @Override
    public void setGhost(boolean ghost) {
        this.ghost = ghost;
    }

    @Override
    public boolean isObserver() {
        if ((game != null) && (game.getPhase() == IGame.Phase.PHASE_VICTORY)) {
            return false;
        }
        return observer;
    }

    @Override
    public void setSeeAll(boolean see_all) {
        see_entire_board = see_all;
    }

    // This simply returns the value, without checking the observer flag
    @Override
    public boolean getSeeAll() {
        return see_entire_board;
    }

    // If observer is false, see_entire_board does nothing
    @Override
    public boolean canSeeAll() {
        return (observer && see_entire_board);
    }

    @Override
    public void setObserver(boolean observer) {
        this.observer = observer;
        // If not an observer, clear the set see all flag
        if (!observer) {
            setSeeAll(false);
        }
    }

    @Override
    public int getColorIndex() {
        return colorIndex;
    }

    @Override
    public void setColorIndex(int index) {
        colorIndex = index;
    }

    @Override
    public int getStartingPos() {
        return startingPos;
    }

    @Override
    public void setStartingPos(int startingPos) {
        this.startingPos = startingPos;
    }

    /**
     * Set deployment zone to edge of board for reinforcements
     */
    @Override
    public void adjustStartingPosForReinforcements() {
        if (startingPos > 10) {
            startingPos -= 10; // deep deploy change to standard
        }
        if (startingPos == Board.START_CENTER) {
            startingPos = Board.START_ANY; // center changes to any
        }
    }

    @Override
    public boolean isEnemyOf(IPlayer other) {
        if(null == other) {
            return true;
        }
        return (id != other.getId()) 
            && ((team == TEAM_NONE) || (team == TEAM_UNASSIGNED) || (team != other.getTeam()));
    }

    /**
     * Two players are equal if their ids are equal
     */
    @Override
    public boolean equals(Object object) {
        if(this == object) {
            return true;
        }
        if((null == object) || (getClass() != object.getClass())) {
            return false;
        }
        final Player other = (Player) object;
        return other.id == id;
    }

    @Override
    public int hashCode() {
        return id;
    }

    @Override
    public void setAdmitsDefeat(boolean admitsDefeat) {
        this.admitsDefeat = admitsDefeat;
    }

    @Override
    public boolean admitsDefeat() {
        return admitsDefeat;
    }
    
    @Override
    public void setAllowTeamChange(boolean allowChange){
        allowingTeamChange = allowChange;
    }
    
    @Override
    public boolean isAllowingTeamChange(){
        return allowingTeamChange;
    }

    @Override
    public void setArtyAutoHitHexes(Vector<Coords> artyAutoHitHexes) {
        this.artyAutoHitHexes = artyAutoHitHexes;
    }

    @Override
    public Vector<Coords> getArtyAutoHitHexes() {
        return artyAutoHitHexes;
    }

    @Override
    public void addArtyAutoHitHex(Coords c) {
        artyAutoHitHexes.add(c);
    }

    @Override
    public boolean hasTAG() {
        for (Iterator<Entity> e = game
                .getSelectedEntities(new EntitySelector() {
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
                }); e.hasNext(); ) {
            Entity m = e.next();
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
    @Override
    public int getBV() {
        int bv = 0;

        for (Entity entity : game.getEntitiesVector()) {
            if (equals(entity.getOwner()) && !entity.isDestroyed()
                    && !entity.isTrapped()) {
                bv += entity.calculateBattleValue();
            }
        }
        return bv;
    }

    /**
     * get the total BV (unmodified by force size mod) for the units of this
     * player that have fled the field
     *
     * @return the BV
     */
    @Override
    public int getFledBV() {
        Enumeration<Entity> fledUnits = game.getRetreatedEntities();
        int bv = 0;
        while (fledUnits.hasMoreElements()) {
            Entity entity = fledUnits.nextElement();
            if (entity.getOwner().equals(this)) {
                bv += entity.calculateBattleValue();
            }
        }
        return bv;
    }

    @Override
    public void setInitialBV() {
        initialBV = getBV();
    }

    @Override
    public int getInitialBV() {
        return initialBV;
    }

    @Override
    public void setCompensationInitBonus(int newBonus) {
        streakCompensationBonus = newBonus;
    }

    @Override
    public int getCompensationInitBonus() {
        return streakCompensationBonus;
    }

    @Override
    public void setConstantInitBonus(int b) {
        constantInitBonus = b;
    }

    @Override
    public int getConstantInitBonus() {
        return constantInitBonus;
    }

    /**
     * @return the bonus to this player's initiative rolls granted by his units
     */
    @Override
    public int getTurnInitBonus() {
        int bonusHQ = 0;
        int bonusMD = 0;
        int bonusQ = 0;
        if (game == null) {
            return 0;
        }
        if (game.getEntitiesVector() == null) {
            return 0;
        }
        for (Entity entity : game.getEntitiesVector()) {
            if (entity.getOwner().equals(this)) {
                if (game.getOptions().booleanOption(OptionsConstants.ADVANCED_TACOPS_MOBILE_HQS)
                    && (bonusHQ == 0) && (entity.getHQIniBonus() > 0)) {
                    bonusHQ = entity.getHQIniBonus();
                }
                
				/*
				 * REMOVED IN IO. 
				 * if (game.getOptions().booleanOption(OptionsConstants.
				 * RPG_MANEI_DOMINI) && (bonusMD == 0) &&
				 * (entity.getMDIniBonus() > 0)) { bonusMD =
				 * entity.getMDIniBonus(); }
				 */
                if (entity.getQuirkIniBonus() > bonusQ) {
                    //TODO: I am assuming that the quirk initiative bonuses go to the highest,
                    //rather than being cumulative
                    //http://www.classicbattletech.com/forums/index.php/topic,52903.new.html#new
                    bonusQ = entity.getQuirkIniBonus();
                }
            }
        }
        return bonusHQ + bonusMD + bonusQ;
    }

    /**
     * @return the bonus to this player's initiative rolls for
     *         the highest value initiative (i.e. the 'commander')
     */
    @Override
    public int getCommandBonus() {
        int commandb = 0;
        if (game.getOptions().booleanOption(OptionsConstants.RPG_COMMAND_INIT)) {
            for (Entity entity : game.getEntitiesVector()) {
                if ((null != entity.getOwner())
                    && entity.getOwner().equals(this)
                    && !entity.isDestroyed()
                    && entity.isDeployed()
                    && !entity.isOffBoard()
                    && entity.getCrew().isActive()
                    && !entity.isCaptured()
                    && !(entity instanceof MechWarrior)) {
                    if (entity.getCrew().getCommandBonus() > commandb) {
                        commandb = entity.getCrew().getCommandBonus();
                    }
                }
            }
        }
        return commandb;
    }

    /**
     * cycle through entities on team and collect all the airborne VTOL/WIGE
     *
     * @return a vector of relevant entity ids
     */
    @Override
    public Vector<Integer> getAirborneVTOL() {

        //a vector of unit ids
        Vector<Integer> units = new Vector<Integer>();
        for (Entity entity : game.getEntitiesVector()) {
            if (entity.getOwner().equals(this)) {
                if (((entity instanceof VTOL)
                     || (entity.getMovementMode() == EntityMovementMode.WIGE)) &&
                    (!entity.isDestroyed()) &&
                    (entity.getElevation() > 0)) {
                    units.add(entity.getId());
                }
            }
        }
        return units;
    }
    
    public String toString() {
        return "Player " + getId() + " (" + getName() + ")";
    }
}
