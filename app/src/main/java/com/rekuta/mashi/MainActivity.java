package com.rekuta.mashi;

import android.animation.*;
import android.app.*;
import android.app.Activity;
import android.content.*;
import android.content.SharedPreferences;
import android.content.res.*;
import android.graphics.*;
import android.graphics.drawable.*;
import android.media.*;
import android.media.MediaPlayer;
import android.net.*;
import android.os.*;
import android.text.*;
import android.text.style.*;
import android.util.*;
import android.view.*;
import android.view.View;
import android.view.View.*;
import android.view.animation.*;
import android.webkit.*;
import android.widget.*;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.*;
import java.text.*;
import java.util.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.*;
import omrecorder.*;
import org.json.*;

public class MainActivity extends Activity {
	
	private Timer _timer = new Timer();
	
	private String filePath = "";
	private double currentRec = 0;
	private boolean withBGM = false;
	private String audioEffect = "";
	private String saveAs = "";
	private String currentReclist = "";
	private Recorder recorder;
	
	private ArrayList<String> reclist = new ArrayList<>();
	private ArrayList<HashMap<String, Object>> samp = new ArrayList<>();
	      
	private EditText edittext1;
	private Button set;
	private TextView textview3;
	private Spinner spinner1; 
	private ListView listview1;
	private TextView textview4;
	private TextView textview2;
	private Button record;
	private Button stop;
	private Button play;
	private ToggleButton bgm;
	private Button del;
	private Button prev;
	private Button next;
	
	private TimerTask samplerec;
	private MediaPlayer playsamp;
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
		set = findViewById(R.id.set);
		textview3 = findViewById(R.id.textview3);
		spinner1 = findViewById(R.id.spinner1); 
		listview1 = findViewById(R.id.listview1);
		textview4 = findViewById(R.id.textview4);
		textview2 = findViewById(R.id.textview2);
		record = findViewById(R.id.record);
		stop = findViewById(R.id.stop);
		play = findViewById(R.id.play);
		bgm = findViewById(R.id.bgm);
		del = findViewById(R.id.del);
		prev = findViewById(R.id.prev);
		next = findViewById(R.id.next);
		settings = getSharedPreferences("settings", Activity.MODE_PRIVATE);
		
