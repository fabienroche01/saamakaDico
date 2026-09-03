package com.saamaka.dico.testeurs

import java.util.Locale
import com.saamaka.dico.testeurs.model.TranslationReliability

/**
 * Copy shared by screens added after the original [AppStrings] model.
 * A null Saamaka value deliberately falls back to English and is listed in
 * [saamakaCopyAwaitingValidation], so an uncertain translation is never invented.
 */
enum class UiCopyKey {
    HOME, LEARN, MORE, ENTRIES, VERSION, APP_TAGLINE, WORD_OF_DAY, LISTEN,
    FAVORITE, OPEN_ENTRY, CONTINUE_LEARNING, CONTINUE_ACTION, QUICK_ACCESS,
    CATEGORIES, DISCOVER, SEE_ALL_CATEGORIES, VERIFY_TODAY, AVAILABLE_WORDS,
    QUICK_SEARCH_HINT, SEARCH_ACTION, LANGUAGE_MENU, ACCESS_MENU,
    PREMIUM_TITLE, PREMIUM_INTRO, MONTHLY, ANNUAL, LOADING_PLAY_PRICES,
    PREMIUM_ACTIVE_MESSAGE, PURCHASE_PENDING, CHECKING_PURCHASES,
    PLAY_UNAVAILABLE, RETRY, PREMIUM_ACTIVE, CONTINUE_PLAN, RESTORE_PURCHASES,
    MANAGE_SUBSCRIPTION, LOADING_PRICE, PRICE_UNAVAILABLE, MONTHLY_DISCLOSURE,
    ANNUAL_TRIAL_DISCLOSURE, ANNUAL_DISCLOSURE, PLAN_UNAVAILABLE,
    BACK_DESCRIPTION, HOME_DESCRIPTION, SEARCH_DESCRIPTION, FAVORITES_DESCRIPTION,
    LEARN_DESCRIPTION, MORE_DESCRIPTION, HERITAGE_TAGLINE, LEARN_UNDERSTAND_PRESERVE,
    SEARCH_ALL_LANGUAGES_HINT, CLEAR, QUICK_ACCESS_TITLE, EXPLORE_BY_THEME, SEE_MY_WORDS,
    RELIABILITY_HIGH, TRANSLATE_THIS_PHRASE, NO_COMPLETE_EXPRESSION, TRANSLATE_PHRASE,
    TRANSLATION_UNAVAILABLE, TO_COMPLETE, SEARCH_OR_TRANSLATE, SEARCH_OR_TRANSLATE_HINT,
    SEARCHING, DICTIONARY_RESULTS, MISSING_TRANSLATION, CREATE_ACCOUNT_FOR_TRIALS,
    NO_TRIALS_LEFT, PREMIUM_TRANSLATION_UNAVAILABLE, VALIDATED_RULE_TRANSLATION,
    INCOMPLETE_SUGGESTION, RELIABILITY, SHOW_DETAILS, HIDE_DETAILS, OTHER_POSSIBILITIES,
    ITEMS_TO_REVIEW, NEW_SEARCH, COPY_ACTION, SHARE_ACTION, CREATE_FREE_ACCOUNT,
    FREE_TRANSLATION_TRIALS, CREATE_MY_FREE_ACCOUNT, PREMIUM_BENEFITS,
    DISCOVER_PREMIUM, REMAINING_TRANSLATIONS, FREE_ACCOUNT_TRIAL_INFO,
    START_TRANSLATION, PREMIUM_UNLIMITED, TRANSLATE_NOW, TEST_TRANSLATOR_ENABLED,
    TEST_TRANSLATION, TRANSLATE_A_PHRASE, FRENCH_PHRASE, WRITE_PHRASE_HINT,
    NO_RESULT_RETRY, TRANSLATION_FAILED_RETRY, TRANSLATING, NEW_PHRASE,
    LOCAL_INCOMPLETE_SUGGESTION, FRENCH_SUGGESTION, LOCAL_DICTIONARY_SUGGESTION,
    TRANSLATION_COPIED, SHARE_TRANSLATION, BILLING_DISCONNECTED,
    SUBSCRIPTION_UNAVAILABLE, BILLING_PRODUCT_ERROR, PURCHASE_CANCELED,
    PURCHASE_NOT_ACKNOWLEDGED, NO_OTHER_WORD, MISSION_SEARCH, EMPTY_MISSION,
    NO_FILTER_RESULT, NO_CATEGORY_WORD, NO_FAVORITE, REMOVED_FROM_FAVORITES,
    REMOVED_FROM_HISTORY, WORD_REVIEW, LEARN_AT_YOUR_PACE, REVIEW, PERFECT_STREAK,
    CHOOSE_ACTIVITY, WHAT_DOES_WORD_MEAN, GOOD_ANSWER, WRONG_ANSWER, CORRECT_ANSWER,
    REVIEW_VOCABULARY, I_KNOW_REVIEW, REVIEW_ACTION, SHORT_EXPRESSIONS,
    CHOOSE_SAAMAKA_THEN_TRANSLATION, CHOOSE_SAAMAKA_FIRST, GAME_FINISHED,
    SUCCESSFUL_MATCHES, ERRORS_COUNT, EXCELLENT_NO_ERROR, VERY_GOOD,
    WELL_DONE, NEW_GAME, CLOSE, MORE_SUBTITLE, TRANSLATE_SUBTITLE,
    CATEGORIES_SUBTITLE, HISTORY_SUBTITLE, TESTER_MISSION, TESTER_MISSION_SUBTITLE,
    ABOUT, FAVORITES_EMPTY_HELP, CLEAR_HISTORY_TITLE, CLEAR_HISTORY_MESSAGE,
    CLEAR_ALL, RECENT_WORDS, EMPTY_HISTORY_HELP, SAVED_IDENTITY,
    WORK_SUMMARY, CLEAR_LOCAL_CORRECTIONS, CLEAR_LOCAL_VALIDATIONS,
    MICROPHONE_DENIED, REMOVE_FROM_HISTORY, EXPLORE_VOCABULARY,
    THEMES_AND_WORDS, WORDS_TO_DISCOVER, EDIT_CORRECTION_HELP,
    TESTER_IDENTITY_SAVED, CORRECTION_COMMENT_HINT, MY_FAVORITES, FAVORITES_COUNT,
    NO_RECENT_WORDS, WORD_COUNT, VALIDATED_BY, MISSION_OF, MISSION_GUIDANCE,
    COMPLETED_COUNT, CHOOSE_CATEGORY, CATEGORY_VALUE, NO_SEARCH_FILTER_RESULT,
    SAAMAKA_TO_COMPLETE, CORRECT_MATCHES, LOCAL_CORRECTIONS_CLEARED,
    LOCAL_VALIDATIONS_CLEARED, TESTER_NAME_USAGE, LISTEN_PRONUNCIATION,
    ENTRY_ALREADY_VALIDATED, MISSING_BEFORE_VALIDATE, EXPORT_NOT_FOUND,
    EXPORT_INVALID, EXPORT_CORRUPT, SHARE_PREPARATION_FAILED, SHARE_RETRY_ALLOWED,
    SHARE_HISTORY_FAILED, I_KNOW, EXPLORE, SUBSCRIPTION_ACTIVE, MONTHLY_OR_ANNUAL,
    COPYRIGHT, WELCOME, ENTER_NAME_INTRO, EXPORT_NOT_ZIP, SHARE_TESTER_WORK,
    OPEN_SHARE_FAILED, EXACT_DICTIONARY_MATCH, LOCAL_CORRECTION, CLOSE_EXPRESSION,
    CLOSE_EXPRESSION_EXTRA, RELIABILITY_MEDIUM, RELIABILITY_LOW, LOCAL_SUGGESTION,
    EXPORT_MY_WORK, REVIEW_ACTIVITY, MANAGE_SUBSCRIPTION_FAILED, QUIZ,
    QUICK_QUESTIONS, REVIEW_WORDS, PHRASES, USEFUL_EXPRESSIONS, GAMES, MATCH_WORDS,
    AUDIO_NOT_FOUND, AUDIO_READ_FAILED, UNKNOWN_ERROR, AUDIO_EMPTY,
    PLAYING_PRONUNCIATION, AUDIO_ERROR, ALL, DOUBTFUL, COMPLETED,
    MISSING_TRANSLATION_STATUS, NEW_STATUS, VALIDATED_STATUS,
    MISSING_SAAMAKA_TRANSLATION, MISSING_FRENCH_TRANSLATION, WORDS_AND_EXPRESSIONS,
    MY_PROGRESS, LEARNING_PATH, PATH_PERCENT, TODAY_QUIZ, QUESTION_NUMBER,
    SCORE_VALUE, TEST_KNOWLEDGE, NEXT, MATCH_WORDS_TITLE, MY_STATISTICS,
    GAMES_COUNT, ERRORS_STAT, PERFECT_GAMES, GAME_SCORE, TESTER_SPACE,
    TESTER_EXPORTS, CORRECTIONS_SPACE, TESTER_WORK_GUIDANCE, LOCAL_VALIDATIONS_COUNT,
    INSTRUCTIONS, AVAILABLE_LANGUAGES, SPEAKER_VALUE, EXPLORE_ACTION, MY_WORDS,
    FIND_MY_WORDS, TODAY, PROGRESSION, LEARNING_ACTIVITIES, TRANSLATOR_INTRO,
    UNLIMITED_SENTENCE_TRANSLATIONS, TESTER_MODE, CATEGORIES_COUNT,
    APPROXIMATE_REVIEW, ACTIVATE_TESTER_MODE, DEACTIVATE_TESTER_MODE
}

