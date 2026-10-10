# CastigoClasses — mod Fabric

Companion client **0.1.0-beta.15** per [CastigoClasses plugin](https://github.com/ZogoratXV/CastigoClasses_plugin). I VFX client richiedono il plugin 0.1.0-beta.3 con CastigoCore beta.9; i punti attributo richiedono almeno il plugin beta.2. Con beta.1 rimangono disponibili HUD e skill, senza pulsanti di assegnazione (protocollo v1 compatibile).

## Strumenti RP e regolazioni — beta.15

Skill ereditate invariate, contributo cure/protezioni all'XP, calibrazione armi per oggetto, 11 preset HUD, bersaglio durante il casting e strumenti admin di diagnosi/prova. [Guida beta.15](docs/AGGIORNAMENTO-BETA15.md). Aggiornare entrambi i JAR.

## Progressione — beta.14

Cap giornaliero con reset in ora italiana, XP senza sfere e proporzionale al contributo, 12 sottoclassi con consumabili, gruppi LuckPerms automatici e impugnatura a due mani configurabile. [Guida beta.14](docs/AGGIORNAMENTO-BETA14.md).

## HUD LuckPerms, casting e icone — beta.13

Editor VFX e F8 rimossi. Tempi di preparazione rispettati da tutte le skill, HUD per gruppi LuckPerms configurabile in gioco con layout PNG ItemsAdder e 40 icone abbinate. [Guida beta.13](docs/AGGIORNAMENTO-BETA13.md). Aggiornare entrambi i JAR.

Le sezioni seguenti descrivono le versioni precedenti: l’editor F8 non è più disponibile nella beta.13.

## Corpo a corpo, bersagli e icone — beta.12

Lame di energia stratificate, affondi tridimensionali, colori distinti per l’arciere, aure sul bersaglio durante la preparazione e scelta delle icone dalla GUI admin. [Guida beta.12](docs/AGGIORNAMENTO-BETA12.md). Aggiornare plugin e mod insieme.

## VFX e animazioni delle armi — beta.11

Preset per 48 abilità: sigilli ampliati, scie intrecciate, impatti con volume e frammenti cubici, tagli, scudi e vortici. [Installazione e utilizzo](docs/AGGIORNAMENTO-VFX-BETA11.md) · [Catalogo delle abilità](docs/VFX-48-ABILITA.md). Aggiornare plugin e mod alla beta.11.

## Motore VFX 3D beta.8

Orison con anelli e colonna a texture, animazione e quattro pagine nell'editor F8. Aggiornare entrambi i JAR alla beta.8: [guida al motore, texture e collaudo](docs/MOTORE-VFX-3D.md).

## Casting ed editor beta.7

Barra casting e editor VFX in gioco con F8: [guida completa](docs/EDITOR-VFX-CASTING.md). Aggiorna plugin e mod alla beta.7.

## Orison beta.6

Nuovo effetto di guarigione sul destinatario e audio incluso nella mod: [installazione, preset e collaudo](docs/ORISON-VFX.md).

## Aggiornamento beta.5

Cure senza party, requisiti per oggetto vanilla/ItemsAdder, `/classe admin` e icone PNG personalizzate: [guida completa](docs/OGGETTI-ICONE-VFX.md). Aggiorna sia plugin sia mod alla beta.5 per le icone.

## Installazione

Il plugin **beta.4** aggiunge cinque discipline con 40 abilità. La mod beta.5 riceve nomi, icone, descrizioni e sblocchi dal server e aggiunge il rendering delle icone PNG. Vedi la [guida alle discipline](docs/DISCIPLINE.md). Il comportamento di combattimento è gestito dal server; la grafica dedicata delle nuove skill è rimandata.

- Minecraft **26.2**, Java **25**.
- Fabric Loader **0.19.5** o superiore.
- Fabric API **0.161.0+26.2**.
- Sostituisci il vecchio JAR della mod con `CastigoClasses-Fabric-0.1.0-beta.13.jar` nella cartella `mods` del profilo Minecraft 26.2. Mantieni Fabric API; non lasciare entrambe le versioni della mod nella cartella.

Il server rimane Purpur e deve avere CastigoCore e CastigoClasses. Questa mod è solo client.

## Utilizzo

- **R**: alterna hotbar vanilla e barra delle otto abilità.
- **1–8** in modalità abilità: lancia lo slot corrispondente. Si rispettano anche eventuali rimappature dei tasti hotbar vanilla. Il nono slot è inutilizzato.
- In modalità abilità i tasti hotbar e la rotella non cambiano l'oggetto impugnato. Con un menu aperto gli input tornano al menu.
- **K**: apre personaggio, attributi e disposizione delle abilità.
- Nella scheda **Disposizione skill**, trascina una skill su un altro slot, oppure clicca due slot, poi premi **Salva disposizione**. L'ordine viene salvato sul server e ritrovato alla riconnessione.
- R e K sono modificabili in **Opzioni → Comandi → Assegnazione tasti → Castigo Classes**.

Il pannello in alto a sinistra mostra la testa della skin attuale (incluso il secondo strato), nome visualizzato del personaggio, classe, livello, vita, risorsa e gruppo principale LuckPerms, se presente. Il nome e il colore della risorsa arrivano dalla classe: Mana, Vigore, Fede o un'altra etichetta.
Le skill mostrano icone, ricariche, livello richiesto e indisponibilità per risorsa insufficiente. Nel pannello K il passaggio del mouse mostra descrizione, costo e ricarica.

## HUD medievale fantasy

Il pannello del personaggio misura 172×64 unità GUI, circa il 46% di area in meno rispetto alla beta.1, con cornici in bronzo/oro, rivetti, fondo scuro e testo pergamena. La testa misura 24×24; livello, classe e gruppo restano visibili, con vita/risorsa su due barre sottili e progressione MMO su una linea dorata.

La barra skill è larga 192 unità GUI invece di 224. Cornici e colori sono condivisi con il pannello e con l'editor K; trascinamento e selezione mantengono le nuove dimensioni.

Quando il plugin invia uno stato valido, vengono nascosti i cuori del giocatore, la barra XP vanilla e il numero del livello vanilla. La fame viene spostata nella posizione prima occupata dai cuori, conservando icone, mezze icone, effetto Fame e animazioni originali. Questo vale sia con hotbar normale sia con barra skill attiva. Il salto delle cavalcature e la barra localizzatore non vengono rimossi insieme all'XP.

In assenza del plugin o dopo il timeout tornano automaticamente tutti gli elementi vanilla alle loro posizioni originali. Le dimensioni indicate seguono l'impostazione di scala GUI di Minecraft.

## Classi dinamiche

Il client riceve il catalogo dal server. Nuove classi, sottoclassi, nomi, attributi, icone di oggetti Minecraft, costi e cooldown non richiedono una nuova build della mod. `/classe reload` sul server aggiorna i client collegati.
Il plugin beta.3 invia descrizioni degli effetti e questa mod li anima sul client: raggi, anelli, spirali ed emissioni puntuali, con suoni posizionali. Tutti gli osservatori vicini con mod beta.4 ricevono gli eventi; i client senza la mod aggiornata non vedono né sentono i VFX delle skill. I preset usano asset Minecraft esistenti. Per nuove forme di rendering e asset esclusivi occorrerà estendere la mod.

La mod invia richieste di attivazione, riordino e assegnazione di un punto; non decide i valori delle statistiche. Danno, cura, risorsa, sblocchi e cooldown vengono decisi dal server.
In server senza plugin, dopo una disconnessione o in assenza di aggiornamenti per 10 secondi torna disponibile la hotbar vanilla. Gli elementi rispettano F1; la barra abilità è disabilitata in modalità spettatore.

## Compilazione

Java 25 e Gradle Wrapper incluso (9.6.0), Loom 1.17.21.

```powershell
.\gradlew.bat build
```

Linux/macOS: `./gradlew build`. Usa il JAR senza suffisso `-sources` in `build/libs`.

I test controllano riordino, catalogo/stato dinamico, timeout, cooldown ricevuti e codec UTF-8 compatibile con Bukkit. Compilazione e test automatici verificati; aspetto dell'HUD e uso multiplayer richiedono ancora la prova in gioco.
Protocollo: [docs/PROTOCOL.md](docs/PROTOCOL.md). Prove manuali: [docs/TEST-IN-GIOCO.md](docs/TEST-IN-GIOCO.md).

## Punti attributo

Apri **K → Attributi** e premi **+** accanto alla statistica, oppure usa `/classe assegna <attributo>` (forza, destrezza, vita, mana/risorsa, intelligenza, attacco, difesa). Ogni pressione spende un punto; il server verifica disponibilità e limiti e salva immediatamente. La scheda **Disposizione skill** conserva il riordino degli otto slot. In questa versione non è previsto il rimborso dei punti.

In `plugins/CastigoClasses/config.yml`:

```yaml
stat-points:
  every-levels: 2
  points-per-award: 1
  per-point:
    strength: 1
    dexterity: 1
    health: 2
    mana: 5
    intelligence: 1
    attack: 1
    defense: 1
```

Il valore predefinito assegna un punto ai livelli 2, 4, 6…; con `every-levels: 1` si inizia dal livello 2. `points-per-award: 0` disabilita le nuove ricompense; un bonus `per-point` pari a zero disabilita l'assegnazione a quell'attributo. Applica le modifiche con `/classe reload`.

I vecchi profili ricevono una sola volta i punti già maturati secondo il loro livello. Successivamente, le modifiche a frequenza e quantità valgono per i livelli futuri, senza ricalcolare ricompense precedenti. Abbassare e riguadagnare livelli non duplica i punti. Cambio classe, sottoclasse e riconnessione conservano punti e assegnazioni. Modificare `per-point` ricalcola invece il bonus di tutti i punti già spesi.

Ogni totale è `base + crescita × (livello - 1) + punti assegnati × bonus per punto`, entro i limiti previsti. Vita massima 1024; gli altri attributi arrivano a 1.000.000. L'aumento di vita o risorsa massima non cura e non ricarica istantaneamente.

| Attributo totale | Effetto effettivo |
|---|---|
| Forza e attacco | Bonus all'attacco corpo a corpo: `attacco + forza × combat.strength-melee-factor`, massimo 2047 |
| Destrezza | Bonus proporzionale alla velocità d'attacco: `destrezza × combat.dexterity-speed-factor`, massimo +200% |
| Vita | Aumenta la vita massima |
| Mana/risorsa | Aumenta la capacità di Mana, Vigore, Fede o altra risorsa della classe |
| Intelligenza | Aumenta danno, cura e barriera delle skill secondo `intelligence-scale` |
| Difesa | Riduce il danno da entità con `danno × 100/(100+difesa)`, prima delle riduzioni vanilla |

Le stesse formule usano sia la crescita automatica sia i punti assegnati. Il pannello mostra danno e velocità degli attributi Minecraft correnti, inclusi i modificatori applicabili: il danno finale di un colpo dipende anche da ricarica, critici, armatura e bersaglio. La riduzione mostrata riguarda solo la difesa MMO. Non si possono spendere punti quando il relativo contributo ha già raggiunto il limite.

## VFX client e nuovo core

[Configurazione VFX/suoni e aggiornamento](docs/VFX-CLIENT.md). La mod dichiara la propria capacità nel saluto e anima gli eventi inviati dal server, con limiti locali e pulizia al cambio mondo/disconnessione. Il server mantiene tutte le decisioni di combattimento.

[Proposta delle cinque discipline e 40 abilità](docs/PROPOSTA-DISCIPLINE.md): documento di valutazione, non classi già implementate.


