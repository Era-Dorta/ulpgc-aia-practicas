/***
 * KillBot agent has a configuration file with absolute paths to auxiliar 
 * files (waypoints map, Jess file, etc). This configuration file is read by
 * using this class.
***/

package quakeagent;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;


public class Configuration {
    static Properties configFie;
    
    /***
     * Open and load configuration file.
     */
    public static void init() throws IOException{
        configFie = new Properties();
        FileInputStream in = new FileInputStream("src/quakeagent/config.properties");      
        configFie.load(in);
        in.close();
    }
   
    
    /***
     * Configuration file is a set of pairs (variable, valor). By this method
     * we get the value for a given variable 'property'.
     * @param property : variable whose value we want to retrieve.
     * @return value for the given property.
     */
    public static String getProperty( String property ){
        return configFie.getProperty( property );
    }
}
