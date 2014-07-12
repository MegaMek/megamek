package megamek.client.ui.swing.boardview;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.image.FilteredImageSource;

import megamek.client.ui.Messages;
import megamek.client.ui.swing.GUIPreferences;
import megamek.client.ui.swing.util.KeyAlphaFilter;
import megamek.common.Aero;
import megamek.common.EntityMovementMode;
import megamek.common.EntityMovementType;
import megamek.common.MiscType;
import megamek.common.MoveStep;
import megamek.common.Tank;
import megamek.common.MovePath.MoveStepType;

/**
 * Sprite for a step in a movement path. Only one sprite should exist for
 * any hex in a path. Contains a colored number, and arrows indicating
 * entering, exiting or turning.
 */
class StepSprite extends Sprite {

    private MoveStep step;
    private Image baseScaleImage;

    public StepSprite(BoardView1 boardView1, final MoveStep step) {
        super(boardView1);
        this.step = step;

        // step is the size of the hex that this step is in
        bounds = new Rectangle(this.boardView1.getHexLocation(step.getPosition()), this.boardView1.hex_size);
        image = null;
        baseScaleImage = null;
    }

    /**
     * Refreshes this StepSprite's image to handle changes in the zoom
     * level.
     */
    public void refreshZoomLevel() {

        if (baseScaleImage == null) {
            return;
        }

        if (this.boardView1.zoomIndex == BoardView1.BASE_ZOOM_INDEX) {
            image = this.boardView1.createImage(new FilteredImageSource(
                    baseScaleImage.getSource(), new KeyAlphaFilter(
                            BoardView1.TRANSPARENT)));
        } else {
            image = this.boardView1.getScaledImage(this.boardView1.createImage(new FilteredImageSource(
                    baseScaleImage.getSource(), new KeyAlphaFilter(
                            BoardView1.TRANSPARENT))),false);
        }
    }

