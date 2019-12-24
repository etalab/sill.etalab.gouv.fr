;; Copyright (c) 2019 DINSIC, Bastien Guerry <bastien.guerry@data.gouv.fr>
;; SPDX-License-Identifier: EPL-2.0
;; License-Filename: LICENSES/EPL-2.0.txt

(ns sillweb.core
  (:require [cljs.core.async :as async]
            [re-frame.core :as re-frame]
            [reagent.core :as reagent]
            [reagent.session :as session]
            [ajax.core :refer [GET POST]]
            [markdown-to-hiccup.core :as md]
            [sillweb.i18n :as i]
            [clojure.string :as s]
            [reitit.frontend :as rf]
            [reitit.frontend.easy :as rfe]))

(defonce dev? false)
(defonce sws-per-page 100)
(defonce timeout 100)
(defonce init-filter {:q "" :group "all" :status "all"})
(defonce frama-base-url "https://framalibre.org/content/")

(re-frame/reg-event-db
 :initialize-db!
 (fn [_ _]
   {:sws-page       0
    :sws            nil
    :sort-sws-by    :name
    :view           :sws
    :reverse-sort   false
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
 (fn [db [_ sws]] (if sws (assoc db :sws sws))))

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
  (if (and (string? s) (string? sub))
    (s/includes? (s/lower-case s) (s/lower-case sub))))

(defn apply-sws-filters [m]
  (let [f @(re-frame/subscribe [:filter?])
        s (:q f)
        g (:group f)
        r (:status f)]
    (filter
     #(and (if s (s-includes?
                  (s/join "" [(:i %) (:fr-desc %) (:en-desc %) (:f %)]) s)
               true)
           (if-not (= r "all")
             (= (:s %) r)
             true)
           (if (and (not-empty g)
                    (not (= g "all")))
             (= g (:g %)) true))
     m)))

(def filter-chan (async/chan 100))
(def display-filter-chan (async/chan 100))

(defn start-display-filter-loop []
  (async/go
    (loop [f (async/<! display-filter-chan)]
      (re-frame/dispatch [:display-filter! f])
      (recur (async/<! display-filter-chan)))))

(defn start-filter-loop []
  (async/go
    (loop [f (async/<! filter-chan)]
      (let [v  @(re-frame/subscribe [:view?])
            l  @(re-frame/subscribe [:lang?])
            fs @(re-frame/subscribe [:filter?])]
        (rfe/push-state v {:lang l}
                        (filter #(and (string? (val %))
                                      (not-empty (val %)))
                                (merge fs f))))
      (re-frame/dispatch [:filter! f])
      (recur (async/<! filter-chan)))))

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

