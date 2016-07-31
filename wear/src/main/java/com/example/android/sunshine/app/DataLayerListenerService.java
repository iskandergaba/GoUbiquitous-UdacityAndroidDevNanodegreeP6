package com.example.android.sunshine.app;

import android.content.Intent;
import android.util.Log;

import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.WearableListenerService;

public class DataLayerListenerService extends WearableListenerService {

    public DataLayerListenerService() {
    }

    @Override
    public void onDataChanged(DataEventBuffer dataEventBuffer) {
        super.onDataChanged(dataEventBuffer);

        for (DataEvent event : dataEventBuffer) {
            if (event.getType() == DataEvent.TYPE_CHANGED) {
                // DataItem changed
                DataItem item = event.getDataItem();
                if (item.getUri().getPath().compareTo("/num") == 0) {
                    DataMap dataMap = DataMapItem.fromDataItem(item).getDataMap();

                    String lowTemp = dataMap.getString(getString(R.string.LOW_TEMP_KEY));
                    String hightTemp = dataMap.getString(getString(R.string.HIGH_TEMP_KEY));
                    String descr = dataMap.getString(getString(R.string.DESC_KEY));
                    int weatherID = dataMap.getInt(getString(R.string.WEATID_KEY));

                    Intent intent = new Intent();

                    intent.putExtra(getString(R.string.LOW_TEMP_KEY), lowTemp);
                    intent.putExtra(getString(R.string.HIGH_TEMP_KEY), hightTemp);
                    intent.putExtra(getString(R.string.DESC_KEY), descr);
                    intent.putExtra(getString(R.string.WEATID_KEY), weatherID);

                    intent.setAction(getString(R.string.Text_RECEIVER_ACTION));
                    sendBroadcast(intent);
                }
            }
        }

    }
}