(ns icky.core
  (:require
   [reagent.core :as reagent]
   [icky.editor :as e]
   [icky.radix :refer [insert] :refer-macros [console-time]]
   [clojure.string :refer [replace split index-of]]
   [goog.json :as gson]
   [goog.functions :refer [debounce]]
   [icky.complete :refer [get-patterns matched-positions re-pattern-highliter get-matches search-box]]
   ;; [icky.data :refer [commands]]
   [re-frisk.core :as rf]
   [icky.music :refer [songs]]
   [icky.files :refer [project-files]])

  (:import [goog.net XhrIo]
           [goog.structs Map]
           [goog.Uri QueryData]
           [goog.events KeyCodes]))

(defonce app-state
  (reagent/atom {}))

(defonce code-state (reagent/atom {:style {} :js-cm-opts {} :on-cm-int (fn [cm])}))

(defonce attribute-state (reagent/atom {}) )

(defn load-remote []
  (.send XhrIo "/columns" (fn [e]
                            (let [data (js->clj (.getResponseJson (.-target e)) :keywordize-keys false)]
                              (reset! attribute-state data)))))

(defn load-data-set! [data-set]
  (.send XhrIo (str "/data/" (name data-set)) (fn [e] (let [data (js->clj (.getResponseJson (.-target e)) :keywordize-keys false)]
                                                 (swap! app-state assoc (keyword data-set) (js->clj data) )))))

;;(doall (map load-data-set! (filter #(complement (get @app-state %)) '(:commands :project-files :songs))))

(defn save [data]
  (let [m (.stringify js/JSON (clj->js data))
        qd (QueryData.)]
    (.add qd "data" m)
    (.send XhrIo "/save" (fn []) "POST" qd)))

(defn page [ratom]
  [:div
   [:p (:text @ratom) "FIXME"]
   [:p (count (get @app-state :commands 0))]])

(def my-songs (into #{} songs))

(defn attribute [subcat prefix name slug enabled]
  ^{:key (str subcat prefix name slug)}
  [:div.attribute-container [:span (str prefix "="  slug " eanbled: " enabled)] [:span name]
   #_(str prefix " : " name ": " slug)]
  )

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

(defn cell [] [:div.cell-wrapper [:div.outer-cursor {} [:div.cursor "Text"]]])

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

(def todo-edit (with-meta search-box
                 {:component-did-mount #(print "mounted")}))

(defonce my-html (reagent/atom ""))

(defn plain-component []
  [:p "My html is " @my-html])

(def component-with-callback
  (with-meta plain-component
    {:component-did-mount
     (fn [this]
       (.log js/console  "mounted"))}))

(defn my-component
  [x y z]
  (let [a :a]
    (reagent/create-class                 ;; <-- expects a map of functions
     {:component-did-mount               ;; the name of a lifecycle function
      #(println "component-did-mount")   ;; your implementation

      :component-will-mount              ;; the name of a lifecycle function
      #(println "component-will-mount")  ;; your implementation

      ;; other lifecycle funcs can go in here

      :display-name  "my-component"  ;; for more helpful warnings & errors

      :reagent-render        ;; Note:  is not :render
      (fn [x y z]           ;; remember to repeat parameters
        [:div (str x " " y " " z)])})))

(defn reload []
  #_(reagent/render [page app-state]
                    (.getElementById js/document "app"))
  (let [search (reagent/render [search-box my-songs]
                               (.getElementById js/document "search"))]
    (.log js/console search))
  (reagent/render [attribute-tree attribute-state]
                  (.getElementById js/document "attributes"))

  (reagent/render [my-component :a :b :c] (.getElementById js/document "excel"))

  #_(reagent/render [export attribute-state] (.getElementById js/document "export"))

  #_(reagent/render [grid] (.getElementById js/document "grid")))

(defn ^:export main []
  (print "main called")
  (rf/enable-frisk!)

  (reload))