		set.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View _view) {
				if (edittext1.getText().toString().equals("")) {
					AppUtil.showMessage(getApplicationContext(), "Please enter voicebank folder.");
					return;
				}
				
				currentReclist = reclist.get((int) (spinner1.getSelectedItemPosition()));
				
				if (currentReclist == "Custom") {
					Intent pickFile = new Intent(Intent.ACTION_GET_CONTENT);
					pickFile.setType("*/*");
					startActivityForResult(pickFile, 0);
				} else {
					_setupRecording(null);
				}
			}
		});
		
		listview1.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> _param1, View _param2, int _param3, long _param4) {
				final int _position = _param3;
				_updateTextViews(_position);
				currentRec = _position;
			}
		});
		
		record.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View _view) {
				_setRecSamp(samp.get((int)currentRec).get(saveAs).toString());
				if (withBGM) {
					playsamp = MediaPlayer.create(getApplicationContext(), R.raw.bgm);
					playsamp.start();
					AppUtil.showMessage(getApplicationContext(), "Please Wait...");
					set.setEnabled(false);
					_enableAll(false);
					samplerec = new TimerTask() {
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
					};
					_timer.schedule(samplerec, (int)(4000));
					playsamp.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
						
						    @Override
						    public void onCompletion(MediaPlayer mp) {
							try {
								recorder.stopRecording();
							} catch(java.io.IOException e) {
								e.printStackTrace();
							}
							_enableAll(true);
							set.setEnabled(true);
							AppUtil.showMessage(getApplicationContext(), "Done!");
							playsamp.reset();
							((BaseAdapter)listview1.getAdapter()).notifyDataSetChanged();
						}
						
					});
				}
				else {
					recorder.startRecording();
					record.setVisibility(View.GONE);
					stop.setVisibility(View.VISIBLE);
					set.setEnabled(false);
					_enableAll(false);
					AppUtil.showMessage(getApplicationContext(), "Recording...");
				}
			}
		});
		
		stop.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View _view) {
				try {
					recorder.stopRecording();
				} catch(java.io.IOException e) {
					e.printStackTrace();
				}
				record.setVisibility(View.VISIBLE);
				stop.setVisibility(View.GONE);
				set.setEnabled(true);
				_enableAll(true);
				((BaseAdapter)listview1.getAdapter()).notifyDataSetChanged();
				AppUtil.showMessage(getApplicationContext(), "Done!");
			}
		});
		
		play.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View _view) {
				if (FileUtil.isExistFile(filePath.concat("/".concat(samp.get((int)currentRec).get(saveAs).toString()).concat(".wav")))) {
					playsamp = MediaPlayer.create(getApplicationContext(), Uri.parse(filePath + "/" + samp.get((int)currentRec).get(saveAs).toString() + ".wav"));
					playsamp.start();
						
					_enableAll(false);
					playsamp.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
					    @Override
					    public void onCompletion(MediaPlayer mp) {
							_enableAll(true);
							AppUtil.showMessage(getApplicationContext(), "Done!");
					    }
                    });
				}
				else {
					AppUtil.showMessage(getApplicationContext(), "No recorded sample yet.");
				}
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
				if (FileUtil.isExistFile(filePath.concat("/".concat(samp.get((int)currentRec).get(saveAs).toString()).concat(".wav")))) {
					AlertDialog.Builder delSampDlg = new AlertDialog.Builder(MainActivity.this);
					delSampDlg.setTitle(R.string.delsamp_title);
					delSampDlg.setMessage(R.string.delsamp_content);
					delSampDlg.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface _dialog, int _which) {
							FileUtil.deleteFile(filePath.concat("/".concat(samp.get((int)currentRec).get(saveAs).toString()).concat(".wav")));
							((BaseAdapter)listview1.getAdapter()).notifyDataSetChanged();
							AppUtil.showMessage(getApplicationContext(), "Deleted");
						}
					});
					delSampDlg.setNegativeButton("No", null);
					delSampDlg.create().show();
				}
				else {
					AppUtil.showMessage(getApplicationContext(), "No recorded sample yet.");
				}
			}
		});
		
		prev.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View _view) {
				if (currentRec == 0) {
					AppUtil.showMessage(getApplicationContext(), "Start");
				}
				else {
					currentRec--;
					_updateTextViews(currentRec);
				}
			}
		});
		
		next.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View _view) {
				if (currentRec == (samp.size() - 1)) {
					AppUtil.showMessage(getApplicationContext(), "End");
				}
				else {
					currentRec++;
					_updateTextViews(currentRec);
				}
			}
		});
	}
	
	private void initializeLogic() {
		
		audioEffect = settings.getString("audioFx", "normal");
		saveAs = settings.getString("saveAs", "romaji");
		
		reclist.add("CV");
		reclist.add("VCV");
		reclist.add("Custom");
			
		spinner1.setAdapter(new ArrayAdapter<String>(getBaseContext(), android.R.layout.simple_spinner_dropdown_item, reclist));
		((ArrayAdapter)spinner1.getAdapter()).notifyDataSetChanged();
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
		final int _id = item.getItemId();
		final String _title = (String) item.getTitle();
		switch((int)_id) {
			case ((int)0): {
				_showSettings();
				break;
			}
			case ((int)1): {
				_showAbout();
				break;
			}
		}
		return super.onOptionsItemSelected(item);
	}
	
	public void _setRecSamp(final String _samp) {
		recorder = OmRecorder.wav(new PullTransport.Default(mic(audioEffect)), new java.io.File(filePath +"/"+ _samp+".wav")); 
	}
	
	public void _updateTextViews(final double _pos) {
		textview4.setText(samp.get((int)_pos).get("hiragana").toString());
		textview2.setText(samp.get((int)_pos).get("romaji").toString());
	}
	
	
	public void _showSettings() {
		AlertDialog.Builder settingsDialog = new AlertDialog.Builder(MainActivity.this);
		settingsDialog.setTitle("Settings");
		LayoutInflater inflater = getLayoutInflater();
		View convertView = (View) inflater.inflate(R.layout.settings, null);
		
		final Switch switch1 = (Switch) convertView.findViewById(R.id.switch1);
		final Switch switch2 = (Switch) convertView.findViewById(R.id.switch2);
		final Switch switch3 = (Switch) convertView.findViewById(R.id.switch3);
		
		switch(audioEffect) {
			case "noisesupressor": {
				switch1.setChecked(true);
				switch2.setChecked(false);
				break;
			}
			case "gaincontrol": {
				switch1.setChecked(false);
				switch2.setChecked(true);
				break;
			}
			case "both": {
				switch1.setChecked(true);
				switch2.setChecked(true);
				break;
			}
			default: {
				switch1.setChecked(false);
				switch2.setChecked(false);
				break;
			}
		}
		
		if (saveAs.equals("hiragana")) switch3.setChecked(true);
		
		settingsDialog.setView(convertView);
		
		settingsDialog.setPositiveButton("Save", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface _dialog, int _which) {
				switch(switch1.isChecked() + "-" + switch2.isChecked()) {
					case "true-true": {
						settings.edit().putString("audioFx", "both").commit();
						audioEffect = "both";
						break;
					}
					case "true-false": {
						settings.edit().putString("audioFx", "noisesuppressor").commit();
						audioEffect = "noisesupressor";
						break;
					}
					case "false-true": {
						settings.edit().putString("audioFx", "gaincontrol").commit();
						audioEffect = "gaincontrol";
						break;
					}
					default: {
						settings.edit().putString("audioFx", "normal").commit();
						audioEffect = "normal";
						break;
					}
				}
                
				if (switch3.isChecked()) {
					settings.edit().putString("saveAs", "hiragana").commit();
					saveAs = "hiragana";
				}
				else {
					settings.edit().putString("saveAs", "romaji").commit();
					saveAs = "romaji";
				}
                
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
	
	
	public void _enableAll(final boolean _enable) {
		del.setEnabled(_enable);
		next.setEnabled(_enable);
		prev.setEnabled(_enable);
		play.setEnabled(_enable);
		record.setEnabled(_enable);
		if (currentReclist.equals("CV")) {
			withBGM = false;
			bgm.setEnabled(false);
		}
		else {
			bgm.setEnabled(_enable);
		}
		if (_enable) {
			listview1.setVisibility(View.VISIBLE);
		}
		else {
			listview1.setVisibility(View.GONE);
		}
	}
	
	public void _setupRecording(Uri _uri) {

        String folderName;
        
		if (currentReclist.equals("Custom")) {
            folderName = edittext1.getText().toString();
        } else {
            folderName = edittext1.getText().toString().concat(" ".concat(currentReclist));
        }

        filePath = FileUtil.getExternalStorageDir().concat("/Rekuta/".concat(folderName));
        currentRec = 0;
        if (!FileUtil.isExistFile(filePath)) {
            FileUtil.makeDir(filePath);
            FileUtil.writeFile(filePath.concat("/.nomedia"), "");
        }
        textview3.setText("Voicebank: ".concat(folderName));

        try {
			InputStream inputstream1;
			
			if (_uri != null && currentReclist.equals("Custom")) {
				inputstream1 = getContentResolver().openInputStream(_uri);
				String[] listSamps = AppUtil.copyFromInputStream(inputstream1).split("\\s+");
				samp.clear();
				for (String str : listSamps) {
					HashMap<String, Object> hashMap = new HashMap<String, Object>();
					hashMap.put("hiragana", str);
					hashMap.put("romaji", str);
					samp.add(hashMap);
				}
			} else {
				inputstream1 = getAssets().open(currentReclist.concat(".json"));
				samp = new Gson().fromJson(AppUtil.copyFromInputStream(inputstream1),new TypeToken<ArrayList<HashMap<String, Object>>>() {}.getType());	
			}
			
            throw new java.io.IOException();
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
        _updateTextViews(currentRec);
        listview1.setAdapter(new Listview1Adapter(samp));
        ((BaseAdapter) listview1.getAdapter()).notifyDataSetChanged();
        _enableAll(true);
	}
	
    private PullableSource mic(String strSet) {

        switch (strSet) {
        case "noisesuppressor":
            return new PullableSource.NoiseSuppressor(
                new PullableSource.Default(
                    new AudioRecordConfig.Default(
                        MediaRecorder.AudioSource.UNPROCESSED, AudioFormat.ENCODING_PCM_16BIT,
                        AudioFormat.CHANNEL_IN_MONO, 44100
                    )
                )
            );
        case "gaincontrol":
            return new PullableSource.AutomaticGainControl(
                new PullableSource.Default(
                    new AudioRecordConfig.Default(
                        MediaRecorder.AudioSource.UNPROCESSED, AudioFormat.ENCODING_PCM_16BIT,
                        AudioFormat.CHANNEL_IN_MONO, 44100
                    )
                )
            );
        case "both":
            return new PullableSource.AutomaticGainControl(
                new PullableSource.NoiseSuppressor(
                    new PullableSource.Default(
                        new AudioRecordConfig.Default(
                            MediaRecorder.AudioSource.UNPROCESSED, AudioFormat.ENCODING_PCM_16BIT,
                            AudioFormat.CHANNEL_IN_MONO, 44100
                        )
                    )
                )
            );
        default:
            return new PullableSource.Default(
                new AudioRecordConfig.Default(
                    MediaRecorder.AudioSource.UNPROCESSED, AudioFormat.ENCODING_PCM_16BIT,
                    AudioFormat.CHANNEL_IN_MONO, 44100
                )
            );
        }
    }
	
	public class Listview1Adapter extends BaseAdapter {
		
		ArrayList<HashMap<String, Object>> _data;
		
		public Listview1Adapter(ArrayList<HashMap<String, Object>> _arr) {
			_data = _arr;
		}
		
		@Override
		public int getCount() {
			return _data.size();
		}
		
		@Override
		public HashMap<String, Object> getItem(int _index) {
			return _data.get(_index);
		}
		
		@Override
		public long getItemId(int _index) {
			return _index;
		}
		
		@Override
		public View getView(final int _position, View _v, ViewGroup _container) {
			LayoutInflater _inflater = getLayoutInflater();
			View _view = _v;
			if (_view == null) {
				_view = _inflater.inflate(R.layout.listsamples, null);
			}
			  
			final ImageView imageview1 = _view.findViewById(R.id.imageview1);
			final TextView textview1 = _view.findViewById(R.id.textview1);
			final TextView textview2 = _view.findViewById(R.id.textview2);
			
			textview1.setText(_data.get((int)_position).get("hiragana").toString());
			textview2.setText(_data.get((int)_position).get("romaji").toString());
			if (FileUtil.isExistFile(filePath.concat("/".concat(_data.get((int)_position).get(saveAs).toString().concat(".wav"))))) {
				imageview1.setImageResource(R.drawable.ic_check_box_black);
			}
			else {
				imageview1.setImageResource(R.drawable.ic_check_box_outline_blank_black);
			}
			
			return _view;
		}
	}
}