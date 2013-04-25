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
    class BattleExperience {
        public int botLife;
        public int botAmmo;
        public int botWeapon;
    }
     * 
     */
   
    
    /*
    class EnemyInfo {
        public String name;
        public int lastKnownDPS;
    }
    */
    /*
     * Battle experience is saved in 4 matrixes, one for each strategic 
     * difference between the bot and the enemy when battle begun.
     */
    private int [][][] battleExperience = {
        { // Bot life (health + armor)
            { 0, 0, 0 },    // X < 25 % (wins, fails, unfinished)
            { 0, 0, 0 },    // 25% <= X < 50%
            { 0, 0, 0 },    // 50% <= X < 75%
            { 0, 0, 0 }     // 75% <= X
        },{ // Bot relative ammo
            { 0, 0, 0 },    // X < 25 % (wins, fails, unfinished)
            { 0, 0, 0 },    // 25% <= X < 50%
            { 0, 0, 0 },    // 50% <= X < 75%
            { 0, 0, 0 }     // 75% <= X
        },{ // Bot relative armament
            { 0, 0, 0 },    // X < 25 % (wins, fails, unfinished)
            { 0, 0, 0 },    // 25% <= X < 50%
            { 0, 0, 0 },    // 50% <= X < 75%
            { 0, 0, 0 }     // 75% <= X
        }
    };
    
    private int totalCases;
    private int[] totalResults = { 0, 0, 0 };
    
    /***
     * Init battle experience.
     */
    Viking(){
        totalCases = 0;
        for( int i=0; i<totalResults.length; i++ ){
            totalResults[i] = 0;
        }
        for( int i=0; i<battleExperience.length; i++ ){
            for( int j=0; j<battleExperience[i].length; j++ ){
                for( int k=0; k<battleExperience[i][j].length; k++ ){
                    battleExperience[i][j][k] = 0;
                }
            }
        }
    }
    
    /***
     * Load batte experience from file.
     * @param filePath : path to file with battle experience.
     */
    /*
    public void loadFromFile( String filePath ) throws FileNotFoundException, IOException{
        totalCases = 0;
        for( int i=0; i<3; i++ ){
            totalResults[i] = 0;
        }
        
        // http://stackoverflow.com/questions/2788080/reading-a-text-file-in-java
        int i = 0, j = 0;
        BufferedReader reader = new BufferedReader( new FileReader( filePath ) );
        String line = null;
        while ((line = reader.readLine()) != null) {
            if( (line.length() > 0) && Character.isDigit( line.charAt( 0 ) ) ){
                String[] parts = line.split("\t");
                
                battleExperience[i][j][0] = Integer.valueOf( parts[0] );
                battleExperience[i][j][1] = Integer.valueOf( parts[1] );
                battleExperience[i][j][2] = Integer.valueOf( parts[2] );
                
                
                totalResults[0] += battleExperience[i][j][0];
                totalResults[1] += battleExperience[i][j][1];
                totalResults[2] += battleExperience[i][j][2];
                
                if( j<3 ){
                    j++;
                }else{
                    j = 0;
                    i++;
                }
            }
        }
        totalCases += totalResults[0] + totalResults[1] + totalResults[2];
    }
    */
    
    
    /***
     * Prints, in human readable form, the bot's battle experience
     ***/
    public void printBattleExperience(){
        System.out.println( "totalCases: " + totalCases );
        System.out.println( "totalWins: " + totalResults[0] );
        System.out.println( "totalFails: " + totalResults[1] );
        System.out.println( "totalUnfinished: " + totalResults[2] );
        
        for( int i=0; i<battleExperience.length; i++ ){
            System.out.println( "Array:" );
            for( int j=0; j<battleExperience[i].length; j++ ){
                System.out.print( "(" );
                for( int k=0; k<battleExperience[i][j].length; k++ ){
                    System.out.print( battleExperience[i][j][k] + ", " );
                }
                System.out.println( ")" );
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
            // For each strategic valeu, classify it in one category
            // or another.
            int diffCategory = getStrategicValueCategory( diffArray[i] );
            
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
    
    private int getStrategicValueCategory( int diff )
    {
        if( diff < 25 ){
            // X < 25%
            return 0;
        }else if( (diff >= 25) && (diff < 50) ){
            // 25% <= X < 50%
            return 1;
        }else if( (diff >= 50) && (diff < 75) ){
            // 50% <= X < 75%
            return 2;
        }else{
            // X >= 75%
            return 3;
        }
    }
    
    public float getResultProbability( BattleResult battleResult )
    {
        switch( battleResult ){
            case WIN:
                return totalResults[0]/(float)totalCases;
            case FAIL:
                return totalResults[1]/(float)totalCases;
            default:
                return totalResults[2]/(float)totalCases;
        }
       
        
        /*
        int i, j, k;
                
        float res = (float) 0.0;
        
        for( i=0; i<4; i++ ){
            for( j=0; j<4; j++ ){
                for( k=0; k<3; k++ ){
                    
                }
            }
        }
        return;
         *
         */
    }
    
    
    public BattleResult attackEnemy( int[] diffArray )
    {
        float resultProbability = 0;
        int preferredResult = 0;
        float currentProbability = 0;
        int diffCategory;
        
        BattleResult[] battleResults = { BattleResult.WIN, BattleResult.FAIL, BattleResult.UNFINISHED };
        
        for( int i=0; i<3; i++ ){
            currentProbability = getResultProbability( battleResults[i] );
            System.out.println( "cp: " + currentProbability );
            for( int j=0; j<4; j++ ){
                diffCategory = getStrategicValueCategory( diffArray[j] );
                System.out.println( "diffCategory: " + diffCategory );
                
                System.out.println( "cp*: " + (battleExperience[j][diffCategory][i]/(float)totalResults[i]) );
                currentProbability *= battleExperience[j][diffCategory][i]/(float)totalResults[i];
            }
            
             System.out.println( "result: " + currentProbability );
             
             if( currentProbability > resultProbability ){
                 resultProbability = currentProbability;
                 preferredResult = i;
             }
        }
        
        switch( preferredResult ){
            case 0:
                System.out.println( "WIN" );
                return BattleResult.WIN;
            case 1:
                System.out.println( "FAIL" );
                return BattleResult.FAIL;
            default:
                System.out.println( "UNFINISHED" );
                return BattleResult.UNFINISHED;
        }
        
    }
}
