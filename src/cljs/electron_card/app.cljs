(ns electron-card.app
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.nodejs :as nodejs]
            [electron-card.state :as state]
            [electron-card.view :as view]
            [electron-card.imgur :as imgur]
            [electron-card.image :as image]
            [electron-card.game :as game]
            [electron-card.game.tts :as tts]
            [cljs.core.async :refer [<!]]))

(nodejs/enable-util-print!)

(defn test-render []
  (go
    (let [comps (state/get-all-components)
          {:keys [width height]} (first comps)
          columns 4
          rows 2
          renderables (map game/component-to-renderable comps)
          {:keys [value errors]} (<! (tts/auto-spreadsheets renderables
                                       :width width :height height))]
      (if errors
        (println "errs" errors)
        (println "value" value)))))

(defn init []
  (state/add-game-update-fn :default view/update-game)
  (state/add-errors-update-fn :default view/update-errors)
  (view/init)
  (state/watch-file "test.cljs")
  (println "init"))

(set! *main-cli-fn* init)
