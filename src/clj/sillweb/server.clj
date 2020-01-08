;; Copyright (c) 2019 DINSIC, Bastien Guerry <bastien.guerry@data.gouv.fr>
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
            [ring.util.codec :as codec]
            [ring.middleware.defaults :refer [site-defaults wrap-defaults]]
            [compojure.core :refer [GET POST defroutes]]
            [compojure.route :refer [not-found resources]]
            [postal.core :as postal]
            [postal.support]
            [taoensso.timbre :as timbre]
            [taoensso.timbre.appenders.core :as appenders]
            [taoensso.timbre.appenders (postal :as postal-appender)]
            [clj-http.client :as http]
            [cheshire.core :as json]
            [tea-time.core :as tt]
            [clojure.data.csv :as data-csv]
            [semantic-csv.core :as semantic-csv]
            [hickory.core :as h]
            [hickory.zip :as hz]
            [hickory.select :as hs]
            [clojure.string :as s]
            [clojure.set :as clset])
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

(def commons-base-url "https://www.wikidata.org/wiki/Special:EntityData/")
(def commons-base-image-url "https://commons.wikimedia.org/wiki/File:")

(def sill-url "https://raw.githubusercontent.com/DISIC/sill/master/2020/sill-2020.csv")

;; Keywords to ignore
;; "parent"
;; "cas-usage"
;; "formats"
;; "composant"
;; "mots-clefs"
(def sill-mapping {:statut    :s
                   :fonction  :f
                   :licence   :l
                   :ID        :id
                   :secteur   :se
                   :composant :c
                   :version   :v
                   :wikidata  :w
                   :nom       :i
                   :groupe    :g})

(def http-get-params {:cookie-policy :standard})

(defn get-sill []
  (map #(clset/rename-keys
         (select-keys % (keys sill-mapping))
         sill-mapping)
       (try (semantic-csv/slurp-csv sill-url)
            (catch Exception e
              (timbre/error "Cannot reach SILL csv URL")))))

(defn wd-get-data [entity]
  (when (not-empty entity)
    (-> (try (http/get (str commons-base-url entity ".json")
                       http-get-params)
             (catch Exception e
               (timbre/error "Cannot reach Wikidata url")))
        :body
        (json/parse-string true)
        :entities
        (as-> e ((keyword entity) e)) ;; kentity
        (select-keys [:claims :descriptions]))))

(defn wc-get-image-url-from-wm-filename [f]
  (if-let [src (try (:body (http/get
                            (str commons-base-image-url
                                 (codec/url-encode f "UTF-8"))
                            http-get-params))
                    (catch Exception e
                      (timbre/error
                       (str "Can't reach image url for " f))))]
    (let [metas (-> src h/parse h/as-hickory
                    (as-> s (hs/select (hs/tag "meta") s)))]
      (->> metas
           (map :attrs)
           (filter #(= (:property %) "og:image"))
           first
           :content))))

(defn wd-get-first-value [k claims]
  (:value (:datavalue (:mainsnak (first (k claims))))))

;; Other properties to consider:
;; - P178: developer
;; - P275: license
;; - P18: image
;; - P306: operating system (linux Q388, macosx Q14116, windows Q1406)

(defn sill-plus-wikidata []
  (for [entry (get-sill)]
    (-> (if-let   [data (wd-get-data (:w entry))]
          (let [claims     (:claims data)
                descs      (:descriptions data)
                logo-claim (wd-get-first-value :P154 claims)
                frama      (wd-get-first-value :P4107 claims)]
            (merge entry
                   {:logo    (when (not-empty logo-claim)
                               (wc-get-image-url-from-wm-filename logo-claim))
                    :website (wd-get-first-value :P856 claims)
                    :sources (wd-get-first-value :P1324 claims)
                    :doc     (wd-get-first-value :P2078 claims)
                    :frama   {:encoded-name (codec/form-encode frama "UTF-8")
                              :name         frama}
                    :fr-desc (if-let [d (:value (:fr descs))] (s/capitalize d))
                    :en-desc (if-let [d (:value (:en descs))] (s/capitalize d))
                    }))
          entry)
        (dissoc :w))))

(defn sill-to-json []
  (when-let [sill-full (sill-plus-wikidata)]
    (spit "data/sill.json"
          (json/generate-string sill-full))
    (timbre/info "Updated sill.json")))

(defn start-tasks []
  (tt/start!)
  (tt/every! 10800 sill-to-json)
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
  (GET "/sill" [] (json-resource "data/sill.json"))
  (GET "/en/about" [] (views/en-about "en"))
  (GET "/en/contact" [] (views/contact "en"))
  (GET "/en/ok" [] (views/ok "en"))
  (GET "/fr/about" [] (views/fr-about "fr"))
  (GET "/fr/contact" [] (views/contact "fr"))
  (GET "/fr/ok" [] (views/ok "fr"))
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
             ;; wrap-reload
             ))

(defn -main [& args]
  (start-tasks)
  (jetty/run-jetty app {:port config/sillweb_port})
  (println (str "sillweb application started on locahost:" config/sillweb_port)))

;; (-main)
