package quakeagent;

import java.io.IOException;
import java.util.Vector;
import java.util.Random;

import java.util.logging.Level;
import java.util.logging.Logger;
import soc.qase.bot.ObserverBot;
import soc.qase.file.bsp.BSPParser;
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
public final class MiBotseMueve extends ObserverBot
{
    //Variables 
    private World world = null;
    private Player player = null;

    private Vector3f posPlayer= new Vector3f(0, 0, 0);

    // Bot previous position.
    private Vector3f prevPosPlayer = new Vector3f(0, 0, 0);

    // Bot previous position.
    private Vector3f destination = new Vector3f(0, 0, 0);    
    
    // Bot movement.
    private int nsinavanzar = 0, nDirectionChanges = 0;

    // Environment information.
    private BSPParser mibsp = null;

    // Distancia al enemigo que estamos atacando
    private float enemyDistance = Float.MAX_VALUE;
    
    //The bot is following a path
    private boolean inPath = false;
    

    private double aimx = 0.0001, aimy = 1, velx = 0.0001 ,vely = 1,
            velz = 0.0001, prevVelX= 0.0001, prevVelY = 0.0001;    

    private int currentWayPoint = 0;
    // Inference engine.
    private Rete engine;

    int dire = 0;
    
    //Path to a given point
    private Waypoint [] path;


    /***
     * Constructor. Set the bot's name and look.
     * @param botName : bot name.
     * @param botSkin : bot skin.
     ***/
    public MiBotseMueve(String botName, String botSkin)
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
    public MiBotseMueve(String botName, String botSkin, boolean trackInv)
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
    public MiBotseMueve(String botName, String botSkin, boolean highThreadSafety, boolean trackInv)
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
    public MiBotseMueve(String botName, String botSkin, String password, boolean highThreadSafety, boolean trackInv)
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
    public MiBotseMueve(String botName, String botSkin, int recvRate, int msgLevel, int fov, int hand, String password, boolean highThreadSafety, boolean trackInv)
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

            engine.batch( Configuration.getProperty( "clp_path" ) );

            engine.eval("(reset)");
            engine.assertString("(color rojo)");

            engine.run();

