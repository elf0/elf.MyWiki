//License: Public Domain
//Author: elf
//EMail: elf198012@gmail.com

package elf.mywiki;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.cookie.DateParseException;
import org.apache.http.impl.cookie.DateUtils;
import org.apache.http.util.EntityUtils;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.View;
import android.view.View.OnKeyListener;
import android.view.Window;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.loopj.android.http.*;

public class MainActivity extends Activity {
	final String _strUrlRoot = "http://en.m.wikipedia.org/wiki/";
	final String _strCache = Environment.getExternalStorageDirectory().getPath() + File.separator + "MyWiki" + File.separator + "Cache";
	WebView _view;
	
	boolean PrepareDir(){
	 File fCacheDir = new File(Environment.getExternalStorageDirectory().getPath(), "MyWiki" + File.separator + "Cache");
	 if(fCacheDir.exists())
	  return true;
		
     return fCacheDir.mkdirs();
	}

	void LoadHtml(String strUrl, String strHtml){
 		_view.loadDataWithBaseURL(strUrl, strHtml, "text/html", "utf-8", strUrl);
	}
/*
    
//	final String _strTimeFormat = "EEE, d MMM yyyy HH:mm:ss 'GMT'";
    int Download(String strUrl, String strFile, boolean bIfModified){
        HttpClient httpClient = new DefaultHttpClient();
        HttpGet get = new HttpGet(strUrl);

    	File file = new File(strFile);
        if(file.exists() && bIfModified){
        	String strLastModifiedTime = DateUtils.formatDate(new Date(file.lastModified()));
//    		strLastModifiedTime = DateUtils.formatDate(new Date(file.lastModified()), _strTimeFormat);
//			if(strLastModifiedTime != null)
    		get.setHeader("If-Modified-Since", strLastModifiedTime);
        }

        try{
			HttpResponse response = httpClient.execute(get);
			StatusLine statusLine = response.getStatusLine();
			int statusCode = statusLine.getStatusCode();
			if(statusCode == 200){
				HttpEntity entity = response.getEntity();
				InputStream stream = entity.getContent();
				byte buffer[] = new byte[64 * 1024];
				BufferedOutputStream bosStreamOut = new BufferedOutputStream(new FileOutputStream(file));
				int nTotal = 0;
				int nBytes;
				while((nBytes = stream.read(buffer)) > 0){
		    		bosStreamOut.write(buffer, 0, nBytes);
		    		nTotal += nBytes;
			    }
				bosStreamOut.close();
				stream.close();
				
				Header hLastModified = response.getFirstHeader("Last-Modified");
				if(hLastModified != null){
					String strLastModifiedTime = hLastModified.getValue();
					Date dtLastModified = DateUtils.parseDate(strLastModifiedTime);
					file.setLastModified(dtLastModified.getTime());
				}

				Header hContentLength = response.getFirstHeader("Content-Length");
				if(hContentLength != null){
					String strContentLength = hContentLength.getValue();
					if(nTotal != Integer.parseInt(strContentLength))
						return -1;
				}
				return nTotal;
			}
			else if(statusCode != 304)
				return 0;
		}catch (Exception e){//ClientProtocolException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        //catch (IOException e) {
			// TODO Auto-generated catch block
		//	e.printStackTrace();
		//	return false;
		//}catch (DateParseException e) {
			// TODO Auto-generated catch block
		//	e.printStackTrace();
		//	return false;
		//}
		return -1;
    }
    */
	
    void DownloadAsync(final String strKey){
        final String strUrl = _strUrlRoot + strKey;
		String strFile = strKey.toLowerCase() + ".html";
		final String strPathName = _strCache + File.separator + strFile;
    	File file = new File(strPathName);
    	long nLastModifiedTime = file.lastModified();
        if(nLastModifiedTime > 0){
    		long nNow = System.currentTimeMillis();
    		if((nNow - nLastModifiedTime) < 7*24*3600*1000){
    			this.setTitle(strKey);
    	 		String strHtml = MainActivity.this.Load(file);
    	 		MainActivity.this.LoadHtml(strUrl, strHtml);
    			return;
    		}
    		 
        }
        
		this.setTitle("Downloading '" + strKey + "'");
    	AsyncHttpClient client = new AsyncHttpClient();
        if(nLastModifiedTime > 0){
        	String strLastModifiedTime = DateUtils.formatDate(new Date(nLastModifiedTime));
//    		strLastModifiedTime = DateUtils.formatDate(new Date(file.lastModified()), _strTimeFormat);
        	client.addHeader("If-Modified-Since", strLastModifiedTime);
        }

    	client.get(strUrl, new FileAsyncHttpResponseHandler(file){
    	    @Override
    	    public void onSuccess(int statusCode, Header[] headers, File file){
//    	    	if(statusCode != 200)
//    	    		return;
                for (Header h: headers) {
                	if(h.getName().compareTo("Last-Modified") == 0){
    					String strLastModifiedTime = h.getValue();
						try{
							Date dtLastModified = DateUtils.parseDate(strLastModifiedTime);
	    					file.setLastModified(dtLastModified.getTime());
						}
						catch (DateParseException e){
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						break;
                	}
                }

    	 		String strHtml = MainActivity.this.Load(file);
    	 		if(strHtml == null)
    	 			strHtml = "<html><head><title>Error</title></head><body>Load page failed! Check your Network and SDCard.</body></html>";
   	 			MainActivity.this.setTitle("*" + strKey + "*");
    	 		MainActivity.this.LoadHtml(strUrl, strHtml);

    	    }

    	    @Override
    	    public void onFailure(int statusCode, Header[] headers, Throwable error, File file)
    	    {
    	 		String strHtml = MainActivity.this.Load(file);
    	 		if(strHtml == null)
    	 			strHtml = "<html><head><title>Error</title></head><body>Load page failed! Check your Network and SDCard.</body></html>";
   	 			MainActivity.this.setTitle(strKey);
    	 		MainActivity.this.LoadHtml(strUrl, strHtml);

    	    	if(statusCode != 304){
    	    		file.delete();
    	    	}
  	        }
    	    
    	    @Override
    	     public void onProgress(int bytesWritten, int totalSize) {
    	    	MainActivity.this.setProgress(bytesWritten*10000/totalSize);
    	     }

//    	     @Override
//    	     public void onFinish(){
//    	 		String strHtml = MainActivity.this.Load(file);
//    	 		if(strHtml == null)
//    	 			strHtml = "<html><head><title>Error</title></head><body>Load page failed! Check your Network and SDCard.</body></html>";
//   	 			MainActivity.this.setTitle(_bUpdated? "*" + strKey + "*" : strKey);
//    	 		MainActivity.this.LoadHtml(strUrl, strHtml);
//    	     }
    	});


    }

    String Load(File file){
    	String strData = null;
      	 try{
      		BufferedInputStream buf = new BufferedInputStream(new FileInputStream(file));
      		byte[] buffer = new byte[buf.available()];
      	 	buf.read(buffer);
      	 	buf.close();
      	 	strData = new String(buffer);
      	 }
		 catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		 }
      	 return strData;
    }
    
/*    boolean Save(String strPathFile, String strData){
    	   try{
    		   FileOutputStream outputStream = new FileOutputStream(strPathFile);
    		   outputStream.write(strData.getBytes());
    		   outputStream.close();
    		   return true;
    	   } catch (Exception e) {
    	     e.printStackTrace();
    	     return false;
    	   }
    }
    */
    
