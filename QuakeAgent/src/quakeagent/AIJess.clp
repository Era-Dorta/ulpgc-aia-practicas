(printout t "Hola Mundo" crlf)

/******************************************************************************
* Templates
******************************************************************************/

(deftemplate bot-state
   "Bot state (health, ammo, ...)"
   
   ; Health
   (slot health (type INTEGER) (default 0))
   
   ; Armor
   (slot armor (type INTEGER) (default 0))
   
   ; Life (health + armor)
   (slot life (type INTEGER) (default -1))
)

(deftemplate enemy
    "An enemy"

    ; Name
    (slot name (type STRING))

    ; Health + armor
    (slot health (type INTEGER) (default 100))
   
    ; Is the enemy atacking the bot (or the team)?
    (slot attacking (type INTEGER) (default 0))

    ; How much damage per second does its current weapon?
    (slot current-dps (type INTEGER)) 

    ; How much damage per second does its most powerful weapon?
    (slot potential-dps (type INTEGER))

    ; Has it got invulnerability?
    (slot untouchable (type INTEGER) (default 0))

    ; Threat a value we give to the enemy according to its life, dps, etc.
    (slot threat (type INTEGER) (default -1))
)


/******************************************************************************
* Initial facts (for testing)
******************************************************************************/

(deffacts initial-facts 
   "Hechos iniciales"
   
   (bot-state (health 25))
   
   ; (enemy (current-dps 45) (potential-dps 55) )
   ; (enemy (current-dps 32) (potential-dps 55) )
   ; (closest-entities invulnerability, health, armor, ammo)
   ; (alternatives)
)


/******************************************************************************
* Decision area.
* These rules will asert one decision or another depending on current bot and
* world states.
******************************************************************************/

/*** How much life (health + armor) we have?  ***/

(defrule r-get-life
   "Get life = health + ammo"
   (declare (salience 100))
   
   ?f <- (bot-state (health ?health) (armor ?armor) (life -1))
   =>
   (assert (bot-state (health ?health) (armor ?armor) (life (+ ?health ?armor) )))
   (retract ?f)
)

(defrule r-low-life
    "If life is below 50, assert (low-life)"
    (bot-state ( life ?life&:(< ?life 50) ))
    =>
    (printout t "Low life (< 50) (" ?life ")" crlf)
    (assert (low-life))
)

/*
(defrule r-medium-life
    "If life is in range [50, 150], assert (medium-health)"
    (bot-state ( life ?life&:(>= ?life 50)&:(<= ?life 150) ))
    =>
    (printout t "Medium life [50, 150] (" ?life ")" crlf)
    (assert (medium-life))
)

(defrule r-high-life
    "If life is greater than 150, assert (high-health)"
    (bot-state ( life ?life&:(> ?life 150) ))
    =>
    (printout t "High life (> 150) (" ?life ")" crlf)
    (assert (high-life))
)
*/
(defrule r-health-preferred
   "If health <= armor, prefer health"
   (bot-state (health ?health) (armor ?armor&:(<= ?health ?armor)) )
   =>
   (printout t "Health <= Armor" crlf)
   (assert (health-preferred))
)

(defrule r-armor-preferred
   "If health > armor, prefer armor"
    (bot-state (health ?health) (armor ?armor&:(> ?health ?armor)))
   =>
   (printout t "Health > Armor" crlf)
   (assert (armor-preferred))
)

/*** How many visible enemies are ***/

(defrule r-count-visible-enemies
    "Count visible enemies"
    ?c <- (accumulate (bind ?count 0)
            (bind ?count (+ ?count 1)) 
            ?count
            (enemy)
          ) 
=>
    (printout t "Number of visible enemies: " ?c crlf)
    (assert (n-visible-enemies ?c))
)



/******************************************************************************
* Decision area.
* These rules will asert one decision or another depending on current bot and
* world states.
******************************************************************************/

(defrule r-low-life-and-health-preferred
   (low-life)
   (health-preferred)
   =>
   (printout t "LOW LIFE & HEALTH PREFERRED -> GO FOR HEALTH" crlf) 
)

(defrule r-low-life-and-armor-preferred
   (low-life)
   (armor-preferred)
   =>
   (printout t "LOW LIFE & ARMOR PREFERRED -> GO FOR ARMOR" crlf) 
)

/*** For each enemy, calculate its threat level ***/

(defrule r-enemy-threat
    (enemy (health ?health) (current-dps ?current-dps) (potential-dps ?potential-dps) (threat ?threat&-1) )
    =>
    (bind ?threat (+ (* ?health 3) (* ?current-dps 2) (* ?potential-dps 1) ))
    (printout t "Enemy threat: " ?threat crlf )
)

(defrule r-low-health-and-no-thread
   "We have low health and there is no visible enemies. Search for life or
   armor"
   (low-health)
   (n-visible-enemies 2)
   =>
   (printout t "Low health and no visible enemies -> RUN FOR LIFE" crlf )
   (assert (decision low-health no-threat look-for-health))
)

(defrule r-low-health-and-no-thread
   "We have low health and there is no visible enemies. Search for life or
   armor"
   (low-health)
   =>
   (printout t "Low health and no visible enemies (2) -> RUN FOR LIFE" crlf )
   (assert (decision low-health no-threat look-for-health))
)

