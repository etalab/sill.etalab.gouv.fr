;; Copyright (c) 2019-2020 DINSIC, Bastien Guerry <bastien.guerry@data.gouv.fr>
;; SPDX-License-Identifier: EPL-2.0
;; License-Filename: LICENSES/EPL-2.0.txt

(ns sillweb.views
  (:require [cheshire.core :as json]
            [clojure.java.io :as io]
            [clojure.instant :as inst]
            [clj-http.client :as http]
            [hiccup.page :as h]
            [ring.util.anti-forgery :as afu]
            [ring.util.response :as response]
            [markdown-to-hiccup.core :as md]
            [sillweb.i18n :as i]))

(defn md-to-string [s]
  (-> s (md/md->hiccup) (md/component)))

(defn default [lang]
  (assoc
   (response/response
    (io/input-stream
     (io/resource
      (str "public/index." lang ".html"))))
   :headers {"Content-Type" "text/html; charset=utf-8"}))

(defn template [lang title subtitle content]
  (h/html5
   {:lang lang}
   [:head
    [:title title]
    [:meta {:charset "utf-8"}]
    [:meta {:name "keywords" :content (i/i lang [:keywords])}]
    [:meta {:name "description" :content (i/i lang [:keywords])}]
    [:meta {:name "viewport" :content "width=device-width, initial-scale=1, shrink-to-fit=yes"}]
    (h/include-css "https://cdnjs.cloudflare.com/ajax/libs/font-awesome/5.11.2/css/all.min.css")
    (h/include-css "/css/style.css")]
   [:body
    [:nav.navbar {:role "navigation" :aria-label "main navigation"}
     [:div.navbar-brand
      [:a.navbar-item {:href "/"}
       [:img {:src    "/images/logo-marianne.svg"
              :alt    "Logo Marianne"
              :width  "120"
              :height "100"}
        "sill.etalab.gouv.fr (alpha)"]]]
     [:div.navbar-end
      [:div.navbar-menu
       [:a.navbar-item {:href (str "/" lang "/contributors") :title (i/i lang [:contributors-baseline])} (i/i lang [:contributors])]
       [:a.navbar-item {:href (str "/" lang "/contact") :title (i/i lang [:contact-baseline])} (i/i lang [:contact])]
       [:a.navbar-item {:href (str "/" lang "/about") :title (i/i lang [:why-this-website?])} (i/i lang [:about])]
       [:a.navbar-item {:href  "https://www.etalab.gouv.fr"
                        :title (i/i lang [:main-etalab-website])} "Etalab"]]]]
    [:section.hero
     [:div.hero-body
      [:div.container
       [:h1.title.has-text-centered title]
       [:h2.subtitle.column.is-8.is-offset-2.has-text-centered subtitle]]]]
    [:section.section
     [:div.column.is-8.is-offset-2 content]]
    [:footer.footer
     [:div.content
      [:div.columns
       [:div.column.is-offset-2.is-4
        [:img {:src "/images/etalab.svg" :width "240px"}]]
       [:div.column.is-offset-1.is-4
        [:h1 "sill.etalab.gouv.fr"]
        [:p (i/i lang [:website-developed-by]) [:a {:href "https://www.etalab.gouv.fr"} "Etalab"]
         (i/i lang [:source-code-available]) [:a {:href "https://github.com/etalab/sillweb"}
                                              (i/i lang [:here])]]]]]]]))

