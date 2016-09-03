package megamek.client.ui.swing.boardview;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import megamek.client.ui.swing.GUIPreferences;
import megamek.common.Compute;
import megamek.common.ComputeECM;
import megamek.common.Coords;
import megamek.common.ECMInfo;
import megamek.common.Entity;
import megamek.common.Game;
import megamek.common.IBoard;
import megamek.common.IHex;
import megamek.common.LosEffects;
import megamek.common.MoveStep;
import megamek.common.IGame.Phase;
import megamek.common.event.GameListener;
import megamek.common.event.GameListenerAdapter;
import megamek.common.event.GameTurnChangeEvent;
import megamek.common.options.OptionsConstants;
import megamek.common.preference.IPreferenceChangeListener;
import megamek.common.preference.PreferenceChangeEvent;

/**
 * A helper class for highlighting and darkening hexes.
 *
 */
class FovHighlightingAndDarkening {

    /**
     * 
     */
    private final BoardView1 boardView1;
    private java.util.List<Color> ringsColors = new ArrayList<>();
    private java.util.List<Integer> ringsRadii = new ArrayList<>();
    GUIPreferences gs = GUIPreferences.getInstance();
    private IPreferenceChangeListener ringsChangeListner;

    public FovHighlightingAndDarkening(BoardView1 boardView1) {
        this.boardView1 = boardView1;
        updateRingsProperties();
        ringsChangeListner = new IPreferenceChangeListener() {
            @Override
            public void preferenceChange(PreferenceChangeEvent e) {
                String eName= e.getName();
                if( eName.equals( GUIPreferences.FOV_HIGHLIGHT_RINGS_RADII)
                        || eName.equals(GUIPreferences.FOV_HIGHLIGHT_RINGS_COLORS_HSB)
                        || eName.equals(GUIPreferences.FOV_HIGHLIGHT_ALPHA) )
                    updateRingsProperties();
            }
        };
        gs.addPreferenceChangeListener( ringsChangeListner );

        cacheGameListner = new GameListenerAdapter() {
            @Override
            public void gameTurnChange(GameTurnChangeEvent e) {
                cacheGameChanged = true;
            }
        };
        this.boardView1.game.addGameListener(cacheGameListner);
    }

    public void die() {
        gs.removePreferenceChangeListener(ringsChangeListner);
        this.boardView1.game.removeGameListener(cacheGameListner);
    };


