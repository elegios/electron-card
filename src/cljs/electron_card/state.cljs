(ns electron-card.state
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [electron-card.game :refer [extract-components]]
            [electron-card.eval :as ev]
            [clojure.set :as set]
            [cljs.core.async :refer [promise-chan put! <!]]))

(def ^:private fs (js/require "fs"))

(def ^:private watcher (atom nil))

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
  (let [ret (promise-chan)]
    ; TODO: report error properly
    (.readFile fs path "utf8" #(put! ret %2))
    ret))

(defn- file-changed [kind file]
  (when (= kind "change")
    (go
      (let [new-source (<! (read-file file))]
        (when (not= @last-source new-source)
          (reset! last-source new-source)
          (let [errs errors
                {:keys [errors value]} (<! (ev/eval new-source))]
            (reset! errs errors)
            (when value
              (reset! last-components (extract-components @game))
              (reset! game value))))))))

(defn get-new-components []
  (set/difference (extract-components @game) @last-components))

(defn get-all-components []
  (extract-components @game))

(defn watch-file [path]
  (when @watcher
    (.close @watcher))
  ; TODO: report error probably
  (reset! watcher (.watch fs path #js{} file-changed))
  (file-changed "change" path))
