import play.Application;
import play.GlobalSettings;
import play.Play;
import utils.S3Blob;

public class Global extends GlobalSettings {

    @Override
    public void onStart(Application application) {

        S3Blob.initialize(application);

        super.onStart(application);
    }

}
