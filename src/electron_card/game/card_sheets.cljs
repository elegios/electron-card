(ns electron-card.game.card-sheets
  (:require [electron-card.game :as game]
            [electron-card.image :as image]
            [electron-card.renderable :as renderable]
            [promesa.core :as p]
            [clojure.spec.alpha :as s]
            [com.rpl.specter :refer [ALL multi-path] :refer-macros [select]]
            [cljsjs.jspdf]))

(s/def ::front :electron-card.game/component)
(s/def ::back :electron-card.game/component)

(s/def ::card (s/keys :req-un [::front ::back]))

(s/def ::cards (s/coll-of ::card :kind sequential?))

(s/def ::card-size-mm (s/tuple number? number?))

(s/def ::backs boolean?)

(defmethod game/game-type :card-sheets [_]
  (s/keys :req-un [::cards ::card-size-mm]
          :opt-un [::backs]))

(defmethod game/extract-components :card-sheets
  [{:keys [cards]}]
  (->> cards
       (select [ALL (multi-path :front :back)])
       set))

(def ^:private sheet-width 210)
(def ^:private sheet-height 297)
(def ^:private sheet-margin 10)

(defn- print-card
  [doc rendered backs card-width card-height]
  (fn [idx {:keys [front back]}]
    (let [cols (quot (- sheet-width  (* 2 sheet-margin)) card-width)
          rows (quot (- sheet-height (* 2 sheet-margin)) card-height)
          page-num (inc (* (quot idx (* cols rows)) (if backs 2 1)))
          idx (mod idx (* cols rows))
          x (+ sheet-margin (* (mod idx cols) card-width))
          back-x (+ sheet-margin (* (- (dec cols) (mod idx cols)) card-width))
          y (+ sheet-margin (* (quot idx cols) card-height))]
      (-> doc
          (.setPage page-num)
          (.addImage (:data-url (rendered front))
            x y card-width card-height))
      (when backs
        (-> doc
            (.setPage (inc page-num))
            (.addImage (:data-url (rendered back))
              back-x y card-width card-height))))))

(defmethod game/export :card-sheets
  [{:keys [cards card-size-mm backs] :as state} directory]
  (let [[card-width card-height] card-size-mm
        cols (quot (- sheet-width  (* 2 sheet-margin)) card-width)
        rows (quot (- sheet-height (* 2 sheet-margin)) card-height)
        cards-per-sheet (* cols rows)
        components (vec (game/extract-components state))
        doc (jspdf.)]
    (dotimes [_ (dec (* (if backs 2 1) (js/Math.ceil (/ (count cards) cards-per-sheet))))]
      (.addPage doc))
    (->> components
         (map renderable/component-to-renderable)
         (map image/image)
         p/all
         (p/map #(zipmap components %))
         (p/map
           (fn [rendered]
             (dorun
               (map-indexed
                 (print-card doc rendered backs card-width card-height)
                 cards))
             (.save doc "sheet.pdf"))))))
