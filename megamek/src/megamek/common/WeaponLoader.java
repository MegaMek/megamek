package megamek.common;

import java.io.File;
import java.io.FileInputStream;
import java.math.BigInteger;
import java.util.List;
import java.util.Locale;

import javax.xml.parsers.DocumentBuilderFactory;

import megamek.common.logging.LogLevel;
import megamek.common.logging.Logger;
import megamek.common.weapons.CustomWeapon;

import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * @author Wong Wing Lun (aka luiges90)
 *         Load custom weapon info from XML file
 */
public final class WeaponLoader {

    private static Logger logger = new Logger();

    private WeaponLoader() {
    }

    private static int parseDamage(String s, String filename) {
        if (s.equalsIgnoreCase("cluster")) {
            return WeaponType.DAMAGE_BY_CLUSTERTABLE;
        } else {
            try {
                return WeaponType.class.getField("DAMAGE_" + s.trim().toUpperCase(Locale.ENGLISH))
                        .getInt(null);
            } catch (IllegalAccessException e) {
                try {
                    return Integer.parseInt(s);
                } catch (NumberFormatException ex) {
                    logger.log(EquipmentLoader.class, "parseDamage",
                            "Could not recognise damage type "
                                    + s + " in " + filename);
                    return 0;
                }
            } catch (NoSuchFieldException e) {
                try {
                    return Integer.parseInt(s);
                } catch (NumberFormatException ex) {
                    logger.log(EquipmentLoader.class, "parseDamage",
                            "Could not recognise damage type "
                                    + s + " in " + filename);
                    return 0;
                }
            }
        }
    }

    private static int parseMaxRange(String s, String filename) {
        try {
            return WeaponType.class.getField("RANGE_" + s.trim().toUpperCase(Locale.ENGLISH))
                    .getInt(null);
        } catch (IllegalAccessException e) {
            try {
                return Integer.parseInt(s);
            } catch (NumberFormatException ex) {
                logger.log(EquipmentLoader.class, "parseMaxRange",
                        "Could not recognise maxRange type "
                                + s + " in " + filename);
                return WeaponType.RANGE_SHORT;
            }
        } catch (NoSuchFieldException e) {
            try {
                return Integer.parseInt(s);
            } catch (NumberFormatException ex) {
                logger.log(EquipmentLoader.class, "parseMaxRange",
                        "Could not recognise maxRange type "
                                + s + " in " + filename);
                return WeaponType.RANGE_SHORT;
            }
        }
    }

    private static int parseInfDamageClass(String s, String filename) {
        try {
            return WeaponType.class.getField("WEAPON_" + s.trim().toUpperCase(Locale.ENGLISH))
                    .getInt(null);
        } catch (IllegalAccessException e) {
            try {
                return Integer.parseInt(s);
            } catch (NumberFormatException ex) {
                logger.log(EquipmentLoader.class, "parseInfDamageClass",
                        "Could not recognise InfDamageClass type "
                                + s + " in " + filename);
                return WeaponType.WEAPON_DIRECT_FIRE;
            }
        } catch (NoSuchFieldException e) {
            try {
                return Integer.parseInt(s);
            } catch (NumberFormatException ex) {
                logger.log(EquipmentLoader.class, "parseInfDamageClass",
                        "Could not recognise InfDamageClass type "
                                + s + " in " + filename);
                return WeaponType.WEAPON_DIRECT_FIRE;
            }
        }
    }

    private static int parseATClass(String s, String filename) {
        try {
            return WeaponType.class.getField("CLASS_" + s.trim().toUpperCase(Locale.ENGLISH))
                    .getInt(null);
        } catch (IllegalAccessException e) {
            try {
                return Integer.parseInt(s);
            } catch (NumberFormatException ex) {
                logger.log(EquipmentLoader.class, "parseATClass",
                        "Could not recognise ATClass type "
                                + s + " in " + filename);
                return WeaponType.CLASS_NONE;
            }
        } catch (NoSuchFieldException e) {
            try {
                return Integer.parseInt(s);
            } catch (NumberFormatException ex) {
                logger.log(EquipmentLoader.class, "parseATClass",
                        "Could not recognise ATClass type "
                                + s + " in " + filename);
                return WeaponType.CLASS_NONE;
            }
        }
    }

