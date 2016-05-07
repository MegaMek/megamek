package megamek.client.ui.swing.boardview;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Transparency;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

import megamek.client.ui.Messages;
import megamek.client.ui.swing.GUIPreferences;
import megamek.client.ui.swing.util.PlayerColors;
import megamek.common.Aero;
import megamek.common.Compute;
import megamek.common.Coords;
import megamek.common.Entity;
import megamek.common.EntityMovementType;
import megamek.common.GunEmplacement;
import megamek.common.IGame;
import megamek.common.IGame.Phase;
import megamek.common.IBoard;
import megamek.common.IPlayer;
import megamek.common.Infantry;
import megamek.common.Mech;
import megamek.common.Mounted;
import megamek.common.Protomech;
import megamek.common.RangeType;
import megamek.common.Tank;
import megamek.common.TechConstants;
import megamek.common.WeaponType;
import megamek.common.options.OptionsConstants;
import megamek.common.options.PilotOptions;

/**
 * Sprite for an entity. Changes whenever the entity changes. Consists of an
 * image, drawn from the Tile Manager; facing and possibly secondary facing
 * arrows; armor and internal bars; and an identification label.
 */
class EntitySprite extends Sprite {

    // Statics
    private final static int SMALL = 0;
    private final static boolean DIRECT = true;
    private final static Color LABEL_TEXT_COLOR = Color.WHITE;
    private static final Color LABEL_CRITICAL_BACK = new Color(200,0,0,200);
    private static final Color LABEL_SPACE_BACK = new Color(0,0,200,200);
    private static final Color LABEL_GROUND_BACK = new Color(50,50,50,200);
    private static Color LABEL_BACK;
    enum Positioning { LEFT, RIGHT };
    
    // Individuals
    final Entity entity;

    private final Image radarBlipImage;
    private final int secondaryPos;

    private Rectangle entityRect;
    private Rectangle labelRect;
    private Font labelFont;
    private Point hexOrigin;
    private boolean criticalStatus;
    private Positioning labelPos;
    /** Used to color the label when this unit is selected for movement etc. */
    private boolean isSelected;
    
    // Keep track of ECM state, as it's too expensive to compute on the fly.
    private boolean isAffectedByECM = false;

    public EntitySprite(BoardView1 boardView1, final Entity entity,
            int secondaryPos, Image radarBlipImage) {
        super(boardView1);
        this.entity = entity;
        this.radarBlipImage = radarBlipImage;
        this.secondaryPos = secondaryPos;
        if (bv.game.getBoard().inSpace()) {
            LABEL_BACK = LABEL_SPACE_BACK;
        } else {
            LABEL_BACK = LABEL_GROUND_BACK;
        }
        getBounds();
    }
    
    private String getAdjShortName() {
        if (onlyDetectedBySensors()) {
            return Messages.getString("BoardView1.sensorReturn"); //$NON-NLS-1$
        } else {
            String name = entity.getShortName();
            int firstApo = name.indexOf('\'');
            int secondApo = name.indexOf('\'', name.indexOf('\'')+1);
            if ((firstApo >= 0) && (secondApo >= 0)) {
                name = name
                        .substring(firstApo+1, secondApo)
                        .toUpperCase();
            }
            return name;
        }
    }

    @Override
    public Rectangle getBounds() {
        // Start with the hex and add the label
        bounds = new Rectangle(0,0,bv.hex_size.width, bv.hex_size.height);
        updateLabel();
        bounds.add(labelRect);
        // Add space for 4 little status boxes
        if (labelPos == Positioning.RIGHT) {
            bounds.add(-4*(labelRect.height+2)+labelRect.x, labelRect.y);
        } else {
            bounds.add(4*(labelRect.height+2)+labelRect.x+labelRect.width, labelRect.y);
        }

        // Move to board position, save this origin for correct drawing
        hexOrigin = bounds.getLocation();
        Point ePos;
        if (secondaryPos == -1) {
            ePos = bv.getHexLocation(entity.getPosition());
        } else {
            ePos = bv.getHexLocation(entity.getSecondaryPositions().get(secondaryPos));
        }
        bounds.setLocation(hexOrigin.x + ePos.x, hexOrigin.y + ePos.y);
        
        entityRect = new Rectangle(bounds.x + (int) (20 * bv.scale), bounds.y
                + (int) (14 * bv.scale), (int) (44 * bv.scale),
                (int) (44 * bv.scale));

        return bounds;
    }

