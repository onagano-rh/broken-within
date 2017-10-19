package com.example;

import java.io.Serializable;

import org.hibernate.search.annotations.Analyze;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.Latitude;
import org.hibernate.search.annotations.Longitude;
import org.hibernate.search.annotations.Spatial;
import org.hibernate.search.annotations.Store;
import org.hibernate.search.spatial.Coordinates;

@Spatial
@Indexed
public class CacheEntity implements Coordinates, Serializable {
    private static final long serialVersionUID = -99721460717658431L;

    @Field(store = Store.YES, analyze = Analyze.NO)
    private String name;

    @Field
    @Latitude
    Double latitude;

    @Field
    @Longitude
    Double longitude;

    public CacheEntity(String name, double latitude, double longitude) {
        this.name = name;
        this.latitude = latitude;
        this.longitude = longitude;
    }
    
    public String getName() {
        return name;
    }

    public Double getLatitude() {
        return latitude;
    }

    public Double getLongitude() {
        return longitude;
    }
    
    @Override
    public String toString() {
        return String.format("[%s,%f,%f]", name, latitude, longitude);
    }
}
