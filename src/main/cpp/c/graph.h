#ifndef POLITOMAP_GRAPH_H
#define POLITOMAP_GRAPH_H

#include <cstdlib>
#include "libs/Node.h"
#include "libs/Dijkstra.h"

    class Graph{
    public:
        Graph();

//receives nodes in array
        static void load_nodes(long id, int size, int nodes[]);

//receives edges as 3 arrays ( start , end , weight )
        static void load_edges(long id, int size, int start_nodes[], int end_nodes[], double weights[]);

//receives start and end node and returns int array of nodes ( possibly ended by -1 )
//return MUST be allocated
        static int *shortestpath(long id, int start, int end);

        void _load_nodes(int size, int nodes[]);

        void _load_edges(int size, int start_nodes[], int end_nodes[], double weights[]);

        int *_shortestpath(int start, int end);

        Node *_nodes= nullptr;
        int n_nodes=0;
        int *node_pos= nullptr;
    };

#endif