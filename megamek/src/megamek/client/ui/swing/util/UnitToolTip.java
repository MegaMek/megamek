package megamek.client.ui.swing.util;

import java.awt.Image;
import java.io.File;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import megamek.client.ui.Messages;
import megamek.client.ui.swing.GUIPreferences;
import megamek.client.ui.swing.tileset.MMStaticDirectoryManager;
import megamek.common.*;
import megamek.common.IGame.Phase;
import megamek.common.options.*;

public class UnitToolTip {
    
    private final static boolean BR = true;
    private final static boolean NOBR = false;
    
    private StringBuffer tip = new StringBuffer();
    private boolean skipBRAfterTableAfterTable = false;
    private Entity entity;
    private IGame game;
    private IPlayer localPlayer;
    private Crew crew;

    public static String getTooltip(Entity en, IPlayer lp) {
        UnitToolTip toolTip = new UnitToolTip(en, lp);
        return toolTip.tip.toString();
    }
    
    private UnitToolTip(Entity en, IPlayer lp) {
        entity = en;
        game = en.getGame();
        crew = en.getCrew();
        localPlayer = lp;
        tip = createTooltip();
    }

    private void addToTT(String tipName, boolean startBR, Object... ttO) {
        if (startBR == BR){
            if (skipBRAfterTableAfterTable) {
                skipBRAfterTableAfterTable = false;
            } else {
                tip.append("<BR>");
            }
        }
        if (ttO != null) {
            tip.append(Messages.getString("BoardView1.Tooltip." + tipName, ttO));
        } else {
            tip.append(Messages.getString("BoardView1.Tooltip." + tipName));
        }
    }

