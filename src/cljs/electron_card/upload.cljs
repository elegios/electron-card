(ns electron-card.upload
  (:require [promesa.core :as p]))

(defn mk-fake-uploader
  []
  (let [prev-num (atom 0)]
    (fn [{:keys [sort-key]}]
      (str sort-key "_" (swap! prev-num inc)))))

(defn cache-uploader
  [upload-fn & {:keys [starting-cache]}]
  (let [cache (atom (or starting-cache {}))]
    (fn [{:keys [data-url] :as arg}]
      (if-let [prev-url (@cache data-url)]
        (p/promise prev-url)
        (p/then (upload-fn arg)
          (fn [value]
            (swap! cache assoc data-url value)
            value))))))

