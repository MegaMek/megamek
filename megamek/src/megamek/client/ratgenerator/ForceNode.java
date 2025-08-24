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
import java.util.stream.Collectors;

import megamek.common.units.EntityMovementMode;
import megamek.logging.MMLogger;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * A force node contains the rules for generating a force when the ForceDescriptor matches the characteristics defined
 * by the force node.
 *
 * @author Neoancient
 */
public class ForceNode extends RulesetNode {
    private final static MMLogger logger = MMLogger.create(ForceNode.class);

    protected Integer echelon;
    protected String echelonName;
    protected ArrayList<ValueNode> nameNodes;
    protected ArrayList<CommanderNode> coNodes;
    protected ArrayList<CommanderNode> xoNodes;
    protected ArrayList<ArrayList<OptionGroupNode>> ruleGroups;
    protected ArrayList<SubForcesNode> subForces;
    protected ArrayList<SubForcesNode> attached;

    protected String desc;

    private ForceNode() {
        super();
        echelon = null;
        echelonName = null;
        nameNodes = new ArrayList<>();
        coNodes = new ArrayList<>();
        xoNodes = new ArrayList<>();
        ruleGroups = new ArrayList<>();
        subForces = new ArrayList<>();
        attached = new ArrayList<>();
    }

    public boolean apply(ForceDescriptor fd) {
        for (ArrayList<OptionGroupNode> group : ruleGroups) {
            ArrayList<OptionGroupNode> toApply = new ArrayList<>();
            for (OptionGroupNode rule : group) {
                if (rule.matches(fd)) {
                    toApply.add(rule);
                }
            }
            for (OptionGroupNode rule : toApply) {
                ValueNode valueNode;
                String content;

                switch (rule.getName()) {
                    case "weightClass":
                        if (fd.getWeightClass() == null
                              || rule.predicates.containsKey("ifWeightClass")) {
                            valueNode = rule.selectOption(fd, true);
                            if (valueNode != null) {
                                fd.setWeightClass(ForceDescriptor.decodeWeightClass(valueNode.getContent()));
                            }
                        }
                        break;
                    case "unitType":
                        if (fd.getUnitType() == null
                              || rule.predicates.containsKey("ifUnitType")) {
                            valueNode = rule.selectOption(fd, true);
                            if (valueNode != null) {
                                fd.setUnitType(ModelRecord.parseUnitType(valueNode.getContent()));
                            }
                        }
                        break;
                    case "chassis":
                        if (fd.getChassis().isEmpty()
                              || rule.predicates.containsKey("ifChassis")) {
                            valueNode = rule.selectOption(fd, true);
                            if (valueNode != null) {
                                for (String c : valueNode.getContent().split(",")) {
                                    fd.getChassis().add(c);
                                }
                            }
                        }
                        break;
                    case "variant":
                        if (fd.getVariants().isEmpty() || rule.predicates.containsKey("ifVariant")) {
                            valueNode = rule.selectOption(fd, true);
                            if (valueNode != null) {
                                for (String c : valueNode.getContent().split(",")) {
                                    fd.getVariants().add(c);
                                }
                            }
                        }
                        break;
                    case "motive":
                        valueNode = rule.selectOption(fd, true);
                        if (valueNode == null) {
                            break;
                        }
                        content = valueNode.getContent();
                        if (content.startsWith("-")) {
                            for (String p : content.replaceFirst("-", "").split(",")) {
                                fd.getMovementModes().remove(EntityMovementMode.parseFromString(p));
                            }
                            break;
                        }
                        if (content.startsWith("+")) {
                            content = content.replace("+", "");
                        } else {
                            fd.getMovementModes().clear();
                        }
                        for (String p : content.split(",")) {
                            fd.getMovementModes().add(EntityMovementMode.parseFromString(p));
                        }
                        break;
                    case "formation":
                        if (null == fd.getFormation()
                              || rule.predicates.containsKey("ifFormation")) {
                            valueNode = rule.selectOption(fd, true);
                            if (valueNode == null) {
                                break;
                            }
                            content = valueNode.getContent();
                            if (content != null) {
                                FormationType ft = FormationType.getFormationType(content);
                                if (null == ft) {
                                    logger.error("Could not parse formation type {}", content);
                                }
                                fd.setFormationType(ft);
                            }
                        }
                        break;
                    case "role":
                        valueNode = rule.selectOption(fd, true);
                        if (valueNode == null) {
                            break;
                        }
                        content = valueNode.getContent();
                        if (content == null) {
                            break;
                        }
                        if (content.startsWith("-")) {
                            for (String p : content.replaceFirst("-", "").split(",")) {
                                fd.getRoles().remove(MissionRole.parseRole(p));
                            }
                            break;
                        }
                        if (content.startsWith("+")) {
                            content = content.replace("+", "");
                        } else {
                            fd.getRoles().clear();
                        }
                        for (String p : content.split(",")) {
                            MissionRole role = MissionRole.parseRole(p);
                            if (role != null) {
                                fd.getRoles().add(role);
                            } else {
                                logger.error("Force generator could not parse mission role {}", p);
                            }
                        }
                        break;
                    case "flags":
                        valueNode = rule.selectOption(fd, true);
                        if (valueNode == null) {
                            break;
                        }
                        content = valueNode.getContent();
                        if (content == null) {
                            break;
                        }
                        if (content.startsWith("-")) {
                            for (String p : content.replaceFirst("-", "").split(",")) {
                                fd.getFlags().remove(p);
                            }
                            break;
                        }
                        if (content.startsWith("+")) {
                            content = content.replace("+", "");
                        } else {
                            fd.getFlags().clear();
                        }
                        for (String p : content.split(",")) {
                            fd.getFlags().add(p);
                        }
                        break;
                    case "changeEschelon":
                        valueNode = rule.selectOption(fd, true);
                        if (valueNode == null) {
                            break;
                        }
                        content = valueNode.getContent();
                        if (content == null) {
                            break;
                        }
                        if (content.endsWith("+")) {
                            fd.setSizeMod(ForceDescriptor.REINFORCED);
                        } else if (content.endsWith("-")) {
                            fd.setSizeMod(ForceDescriptor.UNDERSTRENGTH);
                        }
                        fd.setEchelon(Integer.parseInt(valueNode.getContent().replaceAll("[+\\-]", "")));
                        return false;
                }
            }
        }

        if (fd.getName() == null) {
            fd.setNameNodes(nameNodes);
        }

        String generate = assertions.getProperty("generate");
        if (subForces.isEmpty()) {
            generate = "model";
        }

        processSubForces(fd, generate);

        if (fd.shouldGenerateAttachments()) {
            for (SubForcesNode n : attached) {
                if (n.matches(fd)) {
                    ArrayList<ForceDescriptor> subs = n.generateSubForces(fd, true);
                    if (subs != null) {
                        for (ForceDescriptor sub : subs) {
                            fd.addAttached(sub);
                        }
                    }
                }
            }
        }
        return true;
    }

