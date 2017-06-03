(ns electron-card.app
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.nodejs :as nodejs]
            [electron-card.state :as state]
            [electron-card.view :as view]
            [electron-card.imgur :as imgur]
            [electron-card.image :as image]
            [electron-card.game :as game]
            [electron-card.game.tts :as tts]
            [cljs.core.async :refer [<!]]
            [com.rpl.specter :refer [MAP-VALS] :refer-macros [transform]]))

(nodejs/enable-util-print!)

(defn- log-first
  [val label]
  (println label val))

(defn test-render []
  (go
    (let [{:keys [errors] :as res} (-> (state/get-state) tts/collect-cards set tts/auto-spreadsheets <!)]
      (if (seq errors)
        (println "errs" errors)
        (println "res" (transform [MAP-VALS] count res))))))

(defn init []
  (state/add-game-update-fn :default view/update-game)
  (state/add-errors-update-fn :default view/update-errors)
  (view/init)
  (state/watch-file "test.cljs")
  (println "init"))

(set! *main-cli-fn* init)
