package megamek.client.bot.princess;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import megamek.common.Hex;
import megamek.common.Player;
import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.equipment.Minefield;
import megamek.common.game.Game;
import megamek.common.units.Entity;
import megamek.common.units.EntityMovementMode;
import megamek.common.units.Terrains;
import megamek.common.units.UnitType;

public class MinefieldDeploymentPlanner {
	// #1: go through list of hexes and determine suitability for mine placement
	// - separate set for wheeled/tracked/mech hostiles
	// - all: buildings, water, impassable: X
	// - wheeled: woods X
	// - tracked: light woods +1, light foliage +1, hvy woods/jungle X
	// - mech/infantry: light woods +1, hvy woods +2, hvy jungle X, foliage -1
	// construct ordered list by suitability
	// do this for each mine type
	// #2 pick random of top until mine counts 
	
	public static final Set<Integer> DISALLOWED_TERRAIN_TYPES = Set.of(
			Terrains.MAGMA,
			Terrains.BUILDING,
			Terrains.FUEL_TANK,
			Terrains.ICE,
			Terrains.IMPASSABLE,
			Terrains.RAPIDS,
			Terrains.SPACE,
			Terrains.SKY
			);
	
	public static final int LEGAL_BUT_USELESS = -1000;
	public static final int POSITIVE_BUT_LOW_VALUE = 1;
	
	private double mekProportion = 0.0;
	private double trackProportion = 0.0;
	private double wheelProportion = 0.0;
	private double hoverProportion = 0.0;
	private double infantryProportion = 0.0;
	
	public MinefieldDeploymentPlanner(Player player, Game game) {
		// would prefer to have a map of unit type to score, but tracked, wheeled and hover
		// vehicles have different movement rules while being the same unit type
		// so we settle on this somewhat awkward approach for now
		int mekCount = 0;
		int trackedVeeCount = 0;
		int wheeledVeeCount = 0;
		int infantryCount = 0;
		int hoverVeeCount = 0;
		
		// first, we collect the unit type counts
		for (Entity entity : game.getEntitiesVector()) {
			// ignore off-board and non-hostile entities
			if (entity.isOffBoard() || !entity.getOwner().isEnemyOf(player)) {
				continue;
			}
			
			if (entity.getUnitType() == UnitType.MEK) {
				mekCount++;
			} else if (entity.getUnitType() == UnitType.TANK) {
				if (entity.getMovementMode() == EntityMovementMode.TRACKED) {
					trackedVeeCount++;
				} else if (entity.getMovementMode() == EntityMovementMode.WHEELED) {
					wheeledVeeCount++;
				} else if (entity.getMovementMode() == EntityMovementMode.HOVER) {
					hoverVeeCount++;
				}
			// for now, we lump BA, Protomeks and infantry together; 
			// they're relatively low-impact/low-BV and uncommon in general usage compared
			// to meks and tanks
			} else if (entity.getUnitType() == UnitType.INFANTRY || 
					entity.getUnitType() == UnitType.BATTLE_ARMOR ||
					entity.getUnitType() == UnitType.PROTOMEK) {
				infantryCount++;
			}
		}
		
		double hostileUnitCount = mekCount + trackedVeeCount + wheeledVeeCount + infantryCount + hoverVeeCount;
		mekProportion = (double) mekCount / hostileUnitCount;
		trackProportion = (double) trackedVeeCount / hostileUnitCount;
		wheelProportion = (double) wheeledVeeCount / hostileUnitCount;
		infantryProportion = (double) infantryCount / hostileUnitCount;
		hoverProportion = (double) hoverVeeCount / hostileUnitCount;
	}
	
