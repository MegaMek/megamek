/*
 * Copyright (c) 2020 The MegaMek Team. All rights reserved.
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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek.  If not, see <http://www.gnu.org/licenses/>.
 */

package megamek.common.util;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

import megamek.common.Coords;

/**
 * Class that off-loads serialization related code from Server.java
 */
public class SerializationHelper {
    
    /**
     * Factory method that produces an XStream object suitable for loading MegaMek save games
     */
    public static XStream getXStream() {
        XStream xstream = new XStream();

        // This mirrors the settings is saveGame
        xstream.setMode(XStream.ID_REFERENCES);

        xstream.registerConverter(new Converter() {
            @Override
            public boolean canConvert(Class cls) {
                return (cls == Coords.class);
            }

            @Override
            public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
                int x = 0, y = 0;
                boolean foundX = false, foundY = false;
                while (reader.hasMoreChildren()) {
                    reader.moveDown();
                    switch (reader.getNodeName()) {
                        case "x":
                            x = Integer.parseInt(reader.getValue());
                            foundX = true;
                            break;
                        case "y":
                            y = Integer.parseInt(reader.getValue());
                            foundY = true;
                            break;
                        default:
                            // Unknown node, or <hash>
                            break;
                    }
                    reader.moveUp();
                }
                return (foundX && foundY) ? new Coords(x, y) : null;
            }

            @Override
            public void marshal(Object object, HierarchicalStreamWriter writer, MarshallingContext context) {
                // Unused here
            }
        });
        
        return xstream;
    }
}
