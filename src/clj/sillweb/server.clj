;; Copyright (c) 2019-2020 DINSIC, Bastien Guerry <bastien.guerry@data.gouv.fr>
;; SPDX-License-Identifier: EPL-2.0
;; License-Filename: LICENSES/EPL-2.0.txt

(ns sillweb.server
  (:require [ring.util.response :as response]
            [clojure.java.io :as io]
            [sillweb.config :as config]
            [sillweb.views :as views]
            [sillweb.i18n :as i]
            ;; [ring.middleware.reload :refer [wrap-reload]]
            [ring.adapter.jetty :as jetty]
            [ring.middleware.defaults :refer [site-defaults wrap-defaults]]
            [compojure.core :refer [GET POST defroutes]]
            [compojure.route :refer [not-found resources]]
            [postal.core :as postal]
            [postal.support]
            [taoensso.timbre :as timbre]
            [taoensso.timbre.appenders.core :as appenders]
            [taoensso.timbre.appenders (postal :as postal-appender)]
            [cheshire.core :as json]
            [semantic-csv.core :as semantic-csv])
  (:gen-class))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Setup logging

(timbre/set-config!
 {:level     :debug
  :output-fn (partial timbre/default-output-fn {:stacktrace-fonts {}})
  :appenders
  {:println (timbre/println-appender {:stream :auto})
   :spit    (appenders/spit-appender {:fname config/log-file})
   :postal  (postal-appender/postal-appender ;; :min-level :warn
             ^{:host config/smtp-host
               :user config/smtp-login
               :pass config/smtp-password}
             {:from config/from
              :to   config/admin-email})}})

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Download repos, orgas and stats locally

(defn get-sill-contributors []
  (json/parse-string (slurp "data/sill-contributors.json") true))

(defn json-resource [f]
  (assoc
   (response/response
    (io/input-stream f))
   :headers {"Content-Type" "application/json; charset=utf-8"}))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Setup email sending

(defn send-email
  "Send a templated email."
  [{:keys [email name organization message log]}]
  (try
    (if-let
        [res (postal/send-message
              {:host config/smtp-host
               :port 587
               :user config/smtp-login
               :pass config/smtp-password}
              {:from       config/from
               :message-id #(postal.support/message-id config/msgid-domain)
               :reply-to   email
               :to         config/admin-email
               :subject    (str name " / " organization)
               :body       message})]
      (when (= (:error res) :SUCCESS) (timbre/info log)))
    (catch Exception e
      (timbre/error (str "Can't send email: " (:cause (Throwable->map e)))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Setup routes

(defroutes routes
  (GET "/updates.xml" [] (views/rss))
  (GET "/sill" [] (json-resource "data/sill.json"))
  (GET "/:lang/about" [lang] (views/about lang))
  (GET "/:lang/contact" [lang] (views/contact lang))
  (GET "/:lang/contributors" [lang] (views/contributors lang (get-sill-contributors)))
  (GET "/:lang/ok" [lang] (views/ok lang))
  (POST "/contact" req
        (let [params (clojure.walk/keywordize-keys (:form-params req))]
          (send-email (conj params {:log (str "Sent message from " (:email params)
                                              " (" (:organization params) ")")}))
          (response/redirect (str "/" (:lang params) "/ok"))))
  (GET "/:lang/:page" [lang page]
       (views/default
        (if (contains? i/supported-languages lang)
          lang
          "fr")))
  (GET "/:page" [page] (views/default "fr"))
  (GET "/" [] (views/default "fr"))
  (resources "/")
  (not-found "Not Found"))

(def app (-> #'routes
             (wrap-defaults site-defaults)
             ;; wrap-reload
             ))

(defn -main [& args]
  (jetty/run-jetty app {:port config/sillweb_port :join? false})
  (println (str "sillweb application started on locahost:" config/sillweb_port)))

;; (-main)
