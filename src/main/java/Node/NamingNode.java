package Node;

import Server.Discovery;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.net.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class NamingNode {
    private static int nextID;
    private final String node_IP;
    public String name;
    private final String namingServer_IP;
    private int hash;
    private static int previousID;

    private int amount;
    private String nodes;
    DiscoveryNode discoveryNode;


        //this.namingServer_IP = "192.168.80.3"; //if we do network discovery with broadcasting?
    public NamingNode(String name) throws IOException { //constructor
        this.node_IP = InetAddress.getLocalHost().getHostAddress();
        this.name = name;
        //start discovery
        this.discoveryNode = new DiscoveryNode(name);
        this.discoveryNode.start();

        //start answer
        //this.answerNode = new AnswerNode(name);
        //this.answerNode.start();
        this.namingServer_IP = "192.168.80.3"; //DiscoveryNode.getAddress();
        //this.namingServer_IP = "localhost";
        /*
        this.currentID = discoveryNode.getCurrentID();
        this.nextID = discoveryNode.getNextID();
        this.previousID = discoveryNode.getPreviousID();
        this.nextIP = discoveryNode.getNextIP();
        this.previousIP = discoveryNode.getPreviousIP();
        this.shutdownSocket = new DatagramSocket(8002);

         */



    }
    public void getFile(String filename) {
        try {
            //String URL = "http://localhost:8080/NamingServer/getFile/" + filename; //REST command
            String URL = "http://" + discoveryNode.getServerIP() + ":8080/NamingServer/Files/" + filename;
            System.out.println(Unirest.get(URL).asString().getBody());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public void getNode(String user) throws UnirestException, ParseException {
        //String URL = "http://localhost:8080/NamingServer/Nodes/" + user;
        String URL = "http://" + discoveryNode.getServerIP() + ":8080/NamingServer/Nodes/" + user;
        String data = Unirest.get(URL).asString().getBody();
        JSONParser parser = new JSONParser();
        Object obj = parser.parse(data);
        String status = ((JSONObject)obj).get("node status").toString();
        if(status.equals("Node exists")) {
            this.hash = (int) (long) ((JSONObject) obj).get("node hash");
            this.amount = (int) (long) ((JSONObject) obj).get("node amount");
            this.nodes = ((JSONObject) obj).get("nodes").toString();
        }
        else{
            System.out.println("Error, node does not exist");
        }
    }
    public void newNode(String user, String IP) throws UnirestException {
        //String URL = "http://localhost:8080/NamingServer/Nodes/" + user;
        String URL = "http://" + discoveryNode.getServerIP() + ":8080/NamingServer/Nodes/" + user;
        System.out.println(Unirest.post(URL).header("Content-Type", "application/json").body(IP).asString().getBody());

    }
    public void delete(String user){
        try {
            String url = "http://" + discoveryNode.getServerIP() + ":8080/NamingServer/Nodes/" + user;
            System.out.println(Unirest.delete(url).asString().getBody());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void printOut() throws UnknownHostException {
        System.out.println("Node IP:\t\t" + this.node_IP);
        System.out.println("NamingServer IP:\t" + this.namingServer_IP);
        System.out.println("Node hash:\t\t" + this.hash);
        System.out.println("Node amount:\t\t" + this.amount);
        System.out.println("Nodes:\t\t\t" + this.nodes);
        System.out.println("node hostname + IP : " + InetAddress.getLocalHost());
    }


    public static void main(String[] args) throws IOException, InterruptedException {
        System.out.println("Starting Node...");
        //turn off most of the logging
        ch.qos.logback.classic.Logger root = (ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory.getLogger("org.apache.http");
        root.setLevel(ch.qos.logback.classic.Level.OFF);
        String name;
        if (args.length > 0) {
            name = args[0];
        } else {
            System.out.println("Please give a name to your node!");
            return;
        }
        /*for local, run this test*/
        NamingNode node = new NamingNode(name);
        //new DiscoveryNode(name).start();
       Thread.sleep(15000);
        new ShutdownNode(node).start();
        //String IP = InetAddress.getLocalHost().getHostAddress();



        //node.newNode(name, IP);
        //node.getNode(name);
        //node.printOut();

        //test some files
        //node.getFile("testFile.txt");
        //node.getFile("testFile2.pdf");
        //node.getFile("testFile3.jpg");
        //node.delete();
    }
}