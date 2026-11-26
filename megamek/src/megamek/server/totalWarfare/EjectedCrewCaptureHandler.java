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
package megamek.server.totalWarfare;

import java.util.ArrayList;

import jakarta.annotation.Nullable;
import megamek.common.Hex;
import megamek.common.Report;
import megamek.common.compute.InfantryActionStrengths;
import megamek.common.equipment.GunEmplacement;
import megamek.common.interfaces.IEntityRemovalConditions;
import megamek.common.units.AbstractBuildingEntity;
import megamek.common.units.Crew;
import megamek.common.units.EjectedCrew;
import megamek.common.units.Entity;
import megamek.common.units.MekWarrior;
import megamek.common.units.Terrains;
import megamek.logging.MMLogger;

/**
 * Finds the enemy building or gun emplacement whose crew takes an ejected crew prisoner, and captures the ejected
 * crews the general pick-up does not handle.
 *
 * <p>The book has an ejected MekWarrior picked up, and so captured, by an enemy unit sharing its hex in the End
 * Phase (TO:AR p. 165). A building or a gun emplacement is such a unit, but neither is targetable, and the general
 * pick-up only looks at targetable enemies, so it never finds them. {@link TWGameManager#resolveMekWarriorPickUp()}
 * asks this class for the captor of a MekWarrior. Every other ejected crew - a vehicle crew, for one - has no
 * pick-up of its own, and is captured here the same way.</p>
 *
 * <p>An ejected crew takes no part in infantry vs. infantry actions (see
 * {@link InfantryActionStrengths#fightsInInfantryActions(Entity)}), so this is the only thing that happens to one
 * inside an enemy building.</p>
 */
class EjectedCrewCaptureHandler extends AbstractTWRuleHandler {

    private static final MMLogger LOGGER = MMLogger.create(EjectedCrewCaptureHandler.class);

    /** An ejected crew other than a MekWarrior is taken prisoner by the crew of a building. */
    static final int CAPTURED_BY_BUILDING = 6421;

    EjectedCrewCaptureHandler(TWGameManager gameManager) {
        super(gameManager);
    }

    /**
     * Finds an enemy building or gun emplacement whose crew can capture the given ejected crew. The ejected crew
     * may be on any of the building's floors or on its roof, and in any hex of a building of several hexes. A
     * building the ejected crew is already attacking in an infantry action does not capture them; that action
     * decides their fate.
     *
     * @param ejectedCrew the ejected crew
     *
     * @return the capturing building or gun emplacement, or {@code null} if there is none
     */
    @Nullable
    Entity findCapturingBuilding(EjectedCrew ejectedCrew) {
        for (Entity candidate : getGame().getEntitiesVector()) {
            if (!isInSameHexAs(candidate, ejectedCrew) || !candidate.isEnemyOf(ejectedCrew)) {
                continue;
            }
            String refusal = reasonNotToCapture(candidate, ejectedCrew);
            if (refusal == null) {
                return candidate;
            }
            LOGGER.info("[EjectedCrewCapture] {} shares a hex with {} and stays free: {}",
                  ejectedCrew.getShortName(), candidate.getShortName(), refusal);
        }
        return null;
    }

    /**
     * Whether the candidate is a building or gun emplacement standing where the ejected crew is. The position
     * lookup does not reliably hold every hex of a building, so the building itself is asked.
     */
    private static boolean isInSameHexAs(Entity candidate, EjectedCrew ejectedCrew) {
        return switch (candidate) {
            case AbstractBuildingEntity building -> InfantryActionStrengths.isInside(ejectedCrew, building);
            case GunEmplacement gunEmplacement -> (gunEmplacement.getPosition() != null)
                  && gunEmplacement.getPosition().equals(ejectedCrew.getPosition())
                  && (gunEmplacement.getBoardId() == ejectedCrew.getBoardId());
            default -> false;
        };
    }

    /** Why this building takes no prisoner, for the log, or {@code null} when it captures the ejected crew. */
    private @Nullable String reasonNotToCapture(Entity building, EjectedCrew ejectedCrew) {
        boolean isOutOfAction = !building.isDeployed()
              || building.isDestroyed()
              || building.isDoomed()
              || building.isCaptured()
              || building.isShutDown();
        if (isOutOfAction) {
            return "the building is destroyed, captured or shut down";
        }

        // An abandoned building, or one whose crew has been killed or has surrendered, captures nobody. Only
        // buildings lose their crew head by head; a gun emplacement's crew may legitimately have no head-count.
        Crew crew = building.getCrew();
        boolean hasNobodyLeft = (crew == null)
              || !crew.isActive()
              || crew.isDoomed()
              || crew.isEjected()
              || ((building instanceof AbstractBuildingEntity) && (crew.getCurrentSize() <= 0));
        if (hasNobodyLeft) {
            return "the building has no crew left";
        }

        if (ejectedCrew.getInfantryCombatTargetId() == building.getId()) {
            return "an infantry action against the building decides it";
        }

        Hex hex = getGame().getHex(ejectedCrew.getBoardLocation());
        int roof = (hex == null) ? 0 : Math.max(0, hex.terrainLevel(Terrains.BLDG_ELEV));
        boolean isOnAFloorOrRoof = (ejectedCrew.getElevation() >= 0) && (ejectedCrew.getElevation() <= roof);
        return isOnAFloorOrRoof ? null : "the crew is not on one of its floors or its roof";
    }

    /**
     * Captures every ejected crew other than a MekWarrior that an enemy building can hold. MekWarriors are left to
     * {@link TWGameManager#resolveMekWarriorPickUp()}, which lets their own side and other units reach them first.
     * Called once in the End Phase, after that pick-up.
     */
    void captureOtherCrewsInsideEnemyBuildings() {
        for (Entity entity : new ArrayList<>(getGame().getEntitiesVector())) {
            boolean isCrewToCheck = (entity instanceof EjectedCrew) && !(entity instanceof MekWarrior)
                  && isFreeOnTheGround(entity);
            if (!isCrewToCheck) {
                continue;
            }
            Entity captor = findCapturingBuilding((EjectedCrew) entity);
            if (captor != null) {
                capture((EjectedCrew) entity, captor);
            }
        }
    }

    /** On the board, alive, on the ground and not carried. */
    private static boolean isFreeOnTheGround(Entity ejectedCrew) {
        return (ejectedCrew.getPosition() != null)
              && ejectedCrew.isDeployed()
              && !ejectedCrew.isDestroyed()
              && !ejectedCrew.isDoomed()
              && !ejectedCrew.isCaptured()
              && !ejectedCrew.isAirborne()
              && !ejectedCrew.isSpaceborne()
              && (ejectedCrew.getTransportId() == Entity.NONE);
    }

    /**
     * A crew other than a MekWarrior has no record of who carries it, so it leaves play as a captured unit, the
     * way a unit that surrenders with a building does.
     */
    private void capture(EjectedCrew ejectedCrew, Entity captor) {
        Report report = new Report(CAPTURED_BY_BUILDING, Report.PUBLIC);
        report.add(ejectedCrew.getDisplayName());
        report.addDesc(captor);
        addReport(report);
        LOGGER.info("[EjectedCrewCapture] {} is captured by the crew of {}", ejectedCrew.getShortName(),
              captor.getShortName());

        ejectedCrew.setCaptured(true);
        getGame().removeEntity(ejectedCrew.getId(), IEntityRemovalConditions.REMOVE_CAPTURED);
        gameManager.send(gameManager.createRemoveEntityPacket(ejectedCrew.getId(),
              IEntityRemovalConditions.REMOVE_CAPTURED));
    }
}
