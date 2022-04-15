package Server;

import java.net.DatagramSocket;
import java.net.SocketException;

public class Discovery extends NamingServer {
    boolean running = false;
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

    }
}
private class DiscoveryHandler extends Thread{
    NameServer nameServer;
    boolean running = false;
    DatagramSocket socket;

    private DiscoveryHandler(){}
    public DiscoveryHandler(NameServer nameServer) {
        this.nameServer = nameServer;
        try {
            this.socket = new DatagramSocket(DATAGRAM_PORT);
            this.socket.setBroadcast(true);
            this.socket.setSoTimeout(888);
        } catch (SocketException e) {
            this.socket = null;
            System.out.println("Automatic node discovery disabled");
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        if (this.socket == null) return;

        this.running = true;
        byte[] receiveBuffer = new byte[512];
        DatagramPacket receivePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);
        while (this.running) {
            try {
                this.socket.receive(receivePacket);
                System.out.println("Discovery package received! -> " + receivePacket.getAddress() + ":" + receivePacket.getPort());
                String data = new String(receivePacket.getData()).trim();

                int Id = this.nameServer.hash(data);
                String ip = receivePacket.getAddress().getHostAddress();
                String response;
                if (this.nameServer.addNode(Id,ip)){
                    //adding successful
                    this.nameServer.ipMapLock.readLock().lock();
                    Integer lowerId = this.nameServer.getIdMap().lowerKey(Id-1);
                    if (lowerId == null) lowerId = this.nameServer.getIdMap().lastKey();
                    Integer higherId = this.nameServer.getIdMap().higherKey(Id+1);
                    if (higherId == null) higherId = this.nameServer.getIdMap().firstKey();

                    response = "{\"status\":\"OK\"," +
                            "\"id\":" + Id + "," +
                            "\"nodeCount\":" + this.nameServer.getIdMap().size() + "," +
                            "\"prevNodeId\":" + lowerId+ "," +
                            "\"nextNodeId\":" + higherId + "}";
                    this.nameServer.ipMapLock.readLock().unlock();
                }else{
                    //adding unsuccessful
                    this.nameServer.logger.info("Adding node failed");
                    response = "{\"status\":\"Access Denied\"}";
                }
                DatagramPacket responsePacket = new DatagramPacket(response.getBytes(StandardCharsets.UTF_8), response.length(), receivePacket.getAddress(), receivePacket.getPort());
                this.socket.send(responsePacket);

            } catch (IOException ignore) {}
        }
    }

    public void terminate(){
        this.running = false;
    }
}