    private static BigInteger parseFlag(String s, String filename) {
        try {
            return (BigInteger) WeaponType.class.getField(
                    "F_" + s.trim().toUpperCase(Locale.ENGLISH))
                    .get(null);
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

    private static WeaponType loadWeaponType(String filename, Document doc)
            throws ClassNotFoundException,
            DOMException, InstantiationException, IllegalAccessException {
        if (doc == null) {
            return null;
        }

        WeaponType wt = null;
        Element root = doc.getDocumentElement();
        for (int i = 0; i < root.getChildNodes().getLength(); i++) {
            Node child = root.getChildNodes().item(i);

            if (child.getNodeName().equalsIgnoreCase("BaseType")) {
                Class<?> type;
                try {
                    type = Class.forName("megamek.common.weapons." + child.getTextContent().trim());
                } catch (ClassNotFoundException ex) {
                    try {
                    type = Class.forName("megamek.common.weapons.battlearmor."
                                + child.getTextContent().trim());
                    } catch (ClassNotFoundException ex2) {
                        type = Class.forName("megamek.common.weapons.infantry."
                                + child.getTextContent().trim());
                    }
                }

                if (!WeaponType.class.isAssignableFrom(type)) {
                    throw new ClassCastException(
                            "Type " + type.getSimpleName() + " is not a WeaponType.");
                }

                wt = (WeaponType) type.newInstance();
                break;
            } else if (child.getNodeName().equalsIgnoreCase("WeaponHandler")) {
                wt = new CustomWeapon(child.getTextContent().trim());
            }
        }

        if (wt == null) {
            logger.log(WeaponLoader.class, "loadWeaponType(Document)", LogLevel.ERROR,
                    filename + " must have <BaseType> or <WeaponHandler> child but none found.");
            return null;
        }

        EquipmentLoader.loadEquipmentType(filename, doc, wt);

        for (int i = 0; i < root.getChildNodes().getLength(); i++) {
            Node child = root.getChildNodes().item(i);

            if (child.getNodeName().equalsIgnoreCase("heat")) {
                wt.heat = Integer.parseInt(child.getTextContent());
            } else if (child.getNodeName().equalsIgnoreCase("damage")) {
                wt.damage = parseDamage(child.getTextContent(), filename);
            } else if (child.getNodeName().equalsIgnoreCase("damageShort")) {
                wt.damageShort = Integer.parseInt(child.getTextContent());
            } else if (child.getNodeName().equalsIgnoreCase("damageMedium")) {
                wt.damageMedium = Integer.parseInt(child.getTextContent());
            } else if (child.getNodeName().equalsIgnoreCase("damageLong")) {
                wt.damageLong = Integer.parseInt(child.getTextContent());
            } else if (child.getNodeName().equalsIgnoreCase("explosionDamage")) {
                wt.explosionDamage = Integer.parseInt(child.getTextContent());
            } else if (child.getNodeName().equalsIgnoreCase("rackSize")) {
                wt.rackSize = Integer.parseInt(child.getTextContent());
            } else if (child.getNodeName().equalsIgnoreCase("ammoType")) {
                wt.ammoType = EquipmentLoader.parseAmmoType(child.getTextContent(), filename);
            } else if (child.getNodeName().equalsIgnoreCase("minimumRange")) {
                wt.minimumRange = Integer.parseInt(child.getTextContent());
            } else if (child.getNodeName().equalsIgnoreCase("shortRange")) {
                wt.shortRange = Integer.parseInt(child.getTextContent());
            } else if (child.getNodeName().equalsIgnoreCase("mediumRange")) {
                wt.mediumRange = Integer.parseInt(child.getTextContent());
            } else if (child.getNodeName().equalsIgnoreCase("longRange")) {
                wt.longRange = Integer.parseInt(child.getTextContent());
            } else if (child.getNodeName().equalsIgnoreCase("extremeRange")) {
                wt.extremeRange = Integer.parseInt(child.getTextContent());
            } else if (child.getNodeName().equalsIgnoreCase("waterShortRange")) {
                wt.waterShortRange = Integer.parseInt(child.getTextContent());
            } else if (child.getNodeName().equalsIgnoreCase("waterMediumRange")) {
                wt.waterMediumRange = Integer.parseInt(child.getTextContent());
            } else if (child.getNodeName().equalsIgnoreCase("waterLongRange")) {
                wt.waterLongRange = Integer.parseInt(child.getTextContent());
            } else if (child.getNodeName().equalsIgnoreCase("waterExtremeRange")) {
                wt.waterExtremeRange = Integer.parseInt(child.getTextContent());
            } else if (child.getNodeName().equalsIgnoreCase("infDamageClass")) {
                wt.infDamageClass = parseInfDamageClass(child.getTextContent(), filename);
            } else if (child.getNodeName().equalsIgnoreCase("shortAV")) {
                wt.shortAV = Double.parseDouble(child.getTextContent());
            } else if (child.getNodeName().equalsIgnoreCase("medAV")) {
                wt.medAV = Double.parseDouble(child.getTextContent());
            } else if (child.getNodeName().equalsIgnoreCase("longAV")) {
                wt.longAV = Double.parseDouble(child.getTextContent());
            } else if (child.getNodeName().equalsIgnoreCase("extAV")) {
                wt.extAV = Double.parseDouble(child.getTextContent());
            } else if (child.getNodeName().equalsIgnoreCase("maxRange")) {
                wt.maxRange = parseMaxRange(child.getTextContent(), filename);
            } else if (child.getNodeName().equalsIgnoreCase("capital")) {
                wt.capital = Boolean.parseBoolean(child.getTextContent());
            } else if (child.getNodeName().equalsIgnoreCase("subCapital")) {
                wt.subCapital = Boolean.parseBoolean(child.getTextContent());
            } else if (child.getNodeName().equals("atClass")) {
                wt.atClass = parseATClass(child.getTextContent(), filename);
            } else if (child.getNodeName().equalsIgnoreCase("flags")) {
                for (int j = 0; j < child.getChildNodes().getLength(); ++j) {
                    wt.flags.or(parseFlag(child.getChildNodes().item(j).getTextContent(), filename));
                }
            } else {
                logger.log(EquipmentLoader.class, "loadEquipmentType", "Unknown element "
                        + child.getNodeName() + " in " + filename);
            }
        }

        return wt;
    }

    public static void loadCustomWeapons(List<WeaponType> weapons, File fDir) {
        String[] sa = fDir.list();
        if (sa != null) {
            for (String element : sa) {
                File f = new File(fDir, element);

                if (f.isDirectory()) {
                    loadCustomWeapons(weapons, f);
                } else if (f.getName().toLowerCase().endsWith(".xml")) {
                    try {
                        WeaponType wt = loadWeaponType(f.getName(), DocumentBuilderFactory
                                .newInstance()
                                .newDocumentBuilder()
                                .parse(new FileInputStream(f)));
                        if (wt != null) {
                            weapons.add(wt);
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
