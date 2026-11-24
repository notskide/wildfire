package com.max2idea.android.limbo.files;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class WildfireRomZipImporterActivity extends Activity {
    private static final int PICK_ZIP = 6101;
    private static final int PICK_DESTINATION = 6102;
    private static final int MAX_ENTRIES = 4096;
    private static final long MAX_EXTRACTED_BYTES = 8L * 1024L * 1024L * 1024L;
    private static final String DIRECTORY_MIME = "vnd.android.document/directory";

    private Uri zipUri;
    private Uri destinationTreeUri;
    private TextView status;
    private Button zipButton;
    private Button destinationButton;
    private Button extractButton;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        buildInterface();
    }

    private void buildInterface() {
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(24, 24, 24, 24);

        TextView title = new TextView(this);
        title.setText("Import phone ROM ZIP");
        title.setTextSize(23);
        content.addView(title);

        TextView description = new TextView(this);
        description.setText("Choose a ROM ZIP and a destination folder. Wildfire Lab extracts the archive safely so you can attach compatible image files to an ARM virtual machine. ZIP packages are archives, not bootable disks by themselves.");
        description.setTextSize(15);
        description.setPadding(0, 12, 0, 20);
        content.addView(description);

        zipButton = new Button(this);
        zipButton.setText("Choose ROM ZIP");
        zipButton.setOnClickListener(v -> chooseZip());
        content.addView(zipButton);

        destinationButton = new Button(this);
        destinationButton.setText("Choose destination folder");
        destinationButton.setOnClickListener(v -> chooseDestination());
        content.addView(destinationButton);

        extractButton = new Button(this);
        extractButton.setText("Extract ZIP");
        extractButton.setEnabled(false);
        extractButton.setOnClickListener(v -> beginExtraction());
        content.addView(extractButton);

        status = new TextView(this);
        status.setText("Select a ZIP file and an output folder to begin.");
        status.setTextSize(14);
        status.setPadding(0, 20, 0, 0);
        content.addView(status);

        ScrollView scroll = new ScrollView(this);
        scroll.addView(content);
        setContentView(scroll);
    }

    private void chooseZip() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"application/zip", "application/x-zip-compressed", "application/octet-stream"});
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        startActivityForResult(intent, PICK_ZIP);
    }

    private void chooseDestination() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        startActivityForResult(intent, PICK_DESTINATION);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || data == null || data.getData() == null) return;

        Uri selected = data.getData();
        int flags = data.getFlags() & (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        try {
            getContentResolver().takePersistableUriPermission(selected, flags);
        } catch (Exception ignored) {
        }

        if (requestCode == PICK_ZIP) {
            zipUri = selected;
            zipButton.setText("ROM ZIP selected");
            updateStatus();
        } else if (requestCode == PICK_DESTINATION) {
            destinationTreeUri = selected;
            destinationButton.setText("Destination selected");
            updateStatus();
        }
        extractButton.setEnabled(zipUri != null && destinationTreeUri != null);
    }

    private void updateStatus() {
        String zip = zipUri == null ? "ZIP not selected" : "ZIP selected";
        String destination = destinationTreeUri == null ? "destination not selected" : "destination selected";
        status.setText(zip + "\n" + destination);
    }

    private void beginExtraction() {
        if (zipUri == null || destinationTreeUri == null) return;
        final Uri source = zipUri;
        final Uri destination = destinationTreeUri;
        zipButton.setEnabled(false);
        destinationButton.setEnabled(false);
        extractButton.setEnabled(false);
        status.setText("Extracting ROM archive…");

        new Thread(() -> {
            try {
                ExtractionResult result = extractArchive(source, destination);
                runOnUiThread(() -> {
                    status.setText(result.message);
                    extractButton.setText("Extraction complete");
                });
            } catch (Exception error) {
                String message = error.getMessage();
                if (message == null || message.trim().isEmpty()) message = error.getClass().getSimpleName();
                final String errorMessage = "Import failed: " + message;
                runOnUiThread(() -> {
                    status.setText(errorMessage);
                    zipButton.setEnabled(true);
                    destinationButton.setEnabled(true);
                    extractButton.setEnabled(zipUri != null && destinationTreeUri != null);
                });
            }
        }, "WildfireRomZipExtractor").start();
    }

    private ExtractionResult extractArchive(Uri source, Uri destinationTree) throws Exception {
        Uri treeRoot = DocumentsContract.buildDocumentUriUsingTree(destinationTree, DocumentsContract.getTreeDocumentId(destinationTree));
        String folderName = "Wildfire ROM " + System.currentTimeMillis();
        Uri outputRoot = DocumentsContract.createDocument(getContentResolver(), treeRoot, DIRECTORY_MIME, folderName);
        if (outputRoot == null) throw new IOException("Could not create the destination folder.");

        HashMap<String, Uri> directories = new HashMap<>();
        directories.put("", outputRoot);
        HashSet<String> paths = new HashSet<>();
        ArrayList<String> notableFiles = new ArrayList<>();
        int entries = 0;
        int files = 0;
        long totalBytes = 0;

        try (InputStream raw = getContentResolver().openInputStream(source)) {
            if (raw == null) throw new IOException("The selected ZIP could not be opened.");
            try (ZipInputStream zip = new ZipInputStream(raw)) {
                byte[] buffer = new byte[65536];
                ZipEntry entry;
                while ((entry = zip.getNextEntry()) != null) {
                    entries++;
                    if (entries > MAX_ENTRIES) throw new IOException("The ZIP contains too many entries.");
                    String safePath = normalizeEntryPath(entry.getName());
                    if (!paths.add(safePath)) throw new IOException("The ZIP contains duplicate paths: " + safePath);

                    String[] parts = safePath.split("/");
                    boolean directory = entry.isDirectory();
                    int directoryCount = directory ? parts.length : parts.length - 1;
                    Uri parent = ensureDirectories(parts, directoryCount, directories);

                    if (!directory) {
                        String fileName = parts[parts.length - 1];
                        String mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extensionOf(fileName));
                        if (mime == null) mime = "application/octet-stream";
                        Uri output = DocumentsContract.createDocument(getContentResolver(), parent, mime, fileName);
                        if (output == null) throw new IOException("Could not create extracted file: " + safePath);
                        try (OutputStream stream = getContentResolver().openOutputStream(output, "w")) {
                            if (stream == null) throw new IOException("Could not write extracted file: " + safePath);
                            int read;
                            while ((read = zip.read(buffer)) != -1) {
                                if (read == 0) continue;
                                if (totalBytes > MAX_EXTRACTED_BYTES - read) throw new IOException("The ZIP expands beyond the 8 GiB safety limit.");
                                stream.write(buffer, 0, read);
                                totalBytes += read;
                            }
                        }
                        files++;
                        String lower = fileName.toLowerCase(Locale.ROOT);
                        if (lower.equals("boot.img") || lower.equals("init_boot.img") || lower.equals("recovery.img") || lower.equals("system.img") || lower.equals("vendor.img") || lower.equals("super.img") || lower.equals("payload.bin")) {
                            if (notableFiles.size() < 8) notableFiles.add(fileName);
                        }
                    }
                    zip.closeEntry();
                }
            }
        } catch (Exception error) {
            try {
                DocumentsContract.deleteDocument(getContentResolver(), outputRoot);
            } catch (Exception ignored) {
            }
            throw error;
        }

        if (entries == 0) {
            try {
                DocumentsContract.deleteDocument(getContentResolver(), outputRoot);
            } catch (Exception ignored) {
            }
            throw new IOException("The selected file did not contain a readable ZIP archive.");
        }

        StringBuilder message = new StringBuilder();
        message.append("Extracted ").append(files).append(" files into ").append(folderName).append(".");
        if (!notableFiles.isEmpty()) {
            message.append("\n\nROM components found: ");
            for (int i = 0; i < notableFiles.size(); i++) {
                if (i > 0) message.append(", ");
                message.append(notableFiles.get(i));
            }
        }
        message.append("\n\nNext: create an ARM/ARM64 VM and attach compatible images in its disk, kernel or initrd fields. Device-specific firmware may still need hardware that generic QEMU does not emulate.");
        return new ExtractionResult(message.toString());
    }

    private Uri ensureDirectories(String[] parts, int count, Map<String, Uri> directories) throws Exception {
        String currentPath = "";
        Uri parent = directories.get("");
        for (int i = 0; i < count; i++) {
            currentPath = currentPath.isEmpty() ? parts[i] : currentPath + "/" + parts[i];
            Uri existing = directories.get(currentPath);
            if (existing == null) {
                existing = DocumentsContract.createDocument(getContentResolver(), parent, DIRECTORY_MIME, parts[i]);
                if (existing == null) throw new IOException("Could not create directory: " + currentPath);
                directories.put(currentPath, existing);
            }
            parent = existing;
        }
        return parent;
    }

    private String normalizeEntryPath(String rawName) throws IOException {
        if (rawName == null || rawName.indexOf('\0') >= 0) throw new IOException("The ZIP contains an invalid path.");
        String value = rawName.replace('\\', '/');
        if (value.startsWith("/") || value.matches("(?i)^[a-z]:.*")) throw new IOException("The ZIP contains an absolute path.");
        String[] rawParts = value.split("/");
        ArrayList<String> parts = new ArrayList<>();
        for (String part : rawParts) {
            if (part.isEmpty()) continue;
            if (part.equals(".") || part.equals("..")) throw new IOException("The ZIP contains a path-traversal entry.");
            String safe = part.replaceAll("[:*?\"<>|\\p{Cntrl}]", "_").replaceAll("[. ]+$", "");
            if (safe.isEmpty()) safe = "_";
            if (safe.length() > 240) safe = safe.substring(0, 240);
            parts.add(safe);
        }
        if (parts.isEmpty()) throw new IOException("The ZIP contains an empty path.");
        StringBuilder joined = new StringBuilder();
        for (String part : parts) {
            if (joined.length() > 0) joined.append('/');
            joined.append(part);
        }
        String result = joined.toString();
        if (result.length() > 2048) throw new IOException("The ZIP contains an excessively long path.");
        return result;
    }

    private String extensionOf(String name) {
        int index = name.lastIndexOf('.');
        if (index < 0 || index == name.length() - 1) return "";
        return name.substring(index + 1).toLowerCase(Locale.ROOT);
    }

    private static class ExtractionResult {
        final String message;

        ExtractionResult(String message) {
            this.message = message;
        }
    }
}