    private void updateLabel() {
        int face = (entity.isCommander() && !onlyDetectedBySensors()) ? 
                Font.ITALIC : Font.PLAIN;
        labelFont = new Font("SansSerif", face, (int)(10*Math.max(bv.scale,0.9))); //$NON-NLS-1$
        
        // Check the hexes in directions 2,5,1,4 if they are free of entities
        // and place the label in the direction of the first free hex
        // if none are free, the label will be centered in the current hex
        labelRect = new Rectangle(
                bv.getFontMetrics(labelFont).stringWidth(getAdjShortName())+4, 
                bv.getFontMetrics(labelFont).getAscent()+2);
        
        Coords position = entity.getPosition();
        if (bv.game.getEntitiesVector(position.translated("SE"), true).isEmpty()) {
            labelRect.setLocation((int)(bv.hex_size.width*0.55), (int)(0.75*bv.hex_size.height));
            labelPos = Positioning.RIGHT;
        } else if (bv.game.getEntitiesVector(position.translated("NW"), true).isEmpty()) {
            labelRect.setLocation((int)(bv.hex_size.width*0.45)-labelRect.width, 
                    (int)(0.25*bv.hex_size.height)-labelRect.height);
            labelPos = Positioning.LEFT;
        } else if (bv.game.getEntitiesVector(position.translated("NE"), true).isEmpty()) {
            labelRect.setLocation((int)(bv.hex_size.width*0.55), 
                    (int)(0.25*bv.hex_size.height)-labelRect.height);
            labelPos = Positioning.RIGHT;
        } else if (bv.game.getEntitiesVector(position.translated("SW"), true).isEmpty()) {
            labelRect.setLocation((int)(bv.hex_size.width*0.45)-labelRect.width, 
                    (int)(0.75*bv.hex_size.height));
            labelPos = Positioning.LEFT;
        } else {
            labelRect.setLocation(bv.hex_size.width/2-labelRect.width/2, 
                    (int)(0.75*bv.hex_size.height));
            labelPos = Positioning.RIGHT;
        } 

        // If multiple units are present in a hex, fan out the labels
        labelRect.y += (bv.getFontMetrics(labelFont).getAscent()+4) * 
                bv.game.getEntitiesVector(position).indexOf(entity);
    }

    // Happy little class to hold status info until it gets drawn
    private class Status {
        final Color color;
        final String status;
        final boolean small;

        Status(Color c, String s) {
            color = c;
            status = Messages.getString("BoardView1."+s);
            small = false;
            if (color.equals(Color.RED)) criticalStatus = true;
        }

        Status(Color c, String s, Object objs[]) {
            color = c;
            status = Messages.getString("BoardView1."+s, objs);
            small = false;
            if (color.equals(Color.RED)) criticalStatus = true;
        }

        Status(Color c, String s, boolean direct) {
            color = c;
            status = s;
            small = false;
            if (color.equals(Color.RED)) criticalStatus = true;
        }

        Status(Color c, String s, int t) {
            color = c;
            status = s;
            small = true;
        }
        
        Status(Color c, int b, int t) {
            color = c;
            status = null;
            small = true;
        }

    }
    
