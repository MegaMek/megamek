/*
 * Copyright (C) 2020-2025 The MegaMek Team. All Rights Reserved.
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
 *
 * MechWarrior Copyright Microsoft Corporation. MegaMek was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */

package megamek.common.util;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import megamek.common.TargetRollModifier;
import megamek.common.board.Coords;
import megamek.common.board.CubeCoords;
import megamek.common.equipment.Mounted;
import megamek.common.equipment.NarcPod;
import megamek.common.equipment.Sensor;
import megamek.common.equipment.Transporter;
import megamek.common.game.GameTurn;
import megamek.common.interfaces.ITechnology;
import megamek.common.net.marshalling.SanityInputFilter;
import megamek.common.options.AbstractOptions;
import megamek.common.rolls.Roll;
import megamek.common.units.BTObject;
import megamek.common.units.Crew;
import megamek.common.units.EntityMovementMode;
import megamek.common.units.IBuilding;
import megamek.common.units.InfantryMount;
import megamek.common.weapons.handlers.AttackHandler;
import megamek.server.victory.VictoryCondition;

/**
 * Class that off-loads serialization related code from Server.java
 */
public class SerializationHelper {

    private SerializationHelper() {
    }

    /**
     * Factory method that produces an XStream object suitable for working with MegaMek save games
     */
    public static XStream getSaveGameXStream() {
        final XStream xStream = new XStream();

        // This will make save games much smaller by using a more efficient means of
        // referencing objects in the XML graph
        xStream.setMode(XStream.ID_REFERENCES);

        xStream.allowTypesByRegExp(SanityInputFilter.getFilterList());

        xStream.allowTypeHierarchy(BTObject.class);
        xStream.allowTypeHierarchy(IBuilding.class);
        xStream.allowTypeHierarchy(Crew.class);
        xStream.allowTypeHierarchy(GameTurn.class);
        xStream.allowTypeHierarchy(ITechnology.class);
        xStream.allowTypeHierarchy(Roll.class);
        xStream.allowTypeHierarchy(Transporter.class);
        xStream.allowTypeHierarchy(Mounted.class);
        xStream.allowTypeHierarchy(megamek.common.actions.EntityAction.class);
        xStream.allowTypeHierarchy(megamek.common.icons.AbstractIcon.class);
        xStream.allowTypeHierarchy(AbstractOptions.class);
        xStream.allowTypeHierarchy(megamek.common.options.IOption.class);
        xStream.allowTypeHierarchy(AttackHandler.class);
        xStream.allowTypeHierarchy(VictoryCondition.class);
        xStream.allowTypeHierarchy(megamek.common.strategicBattleSystems.SBFMoveStep.class);
        return xStream;
    }

