(set-env!
 :source-paths    #{"src/cljs"}
 :resource-paths  #{"resources"}
 :dependencies '[[adzerk/boot-cljs          "1.7.228-2"  :scope "test"]
                 [adzerk/boot-cljs-repl     "0.3.3"      :scope "test"]
                 [com.cemerick/piggieback   "0.2.1"      :scope "test"]
                 [org.clojure/tools.nrepl   "0.2.12"     :scope "test"]
                 [weasel                    "0.7.0"      :scope "test"]
                 [org.clojure/clojurescript "1.9.293"]
                 [org.clojure/core.async    "0.3.442"]])

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


