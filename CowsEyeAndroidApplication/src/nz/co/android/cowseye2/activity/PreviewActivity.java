package nz.co.android.cowseye2.activity;

import java.io.IOException;
import java.util.List;

import nz.co.android.cowseye2.R;
import nz.co.android.cowseye2.RiverWatchApplication;
import nz.co.android.cowseye2.event.GetIncidentsEvent;
import nz.co.android.cowseye2.event.SubmissionEvent;
import nz.co.android.cowseye2.event.SubmissionEventBuilderException;
import nz.co.android.cowseye2.service.GetIncidentsAsyncTask;
import nz.co.android.cowseye2.utility.AlertBuilder;
import nz.co.android.cowseye2.utility.Utils;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.google.android.gms.maps.model.LatLng;

/** The activity for showing a preview of the pollution event
 *
 * This will allow the user to see what they have done so far and to submit a
 * pollution event to the server
 * @author Mitchell Lane (modified by Hamish Cundy, SYNERGY2, 2013)
 *
 */
public class PreviewActivity extends AbstractSubmissionActivity {

	private Button submitButton;
	private ImageView image;
	private TextView location;
	private TextView description;
	private TextView tag;
	//private TextView previewTextView;
	//	private ListView tagslist;
	private int maxLength = 1000;
	//	private List <String> imageTags;
	private ProgressDialog progressDialog;
	private Handler handler;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.preview_layout);
		handler = new Handler();
		setupUI();
	}

	/* Sets up the User Interface */
	protected void setupUI() {
		super.setupUI();
		progressDialog = new ProgressDialog(this);
		progressDialog.setCancelable(false);
		progressDialog.setTitle(getString(R.string.sending_incident_title));
		progressDialog.setMessage(getString(R.string.sending_incident_msg));
		submitButton = (Button)findViewById(R.id.submit_button);
		//sends the event to the server
		submitButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				try{
					//attempt to submit a pollution event
					submitPollutionEvent();
				}
				catch(SubmissionEventBuilderException e){
					Toast.makeText(PreviewActivity.this, "You have not succesfully given all required information", Toast.LENGTH_LONG).show();
				}
			}
		});

		image = (ImageView)findViewById(R.id.PreviewImageImage);
		//previewTextView = (TextView)findViewById(R.id.preview_text);
		setPreviewImageOn(Uri.parse(submissionEventBuilder.getImagePath().toString()));
		//		image.setOnClickListener(new Utils.StartNextActivityEventOnClickListener(this, SelectImageActivity.class));

		location = (TextView)findViewById(R.id.PreviewLocationText);
		LatLng latlng = submissionEventBuilder.getGeoCoordinates();
		if( latlng != null ){ //try and set geo coordinate location first
			double lat = latlng.latitude;
			double lon = latlng.longitude;
			location.setText(String.format("%s %.2f, %.2f", getString(R.string.geocoordinates_text), lat, lon));
		}

		else //otherwise set address
			location.setText(submissionEventBuilder.getAddress());
		//location.setText("16 Kepler Way");
		//		location.setOnClickListener(new Utils.StartNextActivityEventOnClickListener(this, RecordLocationActivity.class));

		description = (TextView)findViewById(R.id.PreviewDescriptionText);
		description.setMovementMethod(new ScrollingMovementMethod());
		String descriptionText = submissionEventBuilder.getImageDescription();
		if (descriptionText.length() > maxLength) descriptionText = descriptionText.substring(0, maxLength);
		description.setText(submissionEventBuilder.getImageDescription());

		//		description.setOnClickListener(new Utils.StartNextActivityEventOnClickListener(this, DescriptionActivity.class));

		tag = (TextView)findViewById(R.id.PreviewImageTag);

		StringBuffer st = new StringBuffer();

		for (String s: submissionEventBuilder.getImageTag()){
			if (s!=null){
				st.append(s);
				st.append(", ");
			}
		}


		String text = (String) st.toString();
		if(text.length()>0)
			text = text.substring(0, text.length()-2);
		System.out.println ("Text value is   " + text);
		String ntext = text.substring(0,text.length());
		tag.setText(ntext);


		tag.setOnClickListener(new Utils.StartNextActivityEventOnClickListener(this, DescriptionActivity.class));


	}


	@Override
	  public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		MenuItem item  = menu.findItem(R.id.nextpage);
		item.setVisible(false);
		item.setEnabled(false);
		return true;
	}

	@Override
	protected void nextActivety() {
		// TODO Auto-generated method stub

	}



	/** Enables the preview image, first by trying to decode the URI natively into a bitmap
	 * If this fails then the image will be loaded from the uri handled by the system
	 * @param cameraFileUri - path to the image
	 */
	private void setPreviewImageOn(Uri cameraFileUri) {
		//sets TextView to invisible
		image.setVisibility(View.VISIBLE);
		try{
			Bitmap b = Utils.getAppFriendlyBitmap(cameraFileUri, getContentResolver());
			if(b==null)
				throw new IOException("Bitmap returned is null");
			image.setImageBitmap(b);
		}
		catch(IOException e){
			Log.e(toString(), "bitmap failed to decode : "+e);
			image.setImageURI(cameraFileUri);
			int ih=image.getMeasuredHeight();//height of imageView
			int iw=image.getMeasuredWidth();//width of imageView
			int iH=image.getDrawable().getIntrinsicHeight();//original height of underlying image
			int iW=image.getDrawable().getIntrinsicWidth();//original width of underlying image
			Log.d(toString(), String.format("ih: %d iw:%d iH: %d iW: %d",ih,iw,iH,iW));
			//image.setImageURI(cameraFileUri);

		}
	}


	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		Uri uri = Uri.parse(submissionEventBuilder.getImagePath().toString());
		Log.d(toString(), "onResume " + uri.toString());
		//Utils.RotateImageURI(image,uri);
		setPreviewImageOn(uri);
	}

	/** Submits a pollution event to the server
	 *
	 * @throws SubmissionEventBuilderException if not enough data
	 */
	protected void submitPollutionEvent() throws SubmissionEventBuilderException{
		final SubmissionEvent currentEvent = submissionEventBuilder.build(); // - throws SubmissionEventBuilderException if not enough data
		if(myApplication.isOnline()){
			progressDialog.show();

			new Thread(new Runnable() {
				@Override
				public void run() {
					final boolean success = RiverWatchApplication.processEventResponse(currentEvent.processRaw());
					handler.post(new Runnable() {

						@Override
						public void run() {
							Log.i(toString(), "successfully processed event? : "+ success);
							//only actually remove event if successful
							if(success){
								Toast.makeText(PreviewActivity.this, getString(R.string.success_submission_msg), Toast.LENGTH_LONG).show();
								myApplication.deleteImage(currentEvent);
								new GetIncidentsAsyncTask(new GetIncidentsEvent(myApplication, 0, 50),myApplication).execute();
								// go back to starting activity
								Intent intent = new Intent(PreviewActivity.this, MainScreenActivity.class);
								//Finishes all previous activities on the activity stack
								intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP);
								startActivity(intent);
								finish();
							}
							//if unsuccessful stop event handling and move the event to the end of the queue
							else{
								Toast.makeText(PreviewActivity.this, getString(R.string.failure_submission_msg), Toast.LENGTH_LONG).show();
							}
							progressDialog.dismiss();
						}
					});
				}
			}).start();
		}else{
			AlertDialog.Builder build = new AlertDialog.Builder(this);
			build.setMessage("Could not connect to internet. Your submission will be automatically submitted when you have internet coverage.");
			build.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					new Thread(new Runnable(){
						public void run(){
							//we now want to store the submission data in the image (in case the app gets killed)
							//then poll for Internet/set listener for internet. Once submitted, delete image as above
							try {
								ExifInterface exif = new ExifInterface(currentEvent.getImagePath().getPath());
								LatLng coord = currentEvent.getGeoCoordinates();
								exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE, Double.toString(coord.latitude));
								exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE, Double.toString(coord.longitude));
								exif.setAttribute("EVENT_DESCRIPTION", currentEvent.getImageDescription());
								List<String> tags = currentEvent.getImageTag();
								int count = 1;
								for(String s:tags){
									exif.setAttribute("TAG_" + count, s);
									count++;
								}
								exif.setAttribute("TIMESTAMP", currentEvent.getTimeStamp());
								exif.saveAttributes();
							} catch (IOException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							
							
						}
					});
					finish();
				}
			});
			AlertDialog dialog = build.create();
			dialog.show();
			
			
			
			
			
			
		}
		
	}


}