    /**
     * Checks if options for darkening and highlighting are turned on and the
     * respectively: If there is no LOS from curently selected hex/entity, then
     * darkens hex c. If there is a LOS from the hex c to the selected
     * hex/entity, then hex c is colored according to distance.
     * 
     * @param boardGraph
     *            The board on which we paint.
     * @param c
     *            Hex that is being processed.
     * @param drawX
     *            The x coordinate of hex <b>c</b> on board image. should be
     *            equal to getHexLocation(c).x
     * @param drawY
     *            The y coordinate of hex <b>c</b> on board image. should be
     *            equal to getHexLocation(c).x
     * @param saveBoardImage
     */
    boolean draw(Graphics boardGraph, Coords c, int drawX, int drawY,
            boolean saveBoardImage) {

        Coords src;
        boolean hasLoS = true;
        if (this.boardView1.selected != null) {
            src = this.boardView1.selected;
        } else if (this.boardView1.selectedEntity != null) {
            src = this.boardView1.selectedEntity.getPosition();
        } else {
            src = null;
        }


        //if there is no source we have nothing to do.
        if( (src == null) ||  !this.boardView1.game.getBoard().contains(src) ) {
            return true;
        }
        //dont spoil the image with fov drawings
        if(saveBoardImage) {
            return true;
        }

        // Code for LoS darkening/highlighting
        Point p = new Point(drawX, drawY);
        boolean highlight = gs.getBoolean(GUIPreferences.FOV_HIGHLIGHT);
        boolean darken = gs.getBoolean(GUIPreferences.FOV_DARKEN);

        if ((darken || highlight)
                && (this.boardView1.game.getPhase() == Phase.PHASE_MOVEMENT)) {

            final int pad = 0;
            final int lw = 7;

            boolean sensorsOn = boardView1.game.getOptions().booleanOption(
                    OptionsConstants.ADVANCED_TACOPS_SENSORS);
            boolean doubleBlindOn = boardView1.game.getOptions().booleanOption(
                    OptionsConstants.ADVANCED_DOUBLE_BLIND);
            boolean inclusiveSensorsOn = boardView1.game.getOptions().booleanOption(
                    OptionsConstants.ADVANCED_INCLUSIVE_SENSOR_RANGE);

            boolean targetIlluminated = false;
            for (Entity target : this.boardView1.game.getEntitiesVector(c)){
                targetIlluminated |= target.isIlluminated();
            }
            // Target may be in an illuminated hex
            if (!targetIlluminated) {
                int lightLvl = boardView1.game.isPositionIlluminated(c);
                targetIlluminated = lightLvl != Game.ILLUMINATED_NONE;
            }

            final int max_dist;
            // We don't want to have to compute a LoSEffects yet, as that
            //  can be expensive on large viewing areas
            if ((boardView1.selectedEntity != null) && doubleBlindOn) {
                // We can only use this is double blind is on, otherwise visual
                // range won't effect LoS
                max_dist = this.boardView1.game.getPlanetaryConditions()
                        .getVisualRange(this.boardView1.selectedEntity,
                                targetIlluminated);
            } else {
                max_dist = 60;
            }



            final Color transparent_gray = new Color(0, 0, 0,
                    gs.getInt(GUIPreferences.FOV_DARKEN_ALPHA));
            final Color transparent_light_gray = new Color(0, 0, 0,
                    gs.getInt(GUIPreferences.FOV_DARKEN_ALPHA) / 2);
            final Color selected_color = new Color(50, 80, 150, 70);

            int dist = src.distance(c);

            int visualRange = 30;
            int minSensorRange = 0;
            int maxSensorRange = 0;

            if (dist == 0) {
                this.boardView1.drawHexBorder(boardGraph, p, selected_color,
                        pad, lw);
            } else if (dist < max_dist) {
                LosEffects los = getCachedLosEffects(src, c);
                if (null != this.boardView1.selectedEntity) {
                    if (doubleBlindOn) { // Visual Range only matters in DB
                        visualRange = Compute.getVisualRange(
                                this.boardView1.game,
                                this.boardView1.selectedEntity, los,
                                targetIlluminated);
                    }
                    int bracket = Compute.getSensorRangeBracket(
                            this.boardView1.selectedEntity, null,
                            cachedAllECMInfo);
                    int range = Compute.getSensorRangeByBracket(
                            this.boardView1.game,
                            this.boardView1.selectedEntity, null, los);

                    maxSensorRange = bracket * range;
                    minSensorRange = Math.max((bracket - 1) * range, 0);
                    if (inclusiveSensorsOn) {
                        minSensorRange = 0;
                    }
                }
                // Visual Range only matters in DB: ensure no effect w/o DB
                if (!doubleBlindOn) {
                    visualRange = dist;
                }
                if (!los.canSee() || (dist > visualRange)) {
                    if (darken) {
                        if (sensorsOn
                                && (dist > minSensorRange)
                                && (dist <= maxSensorRange)) {
                            boardView1.drawHexLayer(p, boardGraph,
                                    transparent_light_gray, false);
                        } else {
                            boardView1.drawHexLayer(p, boardGraph,
                                    transparent_gray, true);
                        }
                    }
                    hasLoS = false;
                } else if (highlight) {
                    Iterator<Integer> itR= ringsRadii.iterator();
                    Iterator<Color> itC= ringsColors.iterator();
                    while( itR.hasNext() && itC.hasNext() ){
                        int dt= itR.next();
                        Color ct= itC.next();
                        if (dist <= dt) {
                            boardView1.drawHexLayer(p, boardGraph, ct, false);
                            break;
                        }
                    }
                }
            } else {
                // Max dist should be >= visual dist, this hex can't be seen
                if (darken) {
                    this.boardView1.drawHexLayer(p, boardGraph,
                            transparent_gray, true);
                }
                hasLoS = false;
            }
        }
        return hasLoS;
    }

    List<ECMInfo> cachedAllECMInfo = null;
    Entity cachedSelectedEntity = null;
    StepSprite cachedStepSprite = null;
    Coords cachedSrc = null;
    boolean cacheGameChanged = true;
    Map<Coords, LosEffects> losCache = new HashMap<>();

    private void clearCache() {
        losCache = new HashMap<>();
    }

    GameListener cacheGameListner;

    /**
     * Checks for los effects, preferably from cache, if not getLosEffects
     * is invoked and it's return value is cached.
     * If enviroment has changed between calls to this method the cache is
     * cleared.
     */
    private LosEffects getCachedLosEffects(Coords src, Coords dest) {
        ArrayList<StepSprite> pathSprites = boardView1.pathSprites;
        StepSprite lastStepSprite = pathSprites.size() > 0 ? pathSprites
                .get(pathSprites.size() - 1) : null;
        // lets check if cache should be cleared
        if ((cachedSelectedEntity != this.boardView1.selectedEntity)
                || (cachedStepSprite != lastStepSprite)
                || (!src.equals(cachedSrc)) || (cacheGameChanged)) {
            clearCache();
            cachedSelectedEntity = this.boardView1.selectedEntity;
            cachedStepSprite = lastStepSprite;
            cachedSrc = src;
            cacheGameChanged = false;
            cachedAllECMInfo = ComputeECM
                    .computeAllEntitiesECMInfo(boardView1.game
                            .getEntitiesVector());
        }

        LosEffects los = losCache.get(dest);
        if (los == null) {
            los = this.boardView1.fovHighlightingAndDarkening.getLosEffects(
                    src, dest);
            losCache.put(dest, los);
        }
        return los;
    }