    @Override
    public void prepare() {
        // create image for buffer
        Image tempImage = this.boardView1.createImage(bounds.width, bounds.height);
        Graphics graph = tempImage.getGraphics();

        // fill with key color
        graph.setColor(new Color(BoardView1.TRANSPARENT));
        graph.fillRect(0, 0, bounds.width, bounds.height);

        // setup some variables
        final Point stepPos = this.boardView1.getHexLocation(step.getPosition());
        stepPos.translate(-bounds.x, -bounds.y);
        final Polygon facingPoly = this.boardView1.facingPolys[step.getFacing()];
        final Polygon movePoly = this.boardView1.movementPolys[step.getFacing()];
        Point offsetCostPos;
        Polygon myPoly;
        Color col;
        // set color
        switch (step.getMovementType()) {
            case MOVE_RUN:
            case MOVE_VTOL_RUN:
            case MOVE_OVER_THRUST:
                if (step.isUsingMASC()) {
                    col = GUIPreferences.getInstance().getColor(
                            "AdvancedMoveMASCColor");
                } else {
                    col = GUIPreferences.getInstance().getColor(
                            "AdvancedMoveRunColor");
                }
                break;
            case MOVE_JUMP:
                col = GUIPreferences.getInstance().getColor(
                        "AdvancedMoveJumpColor");
                break;
            case MOVE_SPRINT:
                col = GUIPreferences.getInstance().getColor(
                        "AdvancedMoveSprintColor");
                break;
            case MOVE_ILLEGAL:
                col = GUIPreferences.getInstance().getColor(
                        "AdvancedMoveIllegalColor");
                break;
            default:
                if (step.getType() == MoveStepType.BACKWARDS) {
                    col = GUIPreferences.getInstance().getColor(
                            "AdvancedMoveBackColor");
                } else {
                    col = GUIPreferences.getInstance().getColor(
                            "AdvancedMoveDefaultColor");
                }
                break;
        }

        if (this.boardView1.game.useVectorMove()) {
            drawActiveVectors(step, stepPos, graph);
        }

        drawConditions(step, stepPos, graph, col);

        // draw arrows and cost for the step
        switch (step.getType()) {
            case FORWARDS:
            case SWIM:
            case BACKWARDS:
            case CHARGE:
            case DFA:
            case LATERAL_LEFT:
            case LATERAL_RIGHT:
            case LATERAL_LEFT_BACKWARDS:
            case LATERAL_RIGHT_BACKWARDS:
            case DEC:
            case DECN:
            case ACC:
            case ACCN:
            case LOOP:
                // draw arrows showing them entering the next
                myPoly = new Polygon(movePoly.xpoints, movePoly.ypoints,
                        movePoly.npoints);
                graph.setColor(Color.darkGray);
                myPoly.translate(stepPos.x + 1, stepPos.y + 1);
                graph.drawPolygon(myPoly);
                graph.setColor(col);
                myPoly.translate(-1, -1);
                graph.drawPolygon(myPoly);
                // draw movement cost
                drawMovementCost(step, stepPos, graph, col, true);
                drawRemainingVelocity(step, stepPos, graph, true);
                break;
            case GO_PRONE:
            case HULL_DOWN:
            case DOWN:
            case DIG_IN:
            case FORTIFY:
                // draw arrow indicating dropping prone
                // also doubles as the descent indication
                Polygon downPoly = this.boardView1.movementPolys[7];
                myPoly = new Polygon(downPoly.xpoints, downPoly.ypoints,
                        downPoly.npoints);
                graph.setColor(Color.darkGray);
                myPoly.translate(stepPos.x, stepPos.y);
                graph.drawPolygon(myPoly);
                graph.setColor(col);
                myPoly.translate(-1, -1);
                graph.drawPolygon(myPoly);
                offsetCostPos = new Point(stepPos.x + 1, stepPos.y + 15);
                drawMovementCost(step, offsetCostPos, graph, col, false);
                drawRemainingVelocity(step, stepPos, graph, true);
                break;
            case GET_UP:
            case UP:
            case CAREFUL_STAND:
                // draw arrow indicating standing up
                // also doubles as the climb indication
                Polygon upPoly = this.boardView1.movementPolys[6];
                myPoly = new Polygon(upPoly.xpoints, upPoly.ypoints,
                        upPoly.npoints);
                graph.setColor(Color.darkGray);
                myPoly.translate(stepPos.x, stepPos.y);
                graph.drawPolygon(myPoly);
                graph.setColor(col);
                myPoly.translate(-1, -1);
                graph.drawPolygon(myPoly);
                offsetCostPos = new Point(stepPos.x, stepPos.y + 15);
                drawMovementCost(step, offsetCostPos, graph, col, false);
                drawRemainingVelocity(step, stepPos, graph, true);
                break;
            case CLIMB_MODE_ON:
                // draw climb mode indicator
                String climb;
                if (step.getParent().getEntity().getMovementMode() == EntityMovementMode.WIGE) {
                    climb = Messages.getString("BoardView1.WIGEClimb"); //$NON-NLS-1$
                } else {
                    climb = Messages.getString("BoardView1.Climb"); //$NON-NLS-1$
                }
                if (step.isPastDanger()) {
                    climb = "(" + climb + ")"; //$NON-NLS-1$ //$NON-NLS-2$
                }
                graph.setFont(new Font("SansSerif", Font.PLAIN, 12)); //$NON-NLS-1$
                int climbX = (stepPos.x + 42)
                        - (graph.getFontMetrics(graph.getFont())
                                .stringWidth(climb) / 2);
                graph.setColor(Color.darkGray);
                graph.drawString(climb, climbX, stepPos.y + 39);
                graph.setColor(col);
                graph.drawString(climb, climbX - 1, stepPos.y + 38);
                break;
            case CLIMB_MODE_OFF:
                // cancel climb mode indicator
                String climboff;
                if (step.getParent().getEntity().getMovementMode() == EntityMovementMode.WIGE) {
                    climboff = Messages
                            .getString("BoardView1.WIGEClimbOff"); //$NON-NLS-1$
                } else {
                    climboff = Messages.getString("BoardView1.ClimbOff"); //$NON-NLS-1$
                }
                if (step.isPastDanger()) {
                    climboff = "(" + climboff + ")"; //$NON-NLS-1$ //$NON-NLS-2$
                }
                graph.setFont(new Font("SansSerif", Font.PLAIN, 12)); //$NON-NLS-1$
                int climboffX = (stepPos.x + 42)
                        - (graph.getFontMetrics(graph.getFont())
                                .stringWidth(climboff) / 2);
                graph.setColor(Color.darkGray);
                graph.drawString(climboff, climboffX, stepPos.y + 39);
                graph.setColor(col);
                graph.drawString(climboff, climboffX - 1, stepPos.y + 38);

                break;
            case TURN_LEFT:
            case TURN_RIGHT:
            case THRUST:
            case YAW:
            case EVADE:
            case ROLL:
                // draw arrows showing the facing
                myPoly = new Polygon(facingPoly.xpoints,
                        facingPoly.ypoints, facingPoly.npoints);
                graph.setColor(Color.darkGray);
                myPoly.translate(stepPos.x + 1, stepPos.y + 1);
                graph.drawPolygon(myPoly);
                graph.setColor(col);
                myPoly.translate(-1, -1);
                graph.drawPolygon(myPoly);
                if (this.boardView1.game.useVectorMove()) {
                    drawMovementCost(step, stepPos, graph, col, false);
                }
                break;
            case LOAD:
                // Announce load.
                String load = Messages.getString("BoardView1.Load"); //$NON-NLS-1$
                if (step.isPastDanger()) {
                    load = "(" + load + ")"; //$NON-NLS-1$ //$NON-NLS-2$
                }
                graph.setFont(new Font("SansSerif", Font.PLAIN, 12)); //$NON-NLS-1$
                int loadX = (stepPos.x + 42)
                        - (graph.getFontMetrics(graph.getFont())
                                .stringWidth(load) / 2);
                graph.setColor(Color.darkGray);
                graph.drawString(load, loadX, stepPos.y + 39);
                graph.setColor(col);
                graph.drawString(load, loadX - 1, stepPos.y + 38);
                break;
            case LAUNCH:
            case UNDOCK:
                // announce launch
                String launch = Messages.getString("BoardView1.Launch"); //$NON-NLS-1$
                if (step.isPastDanger()) {
                    launch = "(" + launch + ")"; //$NON-NLS-1$ //$NON-NLS-2$
                }
                graph.setFont(new Font("SansSerif", Font.PLAIN, 12)); //$NON-NLS-1$
                int launchX = (stepPos.x + 42)
                        - (graph.getFontMetrics(graph.getFont())
                                .stringWidth(launch) / 2);
                int launchY = stepPos.y + 38
                        + graph.getFontMetrics(graph.getFont()).getHeight();
                graph.setColor(Color.darkGray);
                graph.drawString(launch, launchX, launchY + 1);
                graph.setColor(col);
                graph.drawString(launch, launchX - 1, launchY);
                break;
            case DROP:
                // announce drop
                String drop = Messages.getString("BoardView1.Drop"); //$NON-NLS-1$
                if (step.isPastDanger()) {
                    drop = "(" + drop + ")"; //$NON-NLS-1$ //$NON-NLS-2$
                }
                graph.setFont(new Font("SansSerif", Font.PLAIN, 12)); //$NON-NLS-1$
                int dropX = (stepPos.x + 42)
                        - (graph.getFontMetrics(graph.getFont())
                                .stringWidth(drop) / 2);
                int dropY = stepPos.y + 38
                        + graph.getFontMetrics(graph.getFont()).getHeight();
                graph.setColor(Color.darkGray);
                graph.drawString(drop, dropX, dropY + 1);
                graph.setColor(col);
                graph.drawString(drop, dropX - 1, dropY);
                break;
            case RECOVER:
                // announce launch
                String recover = Messages.getString("BoardView1.Recover"); //$NON-NLS-1$
                if (step.isDocking()) {
                    recover = Messages.getString("BoardView1.Dock"); //$NON-NLS-1$
                }
                if (step.isPastDanger()) {
                    launch = "(" + recover + ")"; //$NON-NLS-1$ //$NON-NLS-2$
                }
                graph.setFont(new Font("SansSerif", Font.PLAIN, 12)); //$NON-NLS-1$
                int recoverX = (stepPos.x + 42)
                        - (graph.getFontMetrics(graph.getFont())
                                .stringWidth(recover) / 2);
                int recoverY = stepPos.y + 38
                        + graph.getFontMetrics(graph.getFont()).getHeight();
                graph.setColor(Color.darkGray);
                graph.drawString(recover, recoverX, recoverY + 1);
                graph.setColor(col);
                graph.drawString(recover, recoverX - 1, recoverY);
                break;
            case JOIN:
                // announce launch
                String join = Messages.getString("BoardView1.Join"); //$NON-NLS-1$
                if (step.isPastDanger()) {
                    launch = "(" + join + ")"; //$NON-NLS-1$ //$NON-NLS-2$
                }
                graph.setFont(new Font("SansSerif", Font.PLAIN, 12)); //$NON-NLS-1$
                int joinX = (stepPos.x + 42)
                        - (graph.getFontMetrics(graph.getFont())
                                .stringWidth(join) / 2);
                int joinY = stepPos.y + 38
                        + graph.getFontMetrics(graph.getFont()).getHeight();
                graph.setColor(Color.darkGray);
                graph.drawString(join, joinX, joinY + 1);
                graph.setColor(col);
                graph.drawString(join, joinX - 1, joinY);
                break;
            case UNLOAD:
                // Announce unload.
                String unload = Messages.getString("BoardView1.Unload"); //$NON-NLS-1$
                if (step.isPastDanger()) {
                    unload = "(" + unload + ")"; //$NON-NLS-1$ //$NON-NLS-2$
                }
                graph.setFont(new Font("SansSerif", Font.PLAIN, 12)); //$NON-NLS-1$
                int unloadX = (stepPos.x + 42)
                        - (graph.getFontMetrics(graph.getFont())
                                .stringWidth(unload) / 2);
                int unloadY = stepPos.y + 38
                        + graph.getFontMetrics(graph.getFont()).getHeight();
                graph.setColor(Color.darkGray);
                graph.drawString(unload, unloadX, unloadY + 1);
                graph.setColor(col);
                graph.drawString(unload, unloadX - 1, unloadY);
                break;
            case HOVER:
                // announce launch
                String hover = Messages.getString("BoardView1.Hover"); //$NON-NLS-1$
                if (step.isPastDanger()) {
                    hover = "(" + hover + ")"; //$NON-NLS-1$ //$NON-NLS-2$
                }
                graph.setFont(new Font("SansSerif", Font.PLAIN, 12)); //$NON-NLS-1$
                int hoverX = (stepPos.x + 42)
                        - (graph.getFontMetrics(graph.getFont())
                                .stringWidth(hover) / 2);
                int hoverY = stepPos.y + 38
                        + graph.getFontMetrics(graph.getFont()).getHeight();
                graph.setColor(Color.darkGray);
                graph.drawString(hover, hoverX, hoverY + 1);
                graph.setColor(col);
                graph.drawString(hover, hoverX - 1, hoverY);
                drawMovementCost(step, stepPos, graph, col, false);
                break;
            case LAND:
                // announce land
                String land = Messages.getString("BoardView1.Land"); //$NON-NLS-1$
                if (step.isPastDanger()) {
                    land = "(" + land + ")"; //$NON-NLS-1$ //$NON-NLS-2$
                }
                graph.setFont(new Font("SansSerif", Font.PLAIN, 12)); //$NON-NLS-1$
                int landX = (stepPos.x + 42)
                        - (graph.getFontMetrics(graph.getFont())
                                .stringWidth(land) / 2);
                int landY = stepPos.y + 38
                        + graph.getFontMetrics(graph.getFont()).getHeight();
                graph.setColor(Color.darkGray);
                graph.drawString(land, landX, landY + 1);
                graph.setColor(col);
                graph.drawString(land, landX - 1, landY);
                break;
            default:
                break;
        }

        baseScaleImage = this.boardView1.createImage(new FilteredImageSource(
                tempImage.getSource(), new KeyAlphaFilter(BoardView1.TRANSPARENT)));
        // create final image
        if (this.boardView1.zoomIndex == BoardView1.BASE_ZOOM_INDEX) {
            image = this.boardView1.createImage(new FilteredImageSource(
                    tempImage.getSource(), new KeyAlphaFilter(BoardView1.TRANSPARENT)));
        } else {
            image = this.boardView1.getScaledImage(this.boardView1.createImage(new FilteredImageSource(
                    tempImage.getSource(), new KeyAlphaFilter(BoardView1.TRANSPARENT))),false);
        }
        graph.dispose();
        tempImage.flush();
    }

