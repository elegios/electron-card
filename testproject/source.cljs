(defn image-card
  ([src] (image-card src nil))
  ([src suit-src]
   (let [suit-height (if suit-src "12mm" "0px")]
     {:css [[:.background {:width "100%" :height "100%"
                           :border-radius "2mm"
                           :background-color :black}]
            [:.icon-container {:width "100%" :height (str "calc(100% - 2 * " suit-height ")")
                               :top suit-height
                               :display :flex
                               :flex-direction :column
                               :justify-content :center}
             [:img {:max-width "100%" :max-height "100%"
                    :display :block
                    :width :auto
                    :height :auto
                    :top :auto
                    :bottom :auto}]]
            [:.top {:top "0px"}]
            [:.bottom {:bottom "0px"}]
            [:.suit-container {:width "100%" :height suit-height
                               :display :flex
                               :flex-direction :row
                               :justify-content :space-between}
             [:img {:max-width "100%" :max-height "100%"
                    :display :block
                    :width :auto
                    :height :auto
                    :top :auto
                    :bottom :auto}]]]
      :html [[:div.background]
             (when suit-src
               [:div.suit-container.top
                [:img {:src suit-src}]
                [:img {:src suit-src}]])
             [:div.icon-container
              [:img {:src src}]]
             (when suit-src
               [:div.suit-container.bottom
                [:img {:src suit-src}]
                [:img {:src suit-src}]])]
      :width "63mm"
      :height "88mm"})))

(def back (image-card "castle.svg"))

(defn to-card
  [front-url]
  {:type :card
   :front (image-card front-url)
   :back back})

(def icons
  ["white-tower.svg"
   "crossbow.svg"
   "crossed-swords.svg"
   "rosa-shield.svg"
   "power-lightning.svg"])

(def suits
  ["sundial.svg"
   "plants-and-animals.svg"
   "big-gear.svg"])

(def fronts
  (for [icon icons suit suits]
    (image-card icon suit)))

{:type :component-collection
 :components (conj fronts back)}
