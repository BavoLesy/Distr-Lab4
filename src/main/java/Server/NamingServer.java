package Server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@RestController
public class NamingServer extends Thread{
    Logger logger = LoggerFactory.getLogger(NamingServer.class);
    private static final TreeMap<Integer, String> ipMapping = new TreeMap<>();
    static ReadWriteLock ipMapLock = new ReentrantReadWriteLock(); //lock to avoid reading when someone else is writing and vice versa
    //DiscoveryHandler discoveryHandler;
    Discovery discovery;
    public NamingServer() {
        //this.discoveryHandler = new DiscoveryHandler(this);
        //discoveryHandler.start();
        this.discovery = new Discovery(this);
        this.discovery.start();
    }

    public static TreeMap<Integer, String> getIpMapping() {
        return ipMapping;
    }

    public static ReadWriteLock getIpMapLock() {
        return ipMapLock;
    }

    //hash often makes the same hashes from different names, solution?
    public int hash(String string) {
        this.logger.info("Calculating hash of: " + string);
        long max = 2147483647;
        long min = -2147483648;
        return (int) (((long) string.hashCode() + max) * (32768.0 / (max + Math.abs(min))));
    }
    @PostMapping("/NamingServer/Nodes/{node}")
    public String addNode(@PathVariable(value = "node") String name, @RequestBody String IP){
        int hash = hash(name);
        this.logger.info("Adding node " + name + " with hash: " + hash);
        ipMapLock.writeLock().lock();
        if (ipMapping.containsKey(hash)){
            ipMapLock.writeLock().unlock();
            return "Node " + name + " with hash: " + hash + " already exists or has the same hash as another node\n";
        }
        ipMapping.put(hash, IP);
        JSON_Handler.writeFile();
        ipMapLock.writeLock().unlock();
        return "Added Node " + name + " with hash: " + hash +"\n";
    }
    @DeleteMapping("/NamingServer/Nodes/{node}")
    public String removeNode(@PathVariable(value = "node") String name){
        int hash = hash(name);
        this.logger.info("Removing node: " + name + " with hash: " + hash);
        ipMapLock.writeLock().lock();
        if (!ipMapping.containsKey(hash)) {
            ipMapLock.writeLock().unlock();
            return "Node " + name + " with hash: " + hash + " does not exist\n";
        }
        ipMapping.remove(hash);
        JSON_Handler.writeFile();
        ipMapLock.writeLock().unlock();
        return "Node " + name + " with hash: " + hash + " was removed\n";
    }
    @GetMapping("/NamingServer/Files/{filename}")
    public String getFile(@PathVariable(value = "filename") String fileName) {
        this.logger.info("Where is file?: " + fileName);
        int hash = hash(fileName);
        Map.Entry<Integer,String> entry; //get an entry from the map
        ipMapLock.readLock().lock();
        if(ipMapping.floorEntry(hash-1) == null){
            entry = ipMapping.lastEntry();
        }else{
            entry = ipMapping.floorEntry(hash-1); //returns closest value lower than or equal to key (so where the file is located)
        }
        ipMapLock.readLock().unlock();
        return "The file " + fileName + " is located at: " + entry.getValue() +"\n";
    }
    @GetMapping("/NamingServer/Nodes/{node}")
    public String getNodes(@PathVariable(value = "node") String name){
        int hash = hash(name);
        String send;
        Set<Map.Entry<Integer,String>> entries = getIpMapping().entrySet();
        if(getIpMapping().containsKey(hash)) {
            int i = 1;
            StringBuilder nodes = new StringBuilder();
            for (Map.Entry<Integer, String> entry : entries) {
                nodes.append("Node #");
                nodes.append(i);
                nodes.append(": ");
                nodes.append(entry.getKey().toString());
                nodes.append(" with IP ");
                nodes.append(entry.getValue());
                nodes.append(",");
                i++;
            }
            ipMapLock.readLock().lock();
            send = "{\"node status\":\"Node exists\"," + "\"node hash\":" + hash + "," +
                    "\"node amount\":" + getIpMapping().size() +
                    "\"nodes\":\"" + nodes.toString() + "\"}";
            ipMapLock.readLock().unlock();
        }
        else{
            send = "{\"node status\":\"Node does not exist\"}";
        }
        return send;
    }
    //Automatic discovery of new nodes
    private class DiscoveryHandler extends Thread{
        NamingServer nameServer;
        boolean running = false;
        DatagramSocket socket;

        private DiscoveryHandler(){}
        public DiscoveryHandler(NamingServer nameServer) {
            this.nameServer = nameServer;
            try {
                this.socket = new DatagramSocket(8001);
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

                    int hash = this.nameServer.hash(data);
                    String ip = receivePacket.getAddress().getHostAddress();
                    String response;
                    if (this.nameServer.addNode(data,ip).equals("Added Node " + data + " with hash: " + hash + "\n")){
                        //adding successful
                        ipMapLock.readLock().lock();
                        Integer lowerId = getIpMapping().lowerKey(hash-1);
                        if (lowerId == null) lowerId = getIpMapping().lastKey();
                        Integer higherId = getIpMapping().higherKey(hash+1);
                        if (higherId == null) higherId = getIpMapping().firstKey();

                        response = "{\"status\":\"OK\"," +
                                "\"id\":" + hash + "," +
                                "\"nodeCount\":" + getIpMapping().size() + "," +
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


    public void run(){
        System.out.println("Starting NameServer...");
        NamingServer nameServer = new NamingServer();
    }
}
