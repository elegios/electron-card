(ns electron-card.image
  (:require [garden.core :refer [css]]
            [hipo.core :as hipo]
            [hipo.attribute :refer [style-handler]]
            [cljs.core.async :refer [promise-chan put!]]))

(def ^:private dom-to-image (js/require "dom-to-image"))

(defn spreadsheet
  [renderables & {:keys [width height rows columns]}]
  (let [style [:style
               (str ".spreadsheet{"
                    "display:flex;"
                    "flex-wrap:wrap;"
                    "width:" width ";"
                    "height:" height ";}")
               (map (comp css :css) renderables)]
        html [:div.spreadsheet
              style
              (map :html renderables)]
        ret (promise-chan)
        node (hipo/create html [style-handler])]
    (js/document.body.appendChild node)
    (-> (.toPng dom-to-image node)
        (.then
          (fn [value]
            (js/document.body.removeChild node)
            (put! ret {:value value}))
          (fn [error]
            (js/document.body.removeChild node)
            (put! ret {:error error}))))
    ret))

  ; TODO: make html and css to lay out renderables in spreadsheet
  ;       in the same order.
