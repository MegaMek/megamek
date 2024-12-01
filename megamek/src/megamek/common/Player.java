/*
 * Copyright (c) 2000-2004 Ben Mazur (bmazur@sev.org)
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */
package megamek.common;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Vector;

import megamek.client.ui.swing.util.PlayerColour;
import megamek.common.hexarea.BorderHexArea;
import megamek.common.hexarea.HexArea;
import megamek.common.icons.Camouflage;
import megamek.common.options.OptionsConstants;

/**
 * Represents a player in the game.
 *
 * Note that Player should be usable for any type of game (TW, AS, BF, SBF) and therefore should not
 * make any direct use of Game, Entity, AlphaStrikeElement etc., instead using IGame and InGameObject if necessary.
 * Note that two Players are equal if their ID is equal.
 */
public final class Player extends TurnOrdered {

    //region Variable Declarations
    private static final long serialVersionUID = 6828849559007455761L;

    public static final int PLAYER_NONE = -1;
    public static final int TEAM_NONE = 0;
    public static final int TEAM_UNASSIGNED = -1;
    public static final String[] TEAM_NAMES = {"No Team", "Team 1", "Team 2", "Team 3", "Team 4", "Team 5"};
    private transient IGame game;

    private String name;
    private String email;
    private final int id;

    private int team = TEAM_NONE;

    private boolean done = false; // done with phase
    private boolean ghost = false; // disconnected player
    private boolean bot = false;
    private boolean observer = false;
    private boolean gameMaster = false;

    private boolean seeAll = false; // Observer or Game Master can observe double-blind games
    private boolean singleBlind = false; // Bot can observe double-blind games

    // deployment settings
    private int startingPos = Board.START_ANY;
    private int startOffset = 0;
    private int startWidth = 3;
    private int startingAnyNWx = Entity.STARTING_ANY_NONE;
    private int startingAnyNWy = Entity.STARTING_ANY_NONE;
    private int startingAnySEx = Entity.STARTING_ANY_NONE;
    private int startingAnySEy = Entity.STARTING_ANY_NONE;

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

    private List<ICarryable> groundObjectsToPlace = new ArrayList<>();

    //Voting should not be stored in save game so marked transient
    private transient boolean votedToAllowTeamChange = false;
    private transient boolean votedToAllowGameMaster = false;

    private HexArea fleeArea = new BorderHexArea(true, true, true, true);
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
        return (numMfCmd > 0) || (numMfConv > 0) || (numMfVibra > 0) || (numMfActive > 0) || (numMfInferno > 0)
        		|| getGroundObjectsToPlace().size() > 0;
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

    public void setGame(IGame game) {
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
    }

    public boolean isGhost() {
        return ghost;
    }

    public void setGhost(boolean ghost) {
        this.ghost = ghost;
    }

    /**
     * @return true if this player connected as a bot.
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

    /** @return true if this player may become a Game Master. Any human may be a GM*/
    public boolean isGameMasterPermitted() {
        return !bot;
    }

    /** @return true if {@link #gameMaster} flag is true and {@link #isGameMasterPermitted()}*/
    public boolean isGameMaster() {
        return (isGameMasterPermitted() && gameMaster);
    }

    /**
     * If you are checking to see this player is a Game Master, use {@link #isGameMaster()} ()} instead
     * @return the value of gameMaster flag, without checking if it is permitted.
     */
    public boolean getGameMaster() {
        return gameMaster;
    }

    /**
     * sets {@link #gameMaster} but this only allows GM status if other conditions permits it.
     * see {@link #isGameMaster()}
     */
    public void setGameMaster(boolean gameMaster) {
        this.gameMaster = gameMaster;
    }

    /** @return true if {@link #observer} flag is true and not in VICTORY phase*/
    public boolean isObserver() {
        if ((game != null) && game.getPhase().isVictory()) {
            return false;
        }
        return observer;
    }

    /**
     * @return true if this Player is not considered an observer.
     *
     * @see #isObserver()
     */
    public boolean isNotObserver() {
        return !isObserver();
    }

