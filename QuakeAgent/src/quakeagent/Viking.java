/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package quakeagent;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

/**
 *
 * @author moises
 */

public class Viking {
    /*
     * Battle experience is saved in 4 matrixes, one for each strategic 
     * difference between the bot and the enemy when battle begun.
     */
    private int [][][] battleExperience = {
        { // Life difference between bot and enemy.
            { 0, 0, 0 },    // Bot life << Enemy life (wins, fails, unfinished)
            { 0, 0, 0 },    // Bot life < Enemy life
            { 0, 0, 0 },    // Bot life >= Enemy life
            { 0, 0, 0 }     // Bot life >> Enemy life
        },{ // Ammo difference between bot and enemy.
            { 0, 0, 0 },    // Bot ammo << Enemy ammo (wins, fails, unfinished)
            { 0, 0, 0 },    // Bot ammo < Enemy ammo
            { 0, 0, 0 },    // Bot ammo >= Enemy ammo
            { 0, 0, 0 }     // Bot ammo >> Enemy ammo
        },{ // DPS difference between bot and enemy.
            { 0, 0, 0 },    // Bot dps << Enemy dps (wins, fails, unfinished)
            { 0, 0, 0 },    // Bot dps < Enemy dps
            { 0, 0, 0 },    // Bot dps >= Enemy dps
            { 0, 0, 0 }     // Bot dps >> Enemy dps
        },{ // Potencial DPS difference between bot and anemy.
            { 0, 0, 0 },    // Bot potential dps << Enemy potential dps (wins, fails, unfinished)
            { 0, 0, 0 },    // Bot potential dps < Enemy potential dps
            { 0, 0, 0 },    // Bot potential dps >= Enemy potential dps
            { 0, 0, 0 }     // Bot potential dps >> Enemy potential dps
        }
    };
    
    
    /***
     * Load batte experience from file.
     * @param filePath : path to file with battle experience.
     */
    public void loadFromFile( String filePath ) throws FileNotFoundException, IOException{
        // http://stackoverflow.com/questions/2788080/reading-a-text-file-in-java
        int i = 0, j = 0, k = 0;
        BufferedReader reader = new BufferedReader( new FileReader( "/home/moises/array.txt" ) );
        String line = null;
        while ((line = reader.readLine()) != null) {
            if( (line.length() > 0) && Character.isDigit( line.charAt( 0 ) ) ){
                String[] parts = line.split("\t");
                for( String part : parts ){
                    battleExperience[i][j][k] = Integer.valueOf( part );
                    k++;
                }
                k = 0;
                if( j<3 ){
                    j++;
                }else{
                    j = 0;
                    i++;
                }
            }
        }
    }

    
    /***
     * Prints, in human readable form, the bot's battle experience
     ***/
    public void printBattleExperience(){
        int i=0, j=0, k = 0;
        
        for( i=0; i<4; i++ ){
            System.out.println( "Array:" );
            for( j=0; j<4; j++ ){
                for( k=0; k<3; k++ )
                System.out.println( battleExperience[i][j][k] );
            }
        }
    }
    
    
    /***
     * Add a battle experience to the database.
     * @param diffArray : array of strategic differences (life difference, 
     * ammo difference, etc) between bot and enemy when the battle begun.
     * @param result : Battle result (WIN, FAIL or UNFINISHED).
     */
    public void addBattleExperience( int[] diffArray, BattleResult result ){
        int i = 0;
        for( i=0; i<diffArray.length; i++ ){
            // For each strategic difference, classify it in one category
            // or another.
            int diffCategory = 0;
            if( diffArray[i] < -25 ){
                // Bot << Enemy
                diffCategory = 0;
            }else if( (diffArray[i] >= -25) && (diffArray[i] < 0) ){
                // Bot < Enemy
                diffCategory = 1;
            }else if( (diffArray[i] >= 0) && (diffArray[i] < 25) ){
                // Bot >= Enemy
                diffCategory = 2;
            }else{
                // Bot >> Enemy
                diffCategory = 3;
            }
            
            // Sum the battle result in its appropiate matrix and array.
            switch( result ){
                case WIN:
                    battleExperience[i][diffCategory][0]++;
                break;
                case FAIL:
                    battleExperience[i][diffCategory][1]++;
                break;
                case UNFINISHED:
                    battleExperience[i][diffCategory][2]++;
                break;
            }
        }
    }
    
    public boolean attackEnemy( int[] diffArray ){
        
        
        return true;
    }
}
