/*
 * Copyright (C) 2024-2025 The MegaMek Team. All Rights Reserved.
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

import java.io.Serial;
import java.io.Serializable;

import megamek.common.Report;
import megamek.common.moves.MoveStep;
import megamek.common.units.Entity;
import megamek.common.units.EntityMovementType;
import megamek.server.totalWarfare.TWGameManager;

/**
 * Abstract class for objects that can be placed on the ground but are not entities.
 */
public abstract class GroundObject implements ICarryable, Serializable {
    @Serial
    private static final long serialVersionUID = 8849879320465375457L;

    private double tonnage;
    private String name;
    private boolean invulnerable;
    private int id;
    private int ownerId;


    @Override
    public boolean damage(double amount) {
        tonnage -= amount;
        return tonnage <= 0;
    }

    public void setTonnage(double value) {
        tonnage = value;
    }

    @Override
    public double getTonnage() {
        return tonnage;
    }

    @Override
    public boolean isInvulnerable() {
        return invulnerable;
    }

    public void setInvulnerable(boolean value) {
        invulnerable = value;
    }

    public void setName(String value) {
        name = value;
    }

    @Override
    public String generalName() {
        return name;
    }

    @Override
    public String specificName() {
        return name + " (" + tonnage + " tons)";
    }

    @Override
    public String toString() {
        return specificName();
    }

    @Override
    public int getId() {
        return id;
    }

    @Override
    public void setId(int newId) {
        this.id = newId;
    }

    @Override
    public int getOwnerId() {
        return ownerId;
    }

    @Override
    public void setOwnerId(int newOwnerId) {
        this.ownerId = newOwnerId;
    }

    @Override
    public int getStrength() {
        return 0;
    }

    @Override
    public CarriedObjectDamageAllocation getCarriedObjectDamageAllocation() {
        return CarriedObjectDamageAllocation.ANY_HIT;
    }

    @Override
    public void processPickupStep(MoveStep step, Integer cargoPickupLocation,
          TWGameManager gameManager, Entity entityPickingUpTarget, EntityMovementType overallMoveType) {
        gameManager.getGame().removeGroundObject(step.getPosition(), this);
        entityPickingUpTarget.pickupCarryableObject(this, cargoPickupLocation);

        Report report = new Report(2513);
        report.subject = entityPickingUpTarget.getId();
        report.add(entityPickingUpTarget.getDisplayName());
        report.add(this.specificName());
        report.add(step.getPosition().toFriendlyString());
        gameManager.addReport(report);

        // a pickup should be the last step. Send an update for the overall ground
        // object list.
        gameManager.sendGroundObjectUpdate();
    }
}
