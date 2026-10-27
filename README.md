# MLCompass

Bussola per Android in italiano (Kotlin + Jetpack Compose).

## Come funziona

- **Telefono in piano**: quadrante circolare con scala graduata, gradi e direzione al centro.
- **Telefono in verticale**: la vista passa da sola alla **scala lineare** orizzontale
  (la direzione è quella in cui punta il retro del telefono, come una fotocamera).
- **Icona telefono con lucchetto**: blocca la vista corrente; un secondo tocco torna all'automatico.
- **Icona segnale**: apre *Stato del sensore* (accuratezza, campo misurato, campo previsto,
  animazione del movimento a otto per calibrare).
- **Chip "Nord geografico"**: passa tra nord geografico (con declinazione) e nord magnetico.
- **Campo magnetico** in µT in alto a destra.

## Aspetto (icona tavolozza nella barra, oppure ingranaggio → Aspetto)

Schermata con anteprima grande che si muove dal vivo, miniature dei **7 quadranti**
(Classico, Difetto, iPhone, Navigatore, Graduato, Design, Leggibile), **11 colori** e sfondo
(sistema / chiaro / scuro). Ogni scelta si applica subito, senza conferma.
Lo stile iPhone include la bolla di livello.

## Impostazioni

Nord di riferimento, tema (sistema / chiaro / scuro), vista (automatica / quadrante / scala),
fluidità del movimento, schermo sempre acceso, **risposta tattile** (vibra mentre la direzione cambia,
con un impulso più deciso ai punti cardinali), mostra campo magnetico,
permesso di posizione (serve solo per declinazione e campo previsto).

## Compilare l'APK

Ogni push su `main` avvia GitHub Actions (`.github/workflows/build.yml`):
scarica l'APK da **Actions → ultima esecuzione → Artifacts → Bussola-apk**.

L'APK è firmato con la chiave inclusa in `app/bussola.keystore`, quindi ogni nuova versione si
installa sopra la precedente senza disinstallare. È una chiave per uso personale, non per il Play Store.

In locale: `gradle assembleRelease` (Gradle 8.10+, JDK 17, Android SDK 35).
