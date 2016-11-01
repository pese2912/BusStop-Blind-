package com.bus.busstopblind.data.BusAPI;

import java.io.Serializable;

/**
 * Created by Duedapi on 2016-09-23.
 */

public class MyLocation  implements Serializable {

    private static MyLocation instance;
    public static MyLocation getInstance(){
        if(instance == null){
            instance = new MyLocation();
        }
        return instance;
    }

    double latitude;
    double longitude;

    public double getLatitude(){
        return latitude;
    }

    public double getLongitude(){
        return longitude;
    }

    public void setLatitude(double lat){
         latitude = lat;
    }

    public void setLongitude(double lon){
        longitude = lon;
    }
}
