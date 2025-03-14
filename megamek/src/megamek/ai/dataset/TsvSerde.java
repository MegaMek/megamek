/*
 * Copyright (C) 2017-2025 The MegaMek Team. All Rights Reserved.
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
package megamek.ai.dataset;

/**
 * <p>Abstract class to serialize/deserialize objects to/from TSV format.</p>
 * <p>It does not have a fromTsv function because I could not find a way to make a good API for it.</p>
 * @param <T> type of object to serialize/deserialize
 * @author Luana Coppio
 */
public abstract class TsvSerde<T> {

    /**
     * Serializes an object to TSV format.
     * @param obj object to serialize
     * @return the object serialized in TSV format
     */
    public abstract String toTsv(T obj);

    /**
     * Returns the header line for the TSV format.
     * @return the header line
     */
    public abstract String getHeaderLine();
}
