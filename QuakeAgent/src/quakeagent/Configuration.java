/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package quakeagent;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

/**
 *
 * @author usuario
 */
public class Configuration {
    static Properties configFie;
    
    public static void init() throws IOException{
        configFie = new Properties();
        FileInputStream in = new FileInputStream("src/quakeagent/config.properties");
        configFie.load(in);
        in.close();
    }
    
    public static String getProperty( String property ){
        return configFie.getProperty( property );
    }
}
