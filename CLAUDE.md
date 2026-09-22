# OVERSEER — project memory

OVERSEER is a Wear OS watch face that acts as a status "hub" for a small
constellation of sibling apps, all built by the same author as part of a
personal accessibility/biofeedback toolkit. The sibling apps are installed as
**standalone Wear OS apps on the same physical watch** as OVERSEER — most of
the communication below is same-device Android IPC (broadcasts), not
phone↔watch sync. Two of the siblings also ship a phone module that talks to
the watch over Google's Wearable Data Layer API; that's a separate channel
from the same-device broadcasts.

This file was compiled 2026-09-22 by reading this repo plus read-only clones
of the three sibling repos that were locatable on GitHub (`ASPDesignLabs`
org). It's a snapshot — the sibling repos are independently developed and can
drift out of sync with what's documented here.

## Modules in this repo

- `app` (`com.snakesan.overseer`) — the Wear OS watch face. Everything under
  `app/src/main/java/com/snakesan/overseer/presentation/`. This is the "hub"
  that receives/sends everything described below.
- `app/overseermobile` (`com.snakesan.overseermobile`) — a phone companion,
  own separate package, for configuring the watch face's 3-wedge layout.
  **Not yet wired into the sibling-app data flow** — see Known gaps.

## Data model

### Watch: `OverseerState` (`presentation/SystemState.kt`)
Single `@Composable`-observable state object (one per Activity), backed by
SharedPreferences `overseer_cache`. Fields, grouped by origin:
- Local hardware polling: `powerUsage`, `ramUsage`, `batLevel` (5s poll while
  interactive).
- UI config: `bgMode` (0-2 background render style), `colorHoldSeconds`
  (20-30s screen-on timer).
- Vitality: `hpLevel`, `hydStatus`, `mealStatus`, `overcharge`,
  `vitalityOffline`.
- ACK: `ackDeckName`, `ackColorInt`, `ackTargetName`, `ackOffline`,
  `targetMap: Map<Int, String>`.
- Flux: `fluxMode`, `fluxActive`, `fluxOffline`, `fluxProfile`,
  `fluxCustomBank`, `fluxCustomName`, `fluxBpm`, `fluxIntensity`,
  `fluxSleepMode`, `fluxMonochrome`, `fluxHdr`, plus 5 ARGB color ints
  (primary/secondary/l1/l2/background).

### Phone: `OverseerMobileConfig` (`overseermobile/data/WedgeModels.kt`)
- Exactly 3 `WedgeSlot(position 0..2, content)`.
- `WedgeContent` is a sealed class: `Function(WedgeFunction)` where
  `WedgeFunction` ∈ {VITALITY, ACK, FLUX}, or `Shortcut(packageName, label,
  color)` for an arbitrary launchable app.
- Persisted as JSON via `ConfigRepository` (SharedPreferences
  `overseermobile_config`).

## Transfer mechanisms

There's no single "transfer manager" class — four distinct channels:

**1. Inbound status broadcast (siblings → Overseer watch app).**
Same-device broadcast, actions `com.snakesan.overseer.UPDATE_STATUS` and
`com.snakesan.overseer.SYNC_TARGETS`. Received by a `BroadcastReceiver`
dynamically registered in `rememberOverseerState()` (`SystemState.kt`),
dispatched on a String extra `source_app`. See the extras table below.

**2. Outbound control broadcast (Overseer → ACK only).**
`com.snakesan.overseer.ACK_CONTROL`, `Intent.setPackage("com.example.besu")`,
String extra `CMD` ∈ `NEXT_DECK` / `PREV_DECK` / `SET_TARGET:<idx>` (`-1`
clears) / `CRYO_TOGGLE`. Sent from `AckControlOverlay` (`Overlays.kt`), driven
by the watch's crown/tap UI.

**3. Outbound kill-switch broadcast (Overseer → one or all siblings).**
`com.snakesan.overseer.KILL_COMMAND`, extra `target_package`; `setPackage`
is set to that target, or left `null` for "ALL". Sent from
`MainActivity.performKill()`, triggered by the watch face's emergency-stop
long-press (center = ALL, outer sector = that sibling).

**4. Wearable Data Layer API (Google Play Services) — phone ⟷ watch, not
same-device.** This is a genuinely different transport from 1–3:
- `DataRepository.observeInt`/`observeString` (watch, `DataRepository.kt`) —
  generic `DataClient` path/key observer with "fetch on wake + live listener"
  semantics. **Dead code — no callers anywhere in this repo.**
- `FluxLinkRepository` (watch) — `MessageClient` listener on path
  `/flux_overseer_state`, decodes a custom versioned binary packet directly
  into `OverseerState` (layout below). Fed by NeonFlux's **phone** module,
  not its watch module.
- `WatchSync.push()` (phone, `overseermobile/data/WatchSync.kt`) — pushes the
  wedge config as a `DataMap` array to path `/overseer/wedge_config`,
  schema-versioned. **The watch does not read this path yet** (per its own
  doc comment) — editing wedges on the phone has no effect on the watch
  today.

