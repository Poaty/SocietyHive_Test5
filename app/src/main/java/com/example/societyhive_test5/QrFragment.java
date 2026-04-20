package com.example.societyhive_test5;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.google.zxing.BarcodeFormat;
import com.journeyapps.barcodescanner.BarcodeCallback;
import com.journeyapps.barcodescanner.BarcodeView;
import com.journeyapps.barcodescanner.DefaultDecoderFactory;

import java.util.Collections;

public class QrFragment extends Fragment {

    private BarcodeView barcodeView;
    private boolean hasNavigated = false;

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) {
                    startScanning();
                } else {
                    Toast.makeText(requireActivity(),
                            "need camera permission to scan QR codes",
                            Toast.LENGTH_LONG).show();
                }
            });

    private final BarcodeCallback barcodeCallback = result -> {
        if (result != null && result.getText() != null) {
            onQrDetected(result.getText());
        }
    };

    public QrFragment() {
        super(R.layout.fragment_qr);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        barcodeView = view.findViewById(R.id.barcodeView);
        barcodeView.setDecoderFactory(
                new DefaultDecoderFactory(Collections.singletonList(BarcodeFormat.QR_CODE)));

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            startScanning();
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        hasNavigated = false;
        barcodeView.resume();
    }

    @Override
    public void onPause() {
        super.onPause();
        barcodeView.pause();
    }

    private void startScanning() {
        barcodeView.resume();
        barcodeView.decodeContinuous(barcodeCallback);
    }

    private void onQrDetected(String eventId) {
        // hasNavigated stops the callback firing twice before the fragment is replaced
        if (hasNavigated) return;
        hasNavigated = true;

        Bundle args = new Bundle();
        args.putString("eventId", eventId);
        NavHostFragment.findNavController(this)
                .navigate(R.id.eventDetailsFragment, args);
    }
}