    private void drawStatusStrings(Graphics2D g, ArrayList<Status> statusStrings) {
        if (statusStrings.isEmpty()) return;
        
        // The small info blobs
        g.setFont(labelFont);
        
        Rectangle stR = new Rectangle(labelRect.x, labelRect.y, labelRect.height, labelRect.height);
        if (labelPos == Positioning.LEFT) {
            stR.translate(labelRect.width-labelRect.height, 0);
        }
        
        for (Status curStatus: statusStrings) {
            if (curStatus.small) { 
                if (labelPos == Positioning.RIGHT) {
                    stR.translate(-labelRect.height-2,0);
                } else {
                    stR.translate(labelRect.height+2,0);
                }
                g.setColor(LABEL_BACK);
                g.fillRoundRect(stR.x, stR.y, stR.width, stR.height, 5, 5);
                if (curStatus.status == null) {
                    Color damageColor = getDamageColor();
                    if (damageColor != null) {
                        g.setColor(damageColor);
                        g.fillRoundRect(stR.x+2, stR.y+2, stR.width-4, stR.height-4, 5, 5);
                    }

                } else {
                    bv.drawCenteredText(g, curStatus.status, 
                            stR.x+stR.height*0.5f-0.5f, stR.y+stR.height*0.5f-2, curStatus.color, false);
                }
            }
        }

        // When zoomed far out, status wouldn't be readable, therefore
        // draw a big "!" (and the label is red)
        if (bv.scale < 0.55 && criticalStatus) {
            Font bigFont = new Font("SansSerif",Font.BOLD,(int)(42*bv.scale));
            g.setFont(bigFont);
            Point pos = new Point(bv.hex_size.width/2, bv.hex_size.height/2);
            bv.drawTextShadow(g, "!", pos, bigFont);
            bv.drawCenteredText(g, "!", pos, Color.RED, false);
            return;
        }
        
        // Critical status text
        Font boldFont = new Font("SansSerif",Font.BOLD,(int)(12*bv.scale));
        g.setFont(boldFont);
        int y = (int)(bv.hex_size.height * 0.6);
        for (Status curStatus: statusStrings) {
            if (!curStatus.small) { // Critical status
                bv.drawTextShadow(g, curStatus.status, new Point(bv.hex_size.width/2,y), boldFont);
                bv.drawCenteredText(g, curStatus.status, bv.hex_size.width/2, y, curStatus.color, false);
                y -= 14*bv.scale;
            }
        }
    }
    
