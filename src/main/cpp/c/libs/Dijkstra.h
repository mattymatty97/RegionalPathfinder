#ifndef POLITOMAP_DIJKSTRA_H
#define POLITOMAP_DIJKSTRA_H

#include <cstdlib>
#include <time.h>
#include <chrono>
#include <fstream>
#include "Node.h"
#include "PQ.h"


class Dijkstra {


public:
    static int dijkstra(int startID, Node *nodes, int endID, int n_nodes, int** path);
};


#endif //POLITOMAP_DIJKSTRA_H
