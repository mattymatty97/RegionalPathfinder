#include "Edge.h"
Edge::Edge(int ID_From, int ID_To, double value) {

    this->ID_From=ID_From;
    this->ID_To=ID_To;
    this->value=value;
}

int Edge::getID_From(){
    return this->ID_From;
}

int Edge::getID_To(){
    return this->ID_To;
}
double Edge::getValue(){
    return this->value;
}