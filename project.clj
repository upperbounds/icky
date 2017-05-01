(defproject icky "0.1.0-SNAPSHOT"
  :dependencies [[org.clojure/clojure "1.8.0"]
                 ;; [org.clojure/clojure "1.9.0-alpha15"]
                 [org.clojure/clojurescript "1.9.521"]
                 [reagent "0.6.1"]
                 [binaryage/devtools "0.9.4"]
                 [binaryage/dirac "1.2.5"]
                 [org.clojure/core.async "0.3.442"]
                 [compojure "1.5.2"]
                 [ring-server "0.4.0"]
                 [hiccup "1.0.5"]
                 [io.forward/yaml "1.0.6"]
                 [cheshire "5.7.1"]
                 [ring.middleware.logger "0.5.0"]
                 [ring/ring-json "0.4.0"]
                 [garden "1.3.2"]
                 [cljsjs/codemirror "5.24.0-1"]
                 ;;[jvyaml/jvyaml "1.0.0"]
                 [frak "0.1.6"]
                 [re-frisk "0.4.5"]]
  :min-lein-version "2.5.3"
  :source-paths ["src/clj"]
  :plugins [[lein-cljsbuild "1.1.4"]
            [lein-garden "0.3.0"]]
  :clean-targets ^{:protect false} ["resources/public/js/compiled"
                                    "target"
                                    "resources/public/css"]
  :figwheel {:css-dirs ["resources/public/css"]
             ;; Start an nREPL server into the running figwheel process
             :nrepl-port 7888
             :ring-handler icky.core/app}
  :garden {:builds [{:id           "screen"
                     :source-paths ["src/clj"]
                     :stylesheet   icky.css/screen
                     :compiler     {:output-to     "resources/public/css/screen.css"
                                    :pretty-print? true}}]}
  :repl-options {:nrepl-middleware [cemerick.piggieback/wrap-cljs-repl]}
  :profiles
  {:dev
   {:dependencies [[figwheel-sidecar "0.5.10"]
                   [com.cemerick/piggieback "0.2.1"]]
    :plugins      [[lein-figwheel "0.5.10"]
                   [cider/cider-nrepl "0.14.0"]
                   [lein-garden "0.3.0"]]}
   :repl
   {:repl-options {:port             8230
                   :nrepl-middleware [dirac.nrepl/middleware]
                   :init             (do
                                       (require 'dirac.agent)
                                       (dirac.agent/boot!))}}}
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
