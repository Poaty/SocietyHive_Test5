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
import androidx.annotation.OptIn;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * QR scanner screen.
 *
 * Requests camera permission, starts a CameraX preview, and uses ML Kit
 * to scan QR codes continuously. When a valid eventId is detected the
 * fragment navigates to EventDetailsFragment automatically.
 *
 * QR payload format: just the plain Firestore eventId string.
 */
public class QrFragment extends Fragment {

    private PreviewView previewView;
    private ExecutorService cameraExecutor;
    private boolean hasNavigated = false;

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) {
                    startCamera();
                } else {
                    Toast.makeText(requireContext(),
                            "Camera permission is required to scan QR codes.",
                            Toast.LENGTH_LONG).show();
                }
            });

    public QrFragment() {
        super(R.layout.fragment_qr);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        previewView = view.findViewById(R.id.previewView);
        cameraExecutor = Executors.newSingleThreadExecutor();

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        hasNavigated = false;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (cameraExecutor != null) cameraExecutor.shutdown();
    }

    // -------------------------------------------------------------------------
    // Camera
    // -------------------------------------------------------------------------

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> future =
                ProcessCameraProvider.getInstance(requireContext());

        future.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = future.get();
                bindCamera(cameraProvider);
            } catch (ExecutionException | InterruptedException e) {
                Toast.makeText(requireContext(),
                        "Could not start camera.", Toast.LENGTH_SHORT).show();
            }
        }, ContextCompat.getMainExecutor(requireContext()));
    }

    private void bindCamera(@NonNull ProcessCameraProvider cameraProvider) {
        Preview preview = new Preview.Builder().build();
        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        BarcodeScanner scanner = BarcodeScanning.getClient(
                new BarcodeScannerOptions.Builder()
                        .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                        .build());

        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();

        imageAnalysis.setAnalyzer(cameraExecutor, imageProxy -> analyzeImage(imageProxy, scanner));

        cameraProvider.unbindAll();
        cameraProvider.bindToLifecycle(
                getViewLifecycleOwner(),
                CameraSelector.DEFAULT_BACK_CAMERA,
                preview,
                imageAnalysis);
    }

    // -------------------------------------------------------------------------
    // Barcode analysis
    // -------------------------------------------------------------------------

    @OptIn(markerClass = ExperimentalGetImage.class)
    private void analyzeImage(@NonNull ImageProxy imageProxy, @NonNull BarcodeScanner scanner) {
        if (hasNavigated || imageProxy.getImage() == null) {
            imageProxy.close();
            return;
        }

        InputImage image = InputImage.fromMediaImage(
                imageProxy.getImage(),
                imageProxy.getImageInfo().getRotationDegrees());

        scanner.process(image)
                .addOnSuccessListener(barcodes -> {
                    for (Barcode barcode : barcodes) {
                        String value = barcode.getRawValue();
                        if (value != null && !value.isEmpty()) {
                            onQrDetected(value);
                            return;
                        }
                    }
                })
                .addOnCompleteListener(task -> imageProxy.close());
    }

    private void onQrDetected(@NonNull String eventId) {
        if (hasNavigated) return;
        hasNavigated = true;

        requireActivity().runOnUiThread(() -> {
            Bundle args = new Bundle();
            args.putString("eventId", eventId);
            NavHostFragment.findNavController(this)
                    .navigate(R.id.eventDetailsFragment, args);
        });
    }
}
