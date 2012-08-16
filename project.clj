(defproject cascalog-examples "1-SNAPSHOT"
  :description "A playground for Cascalog."
  :min-lein-version "2.0.0"
  :test-paths ["src"]
  :repositories {"conjars" "http://conjars.org/repo/"}
  :dependencies [[org.clojure/clojure "1.3.0"]
                 [midje "1.3.1"]
                 [cascalog "1.10.0"]
                 ;;[cascading/cascading-core "1.2.6"]
                 ]
  :profiles {:dev {:dependencies [[org.apache.hadoop/hadoop-core "0.20.2-dev"]
                                  [midje-cascalog "0.4.0"]
                                  ;;[org.slf4j/slf4j-simple "1.4.3"]
                                  ;;[org.slf4j/jcl-over-slf4j "1.4.3"]
                                  ]}})
