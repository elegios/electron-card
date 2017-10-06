(ns electron-card.app
  (:require [cljs.nodejs :as nodejs]
            [electron-card.state :as state]
            [electron-card.view :as view]
            [electron-card.image :as image]
            [electron-card.renderable :as renderable]
            [electron-card.game :as game]
            [electron-card.game.tts]
            [electron-card.game.component-collection]
            [electron-card.game.card-sheets]
            [promesa.core :as p]
            [com.rpl.specter :refer [MAP-VALS] :refer-macros [transform]]))

(nodejs/enable-util-print!)

; TODO: promesa (actually bluebird) requires that values that are rejected with are Error, add one of those that collects all errors

(defn export-dir [directory]
  (-> (state/get-state)
      (game/export directory)
      (p/then #(js/alert "Export complete"))
      (p/catch #(state/add-errors (if (coll? %) % [%])))))

(defn init []
  (state/add-game-update-fn :default view/update-game)
  (state/add-errors-update-fn :default view/update-errors)
  (view/init)
  (state/watch-file "../testproject/source.cljs")
  (println "init"))

(set! *main-cli-fn* init)
