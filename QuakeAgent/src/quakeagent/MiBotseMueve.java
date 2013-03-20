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


//Cualquier bot debe extender a la clase ObserverBot, para hacer uso de sus funcionalidades
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

/*-------------------------------------------------------------------*/
/**	Constructor que permite especificar el nombre y aspecto del bot
 *	@param botName Nombre del bot durante el juego
 *	@param botSkin Aspecto del bot */
/*-------------------------------------------------------------------*/
	public MiBotseMueve(String botName, String botSkin)
	{
		super((botName == null ? "MiBotseMueve" : botName), botSkin);
		initBot();
	}

/*-------------------------------------------------------------------*/
/**	Constructor que permite ade mÃ¡s de especificar el nombre y aspecto 
 *	del bot, indicar si Ã©ste analizarÃ¡ manualmente su inventario.
 *	@param botName Nombre del bot durante el juego
 *	@param botSkin Aspecto del bot
 *	@param trackInv Si true, El agente analizarÃ¡ manualmente su inventario */
/*-------------------------------------------------------------------*/
	public MiBotseMueve(String botName, String botSkin, boolean trackInv)
	{
		super((botName == null ? "MiBotseMueve" : botName), botSkin, trackInv);
		initBot();
	}

/*-------------------------------------------------------------------*/
/**	Constructor que permite ademÃ¡s de especificar el nombre y aspecto 
 *	del bot, indicar si Ã©ste analizarÃ¡ manualmente su inventario y
 *  si harÃ¡ uso de un hilo en modo seguro.
 *	@param botName Nombre del bot durante el juego
 *	@param botSkin Aspecto del bot
 *	@param highThreadSafety Si true, permite el modo de hilo seguro
 *	@param trackInv Si true, El agente analizarÃ¡ manualmente su inventario */
/*-------------------------------------------------------------------*/
	public MiBotseMueve(String botName, String botSkin, boolean highThreadSafety, boolean trackInv)
	{
		super((botName == null ? "MiBotseMueve" : botName), botSkin, highThreadSafety, trackInv);
		initBot();
	}

/*-------------------------------------------------------------------*/
/**	Constructor que permite ademÃ¡s de especificar el nombre, aspecto 
 *	del bot y la clave del servidor, indicar si Ã©ste analizarÃ¡ manualmente 
 *  su inventario y si harÃ¡ uso de un hilo en modo seguro.
 *	@param botName Nombre del bot durante el juego
 *	@param botSkin Aspecto del bot
 *	@param password clave del servidor
 *	@param highThreadSafety Si true, permite el modo de hilo seguro
 *	@param trackInv Si true, El agente analizarÃ¡ manualmente su inventario */
/*-------------------------------------------------------------------*/
	public MiBotseMueve(String botName, String botSkin, String password, boolean highThreadSafety, boolean trackInv)
	{
		super((botName == null ? "MiBotseMueve" : botName), botSkin, password, highThreadSafety, trackInv);
		initBot();
	}

/*-------------------------------------------------------------------*/
/**	Constructor que permite ademÃ¡s de especificar el nombre, aspecto 
 *	del bot, ratio de comunicaciÃ³n, tipo de mensajes y la clave del servidor,
 *  indicar si Ã©ste analizarÃ¡ manualmente 
 *  su inventario y si harÃ¡ uso de un hilo en modo seguro.
 *  @param botName Nombre del bot durante el juego
 *	@param botSkin Aspecto del bot
 *	@param recvRate Ratio de comunicaciÃ³n 
 *	@param msgLevel Tipo de mensajes
 *	@param fov Campo de visiÃ³n del agente
 *	@param hand Indica la mano en la que se lleva el arma
 *	@param password Clave del servidor
 *	@param highThreadSafety Si true, permite el modo de hilo seguro
 *	@param trackInv Si true, El agente analizarÃ¡ manualmente su inventario */
