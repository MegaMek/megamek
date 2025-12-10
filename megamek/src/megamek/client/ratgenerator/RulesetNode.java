/*
 * Copyright (C) 2016-2025 The MegaMek Team. All Rights Reserved.
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
package megamek.client.ratgenerator;

import java.util.Collection;
import java.util.Objects;
import java.util.Properties;
import java.util.stream.Collectors;

import megamek.common.units.EntityMovementMode;
import megamek.common.units.UnitType;
import megamek.logging.MMLogger;
import org.w3c.dom.Node;

/**
 * Base class of all nodes in the Force Generator faction ruleset files.
 *
 * @author Neoancient
 */
public class RulesetNode {
    private final static MMLogger logger = MMLogger.create(RulesetNode.class);

    protected String name;
    protected Properties predicates;
    protected Properties assertions;

    protected RulesetNode() {
        name = null;
        predicates = new Properties();
        assertions = new Properties();
    }

    public static RulesetNode createFromXml(Node node) {
        RulesetNode retVal = new RulesetNode();
        retVal.loadFromXml(node);
        return retVal;
    }

    protected void loadFromXml(Node node) {
        name = node.getNodeName();
        for (int x = 0; x < node.getAttributes().getLength(); x++) {
            Node wn = node.getAttributes().item(x);
            if (wn.getNodeName().startsWith("if")) {
                predicates.put(wn.getNodeName(), Ruleset.substituteConstants(wn.getTextContent()));
            } else {
                assertions.put(wn.getNodeName(), Ruleset.substituteConstants(wn.getTextContent()));
            }
        }
    }

    /*
     * Allow augmented to be passed separately so the echelon entry in the ruleset
     * TOC
     * can be passed without setting it in the fd.
     */
    public boolean matches(ForceDescriptor fd) {
        return matches(fd, fd.isAugmented());
    }

    public boolean matches(ForceDescriptor fd, boolean augmented) {
        for (Object key : predicates.keySet()) {
            String property = predicates.getProperty((String) key);
            switch ((String) key) {
                case "ifUnitType":
                    if (!matches((fd.getUnitType() == null) ? "" : UnitType.getTypeName(fd.getUnitType()), property)) {
                        return false;
                    }
                    break;
                case "ifWeightClass":
                    if (!matches(fd.getWeightClassCode(), predicates.getProperty((String) key))) {
                        return false;
                    }
                    break;
                case "ifRating":
                    if (!matches(fd.getRating(), predicates.getProperty((String) key))) {
                        return false;
                    }
                    break;
                case "ifRole":
                    if (!collectionMatchesProperty(fd.getRoles().stream()
                          .filter(Objects::nonNull)
                          .map(MissionRole::toString)
                          .collect(Collectors.toList()), predicates.getProperty((String) key))) {
                        return false;
                    }
                    break;
                case "ifFlags":
                    if (!collectionMatchesProperty(fd.getFlags(), predicates.getProperty((String) key))) {
                        return false;
                    }
                    break;
                case "ifMotive":
                    // FIXME: EntityMovementType::toString does not match the property from the file
                    if (!collectionMatchesProperty(fd.getMovementModes().stream()
                          .map(EntityMovementMode::toString)
                          .collect(Collectors.toList()), predicates.getProperty((String) key))) {
                        return false;
                    }
                    break;
                case "ifAugmented":
                    if (predicates.getProperty((String) key).equals("1") != augmented) {
                        return false;
                    }
                    break;
                case "ifDateBetween":
                    if (!matchesDate(fd.getYear(), predicates.getProperty((String) key))) {
                        return false;
                    }
                    break;
                case "ifName":
                    if (property.startsWith("!")) {
                        if ((fd.getName() != null) && matches(fd.getName(), property.replace("!", ""))) {
                            return false;
                        }
                    } else if ((fd.getName() == null) || !matches(fd.getName(), property)) {
                        return false;
                    }
                    break;
                case "ifTopLevel":
                    if (property.equals("1") != fd.isTopLevel()) {
                        return false;
                    }
                    break;
                case "ifFaction":
                    if (!matches(fd.getFaction(), predicates.getProperty((String) key))) {
                        return false;
                    }
                    break;
                case "ifEschelon":
                    if (fd.getEchelon() == null ||
                          !matches(fd.getEchelonCode(), predicates.getProperty((String) key))) {
                        return false;
                    }
                    break;
                case "ifIndex":
                    if (!matches(String.valueOf(fd.getIndex()), predicates.getProperty((String) key))) {
                        return false;
                    }
            }
        }
        return true;
    }

    public boolean matches(String val, String property) {
        if (property.isBlank()) {
            if (val == null) {
                return true;
            } else if (!val.isBlank()) {
                return false;
            }
        }

        if (property.startsWith("!")) {
            return !matches(val, property.replaceFirst("!", ""));
        }
        String[] ands = property.split(",");
        for (String and : ands) {
            String[] ors = and.split("\\|");
            boolean result = false;
            for (String or : ors) {
                if (or.equals(val)) {
                    result = true;
                    break;
                }
            }

            if (!result) {
                return false;
            }
        }
        return true;
    }

