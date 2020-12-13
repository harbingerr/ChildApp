package at.ciit.usagestats;

import com.android.volley.Response;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import static com.android.volley.Request.Method.POST;

public class SetStats extends StringRequest {
    private static final String REGISTER_REQUEST_URL = "https://ehealthtest.000webhostapp.com/SetStats.php";
    private Map<String,String> params;

    public SetStats(String id, String date, String youtube, String instagram, String snapchat, String tiktok, String facebook, String twitter, String twitch, String messenger, Response.Listener<String> listener){
        super(Method.POST, REGISTER_REQUEST_URL, listener, null);
        params = new HashMap<>();
        //"youtube" "instagram" "snapchat" "tiktok" "facebook" "twitter" "twitch" "messenger"
        params.put("id",id);
        params.put("date",date);
        params.put("youtube",youtube);
        params.put("instagram",instagram);
        params.put("snapchat",snapchat);
        params.put("tiktok",tiktok);
        params.put("facebook",facebook);
        params.put("twitter",twitter);
        params.put("twitch",twitch);
        params.put("messenger",messenger);

    }

    @Override
    public Map<String, String> getParams() {
        return params;
    }
}
