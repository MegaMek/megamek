/*
 * MegaMek - Copyright (C) 2000-2004 Ben Mazur (bmazur@sev.org)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 */
package megamek.common;

import megamek.client.ui.swing.util.PlayerColour;
import megamek.common.enums.GamePhase;
import megamek.common.event.GamePlayerChangeEvent;
import megamek.common.icons.Camouflage;
import megamek.common.options.OptionsConstants;

import java.util.Enumeration;
import java.util.Iterator;
import java.util.Objects;
import java.util.Vector;
import java.util.stream.Collectors;

/**
 * Represents a player in the game.
 */
public final class Player extends TurnOrdered {
    //region Variable Declarations
    private static final long serialVersionUID = 6828849559007455761L;

    public static final int PLAYER_NONE = -1;
    public static final int TEAM_NONE = 0;
    public static final int TEAM_UNASSIGNED = -1;
    public static final String[] TEAM_NAMES = {"No Team", "Team 1", "Team 2", "Team 3", "Team 4", "Team 5"};

    private transient Game game;

    private String name;
    private String email;
    private int id;

    private int team = TEAM_NONE;

    private boolean done = false; // done with phase
    private boolean ghost = false; // disconnected player
    private boolean bot = false;
    private boolean observer = false;

    private boolean seeEntireBoard = false; // Player can observe double blind games

    // these are game-specific, and maybe should be separate from the player object
    private int startingPos = Board.START_ANY;

    // number of minefields
    private int numMfConv = 0;
    private int numMfCmd = 0;
    private int numMfVibra = 0;
    private int numMfActive = 0;
    private int numMfInferno = 0;

    // hexes that are automatically hit by artillery
    private Vector<Coords> artyAutoHitHexes = new Vector<>();

    private int initialEntityCount;
    private int initialBV;

    // initiative bonuses go here because we don't know if teams are rolling
    // initiative collectively
    // if they are then we pick the best non-zero bonuses
    private int constantInitBonus = 0;
    private int streakCompensationBonus = 0;

    private Camouflage camouflage = new Camouflage(Camouflage.COLOUR_CAMOUFLAGE, PlayerColour.BLUE.name());
    private PlayerColour colour = PlayerColour.BLUE;

    private Vector<Minefield> visibleMinefields = new Vector<>();

    private boolean admitsDefeat = false;
    
    /**
     * Boolean that keeps track of whether a player has accepted another 
     * player's request to change teams.
     */
    private boolean allowingTeamChange = false;
    //endregion Variable Declarations

