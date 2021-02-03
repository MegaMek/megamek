/*  
* MegaMek - Copyright (C) 2021 - The MegaMek Team  
*  
* This program is free software; you can redistribute it and/or modify it under  
* the terms of the GNU General Public License as published by the Free Software  
* Foundation; either version 2 of the License, or (at your option) any later  
* version.  
*  
* This program is distributed in the hope that it will be useful, but WITHOUT  
* ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS  
* FOR A PARTICULAR PURPOSE. See the GNU General Public License for more  
* details.  
*/ 
package megamek.common;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.StringTokenizer;

import org.apache.commons.lang3.StringUtils;

/**
 * Helper functions for the current (simple) implementation of forces that is 
 * only String-based instead of Force and Lance of MekHQ.
 * @author Simon
 */
public class Force {
    
    private String name;
    private int id;
    private ArrayList<Integer> subForces = new ArrayList<>();
    private ArrayList<Integer> entities = new ArrayList<>();

    public Force(String n) {
        Objects.requireNonNull(n);
        name = n;
    }

    public Force(String n, int nId) {
        this(n);
        id = nId; 
    }

    public String getName() {
        return name;
    }

    public void setName(String n) {
        name = n;
    }


//    public Vector<Force> getSubForces() {
//        return subForces;
//    }
//
//    public boolean isAncestorOf(Force otherForce) {
//        Force pForce = otherForce.getParentForce();
//        while (pForce != null) {
//            if (pForce.getId() == getId()) {
//                return true;
//            }
//            pForce = pForce.getParentForce();
//        }
//        return false;
//    }
//
//    /**
//     * @return the full hierarchical name of the force, including all parents
//     */
//    public String getFullName() {
//        String toReturn = getName();
//        if (null != parentForce) {
//            toReturn += ", " + parentForce.getFullName();
//        }
//        return toReturn;
//    }
//
//    /**
//     * Add a subforce to the subforce vector. In general, this
//     * should not be called directly to add forces to the campaign
//     * because they will not be assigned an id. Use {@link Campaign#addForce(Force, Force)}
//     * instead
//     * The boolean assignParent here is set to false when assigning forces from the
//     * TOE to a scenario, because we don't want to switch this forces real parent
//     * @param sub
//     */
//    public void addSubForce(Force sub, boolean assignParent) {
//        if (assignParent) {
//            sub.setParentForce(this);
//        }
//        subForces.add(sub);
//    }
//
//    public Vector<UUID> getUnits() {
//        return units;
//    }
//
//    /**
//     * @param combatForcesOnly to only include combat forces or to also include non-combat forces
//     * @return all the unit ids in this force and all of its subforces
//     */
//    public Vector<UUID> getAllUnits(boolean combatForcesOnly) {
//        Vector<UUID> allUnits;
//        if (combatForcesOnly && !isCombatForce()) {
//            allUnits = new Vector<>();
//        } else {
//            allUnits = new Vector<>(units);
//        }
//
//        for (Force f : subForces) {
//            allUnits.addAll(f.getAllUnits(combatForcesOnly));
//        }
//        return allUnits;
//    }
//
    /**
     * Add a unit id to the units vector. In general, this
     * should not be called directly to add unid because they will
     * not be assigned a force id. Use {@link Campaign#addUnitToForce(mekhq.campaign.unit.Unit, int)}
     * instead
     * @param uid
     */
    public void addUnit(int id) {
        entities.add(id);
    }
//
//    /**
//     * This should not be directly called except by {@link Campaign#removeUnitFromForce(mekhq.campaign.unit.Unit)}
//     * instead
//     * @param id
//     */
//    public void removeUnit(UUID id) {
//        int idx = 0;
//        boolean found = false;
//        for (UUID uid : getUnits()) {
//            if (uid.equals(id)) {
//                found = true;
//                break;
//            }
//            idx++;
//        }
//        if (found) {
//            units.remove(idx);
//        }
//    }
//
//    public boolean removeUnitFromAllForces(UUID id) {
//        int idx = 0;
//        boolean found = false;
//        for (UUID uid : getUnits()) {
//            if (uid.equals(id)) {
//                found = true;
//                break;
//            }
//            idx++;
//        }
//        if (found) {
//            units.remove(idx);
//        } else {
//            for (Force sub : getSubForces()) {
//                found = sub.removeUnitFromAllForces(id);
//                if (found) {
//                    break;
//                }
//            }
//        }
//        return found;
//    }
//
//    public void clearScenarioIds(Campaign c) {
//        clearScenarioIds(c, true);
//    }
//
//    public void clearScenarioIds(Campaign c, boolean killSub) {
//        if (killSub) {
//            for (UUID uid : getUnits()) {
//                Unit u = c.getUnit(uid);
//                if (null != u) {
//                    u.undeploy();
//                }
//            }
//            // We only need to clear the subForces if we're killing everything.
//            for (Force sub : getSubForces()) {
//                Scenario s = c.getScenario(sub.getScenarioId());
//                if (s != null) {
//                    s.removeForce(sub.getId());
//                }
//                sub.clearScenarioIds(c);
//            }
//        } else {
//            // If we're not killing the units from the scenario, then we need to assign them with the
//            // scenario ID and add them to the scenario.
//            for (UUID uid : getUnits()) {
//                c.getUnit(uid).setScenarioId(getScenarioId());
//                c.getScenario(getScenarioId()).addUnit(uid);
//            }
//        }
//        setScenarioId(-1);
//    }
//
//    @Override
//    public String toString() {
//        return name;
//    }
//
//    public int getId() {
//        return id;
//    }
//
//    public void setId(int i) {
//        this.id = i;
//    }
//
//    public void removeSubForce(int id) {
//        int idx = 0;
//        boolean found = false;
//        for (Force sforce : getSubForces()) {
//            if (sforce.getId() == id) {
//                found = true;
//                break;
//            }
//            idx++;
//        }
//        if (found) {
//            subForces.remove(idx);
//        }
//    }
//
////    public String getIconCategory() {
////        return iconCategory;
////    }
////
////    public void setIconCategory(String s) {
////        this.iconCategory = s;
////    }
////
////    public String getIconFileName() {
////        return iconFileName;
////    }
////
////    public void setIconFileName(String s) {
////        this.iconFileName = s;
////    }
////
////    public LinkedHashMap<String, Vector<String>> getIconMap() {
////        return iconMap;
////    }
////
////    public void setIconMap(LinkedHashMap<String, Vector<String>> iconMap) {
////        this.iconMap = iconMap;
////    }
//
//    
//    
//    private static void processUnitNodes(Force retVal, Node wn, Version version) {
//        NodeList nl = wn.getChildNodes();
//        for (int x = 0; x < nl.getLength(); x++) {
//            Node wn2 = nl.item(x);
//            if (wn2.getNodeType() != Node.ELEMENT_NODE) {
//                continue;
//            }
//            NamedNodeMap attrs = wn2.getAttributes();
//            Node classNameNode = attrs.getNamedItem("id");
//            String idString = classNameNode.getTextContent();
//            retVal.addUnit(UUID.fromString(idString));
//        }
//    }
//
//    private static void processIconMapNodes(Force retVal, Node wn, Version version) {
//        NodeList nl = wn.getChildNodes();
//        for (int x = 0; x < nl.getLength(); x++) {
//            Node wn2 = nl.item(x);
//
//            // If it's not an element node, we ignore it.
//            if (wn2.getNodeType() != Node.ELEMENT_NODE) {
//                continue;
//            }
//
//            NamedNodeMap attrs = wn2.getAttributes();
//            Node keyNode = attrs.getNamedItem("key");
//            String key = keyNode.getTextContent();
//            Vector<String> values = null;
//            if (wn2.hasChildNodes()) {
//                values = processIconMapSubNodes(wn2, version);
//            }
//            retVal.iconMap.put(key, values);
//        }
//    }
//
//    private static Vector<String> processIconMapSubNodes(Node wn, Version version) {
//        Vector<String> values = new Vector<>();
//        NodeList nl = wn.getChildNodes();
//        for (int x = 0; x < nl.getLength(); x++) {
//            Node wn2 = nl.item(x);
//
//            // If it's not an element node, we ignore it.
//            if (wn2.getNodeType() != Node.ELEMENT_NODE) {
//                continue;
//            }
//
//            NamedNodeMap attrs = wn2.getAttributes();
//            Node keyNode = attrs.getNamedItem("name");
//            String key = keyNode.getTextContent();
//            if ((null != key) && !key.isEmpty()) {
//                values.add(key);
//            }
//        }
//        return values;
//    }
//
//    public Vector<Object> getAllChildren(Campaign campaign) {
//        Vector<Object> children = new Vector<>(subForces);
//        //add any units
//        Enumeration<UUID> uids = getUnits().elements();
//        //put them into a temporary array so I can sort it by rank
//        List<Unit> units = new ArrayList<>();
//        List<Unit> unmannedUnits = new ArrayList<>();
//        while (uids.hasMoreElements()) {
//            Unit u = campaign.getUnit(uids.nextElement());
//            if (null != u) {
//                if (null == u.getCommander()) {
//                    unmannedUnits.add(u);
//                } else {
//                    units.add(u);
//                }
//            }
//        }
//        units.sort((u1, u2) -> ((Comparable<Integer>) u2.getCommander().getRankNumeric())
//                .compareTo(u1.getCommander().getRankNumeric()));
//
//        children.addAll(units);
//        children.addAll(unmannedUnits);
//        return children;
//    }
//
//    @Override
//    public boolean equals(Object o) {
//        return (o instanceof Force) && (((Force) o).getId() == id) && ((Force) o).getFullName().equals(getFullName());
//    }
//
//    @Override
//    public int hashCode() {
//        return Objects.hash(getId(), getFullName());
//    }
//
//    public void fixIdReferences(Map<Integer, UUID> uHash) {
//        for (int oid : oldUnits) {
//            UUID nid = uHash.get(oid);
//            if (null != nid) {
//                units.add(nid);
//            }
//        }
//        for (Force sub : subForces) {
//            sub.fixIdReferences(uHash);
//        }
//    }
//
//    public int getTotalBV(Campaign c) {
//        int bvTotal = 0;
//
//        for (Force sforce : getSubForces()) {
//            bvTotal += sforce.getTotalBV(c);
//        }
//
//        for (UUID id : getUnits()) {
//            // no idea how this would happen, but some times a unit in a forces unit ID list has an invalid ID?
//            if (c.getUnit(id) == null) {
//                continue;
//            }
//
//            bvTotal += c.getUnit(id).getEntity().calculateBattleValue();
//        }
//
//        return bvTotal;
//    }
}