(defn sill-page [lang sws-cnt]
  (into
   [:div]
   (if (= sws-cnt 0)
     [[:p (i/i lang [:no-sws-found])] [:br]]
     (for [dd (partition-all
               3 (take sws-per-page
                       (drop (* sws-per-page @(re-frame/subscribe [:sws-page?]))
                             @(re-frame/subscribe [:sws?]))))]
       ^{:key dd}
       [:div.columns
        (for [{:keys [s f l v i g logo fr-desc en-desc website doc sources frama]
               :as   o} dd]
          ^{:key o}
          [:div.column.is-4
           [:div.card
            [:div.card-content
             [:div.media
              [:div.media-content
               [:h2.subtitle i
                (when (= s "O")
                  [:sup.is-size-7.has-text-grey
                   {:title (i/i lang [:warning-testing])}
                   (fa "fa-exclamation-triangle")])]]
              (if (not-empty logo)
                [:div.media-right
                 [:figure.image.is-64x64
                  [:img {:src logo}]]])]
             [:div.content
              [:p (or (cond (= lang "fr") fr-desc
                            (= lang "en") en-desc)
                      f)]
              (when-let [n (not-empty (:encoded-name frama))]
                [:p [:a {:href   (str frama-base-url n)
                         :target "new"}
                     (str (i/i lang [:on-framalibre])
                          (:name frama))]])]]
            [:div.card-footer
             (when website
               [:div.card-footer-item
                [:a {:href   website
                     :target "new"
                     :title  (i/i lang [:go-to-website])}
                 (fa "fa-globe")]])
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
             [:div.card-footer-item
              [:p {:title (i/i lang [:recommended_version])}
               (str (i/i lang [:version]) v)]]
             (when (not-empty l)
               [:div.card-footer-item
                [:p
                 (for [ll (s/split l #", ")]
                   ^{:key ll}
                   [:span
                    [:a {:href   (str "https://spdx.org/licenses/" ll ".html")
                         :title  (i/i lang [:license])
                         :target "new"}
                     ll]
                    " "])]])]]])]))))

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

(defn main-page [q language]
  (let [lang @(re-frame/subscribe [:lang?])
        flt  @(re-frame/subscribe [:filter?])]
    [:div
     (cond

       (= @(re-frame/subscribe [:view?]) :home-redirect)
       (if dev?
         [:p "Testing."]
         (if (contains? i/supported-languages lang)
           (do (set! (.-location js/window) (str "/" lang "/software")) "")
           (do (set! (.-location js/window) (str "/en/software")) "")))

       (= @(re-frame/subscribe [:view?]) :sws)
       (let [org-f          @(re-frame/subscribe [:sort-sws-by?])
             sws            @(re-frame/subscribe [:sws?])
             sws-pages      @(re-frame/subscribe [:sws-page?])
             count-pages    (count (partition-all sws-per-page sws))
             first-disabled (= sws-pages 0)
             last-disabled  (= sws-pages (dec count-pages))]
         [:div
          [:div.level-left
           [:p.control.level-item
            [:input.input
             {:size        20
              :placeholder (i/i lang [:free-search])
              :value       (or @q (:q @(re-frame/subscribe [:display-filter?])))
              :on-change   (fn [e]
                             (let [ev (.-value (.-target e))]
                               (reset! q ev)
                               (async/go
                                 (async/>! display-filter-chan {:q ev})
                                 (<! (async/timeout timeout))
                                 (async/>! filter-chan {:q ev}))))}]]
           [:div.select.level-item
            [:select {:value     (or (:group flt) "all")
                      :on-change (fn [e]
                                   (let [ev (.-value (.-target e))]
                                     (async/go
                                       (async/>! display-filter-chan {:group ev})
                                       (async/>! filter-chan {:group ev}))))}
             [:option {:value "all"} (i/i lang [:mimall])]
             [:option {:value "MIMO"} (i/i lang [:mimo])]
             [:option {:value "MIMDEV"} (i/i lang [:mimdev])]
             [:option {:value "MIMPROD"} (i/i lang [:mimprod])]]]
           [:div.select.level-item
            [:select {:value     (or (:status flt) "all")
                      :on-change (fn [e]
                                   (let [ev (.-value (.-target e))]
                                     (async/go
                                       (async/>! display-filter-chan {:status ev})
                                       (async/>! filter-chan {:status ev}))))}
             [:option {:value "all"} (i/i lang [:all])]
             [:option {:value "R"} (i/i lang [:recommended])]
             [:option {:value "O"} (i/i lang [:tested])]]]
           [:a.button.level-item
            {:class    (str "is-" (if (= org-f :name) "warning" "light"))
             :title    (i/i lang [:sort-alpha])
             :on-click #(re-frame/dispatch [:sort-sws-by! :name])} (i/i lang [:sort-alpha])]
           [:span.button.is-static.level-item
            (let [orgs (count sws)]
              (str orgs " " (if (< orgs 2) (i/i lang [:one-sw]) (i/i lang [:sws]))))]
           [:nav.pagination.level-item
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
             (fa "fa-fast-forward")]]]
          [:br]
          [sill-page lang (count sws)]
          [:br]])

       :else
       (rfe/push-state :sws {:lang lang}))]))

(defn main-class []
  (let [q        (reagent/atom nil)
        language (reagent/atom nil)]
    (reagent/create-class
     {:component-will-mount
      (fn []
        (GET "/sill" :handler
             #(re-frame/dispatch
               [:update-sws! (clojure.walk/keywordize-keys %)])))
      :reagent-render (fn [] (main-page q language))})))

(def routes
  [["/" :home-redirect]
   ["/:lang"
    ["/software" :sws]]])

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
  (reagent/render
   [main-class]
   (. js/document (getElementById "app"))))
