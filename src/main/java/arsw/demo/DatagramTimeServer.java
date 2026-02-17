package arsw.demo;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.Date;

public class DatagramTimeServer {
    private DatagramSocket socket;

    public DatagramTimeServer() {
        try {
            socket = new DatagramSocket(4445);
            System.out.println("Servidor UDP iniciado en puerto 4445");
        } catch (SocketException ex) {
            System.err.println("Error al crear socket: " + ex.getMessage());
        }
    }

    public void startServer() {
        byte[] buf = new byte[256];

        while (true) {
            try {
                DatagramPacket packet = new DatagramPacket(buf, buf.length);
                socket.receive(packet);

                String hora = new Date().toString();
                buf = hora.getBytes();

                InetAddress address = packet.getAddress();
                int port = packet.getPort();

                packet = new DatagramPacket(buf, buf.length, address, port);
                socket.send(packet);

                System.out.println("Hora enviada a " + address + ":" + port);

            } catch (IOException ex) {
                System.err.println("Error: " + ex.getMessage());
            }
        }
    }

    public static void main(String[] args) {
        DatagramTimeServer ts = new DatagramTimeServer();
        ts.startServer();
    }
}