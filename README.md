# CastigoClasses — mod Fabric

Companion client **0.1.0-beta.1** per [CastigoClasses plugin](https://github.com/ZogoratXV/CastigoClasses_plugin).

## Installazione

- Minecraft **26.2**, Java **25**.
- Fabric Loader **0.19.5** o superiore.
- Fabric API **0.161.0+26.2**.
- Copia `CastigoClasses-Fabric-0.1.0-beta.1.jar` e il JAR di Fabric API nella cartella `mods` del profilo Minecraft 26.2.

Il server rimane Purpur e deve avere CastigoCore e CastigoClasses. Questa mod è solo client.

## Utilizzo

- **R**: alterna hotbar vanilla e barra delle otto abilità.
- **1–8** in modalità abilità: lancia lo slot corrispondente. Si rispettano anche eventuali rimappature dei tasti hotbar vanilla. Il nono slot è inutilizzato.
- In modalità abilità i tasti hotbar e la rotella non cambiano l'oggetto impugnato. Con un menu aperto gli input tornano al menu.
- **K**: apre personaggio, attributi e disposizione delle abilità.
- Trascina una skill su un altro slot, oppure clicca due slot, poi premi **Salva disposizione**. L'ordine viene salvato sul server e ritrovato alla riconnessione.
- R e K sono modificabili in **Opzioni → Comandi → Assegnazione tasti → Castigo Classes**.

Il pannello in alto a sinistra mostra la testa della skin attuale (incluso il secondo strato), nome visualizzato del personaggio, classe, livello, vita, risorsa e gruppo principale LuckPerms, se presente. Il nome e il colore della risorsa arrivano dalla classe: Mana, Vigore, Fede o un'altra etichetta.
Le skill mostrano icone, ricariche, livello richiesto e indisponibilità per risorsa insufficiente. Nel pannello K il passaggio del mouse mostra descrizione, costo e ricarica.

## Classi dinamiche

Il client riceve il catalogo dal server. Nuove classi, sottoclassi, nomi, attributi, icone di oggetti Minecraft, costi e cooldown non richiedono una nuova build della mod. `/classe reload` sul server aggiorna i client collegati.
Le animazioni della beta sono particelle e suoni vanilla inviati dal plugin, quindi anche giocatori senza questa mod li ricevono. Per nuovi renderer, asset esclusivi o interazioni client occorrerà estendere la mod.

La mod invia soltanto la richiesta di attivazione dello slot e l'ordine scelto. Danno, cura, risorsa, sblocchi e cooldown vengono decisi dal server.
In server senza plugin, dopo una disconnessione o in assenza di aggiornamenti per 10 secondi torna disponibile la hotbar vanilla. Gli elementi rispettano F1; la barra abilità è disabilitata in modalità spettatore.

## Compilazione

Java 25 e Gradle Wrapper incluso (9.6.0), Loom 1.17.21.

```powershell
.\gradlew.bat build
```

Linux/macOS: `./gradlew build`. Usa il JAR senza suffisso `-sources` in `build/libs`.

I test controllano riordino, catalogo/stato dinamico, timeout, cooldown ricevuti e codec UTF-8 compatibile con Bukkit. Compilazione e test automatici verificati; aspetto dell'HUD e uso multiplayer richiedono ancora la prova in gioco.
Protocollo: [docs/PROTOCOL.md](docs/PROTOCOL.md). Prove manuali: [docs/TEST-IN-GIOCO.md](docs/TEST-IN-GIOCO.md).