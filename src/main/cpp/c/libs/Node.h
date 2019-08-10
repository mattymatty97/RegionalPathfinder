#include <cstdlib>
#include "Edge.h"


#ifndef C_VERSION_NODE_H
#define C_VERSION_NODE_H
#define MAX_VALUE 30
#define STD_VALUE 15


class Node {
private:
    int ID;
    Edge* edges;
    int e_size;
    int n_edges;
    double pq_Value;

public:
    Node(int ID);
    Node();
    int addEdge(int ID, double value);
    Edge* getEdges();
    int getID();
    int getN_Edges();
    double getPQ_Value();
    void setPQ_Value(double value);

    //static functions
    static int generateNodes(Node *nodes, int n_nodes);
    static int _generateNodes(Node *nodes, Node *current, int h, int treeHeight, int* n, int n_nodes);
    static int generateEdges(Node *nodes, int n_nodes, int n_edges);
    static int testEdges(Node *nodes, int n_nodes);

    };
#endif //C_VERSION_NODE_H