    @Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.requestWindowFeature(Window.FEATURE_PROGRESS);
		setContentView(R.layout.activity_main);

	
		_view = (WebView)findViewById(R.id.webView1);
//        mainWebView.addJavascriptInterface(new JavaScript_Output(), "JavaScript_Output");

        WebSettings webSettings = _view.getSettings();
        webSettings.setUseWideViewPort(true);
        webSettings.setLoadWithOverviewMode(true);
        
        webSettings.setSupportZoom(true);
        webSettings.setBuiltInZoomControls(true);
//        webSettings.setJavaScriptEnabled(true);
        
        webSettings.setAppCacheMaxSize(256*1024*1024);
        //String appCacheDir = this.getApplicationContext().getDir("cache", Context.MODE_PRIVATE).getPath();
        PrepareDir();
        webSettings.setAppCachePath(Environment.getExternalStorageDirectory().getPath()+ "/MyWiki/Cache");//appCacheDir);
        webSettings.setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
        webSettings.setAppCacheEnabled(true);
        //webSettings.setDomStorageEnabled(true);
        //webSettings.setAllowFileAccess(true);
        
        _view.setWebChromeClient(new WebChromeClient(){  
         public void onProgressChanged(WebView view, int progress){
          MainActivity.this.setProgress(progress * 100);  
         }  
        });

        _view.setWebViewClient(new WebViewClient(){
//        	@Override
//        	public void onPageFinished(WebView view, String url){
//        		super.onPageFinished(view, url);
//        		view.loadUrl("javascript:(function(){JavaScript_Output.showSource('<html>'+document.getElementsByTagName('html')[0].innerHTML+'</html>'); })();");
                //view.loadUrl("javascript:window.JavaScript_Output.showSource('<html>'+"
                //        + "document.getElementsByTagName('html')[0].innerHTML+'</html>');");
//            }

        	@Override
            public boolean shouldOverrideUrlLoading(WebView view, String strUrl){
        		String strDecodedUrl = Uri.decode(strUrl);
        		int nIndex = strDecodedUrl.indexOf(".wikipedia.org/wiki/");
        		if(nIndex > 0){
        			nIndex += 20;
        			if(nIndex < strDecodedUrl.length()){
        				String strKey = strDecodedUrl.substring(nIndex);
        				DownloadAsync(strKey);
        			}
        		}
                return false;
            }
        	
        	@Override
        	public void onReceivedError(WebView view, int errorCode, String description, String failingUrl){
        		Toast.makeText(getApplicationContext(), "Error " + description, Toast.LENGTH_SHORT).show();
        	   }
        });

        _view.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
        
        final EditText etUrl = (EditText)findViewById(R.id.editText1);
		
		etUrl.setOnKeyListener(new OnKeyListener(){
            @Override  
            public boolean onKey(View v, int keyCode, KeyEvent event){
                if (KeyEvent.KEYCODE_ENTER != keyCode || event.getAction() != KeyEvent.ACTION_DOWN)
                    return false;

                DownloadAsync(etUrl.getText().toString().trim().replace(' ', '_'));
                    return true;
            }
        });
		
		//button
	    Button button1 = (Button)findViewById(R.id.button1); 
		button1.setOnClickListener(new Button.OnClickListener(){ 
            public void onClick(View v)
            {
                DownloadAsync(etUrl.getText().toString().trim().replace(' ', '_'));
            }
        });
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
/*
	final class JavaScript_Output{
        public void showSource(String strHtml){
        	if(!_bLoadFromFile){
              String strFile = _strKey.toLowerCase() + ".html";
              Save(_strCache + File.separator + strFile, strHtml);
        	}
        }  
    }
//*/  
}
