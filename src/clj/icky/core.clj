(ns icky.core
  ;;(:import (org.jvyaml YAML))
  (:require [compojure.core :refer :all]
            [ring.middleware.resource :refer [wrap-resource]]
            [ring.middleware.file-info :refer [wrap-file-info]]
            [ring.util.response :refer [response]]
            [compojure.handler :as handler]
            [compojure.route :as route]
            [ring.middleware.logger :as logger]
            [ring.middleware.reload :as reload]
            [yaml.core :as yaml]
            [ring.middleware.params :refer :all]
            [cheshire.core :as ch]
            [yaml.reader :refer :all]
            [hiccup.middleware :refer [wrap-base-url]]
            [ring.middleware.json :refer [wrap-json-response]]
            [clojure.tools.nrepl.server :refer [start-server stop-server]]))

(def file-map
  {:master "/Users/colin/projects/mojo/app/enums/categories/category_attributes_master.yml"
   :new "/Users/colin/projects/mojo/app/enums/categories/category_attributes_new.yml"
   :sale "/Users/colin/projects/mojo/app/enums/categories/category_attributes_sale.yml"})

(defn from-yaml-file [file]
  (binding [*keywordize* true]
    (yaml/from-file  file)))

(defroutes home-routes
  (GET "/columns" [] (response (from-yaml-file (:master file-map))))
  (GET "/load-file/:file" [file] (response (from-yaml-file ((keyword file) file-map))))
  (POST "/save" request
    (let [json (-> (:params request)
                   (:data)
                   (ch/parse-string)
                   #_(yaml/generate-string))]
      (spit "attrib.json" (:data (:params request)))
      (spit "attrib.yml" (yaml/generate-string json)))
    (response {:success false
               :length "100"}))
  (GET "/tst" [] (response {:hi "there"})))

(def app
  (-> (routes home-routes)
      (wrap-params)
      (reload/wrap-reload)
      (logger/wrap-with-logger)
      (wrap-json-response)
      (handler/site)
      (wrap-base-url)))
