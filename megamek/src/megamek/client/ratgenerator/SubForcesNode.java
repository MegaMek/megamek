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

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Node while contains rules for generating subForces -- either lower echelons or the actual units if this is a
 * bottom-level echelon.
 *
 * @author Neoancient
 */
public class SubForcesNode extends RulesetNode {
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
                    sub.setEchelon(Integer.parseInt(valueNode.getContent()));
                    apply(sub, i);
                    valueNode.apply(sub, i);
                    subs.add(sub);
                }
                if (!isAttached && forceDescriptor.getSizeMod() == ForceDescriptor.REINFORCED) {
                    ForceDescriptor sub = forceDescriptor.createChild(subs.size());
                    sub.setEchelon(Integer.parseInt(valueNode.getContent()));
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
        for (OptionGroupNode n : optionSubForces) {
            if (n.matches(forceDescriptor)) {
                ValueNode vn = n.selectOption(forceDescriptor);
                if (vn != null) {
                    ArrayList<ForceDescriptor> subs = new ArrayList<>();
                    for (int i = 0; i < vn.getNum(); i++) {
                        if (forceDescriptor.getSizeMod() == ForceDescriptor.UNDERSTRENGTH
                              && i == vn.getNum() / 2) {
                            continue;
                        }
                        ForceDescriptor sub = forceDescriptor.createChild(i);
                        if (vn.getContent().endsWith("+")) {
                            sub.setSizeMod(ForceDescriptor.REINFORCED);
                            sub.setEchelon(Integer.parseInt(vn.getContent().replace("+", "")));
                        } else if (vn.getContent().endsWith("-")) {
                            sub.setSizeMod(ForceDescriptor.UNDERSTRENGTH);
                            sub.setEchelon(Integer.parseInt(vn.getContent().replace("-", "")));
                        } else {
                            sub.setEchelon(Integer.parseInt(vn.getContent()));
                        }
                        apply(sub, i);
                        n.apply(sub, i);
                        vn.apply(sub, i);
                        subs.add(sub);
                    }
                    if (forceDescriptor.getSizeMod() == ForceDescriptor.REINFORCED) {
                        ForceDescriptor sub = forceDescriptor.createChild(subs.size());
                        sub.setEchelon(Integer.parseInt(vn.getContent()));
                        apply(sub, vn.getNum() / 2);
                        n.apply(sub, vn.getNum() / 2);
                        subs.add(sub);

                    }
                    retVal.addAll(subs);
                    if (!isAttached && null == forceDescriptor.getGenerationRule()) {
                        forceDescriptor.setGenerationRule(findGenerateProperty(vn, n, this));
                    }
                }
            }
        }
        return retVal;
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