	/**
	 * Given a minefield type, the player, game and board
	 * Return a set of coordinates sorted into buckets by their utility score
	 * Within a bucket, the list of coordinates is randomized
	 */
	public Map<Double, List<Coords>> getBucketedCandidateCoords(int minefieldType, 
			Player player, Game game, Board board) {
		Map<Coords, Double> minefieldScores = buildCoalescedMinefieldScores(minefieldType,
				player, game, board);
		
		Map<Double, List<Coords>> bucketedCoords = new HashMap<>();
		
		for (Coords coords : minefieldScores.keySet()) {
			double scoreBucket = minefieldScores.get(coords);
			
			bucketedCoords.putIfAbsent(scoreBucket, new ArrayList<Coords>());
			bucketedCoords.get(scoreBucket).add(coords);
		}
		
		for (double bucket : bucketedCoords.keySet()) {
			Collections.shuffle(bucketedCoords.get(bucket));
		}
		
		return bucketedCoords;
	}
	
	/**
	 * Given a minefield type, player, game and board, get a sorted list of coordinates/scores
	 * @param minefieldType
	 */
	public List<Entry<Coords, Double>> getOrderedCoords(int minefieldType, Player player, Game game, Board board) {
		Map<Coords, Double> minefieldScores = buildCoalescedMinefieldScores(minefieldType,
				player, game, board);
		
		List<Entry<Coords, Double>> scoreList = new ArrayList<>(minefieldScores.entrySet());
		scoreList.sort(Entry.comparingByValue());
		
		return scoreList.reversed();
	}
	
	/**
	 * Given a minefield type (from Minefield class), build a map of coordinates and
	 * their associated scores. It's a weighted average depending on the numbers and types
	 * of units the opponent has.
	 * 
	 * Currently, we only consider meks and ground-bound vehicles. 
	 */
	public Map<Coords, Double> buildCoalescedMinefieldScores(int minefieldType, 
			Player player, Game game, Board board) {
		Map<Coords, Double> coalescedMap = new HashMap<>();		
		
		// now, we generate the map for each unit type
		Map<Coords, Integer> mekScores = new HashMap<>();
		Map<Coords, Integer> trackedVeeScores = new HashMap<>();
		Map<Coords, Integer> wheeledVeeScores = new HashMap<>();
		
		// only calculate the scores for a unit type if it's present in the opfor
		if (mekProportion > 0) {
			mekScores = getMinefieldScores(minefieldType, UnitType.MEK, EntityMovementMode.BIPED, board);
		}
		
		if (trackProportion > 0) {
			trackedVeeScores = getMinefieldScores(minefieldType, UnitType.TANK, EntityMovementMode.TRACKED, board);
		}
		
		if (wheelProportion > 0) {
			wheeledVeeScores = getMinefieldScores(minefieldType, UnitType.TANK, EntityMovementMode.WHEELED, board);
		}
		
		// now, for each coordinate, we take the weighted average
		// if a coordinate has no value, it means it is unsuitable for that unit type
		// so we treat it as a 0.
		// e.g. A * mek proportion + B * tank proportion + C * wheel proportion
		for (int x = 0; x < board.getWidth(); x++) {
			for (int y = 0; y < board.getHeight(); y++) {
				Coords coords = new Coords(x, y);
				
				double mekScore = mekScores.getOrDefault(coords, 0);
				double trackedVeeScore = trackedVeeScores.getOrDefault(coords, 0);
				double wheeledVeeScore = wheeledVeeScores.getOrDefault(coords, 0);
				
				double totalScore = mekScore * mekProportion +
						trackedVeeScore * trackProportion +
						wheeledVeeScore * wheelProportion;
				
				coalescedMap.put(coords, totalScore);
			}
		}
		
		return coalescedMap;
	}
	
	
	/**
	 * Given a mine field type (from Minefield class), a unit type (mek, tank etc), 
	 * a movement mode (e.g. walking, wheeld etc) and the board, 
	 * get a map of hexes and their suitability values for minefield placement
	 */
	public Map<Coords, Integer> getMinefieldScores(int minefieldType, int unitType,
			EntityMovementMode movementMode, Board board) {
		Map<Coords, Integer> minefieldScores = new HashMap<>();
		
		// these three types of mines only affect meks, so...
		if ((minefieldType == Minefield.TYPE_TRIPWIRE || minefieldType == Minefield.TYPE_PITFALL ||
				minefieldType == Minefield.TYPE_VIBRABOMB) && unitType != UnitType.MEK) {
			return minefieldScores;
		}
				
		for(int x = 0; x < board.getWidth(); x++) {
			for (int y = 0; y < board.getWidth(); y++) {
				Coords coords = new Coords(x, y);
				Hex hex = board.getHex(coords);
				
				// if we can't place the minefield, just move on
				if (hex.containsAnyTerrainOf(DISALLOWED_TERRAIN_TYPES)) {
					continue;
				}
				
				if (!hex.canPlaceMinefield(minefieldType)) {
					continue;
				}
				
				int hexScore = 0;
				
				switch (unitType) {
				// meks and ground tanks are really the only meaningful consideration
				// for mine placement.
				case UnitType.MEK:
					hexScore = getIndividualHexScoreForMeks(hex, board);
					break;
				case UnitType.TANK:
					if (movementMode == EntityMovementMode.TRACKED) {
						hexScore = getIndividualHexScoreForTrackedVees(hex, board);
					} else if (movementMode == EntityMovementMode.WHEELED) {
						hexScore = getIndividualHexScoreForWheeledVees(hex, board);
					}
					break;
				}
				
				if (hexScore != Integer.MIN_VALUE) {
					minefieldScores.put(coords, hexScore);
				}
			}
		}
		
		return minefieldScores;
	}
	
