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

import megamek.common.board.Coords;
import megamek.common.enums.MoveStepType;
import megamek.common.moves.MobileStructureLinkage;
import megamek.common.moves.MovePath;

/** Client and server agree on which movement can leave a sinking mobile deck (TO:AUE p.28). */
public final class MobileStructureNavalRules {
    private MobileStructureNavalRules() { }

    public static MobileStructure deckAt(Entity rider, Coords position, int elevation) {
        if (rider.getGame() == null || position == null || rider instanceof IBuilding) { return null; }
        return rider.getGame().getBoard(rider).getBuildingsAt(position).stream()
              .filter(b -> b instanceof MobileStructure m && m.getNavalState().isSinking()
                    && BuildingElevation.roof(m, position) == elevation)
              .map(b -> (MobileStructure) b).findFirst().orElse(null);
    }

    public static boolean leavingDeck(MobileStructure mobile, Coords destination, int elevation) {
        return destination != null && MobileStructureLinkage.group(mobile).stream().noneMatch(member ->
              member.isIn(destination) && BuildingElevation.roof(member, destination) == elevation);
    }

    public static boolean canSwimOff(Entity rider, int roof) {
        return rider.hasUMU() && (rider instanceof Mek ? roof <= -2 : rider instanceof Infantry && roof <= -1);
    }

    public static boolean canDepart(Entity rider, Coords source, int sourceElevation, Coords destination,
          int destinationElevation, boolean jumping) {
        var mobile = deckAt(rider, source, sourceElevation);
        return mobile == null || !leavingDeck(mobile, destination, destinationElevation)
              || jumping || rider.getMovementMode().isVTOL()
              || canSwimOff(rider, sourceElevation)
              || rider.isAero() && hasFlightDeckAt(rider);
    }

    public static boolean hasFlightDeckAt(Entity rider) {
        return BuildingFlightDeckRules.decksAt(rider.getGame(), rider.getBoardId(), rider.getPosition()).stream()
              .anyMatch(deck -> !deck.helipad() && deck.elevation(rider.getPosition()) == rider.getElevation());
    }

    public static MobileStructure departureCarrier(Entity rider, MovePath path) {
        var mobile = deckAt(rider, rider.getPosition(), rider.getElevation());
        return mobile != null && (leavingDeck(mobile, path.getFinalCoords(), path.getFinalElevation())
              || path.contains(MoveStepType.TAKEOFF) || path.contains(MoveStepType.VERTICAL_TAKE_OFF)) ? mobile : null;
    }
}
