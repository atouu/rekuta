package com.rekuta.mashi;

import android.app.*;
import android.content.*;
import android.graphics.Rect;
import android.media.*;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import java.io.*;
import java.util.*;

public class MainActivity extends Activity {
    
    private int currentRecPos = 0;
    private int recordDelay = 0;
    private boolean withBGM = false;
    private boolean customBgm = false;
    private boolean useKana = false;
    private String customBgmFile = "";
    private String currentRecFile = "";
    private String voicebankDir = "";
    private String saveAs = "";
    private String currentReclist = "";
    private String rekutaDir = "";
    
    private ArrayList<HashMap<String, String>> sampleList;
              
    private EditText edittext1;
    private TextView textview3;
    private TextView textview4;
    private TextView textview2;
    private TextView textview5;
    private Spinner spinner1; 
    private ListView listview1;
    private ToggleButton bgm;
    private Button set;
    private Button record;
    private Button stop;
    private Button play;
    private Button del;
    private Button prev;
    private Button next;
    
    private WavRecorder recorder;
    private MediaPlayer playBGM;
    private SharedPreferences settings;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppUtil.checkThemePreference(this);
        setContentView(R.layout.main);
        initialize(savedInstanceState);
        initializeLogic();
    }
    
    private void initialize(Bundle savedInstanceState) {      
        edittext1 = findViewById(R.id.edittext1);
        textview3 = findViewById(R.id.textview3);
        textview4 = findViewById(R.id.textview4);
        textview2 = findViewById(R.id.textview2);
        textview5 = findViewById(R.id.textview5);
        spinner1 = findViewById(R.id.spinner1); 
        listview1 = findViewById(R.id.listview1);
        bgm = findViewById(R.id.bgm);
        set = findViewById(R.id.set);
        record = findViewById(R.id.record);
        stop = findViewById(R.id.stop);
        play = findViewById(R.id.play);
        del = findViewById(R.id.del);
        prev = findViewById(R.id.prev);
        next = findViewById(R.id.next); 
        settings = getSharedPreferences(getPackageName().concat("_preferences"), Activity.MODE_PRIVATE);
        
        listview1.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapter, View view, int position, long id) {
                updateStrings(position);
                currentRecPos = position;
            }
        });
        
        set.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (TextUtils.isEmpty(edittext1.getText())) {
                    AppUtil.showMessage(getApplicationContext(), R.string.please_enter_vb_folder);
                    return;
                }
                
                currentReclist =  spinner1.getSelectedItem().toString();
                
                if (currentReclist.equals("Custom")) {
                    Intent pickFile = new Intent(Intent.ACTION_GET_CONTENT);
                    pickFile.setType("text/plain");
                    startActivityForResult(pickFile, 0);
                } else {
                    setupRecording(null);
                }
            }
        });
        
        record.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                recorder = new WavRecorder(currentRecFile);
                if (withBGM) {
                    startRecordingWithBGM();
                } else {
                    record.setVisibility(View.GONE);
                    stop.setVisibility(View.VISIBLE);
                    startRecording();
                }
            }
        });
        
        stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                record.setVisibility(View.VISIBLE);
                stop.setVisibility(View.GONE);
                stopRecording();
            }
        });
        
        play.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!FileUtil.isExistFile(currentRecFile)) {
                    AppUtil.showMessage(getApplicationContext(), R.string.no_recorded_sample_yet);
                    return;
                }
                
                enableAll(false);
                MediaPlayer playSample = MediaPlayer.create(getApplicationContext(), Uri.parse(currentRecFile));
                playSample.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                    @Override
                    public void onCompletion(MediaPlayer mp) {
                        mp.release();
                        enableAll(true);
                        AppUtil.showMessage(getApplicationContext(), R.string.done);
                    }
                });
                playSample.start();
            }
        });

        bgm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                withBGM = bgm.isChecked();
            }
        });
        
        del.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!FileUtil.isExistFile(currentRecFile)) {
                    AppUtil.showMessage(getApplicationContext(), R.string.no_recorded_sample_yet);
                    return;
                }
                
                AlertDialog.Builder delSampDlg = new AlertDialog.Builder(MainActivity.this);
                delSampDlg.setTitle(R.string.delsamp_title);
                delSampDlg.setMessage(R.string.delsamp_content);
                delSampDlg.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        FileUtil.deleteFile(currentRecFile);
                        ((BaseAdapter) listview1.getAdapter()).notifyDataSetChanged();
                        AppUtil.showMessage(getApplicationContext(), R.string.deleted);
                        totalRecords();
                    }
                });
                delSampDlg.setNegativeButton(R.string.no, null);
                delSampDlg.create().show();
            }
        });
        
        prev.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (currentRecPos == 0) {
                    AppUtil.showMessage(getApplicationContext(), R.string.start);
                } else {
                    currentRecPos--;
                    updateStrings(currentRecPos);
                }
            }
        });
        
        next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (currentRecPos == (sampleList.size() - 1)) {
                    AppUtil.showMessage(getApplicationContext(), R.string.end);
                } else {
                    currentRecPos++;
                    updateStrings(currentRecPos);
                }
            }
        });
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        useKana = settings.getBoolean("useKana", false);
        saveAs = (useKana | currentReclist.equals("Custom")) ? "hiragana" : "romaji";
        customBgm = settings.getBoolean("customBGM", false);
        customBgmFile = settings.getString("customBGMFile", "None");
        recordDelay = Integer.parseInt(settings.getString("recordDelay", "4000"));
    }
    
    private void initializeLogic() {
        String[] reclist = {"CV", "VCV", "Custom"};
        
        rekutaDir = FileUtil.getExternalStorageDir().concat("/Rekuta");
        
        FileUtil.createNewFile(rekutaDir.concat("/.nomedia"));;
        
        spinner1.setAdapter(new ArrayAdapter<String>(getBaseContext(), R.layout.spinner_item, reclist));
        stop.setVisibility(View.GONE);
        enableAll(false);
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK) return;
        
        switch (requestCode) {
            case 0:
                setupRecording(data.getData());
                break;
            case 1:
                if (data.getBooleanExtra("toggleTheme", false)) {
                    recreate();
                }
            break;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, 0, 0, R.string.settings);
        menu.add(0, 1, 0, R.string.about);
        return super.onCreateOptionsMenu(menu);
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case (0):
                Intent i = new Intent(this, SettingsActivity.class);
                startActivityForResult(i, 1);
                break;
            case (1):
                showAbout();
                break;
        }
        return super.onOptionsItemSelected(item);
    }
    
    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            View v = getCurrentFocus();
            if (edittext1.isFocused()) {
                Rect outRect = new Rect();
                edittext1.getGlobalVisibleRect(outRect);
                if (!outRect.contains((int)event.getRawX(), (int)event.getRawY())) {
                    InputMethodManager imm = (InputMethodManager) v.getContext().getSystemService(Context.INPUT_METHOD_SERVICE); 
                    imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                    edittext1.clearFocus();
                }
            }
        }
        return super.dispatchTouchEvent(event);
    }
    
    public void updateStrings(int pos) {
        currentRecFile = String.format("%s/%s.wav", voicebankDir, sampleList.get(pos).get(saveAs));
        textview4.setText(sampleList.get(pos).get("hiragana"));
        if (currentReclist.equals("Custom")) {
            textview2.setVisibility(View.GONE);
        } else {
            textview2.setText(sampleList.get(pos).get("romaji"));
        }
    }
    
    public void showAbout() {
        AlertDialog.Builder aboutDialog = new AlertDialog.Builder(MainActivity.this);
        aboutDialog.setTitle(R.string.about_title);
        aboutDialog.setMessage(R.string.about_content);
        aboutDialog.setPositiveButton(R.string.okay, null);
        aboutDialog.create().show();
    }
    
    public void enableAll(boolean enable) {
        listview1.setVisibility(enable ? View.VISIBLE : View.GONE);
        textview5.setVisibility(enable ? View.VISIBLE : View.GONE);
        bgm.setEnabled(enable);
        del.setEnabled(enable);
        next.setEnabled(enable);
        prev.setEnabled(enable);
        play.setEnabled(enable);
        record.setEnabled(enable);
    }
    
    public void setupRecording(Uri uri) {
        String folderName = currentReclist.equals("Custom")
            ? edittext1.getText().toString()
            : edittext1.getText().toString().concat(" ").concat(currentReclist);

        voicebankDir = rekutaDir.concat("/").concat(folderName);
        withBGM = false;
        currentRecPos = 0;
        
        FileUtil.makeDir(voicebankDir);

        try {
            InputStream inputstream1;
            
            if (uri != null && currentReclist.equals("Custom")) {
                saveAs = "hiragana";
                inputstream1 = getContentResolver().openInputStream(uri);
                String[] listSamps = AppUtil.copyFromInputStream(inputstream1, "Shift_JIS").split("\\s+");
                sampleList = new ArrayList<>();
                for (String str : listSamps) {
                    HashMap<String,String> hashMap = new HashMap<>();
                    hashMap.put("hiragana", str);
                    sampleList.add(hashMap);
                }
            } else {
                saveAs = useKana ? "hiragana" : "romaji";
                inputstream1 = getAssets().open(currentReclist.concat(".json"));
                sampleList = AppUtil.getFromJSONArray(AppUtil.copyFromInputStream(inputstream1, "UTF-8"));
            }
            totalRecords();
            throw new IOException();
        } catch (IOException e) {
            e.printStackTrace();
        }

        textview3.setText(folderName);
        listview1.setAdapter(new ReclistViewAdapter(sampleList));
        bgm.setChecked(withBGM);
        updateStrings(currentRecPos);
        enableAll(true);
    }
    
    public void startRecording() {
        AppUtil.showMessage(getApplicationContext(), R.string.recording);
        recorder.startRecording();
        set.setEnabled(false);
        enableAll(false);
    }
    
    public void startRecordingWithBGM() {
        if (customBgm) {
            try {
            	playBGM = new MediaPlayer();
                playBGM.setDataSource(getApplicationContext(), Uri.parse(customBgmFile));
                playBGM.prepare();
            } catch(IOException e) {
                AppUtil.showMessage(getApplicationContext(), R.string.custom_bgm_not_found);
                return;
            }
        } else {
            playBGM = MediaPlayer.create(getApplicationContext(), R.raw.bgm);
        }
        
        playBGM.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                stopRecording();
                mp.release();
            }
        });
        playBGM.start();
        
        AppUtil.showMessage(getApplicationContext(), R.string.please_wait);
        set.setEnabled(false);
        enableAll(false);
        
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                recorder.startRecording();
                AppUtil.showMessage(getApplicationContext(), R.string.recording);
            }
        }, recordDelay);
    }
    
    public void stopRecording() {
        ((BaseAdapter)listview1.getAdapter()).notifyDataSetChanged();
        set.setEnabled(true);
        recorder.stopRecording();
        enableAll(true);
        totalRecords();
        AppUtil.showMessage(getApplicationContext(), R.string.done);
    }
    
    public void totalRecords() {
        int total = 0;
        ArrayList<String> fileNames = FileUtil.listFileNames(voicebankDir);
        for (HashMap<String, String> element : sampleList) {
            if (fileNames.contains(element.get(saveAs).concat(".wav"))) {
                total++;
            }
        }
        textview5.setText(String.format("%d/%d", total, sampleList.size()));
    }
    
    public class ReclistViewAdapter extends BaseAdapter {
        
        ArrayList<HashMap<String, String>> data;
        
        public ReclistViewAdapter(ArrayList<HashMap<String, String>> arr) {
            data = arr;
        }
        
        @Override
        public int getCount() {
            return data.size();
        }
        
        @Override
        public HashMap<String, String> getItem(int index) {
            return data.get(index);
        }
        
        @Override
        public long getItemId(int index) {
            return index;
        }
        
        @Override
        public View getView(int position, View v, ViewGroup container) {
            LayoutInflater inflater = getLayoutInflater();
            View view = v;
            if (view == null) {
                view = inflater.inflate(R.layout.sample_list_item, null);
            }
                
            ImageView imageview1 = view.findViewById(R.id.imageview1);
            TextView textview1 = view.findViewById(R.id.textview1);
            TextView textview2 = view.findViewById(R.id.textview2);
            
            textview1.setText(data.get(position).get("hiragana"));
            if (data.get(position).containsKey("romaji")) {
                textview2.setText(data.get(position).get("romaji"));
            } else {
                textview2.setVisibility(View.GONE);
            }
            
            if (FileUtil.isExistFile(String.format("%s/%s.wav", voicebankDir, data.get(position).get(saveAs)))) {
                imageview1.setVisibility(View.VISIBLE);
            } else {
                imageview1.setVisibility(View.INVISIBLE);
            }
            
            return view;
        }
    }
}
