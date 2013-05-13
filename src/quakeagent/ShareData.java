package quakeagent;

import java.util.ArrayList;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import soc.qase.tools.vecmath.Vector3f;
import soc.qase.ai.waypoint.WaypointMap;

public class ShareData {
    // List of team members.
    private static ArrayList<SimpleBot> botArray = new ArrayList<SimpleBot>();
    
    // Team leader and its index in previous list.
    private static SimpleBot botLeader = null;
    private static int leaderIndex = 0;
    
    // Position where the team tries to get together.
    private static final Vector3f groupDestination = new Vector3f();
    
    // Positions of all team members.
    private static final Vector3f[] botsPositions = new Vector3f[QuakeAgent.N_BOTS];
    
    // Inverse of team size. It is used when calculating the mean position of
    // all bots.
    private static final float invNBots = (float) (1.0/QuakeAgent.N_BOTS);
    
    //private static final ReadWriteLock groupDestinationLock = new ReentrantReadWriteLock();
    //Tambien esta la palabra reservada synchronized que si la pones solo 1 hilo puede 
    //acceder al metodo en cuestion
    private final static Semaphore groupDestinationS = new Semaphore(0, true);
    private final static Semaphore groupDestinationLock = new Semaphore(1, true);
    private final static Semaphore changeLeaderLockS = new Semaphore(0, true);
    
    
    private static int calculatePetitions = 1;
    
    // Waypoints map shared by all the team.
    private static WaypointMap map;
    
    // All the team shares a group state, so they all acts as one.
    private static BotStates groupState = BotStates.RENDEZVOUZ;
    
    // 
    private static int waitMaxCount = 50*QuakeAgent.N_BOTS;

    
    /***
     * Adds a bot to the team.
     * @param bot : bot to be added.
     */
    public synchronized static void registerBot( SimpleBot bot ){
        botArray.add(bot);
        botLeader = botArray.get(leaderIndex);
    }

    
    /***
     * Get the current team's leader.
     */
    public static SimpleBot getLeader(){
            return ShareData.botLeader;
    }	

    
    /***
     * Nottify to the team that a team member has died. The team will try to
     * reunite again.
     */
    public synchronized static void botDied(){
        groupState = BotStates.RENDEZVOUZ;
        for( SimpleBot bot: botArray ){
            bot.friendDied();
        }
    }

    
    /***
     * Change the current team leader.
     */
    public synchronized static void changeLeader(){
        // Rotate the current team leader.
        leaderIndex = (leaderIndex + 1)%QuakeAgent.N_BOTS;
        botLeader = botArray.get(leaderIndex);
        
        // 
        changeLeaderLockS.release(QuakeAgent.N_BOTS - 1);
        waitMaxCount = 25*QuakeAgent.N_BOTS;
    }	

    public static boolean waitLeaderChange(){
        if(waitMaxCount == 0){
            //Reached maximum wait time, force changeLeader
            //Change the leader
            changeLeader();			
            //Update bot states
            for( SimpleBot bot: botArray ){
                    bot.leaderForcedChanged();
            }
        }
        waitMaxCount--;
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
        waitMaxCount = 25*QuakeAgent.N_BOTS;
    }

    public static boolean waitLeaderDecision(){
            if(waitMaxCount == 0){
                    //Reached maximum wait time, force changeLeader
                    //Change the leader
                    changeLeader();			
                    //Update bot states
                    for( SimpleBot bot: botArray ){
                            bot.leaderForcedChanged();
                    }
            }
            waitMaxCount--;		
            return groupDestinationS.tryAcquire();	
    }

    public static boolean calculateGroupDestination( Vector3f botPosition, boolean semTaken ){
            System.out.println("calculate pos semtaken " + semTaken + " calculatePetitions "  + calculatePetitions + " groupDestinationS " + groupDestinationS.availablePermits());
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