private data class UiCopyValue(
    val french: String,
    val english: String,
    val dutch: String,
    val saamaka: String? = null
)

private val uiCopy = mapOf(
    UiCopyKey.HOME to UiCopyValue("Accueil", "Home", "Start", "Wosu"),
    UiCopyKey.LEARN to UiCopyValue("Apprendre", "Learn", "Leren"),
    UiCopyKey.MORE to UiCopyValue("Plus", "More", "Moro"),
    UiCopyKey.ENTRIES to UiCopyValue("%d entrées", "%d entries", "%d vermeldingen"),
    UiCopyKey.VERSION to UiCopyValue("version %s", "version %s", "versie %s", "versi %s"),
    UiCopyKey.APP_TAGLINE to UiCopyValue("Les mots qui nous relient", "Words that connect us", "Woorden die ons verbinden"),
    UiCopyKey.WORD_OF_DAY to UiCopyValue("Mot du jour", "Word of the day", "Woord van de dag"),
    UiCopyKey.LISTEN to UiCopyValue("Écouter", "Listen", "Luisteren", "Arki"),
    UiCopyKey.FAVORITE to UiCopyValue("Favori", "Favorite", "Favoriet", "Lobi"),
    UiCopyKey.OPEN_ENTRY to UiCopyValue("Voir la fiche", "Open entry", "Vermelding openen"),
    UiCopyKey.CONTINUE_LEARNING to UiCopyValue("Continuer l’apprentissage", "Continue learning", "Verder leren"),
    UiCopyKey.CONTINUE_ACTION to UiCopyValue("Continuer", "Continue", "Doorgaan", "Go doo"),
    UiCopyKey.QUICK_ACCESS to UiCopyValue("Accès rapides", "Quick access", "Snelle toegang"),
    UiCopyKey.CATEGORIES to UiCopyValue("Catégories", "Categories", "Categorieën"),
    UiCopyKey.DISCOVER to UiCopyValue("À découvrir", "Discover", "Ontdekken"),
    UiCopyKey.SEE_ALL_CATEGORIES to UiCopyValue("Voir toutes les catégories", "See all categories", "Alle categorieën bekijken"),
    UiCopyKey.VERIFY_TODAY to UiCopyValue("À vérifier aujourd’hui", "To review today", "Vandaag te controleren"),
    UiCopyKey.AVAILABLE_WORDS to UiCopyValue("%d mot(s) disponible(s)", "%d word(s) available", "%d woord(en) beschikbaar"),
    UiCopyKey.QUICK_SEARCH_HINT to UiCopyValue("Rechercher un mot ou une expression…", "Search for a word or phrase…", "Zoek een woord of uitdrukking…"),
    UiCopyKey.SEARCH_ACTION to UiCopyValue("Rechercher", "Search", "Zoeken", "Suku"),
    UiCopyKey.LANGUAGE_MENU to UiCopyValue("Choisir la langue de l’application", "Choose app language", "App-taal kiezen"),
    UiCopyKey.ACCESS_MENU to UiCopyValue("Choisir le type de compte", "Choose account type", "Accounttype kiezen"),
    UiCopyKey.PREMIUM_TITLE to UiCopyValue("DicoSaam Premium", "DicoSaam Premium", "DicoSaam Premium", "DicoSaam Premium"),
    UiCopyKey.PREMIUM_INTRO to UiCopyValue("Choisissez votre formule. Les prix affichés sont fournis par Google Play.", "Choose your plan. Displayed prices are provided by Google Play.", "Kies uw abonnement. De getoonde prijzen komen van Google Play."),
    UiCopyKey.MONTHLY to UiCopyValue("Mensuel", "Monthly", "Maandelijks"),
    UiCopyKey.ANNUAL to UiCopyValue("Annuel", "Annual", "Jaarlijks"),
    UiCopyKey.LOADING_PLAY_PRICES to UiCopyValue("Chargement des tarifs Google Play…", "Loading Google Play prices…", "Google Play-prijzen laden…"),
    UiCopyKey.PREMIUM_ACTIVE_MESSAGE to UiCopyValue("Votre abonnement Premium est actif.", "Your Premium subscription is active.", "Uw Premium-abonnement is actief."),
    UiCopyKey.PURCHASE_PENDING to UiCopyValue("Achat en attente de confirmation par Google Play.", "Purchase awaiting confirmation from Google Play.", "Aankoop wacht op bevestiging door Google Play."),
    UiCopyKey.CHECKING_PURCHASES to UiCopyValue("Vérification des achats en cours…", "Checking purchases…", "Aankopen controleren…"),
    UiCopyKey.PLAY_UNAVAILABLE to UiCopyValue("Google Play est temporairement indisponible.", "Google Play is temporarily unavailable.", "Google Play is tijdelijk niet beschikbaar."),
    UiCopyKey.RETRY to UiCopyValue("Réessayer", "Try again", "Opnieuw proberen"),
    UiCopyKey.PREMIUM_ACTIVE to UiCopyValue("Premium actif", "Premium active", "Premium actief"),
    UiCopyKey.CONTINUE_PLAN to UiCopyValue("Continuer avec cette formule", "Continue with this plan", "Doorgaan met dit abonnement"),
    UiCopyKey.RESTORE_PURCHASES to UiCopyValue("Restaurer les achats", "Restore purchases", "Aankopen herstellen"),
    UiCopyKey.MANAGE_SUBSCRIPTION to UiCopyValue("Gérer mon abonnement", "Manage my subscription", "Mijn abonnement beheren"),
    UiCopyKey.LOADING_PRICE to UiCopyValue("Chargement du tarif…", "Loading price…", "Prijs laden…"),
    UiCopyKey.PRICE_UNAVAILABLE to UiCopyValue("Tarif Google Play indisponible", "Google Play price unavailable", "Google Play-prijs niet beschikbaar"),
    UiCopyKey.MONTHLY_DISCLOSURE to UiCopyValue("%s par mois, renouvellement automatique, résiliable à tout moment.", "%s per month, auto-renewing, cancel anytime.", "%s per maand, automatische verlenging, altijd opzegbaar."),
    UiCopyKey.ANNUAL_TRIAL_DISCLOSURE to UiCopyValue("7 jours gratuits, puis %s par an, renouvellement automatique, résiliable à tout moment.", "7 days free, then %s per year, auto-renewing, cancel anytime.", "7 dagen gratis, daarna %s per jaar, automatische verlenging, altijd opzegbaar."),
    UiCopyKey.ANNUAL_DISCLOSURE to UiCopyValue("%s par an, renouvellement automatique, résiliable à tout moment.", "%s per year, auto-renewing, cancel anytime.", "%s per jaar, automatische verlenging, altijd opzegbaar."),
    UiCopyKey.PLAN_UNAVAILABLE to UiCopyValue("Cette formule est temporairement indisponible.", "This plan is temporarily unavailable.", "Dit abonnement is tijdelijk niet beschikbaar."),
    UiCopyKey.BACK_DESCRIPTION to UiCopyValue("Retour", "Back", "Terug", "Baka"),
    UiCopyKey.HOME_DESCRIPTION to UiCopyValue("Ouvrir l’accueil", "Open home", "Start openen"),
    UiCopyKey.SEARCH_DESCRIPTION to UiCopyValue("Ouvrir la recherche", "Open search", "Zoeken openen"),
    UiCopyKey.FAVORITES_DESCRIPTION to UiCopyValue("Ouvrir les favoris", "Open favorites", "Favorieten openen"),
    UiCopyKey.LEARN_DESCRIPTION to UiCopyValue("Ouvrir l’apprentissage", "Open learning", "Leren openen"),
    UiCopyKey.MORE_DESCRIPTION to UiCopyValue("Ouvrir le menu Plus", "Open More menu", "Menu Meer openen")
    ,UiCopyKey.HERITAGE_TAGLINE to UiCopyValue("Notre langue, notre patrimoine", "Our language, our heritage", "Onze taal, ons erfgoed")
    ,UiCopyKey.LEARN_UNDERSTAND_PRESERVE to UiCopyValue("Apprendre • Comprendre • Préserver", "Learn • Understand • Preserve", "Leren • Begrijpen • Behouden")
    ,UiCopyKey.SEARCH_ALL_LANGUAGES_HINT to UiCopyValue("Rechercher en Saamaka, Français, English ou Nederlands", "Search in Saamaka, French, English or Dutch", "Zoeken in Saamaka, Frans, Engels of Nederlands")
    ,UiCopyKey.CLEAR to UiCopyValue("Effacer", "Clear", "Wissen")
    ,UiCopyKey.QUICK_ACCESS_TITLE to UiCopyValue("Accès rapide", "Quick access", "Snelle toegang")
    ,UiCopyKey.EXPLORE_BY_THEME to UiCopyValue("Explorer par thème", "Browse by theme", "Verkennen op thema")
    ,UiCopyKey.SEE_MY_WORDS to UiCopyValue("Voir mes mots →", "See my words →", "Mijn woorden bekijken →")
    ,UiCopyKey.RELIABILITY_HIGH to UiCopyValue("Fiabilité : Élevée", "Reliability: High", "Betrouwbaarheid: Hoog")
    ,UiCopyKey.TRANSLATE_THIS_PHRASE to UiCopyValue("Traduire cette phrase", "Translate this phrase", "Deze zin vertalen")
    ,UiCopyKey.NO_COMPLETE_EXPRESSION to UiCopyValue("Aucune expression complète trouvée dans le dictionnaire", "No complete expression found in the dictionary", "Geen volledige uitdrukking gevonden in het woordenboek")
    ,UiCopyKey.TRANSLATE_PHRASE to UiCopyValue("Traduire la phrase", "Translate the phrase", "De zin vertalen")
    ,UiCopyKey.TRANSLATION_UNAVAILABLE to UiCopyValue("Traduction non disponible", "Translation unavailable", "Vertaling niet beschikbaar")
    ,UiCopyKey.TO_COMPLETE to UiCopyValue("À compléter", "To complete", "Aan te vullen")
    ,UiCopyKey.SEARCH_OR_TRANSLATE to UiCopyValue("Rechercher ou traduire", "Search or translate", "Zoeken of vertalen")
    ,UiCopyKey.SEARCH_OR_TRANSLATE_HINT to UiCopyValue("Écris un mot, une expression ou une phrase", "Enter a word, expression or sentence", "Typ een woord, uitdrukking of zin")
    ,UiCopyKey.SEARCHING to UiCopyValue("Recherche…", "Searching…", "Zoeken…")
    ,UiCopyKey.DICTIONARY_RESULTS to UiCopyValue("Résultats du dictionnaire", "Dictionary results", "Woordenboekresultaten")
    ,UiCopyKey.MISSING_TRANSLATION to UiCopyValue("Traduction manquante", "Missing translation", "Ontbrekende vertaling")
    ,UiCopyKey.CREATE_ACCOUNT_FOR_TRIALS to UiCopyValue("Créez un compte pour accéder aux essais de traduction de phrase.", "Create an account to access phrase translation trials.", "Maak een account aan voor proefvertalingen van zinnen.")
    ,UiCopyKey.NO_TRIALS_LEFT to UiCopyValue("Aucun essai de traduction Premium restant.", "No Premium translation trials remaining.", "Geen Premium-proefvertalingen meer beschikbaar.")
    ,UiCopyKey.PREMIUM_TRANSLATION_UNAVAILABLE to UiCopyValue("Traduction Premium indisponible.", "Premium translation unavailable.", "Premium-vertaling niet beschikbaar.")
    ,UiCopyKey.VALIDATED_RULE_TRANSLATION to UiCopyValue("Traduction construite avec une règle validée", "Translation built using a validated rule", "Vertaling opgebouwd met een gevalideerde regel")
    ,UiCopyKey.INCOMPLETE_SUGGESTION to UiCopyValue("Proposition incomplète", "Incomplete suggestion", "Onvolledig voorstel")
    ,UiCopyKey.RELIABILITY to UiCopyValue("Fiabilité : %s", "Reliability: %s", "Betrouwbaarheid: %s")
    ,UiCopyKey.SHOW_DETAILS to UiCopyValue("Voir les détails", "Show details", "Details bekijken")
    ,UiCopyKey.HIDE_DETAILS to UiCopyValue("Masquer les détails", "Hide details", "Details verbergen")
    ,UiCopyKey.OTHER_POSSIBILITIES to UiCopyValue("Autres possibilités : %s", "Other possibilities: %s", "Andere mogelijkheden: %s")
    ,UiCopyKey.ITEMS_TO_REVIEW to UiCopyValue("Éléments à vérifier : %s", "Items to review: %s", "Te controleren onderdelen: %s")
    ,UiCopyKey.NEW_SEARCH to UiCopyValue("Nouvelle recherche / Nouveau texte", "New search / New text", "Nieuwe zoekopdracht / Nieuwe tekst")
    ,UiCopyKey.COPY_ACTION to UiCopyValue("Copier", "Copy", "Kopiëren", "Kopi")
    ,UiCopyKey.SHARE_ACTION to UiCopyValue("Partager", "Share", "Delen", "Deli")
    ,UiCopyKey.CREATE_FREE_ACCOUNT to UiCopyValue("Créez votre compte gratuitement", "Create your free account", "Maak uw gratis account aan")
    ,UiCopyKey.FREE_TRANSLATION_TRIALS to UiCopyValue("Profitez de 3 traductions de phrases ou textes complets offertes.", "Enjoy 3 free translations of full sentences or texts.", "Probeer 3 gratis vertalingen van volledige zinnen of teksten.")
    ,UiCopyKey.CREATE_MY_FREE_ACCOUNT to UiCopyValue("Créer mon compte gratuitement", "Create my free account", "Mijn gratis account aanmaken")
    ,UiCopyKey.PREMIUM_BENEFITS to UiCopyValue("Traductions illimitées, textes plus longs, apprentissage, quiz et fonctions avancées.", "Unlimited translations, longer texts, learning, quizzes and advanced features.", "Onbeperkte vertalingen, langere teksten, leren, quizzen en geavanceerde functies.")
    ,UiCopyKey.DISCOVER_PREMIUM to UiCopyValue("Découvrir Premium", "Discover Premium", "Premium ontdekken")
    ,UiCopyKey.REMAINING_TRANSLATIONS to UiCopyValue("%d traduction(s) restante(s)", "%d translation(s) remaining", "%d vertaling(en) resterend")
    ,UiCopyKey.FREE_ACCOUNT_TRIAL_INFO to UiCopyValue("Votre compte gratuit permet d'essayer le traducteur Saamaka.", "Your free account lets you try the Saamaka translator.", "Met uw gratis account kunt u de Saamaka-vertaler proberen.")
    ,UiCopyKey.START_TRANSLATION to UiCopyValue("Commencer une traduction", "Start a translation", "Een vertaling starten")
    ,UiCopyKey.PREMIUM_UNLIMITED to UiCopyValue("Premium : traductions illimitées", "Premium: unlimited translations", "Premium: onbeperkte vertalingen")
    ,UiCopyKey.TRANSLATE_NOW to UiCopyValue("Traduire maintenant", "Translate now", "Nu vertalen")
    ,UiCopyKey.TEST_TRANSLATOR_ENABLED to UiCopyValue("Traducteur complet activé pour les tests.", "Full translator enabled for testing.", "Volledige vertaler ingeschakeld voor tests.")
    ,UiCopyKey.TEST_TRANSLATION to UiCopyValue("Tester une traduction", "Test a translation", "Een vertaling testen")
    ,UiCopyKey.TRANSLATE_A_PHRASE to UiCopyValue("Traduire une phrase", "Translate a sentence", "Een zin vertalen")
    ,UiCopyKey.FRENCH_PHRASE to UiCopyValue("Phrase en français", "Sentence in French", "Zin in het Frans")
    ,UiCopyKey.WRITE_PHRASE_HINT to UiCopyValue("Écrivez votre phrase ici…", "Enter your sentence here…", "Typ uw zin hier…")
    ,UiCopyKey.NO_RESULT_RETRY to UiCopyValue("Aucun résultat disponible. Réessayez.", "No result available. Try again.", "Geen resultaat beschikbaar. Probeer opnieuw.")
    ,UiCopyKey.TRANSLATION_FAILED_RETRY to UiCopyValue("La traduction a échoué. Réessayez.", "Translation failed. Try again.", "Vertalen mislukt. Probeer opnieuw.")
    ,UiCopyKey.TRANSLATING to UiCopyValue("Traduction…", "Translating…", "Vertalen…")
    ,UiCopyKey.NEW_PHRASE to UiCopyValue("Nouvelle phrase", "New sentence", "Nieuwe zin")
    ,UiCopyKey.LOCAL_INCOMPLETE_SUGGESTION to UiCopyValue("Proposition locale incomplète — à vérifier", "Incomplete local suggestion — review required", "Onvolledig lokaal voorstel — controleren")
    ,UiCopyKey.FRENCH_SUGGESTION to UiCopyValue("Proposition française", "French suggestion", "Frans voorstel")
    ,UiCopyKey.LOCAL_DICTIONARY_SUGGESTION to UiCopyValue("Proposition locale construite à partir du dictionnaire.", "Local suggestion built from the dictionary.", "Lokaal voorstel opgebouwd uit het woordenboek.")
    ,UiCopyKey.TRANSLATION_COPIED to UiCopyValue("Traduction copiée", "Translation copied", "Vertaling gekopieerd")
    ,UiCopyKey.SHARE_TRANSLATION to UiCopyValue("Partager la traduction", "Share translation", "Vertaling delen")
    ,UiCopyKey.BILLING_DISCONNECTED to UiCopyValue("Google Play Billing n’est pas connecté.", "Google Play Billing is not connected.", "Google Play Billing is niet verbonden.")
    ,UiCopyKey.SUBSCRIPTION_UNAVAILABLE to UiCopyValue("L’abonnement DicoSaam Premium n’est pas disponible pour ce compte Google Play.", "The DicoSaam Premium subscription is unavailable for this Google Play account.", "Het DicoSaam Premium-abonnement is niet beschikbaar voor dit Google Play-account.")
    ,UiCopyKey.BILLING_PRODUCT_ERROR to UiCopyValue("Google Play n’a pas pu récupérer l’abonnement.", "Google Play could not retrieve the subscription.", "Google Play kon het abonnement niet ophalen.")
    ,UiCopyKey.PURCHASE_CANCELED to UiCopyValue("Achat annulé", "Purchase canceled", "Aankoop geannuleerd")
    ,UiCopyKey.PURCHASE_NOT_ACKNOWLEDGED to UiCopyValue("Achat non acquitté", "Purchase not acknowledged", "Aankoop niet bevestigd")
    ,UiCopyKey.NO_OTHER_WORD to UiCopyValue("Aucun autre mot à vérifier", "No other word to review", "Geen ander woord om te controleren")
    ,UiCopyKey.MISSION_SEARCH to UiCopyValue("Rechercher dans la mission", "Search in mission", "Zoeken in missie")
    ,UiCopyKey.EMPTY_MISSION to UiCopyValue("Mission vide", "Empty mission", "Lege missie")
    ,UiCopyKey.NO_FILTER_RESULT to UiCopyValue("Aucun résultat", "No result", "Geen resultaat")
    ,UiCopyKey.NO_CATEGORY_WORD to UiCopyValue("Aucun mot n'est disponible pour cette catégorie.", "No word is available for this category.", "Er is geen woord beschikbaar voor deze categorie.")
    ,UiCopyKey.NO_FAVORITE to UiCopyValue("Aucun favori", "No favorites", "Geen favorieten")
    ,UiCopyKey.REMOVED_FROM_FAVORITES to UiCopyValue("Retiré des favoris", "Removed from favorites", "Uit favorieten verwijderd")
    ,UiCopyKey.REMOVED_FROM_HISTORY to UiCopyValue("Retiré de l’historique", "Removed from history", "Uit geschiedenis verwijderd")
    ,UiCopyKey.WORD_REVIEW to UiCopyValue("Révision des mots", "Word review", "Woorden herhalen")
    ,UiCopyKey.LEARN_AT_YOUR_PACE to UiCopyValue("Progresse à ton rythme, un mot après l’autre", "Learn at your own pace, one word at a time", "Leer in je eigen tempo, woord voor woord")
    ,UiCopyKey.REVIEW to UiCopyValue("À revoir", "Review", "Herhalen")
    ,UiCopyKey.PERFECT_STREAK to UiCopyValue("Série parfaite", "Perfect streak", "Perfecte reeks")
    ,UiCopyKey.CHOOSE_ACTIVITY to UiCopyValue("Choisir une activité", "Choose an activity", "Kies een activiteit")
    ,UiCopyKey.WHAT_DOES_WORD_MEAN to UiCopyValue("Que signifie ce mot ?", "What does this word mean?", "Wat betekent dit woord?")
    ,UiCopyKey.GOOD_ANSWER to UiCopyValue("✅ Bonne réponse", "✅ Correct answer", "✅ Goed antwoord")
    ,UiCopyKey.WRONG_ANSWER to UiCopyValue("❌ Mauvaise réponse", "❌ Wrong answer", "❌ Fout antwoord")
    ,UiCopyKey.CORRECT_ANSWER to UiCopyValue("✅ Réponse correcte : %s", "✅ Correct answer: %s", "✅ Juiste antwoord: %s")
    ,UiCopyKey.REVIEW_VOCABULARY to UiCopyValue("Révise ton vocabulaire Saamaka", "Review your Saamaka vocabulary", "Herhaal je Saamaka-woordenschat")
    ,UiCopyKey.I_KNOW_REVIEW to UiCopyValue("✅ Je connais : %d • 🔁 À revoir : %d", "✅ I know: %d • 🔁 Review: %d", "✅ Ik ken: %d • 🔁 Herhalen: %d")
    ,UiCopyKey.REVIEW_ACTION to UiCopyValue("À revoir", "Review", "Herhalen")
    ,UiCopyKey.SHORT_EXPRESSIONS to UiCopyValue("Découvre des expressions courtes en Saamaka", "Discover short expressions in Saamaka", "Ontdek korte uitdrukkingen in het Saamaka")
    ,UiCopyKey.CHOOSE_SAAMAKA_THEN_TRANSLATION to UiCopyValue("Choisis un mot Saamaka puis sa traduction", "Choose a Saamaka word, then its translation", "Kies een Saamaka-woord en daarna de vertaling")
    ,UiCopyKey.CHOOSE_SAAMAKA_FIRST to UiCopyValue("ℹ️ Choisis d'abord un mot Saamaka.", "ℹ️ Choose a Saamaka word first.", "ℹ️ Kies eerst een Saamaka-woord.")
    ,UiCopyKey.GAME_FINISHED to UiCopyValue("🎉 Partie terminée !", "🎉 Game over!", "🎉 Spel afgelopen!")
    ,UiCopyKey.SUCCESSFUL_MATCHES to UiCopyValue("✅ %d/%d associations réussies", "✅ %d/%d successful matches", "✅ %d/%d juiste combinaties")
    ,UiCopyKey.ERRORS_COUNT to UiCopyValue("❌ %d erreur(s)", "❌ %d error(s)", "❌ %d fout(en)")
    ,UiCopyKey.EXCELLENT_NO_ERROR to UiCopyValue("Excellent ! Aucune erreur 👏", "Excellent! No errors 👏", "Uitstekend! Geen fouten 👏")
    ,UiCopyKey.VERY_GOOD to UiCopyValue("Très bien ! Continue comme ça 👍", "Very good! Keep going 👍", "Heel goed! Ga zo door 👍")
    ,UiCopyKey.WELL_DONE to UiCopyValue("Bien joué ! Encore un peu d'entraînement 💪", "Well done! A little more practice 💪", "Goed gedaan! Nog wat oefenen 💪")
    ,UiCopyKey.NEW_GAME to UiCopyValue("Nouvelle partie", "New game", "Nieuw spel")
    ,UiCopyKey.CLOSE to UiCopyValue("Fermer", "Close", "Sluiten")
    ,UiCopyKey.MORE_SUBTITLE to UiCopyValue("Outils, historique et espace testeur", "Tools, history and tester area", "Hulpmiddelen, geschiedenis en testomgeving")
    ,UiCopyKey.TRANSLATE_SUBTITLE to UiCopyValue("Français ↔ Saamaka", "French ↔ Saamaka", "Frans ↔ Saamaka")
    ,UiCopyKey.CATEGORIES_SUBTITLE to UiCopyValue("Explorer les mots par thème", "Browse words by theme", "Woorden per thema verkennen")
    ,UiCopyKey.HISTORY_SUBTITLE to UiCopyValue("Retrouver les mots consultés", "Find viewed words", "Bekeken woorden terugvinden")
    ,UiCopyKey.TESTER_MISSION to UiCopyValue("Mission testeur", "Tester mission", "Testersmissie")
    ,UiCopyKey.TESTER_MISSION_SUBTITLE to UiCopyValue("Valider et corriger les mots", "Validate and correct words", "Woorden valideren en corrigeren")
    ,UiCopyKey.ABOUT to UiCopyValue("À propos", "About", "Over")
    ,UiCopyKey.FAVORITES_EMPTY_HELP to UiCopyValue("Ajoute des mots à tes favoris pour les retrouver ici.", "Add words to favorites to find them here.", "Voeg woorden toe aan favorieten om ze hier terug te vinden.")
    ,UiCopyKey.CLEAR_HISTORY_TITLE to UiCopyValue("Effacer l’historique ?", "Clear history?", "Geschiedenis wissen?")
    ,UiCopyKey.CLEAR_HISTORY_MESSAGE to UiCopyValue("Tous les mots consultés seront supprimés.", "All viewed words will be removed.", "Alle bekeken woorden worden verwijderd.")
    ,UiCopyKey.CLEAR_ALL to UiCopyValue("Tout effacer", "Clear all", "Alles wissen")
    ,UiCopyKey.RECENT_WORDS to UiCopyValue("%d mot(s) récent(s)", "%d recent word(s)", "%d recent(e) woord(en)")
    ,UiCopyKey.EMPTY_HISTORY_HELP to UiCopyValue("Les mots que tu consulteras apparaîtront ici.", "Words you view will appear here.", "Woorden die je bekijkt verschijnen hier.")
    ,UiCopyKey.SAVED_IDENTITY to UiCopyValue("Identité enregistrée et protégée", "Saved and protected identity", "Opgeslagen en beveiligde identiteit")
    ,UiCopyKey.WORK_SUMMARY to UiCopyValue("Résumé du travail", "Work summary", "Werkoverzicht")
    ,UiCopyKey.CLEAR_LOCAL_CORRECTIONS to UiCopyValue("Effacer les corrections locales", "Clear local corrections", "Lokale correcties wissen")
    ,UiCopyKey.CLEAR_LOCAL_VALIDATIONS to UiCopyValue("Effacer les validations locales", "Clear local validations", "Lokale validaties wissen")
    ,UiCopyKey.MICROPHONE_DENIED to UiCopyValue("Permission microphone refusée", "Microphone permission denied", "Microfoontoestemming geweigerd")
    ,UiCopyKey.REMOVE_FROM_HISTORY to UiCopyValue("Retirer de l’historique", "Remove from history", "Uit geschiedenis verwijderen")
    ,UiCopyKey.EXPLORE_VOCABULARY to UiCopyValue("Explore le vocabulaire par thème", "Browse vocabulary by theme", "Verken woordenschat per thema")
    ,UiCopyKey.THEMES_AND_WORDS to UiCopyValue("%d thèmes • %d mots et expressions", "%d themes • %d words and expressions", "%d thema's • %d woorden en uitdrukkingen")
    ,UiCopyKey.WORDS_TO_DISCOVER to UiCopyValue("%d mot(s) à découvrir", "%d word(s) to discover", "%d woord(en) te ontdekken")
    ,UiCopyKey.EDIT_CORRECTION_HELP to UiCopyValue("Modifie uniquement les éléments qui doivent être corrigés.", "Edit only the items that need correcting.", "Wijzig alleen de onderdelen die moeten worden gecorrigeerd.")
    ,UiCopyKey.TESTER_IDENTITY_SAVED to UiCopyValue("Identité du testeur enregistrée", "Tester identity saved", "Testeridentiteit opgeslagen")
    ,UiCopyKey.CORRECTION_COMMENT_HINT to UiCopyValue("Explique brièvement la correction si nécessaire", "Briefly explain the correction if needed", "Licht de correctie indien nodig kort toe")
    ,UiCopyKey.MY_FAVORITES to UiCopyValue("Mes favoris", "My favorites", "Mijn favorieten")
    ,UiCopyKey.FAVORITES_COUNT to UiCopyValue("%d favori(s)", "%d favorite(s)", "%d favoriet(en)")
    ,UiCopyKey.NO_RECENT_WORDS to UiCopyValue("Aucun mot consulté récemment", "No recently viewed words", "Geen recent bekeken woorden")
    ,UiCopyKey.WORD_COUNT to UiCopyValue("%d mot(s)", "%d word(s)", "%d woord(en)")
    ,UiCopyKey.VALIDATED_BY to UiCopyValue("Mot validé par %s", "Word validated by %s", "Woord gevalideerd door %s")
    ,UiCopyKey.MISSION_OF to UiCopyValue("Mission de %s", "%s's mission", "Missie van %s")
    ,UiCopyKey.MISSION_GUIDANCE to UiCopyValue("Valide, corrige et complète les mots de ta mission.", "Validate, correct and complete the words in your mission.", "Valideer, corrigeer en voltooi de woorden in je missie.")
    ,UiCopyKey.COMPLETED_COUNT to UiCopyValue("%d sur %d terminés", "%d of %d completed", "%d van %d voltooid")
    ,UiCopyKey.CHOOSE_CATEGORY to UiCopyValue("Choisir une catégorie", "Choose a category", "Kies een categorie")
    ,UiCopyKey.CATEGORY_VALUE to UiCopyValue("Catégorie : %s", "Category: %s", "Categorie: %s")
    ,UiCopyKey.NO_SEARCH_FILTER_RESULT to UiCopyValue("Aucun mot ne correspond à ce filtre et à cette recherche.", "No word matches this filter and search.", "Geen woord komt overeen met dit filter en deze zoekopdracht.")
    ,UiCopyKey.SAAMAKA_TO_COMPLETE to UiCopyValue("Saamaka : à compléter", "Saamaka: to complete", "Saamaka: aan te vullen")
    ,UiCopyKey.CORRECT_MATCHES to UiCopyValue("✅ Associations réussies : %d", "✅ Successful matches: %d", "✅ Juiste combinaties: %d")
    ,UiCopyKey.LOCAL_CORRECTIONS_CLEARED to UiCopyValue("Corrections locales effacées", "Local corrections cleared", "Lokale correcties gewist")
    ,UiCopyKey.LOCAL_VALIDATIONS_CLEARED to UiCopyValue("Validations effacées", "Validations cleared", "Validaties gewist")
    ,UiCopyKey.TESTER_NAME_USAGE to UiCopyValue("Il sera associé à vos validations, corrections et enregistrements audio.", "It will be associated with your validations, corrections and audio recordings.", "Deze wordt gekoppeld aan uw validaties, correcties en audio-opnamen.")
    ,UiCopyKey.LISTEN_PRONUNCIATION to UiCopyValue("Écouter la prononciation", "Listen to pronunciation", "Naar uitspraak luisteren")
    ,UiCopyKey.ENTRY_ALREADY_VALIDATED to UiCopyValue("Cette entrée est déjà validée. Une seconde validation identique est désactivée.", "This entry is already validated. A second identical validation is disabled.", "Deze vermelding is al gevalideerd. Een tweede identieke validatie is uitgeschakeld.")
    ,UiCopyKey.MISSING_BEFORE_VALIDATE to UiCopyValue("%s. Propose le texte manquant avec Corriger avant de valider.", "%s. Suggest the missing text with Correct before validating.", "%s. Stel de ontbrekende tekst voor via Corrigeren voordat u valideert.")
    ,UiCopyKey.EXPORT_NOT_FOUND to UiCopyValue("Export introuvable. Veuillez recréer l'envoi.", "Export not found. Please recreate it.", "Export niet gevonden. Maak deze opnieuw.")
    ,UiCopyKey.EXPORT_INVALID to UiCopyValue("Export invalide ou vide. Envoi annulé.", "Invalid or empty export. Sharing canceled.", "Ongeldige of lege export. Delen geannuleerd.")
    ,UiCopyKey.EXPORT_CORRUPT to UiCopyValue("L'export est incomplet ou corrompu. Envoi annulé.", "The export is incomplete or corrupt. Sharing canceled.", "De export is onvolledig of beschadigd. Delen geannuleerd.")
    ,UiCopyKey.SHARE_PREPARATION_FAILED to UiCopyValue("Impossible de préparer le fichier pour le partage.", "Could not prepare the file for sharing.", "Het bestand kon niet voor delen worden voorbereid.")
    ,UiCopyKey.SHARE_RETRY_ALLOWED to UiCopyValue("Ce contenu a déjà été ouvert pour partage. Nouvelle tentative autorisée.", "This content was already opened for sharing. A new attempt is allowed.", "Deze inhoud is al geopend om te delen. Een nieuwe poging is toegestaan.")
    ,UiCopyKey.SHARE_HISTORY_FAILED to UiCopyValue("Partage ouvert, mais l'historique local n'a pas pu être enregistré.", "Sharing opened, but local history could not be saved.", "Delen is geopend, maar de lokale geschiedenis kon niet worden opgeslagen.")
    ,UiCopyKey.I_KNOW to UiCopyValue("Je connais", "I know", "Ik ken dit")
    ,UiCopyKey.EXPLORE to UiCopyValue("Explorer", "Explore", "Verkennen")
    ,UiCopyKey.SUBSCRIPTION_ACTIVE to UiCopyValue("Abonnement actif", "Active subscription", "Actief abonnement")
    ,UiCopyKey.MONTHLY_OR_ANNUAL to UiCopyValue("Mensuel ou annuel", "Monthly or annual", "Maandelijks of jaarlijks")
    ,UiCopyKey.COPYRIGHT to UiCopyValue("© 2026 Fabien Roche. Tous droits réservés.", "© 2026 Fabien Roche. All rights reserved.", "© 2026 Fabien Roche. Alle rechten voorbehouden.")
    ,UiCopyKey.WELCOME to UiCopyValue("Bienvenue dans Saamaka Dico", "Welcome to Saamaka Dico", "Welkom bij Saamaka Dico")
    ,UiCopyKey.ENTER_NAME_INTRO to UiCopyValue("Avant de commencer, indiquez votre nom. Il sera associé à vos validations, corrections et enregistrements audio.", "Before you begin, enter your name. It will be associated with your validations, corrections and audio recordings.", "Voer voordat u begint uw naam in. Deze wordt gekoppeld aan uw validaties, correcties en audio-opnamen.")
    ,UiCopyKey.EXPORT_NOT_ZIP to UiCopyValue("Le fichier d'export n'est pas un ZIP valide.", "The export file is not a valid ZIP file.", "Het exportbestand is geen geldig ZIP-bestand.")
    ,UiCopyKey.SHARE_TESTER_WORK to UiCopyValue("Partager le travail du testeur", "Share tester work", "Testerwerk delen")
    ,UiCopyKey.OPEN_SHARE_FAILED to UiCopyValue("Impossible d'ouvrir le partage.", "Could not open sharing.", "Delen kon niet worden geopend.")
    ,UiCopyKey.EXACT_DICTIONARY_MATCH to UiCopyValue("Correspondance exacte du dictionnaire", "Exact dictionary match", "Exacte overeenkomst in het woordenboek")
    ,UiCopyKey.LOCAL_CORRECTION to UiCopyValue("Correction locale", "Local correction", "Lokale correctie")
    ,UiCopyKey.CLOSE_EXPRESSION to UiCopyValue("Expression proche", "Similar expression", "Vergelijkbare uitdrukking")
    ,UiCopyKey.CLOSE_EXPRESSION_EXTRA to UiCopyValue("Expression proche — contient des mots supplémentaires", "Similar expression — contains additional words", "Vergelijkbare uitdrukking — bevat extra woorden")
    ,UiCopyKey.RELIABILITY_MEDIUM to UiCopyValue("Moyenne", "Medium", "Gemiddeld")
    ,UiCopyKey.RELIABILITY_LOW to UiCopyValue("Faible", "Low", "Laag")
    ,UiCopyKey.LOCAL_SUGGESTION to UiCopyValue("Proposition locale", "Local suggestion", "Lokaal voorstel")
    ,UiCopyKey.EXPORT_MY_WORK to UiCopyValue("Exporter mon travail", "Export my work", "Mijn werk exporteren")
    ,UiCopyKey.REVIEW_ACTIVITY to UiCopyValue("Révision", "Review", "Herhaling")
    ,UiCopyKey.MANAGE_SUBSCRIPTION_FAILED to UiCopyValue("Impossible d’ouvrir la gestion des abonnements Google Play.", "Could not open Google Play subscription management.", "Google Play-abonnements konden niet worden geopend.")
    ,UiCopyKey.QUIZ to UiCopyValue("Quiz", "Quiz", "Quiz", "Quiz"),
    UiCopyKey.QUICK_QUESTIONS to UiCopyValue("Questions rapides", "Quick questions", "Snelle vragen"),
    UiCopyKey.REVIEW_WORDS to UiCopyValue("Revoir les mots", "Review words", "Woorden herhalen"),
    UiCopyKey.PHRASES to UiCopyValue("Phrases", "Phrases", "Zinnen"),
    UiCopyKey.USEFUL_EXPRESSIONS to UiCopyValue("Expressions utiles", "Useful expressions", "Nuttige uitdrukkingen"),
    UiCopyKey.GAMES to UiCopyValue("Jeux", "Games", "Spellen"),
    UiCopyKey.MATCH_WORDS to UiCopyValue("Associer les mots", "Match words", "Woorden koppelen")
    ,UiCopyKey.AUDIO_NOT_FOUND to UiCopyValue("Audio introuvable : %s", "Audio not found: %s", "Audio niet gevonden: %s")
    ,UiCopyKey.AUDIO_READ_FAILED to UiCopyValue("Impossible de lire l'audio : %s", "Could not play audio: %s", "Audio kon niet worden afgespeeld: %s")
    ,UiCopyKey.UNKNOWN_ERROR to UiCopyValue("erreur inconnue", "unknown error", "onbekende fout")
    ,UiCopyKey.AUDIO_EMPTY to UiCopyValue("Audio vide : %s", "Empty audio: %s", "Lege audio: %s")
    ,UiCopyKey.PLAYING_PRONUNCIATION to UiCopyValue("Lecture de la prononciation", "Playing pronunciation", "Uitspraak afspelen")
    ,UiCopyKey.AUDIO_ERROR to UiCopyValue("Erreur audio (%d / %d)", "Audio error (%d / %d)", "Audiofout (%d / %d)")
    ,UiCopyKey.ALL to UiCopyValue("Tous", "All", "Alle"),
    UiCopyKey.DOUBTFUL to UiCopyValue("Douteux", "Doubtful", "Twijfelachtig"),
    UiCopyKey.COMPLETED to UiCopyValue("Terminés", "Completed", "Voltooid"),
    UiCopyKey.MISSING_TRANSLATION_STATUS to UiCopyValue("❓ Traduction manquante", "❓ Missing translation", "❓ Ontbrekende vertaling"),
    UiCopyKey.NEW_STATUS to UiCopyValue("⬜ Nouveau", "⬜ New", "⬜ Nieuw"),
    UiCopyKey.VALIDATED_STATUS to UiCopyValue("✅ Validé", "✅ Validated", "✅ Gevalideerd"),
    UiCopyKey.MISSING_SAAMAKA_TRANSLATION to UiCopyValue("Traduction saamaka manquante", "Missing Saamaka translation", "Saamaka-vertaling ontbreekt"),
    UiCopyKey.MISSING_FRENCH_TRANSLATION to UiCopyValue("Traduction française manquante", "Missing French translation", "Franse vertaling ontbreekt")
    ,UiCopyKey.WORDS_AND_EXPRESSIONS to UiCopyValue("%d mots et expressions", "%d words and expressions", "%d woorden en uitdrukkingen")
    ,UiCopyKey.MY_PROGRESS to UiCopyValue("Ma progression", "My progress", "Mijn voortgang")
    ,UiCopyKey.LEARNING_PATH to UiCopyValue("Ton parcours d'apprentissage", "Your learning journey", "Je leertraject")
    ,UiCopyKey.PATH_PERCENT to UiCopyValue("%d %% du parcours", "%d%% of the journey", "%d%% van het traject")
    ,UiCopyKey.TODAY_QUIZ to UiCopyValue("Quiz du jour", "Today's quiz", "Quiz van vandaag")
    ,UiCopyKey.QUESTION_NUMBER to UiCopyValue("Question %d", "Question %d", "Vraag %d")
    ,UiCopyKey.SCORE_VALUE to UiCopyValue("Score %d", "Score %d", "Score %d")
    ,UiCopyKey.TEST_KNOWLEDGE to UiCopyValue("Teste tes connaissances en Saamaka", "Test your Saamaka knowledge", "Test je kennis van het Saamaka")
    ,UiCopyKey.NEXT to UiCopyValue("Suivant", "Next", "Volgende")
    ,UiCopyKey.MATCH_WORDS_TITLE to UiCopyValue("🧩 Associer les mots", "🧩 Match words", "🧩 Woorden koppelen")
    ,UiCopyKey.MY_STATISTICS to UiCopyValue("Mes statistiques", "My statistics", "Mijn statistieken")
    ,UiCopyKey.GAMES_COUNT to UiCopyValue("🎮 Parties : %d", "🎮 Games: %d", "🎮 Spellen: %d")
    ,UiCopyKey.ERRORS_STAT to UiCopyValue("❌ Erreurs : %d", "❌ Errors: %d", "❌ Fouten: %d")
    ,UiCopyKey.PERFECT_GAMES to UiCopyValue("🏆 Parties parfaites : %d", "🏆 Perfect games: %d", "🏆 Perfecte spellen: %d")
    ,UiCopyKey.GAME_SCORE to UiCopyValue("Score : %d/%d • Erreurs : %d", "Score: %d/%d • Errors: %d", "Score: %d/%d • Fouten: %d")
    ,UiCopyKey.TESTER_SPACE to UiCopyValue("Espace testeur", "Tester area", "Testomgeving")
    ,UiCopyKey.TESTER_EXPORTS to UiCopyValue("Exports et travail testeur", "Exports and tester work", "Exports en testerwerk")
    ,UiCopyKey.CORRECTIONS_SPACE to UiCopyValue("Espace corrections", "Corrections area", "Correctieomgeving")
    ,UiCopyKey.TESTER_WORK_GUIDANCE to UiCopyValue("Valide, corrige et exporte ton travail de testeur.", "Validate, correct and export your tester work.", "Valideer, corrigeer en exporteer je testerwerk.")
    ,UiCopyKey.LOCAL_VALIDATIONS_COUNT to UiCopyValue("%d validation(s) locale(s)", "%d local validation(s)", "%d lokale validatie(s)")
    ,UiCopyKey.INSTRUCTIONS to UiCopyValue("Consignes", "Instructions", "Instructies")
    ,UiCopyKey.AVAILABLE_LANGUAGES to UiCopyValue("Langues disponibles", "Available languages", "Beschikbare talen")
    ,UiCopyKey.SPEAKER_VALUE to UiCopyValue("Locuteur : %s", "Speaker: %s", "Spreker: %s")
    ,UiCopyKey.EXPLORE_ACTION to UiCopyValue("Explorer →", "Explore →", "Verkennen →")
    ,UiCopyKey.MY_WORDS to UiCopyValue("Mes mots", "My words", "Mijn woorden")
    ,UiCopyKey.FIND_MY_WORDS to UiCopyValue("Retrouver mes mots", "Find my words", "Mijn woorden terugvinden")
    ,UiCopyKey.TODAY to UiCopyValue("Aujourd'hui", "Today", "Vandaag")
    ,UiCopyKey.PROGRESSION to UiCopyValue("Progression", "Progress", "Voortgang")
    ,UiCopyKey.LEARNING_ACTIVITIES to UiCopyValue("Quiz • Mots • Phrases • Jeux", "Quiz • Words • Phrases • Games", "Quiz • Woorden • Zinnen • Spellen")
    ,UiCopyKey.TRANSLATOR_INTRO to UiCopyValue("Traduisez des phrases et des textes complets. Les mots et expressions du dictionnaire restent gratuits.", "Translate complete sentences and texts. Dictionary words and expressions remain free.", "Vertaal volledige zinnen en teksten. Woorden en uitdrukkingen uit het woordenboek blijven gratis.")
    ,UiCopyKey.UNLIMITED_SENTENCE_TRANSLATIONS to UiCopyValue("Traductions de phrases et textes sans limite.", "Unlimited sentence and text translations.", "Onbeperkte vertalingen van zinnen en teksten.")
    ,UiCopyKey.TESTER_MODE to UiCopyValue("Mode testeur", "Tester mode", "Testmodus")
    ,UiCopyKey.CATEGORIES_COUNT to UiCopyValue("%d thèmes", "%d themes", "%d thema's")
    ,UiCopyKey.APPROXIMATE_REVIEW to UiCopyValue("Proposition approximative — à vérifier", "Approximate suggestion — review required", "Benaderend voorstel — controleren")
    ,UiCopyKey.ACTIVATE_TESTER_MODE to UiCopyValue("Activer le mode testeur", "Enable tester mode", "Testmodus inschakelen")
    ,UiCopyKey.DEACTIVATE_TESTER_MODE to UiCopyValue("Désactiver le mode testeur", "Disable tester mode", "Testmodus uitschakelen")
)

