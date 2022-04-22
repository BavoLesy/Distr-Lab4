package Node;

import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;

public class ShutdownNode extends Thread{
    private static boolean running;
    private String name;
    private int currentID;
    private int nextID;
    private int previousID;
    private String previousIP;
    private String nextIP;
    private DatagramSocket shutdownSocket;
    private int counter;


    public ShutdownNode(NamingNode node) throws SocketException, InterruptedException {
        running = true;
        this.counter = 0;
        this.name = node.name;
        this.currentID = node.discoveryNode.getCurrentID();
        this.nextID = node.discoveryNode.getNextID();
        this.previousID = node.discoveryNode.getPreviousID();
        this.nextIP = node.discoveryNode.getNextIP();
        this.previousIP = node.discoveryNode.getPreviousIP();
        this.shutdownSocket = new DatagramSocket(8002);
        this.shutdownSocket.setSoTimeout(1000);
        node.delete(name);
    }

    public void run(){
        try {
            System.out.println("Shutting down...");
            this.counter++;
            // Send the nextID to the previousNode and send the previousID to the nextNode using datagrampackets
            String previousResponse;
            String nextResponse;
            previousResponse = "{\"status\":\"Shutdown\"," + "\"sender\":\"nextNode\"," + "\"currentID\":" + currentID + "," +
                "\"nextID\":" + nextID + ","+ "\"nextIP\":" + "\"" + nextIP + "\"" + "}";
            DatagramPacket previousNode = new DatagramPacket(previousResponse.getBytes(), previousResponse.length(), InetAddress.getByName(previousIP), 8001);
            shutdownSocket.send(previousNode);
            nextResponse = "{\"status\":\"Shutdown\"," + "\"sender\":\"previousNode\"," + "\"currentID\":" + currentID + "," + "\"previousID\":" + previousID + "," + "\"previousIP\":" + "\"" + previousIP + "\"" + "}";
            DatagramPacket nextNode = new DatagramPacket(nextResponse.getBytes(), nextResponse.length(), InetAddress.getByName(nextIP), 8001);
            shutdownSocket.send(nextNode);
            setRunning(false);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public static boolean getRunning(){
        return running;
    }
    public void setRunning(boolean a){
       running = a;

    }
}
