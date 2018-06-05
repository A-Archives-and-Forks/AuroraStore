package com.dragons.aurora.fragment;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.dragons.aurora.CircleTransform;
import com.dragons.aurora.PlayStoreApiAuthenticator;
import com.dragons.aurora.R;
import com.dragons.aurora.activities.LoginActivity;
import com.dragons.aurora.task.UserProvidedCredentialsTask;
import com.github.florent37.shapeofview.shapes.CircleView;
import com.percolate.caffeine.ViewUtils;
import com.squareup.picasso.Picasso;

public class AccountsFragment extends UtilFragment {

    private boolean isSecAvailable;
    private String myEmail;
    private View v;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setRetainInstance(true);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (v != null) {
            if ((ViewGroup) v.getParent() != null)
                ((ViewGroup) v.getParent()).removeView(v);
            return v;
        }

        v = inflater.inflate(R.layout.app_acc_inc, container, false);

        ImageView toolbar_back = v.findViewById(R.id.toolbar_back);
        toolbar_back.setOnClickListener(click -> getActivity().onBackPressed());

        myEmail = PreferenceFragment.getString(getActivity(), PlayStoreApiAuthenticator.PREFERENCE_EMAIL);
        isSecAvailable = PreferenceFragment.getBoolean(getActivity(), "SEC_ACCOUNT");

        if (isLoggedIn() && isGoogle())
            drawGoogle();
        else if (isLoggedIn() && isDummy())
            drawDummy();
        return v;
    }

    private void drawDummy() {
        show(v, R.id.dummyIndicator);
        setAvatar(R.drawable.ic_dummy_avatar);
        setText(v, R.id.account_name, R.string.acc_dummy_name);
        setText(v, R.id.account_email, myEmail);
        setText(v, R.id.account_gsf, R.string.device_gsfID, PreferenceFragment.getString(getActivity(),
                PlayStoreApiAuthenticator.PREFERENCE_GSF_ID));
        if (isSecAvailable)
            drawEmptyGoogle();
        else
            drawEmpty();

        drawDummyButtons();
    }

    private void drawGoogle() {
        drawEmptyDummy();

        hide(v, R.id.emptyCard);
        show(v, R.id.googleCard);
        show(v, R.id.googleIndicator);

        setText(v, R.id.account_nameG, PreferenceFragment.getString(getActivity(), "GOOGLE_NAME"));
        setText(v, R.id.account_emailG, myEmail);
        setText(v, R.id.account_gsf, R.string.device_gsfID, PreferenceFragment.getString(getActivity(),
                PlayStoreApiAuthenticator.PREFERENCE_GSF_ID));

        TextView switchGoogle = ViewUtils.findViewById(v, R.id.btn_switchG);
        switchGoogle.setOnClickListener(view -> switchGoogle());

        if (isConnected(getActivity()))
            loadAvatar(PreferenceFragment.getString(getActivity(), "GOOGLE_URL"));

        drawGoogleButtons();
    }

    private void drawEmptyDummy() {
        show(v, R.id.dummy_tapToSwitch);
        setText(v, R.id.account_name, R.string.acc_dummy_name);
        setText(v, R.id.account_email, R.string.account_dummy_email);
        LinearLayout dummyCard = ViewUtils.findViewById(v, R.id.dummyLayout);
        dummyCard.setOnClickListener(v -> loginWithDummy());
    }

    private void drawEmptyGoogle() {
        LinearLayout googleCard = ViewUtils.findViewById(v, R.id.googleLayout);
        TextView removeAccount = ViewUtils.findViewById(v, R.id.btn_remove);
        show(v, R.id.googleCard);
        show(v, R.id.btn_remove);
        show(v, R.id.google_tapToSwitch);
        setText(v, R.id.account_nameG, PreferenceFragment.getString(getActivity(), "GOOGLE_NAME"));
        setText(v, R.id.account_emailG, PreferenceFragment.getString(getActivity(), "GOOGLE_EMAIL"));
        googleCard.setOnClickListener(click -> new UserProvidedCredentialsTask(getContext()).withSavedGoogle());
        removeAccount.setOnClickListener(click -> {
            new UserProvidedCredentialsTask(getContext()).removeGooglePrefs();
            hide(v, R.id.googleCard);
            show(v, R.id.emptyCard);
        });
    }

    private void drawEmpty() {
        show(v, R.id.emptyCard);
        CircleView add_account = v.findViewById(R.id.add_account);
        add_account.setOnClickListener(v -> switchGoogle());
    }

    private void drawDummyButtons() {
        TextView logout = ViewUtils.findViewById(v, R.id.btn_logout);
        TextView switchDummy = ViewUtils.findViewById(v, R.id.btn_switch);
        TextView refreshToken = ViewUtils.findViewById(v, R.id.btn_refresh);

        if (isDummy()) {
            show(v, R.id.btn_logout);
            show(v, R.id.btn_switch);
            show(v, R.id.btn_refresh);
        }

        logout.setOnClickListener(view -> showLogOutDialog());
        switchDummy.setOnClickListener(view -> switchDummy());
        refreshToken.setOnClickListener(view -> refreshMyToken());
    }

    private void drawGoogleButtons() {
        TextView logout = ViewUtils.findViewById(v, R.id.btn_logoutG);
        TextView switchDummy = ViewUtils.findViewById(v, R.id.btn_switchG);

        if (isGoogle()) {
            show(v, R.id.btn_logoutG);
            show(v, R.id.btn_switchG);
        }

        logout.setOnClickListener(view -> showLogOutDialog());
        switchDummy.setOnClickListener(view -> switchGoogle());
    }

    private void setAvatar(int avatar) {
        ImageView avatar_view = v.findViewById(R.id.accounts_Avatar);
        avatar_view.setImageResource(avatar);
    }

    private void loadAvatar(String Url) {
        Picasso.with(getActivity())
                .load(Url)
                .placeholder(R.drawable.ic_dummy_avatar)
                .transform(new CircleTransform())
                .into(((ImageView) v.findViewById(R.id.accounts_AvatarG)));
    }

    private void showLogOutDialog() {
        new AlertDialog.Builder(getActivity())
                .setMessage(R.string.dialog_message_logout)
                .setTitle(R.string.dialog_title_logout)
                .setPositiveButton(android.R.string.yes, (dialogInterface, i) -> {
                    checkOut();
                    new PlayStoreApiAuthenticator(getContext()).logout();
                    dialogInterface.dismiss();
                    getActivity().finish();
                    startActivity(new Intent(getContext(), LoginActivity.class));
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }
}