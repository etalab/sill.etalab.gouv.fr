;; Copyright (c) 2019-2020 DINSIC, Bastien Guerry <bastien.guerry@data.gouv.fr>
;; SPDX-License-Identifier: EPL-2.0
;; License-Filename: LICENSES/EPL-2.0.txt

(ns sillweb.views
  (:require [clojure.instant :as inst]
            [semantic-csv.core :as semantic-csv]
            [clj-rss.core :as rss]
            [hiccup.page :as h]
            [ring.util.anti-forgery :as afu]
            [ring.util.response :as response]
            [sillweb.i18n :as i]))

(def ^{:doc "The URL for the latest SILL updates."}
  latest-updates
  "https://raw.githubusercontent.com/DISIC/sill/master/2020/sill-updates.csv")

(defn sill-updates []
  (try (semantic-csv/slurp-csv latest-updates)
       (catch Exception e (println "Can get latest SILL updates: " e))))

(defn rss-feed
  "Generate a RSS feed from `sill-updates`."
  []
  (rss/channel-xml
   ;; FIXME: hardcode title/description if always english?
   {:title       (i/i "fr" [:updates])
    :link        "https://sill.etalab.gouv.fr/updates.xml"
    :description "Dernières mises à jour de https://sill.etalab.gouv.fr"}
   (sort-by :pubDate
            (map (fn [item]
                   (let [id   (:id item)
                         link (if (not (= id 0))
                                (format "https://sill.etalab.gouv.fr/fr/software?id=%s" id)
                                "https://sill.etalab.gouv.fr")]
                     {:title       (format "%s - %s" (:logiciel item) (:type item))
                      :link        link
                      :description (:commentaire item)
                      :author      "Etalab"
                      :pubDate     (inst/read-instant-date
                                    (str (first (re-find #"(\d+)-(\d+)-(\d+)" (:date item)))
                                         "T10:00:00Z"))}))
                 (sill-updates)))))

(defn rss
  "Expose the RSS feed."
  []
  (assoc
   (response/response (rss-feed))
   :headers {"Content-Type" "text/xml; charset=utf-8"}))

(defn icons []
  [:svg {:aria-hidden "true", :focusable "false", :style "display:none"} [:defs  [:symbol {:viewbox "0 0 32 32", :fill-rule "evenodd", :clip-rule "evenodd", :stroke-linejoin "round", :stroke-miterlimit "1.414", :id "copy", :xmlns "http://www.w3.org/2000/svg"} [:path {:d "M19.75 8.5V1H6.625L1 6.625V23.5h11.25V31H31V8.5H19.75zM6.625 3.651v2.974H3.651l2.974-2.974zm-3.75 17.974V8.5H8.5V2.875h9.375V8.5l-5.625 5.625v7.5H2.875zm15-10.474v2.974h-2.974l2.974-2.974zm11.25 17.974h-15V16h5.625v-5.625h9.375v18.75z", :fill-rule "nonzero"}]] [:symbol {:viewbox "0 0 32 32", :fill-rule "evenodd", :clip-rule "evenodd", :stroke-linejoin "round", :stroke-miterlimit "1.414", :id "envelope", :xmlns "http://www.w3.org/2000/svg"} [:g {:fill-rule "nonzero"} [:path {:d "M30.1 5.419c-9.355-.002-18.733-.005-28.1-.005A1.06 1.06 0 0 0 .975 6.439v19.122A1.06 1.06 0 0 0 2 26.586h28a1.061 1.061 0 0 0 1.025-1.025V6.439a1.056 1.056 0 0 0-.925-1.02zM3.025 7.464h25.95v17.072H3.025V7.464z"}] [:path {:d "M30.06 9.513c.933.098 1.382 1.395.393 1.945L16.54 18.287c-.438.188-.479.178-.893 0L1.733 11.458c-1.743-.968-.065-2.254.894-1.842l13.466 6.61 13.562-6.651c.3-.094.312-.062.405-.062z"}]]] [:symbol {:viewbox "0 0 32 32", :fill-rule "evenodd", :clip-rule "evenodd", :stroke-linejoin "round", :stroke-miterlimit "1.414", :id "facebook", :xmlns "http://www.w3.org/2000/svg"} [:path {:d "M28.188 1H3.812A2.821 2.821 0 0 0 1 3.813v24.375A2.821 2.821 0 0 0 3.813 31H16V17.875h-3.75v-3.75H16V12.25a5.635 5.635 0 0 1 5.625-5.625h3.75v3.75h-3.75a1.88 1.88 0 0 0-1.875 1.875v1.875h5.625l-.937 3.75H19.75V31h8.438A2.821 2.821 0 0 0 31 28.187V3.812A2.821 2.821 0 0 0 28.188 1z"}]] [:symbol {:viewbox "0 0 32 32", :fill-rule "evenodd", :clip-rule "evenodd", :stroke-linejoin "round", :stroke-miterlimit "1.414", :id "github", :xmlns "http://www.w3.org/2000/svg"} [:path {:d "M16 1.371c-8.284 0-15 6.715-15 15 0 6.627 4.298 12.25 10.258 14.233.75.138 1.026-.326 1.026-.722 0-.357-.014-1.54-.021-2.793-4.174.907-5.054-1.77-5.054-1.77-.682-1.733-1.665-2.195-1.665-2.195-1.361-.931.103-.912.103-.912 1.506.106 2.299 1.546 2.299 1.546 1.338 2.293 3.509 1.63 4.365 1.247.134-.969.523-1.631.952-2.006-3.331-.379-6.834-1.666-6.834-7.413 0-1.638.586-2.976 1.546-4.027-.156-.378-.669-1.903.145-3.969 0 0 1.26-.403 4.126 1.537a14.453 14.453 0 0 1 3.755-.505c1.274.006 2.558.173 3.757.505 2.864-1.94 4.121-1.537 4.121-1.537.816 2.066.303 3.591.147 3.969.962 1.05 1.544 2.389 1.544 4.027 0 5.761-3.509 7.029-6.849 7.401.538.466 1.017 1.379 1.017 2.778 0 2.007-.018 3.623-.018 4.117 0 .399.27.867 1.03.72C26.707 28.616 31 22.996 31 16.371c0-8.285-6.716-15-15-15z", :fill-rule "nonzero"}]] [:symbol {:viewbox "0 0 32 32", :fill-rule "evenodd", :clip-rule "evenodd", :stroke-linejoin "round", :stroke-miterlimit "1.414", :id "googleplus", :xmlns "http://www.w3.org/2000/svg"} [:path {:d "M28.188 1H3.812A2.821 2.821 0 0 0 1 3.813v24.375A2.821 2.821 0 0 0 3.813 31h24.375A2.821 2.821 0 0 0 31 28.187V3.812A2.821 2.821 0 0 0 28.187 1zM12.251 23.5a7.493 7.493 0 0 1-7.5-7.5c0-4.148 3.352-7.5 7.5-7.5 2.027 0 3.721.738 5.028 1.963l-2.039 1.958c-.557-.534-1.529-1.154-2.989-1.154-2.56 0-4.653 2.121-4.653 4.734s2.092 4.734 4.653 4.734c2.971 0 4.084-2.133 4.254-3.234h-4.253v-2.573h7.084c.064.375.117.75.117 1.243 0 4.289-2.872 7.33-7.201 7.33l-.001-.001zm15-7.5h-1.875v1.875h-1.875V16h-1.875v-1.875h1.875V12.25h1.875v1.875h1.875V16z", :fill-rule "nonzero"}]] [:symbol {:viewbox "0 0 32 32", :fill-rule "evenodd", :clip-rule "evenodd", :stroke-linejoin "round", :stroke-miterlimit "1.414", :id "magnifier", :xmlns "http://www.w3.org/2000/svg"} [:path {:d "M30.07 26.529l-7.106-6.043c-.735-.662-1.521-.964-2.155-.936a11.194 11.194 0 0 0 2.691-7.299c0-6.214-5.036-11.25-11.25-11.25S1 6.037 1 12.251s5.036 11.25 11.25 11.25c2.786 0 5.334-1.012 7.299-2.691-.03.634.274 1.42.936 2.155l6.043 7.106c1.035 1.149 2.725 1.247 3.756.216 1.031-1.032.934-2.723-.216-3.756l.002-.002zm-17.82-6.78a7.5 7.5 0 1 1 0-15 7.5 7.5 0 0 1 0 15z", :fill-rule "nonzero"}]] [:symbol {:viewbox "0 0 32 32", :fill-rule "evenodd", :clip-rule "evenodd", :stroke-linejoin "round", :stroke-miterlimit "1.414", :id "round-cross", :xmlns "http://www.w3.org/2000/svg"} [:path {:d "M16 .5C24.554.5 31.5 7.446 31.5 16S24.554 31.5 16 31.5.5 24.554.5 16 7.446.5 16 .5zm6.161 11.718a7.233 7.233 0 0 0-2.379-2.379L16 13.621l-3.782-3.782a7.233 7.233 0 0 0-2.379 2.379L13.621 16l-3.782 3.782a7.233 7.233 0 0 0 2.379 2.379L16 18.379l3.782 3.782a7.233 7.233 0 0 0 2.379-2.379L18.379 16l3.782-3.782z", :fill-rule "nonzero"}]] [:symbol {:viewbox "0 0 32 32", :fill-rule "evenodd", :clip-rule "evenodd", :stroke-linejoin "round", :stroke-miterlimit "1.414", :id "rss", :xmlns "http://www.w3.org/2000/svg"} [:path {:d "M28.188 1H3.812A2.821 2.821 0 0 0 1 3.813v24.375A2.821 2.821 0 0 0 3.813 31h24.375A2.821 2.821 0 0 0 31 28.187V3.812A2.821 2.821 0 0 0 28.187 1zM9.175 25.352a2.541 2.541 0 0 1-2.549-2.537 2.553 2.553 0 0 1 2.549-2.543 2.55 2.55 0 0 1 2.549 2.543 2.541 2.541 0 0 1-2.549 2.537zm6.399.023a8.913 8.913 0 0 0-2.62-6.339 8.882 8.882 0 0 0-6.328-2.625v-3.668c6.961 0 12.633 5.666 12.633 12.633h-3.685v-.001zm6.51 0c0-8.526-6.932-15.469-15.451-15.469V6.239c10.546 0 19.13 8.589 19.13 19.137h-3.68v-.001z", :fill-rule "nonzero"}]] [:symbol {:viewbox "0 0 32 32", :fill-rule "evenodd", :clip-rule "evenodd", :stroke-linejoin "round", :stroke-miterlimit "1.414", :id "twitter", :xmlns "http://www.w3.org/2000/svg"} [:path {:d "M31.003 6.695c-1.102.492-2.291.82-3.533.966a6.185 6.185 0 0 0 2.706-3.404 12.404 12.404 0 0 1-3.908 1.495 6.154 6.154 0 0 0-4.495-1.94 6.153 6.153 0 0 0-5.994 7.553A17.468 17.468 0 0 1 3.093 4.932a6.15 6.15 0 0 0-.831 3.094 6.147 6.147 0 0 0 2.736 5.122 6.16 6.16 0 0 1-2.789-.768v.076a6.154 6.154 0 0 0 4.94 6.034 6.149 6.149 0 0 1-2.783.106 6.177 6.177 0 0 0 5.748 4.277 12.347 12.347 0 0 1-9.117 2.549 17.4 17.4 0 0 0 9.44 2.766c11.32 0 17.513-9.381 17.513-17.514 0-.269-.005-.533-.017-.796a12.405 12.405 0 0 0 3.07-3.182v-.001z", :fill-rule "nonzero"}]]]])

(defn head [lang title content?]
  [:head
   [:title title]
   [:meta {:charset "utf-8"}]
   [:meta {:name "keywords" :content (i/i lang [:keywords])}]
   [:meta {:name "description" :content (i/i lang [:keywords])}]
   [:meta {:name "viewport" :content "width=device-width, initial-scale=1, shrink-to-fit=yes"}]
   [:link {:rel "canonical" :href "https://sill.etalab.gouv.fr/"}]
   [:link {:rel   "alternate" :type "application/rss+xml"
           :title (i/i lang [:subscribe-rss-flux])
           :href  "https://sill.etalab.gouv.fr/updates.xml"}]
   [:meta {:property "og:locale", :content "fr_FR"}]
   [:meta {:property "og:type", :content "website"}]
   [:meta {:property "og:title", :content (i/i lang [:index-title])}]
   [:meta {:property "og:url", :content "https://sill.etalab.gouv.fr/"}]
   [:meta {:property "og:site_name", :content (i/i lang [:index-title])}]
   [:meta {:property "og:image", :content "https://www.etalab.gouv.fr/wp-content/uploads/2019/06/etalab-white.png"}]
   [:meta {:name "twitter:card", :content "summary_large_image"}]
   [:meta {:name "twitter:title", :content (i/i lang [:index-title])}]
   [:meta {:name "twitter:site", :content "@Etalab"}]
   [:meta {:name "twitter:creator", :content "@Etalab"}]
   (h/include-css "https://cdnjs.cloudflare.com/ajax/libs/font-awesome/5.11.2/css/all.min.css")
   (h/include-css "/css/style.css")
   (when-not content? [:script {:src "/js/sillweb.js"}])
   [:script {:type "text/javascript" :async true} "var _paq = window._paq || [];_paq.push(['trackPageView']);_paq.push(['enableLinkTracking']);(function(){var u=\"//stats.data.gouv.fr/\";_paq.push(['setTrackerUrl', u+'piwik.php']);_paq.push(['setSiteId', '112']);var d=document, g=d.createElement('script'), s=d.getElementsByTagName('script')[0];g.type='text/javascript'; g.async=true; g.defer=true; g.src=u+'matomo.js'; s.parentNode.insertBefore(g,s);})();"]
   [:noscript [:p [:img {:src "//stats.data.gouv.fr/piwik.php?idsite=112&rec=1" :alt "" :style "border:0;"}]]]])

(defn footer [lang]
  [:footer.footer
   [:div.content
    [:div.columns
     [:div.column.is-offset-2.is-4
      [:img {:src "/images/etalab.svg" :width "240px"}]
      [:ul.footer__social
       [:li [:a {:href "https://twitter.com/etalab", :title "Twitter"}
             [:svg.icon.icon-twitter [:use {:xlink:href "#twitter"}]]]]
       [:li [:a {:href "https://github.com/etalab", :title "Github"}
             [:svg.icon.icon-github [:use {:xlink:href "#github"}]]]]
       [:li [:a {:href "https://www.facebook.com/etalab", :title "Facebook"}
             [:svg.icon.icon-fb [:use {:xlink:href "#facebook"}]]]]
       [:li [:a {:href "mailto:info@data.gouv.fr", :title (i/i lang [:contact-by-email])}
             [:svg.icon.icon-mail [:use {:xlink:href "#envelope"}]]]]]]
     [:div.column.is-offset-1.is-4
      [:h1 "sill.etalab.gouv.fr"]
      [:p (i/i lang [:website-developed-by])
       [:a {:href "https://www.etalab.gouv.fr"} "Etalab"]
       [:a {:href "https://github.com/etalab/sillweb"}
        (i/i lang [:source-code-available]) (i/i lang [:here]) "."]]]]]])

(defn default [lang & [title subtitle content]]
  (let [title    (or title (i/i lang [:index-title]))
        subtitle (or subtitle (i/i lang [:index-subtitle]))
        content0 (if content
                   [:div.column.is-8.is-offset-2 content]
                   [:div.column.is-10.is-offset-1
                    [:div.container {:id "app"}]])]
    (h/html5
     {:lang lang}
     (head lang title (not-empty content))
     [:body
      (icons)
      [:nav.navbar {:role "navigation" :aria-label "main navigation"}
       [:div.navbar-brand
        [:a.navbar-item {:href "https://sill.etalab.gouv.fr"}
         [:img {:src    "/images/logo-marianne.svg"
                :alt    "Logo Marianne"
                :width  "120"
                :height "100"}
          "sill.etalab.gouv.fr"]]]
       [:div.navbar-end
        [:div.navbar-menu
         [:a.navbar-item
          {:href  (str "/" lang "/contributors")
           :title (i/i lang [:contributors-baseline])} (i/i lang [:contributors])]
         [:a.navbar-item
          {:href  (str "/" lang "/contact")
           :title (i/i lang [:contact-baseline])} (i/i lang [:contact])]
         [:a.navbar-item
          {:href  (str "/" lang "/about")
           :title (i/i lang [:why-this-website?])} (i/i lang [:about])]
         [:a.navbar-item
          {:href  "https://www.etalab.gouv.fr"
           :title (i/i lang [:main-etalab-website])} "Etalab"]
         [:a.navbar-item.button {:href  "/updates.xml" :target "new"
                                 :title (i/i lang [:subscribe-rss-flux])}
          [:span.icon [:i.fas.fa-rss]]]]]]
      [:section.hero
       [:div.hero-body
        [:div.container
         [:h1.title.has-text-centered title]
         [:h2.subtitle.column.is-8.is-offset-2.has-text-centered subtitle]]]]
      [:section.section content0]
      (when-not content [:script {:async true} "sillweb.core.init();"])
      (footer lang)])))

(defn contact [lang]
  (default
   lang
   (i/i lang [:contact-form])
   (i/i lang [:contact-baseline])
   [:form
    {:action "/contact" :method "post"}
    (afu/anti-forgery-field)
    [:input {:name "lang" :type "hidden" :value lang}]
    [:div.columns
     [:div.field.column.is-6
      [:label.label (i/i lang [:your-name])]
      [:div.control
       [:input.input {:name "name" :type        "text"
                      :size "30"   :placeholder (i/i lang [:name])}]]]
     [:div.field.column.is-6
      [:label.label (i/i lang [:your-email])]
      [:div.control
       [:input.input {:name        "email"
                      :type        "email"
                      :size        "30"
                      :placeholder (i/i lang [:email-placeholder]) :required true}]]]]
    [:div.field
     [:label.label (i/i lang [:your-affiliation])]
     [:div.control
      [:input.input
       {:name "organization" :type        "text"
        :size "30"           :placeholder (i/i lang [:affiliation-placeholder])}]]]
    [:div.field
     [:label.label (i/i lang [:your-message])]
     [:div.control
      [:textarea.textarea {:rows        "10"
                           :name        "message"             
                           :placeholder (i/i lang [:message-placeholder]) :required true}]]]
    [:div.field.is-pulled-right
     [:div.control
      [:input.button.is-medium.is-info
       {:type  "submit"
        :value (i/i lang [:submit])}]]]
    [:br]]))

(defn ok [lang]
  (default
   lang
   (i/i lang [:message-received])
   (i/i lang [:message-received-ok])
   [:div.has-text-centered
    [:a.button.is-large.is-primary
     {:href (str "/" lang "/repos")}
     (i/i lang [:back-to-website])]]))

(defn show-contributors [contributors]
  [:div.container
   (for [{:keys [Organisme Acronyme Annuaire]} (sort-by :Organisme contributors)]
     ^{:key Acronyme}
     (when (and (not-empty Organisme) (not-empty Acronyme) (not-empty Annuaire))
       [:h2.subtitle
        (i/md-to-string (str Organisme " ([" Acronyme "](" Annuaire "))"))]))
   [:br]])

(defn contributors [lang contributors]
  (condp = lang
    "fr" (default
          lang
          "Les organismes publics qui contribuent au SILL"
          [:span [:a {:href (str "/" lang "/contact")} "Contactez-nous"] " pour participer."]
          (show-contributors contributors))
    "en" (default
          lang
          "Public sector agencies contributing to sill.etalab.gouv.fr" ""
          (show-contributors contributors))
    "de" (default
          lang
          "Beitragszahler des öffentlichen Sektors für sill.etalab.gouv.fr" ""
          (show-contributors contributors))
    "es" (default
          lang
          "Agencias del sector público contribuyendo a sill.etalab.gouv.fr" ""
          (show-contributors contributors))))

(defn about [lang]
  (condp = lang
    "fr" (default
          lang
          "À propos de sill.etalab.gouv.fr"
          [:span [:a {:href (str "/" lang "/contact")} "Contactez-nous"] " si vous avez d'autres questions."]
          [:div.container
           [:h2.subtitle "Qu'est-ce que le Socle Interministériel de Logiciels Libres ?"]
           (i/md-to-string "Le SILL est une liste de [logiciels libres](https://fr.wikipedia.org/wiki/Logiciel_libre) significativement utilisés dans des organismes publics rattachés à la fonction publique d'État ou hospitalière et qu'Etalab recommande pour toute l'administration.")
           [:br]
           [:h2.subtitle "Pourquoi le SILL est publié par ?"]
           (i/md-to-string "[Etalab](https://www.etalab.gouv.fr/) est le département de la [Direction interministérielle du numérique](https://www.numerique.gouv.fr/) consacré à la donnée publique. Etalab propose un [accompagnement](https://www.etalab.gouv.fr/accompagnement-logiciels-libres) autour des logiciels libres, à la fois pour aider les administrations à publier les codes sources qu'elles produisent et pour informer sur les logiciels libres qu'elles peuvent vouloir utiliser.  La publication du SILL est un référentiel important de cet accompagnement.")
           [:br]
           [:h2.subtitle "Qui choisit ces logiciels ?"]
           (i/md-to-string "Le SILL est construit par des agents de la fonction publique d'État et hospitalière qui se réunissent régulièrement pour partager sur les usages effectifs de ces logiciels.  Vous pouvez voir [la liste des organismes contributeurs](/fr/contributors).")
           [:br]
           [:h2.subtitle "Que veut dire « version minimale recommandée » ?"]
           (i/md-to-string "Chaque organisme est libre d'utiliser la version qu'il souhaite, y compris une version plus récente que celle indiquée dans le SILL.  Le SILL indique seulement quelle version est *actuellement* utilisée ; quand nous le pouvons, nous précisons le contexte d'utilisation du logiciel pour vous permettre de comprendre les contraintes qui justifient la recommandation de la version.")
           [:br]
           [:h2.subtitle "Puis-je rejoindre ces groupes et contribuer au SILL ?"]
           (i/md-to-string "Oui ! Votre aide est la bienvenue.  Vous pouvez [nous écrire depuis ce site](contact) ou directement à `opensource@data.gouv.fr`.")
           [:br]
           [:h2.subtitle "Ou trouver d'autres informations ?"]
           (i/md-to-string "Vous trouverez des informations plus détaillées sur [cette page](https://disic.github.io/sill/index.html).")]
          [:br]
          )
    "en" (default
          lang
          "About sill.etalab.gouv.fr" ""
          [:div.container
           [:h2.subtitle "What is this list of recommended free software for the public sector?"]
           (i/md-to-string "This is a list of [free software](https://en.wikipedia.org/wiki/Free_software) heavily used in french public agencies and recommended for the public sector.")
           [:br]
           [:h2.subtitle "Who makes this list?"]
           (i/md-to-string "This list is built by public agents from public agencies: they meet IRL regularily to share the use they have of these software.  You can check the list of [contributing agencies](/en/contributors).")
           [:br]
           [:h2.subtitle "Can I join this group of public agents?"]
           (i/md-to-string "Sure! You help is welcome.  You can [reach us from this website](contact) or directly at `opensource@data.gouv.fr`.")
           [:br]
           [:h2.subtitle "Where can I find more information?"]
           (i/md-to-string "You can find more detailed information on [this page](https://disic.github.io/sill/index.html).")
           [:br]
           ])
    "de" (default
          lang
          "Über sill.etalab.gouv.fr" ""
          [:div.container
           [:h2.subtitle "Wozu dient die interministerielle Liste empfohlener open source Software?"]
           (i/md-to-string "Die SILL ist eine Liste aller freier Software, die in der öffentlichen Verwaltung regelmässig genutzt wird.")
           [:br]
           [:h2.subtitle "Wer entscheidet, welche Software genutzt wird?"]
           (i/md-to-string "Die Liste wird von Mitarbeitern aus verschiedenenen Abteilungder der öffentlichen Verwaltung gemeinsam erarbeitet. In regelmässigen Treffen tauschen diese sich über die Nutzung der Software in den jeweiligen Abteilungen aus. Sie können auf [die Liste der mitwirkenden Verwaltungen](/de/contributors) zugreifen.")
           [:br]
           [:h2.subtitle "Sind diese Arbeitsgruppen offen und kann ich zur Ausarbeitung der Liste beisteuern ?"]
           (i/md-to-string "Ja ! Jede Hilfe ist herzlich willkommen. Sie können uns über [diese Webseite kontaktieren](contact) oder an folgende Adresse schreiben: opensource@data.gouv.fr.")
           [:br]
           [:h2.subtitle "Wie kann ich mehr über den SILL erfahren?"]
           (i/md-to-string "Weitere Informationen finden Sie [hier](https://disic.github.io/sill/index.html).")
           [:br]])
    "es" (default
          lang
          "Sobre sill.etalab.gouv.fr" ""
          [:div.container
           [:h2.subtitle "¿Qué es esta lista de programas gratis recomendados para el sector público?"]
           (i/md-to-string "Esto es una lista de [programas gratis](https://en.wikipedia.org/wiki/Free_software) bastante usada en agencias públicas francesas y recomendada para el sector público")
           [:br]
           [:h2.subtitle "¿Quién hace esta lista?"]
           (i/md-to-string "Esta lista es manejada por agentes públicos de agencias públicas: hacen reuniones en la vida real para compartir el uso que tienen sobre estos programas.  Puedes echarle un ojo a la lista de [agencias contribuyentes](/en/contributors).")
           [:br]
           [:h2.subtitle "¿Puedo unirme a este grupo de agentes públicos?"]
           (i/md-to-string "¡Claro! Tu ayuda siempre es bienvenida. Puedes [contactarnos a traves de este enlace](contact) o directamente en `opensource@data.gouv.fr`.")
           [:br]
           [:h2.subtitle "¿Dónde puedo encontrar más información?"]
           (i/md-to-string "Puedes encontrar información más detallada [aquí](https://disic.github.io/sill/index.html).")
           [:br]
           ])
    "it" (default
          lang
          "A proposito di sill.etalab.gouv.fr" ""
          [:div.container
           [:h2.subtitle "Cos'è questo elenco di software libero raccomandato per il settore pubblico?"]
           (i/md-to-string "Questo è un elenco di [software libero](https://it.wikipedia.org/wiki/Software_libero) largamente utilizzato nelle pubbliche amministrazioni francesi e consigliato per il settore pubblico.")
           [:br]
           [:h2.subtitle "Chi mantiene questo elenco?"]
           (i/md-to-string "Questo elenco è mantenuto da dipendenti pubblici e pubbliche amministrazioni che si si incontrano regolarmente nella vita reale IRL per condividere l'uso che fanno di questi software. Puoi consultare la lista delle [pubbliche amministrazioni che hanno contribuito](/en/contributors).")
           [:br]
           [:h2.subtitle "Posso unirmi a questo gruppo di dipendendenti pubblici?"]
           (i/md-to-string "Certo, il tuo aiuto è benvenuto. Puoi [contattarci attraverso il sito web](contact) o scrivendo direttamente a `opensource@data.gouv.fr`.")
           [:br]
           [:h2.subtitle "Dove posso reperire ulteriori informazioni?"]
           (i/md-to-string "Puoi trovare informazioni dettagliate [qui](https://disic.github.io/sill/index.html).")
           [:br]
           ])))

