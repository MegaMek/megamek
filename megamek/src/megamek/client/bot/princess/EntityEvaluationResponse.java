/*
 * Copyright (C) 2000-2011 Ben Mazur (bmazur@sev.org)
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
package megamek.client.bot.princess;

import java.text.DecimalFormat;

/**
 * @author Deric "Netzilla" Page (deric dot page at usa dot net)
 * @since 12/10/13 3:19 PM
 */
public class EntityEvaluationResponse {
    private double estimatedEnemyDamage;
    private double myEstimatedDamage;
    private double myEstimatedPhysicalDamage;

    public EntityEvaluationResponse() {
        estimatedEnemyDamage = 0;
        myEstimatedDamage = 0;
        myEstimatedPhysicalDamage = 0;
    }

    public double getEstimatedEnemyDamage() {
        return estimatedEnemyDamage;
    }

    public void setEstimatedEnemyDamage(double estimatedEnemyDamage) {
        this.estimatedEnemyDamage = estimatedEnemyDamage;
    }

    public void addToEstimatedEnemyDamage(double amount) {
        this.estimatedEnemyDamage += amount;
    }

    public double getMyEstimatedDamage() {
        return myEstimatedDamage;
    }

    public void setMyEstimatedDamage(double myEstimatedDamage) {
        this.myEstimatedDamage = myEstimatedDamage;
    }

    public void addToMyEstimatedDamage(double amount) {
        this.myEstimatedDamage += amount;
    }

    public double getMyEstimatedPhysicalDamage() {
        return myEstimatedPhysicalDamage;
    }

    public void setMyEstimatedPhysicalDamage(double myEstimatedPhysicalDamage) {
        this.myEstimatedPhysicalDamage = myEstimatedPhysicalDamage;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof EntityEvaluationResponse)) {
            return false;
        }

        EntityEvaluationResponse that = (EntityEvaluationResponse) o;

        if (Double.compare(that.estimatedEnemyDamage, estimatedEnemyDamage) != 0) {
            return false;
        }
        if (Double.compare(that.myEstimatedDamage, myEstimatedDamage) != 0) {
            return false;
        }
        if (Double.compare(that.myEstimatedPhysicalDamage, myEstimatedPhysicalDamage) != 0) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result;
        long temp;
        temp = Double.doubleToLongBits(estimatedEnemyDamage);
        result = (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(myEstimatedDamage);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(myEstimatedPhysicalDamage);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        return result;
    }

    @Override
    public String toString() {
        DecimalFormat format = new DecimalFormat("0.000");
        return "Enemy: " + format.format(estimatedEnemyDamage)
              + "\tMe: " + format.format(myEstimatedDamage)
              + "\tPhysical: " + format.format(myEstimatedPhysicalDamage);
    }
}
