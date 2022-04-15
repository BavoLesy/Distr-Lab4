package Node;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;

public class AnswerNode extends Thread {
    boolean running = true;
    DatagramSocket socket;
    private int amount;
    private String node_IP;
    private String namingServer_IP;
    private int ID;
    private int previousID;
    private int nextID;
    private String nodes;
    private int receivingID;
    private String name;

    public boolean done;
    public AnswerNode(String name) {
        super(name);
        this.name = name;
        try{
            this.socket = new DatagramSocket(8001); // receivingPort
            this.socket.setSoTimeout(1000);
        } catch (SocketException e) {
            this.socket = null;
            System.out.println("Something went wrong");
            e.printStackTrace();
        }
        //this.name = name;
        //this.node_IP = InetAddress.getLocalHost().getHostAddress();

        //this.namingServer_IP = "192.168.80.3";
    }

    public void start() {
        boolean running = true;
        if (socket == null) return;
        byte[] receiveBuffer = new byte[512];
        DatagramPacket receivePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);
        while (running) {
            try {
                socket.receive(receivePacket);
                System.out.println("Discovery package received! -> " + receivePacket.getAddress() + ":" + receivePacket.getPort());
                String receivedData = new String(receivePacket.getData()).trim(); //this is the name of the Node!
                int hash = ToHash.hash(receivedData);
                String IP = receivePacket.getAddress().getHostAddress(); //IP of the Node
                int currentID = ToHash.hash(name);
                String response = "{\"status\":\"nothing changed\"}";
                if(currentID<hash && hash<nextID){
                    nextID = hash;
                    response = "{\"status\":\"OK\"," + "\"sender\":\"NodeNext\"," + "\"currentID\":" + currentID + "," +
                            "\"nextID\":" + nextID+ "\"}";
                } else if (previousID < hash && hash < currentID){
                    previousID = hash;
                    response = "{\"status\":\"OK\"," + "\"sender\":\"NodePrevious\"," + "\"currentID\":" + currentID + "," +
                            "\"nextID\":" + previousID+ "\"}";
                }
                DatagramPacket responsePacket = new DatagramPacket(response.getBytes(StandardCharsets.UTF_8), response.length(), receivePacket.getAddress(),receivePacket.getPort());
                socket.send(responsePacket);
                break;
                //sending port = 8000
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
