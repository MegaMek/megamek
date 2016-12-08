/**
 * 
 */
package megamek.common;

/**
 * Unit roles as defined by Alpha Strike Companion, used in formation building rules
 * in ASC and Campaign Operations
 * 
 * @author Neoancient
 *
 */

public enum UnitRole {
	UNDETERMINED (false),
	AMBUSHER (true),
	BRAWLER (true),
	JUGGERNAUT (true),
	MISSILE_BOAT (true),
	SCOUT (true),
	SKIRMISHER (true),
	SNIPER (true),
	STRIKER (true),
	ATTACK_FIGHTER (false),
	DOGFIGHTER (false),
	FAST_DOGFIGHTER (false),
	FIRE_SUPPORT (false),
	INTERCEPTOR (false),
	TRANSPORT (false);

	private boolean ground;

	UnitRole(boolean ground) {
		this.ground = ground;
	}

	public boolean isGroundRole() {
		return ground;
	}

	public static UnitRole parseRole(String role) {
		switch (role.toLowerCase()) {
		case "ambusher":
			return AMBUSHER;
		case "brawler":
			return BRAWLER;
		case "juggernaut":
			return JUGGERNAUT;
		case "missile_boat":
		case "missile boat":
			return MISSILE_BOAT;
		case "scout":
			return SCOUT;
		case "skirmisher":
			return SKIRMISHER;
		case "sniper":
			return SNIPER;
		case "striker":
			return STRIKER;
		case "attack_fighter":
		case "attack figher":
		case "attack":
			return ATTACK_FIGHTER;
		case "dogfighter":
			return DOGFIGHTER;
		case "fast_dogfighter":
		case "fast dogfighter":
			return FAST_DOGFIGHTER;
		case "fire_support":
		case "fire support":
		case "fire-support":
			return FIRE_SUPPORT;
		case "interceptor":
			return INTERCEPTOR;
		case "transport":
			return TRANSPORT;
		default:
			System.err.println("Could not parse AS Role " + role);
			return UNDETERMINED;
		}
	}
	
	public boolean qualifiesForRole(AlphaStrikeElement unit) {
		switch (this) {
		case AMBUSHER:
			return unit.getPrimaryMovementValue() <= 6
				&& unit.getFinalArmor() <= 5;
		case BRAWLER:
			return unit.getPrimaryMovementValue() >= 8
				&& unit.getPrimaryMovementValue() <= 12;
		case JUGGERNAUT:
			return unit.getPrimaryMovementValue() <= 6
				&& unit.getFinalArmor() >= 7
				&& Math.max(unit.getDamage(AlphaStrikeElement.RANGE_BAND_SHORT),
						unit.getDamage(AlphaStrikeElement.RANGE_BAND_MEDIUM))* 2 >= unit.getArmor();
		case MISSILE_BOAT:
			return unit.getDamage(AlphaStrikeElement.RANGE_BAND_LONG, WeaponType.BFCLASS_LRM) > 0
					|| unit.hasSPA(BattleForceSPA.ARTAIS)
							|| unit.hasSPA(BattleForceSPA.ARTBA)
							|| unit.hasSPA(BattleForceSPA.ARTCM5)
							|| unit.hasSPA(BattleForceSPA.ARTCM7)
							|| unit.hasSPA(BattleForceSPA.ARTCM9)
							|| unit.hasSPA(BattleForceSPA.ARTCM12)
							|| unit.hasSPA(BattleForceSPA.ARTT)
							|| unit.hasSPA(BattleForceSPA.ARTLT)
							|| unit.hasSPA(BattleForceSPA.ARTTC)
							|| unit.hasSPA(BattleForceSPA.ARTSC)
							|| unit.hasSPA(BattleForceSPA.ARTLTC);
		case SCOUT:
			return unit.getPrimaryMovementValue() >= 9
				&& unit.getFinalArmor() <= 4;
		case SKIRMISHER:
			return (unit.getPrimaryMovementValue() >= 9
				|| (unit.getPrimaryMovementValue() >= 8 && unit.getMovementModes().contains("j")))
					&& unit.getFinalArmor() >= 4 && unit.getFinalArmor() <= 8;
		case SNIPER:
			return unit.getDamage(AlphaStrikeElement.LONG_RANGE)
					- unit.getDamage(AlphaStrikeElement.LONG_RANGE, WeaponType.BFCLASS_LRM) >= 1;
		case STRIKER:
			return unit.getPrimaryMovementValue() >= 9 && unit.getFinalArmor() <= 5;
		case ATTACK_FIGHTER:
			return unit.getPrimaryMovementValue() <= 5;
		case DOGFIGHTER:
			return unit.getPrimaryMovementValue() >= 5
				&& unit.getPrimaryMovementValue() >= 7;
		case FAST_DOGFIGHTER:
			return unit.getPrimaryMovementValue() >= 7
				&& unit.getPrimaryMovementValue() <= 9;
		case FIRE_SUPPORT:
			return unit.getPrimaryMovementValue() >= 5
				&& unit.getPrimaryMovementValue() <= 7
				&& unit.getDamage(AlphaStrikeElement.LONG_RANGE) >= 1;
		case INTERCEPTOR:
			return unit.getPrimaryMovementValue() >= 10;
		case TRANSPORT:
			return unit.hasSPA(BattleForceSPA.CK)
					|| unit.hasSPA(BattleForceSPA.IT)
					|| unit.hasSPA(BattleForceSPA.AT)
					|| unit.hasSPA(BattleForceSPA.PT)
					|| unit.hasSPA(BattleForceSPA.VTM)
					|| unit.hasSPA(BattleForceSPA.VTH)
					|| unit.hasSPA(BattleForceSPA.VTS)
					|| unit.hasSPA(BattleForceSPA.MT)
					|| unit.hasSPA(BattleForceSPA.CT)
					|| unit.hasSPA(BattleForceSPA.ST)
					|| (unit.hasSPA(BattleForceSPA.CT) && unit.getSPA(BattleForceSPA.CT) >= 50);
		default:
			return true;
		}
	}

	/* Convert all but initial letter(s) to lower case */
	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer();
		for (String word : name().split("_")) {
			sb.append(Character.toUpperCase(word.charAt(0)))
			.append(word.substring(1).toLowerCase()).append(" ");
		}
		return sb.toString().trim();
	}
};

