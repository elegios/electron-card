(ns electron-card.upload
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.core.async :refer [<!]]))

(defn mk-fake-uploader
  []
  (let [prev-num (atom 0)]
    (fn []
      (go {:value (str (swap! prev-num inc))}))))

(defn cache-uploader
  [upload-fn & {:keys [starting-cache]}]
  (let [cache (atom (or starting-cache {}))]
    (fn [data-url]
      (go
        (if-let [prev-url (@cache data-url)]
          {:value prev-url}
          (let [{:keys [error value] :as res} (<! (upload-fn data-url))]
            (when value
              (swap! cache assoc data-url value))
            res))))))

