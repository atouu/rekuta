package com.rekuta.mashi.activities;

import static com.rekuta.mashi.utilities.Constants.HIRAGANA;
import static com.rekuta.mashi.utilities.Constants.ROMAJI;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.text.InputFilter;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ArrayAdapter;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import com.rekuta.mashi.R;
import com.rekuta.mashi.adapter.ReclistViewAdapter;
import com.rekuta.mashi.databinding.ActivityMainBinding;
import com.rekuta.mashi.libraries.WavRecorder;
import com.rekuta.mashi.utilities.FileUtil;
import com.rekuta.mashi.utilities.Utils;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private int currentRecPos = 0;
    private int recordStart = 0;
    private int recordEnd = 0;
    private int saveAs = 0;
    private boolean initialized = false;
    private boolean withBGM = false;
    private boolean useKana = false;
    private String backgroundMusic = "";
    private String currentRecFile = "";
    private String currentPeakFile = "";
    private String voicebankDir = "";
    private String currentReclist = "";
    private String rekutaDir = "";

    private ArrayList<String[]> sampleList;

    private WavRecorder recorder;
    private SharedPreferences settings;
    private ReclistViewAdapter sampleListAdapter;
    private ActivityMainBinding binding;
    private MediaPlayer playBGM;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        initialize();
        initializeLogic();
    }

    private void initialize() {
        settings = PreferenceManager.getDefaultSharedPreferences(this);

        binding.listview1.setOnItemClickListener((adapter, view, position, id) -> {
            updateStrings(position);
            currentRecPos = position;
        });

        binding.edittext1.setFilters(new InputFilter[]{ (charSeq, i, i1, spanned, i2, i3) -> {
            if (charSeq != null && "?:\\\"*|/\\\\<>".contains(charSeq.toString())) {
                return "";
            }
            return null;
        }});

        binding.set.setOnClickListener(view -> {
            if (TextUtils.isEmpty(binding.edittext1.getText())) {
                Utils.showMessage(this, R.string.please_enter_voicebank_folder);
                return;
            }

            currentReclist = binding.spinner1.getSelectedItem().toString();

            if (currentReclist.equals("Custom")) {
                Utils.filePicker(this, getString(R.string.select_a_reclist), new String[]{"txt"}, files -> setupRecording(files[0]));
            } else {
                setupRecording(null);
            }
        });

        binding.record.setOnClickListener(view -> {
            recorder = new WavRecorder(currentRecFile);
            recorder.setOnAmplitudeChange(amplitude -> runOnUiThread(() -> binding.waveform.addAmplitude(amplitude)));

            binding.waveform.resetAmplitudes();

            if (withBGM) {
                startRecordingWithBGM();
            } else {
                startRecording();
            }
        });

        binding.stop.setOnClickListener(view -> stopRecording());

        binding.play.setOnClickListener(view -> {
            if (!FileUtil.isExistFile(currentRecFile)) {
                Utils.showMessage(this, R.string.no_recorded_sample_yet);
                return;
            }

            enableAll(false);
            MediaPlayer playSample = MediaPlayer.create(this, Uri.parse(currentRecFile));
            playSample.setOnCompletionListener(mp -> {
                playSample.release();
                enableAll(true);
                Utils.showMessage(this, R.string.done);
            });
            playSample.start();
        });

        binding.bgm.setOnClickListener(view -> withBGM = binding.bgm.isChecked());

        binding.del.setOnClickListener(view -> {
            if (!FileUtil.isExistFile(currentRecFile)) {
                Utils.showMessage(this, R.string.no_recorded_sample_yet);
                return;
            }

            AlertDialog.Builder deleteSample = new AlertDialog.Builder(this);
            deleteSample.setTitle(R.string.delete_sample_title);
            deleteSample.setMessage(R.string.delete_sample_content);
            deleteSample.setPositiveButton(R.string.yes, (dialog, which) -> {
                FileUtil.deleteFile(currentRecFile);
                FileUtil.deleteFile(currentPeakFile);

                binding.waveform.resetAmplitudes();
                sampleListAdapter.notifyDataSetChanged();
                Utils.showMessage(this, R.string.deleted);
                totalRecords();
            });
            deleteSample.setNegativeButton(R.string.no, null);
            deleteSample.create().show();
        });

        binding.prev.setOnClickListener(view -> {
            if (currentRecPos == 0) {
                Utils.showMessage(this, R.string.start);
            } else {
                currentRecPos--;
                updateStrings(currentRecPos);
            }
        });

        binding.next.setOnClickListener(view -> {
            if (currentRecPos == (sampleList.size() - 1)) {
                Utils.showMessage(this, R.string.end);
            } else {
                currentRecPos++;
                updateStrings(currentRecPos);
            }
        });

        binding.addComment.setOnClickListener(view ->
            Utils.filePicker(this, getString(R.string.select_an_oremo_comment_file), new String[]{"txt"}, files -> {
                try {
                    InputStream inputstream = new FileInputStream(files[0]);
                    addComments(Utils.copyFromInputStream(inputstream, "Shift_JIS"));
                } catch (FileNotFoundException e) {
                    Utils.showMessage(this, R.string.file_not_found);
                }
            })
        );
    }

    @Override
    protected void onResume() {
        super.onResume();
        useKana = settings.getBoolean(getString(R.string.pref_use_kana), false);
        backgroundMusic = settings.getString(getString(R.string.pref_background_music), "Default");
        recordStart = settings.getInt(getString(R.string.pref_record_start), 4200);
        recordEnd = settings.getInt(getString(R.string.pref_record_end), 9600);
        saveAs = (useKana || currentReclist.equals("Custom")) ? HIRAGANA : ROMAJI;
        if (sampleListAdapter != null) {
            sampleListAdapter.setSaveAs(saveAs);
            sampleListAdapter.notifyDataSetChanged();
        }
    }

    private void initializeLogic() {
        String[] reclist = {"CV", "VCV", "Custom"};

        rekutaDir = FileUtil.getExternalStorageDir().concat("/Rekuta");

        FileUtil.createNewFile(rekutaDir.concat("/.nomedia"));

        binding.textview3.setText(getString(R.string.current_voicebank, "None"));
        binding.spinner1.setAdapter(new ArrayAdapter<>(this, R.layout.spinner_item, reclist));
        binding.edittext1.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, FileUtil.listDirs(rekutaDir)));
        enableAll(false);

        initialized = true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();

        if (itemId == R.id.settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        } else if (itemId == R.id.about) {
            startActivity(new Intent(this, AboutActivity.class));
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            if (binding.edittext1.isFocused()) {
                Utils.setKeyboardDown(this, event, binding.edittext1);
            }
        }
        return super.dispatchTouchEvent(event);
    }

    public void updateStrings(int pos) {
        String sample = sampleList.get(pos)[saveAs];
        currentRecFile = FileUtil.getAbsolutePath(voicebankDir, sample.concat(".wav"));
        currentPeakFile = FileUtil.getAbsolutePath(voicebankDir.concat("/rekuta.cache"), sample.concat(".bin"));

        binding.textview4.setText(sampleList.get(pos)[HIRAGANA]);
        if (sampleList.get(pos)[ROMAJI] == null) {
            binding.textview2.setVisibility(View.GONE);
        } else {
            binding.textview2.setVisibility(View.VISIBLE);
            binding.textview2.setText(sampleList.get(pos)[ROMAJI]);
        }

        loadAmpsFromFile(currentPeakFile);
    }

    public void enableAll(boolean enable) {
        binding.listview1.setVisibility(enable ? View.VISIBLE : View.GONE);
        binding.toolbarTop.setVisibility(enable ? View.VISIBLE : View.GONE);
        binding.set.setEnabled(!initialized || enable);
        binding.bgm.setEnabled(enable);
        binding.del.setEnabled(enable);
        binding.next.setEnabled(enable);
        binding.prev.setEnabled(enable);
        binding.play.setEnabled(enable);
        binding.record.setEnabled(enable);
    }

    public void setupRecording(String path) {
        String folderName = binding.edittext1.getText().toString();

        voicebankDir = FileUtil.getAbsolutePath(rekutaDir, folderName);
        withBGM = false;
        currentRecPos = 0;

        FileUtil.makeDir(voicebankDir);

        try {
            InputStream inputstream;

            if (path != null && currentReclist.equals("Custom")) {
                //noinspection IOStreamConstructor
                inputstream = new FileInputStream(path);
                String[] listSamples = Utils.copyFromInputStream(inputstream, "Shift_JIS").split("\\s+");
                sampleList = new ArrayList<>();
                for (String sample : listSamples) {
                    String[] strArr = new String[2];
                    strArr[0] = sample;
                    sampleList.add(strArr);
                }
                
                saveAs = HIRAGANA;
                binding.addComment.setVisibility(View.VISIBLE);
            } else {
                inputstream = getAssets().open(currentReclist.concat(".json"));
                sampleList = Utils.getFromJSONArray(Utils.copyFromInputStream(inputstream, "UTF-8"));
                
                saveAs = useKana ? HIRAGANA : ROMAJI;
                binding.addComment.setVisibility(View.GONE);
            }
        } catch (IOException e) {
            Utils.showMessage(this, e.getMessage());
            return;
        }

        sampleListAdapter = new ReclistViewAdapter(sampleList, voicebankDir, saveAs);
        binding.listview1.setAdapter(sampleListAdapter);
        binding.textview3.setText(getString(R.string.current_voicebank, folderName));
        binding.waveform.setVisibility(View.VISIBLE);
        binding.bgm.setChecked(withBGM);
        updateStrings(currentRecPos);
        enableAll(true);
        totalRecords();
    }

    public void startRecording() {
        binding.record.setVisibility(View.GONE);
        binding.stop.setVisibility(View.VISIBLE);

        Utils.showMessage(this, R.string.recording);
        recorder.startRecording();
        enableAll(false);
    }

    public void startRecordingWithBGM() {
        if (backgroundMusic.equals("Default")) {
            playBGM = MediaPlayer.create(this, R.raw.bgm);
        } else {
            try {
                playBGM = new MediaPlayer();
                playBGM.setDataSource(backgroundMusic);
                playBGM.prepare();
            } catch (IOException e) {
                Utils.showMessage(this, R.string.custom_bgm_not_found);
                return;
            }
        }
        
        playBGM.setOnCompletionListener(mp -> {
            sampleListAdapter.notifyDataSetChanged();
            Utils.showMessage(this, R.string.done);
            enableAll(true);
            totalRecords();
            playBGM.release();
            playBGM = null;
        });

        Utils.showMessage(this, R.string.please_wait);
        enableAll(false);
        
        Handler handler = new Handler();
        handler.postDelayed(() -> {
            recorder.startRecording();
            Utils.showMessage(this, R.string.recording);

            handler.postDelayed(() -> {
                recorder.stopRecording();
                recorder = null;

                saveAmpsToFile(currentPeakFile);
            }, recordEnd - recordStart);

        }, recordStart);

        playBGM.start();
    }
    
    public void stopRecording() {
        binding.record.setVisibility(View.VISIBLE);
        binding.stop.setVisibility(View.GONE);

        recorder.stopRecording();
        recorder = null;

        sampleListAdapter.notifyDataSetChanged();
        saveAmpsToFile(currentPeakFile);
        enableAll(true);
        totalRecords();
        Utils.showMessage(this, R.string.done);
    }

    public void totalRecords() {
        ArrayList<String> fileNames = FileUtil.listFiles(voicebankDir);

        long total = 0;
        for (String[] sample : sampleList) {
            String fileName = sample[saveAs].concat(".wav");
            if (fileNames.contains(fileName)) {
                total++;
            }
        }

        binding.textview5.setText(String.format(Locale.ENGLISH, "%d/%d", total, sampleList.size()));
    }

    public void addComments(String string) {
        List<String> reference = new ArrayList<>();
        for (String[] element : sampleList) {
            reference.add(element[HIRAGANA]);
        }

        try (BufferedReader reader = new BufferedReader(new StringReader(string))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    continue;
                }

                String[] parts = line.split("\\s+", 2);
                if (parts.length == 2 && reference.contains(parts[0])) {
                    sampleList.set(reference.indexOf(parts[0]), parts);
                }
            }
        } catch (IOException ignored) { }

        sampleListAdapter.notifyDataSetChanged();
        updateStrings(currentRecPos);
    }

    public void saveAmpsToFile(String cacheFile) {
        byte[] ampBytes = binding.waveform.exportDataToBytes();

        FileUtil.writeBytesToFile(ampBytes, cacheFile);
    }

    public void loadAmpsFromFile(String cacheFile) {
        if (!FileUtil.isExistFile(cacheFile)) {
            binding.waveform.resetAmplitudes();
            return;
        }

        byte[] byteData = FileUtil.readBytesFromFile(cacheFile);

        binding.waveform.setAmplitudes(Utils.byteArrayToFloatList(byteData));
    }

}