### Flux binary packet — `/flux_overseer_state` (MessageClient)
Version 1, minimum 33 bytes, decoded in `FluxLinkRepository.applyFluxState()`:

| Field | Type |
|---|---|
| version | byte (must be `1`) |
| isRunning | byte (bool) |
| profile | byte (0=PULSE, 1=GEIGER, 2=THROB, 3=CUSTOM) |
| customBank | byte |
| bpm | int32 |
| intensity | byte |
| sleepMode | byte (bool) |
| monochrome | byte (bool) |
| hdr | byte (bool) |
| primaryColor, secondaryColor, layer1Color, layer2Color, backgroundColor | int32 ARGB, one each |
| nameLength | byte |
| name | `nameLength` UTF-8 bytes |

### `UPDATE_STATUS` / `SYNC_TARGETS` extras by `source_app`

| `source_app` | Extras Overseer reads |
|---|---|
| `VITALITY` | `hp` (Int), `hyd_status` (Int: 0 low / 1 nominal / 2 hi, `-1`="absent"), `meal_status` (Int: 0 none / 1 prep / 2 requested, `-1`="absent"), `overcharge` (Int) |
| `ACK` | `active_deck` (String), `deck_color` (Int ARGB), `active_target` (String) |
| `ACK_SENSOR` | `sensor_state` (String: ARMED/LOCKED/CRYO/IDLE/other), `sensor_pose` (String), `active_target` (String) |
| `ACK_LIST_SYNC` | `raw_targets` (String, `idx:label|idx:label|...`) |
| `FLUX` | `flux_mode` (String), `is_active` (Bool), `flux_profile` (Int), `flux_custom_bank` (Int), `flux_custom_name` (String), `flux_bpm` (Int), `flux_intensity` (Int), `flux_sleep_mode`/`flux_monochrome`/`flux_hdr` (Bool), 5× `flux_*_color` (Int ARGB) |

## Sibling apps

Package → repo, resolved via the `ASPDesignLabs` GitHub org:

| Constant | Package | Repo | What it is |
|---|---|---|---|
| `PKG_VITALITY` | `com.snakesan.vitalitysys` | `ASPDesignLabs/VITALITYSYS` | Gamified ADL/wellness tracker |
| `PKG_ACK` | `com.example.besu` | `ASPDesignLabs/ACK` | "Besu" — deck/target/sensor control |
| `PKG_NEON` | `com.snakesan.neonflux` | `ASPDesignLabs/NEONFLUX` | Haptic biofeedback ("Flux") — **not** a lighting controller |
| `PKG_NEURO` | `com.snakesan.cyberquake` | *not found* | Launch-only shortcut, no telemetry; no matching repo among currently-accessible ASPDesignLabs repos |

