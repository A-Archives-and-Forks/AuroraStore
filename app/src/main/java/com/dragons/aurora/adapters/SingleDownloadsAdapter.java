package com.dragons.aurora.adapters;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.graphics.ColorUtils;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.dragons.aurora.R;
import com.dragons.aurora.Util;

public class SingleDownloadsAdapter extends RecyclerView.Adapter<SingleDownloadsAdapter.ViewHolder> {

    private static SingleClickListener dClickListener;
    private static int dSelected = -1;
    private static boolean isFirstDownload = true;

    private ViewHolder viewHolder;
    private Context context;
    private String[] mLabels;
    private String[] mValues;
    private Integer dotColor;
    private Integer[] mDotColors = {
            R.color.colorGreen,
            R.color.colorRed,
            R.color.colorOrange,
            R.color.colorPurple,
            R.color.colorGold,
            R.color.colorGreenAlt,
            R.color.colorCyan,
            R.color.colorBlue,
            R.color.colorLime,
            R.color.colorPink
    };

    public SingleDownloadsAdapter(Context context, String[] mLabels, String[] mValues) {
        this.context = context;
        this.mLabels = mLabels;
        this.mValues = mValues;
    }

    private void setViewHolder(ViewHolder viewHolder) {
        this.viewHolder = viewHolder;
    }

    public void setOnDownloadBadgeClickListener(SingleClickListener clickListener) {
        dClickListener = clickListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.filter_badge, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        setViewHolder(holder);
        dotColor = viewHolder.itemView.getResources().getColor(mDotColors[position % mDotColors.length]);
        viewHolder.badgeText.setText(mLabels[position]);
        viewHolder.badgeDot.setBackgroundColor(dotColor);
        if (isFirstDownload && isLastPref(position)) {
            toggleBadge(true);
            isFirstDownload = false;
        } else
            setupBadge(position);
    }

    @Override
    public int getItemCount() {
        return mValues.length;
    }

    private void setupBadge(int position) {
        if (dSelected == position) {
            toggleBadge(true);
            putPref(Integer.parseInt(mValues[position]));
        } else {
            toggleBadge(false);
        }
    }

    private void toggleBadge(boolean toggle) {
        if (toggle) {
            viewHolder.badgeText.setTextColor(Color.WHITE);
            paintBadge(viewHolder);
        } else {
            viewHolder.badgeText.setTextColor(Util.getStyledAttribute(context, android.R.attr.textColorPrimary));
            viewHolder.badgeContainer.setBackgroundTintList(null);
        }
    }

    private void paintBadge(ViewHolder holder) {
        ValueAnimator colorAnimation = ValueAnimator.ofObject(new ArgbEvaluator(), ColorUtils.setAlphaComponent(dotColor, 50), dotColor);
        colorAnimation.setDuration(350);
        colorAnimation.addUpdateListener(animator ->
                ViewCompat.setBackgroundTintList(holder.badgeContainer, ColorStateList.valueOf(
                        ColorUtils.setAlphaComponent((int) animator.getAnimatedValue(),
                                180))));
        colorAnimation.start();
    }

    private boolean isLastPref(int position) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getInt("FILTER_DOWNLOADS", 0) == Integer.parseInt(mValues[position]);
    }

    private void putPref(int value) {
        PreferenceManager.getDefaultSharedPreferences(context).edit().putInt("FILTER_DOWNLOADS", value).apply();
    }

    public interface SingleClickListener {
        void onDownloadBadgeClickListener();
    }

    static class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        private RelativeLayout badgeContainer;
        private View badgeDot;
        private TextView badgeText;

        public ViewHolder(View view) {
            super(view);
            badgeContainer = view.findViewById(R.id.badge_container);
            badgeDot = view.findViewById(R.id.badge_dot);
            badgeText = view.findViewById(R.id.badge_text);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            dSelected = getAdapterPosition();
            dClickListener.onDownloadBadgeClickListener();
        }
    }

}