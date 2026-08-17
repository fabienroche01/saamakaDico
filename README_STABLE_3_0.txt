SAAMAKA DICO 3.0 BÊTA — VERSION TESTEURS STABLE

Cette version utilise un identifiant Android distinct :
com.saamaka.dico.testeurs

Cela évite les conflits de signature avec les anciennes versions de test.

ÉTAPE 1 — TEST RAPIDE
Build > Build APK(s)
Fichier : app/build/outputs/apk/debug/app-debug.apk

ÉTAPE 2 — APK RELEASE SIGNÉE (recommandée pour le Redmi A5)
Build > Generate Signed Bundle / APK
Choisir APK > Next
Créer une nouvelle clé .jks et conserver soigneusement :
- fichier .jks
- alias
- mots de passe
Choisir la variante release puis Finish.

L’APK signée sera généralement dans :
app/build/outputs/apk/release/app-release.apk

IMPORTANT
Ne jamais perdre la clé .jks. Elle servira à toutes les futures mises à jour.

FONCTIONS
- 1313 entrées hors connexion
- recherche français ↔ saamaka
- favoris
- historique
- formulaire de correction testeurs
- export groupé des corrections
