(ns electron-card.game.tts
  (:require [electron-card.game :as game]
            [electron-card.image :as image]
            [promesa.core :as p]
            [com.rpl.specter :refer [ALL STAY MAP-VALS must multi-path traverse walker]
                             :refer-macros [transform select]]))

(defn- is-type
  [type]
  (fn [obj]
    (= type (:type obj))))

(defmethod game/extract-components :tts
  [tts]
  (->> tts
       (traverse
         [:objects ALL
          (multi-path
            [(is-type :deck) :cards ALL]
            STAY)
          (is-type :card)
          (multi-path
            (must :front)
            (must :back))])
       (into #{})))

(defn collect-cards
  [tts]
  (select [(walker (is-type :card))] tts))

(def ^:private sizes
  (vec
    (into (sorted-map)
      (for [rows (range 2 (inc 10))
            columns (range 2 (inc (min rows 7)))]
        [(* rows columns) [rows columns]]))))

(defn- make-spreadsheet [width height]
  (fn [renderables]
    (let [[[rows columns]] (filter
                             (fn [[size]] (<= size (count renderables)))
                             sizes)]
      (image/spreadsheet renderables
        :width width :height height
        :rows rows :columns columns))))

(defn- make-spreadsheets
  [renderables width height]
  ; TODO: collect multiple errors
  (p/all (map (make-spreadsheet width height)
              (partition-all 70 renderables))))

(defn- get-size
  [{{:keys [width height]} :front}]
  [width height])

; TODO: rewrite with promesa
; (defn auto-spreadsheets
;   [cards]
;   (let [sized-backed
;         (->> cards
;              (group-by get-size)
;              (transform [MAP-VALS] #(group-by :back %)))
;         sized-backed
;         (for [[[width height] backed] sized-backed
;               [back similar-cards] backed]
;           [width height back similar-cards])]
;     (loop [deck-number 1
;            acc {:sheets {} :backs {} :errors [] :card-ids {}}
;            [[width height back similar-cards] & rest] sized-backed]
;       (let [renderables (map (comp game/component-to-renderable :front)
;                              similar-cards)
;             back-renderable (game/component-to-renderable back)
;             back (<! (image/image back-renderable))
;             sheets (<! (make-spreadsheets renderables width height))
;             errors (concat (filter identity (map :errors sheets)))
;             sheets (map :value sheets)
;             deck-numbers (range deck-number (+ deck-number (count sheets)))
;             card-numbers (for [deck-number deck-numbers
;                                n (range 1 70)]
;                            (+ n (* 100 deck-number)))
;             next (merge-with into acc
;                    {:sheets (map vector deck-numbers sheets)
;                     :backs (map vector deck-numbers (repeat back))
;                     :errors errors
;                     :card-ids (map vector similar-cards card-numbers)})]
;         (if rest
;           (recur (+ deck-number (count sheets)) next rest)
;           (>! ret next))))))

(def ^:private tables
  {:octagon "Table_Octagon"
   :hexagon "Table_Hexagon"
   :circular "Table_Circular"
   :glass "Table_Glass"
   :poker "Table_Poker"
   :rpg "Table_RPG"
   :square "Table_Square"})

(def ^:private skies
  {:forest "Sky_Forest"
   :field "Sky_Field"
   :museum "Sky_Museum"
   :tunnel "Sky_Tunnel"})

(def ^:private default-object
  {:position {:x 0 :y 0 :z 0}
   :rotation {:x 0 :y 0 :z 0}
   :scale {:x 0 :y 0 :z 0}
   :name ""
   :description ""})

(def ^:private default-deck
  (merge default-object
    {:cards []}))

(def ^:private default-game
  {:save-name ""
   :game-mode ""
   :table :octagon
   :sky :forest
   :rules ""
   :objects []})
