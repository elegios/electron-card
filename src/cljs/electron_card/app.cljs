(ns electron-card.app
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.nodejs :as nodejs]
            [electron-card.state :as state]
            [electron-card.view :as view]
            [electron-card.imgur :as imgur]
            [electron-card.image :as image]
            [electron-card.game :as game]
            electron-card.game.tts
            [cljs.core.async :refer [<!]]))

(nodejs/enable-util-print!)

(defn test-render []
  (go
    (let [comps (state/get-all-components)
          {:keys [width height]} (first comps)
          columns 4
          rows 2
          width (str "calc(" columns "*" width ")")
          height (str "calc(" rows "*" height ")")
          renderables (map game/component-to-renderable comps)
          {:keys [value error]} (<! (image/spreadsheet renderables
                                      :width width :height height
                                      :columns columns :rows rows))]
      (if error
        (println error)
        (println value)))))

(defn init []
  (state/add-game-update-fn :default view/update-game)
  (state/add-errors-update-fn :default view/update-errors)
  (view/init)
  (state/watch-file "test.cljs")
  (println "init"))

(set! *main-cli-fn* init)
