(ns sthuebner.cascalog.pagecounts
  (:use cascalog.api
        clojure.test
        [midje sweet cascalog])
  (:require [cascalog.ops :as o]
            [cascalog.playground :as pg]))

(def sample-file
  "data/pagecounts-sample")

(deftest print-textlines
  (let [sample (hfs-textline sample-file)
        query  (<- [?line]
                   (sample ?line))]
    (fact "Executing the above query produces all textlines in
    `sample-file`."
      query
      => (produces [["aa.b File:Incubator-notext.svg 1 27424"]
                    ["aa.b File_talk:Incubator-notext.svg 1 14149"]
                    ["aa.b Help:Contents 1 5426"]
                    ["aa.b Main_Page 3 27719"]
                    ["aa.b MediaWiki:Imgmultipagenext 1 5435"]
                    ["aa.b MediaWiki:Movepagebtn 1 5415"]
                    ["aa.b Special:Contributions/107.0.160.40 1 6297"]
                    ["aa.b Special:Contributions/118.133.185.137 1 6307"]
                    ["aa.b Special:Contributions/125.167.185.217 1 6310"]
                    ["aa.b User:Az1568 1 17624"]]))))


;;; let's split it up into fields
(defn split [s]
  (seq (.split s "\\s+")))

(deftest split-test
  (let [sample (hfs-textline sample-file)]
    (fact "split cuts each line along whitespace, producing 4-tuples."
      (<- [?project ?page ?requests ?bytes]
          (sample ?line)
          (split ?line :> ?project ?page ?requests ?bytes))
      => (produces [["aa.b" "File_talk:Incubator-notext.svg" "1" "14149"]
                    ["aa.b" "User:Az1568" "1" "17624"]
                    ["aa.b" "File:Incubator-notext.svg" "1" "27424"]
                    ["aa.b" "MediaWiki:Movepagebtn" "1" "5415"]
                    ["aa.b" "Help:Contents" "1" "5426"]
                    ["aa.b" "MediaWiki:Imgmultipagenext" "1" "5435"]
                    ["aa.b" "Special:Contributions/107.0.160.40" "1" "6297"]
                    ["aa.b" "Special:Contributions/118.133.185.137" "1" "6307"]
                    ["aa.b" "Special:Contributions/125.167.185.217" "1" "6310"]
                    ["aa.b" "Main_Page" "3" "27719"]]))))

;;; give the dataset a name
(def pagecounts
  (let [sample (hfs-textline sample-file)]
    (<- [?project ?page ?requests ?bytes]
        (sample ?line)
        (split ?line :> ?project ?page ?requests ?bytes))))

(deftest named-counts-test
  (fact "Queries can be bound to vars."
    pagecounts
    => (produces [["aa.b" "File_talk:Incubator-notext.svg" "1" "14149"]
                  ["aa.b" "User:Az1568" "1" "17624"]
                  ["aa.b" "File:Incubator-notext.svg" "1" "27424"]
                  ["aa.b" "MediaWiki:Movepagebtn" "1" "5415"]
                  ["aa.b" "Help:Contents" "1" "5426"]
                  ["aa.b" "MediaWiki:Imgmultipagenext" "1" "5435"]
                  ["aa.b" "Special:Contributions/107.0.160.40" "1" "6297"]
                  ["aa.b" "Special:Contributions/118.133.185.137" "1" "6307"]
                  ["aa.b" "Special:Contributions/125.167.185.217" "1" "6310"]
                  ["aa.b" "Main_Page" "3" "27719"]])))

;;; Let's get some numbers!

;; language and site

(defn lang-site [project]
  (let [[lang site] (.split project "\\.")]
    [lang site]))

(defn parse-int [s]
  (Integer/parseInt s))

(deftest language-test
  (fact "The following query produces a sum of the number of requests
  seen for each combination of language and site."
    (<- [?lang !site ?sum]
        (pagecounts ?project _ ?requests _)
        (lang-site ?project :> ?lang !site)
        (parse-int ?requests :> ?r)
        (o/sum ?r :> ?sum))
    => (produces [["aa" "b" 12]])))


(defn site-name
  "have better site names"
  [site]
  (condp = site
    "b" :wikibooks
    "d" :wiktionary
    "m" :wikimedia
    "mw" :wikipedia-mobile
    "n" :wikinews
    "q" :wikiquote
    "s" :wikisource
    "v" :wikiversity
    "w" :mediawiki
    :wikipedia) )

(deftest named-test
  (fact "Encoding the site allows for more readable results."
    (<- [?lang ?name ?sum]
        (pagecounts ?project _ ?requests _)
        (lang-site ?project :> ?lang !site)
        (site-name !site :> ?name)
        (parse-int ?requests :> ?r)
        (o/sum ?r :> ?sum))
    => (produces [["aa" :wikibooks 12]])))

;; now let's do that with some more data

(deftest top-10-test
  (let [sample (hfs-textline "data/pagecounts-sample-1k")
        counts (<- [?project ?page ?requests ?bytes]
                   (sample ?line)
                   (split ?line :> ?project ?page ?requests ?bytes))
        requests-per-site (<- [?lang ?name ?sum]
                              (counts ?project _ ?requests _)
                              (lang-site ?project :> ?lang !site)
                              (site-name !site :> ?name)
                              (parse-int ?requests :> ?r)
                              (o/sum ?r :> ?sum))]
    (fact "This query produces the top 10 combinations of language and
    name, sorted by ?sum."
      (o/first-n requests-per-site 10
                 :reverse true
                 :sort "?sum")
      => (produces [["en" :wikipedia 1984]
                    ["es" :wikipedia 240]
                    ["ja" :wikipedia 142]
                    ["pt" :wikipedia 141]
                    ["de" :wikipedia 113]
                    ["fr" :wikipedia 76]
                    ["it" :wikipedia 71]
                    ["pl" :wikipedia 58]
                    ["ru" :wikipedia 58]
                    ["commons" :wikimedia 46]]
                   :in-order))))



