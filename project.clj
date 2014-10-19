(defproject om-quiz "0.1.0-SNAPSHOT"
  :description "FIXME: write this!"
  :url "http://example.com/FIXME"

  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/clojurescript "0.0-2311"]
                 [org.clojure/core.async "0.1.267.0-0d7780-alpha"]
                 [om "0.7.1"]

                 [enlive "1.1.5"]
                 
                 [compojure "1.1.6"]]

  :plugins [[lein-ring "0.8.10"]
            [lein-cljsbuild "1.0.3"]]

  :source-paths ["src-clj"]

  :ring {:handler om-quiz.core/app}
  :cljsbuild { 
    :builds [{:id "om-quiz"
              :source-paths ["src-cljs"]
              :compiler {
                :output-to "om_quiz.js"
                :output-dir "out"
                :optimizations :none
                :source-map true}}]})
