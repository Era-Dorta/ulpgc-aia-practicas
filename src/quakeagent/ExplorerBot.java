package quakeagent;

import java.io.IOException;
import java.util.Vector;
import java.util.Random;
import java.util.HashMap;
import java.util.Map;

import java.util.logging.Level;
import java.util.logging.Logger;
import soc.qase.bot.ObserverBot;
import soc.qase.file.bsp.BSPParser;
import soc.qase.state.Origin;
import soc.qase.state.Player;
import soc.qase.state.PlayerMove;
import soc.qase.state.World;
import soc.qase.tools.vecmath.Vector3f;

import soc.qase.state.*;

import java.lang.Math;
import jess.*;
import soc.qase.ai.waypoint.Waypoint;
import soc.qase.ai.waypoint.WaypointMap;
import soc.qase.file.bsp.BSPBrush;

/*
 * Every bot extends ObserverBot class.
 */
public final class ExplorerBot extends ObserverBot
{
    //Variables 
    private World world = null;
    private Player player = null;

    private Vector3f posPlayer = new Vector3f(0, 0, 0);

    // Bot previous position.
    private Vector3f prevPosPlayer = new Vector3f(0, 0, 0);

    // Bot destination.
    private Origin destination = new Origin(0, 0, 0);
    
    // Bot movement.
    private int nDirectionChanges = 0;
    
    // Environment information.
    private BSPParser mibsp = null; 
    
    //When true bot did not found a path so go back
    //to where we were
    private boolean goBack = false;
    
    //The bot is following a path
    private boolean inPath = false;
    

    private double aimx = 0.0001, aimy = 1, aimz = 0, velx = 0.0001 ,vely = 1,
            velz = 0.0001, prevVelX= 0.0001, prevVelY = 0.0001;

    private int currentWayPoint = 0;
    
    private boolean waypointDeleted = false; 

    int dire = 0;
    
    //Path to a given point
    private Waypoint [] path;
    
    //Path to a given point
    private Waypoint [] prevPath;
    
    //How many frames since the bot last moved
    private int framesWithoutMove = 0;
    
    //Set a certain delay when actualizing bot position
    //to better detec when the bot has not moved
    private int delayActualizeBotPosition = 0;
    /***
     * Constructor. Set the bot's name and look.
     * @param botName : bot name.
     * @param botSkin : bot skin.
     ***/
    public ExplorerBot(String botName, String botSkin)
    {
            super((botName == null ? "MiBotseMueve" : botName), botSkin);
            initBot();
    }


    /***
     * Constructor. Set the bot's name and look. It also set
     * whether the bot manually track its inventory or not.
     * @param botName : bot name.
     * @param botSkin : bot skin.
     * @param trackInv : if true, bot will manually track it's inventory.
     ***/
    public ExplorerBot(String botName, String botSkin, boolean trackInv)
    {
            super((botName == null ? "MiBotseMueve" : botName), botSkin, trackInv);
            initBot();
    }


    /***
     * Constructor.
     * @param botName : bot name.
     * @param botSkin : bot skin.
     * @param highThreadSafety : if true, bot will use a thread in safe 
     * mode.
     * @param trackInv : if true, bot will manually track it's inventory.
     ***/
    public ExplorerBot(String botName, String botSkin, boolean highThreadSafety, boolean trackInv)
    {
            super((botName == null ? "MiBotseMueve" : botName), botSkin, highThreadSafety, trackInv);
            initBot();
    }


    /***
     * Constructor.
     * @param botName : bot name.
     * @param botSkin : bot skin.
     * @param password : server password.
     * @param highThreadSafety : if true, bot will use a thread in safe 
     * mode.
     * @param trackInv : if true, bot will manually track it's inventory.
     ***/
    public ExplorerBot(String botName, String botSkin, String password, boolean highThreadSafety, boolean trackInv)
    {
            super((botName == null ? "MiBotseMueve" : botName), botSkin, password, highThreadSafety, trackInv);
            initBot();
    }


