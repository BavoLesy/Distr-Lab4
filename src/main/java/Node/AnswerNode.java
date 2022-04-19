package Node;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class AnswerNode extends Thread {
    boolean running = true;
    DatagramSocket answerSocket;
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
            this.answerSocket = new DatagramSocket(8001);
            this.answerSocket.setBroadcast(true);
            this.answerSocket.setSoTimeout(1000);
        } catch (SocketException e) {
            this.answerSocket = null;
            System.out.println("Something went wrong");
            e.printStackTrace();
        }
        //this.name = name;
        //this.node_IP = InetAddress.getLocalHost().getHostAddress();

        //this.namingServer_IP = "192.168.80.3";
    }
    public void start() {
        List<String> nodesList2 = new ArrayList<>();
        if (answerSocket == null) return;
        byte[] receiveBuffer = new byte[512];
        DatagramPacket receivePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);
        while(true) {
            try {
                Thread.sleep(900);
                //System.out.println("still alive");
                answerSocket.receive(receivePacket);
                String s1 = receivePacket.getAddress().toString();
                String s2 = "/" + InetAddress.getLocalHost().getHostAddress();
                String IP = receivePacket.getAddress().getHostAddress(); //IP of the Current Node
                if((!s1.equals(s2)) && (!nodesList2.contains(IP))) { // We only listen to other IP than our own and only IPs we havent listened to.
                    nodesList2.add(IP);
                    System.out.println("Discovery package received! -> " + receivePacket.getAddress() + ":" + receivePacket.getPort());
                    String receivedData = new String(receivePacket.getData(), 0, receivePacket.getLength()).trim();
                    int hash = ToHash.hash(receivedData);
                    String response;
                    int currentID = ToHash.hash(name);
                    //System.out.println("hash: " + hash);
                    //System.out.println("currentID: " + currentID);
                    //System.out.println("nextID: " + nextID);
                    //System.out.println("previousID: " + previousID);
                    if (currentID < hash && (hash < nextID || nextID == currentID)){
                        nextID = hash;
                        response = "{\"status\":\"nextID changed\"," + "\"sender\":\"Node\"," + "\"currentID\":" + currentID + "," +
                                "\"nextID\":" + nextID + "," + "\"previousID\":" + previousID + "}";
                    } else if (hash < currentID && (previousID < hash || previousID == currentID)) { //
                        previousID = hash;
                        response = "{\"status\":\"previousID changed\"," + "\"sender\":\"Node\"," + "\"currentID\":" + currentID + "," +
                                "\"nextID\":" + nextID + "," + "\"previousID\":" + previousID + "}";
                    }else {
                        response = "{\"status\":\"Nothing changed\"," + "\"sender\":\"Node\"," + "\"currentID\":" + currentID + "," +
                                "\"nextID\":" + nextID + "," + "\"previousID\":" + previousID + "}";
                    }
                    DatagramPacket responsePacket = new DatagramPacket(response.getBytes(StandardCharsets.UTF_8), response.length(), receivePacket.getAddress(), receivePacket.getPort());
                    this.answerSocket.send(responsePacket);
                }
            } catch (IOException | InterruptedException e) {
                //e.printStackTrace();
            }
        }
    }
}
