# Volume Dock 1.2 — OnePlus 7T Pro

Control de volumen Android nativo con una cápsula flotante de tres mandos.

## Controles
- **Altavoz superior**: muestra el deslizador original de Android con `ADJUST_SAME` + `FLAG_SHOW_UI`, sin incrementar ni disminuir el audio.
- **Candado central**: habilita / bloquea el arrastre y guarda la posición.
- **Altavoz tachado inferior**: lleva a cero multimedia, timbre, notificaciones, sistema y alarmas cuando el sistema lo permite. No modifica el volumen de llamadas activas o emergencias.

## Permisos y persistencia
- Permiso de superposición; notificaciones en Android 13+; exclusión opcional del ahorro de batería.
- Permiso opcional de control de No molestar para permitir la bajada de timbre a silencio cuando Android lo exija.
- Servicio en primer plano y notificación continua con START_STICKY; arranque al reiniciar si ya estaba habilitado.
- Detener manualmente: Ajustes > Aplicaciones > Volume Dock > Forzar detención.
- Sin root, sin servicios de accesibilidad y sin Internet.

## Importante
- El botón de silencio **también puede silenciar alarmas programadas**. Recuerda restablecerlas cuando las necesites.
- El permiso de No molestar autoriza cambios de timbre pero no significa que Volume Dock active el modo No molestar; modifica volúmenes individuales.
- Android y OxygenOS pueden terminar el servicio; la permanencia absoluta no puede garantizarse.
- Las compilaciones de prueba se firman con un certificado debug generado por el runner. Puede ser necesario desinstalar una versión anterior antes de instalar esta APK, con pérdida de los ajustes locales.

## Instalador
GitHub Actions > Compilar APK de Volume Dock > Artifacts > VolumeDock-OnePlus-APK. El ZIP contiene `app-debug.apk` firmado.
