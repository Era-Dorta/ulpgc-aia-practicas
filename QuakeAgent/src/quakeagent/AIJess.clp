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