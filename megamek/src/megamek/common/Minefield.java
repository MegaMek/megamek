package megamek.common;

import java.io.*;

public class Minefield implements Serializable, Cloneable {
	public static final int TYPE_CONVENTIONAL 		= 0;
	public static final int TYPE_COMMAND_DETONATED 	= 1;
	public static final int TYPE_VIBRABOMB 			= 2;
	public static final int TYPE_THUNDER 			= 3;

	public static final int TRIGGER_NONE 			= 0;
	
	public static final int CLEAR_NUMBER_WEAPON				= 5;
	public static final int CLEAR_NUMBER_INFANTRY			= 10;
	public static final int CLEAR_NUMBER_INFANTRY_ACCIDENT	= 5;

	public static final int TO_HIT_SIDE 			= ToHitData.SIDE_FRONT;
	public static final int TO_HIT_TABLE 			= ToHitData.HIT_KICK;

	public static final int MAX_DAMAGE 				= 20;

	public static final String IMAGE_FILE 			= "data/hexes/minefieldsign.gif";

	private Coords coords = null;
	private int playerId = Player.PLAYER_NONE;
	private int damage = 0;
	private int secondaryDamage = 0;
	private int setting = 0;
	private int trigger = TRIGGER_NONE;
	private int type = -1;
	private boolean areaEffect = false;
	private boolean oneUse = false;
	
	private Minefield() {
	}
	
	public static Minefield createConventionalMF(Coords coords, int playerId) {
		Minefield mf = new Minefield();
		
		mf.damage = 6;
		mf.type = TYPE_CONVENTIONAL;
		mf.trigger = 7;
		mf.coords = coords;
		mf.playerId = playerId;
		return mf;
	}
	
	public static Minefield createCommandDetonatedMF(Coords coords, int playerId) {
		Minefield mf = new Minefield();
		
		mf.damage = 10;
		mf.secondaryDamage = 4;
		mf.areaEffect = true;
		mf.oneUse = true;
		mf.type = TYPE_COMMAND_DETONATED;
		mf.coords = coords;
		mf.playerId = playerId;
		return mf;
	}
	
	public static Minefield createVibrabombMF(Coords coords, int playerId, int setting) {
		Minefield mf = new Minefield();
		
		mf.damage = 10;
		mf.areaEffect = true;
		mf.oneUse = true;
		mf.setting = setting;
		mf.type = TYPE_VIBRABOMB;
		mf.coords = coords;
		mf.playerId = playerId;
		return mf;
	}
	
	public static Minefield createThunderMF(Coords coords, int playerId, int damage) {
		Minefield mf = new Minefield();
		
		mf.damage = damage;
		mf.type = TYPE_THUNDER;
		mf.trigger = 7;
		mf.coords = coords;
		mf.playerId = playerId;
		return mf;
	}
	
	public Object clone() {
		Minefield mf = new Minefield();
		
		mf.playerId = playerId;
		mf.coords = coords;
		mf.damage = damage;
		mf.secondaryDamage = secondaryDamage;
		mf.areaEffect = areaEffect;
		mf.oneUse = oneUse;
		mf.setting = setting;
		mf.type = type;
		
		return mf;
	}

    public boolean equals(Object object) {
    	Minefield mf;
    	try {
	    	mf = (Minefield) object;
    	} catch (Exception e) {
    		return false;
    	}
    	
    	if (
			mf.playerId == this.playerId &&
			mf.coords.equals(coords) &&
			mf.type == this.type) {
			return true;
		} else {
			return false;
		}
    }
    
	public void setDamage(int damage) {
		this.damage = damage;
	}
	
	public Coords getCoords() {
		return coords;
	}

	public int getDamage() {
		return damage;
	}

	public int getSecondaryDamage() {
		return secondaryDamage;
	}

	public int getTrigger() {
		return trigger;
	}

	public boolean isAreaEffect() {
		return areaEffect;
	}

	public boolean isOneUse() {
		return oneUse;
	}

	public int getSetting() {
		return setting;
	}

	public int getType() {
		return type;
	}

	public int getPlayerId() {
		return playerId;
	}

}
