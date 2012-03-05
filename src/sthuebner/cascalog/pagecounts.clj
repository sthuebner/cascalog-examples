(ns sthuebner.cascalog.pagecounts
  (:use cascalog.api)
  (:require [cascalog.ops :as o]
            [cascalog.playground :as pg]))

(pg/bootstrap-emacs)


(def sample-file "data/pagecounts-sample")

(comment

  (let [sample (hfs-textline sample-file)]
    (?<- (stdout)
         [?line]
         (sample ?line)))
  
  )

;;; let's split it up into fields
(defn split [s]
  (seq (.split s "\\s+")))

(comment

  (let [sample (hfs-textline sample-file)]
    (?<- (stdout)
         [?project ?page ?requests ?bytes]
         (sample ?line)
         (split ?line :> ?project ?page ?requests ?bytes)))

  )

;;; give the dataset a name
(def pagecounts
  (let [sample (hfs-textline sample-file)]
    (<- [?project ?page ?requests ?bytes]
        (sample ?line)
        (split ?line :> ?project ?page ?requests ?bytes))))


;;; language and site
(defn lang-site [project]
  (let [[lang site] (.split project "\\.")]
    [lang site]))

(defn parse-int [s]
  (Integer/parseInt s))

(comment

  (?<- (stdout)
       [?lang !site ?sum]
       (pagecounts ?project _ ?requests _)
       (lang-site ?project :> ?lang !site)
       (parse-int ?requests :> ?r)
       (o/sum ?r :> ?sum))
  )



;;; have better site names
(defn site-name [site]
  (or ({"b" :wikibooks
        "d" :wiktionary
        "m" :wikimedia
        "mw" :wikipedia-mobile
        "n" :wikinews
        "q" :wikiquote
        "s" :wikisource
        "v" :wikiversity
        "w" :mediawiki}
       site)
      :wikipedia))


(comment

  (?<- (stdout)
       [?lang ?name ?sum]
       (pagecounts ?project _ ?requests _)
       (lang-site ?project :> ?lang !site)
       (site-name !site :> ?name)
       (parse-int ?requests :> ?r)
       (o/sum ?r :> ?sum))


  ;; now let's do that with some more data
  (def pagecounts
    (let [sample (hfs-textline "data/pagecounts-sample-1k")]
      (<- [?project ?page ?requests ?bytes]
          (sample ?line)
          (split ?line :> ?project ?page ?requests ?bytes))))


  ;; let's get the top 10
  (defbufferop top-ten [tuples]
    (take 10 tuples))

  (let [requests-per-site
        (<- [?lang ?name ?sum]
            (pagecounts ?project _ ?requests _)
            (lang-site ?project :> ?lang !site)
            (site-name !site :> ?name)
            (parse-int ?requests :> ?r)
            (o/sum ?r :> ?sum))]
    (?<- (stdout)
         [?lang-out ?name-out ?sum-out]
         (requests-per-site ?lang ?name ?sum)
         (:sort ?sum) (:reverse true)
         (top-ten ?lang ?name ?sum :> ?lang-out ?name-out ?sum-out)))
  
  
  )