package com.example.brandycamera.demo;


import android.Manifest;
import android.graphics.Canvas;
import android.graphics.Color;
import android.hardware.Camera;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.TextureView;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.tal.brandysdk.imagekit.TALImageAbilityType;
import com.tal.brandysdk.imagekit.TALImageInferEngine;
import com.tal.brandysdk.imagekit.dataprocess.YuvUtils;
import com.tal.brandysdk.imagekit.internal_.IImageDebugCallback;
import com.tal.brandysdk.imagekit.log.LogUtil;


public class MainActivity extends AppCompatActivity
{
    private static final String TAG = "tal-MainActivity";

    private TextureView textureView;
    private CheckBox checkBoxFace, checkBoxHandup;
    private TextView textFaceCheckin, textFaceAppear, textFaceDisappearLong, textFaceNum;
    private TextView textHandup;

    private FrameLayout frameLayout;
    private MyDrawView draw;

    private MyCameraManager cameraManager;
    private TALImageInferEngine engine;
    private YuvUtils yuvtool;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ActivityCompat.requestPermissions(MainActivity.this,
                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.CAMERA,
                        Manifest.permission.RECORD_AUDIO}, 1);

        findViewWidgets();
        assignEngine();

        /// camera
        yuvtool = new YuvUtils();
        int camCnt = Camera.getNumberOfCameras();
        int camId = (camCnt > 0 ? camCnt - 1 : 0);  // prefer front camera
        cameraManager = new MyCameraManager(this, textureView, camId);
        cameraManager.setPreferedPreviewSize(640, 480);
        cameraManager.setPreviewFrameCallback(new MyCameraManager.IOnPreviewFrameCallback()
        {
            @Override
            public void onPreviewFrame(byte[] data, Camera camera)
            {
                // imgProcessor.process(data, camera);
                Camera.Size size = camera.getParameters().getPreviewSize();
                byte[] rawdata = yuvtool.nv21MirrorLeftRight(data, size.width, size.height);
                engine.detect(rawdata, size.width, size.height);
            }
        });
        textureView.setSurfaceTextureListener(cameraManager);


        ///ui界面交互
        checkBoxFace.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
        {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
            {
                if (isChecked)
                {
                    engine.getConfig().setDetFaceEnable(true)
                            .turnOnAbility(TALImageAbilityType.ImageAbilityStudentAppear,
                                    TALImageAbilityType.ImageAbilityStudentDisappearLongtime,
                                    TALImageAbilityType.ImageAbilityStudentSignIn,
                                    TALImageAbilityType.ImageAbilityFaceNumberChange);

                    showFaceResult(true);
                }
                else
                {
                    engine.getConfig().setDetFaceEnable(false);
                    showFaceResult(false);
                }
            }
        });

        checkBoxHandup.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
        {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
            {
                if (isChecked)
                {
                    engine.getConfig()
                            .setClsHandupEnable(true)
                            .turnOnAbility(TALImageAbilityType.ImageAbilityStudentHandUp)
                            .setHandupDetArea(400, 150, 200, 200);

                    showHandResult(true);
                    draw.putStable("handup_rect", new MyDrawView.IUserDrawAction()
                    {
                        @Override
                        public void drawOn(Canvas canvas)
                        {
                            float r = 1.78f; //screen显示的宽高/视频分辨率
                            canvas.drawRect(400 * r, 150 * r, (400 + 200) * r, (150 + 200) * r, new MyDrawView.BrushRect().setStrokeWidth(2).setColor(Color.CYAN).paint);
                        }
                    });
                    draw.refreshView();
                }
                else
                {
                    engine.getConfig().setClsHandupEnable(false);
                    showHandResult(false);
                    draw.removeStable("handup_rect");
                    draw.refreshView();
                }
            }
        });

    }

    private void showFaceResult(boolean visible)
    {
        int v = (visible ? View.VISIBLE : View.INVISIBLE);
        textFaceCheckin.setVisibility(v);
        textFaceAppear.setVisibility(v);
        textFaceDisappearLong.setVisibility(v);
        textFaceNum.setVisibility(v);
    }

    private void showHandResult(boolean visible)
    {
        int v = (visible ? View.VISIBLE : View.INVISIBLE);
        textHandup.setVisibility(v);
    }

    private void findViewWidgets()
    {
        textureView = findViewById(R.id.camera_preview);
        frameLayout = findViewById(R.id.frame_draw);
        draw = new MyDrawView(getApplicationContext());
        frameLayout.removeAllViews();
        frameLayout.addView(draw);

        checkBoxFace = findViewById(R.id.chbox_face);
        textFaceCheckin = findViewById(R.id.face_result_checkin);
        textFaceAppear = findViewById(R.id.face_result_appear);
        textFaceDisappearLong = findViewById(R.id.face_result_disappear_longtime);
        textFaceNum = findViewById(R.id.face_result_facenumber);

        checkBoxHandup = findViewById(R.id.chbox_handup);
        textHandup = findViewById(R.id.handup_result);


    }

    private void assignEngine()
    {
        engine = new TALImageInferEngine(getApplicationContext(), new TALImageInferEngine.IImageInferenceCallback()
        {
            @Override
            public void faceAppear(final boolean appear)
            {
                Log.d("engine_callback", "appear=" + appear);
                runOnUiThread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        textFaceAppear.setText(appear ? "Y" : "N");
                        if (appear)
                        {
                            textFaceDisappearLong.setText("");
                        }
                    }
                });
            }

            @Override
            public void faceDisappearLongtime()
            {
                Log.d("engine_callback", "disappear");
                runOnUiThread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        textFaceDisappearLong.setText("Y");
                    }
                });
            }

            @Override
            public void signIn(final boolean signin)
            {
                Log.d("engine_callback", "signin=" + signin);
                runOnUiThread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        textFaceCheckin.setText(signin ? "Y" : "N");
                    }
                });
            }

            @Override
            public void faceNumberChange(final int number)
            {
                Log.d("engine_callback", "faceNumber=" + number);
                runOnUiThread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        textFaceNum.setText(String.valueOf(number));
                    }
                });
            }

            @Override
            public void handUp(final boolean handup)
            {
                Log.d("engine_callback", "handup=" + handup);
                runOnUiThread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        textHandup.setText(handup ? "Y" : "N");
                    }
                });
            }

        }/*, "/sdcard/tal/"*/);


        final String facePosResultPrefix = "face_result_";
        final String timeConsumingPrefix = "time_of";
        engine.dbgCb_ = new IImageDebugCallback()
        {
            @Override
            public void facePositions(final float[][] positions)
            {
                if (draw.hasInstant(facePosResultPrefix + "\\d+") != null)
                {
                    draw.removeInstant(facePosResultPrefix + "\\d+");
                    if (positions == null)
                    {
                        draw.refreshView();
                    }
                }

                if (positions != null)
                {
                    int n = positions.length;
                    for (int i = 0; i < n; i++)
                    {
                        final int idx = i;
                        draw.putInstant(facePosResultPrefix + String.valueOf(i), new MyDrawView.IUserDrawAction()
                        {
                            @Override
                            public void drawOn(Canvas canvas)
                            {
                                final int[] data_screen_sizes = cameraManager.getPreviewAndScreenSize();
                                final float r = ((float) (data_screen_sizes[2])) / data_screen_sizes[0];
                                // final int screenW = data_screen_sizes[2], screenH = data_screen_sizes[3];
                                // canvas.drawRect(positions[idx][1] * screenW, positions[idx][2] * screenH, positions[idx][3] * screenW, positions[idx][4] * screenH,
                                //         new MyDrawView.BrushRect().setColor(Color.RED).setStrokeWidth(2).paint);
                                canvas.drawRect(positions[idx][1] * r, positions[idx][2] * r, positions[idx][3] * r, positions[idx][4] * r,
                                        new MyDrawView.BrushRect().setColor(Color.RED).setStrokeWidth(5).paint);
                            }
                        });
                    }
                    draw.refreshView();
                }

            }

            @Override
            public void timeConsuming(final String id, final long millis)
            {
                String keyStr = timeConsumingPrefix + id;
                if (checkBoxFace.isChecked() || checkBoxHandup.isChecked())
                {
                    draw.putInstant(keyStr, new MyDrawView.IUserDrawAction()
                    {
                        @Override
                        public void drawOn(Canvas canvas)
                        {
                            if ("face".equals(id))
                            {
                                canvas.drawText("time face:" + String.valueOf(millis) + "ms", 10, 50, new MyDrawView.BrushText().setTextSize(40).paint);
                                draw.refreshView();
                            }
                            else if ("handup".equals(id))
                            {
                                canvas.drawText("time handup:" + String.valueOf(millis) + "ms", 10, 100, new MyDrawView.BrushText().setTextSize(40).paint);
                                draw.refreshView();
                            }
                        }
                    });
                    // draw.refreshView();  //NOTE: maybe delete in future
                }

            }
        };
    }


}
