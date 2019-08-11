#include <cstdlib>
#include "graph.h"


static Graph *graphs;
static bool *used;
static long last = -1;

static Graph *getGraph(long id) {
    if (id > last) {
        graphs = (Graph *) realloc(graphs, (id + 1) * sizeof(Graph));
        used = (bool *) realloc(used, (id + 1) * sizeof(bool));
        for (long i = last + 1; i <= id; i++)
            used[i] = false;
        last = id;
    }

    if (!used[id]) {
        graphs[id] = Graph();
        used[id] = true;
    }
    Graph *graph = &graphs[id];
    return graph;
}

//receives nodes in array
void Graph::load_nodes(long id, int size, int nodes[]) {
    (*getGraph(id))._load_nodes(size, nodes);
}

void Graph::load_edges(long id, int size, int start_nodes[], int end_nodes[], double weights[]) {
    (*getGraph(id))._load_edges(size, start_nodes, end_nodes, weights);
}

int *Graph::shortestpath(long id, int start, int end) {
    return (*getGraph(id))._shortestpath(start, end);
}

Graph::Graph() {
    n_nodes = 0;
    node_pos = nullptr;
    _nodes = nullptr;
}

void Graph::_load_nodes(int size, int nodes[]) {

    if (_nodes != NULL)
        free(_nodes);

    if (size < 1 || nodes == NULL) return;

    n_nodes = size;
    _nodes = (Node *) malloc(size * sizeof(Node));
    node_pos = (int *) malloc(size * sizeof(int));

    for (int i = 0; i < size; i++) {
        _nodes[i] = Node(nodes[i]);
        node_pos[nodes[i]] = i;
    }
}

//receives edges as 3 arrays ( start , end , weight )
void Graph::_load_edges(int size, int start_nodes[], int end_nodes[], double weights[]) {

    for (int i = 0; i < size; i++) {

        int ID1 = start_nodes[i];
        int ID2 = end_nodes[i];
        double weight = weights[i];

        _nodes[node_pos[ID1]].addEdge(ID2, weight);
        _nodes[node_pos[ID2]].addEdge(ID1, weight);
    }

}

//receives start and end node and returns int array of nodes ( possibly ended by -1 )
//return MUST be allocated
int *Graph::_shortestpath(int start, int end) {
    int *ret = nullptr;

    //the result array is allocated internally
    int n_steps = Dijkstra::dijkstra(start, _nodes, end, n_nodes, &ret);

    return ret;
}
