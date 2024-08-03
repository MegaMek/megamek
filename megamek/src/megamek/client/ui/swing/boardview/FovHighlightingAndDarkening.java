package megamek.client.ui.swing.boardview;

import megamek.client.ui.swing.GUIPreferences;
import megamek.common.*;
import megamek.common.annotations.Nullable;
import megamek.common.event.GameListener;
import megamek.common.event.GameListenerAdapter;
import megamek.common.event.GameTurnChangeEvent;
import megamek.common.options.OptionsConstants;
import megamek.common.planetaryconditions.IlluminationLevel;
import megamek.common.preference.IPreferenceChangeListener;
import org.apache.logging.log4j.LogManager;

import java.awt.*;
import java.util.List;
import java.util.*;

/**
 * A helper class for highlighting and darkening hexes.
 */
class FovHighlightingAndDarkening {
    private final BoardView boardView1;
    private java.util.List<Color> ringsColors = new ArrayList<>();
    private java.util.List<Integer> ringsRadii = new ArrayList<>();
    GUIPreferences gs = GUIPreferences.getInstance();
    private IPreferenceChangeListener ringsChangeListner;

    public FovHighlightingAndDarkening(BoardView boardView1) {
        this.boardView1 = boardView1;
        updateRingsProperties();
        ringsChangeListner = e -> {
            String eName= e.getName();
            if (eName.equals( GUIPreferences.FOV_HIGHLIGHT_RINGS_RADII)
                    || eName.equals(GUIPreferences.FOV_HIGHLIGHT_RINGS_COLORS_HSB)
                    || eName.equals(GUIPreferences.FOV_HIGHLIGHT_ALPHA)) {
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
    }


    /**
     * Checks if options for darkening and highlighting are turned on and the
     * respectively: If there is no LOS from currently selected hex/entity, then
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
    boolean draw(Graphics boardGraph, Coords c, int drawX, int drawY, boolean saveBoardImage) {
        Coords src;
        boolean hasLoS = true;
        // in movement phase, calc LOS based on selected hex, otherwise use selected Entity
        if (boardView1.game.getPhase().isMovement() && this.boardView1.selected != null) {
            src = boardView1.selected;
        } else if (boardView1.getSelectedEntity() != null) {
            Entity viewer = boardView1.getSelectedEntity();
            src = viewer.getPosition();
            // multi-hex units look from the hex closest to the target to avoid self-blocking
            src = viewer.getSecondaryPositions().values().stream()
                    .min(Comparator.comparingInt(co -> co.distance(c))).orElse(src);
        } else {
            src = null;
        }

        // if there is no source we have nothing to do.
        if ((src == null) || !this.boardView1.game.getBoard().contains(src)) {
            return true;
        }
        // don't spoil the image with fov drawings
        if (saveBoardImage) {
            return true;
        }

        // Code for LoS darkening/highlighting
        Point p = new Point(drawX, drawY);
        boolean highlight = this.boardView1.shouldFovHighlight();
        boolean darken = this.boardView1.shouldFovDarken();

        if (darken || highlight) {

            final int pad = 0;
            final int lw = 7;

            boolean sensorsOn = (boardView1.game.getOptions().booleanOption(
                    OptionsConstants.ADVANCED_TACOPS_SENSORS)
                    || boardView1.game.getOptions().booleanOption(OptionsConstants.ADVAERORULES_STRATOPS_ADVANCED_SENSORS));
            boolean doubleBlindOn = boardView1.game.getOptions().booleanOption(
                    OptionsConstants.ADVANCED_DOUBLE_BLIND);
            boolean inclusiveSensorsOn = boardView1.game.getOptions().booleanOption(
                    OptionsConstants.ADVANCED_INCLUSIVE_SENSOR_RANGE);

            // Determine if any of the entities at the coordinates are illuminated, or if the
            // coordinates are illuminated themselves
            boolean targetIlluminated = boardView1.game.getEntitiesVector(c).stream().anyMatch(Entity::isIlluminated)
                    || !IlluminationLevel.determineIlluminationLevel(boardView1.game, c).isNone();

            final int max_dist;
            // We don't want to have to compute a LoSEffects yet, as that
            //  can be expensive on large viewing areas
            if ((boardView1.getSelectedEntity() != null) && doubleBlindOn) {
                // We can only use this is double blind is on, otherwise visual
                // range won't effect LoS
                max_dist = this.boardView1.game.getPlanetaryConditions()
                        .getVisualRange(this.boardView1.getSelectedEntity(),
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
                this.boardView1.drawHexBorder(boardGraph, p, selected_color, pad, lw);
            } else if (dist < max_dist) {
                LosEffects los = getCachedLosEffects(src, c);
                if (null != this.boardView1.getSelectedEntity()) {
                    if (los == null) {
                        los = LosEffects.calculateLOS(boardView1.game, boardView1.getSelectedEntity(), null);
                    }

                    if (doubleBlindOn) { // Visual Range only matters in DB
                        visualRange = Compute.getVisualRange(
                                this.boardView1.game,
                                this.boardView1.getSelectedEntity(), los,
                                targetIlluminated);
                    }
                    int bracket = Compute.getSensorRangeBracket(
                            this.boardView1.getSelectedEntity(), null,
                            cachedAllECMInfo);
                    int range = Compute.getSensorRangeByBracket(
                            this.boardView1.game,
                            this.boardView1.getSelectedEntity(), null, los);

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

                if (((los != null) && !los.canSee()) || (dist > visualRange)) {
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
                    while (itR.hasNext() && itC.hasNext()) {
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
                    this.boardView1.drawHexLayer(p, boardGraph, transparent_gray, true);
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
     * If environment has changed between calls to this method the cache is
     * cleared.
     */
    public @Nullable LosEffects getCachedLosEffects(Coords src, Coords dest) {
        ArrayList<StepSprite> pathSprites = boardView1.pathSprites;
        StepSprite lastStepSprite = pathSprites.isEmpty() ? null : pathSprites.get(pathSprites.size() - 1);
        // lets check if cache should be cleared
        if ((cachedSelectedEntity != this.boardView1.getSelectedEntity())
                || (cachedStepSprite != lastStepSprite)
                || (!src.equals(cachedSrc)) || (cacheGameChanged)) {
            clearCache();
            cachedSelectedEntity = this.boardView1.getSelectedEntity();
            cachedStepSprite = lastStepSprite;
            cachedSrc = src;
            cacheGameChanged = false;
            cachedAllECMInfo = ComputeECM.computeAllEntitiesECMInfo(boardView1.game.getEntitiesVector());
        }

        LosEffects los = losCache.get(dest);
        if (los == null) {
            los = getLosEffects(src, dest);
            if (los == null) {
                return null;
            }
            losCache.put(dest, los);
        }
        return los;
    }

    /**
     * Parses the properties of rings received from GUIPreferences
     */
    private void updateRingsProperties() {
        //prepare the parameters for processing bracket by bracket
        String[] dRingsRadiiRaw = gs.getString(GUIPreferences.FOV_HIGHLIGHT_RINGS_RADII).trim().split("\\s+");
        String[] dRingsColorsRaw = gs.getString(GUIPreferences.FOV_HIGHLIGHT_RINGS_COLORS_HSB).split(";");
        final int highlight_alpha = gs.getInt(GUIPreferences.FOV_HIGHLIGHT_ALPHA);
        final int max_dist = 60;

        ringsRadii= new ArrayList<>();
        ringsColors= new ArrayList<>();

        for (String rrRaw: dRingsRadiiRaw) {
            try {
                int rr = Integer.parseInt(rrRaw.trim());
                ringsRadii.add(Math.min(rr, max_dist));
            } catch (Exception e) {
                LogManager.getLogger().error(String.format("Cannot parse %s parameter '%s'",
                        GUIPreferences.FOV_HIGHLIGHT_RINGS_RADII, rrRaw), e);
                break;
            }
        }

        for (String rcr: dRingsColorsRaw) {
            try {
                String[] hsbr = rcr.trim().split("\\s+");
                float h = Float.parseFloat(hsbr[0]);
                float s = Float.parseFloat(hsbr[1]);
                float b = Float.parseFloat(hsbr[2]);
                Color tc = new Color(Color.HSBtoRGB(h, s, b));
                ringsColors.add(new Color(tc.getRed(), tc.getGreen(), tc.getBlue(), highlight_alpha));
            } catch (Exception e) {
                LogManager.getLogger().error(String.format("Cannot parse %s parameter '%s'",
                        GUIPreferences.FOV_HIGHLIGHT_RINGS_COLORS_HSB, rcr), e);
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
    private @Nullable LosEffects getLosEffects(final Coords src, final Coords dest) {
        /*
         * The getCachedLos method depends that this method uses only
         * information from src, dest, game, selectedEntity and the last
         * stepSprite from path Sprites. If this behavior changes, please
         * change the getCachedLos method accordingly.
         */
        GUIPreferences guip = GUIPreferences.getInstance();
        Board board = this.boardView1.game.getBoard();
        Hex srcHex = board.getHex(src);
        if (srcHex == null) {
            LogManager.getLogger().error("Cannot process line of sight effects with a null source hex.");
            return null;
        }
        Hex dstHex = board.getHex(dest);
        if (dstHex == null) {
            LogManager.getLogger().error("Cannot process line of sight effects with a null destination hex.");
            return null;
        }

        // Need to re-write this to work with Low Alt maps
//        LosEffects.AttackInfo ai = new LosEffects.AttackInfo();
        LosEffects.AttackInfo ai = LosEffects.prepLosAttackInfo(
                this.boardView1.game, this.boardView1.getSelectedEntity(), null, src, dest,
                guip.getMechInFirst(), guip.getMechInSecond()
        );
        //ai.attackPos = src;
        //ai.targetPos = dest;
        // First, we check for a selected unit and use its height. If
        // there's no selected unit we use the mechInFirst GUIPref.
        if (this.boardView1.getSelectedEntity() != null) {
            Entity ae = this.boardView1.getSelectedEntity();
            // Elevation of entity above the hex surface
            int elevation;
            if (!boardView1.pathSprites.isEmpty()) {
                // If we've got a step, get the elevation from it
                int lastStepIdx = this.boardView1.pathSprites.size() - 1;
                MoveStep lastMS = this.boardView1.pathSprites.get(lastStepIdx)
                        .getStep();
                elevation = (ai.lowAltitude) ? lastMS.getAltitude() : lastMS.getElevation();
            } else {
                // otherwise we use entity's altitude / elevation
                elevation = (ai.lowAltitude) ? ae.getAltitude() : ae.getElevation();
            }
            ai.attackAbsHeight = (ai.lowAltitude) ? elevation : srcHex.getLevel() + elevation + ae.getHeight();
        } else {
            // For hexes, getLevel is functionally the same as getAltitude()
            ai.attackAbsHeight = srcHex.getLevel() + ai.attackHeight;
        }
        // First, we take the tallest unit in the destination hex, if no units are present we use
        // the mechInSecond GUIPref.
        ai.targetHeight = ai.targetAbsHeight = Integer.MIN_VALUE;
        for (Entity ent : boardView1.game.getEntitiesVector(dest)) {
            int trAbsheight = (ai.lowAltitude) ? ent.getAltitude() : dstHex.getLevel() + ent.relHeight();
            if (trAbsheight > ai.targetAbsHeight) {
                ai.targetHeight = ent.getHeight();
                ai.targetAbsHeight = trAbsheight;
            }
        }
        if ((ai.targetHeight == Integer.MIN_VALUE)
                && (ai.targetAbsHeight == Integer.MIN_VALUE)) {
            // Current hack for more-correct shading on low-alt maps
            ai.targetHeight = (ai.lowAltitude) ? 1 : (guip.getMechInSecond()) ? 1 : 0;
            ai.targetAbsHeight = dstHex.getLevel() + ai.targetHeight;
        }
        return LosEffects.calculateLos(boardView1.game, ai);
    }
}