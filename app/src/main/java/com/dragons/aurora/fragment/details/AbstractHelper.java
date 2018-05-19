package com.dragons.aurora.fragment.details;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.v4.graphics.ColorUtils;
import android.support.v4.view.ViewCompat;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.dragons.aurora.R;
import com.dragons.aurora.fragment.DetailsFragment;
import com.dragons.aurora.fragment.PreferenceFragment;
import com.dragons.aurora.model.App;
import com.percolate.caffeine.PhoneUtils;
import com.percolate.caffeine.ViewUtils;

public abstract class AbstractHelper {

    protected DetailsFragment fragment;
    protected App app;

    public AbstractHelper(DetailsFragment fragment, App app) {
        this.fragment = fragment;
        this.app = app;
    }

    abstract public void draw();

    protected void setText(View v, int viewId, String text) {
        TextView textView = ViewUtils.findViewById(v, viewId);
        if (null != textView)
            textView.setText(text);
    }

    protected void setText(View v, int viewId, int stringId, Object... text) {
        setText(v, viewId, v.getResources().getString(stringId, text));
    }

    protected void initExpandableGroup(int viewIdHeader, int viewIdContainer, final View.OnClickListener l) {
        TextView viewHeader = (TextView) fragment.getActivity().findViewById(viewIdHeader);
        viewHeader.setVisibility(View.VISIBLE);
        final LinearLayout viewContainer = (LinearLayout) fragment.getActivity().findViewById(viewIdContainer);
        viewHeader.setOnClickListener(v -> {
            boolean isExpanded = viewContainer.getVisibility() == View.VISIBLE;
            if (isExpanded) {
                viewContainer.setVisibility(View.GONE);
                ((TextView) v).setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_expand_more, 0);
            } else {
                if (null != l) {
                    l.onClick(v);
                }
                viewContainer.setVisibility(View.VISIBLE);
                ((TextView) v).setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_expand_less, 0);
            }
        });
    }

    protected boolean isLoggedIn() {
        return PreferenceFragment.getBoolean(fragment.getActivity(), "LOGGED_IN");
    }

    protected boolean isDummy() {
        return PreferenceFragment.getBoolean(fragment.getActivity(), "DUMMY_ACC");
    }

    protected boolean isGoogle() {
        return PreferenceFragment.getBoolean(fragment.getActivity(), "GOOGLE_ACC");
    }

    protected boolean isConnected(Context c) {
        return PhoneUtils.isNetworkAvailable(c);
    }

    protected void hide(View v, int viewID) {
        ViewUtils.findViewById(v, viewID).setVisibility(View.GONE);
    }

    protected void show(View v, int viewID) {
        ViewUtils.findViewById(v, viewID).setVisibility(View.VISIBLE);
    }

    protected void paintButton(int color, int buttonId) {
        android.widget.Button button = fragment.getActivity().findViewById(buttonId);
        if (button != null)
            ViewCompat.setBackgroundTintList(button, ColorStateList.valueOf(color));
    }

    protected void paintRLayout(int color, int layoutId) {
        RelativeLayout relativeLayout = fragment.getActivity().findViewById(layoutId);
        if (relativeLayout != null)
            relativeLayout.setBackgroundColor(color);
    }

    void paintLLayout(int color, int viewID) {
        LinearLayout layout = fragment.getActivity().findViewById(viewID);
        if (layout != null)
            ViewCompat.setBackgroundTintList(layout, ColorStateList.valueOf(ColorUtils.setAlphaComponent(color, 50)));
    }

    protected void paintTextView(int color, int textViewId) {
        TextView textView = fragment.getActivity().findViewById(textViewId);
        if (textView != null)
            textView.setTextColor(color);
    }

    protected void paintImageView(int color, int imageViewId) {
        ImageView imageView = fragment.getActivity().findViewById(imageViewId);
        if (imageView != null)
            imageView.setColorFilter(color);
    }

    protected void paintImageViewBg(int color, int imageViewId) {
        ImageView imageView = fragment.getActivity().findViewById(imageViewId);
        if (imageView != null)
            imageView.setBackgroundColor(color);
    }

    @NonNull
    protected Bitmap getBitmapFromDrawable(@NonNull Drawable drawable) {
        if (drawable instanceof BitmapDrawable) {
            return ((BitmapDrawable) drawable).getBitmap();
        }
        final Bitmap bmp = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        final Canvas canvas = new Canvas(bmp);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bmp;
    }
}
