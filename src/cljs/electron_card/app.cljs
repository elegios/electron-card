(ns electron-card.app
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.nodejs :as nodejs]
            [electron-card.eval :as ev]
            [cljs.core.async :refer [<! promise-chan put!]]))

(nodejs/enable-util-print!)

(def fs (js/require "fs"))

(defn read-file [path]
  (let [ret (promise-chan)]
    ; TODO: report error properly
    (.readFile fs path "utf8" #(put! ret %2))
    ret))

(def last-source (atom ""))

(defn file-changed [kind file]
  (if (not= kind "change")
    (println "not change, ignoring:" kind)
    (go
      (let [new-source (<! (read-file file))]
        (if (= @last-source new-source)
          (println "equal source, ignoring")
          (do
            (println "different source")
            (reset! last-source new-source)
            (println (<! (ev/eval new-source)))))))))

(defn init
  []
  (file-changed "change" "test.cljs")
  (.watch fs "test.cljs" #js{} file-changed)
  (println "init"))

(set! *main-cli-fn* init)
