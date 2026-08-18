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

package megamek.common.equipment;

import java.io.Serial;

import megamek.common.annotations.Nullable;
import megamek.common.board.Coords;
import megamek.common.moves.MoveStep;
import megamek.common.units.Entity;
import megamek.common.units.EntityMovementType;
import megamek.common.units.Mek;
import megamek.logging.MMLogger;
import megamek.server.totalWarfare.TWGameManager;

/**
 * An objective counter for objective-based missions (Standard Missions, Objectives): an abstract marker placed on the
 * board that sides compete to control, scan or destroy. Each End Phase, the side with a strict majority of eligible
 * units within the marker's control radius controls it; controlled objectives score Victory Points per the mission's
 * scoring rule.
 *
 * <P>The owning side of the marker is its owner player (see {@link #getOwnerId()}); friendly/enemy is evaluated
 * relative to the scoring side. As a ground object, the marker's position is where it is placed in the game's ground
 * object map.</P>
 *
 * <P>Objectives are destructible by default (destroyed with their building; a mission opts out with
 * {@code destructible: false}). The Potential, False and Fragile variant flags are stored here with their rules
 * resolving in the End Phase objective resolution. Mobile Objectives ride the carryable-cargo mechanics: they are
 * picked up with the cargo pickup movement step, occupy the carrying arm (blocking its attacks), auto-control for
 * the carrier, and are dropped voluntarily or when the carrier is destroyed, falls, becomes immobile or fails the
 * forced-drop Piloting Skill Roll after taking damage.</P>
 */
public class ObjectiveMarker extends GroundObject {

    @Serial
    private static final long serialVersionUID = 1L;

    private static final MMLogger LOGGER = MMLogger.create(ObjectiveMarker.class);

    /** The highest control radius used by the objectives rules (radius is 0, 1 or 2 hexes). */
    public static final int MAX_CONTROL_RADIUS = 2;

    private int controlRadius = 0;
    private int victoryPointValue = 1;
    private boolean potential = false;
    private boolean confirmed = false;
    private boolean falseObjective = false;
    private boolean fragile = false;
    private boolean mobile = false;
    private boolean destroyed = false;
    private boolean insideBuilding = false;
    private boolean buildingLinkInitialized = false;
    private boolean destructionProcessed = false;
    private int controllingTeam = NO_CONTROLLER;
    private int controllingPlayerId = NO_CONTROLLER;
    // the pre-set lobby placement position; null when the marker has no pre-set position
    private Coords lobbyPosition = null;

    /** Value of {@link #getControllingTeam()} / {@link #getControllingPlayerId()} when no side controls this objective. */
    public static final int NO_CONTROLLER = -1;

    public ObjectiveMarker() {
        // RAW (Objectives - Buildings): objectives are destroyed with their building unless the mission
        // states that objectives cannot be destroyed - scenario files opt out with "destructible: false"
        setInvulnerable(false);
    }

    /** @return The control radius of this objective in hexes (0 = only the objective's own hex) */
    public int getControlRadius() {
        return controlRadius;
    }

    /**
     * Sets the control radius of this objective.
     *
     * @param controlRadius The radius in hexes, 0 to {@link #MAX_CONTROL_RADIUS}
     *
     * @throws IllegalArgumentException When the radius is outside the legal range
     */
    public void setControlRadius(int controlRadius) {
        if ((controlRadius < 0) || (controlRadius > MAX_CONTROL_RADIUS)) {
            throw new IllegalArgumentException("Control radius must be between 0 and " + MAX_CONTROL_RADIUS
                  + ", got " + controlRadius);
        }
        this.controlRadius = controlRadius;
    }

    /** @return The victory points this objective is worth when it scores (default 1) */
    public int getVictoryPointValue() {
        return victoryPointValue;
    }

    public void setVictoryPointValue(int victoryPointValue) {
        this.victoryPointValue = victoryPointValue;
    }

    /** @return {@code true} if this is a Potential Objective that must be confirmed before it can score */
    public boolean isPotential() {
        return potential;
    }

    public void setPotential(boolean potential) {
        this.potential = potential;
    }

    /** @return {@code true} if this Potential Objective has been confirmed as a real objective */
    public boolean isConfirmed() {
        return confirmed;
    }

    public void setConfirmed(boolean confirmed) {
        this.confirmed = confirmed;
    }

    /** @return {@code true} if this is a False Objective that may score nothing at mission end */
    public boolean isFalseObjective() {
        return falseObjective;
    }

    public void setFalseObjective(boolean falseObjective) {
        this.falseObjective = falseObjective;
    }

    /** @return {@code true} if this is a Fragile Objective that risks destruction on qualifying events */
    public boolean isFragile() {
        return fragile;
    }

    public void setFragile(boolean fragile) {
        this.fragile = fragile;
    }

    /** @return {@code true} if this is a Mobile Objective that units can pick up and carry */
    public boolean isMobile() {
        return mobile;
    }

    public void setMobile(boolean mobile) {
        this.mobile = mobile;
    }

