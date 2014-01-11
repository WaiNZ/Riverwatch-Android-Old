package nz.co.android.cowseye2.activity;

import java.io.IOException;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

import nz.co.android.cowseye2.R;
import nz.co.android.cowseye2.common.Constants;
import nz.co.android.cowseye2.utility.AlertBuilder;
import nz.co.android.cowseye2.utility.Utils;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.TextView;
import android.widget.Toast;

/** The activity for selecting an image for a pollution event submission
 *
 * This will allow the user to either capture a new image or select an image from the gallery
 * @author lanemitc
 *
 */
public class SelectImageActivity extends AbstractSubmissionActivity {

	private Button captureImageButton;
	private Uri cameraFileUri = null; // holds path to the image taken or retrieved

	private ImageView previewImageView;
	private TextView previewTextView;
	private Button selectImageFromGalleryButton;
	private boolean fromGallery;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.select_image_layout);
		setupUI();
		loadState(savedInstanceState);
		//starts a new submission event
		submissionEventBuilder.startNewSubmissionEvent();
	}

	/* Sets up the User Interface */
	protected void setupUI(){
		super.setupUI();
		//If we have GPS disabled then ask to activate it
		if(!myApplication.isGPSEnabled())
			AlertBuilder.buildGPSAlertMessage(SelectImageActivity.this, true).show();
		captureImageButton = (Button)findViewById(R.id.capture_image_button);
		selectImageFromGalleryButton = (Button)findViewById(R.id.select_image_from_gallery_button);

		previewImageView = (ImageView)findViewById(R.id.preview_image);
		previewTextView = (TextView)findViewById(R.id.preview_text);


		captureImageButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				startActivityForResult(new Intent(SelectImageActivity.this, TakePictureActivity.class), Constants.REQUEST_CODE_TAKE_PICTURE);
			}
		});
		selectImageFromGalleryButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				//open gallery
				//dealt with at onActivityResult()
				retrieveImageFromGallery();
			}
		});
	}
	
	@Override
	  public boolean onCreateOptionsMenu(Menu menu) {
	    MenuInflater inflater = getSupportMenuInflater(); //getMenuInflater();
	    inflater.inflate(R.menu.kill_menu, menu);
	        return true;
	  }

	@Override
	  public boolean onOptionsItemSelected(MenuItem item) {
		if(item.getItemId() == R.id.crash){
	     throw new NullPointerException();
		}
		return super.onOptionsItemSelected(item);
	     
	  }

	@Override
	protected void nextActivety() {
		//goes to the description activity
		if(cameraFileUri!=null){
			//save the image URI to the submissionEventBuilder
				submissionEventBuilder.setImagePath(cameraFileUri);
				submissionEventBuilder.setFromGallery(fromGallery);
			//start description activity
			startActivity(new Intent(SelectImageActivity.this,DescriptionActivity.class));
			//					Toast.makeText(SelectImageActivity.this, getString(R.string.saving_image), Toast.LENGTH_LONG).show();
		}
		else
			Toast.makeText(SelectImageActivity.this, getString(R.string.please_select_a_image), Toast.LENGTH_LONG).show();
	}



	/** Loads any state back in
	 *  Loads the path of the image if taken*/
	private void loadState(Bundle savedInstanceState) {
		if(savedInstanceState!=null){
			if(savedInstanceState.containsKey(Constants.IMAGE_URI_KEY)){
				cameraFileUri = Uri.parse(savedInstanceState.getString(Constants.IMAGE_URI_KEY));
				setPreviewImageOn(cameraFileUri);
			}
			fromGallery = savedInstanceState.getBoolean(Constants.FROM_GALLERY_KEY);
		}
	}

	/** Save the camera file URI if we have taken a picture */
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		if(cameraFileUri!=null && !cameraFileUri.equals(""))
			outState.putString(Constants.IMAGE_URI_KEY, cameraFileUri.toString());
		outState.putBoolean(Constants.FROM_GALLERY_KEY, fromGallery);

	}

	/** Creates an intents to open the camera application and initiates it*/
	protected void takeImageWithCamera() {
		// create Intent to take a picture and return control to the calling application
		Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
		// create a file to save the image
		cameraFileUri = Utils.getNewCameraFileUri();
		// set the image file name
		intent.putExtra(MediaStore.EXTRA_OUTPUT, cameraFileUri);
		//trigger activity
		startActivityForResult(intent, 	Constants.REQUEST_CODE_CAMERA);
	}


	/** Makes an intent to retrieve an image from the gallery */
	protected void retrieveImageFromGallery() {
		Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
		startActivityForResult(intent, Constants.REQUEST_CODE_GALLERY);
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		//Coming from capturing an image from native activity
		if (requestCode == Constants.REQUEST_CODE_TAKE_PICTURE && resultCode == Activity.RESULT_OK){
			if (data == null || data.getData() == null) {
				//Picture has been taken natively, get the path from activity
				cameraFileUri = Uri.parse(data.getStringExtra(Constants.IMAGE_URI_KEY));
				setPreviewImageOn(cameraFileUri);
				fromGallery = false;
			}
		}
		//Coming from gallery
		if (requestCode == Constants.REQUEST_CODE_GALLERY && resultCode == Activity.RESULT_OK){
			if (data != null) {
				cameraFileUri = data.getData();
				setPreviewImageOn(cameraFileUri);
				fromGallery = true;
			}
		}
		Log.i(toString(), "cameraFileUri : "+cameraFileUri);
	}

	/** Enables the preview image, first by trying to decode the URI natively into a bitmap
	 * If this fails then the image will be loaded from the uri handled by the system
	 * @param cameraFileUri - path to the image
	 */
	private void setPreviewImageOn(Uri cameraFileUri) {
		Log.d(toString(), "setPreviewImageOn: ");

		try{
			Bitmap b = Utils.getAppFriendlyBitmap(cameraFileUri, getContentResolver());
			Log.d(toString(), "bitt..  "+b);

			if(b==null)
				throw new IOException("Bitmap returned is null");
			setPreviewBitmapImageOn(b);
		}
		catch(IOException e){
			Log.e(toString(), "bitmap failed to decode : "+e);
			setPreviewURIImageOn(cameraFileUri);
		}
	}

	/** Enables the preview image
	 * @param uriToImage - URI to the image captured or selected*/
	private void setPreviewURIImageOn(Uri uriToImage) {
		if(uriToImage!=null){
			//sets preview text view to invisible
			previewTextView.setVisibility(View.INVISIBLE);
			//sets image to visible
			previewImageView.setVisibility(View.VISIBLE);
			//set background preview image to image taken
			try {
				ExifInterface exif = new ExifInterface(uriToImage.toString());
				double angle = 0.0;
				int orientation = Integer.parseInt(exif.getAttribute(ExifInterface.TAG_ORIENTATION));
				switch (orientation) {
					case ExifInterface.ORIENTATION_ROTATE_90:
						angle = 90.0;
						break;
					case ExifInterface.ORIENTATION_ROTATE_180:
						angle = 180.0;
						break;
					case ExifInterface.ORIENTATION_ROTATE_270:
						angle = 270.0;
						break;
				}

				if (orientation > 0.0) {
					Matrix matrix=new Matrix();
					previewImageView.setScaleType(ScaleType.MATRIX);   //required
	                int height = previewImageView.getHeight();
	                int width = previewImageView.getWidth();
					//matrix.postRotate((float)angle,previewImageView.getMeasuredHeight()/2,previewImageView.getMeasuredWidth()/2);
	                Log.d(toString(),String.format("previewImageView width: %d height: %d",width,height));
	                matrix.postRotate((float)angle,height/2,width/2);
					previewImageView.setImageMatrix(matrix);
				}
			} catch (IOException e) {
				Log.e(toString(), "failed to find exif image data : "+e);
			}
			previewImageView.setImageURI(uriToImage);

		}

	}

	/** Enables the preview image
	 * @param bitmap - the image bitmap*/
	private void setPreviewBitmapImageOn(Bitmap bitmap) {
		if(bitmap!=null){
			//sets preview text view to invisible
			previewTextView.setVisibility(View.INVISIBLE);
			//sets image to visible
			previewImageView.setVisibility(View.VISIBLE);
			//set background preview image to image taken
			previewImageView.setImageBitmap(bitmap);
		}

	}

	@Override
	public void onBackPressed() {
		if(cameraFileUri!=null){
			String imagePath = cameraFileUri.toString();
			//delete image
			if(imagePath!=null && !imagePath.equals("")){
				myApplication.deleteImage(imagePath);
			}
		}
		super.onBackPressed();
	}

}
