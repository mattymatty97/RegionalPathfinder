package com.mattymatty.RegionalPathfinder.path.loader;

public interface Loader {
    void load(LoadData data);

    void evaluate(LoadData data);

    void validate(LoadData data);
}
