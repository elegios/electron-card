(ns electron-card.image
  (:require [garden.core :refer [css]]
            [hipo.core :as hipo]
            [cljs.core.async :refer [promise-chan put!]]))

(def ^:private dom-to-image (js/require "dom-to-image"))

(defn- render-node
  [node ret]
  (let [ret (or ret (promise-chan))]
    (js/document.body.appendChild node)
    (-> (.toPng dom-to-image node)
        (.then
          (fn [value]
            (js/document.body.removeChild node)
            (put! ret {:value value}))
          (fn [error]
            (js/document.body.removeChild node)
            (put! ret {:errors [error]}))))
    ret))

(defn spreadsheet
  [renderables & {:keys [width height rows columns ret]}]
  (let [style [:style
               (str ".spreadsheet{"
                    "display:flex;"
                    "flex-wrap:wrap;"
                    "width:calc(" columns "*" width ");"
                    "height:calc(" rows "*" height ");}")
               (map (comp css :css) renderables)]
        html [:div.spreadsheet
              style
              (map :html renderables)]
        node (hipo/create html)]
    (render-node node ret)))

(defn image
  [renderable & {:keys [ret]}]
  (let [style [:style (-> renderable :css css)]
        html (conj (:html renderable) style)
        node (hipo/create html)]
    (render-node node ret)))
