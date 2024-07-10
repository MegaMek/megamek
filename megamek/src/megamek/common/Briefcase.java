/*
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
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

import java.io.Serializable;

/**
 * Represents a basic carryable object with no additional other properties
 */
public class Briefcase implements ICarryable, Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 8849879320465375457L;
	
	private double tonnage;
	private String name;
	
	public void setTonnage(double value) {
		tonnage = value;
	}
	
	public double getTonnage() {
		return tonnage;
	}
	
	public void setName(String value) {
		name = value;
	}
	
	public String getName() {
		return name;
	}
	
	public String toString() {
		return name + " (" + tonnage + ")";
	}
}
