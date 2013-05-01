package quakeagent;

import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import soc.qase.tools.vecmath.Vector3f;
import soc.qase.ai.waypoint.WaypointMap;

public class ShareData {
	private static final Vector3f groupDestination = new Vector3f();
	private static final Vector3f[] botsPositions = new Vector3f[QuakeAgent.N_BOTS];
	private static final float invNBots = (float) (1.0/QuakeAgent.N_BOTS);
	//private static final ReadWriteLock groupDestinationLock = new ReentrantReadWriteLock();
	//Tambien esta la palabra reservada synchronized que si la pones solo 1 hilo puede 
	//acceder al metodo en cuestion
	private final static Semaphore groupDestinationS = new Semaphore(0, true);
	private final static Semaphore groupDestinationLock = new Semaphore(1, true);
	private static int calculatePetitions = 1;
	private static WaypointMap map;
	
	public static void setMap(WaypointMap map) {
		ShareData.map = map;
	}

	public static Vector3f getGroupDestination() {
		return groupDestination;
	}
	
	public static void calculateGroupDestination( Vector3f botPosition ) throws InterruptedException{
		groupDestinationLock.acquire();
		botsPositions[calculatePetitions - 1] = botPosition;
		if( calculatePetitions == QuakeAgent.N_BOTS){
			//Calculate the mean on all positions
			//there is where the group will go
			groupDestination.set(0,0,0);
			for( Vector3f position: botsPositions){
				groupDestination.add(position);
			}
			groupDestination.scale(invNBots);
			groupDestination.set((map.findClosestWaypoint(groupDestination)).getPosition());
			calculatePetitions = 1;
			groupDestinationLock.release();
			groupDestinationS.release(QuakeAgent.N_BOTS - 1);			
		}else{			
			calculatePetitions++;			
			groupDestinationLock.release();
			groupDestinationS.acquire();
		}
	}	
}
