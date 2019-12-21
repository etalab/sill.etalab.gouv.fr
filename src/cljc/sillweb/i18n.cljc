(ns sillweb.i18n
  (:require [taoensso.tempura :refer [tr]]))

(def supported-languages #{"fr" "en"})

(def localization
  ;; French translation
  {:fr
   {
    :about                   "À propos"
    :affiliation-placeholder "Par ex. DGFiP"
    :back-to-websie          "Retour au site principal"
    :contact                 "Contact"
    :contact-baseline        "Envie de contribuer ? Un point à éclaircir ? Sollicitez-nous !"
    :contact-by-email        "Contacter par email"
    :contact-form            "Formulaire de contact"
    :email-placeholder       "Par ex. toto@modernisation.gouv.fr"
    :free-search             "Recherche libre"
    :go-to-website           "Visiter le site web"
    :here                    "ici"
    :keywords                "Liste des logiciels libres recommandés par l'État"
    :license                 "Licence"
    :licenses                "Licences"
    :main-etalab-website     "Site principal d'Etalab"
    :message-placeholder     "Votre message"
    :message-received        "Message reçu !"
    :message-received-ok     "Nous nous efforçons de répondre au plus vite."
    :no-sws-found            "Pas de logiciel trouvé : une autre idée de requête ?"
    :one-sw                  "logiciel"
    :remove-filter           "Supprimer le filtre : voir toutes les organisations ou groupes"
    :sort-alpha              "Ordre alphabetique"
    :source-code-available   ", code source disponible "
    :submit                  "Envoyer"
    :sws                     "logiciels"
    :version                 "Version"
    :website-developed-by    "Site développé par la mission "
    :why-this-website?       "Pourquoi ce site ?"
    :your-affiliation        "Votre organisme de rattachement"
    :your-email              "Votre adresse de courriel"
    :your-message            "Message"
    :your-name               "Votre nom"
    :warning-testing         "Ce logiciel est actuellement en observation."
    }
   :en
   {
    :about                   "About"
    :affiliation-placeholder "E.g. DGFiP"
    :back-to-websie          "Back to the main website."
    :contact                 "Contact"
    :contact-baseline        "Want to contribute? A question? Contact us!"
    :contact-by-email        "Contact by email"
    :contact-form            "Contact form"
    :email-placeholder       "E.g. toto@modernisation.gouv.fr"
    :free-search             "Free search"
    :go-to-website           "Visit the website"
    :here                    "here"
    :keywords                "List of recommended software by the central administration"
    :license                 "License"
    :licenses                "Licenses"
    :main-etalab-website     "Etalab main website"
    :message-placeholder     "Your message"
    :message-received        "Message received!"
    :message-received-ok     "We will reply as soon as possible."
    :no-sws-found            "No software found: try another query?"
    :one-sw                  "software"
    :remove-filter           "Remove filter"
    :sort-alpha              "Alphabetical order"
    :source-code-available   ", source code available "
    :submit                  "Send"
    :sws                     "software"
    :version                 "Version"
    :website-developed-by    "Website developed by "
    :why-this-website?       "Why this website?"
    :your-affiliation        "Your affiliation"
    :your-email              "Your email address"
    :your-message            "Message"
    :your-name               "Your name"
    :warning-testing         "This software is currently being tested."
    }})

(def opts {:dict localization})

(defn i [lang input] (tr opts [lang] input))
