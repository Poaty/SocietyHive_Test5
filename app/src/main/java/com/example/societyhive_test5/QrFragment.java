package com.example.societyhive_test5;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

/**
 * Professional QR scanner landing screen.
 *
 * Current state:
 * - camera-first visual layout
 * - no extra action buttons
 * - ready for future CameraX / ML Kit or ZXing integration
 *
 * Later flow:
 * 1. start camera preview automatically
 * 2. detect QR
 * 3. parse payload
 * 4. navigate instantly to event/society/check-in destination
 */
public class QrFragment extends Fragment {

    public QrFragment() {
        super(R.layout.fragment_qr);
    }

    @Override
    public void onViewCreated(@NonNull android.view.View view, @Nullable android.os.Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // UI-first only for now.
        // Later:
        // - request camera permission
        // - start CameraX preview in preview container
        // - scan QR continuously
        // - auto navigate on successful decode
    }
}
