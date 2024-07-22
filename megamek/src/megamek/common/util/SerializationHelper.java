/*
 * Copyright (c) 2020-2024 - The MegaMek Team. All Rights Reserved.
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
package megamek.common.util;

import java.util.regex.Pattern;

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

    private SerializationHelper() {
    }

    /**
     * Factory method that produces an XStream object suitable for working with
     * MegaMek save games
     */
    public static XStream getSaveGameXStream() {
        final XStream xStream = new XStream();

        // This will make save games much smaller by using a more efficient means of
        // referencing objects in the XML graph
        xStream.setMode(XStream.ID_REFERENCES);

        xStream.allowTypesByRegExp(new Pattern[] {
                Pattern.compile("\\[C$"),
                Pattern.compile("\\[I$"),
                Pattern.compile("\\[Ljava\\.lang\\.Enum;$"),
                Pattern.compile("java\\.io\\.File$"),
                Pattern.compile("java\\.lang\\.Boolean$"),
                Pattern.compile("java\\.lang\\.Enum$"),
                Pattern.compile("java\\.lang\\.Integer$"),
                Pattern.compile("java\\.lang\\.Double$"),
                Pattern.compile("java\\.lang\\.Number$"),
                Pattern.compile("java\\.lang\\.StringBuffer$"),
                Pattern.compile("java\\.util\\.ArrayList$"),
                Pattern.compile("java\\.util\\.Collections\\$SetFromMap$"),
                Pattern.compile("java\\.util\\.Collections\\$UnmodifiableCollection$"),
                Pattern.compile("java\\.util\\.Collections\\$UnmodifiableList$"),
                Pattern.compile("java\\.util\\.concurrent\\.ConcurrentHashMap$"),
                Pattern.compile("java\\.util\\.concurrent\\.ConcurrentHashMap\\$Segment$"),
                Pattern.compile("java\\.util\\.concurrent\\.CopyOnWriteArrayList$"),
                Pattern.compile("java\\.util\\.concurrent\\.locks\\.AbstractOwnableSynchronizer$"),
                Pattern.compile("java\\.util\\.concurrent\\.locks\\.AbstractQueuedSynchronizer$"),
                Pattern.compile("java\\.util\\.concurrent\\.locks\\.ReentrantLock$"),
                Pattern.compile("java\\.util\\.concurrent\\.locks\\.ReentrantLock\\$NonfairSync$"),
                Pattern.compile("java\\.util\\.concurrent\\.locks\\.ReentrantLock\\$Sync$"),
                Pattern.compile("java\\.util\\.UUID$"),
                Pattern.compile("java\\.util\\.EnumMap$"),
                Pattern.compile("java\\.util\\.EnumSet.*"),
                Pattern.compile("java\\.util\\.HashMap$"),
                Pattern.compile("java\\.util\\.HashSet$"),
                Pattern.compile("java\\.util\\.Hashtable$"),
                Pattern.compile("java\\.util\\.LinkedHashSet$"),
                Pattern.compile("java\\.util\\.LinkedList$"),
                Pattern.compile("java\\.util\\.TreeMap$"),
                Pattern.compile("java\\.util\\.TreeSet$"),
                Pattern.compile("java\\.util\\.Vector$"),
                Pattern.compile("\\[Ljava\\.lang\\.Object;$"),
                Pattern.compile("\\[Ljava\\.lang\\.String;$"),
                Pattern.compile("\\[Ljava\\.util\\.concurrent\\.ConcurrentHashMap\\$Segment;$"),
                Pattern.compile("\\[\\[Lmegamek.*"),
                Pattern.compile("\\[Lmegamek.*"),
                Pattern.compile("megamek.*"),
                Pattern.compile("\\[\\[Lmekhq.*"),
                Pattern.compile("\\[Lmekhq.*"),
                Pattern.compile("mekhq.*"),
                Pattern.compile("\\[\\[Lmegameklab.*"),
                Pattern.compile("\\[Lmegameklab.*"),
                Pattern.compile("megameklab.*"),
                Pattern.compile("\\[Z$")
        });

        xStream.allowTypeHierarchy(megamek.common.BTObject.class);
        xStream.allowTypeHierarchy(megamek.common.Building.class);
        xStream.allowTypeHierarchy(megamek.common.Crew.class);
        xStream.allowTypeHierarchy(megamek.common.GameTurn.class);
        xStream.allowTypeHierarchy(megamek.common.ITechnology.class);
        xStream.allowTypeHierarchy(megamek.common.Roll.class);
        xStream.allowTypeHierarchy(megamek.common.Transporter.class);
        xStream.allowTypeHierarchy(megamek.common.Mounted.class);
        xStream.allowTypeHierarchy(megamek.common.actions.EntityAction.class);
        xStream.allowTypeHierarchy(megamek.common.icons.AbstractIcon.class);
        xStream.allowTypeHierarchy(megamek.common.options.AbstractOptions.class);
        xStream.allowTypeHierarchy(megamek.common.options.IOption.class);
        xStream.allowTypeHierarchy(megamek.common.weapons.AttackHandler.class);
        xStream.allowTypeHierarchy(megamek.server.victory.IVictoryConditions.class);
        xStream.allowTypeHierarchy(megamek.common.strategicBattleSystems.SBFMoveStep.class);
        return xStream;
    }

    /**
     * Factory method that produces an XStream object suitable for loading MegaMek
     * save games
     */
    public static XStream getLoadSaveGameXStream() {
        XStream xStream = getSaveGameXStream();

        xStream.registerConverter(new Converter() {
            @Override
            public boolean canConvert(Class cls) {
                return (cls == Coords.class);
            }

            @Override
            public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
                int x = 0;
                int y = 0;
                boolean foundX = false;
                boolean foundY = false;
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

        return xStream;
    }
}