(defn contact [lang]
  (template
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
      [:input {:name "organization" :type        "text"
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

(defn ok [lang] ;; FIXME: unused
  (template
   lang
   (i/i lang [:message-received])
   (i/i lang [:message-received-ok])
   [:div.has-text-centered
    [:a.button.is-large.is-primary
     {:href (str "/" lang "/repos")}
     (i/i lang [:back-to-website])]]))

(defn show-contributors [contributors]
  [:div.container
   (for [{:keys [Organisme Acronyme Annuaire]} contributors]
     ^{:key Acronyme}
     [:h2.subtitle
      (md-to-string (str Organisme " ([" Acronyme "](" Annuaire "))"))])
   [:br]])

(defn contributors [lang contributors]
  (condp = lang
    "fr" (template
          lang
          "Les organismes publics qui contribuent au SILL"
          [:span [:a {:href (str "/" lang "/contact")} "Contactez-nous"] " pour participer."]
          (show-contributors contributors))
    "en" (template
          lang
          "Public sector agencies contributing to sill.etalab.gouv.fr" ""
          (show-contributors contributors))
    "de" (template
          lang
          "Beitragszahler des öffentlichen Sektors für sill.etalab.gouv.fr" ""
          (show-contributors contributors))
    "es" (template
          lang
          "Agencias del sector público contribuyendo a sill.etalab.gouv.fr" ""
          (show-contributors contributors))))

(defn about [lang]
  (condp = lang
    "fr" (template
          lang
          "À propos de sill.etalab.gouv.fr" ""
          [:div.container
           [:h2.subtitle "Qu'est-ce que le Socle Interministériel de Logiciels Libres ?"]
           (md-to-string "Le SILL est une liste de [logiciels libres](https://fr.wikipedia.org/wiki/Logiciel_libre) significativement utilisés dans des organismes publics rattachés à la fonction publique d'État ou hospitalière et recommandés pour toute l'administration.")
           [:br]
           [:h2.subtitle "Qui choisit ces logiciels ?"]
           (md-to-string "Le SILL est construit par des agents de la fonction publique d'État ou hospitalière qui se réunissent régulièrement pour partager sur les usages effectifs de ces logiciels.  Vous pouvez voir [la liste des organismes contributeurs](/fr/contributors).")
           [:br]
           [:h2.subtitle "Puis-je rejoindre ces groupes et contribuer au SILL ?"]
           (md-to-string "Oui ! Votre aide est la bienvenue.  Vous pouvez [nous écrire depuis ce site](contact) ou directement à `opensource@data.gouv.fr`.")
           [:br]
           [:h2.subtitle "Ou trouver d'autres informations ?"]
           (md-to-string "Vous trouverez des informations plus détaillées sur [cette page](https://disic.github.io/sill/index.html).")
           [:br]
           ])
    "en" (template
          lang
          "About sill.etalab.gouv.fr" ""
          [:div.container
           [:h2.subtitle "What is this list of recommended free software for the public sector ?"]
           (md-to-string "This is a list of [free software](https://en.wikipedia.org/wiki/Free_software) heavily used in french public agencies and recommended for the public sector.")
           [:br]
           [:h2.subtitle "Who makes this list?"]
           (md-to-string "This list is built by public agents from public agencies: they meet IRL regularily to share the use they have of these software.  You can check the list of [contributing agencies](/en/contributors).")
           [:br]
           [:h2.subtitle "Can I join this group of public agents?"]
           (md-to-string "Sure! You help is welcome.  You can [reach us from this website](contact) or directly at `opensource@data.gouv.fr`.")
           [:br]
           [:h2.subtitle "Where can I find more information?"]
           (md-to-string "You can find more detailed information on [this page](https://disic.github.io/sill/index.html).")
           [:br]
           ])
    "de" (template
          lang
          "Über sill.etalab.gouv.fr" ""
          [:div.container
           [:h2.subtitle "Wozu dient die interministerielle Liste empfohlener open source Software ?"]
           (md-to-string "Die SILL ist eine Liste aller freier Software, die in der öffentlichen Verwaltung regelmässig genutzt wird.")
           [:br]
           [:h2.subtitle "Wer entscheidet, welche Software genutzt wird ?"]
           (md-to-string "Die Liste wird von Mitarbeitern aus verschiedenenen Abteilungder der öffentlichen Verwaltung gemeinsam erarbeitet. In regelmässigen Treffen tauschen diese sich über die Nutzung der Software in den jeweiligen Abteilungen aus. Sie können auf [die Liste der mitwirkenden Verwaltungen](/de/contributors) zugreifen.")
           [:br]
           [:h2.subtitle "Sind diese Arbeitsgruppen offen und kann ich zur Ausarbeitung der Liste beisteuern ?"]
           (md-to-string "Ja ! Jede Hilfe ist herzlich willkommen. Sie können uns über [diese Webseite kontaktieren](contact) oder an folgende Adresse schreiben: opensource@data.gouv.fr.")
           [:br]
           [:h2.subtitle "Wie kann ich mehr über den SILL erfahren ?"]
           (md-to-string "Weitere Informationen finden Sie [hier](https://disic.github.io/sill/index.html).")
           [:br]])
    "es" (template
          lang
          "Sobre sill.etalab.gouv.fr" ""
          [:div.container
           [:h2.subtitle "¿Qué es esta lista de programas gratis recomendados para el sector público?"]
           (md-to-string "Esto es una lista de [programas gratis](https://en.wikipedia.org/wiki/Free_software) bastante usada en agencias públicas francesas y recomendada para el sector público")
           [:br]
           [:h2.subtitle "¿Quién hace esta lista?"]
           (md-to-string "Esta lista es manejada por agentes públicos de agencias públicas: hacen reuniones en la vida real para compartir el uso que tienen sobre estos programas.  Puedes echarle un ojo a la lista de [agencias contribuyentes](/en/contributors).")
           [:br]
           [:h2.subtitle "¿Puedo unirme a este grupo de agentes públicos?"]
           (md-to-string "¡Claro! Tu ayuda siempre es bienvenida. Puedes [contactarnos a traves de este enlace](contact) o directamente en `opensource@data.gouv.fr`.")
           [:br]
           [:h2.subtitle "¿Dónde puedo encontrar más información?"]
           (md-to-string "Puedes encontrar información más detallada [aquí](https://disic.github.io/sill/index.html).")
           [:br]
           ])))