    /**
     * draw conditions separate from the step, This allows me to keep
     * conditions on the Aero even when that step is erased (as per advanced
     * movement). For now, just evading and rolling. eventually loading and
     * unloading as well
     */
    private void drawConditions(MoveStep step, Point stepPos,
            Graphics graph, Color col) {

        if (step.isEvading()) {
            String evade = Messages.getString("BoardView1.Evade"); //$NON-NLS-1$
            graph.setFont(new Font("SansSerif", Font.PLAIN, 12)); //$NON-NLS-1$
            int evadeX = (stepPos.x + 42)
                    - (graph.getFontMetrics(graph.getFont()).stringWidth(
                            evade) / 2);
            graph.setColor(Color.darkGray);
            graph.drawString(evade, evadeX, stepPos.y + 64);
            graph.setColor(col);
            graph.drawString(evade, evadeX - 1, stepPos.y + 63);
        }

        if (step.isRolled()) {
            // Announce roll
            String roll = Messages.getString("BoardView1.Roll"); //$NON-NLS-1$
            graph.setFont(new Font("SansSerif", Font.PLAIN, 12)); //$NON-NLS-1$
            int rollX = (stepPos.x + 42)
                    - (graph.getFontMetrics(graph.getFont()).stringWidth(
                            roll) / 2);
            graph.setColor(Color.darkGray);
            graph.drawString(roll, rollX, stepPos.y + 18);
            graph.setColor(col);
            graph.drawString(roll, rollX - 1, stepPos.y + 17);
        }

    }