    public StringBuffer createTooltip() {
        // Tooltip info for a sensor blip
        if (onlyDetectedBySensors())
            return new StringBuffer(Messages.getString("BoardView1.sensorReturn"));

        // No sensor blip...
        Infantry thisInfantry = null;
        if (entity instanceof Infantry) thisInfantry = (Infantry) entity;
        GunEmplacement thisGunEmp = null;
        if (entity instanceof GunEmplacement) thisGunEmp = (GunEmplacement) entity;
        IAero thisAero = null;
        if (entity.isAero()) thisAero = (IAero) entity;

        StringBuffer tip = new StringBuffer();
        boolean skipBRAfterTable = false;

        // Unit Chassis and Player
        addToTT("Unit", NOBR,
                Integer.toHexString(PlayerColors.getColorRGB(entity.getOwner().getColorIndex())), 
                entity.getChassis(), entity.getOwner().getName());

        // Pilot Info
        // Put everything in table to allow for a pilot photo in second column
        addToTT("PilotStart", BR);

        // Nickname > Name > "Pilot"
        for (int i = 0; i < crew.getSlotCount(); i++) {
            String pnameStr = "Pilot";

            if (crew.isMissing(i)) {
                continue;
            }

            if ((crew.getName(i) != null) && !crew.getName(i).equals("")) {
                pnameStr = crew.getName(i);
            }

            if ((crew.getNickname(i) != null) && !crew.getNickname(i).equals("")) {
                pnameStr = "'" + crew.getNickname(i) + "'";
            }

            if (crew.getSlotCount() > 1) {
                pnameStr += " (" + crew.getCrewType().getRoleName(i) + ")";
            }

            addToTT("Pilot", NOBR, pnameStr,
                    crew.getSkillsAsString(
                            game.getOptions().booleanOption(OptionsConstants.RPG_RPG_GUNNERY)));

            if (!crew.getStatusDesc(i).equals("")) {
                addToTT("PilotStatus", NOBR, crew.getStatusDesc(i));
            }
        }

        // Pilot Advantages
        int numAdv = crew.countOptions(PilotOptions.LVL3_ADVANTAGES);
        if (numAdv == 1) {
            addToTT("Adv1", NOBR, numAdv);
        } else if (numAdv > 1) { 
            addToTT("Advs", NOBR, numAdv);
        }

        // Pilot Manei Domini
        if ((crew.countOptions(PilotOptions.MD_ADVANTAGES) > 0)) { 
            addToTT("MD", NOBR);
        }

        if (entity instanceof Infantry) {
            Infantry inf = (Infantry) entity;
            int spec = inf.getSpecializations();
            if (spec > 0) {
                addToTT("InfSpec", BR, Infantry.getSpecializationName(spec));
            }
        }

        //add portrait?
        if (null != crew) {
            String category = crew.getPortraitCategory(0);
            String file = crew.getPortraitFileName(0);
            if (GUIPreferences.getInstance().getBoolean(GUIPreferences.SHOW_PILOT_PORTRAIT_TT) &&
                    (null != category) && (null != file)) {
                String imagePath = Configuration.portraitImagesDir() + "/" + category + file;
                File f = new File(imagePath);
                if(f.exists()) {
                    // HACK: Get the real portrait to find the size of the image
                    // and scale the tooltip HTML IMG accordingly
                    Image portrait = MMStaticDirectoryManager.getUnscaledPortraitImage(category, file);
                    if (portrait.getWidth(null) > portrait.getHeight(null)) {
                        float h = 60f * portrait.getHeight(null) / portrait.getWidth(null);
                        addToTT("PilotPortraitW", BR, imagePath, (int) h);
                    } else {
                        float w = 60f * portrait.getWidth(null) / portrait.getHeight(null);
                        addToTT("PilotPortraitH", BR, imagePath, (int) w);
                    }
                }
            }
        }

        addToTT("PilotEnd", NOBR);

        // Unit movement ability
        if (thisGunEmp == null) {
            addToTT("Movement", BR, entity.getWalkMP(), entity.getRunMPasString());
            if (entity.getJumpMP() > 0) tip.append("/" + entity.getJumpMP());
        }

        // Armor and Internals
        addToTT("ArmorInternals", BR, entity.getTotalArmor(),
                entity.getTotalInternal());

        // Build a "status bar" visual representation of each
        // component of the unit using block element characters.
        if (GUIPreferences.getInstance().getBoolean(GUIPreferences.SHOW_ARMOR_MINIVIS_TT)) {
            addArmorMiniVisToTT();
            skipBRAfterTable = true;
        }


        // BV Info
        // Only show this if we aren't in double blind and hide enemy bv isn't selected.
        // Should always see this on your own Entities.
        boolean suppressEnemyBV = game.getOptions().booleanOption(OptionsConstants.ADVANCED_SUPPRESS_DB_BV) &&
                game.getOptions().booleanOption(OptionsConstants.ADVANCED_DOUBLE_BLIND);

        if (!(suppressEnemyBV && !EntityVisibilityUtils.trackThisEntitiesVisibilityInfo(localPlayer, entity))) {
            int currentBV = entity.calculateBattleValue(false, false);
            int initialBV = entity.getInitialBV();
            double percentage = (double) currentBV / initialBV;

            addToTT("BV", BR, currentBV, initialBV, percentage);
        }

        // Heat, not shown for units with 999 heat sinks (vehicles)
        if (entity.getHeatCapacity() != 999) {
            if (entity.heat == 0) {
                addToTT("Heat0", BR);
            } else { 
                addToTT("Heat", BR, entity.heat);
            }
        }

        // Actual Movement
        if (thisGunEmp == null) {
            // In the Movement Phase, unit not done
            if (!entity.isDone() && game.getPhase() == Phase.PHASE_MOVEMENT) {
                // "Has not yet moved" only during movement phase
                addToTT("NotYetMoved", BR);

                // In the Movement Phase, unit is done - or in the Firing Phase
            } else if (
                    (entity.isDone() && game.getPhase() == Phase.PHASE_MOVEMENT) 
                    || game.getPhase() == Phase.PHASE_FIRING) {
                int tmm = Compute.getTargetMovementModifier(game,
                        entity.getId()).getValue();
                // Unit didn't move
                if (entity.moved == EntityMovementType.MOVE_NONE) {
                    addToTT("NoMove", BR, tmm);

                    // Unit did move
                } else {
                    // Actual movement and modifier
                    addToTT("MovementF", BR,
                            entity.getMovementString(entity.moved),
                            entity.delta_distance,
                            tmm);
                }
                // Special Moves
                if (entity.isEvading()) 
                    addToTT("Evade", NOBR);

                if ((thisInfantry != null) && (thisInfantry.isTakingCover())) 
                    addToTT("TakingCover", NOBR);

                if (entity.isCharging()) 
                    addToTT("Charging", NOBR);

                if (entity.isMakingDfa()) 
                    addToTT("DFA", NOBR);
            }
        }

        // ASF Velocity
        if (thisAero != null) {
            addToTT("AeroVelocity", BR, thisAero.getCurrentVelocity());
        }

        // Gun Emplacement Status
        if (thisGunEmp != null) {  
            if (thisGunEmp.isTurret() && thisGunEmp.isTurretLocked(thisGunEmp.getLocTurret())) 
                addToTT("TurretLocked", BR);
        }

        // Unit Immobile
        if ((thisGunEmp == null) && (entity.isImmobile()))
            addToTT("Immobile", BR);

        if (entity.isHiddenActivating()) {
            addToTT("HiddenActivating", BR,
                    IGame.Phase.getDisplayableName(entity
                            .getHiddenActivationPhase()));
        } else if (entity.isHidden()) {
            addToTT("Hidden", BR);
        }

        // Jammed by ECM
        if (isAffectedByECM()) {
            addToTT("Jammed", BR);
        }

        // Swarmed
        if (entity.getSwarmAttackerId() != Entity.NONE) {
            addToTT("Swarmed", BR,
                    game.getEntity(entity.getSwarmAttackerId())
                    .getDisplayName());
        }

        // Spotting
        if (entity.isSpotting()) {
            addToTT("Spotting", BR, game.getEntity(entity.getSpotTargetId()).getDisplayName());
        }

        // If DB, add information about who sees this Entity
        if (game.getOptions().booleanOption(OptionsConstants.ADVANCED_DOUBLE_BLIND)) {
            StringBuffer playerList = new StringBuffer();
            boolean teamVision = game.getOptions().booleanOption(
                    OptionsConstants.ADVANCED_TEAM_VISION);
            for (IPlayer player : entity.getWhoCanSee()) {
                if (player.isEnemyOf(entity.getOwner()) || !teamVision) {
                    playerList.append(player.getName());
                    playerList.append(", ");
                }
            }
            if (playerList.length() > 1) {
                playerList.delete(playerList.length() - 2, playerList.length());
                addToTT("SeenBy", BR, playerList.toString());
            }            
        }

        // If sensors, display what sensors this unit is using
        if (game.getOptions().booleanOption(OptionsConstants.ADVANCED_TACOPS_SENSORS)
                || game.getOptions().booleanOption(OptionsConstants.ADVAERORULES_STRATOPS_ADVANCED_SENSORS)) {
            addToTT("Sensors", BR, entity.getSensorDesc());
        }

        // Towing
        if (entity.getAllTowedUnits().size() > 0) {
            String unitList = entity.getAllTowedUnits().stream()
                    .map(id -> entity.getGame().getEntity(id).getDisplayName())
                    .collect(Collectors.joining(", "));
            if (unitList.length() > 1) {
                addToTT("Towing", BR, unitList);
            }
        }

        // Weapon List
        if (GUIPreferences.getInstance()
                .getBoolean(GUIPreferences.SHOW_WPS_IN_TT)) {

            ArrayList<Mounted> weapons = entity.getWeaponList();
            HashMap<String, Integer> wpNames = new HashMap<String,Integer>();

            // Gather names, counts, Clan/IS
            // When clan then the number will be stored as negative
            for (Mounted curWp: weapons) {
                String weapDesc = curWp.getDesc();
                // Append ranges
                WeaponType wtype = (WeaponType)curWp.getType();
                int ranges[];
                if (entity.isAero()) {
                    ranges = wtype.getATRanges();
                } else {
                    ranges = wtype.getRanges(curWp);
                }
                String rangeString = " \u22EF ";
                if ((ranges[RangeType.RANGE_MINIMUM] != WeaponType.WEAPON_NA) 
                        && (ranges[RangeType.RANGE_MINIMUM] != 0)) {
                    rangeString += "(" + ranges[RangeType.RANGE_MINIMUM] + ") ";
                }
                int maxRange = RangeType.RANGE_LONG;
                if (game.getOptions().booleanOption(
                        OptionsConstants.ADVCOMBAT_TACOPS_RANGE)) {
                    maxRange = RangeType.RANGE_EXTREME;
                }
                for (int i = RangeType.RANGE_SHORT; i <= maxRange; i++) {
                    rangeString += ranges[i];
                    if (i != maxRange) {
                        rangeString += "\u2B1D";
                    }
                }
                weapDesc += rangeString;
                if (wpNames.containsKey(weapDesc)) {
                    int number = wpNames.get(weapDesc);
                    if (number > 0) 
                        wpNames.put(weapDesc, number + 1);
                    else 
                        wpNames.put(weapDesc, number - 1);
                } else {
                    WeaponType wpT = ((WeaponType)curWp.getType());

                    if (entity.isClan() && TechConstants.isClan(wpT.getTechLevel(entity.getYear()))) 
                        wpNames.put(weapDesc, -1);
                    else
                        wpNames.put(weapDesc, 1);
                }
            }

            // Print to Tooltip
            tip.append("<FONT SIZE=\"-2\">");

            for (Entry<String, Integer> entry : wpNames.entrySet()) {
                // Check if weapon is destroyed, text gray and strikethrough if so, remove the "x "/"*"
                // Also remove "+", means currently selected for firing
                boolean wpDest = false;
                String nameStr = entry.getKey();
                if (entry.getKey().startsWith("x ")) { 
                    nameStr = entry.getKey().substring(2, entry.getKey().length());
                    wpDest = true;
                }

                if (entry.getKey().startsWith("*")) { 
                    nameStr = entry.getKey().substring(1, entry.getKey().length());
                    wpDest = true;
                }

                if (entry.getKey().startsWith("+")) { 
                    nameStr = entry.getKey().substring(1, entry.getKey().length());
                    nameStr = nameStr.concat(" <I>(Firing)</I>");
                }

                // normal coloring 
                tip.append("<FONT COLOR=#8080FF>");
                // but: color gray and strikethrough when weapon destroyed
                if (wpDest) tip.append("<FONT COLOR=#a0a0a0><S>");

                String clanStr = "";
                if (entry.getValue() < 0) clanStr = Messages.getString("BoardView1.Tooltip.Clan");

                // when more than 5 weapons are present, they will be grouped
                // and listed with a multiplier
                if (weapons.size() > 5) {
                    addToTT("WeaponN", BR, Math.abs(entry.getValue()), clanStr, nameStr);

                } else { // few weapons: list each weapon separately
                    for (int i = 0; i < Math.abs(entry.getValue()); i++) {
                        addToTT("Weapon", BR, Math.abs(entry.getValue()), clanStr, nameStr);
                    }
                }
                // Weapon destroyed? End strikethrough
                if (wpDest) tip.append("</S>");
                tip.append("</FONT>"); 
            }
            tip.append("</FONT>");
        }
        return tip;
    }
    
