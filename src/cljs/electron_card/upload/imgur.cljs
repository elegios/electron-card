(ns electron-card.upload.imgur
  (:require [electron-card.keys :refer [imgur-clientid]
                                :rename {imgur-clientid clientid}]
            [cljs.core.async :refer [promise-chan put!]]))

; TODO: request brings in a lot of dependencies, examine doing without
(def ^:private request
  (.defaults (js/require "request")
    #js{:json true
        :headers #js{:authorization (str "Client-ID " clientid)}}))

(defn- format
  [response]
  (if (response "success")
    {:value ((response "data") "link")}
    {:error response}))

(defn upload
  [data-url]
  (let [ret (promise-chan)]
    (.post request
      #js{:url "https://api.imgur.com/3/upload"
          :formData #js{:type "base64" :image data-url}}
      (fn [err _ body]
        (put! ret
          (if err
            {:error err}
            (format body)))))
    ret))
