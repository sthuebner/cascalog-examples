;; These examples are taken directly or derived from Nathan Marz' blog post
;; http://nathanmarz.com/blog/introducing-cascalog-a-clojure-based-query-language-for-hado.html

(ns sthuebner.cascalog.playground
  (:use cascalog.api
        cascalog.playground
        [midje sweet cascalog])
  (:require [cascalog.ops :as o]))

;; getting started

;; The "age" dataset produces person, name pairs.
(fact
  (<- [?person]          ;; <- the fields written to output
      (age ?person ?age) ;; <- a generator with two variables
      (< ?age 30))       ;; <- filtering predicate
  => (produces [["alice"]
                ["david"]
                ["emily"]
                ["gary"]
                ["kumar"]]))

;;; Operations

(fact
  (<- [?person ?double-age]
      (age ?person ?age)
      (* ?age 2 :> ?double-age))
  => (produces [["david" 50]
                ["emily" 50]
                ["kumar" 54]
                ["alice" 56]
                ["gary" 56]
                ["george" 62]
                ["bob" 66]
                ["luanne" 72]
                ["chris" 80]]))


(fact "young people"
  (<- [?person ?young]
      (age ?person ?age)
      (< ?age 30 :> ?young))
  => (produces [["alice" true]
                ["bob" false]
                ["chris" false]
                ["david" true]
                ["emily" true]
                ["gary" true]
                ["george" false]
                ["kumar" true]
                ["luanne" false]]))

;;; Constant Substitution

(fact "take only persons of age 25"
  (<- [?person]
      (age ?person 25))
  => (produces [["david"] ["emily"]]))

(fact "double age must be 50"
  (<- [?person]
      (age ?person ?age)
      (* ?age 2 :> 50))
  => (produces [["david"] ["emily"]]))

;;; Joins

(fact "Persons' age and gender"
  (<- [?person ?age ?gender]
     (age ?person ?age)
     (gender ?person ?gender))
  => (produces [["emily" 25 "f"]
                ["david" 25 "m"]
                ["alice" 28 "f"]
                ["gary" 28 "m"]
                ["george" 31 "m"]
                ["bob" 33 "m"]
                ["luanne" 36 "f"]
                ["chris" 40 "m"]]))

(fact "Friend's gender"
  (<- [?person ?followed ?gender]
     (follows ?person ?followed)
     (gender ?followed ?gender))
  => (produces [["david" "alice" "f"]
                ["emily" "alice" "f"]
                ["alice" "emily" "f"]
                ["bob" "luanne" "f"]
                ["david" "luanne" "f"]
                ["alice" "bob" "m"]
                ["emily" "bob" "m"]
                ["harold" "bob" "m"]
                ["alice" "david" "m"]
                ["bob" "david" "m"]
                ["emily" "gary" "m"]
                ["george" "gary" "m"]
                ["luanne" "gary" "m"]
                ["bob" "george" "m"]
                ["emily" "george" "m"]
                ["luanne" "harold" "m"]]))

(fact "People following each other"
  (<- [?person1 ?person2]
     (follows ?person1 ?person2)
     (follows ?person2 ?person1))
  => (produces [["alice" "david"]
                ["alice" "emily"]
                ["david" "alice"]
                ["emily" "alice"]]))

(fact "Younger followers"
  (<- [?person ?friend]
      (follows ?person ?friend)
      (age ?person ?age1)
      (age ?friend ?age2)
      (< ?age2 ?age1))
  => (produces [["alice" "david"]
                ["alice" "emily"]
                ["bob" "david"]
                ["bob" "george"]
                ["george" "gary"]
                ["luanne" "gary"]]))

;;; Outer Joins

(fact "ALL people and their friends"
  (<- [?person !!friend]
      (person ?person)
      (follows ?person !!friend))
  => (produces [["alice" "bob"]
                ["alice" "david"]
                ["alice" "emily"]
                ["bob" "david"]
                ["bob" "george"]
                ["bob" "luanne"]
                ["chris" nil]
                ["david" "alice"]
                ["david" "luanne"]
                ["emily" "alice"]
                ["emily" "bob"]
                ["emily" "gary"]
                ["emily" "george"]
                ["gary" nil]
                ["george" "gary"]
                ["harold" "bob"]
                ["kumar" nil]
                ["luanne" "gary"]
                ["luanne" "harold"]]))

;; people not following anybody
(fact
  (<- [?person]
     (person ?person)
     (follows ?person !!friend)
     (nil? !!friend))
  => (produces [["chris"] ["gary"] ["kumar"]]))

;;; Aggregators

(fact "Number of people a person follows"
  (<- [?person ?count]
     (follows ?person _)
     (o/count ?count))
  => (produces [["alice" 3]
                ["bob" 3]
                ["david" 2]
                ["emily" 4]
                ["george" 1]
                ["harold" 1]
                ["luanne" 2]]))

(fact "Average age of friends"
  (<- [?person ?avg]
     (follows ?person ?friend)
     (age ?friend ?age)
     (o/sum ?age :> ?sum)
     (o/count ?count)
     (div ?sum ?count :> ?avg))
  => (produces [["alice" 27.666666666666668]
                ["bob" 30.666666666666668]
                ["david" 32.0]
                ["emily" 30.0]
                ["george" 28.0]
                ["harold" 33.0]
                ["luanne" 28.0]]))

;;; Custom Operations

(defmapcatop split [^String sentence]
  (seq (.split sentence "\\s+")))

(fact "The following query produces wordcounts from the gettysburg
address. The query tests that a proper subset is produced."
  (<- [?word ?count]
      (sentence ?s)
      (split ?s :> ?word)
      (o/count ?count))
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
                         (o/count ?count)
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
