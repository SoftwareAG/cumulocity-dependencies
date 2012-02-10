package org.jcouchdb.document;

import java.util.List;

import org.svenson.JSONProperty;
import org.svenson.JSONTypeHint;

public class ChangeNotification
{
    private long sequence;

    private String id;

    private List<ChangeEntry> changes;

    private boolean deleted;

    public long getSequence()
    {
        return sequence;
    }


    @JSONProperty("seq")
    public void setSequence(long sequence)
    {
        this.sequence = sequence;
    }


    public String getId()
    {
        return id;
    }


    public void setId(String id)
    {
        this.id = id;
    }


    public List<ChangeEntry> getChanges()
    {
        return changes;
    }


    @JSONTypeHint(ChangeEntry.class)
    public void setChanges(List<ChangeEntry> changes)
    {
        this.changes = changes;
    }

    
    public void setDeleted(boolean deleted) {
    	this.deleted = deleted;
    }
    
    
    public boolean isDeleted() {
    	return deleted;
    }

    @Override
    public String toString()
    {
        return super.toString() + "[changes=" + changes + ", id=" + id + ", sequence=" + sequence +
        	", deleted=" + deleted +
            "]";
    }
}