	/**
	 * Worker function that takes the current hex and determines how good it is for
	 * placing a minefield into when the opponent is a mek
	 */
	public int getIndividualHexScoreForMeks(Hex hex, Board board) {
		int hexScore = 0;
		
		// meks are pretty likely to go into woods for cover
		// ultra-heavy is prohibited
		if (hex.containsTerrain(Terrains.WOODS)) {
			switch (hex.terrainLevel(Terrains.WOODS)) {
				case 3: 
					return Integer.MIN_VALUE;
				default:
					hexScore += hex.terrainLevel(Terrains.WOODS);
					break;
			}
		}
		
		// meks are pretty likely to go into jungle for cover
		// ultra-heavy is prohibited
		if (hex.containsTerrain(Terrains.JUNGLE)) {
			switch (hex.terrainLevel(Terrains.JUNGLE)) {
				case 3: 
					return Integer.MIN_VALUE;
				default:
					hexScore += hex.terrainLevel(Terrains.JUNGLE);
					break;
			}
		}
		
		// terrain that causes PSRs for no cover benefit is less likely
		for (int terrain : hex.getTerrainTypes()) {
			if (Terrains.HAZARDS_WITH_BLACK_ICE.contains(terrain)) {
				hexScore -= hex.terrainLevel(terrain);
			}
		}
		
		// terrain that costs extra to move into/through for no cover benefit is less likely
		// example: foliage, rough, etc.
		if (hex.vegetationCeiling() == 1) {
			hexScore--;
		}
		
		if (hex.containsTerrain(Terrains.ROUGH)) {
			hexScore -= hex.terrainLevel(Terrains.ROUGH);
		}
		
		// it's fun to put mines on bridges
		if (hex.containsAnyTerrainOf(Terrains.BRIDGE)) {
			hexScore++;
		}
		
		// calculate the partial cover bonus for this hex
		// if we're really low relative to the rest of the map, you're probably not getting it
		// if we're really high relative to the rest of the map you're probably getting it
		int hexFloor = hex.floor();
		int minElevation = board.getMinElevation();
		int maxElevation = board.getMaxElevation();
		double hexElevationCoverMultiplier = (double) (hexFloor - minElevation) / 
				(double) (maxElevation - minElevation);
		int partialCoverBonus = (int) (2.0 * hexElevationCoverMultiplier);
		
		// terrain next to partial cover is actually pretty good
		// as is terrain that one can completely hide behind
		// a very rough estimation of partial cover as a full calculation of this is super expensive
		for (int direction = 0; direction < 6; direction++) {
			Hex neighborHex = board.getHexInDir(hex.getCoords(), direction);
			
			if (neighborHex == null) {
				continue;
			}
			
			int neighborHexFloor = neighborHex.floor();
			int neighborHexCeiling = neighborHex.ceiling();
			
			// partial cover bonus: buildings that go to this hex floor + 1 
			if (neighborHex.containsTerrain(Terrains.BLDG_ELEV) &&
					neighborHexCeiling == hexFloor + 1) {
				hexScore += partialCoverBonus;
			}
			// full cover bonus: buildings that go to this hex floor + 2 or higher
			else if (neighborHex.containsTerrain(Terrains.BLDG_ELEV) &&
					neighborHexCeiling > hexFloor + 1) {
				hexScore++;
			}
			
			// partial cover bonus: terrain level goes to this hex floor + 1
			if (neighborHexFloor == hexFloor + 1) {
				hexScore += partialCoverBonus;
			// full cover bonus: terrain level goes to this hex floor + 2 or higher
			} else if (neighborHexFloor > hexFloor + 1) {
				hexScore++;
			}
		}
		
		return hexScore;
	}
	
