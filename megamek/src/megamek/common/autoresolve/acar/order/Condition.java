/*
 * Copyright (c) 2025 - The MegaMek Team. All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 *
 */

package megamek.common.autoresolve.acar.order;

import megamek.common.autoresolve.acar.SimulationContext;

public interface Condition {

    static Condition alwaysTrue() {
        return context -> true;
    }

    static Condition alwaysFalse() {
        return context -> false;
    }

    boolean isMet(SimulationContext context);

}