    /**
     *  sets {@link #seeAll}. This will only enable seeAll if other conditions allow it.
     *  see {@link #canIgnoreDoubleBlind()}
     */
    public void setSeeAll(boolean seeAll) {
        this.seeAll = seeAll;
    }

    /**
     * If you are checking to see if double-blind applies to this player, use {@link #canIgnoreDoubleBlind()}
     * @return the value of seeAll flag, without checking if it is permitted
     */
    public boolean getSeeAll() {
        return seeAll;
    }

    /**
     * If you are checking to see if double-blind applies to this player, use {@link #canIgnoreDoubleBlind()}
     * @return true if {@link #seeAll} is true and is permitted
     */
    public boolean canSeeAll() {
        return (isSeeAllPermitted() && seeAll);
    }

    /**
     * If you are checking to see if double-blind applies to this player, use {@link #canIgnoreDoubleBlind()}
     * @return true if player is allowed use seeAll
     * */
    public boolean isSeeAllPermitted() {
        return gameMaster || observer;
    }

    /** set the {@link #observer} flag. Observers have no units ad no team */
    public void setObserver(boolean observer) {
        this.observer = observer;
    }

    /**
     *  sets {@link #seeAll}. This will only enable seeAll if other conditions allow it.
     *  see {@link #canIgnoreDoubleBlind()}
     */
    public void setSingleBlind(boolean singleBlind) {
        this.singleBlind = singleBlind;
    }

    /**
     * If you are checking to see this player can ignore double-blind, use {@link #canIgnoreDoubleBlind()} ()} instead
     * @return the value of singleBlind flag, without checking if it is permitted.
     */
    public boolean getSingleBlind() {
        return singleBlind;
    }

    /**
     * @return true if singleBlind flag is true and {@link #isSingleBlindPermitted()}
     */
    public boolean canSeeSingleBlind() {
        return (isSingleBlindPermitted() && singleBlind);
    }

    /**
     * If you are checking to see if double-blind applies to this player, use {@link #canIgnoreDoubleBlind()}
     * @return true if player is allowed use singleblind (bots only)
     * */
    public boolean isSingleBlindPermitted() {
        return bot;
    }

