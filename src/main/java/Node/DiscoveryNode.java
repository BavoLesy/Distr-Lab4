package Node;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import javax.xml.crypto.Data;
import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DiscoveryNode extends Thread {
    DatagramSocket discoverySocket;
    DatagramSocket answerSocket;
    private final InetAddress broadcastAddress;
    private int amount;
    private String currentIP;
    private String serverIP;
    private int currentID;
    private int previousID;
    private int nextID;

    public int getAmount() {
        return amount;
    }

    public String getCurrentIP() {
        return this.currentIP;
    }

    public String getPreviousIP() {
        return this.previousIP;
    }

    public String getNextIP() {
        return nextIP;
    }

    private String previousIP;
    private String nextIP;
    private String name;


    public boolean done;
    public DiscoveryNode(String name) throws IOException {
        super(name);
        this.name = name;
        this.broadcastAddress = InetAddress.getByName("255.255.255.255"); //Broadcast
        try{
            this.discoverySocket = new DatagramSocket(8000, InetAddress.getLocalHost()); // receivingPort
            this.answerSocket = new DatagramSocket(8001); //socket for answering the broadcast
            this.answerSocket.setBroadcast(true);
            this.answerSocket.setSoTimeout(1000);
            this.discoverySocket.setBroadcast(true);
            this.discoverySocket.setSoTimeout(1000);
            this.currentIP = InetAddress.getLocalHost().getHostAddress();
        } catch (SocketException e) {
            this.discoverySocket = null;
            System.out.println("Something went wrong");
            e.printStackTrace();
        }
        //this.name = name;
        //this.node_IP = InetAddress.getLocalHost().getHostAddress();
        //this.namingServer_IP = "192.168.80.3";
    }
    //Network discovery (multicast)
    public void start() {
        List<String> nodesList = new ArrayList<>();
        List<String> nodesList2 = new ArrayList<>();
        boolean receivedServer = false;
        boolean receivedAllNodes = false;
        int nodecounter = 0;
        byte[] receive = new byte[512];
        //send out our name on the broadcastaddress
        String send = "{\"status\":\"Discovery\"," + "\"name\":" +"\"" + name + "\"" + "}";
        DatagramPacket sendPacket = new DatagramPacket(send.getBytes(StandardCharsets.UTF_8), send.length(), broadcastAddress, 8001); //broadcast on port 8001
        //DatagramPacket sendPacket2 = new DatagramPacket(name.getBytes(), name.length(), broadcastAddress, 8002); //broadcast on port 8002
        DatagramPacket receivePacket = new DatagramPacket(receive, receive.length);  // receivePacket
        while (!receivedAllNodes || !receivedServer) { // send a datagram packet until the NamingServer answers with a receive packet
            try {
                Thread.sleep(1000);
                discoverySocket.send(sendPacket);
                System.out.println("sent packet to: " + sendPacket.getSocketAddress());
                discoverySocket.receive(receivePacket); // receive a packet on this socket
                String receivedData = new String(receivePacket.getData(),0,receivePacket.getLength()).trim();
                System.out.println("received packet from: " + receivePacket.getSocketAddress());
                System.out.println("received data: " + receivedData);
                JSONParser parser = new JSONParser();
                Object obj = parser.parse(receivedData);
                String status = ((JSONObject) obj).get("status").toString();
                String sender = ((JSONObject) obj).get("sender").toString();
                switch (sender) {
                    case "NamingServer":
                        receivedServer = true;
                        this.serverIP = String.valueOf(receivePacket.getAddress().getHostAddress());
                        this.currentID = (int) (long) ((JSONObject) obj).get("node ID");
                        this.amount = (int) (long) ((JSONObject) obj).get("node amount");
                        if (status.equals("OK")) {
                            this.previousID = (int) (long) ((JSONObject) obj).get("previousID");
                            this.nextID = (int) (long) ((JSONObject) obj).get("nextID");
                            this.previousIP = (String) ((JSONObject) obj).get("previousIP");
                            this.nextIP = (String) ((JSONObject) obj).get("nextIP");

                         }
                        break;
                    //make sure we get answer from ALL nodes so use diff IPS
                    case "Node":
                        //this.receivingPreviousID = (int) (long) ((JSONObject)obj).get("previousID");
                        //this.receivingID = (int) (long) ((JSONObject)obj).get("currentID");
                        //this.receivingNextID = (int) (long) ((JSONObject)obj).get("nextID");
                        if(!nodesList.contains(receivePacket.getAddress().getHostAddress())) {
                            nodecounter++;
                            nodesList.add(receivePacket.getAddress().getHostAddress());
                        }
                        break;
                }
                if(nodecounter == amount-1){
                    receivedAllNodes = true;
                }

            }
            catch (IOException | ParseException | InterruptedException e) {
                // e.printStackTrace();
            }
        }
        while(true) {
            try {
                Thread.sleep(900);
                //System.out.println("still alive");
                answerSocket.receive(receivePacket);
                String s1 = receivePacket.getAddress().toString();
                String s2 = "/" + InetAddress.getLocalHost().getHostAddress();
                String IP = receivePacket.getAddress().getHostAddress(); //IP of the Current Node
                System.out.println("package received! -> " + receivePacket.getAddress() + ":" + receivePacket.getPort());
                String receivedData = new String(receivePacket.getData(), 0, receivePacket.getLength()).trim();
                JSONParser parser = new JSONParser();
                Object obj = parser.parse(receivedData);
                String status = ((JSONObject) obj).get("status").toString();
                if(status.equals("Discovery")) {
                    if ((!s1.equals(s2)) && (!nodesList2.contains(IP))) { // We only listen to other IP than our own and only IPs we havent listened to.
                        nodesList2.add(IP);
                        String response;
                        String name = ((JSONObject) obj).get("name").toString();
                        int hash = ToHash.hash(name);
                        System.out.println("hash: " + hash);
                        System.out.println("currentID: " + this.currentID);
                        System.out.println("nextID: " + this.nextID);
                        System.out.println("previousID: " + this.previousID);
                        if (this.currentID < hash && (hash < this.nextID || this.nextID == this.currentID)) {
                            this.nextID = hash;
                            response = "{\"status\":\"nextID changed\"," + "\"sender\":\"Node\"," + "\"currentID\":" + this.currentID + "," +
                                    "\"nextID\":" + this.nextID + "," + "\"previousID\":" + this.previousID + "}";
                        } else if (hash < this.currentID && (this.previousID < hash || this.previousID == this.currentID)) { //
                            this.previousID = hash;
                            response = "{\"status\":\"previousID changed\"," + "\"sender\":\"Node\"," + "\"currentID\":" + this.currentID + "," +
                                    "\"nextID\":" + this.nextID + "," + "\"previousID\":" + this.previousID + "}";
                        } else {
                            response = "{\"status\":\"Nothing changed\"," + "\"sender\":\"Node\"," + "\"currentID\":" + this.currentID + "," +
                                    "\"nextID\":" + this.nextID + "," + "\"previousID\":" + this.previousID + "}";
                        }
                        DatagramPacket responsePacket = new DatagramPacket(response.getBytes(StandardCharsets.UTF_8), response.length(), receivePacket.getAddress(), receivePacket.getPort());
                        this.answerSocket.send(responsePacket);
                    }
                }
                //update our previous and next
                if(status.equals("Shutdown")){
                    String sender = ((JSONObject) obj).get("sender").toString();
                    System.out.println("received data: " + receivedData);
                    if(sender.equals("nextNode")){
                        this.nextID = (int) (long) ((JSONObject) obj).get("nextID");
                        this.nextIP = (String) ((JSONObject) obj).get("nextIP");
                    }else if(sender.equals("previousNode")){
                        this.previousID = (int) (long) ((JSONObject) obj).get("previousID");
                        this.previousIP = (String) ((JSONObject) obj).get("previousIP");
                    }
                }
            } catch (IOException | InterruptedException | ParseException e) {
                //e.printStackTrace();
            }

        }
    }
    public String getServerIP(){
        return this.serverIP;
    }
    public int getCurrentID(){
        return this.currentID;
    }
    public int getPreviousID(){
        return this.previousID;
    }
    public int getNextID(){
        return this.nextID;
    }
    public String getNodeName(){
        return this.name;
    }

}