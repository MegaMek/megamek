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

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * This annotation is used to mark IGame and subclass methods that are intended for use by the Client only
 * (in other words, not by the Server/GameManager). Such methods usually receive information from the
 * server and incorporate it, accepting them as correct. They may fire events.
 */
@Target(ElementType.METHOD)
public @interface ClientOnly {
}
