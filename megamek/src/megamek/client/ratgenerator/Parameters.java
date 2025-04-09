/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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
 */
package megamek.client.ratgenerator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;

import megamek.common.EntityMovementMode;
import megamek.common.annotations.Nullable;
import megamek.logging.MMLogger;

/*
 * A class that holds all the parameters used to generate a table and is used as the key for the cache.
 *
 */
public final class Parameters implements Cloneable {
    private static final MMLogger logger = MMLogger.create(Parameters.class);

    private FactionRecord faction;
    private FactionRecord deployingFaction;

    private int unitType;
    private int year;
    private int networkMask;
    private int roleStrictness;

    private String rating;

    private Collection<Integer> weightClasses;
    private Collection<EntityMovementMode> movementModes;
    private Collection<MissionRole> roles;
    private Collection<MissionRole> rolesExcluded;

    public Parameters(FactionRecord faction, int unitType, int year, String rating, Collection<Integer> weightClasses,
          int networkMask, Collection<EntityMovementMode> movementModes, Collection<MissionRole> roles,
          int roleStrictness, FactionRecord deployingFaction) {
        this.faction = faction;
        this.unitType = unitType;
        this.year = year;
        this.rating = rating;
        this.weightClasses = weightClasses == null ? new ArrayList<>() : new ArrayList<>(weightClasses);
        this.networkMask = networkMask;

        this.movementModes = ((movementModes == null) || movementModes.isEmpty()) ?
                                   EnumSet.noneOf(EntityMovementMode.class) :
                                   EnumSet.copyOf(movementModes);

        this.roles = ((roles == null) || roles.isEmpty()) ? EnumSet.noneOf(MissionRole.class) : EnumSet.copyOf(roles);

        this.rolesExcluded = EnumSet.noneOf((MissionRole.class));

        this.roleStrictness = roleStrictness;
        this.deployingFaction = (deployingFaction == null) ? faction : deployingFaction;
    }

    public Parameters(FactionRecord faction, int unitType, int year, String rating, Collection<Integer> weightClasses,
          int networkMask, Collection<EntityMovementMode> movementModes, Collection<MissionRole> roles,
          Collection<MissionRole> rolesExcluded, int roleStrictness, FactionRecord deployingFaction) {
        this.faction = faction;
        this.unitType = unitType;
        this.year = year;
        this.rating = rating;
        this.weightClasses = weightClasses == null ? new ArrayList<>() : new ArrayList<>(weightClasses);
        this.networkMask = networkMask;

        this.movementModes = ((movementModes == null) || movementModes.isEmpty()) ?
                                   EnumSet.noneOf(EntityMovementMode.class) :
                                   EnumSet.copyOf(movementModes);

        this.roles = ((roles == null) || roles.isEmpty()) ? EnumSet.noneOf(MissionRole.class) : EnumSet.copyOf(roles);

        this.rolesExcluded = ((rolesExcluded == null) || rolesExcluded.isEmpty()) ?
                                   EnumSet.noneOf((MissionRole.class)) :
                                   EnumSet.copyOf(rolesExcluded);

        this.roleStrictness = roleStrictness;
        this.deployingFaction = (deployingFaction == null) ? faction : deployingFaction;
    }

    @Override
    public @Nullable Parameters clone() {
        try {

            Parameters parameters = (Parameters) super.clone();
            parameters.faction = this.faction;
            parameters.deployingFaction = this.deployingFaction;

            parameters.unitType = this.unitType;
            parameters.year = this.year;
            parameters.networkMask = this.networkMask;
            parameters.roleStrictness = this.roleStrictness;

            parameters.rating = this.rating;

            parameters.weightClasses = weightClasses;
            parameters.movementModes = movementModes;
            parameters.roles = this.roles;
            parameters.rolesExcluded = this.rolesExcluded;
            return parameters;
        } catch (CloneNotSupportedException e) {
            logger.error("Failed to clone Parameters. State of the object: {}", this, e);
            return null;
        }
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((deployingFaction == null) ? 0 : deployingFaction.hashCode());
        result = prime * result + ((faction == null) ? 0 : faction.hashCode());
        result = prime * result + ((movementModes == null) ? 0 : movementModes.hashCode());
        result = prime * result + networkMask;
        result = prime * result + ((rating == null) ? 0 : rating.hashCode());
        result = prime * result + roleStrictness;
        result = prime * result + ((roles == null) ? 0 : roles.hashCode());
        result = prime * result + ((rolesExcluded == null) ? 0 : rolesExcluded.hashCode());
        result = prime * result + unitType;
        result = prime * result + ((weightClasses == null) ? 0 : weightClasses.hashCode());
        result = prime * result + year;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj == null) {
            return false;
        }

        if (!(obj instanceof Parameters other)) {
            return false;
        }