	/**
	 * Worker function that takes the current hex and determines how good it is for
	 * placing a minefield into when the opponent is a tracked vehicle
	 */
	public int getIndividualHexScoreForTrackedVees(Hex hex, Board board) {
		int hexScore = 0;
		
		// tanks can't go into water
		if (hex.hasDepth1WaterOrDeeper()) {
			return Integer.MIN_VALUE;
		}
		
		// tanks can only go into light woods
		if (hex.containsTerrain(Terrains.WOODS)) {
			if (hex.terrainLevel(Terrains.WOODS) >= 2) {
				return Integer.MIN_VALUE;
			} else {
				hexScore++;
			}
		}
		
		// tanks can't go into the jungle
		if (hex.containsTerrain(Terrains.JUNGLE)) {
			return Integer.MIN_VALUE;
		}
		
		// terrain that causes PSRs for no cover benefit is less likely
		for (int terrain : hex.getTerrainTypes()) {
			if (Terrains.HAZARDS_WITH_BLACK_ICE.contains(terrain)) {
				hexScore -= hex.terrainLevel(terrain);
			}
		}
		
		// terrain that costs extra to move into/through for no cover benefit is less likely
		// example: rough, etc.		
		if (hex.containsTerrain(Terrains.ROUGH)) {
			hexScore -= hex.terrainLevel(Terrains.ROUGH);
		}
		
		// vehicles like going on pavement because it makes them go faster
		if (hex.containsAnyTerrainOf(Terrains.ROAD, Terrains.PAVEMENT, Terrains.BRIDGE)) {
			hexScore++;
		}
		
		int hexFloor = hex.floor();
		
		// terrain next to higher elevations is good to hide in
		for (int direction = 0; direction < 6; direction++) {
			Hex neighborHex = board.getHexInDir(hex.getCoords(), direction);
			
			if (neighborHex == null) {
				continue;
			}
			
			int neighborHexFloor = neighborHex.floor();
			int neighborHexCeiling = neighborHex.ceiling();
			
			// cover bonus: buildings that go to this hex floor + 1 
			if (neighborHex.containsTerrain(Terrains.BLDG_ELEV) &&
					neighborHexCeiling > hexFloor) {
				hexScore++;
			}
			
			// partial cover bonus: terrain level goes to this hex floor + 1
			if (neighborHexFloor > hexFloor) {
				hexScore++;
			}
		}
		
		return hexScore;
	}
	