    /**
     * Creates the sprite for this entity. Fortunately it is no longer
     * an extra pain to create transparent images in AWT.
     */
    @Override
    public void prepare() {
        final IBoard board = bv.game.getBoard();
   
        // recalculate bounds & label
        getBounds();
        
        // create image for buffer
        GraphicsConfiguration config = GraphicsEnvironment
                .getLocalGraphicsEnvironment().getDefaultScreenDevice()
                .getDefaultConfiguration();
        image = config.createCompatibleImage(bounds.width, bounds.height,
                Transparency.TRANSLUCENT);
        Graphics2D graph = (Graphics2D)image.getGraphics();
        GUIPreferences.AntiAliasifSet(graph);
        
        // translate everything (=correction for label placement)
        graph.translate(-hexOrigin.x, -hexOrigin.y);

        if (!bv.useIsometric()) {
            // The entity sprite is drawn when the hexes are rendered.
            // So do not include the sprite info here.
            if (onlyDetectedBySensors()) {
                graph.drawImage(bv.getScaledImage(radarBlipImage, true), 0, 0, this);
            } else {
                // draw the unit icon translucent if:
                // hidden from the enemy (and activated graphics setting); or
                // submerged
                boolean translucentHiddenUnits = GUIPreferences.getInstance()
                        .getBoolean(GUIPreferences.ADVANCED_TRANSLUCENT_HIDDEN_UNITS);
                boolean shouldBeTranslucent = (trackThisEntitiesVisibilityInfo(entity)
                        && !entity.isVisibleToEnemy()) || entity.isHidden();
                if ((shouldBeTranslucent && translucentHiddenUnits)
                        || (entity.relHeight() < 0)) {
                    graph.setComposite(AlphaComposite.getInstance(
                            AlphaComposite.SRC_OVER, 0.5f));
                }
                graph.drawImage(bv.getScaledImage(bv.tileManager.imageFor(entity, secondaryPos), true),
                        0, 0, this);
                graph.setComposite(AlphaComposite.getInstance(
                        AlphaComposite.SRC_OVER, 1.0f));
            }
        }

        // scale the following draws according to board zoom
        graph.scale(bv.scale, bv.scale);
        
        boolean isInfantry = (entity instanceof Infantry);
        boolean isAero = (entity instanceof Aero);
        
        if ((isAero && ((Aero) entity).isSpheroid() && !board.inSpace())
                && (secondaryPos == 1)) {
            graph.setColor(Color.WHITE);
            graph.draw(bv.facingPolys[entity.getFacing()]);
        }

        if ((secondaryPos == -1) || (secondaryPos == 6)) {
            
            // Gather unit conditions
            ArrayList<Status> stStr = new ArrayList<Status>();
            criticalStatus = false;
            
            // Determine if the entity has a locked turret,
            // and if it is a gun emplacement
            boolean turretLocked = false;
            int crewStunned = 0;
            boolean ge = false;
            if (entity instanceof Tank) {
                turretLocked = !((Tank) entity).hasNoTurret()
                        && !entity.canChangeSecondaryFacing();
                crewStunned = ((Tank) entity).getStunnedTurns();
                ge = entity instanceof GunEmplacement;
            }
            
            // draw elevation/altitude if non-zero
            if (entity.isAirborne()) {
                if (!board.inSpace()) {
                    stStr.add(new Status(Color.CYAN, "A", SMALL));
                    stStr.add(new Status(Color.CYAN, Integer.toString(entity.getAltitude()), SMALL));
                }
            } else if (entity.getElevation() != 0) {
                stStr.add(new Status(Color.CYAN, Integer.toString(entity.getElevation()), SMALL));
            }
            
            // Shutdown
            if (entity.isManualShutdown()) {
                stStr.add(new Status(Color.YELLOW, "SHUTDOWN"));
            } else if (entity.isShutDown()) {
                stStr.add(new Status(Color.RED, "SHUTDOWN"));
            }
            
            // Prone, Hulldown, Stuck, Immobile, Jammed
            if (entity.isProne()) stStr.add(new Status(Color.RED, "PRONE"));
            if (entity.isHiddenActivating()) stStr.add(new Status(Color.RED, "ACTIVATING"));
            if (entity.isHidden()) stStr.add(new Status(Color.RED, "HIDDEN"));
            if (entity.isHullDown()) stStr.add(new Status(Color.ORANGE, "HULLDOWN"));
            if ((entity.isStuck())) stStr.add(new Status(Color.ORANGE, "STUCK"));
            if (!ge && entity.isImmobile()) stStr.add(new Status(Color.RED, "IMMOBILE"));
            if (isAffectedByECM()) stStr.add(new Status(Color.YELLOW, "Jammed"));
            
            // Turret Lock 
            if (turretLocked) stStr.add(new Status(Color.YELLOW, "LOCKED"));
            
            // Grappling & Swarming
            if (entity.getGrappled() != Entity.NONE) {
                if (entity.isGrappleAttacker()) {
                    stStr.add(new Status(Color.YELLOW, "GRAPPLER"));
                } else {
                    stStr.add(new Status(Color.RED, "GRAPPLED"));
                }
            }
            if (entity.getSwarmAttackerId() != Entity.NONE) {
                stStr.add(new Status(Color.RED, "SWARMED"));
            }

            // Transporting
            if ((entity.getLoadedUnits()).size() > 0) {
                stStr.add(new Status(Color.YELLOW, "T", SMALL));
            }

            // Hidden, Unseen Unit
            if (trackThisEntitiesVisibilityInfo(entity)) {
                if (!entity.isEverSeenByEnemy()) {
                    stStr.add(new Status(Color.GREEN, "U", SMALL));
                } else if (!entity.isVisibleToEnemy()) {
                    stStr.add(new Status(Color.GREEN, "H", SMALL));
                }
            }

            // Crew
            if (entity.getCrew().isDead()) stStr.add(new Status(Color.RED, "CrewDead"));
            if (crewStunned > 0)  {
                stStr.add(new Status(Color.YELLOW, "STUNNED", new Object[] { crewStunned }));
            }
            
            // Infantry
            if (isInfantry) {
                int dig = ((Infantry) entity).getDugIn();
                if (dig == Infantry.DUG_IN_COMPLETE) {
                    stStr.add(new Status(Color.PINK, "D", SMALL));
                } else if (dig != Infantry.DUG_IN_NONE) {
                    stStr.add(new Status(Color.YELLOW, "Working", DIRECT));
                    stStr.add(new Status(Color.PINK, "D", SMALL));
                } else if (((Infantry)entity).isTakingCover()) {
                    stStr.add(new Status(Color.YELLOW, "TakingCover"));
                }
            }
            
            // Aero
            if (isAero) {
                Aero a = (Aero) entity;
                if (a.isRolled()) stStr.add(new Status(Color.YELLOW, "ROLLED"));
                if (a.getFuel() <= 0) stStr.add(new Status(Color.RED, "FUEL"));
                if (a.isEvading()) stStr.add(new Status(Color.GREEN, "EVADE"));
                
                if (a.isOutControlTotal() & a.isRandomMove()) {
                    stStr.add(new Status(Color.RED, "RANDOM"));
                } else if (a.isOutControlTotal()) {
                    stStr.add(new Status(Color.RED, "CONTROL"));
                }
            }
            
            if (GUIPreferences.getInstance().getShowDamageLevel()) {
                Color damageColor = getDamageColor();
                if (damageColor != null) {
                    stStr.add(new Status(damageColor, 0, SMALL));
                }
            }

            
            // Unit Label
            // no scaling for the label, its size is changed by varying
            // the font size directly => better control
            graph.scale(1/bv.scale, 1/bv.scale);
            
            // Label background
            if (criticalStatus) {
                graph.setColor(LABEL_CRITICAL_BACK);
            } else {
                graph.setColor(LABEL_BACK);
            }
            graph.fillRoundRect(labelRect.x, labelRect.y, 
                    labelRect.width, labelRect.height, 5, 10);
            
            // Label text
            graph.setFont(labelFont);
            Color textColor = LABEL_TEXT_COLOR;
            if (!entity.isDone() && !onlyDetectedBySensors()) {
                textColor = GUIPreferences.getInstance().getColor(
                        GUIPreferences.ADVANCED_UNITOVERVIEW_VALID_COLOR);
            }
            if (isSelected) {
                textColor = GUIPreferences.getInstance().getColor(
                        GUIPreferences.ADVANCED_UNITOVERVIEW_SELECTED_COLOR);
            }
            bv.drawCenteredText(graph, getAdjShortName(), labelRect.x+labelRect.width/2,
                    labelRect.y+labelRect.height/2-1, textColor, (entity.isDone() && !onlyDetectedBySensors()));

            // Past here, everything is drawing status that shouldn't be seen
            // on a sensor return, so we'll just quit here
            if (onlyDetectedBySensors()) {
                graph.dispose();
                return;
            }
            
            // Draw all the status information now
            drawStatusStrings(graph, stStr);
            
            // from here, scale the following draws according to board zoom
            graph.scale(bv.scale, bv.scale);
            
            // draw facing
            graph.setColor(Color.white);
            if ((entity.getFacing() != -1)
                    && !(isInfantry && !((Infantry) entity).hasFieldGun()
                            && !((Infantry) entity).isTakingCover())
                    && !(isAero && ((Aero) entity).isSpheroid() && !board
                            .inSpace())) {
                graph.draw(bv.facingPolys[entity.getFacing()]);
            }

            // determine secondary facing for non-mechs & flipped arms
            int secFacing = entity.getFacing();
            if (!((entity instanceof Mech) || (entity instanceof Protomech))) {
                secFacing = entity.getSecondaryFacing();
            } else if (entity.getArmsFlipped()) {
                secFacing = (entity.getFacing() + 3) % 6;
            }
            // draw red secondary facing arrow if necessary
            if ((secFacing != -1) && (secFacing != entity.getFacing())) {
                graph.setColor(Color.red);
                graph.draw(bv.facingPolys[secFacing]);
            }
            if ((entity instanceof Aero) && this.bv.game.useVectorMove()) {
                for (int head : entity.getHeading()) {
                    graph.setColor(Color.red);
                    graph.draw(bv.facingPolys[head]);
                }
            }

            // armor and internal status bars
            int baseBarLength = 23;
            int barLength = 0;
            double percentRemaining = 0.00;

            percentRemaining = entity.getArmorRemainingPercent();
            barLength = (int) (baseBarLength * percentRemaining);

            graph.setColor(Color.darkGray);
            graph.fillRect(56, 7, 23, 3);
            graph.setColor(Color.lightGray);
            graph.fillRect(55, 6, 23, 3);
            graph.setColor(getStatusBarColor(percentRemaining));
            graph.fillRect(55, 6, barLength, 3);

            if (!ge) {
                // Gun emplacements don't have internal structure
                percentRemaining = entity.getInternalRemainingPercent();
                barLength = (int) (baseBarLength * percentRemaining);

                graph.setColor(Color.darkGray);
                graph.fillRect(56, 11, 23, 3);
                graph.setColor(Color.lightGray);
                graph.fillRect(55, 10, 23, 3);
                graph.setColor(getStatusBarColor(percentRemaining));
                graph.fillRect(55, 10, barLength, 3);
            }
        }

        graph.dispose();
    }

