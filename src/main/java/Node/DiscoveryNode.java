package Node;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import javax.xml.crypto.Data;
import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;

public class DiscoveryNode extends Thread {
    DatagramSocket discoverySocket;
    DatagramSocket answerSocket;

    private final InetAddress broadcastAddress;
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
    public DiscoveryNode(String name) throws IOException {
        super(name);
        this.name = name;
        this.broadcastAddress = InetAddress.getByName("255.255.255.255"); //Broadcast
        try{
            this.discoverySocket = new DatagramSocket(8000, InetAddress.getLocalHost()); // receivingPort
            this.answerSocket = new DatagramSocket(8002);
            this.answerSocket.setBroadcast(true);
            this.answerSocket.setSoTimeout(1000);
            this.discoverySocket.setBroadcast(true);
            this.discoverySocket.setSoTimeout(1000);
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
        boolean receivedServer = false;
        boolean receivedAllNodes = false;
        int nodecounter = 0;
        byte[] receive = new byte[512];
        //send out our name on the broadcastaddress
        DatagramPacket sendPacket = new DatagramPacket(name.getBytes(), name.length(), broadcastAddress, 8001); //broadcast on port 8001
        DatagramPacket sendPacket2 = new DatagramPacket(name.getBytes(), name.length(), broadcastAddress, 8002); //broadcast on port 8002
        DatagramPacket receivePacket = new DatagramPacket(receive, receive.length);  // receivePacket
        while (!receivedAllNodes || !receivedServer) { // send a datagram packet until the NamingServer answers with a receive packet
            try {
                Thread.sleep(1000);
                discoverySocket.send(sendPacket);
                answerSocket.send(sendPacket2);
                System.out.println("sent packet to: " + sendPacket.getSocketAddress());
                discoverySocket.receive(receivePacket); // receive a packet on this socket
                System.out.println("received packet from: " + receivePacket.getSocketAddress());
                String receivedData = new String(receivePacket.getData(),0,receivePacket.getLength()).trim();
                System.out.println("received data: " + receivedData);
                JSONParser parser = new JSONParser();
                Object obj = parser.parse(receivedData);
                String status = ((JSONObject) obj).get("status").toString();
                String sender = ((JSONObject) obj).get("sender").toString();
                switch (sender) {
                    case "NamingServer":
                        receivedServer = true;
                        this.namingServer_IP = String.valueOf(receivePacket.getAddress().getHostAddress());
                        this.ID = (int) (long) ((JSONObject) obj).get("node ID");
                        this.amount = (int) (long) ((JSONObject) obj).get("node amount");
                        if (status.equals("OK")) {
                            this.previousID = (int) (long) ((JSONObject) obj).get("previousID");
                            this.nextID = (int) (long) ((JSONObject) obj).get("nextID");
                        }
                        break;
                    case "NodeNext":
                        //this.receivingID = (int) (long) ((JSONObject)obj).get("currentID");
                        //this.receivingNextID = (int) (long) ((JSONObject)obj).get("nextID");
                        nodecounter++;
                        break;
                    case "NodePrevious":
                        //this.receivingID = (int) (long) ((JSONObject)obj).get("currentID");
                        //this.receivingPreviousID = (int) (long) ((JSONObject)obj).get("previousID");
                        nodecounter++;
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
                Thread.sleep(1000);
                System.out.println("still alive");
                answerSocket.receive(receivePacket);
                System.out.println("Discovery package received! -> " + receivePacket.getAddress() + ":" + receivePacket.getPort());
                if(receivePacket.getAddress() != InetAddress.getLocalHost()) {
                    String receivedData = new String(receivePacket.getData(), 0, receivePacket.getLength()).trim();
                    String IP = receivePacket.getAddress().getHostAddress(); //IP of the Current Node
                    int hash = ToHash.hash(receivedData);
                    String response;
                    int currentID = ToHash.hash(name);
                    if (currentID < hash && hash < nextID) {
                        nextID = hash;
                        response = "{\"status\":\"OK\"," + "\"sender\":\"NodeNext\"," + "\"currentID\":" + currentID + "," +
                                "\"nextID\":" + nextID + "\"}";
                    } else if (previousID < hash && hash < currentID) {
                        previousID = hash;
                        response = "{\"status\":\"OK\"," + "\"sender\":\"NodePrevious\"," + "\"currentID\":" + currentID + "," +
                                "\"nextID\":" + previousID + "\"}";
                    } else {
                        response = "{\"status\":\"nothing changed\"," + "\"sender\":\"NodeNext\"}";
                    }
                    DatagramPacket responsePacket = new DatagramPacket(response.getBytes(StandardCharsets.UTF_8), response.length(), receivePacket.getAddress(), receivePacket.getPort());
                    this.answerSocket.send(responsePacket);
                }
            } catch (IOException | InterruptedException e) {
                //e.printStackTrace();
            }
        }

    }
    public String getAddress(){
        if(done) {
            return this.namingServer_IP;
        }
     return null;
    }

}

