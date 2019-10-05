package com.mattymatty.RegionalPathfinder.core;

import com.mattymatty.RegionalPathfinder.RegionalPathfinder;
import com.mattymatty.RegionalPathfinder.api.Status;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;

import java.lang.ref.SoftReference;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public class StatusImpl<T> implements Status<T> {
    public Exception ex = null;
    public long syncTime = 0;
    public long totTime = 0;
    public float percentage = 0.0f;
    public static boolean sync = true;
    private boolean stop = false;
    private int status = 0;
    private SoftReference<T> product = new SoftReference<>(null);

    private Runnable onSchedule;
    private Consumer<Float> onProgress;
    private Consumer<T> onDone;
    private Consumer<Exception> onException;

    private AtomicBoolean changed = new AtomicBoolean(false);
    private Semaphore sem;
    private Runnable onSyncSchedule;
    private Consumer<Float> onSyncProgress;
    private Consumer<T> onSyncDone;
    private Consumer<Exception> onSyncException;

    private Thread eventThread;
    private BukkitTask syncronousLooper;

    @Override
    public synchronized boolean isScheduled() {
        return status == 1;
    }

    @Override
    public synchronized boolean isRunning() {
        return status == 2;
    }

    @Override
    public synchronized boolean isDone() {
        return status == 3;
    }

    @Override
    public synchronized boolean hasException() {
        return status == 4;
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
    public synchronized float getPercentage() {
        return percentage;
    }

    @Override
    public synchronized T getProduct() {
        return product.get();
    }

    @Override
    public StatusImpl<T> setOnSchedule(Runnable onSchedule) {
        this.onSchedule = onSchedule;
        if (sync)
            if (isScheduled())
                onSchedule.run();
        return this;
    }

    @Override
    public StatusImpl<T> setOnProgress(Consumer<Float> onProgress) {
        this.onProgress = onProgress;
        if (sync)
            if (isRunning())
                onProgress.accept(percentage);
        return this;
    }

    @Override
    public StatusImpl<T> setOnDone(Consumer<T> onDone) {
        this.onDone = onDone;
        if (sync)
            if (isDone())
                onDone.accept(product.get());
        return this;
    }

    @Override
    public StatusImpl<T> setOnException(Consumer<Exception> onException) {
        this.onException = onException;
        if (sync)
            if (hasException())
                onException.accept(ex);
        return this;
    }

    public StatusImpl() {
        this.sem = new Semaphore(0);
        if (!sync) {
            this.eventThread = new Thread(() -> {
                try {
                    while (!Thread.interrupted()) {
                        sem.acquire();
                        if (this.isScheduled()) {
                            if (this.onSchedule != null)
                                this.onSchedule.run();
                        } else if (this.isRunning()) {
                            if (this.onProgress != null)
                                this.onProgress.accept(percentage);
                        } else if (this.isDone()) {
                            if (this.onDone != null)
                                this.onDone.accept(product.get());
                            return;
                        } else if (this.hasException()) {
                            if (this.onException != null)
                                this.onException.accept(ex);
                            return;
                        }
                    }
                } catch (InterruptedException ignored) {
                }
            });
            this.syncronousLooper = Bukkit.getScheduler().runTaskTimer(RegionalPathfinder.getInstance(), this::syncRun, 1, 1);
            this.eventThread.start();
        }
    }

    @Override
    public StatusImpl<T> setOnSyncSchedule(Runnable onSyncSchedule) {
        this.onSyncSchedule = onSyncSchedule;
        if (sync) {
            if (isScheduled())
                onSyncSchedule.run();
        }
        return this;
    }

    @Override
    public StatusImpl<T> setOnSyncProgress(Consumer<Float> onSyncProgress) {
        this.onSyncProgress = onSyncProgress;
        if (sync) {
            if (isRunning())
                onSyncProgress.accept(percentage);
        }
        return this;
    }

    @Override
    public StatusImpl<T> setOnSyncDone(Consumer<T> onSyncDone) {
        this.onSyncDone = onSyncDone;
        if (sync) {
            if (isDone())
                onSyncDone.accept(product.get());
        }
        return this;
    }

    public synchronized StatusImpl setProduct(T product) {
        this.product = new SoftReference<>(product);
        return this;
    }

    @Override
    public StatusImpl<T> setOnSyncException(Consumer<Exception> onSyncException) {
        this.onSyncException = onSyncException;
        if (sync) {
            if (hasException())
                onSyncException.accept(ex);
        }
        return this;
    }

    public synchronized StatusImpl setStatus(int status) {
        this.status = status;
        sem.release();
        changed.set(true);
        if (sync) {
            syncRun();
        }
        return this;
    }

    private void syncRun() {
        if (stop) {
            if (this.syncronousLooper != null)
                this.syncronousLooper.cancel();
            return;
        }

        if (this.changed.get()) {
            changed.set(false);

            if (this.isScheduled()) {

                if (this.onSyncSchedule != null)
                    this.onSyncSchedule.run();

            } else if (this.isRunning()) {

                if (this.onSyncProgress != null)
                    this.onSyncProgress.accept(percentage);

            } else if (this.isDone()) {

                if (this.onSyncDone != null)
                    this.onSyncDone.accept(product.get());

                stop = true;

            } else if (this.hasException()) {

                if (this.onSyncException != null)
                    this.onSyncException.accept(ex);

                stop = true;
            }
        }
    }
}
