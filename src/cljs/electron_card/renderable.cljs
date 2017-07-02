(ns electron-card.renderable
  (:require [electron-card.state :refer [get-dir]]
            [clojure.string :as str]
            [com.rpl.specter :refer [must walker]
                             :refer-macros [transform]]))

(def ^:private node-path (js/require "path"))

(def ^:private last-id (atom 0))

(defn- is-img
  [arg]
  (and (vector? arg)
       (keyword? (arg 0))
       (str/starts-with? (str (arg 0)) ":img")))

(defn- fix-path
  [path]
  (cond
    (re-find #"^\w+://" path)
    path

    (.isAbsolute node-path path)
    (str "file://" path)

    :default
    (.join node-path (get-dir) path)))

(defn component-to-renderable
  [{:keys [css html width height]}]
  (let [css (if (keyword? (first css)) [css] css)
        html (if (keyword? (first html)) [html] html)
        html (transform [(walker is-img) (must 1) (must :src)]
               fix-path
               html)
        id-str (str (swap! last-id inc))
        calc-width (str "calc(var(--component-scale) * " width ")")
        calc-height (str "calc(var(--component-scale) * " height ")")
        css [(keyword (str ".outer" id-str))
             {:min-width calc-width :max-width calc-width
              :min-height calc-height :max-height calc-height}
             (apply vector (keyword (str ".comp" id-str))
                           {:width width :height height}
                           css)]
        html [(keyword (str "div.outer" id-str))
              (apply vector (keyword (str "div.comp" id-str)) html)]]
    {:css css :html html}))
