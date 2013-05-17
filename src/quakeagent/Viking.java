/***
 * Class Viking
 * Bot AI module which is queried when a battle begins. It has two main 
 * functions:
 *  - Recording battle experience once the battle has finished 
 *  (battle experience = bot state when battle begun + battle result)
 *  - Predicting battle result given the bot current state and battle 
 *  experience. The prediction is done by using a Naive Bayes classifier.
 ***/

package quakeagent;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

public class Viking {
    // Possible results for a battle.
    public static final int WIN = 0;
    public static final int FAIL = 1;
    
    // Bot state variables used in battle result prediction.
    public static final int LIFE = 0;
    public static final int REL_AMMO = 1;
    public static final int REL_ARMAMENT = 2;
    
    /*
     * If a battle is predicted to be lost, bot would keep avoiding it ad 
     * infinitum. The following counter and threshold will force bot to 
     * fight sometimes, even when a bad prediction is given.
     */
    int nCowardMoments = 0;
    static final int maxCowardMoments = 10;
   
    // Total number of cases / battles recorded.
    private int totalCases;
    
    // Total number of battle results {wins, fails}
    private int[] totalResults = { 0, 0 };
    
    /*
     * Battle experience is saved in n matrixes, one for each strategic 
     * value (bot life, bot ammo, etc) recorded when battle begun.
     */
    private int [][][] battleExperience = {
        { // Bot life (health + armor)
            { 0, 0 },    // X < 25 % (wins, fails)
            { 0, 0 },    // 25% <= X < 50%
            { 0, 0 },    // 50% <= X < 75%
            { 0, 0 }     // 75% <= X
        },{ // Bot relative ammo
            { 0, 0 },    // X < 25 % (wins, fails)
            { 0, 0 },    // 25% <= X < 50%
            { 0, 0 },    // 50% <= X < 75%
            { 0, 0 }     // 75% <= X
        },{ // Bot relative armament
            { 0, 0 },    // X < 25 % (wins, fails)
            { 0, 0 },    // 25% <= X < 50%
            { 0, 0 },    // 50% <= X < 75%
            { 0, 0 }     // 75% <= X
        }
    };
    
    
    /***
     * Init battle experience to zero.
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
     * NOT USED.
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
     * @param botState : array with bot state (life, ammo and fire power) when
     * battle begun.
     * @param result : Battle result (WIN, FAIL).
     ***/
    public void addBattleExperience( int[] botState, int result )
    {
        int i = 0;
        for( i=0; i<botState.length; i++ ){
            // For each strategic value, classify it in one category
            // or another.
            int category = getStrategicValueCategory( botState[i] );
            
            // Sum the battle result in its appropiate matrix and array.
            if( result == WIN ){
                battleExperience[i][category][0]++;
            }else{
                battleExperience[i][category][1]++;
            }
        }
        
        totalCases++;
        totalResults[result]++;
    }
    
    
    /***
     * Before being recorded or queried, each strategic value percentage 
     * (bot life, ammo and firepower) is classified in one category or another,
     * so the space for the bayes classifier can be simpliffied.
     ***/
    private int getStrategicValueCategory( int value )
    {
        if( value < 25 ){
            // X < 25%
            return 0;
        }else if( (value >= 25) && (value < 50) ){
            // 25% <= X < 50%
            return 1;
        }else if( (value >= 50) && (value < 75) ){
            // 50% <= X < 75%
            return 2;
        }else{
            // X >= 75%
            return 3;
        }
    }
    
    
    /***
     * Returns an array with the total battle results (wins, fails).
     ***/
    public int[] getBattleResults()
    {
        return totalResults;
    }
    
    
    /***
     * Get ratio bettwen the given battle result and the total number of 
     * battles in bot experience.
     ***/
    public float getResultProbability( int battleResult )
    {
        if( battleResult == WIN ){
            return totalResults[WIN]/(float)totalCases;
        }else{
            return totalResults[FAIL]/(float)totalCases;
        }
    }
    
    
    /***
     * Get the expected battle result, given the current bot's life, relative
     * ammo and relative fire power.
     ***/
    public int getExpectedBattleResult( int life, int relAmmo, int relFirePower )
    {
        int [] strategicValues = { life, relAmmo, relFirePower };
        float resultProbability = 0;
        int preferredResult = 0;
        float currentProbability = 0;
        int category;
        
        int[] battleResults = { WIN, FAIL };
        
        // Iterate over each possible battle result.
        for( int i=0; i<battleResults.length; i++ ){
            // Get P(battleResult)
            currentProbability = getResultProbability( battleResults[i] );
            
            // Iterate over each strategic value matrix.
            for( int j=0; j<battleExperience.length; j++ ){
                // Get the category for the current strategic value.
                category = getStrategicValueCategory( strategicValues[j] );
                
                // Get P(strategic_value|battleResult).
                if( totalResults[i] != 0 ){
                    currentProbability *= battleExperience[j][category][i]/(float)totalResults[i];
                }
            }
            
            // Get the minimum probability until now.
            if( currentProbability > resultProbability ){
                resultProbability = currentProbability;
                preferredResult = i;
            }
        }
        
        // Return the predicted result.
        if( preferredResult == 0 ){
            return WIN;
        }else{
            /*
             * If a battle is predicted to be lost increment a "coward" 
             * counter. If it reaches a given threshold, lie to bot by giving
             * it a good battle prediction.
             */
            nCowardMoments++;
            if( nCowardMoments < maxCowardMoments ){
                return FAIL;
            }else{
                nCowardMoments = 0;
                return WIN;
            }
        }
    }
}
