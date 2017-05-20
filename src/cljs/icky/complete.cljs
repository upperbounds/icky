(ns icky.complete
  (:require [clojure.string :refer [replace split index-of]]
            [goog.style :refer [scrollIntoContainerView]]
            [goog.a11y.aria]
            [re-frisk.core :as rf]
            [goog.positioning :refer [positionAtAnchor flipCornerVertical OverFlow Corner]]
            [reagent.core :as reagent]))

(extend-type js/NodeList
  ISeqable
  (-seq [array] (array-seq array 0)))

(extend-type js/HTMLCollection
  ISeqable
  (-seq [array] (array-seq array 0)))

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

(defn complete-candidates [state]
  (let [hits (get @state :matches [])]
    [:div.ac-render-container {:style {:display (if (or (not(get @state :open?)) (zero? (count hits))) "none" "block")}} [:p (str "matches: " (count hits))]
     [:div.ac-renderer
      (doall (map-indexed
              (fn [i item]
                ^{:key i} [:div.ac-row {:class-name (if (= i (get @state :selected 0)) "ac-highlighted")}
                           (re-pattern-highliter (get-patterns (get @state :previous-input [])) item)
                           ])
              (take 25 hits)))]]))

(defn prevent-and-stop-event [e f]
  (f e)
  (.preventDefault e)
  (.stopPropagation e))

(defn search-box [initial-candidates]
  (let [state (reagent/atom {:matches [] :selected 0 :open?  false})
        change-fn (fn [e] (let [val (.. e -target -value)
                                prev-input (get @state :previous-input "")
                                go-back? (if (or (empty? (:matches @state)) (< (count val) (count prev-input))) true false)
                                candidates (if (or (empty? (:matches @state)) (< (count val) (count prev-input)))
                                             initial-candidates
                                             (:matches @state))
                                ms (if (= val "") [] (get-matches val candidates))]
                            (.log js/console prev-input)
                            (swap! state merge {:previous-input val :matches ms :selected 0 :open? true})))
        key-fn (fn [event]
                 (.log js/console (.-key event) (.-keyCode event) )
                 (letfn [(scroll-selection! []
                           (let [containing-node (first (array-seq (.getElementsByClassName (reagent/dom-node (:renderer @state)) "ac-renderer")))
                                 selection-nodes (array-seq (.getElementsByClassName containing-node  "ac-row"))]
                             (scrollIntoContainerView (nth selection-nodes (:selected @state))  containing-node)))
                         (move-up! [event]
                           (when-not (zero? (get @state :selected 0))
                             (swap! state assoc :selected (dec (get @state :selected 0))))
                           (scroll-selection!))
                         (move-down! [event]
                           (when (< (get @state :selected 0) (dec (count (get @state :matches))))
                               (swap! state assoc :selected (inc (get @state :selected 0)))
                             (scroll-selection!)))
                         (move-first! []
                           (swap! state assoc :selected 0)
                           (scroll-selection!))
                         (move-last! []
                           (swap! state assoc :selected (dec (count (get @state :matches))))
                           (scroll-selection!))
                         ;; TODO
                         (move-page-up! [] (print "page up"))
                         ;; TODO
                         (move-page-down! [] (print "page down"))
                         (close [])
                         (select [])
                         (persistent-action! [] (.log js/console "persistant action"))
                         (mark! []
                           (.log js/console "marked"))]
                   (cond
                     (or (= "ArrowDown" (.-key event)) (and (.-ctrlKey event) (= "n" (.-key event))))
                     (prevent-and-stop-event event move-down!)

                     (or (= "ArrowUp" (.-key event)) (and (.-ctrlKey event) (= "p" (.-key event))))
                     (prevent-and-stop-event event move-up!)

                     (= "Escape" (.-key event))
                     (prevent-and-stop-event event #(swap! state assoc :open? false))

                     (=(.-keyCode event) 190)
                     (prevent-and-stop-event event move-last!)

                     (=(.-keyCode event) 188)
                     (prevent-and-stop-event event move-first!)

                     (and (.-ctrlKey event) (=(.-key event) "v"))
                     (move-page-down!)

                     (=(.-keyCode event) 86)
                     (move-page-up!)

                     (and (.-ctrlKey event) (= (.-key event) " "))
                     (mark!)

                     (= "Enter" (.-key event))
                     (do
                       (swap! state merge {:open? false :previous-input (nth (:matches @state) (:selected @state))})
                       )

                     #_(and (=(.-ctlKey event) (or (= (.-key event) "j") (= (.-key event) "z"))))
                     (=(.-keyCode event) 74)
                     (persistent-action!))))]
    (rf/add-data :search-state state)
    (reagent/create-class {:component-did-mount
                           (fn [_]
                             (let [renderer (if (:renderer @state)
                                              (:renderer @state)
                                              (swap! state assoc :renderer (.createElement js/document "div")))
                                   elem (:renderer renderer)
                                   dn (reagent/dom-node _)]
                               (set! (.-className elem) "ac-renderer-container")
                               (.appendChild (.-body js/document) elem)
                               (reagent/render [complete-candidates state] elem)
                               (.log js/console (positionAtAnchor dn
                                                                 (.-BOTTOM_LEFT Corner)
                                                                 elem
                                                                 (flipCornerVertical (.-BOTTOM_LEFT Corner))
                                                                 nil
                                                                 nil
                                                                 ;;                  (.-ADJUST_X_EXCEPT_OFFSCREEN Overflow)
                                                                 65))))
                           :component-will-unmount
                           (fn [_]
                             (let [elem (:renderer @state)]
                               (reagent/unmount-component-at-node elem)
                               (.remove elem)))
                           :render (fn [i]
                                     (let [hits (:matches @state)]
                                       [:div.search-container {:on-key-press key-fn}
                                        [:input.smooth
                                         {:on-change change-fn
                                          :content-editable true
                                          :value (get @state :previous-input "")
                                          :on-key-down key-fn
                                          :tab-index 0
                                          :on-blur #(when(= (.-body js/document)
                                                            (.-activeElement js/document))
                                                      (swap! state assoc :matches []))}]
                                        ]))})))

;; The default things that helm does when open with selections

;; (defvar helm-map
;;   (let ((map (make-sparse-keymap)))
;;     (set-keymap-parent map minibuffer-local-map)
;;     (define-key map (kbd "<down>")     'helm-next-line)
;;     (define-key map (kbd "<up>")       'helm-previous-line)
;;     (define-key map (kbd "C-n")        'helm-next-line)
;;     (define-key map (kbd "C-p")        'helm-previous-line)
;;     (define-key map (kbd "<C-down>")   'helm-follow-action-forward) ;; TODO
;;     (define-key map (kbd "<C-up>")     'helm-follow-action-backward) ;; TODO
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
