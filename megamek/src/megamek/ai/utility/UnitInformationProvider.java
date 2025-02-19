package megamek.ai.utility;

import megamek.client.bot.princess.CardinalEdge;
import megamek.client.bot.princess.UnitBehavior;
import megamek.common.Coords;
import megamek.common.Entity;
import megamek.common.Targetable;

import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;


/**
 * Interface for providing information about the current unit and other on the board.
 */
public interface UnitInformationProvider {

    /**
     * Gets the set of coordinates in the move path.
     *
     * @return The set of coordinates.
     */
    Set<Coords> getCoordsSet();

    /**
     * Gets the final coordinates of the move path.
     *
     * @return The final coordinates.
     */
    Coords getFinalPosition();

    /**
     * Gets the starting coordinates of the move path.
     *
     * @return The starting coordinates.
     */
    Coords getStartingPosition();

    /**
     * Gets the final facing of the move path.
     *
     * @return The final facing.
     */
    int getFinalFacing();

    /**
     * Gets the distance moved.
     *
     * @return The distance moved.
     */
    int getDistanceMoved();

    /**
     * Gets the hexes moved.
     *
     * @return The hexes moved.
     */
    int getHexesMoved();

    /**
     * Checks if the unit is jumping.
     * @return true if the unit is jumping, false otherwise
     */
    boolean isJumping();

    /**
     * Gets the final altitude of the move path.
     *
     * @return The final altitude.
     */
    int getFinalAltitude();

    /**
     * Gets the probability of success of the move path.
     * @return The probability of success of the move path. 0.0 meas 0% of chance of success, 1.0 means 100% chance of success.
     */
    double getMovePathSuccessProbability();

    /**
     * Gets the maximum run MP of the current unit.
     * @return The maximum run MP of the current unit.
     */
    int getMaxRunMP();

    /**
     * Gets the maximum weapon range of the unit.
     *
     * @return The maximum weapon range of the unit.
     */
    int getMaxWeaponRange();

    /**
     * Gets the behavior type of the unit.
     * @return The behavior type of the unit.
     */
    UnitBehavior.BehaviorType getBehaviorType();

    Entity getCurrentUnit();

    int getTotalHealth();

    int getHeatCapacity();

    Coords getEntityClusterCentroid(Targetable self);
}
