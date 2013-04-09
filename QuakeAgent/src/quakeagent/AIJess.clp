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

(deffacts initial-facts 
   "Hechos iniciales"
   (health 25)
   (n-visible-enemies 0)
)


/******************************************************************************
* TODO: Give this area a good name.
* These rulles will assert some facts or others indicating bot's and world's
* state.
******************************************************************************/


/*** How much health we have?  ***/

(defrule r-low-health
    "If health + armor is below 50, assert (low-health)"
    ?f <- ( health ?health&:(< ?health 50) )
    =>
    (printout t "Low health (< 50) -> RUN FOR LIFE!" crlf)
    (retract ?f)
    (assert (low-health))
)

(defrule r-medium-health
    "If health + armor is in range [50, 150], assert (medium-health)"
    ?f <- ( health ?health&:(>= ?health 50)&:(<= ?health 150) )
    =>
    (printout t "Medium health 2 [50, 150]" crlf)
    (retract ?f)
    (assert (medium-health))
)

(defrule r-high-health
    "If health + armor is above 150, assert (high-health)"
    ?f <- ( health ?health&:(> ?health 150) )
    =>
    (printout t "High health" crlf)
    (retract ?f)
    (assert (half-health))
)


/*** How much threat there is in the zone ***/

(defrule r-no-threat
   "If there's not visible enemy, there's no threat (we assert (no-threat))"
   ?f <- (n-visible-enemies 0)
   =>
   (printout t "No threat (n-visible-enemies 0)" crlf)
   (retract ?f)
   (assert (no-threat))
)


/******************************************************************************
* Decision area.
* These rules will asert one decision or another depending on current bot and
* world states.
******************************************************************************/

(defrule r-low-health-and-no-thread
   "We have low health and there is not thread in the zone. Search for life 
   or armor"
   (low-health)
   (no-threat)
   =>
   (printout t "Low health and no visible enemies -> RUN FOR LIFE" crlf )
)
