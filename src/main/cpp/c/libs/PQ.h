
#include "Node.h"

#ifndef C_VERSION_PQ_H
#define C_VERSION_PQ_H


class PQ {
public:
    void PQinit(int);

    bool PQempty();

    void PQinsert(Node *);

    Node *PQextractMax();

    Node *PQshowMax();

    void PQdisplay();

    int PQsize();

    void PQchange(int pos, Node *node);

    int PQfindPos(Node *node);

    PQ();

private:
    int heapsize;
    Node **array;
    int* qp;

    int LEFT(int i);

    int RIGHT(int i);

    int PARENT(int i);

    void Swap(int n1, int n2);

    void Heapify(int i);
};


#endif //C_VERSION_PQ_H
