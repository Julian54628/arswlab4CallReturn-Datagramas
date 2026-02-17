package arsw.demo;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

public class DatagramTimeClient {
    private static String ultimaHora = "Esperando primera respuesta...";
    private static boolean servidorActivo = false;

    public static void main(String[] args) {
        System.out.println("Cliente UDP iniciado, se actualizara cada 5 segundos");

        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                pedirHora();
            }
        }, 0, 5000);
    }

    private static void pedirHora() {
        DatagramSocket socket = null;
        try {
            socket = new DatagramSocket();
            socket.setSoTimeout(1000);

            byte[] buf = new byte[256];
            InetAddress address = InetAddress.getByName("127.0.0.1");

            DatagramPacket packet = new DatagramPacket(buf, buf.length, address, 4445);
            socket.send(packet);

            packet = new DatagramPacket(buf, buf.length);
            socket.receive(packet);

            String horaRecibida = new String(packet.getData(), 0, packet.getLength());
            ultimaHora = horaRecibida;

            if (!servidorActivo) {
                System.out.println(new Date() + " - Servidor conectado, hora " + ultimaHora);
                servidorActivo = true;
            } else {
                System.out.println(new Date() + " - Hora actualizada  " + ultimaHora);
            }

        } catch (SocketTimeoutException e) {
            if (servidorActivo) {
                System.out.println(new Date() + " - Servidor desconectado, manteniendo " + ultimaHora);
                servidorActivo = false;
            }
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
        } finally {
            if (socket != null) socket.close();
        }
    }
}