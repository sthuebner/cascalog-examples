;; These examples are taken directly or derived from Nathan Marz' blog post
;; http://nathanmarz.com/blog/introducing-cascalog-a-clojure-based-query-language-for-hado.html

(ns sthuebner.cascalog.playground
  (:use cascalog.api
        cascalog.playground
        [midje sweet cascalog])
  (:require [cascalog.ops :as c]))


(comment
  ;; getting started





  ;; people younger than 30
  (?<- (stdout)                             ; destination
       [?person ?age]                       ; output vars
       (age ?person ?age)                   ; generator
       (< ?age 30))                         ; filter


  ;; compute new values
  (?<- (stdout)
       [?person ?double-age]
       (age ?person ?age)
       
       (* ?age 2 :> ?double-age))


  ;; young vs. not-young people
  (?<- (stdout)
       [?person ?young]
       (age ?person ?age)
       (< ?age 30 :> ?young))           ; ?young = true | false




  



;;; Constrain with constants

  
  ;; constrain on an input vars
  (?<- (stdout)
       [?person]
       (age ?person 25))


  
  ;; constrain on a function's result
  (?<- (stdout)
       [?person]
       (age ?person ?age)
       (* ?age 2 :> 50))

  

  



  
  
;;; Joins

  ;; people with their age and gender
  (?<- (stdout)
       [?person ?age ?gender]
       
       (age     ?person   ?age)
       (gender  ?person   ?gender))





  ;; a person's friends with their gender
  (?<- (stdout)
       [?person           ?followed   ?gender]
       
       (follows ?person    ?followed )
       (gender  ?followed  ?gender ))





  ;; mutual followers
  (?<- (stdout)
       [?person1 ?person2]
       (follows ?person1 ?person2)
       (follows ?person2 ?person1))





;;;; Abstraction I: composing queries

  (defn younger-than? [limit age]
    (< age limit))


  (?<- (stdout)
       [?person ?age]
       (age ?person ?age)
       (younger-than? 50 ?age))



  ;; young people: Whom do they follow?
  (let [younger-than-30 (<- [?person ?age]
                            (age ?person ?age)
                            (younger-than? 30 ?age))]
    (?<- (stdout)
         [?youngster ?followed ?age]
         (younger-than-30 ?youngster _)    ; using another query as a generator
         (follows         ?youngster ?followed)
         (age ?followed ?age)))



  ;; define a parameterized "view" on an existing dataset
  (defn young-people [limit]
    (<- [?person ?age]
        (age ?person ?age)
        (younger-than? limit ?age)))

  
  (let [younger-than-30 (young-people 30)]
    (?<- (stdout)
         [?youngster ?followed ?age]
         (younger-than-30 ?youngster _)
         (follows ?youngster ?followed)
         (age ?followed ?age)))
  






;;; Aggregators

  ;; sum up people's age
  (?<- (stdout)
       [?sum]
       (age _ ?age)                       ; _ = ignore value
       (c/sum ?age :> ?sum))



  ;; count friends
  (?<- (stdout)
       [?person ?count]
       (follows ?person _)
       (c/count ?count))






  

;;;; Abstraction II: operations
  

  ;; friends' average age
  (?<- (stdout)
       [?person ?avg]
       (follows ?person ?friend)
       (age ?friend ?age)
       
       (c/sum ?age :> ?sum)
       (c/count ?count)
       (div ?sum ?count :> ?avg))


  ;; let's abstract average
  (def average
    (<- [?n :> ?avg]                      ; Predicate Macro
        (c/sum ?n :> ?sum)
        (c/count ?cnt)
        (div ?sum ?cnt :> ?avg)))


  ;; use average
  (?<- (stdout)
       [?person ?avg]
       (follows ?person ?friend)
       (age ?friend ?age)
       (average ?age :> ?avg))          ; here we go







;;;; parameterized predicates

  (deffilterop [younger-than? [x]] [age]
    (< age x))






















;;; Custom Operations

  (defmapcatop split [^String sentence]
    (seq (.split sentence "\\s+")))

  (fact "The following query produces wordcounts from the gettysburg
address. The query tests that a proper subset is produced."

        (<- [?word ?count]
            (sentence ?s)
            (split ?s :> ?word)
            (c/count ?count))
        
        (produces-some [["But" 1]
                        ["Four" 1]
                        ["God" 1]
                        ["It" 3]
                        ["Liberty" 1]
                        ["Now" 1]
                        ["The" 2]
                        ["We" 2]
                        ["a" 7]
                        ["above" 1]]))







;;; Sub Queries

  (let [many-followers (<- [?p]
                           (follows _ ?p)
                           (c/count ?count)
                           (> ?count 2))]
    (fact "People following popular people"
          (<- [?person ?friend]
              (follows ?person ?friend)
              (many-followers ?friend))
          => (produces [["alice" "bob"]
                        ["emily" "bob"]
                        ["emily" "gary"]
                        ["george" "gary"]
                        ["harold" "bob"]
                        ["luanne" "gary"]])))

;;; Sorting

  (defbufferop first-tuple [tuples]
    (first tuples))

  (fact "youngest friend"
        (<- [?person ?friend-out]
            (follows ?person ?friend)
            (age ?friend ?age)
            (:sort ?age)
            (first-tuple ?friend :> ?friend-out))
        (produces [["alice" "david"]
                   ["bob" "david"]
                   ["david" "alice"]
                   ["emily" "gary"]
                   ["george" "gary"]
                   ["harold" "bob"]
                   ["luanne" "gary"]]))
)