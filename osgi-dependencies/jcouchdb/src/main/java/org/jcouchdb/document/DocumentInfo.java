package org.jcouchdb.document;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.svenson.JSONProperty;

/**
 * Provides information about created or updated documents.
 *
 * @author fforw at gmx dot de
 *
 */
public class DocumentInfo
{
    private static Logger LOG = LoggerFactory.getLogger(DocumentInfo.class);
    
    private String id, revision, error, reason;

    private boolean ok;
    
    private boolean accepted;

    public String getId()
    {
        return id;
    }

    public void setId(String id)
    {
        this.id = id;
    }

    @JSONProperty("rev")
    public String getRevision()
    {
        return revision;
    }

    public void setRevision(String revision)
    {
        this.revision = revision;
    }

    public boolean isOk()
    {
        return ok;
    }

    public void setOk(boolean ok)
    {
        this.ok = ok;
    }

    public boolean isAccepted() 
    {
        return accepted;
    }
    
    public void setAccepted(boolean accepted) 
    {
        LOG.warn("Document was saved but the quorum was not met.");
        this.accepted = accepted;
    }

    public String getError()
    {
        return error;
    }

    public void setError(String error)
    {
        if (error != null)
        {
            this.ok = false;
        }
        this.error = error;
    }

    public String getReason()
    {
        return reason;
    }

    public void setReason(String reason)
    {
        this.reason = reason;
    }
    
    @Override
    public String toString()
    {
        return super.toString()+": id = "+id+", revision = "+revision+(ok ? ", ok = true" : ", ok = false, error = "+error+", reason = " + reason);
    }

}
