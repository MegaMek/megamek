/**
 * MegaMek - Copyright (C) 2004 Ben Mazur (bmazur@sev.org)
 * 
 *  This program is free software; you can redistribute it and/or modify it 
 *  under the terms of the GNU General Public License as published by the Free 
 *  Software Foundation; either version 2 of the License, or (at your option) 
 *  any later version.
 * 
 *  This program is distributed in the hope that it will be useful, but 
 *  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY 
 *  or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License 
 *  for more details.
 */
/*
 * Created on May 10, 2004
 *
 */
package megamek.common;

import megamek.common.actions.*;
/**
 * @author Andrew Hunter
 * A class representing a weapon.
 */
public abstract class Weapon extends WeaponType {
	abstract public AttackHandler fire(WeaponAttackAction waa, Game g);
	abstract public ToHitData toHit(WeaponAttackAction waa,Game g);
}
