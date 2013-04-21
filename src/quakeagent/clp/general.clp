; (printout t "Hola Mundo" crlf)

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
  
   ; Ammo (percentage)
   (slot ammo (type INTEGER) (default 0))

    ; Fire power (percentage)
    (slot fire-power (type INTEGER) (default 0))
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
* Global (for retrieving decisions)
******************************************************************************/

(defglobal
    ?*preferred-object* = "health"
)

/******************************************************************************
* Initial facts (for testing)
******************************************************************************/

/*
(deffacts initial-facts 
   "Hechos iniciales"
   
   (bot-state (health 100) (armor 51) (ammo 55) (fire-power 55) )
   
   ; (enemy (current-dps 45) (potential-dps 55) )
   ; (enemy (current-dps 32) (potential-dps 55) )
   ; (closest-entities invulnerability, health, armor, ammo)
   ; (alternatives)
)
*/

/******************************************************************************
* Decision area.
* These rules will asert one decision or another depending on current bot and
* world states.
******************************************************************************/

/*** How much life (health + armor) we have?  ***/

(defrule r-get-life
   "Get life = health + ammo"
   (declare (salience 100))
   
   ?f <- (bot-state (health ?health) (armor ?armor) (life -1) (ammo ?ammo) (fire-power ?fire-power) )
   =>
   (assert (bot-state (health ?health) (armor ?armor) (life (+ ?health ?armor) ) (ammo ?ammo) (fire-power ?fire-power) ))
   (retract ?f)
)

(defrule r-low-life
    "If life is below 50, assert (low-life)"
    (bot-state ( life ?life&:(< ?life 50) ))
    =>
    ; (printout t "Low life (< 50) (" ?life ")" crlf)
    (assert (low-life))
)

(defrule r-medium-life
    "If life is in range [50, 150], assert (medium-health)"
    (bot-state ( life ?life&:(>= ?life 50)&:(<= ?life 150) ))
    =>
    ; (printout t "Medium life [50, 150] (" ?life ")" crlf)
    (assert (medium-life))
)

(defrule r-high-life
    "If life is greater than 150, assert (high-health)"
    (bot-state ( life ?life&:(> ?life 150) ))
    =>
    ; (printout t "High life (> 150) (" ?life ")" crlf)
    (assert (high-life))
)

(defrule r-health-preferred
   "If health <= armor, prefer health"
   (bot-state (health ?health) (armor ?armor&:(<= ?health ?armor)) )
   =>
   ; (printout t "Health <= Armor" crlf)
   (assert (health-preferred))
)

(defrule r-armor-preferred
   "If health > armor, prefer armor"
    (bot-state (health ?health) (armor ?armor&:(> ?health ?armor)))
   =>
   ; (printout t "Health > Armor" crlf)
   (assert (armor-preferred))
)


/*** How much ammo (percentage) we have?  ***/

(defrule r-low-ammo
    "If ammo percentage is under 50%, assert (low-ammo)"
    (bot-state (ammo ?ammo&:(< ?ammo 50)))
    =>
    ; (printout t "Low ammo" crlf)
    (assert (low-ammo))
)

(defrule r-high-ammo
    "If ammo percentage is above 50%, assert (high-ammo)"
    (bot-state (ammo ?ammo&:(>= ?ammo 50)))
    =>
    ; (printout t "High ammo" crlf )
    (assert (high-ammo))
)

/*** How much fire power (percentage) we have?  ***/

(defrule r-low-fire-power
    "If fire power is under 50%, assert (low-fire-power)"
    (bot-state (fire-power ?fire-power&:(< ?fire-power 50)))
    =>
    ; (printout t "Low fire power" crlf)
    (assert (low-fire-power))
)

(defrule r-high-fire-power
    "If fire power is above 50%, assert (high-fire-power)"
    (bot-state (fire-power ?fire-power&:(>= ?fire-power 50)))
    =>
    ; (printout t "High fire power" crlf)
    (assert (high-fire-power))
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
    ; (printout t "Number of visible enemies: " ?c crlf)
    (assert (n-visible-enemies ?c))
)



/******************************************************************************
* Decision area.
* These rules will asert one decision or another depending on current bot and
* world states.
******************************************************************************/

/* Decisions for low life */

(defrule r-low-life-and-health-preferred
   (low-life)
   (health-preferred)
   =>
   ; (printout t "LOW LIFE & HEALTH PREFERRED -> GO FOR HEALTH" crlf)
   (bind ?*preferred-object* "health")
)

