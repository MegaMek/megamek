/*
 * Copyright (c) 2000-2004 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2002-2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MegaMek was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */

package megamek.common;

import java.io.Serial;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Vector;
import java.util.stream.Collectors;

import megamek.client.ui.util.PlayerColour;
import megamek.common.board.Board;
import megamek.common.board.BoardLocation;
import megamek.common.compute.ComputeECM;
import megamek.common.equipment.ICarryable;
import megamek.common.equipment.Minefield;
import megamek.common.equipment.MiscType;
import megamek.common.game.Game;
import megamek.common.game.IGame;
import megamek.common.game.InGameObject;
import megamek.common.hexArea.BorderHexArea;
import megamek.common.hexArea.HexArea;
import megamek.common.icons.Camouflage;
import megamek.common.interfaces.IStartingPositions;
import megamek.common.options.OptionsConstants;
import megamek.common.turns.TurnOrdered;
import megamek.common.units.Entity;
import megamek.common.units.MekWarrior;
import megamek.logging.MMLogger;

/**
 * Represents a player in the game.
 * <p>
 * Note that Player should be usable for any type of game (TW, AS, BF, SBF) and therefore should not make any direct use
 * of Game, Entity, AlphaStrikeElement etc., instead using IGame and InGameObject if necessary. Note that two Players
 * are equal if their ID is equal.
 */
public final class Player extends TurnOrdered {

    //region Variable Declarations
    @Serial
    private static final long serialVersionUID = 6828849559007455761L;
    private static final MMLogger LOGGER = MMLogger.create(Player.class);