/*-------------------------------------------------------------------*/
	public MiBotseMueve(String botName, String botSkin, int recvRate, int msgLevel, int fov, int hand, String password, boolean highThreadSafety, boolean trackInv)
	{
		super((botName == null ? "MiBotseMueve" : botName), botSkin, recvRate, msgLevel, fov, hand, password, highThreadSafety, trackInv);
		initBot();
	}

	//InicializaciÃ³n del bot
	private void initBot()
	{		
		//Autorefresco del inventario
		this.setAutoInventoryRefresh(true);
                System.out.println("Working Directory = " +
              System.getProperty("user.dir"));
                
		try {
			engine = new Rete();

                        engine.batch( Configuration.getProperty( "clp_path" ) );
                        
                        engine.eval("(reset)");
                        engine.assertString("(color rojo)");
                        
                        engine.run();
			
                        Value v = engine.eval("?*VARGLOB*");
                        System.out.println(v.intValue(engine.getGlobalContext()));
		} catch (JessException je) {
			System.out.println("initBot: Error en la linea " + je.getLineNumber());
			System.out.println("Codigo:\n" + je.getProgramText());
			System.out.println("Mensaje:\n" + je.getMessage());
			System.out.println("Abortado");
                        //System.out.println( str );
			System.exit(1);
		}
	}

/*-------------------------------------------------------------------*/
/**	Rutina central del agente para especificar su comportamiento
 *	@param w Objeto de tipo World que contiene el estado actual del juego */
