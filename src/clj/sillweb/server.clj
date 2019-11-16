;; Copyright (c) 2019 DINSIC, Bastien Guerry <bastien.guerry@data.gouv.fr>
;; SPDX-License-Identifier: EPL-2.0
;; License-Filename: LICENSES/EPL-2.0.txt

(ns sillweb.server
  (:require [ring.util.response :as response]
            [clojure.java.io :as io]
            [sillweb.config :as config]
            [sillweb.views :as views]
            [sillweb.i18n :as i]
            [org.httpkit.server :as server]
            [ring.middleware.reload :refer [wrap-reload]]
            [ring.middleware.params :as params]
            [ring.middleware.defaults :refer [site-defaults wrap-defaults]]
            [compojure.core :refer [GET POST defroutes]]
            [compojure.route :refer [not-found resources]]
            [postal.core :as postal]
            [taoensso.timbre :as timbre]
            [taoensso.timbre.appenders.core :as appenders]
            [taoensso.timbre.appenders (postal :as postal-appender)]
            [clj-http.client :as http]
            [cheshire.core :as json]
            [tea-time.core :as tt]
            [clojure.data.csv :as data-csv]
            [semantic-csv.core :as semantic-csv]
            [yaml.core :as yaml])
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

(defonce sill-url "https://raw.githubusercontent.com/DISIC/sill/master/2020/sill-2020.yaml")

(def sill-mapping {:statut   :s
                   :fonction :f
                   :licence  :l
                   :secteur  :e
                   :version  :v
                   :logiciel :i})

(def sill-rm-ks (into [] (map keyword '("parent"
                                        "linux-mimo"
                                        "version-fr"
                                        "cas-usage"
                                        "formats"
                                        "composant"
                                        "android"
                                        "mots-clefs"
                                        "win-x86-x64"))))

(defn update-sill []
  (spit "sill.json"
        (json/generate-string
         (map #(clojure.set/rename-keys
                (apply dissoc % sill-rm-ks) sill-mapping)
              (map #(into {} %)
                   (yaml/parse-string
                    (:body (http/get sill-url))
                    :keywords true)))))
  (timbre/info (str "updated sill.json")))

(defn start-tasks []
  (tt/start!)
  (def update-sill! (tt/every! 10800 update-sill))
  (timbre/info "Tasks started!"))
;; (tt/cancel! update-*!)

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
  (GET "/sill" [] (json-resource "sill.json"))

  (GET "/en/about" [] (views/en-about "en"))
  (GET "/en/contact" [] (views/contact "en"))
  (GET "/en/ok" [] (views/ok "en"))
  (GET "/fr/about" [] (views/fr-about "fr"))
  (GET "/fr/contact" [] (views/contact "fr"))
  (GET "/fr/ok" [] (views/ok "fr"))

  ;; Backward compatibility
  (GET "/contact" [] (response/redirect "/fr/contact"))
  (GET "/apropos" [] (response/redirect "/fr/about"))

  (POST "/contact" req
        (let [params (clojure.walk/keywordize-keys (:form-params req))]
          (send-email (conj params {:log (str "Sent message from " (:email params)
                                              " (" (:organization params) ")")}))
          (response/redirect (str "/" (:lang params) "/ok"))))
  
  (GET "/:lang/:page" [lang page]
       (views/default
        (if (contains? i/supported-languages lang)
          lang
          "en")))
  (GET "/:page" [page] (views/default "en"))
  (GET "/" [] (views/default "en"))
  
  (resources "/")
  (not-found "Not Found"))

(def app (-> #'routes
             (wrap-defaults site-defaults)
             params/wrap-params
             wrap-reload))

(defn -main [& args]
  (start-tasks)
  (def server (server/run-server app {:port config/sillweb_port}))
  (println (str "sillweb application started on locahost:" config/sillweb_port)))

;; (-main)