    /**Parses the properties of rings received from GUIPreferencess.
     *
     */
    private void updateRingsProperties() {
        //prepare the parameters for processing bracket by bracket
        String[] dRingsRadiiRaw = gs
                .getString(GUIPreferences.FOV_HIGHLIGHT_RINGS_RADII).trim()
                .split("\\s+");
        String[] dRingsColorsRaw = gs.getString(
                GUIPreferences.FOV_HIGHLIGHT_RINGS_COLORS_HSB).split(";");
        final int highlight_alpha = gs
                .getInt(GUIPreferences.FOV_HIGHLIGHT_ALPHA);
        final int max_dist = 60;

        ringsRadii= new ArrayList<>();
        ringsColors= new ArrayList<>();

        for(String rrRaw: dRingsRadiiRaw){
            try {
                int rr= Integer.parseInt(rrRaw.trim());
                ringsRadii.add( Math.min(rr, max_dist) );
            } catch (NumberFormatException e) {
                System.err.printf("%s parameter unparsable '%s'"
                        ,GUIPreferences.FOV_HIGHLIGHT_RINGS_RADII, rrRaw );
                e.printStackTrace();
                System.err.flush();
                break;
            }
        }

        for(String rcr: dRingsColorsRaw ){
            try {
                String[] hsbr= rcr.trim().split("\\s+");
                float h=Float.parseFloat( hsbr[0] );
                float s=Float.parseFloat( hsbr[1] );
                float b=Float.parseFloat( hsbr[2] );
                Color tc= new Color( Color.HSBtoRGB(h, s, b) );
                ringsColors.add(new Color(tc.getRed(), tc.getGreen(), tc
                        .getBlue(), highlight_alpha));
            } catch (NumberFormatException e) {
                System.err.printf("%s parameter unparsable '%s'"
                        ,GUIPreferences.FOV_HIGHLIGHT_RINGS_COLORS_HSB, rcr );
                e.printStackTrace();
                System.err.flush();
                break;
            }
        }
    }

    /**
     * Calculate the LosEffects between the given Coords. Unit height for
     * the source hex is determined by the selectedEntity if present
     * otherwise the GUIPreference 'mechInFirst' is used. If pathSprites are
     * not empty then elevation from last step is used for attacker
     * elevation, also it is assumed that last step's position is equal to
     * src.
     * Unit height for the destination hex is determined by the tallest unit
     * present in that hex. If no units are present, the GUIPreference
     * 'mechInSecond' is used.
     */
    private LosEffects getLosEffects(Coords src, Coords dest) {
        /*
         * The getCachedLos method depends that this method uses only
         * information from src, dest, game, selectedEntity and the last
         * stepSprite from path Sprites. If this behavior changes, please
         * change
         * getCachedLos method accordingly.
         */
        GUIPreferences guip = GUIPreferences.getInstance();
        IBoard board = this.boardView1.game.getBoard();
        IHex srcHex = board.getHex(src);
        IHex dstHex = board.getHex(dest);
        LosEffects.AttackInfo ai = new LosEffects.AttackInfo();
        ai.attackPos = src;
        ai.targetPos = dest;
        // First, we check for a selected unit and use its height. If
        // there's
        // no selected unit we use the mechInFirst GUIPref.
        if (this.boardView1.selectedEntity != null) {
            ai.attackHeight = this.boardView1.selectedEntity.getHeight();
            // Elevation of entity above the hex surface
            int elevation;
            if (this.boardView1.pathSprites.size() > 0) {
                // If we've got a step, get the elevation from it
                int lastStepIdx = this.boardView1.pathSprites.size() - 1;
                MoveStep lastMS = this.boardView1.pathSprites.get(lastStepIdx)
                        .getStep();
                elevation = lastMS.getElevation();
            } else {
                // otherwise we use entity's elevation
                elevation = this.boardView1.selectedEntity.getElevation();
            }
            ai.attackAbsHeight = srcHex.surface() + elevation
                    + this.boardView1.selectedEntity.getHeight();
        } else {
            ai.attackHeight = guip.getMechInFirst() ? 1 : 0;
            ai.attackAbsHeight = srcHex.surface() + ai.attackHeight;
        }
        // First, we take the tallest unit in the destination hex, if no
        // units
        // are present we use the mechInSecond GUIPref.
        ai.targetHeight = ai.targetAbsHeight = Integer.MIN_VALUE;
        for (Entity ent : this.boardView1.game.getEntitiesVector(dest)) {
            int trAbsheight = dstHex.surface() + ent.relHeight();
            if (trAbsheight > ai.targetAbsHeight) {
                ai.targetHeight = ent.getHeight();
                ai.targetAbsHeight = trAbsheight;
            }
        }
        if ((ai.targetHeight == Integer.MIN_VALUE)
                && (ai.targetAbsHeight == Integer.MIN_VALUE)) {
            ai.targetHeight = guip.getMechInSecond() ? 1 : 0;
            ai.targetAbsHeight = dstHex.surface() + ai.targetHeight;
        }
        return LosEffects.calculateLos(this.boardView1.game, ai);
    }




}