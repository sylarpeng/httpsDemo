package vpclub.httpsdemo;

import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;


import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.AllowAllHostnameVerifier;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.CertificateFactory;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;

import okhttp3.OkHttpClient;


public class MainActivity extends AppCompatActivity {

    private OkHttpClient mOkHttpClient;
    private TextView tvMsg;
    public static final String CLIENT_KET_PASSWORD = "213679301700631"; //
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        tvMsg= (TextView) findViewById(R.id.tv_msg);
        mOkHttpClient = new OkHttpClient();

    }

    public void click(View v){
        new Thread(new Runnable() {
            @Override
            public void run() {
                starHttpsCer("https://kyfw.12306.cn/otn/");
            }
        }).start();


    }

    public void click2(View v){
        new Thread(new Runnable() {
            @Override
            public void run() {
                starHttpsPfx("https://120.25.93.63:8089/api/1.0/QRCodeServer/QRCode/GainHttpsContent");
            }
        }).start();

    }

    Handler handler=new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if(msg.obj!=null){
                tvMsg.setText(msg.obj.toString());
            }

        }
    };


    /**
     * okhttp 实现https请求
     */
//    private void starHttpsCer() {
//        try{
//            OkHttpClient.Builder builder = new OkHttpClient.Builder();
//            builder.sslSocketFactory(setCertificates(MainActivity.this.getAssets().open("srca.cer")));
//            OkHttpClient client=builder.build();
//            Request request = new Request.Builder().url("https://kyfw.12306.cn/otn/").build();
//            Response response = client.newCall(request).execute();
//            if (response.isSuccessful()) {
//                Message msg=Message.obtain();
//                msg.obj=response.body().toString();
//                handler.sendMessage(msg);
//            }
//        }catch (Exception e){
//            e.printStackTrace();
//
//        }
//    }

    /**
     * HttpsURLConnection 实现https请求
     */
    private void starHttpsCer(String urlStr) {
        HttpsURLConnection conn = null;
        try {
            URL url = new URL(urlStr);
            conn = (HttpsURLConnection) url.openConnection();
            conn.setSSLSocketFactory(setCertificates(MainActivity.this.getAssets().open("srca.cer")));
            conn.connect();
            if(conn.getResponseCode() == 200) {
                InputStream is = conn.getInputStream();
                ByteArrayOutputStream bytestream = new ByteArrayOutputStream();
                int ch;
                while ((ch = is.read()) != -1) {
                    bytestream.write(ch);
                }
                is.close();
                conn.disconnect();
                byte[] result = bytestream.toByteArray();
                Message msg=Message.obtain();
                msg.obj=new String(result);
                handler.sendMessage(msg);
            }
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    public SSLSocketFactory setCertificates(InputStream... certificates){
        try{
            CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
            KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            keyStore.load(null);
            int index = 0;
            for (InputStream certificate : certificates){
                String certificateAlias = Integer.toString(index++);
                keyStore.setCertificateEntry(certificateAlias, certificateFactory.generateCertificate(certificate));

                try{
                    if (certificate != null)
                        certificate.close();
                } catch (IOException e){
                    e.printStackTrace() ;
                }
            }

            //取得SSL的SSLContext实例
            SSLContext sslContext = SSLContext.getInstance("TLS");
            TrustManagerFactory trustManagerFactory = TrustManagerFactory.
                    getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init(keyStore);

//            //初始化keystore
//            KeyStore clientKeyStore = KeyStore.getInstance(KeyStore.getDefaultType());
//            clientKeyStore.load(getAssets().open("zhxu_client.jks"), "123456".toCharArray());
//
//            KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
//            keyManagerFactory.init(clientKeyStore, "123456".toCharArray());

            sslContext.init(null, trustManagerFactory.getTrustManagers(), new SecureRandom());
//            sslContext.init(null, null, new SecureRandom());
            return sslContext.getSocketFactory() ;


        } catch (Exception e){
            e.printStackTrace();
        }
        return null ;
    }


    public void starHttpsPfx(String url)
    {

        try
        {
            KeyStore trustStore = KeyStore.getInstance("PKCS12", "BC");
            trustStore.load(MainActivity.this.getAssets().open("213679301700631.pfx"), CLIENT_KET_PASSWORD.toCharArray());
            org.apache.http.conn.ssl.SSLSocketFactory sf = new SSLSocketFactoryEx(trustStore, CLIENT_KET_PASSWORD.toCharArray());
            sf.setHostnameVerifier(org.apache.http.conn.ssl.SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);

            HttpParams params = new BasicHttpParams();
            HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
            HttpProtocolParams.setContentCharset(params, "utf-8");

            SchemeRegistry registry = new SchemeRegistry();
            registry.register(new Scheme("http", PlainSocketFactory
                    .getSocketFactory(), 80));
            registry.register(new Scheme("https", sf, 443));

            HttpClient client = null;
            String msg = "";
            try
            {
                ClientConnectionManager ccm =
                        new ThreadSafeClientConnManager(params, registry);
                client = new DefaultHttpClient(ccm, params);
                HttpGet hg = new HttpGet(url);
                HttpResponse response = client.execute(hg);
                HttpEntity entity = response.getEntity();
                if (entity != null)
                {
                    InputStream instreams = entity.getContent();
                    msg = convertStreamToString(instreams);
                }
                Message message=Message.obtain();
                message.obj=msg;
                handler.sendMessage(message);
            }
            catch (Exception e)
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    public static String convertStreamToString(InputStream is)
    {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();

        String line = "";
        try
        {
            while ((line = reader.readLine()) != null)
            {
                sb.append(line + "\n");
            }
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        finally
        {
            try
            {
                is.close();
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
        return sb.toString();
    }


}


