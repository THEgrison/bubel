# Rapport d’audit Russki (pré-v1)

**Date d’exécution** : 18 novembre 2026 (Europe/Paris)  
**Branche** : `cursor/russki-pre-release-audit-ad1c`  
**Environnement** : Cloud Agent Linux, Android SDK 36/37, **pas d’adb / pas d’émulateur connecté**

## 1. Résultats réellement observés

### Compilation

| Commande | Résultat |
| --- | --- |
| `./gradlew :app:assembleDebug` | **SUCCÈS** |
| APK produit | `/agent/repos/tmp-9335cb66bca48322/app/build/outputs/apk/debug/app-debug.apk` (~20,7 Mo) |

### Tests unitaires

| Commande | Résultat |
| --- | --- |
| `./gradlew :app:testDebugUnitTest` | **SUCCÈS — 45 tests, 0 failure, 0 error, 0 skipped** |

Suites exécutées :

| Suite | Tests | Échecs |
| --- | --- | --- |
| AlphabetContentTest | 10 | 0 |
| OfflinePersistenceTest | 6 | 0 |
| OnlineApiTest | 9 | 0 |
| PathSafetyTest | 3 | 0 |
| LocalAudioPlayerTest | 2 | 0 |
| MainActivitySmokeTest | 1 | 0 |
| QuestionGeneratorTest | 3 | 0 |
| QuizEngineTest | 4 | 0 |
| SrsSchedulerTest | 4 | 0 |
| TemporaryContentSourceTest | 3 | 0 |

### Tests instrumentés

| Commande | Résultat |
| --- | --- |
| `./gradlew :app:connectedDebugAndroidTest` | **NON EXÉCUTÉ jusqu’au bout** — `adb: command not found`, aucun appareil/AVD |

Un smoke `MainActivityInstrumentedTest` est présent sous `app/src/androidTest/` pour un futur run sur téléphone/émulateur.

### Lancement / navigation

| Contrôle | Résultat |
| --- | --- |
| `MainActivity` via Robolectric (`MainActivitySmokeTest`) | **OK** — activité démarre sans crash |
| Navigation Compose Accueil/Apprendre/Réviser/Profil sur appareil | **NON EXÉCUTÉ** (pas d’UI device dans cet environnement) |

### Couverture fonctionnelle (via tests unitaires / code review)

| Scénario | Statut observé |
| --- | --- |
| 33 lettres | **OK** (AlphabetContentTest) |
| Leçons / traductions seed | **OK** (seed + OfflinePersistence) |
| Types de quiz (7) | **OK** (QuestionGeneratorTest couvre tous les `QuizQuestionType`) |
| Corrections / score / anti double-comptage | **OK** (QuizEngineTest) |
| Flashcards + SRS / dates de révision | **OK** (SrsSchedulerTest + OfflinePersistence) |
| Persistance progression | **OK** (OfflinePersistenceTest restart / update) |
| Migration Room 1→2 | **OK** (OfflinePersistenceTest) |
| Mode avion / online disabled | **OK** (OnlineApiTest + DisabledOnlineApi) |
| Erreurs réseau / payload invalide | **OK** (OnlineApiTest) |
| Téléchargements interrompus / checksum / reprise Range | **OK** (OnlineApiTest) |
| Traversée de chemins packs | **OK** (PathSafetyTest — corrigé à l’audit) |
| Fichiers audio manquants | **OK** (LocalAudioPlayerTest — pas de crash) |
| Lecture / pause audio réelle | **NON TESTÉ avec fichier réel** — aucun OGG embarqué ; lecteur ajouté, pas de media dans le seed |
| Sync progression | **OK comportement attendu** — refuse sans opt-in / sans jeton (pas de faux succès) |

### Dépendances / avertissements

- Dépendances résolues (Room 2.8.5, OkHttp 4.12, WorkManager 2.10.5, Retrofit 2.11, Compose BOM).
- Avertissements AGP 9 : `android.builtInKotlin=false`, `android.newDsl=false`, APIs `applicationVariants` obsolètes — **non bloquants**, liés à KSP.
- Pas de conflit de dépendances critique observé sur `debugRuntimeClasspath`.

### Sécurité (contrôles effectués)

