(ns electron-card.upload.file
  (:require [promesa.core :as p]
            [clojure.string :as str]))

(def ^:private path (js/require "path"))
(def ^:private fs (js/require "fs"))

(defn make-save-fn
  [directory]
  (let [file-num (atom 0)]
    (fn [data-url]
      (p/promise
        (fn [resolve reject]
          (let [num (swap! file-num inc)
                path (.join path directory (str num ".png"))
                data (str/replace data-url #"^data:image/png;base64," "")]
            (.writeFile fs path data "base64"
              (fn [err]
                (if err
                  (reject [err])
                  (resolve path))))))))))

