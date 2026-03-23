package com.example.societyhive_test5;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;

/**
 * QR scanner screen.
 *
 * Uses ZXing (via zxing-android-embedded) to launch a fullscreen QR scanner.
 * On a successful scan the raw value is treated as a Firestore eventId and
 * the user is navigated to EventDetailsFragment automatically.
 *
 * QR payload format: plain Firestore eventId string.
 */
public class QrFragment extends Fragment {

    private boolean hasNavigated = false;

    private final ActivityResultLauncher<ScanOptions> barcodeLauncher =
            registerForActivityResult(new ScanContract(), result -> {
                if (result.getContents() != null) {
                    onQrDetected(result.getContents());
                } else {
                    Toast.makeText(requireContext(),
                            "Scan cancelled.", Toast.LENGTH_SHORT).show();
                }
            });

    public QrFragment() {
        super(R.layout.fragment_qr);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        launchScanner();
    }

    @Override
    public void onResume() {
        super.onResume();
        hasNavigated = false;
    }

    private void launchScanner() {
        ScanOptions options = new ScanOptions();
        options.setDesiredBarcodeFormats(ScanOptions.QR_CODE);
        options.setPrompt("Scan a SocietyHive event QR code");
        options.setBeepEnabled(false);
        options.setOrientationLocked(false);
        barcodeLauncher.launch(options);
    }

    private void onQrDetected(@NonNull String eventId) {
        if (hasNavigated) return;
        hasNavigated = true;

        Bundle args = new Bundle();
        args.putString("eventId", eventId);
        NavHostFragment.findNavController(this)
                .navigate(R.id.eventDetailsFragment, args);
    }
}
