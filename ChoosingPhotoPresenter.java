package seven.of.hearts.profilephotorecycler.profile;

import android.app.Activity;
import android.graphics.Bitmap;

import seven.of.hearts.mvp.classes.BasePresenter;
import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EBean;

import java.util.ArrayList;

/*
 * Created by Eugene Zelikson (7ofHearts).
 */
@EBean
class ChoosingPhotoPresenter extends BasePresenter<ChoosingPhotoFragment> {

    @Bean
    protected ChoosingPhotoModel model;

    @Override
    protected void initModel() {
        model.applyPresenter(this);
    }

    ArrayList<String> getAllShownImagesPath (Activity activity) {
        return model.getAllShownImagesPath(activity);
    }

    String getUserAvatar () {
        return model.getUserAvatar();
    }

    void saveImageToLocalFolder(Bitmap avatar, boolean isEdit){
        model.saveImageOnLocal(avatar, isEdit);
    }

    void goToEditProfilePage(){
        getView().goToEditProfile();
    }

    void goToCreateProfilePage(String avatarPath){
        getView().goToCreateProfilePage(avatarPath);
    }

}