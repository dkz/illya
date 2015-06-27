(defproject illya "0.1.0-SNAPSHOT"
  :license {:name "BSD 2-clause license"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [com.ninjudd/ring-async "0.2.0"]
                 [com.orientechnologies/orientdb-client "2.0.10"]
                 [com.orientechnologies/orientdb-enterprise "2.0.10"]
                 [com.orientechnologies/orientdb-core "2.0.10"]
                 [com.orientechnologies/orientdb-graphdb "2.0.10"]
                 [compojure "1.3.4"]
                 [hiccup "1.0.5"]
                 [sigmund "0.1.1"]
                 [spyscope "0.1.5"]
                 [cats "0.4.0"]]
  :plugins [[lein-ring "0.9.6"]]
  :ring {:handler illya.app/app-handler
         :init illya.app/app-init
         :nrepl {:start? true
                 :port 40400}}
  :target-path "target/%s"
  :main ^:skip-aot illya.app
  :jvm-opts ["-Dillya.config=config.properties"]
  :profiles {:uberjar {:aot :all}})
