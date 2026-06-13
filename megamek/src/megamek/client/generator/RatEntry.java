/*
 * Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
 * Copyright © 2013 Edward Cullen (eddy@obsessedcomputers.co.uk)
 * Copyright (C) 2011-2025 The MegaMek Team. All Rights Reserved.
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
package megamek.client.generator;

import java.util.Vector;

/**
 * Keeps track of a RAT entry, stores the name of a unit in the RAT, and its change of appearing (weight).
 *
 * @author arlith
 */
public class RatEntry {
    private Vector<String> units;
    private Vector<Float> weights;

    RatEntry() {
        setUnits(new Vector<>());
        setWeights(new Vector<>());
    }

    public Vector<String> getUnits() {
        return units;
    }

    public void setUnits(Vector<String> units) {
        this.units = units;
    }

    public Vector<Float> getWeights() {
        return weights;
    }

    public void setWeights(Vector<Float> weights) {
        this.weights = weights;
    }
}
