package com.mattymatty.RegionalPathfinder.exeptions;

import com.mattymatty.RegionalPathfinder.api.region.Region;

public class LoaderException extends RegionException {
    public LoaderException(Region region) {
        super(region);
    }

    public LoaderException(String message, Region region) {
        super(message, region);
    }

    public LoaderException(String message, Throwable cause, Region region) {
        super(message, cause, region);
    }

    public LoaderException(Throwable cause, Region region) {
        super(cause, region);
    }

    public LoaderException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace, Region region) {
        super(message, cause, enableSuppression, writableStackTrace, region);
    }
}
