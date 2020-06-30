package seven.of.hearts.profilephotorecycler.profile;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.format.Time;
import android.view.KeyEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import seven.of.hearts.profilephotorecycler.R;
import seven.of.hearts.profilephotorecycler.main.edit_profile.EditProfileActivity;
import seven.of.hearts.profilephotorecycler.main.edit_profile.choose_photo.view_holder.GalleryAdapter;
import seven.of.hearts.profilephotorecycler.main.edit_profile.choose_photo.view_holder.GalleryItem;
import seven.of.hearts.profilephotorecycler.main.edit_profile.editing_profile.EditProfileFragment_;
import seven.of.hearts.profilephotorecycler.mvp.classes.BaseFragment;
import seven.of.hearts.profilephotorecycler.views.header.Header;
import seven.of.hearts.profilephotorecycler.views.interfaces.OnHeaderButtonClick;
import seven.of.hearts.profilephotorecycler.views.square_image_view.ZoomImageView;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EFragment;
import org.androidannotations.annotations.FragmentArg;
import org.androidannotations.annotations.ViewById;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/*
 * Created by Eugene Zelikson (7ofHearts).
 */
@EFragment(R.layout.fragment_choose_photo)
public class ChoosingPhotoFragment extends BaseFragment implements GalleryAdapter.ItemClickListener, OnHeaderButtonClick {

    @FragmentArg
    protected boolean isEdit;

    @ViewById(R.id.recycle_photo)
    protected RecyclerView recyclerView;

    @ViewById(R.id.avatar)
    protected TextView avatar;

    @ViewById(R.id.header)
    protected Header header;

    @ViewById(R.id.avatar_container)
    protected FrameLayout frameLayout;

    private ArrayList<String> photos = new ArrayList<>();
    private static final int PICK_IMAGE = 1111;
    private static final int REQUEST_IMAGE_CAPTURE = 2222;
    private static final int PERMISSION_WRITE_EXTERNAL_STORAGE = 3333;
    private static final int PERMISSION_CAMERA = 4444;
    private Uri mHighQualityImageUri = null;


    @Bean
    protected ChoosingPhotoFragment presenter;

    @Bean
    protected GalleryAdapter adapter;

    @Override
    protected void initPresenter() {
        presenter.applyView(this);
    }

    @Override
    protected void start() {

    }

    @AfterViews
    public void afterViews() {
        if (checkStoragePermission()) {
            photos = new ArrayList<>(presenter.getAllShownImagesPath(getActivity()));
            Collections.reverse(photos);
            List<GalleryItem> listPhoto = new ArrayList<>();
            for (int i = 0; i < photos.size(); i++) {
                listPhoto.add(new GalleryItem(photos.get(i), false));
            }
            GridLayoutManager gLayout = new GridLayoutManager(getContext(), 3, GridLayoutManager.VERTICAL, false);
            recyclerView.setLayoutManager(gLayout);
            adapter.setItems(listPhoto);
            recyclerView.setAdapter(adapter);
            adapter.setOnItemClickListener(this);

            String avatarURL = presenter.getUserAvatar();
            if (!avatarURL.isEmpty()) {
                Glide.with(getContext()).load(avatarURL)
                        .skipMemoryCache(true)
                        .diskCacheStrategy(DiskCacheStrategy.NONE)
                        .into(avatar);
            }
        }

        header.setListener(this);
        header.addClickListenerOnButton();
        onBackClick();

    }

