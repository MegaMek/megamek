/*
 * MegaMek - Copyright (C) 2000-2002 Ben Mazur (bmazur@sev.org)
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
