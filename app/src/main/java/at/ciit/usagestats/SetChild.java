package at.ciit.usagestats;

import android.text.Editable;

import com.android.volley.Response;
import com.android.volley.toolbox.StringRequest;

import java.util.HashMap;
import java.util.Map;

public class SetChild extends StringRequest {
    private static final String REGISTER_REQUEST_URL = "https://ehealthtest.000webhostapp.com/Register.php";
    private Map<String,String> params;

    public SetChild(String name, Response.Listener<String> listener){
        super(Method.POST, REGISTER_REQUEST_URL, listener, null);
        params = new HashMap<>();
        params.put("name",name);
    }

    @Override
    public Map<String, String> getParams() {
        return params;
    }
}
