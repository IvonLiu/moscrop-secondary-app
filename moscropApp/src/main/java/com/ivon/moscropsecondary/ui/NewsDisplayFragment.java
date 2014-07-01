package com.ivon.moscropsecondary.ui;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.Fragment;
import android.text.method.ScrollingMovementMethod;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.TextView;

import com.ivon.moscropsecondary.R;
import com.ivon.moscropsecondary.util.Logger;

public class NewsDisplayFragment extends Fragment {
	
	private String url = null;
	private String htmlContent = "";
	private String title = "";
	
	
	public static NewsDisplayFragment newInstance(String url, String htmlContent, String title) {
		NewsDisplayFragment ndf = new NewsDisplayFragment();
		ndf.url = url;
		ndf.htmlContent = htmlContent;
		ndf.title = title;
		return ndf;
	}
	
	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {

		setHasOptionsMenu(true);
		
		View mContentView = inflater.inflate(R.layout.fragment_newsdisplay, container, false);
		
		TextView tv = (TextView) mContentView.findViewById(R.id.fnd_title);
		if(tv != null) {
			tv.setText(title);
		}
		
		WebView wv = (WebView) mContentView.findViewById(R.id.fnd_webview);
		if(wv != null) {
			wv.loadDataWithBaseURL(null, getHtmlData(htmlContent), "text/html", "UTF-8", null);
		}
		
	    return mContentView;
	}
	
	private String getHtmlData(String bodyHTML) {
	    String head = "<head><style>img{max-width: 90%; width:auto; height: auto;}</style></head>";
	    return "<html>" + head + "<body>" + bodyHTML + "</body></html>";
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int itemId = item.getItemId();
        if(itemId == R.id.action_openbrowser) {
        	openExternalBrowser();
        	return true;
        } else if(itemId == R.id.action_viewsource) {
        	showSource();
        	return true;
        }
        return super.onOptionsItemSelected(item);
	}
	
	private void openExternalBrowser() {
		if(url != null) {
			Uri webpage = Uri.parse(url);
		    Intent intent = new Intent(Intent.ACTION_VIEW, webpage);
		    if (intent.resolveActivity(getActivity().getPackageManager()) != null) {
		        startActivity(intent);
		    }
		}
	}
	
	private void showSource() {
		TextView tv = new TextView(getActivity());
		tv.setText(htmlContent);
		tv.setMovementMethod(new ScrollingMovementMethod());
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setPositiveButton("export", new OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				// TODO Auto-generated method stub
				exportString();
			}
			
		});
		builder.setView(tv)
			   .create()
			   .show();
	}
	
	private void exportString() {
		Logger.log("try export");
		// Create a file on external storage
		File root = Environment.getExternalStorageDirectory();
		File dir = new File(root.getAbsolutePath(), "/ivonapps");
		dir.mkdirs();
		File outFile = new File(dir, "htmlStringDump.txt");
		
		try {
			FileOutputStream fos = new FileOutputStream(outFile, true);
			
			PrintWriter pw = new PrintWriter(new BufferedWriter(
					new OutputStreamWriter(fos)));
			
			pw.println(htmlContent);
			
			pw.flush();
			pw.close();
			fos.close();
			Logger.log("exported");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}