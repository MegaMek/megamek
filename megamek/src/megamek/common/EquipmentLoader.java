package megamek.common;

import java.util.Locale;

import megamek.common.logging.Logger;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * @author Wong Wing Lun (aka luiges90)
 *         Load equipment info from XML file, shared by WeaponLoader and AmmoLoader
 */
public final class EquipmentLoader {

    private static Logger logger = new Logger();

    static int parseAmmoType(String s, String filename) {
        try {
            return AmmoType.class.getField("T_" + s.trim().toUpperCase(Locale.ENGLISH))
                    .getInt(null);
        } catch (IllegalAccessException e) {
            try {
                return Integer.parseInt(s);
            } catch (NumberFormatException ex) {
                logger.log(EquipmentLoader.class, "parseAmmoType", "Could not recognise AmmoType "
                        + s + " in " + filename);
                return AmmoType.T_NA;
            }
        } catch (NoSuchFieldException e) {
            try {
                return Integer.parseInt(s);
            } catch (NumberFormatException ex) {
                logger.log(EquipmentLoader.class, "parseAmmoType", "Could not recognise AmmoType "
                        + s + " in " + filename);
                return AmmoType.T_NA;
            }
        }
    }

    private static int parseRating(String s, String filename) {
        try {
            return EquipmentType.class.getField("RATING_" + s.trim().toUpperCase(Locale.ENGLISH))
                    .getInt(
                    null);
        } catch (IllegalAccessException e) {
            try {
                return Integer.parseInt(s);
            } catch (NumberFormatException ex) {
                logger.log(EquipmentLoader.class, "parseAmmoType", "Could not recognise Rating "
                        + s + " in " + filename);
                return EquipmentType.RATING_C;
            }
        } catch (NoSuchFieldException e) {
            try {
                return Integer.parseInt(s);
            } catch (NumberFormatException ex) {
                logger.log(EquipmentLoader.class, "parseAmmoType", "Could not recognise Rating "
                        + s + " in " + filename);
                return EquipmentType.RATING_C;
            }
        }
    }

    static EquipmentType loadEquipmentType(String filename, Document doc, EquipmentType et) {
        if (doc == null) {
            return null;
        }

        Element root = doc.getDocumentElement();
        String techLevel = null;
        for (int i = 0; i < root.getChildNodes().getLength(); i++) {
            Node child = root.getChildNodes().item(i);

            if (child.getNodeName().equalsIgnoreCase("Name")) {
                et.name = child.getTextContent().trim();
            } else if (child.getNodeName().equalsIgnoreCase("ShortName")) {
                et.shortName = child.getTextContent().trim();
            } else if (child.getNodeName().equalsIgnoreCase("lookupName")) {
                for (int j = 0; j < child.getChildNodes().getLength(); ++j) {
                    String name = child.getChildNodes().item(j).getTextContent().trim();
                    if (!name.equals("")) {
                        et.addLookupName(name);
                    }
                }
            } else if (child.getNodeName().equalsIgnoreCase("tonnage")) {
                et.tonnage = Float.parseFloat(child.getTextContent());
            } else if (child.getNodeName().equalsIgnoreCase("criticals")) {
                et.criticals = Integer.parseInt(child.getTextContent());
            } else if (child.getNodeName().equalsIgnoreCase("tankslots")) {
                et.tankslots = Integer.parseInt(child.getTextContent());
            } else if (child.getNodeName().equalsIgnoreCase("explosive")) {
                et.explosive = Boolean.parseBoolean(child.getTextContent());
            } else if (child.getNodeName().equalsIgnoreCase("hittable")) {
                et.hittable = Boolean.parseBoolean(child.getTextContent());
            } else if (child.getNodeName().equalsIgnoreCase("spreadable")) {
                et.spreadable = Boolean.parseBoolean(child.getTextContent());
            } else if (child.getNodeName().equalsIgnoreCase("toHitModifier")) {
                et.toHitModifier = Integer.parseInt(child.getTextContent());
            } else if (child.getNodeName().equalsIgnoreCase("subtype")) {
                et.subType = Long.parseLong(child.getTextContent());
            } else if (child.getNodeName().equalsIgnoreCase("bv")) {
                et.bv = Double.parseDouble(child.getTextContent());
            } else if (child.getNodeName().equalsIgnoreCase("cost")) {
                et.cost = Double.parseDouble(child.getTextContent());
            } else if (child.getNodeName().equalsIgnoreCase("TechLevel")) {
                techLevel = child.getTextContent();
            } else if (child.getNodeName().equalsIgnoreCase("techRating")) {
                et.techRating = parseRating(child.getTextContent(), filename);
            } else if (child.getNodeName().equalsIgnoreCase("availRating")) {
                String[] s = child.getTextContent().split(",");
                for (int j = 0; j < et.availRating.length; ++j) {
                    et.availRating[j] = parseRating(s[j], filename);
                }
            } else if (child.getNodeName().equalsIgnoreCase("introDate")) {
                et.introDate = Integer.parseInt(child.getTextContent());
            } else if (child.getNodeName().equalsIgnoreCase("extinctDate")) {
                et.extinctDate = Integer.parseInt(child.getTextContent());
            } else if (child.getNodeName().equalsIgnoreCase("reintroDate")) {
                et.reintroDate = Integer.parseInt(child.getTextContent());
            } else if (child.getNodeName().equalsIgnoreCase("instantModeSwitch")) {
                et.instantModeSwitch = Boolean.parseBoolean(child.getTextContent());
            }

        }

        et.internalName = et.name;
        et.addLookupName(et.internalName);

        if (techLevel != null) {
            et.techLevel.clear();
            if (techLevel.equalsIgnoreCase("IS") || techLevel.equalsIgnoreCase("Inner Sphere")) {
                et.techLevel.put(et.introDate, TechConstants.T_IS_UNOFFICIAL);
            } else if (techLevel.equalsIgnoreCase("CL") || techLevel.equalsIgnoreCase("Clan")) {
                et.techLevel.put(et.introDate, TechConstants.T_CLAN_UNOFFICIAL);
            } else {
                logger.log(EquipmentLoader.class, "loadEquipmentType", "Unknown techLevel "
                        + techLevel + " in " + filename);
                et.techLevel.put(et.introDate, TechConstants.T_TECH_UNKNOWN);
            }
        } else {
            logger.log(EquipmentLoader.class, "loadEquipmentType", "No techLevel set in "
                    + filename);
        }

        return et;
    }

}