            Value v = engine.eval("?*VARGLOB*");
            System.out.println(v.intValue(engine.getGlobalContext()));
        } catch (JessException je) {
            System.out.println("initBot: Error in line " + je.getLineNumber());
            System.out.println("Code:\n" + je.getProgramText());
            System.out.println("Message:\n" + je.getMessage());
            System.out.println("Aborted");
            System.exit(1);
        }
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

        System.out.println("AI...\n");

        // Retrive game current state.
        world = w;

        // Get information about the bot.
        player = world.getPlayer();
        
        posPlayer = player.getPlayerMove().getOrigin().toVector3f(); 

        // Print various information about the bot.
        //System.out.println("Is Running? " + player.isRunning() + "\n");
        //System.out.println("getPosition " + player.getPosition() + "\n");
        //System.out.println("isAlive " + player.isAlive() + "\n");
        //System.out.println("Arma visible?..." + findVisibleWeapon() + "\n");
        //System.out.println("Entidad visible?..." + findEntity() + "\n");

        // Decide a movement direction.
        setMovementDir();

        // Print information about the bot's state.
        printState();

        // Get the distance to the nearest obstacle in the direction
        // the bot moves to.
        getObstacleDistance();

        // Atack!
        setAction(Action.ATTACK, true);

        // TODO: Complete.
        try 
        {
            engine.retractString("(color rojo)");
            engine.assertString("(color rojo)");

            engine.assertString("(color azul)");
            engine.run();
            engine.eval("(facts)");

            Value v = engine.eval("?*VARGLOB*");
            System.out.println(v.intValue(engine.getGlobalContext()));

        } catch (JessException je) {
            System.out.println("initBot: Error en la linea " + je.getLineNumber());
            System.out.println("Codigo:\n" + je.getProgramText());
            System.out.println("Mensaje:\n" + je.getMessage());
            System.out.println("Abortado");
            System.exit(1);
        }
    }


    /***
     * Decide in which direction the bot will move.
     ***/
    private void setMovementDir()
    {

            if(!inPath){
                path = findShortestPathToWeapon(null);
                currentWayPoint = 0;
                inPath = true;
            }else{
               if( posPlayer.distance(path[currentWayPoint].getPosition()) < 50 ){
                   if( currentWayPoint < path.length - 1){
                        currentWayPoint++;
                   }else{
                       //Bot reached destination
                       inPath = false;                       
                   } 
                   
               }  
                float distObstacle = getObstacleDistance();

                /*if(distObstacle < 10 || Float.isNaN(distObstacle) ){
                    System.out.println("Error me choco con un obstaculo\n");
                    //Llegue al destino
                    currentWayPoint++;
                }   */            
            }
        velx = path[currentWayPoint].getPosition().x - posPlayer.x;
        vely = path[currentWayPoint].getPosition().y - posPlayer.y;
        velz = path[currentWayPoint].getPosition().z - posPlayer.z;           
        Vector3f DirMov = new Vector3f(velx, vely, velz);
        //Set aim in the same direction as the bot moves
        Vector3f aim = new Vector3f(velx, vely, velz);
        setBotMovement(DirMov, aim, 100, PlayerMove.POSTURE_NORMAL); 
    }

    
    /***
     * List available weapons with its ammunition. Ammunition can be queried
     * directly or through the weapon.
     */
    private void listArmament()
    {
        String nf = "listArmament";

        System.out.println("---------- Entrando en " + nf);
        try {
            // Clear previous info.
            engine.reset();

            if( world.getInventory().getCount(PlayerGun.BLASTER)>=1 ){
                System.out.println("BLASTER");			
            }

            if( world.getInventory().getCount(PlayerGun.SHOTGUN)>=1 ){
                System.out.print("SHOTGUN");
                if (world.getInventory().getCount(PlayerGun.getAmmoInventoryIndexByGun(PlayerGun.SHOTGUN))>0){
                    System.out.print(" y municiones");
                    engine.store("SHOTGUN", new Value(1, RU.INTEGER));
                }
                System.out.println("");
            }else{
                engine.store("SHOTGUN", new Value(0, RU.INTEGER));				
            }

            if (world.getInventory().getCount(PlayerGun.SUPER_SHOTGUN)>=1){
                System.out.print("SUPER_SHOTGUN");
                if( world.getInventory().getCount(PlayerGun.getAmmoInventoryIndexByGun(PlayerGun.SUPER_SHOTGUN))>0 ){
                    System.out.print(" y municiones");
                    engine.store("SUPER_SHOTGUN", new Value(1, RU.INTEGER));
                }
                System.out.println("");
            }else{
                engine.store("SUPER_SHOTGUN", new Value(0, RU.INTEGER));
            }

            // Query shells quantity directly (shotgun and supershotgun).
            if (world.getInventory().getCount(PlayerGun.SHELLS)>=1){
                System.out.println("SHELLS disponibles");
            }

            // It uses BULLETS.
            if( world.getInventory().getCount(PlayerGun.CHAINGUN)>=1 ){
                System.out.print("CHAINGUN");
                if (world.getInventory().getCount(PlayerGun.getAmmoInventoryIndexByGun(PlayerGun.CHAINGUN))>0){
                    System.out.print(" y municiones");	
                    engine.store("CHAINGUN", new Value(1, RU.INTEGER));
                }
                System.out.println("");
            }else{
                engine.store("CHAINGUN", new Value(0, RU.INTEGER));				
            }

            // It uses BULLETS.
            if( world.getInventory().getCount(PlayerGun.MACHINEGUN)>=1 ){
                System.out.print("MACHINEGUN");
                //Consultamos la municiÃ³n a travÃ©s del arma
                if (world.getInventory().getCount(PlayerGun.getAmmoInventoryIndexByGun(PlayerGun.MACHINEGUN))>0){
                    System.out.print(" y municiones");	
                    engine.store("MACHINEGUN", new Value(1, RU.INTEGER));
                }
                System.out.println("");
            }else{
                engine.store("MACHINEGUN", new Value(0, RU.INTEGER));				
            }

            // Ammo for chaingun and machinegun.
            if (world.getInventory().getCount(PlayerGun.BULLETS)>=1){
                System.out.println("BULLETS disponibles");
            }

            // It uses grenades.
            if (world.getInventory().getCount(PlayerGun.GRENADE_LAUNCHER )>=1){
                System.out.println("GRENADE_LAUNCHER \n");
                if (world.getInventory().getCount(PlayerGun.getAmmoInventoryIndexByGun(PlayerGun.GRENADE_LAUNCHER ))>0){
                        System.out.println("y municiones\n");
                }
            }	

            // Query grenades quantity (for grenade launcher).
            int cantidad = world.getInventory().getCount(PlayerGun.GRENADES);
            if (cantidad >= 1){
                System.out.println("GRENADES disponibles");
            }
            engine.store("GRENADES", new Value(cantidad, RU.INTEGER));			

            // It use Rockets.
            if( world.getInventory().getCount(PlayerGun.ROCKET_LAUNCHER )>=1 ){
                System.out.println("ROCKET_LAUNCHER");
                if (world.getInventory().getCount(PlayerGun.getAmmoInventoryIndexByGun(PlayerGun.ROCKET_LAUNCHER ))>0){
                    System.out.println(" y municiones");
                    engine.store("ROCKET_LAUNCHER", new Value(1, RU.INTEGER));
                }	
            }else{
                engine.store("ROCKET_LAUNCHER", new Value(0, RU.INTEGER));
            }
            
            // Are there rockets? (for rocket launcher).
            if (world.getInventory().getCount(PlayerGun.ROCKETS )>=1){
                System.out.println("ROCKETS disponibles");
            }

            // It uses cells.
            if( world.getInventory().getCount(PlayerGun.HYPERBLASTER)>=1 ){
                System.out.println("HYPERBLASTER\n");
                if (world.getInventory().getCount(PlayerGun.getAmmoInventoryIndexByGun(PlayerGun.HYPERBLASTER))>0){
                    System.out.println("y municiones\n");
                }
            }
            
            if( world.getInventory().getCount(PlayerGun.BFG10K)>=1 ){
                System.out.println("BFG10K\n");
                if (world.getInventory().getCount(PlayerGun.getAmmoInventoryIndexByGun(PlayerGun.BFG10K))>0){
                    System.out.println("y municiones\n");	
                }
            }
            
            // Ammo for BFG10K and HYPERBLASTER.
            if( world.getInventory().getCount(PlayerGun.CELLS)>=1 ){
                System.out.println("CELLS disponibles");
            }

            // It uses SLUGS.
            if( world.getInventory().getCount(PlayerGun.RAILGUN)>=1 ){
                System.out.println("RAILGUN\n");
                if( world.getInventory().getCount(PlayerGun.getAmmoInventoryIndexByGun(PlayerGun.RAILGUN))>0 ){
                    System.out.println("y municiones\n");
                }
            }
            
            // Ammo for RAILGUN.
            if( world.getInventory().getCount(PlayerGun.SLUGS)>=1 ){
                System.out.println("SLUGS disponibles");
            }	

            // Change weapon?
            //changeWeaponByInventoryIndex(PlayerGun.MACHINEGUN)
        } 
        catch (JessException je) 
        {
            System.out.println(nf + "Error en la linea " + je.getLineNumber());
            System.out.println("Codigo:\n" + je.getProgramText());
            System.out.println("Mensaje:\n" + je.getMessage());
            System.out.println("Abortado");
            System.exit(1);
        }
        System.out.println("---------- Saliendo de " + nf);
    }


    /***
     * Select which weapon use. This function uses Jess.
     ***/
    private void selectWeapon()
    {
        String nf="=========== selectWeapon: ";
        System.out.println(nf + " ENTRANDO EN LA FUNCION");

        try{
            // Save distance to enemy.
            engine.store("DISTANCIA", new Value(enemyDistance, RU.FLOAT));
            
            // Save current health.
            int health = getHealth();
            engine.store("HEALTH", new Value(health, RU.INTEGER));
            
            // Print distance to enemy and current health.
            System.out.println("Distancia: " + enemyDistance + "  Salud: " + health);
//			engine.batch("armas_v03.clp");
            
            // TODO: Comment what this does.
            engine.assertString("(inicio)");
            engine.run();

            // Get Jess response.
            Value vsalida = engine.fetch("SALIDA");
            String salida = vsalida.stringValue(engine.getGlobalContext());
//			String salida = vsalida.stringValue(null);
            System.out.println("Jess me aconseja: " + salida);
            
            // Change weapon according to Jess' advice.
            if( salida.compareTo("Blaster") == 0 ){
                changeWeapon(PlayerGun.BLASTER);
            }else if( salida.compareTo("Shotgun") == 0 ){
                changeWeapon(PlayerGun.SHOTGUN);
            }else if( salida.compareTo("Grenades") == 0 ){
                changeWeapon(PlayerGun.GRENADES);
            }else if (salida.compareTo("Rocketlauncher") == 0){
                changeWeapon(PlayerGun.ROCKET_LAUNCHER);
            }else if (salida.compareTo("Chaingun") == 0){
                changeWeapon(PlayerGun.CHAINGUN);
            }
            if( salida.compareTo("Machinegun") == 0 ){
                changeWeapon(PlayerGun.MACHINEGUN);
            }
            if( salida.compareTo("Supershotgun") == 0 ){
                changeWeapon(PlayerGun.SUPER_SHOTGUN);
            }

        } catch (JessException je) {
            System.out.println(nf + "Error en la linea " + je.getLineNumber());
            System.out.println("Codigo:\n" + je.getProgramText());
            System.out.println("Mensaje:\n" + je.getMessage());
            System.out.println("Abortado");
            System.exit(1);
        }

        System.out.println(nf + " SALIENDO DE LA FUNCION");
    }


    /***
     * Print bot's current state.
     ***/
    private void printState()
    {
        // Health.
        System.out.println("Vida "+ player.getHealth());

        // TODO: Comment this.
        System.out.println("mi FRAGS " + player.getPlayerStatus().getStatus(PlayerStatus.FRAGS));

        // Get active weapon index.
        int aux=player.getWeaponIndex();
        // System.out.println("Indice arma actual: " + world.getInventory().getItemString(aux));
        // If active weapon is not the Blaster, print its ammo.
        if( aux!=PlayerGun.BLASTER ){
            System.out.println("Municion arma actual "+ player.getAmmo());
        }

        // Armor.
        System.out.println("Armadura "+ player.getArmor());
    }


    /***
     * Search for a visible weapon and run for it.
     * This function doesn't control whether the bot already has the
     * weapon or not.
     * @return true if a weapon was found and the bot moved to it.
     */
    private boolean findVisibleWeapon()
    {
        // Only works if we have information about the bot.
        if( player != null ){
            // Initializations.
            Entity nearestWeapon = null;
            Vector3f pos = null;
            Origin playerOrigin = null;
            pos = new Vector3f(0, 0, 0);

            // Get bot position.
            playerOrigin = player.getPlayerMove().getOrigin();
            pos.set( playerOrigin.getX(), playerOrigin.getY(), playerOrigin.getZ() );

            // Get the nearest weapon.
            nearestWeapon = this.getNearestWeapon( null );
            
            // Get the nearest enemy.
            // this.getNearestEnemy();

            // Did we find a weapon?
            if (nearestWeapon!=null){
                Vector3f weap = new Vector3f(nearestWeapon.getOrigin());
                Vector3f DirMov;

                DirMov = new Vector3f(0, 0, 0);

                // Check weapon visibility. Only allowed if we have got
                // information about the BSP tree.
                if (mibsp!=null){
                    if( mibsp.isVisible( weap, pos ) ){
                        System.out.println("Veo arma\n");

                        // Set a vector from the bot to the weapon.
                        DirMov.set(weap.x-pos.x, weap.y-pos.y, weap.z-pos.z);

                        // Normalize previous vector.
                        DirMov.normalize();

                        // Command the movement.
                        setBotMovement(DirMov, null, 200, PlayerMove.POSTURE_NORMAL);

                        // A movement has been decided; return true.
                        return true;
                    }						
                }
            }
        }

        // By default return false.
        return false;
    }


    /***
     * Search for a visible entity.
     * @return 
     */
    private boolean findEntity()
    {
        // Check if there is player information.
        if (player!=null){
            // Check if there is environment info.
            if (mibsp!=null){
                // Initializations.
                Entity nearestEntity = null;
                Entity tempEntity = null;
                Vector entities = null;
                Origin playerOrigin = null;
                Origin entityOrigin = null;
                Vector3f entPos; 
                Vector3f entDir;
                Vector3f pos = null;
                float entDist = Float.MAX_VALUE;

                // Bot position.
                pos = new Vector3f(0, 0, 0);
                entDir = new Vector3f(0, 0, 0);
                entPos = new Vector3f(0, 0, 0);

                // Bot position (kept in a Vector3f).
                playerOrigin = player.getPlayerMove().getOrigin();
                pos.set(playerOrigin.getX(), playerOrigin.getY(), playerOrigin.getZ());

                // Get information about entities.
                entities = world.getItems();
                //world.getOpponents();//Obtiene listado de enemigos

                // Print the number of entities.
                System.out.println("Entidades "+ entities.size());

                // Determine the most interesting entity, according
                // to its distance and visibility.
                for(int i = 0; i < entities.size(); i++){
                    // Get the current entity.
                    tempEntity = (Entity) entities.elementAt(i);

                    // Print the current entity's type (item, weapon, object or player).
                    System.out.println("Entidad de tipo "+ tempEntity.getCategory() + ", tipo " + tempEntity.getType() + ", subtipo " + tempEntity.getSubType());

                    // Get current entity's position.
                    entityOrigin = tempEntity.getOrigin();

                    // Initialize a vector with x and y (we don't care about z).
                    entPos.set(entityOrigin.getX(), entityOrigin.getY(), 0);

                    // Set a vector between entity and the player, projected in 2D.
                    entDir.sub(entPos, pos);

                    //Uso BSPPARSER para saber si la entidad y el observador se "ven", es decir no hay obstÃ¡culos entre ellos
                    // Get the position of the player and the entity.
                    Vector3f a = new Vector3f(playerOrigin);
                    Vector3f b = new Vector3f(entityOrigin);

                    // Check if the current entity is closer than the
                    // closest entity until now. Also check if it is visible
                    // from the player. If true, save current entity
                    // as the closest one.
                    if( (nearestEntity == null || entDir.length() < entDist) 
                        && entDir.length() > 0 
                        && mibsp.isVisible(a,b)){
                            nearestEntity = tempEntity;
                            entDist = entDir.length();
                    }
                }

                // Did we found a nearest entity?
                if(nearestEntity != null){
                    // Get the position of the nearest entity.
                    entityOrigin = nearestEntity.getOrigin();
                    entPos.set(entityOrigin.getX(), entityOrigin.getY(), 0);

                    // Set direction movement according to the selected entity
                    // and player's position.
                    entDir.sub(entPos, pos);
                    entDir.normalize();

                    // Move to the nearest entity.
                    //setBotMovement(entDir, null, 200, PlayerMove.POSTURE_NORMAL);
                    //return true;
                }				
            }					
        }

        return false;
    }


    /***
     * Search for a visible enemy.
     * @return true if an visible enemy was found and the bot attacked him/her.
     ***/
    private boolean findVisibleEnemy()
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
                Vector enemies = null;
                Origin playerOrigin = null;
                Origin enemyOrigin = null;
                Vector3f enPos; 
                Vector3f enDir;
                Vector3f pos = null;
                boolean NearestVisible=false;
                float enDist = Float.MAX_VALUE;

                // Bot position.
                pos = new Vector3f(0, 0, 0);
                enDir = new Vector3f(0, 0, 0);
                enPos = new Vector3f(0, 0, 0);

                // Bot position (save as a Vector3f).
                playerOrigin = player.getPlayerMove().getOrigin();
                pos.set(playerOrigin.getX(), playerOrigin.getY(), playerOrigin.getZ());

                // If we'd want to get the closest enemy...
                Entity enemy=null;
                // ... we'd have to uncomment this -->  enemy=this.getNearestEnemy();
                if (enemy!=null)
                    System.out.println("Hay enemigo cercano ");

                // Get information about all enemies.
                enemies = world.getOpponents();

                // Print number of enemies.
                System.out.println("Enemigos "+ enemies.size());

                // Get the most interesting enemy according to 2D distance and
                // visibility.
                for( int i = 0; i < enemies.size(); i++ ){
                    // Get current entity.
                    tempEnemy = (Entity) enemies.elementAt(i);

                    // Get current entity's position.
                    enemyOrigin = tempEnemy.getOrigin();

                    // Get enemy pos as a vector (we don't care about Z).
                    enPos.set(enemyOrigin.getX(), enemyOrigin.getY(),enemyOrigin.getZ());

                    // Set a 2D vector between entity and bot positions.
                    enDir.sub(enPos, pos);

                    // Get player and enemy positions as a Vector3f.
                    Vector3f a = new Vector3f(playerOrigin);
                    Vector3f b = new Vector3f(enemyOrigin);

                    // Check if current enemy is visible and neared than the
                    // nearest enemy found until now. If true, save it as the
                    // new closest enemy.
                    if((nearestEnemy == null || enDir.length() < enDist) && enDir.length() > 0 ){
                        nearestEnemy = tempEnemy;
                        enDist = enDir.length();

                        // Nearest enemy is visible.
                        if (mibsp.isVisible(a,b)){
                            NearestVisible=true;							
                        }else{
                            NearestVisible=false;
                        }

                    }
                } // for

                // Did we find a nearest enemy?
                if(nearestEnemy != null){
                    // Get tntity's position.
                    enemyOrigin = nearestEnemy.getOrigin();
                    enPos.set(enemyOrigin.getX(), enemyOrigin.getY(), enemyOrigin.getZ());

                    // Set movement direction according to the selected entity
                    // and bot position.
                    enDir.sub(enPos, pos);
                    //enDir.normalize();

                    if ( NearestVisible ){
                        // Nearest enemy is visible, attack!
                        System.out.println("Ataca enemigo ");
                        this.sendConsoleCommand("Modo ataque");

                        // Set weapon's angle.
                        Angles arg0=new Angles(enDir.x,enDir.y,enDir.z);
                        player.setGunAngles(arg0);

                        // Stop the movement and set attack mode.
                        setBotMovement(enDir, null, 0, PlayerMove.POSTURE_NORMAL);
                        setAction(Action.ATTACK, true);		
                        
                        // Distance to enemy (for the inference engine).
                        enemyDistance = enDist;
                        return true;
                    }else{
                        // Nearest enemy is not visible. Try to go to him/her.
                        System.out.println("Hay enemigo, pero no estÃ¡ visible ");
                        enemyDistance = Float.MAX_VALUE;
                    }
                } // End of if asking for nearest enemy.				
            } // End of if (mibsp!=null)
        } // End of if (player!=null)

        return false;
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