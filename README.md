# Ejercicio 5: Datagramas UDP

## Estructura del Proyecto

```
src/main/java/arsw/udp/
├── DatagramTimeServer.java    # Servidor de hora
└── DatagramTimeClient.java    # Cliente de hora
```

## Objetivo del Ejercicio

"Utilizando Datagramas escriba un programa que se conecte a un servidor que responde la hora actual en el servidor. El programa debe actualizar la hora cada 5 segundos según los datos del servidor. Si una hora no es recibida debe mantener la hora que tenía. Para la prueba se apagará el servidor y después de unos segundos se reactivará. El cliente debe seguir funcionando y actualizarse cuando el servidor esté nuevamente funcionando."

## Los Códigos Originales (Figuras 5 y 6 del tutorial)

### DatagramTimeServer original (Figura 5)

```java
public void startServer() {
    byte[] buf = new byte[256];
    try {
        DatagramPacket packet = new DatagramPacket(buf, buf.length);
        socket.receive(packet);
        
        String dString = new Date().toString();
        buf = dString.getBytes();
        
        InetAddress address = packet.getAddress();
        int port = packet.getPort();
        packet = new DatagramPacket(buf, buf.length, address, port);
        socket.send(packet);
        
    } catch (IOException ex) {
        Logger.getLogger(DatagramTimeServer.class.getName()).log(Level.SEVERE, null, ex);
    }
    socket.close();
}
```

¿Qué hace este código?:

- Espera a que llegue un paquete
- Obtiene la dirección y puerto de quien envió el paquete
- Envía la hora actual a esa dirección y puerto
- Cierra el socket y termina

**Problema**: Solo atiende UNA petición y se muere. Para el ejercicio necesitamos un servidor que siga funcionando para poder apagarlo y prenderlo mientras el cliente sigue corriendo.

### DatagramTimeClient original (Figura 6)

```java
DatagramSocket socket = new DatagramSocket();
byte[] buf = new byte[256];
InetAddress address = InetAddress.getByName("127.0.0.1");
DatagramPacket packet = new DatagramPacket(buf, buf.length, address, 4445);
socket.send(packet);

packet = new DatagramPacket(buf, buf.length);
socket.receive(packet);

String received = new String(packet.getData(), 0, packet.getLength());
System.out.println("Date: " + received);
```

¿Qué hace este código?:

- Envía un paquete vacío al servidor
- Espera la respuesta
- Muestra la hora recibida
- Termina

**Problemas**:

- Solo hace UNA petición, no cada 5 segundos
- Si el servidor no responde, se queda esperando PARA SIEMPRE (el programa se cuelga)
- No guarda la última hora si hay error

## Por qué hubo que modificar los códigos

El enunciado pide específicamente:

- "actualizar la hora cada 5 segundos" → El cliente original solo hace una petición
- "Si una hora no es recibida debe mantener la hora que tenía" → El original no guarda nada, si no hay respuesta se queda esperando
- "se apagará el servidor y después de unos segundos se reactivará" → Para esto el servidor debe poder arrancarse y detenerse varias veces
- "El cliente debe seguir funcionando" → El original termina después de la primera petición

## Explicación de los Cambios

1. **En DatagramTimeServer: Loop infinito**

   Código original:

   ```java
   socket.receive(packet);
   // responder
   socket.close();  // El servidor muere aquí
   ```

   Código modificado:

   ```java
   while (true) {
       socket.receive(packet);
       // responder
       // No se cierra el socket
   }
   ```

   **Por qué**:

   - El servidor necesita estar siempre vivo para recibir múltiples peticiones
   - Si se cerrara después de cada respuesta, habría que reiniciarlo manualmente cada vez
   - El `while(true)` permite que el servidor atienda peticiones indefinidamente hasta que lo matemos con Ctrl+C

2. **En DatagramTimeClient: Timer para peticiones cada 5 segundos**

   Código agregado:

   ```java
   Timer timer = new Timer();
   timer.scheduleAtFixedRate(new TimerTask() {
       @Override
       public void run() {
           pedirHora();  // Esta función se ejecuta cada 5 segundos
       }
   }, 0, 5000);
   ```

   **Por qué**:

   - El enunciado pide actualizar cada 5 segundos
   - `scheduleAtFixedRate` ejecuta la tarea repetidamente con el intervalo especificado
   - El `0` es el delay inicial (empieza ya), `5000` son los milisegundos entre ejecuciones

3. **En DatagramTimeClient: Timeout para no colgarse**

   Código agregado:

   ```java
   socket.setSoTimeout(1000);  // Espera máximo 1 segundo
   ```

   **Por qué**:

   - El método `receive()` es bloqueante, se queda esperando hasta recibir algo
   - Si el servidor está apagado, esperaría para siempre y el programa se colgaría
   - Con `setSoTimeout(1000)`, si en 1 segundo no llega respuesta, lanza una excepción `SocketTimeoutException`

4. **En DatagramTimeClient: Capturar timeout y mantener última hora**

   Código agregado:

   ```java
   private static String ultimaHora = "Esperando primera respuesta...";
   private static boolean servidorActivo = false;

   catch (SocketTimeoutException e) {
       if (servidorActivo) {
           System.out.println(new Date() + " - Servidor DESCONECTADO - Manteniendo: " + ultimaHora);
           servidorActivo = false;
       }
   }
   ```

   **Por qué**:

   - `ultimaHora` guarda el último valor recibido para mostrarlo cuando el servidor falla
   - `servidorActivo` permite saber si hubo un cambio de estado (conectado → desconectado) y mostrar el mensaje solo una vez
   - Si no hubiera esta variable, mostraría "DESCONECTADO" cada 5 segundos aunque ya lo supieramos

5. **En DatagramTimeClient: Mostrar actualizaciones**

   Código agregado:

   ```java
   if (!servidorActivo) {
       System.out.println(new Date() + " - Servidor CONECTADO - Hora: " + ultimaHora);
       servidorActivo = true;
   } else {
       System.out.println(new Date() + " - Hora actualizada: " + ultimaHora);
   }
   ```

   **Por qué**:

   - La primera vez que se conecta, muestra "CONECTADO" y la hora
   - Las siguientes veces solo muestra "actualizada" para no repetir el mismo mensaje
   - Así se ve claramente cuándo hay cambios de estado

## Comparativa Completa

| Aspecto                     | Código Original                     | Código Modificado                  | Por qué del cambio                                                                 |
|-----------------------------|-------------------------------------|------------------------------------|------------------------------------------------------------------------------------|
| Servidor - Duración         | Atiende 1 cliente y muere           | Atiende infinitos clientes         | El enunciado requiere apagar y prender el servidor mientras el cliente sigue      |
| Servidor - Socket           | Se cierra después de responder     | Nunca se cierra                   | Si se cerrara, tocaría reiniciar manualmente                                      |
| Cliente - Frecuencia        | Una sola vez                       | Cada 5 segundos                   | El enunciado lo pide explícitamente                                               |
| Cliente - Timeout           | No tiene, se cuelga si no hay respuesta | 1 segundo de timeout              | Evita que el programa se quede congelado                                          |
| Cliente - Estado            | No guarda nada                     | Guarda última hora                 | Para mostrar algo cuando el servidor falla                                        |
| Cliente - Mensajes          | Solo muestra la hora               | Muestra CONECTADO/DESCONECTADO/actualizada | Para ver claramente qué está pasando                                              |
