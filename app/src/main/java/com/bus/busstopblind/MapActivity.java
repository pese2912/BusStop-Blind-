package com.bus.busstopblind;

import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Point;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Build;
import android.os.Handler;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.bus.busstopblind.data.BusAPI.CrdntPrxmtSttnListResponse;
import com.bus.busstopblind.data.BusAPI.MyLocation;
import com.bus.busstopblind.data.BusAPI.SttnNoListResponse;
import com.bus.busstopblind.data.TmapAPI.TmapAPIResult;
import com.skp.Tmap.TMapData;
import com.skp.Tmap.TMapPoint;
import com.skp.Tmap.TMapPolyLine;
import com.skp.Tmap.TMapView;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import okhttp3.Request;

/**
 * Created by Sangsu on 2016-10-21.
 */

public class MapActivity extends AppCompatActivity {

    boolean CompleteFlag;

    public  static  final String TMAP_API_KEY = "d18832a7-4284-398e-a2a1-b8de8c49b2b0";
    SttnNoListResponse sttnNoListResponse;
    TMapView tmapview;
    CrdntPrxmtSttnListResponse crdntPrxmtSttnListResponse;
    LocationManager locationManager;
    String locationProvider;
    TextToSpeech tts;
    TmapAPIResult RouteResult;
    ArrayList<TMapPoint> PassList;
    BroadcastReceiver m_br;

    int Idx;

    IntentFilter filter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        CompleteFlag= false;

        Intent intent = getIntent();
        sttnNoListResponse = (SttnNoListResponse) intent.getExtras().getSerializable("sttnNoList");
        crdntPrxmtSttnListResponse = (CrdntPrxmtSttnListResponse) intent.getExtras().getSerializable("crdntPrxmtSttnListResponse");

        filter = new IntentFilter();
        filter.addAction(TextToSpeech.ACTION_TTS_QUEUE_PROCESSING_COMPLETED);

        RelativeLayout relativeLayout = new RelativeLayout(this);
        tmapview = new TMapView(this);
        tmapview.setSKPMapApiKey(TMAP_API_KEY);
        tmapview.setLanguage(TMapView.LANGUAGE_KOREAN);
        tmapview.setIconVisibility(true);
        tmapview.setLocationPoint(MyLocation.getInstance().getLongitude(), MyLocation.getInstance().getLatitude());
        tmapview.setZoomLevel(19);
        tmapview.setMapType(TMapView.MAPTYPE_STANDARD);
        tmapview.setCompassMode(true);

        tmapview.setTrackingMode(true);
        relativeLayout.addView(tmapview);
        setContentView(relativeLayout);

        PassList = new ArrayList<>();

        Idx = 0;

        //음성출력
        tts = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {

            @Override
            public void onInit(int status) {
                if (status != TextToSpeech.ERROR) {
                    tts.setLanguage(Locale.KOREAN);
                    tts.setSpeechRate(0.8f);

                }
            }
        });




        m_br = new BroadcastReceiver() {

            // 브로드캐스트 알림이 수신되면 호출되는 onReceive 메소드를 정의한다.
            public void onReceive(Context context, Intent intent) {
                // Intent 로부터 어떤 동작으로 인해 Broadcast Receiver 에게 알림이
                // 수신되었는지에 대한 정보를 가져온다.
                String act = intent.getAction();

                // TTS의 음성출력이 완료되어 알림이 수신된 경우
                if (act.equals(TextToSpeech.ACTION_TTS_QUEUE_PROCESSING_COMPLETED)) {

                    // 토스트 메시지를 통해 TTS 의 음성출력이 완료되었음을 사용자에게 알린다.
                    Toast.makeText(MapActivity.this, "TTS 음성 출력 완료", Toast.LENGTH_SHORT).show();
                    // 등록된 브로드캐스트 리시버를 해제한다.
                    unregisterReceiver(m_br);

                    if(CompleteFlag){
                        new Handler().postDelayed(new Runnable() {// 1 초 후에 실행
                            @Override
                            public void run() {
                                // 실행할 동작 코딩
                                BusArrival();
                            }
                        }, 2000);

                    }
                }
            }
        };

        // gps
        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

        Criteria criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_FINE); // 정확도
        criteria.setPowerRequirement(Criteria.POWER_HIGH); // 전원 소비량
        criteria.setAltitudeRequired(false); // 고도, 높이 값을 얻어 올지를 결정
        criteria.setBearingRequired(false);// provider 기본 정보(방위, 방향)
        criteria.setSpeedRequired(true); // 속도
        criteria.setCostAllowed(true); // 위치 정보를 얻어 오는데 들어가는 금전적 비용
        locationProvider = locationManager.getBestProvider(criteria, true);