    /**
     * Double-blind uses Line-of-sight to determine which units are displayed on the board
     * and in reports. seeAll and singleBlind flags allow this to be ignored, granting a view
     * of the entire map and units.
     * @return true if this player ignores the double-blind setting.
     */
    public boolean canIgnoreDoubleBlind() {
        return canSeeSingleBlind() || canSeeAll();
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

    public int getStartOffset() {
        return startOffset;
    }

    public void setStartOffset(int startOffset) {
        this.startOffset = startOffset;
    }

    public int getStartWidth() {
        return startWidth;
    }

    public void setStartWidth(int startWidth) {
        this.startWidth = startWidth;
    }

    public int getStartingAnyNWx() {
        return startingAnyNWx;
    }

    public void setStartingAnyNWx(int i) {
        this.startingAnyNWx = i;
    }

    public int getStartingAnyNWy() {
        return startingAnyNWy;
    }

    public void setStartingAnyNWy(int i) {
        this.startingAnyNWy = i;
    }

    public int getStartingAnySEx() {
        return startingAnySEx;
    }

    public void setStartingAnySEx(int i) {
        this.startingAnySEx = i;
    }

    public int getStartingAnySEy() {
        return startingAnySEy;
    }

    public void setStartingAnySEy(int i) {
        this.startingAnySEy = i;
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

    public boolean doesNotAdmitDefeat() {
        return !admitsDefeat();
    }

    /**
	 * Collection of carryable objects that this player will be placing during the game.
	 */
	public List<ICarryable> getGroundObjectsToPlace() {
		return groundObjectsToPlace;
	}

	/**
	 * Present for serialization purposes only
	 */
	public void setGroundObjectsToPlace(List<ICarryable> groundObjectsToPlace) {
		this.groundObjectsToPlace = groundObjectsToPlace;
	}

	public void setVotedToAllowTeamChange(boolean allowChange) {
        votedToAllowTeamChange = allowChange;
    }

    public boolean getVotedToAllowTeamChange() {
        return votedToAllowTeamChange;
    }

    public void setVotedToAllowGameMaster(boolean allowChange) {
        votedToAllowGameMaster = allowChange;
    }

    public boolean getVotedToAllowGameMaster() {
        return votedToAllowGameMaster;
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
     * Returns the combined strength (Battle Value/PV) of all the player's usable assets. This includes only
     * units that should count according to {@link InGameObject#countForStrengthSum()}.
     *
     * @return The combined strength (BV/PV) of all the player's assets
     */
    public int getBV() {
        return game.getInGameObjects().stream()
                .filter(this::isMyUnit)
                .filter(InGameObject::countForStrengthSum)
                .mapToInt(InGameObject::getStrength).sum();
    }

    /**
     * Returns true when the given unit belongs to this Player.
     *
     * @param unit The unit
     * @return True when the unit belongs to "me", this Player
     */
    public boolean isMyUnit(InGameObject unit) {
        return unit.getOwnerId() == id;
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
        if (game == null) {
            return 0;
        }

        int bonus = 0;
        for (InGameObject object : game.getInGameObjects()) {
            if (object instanceof Entity && ((Entity) object).getOwner().equals(this)) {
                Entity entity = (Entity) object;
                if (game.getOptions().booleanOption(OptionsConstants.ADVANCED_TACOPS_MOBILE_HQS)) {
                    bonus = Math.max(entity.getHQIniBonus(), bonus);
                }

                bonus = Math.max(bonus, entity.getQuirkIniBonus());
            }
        }
        return bonus;
    }

    /**
     * @return the bonus to this player's initiative rolls for the highest value initiative
     * (i.e. the 'commander')
     */
    public int getCommandBonus() {
        if (game == null) {
            return 0;
        }
        int commandb = 0;
        for (InGameObject unit : game.getInGameObjects()) {
            if (unit instanceof Entity) {
                Entity entity = (Entity) unit;
                if ((null != entity.getOwner())
                        && entity.getOwner().equals(this)
                        && !entity.isDestroyed()
                        && entity.isDeployed()
                        && !entity.isOffBoard()
                        && entity.getCrew().isActive()
                        && !entity.isCaptured()
                        && !(entity instanceof MekWarrior)) {
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
        }
        return commandb;
    }

    public String getColorForPlayer() {
        return "<B><font color='" + getColour().getHexString(0x00F0F0F0) + "'>" + getName() + "</font></B>";
    }

    public String getColoredPlayerNameWithTeam() {
        return "<B><font color='" + getColour().getHexString(0x00F0F0F0) + "'>" + getName() +
            " (" + TEAM_NAMES[team] + ")</font></B>";
    }

    /**
     * Clears any data from this Player that should not be transmitted to other players from the server,
     * such as email addresses. Note that this changes this Player's data permanently and should typically
     * be done to a copy of the player, see {@link #copy()}.
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

    public Player copy() {
        var copy = new Player(id, name);

        copy.email = email;

        copy.game = game;
        copy.team = team;

        copy.done = done;
        copy.ghost = ghost;
        copy.bot = bot;
        copy.observer = observer;
        copy.gameMaster = gameMaster;

        copy.seeAll = seeAll;
        copy.singleBlind = singleBlind;

        copy.startingPos = startingPos;
        copy.startOffset = startOffset;
        copy.startWidth = startWidth;

        copy.startingAnyNWx = startingAnyNWx;
        copy.startingAnyNWy = startingAnyNWy;
        copy.startingAnySEx = startingAnySEx;
        copy.startingAnySEy = startingAnySEy;

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

    /**
     * @return The area of the board this player's units are allowed to flee from; An empty area as return value means they
     * may not flee at all.
     */
    public HexArea getFleeZone() {
        return fleeArea;
    }

    /**
     * Sets the board area this player's units may flee from. The area may be empty, in which case the units may not flee.
     *
     * @param fleeArea The new flee area.
     * @see megamek.common.hexarea.BorderHexArea
     */
    public void setFleeZone(HexArea fleeArea) {
        this.fleeArea = fleeArea;
    }
}
