(ns electron-card.game.component-collection
  (:require [electron-card.game :as game]
            [electron-card.renderable :as renderable]
            [electron-card.image :as image]
            [promesa.core :as p]
            [clojure.spec.alpha :as s]
            [clojure.string :as str]))

(s/def ::components (s/coll-of :electron-card.game/component))

(def ^:private path (js/require "path"))
(def ^:private fs (js/require "fs"))

(defn- make-save-fn
  [directory]
  ; TODO: ensure directory exists
  (let [file-num (atom 0)]
    (fn [{:keys [data-url sort-key]}]
      (p/promise
        (fn [resolve reject]
          (let [num (swap! file-num inc)
                path (.join path directory (str sort-key "_" num ".png"))
                data (str/replace data-url #"^data:image/png;base64," "")]
            (.writeFile fs path data "base64"
              (fn [err]
                (if err
                  (reject [err])
                  (resolve path))))))))))

(defmethod game/game-type :component-collection [_]
  (s/keys :req-un [:electron-card.game/type ::components]))

(defmethod game/extract-components :component-collection
  [{:keys [components]}]
  (set components))

(defmethod game/export :component-collection
  [{:keys [components]} directory]
  (let [components (set components)
        upload-fn (make-save-fn directory)]
    (->> components
         (map renderable/component-to-renderable)
         (map image/image)
         (map #(p/then % upload-fn))
         p/all)))
