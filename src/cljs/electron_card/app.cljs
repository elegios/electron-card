(ns electron-card.app
  (:require [cljs.nodejs :as nodejs]
            [electron-card.state :as state]
            [electron-card.view :as view]
            [electron-card.image :as image]
            [electron-card.upload.file :as file]
            [electron-card.game :as game]
            [electron-card.game.tts :as tts]
            [promesa.core :as p]
            [com.rpl.specter :refer [MAP-VALS] :refer-macros [transform]]))

(nodejs/enable-util-print!)

; TODO: promesa (actually bluebird) requirest that values that are rejected with are Error, add one of those that collects all errors

(defn test-render []
  (let [upload-fn (file/make-save-fn "./out")]
    (->> (state/get-all-components)
         (map game/component-to-renderable)
         (map image/image)
         (map #(p/then % upload-fn))
         p/all
         (p/map #(println "value: " %))
         (p/catch #(println "catch: " %)))))

(defn init []
  (state/add-game-update-fn :default view/update-game)
  (state/add-errors-update-fn :default view/update-errors)
  (view/init)
  (state/watch-file "test.cljs")
  (println "init"))

(set! *main-cli-fn* init)
