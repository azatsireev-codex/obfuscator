# GOOLak (autonomous)

GOOLak is a standalone Spigot plugin with antivirus-oriented architecture.

Currently implemented feature module:
- `features.xray`: MAP_CHUNK interception via ProtocolLib with direct payload rewrite (`packet.setData(...)` style).

## Build

From repository root:

```bash
mvn -f lightxray-plugin/pom.xml -DskipTests package
```

No dependency on `orebfuscator-*` modules is required for build or runtime.

## Runtime requirements

- Spigot/Paper/Folia server
- ProtocolLib

## XRay mode notes

- GOOLak rewrites chunk packet section data directly in the MAP_CHUNK payload.
- `features.xray.boundaryOnly: false` — process full chunk area (better protection).
- `features.xray.boundaryOnly: true` — process only chunk boundaries (lighter mode).
- `features.xray.replacementMaterial` must exist in a section being rewritten.

## Network strategy

- Uses payload-level replacement during MAP_CHUNK interception.
- Does not depend on post-send block-update bursts (`sendBlockChange` / `sendMultiBlockChange`).