    //region Constructors
    public Player(int id, String name) {
        this.name = name;
        this.id = id;
    }
    //endregion Constructors

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
        return (numMfCmd > 0) || (numMfConv > 0) || (numMfVibra > 0) || (numMfActive > 0) || (numMfInferno > 0);
    }

    public void setNbrMFConventional(int nbrMF) {
        numMfConv = nbrMF;
    }

    public void setNbrMFCommand(int nbrMF) {
        numMfCmd = nbrMF;
    }

    public void setNbrMFVibra(int nbrMF) {
        numMfVibra = nbrMF;
    }

    public void setNbrMFActive(int nbrMF) {
        numMfActive = nbrMF;
    }

    public void setNbrMFInferno(int nbrMF) {
        numMfInferno = nbrMF;
    }

    public int getNbrMFConventional() {
        return numMfConv;
    }

    public int getNbrMFCommand() {
        return numMfCmd;
    }

    public int getNbrMFVibra() {
        return numMfVibra;
    }

    public int getNbrMFActive() {
        return numMfActive;
    }

    public int getNbrMFInferno() {
        return numMfInferno;
    }

    public Camouflage getCamouflage() {
        return camouflage;
    }

    public void setCamouflage(Camouflage camouflage) {
        this.camouflage = camouflage;
    }

    public void setGame(Game game) {
        this.game = game;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public int getId() {
        return id;
    }

    public int getTeam() {
        return team;
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

    /**
     * Specifies if this player connected as a bot.
     */
    public boolean isBot() {
        return bot;
    }

    /**
     * Sets whether this player connected as a bot.
     */
    public void setBot(boolean bot) {
        this.bot = bot;
    }

    public boolean isObserver() {
        if ((game != null) && (game.getPhase() == GamePhase.VICTORY)) {
            return false;
        }
        return observer;
    }

    public void setSeeAll(boolean seeAll) {
        seeEntireBoard = seeAll;
    }

    /**
     * This simply returns the value, without checking the observer flag
     */
    public boolean getSeeAll() {
        return seeEntireBoard;
    }

    /**
     * If observer is false, see_entire_board does nothing
     */
    public boolean canSeeAll() {
        return (observer && seeEntireBoard);
    }

    public void setObserver(boolean observer) {
        this.observer = observer;
        // If not an observer, clear the set see all flag
        if (!observer) {
            setSeeAll(false);
        }
        if (game != null && game.getTeamForPlayer(this) != null) {
            game.getTeamForPlayer(this).cacheObserverStatus();
        }
    }

    public PlayerColour getColour() {
        return colour;
    }

    public void setColour(PlayerColour colour) {
        this.colour = Objects.requireNonNull(colour, "Colour cannot be set to null");
    }

    public int getStartingPos() {
        return startingPos;
    }

    public void setStartingPos(int startingPos) {
        this.startingPos = startingPos;
    }

    /**
     * Set deployment zone to edge of board for reinforcements
     */
    public void adjustStartingPosForReinforcements() {
        if (startingPos > 10) {
            startingPos -= 10; // deep deploy change to standard
        }

        if (startingPos == Board.START_CENTER) {
            startingPos = Board.START_ANY; // center changes to any
        }
    }

    public boolean isEnemyOf(Player other) {
        if (null == other) {
            return true;
        }
        return (id != other.getId()) 
            && ((team == TEAM_NONE) || (team == TEAM_UNASSIGNED) || (team != other.getTeam()));
    }

    public void setAdmitsDefeat(boolean admitsDefeat) {
        this.admitsDefeat = admitsDefeat;
    }

    public boolean admitsDefeat() {
        return admitsDefeat;
    }

    public void setAllowTeamChange(boolean allowChange) {
        allowingTeamChange = allowChange;
    }

    public boolean isAllowingTeamChange() {
        return allowingTeamChange;
    }

    public void setArtyAutoHitHexes(Vector<Coords> artyAutoHitHexes) {
        this.artyAutoHitHexes = artyAutoHitHexes;
    }

    public Vector<Coords> getArtyAutoHitHexes() {
        return artyAutoHitHexes;
    }

    public void addArtyAutoHitHex(Coords c) {
        artyAutoHitHexes.add(c);
    }

    public boolean hasTAG() {
        for (Iterator<Entity> e = game.getSelectedEntities(new EntitySelector() {
                    private final int ownerId = getId();

                    @Override
                    public boolean accept(Entity entity) {
                        if (entity.getOwner() == null) {
                            return false;
                        }
                        return ownerId == entity.getOwner().getId();
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

    public int getEntityCount() {
        return Math.toIntExact(game.getPlayerEntities(this, false).stream()
                .filter(entity -> !entity.isDestroyed() && !entity.isTrapped()).count());
    }

    public int getInitialEntityCount() {
        return initialEntityCount;
    }

    public void setInitialEntityCount(final int initialEntityCount) {
        this.initialEntityCount = initialEntityCount;
    }

    public void changeInitialEntityCount(final int initialEntityCountChange) {
        this.initialEntityCount += initialEntityCountChange;
    }

    /**
     * @return The combined Battle Value of all the player's current assets.
     */
    public int getBV() {
        return game.getPlayerEntities(this, true).stream()
                .filter(entity -> !entity.isDestroyed() && !entity.isTrapped())
                .mapToInt(Entity::calculateBattleValue).sum();
    }

    /**
     * get the total BV (unmodified by force size mod) for the units of this
     * player that have fled the field
     *
     * @return the BV
     */
    public int getFledBV() {
        //TODO: I'm not sure how squadrons are treated here - see getBV()
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

    public int getInitialBV() {
        return initialBV;
    }

    public void setInitialBV(final int initialBV) {
        this.initialBV = initialBV;
    }

    public void changeInitialBV(final int initialBVChange) {
        this.initialBV += initialBVChange;
    }

    @Override
    public void setInitCompensationBonus(int newBonus) {
        streakCompensationBonus = newBonus;
    }

    @Override
    public int getInitCompensationBonus() {
        return streakCompensationBonus;
    }

    public void setConstantInitBonus(int b) {
        constantInitBonus = b;
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
     * @return the bonus to this player's initiative rolls for the highest value initiative
     * (i.e. the 'commander')
     */
    public int getCommandBonus() {
        int commandb = 0;
        
        if (game == null) {
            return 0;
        }
        
        for (Entity entity : game.getEntitiesVector()) {
            if ((null != entity.getOwner())
                    && entity.getOwner().equals(this)
                    && !entity.isDestroyed()
                    && entity.isDeployed()
                    && !entity.isOffBoard()
                    && entity.getCrew().isActive()
                    && !entity.isCaptured()
                    && !(entity instanceof MechWarrior)) {
                int bonus = 0;
                if (game.getOptions().booleanOption(OptionsConstants.RPG_COMMAND_INIT)) {
                    bonus = entity.getCrew().getCommandBonus();
                }
                //Even if the RPG option is not enabled, we still get the command bonus provided by special equipment.
                //Since we are not designating a single force commander at this point, we assume a superheavy tripod
                //is the force commander if that gives the highest bonus.
                if (entity.hasCommandConsoleBonus() || entity.getCrew().hasActiveTechOfficer()) {
                    bonus += 2;
                }
                //Once we've gotten the status of the command console (if any), reset the flag that tracks
                //the previous turn's action.
                if (bonus > commandb) {
                    commandb = bonus;
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
    public Vector<Integer> getAirborneVTOL() {
        // a vector of unit ids
        return game.getEntitiesVector().stream().filter(entity -> entity.getOwner().equals(this)).filter(entity -> ((entity instanceof VTOL) || (entity.getMovementMode() == EntityMovementMode.WIGE)) && (!entity.isDestroyed()) && (entity.getElevation() > 0)).map(Entity::getId).collect(Collectors.toCollection(Vector::new));
    }

    public String getColorForPlayer() {
        return "<B><font color='" + getColour().getHexString(0x00F0F0F0) + "'>" + getName() + "</font></B>";
    }

    /**
     * Un-sets any data that may be considered private.
     *
     * This method clears any data that should not be transmitted to other players from the server,
     * such as email addresses.
     */
    public void redactPrivateData() {
        this.email = null;
    }

    @Override
    public String toString() {
        return "Player " + getId() + " (" + getName() + ")";
    }

    /**
     * Two players are equal if their ids are equal
     */
    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        } else if ((null == object) || (getClass() != object.getClass())) {
            return false;
        } else {
            final Player other = (Player) object;
            return other.id == id;
        }
    }

    @Override
    public int hashCode() {
        return id;
    }

    /**
     * TODO : I should be a clone override, not my own method
     */
    public Player copy() {
        var copy = new Player(this.id, this.name);

        copy.email = email;

        copy.game = game;
        copy.team = team;

        copy.done = done;
        copy.ghost = ghost;
        copy.observer = observer;

        copy.seeEntireBoard = seeEntireBoard;

        copy.startingPos = startingPos;

        copy.numMfConv = numMfConv;
        copy.numMfCmd = numMfCmd;
        copy.numMfVibra = numMfVibra;
        copy.numMfActive = numMfActive;
        copy.numMfInferno = numMfInferno;

        copy.artyAutoHitHexes = new Vector<>(artyAutoHitHexes);

        copy.initialEntityCount = initialEntityCount;
        copy.initialBV = initialBV;

        copy.constantInitBonus = constantInitBonus;
        copy.streakCompensationBonus = streakCompensationBonus;

        copy.camouflage = camouflage;
        copy.colour = colour;

        copy.visibleMinefields = new Vector<>(visibleMinefields);

        copy.admitsDefeat = admitsDefeat;

        return copy;
    }
}
