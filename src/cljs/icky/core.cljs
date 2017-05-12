(ns icky.core
  (:require
   [reagent.core :as reagent]
   [re-frisk.core :as rf]
   [devtools.core :as devtools]
   [icky.editor :as e]
   [clojure.string :refer [replace split index-of]]
   [goog.json :as gson]
   [goog.functions :refer [debounce]]
   ;;[goog.Uri.QueryData]
   [dirac.runtime]
   [icky.data :refer [commands]]
   [icky.music :refer [songs]]
   [icky.files :refer [project-files]])

  (:import [goog.net XhrIo]
           [goog.structs Map]
           [goog.Uri QueryData]
           [goog.events KeyCodes]))

(defonce debug?
  ^boolean js/goog.DEBUG)

(defonce app-state

  (reagent/atom {}))

(defonce code-state (reagent/atom {:style {} :js-cm-opts {} :on-cm-int (fn [cm])}))

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

(def my-songs (into #{} songs))

(defn element-in-viewport
  "is any part of the element in the viewport? adapted from:
   HTTP://stackoverflow.com/questions/123999/how-to-tell-if-a-dom-element-is-visible-in-the-current-viewport/7557433#7557433"
  [e bounding-element] (let [rect (.getBoundingClientRect e)
                              bounding-rect (.getBoundingClientRect bounding-element)]
        (and (>= (.-top rect) 0) (>= (.-left rect) 0)
             #_(<= (.-bottom rect) (or (.-innerHeight js/window) (.. js/document -documentElement -clientHeight)))
             #_(<= (.-right rect) (or (.-innerWidth js/window) (.. js/document -documentElement -clientWidth)))
             (<= (.-top rect) (.-bottom bounding-rect))
             (<= (.-right rect) (.-right bounding-rect)))))

(defn get-patterns [pattern]
  (let [patterns (-> pattern
                     (replace #"\\\s" "\\s")
                     (replace #"\\\s*$"  "")
                     (split #"\s"))
        result (map #(if (= "!" (first %))
                       [not (re-pattern(str "(?i)" (subs % 1)))]
                       [identity (re-pattern(str "(?i)" %))])
                    patterns)]
    result))

(defn matched-positions [pat candidate]
  (let [[f re] pat]
    (loop [candidate candidate offset 0 matches []]
      (if-let [match (re-find re candidate)]
        (let [start (index-of candidate (f match))
              end (+ start (count match))]
          (recur (subs candidate end) (+ end offset) (conj matches [(+ offset start)  (+ offset end)])))
        matches))))

(defn re-pattern-highliter
  "highlight a match based on the re patterns that matched it"
  [patterns candidate]
  (let [match-indices (->> patterns
                           (map #(matched-positions % candidate))
                           (apply concat)
                           (sort-by first))
        optimal-matches (reduce
                         (fn [candidates next-candidate]
                           (let [last-pair (last candidates)]
                             (if (> (last last-pair) (first next-candidate))
                               (assoc candidates (dec (count candidates)) [(min (first last-pair) (first next-candidate))
                                                                           (max (last last-pair) (last next-candidate))])
                               (conj candidates next-candidate))))
                         [(first match-indices)]
                         (rest match-indices))]
    ;; optimal-matches
    (into [] (concat [:div] (loop [pats optimal-matches
                                   parts []
                                   final []
                                   last-index 0]
                              (if (empty? pats)
                                (if (not= last-index (count candidate)) (conj final [:span.ac-highlighted-nomatch (subs candidate last-index  (count candidate))]) final)
                                (let [pair (first pats)
                                      lead (if (< last-index (first pair)) [:span.ac-highlighted-nomatch (subs candidate last-index (first pair))])
                                      highlight [:span.ac-highlighted-match (subs candidate (first pair) (last pair))]]
                                  (recur (rest pats)
                                         []
                                         (if lead
                                           (conj (conj final lead) highlight)
                                           (conj final highlight ))
                                         (last pair)))))))))

(defn get-matches [input candidates]
  (let [patterns (get-patterns input)]
    (filter #(every? (fn [[f re] p] (f (re-find re  %))) patterns) candidates)))

;; (defvar helm-map
;;   (let ((map (make-sparse-keymap)))
;;     (set-keymap-parent map minibuffer-local-map)
;;     (define-key map (kbd "<down>")     'helm-next-line)
;;     (define-key map (kbd "<up>")       'helm-previous-line)
;;     (define-key map (kbd "C-n")        'helm-next-line)
;;     (define-key map (kbd "C-p")        'helm-previous-line)
;;     (define-key map (kbd "<C-down>")   'helm-follow-action-forward)
;;     (define-key map (kbd "<C-up>")     'helm-follow-action-backward)
;;     (define-key map (kbd "<prior>")    'helm-previous-page)
;;     (define-key map (kbd "<next>")     'helm-next-page)
;;     (define-key map (kbd "M-v")        'helm-previous-page)
;;     (define-key map (kbd "C-v")        'helm-next-page)
;;     (define-key map (kbd "M-<")        'helm-beginning-of-buffer)
;;     (define-key map (kbd "M->")        'helm-end-of-buffer)
;;     (define-key map (kbd "C-g")        'helm-keyboard-quit)
;;     (define-key map (kbd "<right>")    'helm-next-source)
;;     (define-key map (kbd "<left>")     'helm-previous-source)
;;     (define-key map (kbd "<RET>")      'helm-maybe-exit-minibuffer)
;;     (define-key map (kbd "C-i")        'helm-select-action)
;;     (define-key map (kbd "C-z")        'helm-execute-persistent-action)
;;     (define-key map (kbd "C-j")        'helm-execute-persistent-action)
;;     (define-key map (kbd "C-o")        'helm-next-source)
;;     (define-key map (kbd "M-o")        'helm-previous-source)
;;     (define-key map (kbd "C-l")        'helm-recenter-top-bottom-other-window)
;;     (define-key map (kbd "M-C-l")      'helm-reposition-window-other-window)
;;     (define-key map (kbd "C-M-v")      'helm-scroll-other-window)
;;     (define-key map (kbd "M-<next>")   'helm-scroll-other-window)
;;     (define-key map (kbd "C-M-y")      'helm-scroll-other-window-down)
;;     (define-key map (kbd "C-M-S-v")    'helm-scroll-other-window-down)
;;     (define-key map (kbd "M-<prior>")  'helm-scroll-other-window-down)
;;     (define-key map (kbd "<C-M-down>") 'helm-scroll-other-window)
;;     (define-key map (kbd "<C-M-up>")   'helm-scroll-other-window-down)
;;     (define-key map (kbd "C-@")        'helm-toggle-visible-mark)
;;     (define-key map (kbd "C-SPC")      'helm-toggle-visible-mark)
;;     (define-key map (kbd "M-SPC")      'helm-toggle-visible-mark)
;;     (define-key map (kbd "M-[")        nil)
;;     (define-key map (kbd "M-(")        'helm-prev-visible-mark)
;;     (define-key map (kbd "M-)")        'helm-next-visible-mark)
;;     (define-key map (kbd "C-k")        'helm-delete-minibuffer-contents)
;;     (define-key map (kbd "C-x C-f")    'helm-quit-and-find-file)
;;     (define-key map (kbd "M-m")        'helm-toggle-all-marks)
;;     (define-key map (kbd "M-a")        'helm-mark-all)
;;     (define-key map (kbd "M-U")        'helm-unmark-all)
;;     (define-key map (kbd "C-M-a")      'helm-show-all-in-this-source-only)
;;     (define-key map (kbd "C-M-e")      'helm-display-all-sources)
;;     (define-key map (kbd "C-s")        'undefined)
;;     (define-key map (kbd "M-s")        'undefined)
;;     (define-key map (kbd "C-}")        'helm-narrow-window)
;;     (define-key map (kbd "C-{")        'helm-enlarge-window)
;;     (define-key map (kbd "C-c -")      'helm-swap-windows)
;;     (define-key map (kbd "C-c C-y")    'helm-yank-selection)
;;     (define-key map (kbd "C-c C-k")    'helm-kill-selection-and-quit)
;;     (define-key map (kbd "C-c C-i")    'helm-copy-to-buffer)
;;     (define-key map (kbd "C-c C-f")    'helm-follow-mode)
;;     (define-key map (kbd "C-c C-u")    'helm-refresh)
;;     (define-key map (kbd "C-c >")      'helm-toggle-truncate-line)
;;     (define-key map (kbd "M-p")        'previous-history-element)
;;     (define-key map (kbd "M-n")        'next-history-element)
;;     (define-key map (kbd "C-!")        'helm-toggle-suspend-update)
;;     (define-key map (kbd "C-x b")      'helm-resume-previous-session-after-quit)
;;     (define-key map (kbd "C-x C-b")    'helm-resume-list-buffers-after-quit)
;;     ;; Disable `file-cache-minibuffer-complete'.
;;     (define-key map (kbd "<C-tab>")    'undefined)
;;     ;; Multi keys
;;     (define-key map (kbd "C-t")        'helm-toggle-resplit-and-swap-windows)
;;     ;; Debugging command
;;     (define-key map (kbd "C-h C-d")    'undefined)
;;     (define-key map (kbd "C-h C-d")    'helm-enable-or-switch-to-debug)
;;     ;; Allow to eval keymap without errors.
;;     (define-key map [f1] nil)
;;     (define-key map (kbd "C-h C-h")    'undefined)
;;     (define-key map (kbd "C-h h")      'undefined)
;;     (helm-define-key-with-subkeys map
;;       (kbd "C-w") ?\C-w 'helm-yank-text-at-point
;;       '((?\C-_ . helm-undo-yank-text-at-point)))
;;     ;; Use `describe-mode' key in `global-map'.
;;     (cl-dolist (k (where-is-internal 'describe-mode global-map))
;;       (define-key map k 'helm-help))
;;     (define-key map (kbd "C-c ?")    'helm-help)
;;     ;; Bind all actions from 1 to 12 to their corresponding nth index+1.
;;     (cl-loop for n from 0 to 12 do
;;              (define-key map (kbd (format "<f%s>" (1+ n)))
;;                `(lambda ()
;;                   (interactive)
;;                   (helm-select-nth-action ,n))))
;;     map)
;;   "Keymap for helm.")


(defn search-box [ratom initial-candidates]
  (let [state (reagent/atom {:matches [] :selected 0})
        change-fn (fn [e] (let [val (.. e -target -value)
                                prev-input (get @state :previous-input "")
                                go-back? (if (or (empty? (:matches @state)) (< (count val) (count prev-input))) true false)
                                candidates (if (or (empty? (:matches @state)) (< (count val) (count prev-input)))
                                             initial-candidates
                                             (:matches @state))
                                ms (if (= val "") [] (get-matches val candidates))]
                            (.log js/console (str (.-key e) " :  " (.-keyCode e) " : " (.-metaKey e)))
                            ;;(swap! state assoc :value val)
                            (swap! state assoc :previous-input val)
                            (swap! state assoc :matches ms)))
        ;; key-fn (fn [e] (.persist e) (.log js/console (str (.-key e) " " (.-keyCode e))))
        key-fn (fn [event]
                 (letfn [(move-up [] (.log js/console "moving up")
                           (swap! state assoc :selected (dec (get @state :selected 0)))
                           (.preventDefault event)
                           (.stopPropagation event))
                         (move-down [] (.log js/console "moving down")
                           (swap! state assoc :selected (inc (get @state :selected 0)))
                           (.preventDefault event)
                           (.stopPropagation event))
                         (move-beginning [])
                         (move-end [])
                         (close [])
                         (select [])
                         (persistent-action []) ]
                   (.log js/console (str (.-key event) " " (.-keyCode event)))
                   (cond
                     (or (= "ArrowDown" (.-key event)) (and (.-ctrlKey event) (= "n" (.-key event))))
                     (move-down)
                     (or (= "ArrowUp" (.-key event)) (and (.-ctrlKey event) (= "p" (.-key event))))
                     (move-up)
                     (= "Escape" (.-key event))
                     (reset! state {:matches []})
                     )
                   (.log js/console (str (.-key event) " " (.-keyCode event)))))]
    (rf/add-data :search-state state)
    (fn []
      (let [hits (:matches @state)]
        [:div.search-container {:on-key-press key-fn}
         [:input.smooth
          {:on-change change-fn
           :content-editable true
           :on-key-down key-fn
           :tab-index 0
           :on-blur #(when(= (.-body js/document)
                             (.-activeElement js/document))
                       (reset! state {:matches []}))}]
         [:div.ac-renderer {:style {:display (if (zero? (count hits)) "none" "block")}} [:p (str "matches: " (count hits))]
          (doall (map-indexed
                  (fn [i item] ^{:key i} [:div.ac-row {:class-name (if (= i (get @state :selected 0)) "ac-highlighted")}
                                          (re-pattern-highliter (get-patterns (get @state :previous-input [])) item)
                                          #_[:div [:span "a"]
                                             [:span.ac-highlighted-match "log"]
                                             [:span " is a "]
                                             [:span.ac-highlighted-match "log"]
                                             [:span " but n"]
                                             [:span.ac-highlighted-match "o"]
                                             [:span "t "]
                                             [:span.ac-highlighted-match "log"]
                                             [:span.ac-highlighted-match "g"]]
                                          ;;item
                                          ])
                  hits))]]))))

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
  (reagent/render [search-box app-state (set my-songs)]
                  (.getElementById js/document "search"))
  (reagent/render [attribute-tree attribute-state]
                  (.getElementById js/document "attributes"))

  (reagent/render [cell] (.getElementById js/document "excel"))

  #_(reagent/render [export attribute-state] (.getElementById js/document "export"))

  #_(reagent/render [grid] (.getElementById js/document "grid"))

  #_(reagent/render [e/code-mirror code-state] (.getElementById js/document "editor")))


(defn ^:export main []
  (dev-setup)
  (reload))
