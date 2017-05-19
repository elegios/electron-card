(ns electron-card.app
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.nodejs :as nodejs]
            [electron-card.eval :as ev]
            [cljs.core.async :refer [<! promise-chan put!]]
            [garden.core :refer [css]]
            [hipo.core :as hipo]))

(nodejs/enable-util-print!)

(def fs (js/require "fs"))

(defn read-file [path]
  (let [ret (promise-chan)]
    ; TODO: report error properly
    (.readFile fs path "utf8" #(put! ret %2))
    ret))

(def last-source (atom ""))
(def components-html (atom nil))

(defn render-result [{c :css h :html}]
  (let [comp-style (js/document.getElementById "components-style")
        comp-html [:div#components-html h]]
    (set! (.-innerHTML comp-style) (apply css c))
    (if @components-html
      (hipo/reconciliate! @components-html comp-html)
      (do
        (reset! components-html (hipo/create comp-html))
        (js/document.body.appendChild @components-html)))))

(defn file-changed [kind file]
  (if (not= kind "change")
    (println "not change, ignoring:" kind)
    (go
      (let [new-source (<! (read-file file))]
        (if (= @last-source new-source)
          (println "equal source, ignoring")
          (do
            (println "different source")
            (reset! last-source new-source)
            (let [{:keys [errors value]} (<! (ev/eval new-source))]
              (if value
                (render-result value)
                (println errors)))))))))

(defn init
  []
  (file-changed "change" "test.cljs")
  (.watch fs "test.cljs" #js{} file-changed)
  (println "init"))

(set! *main-cli-fn* init)
