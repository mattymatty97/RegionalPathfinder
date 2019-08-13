
#ifndef C_VERSION_EDGE_H
#define C_VERSION_EDGE_H


class Edge {

private:
    int ID_From;
    int ID_To;
    double value;

public:
    Edge(int ID_From, int ID_To, double value);
    int getID_From();
    int getID_To();
    double getValue();
};


#endif //C_VERSION_EDGE_H