    private Color getDamageColor() {
        switch (entity.getDamageLevel()) {
            case Entity.DMG_CRIPPLED:
                return Color.black;
            case Entity.DMG_HEAVY:
                return Color.red;
            case Entity.DMG_MODERATE:
                return Color.yellow;
            case Entity.DMG_LIGHT:
                return Color.green;
        }
        return null;
    }

    /**
     * We only want to show double-blind visibility indicators on our own
     * mechs and teammates mechs (assuming team vision option).
     */
    private boolean trackThisEntitiesVisibilityInfo(Entity e) {
        IPlayer localPlayer = bv.getLocalPlayer();
        if (localPlayer == null) {
            return false;
        }
        if (bv.game.getOptions().booleanOption("double_blind") //$NON-NLS-1$
                && ((e.getOwner().getId() == localPlayer.getId()) || (bv.game
                        .getOptions().booleanOption("team_vision") //$NON-NLS-1$
                && (e.getOwner().getTeam() == localPlayer.getTeam())))) {
            return true;
        }
        return false;
    }

    /**
     * Used to determine if this EntitySprite is only detected by an enemies
     * sensors and hence should only be a sensor return.
     *
     * @return
     */
    private boolean onlyDetectedBySensors() {
        boolean sensors = bv.game.getOptions().booleanOption(
                "tacops_sensors");
        boolean sensorsDetectAll = bv.game.getOptions().booleanOption(
                "sensors_detect_all");
        boolean doubleBlind = bv.game.getOptions().booleanOption(
                "double_blind");
        boolean hasVisual = entity.hasSeenEntity(bv.getLocalPlayer());
        boolean hasDetected = entity.hasDetectedEntity(bv.getLocalPlayer());

        if (sensors && doubleBlind && !sensorsDetectAll
                && !trackThisEntitiesVisibilityInfo(entity)
                && hasDetected && !hasVisual) {
            return true;
        } else {
            return false;
        }
    }

