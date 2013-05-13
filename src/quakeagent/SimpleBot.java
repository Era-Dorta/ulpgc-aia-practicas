package quakeagent;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import soc.qase.bot.ObserverBot;
import soc.qase.file.bsp.BSPParser;
import soc.qase.tools.vecmath.Vector3f;

import java.util.Random;

import soc.qase.state.*;

import jess.*;
import soc.qase.ai.waypoint.Waypoint;
import soc.qase.ai.waypoint.WaypointMap;

/*
 * Every bot extends ObserverBot class.
 */
public final class SimpleBot extends ObserverBot
implements ShareDataListener
{	
    /*
    public enum BotStates {
        SEARCH_OBJECT, SEARCH_LOST_ENEMY, RENDEZVOUZ,
        FIGHTING
    }*/
    
    private BotStates botState = BotStates.RENDEZVOUZ;
    private BotStates prevBotState = botState;
    private BotStates mainState = botState;
    private boolean isLeader = false;
    private boolean gotSemaphore = true;
    
    private int prevWaypoint = 0;
    private int nextWaypoint = 0;
    
    //private String[] enemiesNames = {"Player"};
    //Variables 
    private World world = null;
    private Player player = null;

    private Vector3f prevPosPlayer = new Vector3f(0, 0, 0);

    private Vector3f posPlayer= new Vector3f(0, 0, 0);
    
    // These variables are used so the bot occasionally look behind for 
    // enemies.
    int nGlances = 0;
    boolean lookBehind = false;
    
    // Environment information.
    private BSPParser mibsp = null;

    // Distance to the enemy
    private float enemyDistance = Float.MAX_VALUE;
    
    // Bot's life (health + armor).
    private float life = 0, health = 0, armor = 0;
    
    // Bot relative ammo (current / maximum ammo )
    private float relativeAmmo;
    
    // Bot relative armament = (Total weapons / Maximum weapons)*100;
    private float relativeArmament;
    
    // Ammo that we should recharge.
    private String preferredAmmo = null;
    
    // When a battle begins, the bot current state (life, relative ammo, 
    // relative armament) is kept here. When the battle finishes, this
    // info. is passed to "Viking" module along with the battle result.
    private int[] botStateWhenBattleBegun = {0, 0, 0};
    
    // Bayesian classifier which keeps all battle results and used it for
    // predicting the result of a battle.
    private Viking viking = new Viking();
    
    //When true bot did not found a path so go back
    //to where we were
    private boolean goBack = false;
    
    // Bot position
    Vector3f pos = new Vector3f(0, 0, 0);
    
    //Struck with info about the enemies 
    class EnemyInfo{
    	public EnemyInfo(){
            position = new Origin();
            dead = false;
            timesAskDead = 0;
    	}    	
    	
    	public EnemyInfo( Origin position, boolean dead, int timesAskDead){
            this.position = position;
            this.dead = dead;
            this.timesAskDead = timesAskDead;
    	}
    	
    	public boolean isDead() {    		
            if( dead ){
                    timesAskDead++;
                    if(timesAskDead > 75){
                            dead = false;
                            timesAskDead = 0;
                    }    	    			
            }
                    return dead;
            }

            public void setDead(boolean dead) {
                    this.dead = dead;
            }
		
    	public Origin position;
    	private boolean dead;
    	private int timesAskDead;
    }
    
    // Array of positions and if there is an enemy dead there
    private Map<String, EnemyInfo > enemiesInfo = new HashMap<String, EnemyInfo>();
    
    private Origin lastKnownEnemyPosition = new Origin();
    
    private String lastKnownEnemyName = null;
    
    //The bot is following a path
    private boolean inPath = false;
    

    private double aimx = 0.0001, aimy = 1, aimz = 0, velx = 0.0001 ,vely = 1,
            velz = 0.0001;

    private int currentWayPoint = 0;
    // Inference engine.
    private Rete engine;

    int dire = 0;
    
    //Path to a given point
    private Waypoint [] path;
    
    //Path to a given point
    private Waypoint [] prevPath;
    
    private String preferredObject;


    /***
     * Constructor. Set the bot's name and look.
     * @param botName : bot name.
     * @param botSkin : bot skin.
     ***/
    public SimpleBot(String botName, String botSkin)
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
    public SimpleBot(String botName, String botSkin, boolean trackInv)
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
    public SimpleBot(String botName, String botSkin, boolean highThreadSafety, boolean trackInv)
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
    public SimpleBot(String botName, String botSkin, String password, boolean highThreadSafety, boolean trackInv)
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
    public SimpleBot(String botName, String botSkin, int recvRate, int msgLevel, int fov, int hand, String password, boolean highThreadSafety, boolean trackInv)
    {
            super((botName == null ? "MiBotseMueve" : botName), botSkin, recvRate, msgLevel, fov, hand, password, highThreadSafety, trackInv);
            initBot();
    }

    
    public void setMap(WaypointMap map)
    {
        this.wpMap = map;
    }    

    /***
     * Bot initialization.
     ***/
    private void initBot()
    {	
        // Inventory auto refresh.
        this.setAutoInventoryRefresh(true);
        
        // Init the inference engine.
        try {
            engine = new Rete();
            engine.batch( Configuration.getProperty( "clp_path" ) + "/general.clp" );
            engine.eval("(reset)");
        } catch (JessException je) {
            //System.out.println("initBot: Error in line " + je.getLineNumber());
            //System.out.println("Code:\n" + je.getProgramText());
            //System.out.println("Message:\n" + je.getMessage());
            //System.out.println("Aborted");
            System.exit(1);
        }
        
        viking = new Viking();
        
        //Set this bot in share data
        ShareData.registerBot(this);
        //Get first leader
        isLeader = (this == ShareData.getLeader());
        if(isLeader){
        	System.out.println("Init I am the leader " +  this.getPlayerInfo().getName());
        	this.sendConsoleCommand("Init I am the leader " +  this.getPlayerInfo().getName() );
        }
    }

    /*
     * Update 
     */
    private void updateBotState()
    {
        // Update info about health, armor and life.
        //prevHealth = health ;
        health = player.getHealth();
        armor = player.getArmor();
        life = health + armor;

        // Update firepower info (ammo percentage, weapons percentage, and
        // weapon with minimum ammo percentage).
        updateFirePowerInfo();
        
        // Update bot position
        Origin playerOrigin = player.getPlayerMove().getOrigin();
        pos.set(playerOrigin.getX(), playerOrigin.getY(), playerOrigin.getZ());
        
        prevPosPlayer = posPlayer;
        posPlayer = player.getPosition().toVector3f();
    }
    
    private boolean playerHasDied(){
        // if there was a sudden change in player position
        return (posPlayer.x < prevPosPlayer.x - 200 ||  posPlayer.x > prevPosPlayer.x + 200
    			|| posPlayer.y < prevPosPlayer.y - 200 ||  posPlayer.y > prevPosPlayer.y + 200
    			|| posPlayer.z < prevPosPlayer.z - 200 ||  posPlayer.z > prevPosPlayer.z + 200); 
    }

    /***
     * Change bot state (FIGHTING, SEARCHING OBJECT, etc).
     */
    public void changeState( BotStates newBotState )
    {
        prevBotState = botState;
        botState = newBotState;
        this.sendConsoleCommand( "Change my state to [" + botState + "]" );
    }
    
    /***
     * Main bot AI algorithm. 
     * @param w : Game current state.
     ***/
    @Override
    public void runAI(World w)
    {
        if (mibsp==null){
            mibsp = this.getBSPParser();
        }

        //System.out.println( "AI...\n" );
        
        // Retrieve world and player current states.
        world = w;
        player = world.getPlayer();
        
        // Update bot state information (pos, health, armor, firepower, etc).
        updateBotState();
        
        //The group state is rendezvouz, so some bot is not within the group
        if(ShareData.getGroupState() != mainState){
        	mainState = ShareData.getGroupState();
        	changeState(ShareData.getGroupState());
        }       

        // Tell the bot not to move, standard action    
        Vector3f DirMov = new Vector3f(velx, vely, velz);
        Vector3f aim = new Vector3f(aimx, aimy, aimz);        
        setBotMovement(DirMov, aim, 0, PlayerMove.POSTURE_NORMAL);
        
        // If player just reborn, start by searching an object.
        // TODO: ¿Y si el respawn se configura para que no sea automatico?.
        // Tiene sentido mirar la variable respawnNeeded 
        if( playerHasDied() && (botState == BotStates.FIGHTING) ){
            // In the player was fighting record battle experience.
            this.sendConsoleCommand( "I'LL BE BACK!" );
            System.out.println( "I'LL BE BACK!" );
            viking.addBattleExperience( botStateWhenBattleBegun, Viking.FAIL );
                
            //Reunite with all your friends
            ShareData.botDied();
        }
        
        // Is there any visible enemy? If so, retrieve info about him/her.
        /*
        if( lastKnownEnemyName == null ){
            changeState( prevBotState );
        }
         * 
         */
        
        if( (botState == BotStates.FIGHTING) ){
            // The bot is currently fighting. Get info about its current enemy.
            Entity currentEnemy = world.getOpponentByName( lastKnownEnemyName );
            EnemyInfo enemyInfo = retrieveEnemyInfo( currentEnemy );

            // Check if current enemy has died during last frame.
            //if( enemyHasDied( currentEnemy, enemyInfo ) ){
            if( currentEnemy.hasDied() ){
                this.sendConsoleCommand( "HAHAHA - YOU DIED! [" + lastKnownEnemyName + "]" );
                System.out.println( "HAHAHA - YOU DIED! [" + lastKnownEnemyName + "]" );
                viking.addBattleExperience( botStateWhenBattleBegun, Viking.WIN );

                // Stop figthing
                lastKnownEnemyName = null;
                // Current enemy has die. Go back to previous state.
                changeState( mainState );
            }else{
                // Check current enemy's visibility.
                if( enemyIsVisible( currentEnemy.getOrigin().toVector3f() ) ){
                    // Current enemy is visible. Attack him/her!.
                    attackEnemy( currentEnemy );
                }else{
                    // Current enemy is not visible. Search it!
                    this.sendConsoleCommand( "NINJA! WHERE DID YOU GO?" );
                    inPath = false;
                    lastKnownEnemyName = null;
                    changeState( BotStates.SEARCH_LOST_ENEMY );
                }
            }               
        }
        
        // This is not implemented with a "else" because previous if can
        // change bot state to "SEARCH_OBJECT".
        if( botState != BotStates.FIGHTING ){
            // Bot is not fighting currently. Is there any visible enemy? If
            // so, retrieve information about him/her.
            Entity enemy = findVisibleEnemy();
            
            // Get expected battle result.
            int expectedBattleResult = viking.getExpectedBattleResult( (int)life, (int)relativeAmmo, (int)relativeArmament );

            if( (enemy != null) && (expectedBattleResult == Viking.WIN) ){
                // There is a visible new enemy. Retrieve information about
                // him/her and fight!.
                this.sendConsoleCommand( "LET'S FIGHT!");
                retrieveEnemyInfo( enemy );
                startBattle( enemy );
            }else{
                if(enemy != null){
                    this.sendConsoleCommand( "NOO, I'M SCARED!");
                }
                // Bot decides which object (health, armor, etc) prefers given 
                // its current state.
                decidePreferredObject();
            }
        }
       
        // Decide a movement direction.
        setMovementDir();
        
        // Get the distance to the nearest obstacle in the direction
        // the bot moves to.
        getObstacleDistance();
        
        prevPosPlayer = posPlayer;
        
        //System.out.println( "AI...OK\n" );
    }
    
    /***
     * Feed the inference engine with the bot's current state. The 
     * inference engine will then return which object type (health, armor,
     * ammo or weapon) the bot should look for.
    ***/
    private void decidePreferredObject()
    {
        Fact f;
        try {
            engine.eval("(reset)");
            
            // Feed inference engine with bot's current state.
            f = new Fact("bot-state", engine );
            f.setSlotValue("health", new Value( health, RU.INTEGER));
            f.setSlotValue("armor", new Value( armor, RU.INTEGER));
            f.setSlotValue("ammo", new Value( relativeAmmo, RU.INTEGER));
            f.setSlotValue("fire-power", new Value( relativeArmament, RU.INTEGER));
            engine.assertFact(f);

            // Execute inference engine and save the result in 
            // "preferredObject".
            engine.run();
            Value v = engine.eval("?*preferred-object*");
            preferredObject = v.stringValue(engine.getGlobalContext());
            //System.out.println( "Preferred object: " + v.stringValue(engine.getGlobalContext()));
        } catch (JessException ex) {
            Logger.getLogger(SimpleBot.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /***
     * Decide in which direction the bot will move.
     ***/
    private void setMovementDir()
    {    	
        if(!inPath){
            prevPath = path;
            inPath = true;
            
            System.out.println(this.getPlayerInfo().getName() +  " set move in path false" );
            
            switch(botState){
        	case RENDEZVOUZ:
        		System.out.println(this.getPlayerInfo().getName() +  " prev rendezvouz" );
    			gotSemaphore = ShareData.calculateGroupDestination(posPlayer, gotSemaphore);
                if(!gotSemaphore){
                	System.out.println(this.getPlayerInfo().getName() +  " waiting for calculation gropu destination" );
                	inPath = false;
                	return;
                }
                // Try to reunite with the team at a given point.
                this.sendConsoleCommand("Rendezvouz mode" );
                Origin dest = new Origin(ShareData.getGroupDestination());
                System.out.println("destination " + dest.toVector3f() + " current " + posPlayer);
                path = findShortestPath(dest);
                System.out.println("I am " + getPlayerInfo().getName() + " my destination is " +  path[path.length - 1].getPosition());
                //rendezvousMode = false;
                break;
        	case SEARCH_LOST_ENEMY:
                    // Bot was figthing an enemy but lost him/her. Try to find
                    // him/her again.
                    
                    this.sendConsoleCommand("Searching for lost enemy [" + lastKnownEnemyName + "]" );
                    
                    //If the enemy died or we do not have information to follow the bot then change the current bot state
                    if( lastKnownEnemyName == null || enemiesInfo.get(lastKnownEnemyName) == null ||
                     enemiesInfo.get(lastKnownEnemyName).isDead()){
                    	inPath = false;
                    	changeState(mainState);
                    }else{
                    	path = findShortestPath(lastKnownEnemyPosition);
                    }	                    
                break;
        	case SEARCH_OBJECT:
        		System.out.println("Search object ");
    			if(isLeader){
                    this.sendConsoleCommand( "Life: (" + life + ") " +
                                              "Relative ammo: (" + relativeAmmo + ")" +
                                              "Relative armament: (" + relativeArmament + ") ->" +
                                              "Voy a buscar [" + preferredObject + "]" );                    
                    //System.out.println( "Searching for an object type [" + preferredObject + "]" );
                    
                    //System.out.println( "findShortestPathToItem 1" );
                    if( preferredObject.equals( "weapon" ) ){
                        path = findShortestPathToWeapon( null );
                    }else if( preferredObject.equals( "ammo" ) ){
                        //System.out.println( "\t Preferred Ammo: " + preferredAmmo );
                        path = findShortestPathToItem( "ammo", preferredAmmo );
                    }else if( preferredObject.equals( "armor" ) ){
                        path = findShortestPathToItem( "armor", null );
                    }else{
                        // TODO
                        // Se ha probado las siguientes strings sin exito
                        // "life", "health", "healing", "hp", Entity.TYPE_HEALTH.
                        path = findShortestPathToItem( "armor", null );
                        preferredObject = "armor";
                    }
                    this.sendConsoleCommand(getPlayerInfo().getName() + " Leader decision is " +  path[path.length - 1].getPosition());
                    System.out.println(this.getPlayerInfo().getName() + " Leader decision is " +  path[path.length - 1].getPosition());
                    ShareData.setGroupDestination(path[path.length - 1].getPosition());
                    System.out.println( this.getPlayerInfo().getName() + " Leader decision sended ");
                    this.sendConsoleCommand(this.getPlayerInfo().getName() + " Leader decision sended ");
                    //System.out.println( "findShortestPathToItem 2" );
                    
                   //this.sendConsoleCommand("Voy a buscar un arma");
                   //path = findShortestPathToWeapon(null);
    			}else{
    				gotSemaphore = ShareData.waitLeaderDecision();
    				if(!gotSemaphore){
    					System.out.println(this.getPlayerInfo().getName() +  " waiting for leader decision" );
    					inPath = false;
    					return;
    				}
    		        this.sendConsoleCommand(getPlayerInfo().getName() + " Waiting leader decision " +  this.getPlayerInfo().getName() );
    		        System.out.println(getPlayerInfo().getName() + " Waiting leader decision " +  this.getPlayerInfo().getName() );
    				
                    dest = new Origin(ShareData.getGroupDestination());
                    path = findShortestPath(dest);      
                    System.out.println(getPlayerInfo().getName() + " Leader decided " +  dest);
                    this.sendConsoleCommand(getPlayerInfo().getName() + " Leader decided " +  dest );
    			}
                break;
            case FIGHTING:
            	//Bot is not following a path and found an enemy, get path to enemy, 
            	//so bot can move while shooting 
            	path = findShortestPath(lastKnownEnemyPosition);   
            } // Switch end.
        	
            // The previous instructions should have given the bot a path
            // to follow. If not, try to follow the previous path in reverse
            // order.
            if(path == null || path.length == 0){
               if(prevPath != null){
                   //this.sendConsoleCommand( this.getPlayerInfo().getName() + "Noo waypoints going back");	                		   
                   goBack = true;
                   path = prevPath;
               }else{
                   try{
                       //System.out.println("No waypoint, we are fucked");
                       System.in.read();
                    }catch (IOException e){
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
               }
            }else{
                // The bot has a path to follow. Start by the beginning.
            	currentWayPoint = 0;	                                	
            }
            
        	
        }else{
            // The bot was currently following a path. Keep following it.
            if( posPlayer.distance(path[currentWayPoint].getPosition()) < 25 ){
               if(!goBack){
                   if( currentWayPoint < path.length - 1){
                	   	if( botState != BotStates.FIGHTING ){
                	   		//Bot is not fighting, keep going
                           currentWayPoint++;
                	   	}else{
                	   		//Bot is fighting, move a bit
                	   		if(currentWayPoint == nextWaypoint){
                	   			currentWayPoint = prevWaypoint;
                	   		}else{
                	   			currentWayPoint = nextWaypoint;
                	   		}
                	   	}
                   }else{
                       //Bot reached destination
                	   System.out.println("Bot reached destination ");
                       inPath = false; 
                        switch(botState){
                        case SEARCH_LOST_ENEMY:
                        	changeState(mainState);                		
                            break;
                        case RENDEZVOUZ:
                        case SEARCH_OBJECT:
                        	mainState = BotStates.SEARCH_OBJECT;
                        	changeState(mainState); 
                        	ShareData.setGroupState(BotStates.SEARCH_OBJECT);
                        	if(isLeader){
                        		ShareData.changeLeader();
                        	}else{	
                        		gotSemaphore = ShareData.waitLeaderChange();
                				if(!gotSemaphore){
                					System.out.println(this.getPlayerInfo().getName() +  " waiting for leader change" );
                					inPath = true;
                					return;
                				}
                        	}
                        	isLeader = (this == ShareData.getLeader());
                            if(isLeader){
                            	System.out.println("Leader changed, I am the leader " +  this.getPlayerInfo().getName());
                            	this.sendConsoleCommand("Leader changed, I am the leader " +  this.getPlayerInfo().getName() );
                            }
                            break;
                        case FIGHTING:
                        	inPath = true;
                        	currentWayPoint = prevWaypoint;
                        	break;
                        }
                   }
               }else{
                   if( currentWayPoint > 0){
                           currentWayPoint--;
                   }else{
                	   System.out.println("Bot reached destination backwards");
                       //Bot reached destination
                       inPath = false;   
                       goBack = false;
                       //currentWayPoint = 0;
                   }
               }
           }             
        }
        
        // Set the movement and aiming direction.
        /*
        System.out.printf( "Soy " + getPlayerInfo().getName() + 
                           " Voy en direccion %f %f el currentway es %d el total es %d \n", 
                           velx, vely, currentWayPoint, path.length );
        System.out.printf( "Estoy en %f %f %f voy a %f %f %f \n", 
                           posPlayer.x, posPlayer.y, posPlayer.z,
                           path[currentWayPoint].getPosition().x,
                           path[currentWayPoint].getPosition().y, 
                           path[currentWayPoint].getPosition().z );
         */
        velx = path[currentWayPoint].getPosition().x - posPlayer.x;
        vely = path[currentWayPoint].getPosition().y - posPlayer.y;
        velz = path[currentWayPoint].getPosition().z - posPlayer.z;           
        Vector3f DirMov = new Vector3f(velx, vely, velz);       

        // The bot occasionally looks behind for enemies.
        Vector3f aim;
        if( lookBehind ){
            aim = new Vector3f( -velx, -vely, velz );
            nGlances++;
        }else{
            aim = new Vector3f( velx, vely, velz );
            nGlances++;
        }
        if( nGlances > 10 ){
            nGlances = 0;
            lookBehind = !lookBehind;
        }
        
        //From previous frame we got where to aim
        if(botState == BotStates.FIGHTING){
        	aim = new Vector3f( aimx, aimy, aimz );
        }
        
        
        //Set aim in the same direction as the bot moves
        setBotMovement(DirMov, aim, 200, PlayerMove.POSTURE_NORMAL);
        
        //Do not update here the aim, when fighting,
        //since it will be updated in attack method
        if(botState != BotStates.FIGHTING){
	        aimx = aim.x;
	        aimy = aim.y;
	        aimz = aim.z;    
        }
    }

    
    private void updateFirePowerInfo(){
        // Ammo and maxAmmo for each weapon.
        int ammo, maxAmmo;
        
        // Relative Ammo (ammo/maxAmmo) of the current weapon.
        float currentRelativeAmmo = 0;
        
        // Sum of all "relativeAmmo"s. It will divide by the total number of 
        // weapons.
        float totalRelativeAmmo = 0;
        
        // Minimum Relative Ammo and weaponWithMinimumAmmo are used to find
        // out which weapon we should recharge.
        float minRelativeAmmo = 500;
        
        // Total number of weapons.
        int totalWeapons = 0;
        
        // Associate each weapon with its corresponding ammo.
        int[][] weaponsAmmo =
        {
            { PlayerGun.SHOTGUN, PlayerGun.SHELLS },
            { PlayerGun.SUPER_SHOTGUN, PlayerGun.SHELLS },   
            { PlayerGun.HYPERBLASTER, PlayerGun.CELLS },
            { PlayerGun.BFG10K, PlayerGun.CELLS },
            { PlayerGun.MACHINEGUN, PlayerGun.BULLETS },
            { PlayerGun.CHAINGUN, PlayerGun.BULLETS },
            { PlayerGun.GRENADE_LAUNCHER, PlayerGun.GRENADES },
            { PlayerGun.ROCKET_LAUNCHER, PlayerGun.ROCKETS },
            { PlayerGun.RAILGUN, PlayerGun.SLUGS }
        };
        
        /*
        String[] weaponsStr =
        {
            Entity.TYPE_SHOTGUN, Entity.TYPE_SUPERSHOTGUN, 
            Entity.TYPE_HYPERBLASTER, Entity.TYPE_BFG,
            Entity.TYPE_MACHINEGUN, Entity.TYPE_CHAINGUN,
            Entity.TYPE_GRENADELAUNCHER, Entity.TYPE_ROCKETLAUNCHER,
            Entity.TYPE_RAILGUN
        };
        */
        String[] ammoStrings =
        {
            "shells", "shells",
            "cells", "cells",
            "bullets", "bullets",
            null, // Todo: granadas cuenta como arma?
            "rockets",
            "slugs"
        };
        
        // Get player's inventory.
        Inventory inventory = world.getInventory();
        
        // Iterate over player weapons.
        for( int i=0; i<weaponsAmmo.length; i++ ){
            if( inventory.getCount( weaponsAmmo[i][0] ) > 0 ){
                // Get the relative ammo for the current weapon.
                ammo = inventory.getCount( weaponsAmmo[i][1] );
                maxAmmo = PlayerGun.getMaxAmmo( weaponsAmmo[i][1] );
                currentRelativeAmmo = ammo / (float)maxAmmo;
                
                // Find out which weapon is the one with the minimum relative
                // ammo.
                if( currentRelativeAmmo < minRelativeAmmo ){
                    minRelativeAmmo = currentRelativeAmmo; 
                    preferredAmmo = ammoStrings[i];
                }
                
                // Sum global quantities.
                totalRelativeAmmo += currentRelativeAmmo;
                totalWeapons++;
                
                //System.out.println( ammo + " / " + maxAmmo );
            }
        }
        
        // totalWeapons doesn't include Blaster, so we make a distinction. 
        if( totalWeapons > 0 ){
            relativeAmmo = ((totalRelativeAmmo / (float)totalWeapons)*100);
        }else{
            relativeAmmo = 100;
            
        }
        
        // Bot relative armament = (Total weapons / Maximum weapons)*100;
        // (10 is the maximum number of weapons in quake (including blaster).
        relativeArmament = ((totalWeapons+1) / (float)10) * 100;
        
        // Print info.
        //System.out.println( "ammoPercentage: " + relativeAmmo + " %" );
        //System.out.println( "relativeArmament: " + (totalWeapons+1) + "/ 10 -> " + relativeArmament + "% )" );
    }


    /***
     * Select which weapon use.
     ***/
    private void selectWeapon( float distance )
    {
        int preferredWeapon = WeaponType.getBetterWeapon( distance, world.getInventory() );
        changeWeapon( preferredWeapon );
    }

    
    private boolean enemyIsVisible( Vector3f enemyPos )
    {
        // Get player and enemy positions as a Vector3f. TODO.
        Vector3f a = posPlayer; // new Vector3f(playerOrigin);
        Vector3f b = enemyPos;
        
        if (mibsp.isVisible(a,b)){
            Vector3f aim = new Vector3f(aimx, aimy, aimz);
            //Dot product of two normalized vectors gives
            //cos of the angle between them, 
            //cos is positive from 0 to 90º and from 0 to -90º
            //So as long as the cos is positive the enemy is in front of us
            //Vector from player to enemy, enemy - player
            // a = player, b = enemy
            aim.normalize();
            b.sub(a);
            b.normalize();

            // Enemy is visible if he/her is in front of us.
            return ( aim.dot(b) >= 0 );

        }else{
            return false;
        }
    }
    
    

    /***
     * Search for a visible enemy.
     * @return the entity of the closest visible enemy, or null if there are
     * not such an enemy.
     ***/
    private Entity findVisibleEnemy()
    {
        setAction(Action.ATTACK, false);
        // Is there information about player?
        if (player!=null)
        {
            // Is there information about environment?
            if (mibsp!=null)
            {
                // Initializations.
                Entity nearestEnemy = null;
                Entity tempEnemy = null;
                Vector<Entity> enemies = null;
                
                Origin enemyOrigin = null;
                Vector3f enPos; 
                Vector3f enDir;
                boolean NearestVisible=false;
                float enDist = Float.MAX_VALUE;

                // Bot position
                enDir = new Vector3f(0, 0, 0);
                enPos = new Vector3f(0, 0, 0);

                // Get information about all enemies.
                enemies = world.getOpponents();

                // Print number of enemies.
                //System.out.println("Enemigos " + enemies.size());

                // Get the most interesting enemy according to 2D distance and
                // visibility.
                for( int i = 0; i < enemies.size(); i++ ){
                    // Get current entity.
                    tempEnemy = (Entity) enemies.elementAt(i);
                    
                    EnemyInfo enemyInfo = retrieveEnemyInfo( tempEnemy );
                    if( !enemyHasDied( tempEnemy, enemyInfo ) ){
                    
                        // Those bots whose name starts with "KillBot" are allies.
                        // Ignore them.
                        if( !tempEnemy.getName().startsWith( "KillBot" ) ){

                            // Get current entity's position.
                            enemyOrigin = tempEnemy.getOrigin();

                            // Get enemy pos as a vector (we don't care about Z).
                            enPos.set(enemyOrigin.getX(), enemyOrigin.getY(),enemyOrigin.getZ());

                            // Set a 2D vector between entity and bot positions.
                            enDir.sub(enPos, pos);

                            // Check if current enemy is visible and neared than the
                            // nearest enemy found until now. If true, save it as the
                            // new closest enemy.
                            if((nearestEnemy == null || enDir.length() < enDist) && enDir.length() > 0){
                                nearestEnemy = tempEnemy;
                                enDist = enDir.length();

                                // Check if nearest enemy is visible.
                                NearestVisible = enemyIsVisible( enemyOrigin.toVector3f() );
                            }
                        }
                    }
                } // for

                if( NearestVisible ){
                    return nearestEnemy;
                }
                				
            } // End of if (mibsp!=null)
        } // End of if (player!=null)

        return null;
    }

    private EnemyInfo retrieveEnemyInfo( Entity enemy )
    {
        EnemyInfo enemyInfo = enemiesInfo.get( enemy.getName() );
        if(enemyInfo == null){
            //This is the first time we face this enemy
            enemyInfo = new EnemyInfo();
            enemiesInfo.put( enemy.getName(), enemyInfo);                        			
        }
        
        enemyInfo.position = enemy.getOrigin();
        
        return enemyInfo;
    }
    
    private boolean enemyHasDied( Entity enemy, EnemyInfo enemyInfo )
    {
        //If enemy was in a previous frame do not erase that information
        if(!enemyInfo.isDead()){
            enemyInfo.setDead(enemy.hasDied());
        }

        return enemyInfo.isDead();
    }
    
    
    private void startBattle( Entity enemy )
    {        
        // Show bot's battle statistics (wins, fails).
        int[] totalResults = viking.getBattleResults();
        
        this.sendConsoleCommand( "Starting battle (" + 
                                 totalResults[Viking.WIN] + ", " +
                                 totalResults[Viking.FAIL] + ")" );
        
        // Save enemy's name.
        lastKnownEnemyName = enemy.getName();
        
        // Save bot state when battle begun.
        botStateWhenBattleBegun[0] = (int)life;
        botStateWhenBattleBegun[1] = (int)relativeAmmo;
        botStateWhenBattleBegun[2] = (int)relativeArmament;
        

        // Get expected battle result.
        /*
        int expectedBattleResult = viking.getExpectedBattleResult( botStateWhenBattleBegun );

        switch( expectedBattleResult ){
            case Viking.WIN:
                this.sendConsoleCommand( "Resultado esperado: WIN" );
            break;
            case Viking.FAIL:
                this.sendConsoleCommand( "Resultado esperado: FAIL" );
            break;
            default:
                this.sendConsoleCommand( "Resultado esperado: UNFINISHED" );
            break;     
        }
        */
        changeState( BotStates.FIGHTING );
        
        //Save prev and next waypoint so the bot will move between
        prevWaypoint = currentWayPoint - 2;
        if(prevWaypoint < 0){
        	prevWaypoint = 0;
        }
        nextWaypoint = currentWayPoint + 2;
        
        if(path != null && nextWaypoint > path.length - 1){
        	nextWaypoint = path.length - 1;
        }     
    }
    
    private void attackEnemy( Entity enemy )
    {
        Origin enemyOrigin = null;
        Vector3f enPos; 
        Vector3f enDir;
        float enDist = Float.MAX_VALUE;
        
        enDir = new Vector3f(0, 0, 0);
        enPos = new Vector3f(0, 0, 0);
        
        ////System.out.println( "ATTACK_MODE" );
        ////System.out.println( "lastOpponenName: [" + opponentName + "]" );
        // Did we find a nearest enemy?
            // Get tntity's position.
        enemyOrigin = enemy.getOrigin();
        enPos.set(enemyOrigin.getX(), enemyOrigin.getY(), enemyOrigin.getZ());

        // Set movement direction according to the selected entity
        // and bot position.
        enDir.sub( enPos, pos );
        //enDir.normalize();

        lastKnownEnemyPosition = enemyOrigin;
        
        //this.sendConsoleCommand("Modo ataque");
        selectWeapon( enDir.length() );
        
        // Set weapon's angle.
        Angles arg0=new Angles(enDir.x,enDir.y,enDir.z);
        player.setGunAngles(arg0);

        // Set attack mode.
        setAction(Action.ATTACK, true);	

        aimx = enDir.x;
        aimy = enDir.y;
        aimz = enDir.z;

        // Distance to enemy (for the inference engine).
        enemyDistance = enDir.length();
        //System.out.println("Enemy distance " + enemyDistance + " range " + WeaponType.getBetterRange(enemyDistance));
        //return true;
        
        /*
        }else{
            // Nearest enemy is not visible. Try to go to him/her.
            if(botState == BotStates.FIGHTING){
                    prevBotState = botState;
                    botState = BotStates.SEARCH_LOST_ENEMY;
            }                                           
            //System.out.println("Hay enemigo, pero no estÃ¡ visible ");
            enemyDistance = Float.MAX_VALUE;

            lastFrameAttackedEnemy = null;
        }*/
        // End of if asking for nearest enemy.
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
            //System.out.println("Distancia mmínima obstáculo " + distmin);
        }	
        return distmin;
    }

	@Override
	public void leaderForcedChanged() {
		if(isLeader){
			System.out.println(this.getPlayerInfo().getName() +  " I fail, I've been kick out force change" );
		}
    	mainState = BotStates.SEARCH_OBJECT;
    	changeState(mainState); 
    	inPath = false;
        gotSemaphore = true;
    	isLeader = (this == ShareData.getLeader());
    	if(isLeader){
    		System.out.println(this.getPlayerInfo().getName() +  " forced changed I am new leader " );
    	}
	}


	@Override
	public void friendDied() {
		System.out.println(this.getPlayerInfo().getName() + " a friend died, changing to rendenvouz");
		//A friendly bot died, go to rendevouz again
    	mainState = ShareData.getGroupState();
    	changeState(mainState); 
    	inPath = false;		
        gotSemaphore = true;
	}
}
