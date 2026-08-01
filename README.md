# BluetoothJammer — Edición Mejorada

Herramienta de **investigación educativa** sobre seguridad Bluetooth para Android (Kotlin / Material Components XML / Material 3).

Versión mejorada del proyecto original [eikarna/BluetoothJammer](https://github.com/eikarna/BluetoothJammer), con detección real de dispositivos cercanos, clasificación de altavoces y múltiples técnicas de ataque seleccionables.

---

## ⚠️ DISCLAIMER LEGAL

> **ESTA APLICACIÓN ES SOLO PARA FINES EDUCATIVOS Y DE INVESTIGACIÓN.**
>
> - Úsala **ÚNICAMENTE con dispositivos de tu propiedad** y dentro de tu propio entorno.
> - **Interferir, atacar o degradar dispositivos que no te pertenecen es ilegal** en la mayoría de jurisdicciones (FCC en EE. UU., normativa europea, legislación local, etc.) y puede constituir un delito.
> - La aplicación incluye un **aviso obligatorio al abrirse** y recordatorios al iniciar cualquier ataque.
> - El desarrollador, colaboradores y mantenedores **no se hacen responsables del uso indebido** de esta herramienta. El uso correcto o incorrecto es responsabilidad exclusiva del usuario final.
> - Esta herramienta **no** es un jammer de radiofrecuencia: un teléfono no puede emitir RF arbitraria. Implementa técnicas de denegación de servicio a nivel de protocolo Bluetooth (L2CAP/RFCOMM, GATT, SDP, advertising BLE) dentro de los límites del SDK de Android.

---

## Novedades de esta versión

- **Detección real de dispositivos cercanos** (antes solo listaba los ya emparejados).
- **Clasificador de altavoces** con 3 señales combinadas (clase Bluetooth, appearance BLE, nombre).
- **5 técnicas de ataque seleccionables** tras elegir el objetivo.
- **Botón Stop funcional**: los ataques ahora se detienen de verdad (antes requería forzar el cierre de la app).
- **Aviso educativo obligatorio** al abrir la aplicación.
- **UI renovada**: botón Escanear, filtro "Solo altavoces", línea de estado, badges 🔊, RSSI y tipo de dispositivo.
- **Correcciones**: buffer de 0 bytes en el flood L2CAP (no escribía nada en RFCOMM), imports correctos del paquete `android.bluetooth.le`, permisos y compatibilidad API 24–34.

---

## Funcionalidades

### 1. Detección de dispositivos (`ScanNearbyDevices`)

Combina **tres fuentes** en una sola lista deduplicada por dirección MAC:

- **Emparejados** (`bondedDevices`) — siempre visibles como ancla.
- **Discovery clásico** (`startDiscovery` + `ACTION_FOUND`) — captura nombre, clase de dispositivo y RSSI.
- **Escaneo BLE** (`BluetoothLeScanner`, modo low-latency) — captura nombre, RSSI y el campo *appearance* del `ScanRecord`.

Los resultados se ordenan con los altavoces primero y luego por cercanía (RSSI), y se entregan por eventos (sin el antiguo polling de 1 segundo).

### 2. Clasificador de altavoces (`SpeakerClassifier`)

Determina si un dispositivo es probablemente un altavoz, combinando señales de mayor a menor fiabilidad:

| Señal | Fuente | Confianza |
|---|---|---|
| Clase Bluetooth | `BluetoothClass` major Audio/Video + minor (Altavoz, Hi-Fi, Pantalla+altavoz) | Alta |
| Clase Bluetooth | Audio portátil / Audio de coche | Media |
| Appearance BLE | Campo GAP `0x0017` (Generic Speaker) | Alta |
| Nombre | Heurística de keywords (JBL, Sonos, soundbar, echo, …) | Media |

Los auriculares quedan **excluidos explícitamente**. La UI muestra un badge 🔊 con la razón de la clasificación ("Clase BT", "BLE", "Nombre: …").

### 3. Técnicas de ataque (`AttackType` / interfaz `BluetoothAttack`)

Se eligen con un selector (Spinner) una vez seleccionado el objetivo. El campo **Threads** actúa como intensidad (workers/concurrencia según el tipo).

| Tipo | Capa | Descripción | Intensidad |
|---|---|---|---|
| **L2CAP Flood (clásico)** | RFCOMM/L2CAP | Abre sockets RFCOMM con UUIDs aleatorios y satura el socket conectado. | 1–64 workers |
| **GATT Flood (BLE)** | GATT | Llena la tabla de conexiones GATT del periférico (la mayoría solo acepta unas pocas). | 4–64 conexiones paralelas |
| **Pairing Flood** | Bonding | Inunda al objetivo de solicitudes de emparejamiento (spam de diálogos). | 1–3 workers (el stack serializa) |
| **SDP Query Storm** | SDP | Satura el servidor SDP con consultas de servicios repetidas. | 1–16 consultas concurrentes |
| **Advertising Flood (BLE)** | Advertising | Contamina el canal de anuncios BLE con UUIDs aleatorios (requiere soporte de advertising BLE). | n/a |

Todos los ataques:

- Corre sobre coroutines en `Dispatchers.IO` con su propio flag de ejecución.
- Reportan progreso al log de la app (activable/desactivable con el switch Log).
- **Se detienen limpiamente** con el botón Stop: cancela el scope y cierra sockets/conexiones.

### 4. Seguridad y UX

- **Diálogo de aviso educativo** obligatorio (no cancelable) al abrir la app.
- **Toast recordatorio** ("úsalo solo con dispositivos de tu propiedad") al iniciar cada ataque.
- Log con marcas de tiempo (`Logger`) y límite de 100 líneas.

---

## Requisitos

- **Android**: minSdk 24 (Android 7.0), targetSdk 34.
- **Permisos** (declarados en el manifest): `BLUETOOTH`, `BLUETOOTH_ADMIN`, `BLUETOOTH_SCAN`, `BLUETOOTH_ADVERTISE`, `BLUETOOTH_CONNECT`, `ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION`.
- **Compilación**: JDK 17, Android SDK platform 34 + build-tools 34.0.0, Gradle 8.7 (wrapper incluido), AGP 8.6.0, Kotlin 1.9.0.

### Compilar

```bash
./gradlew :app:assembleDebug
# APK de salida:
#   app/build/outputs/apk/debug/app-debug.apk
```

### Tests unitarios

```bash
./gradlew :app:testDebugUnitTest
# 8 tests (clasificador de altavoces)
```

### Nota para ARM64 (Termux / aarch64)

AGP 8.6 distribuye `aapt2` compilado solo para **x86-64**, que no ejecuta en ARM64. Para compilar en un dispositivo ARM64:

1. Instala el paquete nativo de Termux: `pkg install aapt2`
2. Añade en `~/.gradle/gradle.properties`:

```properties
android.aapt2FromMavenOverride=/data/data/com.termux/files/usr/bin/aapt2
```

3. El shebang de `gradlew` (`#!/usr/bin/env sh`) no resuelve en Termux; ejecútalo con `sh gradlew`.

---

## Instalación

1. Compila o descarga el APK debug (`app/build/outputs/apk/debug/app-debug.apk`).
2. Copia el APK a tu dispositivo (p. ej. `~/storage/downloads/` en Termux).
3. Ábrelo con el gestor de archivos y permite "instalar aplicaciones desconocidas".
4. Al abrir la app: acepta el aviso educativo y concede los permisos de Bluetooth/ubicación cuando se soliciten.

---

## Estructura del proyecto

```
app/src/main/java/com/eikarna/bluetoothjammer/
├── MainActivity.kt            # Detección, lista de dispositivos, aviso educativo
├── AttackActivity.kt          # Selección de objetivo, tipo de ataque y control Start/Stop
├── api/
│   ├── BluetoothAttack.kt     # Interfaz común + enum AttackType (selector)
│   ├── ScanNearbyDevices.kt   # Motor de escaneo (emparejados + clásico + BLE)
│   ├── SpeakerClassifier.kt   # Clasificador de altavoces (clase BT/appearance/nombre)
│   ├── AttackDevices.kt       # L2capFloodAttack (flood RFCOMM/L2CAP)
│   ├── GattFloodAttack.kt     # Flood de conexiones GATT (BLE)
│   ├── PairingFloodAttack.kt  # Flood de emparejamiento
│   ├── SdpFloodAttack.kt      # Tormenta de consultas SDP
│   └── AdvertiseFloodAttack.kt# Flood de advertising BLE
└── util/Logger.kt             # Log con timestamp
```

---

## Limitaciones conocidas

- **Identificación de altavoces** depende de lo que publique cada fabricante (clase/appearance); los dispositivos que no publican metadata solo se detectan por heurística de nombre.
- **Advertising Flood** requiere que el hardware soporte advertising BLE (`isMultipleAdvertisementSupported`) y falla si el radio está ocupado.
- **No es un jammer de RF**: no puede interferir físicamente en la banda de 2.4 GHz; eso requiere SDR (HackRF/ESP32) fuera del alcance de un teléfono.
- El SDK de Android no expone L2CAP a PSMs arbitrarios ni permite suplantar MAC en advertising; las técnicas se limitan a lo que la API pública permite.
- La detección y la efectividad dependen del hardware del teléfono (antena, alcance).

---

## Créditos

- Repositorio original: [eikarna/BluetoothJammer](https://github.com/eikarna/BluetoothJammer)
- Inspiración y asistencia de desarrollo: ChatGPT-4o (repo original) y herramientas de desarrollo asistido (esta edición).

---

*Este proyecto se publica con fines educativos. Respeta la privacidad y la propiedad de los demás.*