    public static final int PLAYER_NONE = -1;
    public static final int TEAM_NONE = 0;
    public static final int TEAM_UNASSIGNED = -1;
    public static final String[] TEAM_NAMES = { "No Team", "Team 1", "Team 2", "Team 3", "Team 4", "Team 5" };
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
    private List<BoardLocation> artyAutoHitHexes = new ArrayList<>();

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
        artyAutoHitHexes.clear();
    }

    public boolean containsMinefield(Minefield mf) {
        return visibleMinefields.contains(mf);
    }

    public boolean hasMinefields() {
        return (numMfCmd > 0) ||
              (numMfConv > 0) ||
              (numMfVibra > 0) ||
              (numMfActive > 0) ||
              (numMfInferno > 0) ||
              !getGroundObjectsToPlace().isEmpty();
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

    /** @return true if this player may become a Game Master. Any human may be a GM */
    public boolean isGameMasterPermitted() {
        return !bot;
    }

    /** @return true if {@link #gameMaster} flag is true and {@link #isGameMasterPermitted()} */
    public boolean isGameMaster() {
        return (isGameMasterPermitted() && gameMaster);
    }

    /**
     * If you are checking to see this player is a Game Master, use {@link #isGameMaster()} instead
     *
     * @return the value of gameMaster flag, without checking if it is permitted.
     */
    public boolean getGameMaster() {
        return gameMaster;
    }

    /**
     * sets {@link #gameMaster} but this only allows GM status if other conditions permits it. see
     * {@link #isGameMaster()}
     */
    public void setGameMaster(boolean gameMaster) {
        this.gameMaster = gameMaster;
    }

    /** @return true if {@link #observer} flag is true and not in VICTORY phase */
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
     * sets {@link #seeAll}. This will only enable seeAll if other conditions allow it. see
     * {@link #canIgnoreDoubleBlind()}
     */
    public void setSeeAll(boolean seeAll) {
        this.seeAll = seeAll;
    }

    /**
     * If you are checking to see if double-blind applies to this player, use {@link #canIgnoreDoubleBlind()}
     *
     * @return the value of seeAll flag, without checking if it is permitted
     */
    public boolean getSeeAll() {
        return seeAll;
    }

    /**
     * If you are checking to see if double-blind applies to this player, use {@link #canIgnoreDoubleBlind()}
     *
     * @return true if {@link #seeAll} is true and is permitted
     */
    public boolean canSeeAll() {
        return (isSeeAllPermitted() && seeAll);
    }

    /**
     * If you are checking to see if double-blind applies to this player, use {@link #canIgnoreDoubleBlind()}
     *
     * @return true if player is allowed use seeAll
     */
    public boolean isSeeAllPermitted() {
        return gameMaster || observer;
    }

    /** set the {@link #observer} flag. Observers have no units add no team */
    public void setObserver(boolean observer) {
        this.observer = observer;
    }

    /**
     * sets {@link #seeAll}. This will only enable seeAll if other conditions allow it. see
     * {@link #canIgnoreDoubleBlind()}
     */
    public void setSingleBlind(boolean singleBlind) {
        this.singleBlind = singleBlind;
    }

    /**
     * If you are checking to see this player can ignore double-blind, use {@link #canIgnoreDoubleBlind()} instead
     *
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
     *
     * @return true if player is allowed use single blind (bots only)
     */
    public boolean isSingleBlindPermitted() {
        return bot;
    }

    /**
     * Double-blind uses Line-of-sight to determine which units are displayed on the board and in reports. seeAll and
     * singleBlind flags allow this to be ignored, granting a view of the entire map and units.
     *
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
        if ((startingPos > 10) && (startingPos < IStartingPositions.START_LOCATION_NAMES.length)) {
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
        return (id != other.getId()) && ((team == TEAM_NONE) || (team == TEAM_UNASSIGNED) || (team != other.getTeam()));
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

    public void setArtyAutoHitHexes(List<BoardLocation> newArtyAutoHitHexes) {
        artyAutoHitHexes.clear();
        artyAutoHitHexes.addAll(newArtyAutoHitHexes);
        artyAutoHitHexes.removeIf(BoardLocation::isNoLocation);
    }

    public List<BoardLocation> getArtyAutoHitHexes() {
        return artyAutoHitHexes;
    }

    public void addArtyAutoHitHex(BoardLocation boardLocation) {
        artyAutoHitHexes.add(boardLocation);
        artyAutoHitHexes.removeIf(BoardLocation::isNoLocation);
    }

    public void removeArtyAutoHitHex(BoardLocation boardLocation) {
        artyAutoHitHexes.remove(boardLocation);
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
     * Returns the combined strength (Battle Value/PV) of all the player's usable assets. This includes only units that
     * should count according to {@link InGameObject#countForStrengthSum()}.
     *
     * @return The combined strength (BV/PV) of all the player's assets
     */
    public int getBV() {
        return List.copyOf(game.getInGameObjects())
              .stream()
              .filter(this::isMyUnit)
              .filter(InGameObject::countForStrengthSum)
              .mapToInt(InGameObject::getStrength)
              .sum();
    }

    /**
     * Returns true when the given unit belongs to this Player.
     *
     * @param unit The unit
     *
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
            if (object instanceof Entity entity && entity.getOwner().equals(this)) {
                if (game.getOptions().booleanOption(OptionsConstants.ADVANCED_TAC_OPS_MOBILE_HQS)) {
                    bonus = Math.max(entity.getHQIniBonus(), bonus);
                }

                bonus = Math.max(bonus, entity.getQuirkIniBonus());
            }
        }
        return bonus;
    }

    /**
     * @return the best HQ initiative bonus from this player's units (TacOps Mobile HQs option)
     */
    public int getHQInitBonus() {
        if (game == null) {
            return 0;
        }
        if (!game.getOptions().booleanOption(OptionsConstants.ADVANCED_TAC_OPS_MOBILE_HQS)) {
            return 0;
        }

        int bonus = 0;
        for (InGameObject object : game.getInGameObjects()) {
            if (object instanceof Entity entity && entity.getOwner().equals(this)) {
                bonus = Math.max(entity.getHQIniBonus(), bonus);
            }
        }
        return bonus;
    }

    /**
     * @return the best quirk initiative bonus from this player's units
     */
    public int getQuirkInitBonus() {
        if (game == null) {
            return 0;
        }

        int bonus = 0;
        for (InGameObject object : game.getInGameObjects()) {
            if (object instanceof Entity entity && entity.getOwner().equals(this)) {
                bonus = Math.max(bonus, entity.getQuirkIniBonus());
            }
        }
        return bonus;
    }

    /**
     * @return the name of the quirk providing the best initiative bonus, or null if none
     */
    public String getQuirkInitBonusName() {
        if (game == null) {
            return null;
        }

        int bestBonus = 0;
        String bestQuirkName = null;
        for (InGameObject object : game.getInGameObjects()) {
            if (object instanceof Entity entity && entity.getOwner().equals(this)) {
                int entityBonus = entity.getQuirkIniBonus();
                if (entityBonus > bestBonus) {
                    bestBonus = entityBonus;
                    // Determine which quirk is providing the bonus
                    if (entity.hasQuirk(OptionsConstants.QUIRK_POS_BATTLE_COMP)) {
                        bestQuirkName = "Battle Computer";
                    } else if (entity.hasQuirk(OptionsConstants.QUIRK_POS_COMMAND_MEK)) {
                        bestQuirkName = "Command Mek";
                    }
                }
            }
        }
        return bestQuirkName;
    }

    /**
     * @return the best command console/tech officer initiative bonus from this player's units (+2)
     */
    public int getCommandConsoleBonus() {
        if (game == null) {
            return 0;
        }

        for (InGameObject object : game.getInGameObjects()) {
            if (object instanceof Entity entity && entity.getOwner().equals(this)) {
                if (isActiveForCommandBonus(entity)) {
                    if (entity.hasCommandConsoleBonus() || entity.getCrew().hasActiveTechOfficer()) {
                        return 2;
                    }
                }
            }
        }
        return 0;
    }

    /**
     * @return the best crew command skill initiative bonus from this player's units (RPG option)
     */
    public int getCrewCommandBonus() {
        if (game == null) {
            return 0;
        }
        if (!game.getOptions().booleanOption(OptionsConstants.RPG_COMMAND_INIT)) {
            return 0;
        }

        int bonus = 0;
        for (InGameObject object : game.getInGameObjects()) {
            if (object instanceof Entity entity && entity.getOwner().equals(this)) {
                if (isActiveForCommandBonus(entity)) {
                    bonus = Math.max(bonus, entity.getCrew().getCommandBonus());
                }
            }
        }
        return bonus;
    }

    /**
     * @return the bonus to this player's initiative rolls for the highest value initiative (i.e. the 'commander')
     */
    public int getOverallCommandBonus() {
        if (game == null) {
            return 0;
        }
        boolean useCommandInit = game.getOptions().booleanOption(OptionsConstants.RPG_COMMAND_INIT);
        // entities are owned by this player, active, and not individual pilots
        ArrayList<Entity> entities = game.getInGameObjects()
              .stream()
              .filter(Entity.class::isInstance)
              .map(Entity.class::cast)
              .filter(entity -> (null != entity.getOwner()) &&
                    entity.getOwner().equals(this))
              .collect(Collectors.toCollection(ArrayList::new));
        int commandBonus = 0;
        for (Entity entity : entities) {
            int bonus = getIndividualCommandBonus(entity, useCommandInit);
            if (bonus > commandBonus) {
                commandBonus = bonus;
            }
        }
        return commandBonus;
    }

    /**
     * Calculate command bonus for an individual entity within the player's force or team
     * TODO: move all of this into Entity
     *
     * @param entity         being considered
     * @param useCommandInit boolean based on game options
     *
     */
    public int getIndividualCommandBonus(Entity entity, boolean useCommandInit) {
        int bonus = 0;
        // Only consider this during normal rounds when unit is deployed on board, or about to deploy this round.
        if (isActiveForCommandBonus(entity)) {
            if (useCommandInit) {
                bonus = entity.getCrew().getCommandBonus();
            }
            //Even if the RPG option is not enabled, we still get the command bonus provided by special equipment.
            //Since we are not designating a single force commander at this point, we assume a superheavy tripod
            //is the force commander if that gives the highest bonus.
            if (entity.hasCommandConsoleBonus() || entity.getCrew().hasActiveTechOfficer()) {
                bonus += 2;
            }
        }
        return bonus;
    }

    /**
     * Calculate the Triple-Core Processor initiative bonus for this player's force. Per IO pg 81, a TCP-implanted
     * warrior with VDNI/BVDNI provides: - +2 base initiative bonus - +1 additional if unit has CCM, C3/C3i, or >3 tons
     * communications equipment - -1 if unit is shutdown or ECM-affected (unless unit has own ECM for counter-ECM)
     *
     * @return The TCP initiative bonus from the best qualifying entity
     */
    public int getTCPInitBonus() {
        if (game == null) {
            LOGGER.debug("TCP: game is null for player {}", name);
            return 0;
        }

        LOGGER.debug("TCP: Checking for player {} in round {}", name, game.getCurrentRound());

        int bestBonus = 0;
        for (InGameObject object : game.getInGameObjects()) {
            if (!(object instanceof Entity entity)) {
                continue;
            }
            if (!entity.getOwner().equals(this)) {
                continue;
            }
            // Must be deployed and on-board, OR about to deploy next round
            // Uses same pattern as getCommandConsoleBonus() and getCrewCommandBonus()
            if (entity.isDestroyed()) {
                LOGGER.debug("TCP: {} skipped - destroyed", entity.getDisplayName());
                continue;
            }
            boolean eligibleForBonus = (entity.isDeployed() && !entity.isOffBoard()) ||
                  (entity.getDeployRound() == (game.getCurrentRound() + 1));
            if (!eligibleForBonus) {
                LOGGER.debug("TCP: {} skipped - not deployed or deploying next round", entity.getDisplayName());
                continue;
            }
            // Must have TCP + VDNI/BVDNI
            if (!entity.hasAbility(OptionsConstants.MD_TRIPLE_CORE_PROCESSOR)) {
                LOGGER.debug("TCP: {} skipped - no TCP implant", entity.getDisplayName());
                continue;
            }
            if (!entity.hasAbility(OptionsConstants.MD_VDNI)
                  && !entity.hasAbility(OptionsConstants.MD_BVDNI)) {
                LOGGER.debug("TCP: {} skipped - no VDNI/BVDNI", entity.getDisplayName());
                continue;
            }
            // Crew must be active
            if (entity.getCrew() == null || !entity.getCrew().isActive()) {
                LOGGER.debug("TCP: {} skipped - crew not active", entity.getDisplayName());
                continue;
            }

            // Base +2 bonus
            int bonus = 2;

            // +1 for Cockpit Command Module, C3/C3i, or >3 tons communications equipment
            if (hasTCPCommandEquipment(entity)) {
                bonus += 1;
            }

            // Per Xotl ruling: negative modifiers stack cumulatively
            // -1 if shutdown
            if (entity.isShutDown()) {
                bonus -= 1;
            }
            // -1 if ECM-affected, unless unit has own ECM (counter-ECM per IO pg 81)
            if (isEntityECMAffected(entity) && !entity.hasECM()) {
                bonus -= 1;
            }
            // -1 if EMI conditions are active (global effect, can't be countered)
            if (game instanceof Game twGame && twGame.getPlanetaryConditions().getEMI().isEMI()) {
                bonus -= 1;
            }

            LOGGER.debug("TCP: {} qualifies with bonus {} (deployed={}, deployRound={})",
                  entity.getDisplayName(), bonus, entity.isDeployed(), entity.getDeployRound());
            bestBonus = Math.max(bestBonus, bonus);
        }
        LOGGER.debug("TCP: Final TCP bonus for player {}: {}", name, bestBonus);
        return bestBonus;
    }

    /**
     * Check if an entity has command equipment that qualifies for TCP +1 initiative bonus. This includes: Cockpit
     * Command Module, C3/C3i systems, or >3 tons of communications equipment.
     */
    private boolean hasTCPCommandEquipment(Entity entity) {
        // Cockpit Command Module
        if (entity.hasCommandConsoleBonus()) {
            return true;
        }

        // C3 or C3i system
        if (entity.hasAnyC3System()) {
            return true;
        }

        // More than 3 tons of communications equipment
        double commsTonnage = 0;
        for (var m : entity.getMisc()) {
            if (m.getType().hasFlag(MiscType.F_COMMUNICATIONS)) {
                commsTonnage += m.getTonnage();
            }
        }
        return commsTonnage > 3;
    }

    /**
     * Check if an entity is affected by hostile ECM for TCP initiative penalty purposes.
     */
    private boolean isEntityECMAffected(Entity entity) {
        if (entity.getPosition() == null) {
            return false;
        }
        return ComputeECM.isAffectedByECM(entity, entity.getPosition(), entity.getPosition());
    }

    /**
     * Checks if an entity is active and available for command bonus purposes. Entity must be not destroyed, have active
     * crew, not captured, not an ejected pilot, and either deployed on-board or deploying next round.
     *
     * @param entity the entity to check
     *
     * @return true if the entity can provide command bonuses
     */
    private boolean isActiveForCommandBonus(Entity entity) {
        boolean isAlive = !entity.isDestroyed() && entity.getCrew().isActive() && !entity.isCaptured();
        boolean isNotEjectedPilot = !(entity instanceof MekWarrior);
        boolean isDeployedOnBoard = entity.isDeployed() && !entity.isOffBoard();
        boolean isDeployingNextRound = entity.getDeployRound() == (game.getCurrentRound() + 1);

        return isAlive && isNotEjectedPilot && (isDeployedOnBoard || isDeployingNextRound);
    }

    public String getColorForPlayer() {
        return "<B><font color='" + getColour().getHexString(0x00F0F0F0) + "'>" + getName() + "</font></B>";
    }

    public String getColoredPlayerNameWithTeam() {
        if (team == -1) {
            team = 0;
        }
        return "<B><font color='" +
              getColour().getHexString(0x00F0F0F0) +
              "'>" +
              getName() +
              " (" +
              TEAM_NAMES[team] +
              ")</font></B>";
    }

    /**
     * Clears any data from this Player that should not be transmitted to other players from the server, such as email
     * addresses. Note that this changes this Player's data permanently and should typically be done to a copy of the
     * player, see {@link #copy()}.
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

        copy.artyAutoHitHexes = new ArrayList<>(artyAutoHitHexes);

        copy.initialEntityCount = initialEntityCount;
        copy.initialBV = initialBV;

        copy.constantInitBonus = constantInitBonus;
        copy.streakCompensationBonus = streakCompensationBonus;

        copy.camouflage = camouflage;
        copy.colour = colour;

        copy.visibleMinefields = new Vector<>(visibleMinefields);

        copy.admitsDefeat = admitsDefeat;

        copy.setInitiative(getInitiative());

        return copy;
    }

    /**
     * @return The area of the board this player's units are allowed to flee from; An empty area as return value means
     *       they may not flee at all.
     */
    public HexArea getFleeZone() {
        return fleeArea;
    }

    /**
     * Sets the board area this player's units may flee from. The area may be empty, in which case the units may not
     * flee.
     *
     * @param fleeArea The new flee area.
     *
     * @see megamek.common.hexArea.BorderHexArea
     */
    public void setFleeZone(HexArea fleeArea) {
        this.fleeArea = fleeArea;
    }
}