    private void drawActiveVectors(MoveStep step, Point stepPos,
            Graphics graph) {

        /*
         * TODO: it might be better to move this to the MovementSprite so
         * that it is visible before first step and you can't see it for all
         * entities
         */

        int[] activeXpos = { 39, 59, 59, 40, 19, 19 };
        int[] activeYpos = { 20, 28, 52, 59, 52, 28 };

        int[] v = step.getVectors();
        for (int i = 0; i < 6; i++) {

            String active = Integer.toString(v[i]);
            graph.setFont(new Font("SansSerif", Font.PLAIN, 12)); //$NON-NLS-1$
            graph.setColor(Color.darkGray);
            graph.drawString(active, activeXpos[i] + stepPos.x,
                    activeYpos[i] + stepPos.y);
            graph.setColor(Color.red);
            graph.drawString(active, (activeXpos[i] + stepPos.x) - 1,
                    (activeYpos[i] + stepPos.y) - 1);

        }

    }

    @Override
    public Rectangle getBounds() {
        bounds = new Rectangle(this.boardView1.getHexLocation(step.getPosition()), this.boardView1.hex_size);
        return bounds;
    }

    public MoveStep getStep() {
        return step;
    }

    public Font getMovementFont() {

        String fontName = GUIPreferences.getInstance().getString(
                GUIPreferences.ADVANCED_MOVE_FONT_TYPE);
        int fontStyle = GUIPreferences.getInstance().getInt(
                GUIPreferences.ADVANCED_MOVE_FONT_STYLE);
        int fontSize = GUIPreferences.getInstance().getInt(
                GUIPreferences.ADVANCED_MOVE_FONT_SIZE);

        return new Font(fontName, fontStyle, fontSize);
    }

