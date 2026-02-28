# LightXRay (autonomous)

LightXRay is a standalone Spigot plugin module that intercepts `MAP_CHUNK` packets via ProtocolLib.

## Build

From repository root:

```bash
mvn -f lightxray-plugin/pom.xml -DskipTests package
```

No dependency on `orebfuscator-*` modules is required for build or runtime.

## Runtime requirements

- Spigot/Paper server
- ProtocolLib