    /**
     * Factory method that produces an XStream object suitable for loading MegaMek save games
     *
     * @return XStream instance to deserialize into a Game instance
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

        // Necessary because, while Java 17+ supports Record serialization/deserialization, XStream 1.4.x
        // does not (natively).
        xStream.registerConverter(new Converter() {
            @Override
            public boolean canConvert(Class cls) {
                return (cls == NarcPod.class);
            }

            @Override
            public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
                int team = -1;
                int location = -1;
                while (reader.hasMoreChildren()) {
                    reader.moveDown();
                    try {
                        switch (reader.getNodeName()) {
                            case "team" -> team = Integer.parseInt(reader.getValue());
                            case "location" -> location = Integer.parseInt(reader.getValue());
                        }
                        reader.moveUp();
                    } catch (NumberFormatException e) {
                        // Narc Pods with malformed entries will be silently ignored
                        return null;
                    }
                }
                return ((team > -1) && (location > -1)) ? new NarcPod(team, location) : null;
            }

            @Override
            public void marshal(Object object, HierarchicalStreamWriter writer, MarshallingContext context) {
                // Unused here
            }
        });

        xStream.registerConverter(new Converter() {
            @Override
            public boolean canConvert(Class cls) {
                return (cls == Sensor.class);
            }

            @Override
            public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
                int type = -1;
                while (reader.hasMoreChildren()) {
                    reader.moveDown();
                    try {
                        switch (reader.getNodeName()) {
                            case "type" -> type = Integer.parseInt(reader.getValue());
                        }
                        reader.moveUp();
                    } catch (NumberFormatException e) {
                        // Sensors
                        return null;
                    }
                }
                return (type > -1) ? new Sensor(type) : null;
            }

            @Override
            public void marshal(Object object, HierarchicalStreamWriter writer, MarshallingContext context) {
                // Unused here
            }
        });

        xStream.registerConverter(new Converter() {
            @Override
            public boolean canConvert(Class cls) {
                return (cls == InfantryMount.class);
            }

            @Override
            public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
                String name = null;
                InfantryMount.BeastSize size = null;
                double weight = -1.0;
                int movementPoints = -1;
                EntityMovementMode movementMode = null;
                int burstDamage = -1;
                int vehicleDamage = -1;
                double damageDivisor = -1.0;
                int maxWaterDepth = -1;
                int secondaryGroundMP = -1;
                int uwEndurance = -1;
                boolean custom = false;

                while (reader.hasMoreChildren()) {
                    reader.moveDown();
                    try {
                        // 12 fields
                        switch (reader.getNodeName()) {
                            case "name" -> name = reader.getValue();
                            case "size" -> size = InfantryMount.BeastSize.valueOf(reader.getValue());
                            case "weight" -> weight = Double.parseDouble(reader.getValue());
                            case "movementPoints" -> movementPoints = Integer.parseInt(reader.getValue());
                            case "movementMode" -> movementMode = EntityMovementMode.valueOf(reader.getValue());
                            case "burstDamage" -> burstDamage = Integer.parseInt(reader.getValue());
                            case "vehicleDamage" -> vehicleDamage = Integer.parseInt(reader.getValue());
                            case "damageDivisor" -> damageDivisor = Double.parseDouble(reader.getValue());
                            case "maxWaterDepth" -> maxWaterDepth = Integer.parseInt(reader.getValue());
                            case "secondaryGroundMP" -> secondaryGroundMP = Integer.parseInt(reader.getValue());
                            case "uwEndurance" -> uwEndurance = Integer.parseInt(reader.getValue());
                            case "custom" -> custom = Boolean.parseBoolean(reader.getValue());
                        }
                        reader.moveUp();
                    } catch (NumberFormatException e) {
                        // Sensors
                        return null;
                    }
                }
                boolean read = ((name != null) && (size != null) && (movementMode != null) &&
                      (weight > -1.0) && (damageDivisor > -1.0) &&
                      (movementPoints > -1) && (burstDamage > -1) && (vehicleDamage > -1) &&
                      (maxWaterDepth > -1) && (secondaryGroundMP > -1) && (uwEndurance > -1));
                return (read) ? new InfantryMount(
                      name, size, weight, movementPoints, movementMode, burstDamage, vehicleDamage,
                      damageDivisor, maxWaterDepth, secondaryGroundMP, uwEndurance, custom
                ) : null;
            }

            @Override
            public void marshal(Object object, HierarchicalStreamWriter writer, MarshallingContext context) {
                // Unused here
            }
        });

        xStream.registerConverter(new Converter() {
            @Override
            public boolean canConvert(Class cls) {
                return (cls == TargetRollModifier.class);
            }

            @Override
            public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
                Integer value = null;
                String description = "";
                boolean cumulative = false;

                while (reader.hasMoreChildren()) {
                    reader.moveDown();
                    try {
                        switch (reader.getNodeName()) {
                            case "value" -> value = Integer.parseInt(reader.getValue());
                            case "description" -> description = reader.getValue();
                            case "cumulative" -> cumulative = Boolean.parseBoolean(reader.getValue());
                        }
                        reader.moveUp();
                    } catch (NumberFormatException e) {
                        // Narc Pods with malformed entries will be silently ignored
                        return null;
                    }
                }
                return (value != null) ? new TargetRollModifier(value, description, cumulative) : null;
            }

            @Override
            public void marshal(Object object, HierarchicalStreamWriter writer, MarshallingContext context) {
                // Unused here
            }
        });

        xStream.registerConverter(new Converter() {
            @Override
            public boolean canConvert(Class cls) {
                return (cls == CubeCoords.class);
            }

            @Override
            public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
                double q = Double.NaN;
                double r = Double.NaN;
                double s = Double.NaN;

                while (reader.hasMoreChildren()) {
                    reader.moveDown();
                    try {
                        switch (reader.getNodeName()) {
                            case "q" -> q = Double.parseDouble(reader.getValue());
                            case "r" -> r = Double.parseDouble(reader.getValue());
                            case "s" -> s = Double.parseDouble(reader.getValue());
                        }
                        reader.moveUp();
                    } catch (NumberFormatException e) {
                        // CubeCoords with malformed entries will be silently ignored
                        return null;
                    }
                }
                return (!Double.isNaN(q) && !Double.isNaN(r) && !Double.isNaN(s))
                    ? new CubeCoords(q, r, s) : null;
            }

            @Override
            public void marshal(Object object, HierarchicalStreamWriter writer, MarshallingContext context) {
                // Unused here
            }
        });

        return xStream;
    }
}