/*

        // LocationManaer.NETWORK_PROVIDER : 기지국들로부터 현재 위치 확인
        // LocationManaer.GPS_PROVIDER : GPS들로부터 현재 위치 확인
        if (locationManager.isProviderEnabled(locationManager.GPS_PROVIDER) == true) {
            locationProvider = LocationManager.GPS_PROVIDER;
        } else
            locationProvider = LocationManager.NETWORK_PROVIDER;

*/

        Log.e("Provider", locationProvider);
        // 현재 위치를 조회한다. 결과는 locationListener를 통해 수신
        locationManager.requestLocationUpdates(locationProvider, 1000, 0,
                locationListener);

        try {

            // TMAP 보행자 경로 탐색
            NetworkManager.getInstance().getSearchFindPath(MyApplication.getContext(), MyLocation.getInstance().getLongitude(), MyLocation.getInstance().getLatitude(),
                    crdntPrxmtSttnListResponse.body.items.item.get(0).gpslong, crdntPrxmtSttnListResponse.body.items.item.get(0).gpslati,
                    "출발", crdntPrxmtSttnListResponse.body.items.item.get(0).nodenm, new NetworkManager.OnResultListener<TmapAPIResult>() {

                        @Override
                        public void onSuccess(Request request, TmapAPIResult result) {


                            Toast.makeText(MapActivity.this, "TMAP 경로탐색 성공", Toast.LENGTH_SHORT).show();

                            RouteResult = result;


                            for(int i=0; i< result.features.size(); i++){

                                if(result.features.get(i) !=null && i != 0){
                                    if(result.features.get(i).geometry.type.equals("Point")){

                                        TMapPoint point = new TMapPoint(result.features.get(i).geometry.coordinates[1],result.features.get(i).geometry.coordinates[0]);
                                        PassList.add(point);
                                    }
                                }
                            }


                            if(Idx == 0 && RouteResult.features != null) {

                                String toSpeak = RouteResult.features.get(Idx).properties.description + "해주세요.";

                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                    ttsGreater21(toSpeak);
                                } else {
                                    ttsUnder20(toSpeak);
                                }

                                Idx++;
                            }
                        }


                        @Override
                        public void onFail(Request request, IOException exception) {
                            Toast.makeText(MapActivity.this, "TMAP 경로탐색 실패"+exception.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });


        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        drawLine();

    }


    // 버스 도착정보 액티비티로 전환
    public void BusArrival(){

        locationManager.removeUpdates(locationListener);
        Intent intent = new Intent(MapActivity.this, ArrivalActivity.class);
        intent.putExtra("crdntPrxmtSttnListResponse",crdntPrxmtSttnListResponse);
        startActivity(intent);
    }


    //GPS 수신시
    LocationListener locationListener = new LocationListener() {
        public void onLocationChanged(Location location) {


            MyLocation.getInstance().setLongitude(location.getLongitude());
            MyLocation.getInstance().setLatitude(location.getLatitude());
            drawLine();

            if(!CompleteFlag) {
                if (Idx != 0 && Idx < RouteResult.features.size()) {

                    Log.e("MyLocation :  ", location.getLongitude() + "   ," + location.getLatitude());
                    Log.e("Gps Path :  ", RouteResult.features.get(Idx).geometry.coordinates[0] + "   ," + RouteResult.features.get(Idx).geometry.coordinates[1]);


                    if (RouteResult.features.get(Idx).geometry.type.equals("LineString")) {
                        Idx++;
                    } else {

                        //거리 구하기
                        double distance;

                        Location locationA = new Location("point A");

                        locationA.setLatitude(MyLocation.getInstance().getLatitude());
                        locationA.setLongitude(MyLocation.getInstance().getLongitude());

                        Location locationB = new Location("point B");

                        locationB.setLatitude(RouteResult.features.get(Idx).geometry.coordinates[1]);
                        locationB.setLongitude(RouteResult.features.get(Idx).geometry.coordinates[0]);

                        distance = locationA.distanceTo(locationB);

                        Log.e("다음 거리 :  ", distance + "");

                        //음성 경로 안내
                        if (distance <= 7) {

                            String toSpeak = RouteResult.features.get(Idx).properties.description + "해주세요.";

                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                ttsGreater21(toSpeak);
                            } else {
                                ttsUnder20(toSpeak);
                            }

                            Idx++;
                        }
                    }
                } else {


                    double distance;

                    Location locationA = new Location("point A");

                    locationA.setLatitude(MyLocation.getInstance().getLatitude());
                    locationA.setLongitude(MyLocation.getInstance().getLongitude());

                    Location locationB = new Location("point B");


                    locationB.setLatitude(crdntPrxmtSttnListResponse.body.items.item.get(0).gpslati);
                    locationB.setLongitude(crdntPrxmtSttnListResponse.body.items.item.get(0).gpslong);

                    distance = locationA.distanceTo(locationB);

                    Log.e("다음 거리 :  ", distance + "");

                    //목적지 도착시
                    if (distance <= 10) {

                        String toSpeak = "목적지에 도착하였습니다. 버스도착정보를 안내하겠습니다.";

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            ttsGreater21(toSpeak);
                        } else {
                            ttsUnder20(toSpeak);
                        }

                        CompleteFlag = true;
                    }
                }

            }
        }

        public void onStatusChanged(String provider, int status, Bundle extras) {
        }

        public void onProviderEnabled(String provider) {
        }

        public void onProviderDisabled(String provider) {
        }
    };



    @Override
    protected void onDestroy() {

        locationManager.removeUpdates(locationListener);
        if(tts !=null){
            tts.stop();
            tts.shutdown();
        }

        super.onDestroy();
    }

    // 지도에 경로 그리기
    public void drawLine() {

        TMapPoint point1 = new TMapPoint(MyLocation.getInstance().getLatitude(), MyLocation.getInstance().getLongitude());
        TMapPoint point2 = new TMapPoint(crdntPrxmtSttnListResponse.body.items.item.get(0).gpslati, crdntPrxmtSttnListResponse.body.items.item.get(0).gpslong);


        final TMapData tmapdata = new TMapData();
        tmapdata.findPathDataWithType(TMapData.TMapPathType.PEDESTRIAN_PATH, point1, point2, PassList, 0, new TMapData.FindPathDataListenerCallback() {


            @Override
            public void onFindPathData(TMapPolyLine tMapPolyLine) {
                tmapview.removeAllTMapPolyLine();
                tmapview.setLocationPoint(MyLocation.getInstance().getLongitude(), MyLocation.getInstance().getLatitude());
                tmapview.setCompassMode(true);
                tmapview.addTMapPath(tMapPolyLine);

            }
        });
    }

    @SuppressWarnings("deprecation")
    private void ttsUnder20(String text) {
        HashMap<String, String> map = new HashMap<>();
        map.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "MessageId");
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, map);
        registerReceiver(m_br, filter);
    }
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void ttsGreater21(String text) {
        String utteranceId=this.hashCode() + "";
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId);
        registerReceiver(m_br, filter);
    }

}
