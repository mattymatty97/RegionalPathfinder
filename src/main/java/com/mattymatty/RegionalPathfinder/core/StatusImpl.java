package com.mattymatty.RegionalPathfinder.core;

import com.mattymatty.RegionalPathfinder.api.Status;
import org.bukkit.Location;

import java.util.List;
import java.util.Observable;
import java.util.Observer;
import java.util.function.Consumer;

public class StatusImpl<T> extends Observable implements Status<T> {
    int status=0;

    T product;

    public Exception ex = null;

    public long syncTime = 0;
    public long totTime = 0;

    @Override
    public synchronized boolean isScheduled() {
        return status==1;
    }

    @Override
    public synchronized boolean isRunning() {
        return status==2;
    }

    @Override
    public synchronized boolean isDone() {
        return status==3;
    }

    @Override
    public synchronized boolean hasException() {
        return status==4;
    }

    @Override
    public synchronized Exception getException() {
        return ex;
    }

    @Override
    public synchronized long getTimeSync() {
        return syncTime;
    }

    @Override
    public synchronized long getTimeTotal() {
        return totTime;
    }

    @Override
    public synchronized T getProduct() {
        return product;
    }

    @Override
    public synchronized Status<T> addListener(Consumer<Status<T>> callback) {
        this.addObserver((o,a)-> callback.accept(this));
        callback.accept(this);
        return this;
    }

    @Override
    public synchronized Status<T> clearListeners() {
        this.deleteObservers();
        return this;
    }


    public synchronized StatusImpl setStatus(int status) {
        this.status = status;
        changed();
        return this;
    }

    public synchronized StatusImpl setProduct(T product) {
        this.product = product;
        return this;
    }

    private void changed(){
        this.setChanged();
        this.notifyObservers();
    }
}
