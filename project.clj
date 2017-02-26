(defproject icky "0.1.0-SNAPSHOT"
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/clojurescript "1.9.494"]
                 [reagent "0.6.0"]
                 [binaryage/devtools "0.9.1"]
                 [garden "1.3.2"]
                 [frak "0.1.6"]
                 [re-frisk "0.3.2"]]

  :min-lein-version "2.5.3"

  :source-paths ["src/clj"]

  :plugins [[lein-cljsbuild "1.1.4"]
            [lein-garden "0.2.8"]]

  :clean-targets ^{:protect false} ["resources/public/js/compiled"
                                    "target"
                                    "resources/public/css"]

  :figwheel {:css-dirs ["resources/public/css"]}

  :garden {:builds [{:id           "screen"
                     :source-paths ["src/clj"]
                     :stylesheet   icky.css/screen
                     :compiler     {:output-to     "resources/public/css/screen.css"
                                    :pretty-print? true}}]}
  :repl-options {:nrepl-middleware [cemerick.piggieback/wrap-cljs-repl]}

  :profiles
  {:dev
   {:dependencies [[figwheel-sidecar "0.5.9"]
                   [com.cemerick/piggieback "0.2.1"]]

    :plugins      [[lein-figwheel "0.5.9"]
                   [cider/cider-nrepl "0.13.0"]
                   [lein-garden "0.3.0"]]}}

  :cljsbuild
  {:builds
   [{:id           "dev"
     :source-paths ["src/cljs"]
     :figwheel     {:on-jsload "icky.core/reload"}
     :compiler     {:main                 icky.core
                    :optimizations        :none
                    :output-to            "resources/public/js/compiled/app.js"
                    :output-dir           "resources/public/js/compiled/dev"
                    :asset-path           "js/compiled/dev"
                    :source-map-timestamp true}}

    {:id           "min"
     :source-paths ["src/cljs"]
     :compiler     {:main            icky.core
                    :optimizations   :advanced
                    :output-to       "resources/public/js/compiled/app2.js"
                    :output-dir      "resources/public/js/compiled/min"
                    :closure-defines {goog.DEBUG false}
                    :pretty-print    false}}]})
