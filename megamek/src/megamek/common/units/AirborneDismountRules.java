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
package megamek.common.units;

import megamek.common.Hex;
import megamek.common.annotations.Nullable;
import megamek.common.game.Game;
import megamek.common.options.OptionsConstants;

/**
 * Stateless rules for leaving a VTOL or WiGE that has not landed (TW p.225, Dismounting From VTOLs, as replaced by
 * errata v12.0). Infantry with Jump MP may dismount and are placed on the ground, or on the roof in a building hex.
 * Infantry with VTOL MP may dismount and stay at the carrier's elevation. Battle armor that must still eject its
 * missile launchers or detachable weapon packs before it can jump has no Jump MP, so it cannot dismount. No other unit
 * may leave an airborne VTOL or WiGE. Both dismount in the carrier's own hex. The client (Unload button and choices)
 * and the move step legality checked by the server share these checks.
 */
public final class AirborneDismountRules {

    private AirborneDismountRules() {}

    /**
     * Checks whether a carrier using VTOL or WiGE movement (including a LAM in AirMek mode) is airborne at the given
     * elevation in the given hex. A carrier at elevation 0, or resting on a roof or bridge deck, has landed. This
     * matches {@link Entity#isAirborneVTOLorWIGE()}, but for a position and elevation in a planned move rather than the
     * carrier's current ones.
     *
     * @param carrier   the carrying unit
     * @param hex       the hex the carrier is in, may be {@code null}
     * @param elevation the carrier's elevation in that hex
     *
     * @return {@code true} if the carrier is a VTOL or WiGE that has not landed
     */
    public static boolean isCarrierAirborne(Entity carrier, @Nullable Hex hex, int elevation) {
        EntityMovementMode movementMode = carrier.getMovementMode();
        if ((movementMode != EntityMovementMode.VTOL) && (movementMode != EntityMovementMode.WIGE)) {
            return false;
        }
        if (elevation <= 0) {
            return false;
        }
        if (hex == null) {
            return true;
        }
        return (elevation > hex.terrainLevel(Terrains.BLDG_ELEV))
              && (elevation > hex.terrainLevel(Terrains.BRIDGE_ELEV));
    }

    /**
     * Checks whether a carried unit may leave a VTOL or WiGE that has not landed. Infantry with Jump or VTOL MP may
     * (TW p.225, errata v12.0). Battle armor that has not yet ejected its missile launchers or detachable weapon packs
     * reports no Jump MP, so it may not. From a VTOL only, infantry with usable glider wings may also leave as if
     * they were jump infantry (IO p.85), and unarmored conventional infantry may use zip lines when that option is on
     * (TO p.219).
     *
     * @param game      the game, for the zip line option
     * @param carrier   the airborne VTOL or WiGE
     * @param passenger the carried unit
     *
     * @return {@code true} if the unit may dismount while the carrier is airborne
     */
    public static boolean canDismountFromAirborneCarrier(Game game, Entity carrier, Entity passenger) {
        if (!(passenger instanceof Infantry infantry)) {
            return false;
        }
        // VTOL infantry keep their VTOL MP in the Jump MP field
        if (infantry.getJumpMP() > 0) {
            return true;
        }
        if (!(carrier instanceof VTOL)) {
            return false;
        }
        if (infantry.canExitVTOLWithGliderWings()) {
            return true;
        }
        return game.getOptions().booleanOption(OptionsConstants.ADVANCED_GROUND_MOVEMENT_TAC_OPS_ZIPLINES)
              && !infantry.isMechanized();
    }

    /**
     * Returns the elevation a unit dismounting from an airborne VTOL or WiGE is placed at. Infantry with VTOL MP stay
     * at the carrier's elevation; everything else is placed on the roof in a building hex, or on the ground (TW p.225,
     * errata v12.0).
     *
     * @param passenger        the dismounting unit
     * @param hex              the carrier's hex, may be {@code null}
     * @param carrierElevation the carrier's elevation
     *
     * @return the elevation of the dismounted unit
     */
    public static int dismountElevation(Entity passenger, @Nullable Hex hex, int carrierElevation) {
        if (passenger.getMovementMode() == EntityMovementMode.VTOL) {
            return carrierElevation;
        }
        if ((hex != null) && hex.containsTerrain(Terrains.BLDG_ELEV)) {
            return hex.terrainLevel(Terrains.BLDG_ELEV);
        }
        return 0;
    }
}
