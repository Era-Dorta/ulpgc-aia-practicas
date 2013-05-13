package quakeagent;

import java.util.HashMap;
import java.util.Map;
import soc.qase.state.Entity;
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
	private final static int closeDistanceMax = 50;
	private final static float closeDistanceMaxInv = (float)1.0/closeDistanceMax;
	
	//For medium range
	private final static int mediumDistanceMin = 30;	
	private final static int mediumDistanceMax = 100;
	private final static float mediumDistanceHalf = (float)((mediumDistanceMax + mediumDistanceMin)* 0.5);
	private final static float leftM = -1/(mediumDistanceMin - mediumDistanceHalf);
	private final static float leftB = mediumDistanceMin/(mediumDistanceMin - mediumDistanceHalf);
	private final static float rightM = -1/(mediumDistanceMax - mediumDistanceHalf);
	private final static float rightB = mediumDistanceMax/(mediumDistanceMax - mediumDistanceHalf);	
	
	//For large range
	private final static int largeDistanceMin = 90;
	private final static float largeDistanceMinInv = (float)1.0/largeDistanceMin;		
	private final static int largeDistanceMax = 150;		
	
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
        
        public static int getBetterWeapon( Inventory inventory, float distance )
        {
            Range preferredRange = getBetterRange( distance );
            
            
            for( int i=0; i<weaponsNames.length; i++ ){
                if( ( (inventory.getCount( weaponsNames[i] ) > 0) ) && 
                      (ranges[i] == preferredRange) ){
                    return weaponsNames[i];
                }
                   
            }
            return 0;
	}
}
