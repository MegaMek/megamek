/*
 * MegaMek - Copyright (C) 2000-2011 Ben Mazur (bmazur@sev.org)
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
package megamek.common.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Target;

/**
 * When used on a method, this annotation denotes that the method may return a null value.  As a result, consumers of
 * that value should be prepared to handle nulls.</br>
 * </br>
 * When used on a parameter, this annotation denotes that the parameter may contain a null value.  As a result, any
 * overriding methods need ot be able to handle nulls.
 */
@Target({ElementType.METHOD, ElementType.PARAMETER})
@Documented
@Inherited
public @interface Nullable {

}
