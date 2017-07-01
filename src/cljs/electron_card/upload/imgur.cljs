(ns electron-card.upload.imgur
  (:require [electron-card.keys :refer [imgur-clientid]
                                :rename {imgur-clientid clientid}]
            [promesa.core :as p]))

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
  (p/promise
    (fn [resolve reject]
      (.post request
        #js{:url "https://api.imgur.com/3/upload"
            :formData #js{:type "base64" :image data-url}}
        (fn [err _ body]
          (cond
            err (reject err)
            (body "success") (resolve ((body "data") "link"))
            :default (reject [body])))))))
