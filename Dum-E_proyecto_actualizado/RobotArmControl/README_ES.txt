ROBOT ARM CONTROL - Proyecto Android

Incluye:
- Interfaz estilo mando, radar verde fijo y punto rojo para el eco detectado.
- Conexión Bluetooth clásico SPP para HC-05 ya emparejado.
- Botones para base, brazo y pinza.
- Android 12/13: permiso BLUETOOTH_CONNECT.

IMPORTANTE:
Este es el PROYECTO FUENTE, no un APK ya compilado.
Para compilar sin instalar Android Studio:
1. Crea una cuenta en GitHub.
2. Crea un repositorio nuevo vacío.
3. Sube todos los archivos/carpetas de este proyecto conservando su estructura.
4. Abre la pestaña Actions.
5. Ejecuta "Build Android APK" (Run workflow).
6. Cuando termine, abre la ejecución y descarga el artifact "RobotArmControl-debug".
7. Dentro del ZIP estará app-debug.apk. Pásalo al Android e instálalo.

Antes de conectar:
- Empareja HC-05 desde Ajustes > Bluetooth del teléfono (PIN frecuente 1234 o 0000; depende del módulo).
- Abre la app y concede permiso Bluetooth.
- Pulsa CONECTAR HC-05.

PROTOCOLO QUE LA APP ENVÍA (una línea por orden):
BASE_LEFT
BASE_RIGHT
ARM_UP
ARM_DOWN
GRIP_OPEN
GRIP_CLOSE

PROTOCOLO QUE LA APP RECIBE:
La placa debe enviar la distancia en centímetros como número seguido de salto de línea.
Ejemplo: 47.5\n

NOTA DE SEGURIDAD:
No conectes los servos al pin 5V del Arduino para alimentarlos. Usa una fuente regulada de 5V suficiente para los servos y une GND de la fuente con GND del Arduino. No uses las baterías rectangulares de 9V directamente en los servos.
Los comandos son iniciales; ajustaremos el programa Arduino a tu montaje y comprobaremos límites/seguridad antes de mover motores.
