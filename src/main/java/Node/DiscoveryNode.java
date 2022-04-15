package Node;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.net.*;

public class DiscoveryNode extends Thread {
    boolean running = true;
    DatagramSocket socket;

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
            this.socket = new DatagramSocket(8000); // receivingPort
            this.socket.setBroadcast(true);
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
                socket.send(sendPacket);
                socket.send(sendPacket2);
                System.out.println("sent packet to: " + sendPacket.getSocketAddress());
                socket.receive(receivePacket); // receive a packet on this socket
                System.out.println("received packet from: " + receivePacket.getSocketAddress());
                String receivedData = new String(receivePacket.getData(),0,receivePacket.getLength()).trim();
                System.out.println("received data: " + receivedData);
                JSONParser parser = new JSONParser();
                Object obj = parser.parse(receivedData);
                String status = ((JSONObject) obj).get("status").toString();
                String sender = ((JSONObject) obj).get("sender").toString();
                if (sender.equals("NamingServer")) {
                    receivedServer = true;
                        this.namingServer_IP = String.valueOf(receivePacket.getAddress().getHostAddress());
                        this.ID = (int) (long) ((JSONObject) obj).get("node ID");
                        this.amount = (int) (long) ((JSONObject) obj).get("node amount");
                    if(status.equals("OK")) {
                        this.previousID = (int) (long) ((JSONObject) obj).get("previousID");
                        this.nextID = (int) (long) ((JSONObject) obj).get("nextID");
                    }
                }else if(sender.equals("NodeNext")) {
                    //this.receivingID = (int) (long) ((JSONObject)obj).get("currentID");
                    //this.receivingNextID = (int) (long) ((JSONObject)obj).get("nextID");
                    nodecounter++;

                }else if(sender.equals("NodePrevious")) {
                    //this.receivingID = (int) (long) ((JSONObject)obj).get("currentID");
                    //this.receivingPreviousID = (int) (long) ((JSONObject)obj).get("previousID");
                    nodecounter++;

                }
                if(nodecounter == amount-1){
                    receivedAllNodes = true;
                }


            }
             catch (IOException | ParseException | InterruptedException e) {
               // e.printStackTrace();
            }
        }
            done = true;
    }
    public String getAddress(){
        if(done) {
            return this.namingServer_IP;
        }
     return null;
    }

}
/*
            try { // If we receive
                socket.receive(receivePacket); // So now timeout for 2s
                received = true;
                System.out.println("received packet from: " + receivePacket.getSocketAddress());
                String data = new String(receivePacket.getData()).trim();
                System.out.println("received data: " + data);
                this.node_IP = InetAddress.getLocalHost().getHostAddress();
                this.namingServer_IP = String.valueOf(receivePacket.getAddress().getHostAddress());
                JSONParser parser = new JSONParser();
                Object obj = parser.parse(data);
                String status = ((JSONObject)obj).get("node").toString();
                if (status.equals("Added successfully")){
                    this.hash =   (int) (long) ((JSONObject)obj).get("node hash");
                    this.amount = (int) (long) ((JSONObject)obj).get("node amount");
                    this.nodes  =  ((JSONObject)obj).get("nodes").toString();
                }else if (status.equals("Error: Node was not added")){
                    System.out.println("Error: Node was not added");
                }
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
    }
}
*/

/*

            try { // If we receive
                socket.receive(receivePacket); // So now timeout for 2s
                received = true;
                System.out.println("received packet from: " + receivePacket.getSocketAddress());
                String data = new String(receivePacket.getData()).trim();
                System.out.println("received data: " + data);
                this.node_IP = InetAddress.getLocalHost().getHostAddress();
                this.namingServer_IP = String.valueOf(receivePacket.getAddress().getHostAddress());

                JSONParser parser = new JSONParser();
                Object obj = parser.parse(data);
                String status = ((JSONObject)obj).get("node").toString();

                if (status.equals("Added successfully")){
                    this.hash =   (int) (long) ((JSONObject)obj).get("node hash");
                    this.amount = (int) (long) ((JSONObject)obj).get("node amount");
                    this.nodes  =  ((JSONObject)obj).get("nodes").toString();
                }else if (status.equals("Error: Node was not added")){
                    System.out.println("Error: Node was not added");
                }

            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
    }
}
*/