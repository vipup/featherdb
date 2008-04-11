package com.fourspaces.featherdb.httpd;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.fourspaces.featherdb.auth.Credentials;
import com.fourspaces.featherdb.backend.BackendException;
import com.fourspaces.featherdb.document.Document;
import com.fourspaces.featherdb.document.DocumentCreationException;

public class UpdateDocument extends BaseRequestHandler {

	public void handleInner(Credentials credentials, HttpServletRequest request, HttpServletResponse response, String db, String id, String rev) throws IOException, BackendException, DocumentCreationException{
		
		if (featherDB.getBackend().doesDatabaseExist(db)) {
			Document newDoc=null;
			String contentType = request.getContentType();
			if (contentType==null) {
				contentType = "application/javascript";
			}

			if (id!=null) {
				Document oldDoc = featherDB.getBackend().getDocument(db, id);
				if (oldDoc!=null) {
					if (oldDoc.getContentType().equals(contentType)) {
						sendError(response,"Mismatch in Content-Type",HttpServletResponse.SC_CONFLICT);
						log.error("Attempted to update a doc of type: {} with one of type: {}",oldDoc.getContentType(),contentType);
						return;
					}
					newDoc = Document.newRevision(featherDB.getBackend(), oldDoc,credentials.getUsername());
				}
			}
			if (newDoc == null) {
				// if 'id' is null, a new id will be generated by the backend
				newDoc= Document.newDocument(featherDB.getBackend(), db,id, contentType,credentials.getUsername());
			}
			newDoc.setRevisionData(request.getInputStream());
			newDoc = featherDB.getBackend().saveDocument(newDoc);
			featherDB.recalculateViewForDocument(newDoc);
			sendOK(response, db+"/"+newDoc.getId()+"/"+newDoc.getRevision()+" saved");
		} else {
			sendError(response, "Database does not exist: "+db,HttpServletResponse.SC_NOT_FOUND);
		}
	}

	public boolean match(Credentials credentials, HttpServletRequest request, String db, String id) {
		return (db!=null && !db.startsWith("_") && ((id!=null && request.getMethod().equals("PUT")) || (/*id==null &&*/ request.getMethod().equals("POST"))) && credentials.isAuthorizedWrite(db));
	}

}