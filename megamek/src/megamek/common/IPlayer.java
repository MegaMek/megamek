/**
 * MegaMek - Copyright (C) 2003, 2004 Ben Mazur (bmazur@sev.org)
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

import java.util.Vector;

/**
 * Created with IntelliJ IDEA.
 * User: Deric "Netzilla" Page (deric dot page at usa dot net)
 * Date: 8/17/13
 * Time: 10:17 AM
 * To change this template use File | Settings | File Templates.
 */
public interface IPlayer extends ITurnOrdered {
    public static final int PLAYER_NONE = -1;
    public static final int TEAM_NONE = 0;
    public static final int TEAM_UNASSIGNED = -1;
    public static final String[] colorNames = { "Blue", "Red", "Green", "Cyan",
            "Pink", "Orange", "Gray", "Brown", "Purple", "Turquoise ",
            "Maroon", "Spring Green", "Gold", "Sienna", "Violet", "Navy",
            "Olive Drab", "Fuchsia", "FireBrick", "Dark Golden Rod", "Coral",
            "Chartreuse", "Deep Purple", "Yellow" };
    public static final String[] teamNames = {"No Team", "Team 1", "Team 2",
                                              "Team 3", "Team 4", "Team 5"};
    public static final int MAX_TEAMS = teamNames.length;
    /**
     * The "no camo" category.
     */
    public static final String NO_CAMO = "-- No Camo --";
    /**
     * The category for camos in the root directory.
     */
    public static final String ROOT_CAMO = "-- General --";

    Vector<Minefield> getMinefields();

    void addMinefield(Minefield mf);

    void addMinefields(Vector<Minefield> minefields);

    void removeMinefield(Minefield mf);

    void removeMinefields();

    void removeArtyAutoHitHexes();

    boolean containsMinefield(Minefield mf);

    boolean hasMinefields();

    void setNbrMFConventional(int nbrMF);

    void setNbrMFCommand(int nbrMF);

    void setNbrMFVibra(int nbrMF);

    void setNbrMFActive(int nbrMF);

    void setNbrMFInferno(int nbrMF);

    int getNbrMFConventional();

    int getNbrMFCommand();

    int getNbrMFVibra();

    int getNbrMFActive();

    int getNbrMFInferno();

    void setCamoCategory(String name);

    String getCamoCategory();

    void setCamoFileName(String name);

    String getCamoFileName();

    void setGame(IGame game);

    String getName();

    void setName(String name);

    int getId();

    int getTeam();

    void setTeam(int team);

    boolean isDone();

    void setDone(boolean done);

    boolean isGhost();

    void setGhost(boolean ghost);

    boolean isObserver();

    void setSeeAll(boolean see_all);

    // This simply returns the value, without checking the observer flag
    boolean getSeeAll();

    // If observer is false, see_entire_board does nothing
    boolean canSeeAll();

    void setObserver(boolean observer);

    int getColorIndex();

    void setColorIndex(int index);

    int getStartingPos();

    void setStartingPos(int startingPos);

    /**
     * Set deployment zone to edge of board for reinforcements
     */
    void adjustStartingPosForReinforcements();

    boolean isEnemyOf(IPlayer other);

    void setAdmitsDefeat(boolean admitsDefeat);

    boolean admitsDefeat();
    
    void setAllowTeamChange(boolean allowChange);
    
    boolean isAllowingTeamChange();

    void setArtyAutoHitHexes(Vector<Coords> artyAutoHitHexes);

    Vector<Coords> getArtyAutoHitHexes();

    void addArtyAutoHitHex(Coords c);

    boolean hasTAG();

    /**
     * @return The combined Battle Value of all the player's current assets.
     */
    int getBV();

    /**
     * get the total BV (unmodified by force size mod) for the units of this
     * player that have fled the field
     *
     * @return the BV
     */
    int getFledBV();

    void setInitialBV();

    int getInitialBV();

    void setCompensationInitBonus(int newBonus);

    int getCompensationInitBonus();

    void setConstantInitBonus(int b);

    int getConstantInitBonus();

    /**
     * @return the bonus to this player's initiative rolls granted by his units
     */
    int getTurnInitBonus();

    /**
     * @return the bonus to this player's initiative rolls for
     *         the highest value initiative (i.e. the 'commander')
     */
    int getCommandBonus();

    /**
     * cycle through entities on team and collect all the airborne VTOL/WIGE
     *
     * @return a vector of relevant entity ids
     */
    Vector<Integer> getAirborneVTOL();
    
    // Make sure IPlayer implements both
    boolean equals(Object obj);
    
    int hashCode();
}