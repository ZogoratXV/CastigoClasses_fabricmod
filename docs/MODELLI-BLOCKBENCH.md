# Mappatura modelli Blockbench

Gli ID sono quelli delle skill base ereditate. I file compilati sono in `assets/castigoclasses/models/blockbench/`; le texture in `assets/castigoclasses/textures/blockbench/`.

| Skill ID | Modello |
|---|---|
| `mago_bianco_orison` | `01_orison_di_cura` |
| `mago_bianco_dardo_consacrato` | `02_dardo_consacrato` |
| `mago_bianco_benedizione` | `03_benedizione_rigenerante` |
| `mago_bianco_purificazione` | `04_purificazione` |
| `mago_bianco_vincolo_custode` | `05_vincolo_del_custode` |
| `mago_bianco_luce_respingente` | `06_luce_respingente` |
| `mago_bianco_intercessione` | `07_grande_intercessione` |
| `mago_bianco_santuario` | `08_santuario_di_luce` |
| `mago_nero_dardo_maledetto` | `09_dardo_maledetto` |
| `mago_nero_malocchio` | `10_malocchio` |
| `mago_nero_consunzione` | `11_consunzione` |
| `mago_nero_terrore` | `12_terrore_sussurrato` |
| `mago_nero_cerchio_rovina` | `13_cerchio_della_rovina` |
| `mago_nero_sigillo_tormento` | `14_sigillo_del_tormento` |
| `mago_nero_patto_sangue` | `15_patto_del_sangue` |
| `mago_nero_vortice_tenebre` | `16_vortice_delle_tenebre` |
| `guerriero_scudo_affondo_disciplinato` | `17_affondo_disciplinato` |
| `guerriero_scudo_urto_scudo` | `18_urto_di_scudo` |
| `guerriero_scudo_guardia_salda` | `19_guardia_salda` |
| `guerriero_scudo_passo_guardiano` | `20_passo_del_guardiano` |
| `guerriero_scudo_difesa_compagno` | `21_difesa_del_compagno` |
| `guerriero_scudo_contrattacco` | `22_contrattacco_del_veterano` |
| `guerriero_scudo_riscossa` | `23_riscossa` |
| `guerriero_scudo_baluardo` | `24_baluardo` |
| `guerriero_due_mani_fendente_pesante` | `25_fendente_pesante` |
| `guerriero_due_mani_affondo_lungo` | `26_affondo_lungo` |
| `guerriero_due_mani_carica` | `27_carica_travolgente` |
| `guerriero_due_mani_arresto` | `28_colpo_di_arresto` |
| `guerriero_due_mani_circolare` | `29_fendente_circolare` |
| `guerriero_due_mani_falciata` | `30_falciata_bassa` |
| `guerriero_due_mani_spezzaguardia` | `31_spezzaguardia` |
| `guerriero_due_mani_sequenza` | `32_sequenza_del_campione` |
| `arciere_tiro_preciso` | `33_tiro_preciso` |
| `arciere_passo_cacciatore` | `34_passo_del_cacciatore` |
| `arciere_occhio_cacciatore` | `35_occhio_del_cacciatore` |
| `arciere_disimpegno` | `36_disimpegno` |
| `arciere_tiro_ostacolante` | `37_tiro_ostacolante` |
| `arciere_tiro_copertura` | `38_tiro_di_copertura` |
| `arciere_doppio_tiro` | `39_doppio_tiro` |
| `arciere_colpo_maestro` | `40_colpo_del_maestro` |

## Importazione
Eseguire `python tools/import_blockbench_vfx.py ARCHIVIO.zip` dalla mod. Il file originale è il pacchetto VFX_RPG_40_modelli_Blockbench.zip fornito dall’utente. Il convertitore non esegue file dell’archivio. Non serve copiare i bbmodel in ItemsAdder.

Il protocollo negozia `blockbenchVfx: 1`. I pacchetti VFX aggiungono `model` (ID e fase), `modelTicks`, opzionalmente `modelDelay` e `modelAnchorHeight`. I client precedenti ricevono la presentazione precedente; installare entrambe le beta.17 per usare gli asset nuovi. I custom PNG possono ancora essere sostituiti con un resource pack usando gli stessi percorsi.

I materiali additive sono convertiti al passaggio emissivo trasparente esistente. Gerarchie e pivot sono conservati; la traslazione di volo delle scene campione è sostituita dal percorso server. Riferimento per le trasformazioni: https://github.com/JannisX11/blockbench/blob/master/js/animations/timeline_animators.js
