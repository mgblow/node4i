package org.eclipse.milo.platform.util;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.util.Properties;

public class Props {


    public static Object getProperty(String key)  {
        try {
            Properties properties;
            URL url = new URL("http://localhost/platform-config.properties");
            InputStream in = url.openStream();
            Reader reader = new InputStreamReader(in, "UTF-8"); // for example
            properties = new Properties();
            try {
                properties.load(reader);
            } finally {
                reader.close();
            }
            return properties.get(key);
        }catch (Exception e){
            return null;
        }
    }
}
