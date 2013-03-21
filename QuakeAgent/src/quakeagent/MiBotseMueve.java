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

/*
 * Every bot extends ObserverBot class.
 */
public final class MiBotseMueve extends ObserverBot
{
    //Variables 
    private World world = null;
    private Player player = null;

    private Vector3f PosPlayer= new Vector3f(0, 0, 0);

    // Bot previous position.
    private Vector3f prevPosPlayer = new Vector3f(0, 0, 0);

    // Bot movement.
    private int nsinavanzar = 0, velx = 50 ,vely = 50, cambios = 0;

    // Environment information.
    private BSPParser mibsp = null;

    // Distancia al enemigo que estamos atacando
    private float distanciaEnemigo = Float.MAX_VALUE;

    // Inference engine.
    private Rete engine;

    int dire = 0;


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
     * @param 
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
     * @param 
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
     * @param 
     ***/
    public MiBotseMueve(String botName, String botSkin, int recvRate, int msgLevel, int fov, int hand, String password, boolean highThreadSafety, boolean trackInv)
    {
            super((botName == null ? "MiBotseMueve" : botName), botSkin, recvRate, msgLevel, fov, hand, password, highThreadSafety, trackInv);
            initBot();
    }


    /*
     * Bot initialization.
     */
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

        // Print various information about the bot.
        System.out.println("Is Running? " + player.isRunning() + "\n");
        System.out.println("getPosition " + player.getPosition() + "\n");
        System.out.println("isAlive " + player.isAlive() + "\n");
        System.out.println("Arma visible?..." + findVisibleWeapon() + "\n");
        System.out.println("Entidad visible?..." + findEntity() + "\n");

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
        /*
        Origin playerOrigin = player.getPlayerMove().getOrigin();


        Vector3f a = new Vector3f(playerOrigin);
        Vector3f b = new Vector3f( playerOrigin.getX(), playerOrigin.getY()+10*(vely+1), playerOrigin.getZ() );

        if ( mibsp.isVisible(a,b) ){
            System.out.println( "Pa'lante 1" );
            vely = 1;
        }else{
            System.out.println( "Pa'tras 1" );
            vely = -1;
        }
        */

        // Print current position.
        System.out.println("PosiciÃ³n actual: ("+player.getPlayerMove().getOrigin().getX()+","+
                        player.getPlayerMove().getOrigin().getY()+","+
                        player.getPlayerMove().getOrigin().getZ()+")");	

        // Get the distance between previous and current positions.
        double dist = Math.sqrt(Math.pow(prevPosPlayer.y - player.getPlayerMove().getOrigin().getY(),2)+
                        Math.pow(prevPosPlayer.x - player.getPlayerMove().getOrigin().getX(),2));

        // We walked a small distance and it's no the first time we ask for it.
        if( dist < 5 && nsinavanzar>0 ){
            nsinavanzar++;

            // If we walked a small distance 5 consecutive times, change
            // movement.
            if( nsinavanzar>5 ){
                //Provoca un cambio de sentido en la direcciÃ³n y de la velocidad
                    vely = (int)(Math.random()*20)-10; 

                    //Resetea el nÃºmero de veces en que ha preguntado y no hubo movimiento
                    nsinavanzar=1;

                    //Incrementa el contador de cambios de direcciÃ³n
                    cambios++;

                    //Si es un nÃºmero par cambia tambiÃ©n el sentido de la velocidad en x
                    if (cambios == 2)
                    {
                            cambios = 0;
                            velx = (int)(Math.random()*20)-10;
                    }			

                    //Muestra la nueva direcciÃ³n de la velocidad
                    System.out.println("Cambio de direcciÃ³n de movimiento, x = " + velx + ", y = " + vely);
            }		

        }else{
            // Bot moved a good distance. Save current position.
            
            nsinavanzar=1;

            // Save current position as the previous one.
            prevPosPlayer.set(player.getPlayerMove().getOrigin().getX(),
                            player.getPlayerMove().getOrigin().getY(),
                            player.getPlayerMove().getOrigin().getZ());				
        }

        // Create a vector with the new movement direction.
        Vector3f DirMov = new Vector3f(velx, vely, 0);

        // Order the movement. If second argument is null, the bot looks at its
        // destiny. Otherwise, bot looks at the given direction.
        setBotMovement( DirMov, null, 200, PlayerMove.POSTURE_NORMAL );
        // We can set PlayerMove.POSTURE_DUCKED (LIEEEEE...)
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
            
            // Ammo for BFG10K y HYPERBLASTER.
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
            engine.store("DISTANCIA", new Value(distanciaEnemigo, RU.FLOAT));
            int health = getHealth();
            engine.store("HEALTH", new Value(health, RU.INTEGER));
            System.out.println("Distancia: " + distanciaEnemigo + "  Salud: " + health);
//			engine.batch("armas_v03.clp");
            engine.assertString("(inicio)");
            engine.run();

