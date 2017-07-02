(ns electron-card.view
  (:require [hipo.core :as hipo]
            [garden.core :refer [css]]
            [electron-card.game :as game]
            [electron-card.state :as state]
            [clojure.string :as str]))

(def ^:private components-html (atom nil))

(defn update-errors [errors]
  (apply js/console.error errors)
  (set! (.-textContent (js/document.getElementById "errors"))
        (str/join "\n" errors)))

(defn render-components [components]
  (let [comp-style (js/document.getElementById "components-style")
        renderables (map game/component-to-renderable components)
        comp-html (apply vector :div#components-html.component-container (map :html renderables))]
    (set! (.-innerHTML comp-style) (apply css (map :css renderables)))
    (if @components-html
      (hipo/reconciliate! @components-html comp-html)
      (do
        (reset! components-html (hipo/create comp-html))
        (.appendChild (js/document.getElementById "display-area")
                      @components-html)))))

(defn update-game []
  (if (.-checked (js/document.getElementById "show-new"))
    (render-components (state/get-new-components))
    (render-components (state/get-all-components))))

(defn init []
  (set! (.-onchange (js/document.getElementById "show-new"))
        update-game))

