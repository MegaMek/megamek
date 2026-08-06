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

import java.util.ArrayList;

import megamek.codeUtilities.MathUtility;
import megamek.logging.MMLogger;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Node while contains rules for generating subForces -- either lower echelons or the actual units if this is a
 * bottom-level echelon.
 *
 * @author Neoancient
 */
public class SubForcesNode extends RulesetNode {
    private static final MMLogger LOGGER = MMLogger.create(SubForcesNode.class);

    String altFaction;
    boolean parentFaction;
    ArrayList<ValueNode> subForces;
    ArrayList<OptionGroupNode> optionSubForces;

    protected SubForcesNode() {
        altFaction = null;
        parentFaction = false;
        subForces = new ArrayList<>();
        optionSubForces = new ArrayList<>();
    }

    @Deprecated(since = "0.51.0", forRemoval = true)
    public ArrayList<ForceDescriptor> generateSubForces(ForceDescriptor forceDescriptor) {
        return generateSubForces(forceDescriptor, false);
    }

    public ArrayList<ForceDescriptor> generateSubForces(ForceDescriptor forceDescriptor,
          boolean isAttached) {
        ArrayList<ForceDescriptor> retVal = new ArrayList<>();
        for (ValueNode valueNode : subForces) {
            if (valueNode.matches(forceDescriptor)) {
                ArrayList<ForceDescriptor> subs = new ArrayList<>();
                for (int i = 0; i < valueNode.getNum(); i++) {
                    /* Remove the middle weight class to keep the overall weight class
                     * roughly the same.
                     */
                    if (!isAttached && forceDescriptor.getSizeMod() == ForceDescriptor.UNDERSTRENGTH
                          && i == valueNode.getNum() / 2) {
                        continue;
                    }
                    ForceDescriptor sub = forceDescriptor.createChild(i);
                    sub.setEchelon(parseEchelon(valueNode.getContent()));
                    apply(sub, i);
                    valueNode.apply(sub, i);
                    subs.add(sub);
                }
                if (!isAttached && forceDescriptor.getSizeMod() == ForceDescriptor.REINFORCED) {
                    ForceDescriptor sub = forceDescriptor.createChild(subs.size());
                    sub.setEchelon(parseEchelon(valueNode.getContent()));
                    apply(sub, valueNode.getNum() / 2);
                    valueNode.apply(sub, valueNode.getNum() / 2);
                    subs.add(sub);

                }
                retVal.addAll(subs);
                if (!isAttached && null == forceDescriptor.getGenerationRule()) {
                    forceDescriptor.setGenerationRule(findGenerateProperty(valueNode, this));
                }
            }
        }
        for (OptionGroupNode optionGroup : optionSubForces) {
            if (optionGroup.matches(forceDescriptor)) {
                ValueNode valueNode = optionGroup.selectOption(forceDescriptor);
                if (valueNode != null) {
                    if (isAttached) {
                        LOGGER.debug("[ForceGen][Attached] subforceOption picked: parentEschelon={} " +
                                    "parentUnitType={} parentName='{}' optionContent='{}' optionUnitType='{}' " +
                                    "optionName='{}' optionNum={}",
                              forceDescriptor.getEchelon(), forceDescriptor.getUnitType(),
                              forceDescriptor.getName(), valueNode.getContent(),
                              valueNode.assertions.getProperty("unitType"),
                              valueNode.assertions.getProperty("name"), valueNode.getNum());
                    }
                    ArrayList<ForceDescriptor> subs = new ArrayList<>();
                    for (int i = 0; i < valueNode.getNum(); i++) {
                        if (forceDescriptor.getSizeMod() == ForceDescriptor.UNDERSTRENGTH
                              && i == valueNode.getNum() / 2) {
                            continue;
                        }
                        ForceDescriptor sub = forceDescriptor.createChild(i);
                        if (valueNode.getContent().endsWith("+")) {
                            sub.setSizeMod(ForceDescriptor.REINFORCED);
                            sub.setEchelon(parseEchelon(valueNode.getContent().replace("+", "")));
                        } else if (valueNode.getContent().endsWith("-")) {
                            sub.setSizeMod(ForceDescriptor.UNDERSTRENGTH);
                            sub.setEchelon(parseEchelon(valueNode.getContent().replace("-", "")));
                        } else {
                            sub.setEchelon(parseEchelon(valueNode.getContent()));
                        }
                        apply(sub, i);
                        optionGroup.apply(sub, i);
                        valueNode.apply(sub, i);
                        if (isAttached) {
                            LOGGER.debug("[ForceGen][Attached]   created child[{}]: echelon={} " +
                                        "unitType={} name='{}' weightClass={}",
                                  i, sub.getEchelon(), sub.getUnitType(), sub.getName(),
                                  sub.getWeightClass());
                        }
                        subs.add(sub);
                    }
                    if (forceDescriptor.getSizeMod() == ForceDescriptor.REINFORCED) {
                        ForceDescriptor sub = forceDescriptor.createChild(subs.size());
                        sub.setEchelon(parseEchelon(valueNode.getContent()));
                        apply(sub, valueNode.getNum() / 2);
                        optionGroup.apply(sub, valueNode.getNum() / 2);
                        subs.add(sub);

                    }
                    retVal.addAll(subs);
                    if (!isAttached && null == forceDescriptor.getGenerationRule()) {
                        forceDescriptor.setGenerationRule(findGenerateProperty(valueNode, optionGroup, this));
                    }
                }
            }
        }
        return retVal;
    }

