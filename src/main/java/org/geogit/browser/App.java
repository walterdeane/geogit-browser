package org.geogit.browser;

import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
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

import org.geogit.api.Ref;
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
	private File base;
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
		JFrame frame = new JFrame("TreeDemo");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		// Add content to the window.
		frame.add(new App());

		// Display the window.
		frame.pack();
		frame.setVisible(true);
	}

	public App() {
		
		super(new GridLayout(1, 0));
		
		JFileChooser chooser = new JFileChooser();
		chooser.setDialogTitle("Select geogit repository folder");
		chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		int returnVal = chooser.showOpenDialog(App.this);
		if(returnVal == JFileChooser.APPROVE_OPTION) {
		base = chooser.getSelectedFile();
		} else {
        	System.out.println("Open command cancelled by user." );
        	return;
        }
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
		this.createCommitViewNodes(top);
		tree = new JTree(top);
		
		//Listen for when the selection changes.
        tree.addTreeSelectionListener(this);
		JScrollPane treeView = new JScrollPane(tree);
		htmlPane = new JEditorPane();
		htmlPane.setEditable(false);
		htmlPane.setContentType("text/html");
		// initHelp();
		JScrollPane htmlView = new JScrollPane(htmlPane);

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

	private void createDataViewNodes(DefaultMutableTreeNode top) {
		DefaultMutableTreeNode featureTypes = null;
		featureTypes = new DefaultMutableTreeNode("Data View");
		top.add(featureTypes);                                                                                                                                                                                                                 
		try {
			List<Name> featureTypeNames = store.getNames();
			for (Name type : featureTypeNames) {
				DefaultMutableTreeNode featureTypeNode = new DefaultMutableTreeNode(
						new RepoInfo(type,
								RepoInfo.EntryType.FEATURE_TYPE_NAME));
				featureTypes.add(featureTypeNode);
				GeoGitFeatureSource featureSource = store.getFeatureSource(type);
				SimpleFeatureIterator it = featureSource.getFeatures().features();
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
	
	private void createCommitViewNodes(DefaultMutableTreeNode top) {
		DefaultMutableTreeNode workingTree  = new DefaultMutableTreeNode("Working Tree");		
		top.add(workingTree); 
		
		Ref head = store.getRepository().getHead();
		DefaultMutableTreeNode headNode  = new DefaultMutableTreeNode(new RepoInfo(head, EntryType.REF_TREE));		
		workingTree.add(headNode); 
		Iterator<Ref> headTree = store.getRepository().getHeadTree().iterator(null);
		while (headTree.hasNext()){
			Ref namespace = headTree.next();
			DefaultMutableTreeNode nsnode  = new DefaultMutableTreeNode(new RepoInfo(namespace, EntryType.REF_NAMESPACE));
			headNode.add(nsnode);
			
			Iterator<Ref> nsTree = store.getRepository().getObjectDatabase().getTree(namespace.getObjectId()).iterator(null);
			while (nsTree.hasNext()){
				Ref featType = nsTree.next();
				DefaultMutableTreeNode ftnode  = new DefaultMutableTreeNode(new RepoInfo(featType, EntryType.REF_FEATURE_TYPE));
				nsnode.add(ftnode);
				
				Iterator<Ref> ftTree = store.getRepository().getObjectDatabase().getTree(featType.getObjectId()).iterator(null);
				while (ftTree.hasNext()){
					Ref feat = ftTree.next();
					DefaultMutableTreeNode fnode  = new DefaultMutableTreeNode(new RepoInfo(feat, EntryType.REF_FEATURE));
					ftnode.add(fnode);
				}	
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
					type = getFeatureType((Name)info.getEntry());
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
				ref =  (Ref) info.getEntry();
				try {
					DefaultMutableTreeNode parentInfo = (DefaultMutableTreeNode) node.getParent();
					RepoInfo info2 = (RepoInfo) parentInfo.getUserObject();
					String typeNameString = null;
					Name typeName = null;
					if (info2.getType().equals(RepoInfo.EntryType.REF_FEATURE_TYPE)){
						typeNameString = (String)((Ref) info2.getEntry()).getName();
						DefaultMutableTreeNode parentInfo2 = (DefaultMutableTreeNode) parentInfo.getParent();
						RepoInfo info3= (RepoInfo) parentInfo2.getUserObject();
						String namespaceString =  (String)((Ref) info3.getEntry()).getName();
						typeName = new NameImpl(namespaceString,typeNameString);
					} 
					SimpleFeaturefeature = this.getFeature(typeName, new FeatureIdImpl((String)((Ref) info.getEntry()).getName()));
					this.htmlPane.setText(displayFeature(SimpleFeaturefeature) + displayRef(ref));
				} catch (IOException e) {
					e.printStackTrace();
				}
				break;
			case REF_FEATURE_TYPE:
				try {
					 ref =  (Ref) info.getEntry();
					type = getFeatureType(ref.getName());
					this.htmlPane.setText(displayFeatureType(type) + displayRef(ref));
				} catch (IOException e) {
					e.printStackTrace();
				}
				break;
			case REF_NAMESPACE:
				this.htmlPane.setText(this.displayNamespace((Ref) info.getEntry()));
				break;
			case FEATURE_ID:

				try {
					DefaultMutableTreeNode parentInfo = (DefaultMutableTreeNode) node.getParent();
					RepoInfo info2 = (RepoInfo) parentInfo.getUserObject();
					Name typeName = null;
					if (info2.getType().equals(RepoInfo.EntryType.FEATURE_TYPE_NAME)){
						typeName = (Name) info2.getEntry();
					} else if (info2.getType().equals(RepoInfo.EntryType.FEATURE_TYPE)){
						typeName = ((SimpleFeatureType) info2.getEntry()).getName();
					}
					SimpleFeaturefeature = this.getFeature(typeName,(FeatureId) info.getEntry());
					this.htmlPane.setText(displayFeature(SimpleFeaturefeature));
				} catch (IOException e) {
					e.printStackTrace();
				}
				break;
			case REF_COMMIT:
				break;
			}
		} else {
			//TODO: add help text display
		}
		if (DEBUG) {
			System.out.println(nodeInfo.toString());
		}

	}

	private String displayFeatureType(SimpleFeatureType type) {
		String output = "";
		output += "<h1>" + type.getName().getNamespaceURI() +":" + type.getName().getLocalPart() + "</h1>\n";
		for (AttributeDescriptor attribute : type.getAttributeDescriptors()){
			output += "<li>" + attribute.toString() + " </li>  \n";
		}
		return output;
	}
	
	private String displayFeature(SimpleFeature feature) {
		String output = "";
		output += "<h1>" + feature.getID()+ "</h1>\n";
		for (Property prop : feature.getProperties()){
			output += "<li>" + prop.getName().getLocalPart() + " : " + prop.getValue() + " </li>  \n";
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
		output+= displayRef(ref);
		return output;
	}
	

	private SimpleFeatureType getFeatureType(Name entry) throws IOException {
		return store.getSchema(entry);
	}
	
	private SimpleFeatureType getFeatureType(String featureTypeName) throws IOException {
		return store.getSchema(featureTypeName);
	}
	
	private SimpleFeature getFeature(Name entry, FeatureId id) throws IOException {
		if (entry==null)throw new NullPointerException("Feature type name can not be null");
		SimpleFeature feature = null;
		SimpleFeatureSource source =  store.getFeatureSource(entry);
		FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2();
	    Filter filter;
	    filter = ff.id(id);
		SimpleFeatureIterator it = source.getFeatures(filter).features();
		if (it.hasNext()){
			feature = it.next();
		}
		return feature;
	}
}