    private void drawRemainingVelocity(MoveStep step, Point stepPos,
            Graphics graph, boolean shiftFlag) {
        String velString = null;
        StringBuffer velStringBuf = new StringBuffer();

        if (this.boardView1.game.useVectorMove()) {
            return;
        }

        if (!step.getParent().getEntity().isAirborne()
                || !(step.getParent().getEntity() instanceof Aero)) {
            return;
        }

        if (((Aero) step.getParent().getEntity()).isSpheroid()) {
            return;
        }

        int distTraveled = step.getDistance();
        int velocity = step.getVelocity();
        if (this.boardView1.game.getBoard().onGround()) {
            velocity *= 16;
        }

        velStringBuf.append("(").append(distTraveled).append("/")
                .append(velocity).append(")");

        Color col = Color.GREEN;
        if (step.getVelocityLeft() > 0) {
            col = Color.RED;
        }

        // Convert the buffer to a String and draw it.
        velString = velStringBuf.toString();
        graph.setFont(new Font("SansSerif", Font.PLAIN, 12)); //$NON-NLS-1$
        int costX = stepPos.x + 42;
        if (shiftFlag) {
            costX -= (graph.getFontMetrics(graph.getFont()).stringWidth(
                    velString) / 2);
        }
        graph.setColor(Color.darkGray);
        graph.drawString(velString, costX, stepPos.y + 28);
        graph.setColor(col);
        graph.drawString(velString, costX - 1, stepPos.y + 27);

        // if we are in atmosphere, then report the free turn status as well
        if (!this.boardView1.game.getBoard().inSpace()) {
            String turnString = null;
            StringBuffer turnStringBuf = new StringBuffer();
            turnStringBuf.append("<").append(step.getNStraight())
                    .append(">");

            col = Color.RED;
            if (step.dueFreeTurn()) {
                col = Color.GREEN;
            } else if (step.canAeroTurn(this.boardView1.game)) {
                col = Color.YELLOW;
            }
            // Convert the buffer to a String and draw it.
            turnString = turnStringBuf.toString();
            graph.setFont(new Font("SansSerif", Font.PLAIN, 10)); //$NON-NLS-1$
            costX = stepPos.x + 50;
            graph.setColor(Color.darkGray);
            graph.drawString(turnString, costX, stepPos.y + 15);
            graph.setColor(col);
            graph.drawString(turnString, costX - 1, stepPos.y + 14);
        }
    }