| Contrôle | Résultat |
| --- | --- |
| Secrets dans sources | Aucune clé API / password hardcodé trouvé |
| `RUSSKI_API_BASE_URL` BuildConfig | Chaîne vide par défaut |
| Scan strings DEX APK | Aucun motif secret évident |
| HTTPS / cleartext | `network_security_config` cleartext=false |
| Validation données distantes | RemoteJson + PathSafety |
| Chemins packs | Traversée `../` rejetée (fix audit) |
| Main thread DB prod | `allowMainThreadQueries` **uniquement** helpers de test |
| Fonctionnalités inactives | Sync / téléchargement distant : messages d’indisponibilité explicites ; pas de bouton « lire l’audio » sans fichier |

## 2. Corrections appliquées pendant l’audit

1. **PathSafety** — validation des `relativePath` / ids d’assets ; refus de `../` à l’install.
2. **AssetAudioAvailability** — résolution aussi sous `filesDir` (packs installés), pas seulement assets.
3. **LocalAudioPlayer** — pause/release cycle de vie ; échec silencieux si fichier absent.
4. **Préférence « Priorité hors ligne »** branchée sur `PackCoordinator` (ignorait le catalogue distant à tort auparavant : toujours `false`).
5. Message d’erreur pack local obsolète corrigé.
6. Tests PathSafety / audio manquant / smoke MainActivity + squelette instrumenté.

## 3. Problèmes restants

1. **Aucun enregistrement audio pédagogique embarqué** — play/pause réel non vérifiable ; UI affiche « Pas d’enregistrement natif ».
2. **Pas de Media3** — lecteur `MediaPlayer` basique ; non branché en boutons UI (seulement API prête).
3. **Préférences / sync token** en mémoire process — pas encore DataStore.
4. **Backend online non déployé** — catalogue distant / sync nécessitent `RUSSKI_API_BASE_URL` + serveur.
5. **Tests instrumentés UI** non joués ici (pas d’adb).
6. **Navigation Compose** non validée sur device.
7. **Avertissements AGP 9 / KSP** à migrer quand KSP supportera le Kotlin intégré.
8. **Licences audio** : N/A tant qu’aucun fichier audio n’est livré ; à documenter pack par pack lors de l’ajout CDN.
9. **Release signée** non produite dans cet audit (volontaire — pas de keystore secret dans le dépôt).

## 4. Commandes de reconstruction

```bash
cd /chemin/vers/russki
# optionnel : echo "sdk.dir=/chemin/Android/Sdk" > local.properties
./gradlew clean :app:assembleDebug :app:testDebugUnitTest
```

Avec API distante (HTTPS) :

```bash
./gradlew :app:assembleDebug -PRUSSKI_API_BASE_URL=https://api.exemple.fr
```

## 5. APK debug

```bash
./gradlew :app:assembleDebug
```

Fichier :

```text
app/build/outputs/apk/debug/app-debug.apk
```

Chemin absolu (machine d’audit) :

```text
/agent/repos/tmp-9335cb66bca48322/app/build/outputs/apk/debug/app-debug.apk
```

## 6. Installation sur téléphone

1. Activer **Options développeur** → **Débogage USB** (ou autoriser sources inconnues pour sideload).
2. Copier l’APK sur le téléphone, ou :

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

3. Ouvrir **Russki**.

## 7. Version signée (release)

1. Créer un keystore **hors git** :

```bash
keytool -genkey -v -keystore russki-upload.jks -keyalg RSA -keysize 2048 -validity 10000 -alias russki
```

2. Dans `app/build.gradle.kts` (local, non commité) ou via variables d’environnement CI :

```kotlin
android {
  signingConfigs {
    create("release") {
      storeFile = file("/chemin/privé/russki-upload.jks")
      storePassword = System.getenv("RUSSKI_STORE_PASSWORD")
      keyAlias = "russki"
      keyPassword = System.getenv("RUSSKI_KEY_PASSWORD")
    }
  }
  buildTypes.getByName("release") {
    signingConfig = signingConfigs.getByName("release")
    isMinifyEnabled = true
  }
}
```

3. Générer :

```bash
./gradlew :app:assembleRelease
# sortie typique : app/build/outputs/apk/release/app-release.apk
```

Ne jamais committer le keystore ni les mots de passe.

## 8. Synthèse fonctionnalités / limites

**Opérationnel hors ligne** : alphabet 33 lettres, exercices, vocabulaire salutations, phrases seed, quiz (7 types), flashcards+SRS, Room + migrations, packs locaux (démo assets), UI packs/profil, client online désactivé sans URL.

**Limites v1** : pas d’audio natif embarqué ; sync/catalogue distant dépendent d’un serveur encore à déployer ; pas de validation UI sur device dans cet audit ; préférences non persistées DataStore.
