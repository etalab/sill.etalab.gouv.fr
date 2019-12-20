(ns sillweb.i18n
  (:require [taoensso.tempura :refer [tr]]))

(def supported-languages #{"fr" "en"})

(def localization
  ;; French translation
  {:fr
   {:no-sws-found            "Pas de logiciel trouvé : une autre idée de requête ?"
    :one-sw                  "logiciel"
    :sws                     "logiciels"
    :contact-by-email        "Contacter par email"
    :go-to-website           "Visiter le site web"
    :license                 "Licence"
    :licenses                "Licences"
    :free-search             "Recherche libre"
    :remove-filter           "Supprimer le filtre : voir toutes les organisations ou groupes"
    :sort-alpha              "Ordre alphabetique"
    :keywords                "Liste des logiciels libres recommandés par l'État"
    :why-this-website?       "Pourquoi ce site ?"
    :main-etalab-website     "Site principal d'Etalab"
    :website-developed-by    "Site développé par la mission "
    :source-code-available   ", code source disponible "
    :here                    "ici"
    :contact                 "Contact"
    :contact-form            "Formulaire de contact"
    :contact-baseline        "Envie de contribuer ? Un point à éclaircir ? Sollicitez-nous !"
    :your-name               "Votre nom"
    :your-email              "Votre adresse de courriel"
    :email-placeholder       "Par ex. toto@modernisation.gouv.fr"
    :your-affiliation        "Votre organisme de rattachement"
    :affiliation-placeholder "Par ex. DGFiP"
    :your-message            "Message"
    :message-placeholder     "Votre message"
    :submit                  "Envoyer"
    :message-received        "Message reçu !"
    :message-received-ok     "Nous nous efforçons de répondre au plus vite."
    :back-to-websie          "Retour au site principal"
    :about                   "À propos"}
   :en
   {:no-sws-found            "No software found: try another query?"
    :one-sw                  "software"
    :sws                     "software"
    :contact-by-email        "Contact by email"
    :go-to-website           "Visit the website"
    :license                 "License"
    :licenses                "Licenses"
    :free-search             "Free search"
    :remove-filter           "Remove filter"
    :sort-alpha              "Alphabetical order"
    :keywords                "List of recommended software by the central administration"
    :why-this-website?       "Why this website?"
    :main-etalab-website     "Etalab main website"
    :website-developed-by    "Website developed by "
    :source-code-available   ", source code available "
    :here                    "here"
    :contact                 "Contact"
    :contact-form            "Contact form"
    :contact-baseline        "Want to contribute? A question? Contact us!"
    :your-name               "Your name"
    :your-email              "Your email address"
    :email-placeholder       "E.g. toto@modernisation.gouv.fr"
    :your-affiliation        "Your affiliation"
    :affiliation-placeholder "E.g. DGFiP"
    :your-message            "Message"
    :message-placeholder     "Your message"
    :submit                  "Send"
    :message-received        "Message received!"
    :message-received-ok     "We will reply as soon as possible."
    :back-to-websie          "Back to the main website."
    :about                   "About"}})

(def opts {:dict localization})

(defn i [lang input] (tr opts [lang] input))
