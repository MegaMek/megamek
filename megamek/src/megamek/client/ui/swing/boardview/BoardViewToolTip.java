package megamek.client.ui.swing.boardview;

import megamek.client.ui.Messages;
import megamek.client.ui.swing.ClientGUI;
import megamek.client.ui.swing.GUIPreferences;
import megamek.client.ui.swing.tooltip.HexTooltip;
import megamek.client.ui.swing.tooltip.PilotToolTip;
import megamek.client.ui.swing.tooltip.UnitToolTip;
import megamek.client.ui.swing.util.KeyCommandBind;
import megamek.client.ui.swing.util.UIUtil;
import megamek.common.*;
import megamek.common.actions.ArtilleryAttackAction;
import megamek.common.annotations.Nullable;
import megamek.common.options.OptionsConstants;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.*;
import java.util.List;

import static megamek.client.ui.swing.util.UIUtil.guiScaledFontHTML;
import static megamek.client.ui.swing.util.UIUtil.uiWhite;

class BoardViewToolTip {

    private final GUIPreferences GUIP = GUIPreferences.getInstance();

    private final ClientGUI clientGui;
    private final Game game;

    BoardViewToolTip(Game game, ClientGUI clientGui) {
        this.clientGui = clientGui;
        this.game = game;
    }

    String getHexTooltip(MouseEvent event, BoardView bv) {
        return getHexTooltip(event.getPoint(), bv, null);
    }

    String getHexTooltip(MouseEvent event, BoardView bv, Coords movementTarget) {
        return getHexTooltip(event.getPoint(), bv, movementTarget);
    }

