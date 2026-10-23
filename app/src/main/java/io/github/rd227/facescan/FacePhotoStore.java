package io.github.rd227.facescan;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.AtomicFile;

import androidx.core.content.FileProvider;
import androidx.exifinterface.media.ExifInterface;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/** One private, encrypted photo. An upload copy is created only on an explicit chooser action. */
public final class FacePhotoStore {
    private static final String KEY_ALIAS = "facescan.photo.v1";
    private static final int MAGIC = 0x46535031;
    private final Context context;
    private final File photo;

    public FacePhotoStore(Context context) {
        this.context = context.getApplicationContext();
        photo = new File(this.context.getFilesDir(), "face-photo.enc");
    }

    public boolean exists() { return photo.isFile(); }

    public void save(Uri source) throws IOException, GeneralSecurityException {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        try (InputStream input = open(source)) { BitmapFactory.decodeStream(input, null, options); }
        if (options.outWidth <= 0 || options.outHeight <= 0) throw new IOException("无法读取这张图片。");
        options.inSampleSize = 1;
        while (Math.max(options.outWidth, options.outHeight) / options.inSampleSize > 1440) {
            options.inSampleSize *= 2;
        }
        options.inJustDecodeBounds = false;
        Bitmap bitmap;
        try (InputStream input = open(source)) { bitmap = BitmapFactory.decodeStream(input, null, options); }
        if (bitmap == null) throw new IOException("图片解码失败。");
        Bitmap oriented = bitmap;
        try {
            Matrix transform = new Matrix();
            try (InputStream input = open(source)) {
                int orientation = new ExifInterface(input).getAttributeInt(
                        ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
                switch (orientation) {
                    case ExifInterface.ORIENTATION_FLIP_HORIZONTAL: transform.setScale(-1, 1); break;
                    case ExifInterface.ORIENTATION_ROTATE_180: transform.setRotate(180); break;
                    case ExifInterface.ORIENTATION_FLIP_VERTICAL: transform.setScale(1, -1); break;
                    case ExifInterface.ORIENTATION_TRANSPOSE:
                        transform.setRotate(90); transform.postScale(-1, 1); break;
                    case ExifInterface.ORIENTATION_ROTATE_90: transform.setRotate(90); break;
                    case ExifInterface.ORIENTATION_TRANSVERSE:
                        transform.setRotate(-90); transform.postScale(-1, 1); break;
                    case ExifInterface.ORIENTATION_ROTATE_270: transform.setRotate(-90); break;
                    default: break;
                }
            } catch (IOException ignored) { /* PNG and other images may not contain EXIF. */ }
            if (!transform.isIdentity()) oriented = Bitmap.createBitmap(bitmap, 0, 0,
                    bitmap.getWidth(), bitmap.getHeight(), transform, true);
            byte[] jpeg;
            try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
                if (!oriented.compress(Bitmap.CompressFormat.JPEG, 90, output)) throw new IOException("图片压缩失败。");
                jpeg = output.toByteArray();
            }
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key());
            byte[] encrypted = cipher.doFinal(jpeg);
            AtomicFile file = new AtomicFile(photo);
            FileOutputStream stream = null;
            try {
                stream = file.startWrite();
                DataOutputStream output = new DataOutputStream(stream);
                output.writeInt(MAGIC);
                output.write(cipher.getIV());
                output.writeInt(encrypted.length);
                output.write(encrypted);
                output.flush();
                file.finishWrite(stream);
            } catch (IOException exception) {
                file.failWrite(stream);
                throw exception;
            }
        } finally {
            if (oriented != bitmap) oriented.recycle();
            bitmap.recycle();
        }
    }

    public Bitmap preview() throws IOException, GeneralSecurityException {
        byte[] jpeg = decrypt();
        Bitmap bitmap = BitmapFactory.decodeByteArray(jpeg, 0, jpeg.length);
        if (bitmap == null) throw new IOException("本机照片无法读取，请重新选择。");
        return bitmap;
    }

    public Uri exportForUpload() throws IOException, GeneralSecurityException {
        byte[] jpeg = decrypt();
        File folder = new File(context.getCacheDir(), "photo-share");
        if (!folder.isDirectory() && !folder.mkdirs()) throw new IOException("无法创建临时图片。");
        File copy = new File(folder, "saved-face.jpg");
        try (FileOutputStream output = new FileOutputStream(copy)) { output.write(jpeg); }
        return FileProvider.getUriForFile(context, context.getPackageName() + ".files", copy);
    }

    public void clearTemporaryFiles() {
        File folder = new File(context.getCacheDir(), "photo-share");
        File[] files = folder.listFiles();
        if (files != null) for (File file : files) if (!file.delete()) file.deleteOnExit();
    }

    public void delete() throws IOException {
        new AtomicFile(photo).delete();
        clearTemporaryFiles();
        if (photo.exists()) throw new IOException("照片删除失败，请重试。");
    }

    private InputStream open(Uri source) throws IOException {
        InputStream input = context.getContentResolver().openInputStream(source);
        if (input == null) throw new IOException("无法打开这张照片。");
        return input;
    }

    private byte[] decrypt() throws IOException, GeneralSecurityException {
        try (DataInputStream input = new DataInputStream(new AtomicFile(photo).openRead())) {
            if (input.readInt() != MAGIC) throw new IOException("本机照片格式无效。");
            byte[] iv = new byte[12];
            input.readFully(iv);
            int size = input.readInt();
            if (size < 16 || size > 8 * 1024 * 1024) throw new IOException("本机照片大小无效。");
            byte[] encrypted = new byte[size];
            input.readFully(encrypted);
            if (input.read() != -1) throw new IOException("本机照片格式无效。");
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key(), new GCMParameterSpec(128, iv));
            return cipher.doFinal(encrypted);
        }
    }

    private SecretKey key() throws GeneralSecurityException, IOException {
        KeyStore store = KeyStore.getInstance("AndroidKeyStore");
        store.load(null);
        if (store.containsAlias(KEY_ALIAS)) return (SecretKey) store.getKey(KEY_ALIAS, null);
        KeyGenerator generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
        generator.init(new KeyGenParameterSpec.Builder(KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build());
        return generator.generateKey();
    }
}
