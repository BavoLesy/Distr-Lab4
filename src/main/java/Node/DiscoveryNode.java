package Node;

import Server.NamingServer;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;

public class DiscoveryNode extends NamingNode {
    private final String name;
    private String node_IP;
    private String namingServer_IP;
    private int hash;
    private int amount;
    private String nodes;

    public DiscoveryNode(String name) throws UnknownHostException {
        super(name);
        this.name = name;
        this.node_IP = InetAddress.getLocalHost().getHostAddress();
        this.namingServer_IP = "192.168.80.3";
    }

    //Network discovery (multicast)
    public void Discovery() throws IOException {
        boolean received = false;
        InetAddress broadcastAddress = InetAddress.getByName("255.255.255.255"); //Broadcast
        DatagramSocket socket = new DatagramSocket(8000); // receiving port
        socket.setSoTimeout(1000); // wait for 1 s when we try to receive()
        byte[] receive = new byte[512];
        //send out our name on the broadcastaddress
        DatagramPacket sendPacket = new DatagramPacket(name.getBytes(), name.length(), broadcastAddress, 8001); //broadcast on port 8001
        DatagramPacket receivePacket = new DatagramPacket(receive, receive.length);  // receivePacket
        while (!received) { // send a datagram packet until the NamingServer answers with a receive packet
            socket.send(sendPacket);
            System.out.println("sent packet to: " + sendPacket.getSocketAddress());
            try {
                socket.receive(receivePacket); // receive a packet on this socket
                System.out.println("received packet from: " + receivePacket.getSocketAddress());
                String receivedData = new String(receivePacket.getData()).trim();
                System.out.println("received data: " + receivedData);
                JSONParser parser = new JSONParser();
                Object obj = parser.parse(receivedData);
                String sender = ((JSONObject) obj).get("sender").toString();
                if (sender.equals("namingServer")) {
                    this.namingServer_IP = String.valueOf(receivePacket.getAddress().getHostAddress());

                }
                else if(sender.equals("namingNode")){
                    //
                }

            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
    }

    public void Answer() throws SocketException {
        DatagramSocket socket = new DatagramSocket(8000); // receiving port
        boolean running = true;
        if (socket == null) return;
        byte[] receiveBuffer = new byte[512];
        DatagramPacket receivePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);
        while (running) {
            try {
                socket.receive(receivePacket);
                System.out.println("Discovery package received! -> " + receivePacket.getAddress() + ":" + receivePacket.getPort());
                String receivedData = new String(receivePacket.getData()).trim(); //this is the name of the Node!
                int hash = NamingServer.hash(receivedData);
                String IP = receivePacket.getAddress().getHostAddress(); //IP of the Node
                int currentID = NamingServer.hash(name);
                int nextID = NamingServer.hash(nextID);
                int previousID = NamingServer.hash(previousID);
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
                
                DatagramPacket responsePacket = new DatagramPacket(response.getBytes(StandardCharsets.UTF_8), response.length(), receivePacket.getAddress(), receivePacket.getPort());
                socket.send(responsePacket);
                break;
                //sending port = 8000
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
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