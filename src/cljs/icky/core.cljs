(ns icky.core
  (:require
   [reagent.core :as reagent]
   [re-frisk.core :as rf]
   [devtools.core :as devtools]
   [icky.editor :as e]
   [clojure.string :refer [replace split]]
   [goog.json :as gson]
   ;;[goog.Uri.QueryData]
   [dirac.runtime]
   [icky.data :refer [commands]]
   [icky.music :refer [songs]])

  (:import [goog.net XhrIo]
           [goog.structs Map]
           [goog.Uri QueryData]
           [goog.events KeyCodes]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Vars

(defonce debug?
  ^boolean js/goog.DEBUG)

(defonce app-state
  (reagent/atom
   {:text "Hello, what is your name? "}))


(defonce code-state (reagent/atom {:style {} :js-cm-opts {} :on-cm-int (fn [cm])}))
;; (defonce client (js/algoliasearch "Z8CHXVEJ2D" "1e408a8a03a2ad57391ba18345680301"))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Page
(defonce attribute-state (reagent/atom {}) )

(defn load-remote []
  (.send XhrIo "/columns" (fn [e]
                            (let [data (js->clj (.getResponseJson (.-target e)) :keywordize-keys false)]
                              (reset! attribute-state data)))))

(defn save [data]
  (let [m (.stringify js/JSON (clj->js data))
        qd (QueryData.)]
    (.add qd "data" m)
    (.send XhrIo "/save" (fn []) "POST" qd)))

(defn page [ratom]
  [:div
   [:p (:text @ratom) "FIXME"]
   [:p (count commands)]])


;; (defconst helm-mm-space-regexp "\\s\\\\s-"
;;  (replace-regexp-in-string helm-mm-space-regexp "\\s-" pattern nil t)))
;;  "this is the\\ fun".replace(/\\ /, '\\s').split(/\s/)

(defn get-patterns-fn [input]
  (let [patterns (-> input
                     (replace #"\\\s" "\\s")
                     (replace #"\\\s*$", "")
                 (split #"\s"))]
    (fn [to-match] (reduce #(if (re-find (re-pattern (str "(?i)" %2)) to-match ) true (reduced false)) false patterns))))

(defn get-matches [input state] (let [filter-fn (get-patterns-fn input)
                                last-input (get @state :input "" )
                                candidates songs]
                             (swap! state assoc :input input)
                            (filter filter-fn candidates)))

(defn search-candidate [value]
  ^{:key value} [:div.solid value])

(defn search-box [ratom]
  (let [state (reagent/atom {:matches []})
        change-fn (fn [e] (let [val (.. e -target -value)
                               ms (if (= val "") [] (get-matches val state))]
                           (swap! state assoc :matches ms)))]
    (fn []
      [:div.search-container
       [:input.smooth
        {:on-change change-fn
         :on-blur #(.log js/console "blur")}]
        [:div [:p (str "text " (count (:matches @state)))]
        (for [item (:matches @state)] (search-candidate item) )
        ]])))

(defn attribute [subcat prefix name slug enabled]
  ^{:key (str subcat prefix name slug)}
  [:div.attribute-container [:span (str prefix "="  slug " eanbled: " enabled)] [:span name]
   #_(str prefix " : " name ": " slug)]
  )

(defn dev-setup []
  (when debug?
    (enable-console-print!)
    (rf/enable-frisk!)
    #_(rf/add-data :app-state app-state)
    (rf/add-data :attribute-state attribute-state)
    (dirac.runtime/install!)
    (devtools/install!)))

(defn attributes [type als]
  ^{:key type} [:div
                [:div.attribute (for [kids (get als ":children")]
                                  (attribute (get als ":name") type (get kids ":name") (get kids ":slug") true))
                 [:div.attribute-add {:on-click (fn [e] (.stopPropagation e))} (str "Add to " type)]]])

(defn sub-category [cat e]
  (let [name (get @e ":name")
        slug (get @e ":slug")
        children (get @e ":children")
        visible? (:expanded? @e)
        toggle (fn [ev]
                 (swap! e assoc :expanded? (not visible?))
                 (.stopPropagation ev))]
    ^{:key (str cat name)} [:div.sub-category {:tabIndex 0
                                               :on-click toggle
                                               :on-key-press (fn [e]
                                                               (let [key-code (.. e -keyCode)]
                                                                 (when (= key-code (.. KeyCodes -ENTER)
                                                                          (toggle e))))
                                                               (.preventDefault e))}
                            [:i.fa.fa-plus-circle]
                            [:span.item name]
                            (when visible?
                              [:div.container
                               [:span.item slug]
                               [:div (for [[type attrib] (get @e ":attributes")] (attributes type attrib))]])]))


(defn sort-handler [e] (let [key-code (.. e -keyCode )
                             target (.. e -target)]
                         (.log js/console (.-tagName target))
                         ["INPUT" "SELECT" "TEXTAREA"]
                         ;; element.tagName == 'INPUT' || element.tagName == 'SELECT' || element.tagName == 'TEXTAREA' || element.isContentEditable;
                         (if (= key-code (.. KeyCodes -ENTER)
                                #_(toggle e)
                                )
                           (.preventDefault e))))

(defn selection-expander [e]
  (when-let [v (js/parseInt (.. e -target -value))]
    (swap! cat assoc ":order" v))
  (.preventDefault e)
  (.stopPropagation e))

(defn category [idx cat]
  (let [name (get @cat ":name")
        slug (get @cat ":slug")
        children (get @cat ":children")
        order (get @cat ":order")
        visible? (:expanded? @cat)
        toggle (fn [ev] (swap! cat assoc :expanded? (not visible?)))]

    ^{:key (str cat name)} [:div.category.row {:tabIndex 0 :on-click toggle :on-key-press sort-handler}
                            [:div.col
                             [:i.fa.fa-plus-circle]
                             [:span.item [:b name]]]
                            [:div.col
                             [:input {:type "text" :size 2 :default-value order :on-blur selection-expander}]]
                            (when visible?
                              [:div.container
                               [:span.item name]
                               [:span.item slug]
                               [:div (doall (map-indexed #(sub-category name (reagent/cursor cat [":children" %1]))  children))]])]))

(defn attribute-tree [state]
  (fn []
    (let [categories (sort-by #(get % ":order" 0) (get @state "categories"))]
      ;; (.log js/console  categories)
      [:div.container
       [:div.row
        [:div.col]
        [:div.col
         (doall (map
                 (fn [i]
                   (let [idx (reduce #(if (= (get %2 ":slug") (get i ":slug")) (reduced %1) (inc %1)) 0 (get @state "categories"))]
                     (category idx (reagent/cursor attribute-state ["categories" idx]))))
                 categories))]
        [:div.col]]])))

(defn cell [] [:div.cell-wrapper [:div.outer-cursor [:div.cursor "Text"]]])

(defn export [data]
  [:div [:button {:on-click
                  (fn []
                    (.log js/console @data)
                    (save @data))}
         "export"]])

(def grid-size 10)

(defn grid-cell [cell]
  [:span cell])
(defn grid-row [rows]
  [:div.ik-column (doall (map grid-cell rows))])

(defn grid []
  [:div
   #_(doall (map grid-row (range grid-size)))
   (doall
    (map
     (fn [x] ^{:key x} [:div.ik-column
                        (doall (map (fn [y] [:div.ik-cell [:b (str x " - " y)]])) (range grid-size)) ])
     (range grid-size) ))])


(defn editor [])

(defn reload []
  #_(reagent/render [page app-state]
                    (.getElementById js/document "app"))
  (reagent/render [search-box app-state]
                  (.getElementById js/document "search"))
  (reagent/render [attribute-tree attribute-state]
                  (.getElementById js/document "attributes"))
  #_(reagent/render [cell] (.getElementById js/document "excel"))

  #_(reagent/render [export attribute-state] (.getElementById js/document "export"))

  #_(reagent/render [grid] (.getElementById js/document "grid"))

  #_(reagent/render [e/code-mirror code-state] (.getElementById js/document "editor")))


(defn ^:export main []
  (dev-setup)
  (reload))
