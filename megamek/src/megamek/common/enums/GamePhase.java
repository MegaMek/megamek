/*
 * Copyright (C) 2021-2025 The MegaMek Team. All Rights Reserved.
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

package megamek.common.enums;

import java.util.ResourceBundle;

import megamek.MegaMek;
import megamek.common.game.Game;
import megamek.common.options.OptionsConstants;

public enum GamePhase {
    UNKNOWN("GamePhase.UNKNOWN.text"),
    LOUNGE("GamePhase.LOUNGE.text"),
    SELECTION("GamePhase.SELECTION.text"),
    EXCHANGE("GamePhase.EXCHANGE.text"),
    DEPLOYMENT("GamePhase.DEPLOYMENT.text"),
    INITIATIVE("GamePhase.INITIATIVE.text"),
    INITIATIVE_REPORT("GamePhase.INITIATIVE_REPORT.text"),
    SBF_DETECTION("GamePhase.SBF_DETECTION.text"),
    SBF_DETECTION_REPORT("GamePhase.SBF_DETECTION_REPORT.text"),
    TARGETING("GamePhase.TARGETING.text"),
    TARGETING_REPORT("GamePhase.TARGETING_REPORT.text"),
    PREMOVEMENT("GamePhase.PREMOVEMENT.text"),
    MOVEMENT("GamePhase.MOVEMENT.text"),
    MOVEMENT_REPORT("GamePhase.MOVEMENT_REPORT.text"),
    OFFBOARD("GamePhase.OFFBOARD.text"),
    OFFBOARD_REPORT("GamePhase.OFFBOARD_REPORT.text"),
    POINTBLANK_SHOT("GamePhase.POINTBLANK_SHOT.text"), // Fake phase only reached through hidden units
    PRE_FIRING("GamePhase.PREFIRING.text"),
    FIRING("GamePhase.FIRING.text"),
    FIRING_REPORT("GamePhase.FIRING_REPORT.text"),
    PHYSICAL("GamePhase.PHYSICAL.text"),
    PHYSICAL_REPORT("GamePhase.PHYSICAL_REPORT.text"),
    END("GamePhase.END.text"),
    END_REPORT("GamePhase.END_REPORT.text"),
    VICTORY("GamePhase.VICTORY.text"),
    DEPLOY_MINEFIELDS("GamePhase.DEPLOY_MINEFIELDS.text"),
    STARTING_SCENARIO("GamePhase.STARTING_SCENARIO.text"),
    SET_ARTILLERY_AUTO_HIT_HEXES("GamePhase.SET_ARTILLERY_AUTOHIT_HEXES.text");

    private final String name;

    GamePhase(final String name) {
        final ResourceBundle resources = ResourceBundle.getBundle("megamek.common.messages",
              MegaMek.getMMOptions().getLocale());
        this.name = resources.getString(name);
    }

    // region comparison methods
    public boolean isUnknown() {
        return this == UNKNOWN;
    }

    public boolean isLounge() {
        return this == LOUNGE;
    }

    public boolean isSelection() {
        return this == SELECTION;
    }

    public boolean isExchange() {
        return this == EXCHANGE;
    }

    public boolean isDeployment() {
        return this == DEPLOYMENT;
    }

    public boolean isInitiative() {
        return this == INITIATIVE;
    }

    public boolean isInitiativeReport() {
        return this == INITIATIVE_REPORT;
    }

    public boolean isTargeting() {
        return this == TARGETING;
    }

    public boolean isTargetingReport() {
        return this == TARGETING_REPORT;
    }

    public boolean isPremovement() {
        return this == PREMOVEMENT;
    }

    public boolean isMovement() {
        return this == MOVEMENT;
    }

    public boolean isMovementReport() {
        return this == MOVEMENT_REPORT;
    }

    public boolean isOffboard() {
        return this == OFFBOARD;
    }

    public boolean isOffboardReport() {
        return this == OFFBOARD_REPORT;
    }

    public boolean isPointblankShot() {
        return this == POINTBLANK_SHOT;
    }

    public boolean isPreFiring() {
        return this == PRE_FIRING;
    }

    public boolean isFiring() {
        return this == FIRING;
    }

    public boolean isFiringReport() {
        return this == FIRING_REPORT;
    }

    public boolean isPhysical() {
        return this == PHYSICAL;
    }

    public boolean isPhysicalReport() {
        return this == PHYSICAL_REPORT;
    }

    public boolean isEnd() {
        return this == END;
    }

    public boolean isEndReport() {
        return this == END_REPORT;
    }

    public boolean isVictory() {
        return this == VICTORY;
    }

    public boolean isDeployMinefields() {
        return this == DEPLOY_MINEFIELDS;
    }

    public boolean isStartingScenario() {
        return this == STARTING_SCENARIO;
    }

    public boolean isSetArtilleryAutoHitHexes() {
        return this == SET_ARTILLERY_AUTO_HIT_HEXES;
    }

    // endregion

    public boolean isReport() {
        return switch (this) {
            case INITIATIVE_REPORT, TARGETING_REPORT, MOVEMENT_REPORT, OFFBOARD_REPORT, FIRING_REPORT, PHYSICAL_REPORT,
                 END_REPORT, SBF_DETECTION_REPORT, VICTORY -> true;
            default -> false;
        };
    }

    /**
     * Returns true when this phase shows the game map.
     */
    public boolean isOnMap() {
        return switch (this) {
            case UNKNOWN, LOUNGE, SELECTION, EXCHANGE, INITIATIVE, POINTBLANK_SHOT, END -> false;
            default -> true;
        };
    }

    /**
     * @return true if this phase has turns. If false, the phase is simply waiting for everybody to declare "done".
     */
    public boolean usesTurns() {
        return switch (this) {
            case SET_ARTILLERY_AUTO_HIT_HEXES, DEPLOY_MINEFIELDS, DEPLOYMENT, PREMOVEMENT, MOVEMENT, PRE_FIRING, FIRING,
                 PHYSICAL, TARGETING, OFFBOARD -> true;
            default -> false;
        };
    }

    /**
     * @param game The current {@link Game}
     *
     * @return true if this phase is simultaneous
     */
    public boolean isSimultaneous(final Game game) {
        return switch (this) {
            case DEPLOYMENT -> game.getOptions().booleanOption(OptionsConstants.INIT_SIMULTANEOUS_DEPLOYMENT);
            case MOVEMENT -> game.getOptions().booleanOption(OptionsConstants.INIT_SIMULTANEOUS_MOVEMENT);
            case FIRING -> game.getOptions().booleanOption(OptionsConstants.INIT_SIMULTANEOUS_FIRING);
            case PHYSICAL -> game.getOptions().booleanOption(OptionsConstants.INIT_SIMULTANEOUS_PHYSICAL);
            case TARGETING, OFFBOARD -> game.getOptions().booleanOption(OptionsConstants.INIT_SIMULTANEOUS_TARGETING);
            case PREMOVEMENT, PRE_FIRING -> true;
            default -> false;
        };
    }

    public boolean isDuringOrAfter(final GamePhase otherPhase) {
        return compareTo(otherPhase) >= 0;
    }

    public boolean isBefore(final GamePhase otherPhase) {
        return compareTo(otherPhase) < 0;
    }

    @Override
    public String toString() {
        return name();
    }

    public String localizedName() {
        return name;
    }
}
