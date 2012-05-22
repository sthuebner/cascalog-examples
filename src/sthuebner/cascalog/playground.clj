;; These examples are taken directly or derived from Nathan Marz' blog post
;; http://nathanmarz.com/blog/introducing-cascalog-a-clojure-based-query-language-for-hado.html

(ns sthuebner.cascalog.playground
  (:use cascalog.api
        cascalog.playground
        [midje sweet cascalog])
  (:require [cascalog.ops :as c]))

;; getting started



;; The "age" dataset produces person, name pairs.

(?<- (stdout)                           ; destination
     [?person]                          ; output vars
     (age ?person ?age)                 ; generator
     (< ?age 30))                       ; filter


;; compute new values
(?<- (stdout)
     [?person ?double-age]
     (age ?person ?age)
     
     (* ?age 2 :> ?double-age))


;; say "true" or "false"
(?<- (stdout)
     [?person ?young]
     (age ?person ?age)
    
     (< ?age 30 :> ?young))



;;; Constant Substitution

(?<- (stdout)
     [?person]
     (age ?person 25))


(?<- (stdout)
     [?person]
     (age ?person ?age)
     (* ?age 2 :> 50))


;;; Joins

;; person, age, and gender
(?<- (stdout)
     [?person ?age ?gender]
     (age ?person ?age)                 ; ?person
     (gender ?person ?gender))          ; ?person





;; person, friends, friends' gender
(?<- (stdout)
     [?person ?followed ?gender]
     (follows ?person ?followed)        ; ?followed
     (gender ?followed ?gender))        ; ?followed





;; mutual followers
(?<- (stdout)
     [?person1 ?person2]
     (follows ?person1 ?person2)
     (follows ?person2 ?person1))










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
     (average ?age :> ?avg))




;;;; parameterized predicates

(deffilterop [younger-than? [x]] [age]
  (< age x))


(?<- (stdout)
     [?person ?age]
     (age ?person ?age)
     (younger-than? 50 ?age))


(defn young-people [limit]
  (<- [?person ?age]
      (age ?person ?age)
      (younger-than? limit ?age)))






















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
