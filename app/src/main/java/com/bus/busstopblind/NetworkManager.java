package com.bus.busstopblind;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.CookieManager;
import java.net.URLEncoder;
import java.util.concurrent.TimeUnit;

import okhttp3.Cache;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.JavaNetCookieJar;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.bus.busstopblind.data.BusAPI.AcctoArvlPrearngeInfoList;
import com.bus.busstopblind.data.BusAPI.CrdntPrxmtSttnList;
import com.bus.busstopblind.data.BusAPI.SttnNoListResult;
import com.bus.busstopblind.data.TmapAPI.GeometryDeserializer;
import com.bus.busstopblind.data.TmapAPI.TmapAPIResult;
import com.bus.busstopblind.data.TmapAPI.TmapGeometry;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;


/**
 * Created by Sangsu on 2016-10-21.
 */

public class NetworkManager {

    private static NetworkManager instance;
    public static NetworkManager getInstance(){
        if(instance == null){
            instance = new NetworkManager();
        }
        return instance;
    }
    private  static  final String ServiceKey  = "lITsDDRhJmaQbwahj3PB0QbkZp6sURTz2YujhI1qf1t1y8xpQPYkdrAa133qM5Fx0pw7hYE0HfXFFANZBB2sWw%3D%3D";
    private static final int DEFAULT_CACHE_SIZE = 50*1024*1024;
    private static final String DEFAULT_CACHE_DIR="miniapp";
    OkHttpClient mClient;

    private NetworkManager(){

        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        Context context = MyApplication.getContext();
        CookieManager cookieManager = new CookieManager();
        builder.cookieJar(new JavaNetCookieJar(cookieManager)); //메모리 저장하는 쿠키

        File dir = new File(context.getExternalCacheDir(),DEFAULT_CACHE_DIR);
        if(!dir.exists()){
            dir.mkdir();
        }

        builder.cache(new Cache(dir, DEFAULT_CACHE_SIZE));

        builder.connectTimeout(30, TimeUnit.SECONDS);
        builder.readTimeout(30, TimeUnit.SECONDS);
        builder.writeTimeout(30, TimeUnit.SECONDS);

        mClient = builder.build(); // 외장메모리 저장하는 캐시

    }

    public interface OnResultListener<T>{
        public  void onSuccess(Request request, T result);
        public void onFail(Request request, IOException exception);
    }

    private static final  int MESSAGE_SUCCESS= 1;
    private static final int MESSAGE_FAIL = 0;

    class NetworkHandler extends Handler {
        public NetworkHandler(Looper looper){
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            NetworkResult result  = (NetworkResult)msg.obj;

            switch (msg.what){
                case  MESSAGE_SUCCESS:
                    result.listener.onSuccess(result.request, result.result);
                    break;

                case MESSAGE_FAIL:
                    result.listener.onFail(result.request, result.exception);
                    break;
            }
        }
    }

    NetworkHandler mHandler = new NetworkHandler(Looper.getMainLooper());
    static class NetworkResult<T>{
        Request request;
        OnResultListener<T> listener;
        IOException exception;
        T result;

    }

    Gson gson = new Gson();
    Gson routeGson =  new GsonBuilder().registerTypeAdapter(TmapGeometry.class, new GeometryDeserializer()).create();

    public static final String BUS_SERVER ="http://openapi.tago.go.kr/openapi/service";
    private static final String BUS_STOP_NAME_URL = BUS_SERVER + "/BusSttnInfoInqireService/getSttnNoList?ServiceKey=%s&cityCode=%s&nodeNm=%s&numOfRows=%s&pageNo=%s";


    public Request getSttnNoList(Object tag, int cityCode, String nodeNm, int numOfRows,int pageNo, //정류소명으로 정류장 찾기
                                                OnResultListener<SttnNoListResult> listener) {

        String url = String.format(BUS_STOP_NAME_URL, ServiceKey, cityCode, nodeNm, numOfRows,pageNo);

        Request request = new Request.Builder()
                .url(url)
                .header("Accept", "application/json")
                .build();

        final NetworkResult<SttnNoListResult> result = new NetworkResult<>();
        result.request = request;
        result.listener = listener;

        mClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                result.exception = e;
                mHandler.sendMessage(mHandler.obtainMessage(MESSAGE_FAIL, result));
            }


            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    SttnNoListResult data= new SttnNoListResult();
                    try {
                      //  Log.e("daatatata",response.body().string() );
                        data = gson.fromJson(response.body().charStream(), SttnNoListResult.class);
                    }catch (JsonSyntaxException e){
                        Log.d("JsonSyntaxException", e.getMessage());
                    }

                    if(data!= null) {

                        result.result = data;

                    }

