;; These examples are taken directly or derived from Nathan Marz' blog post
;; http://nathanmarz.com/blog/introducing-cascalog-a-clojure-based-query-language-for-hado.html

(ns sthuebner.cascalog.playground
  (:use cascalog.api
        cascalog.playground
        [midje sweet cascalog])
  (:require [cascalog.ops :as c]))


(comment
  ;; getting started





  


  
  ;; A  sample query
  
  (<- [?person ?year]                       ; output vars
      (age ?person ?year)                   ; generator
      (< ?year 30))                         ; filter


  (?<- (stdout)
       [?person]
       (age ?person ?year)
       (< ?year 30))



  
  ;; Execute the query
  (def example-query
    (<- [?person ?year]                       ; output vars
        (age ?person ?year)                   ; generator
        (< ?year 30)))

  (?- (stdout)                               ; destination
      example-query)



  (?<- (stdout)
       [?person ?year ?young]
       (age ?person ?year)
       (< ?year 30 :> ?young))
  




   ;; compute new values
   (?<- (stdout)
        [?person ?year ?a ?b]
        (age ?person ?year)
        (* ?year 2 :> ?a)
        (/ ?year 2 :> ?b))



   ;; young vs. not-young people
   (?<- (stdout)
        [?person ?young]
        (age ?person ?year)
        (< ?year 30 :> ?young))           ; ?young = true | false








 ;;; Constrain with constants

   (?<- (stdout)
        [?person]
        (age ?person ?year)
        (= 25 ?year))

   ;; constrain on an input vars
   (?<- (stdout)
        [?person]
        (age ?person 25))



   ;; constrain on a function's result
   (?<- (stdout)
        [?person]
        (age ?person ?year)
        (* ?year 2 :> 50))









 ;;; Joins

   ;; people with their age and gender
   (?<- (stdout)
        [?person ?year ?gender]

        (age     ?person   ?year)
        (gender  ?person   ?gender))



   
   ;; OUTER JOIN
   (?<- (stdout)
        [?person !!age !!gender]

        (age     ?person   !!age)
        (gender  ?person   !!gender))





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

   (defn less-than? [limit age]
     (< age limit))


   (?<- (stdout)
        [?person ?year]
        (age ?person ?year)
        (less-than? 30 ?year))


   ;; define a "parameterized view" on an existing dataset
   (defn people-below [limit]
     (<- [?person ?year]
         (age ?person ?year)
         (less-than? limit ?year)))


   (def youngsters (people-below 30))

   (?- (stdout) youngsters)


   (?<- (stdout)
        [?youngster ?friend ?year]
        ((people-below 30) ?youngster _)
        (follows ?youngster ?friend)
        (age ?friend ?year))





;;; Aggregators

  ;; count friends
  (?<- (stdout)
       [?person ?count]
       (follows ?person _)
       (c/count ?count))



  ;; sum up people's age
  (?<- (stdout)
       [?sum]
       (age _ ?year)                       ; _ = ignore value
       (c/sum ?year :> ?sum))






  

;;;; Abstraction II: operations
  

  ;; friends' average age
  (?<- (stdout)
       [?person ?avg]
       (follows ?person ?friend)
       (age ?friend ?year)

       ;; compute ?avg age
       (c/sum ?year :> ?sum)
       (c/count ?count)
       (div ?sum ?count :> ?avg)
       )


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
       (age ?friend ?year)
       (average ?year :> ?avg))          ; here we go





  

  ;; putting pieces together
  (?<- (stdout)
       [?youngster ?friends-avg-age]
       (youngsters ?youngster _)
       (follows ?youngster ?friend)
       (age ?friend ?friend-age)
       (average ?friend-age :> ?friends-avg-age))





  ;; Testing
  
  (def average-age-of-youngsters-friends
    (<- [?youngster ?friends-avg-age]
        (youngsters ?youngster _)
        (follows ?youngster ?friend)
        (age ?friend ?friend-age)
        (average ?friend-age :> ?friends-avg-age)))

  (fact
   average-age-of-youngsters-friends
   => (produces [["alice" 27.666666666666668]
                 ["david" 32.0]
                 ["emily" 30.0]]))





  ;; Template Taps
  (?<- (lfs-textline "/tmp/foo"
                     :sink-template "%s/"
                     :templatefields [ "?1st-letter" ]
                     :outfields [ "?name" "?year" ]
                     :sinkmode :replace)
       [?name ?year ?1st-letter]
       (age ?name ?year)
       (first ?name :> ?1st-letter))
  



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
            (age ?friend ?year)
            (:sort ?year)
            (first-tuple ?friend :> ?friend-out))
        (produces [["alice" "david"]
                   ["bob" "david"]
                   ["david" "alice"]
                   ["emily" "gary"]
                   ["george" "gary"]
                   ["harold" "bob"]
                   ["luanne" "gary"]]))
)
