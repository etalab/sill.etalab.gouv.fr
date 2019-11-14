(ns sillweb.i18n
  (:require [taoensso.tempura :refer [tr]]))

(def supported-languages #{"fr"})

(def localization
  ;; French translation
  {:fr
   {:no-sws-found            "Pas de logiciel trouvé : une autre idée de requête ?"
    :one-sw                  "Un logiciel"
    :sws                     "Logiciels"
    :contact-by-email        "Contacter par email"
    :go-to-website           "Visiter le site web"
    :license                 "Licence"
    :licenses                "Licences"
    :free-search             "Recherche libre"
    :remove-filter           "Supprimer le filtre : voir toutes les organisations ou groupes"
    :sort-alpha              "Ordre alphabetique"
    :keywords                "Accès aux codes sources du secteur public"
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
    :about                   "À propos"}})

(def opts {:dict localization})

(defn i [lang input] (tr opts [lang] input))
