(ns electron-card.renderable
  (:require [electron-card.state :refer [get-dir]]
            [clojure.string :as str]
            [clojure.spec.alpha :as s]
            [com.rpl.specter :refer [must walker]
                             :refer-macros [transform]]
            [garden.selectors :as selectors :refer [nth-child]]))

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

(selectors/defselector immediate ">")

(defn- to-css
  [idx {{:keys [width height top bottom left right css inner-css]} :opts :as element}]
  (into
    [(immediate :div (nth-child (str (inc idx))))
     (merge (into {}
                  (filter second)
                  [[:width width] [:height height]
                   [:top top] [:bottom bottom]
                   [:left left] [:right right]])
            css)]
    inner-css))

(defmulti ^:private to-html :type)

(defmethod to-html :img
  [{{:keys [src]} :opts}]
  [:div.image-container
   [:img {:src src}]])

(defmethod to-html :html
  [{{:keys [children]} :opts}]
  (apply vector :div children))

(defn component-to-renderable
  [component]
  (let [{:keys [width height elements sort-key]}
        (s/conform :electron-card.game/component component)
        id-str (str (swap! last-id inc))
        calc-width (str "calc(var(--component-scale) * " width ")")
        calc-height (str "calc(var(--component-scale) * " height ")")
        elements (filter identity elements)
        element-css (map-indexed to-css elements)
        element-html (map to-html elements)
        css [(keyword (str ".outer" id-str))
             {:min-width calc-width :max-width calc-width
              :min-height calc-height :max-height calc-height}
             (apply vector (immediate :div)
                           {:width width :height height}
                           element-css)]
        html [(keyword (str "div.outer" id-str))
              (apply vector :div element-html)]
        html (transform [(walker is-img) (must 1) (must :src)]
                        fix-path html)]
    {:css css :html html :sort-key sort-key}))
