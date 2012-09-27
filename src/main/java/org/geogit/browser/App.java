package org.geogit.browser;

import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JEditorPane;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTree;
import javax.swing.UIManager;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;

import org.geogit.api.ObjectId;
import org.geogit.api.Ref;
import org.geogit.api.RevBlob;
import org.geogit.api.RevCommit;
import org.geogit.api.RevObject;
import org.geogit.api.RevTree;
import org.geogit.api.TreeVisitor;
import org.geogit.api.config.BranchConfigObject;
import org.geogit.api.config.Config;
import org.geogit.api.config.RemoteConfigObject;
import org.geogit.repository.Repository;
import org.geotools.data.geogit.GeoGitDataStore;
import org.geotools.data.geogit.GeoGitFeatureSource;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.NameImpl;
import org.geotools.filter.identity.FeatureIdImpl;
import org.opengis.feature.Property;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.Name;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.Id;
import org.opengis.filter.identity.FeatureId;
import org.geogit.browser.RepoInfo.EntryType;
import org.geogit.browser.RepoInfo.EntryType.*;

import com.vividsolutions.jts.io.ParseException;

/**
 * Hello world!
 * 
 */
public class App extends JPanel implements TreeSelectionListener {
	private GeoGitUtil util;
	private JTree tree;
	private Repository repo;
	private JEditorPane htmlPane;
	private GeoGitDataStore store;
	private File myFile;
	private File base = new File("C:\\site_data_dir\\data\\admin");
	private URL helpFile;
	private String helpText;
	private JScrollPane treeView;
	private JScrollPane htmlView;
	private static boolean DEBUG = false;
	private static boolean useSystemLookAndFeel = false;
	static Logger logger = org.geotools.util.logging.Logging
			.getLogger("qpws.parkinfo.rest.geogit");

