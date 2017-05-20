(ns electron-card.game)

(def last-id (atom 0))

(defn component-to-renderable
  [{:keys [css html width height]}]
  (let [css (if (keyword? (first css)) [css] css)
        html (if (keyword? (first html)) [html] html)
        id-str (str (swap! last-id inc))
        calc-width (str "calc(var(--component-scale) * " width ")")
        calc-height (str "calc(var(--component-scale) * " height ")")
        css [(keyword (str ".outer" id-str))
             {:min-width calc-width :max-width calc-width
              :min-height calc-height :max-height calc-height}
             (apply vector (keyword (str ".inner" id-str))
                           {:min-width width :max-width width
                            :min-height height :max-height height}
                           css)]
        html [(keyword (str "div.outer" id-str))
              (apply vector (keyword (str "div.inner" id-str)) html)]]
    {:css css :html html}))

(defmulti extract-components :type)

(defmethod extract-components :component-collection
  [{:keys [components]}]
  (set components))

(defmethod extract-components :default
  [coll]
  (set coll))