    public void processSubForces(ForceDescriptor forceDescriptor, String generate) {
        processSubForces(forceDescriptor, generate, Ruleset.findRuleset(forceDescriptor.getFaction()));
    }

    public void processSubForces(ForceDescriptor forceDescriptor, String generate, Ruleset ruleset) {
        for (SubForcesNode subforcesNode : subForces) {
            if (subforcesNode.matches(forceDescriptor)) {
                ArrayList<ForceDescriptor> subs = null;
                if (subforcesNode.getAltFaction() != null || subforcesNode.useParentFaction()) {
                    String faction = subforcesNode.getAltFaction();
                    if (subforcesNode.useParentFaction()) {
                        faction = ruleset.getParent();
                    }
                    if (faction != null) {
                        Ruleset rs;
                        ForceNode fn = null;
                        do {
                            rs = Ruleset.findRuleset(faction);
                            if (rs != null) {
                                fn = rs.findForceNode(forceDescriptor);
                                if (fn == null) {
                                    faction = rs.getParent();
                                } else {
                                    fn.processSubForces(forceDescriptor, generate, rs);
                                }
                            }
                        } while (rs != null && fn == null);
                    }
                } else {
                    subs = subforcesNode.generateSubForces(forceDescriptor, false);
                }
                if (subs != null) {
                    for (ForceDescriptor sub : subs) {
                        forceDescriptor.addSubForce(sub);
                    }
                }
            }
        }
    }

