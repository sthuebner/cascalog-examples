(ns sthuebner.cascalog.pagecounts
  (:refer-clojure :exclude [bytes])
  (:use cascalog.api
        clojure.test
        [midje sweet cascalog])
  (:require [cascalog.ops :as c]
            [cascalog.playground :as pg]))

(def sample-file
  "data/pagecounts-sample")



(fact "Executing the above query produces all textlines in
    `sample-file`."
      (hfs-textline sample-file)
      => (produces [["aa.b File:Incubator-notext.svg 1 27424"]
                    ["aa.b File_talk:Incubator-notext.svg 1 14149"]
                    ["aa.b Help:Contents 1 5426"]
                    ["aa.b Main_Page 3 27719"]
                    ["aa.b MediaWiki:Imgmultipagenext 1 5435"]
                    ["aa.b MediaWiki:Movepagebtn 1 5415"]
                    ["aa.b Special:Contributions/107.0.160.40 1 6297"]
                    ["aa.b Special:Contributions/118.133.185.137 1 6307"]
                    ["aa.b Special:Contributions/125.167.185.217 1 6310"]
                    ["aa.b User:Az1568 1 17624"]]))


;;; let's split it up into fields
(defn split [s]
  (seq (.split s "\\s+")))

(fact
 (split "aa.b User:Az1568 1 17624") => ["aa.b" "User:Az1568" "1" "17624"])


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
                      ["aa.b" "Main_Page" "3" "27719"]])))




;;;; Log files usually have a large number of aspects to them...



(defprotocol PageCount
  (project [record])
  (page [record])
  (requests [record])
  (bytes [record]))

(defrecord PcRecord [v]
  PageCount
  (project [_] (v 0))
  (page [_] (v 1))
  (requests [_] (v 2))
  (bytes [_] (v 3)))

(defn make-pcrecord [line]
  (->> line split vec PcRecord.))


(def pagecounts-sample
  (<- [?record]
      ((hfs-textline sample-file) ?line)
      (make-pcrecord ?line :> ?record)
      (:distinct false)))


(comment

  ;; let's query for some properties
  (?<- (stdout) [?prj ?page ?requests]
       (pagecounts-sample ?record)
       (project ?record :> ?prj)
       (page ?record :> ?page)
       (requests ?record :> ?requests))


  (?<- (stdout) [?prj ?page ?requests]
       (pagecounts-sample ?record)
       ((o/juxt #'project #'page #'requests) ?record :> ?prj ?page ?requests))

  )



;;; Let's get some numbers!

;; language and site

(defn lang-site [record]
  (let [[lang site] (.split (project record) "\\.")]
    [lang site]))

(defn parse-int [s]
  (Integer/parseInt s))

(fact "The following query produces a sum of the number of requests
  seen for each combination of language and site."
      (<- [?lang !site ?sum]
          (pagecounts-sample ?record)
          (lang-site ?record :> ?lang !site) ; retrieve language and site
          (requests ?record :> ?requests)    ; get # requests
          (parse-int ?requests :> ?r)
          (c/sum ?r :> ?sum))                ; sum up per language and site
      => (produces [["aa" "b" 12]]))


(defn site-name
  "have better site names"
  [site]
  (get {"b" :wikibooks
        "d" :wiktionary
        "m" :wikimedia
        "mw" :wikipedia-mobile
        "n" :wikinews
        "q" :wikiquote
        "s" :wikisource
        "v" :wikiversity
        "w" :mediawiki}
       site
       :wikipedia))



(fact "Encoding the site allows for more readable results."
      (<- [?lang ?name ?sum]
          (pagecounts-sample ?record)
          (lang-site ?record :> ?lang !site)
          (site-name !site :> ?name)
          (requests ?record :> ?requests)
          (parse-int ?requests :> ?r)
          (c/sum ?r :> ?sum))
      => (produces [["aa" :wikibooks 12]]))




;; now let's do that with some more data


(defn pagecounts
  ([]
     (pagecounts sample-file))
  ([path]
     (<- [?record]
         ((hfs-textline path) ?line)
         (make-pcrecord ?line :> ?record)
         (:distinct false))))


(let [sample-file "data/pagecounts-sample-1k"
      requests-per-site (<- [?lang ?name ?sum]
                            ((pagecounts sample-file) ?record)
                            (lang-site ?record :> ?lang !site)
                            (site-name !site :> ?name)
                            (requests ?record :> ?requests)
                            (parse-int ?requests :> ?r)
                            (c/sum ?r :> ?sum))]
  (fact "This query produces the top 10 combinations of language and
    name, sorted by ?sum."
        (c/first-n requests-per-site 10
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
                     :in-order)))



