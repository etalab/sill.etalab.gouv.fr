;; Copyright (c) 2019-2021 DINSIC, Bastien Guerry <bastien.guerry@data.gouv.fr>
;; SPDX-License-Identifier: EPL-2.0
;; License-Filename: LICENSES/EPL-2.0.txt

(ns sillweb.views
  (:require [clojure.instant :as inst]
            [semantic-csv.core :as semantic-csv]
            [clj-rss.core :as rss]
            [hiccup.page :as h]
            [ring.util.anti-forgery :as afu]
            [ring.util.response :as response]
            [sillweb.i18n :as i]
            [sillweb.config :as config]))

(def ^{:doc "The URL for the latest SILL updates."}
  latest-updates
  "https://git.sr.ht/~etalab/sill/blob/master/updates.csv")

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
   (h/include-css "https://cdnjs.cloudflare.com/ajax/libs/font-awesome/5.13.0/css/all.min.css")
   (h/include-css (str config/sillweb_base_url "/css/style.css"))
   (h/include-css (str config/sillweb_base_url "/css/custom.css"))
   (h/include-css (str config/sillweb_base_url "/css/dsfr.min.css"))
   [:script {:src  (str config/sillweb_base_url "/js/dsfr.module.min.js")
             :type "module"}]
   [:script {:src      (str config/sillweb_base_url "/js/dsfr.nomodule.min.js")
             :type     "text/javascript"
             :nomodule true}]
   (when-not content? [:script {:src (str config/sillweb_base_url "/js/sillweb.js")}])
   [:script {:src "https://tag.aticdn.net/611901/smarttag.js"}]
   [:script "var ATTag = new ATInternet.Tracker.Tag(); ATTag.page.send({name:'Page_Name'});"]
   [:script {:async true} "var _paq = window._paq || [];_paq.push(['trackPageView']);_paq.push(['enableLinkTracking']);(function(){var u=\"//stats.data.gouv.fr/\";_paq.push(['setTrackerUrl', u+'piwik.php']);_paq.push(['setSiteId', '112']);var d=document, g=d.createElement('script'), s=d.getElementsByTagName('script')[0];g.type='text/javascript'; g.async=true; g.defer=true; g.src=u+'matomo.js'; s.parentNode.insertBefore(g,s);})();"]
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
        (i/i lang [:source-code-available]) (i/i lang [:here]) "."]]
      (when (= lang "fr")
        [:P "Etalab est un département de la "
         [:a {:href "https://www.numerique.gouv.fr/"}
          "direction interministérielle du numérique"] "."])
      [:div.is-size-7
       [:p (i/i lang [:public-sector-websites])
        [:a {:href "https://www.elysee.fr"} "elysee.fr"]
        " "
        [:a {:href "https://www.gouvernement.fr"} "gouvernement.fr"]
        " "
        [:a {:href "https://www.service-public.fr"} "service-public.fr"]
        " "
        [:a {:href "https://legifrance.gouv.fr"} "legifrance.gouv.fr"]
        " "
        [:a {:href "https://data.gouv.fr"} "data.gouv.fr"]]]]]]])

