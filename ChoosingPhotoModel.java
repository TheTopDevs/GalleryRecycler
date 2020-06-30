package seven.of.hearts.profilephotorecycler.profile;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.util.Log;

import seven.of.hearts.database.RealmUserData;
import seven.of.hearts.mvp.classes.BaseModel;
import seven.of.hearts.retrofit_util.ApiInterface;
import cseven.of.hearts.retrofit_util.response.upload_update_avatar.UploadUpdateAvatarResponse;

import org.androidannotations.annotations.EBean;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/*
 * Created by Eugene Zelikson (7ofHearts).
 */
@EBean
class ChoosingPhotoModel extends BaseModel<ChoosingPhotoFragment> {

    private String directoryPath;
    private Context context;

    ChoosingPhotoModel(Context context) {
        super(context);
        this.context = context;
        directoryPath = context.getFilesDir().getAbsolutePath() + "/avatar/";
    }

    ArrayList<String> getAllShownImagesPath(Activity activity) {
        Uri uri;
        Cursor cursor;
        int column_index_data;
        ArrayList<String> listOfAllImages = new ArrayList<>();
        String absolutePathOfImage = null;
        uri = android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI;

        String[] projection = {MediaStore.MediaColumns.DATA,
                MediaStore.Images.Media.BUCKET_DISPLAY_NAME};

        cursor = activity.getContentResolver().query(uri, projection, null,
                null, null);
        if (cursor != null) {
            column_index_data = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA);
            while (cursor.moveToNext()) {
                absolutePathOfImage = cursor.getString(column_index_data);
                listOfAllImages.add(absolutePathOfImage);
            }
            cursor.close();
        }
        return listOfAllImages;
    }

    String getUserAvatar() {
        if (!getDatabaseManager().isTableUserDataEmpty()) {
            RealmUserData userData = getDatabaseManager().getUserData();
            return userData.getUserPhoto();
        }
        return "";
    }

    //save avatar image in local hide folder
    void saveImageOnLocal(Bitmap avatarBitmap, boolean isEdit) {
        deleteAllFileInDirectory();
        File myDir = new File(directoryPath);
        if (!myDir.exists()) {
            myDir.mkdirs();
        }
        long currentTime = System.currentTimeMillis();
        String avatarName = currentTime + ".jpg";
        File avatarFile = new File(myDir, avatarName);
        if (avatarFile.exists())
            avatarFile.delete();
        try {
            FileOutputStream out = new FileOutputStream(avatarFile);
            avatarBitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
            out.flush();
            out.close();
            if (isEdit) {
                createMultiPart(avatarFile);
            } else {
                getPresenter().goToCreateProfilePage(avatarFile.getAbsolutePath());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //create multipart for send to the server
    private void createMultiPart(File avatarFile) {
        RequestBody rbImage = RequestBody.create(MediaType.parse("multipart/form-data"), avatarFile);
        MultipartBody.Part imagePart = MultipartBody.Part.createFormData("photo", avatarFile.getName(), rbImage);
        sendAvatar(imagePart);
    }

    //request for send avatar to the server
    private void sendAvatar(MultipartBody.Part avatar) {
        ApiInterface apiService = getRetrofit().create(ApiInterface.class);
        Call<UploadUpdateAvatarResponse> call = apiService.uploadUpdateAvatar(getToken(), avatar);
        call.enqueue(new Callback<UploadUpdateAvatarResponse>() {
            @Override
            public void onResponse(@NonNull Call<UploadUpdateAvatarResponse> call, @NonNull Response<UploadUpdateAvatarResponse> response) {
                UploadUpdateAvatarResponse responded = response.body();
                if (responded != null){
                    if (responded.status == 200){
                        getDatabaseManager().updateAvatarInTableUserData(responded.photo);
                        getPresenter().showToast(responded.message);
                        deleteAllFileInDirectory();
                        getPresenter().goToEditProfilePage();
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<UploadUpdateAvatarResponse> call, @NonNull Throwable t) {
                deleteAllFileInDirectory();
                t.printStackTrace();
            }
        });
    }

    //delete all temporary file
    private void deleteAllFileInDirectory() {
        File dir = new File(directoryPath);
        if (dir.isDirectory()) {
            String[] children = dir.list();
            for (String aChildren : children) {
                new File(dir, aChildren).delete();
            }
        }
    }
}
