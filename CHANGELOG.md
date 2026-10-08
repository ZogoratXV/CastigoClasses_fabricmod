# Changelog

## 0.1.0-beta.7 — 2026-10-08

- Barra casting dorata con nome skill e riempimento da sinistra a destra.
- Editor F8 rimappabile con selezione classe/skill/fase, parametri grafici/audio e anteprima locale nel mondo.
- Progetti JSON locali e applicazione/ripristino server con risposta esplicita e permesso admin.
- 23 test mod; documentati i limiti rispetto a Photon e il collaudo visivo ancora necessario.

## 0.1.0-beta.6 — 2026-10-07

- Renderer Orison con doppi anelli, colonna verde e scintille ascendenti, agganciato al destinatario.
- Audio OGG originale incluso nel JAR; generatore riproducibile nei sorgenti.
- Parsing UUID e geometria con budget, rimozione su morte/despawn/cambio mondo; 20 test mod.

## 0.1.0-beta.5 — 2026-10-07

- Icone skill PNG da resource pack tramite identificatori texture:namespace:textures/..., nella HUD e nella schermata di riordino.
- Ripiego sull'icona vanilla quando il file manca; nessun download di asset dal protocollo delle skill.
- Guida condivisa per GUI amministrativa, oggetti ItemsAdder, asset e VFX.

## 0.1.0-beta.4 — 2026-10-07

- Motore VFX client per linee, anelli, spirali ed emissioni puntuali, comandato dagli eventi del plugin beta.3.
- Audio posizionale, colore/dimensione/quantità/durata configurabili dal server.
- Validazione degli eventi e budget per animazioni, particelle e suoni; pulizia al cambio mondo/disconnessione.
- Negoziazione clientVfx compatibile con protocollo v1 e server precedenti; HUD e punti attributo conservati.

## 0.1.0-beta.3 — 2026-10-07

- Menu K diviso in Attributi e Disposizione skill, mantenendo il tema medievale.
- Pulsanti di assegnazione, saldo, punti spesi, bonus ed effetti reali ricevuti dal server.
- Compatibilità con stati precedenti senza punti attributo.
- Versione risorse collegata alla versione di build per evitare metadati obsoleti.
- Build e 10 test automatici superati; verifica in gioco da effettuare.

## 0.1.0-beta.2 — 2026-10-06

- HUD compatta medievale fantasy: cornici bronzo/oro, testi pergamena, pannello 172×64 e ritratto 24×24.
- Barra di otto skill ridotta a 192 unità GUI, con riordino adattato e tema coerente nell'editor.
- Cuori, barra esperienza e livello vanilla nascosti quando il sistema classi è attivo.
- Fame spostata nella posizione dei cuori, mantenendo il renderer originale.
- Ripristino dell'HUD vanilla senza stato server; conservati gli altri indicatori contestuali.
- Compatibile con plugin 0.1.0-beta.1, senza modifiche al protocollo.

## 0.1.0-beta.1 — 2026-10-06

- Pannello personaggio con testa della skin, nome, classe, vita, risorsa, livello e gruppo LuckPerms.
- Barra di otto skill alternabile con keybind, indicatori di cooldown e risorsa.
- Editor della disposizione con trascinamento o selezione di due slot, persistenza server.
- Catalogo dinamico e protocollo v1, ripristino della hotbar quando non disponibile.
- Build Minecraft 26.2 / Fabric Loader 0.19.5 / Fabric API 0.161.0+26.2 / Java 25.
