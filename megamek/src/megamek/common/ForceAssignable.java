/*
 * Copyright (c) 2022 - The MegaMek Team. All Rights Reserved.
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

import megamek.common.alphaStrike.AlphaStrikeElement;
import megamek.common.force.Force;

import java.util.List;
import java.util.stream.Collectors;

/**
 * This interface represents all GameObjects that can be assigned to a force in MM. These are at the least
 * Entity and AlphaStrikeElement but might at some point include BFUnits, SBFFormations or ACS Combat Teams.
 *
 * The purpose of this interface is to disentangle Force and Entity and facilitate other types of games than TW.
 */
public interface ForceAssignable extends InGameObject {

    /**
     * Returns a String representation of the force hierarchy this entity belongs to.
     * The String contains all forces from top to bottom separated by backslash
     * with no backslash at beginning or end. Each force is followed by a unique id
     * separated by the vertical bar. E.g.
     * <BR><BR>Regiment|1\Battalion B|11\Alpha Company|18\Battle Lance II|112
     *
     * <BR><BR>If this is not empty, the server will attempt to reconstruct the force
     * hierarchy when it receives this entity and will empty the string.
     * This should be used for loading/saving MULs or transfer from other sources that
     * don't have access to the current MM game's forces, such as MekHQ or the Force
     * Generators. At all other times, forceId should be used instead and this should return
     * an empty string.
     *
     * @return The string representation of this ForceAssignable's force
     */
    String getForceString();

    /**
     * Sets the force string, see {@link #getForceString()}.
     *
     * @param newForceString The new force string
     */
    void setForceString(String newForceString);

    /** @return The unique id of the force this ForceAssignable belongs to. */
    int getForceId();

    /**
     * Sets the unique id of the force this ForceAssignable belongs to.
     *
     * @param newId the id of the new force to assign this ForceAssignable to.
     */
    void setForceId(int newId);

    /** @return True when this ForceAssignable is part of any force. This tests only the assigned force id. */
    default boolean partOfForce() {
        return getForceId() != Force.NO_FORCE;
    }

    /**
     * Filters the given list of ForceAssignables, keeping only those that are an Entity and returns
     * a new list of those Entities.
     *
     * @param forceAssignableList The list of ForceAssignables to filter
     * @return A filtered list of all Entities in the given list
     */
    static List<Entity> filterToEntityList(List<ForceAssignable> forceAssignableList) {
        return forceAssignableList.stream().filter(a -> a instanceof Entity).map(a -> (Entity) a).collect(Collectors.toList());
    }

    /**
     * Filters the given list of ForceAssignables, keeping only those that are an AlphaStrikeElement and returns
     * a new list of those AlphaStrikeElements.
     *
     * @param forceAssignableList The list of ForceAssignables to filter
     * @return A filtered list of all AlphaStrikeElements in the given list
     */
    static List<AlphaStrikeElement> filterToAlphaStrikeElementList(List<ForceAssignable> forceAssignableList) {
        return forceAssignableList.stream()
                .filter(a -> a instanceof AlphaStrikeElement)
                .map(a -> (AlphaStrikeElement) a)
                .collect(Collectors.toList());
    }
}
