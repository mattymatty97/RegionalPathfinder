//
// Created by pathfinders on 03/11/18.
//
#include <jni.h>
#include <string>

#include "graph.h"

//adapter library for loadCNodes
extern "C" JNIEXPORT void JNICALL
Java_com_mattymatty_RegionalPathfinder_core_graph_Graph_loadCNodes(
        JNIEnv *env,
        jobject /* this */,jlong id,jint size, jintArray nodes) {
        jboolean boolean;
        //convert java array to c array
        int * nodes_array = env->GetIntArrayElements(nodes,&boolean);
        //call function
        Graph::load_nodes(id,size,nodes_array);
}

//adapter library for loadCEdges
extern "C" JNIEXPORT void JNICALL
Java_com_mattymatty_RegionalPathfinder_core_graph_Graph_loadCEdges(
        JNIEnv *env,
        jobject /* this */,jlong id,jint size, jintArray start_nodes,jintArray end_nodes,jdoubleArray weights) {
        jboolean boolean;
        //convert java arrays to c arrays
        int* start_nodes_array = env->GetIntArrayElements(start_nodes,&boolean);
        int* end_nodes_array = env->GetIntArrayElements(end_nodes,&boolean);
        double * weights_array = env->GetDoubleArrayElements(weights,&boolean);
        //call function
        Graph::load_edges(id,size,start_nodes_array,end_nodes_array,weights_array);
}

//adapter library for shortestCPath
extern "C" JNIEXPORT jintArray JNICALL
Java_com_mattymatty_RegionalPathfinder_core_graph_Graph_shortestCPath(
        JNIEnv *env,
        jobject /* this */,jlong id,jint start,jint end) {
    jintArray jret;
    int i=0;
    //call function
    int* cret = Graph::shortestpath(id,start,end);
    //read array lenght
    for(i=0;(cret[i]!=-1)/*&&(cret[i]!=end)*/;i++);
    //build java array
    jret = env->NewIntArray(i);
    env->SetIntArrayRegion(jret,0,i-1,cret);
    //free memory
    free(cret);
    return jret;
}