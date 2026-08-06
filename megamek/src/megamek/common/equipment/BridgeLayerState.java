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

import java.io.Serializable;

import megamek.common.annotations.Nullable;
import megamek.common.board.Coords;
import megamek.common.units.ConvInfantry;

/**
 * Mutable per-mount state for a Bridge-Layer (AVLB) {@link MiscMounted}, holding the carried folding bridge's current
 * Construction Factor and the deploy lifecycle. Kept in its own holder (referenced by a single field on
 * {@link MiscMounted}) so the large {@code MiscMounted}/{@code Entity} classes are not bloated with bridgelayer
 * fields.
 *
 * <p>Lifecycle: a bridgelayer starts <em>carried</em> (the folding bridge occupies the unit, absorbs attacks to its
 * location, and blocks weapon fire there). The controlling player may declare a deploy; the unit must then remain
 * stationary and the bridge is placed on the board in the following End Phase, after which the mount is
 * <em>deployed</em> (spent). Construction Factors per TechManual Bridge-Layer table.</p>
 *
 * @author Claude Code (Opus 4.8)
 */
public class BridgeLayerState implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Dedicated log4j2 logger name for all Bridge-Layer (AVLB) diagnostics. AVLB code logs gate/eligibility reasons and
     * state transitions through this name (tagged {@code [AVLB]}) so the whole feature flow can be enabled and grepped
     * from megamek.log without turning on debug for the large host classes (Entity, MoveStep). Enable it in
     * {@code mmconf/log4j2.xml}.
     */
    public static final String DIAGNOSTIC_LOGGER_NAME = "megamek.feature.AVLB";

    /** Carried bridge Construction Factor for the Light Bridge-Layer (TechManual Bridge-Layer table). */
    public static final int LIGHT_BRIDGE_LAYER_CF = 8;

    /** Carried bridge Construction Factor for the Medium Bridge-Layer (TechManual Bridge-Layer table). */
    public static final int MEDIUM_BRIDGE_LAYER_CF = 20;

    /** Carried bridge Construction Factor for the Heavy Bridge-Layer (TechManual Bridge-Layer table). */
    public static final int HEAVY_BRIDGE_LAYER_CF = 45;

    /** Light bridge terrain type level (matches {@link ConvInfantry#BRIDGE_TYPE_LIGHT}). */
    private static final int TERRAIN_TYPE_LIGHT = ConvInfantry.BRIDGE_TYPE_LIGHT;

    /** Medium bridge terrain type level (matches {@link ConvInfantry#BRIDGE_TYPE_MEDIUM}). */
    private static final int TERRAIN_TYPE_MEDIUM = ConvInfantry.BRIDGE_TYPE_MEDIUM;

    /**
     * Heavy bridge terrain type level. The placed bridge's displayed type name (Light/Medium/Heavy) is derived from
     * this terrain level via {@link megamek.common.enums.BuildingType#getType(int)}, so the Heavy variant uses level 3
     * (HEAVY) to avoid being mislabelled as a "Medium" bridge. Bridge images are selected by the exits bitmask, not the
     * type level (the tileset wildcards the level), so any level renders.
     */
    private static final int TERRAIN_TYPE_HEAVY = 3;

    private int currentCF;
    private boolean deployed;
    private boolean deployMechanismDisabled;
    private boolean deployPending;
    /** The hex the bridge will be placed in, or {@code null} when no deploy is pending. */
    private Coords deployTarget;
    private int deployExits;
    private int deployDeclaredTurn = -1;

    /**
     * Creates the carried-bridge state for a bridgelayer mount, initializing the current CF from the equipment
     * variant.
     *
     * @param type the bridgelayer {@link MiscType} (must be a bridgelayer; see {@link #isBridgeLayer(MiscType)})
     */
    public BridgeLayerState(MiscType type) {
        currentCF = initialCF(type);
    }

    /**
     * @param type an equipment type, possibly {@code null}
     *
     * @return {@code true} if the type is any of the Light/Medium/Heavy Bridge-Layer variants
     */
    public static boolean isBridgeLayer(@Nullable MiscType type) {
        return (type != null)
              && (type.hasFlag(MiscType.F_LIGHT_BRIDGE_LAYER)
              || type.hasFlag(MiscType.F_MEDIUM_BRIDGE_LAYER)
              || type.hasFlag(MiscType.F_HEAVY_BRIDGE_LAYER));
    }

    /**
     * @param type a bridgelayer equipment type
     *
     * @return the starting carried-bridge Construction Factor for the variant, or 0 if not a bridgelayer
     */
    public static int initialCF(MiscType type) {
        if (type.hasFlag(MiscType.F_HEAVY_BRIDGE_LAYER)) {
            return HEAVY_BRIDGE_LAYER_CF;
        } else if (type.hasFlag(MiscType.F_MEDIUM_BRIDGE_LAYER)) {
            return MEDIUM_BRIDGE_LAYER_CF;
        } else if (type.hasFlag(MiscType.F_LIGHT_BRIDGE_LAYER)) {
            return LIGHT_BRIDGE_LAYER_CF;
        }
        return 0;
    }

    /**
     * @param type a bridgelayer equipment type
     *
     * @return the board bridge terrain type level placed when this variant deploys (Light = 1, Medium = 2, Heavy = 3).
     *       The deployed bridge's displayed type name is taken from this level, so each variant maps to its matching
     *       level; bridge images are selected by the exits bitmask, not the type level, so any level renders.
     */
    public static int terrainBridgeType(MiscType type) {
        if (type.hasFlag(MiscType.F_LIGHT_BRIDGE_LAYER)) {
            return TERRAIN_TYPE_LIGHT;
        } else if (type.hasFlag(MiscType.F_HEAVY_BRIDGE_LAYER)) {
            return TERRAIN_TYPE_HEAVY;
        }
        return TERRAIN_TYPE_MEDIUM;
    }

    /**
     * @return the carried bridge's current Construction Factor; reduced as attacks are absorbed, and the bridge is
     *       destroyed when it reaches 0
     */
    public int getCurrentCF() {
        return currentCF;
    }

    /**
     * Sets the carried bridge's current Construction Factor, clamped to a minimum of 0.
     *
     * @param constructionFactor the new Construction Factor
     */
    public void setCurrentCF(int constructionFactor) {
        currentCF = Math.max(0, constructionFactor);
    }

    /**
     * @return {@code true} once the folding bridge has been placed on the board and the mount is spent
     */
    public boolean isDeployed() {
        return deployed;
    }

    /**
     * @param deployed whether the folding bridge has been placed on the board
     */
    public void setDeployed(boolean deployed) {
        this.deployed = deployed;
    }

    /**
     * @return {@code true} if a critical hit has disabled the deploy mechanism; the bridge can no longer be deployed
     */
    public boolean isDeployMechanismDisabled() {
        return deployMechanismDisabled;
    }

    /**
     * @param disabled whether a critical hit has disabled the deploy mechanism
     */
    public void setDeployMechanismDisabled(boolean disabled) {
        deployMechanismDisabled = disabled;
    }

    /**
     * @return {@code true} while a deploy has been declared and is awaiting placement in a later End Phase
     */
    public boolean isDeployPending() {
        return deployPending;
    }

    /**
     * @return the hex the bridge will be placed in, or {@code null} when no deploy is pending
     */
    public @Nullable Coords getDeployTarget() {
        return deployTarget;
    }

    /**
     * @return the exits bitmask of the two hexsides the placed bridge will connect
     */
    public int getDeployExits() {
        return deployExits;
    }

    /**
     * @return the game round in which the deploy was declared, or -1 if none is pending
     */
    public int getDeployDeclaredTurn() {
        return deployDeclaredTurn;
    }

    /**
     * Records a pending deploy declaration.
     *
     * @param target       the hex the bridge will be placed in (directly in front of the unit)
     * @param exits        the exits bitmask of the two hexsides the bridge will connect
     * @param declaredTurn the current game round in which the deploy was declared
     */
    public void startDeploy(Coords target, int exits, int declaredTurn) {
        deployPending = true;
        deployTarget = target;
        deployExits = exits;
        deployDeclaredTurn = declaredTurn;
    }

    /** Clears any pending deploy declaration (cancellation or after placement). */
    public void clearPendingDeploy() {
        deployPending = false;
        deployTarget = null;
        deployExits = 0;
        deployDeclaredTurn = -1;
    }
}
