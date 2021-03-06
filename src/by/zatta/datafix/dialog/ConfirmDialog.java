package by.zatta.datafix.dialog;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import by.zatta.datafix.BaseActivity;
import by.zatta.datafix.R;
import by.zatta.datafix.assist.ShellProvider;
import by.zatta.datafix.model.AppEntry;
import android.app.DialogFragment;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.LinearLayout.LayoutParams;

public class ConfirmDialog extends DialogFragment implements View.OnClickListener{
	List<AppEntry> fls;
	CheckBox mCbCopyScript;
	
	private LinearLayout mLinLayFlashView;
	
    public static ConfirmDialog newInstance(List<AppEntry> apps) {
        ConfirmDialog f = new ConfirmDialog();
        
        Bundle args = new Bundle();
        args.putParcelableArrayList("lijst", (ArrayList<? extends Parcelable>) apps);
        f.setArguments(args);
        
        return f;
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        fls = getArguments().getParcelableArrayList("lijst");
        setStyle(DialogFragment.STYLE_NORMAL, 0);
        setRetainInstance(false);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
    	getDialog().setTitle("Create / update files");
        View v = inflater.inflate(R.layout.confirm_dialog, container, false);
        
        TextView tv = (TextView) v.findViewById(R.id.text);
        mCbCopyScript = (CheckBox) v.findViewById(R.id.cbCopyScript);
        
        SharedPreferences getPrefs = PreferenceManager.getDefaultSharedPreferences(getActivity().getBaseContext());
		String tibuState = getPrefs.getString("tibuState", "undefined");
		int color = 0;
		if (tibuState.contains("true")){
			tibuState = "You have Titanium installed and you have backupFollowSymlinks checked.";
			color = getResources().getColor(R.color.green);
		}
		if (tibuState.contains("false")){
			tibuState = "You have Titanium installed and need to check backupFollowSymLinks in it's settings.";
			color = getResources().getColor(R.color.red);
		}
		if (tibuState.contains("null")){
			tibuState = "You don't have Titanium Backup installed. Once you do, check backupFollowSymLinks in it's settings.";
			color = getResources().getColor(R.color.white);
		}
		tv.setTextColor(color);
		tv.setText(tibuState);
		
        mLinLayFlashView = (LinearLayout) v.findViewById(R.id.llFilesForDialog);
        buildForm();
        Button NO = (Button)v.findViewById(R.id.btnNoInstall);
        Button YESANDREBOOT = (Button) v.findViewById(R.id.btnYesAndReboot);
        Button YESNOREBOOT = (Button) v.findViewById(R.id.btnYesNoReboot);
        
        NO.setOnClickListener(this); 
        YESANDREBOOT.setOnClickListener(this); 
        YESNOREBOOT.setOnClickListener(this);
		return v;
    }

    private void buildForm() {
		boolean cachefile = false;
    	boolean datafile = false;
    	for (AppEntry ff : fls){
    		if (ff.getCacheBool().equals("true")) cachefile = true;
    		if (ff.getDataBool().equals("true")) datafile = true;
    	}
    	addFormField("Add to cache.txt:");
    	if (cachefile){
    		for (AppEntry ff: fls){
    			if (ff.getCacheBool().equals("true"))
    				addFormField("  "+ ff.getPackName());
    		}
    	}else{
    		addFormField("  No caches will go to /data/data");
    	}
    	
    	addFormField("");
    	
    	addFormField("Add to data.txt:");
    	if (datafile){	
    		for (AppEntry ff: fls){
    			if (ff.getDataBool().equals("true"))
    				addFormField("  "+ ff.getPackName());
    		}	
    	}else{
    		addFormField("  No apps will fully reside in /data/data");
    	}
	}

	private void addFormField(String label) {
		TextView tvLabel = new TextView(getActivity());
		tvLabel.setLayoutParams(getDefaultParams(false));
		tvLabel.setText(label);

		mLinLayFlashView.addView(tvLabel);
		
	}

	private LayoutParams getDefaultParams(boolean isLabel) {
		LayoutParams params = new LayoutParams(LayoutParams.FILL_PARENT,
				LayoutParams.WRAP_CONTENT);
		if (isLabel) {
			params.bottomMargin = 5;
			params.topMargin = 10;
		}
		return params;
	}

	
	@Override
	public void onClick(View v) {
		SharedPreferences getPrefs = PreferenceManager.getDefaultSharedPreferences(getActivity().getBaseContext());
		String scripttype = getPrefs.getString("initDContent", "undefined");
		String copyscript = "";
		if (mCbCopyScript.isChecked()) copyscript = " copyscript";
		else copyscript = " dontcopy";
		switch (v.getId()){
		case R.id.btnNoInstall:
			break;
		case R.id.btnYesAndReboot:
			try {
				writeFiles();
				ShellProvider.INSTANCE.getCommandOutput("chmod 777 /data/data/by.zatta.datafix/files/totalscript.sh");
				ShellProvider.INSTANCE.getCommandOutput("/data/data/by.zatta.datafix/files/totalscript.sh prepare_runtime " + scripttype + copyscript +" reboot");
				} catch (Exception e) {	}
				
			break;
		case R.id.btnYesNoReboot:
			try {
				writeFiles();
				ShellProvider.INSTANCE.getCommandOutput("chmod 777 /data/data/by.zatta.datafix/files/totalscript.sh");
				ShellProvider.INSTANCE.getCommandOutput("/data/data/by.zatta.datafix/files/totalscript.sh prepare_runtime " + scripttype + copyscript +" noreboot");
				} catch (Exception e) {	}
			break;
		}
		dismiss();
	}

	public void writeFiles(){
	    try
	    {
	    	
	    	// /data/local with two files : "move_cache.txt" and "skip_apps.txt".
	    	File cache = new File(getActivity().getBaseContext().getFilesDir()+"/move_cache.txt");
	        cache.delete();
	        FileWriter c = new FileWriter(cache, true);
	        File data = new File(getActivity().getBaseContext().getFilesDir()+"/skip_apps.txt");
	        data.delete();
	        FileWriter d = new FileWriter(data, true);
	        File user = new File(getActivity().getBaseContext().getFilesDir()+"/user_apps.txt");
	        user.delete();
	        FileWriter u = new FileWriter(user, true);
	        File syst = new File(getActivity().getBaseContext().getFilesDir()+"/system_apps.txt");
	        syst.delete();
	        FileWriter s = new FileWriter(syst, true);
	         
	        for (AppEntry ff: fls){
				if (ff.getCacheBool().equals("true")){
					c.append(ff.getPackName()+ '\n');
				}
				if (ff.getDataBool().equals("true")){
					d.append(ff.getPackName()+ '\n');
				}
				if (ff.getType().equals("user")){
					u.append(ff.getPackName() + '\n');
				}
				if (ff.getType().equals("system")){
					s.append(ff.getPackName() + '\n');
				}
				
			}
	        	        
	        
	        	        
	        c.flush();
	        c.close();
	        d.flush();
	        d.close();
	        u.flush();
	        u.close();
	        s.flush();
	        s.close();
	        
	        if (BaseActivity.DEBUG){
	        System.out.println("Wrote file:" + cache.getName() );
	        System.out.println("Wrote file:" + data.getName() );
	        }
	     }catch(IOException e){}
	   }    
}

