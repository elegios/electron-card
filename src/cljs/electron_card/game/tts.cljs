(ns electron-card.game.tts
  (:require [electron-card.game :as game]
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
