(ns icky.dev
  (:require [devtools.core :as devtools]
            [re-frisk.core :as rf]))
(enable-console-print!)

(defonce debug?
  ^boolean js/goog.DEBUG)

(defn dev-setup []
  (when debug?
    (enable-console-print!)
    (rf/enable-frisk!)
    #_(rf/add-data :app-state app-state)
    ;; (rf/add-data :attribute-state attribute-state)
    (dirac.runtime/install!)
    (devtools/install!)))
