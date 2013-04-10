(printout t "Hola Mundo" crlf)

/*
(defglobal 
    ?*VARGLOB* = 123
)

(defrule r1
    (color rojo)
    =>
    (printout t "Bien! color rojo" crlf)
)

(defrule r2
    (color azul)
    =>
    (printout t "Bien! color azul" crlf)
    (bind ?*VARGLOB* 456)
)
*/


/******************************************************************************
* Templates
******************************************************************************/

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

    ; Threat (a value we give to the enemy according to its life, dps, etc.
    (slot threat (type INTEGER) (default -1))
)


/******************************************************************************
* Initial facts (for testing)
******************************************************************************/

(deffacts initial-facts 
   "Hechos iniciales"
   (health 25)
   (enemy (current-dps 45) (potential-dps 55) )
   (enemy (current-dps 32) (potential-dps 55) )
)


/******************************************************************************
* Preprocesing area.
* These rulles will take environment variables (such as bot health) and, 
* according to them, assert "normalized" facts (ie. high-health) for its use
* at the decision area.
******************************************************************************/

/*** How much health we have?  ***/

(defrule r-low-health
    "If health is below 30, assert (low-health)"
    ?f <- ( health ?health&:(< ?health 30) )
    =>
    (printout t "Low health (< 30)" crlf)
    (retract ?f)
    (assert (low-health))
)


(defrule r-medium-health
    "If health is in range [30, 60], assert (medium-health)"
    ?f <- ( health ?health&:(>= ?health 30)&:(<= ?health 60) )
    =>
    (printout t "Medium health [30, 60]" crlf)
    (retract ?f)
    (assert (medium-health))
)


(defrule r-high-health
    "If health is in range [61, 99], assert (high-health)"
    ?f <- ( health ?health&:(>= ?health 61)&:(<= ?health 99) )
    =>
    (printout t "High health [61, 99]" crlf)
    (retract ?f)
    (assert (high-health))
)

(defrule r-full-health
    "If health is equal or greater than 100, assert (full-health)"
    ?f <- ( health ?health&:(>= ?health 100) )
    =>
    (printout t "Full health (>= 100)" crlf)
    (retract ?f)
    (assert (full-health))
)


/*** How much armor we have?  ***/

(defrule r-low-armor
    "If health is below 30, assert (low-armor)"
    ?f <- ( armor ?armor&:(< ?armor 30) )
    =>
    (printout t "Low armor (< 30)" crlf)
    (retract ?f)
    (assert (low-armor))
)


(defrule r-medium-armor
    "If armor is in range [30, 60], assert (medium-armor)"
    ?f <- ( armor ?armor&:(>= ?armor 30)&:(<= ?armor 60) )
    =>
    (printout t "Medium armor [30, 60]" crlf)
    (retract ?f)
    (assert (medium-armor))
)


(defrule r-high-armor
    "If armor is in range [61, 99], assert (high-armor)"
    ?f <- ( armor ?armor&:(>= ?armor 61)&:(<= ?armor 99) )
    =>
    (printout t "High armor [61, 99]" crlf)
    (retract ?f)
    (assert (high-armor))
)

(defrule r-full-armor
    "If armor is equal or greater than 100, assert (full-armor)"
    ?f <- ( armor ?armor&:(>= ?armor 100) )
    =>
    (printout t "Full armor (>= 100)" crlf)
    (retract ?f)
    (assert (full-armor))
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


/*** For each enemy, calculate its threat level ***/

(defrule r-enemy-threat
    (enemy (health ?health) (current-dps ?current-dps) (potential-dps ?potential-dps) (threat ?threat&-1) )
    =>
    (bind ?threat (+ (* ?health 3) (* ?current-dps 2) (* ?potential-dps 1) ))
    (printout t "Enemy threat: " ?threat crlf )
)


/******************************************************************************
* Decision area.
* These rules will asert one decision or another depending on current bot and
* world states.
******************************************************************************/

(defrule r-low-health-and-no-thread
   "We have low health and there is no visible enemies. Search for life or
   armor"
   (health ?health&:(< ?health 50) )
   (n-visible-enemies 0)
   =>
   (printout t "Low health and no visible enemies -> RUN FOR LIFE" crlf )
   (assert (decision low-health no-threat look-for-health))
)
