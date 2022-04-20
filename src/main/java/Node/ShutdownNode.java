package Node;

import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;

public class ShutdownNode extends Thread{
    private String name;
    private int currentID;
    private int nextID;
    private int previousID;
    private String previousIP;
    private String nextIP;
    private DatagramSocket shutdownSocket;


    public ShutdownNode(NamingNode node) throws SocketException {
        this.name = node.discoveryNode.getNodeName();
        this.currentID = node.discoveryNode.getCurrentID();
        this.nextID = node.discoveryNode.getNextID();
        this.previousID = node.discoveryNode.getPreviousID();
        this.nextIP = node.discoveryNode.getNextIP();
        this.previousIP = node.discoveryNode.getPreviousIP();
        this.shutdownSocket = new DatagramSocket(8002);
        node.delete(name);
    }

    public void start(){
        try {
            System.out.println("Shutting down...");
            // Send the nextID to the previousNode and send the previousID to the nextNode using datagrampackets
            String previousResponse;
            String nextResponse;
            previousResponse = "{\"status\":\"Shutdown\"," + "\"sender\":\"nextNode\"," + "\"currentID\":" + currentID + "," +
                "\"nextID\":" + nextID + ","+ "\"nextIP\":" + nextIP + "}";
            DatagramPacket previousNode = new DatagramPacket(previousResponse.getBytes(), previousResponse.length(), InetAddress.getByName(previousIP), 8001);
            shutdownSocket.send(previousNode);
            nextResponse = "{\"status\":\"shutdown\"," + "\"sender\":\"previousNode\"," + "\"currentID\":" + currentID + "," + "\"previousID\":" + previousID + "," + "\"previousIP\":" + previousIP + "}";
            DatagramPacket nextNode = new DatagramPacket(nextResponse.getBytes(), nextResponse.length(), InetAddress.getByName(nextIP), 8001);
            shutdownSocket.send(nextNode);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
