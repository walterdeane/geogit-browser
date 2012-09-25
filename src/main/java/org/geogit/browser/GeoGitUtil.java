package org.geogit.browser;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geogit.api.ObjectId;
import org.geogit.api.Ref;
import org.geogit.api.RevObject.TYPE;
import org.geogit.api.RevTree;
import org.geogit.repository.Repository;
import org.geogit.storage.ObjectDatabase;
import org.geogit.storage.ObjectWriter;
import org.geogit.storage.RefDatabase;
import org.geogit.storage.WrappedSerialisingFactory;
import org.geogit.storage.bdbje.EntityStoreConfig;
import org.geogit.storage.bdbje.EnvironmentBuilder;
import org.geogit.storage.bdbje.JERepositoryDatabase;
import org.geotools.data.geogit.GeoGitDataStore;
import org.opengis.feature.type.Name;

import com.sleepycat.je.Environment;

/**
 * 
 */

/**
 * @author wdeane
 *
 */
public class GeoGitUtil {

	//private String baseEnvHome = "C:\\site_data_dir\\data";
//	public String getBaseEnvHome() {
//		return baseEnvHome;
//	}
//
//	public void setBaseEnvHome(String baseEnvHome) {
//		this.baseEnvHome = baseEnvHome;
//	}

	public String getRepoHome() {
		return repoHome;
	}

	public void setRepoHome(String repoHome) {
		this.repoHome = repoHome;
	}

	public String getIndexHome() {
		return indexHome;
	}

	public void setIndexHome(String indexHome) {
		this.indexHome = indexHome;
	}

	private String repoHome = "repository";
	private String indexHome = "index";
	public static final String TYPE_NAMES_REF_TREE = "typeNames";
    public static final Logger LOGGER = org.geotools.util.logging.Logging
            .getLogger("qpws.parkinfo.admin.dao.AbstractAdminOnlineTest");

	
	protected Repository getRepository(File geogitDirectory) throws FileNotFoundException {
		if (!geogitDirectory.exists())throw new FileNotFoundException("Geogit repository not found. Path: \"" + geogitDirectory.getAbsolutePath() + "\" could not be found.");

        final File defRepositoryHome = new File(geogitDirectory, this.repoHome);
        final File defIndexHome = new File(geogitDirectory, this.indexHome);
        defRepositoryHome.mkdirs();
        defIndexHome.mkdirs();

        EntityStoreConfig defConfig = new EntityStoreConfig();
        defConfig.setCacheMemoryPercentAllowed(50);
        EnvironmentBuilder esb2 = new EnvironmentBuilder(defConfig);
        Properties bdbEnvProperties2 = null;
        Environment environment2;
        environment2 = esb2.buildEnvironment(defRepositoryHome, bdbEnvProperties2);

        Environment stagingEnvironment2;
        stagingEnvironment2 = esb2.buildEnvironment(defIndexHome, bdbEnvProperties2);

        JERepositoryDatabase defaultRepositoryDatabase = new JERepositoryDatabase(environment2,
                stagingEnvironment2);

        Repository defaultRepo = new Repository(defaultRepositoryDatabase, geogitDirectory);

        defaultRepo.create();
        try {
            this.initRepo(defaultRepo);
        } catch (IOException e) {
            LOGGER.log(Level.FINER, e.getMessage(), e);
        }
        return defaultRepo;
    }
	
	public void initRepo(Repository repo) throws IOException {

        final RefDatabase refDatabase = repo.getRefDatabase();
        final ObjectDatabase objectDatabase = repo.getObjectDatabase();

        Ref typesTreeRef = refDatabase.getRef(TYPE_NAMES_REF_TREE);
        if (null == typesTreeRef) {
            LOGGER.info("Initializing type name references. Types tree does not exist");
            final RevTree typesTree = objectDatabase.newTree();
            ObjectId typesTreeId;
            try {
                WrappedSerialisingFactory serialisingFactory;
                serialisingFactory = WrappedSerialisingFactory.getInstance();
                ObjectWriter<RevTree> treeWriter = serialisingFactory
                        .createRevTreeWriter(typesTree);
                typesTreeId = objectDatabase.put(treeWriter);
            } catch (Exception e) {
                throw new IOException(e);
            }
            typesTreeRef = new Ref(TYPE_NAMES_REF_TREE, typesTreeId, TYPE.TREE);
            refDatabase.put(typesTreeRef);
        }
//        GeoGitDataStore ggit = new GeoGitDataStore(repo);
//
//        List<Name> featureTypes = ggit.getNames();
//        LOGGER.log(Level.WARNING, "outputting type names after setup of repo");
//        for (Name type : featureTypes) {
//            LOGGER.log(Level.INFO, type.getNamespaceURI() + ":" + type.getLocalPart());
//        }
	}


}
