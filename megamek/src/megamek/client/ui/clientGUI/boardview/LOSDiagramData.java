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
package megamek.client.ui.clientGUI.boardview;

import java.util.List;

import megamek.common.annotations.Nullable;
import megamek.common.board.Coords;

/**
 * Immutable data model for rendering a LOS elevation diagram between two hexes. Decoupled from the game state so the
 * rendering panel does not depend on game logic.
 *
 * @param hexPath            the ordered list of hex data along the LOS path (attacker to target)
 * @param attackerAbsHeight  the attacker's absolute height (hex floor + unit height)
 * @param targetAbsHeight    the target's absolute height (hex floor + unit height)
 * @param attackPos          the attacker's hex coordinates
 * @param targetPos          the target's hex coordinates
 * @param losBlocked         whether LOS is completely blocked along this path
 * @param attackerUnitType   the attacker's unit type for silhouette rendering
 * @param targetUnitType     the target's unit type for silhouette rendering
 * @param attackerIsHullDown whether the attacker is hull-down (reduces LOS profile by 1 TW level)
 * @param targetIsHullDown   whether the target is hull-down (reduces LOS profile by 1 TW level)
 * @param attackerName       display name of the attacker entity, or empty if none
 * @param targetName         display name of the target entity, or empty if none
 * @param losRuleMode        the active LOS rule set; drives the per-hex comparison level used to flag
 *                           blockers (BMM adjacency rule for {@link LosRuleMode#STANDARD} and
 *                           {@link LosRuleMode#DEAD_ZONE}, linear interp matching the engine's
 *                           {@code losElevation} for {@link LosRuleMode#DIAGRAMMED}). The line itself is
 *                           drawn straight from eye level to eye level in every mode
 * @param deadZone           true if the engine flagged this LOS as blocked by a TacOps dead-zone shadow
 *                           (see {@link megamek.common.LosEffects#isBlockedByDeadZone()}). The panel
 *                           hatches the lower endpoint's hex column as a marker
 * @param deadZoneVictimPos  the lower-elevation endpoint - the unit sitting inside the dead-zone shadow.
 *                           {@code null} when {@code deadZone} is false
 * @param attackerHasMastMount whether the attacker is a VTOL with a working Mast Mount. The Mast Mount raises
 *                             onboard sensors by 1 level for spotting only (TacOps), so the diagram draws a
 *                             "+1 spotting eye" marker one level above the attacker silhouette
 * @param targetHasMastMount   whether the target is a VTOL with a working Mast Mount (same marker as the attacker)
 * @param attackerSpottingClear whether the attacker, spotting from its +1 Mast Mount elevation, has clear LOS to
 *                              the target. Colors the attacker's eye marker. Meaningful only when
 *                              {@code attackerHasMastMount} is true
 * @param targetSpottingClear  whether the target, spotting from its +1 Mast Mount elevation, has clear LOS to the
 *                             attacker. Colors the target's eye marker. Meaningful only when
 *                             {@code targetHasMastMount} is true
 */
record LOSDiagramData(
      List<HexRow> hexPath,
      int attackerAbsHeight,
      int targetAbsHeight,
      Coords attackPos,
      Coords targetPos,
      boolean losBlocked,
      DiagramUnitType attackerUnitType,
      DiagramUnitType targetUnitType,
      boolean attackerIsHullDown,
      boolean targetIsHullDown,
      boolean attackerAtAltitude,
      boolean targetAtAltitude,
      String attackerName,
      String targetName,
      LosRuleMode losRuleMode,
      boolean deadZone,
      @Nullable Coords deadZoneVictimPos,
      boolean attackerHasMastMount,
      boolean targetHasMastMount,
      boolean attackerSpottingClear,
      boolean targetSpottingClear
) {

    /**
     * Returns whether the attacker is a Mek.
     *
     * @return true if the attacker unit type is a Mek
     */
    @Deprecated(since = "0.51.0", forRemoval = true)
    public boolean attackerIsMek() {
        return attackerUnitType.isMek();
    }

    /**
     * Returns whether the target is a Mek.
     *
     * @return true if the target unit type is a Mek
     */
    @Deprecated(since = "0.51.0", forRemoval = true)
    public boolean targetIsMek() {
        return targetUnitType.isMek();
    }

    /**
     * Data for a single hex along the LOS path.
     *
     * @param coords           the hex coordinates
     * @param groundElevation  the hex ground level
     * @param buildingHeight   the number of building levels above ground (0 if none)
     * @param woodsHeight      the foliage elevation above ground (0 if none)
     * @param woodsLevel       the woods density (0=none, 1=light, 2=heavy, 3=ultra)
     * @param jungleLevel      the jungle density (0=none, 1=light, 2=heavy, 3=ultra)
     * @param waterDepth       the water depth below surface level (0 if none)
     * @param smokeLevel       the smoke level (0=none, 1=light, 2=heavy, etc.)
     * @param industrialHeight the industrial zone height above ground (0 if none)
     * @param hasScreen        true if a smoke/ECM screen is present
     * @param hasFields        true if planted fields are present
     * @param hasFire          true if the hex is on fire
     * @param eruptingGeyser   true if an erupting geyser is present (its plume blocks LOS as ultra-heavy woods)
     * @param splitHex         true if this hex was part of a split LOS path (line along hex edge)
     * @param splitAlternate   the alternate hex coordinates if this is a split hex, null otherwise
     * @param blocksLOS        true if this specific hex blocks the LOS line
     * @param losLineElevation the interpolated LOS line elevation at this hex position
     */
    public record HexRow(
          Coords coords,
          int groundElevation,
          int buildingHeight,
          int woodsHeight,
          int woodsLevel,
          int jungleLevel,
          int waterDepth,
          int smokeLevel,
          int industrialHeight,
          boolean hasScreen,
          boolean hasFields,
          boolean hasFire,
          boolean eruptingGeyser,
          boolean splitHex,
          @Nullable Coords splitAlternate,
          boolean blocksLOS,
          double losLineElevation
    ) {

        /**
         * Returns the total height of the tallest feature in this hex (ground + building or woods).
         *
         * @return the maximum elevation of any feature in this hex
         */
        public int topElevation() {
            return groundElevation + Math.max(buildingHeight,
                  Math.max(woodsHeight, industrialHeight));
        }

        /**
         * Returns true if this hex has woods or jungle terrain.
         *
         * @return true if woods or jungle is present
         */
        public boolean hasWoodsOrJungle() {
            return woodsLevel > 0 || jungleLevel > 0;
        }

        /**
         * Returns true if this hex affects LOS beyond just elevation blocking.
         *
         * @return true if the hex contains any LOS-modifying terrain
         */
        public boolean hasLosModifiers() {
            return hasWoodsOrJungle() || smokeLevel > 0 || hasScreen || hasFields
                  || hasFire || industrialHeight > 0 || eruptingGeyser;
        }
    }
}
