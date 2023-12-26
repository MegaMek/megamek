/*
 * Copyright (c) 2023 - The MegaMek Team. All Rights Reserved.
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
import java.util.Enumeration;
import java.util.List;

import org.apache.logging.log4j.LogManager;

import megamek.client.ui.swing.DeploymentDisplay;
import megamek.client.ui.swing.GUIPreferences;
import megamek.client.ui.swing.MovementDisplay;
import megamek.common.enums.GamePhase;

/**
 * Construction Factor Warning Logic.  Handles events, help
 * methods and logic related to CF Warning in a way that
 * can be unit tested and encapsulated from BoardView and
 * ClientGUI and other actors.
 */
public class ConstructionFactorWarning {
    /*
     * Handler for ClientGUI actionPerformed event. Encapsulates
     * as much Construction Factory Warning logic possible.
     */
    public static void handleActionPerformed() {
        toggleCFWarning();
    }

    /*
     * Return true if the passed in phase is a phase that should allow
     * Construction Factor Warnings such as Deploy and Movement.
     */
    public static boolean isCFWarningPhase(GamePhase gp) {
        return (gp == GamePhase.DEPLOYMENT || gp == GamePhase.MOVEMENT);
    }

    public static boolean shouldShow(GamePhase gp) {
        return shouldShow(gp, true);
    }

    /*
     * Returns true if the show construction factor warning preference
     * is enabled and in a phase that should show warnings.
     */
    public static boolean shouldShow(GamePhase gp, boolean isEnabled) {
        return (isEnabled && isCFWarningPhase(gp));
    }

    private static boolean toggleCFWarning() {
        //Toggle the GUI Preference setting for CF Warning setting.
        GUIPreferences GUIP = GUIPreferences.getInstance();
        GUIP.setShowCFWarnings(!GUIP.getShowCFWarnings());
        return (GUIP.getShowCFWarnings());
    }

    /**
     * For the provided entity, find all hexes within movement range with
     * buildings that would collapse if entered or landed upon.  This is
     * used by the {@link MovementDisplay} class.
     *
     * @param g {@link Game} provided by the phase display class
     * @param e {@link Entity} currently selected in the movement phase.
     * @param b {@link Board} board object with building data.
     *
     * @return returns a list of {@link Coords} that where warning flags
     * 		should be placed.
     */
    public static List<Coords> findCFWarningsMovement(Game g, Entity e, Board b) {
        List<Coords> warnList = new ArrayList<Coords>();

        try {
            //Only calculate CF Warnings for entity types in the whitelist.
            if (!entityTypeIsInWhiteList(e)) {
                return warnList;
            }

            Coords pos = e.getPosition();
            int range = (e.getJumpMP() > e.getRunMP()) ? e.getJumpMP() : e.getRunMP();

            List<Coords> hexesToCheck = new ArrayList<Coords>();
            if (pos != null) {
                hexesToCheck = pos.allAtDistanceOrLess(range + 1);
            } else {
                return hexesToCheck;
            }

            // For each hex in jumping range, look for buildings, if found check for collapse.
            for (Coords c : hexesToCheck) {
                //is there a building at this location?  If so add it to hexes with buildings.
                Building bld = b.getBuildingAt(c);

                // If a building, compare total weight and add to warning list.
                if (null != bld) {
                    if (calculateTotalTonnage(g, e, c) > bld.getCurrentCF(c)) {
                        warnList.add(c);
                    }
                }
            }
        } catch (Exception exc) {
            // Something bad is going to happen.  This is a passive feature return an empty list.
            LogManager.getLogger().error("Unable to calculate construction factor collapse candidates. (Movement)");
            return new ArrayList<Coords>();
        }

        return warnList;
    }

    /*
     *  Returns true if the selected entity should have CF warnings calculated when selected.
     */
    protected static boolean entityTypeIsInWhiteList(Entity e) {
        // Include entities that are ground units and onboard only.  Flying units need not apply.
        return (e.isGround() && !e.isOffBoard());
    }

    /**
     * Looks for all building locations in a legal deploy zone that would collapse
     * if the currently selected entity would deploy there.  This is used by
     * {@link DeploymentDisplay} to render a warning sprite on danger hexes.
     *
     * @param g {@link Game} provided by the phase display class
     * @param e {@link Entity} currently selected in the movement phase.
     * @param b {@link Board} board object with building data.
     *
     * @return returns a list of {@link Coords} that where warning flags
     * 		should be placed.
     */
    public static List<Coords> findCFWarningsDeployment(Game g, Entity e, Board b) {
	List<Coords> warnList = new ArrayList<Coords>();

        try {
            //Only calculate CF Warnings for entity types in the whitelist.
            if (!entityTypeIsInWhiteList(e)) {
                return warnList;
            }

            Enumeration<Building> buildings = b.getBuildings();

            // Enumerate through all the buildings
            while (buildings.hasMoreElements()) {
                Building bld = buildings.nextElement();
                List<Coords> buildingList = bld.getCoordsList();

                // For each hex occupied by the building, check if it's a legal deploy hex.
                for (Coords c : buildingList) {
                    if (b.isLegalDeployment(c, e)) {
                        // Check for weight limits for collapse and add a warning sprite.
                        if (calculateTotalTonnage(g, e, c) > bld.getCurrentCF(c)) {
                            warnList.add(c);
                        }
                    }
                }
            }
        } catch (Exception exc) {
            // Something bad is going to happen.  This is a passive feature return an empty list.
            LogManager.getLogger().error("Unable to calculate construction factor collapse candidates. (Deployment)");
            return new ArrayList<Coords>();
        }

        return warnList;
    }

    /*
     *  Determine the total weight burden for a building hex at a location.
     *  This includes the entity current weight summed with any unit weights
     *  at the hex location that could cause a building to collapse.
     */
    protected static double calculateTotalTonnage(Game g, Entity e, Coords c) {
        // Calculate total weight of entity and all entities at the location.
        double totalWeight = e.getWeight();
        List<Entity> units = g.getEntitiesVector(c, true);
        for (Entity ent : units) {
            if (e != ent) {
                totalWeight += ent.getWeight();
            }
        }
        return totalWeight;
    }
}