            Value vsalida = engine.fetch("SALIDA");
            String salida = vsalida.stringValue(engine.getGlobalContext());
//			String salida = vsalida.stringValue(null);
            System.out.println("Jess me aconseja: " + salida);
            // Cambia el arma en funcion del consejo dado por Jess
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
     * Print bot state
     ***/
    private void printState()
    {
        // Health.
        System.out.println("Vida "+ player.getHealth());

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
     * @return true if a weapon was found ant the bot moved to it.
     */
    private boolean findVisibleWeapon()
    {
        // Only if we have information about the bot.
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


    /*-------------------------------------------------------------------*/
    /**	Rutina que busca una entidad visible						     */
    /*-------------------------------------------------------------------*/
    private boolean findEntity()
    {

            //Hay informaciÃ³n del jugador disponible
            if (player!=null)
            {
                    //Hay informaciÃ³n del entorno disponible
                    if (mibsp!=null)
                    {
//				Variables
                            Entity nearestEntity = null;
                            Entity tempEntity = null;
                            Vector entities = null;
                            Origin playerOrigin = null;
                            Origin entityOrigin = null;
                            Vector3f entPos; 
                            Vector3f entDir;
                            Vector3f pos = null;
                            float entDist = Float.MAX_VALUE;

                            //PosiciÃ³n del bot
                            pos = new Vector3f(0, 0, 0);
                            entDir = new Vector3f(0, 0, 0);
                            entPos = new Vector3f(0, 0, 0);

                            //PosiciÃ³n del jugador que se almacena en un Vector3f
                            playerOrigin = player.getPlayerMove().getOrigin();
                            pos.set(playerOrigin.getX(), playerOrigin.getY(), playerOrigin.getZ());

//				Obtiene informaciÃ³n de las entidades
                            entities = world.getItems();
                            //world.getOpponents();//Obtiene listado de enemigos

                            //Muestra el nÃºmero de entidades disponibles
                            System.out.println("Entidades "+ entities.size());

                            //Determina la entidad mÃ¡s interesante siguiendo un criterio de distancia en 2D y visibilidad
                            for(int i = 0; i < entities.size(); i++)//Para cada entidad
                            {
                                    //Obtiene informaciÃ³n de la entidad actual
                                    tempEntity = (Entity) entities.elementAt(i);

                                    //Muestra la categorÃ­a ("items", "weapons", "objects", o "player")
                                    System.out.println("Entidad de tipo "+ tempEntity.getCategory() + ", tipo " + tempEntity.getType() + ", subtipo " + tempEntity.getSubType());

                                    //Obtiene la posiciÃ³n de la entidad que estÃ¡ siendo analizada
                                    entityOrigin = tempEntity.getOrigin();

                                    //Inicializa un Vector considerando sÃ³lo la x e y, es decir despreciando z
                                    entPos.set(entityOrigin.getX(), entityOrigin.getY(), 0);

                                    //Vector que une las posiciones de la entidad y el jugador proyectado en 2D
                                    entDir.sub(entPos, pos);

                                    //Uso BSPPARSER para saber si la entidad y el observador se "ven", es decir no hay obstÃ¡culos entre ellos
                                    Vector3f a = new Vector3f(playerOrigin);
                                    Vector3f b = new Vector3f(entityOrigin);

                                    //Si la entidad es visible (usando la informaicÃ³n del bsp) y su distancia menor a la mÃ­nima almacenada (o no habÃ­a nada almacenado), la almacena
                                    if((nearestEntity == null || entDir.length() < entDist) && entDir.length() > 0 && mibsp.isVisible(a,b))
                                    {
                                            nearestEntity = tempEntity;
                                            entDist = entDir.length();
                                    }
                            }//for

                                                            //Para la entidad seleccionada, calcula la direcciÃ³n de movimiento
                            if(nearestEntity != null)
                            {
                                    //PosiciÃ³n de la entidad
                                    entityOrigin = nearestEntity.getOrigin();
                                    entPos.set(entityOrigin.getX(), entityOrigin.getY(), 0);

                                    //DireciÃ³n de movimiento en base a la entidad elegida y la posiciÃ³n del jugador
                                    entDir.sub(entPos, pos);
                                    entDir.normalize();

                                    //Comanda el movimiento hacia la entidad selecionada
                                    //setBotMovement(entDir, null, 200, PlayerMove.POSTURE_NORMAL);
                                    //return true;
                            }				
                    }					
            }

            return false;

    }


    /*-------------------------------------------------------------------*/
    /**	Rutina que busca un enemigo visible							     */
    /*-------------------------------------------------------------------*/
    private boolean BuscaEnemigoVisible()
    {
            setAction(Action.ATTACK, false);

            //Hay informaciÃ³n del jugador disponible
            if (player!=null)
            {
                    //Hay informaciÃ³n del entorno disponible
                    if (mibsp!=null)
                    {
//				Variables
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

                            //PosiciÃ³n del bot
                            pos = new Vector3f(0, 0, 0);
                            enDir = new Vector3f(0, 0, 0);
                            enPos = new Vector3f(0, 0, 0);

                            //PosiciÃ³n del jugador que se almacena en un Vector3f
                            playerOrigin = player.getPlayerMove().getOrigin();
                            pos.set(playerOrigin.getX(), playerOrigin.getY(), playerOrigin.getZ());


                            //Si sÃ³lo queremos acceder al enemigo mÃ¡s cercano
                            Entity enemy=null;
// Tengo que descomentar esto -->  enemy=this.getNearestEnemy();//Obtiene el enemigo mÃ¡s cercano
                            if (enemy!=null)
                                    System.out.println("Hay enemigo cercano ");

//				Obtiene informaciÃ³n de todos los enemigos
                            enemies = world.getOpponents();

                            //Muestra el nÃºmero de enemigos disponibles
                            System.out.println("Enemigos "+ enemies.size());

                            //Determina el enemigo mÃ¡s interesante siguiendo un criterio de distancia en 2D y visibilidad
                            for(int i = 0; i < enemies.size(); i++)//Para cada entidad
                            {
                                    //Obtiene informaciÃ³n de la entidad actual
                                    tempEnemy = (Entity) enemies.elementAt(i);

                                    //Obtiene la posiciÃ³n de la entidad que estÃ¡ siendo analizada
                                    enemyOrigin = tempEnemy.getOrigin();

                                    //Inicializa un Vector considerando sÃ³lo la x e y, es decir despreciando z
                                    enPos.set(enemyOrigin.getX(), enemyOrigin.getY(),enemyOrigin.getZ());

                                    //Vector que une las posiciones de la entidad y el jugador proyectado en 2D
                                    enDir.sub(enPos, pos);

                                    //Uso BSPPARSER para saber si la entidad y el observador se "ven", es decir no hay obstÃ¡culos entre ellos
                                    Vector3f a = new Vector3f(playerOrigin);
                                    Vector3f b = new Vector3f(enemyOrigin);

                                    //Si la entidad es visible (usando la informaicÃ³n del bsp) y su distancia menor a la mÃ­nima almacenada (o no habÃ­a nada almacenado), la almacena
                                    if((nearestEnemy == null || enDir.length() < enDist) && enDir.length() > 0 )
                                    {
                                            nearestEnemy = tempEnemy;
                                            enDist = enDir.length();

                                            //Es visible el mÃ¡s cercano
                                            if (mibsp.isVisible(a,b))
                                            {
                                                    NearestVisible=true;							
                                            }
                                            else
                                            {
                                                    NearestVisible=false;
                                            }

                                    }
                            }//for

                            //Para la entidad seleccionada, calcula la direcciÃ³n de movimiento
                            if(nearestEnemy != null)
                            {
                                    //PosiciÃ³n de la entidad
                                    enemyOrigin = nearestEnemy.getOrigin();
                                    enPos.set(enemyOrigin.getX(), enemyOrigin.getY(), enemyOrigin.getZ());

                                    //DireciÃ³n de movimiento en base a la entidad elegida y la posiciÃ³n del jugador
                                    enDir.sub(enPos, pos);
                                    //enDir.normalize();

                                    if (NearestVisible)//Si es visible ataca
                                    {
                                            System.out.println("Ataca enemigo ");
                                            this.sendConsoleCommand("Modo ataque");

//						Ã�ngulo del arma
                                            Angles arg0=new Angles(enDir.x,enDir.y,enDir.z);
                                            player.setGunAngles(arg0);

//						Para el movimiento y establece el modo de ataque

                                            setAction(Action.ATTACK, true);		

                                            setBotMovement(enDir, null, 0, PlayerMove.POSTURE_NORMAL);
                                            // Distancia al enemigo (para el motor de inferencia)
                                            distanciaEnemigo = enDist;
                                            return true;
                                    }
                                    else//en otro caso intenta ir hacia el enemigo
                                    {
                                            System.out.println("Hay enemigo, pero no estÃ¡ visible ");
                                            distanciaEnemigo = Float.MAX_VALUE;
                                    }


                            }				
                    }					
            }

            return false;

    }


    /*-------------------------------------------------------------------*/
    /**	Rutina que indica la distancia a un obstÃ¡culo en una direcciÃ³n   */
    /*-------------------------------------------------------------------*/
    private void getObstacleDistance()
    {			
            //Crea un vestor en la direcciÃ³n de movimiento del bot
            Vector3f movDir = new Vector3f(player.getPlayerMove().getDirectionalVelocity().x, 
                                            player.getPlayerMove().getDirectionalVelocity().y,0.f);

            //Obtiene la distancia mÃ­nima a un obstÃ¡culo en esa direcciÃ³n
            float distmin = this.getObstacleDistance(movDir,2500.f);			

            //La muestra
            if (distmin!=Float.NaN)
            {
                    System.out.println("Distancia mmínima obstáculo " + distmin);
            }			
    }
}