//private static final String SEPERATOR = "|";
//
//public static String getForceName(Entity entity) {
//  final String force = entity.getForce(); 
//  if (force == null) {
//      return "";
//  }
//  if (force.contains(SEPERATOR)) {
//      return force.substring(0, force.indexOf(SEPERATOR));
//  } else {
//      return force;
//  }
//}
//
//public static boolean hasForce(Entity entity) {
//  return (entity.getForce() != null) && !entity.getForce().isEmpty(); 
//}
//
//public static int forceLevel(Entity entity) {
//  if (!hasForce(entity)) {
//      return 0;
//  }
//  return StringUtils.countMatches(entity.getForce(), SEPERATOR) + 1;
//}
//
//public static int lowestForceLevel(Entity entity) {
//  if (!hasForce(entity)) {
//      return 0;
//  }
//  List<String> forceList = getFullForceList(entity);
//  for (int i = forceList.size() - 1; i >= 0; i--) {
//      if (!forceList.get(i).equals("_")) {
//          return forceList.size() - i + 1;
//      }
//  }
//  // Should not arrive here
//  return 0;
//}
//
//public static HashSet<String> getForces(IGame game) {
//  HashSet<String> result = new HashSet<>();
//  for (Entity entity: game.getEntitiesVector()) {
//      if (hasForce(entity)) {
//          result.add(entity.getForce());
//      }
//  }
//  return result;
//}
//
//public static HashSet<String> getAvailableForces(IGame game, IPlayer player) {
//  HashSet<String> result = new HashSet<>();
//  for (Entity entity: game.getEntitiesVector()) {
//      if (!entity.getOwner().isEnemyOf(player) && hasForce(entity)) {
//          result.add(entity.getForce());
//      }
//  }
//  return result;
//}
//
///**
//* Returns an ArrayList containing the forces of the provided entity from
//* highest to lowest. For any empty levels below the top level, the list
//* will contain a null (e.g. when a unit belongs to a company but to no 
//* individual lance). 
//*/
//public static ArrayList<String> getFullForceList(Entity entity) {
//  ArrayList<String> result = new ArrayList<>(); 
//  final String force = entity.getForce(); 
//  if (force != null) {
//      StringTokenizer tk = new StringTokenizer(force, SEPERATOR);
//      while (tk.hasMoreElements()) {
//          String nextForce = tk.nextToken();
//          result.add((nextForce != "" ? nextForce : null));
//      }
//  }
//  return result;
//}
//
//public static void addToNewTopLevelForce(Entity entity, String name, int level) {
//  int oldLevel = forceLevel(entity);
//  if (level <= oldLevel) {
//      return; // This should not happen
//  }
//  String newForce = name;
//  for (int i = 0; i < level - oldLevel - 1; i++) {
//      newForce += SEPERATOR + "_";
//  }
//  if (hasForce(entity)) {
//  newForce += SEPERATOR + entity.getForce();
//  }
//  entity.setForce(newForce);
//}
//
//public static String getForceToDepth(Entity entity, int depth) {
//  if (!hasForce(entity)) {
//      return null;
//  }
//  ArrayList<String> forces = getFullForceList(entity);
//  String result = "";
//  int d = 0;
//  for (String level: forces) {
//      result += (level != null ? level : "_") + SEPERATOR;
//      if (d == depth) {
//          break;
//      }
//      d++;
//  }
//  result += SEPERATOR;
//  return result.replace(SEPERATOR + SEPERATOR, "");
//}
//
