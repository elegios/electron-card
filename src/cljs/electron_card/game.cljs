(ns electron-card.game
  (:require [clojure.spec.alpha :as s]))

(defmulti extract-components :type)

(defmethod extract-components :empty
  [_]
  #{})

(defmethod extract-components :component-collection
  [{:keys [components]}]
  (set components))

(defmethod extract-components :default
  [coll]
  ; TODO: report error probably
  (set coll))

(defmulti game-type :type)

(s/def ::game (s/multi-spec game-type :type))

(s/def ::sort-key string?)

(s/def ::width string?)
(s/def ::height string?)
(s/def ::top string?)
(s/def ::bottom string?)
(s/def ::left string?)
(s/def ::right string?)

(s/def ::css (s/map-of keyword? any?))

; TODO: errors within css specs won't be reported nicely
(defn- inner-css?
  [[key css & children]]
  (and (keyword? key)
       (s/valid? ::css css)
       (every? inner-css? children)))
(s/def ::inner-css (s/coll-of inner-css?))

; TODO: errors within html specs won't be reported nicely
(declare html?)
(defn- html-child?
  [child]
  (or (nil? child)
      (string? child)
      (and (seq? child)
           (every? html-child? child))
      (html? child)))
(defn- html?
  [spec]
  (s/valid? (s/cat :key keyword?
                   :attributes (s/? (s/map-of keyword? any?))
                   :children (s/* html-child?))
            spec))
(s/def ::children (s/coll-of html-child?))

(s/def ::src string?)

(defmulti ^:private element-type first)
(defmethod element-type nil [_]
  any?)
(defmethod element-type :html [_]
  (s/cat :type #{:html}
         :opts (s/keys* :opt-un [::width ::height ::top ::bottom ::left ::right
                                 ::children ::css ::inner-css])))
(defmethod element-type :img [_]
  (s/cat :type #{:img}
         :opts (s/keys* :req-un [::src]
                        :opt-un [::width ::height ::top ::bottom ::left ::right
                                 ::css])))

; TODO: not sure about the retag function here, though it's only relevant for generation of things(?)
(s/def ::element (s/multi-spec element-type #(vec (conj (seq %1) %2))))

(s/def ::elements (s/coll-of ::element :kind sequential?))

(s/def ::component (s/keys :req-un [::width ::height ::elements ::sort-key]))
(s/def ::components (s/coll-of ::component))

(defmethod game-type :component-collection [_]
  (s/keys :req-un [::type ::components]))
