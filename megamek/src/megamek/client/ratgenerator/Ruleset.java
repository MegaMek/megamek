/*
 * MegaMek - Copyright (C) 2016 The MegaMek Team
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
package megamek.client.ratgenerator;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.HashMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Container for all the rule nodes for a faction. Has methods for processing the rules to
 * fill out a ForceDescriptor.
 * 
 * @author Neoancient
 *
 */
public class Ruleset {
	
	private final static String directory = "data/forcegenerator/faction_rules";
	
	private static HashMap<String,Ruleset> rulesets;
	private static boolean initialized;
	private static boolean initializing;
	
	private String faction;
	private DefaultsNode defaults;
	private TOCNode toc;
	private int customRankBase;
	private HashMap<Integer,String> customRanks;
	private ArrayList<ForceNode> forceNodes;
	private String parent;
	
	private Ruleset() {
		faction = "IS";
		forceNodes = new ArrayList<>();
		customRanks = new HashMap<>();
		parent = null;
	}
	
	public static Ruleset findRuleset(ForceDescriptor fd) {
		return findRuleset(fd.getFaction());
	}
		
	public static Ruleset findRuleset(String faction) {
		if (faction == null) {
			faction = "IS";
		}
		if (rulesets.containsKey(faction)) {
			return rulesets.get(faction);
		}
		if (faction.contains(".")) {
			faction = faction.split("\\.")[0];
			if (rulesets.containsKey(faction)) {
				return rulesets.get(faction);
			}
		}
		FactionRecord fRec = RATGenerator.getInstance().getFaction(faction);
		if (fRec != null) {
			if (fRec.isClan()) {
				return faction.equals("CLAN")?null:findRuleset("CLAN");
			} else if (fRec.isPeriphery()) {
				return faction.equals("Periphery")?null:findRuleset("Periphery");
			}
		}
		return faction == null || faction.equals("IS")? null : findRuleset("IS");
	}
	
	public int getCustomRankBase() {
		return customRankBase;
	}
	
	public HashMap<Integer,String> getCustomRanks() {
		return customRanks;
	}
	
	public void process(ForceDescriptor fd) {
		if (fd.isTopLevel()) {
			defaults.apply(fd);
		}
		
		Ruleset rs = findRuleset(fd.getFaction());
		boolean applied = false;
		ForceNode fn = null;
		do {
			fn = rs.findForceNode(fd);
			if (fn == null) {
				if (rs.getParent() == null) {
					rs = null;
				} else {
					rs = rulesets.get(rs.getParent());
				}				
			} else {
				applied = fn.apply(fd);
			}
		} while (rs != null && (fn == null || !applied));
		
		if (!applied && fd.getEschelon() == 1 && !fd.isElement()) {
			ModelRecord mRec = fd.generate();
			if (mRec != null) {
				fd.setUnit(mRec);
			}
		}
		for (ForceDescriptor sub : fd.getSubforces()) {
			rs = this;
			if (!fd.getFaction().equals(sub.getFaction())) {
				rs = findRuleset(sub.getFaction());
			}
			if (rs == null) {
				process(sub);
			} else {
				rs.process(sub);
			}
		}
		for (ForceDescriptor sub : fd.getAttached()) {
			process(sub);
		}

		for (ForceDescriptor sub : fd.getAttached()) {
			sub.assignCommanders();
			sub.assignPositions();
//			sub.assignBloodnames();
		}

		if (fd.isTopLevel()) {
			fd.assignCommanders();
			fd.assignPositions();
//			fd.assignBloodnames();
		}
	}
	
	public String getDefaultUnitType(ForceDescriptor fd) {
		return defaults.getUnitType(fd);
	}
	
	public String getDefaultEschelon(ForceDescriptor fd) {
		return defaults.getEschelon(fd);
	}
	
	public String getDefaultRating(ForceDescriptor fd) {
		return defaults.getRating(fd);
	}
	
	public TOCNode getTOCNode() {
		return toc;
	}
	
	public ForceNode findForceNode(ForceDescriptor fd) {
		for (ForceNode n : forceNodes) {
			if (n.getEschelon().equals(fd.getEschelon()) && n.matches(fd)) {
				return n;
			}
		}		
		return null;
	}
	
	public ForceNode findForceNode(ForceDescriptor fd, int eschelon, boolean augmented) {
		for (ForceNode n : forceNodes) {
			if (n.getEschelon() == eschelon && n.matches(fd, augmented)) {
				return n;
			}
		}		
		return null;
	}
	
	public HashMap<String,String> getEschelonNames(String unitType) {
		HashMap<String,String> retVal = new HashMap<String,String>();
		for (ForceNode n : forceNodes) {
			if (n.matchesPredicate(unitType, "ifUnitType")) {
				retVal.put(n.getEschelonCode(), n.getEschelonName());
			}
		}
		return retVal;
	}
	
