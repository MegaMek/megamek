/**
 * MegaMek - Copyright (C) 2003 Ben Mazur (bmazur@sev.org)
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

package megamek.common;

import java.util.*;
import java.io.*;

/**
 * A handy utility class for collecting <code>Vectors</code> of
 * <code>GameTurn</code> objects.
 */
public class TurnVectors implements Serializable{
    public Vector non_infantry_non_protomechs = null;
    public Vector infantry_and_protomechs     = null;
    
    public TurnVectors(int non_inf_non_protos, int inf_and_protos)
    {
	non_infantry_non_protomechs = new Vector(non_inf_non_protos);
	infantry_and_protomechs     = new Vector(inf_and_protos);
    }
}
