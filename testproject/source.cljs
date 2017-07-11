(require '[clojure.string :as str])

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

(defn regex-escape
  [literal]
  (str/replace literal #"[-[\]{}()*+!<=:?.\/\\^$|#\s,]" "\\$&"))

(def literal-replacements
  {"(MACHINE)" [:img {:src "big-gear.svg"}]
   "(MAGIC)" [:img {:src "sundial.svg"}]
   "(NATURE)" [:img {:src "plants-and-animals.svg"}]
   "(HOUSE)" [:img {:src "white-tower.svg"}]
   "(RANGED)" [:img {:src "crossbow.svg"}]
   "(WARRIOR)" [:img {:src "crossed-swords.svg"}]
   "(DEFENDER)" [:img {:src "rosa-shield.svg"}]
   "(ATTACK)" [:img {:src "power-lightning.svg"}]
   "(SPECIAL)" [:img {:src "eclipse-flare.svg"}]})

(def keywords
  #{"atk" "draw" "spend" "unspend" "passive" "exile" "destroy" "discard"
    "permanent" "permanents"})

(defn regex-find
  [re s]
  (when-let [result (.exec re s)]
    {:index (.-index result)
     :result (aget result 0)}))

(defn symbolize-text
  [text]
  (let [re (->> literal-replacements
                 keys
                (map regex-escape)
                (str/join "|")
                re-pattern)]
    (loop [res []
           text text]
      (if-let [{:keys [index result]} (regex-find re text)]
        (recur (conj res
                     (subs text 0 index)
                     (literal-replacements result))
               (subs text (+ index (count result))))
        [:p (filter #(and % (not= "" %))
                    (conj res text))]))))

(defn extra-card
  [{:keys [title texts]}]
  (let [body-font-size "10pt"
        title-underline-width "0.5mm"]
    {:css [[:.background {:width "100%" :height "100%"
                          :border-radius "2mm"
                          :background-color :black}]
           [:.title {:color :white
                     :width "80%"
                     :text-align :center
                     :left "10%"
                     :top "2mm"
                     :font-size "16pt"
                     :border-bottom-style :solid
                     :border-bottom-width title-underline-width}]
           [:.texts {:top "9.8mm"
                     :color :white
                     :font-size body-font-size
                     :width "90%"
                     :left "5%"}
            [:p {:margin-top "0px"}]
            [:img {:height body-font-size
                   :width :auto
                   :display :inline
                   :position :relative
                   :top "0.5mm"
                   :transform "scale(1.2)"}]]]
     :html [[:div.background]
            [:div.title title]
            [:div.texts
             (map symbolize-text texts)]]
     :width "63mm"
     :height "88mm"}))

(def extra-data
  [{:title "Mechanical Might"
    :texts ["Your non-(MACHINE) basics cannot be spent"
            "Your (MACHINE) basics: Sacrifice a non-(MACHINE): Unspend self"
            "(MACHINE)(RANGED): Spend: +2 (ATK)"
            "(MACHINE)(HOUSE): Spend: +1 (DRAW)"
            "(MACHINE)(DEFENDER): Spend: Return a permanent to its owners hand"
            "(ATTACK): Effect: Unspend"]}
   {:title "Exodia"
    :texts ["Your (SPECIAL) are permanents and can be played without a discard, one per turn."
            "Destroy one (MAGIC)(SPECIAL), (NATURE)(SPECIAL) and (MACHINE) (SPECIAL) under your control: +5 (ATK)"]}
   {:title "Giants"
    :texts ["Your (SPECIAL) are permanents and can be played for 2 discards."
            "(NATURE)(SPECIAL): +3 (ATK)"
            "(MACHINE)(SPECIAL): +2 (DEF)"
            "(MAGIC)(SPECIAL): Exile a card from your hand or board: Destroy a permanent."]}])

(def back (image-card "castle.svg"))

(def icons
  ["white-tower.svg"
   "crossbow.svg"
   "crossed-swords.svg"
   "rosa-shield.svg"
   "power-lightning.svg"
   "eclipse-flare.svg"])

(def suits
  ["sundial.svg"
   "plants-and-animals.svg"
   "big-gear.svg"])

(def fronts
  (for [icon icons suit suits]
    (image-card icon suit)))

(def extras
  (map extra-card extra-data))

{:type :component-collection
 :components (into (conj fronts back) extras)}
