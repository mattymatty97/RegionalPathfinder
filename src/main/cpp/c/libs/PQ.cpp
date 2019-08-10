

#include <cstdio>
#include "PQ.h"

int PQ::LEFT(int i) {
    return i * 2 + 1;
}

int PQ::RIGHT(int i) {
    return i * 2 + 2;
}

int PQ::PARENT(int i) {
    return (i - 1) / 2;
}


void PQ::PQinit(int size) {
    heapsize = 0;
    this->array = (Node **) malloc(size * sizeof(Node *));
    this->qp=(int*) malloc(size* sizeof(int));
}


bool PQ::PQempty() {
    return this->heapsize == 0;
}

void PQ::PQinsert(Node *node) {
    int i;
    i = this->heapsize++;
    while ((i >= 1) && (array[PARENT(i)]->getPQ_Value() < node->getPQ_Value())) {
        this->array[i] = this->array[PARENT(i)];
        qp[i]=qp[PARENT(i)];
        i = (i - 1) / 2;
    }
    array[i] = node;
    qp[i]=node->getID();
    return;
}


void PQ::Swap(int n1, int n2) {
    Node *temp;
    int t;

    temp = array[n1];
    t=array[n1]->getID();

    array[n1] = array[n2];
    qp[n1]=qp[n2];

    array[n2] = temp;
    qp[n2]=t;
    return;
}

void PQ::Heapify(int i) {
    int l, r, largest;
    l = LEFT(i);
    r = RIGHT(i);
    if ((l < this->heapsize) && (this->array[l]->getPQ_Value() < this->array[i]->getPQ_Value()))
        largest = l;
    else
        largest = i;
    if ((r < this->heapsize) && (this->array[r]->getPQ_Value() < this->array[largest]->getPQ_Value()))
        largest = r;
    if (largest != i) {
        Swap(i, largest);
        Heapify(largest);
    }
    return;
}

Node *PQ::PQextractMax() {

    Node *item;
    Swap(0, this->heapsize - 1);
    item = this->array[this->heapsize - 1];
    this->heapsize--;
    Heapify(0);
    return item;
}

Node *PQ::PQshowMax() {
    return this->array[0];
}

void PQ::PQdisplay() {
    int i;
    for (i = 0; i < this->heapsize; i++) {
        fprintf(stdout, "Node %d, pq_value= %d", array[i]->getID(), array[i]->getPQ_Value());
    }
}

int PQ::PQsize() {
    return heapsize;
}

void PQ::PQchange(int pos, Node *node) {
    while ((pos >= 1) && (this->array[PARENT(pos)]->getPQ_Value() > node->getPQ_Value())) {
        this->array[pos] = this->array[PARENT(pos)];
        pos = (pos - 1) / 2;
    }
    this->array[pos] = node;
    Heapify(pos);
    return;
}

int PQ::PQfindPos(Node *node) {
    /*for (int i = 0; i < this->heapsize; i++) {
        if (array[i]->getID() == node->getID())
            return i;
    }
    return -1;*/
    return qp[node->getID()];
}

PQ::PQ() {};