    /**
     * @return HTML summarizing the terrain, units and deployment of the hex under the mouse
     */
    String getHexTooltip(Point point, BoardView bv, Coords movementTarget) {
        final Coords coords = bv.getCoordsAt(point);
        if (!game.getBoard().contains(coords)) {
            return null;
        }
        Entity selectedEntity = clientGui != null ? clientGui.getSelectedUnit() : null;
        Hex mhex = game.getBoard().getHex(coords);

        String result = "";

        // Hex Terrain
        if (GUIP.getShowMapHexPopup() && (mhex != null)) {
            StringBuffer sbTerrain = new StringBuffer();
            appendTerrainTooltip(sbTerrain, mhex, GUIP);
            String sTrerain = sbTerrain.toString();

            // Distance from the selected unit and a planned movement end point
            if ((selectedEntity != null) && (selectedEntity.getPosition() != null)) {
                int distance = selectedEntity.getPosition().distance(coords);
                if (distance == 1) {
                    sTrerain += Messages.getString("BoardView1.Tooltip.Distance1");
                } else {
                    sTrerain += Messages.getString("BoardView1.Tooltip.DistanceN", distance);
                }

                if (game.getOptions().booleanOption(OptionsConstants.ADVANCED_TACOPS_SENSORS)) {
                    LosEffects los = bv.fovHighlightingAndDarkening.getCachedLosEffects(selectedEntity.getPosition(), coords);
                    int bracket = Compute.getSensorRangeBracket(selectedEntity, null,
                            bv.fovHighlightingAndDarkening.cachedAllECMInfo);
                    int range = Compute.getSensorRangeByBracket(game, selectedEntity, null, los);

                    int maxSensorRange = bracket * range;
                    int minSensorRange = Math.max((bracket - 1) * range, 0);
                    if (game.getOptions().booleanOption(OptionsConstants.ADVANCED_INCLUSIVE_SENSOR_RANGE)) {
                        minSensorRange = 0;
                    }
                    sTrerain += "<BR>";
                    if ((distance > minSensorRange) && (distance <= maxSensorRange)) {
                        sTrerain += Messages.getString("BoardView1.Tooltip.SensorsHexInRange");
                    } else {
                        sTrerain += Messages.getString("BoardView1.Tooltip.SensorsHexNotInRange1");
                        String tmp = Messages.getString("BoardView1.Tooltip.SensorsHexNotInRange2");
                        sTrerain += guiScaledFontHTML(GUIP.getWarningColor()) + tmp + "<FONT>";
                        sTrerain += Messages.getString("BoardView1.Tooltip.SensorsHexNotInRange3");
                    }
                }

                if (game.getPhase().isMovement() && (movementTarget != null)) {
                    sTrerain += "<BR>";
                    int disPM = movementTarget.distance(coords);
                    String sDinstanceMove = "";
                    if (disPM == 1) {
                        sDinstanceMove = Messages.getString("BoardView1.Tooltip.DistanceMove1");
                    } else {
                        sDinstanceMove = Messages.getString("BoardView1.Tooltip.DistanceMoveN", disPM);
                    }
                    sTrerain += "<I>" + sDinstanceMove + "</I>";
                }
            }

            sTrerain = guiScaledFontHTML(GUIP.getUnitToolTipTerrainFGColor()) + sTrerain + "</FONT>";
            String col = "<TD>" + sTrerain + "</TD>";
            String row = "<TR>" + col + "</TR>";
            String table = "<TABLE BORDER=0 BGCOLOR=" + GUIPreferences.hexColor(GUIP.getUnitToolTipTerrainBGColor()) + " width=100%>" + row + "</TABLE>";
            result += table;

            StringBuffer sbBuildings = new StringBuffer();
            appendBuildingsTooltip(sbBuildings, mhex);
            result += sbBuildings.toString();

            if (bv.displayInvalidFields()) {
                StringBuffer errBuff = new StringBuffer();
                if (!mhex.isValid(errBuff)) {
                    String sInvalidHex = Messages.getString("BoardView1.invalidHex");
                    sInvalidHex += "<BR>";
                    String errors = errBuff.toString();
                    errors = errors.replace("\n", "<BR>");
                    sInvalidHex += errors;
                    sInvalidHex = guiScaledFontHTML(GUIP.getUnitToolTipFGColor()) + sInvalidHex + "</FONT>";
                    result += "<BR>" + sInvalidHex;
                }
            }
        }

        // Show the player(s) that may deploy here
        // in the artillery autohit designation phase
        if (game.getPhase().isSetArtilleryAutohitHexes() && (mhex != null)) {
            String sAttilleryAutoHix = "";
            Enumeration<Player> allP = game.getPlayers();
            boolean foundPlayer = false;
            // loop through all players
            while (allP.hasMoreElements()) {
                Player cp = allP.nextElement();
                if (game.getBoard().isLegalDeployment(coords, cp)) {
                    if (!foundPlayer) {
                        foundPlayer = true;
                        sAttilleryAutoHix += Messages.getString("BoardView1.Tooltip.ArtyAutoHeader") + "<BR>";
                    }

                    String sName = "&nbsp;&nbsp;" + cp.getName();
                    sName = guiScaledFontHTML(cp.getColour().getColour()) + sName + "</FONT>";
                    sAttilleryAutoHix += "<B>" + sName + "</B>";
                    sAttilleryAutoHix += "<BR>";
                }
            }
            if (foundPlayer) {
                sAttilleryAutoHix += "<BR>";
            }

            // Add a hint with keybind that the zones can be shown graphically
            String keybindText = KeyCommandBind.getDesc(KeyCommandBind.getBindByCmd("autoArtyDeployZone"));
            String msg_artyautohit = Messages.getString("BoardView1.Tooltip.ArtyAutoHint1") + "<BR>";
            msg_artyautohit += Messages.getString("BoardView1.Tooltip.ArtyAutoHint2") + "<BR>";
            msg_artyautohit += Messages.getString("BoardView1.Tooltip.ArtyAutoHint3", keybindText);
            sAttilleryAutoHix += "<I>" + msg_artyautohit + "</I>";

            sAttilleryAutoHix = guiScaledFontHTML(uiWhite()) + sAttilleryAutoHix + "</FONT>";

            String col = "<TD>" + sAttilleryAutoHix + "</TD>";
            String row = "<TR>" + col + "</TR>";
            String table = "<TABLE BORDER=0 width=100%>" + row + "</TABLE>";
            result += table;
        }

        // check if it's on any flares
        for (FlareSprite fSprite : bv.flareSprites) {
            if (fSprite.isInside(point)) {
                result += fSprite.getTooltip().toString();
            }
        }

        // Add wreck info
        var wreckList = bv.useIsometric() ? bv.isometricWreckSprites : bv.wreckSprites;
        for (var wSprite : wreckList) {
            if (wSprite.getPosition().equals(coords)) {
                String sWreck = wSprite.getTooltip().toString();
                sWreck = guiScaledFontHTML(GUIP.getUnitToolTipAltFGColor()) + sWreck + "</FONT>";
                String col = "<TD>" + sWreck + "</TD>";
                String row = "<TR>" + col + "</TR>";
                String rows = row;

                if (!wSprite.entity.getCrew().isEjected()) {
                    String sPilot = PilotToolTip.getPilotTipShort(wSprite.entity, GUIP.getshowPilotPortraitTT(), false).toString();
                    col = "<TD>" + sPilot + "</TD>";
                    row = "<TR>" + col + "</TR>";
                    rows += row;
                }

                String table = "<TABLE BORDER=0 BGCOLOR=" + GUIPreferences.hexColor(GUIP.getUnitToolTipAltBGColor()) + " width=100%>" + rows + "</TABLE>";
                result += table;
            }
        }

        // Entity tooltips
        int entityCount = 0;
        // Maximum number of entities to show in the tooltip
        int maxShown = 4;
        boolean hidden = false;

        Set<Entity> coordEnts = new HashSet<>(game.getEntitiesVector(coords, true));
        for (Entity entity : coordEnts) {
            entityCount++;

            // List only the first four units
            if (entityCount <= maxShown) {
                if (EntityVisibilityUtils.detectedOrHasVisual(localPlayer(), game, entity)) {
                    StringBuffer sbEntity = new StringBuffer();
                    appendEntityTooltip(sbEntity, entity);
                    result += sbEntity.toString();
                } else {
                    hidden = true;
                }
            }
        }
        // Info block if there are more than 4 units in that hex
        if (entityCount > maxShown && !hidden) {
            String sUnitsInfo = "There ";
            if (entityCount - maxShown == 1) {
                sUnitsInfo += "is 1 more<BR>unit";
            } else {
                sUnitsInfo += "are " + (entityCount - maxShown) + " more<BR>units";
            }
            sUnitsInfo += " in this hex...";

            sUnitsInfo = guiScaledFontHTML(GUIP.getUnitToolTipBlockFGColor()) + sUnitsInfo + "</FONT>";
            String col = "<TD>" + sUnitsInfo + "</TD>";
            String row = "<TR>" + col + "</TR>";
            String table = "<TABLE BORDER=0 BGCOLOR=" + GUIPreferences.hexColor(GUIP.getUnitToolTipBlockBGColor()) + " width=100%>" + row + "</TABLE>";
            result += table;
        }

        // check if it's on any attacks
        for (AttackSprite aSprite : bv.attackSprites) {
            if (aSprite.isInside(coords)) {
                String sAttackSprite = aSprite.getTooltip().toString();
                sAttackSprite = guiScaledFontHTML(GUIP.getUnitToolTipAltFGColor()) + sAttackSprite + "</FONT>";
                String col = "<TD>" + sAttackSprite + "</TD>";
                String row = "<TR>" + col + "</TR>";
                String table = "<TABLE BORDER=0 BGCOLOR=" + GUIPreferences.hexColor(GUIP.getUnitToolTipAltBGColor()) + " width=100%>" + row + "</TABLE>";
                result += table;
            }
        }

        // Artillery attacks
        for (ArtilleryAttackAction aaa : getArtilleryAttacksAtLocation(game, coords)) {
            // Default texts if no real names can be found
            String wpName = Messages.getString("BoardView1.Artillery");
            String ammoName = "Unknown";

            // Get real weapon and ammo name
            final Entity artyEnt = game.getEntity(aaa.getEntityId());
            if (artyEnt != null) {
                if (aaa.getWeaponId() > -1) {
                    wpName = artyEnt.getEquipment(aaa.getWeaponId()).getName();
                    if (aaa.getAmmoId() > -1) {
                        ammoName = artyEnt.getEquipment(aaa.getAmmoId()).getName();
                    }
                }
            }

            String msg_artilleryatack;

            if (aaa.getTurnsTilHit() == 1) {
                msg_artilleryatack = Messages.getString("BoardView1.Tooltip.ArtilleryAttackOne1", wpName);
                msg_artilleryatack += "<BR>&nbsp;&nbsp;";
                msg_artilleryatack += Messages.getString("BoardView1.Tooltip.ArtilleryAttackOne2", ammoName);
            } else {
                msg_artilleryatack = Messages.getString("BoardView1.Tooltip.ArtilleryAttackN1", wpName, aaa.getTurnsTilHit());
                msg_artilleryatack += "<BR>&nbsp;&nbsp;";
                msg_artilleryatack += Messages.getString("BoardView1.Tooltip.ArtilleryAttackN2", ammoName);
            }

            msg_artilleryatack = guiScaledFontHTML(GUIP.getUnitToolTipBlockFGColor()) + msg_artilleryatack + "</FONT>";
            String col = "<TD>" + msg_artilleryatack + "</TD>";
            String row = "<TR>" + col + "</TR>";
            String table = "<TABLE BORDER=0 BGCOLOR=" + GUIPreferences.hexColor(GUIP.getUnitToolTipBlockBGColor()) + " width=100%>" + row + "</TABLE>";
            result += table;
        }

        // Artillery fire adjustment
        final Mounted curWeapon = getSelectedArtilleryWeapon();
        if ((curWeapon != null) && (selectedEntity != null)) {
            // process targeted hexes
            int amod = 0;
            // Check the predesignated hexes
            if (selectedEntity.getOwner().getArtyAutoHitHexes().contains(coords)) {
                amod = TargetRoll.AUTOMATIC_SUCCESS;
            } else {
                amod = selectedEntity.aTracker.getModifier(curWeapon, coords);
            }

            String msg_artilleryautohit;

            if (amod == TargetRoll.AUTOMATIC_SUCCESS) {
                msg_artilleryautohit = Messages.getString("BoardView1.ArtilleryAutohit");
            } else {
                msg_artilleryautohit = Messages.getString("BoardView1.ArtilleryAdjustment", amod);
            }
            msg_artilleryautohit = guiScaledFontHTML(UIUtil.uiWhite()) + msg_artilleryautohit + "</FONT>";
            result += msg_artilleryautohit + "<BR>";
        }

        final Collection<SpecialHexDisplay> shdList = game.getBoard().getSpecialHexDisplay(coords);
        int round = game.getRoundCount();
        if (shdList != null) {
            String sSpecialHex = "";
            boolean isHexAutoHit = localPlayer().getArtyAutoHitHexes().contains(coords);
            for (SpecialHexDisplay shd : shdList) {
                boolean isTypeAutoHit = shd.getType() == SpecialHexDisplay.Type.ARTILLERY_AUTOHIT;
                // Don't draw if this SHD is obscured from this player
                // The SHD list may also contain stale SHDs, so don't show
                // tooltips for SHDs that aren't drawn.
                // The exception is auto hits.  There will be an icon for auto
                // hits, so we need to draw a tooltip
                if (!shd.isObscured(localPlayer())
                        && (shd.drawNow(game.getPhase(), round, localPlayer())
                        || (isHexAutoHit && isTypeAutoHit))) {
                    if (shd.getType() == SpecialHexDisplay.Type.PLAYER_NOTE) {
                        if (localPlayer().equals(shd.getOwner())) {
                            sSpecialHex += "Note: ";
                        } else {
                            sSpecialHex += "Note (" + shd.getOwner().getName() + "): ";
                        }
                    }
                    String buf = shd.getInfo();
                    buf = buf.replaceAll("\\n", "<BR>");
                    sSpecialHex += buf;
                    sSpecialHex = guiScaledFontHTML(UIUtil.uiWhite()) + sSpecialHex + "</FONT>";
                    sSpecialHex += "<BR>";
                }
            }

            result += sSpecialHex;
        }

        StringBuffer txt = new StringBuffer();
        String div = "<DIV WIDTH=" + UIUtil.scaleForGUI(500) + ">" + result + "</DIV>";
        txt.append(UnitToolTip.wrapWithHTML(div));

        // Check to see if the tool tip is completely empty
        if (result.isEmpty()) {
            return "";
        }

        // Now that a valid tooltip text seems to be present,
        // (re)set the tooltip dismissal delay time to the preference
        // value so that the tooltip actually appears
        if (GUIP.getTooltipDismissDelay() >= 0) {
            ToolTipManager.sharedInstance().setDismissDelay(GUIP.getTooltipDismissDelay());
        } else {
            ToolTipManager.sharedInstance().setDismissDelay(bv.dismissDelay);
        }

        return txt.toString();
    }