val saamakaCopyAwaitingValidation: Set<UiCopyKey> = uiCopy
    .filterValues { it.saamaka == null }
    .keys

fun AppStrings.ui(key: UiCopyKey, vararg arguments: Any): String {
    val value = requireNotNull(uiCopy[key]) { "Missing UI copy for $key" }
    val template = when (uiLanguage) {
        UiLanguage.FRENCH -> value.french
        UiLanguage.ENGLISH -> value.english
        UiLanguage.DUTCH -> value.dutch
        UiLanguage.SAAMAKA -> value.saamaka ?: value.english
    }
    return if (arguments.isEmpty()) template else String.format(Locale.ROOT, template, *arguments)
}

fun AppStrings.provenanceLabel(provenance: LocalMatchProvenance): String = ui(
    when (provenance) {
        LocalMatchProvenance.DICTIONARY -> UiCopyKey.EXACT_DICTIONARY_MATCH
        LocalMatchProvenance.LOCAL_CORRECTION -> UiCopyKey.LOCAL_CORRECTION
        LocalMatchProvenance.ATTESTED_EXPRESSION -> UiCopyKey.VALIDATED_RULE_TRANSLATION
    }
)

fun AppStrings.reliabilityLabel(reliability: TranslationReliability): String = ui(
    when (reliability) {
        TranslationReliability.HIGH -> UiCopyKey.RELIABILITY_HIGH
        TranslationReliability.MEDIUM -> UiCopyKey.RELIABILITY_MEDIUM
        TranslationReliability.LOW -> UiCopyKey.RELIABILITY_LOW
    }
).substringAfter(": ")

