package quakeagent;

import java.util.HashMap;
import java.util.Map;
import soc.qase.state.Inventory;
import soc.qase.state.PlayerGun;

public class WeaponType {
	
	private static int[] weaponsNames = { PlayerGun.GRENADE_LAUNCHER, PlayerGun.GRENADES , PlayerGun.CHAINGUN
			     , PlayerGun.HYPERBLASTER , PlayerGun.MACHINEGUN , PlayerGun.RAILGUN , PlayerGun.ROCKET_LAUNCHER  , PlayerGun.SHOTGUN
			     , PlayerGun.SUPER_SHOTGUN, PlayerGun.BLASTER, PlayerGun.BFG10K }; 
	
	private static Range[] ranges = { Range.MEDIUM_RANGE, Range.MEDIUM_RANGE, Range.MEDIUM_RANGE
		     , Range.MEDIUM_RANGE , Range.MEDIUM_RANGE , Range.LONG_RANGE , Range.LONG_RANGE  , Range.CLOSE_RANGE
		     , Range.CLOSE_RANGE, Range.MEDIUM_RANGE, Range.MEDIUM_RANGE };
	
    // Hash of weapons names with its ranges
    private final static Map<Integer, Range > weaponRanges = new HashMap<Integer, Range>();

    //For close range
	private final static int closeDistanceMax = 300;
	private final static float closeDistanceMaxInv = (float)1.0/closeDistanceMax;
	
	//For medium range
	private final static int mediumDistanceMin = 200;	
	private final static int mediumDistanceMax = 500;
	private final static float mediumDistanceHalf = (float)((mediumDistanceMax + mediumDistanceMin)* 0.5);
	private final static float leftM = -1/(mediumDistanceMin - mediumDistanceHalf);
	private final static float leftB = mediumDistanceMin/(mediumDistanceMin - mediumDistanceHalf);
	private final static float rightM = -1/(mediumDistanceMax - mediumDistanceHalf);
	private final static float rightB = mediumDistanceMax/(mediumDistanceMax - mediumDistanceHalf);	
	
	//For large range
	private final static int largeDistanceMin = 400;
	private final static float largeDistanceMinInv = (float)1.0/largeDistanceMin;		
	private final static int largeDistanceMax = 600;		
	
	public static void init(){
		for(int i = 0; i < weaponsNames.length; i++ ){
			weaponRanges.put(weaponsNames[i], ranges[i]);
		}
	}
	
	public static Range getWeaponranges( int weapon ) {
		return weaponRanges.get(weapon);
	}	
	
	//Fuzzy logic methods that return how much a distance belongs to a given set
	private static float isClose( float distance ){
		if(distance > closeDistanceMax){
			//If bigger than closeDistanceMax, is not closeRange
			return 0;
		}else{
			//The closer the distance the more it belongs
			//to the close set
			return (float)((-distance + closeDistanceMax)*closeDistanceMaxInv);
		}
	}
	
	private static float isMedium( float distance ){
		if(distance > mediumDistanceMax || distance < mediumDistanceMin){
			//If bigger than max or smaller than min is not medium range
			return 0;
		}else{
			//Left part of the triangle
			if(distance < mediumDistanceHalf){
				return (float)(leftM*distance + leftB);
			}else{
			//Right part of the triangle
				return (float)(rightM*distance + rightB);
			}
		}
	}	
	
	private static float isLong( float distance ){
		if(distance < largeDistanceMin){
			//If bigger than closeDistanceMax, is not closeRange
			return 0;
		}else{
			if(distance > largeDistanceMax){
				return 1;
			}else{
				//The longer the distance the more it belongs
				//to the long set
				return (float)((distance - largeDistanceMin)*largeDistanceMinInv);
			}
		}
	}	
	
	//Given a distance returns if it is from close, medium or long range
	public static Range getBetterRange( float distance ){
		float isClose = isClose(distance);
		float isMedium = isMedium(distance);
		float isLong = isLong(distance);
		if(isClose > isMedium){
			if(isClose > isLong){
				return Range.CLOSE_RANGE;
			}else{
				return Range.LONG_RANGE;
			}
		}else{
			if(isMedium > isLong){
				return Range.MEDIUM_RANGE;
			}else{
				return Range.LONG_RANGE;
			}			
		}		
	}
        
        public static int getBetterWeapon( float distance, Inventory inventory )
        {
            Range preferredRange = getBetterRange( distance );
            int preferredWeapon = -1;

            // Search in the inventory for a weapon with the preferred range.
            for( int i=0; i<weaponsNames.length; i++ ){
                if( (inventory.getCount( weaponsNames[i] ) > 0) ){
                    if( ranges[i] == preferredRange ){
                        // We have a weapon with the desired range. Choose it.
                        System.out.println( "Preferred weapon (good range): " + weaponsNames[i] );
                        return weaponsNames[i];
                    }else{
                        /* 
                         * Save the current weapon, so if we don't find a 
                         * weapon with the desired range, at least we could
                         * use this one instead of Blaster.
                         */
                        preferredWeapon = weaponsNames[i];
                        System.out.println( "Preferred weapon (bad range): " + weaponsNames[i] );
                    }
                }
                
            }
            
            /*
             * If we didn't found a weapon with the desired range, we try to
             * use a weapon different than BLASTER.
             */
            if( preferredWeapon != -1 ){
                System.out.println( "Preferred weapon (bad range): " + preferredWeapon );
                return preferredWeapon;
            }else{
                System.out.println( "\"Preferred weapon\": BLASTER" );
                return PlayerGun.BLASTER;
            }
	}
}
