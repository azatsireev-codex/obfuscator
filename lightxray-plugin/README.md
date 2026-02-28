# GOOLak (autonomous)

GOOLak is a standalone Spigot plugin with antivirus-oriented architecture.

Currently implemented feature module:
- `features.xray`: MAP_CHUNK interception via ProtocolLib.

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

- `features.xray.boundaryOnly: false` — scan full chunk area (better protection).
- `features.xray.requireEnclosed: true` — mimic enclosed-block style filtering.
- `features.xray.checkViewCone: false` — do not skip blocks in front of player (stronger protection).
