/* Copyright 2017 Urban Airship and Contributors */

package com.urbanairship.iam;

import android.os.Parcel;

import com.urbanairship.BaseTestCase;
import com.urbanairship.iam.banner.BannerDisplayContent;
import com.urbanairship.iam.custom.CustomDisplayContent;
import com.urbanairship.json.JsonException;
import com.urbanairship.json.JsonMap;
import com.urbanairship.json.JsonValue;

import org.junit.Before;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;

/**
 * {@link InAppMessage} tests.
 */
public class InAppMessageTest extends BaseTestCase {

    CustomDisplayContent customDisplayContent;
    BannerDisplayContent bannerDisplayContent;

    @Before
    public void setup() {
        customDisplayContent = new CustomDisplayContent(JsonValue.NULL);
        bannerDisplayContent = BannerDisplayContent.newBuilder().setAlert("oh hi").build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMissingId() {
        InAppMessage.newBuilder()
                    .setDisplayContent(customDisplayContent)
                    .build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMissingDisplayContent() {
        InAppMessage.newBuilder()
                    .setId("messageId")
                    .build();
    }

    @Test
    public void testBannerDisplayContent() throws JsonException {
        InAppMessage message = InAppMessage.newBuilder()
                                           .setDisplayContent(bannerDisplayContent)
                                           .setId("messageId")
                                           .build();

        assertEquals(InAppMessage.TYPE_BANNER, message.getType());
        assertEquals(bannerDisplayContent, message.getDisplayContent());

        verifyParcelable(message);
        verifyJsonSerialization(message);
    }

    @Test
    public void testCustomDisplayContent() throws JsonException {
        InAppMessage message = InAppMessage.newBuilder()
                                           .setDisplayContent(customDisplayContent)
                                           .setId("messageId")
                                           .build();

        assertEquals(InAppMessage.TYPE_CUSTOM, message.getType());
        assertEquals(customDisplayContent, message.getDisplayContent());

        verifyParcelable(message);
        verifyJsonSerialization(message);
    }

    @Test
    public void testFromJson() throws JsonException {
        JsonMap jsonMap = JsonMap.newBuilder()
                                      .put("display_type", "custom")
                                      .put("display", customDisplayContent.toJsonValue())
                                      .put("messageId", "message id")
                                      .build();

        InAppMessage message = InAppMessage.fromJson(jsonMap.toJsonValue());

        assertEquals("message id", message.getId());
        assertEquals(InAppMessage.TYPE_CUSTOM, message.getType());
        assertEquals(customDisplayContent, message.getDisplayContent());
    }

    @Test(expected = JsonException.class)
    public void testFromJsonInvalidDisplayType() throws JsonException {
        JsonMap jsonMap = JsonMap.newBuilder()
                                 .put("display_type", "invalid")
                                 .put("display", customDisplayContent.toJsonValue())
                                 .put("message_id", "message id")
                                 .build();

        InAppMessage.fromJson(jsonMap.toJsonValue());
    }

    private void verifyParcelable(InAppMessage message) {
        // Write the message to a parcel
        Parcel parcel = Parcel.obtain();
        message.writeToParcel(parcel, 0);

        // Reset the parcel so we can read it
        parcel.setDataPosition(0);

        // Create the message from the parcel
        InAppMessage fromParcel = InAppMessage.CREATOR.createFromParcel(parcel);

        // Validate the data
        assertEquals(message, fromParcel);
    }

    public void verifyJsonSerialization(InAppMessage message) throws JsonException {
        JsonValue jsonValue = message.toJsonValue();
        InAppMessage fromJson = InAppMessage.fromJson(jsonValue);
        assertEquals(message, fromJson);
    }
}