    @Click(R.id.browse)
    public void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE);
    }

    @Click(R.id.btn_camera)
    public void openCameraIntent() {
        if (checkCameraPermission()) {
            mHighQualityImageUri = generateTimeStampPhotoFileUri();
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, mHighQualityImageUri);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivityForResult(intent, REQUEST_IMAGE_CAPTURE);
        }
    }

    private Uri generateTimeStampPhotoFileUri() {
        Uri photoFileUri = null;
        File outputDir = getPhotoDirectory();
        if (outputDir != null) {
            Time t = new Time();
            t.setToNow();
            File photoFile = new File(outputDir, System.currentTimeMillis() + ".jpg");
            photoFileUri = Uri.fromFile(photoFile);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                photoFileUri = FileProvider.getUriForFile(getActivity(), getActivity().getPackageName() + ".provider", photoFile);
            }
        }
        return photoFileUri;
    }

    private File getPhotoDirectory() {
        File outputDir = null;
        String externalStorageState = Environment.getExternalStorageState();
        if (externalStorageState.equals(Environment.MEDIA_MOUNTED)) {
            File photoDir = Environment
                    .getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
            outputDir = new File(photoDir, getString(R.string.app_name));
            if (!outputDir.exists())
                if (!outputDir.mkdirs()) {
                    Toast.makeText(getContext(), "Failed to create directory "
                            + outputDir.getAbsolutePath(), Toast.LENGTH_LONG).show();
                    outputDir = null;
                }
        }
        return outputDir;
    }

    public boolean checkStoragePermission() {
        if (ContextCompat.checkSelfPermission(getContext(), android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSION_WRITE_EXTERNAL_STORAGE);
            return false;
        } else {
            return true;
        }
    }

    public boolean checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{android.Manifest.permission.CAMERA}, PERMISSION_CAMERA);
            return false;
        } else {
            return true;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case PERMISSION_WRITE_EXTERNAL_STORAGE:
                if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                    afterViews();
                } else {
                    Snackbar.make(recyclerView, getString(R.string.need_storage_permission), Snackbar.LENGTH_INDEFINITE).setAction(getString(R.string.ok), new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            afterViews();
                        }
                    }).setActionTextColor(ContextCompat.getColor(getContext(), R.color.button_no_active)).show();
                }
                break;
            case PERMISSION_CAMERA:
                if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                    openCameraIntent();
                }
                break;
        }
    }

    @Override
    public void onClickListener(int index) {
        Glide.with(getContext()).load("file://" + photos.get(index))
                .thumbnail(0.1f)
                .skipMemoryCache(true)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .into(avatar);
    }

    //Save chosen avatar
    @Override
    public void OnRightButtonClick(int id) {
        Bitmap viewBitmap = loadBitmapFromView(avatar);
        presenter.saveImageToLocalFolder(viewBitmap, isEdit);
    }

    //converting view to the bitmap
    private Bitmap loadBitmapFromView(View view) {
        Bitmap bitmap = Bitmap.createBitmap(view.getWidth(), view.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        view.draw(canvas);
        return bitmap;
    }

    //Open EditProfile page
    public void goToEditProfile() {
        ((EditProfileActivity) getActivity()).showFragmentWithoutBackStack(EditProfileFragment_.builder().build());
    }

    //Open CreateProfile page on Authorization Activity
    public void goToCreateProfilePage(String avatarPath) {
        ((EditProfileActivity) getActivity()).goToAuthorizationActivityWithAvatar(avatarPath);
    }

    //Open the fragment from which the transition was made
    @Override
    public void OnLeftButtonClick(int id) {
        if (isEdit) {
            ((EditProfileActivity) getActivity()).showFragmentWithoutBackStack(EditProfileFragment_.builder().build());
        } else {
            ((EditProfileActivity) getActivity()).goToAuthorizationActivity();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != Activity.RESULT_CANCELED) {
            if (requestCode == PICK_IMAGE) {
                Uri selectedImageUri = data.getData();
                Glide.with(getContext()).load(selectedImageUri)
                        .thumbnail(0.1f)
                        .skipMemoryCache(true)
                        .diskCacheStrategy(DiskCacheStrategy.NONE)
                        .into(avatar);
            }
        }

        if (requestCode == REQUEST_IMAGE_CAPTURE) {
            Glide.with(getContext()).load(mHighQualityImageUri)
                    .thumbnail(0.1f)
                    .skipMemoryCache(true)
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .into(avatar);
        }
    }
}
