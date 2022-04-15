package Server;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;

public class Discovery extends Thread {
    boolean running = true;
    DatagramSocket socket;
    NamingServer ns;
    public Discovery(NamingServer nameserver){
        this.ns = nameserver;
        try{
            this.socket = new DatagramSocket(8001); // receivingPort
            this.socket.setBroadcast(true);
            this.socket.setSoTimeout(900);
        } catch (SocketException e) {
            this.socket = null;
            System.out.println("Something went wrong");
            e.printStackTrace();
        }
    }
    @Override
    public void run() {
        if (this.socket == null) return;
        byte[] receiveBuffer = new byte[512];
        DatagramPacket receivePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);
        while (this.running) {
            try {
                this.socket.receive(receivePacket);
                System.out.println("Discovery package received! -> " + receivePacket.getAddress() + ":" + receivePacket.getPort());
                String receivedData = new String(receivePacket.getData()).trim(); //this is the name of the Node!
                int hash = ns.hash(receivedData);
                String IP = receivePacket.getAddress().getHostAddress(); //IP of the Node
                String response;
                if (ns.addNode(receivedData, IP).equals("Added Node " + receivedData + " with hash: " + hash + "\n")){
                    //if adding is successful
                    NamingServer.ipMapLock.readLock().lock();
                    Integer previousID = NamingServer.getIpMapping().lowerKey(hash-1);
                    if (previousID == null) previousID = NamingServer.getIpMapping().lastKey();
                    Integer nextID = NamingServer.getIpMapping().higherKey(hash+1);
                    if (nextID == null) nextID = NamingServer.getIpMapping().firstKey();
                    response = "{\"status\":\"OK\"," + "\"sender\":\"NamingServer\"," + "\"node ID\":" + hash + "," +
                            "\"node amount\":" + NamingServer.getIpMapping().size() + ","
                            + "\"previousID\":" + previousID + "," + "\"nextID\":" + nextID + "}";
                    NamingServer.ipMapLock.readLock().unlock();
                }else{
                    //adding unsuccessful
                    ns.logger.info("Adding node failed");
                    response = "{\"status\":\"Node already exists\"," + "\"sender\":\"NamingServer\"," + "\"node ID\":" + hash + "," +
                            "\"node amount\":" + NamingServer.getIpMapping().size() + "}";
                }
                DatagramPacket responsePacket = new DatagramPacket(response.getBytes(StandardCharsets.UTF_8), response.length(), receivePacket.getAddress(), receivePacket.getPort());
                this.socket.send(responsePacket);
                //sending port = 8000
                } catch (IOException e) {
                //e.printStackTrace();
            }
        }
    }
    public void terminate(){
        this.running = false;
    }
}




