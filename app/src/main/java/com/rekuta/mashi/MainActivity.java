package com.rekuta.mashi;

import android.app.*;
import android.content.*;
import android.media.*;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.*;
import android.widget.*;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.*;
import java.util.*;
import omrecorder.*;

public class MainActivity extends Activity {
    
    private int currentRec = 0;
    private boolean withBGM = false;
    private String filePath = "";
    private String audioEffect = "";
    private String saveAs = "";
    private String currentReclist = "";
    
    private ArrayList<HashMap<String, String>> sampleList = new ArrayList<>();
                
    private EditText edittext1;
    private TextView textview3;
    private TextView textview4;
    private TextView textview2;
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
    
    private Recorder recorder;
    private SharedPreferences settings;
    
    @Override
    protected void onCreate(Bundle _savedInstanceState) {
        super.onCreate(_savedInstanceState);
        setContentView(R.layout.main);
        initialize(_savedInstanceState);
        initializeLogic();
    }
    
    private void initialize(Bundle _savedInstanceState) {      
        edittext1 = findViewById(R.id.edittext1);
        textview3 = findViewById(R.id.textview3);
        textview4 = findViewById(R.id.textview4);
        textview2 = findViewById(R.id.textview2);
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
        settings = getSharedPreferences("settings", Activity.MODE_PRIVATE);
        
        set.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View _view) {
                if (TextUtils.isEmpty(edittext1.getText())) {
                    AppUtil.showMessage(getApplicationContext(), "Please enter voicebank folder.");
                    return;
                }
                
                currentReclist =  spinner1.getSelectedItem().toString();
                
                if (currentReclist.equals("Custom")) {
                    Intent pickFile = new Intent(Intent.ACTION_GET_CONTENT);
                    pickFile.setType("*/*");
                    startActivityForResult(pickFile, 0);
                } else {
                    _setupRecording();
                }
            }
        });
        
        listview1.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> _adapter, View _view, int _position, long _id) {
                _updateTextViews(_position);
                currentRec = _position;
            }
        });
        
        record.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View _view) {
                _setRecordSample(sampleList.get(currentRec).get(saveAs));
                if (withBGM) {
                    _startRecordingWithBGM();
                } else {
                    record.setVisibility(View.GONE);
                    stop.setVisibility(View.VISIBLE);
                    _startRecording();
                }
            }
        });
        
        stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View _view) {
                record.setVisibility(View.VISIBLE);
                stop.setVisibility(View.GONE);
                _stopRecording();
            }
        });
        
        play.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View _view) {
                if (!FileUtil.isExistFile(filePath.concat("/").concat(sampleList.get(currentRec).get(saveAs)).concat(".wav"))) {
                    AppUtil.showMessage(getApplicationContext(), "No recorded sample yet.");
                    return;
                }
                
                _enableAll(false);
                MediaPlayer playSample = MediaPlayer.create(getApplicationContext(), Uri.parse(filePath.concat("/").concat(sampleList.get(currentRec).get(saveAs)).concat(".wav")));
                playSample.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                    @Override
                    public void onCompletion(MediaPlayer mp) {
                        _enableAll(true);
                        AppUtil.showMessage(getApplicationContext(), "Done!");
                    }
                });
                playSample.start();
            }
        });
        
        bgm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View _view) {
                withBGM = bgm.isChecked();
            }
        });
        
        del.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View _view) {
                String delSampFilePath = filePath.concat("/").concat(sampleList.get(currentRec).get(saveAs)).concat(".wav");
                
                if (!FileUtil.isExistFile(delSampFilePath)) {
                    AppUtil.showMessage(getApplicationContext(), "No recorded sample yet.");
                    return;
                }
                
                AlertDialog.Builder delSampDlg = new AlertDialog.Builder(MainActivity.this);
                delSampDlg.setTitle(R.string.delsamp_title);
                delSampDlg.setMessage(R.string.delsamp_content);
                delSampDlg.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface _dialog, int _which) {
                        FileUtil.deleteFile(delSampFilePath);
                        ((BaseAdapter) listview1.getAdapter()).notifyDataSetChanged();
                        AppUtil.showMessage(getApplicationContext(), "Deleted");
                    }
                });
                delSampDlg.setNegativeButton("No", null);
                delSampDlg.create().show();
            }
        });
        
        prev.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View _view) {
                if (currentRec == 0) {
                    AppUtil.showMessage(getApplicationContext(), "Start");
                } else {
                    currentRec--;
                    _updateTextViews(currentRec);
                }
            }
        });
        
        next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View _view) {
                if (currentRec == (sampleList.size() - 1)) {
                    AppUtil.showMessage(getApplicationContext(), "End");
                } else {
                    currentRec++;
                    _updateTextViews(currentRec);
                }
            }
        });
    }
    
    private void initializeLogic() {
        String[] reclist = {"CV", "VCV", "Custom"};
        
        audioEffect = settings.getString("audioFx", "default");
        saveAs = settings.getString("saveAs", "romaji");
        
        spinner1.setAdapter(new ArrayAdapter<String>(getBaseContext(), android.R.layout.simple_spinner_dropdown_item, reclist));
        stop.setVisibility(View.GONE);
        _enableAll(false);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 0 && resultCode == RESULT_OK) {
            _setupRecording(data.getData());
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, 0, 0, "Settings");
        menu.add(0, 1, 0, "About");
        return super.onCreateOptionsMenu(menu);
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case (0):
                _showSettings();
                break;
            case (1):
                _showAbout();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    public void _setRecordSample(String _samp) {
        recorder = OmRecorder.wav(new PullTransport.Default(mic(audioEffect)), new File(filePath.concat("/").concat(_samp).concat(".wav"))); 
    }
    
    public void _updateTextViews(int _pos) {
        textview4.setText(sampleList.get(_pos).get("hiragana"));
        textview2.setText(sampleList.get(_pos).get("romaji"));
    }
    
    public void _showSettings() {
        AlertDialog.Builder settingsDialog = new AlertDialog.Builder(MainActivity.this);
        settingsDialog.setTitle("Settings");
        LayoutInflater inflater = getLayoutInflater();
        View convertView = inflater.inflate(R.layout.settings, null);
        
        final Switch switch1 = (Switch) convertView.findViewById(R.id.switch1);
        final Switch switch2 = (Switch) convertView.findViewById(R.id.switch2);
        final Switch switch3 = (Switch) convertView.findViewById(R.id.switch3);
        
        switch1.setChecked(Arrays.asList("noisesuppressor","both").contains(audioEffect));
        switch2.setChecked(Arrays.asList("gaincontrol","both").contains(audioEffect));
        switch3.setChecked(saveAs.equals("hiragana"));
        
        settingsDialog.setView(convertView);
        settingsDialog.setPositiveButton("Save", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface _dialog, int _which) {
                HashMap<String,String> fxMap = new HashMap<>();
                fxMap.put("true-true", "both");
                fxMap.put("true-false", "noisesuppressor");
                fxMap.put("false-true", "gaincontrol");
                fxMap.put("false-false", "default");
                
                audioEffect = fxMap.get(switch1.isChecked() + "-" + switch2.isChecked());
                settings.edit().putString("audioFx", audioEffect).commit();
                
                saveAs = switch3.isChecked() ? "hiragana" : "romaji";
                settings.edit().putString("saveAs", saveAs).commit();
                
                if (listview1.getAdapter() != null) ((BaseAdapter)listview1.getAdapter()).notifyDataSetChanged();
            }
        });
        settingsDialog.setNegativeButton("Cancel", null);
        settingsDialog.create().show();
    }
    
    public void _showAbout() {
        AlertDialog.Builder aboutDialog = new AlertDialog.Builder(MainActivity.this);
        aboutDialog.setTitle(R.string.about_title);
        aboutDialog.setMessage(R.string.about_content);
        aboutDialog.setPositiveButton("Okay", null);
        aboutDialog.create().show();
    }
    
    public void _enableAll(boolean _enable) {
        listview1.setVisibility(_enable ? View.VISIBLE : View.GONE);
        bgm.setEnabled(currentReclist.equals("CV") ? false : _enable);
        del.setEnabled(_enable);
        next.setEnabled(_enable);
        prev.setEnabled(_enable);
        play.setEnabled(_enable);
        record.setEnabled(_enable);
    }
    
    public void _setupRecording(Uri... _uri) {
        String folderName = currentReclist.equals("Custom")
                            ? edittext1.getText().toString()
                            : edittext1.getText().toString().concat(" ").concat(currentReclist);

        filePath = FileUtil.getExternalStorageDir().concat("/Rekuta/").concat(folderName);
        withBGM = false;
        currentRec = 0;

        FileUtil.createNewFile(filePath.concat("/.nomedia"));

        try {
            InputStream inputstream1;
            
            if (_uri.length > 0 && currentReclist.equals("Custom")) {
                inputstream1 = getContentResolver().openInputStream(_uri[0]);
                String[] listSamps = AppUtil.copyFromInputStream(inputstream1).split("\\s+");
                sampleList.clear();
                for (String str : listSamps) {
                    HashMap<String,String> hashMap = new HashMap<>();
                    hashMap.put("hiragana", str);
                    hashMap.put("romaji", str);
                    sampleList.add(hashMap);
                }
            } else {
                inputstream1 = getAssets().open(currentReclist.concat(".json"));
                sampleList = new Gson().fromJson(AppUtil.copyFromInputStream(inputstream1),new TypeToken<ArrayList<HashMap<String, String>>>() {}.getType());  
            }
            throw new java.io.IOException();
        } catch (java.io.IOException e) {
                e.printStackTrace();
        }

        textview3.setText("Voicebank: ".concat(folderName));
        listview1.setAdapter(new ReclistViewAdapter(sampleList));
        bgm.setChecked(withBGM);
        _updateTextViews(currentRec);
        _enableAll(true);
    }
    
    public void _startRecording() {
        recorder.startRecording();
        set.setEnabled(false);
        _enableAll(false);
        AppUtil.showMessage(getApplicationContext(), "Recording...");
    }
    
    public void _startRecordingWithBGM() {
        AppUtil.showMessage(getApplicationContext(), "Please Wait...");
        set.setEnabled(false);
        _enableAll(false);
        
        MediaPlayer playBGM = MediaPlayer.create(getApplicationContext(), R.raw.bgm);
        playBGM.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                _stopRecording();
            }
        });
        playBGM.start();
        
        Timer recordDelay = new Timer();
        recordDelay.schedule(new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        recorder.startRecording();
                        AppUtil.showMessage(getApplicationContext(), "Recording...");
                    }
                  });
            }
        }, 4000);
    }
    
    public void _stopRecording() {
        try {
            recorder.stopRecording();
        } catch(IOException e) {
            e.printStackTrace();
        }
        set.setEnabled(true);
        _enableAll(true);
        ((BaseAdapter)listview1.getAdapter()).notifyDataSetChanged();
        AppUtil.showMessage(getApplicationContext(), "Done!");
    }
    
    private PullableSource mic(String strSet) {
        PullableSource.Default defaultSource = new PullableSource.Default(
            new AudioRecordConfig.Default(
                MediaRecorder.AudioSource.UNPROCESSED, AudioFormat.ENCODING_PCM_16BIT,
                AudioFormat.CHANNEL_IN_MONO, 44100));
        
        switch (strSet) {
            case "noisesuppressor":
                return new PullableSource.NoiseSuppressor(defaultSource);
            case "gaincontrol":
                return new PullableSource.AutomaticGainControl(defaultSource);
            case "both":
                return new PullableSource.AutomaticGainControl(
                    new PullableSource.NoiseSuppressor(defaultSource));
            default:
                return defaultSource;
        }
    }
    
    public class ReclistViewAdapter extends BaseAdapter {
        
        ArrayList<HashMap<String, String>> _data;
        
        public ReclistViewAdapter(ArrayList<HashMap<String, String>> _arr) {
            _data = _arr;
        }
        
        @Override
        public int getCount() {
            return _data.size();
        }
        
        @Override
        public HashMap<String, String> getItem(int _index) {
            return _data.get(_index);
        }
        
        @Override
        public long getItemId(int _index) {
            return _index;
        }
        
        @Override
        public View getView(int _position, View _v, ViewGroup _container) {
            LayoutInflater _inflater = getLayoutInflater();
            View _view = _v;
            if (_view == null) {
                _view = _inflater.inflate(R.layout.listsamples, null);
            }
                
            ImageView imageview1 = _view.findViewById(R.id.imageview1);
            TextView textview1 = _view.findViewById(R.id.textview1);
            TextView textview2 = _view.findViewById(R.id.textview2);
            
            textview1.setText(_data.get(_position).get("hiragana"));
            textview2.setText(_data.get(_position).get("romaji"));
            if (FileUtil.isExistFile(filePath.concat("/").concat(_data.get(_position).get(saveAs)).concat(".wav"))) {
                imageview1.setImageResource(R.drawable.ic_check_box_black);
            } else {
                imageview1.setImageResource(R.drawable.ic_check_box_outline_blank_black);
            }
            
            return _view;
        }
    }
}
