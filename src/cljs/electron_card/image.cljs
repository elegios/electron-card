(ns electron-card.image
  (:require [garden.core :refer [css]]
            [hipo.core :as hipo]
            [promesa.core :as p]))

(def ^:private dom-to-image (js/require "dom-to-image"))

(defn- render-node
  [node]
  (p/promise
    (fn [resolve reject]
      (js/document.body.appendChild node)
      ; TODO: the added children are not guaranteed to have rendered in the next call, insert some form of wait here
      ; TODO: components with the exact same size might not be rendered to the same size (off by one pixel) (presumably only the case when exact size is a fraction)
      (-> (.toPng dom-to-image node)
          (.then
            (fn [value]
              (js/document.body.removeChild node)
              (resolve value))
            (fn [error]
              (js/document.body.removeChild node)
              (reject [error])))))))

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
