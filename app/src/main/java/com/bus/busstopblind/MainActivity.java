package com.bus.busstopblind;

import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Handler;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.bus.busstopblind.data.BusAPI.AcctoArvlPrearngeInfoList;
import com.bus.busstopblind.data.BusAPI.CrdntPrxmtSttnList;
import com.bus.busstopblind.data.BusAPI.CrdntPrxmtSttnListResponse;
import com.bus.busstopblind.data.BusAPI.MyLocation;
import com.bus.busstopblind.data.BusAPI.SttnNoListResponse;
import com.bus.busstopblind.data.BusAPI.SttnNoListResult;
import com.bus.busstopblind.data.TmapAPI.TmapAPIResult;
import com.skp.Tmap.TMapGpsManager;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;

import okhttp3.Request;

/**
 * Created by Sangsu on 2016-10-21.
 */

public class MainActivity extends AppCompatActivity {


    boolean CompleteFlag;

    IntentFilter filter;
    TextToSpeech tts;
    BroadcastReceiver m_br;
    Intent i;
    SpeechRecognizer mRecognizer;
    TextView textView;
    int cityCode;
    SttnNoListResponse sttnNoListResponse;
    CrdntPrxmtSttnListResponse crdntPrxmtSttnListResponse;
    GpsInfo gpsinfo;


    @Override
    protected void onCreate(Bundle savedInstanceState) {


        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        CompleteFlag = false;

        textView = (TextView) findViewById(R.id.ment);
        filter = new IntentFilter();
        filter.addAction(TextToSpeech.ACTION_TTS_QUEUE_PROCESSING_COMPLETED);

        //음성인식
        i = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        i.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, getPackageName());
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ko-KR");


