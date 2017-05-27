(ns electron-card.game.tts
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [electron-card.game :as game]
            [electron-card.image :as image]
            [cljs.core.async :as async :refer [promise-chan <! >! to-chan close!]]
            [com.rpl.specter :refer [ALL STAY must multi-path traverse]]))

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

(def ^:private sizes
  (vec
    (into (sorted-map)
      (for [rows (range 2 (inc 10))
            columns (range 2 (inc (min rows 7)))]
        [(* rows columns) [rows columns]]))))

(defn- auto-spreadsheet [width height]
  (fn [renderables out]
    (go
      (let [[[rows columns]] (filter
                               (fn [[size]] (<= size (count renderables)))
                               sizes)]
        (>! out (<! (image/spreadsheet renderables
                      :width width :height height
                      :rows rows :columns columns)))
        (close! out)))))

(defn auto-spreadsheets
  [renderables & {:keys [width height ret]}]
  (let [ret (or ret (promise-chan))]
    (go
      (let [partitions (to-chan (partition-all 70 renderables))
            ch (async/chan 5)
            _ (async/pipeline-async 5 ch (auto-spreadsheet width height) partitions)
            sheets (<! (async/into [] ch))
            errors (filter identity (map :errors sheets))
            values (map :value sheets)]
        (>! ret
          (if (seq errors)
            {:errors (concat errors)}
            {:value
              {:spreadsheets
               (into {} (map vector (rest (range)) values))
               :card-ids
               (into {} (map vector renderables
                          (for [hundreds (rest (range))
                                nums (range 1 71)]
                            (+ nums (* 100 hundreds)))))}}))))
    ret))
