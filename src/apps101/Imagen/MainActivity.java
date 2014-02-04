/*
Copyright (c) 2014 Lawrence Angrave

Dual licensed under Apache2.0 and MIT Open Source License (included below): 
Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.
 */
package apps101.Imagen;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.text.TextPaint;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.Toast;

public class MainActivity extends Activity {

	private static final int REQUEST_CODE = 1;
	private static final String TAG = MainActivity.class.getSimpleName();
	private Bitmap mBitmap;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.d(TAG, "onCreate!");
		
		// set contentView with the layout 
		setContentView(R.layout.activity_main);
		
		// app: when we click on a button it will display an image
		// instead of specifying onclick inside XML we'll do it programatically
		// we can make the activity implement the onClickListener or make an 
		// anonymous class
		//
	
		
		
		OnClickListener listener = new OnClickListener() {
			
			@Override
			public void onClick(View arg0) {
				// Do some Intent magic to open the Gallery?
				// lets the user choose a photo to display
				// we make a new intent
				Intent intent = new Intent(Intent.ACTION_GET_CONTENT);   //specify the action we care about
				intent.setType("image/*");                               //specify the type /* = any kind of image
				
				// we can pass in a REQUEST_CODE and get notified when child is complete
				// the REQUEST_CODE is useful to discern between which request the result if for
				// 
				// Its good practice to wrap startActivity in a try|catch block (to make sure there's
				// an activity that can respond
				// Here we use the chooser (i.e. user can choose which app to responds)
				
				startActivityForResult(Intent.createChooser(intent,"Select..."), REQUEST_CODE);
			}
		};
		findViewById(R.id.button1).setOnClickListener(listener );
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
	
	// handles the response when the user selects an image
	// 
	// The Intent data is the intent that tells us what the user picked
	//
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		
		// make sure that the request code matches the request code we sent
		// also validate the user selected a picture 
		
		if(requestCode == REQUEST_CODE && resultCode == RESULT_OK) {
			Uri uri = data.getData();  // tells us what the user picked
			Log.d(TAG,uri.toString());
			Toast.makeText(getApplicationContext(), uri.toString(), Toast.LENGTH_LONG).show();
			
			// open stream can give an exception
			// think about what we want to do in the ideal case
			// think about what we want to do in the error condition
			
			try {
				// contentResolver() understands the uri. 
				// inputStream represents a river
				// if our picture was very large we don't want the whole thing in memory
				// we process the photo chunks at a time
				
				InputStream stream = getContentResolver().openInputStream(uri);
				
				// use the BitmapFactor to convert a stream into a bitmap
				// old: BitmapFactory.decodeStream(stream) 
				// 
				// instead of decoding the stream we peek at the size
				
				BitmapFactory.Options options = new BitmapFactory.Options(); 
				
				// we set the inJustDecodeBounds property to true 
				// to have the options calculate the bitmap size
				options.inJustDecodeBounds = true; 
				
				// previously we were generating the bitmap, now we're just populating 
				// the options object
				// mBitmap = BitmapFactory.decodeStream(stream);
				
				BitmapFactory.decodeStream(stream, null, options); 
				// we now have width and height details in the in options object
				int w = options.outWidth;                       	// options divides fields into things passed in and output
				int h = options.outHeight; 
				
				Log.d(TAG, "Bitmap raw size:" + w + "+ h"); 
				
				// don't leave streams open!! consume resources
				stream.close(); 
				
				// we're going to set the image to the size of the display
				// first get size of display
				int displayW = getResources().getDisplayMetrics().widthPixels; 
				int displayH = getResources().getDisplayMetrics().heightPixels;
				
				// our plan is to open the stream a second time. on the second time
				// we'll create the bitmap with a specified size
				// what we do is we try different sizes. Until the size is less than
				// the display we keep shrinking the image in half
				
				int sample = 1;   //how many pixels we take 1 = every pixel, 2 = every 2nd pixel, 
				while( w / sample > displayW || h / sample > displayH){ 
					sample = sample*2; 
				}
				Log.d(TAG, "Sampling at " + sample); 
				options.inJustDecodeBounds  = false; 
				options.inSampleSize = sample; 
				
				stream = getContentResolver().openInputStream(uri); 
				
				// previously we had 
				// mBitmap = BitmapFactory.decodeStream(stream, null, options)
				// however this gives us an immutable bitmap
				
				Bitmap bm = BitmapFactory.decodeStream(stream,null, options); 
				stream.close(); 
				
				// note: if we do this multiple times we're no longer using the same bitmap
				//       we're storing a new bitmap into mBitmap. Therefore we want to call 
				//       mBitmap.recycle(); 
				if(mBitmap != null){ 
					mBitmap.recycle(); 
				}
				
				// when you pass in a width & height you get a mutable bitmap
				mBitmap = Bitmap.createBitmap(bm.getWidth(), bm.getHeight(), Bitmap.Config.ARGB_8888); 
				Canvas c = new Canvas(mBitmap);
				// bitmap, width, height, paint (can be null)
				c.drawBitmap(bm,0,0, null); 
				TextPaint tp = new TextPaint(); 
				tp.setTextSize(bm.getHeight()/2); 
				tp.setColor(0x800000ff);   // AARRGGBB
				
				c.drawText("Gotcha", 0, bm.getHeight()/2, tp); 
				
				bm.recycle(); //tells android it can release the system resources for the bitmap
				
				
				// still have to set image
				// note we have to cast from findViewById (it doesn't know its an ImageView)
				ImageView v = (ImageView) findViewById(R.id.imageView1);
				v.setImageBitmap(mBitmap);
			} catch (Exception e) {
				Log.e(TAG,"Decoding Bitmap",e);
			}
			
		} 
	}
	
	public void saveAndShare(View v){ 
		// the first thing we do is check that the bitmap does not point to null
		if(mBitmap == null) {
			return; // not very user friendly
		}
		// save my bitmap in local storage an phone image gallery
		// world readable publicly accessible storage
		// Class Environment. From environment we can get StoragePublicDirectory
		// we want the pictures directory
		File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES); 
		Log.d(TAG, "saveAndShare path = " +path); 
		
		// make sure that the path exists. if the directory doesn't exist its created  
		path.mkdirs();
		String filename = "Imagen_" + System.currentTimeMillis() + ".jpg"; //create unique filename
		
		File file = new File(path, filename); 
		
		// to save the bitmap we need to convert the bitmap into a standard picture format jpg,png, ...
		// FileOutputStream is a way for us to create a river by pushing bytes
		// we tell it what river to create by passing it a pointer to a file object
		FileOutputStream stream;
		try {
			stream = new FileOutputStream(file);
			mBitmap.compress(CompressFormat.JPEG, 100, stream); 
			stream.close();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			Log.e(TAG, "saveAndShare (compressing)", e);
			return; // we don't want to continue if there was an erro
		} 
		
		
		
		
		// In order for the image to appear in the gallery we have to notify the media
		// scanner
		// for the second parameter the scanner needs a Uri  
		
		Uri uri = Uri.fromFile(file); 
		Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE); 
		intent.setData(uri); 
		sendBroadcast(intent);   //we send a broadcast and android listens for it
		
		// share our file with others
		
		Intent share = new Intent(Intent.ACTION_SEND) ;
		share.setType("image/jpeg");              // we have to specify the type
		share.putExtra(Intent.EXTRA_STREAM, uri); // tell the intent where the file is
		startActivity(Intent.createChooser(share,"Share using...")); 
		
		 
		
		
	}
}
