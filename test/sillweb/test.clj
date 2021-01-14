;; Copyright (c) 2019-2021 DINSIC, Bastien Guerry <bastien.guerry@data.gouv.fr>
;; SPDX-License-Identifier: EPL-2.0
;; License-Filename: LICENSES/EPL-2.0.txt

(ns sillweb.test
  (:require [clojure.test :refer :all]
            [sillweb.config :as config]))

(deftest test-environment-variables
  (testing "Checking if all environment variables contain strings."
    (is (and (string? (System/getenv "SMTP_HOST"))
             (string? (System/getenv "SMTP_LOGIN"))
             (string? (System/getenv "SMTP_PASSWORD"))
             (string? (System/getenv "SILLWEB_ADMIN_EMAIL"))
             (string? (System/getenv "SILLWEB_FROM"))
             (string? (System/getenv "SILLWEB_PORT"))))))

