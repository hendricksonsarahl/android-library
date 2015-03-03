/*
Copyright 2009-2015 Urban Airship Inc. All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice, this
list of conditions and the following disclaimer.

2. Redistributions in binary form must reproduce the above copyright notice,
this list of conditions and the following disclaimer in the documentation
and/or other materials provided with the distribution.

THIS SOFTWARE IS PROVIDED BY THE URBAN AIRSHIP INC ``AS IS'' AND ANY EXPRESS OR
IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO
EVENT SHALL URBAN AIRSHIP INC OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.urbanairship.push.iam;

import android.annotation.TargetApi;
import android.app.Fragment;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.widget.ViewDragHelper;
import android.support.v7.widget.CardView;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;

import com.urbanairship.Logger;
import com.urbanairship.R;
import com.urbanairship.UAirship;
import com.urbanairship.actions.ActionRunRequest;
import com.urbanairship.actions.ActionValue;
import com.urbanairship.actions.Situation;
import com.urbanairship.push.notifications.NotificationActionButton;
import com.urbanairship.push.notifications.NotificationActionButtonGroup;
import com.urbanairship.widget.SwipeDismissViewLayout;

import java.util.Map;

/**
 * A fragment that displays an in-app message.
 */
@TargetApi(Build.VERSION_CODES.HONEYCOMB_MR1)
public class InAppMessageFragment extends Fragment {

    /**
     * Default primary color for in-app messages. The value is only used if the in-app message's
     * {@link InAppMessage#getPrimaryColor()} returns null.
     */
    public static final int DEFAULT_PRIMARY_COLOR = Color.WHITE;

    /**
     * Default secondary color for in-app messages. The value is only used if the in-app message's
     * {@link InAppMessage#getSecondaryColor()} returns null.
     */
    public static final int DEFAULT_SECONDARY_COLOR = Color.DKGRAY;

    /**
     * Default duration in milliseconds. The value is only used if the in-app message's
     * {@link InAppMessage#getDuration()} returns null.
     */
    public static final long DEFAULT_DURATION = 15000;

    private static final String MESSAGE = "message";
    private static final String DISMISS_ANIMATION = "dismiss_animation";
    private static final String DISMISSED = "dismissed";

    private InAppMessage message;
    private boolean isDismissed;
    private Timer timer;

    /**
     * Creates a new InAppMessageFragment fragment.
     *
     * @param message The associated in-app message.
     * @return A new InAppMessageFragment.
     */
    public static InAppMessageFragment newInstance(InAppMessage message) {
        return newInstance(message, 0);
    }

    /**
     * Creates a new InAppMessageFragment fragment.
     *
     * @param message The associated in-app message..
     * @param dismissAnimation Resource ID of a fragment transition to run when the message is dismissed.
     * @return A new InAppMessageFragment.
     */
    public static InAppMessageFragment newInstance(InAppMessage message, int dismissAnimation) {
        Bundle args = new Bundle();
        args.putParcelable(MESSAGE, message);
        args.putInt(DISMISS_ANIMATION, dismissAnimation);

        InAppMessageFragment fragment = new InAppMessageFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstance) {
        super.onCreate(savedInstance);
        this.setRetainInstance(true);

        this.message = getArguments().getParcelable(MESSAGE);
        this.isDismissed = savedInstance != null && savedInstance.getBoolean(DISMISSED, false);

        long duration = message.getDuration() == null ? DEFAULT_DURATION : message.getDuration();
        this.timer = new Timer(duration) {
            @Override
            protected void onFinish() {
                dismiss(true);
                ResolutionEvent resolutionEvent = ResolutionEvent.createTimedOutResolutionEvent(message, timer.getRunTime());
                UAirship.shared().getAnalytics().addEvent(resolutionEvent);
            }
        };

    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(DISMISSED, isDismissed);
    }