    private List<ArtilleryAttackAction> getArtilleryAttacksAtLocation(Game game, Coords c) {
        List<ArtilleryAttackAction> v = new ArrayList<>();

        for (Enumeration<ArtilleryAttackAction> attacks = game.getArtilleryAttacks(); attacks.hasMoreElements(); ) {
            ArtilleryAttackAction a = attacks.nextElement();
            Targetable target = game.getTarget(a.getTargetType(), a.getTargetId());

            if ((target != null) && c.equals(target.getPosition())) {
                v.add(a);
            }
        }
        return v;
    }

    /**
     * Appends HTML describing the terrain of a given hex
     */
    public void appendTerrainTooltip(StringBuffer txt, @Nullable Hex mhex, GUIPreferences GUIP) {
        if (mhex == null) {
            return;
        }

        txt.append(HexTooltip.getTerrainTip(mhex, GUIP, game));
    }

    /**
     * Appends HTML describing the buildings and minefields in a given hex
     */
    public void appendBuildingsTooltip(StringBuffer txt, @Nullable Hex mhex) {
        if ((mhex != null) && (clientGui != null)) {
            String result = HexTooltip.getHexTip(mhex, clientGui.getClient(), GUIP);
            txt.append(result);
        }
    }

    /**
     * Appends HTML describing a given Entity aka Unit
     */
    public void appendEntityTooltip(StringBuffer txt, @Nullable Entity entity) {
        if (entity == null) {
            return;
        }

        String result = "";

        result += "<HR STYLE=WIDTH:90% />";
        // Table to add a bar to the left of an entity in
        // the player's color
        String color = GUIPreferences.hexColor(GUIP.getUnitToolTipFGColor());
        if (!EntityVisibilityUtils.onlyDetectedBySensors(localPlayer(), entity)) {
            color = entity.getOwner().getColour().getHexString();
        }
        String col1 = "<TD BGCOLOR=#" + color + " WIDTH=6></TD>";
        // Entity tooltip
        String col2 = "<TD>" + UnitToolTip.getEntityTipGame(entity, localPlayer()) + "</TD>";
        String row = "<TR>" + col1 + col2 + "</TR>";
        String table = "<TABLE WIDTH=100% BGCOLOR=" + GUIPreferences.hexColor(GUIP.getUnitToolTipBGColor()) + ">" + row + "</TABLE>";
        result += table;

        txt.append(result);
    }

    private Player localPlayer() {
        return (clientGui != null) ? clientGui.getClient().getLocalPlayer() : null;
    }

    /**
     * @return The weapon selected in the unit display if that weapon is an artillery
     * weapon and the unit itself is owned by the local player - null otherwise
     */
    @Nullable
    private Mounted getSelectedArtilleryWeapon() {
        if ((clientGui != null) && clientGui.hasSelectedWeapon()) {
            Entity selectedUnit = clientGui.getSelectedUnit();
            Mounted selectedWeapon = clientGui.getSelectedWeapon();

            // We don't want to display artillery auto-hit/adjusted fire hexes during
            // the artyautohithexes phase. These could be displayed if the player
            // uses the /reset command in some situations
            if ((selectedUnit != null)
                    && !game.getPhase().isSetArtilleryAutohitHexes()
                    && Objects.equals(localPlayer(), selectedUnit.getOwner())
                    && (selectedWeapon.getType() instanceof WeaponType)
                    && selectedWeapon.getType().hasFlag(WeaponType.F_ARTILLERY)) {
                return selectedWeapon;
            }
        }
        return null;
    }
}
