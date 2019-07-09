package org.smartregister.child;

import id.zelory.compressor.Compressor;
import org.smartregister.Context;
import org.smartregister.CoreLibrary;
import org.smartregister.child.domain.ChildMetadata;
import org.smartregister.child.util.AppProperties;
import org.smartregister.child.util.Utils;
import org.smartregister.repository.EventClientRepository;
import org.smartregister.repository.Repository;
import org.smartregister.repository.UniqueIdRepository;
import org.smartregister.sync.ClientProcessorForJava;
import org.smartregister.sync.helper.ECSyncHelper;
import org.smartregister.view.LocationPickerView;

/**
 * Created by ndegwamartin on 25/02/2019.
 */
public class ChildLibrary {
    private static ChildLibrary instance;

    private final Context context;
    private final Repository repository;
    private final ChildMetadata metadata;

    private int applicationVersion;
    private int databaseVersion;

    private UniqueIdRepository uniqueIdRepository;
    private EventClientRepository eventClientRepository;
    private ECSyncHelper syncHelper;

    private ClientProcessorForJava clientProcessorForJava;
    private Compressor compressor;
    private LocationPickerView locationPickerView;
    private AppProperties properties;

    private ChildLibrary(Context contextArg, Repository repositoryArg, ChildMetadata metadataArg, int applicationVersion,
                         int databaseVersion) {
        this.context = contextArg;
        repository = repositoryArg;
        this.metadata = metadataArg;
        this.applicationVersion = applicationVersion;
        this.databaseVersion = databaseVersion;
        this.properties = Utils.getProperties(this.context.applicationContext());
    }

    public static void init(Context context, Repository repository, ChildMetadata metadataArg, int applicationVersion,
                            int databaseVersion) {
        if (instance == null) {
            instance = new ChildLibrary(context, repository, metadataArg, applicationVersion, databaseVersion);
        }
    }

    public static ChildLibrary getInstance() {
        if (instance == null) {
            throw new IllegalStateException(" Instance does not exist!!! Call " + CoreLibrary.class.getName() +
                    ".init method in the onCreate method of " + "your Application class ");
        }
        return instance;
    }

    public ChildMetadata metadata() {
        return metadata;
    }

    public int getApplicationVersion() {
        return applicationVersion;
    }

    public int getDatabaseVersion() {
        return databaseVersion;
    }

    public UniqueIdRepository getUniqueIdRepository() {
        if (uniqueIdRepository == null) {
            uniqueIdRepository = new UniqueIdRepository(getRepository());
        }
        return uniqueIdRepository;
    }

    public Repository getRepository() {
        return repository;
    }

    public EventClientRepository eventClientRepository() {
        if (eventClientRepository == null) {
            eventClientRepository = new EventClientRepository(getRepository());
        }
        return eventClientRepository;
    }

    public ECSyncHelper getEcSyncHelper() {
        if (syncHelper == null) {
            syncHelper = ECSyncHelper.getInstance(context().applicationContext());
        }
        return syncHelper;
    }

    public Context context() {
        return context;
    }

    public ClientProcessorForJava getClientProcessorForJava() {
        if (clientProcessorForJava == null) {
            clientProcessorForJava = ClientProcessorForJava.getInstance(context().applicationContext());
        }
        return clientProcessorForJava;
    }

    public void setClientProcessorForJava(ClientProcessorForJava clientProcessorForJava) {
        this.clientProcessorForJava = clientProcessorForJava;
    }

    public Compressor getCompressor() {
        if (compressor == null) {
            compressor = Compressor.getDefault(context().applicationContext());
        }
        return compressor;
    }

    public LocationPickerView getLocationPickerView(android.content.Context context) {
        if (locationPickerView == null) {
            locationPickerView = new LocationPickerView(context);
            locationPickerView.init();
        }
        return locationPickerView;
    }

    public AppProperties getProperties() {
        return properties;
    }

    public void setProperties(AppProperties properties) {
        this.properties = properties;
    }

}
