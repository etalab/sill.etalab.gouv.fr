;; Copyright (c) 2019-2021 DINSIC, Bastien Guerry <bastien.guerry@data.gouv.fr>
;; SPDX-License-Identifier: EPL-2.0
;; License-Filename: LICENSES/EPL-2.0.txt

(ns sillweb.config)

(def smtp-host
  (or (System/getenv "SMTP_HOST")
      "localhost"))

(def smtp-login
  (System/getenv "SMTP_LOGIN"))

(def smtp-password
  (System/getenv "SMTP_PASSWORD"))

(def msgid-domain
  (System/getenv "SILLWEB_MSGID_DOMAIN"))

(def admin-email
  (or (System/getenv "SILLWEB_ADMIN_EMAIL")
      "bastien.guerry@data.gouv.fr"))

(def from
  (or (System/getenv "SILLWEB_FROM")
      smtp-login))

(def sillweb_port
  (or (read-string (System/getenv "SILLWEB_PORT"))
      3000))

(def log-file "log.txt")