internal fun AppStrings.missionFilterLabel(filter: MissionFilter): String = ui(
    when (filter) {
        MissionFilter.ALL -> UiCopyKey.ALL
        MissionFilter.DOUBTFUL -> UiCopyKey.DOUBTFUL
        MissionFilter.TO_COMPLETE -> UiCopyKey.TO_COMPLETE
        MissionFilter.COMPLETED -> UiCopyKey.COMPLETED
    }
)

internal fun AppStrings.missionStatusLabel(status: MissionVisualStatus): String = when (status) {
    MissionVisualStatus.DOUBTFUL -> "⚠️ ${ui(UiCopyKey.DOUBTFUL)}"
    MissionVisualStatus.TO_COMPLETE -> ui(UiCopyKey.MISSING_TRANSLATION_STATUS)
    MissionVisualStatus.NEW -> ui(UiCopyKey.NEW_STATUS)
    MissionVisualStatus.ALREADY_VALIDATED -> ui(UiCopyKey.VALIDATED_STATUS)
}

internal fun AppStrings.missionMissingMessage(message: String): String = when (message) {
    "Traduction saamaka manquante" -> ui(UiCopyKey.MISSING_SAAMAKA_TRANSLATION)
    "Traduction française manquante" -> ui(UiCopyKey.MISSING_FRENCH_TRANSLATION)
    else -> message
}
