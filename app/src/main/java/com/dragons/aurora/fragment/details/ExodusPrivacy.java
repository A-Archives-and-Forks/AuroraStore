package com.dragons.aurora.fragment.details;


import android.content.Context;
import android.content.Intent;
import android.graphics.Paint;
import android.net.Uri;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.dragons.aurora.R;
import com.dragons.aurora.fragment.DetailsFragment;
import com.dragons.aurora.model.App;
import com.percolate.caffeine.ViewUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import static com.android.volley.VolleyLog.TAG;

public class ExodusPrivacy extends AbstractHelper {

    public ExodusPrivacy(DetailsFragment fragment, App app) {
        super(fragment, app);
    }

    @Override
    public void draw() {
        getExodusReport(fragment.getActivity(), "https://reports.exodus-privacy.eu.org/api/search/" + app.getPackageName());
    }

    private void getExodusReport(Context context, String EXODUS_PATH) {
        RequestQueue mRequestQueue = Volley.newRequestQueue(context);
        JsonObjectRequest jsonObjReq = new JsonObjectRequest(Request.Method.GET,
                EXODUS_PATH, null, response -> {
            try {
                JSONObject exodusReport = response.getJSONObject(app.getPackageName());
                JSONArray reportsArray = exodusReport.getJSONArray("reports");
                JSONObject trackersReport = reportsArray.getJSONObject(0);
                JSONArray trackers = trackersReport.getJSONArray("trackers");
                String appId = trackersReport.getString("id");
                drawExodus(trackers, appId);
                Log.i("EXODUS_PRIVACY", trackers.toString());
            } catch (JSONException e) {
                Log.i("EXODUS_PRIVACY", "Error occurred at Exodus Privacy");
            }
        }, error -> VolleyLog.d(TAG, "Error: " + error.getMessage()));
        mRequestQueue.add(jsonObjReq);
    }

    private void drawExodus(JSONArray appTrackers, String appId) {
        if (fragment.getActivity() != null) {
            ViewUtils.findViewById(fragment.getActivity(), R.id.exodus_card).setVisibility(View.VISIBLE);
            if (appTrackers.length() > 0) {
                setText(fragment.getView(), R.id.exodus_description, R.string.exodus_hasTracker, appTrackers.length());
            } else {
                setText(fragment.getView(), R.id.exodus_description, R.string.exodus_noTracker);
            }

            TextView viewMore = fragment.getActivity().findViewById(R.id.viewMore);
            viewMore.setPaintFlags(viewMore.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
            viewMore.setOnClickListener(click -> fragment.getActivity().startActivity(new Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://reports.exodus-privacy.eu.org/reports/" + appId + "/"))));
        }
    }
}