    /**
     * Parses an echelon level from a subforce rule's content, tolerating malformed data instead of throwing. Echelon
     * strings in the faction_rules XML are authored integers (any +/- size suffix is stripped by the caller), so a
     * non-numeric value means a malformed ruleset. Rather than abort the whole force generation with a
     * {@link NumberFormatException}, the bad value is logged and echelon 0 is used as a safe fallback.
     *
     * @param content the subforce content to parse (already stripped of any +/- size suffix)
     *
     * @return the parsed echelon, or 0 if the content is not a valid integer
     */
    private int parseEchelon(String content) {
        int echelon = MathUtility.parseInt(content, -1);
        if (echelon < 0) {
            LOGGER.warn("[ForceGen] Malformed echelon value '{}' in a subforce rule; defaulting to 0.", content);
            return 0;
        }
        return echelon;
    }

    /**
     * Used to check the element hierarchy looking for a node with a "generate" assertion set.
     *
     * @param nodes A series of ruleset nodes, sorted from the innermost to the outermost.
     *
     * @return The value of the innermost "generate" property that is set, or null if none are set.
     */
    private String findGenerateProperty(RulesetNode... nodes) {
        for (RulesetNode n : nodes) {
            final String prop = n.assertions.getProperty("generate");
            if (null != prop) {
                return prop;
            }
        }
        return null;
    }

    public String getAltFaction() {
        return altFaction;
    }

    public boolean useParentFaction() {
        return parentFaction;
    }

    public static SubForcesNode createFromXml(Node node) {
        SubForcesNode retVal = new SubForcesNode();
        retVal.loadFromXml(node);
        return retVal;
    }

    @Override
    protected void loadFromXml(Node node) {
        super.loadFromXml(node);

        NodeList nl = node.getChildNodes();
        for (int x = 0; x < nl.getLength(); x++) {
            Node wn = nl.item(x);

            switch (wn.getNodeName()) {
                case "subforce" -> subForces.add(ValueNode.createFromXml(wn));
                case "subforceOption" -> optionSubForces.add(OptionGroupNode.createFromXml(wn));
                case "asFaction" -> altFaction = wn.getTextContent().trim();
                case "asParent" -> parentFaction = true;
            }
        }
    }
}