(defn default [lang & [title subtitle content]]
  (let [title    (or title (i/i lang [:index-title]))
        subtitle (or subtitle (i/i lang [:index-subtitle]))
        content0 (if content
                   [:section.section.container content]
                   [:section.section.container {:id "app"}])]
    (h/html5
     {:lang lang}
     (head lang title (not-empty content))
     [:body
      [:nav.navbar.is-spaced {:role "navigation" :aria-label "main navigation"}
       [:div.navbar-brand
        [:a.navbar-item {:href "https://sill.etalab.gouv.fr"}
         [:img {:src   "/images/logo-marianne.svg"
                :width "120px"
                :alt   "Logo Marianne"}]]]
       [:div.navbar-end
        [:div.navbar-menu
         [:a.navbar-item
          {:href  (str "/" lang "/papillon")
           :title (i/i lang [:papillon])} "PAPILLON"]
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
      [:section.hero.is-small
       [:div.hero-body
        [:div.container
         [:p.title title]
         [:p.subtitle subtitle]]]]
      content0
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
     {:href (str "/" lang "/software")}
     (i/i lang [:back-to-website])]]))

(defn show-contributors [contributors]
  [:div.container
   (for [{:keys [Organisme Acronyme Annuaire PULL]} (sort-by :Organisme contributors)]
     ^{:key Acronyme}
     (when (and (not-empty Organisme) (not-empty Acronyme) (not-empty Annuaire))
       (if (not-empty PULL)
         [:h2.subtitle
          (i/md-to-string (str Organisme " ([" Acronyme "](" Annuaire ")) - [Politique d'utilisation de logiciels libres](" PULL ")"))]
         [:h2.subtitle
          (i/md-to-string (str Organisme " ([" Acronyme "](" Annuaire "))"))])))
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
           (i/md-to-string "Le SILL est une liste de [logiciels libres](https://fr.wikipedia.org/wiki/Logiciel_libre) significativement utilisés dans des organismes publics rattachés à la fonction publique d'État ou hospitalière et qui forment un catalogue de référence pour l'administration.")
           [:br]
           [:h2.subtitle "Pourquoi le SILL est-il publié par Etalab ?"]
           (i/md-to-string "[Etalab](https://www.etalab.gouv.fr/) est le département de la [Direction interministérielle du numérique](https://www.numerique.gouv.fr/) consacré à la donnée publique. Etalab propose un [accompagnement](https://www.etalab.gouv.fr/accompagnement-logiciels-libres) autour des logiciels libres, à la fois pour aider les administrations à publier les codes sources qu'elles produisent et pour informer sur les logiciels libres qu'elles peuvent vouloir utiliser.  La publication du SILL est un référentiel important de cet accompagnement.")
           [:br]
           [:h2.subtitle "Qui choisit ces logiciels ?"]
           (i/md-to-string "Le SILL est construit par des agents de la fonction publique d'État et hospitalière qui se réunissent régulièrement pour partager sur les usages effectifs de ces logiciels.  Vous pouvez voir [la liste des organismes contributeurs](/fr/contributors).")
           [:br]
           [:a {:name "support"} [:h2.subtitle "Que signifie « couvert par le marché de support » ?"]]
           [:br]
           (i/md-to-string "Il existe un marché de support interministériel pour certains logiciels libres.  Le SILL indique pour chaque logiciel s'il est couvert par le support.")
           [:br]
           [:h2.subtitle "Que signifie « version recommandée » ?"]
           (i/md-to-string "Chaque organisme est libre d'utiliser la version qu'il souhaite, y compris une version plus récente que celle indiquée dans le SILL.  Le SILL indique seulement quelle version est *actuellement* utilisée par une administration, appelée « version minimale » ; si une version « maximale » est indiquée, il n'est pas recommandé d'utiliser le logiciel dans une version ultérieure.  Quand nous le pouvons, nous précisons le contexte d'utilisation du logiciel pour vous permettre de comprendre les contraintes qui justifient la recommandation de la version minimale.")
           [:br]
           [:h2.subtitle "Que signifie « en observation » ?"]
           (i/md-to-string "Les logiciels du SILL sont portés par des référents qui les évaluent. Lorsqu'un logiciel est proposé par un référent SILL, il peut entrer « en observation », le temps que l'ensemble des référents s'accorde à proposer ce logiciel en recommandation.")
           [:br]
           [:h2.subtitle "Puis-je télécharger le SILL ?"]
           (i/md-to-string "Oui, vous pouvez télécharger le SILL actuel [au format CSV](https://git.sr.ht/~etalab/sill/blob/master/sill.csv) ou la version en date du 5 mai [au format PDF](https://www.mim-libre.fr/wp-content/uploads/2020/05/sill-2020.pdf).")
           [:br]
           [:h2.subtitle "Puis-je rejoindre ces groupes et contribuer au SILL ?"]
           (i/md-to-string "Oui ! Votre aide est la bienvenue.  Si vous êtes agent d'une administration publique et souhaitez faire référencer un logiciel libre que vous utilisez, vous pouvez notamment vous proposer comme référent SILL. N'hésitez pas à [nous écrire depuis ce site](contact) ou directement via `logiciels-libres@data.gouv.fr`.")
           [:br]
           [:h2.subtitle "Où trouver d'autres informations ?"]
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
           (i/md-to-string "Sure! You help is welcome.  You can [reach us from this website](contact) or directly at `logiciels-libres@data.gouv.fr`.")
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
           (i/md-to-string "Ja ! Jede Hilfe ist herzlich willkommen. Sie können uns über [diese Webseite kontaktieren](contact) oder an folgende Adresse schreiben: logiciels-libres@data.gouv.fr.")
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
           (i/md-to-string "¡Claro! Tu ayuda siempre es bienvenida. Puedes [contactarnos a traves de este enlace](contact) o directamente en `logiciels-libres@data.gouv.fr`.")
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
           (i/md-to-string "Certo, il tuo aiuto è benvenuto. Puoi [contattarci attraverso il sito web](contact) o scrivendo direttamente a `logiciels-libres@data.gouv.fr`.")
           [:br]
           [:h2.subtitle "Dove posso reperire ulteriori informazioni?"]
           (i/md-to-string "Puoi trovare informazioni dettagliate [qui](https://disic.github.io/sill/index.html).")
           [:br]
           ])))
