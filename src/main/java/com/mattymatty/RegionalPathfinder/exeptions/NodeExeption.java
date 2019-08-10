package com.mattymatty.RegionalPathfinder.exeptions;

public class NodeExeption extends GraphExeption {
    Long nodeId;
    String nodeName;
    public NodeExeption(long id,String message){
        super(message);
        nodeId=id;
    }
    public NodeExeption(String name,String message){
        super(message);
        nodeName=name;
    }

    public long getNodeId() {
        return nodeId;
    }

    public String getNodeName() {
        return nodeName;
    }

    public String getCompleteMessage(){
        StringBuilder sb = new StringBuilder(getMessage());
        sb.append(" : ");
        if(nodeId!=null)
            sb.append(nodeId);
        if(nodeName!=null)
            sb.append(nodeName);
        return sb.toString();
    }
}