    private void addArmorMiniVisToTT() {
        String armorChar = GUIPreferences.getInstance().getString("AdvancedArmorMiniArmorChar");
        String internalChar = GUIPreferences.getInstance().getString("AdvancedArmorMiniISChar");
        String destroyedChar = GUIPreferences.getInstance().getString("AdvancedArmorMiniDestroyedChar");
        String fontSize = Integer.toString(GUIPreferences.getInstance().getInt("AdvancedArmorMiniFrontSizeMod"));
        // HTML color String from Preferences
        String colorIntact = Integer
                .toHexString(GUIPreferences.getInstance()
                        .getColor("AdvancedArmorMiniColorIntact").getRGB() & 0xFFFFFF);
        String colorPartialDmg = Integer
                .toHexString(GUIPreferences.getInstance()
                        .getColor("AdvancedArmorMiniColorPartialDmg").getRGB() & 0xFFFFFF);
        String colorDamaged = Integer
                .toHexString(GUIPreferences.getInstance()
                        .getColor("AdvancedArmorMiniColorDamaged").getRGB() & 0xFFFFFF);
        int visUnit = GUIPreferences.getInstance().getInt("AdvancedArmorMiniUnitsPerBlock");
        addToTT("ArmorMiniPanelStart", BR);
        for (int loc = 0 ; loc < entity.locations(); loc++) {
            // addToTT("ArmorMiniPanelPart", BR, getLocationAbbr(loc));
            // If location is destroyed, mark it and move on
            if (entity.getInternal(loc) == IArmorState.ARMOR_DOOMED ||
                    entity.getInternal(loc) == IArmorState.ARMOR_DESTROYED) {
                // This is a really awkward way of making sure
                addToTT("ArmorMiniPanelPartNoRear", BR, entity.getLocationAbbr(loc), fontSize);
                for (int a = 0; a <= entity.getOInternal(loc)/visUnit; a++) {
                    addToTT("BlockColored", NOBR, destroyedChar, fontSize, colorDamaged);
                }

            } else {
                // Put rear armor blocks first, with some spacing, if unit has any.
                if (entity.hasRearArmor(loc)) {
                    addToTT("ArmorMiniPanelPartRear", BR, entity.getLocationAbbr(loc), fontSize);
                    for (int a = 0; a <= (entity.getOArmor(loc, true)/visUnit); a++) {
                        if (a < (entity.getArmor(loc, true)/visUnit)) {
                            addToTT("BlockColored", NOBR, armorChar, fontSize, colorIntact);
                        } else if (a == (entity.getArmor(loc, true)/visUnit) &&
                                (entity.getArmor(loc, true) % visUnit) > 0) {
                            // Fraction of a visUnit left, but still display a "full" if at starting max armor
                            if (entity.getArmor(loc, true) == entity.getOArmor(loc, true)) {
                                addToTT("BlockColored", NOBR, armorChar, fontSize, colorIntact);
                            } else {
                                addToTT("BlockColored", NOBR, armorChar, fontSize, colorPartialDmg);;
                            }
                        } else if ((entity.getOArmor(loc, true) % visUnit) > 0) {
                            addToTT("BlockColored", NOBR, armorChar, fontSize, colorDamaged);
                        }
                    }
                    tip.append("&nbsp;&nbsp;");
                    addToTT("ArmorMiniPanelPart", BR, entity.getLocationAbbr(loc), fontSize);
                } else {
                    addToTT("ArmorMiniPanelPartNoRear", BR, entity.getLocationAbbr(loc), fontSize);
                }
                // Add IS shade blocks.
                for (int a = 0; a <= (entity.getOInternal(loc)/visUnit); a++) {
                    if (a < (entity.getInternal(loc)/visUnit)) {
                        addToTT("BlockColored", NOBR, internalChar, fontSize, colorIntact);
                    } else if (a == (entity.getInternal(loc)/visUnit) &&
                            (entity.getInternal(loc) % visUnit) > 0) {
                        // Fraction of a visUnit left, but still display a "full" if at starting max armor
                        if (entity.getInternal(loc) == entity.getOInternal(loc)) {
                            addToTT("BlockColored", NOBR, internalChar, fontSize, colorIntact);
                        } else {
                            addToTT("BlockColored", NOBR, internalChar, fontSize, colorPartialDmg);
                        }
                    } else if ((entity.getOInternal(loc) % visUnit) > 0) {
                        addToTT("BlockColored", NOBR, internalChar, fontSize, colorDamaged);
                    }
                }
                // Add main armor blocks.
                for (int a = 0; a <= (entity.getOArmor(loc)/visUnit); a++) {
                    if (a < (entity.getArmor(loc)/visUnit)) {
                        addToTT("BlockColored", NOBR, armorChar, fontSize, colorIntact);
                    } else if (a == (entity.getArmor(loc)/visUnit) &&
                            (entity.getArmor(loc) % visUnit) > 0) {
                        // Fraction of a visUnit left, but still display a "full" if at starting max armor
                        if (entity.getArmor(loc) == entity.getOArmor(loc)) {
                            addToTT("BlockColored", NOBR, armorChar, fontSize, colorIntact);
                        } else {
                            addToTT("BlockColored", NOBR, armorChar, fontSize, colorPartialDmg);
                        }
                    } else if ((entity.getOArmor(loc) % visUnit) > 0){
                        addToTT("BlockColored", NOBR, armorChar, fontSize, colorDamaged);
                    }
                }
            }

        }
        addToTT("ArmorMiniPanelEnd", NOBR);
    }

}
