package com.saamaka.dico.testeurs

/** Saamaka uses French until these new account strings are linguistically validated. */
internal enum class AccountText(val fr: String, val en: String, val nl: String) {
    CREATE("Créer un compte gratuit", "Create a free account", "Gratis account maken"),
    ACCOUNT("Mon compte", "My account", "Mijn account"),
    BENEFIT("Créez votre compte gratuit pour profiter davantage de la traduction de textes et des activités d’apprentissage en Saamaka.", "Create your free account to get more from text translation and Saamaka learning activities.", "Maak je gratis account en haal meer uit tekstvertaling en leeractiviteiten in het Saamaka."),
    LOCAL_DATA("Vos favoris et votre progression restent sur ce téléphone. Leur synchronisation entre appareils n’est pas encore disponible.", "Your favourites and progress stay on this phone. Cross-device syncing is not available yet.", "Je favorieten en voortgang blijven op deze telefoon. Synchronisatie tussen apparaten is nog niet beschikbaar."),
    EMAIL("Adresse e-mail", "Email address", "E-mailadres"),
    PASSWORD("Mot de passe", "Password", "Wachtwoord"),
    CONFIRM_PASSWORD("Confirmer le mot de passe", "Confirm password", "Wachtwoord bevestigen"),
    PASSWORD_HINT("Au moins 6 caractères.", "At least 6 characters.", "Minimaal 6 tekens."),
    SIGN_IN("Se connecter", "Sign in", "Inloggen"),
    HAVE_ACCOUNT("J’ai déjà un compte", "I already have an account", "Ik heb al een account"),
    FORGOT("Mot de passe oublié", "Forgot password", "Wachtwoord vergeten"),
    LATER("Continuer sans inscription", "Continue without signing up", "Doorgaan zonder registratie"),
    CLOSE("Fermer", "Close", "Sluiten"),
    CANCEL("Annuler", "Cancel", "Annuleren"),
    SIGN_OUT("Se déconnecter", "Sign out", "Uitloggen"),
    SIGN_OUT_GUEST("Se déconnecter / Invité", "Sign out / Guest", "Uitloggen / Gast"),
    VERIFY_REQUIRED("Confirmez votre adresse avec le lien reçu par e-mail, puis appuyez sur « J’ai confirmé mon e-mail ». Le statut gratuit sera alors activé.", "Confirm your address using the email link, then tap 'I verified my email' to activate your free account.", "Bevestig je adres via de e-maillink en tik daarna op 'Ik heb mijn e-mail bevestigd' om je gratis account te activeren."),
    PROCESSING("Traitement en cours…", "Processing…", "Bezig…"),
    VERIFICATION_SEND_FAILED("Votre compte existe, mais l’e-mail de vérification n’a pas pu être envoyé. Utilisez « Renvoyer l’e-mail de vérification » pour réessayer.", "Your account exists, but the verification email could not be sent. Use 'Resend verification email' to try again.", "Je account bestaat, maar de verificatie-e-mail kon niet worden verzonden. Gebruik 'Verificatie-e-mail opnieuw verzenden' om het opnieuw te proberen."),
    CHECK_EMAIL("E-mail de vérification envoyé. Pensez à vérifier les indésirables.", "Verification email sent. Please check your spam folder too.", "Verificatie-e-mail verzonden. Controleer ook je spammap."),
    CHECK_VERIFICATION("J’ai confirmé mon e-mail", "I verified my email", "Ik heb mijn e-mail bevestigd"),
    RESEND("Renvoyer l’e-mail de vérification", "Resend verification email", "Verificatie-e-mail opnieuw verzenden"),
    VERIFIED("Adresse e-mail vérifiée. Votre compte gratuit est activé.", "Email verified. Your free account is active.", "E-mailadres bevestigd. Je gratis account is actief."),
    SIGNED_IN("Connexion réussie.", "Signed in.", "Ingelogd."),
    RESET_SENT("Si un compte correspond à cette adresse, vous recevrez un lien de réinitialisation. Vérifiez aussi les indésirables.", "If an account matches this address, you will receive a reset link. Please check spam too.", "Als er een account met dit adres bestaat, ontvang je een herstellink. Controleer ook je spammap."),
    DELETE("Supprimer mon compte", "Delete my account", "Mijn account verwijderen"),
    DELETE_NOTICE("Votre compte de connexion sera supprimé définitivement. Les données locales restent sur ce téléphone. Cette action ne résilie pas un abonnement Google Play.", "Your sign-in account will be permanently deleted. Local data stays on this phone. This does not cancel a Google Play subscription.", "Je inlogaccount wordt definitief verwijderd. Lokale gegevens blijven op deze telefoon. Dit annuleert geen Google Play-abonnement."),
    CONFIRM_DELETE("Confirmer la suppression", "Confirm deletion", "Verwijdering bevestigen"),
    CURRENT_PASSWORD("Votre mot de passe actuel", "Your current password", "Je huidige wachtwoord"),
    SUBSCRIPTIONS("Gérer mon abonnement Google Play", "Manage my Google Play subscription", "Mijn Google Play-abonnement beheren"),
    DELETED("Compte supprimé.", "Account deleted.", "Account verwijderd."),
    DATA_USE("Votre adresse e-mail sert à la connexion et à la gestion du compte via Firebase Authentication.", "Your email is used for sign-in and account management through Firebase Authentication.", "Je e-mailadres wordt gebruikt voor inloggen en accountbeheer via Firebase Authentication."),
    NETWORK_ERROR("Connexion impossible. Vérifiez votre accès Internet puis réessayez.", "Unable to connect. Check your internet connection and retry.", "Geen verbinding. Controleer je internetverbinding en probeer opnieuw."),
    TOO_MANY_REQUESTS("Trop de tentatives. Patientez avant de réessayer.", "Too many attempts. Please wait before retrying.", "Te veel pogingen. Wacht even en probeer opnieuw."),
    WEAK_PASSWORD("Choisissez un mot de passe plus robuste.", "Choose a stronger password.", "Kies een sterker wachtwoord."),
    INVALID_EMAIL("Vérifiez le format de votre adresse e-mail.", "Check your email address format.", "Controleer je e-mailadres."),
    EXISTING_ACCOUNT("Cette adresse est déjà utilisée. Connectez-vous ou réinitialisez votre mot de passe.", "This email is already in use. Sign in or reset your password.", "Dit e-mailadres is al in gebruik. Log in of herstel je wachtwoord."),
    REAUTH_REQUIRED("Reconnectez-vous avant cette opération.", "Sign in again before this operation.", "Log opnieuw in voor deze handeling."),
    INVALID_LOGIN("Connexion refusée. Vérifiez vos identifiants ou réinitialisez votre mot de passe.", "Sign-in failed. Check your credentials or reset your password.", "Inloggen mislukt. Controleer je gegevens of herstel je wachtwoord."),
    UNAVAILABLE("L’inscription par e-mail n’est pas encore disponible. Réessayez plus tard.", "Email registration is not available yet. Please try again later.", "Registratie via e-mail is nog niet beschikbaar. Probeer later opnieuw."),
    GENERIC_ERROR("L’opération a échoué. Réessayez dans quelques instants.", "The operation failed. Please try again shortly.", "De handeling is mislukt. Probeer het straks opnieuw.");

    fun text(language: UiLanguage): String = when (language) {
        UiLanguage.ENGLISH -> en
        UiLanguage.DUTCH -> nl
        UiLanguage.FRENCH, UiLanguage.SAAMAKA -> fr
    }
}
