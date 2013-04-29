package quakeagent;

import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import soc.qase.tools.vecmath.Vector3f;

public class ShareData {
	private static final Vector3f groupDestination = new Vector3f();
	private static boolean calculateGroupDestination =  true;
	//private static final ReadWriteLock groupDestinationLock = new ReentrantReadWriteLock();
	//Tambien esta la palabra reservada synchronized que si la pones solo 1 hilo puede 
	//acceder al metodo en cuestion
	private final static Semaphore groupDestinationS = new Semaphore(0, true);
	private final static Semaphore groupDestinationLock = new Semaphore(1, true);
	private static int calculatePetitions = 1;
	
	public static Vector3f getGroupDestination() {
		return groupDestination;
	}
	
	public static void calculateGroupDestination() throws InterruptedException{
		groupDestinationLock.acquire();
		if( calculatePetitions == QuakeAgent.N_BOTS){
			groupDestination.set(10,15,20);
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
