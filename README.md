# Volume Dock 1.3 — OnePlus 7T Pro

Cápsula flotante Android con **solo dos botones**. Actualización solicitada por el propietario: se retira el botón de silencio y toda autorización de No molestar.

## Controles
- **Altavoz (superior):** muestra el deslizador de volumen nativo de Android con `AudioManager.ADJUST_SAME` y `FLAG_SHOW_UI`, sin variar el volumen.
- **Candado (inferior):** bloquea o desbloquea la posibilidad de arrastrar la cápsula. Su posición se guarda.
- Nueva cápsula compacta **64 × 128 dp** (antes 64 × 192 dp).

## Funcionamiento
- Servicio Android en primer plano con notificación y `START_STICKY`.
- Inicio al arrancar el terminal si se había activado y Android lo permite.
- Superposición, notificaciones en Android 13+ y exclusión opcional de ahorro de batería.
- Sin permiso de No molestar, sin root, sin accesibilidad, sin Internet y sin botón para cerrar.
- Para detener la aplicación: Ajustes del dispositivo > Aplicaciones > Volume Dock > Forzar detención.

## Limitaciones
- Android/OxygenOS pueden detener un servicio incluso si está en primer plano; no es posible prometer persistencia absoluta.
- Instalador debug firmado automáticamente en cada runner de GitHub Actions. La firma puede cambiar entre compilaciones, de modo que Android podría exigir desinstalar la versión anterior antes de instalar una nueva (con pérdida de ajustes internos).
- Compatibilidad real con el OnePlus 7T Pro pendiente de verificación mediante prueba en el dispositivo.

## Instalador
GitHub Actions → Compilar APK de Volume Dock → Artifacts → VolumeDock-OnePlus-APK → app-debug.apk.
