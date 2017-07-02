(ns electron-card.state
  (:require [electron-card.game :refer [extract-components]]
            [electron-card.eval :as ev]
            [clojure.set :as set]
            [promesa.core :as p]))

(def ^:private node-path (js/require "path"))
(def ^:private fs (js/require "fs"))

(def ^:private watcher (atom nil))
(def ^:private dir (atom nil))

(def ^:private last-source (atom ""))
(def ^:private game (atom {:type :empty}))
(def ^:private errors (atom []))
(def ^:private last-components (atom #{}))

(defn add-game-update-fn [key f]
  (add-watch game key #(f %4)))
(defn remove-game-update-fn [key]
  (remove-watch game key))

(defn add-errors-update-fn [key f]
  (add-watch errors key #(f %4)))
(defn remove-errors-update-fn [key]
  (remove-watch errors key))

(defn- read-file [path]
  (p/promise
    (fn [resolve reject]
      (.readFile fs path "utf8"
        (fn [err data]
          (if err
            (reject [err])
            (resolve data)))))))

(defn- file-changed [kind file]
  (when (= kind "change")
    (-> (read-file (.join node-path @dir file))
        (p/then
          (fn [new-source]
            (when (not= @last-source new-source)
              (reset! last-source new-source)
              (p/then (ev/eval new-source)
                (fn [value]
                  (reset! errors [])
                  (reset! last-components (extract-components @game))
                  (reset! game value))))))
        (p/catch #(reset! errors (if (coll? %) % [%]))))))

(defn get-dir []
  @dir)

(defn get-state []
  @game)

(defn get-new-components []
  (set/difference (extract-components @game) @last-components))

(defn get-all-components []
  (extract-components @game))

(defn watch-file [path]
  (when @watcher
    (.close @watcher))
  (reset! dir (.dirname node-path path))
  ; TODO: report error probably
  (reset! watcher (.watch fs path #js{} file-changed))
  (file-changed "change" (.basename node-path path)))

(defn add-errors [errs]
  (swap! errors into errs))