    /***
     * Constructor.
     * @param botName : bot name.
     * @param botSkin : bot skin.
     * @param recvRate : communication ratio.
     * @param msgLevel : messages type.
     * @param fov : bot line of vision 
     * @param hand : indicates in which hand the bot has the weapon.
     * @param password : server password.
     * @param highThreadSafety : if true, bot will use a thread in safe 
     * mode.
     * @param trackInv : if true, bot will manually track it's inventory. 
     ***/
    public ExplorerBot(String botName, String botSkin, int recvRate, int msgLevel, int fov, int hand, String password, boolean highThreadSafety, boolean trackInv)
    {
            super((botName == null ? "MiBotseMueve" : botName), botSkin, recvRate, msgLevel, fov, hand, password, highThreadSafety, trackInv);
            initBot();
    }

    
    public void setMap(WaypointMap map)
    {
        this.wpMap = map;
    }    
    
    public WaypointMap getMap(){
    	return this.wpMap;
    }

    /***
     * Bot initialization.
     ***/
    private void initBot()
    {	
        // Inventory auto refresh.
        this.setAutoInventoryRefresh(true);
        
    }   

    /***
     * Main bot AI algorithm. 
     * @param w : Game current state.
     ***/
    public void runAI(World w)
    {
        if (mibsp==null){
            mibsp = this.getBSPParser();
        }

        System.out.println("AI...\n" + rand() );
        // Retrive game current state.
        world = w;

        // Get information about the bot.
        player = world.getPlayer();
              
        posPlayer = player.getPosition().toVector3f(); 
        
        //Tell the bot not to move, standard action    
        Vector3f DirMov = new Vector3f(velx, vely, velz);
        Vector3f aim = new Vector3f(aimx, aimy, aimz);        
        setBotMovement(DirMov, aim, 0, PlayerMove.POSTURE_NORMAL);
             
        
        // Decide a movement direction.
        setMovementDir();

        // Print information about the bot's state.
        //printState();

        // Get the distance to the nearest obstacle in the direction
        // the bot moves to.
        getObstacleDistance();
    }
    
    public static int rand() {
    	int max = 2000;
    	int min = 2000;
        int ii = -min + (int) (Math.random() * ((max - (-min)) + 1));
        return ii;
    }

