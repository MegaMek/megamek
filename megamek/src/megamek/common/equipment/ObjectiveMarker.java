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
 * <P>The objective variant flags (Potential, False, Fragile, Mobile) are stored here but their rules resolve in
 * later implementation phases; in particular, markers cannot be picked up yet (Mobile Objective carry rules) and are
 * created invulnerable unless a mission enables objective destruction.</P>
 */
public class ObjectiveMarker extends GroundObject {

    @Serial
    private static final long serialVersionUID = 1L;

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

    public ObjectiveMarker() {
        // Objectives cannot be destroyed unless the mission says otherwise; missions that allow
        // destruction clear this (objective destruction resolves in a later implementation phase)
        setInvulnerable(true);
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
     * Objective markers cannot be picked up yet; the Mobile Objective carry rules resolve in a later implementation
     * phase.
     *
     * @param isCarrierHullDown is the unit that's picking this up hull down, or otherwise able to pick up ground-level
     *                          objects
     *
     * @return {@code false}
     */
    @Override
    public boolean canBePickedUp(boolean isCarrierHullDown) {
        return false;
    }

    @Override
    public String specificName() {
        return generalName() + " (Objective)";
    }
}
