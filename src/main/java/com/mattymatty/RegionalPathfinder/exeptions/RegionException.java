package com.mattymatty.RegionalPathfinder.exeptions;

import com.mattymatty.RegionalPathfinder.api.region.Region;

public class RegionException extends RuntimeException {
    private final Region region;

    public RegionException(Region region) {
        this.region = region;
    }

    public RegionException(String message, Region region) {
        super(message);
        this.region = region;
    }

    public RegionException(String message, Throwable cause, Region region) {
        super(message, cause);
        this.region = region;
    }

    public RegionException(Throwable cause, Region region) {
        super(cause);
        this.region = region;
    }

    public RegionException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace, Region region) {
        super(message, cause, enableSuppression, writableStackTrace);
        this.region = region;
    }

    public Region getRegion() {
        return region;
    }
}
