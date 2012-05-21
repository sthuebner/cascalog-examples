(defproject cascalog-examples "1-SNAPSHOT"
  :description "A playground for Cascalog."
  :test-directory "src"
  :dependencies [[org.clojure/clojure "1.3.0"]
                 [midje "1.3.1"]
                 [cascalog "1.8.7"]]
  :dev-dependencies [[org.apache.hadoop/hadoop-core "0.20.2"]
                     [midje-cascalog "0.4.0"]
                     [org.slf4j/slf4j-simple "1.6.4"]])