    public boolean matchesDate(Integer year, String property) {
        String[] ands = property.split("\\+");
        for (String and : ands) {
            String[] ors = and.split("\\|");
            boolean result = false;
            for (String or : ors) {
                if (or.contains(",")) {
                    String[] dates = or.split(",", 2);
                    if ((dates[0].isBlank() || year >= Integer.parseInt(dates[0]))
                          && (dates[1].isBlank() || year <= Integer.parseInt(dates[1]))) {
                        result = true;
                        break;
                    }
                }
            }
            if (!result) {
                return false;
            }
        }
        return true;
    }

    /**
     * Each csv field of property must be contained in the list for a match.
     *
     */
    public boolean collectionMatchesProperty(Collection<String> list, String property) {
        if (property.isBlank()) {
            return list.isEmpty();
        } else if (property.startsWith("!")) {
            return !collectionMatchesProperty(list, property.replaceFirst("!", ""));
        }
        String[] ands = property.split(",");
        for (String and : ands) {
            String[] ors = and.split("\\|");
            boolean result = false;
            for (String or : ors) {
                for (String s : list) {
                    if (or.equals(s)) {
                        result = true;
                        break;
                    }
                    if (result) {
                        break;
                    }
                }
            }

            if (!result) {
                return false;
            }
        }
        return true;
    }

    public boolean matchesPredicate(String val, String key) {
        if (!predicates.containsKey(key)) {
            return true;
        }
        return matches(val, predicates.getProperty(key));
    }

    public void apply(ForceDescriptor fd, int i) {
        for (Object key : assertions.keySet()) {
            String property = assertions.getProperty((String) key);
            switch ((String) key) {
                case "unitType":
                    int unitType = ModelRecord.parseUnitType(property);
                    if (unitType < 0) {
                        fd.setUnitType(null);
                    } else {
                        fd.setUnitType(unitType);
                    }
                    break;
                case "weightClass":
                    if (property.contains(",")) {
                        String[] weights = property.split(",");
                        fd.setWeightClass(ForceDescriptor.decodeWeightClass(weights[i]));
                    } else if (!property.isBlank()) {
                        fd.setWeightClass(ForceDescriptor.decodeWeightClass(property));
                    }
                    break;
                case "rating":
                    fd.setRating(property);
                    break;
                case "role":
                    if (!property.startsWith("+") && !property.startsWith("-")) {
                        fd.getRoles().clear();
                    }
                    for (String p : property.split(",")) {
                        if (p.startsWith("-")) {
                            fd.getRoles().remove(MissionRole.parseRole(p.replace("-", "")));
                        } else {
                            MissionRole role = MissionRole.parseRole(p.replace("+", ""));
                            if (role != null) {
                                fd.getRoles().add(role);
                            } else {
                                logger.error("Force generator could not parse role {}", p);
                            }
                        }
                    }
                    break;
                case "formation":
                    FormationType ft = FormationType.getFormationType(property);
                    if (null != ft) {
                        fd.setFormationType(ft);
                    } else {
                        logger.warn("Could not parse formation type {}", property);
                    }
                    break;
                case "flags":
                    if (!property.startsWith("+") && !property.startsWith("-")) {
                        fd.getFlags().clear();
                    }
                    for (String p : property.split(",")) {
                        if (p.startsWith("-")) {
                            fd.getFlags().remove(p.replace("-", ""));
                        } else {
                            fd.getFlags().add(p.replace("+", ""));
                        }
                    }
                    break;
                case "motive":
                    if (!property.startsWith("+") && !property.startsWith("-")) {
                        fd.getMovementModes().clear();
                    }
                    for (String p : property.split(",")) {
                        if (p.startsWith("-")) {
                            fd.getMovementModes().remove(EntityMovementMode.parseFromString(p.replace("-", "")));
                        } else {
                            fd.getMovementModes().add(EntityMovementMode.parseFromString(p.replace("+", "")));
                        }
                    }
                    break;
                case "augmented":
                    fd.setAugmented(property.equals("1"));
                    break;
                case "chassis":
                    if (!property.startsWith("+") && !property.startsWith("-")) {
                        fd.getChassis().clear();
                    }
                    for (String p : property.split(",")) {
                        if (p.startsWith("-")) {
                            fd.getChassis().remove(p.replace("-", ""));
                        } else {
                            fd.getChassis().add(p.replace("+", ""));
                        }
                    }
                    break;
                case "model":
                    if (!property.startsWith("+") && !property.startsWith("-")) {
                        fd.getModels().clear();
                    }
                    for (String p : property.split(",")) {
                        if (p.startsWith("-")) {
                            fd.getModels().remove(p.replace("-", ""));
                        } else {
                            fd.getModels().add(p.replace("+", ""));
                        }
                    }
                    break;
                case "variant":
                    if (!property.startsWith("+") && !property.startsWith("-")) {
                        fd.getVariants().clear();
                    }
                    for (String p : property.split(",")) {
                        if (p.startsWith("-")) {
                            fd.getVariants().remove(p.replace("-", ""));
                        } else {
                            fd.getVariants().add(p.replace("+", ""));
                        }
                    }
                    break;
                case "echelon":
                    fd.setEchelon(Integer.parseInt(property));
                    break;
                case "faction":
                    fd.setFaction(property);
                    fd.setRankSystem(null);
                    fd.setTopLevel(true);
                    break;
                case "rankSystem":
                    fd.setRankSystem(Integer.parseInt(property));
                    break;
                case "name":
                    fd.setName(property);
                    break;
                case "fluffName":
                    fd.setFluffName(property);
                    break;
            }
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
