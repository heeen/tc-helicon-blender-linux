package tchelicon.com.blenderappandroid;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.util.Arrays;
import java.util.Comparator;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/* JADX INFO: loaded from: classes.dex */
public class FileSystem {
    private static final String TAG = "FileSystem";

    public String presetFolderPath(Context context) {
        return context.getFilesDir().getAbsolutePath() + "/presets/";
    }

    public String nextPresetPath(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(Constants.PREFS_NAME, 0);
        SharedPreferences.Editor editorEdit = sharedPreferences.edit();
        int i = sharedPreferences.getInt(Constants.kPresetNumber, 1);
        editorEdit.putInt(Constants.kPresetNumber, i + 1);
        editorEdit.commit();
        return "Preset " + Integer.toString(i) + ".json";
    }

    public boolean savePreset(Context context, String[] strArr) {
        String strNextPresetPath = nextPresetPath(context);
        String strReplace = strNextPresetPath.replace(".json", "");
        try {
            JSONObject jSONObject = new JSONObject();
            JSONArray jSONArray = new JSONArray(strArr);
            jSONObject.put(Constants.kPresetName, strReplace);
            jSONObject.put(Constants.kPresetValues, jSONArray);
            String string = jSONObject.toString();
            try {
                File file = new File(presetFolderPath(context), strNextPresetPath);
                file.getParentFile().mkdirs();
                FileOutputStream fileOutputStream = new FileOutputStream(file);
                fileOutputStream.write(string.getBytes());
                fileOutputStream.close();
                return true;
            } catch (Exception e) {
                e.printStackTrace();
                Log.e(TAG, "savePreset write failed: " + e.toString());
                return false;
            }
        } catch (JSONException e2) {
            Log.e(TAG, "savePreset unexpected JSON exception", e2);
            return false;
        }
    }

    public void saveNewPresetName(Context context, String str, String str2) {
        JSONObject jSONObjectLoadPreset = loadPreset(context, str);
        try {
            jSONObjectLoadPreset.put(Constants.kPresetName, str2);
            String string = jSONObjectLoadPreset.toString();
            try {
                FileOutputStream fileOutputStream = new FileOutputStream(new File(presetFolderPath(context), str));
                fileOutputStream.write(string.getBytes());
                fileOutputStream.close();
            } catch (Exception e) {
                e.printStackTrace();
                Log.e(TAG, "savePreset write failed: " + e.toString());
            }
        } catch (JSONException e2) {
            Log.e(TAG, "saveNewPresetName unexpected JSON exception", e2);
        }
    }

    public String[] listPresetFiles(Context context) {
        String[] list = new File(presetFolderPath(context)).list();
        if (list != null) {
            Arrays.sort(list, new Comparator<String>() { // from class: tchelicon.com.blenderappandroid.FileSystem.1
                @Override // java.util.Comparator
                public int compare(String str, String str2) {
                    return Integer.compare(Integer.parseInt(str.split(" ")[1].replace(".json", "")), Integer.parseInt(str2.split(" ")[1].replace(".json", "")));
                }
            });
        }
        return list;
    }

    public String[] listPresetNames(Context context, String[] strArr) {
        String[] strArr2 = new String[strArr.length];
        int i = 0;
        for (String str : strArr) {
            JSONObject jSONObjectLoadPreset = loadPreset(context, str);
            if (jSONObjectLoadPreset != null) {
                try {
                    strArr2[i] = jSONObjectLoadPreset.getString(Constants.kPresetName);
                } catch (JSONException e) {
                    Log.d(TAG, "listPresetNames getPreset name failed: " + e.toString());
                    strArr2[i] = str;
                }
            } else {
                strArr2[i] = str;
            }
            i++;
        }
        return strArr2;
    }

    public JSONObject loadPreset(Context context, String str) {
        File file = new File(presetFolderPath(context), str);
        if (file.exists()) {
            try {
                String str2 = "";
                BufferedReader bufferedReader = new BufferedReader(new FileReader(file));
                while (true) {
                    String line = bufferedReader.readLine();
                    if (line != null) {
                        str2 = str2 + line;
                    } else {
                        bufferedReader.close();
                        Log.d(TAG, "Preset: " + str2);
                        try {
                            return new JSONObject(str2);
                        } catch (JSONException e) {
                            Log.d(TAG, "LoadPreset json object failed: " + e.toString());
                            return null;
                        }
                    }
                }
            } catch (Exception e2) {
                e2.printStackTrace();
                Log.e(TAG, "loadPreset write failed: " + e2.toString());
                return null;
            }
        } else {
            Log.d(TAG, "Load presetFile doesnt exist");
            return null;
        }
    }

    public void deletePreset(Context context, String str) {
        File file = new File(presetFolderPath(context), str);
        if (!file.exists() || file.delete()) {
            return;
        }
        Log.d(TAG, "Preset Failed to delete");
    }
}
