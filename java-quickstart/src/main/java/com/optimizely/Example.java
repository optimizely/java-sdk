package com.optimizely;

import com.optimizely.ab.Optimizely;
import com.optimizely.ab.config.Variation;
import com.optimizely.ab.config.parser.ConfigParseException;
import com.optimizely.ab.event.AsyncEventHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.util.HashMap;
import java.util.Map;

import java.util.Random;

public class Example {
    public static void main(String[] args) {

        String datafile = getDatafile();

        try {
            Optimizely optimizely = Optimizely.builder(datafile, new AsyncEventHandler(100,2)).build();
            System.out.println("hello world");
            Random random = new Random();
            String user = String.valueOf(random.nextInt());
            Map<String,String> attributes = new HashMap<>();
            attributes.put("browser_type", "chrome");
/*            Variation v = optimizely.activate("background_experiment", user, attributes);

            if (v != null) {
                optimizely.track("sample_conversion", user, null);
                System.out.println(String.format("Found variation %s", v.getKey()));
                Thread.sleep(10000);
            }
            else {
                System.out.println("didn't get a variation");
            }
*/
            if (optimizely.isFeatureEnabled("eet_feature", user, attributes)) {
                optimizely.track("eet_conversion", user, null);
                System.out.println("feature enabled");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return;
    }

    public static String getDatafile() {
        try {
            String url = "https://cdn.optimizely.com/datafiles/BX9Y3bTa4YErpHZEMpAwHm.json";
            CloseableHttpClient httpclient = HttpClients.createDefault();
            HttpGet httpget = new HttpGet(url);
            String datafile = EntityUtils.toString(
                    httpclient.execute(httpget).getEntity());
            return datafile;
        }
        catch (Exception e) {
            System.out.print(e);
            return null;
        }
    }
}
