;; Copyright (c) 2019-2021 DINSIC, Bastien Guerry <bastien.guerry@data.gouv.fr>
;; SPDX-License-Identifier: EPL-2.0
;; License-Filename: LICENSES/EPL-2.0.txt

(ns sillweb.core
  (:require [cljs.core.async :as async]
            [goog.labs.format.csv :as csv]
            [re-frame.core :as re-frame]
            [reagent.core :as reagent]
            [reagent.dom]
            [ajax.core :refer [GET]]
            [sillweb.i18n :as i]
            [clojure.string :as s]
            [clojure.walk :as walk]
            [reitit.frontend :as rf]
            [reitit.frontend.easy :as rfe]))

(defonce dev? false)
(defonce sws-per-page 50)
(defonce minimum-search-string-size 3)
(defonce timeout 100)
(defonce init-filter {:q "" :id "" :group "" :year "2021"})
(defonce frama-base-url "https://framalibre.org/content/")
(defonce comptoir-base-url "https://comptoir-du-libre.org/fr/softwares/")
(defonce sill-csv-url "https://raw.githubusercontent.com/DISIC/sill/master/2020/sill-2020.csv")

(defn rows->maps [csv]
  (let [headers (map keyword (first csv))
        rows    (rest csv)]
    (map #(zipmap headers %) rows)))

(re-frame/reg-event-db
 :initialize-db!
 (fn [_ _]
   {:sws-page       0
    :sws            nil
    :sort-sws-by    :name
    :view           :sws
    :reverse-sort   false
    :only-support   false
    :only-public    false
    :filter         init-filter
    :display-filter init-filter
    :lang           "en"}))

(re-frame/reg-event-db
 :lang!
 (fn [db [_ lang]]
   (assoc db :lang lang)))

(re-frame/reg-sub
 :lang?
 (fn [db _] (:lang db)))

(re-frame/reg-event-db
 :filter!
 (fn [db [_ s]]
   (re-frame/dispatch [:sws-page! 0])
   ;; FIXME: Find a more idiomatic way?
   (assoc db :filter (merge (:filter db) s))))

(re-frame/reg-event-db
 :display-filter!
 (fn [db [_ s]]
   (assoc db :display-filter (merge (:display-filter db) s))))

(re-frame/reg-event-db
 :sws-page!
 (fn [db [_ n]] (assoc db :sws-page n)))

(re-frame/reg-event-db
 :view!
 (fn [db [_ view query-params]]
   (re-frame/dispatch [:sws-page! 0])
   (re-frame/dispatch [:filter! (merge init-filter query-params)])
   (re-frame/dispatch [:display-filter! (merge init-filter query-params)])
   (assoc db :view view)))

(re-frame/reg-event-db
 :update-sws!
 (fn [db [_ sws]] (when sws (assoc db :sws sws))))

(re-frame/reg-sub
 :papillon?
 (fn [db _] (:papillon db)))

(re-frame/reg-event-db
 :update-papillon!
 (fn [db [_ papillon]] (when papillon (assoc db :papillon papillon))))

(re-frame/reg-sub
 :sort-sws-by?
 (fn [db _] (:sort-sws-by db)))

(re-frame/reg-sub
 :sws-page?
 (fn [db _] (:sws-page db)))

(re-frame/reg-sub
 :filter?
 (fn [db _] (:filter db)))

(re-frame/reg-sub
 :display-filter?
 (fn [db _] (:display-filter db)))

(re-frame/reg-sub
 :view?
 (fn [db _] (:view db)))

(re-frame/reg-sub
 :only-support?
 (fn [db _] (:only-support db)))

(re-frame/reg-event-db
 :only-support!
 (fn [db _] (assoc db :only-support (not (:only-support db)))))

(re-frame/reg-sub
 :only-public?
 (fn [db _] (:only-public db)))

(re-frame/reg-event-db
 :only-public!
 (fn [db _] (assoc db :only-public (not (:only-public db)))))

(re-frame/reg-sub
 :reverse-sort?
 (fn [db _] (:reverse-sort db)))

(re-frame/reg-event-db
 :reverse-sort!
 (fn [db _] (assoc db :reverse-sort (not (:reverse-sort db)))))

(re-frame/reg-event-db
 :sort-sws-by!
 (fn [db [_ k]]
   (re-frame/dispatch [:sws-page! 0])
   (when (= k (:sort-sws-by db))
     (re-frame/dispatch [:reverse-sort!]))
   (assoc db :sort-sws-by k)))

(defn or-kwds [m ks]
  (first (remove nil? (map #(apply % [m]) ks))))

(defn fa [s]
  [:span.icon [:i {:class (str "fas " s)}]])

(defn fab [s]
  [:span.icon [:i {:class (str "fab " s)}]])

(defn s-includes? [s sub]
  (when (and (string? s) (string? sub))
    (s/includes? (s/lower-case s) (s/lower-case sub))))

(defn apply-sws-filters [m]
  (let [f  @(re-frame/subscribe [:filter?])
        su @(re-frame/subscribe [:only-support?])
        pu @(re-frame/subscribe [:only-public?])
        s  (s/replace (s/trim (:q f)) #"\s+" " ")
        i  (:id f)
        g  (:group f)
        y  (:year f)]
    (filter
     #(and (if (not-empty i) (= i (:id %)) true)
           (if s (s-includes?
                  (s/join "" [(:i %) (:fr-desc %) (:en-desc %) (:f %)
                              (:u %) (:p %) (:a %)]) s)
               true)
           (if-not su true (= "Oui" (:su %)))
           (if-not pu true (= "Oui" (:a %)))
           (if-not (not-empty i) (s-includes? (:y %) y) true)
           (if (and (not-empty g)
                    (not (= g "")))
             (= g (:g %)) true))
     m)))

(def filter-chan (async/chan))
(def display-filter-chan (async/chan))

(defn start-display-filter-loop []
  (async/go
    (loop [f (async/<! display-filter-chan)]
      (re-frame/dispatch [:display-filter! f])
      (recur (async/<! display-filter-chan)))))

(defn start-filter-loop []
  (async/go
    (loop [f (async/<! filter-chan)]
      (let [v    @(re-frame/subscribe [:view?])
            l    @(re-frame/subscribe [:lang?])
            fs   @(re-frame/subscribe [:filter?])
            n-f0 (filter #(let [s (val %)]
                            (and (string? s) (not-empty s)))
                         (merge fs f))
            n-f  (filter #(not (= (first %) :id)) n-f0)]
        (rfe/push-state v {:lang l} n-f))
      (re-frame/dispatch [:filter! f])
      (recur (async/<! filter-chan)))))

(re-frame/reg-sub
 :sws-nofilter?
 (fn [db _]
   (let [sws (:sws db)
         sws (case @(re-frame/subscribe [:sort-sws-by?])
               :name (sort #(compare (or-kwds %1 [:i])
                                     (or-kwds %2 [:i]))
                           sws)
               sws)]
     sws)))

(re-frame/reg-sub
 :sws?
 (fn [db _]
   (let [sws (:sws db)
         sws (case @(re-frame/subscribe [:sort-sws-by?])
               :name (sort #(compare (or-kwds %1 [:i])
                                     (or-kwds %2 [:i]))
                           sws)
               sws)]
     (apply-sws-filters (if @(re-frame/subscribe [:reverse-sort?])
                          (reverse sws)
                          sws)))))

(defn sill-page [lang sws sws-pages]
  (let [papillon @(re-frame/subscribe [:papillon?])]
    (into
     [:div.tile.is-ancestor.is-vertical]
     (for [dd (partition-all
               2
               (take sws-per-page
                     (drop (* sws-per-page sws-pages) sws)))]
       ^{:key dd}
       [:div.tile.is-parent.is-horizontal
        (for [{:keys [;; See https://github.com/etalab/sill-data
                      s f l id u v i co a p su
                      logo fr-desc en-desc website doc sources frama]
               :as   o}
              dd]
          ^{:key o}
          [:div.tile.is-parent
           [:div.tile.is-child.card
            {:class (when (= (count sws) 1) "is-6")} ;; FIXME: perfs?
            [:div.card-content.fixed-bottom
             [:div.media
              [:div.media-content
               [:p.is-size-4
                (if website
                  [:a {:href   website
                       :target "new"
                       :title  (i/i lang [:go-to-website])}
                   (when (not-empty p) (str p ": ")) i]
                  [:span (when (not-empty p) (str p ": ")) i])

                (when (= a "Oui")
                  [:span " " [:img {:src   "/images/marianne.png"
                                    :title (i/i lang [:public])
                                    :width "30px"}]])

                (when (= s "O")
                  [:sup.is-size-7.has-text-grey
                   {:title (i/i lang [:warning-testing])}
                   (fa "fa-vial")])]]
              (when (not-empty logo)
                [:div.media-right
                 [:figure.image.is-64x64
                  [:img {:src logo}]]])]
             [:div.content
              [:p (cond (= lang "fr") fr-desc
                        (= lang "en") en-desc)]
              (when (not-empty f)
                [:div [:b (i/i lang [:function]) " "]
                 (i/md-to-string f)
                 [:br]])
              (when (not-empty u)
                [:div [:b (i/i lang [:context-of-use]) " "]
                 (i/md-to-string u)
                 [:br]])
              (when-let [used (->> (filter #(= id (:software_sill_id %)) papillon)
                                   (remove #(= "" %))
                                   seq)]
                [:div
                 [:span [:b (i/i lang [:instances])]
                  [:sup
                   [:a.has-text-grey.is-size-7
                    {:on-click #(rfe/push-state :papillon {:lang lang})
                     :title    "PAPILLON"}
                    (fa "fa-question-circle")]]
                  ": "
                  (for [{:keys [service_name service_url agency_name]
                         :as   u} used]
                    ^{:key u}
                    [:span [:a {:href   service_url
                                :target "new"
                                :title  (str service_name " (" agency_name ")")}
                            (str service_name " · ")]])]
                 [:br]
                 [:br]])
              (when-let [lic (not-empty l)]
                (let [lics (s/split l #", ")]
                  [:div [:p [:strong
                             (i/i lang (if (> (count lics) 1)
                                         [:licenses] [:license]))
                             (if (= lang "fr") " : " ": ")]
                         (for [ll lics]
                           ^{:key ll}
                           [:span
                            [:a {:href   (str "https://spdx.org/licenses/" ll ".html")
                                 :title  (i/i lang [:license-title])
                                 :target "new"}
                             ll]
                            " "])]
                   [:br]]))]]
            [:div.card-footer
             [:div.card-footer-item
              [:a {:title    (i/i lang [:permalink])
                   :on-click #(rfe/push-state :sws {:lang lang} {:id id})} (fa "fa-link")]]
             (when sources
               [:div.card-footer-item
                [:a {:href   sources
                     :target "new"
                     :title  (i/i lang [:go-to-source])}
                 (fa "fa-code")]])
             (when doc
               [:div.card-footer-item
                [:a {:href   doc
                     :target "new"
                     :title  (i/i lang [:read-the-docs])}
                 (fa "fa-book")]])

             (when-let [c (not-empty co)]
               [:div.card-footer-item
                [:a {:href   (str comptoir-base-url c)
                     :title  (i/i lang [:on-comptoir])
                     :target "new"}
                 [:figure.image.is-32x32
                  [:img {:src "/images/adu.png"}]]]])
             (when-let [n (not-empty (:encoded-name frama))]
               [:div.card-footer-item
                [:a {:href   (str frama-base-url n)
                     :title  (str (i/i lang [:on-framalibre])
                                  (:name frama))
                     :target "new"}
                 [:figure.image.is-32x32
                  [:img {:src "/images/frama.png"}]]]])
             (when (and (= lang "fr") (= su "Oui"))
               [:div.card-footer-item
                [:a {:href  (str "/" lang "/about#support")
                     :title (i/i lang [:supported])}
                 (fa "fa-hands-helping")]])
             (when (not-empty v)
               [:div.card-footer-item
                [:p {:title (i/i lang [:recommended_version])}
                 (str (i/i lang [:version]) v)]])]]])]))))

(defn change-sws-page [next]
  (let [sws-page    @(re-frame/subscribe [:sws-page?])
        count-pages (count (partition-all
                            sws-per-page @(re-frame/subscribe [:sws?])))]
    (cond
      (= next "first")
      (re-frame/dispatch [:sws-page! 0])
      (= next "last")
      (re-frame/dispatch [:sws-page! (dec count-pages)])
      (and (< sws-page (dec count-pages)) next)
      (re-frame/dispatch [:sws-page! (inc sws-page)])
      (and (> sws-page 0) (not next))
      (re-frame/dispatch [:sws-page! (dec sws-page)]))))

(defn stats-card [lang heading data & [thead]]
  [:div.column
   [:div.card
    [:h1.card-header-title.subtitle heading]
    [:div.card-content
     [:table.table.is-fullwidth
      thead
      [:tbody
       (for [[a b] (sort (fn [[_ a] [_ b]] (compare b a)) data)]
         ^{:key [a b]}
         [:tr [:td (or (not-empty a) (i/i lang [:unspecified]))] [:td b]])]]]]])

(defn stats-page [stats]
  (let [lang @(re-frame/subscribe [:lang?])]
    [:div
     [:div.columns
      (stats-card
       lang
       (str (i/i lang [:years-count]) " (" (i/i lang [:recommended]) ")")
       (:years stats)
       [:thead [:tr
                [:th (i/i lang [:year])]
                [:th (i/i lang [:count])]]])
      [:div.column
       [:a {:href "https://etalab.github.io/sill-data/sill-years.svg"}
        [:img {:src "https://etalab.github.io/sill-data/sill-years.svg"}]]]]
     [:div.columns
      (stats-card
       lang
       (str (i/i lang [:groups-count]) " (2021)")
       (:group stats)
       [:thead [:tr
                [:th (i/i lang [:group])]
                [:th (i/i lang [:count])]]])]
     [:div.columns
      [:div.column
       [:a {:href "https://etalab.github.io/sill-data/sill-licenses.svg"}
        [:img {:src "https://etalab.github.io/sill-data/sill-licenses.svg"}]]]]
     [:div.columns
      (stats-card
       lang
       (str (i/i lang [:licenses-count]) " (2021)")
       (:licenses stats)
       [:thead [:tr
                [:th (i/i lang [:license])]
                [:th (i/i lang [:count])]]])]]))

(defn stats-class []
  (let [stats (reagent/atom nil)]
    (reagent/create-class
     {:component-did-mount
      (fn []
        (GET "https://etalab.github.io/sill-data/sill-stats.json"
             :handler #(reset! stats (walk/keywordize-keys %))))
      :reagent-render (fn [] (stats-page @stats))})))

(defn papillon-page []
  (let [lang     @(re-frame/subscribe [:lang?])
        papillon @(re-frame/subscribe [:papillon?])]
    [:div
     [:h1 "PAPILLON"]
     [:br]
     [:p (i/md-to-string (i/i lang [:papillon]))]
     [:br]
     [:table.table.is-fullwidth
      [:thead
       [:tr
        [:th (i/i lang [:name])]
        [:th (i/i lang [:freesoftware])]
        [:th (i/i lang [:agency])]
        [:th (i/i lang [:users])]
        [:th (i/i lang [:subscription])]]]
      [:tbody
       (for [p (sort-by :service_name papillon)]
         ^{:key p}
         (when (not-empty (:service_url p))
           [:tr
            [:td [:a {:target "new"
                      :title  (:description p)
                      :href   (:service_url p)}
                  (:service_name p)]]
            [:td (if-let [id (not-empty (:software_sill_id p))]
                   [:a {:on-click #(do (rfe/push-state :sws {:lang lang} {:id id})
                                       ;; FIXME: workaround to scroll to top
                                       (js/window.scrollTo 0 0))}
                    (:software_name p)]
                   (:software_name p))]
            [:td [:a {:target "new"
                      :href   (:agency_url p)} (:agency_name p)]]
            [:td (:usage_scope p)]
            [:td (:signup_scope p)]]))]]]))

(defn navbar [first-disabled last-disabled]
  [:nav.level-item
   {:role "navigation" :aria-label "pagination"}
   [:a.pagination-previous
    {:on-click #(change-sws-page "first")
     :disabled first-disabled}
    (fa "fa-fast-backward")]
   [:a.pagination-previous
    {:on-click #(change-sws-page nil)
     :disabled first-disabled}
    (fa "fa-step-backward")]
   [:a.pagination-next
    {:on-click #(change-sws-page true)
     :disabled last-disabled}
    (fa "fa-step-forward")]
   [:a.pagination-next
    {:on-click #(change-sws-page "last")
     :disabled last-disabled}
    (fa "fa-fast-forward")]])

(defn main-page [q]
  (let [lang @(re-frame/subscribe [:lang?])
        flt  @(re-frame/subscribe [:filter?])
        view @(re-frame/subscribe [:view?])]
    [:div
     (condp = view

       :home-redirect
       (if dev?
         [:p "Testing."]
         (if (contains? i/supported-languages lang)
           (do (set! (.-location js/window) (str "/" lang "/software")) "")
           (do (set! (.-location js/window) (str "/en/software")) "")))

       :papillon
       [papillon-page]

       :stats
       [stats-class]

       :sws
       (let [org-f          @(re-frame/subscribe [:sort-sws-by?])
             sws            @(re-frame/subscribe [:sws?])
             count-sws      (count sws)
             sws-pages      @(re-frame/subscribe [:sws-page?])
             count-pages    (count (partition-all sws-per-page sws))
             first-disabled (= sws-pages 0)
             last-disabled  (= sws-pages (dec count-pages))]
         [:div
          [:div.level
           [:div.level-item.control
            [:input.input
             {:placeholder (i/i lang [:free-search])
              :value       (or @q (:q @(re-frame/subscribe [:display-filter?])))
              :on-change   (fn [e]
                             (let [ev      (.-value (.-target e))
                                   ev-size (count ev)]
                               (reset! q ev)
                               (when (or (= ev-size 0)
                                         (>= ev-size minimum-search-string-size))
                                 (async/go
                                   (async/>! display-filter-chan {:q ev})
                                   (async/<! (async/timeout timeout))
                                   (async/>! filter-chan {:q ev})))))}]]]
          [:div.level
           (when (not-empty (str (:id flt) (:group flt)))
             [:div.level-item.delete.is-large
              {:title    (i/i lang [:clear-filters])
               :on-click #(rfe/push-state :sws {:lang lang} {})}])
           [:div.level-item
            [:div.select
             [:select
              {:value     (or (:group flt) "")
               :on-change (fn [e]
                            (let [ev (.-value (.-target e))]
                              (async/go
                                (async/>! display-filter-chan {:group ev})
                                (async/>! filter-chan {:group ev}))))}
              [:option {:value ""} (i/i lang [:mimall])]
              [:option {:value "MIMO"} (i/i lang [:mimo])]
              [:option {:value "MIMDEV"} (i/i lang [:mimdev])]
              [:option {:value "MIMPROD"} (i/i lang [:mimprod])]]]]
           (when  (= lang "fr")
             [:div.level-item
              [:input {:type      "checkbox"
                       :checked   @(re-frame/subscribe [:only-public?])
                       :on-change #(re-frame/dispatch [:only-public!])}]
              [:div {:title (i/i lang [:public])}
               "  " [:img {:src   "/images/marianne.png"
                           :title (i/i lang [:public])
                           :width "65px"}]]])
           (when (= lang "fr")
             [:div.level-item
              [:input {:type      "checkbox"
                       :checked   @(re-frame/subscribe [:only-support?])
                       :on-change #(re-frame/dispatch [:only-support!])}]
              [:span {:title (i/i lang [:supported])} "  Support"]])
           [:div.level-item
            [:div.select
             [:select {:value     (:year flt)
                       :on-change (fn [e]
                                    (let [ev (.-value (.-target e))]
                                      (async/go
                                        (async/>! display-filter-chan {:year ev})
                                        (async/>! filter-chan {:year ev}))))}
              [:option {:value "2021"} "2021"]
              [:option {:value "2020"} "2020"]
              [:option {:value "2019"} "2019"]
              [:option {:value "2018"} "2018"]]]]
           [:a.level-item.button
            {:class    (str "is-" (if (= org-f :name) "info" "light"))
             :title    (i/i lang [:sort-alpha])
             :on-click #(re-frame/dispatch [:sort-sws-by! :name])} (i/i lang [:sort-alpha])]
           [:button.level-item.button.is-static
            (let [orgs count-sws]
              (str orgs " " (if (< orgs 2)
                              (i/i lang [:one-sw])
                              (i/i lang [:sws]))))]
           (navbar first-disabled last-disabled)
           [:a.level-item {:title    (i/i lang [:stats])
                           :on-click #(rfe/push-state :stats {:lang lang} {})}
            (fa "fa-chart-bar")]
           [:a.level-item {:title (i/i lang [:download])
                           :href  sill-csv-url}
            (fa "fa-file-csv")]]
          [:br]
          (if (= count-sws 0)
            [:div [:p (i/i lang [:no-sws-found])] [:br]]
            [sill-page lang sws sws-pages])
          [:br]
          (when (= count-sws 1)
            [:div
             (if-let [sws-si
                      (not-empty
                       (filter #(some (fn [e] (= e (:id %)))
                                      (s/split (:si (first sws)) #" *; *"))
                               @(re-frame/subscribe [:sws-nofilter?])))]
               [:div
                [:h2 (i/i lang [:similar-to])]
                [:br]
                [sill-page lang sws-si sws-pages]])
             [:br]
             [:div.is-size-4 (i/md-to-string (i/i lang [:need-more-data]))]])
          (when (> count-sws 50)
            (navbar first-disabled last-disabled))
          [:br]])

       :else ;; FIXME
       (rfe/push-state :sws {:lang lang}))]))

(defn main-class []
  (let [q (reagent/atom nil)]
    (reagent/create-class
     {:component-did-mount
      (fn []
        (GET "https://raw.githubusercontent.com/etalab/papillon/master/papillon.csv"
             :handler #(re-frame/dispatch
                        [:update-papillon! (rows->maps (csv/parse %))]))
        (GET "https://etalab.github.io/sill-data/sill.json"
             :handler #(re-frame/dispatch
                        [:update-sws! (walk/keywordize-keys %)])))
      :reagent-render (fn [] (main-page q))})))

(def routes
  [["/" :home-redirect]
   ["/:lang"
    ["/software" :sws]
    ["/papillon" :papillon]
    ["/stats" :stats]]])

(defn on-navigate [match]
  (let [target-page (:name (:data match))
        lang        (:lang (:path-params match))
        params      (:query-params match)]
    (when (string? lang) (re-frame/dispatch [:lang! lang]))
    (re-frame/dispatch [:view! (keyword target-page) params])))

(defn ^:export init []
  (re-frame/clear-subscription-cache!)
  (re-frame/dispatch-sync [:initialize-db!])
  (re-frame/dispatch
   [:lang! (subs (or js/navigator.language "en") 0 2)])
  (rfe/start!
   (rf/router routes {:conflicts nil})
   on-navigate
   {:use-fragment false})
  (start-filter-loop)
  (start-display-filter-loop)
  (reagent.dom/render
   [main-class]
   (. js/document (getElementById "app"))))
