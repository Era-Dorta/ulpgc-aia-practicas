package quakeagent;

import java.util.ArrayList;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import soc.qase.tools.vecmath.Vector3f;
import soc.qase.ai.waypoint.WaypointMap;

public class ShareData {
	private static ArrayList<SimpleBot> botArray = new ArrayList<SimpleBot>();
	private static SimpleBot botLeader = null;
	private static int leaderIndex = 0;
	private static final Vector3f groupDestination = new Vector3f();
	private static final Vector3f[] botsPositions = new Vector3f[QuakeAgent.N_BOTS];
	private static final float invNBots = (float) (1.0/QuakeAgent.N_BOTS);
	//private static final ReadWriteLock groupDestinationLock = new ReentrantReadWriteLock();
	//Tambien esta la palabra reservada synchronized que si la pones solo 1 hilo puede 
	//acceder al metodo en cuestion
	private final static Semaphore groupDestinationS = new Semaphore(0, true);
	private final static Semaphore groupDestinationLock = new Semaphore(1, true);
	private final static Semaphore changeLeaderLockS = new Semaphore(0, true);
	private static int calculatePetitions = 1;
	private static WaypointMap map;
	private static BotStates groupState = BotStates.RENDEZVOUZ;

	public synchronized static void registerBot( SimpleBot bot ){
		botArray.add(bot);
		botLeader = botArray.get(leaderIndex);
	}
	
	public static SimpleBot getFirstLeader(){
		return ShareData.botLeader;
	}
	
	public static SimpleBot getLeader(){
		return ShareData.botLeader;
	}	
	
	public synchronized static void changeLeader(){
		leaderIndex = (leaderIndex + 1)%QuakeAgent.N_BOTS;
		botLeader = botArray.get(leaderIndex);
		changeLeaderLockS.release(QuakeAgent.N_BOTS - 1);
	}	
	
	public static boolean waitLeaderChange(){
		return changeLeaderLockS.tryAcquire();	
	}	
	
	public static BotStates getGroupState(){
		return ShareData.groupState;
	}
	
	public synchronized static void setGroupState( BotStates groupState ){
		ShareData.groupState = groupState;
	}
	
	public static void setMap(WaypointMap map) {
		ShareData.map = map;
	}

	public static Vector3f getGroupDestination() {
		return groupDestination;
	}
	
	public static void setGroupDestination( Vector3f groupDestination ) {
		ShareData.groupDestination.set(groupDestination);
		groupDestinationS.release(QuakeAgent.N_BOTS - 1);
	}
	
	public static boolean waitLeaderDecision(){
		return groupDestinationS.tryAcquire();	
	}
	
	public static boolean calculateGroupDestination( Vector3f botPosition, boolean semTaken ){
		if(!semTaken){
			return groupDestinationS.tryAcquire();
		}
		
		try {
			groupDestinationLock.acquire();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		botsPositions[calculatePetitions - 1] = botPosition;
		if( calculatePetitions == QuakeAgent.N_BOTS){
			//Calculate the mean on all positions
			//there is where the group will go
			groupDestination.set(0,0,0);
			for( Vector3f position: botsPositions){
				groupDestination.add(position);
			}
			System.out.println("groupDestination " + groupDestination);
			groupDestination.scale(invNBots);
			System.out.println("groupDestination " + groupDestination);
			groupDestination.set((map.findClosestWaypoint(groupDestination)).getPosition());
			System.out.println("groupDestination " + groupDestination);
			calculatePetitions = 1;
			groupDestinationLock.release();
			groupDestinationS.release(QuakeAgent.N_BOTS - 1);	
			return true;
		}else{			
			calculatePetitions++;			
			groupDestinationLock.release();
			return groupDestinationS.tryAcquire();
		}
	}	
}