    private Color getStatusBarColor(double percentRemaining) {
        if (percentRemaining <= .25) {
            return Color.red;
        } else if (percentRemaining <= .75) {
            return Color.yellow;
        } else {
            return new Color(16, 196, 16);
        }
    }

    /**
     * Overrides to provide for a smaller sensitive area.
     */
    @Override
    public boolean isInside(Point point) {
        return entityRect.contains(point.x, point.y);
    }
    
    public Coords getPosition() {
        return entity.getPosition();
    }
    
    private StringBuffer tooltipString;
    private final boolean BR = true;
    private final boolean NOBR = false;
    
    /**
     * Adds a resource string to the entity tooltip
     * 
     * @param ttSName The resource string name. "BoardView1.Tooltip." will be added in front, so
     * "Pilot" will retrieve BoardView1.Tooltip.Pilot
     * @param startBR = true will start the string with a &lt;BR&gt;; The constants BR and NOBR can be used here. 
     * @param ttO a list of Objects to insert into the {x} places in the resource.
     */
    private void addToTT(String ttSName, boolean startBR, Object... ttO) {
        if (startBR == BR)
            tooltipString.append("<BR>");
        if (ttO != null) {
            tooltipString.append(Messages.getString("BoardView1.Tooltip."
                    + ttSName, ttO));
        } else {
            tooltipString.append(Messages.getString("BoardView1.Tooltip."
                    + ttSName));
        }
    }
    
