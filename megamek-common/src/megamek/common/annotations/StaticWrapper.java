/*
 * Mockable.java
 *
 * Copyright (c) 2012 Deric "Netzilla" Page <deric dot page at usa dot net>. All rights reserved.
 *
 * This file is part of MZBuilder.
 *
 * MZBuilder is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MZBuilder is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with starmadaBuilder.  If not, see <http://www.gnu.org/licenses/>.
 */

package megamek.common.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * Denotes a method created purely for the purposes of wrapping a static method call in a non-static method so that
 * a mock can be created.
 *
 * @author Deric "Netzilla" Page (deric dot page at usa dot net)
 * @since: 2/20/14 7:51 AM
 * @version: %Id%
 */
@Target({ElementType.METHOD})
@Documented
public @interface StaticWrapper {

}
