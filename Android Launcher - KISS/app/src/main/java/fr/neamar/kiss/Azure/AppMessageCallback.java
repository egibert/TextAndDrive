/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.neamar.kiss.Azure;

import com.microsoft.azure.sdk.iot.device.IotHubMessageResult;
import com.microsoft.azure.sdk.iot.device.Message;
import com.microsoft.azure.sdk.iot.device.MessageCallback;

import org.json.JSONException;
import org.json.JSONObject;

/**
 *
 * @author eduard
 */
public class AppMessageCallback implements MessageCallback {
    public IotHubMessageResult execute(Message msg, Object context) {
        System.out.println("Received message from hub: " + new String(msg.getBytes(), Message.DEFAULT_IOTHUB_MESSAGE_CHARSET));
        try {
            JSONObject object = new JSONObject(new String(msg.getBytes()));

        } catch (JSONException e) {
            e.printStackTrace();
        }

        return IotHubMessageResult.COMPLETE;
    }
}
