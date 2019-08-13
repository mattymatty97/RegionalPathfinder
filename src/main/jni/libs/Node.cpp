#include <limits.h>
#include <cstdio>
#include <math.h>
#include <time.h>
#include "Node.h"


#define VERBOSE true


Node::Node() : ID(0), n_edges(0), e_size(4), edges(), pq_Value(INT_MAX) {}

Node::Node(int ID) {
    this->ID = ID;
    this->edges = (Edge *) malloc(4 * sizeof(Edge));
    e_size = 4;
    n_edges = 0;
    pq_Value = INT_MAX;
}

int Node::addEdge(int ID, double value) {
    for (int i = 0; i < this->n_edges; i++) {
        if (this->edges[i].getID_To() == ID) {
            //fprintf(stdout, "!!!Edge already present (%d, %d)\n",this->ID,ID);
            return -1;
        }

    }

    if (n_edges == e_size) {
        this->edges = (Edge *) realloc(edges, 4 * e_size * sizeof(Edge));
        e_size *= 4;
    }

    edges[n_edges] = Edge(this->ID, ID, value);
    this->n_edges++;

    if(VERBOSE)
        fprintf(stdout, "\nCreate edge between %d and %d, value=%d", this->ID, ID, value);

    return n_edges;
}

int Node::getID() {
    return this->ID;
}

Edge *Node::getEdges() {
    return this->edges;
}

int Node::getN_Edges() {
    return this->n_edges;
}

double Node::getPQ_Value() {
    return this->pq_Value;
}

void Node::setPQ_Value(double value) {
    this->pq_Value = value;
}


//static functions

int Node::generateNodes(Node *nodes, int n_nodes) {

    int n=0;
    int treeHeight = (int) floor(log2(n_nodes));
    nodes[0] = Node(0);

    Node::_generateNodes(nodes, &nodes[0], 1, treeHeight, &n, n_nodes);
}

 int Node::_generateNodes(Node *nodes, Node *current, int h, int treeHeight, int* n, int n_nodes) {

    //left
    if (*n < n_nodes - 1 && h <= treeHeight) {
        (*n)++;
        nodes[*n] = Node(*n);

        current->addEdge(*n, STD_VALUE);
        nodes[*n].addEdge(current->getID(), STD_VALUE);

        if(VERBOSE){
            for (int i = 0; i < h; i++) {
                fprintf(stdout, "|\t");
            }
            fprintf(stdout, "left son of %d created (ID=%d)\n", current->getID(), *n);
            fflush(stdout);
        }

        _generateNodes(nodes, &nodes[*n], h + 1, treeHeight, n, n_nodes);
    }

    //right
    if (*n < n_nodes - 1 && h <= treeHeight) {
        (*n)++;
        nodes[*n] = Node(*n);

        current->addEdge(*n, STD_VALUE);
        nodes[*n].addEdge(current->getID(), STD_VALUE);

        if(VERBOSE){
            for (int i = 0; i < h; i++) {
                fprintf(stdout, "|\t");
            }
            fprintf(stdout, "right son of %d created (ID=%d)\n", current->getID(), *n);
            fflush(stdout);
        }

        _generateNodes(nodes, &nodes[*n], h + 1, treeHeight, n, n_nodes);
    }

}

int Node::generateEdges(Node *nodes, int n_nodes, int n_edges) {

    int ID1, ID2, value;
    srand((unsigned) time(nullptr));

    //  |
    // \|/ I have to subtract the number of already existing edges (equals to number of nodes)

    for (int i = n_nodes; i < n_edges; i++) {
        ID1 = (rand() % n_nodes);
        while ((ID2 = (rand() % n_nodes)) == ID1);
        value = rand() % MAX_VALUE;

        nodes[ID1].addEdge(ID2, value);
        nodes[ID2].addEdge(ID1, value);
    }

    fprintf(stdout, "\n\n");

}

int Node::testEdges(Node *nodes, int n_nodes) {
    int e = 0;
    int sum_wh = 0;

    for (int i = 0; i < n_nodes; i++) {
        if(VERBOSE)
            printf("\nNODE %d CONNECTED TO ", i);
        for (int j = 0; j < nodes[i].getN_Edges(); j++) {
            if(VERBOSE)
                printf("%d ", nodes[i].getEdges()[j].getID_To());

            e++;
            sum_wh += nodes[i].getEdges()[j].getValue();
        }
    }

    fprintf(stdout, "\nNumber of nodes: %d", n_nodes);
    fprintf(stdout, "\nNumber of edges: %d", (e / 2));
    fprintf(stdout, "\nAverage edges per node: %f", ((e / 2) / (float) n_nodes));

}