    /***
     * Decide in which direction the bot will move.
     ***/
    private void setMovementDir()
    {
        if(!inPath){
        	prevPath = path;
        	this.sendConsoleCommand( this.getPlayerInfo().getName() + " new path");	
        	System.out.println( "findShortestPathTo to "  ); 
        	//If the waypoint was deleted the bot went back to where it was, now we 
        	//want to go again to previous position to see if the correction work out
        	if(!waypointDeleted){
        		//int aux[] = {rand(),rand(),rand()};
        		int aux[] = {-1191,1506,569};
        		destination.setXYZ(aux);
        	}else{
        		waypointDeleted = false;
        	}
            path = findShortestPath( destination );
            System.out.println( "findShortestPathToItem 2" );                  	                
                    	
            if(path == null || path.length == 0){
         	   if(prevPath != null){
         		   this.sendConsoleCommand( this.getPlayerInfo().getName() + "Noo waypoints going back");	                		  
         		   goBack = true;
         		   path = prevPath;
         	   }else{
                	   try {
                		   System.out.println("No waypoint, we are fucked");
                		   System.in.read();
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
         	   }
            }else{
            	currentWayPoint = 0;	                                	
            }
            inPath = true;
        	
        }else{
           if( posPlayer.distance(path[currentWayPoint].getPosition()) < 25 ){
        	   if(!goBack){
        		   if( currentWayPoint < path.length - 1){
        			   currentWayPoint++;
        		   }else{
                       //Bot reached destination
                       inPath = false;  
        		   }
        	   }else{
        		   if( currentWayPoint > 0){
        			   currentWayPoint--;
        		   }else{
                       //Bot reached destination
                       inPath = false;  
                       goBack = false;
                       //currentWayPoint = 0;
        		   }
        	   }
           }            
        }
        System.out.printf("Soy " + getPlayerInfo().getName() + " Voy en direccion %f %f el currentway es %d el total es %d \n", velx, vely, currentWayPoint, path.length);
        System.out.printf("Estoy en %f %f %f voy a %f %f %f \n", posPlayer.x,posPlayer.y,posPlayer.z,path[currentWayPoint].getPosition().x,
        		path[currentWayPoint].getPosition().y, path[currentWayPoint].getPosition().z);
        velx = path[currentWayPoint].getPosition().x - posPlayer.x;
        vely = path[currentWayPoint].getPosition().y - posPlayer.y;
        velz = path[currentWayPoint].getPosition().z - posPlayer.z;           
        Vector3f DirMov = new Vector3f(velx, vely, velz);
        //Set aim in the same direction as the bot moves
        Vector3f aim = new Vector3f(velx, vely, velz);
        setBotMovement(DirMov, aim, 200, PlayerMove.POSTURE_NORMAL); 
        aimx = aim.x;
        aimy = aim.y;
        aimz = aim.z;     
        

      //Distance the bot moved
      double dist = Math.sqrt(Math.pow(prevPosPlayer.y - player.getPlayerMove().getOrigin().getY(),2)+
      Math.pow(prevPosPlayer.x - player.getPlayerMove().getOrigin().getX(),2));
      System.out.println("Distance from last position " + dist);
      System.out.println("framesWithoutMove " + framesWithoutMove);
      //Distance is low 
      if (dist < 50 && framesWithoutMove>0)
      {
    	  System.out.println("Contando dentro de dist < 10");
    	  framesWithoutMove++;
	
	      //If it is the 10th time we do not move
	      if (framesWithoutMove>30)
	      {
	          //Explorer bot will modify the map to make it better, so
	          //unlock it to allow the bot to change its nodes
	          wpMap.unlockMap();
	          
		      framesWithoutMove=1;
		      this.sendConsoleCommand( this.getPlayerInfo().getName() + " did not move, deleting waypoint");	
		      //Delete current node
		      if(!wpMap.deleteNode(path[currentWayPoint])){
		    	  System.out.println("Could not erase waypoint");
		      }
		      //Create a new waypoint in current position
		      Waypoint newWaypoint = new Waypoint(posPlayer);
		      
		      //TODO SHOULD ADD MORE EDGES
		      //First idea
		      //Add an edge to the closes waypoint, we can assume we came from there
		      //newWaypoint.addEdge(wpMap.findClosestWaypoint(posPlayer));
		      
		      //Second idea
		      //Conect the new waypoint to the previous and next in the path
		      if(currentWayPoint - 1 > 0){
		    	  newWaypoint.addEdge(path[currentWayPoint - 1]);
		      }
		      
		      if(currentWayPoint + 1 < path.length){
		    	  newWaypoint.addEdge(path[currentWayPoint + 1]);
		      }
		      
		      //Third idea
		      //Mirar en las cuatro direcciones y pillar el waypoint mas cercano en cada 
		      //direccion y enlazar con esos
		      
		      //Add the new waypoint 
		      wpMap.addNode(newWaypoint);
		      
		      //Since we changed the waypoint map, lest say we are not in a path, and lets find
		      //another path to go
		      waypointDeleted = true;
		      goBack = true;
		      
		      System.out.println("Antes de lock");
		      //wpMap.lockMap();
		      System.out.println("Despues de lock");
	          //wpMap.saveMap(Configuration.getProperty( "map_waypoints_better_path"));		
	          System.out.println("Destination was " + destination);
	          disconnect();
	      }	
      }
      else//Bot is moving
      {
    	  if(delayActualizeBotPosition > 10){
        	  framesWithoutMove=1;
        	  delayActualizeBotPosition = 0;
        	  System.out.println("Player moved more than 10");
    	      //Actualiza la que serÃ¡ la posiciÃ³n previa para la siguiente iteraciÃ³n
    	      prevPosPlayer.set(player.getPlayerMove().getOrigin().getX(),
    	      player.getPlayerMove().getOrigin().getY(),
    	      player.getPlayerMove().getOrigin().getZ());    		  
    	  }else{
    		  delayActualizeBotPosition++;
    	  }
	
      }        
    }
    
    /***
     * Get the minimum distance to an obstacle in the direction the bot is
     * moving to.
     * @return nothing.
     */
    private float getObstacleDistance()
    {			
        // Set a vector in the direction of bot movement.
        Vector3f movDir = new Vector3f(player.getPlayerMove().getDirectionalVelocity().x, 
                                        player.getPlayerMove().getDirectionalVelocity().y,0.f);

        // Get the minimum distance to an obstacle on that direction.
        float distmin = this.getObstacleDistance(movDir,2500.f);			

        // Print the distance.
        if( distmin!=Float.NaN ){
            System.out.println("Distancia mmínima obstáculo " + distmin);
        }	
        return distmin;
    }
}