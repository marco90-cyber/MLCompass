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

## Personalizzazione (ingranaggio → Personalizzazione)

7 stili di quadrante con anteprima animata: **Classico, Difetto, iPhone, Navigatore, Graduato,
Design, Leggibile**. Menu colori con 11 tinte (rosso, blu, verde, giallo, bianco, grafite, talpa,
arancio, viola, turchese, rosa). Lo stile iPhone include la bolla di livello.

## Impostazioni

Nord di riferimento, tema (sistema / chiaro / scuro), vista (automatica / quadrante / scala),
fluidità del movimento, schermo sempre acceso, vibrazione verso il Nord, mostra campo magnetico,
permesso di posizione (serve solo per declinazione e campo previsto).

## Compilare l'APK

Ogni push su `main` avvia GitHub Actions (`.github/workflows/build.yml`):
scarica l'APK da **Actions → ultima esecuzione → Artifacts → Bussola-apk**.

L'APK è firmato con la chiave inclusa in `app/bussola.keystore`, quindi ogni nuova versione si
installa sopra la precedente senza disinstallare. È una chiave per uso personale, non per il Play Store.

In locale: `gradle assembleRelease` (Gradle 8.10+, JDK 17, Android SDK 35).
