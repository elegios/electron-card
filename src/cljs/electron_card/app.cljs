(ns electron-card.app
  (:require [cljs.nodejs :as nodejs]
            [electron-card.state :as state]
            [electron-card.view :as view]))

(nodejs/enable-util-print!)

(defn init []
  (state/add-game-update-fn :default view/update-game)
  (state/add-errors-update-fn :default view/update-errors)
  (view/init)
  (state/watch-file "test.cljs")
  (println "init"))

(set! *main-cli-fn* init)
