(require '[clojure.string :as str])

(defn image-card
  ([src] (image-card src nil "None" "None"))
  ([src suit-src active passive]
   (let [suit-height (if suit-src "12mm" "0px")
         suit-container-img-css [:img {:max-width "100%" :max-height "100%"
                                       :display :block
                                       :width :auto :height :auto
                                       :top :auto :bottom :auto}]
         text (when (and active passive)
                [:div [:b.active "Active: "] active [:br]
                      [:b "Passive: "] passive])
         top-children [[:img {:src suit-src}]
                       [:img {:src suit-src}]]
         bottom-children [[:img {:src suit-src}]
                          text
                          [:img {:src suit-src}]]
         suit-container [:html :width "100%" :height suit-height
                               :css {:display :flex
                                     :flex-direction :row
                                     :justify-content :space-between
                                     :font-size "10pt"
                                     :color :gray}
                               :inner-css [suit-container-img-css
                                           [:.active {:margin-left "5px"}]]]]
     {:sort-key (str "1 " suit-src " " src)
      :width "63mm" :height "88mm"
      :elements [[:html :width "100%" :height "100%"
                        :css {:border-radius "2mm"
                              :background-color :black}]
                 (when suit-src
                   (conj suit-container :children top-children))
                 [:img :width "100%" :height (str "calc(100% - 2 * " suit-height ")")
                       :top suit-height
                       :src src]
                 (when suit-src
                   (conj suit-container :children bottom-children
                                        :bottom "0px"))]})))

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
   "(CEASEFIRE)" [:img {:src "yin-yang.svg"}]
   "(SPECIAL)" [:img {:src "eclipse-flare.svg"}]
   "\n" [:br]})

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
        title-font-size "16pt"
        title-underline-width "0.5mm"]
    {:sort-key (str "0 " title)
     :width "63mm" :height "88mm"
     :elements [[:html :width "100%" :height "100%"
                       :css {:border-radius "2mm"
                             :background-color "black"}]
                [:html :left "10%" :width "80%"
                       :children [title]
                       :css {:color :white
                             :text-align :center
                             :font-size title-font-size
                             :border-bottom-style :solid
                             :border-bottom-width title-underline-width}]
                [:html :left "5%" :right "5%"
                       :top "9.8mm"
                       :children (map symbolize-text texts)
                       :css {:color :white
                             :font-size body-font-size}
                       :inner-css [[:p {:margin-top "0px"}]
                                   [:img {:height body-font-size
                                          :width :auto
                                          :display :inline
                                          :position :relative
                                          :top "0.5mm"
                                          :transform "scale(1.2)"}]]]]}))

(def extra-data
  [{:title "Mechanical Might"
    :texts ["If a (MACHINE) basic you control would be destroyed you may do one of these instead:
             - Reveal and discard a non-(MACHINE) card.
             - Sacrifice a non-(MACHINE) permanent you control."
            "Reveal and discard a non-(MACHINE) card: Draw a card. You may unspend a basic (MACHINE) you control. Use only on your turn, and only once per turn."]}
   {:title "Exodia"
    :texts ["Your (SPECIAL) are permanents and can be played without a discard, one per turn."
            "If a (SPECIAL) you own would enter a discard pile, you may reveal it. If you do, shuffle it into your deck instead."
            "Destroy one (MAGIC)(SPECIAL), (NATURE)(SPECIAL) and (MACHINE)(SPECIAL) under your control: +5/+0"]}
   {:title "Giants"
    :texts ["Your (SPECIAL) are permanents and can be played for 2 discards."
            "(NATURE)(SPECIAL): Passive: +3/+0"
            "(MACHINE)(SPECIAL): Passive: +0/+2"
            "(MAGIC)(SPECIAL): Exile a card from your hand or board: Destroy a permanent. Use only on your turn, and only once per turn."]}
   {:title "Sticky Bombs"
    :texts ["Your (SPECIAL) are permanents that can be played by discarding a card."
            "When you play a (SPECIAL), take up to two cards from your graveyard, attach them to cards your opponent controls of the same kind. (If one of your attached cards would leave the board, exile it instead)"
            "(SPECIAL) Active: Destroy all cards your opponent controls that you have attached cards to (through the effect above). Destroy self."]}
   {:title "Trinities"
    :texts ["Three basic cards of the same kind but different suits form a trinity."
            "You may play a card that would complete a trinity without a discard."
            "Each trinity you control grants an effect depending on its kind:"
            "(WARRIOR): +1/+0
             (DEFENDER): +0/+1
             (HOUSE): +1 (DRAW)
             (RANGED): Spend a (RANGED) you control: Destroy a permanent. Use only on your turn, and only once per turn."]}
   {:title "Ninjas"
    :texts ["Twice per turn, when you play a basic permanent you may return a similar (i.e. same kind or suit) card you control to its owners hand instead of discarding a card."
            "If the swapped cards differ in:
             - Kind: The new card does not have summoning sickness.
             - Suit, new card (MAGIC): Take control of target permanent until end of turn.
             - Suit, new card (MACHINE): You may draw two cards, then exile one card from your hand.
             - Suit, new card (NATURE): +1/+0"]}
   {:title "Power"
    :texts ["Your basic (MAGIC) cards start the game exiled, and are exiled when they would leave the board."
            "When you play a basic permanent you may empower it (attach an exiled (MAGIC) card of the same kind)."
            "If an empowered card would be destroyed, exile the attached (MAGIC) card instead."
            "(DEFENDER): Passive: None.
             (RANGED): Passive: +X (ATK), where X is the number of empowered cards you control."]}])

(def back (image-card "castle.svg"))

(def basics
  [["white-tower.svg" "None" "Base draw = 3"]
   ["crossbow.svg" "+1/+0" "+1/+0"]
   ["crossed-swords.svg" "+0/+1" "+1/+0"]
   ["rosa-shield.svg" "+0/+1" "+0/+1"]
   ["power-lightning.svg"]
   ["eclipse-flare.svg"]
   ["yin-yang.svg" "Exile self." "-3/+3"]])

(def suits
  ["sundial.svg"
   "plants-and-animals.svg"
   "big-gear.svg"])

(def fronts
  (for [[icon active passive] basics suit suits]
    (image-card icon suit active passive)))

(def extras
  (map extra-card extra-data))

; DPI for tabletopia 254(?)
{:type :component-collection
 :components (into (conj fronts back) extras)}
