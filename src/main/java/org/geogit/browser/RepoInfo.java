package org.geogit.browser;

import org.geogit.api.Ref;
import org.geogit.api.RevCommit;
import org.geogit.api.config.BranchConfigObject;
import org.geogit.api.config.RemoteConfigObject;
import org.geotools.filter.identity.FeatureIdVersionedImpl;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.Name;

public class RepoInfo {

	public static enum EntryType{
		FEATURE,FEATURE_ID, FEATURE_TYPE_NAME, FEATURE_TYPE, REV_COMMIT,CONFIG_BRANCH,CONFIG_REMOTE,REF_TREE,REF_TAG,REF_REMOTE, REF_NAMESPACE, REF_FEATURE_TYPE, REF_FEATURE;
	}
	private EntryType type;
	
	private Object entry;

	public RepoInfo(Object entry, EntryType entryType) {
		this.entry = entry;
		this.type = entryType;
	}

	public EntryType getType() {
		return type;
	}

	public void setType(EntryType type) {
		this.type = type;
	}

	public Object getEntry() {
		return entry;
	}

	public void setEntry(Object entry) {
		this.entry = entry;
	}
	
	public String toString(){
		String value = null;
		Name typeName  = null;
		switch (this.type){
		case FEATURE_TYPE_NAME:
			 typeName = (Name) entry;
			value =  typeName.getNamespaceURI() + ":"+ typeName.getLocalPart();
			break;
		case FEATURE_TYPE:
			 typeName = ((SimpleFeatureType) entry).getName();
			value =  typeName.getNamespaceURI() + ":"+ typeName.getLocalPart();
			break;
	case REV_COMMIT:
		RevCommit commit = (RevCommit) entry;
		value =  "Commit."+ commit.getId();
		break;
	case FEATURE_ID:
		value = ((FeatureIdVersionedImpl)entry).getID();
		break;
	case CONFIG_REMOTE:
		value = ((RemoteConfigObject)entry).getName();
		break;
	case CONFIG_BRANCH:
		value = ((BranchConfigObject)entry).getName();
		break;
	case REF_NAMESPACE:
		default: 
		value =  ( (Ref) entry).getName();
		break;
	}
		return value;
	}
	
}
