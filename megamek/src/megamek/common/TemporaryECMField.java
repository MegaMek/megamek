/*
 * Copyright (C) 2025-2026 The MegaMek Team. All Rights Reserved.
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
package megamek.common;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

import megamek.common.board.Coords;
import megamek.common.enums.GamePhase;

/**
 * Represents a temporary ECM field that expires after a set duration.
 * <p>
 * EMP mines create temporary ECM bubbles that last until the End Phase of the following turn per Tactical Operations:
 * Advanced Rules (Experimental).
 * </p>
 */
public class TemporaryECMField implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private final Coords position;
    private final int range;
    private final int creationRound;
    private final int expirationRound;
    private final GamePhase expirationPhase;
    private final int playerId;
    private final String source;

    /**
     * Creates a new temporary ECM field.
     *
     * @param position        The center hex of the ECM field
     * @param range           The range in hexes (0 = single hex)
     * @param creationRound   The game round when this field was created
     * @param expirationRound The game round when this field expires
     * @param expirationPhase The phase during which this field expires
     * @param playerId        The player who created this field (or Player.PLAYER_NONE)
     * @param source          Description of the source (e.g., "EMP Mine")
     */
    public TemporaryECMField(Coords position, int range, int creationRound,
          int expirationRound, GamePhase expirationPhase,
          int playerId, String source) {
        this.position = position;
        this.range = range;
        this.creationRound = creationRound;
        this.expirationRound = expirationRound;
        this.expirationPhase = expirationPhase;
        this.playerId = playerId;
        this.source = source;
    }

    /**
     * Creates a temporary ECM field from an EMP mine detonation.
     * <p>
     * Per TO:AR, the ECM bubble lasts until the End Phase of the following turn.
     * </p>
     *
     * @param position     The hex where the EMP mine detonated
     * @param currentRound The current game round
     * @param playerId     The player who deployed the mine
     *
     * @return A new TemporaryECMField configured for EMP mine effects
     */
    public static TemporaryECMField fromEMPMine(Coords position, int currentRound, int playerId) {
        return new TemporaryECMField(
              position,
              0,  // Single hex (0 range)
              currentRound,
              currentRound + 1,  // Expires next round
              GamePhase.END,     // During End Phase
              playerId,
              "EMP Mine"
        );
    }

    /**
     * @return The center position of this ECM field
     */
    public Coords getPosition() {
        return position;
    }

    /**
     * @return The range of this ECM field in hexes (0 = single hex only)
     */
    public int getRange() {
        return range;
    }

    /**
     * @return The round this field was created
     */
    public int getCreationRound() {
        return creationRound;
    }

    /**
     * @return The round this field expires
     */
    public int getExpirationRound() {
        return expirationRound;
    }

    /**
     * @return The phase during which this field expires
     */
    public GamePhase getExpirationPhase() {
        return expirationPhase;
    }

    /**
     * @return The player ID associated with this field
     */
    public int getPlayerId() {
        return playerId;
    }

    /**
     * @return Description of what created this ECM field
     */
    public String getSource() {
        return source;
    }

    /**
     * Checks if this ECM field has expired.
     *
     * @param currentRound The current game round
     * @param currentPhase The current game phase
     *
     * @return true if this field has expired
     */
    public boolean isExpired(int currentRound, GamePhase currentPhase) {
        if (currentRound > expirationRound) {
            return true;
        }
        if (currentRound == expirationRound) {
            return currentPhase.ordinal() >= expirationPhase.ordinal();
        }
        return false;
    }

    /**
     * Checks if this ECM field affects the given coordinates.
     *
     * @param coords The coordinates to check
     *
     * @return true if this ECM field covers the given hex
     */
    public boolean affectsHex(Coords coords) {
        if (coords == null || position == null) {
            return false;
        }
        return position.distance(coords) <= range;
    }

    /**
     * Converts this temporary field to an ECMInfo object for use in ECM calculations.
     * <p>
     * EMP mine ECM is hostile to everyone (including the player who placed the mine),
     * similar to Chaff. This is achieved by passing null as the owner.
     * </p>
     *
     * @return An ECMInfo representing this temporary field, hostile to all players
     */
    public ECMInfo toECMInfo() {
        // Pass null owner to make ECM hostile to everyone (like Chaff)
        // Per ECMInfo: "ECM without an owner is always considered an enemy"
        return new ECMInfo(range, position, null, 1.0, 0);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof TemporaryECMField other)) {
            return false;
        }
        return range == other.range &&
              creationRound == other.creationRound &&
              expirationRound == other.expirationRound &&
              playerId == other.playerId &&
              Objects.equals(position, other.position) &&
              expirationPhase == other.expirationPhase;
    }

    @Override
    public int hashCode() {
        return Objects.hash(position, range, creationRound, expirationRound, expirationPhase, playerId);
    }

    @Override
    public String toString() {
        return "TemporaryECMField{" +
              "position=" + position +
              ", range=" + range +
              ", source='" + source + '\'' +
              ", expiresRound=" + expirationRound +
              ", expiresPhase=" + expirationPhase +
              '}';
    }
}
