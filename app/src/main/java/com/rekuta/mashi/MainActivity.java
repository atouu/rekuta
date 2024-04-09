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
import com.rekuta.mashi.databinding.ActivityMainBinding;
import com.rekuta.mashi.databinding.SampleListItemBinding;
import java.io.*;
import java.util.*;

public class MainActivity extends Activity {
    
    private ActivityMainBinding binding;
    
    private int currentRecPos = 0;
    private int recordStart = 0;
    private int recordEnd = 0;
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
    
    private WavRecorder recorder;
    private MediaPlayer playBGM;
    private SharedPreferences settings;
    private BaseAdapter sampleListAdapater;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Utils.checkThemePreference(this);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        initialize(savedInstanceState);
        initializeLogic();
    }
    
    private void initialize(Bundle savedInstanceState) {      
        settings = Utils.getDefaultSharedPreferences(this);
        
        binding.listview1.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapter, View view, int position, long id) {
                updateStrings(position);
                currentRecPos = position;
            }
        });
        
        binding.set.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (TextUtils.isEmpty(binding.edittext1.getText())) {
                    Utils.showMessage(getApplicationContext(), R.string.please_enter_vb_folder);
                    return;
                }
                
                currentReclist =  binding.spinner1.getSelectedItem().toString();
                
                if (currentReclist.equals("Custom")) {
                    Intent pickFile = new Intent(Intent.ACTION_GET_CONTENT);
                    pickFile.setType("text/plain");
                    startActivityForResult(pickFile, 0);
                } else {
                    setupRecording(null);
                }
            }
        });
        
        binding.record.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                recorder = new WavRecorder(currentRecFile);
                if (withBGM) {
                    startRecordingWithBGM();
                } else {
                    binding.record.setVisibility(View.GONE);
                    binding.stop.setVisibility(View.VISIBLE);
                    startRecording();
                }
            }
        });
        
        binding.stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                binding.record.setVisibility(View.VISIBLE);
                binding.stop.setVisibility(View.GONE);
                stopRecording();
            }
        });
        
        binding.play.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!FileUtil.isExistFile(currentRecFile)) {
                    Utils.showMessage(getApplicationContext(), R.string.no_recorded_sample_yet);
                    return;
                }
                
                enableAll(false);
                MediaPlayer playSample = MediaPlayer.create(getApplicationContext(), Uri.parse(currentRecFile));
                playSample.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                    @Override
                    public void onCompletion(MediaPlayer mp) {
                        mp.release();
                        enableAll(true);
                        Utils.showMessage(getApplicationContext(), R.string.done);
                    }
                });
                playSample.start();
            }
        });

        binding.bgm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                withBGM = binding.bgm.isChecked();
            }
        });
        
        binding.del.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!FileUtil.isExistFile(currentRecFile)) {
                    Utils.showMessage(getApplicationContext(), R.string.no_recorded_sample_yet);
                    return;
                }
                
                AlertDialog.Builder delSampDlg = new AlertDialog.Builder(MainActivity.this);
                delSampDlg.setTitle(R.string.delsamp_title);
                delSampDlg.setMessage(R.string.delsamp_content);
                delSampDlg.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        FileUtil.deleteFile(currentRecFile);
                        sampleListAdapater.notifyDataSetChanged();
                        Utils.showMessage(getApplicationContext(), R.string.deleted);
                        totalRecords();
                    }
                });
                delSampDlg.setNegativeButton(R.string.no, null);
                delSampDlg.create().show();
            }
        });
        
        binding.prev.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (currentRecPos == 0) {
                    Utils.showMessage(getApplicationContext(), R.string.start);
                } else {
                    currentRecPos--;
                    updateStrings(currentRecPos);
                }
            }
        });
        
        binding.next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (currentRecPos == (sampleList.size() - 1)) {
                    Utils.showMessage(getApplicationContext(), R.string.end);
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
        saveAs = (useKana || currentReclist.equals("Custom")) ? "hiragana" : "romaji";
        customBgm = settings.getBoolean("customBGM", false);
        customBgmFile = settings.getString("customBGMFile", "None");
        recordStart = Integer.parseInt(settings.getString("recordStart", "4200"));
        recordEnd = Integer.parseInt(settings.getString("recordEnd", "9600"));
    }
    
    private void initializeLogic() {
        String[] reclist = {"CV", "VCV", "Custom"};
        
        rekutaDir = FileUtil.getExternalStorageDir().concat("/Rekuta");
        
        FileUtil.createNewFile(rekutaDir.concat("/.nomedia"));
        
        binding.spinner1.setAdapter(new ArrayAdapter<String>(getBaseContext(), R.layout.spinner_item, reclist));
        binding.stop.setVisibility(View.GONE);
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
            if (binding.edittext1.isFocused()) {
                Rect outRect = new Rect();
                binding.edittext1.getGlobalVisibleRect(outRect);
                if (!outRect.contains((int)event.getRawX(), (int)event.getRawY())) {
                    InputMethodManager imm = (InputMethodManager) v.getContext().getSystemService(Context.INPUT_METHOD_SERVICE); 
                    imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                    binding.edittext1.clearFocus();
                }
            }
        }
        return super.dispatchTouchEvent(event);
    }
    
    public void updateStrings(int pos) {
        currentRecFile = String.format("%s/%s.wav", voicebankDir, sampleList.get(pos).get(saveAs));
        binding.textview4.setText(sampleList.get(pos).get("hiragana"));
        if (currentReclist.equals("Custom")) {
            binding.textview2.setVisibility(View.GONE);
        } else {
            binding.textview2.setText(sampleList.get(pos).get("romaji"));
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
        binding.listview1.setVisibility(enable ? View.VISIBLE : View.GONE);
        binding.textview5.setVisibility(enable ? View.VISIBLE : View.GONE);
        binding.bgm.setEnabled(enable);
        binding.del.setEnabled(enable);
        binding.next.setEnabled(enable);
        binding.prev.setEnabled(enable);
        binding.play.setEnabled(enable);
        binding.record.setEnabled(enable);
    }
    
    public void setupRecording(Uri uri) {
        String folderName = currentReclist.equals("Custom")
            ? binding.edittext1.getText().toString()
            : String.format("%s %s", binding.edittext1.getText(), currentReclist);

        voicebankDir = rekutaDir.concat("/").concat(folderName);
        withBGM = false;
        currentRecPos = 0;
        
        FileUtil.makeDir(voicebankDir);

        try {
            InputStream inputstream1;
            
            if (uri != null && currentReclist.equals("Custom")) {
                saveAs = "hiragana";
                inputstream1 = getContentResolver().openInputStream(uri);
                String[] listSamps = Utils.copyFromInputStream(inputstream1, "Shift_JIS").split("\\s+");
                sampleList = new ArrayList<>();
                for (String str : listSamps) {
                    HashMap<String,String> hashMap = new HashMap<>();
                    hashMap.put("hiragana", str);
                    sampleList.add(hashMap);
                }
            } else {
                saveAs = useKana ? "hiragana" : "romaji";
                inputstream1 = getAssets().open(currentReclist.concat(".json"));
                sampleList = Utils.getFromJSONArray(Utils.copyFromInputStream(inputstream1, "UTF-8"));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        binding.textview3.setText(folderName);
        sampleListAdapater = new ReclistViewAdapter(sampleList);
        binding.listview1.setAdapter(sampleListAdapater);
        binding.bgm.setChecked(withBGM);
        updateStrings(currentRecPos);
        enableAll(true);
        totalRecords();
    }
    
    public void startRecording() {
        Utils.showMessage(getApplicationContext(), R.string.recording);
        recorder.startRecording();
        binding.set.setEnabled(false);
        enableAll(false);
    }
    
    public void startRecordingWithBGM() {
        if (customBgm) {
            try {
            	playBGM = new MediaPlayer();
                playBGM.setDataSource(getApplicationContext(), Uri.parse(customBgmFile));
                playBGM.prepare();
            } catch(IOException e) {
                Utils.showMessage(getApplicationContext(), R.string.custom_bgm_not_found);
                return;
            }
        } else {
            playBGM = MediaPlayer.create(getApplicationContext(), R.raw.bgm);
        }
        
        playBGM.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                sampleListAdapater.notifyDataSetChanged();
                Utils.showMessage(getApplicationContext(), R.string.done);
                binding.set.setEnabled(true);
                enableAll(true);
                totalRecords();
                mp.release();
            }
        });
        
        playBGM.start();
        
        Utils.showMessage(getApplicationContext(), R.string.please_wait);
        binding.set.setEnabled(false);
        enableAll(false);
        
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                recorder.startRecording();
                Utils.showMessage(getApplicationContext(), R.string.recording);
                
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        recorder.stopRecording();
                    }
                }, recordEnd - recordStart);
            }
        }, recordStart);
    }
    
    public void stopRecording() {
        sampleListAdapater.notifyDataSetChanged();
        binding.set.setEnabled(true);
        recorder.stopRecording();
        enableAll(true);
        totalRecords();
        Utils.showMessage(getApplicationContext(), R.string.done);
    }
    
    public void totalRecords() {
        int total = 0;
        ArrayList<String> fileNames = FileUtil.listFileNames(voicebankDir);
        for (HashMap<String, String> element : sampleList) {
            if (fileNames.contains(element.get(saveAs).concat(".wav"))) {
                total++;
            }
        }
        binding.textview5.setText(String.format("%d/%d", total, sampleList.size()));
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
        public View getView(int position, View convertView, ViewGroup container) {
            SampleListItemBinding listBinding;
            
            if (convertView == null) {
                listBinding = SampleListItemBinding.inflate(getLayoutInflater(), container, false);
                convertView = listBinding.getRoot();
                convertView.setTag(R.id.viewBinding, listBinding);
            } else {
                listBinding = ((SampleListItemBinding) convertView.getTag(R.id.viewBinding));
            }
            
            listBinding.textview1.setText(data.get(position).get("hiragana"));
            if (data.get(position).containsKey("romaji")) {
                listBinding.textview2.setText(data.get(position).get("romaji"));
            } else {
                listBinding.textview2.setVisibility(View.GONE);
            }
            
            if (FileUtil.isExistFile(String.format("%s/%s.wav", voicebankDir, data.get(position).get(saveAs)))) {
                listBinding.imageview1.setVisibility(View.VISIBLE);
            } else {
                listBinding.imageview1.setVisibility(View.INVISIBLE);
            }
            
            return convertView;
        }
    }
}