    public Integer getEchelon() {
        return echelon;
    }

    public String getEchelonCode() {
        String retVal = echelon.toString();
        if (predicates.containsKey("ifAugmented")
              && predicates.getProperty("ifAugmented").equals("1")) {
            retVal += "*";
        }
        return retVal;
    }

    public ArrayList<CommanderNode> getCoNodes() {
        return coNodes;
    }

    public ArrayList<CommanderNode> getXoNodes() {
        return xoNodes;
    }

    public Integer getCoRank(ForceDescriptor fd) {
        for (CommanderNode n : coNodes) {
            if (n.matches(fd)) {
                return n.getRank();
            }
        }
        return null;
    }

    public void setEchelon(Integer echelon) {
        this.echelon = echelon;
    }

    public String getEchelonName() {
        return echelonName;
    }

    public static ForceNode createFromXml(Node node) {
        ForceNode retVal = new ForceNode();
        retVal.loadFromXml(node);
        return retVal;
    }

    @Override
    protected void loadFromXml(Node node) throws IllegalArgumentException {
        super.loadFromXml(node);

        try {
            echelon = Integer.parseInt(assertions.getProperty("echelon"));
            assertions.remove("echelon");
        } catch (Exception ex) {
            throw new IllegalArgumentException("Force Generator: force node is missing echelon attribute.");
        }

        if (assertions.containsKey("eschName")) {
            echelonName = assertions.getProperty("eschName");
            assertions.remove("eschName");
        }
        ArrayList<OptionGroupNode> currentRuleGroup = null;
        NodeList nl = node.getChildNodes();
        for (int x = 0; x < nl.getLength(); x++) {
            Node wn = nl.item(x);

            switch (wn.getNodeName()) {
                case "name":
                    nameNodes.add(ValueNode.createFromXml(wn));
                    break;
                case "co":
                    coNodes.add(CommanderNode.createFromXml(wn));
                    break;
                case "xo":
                    xoNodes.add(CommanderNode.createFromXml(wn));
                    break;
                case "weightClass":
                case "unitType":
                case "chassis":
                case "variant":
                case "motive":
                case "formation":
                case "role":
                case "flags":
                case "changeEschelon":
                    if (currentRuleGroup == null) {
                        currentRuleGroup = new ArrayList<>();
                        ruleGroups.add(currentRuleGroup);
                    }
                    ruleGroups.get(0).add(OptionGroupNode.createFromXml(wn));
                    break;
                case "ruleGroup":
                    currentRuleGroup = new ArrayList<>();
                    ruleGroups.add(currentRuleGroup);
                    for (int y = 0; y < wn.getChildNodes().getLength(); y++) {
                        Node wn2 = wn.getChildNodes().item(y);
                        if (wn2.getNodeName().equals("weightClass")
                              || wn2.getNodeName().equals("unitType")
                              || wn2.getNodeName().equals("chassis")
                              || wn2.getNodeName().equals("variant")
                              || wn2.getNodeName().equals("motive")
                              || wn2.getNodeName().equals("formation")
                              || wn2.getNodeName().equals("role")
                              || wn2.getNodeName().equals("flags")
                              || wn2.getNodeName().equals("changeEschelon")) {
                            currentRuleGroup.add(OptionGroupNode.createFromXml(wn2));
                        }
                    }
                    break;
                case "subForces":
                    subForces.add(SubForcesNode.createFromXml(wn));
                    break;
                case "attachedForces":
                    attached.add(SubForcesNode.createFromXml(wn));
                    break;
            }
        }
    }

    /**
     * Used for debugging output
     *
     * @return A description of the node
     */
    public String show() {
        if (null == desc) {
            desc = "Force Node [echelon:" + echelon + " predicates:"
                  + predicates.entrySet().stream().map(e -> e.getKey() + "=" + e.getValue())
                  .collect(Collectors.joining(","))
                  + " assertions:"
                  + assertions.entrySet().stream().map(e -> e.getKey() + "=" + e.getValue())
                  .collect(Collectors.joining(","))
                  + "]";
        }
        return desc;
    }
}
