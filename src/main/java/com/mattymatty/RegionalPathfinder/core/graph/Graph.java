package com.mattymatty.RegionalPathfinder.core.graph;


import com.mattymatty.RegionalPathfinder.RegionalPathfinder;
import com.mattymatty.RegionalPathfinder.api.region.Region;
import com.mattymatty.RegionalPathfinder.exeptions.GraphExeption;
import com.mattymatty.RegionalPathfinder.exeptions.NodeExeption;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings({"UseSparseArrays","WeakerAccess"})
public class Graph{
    static long ctn=0;

    long id;

    public Graph() {
        id=ctn++;
    }

    //memorize nodes by ID
    Map<Integer,Node> nodeIDMap = new HashMap<>();
    //edges does not need a map
    List<Edge> edges = new ArrayList<>();
    boolean changed=false;



    public void addNode(Node node) throws GraphExeption{
        if(node.graph!=this)
            throw new GraphExeption("Node not part of this graph");
        nodeIDMap.put(node.getN_id(),node);
    }

    public void addEdge(Edge edge) throws GraphExeption{
        if(edge.graph!=this)
            throw new GraphExeption("Edge not part of this graph");
        edges.add(edge);
        changed=true;
    }

    public List<Node> getNodes(){
        return new ArrayList<>(nodeIDMap.values());
    }

    public List<Edge> getEdges(){
        return new ArrayList<>(edges);
    }


    public Map<Integer,Node> getNodesById(){
        return new HashMap<>(nodeIDMap);
    }


    public List<Node> shortestPath(Node node1, Node node2) throws GraphExeption{
        if(node1.graph!=this || node2.graph!=this)
            throw new GraphExeption("Node is not part of this graph");
        return _shortestPath(node1.n_id,node2.n_id);
    }

    public List<Node> shortestPath(int id1,int id2) throws NodeExeption{
        Node node1 = nodeIDMap.get(id1);
        if(node1==null)
            throw new NodeExeption(id1,"Unknown Node ID");
        Node node2 = nodeIDMap.get(id2);
        if(node2==null)
            throw new NodeExeption(id2,"Unknown Node ID");
        return _shortestPath(id1,id2);
    }

    private List<Node> _shortestPath(int node1, int node2){
        //if the graph is not synced with c library sync it ( replace all datas :sweat_smile:)
        if(changed){
            loadCNodes(id,nodeIDMap.size(),nodeIDMap.values().stream().mapToInt(Node::getN_id).toArray());
            loadCEdges(id,edges.size(),edges.stream().map(Edge::getNode_1).mapToInt(Node::getN_id).toArray(),
                    edges.stream().map(Edge::getNode_2).mapToInt(Node::getN_id).toArray(),
                    edges.stream().mapToDouble(Edge::getDistance).toArray());
        }

        //call native method
        int[] ret = shortestCPath(id,node1,node2);

        //if no solution found
        if(ret.length<2)
            return null;

        //if a solution extract the nodes from id's and build the ordered list
        List<Node> path = new ArrayList<>();
        for ( int id : ret ){
            Node node = nodeIDMap.get(id);
            path.add(node);
        }

        //lock the list
        return Collections.unmodifiableList(path);
    }


    //native methods ( methods from c sources )
    private native void loadCNodes(long id,int size , int[] nodes);
    private native void loadCEdges(long id,int size , int[] startNodes,int[] endNodes,double[] weights);
    private native int[] shortestCPath(long id,int start,int end);

    //specify which library use to load native methods
    static {
        try {
            System.load(RegionalPathfinder.getInstance().getDataFolder().getAbsolutePath() + "/libs/libjni.so");
        }catch (Exception ex){
            ex.printStackTrace();
        }
    }

    static public class Node{
        final int n_id;
        final Graph graph;

        /**
         * Node class
         * Each node is a point of interest on the map
         * @param n_id local id used for pathfinding algorithm
         */
        public Node(Graph graph,int n_id) {
            this.n_id = n_id;
            this.graph=graph;
        }

        public int getN_id() {
            return n_id;
        }
    }

    static public class Edge {
        final Node node_1;
        final Node node_2;
        final double distance;
        final Graph graph;

        public Edge(Node node_1, Node node_2, double distance) throws GraphExeption {
            this.node_1 = node_1;
            this.node_2 = node_2;
            this.distance = distance;
            this.graph=node_1.graph;
            if(node_1.graph != node_2.graph)
                throw new GraphExeption("Nodes are not in the same graph");
        }

        public Node getNode_1() {
            return node_1;
        }

        public Node getNode_2() {
            return node_2;
        }

        public double getDistance() {
            return distance;
        }
    }

}