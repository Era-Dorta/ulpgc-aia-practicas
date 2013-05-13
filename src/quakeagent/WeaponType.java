package quakeagent;

public class WeaponType {
	private static Range r;
	//For close range
	private final static int closeDistanceMax = 50;
	private final static float closeDistanceMaxInv = (float)1.0/closeDistanceMax;
	
	//For medium range
	private final static int mediumDistanceMax = 30;
	private final static float mediumDistanceMaxInv = (float)1.0/mediumDistanceMax;	
	private final static int mediumDistanceMin = 100;
	private final static float mediumDistanceMinInv = (float)1.0/mediumDistanceMin;	
	private final static float mediumDistanceHalf = (float)((mediumDistanceMax + mediumDistanceMin)* 0.5);
	private final static float mediumDistanceHalfInv = (float)1.0/mediumDistanceHalf;
	private final static float leftM = -1/(mediumDistanceMin - mediumDistanceHalf);
	private final static float leftB = mediumDistanceMin/(mediumDistanceMin - mediumDistanceHalf);
	private final static float rightM = -1/(mediumDistanceMax - mediumDistanceHalf);
	private final static float rightB = mediumDistanceMax/(mediumDistanceMax - mediumDistanceHalf);	
	
	//For large range
	private final static int largeDistanceMin = 90;
	private final static float largeDistanceMinInv = (float)1.0/largeDistanceMin;		
	private final static int largeDistanceMax = 150;			
	
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
	
	public static Range getBetterRange( float distance ){
		float isClose = isClose(distance);
		float isMedium = isMedium(distance);
		float isLong = isLong(distance);
		System.out.println("isClose " +  isClose + " isMedium " + isMedium + " isLong " + isLong);
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
}
