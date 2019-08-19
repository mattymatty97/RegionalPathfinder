package com.mattymatty.RegionalPathfinder.exeptions;

import com.mattymatty.RegionalPathfinder.api.region.Region;

public class AsyncExecption extends RegionException {
    public AsyncExecption(Region region) {
        super(region);
    }

    public AsyncExecption(String message, Region region) {
        super(message, region);
    }

    public AsyncExecption(String message, Throwable cause, Region region) {
        super(message, cause, region);
    }

    public AsyncExecption(Throwable cause, Region region) {
        super(cause, region);
    }

    public AsyncExecption(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace, Region region) {
        super(message, cause, enableSuppression, writableStackTrace, region);
    }
}