### VitalitySys (`ASPDesignLabs/VITALITYSYS`)
Two modules sharing package `com.snakesan.vitalitysys`: `VITALITYDECK`
(standalone Wear OS app — this is Overseer's actual peer) and
`VITALITYMOBILE` (phone, Health Connect + Room; unrelated to Overseer).

- `hp` (0-100): synthetic health meter fed by 4 tracked protocols — NUTRIENT
  (meals vs. schedule), CHEMISTRY (medication doses), HYDRATION (water
  intake), MAINTENANCE (hygiene/self-care) — with a "Bio-Drift" penalty when
  more than one protocol is failing at once.
- `hyd_status` / `meal_status`: literal hydration-pace and meal-timing
  status; values match Overseer's assumptions exactly. VitalitySys always
  sends both, so Overseer's `-1` ("extra absent") sentinel never actually
  fires in practice.
- `overcharge` (0-50): reward meter that builds over ~60 min while `hp`
  holds at 100 and resets the instant `hp` drops — computed redundantly on
  both the watch and phone, consistent with Overseer's own client-side
  "zero it if hp < 100" rule.
- Sent from three places: `MainActivity.broadcastToOverseerLocal()` (primary
  path — resume/config-change/every 60s), `SentinelWorker` (15-min
  WorkManager tick — **omits `overcharge`**, an internal inconsistency, not
  an Overseer-contract break), and `VitalityDataService` (relays a
  phone-pushed Data Layer message into the same broadcast).
- Listens for `KILL_COMMAND` in `MainActivity` — **ignores `target_package`**,
  reacts to the action alone: cancels WorkManager, detaches its
  `MessageClient`, force-exits.
- Never sends `SYNC_TARGETS`.

### ACK / "Besu" (`ASPDesignLabs/ACK`)
Two modules: `:app` (phone, `com.example.besu`) and `:wear` (watch, same
`applicationId` despite its Kotlin `namespace`/package being
`com.example.besu.wear`, with source files misfiled under a stale
`com.snakesan.wear.presentation` directory — cosmetic only). The `:wear`
module is Overseer's peer.

- "Deck" = a named output profile (phone's `CommandRepository`/`DeckMeta` is
  source of truth). "Target" = an addressee slot (phone's
  `TargetRepository`/`TargetSlot`). "CRYO" = sensor listeners fully
  unregistered (`enterCryo`).
- **Gap:** `source_app=ACK` broadcasts (`MainActivity.broadcastDeckToHUD`,
  `WearConfigListenerService.broadcastToOverseer`) never include
  `active_target` — Overseer just keeps whatever it last cached, so this is
  silent, not broken.
- **Bug:** `source_app=ACK_SENSOR` emits `sensor_pose="POSE_LOCKED"` (the raw
  enum name), but Overseer's color logic (`SystemState.kt`) special-cases the
  literal string `"LOCKED"` — that branch can never fire, so it silently
  falls back to the cached deck color instead of turning green.
- `ACK_CONTROL` and `KILL_COMMAND` are both handled inside
  `BackgroundSensorService`, which registers its receiver dynamically in
  `onCreate()` — **but the code that starts this service from
  `MainActivity.onCreate` is commented out**, so on a fresh install neither
  channel may be listening until some other flow (pose tracking, cryo, etc.)
  starts the service once.
- `KILL_COMMAND` handling ignores `target_package` (unconditional exit), same
  pattern as VitalitySys — correctness rests entirely on Android's own
  `setPackage` targeting, not on the receiver checking the extra.

### NeonFlux / "Flux" (`ASPDesignLabs/NEONFLUX`)
One package `com.snakesan.neonflux`, two modules: `:app` (watch) and
`:neonfluxmobile` (phone, bundles the watch APK for one Play listing).

**This is a haptic/vibration biofeedback app, not a lighting controller.**
`profile` 0-3 = vibration-motor waveform (PULSE/GEIGER/THROB/CUSTOM
sequencer bank); `bpm` = metronome rate; `intensity` = motor amplitude;
`sleepMode` = suppresses screen/ambient sensors during playback.
`monochrome`/`hdr`/the 5 colors are purely NeonFlux's own on-screen neon
theme (kept in sync between its phone and watch modules for its own UI) —
**not** physical lights, despite the field names.

The two transport channels carry very different amounts of data:
- The broadcast (`source_app=FLUX`, sent only from the **watch** module's
  `FluxService.updateState()`, labeled "Legacy Bridge" in that code) carries
  only `flux_mode` (a display string like `"CLINICAL (60 BPM)"`, not the
  `MODE_PULSE`-style name), `is_active`, and an undocumented `halt_reason`
  Overseer never reads.
- The full state (profile, bank, bpm, intensity, sleep/mono/hdr, all 5
  colors, custom name) only arrives via the binary MessageClient packet,
  sent from NeonFlux's **phone** module
  (`neonfluxmobile/.../MainActivity.sendOverseerFluxState()`) — byte layout
  matches Overseer's decoder exactly, field for field.
- **Practical implication:** on a watch-only setup — no phone, or the
  phone's NeonFlux app not installed/running — Overseer only ever gets
  `fluxMode`/`fluxActive` from the legacy broadcast; profile, colors, BPM,
  etc. stay at whatever was last cached (or their defaults).

NeonFlux's manifest declares a signature-level permission
`com.snakesan.neonflux.permission.OVERSEER_CONTROL` guarding its
`KILL_COMMAND` receiver. **Overseer's own manifest
(`app/src/main/AndroidManifest.xml`) does not request this permission** — so
Overseer's emergency-stop broadcast may silently fail to reach NeonFlux.
Worth verifying on-device if that kill path matters in practice.

Never sends `SYNC_TARGETS`.

### Cyberquake / "Neuro" (unresolved)
`PKG_NEURO = com.snakesan.cyberquake` appears in Overseer's `<queries>`
manifest block and as a "NEURO" launcher shortcut in `LauncherOverlay` — but
it has no `source_app` in the broadcast contract and no fields in
`OverseerState`. It's launch-only today. No repo matching this package
turned up among the ASPDesignLabs repos this session could see — if you have
its actual repo name/location, attach it and this section can be filled in.

## Known gaps / incomplete wiring

1. `DataRepository` (watch, `DataClient` observeInt/observeString) has zero
   callers — dead scaffold code.
2. `WatchSync`'s wedge-config push (`/overseer/wedge_config`) is one-way and
   unconsumed — the watch doesn't read this path, so the phone's wedge
   editor currently has no effect on the watch face.
3. NeonFlux's broadcast channel is far thinner than its MessageClient
   channel — most Flux telemetry depends on the phone app being installed
   and connected (see NeonFlux section above).
4. ACK: `active_target` is missing from `source_app=ACK` broadcasts, and the
   `sensor_pose` value mismatch means the "LOCKED" color state can never
   render.
5. ACK: the service that owns `ACK_CONTROL`/`KILL_COMMAND` may not start on
   a fresh install (its startup call is commented out in `MainActivity`).
6. Overseer never requests NeonFlux's `OVERSEER_CONTROL` permission, which
   may block Overseer's kill-switch broadcast from reaching it.
7. Neither VitalitySys nor ACK validate `target_package` on `KILL_COMMAND` —
   both exit unconditionally on receipt; correctness currently depends
   entirely on Overseer's `Intent.setPackage` targeting being correct.
