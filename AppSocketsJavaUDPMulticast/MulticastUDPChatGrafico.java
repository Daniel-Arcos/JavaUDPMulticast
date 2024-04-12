
import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.*;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.SocketException;
public class MulticastUDPChatGrafico {
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(VentanaIngreso::new);
    }
}

class VentanaIngreso extends JFrame {
    private JTextField textFieldNombre;
    private JButton btnAceptar, btnSalir;

    public VentanaIngreso() {
        inicializarUI();
    }

    private void inicializarUI() {
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception e) {
            System.out.println("Error");
        }

        //Ajuste ventana
        setTitle("Ingreso de Nombre");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(300, 150);
        setLocationRelativeTo(null);
        setLayout(new FlowLayout());

        textFieldNombre = new JTextField(20);
        btnAceptar = new JButton("Aceptar");
        btnSalir = new JButton("Salir");

        //Listeners en los botones
        btnAceptar.addActionListener(e -> abrirVentanaMensajes());
        btnSalir.addActionListener(e -> System.exit(0));

        add(textFieldNombre);
        add(btnAceptar);
        add(btnSalir);

        setVisible(true);
    }

    private void abrirVentanaMensajes() {
        String nombre = textFieldNombre.getText().trim();
        if (!nombre.isEmpty()) {
            VentanaMensajes ventanaMensajes = new VentanaMensajes(nombre);
            ventanaMensajes.setVisible(true);
            this.dispose();
        } else {
            JOptionPane.showMessageDialog(this, "Por favor, ingrese un nombre.", "Advertencia", JOptionPane.WARNING_MESSAGE);
        }
    }
}

class VentanaMensajes extends JFrame implements Runnable {
    private JLabel labelNombre;
    private JTextArea areaMensajes;
    private JTextField textFieldMensaje;
    private JButton btnEnviar;
    private JButton btnSalirChar;
    MulticastSocket socket;
    InetAddress grupo;
    int puerto = 8080;
    String nombre;
    boolean sesionActiva = true;

    @SuppressWarnings("deprecation")
    public VentanaMensajes(String nombre) {
        try {
            grupo = InetAddress.getByName("224.0.0.0");
            socket = new MulticastSocket(puerto);
            socket.joinGroup(grupo);
            this.nombre = nombre;
            inicializarUI(nombre);
            new Thread(this).start();
        } catch (SocketException e) {
            System.out.println("Socket: " + e.getMessage());
        } catch (IOException e) {
            System.out.println("IO: " + e.getMessage());
        } 
        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                salirChat();
            }
        });
    }

    private void inicializarUI(String nombre) {
        setTitle("Chat");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(500, 400);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        labelNombre = new JLabel("Nombre usuario: " + nombre);
        labelNombre.setFont(new Font("Arial", Font.PLAIN, 15));
        areaMensajes = new JTextArea();
        areaMensajes.setEditable(false);
        areaMensajes.setFont(new Font("Arial", Font.PLAIN, 15));
        JScrollPane scrollPane = new JScrollPane(areaMensajes);
        textFieldMensaje = new JTextField(20);
        btnEnviar = new JButton("Enviar");
        btnSalirChar = new JButton("Salir del chat");

        ActionListener enviarMensaje = e -> enviarMensaje(nombre + ": " + textFieldMensaje.getText());
        ActionListener salir = e -> salirChat();
        btnEnviar.addActionListener(enviarMensaje);
        textFieldMensaje.addActionListener(enviarMensaje);
        btnSalirChar.addActionListener(salir);

        JPanel panelSuperior = new JPanel(new FlowLayout());
        panelSuperior.add(labelNombre, LEFT_ALIGNMENT);
        panelSuperior.add(btnSalirChar, RIGHT_ALIGNMENT);
        add(panelSuperior, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
        JPanel panelInferior = new JPanel(new FlowLayout());
        panelInferior.add(textFieldMensaje);
        panelInferior.add(btnEnviar);
        add(panelInferior, BorderLayout.SOUTH);

        enviarMensaje(nombre + " se ha unido al chat");
    }

    private void enviarMensaje(String mensaje) {
        if (!mensaje.isEmpty()) {
            try {
                byte[] bufer = mensaje.getBytes();
                DatagramPacket datagram = new DatagramPacket(bufer, bufer.length, grupo, puerto);
                socket.send(datagram);
                textFieldMensaje.setText("");
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    @SuppressWarnings("deprecation")
    private void salirChat() {
        try {
            sesionActiva = false;
            enviarMensaje(nombre + " ha abandonado el chat.");
            socket.leaveGroup(grupo);
            socket.close();
            dispose();
            
        } catch (IOException ex) {
            ex.printStackTrace();
        }     
    }

    @Override
    public void run() {
        byte[] bufer = new byte[1024];
        while (sesionActiva) {
            try {
                DatagramPacket mensajeEntrada = new DatagramPacket(bufer, bufer.length, grupo, puerto);
                socket.receive (mensajeEntrada);

                String linea = new String(bufer, 0, mensajeEntrada.getLength());
                SwingUtilities.invokeLater(() -> areaMensajes.append(linea + "\n"));
            }
            catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }
}

