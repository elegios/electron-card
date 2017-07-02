(ns electron-card.game)

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
