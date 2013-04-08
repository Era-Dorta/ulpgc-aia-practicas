(printout t "Hola Mundo" crlf)

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

/******************************************************************************
* Decision area.
* We'll feed Jess with facts from Java and wait for a decision. The facts 
* will inform Jess about the state of the world and the bot.
* For example: (health 50).
******************************************************************************/

;; Assert a fact indicating whether bot's health + armor is low, medium or 
;; high.

; If bot's health + armor is below 50, we'll asert (low-health).
(defrule r-low-health
    ?f <- ( health ?health&:(< ?health 50) )
    =>
    (printout t "Low health (< 50) -> RUN FOR LIFE!" crlf)
    (retract ?f)
    (assert (low-health))
)

; If bot's health + armor is between 50 and 150, we'll asert (medium-health).
(defrule r-medium-health
    ?f <- ( health ?health&:(and (>= ?health 50) (<= ?health 150) ) )
    =>
    (printout t "Medium health [50, 150]" crlf)
    (retract ?f)
    (assert (medium-health))
)

; If bot's health + armor is above 150, we'll asert (high-health).
(defrule r-high-health
    ?f <- ( health ?health&:(> ?health 150) )
    =>
    (printout t "High health" crlf)
    (retract ?f)
    (assert (half-health))
)