        if (deployingFaction == null) {
            if (other.deployingFaction != null) {
                return false;
            }
        } else if (!deployingFaction.equals(other.deployingFaction)) {
            return false;
        }

        if (faction == null) {
            if (other.faction != null) {
                return false;
            }
        } else if (!faction.equals(other.faction)) {
            return false;
        }

        if (movementModes == null) {
            if (other.movementModes != null) {
                return false;
            }
        } else if (!movementModes.equals(other.movementModes)) {
            return false;
        }

        if (networkMask != other.networkMask) {
            return false;
        }

        if (rating == null) {
            if (other.rating != null) {
                return false;
            }
        } else if (!rating.equals(other.rating)) {
            return false;
        }

        if (roleStrictness != other.roleStrictness) {
            return false;
        }

        if (roles == null) {
            if (other.roles != null) {
                return false;
            }
        } else if (!roles.equals(other.roles)) {
            return false;
        }

        if (rolesExcluded == null) {
            if (other.rolesExcluded != null) {
                return false;
            }
        } else if (!rolesExcluded.equals(other.rolesExcluded)) {
            return false;
        }

        if (unitType != other.unitType) {
            return false;
        }

        if (weightClasses == null) {
            if (other.weightClasses != null) {
                return false;
            }
        } else if (!weightClasses.equals(other.weightClasses)) {
            return false;
        }

        return year == other.year;
    }

    public FactionRecord getFaction() {
        return faction;
    }

    public void setFaction(FactionRecord faction) {
        this.faction = faction;
    }

    public int getUnitType() {
        return unitType;
    }

    public void setUnitType(int unitType) {
        this.unitType = unitType;
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public String getRating() {
        return rating;
    }

    public void setRating(String rating) {
        this.rating = rating;
    }

    public Collection<Integer> getWeightClasses() {
        return new ArrayList<>(weightClasses);
    }

    public void setWeightClasses(Collection<Integer> weightClasses) {
        this.weightClasses = weightClasses;
    }

    public int getNetworkMask() {
        return networkMask;
    }

    /**
     * @deprecated no indicated uses.
     */
    @Deprecated(since = "0.50.05", forRemoval = true)
    public void setNetworkMask(int networkMask) {
        this.networkMask = networkMask;
    }

    public Collection<EntityMovementMode> getMovementModes() {
        return new ArrayList<>(movementModes);
    }

    public void setMovementModes(Collection<EntityMovementMode> movementModes) {
        this.movementModes = movementModes;
    }

    public Collection<MissionRole> getRoles() {
        return new ArrayList<>(roles);
    }

    public void setRoles(Collection<MissionRole> roles) {
        this.roles = roles;
    }

    /**
     * @deprecated no indicated uses.
     */
    @Deprecated(since = "0.50.05", forRemoval = true)
    public Collection<MissionRole> getRolesExcluded() {
        return rolesExcluded;
    }

    /**
     * @deprecated no indicated uses.
     */
    @Deprecated(since = "0.50.05", forRemoval = true)
    public void setRolesExcluded(Collection<MissionRole> rolesExcluded) {
        this.rolesExcluded = rolesExcluded;
    }

    public int getRoleStrictness() {
        return roleStrictness;
    }

    /**
     * @deprecated no indicated uses.
     */
    @Deprecated(since = "0.50.05", forRemoval = true)
    public void setRoleStrictness(int roleStrictness) {
        this.roleStrictness = roleStrictness;
    }

    public FactionRecord getDeployingFaction() {
        return deployingFaction;
    }

    /**
     * @deprecated no indicated uses.
     */
    @Deprecated(since = "0.50.05", forRemoval = true)
    public void setDeployingFaction(FactionRecord deployingFaction) {
        this.deployingFaction = deployingFaction;
    }

    public Parameters copy() {
        return new Parameters(faction,
              unitType,
              year,
              rating,
              weightClasses,
              networkMask,
              movementModes,
              roles,
              rolesExcluded,
              roleStrictness,
              deployingFaction);
    }

    @Override
    public String toString() {
        return "Parameters{" +
                     "faction=" +
                     (faction != null ? faction : "null") +
                     ", deployingFaction=" +
                     (deployingFaction != null ? deployingFaction : "null") +
                     ", unitType=" +
                     unitType +
                     ", year=" +
                     year +
                     ", rating=" +
                     (rating != null ? rating : "null") +
                     ", weightClasses=" +
                     (weightClasses != null ? weightClasses : "[]") +
                     ", networkMask=" +
                     networkMask +
                     ", movementModes=" +
                     (movementModes != null ? movementModes : "[]") +
                     ", roles=" +
                     (roles != null ? roles : "[]") +
                     ", rolesExcluded=" +
                     (rolesExcluded != null ? rolesExcluded : "[]") +
                     ", roleStrictness=" +
                     roleStrictness +
                     '}';
    }
}