	/**
	 * Worker function that takes the current hex and determines how good it is for
	 * placing a minefield into when the opponent is a wheeled vehicle
	 */
	public int getIndividualHexScoreForWheeledVees(Hex hex, Board board) {
		int hexScore = 0;
		
		// wheeled vehicles can't go into a whole bunch of terrain
		if (hex.hasDepth1WaterOrDeeper() ||	hex.hasVegetation() ||
				hex.containsAnyTerrainOf(Terrains.ROUGH, Terrains.RUBBLE)) {
			return Integer.MIN_VALUE;
		}
		
		// terrain that causes PSRs for no cover benefit is less likely
		for (int terrain : hex.getTerrainTypes()) {
			if (Terrains.HAZARDS_WITH_BLACK_ICE.contains(terrain)) {
				hexScore -= hex.terrainLevel(terrain);
			}
		}
		
		// vehicles like going on pavement because it makes them go faster
		if (hex.containsAnyTerrainOf(Terrains.ROAD, Terrains.PAVEMENT, Terrains.BRIDGE)) {
			hexScore++;
		}
		
		int hexFloor = hex.floor();
		
		// terrain next to higher elevations is good to hide in
		for (int direction = 0; direction < 6; direction++) {
			Hex neighborHex = board.getHexInDir(hex.getCoords(), direction);
			
			if (neighborHex == null) {
				continue;
			}
			
			int neighborHexFloor = neighborHex.floor();
			int neighborHexCeiling = neighborHex.ceiling();
			
			// LOS block/chokepoint bonus: buildings that go to this hex floor + 1 
			if (neighborHex.containsTerrain(Terrains.BLDG_ELEV) &&
					neighborHexCeiling > hexFloor) {
				hexScore++;
			}
			
			// partial cover bonus: terrain level goes to this hex floor + 1
			if (neighborHexFloor > hexFloor) {
				hexScore++;
			}
		}
		
		return hexScore;
	}
	
	/**
	 * Worker function that takes the current hex and determines how good it is for
	 * placing a minefield into when the opponent is a mek
	 * 
	 * Note that infantry is inherently less valuable BV-wise,
	 * so we cap the hex scores at 1.
	 */
	public int getIndividualHexScoreForInfantry(Hex hex, Board board) {
		int hexScore = 0;
		
		// most infantry can't go into water
		if (hex.hasDepth1WaterOrDeeper() || hex.containsAnyTerrainOf(Terrains.FIRE, 
				Terrains.MAGMA, Terrains.HAZARDOUS_LIQUID)) {
			return Integer.MIN_VALUE;
		}
		
		// infantry are pretty likely to go into woods for cover
		if (hex.containsTerrain(Terrains.WOODS)) {
			hexScore += hex.terrainLevel(Terrains.WOODS);
		}
		
		// infantry are pretty likely to go into jungle for cover
		if (hex.containsTerrain(Terrains.JUNGLE)) {
			hexScore += hex.terrainLevel(Terrains.JUNGLE);
		}
		
		// infantry don't like being in open terrain because of the 2x damage
		if (hex.isClearHex()) {
			hexScore--;
		}
		
		int hexFloor = hex.floor();
		
		// terrain next to higher elevation is actually pretty good
		// a very rough estimation of partial cover as a full calculation of this is super expensive
		for (int direction = 0; direction < 6; direction++) {
			Hex neighborHex = board.getHexInDir(hex.getCoords(), direction);
			
			if (neighborHex == null) {
				continue;
			}
			
			int neighborHexFloor = neighborHex.floor();
			int neighborHexCeiling = neighborHex.ceiling();
			
			// full cover bonus: buildings that go to this hex floor + 2 or higher
			if (neighborHex.containsTerrain(Terrains.BLDG_ELEV) &&
					neighborHexCeiling > hexFloor) {
				hexScore++;
			}
			
			// full cover bonus: terrain level goes to this hex floor + 2 or higher
			if (neighborHexFloor > hexFloor) {
				hexScore++;
			}
		}
		
		if (hexScore > 0) {
			return POSITIVE_BUT_LOW_VALUE;
		} else {
			return hexScore;
		}
	}
	
	// TODO:
	// X mek partial cover bonus reduction based on how low we are
	// bonus for distance from nearest deployment zone
	// X wheeled vehicle map
	// infantry map
	// hover tank map
	// X weighted average for known opponent unit counts
	// actual placement by bot
	// appropriately set vibrabom threshold
}