    private void drawMovementCost(MoveStep step, Point stepPos,
            Graphics graph, Color col, boolean shiftFlag) {
        String costString = null;
        StringBuffer costStringBuf = new StringBuffer();
        costStringBuf.append(step.getMpUsed());

        // If the step is using a road bonus, mark it.
        if (step.isOnlyPavement()
                && (step.getParent().getEntity() instanceof Tank)) {
            costStringBuf.append("+"); //$NON-NLS-1$
        }

        // If the step is dangerous, mark it.
        if (step.isDanger()) {
            costStringBuf.append("*"); //$NON-NLS-1$
        }

        // If the step is past danger, mark that.
        if (step.isPastDanger()) {
            costStringBuf.insert(0, "("); //$NON-NLS-1$
            costStringBuf.append(")"); //$NON-NLS-1$
        }

        if (step.isUsingMASC()
                && !step.getParent().getEntity()
                        .hasWorkingMisc(MiscType.F_JET_BOOSTER)) {
            costStringBuf.append("["); //$NON-NLS-1$
            costStringBuf.append(step.getTargetNumberMASC());
            costStringBuf.append("+]"); //$NON-NLS-1$
        }

        if ((step.getMovementType() == EntityMovementType.MOVE_VTOL_WALK)
                || (step.getMovementType() == EntityMovementType.MOVE_VTOL_RUN)
                || (step.getMovementType() == EntityMovementType.MOVE_SUBMARINE_WALK)
                || (step.getMovementType() == EntityMovementType.MOVE_SUBMARINE_RUN)) {
            costStringBuf.append("{").append(step.getElevation())
                    .append("}");
        }

        if (step.getParent().getEntity().isAirborne()) {
            costStringBuf.append("{").append(step.getAltitude())
                    .append("}");
        }

        // Convert the buffer to a String and draw it.
        costString = costStringBuf.toString();
        graph.setFont(getMovementFont()); //$NON-NLS-1$
        int costX = stepPos.x + 42;
        if (shiftFlag) {
            costX -= (graph.getFontMetrics(graph.getFont()).stringWidth(
                    costString) / 2);
        }
        graph.setColor(Color.darkGray);
        graph.drawString(costString, costX, stepPos.y + 39);
        graph.setColor(col);
        graph.drawString(costString, costX - 1, stepPos.y + 38);
    }

}