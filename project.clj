(defproject cascalog-examples "1-SNAPSHOT"
  :description "A playground for Cascalog."
  :min-lein-version "2.0.0"
  :test-paths ["src"]
  :repositories {"conjars" "http://conjars.org/repo/"}
  :dependencies [[midje "1.3.1"]
                 [cascalog "1.10.0"]]
  :profiles {:dev {:dependencies [[org.apache.hadoop/hadoop-core "0.20.2-dev"]
                                  [midje-cascalog "0.4.0"]
                                  [ritz/ritz-swank "0.5.0"]]}})
