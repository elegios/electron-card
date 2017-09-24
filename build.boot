(set-env!
 :source-paths    #{"src/cljs"}
 :resource-paths  #{"resources"}
 :dependencies '[[adzerk/boot-cljs          "1.7.228-2"  :scope "test"]
                 [adzerk/boot-cljs-repl     "0.3.3"      :scope "test"]
                 [com.cemerick/piggieback   "0.2.1"      :scope "test"]
                 [org.clojure/tools.nrepl   "0.2.12"     :scope "test"]
                 [weasel                    "0.7.0"      :scope "test"]
                 [org.clojure/clojurescript "1.9.854"]
                 [funcool/promesa           "1.8.1"]
                 [garden                    "1.3.2"]
                 [hipo                      "0.5.2"]
                 [com.rpl/specter           "1.0.1"]])

(require
 '[adzerk.boot-cljs      :refer [cljs]]
 '[adzerk.boot-cljs-repl :refer [cljs-repl start-repl]])

(deftask build []
  (comp (speak)
        (cljs)
        (target)))

(deftask run []
  (comp (watch)
        (build)))

(deftask production []
  (task-options! cljs {:optimizations :simple
                       :compiler-options {:target :nodejs
                                          :pretty-print false
                                          :optimize-constants true
                                          :static-fns true}})
  identity)

(deftask development []
  (task-options! cljs {:optimizations :none
                       :compiler-options {:target :nodejs}})
  identity)

(deftask dev
  "Simple alias to run application in development mode"
  []
  (comp (development)
        (run)))


