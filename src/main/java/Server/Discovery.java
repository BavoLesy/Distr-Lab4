package Server;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;

public class Discovery extends NamingServer {
    boolean running = true;
    DatagramSocket socket;

    public Discovery(){
        try{
            this.socket = new DatagramSocket(8001); // receivingPort
            this.socket.setBroadcast(true);
            this.socket.setSoTimeout(888);
        } catch (SocketException e) {
            this.socket = null;
            System.out.println("Something went wrong");
            e.printStackTrace();
        }
    }
    public void start() {
        if (this.socket == null) return;
        byte[] receiveBuffer = new byte[512];
        DatagramPacket receivePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);
        while (this.running) {
            try {
                this.socket.receive(receivePacket);
                System.out.println("Discovery package received! -> " + receivePacket.getAddress() + ":" + receivePacket.getPort());
                String receivedData = new String(receivePacket.getData()).trim(); //this is the name of the Node!
                int hash = hash(receivedData);
                String IP = receivePacket.getAddress().getHostAddress(); //IP of the Node
                String response;
                if (addNode(receivedData, IP).equals("Added Node " + receivedData + " with hash: " + hash + "\n")){
                    //if adding is successful
                    ipMapLock.readLock().lock();
                    Integer lowerId = getIpMapping().lowerKey(hash-1);
                    if (lowerId == null) lowerId = getIpMapping().lastKey();
                    Integer higherId = getIpMapping().higherKey(hash+1);
                    if (higherId == null) higherId = getIpMapping().firstKey();
                    response = "{\"status\":\"Node added\"," + "\"sender\":\"NamingServer\"," + "\"node ID\":" + hash + "," +
                            "\"node amount\":" + getIpMapping().size()
                            + "\"previousID\":" + lowerId + "," + "\"nextID\":" + higherId + "\"}";
                    ipMapLock.readLock().unlock();
                }else{
                    //adding unsuccessful
                    logger.info("Adding node failed");
                    response = "{\"status\":\"Node was not added\"}";
                }
                DatagramPacket responsePacket = new DatagramPacket(response.getBytes(StandardCharsets.UTF_8), response.length(), receivePacket.getAddress(), receivePacket.getPort());
                this.socket.send(responsePacket);
                break;
                //sending port = 8000
                } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    public void end(){
        this.running = false;
    }
}