                    mHandler.sendMessage(mHandler.obtainMessage(MESSAGE_SUCCESS, result));

                }else{
                    throw new IOException(response.message());
                }
            }
        });

        return request;
    }


    private static final String BUS_STOP_LOCATION_URL = BUS_SERVER + "/BusSttnInfoInqireService/getCrdntPrxmtSttnList?ServiceKey=%s&gpsLati=%s&gpsLong=%s&numOfRows=%s&pageNo=%s";


    public Request getCrdntPrxmtSttnList(Object tag, double gpsLati, double gpsLong, int numOfRows,int pageNo,
                                 OnResultListener<CrdntPrxmtSttnList> listener) {

        String url = String.format(BUS_STOP_LOCATION_URL, ServiceKey, gpsLati, gpsLong, numOfRows,pageNo);

        Request request = new Request.Builder()
                .url(url)
                .header("Accept", "application/json")
                .build();

        final NetworkResult<CrdntPrxmtSttnList> result = new NetworkResult<>();
        result.request = request;
        result.listener = listener;

        mClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                result.exception = e;
                mHandler.sendMessage(mHandler.obtainMessage(MESSAGE_FAIL, result));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    CrdntPrxmtSttnList data= new CrdntPrxmtSttnList();

                    try {
                    //    Log.e("daatatata",response.body().string() );
                        data = gson.fromJson(response.body().charStream(), CrdntPrxmtSttnList.class);
                    }catch (JsonSyntaxException e){
                        Log.d("JsonSyntaxException", e.getMessage());
                    }

                    if(data!= null) {

                        result.result = data;
                    }

                    mHandler.sendMessage(mHandler.obtainMessage(MESSAGE_SUCCESS, result));

                }else{
                    throw new IOException(response.message());
                }
            }
        });

        return request;
    }


    private static final String BUS_STOP_ARRIVAL_URL = BUS_SERVER + "/ArvlInfoInqireService/getSttnAcctoArvlPrearngeInfoList?ServiceKey=%s&cityCode=%s&nodeId=%s&numOfRows=%s&pageNo=%s";


    public Request getSttnAcctoArvlPrearngeInfoList(Object tag, int cityCode, String nodeId, int numOfRows,int pageNo,
                                         OnResultListener<AcctoArvlPrearngeInfoList> listener) {

        String url = String.format(BUS_STOP_ARRIVAL_URL, ServiceKey, cityCode, nodeId, numOfRows,pageNo);

        Request request = new Request.Builder()
                .url(url)
                .header("Accept", "application/json")
                .build();

        final NetworkResult<AcctoArvlPrearngeInfoList> result = new NetworkResult<>();
        result.request = request;
        result.listener = listener;

        mClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {

                result.exception = e;
                mHandler.sendMessage(mHandler.obtainMessage(MESSAGE_FAIL, result));

            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    AcctoArvlPrearngeInfoList data= new AcctoArvlPrearngeInfoList();

                    try {
                       // Log.e("daatatata",response.body().string() );
                        data = gson.fromJson(response.body().charStream(), AcctoArvlPrearngeInfoList.class);

                    }catch (JsonSyntaxException e){
                        Log.d("JsonSyntaxException", e.getMessage());
                    }

                    if(data!= null) {

                        result.result = data;
                    }

                    mHandler.sendMessage(mHandler.obtainMessage(MESSAGE_SUCCESS, result));

                }else{
                    throw new IOException(response.message());
                }
            }
        });

        return request;
    }

    public static final String TMAP_SERVER ="https://apis.skplanetx.com/tmap";
    private static final String SEARCH_FIND_PATH_URL = TMAP_SERVER + "/routes/pedestrian?callback=&version=1";

    public Request getSearchFindPath(Object tag, double startX, double startY,  double endX, double endY, String startName, String endName,
                                     OnResultListener<TmapAPIResult> listener)  throws UnsupportedEncodingException {

        RequestBody body = new FormBody.Builder() // 바디 설정
                .add("startX", startX + "")
                .add("startY", startY+"")
                .add("endX", endX + "")
                .add("endY", endY+"")
                .add("startName", startName)
                .add("endName", endName)
                .add("reqCoordType", "WGS84GEO")
                .add("resCoordType", "WGS84GEO")
                .build();

        Request request = new Request.Builder()
                .url(SEARCH_FIND_PATH_URL)
                .addHeader("Accept", "application/json")
                .addHeader("appKey", "d18832a7-4284-398e-a2a1-b8de8c49b2b0")
                .post(body)
                .build();


        final NetworkResult<TmapAPIResult> result = new NetworkResult<>();
        result.request = request;
        result.listener = listener;

        mClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                result.exception = e;
                mHandler.sendMessage(mHandler.obtainMessage(MESSAGE_FAIL, result));

            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {

                if (response.isSuccessful()) {

                    TmapAPIResult data= new TmapAPIResult();

                    try {

                        data = routeGson.fromJson(response.body().charStream(), TmapAPIResult.class);




                    }catch (JsonSyntaxException e){
                            e.printStackTrace();
                    }

                    if(data!= null) {
                        result.result = data;
                    }

                    mHandler.sendMessage(mHandler.obtainMessage(MESSAGE_SUCCESS, result));

                }else{
                    throw new IOException(response.message());
                }
            }
        });

        return request;
    }


}
