(ns electron-card.app
  (:require [cljs.nodejs :as nodejs]
            [electron-card.state :as state]
            [electron-card.view :as view]
            [electron-card.image :as image]
            [electron-card.renderable :as renderable]
            [electron-card.upload.file :as file]
            [electron-card.game :as game]
            [electron-card.game.tts :as tts]
            [promesa.core :as p]
            [com.rpl.specter :refer [MAP-VALS] :refer-macros [transform]]))

(nodejs/enable-util-print!)

; TODO: promesa (actually bluebird) requirest that values that are rejected with are Error, add one of those that collects all errors

; TODO: present feedback that it's done, probably move out of app.cljs
(defn export-components [directory]
  (let [upload-fn (file/make-save-fn directory)]
    (->> (state/get-all-components)
         (map renderable/component-to-renderable)
         (map image/image)
         (map #(p/then % upload-fn))
         p/all
         (p/map #(println "value: " %))
         (p/catch #(state/add-errors (if (coll? %) % [%]))))))

(defn init []
  (state/add-game-update-fn :default view/update-game)
  (state/add-errors-update-fn :default view/update-errors)
  (view/init)
  (state/watch-file "../testproject/source.cljs")
  (println "init"))

(set! *main-cli-fn* init)