    @Override
    public void onResume() {
        super.onResume();
        timer.start();

        UAirship.shared().getInAppMessageManager().onInAppMessageFragmentResumed(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        timer.stop();

        UAirship.shared().getInAppMessageManager().onInAppMessageFragmentPaused(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (message == null || message.getAlert() == null) {
            dismiss(false);
            UAirship.shared().getInAppMessageManager().onInAppMessageFinished(message);
            return null;
        }

        int primaryColor = message.getPrimaryColor() == null ? DEFAULT_PRIMARY_COLOR : message.getPrimaryColor();
        int secondaryColor = message.getSecondaryColor() == null ? DEFAULT_SECONDARY_COLOR : message.getSecondaryColor();

        final SwipeDismissViewLayout view = (SwipeDismissViewLayout) inflater.inflate(R.layout.ua_fragment_iam, container, false);
        view.setListener(new SwipeDismissViewLayout.Listener() {
            @Override
            public void onDismissed(View view) {
                dismiss(false);
                UAirship.shared().getInAppMessageManager().onInAppMessageFinished(message);

                ResolutionEvent resolutionEvent = ResolutionEvent.createUserDismissedResolutionEvent(message, timer.getRunTime());
                UAirship.shared().getAnalytics().addEvent(resolutionEvent);
            }

            @Override
            public void onDragStateChanged(View view, int state) {
                switch (state) {
                    case ViewDragHelper.STATE_DRAGGING:
                        timer.stop();
                        break;
                    case ViewDragHelper.STATE_IDLE:
                        timer.start();
                        break;
                }
            }
        });

        FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) view.getLayoutParams();
        layoutParams.gravity = message.getPosition() == InAppMessage.POSITION_TOP ? Gravity.TOP : Gravity.BOTTOM;
        view.setLayoutParams(layoutParams);

        final CardView cardView = (CardView) view.findViewById(R.id.in_app_message);
        cardView.setCardBackgroundColor(primaryColor);

        if (!message.getClickActionValues().isEmpty()) {
            cardView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onMessageClicked(v);
                }
            });
        } else {
            cardView.setClickable(false);
            cardView.setForeground(null);
        }

        TextView alertView = (TextView) cardView.findViewById(R.id.alert);
        alertView.setText(message.getAlert());
        alertView.setTextColor(secondaryColor);

        View buttonDivider = view.findViewById(R.id.action_divider);
        buttonDivider.setBackgroundColor(secondaryColor);

        View actionButtons = cardView.findViewById(R.id.action_buttons);
        NotificationActionButtonGroup group = UAirship.shared().getPushManager().getNotificationActionGroup(message.getButtonGroupId());
        if (group != null) {

            Resources r = getResources();
            int size = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 32, r.getDisplayMetrics());

            for (final NotificationActionButton actionButton : group.getNotificationActionButtons()) {
                Button button = (Button) inflater.inflate(R.layout.ua_iam_button, (ViewGroup) actionButtons, false);

                final Drawable drawable = getResources().getDrawable(actionButton.getIcon());
                drawable.setBounds(0, 0, size, size);
                drawable.setColorFilter(secondaryColor, PorterDuff.Mode.MULTIPLY);
                button.setCompoundDrawables(drawable, null, null, null);
                button.setText(actionButton.getLabel());
                button.setTextColor(secondaryColor);

                button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        onButtonClicked(view, actionButton);
                    }
                });

                ((ViewGroup) actionButtons).addView(button);
            }

        } else {
            actionButtons.setVisibility(View.GONE);
            buttonDivider.setVisibility(View.GONE);
        }

        ImageButton imageButton = (ImageButton) cardView.findViewById(R.id.close);
        if (imageButton != null) {
            final Drawable drawable = imageButton.getDrawable();
            if (drawable != null) {
                drawable.setColorFilter(secondaryColor, PorterDuff.Mode.MULTIPLY);
            }

            imageButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dismiss(true);
                    UAirship.shared().getInAppMessageManager().onInAppMessageFinished(message);

                    ResolutionEvent resolutionEvent = ResolutionEvent.createUserDismissedResolutionEvent(message, timer.getRunTime());
                    UAirship.shared().getAnalytics().addEvent(resolutionEvent);
                }
            });
        }

        return view;
    }

    /**
     * Dismisses the fragment.
     *
     * @param animate {@code true} if the fragment should animate out, otherwise {@code false}.
     */
    public void dismiss(boolean animate) {
        timer.stop();

        if (isDismissed) {
            return;
        }

        isDismissed = true;

        if (getActivity() != null) {
            getActivity().getFragmentManager().beginTransaction()
                         .setCustomAnimations(0, animate ? getArguments().getInt(DISMISS_ANIMATION, 0) : 0)
                         .remove(this)
                         .commit();
        }
    }

    /**
     * Checks if the fragment has been dismissed.
     *
     * @return {@code true} if the fragment is dismissed, otherwise {@code false}.
     */
    public boolean isDismissed() {
        return isDismissed;
    }

    /**
     * Gets the in-app message.
     *
     * @return The in-app message.
     */
    public InAppMessage getMessage() {
        return message;
    }

    /**
     * Gets the dismiss animation resource ID.
     *
     * @return The dismiss animation resource ID.
     */
    public int getDismissAnimation() {
        return getArguments().getInt(DISMISS_ANIMATION, 0);
    }

    /**
     * Called when the message body is clicked. Will dismiss the fragment and run
     * actions with the {@link InAppMessage#getClickActionValues()}.
     *
     * @param view The view that was clicked.
     */
    protected void onMessageClicked(View view) {
        dismiss(true);
        runActions(message.getClickActionValues());
        UAirship.shared().getInAppMessageManager().onInAppMessageFinished(message);

        ResolutionEvent resolutionEvent = ResolutionEvent.createClickedResolutionEvent(message, timer.getRunTime());
        UAirship.shared().getAnalytics().addEvent(resolutionEvent);
    }

    /**
     * Called when a message button is clicked. Will dismiss the fragment and run
     * actions for the button's ID in {@link InAppMessage#getButtonActionValues(String)}.
     *
     * @param view The view that was clicked.
     * @param actionButton The associated button.
     */
    protected void onButtonClicked(View view, NotificationActionButton actionButton) {
        Logger.info("In-app message button clicked: " + actionButton.getId());
        dismiss(true);

        runActions(message.getButtonActionValues(actionButton.getId()));
        UAirship.shared().getInAppMessageManager().onInAppMessageFinished(message);

        ResolutionEvent resolutionEvent = ResolutionEvent.createButtonClickedResolutionEvent(getActivity(), message, actionButton, timer.getRunTime());
        UAirship.shared().getAnalytics().addEvent(resolutionEvent);
    }

    /**
     * Helper method to run a map of action name to action values.
     *
     * @param actionValueMap The action value map.
     */
    private void runActions(Map<String, ActionValue> actionValueMap) {
        if (actionValueMap == null) {
            return;
        }

        for (Map.Entry<String, ActionValue> entry : actionValueMap.entrySet()) {
            ActionRunRequest.createRequest(entry.getKey())
                            .setValue(entry.getValue())
                            .setSituation(Situation.MANUAL_INVOCATION)
                            .run();
        }
    }
}