    /** @return {@code true} if this objective has been destroyed; destroyed objectives no longer score */
    public boolean isDestroyed() {
        return destroyed;
    }

    public void setDestroyed(boolean destroyed) {
        this.destroyed = destroyed;
    }

    /**
     * @return {@code true} if this objective sits inside a building and is destroyed with it. Only meaningful once
     *       {@link #isBuildingLinkInitialized()} is {@code true}; the link is detected in the first End Phase.
     */
    public boolean isInsideBuilding() {
        return insideBuilding;
    }

    public void setInsideBuilding(boolean insideBuilding) {
        this.insideBuilding = insideBuilding;
    }

    /** @return {@code true} once the building link ({@link #isInsideBuilding()}) has been detected */
    public boolean isBuildingLinkInitialized() {
        return buildingLinkInitialized;
    }

    public void setBuildingLinkInitialized(boolean buildingLinkInitialized) {
        this.buildingLinkInitialized = buildingLinkInitialized;
    }

    /**
     * @return {@code true} if this objective's destruction has already been reported; used so a destroyed objective
     *       is reported exactly once regardless of how it was destroyed
     */
    public boolean isDestructionProcessed() {
        return destructionProcessed;
    }

    public void setDestructionProcessed(boolean destructionProcessed) {
        this.destructionProcessed = destructionProcessed;
    }

    /**
     * Damages this objective. Objective counters are abstract markers without a damage capacity: any damage destroys
     * them. Per the {@link ICarryable} contract, this does <i>not</i> check {@link #isInvulnerable()} - callers must
     * do that, and objectives are invulnerable unless the mission allows their destruction.
     *
     * @param amount The damage
     *
     * @return {@code true} - the objective is destroyed by any damage
     */
    @Override
    public boolean damage(double amount) {
        destroyed = true;
        return true;
    }

    /**
     * @return The team currently controlling this objective per the last End Phase control resolution, or
     *       {@link #NO_CONTROLLER} when it is uncontrolled or controlled by an unteamed player (then see
     *       {@link #getControllingPlayerId()})
     */
    public int getControllingTeam() {
        return controllingTeam;
    }

    /**
     * @return The ID of the unteamed player currently controlling this objective per the last End Phase control
     *       resolution, or {@link #NO_CONTROLLER} when it is uncontrolled or controlled by a team
     */
    public int getControllingPlayerId() {
        return controllingPlayerId;
    }

    /**
     * Records the controller of this objective, as resolved in the End Phase. Exactly one of the two values should
     * be set; pass {@link #NO_CONTROLLER} for both when the objective is uncontrolled.
     *
     * @param controllingTeam     The controlling team, or {@link #NO_CONTROLLER}
     * @param controllingPlayerId The controlling unteamed player's ID, or {@link #NO_CONTROLLER}
     */
    public void setController(int controllingTeam, int controllingPlayerId) {
        this.controllingTeam = controllingTeam;
        this.controllingPlayerId = controllingPlayerId;
    }

    /**
     * @return The board position this marker should be placed at when the game starts, as configured in the lobby,
     *       or {@code null} when the marker has no pre-set position (it is then placed by its owner during the
     *       Deploy Minefields phase like other carryable objects). The lobby position is only meaningful while the
     *       marker rides a player's ground-objects-to-place list; once placed, the ground object map holds the
     *       position.
     */
    public @Nullable Coords getLobbyPosition() {
        return lobbyPosition;
    }

    public void setLobbyPosition(@Nullable Coords lobbyPosition) {
        this.lobbyPosition = lobbyPosition;
    }

    /**
     * Only Mobile Objectives can be picked up and carried (Mobile Objectives variant); other objective counters are
     * static. A destroyed counter cannot be picked up.
     *
     * @param isCarrierHullDown is the unit that's picking this up hull down, or otherwise able to pick up ground-level
     *                          objects
     *
     * @return {@code true} for an intact Mobile Objective
     */
    @Override
    public boolean canBePickedUp(boolean isCarrierHullDown) {
        return mobile && !destroyed;
    }

    // CHECKSTYLE IGNORE ForbiddenWords FOR 4 LINES - verbatim quote from the printed Objectives rules
    /**
     * Only Meks can pick up Mobile Objectives (RAW: "any standing mobile 'Mech with at least one functional hand
     * actuator or claw"); pickup attempts by other unit types are refused here as the server-side gate.
     */
    @Override
    public void processPickupStep(MoveStep step, Integer cargoPickupLocation, TWGameManager gameManager,
          Entity entityPickingUpTarget, EntityMovementType overallMoveType) {
        if (!(entityPickingUpTarget instanceof Mek)) {
            LOGGER.debug("[Objective] {} cannot pick up {}: only Meks can carry Mobile Objectives",
                  entityPickingUpTarget.getShortName(), generalName());
            return;
        }
        super.processPickupStep(step, cargoPickupLocation, gameManager, entityPickingUpTarget, overallMoveType);
    }

    @Override
    public String specificName() {
        return generalName() + " (Objective)";
    }
}
