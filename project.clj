;; Copyright (c) 2019 DINSIC, Bastien Guerry <bastien.guerry@data.gouv.fr>
;; SPDX-License-Identifier: EPL-2.0
;; License-Filename: LICENSES/EPL-2.0.txt

(defproject sillweb "0.2.3"
  :description "Frontend to display public sector source code repositories"
  :url "https://github.com/etalab/sillweb"
  :license {:name "Eclipse Public License - v 2.0"
            :url  "http://www.eclipse.org/legal/epl-v20.html"}
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [compojure "1.6.1"]
                 [http-kit "2.3.0"]
                 [clj-http "3.10.0"]
                 [ring/ring-core "1.8.0"]
                 [ring/ring-devel "1.8.0"]
                 [ring/ring-defaults "0.3.2"]
                 [cheshire "5.9.0"]
                 [com.draines/postal "2.0.3"]
                 [com.taoensso/timbre "4.10.0"]
                 [markdown-to-hiccup "0.6.2"]
                 ;; FIXME: this explicit require should not be needed:
                 [org.clojure/tools.reader "1.3.2"]
                 [tea-time "1.0.1"]
                 [clj-http "3.10.0"]
                 [cheshire "5.9.0"]
                 [com.taoensso/tempura "1.2.1"]
                 [org.clojure/data.csv "0.1.4"]
                 [semantic-csv "0.2.0"]
                 [org.flatland/ordered "1.5.7"]
                 [hickory "0.7.1"]]
  :source-paths ["src/clj" "src/cljc" "src/cljs"]
  :main sillweb.server
  :uberjar-name "sillweb-standalone.jar"
  :auto-clean false
  :clean-targets ^{:protect false} ["target" "resources/public/js/dev/"
                                    "resources/public/js/sillweb.js"]
  :aliases {"fig"       ["trampoline" "run" "-m" "figwheel.main"]
            "fig:build" ["trampoline" "run" "-m" "figwheel.main" "-b" "dev" "-r"]
            "fig:min"   ["run" "-m" "figwheel.main" "-O" "advanced" "-bo" "dev"]}
  :profiles {:uberjar {:aot :all}
             :dev     {:source-paths ["src/cljc" "src/cljs"]
                       :dependencies [[cljs-ajax "0.8.0"]
                                      [cljs-bean "1.5.0"]
                                      [com.bhauman/figwheel-main "0.2.3"]
                                      [com.bhauman/rebel-readline-cljs "0.1.4"]
                                      [markdown-to-hiccup "0.6.2"]
                                      [org.clojure/clojurescript "1.10.597"]
                                      [org.clojure/core.async "0.6.532"]
                                      [re-frame "0.10.9"]
                                      [reagent "0.8.1"]
                                      [reagent-utils "0.3.3"]
                                      [metosin/reitit-frontend "0.3.10"]]}})