        // Text To Speach 음성출력
        tts = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {

            @Override
            public void onInit(int status) {
                if (status != TextToSpeech.ERROR) {
                    tts.setLanguage(Locale.KOREAN);
                    tts.setSpeechRate(0.8f);
                    String toSpeak = getResources().getString(R.string.GuidanceMent);

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        ttsGreater21(toSpeak);
                    } else {
                        ttsUnder20(toSpeak);
                    }
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
                    Toast.makeText(MainActivity.this, "TTS 음성 출력 완료", Toast.LENGTH_SHORT).show();
                    // 등록된 브로드캐스트 리시버를 해제한다.
                    unregisterReceiver(m_br);

                    //경로를 찾기 위해 다음 액티비티 실행
                    if(CompleteFlag){
                        new Handler().postDelayed(new Runnable() {// 2 초 후에 실행
                            @Override
                            public void run() {

                                viewMap();
                            }
                        }, 2000);

                    }
                    //다시 음성인식
                    else {
                        mRecognizer = SpeechRecognizer.createSpeechRecognizer(context);
                        mRecognizer.setRecognitionListener(listener);
                        mRecognizer.startListening(i);
                    }
                }
            }
        };


        gpsinfo = new GpsInfo(MainActivity.this);
        // GPS 사용유무 가져오기
        if (gpsinfo.isGetLocation()) {

            MyLocation.getInstance().setLongitude(gpsinfo.getLongitude());
            MyLocation.getInstance().setLatitude(gpsinfo.getLatitude());


        } else {
            // GPS 를 사용할수 없으므로
            gpsinfo.showSettingsAlert();
        }
    }


    // Map 액티비티로 전환
    public void viewMap(){
        gpsinfo.stopUsingGPS();
        Intent intent = new Intent(MainActivity.this, MapActivity.class);
        intent.putExtra("sttnNoList",sttnNoListResponse);
        intent.putExtra("crdntPrxmtSttnListResponse",crdntPrxmtSttnListResponse);
        startActivity(intent);
    }

    @Override
    protected void onDestroy() {
        gpsinfo.stopUsingGPS();
        if(tts !=null){
            tts.stop();
            tts.shutdown();
        }
        //해제
        if(mRecognizer!=null)
            mRecognizer.destroy();

        super.onDestroy();
    }


    //음성출력
    @SuppressWarnings("deprecation")
    private void ttsUnder20(String text) {
        HashMap<String, String> map = new HashMap<>();
        map.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "MessageId");
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, map);
        // IntentFilter 에 추가된 동작이 발생할 경우 이 알림을 수신하는
        // Broadcast Receiver 를 등록한다.
        registerReceiver(m_br, filter);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void ttsGreater21(String text) {
        String utteranceId=this.hashCode() + "";
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId);
        // IntentFilter 에 추가된 동작이 발생할 경우 이 알림을 수신하는
        // Broadcast Receiver 를 등록한다.
        registerReceiver(m_br, filter);
    }

    //음성인식
    private RecognitionListener listener = new RecognitionListener() {
        @Override
        public void onReadyForSpeech(Bundle params) {

        }

        @Override
        public void onBeginningOfSpeech() {

        }

        //입력받는 소리의 크기
        @Override
        public void onRmsChanged(float rmsdB) {

        }

        @Override
        public void onBufferReceived(byte[] buffer) {

        }

        @Override
        public void onEndOfSpeech() {

        }

        @Override
        public void onError(int error) {

            if(error==7){
                //not match
            }else if(error==6){
                //time out
            }else if(error==3){
                Toast.makeText(MainActivity.this,"에러발생 : 다른 녹음기능이 켜져있는지 확인하세요.",Toast.LENGTH_SHORT).show();
            }else if(error==1||error==2){
                Toast.makeText(MainActivity.this,"에러발생 : 인터넷이 연결되어있는지 확인하세요.",Toast.LENGTH_SHORT).show();
            }else{
                Toast.makeText(MainActivity.this,"에러발생 에러코드 : "+error,Toast.LENGTH_SHORT).show();
            }
            if(mRecognizer!=null){
                mRecognizer.destroy();
                mRecognizer.setRecognitionListener(listener);
                mRecognizer.startListening(i);
            }else{
                mRecognizer.startListening(i);
            }
        }

        //음성인식 결과
        @Override
        public void onResults(Bundle results) {

            String key = "";
            key = SpeechRecognizer.RESULTS_RECOGNITION;
            ArrayList<String> mResult = results.getStringArrayList(key);
            final String[] rs = new String[mResult.size()];
            mResult.toArray(rs);
            Toast.makeText(MainActivity.this, "음성인식 완료 " + rs[0], Toast.LENGTH_SHORT).show();

            //현재 위치 GPS 기반으로 정류장 검색
            NetworkManager.getInstance().getCrdntPrxmtSttnList(MyApplication.getContext(), MyLocation.getInstance().getLatitude(), MyLocation.getInstance().getLongitude(), 999, 1, new NetworkManager.OnResultListener<CrdntPrxmtSttnList>() {
                @Override
                public void onSuccess(Request request, CrdntPrxmtSttnList result) {

                    //아무 결과 없을 시 다시 음성출력력
                    if (result.response == null) {
                       Toast.makeText(MainActivity.this, "현재 위치 정류장 검색 실패", Toast.LENGTH_SHORT).show();
                        if (mRecognizer != null) {
                            mRecognizer.destroy();

                            String toSpeak = getResources().getString(R.string.ReplyMent2);
                            textView.setText(toSpeak);
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                ttsGreater21(toSpeak);
                            } else {
                                ttsUnder20(toSpeak);
                            }
                        }

                    } else { // 검색 성공 시

                        cityCode = result.response.body.items.item.get(0).citycode;
                        Toast.makeText(MainActivity.this, "현재 위치 성공: " + result.response.body.items.item.get(0).nodenm, Toast.LENGTH_SHORT).show();
                        crdntPrxmtSttnListResponse = result.response;

                        // 음성인식으로 목적지 정류장 검색
                        NetworkManager.getInstance().getSttnNoList(MyApplication.getContext(),
                                cityCode, rs[0], 999, 1, new NetworkManager.OnResultListener<SttnNoListResult>() {
                                    @Override
                                    public void onSuccess(Request request, SttnNoListResult result) {

                                        if (result.response == null) { // 검색결과 없을 시 다시
                                            Toast.makeText(MainActivity.this, "목적지 정류장 검색 실패", Toast.LENGTH_SHORT).show();
                                            if (mRecognizer != null) {
                                                mRecognizer.destroy();

                                                String toSpeak = getResources().getString(R.string.ReplyMent);
                                                textView.setText(toSpeak);
                                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                                    ttsGreater21(toSpeak);
                                                } else {
                                                    ttsUnder20(toSpeak);
                                                }
                                            }

                                        } else { // 성공 시 경로 안내 시작
                                            Toast.makeText(MainActivity.this, "목적지 성공: " + result.response.body.items.item.get(0).nodenm, Toast.LENGTH_SHORT).show();
                                            sttnNoListResponse = result.response;

                                            CompleteFlag = true;
                                            tts.setLanguage(Locale.KOREAN);
                                            tts.setSpeechRate(0.8f);
                                            String toSpeak = "목적지는 "+result.response.body.items.item.get(0).nodenm+"입니다. 가까운 정류장인 "+
                                                    crdntPrxmtSttnListResponse.body.items.item.get(0).nodenm+"으로 경로안내를 시작합니다.";
                                            textView.setText(toSpeak);
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                                ttsGreater21(toSpeak);
                                            } else {
                                                ttsUnder20(toSpeak);
                                            }

                                        }
                                    }


                                    // 실패 시
                                    @Override
                                    public void onFail(Request request, IOException exception) {
                                        Toast.makeText(MainActivity.this, "실패: " + exception.getMessage(), Toast.LENGTH_SHORT).show();
                                        if (mRecognizer != null) {
                                            mRecognizer.destroy();
                                            mRecognizer.setRecognitionListener(listener);
                                            mRecognizer.startListening(i);
                                        } else {
                                            mRecognizer.startListening(i);
                                        }

                                    }
                                });
                    }
                }

                @Override
                public void onFail(Request request, IOException exception) {
                    Toast.makeText(MainActivity.this, "실패: " + exception.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }


        // 부분결과
        @Override
        public void onPartialResults(Bundle partialResults) {

            String key = "";
            key = SpeechRecognizer.RESULTS_RECOGNITION;
            ArrayList<String> mResult = partialResults.getStringArrayList(key);
            String[] rs = new String[mResult.size()];
            mResult.toArray(rs);
            Toast.makeText(MainActivity.this,"음성인식 부분 완료 "+rs[0],Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onEvent(int eventType, Bundle params) {

        }
    };
}