	public static void main(String[] args) {
		System.out.println("Hello World!");
		javax.swing.SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				createAndShowGUI();
			}
		});

	}

	private static void createAndShowGUI() {
		if (useSystemLookAndFeel) {
			try {
				UIManager.setLookAndFeel(UIManager
						.getSystemLookAndFeelClassName());
			} catch (Exception e) {
				System.err.println("Couldn't use system look and feel.");
			}
		}

		// Create and set up the window.
		JFrame frame = new JFrame("GeoGit Browser");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		// Add content to the window.
		frame.add(new App());

		// Display the window.
		frame.pack();
		frame.setVisible(true);
	}

	public App() {

		super(new GridLayout(1, 0));
		URL helpFile = this.getClass().getResource("/help.html");
		BufferedReader in =null;
		helpText = "";
		String inputLine;
		try {
			in = new BufferedReader(
					new InputStreamReader(helpFile.openStream()));
			while ((inputLine = in.readLine()) != null) {
				helpText += inputLine;
			}
			in.close();

		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} finally{
			if (in!=null)
				try {
					in.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
		}
//		 JFileChooser chooser = new JFileChooser();
//		 chooser.setDialogTitle("Select geogit repository folder");
//		 chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
//		 int returnVal = chooser.showOpenDialog(App.this);
//		 if(returnVal == JFileChooser.APPROVE_OPTION) {
//		 base = chooser.getSelectedFile();
//		 } else {
//		 System.out.println("Open command cancelled by user." );
//		 return;
//		 }
		util = new GeoGitUtil();

		try {
			repo = util.getRepository(base);
			store = new GeoGitDataStore(repo);
		} catch (FileNotFoundException e) {
			System.err.println("Base Directory not found!!!");
			e.printStackTrace();
			return;
		} catch (IOException e) {
			System.err.println("Could not open GeogitDataStore!!!");
			e.printStackTrace();
			return;
		}
		DefaultMutableTreeNode top = new DefaultMutableTreeNode("GeoGit");
		createDataViewNodes(top);
		this.createHeadViewNodes(top);
		this.createCommitViewNodes(top);
		createBranchViewNodes(top);
		this.createRemotesViewNodes(top);
		tree = new JTree(top);

		// Listen for when the selection changes.
		tree.addTreeSelectionListener(this);
		treeView = new JScrollPane(tree);
		htmlPane = new JEditorPane();
		htmlPane.setEditable(false);
		htmlPane.setContentType("text/html");
		// initHelp();
		htmlView = new JScrollPane(htmlPane);

		// Add the scroll panes to a split pane.
		JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
		splitPane.setTopComponent(treeView);
		splitPane.setBottomComponent(htmlView);

		Dimension minimumSize = new Dimension(300, 50);
		htmlView.setMinimumSize(minimumSize);
		treeView.setMinimumSize(minimumSize);
		splitPane.setDividerLocation(300);
		splitPane.setPreferredSize(new Dimension(800, 600));

		// Add the split pane to this panel.
		add(splitPane);
	}

	public void windowClosed(WindowEvent e) {
		if (repo != null)
			repo.close();

	}

	private void createBranchViewNodes(DefaultMutableTreeNode top) {
		DefaultMutableTreeNode featureTypes = null;
		featureTypes = new DefaultMutableTreeNode("Branches");
		top.add(featureTypes);
		Config config = new Config(this.store.getRepository());
		Map<String, BranchConfigObject> branches = config.getBranches();
		for (BranchConfigObject branch : config.getBranches().values()){
			DefaultMutableTreeNode configNode = new DefaultMutableTreeNode(
					new RepoInfo(branch, RepoInfo.EntryType.CONFIG_BRANCH));
			featureTypes.add(configNode);
		}

	}
	
	private void createRemotesViewNodes(DefaultMutableTreeNode top) {
		DefaultMutableTreeNode featureTypes = null;
		featureTypes = new DefaultMutableTreeNode("Remotes");
		top.add(featureTypes);
		Config config = new Config(this.store.getRepository());
		Map<String, RemoteConfigObject> branches = config.getRemotes();
		for (RemoteConfigObject branch : config.getRemotes().values()){
			DefaultMutableTreeNode configNode = new DefaultMutableTreeNode(
					new RepoInfo(branch, RepoInfo.EntryType.CONFIG_REMOTE));
			featureTypes.add(configNode);
		}

	}

	private void createDataViewNodes(DefaultMutableTreeNode top) {
		DefaultMutableTreeNode featureTypes = null;
		featureTypes = new DefaultMutableTreeNode("Data View");
		top.add(featureTypes);
		try {
			List<Name> featureTypeNames = store.getNames();
			for (Name type : featureTypeNames) {
				DefaultMutableTreeNode featureTypeNode = new DefaultMutableTreeNode(
						new RepoInfo(type, RepoInfo.EntryType.FEATURE_TYPE_NAME));
				featureTypes.add(featureTypeNode);
				GeoGitFeatureSource featureSource = store
						.getFeatureSource(type);
				SimpleFeatureIterator it = featureSource.getFeatures()
						.features();
				while (it.hasNext()) {
					SimpleFeature feature = it.next();
					DefaultMutableTreeNode featureIdNode = new DefaultMutableTreeNode(
							new RepoInfo(feature.getIdentifier(),
									RepoInfo.EntryType.FEATURE_ID));
					featureTypeNode.add(featureIdNode);
				}
			}
		} catch (IOException e) {
			System.err.println("There was a problem opening repo!!!!!");
			e.printStackTrace();
		}

	}

	private void createHeadViewNodes(DefaultMutableTreeNode top) {
		DefaultMutableTreeNode workingTree = new DefaultMutableTreeNode(
				"Working Tree");
		top.add(workingTree);

		Ref head = store.getRepository().getHead();
		DefaultMutableTreeNode headNode = new DefaultMutableTreeNode(
				new RepoInfo(head, EntryType.REF_TREE));
		workingTree.add(headNode);
		Iterator<Ref> headTree = store.getRepository().getHeadTree()
				.iterator(null);
		while (headTree.hasNext()) {
			Ref namespace = headTree.next();
			DefaultMutableTreeNode nsnode = new DefaultMutableTreeNode(
					new RepoInfo(namespace, EntryType.REF_NAMESPACE));
			headNode.add(nsnode);

			Iterator<Ref> nsTree = store.getRepository().getObjectDatabase()
					.getTree(namespace.getObjectId()).iterator(null);
			while (nsTree.hasNext()) {
				Ref featType = nsTree.next();
				DefaultMutableTreeNode ftnode = new DefaultMutableTreeNode(
						new RepoInfo(featType, EntryType.REF_FEATURE_TYPE));
				nsnode.add(ftnode);

				Iterator<Ref> ftTree = store.getRepository()
						.getObjectDatabase().getTree(featType.getObjectId())
						.iterator(null);
				while (ftTree.hasNext()) {
					Ref feat = ftTree.next();
					DefaultMutableTreeNode fnode = new DefaultMutableTreeNode(
							new RepoInfo(feat, EntryType.REF_FEATURE));
					ftnode.add(fnode);
				}
			}
		}
	}

	private void createCommitViewNodes(DefaultMutableTreeNode top) {
		DefaultMutableTreeNode commitTree = new DefaultMutableTreeNode(
				"Commits");
		top.add(commitTree);

		Ref head = store.getRepository().getHead();
		// DefaultMutableTreeNode headNode = new DefaultMutableTreeNode(new
		// RepoInfo(head, EntryType.REF_TREE));
		// workingTree.add(headNode);
		RevCommit headCommit = store.getRepository().getCommit(
				head.getObjectId());
		addCommitNav(commitTree, headCommit);

	}

	private void addCommitNav(DefaultMutableTreeNode parentTree,
			RevCommit commit) {
		DefaultMutableTreeNode commitNode = new DefaultMutableTreeNode(
				new RepoInfo(commit, EntryType.REV_COMMIT));
		parentTree.add(commitNode);
		for (ObjectId commitId : commit.getParentIds()) {
			try {
				RevCommit parentCommit = store.getRepository().getCommit(
						commitId);
				// DefaultMutableTreeNode parentCommitNode = new
				// DefaultMutableTreeNode(new RepoInfo(parentCommit,
				// EntryType.REV_COMMIT));
				// parentTree.add(parentCommitNode);
				addCommitNav(parentTree, parentCommit);
			} catch (IllegalArgumentException ex) {
				logger.log(Level.WARNING, "Could not find commit for objectid:"
						+ commitId);
			}
		}
	}

	public void valueChanged(TreeSelectionEvent arg0) {
		DefaultMutableTreeNode node = (DefaultMutableTreeNode) tree
				.getLastSelectedPathComponent();
		if (node == null)
			return;
		SimpleFeatureType type = null;
		SimpleFeature SimpleFeaturefeature = null;
		Ref ref = null;
		Object nodeInfo = node.getUserObject();
		if (nodeInfo instanceof RepoInfo) {
			RepoInfo info = (RepoInfo) nodeInfo;
			switch (info.getType()) {
			case FEATURE_TYPE_NAME:
				try {
					type = getFeatureType((Name) info.getEntry());
					this.htmlPane.setText(displayFeatureType(type));
				} catch (IOException e) {
					e.printStackTrace();
				}
				break;
			case FEATURE_TYPE:
				break;
			case FEATURE:
				break;
			case REF_FEATURE:
				ref = (Ref) info.getEntry();
				try {
					DefaultMutableTreeNode parentInfo = (DefaultMutableTreeNode) node
							.getParent();
					RepoInfo info2 = (RepoInfo) parentInfo.getUserObject();
					String typeNameString = null;
					Name typeName = null;
					if (info2.getType().equals(
							RepoInfo.EntryType.REF_FEATURE_TYPE)) {
						typeNameString = (String) ((Ref) info2.getEntry())
								.getName();
						DefaultMutableTreeNode parentInfo2 = (DefaultMutableTreeNode) parentInfo
								.getParent();
						RepoInfo info3 = (RepoInfo) parentInfo2.getUserObject();
						String namespaceString = (String) ((Ref) info3
								.getEntry()).getName();
						typeName = new NameImpl(namespaceString, typeNameString);
					}
					SimpleFeaturefeature = this.getFeature(
							typeName,
							new FeatureIdImpl((String) ((Ref) info.getEntry())
									.getName()));
					this.htmlPane.setText(displayFeature(SimpleFeaturefeature)
							+ displayRef(ref));
				} catch (IOException e) {
					e.printStackTrace();
				}
				break;
			case REF_FEATURE_TYPE:
				try {
					ref = (Ref) info.getEntry();
					type = getFeatureType(ref.getName());
					this.htmlPane.setText(displayFeatureType(type)
							+ displayRef(ref));
				} catch (IOException e) {
					e.printStackTrace();
				}
				break;
			case REF_NAMESPACE:
				this.htmlPane.setText(this.displayNamespace((Ref) info
						.getEntry()));
				break;
			case FEATURE_ID:

				try {
					DefaultMutableTreeNode parentInfo = (DefaultMutableTreeNode) node
							.getParent();
					RepoInfo info2 = (RepoInfo) parentInfo.getUserObject();
					Name typeName = null;
					if (info2.getType().equals(
							RepoInfo.EntryType.FEATURE_TYPE_NAME)) {
						typeName = (Name) info2.getEntry();
					} else if (info2.getType().equals(
							RepoInfo.EntryType.FEATURE_TYPE)) {
						typeName = ((SimpleFeatureType) info2.getEntry())
								.getName();
					}
					SimpleFeaturefeature = this.getFeature(typeName,
							(FeatureId) info.getEntry());
					this.htmlPane.setText(displayFeature(SimpleFeaturefeature));
				} catch (IOException e) {
					e.printStackTrace();
				}
				break;
			case REV_COMMIT:
				this.htmlPane.setText(this.displayRevCommit((RevCommit) info
						.getEntry()));
				break;
			case CONFIG_REMOTE:
				this.htmlPane.setText(this.displayRemoteConfigObject((RemoteConfigObject) info
						.getEntry()));
				break;
			case CONFIG_BRANCH:
				this.htmlPane.setText(this.displayBranchConfigObject((BranchConfigObject) info
						.getEntry()));
				break;
			default:
				this.htmlPane.setText(helpText);
			}
		} else {
			this.htmlPane.setText(helpText);
		}
		if (DEBUG) {
			System.out.println(nodeInfo.toString());
		}
		 ScrollUtil.scroll(htmlView, 8,1);
	}

	private String displayFeatureType(SimpleFeatureType type) {
		String output = "";
		output += "<h1>" + type.getName().getNamespaceURI() + ":"
				+ type.getName().getLocalPart() + "</h1>\n";
		output += "<ul>\n";
		for (AttributeDescriptor attribute : type.getAttributeDescriptors()) {
			output += "<li><b>Attribute:</b> " + attribute.getLocalName() + " <b>Type:</b> " + attribute.getType().getBinding().getName()+ " </li>  \n";
		}
		output += "</ul>\n";
		return output;
	}

	private String displayRemoteConfigObject(RemoteConfigObject config) {
		String output = "";
		output += "<h1>" + config.getName() + "</h1>\n";
		output += "<ul>\n";
		output += "<li><b>Name:</b>" + config.getName() + " </li>  \n";
			output += "<li><b>Fetch:</b>" + config.getFetch() + " </li>  \n";
			output += "<li><b>Url:</b>" + config.getUrl() + " </li>  \n";
		output += "</ul>\n";
		return output;
	}
	
	private String displayBranchConfigObject(BranchConfigObject config) {
		String output = "";
		output += "<h1>" + config.getName() + "</h1>\n";
		output += "<ul>\n";
		output += "<li><b>Name:</b>" + config.getName() + " </li>  \n";
			output += "<li><b>Merge:</b>" + config.getMerge() + " </li>  \n";
			output += "<li><b>Remote:</b>" + config.getRemote() + " </li>  \n";
		output += "</ul>\n";
		return output;
	}
	private String displayRevCommit(RevCommit revCommit) {
		String output = "";
		output += "<h1>Commit." + revCommit.getId() + "</h1>\n";
		output += "<ul><li><b>Id:</b> " + revCommit.getId() + "</li>\n";
		output += "<li><b>Timestamp:</b> " + formatTimeStamp(revCommit.getTimestamp())
				+ "</li>\n";
		output += "<li><b>Author:</b> " + revCommit.getAuthor() + "</li>\n";
		output += "<li><b>Message:</b> " + revCommit.getMessage()
				+ "</li></ul>\n";
		output += "<h2>Parent Commits</h2>\n";
		output += "<table border='1'>\n";
		output += "<tr><th>Commit</th><th>Timestamp</th><th>author</th><th>message</th></tr>\n";
		output += displayParentRevCommit(revCommit);
		output += "</table>\n";
		return output;
	}
	
	private String formatTimeStamp(long timestamp)  {
		SimpleDateFormat sdf = new SimpleDateFormat("MMM dd,yyyy HH:mm");
		 
		Date resultdate = new Date( timestamp);
		return sdf.format(resultdate);
	}

	private String displayParentRevCommit(RevCommit revCommit) {
		String output = "";
		for (ObjectId commitId : revCommit.getParentIds()) {
			try {
				RevCommit commit = store.getRepository().getCommit(commitId);
				output += "<tr><td>Commit." + commit.getId() + "</td><td>"
						+ formatTimeStamp(commit.getTimestamp()) + "</td><td>"
						+ commit.getAuthor() + "</td><td>"
						+ commit.getMessage() + "</td></tr>\n";
			} catch (Exception ex) {
			}
		}
		return output;
	}

	private String displayFeature(SimpleFeature feature) {
		String output = "";
		output += "<h1>" + feature.getID() + "</h1>\n";
		for (Property prop : feature.getProperties()) {
			output += "<li><b>Attribute:</b> " + prop.getName().getLocalPart() + " <b>Value:</b> "
					+ prop.getValue() + "</li>  \n";
		}
		return output;
	}

	private String displayRef(Ref ref) {
		String output = "<h1>Ref Details</h1>";
		output += "<p>name: " + ref.getName() + "<br />";
		output += "objectid: " + ref.getObjectId() + "<br />";
		output += "ref type: " + ref.getType() + "</p>";
		return output;
	}

	private String displayNamespace(Ref ref) {
		String output = "<h1>Namespace: " + ref.getName() + "</h1>";
		output += displayRef(ref);
		return output;
	}

	private SimpleFeatureType getFeatureType(Name entry) throws IOException {
		return store.getSchema(entry);
	}

	private SimpleFeatureType getFeatureType(String featureTypeName)
			throws IOException {
		return store.getSchema(featureTypeName);
	}

	private SimpleFeature getFeature(Name entry, FeatureId id)
			throws IOException {
		if (entry == null)
			throw new NullPointerException("Feature type name can not be null");
		SimpleFeature feature = null;
		SimpleFeatureSource source = store.getFeatureSource(entry);
		FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2();
		Filter filter;
		filter = ff.id(id);
		SimpleFeatureIterator it = source.getFeatures(filter).features();
		if (it.hasNext()) {
			feature = it.next();
		}
		return feature;
	}
}
