package net.magmastone.inthefrige.network.tasks;

import android.app.Activity;
import android.os.AsyncTask;
import android.util.Log;

import net.magmastone.inthefrige.network.FRGItem;
import net.magmastone.inthefrige.network.NetworkInterfacer;
import net.magmastone.inthefrige.network.UPCItem;

/**
 * Created by alex on 3/4/15.
 */
public class SetFRGStatusTask extends AsyncTask<String,String,FRGItem> {

    public NetworkResults caller;
    public Exception ex;
    public SetFRGStatusTask(NetworkResults a){
        caller=a;
    }

    @Override
    protected FRGItem doInBackground(String... params) {
    try{
        NetworkInterfacer iface = new NetworkInterfacer("192.168.0.183:8080");
        FRGItem item;
        if(Integer.valueOf(params[1]).intValue() > 0) {
            item = iface.db.postFrg(params[0], "alex", params[1], params[2]);
        }else{
             item = iface.db.remItem(params[0], "alex");
        }
        Log.d("NewUPCTask", "Posted a UPC");
        return item;
    }catch (Exception e){
        ex=e;
        return null;
    }
    }

    @Override
    protected void onPostExecute(FRGItem upcItem) {
    if(upcItem != null){
        super.onPostExecute(upcItem);
        if(caller != null) {
            caller.NetworkSuccess(upcItem);
        }
    }else{
        if(caller != null) {
            caller.NetworkFailed(ex);
        }
    }
    }

    public interface NetworkResults{
        public void NetworkSuccess(FRGItem it);
        public void NetworkFailed(Exception e);
    }
}
