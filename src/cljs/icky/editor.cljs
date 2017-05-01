(ns icky.editor
  (:require
   [cljsjs.codemirror]
   [reagent.core :as reagent]))

(defn on-change [v] )

(defn code-mirror
  "Create a code-mirror editor. The parameters:
  value-atom (reagent atom)
    when this changes, the editor will update to reflect it.
  options
  :style (reagent style map)
    will be applied to the container element
  :js-cm-opts
    options passed into the CodeMirror constructor
  :on-cm-init (fn [cm] -> nil)
    called with the CodeMirror instance, for whatever extra fiddling you want to do."
  [value-atom {:keys [style
                      js-cm-opts
                      on-cm-init]}]
  (.log js/console style)
  (let [cm (atom nil)]
    (reagent/create-class
     {:component-did-mount
      (fn [this]
        (let [el (reagent/dom-node this)
              inst (js/CodeMirror.
                    el
                    (clj->js
                     (merge
                      {:lineNumbers false
                       :viewportMargin js/Infinity
                       :matchBrackets true
                       :autofocus true
                       ;;   :value @value-atom
                       :value "(conj [1 2 3] 4)"
                       :autoCloseBrackets true
                       :mode "clojure"}
                      js-cm-opts)))]

          #_(reset! cm inst)
          #_(.on inst "change"
               (fn []
                 (let [value (.getValue inst)]
                   (when-not (= value @value-atom)
                     (on-change value)))))
          #_(when on-cm-init
            (on-cm-init inst))
          ))

      :component-did-update
      (fn [this old-argv]
        #_(when-not (= @value-atom (.getValue @cm))
          (.setValue @cm @value-atom)
          ;; reset the cursor to the end of the text, if the text was changed externally
          (let [last-line (.lastLine @cm)
                last-ch (count (.getLine @cm last-line))]
            (.setCursor @cm last-line last-ch))))

      :reagent-render
      (fn [_ _ _]
        @value-atom
        [:div {:style style}])})))
