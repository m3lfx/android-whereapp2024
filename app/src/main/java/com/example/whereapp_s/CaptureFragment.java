package com.example.whereapp_s;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.BitmapDrawable;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;

import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.squareup.picasso.Picasso;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;


public class CaptureFragment extends Fragment implements LocationListener {

    private ImageView imageView;
    // The filepath for the photo
    String currentPhotoPath;
    // A reference to our database
    private DataManager dataManager;
    private Location location = new Location("");
    private LocationManager locationManager;
    private String provider;
    // Where the captured image is stored
    private Uri imageUri = Uri.EMPTY;

    /** @noinspection deprecation*/
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        dataManager = new DataManager(getActivity().getApplicationContext());
        locationManager = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);
        Criteria criteria = new Criteria();

        provider = locationManager.getBestProvider(criteria, false);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_capture, container, false);
        imageView = view.findViewById(R.id.imageView);
        Button btnCapture = view.findViewById(R.id.btnCapture);
        Button btnSave = view.findViewById(R.id.btnSave);
        final EditText editTextTitle = view.findViewById(R.id.editTextTitle);
        final EditText editTextTag1 = view.findViewById(R.id.editTextTag1);
        final EditText editTextTag2 = view.findViewById(R.id.editTextTag2);
        final EditText editTextTag3 = view.findViewById(R.id.editTextTag3);
        ActivityResultLauncher<Uri> takeAPhoto = registerForActivityResult(
                new ActivityResultContracts.TakePicture(), result ->
                {
                    if (!result)
                        return;

//                    imageUri = result.toString();
                    Log.e("imageuri", result.toString());
                    Picasso.get().load(imageUri).centerCrop().fit().into(imageView);
                });

        btnCapture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                cameraIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                File photoFile = null;
                try {
                    photoFile = createImageFile();
                } catch (IOException ex) {
// Error occurred while creating the File
                    Log.e("error", "error creating file");
                }
// Continue only if the File was successfully created
                if (photoFile != null) {

                    imageUri = FileProvider.getUriForFile(requireContext(), "com.example.whereapp_s.fileprovider", photoFile);
                    Log.e("uri", imageUri.toString());

                    takeAPhoto.launch(imageUri);
                }
            }
        });

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (imageUri != null) {
                    if (!imageUri.equals(Uri.EMPTY)) {
// We have a photo to save
                        Photo photo = new Photo();
                        photo.setTitle(editTextTitle.getText().toString());
                        photo.setStorageLocation(imageUri);
                        photo.setGpsLocation(location);
// What is in the tags
                        String tag1 = editTextTag1.getText().toString();
                        String tag2 = editTextTag2.getText().toString();
                        String tag3 = editTextTag3.getText().toString();
// Assign the strings to the Photo object
                        photo.setTag1(tag1);
                        photo.setTag2(tag2);
                        photo.setTag3(tag3);
// Send the new object to our DataManager
                        dataManager.addPhoto(photo);
                        Toast.makeText(getActivity(), "Saved", Toast.LENGTH_LONG).
                                show();
                    } else {
// No image
                        Toast.makeText(getActivity(), "No image to save", Toast.
                                LENGTH_LONG).show();
                    }
                } else {
// Uri not initialized
                    Log.e("Error ", "uri is null");
                }
            }
        });
        return view;
    }

    private File createImageFile() throws IOException {
// Create an image file name
        String timeStamp = new SimpleDateFormat
                ("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
//        File storageDir = Environment.getExternalStoragePublicDirectory(
//                Environment.DIRECTORY_PICTURES);
        File storageDir = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        Log.e("storage", storageDir.toString());

        File image = File.createTempFile(
                imageFileName, // filename
                ".jpg", // extension
                storageDir // folder
        );
// Save for use with ACTION_VIEW Intent
        currentPhotoPath = "file:" + image.getAbsolutePath();
        return image;
    }

    @Override
    public void onLocationChanged(@NonNull Location mlocation) {

        location = mlocation;
        Log.i("location",location.toString());
    }
    @Override
    public void onProviderEnabled(@NonNull String provider) {
        LocationListener.super.onProviderEnabled(provider);
    }

    @Override
    public void onProviderDisabled(@NonNull String provider) {
        LocationListener.super.onProviderDisabled(provider);
    }

    @Override
    public void onResume() {
        super.onResume();

        if (ActivityCompat.checkSelfPermission(getContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        locationManager.requestLocationUpdates(provider, 500, 1, this);
    }
    @Override
    public void onPause() {
        super.onPause();
        locationManager.removeUpdates(this);
    }

    public void onDestroy() {
        super.onDestroy();
// Make sure we don't run out of memory
        BitmapDrawable bd = (BitmapDrawable) imageView.getDrawable();
        bd.getBitmap().recycle();
        imageView.setImageBitmap(null);
    }



}