package quakeagent;

import java.io.IOException;
import java.util.Vector;
import java.util.Random;

import soc.qase.bot.ObserverBot;
import soc.qase.file.bsp.BSPParser;
import soc.qase.state.Player;
import soc.qase.state.PlayerMove;
import soc.qase.state.World;
import soc.qase.tools.vecmath.Vector3f;

import soc.qase.state.*;

import java.lang.Math;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import jess.*;

public class QuakeAgent {
        
    static MiBotseMueve MiBot,MiBot2;  
    
    public static void main(String[] args) throws IOException {
        // TODO code application logic here
        Init();	
    }
    
    public static void Init() throws IOException{		
        Configuration.init();
        
        //Establece la ruta del quake2, necesaria para tener información sobre los mapas.
        String quake2_path=Configuration.getProperty( "quake2_path" );
        System.setProperty("QUAKE2", quake2_path); 
        
        //Creación del bot (pueden crearse múltiples bots)
        MiBot = new MiBotseMueve("SoyBot","female/athena");

        //Conecta con el localhost (el servidor debe estar ya lanzado para que se produzca la conexión)
        MiBot.connect(getIpAddress(), 27910);//Ejemplo de conexión a la máquina local
    }
    
    public static String getIpAddress(){
        String res = "127.0.0.1";

        try{

            for(Enumeration ifaces = NetworkInterface.getNetworkInterfaces();ifaces.hasMoreElements();){
                NetworkInterface iface = (NetworkInterface)ifaces.nextElement();

                Enumeration nets = NetworkInterface.getNetworkInterfaces();
                for (Iterator it = Collections.list(nets).iterator(); it.hasNext();) {
                    NetworkInterface netint = (NetworkInterface) it.next();
                    Enumeration inetAddresses = netint.getInetAddresses();
                    for (Iterator it2 = Collections.list(inetAddresses).iterator(); it2.hasNext();) {
                        InetAddress inetAddress = (InetAddress) it2.next();
                        if (netint.getName().indexOf("eth0") != -1) {
                            res = inetAddress.toString();
                        }
                        if (netint.getName().indexOf("wlan0") != -1) {
                            res = inetAddress.toString();
                        }                
                    }
                }
            }
        }catch (SocketException e){
            System.out.println( "Error reading IP address" );
        }
        //Address is /address
        //so the / must be deleted
        res = res.split("/")[1].trim();    
        return res;
    }
}