    /**
     * Adds a resource string to the entity tooltip
     * 
     * @param ttSName The resource string name. "BoardView1.Tooltip." will be added in front, so
     * "Pilot" will retrieve BoardView1.Tooltip.Pilot
     * @param startBR = true will start the string with a &lt;BR&gt;; The constants BR and NOBR can be used here. 
     */
    private void addToTT(String ttSName, boolean startBR) {
        addToTT(ttSName, startBR, (Object[]) null);
    }
    
    @Override
    public StringBuffer getTooltip() {
        
        // Tooltip info for a sensor blip
        if (onlyDetectedBySensors())
            return new StringBuffer(Messages.getString("BoardView1.sensorReturn"));

        // No sensor blip...
        Infantry thisInfantry = null;
        if (entity instanceof Infantry) thisInfantry = (Infantry) entity;
        GunEmplacement thisGunEmp = null;
        if (entity instanceof GunEmplacement) thisGunEmp = (GunEmplacement) entity;
        Aero thisAero = null;
        if (entity instanceof Aero) thisAero = (Aero) entity;
        
        tooltipString = new StringBuffer();

        // Unit Chassis and Player
        addToTT("Unit", NOBR,
                Integer.toHexString(PlayerColors.getColorRGB(
                        entity.getOwner().getColorIndex())), 
                entity.getChassis(), 
                entity.getOwner().getName());
        
        // Pilot Info
        // Nickname > Name > "Pilot"
        String pnameStr = "Pilot";

        if ((entity.getCrew().getName() != null)
                && !entity.getCrew().getName().equals("")) 
            pnameStr = entity.getCrew().getName();
        
        if ((entity.getCrew().getNickname() != null)
                && !entity.getCrew().getNickname().equals("")) 
            pnameStr = "'" + entity.getCrew().getNickname() + "'";
        
        addToTT("Pilot", BR,
                pnameStr, 
                entity.getCrew().getGunnery(), 
                entity.getCrew().getPiloting());

        // Pilot Status
        if (!entity.getCrew().getStatusDesc().equals(""))
            addToTT("PilotStatus", NOBR, 
                    entity.getCrew().getStatusDesc());
        
        // Pilot Advantages
        int numAdv = entity.getCrew().countOptions(
                PilotOptions.LVL3_ADVANTAGES);
        if (numAdv == 1)
            addToTT("Adv1", NOBR, numAdv);
        else if (numAdv > 1) 
            addToTT("Advs", NOBR, numAdv);
        
        // Pilot Manei Domini
        if ((entity.getCrew().countOptions(
                PilotOptions.MD_ADVANTAGES) > 0)) 
            addToTT("MD", NOBR);
        
        // Unit movement ability
        if (thisGunEmp == null) {
            addToTT("Movement", BR, entity.getWalkMP(), entity.getRunMPasString());
            if (entity.getJumpMP() > 0) tooltipString.append("/" + entity.getJumpMP());
        }
        
        // Armor and Internals
        addToTT("ArmorInternals", BR, entity.getTotalArmor(),
                entity.getTotalInternal());

        // Heat, not shown for units with 999 heat sinks (vehicles)
        if (entity.getHeatCapacity() != 999) {
            if (entity.heat == 0) 
                addToTT("Heat0", BR);
            else 
                addToTT("Heat", BR, entity.heat);
        }

        // Actual Movement
        if (thisGunEmp == null) {
            // In the Movement Phase, unit not done
            if (!entity.isDone() && this.bv.game.getPhase() == Phase.PHASE_MOVEMENT) {
                // "Has not yet moved" only during movement phase
                addToTT("NotYetMoved", BR);
                
            // In the Movement Phase, unit is done - or in the Firing Phase
            } else if (
                    (entity.isDone() && this.bv.game.getPhase() == Phase.PHASE_MOVEMENT) 
                    || this.bv.game.getPhase() == Phase.PHASE_FIRING) {
                int tmm = Compute.getTargetMovementModifier(bv.game,
                        entity.getId()).getValue();
                // Unit didn't move
                if (entity.moved == EntityMovementType.MOVE_NONE) {
                    addToTT("NoMove", BR, tmm);
                    
                // Unit did move
                } else {
                    // Colored arrow
                    // get the color resource
                    String guipName = "AdvancedMoveDefaultColor";
                    if ((entity.moved == EntityMovementType.MOVE_RUN)
                            || (entity.moved == EntityMovementType.MOVE_VTOL_RUN)
                            || (entity.moved == EntityMovementType.MOVE_OVER_THRUST)) 
                        guipName = "AdvancedMoveRunColor";
                    else if (entity.moved == EntityMovementType.MOVE_SPRINT) 
                        guipName = "AdvancedMoveSprintColor";
                    else if (entity.moved == EntityMovementType.MOVE_JUMP) 
                        guipName = "AdvancedMoveJumpColor";

                    // HTML color String from Preferences
                    String moveTypeColor = Integer
                            .toHexString(GUIPreferences.getInstance()
                                    .getColor(guipName).getRGB() & 0xFFFFFF);

                    // Arrow
                    addToTT("Arrow", BR, moveTypeColor);

                    // Actual movement and modifier
                    addToTT("MovementF", NOBR,
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
        
        // If DB, add information about who sees this Entity
        if (bv.game.getOptions().booleanOption("double_blind")) {
            StringBuffer playerList = new StringBuffer();
            boolean teamVision = bv.game.getOptions().booleanOption(
                    "team_vision");
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
        if (bv.game.getOptions().booleanOption("tacops_sensors")) {
            addToTT("Sensors", BR, entity.getSensorDesc());
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
                if (entity instanceof Aero) {
                    ranges = wtype.getATRanges();
                } else {
                    ranges = wtype.getRanges(curWp);
                }
                String rangeString = "(";
                if ((ranges[RangeType.RANGE_MINIMUM] != WeaponType.WEAPON_NA) 
                        && (ranges[RangeType.RANGE_MINIMUM] != 0)) {
                    rangeString += ranges[RangeType.RANGE_MINIMUM] + "/";
                } else {
                    rangeString += "-/";
                }
                int maxRange = RangeType.RANGE_LONG;
                if (bv.game.getOptions().booleanOption(
                        OptionsConstants.AC_TAC_OPS_RANGE)) {
                    maxRange = RangeType.RANGE_EXTREME;
                }
                for (int i = RangeType.RANGE_SHORT; i <= maxRange; i++) {
                    rangeString += ranges[i];
                    if (i != maxRange) {
                        rangeString += "/";
                    }
                }
                
                weapDesc += rangeString + ")";
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
            tooltipString.append("<FONT SIZE=\"-2\">");

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
                tooltipString.append("<FONT COLOR=#8080FF>");
                // but: color gray and strikethrough when weapon destroyed
                if (wpDest) tooltipString.append("<FONT COLOR=#a0a0a0><S>");

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
                if (wpDest) tooltipString.append("</S>");
                tooltipString.append("</FONT>"); 
            }
            tooltipString.append("</FONT>");
        }
        return tooltipString;
    }
    
    public String getPlayerColor() {
        if (onlyDetectedBySensors()) {
            return "C0C0C0";
        } else {
            return Integer.toHexString(PlayerColors.getColorRGB(entity
                    .getOwner().getColorIndex()));
        }
    }
    
    public boolean isAffectedByECM() {
        return isAffectedByECM;
    }

    public void setAffectedByECM(boolean isAffectedByECM) {
        boolean changed = isAffectedByECM != this.isAffectedByECM;
        this.isAffectedByECM = isAffectedByECM;
        // We need to prepare the icon again if the value changed
        if (changed) {
            prepare();
        }
    }
    
    /** Marks the entity as selected for movement etc., recoloring the label */
    public void setSelected(boolean status) {
        if (isSelected != status) {
            isSelected = status;
            prepare();
        }
    }
    
    /** Returns if the entity is marked as selected for movement etc., recoloring the label */
    public boolean getSelected() {
        return isSelected;
    }
}

