package megamek.client.ui.swing.boardview;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Transparency;
import java.awt.image.ImageObserver;
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
import megamek.common.EntityMovementMode;
import megamek.common.EntityMovementType;
import megamek.common.GunEmplacement;
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

    Entity entity;

    private Image radarBlipImage;
    private int secondaryPos;

    private Rectangle entityRect;
    private Rectangle labelRect;

    // Keep track of ECM state, as it's too expensive to compute on the fly.
    private boolean isAffectedByECM = false;

    public EntitySprite(BoardView1 boardView1, final Entity entity,
            int secondaryPos, Image radarBlipImage) {
        super(boardView1);
        this.entity = entity;
        this.radarBlipImage = radarBlipImage;
        this.secondaryPos = secondaryPos;
        image = null;

        updateLabelRect();
        getBounds();
    }
    
    private String getAdjShortName() {
        if (onlyDetectedBySensors()) {
            return Messages.getString("BoardView1.sensorReturn"); //$NON-NLS-1$
        }
        String shortName = entity.getShortName();

        if (entity.getMovementMode() == EntityMovementMode.VTOL) {
            shortName += " (FL: " + //$NON-NLS-1$
                    Integer.toString(entity.getElevation()) + ")";
        }
        if (entity.getMovementMode() == EntityMovementMode.SUBMARINE) {
            shortName += " (Depth: " +
                    Integer.toString(entity.getElevation()) + ")";
        }
        return shortName;
    }

    @Override
    public Rectangle getBounds() {
        // Start with the hex itself
        Rectangle tempBounds = new Rectangle(0,0,bv.hex_size.width, bv.hex_size.height);

        // Add space for the label
        tempBounds.add(labelRect);
        
        // Move to board position
        if (secondaryPos == -1) {
            tempBounds.setLocation(bv.getHexLocation(entity.getPosition()));
        } else {
            tempBounds.setLocation(bv.getHexLocation(entity
                    .getSecondaryPositions().get(secondaryPos)));
        }
        
        // add space if the unit is elevated and the sprite displaced upwards
        if (bv.useIsometric()) {
            int altAdjust = 0;
            if (entity.isAirborne() || entity.isAirborneVTOLorWIGE()) {
                altAdjust = (int) (bv.DROPSHDW_DIST * bv.scale);
            } else if ((entity.getElevation() != 0)
                    && !(entity instanceof GunEmplacement)) {
                altAdjust = (int) (entity.getElevation() * BoardView1.HEX_ELEV * bv.scale);
            }
            tempBounds.add(tempBounds.x, tempBounds.y - altAdjust);
        }

        bounds = tempBounds;

        entityRect = new Rectangle(bounds.x + (int) (20 * bv.scale), bounds.y
                + (int) (14 * bv.scale), (int) (44 * bv.scale),
                (int) (44 * bv.scale));

        return bounds;
    }

    private void updateLabelRect() {
        int face = (entity.isCommander() && !onlyDetectedBySensors()) ? 
                Font.ITALIC : Font.PLAIN;
        Font font = new Font("SansSerif", face, (int)(10*Math.max(bv.scale,0.9))); //$NON-NLS-1$
        labelRect = new Rectangle((int)(0.55*bv.hex_size.width), (int)(0.75*bv.hex_size.height), 
                bv.getFontMetrics(font).stringWidth(getAdjShortName()) + 1, 
                bv.getFontMetrics(font).getAscent());
    }
    
    @Override
    public void drawOnto(Graphics g, int x, int y, ImageObserver observer) {
        boolean translucentHiddenUnits = GUIPreferences.getInstance()
                .getBoolean(GUIPreferences.ADVANCED_TRANSLUCENT_HIDDEN_UNITS);

        if ((trackThisEntitiesVisibilityInfo(entity)
                && !entity.isVisibleToEnemy() && translucentHiddenUnits)
                || (entity.relHeight() < 0)) {
            // create final image with translucency
            drawOnto(g, x, y, observer, true);
        } else {
            drawOnto(g, x, y, observer, false);
        }
    }

    private void drawStatus(Graphics2D g, int x, int y, Color color, String status) {
        drawStatus(g, x, y, color, status, null);
    }
    
    private void drawStatus(Graphics2D g, int x, int y, Color color, String status, Object[] object) {
        String fullString;
        if (object != null) { 
            fullString = Messages.getString("BoardView1."+status, object);
        } else {
            fullString = Messages.getString("BoardView1."+status);
        }
        drawStatusString(g, x, y, color, fullString);
    }
    
    private void drawStatusString(Graphics2D g, int x, int y, Color color, String fullString) {
        Font font = g.getFont();
        Rectangle tR = new Rectangle(x, y, 
                bv.getFontMetrics(font).stringWidth(fullString) + 1, 
                bv.getFontMetrics(font).getAscent());
        g.setColor(new Color(50,50,50,150));
        g.fillRect(tR.x, tR.y-tR.height, tR.width, tR.height);
        g.setColor(color);
        g.drawString(fullString, x, y); //$NON-NLS-1$
    }
    
    /**
     * Creates the sprite for this entity. Fortunately it is no longer
     * an extra pain to create transparent images in AWT.
     */
    @Override
    public void prepare() {
        final IBoard board = bv.game.getBoard();
        
        updateLabelRect();
        getBounds();
        
        // create image for buffer
        GraphicsConfiguration config = GraphicsEnvironment
                .getLocalGraphicsEnvironment().getDefaultScreenDevice()
                .getDefaultConfiguration();
        Image tempImage = config.createCompatibleImage(bounds.width, bounds.height,
                Transparency.TRANSLUCENT);
        Graphics2D graph = (Graphics2D)tempImage.getGraphics();
        GUIPreferences.AntiAliasifSet(graph);

        graph.setColor(Color.RED);
        graph.drawRect(1, 1, bounds.width-2, bounds.height-2);
        
        if (!bv.useIsometric()) {
            // The entity sprite is drawn when the hexes are rendered.
            // So do not include the sprite info here.
            if (onlyDetectedBySensors()) {
                graph.drawImage(bv.getScaledImage(radarBlipImage, true), 0, 0, this);
            } else {
                graph.drawImage(bv.getScaledImage(bv.tileManager.imageFor(entity, secondaryPos), true),
                        0, 0, this);
            }
        }

        // scale the following draws according to board zoom
        graph.scale(bv.scale, bv.scale);
        
        boolean isInfantry = (entity instanceof Infantry);
        boolean isAero = (entity instanceof Aero);
        
        if ((isAero && ((Aero) entity).isSpheroid() && !board.inSpace())
                && (secondaryPos == 1)) {
            graph.setColor(Color.white);
            graph.draw(bv.facingPolys[entity.getFacing()]);
        }

        if ((secondaryPos == -1) || (secondaryPos == 6)) {
            // no scaling for the label
            graph.scale(1/bv.scale, 1/bv.scale);
            // draw box with shortName
            Color text, bkgd, bord;
            if (entity.isDone() && !onlyDetectedBySensors()) {
                text = Color.lightGray;
                bkgd = Color.darkGray;
                bord = Color.black;
            } else if (entity.isImmobile() && !onlyDetectedBySensors()) {
                text = Color.darkGray;
                bkgd = Color.black;
                bord = Color.lightGray;
            } else {
                text = Color.black;
                bkgd = Color.lightGray;
                bord = Color.darkGray;
            }
            int face = (entity.isCommander() && !onlyDetectedBySensors()) ? 
                    Font.ITALIC : Font.PLAIN;
            Font font = new Font("SansSerif", face, (int)(10*Math.max(bv.scale,0.9))); //$NON-NLS-1$
            graph.setFont(font);
            graph.setColor(bord);
            graph.fillRect(labelRect.x, labelRect.y, labelRect.width,
                    labelRect.height);
            graph.setColor(bkgd);
            graph.fillRect(labelRect.x-1, labelRect.y-1, labelRect.width,
                    labelRect.height);
            graph.setColor(text);
            graph.drawString(getAdjShortName(), labelRect.x,
                    labelRect.y + labelRect.height-2);
           
            // Past here, everything is drawing status that shouldn't be seen
            // on a sensor return, so we'll just quit here
            if (onlyDetectedBySensors()) {
                image = tempImage;
                graph.dispose();
                tempImage.flush();
                return;
            }
            
            // scale the following draws according to board zoom
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

            // Determine if the entity has a locked turret,
            // and if it is a gun emplacement
            boolean turretLocked = false;
            // boolean turretJammed = false;
            int crewStunned = 0;
            boolean ge = false;
            if (entity instanceof Tank) {
                turretLocked = !((Tank) entity).hasNoTurret()
                        && !entity.canChangeSecondaryFacing();
                crewStunned = ((Tank) entity).getStunnedTurns();
                ge = entity instanceof GunEmplacement;
            }

            // draw condition strings

            // draw elevation/altitude if non-zero
            if (entity.isAirborne()) {
                if (!board.inSpace()) {
                    drawStatusString(graph, 25, 14, Color.PINK, Integer.toString(entity.getAltitude())+"A");
                }
            } else if (entity.getElevation() != 0) {
                drawStatusString(graph, 25, 14, Color.PINK, Integer.toString(entity.getElevation()));
            }

            if (entity instanceof Aero) {
                Aero a = (Aero) entity;

                if (a.isRolled()) {
                    drawStatus(graph,17,14,Color.RED,"ROLLED");
                }

                if (a.isOutControlTotal() & a.isRandomMove()) {
                    drawStatus(graph,17,34,Color.RED,"RANDOM");
                } else if (a.isOutControlTotal()) {
                    drawStatus(graph,17,38,Color.RED,"CONTROL");
                }
                if (a.getFuel() <= 0) {
                    drawStatus(graph,17,38,Color.RED,"FUEL");
                }

                if (a.isEvading()) {
                    drawStatus(graph,17,38,Color.RED,"EVADE");
                }
            }

            if (entity.getCrew().isDead()) {
                drawStatus(graph,17,38,Color.RED,"CrewDead");
            } else if (!ge && entity.isImmobile()) {
                if (entity.isProne()) {
                    drawStatus(graph,17,34,Color.RED,"IMMOBILE");
                    drawStatus(graph,25,47,Color.YELLOW,"PRONE");
                } else if (crewStunned > 0) {
                    drawStatus(graph,17,34,Color.RED,"IMMOBILE");
                    drawStatus(graph,21,47,Color.YELLOW,"STUNNED",new Object[] { crewStunned });
                } else if (turretLocked) {
                    drawStatus(graph,17,34,Color.RED,"IMMOBILE");
                    drawStatus(graph,21,47,Color.YELLOW,"LOCKED");
                } else {
                    drawStatus(graph,17,38,Color.RED,"IMMOBILE");
                }
            } else if (entity.isProne()) {
                drawStatus(graph,25,38,Color.YELLOW,"PRONE");
            } else if (crewStunned > 0) {
                drawStatus(graph,21,47,Color.YELLOW,"STUNNED",new Object[] { crewStunned });
            } else if (turretLocked) {
                drawStatus(graph,21,38,Color.YELLOW,"LOCKED");
            } else if ((entity.getGrappled() != Entity.NONE) 
                    && entity.isGrappleAttacker()) {
                drawStatus(graph,21,38,Color.RED,"GRAPPLER");
            } else if ((entity.getGrappled() != Entity.NONE) ) {
                drawStatus(graph,21,38,Color.RED,"GRAPPLED");
            }

            // If this unit is shutdown, say so.
            if (entity.isManualShutdown()) {
                drawStatus(graph,49,70,Color.YELLOW,"SHUTDOWN");
            } else if (entity.isShutDown()) {
                drawStatus(graph,49,70,Color.RED,"SHUTDOWN");
            }

            // If this unit is being swarmed or is swarming another, say so.
            if (entity.getSwarmAttackerId() != Entity.NONE) {
                drawStatus(graph,16,21,Color.RED,"SWARMED");
            }

            // If this unit is transporting another, say so.
            if ((entity.getLoadedUnits()).size() > 0) {
                drawStatusString(graph, 19, 70, Color.BLACK, "T");
            }

            // If this unit is stuck, say so.
            if ((entity.isStuck())) {
                drawStatus(graph,25,60,Color.ORANGE,"STUCK");
            }

            // If this unit is currently unknown to the enemy, say so.
            if (trackThisEntitiesVisibilityInfo(entity)) {
                if (!entity.isEverSeenByEnemy()) {
                    drawStatusString(graph, 29, 70, Color.BLACK, "U");
                } else if (!entity.isVisibleToEnemy()
                        && !GUIPreferences.getInstance().getBoolean(
                                        GUIPreferences.ADVANCED_TRANSLUCENT_HIDDEN_UNITS)) {
                    // If this unit is currently hidden from the enemy, say so
                    drawStatusString(graph, 29, 70, Color.BLACK, "H");
                }
            }

            // If hull down, show
            if (entity.isHullDown()) {
                drawStatusString(graph, 14, 38, Color.YELLOW, Messages.getString("UnitOverview.HULLDOWN"));
            } else if (entity instanceof Infantry) {
                int dig = ((Infantry) entity).getDugIn();
                if (dig == Infantry.DUG_IN_COMPLETE) {
                    drawStatusString(graph, 26, 70, Color.RED, "D");
                } else if (dig != Infantry.DUG_IN_NONE) {
                    drawStatusString(graph, 22, 70, Color.RED, "Working");
                } else if (((Infantry)entity).isTakingCover()) {
                    drawStatus(graph, 22, 70, Color.RED, "TakingCover");
                }
            }
            
            // Notify ECM effects
            if (isAffectedByECM()) {
                drawStatus(graph,21,50,Color.RED,"Jammed"); //$NON-NLS-1$
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

            if (GUIPreferences.getInstance().getShowDamageLevel()) {
                Color damageColor = getDamageColor();
                if (damageColor != null) {
                    graph.setColor(damageColor);
                    graph.fillOval(20, 15, 12, 12);
                }
            }
        }

//        image = bv.getScaledImage(tempImage, false);
        image = tempImage;

        graph.dispose();
        tempImage.flush();
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
                
                // Unit didn't move
                if (entity.moved == EntityMovementType.MOVE_NONE) {
                    addToTT("NoMove", BR);
                    
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
                            Compute.getTargetMovementModifier(this.bv.game,entity.getId()).getValue());
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
}