	public String getEschelonName(ForceDescriptor fd) {
		for (ForceNode fn : forceNodes) {
			if (fn.matches(fd) && fn.getEschelon() == fd.getEschelon()) {
				return fn.getEschelonName();
			}
		}
		return null;
	}
	
	public CommanderNode getCoNode(ForceDescriptor fd) {
		for (ForceNode fn : forceNodes) {
			if (fn.getEschelon() == fd.getEschelon() && fn.matches(fd)) {
				for (CommanderNode rn : fn.getCoNodes()) {
					if (rn.matches(fd)) {
						return rn;
					}
				}
			}
		}
		return null;
	}
	
	public CommanderNode getXoNode(ForceDescriptor fd) {
		for (ForceNode fn : forceNodes) {
			if (fn.getEschelon() == fd.getEschelon() && fn.matches(fd)) {
				for (CommanderNode rn : fn.getXoNodes()) {
					if (rn.matches(fd)) {
						return rn;
					}
				}
			}
		}
		return null;
	}
	
	public String getParent() {
		return parent;
	}
	
	public static void loadData() {
		initialized = false;
		initializing = true;
		rulesets = new HashMap<String,Ruleset>();
		
		File dir = new File(directory);
		if (!dir.exists()) {
			System.err.println("Could not locate force generator faction rules.");
			initializing = false;
		}
		for (File f : dir.listFiles()) {
			if (!f.getPath().endsWith(".xml")) {
				continue;
			}
			Ruleset rs = createFromFile(f);
			if (rs != null) {
				rulesets.put(rs.getFaction(), rs);
			}
		}
		initialized = true;
		initializing = false;
	}
	
	private static Ruleset createFromFile(File f) {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		Document xmlDoc = null;
		
		DocumentBuilder db;
		try {
			FileInputStream fis = new FileInputStream(f);
			db = dbf.newDocumentBuilder();
			xmlDoc = db.parse(fis);
			fis.close();
		} catch (Exception e) {
			System.err.println("While loading force template from file " + f.getName()
					+ ": " + e.getMessage());
			return null;
		}
		
		Ruleset retVal = new Ruleset();
		
		Element elem = xmlDoc.getDocumentElement();
		if (!elem.getNodeName().equals("ruleset")) {
			System.err.println("Could not find ruleset element in file " + f.getName());
			return null;
		}
		if (elem.getAttribute("faction").length() > 0) {
			retVal.faction = elem.getAttribute("faction");
		} else {
			System.err.println("Faction is not declared in ruleset file " + f.getName());
			return null;
		}
		if (elem.getAttribute("parent").length() > 0) {
			retVal.parent = elem.getAttribute("parent");
		} else {
			if (retVal.faction.contains(".")) {
				retVal.parent = retVal.faction.split("\\.")[0];
			} else {
				FactionRecord fRec = RATGenerator.getInstance().getFaction(retVal.faction);
				if (fRec == null) {
					retVal.parent = null;
				} else if (fRec.isClan()) {
					retVal.parent = "CLAN"; 
				} else if (fRec.isPeriphery()) {
					retVal.parent = "PERIPHERY"; 
				} else {
					retVal.parent = "IS";
				}
				if (retVal.faction.equals(retVal.parent)) {
					retVal.parent = null;
				}
			}
		}
		NodeList nl = elem.getChildNodes();
		elem.normalize();
		
		for (int x = 0; x < nl.getLength(); x++) {
			Node wn = nl.item(x);
			switch (wn.getNodeName()) {
			case "defaults":
				retVal.defaults = DefaultsNode.createFromXml(wn);
				break;
			case "toc":
				retVal.toc = TOCNode.createFromXml(wn);
				break;
			case "customRanks":
				for (int y = 0; y < wn.getChildNodes().getLength(); y++) {
					Node wn2 = wn.getChildNodes().item(y);
					switch (wn2.getNodeName()) {
					case "base":
						retVal.customRankBase = Integer.parseInt(wn2.getTextContent());
						break;
					case "rank":
						String[] fields = wn2.getTextContent().split(":");
						int rank = Integer.parseInt(fields[0]);
						retVal.customRanks.put(rank, fields[1]);
						break;
					}
				}
				break;
			case "force":
				try {
					retVal.forceNodes.add(ForceNode.createFromXml(wn));
				} catch (IllegalArgumentException ex) {
					System.err.println("In file " + f.getName()
							+ " while processing force node" + 
							((wn.getAttributes().getNamedItem("eschName") == null)?"":
								" " + wn.getAttributes().getNamedItem("eschName")) +
								": ");
					System.err.println(ex.getMessage());
				}
				break;
			}
		}		
		
		return retVal;
	}

	public static boolean isInitialized() {
		return initialized;
	}
	
	public static boolean isInitializing() {
		return initializing;
	}
	
	public String getFaction() {
		return faction;
	}
	
}
