;; Copyright (c) 2019-2020 DINSIC, Bastien Guerry <bastien.guerry@data.gouv.fr>
;; SPDX-License-Identifier: EPL-2.0
;; License-Filename: LICENSES/EPL-2.0.txt

(ns sillweb.i18n
  (:require [taoensso.tempura :refer [tr]]))

(def supported-languages #{"fr" "en" "de" "es" "it"})

(def localization
  ;; French translation
  {:fr
   {
    :about                   "À propos"
    :affiliation-placeholder "Par ex. DGFiP"
    :all                     "Tous"
    :back-to-website         "Retour au site principal"
    :clear-filters           "Effacer les filtres"
    :contact                 "Contact"
    :contact-baseline        "Envie de contribuer ? Un point à éclaircir ? Sollicitez-nous !"
    :contact-by-email        "Contacter par email"
    :contact-form            "Formulaire de contact"
    :context-of-use          "Contexte d'usage :"
    :contributors            "Contributeurs"
    :contributors-baseline   "La liste des organismes publics contributeurs"
    :download                "Télécharger"
    :email-placeholder       "Par ex. toto@modernisation.gouv.fr"
    :free-search             "Recherche libre"
    :function                "Fonction :"
    :go-to-source            "Voir le code source"
    :go-to-website           "Visiter le site web"
    :here                    "ici"
    :keywords                "Liste des logiciels libres recommandés par l'État"
    :license                 "Licence"
    :licenses                "Licences"
    :main-etalab-website     "Site principal d'Etalab"
    :message-placeholder     "Votre message"
    :message-received        "Message reçu !"
    :message-received-ok     "Nous nous efforçons de répondre au plus vite."
    :mimall                  "Tout type"
    :mimdev                  "Développement (MIMDEV)"
    :mimo                    "Bureautique (MIMO)"
    :mimprod                 "Production (MIMPROD)"
    :name                    "Nom"
    :no-sws-found            "Pas de logiciel trouvé : une autre idée de requête ?"
    :on-comptoir             "Fiche Comptoir du libre"
    :on-framalibre           "Sur Framalibre : "
    :one-sw                  "logiciel"
    :read-the-docs           "Lire la documentation"
    :recommended             "Recommandés"
    :recommended_version     "Version utilisée et recommandée"
    :sort-alpha              "A-Z <=> Z-A"
    :source-code-available   ", code source disponible "
    :submit                  "Envoyer"
    :sws                     "logiciels"
    :tested                  "En observation"
    :version                 "Version: "
    :warning-testing         "Ce logiciel est actuellement en observation."
    :website-developed-by    "Site développé par la mission "
    :why-this-website?       "Pourquoi ce site ?"
    :your-affiliation        "Votre organisme de rattachement"
    :your-email              "Votre adresse de courriel"
    :your-message            "Message"
    :your-name               "Votre nom"
    :year                    "Année"
    }
   :en
   {
    :about                   "About"
    :affiliation-placeholder "E.g. DGFiP"
    :all                     "All"
    :back-to-website         "Back to the main website."
    :clear-filters           "Clear filters"
    :contact                 "Contact"
    :contact-baseline        "Want to contribute? A question? Contact us!"
    :contact-by-email        "Contact by email"
    :contact-form            "Contact form"
    :context-of-use          "Context of use:"
    :contributors            "Contributors"
    :contributors-baseline   "The list of contributing public sector agencies"
    :download                "Download"
    :email-placeholder       "E.g. toto@modernisation.gouv.fr"
    :free-search             "Free search"
    :function                "Function:"
    :go-to-source            "Browse the source code"
    :go-to-website           "Visit the website"
    :here                    "here"
    :keywords                "List of recommended software by the public sector"
    :license                 "License"
    :licenses                "Licenses"
    :main-etalab-website     "Etalab main website"
    :message-placeholder     "Your message"
    :message-received        "Message received!"
    :message-received-ok     "We will reply as soon as possible."
    :mimall                  "All types"
    :mimdev                  "Development tools"
    :mimo                    "Office tools"
    :mimprod                 "Production tools"
    :name                    "Name"
    :no-sws-found            "No software found: try another query?"
    :on-comptoir             "On Comptoir du libre"
    :on-framalibre           "On Framalibre: "
    :one-sw                  "software"
    :read-the-docs           "Read the documentation"
    :recommended             "Recommended"
    :recommended_version     "Used and recommended version"
    :sort-alpha              "A-Z <=> Z-A"
    :source-code-available   ", source code available "
    :submit                  "Send"
    :sws                     "software"
    :tested                  "Under observation"
    :version                 "Version: "
    :warning-testing         "This software is currently being tested."
    :website-developed-by    "Website developed by "
    :why-this-website?       "Why this website?"
    :your-affiliation        "Your affiliation"
    :your-email              "Your email address"
    :your-message            "Message"
    :your-name               "Your name"
    :year                    "Year"
    }
   :de
   {
    :about                   "Über uns"
    :affiliation-placeholder "z.B. DGFiP"
    :all                     "Alle"
    :back-to-website         "Home"
    :clear-filters           "Filter löschen"
    :contact                 "Kontakt"
    :contact-baseline        "Sie haben Fragen oder Anregungen ? Schreiben Sie uns !"
    :contact-by-email        "Kontakt per Email."
    :contact-form            "Kontaktformular"
    :context-of-use          "Nutzungskontext:"
    :contributors            "Beitragszahler"
    :contributors-baseline   "Beitragszahler des öffentlichen Sektors"
    :download                "Download"
    :email-placeholder       "z.B. toto@modernisation.gouv.fr"
    :free-search             "Freie Suche"
    :function                "Funktion:"
    :go-to-source            "Quellcode sehen"
    :go-to-website           "Webseite besuchen"
    :here                    "hier"
    :keywords                "Empfohlene open source Software für die öffentliche Verwaltung"
    :license                 "Lizenz"
    :licenses                "Lizenzen"
    :main-etalab-website     "Webseite von Etalab"
    :message-placeholder     "Ihre Nachricht"
    :message-received        "Nachricht erhalten !"
    :message-received-ok     "Sie hören bald von uns !"
    :mimall                  "Alle"
    :mimdev                  "Software-Entwicklung (MIMDEV)"
    :mimo                    "Bürosoftware (MIMO)"
    :mimprod                 "Produktion (MIMPROD)"
    :name                    "Name"
    :no-sws-found            "Keine Ergebnisse : Versuchen Sie einen anderen Suchbegriff."
    :on-comptoir             "Auf Comptoir du libre"
    :on-framalibre           "Auf Framalibre : "
    :one-sw                  "Software"
    :read-the-docs           "Dokumentation leseen"
    :recommended             "Empfohlen"
    :recommended_version     "Genutzte und empfohlene Version"
    :sort-alpha              "A-Z <=> Z-A"
    :source-code-available   ", Sourcecode verfügbar "
    :submit                  "Abschicken"
    :sws                     "Software"
    :tested                  "In Beobachtung"
    :version                 "Version: "
    :warning-testing         "Evaluierung dieser Software noch nicht abgeshlossen."
    :website-developed-by    "Webseite entwickelt von "
    :why-this-website?       "Warum diese Webseite ?"
    :your-affiliation        "Organisation"
    :your-email              "Email-Adresse"
    :your-message            "Nachricht"
    :your-name               "Name"
    :year                    "Jahr"
    }
   :es
   {
    :about                   "Sobre nosotros"
    :affiliation-placeholder "E.g. DGFiP"
    :all                     "Todo"
    :back-to-website         "Volver a la web principal"
    :clear-filters           "Reiniciar filtros"
    :contact                 "Contacto"
    :contact-baseline        "¿Quieres contribuir? ¿Una pregunta? ¡Contactanos!"
    :contact-by-email        "Contacto por email"
    :contact-form            "Formulario de contacto"
    :context-of-use          "Contexto de uso:"
    :contributors            "Contribuidores"
    :contributors-baseline   "Lita de agencias del sector público contribuyentes"
    :download                "Descargar"
    :email-placeholder       "E.g. toto@modernisation.gouv.fr"
    :free-search             "Busqueda libre"
    :function                "Función:"
    :go-to-source            "Navega al código fuente"
    :go-to-website           "Visita el sitio web"
    :here                    "aquí"
    :keywords                "Lista de programas recomendados por el sector público"
    :license                 "Licencia"
    :licenses                "Licencias"
    :main-etalab-website     "Web principal de etalab"
    :message-placeholder     "Tu mensaje"
    :message-received        "¡Mensaje recibido!"
    :message-received-ok     "Te responderemos lo más rápido posible."
    :mimall                  "Todos los tipos"
    :mimdev                  "Herramientas de desarrollo"
    :mimo                    "Herramientas de oficina"
    :mimprod                 "Herramientas de producción"
    :name                    "Nombre"
    :no-sws-found            "No se encontraoron programas, intenta de otra manera"
    :on-comptoir             "En Comptoir du libre"
    :on-framalibre           "En Framalibre: "
    :one-sw                  "programas"
    :read-the-docs           "Leer la documentación"
    :recommended             "Recomendado"
    :recommended_version     "Version usada y recomendada"
    :sort-alpha              "A-Z <=> Z-A"
    :source-code-available   ", código fuente disponible "
    :submit                  "Enviar"
    :sws                     "programas"
    :tested                  "En observación"
    :version                 "Versión: "
    :warning-testing         "Este programa esta siendo actualmente testado."
    :website-developed-by    "Sitio web desarrollado por "
    :why-this-website?       "¿Por qué este sitio web?"
    :your-affiliation        "Tu asociación"
    :your-email              "Tu correo electrónico"
    :your-message            "Mensaje"
    :your-name               "Tu nombre"
    :year                    "Año"
    }
   :it
   {
    :about                   "Chi siamo"
    :affiliation-placeholder "E.g. DGFiP"
    :all                     "Tutti"
    :back-to-website         "Torna al sito principale."
    :clear-filters           "Azzera filtri."
    :contact                 "Contatta"
    :contact-baseline        "Vuoi contribuire? Domande? Contattaci!"
    :contact-by-email        "Contattaci via email"
    :contact-form            "Modulo di contatto"
    :context-of-use          "Contesto d'uso:"
    :contributors            "Contributori"
    :contributors-baseline   "Lista delle pubbliche amministrazioni che hanno contribuito"
    :download                "Scarica"
    :email-placeholder       "E.g. toto@modernisation.gouv.fr"
    :free-search             "Ricerca libera"
    :function                "Funzione:"
    :go-to-source            "Naviga nel codice sorgente"
    :go-to-website           "Visita il sito web"
    :here                    "qui"
    :keywords                "Lista del software consigliato per la pubblica amministrazione"
    :license                 "Licenza"
    :licenses                "Licenze"
    :main-etalab-website     "Sito principale di Etalab"
    :message-placeholder     "Tuo messaggio"
    :message-received        "Messaggi ricevuti!"
    :message-received-ok     "Risponderemo appena possibile."
    :mimall                  "Tutte le tipologie"
    :mimdev                  "Strumenti di sviluppo"
    :mimo                    "Strumenti Office tools"
    :mimprod                 "Production tools"
    :name                    "Nome"
    :no-sws-found            "Non ho trovato software: prova con un'altra ricerca?"
    :on-comptoir             "Su Comptoir du libre"
    :on-framalibre           "Su Framalibre: "
    :one-sw                  "software"
    :read-the-docs           "Leggi la documentazione"
    :recommended             "Consigliato"
    :recommended_version     "Versione consigliata e in uso"
    :sort-alpha              "A-Z <=> Z-A"
    :source-code-available   ", codice sorgente disponibile "
    :submit                  "Inviato"
    :sws                     "software"
    :tested                  "Sotto osservazione"
    :version                 "Versione: "
    :warning-testing         "Questo software è attualmente sotto test."
    :website-developed-by    "Sito web realizzato da "
    :why-this-website?       "Perchè questo sito?"
    :your-affiliation        "Tua affiliazione"
    :your-email              "Tuo indirizzo email"
    :your-message            "Messaggio"
    :your-name               "Tuo nome"
    :year                    "Anno"
    }
   })

(def opts {:dict localization})

(defn i [lang input] (tr opts [lang] input))
