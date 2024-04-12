import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.SocketException;
import java.util.Scanner;

public class MulticastUDPChat {

    public static String nombre;
    static volatile boolean terminado;

    @SuppressWarnings("deprecation")
    public static void main(String[] args) {
        try{
            int puerto = 8080;
            InetAddress grupo = InetAddress.getByName("224.0.0.0");
            MulticastSocket socket = new MulticastSocket(puerto);
            socket.joinGroup(grupo);

            Thread lectura = new Thread(new HiloLectura(socket, grupo, puerto));
            lectura.start();
            Scanner scan = new Scanner(System.in);
            System.out.println("Ingresa tu nombre: ");
            nombre = scan.nextLine();

            System.out.println("Puede comenzar a escribir mensajes en el grupo...");
            byte[] bufer = new byte[1024];
            String linea;
            while (true) {
                linea = scan.nextLine();
                if (linea.equalsIgnoreCase("Adios")) {
                    terminado = true;
                    linea = nombre + ": Ha terminado la conexion.";
                    bufer = linea.getBytes();
                    DatagramPacket mensajeSalida = new DatagramPacket(bufer, bufer.length, grupo, puerto);
                    socket.send(mensajeSalida);

                    socket.leaveGroup(grupo);
                    socket.close();
                    break;
                }
                linea = nombre + ": " + linea;
                bufer = linea.getBytes();
                DatagramPacket datagram = new DatagramPacket(bufer, bufer.length, grupo, puerto);
                socket.send(datagram);
            }

            scan.close();
        } catch (SocketException e) {
            System.out.println("Socket: " + e.getMessage());
        } catch (IOException e) {
            System.out.println("IO: " + e.getMessage());
        } 
    }
}

/**
 * HiloLectura
 */
class HiloLectura implements Runnable {

    MulticastSocket socket;
    InetAddress grupo;
    int puerto;

    public HiloLectura(MulticastSocket socket, InetAddress grupo, int puerto) {
        this.socket = socket;
        this.grupo = grupo;
        this.puerto = puerto;
    }

    @Override
    public void run() {
        byte[] bufer = new byte[1024];
        String linea;

        while (!MulticastUDPChat.terminado) {
            try {
                DatagramPacket mensajeEntrada = new DatagramPacket(bufer, bufer.length, grupo, puerto);
                socket.receive (mensajeEntrada);

                linea = new String(bufer, 0, mensajeEntrada.getLength());
                if (!linea.startsWith(MulticastUDPChat.nombre)) {
                    System.out.println(linea);
                }
            } catch (IOException e) {
                System.out.println("Comunicacion y sockets cerrados");
            }    
                    
        }
    }
}