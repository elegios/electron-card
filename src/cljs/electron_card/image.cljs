(ns electron-card.image
  (:require [garden.core :refer [css]]
            [hipo.core :as hipo]
            [promesa.core :as p]))

(def ^:private dom-to-image (js/require "dom-to-image"))

(defn- render-node
  [node]
  (let [parent (js/document.getElementById "render-area")]
    (.appendChild parent node)
    (-> (p/delay 500)
        (p/then #(p/promise (.toPng dom-to-image node)))
        (p/catch #(p/rejected [%]))
        (p/finally #(.removeChild parent node)))))

(defn spreadsheet
  [renderables & {:keys [width height rows columns]}]
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
    (render-node node)))

(defn image
  [renderable]
  (let [style [:style (-> renderable :css css)]
        html (conj (:html renderable) style)
        node (hipo/create html)]
    (render-node node)))