/*-------------------------------------------------------------------*/
        public void runAI(World w)
	{
            
            if (mibsp==null)
			mibsp = this.getBSPParser();
            
            System.out.println("AI...\n");
            //InformaciÃ³n del juego almacenada en una variable miembro
		world = w;
		
		//Obtiene informaciÃ³n del bot
		player = world.getPlayer();
                        
                System.out.println("Is Running? " + player.isRunning() + "\n");
                System.out.println("getPosition " + player.getPosition() + "\n");
                System.out.println("isAlive " + player.isAlive() + "\n");
                
                System.out.println("Arma visible?..." + BuscaArmaVisible() + "\n");
                System.out.println("Entidad visible?..." + BuscaEntidad() + "\n");
                    
                EstableceDirMovimiento();
				
                // Funciones auxiliares
                Estado();
                DistObs();
                
                setAction(Action.ATTACK, true);
                
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
       
	/*-------------------------------------------------------------------*/
	/**	Rutina que configura la direcciÃ³n de avance en el movimiento.    */
	/**	BÃ¡sicamente, si detecta que el bot no avanza durante un tiempo   */
	/**	cambia su direcciÃ³n de movimiento							     */
	/*-------------------------------------------------------------------*/
	private void EstableceDirMovimiento()
	{
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
            
                /*
		//Mostrar posiciÃ³n del bot
		System.out.println("PosiciÃ³n actual: ("+player.getPlayerMove().getOrigin().getX()+","+
				player.getPlayerMove().getOrigin().getY()+","+
				player.getPlayerMove().getOrigin().getZ()+")");	
                
                
		
		//Calcula la distancia desde la posiciÃ³n previa y la actual
		double dist = Math.sqrt(Math.pow(prevPosPlayer.y - player.getPlayerMove().getOrigin().getY(),2)+
				Math.pow(prevPosPlayer.x - player.getPlayerMove().getOrigin().getX(),2));
		
		//Si la distancia es baja y no es la primera vez que lo preguntamos (nsinavanzar vale 0 en ese caso)
		if (dist < 5 && nsinavanzar>0)
		{
			nsinavanzar++;
			
			//Tras 10 veces sin moverse cambia de sentido
			if (nsinavanzar>5)
			{
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
			
		}
		else// Se ha movido bastante en relaciÃ³n a la posiciÃ³n previa, guarda la posiciÃ³n actual
		{
			nsinavanzar=1;
			
			//Actualiza la que serÃ¡ la posiciÃ³n previa para la siguiente iteraciÃ³n
			prevPosPlayer.set(player.getPlayerMove().getOrigin().getX(),
					player.getPlayerMove().getOrigin().getY(),
					player.getPlayerMove().getOrigin().getZ());				
		}*/
		
		//Crea un vector con la nueva direcciÃ³n de movimiento
		Vector3f DirMov = new Vector3f(velx, vely, 0);
				
		//Comanda el movimiento, si el segundo parÃ¡metro es null mira al destino, 
		//en otro caso mira en la direcciÃ³n indicada
		setBotMovement(DirMov, null, 200, PlayerMove.POSTURE_NORMAL);
		//Otra postura, p.e. agachado PlayerMove.POSTURE_DUCKED
		
		//CÃ³digo ejemplo alternatico para mirar no al destino sino a una direcciÃ³n indicada
		//Vector3f DirVista = new Vector3f(0, 1, 0);
		//setBotMovement(DirMov, DirVista, 200, PlayerMove.POSTURE_NORMAL);
           
	}
	
	/*-------------------------------------------------------------------*/
	/**	Rutina que chequea las armas disponibles					     */
	/**	Cada arma tiene un tipo de municiÃ³n. La cantidad de municiÃ³n se  */
	/**	consulta de forma directa o a travÃ©s del arma				     */
	/*-------------------------------------------------------------------*/
	private void ListaArmamento()
	{
		String nf = "ListaArmamento";
		
		System.out.println("---------- Entrando en " + nf);
		try {
			// Limpia toda la informacion anterior
			engine.reset();
			
			if (world.getInventory().getCount(PlayerGun.BLASTER)>=1)
			{
				System.out.println("BLASTER");			
			}

			if (world.getInventory().getCount(PlayerGun.SHOTGUN)>=1)//Necesita shells
			{
				System.out.print("SHOTGUN");
				//Consultamos la municiÃ³n a travÃ©s del arma
				if (world.getInventory().getCount(PlayerGun.getAmmoInventoryIndexByGun(PlayerGun.SHOTGUN))>0)
				{
					System.out.print(" y municiones");
					engine.store("SHOTGUN", new Value(1, RU.INTEGER));
				}
				System.out.println("");
			}
			else
			{
				engine.store("SHOTGUN", new Value(0, RU.INTEGER));				
			}
			
			if (world.getInventory().getCount(PlayerGun.SUPER_SHOTGUN)>=1)//Necesita shells
			{
				System.out.print("SUPER_SHOTGUN");
				//Consultamos la municiÃ³n a travÃ©s del arma
				if (world.getInventory().getCount(PlayerGun.getAmmoInventoryIndexByGun(PlayerGun.SUPER_SHOTGUN))>0)
				{
					System.out.print(" y municiones");
					engine.store("SUPER_SHOTGUN", new Value(1, RU.INTEGER));
				}
				System.out.println("");
			}
			else
			{
				engine.store("SUPER_SHOTGUN", new Value(0, RU.INTEGER));
			}
			
			//Consulta SHELLS de forma directa
			if (world.getInventory().getCount(PlayerGun.SHELLS)>=1)//MuniciÃ³n para Shotgun y Supershotgun
			{
				System.out.println("SHELLS disponibles");
			}

			if (world.getInventory().getCount(PlayerGun.CHAINGUN)>=1)//Usa BULLETS
			{
				System.out.print("CHAINGUN");
				//Consultamos la municiÃ³n a travÃ©s del arma
				if (world.getInventory().getCount(PlayerGun.getAmmoInventoryIndexByGun(PlayerGun.CHAINGUN))>0)
				{
					System.out.print(" y municiones");	
					engine.store("CHAINGUN", new Value(1, RU.INTEGER));
				}
				System.out.println("");
			}
			else
			{
				engine.store("CHAINGUN", new Value(0, RU.INTEGER));				
			}
			
			if (world.getInventory().getCount(PlayerGun.MACHINEGUN)>=1)//Usa BULLETS
			{
				System.out.print("MACHINEGUN");
				//Consultamos la municiÃ³n a travÃ©s del arma
				if (world.getInventory().getCount(PlayerGun.getAmmoInventoryIndexByGun(PlayerGun.MACHINEGUN))>0)
				{
					System.out.print(" y municiones");	
					engine.store("MACHINEGUN", new Value(1, RU.INTEGER));
				}
				System.out.println("");
			}
			else
			{
				engine.store("MACHINEGUN", new Value(0, RU.INTEGER));				
			}
			
			if (world.getInventory().getCount(PlayerGun.BULLETS)>=1)//MuniciÃ³n para chaingun y machinegun
			{
				System.out.println("BULLETS disponibles");
			}

			if (world.getInventory().getCount(PlayerGun.GRENADE_LAUNCHER )>=1)//Usa GRENADES
			{
				System.out.println("GRENADE_LAUNCHER \n");
				//Consultamos la municiÃ³n a travÃ©s del arma
				if (world.getInventory().getCount(PlayerGun.getAmmoInventoryIndexByGun(PlayerGun.GRENADE_LAUNCHER ))>0)
					System.out.println("y municiones\n");	
			}	
			
			int cantidad = world.getInventory().getCount(PlayerGun.GRENADES);
			if (cantidad >= 1)//MuniciÃ³n para grenade launcher
			{
				System.out.println("GRENADES disponibles");
			}
			engine.store("GRENADES", new Value(cantidad, RU.INTEGER));				
			
			if (world.getInventory().getCount(PlayerGun.ROCKET_LAUNCHER )>=1)//Usa Rockets
			{
				System.out.println("ROCKET_LAUNCHER");
//				Consultamos la municiÃ³n a travÃ©s del arma
				if (world.getInventory().getCount(PlayerGun.getAmmoInventoryIndexByGun(PlayerGun.ROCKET_LAUNCHER ))>0)
				{
					System.out.println(" y municiones");
					engine.store("ROCKET_LAUNCHER", new Value(1, RU.INTEGER));
				}	
			}
			else
			{
				engine.store("ROCKET_LAUNCHER", new Value(0, RU.INTEGER));
			}
			if (world.getInventory().getCount(PlayerGun.ROCKETS )>=1)//MuniciÃ³n para ROCKET_LAUNCHER 
			{
				System.out.println("ROCKETS disponibles");
			}

			if (world.getInventory().getCount(PlayerGun.HYPERBLASTER)>=1)//Usa CELLS
			{
				System.out.println("HYPERBLASTER\n");
//				Consultamos la municiÃ³n a travÃ©s del arma
				if (world.getInventory().getCount(PlayerGun.getAmmoInventoryIndexByGun(PlayerGun.HYPERBLASTER))>0)
					System.out.println("y municiones\n");	
			}
			if (world.getInventory().getCount(PlayerGun.BFG10K)>=1)
			{
				System.out.println("BFG10K\n");
//				Consultamos la municiÃ³n a travÃ©s del arma
				if (world.getInventory().getCount(PlayerGun.getAmmoInventoryIndexByGun(PlayerGun.BFG10K))>0)
					System.out.println("y municiones\n");	
			}	
			if (world.getInventory().getCount(PlayerGun.CELLS)>=1)//MuniciÃ³n para BFG10K e HYPERBLASTER
			{
				System.out.println("CELLS disponibles");
			}

			if (world.getInventory().getCount(PlayerGun.RAILGUN)>=1)//Usa SLUGS
			{
				System.out.println("RAILGUN\n");
//				Consultamos la municiÃ³n a travÃ©s del arma
				if (world.getInventory().getCount(PlayerGun.getAmmoInventoryIndexByGun(PlayerGun.RAILGUN))>0)
					System.out.println("y municiones\n");	
			}
			if (world.getInventory().getCount(PlayerGun.SLUGS)>=1)//MuniciÃ³n para RAILGUN
			{
				System.out.println("SLUGS disponibles");
			}	

			//Una vez conocidas las armas disponibles y la situaciÃ³n, puede ser Ãºtil cambiar el arma activa
			//Para cambiar de arma p.e.
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
	
	
	
	/*-------------------------------------------------------------------*/
	/**	Rutina que decide que arma usar. Usa Jess                        */
	/*-------------------------------------------------------------------*/
	private void EscogeArma()
	{
		String nf="=========== EscogeArma: ";
		System.out.println(nf + " ENTRANDO EN LA FUNCION");

		try {

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
			if (salida.compareTo("Blaster") == 0)
			{
				changeWeapon(PlayerGun.BLASTER);
			} else
			if (salida.compareTo("Shotgun") == 0)
			{
				changeWeapon(PlayerGun.SHOTGUN);
			} else
			if (salida.compareTo("Grenades") == 0)
			{
				changeWeapon(PlayerGun.GRENADES);
			} else
			if (salida.compareTo("Rocketlauncher") == 0)
			{
				changeWeapon(PlayerGun.ROCKET_LAUNCHER);
			} else
			if (salida.compareTo("Chaingun") == 0)
			{
				changeWeapon(PlayerGun.CHAINGUN);
			}
			if (salida.compareTo("Machinegun") == 0)
			{
				changeWeapon(PlayerGun.MACHINEGUN);
			}
			if (salida.compareTo("Supershotgun") == 0)
			{
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
	} // EscogeArma
	
	


	
	
	
	/*-------------------------------------------------------------------*/
	/**	Rutina que reporta el estado del bot						     */
	/*-------------------------------------------------------------------*/
	private void Estado()
	{
		//Escribe la cantidad de actual
		System.out.println("Vida "+ player.getHealth());
		
		
		System.out.println("mi FRAGS " + player.getPlayerStatus().getStatus(PlayerStatus.FRAGS));
		
		//Muestra el Ã­ndice del arma activa
		int aux=player.getWeaponIndex();
		//System.out.println("Indice arma actual: " + world.getInventory().getItemString(aux));
		//Si el arma activa no es Blaster, escribe su nÃºmero de municiones
		if (aux!=PlayerGun.BLASTER) System.out.println("Municion arma actual "+ player.getAmmo());
		
		//Parea disponer de informaciÃ³n sobre las municiones
		System.out.println("Armadura "+ player.getArmor());
		
	}
	

	/*-------------------------------------------------------------------*/
	/**	Rutina que busca un arma visible, y se dirige hacia ella	     */
	/** No controla si ya el bot dispone de dicha arma					 */
	/*-------------------------------------------------------------------*/
	private boolean BuscaArmaVisible()
	{
		//Se aplica sÃ³lo si se dispone de informaciÃ³n del bot
		if (player!=null)
		{
			Entity nearestWeapon = null;
			Vector3f pos = null;
			Origin playerOrigin = null;
			
			//Inicializaciones
			pos = new Vector3f(0, 0, 0);
			
			//PosiciÃ³n del jugador que se almacena en un Vector3f
			playerOrigin = player.getPlayerMove().getOrigin();
			pos.set(playerOrigin.getX(), playerOrigin.getY(), playerOrigin.getZ());
			
			//Obtiene el arma mÃ¡s cercana 
			nearestWeapon = this.getNearestWeapon(null);
			//this.getNearestEnemy();//Obtiene el enemigo mÃ¡s cercano
			
			//Si no es nula
			if (nearestWeapon!=null)
			{
				Vector3f weap = new Vector3f(nearestWeapon.getOrigin());
				Vector3f DirMov;
				
				DirMov = new Vector3f(0, 0, 0);
				
				//Chequea la visibilidad del arma, sÃ³lo posible si disponemos de informaciÃ³n del Ã¡rbol BSP
				if (mibsp!=null)
				{
					//Si 
					if (mibsp.isVisible(weap, pos))
					{
						System.out.println("Veo arma\n");
						
						//Establece el vetor uniendo el bot y el arma, para indicar la direcciÃ³n que debe
						//seguir el bot en su movimiento
						DirMov.set(weap.x-pos.x, weap.y-pos.y, weap.z-pos.z);
			
						//Normaliza la direcciÃ³n a seguir
						DirMov.normalize();
						
						//Comanda el movimiento
						setBotMovement(DirMov, null, 200, PlayerMove.POSTURE_NORMAL);
						
						//Retorna true para indicar que ha establecido el movimiento
						return true;
					}						
				}
			}
		}
		
		//En cualquier otro caso retorna false
		return false;
	}
	
	
	/*-------------------------------------------------------------------*/
	/**	Rutina que busca una entidad visible						     */
	/*-------------------------------------------------------------------*/
	private boolean BuscaEntidad()
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
	private void DistObs()
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