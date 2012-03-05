;;;; These examples are taken directly or derived from Nathan Marz' blog post
;;;; http://nathanmarz.com/blog/introducing-cascalog-a-clojure-based-query-language-for-hado.html


(ns sthuebner.cascalog.playground
  (:use cascalog.api
        cascalog.playground)
  (:require [cascalog.ops :as o]))

;; useful for working within Emacs
(bootstrap-emacs)


(comment

  ;;;; getting started

  ;; Persons younger than 30
  (?<- (stdout)                         ; where output goes to
       [?person]                        ; the fields written to output
       (age ?person ?age)               ; a generator with two variables
       (< ?age 30))                     ; an operation (here: a filter)


  
  ;;; Operations

  (?<- (stdout)
       [?person ?double-age]
       (age ?person ?age)
       (* ?age 2 :> ?double-age))

  ;; young people
  (?<- (stdout)
       [?person ?young]
       (age ?person ?age)
       (< ?age 30 :> ?young))


  
  ;;; Constant Substitution
  
  (?<- (stdout)
       [?person]
       (age ?person 25))                ; take only persons of age 25

  (?<- (stdout)
       [?person]
       (age ?person ?age)
       (* ?age 2 :> 50))                ; double age must be 50


  
  ;;; Joins

  ;; Persons' age and gender
  (?<- (stdout)
       [?person ?age ?gender]
       (age ?person ?age)
       (gender ?person ?gender))

  ;; Friend's gender
  (?<- (stdout)
       [?person ?followed ?gender]
       (follows ?person ?followed)
       (gender ?followed ?gender))

  ;; People following each other
  (?<- (stdout)
       [?person1 ?person2]
       (follows ?person1 ?person2)
       (follows ?person2 ?person1))

  ;; Younger followers
  (?<- (stdout)
       [?person ?friend]
       (follows ?person ?friend)
       (age ?person ?age1)
       (age ?friend ?age2)
       (< ?age2 ?age1))



  ;;; Aggregators

  ;; Number of people a person follows
  (?<- (stdout)
       [?person ?count]
       (follows ?person _)
       (o/count ?count))

  ;; Average age of friends
  (?<- (stdout)
       [?person ?avg]
       (follows ?person ?friend)
       (age ?friend ?age)
       (o/sum ?age :> ?sum)
       (o/count ?count)
       (div ?sum ?count :> ?avg))



  ;;; Custom Operations

  (defmapcatop split [^String sentence]
    (seq (.split sentence "\\s+")))
  
  (?<- (stdout)
       [?word ?count]
       (sentence ?s)
       (split ?s :> ?word)
       (o/count ?count))




  ;;; Sub Queries

  ;; People following popular people
  (let [many-followers (<- [?p]
                           (follows _ ?p)
                           (o/count ?count)
                           (> ?count 2))]
    (?<- (stdout)
         [?person ?friend]
         (follows ?person ?friend)
         (many-followers ?friend)
         ))

  )
