package megamek.common;

import java.io.File;
import java.io.FileInputStream;
import java.math.BigInteger;
import java.util.List;
import java.util.Locale;

import javax.xml.parsers.DocumentBuilderFactory;

import megamek.common.logging.LogLevel;
import megamek.common.logging.Logger;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * @author Wong Wing Lun (aka luiges90)
 *         Load custom ammo info from XML file
 */
public final class AmmoLoader {

    private static Logger logger = new Logger();

    private AmmoLoader() {
    }

    private static BigInteger parseFlag(String s, String filename) {
        try {
            return (BigInteger) AmmoType.class
                    .getField("F_" + s.trim().toUpperCase(Locale.ENGLISH)).get(
                    null);
        } catch (IllegalAccessException e) {
            try {
                return new BigInteger(s);
            } catch (NumberFormatException ex) {
                logger.log(EquipmentLoader.class, "parseFlag", "Could not recognise flag "
                        + s + " in " + filename);
                return BigInteger.ZERO;
            }
        } catch (NoSuchFieldException e) {
            try {
                return new BigInteger(s);
            } catch (NumberFormatException ex) {
                logger.log(EquipmentLoader.class, "parseFlag", "Could not recognise flag "
                        + s + " in " + filename);
                return BigInteger.ZERO;
            }
        }
    }

    private static long parseMunitionType(String s, String filename) {
        try {
            return AmmoType.class.getField("M_" + s.trim().toUpperCase(Locale.ENGLISH))
                    .getLong(null);
        } catch (IllegalAccessException e) {
            try {
                return Long.parseLong(s);
            } catch (NumberFormatException ex) {
                logger.log(EquipmentLoader.class, "parseMunitionType",
                        "Could not recognise munition type "
                                + s + " in " + filename);
                return AmmoType.M_STANDARD;
            }
        } catch (NoSuchFieldException e) {
            try {
                return Long.parseLong(s);
            } catch (NumberFormatException ex) {
                logger.log(EquipmentLoader.class, "parseMunitionType",
                        "Could not recognise munition type "
                                + s + " in " + filename);
                return AmmoType.M_STANDARD;
            }
        }
    }

    private static AmmoType loadAmmoType(String filename, Document doc) {
        if (doc == null) {
            return null;
        }

        AmmoType at = new AmmoType();
        Element root = doc.getDocumentElement();

        EquipmentLoader.loadEquipmentType(filename, doc, at);

        for (int i = 0; i < root.getChildNodes().getLength(); i++) {
            Node child = root.getChildNodes().item(i);

            if (child.getNodeName().equalsIgnoreCase("damagePerShot")) {
                at.damagePerShot = Integer.parseInt(child.getTextContent());
            } else if (child.getNodeName().equalsIgnoreCase("rackSize")) {
                at.rackSize = Integer.parseInt(child.getTextContent());
            } else if (child.getNodeName().equalsIgnoreCase("ammoType")) {
                at.ammoType = EquipmentLoader.parseAmmoType(child.getTextContent(), filename);
            } else if (child.getNodeName().equalsIgnoreCase("munitionType")) {
                for (int j = 0; j < child.getChildNodes().getLength(); ++j) {
                    at.munitionType |= parseMunitionType(child.getChildNodes().item(j)
                            .getTextContent(), filename);
                }
            } else if (child.getNodeName().equalsIgnoreCase("shots")) {
                at.shots = Integer.parseInt(child.getTextContent());
            } else if (child.getNodeName().equalsIgnoreCase("shortName")) {
                at.shortName = child.getTextContent().trim();
            } else if (child.getNodeName().equalsIgnoreCase("flags")) {
                for (int j = 0; j < child.getChildNodes().getLength(); ++j) {
                    at.flags.or(parseFlag(child.getChildNodes().item(j).getTextContent(), filename));
                }
            } else {
                logger.log(EquipmentLoader.class, "loadEquipmentType", "Unknown element "
                        + child.getNodeName() + " in " + filename);
            }
        }

        return at;
    }

    public static void loadCustomAmmo(List<AmmoType> ammo, File fDir) {
        String[] sa = fDir.list();
        if (sa != null) {
            for (String element : sa) {
                File f = new File(fDir, element);

                if (f.isDirectory()) {
                    loadCustomAmmo(ammo, f);
                } else if (f.getName().toLowerCase().endsWith(".xml")) {
                    try {
                        AmmoType wt = loadAmmoType(f.getName(), DocumentBuilderFactory
                                .newInstance()
                                .newDocumentBuilder()
                                .parse(new FileInputStream(f)));
                        if (wt != null) {
                            ammo.add(wt);
                        }
                    } catch (Exception ex) {
                        logger.log(WeaponLoader.class, "loadCustomWeapons()", LogLevel.ERROR,
                                "Could not load " + f.getAbsolutePath());
                    }
                }

            }
        }
    }

}
