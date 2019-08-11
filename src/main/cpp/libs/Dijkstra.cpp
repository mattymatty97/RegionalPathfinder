#include <iostream>
#include <math.h>
#include <limits.h>
#include "Dijkstra.h"

#define VERBOSE true

int Dijkstra::dijkstra(int startID, Node *nodes, int endID, int n_nodes, int** path) {

    Node *v, *w;
    Edge *t;

    //init heap-based priority queue
    PQ pq = PQ();
    pq.PQinit(n_nodes);

    //init of parent array
    int *st;
    st = (int *) malloc(n_nodes * sizeof(int));

    if (startID >= n_nodes || startID < 0) return NULL;
    if (endID>= n_nodes || endID < 0) return NULL;

    //initial insertion of nodes in PQ
    for (int i = 0; i < n_nodes; i++) {
        st[i] = -1;
        pq.PQinsert(&nodes[i]);
        //initial value of distance is INT_MAX
    }

    //the distance from himself is 0
    nodes[startID].setPQ_Value(0);
    //the father of the first node is himself
    st[startID] = startID;

    //update the priority queue with the distance value
    pq.PQchange(startID, &(nodes[startID]));

    while (!pq.PQempty()) {
        if ((v = pq.PQextractMax())->getPQ_Value() != INT_MAX) {
            for (int i = 0; i < v->getN_Edges(); i++) {    //for every edge of the node
                t = &(v->getEdges()[i]);
                w = &nodes[t->getID_To()];
                if (v->getPQ_Value() + t->getValue() <
                    w->getPQ_Value()) {  //if the distance of the node + edge distance is less than distance of the connected node
                    w->setPQ_Value(
                            v->getPQ_Value() + t->getValue());     //the distance of the connected node is updated
                    pq.PQchange(pq.PQfindPos(w),
                                w);                    //update of PQ (I need to find the position of node for the PQChange)
                    st[w->getID()] = v->getID();                          //update the father array
                }
            }
        }
    }

    if(VERBOSE){
        for (int i = 0; i < n_nodes; i++) {
            fprintf(stdout, "parent of %d id %d \n", i, st[i]);
        }
        fprintf(stdout, "min distances from %d\n", startID);
        for (int i = 0; i < n_nodes; i++) {
            fprintf(stdout, "%d : %d metres\n", i, nodes[i].getPQ_Value());
        }
    }
    fprintf(stdout, "\n\nPath from %d to %d:\n", startID, endID);
    int j = endID;
    int n_steps=0;
    while (st[j] != j) {
        fprintf(stdout, "%d->", j);
        j = st[j];
        n_steps++;
    }
    fprintf(stdout, "%d", startID);
    fprintf(stdout, "\nWeight=%d", nodes[endID].getPQ_Value());

    *path=(int*)malloc((n_steps+2)* sizeof(int));

    j=endID;
    for(int i=n_steps-1; i>=0; i--, j=st[j]){
        (*path)[i]=st[j];
    }

    //adding terminating value (-1)
    (*path)[0]=startID;
    (*path)[n_steps]=endID;
    (*path)[n_steps+1]=-1;

    return n_steps+1;
}