(defrule r-low-life-and-armor-preferred
   (low-life)
   (armor-preferred)
   =>
   ; (printout t "LOW LIFE & ARMOR PREFERRED -> GO FOR ARMOR" crlf) 
   (bind ?*preferred-object* "armor")
)


/* Decisions for medium life */

(defrule r-medium-life-and-low-ammo
    "We have medium life and low ammo. Go for ammo"
    (medium-life)
    (low-ammo)
    =>
    ; (printout t "MEDIUM LIFE & LOW AMMO -> GO FOR AMMO" crlf)
    (bind ?*preferred-object* "ammo")
)

(defrule r-medium-life-and-high-ammo-and-low-fire-power
    "We have medium life, high ammo and low fire power. Go for a better weapon"
    (medium-life)
    (high-ammo)
    (low-fire-power)
    =>
    ; (printout t "MEDIUM LIFE & HIGH AMMO & LOW-FIRE-POWER -> GO FOR A BETTER WEAPON" crlf)
    (bind ?*preferred-object* "weapon")
)

(defrule r-medium-life-and-high-ammo-and-high-fire-power-and-armor-preferred
    "We have medium life, high ammo, high fire and armor > life"
    (medium-life)
    (high-ammo)
    (high-fire-power)
    (armor-preferred)
    =>
    ; (printout t "MEDIUM LIFE & HIGH AMMO & HIGH FIRE POWER & ARMOR PREFERRED -> GO FOR ARMOR" crlf)
    (bind ?*preferred-object* "armor")
)

(defrule r-medium-life-and-high-ammo-and-high-fire-power-and-health-preferred
    "We have medium life, high ammo, high fire and armor > life"
    (medium-life)
    (high-ammo)
    (high-fire-power)
    (health-preferred)
    =>
    ; (printout t "MEDIUM LIFE & HIGH AMMO & HIGH FIRE POWER & HEALTH PREFERRED -> GO FOR HEALTH" crlf)
    (bind ?*preferred-object* "health")
)


/* Decisions for high life */

(defrule r-high-life-and-low-ammo
    "We have high life and low ammo. Go for ammo"
    (high-life)
    (low-ammo)
    =>
    ; (printout t "HIGH LIFE & LOW AMMO -> GO FOR AMMO" crlf)
    (bind ?*preferred-object* "ammo")
)

(defrule r-high-life-and-high-ammo-and-low-fire-power
    "We have medium life, high ammo and low fire power. Go for a better weapon"
    (high-life)
    (high-ammo)
    (low-fire-power)
    =>
    ; (printout t "HIGH LIFE & HIGH AMMO & LOW-FIRE-POWER -> GO FOR A BETTER WEAPON" crlf)
    (bind ?*preferred-object* "weapon")
)

(defrule r-high-life-and-high-ammo-and-high-fire-power
    "We have high life, high ammo and high fire"
    (high-life)
    (high-ammo)
    (high-fire-power)
    =>
    ; (printout t "HIGH LIFE & HIGH AMMO & HIGH FIRE POWER -> I'M GOOD" crlf)
    (bind ?*preferred-object* "nothing")
)



/*
(defrule r-medium-life-and-low-ammo
    (medium-life)
    (high-ammo)
    (armed-to-the-teeth)
    =>
    ; (printout t "MEDIUM LIFE & HIGH AMMO & ARMED-TO-THE-TEETH -> GO FOR AMMO" crlf)
)
*/

/*** For each enemy, calculate its threat level ***/

(defrule r-enemy-threat
    (enemy (health ?health) (current-dps ?current-dps) (potential-dps ?potential-dps) (threat ?threat&-1) )
    =>
    (bind ?threat (+ (* ?health 3) (* ?current-dps 2) (* ?potential-dps 1) ))
    ; (printout t "Enemy threat: " ?threat crlf )
)

(defrule r-low-health-and-no-thread
   "We have low health and there is no visible enemies. Search for life or
   armor"
   (low-health)
   (n-visible-enemies 2)
   =>
   ; (printout t "Low health and no visible enemies -> RUN FOR LIFE" crlf )
   (assert (decision low-health no-threat look-for-health))
)

(defrule r-low-health-and-no-thread
   "We have low health and there is no visible enemies. Search for life or
   armor"
   (low-health)
   =>
   ; (printout t "Low health and no visible enemies (2) -> RUN FOR LIFE" crlf )
   (assert (decision low-health no-threat look-for-health))
)

/*
(defrule r-facts
    (declare (salience -50))
    =>
    (facts)
)
*/