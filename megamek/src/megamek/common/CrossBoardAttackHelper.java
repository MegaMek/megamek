/*
 * Copyright (c) 2025 - The MegaMek Team. All Rights Reserved.
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

public final class CrossBoardAttackHelper {

    /**
     * Returns true when the given attacker can possibly attack the given target wherein attacker and target are not on
     * the same board.
     *
     * <P>Note: Returns false when attacker and target are on the same board! Also returns false when attacker or
     * target do not have a valid position, i.e. are undeployed or offboard or transported.</P>
     *
     * <P>Note: This method is strict in that when it returns false, no attack is possible. It
     * may however return true when an attack is possible in principle but may still be hindered by some circumstance.
     * In other words, this method checks for general features such as unit types and a connection between the boards of
     * attacker and target but it does not check ammo availability, disallowed multiple targets, firing arcs etc.</P>
     *
     * @param attacker The attacking unit
     * @param target   The target unit or object
     * @param game     The game object
     *
     * @return True when an attack is possible in principle (but might still be unavailable), false only when an attack
     *       is definitely impossible
     */
    public static boolean isCrossBoardAttackPossible(Entity attacker, Targetable target, Game game) {
        if ((attacker == null) || (target == null) || game.onTheSameBoard(attacker, target)
              || !game.hasBoardLocationOf(attacker) || !game.hasBoardLocationOf(target)) {
            return false;
        }

        if (attacker.isInfantry() || attacker.isProtoMek()) {
            // some infantry can make G2A attacks, see WeaponAttackAction l.2779
            return false;
        }

        // An aero on an atmospheric board in a ground board hex is targetable by ground units along its flight path
        if (Compute.isGroundToAir(attacker, target)
              && game.onDirectlyConnectedBoards(attacker, target)
              && inCorrespondingHexes(game, target, attacker)) {
            return true;
        }

        // An aero on an atmospheric board in a ground board hex is targetable by ground units along its flight path
        if (Compute.isAirToGround(attacker, target)
              && game.onDirectlyConnectedBoards(attacker, target)
              && inCorrespondingHexes(game, attacker, target)) {
            return true;
        }

        // A2A attacks are possible between ground map and atmospheric map
        if (Compute.isAirToAir(game, attacker, target) &&
                  (game.onDirectlyConnectedBoards(attacker, target) ||
                         onGroundMapsWithinOneAtmoMap(game, attacker, target))) {
            return true;
        }

        // A2O fire is possible using capital and sub-capital weapons
        if (attacker.isCapitalScale() &&
                  game.isOnGroundMap(attacker) &&
                  game.isOnSpaceMap(target.getBoardLocation()) &&
                  target.isLargeAerospace()
                  // @@MultiBoardTODO: bring in line with isLargeCraft, SC are not large craft!
                  &&
                  (target instanceof Entity)) {
            // @@MultiBoardTODO: might add some checks; not necessarily capital scale, but the weapons must be
            return true;
        }

        return isOrbitToSurface(attacker, target, game) ||
                     isCrossBoardArtyAttack(attacker, target, game) ||
                     isSurfaceToOrbit(attacker, target, game) ||
                     isAirborneToSurface(attacker, target, game);
    }

    /**
     * Returns true when the flying unit is in the precise hex of an atmospheric board that corresponds to the board of
     * the ground unit. In this case, e.g., the ground unit can make a G2A attack. Note that the flying and ground unit
     * can both be target or attacker depending on the situation (i.e., order matters).
     *
     * @param flying The flying (aero) unit
     * @param ground The ground unit
     *
     * @return True when the flying unit is in the hex "over" the ground unit
     */
    private static boolean inCorrespondingHexes(IGame game, Targetable flying, Targetable ground) {
        return (flying != null)
              && (ground != null)
              && game.isOnGroundMap(ground)
              && game.isOnAtmosphericMap(flying)
              && game.getBoard(flying).getEmbeddedBoardAt(flying.getPosition()) == game.getBoard(ground).getBoardId();
    }

    public static boolean onGroundMapsWithinOneAtmoMap(Game game, Targetable unit1, Targetable unit2) {
        return game.isOnGroundMap(unit1) &&
                     game.isOnGroundMap(unit2) &&
                     game.hasEnclosingBoard(unit1.getBoardId()) &&
                     game.getBoard(unit1).getEnclosingBoardId() == game.getBoard(unit2).getEnclosingBoardId();
    }

    /**
     * Returns true when an attack of the given attacker on the given target is an orbit-to-surface attack. When true,
     * the attack may still be impossible because of ammo, arc, distance etc. but when false, the attack cannot work as
     * an O2S attack.
     *
     * @param attacker The attacking unit for the O2S attack
     * @param target   The target
     * @param game     The game
     *
     * @return True when an attack must be handled as an O2S attack
     */
    public static boolean isOrbitToSurface(Entity attacker, Targetable target, Game game) {
        return (attacker != null) &&
                     (target != null) &&
                     BoardHelper.isTrueSpaceHex(game, game.getBoard(attacker), attacker.getPosition()) &&
                     game.isOnGroundMap(target) &&
                     game.onConnectedBoards(attacker, target) &&
                     target instanceof HexTarget;
    }

    /**
     * Returns true when an attack of the given attacker on the given target is an airborne-to-surface attack. When
     * true, the attack may still be impossible because of ammo, arc, distance etc. but when false, the attack cannot
     * work as an A2S attack.
     *
     * @param attacker The attacking unit for the A2S attack
     * @param target   The target
     * @param game     The game
     *
     * @return True when an attack must be handled as an A2S attack
     */
    public static boolean isAirborneToSurface(Entity attacker, Targetable target, Game game) {
        return (attacker != null) &&
                     (target != null) &&
                     isInAtmosphericTypeHex(attacker, game) &&
                     game.isOnGroundMap(target) &&
                     game.onConnectedBoards(attacker, target) &&
                     target instanceof HexTarget;
    }

    private static boolean isInAtmosphericTypeHex(Targetable targetable, Game game) {
        Board board = game.getBoard(targetable);
        return board.isLowAltitude() ||
                     (board.isSpace() && !BoardHelper.isTrueSpaceHex(game, board, targetable.getPosition()));
    }

    /**
     * Returns true when an attack of the given attacker on the given target is a surface-to-orbit attack. When true,
     * the attack may still be impossible because of ammo, arc, distance etc. but when false, the attack cannot work as
     * an S2O attack.
     *
     * @param attacker The attacking unit for the S2O attack
     * @param target   The target
     * @param game     The game
     *
     * @return True when an attack must be handled as an S2O attack
     */
    public static boolean isSurfaceToOrbit(Entity attacker, Targetable target, Game game) {
        return (attacker != null) &&
                     (target != null) &&
                     game.isOnSpaceMap(target) &&
                     game.isOnGroundMap(attacker) &&
                     game.onConnectedBoards(attacker, target) &&
                     target.isLargeAerospace();
    }

    /**
     * Returns the distance between attacker and target provided they are on different ground map sheets. The distance
     * is given in hexes and can be converted to a map sheet distance by dividing by 17 (see
     * WeaponAttackAction.toHitIsImpossible()). The position of attacker or target on their respective boards does not
     * influence the distance, as the result is supposed to be treated as a multiple of "map sheets". The hex distance
     * is measured as the distance in hexes between the ground maps on their atmospheric map multiplied by 17
     * (Board.DEFAULT_BOARD_HEIGHT). If the ground maps are not on the same atmospheric map but instead two different
     * ones within a common space map, the distance is equal to the hex distance of the atmospheric maps on the space
     * map times 36 * 17 as each space hex measures 18 km and each atmo hex 0.5 km. If attacker and target are on the
     * same map or one of them is not on a ground map or their maps are not connected at all (no common space map), this
     * will return {@link Integer#MAX_VALUE}.
     *
     * @param attacker The attacker
     * @param target   The target (typically a hex)
     * @param game     The game
     *
     * @return The distance between attacker and target measured in hexes
     */
    public static int getCrossBoardGroundMapDistance(Entity attacker, Targetable target, Game game) {
        if (!isCrossBoardArtyAttack(attacker, target, game)) {
            return Integer.MAX_VALUE;
        }
        Board attackerBoard = game.getBoard(attacker);
        Board targetBoard = game.getBoard(target);
        if (!game.hasEnclosingBoard(attackerBoard.getBoardId()) || !game.hasEnclosingBoard(targetBoard.getBoardId())) {
            return Integer.MAX_VALUE;
        }
        if (attackerBoard.getEnclosingBoardId() == targetBoard.getEnclosingBoardId()) {
            // Both ground maps are contained in a low atmosphere map
            Board atmoBoard = game.getEnclosingBoard(attackerBoard);
            Coords attackerBoardPosition = atmoBoard.embeddedBoardPosition(attackerBoard.getBoardId());
            Coords targetBoardPosition = atmoBoard.embeddedBoardPosition(targetBoard.getBoardId());
            return attackerBoardPosition.distance(targetBoardPosition) * Board.DEFAULT_BOARD_HEIGHT;
        } else {
            Board attackerAtmoBoard = game.getEnclosingBoard(attackerBoard);
            Board targetAtmoBoard = game.getEnclosingBoard(targetBoard);
            if (!game.hasEnclosingBoard(attackerAtmoBoard.getBoardId()) ||
                      !game.hasEnclosingBoard(targetAtmoBoard.getBoardId())) {
                return Integer.MAX_VALUE;
            }
            if (attackerAtmoBoard.getEnclosingBoardId() == targetAtmoBoard.getEnclosingBoardId()) {
                // The ground maps are contained in different low atmosphere maps but those in turn in the
                // same high atmo map
                Board spaceBoard = game.getEnclosingBoard(attackerAtmoBoard);
                Coords attackerBoardPosition = spaceBoard.embeddedBoardPosition(attackerAtmoBoard.getBoardId());
                Coords targetBoardPosition = spaceBoard.embeddedBoardPosition(targetAtmoBoard.getBoardId());
                return attackerBoardPosition.distance(targetBoardPosition) * Board.DEFAULT_BOARD_HEIGHT * 36;
            }
        }
        return Integer.MAX_VALUE;
    }

    /**
     * Returns true when the given attacker and target may generally take part in an artillery attack from one ground
     * board to another ground board. When true, the attack may still be impossible because of ammo, arc, distance etc.
     * but when false, the attack is definitely impossible.
     * <p>
     * This method checks for null combatants and if both are on connected but different ground maps
     *
     * @param attacker The attacking unit firing an artillery weapon
     * @param target   The target (hex)
     * @param game     The game
     *
     * @return True when a cross board arty attack is possible in principle, false when it is definitely impossible
     */
    public static boolean isCrossBoardArtyAttack(Entity attacker, Targetable target, Game game) {
        // @@MultiBoardTODO: forbid attacks on different atmo maps as no direction between them exists?
        return (attacker != null) &&
                     (target != null) &&
                     !game.onTheSameBoard(attacker, target) &&
                     game.isOnGroundMap(attacker) &&
                     game.isOnGroundMap(target) &&
                     game.onConnectedBoards(attacker, target);
    }

    private CrossBoardAttackHelper() {
    }
}

