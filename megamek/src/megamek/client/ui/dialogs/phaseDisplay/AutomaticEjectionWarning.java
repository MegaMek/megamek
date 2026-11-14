/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
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

package megamek.client.ui.dialogs.phaseDisplay;

import java.util.ArrayList;
import java.util.List;

import megamek.client.ui.clientGUI.ClientGUI;
import megamek.client.ui.clientGUI.GUIPreferences;
import megamek.common.game.Game;
import megamek.common.units.AutomaticEjectionRules;
import megamek.common.units.CrewArmorKitRules;
import megamek.common.units.Entity;
import megamek.logging.MMLogger;

/**
 * Decides when to warn that ejecting crews will die, and opens the warning.
 * <p>
 * A unit can come onto the board two ways: deployed in the deployment phase, or walking on from the map edge in the
 * movement phase. Core rules force walk-on for every unit set to an edge zone, so a warning given only at deployment
 * misses most of a Core force. Both displays ask this one object, so the warning comes once per round whichever way
 * the units arrive.
 * <p>
 * One instance lives as long as the client, because the phase displays are rebuilt every phase and cannot remember
 * that the warning was already given.
 */
public class AutomaticEjectionWarning {
    private static final MMLogger LOGGER = MMLogger.create(AutomaticEjectionWarning.class);
    private static final int NOT_WARNED = Integer.MIN_VALUE;

    private final ClientGUI clientGUI;
    private int roundLastWarned = NOT_WARNED;

    /**
     * @param clientGUI the client whose player is warned
     */
    public AutomaticEjectionWarning(ClientGUI clientGUI) {
        this.clientGUI = clientGUI;
    }

    /**
     * Opens the warning if it is due, and reports whether the player backed out.
     * <p>
     * It is due once per round, when the player has not turned it off, the conditions would kill an ejecting crew,
     * and at least one of their units is set to eject a crew that would die.
     *
     * @param arrivalRoute how the unit being placed is coming onto the board, for the log
     *
     * @return {@code true} if the player pressed Cancel, so the deployment or walk-on must not be sent
     */
    public boolean warnIfDue(String arrivalRoute) {
        Game game = clientGUI.getClient().getGame();
        int round = game.getCurrentRound();
        if (roundLastWarned == round) {
            LOGGER.info("[EnvironmentalSealing] {}: ejection warning already given in round {}", arrivalRoute,
                  round);
            return false;
        }
        if (!GUIPreferences.getInstance().getNagForAutoEject()) {
            LOGGER.info("[EnvironmentalSealing] {}: ejection warning turned off by the player", arrivalRoute);
            return false;
        }
        List<Entity> unitsWithEjectionSystems = unitsWithAnEjectionSystem(
              game.getPlayerEntities(clientGUI.getClient().getLocalPlayer(), false));
        if (!isWarningDue(game, unitsWithEjectionSystems)) {
            LOGGER.info("[EnvironmentalSealing] {}: no ejection warning - no crew set to eject would die here",
                  arrivalRoute);
            return false;
        }

        roundLastWarned = round;
        LOGGER.info("[EnvironmentalSealing] {}: warning about auto-ejection in round {} - listing {} unit(s) with an "
              + "ejection system", arrivalRoute, round, unitsWithEjectionSystems.size());
        AutomaticEjectionDialog ejectionDialog = new AutomaticEjectionDialog(clientGUI.getFrame(), clientGUI,
              unitsWithEjectionSystems, game.getPlanetaryConditions().whyLethalToEjectedCrew());
        ejectionDialog.setVisible(true);
        boolean isCancelled = ejectionDialog.isDeploymentCancelled();
        LOGGER.info("[EnvironmentalSealing] {}: ejection warning closed with {}", arrivalRoute,
              isCancelled ? "Cancel" : "Deploy");
        return isCancelled;
    }

    /**
     * Whether the warning has something to say: the conditions would kill an ejecting crew, and at least one of these
     * units is set to eject a crew their kit does not protect.
     * <p>
     * A crew whose kit covers everything out there survives ejecting, so warning about them would be telling the
     * player about a danger they have already dealt with.
     *
     * @param game  the game whose conditions and options apply
     * @param units the player's units with an ejection system
     *
     * @return {@code true} if at least one crew would be killed by ejecting
     */
    public static boolean isWarningDue(Game game, List<Entity> units) {
        if (!game.getPlanetaryConditions().isLethalToEjectedCrew()) {
            return false;
        }
        for (Entity entity : units) {
            if (AutomaticEjectionRules.willEjectAutomatically(entity, game)
                  && !CrewArmorKitRules.survivesEjection(entity, game)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Every unit that has an ejection system, whether or not it is switched on.
     * <p>
     * The dialog lists all of them rather than only the ones at risk, so the player can see the state of the whole
     * force and change any of it.
     *
     * @param units the player's units
     *
     * @return the ones with an ejection system, in the order given
     */
    public static List<Entity> unitsWithAnEjectionSystem(List<Entity> units) {
        List<Entity> withEjectionSystems = new ArrayList<>();
        for (Entity entity : units) {
            if (AutomaticEjectionRules.hasEjectionSystem(entity)) {
                withEjectionSystems.add(entity);
            }
        }
        return withEjectionSystems;
    }
}
